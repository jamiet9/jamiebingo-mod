package com.jamie.jamiebingo.addons.effects;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EffectRandomMovement implements CustomRandomEffect {

    private static final Random RNG = new Random();

    // mutable list
    private static List<Integer> mapping = new ArrayList<>();

    private static boolean ACTIVE = false;

    @Override
    public String id() {
        return "random_movement";
    }

    @Override
    public String displayName() {
        return "Random Movement";
    }

    @Override
    public void onApply(MinecraftServer server) {
        mapping.clear();
        mapping.add(0);
        mapping.add(1);
        mapping.add(2);
        mapping.add(3);

        java.util.Collections.shuffle(mapping, RNG);
        ACTIVE = true;
    }

    @Override
    public void onRemove(MinecraftServer server) {
        ACTIVE = false;
        mapping.clear();
        mapping.add(0);
        mapping.add(1);
        mapping.add(2);
        mapping.add(3);
    }

    @Override
    public void onTick(MinecraftServer server) {
        // server does nothing
    }

    /* ===============================
       CLIENT MOVEMENT REMAP
       =============================== */

    public static float[] remapMovement(float forward, float left) {
        if (!ACTIVE) return new float[]{forward, left};

        float[] in = new float[]{
                forward,   // forward
                -forward,  // backward
                left,      // left
                -left      // right
        };

        float newForward = 0;
        float newLeft = 0;

        for (int i = 0; i < 4; i++) {
            switch (mapping.get(i)) {
                case 0 -> newForward += in[i];
                case 1 -> newForward -= in[i];
                case 2 -> newLeft += in[i];
                case 3 -> newLeft -= in[i];
            }
        }

        return new float[]{newForward, newLeft};
    }
}
