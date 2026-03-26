package com.jamie.jamiebingo.addons.effects;

import java.util.*;

public final class CustomEffectRegistry {

    private static final Map<String, CustomRandomEffect> EFFECTS = new LinkedHashMap<>();
    private static final Random RANDOM = new Random();

    private CustomEffectRegistry() {}

    /* =========================
       REGISTRATION
       ========================= */

    public static void register(CustomRandomEffect effect) {
        String key = normalize(effect.id());
        EFFECTS.put(key, effect);
        System.out.println("[JamieBingo] Registered custom effect: " + key);
    }

    /* =========================
       LOOKUP
       ========================= */

    public static CustomRandomEffect getById(String id) {
        if (id == null) return null;
        return EFFECTS.get(normalize(id));
    }

    public static Collection<CustomRandomEffect> all() {
        return EFFECTS.values();
    }

    public static CustomRandomEffect getRandom() {
        if (EFFECTS.isEmpty()) return null;

        List<CustomRandomEffect> list = new ArrayList<>(EFFECTS.values());
        return list.get(RANDOM.nextInt(list.size()));
    }

    /* =========================
       HELPERS
       ========================= */

    private static String normalize(String id) {
        id = id.toLowerCase(Locale.ROOT);

        // Strip namespace if provided
        if (id.contains(":")) {
            id = id.substring(id.indexOf(':') + 1);
        }

        return id;
    }
}
