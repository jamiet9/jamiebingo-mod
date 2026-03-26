package com.jamie.jamiebingo.client.casino;

import net.minecraft.client.Minecraft;
import com.jamie.jamiebingo.client.ClientMinecraftUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/**
 * Pure client-side casino animation state.
 * Mirrors server state for visuals only.
 */
public class ClientCasinoState {

    private static boolean active = false;
    private static boolean finishing = false;
    private static boolean rerollPhase = false;
    private static boolean fakeRerollPhase = false;
    private static boolean draftPhase = false;

    private static int gridSize = 0;

    private static SlotPos currentSlot;
    private static String currentRarity;
    private static String currentPath;

    private static final Map<SlotPos, VisualSlot> resolvedSlots = new HashMap<>();

    private static int rerollsRemaining = 0;
    private static String rerollPlayerName = null;
    private static String draftPlayerName = null;
    private static boolean yourTurn = false;
    private static SlotPos lastRerolledSlot = null;
    private static long lastRerolledTick = 0;
    private static int lastSpecialRerolledMineIndex = -1;
    private static boolean lastSpecialRerolledDefuse = false;
    private static long lastSpecialRerolledTick = 0;
    private static SlotPos lastDraftPlacedSlot = null;
    private static long lastDraftPlacedTick = 0;
    private static int skipVotes = 0;
    private static int skipTotal = 0;
    private static boolean localSkipVoted = false;
    private static final List<VisualSlot> draftChoices = new ArrayList<>();
    private static int selectedDraftChoice = 0;

    private static final List<Item> previewItems = new ArrayList<>();
    private static int previewIndex = 0;
    private static long lastPreviewTick = 0;

    private static final int PREVIEW_INTERVAL = 2;
    private static final int SHUFFLE_INTERVAL = 40;

    public static void start(int size) {
        active = true;
        finishing = false;
        rerollPhase = false;
        fakeRerollPhase = false;
        draftPhase = false;

        gridSize = Math.max(1, size);

        rerollsRemaining = 0;
        rerollPlayerName = null;
        draftPlayerName = null;
        yourTurn = false;

        currentSlot = null;
        currentRarity = null;
        currentPath = null;
        lastRerolledSlot = null;
        lastRerolledTick = 0;
        lastSpecialRerolledMineIndex = -1;
        lastSpecialRerolledDefuse = false;
        lastSpecialRerolledTick = 0;
        lastDraftPlacedSlot = null;
        lastDraftPlacedTick = 0;
        skipVotes = 0;
        skipTotal = 0;
        localSkipVoted = false;

        resolvedSlots.clear();
        draftChoices.clear();
        selectedDraftChoice = 0;

        rebuildPreviewPool();
        previewIndex = 0;
        lastPreviewTick = 0;
    }

