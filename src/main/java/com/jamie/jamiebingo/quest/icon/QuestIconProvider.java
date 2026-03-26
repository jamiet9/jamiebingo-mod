package com.jamie.jamiebingo.quest.icon;

import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.quest.QuestDatabase;
import com.jamie.jamiebingo.quest.QuestDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import com.jamie.jamiebingo.util.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.server.packs.resources.Resource;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Predicate;

public class QuestIconProvider {

    private static final Map<String, QuestIconData> CACHE = new HashMap<>();
    private static final Set<String> DEBUG_LOGGED_IDS = new HashSet<>();

    private static final Identifier HEART_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/gui/sprites/hud/heart/full.png");
    private static final Identifier HUNGER_EMPTY_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/gui/sprites/hud/food_empty.png");
    private static final Identifier HUNGER_FULL_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/gui/sprites/hud/food_full.png");
    private static final Identifier EXP_BAR_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/entity/experience_orb.png");
    private static final Identifier ADVANCEMENT_ICON_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("jamiebingo:textures/gui/advancement_icon.png");
    private static final Identifier ANGRY_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("jamiebingo:textures/gui/quest_icons/angry_face.png");
    private static final Identifier INFINITY_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("jamiebingo:textures/gui/quest_icons/infinity.png");
    private static final Identifier ZZZ_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("jamiebingo:textures/gui/quest_icons/zzz.png");
    private static final Identifier WATER_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/block/water_still.png");
    private static final Identifier FIRE_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/block/fire_0.png");
    private static final Identifier NETHER_PORTAL_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/block/nether_portal.png");
    private static final Identifier BARRIER_ITEM_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/item/barrier.png");
    private static final EntityType<? extends LivingEntity> HAPPY_GHAST_ENTITY =
            resolveLivingEntityType("minecraft:happy_ghast", EntityType.GHAST);

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\bnumber\\s+(\\d+)\\b");
    private static final Pattern KM_PATTERN = Pattern.compile("\\b(\\d+)\\s*km\\b");
    private static final Pattern M_PATTERN = Pattern.compile("\\b(\\d+)\\s*m\\b");
    private static final Pattern ANY_NUMBER_PATTERN = Pattern.compile("\\b(\\d+)\\b");
    private static final Pattern PURE_NUMBER_PATTERN = Pattern.compile("^\\d+\\s*(km|m)?$");

    private static final Map<String, String> EFFECT_TEXTURES = Map.ofEntries(
            Map.entry("absorption", "minecraft:textures/mob_effect/absorption.png"),
            Map.entry("jump boost", "minecraft:textures/mob_effect/jump_boost.png"),
            Map.entry("poison", "minecraft:textures/mob_effect/poison.png"),
            Map.entry("weakness", "minecraft:textures/mob_effect/weakness.png"),
            Map.entry("glowing", "minecraft:textures/mob_effect/glowing.png"),
            Map.entry("levitation", "minecraft:textures/mob_effect/levitation.png"),
            Map.entry("mining fatigue", "minecraft:textures/mob_effect/mining_fatigue.png"),
            Map.entry("mining fatique", "minecraft:textures/mob_effect/mining_fatigue.png"),
            Map.entry("nausea", "minecraft:textures/mob_effect/nausea.png"),
            Map.entry("bad omen", "minecraft:textures/mob_effect/bad_omen.png"),
            Map.entry("swiftness", "minecraft:textures/mob_effect/speed.png"),
            Map.entry("speed", "minecraft:textures/mob_effect/speed.png"),
            Map.entry("water breathing", "minecraft:textures/mob_effect/water_breathing.png"),
            Map.entry("wither", "minecraft:textures/mob_effect/wither.png"),
            Map.entry("slowness", "minecraft:textures/mob_effect/slowness.png")
    );

    private static final Map<String, String> SPECIAL_ITEMS = Map.ofEntries(
            Map.entry("music disc", "minecraft:music_disc_13"),
            Map.entry("disc fragment", "minecraft:disc_fragment_5"),
            Map.entry("exp bottle", "minecraft:experience_bottle"),
            Map.entry("experience bottle", "minecraft:experience_bottle"),
            Map.entry("experience bar", "minecraft:experience_bottle"),
            Map.entry("xp bottle", "minecraft:experience_bottle"),
            Map.entry("wheat seed", "minecraft:wheat_seeds"),
            Map.entry("wheat seeds", "minecraft:wheat_seeds"),
            Map.entry("sweet berry", "minecraft:sweet_berries"),
            Map.entry("glow berry", "minecraft:glow_berries"),
            Map.entry("poisonous potato", "minecraft:poisonous_potato"),
            Map.entry("carrot on a stick", "minecraft:carrot_on_a_stick"),
            Map.entry("warped fungus", "minecraft:warped_fungus"),
            Map.entry("crimson fungus", "minecraft:crimson_fungus"),
            Map.entry("golden carrot", "minecraft:golden_carrot"),
            Map.entry("glow ink", "minecraft:glow_ink_sac"),
            Map.entry("glow ink sac", "minecraft:glow_ink_sac"),
            Map.entry("feather", "minecraft:feather"),
            Map.entry("seed", "minecraft:wheat_seeds"),
            Map.entry("banner pattern", "minecraft:flower_banner_pattern"),
            Map.entry("barrier block", "minecraft:barrier"),
            Map.entry("barrier", "minecraft:barrier"),
            Map.entry("skull", "minecraft:skeleton_skull"),
            Map.entry("sword", "minecraft:iron_sword"),
            Map.entry("pickaxe", "minecraft:iron_pickaxe"),
            Map.entry("shovel", "minecraft:iron_shovel"),
            Map.entry("axe", "minecraft:iron_axe"),
            Map.entry("hoe", "minecraft:iron_hoe"),
            Map.entry("book and quill", "minecraft:writable_book"),
            Map.entry("book and quil", "minecraft:writable_book"),
            Map.entry("book", "minecraft:book"),
            Map.entry("firework", "minecraft:firework_rocket"),
            Map.entry("firework rocket", "minecraft:firework_rocket"),
            Map.entry("bonemeal", "minecraft:bone_meal"),
            Map.entry("bone meal", "minecraft:bone_meal"),
            Map.entry("name tag", "minecraft:name_tag"),
            Map.entry("gold ingot", "minecraft:gold_ingot"),
            Map.entry("iron ingot", "minecraft:iron_ingot"),
            Map.entry("sheers", "minecraft:shears"),
            Map.entry("sheer", "minecraft:shears"),
            Map.entry("mob spawner", "minecraft:spawner"),
            Map.entry("spawner", "minecraft:spawner"),
            Map.entry("rabbit stew", "minecraft:rabbit_stew"),
            Map.entry("milk", "minecraft:milk_bucket"),
            Map.entry("milk bucket", "minecraft:milk_bucket"),
            Map.entry("goat horn", "minecraft:goat_horn"),
            Map.entry("goat horns", "minecraft:goat_horn"),
            Map.entry("eye of ender", "minecraft:ender_eye"),
            Map.entry("powdered snow bucket", "minecraft:powder_snow_bucket"),
            Map.entry("spyglass", "minecraft:spyglass"),
            Map.entry("honeycomb", "minecraft:honeycomb"),
            Map.entry("wind charge", "minecraft:wind_charge"),
            Map.entry("wolf armor", "minecraft:wolf_armor"),
            Map.entry("wither rose", "minecraft:wither_rose"),
            Map.entry("lapis", "minecraft:lapis_lazuli"),
            Map.entry("redstone dust", "minecraft:redstone"),
            Map.entry("nether quartz", "minecraft:quartz"),
            Map.entry("quartz ore", "minecraft:nether_quartz_ore"),
            Map.entry("sheared pumpkin", "minecraft:carved_pumpkin"),
            Map.entry("sheard pumpkin", "minecraft:carved_pumpkin"),
            Map.entry("enchantment bottle", "minecraft:experience_bottle"),
            Map.entry("netherite ingot", "minecraft:netherite_ingot"),
            Map.entry("banner", "minecraft:white_banner"),
            Map.entry("stone brick", "minecraft:stone_bricks"),
            Map.entry("stone bricks", "minecraft:stone_bricks"),
            Map.entry("iron chain", "minecraft:iron_chain"),
            Map.entry("iron chains", "minecraft:iron_chain"),
            Map.entry("chain", "minecraft:iron_chain"),
            Map.entry("short grass", "minecraft:short_grass"),
            Map.entry("grass", "minecraft:short_grass")
    );

    private static final Map<String, EntityType<? extends LivingEntity>> MOB_ENTITIES = Map.ofEntries(
            Map.entry("zombie", EntityType.ZOMBIE),
            Map.entry("husk", EntityType.HUSK),
            Map.entry("drowned", EntityType.DROWNED),
            Map.entry("skeleton", EntityType.SKELETON),
            Map.entry("wither skeleton", EntityType.WITHER_SKELETON),
            Map.entry("creeper", EntityType.CREEPER),
            Map.entry("spider", EntityType.SPIDER),
            Map.entry("cave spider", EntityType.CAVE_SPIDER),
            Map.entry("enderman", EntityType.ENDERMAN),
            Map.entry("blaze", EntityType.BLAZE),
            Map.entry("slime", EntityType.SLIME),
            Map.entry("magma cube", EntityType.MAGMA_CUBE),
            Map.entry("phantom", EntityType.PHANTOM),
            Map.entry("shulker", EntityType.SHULKER),
            Map.entry("zombie villager", EntityType.ZOMBIE_VILLAGER),
            Map.entry("zombified piglin", EntityType.ZOMBIFIED_PIGLIN),
            Map.entry("zombie piglin", EntityType.ZOMBIFIED_PIGLIN),
            Map.entry("zombie pigman", EntityType.ZOMBIFIED_PIGLIN),
            Map.entry("piglin", EntityType.PIGLIN),
            Map.entry("piglin brute", EntityType.PIGLIN_BRUTE),
            Map.entry("hoglin", EntityType.HOGLIN),
            Map.entry("zoglin", EntityType.ZOGLIN),
            Map.entry("strider", EntityType.STRIDER),
            Map.entry("guardian", EntityType.GUARDIAN),
            Map.entry("elder guardian", EntityType.ELDER_GUARDIAN),
            Map.entry("endermite", EntityType.ENDERMITE),
            Map.entry("silverfish", EntityType.SILVERFISH),
            Map.entry("ghast", EntityType.GHAST),
            Map.entry("stray", EntityType.STRAY),
            Map.entry("witch", EntityType.WITCH),
            Map.entry("bee", EntityType.BEE),
            Map.entry("llama", EntityType.LLAMA),
            Map.entry("goat", EntityType.GOAT),
            Map.entry("horse", EntityType.HORSE),
            Map.entry("donkey", EntityType.DONKEY),
            Map.entry("cat", EntityType.CAT),
            Map.entry("ocelot", EntityType.OCELOT),
            Map.entry("wolf", EntityType.WOLF),
            Map.entry("bat", EntityType.BAT),
            Map.entry("parrot", EntityType.PARROT),
            Map.entry("fox", EntityType.FOX),
            Map.entry("rabbit", EntityType.RABBIT),
            Map.entry("chicken", EntityType.CHICKEN),
            Map.entry("cow", EntityType.COW),
            Map.entry("pig", EntityType.PIG),
            Map.entry("sheep", EntityType.SHEEP),
            Map.entry("mule", EntityType.MULE),
            Map.entry("nautilus", EntityType.NAUTILUS),
            Map.entry("dolphin", EntityType.DOLPHIN),
            Map.entry("frog", EntityType.FROG),
            Map.entry("camel", EntityType.CAMEL),
            Map.entry("panda", EntityType.PANDA),
            Map.entry("mooshroom", EntityType.MOOSHROOM),
            Map.entry("allay", EntityType.ALLAY),
            Map.entry("armadillo", EntityType.ARMADILLO),
            Map.entry("turtle", EntityType.TURTLE),
            Map.entry("polar bear", EntityType.POLAR_BEAR),
            Map.entry("snow golem", EntityType.SNOW_GOLEM),
            Map.entry("snowman", EntityType.SNOW_GOLEM),
            Map.entry("happy ghast", HAPPY_GHAST_ENTITY),
            Map.entry("dried ghast", EntityType.GHAST),
            Map.entry("ghastling", EntityType.GHAST),
            Map.entry("iron golem", EntityType.IRON_GOLEM),
            Map.entry("pillager", EntityType.PILLAGER),
            Map.entry("vindicator", EntityType.VINDICATOR),
            Map.entry("evoker", EntityType.EVOKER),
            Map.entry("vex", EntityType.VEX),
            Map.entry("ravager", EntityType.RAVAGER),
            Map.entry("villager", EntityType.VILLAGER),
            Map.entry("ender dragon", EntityType.ENDER_DRAGON),
            Map.entry("enderdragon", EntityType.ENDER_DRAGON),
            Map.entry("breeze", EntityType.BREEZE),
            Map.entry("bogged", EntityType.BOGGED),
            Map.entry("warden", EntityType.WARDEN),
            Map.entry("creaking", EntityType.CREAKING)
    );
    private static final List<String> MOB_ENTITY_KEYS = MOB_ENTITIES.keySet()
            .stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();

    private static class IconSpec {
        final ItemStack item;
        final Identifier texture;
        final QuestIconData.TextureRegion region;
        final EntityType<? extends LivingEntity> entityType;

        IconSpec(ItemStack item, Identifier texture, QuestIconData.TextureRegion region, EntityType<? extends LivingEntity> entityType) {
            this.item = item;
            this.texture = texture;
            this.region = region;
            this.entityType = entityType;
        }

        IconSpec(ItemStack item, Identifier texture) {
            this(item, texture, null, null);
        }

        IconSpec(ItemStack item, Identifier texture, QuestIconData.TextureRegion region) {
            this(item, texture, region, null);
        }

        IconSpec(EntityType<? extends LivingEntity> entityType) {
            this(com.jamie.jamiebingo.util.ItemStackUtil.empty(), null, null, entityType);
        }
    }

    public static QuestIconData iconFor(BingoSlot slot) {
        if (slot == null || slot.getId() == null || !slot.getId().startsWith("quest.")) {
            return QuestIconData.empty(com.jamie.jamiebingo.util.ItemStackUtil.empty());
        }
        String id = slot.getId();
        return CACHE.computeIfAbsent(id, QuestIconProvider::buildForQuestId);
    }

