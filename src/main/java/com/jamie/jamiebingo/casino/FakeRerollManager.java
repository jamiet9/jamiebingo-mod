package com.jamie.jamiebingo.casino;

import com.jamie.jamiebingo.bingo.BingoCard;
import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.data.BroadcastHelper;
import com.jamie.jamiebingo.data.TeamData;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.PacketCasinoEnterRerollPhase;
import com.jamie.jamiebingo.network.PacketCasinoRerollCount;
import com.jamie.jamiebingo.network.PacketCasinoRerollReject;
import com.jamie.jamiebingo.network.PacketCasinoRerollTurn;
import com.jamie.jamiebingo.network.PacketCasinoRollFinal;
import com.jamie.jamiebingo.network.PacketCasinoRollPath;
import com.jamie.jamiebingo.network.PacketCasinoRollRarity;
import com.jamie.jamiebingo.network.PacketCasinoShutdown;
import com.jamie.jamiebingo.network.PacketCasinoSlotStart;
import com.jamie.jamiebingo.network.PacketCasinoStart;
import com.jamie.jamiebingo.network.PacketPlayTeamSound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Pregame fake-reroll phase.
 * Uses the casino reroll screen packets but only syncs rerolls to the acting player's team.
 */
public final class FakeRerollManager {
    private static final Set<SlotKey> rollingSlots = new HashSet<>();
    private static final int TURN_TIMEOUT_TICKS = 20 * 12;
    private static final int FINISH_DELAY_TICKS = 20 * 2;
    private static int turnDeadlineTick = -1;
    private static boolean active = false;

    private FakeRerollManager() {}

    public static boolean isActive() {
        return active;
    }

    public static boolean start(MinecraftServer server) {
        if (server == null) return false;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || data.currentCard == null || !data.isActive()) return false;
        if (!data.fakeRerollsEnabled || data.fakeRerollsPerPlayer <= 0) return false;
        if (!BingoGameData.isFakeRerollsSupportedWinCondition(data.winCondition)) return false;
        if (countActiveTeams(server, data) <= 1) return false;
        if (data.isFakeRerollPhaseActive()) return true;

        active = true;
        rollingSlots.clear();
        syncAllPlayersToFakePhase(server, data);