    public static void end() {
        active = false;
        finishing = false;
        rerollPhase = false;
        fakeRerollPhase = false;
        draftPhase = false;

        gridSize = 0;

        rerollsRemaining = 0;
        rerollPlayerName = null;
        draftPlayerName = null;
        yourTurn = false;

        currentSlot = null;
        currentRarity = null;
        currentPath = null;

        resolvedSlots.clear();
        draftChoices.clear();
        selectedDraftChoice = 0;

        lastRerolledSlot = null;
        lastRerolledTick = 0;
        lastSpecialRerolledMineIndex = -1;
        lastSpecialRerolledDefuse = false;
        lastSpecialRerolledTick = 0;
        lastDraftPlacedSlot = null;
        lastDraftPlacedTick = 0;
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isFinishing() {
        return finishing;
    }

    public static void beginFinishing() {
        finishing = true;
        rerollPhase = false;
        draftPhase = false;

        currentSlot = null;
        currentRarity = null;
        currentPath = null;
    }

    public static void beginRerollPhase(int rerolls) {
        beginRerollPhase(rerolls, false);
    }

    public static void beginRerollPhase(int rerolls, boolean isFakePhase) {
        rerollPhase = true;
        fakeRerollPhase = isFakePhase;
        draftPhase = false;
        finishing = false;

        rerollsRemaining = Math.max(0, rerolls);

        currentSlot = null;
        currentRarity = null;
        currentPath = null;
    }

    public static void beginDraftPhase() {
        draftPhase = true;
        rerollPhase = false;
        fakeRerollPhase = false;
        finishing = false;

        rerollPlayerName = null;
        draftPlayerName = null;
        yourTurn = false;

        currentSlot = null;
        currentRarity = null;
        currentPath = null;
        lastDraftPlacedSlot = null;
        lastDraftPlacedTick = 0;

        draftChoices.clear();
        selectedDraftChoice = 0;
    }

    public static void endRerollPhase() {
        rerollPhase = false;
        fakeRerollPhase = false;
        draftPhase = false;
        rerollsRemaining = 0;
        rerollPlayerName = null;
        draftPlayerName = null;
        yourTurn = false;
        draftChoices.clear();
        selectedDraftChoice = 0;
        lastDraftPlacedSlot = null;
        lastDraftPlacedTick = 0;
    }

    public static boolean isRerollPhase() {
        return rerollPhase;
    }

    public static boolean isFakeRerollPhase() {
        return rerollPhase && fakeRerollPhase;
    }

    public static boolean isDraftPhase() {
        return draftPhase;
    }

    public static void setRerollTurn(String playerName, boolean isYourTurn) {
        rerollPlayerName = playerName;
        draftPlayerName = null;
        yourTurn = isYourTurn;
    }

    public static void setDraftTurn(String playerName, boolean isYourTurn) {
        draftPlayerName = playerName;
        rerollPlayerName = null;
        yourTurn = isYourTurn;
    }

    public static boolean isLocalPlayersTurn() {
        return yourTurn;
    }

    public static String getCurrentRerollPlayerName() {
        return rerollPlayerName;
    }

    public static String getCurrentDraftPlayerName() {
        return draftPlayerName;
    }

    public static void setRerollsRemaining(int value) {
        rerollsRemaining = Math.max(0, value);
    }

    public static int getRerollsRemaining() {
        return rerollsRemaining;
    }

    public static void setSkipVoteStatus(int votes, int total) {
        skipVotes = Math.max(0, votes);
        skipTotal = Math.max(0, total);
    }

    public static int getSkipVotes() {
        return skipVotes;
    }

    public static int getSkipTotal() {
        return skipTotal;
    }

    public static boolean hasLocalSkipVoted() {
        return localSkipVoted;
    }

    public static void setLocalSkipVoted(boolean voted) {
        localSkipVoted = voted;
    }

    public static SlotPos getLastRerolledSlot() {
        return lastRerolledSlot;
    }

    public static long getLastRerolledTick() {
        return lastRerolledTick;
    }

    public static void setDraftChoices(List<VisualSlot> choices) {
        draftChoices.clear();
        if (choices != null) {
            for (VisualSlot slot : choices) {
                if (slot != null) {
                    draftChoices.add(slot);
                }
            }
        }
        if (selectedDraftChoice >= draftChoices.size()) {
            selectedDraftChoice = 0;
        }
    }

    public static List<VisualSlot> getDraftChoices() {
        return List.copyOf(draftChoices);
    }

    public static void setSelectedDraftChoice(int index) {
        if (index < 0 || index >= draftChoices.size()) return;
        selectedDraftChoice = index;
    }

    public static int getSelectedDraftChoice() {
        return selectedDraftChoice;
    }

    private static boolean isValidSlot(int x, int y) {
        return gridSize > 0 &&
               x >= 0 && y >= 0 &&
               x < gridSize && y < gridSize;
    }

    public static void onSlotStart(int x, int y) {
        if (x == -1001) {
            Minecraft mc = ClientMinecraftUtil.getMinecraft();
            var level = ClientMinecraftUtil.getLevel(mc);
            lastSpecialRerolledMineIndex = Math.max(0, y);
            lastSpecialRerolledDefuse = false;
            lastSpecialRerolledTick = level != null ? level.getGameTime() : 0;
            return;
        }
        if (x == -1002) {
            Minecraft mc = ClientMinecraftUtil.getMinecraft();
            var level = ClientMinecraftUtil.getLevel(mc);
            lastSpecialRerolledMineIndex = -1;
            lastSpecialRerolledDefuse = true;
            lastSpecialRerolledTick = level != null ? level.getGameTime() : 0;
            return;
        }
        if (!isValidSlot(x, y)) return;

        currentSlot = new SlotPos(x, y);
        currentRarity = null;
        currentPath = null;
        previewIndex = 0;
        lastPreviewTick = 0;
    }

    public static void onRarityRolled(int x, int y, String rarity) {
        if (!isValidSlot(x, y)) return;
        if (!matchesCurrent(x, y)) return;

        currentRarity = rarity;
    }

    public static void onPathRolled(int x, int y, String value) {
        if (!isValidSlot(x, y)) return;
        if (!matchesCurrent(x, y)) return;

        currentPath = value;
    }

    public static void onFinalRolled(
            int x, int y,
            String id,
            String name,
            String category,
            String rarity,
            boolean isQuest
    ) {
        if (!isValidSlot(x, y)) return;

        resolvedSlots.put(
                new SlotPos(x, y),
                new VisualSlot(id, name, category, rarity, isQuest)
        );

        currentSlot = null;
        currentRarity = null;
        currentPath = null;

        if (rerollPhase) {
            lastRerolledSlot = new SlotPos(x, y);
            Minecraft mc = ClientMinecraftUtil.getMinecraft();
            var level = ClientMinecraftUtil.getLevel(mc);
            lastRerolledTick = level != null ? level.getGameTime() : 0;
        } else if (draftPhase) {
            lastDraftPlacedSlot = new SlotPos(x, y);
            Minecraft mc = ClientMinecraftUtil.getMinecraft();
            var level = ClientMinecraftUtil.getLevel(mc);
            lastDraftPlacedTick = level != null ? level.getGameTime() : 0;
        }

        if (!rerollPhase && !draftPhase && resolvedSlots.size() == gridSize * gridSize) {
            finishing = true;
        }
    }

    private static boolean matchesCurrent(int x, int y) {
        return currentSlot != null &&
                currentSlot.x == x &&
                currentSlot.y == y;
    }

    public static void cancelRollingSlot(int x, int y) {
        if (currentSlot == null) return;

        if (currentSlot.x == x && currentSlot.y == y) {
            currentSlot = null;
            currentRarity = null;
            currentPath = null;
            previewIndex = 0;
            lastPreviewTick = 0;
        }
    }

    private static void rebuildPreviewPool() {
        previewItems.clear();

        for (Item item : ForgeRegistries.ITEMS) {
            if (item != null) {
                ItemStack stack = new ItemStack(item);
                if (!stack.isEmpty()) {
                    previewItems.add(item);
                }
            }
        }

        if (previewItems.isEmpty()) {
            previewItems.add(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond"));
        }

        Collections.shuffle(previewItems);
    }

    public static ItemStack getRollingPreviewStack() {
        if (!active || currentSlot == null || previewItems.isEmpty()) {
            return com.jamie.jamiebingo.util.ItemStackUtil.empty();
        }

        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        var level = ClientMinecraftUtil.getLevel(mc);
        if (level == null) return com.jamie.jamiebingo.util.ItemStackUtil.empty();

        long gameTime = level.getGameTime();

        if (gameTime - lastPreviewTick >= PREVIEW_INTERVAL) {
            previewIndex = (previewIndex + 1) % previewItems.size();
            lastPreviewTick = gameTime;
        }

        if (gameTime % SHUFFLE_INTERVAL == 0) {
            Collections.shuffle(previewItems);
        }

        return new ItemStack(previewItems.get(previewIndex));
    }

    public static int getGridSize() {
        return gridSize;
    }

    public static SlotPos getCurrentSlot() {
        return currentSlot;
    }

    public static Map<SlotPos, VisualSlot> getResolvedSlots() {
        return resolvedSlots;
    }

    public static SlotPos getLastDraftPlacedSlot() {
        return lastDraftPlacedSlot;
    }

    public static long getLastDraftPlacedTick() {
        return lastDraftPlacedTick;
    }

    public static int getLastSpecialRerolledMineIndex() {
        return lastSpecialRerolledMineIndex;
    }

    public static boolean isLastSpecialRerolledDefuse() {
        return lastSpecialRerolledDefuse;
    }

    public static long getLastSpecialRerolledTick() {
        return lastSpecialRerolledTick;
    }

    public record SlotPos(int x, int y) {}

    public record VisualSlot(
            String id,
            String name,
            String category,
            String rarity,
            boolean isQuest
    ) {}
}