    private static QuestIconData buildForQuestId(String questId) {
        QuestDefinition def = QuestDatabase.getQuests().stream()
                .filter(q -> q.id.equals(questId))
                .findFirst()
                .orElse(null);

        QuestIconData special = buildSpecialQuestIcon(def);
        if (special != null) {
            return special;
        }

        if (def == null || def.texture == null || def.texture.isBlank()) {
            return QuestIconData.empty(item("minecraft:barrier"));
        }

        QuestIconData parsed = parseTexture(def.texture);
        QuestIconData finalIcon = applyQuestEntityOverride(def, parsed);
        if (DEBUG_LOGGED_IDS.add(questId) && finalIcon != null) {
            String main = finalIcon.mainEntityType == null ? "none" : String.valueOf(finalIcon.mainEntityType);
            String corner = finalIcon.cornerEntityType == null ? "none" : String.valueOf(finalIcon.cornerEntityType);
            int rotating = finalIcon.rotatingEntities == null ? 0 : finalIcon.rotatingEntities.size();
            System.out.println("[JamieBingo] Quest icon entity: " + questId
                    + " | main=" + main
                    + " | corner=" + corner
                    + " | rotating=" + rotating);
        }
        return finalIcon;
    }

    private static QuestIconData buildSpecialQuestIcon(QuestDefinition def) {
        if (def == null || def.id == null) return null;

        QuestIconData newQuestOverride = buildNewQuestIconOverride(def);
        if (newQuestOverride != null) return newQuestOverride;

        if (def.id.startsWith("quest.wear_") && def.id.contains("_colored_leather_")) {
            ItemStack colored = buildColoredLeather(def.id);
            if (!colored.isEmpty()) {
                return QuestIconData.fromItems(
                        colored,
                        List.of(),
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:armor_stand"),
                        null
                );
            }
        }

        if (def.id.equals("quest.wear_1_piece_of_chain_armor")) {
            List<ItemStack> chainPieces = List.of(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:chainmail_boots"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:chainmail_leggings"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:chainmail_chestplate"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:chainmail_helmet")
            );
            return QuestIconData.fromItems(
                    chainPieces.get(0),
                    chainPieces,
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:armor_stand"),
                    null
            );
        }

        if (def.id.startsWith("quest.kill_") && def.id.endsWith("_sheep")) {
            QuestIconData sheep = buildColoredSheepKillIcon(def.id);
            if (sheep != null) return sheep;
        }

        if (def.id.equals("quest.obtain_every_type_of_sword")) {
            List<ItemStack> swords = List.of(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_sword"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_sword"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_sword"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_sword"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_sword"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_sword"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherite_sword")
            );
            return QuestIconData.fromItems(
                    swords.get(0),
                    swords,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null
            );
        }

        if (def.id.equals("quest.obtain_every_type_of_pickaxe")) {
            List<ItemStack> picks = List.of(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_pickaxe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_pickaxe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_pickaxe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_pickaxe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_pickaxe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_pickaxe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherite_pickaxe")
            );
            return QuestIconData.fromItems(
                    picks.get(0),
                    picks,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null
            );
        }

        if (def.id.equals("quest.die_by_drowning")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:skeleton_skull"),
                    null,
                    WATER_TEXTURE,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.look_at_20_unique_mobs_with_a_spyglass")) {
            return buildSpyglassUniqueMobsIcon(20);
        }

        if (def.id.equals("quest.look_at_15_unique_mobs_with_a_spyglass")) {
            return buildSpyglassUniqueMobsIcon(15);
        }

        if (def.id.equals("quest.look_at_10_unique_mobs_with_a_spyglass")) {
            return buildSpyglassUniqueMobsIcon(10);
        }

        if (def.id.equals("quest.look_at_5_unique_mobs_with_a_spyglass")) {
            return buildSpyglassUniqueMobsIcon(5);
        }

