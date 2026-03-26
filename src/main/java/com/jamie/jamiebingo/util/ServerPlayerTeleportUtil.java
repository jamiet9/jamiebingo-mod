package com.jamie.jamiebingo.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;

import java.lang.reflect.Method;
import java.util.Set;

public final class ServerPlayerTeleportUtil {

    private ServerPlayerTeleportUtil() {
    }

    public static boolean teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, Set<Relative> relatives, float yaw, float pitch, boolean dismount) {
        if (player == null || level == null) return false;

        // Direct mapped call (preferred for dimension changes).
        try {
            return player.teleportTo(level, x, y, z, relatives, yaw, pitch, dismount);
        } catch (Throwable ignored) {
        }

        // Preferred signature
        try {
            Method m = player.getClass().getMethod(
                    "teleportTo",
                    ServerLevel.class, double.class, double.class, double.class,
                    Set.class, float.class, float.class, boolean.class
            );
            Object res = m.invoke(player, level, x, y, z, relatives, yaw, pitch, dismount);
            return asBoolean(res, true);
        } catch (ReflectiveOperationException ignored) {
        }

        // Alternative without boolean
        try {
            Method m = player.getClass().getMethod(
                    "teleportTo",
                    ServerLevel.class, double.class, double.class, double.class,
                    Set.class, float.class, float.class
            );
            Object res = m.invoke(player, level, x, y, z, relatives, yaw, pitch);
            return asBoolean(res, true);
        } catch (ReflectiveOperationException ignored) {
        }

        // Fallback: setPos + change level
        try {
            Method m = player.getClass().getMethod("teleportTo", double.class, double.class, double.class);
            m.invoke(player, x, y, z);
            return true;
        } catch (ReflectiveOperationException ignored) {
        }

        return false;
    }

    private static boolean asBoolean(Object res, boolean defaultValue) {
        if (res instanceof Boolean b) return b;
        return defaultValue;
    }
}
