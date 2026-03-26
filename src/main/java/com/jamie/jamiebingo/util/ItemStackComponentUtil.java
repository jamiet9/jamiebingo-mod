package com.jamie.jamiebingo.util;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class ItemStackComponentUtil {
    private ItemStackComponentUtil() {
    }

    private static final CustomData EMPTY_CUSTOM_DATA = resolveEmptyCustomData();

    public static CustomData emptyCustomData() {
        return EMPTY_CUSTOM_DATA;
    }

    public static CompoundTag copyCustomDataTag(CustomData data) {
        CompoundTag tag = extractTag(data);
        return tag == null ? new CompoundTag() : tag;
    }

    public static CompoundTag getCustomDataTagOrLegacy(ItemStack stack) {
        if (stack == null) return new CompoundTag();
        DataComponents.refresh();
        CustomData data = getOrDefault(
                stack,
                DataComponents.CUSTOM_DATA,
                emptyCustomData()
        );
        CompoundTag tag = extractTag(data);
        if (tag != null && !com.jamie.jamiebingo.util.NbtUtil.isEmptyTag(tag)) return tag;
        CompoundTag legacy = getLegacyTag(stack);
        return legacy == null ? new CompoundTag() : legacy;
    }

    public static <T> void set(ItemStack stack, DataComponentType<T> type, T value) {
        if (stack == null) return;
        DataComponents.refresh();
        if (type == null) {
            if (value instanceof Component c) {
                setHoverName(stack, c);
            }
            return;
        }
        try {
            Method m = stack.getClass().getMethod("set", DataComponentType.class, Object.class);
            m.invoke(stack, type, value);
            return;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : stack.getClass().getMethods()) {
                if (m.getParameterCount() != 2) continue;
                if (m.getParameterTypes()[0] != DataComponentType.class) continue;
                m.invoke(stack, type, value);
                return;
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : stack.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() != 2) continue;
                if (m.getParameterTypes()[0] != DataComponentType.class) continue;
                m.setAccessible(true);
                m.invoke(stack, type, value);
                return;
            }
        } catch (Throwable ignored) {
        }
        if (type == DataComponents.CUSTOM_NAME && value instanceof Component c) {
            setHoverName(stack, c);
        }
    }

    public static void remove(ItemStack stack, DataComponentType<?> type) {
        if (stack == null || type == null) return;
        DataComponents.refresh();
        try {
            Method m = stack.getClass().getMethod("remove", DataComponentType.class);
            m.invoke(stack, type);
            return;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : stack.getClass().getMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (m.getParameterTypes()[0] != DataComponentType.class) continue;
                m.invoke(stack, type);
                return;
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : stack.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (m.getParameterTypes()[0] != DataComponentType.class) continue;
                m.setAccessible(true);
                m.invoke(stack, type);
                return;
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(ItemStack stack, DataComponentType<T> type) {
        if (stack == null || type == null) return null;
        DataComponents.refresh();
        try {
            Method m = stack.getClass().getMethod("get", DataComponentType.class);
            Object out = m.invoke(stack, type);
            return (T) out;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : stack.getClass().getMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (m.getParameterTypes()[0] != DataComponentType.class) continue;
                Object out = m.invoke(stack, type);
                return (T) out;
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : stack.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (m.getParameterTypes()[0] != DataComponentType.class) continue;
                m.setAccessible(true);
                Object out = m.invoke(stack, type);
                return (T) out;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getOrDefault(ItemStack stack, DataComponentType<T> type, T def) {
        if (stack == null || type == null) return def;
        DataComponents.refresh();
        try {
            Method m = stack.getClass().getMethod("getOrDefault", DataComponentType.class, Object.class);
            Object out = m.invoke(stack, type, def);
            return (T) out;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : stack.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() != 2) continue;
                if (m.getParameterTypes()[0] != DataComponentType.class) continue;
                m.setAccessible(true);
                Object out = m.invoke(stack, type, def);
                return (T) out;
            }
        } catch (Throwable ignored) {
        }
        T out = get(stack, type);
        return out != null ? out : def;
    }

    public static boolean has(ItemStack stack, DataComponentType<?> type) {
        if (stack == null || type == null) return false;
        DataComponents.refresh();
        try {
            Method m = stack.getClass().getMethod("has", DataComponentType.class);
            Object out = m.invoke(stack, type);
            if (out instanceof Boolean b) return b;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : stack.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (m.getParameterTypes()[0] != DataComponentType.class) continue;
                if (m.getReturnType() != boolean.class && m.getReturnType() != Boolean.class) continue;
                m.setAccessible(true);
                Object out = m.invoke(stack, type);
                if (out instanceof Boolean b) return b;
            }
        } catch (Throwable ignored) {
        }
        return get(stack, (DataComponentType<Object>) type) != null;
    }

    public static CompoundTag getLegacyTagOrEmpty(ItemStack stack) {
        CompoundTag tag = getLegacyTag(stack);
        return tag == null ? new CompoundTag() : tag;
    }

    public static void updateCustomData(ItemStack stack, Consumer<CompoundTag> mutator) {
        if (stack == null || mutator == null) return;
        DataComponents.refresh();

        if (DataComponents.CUSTOM_DATA != null) {
            // Try CustomData.update(DataComponentType, ItemStack, Consumer)
            try {
                Method update = CustomData.class.getMethod("update", DataComponentType.class, ItemStack.class, Consumer.class);
                update.invoke(null, DataComponents.CUSTOM_DATA, stack, mutator);
                return;
            } catch (Throwable ignored) {
            }
        }

        // Fallback: read current tag, mutate, write new CustomData
        CustomData empty = emptyCustomData();
        CustomData data = DataComponents.CUSTOM_DATA == null
                ? empty
                : getOrDefault(stack, DataComponents.CUSTOM_DATA, empty);
        CompoundTag tag = extractTag(data);
        if (tag == null) tag = new CompoundTag();
        mutator.accept(tag);
        CustomData newData = createCustomData(tag);
        if (newData != null && DataComponents.CUSTOM_DATA != null) {
            set(stack, DataComponents.CUSTOM_DATA, newData);
            return;
        }

        // Last resort: legacy tag on the item stack.
        CompoundTag legacy = getOrCreateLegacyTag(stack);
        if (legacy != null) {
            mutator.accept(legacy);
            setLegacyTag(stack, legacy);
        }
    }

    private static CompoundTag extractTag(CustomData data) {
        if (data == null) return null;
        try {
            Method m = data.getClass().getMethod("copyTag");
            Object out = m.invoke(data);
            if (out instanceof CompoundTag tag) return tag;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : data.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!CompoundTag.class.isAssignableFrom(m.getReturnType())) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("tag")) continue;
                Object out = m.invoke(data);
                if (out instanceof CompoundTag tag) return tag;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static CustomData createCustomData(CompoundTag tag) {
        try {
            for (Constructor<?> c : CustomData.class.getDeclaredConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 1 && params[0] == CompoundTag.class) {
                    c.setAccessible(true);
                    Object out = c.newInstance(tag);
                    if (out instanceof CustomData cd) return cd;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static CustomData resolveEmptyCustomData() {
        try {
            java.lang.reflect.Field f = CustomData.class.getField("EMPTY");
            Object out = f.get(null);
            if (out instanceof CustomData cd) return cd;
        } catch (Throwable ignored) {
        }
        try {
            for (java.lang.reflect.Field f : CustomData.class.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                if (!CustomData.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object out = f.get(null);
                if (out instanceof CustomData cd) return cd;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static void setHoverName(ItemStack stack, Component name) {
        if (stack == null || name == null) return;
        try {
            Method m = stack.getClass().getMethod("setHoverName", Component.class);
            m.invoke(stack, name);
            return;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : stack.getClass().getMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (!Component.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
                m.invoke(stack, name);
                return;
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : stack.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (!Component.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
                if (m.getReturnType() != void.class && !ItemStack.class.isAssignableFrom(m.getReturnType())) continue;
                m.setAccessible(true);
                m.invoke(stack, name);
                return;
            }
        } catch (Throwable ignored) {
        }
        // Legacy fallback: write display name tag
        try {
            CompoundTag tag = getOrCreateLegacyTag(stack);
            if (tag != null) {
                CompoundTag display = new CompoundTag();
                com.jamie.jamiebingo.util.NbtUtil.putString(display, "Name", ComponentUtil.toJson(name));
                com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "display", display);
                setLegacyTag(stack, tag);
            }
        } catch (Throwable ignored) {
        }
    }

    private static CompoundTag getLegacyTag(ItemStack stack) {
        if (stack == null) return null;
        try {
            Method m = stack.getClass().getMethod("getTag");
            Object out = m.invoke(stack);
            if (out instanceof CompoundTag tag) return tag;
            if (out instanceof java.util.Optional<?> opt) {
                Object v = opt.orElse(null);
                if (v instanceof CompoundTag tag) return tag;
            }
            return null;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : stack.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("tag")) continue;
                Object out = m.invoke(stack);
                if (out instanceof CompoundTag tag) return tag;
                if (out instanceof java.util.Optional<?> opt) {
                    Object v = opt.orElse(null);
                    if (v instanceof CompoundTag tag) return tag;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static CompoundTag getOrCreateLegacyTag(ItemStack stack) {
        if (stack == null) return null;
        try {
            Method m = stack.getClass().getMethod("getOrCreateTag");
            Object out = m.invoke(stack);
            return out instanceof CompoundTag tag ? tag : null;
        } catch (Throwable ignored) {
        }
        CompoundTag tag = getLegacyTag(stack);
        if (tag != null) return tag;
        tag = new CompoundTag();
        setLegacyTag(stack, tag);
        return tag;
    }

    public static void putLegacyBoolean(ItemStack stack, String key, boolean value) {
        if (stack == null || key == null) return;
        try {
            CompoundTag tag = getOrCreateLegacyTag(stack);
            if (tag != null) {
                com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, key, value);
                setLegacyTag(stack, tag);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void setLegacyTag(ItemStack stack, CompoundTag tag) {
        if (stack == null || tag == null) return;
        try {
            Method m = stack.getClass().getMethod("setTag", CompoundTag.class);
            m.invoke(stack, tag);
            return;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : stack.getClass().getMethods()) {
                if (m.getParameterCount() != 1) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("tag")) continue;
                Class<?> p = m.getParameterTypes()[0];
                if (p == CompoundTag.class) {
                    m.invoke(stack, tag);
                    return;
                }
                if (p == java.util.Optional.class) {
                    m.invoke(stack, java.util.Optional.of(tag));
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
