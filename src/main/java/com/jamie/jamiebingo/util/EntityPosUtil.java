package com.jamie.jamiebingo.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class EntityPosUtil {
    private EntityPosUtil() {
    }

    public static BlockPos getBlockPos(Entity entity) {
        if (entity == null) return BlockPos.ZERO;
        try {
            Method m = entity.getClass().getMethod("blockPosition");
            Object out = m.invoke(entity);
            if (out instanceof BlockPos pos) return pos;
        } catch (Throwable ignored) {
        }
        try {
            Method m = entity.getClass().getMethod("blockPos");
            Object out = m.invoke(entity);
            if (out instanceof BlockPos pos) return pos;
        } catch (Throwable ignored) {
        }
        try {
            for (Field f : entity.getClass().getDeclaredFields()) {
                if (!BlockPos.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object out = f.get(entity);
                if (out instanceof BlockPos pos) return pos;
            }
        } catch (Throwable ignored) {
        }
        return BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
    }
}
