package com.jamie.jamiebingo.quest;

public class QuestDefinition {

    public final String id;
    public final String name;
    public final String description;
    public final String rarity;
    public final String category;
    public final String extra;
    public final String texture;
    public final QuestRequirements requirements;

    public QuestDefinition(
            String id,
            String name,
            String description,
            String rarity,
            String category,
            String extra,
            String texture,
            QuestRequirements requirements
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rarity = rarity;
        this.category = category;
        this.extra = extra;
        this.texture = texture;
        this.requirements = requirements;
    }
}
