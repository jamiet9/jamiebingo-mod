package com.jamie.jamiebingo.data;

import com.jamie.jamiebingo.JamieBingo;
import com.jamie.jamiebingo.bingo.BingoWinEvaluator;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.PacketPlayTeamSound;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID)
public final class GameTimerTicker {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private static int lastBroadcastSeconds = -1;
    private static boolean lastBroadcastCountdown = false;
    private static int lastCountdownWarningSeconds = -1;
    private static int lastFinalSecond = -1;
    private static final java.util.Map<java.util.UUID, Integer> lastRushWarningByTeam = new java.util.HashMap<>();

    private GameTimerTicker() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        com.jamie.jamiebingo.util.ServerTickUtil.tick(server);

        BingoGameData data = BingoGameData.get(server);
        if (data != null && data.isActive() && data.startCountdownActive && data.startCountdownEndTick >= 0) {
            int now = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
            int remaining = Math.max(0, (data.startCountdownEndTick - now) / 20);

            if (now % 20 == 0) {
                Component msg = com.jamie.jamiebingo.util.ComponentUtil.literal("Game starts in " + remaining + "s")
                        .withStyle(ChatFormatting.GOLD);
                for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                    player.displayClientMessage(msg, true);
                }
            }

            if (now >= data.startCountdownEndTick) {
                // Fallback: finalize start even if a scheduled countdown task was cleared.
                LOGGER.info("[JamieBingo] GameTimerTicker fallback finalize firing: now={} endTick={}", now, data.startCountdownEndTick);
                data.finalizeGameStart(server);
            }
        }
        if (data == null
                || !data.isActive()
                || data.startCountdownActive) {
            if (lastBroadcastSeconds != -1) {
                lastBroadcastSeconds = -1;
                lastBroadcastCountdown = false;
                lastCountdownWarningSeconds = -1;
                lastFinalSecond = -1;
                lastRushWarningByTeam.clear();
            }
            return;
        }

        data.restoreTimerBaselineIfNeeded(server);
        boolean preMatchHold = data.gameStartTick < 0
                && (data.pregameBoxActive
                || data.isRerollPhaseActive()
                || com.jamie.jamiebingo.casino.CasinoModeManager.isCasinoInProgress());
        if (preMatchHold) {
            if (lastBroadcastSeconds != -1) {
                lastBroadcastSeconds = -1;
                lastBroadcastCountdown = false;
                lastCountdownWarningSeconds = -1;
                lastFinalSecond = -1;
                lastRushWarningByTeam.clear();
            }
            return;
        }
        if (data.gameStartTick < 0) {
            if (lastBroadcastSeconds != -1) {
                lastBroadcastSeconds = -1;
                lastBroadcastCountdown = false;
                lastCountdownWarningSeconds = -1;
                lastFinalSecond = -1;
                lastRushWarningByTeam.clear();
            }
            return;
        }

        if (data.pendingWinEndActive) {
            if (lastBroadcastSeconds != -1) {
                lastBroadcastSeconds = -1;
                lastBroadcastCountdown = false;
                lastCountdownWarningSeconds = -1;
                lastFinalSecond = -1;
                lastRushWarningByTeam.clear();
            }
            if (com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) >= data.pendingWinEndTick) {
                data.stopGameAfterWin(server);
                data.clearPendingWinEnd();
            }
            return;
        }

        int currentTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);

        boolean countdown = data.countdownEnabled;
        int seconds;

        if (countdown) {
            if (data.countdownEndTick < 0) {
                data.countdownEndTick = data.gameStartTick + data.countdownMinutes * 60 * 20;
            }
            seconds = Math.max(0, (data.countdownEndTick - currentTick) / 20);

            if (seconds != lastBroadcastSeconds) {
                if (seconds == 600 || seconds == 300) {
                    broadcastCountdownWarning(server, seconds);
                    lastCountdownWarningSeconds = seconds;
                }
                if (seconds <= 60) {
                    broadcastFinalMinute(server, seconds);
                    lastFinalSecond = seconds;
                } else {
                    lastFinalSecond = -1;
                }
            }

            if (seconds <= 0 && !data.countdownExpired) {
                data.countdownExpired = true;
                BingoWinEvaluator.forcePointsWin(server);
            }
        } else {
            seconds = Math.max(0, (currentTick - data.gameStartTick) / 20);
            lastCountdownWarningSeconds = -1;
            lastFinalSecond = -1;
        }

        data.updateResumeTimerSnapshot(countdown, seconds);

        tickRushOnly(server);

        if (currentTick % 20 == 0 || seconds != lastBroadcastSeconds || countdown != lastBroadcastCountdown) {
            for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                BroadcastHelper.syncGameTimer(player);
            }
            lastBroadcastSeconds = seconds;
            lastBroadcastCountdown = countdown;
        }
    }

    public static void tickRushOnly(MinecraftServer server) {
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isActive() || !data.rushEnabled) {
            lastRushWarningByTeam.clear();
            return;
        }
        data.restoreRushDeadlinesIfNeeded(server);
        int currentTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        processRush(server, data, currentTick);
        data.updateRushResumeSnapshot(server);
    }

    private static void processRush(MinecraftServer server, BingoGameData data, int currentTick) {
        if (server == null || data == null || !data.rushEnabled) {
            lastRushWarningByTeam.clear();
            return;
        }

        TeamData teamData = TeamData.get(server);
        java.util.List<ServerPlayer> onlinePlayers = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server);

        // Singleplayer hard-path: drive rush purely from whichever deadline exists to survive team-id remap drift on rejoin.
        if (onlinePlayers.size() == 1) {
            ServerPlayer lonePlayer = onlinePlayers.get(0);
            java.util.UUID lonePlayerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(lonePlayer);
            java.util.UUID loneTeamId = teamData.getTeamForPlayer(lonePlayerId);

            java.util.UUID selectedTeamId = null;
            Integer selectedDeadline = null;
            if (loneTeamId != null) {
                Integer ownDeadline = data.getRushDeadlineTickForTeam(loneTeamId);
                if (ownDeadline != null) {
                    selectedTeamId = loneTeamId;
                    selectedDeadline = ownDeadline;
                }
            }
            if (selectedDeadline == null) {
                int bestDeadline = Integer.MIN_VALUE;
                for (java.util.UUID teamId : data.getRushDeadlineTeamsSnapshot()) {
                    if (teamId == null) continue;
                    Integer deadline = data.getRushDeadlineTickForTeam(teamId);
                    if (deadline == null) continue;
                    if (deadline > bestDeadline) {
                        bestDeadline = deadline;
                        selectedTeamId = teamId;
                        selectedDeadline = deadline;
                    }
                }
            }
            if (selectedDeadline == null) {
                lastRushWarningByTeam.clear();
                return;
            }

            java.util.UUID warningKey = selectedTeamId != null ? selectedTeamId : lonePlayerId;
            int seconds = Math.max(0, (selectedDeadline - currentTick) / 20);
            int prev = lastRushWarningByTeam.getOrDefault(warningKey, -1);
            if (seconds <= 10 && seconds > 0 && seconds != prev) {
                NetworkHandler.send(
                        new PacketPlayTeamSound("minecraft:block.note_block.bell", 1.0f, 0.9f),
                        PacketDistributor.PLAYER.with(lonePlayer)
                );
                lonePlayer.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 0.8f);
            }
            lastRushWarningByTeam.put(warningKey, seconds);

            if (currentTick >= selectedDeadline) {
                java.util.UUID eliminationTeam = loneTeamId != null ? loneTeamId : selectedTeamId;
                if (eliminationTeam != null) {
                    data.eliminateTeamForRush(server, eliminationTeam);
                }
            }
            return;
        }

        java.util.List<java.util.UUID> toEliminate = new java.util.ArrayList<>();
        java.util.Set<java.util.UUID> teamsToProcess = new java.util.HashSet<>();
        for (TeamData.TeamInfo team : teamData.getTeams()) {
            if (team == null || team.id == null) continue;
            teamsToProcess.add(team.id);
        }
        teamsToProcess.addAll(data.getRushDeadlineTeamsSnapshot());

        for (java.util.UUID teamId : teamsToProcess) {
            if (teamId == null) continue;
            if (data.isTeamEliminated(teamId)) continue;
            Integer deadline = data.getRushDeadlineTickForTeam(teamId);
            if (deadline == null) continue;
            java.util.List<java.util.UUID> members = resolveRushMembers(server, teamData, teamId);
            if (members.isEmpty() && onlinePlayers.size() == 1) {
                members.add(com.jamie.jamiebingo.util.EntityUtil.getUUID(onlinePlayers.get(0)));
            }
            if (members.isEmpty()) continue;
            int seconds = Math.max(0, (deadline - currentTick) / 20);
            int prev = lastRushWarningByTeam.getOrDefault(teamId, -1);
            if (seconds <= 10 && seconds > 0 && seconds != prev) {
                for (java.util.UUID memberId : members) {
                    ServerPlayer player = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
                    if (player != null) {
                        NetworkHandler.send(
                                new PacketPlayTeamSound("minecraft:block.note_block.bell", 1.0f, 0.9f),
                                PacketDistributor.PLAYER.with(player)
                        );
                        player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 0.8f);
                    }
                }
            }
            lastRushWarningByTeam.put(teamId, seconds);
            if (currentTick >= deadline) {
                toEliminate.add(teamId);
            }
        }

        if (!toEliminate.isEmpty()) {
            for (java.util.UUID teamId : toEliminate) {
                data.eliminateTeamForRush(server, teamId);
            }
        }
    }

    private static java.util.List<java.util.UUID> resolveRushMembers(
            MinecraftServer server,
            TeamData teamData,
            java.util.UUID teamId
    ) {
        java.util.List<java.util.UUID> out = new java.util.ArrayList<>();
        if (server == null || teamData == null || teamId == null) return out;

        TeamData.TeamInfo info = teamData.getTeams().stream()
                .filter(t -> t != null && teamId.equals(t.id))
                .findFirst()
                .orElse(null);
        if (info != null && !info.members.isEmpty()) {
            out.addAll(info.members);
            return out;
        }

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            if (player == null) continue;
            java.util.UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
            java.util.UUID mappedTeam = teamData.getTeamForPlayer(playerId);
            if (teamId.equals(mappedTeam)) {
                out.add(playerId);
            }
        }
        return out;
    }

    private static void broadcastCountdownWarning(MinecraftServer server, int seconds) {
        if (server == null) return;

        String label = seconds == 600 ? "10 minutes remaining!" : "5 minutes remaining!";
        BroadcastHelper.broadcast(server, com.jamie.jamiebingo.util.ComponentUtil.literal(label).withStyle(ChatFormatting.GOLD));

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            player.playSound(SoundEvents.UI_TOAST_IN, 1.0f, 1.0f);
        }
    }

    private static void broadcastFinalMinute(MinecraftServer server, int seconds) {
        if (server == null) return;

        int clamped = Math.max(0, seconds);
        String text = "Time Left: " + clamped + "s";
        Component msg = com.jamie.jamiebingo.util.ComponentUtil.literal(text).withStyle(ChatFormatting.RED);

        float basePitch = 0.7f + (60 - clamped) * 0.02f;
        float pitch = Math.min(2.0f, Math.max(0.6f, basePitch));
        boolean lastTen = clamped <= 10;

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            player.displayClientMessage(msg, true);
            player.playSound(
                    lastTen ? SoundEvents.NOTE_BLOCK_BELL.value() : SoundEvents.NOTE_BLOCK_PLING.value(),
                    lastTen ? 1.0f : 0.7f,
                    pitch
            );
        }
    }
}






