package com.jamie.jamiebingo.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.List;

public final class InventoryUtil {
    private InventoryUtil() {
    }

    public static NonNullList<ItemStack> getNonEquipmentItems(Inventory inv) {
        if (inv == null) return NonNullList.create();

        // Try method by name first.
        try {
            Method m = inv.getClass().getMethod("getNonEquipmentItems");
            Object out = m.invoke(inv);
            if (out instanceof NonNullList<?> nl) {
                @SuppressWarnings("unchecked")
                NonNullList<ItemStack> casted = (NonNullList<ItemStack>) nl;
                return casted;
            }
            if (out instanceof List<?> l) {
                @SuppressWarnings("unchecked")
                List<ItemStack> list = (List<ItemStack>) l;
                return NonNullList.of(com.jamie.jamiebingo.util.ItemStackUtil.empty(), list.toArray(new ItemStack[0]));
            }
        } catch (Throwable ignored) {
        }

        // Prefer a field that looks like the main inventory list.
        NonNullList<ItemStack> best = null;
        int bestSize = -1;
        for (java.lang.reflect.Field f : inv.getClass().getDeclaredFields()) {
            try {
                f.setAccessible(true);
                Object out = f.get(inv);
                if (out instanceof NonNullList<?> nl) {
                    @SuppressWarnings("unchecked")
                    NonNullList<ItemStack> list = (NonNullList<ItemStack>) nl;
                    int size = list.size();
                    String name = f.getName().toLowerCase();
                    if (name.contains("item") || name.contains("main")) {
                        if (size > bestSize) {
                            bestSize = size;
                            best = list;
                        }
                    } else if (best == null && size > bestSize) {
                        bestSize = size;
                        best = list;
                    }
                } else if (out instanceof List<?> l) {
                    @SuppressWarnings("unchecked")
                    List<ItemStack> list = (List<ItemStack>) l;
                    int size = list.size();
                    String name = f.getName().toLowerCase();
                    if (name.contains("item") || name.contains("main")) {
                        if (size > bestSize) {
                            bestSize = size;
                            best = NonNullList.of(com.jamie.jamiebingo.util.ItemStackUtil.empty(), list.toArray(new ItemStack[0]));
                        }
                    } else if (best == null && size > bestSize) {
                        bestSize = size;
                        best = NonNullList.of(com.jamie.jamiebingo.util.ItemStackUtil.empty(), list.toArray(new ItemStack[0]));
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        if (best != null) return best;

        // Fallback: iterate container slots (covers versions without exposed lists).
        NonNullList<ItemStack> out = NonNullList.create();
        try {
            int size = inv.getContainerSize();
            for (int i = 0; i < size; i++) {
                out.add(inv.getItem(i));
            }
        } catch (Throwable ignored) {
        }
        if (!out.isEmpty()) return out;

        // Try any no-arg method returning NonNullList/List of ItemStack.
        for (Method m : inv.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            Class<?> rt = m.getReturnType();
            if (!NonNullList.class.isAssignableFrom(rt) && !List.class.isAssignableFrom(rt)) continue;
            try {
                Object outObj = m.invoke(inv);
                if (outObj instanceof NonNullList<?> nl) {
                    @SuppressWarnings("unchecked")
                    NonNullList<ItemStack> casted = (NonNullList<ItemStack>) nl;
                    return casted;
                }
                if (outObj instanceof List<?> l) {
                    @SuppressWarnings("unchecked")
                    List<ItemStack> list = (List<ItemStack>) l;
                    return NonNullList.of(com.jamie.jamiebingo.util.ItemStackUtil.empty(), list.toArray(new ItemStack[0]));
                }
            } catch (Throwable ignored) {
            }
        }

        return NonNullList.create();
    }

    public static NonNullList<ItemStack> getEquipment(Inventory inv) {
        if (inv == null) return NonNullList.create();

        try {
            Method m = inv.getClass().getMethod("getEquipment");
            Object out = m.invoke(inv);
            if (out instanceof NonNullList<?> nl) {
                @SuppressWarnings("unchecked")
                NonNullList<ItemStack> casted = (NonNullList<ItemStack>) nl;
                return casted;
            }
            if (out instanceof List<?> l) {
                @SuppressWarnings("unchecked")
                List<ItemStack> list = (List<ItemStack>) l;
                return NonNullList.of(com.jamie.jamiebingo.util.ItemStackUtil.empty(), list.toArray(new ItemStack[0]));
            }
        } catch (Throwable ignored) {
        }

        // Try any no-arg method returning NonNullList/List.
        for (Method m : inv.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            Class<?> rt = m.getReturnType();
            if (!NonNullList.class.isAssignableFrom(rt) && !List.class.isAssignableFrom(rt)) continue;
            try {
                Object out = m.invoke(inv);
                if (out instanceof NonNullList<?> nl) {
                    @SuppressWarnings("unchecked")
                    NonNullList<ItemStack> casted = (NonNullList<ItemStack>) nl;
                    return casted;
                }
                if (out instanceof List<?> l) {
                    @SuppressWarnings("unchecked")
                    List<ItemStack> list = (List<ItemStack>) l;
                    return NonNullList.of(com.jamie.jamiebingo.util.ItemStackUtil.empty(), list.toArray(new ItemStack[0]));
                }
            } catch (Throwable ignored) {
            }
        }

        return NonNullList.create();
    }

    public static ItemStack getEquipmentItem(Player player, EquipmentSlot slot) {
        if (player == null || slot == null) return com.jamie.jamiebingo.util.ItemStackUtil.empty();

        // Prefer reflective getItemBySlot on LivingEntity.
        ItemStack bySlot = getItemBySlot(player, slot);
        if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(bySlot)) return bySlot;

        Inventory inv = PlayerInventoryUtil.getInventory(player);
        NonNullList<ItemStack> equipment = getEquipment(inv);
        if (equipment.isEmpty()) return com.jamie.jamiebingo.util.ItemStackUtil.empty();

        int idx = switch (slot) {
            case FEET -> 0;
            case LEGS -> 1;
            case CHEST -> 2;
            case HEAD -> 3;
            case OFFHAND -> equipment.size() > 4 ? 4 : equipment.size() - 1;
            default -> 0;
        };
        if (idx < 0 || idx >= equipment.size()) return com.jamie.jamiebingo.util.ItemStackUtil.empty();
        ItemStack stack = equipment.get(idx);
        return stack == null ? com.jamie.jamiebingo.util.ItemStackUtil.empty() : stack;
    }

    public static ItemStack getItemBySlot(LivingEntity entity, EquipmentSlot slot) {
        if (entity == null || slot == null) return com.jamie.jamiebingo.util.ItemStackUtil.empty();
        try {
            Method m = entity.getClass().getMethod("getItemBySlot", EquipmentSlot.class);
            Object out = m.invoke(entity, slot);
            if (out instanceof ItemStack stack) return stack;
        } catch (Throwable ignored) {
        }

        // Try any method with EquipmentSlot parameter returning ItemStack.
        for (Method m : entity.getClass().getMethods()) {
            if (m.getParameterCount() != 1) continue;
            if (m.getParameterTypes()[0] != EquipmentSlot.class) continue;
            if (!ItemStack.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                Object out = m.invoke(entity, slot);
                if (out instanceof ItemStack stack) return stack;
            } catch (Throwable ignored) {
            }
        }

        if (entity instanceof Player p) {
            return getEquipmentItem(p, slot);
        }
        return com.jamie.jamiebingo.util.ItemStackUtil.empty();
    }
}
