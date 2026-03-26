package com.jamie.jamiebingo.casino;

import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.bingo.WinCondition;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.PacketCasinoEnterRerollPhase;
import com.jamie.jamiebingo.network.PacketCasinoRollFinal;
import com.jamie.jamiebingo.network.PacketCasinoRerollCount;
import com.jamie.jamiebingo.network.PacketCasinoRerollTurn;
import com.jamie.jamiebingo.network.PacketCasinoShutdown;
import com.jamie.jamiebingo.network.PacketCasinoSkipStatus;
import com.jamie.jamiebingo.network.PacketCasinoStart;
import com.jamie.jamiebingo.network.PacketSyncCard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Controls Casino Mode lifecycle on the server.
 */
public class CasinoModeManager {

    private static boolean casinoEnabled = false;
    private static CasinoGenerationSession activeSession;

    /** 🔒 Guard to prevent finishCasino() re-entry */
    private static boolean casinoFinished = false;
    private static long casinoEndTimeMillis = 0L;
    private static long casinoVisualSeed = 0L;
    private static int casinoGridSize = 0;
    private static int casinoFailSafeTick = -1;
    private static int rerollFailSafeTick = -1;
    private static final int CASINO_FAILSAFE_TICKS = 20 * 90;
    private static final int REROLL_FAILSAFE_TICKS = 20 * 60 * 120;

    /* ===============================
       SKIP VOTING STATE
       =============================== */

    private static final Set<UUID> skipVotes = new HashSet<>();
    private static boolean skipTriggered = false;

    /* ===============================
       TOGGLE
       =============================== */

    public static void setCasinoEnabled(boolean enabled) {
        casinoEnabled = enabled;
    }

    public static boolean isCasinoEnabled() {
        return casinoEnabled;
    }

    public static boolean isCasinoInProgress() {
        return activeSession != null || CasinoDraftManager.isDraftActive() || FakeRerollManager.isActive();
    }

