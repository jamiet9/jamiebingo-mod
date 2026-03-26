package com.jamie.jamiebingo.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClientProgressData {

    private static final Map<String, Boolean> completed = new HashMap<>();
    private static final Map<String, Integer> partial = new HashMap<>();

    public static void clear() {
        completed.clear();
        partial.clear();
    }

    // ===== SIMPLE BULK SET (used by PacketSyncProgress) =====
    public static void set(Set<String> completedIds) {
        clear();
        if (completedIds == null) return;
        for (String id : completedIds) {
            setCompleted(id, true);
        }
    }

    // ===== COMPLETED FLAGS =====

    public static void setCompleted(String id, boolean v) {
        completed.put(id, v);
    }

    public static boolean isCompleted(String id) {
        return completed.getOrDefault(id, false);
    }

    // ===== PARTIAL / MULTISTEP =====

    public static void setPartial(String id, int value) {
        partial.put(id, value);
    }

    public static int getPartial(String id) {
        return partial.getOrDefault(id, 0);
    }
}
