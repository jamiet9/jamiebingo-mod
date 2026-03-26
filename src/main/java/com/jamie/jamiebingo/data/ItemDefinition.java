package com.jamie.jamiebingo.data;

public class ItemDefinition {

    private final String id;
    private final String name;
    private final String category;
    private final String rarity;
    private final boolean enabled;

    public ItemDefinition(String id, String name, String category, String rarity, boolean enabled) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.rarity = rarity;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getRarity() {
        return rarity;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "ItemDefinition{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", rarity='" + rarity + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
