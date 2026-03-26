package com.jamie.jamiebingo;

public class ItemDefinition {

    private String id;
    private String name;
    private String category;
    private String rarity;
    private boolean enabled = true;
    private boolean requiresHostileMobsEnabled = false;
    private boolean requiresHostileMobsDisabled = false;
    private boolean requiresDaylightCycleEnabled = false;
    private boolean requiresDaylightCycleDisabled = false;

    // Gson requires a no-arg constructor
    public ItemDefinition() {}

    public ItemDefinition(String id, String name, String category, String rarity) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.rarity = rarity;
        this.enabled = true;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String category() {
        return category;
    }

    public String rarity() {
        return rarity;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean requiresHostileMobsEnabled() {
        return requiresHostileMobsEnabled;
    }

    public boolean requiresHostileMobsDisabled() {
        return requiresHostileMobsDisabled;
    }

    public boolean requiresDaylightCycleEnabled() {
        return requiresDaylightCycleEnabled;
    }

    public boolean requiresDaylightCycleDisabled() {
        return requiresDaylightCycleDisabled;
    }
}
