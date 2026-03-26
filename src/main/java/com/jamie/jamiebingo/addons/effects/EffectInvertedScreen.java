package com.jamie.jamiebingo.addons.effects;

public class EffectInvertedScreen implements CustomRandomEffect {

    public static boolean ACTIVE = false;

    @Override
    public String id() {
        return "inverted_screen";
    }

    @Override
    public String displayName() {
        return "Inverted Screen";
    }

    @Override
    public void onApply(net.minecraft.server.MinecraftServer server) {
        ACTIVE = true;
    }

    @Override
    public void onRemove(net.minecraft.server.MinecraftServer server) {
        ACTIVE = false;
    }
}
