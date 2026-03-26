package com.jamie.jamiebingo.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;

public final class LevelSetBlockUtil {

    private LevelSetBlockUtil() {
    }

    public static boolean setBlock(Level level, BlockPos pos, BlockState state, int flags) {
        if (level == null || pos == null || state == null) return false;

        // Most common signature
        try {
            Method m = level.getClass().getMethod("setBlock", BlockPos.class, BlockState.class, int.class);
            Object res = m.invoke(level, pos, state, flags);
            return asBoolean(res);
        } catch (ReflectiveOperationException ignored) {
        }

        // Alternative: setBlockAndUpdate(BlockPos, BlockState)
        try {
            Method m = level.getClass().getMethod("setBlockAndUpdate", BlockPos.class, BlockState.class);
            Object res = m.invoke(level, pos, state);
            return asBoolean(res, true);
        } catch (ReflectiveOperationException ignored) {
        }

        // Alternative: setBlock(BlockPos, BlockState)
        try {
            Method m = level.getClass().getMethod("setBlock", BlockPos.class, BlockState.class);
            Object res = m.invoke(level, pos, state);
            return asBoolean(res, true);
        } catch (ReflectiveOperationException ignored) {
        }

        // Fallback: search any method named "setBlock" with compatible params
        for (Method m : level.getClass().getMethods()) {
            if (!"setBlock".equals(m.getName())) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 3 && p[0] == BlockPos.class && p[1] == BlockState.class && p[2] == int.class) {
                try {
                    Object res = m.invoke(level, pos, state, flags);
                    return asBoolean(res);
                } catch (ReflectiveOperationException ignored) {
                }
            } else if (p.length == 2 && p[0] == BlockPos.class && p[1] == BlockState.class) {
                try {
                    Object res = m.invoke(level, pos, state);
                    return asBoolean(res, true);
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }

        return false;
    }

    private static boolean asBoolean(Object res) {
        return asBoolean(res, false);
    }

    private static boolean asBoolean(Object res, boolean defaultValue) {
        if (res instanceof Boolean b) return b;
        return defaultValue;
    }
}
