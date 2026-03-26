package com.jamie.jamiebingo.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ClientQuestProgressData {

    private static final Map<String, Integer> progress = new HashMap<>();
    private static final Map<String, Integer> max = new HashMap<>();

    public static void clear() {
        progress.clear();
        max.clear();
    }

    public static void set(Map<String, Integer> progressMap, Map<String, Integer> maxMap) {
        clear();
        if (progressMap != null) {
            progress.putAll(progressMap);
        }
        if (maxMap != null) {
            max.putAll(maxMap);
        }
    }

    public static int getProgress(String questId) {
        return progress.getOrDefault(questId, 0);
    }

    public static int getMax(String questId) {
        return max.getOrDefault(questId, 0);
    }

    public static Map<String, Integer> getAllProgress() {
        return Collections.unmodifiableMap(progress);
    }
}
