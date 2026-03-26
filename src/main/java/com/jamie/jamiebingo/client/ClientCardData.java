package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.bingo.BingoCard;
import com.jamie.jamiebingo.bingo.BingoSlot;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ClientCardData {

    private static BingoCard currentCard;
    private static Set<Integer> fakeGreenSlotIndices = Set.of();
    private static Set<Integer> fakeRedSlotIndices = Set.of();

    public static void setCard(BingoCard card) {
        setCard(card, Set.of(), Set.of());
    }

    public static void setCard(BingoCard card, Set<Integer> fakeGreenIndices, Set<Integer> fakeRedIndices) {
        Set<String> changed = changedSlotIds(currentCard, card);
        currentCard = card;
        setFakeMarkers(fakeGreenIndices, fakeRedIndices);
        if (!changed.isEmpty()) {
            ClientRerollAnimation.animate(changed, 24);
        }
    }

    public static void setFakeMarkers(Set<Integer> fakeGreenIndices, Set<Integer> fakeRedIndices) {
        fakeGreenSlotIndices = fakeGreenIndices == null
                ? Set.of()
                : Collections.unmodifiableSet(new HashSet<>(fakeGreenIndices));
        Set<Integer> red = fakeRedIndices == null
                ? Set.of()
                : new HashSet<>(fakeRedIndices);
        if (!fakeGreenSlotIndices.isEmpty() && !red.isEmpty()) {
            red.removeAll(fakeGreenSlotIndices);
        }
        fakeRedSlotIndices = Collections.unmodifiableSet(new HashSet<>(red));
    }

    public static BingoCard getCard() {
        return currentCard;
    }

    public static boolean hasCard() {
        return currentCard != null;
    }

    public static boolean isGreenFakeSlotIndex(int index) {
        return fakeGreenSlotIndices.contains(index);
    }

    public static boolean isRedFakeSlotIndex(int index) {
        return fakeRedSlotIndices.contains(index);
    }

    public static void clear() {
        currentCard = null;
        fakeGreenSlotIndices = Set.of();
        fakeRedSlotIndices = Set.of();
        ClientRerollAnimation.clear();
    }

    private static Set<String> changedSlotIds(BingoCard previous, BingoCard next) {
        Set<String> changed = new HashSet<>();
        if (previous == null || next == null) return changed;
        if (previous.getSize() != next.getSize()) return changed;
        int size = next.getSize();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BingoSlot before = previous.getSlot(x, y);
                BingoSlot after = next.getSlot(x, y);
                String beforeId = before == null ? "" : before.getId();
                String afterId = after == null ? "" : after.getId();
                if (afterId == null || afterId.isBlank()) continue;
                if (!afterId.equals(beforeId)) {
                    changed.add(afterId);
                }
            }
        }
        return changed;
    }
}
