package com.jamie.jamiebingo.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class EntityServerUtil {

    private EntityServerUtil() {
    }

    public static MinecraftServer getServer(Entity entity) {
        Level level = EntityLevelUtil.getLevel(entity);
        MinecraftServer server = LevelServerUtil.getServer(level);
        if (server != null) return server;
        if (entity == null) return null;
        try {
            for (Method m : entity.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!MinecraftServer.class.isAssignableFrom(m.getReturnType())) continue;
                Object out = m.invoke(entity);
                if (out instanceof MinecraftServer s) return s;
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Field f : entity.getClass().getDeclaredFields()) {
                if (!MinecraftServer.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object out = f.get(entity);
                if (out instanceof MinecraftServer s) return s;
            }
        } catch (Throwable ignored) {
        }
        try {
            return net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        } catch (Throwable ignored) {
        }
        return null;
    }
}
