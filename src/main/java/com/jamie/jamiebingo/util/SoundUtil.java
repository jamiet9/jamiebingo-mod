package com.jamie.jamiebingo.util;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class SoundUtil {
    private SoundUtil() {
    }

    public static SoundEvent create(Identifier id) {
        if (id == null) return null;
        // Reflect registry "get" / "getValue" to avoid signature churn.
        try {
            Class<?> reg = net.minecraft.core.registries.BuiltInRegistries.class;
            Object soundRegistry = reg.getField("SOUND_EVENT").get(null);
            for (java.lang.reflect.Method m : soundRegistry.getClass().getMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (!m.getName().toLowerCase().contains("get")) continue;
                Class<?> p = m.getParameterTypes()[0];
                if (p.isAssignableFrom(Identifier.class)) {
                    Object out = m.invoke(soundRegistry, id);
                    if (out instanceof SoundEvent se) return se;
                    // Some registries return Optional or Holder/Reference wrappers.
                    try {
                        java.lang.reflect.Method value = out.getClass().getMethod("value");
                        Object v = value.invoke(out);
                        if (v instanceof SoundEvent se2) return se2;
                    } catch (Throwable ignored) {
                    }
                    try {
                        java.lang.reflect.Method get = out.getClass().getMethod("get");
                        Object v = get.invoke(out);
                        if (v instanceof SoundEvent se3) return se3;
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static void playToPlayer(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        if (player == null || sound == null) return;

        // Try direct player sound if available.
        try {
            java.lang.reflect.Method m = player.getClass().getMethod("playSound", SoundEvent.class, float.class, float.class);
            m.invoke(player, sound, volume, pitch);
            return;
        } catch (Throwable ignored) {
        }

        // Fallback: level broadcast at player's position.
        try {
            Level level = player.level();
            if (level != null) {
                level.playSound(
                        null,
                        player.blockPosition(),
                        sound,
                        SoundSource.PLAYERS,
                        volume,
                        pitch
                );
            }
        } catch (Throwable ignored) {
        }
    }
}
