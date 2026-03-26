package com.jamie.jamiebingo.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ClientRerollAnimation {
    private static final Map<String, Long> untilTickBySlot = new HashMap<>();
    private static final int DEFAULT_DURATION_TICKS = 20;

    private ClientRerollAnimation() {
    }

    public static void animate(Collection<String> slotIds) {
        animate(slotIds, DEFAULT_DURATION_TICKS);
    }

    public static void animate(Collection<String> slotIds, int durationTicks) {
        if (slotIds == null || slotIds.isEmpty()) return;
        long now = currentTick();
        long until = now + Math.max(1, durationTicks);
        for (String id : slotIds) {
            if (id == null || id.isBlank()) continue;
            Long existing = untilTickBySlot.get(id);
            if (existing == null || existing < until) {
                untilTickBySlot.put(id, until);
            }
        }
    }

    public static boolean isAnimating(String slotId) {
        if (slotId == null || slotId.isBlank()) return false;
        Long until = untilTickBySlot.get(slotId);
        if (until == null) return false;
        long now = currentTick();
        if (now >= until) {
            untilTickBySlot.remove(slotId);
            return false;
        }
        return true;
    }

    public static float pulse(float partialTicks) {
        long ticks = currentTick();
        float t = (ticks + partialTicks) * 0.8f;
        return (float) (0.5f + 0.5f * Math.sin(t));
    }

    public static void tick() {
        if (untilTickBySlot.isEmpty()) return;
        long now = currentTick();
        Iterator<Map.Entry<String, Long>> it = untilTickBySlot.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() <= now) {
                it.remove();
            }
        }
    }

    public static void clear() {
        untilTickBySlot.clear();
    }

    private static long currentTick() {
        var mc = ClientMinecraftUtil.getMinecraft();
        var level = ClientMinecraftUtil.getLevel(mc);
        return level != null ? level.getGameTime() : 0L;
    }
}
