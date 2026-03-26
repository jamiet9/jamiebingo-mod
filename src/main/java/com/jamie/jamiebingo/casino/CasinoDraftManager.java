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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

public class CasinoDraftManager {

    private static final int DRAFT_CHOICES = 3;
    private static final int DRAFT_FINISH_DELAY_TICKS = 60;

    private static boolean draftActive = false;
    private static final List<UUID> turnOrder = new ArrayList<>();
    private static int turnIndex = 0;
    private static final List<ResolvedSlot> currentChoices = new ArrayList<>();

    public static boolean isDraftActive() {
        return draftActive;
    }

    public static void forceReset() {
        draftActive = false;
        turnOrder.clear();
        turnIndex = 0;
        currentChoices.clear();
    }

    public static void start(MinecraftServer server) {
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || draftActive) return;

        int size = Math.max(1, data.size);
        data.currentCard = new BingoCard(size);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

        turnOrder.clear();
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            turnOrder.add(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
        }
        turnIndex = 0;

        if (turnOrder.isEmpty()) {
            finish(server);
            return;
        }

        draftActive = true;
        long end = System.currentTimeMillis() + 1000L;

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            NetworkHandler.send(new PacketCasinoStart(end, 0L, size), PacketDistributor.PLAYER.with(player));
            NetworkHandler.send(new PacketCasinoEnterDraftPhase(), PacketDistributor.PLAYER.with(player));
        }

        refreshChoices(server);
        broadcastTurn(server);
        broadcastChoices(server);
    }

    public static void requestPlace(MinecraftServer server, ServerPlayer player, int choiceIndex, int x, int y) {
        if (server == null || player == null || !draftActive) return;

        BingoGameData data = BingoGameData.get(server);
        if (data == null || data.currentCard == null) return;

        UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
        UUID current = getCurrentPlayerId();
        if (current == null || !current.equals(playerId)) return;

        BingoCard card = data.currentCard;
        int size = card.getSize();
        if (x < 0 || y < 0 || x >= size || y >= size) return;
        if (card.getSlot(x, y) != null) return;

        if (choiceIndex < 0 || choiceIndex >= currentChoices.size()) return;
        ResolvedSlot chosen = currentChoices.get(choiceIndex);
        if (chosen == null) return;

        BingoSlot slot = new BingoSlot(chosen.id, chosen.name, chosen.category, chosen.rarity);
        card.setSlot(x, y, slot);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

        NetworkHandler.send(new PacketCasinoRollFinal(
                        x,
                        y,
                        chosen.id,
                        chosen.name,
                        chosen.category,
                        chosen.rarity,
                        chosen.isQuest
                ),
                PacketDistributor.ALL.noArg());

        if (isCardFull(card)) {
            finish(server);
            return;
        }

        advanceTurn();
        refreshChoices(server);
        broadcastTurn(server);
        broadcastChoices(server);
    }

    public static void syncPlayerJoin(ServerPlayer player) {
        if (player == null || !draftActive) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (data == null || data.currentCard == null) return;

        long end = System.currentTimeMillis() + 1000L;
        NetworkHandler.send(new PacketCasinoStart(end, 0L, data.currentCard.getSize()), PacketDistributor.PLAYER.with(player));
        NetworkHandler.send(new PacketCasinoEnterDraftPhase(), PacketDistributor.PLAYER.with(player));

        for (int yy = 0; yy < data.currentCard.getSize(); yy++) {
            for (int xx = 0; xx < data.currentCard.getSize(); xx++) {
                BingoSlot slot = data.currentCard.getSlot(xx, yy);
                if (slot == null) continue;
                String id = slot.getId();
                boolean isQuest = id != null && id.startsWith("quest.");
                NetworkHandler.send(new PacketCasinoRollFinal(
                        xx,
                        yy,
                        slot.getId(),
                        slot.getName(),
                        slot.getCategory(),
                        slot.getRarity(),
                        isQuest
                ), PacketDistributor.PLAYER.with(player));
            }
        }

        UUID current = getCurrentPlayerId();
        if (current != null) {
            ServerPlayer currentPlayer = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, current);
            if (currentPlayer != null) {
                NetworkHandler.send(new PacketCasinoDraftTurn(
                        currentPlayer.getName().getString(),
                        current.equals(com.jamie.jamiebingo.util.EntityUtil.getUUID(player))
                ), PacketDistributor.PLAYER.with(player));
            }
        }

        NetworkHandler.send(new PacketCasinoDraftChoices(toPacketChoices(currentChoices)), PacketDistributor.PLAYER.with(player));
    }

    private static void refreshChoices(MinecraftServer server) {
        BingoGameData data = BingoGameData.get(server);
        if (data == null || data.currentCard == null) return;

        currentChoices.clear();

        Set<String> usedKeys = buildUsedKeys(data.currentCard, data);
        Map<String, Integer> categoryCounts = buildCategoryCounts(data.currentCard);

        Set<String> tempUsed = new HashSet<>(usedKeys);
        for (int i = 0; i < DRAFT_CHOICES; i++) {
            ResolvedSlot rolled = ConfigurableCardGenerator.rollResolvedSlot(
                    data,
                    server,
                    tempUsed,
                    categoryCounts,
                    data.difficulty,
                    data.composition == null ? CardComposition.CLASSIC_ONLY : data.composition,
                    data.questPercent,
                    data.getBlacklistedSlotIds()
            );
            if (rolled == null) break;
            currentChoices.add(rolled);
            tempUsed.add(groupKeyForResolved(rolled, data));
        }

        while (currentChoices.size() < DRAFT_CHOICES) {
            currentChoices.add(new ResolvedSlot("minecraft:dirt", "Dirt", "misc", "common", false));
        }
    }

    private static void broadcastTurn(MinecraftServer server) {
        UUID current = getCurrentPlayerId();
        if (current == null) return;
        ServerPlayer currentPlayer = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, current);
        if (currentPlayer == null) return;

        String name = currentPlayer.getName().getString();
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            boolean yourTurn = current.equals(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
            NetworkHandler.send(
                    new PacketCasinoDraftTurn(name, yourTurn),
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

    private static void broadcastChoices(MinecraftServer server) {
        PacketCasinoDraftChoices msg = new PacketCasinoDraftChoices(toPacketChoices(currentChoices));
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            NetworkHandler.send(msg, PacketDistributor.PLAYER.with(player));
        }
    }

    private static List<PacketCasinoDraftChoices.Choice> toPacketChoices(List<ResolvedSlot> slots) {
        List<PacketCasinoDraftChoices.Choice> out = new ArrayList<>();
        for (ResolvedSlot slot : slots) {
            if (slot == null) continue;
            out.add(new PacketCasinoDraftChoices.Choice(
                    slot.id,
                    slot.name,
                    slot.category,
                    slot.rarity,
                    slot.isQuest
            ));
        }
        return out;
    }

    private static UUID getCurrentPlayerId() {
        if (!draftActive || turnOrder.isEmpty()) return null;
        return turnOrder.get(turnIndex);
    }

    private static void advanceTurn() {
        if (turnOrder.isEmpty()) return;
        turnIndex = (turnIndex + 1) % turnOrder.size();
    }

    private static boolean isCardFull(BingoCard card) {
        int size = card.getSize();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (card.getSlot(x, y) == null) return false;
            }
        }
        return true;
    }

    private static Map<String, Integer> buildCategoryCounts(BingoCard card) {
        Map<String, Integer> counts = new HashMap<>();
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
            com.jamie.jamiebingo.quest.QuestDefinition q = com.jamie.jamiebingo.quest.QuestDatabase.getQuestById(id);
            if (q != null) {
                return ColorVariantUtil.questGroupKey(q);
            }
            return "quest:" + id.substring("quest.".length());
        }
        boolean separate = data != null && data.itemColorVariantsSeparate;
        return separate ? id : ColorVariantUtil.itemGroupKey(id);
    }

    private static String groupKeyForResolved(ResolvedSlot slot, BingoGameData data) {
        if (slot == null || slot.id == null || slot.id.isBlank()) return "";
        if (slot.isQuest) {
            com.jamie.jamiebingo.quest.QuestDefinition q = com.jamie.jamiebingo.quest.QuestDatabase.getQuestById(slot.id);
            if (q != null) {
                return ColorVariantUtil.questGroupKey(q);
            }
            return "quest:" + slot.id.substring("quest.".length());
        }
        boolean separate = data != null && data.itemColorVariantsSeparate;
        return separate ? slot.id : ColorVariantUtil.itemGroupKey(slot.id);
    }

    private static void finish(MinecraftServer server) {
        BingoGameData data = BingoGameData.get(server);
        draftActive = false;
        turnOrder.clear();
        turnIndex = 0;
        currentChoices.clear();

        if (data == null) return;

        if (data.rerollsPerPlayer > 0) {
            schedule(server, DRAFT_FINISH_DELAY_TICKS, () -> CasinoRerollManager.start(server));
            return;
        }

        schedule(server, DRAFT_FINISH_DELAY_TICKS, () -> {
            for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                NetworkHandler.send(new PacketCasinoShutdown(), PacketDistributor.PLAYER.with(player));
            }
            data.beginMatchAfterCasino(server);
        });
    }

    private static void schedule(MinecraftServer server, int delay, Runnable task) {
        if (server == null || task == null) return;
        int runAt = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + Math.max(0, delay);
        CasinoTickScheduler.schedule(server, runAt, task);
    }
}
