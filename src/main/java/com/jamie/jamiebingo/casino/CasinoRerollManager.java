package com.jamie.jamiebingo.casino;

import com.jamie.jamiebingo.bingo.BingoCard;
import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.bingo.CardComposition;
import com.jamie.jamiebingo.bingo.ColorVariantUtil;
import com.jamie.jamiebingo.bingo.ConfigurableCardGenerator;
import com.jamie.jamiebingo.bingo.ResolvedSlot;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.network.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CasinoRerollManager {

    /** Slots currently rolling (server-only safety) */
    private static final Set<SlotKey> rollingSlots = new HashSet<>();
    private static final int LAST_REROLL_FINISH_DELAY_TICKS = 20 * 3;
    private static final int TURN_TIMEOUT_TICKS = 20 * 12;
    public static final int SPECIAL_MINE_X = -1001;
    public static final int SPECIAL_DEFUSE_X = -1002;
    private static int turnDeadlineTick = -1;

    /* ===============================
       START REROLL PHASE
       =============================== */

    public static void start(MinecraftServer server) {
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (data == null) return;

        if (data.isRerollPhaseActive()) return;
        if (data.currentCard == null) return;

        rollingSlots.clear();
        syncCardStateForRerollPlayers(server, data);

        // ✅ FIX (REAL): if rerolls are 0, we must still shutdown the casino UI
        // Clients are stuck on "Finalizing card" until they receive PacketCasinoShutdown.
        if (data.rerollsPerPlayer <= 0) {
            turnDeadlineTick = -1;

            schedule(server, LAST_REROLL_FINISH_DELAY_TICKS, () -> {
                for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                    NetworkHandler.send(
                            new PacketCasinoShutdown(),
                            PacketDistributor.PLAYER.with(player)
                    );
                }
                data.beginMatchAfterCasino(server);
            });

            return;
        }

        // Normal reroll phase
        data.beginRerollPhase(
                com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)
                        .stream()
                        .map(ServerPlayer::getUUID)
                        .toList()
        );
        CasinoModeManager.armRerollFailSafe(server);

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            NetworkHandler.send(
                    new PacketCasinoEnterRerollPhase(data.getRemainingRerolls(com.jamie.jamiebingo.util.EntityUtil.getUUID(player)), false),
                    PacketDistributor.PLAYER.with(player)
            );
        }

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            NetworkHandler.send(
                    new PacketCasinoRerollCount(data.getRemainingRerolls(com.jamie.jamiebingo.util.EntityUtil.getUUID(player))),
                    PacketDistributor.PLAYER.with(player)
            );
        }

        turnDeadlineTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + TURN_TIMEOUT_TICKS;
        broadcastTurn(server);
    }

    private static void syncCardStateForRerollPlayers(MinecraftServer server, BingoGameData data) {
        if (server == null || data == null || data.currentCard == null) return;
        long end = System.currentTimeMillis() + 1000L;
        int gridSize = Math.max(1, data.currentCard.getSize());

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            NetworkHandler.send(
                    new PacketCasinoStart(end, 0L, gridSize),
                    PacketDistributor.PLAYER.with(player)
            );
        }

        for (int y = 0; y < data.currentCard.getSize(); y++) {
            for (int x = 0; x < data.currentCard.getSize(); x++) {
                BingoSlot slot = data.currentCard.getSlot(x, y);
                if (slot == null) continue;
                String id = slot.getId();
                boolean isQuest = id != null && id.startsWith("quest.");
                PacketCasinoRollFinal msg = new PacketCasinoRollFinal(
                        x,
                        y,
                        slot.getId(),
                        slot.getName(),
                        slot.getCategory(),
                        slot.getRarity(),
                        isQuest
                );
                for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                    NetworkHandler.send(msg, PacketDistributor.PLAYER.with(player));
                }
            }
        }
    }

    /* ===============================
       REROLL REQUEST
       =============================== */

    public static void requestReroll(
            MinecraftServer server,
            ServerPlayer player,
            int x,
            int y
    ) {
        if (server == null || player == null) return;
        if (FakeRerollManager.isActive()) {
            FakeRerollManager.requestSelection(server, player, x, y);
            return;
        }

        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isRerollPhaseActive() || data.currentCard == null) {
            reject(player, x, y);
            return;
        }

        UUID current = data.getCurrentRerollPlayer();
        if (!com.jamie.jamiebingo.util.EntityUtil.getUUID(player).equals(current)) {
            reject(player, x, y);
            return;
        }

        BingoCard card = data.currentCard;
        int size = card.getSize();
        UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);

        if (x == SPECIAL_MINE_X || x == SPECIAL_DEFUSE_X) {
            if (data.getRemainingRerolls(playerId) <= 0) {
                reject(player, x, y);
                return;
            }
            boolean success = handleSpecialReroll(server, data, x, y);
            if (!success) {
                reject(player, x, y);
                return;
            }
            if (!data.consumeReroll(playerId)) {
                reject(player, x, y);
                return;
            }
            CasinoModeManager.bumpRerollFailSafe(server);
            turnDeadlineTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + TURN_TIMEOUT_TICKS;
            NetworkHandler.send(
                    new PacketCasinoRerollCount(data.getRemainingRerolls(playerId)),
                    PacketDistributor.PLAYER.with(player)
            );
            sendToRerollPlayers(server, new PacketCasinoSlotStart(x, y));
            data.advanceRerollTurn();
            turnDeadlineTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + TURN_TIMEOUT_TICKS;
            broadcastTurn(server);
            if (isFinished(server)) {
                finish(server);
            }
            return;
        }

        if (x < 0 || y < 0 || x >= size || y >= size) {
            reject(player, x, y);
            return;
        }

        SlotKey key = new SlotKey(x, y);
        if (rollingSlots.contains(key)) {
            reject(player, x, y);
            return;
        }

        BingoSlot oldSlot = card.getSlot(x, y);
        if (oldSlot == null) {
            reject(player, x, y);
            return;
        }

        if (!data.consumeReroll(playerId)) {
            reject(player, x, y);
            return;
        }
        CasinoModeManager.bumpRerollFailSafe(server);
        turnDeadlineTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + TURN_TIMEOUT_TICKS;

        rollingSlots.add(key);

        NetworkHandler.send(
                new PacketCasinoRerollCount(data.getRemainingRerolls(playerId)),
                PacketDistributor.PLAYER.with(player)
        );

        Set<String> usedKeys = buildUsedKeys(card, data);
        usedKeys.remove(groupKeyForBingoSlot(oldSlot, data));

        Map<String, Integer> categoryCounts = buildCategoryCounts(card);
        if (oldSlot.getCategory() != null) {
            categoryCounts.computeIfPresent(oldSlot.getCategory(), (k, v) -> Math.max(0, v - 1));
        }

        ResolvedSlot resolved = ConfigurableCardGenerator.rollResolvedSlot(
                data,
                server,
                usedKeys,
                categoryCounts,
                data.difficulty,
                data.composition == null ? CardComposition.CLASSIC_ONLY : data.composition,
                data.questPercent,
                data.getBlacklistedSlotIds()
        );

        if (resolved == null) {
            rollingSlots.remove(key);
            reject(player, x, y);
            return;
        }

        final int tStart  = 0;
        final int tRarity = 10;
        final int tPath   = 22;
        final int tFinal  = 40;

        schedule(server, tStart,
                () -> sendToRerollPlayers(server, new PacketCasinoSlotStart(x, y)));

        schedule(server, tRarity,
                () -> sendToRerollPlayers(server, new PacketCasinoRollRarity(x, y, resolved.rarity)));

        schedule(server, tPath,
                () -> sendToRerollPlayers(server, new PacketCasinoRollPath(x, y, resolved.category)));

        schedule(server, tFinal, () -> {

            BingoSlot newSlot = new BingoSlot(
                    resolved.id,
                    resolved.name,
                    resolved.category,
                    resolved.rarity
            );

            card.setSlot(x, y, newSlot);
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

            sendToRerollPlayers(server, new PacketCasinoRollFinal(
                    x, y,
                    resolved.id,
                    resolved.name,
                    resolved.category,
                    resolved.rarity,
                    resolved.isQuest
            ));

            rollingSlots.remove(key);

            data.advanceRerollTurn();
            turnDeadlineTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + TURN_TIMEOUT_TICKS;
            broadcastTurn(server);

            if (isFinished(server)) {
                finish(server);
            }
        });
    }

    /* ===============================
       REJECTION
       =============================== */

    private static void reject(ServerPlayer player, int x, int y) {
        NetworkHandler.send(
                new PacketCasinoRerollReject(x, y),
                PacketDistributor.PLAYER.with(player)
        );
    }

    private static boolean handleSpecialReroll(MinecraftServer server, BingoGameData data, int x, int y) {
        if (x == SPECIAL_MINE_X) {
            if (!com.jamie.jamiebingo.mines.MineModeManager.isActive()) return false;
            return com.jamie.jamiebingo.mines.MineModeManager.rerollMineByIndex(server, data, y);
        }
        if (x == SPECIAL_DEFUSE_X) {
            if (!com.jamie.jamiebingo.mines.MineModeManager.isActive()) return false;
            return com.jamie.jamiebingo.mines.MineModeManager.rerollDefuseGoal(server, data);
        }
        return false;
    }

    private static Map<String, Integer> buildCategoryCounts(BingoCard card) {
        Map<String, Integer> counts = new HashMap<>();
        if (card == null) return counts;
        int size = card.getSize();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot == null) continue;
                String category = slot.getCategory();
                if (category == null || category.isBlank()) continue;
                counts.merge(category, 1, Integer::sum);
            }
        }
        return counts;
    }

    private static Set<String> buildUsedKeys(BingoCard card, BingoGameData data) {
        Set<String> keys = new HashSet<>();
        if (card == null) return keys;
        int size = card.getSize();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot == null) continue;
                keys.add(groupKeyForBingoSlot(slot, data));
            }
        }
        return keys;
    }

    private static String groupKeyForBingoSlot(BingoSlot slot, BingoGameData data) {
        if (slot == null) return "";
        String id = slot.getId();
        if (id == null || id.isBlank()) return "";
        if (id.startsWith("quest.")) {
            com.jamie.jamiebingo.quest.QuestDefinition q =
                    com.jamie.jamiebingo.quest.QuestDatabase.getQuestById(id);
            if (q != null) {
                return ColorVariantUtil.questGroupKey(q);
            }
            return "quest:" + id.substring("quest.".length());
        }
        boolean separate = data != null && data.itemColorVariantsSeparate;
        return separate ? id : ColorVariantUtil.itemGroupKey(id);
    }

    /* ===============================
       FINISH
       =============================== */

    private static boolean isFinished(MinecraftServer server) {
        BingoGameData data = BingoGameData.get(server);

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            if (data.getRemainingRerolls(com.jamie.jamiebingo.util.EntityUtil.getUUID(player)) > 0) {
                return false;
            }
        }
        return rollingSlots.isEmpty();
    }

    public static void finish(MinecraftServer server) {
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isRerollPhaseActive()) return;

        data.endRerollPhase();
        rollingSlots.clear();
        turnDeadlineTick = -1;
        CasinoModeManager.bumpRerollFailSafe(server);

        schedule(server, LAST_REROLL_FINISH_DELAY_TICKS, () -> {
            for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                NetworkHandler.send(
                        new PacketCasinoShutdown(),
                        PacketDistributor.PLAYER.with(player)
                );
            }
            data.beginMatchAfterCasino(server);
        });
    }

    /* ===============================
       TURN BROADCAST
       =============================== */

    private static void broadcastTurn(MinecraftServer server) {
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isRerollPhaseActive()) return;
        UUID current = data.getCurrentRerollPlayer();
        if (current == null) return;

        ServerPlayer currentPlayer = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, current);
        if (currentPlayer == null) return;

        String name = currentPlayer.getName().getString();

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            boolean yourTurn = com.jamie.jamiebingo.util.EntityUtil.getUUID(player).equals(current);
            NetworkHandler.send(
                    new PacketCasinoRerollTurn(name, yourTurn),
                    PacketDistributor.PLAYER.with(player)
            );
            if (yourTurn) {
                playTurnBell(player);
            }
        }
    }

    private static void playTurnBell(ServerPlayer player) {
        if (player == null) return;
        NetworkHandler.send(new PacketPlayTeamSound("minecraft:block.note_block.bell", 1.0f, 1.0f), PacketDistributor.PLAYER.with(player));
        player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 1.0f);
    }

    /* ===============================
       HELPERS
       =============================== */

    private static void sendToRerollPlayers(MinecraftServer server, Object msg) {
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isRerollPhaseActive()) return;

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            NetworkHandler.send(
                    msg,
                    PacketDistributor.PLAYER.with(player)
            );
        }
    }

    private static void schedule(MinecraftServer server, int delay, Runnable task) {
        int runAt = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + Math.max(0, delay);
        CasinoTickScheduler.schedule(server, runAt, task);
    }

    public static void onServerTick(MinecraftServer server) {
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isRerollPhaseActive()) {
            turnDeadlineTick = -1;
            return;
        }
        // Do not auto-advance reroll turns by timeout.
        // Turn changes only after the current player uses a reroll.
    }

    private record SlotKey(int x, int y) {}
}





