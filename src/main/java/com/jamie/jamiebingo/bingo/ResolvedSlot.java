package com.jamie.jamiebingo.bingo;

/**
 * Inert result of resolving a single bingo slot.
 * Contains no gameplay logic and is safe for casino visuals.
 */
public class ResolvedSlot {

    public final String id;
    public final String name;
    public final String category;
    public final String rarity;
    public final boolean isQuest;

    public ResolvedSlot(
            String id,
            String name,
            String category,
            String rarity,
            boolean isQuest
    ) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.rarity = rarity;
        this.isQuest = isQuest;
    }
}
