package com.jamie.jamiebingo.client;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ClientHighlightedSlots {

    private static final Set<String> highlighted = new HashSet<>();

    private ClientHighlightedSlots() {
    }

    public static void set(Set<String> newSet) {
        highlighted.clear();
        if (newSet != null && !newSet.isEmpty()) {
            highlighted.addAll(newSet);
        }
    }

    public static boolean isHighlighted(String slotId) {
        return highlighted.contains(slotId);
    }

    public static Set<String> getAll() {
        return Collections.unmodifiableSet(highlighted);
    }

    public static void clear() {
        highlighted.clear();
    }
}
