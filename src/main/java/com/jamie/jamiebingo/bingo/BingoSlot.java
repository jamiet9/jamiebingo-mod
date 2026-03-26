package com.jamie.jamiebingo.bingo;

public class BingoSlot {

    private final String id;
    private final String name;
    private final String category;
    private final String rarity;

    public BingoSlot(String id, String name, String category, String rarity) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.rarity = rarity;
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

    @Override
    public String toString() {
        return name + " (" + rarity + ")";
    }
}
