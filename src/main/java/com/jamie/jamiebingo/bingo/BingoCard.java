package com.jamie.jamiebingo.bingo;

import java.util.HashSet;
import java.util.Set;

public class BingoCard {

    private final int size;
    private final BingoSlot[][] slots;

    public BingoCard(int size) {
        this.size = size;
        this.slots = new BingoSlot[size][size];
    }

    public int getSize() {
        return size;
    }

    public void setSlot(int x, int y, BingoSlot slot) {
        slots[y][x] = slot;
    }

    public BingoSlot getSlot(int x, int y) {
        return slots[y][x];
    }

    public BingoSlot[][] getSlots() {
        return slots;
    }

    /**
     * Returns all slot IDs currently present on the card.
     * Used by casino reroll logic to avoid duplicates.
     *
     * Safe, read-only helper.
     */
    public Set<String> getAllIds() {
        Set<String> ids = new HashSet<>();

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BingoSlot slot = getSlot(x, y);
                if (slot != null) {
                    ids.add(slot.getId());
                }
            }
        }

        return ids;
    }
}
