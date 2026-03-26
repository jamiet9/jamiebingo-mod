package com.jamie.jamiebingo.addons.effects;

import net.minecraft.server.MinecraftServer;

public interface CustomRandomEffect {

    /** Unique ID used by scheduler */
    String id();

    /** Name shown in HUD */
    String displayName();

    /** Called once when effect starts */
    default void onApply(MinecraftServer server) {}

    /** Called every server tick while active */
    default void onTick(MinecraftServer server) {}

    /** Called once when effect ends */
    default void onRemove(MinecraftServer server) {}
}
