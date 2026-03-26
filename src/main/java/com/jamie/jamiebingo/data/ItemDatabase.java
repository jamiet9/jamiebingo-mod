package com.jamie.jamiebingo.data;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.jamie.jamiebingo.ItemDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class ItemDatabase {

    private static final List<ItemDefinition> ITEMS = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static final Set<String> HOSTILE_DISABLED_BLOCKLIST = Set.of(
            "minecraft:end_rod",
            "minecraft:chorus_flower",
            "minecraft:purpur_block",
            "minecraft:purpur_pillar",
            "minecraft:purpur_stairs",
            "minecraft:end_stone",
            "minecraft:end_stone_bricks",
            "minecraft:dragon_egg",
            "minecraft:ender_chest",
            "minecraft:beacon",
            "minecraft:blaze_rod",
            "minecraft:ghast_tear",
            "minecraft:ender_eye",
            "minecraft:wither_skeleton_skull",
            "minecraft:zombie_head",
            "minecraft:creeper_head",
            "minecraft:dragon_head",
            "minecraft:nether_star",
            "minecraft:prismarine_shard",
            "minecraft:end_crystal",
            "minecraft:chorus_fruit",
            "minecraft:popped_chorus_fruit",
            "minecraft:dragon_breath",
            "minecraft:lingering_potion",
            "minecraft:totem_of_undying",
            "minecraft:shulker_shell",
            "minecraft:music_disc_13",
            "minecraft:music_disc_cat",
            "minecraft:music_disc_blocks",
            "minecraft:music_disc_chirp",
            "minecraft:music_disc_far",
            "minecraft:music_disc_mall",
            "minecraft:music_disc_mellohi",
            "minecraft:music_disc_stal",
            "minecraft:music_disc_strad",
            "minecraft:music_disc_ward",
            "minecraft:music_disc_11",
            "minecraft:music_disc_wait",
            "minecraft:music_disc_otherside",
            "minecraft:music_disc_5",
            "minecraft:music_disc_pigstep",
            "minecraft:disc_fragment_5",
            "minecraft:trident",
            "minecraft:phantom_membrane",
            "minecraft:nautilus_shell",
            "minecraft:flower_banner_pattern",
            "minecraft:creeper_banner_pattern",
            "minecraft:skull_banner_pattern",
            "minecraft:mojang_banner_pattern",
            "minecraft:globe_banner_pattern",
            "minecraft:piglin_banner_pattern",
            "minecraft:ochre_froglight",
            "minecraft:verdant_froglight",
            "minecraft:pearlescent_froglight"
    );

    public static boolean isLoaded() {
        return !ITEMS.isEmpty();
    }

    /**
     * Loads bingo_items.json once. Supports BOTH:
     * 1) [ {item}, {item} ]
     * 2) { "items": [ {item}, {item} ] }
     * 3) { "anything": {object} } -> values become list
     */
    public static void load() {
        if (isLoaded())
            return;

        ResourceManager resourceManager = getResourceManager();

        try {
            var resOpt = resourceManager == null
                    ? Optional.empty()
                    : getResourceOptional(
                            resourceManager,
                            com.jamie.jamiebingo.util.IdUtil.id("jamiebingo:bingo_items.json")
                    );

            InputStreamReader reader;
            if (resOpt.isEmpty()) {
                reader = tryOpenFromClassLoader();
                if (reader == null) {
                    System.out.println("[JamieBingo] bingo_items.json NOT FOUND!");
                    return;
                }
            } else {
                InputStream stream = openResource(resOpt.get());
                if (stream == null) {
                    System.out.println("[JamieBingo] bingo_items.json NOT FOUND!");
                    return;
                }
                reader = new InputStreamReader(stream);
            }

            try (reader) {

                Gson gson = new Gson();
                JsonElement root = JsonParser.parseReader(reader);

                Type listType = new TypeToken<List<ItemDefinition>>() {}.getType();
                List<ItemDefinition> loaded = new ArrayList<>();

                // CASE 1 ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â Pure JSON array
                if (root.isJsonArray()) {
                    loaded = gson.fromJson(root, listType);
                }

                // CASE 2 ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¾Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡Ãƒâ€šÃ‚Â¬ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¡ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â Object root
                else if (root.isJsonObject()) {
                    JsonObject obj = root.getAsJsonObject();

                    // Prefer "items" field if it exists
                    if (obj.has("items") && obj.get("items").isJsonArray()) {
                        loaded = gson.fromJson(obj.get("items"), listType);
                    }
                    else {
                        // fallback: collect all values that are objects
                        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                            if (entry.getValue().isJsonObject()) {
                                ItemDefinition def = gson.fromJson(entry.getValue(), ItemDefinition.class);
                                loaded.add(def);
                            }
                        }
                    }
                }

                normalizeLoadedCategories(loaded);
                ITEMS.addAll(loaded);
                if (resourceManager != null) {
                    addMissingItems();
                }
                System.out.println("[JamieBingo] Loaded " + ITEMS.size() + " bingo items successfully.");
            }

        } catch (Exception e) {
            System.out.println("[JamieBingo] Failed to load bingo items: " + e);
            e.printStackTrace();
        }
    }

    private static Optional<?> getResourceOptional(ResourceManager manager, Identifier id) {
        if (manager == null || id == null) return Optional.empty();
        Class<?> idClass = id.getClass();
        try {
            for (var m : manager.getClass().getMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (!Optional.class.isAssignableFrom(m.getReturnType())) continue;
                Class<?> param = m.getParameterTypes()[0];
                if (!param.isAssignableFrom(idClass)) continue;
                return (Optional<?>) m.invoke(manager, id);
            }
            for (var m : manager.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (!Optional.class.isAssignableFrom(m.getReturnType())) continue;
                Class<?> param = m.getParameterTypes()[0];
                if (!param.isAssignableFrom(idClass)) continue;
                m.setAccessible(true);
                return (Optional<?>) m.invoke(manager, id);
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private static InputStream openResource(Object resource) {
        if (resource == null) return null;
        try {
            var m = resource.getClass().getMethod("open");
            return (InputStream) m.invoke(resource);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static void normalizeLoadedCategories(List<ItemDefinition> loaded) {
        if (loaded == null || loaded.isEmpty()) return;
        for (ItemDefinition def : loaded) {
            if (def == null) continue;
            String normalized = normalizeCategory(def.category());
            if (normalized == null) continue;
            if (normalized.equals(def.category())) continue;
            setCategoryReflective(def, normalized);
        }
    }

    private static void setCategoryReflective(ItemDefinition def, String category) {
        if (def == null || category == null) return;
        try {
            java.lang.reflect.Field f = ItemDefinition.class.getDeclaredField("category");
            f.setAccessible(true);
            f.set(def, category);
        } catch (Exception ignored) {
        }
    }

    private static String normalizeCategory(String category) {
        if (category == null) return null;
        String s = category.trim().toLowerCase(Locale.ROOT);
        if (s.isBlank()) return s;
        return switch (s) {
            case "shove;", "shove" -> "shovel";
            case "restone", "resdstone" -> "redstone";
            case "moster" -> "monster";
            case "unkown" -> "unknown";
            case "die" -> "death";
            default -> s;
        };
    }

    private static void addMissingItems() {
        Set<String> existing = ITEMS.stream()
                .filter(Objects::nonNull)
                .map(ItemDefinition::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Item item : ForgeRegistries.ITEMS) {
            if (item == null) continue;
            Identifier id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null) continue;
            String key = id.toString();
            if ("minecraft:air".equals(key)) continue;
            if (existing.contains(key)) continue;
            ITEMS.add(new ItemDefinition(key, key, "impossible", "impossible"));
        }
    }

    private static InputStreamReader tryOpenFromClassLoader() {
        ClassLoader loader = ItemDatabase.class.getClassLoader();
        if (loader == null) return null;

        InputStream stream = loader.getResourceAsStream("data/jamiebingo/bingo_items.json");
        if (stream == null) {
            stream = loader.getResourceAsStream("jamiebingo/bingo_items.json");
        }
        if (stream == null) return null;
        return new InputStreamReader(stream);
    }

    private static ResourceManager getResourceManager() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            try {
                for (var m : server.getClass().getMethods()) {
                    if (m.getParameterCount() != 0) continue;
                    if (ResourceManager.class.isAssignableFrom(m.getReturnType())) {
                        return (ResourceManager) m.invoke(server);
                    }
                }
                for (var m : server.getClass().getDeclaredMethods()) {
                    if (m.getParameterCount() != 0) continue;
                    if (ResourceManager.class.isAssignableFrom(m.getReturnType())) {
                        m.setAccessible(true);
                        return (ResourceManager) m.invoke(server);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
    public static List<String> getCategories() {
        return ITEMS.stream()
                .filter(ItemDatabase::isAllowed)
                .map(ItemDefinition::category)
                .distinct()
                .toList();
    }

    public static List<ItemDefinition> getAllItems() {
        return Collections.unmodifiableList(ITEMS);
    }

    public static List<ItemDefinition> getAllowedItems() {
        return ITEMS.stream()
                .map(ItemDatabase::withEffectiveRarity)
                .filter(ItemDatabase::isAllowed)
                .toList();
    }

    public static ItemDefinition getById(String id) {
        if (id == null || id.isBlank()) return null;
        for (ItemDefinition def : ITEMS) {
            if (def != null && id.equals(def.id())) {
                return withEffectiveRarity(def);
            }
        }
        return null;
    }

    public static ItemDefinition getRawById(String id) {
        if (id == null || id.isBlank()) return null;
        for (ItemDefinition def : ITEMS) {
            if (def != null && id.equals(def.id())) {
                return def;
            }
        }
        return null;
    }

    public static List<String> getCategoriesForRarity(String rarity) {
        if (rarity == null) return List.of();
        return ITEMS.stream()
                .map(ItemDatabase::withEffectiveRarity)
                .filter(i -> i.rarity().equalsIgnoreCase(rarity))
                .filter(ItemDatabase::isAllowed)
                .map(ItemDefinition::category)
                .distinct()
                .toList();
    }

    public static String getRandomCategory(Random rand) {
        var cats = getCategories();
        if (cats.isEmpty()) return "misc";
        return cats.get(rand.nextInt(cats.size()));
    }

    public static String getRandomCategoryForRarity(String rarity, Random rand) {
        var cats = getCategoriesForRarity(rarity);
        if (cats.isEmpty()) return null;
        return cats.get(rand.nextInt(cats.size()));
    }

    public static ItemDefinition getRandomItem(String rarity, String category, Random rand) {
        List<ItemDefinition> filtered = ITEMS.stream()
                .map(ItemDatabase::withEffectiveRarity)
                .filter(i -> i.rarity().equalsIgnoreCase(rarity))
                .filter(i -> i.category().equalsIgnoreCase(category))
                .filter(ItemDatabase::isAllowed)
                .collect(Collectors.toList());

        if (filtered.isEmpty())
            return null;

        return filtered.get(rand.nextInt(filtered.size()));
    }

    public static ItemDefinition getRandomAny(Random rand) {
        List<ItemDefinition> filtered = ITEMS.stream()
                .map(ItemDatabase::withEffectiveRarity)
                .filter(ItemDatabase::isAllowed)
                .collect(Collectors.toList());

        if (filtered.isEmpty())
            return null;
        return filtered.get(rand.nextInt(filtered.size()));
    }

    private static boolean isAllowed(ItemDefinition item) {
        if (item == null) return false;
        if (!item.enabled()) return false;
        if (!isValidRegistryItem(item.id())) return false;
        if (isDisallowedAggregateEntry(item.id())) return false;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            BingoGameData data = BingoGameData.get(server);
            if (data != null && data.isSlotBlocked(item.id())) return false;
        }
        return isAllowedByHostileSetting(item) && isAllowedByDaylightSetting(item);
    }

    private static boolean isValidRegistryItem(String id) {
        if (id == null || id.isBlank()) return false;
        if (ServerLifecycleHooks.getCurrentServer() == null) return true;
        try {
            return ForgeRegistries.ITEMS != null
                    && ForgeRegistries.ITEMS.containsKey(com.jamie.jamiebingo.util.IdUtil.id(id));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static ItemDefinition withEffectiveRarity(ItemDefinition item) {
        if (item == null || item.id() == null || item.id().isBlank()) return item;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return item;
        BingoGameData data = BingoGameData.get(server);
        if (data == null) return item;
        String effectiveRarity = data.getEffectiveRarity(item.id(), item.rarity());
        if (effectiveRarity == null || effectiveRarity.equalsIgnoreCase(item.rarity())) return item;
        return new ItemDefinition(item.id(), item.name(), item.category(), effectiveRarity);
    }

    private static boolean isDisallowedAggregateEntry(String id) {
        if (id == null || id.isBlank()) return false;
        String key = id.toLowerCase(Locale.ROOT);
        return "minecraft:armor_trim".equals(key);
    }

    private static boolean isAllowedByHostileSetting(ItemDefinition item) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return true;

        BingoGameData data = BingoGameData.get(server);
        if (data == null || data.hostileMobsEnabled) return true;
        if (item.requiresHostileMobsEnabled()) return false;
        if (item.requiresHostileMobsDisabled()) return true;
        String id = item.id();
        if (id == null) return true;
        return !HOSTILE_DISABLED_BLOCKLIST.contains(id);
    }

    private static boolean isAllowedByDaylightSetting(ItemDefinition item) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return true;

        BingoGameData data = BingoGameData.get(server);
        if (data == null) return true;

        boolean daylightEnabled = data.daylightMode == BingoGameData.DAYLIGHT_ENABLED;
        if (item.requiresDaylightCycleEnabled() && !daylightEnabled) return false;
        if (item.requiresDaylightCycleDisabled() && daylightEnabled) return false;
        return true;
    }
}

