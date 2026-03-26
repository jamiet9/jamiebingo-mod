package com.jamie.jamiebingo.bingo;

import com.jamie.jamiebingo.ItemDefinition;
import com.jamie.jamiebingo.quest.QuestDefinition;

import java.util.Locale;

public final class ColorVariantUtil {

    private static final String[] COLOR_TOKENS = new String[] {
            "light_blue",
            "light_gray",
            "white",
            "orange",
            "magenta",
            "yellow",
            "lime",
            "pink",
            "gray",
            "cyan",
            "purple",
            "blue",
            "brown",
            "green",
            "red",
            "black"
    };

    private ColorVariantUtil() {
    }

    public static String questGroupKey(QuestDefinition quest) {
        if (quest == null || quest.id == null) return "";
        String raw = quest.id.toLowerCase(Locale.ROOT);
        String core = raw.startsWith("quest.") ? raw.substring("quest.".length()) : raw;
        String stripped = stripColorTokens(core);
        if (stripped.isBlank()) stripped = core;
        return "quest:" + stripped;
    }

    public static String itemGroupKey(ItemDefinition item, boolean separateColorVariants) {
        if (item == null || item.id() == null) return "";
        if (separateColorVariants) return item.id();
        return itemGroupKey(item.id());
    }

    public static String itemGroupKey(String itemId) {
        if (itemId == null || itemId.isBlank()) return "";
        String raw = itemId.toLowerCase(Locale.ROOT);
        int split = raw.indexOf(':');
        if (split < 0) {
            String stripped = stripColorTokens(raw);
            return stripped.isBlank() ? raw : stripped;
        }
        String namespace = raw.substring(0, split);
        String path = raw.substring(split + 1);
        String stripped = stripColorTokens(path);
        if (stripped.isBlank()) stripped = path;
        return namespace + ":" + stripped;
    }

    private static String stripColorTokens(String value) {
        if (value == null || value.isBlank()) return "";
        String out = value;
        for (String color : COLOR_TOKENS) {
            out = out.replaceAll("(^|_)" + color + "(?=_|$)", "$1");
        }
        out = out.replaceAll("__+", "_");
        out = out.replaceAll("^_+", "");
        out = out.replaceAll("_+$", "");
        return out;
    }
}
