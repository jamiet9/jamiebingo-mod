package com.jamie.jamiebingo.casino;

import com.jamie.jamiebingo.bingo.*;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.network.*;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

/**
 * Server-side orchestrator for Casino Mode generation.
 *
 * GUARANTEES:
 * - Slot timing is deterministic
 * - Card is constructed from casino results
 * - Casino duration never exceeds 60 seconds
 * - Safe early skip supported
 */
public class CasinoGenerationSession {

    /* ===============================
       TIMING CONSTANTS
       =============================== */

    private static final int CLIENT_SCREEN_GRACE = 40; // 2s
    private static final int MAX_TOTAL_TICKS = 20 * 60; // 60 seconds

    private static final int BASE_SLOT_DURATION = 60;
    private static final int BASE_RARITY = 10;
    private static final int BASE_PATH   = 30;
    private static final int BASE_FINAL  = 50;

    /* ===============================
       STATE
       =============================== */

    private final MinecraftServer server;
    private final int size;
    private final String difficulty;
    private final CardComposition composition;
    private final int questPercent;
    private final Set<String> blacklistedIds;
    private final boolean separateItemColors;
    private final BingoGameData data;

    private final List<SlotPos> slotOrder;
    private final Set<String> usedKeys = new HashSet<>();
    private final Map<String, Integer> categoryCounts = new HashMap<>();

    private final BingoSlot[][] resolvedGrid;

    private final int slotDuration;
    private final int ticksRarity;
    private final int ticksPath;
    private final int ticksFinal;

    private boolean started = false;
    private boolean finished = false;

    public CasinoGenerationSession(MinecraftServer server, BingoGameData data) {
        this.server = server;
        this.data = data;
        this.size = Math.max(1, data.size);
        this.difficulty = data.difficulty;
        this.composition = data.composition;
        this.questPercent = data.questPercent;
        this.blacklistedIds = data == null ? Collections.emptySet() : data.getBlacklistedSlotIds();
        this.separateItemColors = data != null && data.itemColorVariantsSeparate;

        this.slotOrder = buildSlotOrder(size);
        this.resolvedGrid = new BingoSlot[size][size];

        int totalSlots = size * size;
        int idealTotal = CLIENT_SCREEN_GRACE + totalSlots * BASE_SLOT_DURATION;

        double scale = idealTotal <= MAX_TOTAL_TICKS
                ? 1.0
                : (double) MAX_TOTAL_TICKS / idealTotal;

        this.slotDuration = Math.max(10, (int) (BASE_SLOT_DURATION * scale));
        this.ticksRarity  = Math.max(2,  (int) (BASE_RARITY * scale));
        this.ticksPath    = Math.max(4,  (int) (BASE_PATH * scale));
        this.ticksFinal   = Math.max(6,  (int) (BASE_FINAL * scale));

        System.out.println(
                "[Casino] Timing scale=" + String.format("%.2f", scale) +
                " slot=" + slotDuration +
                " total≈" + (CLIENT_SCREEN_GRACE + slotDuration * totalSlots)
        );
    }

    /* ===============================
       START
       =============================== */

    public void start() {
        if (started) return;
        started = true;

        usedKeys.clear();

        int baseTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + CLIENT_SCREEN_GRACE;

        for (int i = 0; i < slotOrder.size(); i++) {
            SlotPos pos = slotOrder.get(i);

            ResolvedSlot slot = resolveSlot();
            usedKeys.add(ConfigurableCardGenerator.uniqueKeyForSlot(slot, separateItemColors));
            incrementCategory(slot.category);

            resolvedGrid[pos.y][pos.x] = new BingoSlot(
                    slot.id,
                    slot.name,
                    slot.category,
                    slot.rarity
            );

            int slotStartTick = baseTick + (i * slotDuration);
            scheduleSlot(pos, slot, slotStartTick);
        }

        int finishTick = baseTick + (slotOrder.size() * slotDuration) + 20;
        CasinoTickScheduler.schedule(server, finishTick, this::finishCasino);
    }