        List<UUID> turnPlayers = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)
                .stream()
                .map(ServerPlayer::getUUID)
                .toList();
        data.beginFakeRerollPhase(turnPlayers);

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            NetworkHandler.send(
                    new PacketCasinoEnterRerollPhase(data.getRemainingFakeRerolls(com.jamie.jamiebingo.util.EntityUtil.getUUID(player)), true),
                    PacketDistributor.PLAYER.with(player)
            );
            NetworkHandler.send(
                    new PacketCasinoRerollCount(data.getRemainingFakeRerolls(com.jamie.jamiebingo.util.EntityUtil.getUUID(player))),
                    PacketDistributor.PLAYER.with(player)
            );
        }

        turnDeadlineTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + TURN_TIMEOUT_TICKS;
        advancePastIneligibleTurns(server, data);
        broadcastTurn(server, data);
        return true;
    }

    public static void syncPlayerJoin(ServerPlayer player) {
        if (player == null) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isFakeRerollPhaseActive() || data.currentCard == null) return;

        long end = System.currentTimeMillis() + 1000L;
        NetworkHandler.send(new PacketCasinoStart(end, 0L, data.currentCard.getSize()), PacketDistributor.PLAYER.with(player));
        sendCardViewToPlayer(server, data, player);

        UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
        NetworkHandler.send(new PacketCasinoEnterRerollPhase(data.getRemainingFakeRerolls(playerId), true), PacketDistributor.PLAYER.with(player));
        NetworkHandler.send(new PacketCasinoRerollCount(data.getRemainingFakeRerolls(playerId)), PacketDistributor.PLAYER.with(player));

        UUID current = data.getCurrentFakeRerollPlayer();
        if (current != null) {
            ServerPlayer currentPlayer = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, current);
            if (currentPlayer != null) {
                NetworkHandler.send(
                        new PacketCasinoRerollTurn(currentPlayer.getName().getString(), playerId.equals(current)),
                        PacketDistributor.PLAYER.with(player)
                );
            }
        }
    }

    public static void requestSelection(MinecraftServer server, ServerPlayer player, int x, int y) {
        if (server == null || player == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isFakeRerollPhaseActive() || data.currentCard == null) {
            reject(player, x, y);
            return;
        }

        UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
        UUID current = data.getCurrentFakeRerollPlayer();
        if (current == null || !current.equals(playerId)) {
            reject(player, x, y);
            return;
        }

        int size = data.currentCard.getSize();
        if (x < 0 || y < 0 || x >= size || y >= size) {
            reject(player, x, y);
            return;
        }

        SlotKey key = new SlotKey(x, y);
        if (rollingSlots.contains(key)) {
            reject(player, x, y);
            return;
        }

        TeamData teamData = TeamData.get(server);
        UUID teamId = teamData.getTeamForPlayer(playerId);
        if (teamId == null) {
            reject(player, x, y);
            return;
        }

        int idx = y * size + x;
        if (data.hasTeamChosenFakeSlot(teamId, idx)) {
            reject(player, x, y);
            return;
        }

        if (!data.consumeFakeReroll(playerId)) {
            reject(player, x, y);
            return;
        }

        BingoSlot real = data.assignOrGetFakeRealSlot(server, idx, teamId, player.getName().getString());
        if (real == null) {
            reject(player, x, y);
            return;
        }
        data.revealFakeRealForTeam(teamId, idx);

        rollingSlots.add(key);
        NetworkHandler.send(
                new PacketCasinoRerollCount(data.getRemainingFakeRerolls(playerId)),
                PacketDistributor.PLAYER.with(player)
        );

        final int tStart = 0;
        final int tRarity = 10;
        final int tPath = 22;
        final int tFinal = 42;

        schedule(server, tStart, () -> sendToTeam(server, teamId, new PacketCasinoSlotStart(x, y)));
        schedule(server, tRarity, () -> sendToTeam(server, teamId, new PacketCasinoRollRarity(x, y, real.getRarity())));
        schedule(server, tPath, () -> sendToTeam(server, teamId, new PacketCasinoRollPath(x, y, real.getCategory())));
        schedule(server, tFinal, () -> {
            String id = real.getId();
            boolean isQuest = id != null && id.startsWith("quest.");
            sendToTeam(server, teamId, new PacketCasinoRollFinal(x, y, real.getId(), real.getName(), real.getCategory(), real.getRarity(), isQuest));
            syncTeamCard(server, teamId);
            rollingSlots.remove(key);
            data.advanceFakeRerollTurn();
            turnDeadlineTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + TURN_TIMEOUT_TICKS;
            advancePastIneligibleTurns(server, data);
            if (isFinished(server, data)) {
                finish(server);
                return;
            }
            broadcastTurn(server, data);
        });
    }

    public static void tick(MinecraftServer server) {
        if (server == null || !active) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isFakeRerollPhaseActive()) return;
        // Do not auto-advance fake reroll turns by timeout.
        // Turn only changes after the current player uses a fake reroll (or is skipped as ineligible).
    }

    public static void finish(MinecraftServer server) {
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isFakeRerollPhaseActive()) {
            active = false;
            return;
        }
        data.endFakeRerollPhase();
        active = false;
        rollingSlots.clear();
        turnDeadlineTick = -1;

        schedule(server, FINISH_DELAY_TICKS, () -> {
            for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                NetworkHandler.send(new PacketCasinoShutdown(), PacketDistributor.PLAYER.with(player));
            }
            for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                BroadcastHelper.syncCard(player);
            }
            BingoGameData live = BingoGameData.get(server);
            live.startCountdownOrFinalize(server);
        });
    }

    private static void syncAllPlayersToFakePhase(MinecraftServer server, BingoGameData data) {
        long end = System.currentTimeMillis() + 1000L;
        int size = Math.max(1, data.currentCard.getSize());
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            NetworkHandler.send(new PacketCasinoStart(end, 0L, size), PacketDistributor.PLAYER.with(player));
            sendCardViewToPlayer(server, data, player);
        }
    }

    private static void sendCardViewToPlayer(MinecraftServer server, BingoGameData data, ServerPlayer player) {
        if (server == null || data == null || player == null || data.currentCard == null) return;
        TeamData teamData = TeamData.get(server);
        UUID teamId = teamData.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
        BingoCard card = teamId == null ? data.currentCard : data.getActiveCardForTeam(teamId);
        if (card == null) card = data.currentCard;
        if (card == null) return;
        int size = card.getSize();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot == null) continue;
                String id = slot.getId();
                boolean isQuest = id != null && id.startsWith("quest.");
                NetworkHandler.send(
                        new PacketCasinoRollFinal(x, y, slot.getId(), slot.getName(), slot.getCategory(), slot.getRarity(), isQuest),
                        PacketDistributor.PLAYER.with(player)
                );
            }
        }
    }

    private static void syncTeamCard(MinecraftServer server, UUID teamId) {
        if (server == null || teamId == null) return;
        TeamData.TeamInfo team = TeamData.get(server).getTeams().stream()
                .filter(t -> t != null && teamId.equals(t.id))
                .findFirst()
                .orElse(null);
        if (team == null) return;
        for (UUID memberId : team.members) {
            ServerPlayer player = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
            if (player != null) {
                BroadcastHelper.syncCard(player);
            }
        }
    }

    private static void sendToTeam(MinecraftServer server, UUID teamId, Object packet) {
        if (server == null || teamId == null || packet == null) return;
        TeamData.TeamInfo team = TeamData.get(server).getTeams().stream()
                .filter(t -> t != null && teamId.equals(t.id))
                .findFirst()
                .orElse(null);
        if (team == null) return;
        for (UUID memberId : team.members) {
            ServerPlayer player = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
            if (player != null) {
                NetworkHandler.send(packet, PacketDistributor.PLAYER.with(player));
            }
        }
    }

    private static void broadcastTurn(MinecraftServer server, BingoGameData data) {
        if (server == null || data == null || !data.isFakeRerollPhaseActive()) return;
        UUID current = data.getCurrentFakeRerollPlayer();
        if (current == null) return;
        ServerPlayer currentPlayer = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, current);
        if (currentPlayer == null) return;
        String name = currentPlayer.getName().getString();
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
            boolean yourTurn = playerId.equals(current);
            NetworkHandler.send(new PacketCasinoRerollTurn(name, yourTurn), PacketDistributor.PLAYER.with(player));
            NetworkHandler.send(new PacketCasinoRerollCount(data.getRemainingFakeRerolls(playerId)), PacketDistributor.PLAYER.with(player));
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

    private static void advancePastIneligibleTurns(MinecraftServer server, BingoGameData data) {
        if (server == null || data == null || !data.isFakeRerollPhaseActive()) return;
        List<UUID> players = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server).stream().map(ServerPlayer::getUUID).toList();
        int loops = players.size() + 1;
        TeamData teams = TeamData.get(server);
        while (loops-- > 0) {
            UUID current = data.getCurrentFakeRerollPlayer();
            if (current == null) return;
            UUID teamId = teams.getTeamForPlayer(current);
            boolean hasRerolls = data.getRemainingFakeRerolls(current) > 0;
            boolean hasSelectable = teamId != null && data.teamHasFakeSelectableSlots(teamId);
            if (hasRerolls && hasSelectable) {
                return;
            }
            data.advanceFakeRerollTurn();
        }
    }

    private static boolean isFinished(MinecraftServer server, BingoGameData data) {
        if (server == null || data == null) return true;
        TeamData teams = TeamData.get(server);
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
            UUID teamId = teams.getTeamForPlayer(playerId);
            if (teamId == null) continue;
            if (data.getRemainingFakeRerolls(playerId) <= 0) continue;
            if (!data.teamHasFakeSelectableSlots(teamId)) continue;
            return false;
        }
        return rollingSlots.isEmpty();
    }

    private static int countActiveTeams(MinecraftServer server, BingoGameData data) {
        if (server == null || data == null) return 0;
        TeamData teams = TeamData.get(server);
        int count = 0;
        for (TeamData.TeamInfo team : teams.getTeams()) {
            if (team == null || team.id == null || team.members == null || team.members.isEmpty()) continue;
            if (data.isTeamEliminated(team.id)) continue;
            boolean hasActiveMember = false;
            for (UUID memberId : team.members) {
                if (memberId == null) continue;
                if (!data.isParticipant(memberId) || data.isSpectator(memberId) || data.isPlayerEliminated(memberId)) {
                    continue;
                }
                hasActiveMember = true;
                break;
            }
            if (hasActiveMember) {
                count++;
                if (count > 1) return count;
            }
        }
        return count;
    }

    private static void reject(ServerPlayer player, int x, int y) {
        NetworkHandler.send(new PacketCasinoRerollReject(x, y), PacketDistributor.PLAYER.with(player));
    }

    private static void schedule(MinecraftServer server, int delayTicks, Runnable run) {
        if (server == null || run == null) return;
        int when = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + Math.max(0, delayTicks);
        CasinoTickScheduler.schedule(server, when, run);
    }

    private record SlotKey(int x, int y) {}
}
