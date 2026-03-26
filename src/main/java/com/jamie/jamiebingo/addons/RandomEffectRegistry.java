package com.jamie.jamiebingo.addons;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Central registry for random effects.
 * For now this only includes vanilla potion effects.
 * Custom effects will be added later.
 */
public final class RandomEffectRegistry {

    private static final Random RANDOM = new Random();

    private RandomEffectRegistry() {
        // no instances
    }

    /**
     * Returns a random vanilla MobEffect.
     * Instant effects are excluded.
     */
    public static MobEffect getRandomVanillaEffect() {

        List<MobEffect> valid = getAllVanillaEffects();
        if (valid.isEmpty()) return null;
        return valid.get(RANDOM.nextInt(valid.size()));
    }

    public static List<MobEffect> getAllVanillaEffects() {
        List<MobEffect> valid = new ArrayList<>();

        for (MobEffect effect : ForgeRegistries.MOB_EFFECTS.getValues()) {
            if (effect == null) continue;
            if (effect.isInstantenous()) continue;
            valid.add(effect);
        }

        return valid;
    }
}
