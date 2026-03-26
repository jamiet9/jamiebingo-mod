package com.jamie.jamiebingo.bingo;

/**
 * Controls how the bingo card is composed.
 * This does NOT define game behaviour (that is BingoMode).
 */
public enum CardComposition {

    /**
     * Classic items only (no quests at all).
     */
    CLASSIC_ONLY,

    /**
     * Quests only (current Quest Mode behaviour).
     */
    QUEST_ONLY,

    /**
     * Hybrid: per-slot roll using quest percentage.
     */
    HYBRID_PERCENT,

    /**
     * Hybrid: Quest is treated as a category.
     */
    HYBRID_CATEGORY
}
