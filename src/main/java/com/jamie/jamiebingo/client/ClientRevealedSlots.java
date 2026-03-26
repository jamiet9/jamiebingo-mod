package com.jamie.jamiebingo.client;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ClientRevealedSlots {

    /**
     * Slots revealed for the LOCAL PLAYER'S TEAM in BLIND mode.
     */
    private static final Set<String> revealed = new HashSet<>();

    /**
     * Called by PacketSyncRevealedSlots.
     * This must NOT be allowed to desync with card sync.
     */
    public static void set(Set<String> newSet) {
        revealed.clear();

        if (newSet != null && !newSet.isEmpty()) {
            revealed.addAll(newSet);
            return;
        }

        // 🔑 SAFETY NET:
        // If the server sends nothing yet (race condition),
        // we still allow the game to start by revealing top-left.
        revealTopLeftIfPossible();
    }

    /**
     * Used by BingoCardScreen to decide if a slot is visible.
     */
    public static boolean isRevealed(String slotId) {
        return revealed.contains(slotId);
    }

    /**
     * Exposed for debugging / overlays.
     */
    public static Set<String> getAll() {
        return Collections.unmodifiableSet(revealed);
    }

    /**
     * Called when the client explicitly resets game state.
     * SHOULD NOT be called during normal card sync.
     */
    public static void clear() {
        revealed.clear();
    }

    /**
     * Ensures BLIND mode is never soft-locked.
     * Reveals the top-left slot (0,0) if a card exists.
     */
    private static void revealTopLeftIfPossible() {
        if (!ClientCardData.hasCard()) return;

        var card = ClientCardData.getCard();
        if (card == null || card.getSize() <= 0) return;

        var slot = card.getSlot(0, 0);
        if (slot != null) {
            revealed.add(slot.getId());
        }
    }
}
