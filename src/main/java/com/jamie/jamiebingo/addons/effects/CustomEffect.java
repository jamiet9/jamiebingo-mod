package com.jamie.jamiebingo.addons.effects;

import net.minecraft.server.MinecraftServer;

public interface CustomEffect {

    /** Unique string ID (used for save + lookup) */
    String id();

    /** Display name shown to players */
    String displayName();

    /** Called once when the effect starts */
    void onApply(MinecraftServer server);

    /** Called every server tick while active (optional) */
    default void onTick(MinecraftServer server) {}

    /** Called once when the effect ends */
    default void onRemove(MinecraftServer server) {}
}
