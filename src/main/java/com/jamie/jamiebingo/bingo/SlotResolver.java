package com.jamie.jamiebingo.bingo;

import com.jamie.jamiebingo.ItemDefinition;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.data.ItemDatabase;
import com.jamie.jamiebingo.quest.QuestDatabase;
import com.jamie.jamiebingo.quest.QuestDefinition;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class SlotResolver {

    private SlotResolver() {
    }

    public static BingoSlot resolveSlot(String id) {
        if (id == null || id.isBlank()) return null;

        ItemDatabase.load();
        QuestDatabase.load();

        if (id.startsWith("quest.")) {
            QuestDefinition q = QuestDatabase.getQuests().stream()
                    .filter(def -> id.equals(def.id))
                    .findFirst()
                    .orElse(null);
            if (q == null) return null;
            return new BingoSlot(q.id, q.name, q.category, effectiveRarity(id, q.rarity));
        }

        ItemDefinition def = ItemDatabase.getById(id);
        if (def == null) return null;
        return new BingoSlot(def.id(), def.name(), def.category(), effectiveRarity(id, def.rarity()));
    }

    private static String effectiveRarity(String id, String fallback) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return fallback;
        BingoGameData data = BingoGameData.get(server);
        if (data == null) return fallback;
        return data.getEffectiveRarity(id, fallback);
    }
}