        if (def.id.equals("quest.opponent_stands_on_netherrack")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherrack"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"),
                    null,
                    null,
                    List.of(),
                    BARRIER_ITEM_TEXTURE,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.opponent_stands_on_stone")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"),
                    null,
                    null,
                    List.of(),
                    BARRIER_ITEM_TEXTURE,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.build_all_golems")) {
            QuestIconData blocks = buildItemCycle(
                    List.of("snow_block", "copper_block", "iron_block"),
                    null,
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:carved_pumpkin")
            );
            if (blocks != null) return blocks;
        }

        if (def.id.equals("quest.kill_all_golems")) {
            List<EntityType<? extends LivingEntity>> golems = new ArrayList<>();
            golems.add(EntityType.IRON_GOLEM);
            EntityType<? extends LivingEntity> copper = entityType("minecraft:copper_golem");
            if (copper != null) {
                golems.add(copper);
            }
            golems.add(EntityType.SNOW_GOLEM);
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_sword"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    golems.get(0),
                    golems,
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.visit_all_cave_biomes")) {
            QuestIconData blocks = buildItemCycle(
                    List.of("big_dripleaf", "dripstone_block", "sculk_sensor"),
                    null,
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots")
            );
            if (blocks != null) return blocks;
        }

        if (def.id.equals("quest.visit_10_unique_biomes")) {
            QuestIconData biomes = buildBiomeVisitIcon("10");
            if (biomes != null) return biomes;
        }
        if (def.id.equals("quest.visit_15_unique_biomes")) {
            QuestIconData biomes = buildBiomeVisitIcon("15");
            if (biomes != null) return biomes;
        }
        if (def.id.equals("quest.visit_20_unique_biomes")) {
            QuestIconData biomes = buildBiomeVisitIcon("20");
            if (biomes != null) return biomes;
        }

        if (def.id.equals("quest.obtain_all_nautilus_armor_types")) {
            List<ItemStack> armor = new ArrayList<>(List.of(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_nautilus_armor"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_nautilus_armor"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_nautilus_armor"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_nautilus_armor"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherite_nautilus_armor")
            ));
            armor.removeIf(ItemStack::isEmpty);
            return QuestIconData.fromItems(
                    armor.get(0),
                    armor,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null
            );
        }

        if (def.id.equals("quest.tame_all_wolf_types")) {
            return buildVariantMobIcon(
                    EntityType.WOLF,
                    Registries.WOLF_VARIANT,
                    List.of(
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:pale"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:ashen"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:black"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:chestnut"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:rusty"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:snowy"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:spotted"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:striped"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:woods")
                    ),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:bone"),
                    "variants"
            );
        }

        if (def.id.equals("quest.breed_all_pig_variants")) {
            return buildVariantMobIcon(
                    EntityType.PIG,
                    Registries.PIG_VARIANT,
                    List.of(
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:temperate"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:warm"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:cold")
                    ),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:potato"),
                    "variants"
            );
        }

        if (def.id.equals("quest.breed_all_cow_variants")) {
            return buildVariantMobIcon(
                    EntityType.COW,
                    Registries.COW_VARIANT,
                    List.of(
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:temperate"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:warm"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:cold")
                    ),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wheat"),
                    "variants"
            );
        }

        if (def.id.equals("quest.breed_all_chicken_variants")) {
            return buildVariantMobIcon(
                    EntityType.CHICKEN,
                    Registries.CHICKEN_VARIANT,
                    List.of(
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:temperate"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:warm"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:cold")
                    ),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wheat_seeds"),
                    "variants"
            );
        }

        if (def.id.equals("quest.tame_every_cat_variant")) {
            return buildVariantMobIcon(
                    EntityType.CAT,
                    Registries.CAT_VARIANT,
                    List.of(
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:all_black"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:black"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:british_shorthair"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:calico"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:jellie"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:persian"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:ragdoll"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:red"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:siamese"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:tabby"),
                            com.jamie.jamiebingo.util.IdUtil.id("minecraft:white")
                    ),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:salmon"),
                    "variants"
            );
        }

        if (def.id.equals("quest.breed_frogs")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:slime_ball"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    EntityType.FROG,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.breed_wolf")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:beef"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    EntityType.WOLF,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.breed_panda")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:bamboo"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    EntityType.PANDA,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.breed_mooshroom")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wheat"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    EntityType.MOOSHROOM,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.breed_turtle")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:seagrass"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    EntityType.TURTLE,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.breed_armadillo")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:spider_eye"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    EntityType.ARMADILLO,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.die_to_warden")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:skeleton_skull"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    EntityType.WARDEN,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.die_to_creaking")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:skeleton_skull"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    EntityType.CREAKING,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.summon_iron_golem")) {
            return QuestIconData.fromItems(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_block"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:carved_pumpkin"),
                    null
            );
        }

        if (def.id.equals("quest.open_trial_vault")) {
            ItemStack vault = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:vault");
            ItemStack key = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:trial_key");
            if (!vault.isEmpty()) {
                return QuestIconData.fromItems(
                        vault,
                        List.of(),
                        key.isEmpty() ? com.jamie.jamiebingo.util.ItemStackUtil.empty() : key,
                        null
                );
            }
            return QuestIconData.fromTextures(
                    com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/block/vault_front_on.png"),
                    List.of(),
                    key.isEmpty() ? null : com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/item/trial_key.png"),
                    null
            );
        }

        if (def.id.equals("quest.open_ominous_vault")) {
            ItemStack vault = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:vault");
            ItemStack key = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:ominous_trial_key");
            if (!vault.isEmpty()) {
                return QuestIconData.fromItems(
                        vault,
                        List.of(),
                        key,
                        null
                );
            }
            return QuestIconData.fromTextures(
                    com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/block/vault_front_on_ominous.png"),
                    List.of(),
                    key.isEmpty() ? null : com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/item/ominous_trial_key.png"),
                    null
            );
        }

        if (def.id.equals("quest.trample_farmland")) {
            return QuestIconData.fromItems(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:farmland"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"),
                    null
            );
        }

        if (def.id.equals("quest.apply_armor_trim")) {
            QuestIconData trims = buildItemCycleByPath(
                    path -> path.contains("armor_trim_smithing_template"),
                    null,
                    "sentry_armor_trim_smithing_template"
            );
            if (trims != null) {
                return QuestIconData.full(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:smithing_table"),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        List.of(),
                        trims.rotatingIcons,
                        List.of()
                );
            }
        }

        if (def.id.equals("quest.opponent_wears_armor")) {
            QuestIconData armor = buildArmorCycle(null, com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:barrier"));
            if (armor != null) return armor;
        }

        if (def.id.equals("quest.obtain_6_unique_buckets")) {
            QuestIconData buckets = buildItemCycleByPath(
                    path -> path.endsWith("_bucket"),
                    "6",
                    "bucket"
            );
            if (buckets != null) return buckets;
        }

        if (def.id.equals("quest.obtain_6_unique_brick_blocks")) {
            QuestIconData bricks = buildItemCycleByPath(
                    path -> path.contains("bricks"),
                    "6",
                    null
            );
            if (bricks != null) return bricks;
        }

        if (def.id.equals("quest.obtain_5_unique_pressure_plates")) {
            QuestIconData plates = buildItemCycleByPath(
                    path -> path.endsWith("_pressure_plate"),
                    "5",
                    "oak_pressure_plate"
            );
            if (plates != null) return plates;
        }

        if (def.id.equals("quest.obtain_3_unique_music_disks")) {
            QuestIconData discs = buildItemCycleByPath(
                    path -> path.startsWith("music_disc_"),
                    "3",
                    "music_disc_13"
            );
            if (discs != null) return discs;
        }

        if (def.id.equals("quest.obtain_all_wool_blocks")) {
            QuestIconData wool = buildItemCycleByPath(
                    path -> path.endsWith("_wool"),
                    null,
                    "white_wool"
            );
            if (wool != null) return wool;
        }

        if (def.id.equals("quest.obtain_all_froglights")) {
            QuestIconData lights = buildItemCycle(
                    List.of("pearlescent_froglight", "verdant_froglight", "ochre_froglight"),
                    null,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty()
            );
            if (lights != null) return lights;
        }

        if (def.id.equals("quest.obtain_all_mushrooms")) {
            QuestIconData mush = buildItemCycle(
                    List.of("brown_mushroom", "red_mushroom", "warped_fungus", "crimson_fungus"),
                    null,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty()
            );
            if (mush != null) return mush;
        }

        if (def.id.equals("quest.obtain_all_pumpkin_types")) {
            QuestIconData pumpkins = buildItemCycle(
                    List.of("pumpkin", "carved_pumpkin", "jack_o_lantern"),
                    null,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty()
            );
            if (pumpkins != null) return pumpkins;
        }

        if (def.id.equals("quest.obtain_all_rail_types")) {
            QuestIconData rails = buildItemCycleByPath(
                    path -> path.equals("rail") || path.endsWith("_rail"),
                    null,
                    "rail"
            );
            if (rails != null) return rails;
        }

        if (def.id.equals("quest.obtain_all_minecart_types")) {
            QuestIconData carts = buildItemCycleByPath(
                    path -> path.contains("minecart"),
                    null,
                    "minecart"
            );
            if (carts != null) return carts;
        }

        if (def.id.equals("quest.obtain_all_log_types")
                || def.id.equals("quest.obtain_6_log_variants")
                || def.id.equals("quest.obtain_8_log_variants")) {
            String number = null;
            if (def.id.equals("quest.obtain_6_log_variants")) number = "6";
            if (def.id.equals("quest.obtain_8_log_variants")) number = "8";
            QuestIconData logs = buildItemCycle(
                    List.of(
                            "oak_log",
                            "spruce_log",
                            "birch_log",
                            "jungle_log",
                            "acacia_log",
                            "dark_oak_log",
                            "mangrove_log",
                            "cherry_log",
                            "crimson_stem",
                            "warped_stem"
                    ),
                    number,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty()
            );
            if (logs != null) return logs;
        }

        if (def.id.equals("quest.obtain_all_nether_ore_blocks")) {
            QuestIconData netherBlocks = buildItemCycle(
                    List.of("nether_gold_ore", "nether_quartz_ore", "ancient_debris"),
                    null,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty()
            );
            if (netherBlocks != null) return netherBlocks;
        }

        if (def.id.equals("quest.obtain_all_nether_ores")) {
            QuestIconData netherOres = buildItemCycle(
                    List.of("gold_nugget", "quartz", "netherite_scrap"),
                    null,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty()
            );
            if (netherOres != null) return netherOres;
        }

        if (def.id.equals("quest.obtain_all_overworld_ore_blocks")) {
            QuestIconData blocks = buildItemCycle(
                    List.of(
                            "coal_ore", "deepslate_coal_ore",
                            "iron_ore", "deepslate_iron_ore",
                            "lapis_ore", "deepslate_lapis_ore",
                            "copper_ore", "deepslate_copper_ore",
                            "gold_ore", "deepslate_gold_ore",
                            "redstone_ore", "deepslate_redstone_ore",
                            "diamond_ore", "deepslate_diamond_ore",
                            "emerald_ore", "deepslate_emerald_ore"
                    ),
                    null,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty()
            );
            if (blocks != null) return blocks;
        }

        if (def.id.equals("quest.obtain_all_overworld_ores")) {
            QuestIconData items = buildItemCycle(
                    List.of("coal", "copper_ingot", "lapis_lazuli", "iron_ingot", "redstone", "gold_ingot", "diamond", "emerald"),
                    null,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty()
            );
            if (items != null) return items;
        }

        if (def.id.equals("quest.break_any_piece_of_armor")) {
            QuestIconData armor = buildArmorCycle("0%", com.jamie.jamiebingo.util.ItemStackUtil.empty());
            if (armor != null) return armor;
        }

        if (def.id.equals("quest.break_any_tool")) {
            QuestIconData tools = buildToolCycle("0%", com.jamie.jamiebingo.util.ItemStackUtil.empty());
            if (tools != null) return tools;
        }

        if (def.id.equals("quest.wear_four_unique_armor_pieces")) {
            QuestIconData wear = buildItemCycle(
                    List.of("netherite_helmet", "leather_chestplate", "iron_leggings", "golden_boots"),
                    null,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty()
            );
            if (wear != null) return wear;
        }

        if (def.id.equals("quest.wear_full_uniquely_colored_armor")
                || def.id.equals("quest.wear_full_uniquely_coloured_leather_armor")) {
            List<ItemStack> items = new ArrayList<>();
            for (net.minecraft.world.item.DyeColor dye : net.minecraft.world.item.DyeColor.values()) {
                String colorName = dye.getName();
                ItemStack helmet = buildColoredLeather("quest.wear_" + colorName + "_colored_leather_helmet");
                ItemStack chest = buildColoredLeather("quest.wear_" + colorName + "_colored_leather_chestplate");
                ItemStack legs = buildColoredLeather("quest.wear_" + colorName + "_colored_leather_leggings");
                ItemStack boots = buildColoredLeather("quest.wear_" + colorName + "_colored_leather_boots");
                if (helmet.isEmpty()) helmet = buildColoredLeatherItem(colorName, "helmet");
                if (chest.isEmpty()) chest = buildColoredLeatherItem(colorName, "chestplate");
                if (legs.isEmpty()) legs = buildColoredLeatherItem(colorName, "leggings");
                if (boots.isEmpty()) boots = buildColoredLeatherItem(colorName, "boots");
                items.add(helmet);
                items.add(chest);
                items.add(legs);
                items.add(boots);
            }
            items.removeIf(ItemStack::isEmpty);
            if (items.isEmpty()) return null;
            return QuestIconData.fromItems(
                items.get(0),
                    items,
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:armor_stand"),
                    null
            );
        }

        if (def.id.equals("quest.get_15_advancements")) {
            QuestIconData adv = buildAdvancementIcon("minecraft:story/root", "15", com.jamie.jamiebingo.util.ItemStackUtil.empty());
            if (adv != null) return adv;
        }
        if (def.id.equals("quest.get_25_advancements")) {
            QuestIconData adv = buildAdvancementIcon("minecraft:story/root", "25", com.jamie.jamiebingo.util.ItemStackUtil.empty());
            if (adv != null) return adv;
        }
        if (def.id.equals("quest.get_35_advancements")) {
            QuestIconData adv = buildAdvancementIcon("minecraft:story/root", "35", com.jamie.jamiebingo.util.ItemStackUtil.empty());
            if (adv != null) return adv;
        }

        if (def.id.equals("quest.opponent_obtains_advancement")) {
            QuestIconData adv = buildAdvancementIcon("minecraft:story/root", null,
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:barrier"));
            if (adv != null) return adv;
        }

        if (def.id.equals("quest.get_bullseye_advancement")) {
            QuestIconData adv = buildAdvancementIcon(
                    "minecraft:adventure/bullseye",
                    null,
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:target")
            );
            if (adv != null) return adv;
        }

        if (def.id.equals("quest.get_sniper_duel_advancement")) {
            QuestIconData adv = buildAdvancementIcon(
                    "minecraft:adventure/sniper_duel",
                    null,
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:bow")
            );
            if (adv != null) return adv;
        }

        if (def.id.equals("quest.do_power_of_books_advancement")) {
            QuestIconData adv = buildAdvancementIcon("minecraft:adventure/power_of_books", null,
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:chiseled_bookshelf"));
            if (adv != null) return adv;
        }

        if (def.id.equals("quest.obtain_the_mob_kabob_advancement")) {
            QuestIconData adv = buildAdvancementIcon(
                    "minecraft:adventure/root",
                    null,
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_spear")
            );
            if (adv != null) return adv;
        }

        if (def.id.equals("quest.get_any_spyglass_advancement")) {
            QuestIconData adv = buildAdvancementIcon(
                    "minecraft:adventure/spyglass_at_parrot",
                    null,
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:spyglass")
            );
            if (adv != null) return adv;
        }

        if (def.id.equals("quest.get_this_boat_has_legs_advancement")) {
            QuestIconData adv = buildAdvancementIcon(
                    "minecraft:adventure/this_boat_has_legs",
                    null,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    EntityType.STRIDER
            );
            if (adv != null) return adv;
        }

        if (def.id.equals("quest.get_subspace_bubble_advancement")) {
            QuestIconData adv = buildAdvancementIcon(
                    "minecraft:nether/fast_travel",
                    "875",
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    NETHER_PORTAL_TEXTURE,
                    null
            );
            if (adv != null) return adv;
        }

        if (def.id.equals("quest.do_star_trader_advancement")) {
            QuestIconData adv = buildAdvancementIcon(
                    "minecraft:adventure/trade",
                    null,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    effectTexture("levitation"),
                    EntityType.VILLAGER
            );
            if (adv != null) return adv;
        }

        if (def.id.equals("quest.do_caves_and_cliffs_advancement")) {
            QuestIconData adv = buildAdvancementIcon(
                    "minecraft:adventure/caves_and_cliffs",
                    null,
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:bedrock"),
                    effectTexture("levitation"),
                    null
            );
            if (adv != null) return adv;
        }

        if (def.id.equals("quest.whole_team_wear_a_piece_of_enchanted_armor")) {
            ItemStack enchanted = buildGlintArmor();
            if (!enchanted.isEmpty()) {
                return QuestIconData.fromItems(
                    enchanted,
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null
                ).withCornerCopies(2);
            }
        }

        if (def.id.equals("quest.tame_mule")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    HEART_TEXTURE,
                    null,
                    null,
                    EntityType.MULE,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }


        if (def.id.equals("quest.full_set_of_gold_tools")) {
            List<ItemStack> tools = List.of(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_shovel"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_hoe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_sword"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_axe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_pickaxe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_spear")
            );
            return QuestIconData.fromItems(
                    tools.get(0),
                    tools,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null
            );
        }

        if (def.id.equals("quest.full_set_of_iron_tools")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_shovel"),
                    List.of(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_shovel"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_hoe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_sword"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_axe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_pickaxe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_spear")
            ),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.find_an_end_city")) {
            return QuestIconData.fromItems(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:purpur_block"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"),
                    null
            );
        }

        if (def.id.equals("quest.visit_all_nether_biomes")) {
            List<ItemStack> biomes = List.of(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherrack"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:soul_sand"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:crimson_stem"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:warped_stem"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:basalt")
            );
            return QuestIconData.fromItems(
                    biomes.get(0),
                    biomes,
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"),
                    null
            );
        }

        if (def.id.equals("quest.obtain_more_hoppers_than_the_opponent")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:hopper"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    effectTexture("jump boost"),
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.obtain_more_dried_kelp_blocks_than_the_opponent")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:dried_kelp_block"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    effectTexture("jump boost"),
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.eat_10_unique_foods")) {
            List<ItemStack> foods = List.of(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:apple"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cooked_beef"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:baked_potato"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:bread"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cooked_chicken"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cooked_porkchop"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_carrot"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cooked_cod")
            );
            return QuestIconData.full(
                    foods.get(0),
                    foods,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    "10",
                    null,
                    List.of(),
                    HUNGER_FULL_TEXTURE,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.eat_5_unique_foods")) {
            List<ItemStack> foods = List.of(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:apple"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cooked_beef"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:baked_potato"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:bread"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cooked_chicken"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cooked_porkchop"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_carrot"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cooked_cod")
            );
            return QuestIconData.full(
                    foods.get(0),
                    foods,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    "5",
                    null,
                    List.of(),
                    HUNGER_FULL_TEXTURE,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.eat_20_unique_foods")) {
            List<ItemStack> foods = List.of(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:apple"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cooked_beef"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:baked_potato"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:bread"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cooked_chicken"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cooked_porkchop"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_carrot"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cooked_cod")
            );
            return QuestIconData.full(
                    foods.get(0),
                    foods,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    "20",
                    null,
                    List.of(),
                    HUNGER_FULL_TEXTURE,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.kill_zombie_villager")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_sword"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    EntityType.ZOMBIE_VILLAGER,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.obtain_every_type_of_horse_armor")) {
            List<ItemStack> armors = List.of(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_horse_armor"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_horse_armor"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_horse_armor"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_horse_armor")
            );
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    EntityType.HORSE,
                    List.of(),
                    null,
                    List.of(),
                    armors,
                    List.of()
            );
        }

        if (def.id.equals("quest.reach_bedrock")) {
            return QuestIconData.fromItems(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:bedrock"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"),
                    null
            );
        }

        if (def.id.equals("quest.reach_sky_limit")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"),
                    null,
                    effectTexture("levitation"),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.find_a_fortress")) {
            return QuestIconData.fromItems(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:nether_bricks"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"),
                    null
            );
        }


        if (def.id.equals("quest.find_a_stronghold")) {
            return QuestIconData.fromItems(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:ender_eye"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"),
                    null
            );
        }

        if (def.id.equals("quest.an_opponent_touches_water")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:barrier"),
                    null,
                    WATER_TEXTURE,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.an_opponent_catches_on_fire")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:barrier"),
                    null,
                    FIRE_TEXTURE,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.remove_a_status_effect_with_a_milk_bucket")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:milk_bucket"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    HUNGER_FULL_TEXTURE,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of(
                            effectTexture("absorption"),
                            effectTexture("jump boost"),
                            effectTexture("poison"),
                            effectTexture("weakness"),
                            effectTexture("glowing"),
                            effectTexture("levitation"),
                            effectTexture("mining fatigue"),
                            effectTexture("nausea"),
                            effectTexture("bad omen"),
                            effectTexture("swiftness"),
                            effectTexture("water breathing"),
                            effectTexture("wither"),
                            effectTexture("slowness")
                    )
            ).withCornerCopies(2);
        }

        if (def.id.equals("quest.have_more_levels_than_the_opponent")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:experience_bottle"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    effectTexture("jump boost"),
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.deal_400_damage")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_sword"),
                    "400",
                    HEART_TEXTURE,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.sleep_alone_in_the_overworld")) {
            return QuestIconData.fromItems(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:red_bed"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:player_head"),
                    null
            );
        }

        if (def.id.equals("quest.sprint_1km")) {
            return QuestIconData.fromTextures(
                    effectTexture("swiftness"),
                    List.of(),
                    null,
                    "1km"
            );
        }

        if (def.id.startsWith("quest.eat_")) {
            QuestIconData base = def.texture != null ? parseTexture(def.texture) : null;
            if (base != null && base.cornerTexture == null && base.cornerIcon.isEmpty()) {
                return QuestIconData.full(
                        base.mainIcon,
                        base.rotatingIcons,
                        base.cornerIcon,
                        base.numberText,
                        base.mainTexture,
                        base.rotatingTextures,
                        HUNGER_FULL_TEXTURE,
                        base.mainRegion,
                        base.cornerRegion,
                        base.mainEntityType,
                        base.rotatingEntities,
                        base.cornerEntityType,
                        base.rotatingCornerEntities,
                        base.rotatingCornerIcons,
                        base.rotatingCornerTextures
                );
            }
        }

        if (def.id.equals("quest.reach_level_15")) {
            return QuestIconData.fromItems(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:experience_bottle"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    "15"
            );
        }

        if (def.id.equals("quest.reach_level_30")) {
            return QuestIconData.fromItems(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:experience_bottle"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    "30"
            );
        }

        if (def.id.contains("_more_") && def.id.contains("_than_the_opponent")) {
            QuestIconData base = def.texture != null ? parseTexture(def.texture) : null;
            if (base != null) {
                return QuestIconData.full(
                        base.mainIcon,
                        base.rotatingIcons,
                        base.cornerIcon,
                        base.numberText,
                        base.mainTexture,
                        base.rotatingTextures,
                        effectTexture("jump boost"),
                        base.mainRegion,
                        base.cornerRegion,
                        base.mainEntityType,
                        base.rotatingEntities,
                        base.cornerEntityType,
                        base.rotatingCornerEntities,
                        base.rotatingCornerIcons,
                        base.rotatingCornerTextures
                );
            }
        }

        if (def.id.equals("quest.kill_7_unique_hostile_mobs")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_sword"),
                    "7",
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    EntityType.ZOMBIE,
                    List.of(
                            EntityType.SKELETON,
                            EntityType.CREEPER,
                            EntityType.SPIDER,
                            EntityType.ENDERMAN,
                            EntityType.WITCH,
                            EntityType.GUARDIAN,
                            EntityType.ZOMBIE_VILLAGER
                    ),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.kill_30_undead_mobs")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_sword"),
                    "30",
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    EntityType.ZOMBIE,
                    List.of(
                            EntityType.SKELETON,
                            EntityType.WITHER_SKELETON,
                            EntityType.DROWNED,
                            EntityType.ZOMBIE_VILLAGER,
                            EntityType.STRAY
                    ),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.take_200_damage")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:player_head"),
                    "200",
                    HEART_TEXTURE,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        if (def.id.equals("quest.breed_4_unique_animals")) {
            return buildBreedUniqueAnimalsIcon(4);
        }
        if (def.id.equals("quest.breed_6_unique_animals")) {
            return buildBreedUniqueAnimalsIcon(6);
        }
        if (def.id.equals("quest.breed_8_unique_animals")) {
            return buildBreedUniqueAnimalsIcon(8);
        }
        if (def.id.equals("quest.obtain_64_of_any_one_item_block")) {
            return QuestIconData.fromItems(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:chest"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    "64"
            );
        }
        if (def.id.equals("quest.fill_inventory_with_unique_items")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:chest"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    INFINITY_TEXTURE,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.distract_a_piglin_with_gold")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:gold_ingot"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    EntityType.PIGLIN,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.die_to_intentional_game_design")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:red_bed"),
                    List.of(
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:red_bed"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:respawn_anchor")
                    ),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:skeleton_skull"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.an_opponent_jumps")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:barrier"),
                    null,
                    effectTexture("jump boost"),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.an_opponent_catches_on_fire")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.<ItemStack>of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:barrier"),
                    null,
                    FIRE_TEXTURE,
                    List.<Identifier>of(),
                    null,
                    null,
                    null,
                    null,
                    List.<EntityType<? extends LivingEntity>>of(),
                    null,
                    List.<EntityType<? extends LivingEntity>>of(),
                    List.<ItemStack>of(),
                    List.<Identifier>of()
            );
        }
        if (def.id.equals("quest.an_opponent_touches_water")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.<ItemStack>of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:barrier"),
                    null,
                    WATER_TEXTURE,
                    List.<Identifier>of(),
                    null,
                    null,
                    null,
                    null,
                    List.<EntityType<? extends LivingEntity>>of(),
                    null,
                    List.<EntityType<? extends LivingEntity>>of(),
                    List.<ItemStack>of(),
                    List.<Identifier>of()
            );
        }
        if (def.id.equals("quest.an_opponent_takes_100_total_damage")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:barrier"),
                    "100",
                    HEART_TEXTURE,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.an_opponent_takes_fall_damage")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:barrier"),
                    null,
                    effectTexture("levitation"),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.sleep_alone_in_the_overworld")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:red_bed"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    ZZZ_TEXTURE,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.enter_nether")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    NETHER_PORTAL_TEXTURE,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.use_a_brewing_stand")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:brewing_stand"),
                    List.of(),
                    waterBottle(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.use_an_anvil")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:anvil"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:name_tag"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.fill_an_armor_stand_with_armor")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:armor_stand"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_leggings"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_chestplate"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_helmet")
                    ),
                    List.of()
            );
        }
        if (def.id.equals("quest.wear_iron_armor_set")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_boots"),
                    List.of(
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_boots"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_leggings"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_chestplate"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_helmet")
                    ),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:armor_stand"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.wear_copper_armor_set")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_boots"),
                    List.of(
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_boots"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_leggings"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_chestplate"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_helmet")
                    ),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:armor_stand"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.wear_gold_armor_set")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_boots"),
                    List.of(
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_boots"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_leggings"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_chestplate"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_helmet")
                    ),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:armor_stand"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.wear_leather_armor_set")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"),
                    List.of(
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_leggings"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_chestplate"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_helmet")
                    ),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:armor_stand"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.full_set_of_wooden_tools")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_shovel"),
                    List.of(
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_shovel"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_hoe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_sword"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_axe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_pickaxe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_spear")
            ),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.full_set_of_stone_tools")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_shovel"),
                    List.of(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_shovel"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_hoe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_sword"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_axe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_pickaxe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_spear")
            ),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.full_set_of_diamond_tools")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_shovel"),
                    List.of(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_shovel"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_hoe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_sword"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_axe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_pickaxe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_spear")
            ),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.obtain_every_type_of_axe")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_axe"),
                    List.of(
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_axe"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_axe"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_axe"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_axe"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_axe"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_axe"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherite_axe")
                    ),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.obtain_every_type_of_hoe")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_hoe"),
                    List.of(
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_hoe"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_hoe"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_hoe"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_hoe"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_hoe"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_hoe"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherite_hoe")
                    ),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.obtain_every_type_of_seed")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wheat_seeds"),
                    List.of(
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wheat_seeds"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:beetroot_seeds"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:melon_seeds"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:pumpkin_seeds")
                    ),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.kill_15_unique_hostile_mobs")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_sword"),
                    "15",
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    EntityType.ZOMBIE,
                    List.of(
                            EntityType.ZOMBIE,
                            EntityType.HUSK,
                            EntityType.DROWNED,
                            EntityType.SKELETON,
                            EntityType.WITHER_SKELETON,
                            EntityType.CREEPER,
                            EntityType.SPIDER,
                            EntityType.CAVE_SPIDER,
                            EntityType.ENDERMAN,
                            EntityType.BLAZE,
                            EntityType.SLIME,
                            EntityType.MAGMA_CUBE,
                            EntityType.PHANTOM,
                            EntityType.SHULKER,
                            EntityType.WITCH,
                            EntityType.GUARDIAN,
                            EntityType.ELDER_GUARDIAN,
                            EntityType.GHAST,
                            EntityType.STRAY,
                            EntityType.ENDERMITE,
                            EntityType.SILVERFISH,
                            EntityType.ZOMBIE_VILLAGER,
                            EntityType.ZOMBIFIED_PIGLIN,
                            EntityType.PIGLIN,
                            EntityType.HOGLIN,
                            EntityType.ZOGLIN
                    ),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.kill_wither_skeleton")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_sword"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    EntityType.WITHER_SKELETON,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.find_a_bastion")) {
            return QuestIconData.fromItems(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:gilded_blackstone"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"),
                    null
            );
        }
        if (def.id.equals("quest.obtain_all_netherite_tools")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherite_shovel"),
                    List.of(
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherite_shovel"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherite_hoe"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherite_sword"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherite_axe"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherite_pickaxe"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherite_spear")
                    ),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.use_an_enchanting_table")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:enchanting_table"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:experience_bottle"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.use_a_smithing_table")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:smithing_table"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherite_ingot"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.get_mining_fatigue")) {
            return QuestIconData.fromTextures(
                    effectTexture("mining fatigue"),
                    List.of(),
                    null,
                    null
            );
        }
        if (def.id.equals("quest.obtain_every_type_of_shovel")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_shovel"),
                    List.of(
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_shovel"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_shovel"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_shovel"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_shovel"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_shovel"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_shovel"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherite_shovel")
                    ),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.mine_diamond_ore")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_ore"),
                    List.of(
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_ore"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:deepslate_diamond_ore")
                    ),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_pickaxe"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.mine_emerald_ore")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:emerald_ore"),
                    List.of(
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:emerald_ore"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:deepslate_emerald_ore")
                    ),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_pickaxe"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.mine_turtle_egg")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:turtle_egg"),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_pickaxe"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.obtain_every_type_of_raw_ore_block")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:raw_iron_block"),
                    List.of(
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:raw_iron_block"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:raw_gold_block"),
                            com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:raw_copper_block")
                    ),
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.equals("quest.enrage_a_zombie_piglin")) {
            return QuestIconData.full(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    List.of(),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_sword"),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    EntityType.ZOMBIFIED_PIGLIN,
                    List.of(),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        if (def.id.startsWith("quest.wear_") && def.id.contains("_colored_leather_")) {
            ItemStack stack = buildColoredLeather(def.id);
            if (!stack.isEmpty()) {
                return QuestIconData.fromItems(
                        stack,
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null
                );
            }
        }

        if (def.id.equals("quest.collect_all_spears")) {
            List<ItemStack> spears = List.of(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_spear"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_spear"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_spear"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_spear"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_spear"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_spear"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherite_spear")
            );
            return QuestIconData.fromItems(
                    spears.get(0),
                    spears,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null
            );
        }

        if (def.id.equals("quest.obtain_all_copper_tools")) {
            List<ItemStack> copperTools = List.of(
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_shovel"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_hoe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_sword"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_axe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_pickaxe"),
                    com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_spear")
            );
            return QuestIconData.fromItems(
                    copperTools.get(0),
                    copperTools,
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    null
            );
        }

        return null;
    }

    private static QuestIconData buildBreedUniqueAnimalsIcon(int count) {
        List<EntityType<? extends LivingEntity>> animals = List.of(
                EntityType.CHICKEN,
                EntityType.PIG,
                EntityType.COW,
                EntityType.CAT,
                EntityType.OCELOT,
                EntityType.WOLF,
                EntityType.HORSE,
                EntityType.GOAT,
                EntityType.SHEEP,
                EntityType.PARROT,
                EntityType.HOGLIN,
                EntityType.STRIDER
        );

        List<ItemStack> items = List.of(
                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wheat_seeds"),
                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:potato"),
                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wheat"),
                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cod"),
                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cod"),
                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:beef"),
                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_carrot"),
                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wheat"),
                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wheat"),
                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wheat_seeds"),
                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:crimson_fungus"),
                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:warped_fungus")
        );

        return QuestIconData.full(
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                List.of(),
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                String.valueOf(count),
                null,
                List.of(),
                null,
                null,
                null,
                animals.get(0),
                animals,
                null,
                List.of(),
                items,
                List.of()
        );
    }

    private static QuestIconData buildSpyglassUniqueMobsIcon(int count) {
        return QuestIconData.full(
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                List.of(),
                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:spyglass"),
                String.valueOf(count),
                null,
                List.of(),
                null,
                null,
                null,
                EntityType.ZOMBIE,
                List.of(
                        EntityType.SKELETON,
                        EntityType.CREEPER,
                        EntityType.SPIDER,
                        EntityType.ENDERMAN,
                        EntityType.COW,
                        EntityType.PIG,
                        EntityType.SHEEP,
                        EntityType.CHICKEN,
                        EntityType.VILLAGER,
                        EntityType.WITCH
                ),
                null,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private static QuestIconData buildBiomeVisitIcon(String number) {
        List<String> ids = List.of(
                "minecraft:water_bucket",
                "minecraft:grass_block",
                "minecraft:red_sand",
                "minecraft:sand",
                "minecraft:coarse_dirt",
                "minecraft:snow_block",
                "minecraft:mycelium",
                "minecraft:netherrack",
                "minecraft:soul_sand",
                "minecraft:crimson_nylium",
                "minecraft:warped_nylium"
        );
        List<ItemStack> blocks = new ArrayList<>();
        for (String id : ids) {
            ItemStack stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack(id);
            if (!stack.isEmpty()) blocks.add(stack);
        }
        if (blocks.isEmpty()) return null;
        return QuestIconData.fromItems(
                blocks.get(0),
                blocks,
                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"),
                number
        );
    }

    private static QuestIconData buildVariantMobIcon(EntityType<? extends LivingEntity> type,
                                                     ResourceKey<? extends net.minecraft.core.Registry<?>> registryKey,
                                                     List<Identifier> fallbackVariants,
                                                     ItemStack cornerItem,
                                                     String numberText) {
        List<Identifier> variants = resolveVariantIds(registryKey, fallbackVariants);
        if (variants == null || variants.isEmpty()) variants = fallbackVariants;
        List<EntityType<? extends LivingEntity>> entities = new ArrayList<>();
        if (variants != null) {
            for (int i = 0; i < variants.size(); i++) {
                entities.add(type);
            }
        }
        if (variants == null || variants.isEmpty()) return null;
        return QuestIconData.full(
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                List.of(),
                cornerItem == null ? com.jamie.jamiebingo.util.ItemStackUtil.empty() : cornerItem,
                numberText,
                null,
                List.of(),
                null,
                null,
                null,
                type,
                entities,
                null,
                List.of(),
                List.of(),
                List.of()
        ).withRotatingEntityVariants(variants);
    }

    private static QuestIconData buildVariantMobIcon(EntityType<? extends LivingEntity> type,
                                                     ResourceKey<? extends net.minecraft.core.Registry<?>> registryKey,
                                                     List<Identifier> fallbackVariants,
                                                     ItemStack cornerItem) {
        return buildVariantMobIcon(type, registryKey, fallbackVariants, cornerItem, null);
    }

    private static QuestIconData buildVariantTextureIcon(List<Identifier> textures, ItemStack cornerItem) {
        if (textures == null || textures.isEmpty()) return null;
        Identifier main = textures.get(0);
        return QuestIconData.full(
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                List.of(),
                cornerItem == null ? com.jamie.jamiebingo.util.ItemStackUtil.empty() : cornerItem,
                null,
                main,
                textures,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private static List<Identifier> resolveVariantIds(ResourceKey<? extends net.minecraft.core.Registry<?>> registryKey,
                                                      List<Identifier> fallback) {
        if (registryKey == null) return fallback;
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Object mc = mcClass.getMethod("getInstance").invoke(null);
            if (mc == null) return fallback;
            Object level = null;
            try {
                java.lang.reflect.Field f = mcClass.getDeclaredField("level");
                f.setAccessible(true);
                level = f.get(mc);
            } catch (Throwable ignored) {
            }
            if (level == null) return fallback;
            Object access = level.getClass().getMethod("registryAccess").invoke(level);
            if (access == null) return fallback;
            Object reg = access.getClass().getMethod("registryOrThrow", ResourceKey.class).invoke(access, registryKey);
            if (reg == null) return fallback;
            Object keySet = reg.getClass().getMethod("keySet").invoke(reg);
            if (keySet instanceof Set<?> set && !set.isEmpty()) {
                List<Identifier> out = new ArrayList<>();
                for (Object o : set) {
                    if (o instanceof Identifier id) {
                        out.add(id);
                    } else if (o instanceof ResourceKey<?> rk) {
                        Identifier id = getResourceKeyLocation(rk);
                        if (id != null) out.add(id);
                    }
                }
                if (!out.isEmpty()) return out;
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static Identifier getResourceKeyLocation(ResourceKey<?> rk) {
        if (rk == null) return null;
        try {
            java.lang.reflect.Method m = rk.getClass().getMethod("location");
            Object out = m.invoke(rk);
            if (out instanceof Identifier id) return id;
        } catch (Throwable ignored) {
        }
        try {
            java.lang.reflect.Method m = rk.getClass().getMethod("getPath");
            Object out = m.invoke(rk);
            if (out instanceof String s) return com.jamie.jamiebingo.util.IdUtil.id(s);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static QuestIconData buildNewQuestIconOverride(QuestDefinition def) {
        String id = def.id;
        switch (id) {
            case "quest.ride_a_pig_whilst_being_on_top_of_a_happy_ghast":
                return QuestIconData.full(
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        HAPPY_GHAST_ENTITY,
                        List.of(),
                        EntityType.PIG,
                        List.of(),
                        List.of(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:saddle")),
                        List.of()
                ).withCornerCopies(2);
            case "quest.fully_hydrate_a_dried_ghast":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:dried_ghast"),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:water_bucket"),
                        null
                );
            case "quest.attach_a_boat_to_a_happy_ghast_using_a_lead":
                return QuestIconData.full(
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:oak_boat"),
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        HAPPY_GHAST_ENTITY,
                        List.of(),
                        null,
                        List.of(),
                        List.of(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:lead")),
                        List.of()
                ).withCornerCopies(2);
            case "quest.have_a_tamed_wolf_kill_another_players_tamed_wolf":
                return QuestIconData.full(
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:player_head"),
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        EntityType.WOLF,
                        List.of(),
                        null,
                        List.of(EntityType.WOLF),
                        List.of(),
                        List.of()
                ).withCornerCopies(2);
            case "quest.dye_the_collar_of_a_tamed_wolf":
                return QuestIconData.full(
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        EntityType.WOLF,
                        List.of(),
                        null,
                        List.of(),
                        List.of(
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:red_dye"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:blue_dye"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:green_dye"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:yellow_dye"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:purple_dye")
                        ),
                        List.of()
                );
            case "quest.have_a_tamed_wolf_kill_3_unique_mobs":
                return buildWolfUniqueKillIcon("3");
            case "quest.have_a_tamed_wolf_kill_5_unique_mobs":
                return buildWolfUniqueKillIcon("5");
            case "quest.have_a_tamed_wolf_kill_8_unique_mobs":
                return buildWolfUniqueKillIcon("8");
            case "quest.have_10_live_tamed_mobs":
                return QuestIconData.full(
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        "10",
                        null,
                        List.of(),
                        HEART_TEXTURE,
                        null,
                        null,
                        EntityType.WOLF,
                        List.of(EntityType.CAT, EntityType.HORSE, EntityType.PARROT, EntityType.FOX),
                        null,
                        List.of(),
                        List.of(),
                        List.of()
                );
            case "quest.whole_team_breeds_an_animal_at_the_same_time":
                return QuestIconData.full(
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        EntityType.CHICKEN,
                        List.of(EntityType.CHICKEN, EntityType.PIG, EntityType.COW, EntityType.SHEEP, EntityType.WOLF, EntityType.CAT),
                        null,
                        List.of(),
                        List.of(
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wheat_seeds"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:carrot"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wheat"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wheat"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:beef"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cod")
                        ),
                        List.of()
                ).withCornerCopies(2);
            case "quest.ride_a_happy_ghast_for_200_meters": {
                ItemStack harness = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:white_harness");
                return QuestIconData.full(
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        List.of(),
                        harness,
                        "200m",
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        HAPPY_GHAST_ENTITY,
                        List.of(),
                        null,
                        List.of(),
                        List.of(),
                        List.of()
                );
            }
            case "quest.kill_a_mob_using_a_spear_whilst_riding_an_armored_horse":
                return QuestIconData.full(
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_horse_armor"),
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        EntityType.HORSE,
                        List.of(),
                        null,
                        List.of(),
                        List.of(
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:wooden_spear"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_spear"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_spear"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_spear"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:golden_spear"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_spear"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherite_spear")
                        ),
                        List.of()
                ).withCornerCopies(2);
            case "quest.throw_a_diamond_pickaxe_into_lava":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_pickaxe"),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:lava_bucket"),
                        null
                );
            case "quest.obtain_all_cobblestone_block_varients":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cobblestone"),
                        List.of(
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cobblestone_stairs"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cobblestone_slab"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cobblestone_wall"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:mossy_cobblestone")
                        ),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null
                );
            case "quest.obtain_all_stone_brick_varients":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_bricks"),
                        List.of(
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cracked_stone_bricks"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_brick_stairs"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_brick_slab"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:mossy_stone_bricks")
                        ),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null
                );
            case "quest.obtain_all_sandstone_block_varients":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:sandstone"),
                        List.of(
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:sandstone_stairs"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:sandstone_slab"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cut_sandstone"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:smooth_sandstone")
                        ),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null
                );
            case "quest.obtain_all_red_sandstone_block_varients":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:red_sandstone"),
                        List.of(
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:red_sandstone_stairs"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:red_sandstone_slab"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cut_red_sandstone"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:smooth_red_sandstone")
                        ),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null
                );
            case "quest.obtain_all_mossy_block_varients":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:mossy_cobblestone"),
                        List.of(
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:mossy_cobblestone_stairs"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:mossy_cobblestone_slab"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:mossy_stone_bricks"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:mossy_stone_brick_wall")
                        ),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null
                );
            case "quest.obtain_all_blackstone_block_varients":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:blackstone"),
                        List.of(
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:gilded_blackstone"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:polished_blackstone"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:polished_blackstone_bricks"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:chiseled_polished_blackstone")
                        ),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null
                );
            case "quest.obtain_all_end_stone_varients":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:end_stone"),
                        List.of(
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:end_stone_bricks"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:end_stone_brick_stairs"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:end_stone_brick_slab"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:end_stone_brick_wall")
                        ),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null
                );
            case "quest.obtain_all_snow_varients":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:snow_block"),
                        List.of(
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:snow"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:powder_snow_bucket"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:snowball")
                        ),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null
                );
            case "quest.ring_a_bell":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:bell"),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        "ring"
                );
            case "quest.whole_team_has_a_tamed_wolf":
                return QuestIconData.full(
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        EntityType.WOLF,
                        List.of(),
                        null,
                        List.of(),
                        List.of(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:bone")),
                        List.of()
                ).withCornerCopies(2);
            case "quest.whole_team_has_a_tamed_cat":
                return QuestIconData.full(
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        EntityType.CAT,
                        List.of(),
                        null,
                        List.of(),
                        List.of(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cod")),
                        List.of()
                ).withCornerCopies(2);
            case "quest.ride_a_minecart_for_25_meters":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:minecart"),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:rail"),
                        "25m"
                );
            case "quest.get_stuck_in_a_cobweb":
                return QuestIconData.full(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cobweb"),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null,
                        null,
                        List.of(),
                        effectTexture("slowness"),
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        List.of(),
                        List.of(),
                        List.of()
                );
            case "quest.push_a_button_20_times":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_button"),
                        List.of(
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:oak_button"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:spruce_button"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:birch_button"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:jungle_button"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:acacia_button"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:dark_oak_button"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:mangrove_button"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cherry_button"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:bamboo_button"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:pale_oak_button"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:crimson_button"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:warped_button"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone_button"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:polished_blackstone_button")
                        ),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        "push"
                );
            case "quest.flick_a_lever_20_times":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:lever"),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        "flick"
                );
            case "quest.decorate_a_pot_with_a_pottery_sherd":
                return QuestIconData.full(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:decorated_pot"),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        List.of(),
                        List.of(
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:angler_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:archer_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:arms_up_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:blade_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:brewer_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:burn_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:danger_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:explorer_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:friend_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:heart_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:heartbreak_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:howl_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:miner_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:mourner_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:plenty_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:prize_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:scrape_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:sheaf_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:shelter_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:skull_pottery_sherd"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:snort_pottery_sherd")
                        ),
                        List.of()
                );
            case "quest.place_a_bee_nest_or_a_bee_hive_in_the_nether":
                return QuestIconData.full(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:bee_nest"),
                        List.of(
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:bee_nest"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:beehive")
                        ),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null,
                        null,
                        List.of(),
                        NETHER_PORTAL_TEXTURE,
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        List.of(),
                        List.of(),
                        List.of()
                );
            case "quest.right_click_a_fletching_table":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:fletching_table"),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        "click"
                );
            case "quest.play_sound_from_a_note_block_whilst_playing_music_on_a_jukebox":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:note_block"),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:jukebox"),
                        null
                );
            case "quest.boat_on_ice":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:oak_boat"),
                        List.of(
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:spruce_boat"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:birch_boat"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:jungle_boat"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:acacia_boat"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:dark_oak_boat"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:mangrove_boat"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:cherry_boat"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:bamboo_raft"),
                                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:pale_oak_boat")
                        ),
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:ice"),
                        null
                );
            case "quest.crouch_on_soul_sand":
                return QuestIconData.full(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:soul_sand"),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null,
                        null,
                        List.of(),
                        effectTexture("slowness"),
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        List.of(),
                        List.of(),
                        List.of()
                );
            case "quest.throw_a_bucket_of_axolotl_into_lava":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:axolotl_bucket"),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:lava_bucket"),
                        null
                );
            case "quest.mine_stone_using_your_fists":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:stone"),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        "punch"
                );
            case "quest.jump_on_mud":
                return QuestIconData.full(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:mud"),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        null,
                        null,
                        List.of(),
                        effectTexture("jump boost"),
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        List.of(),
                        List.of(),
                        List.of()
                );
            case "quest.stand_on_a_block_of_copper":
                return QuestIconData.fromItems(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:copper_block"), List.of(), com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"), null);
            case "quest.stand_on_a_block_of_iron":
                return QuestIconData.fromItems(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_block"), List.of(), com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"), null);
            case "quest.stand_on_a_block_of_lapis_lazuli":
                return QuestIconData.fromItems(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:lapis_block"), List.of(), com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"), null);
            case "quest.stand_on_a_block_of_gold":
                return QuestIconData.fromItems(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:gold_block"), List.of(), com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"), null);
            case "quest.stand_on_a_block_of_redstone":
                return QuestIconData.fromItems(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:redstone_block"), List.of(), com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"), null);
            case "quest.stand_on_a_block_of_of_diamond":
                return QuestIconData.fromItems(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_block"), List.of(), com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"), null);
            case "quest.stand_on_a_block_of_netherite":
                return QuestIconData.fromItems(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:netherite_block"), List.of(), com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"), null);
            case "quest.stand_on_a_block_of_coal":
                return QuestIconData.fromItems(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:coal_block"), List.of(), com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"), null);
            case "quest.stand_on_a_block_of_emerald":
                return QuestIconData.fromItems(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:emerald_block"), List.of(), com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"), null);
            case "quest.trigger_a_sculk_sensor":
                return QuestIconData.fromItems(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:sculk_sensor"), List.of(), com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots"), null);
            case "quest.use_a_ladder_to_climb_64_meters_in_hieght":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:ladder"),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        "64m"
                );
            case "quest.use_a_crafter_to_craft_a_crafter":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:crafter"),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:crafter"),
                        null
                );
            case "quest.put_a_crafting_table_in_a_crafting_table":
                return QuestIconData.fromItems(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:crafting_table"), List.of(), com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:crafting_table"), null);
            case "quest.put_a_furnace_in_a_furnace":
                return QuestIconData.fromItems(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:furnace"), List.of(), com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:furnace"), null);
            case "quest.put_a_blast_furnace_in_a_blast_furnace":
                return QuestIconData.fromItems(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:blast_furnace"), List.of(), com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:blast_furnace"), null);
            case "quest.put_a_smoker_in_a_smoker":
                return QuestIconData.fromItems(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:smoker"), List.of(), com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:smoker"), null);
            case "quest.put_a_decorated_pot_in_a_decorated_pot":
                return QuestIconData.fromItems(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:decorated_pot"), List.of(), com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:decorated_pot"), null);
            case "quest.put_a_chest_in_a_chest":
                return QuestIconData.fromItems(com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:chest"), List.of(), com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:chest"), null);
            case "quest.say_gg_in_chat_when_this_is_the_last_remaining_slot":
                return QuestIconData.fromItems(
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        List.of(),
                        com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                        "GG"
                );
            default:
                return null;
        }
    }

    private static QuestIconData buildWolfUniqueKillIcon(String numberText) {
        return QuestIconData.full(
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                List.of(),
                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_sword"),
                numberText,
                null,
                List.of(),
                null,
                null,
                null,
                EntityType.WOLF,
                List.of(),
                null,
                List.of(
                        EntityType.ZOMBIE,
                        EntityType.SKELETON,
                        EntityType.CREEPER,
                        EntityType.SPIDER,
                        EntityType.ENDERMAN,
                        EntityType.BLAZE,
                        EntityType.SLIME
                ),
                List.of(),
                List.of()
        ).withCornerCopies(2);
    }

    private static QuestIconData buildTrimmedArmorCycle() {
        List<Identifier> patterns = List.of(
                com.jamie.jamiebingo.util.IdUtil.id("minecraft:sentry"),
                com.jamie.jamiebingo.util.IdUtil.id("minecraft:dune"),
                com.jamie.jamiebingo.util.IdUtil.id("minecraft:coast"),
                com.jamie.jamiebingo.util.IdUtil.id("minecraft:ward"),
                com.jamie.jamiebingo.util.IdUtil.id("minecraft:eye"),
                com.jamie.jamiebingo.util.IdUtil.id("minecraft:vex"),
                com.jamie.jamiebingo.util.IdUtil.id("minecraft:tide")
        );
        List<ItemStack> trimmed = new ArrayList<>();
        for (Identifier patternId : patterns) {
            ItemStack stack = buildTrimmedDiamondChestplate(patternId);
            if (!stack.isEmpty()) trimmed.add(stack);
        }
        if (trimmed.isEmpty()) return null;
        return QuestIconData.fromItems(
                trimmed.get(0),
                trimmed,
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                null
        ).withCornerCopies(2);
    }

    private static QuestIconData buildItemCycle(List<String> itemIds, String numberText, ItemStack corner) {
        if (itemIds == null || itemIds.isEmpty()) return null;
        List<ItemStack> items = new ArrayList<>();
        for (String id : itemIds) {
            ItemStack stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack(id.startsWith("minecraft:") ? id : "minecraft:" + id);
            if (!stack.isEmpty()) items.add(stack);
        }
        if (items.isEmpty()) return null;
        return QuestIconData.fromItems(
                items.get(0),
                items,
                corner == null ? com.jamie.jamiebingo.util.ItemStackUtil.empty() : corner,
                numberText
        );
    }

    private static QuestIconData buildArmorCycle(String numberText, ItemStack corner) {
        List<ItemStack> items = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS) {
            Identifier id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null) continue;
            String path = id.getPath();
            if (path.contains("horse_armor")) continue;
            if (path.endsWith("_helmet") || path.endsWith("_chestplate") || path.endsWith("_leggings") || path.endsWith("_boots")) {
                items.add(new ItemStack(item));
            }
        }
        if (items.isEmpty()) return null;
        return QuestIconData.fromItems(
                items.get(0),
                items,
                corner == null ? com.jamie.jamiebingo.util.ItemStackUtil.empty() : corner,
                numberText
        );
    }

    private static QuestIconData buildToolCycle(String numberText, ItemStack corner) {
        List<ItemStack> items = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS) {
            Identifier id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null) continue;
            String path = id.getPath();
            if (path.endsWith("_sword") || path.endsWith("_axe") || path.endsWith("_pickaxe")
                    || path.endsWith("_shovel") || path.endsWith("_hoe") || path.endsWith("_spear")) {
                items.add(new ItemStack(item));
            }
        }
        if (items.isEmpty()) return null;
        return QuestIconData.fromItems(
                items.get(0),
                items,
                corner == null ? com.jamie.jamiebingo.util.ItemStackUtil.empty() : corner,
                numberText
        );
    }

    private static ItemStack buildColoredLeatherItem(String colorName, String slot) {
        if (colorName == null || slot == null) return com.jamie.jamiebingo.util.ItemStackUtil.empty();
        net.minecraft.world.item.DyeColor dye = net.minecraft.world.item.DyeColor.byName(colorName, net.minecraft.world.item.DyeColor.WHITE);

        ItemStack stack;
        if (slot.contains("boots")) {
            stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots");
        } else if (slot.contains("leggings")) {
            stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_leggings");
        } else if (slot.contains("chestplate") || slot.contains("tunic")) {
            stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_chestplate");
        } else {
            stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_helmet");
        }

        int color = dye.getTextureDiffuseColor();
        com.jamie.jamiebingo.util.ItemStackComponentUtil.set(
                stack,
                DataComponents.DYED_COLOR,
                new DyedItemColor(color)
        );
        return stack;
    }

    private static QuestIconData buildAdvancementIcon(String advancementId, String numberText, ItemStack corner) {
        ItemStack cornerItem = corner == null ? com.jamie.jamiebingo.util.ItemStackUtil.empty() : corner;
        return QuestIconData.full(
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                List.of(),
                cornerItem,
                numberText,
                ADVANCEMENT_ICON_TEXTURE,
                List.of(),
                null,
                null,
                null,
                null,
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private static QuestIconData buildAdvancementIcon(String advancementId,
                                                     String numberText,
                                                     ItemStack cornerItem,
                                                     Identifier cornerTexture,
                                                     EntityType<? extends LivingEntity> cornerEntity) {
        ItemStack cornerStack = cornerItem == null ? com.jamie.jamiebingo.util.ItemStackUtil.empty() : cornerItem;
        return QuestIconData.full(
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                List.of(),
                cornerStack,
                numberText,
                ADVANCEMENT_ICON_TEXTURE,
                List.of(),
                cornerTexture,
                null,
                null,
                null,
                List.of(),
                cornerEntity,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private static String resolveAdvancementIconItemId(String advancementId) {
        if (advancementId == null || advancementId.isBlank()) return null;
        try {
            Identifier advId = com.jamie.jamiebingo.util.IdUtil.id(advancementId);
            String resPath = "data/" + advId.getNamespace() + "/advancements/" + advId.getPath() + ".json";
            try (InputStream in = net.minecraft.client.Minecraft.class.getClassLoader().getResourceAsStream(resPath)) {
                if (in != null) {
                    try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                        if (root.has("display")) {
                            JsonObject display = root.getAsJsonObject("display");
                            if (display.has("icon")) {
                                JsonObject icon = display.getAsJsonObject("icon");
                                if (icon.has("item")) return icon.get("item").getAsString();
                                if (icon.has("id")) return icon.get("id").getAsString();
                            }
                        }
                    }
                }
            }

            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Object mc = mcClass.getMethod("getInstance").invoke(null);
            if (mc == null) return null;
            Object rm = mcClass.getMethod("getResourceManager").invoke(mc);
            if (rm == null) return null;

            Identifier resId = com.jamie.jamiebingo.util.IdUtil.id(
                    advId.getNamespace() + ":advancements/" + advId.getPath() + ".json"
            );

            Object opt = rm.getClass().getMethod("getResource", Identifier.class).invoke(rm, resId);
            if (!(opt instanceof Optional<?> o) || o.isEmpty()) return null;
            Object resObj = o.get();
            if (!(resObj instanceof Resource res)) return null;

            try (InputStream in = res.open();
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (!root.has("display")) return null;
                JsonObject display = root.getAsJsonObject("display");
                if (!display.has("icon")) return null;
                JsonObject icon = display.getAsJsonObject("icon");
                if (icon.has("item")) return icon.get("item").getAsString();
                if (icon.has("id")) return icon.get("id").getAsString();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static ItemStack buildGlintArmor() {
        ItemStack stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_chestplate");
        if (stack.isEmpty()) return stack;
        com.jamie.jamiebingo.util.ItemStackComponentUtil.set(stack, DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private static QuestIconData buildItemCycleByPath(Predicate<String> filter, String numberText, String preferredFirstPath) {
        if (filter == null) return null;
        List<ItemStack> items = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS) {
            Identifier id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null) continue;
            String path = id.getPath();
            if (!filter.test(path)) continue;
            items.add(new ItemStack(item));
        }

        if (preferredFirstPath != null && !preferredFirstPath.isBlank()) {
            ItemStack preferred = item("minecraft:" + preferredFirstPath);
            if (!preferred.isEmpty()) {
                items.removeIf(stack -> stack.getItem() == preferred.getItem());
                items.add(0, preferred);
            }
        }

        if (items.isEmpty()) return null;
        items.removeIf(ItemStack::isEmpty);
        if (items.isEmpty()) return null;
        return QuestIconData.fromItems(
                items.get(0),
                items,
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                numberText
        );
    }

    private static ItemStack buildTrimmedDiamondChestplate(Identifier patternId) {
        ItemStack stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:diamond_chestplate");
        if (stack.isEmpty()) return stack;
        try {
            Identifier lapisId = com.jamie.jamiebingo.util.IdUtil.id("minecraft:lapis");
            ResourceKey<TrimMaterial> materialKey = ResourceKey.create(Registries.TRIM_MATERIAL, lapisId);
            ResourceKey<TrimPattern> patternKey = ResourceKey.create(Registries.TRIM_PATTERN, patternId);
            Object materialRegistry = findRegistry(Registries.TRIM_MATERIAL);
            Object patternRegistry = findRegistry(Registries.TRIM_PATTERN);
            Holder<TrimMaterial> material = getHolder(materialRegistry, materialKey);
            Holder<TrimPattern> pattern = getHolder(patternRegistry, patternKey);
            if (material == null || pattern == null) return stack;
            ArmorTrim trim = new ArmorTrim(material, pattern);
            com.jamie.jamiebingo.util.ItemStackComponentUtil.set(stack, DataComponents.TRIM, trim);
        } catch (Throwable ignored) {
        }
        return stack;
    }

    private static Object findRegistry(ResourceKey<? extends net.minecraft.core.Registry<?>> key) {
        if (key == null) return null;
        try {
            for (java.lang.reflect.Field f : net.minecraft.core.registries.BuiltInRegistries.class.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                if (!net.minecraft.core.Registry.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object reg = f.get(null);
                if (reg == null) continue;
                Object regKey = invokeMethod(reg, "key");
                if (key.equals(regKey)) return reg;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> Holder<T> getHolder(Object registry, ResourceKey<T> key) {
        if (registry == null || key == null) return null;
        Object holderObj = invokeMethod(registry, "getHolder", key);
        if (holderObj == null) {
            holderObj = invokeMethod(registry, "getHolderOrThrow", key);
        }
        if (holderObj instanceof java.util.Optional<?> opt) {
            holderObj = opt.orElse(null);
        }
        if (holderObj instanceof Holder<?> h) {
            return (Holder<T>) h;
        }
        return null;
    }

    private static Object invokeMethod(Object target, String name, Object arg) {
        if (target == null) return null;
        try {
            for (java.lang.reflect.Method m : target.getClass().getMethods()) {
                if (!m.getName().equals(name)) continue;
                if (m.getParameterCount() != 1) continue;
                return m.invoke(target, arg);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object invokeMethod(Object target, String name) {
        if (target == null) return null;
        try {
            for (java.lang.reflect.Method m : target.getClass().getMethods()) {
                if (!m.getName().equals(name)) continue;
                if (m.getParameterCount() != 0) continue;
                return m.invoke(target);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static EntityType<? extends LivingEntity> entityType(String id) {
        if (id == null || id.isBlank()) return null;
        return (EntityType<? extends LivingEntity>) ForgeRegistries.ENTITY_TYPES.getValue(com.jamie.jamiebingo.util.IdUtil.id(id));
    }

    private static QuestIconData parseTexture(String texture) {
        String t = texture.trim().toLowerCase();
        if (t.isBlank()) {
            return QuestIconData.empty(item("minecraft:barrier"));
        }

        String numberText = extractNumber(t);
        String mainPart = t;
        String cornerPart = null;
        List<String> cycleParts = null;
        int cornerCopies = (t.contains("two mini player head")
                || t.contains("two mini player heads")
                || t.contains("two player heads")) ? 2 : 1;

        String cycleToken = null;
        if (t.contains("cycle between")) {
            cycleToken = "cycle between";
        } else if (t.contains("cycle through")) {
            cycleToken = "cycle through";
        }

        if (cycleToken != null) {
            String listPart = t.substring(t.indexOf(cycleToken) + cycleToken.length()).trim();
            int withIdx = listPart.indexOf(" with ");
            if (withIdx >= 0) {
                cornerPart = listPart.substring(withIdx + " with ".length()).trim();
                listPart = listPart.substring(0, withIdx).trim();
            }
            cycleParts = splitList(listPart);
            if (!cycleParts.isEmpty()) {
                mainPart = cycleParts.get(0);
            }
        } else if (t.contains(" with small ")) {
            String[] parts = t.split(" with small ", 2);
            mainPart = parts[0].trim();
            cornerPart = parts[1].trim();
        } else if (t.contains(" with mini ")) {
            String[] parts = t.split(" with mini ", 2);
            mainPart = parts[0].trim();
            cornerPart = parts[1].trim();
        } else if (t.contains(" with ")) {
            String[] parts = t.split(" with ", 2);
            mainPart = parts[0].trim();
            cornerPart = parts[1].trim();
        }

        List<String> cornerCycleParts = null;

        String mainPartRaw = mainPart;
        String cornerPartRaw = cornerPart;
        mainPart = stripTrailingWords(mainPart);
        if (cornerPart != null) {
            cornerPart = stripNumberCorner(cornerPart);
            if (cornerPart.contains("cycling")) {
                String cleaned = cornerPart
                        .replace("cycling", "")
                        .replace("also", "")
                        .replace("in small", "")
                        .replace("small", "")
                        .replace("in corner", "")
                        .trim();
                cornerCycleParts = splitList(cleaned);
                cornerCycleParts = expandCycleParts(cornerCycleParts, cleaned);
                if (!cornerCycleParts.isEmpty()) {
                    cornerPart = cornerCycleParts.get(0);
                }
            }
            cornerPart = stripTrailingWords(cornerPart);
            if (isNumberToken(cornerPart)) {
                cornerPart = null;
            }
        }

        if (cycleParts != null && !cycleParts.isEmpty()) {
            cycleParts = expandCycleParts(cycleParts, t);
        }

        if (cornerCopies > 1) {
            if (cornerPart == null || cornerPart.isBlank()) {
                cornerPart = "player head";
            } else if (cornerPart.contains("player head") || cornerPart.contains("player heads")) {
                cornerPart = "player head";
            }
        }

        IconSpec mainSpec = iconForToken(mainPart);
        IconSpec cornerSpec = cornerPart != null ? iconForToken(cornerPart) : null;
        if (isBarrierSpec(mainSpec) && mainPartRaw != null && !mainPartRaw.equals(mainPart)) {
            mainSpec = iconForToken(mainPartRaw);
        }
        if (isBarrierSpec(cornerSpec) && cornerPartRaw != null && !cornerPartRaw.equals(cornerPart)) {
            cornerSpec = iconForToken(cornerPartRaw);
        }
        boolean mainBaby = mainPartRaw != null && mainPartRaw.contains("baby ");
        boolean cornerBaby = cornerPartRaw != null && cornerPartRaw.contains("baby ");

        List<ItemStack> rotatingItems = new ArrayList<>();
        List<Identifier> rotatingTextures = new ArrayList<>();
        List<EntityType<? extends LivingEntity>> rotatingEntities = new ArrayList<>();
        List<Integer> rotatingEntityColors = new ArrayList<>();
        boolean hasEntityColors = false;
        if (cycleParts != null && !cycleParts.isEmpty()) {
            for (String part : cycleParts) {
                boolean partBaby = part.contains("baby ");
                IconSpec spec = iconForToken(stripTrailingWords(part));
                if (spec != null && spec.entityType != null) {
                    rotatingEntities.add(spec.entityType);
                    rotatingEntityColors.add(partBaby ? -1 : null);
                    if (partBaby) hasEntityColors = true;
                } else if (spec != null && spec.texture != null) {
                    rotatingTextures.add(spec.texture);
                } else if (spec != null && spec.item != null && !spec.item.isEmpty()) {
                    rotatingItems.add(spec.item);
                }
            }
        }

        List<ItemStack> rotatingCornerItems = new ArrayList<>();
        List<Identifier> rotatingCornerTextures = new ArrayList<>();
        List<EntityType<? extends LivingEntity>> rotatingCornerEntities = new ArrayList<>();
        List<Integer> rotatingCornerEntityColors = new ArrayList<>();
        boolean hasCornerEntityColors = false;
        if (cornerCycleParts != null && !cornerCycleParts.isEmpty()) {
            for (String part : cornerCycleParts) {
                boolean partBaby = part.contains("baby ");
                IconSpec spec = iconForToken(stripTrailingWords(part));
                if (spec != null && spec.entityType != null) {
                    rotatingCornerEntities.add(spec.entityType);
                    rotatingCornerEntityColors.add(partBaby ? -1 : null);
                    if (partBaby) hasCornerEntityColors = true;
                } else if (spec != null && spec.texture != null) {
                    rotatingCornerTextures.add(spec.texture);
                } else if (spec != null && spec.item != null && !spec.item.isEmpty()) {
                    rotatingCornerItems.add(spec.item);
                }
            }
        }

        if (!rotatingEntities.isEmpty() || !rotatingTextures.isEmpty() || !rotatingItems.isEmpty()
                || !rotatingCornerEntities.isEmpty() || !rotatingCornerTextures.isEmpty() || !rotatingCornerItems.isEmpty()
                || (mainSpec != null && mainSpec.region != null) || (cornerSpec != null && cornerSpec.region != null)
                || (mainSpec != null && mainSpec.entityType != null) || (cornerSpec != null && cornerSpec.entityType != null)) {
            if (!hasEntityColors) rotatingEntityColors = List.of();
            if (!hasCornerEntityColors) rotatingCornerEntityColors = List.of();
            Integer mainEntityColor = (mainSpec != null && mainSpec.entityType != null && mainBaby) ? -1 : null;
            Integer cornerEntityColor = (cornerSpec != null && cornerSpec.entityType != null && cornerBaby) ? -1 : null;

            QuestIconData out = new QuestIconData(
                    mainSpec != null && mainSpec.item != null ? mainSpec.item : com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    rotatingItems,
                    cornerSpec != null && cornerSpec.item != null ? cornerSpec.item : com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    numberText,
                    mainSpec != null ? mainSpec.texture : null,
                    rotatingTextures,
                    cornerSpec != null ? cornerSpec.texture : null,
                    mainSpec != null ? mainSpec.region : null,
                    cornerSpec != null ? cornerSpec.region : null,
                    mainSpec != null ? mainSpec.entityType : null,
                    rotatingEntities,
                    cornerSpec != null ? cornerSpec.entityType : null,
                    null,
                    List.of(),
                    mainEntityColor,
                    rotatingEntityColors,
                    cornerEntityColor,
                    rotatingCornerEntityColors,
                    rotatingCornerEntities,
                    rotatingCornerItems,
                    rotatingCornerTextures,
                    1
            );
            return cornerCopies > 1 ? out.withCornerCopies(cornerCopies) : out;
        }

        if (mainSpec != null && mainSpec.texture != null) {
            QuestIconData out = QuestIconData.fromTextures(
                    mainSpec.texture,
                    List.of(),
                    cornerSpec != null ? cornerSpec.texture : null,
                    numberText
            );
            return cornerCopies > 1 ? out.withCornerCopies(cornerCopies) : out;
        }

        QuestIconData out = QuestIconData.fromItems(
                mainSpec != null ? mainSpec.item : com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                List.of(),
                cornerSpec != null ? cornerSpec.item : com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                numberText
        );
        return cornerCopies > 1 ? out.withCornerCopies(cornerCopies) : out;
    }

    private static String extractNumber(String t) {
        Matcher m = NUMBER_PATTERN.matcher(t);
        if (m.find()) return m.group(1);
        Matcher km = KM_PATTERN.matcher(t);
        if (km.find()) return km.group(1) + "km";
        Matcher meters = M_PATTERN.matcher(t);
        if (meters.find()) return meters.group(1) + "m";
        Matcher any = ANY_NUMBER_PATTERN.matcher(t);
        if (any.find()) return any.group(1);
        return null;
    }

    private static boolean isNumberToken(String t) {
        if (t == null) return false;
        String token = t.trim().toLowerCase()
                .replace("number", "")
                .replace("small", "")
                .replace("with", "")
                .replace("and", "")
                .trim();
        if (token.isBlank()) return true;
        return PURE_NUMBER_PATTERN.matcher(token).matches();
    }

    private static String stripNumberCorner(String cornerPart) {
        if (cornerPart == null) return null;
        String cleaned = cornerPart;
        cleaned = cleaned.replaceAll("number\\s+\\d+\\s+and\\s+", "");
        cleaned = cleaned.replaceAll("number\\s+\\d+\\s+with\\s+", "");
        cleaned = cleaned.replaceAll("number\\s+\\d+\\s*", "");
        return cleaned.trim();
    }

    private static List<String> splitList(String listPart) {
        String cleaned = listPart.replace(" and ", ",");
        String[] parts = cleaned.split(",");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String s = p.trim();
            if (!s.isBlank()) out.add(s);
        }
        return out;
    }

    private static List<String> expandCycleParts(List<String> parts, String sourceText) {
        if (parts == null || parts.isEmpty()) return parts;
        List<String> out = new ArrayList<>();
        String lowerSource = sourceText == null ? "" : sourceText.toLowerCase();

        for (String part : parts) {
            String p = part == null ? "" : part.trim().toLowerCase();
            if (p.isBlank()) continue;

            if (p.contains("effect textures") || p.contains("effect texture")) {
                out.addAll(List.of(
                        "absorption",
                        "jump boost",
                        "poison",
                        "weakness",
                        "glowing",
                        "levitation",
                        "mining fatigue",
                        "nausea",
                        "bad omen",
                        "speed",
                        "water breathing",
                        "wither",
                        "slowness"
                ));
                continue;
            }
            if (p.contains("all axes")) {
                out.addAll(List.of("wooden axe", "stone axe", "copper axe", "iron axe", "golden axe", "diamond axe", "netherite axe"));
                continue;
            }
            if (p.contains("all hoes")) {
                out.addAll(List.of("wooden hoe", "stone hoe", "copper hoe", "iron hoe", "golden hoe", "diamond hoe", "netherite hoe"));
                continue;
            }
            if (p.contains("all pickaxes")) {
                out.addAll(List.of("wooden pickaxe", "stone pickaxe", "copper pickaxe", "iron pickaxe", "golden pickaxe", "diamond pickaxe", "netherite pickaxe"));
                continue;
            }
            if (p.contains("all shovels")) {
                out.addAll(List.of("wooden shovel", "stone shovel", "copper shovel", "iron shovel", "golden shovel", "diamond shovel", "netherite shovel"));
                continue;
            }
            if (p.contains("all swords")) {
                out.addAll(List.of("wooden sword", "stone sword", "copper sword", "iron sword", "golden sword", "diamond sword", "netherite sword"));
                continue;
            }
            if (p.contains("all spears")) {
                out.addAll(List.of("wooden spear", "stone spear", "copper spear", "iron spear", "golden spear", "diamond spear", "netherite spear"));
                continue;
            }
            if (p.contains("all copper tools")) {
                out.addAll(List.of("copper shovel", "copper hoe", "copper sword", "copper axe", "copper pickaxe", "copper spear"));
                continue;
            }
            if (p.contains("all sapling types")) {
                out.addAll(List.of(
                        "oak sapling",
                        "spruce sapling",
                        "birch sapling",
                        "jungle sapling",
                        "acacia sapling",
                        "dark oak sapling",
                        "mangrove propagule",
                        "azalea",
                        "flowering azalea"
                ));
                continue;
            }
            if (p.contains("all flower types")) {
                out.addAll(List.of(
                        "dandelion",
                        "poppy",
                        "blue orchid",
                        "allium",
                        "azure bluet",
                        "red tulip",
                        "orange tulip",
                        "white tulip",
                        "pink tulip",
                        "oxeye daisy",
                        "cornflower",
                        "lily of the valley",
                        "wither rose",
                        "sunflower",
                        "lilac",
                        "rose bush",
                        "peony",
                        "spore blossom"
                ));
                continue;
            }
            if (p.contains("all seed")) {
                out.addAll(List.of("wheat seeds", "beetroot seeds", "melon seeds", "pumpkin seeds"));
                continue;
            }
            if (p.contains("all hostile mob") || p.contains("all hostile mobs")) {
                out.addAll(List.of(
                        "zombie", "husk", "drowned", "skeleton", "wither skeleton", "creeper",
                        "spider", "cave spider", "enderman", "blaze", "slime", "magma cube",
                        "phantom", "shulker", "witch", "guardian", "elder guardian", "ghast",
                        "stray", "endermite", "silverfish", "zombie villager", "zombie piglin",
                        "piglin", "hoglin", "zoglin"
                ));
                continue;
            }
            if (p.contains("many mob") || p.contains("many mobs")) {
                out.addAll(List.of(
                        "zombie", "skeleton", "creeper", "spider", "enderman",
                        "chicken", "cow", "pig", "sheep", "villager"
                ));
                continue;
            }
            if (p.contains("baby mob") || p.contains("baby mobs")) {
                out.addAll(List.of(
                        "baby zombie",
                        "baby villager",
                        "baby pig",
                        "baby cow",
                        "baby sheep",
                        "baby chicken",
                        "baby wolf",
                        "baby cat"
                ));
                continue;
            }
            if (p.contains("status effect icon") || p.contains("status effect icons")) {
                out.addAll(List.of(
                        "absorption",
                        "jump boost",
                        "poison",
                        "weakness",
                        "glowing",
                        "levitation",
                        "mining fatigue",
                        "nausea",
                        "bad omen",
                        "speed",
                        "water breathing",
                        "wither",
                        "slowness"
                ));
                continue;
            }
            if (p.contains("armor trim types") || p.contains("armor trims") || p.contains("armor trim templates")) {
                out.addAll(List.of(
                        "sentry armor trim smithing template",
                        "dune armor trim smithing template",
                        "coast armor trim smithing template",
                        "wild armor trim smithing template",
                        "ward armor trim smithing template",
                        "eye armor trim smithing template",
                        "vex armor trim smithing template",
                        "tide armor trim smithing template",
                        "snout armor trim smithing template",
                        "rib armor trim smithing template",
                        "spire armor trim smithing template",
                        "wayfinder armor trim smithing template",
                        "shaper armor trim smithing template",
                        "silence armor trim smithing template",
                        "raiser armor trim smithing template",
                        "host armor trim smithing template",
                        "flow armor trim smithing template",
                        "bolt armor trim smithing template"
                ));
                continue;
            }
            if (p.contains("workstation types") || p.contains("workstations")) {
                out.addAll(List.of(
                        "barrel",
                        "blast furnace",
                        "brewing stand",
                        "cartography table",
                        "composter",
                        "fletching table",
                        "grindstone",
                        "lectern",
                        "loom",
                        "smithing table",
                        "smoker",
                        "stonecutter",
                        "cauldron"
                ));
                continue;
            }

            String material = detectMaterial(parts, lowerSource);
            String adjusted = applyMaterialToPart(part, material, lowerSource);
            out.add(adjusted);
        }

        return out;
    }

    private static String detectMaterial(List<String> parts, String sourceText) {
        String lower = sourceText == null ? "" : sourceText.toLowerCase();
        if (lower.contains("netherite")) return "netherite";
        if (lower.contains("diamond")) return "diamond";
        if (lower.contains("gold ")) return "gold";
        if (lower.contains("golden")) return "gold";
        if (lower.contains("iron")) return "iron";
        if (lower.contains("stone")) return "stone";
        if (lower.contains("wooden")) return "wooden";
        if (lower.contains("leather")) return "leather";
        if (lower.contains("chain")) return "chain";

        for (String part : parts) {
            String p = part.toLowerCase();
            if (p.contains("netherite")) return "netherite";
            if (p.contains("diamond")) return "diamond";
            if (p.contains("gold ")) return "gold";
            if (p.contains("golden")) return "gold";
            if (p.contains("iron")) return "iron";
            if (p.contains("stone")) return "stone";
            if (p.contains("wooden")) return "wooden";
            if (p.contains("leather")) return "leather";
            if (p.contains("chain")) return "chain";
        }
        return null;
    }

    private static String applyMaterialToPart(String part, String material, String sourceText) {
        if (material == null || part == null) return part;

        String p = part.trim();
        String lower = p.toLowerCase();
        boolean isHorseArmor = sourceText != null && sourceText.toLowerCase().contains("horse armor");
        if (isHorseArmor) {
            if (!lower.contains("horse armor")) {
                p = p + " horse armor";
                lower = p.toLowerCase();
            }
        }

        boolean isTool = lower.equals("shovel") || lower.equals("hoe") || lower.equals("sword")
                || lower.equals("axe") || lower.equals("pickaxe");
        boolean isArmor = lower.equals("boots") || lower.equals("leggings")
                || lower.equals("chestplate") || lower.equals("helmet");

        if ((isTool || isArmor) && !lower.contains(material)) {
            String prefix = material;
            if (material.equals("gold")) prefix = "golden";
            if (material.equals("chain")) prefix = "chainmail";
            p = prefix + " " + p;
        }

        if (material.equals("gold") && lower.startsWith("gold ")) {
            p = "golden " + p.substring("gold ".length());
        }
        if (material.equals("chain") && lower.startsWith("chain ")) {
            p = "chainmail " + p.substring("chain ".length());
        }

        return p;
    }

    private static String stripTrailingWords(String s) {
        String out = s;
        out = out.replace("textures", "");
        out = out.replace("texture", "");
        out = out.replace("block", "");
        out = out.replace("effect", "");
        out = out.replace("variants", "");
        out = out.replace("variant", "");
        out = out.replace("entities", "");
        out = out.replace("entity", "");
        out = out.replace("mobs", "");
        out = out.replace("mob", "");
        out = out.replace("skins", "");
        out = out.replace("skin", "");
        out = out.replace("bar texture", "bar");
        out = out.replace("in corner", "");
        out = out.replace("corner", "");
        return out.trim();
    }

    private static QuestIconData buildColoredSheepKillIcon(String questId) {
        if (questId == null) return null;
        String colorName = questId
                .replace("quest.kill_", "")
                .replace("_sheep", "");

        net.minecraft.world.item.DyeColor dye =
                net.minecraft.world.item.DyeColor.byName(colorName, null);
        if (dye == null) return null;

        return new QuestIconData(
                com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                List.of(),
                com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:iron_sword"),
                null,
                null,
                List.of(),
                null,
                null,
                null,
                EntityType.SHEEP,
                List.of(),
                null,
                null,
                List.of(),
                dye.getId(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                1
        );
    }

    private static IconSpec iconForToken(String tokenRaw) {
        if (tokenRaw == null || tokenRaw.isBlank()) return new IconSpec(com.jamie.jamiebingo.util.ItemStackUtil.empty(), null, null);

        String token = tokenRaw.toLowerCase().trim();
        token = token.replace("with small", "")
                .replace("with mini", "")
                .replace("small", "")
                .replace("smal", "")
                .replace("mini", "")
                .replace("windcharge", "wind charge")
                .replace("armour", "armor")
                .replace("pilliger", "pillager")
                .replace("villiger", "villager")
                .replace("endarman", "enderman")
                .replace("llamma", "llama")
                .replace("ocelott", "ocelot")
                .replace("netherack", "netherrack")
                .replace("gaurdian", "guardian")
                .replace("honecomb", "honeycomb")
                .replace("iron chains", "iron chain")
                .replace("iron chain", "iron chain")
                .replace("chain ", "chainmail ")
                .replace("nametag", "name tag")
                .replace("pants", "leggings")
                .replace("tunic", "chestplate")
                .replace("cap", "helmet")
                .replace("wood shovel", "wooden shovel")
                .replace("wood hoe", "wooden hoe")
                .replace("wood sword", "wooden sword")
                .replace("wood axe", "wooden axe")
                .replace("wood pickaxe", "wooden pickaxe")
                .trim();

        if (token.contains("angry face")) {
            return new IconSpec(com.jamie.jamiebingo.util.ItemStackUtil.empty(), ANGRY_TEXTURE);
        }
        if (token.contains("infinite symbol")) {
            return new IconSpec(com.jamie.jamiebingo.util.ItemStackUtil.empty(), INFINITY_TEXTURE);
        }
        if (token.equals("zzz")) {
            return new IconSpec(com.jamie.jamiebingo.util.ItemStackUtil.empty(), ZZZ_TEXTURE);
        }
        if (token.contains("chiseled bookshelf")) {
            return new IconSpec(item("minecraft:chiseled_bookshelf"), null);
        }
        if (token.contains("wither rose")) {
            return new IconSpec(item("minecraft:wither_rose"), null);
        }
        if (token.contains("advancement background")) {
            return new IconSpec(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/gui/advancements/backgrounds/stone.png")
            );
        }
        if (token.contains("advancement icon") || token.equals("advancement") || token.contains("advancement")) {
            return new IconSpec(
                    com.jamie.jamiebingo.util.ItemStackUtil.empty(),
                    ADVANCEMENT_ICON_TEXTURE
            );
        }

        if (token.contains("heart")) {
            return new IconSpec(com.jamie.jamiebingo.util.ItemStackUtil.empty(), HEART_TEXTURE);
        }
        if (token.contains("empty hunger bar")) {
            return new IconSpec(com.jamie.jamiebingo.util.ItemStackUtil.empty(), HUNGER_EMPTY_TEXTURE);
        }
        if (token.contains("hunger bar") || token.equals("hunger bar") || token.equals("hunger")) {
            return new IconSpec(com.jamie.jamiebingo.util.ItemStackUtil.empty(), HUNGER_FULL_TEXTURE);
        }
        if (token.contains("experience bar") || token.contains("exp bar") || token.contains("xp bar")) {
            return new IconSpec(com.jamie.jamiebingo.util.ItemStackUtil.empty(), EXP_BAR_TEXTURE);
        }

        ItemStack potionItem = potionItemForToken(token);
        if (potionItem != null) {
            return new IconSpec(potionItem, null);
        }

        for (Map.Entry<String, String> e : SPECIAL_ITEMS.entrySet()) {
            if (token.equals(e.getKey())) {
                return new IconSpec(item(e.getValue()), null);
            }
        }
        List<Map.Entry<String, String>> sorted = new ArrayList<>(SPECIAL_ITEMS.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));
        for (Map.Entry<String, String> e : sorted) {
            if (token.contains(e.getKey())) {
                return new IconSpec(item(e.getValue()), null);
            }
        }

        for (Map.Entry<String, String> e : EFFECT_TEXTURES.entrySet()) {
            if (token.contains(e.getKey())) {
                return new IconSpec(com.jamie.jamiebingo.util.ItemStackUtil.empty(), com.jamie.jamiebingo.util.IdUtil.id(e.getValue()));
            }
        }

        if (token.contains("water texture") || token.equals("water") || token.contains("water source")) {
            return new IconSpec(com.jamie.jamiebingo.util.ItemStackUtil.empty(), WATER_TEXTURE);
        }
        if (token.contains("fire texture") || token.equals("fire")) {
            return new IconSpec(com.jamie.jamiebingo.util.ItemStackUtil.empty(), FIRE_TEXTURE);
        }
        if (token.contains("nether portal")) {
            return new IconSpec(com.jamie.jamiebingo.util.ItemStackUtil.empty(), NETHER_PORTAL_TEXTURE);
        }
        if (token.contains("spider eye")) {
            return new IconSpec(item("minecraft:spider_eye"), null);
        }

        EntityType<? extends LivingEntity> matched = matchEntityToken(token);
        if (matched == null && token.contains("baby ")) {
            matched = matchEntityToken(token.replace("baby ", "").trim());
        }
        if (matched != null) {
            return new IconSpec(matched);
        }

        String normalized = token
                .replace("'", "")
                .replace("\"", "")
                .replace(".", "")
                .replace("  ", " ")
                .trim()
                .replace(" ", "_");

        if (normalized.contains("diamond_deepslate_ore")) {
            normalized = normalized.replace("diamond_deepslate_ore", "deepslate_diamond_ore");
        }
        if (normalized.contains("emerald_deepslate_ore")) {
            normalized = normalized.replace("emerald_deepslate_ore", "deepslate_emerald_ore");
        }

        if (normalized.contains("gold_") && !normalized.contains("golden_")) {
            if (normalized.matches(".*gold_(boots|leggings|chestplate|helmet|horse_armor|shovel|hoe|sword|axe|pickaxe|spear).*")) {
                normalized = normalized.replace("gold_", "golden_");
            }
        }

        if (normalized.contains("leather_tunic")) normalized = normalized.replace("leather_tunic", "leather_chestplate");
        if (normalized.contains("leather_cap")) normalized = normalized.replace("leather_cap", "leather_helmet");
        if (normalized.contains("leather_pants")) normalized = normalized.replace("leather_pants", "leather_leggings");

        ItemStack stack = item("minecraft:" + normalized);
        if (!stack.isEmpty()) {
            return new IconSpec(stack, null);
        }

        return new IconSpec(item("minecraft:barrier"), null);
    }

    private static boolean isBarrierSpec(IconSpec spec) {
        if (spec == null) return false;
        if (spec.entityType != null || spec.texture != null) return false;
        return spec.item != null && !spec.item.isEmpty() && spec.item.is(Items.BARRIER);
    }

    private static EntityType<? extends LivingEntity> matchEntityToken(String token) {
        if (token == null || token.isBlank()) return null;
        String t = token.toLowerCase().trim().replace("_", " ");
        for (String key : MOB_ENTITY_KEYS) {
            if (t.matches(".*\\b" + Pattern.quote(key) + "\\b.*")) {
                return MOB_ENTITIES.get(key);
            }
        }
        return null;
    }


    private static QuestIconData applyQuestEntityOverride(QuestDefinition def, QuestIconData base) {
        if (def == null || base == null || def.id == null) return base;

        String id = def.id.toLowerCase(Locale.ROOT);
        if (!id.startsWith("quest.")) return base;
        if (id.startsWith("quest.kill_") && id.endsWith("_sheep")) return base;

        String token = null;
        if (id.startsWith("quest.breed_")) token = id.substring("quest.breed_".length());
        else if (id.startsWith("quest.tame_")) token = id.substring("quest.tame_".length());
        else if (id.startsWith("quest.kill_")) token = id.substring("quest.kill_".length());
        else if (id.startsWith("quest.find_")) token = id.substring("quest.find_".length());
        else if (id.startsWith("quest.ride_")) token = id.substring("quest.ride_".length());
        else if (id.startsWith("quest.shear_")) token = id.substring("quest.shear_".length());
        else if (id.startsWith("quest.milk_")) token = id.substring("quest.milk_".length());
        else if (id.startsWith("quest.die_by_")) token = id.substring("quest.die_by_".length());
        else if (id.startsWith("quest.die_to_")) token = id.substring("quest.die_to_".length());
        else if (id.startsWith("quest.kill_a_")) token = id.substring("quest.kill_a_".length());
        else if (id.startsWith("quest.breed_a_")) token = id.substring("quest.breed_a_".length());
        else if (id.startsWith("quest.tame_a_")) token = id.substring("quest.tame_a_".length());

        if (token == null || token.isBlank()) return base;
        if (token.matches(".*\\d+.*") || token.contains("unique") || token.contains("mobs")) return base;

        String tokenName = token.replace("_", " ");
        EntityType<? extends LivingEntity> idEntity = matchEntityToken(tokenName);
        if (idEntity == null) return base;
        if (base.mainEntityType == idEntity) return base;

        return new QuestIconData(
                base.mainIcon,
                base.rotatingIcons,
                base.cornerIcon,
                base.numberText,
                base.mainTexture,
                base.rotatingTextures,
                base.cornerTexture,
                base.mainRegion,
                base.cornerRegion,
                idEntity,
                base.rotatingEntities,
                base.cornerEntityType,
                base.mainEntityVariant,
                base.rotatingEntityVariants,
                base.mainEntityColor,
                base.rotatingEntityColors,
                base.cornerEntityColor,
                base.rotatingCornerEntityColors,
                base.rotatingCornerEntities,
                base.rotatingCornerIcons,
                base.rotatingCornerTextures,
                base.cornerCopies
        );
    }

    private static ItemStack buildColoredLeather(String questId) {
        String id = questId.toLowerCase();
        String colorPart = id.substring("quest.wear_".length(), id.indexOf("_colored_leather_"));
        String slotPart = id.substring(id.indexOf("_colored_leather_") + "_colored_leather_".length());

        net.minecraft.world.item.DyeColor dye = net.minecraft.world.item.DyeColor.byName(colorPart, null);
        if (dye == null) return com.jamie.jamiebingo.util.ItemStackUtil.empty();

        ItemStack stack;
        if (slotPart.contains("boots")) {
            stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_boots");
        } else if (slotPart.contains("leggings")) {
            stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_leggings");
        } else if (slotPart.contains("chestplate")) {
            stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_chestplate");
        } else if (slotPart.contains("helmet")) {
            stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:leather_helmet");
        } else {
            return com.jamie.jamiebingo.util.ItemStackUtil.empty();
        }

        int color = dye.getTextureDiffuseColor();
        com.jamie.jamiebingo.util.ItemStackComponentUtil.set(
                stack,
                DataComponents.DYED_COLOR,
                new DyedItemColor(color)
        );
        return stack;
    }

    private static ItemStack potionItemForToken(String token) {
        if (token == null || token.isBlank()) return null;
        if (!token.contains("potion")) return null;

        ItemStack stack;
        if (token.contains("lingering")) {
            stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:lingering_potion");
        } else if (token.contains("splash")) {
            stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:splash_potion");
        } else {
            stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:potion");
        }

        Holder<net.minecraft.world.item.alchemy.Potion> potion = null;
        if (token.contains("water breathing")) {
            potion = net.minecraft.world.item.alchemy.Potions.WATER_BREATHING;
        } else if (token.contains("healing")) {
            potion = net.minecraft.world.item.alchemy.Potions.HEALING;
        } else if (token.contains("invisibility")) {
            potion = net.minecraft.world.item.alchemy.Potions.INVISIBILITY;
        } else if (token.contains("swiftness") || token.contains("speed")) {
            potion = net.minecraft.world.item.alchemy.Potions.SWIFTNESS;
        } else if (token.contains("leaping")) {
            potion = net.minecraft.world.item.alchemy.Potions.LEAPING;
        } else if (token.contains("regeneration")) {
            potion = net.minecraft.world.item.alchemy.Potions.REGENERATION;
        }

        if (potion != null) {
            com.jamie.jamiebingo.util.ItemStackComponentUtil.set(
                    stack,
                    DataComponents.POTION_CONTENTS,
                    new PotionContents(potion)
            );
        }

        return stack;
    }

    private static Identifier effectTexture(String key) {
        if (key == null) return null;
        String path = EFFECT_TEXTURES.get(key);
        if (path == null) return null;
        return com.jamie.jamiebingo.util.IdUtil.id(path);
    }

    private static ItemStack waterBottle() {
        ItemStack stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:potion");
        com.jamie.jamiebingo.util.ItemStackComponentUtil.set(
                stack,
                DataComponents.POTION_CONTENTS,
                new PotionContents(net.minecraft.world.item.alchemy.Potions.WATER)
        );
        return stack;
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends LivingEntity> resolveLivingEntityType(
            String id,
            EntityType<? extends LivingEntity> fallback
    ) {
        try {
            EntityType<?> resolved = ForgeRegistries.ENTITY_TYPES.getValue(com.jamie.jamiebingo.util.IdUtil.id(id));
            if (resolved != null) return (EntityType<? extends LivingEntity>) resolved;
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static ItemStack item(String id) {
        try {
            Item i = ForgeRegistries.ITEMS.getValue(com.jamie.jamiebingo.util.IdUtil.id(id));
            return i == null ? com.jamie.jamiebingo.util.ItemStackUtil.empty() : new ItemStack(i);
        } catch (Exception ignored) {}
        return com.jamie.jamiebingo.util.ItemStackUtil.empty();
    }
}