    /* ===============================
       FORCE SKIP (MAJORITY VOTE)
       =============================== */

    public void forceFinishEarly() {
        if (finished) return;

        System.out.println("[Casino] FORCE FINISH EARLY");

        for (SlotPos pos : slotOrder) {
            if (resolvedGrid[pos.y][pos.x] == null) {
                ResolvedSlot slot = resolveSlot();
                usedKeys.add(ConfigurableCardGenerator.uniqueKeyForSlot(slot, separateItemColors));
                incrementCategory(slot.category);

                resolvedGrid[pos.y][pos.x] = new BingoSlot(
                        slot.id,
                        slot.name,
                        slot.category,
                        slot.rarity
                );
            }
        }

        finishCasino();
    }

    /**
     * Compatibility wrapper for CasinoModeManager
     */
    public void forceFinish() {
        forceFinishEarly();
    }

    /* ===============================
       FINISH
       =============================== */

    private void finishCasino() {
        if (finished) return;
        finished = true;

        System.out.println("[Casino] FINISH RUN @tick " + com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server));

        BingoGameData data = BingoGameData.get(server);

        BingoCard card = new BingoCard(size);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                card.setSlot(x, y, resolvedGrid[y][x]);
            }
        }

        data.currentCard = card;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

        NetworkHandler.send(
                new PacketSyncCard(card, data.winCondition, data.teamChestEnabled),
                PacketDistributor.ALL.noArg()
        );

        CasinoModeManager.finishCasino(server);
    }

    /* ===============================
       SLOT SCHEDULING
       =============================== */

    private void scheduleSlot(SlotPos pos, ResolvedSlot slot, int startTick) {
        int x = pos.x;
        int y = pos.y;

        CasinoTickScheduler.schedule(server, startTick,
                () -> broadcast(new PacketCasinoSlotStart(x, y)));

        CasinoTickScheduler.schedule(server, startTick + ticksRarity,
                () -> broadcast(new PacketCasinoRollRarity(x, y, slot.rarity)));

        CasinoTickScheduler.schedule(server, startTick + ticksPath,
                () -> broadcast(new PacketCasinoRollPath(
                        x, y,
                        slot.isQuest ? "QUEST" : slot.category)));

        CasinoTickScheduler.schedule(server, startTick + ticksFinal,
                () -> broadcast(new PacketCasinoRollFinal(
                        x, y,
                        slot.id,
                        slot.name,
                        slot.category,
                        slot.rarity,
                        slot.isQuest)));
    }

    /* ===============================
       SLOT RESOLUTION
       =============================== */

    private ResolvedSlot resolveSlot() {
        return ConfigurableCardGenerator.rollResolvedSlot(
                data,
                server,
                usedKeys,
                categoryCounts,
                difficulty,
                composition,
                questPercent,
                blacklistedIds
        );
    }

    /* ===============================
       HELPERS
       =============================== */

    private void broadcast(Object packet) {
        NetworkHandler.send(
                packet,
                PacketDistributor.ALL.noArg()
        );
    }

    private static List<SlotPos> buildSlotOrder(int size) {
        List<SlotPos> list = new ArrayList<>();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                list.add(new SlotPos(x, y));
            }
        }
        return list;
    }

    private void incrementCategory(String category) {
        if (category == null || category.isBlank()) return;
        categoryCounts.merge(category, 1, Integer::sum);
    }

    public record SlotPos(int x, int y) {}

    public List<ResolvedVisualSlot> getResolvedSlots() {
        List<ResolvedVisualSlot> out = new ArrayList<>();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BingoSlot slot = resolvedGrid[y][x];
                if (slot != null) {
                    out.add(new ResolvedVisualSlot(x, y, slot));
                }
            }
        }
        return out;
    }

    public record ResolvedVisualSlot(int x, int y, BingoSlot slot) {}

    /* ===============================
       QUEST SNAPSHOT
       =============================== */
}


