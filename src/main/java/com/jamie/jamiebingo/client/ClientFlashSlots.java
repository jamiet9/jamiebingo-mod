package com.jamie.jamiebingo.client;

import net.minecraft.client.Minecraft;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ClientFlashSlots {

    private record FlashState(long untilTick, int style) {}
    private static final Map<String, FlashState> flashUntilTick = new HashMap<>();

    private ClientFlashSlots() {}

    public static void flash(Collection<String> slots, int durationTicks) {
        flash(slots, durationTicks, com.jamie.jamiebingo.network.PacketFlashSlots.STYLE_PULSE);
    }

    public static void flash(Collection<String> slots, int durationTicks, int style) {
        if (slots == null || slots.isEmpty()) return;
        long now = currentTick();
        long until = now + Math.max(1, durationTicks);
        for (String id : slots) {
            if (id == null || id.isBlank()) continue;
            FlashState existing = flashUntilTick.get(id);
            if (existing == null || existing.untilTick() < until) {
                flashUntilTick.put(id, new FlashState(until, style));
            } else if (existing.style() != style && existing.untilTick() > now) {
                flashUntilTick.put(id, new FlashState(existing.untilTick(), style));
            }
        }
        pruneExpired(now);
    }

    public static boolean isFlashing(String slotId) {
        if (slotId == null || slotId.isBlank()) return false;
        long now = currentTick();
        FlashState state = flashUntilTick.get(slotId);
        if (state == null) return false;
        if (state.untilTick() <= now) {
            flashUntilTick.remove(slotId);
            return false;
        }
        return true;
    }

    public static boolean isShakeFlashing(String slotId) {
        if (!isFlashing(slotId)) return false;
        FlashState state = flashUntilTick.get(slotId);
        return state != null && state.style() == com.jamie.jamiebingo.network.PacketFlashSlots.STYLE_SHAKE;
    }

    public static int shakeOffsetX(String slotId) {
        if (!isShakeFlashing(slotId)) return 0;
        long tick = currentTick();
        float seed = slotId == null ? 0f : (Math.abs(slotId.hashCode()) % 360) * ((float) Math.PI / 180.0f);
        float angle = tick * 1.2f + seed;
        return Math.round((float) Math.sin(angle) * 5.0f);
    }

    public static int shakeOffsetY(String slotId) {
        if (!isShakeFlashing(slotId)) return 0;
        long tick = currentTick();
        float seed = slotId == null ? 0f : (Math.abs(slotId.hashCode() * 31) % 360) * ((float) Math.PI / 180.0f);
        float angle = tick * 1.1f + seed;
        return Math.round((float) Math.cos(angle) * 4.0f);
    }

    public static float pulse(float partialTicks) {
        float t = (float) (currentTick() + partialTicks);
        return (float) (Math.sin(t * 0.4f) * 0.5f + 0.5f);
    }

    public static void clear() {
        flashUntilTick.clear();
    }

    private static long currentTick() {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        var level = ClientMinecraftUtil.getLevel(mc);
        if (level == null) return 0;
        return level.getGameTime();
    }

    private static void pruneExpired(long now) {
        Iterator<Map.Entry<String, FlashState>> it = flashUntilTick.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().untilTick() <= now) {
                it.remove();
            }
        }
    }
}