    public static void resetTransientState(MinecraftServer server, boolean closeUi) {
        activeSession = null;
        casinoFinished = false;
        casinoEndTimeMillis = 0L;
        casinoVisualSeed = 0L;
        casinoGridSize = 0;
        casinoFailSafeTick = -1;
        rerollFailSafeTick = -1;
        skipVotes.clear();
        skipTriggered = false;
        CasinoDraftManager.forceReset();
        CasinoTickScheduler.clearAll();

        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (data != null && data.isRerollPhaseActive()) {
            data.endRerollPhase();
        }
        if (data != null && data.isFakeRerollPhaseActive()) {
            data.endFakeRerollPhase();
        }
        if (!closeUi) return;

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            NetworkHandler.send(
                    new PacketCasinoShutdown(),
                    PacketDistributor.PLAYER.with(player)
            );
        }
        broadcastSkipStatus(server);
    }

    /* ===============================
       START CASINO
       =============================== */

    public static boolean tryStartCasino(MinecraftServer server) {

        BingoGameData data = BingoGameData.get(server);
        if (!casinoEnabled) return false;
        if (data.casinoMode != BingoGameData.CASINO_ENABLED) return false;
        if (activeSession != null) return true;

        // 🚫 Blind mode must NEVER show casino preview
        if (data.winCondition == WinCondition.BLIND) {
            return false;
        }

        if (data.size <= 0) {
            data.size = 5;
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        }

        data.clearAllPlayerInventories(server);

        casinoFinished = false;
        skipVotes.clear();
        skipTriggered = false;

        System.out.println("[Casino] Preparing casino session");

        activeSession = new CasinoGenerationSession(server, data);
        casinoFailSafeTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + CASINO_FAILSAFE_TICKS;

        long visualSeed = System.nanoTime();
        long endTimeMillis = System.currentTimeMillis() + 10_000L;
        casinoEndTimeMillis = endTimeMillis;
        casinoVisualSeed = visualSeed;
        casinoGridSize = data.size;

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            NetworkHandler.send(
                    new PacketCasinoStart(endTimeMillis, visualSeed, data.size),
                    PacketDistributor.PLAYER.with(player)
            );
        }
        broadcastSkipStatus(server);

        server.schedule(new TickTask(
                com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + 1,
                activeSession::start
        ));

        return true;
    }

    /* ===============================
       SKIP VOTING
       =============================== */

    public static void registerSkipVote(MinecraftServer server, ServerPlayer player) {

        if (activeSession == null || skipTriggered) return;

        UUID id = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
        if (!skipVotes.add(id)) return;

        int totalPlayers = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server).size();
        int votes = skipVotes.size();

        System.out.println("[Casino] Skip vote: " + votes + "/" + totalPlayers);
        broadcastSkipStatus(server);

        if (totalPlayers > 0 && votes * 2 >= totalPlayers) {
            triggerSkip(server);
        }
    }

    public static void startPregamePhasesOrFinalize(MinecraftServer server, BingoGameData data) {
        if (server == null || data == null) return;
        if (data.casinoMode == BingoGameData.CASINO_DRAFT) {
            CasinoDraftManager.start(server);
            return;
        }
        boolean casinoStarted = tryStartCasino(server);
        if (casinoStarted) return;
        if (data.rerollsPerPlayer > 0) {
            CasinoRerollManager.start(server);
        } else if (FakeRerollManager.start(server)) {
            return;
        } else {
            data.startCountdownOrFinalize(server);
        }
    }

    private static void triggerSkip(MinecraftServer server) {
        if (skipTriggered) return;
        skipTriggered = true;

        System.out.println("[Casino] Skip vote PASSED");

        CasinoTickScheduler.clearAll();

        server.schedule(new TickTask(
                com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + 1,
                () -> {
                    if (activeSession != null) {
                        activeSession.forceFinish();
                    }
                }
        ));
    }

    /* ===============================
       FINISH CASINO GENERATION
       =============================== */

    public static void finishCasino(MinecraftServer server) {

        if (casinoFinished) {
            System.out.println("[Casino] finishCasino() ignored (already finished)");
            return;
        }

        casinoFinished = true;
        activeSession = null;
        casinoFailSafeTick = -1;
        casinoEndTimeMillis = 0L;
        casinoVisualSeed = 0L;

        System.out.println("[Casino] Casino generation finished");

        CasinoTickScheduler.clearAll();
        skipVotes.clear();
        skipTriggered = false;
        broadcastSkipStatus(server);

        BingoGameData data = BingoGameData.get(server);

        // 🔑 ALWAYS sync final card first
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            NetworkHandler.send(
                    new PacketSyncCard(data.currentCard, data.winCondition, data.teamChestEnabled),
                    PacketDistributor.PLAYER.with(player)
            );
        }

        // Ensure clients always have a fully-populated casino grid, even on instant skip.
        if (data.currentCard != null) {
            int size = data.currentCard.getSize();
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    BingoSlot slot = data.currentCard.getSlot(x, y);
                    if (slot == null) continue;
                    String id = slot.getId();
                    boolean isQuest = id != null && id.startsWith("quest.");
                    NetworkHandler.send(
                            new PacketCasinoRollFinal(
                                    x,
                                    y,
                                    slot.getId(),
                                    slot.getName(),
                                    slot.getCategory(),
                                    slot.getRarity(),
                                    isQuest
                            ),
                            PacketDistributor.ALL.noArg()
                    );
                }
            }
        }

        if (data.rerollsPerPlayer > 0) {

            System.out.println("[Casino] Entering reroll phase (" +
                    data.rerollsPerPlayer + " per player)");

            server.schedule(new TickTask(
                    com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + 1,
                    () -> CasinoRerollManager.start(server)
            ));

        } else {

            for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                NetworkHandler.send(
                        new PacketCasinoShutdown(),
                        PacketDistributor.PLAYER.with(player)
                );
            }

            server.schedule(new TickTask(
                    com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + 1,
                    () -> data.beginMatchAfterCasino(server)
            ));
        }
    }

    public static void syncPlayerJoin(ServerPlayer player) {
        if (player == null) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (data == null) return;

        if (CasinoDraftManager.isDraftActive()) {
            CasinoDraftManager.syncPlayerJoin(player);
            return;
        }

        if (activeSession != null) {
            long now = System.currentTimeMillis();
            long end = casinoEndTimeMillis > now ? casinoEndTimeMillis : now + 1000L;

            NetworkHandler.send(
                    new PacketCasinoStart(end, casinoVisualSeed, casinoGridSize),
                    PacketDistributor.PLAYER.with(player)
            );
            int totalPlayers = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server).size();
            NetworkHandler.send(
                    new PacketCasinoSkipStatus(skipVotes.size(), totalPlayers),
                    PacketDistributor.PLAYER.with(player)
            );

            for (CasinoGenerationSession.ResolvedVisualSlot slot : activeSession.getResolvedSlots()) {
                if (slot.slot() == null) continue;
                String id = slot.slot().getId();
                boolean isQuest = id != null && id.startsWith("quest.");
                NetworkHandler.send(
                        new PacketCasinoRollFinal(
                                slot.x(),
                                slot.y(),
                                slot.slot().getId(),
                                slot.slot().getName(),
                                slot.slot().getCategory(),
                                slot.slot().getRarity(),
                                isQuest
                        ),
                        PacketDistributor.PLAYER.with(player)
                );
            }
            return;
        }

        if (!data.isRerollPhaseActive()) {
            if (data.isFakeRerollPhaseActive()) {
                FakeRerollManager.syncPlayerJoin(player);
            }
            return;
        }

        long end = System.currentTimeMillis() + 1000L;
        NetworkHandler.send(
                new PacketCasinoStart(end, 0L, data.size),
                PacketDistributor.PLAYER.with(player)
        );

        if (data.currentCard != null) {
            for (int y = 0; y < data.currentCard.getSize(); y++) {
                for (int x = 0; x < data.currentCard.getSize(); x++) {
                    BingoSlot slot = data.currentCard.getSlot(x, y);
                    if (slot == null) continue;
                    String id = slot.getId();
                    boolean isQuest = id != null && id.startsWith("quest.");
                    NetworkHandler.send(
                            new PacketCasinoRollFinal(
                                    x,
                                    y,
                                    slot.getId(),
                                    slot.getName(),
                                    slot.getCategory(),
                                    slot.getRarity(),
                                    isQuest
                            ),
                            PacketDistributor.PLAYER.with(player)
                    );
                }
            }
        }

        NetworkHandler.send(
                new PacketCasinoEnterRerollPhase(data.getRemainingRerolls(com.jamie.jamiebingo.util.EntityUtil.getUUID(player)), false),
                PacketDistributor.PLAYER.with(player)
        );
        NetworkHandler.send(
                new PacketCasinoRerollCount(data.getRemainingRerolls(com.jamie.jamiebingo.util.EntityUtil.getUUID(player))),
                PacketDistributor.PLAYER.with(player)
        );

        UUID current = data.getCurrentRerollPlayer();
        if (current != null) {
            ServerPlayer currentPlayer = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, current);
            if (currentPlayer != null) {
                String name = currentPlayer.getName().getString();
                NetworkHandler.send(
                        new PacketCasinoRerollTurn(name, com.jamie.jamiebingo.util.EntityUtil.getUUID(player).equals(current)),
                        PacketDistributor.PLAYER.with(player)
                );
            }
        }
    }

    public static void armRerollFailSafe(MinecraftServer server) {
        if (server == null) return;
        rerollFailSafeTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + REROLL_FAILSAFE_TICKS;
    }

    public static void bumpRerollFailSafe(MinecraftServer server) {
        armRerollFailSafe(server);
    }

    public static void tickFailSafe(MinecraftServer server) {
        if (server == null) return;
        int now = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        BingoGameData data = BingoGameData.get(server);

        if (activeSession != null && casinoFailSafeTick >= 0 && now >= casinoFailSafeTick) {
            System.out.println("[Casino] FAILSAFE: forcing casino generation completion");
            CasinoGenerationSession session = activeSession;
            if (session != null) {
                session.forceFinish();
            }
            casinoFailSafeTick = now + 20 * 15;
        }

        if (data != null && data.isRerollPhaseActive()) {
            if (rerollFailSafeTick < 0) {
                rerollFailSafeTick = now + REROLL_FAILSAFE_TICKS;
            } else if (now >= rerollFailSafeTick) {
                System.out.println("[Casino] FAILSAFE: forcing reroll phase completion");
                CasinoRerollManager.finish(server);
                rerollFailSafeTick = -1;
            }
        } else if (data != null && data.isFakeRerollPhaseActive()) {
            rerollFailSafeTick = -1;
            FakeRerollManager.tick(server);
        } else {
            rerollFailSafeTick = -1;
        }
    }

    private static void broadcastSkipStatus(MinecraftServer server) {
        if (server == null) return;
        int totalPlayers = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server).size();
        NetworkHandler.send(
                new PacketCasinoSkipStatus(skipVotes.size(), totalPlayers),
                PacketDistributor.ALL.noArg()
        );
    }
}






