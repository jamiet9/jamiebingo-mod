package com.jamie.jamiebingo.client;

import net.minecraft.world.item.DyeColor;

import java.util.HashMap;
import java.util.Map;

public class ClientSlotOwnership {

    private static final Map<String, DyeColor> owners = new HashMap<>();

    public static void clear() {
        owners.clear();
    }

    public static void setAll(Map<String, DyeColor> data) {
        owners.clear();
        if (data != null) owners.putAll(data);
    }

    public static DyeColor getOwner(String slotId) {
        return owners.get(slotId);
    }

    public static boolean isOwned(String slotId) {
        return owners.containsKey(slotId);
    }
}
