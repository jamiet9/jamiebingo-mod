package com.jamie.jamiebingo.client.screen;

import com.jamie.jamiebingo.client.ClientMinecraftUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

final class MenuTextureQualityUtil {
    private static long lastAttemptMs;

    private MenuTextureQualityUtil() {
    }

    static void ensureNearestFiltering() {
        long now = System.currentTimeMillis();
        if (now - lastAttemptMs < 1000L) return;
        lastAttemptMs = now;
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        if (mc == null) return;

        trySetNearest(mc, resolveAtlasId(
                "net.minecraft.world.inventory.InventoryMenu",
                "BLOCK_ATLAS",
                "textures/atlas/blocks.png"
        ));
        trySetNearest(mc, resolveAtlasId(
                "net.minecraft.client.renderer.texture.TextureAtlas",
                "LOCATION_BLOCKS",
                "textures/atlas/blocks.png"
        ));
    }

    private static Identifier resolveAtlasId(String className, String fieldName, String fallback) {
        try {
            Class<?> c = Class.forName(className);
            Object out = c.getField(fieldName).get(null);
            if (out instanceof Identifier id) return id;
        } catch (Throwable ignored) {
        }
        return com.jamie.jamiebingo.util.IdUtil.id(fallback);
    }

    private static void trySetNearest(Minecraft mc, Identifier id) {
        if (mc == null || id == null) return;
        try {
            Object texture = mc.getTextureManager().getTexture(id);
            if (texture == null) return;
            java.lang.reflect.Method setFilter = findSetFilter(texture.getClass());
            if (setFilter != null) {
                setFilter.setAccessible(true);
                setFilter.invoke(texture, false, false);
            }
        } catch (Throwable ignored) {
        }
    }

    private static java.lang.reflect.Method findSetFilter(Class<?> cls) {
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod("setFilter", boolean.class, boolean.class);
            } catch (Throwable ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
