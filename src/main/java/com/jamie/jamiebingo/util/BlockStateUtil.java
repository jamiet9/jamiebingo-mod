package com.jamie.jamiebingo.util;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.lang.reflect.Method;

public final class BlockStateUtil {

    private BlockStateUtil() {
    }

    public static BlockState defaultState(Block block) {
        if (block == null) {
            return null;
        }
        try {
            return block.defaultBlockState();
        } catch (Throwable ignored) {
        }

        // Look for zero-arg method returning BlockState (obf name varies)
        for (Method m : block.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() != BlockState.class) continue;
            try {
                return (BlockState) m.invoke(block);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static BlockState setValue(BlockState state, Property property, Comparable value) {
        if (state == null || property == null || value == null) {
            return state;
        }
        try {
            return state.setValue(property, value);
        } catch (Throwable ignored) {
        }

        // Look for a method taking (Property, Comparable) returning BlockState
        for (Method m : state.getClass().getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (p.length != 2) continue;
            if (!Property.class.isAssignableFrom(p[0])) continue;
            if (!Comparable.class.isAssignableFrom(p[1])) continue;
            if (m.getReturnType() != BlockState.class) continue;
            try {
                return (BlockState) m.invoke(state, property, value);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return state;
    }
}
