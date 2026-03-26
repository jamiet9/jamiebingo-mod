package com.jamie.jamiebingo.quest;

import com.jamie.jamiebingo.bingo.CardComposition;
import com.jamie.jamiebingo.bingo.WinCondition;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.data.BroadcastHelper;
import com.jamie.jamiebingo.data.TeamData;
import com.jamie.jamiebingo.data.TeamScoreData;
import com.jamie.jamiebingo.mines.MineModeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import com.jamie.jamiebingo.util.DataComponents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.Leashable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.stats.Stats;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.SaplingGrowTreeEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber
public class QuestEvents {

    private static final int INVENTORY_SCAN_INTERVAL = 20;
    private static final int LEADER_SCAN_INTERVAL = 40;
    private static final int ADVANCEMENT_SCAN_INTERVAL = 100;
    private static final int SPRINT_DISTANCE_TARGET_CM = 100000;
    private static final int DAMAGE_SCALE = 1;
    private static final int TAKE_DAMAGE_TARGET = 200 * DAMAGE_SCALE;
    private static final int DEAL_DAMAGE_TARGET = 400 * DAMAGE_SCALE;
    private static final int OPPONENT_DAMAGE_TARGET = 100 * DAMAGE_SCALE;
    private static final int PUMPKIN_WEAR_TICKS = 20 * 60 * 5;
    private static final int SPYGLASS_CHECK_INTERVAL = 5;
    private static final int CROUCH_DISTANCE_TARGET_CM = 10000;
    private static final int SWIM_DISTANCE_TARGET_CM = 50000;
    private static final int BOAT_DISTANCE_TARGET_CM = 200000;
    private static final int LOVE_CHECK_INTERVAL = 10;
    private static final int LOVE_CHECK_TIMEOUT = 100;
    private static final int LOVE_OWNER_TIMEOUT = 2400;
    private static final int TAME_CHECK_INTERVAL = 10;
    private static final int TAME_CHECK_TIMEOUT = 200;
    private static final int WHOLE_TEAM_SAME_TIME_TICKS = 200;
    private static final int LOOK_DIFFERENT_MEMBER_WINDOW_TICKS = 40;

    private static final Map<String, UUID> opponentQuestFirstTeam = new HashMap<>();
    private static final Map<UUID, Integer> teamDeathCounts = new HashMap<>();
    private static final Map<UUID, Integer> teamDamageTaken = new HashMap<>();
    private static final Map<String, UUID> leaderQuestOwner = new HashMap<>();

    private static final Map<UUID, Vec3> lastSprintPos = new HashMap<>();
    private static final Map<UUID, ResourceKey<Level>> lastDimensions = new HashMap<>();
    private static final Set<UUID> milkHadEffects = new HashSet<>();
    private static final Map<UUID, Set<Identifier>> uniqueFoods = new HashMap<>();
    private static final Map<UUID, Set<Item>> uniqueBowlFoods = new HashMap<>();
    private static final Map<UUID, Set<Identifier>> uniqueHostileKills = new HashMap<>();
    private static final Map<UUID, Set<String>> uniqueBreeds = new HashMap<>();
    private static final Map<UUID, Set<Identifier>> uniqueChickenVariants = new HashMap<>();
    private static final Map<UUID, Set<Identifier>> uniqueCowVariants = new HashMap<>();
    private static final Map<UUID, Set<Identifier>> uniquePigVariants = new HashMap<>();
    private static final Map<UUID, Set<Identifier>> uniqueWolfVariants = new HashMap<>();
    private static final Map<UUID, Set<Identifier>> uniqueCatVariants = new HashMap<>();
    private static final Map<UUID, Set<Identifier>> netherBiomes = new HashMap<>();
    private static final Map<UUID, Set<Identifier>> uniqueCrafts = new HashMap<>();
    private static final Map<UUID, Set<String>> uniqueArmorTrims = new HashMap<>();
    private static final Map<UUID, Set<Identifier>> uniqueSpyglassLooks = new HashMap<>();
    private static final Map<UUID, Set<Identifier>> uniqueArmorTrimTemplates = new HashMap<>();
    private static final Map<UUID, Set<Identifier>> uniqueBiomes = new HashMap<>();
    private static final Map<UUID, Set<Identifier>> uniqueCaveBiomes = new HashMap<>();
    private static final Map<UUID, Set<String>> uniqueDamageSources = new HashMap<>();
    private static final Map<UUID, Set<Identifier>> uniqueRaidMobKills = new HashMap<>();
    private static final Map<UUID, Set<Identifier>> uniqueGolemKills = new HashMap<>();
    private static final Map<UUID, Set<Identifier>> uniqueGolemBuilds = new HashMap<>();
    private static final Map<UUID, Integer> crouchDistanceCm = new HashMap<>();
    private static final Map<UUID, Integer> swimDistanceCm = new HashMap<>();
    private static final Map<UUID, Integer> boatDistanceCm = new HashMap<>();
    private static final Map<UUID, Vec3> lastCrouchPos = new HashMap<>();
    private static final Map<UUID, Vec3> lastSwimPos = new HashMap<>();
    private static final Map<UUID, Vec3> lastBoatPos = new HashMap<>();
    private static final Map<UUID, Integer> lastCakeEatTick = new HashMap<>();
    private static final Map<UUID, Integer> lastEnderPearlTick = new HashMap<>();
    private static final Map<UUID, Integer> lastDeathTick = new HashMap<>();
    private static final Map<UUID, Integer> lastBreedTick = new HashMap<>();
    private static final Map<UUID, UUID> lastLookedTeammateByViewer = new HashMap<>();
    private static final Map<UUID, Integer> lastLookedTeammateTick = new HashMap<>();
    private static final Map<UUID, Integer> babyMobKills = new HashMap<>();
    private static final Map<UUID, Integer> advancementBaseCounts = new HashMap<>();
    private static final Map<UUID, Integer> advancementEarnedCounts = new HashMap<>();
    private static final Map<UUID, Set<Identifier>> advancementEarnedIds = new HashMap<>();
    private static final Map<UUID, Integer> lastVillagerTrades = new HashMap<>();
    private static final Map<UUID, Integer> lastVineTick = new HashMap<>();
    private static final Map<UUID, Integer> lastTntMinecartTick = new HashMap<>();
    private static final Map<UUID, ItemStack> lastAnvilOutput = new HashMap<>();
    private static final Map<UUID, ItemStack> lastLoomOutput = new HashMap<>();
    private static final Map<UUID, ItemStack> lastSmithingOutput = new HashMap<>();
    private static final Map<UUID, ItemStack> lastGrindstoneOutput = new HashMap<>();
    private static final Map<UUID, Boolean> lastGrindstoneHadEnchant = new HashMap<>();
    private static final Map<UUID, Boolean> lastGrindstoneInputHadEnchant = new HashMap<>();
    private static final Map<UUID, Integer> lastGrindstoneEnchantTick = new HashMap<>();
    private static final Map<UUID, Integer> lastGrindstoneXpTriggerTick = new HashMap<>();
    private static final Map<UUID, ItemStack> lastStonecutterOutput = new HashMap<>();
    private static final Map<UUID, ItemStack> lastCrafterOutput = new HashMap<>();
    private static final Map<UUID, Boolean> sleepAloneCandidate = new HashMap<>();
    private static final Map<UUID, Integer> lastSpyglassTick = new HashMap<>();
    private static final Map<UUID, Vec3> lastHappyGhastPos = new HashMap<>();
    private static final Map<UUID, Integer> happyGhastDistanceCm = new HashMap<>();
    private static final Map<UUID, Vec3> lastMinecart25Pos = new HashMap<>();
    private static final Map<UUID, Integer> minecart25DistanceCm = new HashMap<>();
    private static final Map<UUID, Vec3> lastLadderPos = new HashMap<>();
    private static final Map<UUID, Integer> ladderClimbCm = new HashMap<>();
    private static final Map<UUID, Set<Identifier>> uniqueWolfKillMobs = new HashMap<>();
    private static final Map<UUID, LoveOwner> loveOwnerByEntity = new HashMap<>();
    private static final Map<UUID, PendingProjectileKill> pendingProjectileKills = new HashMap<>();
    private static final Map<UUID, Integer> recentHappyGhastLeadUseTick = new HashMap<>();
    private static final Map<BlockPos, SaplingOwner> saplingOwners = new HashMap<>();

    private static boolean isBabyMob(LivingEntity entity) {
        if (entity == null) return false;
        if (entity instanceof AgeableMob ageable && ageable.isBaby()) {
            return true;
        }
        try {
            Method isBaby = entity.getClass().getMethod("isBaby");
            if (isBaby.getReturnType() == boolean.class || isBaby.getReturnType() == Boolean.class) {
                Object out = isBaby.invoke(entity);
                return out instanceof Boolean b && b;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static final List<PendingBlockCheck> pendingComposterChecks = new ArrayList<>();
    private static final List<PendingBlockCheck> pendingAnchorChecks = new ArrayList<>();
    private static final List<PendingEggCheck> pendingEggChecks = new ArrayList<>();
    private static final List<PendingArmorStandCheck> pendingArmorStandChecks = new ArrayList<>();
    private static final List<PendingEndCrystalCheck> pendingEndCrystalChecks = new ArrayList<>();
    private static final List<PendingVaultKeyCheck> pendingVaultKeyChecks = new ArrayList<>();
    private static final List<PendingWolfArmorCheck> pendingWolfArmorChecks = new ArrayList<>();
    private static final List<PendingTameCheck> pendingTameChecks = new ArrayList<>();
    private static final List<PendingCrafterUse> pendingCrafterUses = new ArrayList<>();
    private static final List<PendingShieldDisableCheck> pendingShieldDisableChecks = new ArrayList<>();
    private static final List<PendingLoveCheck> pendingLoveChecks = new ArrayList<>();
    private static final List<PendingButtonPressCheck> pendingButtonPressChecks = new ArrayList<>();
    private static final List<PendingContainerInsertCheck> pendingContainerInsertChecks = new ArrayList<>();
    private static final List<PendingCraftingTableInsertCheck> pendingCraftingTableInsertChecks = new ArrayList<>();
    private static final List<PendingItemLavaCheck> pendingItemLavaChecks = new ArrayList<>();

    private static final Map<Identifier, String> BREED_QUESTS = Map.ofEntries(
            Map.entry(id("minecraft:chicken"), "quest.breed_chicken"),
            Map.entry(id("minecraft:cow"), "quest.breed_cow"),
            Map.entry(id("minecraft:goat"), "quest.breed_goat"),
            Map.entry(id("minecraft:pig"), "quest.breed_pig"),
            Map.entry(id("minecraft:sheep"), "quest.breed_sheep"),
            Map.entry(id("minecraft:fox"), "quest.breed_fox"),
            Map.entry(id("minecraft:hoglin"), "quest.breed_hoglin"),
            Map.entry(id("minecraft:horse"), "quest.breed_horse"),
            Map.entry(id("minecraft:ocelot"), "quest.breed_ocelot"),
            Map.entry(id("minecraft:rabbit"), "quest.breed_rabbit"),
            Map.entry(id("minecraft:strider"), "quest.breed_strider"),
            Map.entry(id("minecraft:armadillo"), "quest.breed_armadillo"),
            Map.entry(id("minecraft:mooshroom"), "quest.breed_mooshroom"),
            Map.entry(id("minecraft:panda"), "quest.breed_panda"),
            Map.entry(id("minecraft:turtle"), "quest.breed_turtle"),
            Map.entry(id("minecraft:wolf"), "quest.breed_wolf"),
            Map.entry(id("minecraft:camel"), "quest.breed_camels"),
            Map.entry(id("minecraft:nautilus"), "quest.breed_nautilus"),
            Map.entry(id("minecraft:frog"), "quest.breed_frogs")
    );

    private static final List<String> COLOR_NAMES = List.of(
            "black", "blue", "brown", "cyan", "gray", "green", "light_blue", "light_gray",
            "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow"
    );

    private static final Set<Item> BANNER_PATTERN_ITEMS = itemSet(
            "minecraft:flower_banner_pattern",
            "minecraft:creeper_banner_pattern",
            "minecraft:skull_banner_pattern",
            "minecraft:mojang_banner_pattern",
            "minecraft:globe_banner_pattern",
            "minecraft:piglin_banner_pattern"
    );

    private static final Set<Item> MUSIC_DISC_ITEMS = itemSet(
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
            "minecraft:disc_fragment_5"
    );

    private static final Set<Item> PRESSURE_PLATE_ITEMS = itemSet(
            "minecraft:oak_pressure_plate",
            "minecraft:spruce_pressure_plate",
            "minecraft:birch_pressure_plate",
            "minecraft:jungle_pressure_plate",
            "minecraft:acacia_pressure_plate",
            "minecraft:dark_oak_pressure_plate",
            "minecraft:mangrove_pressure_plate",
            "minecraft:cherry_pressure_plate",
            "minecraft:bamboo_pressure_plate",
            "minecraft:crimson_pressure_plate",
            "minecraft:warped_pressure_plate",
            "minecraft:stone_pressure_plate",
            "minecraft:polished_blackstone_pressure_plate",
            "minecraft:light_weighted_pressure_plate",
            "minecraft:heavy_weighted_pressure_plate"
    );

    private static final Set<Item> BRICK_BLOCK_ITEMS = itemSet(
            "minecraft:bricks",
            "minecraft:stone_bricks",
            "minecraft:nether_bricks",
            "minecraft:red_nether_bricks",
            "minecraft:end_stone_bricks",
            "minecraft:prismarine_bricks",
            "minecraft:mud_bricks",
            "minecraft:deepslate_bricks",
            "minecraft:tuff_bricks"
    );

    private static final Set<Item> BUCKET_ITEMS = itemSet(
            "minecraft:bucket",
            "minecraft:water_bucket",
            "minecraft:lava_bucket",
            "minecraft:milk_bucket",
            "minecraft:powder_snow_bucket",
            "minecraft:cod_bucket",
            "minecraft:salmon_bucket",
            "minecraft:pufferfish_bucket",
            "minecraft:tropical_fish_bucket",
            "minecraft:axolotl_bucket",
            "minecraft:tadpole_bucket"
    );

    private static final Set<Item> WORKSTATION_ITEMS = itemSet(
            "minecraft:barrel",
            "minecraft:blast_furnace",
            "minecraft:smoker",
            "minecraft:cartography_table",
            "minecraft:fletching_table",
            "minecraft:grindstone",
            "minecraft:loom",
            "minecraft:smithing_table",
            "minecraft:stonecutter",
            "minecraft:composter",
            "minecraft:brewing_stand",
            "minecraft:lectern",
            "minecraft:cauldron"
    );

    private static final Set<Item> MINECART_ITEMS = itemSet(
            "minecraft:minecart",
            "minecraft:chest_minecart",
            "minecraft:hopper_minecart",
            "minecraft:tnt_minecart",
            "minecraft:furnace_minecart"
    );

    private static final Set<Item> FURNACE_VARIANTS = itemSet(
            "minecraft:furnace",
            "minecraft:blast_furnace",
            "minecraft:smoker"
    );

    private static final Set<Item> FROGLIGHTS = itemSet(
            "minecraft:ochre_froglight",
            "minecraft:verdant_froglight",
            "minecraft:pearlescent_froglight"
    );

    private static final Set<Item> MUSHROOM_ITEMS = itemSet(
            "minecraft:red_mushroom",
            "minecraft:brown_mushroom",
            "minecraft:crimson_fungus",
            "minecraft:warped_fungus"
    );

    private static final Set<Item> PUMPKIN_TYPES = itemSet(
            "minecraft:pumpkin",
            "minecraft:carved_pumpkin",
            "minecraft:jack_o_lantern"
    );

    private static final Set<Item> RAIL_TYPES = itemSet(
            "minecraft:rail",
            "minecraft:powered_rail",
            "minecraft:detector_rail",
            "minecraft:activator_rail"
    );

    private static final Set<Item> TORCH_TYPES = itemSet(
            "minecraft:torch",
            "minecraft:soul_torch",
            "minecraft:copper_torch",
            "minecraft:redstone_torch"
    );

    private static final Set<Item> NAUTILUS_ARMOR_TYPES = itemSet(
            "minecraft:copper_nautilus_armor",
            "minecraft:iron_nautilus_armor",
            "minecraft:golden_nautilus_armor",
            "minecraft:gold_nautilus_armor",
            "minecraft:diamond_nautilus_armor",
            "minecraft:netherite_nautilus_armor"
    );

    private static final Set<Item> ALL_WOOL_TYPES = itemSet(
            "minecraft:black_wool",
            "minecraft:blue_wool",
            "minecraft:brown_wool",
            "minecraft:cyan_wool",
            "minecraft:gray_wool",
            "minecraft:green_wool",
            "minecraft:light_blue_wool",
            "minecraft:light_gray_wool",
            "minecraft:lime_wool",
            "minecraft:magenta_wool",
            "minecraft:orange_wool",
            "minecraft:pink_wool",
            "minecraft:purple_wool",
            "minecraft:red_wool",
            "minecraft:white_wool",
            "minecraft:yellow_wool"
    );

    private static final Set<Item> LOG_TYPES = itemSet(
            "minecraft:oak_log",
            "minecraft:spruce_log",
            "minecraft:birch_log",
            "minecraft:jungle_log",
            "minecraft:acacia_log",
            "minecraft:dark_oak_log",
            "minecraft:mangrove_log",
            "minecraft:cherry_log",
            "minecraft:pale_oak_log"
    );

    private static final Set<Item> OVERWORLD_ORE_BLOCKS = itemSet(
            "minecraft:coal_ore",
            "minecraft:deepslate_coal_ore",
            "minecraft:iron_ore",
            "minecraft:deepslate_iron_ore",
            "minecraft:copper_ore",
            "minecraft:deepslate_copper_ore",
            "minecraft:gold_ore",
            "minecraft:deepslate_gold_ore",
            "minecraft:redstone_ore",
            "minecraft:deepslate_redstone_ore",
            "minecraft:lapis_ore",
            "minecraft:deepslate_lapis_ore",
            "minecraft:diamond_ore",
            "minecraft:deepslate_diamond_ore",
            "minecraft:emerald_ore",
            "minecraft:deepslate_emerald_ore"
    );

    private static final Set<Item> OVERWORLD_ORES = itemSet(
            "minecraft:coal",
            "minecraft:copper_ingot",
            "minecraft:lapis_lazuli",
            "minecraft:iron_ingot",
            "minecraft:redstone",
            "minecraft:gold_ingot",
            "minecraft:diamond",
            "minecraft:emerald"
    );

    private static final Set<Item> NETHER_ORE_BLOCKS = itemSet(
            "minecraft:nether_gold_ore",
            "minecraft:nether_quartz_ore",
            "minecraft:ancient_debris"
    );

    private static final Set<Item> NETHER_ORES = itemSet(
            "minecraft:gold_nugget",
            "minecraft:quartz",
            "minecraft:netherite_scrap"
    );

    private static final Set<Item> COBBLESTONE_VARIANTS = itemSet(
            "minecraft:cobblestone",
            "minecraft:cobblestone_stairs",
            "minecraft:cobblestone_slab",
            "minecraft:cobblestone_wall",
            "minecraft:mossy_cobblestone",
            "minecraft:mossy_cobblestone_stairs",
            "minecraft:mossy_cobblestone_slab",
            "minecraft:mossy_cobblestone_wall"
    );

    private static final Set<Item> STONE_BRICK_VARIANTS = itemSet(
            "minecraft:stone_bricks",
            "minecraft:cracked_stone_bricks",
            "minecraft:stone_brick_stairs",
            "minecraft:stone_brick_slab",
            "minecraft:stone_brick_wall",
            "minecraft:chiseled_stone_bricks",
            "minecraft:mossy_stone_bricks",
            "minecraft:mossy_stone_brick_stairs",
            "minecraft:mossy_stone_brick_slab",
            "minecraft:mossy_stone_brick_wall"
    );

    private static final Set<Item> SANDSTONE_VARIANTS = itemSet(
            "minecraft:sandstone",
            "minecraft:sandstone_stairs",
            "minecraft:sandstone_slab",
            "minecraft:sandstone_wall",
            "minecraft:chiseled_sandstone",
            "minecraft:smooth_sandstone",
            "minecraft:smooth_sandstone_stairs",
            "minecraft:smooth_sandstone_slab",
            "minecraft:cut_sandstone",
            "minecraft:cut_sandstone_slab"
    );

    private static final Set<Item> RED_SANDSTONE_VARIANTS = itemSet(
            "minecraft:red_sandstone",
            "minecraft:red_sandstone_stairs",
            "minecraft:red_sandstone_slab",
            "minecraft:red_sandstone_wall",
            "minecraft:chiseled_red_sandstone",
            "minecraft:smooth_red_sandstone",
            "minecraft:smooth_red_sandstone_stairs",
            "minecraft:smooth_red_sandstone_slab",
            "minecraft:cut_red_sandstone",
            "minecraft:cut_red_sandstone_slab"
    );

    private static final Set<Item> MOSSY_VARIANTS = itemSet(
            "minecraft:mossy_cobblestone",
            "minecraft:mossy_cobblestone_stairs",
            "minecraft:mossy_cobblestone_slab",
            "minecraft:mossy_cobblestone_wall",
            "minecraft:mossy_stone_bricks",
            "minecraft:mossy_stone_brick_stairs",
            "minecraft:mossy_stone_brick_slab",
            "minecraft:mossy_stone_brick_wall"
    );

    private static final Set<Item> BLACKSTONE_VARIANTS = itemSet(
            "minecraft:blackstone",
            "minecraft:gilded_blackstone",
            "minecraft:blackstone_stairs",
            "minecraft:blackstone_slab",
            "minecraft:blackstone_wall",
            "minecraft:chiseled_polished_blackstone",
            "minecraft:polished_blackstone",
            "minecraft:polished_blackstone_stairs",
            "minecraft:polished_blackstone_slab",
            "minecraft:polished_blackstone_wall",
            "minecraft:polished_blackstone_pressure_plate",
            "minecraft:polished_blackstone_button",
            "minecraft:polished_blackstone_bricks",
            "minecraft:cracked_polished_blackstone_bricks",
            "minecraft:polished_blackstone_brick_stairs",
            "minecraft:polished_blackstone_brick_slab",
            "minecraft:polished_blackstone_brick_wall"
    );

    private static final Set<Item> END_STONE_VARIANTS = itemSet(
            "minecraft:end_stone",
            "minecraft:end_stone_bricks",
            "minecraft:end_stone_brick_stairs",
            "minecraft:end_stone_brick_slab",
            "minecraft:end_stone_brick_wall"
    );

    private static final Set<Item> SNOW_VARIANTS = itemSet(
            "minecraft:snow_block",
            "minecraft:snow",
            "minecraft:powder_snow_bucket",
            "minecraft:snowball"
    );

    private static final Set<Identifier> RAID_MOBS = Set.of(
            id("minecraft:pillager"),
            id("minecraft:vindicator"),
            id("minecraft:evoker"),
            id("minecraft:ravager"),
            id("minecraft:witch"),
            id("minecraft:vex")
    );

    private static final Set<Identifier> GOLEM_TYPES = Set.of(
            id("minecraft:iron_golem"),
            id("minecraft:snow_golem"),
            id("minecraft:copper_golem")
    );

    private static final Set<Identifier> UNIQUE_BREED_TYPES = Set.of(
            id("minecraft:chicken"),
            id("minecraft:pig"),
            id("minecraft:cow"),
            id("minecraft:cat"),
            id("minecraft:ocelot"),
            id("minecraft:wolf"),
            id("minecraft:horse"),
            id("minecraft:goat"),
            id("minecraft:sheep"),
            id("minecraft:parrot"),
            id("minecraft:hoglin"),
            id("minecraft:strider"),
            id("minecraft:armadillo"),
            id("minecraft:mooshroom"),
            id("minecraft:panda"),
            id("minecraft:turtle")
    );

    private static final Set<Identifier> NETHER_BIOMES = Set.of(
            id("minecraft:nether_wastes"),
            id("minecraft:soul_sand_valley"),
            id("minecraft:crimson_forest"),
            id("minecraft:warped_forest"),
            id("minecraft:basalt_deltas")
    );

    private static final Set<Identifier> CAVE_BIOMES = Set.of(
            id("minecraft:dripstone_caves"),
            id("minecraft:lush_caves"),
            id("minecraft:deep_dark")
    );

    private static final Map<Identifier, String> LEAD_TARGET_QUESTS = Map.ofEntries(
            Map.entry(id("minecraft:cow"), "quest.attach_a_lead_to_a_cow"),
            Map.entry(id("minecraft:dolphin"), "quest.attach_a_lead_to_a_dolphin"),
            Map.entry(id("minecraft:fox"), "quest.attach_a_lead_to_a_fox"),
            Map.entry(id("minecraft:frog"), "quest.attach_a_lead_to_a_frog"),
            Map.entry(id("minecraft:strider"), "quest.attach_a_lead_to_a_strider"),
            Map.entry(id("minecraft:iron_golem"), "quest.attach_a_lead_to_an_iron_golem"),
            Map.entry(id("minecraft:boat"), "quest.attach_a_lead_to_a_cherry_chest_boat"),
            Map.entry(id("minecraft:chest_boat"), "quest.attach_a_lead_to_a_cherry_chest_boat")
    );
    private static TagKey<EntityType<?>> arthropodTag() {
        return TagKey.create(Registries.ENTITY_TYPE, id("minecraft:arthropod"));
    }

    private static TagKey<EntityType<?>> undeadTag() {
        return TagKey.create(Registries.ENTITY_TYPE, id("minecraft:undead"));
    }

    private static Identifier id(String value) {
        return com.jamie.jamiebingo.util.IdUtil.id(value);
    }

    private static Set<Item> itemSet(String... ids) {
        Set<Item> out = new HashSet<>();
        for (String id : ids) {
            Item item = com.jamie.jamiebingo.util.ItemLookupUtil.item(id);
            if (item != null) out.add(item);
        }
        return out;
    }

    public static void resetForGame() {
        resetForGame(null);
    }

    public static void resetForGame(MinecraftServer server) {
        opponentQuestFirstTeam.clear();
        teamDeathCounts.clear();
        teamDamageTaken.clear();
        leaderQuestOwner.clear();
        lastSprintPos.clear();
        lastDimensions.clear();
        milkHadEffects.clear();
        uniqueFoods.clear();
        uniqueBowlFoods.clear();
        uniqueHostileKills.clear();
        uniqueBreeds.clear();
        uniqueChickenVariants.clear();
        uniqueCowVariants.clear();
        uniquePigVariants.clear();
        uniqueWolfVariants.clear();
        uniqueCatVariants.clear();
        netherBiomes.clear();
        uniqueCrafts.clear();
        uniqueArmorTrims.clear();
        uniqueSpyglassLooks.clear();
        uniqueArmorTrimTemplates.clear();
        uniqueBiomes.clear();
        uniqueCaveBiomes.clear();
        uniqueDamageSources.clear();
        uniqueRaidMobKills.clear();
        uniqueGolemKills.clear();
        uniqueGolemBuilds.clear();
        crouchDistanceCm.clear();
        swimDistanceCm.clear();
        boatDistanceCm.clear();
        lastCrouchPos.clear();
        lastSwimPos.clear();
        lastBoatPos.clear();
        lastHappyGhastPos.clear();
        happyGhastDistanceCm.clear();
        lastMinecart25Pos.clear();
        minecart25DistanceCm.clear();
        lastLadderPos.clear();
        ladderClimbCm.clear();
        babyMobKills.clear();
        uniqueWolfKillMobs.clear();
        advancementBaseCounts.clear();
        advancementEarnedCounts.clear();
        advancementEarnedIds.clear();
        lastVillagerTrades.clear();
        lastVineTick.clear();
        lastTntMinecartTick.clear();
        lastAnvilOutput.clear();
        lastLoomOutput.clear();
        lastSmithingOutput.clear();
        lastGrindstoneOutput.clear();
        lastGrindstoneHadEnchant.clear();
        lastGrindstoneInputHadEnchant.clear();
        lastGrindstoneEnchantTick.clear();
        lastStonecutterOutput.clear();
        lastCrafterOutput.clear();
        sleepAloneCandidate.clear();
        lastSpyglassTick.clear();
        lastBreedTick.clear();
        lastLookedTeammateByViewer.clear();
        lastLookedTeammateTick.clear();
        loveOwnerByEntity.clear();
        pendingProjectileKills.clear();
        pendingComposterChecks.clear();
        pendingAnchorChecks.clear();
        pendingEggChecks.clear();
        pendingArmorStandChecks.clear();
        pendingEndCrystalChecks.clear();
        pendingVaultKeyChecks.clear();
        pendingWolfArmorChecks.clear();
        pendingTameChecks.clear();
        pendingCrafterUses.clear();
        pendingShieldDisableChecks.clear();
        pendingLoveChecks.clear();
        pendingButtonPressChecks.clear();
        pendingContainerInsertChecks.clear();
        pendingCraftingTableInsertChecks.clear();
        pendingItemLavaChecks.clear();
        recentHappyGhastLeadUseTick.clear();
        saplingOwners.clear();

        if (server != null && advancementsLoaded(server)) {
            for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                int total = countCompletedAdvancements(player, server);
                UUID id = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
                advancementBaseCounts.put(id, total);
                advancementEarnedCounts.put(id, 0);
                advancementEarnedIds.put(id, new HashSet<>());
            }
        }
    }

    public static void initAdvancementBase(ServerPlayer player) {
        if (player == null) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null || !advancementsLoaded(server)) return;
        int total = countCompletedAdvancements(player, server);
        advancementBaseCounts.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(player), total);
        advancementEarnedCounts.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(player), 0);
        advancementEarnedIds.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(player), new HashSet<>());
    }

    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isQuestActive(player)) return;
        var holder = event.getAdvancement();
        if (holder == null) return;
        if (!shouldCountAdvancement(holder)) return;
        Identifier advId = getAdvancementId(holder);
        Set<Identifier> set = advancementEarnedIds.computeIfAbsent(
                com.jamie.jamiebingo.util.EntityUtil.getUUID(player),
                k -> new HashSet<>()
        );
        if (advId != null && set.add(advId)) {
            advancementEarnedCounts.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(player), set.size());
        }
        triggerOpponentQuest(player, "quest.opponent_obtains_advancement");
        MineModeManager.onPlayerTrigger(
                com.jamie.jamiebingo.util.EntityServerUtil.getServer(player),
                player,
                MineModeManager.Trigger.OBTAIN_ADVANCEMENT
        );
    }

    private static boolean isQuestActive(ServerPlayer p) {
        if (p == null) return false;
        BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
        if (!data.isActive()) return false;
        if (data.startCountdownActive) return false;
        if (data.pregameBoxActive) return false;
        if (com.jamie.jamiebingo.world.PregameBoxManager.isInsideBox(p, data)) return false;
        if (!data.areQuestsReleased(com.jamie.jamiebingo.util.EntityUtil.getUUID(p))) return false;
        if (data.minesEnabled) return true;
        if (data.composition != CardComposition.CLASSIC_ONLY) return true;
        return data.currentCardHasQuests();
    }

    private static boolean hasQuest(MinecraftServer server, String questId) {
        if (server == null) return false;
        BingoGameData data = BingoGameData.get(server);
        return data != null && data.getCurrentCard() != null && data.cardContains(questId);
    }

    private static UUID getTeamId(ServerPlayer player) {
        TeamData teamData = TeamData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        return teamData.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
    }

    private static void completeForTeam(MinecraftServer server, UUID teamId, String questId) {
        if (server == null || teamId == null) return;
        TeamData teamData = TeamData.get(server);
        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);
        if (team == null || team.members.isEmpty()) return;

        for (UUID memberId : team.members) {
            ServerPlayer member = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
            if (member == null) continue;
            QuestTracker.complete(member, questId);
            return;
        }
    }

    private static void completeForAllOtherTeams(MinecraftServer server, UUID excludedTeamId, String questId) {
        TeamData teamData = TeamData.get(server);
        for (TeamData.TeamInfo team : teamData.getTeams()) {
            if (team.id.equals(excludedTeamId)) continue;
            if (team.members.isEmpty()) continue;
            completeForTeam(server, team.id, questId);
        }
    }

    private static void triggerOpponentQuest(ServerPlayer triggeringPlayer, String questId) {
        if (triggeringPlayer == null) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(triggeringPlayer);
        if (!hasQuest(server, questId)) return;

        if (opponentQuestFirstTeam.containsKey(questId)) return;

        UUID triggeringTeam = getTeamId(triggeringPlayer);
        if (triggeringTeam == null) return;

        opponentQuestFirstTeam.put(questId, triggeringTeam);
        completeForAllOtherTeams(server, triggeringTeam, questId);
    }

    private static void updateLeaderQuest(MinecraftServer server, String questId, Map<UUID, Integer> counts) {
        if (!hasQuest(server, questId)) return;

        UUID leader = null;
        int best = Integer.MIN_VALUE;
        boolean tie = false;

        for (var entry : counts.entrySet()) {
            int value = entry.getValue();
            if (value > best) {
                best = value;
                leader = entry.getKey();
                tie = false;
            } else if (value == best) {
                tie = true;
            }
        }

        if (tie || best <= 0) leader = null;

        UUID previous = leaderQuestOwner.get(questId);
        if (previous != null && !previous.equals(leader)) {
            BingoGameData data = BingoGameData.get(server);
            if (data.removeCompletedForTeam(previous, questId)) {
                TeamScoreData scores = TeamScoreData.get(server);
                if (data.winCondition == WinCondition.RARITY) {
                    scores.recomputeRarityScores(server, data);
                } else {
                    scores.recomputeStandardScores(server, data);
                }
                leaderQuestOwner.remove(questId);
            }
        }

        if (leader != null && !leader.equals(previous)) {
            leaderQuestOwner.put(questId, leader);
            completeForTeam(server, leader, questId);
        }
    }
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {

        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        if (com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) % LEADER_SCAN_INTERVAL == 0) {
            BingoGameData data = BingoGameData.get(server);
            if (data.isActive() && data.composition != CardComposition.CLASSIC_ONLY
                    && !data.startCountdownActive && !data.pregameBoxActive) {
                updateLeaderQuests(server);
            }
        }

        processPendingChecks(server);
        processSleepAlone(server);
        checkWholeTeamQuests(server);
        com.jamie.jamiebingo.casino.CasinoModeManager.tickFailSafe(server);
        com.jamie.jamiebingo.data.GameTimerTicker.tickRushOnly(server);
        MineModeManager.onServerTick(server);
        com.jamie.jamiebingo.power.PowerSlotManager.onServerTick(server);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;

        net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p);
        if (!(baseLevel instanceof ServerLevel level)) return;

        if (p.getY() >= level.getMaxY() - 1) {
            QuestTracker.complete(p, "quest.reach_sky_limit");
        }

        if (level.getBlockState(com.jamie.jamiebingo.util.EntityPosUtil.getBlockPos(p).below()).is(Blocks.BEDROCK)) {
            QuestTracker.complete(p, "quest.reach_bedrock");
        }
        if (baseLevel.dimension() == Level.NETHER && p.getY() >= 128.0D) {
            QuestTracker.complete(p, "quest.travel_to_nether_roof");
        }

        net.minecraft.world.food.FoodData food = com.jamie.jamiebingo.util.PlayerFoodUtil.getFoodData(p);
        if (food != null && com.jamie.jamiebingo.util.FoodDataUtil.getFoodLevel(food) <= 0) {
            QuestTracker.complete(p, "quest.empty_hunger_bar");
        }

        boolean hasWrittenBook = false;
        for (int i = 0; i < com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(p).getContainerSize(); i++) {
            ItemStack stack = com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(p).getItem(i);
            if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack) && stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:written_book"))) {
                hasWrittenBook = true;
                break;
            }
        }
        if (hasWrittenBook) {
            QuestTracker.complete(p, "quest.write_a_book");
        }

        if (baseLevel.dimension() == Level.OVERWORLD && p.isSleeping()) {
            int sleepers = 0;
            var server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(p);
            for (ServerPlayer other : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                if (other.isSleeping()) sleepers++;
            }
            if (sleepers == 1) {
                sleepAloneCandidate.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), true);
            }
        }

        if (com.jamie.jamiebingo.util.PlayerExperienceUtil.getExperienceLevel(p) >= 15) {
            QuestTracker.complete(p, "quest.reach_level_15");
        }

        if (com.jamie.jamiebingo.util.PlayerExperienceUtil.getExperienceLevel(p) >= 30) {
            QuestTracker.complete(p, "quest.reach_level_30");
        }

        if (p.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION)) {
            QuestTracker.complete(p, "quest.get_absorption");
        }

        if (p.hasEffect(net.minecraft.world.effect.MobEffects.JUMP_BOOST)) {
            QuestTracker.complete(p, "quest.get_jump_boost");
        }

        if (p.hasEffect(net.minecraft.world.effect.MobEffects.POISON)) {
            QuestTracker.complete(p, "quest.get_poisoned");
        }

        if (p.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)) {
            QuestTracker.complete(p, "quest.get_weakness");
        }

        if (p.hasEffect(net.minecraft.world.effect.MobEffects.BAD_OMEN)) {
            QuestTracker.complete(p, "quest.get_bad_omen");
        }

        if (p.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)) {
            QuestTracker.complete(p, "quest.get_glowing");
        }

        if (p.hasEffect(net.minecraft.world.effect.MobEffects.MINING_FATIGUE)) {
            QuestTracker.complete(p, "quest.get_mining_fatigue");
        }

        if (p.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA)) {
            QuestTracker.complete(p, "quest.get_nausea");
        }

        if (p.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)) {
            QuestTracker.complete(p, "quest.get_levitation");
        }

        if (p.getActiveEffects().size() >= 3) {
            QuestTracker.complete(p, "quest.get_3_status_effects_at_once");
        }

        if (p.getActiveEffects().size() >= 6) {
            QuestTracker.complete(p, "quest.get_6_status_effects_at_once");
        }

        if (p.isSprinting()) {
            Vec3 last = lastSprintPos.get(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
            Vec3 now = p.position();
            if (last != null) {
                double dist = now.distanceTo(last);
                int add = (int) Math.round(dist * 100);
                QuestTracker.addProgress(p, "quest.sprint_1km", add, SPRINT_DISTANCE_TARGET_CM);
            }
            lastSprintPos.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), now);
        } else {
            lastSprintPos.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }

        if (p.isCrouching()) {
            updateDistanceProgress(p, lastCrouchPos, crouchDistanceCm, "quest.crouch_for_100m", CROUCH_DISTANCE_TARGET_CM);
        } else {
            lastCrouchPos.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }

        if (p.isSwimming()) {
            updateDistanceProgress(p, lastSwimPos, swimDistanceCm, "quest.swim_for_500m", SWIM_DISTANCE_TARGET_CM);
        } else {
            lastSwimPos.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }

        if (p.getVehicle() instanceof Boat) {
            updateDistanceProgress(p, lastBoatPos, boatDistanceCm, "quest.boat_for_2km", BOAT_DISTANCE_TARGET_CM);
        } else {
            lastBoatPos.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }

        if (isRidingHappyGhast(p)) {
            updateDistanceProgress(p, lastHappyGhastPos, happyGhastDistanceCm,
                    "quest.ride_a_happy_ghast_for_200_meters", 20000);
        } else {
            lastHappyGhastPos.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }

        if (p.getVehicle() instanceof AbstractMinecart) {
            updateDistanceProgress(p, lastMinecart25Pos, minecart25DistanceCm,
                    "quest.ride_a_minecart_for_25_meters", 2500);
        } else {
            lastMinecart25Pos.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }

        if (p.onClimbable()) {
            updateLadderClimbProgress(p);
        } else {
            lastLadderPos.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }

        ResourceKey<Level> currentDim = baseLevel.dimension();
        ResourceKey<Level> lastDim = lastDimensions.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), currentDim);
        if (lastDim != null && !lastDim.equals(currentDim)) {
            if (currentDim == Level.NETHER) {
                QuestTracker.complete(p, "quest.enter_nether");
            }
            if (currentDim == Level.END) {
                QuestTracker.complete(p, "quest.enter_end");
            }
        }

        if (p.getItemBySlot(EquipmentSlot.HEAD).is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:carved_pumpkin"))) {
            QuestTracker.addProgress(p, "quest.wear_a_carved_pumpkin_for_5_minutes", 1, PUMPKIN_WEAR_TICKS);
        } else {
            QuestTracker.setProgress(p, "quest.wear_a_carved_pumpkin_for_5_minutes", 0);
        }

        checkBiomeQuests(p, level);

        if (com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(p) % INVENTORY_SCAN_INTERVAL == 0) {
            checkInventoryQuests(p);
            checkInventoryObtainTriggers(p);
        }

        if (com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(p) % ADVANCEMENT_SCAN_INTERVAL == 0) {
            checkAdvancementQuests(p);
        }

        if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(p.getItemBySlot(EquipmentSlot.HEAD))
                || !com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(p.getItemBySlot(EquipmentSlot.CHEST))
                || !com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(p.getItemBySlot(EquipmentSlot.LEGS))
                || !com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(p.getItemBySlot(EquipmentSlot.FEET))) {
            triggerOpponentQuest(p, "quest.opponent_wears_armor");
            MineModeManager.onPlayerTrigger(
                    com.jamie.jamiebingo.util.EntityServerUtil.getServer(p),
                    p,
                    MineModeManager.Trigger.WEAR_ARMOR
            );
        }

        BlockPos below = p.blockPosition().below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.is(Blocks.NETHERRACK)) {
            triggerOpponentQuest(p, "quest.opponent_stands_on_netherrack");
            MineModeManager.onPlayerTrigger(
                    com.jamie.jamiebingo.util.EntityServerUtil.getServer(p),
                    p,
                    MineModeManager.Trigger.STAND_ON_NETHERRACK
            );
        }
        if (belowState.is(Blocks.STONE)) {
            triggerOpponentQuest(p, "quest.opponent_stands_on_stone");
            MineModeManager.onPlayerTrigger(
                    com.jamie.jamiebingo.util.EntityServerUtil.getServer(p),
                    p,
                    MineModeManager.Trigger.STAND_ON_STONE
            );
        }

        if (belowState.is(Blocks.COPPER_BLOCK)) {
            QuestTracker.complete(p, "quest.stand_on_a_block_of_copper");
        }
        if (belowState.is(Blocks.IRON_BLOCK)) {
            QuestTracker.complete(p, "quest.stand_on_a_block_of_iron");
        }
        if (belowState.is(Blocks.LAPIS_BLOCK)) {
            QuestTracker.complete(p, "quest.stand_on_a_block_of_lapis_lazuli");
        }
        if (belowState.is(Blocks.GOLD_BLOCK)) {
            QuestTracker.complete(p, "quest.stand_on_a_block_of_gold");
        }
        if (belowState.is(Blocks.REDSTONE_BLOCK)) {
            QuestTracker.complete(p, "quest.stand_on_a_block_of_redstone");
        }
        if (belowState.is(Blocks.DIAMOND_BLOCK)) {
            QuestTracker.complete(p, "quest.stand_on_a_block_of_of_diamond");
        }
        if (belowState.is(Blocks.NETHERITE_BLOCK)) {
            QuestTracker.complete(p, "quest.stand_on_a_block_of_netherite");
        }
        if (belowState.is(Blocks.COAL_BLOCK)) {
            QuestTracker.complete(p, "quest.stand_on_a_block_of_coal");
        }
        if (belowState.is(Blocks.EMERALD_BLOCK)) {
            QuestTracker.complete(p, "quest.stand_on_a_block_of_emerald");
        }

        if (p.isCrouching() && belowState.is(Blocks.SOUL_SAND)) {
            QuestTracker.complete(p, "quest.crouch_on_soul_sand");
        }

        if (isInCobweb(level, p.blockPosition())) {
            QuestTracker.complete(p, "quest.get_stuck_in_a_cobweb");
        }

        if (isOnBoatOnIce(level, p)) {
            QuestTracker.complete(p, "quest.boat_on_ice");
        }

        if (isSculkSensorPoweredNearby(level, p.blockPosition())) {
            QuestTracker.complete(p, "quest.trigger_a_sculk_sensor");
        }

        if (isCrawling(p)) {
            QuestTracker.complete(p, "quest.enter_a_crawl_state");
        }

        checkLiveTamedMobQuest(p, level);

        if (p.isOnFire()) {
            triggerOpponentQuest(p, "quest.an_opponent_catches_on_fire");
            MineModeManager.onPlayerTrigger(
                    com.jamie.jamiebingo.util.EntityServerUtil.getServer(p),
                    p,
                    MineModeManager.Trigger.CATCH_FIRE
            );
        }

        if (p.isInWater() && !(p.getVehicle() instanceof net.minecraft.world.entity.vehicle.boat.Boat)) {
            triggerOpponentQuest(p, "quest.an_opponent_touches_water");
            MineModeManager.onPlayerTrigger(
                    com.jamie.jamiebingo.util.EntityServerUtil.getServer(p),
                    p,
                    MineModeManager.Trigger.TOUCH_WATER
            );
        }

        if (isNearVines(p)) {
            lastVineTick.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(p));
        }

        if (p.getVehicle() instanceof Pig) {
            ItemStack main = p.getMainHandItem();
            ItemStack off = p.getOffhandItem();
            if (main.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:carrot_on_a_stick")) || off.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:carrot_on_a_stick"))) {
                QuestTracker.complete(p, "quest.ride_pig_with_a_carrot_on_a_stick");
            }
            Entity pig = p.getVehicle();
            if (getSupportedHappyGhast(pig) != null) {
                QuestTracker.complete(p, "quest.ride_a_pig_whilst_being_on_top_of_a_happy_ghast");
            }
        }

        checkRecentHappyGhastBoatLeadQuest(p, level);

        if (p.getVehicle() instanceof AbstractHorse horse) {
            if (horse.isSaddled()) {
                QuestTracker.complete(p, "quest.ride_horse_with_a_saddle");
            }
        }

        checkSpyglassLook(p);
        checkAnvilUsage(p);
        checkLoomUsage(p);
        checkSmithingUsage(p);
        checkGrindstoneUsage(p);
        checkStonecutterUsage(p);
        checkCrafterUsage(p);
        checkVillagerTradeStat(p);

        if (com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(p) % 40 == 0) {
            checkRaidStart(p, level);
            checkStructureQuests(p, level);
        }
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (!(event.getEntityMounting() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;
        if (!event.isMounting()) return;

        Entity mount = event.getEntityBeingMounted();
        if (mount instanceof AbstractMinecart) {
            QuestTracker.complete(p, "quest.ride_minecart");
        }

        if (mount instanceof AbstractHorse horse) {
            if (horse.isSaddled()) {
                QuestTracker.complete(p, "quest.ride_horse_with_a_saddle");
            }
        }

        if (mount instanceof Pig) {
            ItemStack main = p.getMainHandItem();
            ItemStack off = p.getOffhandItem();
            if (main.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:carrot_on_a_stick")) || off.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:carrot_on_a_stick"))) {
                QuestTracker.complete(p, "quest.ride_pig_with_a_carrot_on_a_stick");
            }
            if (getSupportedHappyGhast(mount) != null) {
                QuestTracker.complete(p, "quest.ride_a_pig_whilst_being_on_top_of_a_happy_ghast");
            }
        }

        if (mount instanceof Llama) {
            if (isTamedEntity(mount)) {
                QuestTracker.complete(p, "quest.tame_llama");
            } else {
                net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p);
                if (baseLevel instanceof ServerLevel serverLevel) {
                    int tick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(
                            com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
                    pendingTameChecks.add(new PendingTameCheck(
                            serverLevel,
                            com.jamie.jamiebingo.util.EntityUtil.getUUID(mount),
                            com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                            tick + 1,
                            tick + TAME_CHECK_TIMEOUT
                    ));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEndermanAggro(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof EnderMan enderman)) return;
        if (!(event.getNewTarget() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;

        if (isEndermanStaredAt(enderman, p)) {
            QuestTracker.complete(p, "quest.look_at_an_endarman_in_the_eyes_to_enrage_them");
        }
    }

    @SubscribeEvent
    public static void onTame(net.minecraftforge.event.entity.living.AnimalTameEvent event) {
        if (!(event.getTamer() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;

        Animal animal = event.getAnimal();
        if (animal instanceof Wolf) {
            QuestTracker.complete(p, "quest.tame_a_wolf");
            recordUniqueBreed(p, EntityType.WOLF);
            recordVariant(p, animal, Registries.WOLF_VARIANT, uniqueWolfVariants, "quest.tame_all_wolf_types");
        } else if (animal instanceof Cat) {
            QuestTracker.complete(p, "quest.tame_a_cat");
            recordUniqueBreed(p, EntityType.CAT);
            recordVariant(p, animal, Registries.CAT_VARIANT, uniqueCatVariants, "quest.tame_every_cat_variant");
        } else if (animal instanceof AbstractHorse) {
            QuestTracker.complete(p, "quest.tame_a_horse");
            recordUniqueBreed(p, EntityType.HORSE);
            if (animal instanceof net.minecraft.world.entity.animal.equine.Donkey) {
                QuestTracker.complete(p, "quest.tame_donkey");
            }
            if (animal instanceof net.minecraft.world.entity.animal.equine.Mule) {
                QuestTracker.complete(p, "quest.tame_mule");
            }
        } else if (animal instanceof net.minecraft.world.entity.animal.parrot.Parrot) {
            QuestTracker.complete(p, "quest.tame_a_parrot");
            recordUniqueBreed(p, EntityType.PARROT);
        } else if (animal instanceof Llama) {
            QuestTracker.complete(p, "quest.tame_llama");
        } else {
            Identifier tameId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(animal.getType());
            if (tameId != null && "minecraft:nautilus".equals(tameId.toString())) {
                QuestTracker.complete(p, "quest.tame_a_nautilus");
            }
        }
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (!(event.getCausedByPlayer() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(p);
        if (server != null) {
            lastBreedTick.put(
                    com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                    com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server)
            );
        }

        EntityType<?> type = event.getChild().getType();
        Identifier id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(type);
        String questId = id != null ? BREED_QUESTS.get(id) : null;
        if (questId != null) {
            QuestTracker.complete(p, questId);
        }

        recordUniqueBreed(p, type);

        Entity child = event.getChild();
        if (child instanceof Chicken) {
            recordVariant(p, child, Registries.CHICKEN_VARIANT, uniqueChickenVariants, "quest.breed_all_chicken_variants");
        } else if (child instanceof Cow) {
            recordVariant(p, child, Registries.COW_VARIANT, uniqueCowVariants, "quest.breed_all_cow_variants");
        } else if (child instanceof Pig) {
            recordVariant(p, child, Registries.PIG_VARIANT, uniquePigVariants, "quest.breed_all_pig_variants");
        }

        try {
            Method pa = event.getClass().getMethod("getParentA");
            Method pb = event.getClass().getMethod("getParentB");
            Object a = pa.invoke(event);
            Object b = pb.invoke(event);
            if ((a instanceof net.minecraft.world.entity.animal.frog.Frog) || (b instanceof net.minecraft.world.entity.animal.frog.Frog)) {
                QuestTracker.complete(p, "quest.breed_frogs");
            }
            if ((a instanceof net.minecraft.world.entity.animal.turtle.Turtle) || (b instanceof net.minecraft.world.entity.animal.turtle.Turtle)) {
                QuestTracker.complete(p, "quest.breed_turtle");
            }
        } catch (Throwable ignored) {
        }
    }


    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Entity entity = event.getEntity();
        Identifier id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id != null) {
            Player nearest = level.getNearestPlayer(entity, 16.0D);
            if (nearest instanceof ServerPlayer sp && isQuestActive(sp)) {
                if ("minecraft:copper_golem".equals(id.toString())) {
                    QuestTracker.complete(sp, "quest.construct_a_copper_golem");
                }
                if (GOLEM_TYPES.contains(id)) {
                    recordGolemBuild(sp, id);
                    if ("minecraft:iron_golem".equals(id.toString())) {
                        QuestTracker.complete(sp, "quest.summon_iron_golem");
                    }
                }
                // Hydrate dried ghast quest is now advancement-driven.
            }
        }
        if (entity instanceof FireworkRocketEntity rocket) {
            Entity owner = rocket.getOwner();
            if (owner instanceof ServerPlayer sp && isQuestActive(sp) && isCrossbowRocket(rocket)) {
                QuestTracker.complete(sp, "quest.shoot_a_firework_from_a_crossbow");
            }
        }
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity itemEntity) {
            ItemStack dropped = itemEntity.getItem();
            ServerPlayer owner = null;
            if (itemEntity.getOwner() instanceof ServerPlayer sp) {
                owner = sp;
            } else if (itemEntity.getOwner() != null) {
                owner = getOwnerPlayer(itemEntity.getOwner(), level.getServer());
            }
            if (owner != null && isQuestActive(owner) && !com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(dropped)) {
                if (dropped.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_pickaxe"))) {
                    if (itemEntity.isInLava()) {
                        QuestTracker.complete(owner, "quest.throw_a_diamond_pickaxe_into_lava");
                    }
                    pendingItemLavaChecks.add(new PendingItemLavaCheck(
                            level,
                            com.jamie.jamiebingo.util.EntityUtil.getUUID(itemEntity),
                            com.jamie.jamiebingo.util.EntityUtil.getUUID(owner),
                            "quest.throw_a_diamond_pickaxe_into_lava",
                            com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer()) + 200
                    ));
                }
                if (dropped.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:axolotl_bucket"))) {
                    if (itemEntity.isInLava()) {
                        QuestTracker.complete(owner, "quest.throw_a_bucket_of_axolotl_into_lava");
                    }
                    pendingItemLavaChecks.add(new PendingItemLavaCheck(
                            level,
                            com.jamie.jamiebingo.util.EntityUtil.getUUID(itemEntity),
                            com.jamie.jamiebingo.util.EntityUtil.getUUID(owner),
                            "quest.throw_a_bucket_of_axolotl_into_lava",
                            com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer()) + 200
                    ));
                }
            }

            BlockPos pos = itemEntity.blockPosition();
            BlockPos crafterPos = findNearbyCrafterPos(level, pos);
            if (crafterPos != null) {
                PendingCrafterUse match = findPendingCrafterUse(level, crafterPos);
                if (match != null) {
                    ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(
                            level.getServer(), match.playerId);
                    if (p != null && isQuestActive(p)) {
                        QuestTracker.complete(p, "quest.use_a_crafter");
                    }
                    pendingCrafterUses.remove(match);
                } else {
                    Player nearest = level.getNearestPlayer(
                            crafterPos.getX() + 0.5D, crafterPos.getY() + 0.5D, crafterPos.getZ() + 0.5D, 16.0D, false);
                    if (nearest instanceof ServerPlayer p && isQuestActive(p)) {
                        QuestTracker.complete(p, "quest.use_a_crafter");
                    }
                }
            }
        }
        if (entity instanceof ExperienceOrb orb) {
            handleBreedingXp(level, orb);
            handleGrindstoneXp(level, orb);
        }
    }

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;

        ItemStack stack = event.getItem();
        Item item = com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack);

        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:apple")) {
            QuestTracker.complete(p, "quest.eat_apple");
        }
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:beetroot")) {
            QuestTracker.complete(p, "quest.eat_beetroot");
        }
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:beetroot_soup")
                || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:beetroot_stew")) {
            QuestTracker.complete(p, "quest.eat_beetroot_soup");
            recordUniqueBowlFood(p, item);
        }
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:mushroom_stew")
                || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:rabbit_stew")
                || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:suspicious_stew")
                || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:beetroot_soup")
                || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:beetroot_stew")) {
            recordUniqueBowlFood(p, item);
        }
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:carrot")) {
            QuestTracker.complete(p, "quest.eat_carrot");
        }
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:dried_kelp")) {
            QuestTracker.complete(p, "quest.eat_dried_kelp");
        }
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:enchanted_golden_apple")) {
            QuestTracker.complete(p, "quest.eat_enchanted_golden_apple");
        }
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:spider_eye")) {
            QuestTracker.complete(p, "quest.eat_spider_eye");
        }
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:poisonous_potato")) {
            QuestTracker.complete(p, "quest.eat_poisonous_potato");
        }
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:suspicious_stew")) {
            QuestTracker.complete(p, "quest.eat_suspicious_stew");
        }
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:glow_berries")) {
            QuestTracker.complete(p, "quest.eat_a_glow_berry");
        }
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:cookie")) {
            QuestTracker.complete(p, "quest.eat_cookie");
        }
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:pumpkin_pie")) {
            QuestTracker.complete(p, "quest.eat_pumpkin_pie");
        }
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:rabbit_stew")) {
            QuestTracker.complete(p, "quest.eat_rabbit_stew");
        }
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:chorus_fruit")) {
            QuestTracker.complete(p, "quest.eat_chorus_fruit");
        }

        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:milk_bucket") && milkHadEffects.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p))) {
            QuestTracker.complete(p, "quest.remove_a_status_effect_with_a_milk_bucket");
        }

        if (com.jamie.jamiebingo.util.ItemStackComponentUtil.has(stack, DataComponents.FOOD)) {
            recordUniqueFood(p, stack);
        }

        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:potion") || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:splash_potion") || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:lingering_potion")) {
            Holder<Potion> potion = getPotionHolder(stack);
            if (potion != null && potion.is(Potions.WATER_BREATHING)) {
                QuestTracker.complete(p, "quest.obtain_a_potion_of_water_breathing");
            }
            if (potion != null && potion.is(Potions.HEALING)) {
                QuestTracker.complete(p, "quest.obtain_a_potion_of_healing");
            }
            if (potion != null && potion.is(Potions.INVISIBILITY)) {
                QuestTracker.complete(p, "quest.obtain_a_potion_of_invisibility");
            }
            if (potion != null && potion.is(Potions.SWIFTNESS)) {
                QuestTracker.complete(p, "quest.obtain_a_potion_of_swiftness");
            }
            if (potion != null && potion.is(Potions.LEAPING)) {
                QuestTracker.complete(p, "quest.obtain_a_potion_of_leaping");
            }
        }

        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:lingering_potion")) {
            QuestTracker.complete(p, "quest.obtain_a_lingering_potion");
        }

        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:ender_pearl")) {
            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(p);
            if (server != null) {
                lastEnderPearlTick.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                        com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server));
            }
        }
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;

        if (event.getItem().is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:goat_horn"))) {
            QuestTracker.complete(p, "quest.toot_a_goat_horn");
        }

        if (event.getItem().is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:milk_bucket")) && !p.getActiveEffects().isEmpty()) {
            milkHadEffects.add(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;

        net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p);
        if (!(baseLevel instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        ItemStack stack = event.getItemStack();

        if (state.is(Blocks.CAKE)) {
            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(p);
            if (server != null) {
                lastCakeEatTick.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                        com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server));
            }
        }

        if (state.is(Blocks.COMPOSTER)) {
            pendingComposterChecks.add(new PendingBlockCheck(level, pos, com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                    com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer()) + 1));
        }

        if (state.is(Blocks.BREWING_STAND)) {
            // handled via advancement
        }

        if (state.is(Blocks.LOOM)) {
            // handled via loom output tracking
        }

        if (state.is(Blocks.ENCHANTING_TABLE)) {
            // handled via enchantment advancement
        }

        if (state.is(Blocks.ANVIL) || state.is(Blocks.CHIPPED_ANVIL) || state.is(Blocks.DAMAGED_ANVIL)) {
            // handled via anvil output tracking
        }

        if (state.is(Blocks.JUKEBOX) && com.jamie.jamiebingo.util.ItemStackComponentUtil.get(stack, DataComponents.JUKEBOX_PLAYABLE) != null) {
            QuestTracker.complete(p, "quest.use_a_jukebox_to_play_a_music_disc");
        }

        if (state.is(Blocks.BELL)) {
            QuestTracker.complete(p, "quest.ring_a_bell");
        }

        if (state.is(Blocks.FLETCHING_TABLE)) {
            QuestTracker.complete(p, "quest.right_click_a_fletching_table");
        }
        if (state.is(Blocks.LECTERN)
                && stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:writable_book"))) {
            QuestTracker.complete(p, "quest.place_a_book_and_quil_into_a_lectern");
        }

        if (state.is(Blocks.NOTE_BLOCK) && isJukeboxPlayingNearby(level, pos)) {
            QuestTracker.complete(p, "quest.play_sound_from_a_note_block_whilst_playing_music_on_a_jukebox");
        }

        if (state.is(BlockTags.BUTTONS)) {
            boolean wasPowered = state.hasProperty(BlockStateProperties.POWERED)
                    && state.getValue(BlockStateProperties.POWERED);
            pendingButtonPressChecks.add(new PendingButtonPressCheck(
                    level,
                    pos,
                    com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                    wasPowered,
                    com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer()) + 1
            ));
        }

        if (state.is(Blocks.LEVER)) {
            QuestTracker.increment(p, "quest.flick_a_lever_20_times", 20);
        }

        if ((state.is(Blocks.CRIMSON_SIGN) || state.is(Blocks.CRIMSON_WALL_SIGN))
                && stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:glow_ink_sac"))) {
            QuestTracker.complete(p, "quest.use_glow_ink_on_a_crimson_sign");
        }

        if (state.is(Blocks.WATER_CAULDRON)) {
            if (stack.is(ItemTags.DYEABLE) || com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack) instanceof BannerItem) {
                QuestTracker.complete(p, "quest.use_a_cauldron_to_wash_something");
            }
        }

        if (state.is(Blocks.RESPAWN_ANCHOR)) {
            pendingAnchorChecks.add(new PendingBlockCheck(level, pos, com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                    com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer()) + 1));
        }

        if (state.is(BlockTags.CANDLES)) {
            if (stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:flint_and_steel"))
                    || stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:fire_charge"))) {
                QuestTracker.complete(p, "quest.light_a_candle");
            }
        }

        if (state.is(BlockTags.CAMPFIRES) && stack.is(ItemTags.SHOVELS)) {
            if (!state.hasProperty(BlockStateProperties.LIT) || state.getValue(BlockStateProperties.LIT)) {
                QuestTracker.complete(p, "quest.extinguish_a_campfire_with_a_shovel");
            }
        }

        if (isWaxedCopper(state) && stack.is(ItemTags.AXES)) {
            QuestTracker.complete(p, "quest.scrape_wax_off_a_copper_block");
        }

        if (isCopperBlock(state) && !isWaxedCopper(state)
                && stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:honeycomb"))) {
            QuestTracker.complete(p, "quest.wax_a_copper_block");
        }

        if ((state.is(Blocks.SUSPICIOUS_SAND) || state.is(Blocks.SUSPICIOUS_GRAVEL))
                && stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:brush"))) {
            QuestTracker.complete(p, "quest.use_a_brush_on_suspicious_sand_gravel");
        }

        // Crafter/Grindstone/Stonecutter completion is tracked when output is actually taken.

        if (state.is(Blocks.FLOWER_POT) && stack.is(ItemTags.FLOWERS)) {
            QuestTracker.complete(p, "quest.put_a_flower_in_a_pot");
        }

        if (state.is(BlockTags.BANNERS) && stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:filled_map"))) {
            QuestTracker.complete(p, "quest.right_click_a_banner_with_a_map");
        }

        if (stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:painting"))) {
            QuestTracker.complete(p, "quest.place_a_painting");
        }

        if (stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:end_crystal"))) {
            pendingEndCrystalChecks.add(new PendingEndCrystalCheck(level, pos, com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                    com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer()) + 1));
        }

        if (state.is(Blocks.VAULT)) {
            if (stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:trial_key"))
                    || stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:ominous_trial_key"))) {
            pendingVaultKeyChecks.add(new PendingVaultKeyCheck(
                    level,
                    pos,
                    com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                    com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack),
                    countItem(p, com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack)),
                    stack.getCount(),
                    event.getHand(),
                    com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer()) + 1
            ));
        }
        }

        if (state.is(Blocks.CRAFTER)) {
            pendingCrafterUses.add(new PendingCrafterUse(
                    level,
                    pos,
                    com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                    com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer()) + 80
            ));
        }

        int nowTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer());
        if (state.is(Blocks.CRAFTING_TABLE)) {
            pendingCraftingTableInsertChecks.add(new PendingCraftingTableInsertCheck(
                    com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                    com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:crafting_table"),
                    "quest.put_a_crafting_table_in_a_crafting_table",
                    nowTick + 200
            ));
        }
        if (state.is(Blocks.FURNACE)) {
            pendingContainerInsertChecks.add(new PendingContainerInsertCheck(
                    level,
                    pos,
                    com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                    com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:furnace"),
                    "quest.put_a_furnace_in_a_furnace",
                    nowTick + 200
            ));
        }
        if (state.is(Blocks.BLAST_FURNACE)) {
            pendingContainerInsertChecks.add(new PendingContainerInsertCheck(
                    level,
                    pos,
                    com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                    com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:blast_furnace"),
                    "quest.put_a_blast_furnace_in_a_blast_furnace",
                    nowTick + 200
            ));
        }
        if (state.is(Blocks.SMOKER)) {
            pendingContainerInsertChecks.add(new PendingContainerInsertCheck(
                    level,
                    pos,
                    com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                    com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:smoker"),
                    "quest.put_a_smoker_in_a_smoker",
                    nowTick + 200
            ));
        }
        if (state.is(Blocks.CHEST)) {
            pendingContainerInsertChecks.add(new PendingContainerInsertCheck(
                    level,
                    pos,
                    com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                    com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:chest"),
                    "quest.put_a_chest_in_a_chest",
                    nowTick + 200
            ));
        }
        if (state.is(Blocks.DECORATED_POT) && stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:decorated_pot"))) {
            QuestTracker.complete(p, "quest.put_a_decorated_pot_in_a_decorated_pot");
        }

        checkContainerFillQuests(p, level, pos, state);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;

        ItemStack stack = event.getItemStack();
        if (stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:ender_pearl"))) {
            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(p);
            if (server != null) {
                lastEnderPearlTick.put(
                        com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                        com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server)
                );
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;

        Entity target = event.getTarget();
        ItemStack stack = event.getItemStack();

        if (target instanceof Piglin && stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:gold_ingot"))) {
            QuestTracker.complete(p, "quest.distract_a_piglin_with_gold");
        }

        if (isLeadItem(stack)) {
            handleLeadQuests(p, target);
            if (isHappyGhastEntity(target) || target instanceof Boat) {
                net.minecraft.world.level.Level baseLevel2 = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p);
                if (baseLevel2 instanceof ServerLevel serverLevel2) {
                    if (isHappyGhastBoatLeadPairNearby(serverLevel2, p, target)) {
                        QuestTracker.complete(p, "quest.attach_a_boat_to_a_happy_ghast_using_a_lead");
                    } else {
                        recentHappyGhastLeadUseTick.put(
                                com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                                com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(serverLevel2.getServer())
                        );
                    }
                }
            }
        }

        if (target instanceof AbstractHorse horse) {
            if (isDyedLeatherHorseArmor(horse)) {
                QuestTracker.complete(p, "quest.equip_a_horse_with_coloured_horse_armor");
            }
        }

        if (target instanceof Wolf wolf) {
            if (com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack) instanceof net.minecraft.world.item.DyeItem
                    && isTamedEntity(wolf)) {
                QuestTracker.complete(p, "quest.dye_the_collar_of_a_tamed_wolf");
            }
            if (stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:name_tag"))
                    && com.jamie.jamiebingo.util.ItemStackComponentUtil.has(stack, DataComponents.CUSTOM_NAME)
                    && isTamedEntity(wolf)) {
                QuestTracker.complete(p, "quest.give_a_name_to_a_tamed_wolf");
            }
            if (hasWolfArmor(wolf)) {
                QuestTracker.complete(p, "quest.equip_wolf_armor_on_a_wolf");
            }
            if (stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wolf_armor"))) {
                net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p);
                if (baseLevel instanceof ServerLevel serverLevel) {
                    pendingWolfArmorChecks.add(new PendingWolfArmorCheck(
                            serverLevel,
                            com.jamie.jamiebingo.util.EntityUtil.getUUID(wolf),
                            com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                            com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p)) + 1
                    ));
                }
            }
            if (stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:armadillo_scute")) && hasWolfArmor(wolf)) {
                QuestTracker.complete(p, "quest.repair_wolf_armor");
            }
        }

        if (target instanceof Cat cat
                && stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:name_tag"))
                && com.jamie.jamiebingo.util.ItemStackComponentUtil.has(stack, DataComponents.CUSTOM_NAME)
                && isTamedEntity(cat)) {
            QuestTracker.complete(p, "quest.give_a_name_to_a_tamed_cat");
        }

        if (target instanceof net.minecraft.world.entity.animal.golem.SnowGolem
                && stack.getItem() instanceof ShearsItem) {
            QuestTracker.complete(p, "quest.shear_a_snow_golem");
        }

        if (target.getType() == EntityType.ALLAY
                && stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_sword"))) {
            QuestTracker.complete(p, "quest.give_a_diamond_sword_to_an_allay");
        }

        // Hydrate dried ghast quest is now advancement-driven.

        Identifier targetId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (targetId != null && "nautilus".equals(targetId.getPath())) {
            net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p);
            if (baseLevel instanceof ServerLevel serverLevel) {
                int tick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
                pendingTameChecks.add(new PendingTameCheck(
                        serverLevel,
                        com.jamie.jamiebingo.util.EntityUtil.getUUID(target),
                        com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                        tick + 1,
                        tick + TAME_CHECK_TIMEOUT
                ));
            }
        }

        if (target instanceof ArmorStand stand) {
            if (armorStandHasFullArmor(stand)) {
                QuestTracker.complete(p, "quest.fill_an_armor_stand_with_armor");
            } else {
                net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p);
                if (baseLevel instanceof ServerLevel serverLevel) {
                    pendingArmorStandChecks.add(new PendingArmorStandCheck(
                            serverLevel,
                            com.jamie.jamiebingo.util.EntityUtil.getUUID(stand),
                            com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                            com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p)) + 1
                    ));
                }
            }
        }

        if (target instanceof Villager) {
            // handled via trade stat tracking
        }

        if (target instanceof Llama) {
            net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p);
            if (baseLevel instanceof ServerLevel serverLevel) {
                int tick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
                pendingTameChecks.add(new PendingTameCheck(
                        serverLevel,
                        com.jamie.jamiebingo.util.EntityUtil.getUUID(target),
                        com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                        tick + 1,
                        tick + TAME_CHECK_TIMEOUT
                ));
            }
        }

        boolean turtleFeed = target instanceof net.minecraft.world.entity.animal.turtle.Turtle
                && stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:seagrass"));
        boolean frogFeed = target instanceof net.minecraft.world.entity.animal.frog.Frog
                && stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:slime_ball"));
        if (turtleFeed || frogFeed) {
            net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p);
            if (baseLevel instanceof ServerLevel serverLevel) {
                int tick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
                pendingLoveChecks.add(new PendingLoveCheck(
                        serverLevel,
                        com.jamie.jamiebingo.util.EntityUtil.getUUID(target),
                        com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                        tick + 1,
                        tick + LOVE_CHECK_TIMEOUT
                ));
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;

        Entity target = event.getTarget();
        if (target instanceof ArmorStand stand) {
            if (armorStandHasFullArmor(stand)) {
                QuestTracker.complete(p, "quest.fill_an_armor_stand_with_armor");
            } else {
                net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p);
                if (baseLevel instanceof ServerLevel serverLevel) {
                    pendingArmorStandChecks.add(new PendingArmorStandCheck(
                            serverLevel,
                            com.jamie.jamiebingo.util.EntityUtil.getUUID(stand),
                            com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                            com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p)) + 1
                    ));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;

        ItemStack stack = event.getItem().getItem();
        Item item = com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack);

        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:obsidian")) {
            triggerOpponentQuest(p, "quest.an_opponent_obtains_obsidian");
            MineModeManager.onPlayerTrigger(
                    com.jamie.jamiebingo.util.EntityServerUtil.getServer(p),
                    p,
                    MineModeManager.Trigger.OBTAIN_OBSIDIAN
            );
        }

        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wheat_seeds")) {
            triggerOpponentQuest(p, "quest.an_opponent_obtains_wheat_seeds");
            MineModeManager.onPlayerTrigger(
                    com.jamie.jamiebingo.util.EntityServerUtil.getServer(p),
                    p,
                    MineModeManager.Trigger.OBTAIN_WHEAT_SEEDS
            );
        }

        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:crafting_table")) {
            triggerOpponentQuest(p, "quest.an_opponent_obtains_a_crafting_table");
            MineModeManager.onPlayerTrigger(
                    com.jamie.jamiebingo.util.EntityServerUtil.getServer(p),
                    p,
                    MineModeManager.Trigger.OBTAIN_CRAFTING_TABLE
            );
        }

        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:red_tulip")) {
            Entity ownerEntity = event.getItem().getOwner();
            if (ownerEntity instanceof ServerPlayer owner
                    && !com.jamie.jamiebingo.util.EntityUtil.getUUID(owner).equals(com.jamie.jamiebingo.util.EntityUtil.getUUID(p))) {
                UUID ownerTeam = getTeamId(owner);
                UUID pickerTeam = getTeamId(p);
                if (ownerTeam != null && ownerTeam.equals(pickerTeam)) {
                    QuestTracker.complete(owner, "quest.give_a_red_tulip_to_a_teamate");
                }
            }
        }

        recordArmorTrimTemplate(p, stack);
        checkInventoryQuests(p);
        checkInventoryObtainTriggers(p);
    }

    @SubscribeEvent
    public static void onItemToss(net.minecraftforge.event.entity.item.ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;
        net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p);
        if (!(baseLevel instanceof ServerLevel level)) return;
        net.minecraft.world.entity.item.ItemEntity itemEntity = event.getEntity();
        if (itemEntity == null) return;
        ItemStack stack = itemEntity.getItem();
        if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) return;
        int expire = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer()) + 200;
        if (stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_pickaxe"))) {
            pendingItemLavaChecks.add(new PendingItemLavaCheck(
                    level,
                    com.jamie.jamiebingo.util.EntityUtil.getUUID(itemEntity),
                    com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                    "quest.throw_a_diamond_pickaxe_into_lava",
                    expire
            ));
        }
        if (stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:axolotl_bucket"))) {
            pendingItemLavaChecks.add(new PendingItemLavaCheck(
                    level,
                    com.jamie.jamiebingo.util.EntityUtil.getUUID(itemEntity),
                    com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                    "quest.throw_a_bucket_of_axolotl_into_lava",
                    expire
            ));
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;

        ItemStack crafted = event.getCrafting();
        Item item = crafted.getItem();
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:crafting_table")) {
            triggerOpponentQuest(p, "quest.an_opponent_obtains_a_crafting_table");
            MineModeManager.onPlayerTrigger(
                    com.jamie.jamiebingo.util.EntityServerUtil.getServer(p),
                    p,
                    MineModeManager.Trigger.OBTAIN_CRAFTING_TABLE
            );
        }
        if (isNetheriteEquipment(item)) {
            QuestTracker.complete(p, "quest.use_a_smithing_table");
        }
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:shield") && hasBannerPatterns(crafted)) {
            QuestTracker.complete(p, "quest.decorate_a_shield_with_a_banner");
        }
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:decorated_pot")
                && craftedWithPotterySherd(event)) {
            QuestTracker.complete(p, "quest.decorate_a_pot_with_a_pottery_sherd");
        }
        if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:crafter")
                && p.containerMenu instanceof CrafterMenu) {
            QuestTracker.complete(p, "quest.use_a_crafter_to_craft_a_crafter");
        }

        recordUniqueCraft(p, crafted);
        recordArmorTrimTemplate(p, crafted);
        checkInventoryQuests(p);
        checkInventoryObtainTriggers(p);
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Entity projectile = event.getProjectile();
        Entity owner = projectile instanceof net.minecraft.world.entity.projectile.Projectile proj
                ? proj.getOwner()
                : null;
        if (!(owner instanceof ServerPlayer shooter)) return;
        if (!isQuestActive(shooter)) return;

        Entity hit = event.getRayTraceResult() instanceof EntityHitResult ehr
                ? ehr.getEntity()
                : null;

        if (hit instanceof net.minecraft.world.entity.monster.Creeper
                && projectile instanceof net.minecraft.world.entity.projectile.FishingHook) {
            QuestTracker.complete(shooter, "quest.attach_a_fishing_rod_to_a_creeper");
        }

        if (hit instanceof Witch && isSplashPotionProjectile(projectile)) {
            if (projectileCarriesItem(projectile, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:splash_potion"))) {
                QuestTracker.complete(shooter, "quest.throw_a_splash_potion_at_a_witch");
            }
        }

        if (hit instanceof ServerPlayer target) {
            UUID shooterTeam = getTeamId(shooter);
            UUID targetTeam = getTeamId(target);
            if (shooterTeam != null && !shooterTeam.equals(targetTeam)) {
                if (projectile instanceof Snowball) {
                    QuestTracker.complete(shooter, "quest.hit_an_opponent_with_a_snowball");
                }

                Identifier projId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(projectile.getType());
                if (projId != null) {
                    String path = projId.getPath();
                    if (path.contains("wind_charge")) {
                        triggerOpponentQuest(shooter, "quest.opponent_is_hit_by_a_windcharge");
                    }
                }
            }
        }

        if (event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult ehr2
                && ehr2.getEntity() instanceof LivingEntity hitEntity) {
            MinecraftServer server = shooter.level() instanceof ServerLevel sl ? sl.getServer() : null;
            if (server == null) return;
            int tick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
            if (hitEntity instanceof net.minecraft.world.entity.monster.Blaze
                    && isSnowballDamage(null, projectile, projectile)) {
                pendingProjectileKills.put(
                        com.jamie.jamiebingo.util.EntityUtil.getUUID(hitEntity),
                        new PendingProjectileKill(com.jamie.jamiebingo.util.EntityUtil.getUUID(shooter), "snowball", tick + 100)
                );
            }
            if (hitEntity instanceof net.minecraft.world.entity.monster.breeze.Breeze
                    && isWindChargeDamage(null, projectile, projectile)) {
                pendingProjectileKills.put(
                        com.jamie.jamiebingo.util.EntityUtil.getUUID(hitEntity),
                        new PendingProjectileKill(com.jamie.jamiebingo.util.EntityUtil.getUUID(shooter), "wind_charge", tick + 100)
                );
            }
        }
    }

    @SubscribeEvent
    public static void onEggImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownEgg egg)) return;
        if (!(egg.getOwner() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;

        Vec3 hit = egg.position();
        net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p);
        if (!(baseLevel instanceof ServerLevel level)) return;
        pendingEggChecks.add(new PendingEggCheck(level, hit, com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer()) + 1));
    }

    @SubscribeEvent
    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;

        net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p);
        if (baseLevel instanceof ServerLevel level) {
            if (isOnMud(level, p)) {
                QuestTracker.complete(p, "quest.jump_on_mud");
            }
        }

        if (baseLevel instanceof ServerLevel level) {
            BlockPos feet = p.blockPosition();
            boolean waterLaunch = level.getFluidState(feet).is(FluidTags.WATER)
                    || level.getFluidState(feet.below()).is(FluidTags.WATER)
                    || p.isInWater();
            if (waterLaunch) {
                return;
            }
        }

        triggerOpponentQuest(p, "quest.an_opponent_jumps");
        MineModeManager.onPlayerTrigger(
                com.jamie.jamiebingo.util.EntityServerUtil.getServer(p),
                p,
                MineModeManager.Trigger.JUMP
        );
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(com.jamie.jamiebingo.util.DamageSourceUtil.getEntity(event.getSource()) instanceof ServerPlayer attacker)) return;
        if (!isQuestActive(attacker)) return;

        if (target instanceof ZombifiedPiglin) {
            QuestTracker.complete(attacker, "quest.enrage_a_zombie_piglin");
        }

        int amount = (int) Math.ceil(event.getAmount() * DAMAGE_SCALE);
        if (amount > 0) {
            QuestTracker.addProgress(attacker, "quest.deal_400_damage", amount, DEAL_DAMAGE_TARGET);
        }
    }

    @SubscribeEvent
    public static void onProjectileDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target instanceof net.minecraft.world.entity.monster.Blaze)
                && !(target instanceof net.minecraft.world.entity.monster.breeze.Breeze)) {
            return;
        }
        DamageSource source = event.getSource();
        Entity srcEntity = com.jamie.jamiebingo.util.DamageSourceUtil.getEntity(source);
        Entity direct = com.jamie.jamiebingo.util.DamageSourceUtil.getDirectEntity(source);
        MinecraftServer server = target.level() instanceof ServerLevel sl ? sl.getServer() : null;
        if (server == null) return;
        ServerPlayer owner = getOwnerPlayer(direct, server);
        if (owner == null) {
            owner = getOwnerPlayer(srcEntity, server);
        }
        if (owner == null || !isQuestActive(owner)) return;

        int tick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        if (target instanceof net.minecraft.world.entity.monster.Blaze
                && (isSnowballDamage(source, direct, srcEntity) || isSnowballProjectile(direct) || isSnowballProjectile(srcEntity))) {
            pendingProjectileKills.put(
                    com.jamie.jamiebingo.util.EntityUtil.getUUID(target),
                    new PendingProjectileKill(com.jamie.jamiebingo.util.EntityUtil.getUUID(owner), "snowball", tick + 100)
            );
        }
        if (target instanceof net.minecraft.world.entity.monster.breeze.Breeze
                && (isWindChargeDamage(source, direct, srcEntity) || isWindChargeProjectile(direct) || isWindChargeProjectile(srcEntity))) {
            pendingProjectileKills.put(
                    com.jamie.jamiebingo.util.EntityUtil.getUUID(target),
                    new PendingProjectileKill(com.jamie.jamiebingo.util.EntityUtil.getUUID(owner), "wind_charge", tick + 100)
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;

        int amount = (int) Math.ceil(event.getAmount() * DAMAGE_SCALE);
        if (amount > 0) {
            QuestTracker.addProgress(p, "quest.take_200_damage", amount, TAKE_DAMAGE_TARGET);

            UUID teamId = getTeamId(p);
            if (teamId != null) {
                int total = teamDamageTaken.getOrDefault(teamId, 0) + amount;
                teamDamageTaken.put(teamId, total);
                if (total >= OPPONENT_DAMAGE_TARGET) {
                    triggerOpponentQuest(p, "quest.an_opponent_takes_100_total_damage");
                }
            }
            MineModeManager.onPlayerTrigger(
                    com.jamie.jamiebingo.util.EntityServerUtil.getServer(p),
                    p,
                    MineModeManager.Trigger.TAKE_100_DAMAGE,
                    amount
            );
        }

        DamageSource source = event.getSource();
        if (source != null && "fall".equals(source.getMsgId())) {
            triggerOpponentQuest(p, "quest.an_opponent_takes_fall_damage");
            MineModeManager.onPlayerTrigger(
                    com.jamie.jamiebingo.util.EntityServerUtil.getServer(p),
                    p,
                    MineModeManager.Trigger.TAKE_FALL_DAMAGE
            );
        }

        Entity srcEntity = com.jamie.jamiebingo.util.DamageSourceUtil.getEntity(source);
        Entity directForOpponent = com.jamie.jamiebingo.util.DamageSourceUtil.getDirectEntity(source);
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(p);
        ServerPlayer attacker = getOwnerPlayer(directForOpponent, server);
        if (attacker == null) {
            attacker = getOwnerPlayer(srcEntity, server);
        }
        if (attacker != null) {
            UUID attackerTeam = getTeamId(attacker);
            UUID targetTeam = getTeamId(p);
            if (attackerTeam != null && targetTeam != null && !attackerTeam.equals(targetTeam)) {
                if (isWindChargeProjectile(directForOpponent) || isWindChargeProjectile(srcEntity)) {
                    triggerOpponentQuest(attacker, "quest.opponent_is_hit_by_a_windcharge");
                }
            }
        }
        recordDamageSource(p, source);

        Entity direct = com.jamie.jamiebingo.util.DamageSourceUtil.getDirectEntity(source);
        Entity killer = com.jamie.jamiebingo.util.DamageSourceUtil.getEntity(source);
        if (direct instanceof MinecartTNT || killer instanceof MinecartTNT
                || (direct != null && direct.getType() == EntityType.TNT_MINECART)
                || (killer != null && killer.getType() == EntityType.TNT_MINECART)) {
            lastTntMinecartTick.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(p));
        }
    }

    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;
        net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p);
        if (baseLevel == null) return;
        if (baseLevel.dimension() != Level.OVERWORLD) return;
        if (!isDay(baseLevel)) return;
        if (sleepAloneCandidate.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p)) != null) {
            QuestTracker.complete(p, "quest.sleep_alone_in_the_overworld");
        }
    }

    @SubscribeEvent
    public static void onItemBreak(PlayerDestroyItemEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;
        ItemStack stack = event.getOriginal();
        if (stack == null || stack.isEmpty()) return;
        Item item = com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack);
        if (isToolItem(item)) {
            QuestTracker.complete(p, "quest.break_any_tool");
        }
    }

    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;
        pendingShieldDisableChecks.add(new PendingShieldDisableCheck(
                com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(p) + 1
        ));
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;

        MinecraftServer deathServer = com.jamie.jamiebingo.util.EntityServerUtil.getServer(p);
        if (deathServer != null) {
            lastDeathTick.put(
                    com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                    com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(deathServer)
            );
        }

        triggerOpponentQuest(p, "quest.an_opponent_dies");
        MineModeManager.onPlayerTrigger(
                com.jamie.jamiebingo.util.EntityServerUtil.getServer(p),
                p,
                MineModeManager.Trigger.DIE
        );

        UUID teamId = getTeamId(p);
        if (teamId != null) {
            int deaths = teamDeathCounts.getOrDefault(teamId, 0) + 1;
            teamDeathCounts.put(teamId, deaths);
            if (deaths >= 3) {
                triggerOpponentQuest(p, "quest.an_opponent_dies_3_times");
            }
        }

        DamageSource source = event.getSource();
        Entity killer = com.jamie.jamiebingo.util.DamageSourceUtil.getEntity(source);
        Entity direct = com.jamie.jamiebingo.util.DamageSourceUtil.getDirectEntity(source);
        String msgId = source == null ? "" : source.getMsgId();

        if (killer instanceof Bee) {
            QuestTracker.complete(p, "quest.die_by_bee_sting");
        }

        if ("sweetBerryBush".equals(msgId)) {
            QuestTracker.complete(p, "quest.die_by_berry_bush");
        }

        if ("cactus".equals(msgId)) {
            QuestTracker.complete(p, "quest.die_by_cactus");
        }

        if ("fall".equals(msgId)) {
            Integer lastTick = lastVineTick.get(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
            if (lastTick != null && com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(p) - lastTick <= 100) {
                QuestTracker.complete(p, "quest.die_by_falling_off_vines");
            }
        }

        if ("explosion".equals(msgId) || "explosion.player".equals(msgId)
                || (source != null && source.is(DamageTypeTags.IS_EXPLOSION))) {
            if (direct instanceof PrimedTnt || killer instanceof PrimedTnt || direct == null) {
                QuestTracker.complete(p, "quest.die_to_tnt");
            }
        }

        if (killer instanceof Llama || direct instanceof net.minecraft.world.entity.projectile.LlamaSpit) {
            QuestTracker.complete(p, "quest.die_by_llama");
        }

        if ("badRespawnPoint".equals(msgId)) {
            QuestTracker.complete(p, "quest.die_to_intentional_game_design");
        }

        if (killer instanceof IronGolem) {
            QuestTracker.complete(p, "quest.die_to_iron_golem");
        }

        if (direct instanceof MinecartTNT || killer instanceof MinecartTNT
                || (direct != null && direct.getType() == EntityType.TNT_MINECART)
                || (killer != null && killer.getType() == EntityType.TNT_MINECART)) {
            QuestTracker.complete(p, "quest.die_to_tnt_minecart");
        }
        Integer lastTntTick = lastTntMinecartTick.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        if (lastTntTick != null && com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(p) - lastTntTick <= 100) {
            QuestTracker.complete(p, "quest.die_to_tnt_minecart");
        }

        if (direct instanceof FireworkRocketEntity || killer instanceof FireworkRocketEntity) {
            QuestTracker.complete(p, "quest.die_by_firework_rocket");
        }

        if ("fallingStalactite".equals(msgId) || "falling_stalactite".equals(msgId)) {
            QuestTracker.complete(p, "quest.die_to_falling_stalactite");
        }

        if ("anvil".equals(msgId)) {
            QuestTracker.complete(p, "quest.die_by_anvil");
        }

        if ("magic".equals(msgId) || "indirectMagic".equals(msgId)) {
            QuestTracker.complete(p, "quest.die_by_magic");
        }

        if (msgId.contains("drown")) {
            QuestTracker.complete(p, "quest.die_by_drowning");
        }
        if (msgId.contains("freeze")) {
            QuestTracker.complete(p, "quest.die_by_freezing");
        }
        if (msgId.contains("trident")) {
            QuestTracker.complete(p, "quest.die_to_trident");
        }

        if (direct != null) {
            Identifier directId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(direct.getType());
            if (directId != null && "trident".equals(directId.getPath())) {
                QuestTracker.complete(p, "quest.die_to_trident");
            }
        }

        Identifier killerId = null;
        if (killer != null) {
            killerId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(killer.getType());
        } else if (direct != null) {
            killerId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(direct.getType());
        }
        if (killerId != null) {
            String path = killerId.getPath();
            if ("pufferfish".equals(path)) {
                QuestTracker.complete(p, "quest.die_to_pufferfish");
            }
            if ("creaking".equals(path)) {
                QuestTracker.complete(p, "quest.die_to_creaking");
            }
            if ("piglin_brute".equals(path)) {
                QuestTracker.complete(p, "quest.die_to_piglin_brute");
            }
            if ("polar_bear".equals(path)) {
                QuestTracker.complete(p, "quest.die_to_polar_bear");
            }
            if ("warden".equals(path)) {
                QuestTracker.complete(p, "quest.die_to_warden");
            }
            if ("witch".equals(path)) {
                QuestTracker.complete(p, "quest.die_to_witch");
            }
            if ("trident".equals(path)) {
                QuestTracker.complete(p, "quest.die_to_trident");
            }
        }
    }
    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        Entity srcEntity = com.jamie.jamiebingo.util.DamageSourceUtil.getEntity(event.getSource());
        Entity directEntity = com.jamie.jamiebingo.util.DamageSourceUtil.getDirectEntity(event.getSource());
        LivingEntity target = event.getEntity();
        ServerPlayer killer = null;
        if (srcEntity instanceof ServerPlayer sp) {
            killer = sp;
        } else if (directEntity instanceof net.minecraft.world.entity.projectile.Projectile proj
                && proj.getOwner() instanceof ServerPlayer sp) {
            killer = sp;
        }
        if (killer == null) {
            MinecraftServer server = null;
            if (target != null && target.level() instanceof ServerLevel serverLevel) {
                server = serverLevel.getServer();
            }
            if (server != null) {
                killer = getOwnerPlayer(directEntity, server);
                if (killer == null) {
                    killer = getOwnerPlayer(srcEntity, server);
                }
            }
        }
        MinecraftServer deathServer = target.level() instanceof ServerLevel sl ? sl.getServer() : null;
        if (target instanceof net.minecraft.world.entity.monster.Blaze) {
            UUID pending = consumePendingProjectileKill(
                    target,
                    "snowball",
                    deathServer == null ? 0 : com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(deathServer)
            );
            if (pending != null && deathServer != null) {
                ServerPlayer owner = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(deathServer, pending);
                if (owner != null && isQuestActive(owner)) {
                    QuestTracker.complete(owner, "quest.kill_a_blaze_with_a_snowball");
                }
            }
            if (pending == null && isSnowballDamage(event.getSource(), directEntity, srcEntity)) {
                ServerPlayer owner = killer;
                if (owner == null && deathServer != null) {
                    owner = getOwnerPlayer(directEntity, deathServer);
                    if (owner == null) owner = getOwnerPlayer(srcEntity, deathServer);
                }
                if (owner != null && isQuestActive(owner)) {
                    QuestTracker.complete(owner, "quest.kill_a_blaze_with_a_snowball");
                }
            }
        }
        if (target instanceof net.minecraft.world.entity.monster.breeze.Breeze) {
            UUID pending = consumePendingProjectileKill(
                    target,
                    "wind_charge",
                    deathServer == null ? 0 : com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(deathServer)
            );
            if (pending != null && deathServer != null) {
                ServerPlayer owner = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(deathServer, pending);
                if (owner != null && isQuestActive(owner)) {
                    QuestTracker.complete(owner, "quest.kill_a_breeze_with_a_wind_charge");
                }
            }
            if (pending == null && isWindChargeDamage(event.getSource(), directEntity, srcEntity)) {
                ServerPlayer owner = killer;
                if (owner == null && deathServer != null) {
                    owner = getOwnerPlayer(directEntity, deathServer);
                    if (owner == null) owner = getOwnerPlayer(srcEntity, deathServer);
                }
                if (owner != null && isQuestActive(owner)) {
                    QuestTracker.complete(owner, "quest.kill_a_breeze_with_a_wind_charge");
                }
            }
        }

        if (srcEntity instanceof Wolf attackingWolf && deathServer != null) {
            ServerPlayer wolfOwner = getOwnerPlayer(attackingWolf, deathServer);
            if (wolfOwner != null && isQuestActive(wolfOwner)) {
                Identifier killedType = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
                if (killedType != null) {
                    Set<Identifier> seen = uniqueWolfKillMobs.computeIfAbsent(
                            com.jamie.jamiebingo.util.EntityUtil.getUUID(wolfOwner),
                            k -> new HashSet<>()
                    );
                    if (seen.add(killedType)) {
                        QuestTracker.addProgress(wolfOwner, "quest.have_a_tamed_wolf_kill_3_unique_mobs", 1, 3);
                        QuestTracker.addProgress(wolfOwner, "quest.have_a_tamed_wolf_kill_5_unique_mobs", 1, 5);
                        QuestTracker.addProgress(wolfOwner, "quest.have_a_tamed_wolf_kill_8_unique_mobs", 1, 8);
                    }
                }

                if (target instanceof Wolf victimWolf) {
                    ServerPlayer victimOwner = getOwnerPlayer(victimWolf, deathServer);
                    if (victimOwner != null
                            && !com.jamie.jamiebingo.util.EntityUtil.getUUID(victimOwner).equals(com.jamie.jamiebingo.util.EntityUtil.getUUID(wolfOwner))) {
                        UUID ownerTeam = getTeamId(wolfOwner);
                        UUID victimTeam = getTeamId(victimOwner);
                        if (ownerTeam != null && victimTeam != null && !ownerTeam.equals(victimTeam)) {
                            QuestTracker.complete(wolfOwner, "quest.have_a_tamed_wolf_kill_another_players_tamed_wolf");
                        }
                    }
                }
            }
        }

        if (killer == null || !isQuestActive(killer)) return;
        Entity direct = directEntity;

        if (killer.getVehicle() instanceof AbstractHorse horse
                && !com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(getHorseArmorItem(horse))) {
            ItemStack main = killer.getMainHandItem();
            ItemStack off = killer.getOffhandItem();
            if (main.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wooden_spear"))
                    || main.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:stone_spear"))
                    || main.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_spear"))
                    || main.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_spear"))
                    || main.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_spear"))
                    || main.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_spear"))
                    || main.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_spear"))
                    || off.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wooden_spear"))
                    || off.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:stone_spear"))
                    || off.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_spear"))
                    || off.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_spear"))
                    || off.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_spear"))
                    || off.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_spear"))
                    || off.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_spear"))) {
                QuestTracker.complete(killer, "quest.kill_a_mob_using_a_spear_whilst_riding_an_armored_horse");
            }
        }

        Identifier typeId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (typeId != null) {
            String path = typeId.getPath();
            switch (path) {
                case "bat" -> QuestTracker.complete(killer, "quest.kill_a_bat");
                case "bee" -> QuestTracker.complete(killer, "quest.kill_bee");
                case "blaze" -> QuestTracker.complete(killer, "quest.kill_blaze");
                case "bogged" -> QuestTracker.complete(killer, "quest.kill_bogged");
                case "breeze" -> QuestTracker.complete(killer, "quest.kill_breeze");
                case "cave_spider" -> QuestTracker.complete(killer, "quest.kill_cave_spider");
                case "drowned" -> QuestTracker.complete(killer, "quest.kill_drowned");
                case "endermite" -> QuestTracker.complete(killer, "quest.kill_endermite");
                case "evoker" -> QuestTracker.complete(killer, "quest.kill_evoker");
                case "guardian" -> QuestTracker.complete(killer, "quest.kill_guardian");
                case "husk" -> QuestTracker.complete(killer, "quest.kill_husk");
                case "magma_cube" -> QuestTracker.complete(killer, "quest.kill_magma_cube");
                case "piglin_brute" -> QuestTracker.complete(killer, "quest.kill_piglin_brute");
                case "pillager" -> QuestTracker.complete(killer, "quest.kill_pillager");
                case "ravager" -> QuestTracker.complete(killer, "quest.kill_ravager");
                case "shulker" -> QuestTracker.complete(killer, "quest.kill_shulker");
                case "slime" -> QuestTracker.complete(killer, "quest.kill_slime");
                case "vex" -> QuestTracker.complete(killer, "quest.kill_vex");
                case "wither_skeleton" -> QuestTracker.complete(killer, "quest.kill_wither_skeleton");
                case "zombie" -> QuestTracker.complete(killer, "quest.kill_zombie");
                case "zombified_piglin" -> QuestTracker.complete(killer, "quest.kill_zombified_piglin");
                default -> {
                }
            }
            if (RAID_MOBS.contains(typeId)) {
                recordRaidKill(killer, typeId);
            }
            if (GOLEM_TYPES.contains(typeId)) {
                recordGolemKill(killer, typeId);
            }
        }

        if (target instanceof Ghast) {
            QuestTracker.complete(killer, "quest.kill_ghast");
        }
        if (target instanceof net.minecraft.world.entity.animal.golem.SnowGolem) {
            QuestTracker.complete(killer, "quest.kill_snow_golem");
        }
        if (target instanceof ZombieVillager) {
            QuestTracker.complete(killer, "quest.kill_zombie_villager");
        }

        if (isBabyMob(target)) {
            QuestTracker.complete(killer, "quest.kill_a_baby_mob");
            UUID id = com.jamie.jamiebingo.util.EntityUtil.getUUID(killer);
            int next = babyMobKills.getOrDefault(id, 0) + 1;
            babyMobKills.put(id, next);
            QuestTracker.addProgress(killer, "quest.kill_5_baby_mobs", 1, 5);
            QuestTracker.addProgress(killer, "quest.kill_20_baby_mobs", 1, 20);
        }

        if (target instanceof net.minecraft.world.entity.animal.golem.SnowGolem && killer.level().dimension() == Level.NETHER) {
            QuestTracker.complete(killer, "quest.kill_a_snow_golem_in_the_nether");
        }

        if (target instanceof net.minecraft.world.entity.monster.Blaze) {
            if (isSnowballDamage(event.getSource(), direct, srcEntity)) {
                QuestTracker.complete(killer, "quest.kill_a_blaze_with_a_snowball");
            }
        }

        if (target instanceof net.minecraft.world.entity.monster.breeze.Breeze) {
            if (isWindChargeDamage(event.getSource(), direct, srcEntity)) {
                QuestTracker.complete(killer, "quest.kill_a_breeze_with_a_wind_charge");
            }
        }

        if (target instanceof net.minecraft.world.entity.monster.illager.Pillager) {
            ItemStack main = killer.getMainHandItem();
            ItemStack off = killer.getOffhandItem();
            if (main.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:crossbow"))
                    || off.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:crossbow"))) {
                QuestTracker.complete(killer, "quest.kill_a_pillager_with_a_crossbow");
            }
        }
        if (target instanceof Guardian && !(target instanceof ElderGuardian)) {
            QuestTracker.complete(killer, "quest.kill_guardian");
        }
        if (target instanceof Stray) {
            QuestTracker.complete(killer, "quest.kill_stray");
        }
        if (target instanceof Witch) {
            QuestTracker.complete(killer, "quest.kill_witch");
        }
        if (target instanceof Zoglin) {
            QuestTracker.complete(killer, "quest.kill_zoglin");
        }
        if (target instanceof ElderGuardian) {
            QuestTracker.complete(killer, "quest.kill_elder_guardian");
        }
        if (target instanceof Endermite) {
            QuestTracker.complete(killer, "quest.kill_endermite");
        }
        if (target instanceof Silverfish) {
            QuestTracker.complete(killer, "quest.kill_silverfish");
        }
        if (target instanceof EnderDragon) {
            QuestTracker.complete(killer, "quest.kill_the_ender_dragon");
        }

        if (target instanceof Sheep sheep) {
            String color = sheep.getColor().getName();
            QuestTracker.complete(killer, "quest.kill_" + color + "_sheep");
        }

        if (target instanceof ServerPlayer victim) {
            UUID killerTeam = getTeamId(killer);
            UUID victimTeam = getTeamId(victim);
            if (killerTeam != null && !killerTeam.equals(victimTeam)) {
                QuestTracker.complete(killer, "quest.kill_an_opponent");
            }
            return;
        }

        QuestTracker.increment(killer, "quest.kill_100_mobs", 100);

        if (target.getType().is(arthropodTag())) {
            QuestTracker.increment(killer, "quest.kill_20_arthropods", 20);
        }

        if (target.getType().is(undeadTag())) {
            QuestTracker.increment(killer, "quest.kill_30_undead_mobs", 30);
        }

        if (target instanceof Enemy) {
            recordUniqueHostileKill(killer, target);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event == null) return;
        Entity exploder = null;
        try {
            exploder = event.getExplosion().getDirectSourceEntity();
        } catch (Throwable ignored) {
            // fallback below
        }
        if (!(exploder instanceof MinecartTNT)
                && (exploder == null || exploder.getType() != EntityType.TNT_MINECART)) {
            return;
        }
        int tick = 0;
        if (event.getLevel() instanceof ServerLevel level) {
            tick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer());
        }
        for (Entity entity : event.getAffectedEntities()) {
            if (entity instanceof ServerPlayer player) {
                lastTntMinecartTick.put(
                        com.jamie.jamiebingo.util.EntityUtil.getUUID(player),
                        tick > 0 ? tick : com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(player)
                );
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;

        BlockState state = event.getState();
        ItemStack mainHand = p.getMainHandItem();
        if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) {
            QuestTracker.complete(p, "quest.mine_diamond_ore");
        }

        if (state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)) {
            QuestTracker.complete(p, "quest.mine_emerald_ore");
        }

        if (state.is(Blocks.SPAWNER)) {
            QuestTracker.complete(p, "quest.mine_mob_spawner");
        }

        if (state.is(Blocks.TURTLE_EGG)) {
            QuestTracker.complete(p, "quest.mine_turtle_egg");
        }

        if (state.is(Blocks.OBSIDIAN)
                && mainHand.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_pickaxe"))) {
            QuestTracker.complete(p, "quest.mine_obsidian_using_an_iron_pickaxe");
        }

        if (state.is(Blocks.STONE) && com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(mainHand)) {
            QuestTracker.complete(p, "quest.mine_stone_using_your_fists");
        }
    }

    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!isQuestActive(p)) return;
        QuestTracker.complete(p, "quest.trample_farmland");
    }

    @SubscribeEvent
    public static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockState state = event.getPlacedBlock();
        BlockPos pos = event.getPos();
        int tick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer());

        if (state.is(BlockTags.SAPLINGS) && event.getEntity() instanceof ServerPlayer p && isQuestActive(p)) {
            saplingOwners.put(
                    pos.immutable(),
                    new SaplingOwner(
                            com.jamie.jamiebingo.util.EntityUtil.getUUID(p),
                            level.dimension(),
                            tick + 20 * 60 * 20
                    )
            );
        }

        if ((state.is(Blocks.BEE_NEST) || state.is(Blocks.BEEHIVE))
                && level.dimension() == Level.NETHER
                && event.getEntity() instanceof ServerPlayer p
                && isQuestActive(p)) {
            QuestTracker.complete(p, "quest.place_a_bee_nest_or_a_bee_hive_in_the_nether");
        }

        if (state.is(Blocks.FROGSPAWN)) {
            ServerPlayer owner = null;
            int loveTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer());
            UUID ownerId = consumeLoveOwner(event.getEntity(), loveTick);
            if (ownerId != null) {
                owner = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(level.getServer(), ownerId);
            }
            Player nearest = owner != null ? owner : null;
            if (nearest instanceof ServerPlayer p && isQuestActive(p)) {
                QuestTracker.complete(p, "quest.breed_frogs");
                recordUniqueBreed(p, EntityType.FROG);
            }
        }
    }

    @SubscribeEvent
    public static void onSaplingGrow(SaplingGrowTreeEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        if (pos == null) return;
        int now = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer());

        ServerPlayer credited = null;
        for (int dx = -2; dx <= 2 && credited == null; dx++) {
            for (int dy = -2; dy <= 2 && credited == null; dy++) {
                for (int dz = -2; dz <= 2 && credited == null; dz++) {
                    SaplingOwner owner = saplingOwners.get(pos.offset(dx, dy, dz));
                    if (owner == null) continue;
                    if (!owner.dimension.equals(level.dimension())) continue;
                    if (owner.expireTick < now) continue;
                    ServerPlayer byId = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(level.getServer(), owner.playerId);
                    if (byId != null && isQuestActive(byId)) {
                        credited = byId;
                    }
                }
            }
        }

        if (credited == null) {
            Player nearestPlayer = level.getNearestPlayer(
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 12.0D, false);
            if (nearestPlayer instanceof ServerPlayer nearest && isQuestActive(nearest)) {
                credited = nearest;
            }
        }

        if (credited != null) {
            QuestTracker.increment(credited, "quest.grow_20_trees", 20);
        }
    }

    private static void checkBiomeQuests(ServerPlayer p, ServerLevel level) {
        BlockPos pos = com.jamie.jamiebingo.util.EntityPosUtil.getBlockPos(p);
        Identifier biomeId = level.getBiome(pos)
                .unwrapKey()
                .map(ResourceKey::identifier)
                .orElse(null);
        if (biomeId == null) return;

        if (level.getBiome(pos).is(BiomeTags.IS_BADLANDS)) {
            QuestTracker.complete(p, "quest.find_a_badlands_biome");
        }

        if (biomeId.equals(id("minecraft:mushroom_fields"))) {
            QuestTracker.complete(p, "quest.find_a_mushroom_biome");
        }

        if (biomeId.equals(id("minecraft:ice_spikes"))) {
            QuestTracker.complete(p, "quest.find_an_ice_spike_biome");
        }

        recordUniqueBiome(p, biomeId);

        if (CAVE_BIOMES.contains(biomeId)) {
            recordUniqueCaveBiome(p, biomeId);
        }

        if (level.dimension() == Level.NETHER) {
            recordNetherBiome(p, biomeId);
        }
    }

    private static boolean isHappyGhastEntity(Entity entity) {
        if (entity == null) return false;
        Identifier id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) return false;
        String path = id.getPath();
        return path.contains("happy_ghast") || path.contains("happyghast");
    }

    private static boolean isDriedGhastEntity(Entity entity) {
        if (entity == null) return false;
        Identifier id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) return false;
        String path = id.getPath();
        return path.contains("dried_ghast") || path.contains("driedghast") || path.contains("ghastling");
    }

    private static boolean isRidingHappyGhast(ServerPlayer p) {
        if (p == null) return false;
        Entity root = rootVehicle(p);
        return isHappyGhastEntity(root);
    }

    private static Entity getSupportedHappyGhast(Entity entity) {
        if (entity == null) return null;
        Entity root = rootVehicle(entity);
        if (isHappyGhastEntity(root)) {
            return root;
        }

        net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(entity);
        if (!(baseLevel instanceof ServerLevel level)) return null;

        AABB feetBox = entity.getBoundingBox().inflate(0.3D, 0.2D, 0.3D).move(0.0D, -0.9D, 0.0D);
        List<Entity> nearby = level.getEntities(entity, feetBox, QuestEvents::isHappyGhastEntity);
        Entity best = null;
        double bestDelta = Double.MAX_VALUE;
        double entityBottom = entity.getBoundingBox().minY;
        for (Entity e : nearby) {
            double delta = Math.abs(entityBottom - e.getBoundingBox().maxY);
            if (delta < bestDelta) {
                bestDelta = delta;
                best = e;
            }
        }
        return best;
    }

    private static void updateLadderClimbProgress(ServerPlayer p) {
        UUID id = com.jamie.jamiebingo.util.EntityUtil.getUUID(p);
        Vec3 now = p.position();
        Vec3 last = lastLadderPos.put(id, now);
        if (last == null) return;
        double dy = now.y - last.y;
        if (dy <= 0.0D) return;
        int cm = (int) Math.round(dy * 100.0D);
        if (cm <= 0) return;
        QuestTracker.addProgress(p, "quest.use_a_ladder_to_climb_64_meters_in_hieght", cm, 6400);
    }

    private static boolean isInCobweb(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return false;
        return level.getBlockState(pos).is(Blocks.COBWEB)
                || level.getBlockState(pos.above()).is(Blocks.COBWEB);
    }

    private static boolean isOnBoatOnIce(ServerLevel level, ServerPlayer p) {
        if (level == null || p == null) return false;
        if (!(p.getVehicle() instanceof Boat boat)) return false;
        BlockPos belowBoat = boat.blockPosition().below();
        BlockState state = level.getBlockState(belowBoat);
        return state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE)
                || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.FROSTED_ICE);
    }

    private static boolean isSculkSensorPoweredNearby(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return false;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos check = pos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(check);
                    if (!state.is(Blocks.SCULK_SENSOR)) continue;
                    if (state.hasProperty(BlockStateProperties.POWER) && state.getValue(BlockStateProperties.POWER) > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isCrawling(ServerPlayer p) {
        if (p == null) return false;
        if (p.isInWater()) return false;
        return (p.isSwimming() && p.onGround())
                || p.getPose() == net.minecraft.world.entity.Pose.SWIMMING;
    }

    private static boolean isOnMud(ServerLevel level, ServerPlayer p) {
        if (level == null || p == null) return false;
        BlockPos feet = p.blockPosition();
        BlockPos below = feet.below();
        BlockPos on = p.getOnPos();
        return level.getBlockState(feet).is(Blocks.MUD)
                || level.getBlockState(below).is(Blocks.MUD)
                || level.getBlockState(on).is(Blocks.MUD)
                || level.getBlockState(on.below()).is(Blocks.MUD);
    }

    private static void checkLiveTamedMobQuest(ServerPlayer p, ServerLevel level) {
        if (p == null || level == null) return;
        AABB area = p.getBoundingBox().inflate(96.0D, 48.0D, 96.0D);
        List<Entity> entities = level.getEntities((Entity) null, area, Entity::isAlive);
        int count = 0;
        for (Entity e : entities) {
            if (!isTamedEntity(e)) continue;
            ServerPlayer owner = getOwnerPlayer(e, com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
            if (owner == null) continue;
            if (com.jamie.jamiebingo.util.EntityUtil.getUUID(owner).equals(com.jamie.jamiebingo.util.EntityUtil.getUUID(p))) {
                count++;
                if (count >= 10) {
                    break;
                }
            }
        }
        QuestTracker.setProgress(p, "quest.have_10_live_tamed_mobs", Math.min(count, 10));
        if (count >= 10) {
            QuestTracker.complete(p, "quest.have_10_live_tamed_mobs");
        }
    }

    private static boolean craftedWithPotterySherd(PlayerEvent.ItemCraftedEvent event) {
        if (event == null) return false;
        return true;
    }

    private static boolean isJukeboxPlayingNearby(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return false;
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos check = pos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(check);
                    if (!state.is(Blocks.JUKEBOX)) continue;
                    if (state.hasProperty(BlockStateProperties.HAS_RECORD) && state.getValue(BlockStateProperties.HAS_RECORD)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void checkInventoryQuests(ServerPlayer p) {
        ItemStack head = p.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = p.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = p.getItemBySlot(EquipmentSlot.FEET);

        checkTeamTrimmedArmor(p);

        if (isArmorSet(head, chest, legs, feet, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_helmet"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_chestplate"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_leggings"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_boots"))) {
            QuestTracker.complete(p, "quest.wear_iron_armor_set");
        }

        if (isArmorSet(head, chest, legs, feet, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:leather_helmet"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:leather_chestplate"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:leather_leggings"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:leather_boots"))) {
            QuestTracker.complete(p, "quest.wear_leather_armor_set");
        }

        if (isArmorSet(head, chest, legs, feet, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_helmet"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_chestplate"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_leggings"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_boots"))) {
            QuestTracker.complete(p, "quest.wear_gold_armor_set");
        }

        if (isArmorSet(head, chest, legs, feet, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_helmet"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_chestplate"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_leggings"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_boots"))) {
            QuestTracker.complete(p, "quest.wear_diamond_armor_set");
        }

        if (isArmorSet(head, chest, legs, feet, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_helmet"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_chestplate"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_leggings"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_boots"))) {
            QuestTracker.complete(p, "quest.wear_copper_armor_set");
        }

        if (isChainOrNetheritePiece(head, chest, legs, feet, true)) {
            QuestTracker.complete(p, "quest.wear_1_piece_of_chain_armor");
        }

        if (isChainOrNetheritePiece(head, chest, legs, feet, false)) {
            QuestTracker.complete(p, "quest.wear_1_piece_of_netherite_armor");
        }

        checkColoredLeatherArmor(p, head, chest, legs, feet);

        if (hasFullToolSet(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wooden_shovel"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wooden_hoe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wooden_sword"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wooden_axe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wooden_pickaxe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wooden_spear"))) {
            QuestTracker.complete(p, "quest.full_set_of_wooden_tools");
        }

        if (hasFullToolSet(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:stone_shovel"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:stone_hoe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:stone_sword"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:stone_axe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:stone_pickaxe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:stone_spear"))) {
            QuestTracker.complete(p, "quest.full_set_of_stone_tools");
        }

        if (hasFullToolSet(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_shovel"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_hoe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_sword"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_axe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_pickaxe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_spear"))) {
            QuestTracker.complete(p, "quest.full_set_of_iron_tools");
        }

        if (hasFullToolSet(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_shovel"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_hoe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_sword"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_axe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_pickaxe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_spear"))) {
            QuestTracker.complete(p, "quest.full_set_of_gold_tools");
        }

        if (hasFullToolSet(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_shovel"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_hoe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_sword"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_axe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_pickaxe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_spear"))) {
            QuestTracker.complete(p, "quest.full_set_of_diamond_tools");
        }

        if (hasFullToolSet(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_shovel"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_hoe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_sword"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_axe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_pickaxe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_spear"))) {
            QuestTracker.complete(p, "quest.obtain_all_netherite_tools");
        }

        if (hasItemSet(p, Set.of(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wooden_axe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:stone_axe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_axe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_axe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_axe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_axe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_axe")))) {
            QuestTracker.complete(p, "quest.obtain_every_type_of_axe");
        }

        if (hasItemSet(p, Set.of(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wooden_hoe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:stone_hoe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_hoe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_hoe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_hoe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_hoe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_hoe")))) {
            QuestTracker.complete(p, "quest.obtain_every_type_of_hoe");
        }

        if (hasItemSet(p, Set.of(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wooden_pickaxe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:stone_pickaxe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_pickaxe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_pickaxe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_pickaxe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_pickaxe"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_pickaxe")))) {
            QuestTracker.complete(p, "quest.obtain_every_type_of_pickaxe");
        }

        if (hasItemSet(p, Set.of(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wooden_shovel"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:stone_shovel"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_shovel"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_shovel"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_shovel"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_shovel"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_shovel")))) {
            QuestTracker.complete(p, "quest.obtain_every_type_of_shovel");
        }

        if (hasItemSet(p, Set.of(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wooden_sword"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:stone_sword"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_sword"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_sword"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_sword"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_sword"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_sword")))) {
            QuestTracker.complete(p, "quest.obtain_every_type_of_sword");
        }

        if (hasItemSet(p, Set.of(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:raw_iron_block"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:raw_gold_block"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:raw_copper_block")))) {
            QuestTracker.complete(p, "quest.obtain_every_type_of_raw_ore_block");
        }

        if (hasItemSet(p, Set.of(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:leather_horse_armor"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_horse_armor"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_horse_armor"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_horse_armor")))) {
            QuestTracker.complete(p, "quest.obtain_every_type_of_horse_armor");
        }

        if (hasItemSet(p, Set.of(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wheat_seeds"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:beetroot_seeds"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:melon_seeds"), com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:pumpkin_seeds")))) {
            QuestTracker.complete(p, "quest.obtain_every_type_of_seed");
        }

        if (hasItemSet(p, Set.of(
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wooden_spear"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:stone_spear"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_spear"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:iron_spear"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:golden_spear"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:diamond_spear"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_spear")
        ))) {
            QuestTracker.complete(p, "quest.collect_all_spears");
        }

        if (hasItemSet(p, Set.of(
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_shovel"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_hoe"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_sword"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_axe"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_pickaxe"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:copper_spear")
        ))) {
            QuestTracker.complete(p, "quest.obtain_all_copper_tools");
        }

        if (countTaggedItems(p, ItemTags.SAPLINGS) >= 5) {
            QuestTracker.complete(p, "quest.obtain_5_types_of_saplings");
        }

        if (countTaggedItems(p, ItemTags.FLOWERS) >= 6) {
            QuestTracker.complete(p, "quest.obtain_6_unique_flowers");
        }

        checkEggCollections(p);
        checkBundleOfBundles(p);
        checkArmorTrimTemplatesInInventory(p);

        checkPotionInventory(p);
        checkAnyStack64(p);
        checkColor64(p);
        checkUniqueCollections(p);
        checkObtainAllCollections(p);
        checkObtain64Extras(p);
        checkInventoryUnique(p);
    }

    private static void checkInventoryObtainTriggers(ServerPlayer p) {
        if (p == null) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(p);
        if (server == null) return;

        if (hasItem(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:obsidian"))) {
            triggerOpponentQuest(p, "quest.an_opponent_obtains_obsidian");
            MineModeManager.onPlayerTrigger(server, p, MineModeManager.Trigger.OBTAIN_OBSIDIAN);
        }

        if (hasItem(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wheat_seeds"))) {
            triggerOpponentQuest(p, "quest.an_opponent_obtains_wheat_seeds");
            MineModeManager.onPlayerTrigger(server, p, MineModeManager.Trigger.OBTAIN_WHEAT_SEEDS);
        }

        if (hasItem(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:crafting_table"))) {
            triggerOpponentQuest(p, "quest.an_opponent_obtains_a_crafting_table");
            MineModeManager.onPlayerTrigger(server, p, MineModeManager.Trigger.OBTAIN_CRAFTING_TABLE);
        }
    }

    private static void checkAdvancementQuests(ServerPlayer p) {
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(p);
        if (server == null) return;
        if (!advancementsLoaded(server)) return;

        net.minecraft.advancements.AdvancementHolder sniper = getAdvancement(server, id("minecraft:adventure/sniper_duel"));
        if (sniper != null && isAdvancementDone(p, sniper)) {
            QuestTracker.complete(p, "quest.get_sniper_duel_advancement");
        }

        net.minecraft.advancements.AdvancementHolder bullseye = getAdvancement(server, id("minecraft:adventure/bullseye"));
        if (bullseye != null && isAdvancementDone(p, bullseye)) {
            QuestTracker.complete(p, "quest.get_bullseye_advancement");
        }

        net.minecraft.advancements.AdvancementHolder boatLegs = getAdvancement(server, id("minecraft:nether/ride_strider"));
        if (boatLegs != null && isAdvancementDone(p, boatLegs)) {
            QuestTracker.complete(p, "quest.get_this_boat_has_legs_advancement");
        }

        if (hasAnySpyglassAdvancement(p, server)) {
            QuestTracker.complete(p, "quest.get_any_spyglass_advancement");
        }

        net.minecraft.advancements.AdvancementHolder bastion = getAdvancement(server, id("minecraft:nether/find_bastion"));
        if (bastion != null && isAdvancementDone(p, bastion)) {
            QuestTracker.complete(p, "quest.find_a_bastion");
        }

        net.minecraft.advancements.AdvancementHolder allayFriend =
                getAdvancement(server, id("minecraft:husbandry/allay_deliver_item_to_player"));
        if (allayFriend != null && isAdvancementDone(p, allayFriend)) {
            QuestTracker.complete(p, "quest.have_an_allay_give_you_an_item");
        }

        net.minecraft.advancements.AdvancementHolder fortress = getAdvancement(server, id("minecraft:nether/find_fortress"));
        if (fortress != null && isAdvancementDone(p, fortress)) {
            QuestTracker.complete(p, "quest.find_a_fortress");
        }

        net.minecraft.advancements.AdvancementHolder stronghold = getAdvancement(server, id("minecraft:story/follow_ender_eye"));
        if (stronghold != null && isAdvancementDone(p, stronghold)) {
            QuestTracker.complete(p, "quest.find_a_stronghold");
        }

        net.minecraft.advancements.AdvancementHolder endCity = getAdvancement(server, id("minecraft:end/find_end_city"));
        if (endCity != null && isAdvancementDone(p, endCity)) {
            QuestTracker.complete(p, "quest.find_an_end_city");
        }

        net.minecraft.advancements.AdvancementHolder stayHydrated =
                getAdvancement(server, id("minecraft:husbandry/stay_hydrated"));
        if ((stayHydrated != null && isAdvancementDone(p, stayHydrated))
                || isAdvancementDone(p, getAdvancementBySuffix(server, "stay_hydrated"))
                || isAdvancementDone(p, getAdvancementByTitle(server, "stay hydrated"))) {
            QuestTracker.complete(p, "quest.fully_hydrate_a_dried_ghast");
        }

        net.minecraft.advancements.AdvancementHolder crafterCrafter =
                getAdvancementBySuffix(server, "crafters_crafting_crafters");
        if ((crafterCrafter != null && isAdvancementDone(p, crafterCrafter))
                || isAdvancementDone(p, getAdvancementByTitle(server, "crafters crafting crafters"))) {
            QuestTracker.complete(p, "quest.use_a_crafter_to_craft_a_crafter");
        }

        net.minecraft.advancements.AdvancementHolder enchant = getAdvancement(server, id("minecraft:story/enchant_item"));
        if (enchant != null && isAdvancementDone(p, enchant)) {
            QuestTracker.complete(p, "quest.use_an_enchanting_table");
        }

        net.minecraft.advancements.AdvancementHolder brew = getAdvancement(server, id("minecraft:nether/brew_potion"));
        if (brew != null && isAdvancementDone(p, brew)) {
            QuestTracker.complete(p, "quest.use_a_brewing_stand");
        }

        net.minecraft.advancements.AdvancementHolder upgrade = getAdvancement(server, id("minecraft:story/upgrade_gear"));
        if (upgrade != null && isAdvancementDone(p, upgrade)) {
            QuestTracker.complete(p, "quest.use_a_smithing_table");
        }

        net.minecraft.advancements.AdvancementHolder caves = getAdvancement(server, id("minecraft:adventure/caves_and_cliffs"));
        if ((caves != null && isAdvancementDone(p, caves))
                || isAdvancementDone(p, getAdvancementBySuffix(server, "caves_and_cliffs"))
                || isAdvancementDone(p, getAdvancementByTitle(server, "caves and cliffs"))) {
            QuestTracker.complete(p, "quest.do_caves_and_cliffs_advancement");
        }

        net.minecraft.advancements.AdvancementHolder powerBooks = getAdvancement(server, id("minecraft:adventure/power_of_books"));
        if ((powerBooks != null && isAdvancementDone(p, powerBooks))
                || isAdvancementDone(p, getAdvancementBySuffix(server, "power_of_books"))
                || isAdvancementDone(p, getAdvancementByTitle(server, "power of books"))) {
            QuestTracker.complete(p, "quest.do_power_of_books_advancement");
        }

        net.minecraft.advancements.AdvancementHolder starTrader = getAdvancement(server, id("minecraft:adventure/star_trader"));
        if ((starTrader != null && isAdvancementDone(p, starTrader))
                || isAdvancementDone(p, getAdvancementBySuffix(server, "star_trader"))
                || isAdvancementDone(p, getAdvancementByTitle(server, "star trader"))) {
            QuestTracker.complete(p, "quest.do_star_trader_advancement");
        }

        net.minecraft.advancements.AdvancementHolder mobKabob = getAdvancement(server, id("minecraft:adventure/kill_all_mobs"));
        if ((mobKabob != null && isAdvancementDone(p, mobKabob))
                || isAdvancementDone(p, getAdvancementBySuffix(server, "kill_all_mobs"))
                || isAdvancementDone(p, getAdvancementByTitle(server, "mob kabob"))) {
            QuestTracker.complete(p, "quest.obtain_the_mob_kabob_advancement");
        }

        net.minecraft.advancements.AdvancementHolder subspace = getAdvancement(server, id("minecraft:nether/fast_travel"));
        if (subspace != null && isAdvancementDone(p, subspace)) {
            QuestTracker.complete(p, "quest.get_subspace_bubble_advancement");
        }

        net.minecraft.advancements.AdvancementHolder enterNether = getAdvancement(server, id("minecraft:story/enter_the_nether"));
        if (enterNether != null && isAdvancementDone(p, enterNether)) {
            QuestTracker.complete(p, "quest.enter_nether");
        }

        net.minecraft.advancements.AdvancementHolder enterEnd = getAdvancement(server, id("minecraft:story/enter_the_end"));
        if (enterEnd != null && isAdvancementDone(p, enterEnd)) {
            QuestTracker.complete(p, "quest.enter_end");
        }

        int earned = advancementEarnedCounts.getOrDefault(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), 0);
        if (server != null && BingoGameData.get(server).teamSyncEnabled) {
            UUID teamId = TeamData.get(server).getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
            if (teamId != null) {
                earned = countTeamUniqueAdvancements(server, teamId);
            }
        }
        if (earned >= 15) {
            QuestTracker.complete(p, "quest.get_15_advancements");
        }
        if (earned >= 25) {
            QuestTracker.complete(p, "quest.get_25_advancements");
        }
        if (earned >= 35) {
            QuestTracker.complete(p, "quest.get_35_advancements");
        }
        BroadcastHelper.broadcastProgress(server);
    }

    private static void updateLeaderQuests(MinecraftServer server) {
        TeamData teamData = TeamData.get(server);
        long activeTeams = teamData.getTeams().stream().filter(t -> !t.members.isEmpty()).count();
        if (activeTeams < 2) {
            return;
        }
        Map<UUID, Integer> driedKelpCounts = new HashMap<>();
        Map<UUID, Integer> hopperCounts = new HashMap<>();
        Map<UUID, Integer> levelCounts = new HashMap<>();
        Map<UUID, Integer> uniqueCraftCounts = new HashMap<>();

        for (TeamData.TeamInfo team : teamData.getTeams()) {
            if (team.members.isEmpty()) continue;

            int dried = 0;
            int hoppers = 0;
            int levels = 0;
            Set<Identifier> uniqueCraftsForTeam = new HashSet<>();

            for (UUID memberId : team.members) {
                ServerPlayer member = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
                if (member == null) continue;
                dried += countItem(member, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:dried_kelp_block"));
                hoppers += countItem(member, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:hopper"));
                levels += com.jamie.jamiebingo.util.PlayerExperienceUtil.getExperienceLevel(member);
                uniqueCraftsForTeam.addAll(uniqueCrafts.getOrDefault(com.jamie.jamiebingo.util.EntityUtil.getUUID(member), Set.of()));
            }

            driedKelpCounts.put(team.id, dried);
            hopperCounts.put(team.id, hoppers);
            levelCounts.put(team.id, levels);
            uniqueCraftCounts.put(team.id, uniqueCraftsForTeam.size());
        }

        updateLeaderQuest(server, "quest.obtain_more_dried_kelp_blocks_than_the_opponent", driedKelpCounts);
        updateLeaderQuest(server, "quest.obtain_more_hoppers_than_the_opponent", hopperCounts);
        updateLeaderQuest(server, "quest.have_more_levels_than_the_opponent", levelCounts);
        updateLeaderQuest(server, "quest.have_more_unique_crafts_than_the_opponents", uniqueCraftCounts);
    }

    private static void processPendingChecks(MinecraftServer server) {
        int tick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);

        pendingComposterChecks.removeIf(check -> {
            if (tick < check.checkTick) return false;
            ServerLevel level = check.level;
            BlockState state = level.getBlockState(check.pos);
            if (state.is(Blocks.COMPOSTER)) {
                int lvl = state.getValue(ComposterBlock.LEVEL);
                if (lvl >= 7) {
                    ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, check.playerId);
                    if (p != null) QuestTracker.complete(p, "quest.fill_a_composter");
                }
            }
            return true;
        });

        pendingAnchorChecks.removeIf(check -> {
            if (tick < check.checkTick) return false;
            ServerLevel level = check.level;
            BlockState state = level.getBlockState(check.pos);
            if (state.is(Blocks.RESPAWN_ANCHOR)) {
                int charge = state.getValue(RespawnAnchorBlock.CHARGE);
                if (charge >= 4) {
                    ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, check.playerId);
                    if (p != null) QuestTracker.complete(p, "quest.charge_a_respawn_anchor_to_max");
                }
            }
            return true;
        });

        pendingEggChecks.removeIf(check -> {
            if (tick < check.checkTick) return false;
            ServerLevel level = check.level;
            BlockPos min = new BlockPos(
                    check.pos.getX() - 2,
                    check.pos.getY() - 2,
                    check.pos.getZ() - 2
            );
            BlockPos max = new BlockPos(
                    check.pos.getX() + 2,
                    check.pos.getY() + 2,
                    check.pos.getZ() + 2
            );
            AABB box = new AABB(Vec3.atLowerCornerOf(min), Vec3.atLowerCornerOf(max));
            List<Chicken> chickens = level.getEntitiesOfClass(
                    Chicken.class,
                    box,
                    c -> c.isBaby() && com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(c) < 5
            );
            if (!chickens.isEmpty()) {
                ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, check.playerId);
                if (p != null) QuestTracker.complete(p, "quest.spawn_a_chicken_with_an_egg");
            }
            return true;
        });

        pendingArmorStandChecks.removeIf(check -> {
            if (tick < check.checkTick) return false;
            ServerLevel level = check.level;
            Entity entity = level.getEntity(check.standId);
            if (entity instanceof ArmorStand stand && armorStandHasFullArmor(stand)) {
                ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, check.playerId);
                if (p != null) {
                    QuestTracker.complete(p, "quest.fill_an_armor_stand_with_armor");
                }
            }
            return true;
        });

        pendingEndCrystalChecks.removeIf(check -> {
            if (tick < check.checkTick) return false;
            ServerLevel level = check.level;
            AABB box = new AABB(
                    check.pos.getX() - 1, check.pos.getY() - 1, check.pos.getZ() - 1,
                    check.pos.getX() + 2, check.pos.getY() + 2, check.pos.getZ() + 2
            );
            List<EndCrystal> crystals = level.getEntitiesOfClass(EndCrystal.class, box);
            if (!crystals.isEmpty()) {
                ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, check.playerId);
                if (p != null) {
                    QuestTracker.complete(p, "quest.place_an_end_crystal");
                }
            }
            return true;
        });

        pendingVaultKeyChecks.removeIf(check -> {
            if (tick < check.checkTick) return false;
            ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, check.playerId);
            if (p != null) {
                int now = countItem(p, check.keyItem);
                int handCount = 0;
                ItemStack hand = check.hand == net.minecraft.world.InteractionHand.MAIN_HAND
                        ? p.getMainHandItem()
                        : p.getOffhandItem();
                if (hand.is(check.keyItem)) {
                    handCount = hand.getCount();
                }
                boolean consumed = now < check.countBefore || handCount < check.handCountBefore;
                if (!consumed) {
                    BlockEntity be = check.level.getBlockEntity(check.pos);
                    if (vaultKeyUsed(be)) {
                        consumed = true;
                    }
                }
                if (consumed) {
                    if (check.keyItem == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:trial_key")) {
                        QuestTracker.complete(p, "quest.open_trial_vault");
                    }
                    if (check.keyItem == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:ominous_trial_key")) {
                        QuestTracker.complete(p, "quest.open_ominous_vault");
                    }
                }
            }
            return true;
        });

        pendingWolfArmorChecks.removeIf(check -> {
            if (tick < check.checkTick) return false;
            ServerLevel level = check.level;
            Entity entity = level.getEntity(check.wolfId);
            if (entity instanceof Wolf wolf && hasWolfArmor(wolf)) {
                ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, check.playerId);
                if (p != null) {
                    QuestTracker.complete(p, "quest.equip_wolf_armor_on_a_wolf");
                }
            }
            return true;
        });

        pendingTameChecks.removeIf(check -> {
            if (tick < check.checkTick) return false;
            ServerLevel level = check.level;
            Entity entity = level.getEntity(check.entityId);
            if (entity != null && isTamedEntity(entity)) {
                ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, check.playerId);
                if (p != null) {
                    Identifier id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                    if (id != null && "nautilus".equals(id.getPath())) {
                        QuestTracker.complete(p, "quest.tame_a_nautilus");
                    } else if (entity instanceof Llama) {
                        QuestTracker.complete(p, "quest.tame_llama");
                    }
                }
                return true;
            }
            if (tick >= check.expireTick) {
                return true;
            }
            check.checkTick = tick + TAME_CHECK_INTERVAL;
            return false;
        });

        pendingCrafterUses.removeIf(check -> tick >= check.expireTick);

        pendingButtonPressChecks.removeIf(check -> {
            if (tick < check.checkTick) return false;
            BlockState state = check.level.getBlockState(check.pos);
            if (state.is(BlockTags.BUTTONS)
                    && state.hasProperty(BlockStateProperties.POWERED)
                    && state.getValue(BlockStateProperties.POWERED)
                    && !check.wasPoweredOnClick) {
                ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, check.playerId);
                if (p != null && isQuestActive(p)) {
                    QuestTracker.increment(p, "quest.push_a_button_20_times", 20);
                }
            }
            return true;
        });

        pendingContainerInsertChecks.removeIf(check -> {
            ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, check.playerId);
            if (p == null || !isQuestActive(p)) return true;
            if (tick >= check.expireTick) return true;
            BlockEntity be = check.level.getBlockEntity(check.pos);
            if (be instanceof Container container) {
                if (countSlotsFilledWithItem(container, check.targetItem) > 0) {
                    QuestTracker.complete(p, check.questId);
                    return true;
                }
            }
            return false;
        });

        pendingCraftingTableInsertChecks.removeIf(check -> {
            ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, check.playerId);
            if (p == null || !isQuestActive(p)) return true;
            if (tick >= check.expireTick) return true;
            if (!(p.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu menu)) return false;
            int end = Math.min(9, menu.slots.size() - 1);
            for (int i = 1; i <= end; i++) {
                var slot = menu.slots.get(i);
                if (slot == null || !slot.hasItem()) continue;
                ItemStack stack = slot.getItem();
                if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack) && stack.is(check.targetItem)) {
                    QuestTracker.complete(p, check.questId);
                    return true;
                }
            }
            return false;
        });

        pendingItemLavaChecks.removeIf(check -> {
            ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, check.playerId);
            if (p == null || !isQuestActive(p)) return true;
            if (tick >= check.expireTick) return true;
            Entity entity = check.level.getEntity(check.itemEntityId);
            if (!(entity instanceof net.minecraft.world.entity.item.ItemEntity itemEntity)) {
                return true;
            }
            if (itemEntity.isInLava()) {
                QuestTracker.complete(p, check.questId);
                return true;
            }
            return false;
        });

        pendingShieldDisableChecks.removeIf(check -> {
            if (tick < check.checkTick) return false;
            ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, check.playerId);
            if (p != null) {
                if (isShieldOnCooldown(p)) {
                    QuestTracker.complete(p, "quest.have_your_shield_disabled");
                }
            }
            return true;
        });

        pendingLoveChecks.removeIf(check -> {
            if (tick < check.checkTick) return false;
            ServerLevel level = check.level;
            Entity entity = level.getEntity(check.entityId);
            if (entity != null && isInLoveAnimal(entity)) {
                loveOwnerByEntity.put(
                        check.entityId,
                        new LoveOwner(check.playerId, tick + LOVE_OWNER_TIMEOUT)
                );
                return true;
            }
            if (tick >= check.expireTick) {
                return true;
            }
            check.checkTick = tick + LOVE_CHECK_INTERVAL;
            return false;
        });
        loveOwnerByEntity.entrySet().removeIf(entry -> tick >= entry.getValue().expireTick);
        pendingProjectileKills.entrySet().removeIf(entry -> tick >= entry.getValue().expireTick);
        processSaplingGrowths(server, tick);
        saplingOwners.entrySet().removeIf(entry -> entry.getValue().expireTick <= tick);
    }

    private static void processSaplingGrowths(MinecraftServer server, int tick) {
        if (server == null || saplingOwners.isEmpty()) return;
        java.util.List<Map.Entry<BlockPos, SaplingOwner>> snapshot = new ArrayList<>(saplingOwners.entrySet());
        for (Map.Entry<BlockPos, SaplingOwner> entry : snapshot) {
            SaplingOwner owner = entry.getValue();
            if (owner == null || owner.expireTick <= tick) continue;
            ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, owner.playerId);
            if (p == null || !isQuestActive(p)) continue;
            net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p);
            if (!(baseLevel instanceof ServerLevel level)) continue;
            if (!level.dimension().equals(owner.dimension)) continue;
            BlockPos saplingPos = entry.getKey();
            if (!isTreeGrownAt(level, saplingPos)) continue;
            QuestTracker.increment(p, "quest.grow_20_trees", 20);
            saplingOwners.entrySet().removeIf(e -> e.getKey().closerThan(saplingPos, 3.0D));
        }
    }

    private static boolean isTreeGrownAt(ServerLevel level, BlockPos center) {
        if (level == null || center == null) return false;
        BlockState current = level.getBlockState(center);
        if (current.is(BlockTags.SAPLINGS)) return false;

        int logs = 0;
        int leaves = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = 0; dy <= 10; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos check = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(check);
                    if (state.is(BlockTags.LOGS)) logs++;
                    if (state.is(BlockTags.LEAVES)) leaves++;
                    if (logs >= 1 && leaves >= 2) return true;
                }
            }
        }
        return false;
    }

    private static void recordUniqueFood(ServerPlayer p, ItemStack stack) {
        Identifier id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack));
        if (id == null) return;

        Set<Identifier> set = uniqueFoods.computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), k -> new HashSet<>());
        if (set.add(id)) {
            int count = set.size();
            if (count >= 5) QuestTracker.complete(p, "quest.eat_5_unique_foods");
            if (count >= 10) QuestTracker.complete(p, "quest.eat_10_unique_foods");
            if (count >= 20) QuestTracker.complete(p, "quest.eat_20_unique_foods");
            BroadcastHelper.broadcastProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
        }
    }

    private static void recordUniqueHostileKill(ServerPlayer p, LivingEntity target) {
        Identifier id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (id == null) return;

        Set<Identifier> set = uniqueHostileKills.computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), k -> new HashSet<>());
        if (set.add(id)) {
            int count = set.size();
            if (count >= 7) QuestTracker.complete(p, "quest.kill_7_unique_hostile_mobs");
            if (count >= 10) QuestTracker.complete(p, "quest.kill_10_unique_hostile_mobs");
            if (count >= 15) QuestTracker.complete(p, "quest.kill_15_unique_hostile_mobs");
            BroadcastHelper.broadcastProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
        }
    }

    private static void recordUniqueBreed(ServerPlayer p, EntityType<?> type) {
        Identifier id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null || !UNIQUE_BREED_TYPES.contains(id)) return;

        Set<String> set = uniqueBreeds.computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), k -> new HashSet<>());
        if (set.add(id.toString())) {
            int count = set.size();
            if (count >= 4) QuestTracker.complete(p, "quest.breed_4_unique_animals");
            if (count >= 6) QuestTracker.complete(p, "quest.breed_6_unique_animals");
            if (count >= 8) QuestTracker.complete(p, "quest.breed_8_unique_animals");
            BroadcastHelper.broadcastProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
        }
    }

    private static void recordUniqueBowlFood(ServerPlayer p, Item item) {
        if (item == null) return;
        Set<Item> set = uniqueBowlFoods.computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), k -> new HashSet<>());
        if (set.add(item)) {
            if (set.size() >= 4) {
                QuestTracker.complete(p, "quest.eat_all_types_of_bowl_foods");
            }
            BroadcastHelper.broadcastProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
        }
    }

    private static void recordGolemBuild(ServerPlayer p, Identifier id) {
        if (id == null) return;
        Set<Identifier> set = uniqueGolemBuilds.computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), k -> new HashSet<>());
        if (set.add(id)) {
            if (set.containsAll(GOLEM_TYPES)) {
                QuestTracker.complete(p, "quest.build_all_golems");
            }
            BroadcastHelper.broadcastProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
        }
    }

    private static void recordGolemKill(ServerPlayer p, Identifier id) {
        if (id == null) return;
        Set<Identifier> set = uniqueGolemKills.computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), k -> new HashSet<>());
        if (set.add(id)) {
            if (set.containsAll(GOLEM_TYPES)) {
                QuestTracker.complete(p, "quest.kill_all_golems");
            }
            BroadcastHelper.broadcastProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
        }
    }

    private static void recordRaidKill(ServerPlayer p, Identifier id) {
        if (id == null) return;
        Set<Identifier> set = uniqueRaidMobKills.computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), k -> new HashSet<>());
        if (set.add(id)) {
            if (set.containsAll(RAID_MOBS)) {
                QuestTracker.complete(p, "quest.kill_all_raid_mobs");
            }
            BroadcastHelper.broadcastProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
        }
    }

    private static void recordDamageSource(ServerPlayer p, DamageSource source) {
        if (source == null) return;
        String id = source.getMsgId();
        if (id == null || id.isBlank()) return;
        StringBuilder key = new StringBuilder(id);
        Entity sourceEntity = com.jamie.jamiebingo.util.DamageSourceUtil.getEntity(source);
        if (sourceEntity != null) {
            Identifier sourceType = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(sourceEntity.getType());
            if (sourceType != null) {
                key.append("|src=").append(sourceType);
            }
        }
        Entity directEntity = com.jamie.jamiebingo.util.DamageSourceUtil.getDirectEntity(source);
        if (directEntity != null && directEntity != sourceEntity) {
            Identifier directType = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(directEntity.getType());
            if (directType != null) {
                key.append("|direct=").append(directType);
            }
        }
        Set<String> set = uniqueDamageSources.computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), k -> new HashSet<>());
        boolean changed = set.add(key.toString());
        int count = set.size();
        if (count >= 8) QuestTracker.complete(p, "quest.take_damage_from_8_unique_sources");
        if (count >= 12) QuestTracker.complete(p, "quest.take_damage_from_12_unique_sources");
        if (count >= 15) QuestTracker.complete(p, "quest.take_damage_from_15_unique_sources");
        if (changed) {
            BroadcastHelper.broadcastProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
        }
    }

    public static int getUniqueDamageSourceCount(UUID playerId) {
        return uniqueDamageSources.getOrDefault(playerId, Set.of()).size();
    }

    public static int getUniqueBannerPatternCount(ServerPlayer p) {
        return countUniqueItemsByNameContains(p, "pattern");
    }

    public static int getUniqueMusicDiscCount(ServerPlayer p) {
        return countUniqueItems(p, MUSIC_DISC_ITEMS);
    }

    public static int getUniquePressurePlateCount(ServerPlayer p) {
        return countUniqueItems(p, PRESSURE_PLATE_ITEMS);
    }

    public static int getUniqueBrickBlockCount(ServerPlayer p) {
        return countUniqueItemsByNameContains(p, "bricks");
    }

    public static int getUniqueBucketCount(ServerPlayer p) {
        return countUniqueItems(p, BUCKET_ITEMS);
    }

    public static int getUniqueWorkstationCount(ServerPlayer p) {
        return countUniqueItems(p, WORKSTATION_ITEMS);
    }

    public static int getUniqueLogCount(ServerPlayer p) {
        return countTaggedItems(p, ItemTags.LOGS);
    }

    private static void recordVariant(
            ServerPlayer p,
            Entity entity,
            ResourceKey<?> registryKey,
            Map<UUID, Set<Identifier>> map,
            String questId
    ) {
        Identifier variantId = getVariantId(entity);
        if (variantId == null) return;

        Set<Identifier> set = map.computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), k -> new HashSet<>());
        if (set.add(variantId)) {
            int total = getRegistrySize(p, registryKey, fallbackVariantCount(registryKey));
            if (total > 0 && set.size() >= total) {
                QuestTracker.complete(p, questId);
            }
            BroadcastHelper.broadcastProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
        }
    }

    private static int fallbackVariantCount(ResourceKey<?> registryKey) {
        if (registryKey == Registries.CHICKEN_VARIANT) return 3;
        if (registryKey == Registries.COW_VARIANT) return 3;
        if (registryKey == Registries.PIG_VARIANT) return 3;
        if (registryKey == Registries.WOLF_VARIANT) return 9;
        if (registryKey == Registries.CAT_VARIANT) return 11;
        return 3;
    }

    private static int getRegistrySize(ServerPlayer p, ResourceKey<?> registryKey, int fallback) {
        if (p == null || registryKey == null) return fallback;
        net.minecraft.core.RegistryAccess access = p.level().registryAccess();
        try {
            Method m = access.getClass().getMethod("registryOrThrow", ResourceKey.class);
            Object reg = m.invoke(access, registryKey);
            if (reg != null) {
                Method size = reg.getClass().getMethod("size");
                Object out = size.invoke(reg);
                if (out instanceof Integer i) return i;
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static Identifier getVariantId(Entity entity) {
        if (entity == null) return null;
        try {
            Method m = findMethodByNameAndArgCount(entity.getClass(), "getVariant", 0);
            if (m != null) {
                Object out = m.invoke(entity);
                if (out instanceof Holder<?> h) {
                    String name = holderName(h);
                    if (name != null) return id(name);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void recordNetherBiome(ServerPlayer p, Identifier biomeId) {
        if (!NETHER_BIOMES.contains(biomeId)) return;
        Set<Identifier> set = netherBiomes.computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), k -> new HashSet<>());
        if (set.add(biomeId)) {
            if (set.size() >= NETHER_BIOMES.size()) {
                QuestTracker.complete(p, "quest.visit_all_nether_biomes");
            }
            BroadcastHelper.broadcastProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
        }
    }

    private static void recordUniqueBiome(ServerPlayer p, Identifier biomeId) {
        Set<Identifier> set = uniqueBiomes.computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), k -> new HashSet<>());
        boolean changed = set.add(biomeId);
        int count = set.size();
        if (count >= 10) QuestTracker.complete(p, "quest.visit_10_unique_biomes");
        if (count >= 15) QuestTracker.complete(p, "quest.visit_15_unique_biomes");
        if (count >= 20) QuestTracker.complete(p, "quest.visit_20_unique_biomes");
        if (changed) {
            BroadcastHelper.broadcastProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
        }
    }

    private static void recordUniqueCaveBiome(ServerPlayer p, Identifier biomeId) {
        if (!CAVE_BIOMES.contains(biomeId)) return;
        Set<Identifier> set = uniqueCaveBiomes.computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), k -> new HashSet<>());
        if (set.add(biomeId)) {
            if (set.size() >= CAVE_BIOMES.size()) {
                QuestTracker.complete(p, "quest.visit_all_cave_biomes");
            }
            BroadcastHelper.broadcastProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
        }
    }

    private static void recordUniqueCraft(ServerPlayer p, ItemStack stack) {
        if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) return;
        Identifier id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return;

        Set<Identifier> set = uniqueCrafts.computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), k -> new HashSet<>());
        if (set.add(id)) {
            int count = set.size();
            if (count >= 25) QuestTracker.complete(p, "quest.craft_25_unique_items");
            if (count >= 50) QuestTracker.complete(p, "quest.craft_50_unique_items");
            if (count >= 75) QuestTracker.complete(p, "quest.craft_75_unique_items");
            BroadcastHelper.broadcastProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
        }
    }

    private static void recordAppliedArmorTrim(ServerPlayer p, ItemStack stack) {
        if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) return;
        Object trimObj = com.jamie.jamiebingo.util.ItemStackComponentUtil.get(stack, DataComponents.TRIM);
        if (!(trimObj instanceof ArmorTrim trim)) return;

        QuestTracker.complete(p, "quest.apply_armor_trim");

        String pattern = holderName(trim.pattern());
        String material = holderName(trim.material());
        String key = pattern + "|" + material;

        Set<String> set = uniqueArmorTrims.computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), k -> new HashSet<>());
        if (set.add(key)) {
            int count = set.size();
            if (count >= 3) QuestTracker.complete(p, "quest.apply_3_unique_armor_trims");
            if (count >= 5) QuestTracker.complete(p, "quest.apply_5_unique_armor_trims");
            if (count >= 7) QuestTracker.complete(p, "quest.apply_7_unique_armor_trims");
            BroadcastHelper.broadcastProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
        }
    }

    private static void recordUniqueSpyglassLook(ServerPlayer p, Identifier entityId) {
        if (entityId == null) return;
        Set<Identifier> set = uniqueSpyglassLooks.computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), k -> new HashSet<>());
        if (set.add(entityId)) {
            int count = set.size();
            if (count >= 5) QuestTracker.complete(p, "quest.look_at_5_unique_mobs_with_a_spyglass");
            if (count >= 10) QuestTracker.complete(p, "quest.look_at_10_unique_mobs_with_a_spyglass");
            if (count >= 15) QuestTracker.complete(p, "quest.look_at_15_unique_mobs_with_a_spyglass");
            if (count >= 20) QuestTracker.complete(p, "quest.look_at_20_unique_mobs_with_a_spyglass");
            BroadcastHelper.broadcastProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
        }
    }

    private static void recordArmorTrimTemplate(ServerPlayer p, ItemStack stack) {
        if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) return;
        Item item = stack.getItem();
        if (!(item instanceof net.minecraft.world.item.SmithingTemplateItem)) return;

        Identifier id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
        if (id == null) return;
        if ("minecraft:netherite_upgrade_smithing_template".equals(id.toString())) return;
        if (!id.getPath().contains("armor_trim")) return;

        Set<Identifier> set = uniqueArmorTrimTemplates.computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), k -> new HashSet<>());
        if (set.add(id)) {
            int count = set.size();
            if (count >= 2) QuestTracker.complete(p, "quest.obtain_2_unique_armor_trims");
            BroadcastHelper.broadcastProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
        }
    }

    public static int getUniqueFoodCount(UUID playerId) {
        return uniqueFoods.getOrDefault(playerId, Set.of()).size();
    }

    public static int getUniqueHostileKillCount(UUID playerId) {
        return uniqueHostileKills.getOrDefault(playerId, Set.of()).size();
    }

    public static int getUniqueBreedCount(UUID playerId) {
        return uniqueBreeds.getOrDefault(playerId, Set.of()).size();
    }

    public static int getNetherBiomeCount(UUID playerId) {
        return netherBiomes.getOrDefault(playerId, Set.of()).size();
    }

    public static int getUniqueCraftCount(UUID playerId) {
        return uniqueCrafts.getOrDefault(playerId, Set.of()).size();
    }

    public static int getUniqueArmorTrimCount(UUID playerId) {
        return uniqueArmorTrims.getOrDefault(playerId, Set.of()).size();
    }

    public static int getUniqueSpyglassLookCount(UUID playerId) {
        return uniqueSpyglassLooks.getOrDefault(playerId, Set.of()).size();
    }

    public static int getUniqueArmorTrimTemplateCount(UUID playerId) {
        return uniqueArmorTrimTemplates.getOrDefault(playerId, Set.of()).size();
    }

    public static int getUniqueBiomeCount(UUID playerId) {
        return uniqueBiomes.getOrDefault(playerId, Set.of()).size();
    }

    public static int getUniqueCaveBiomeCount(UUID playerId) {
        return uniqueCaveBiomes.getOrDefault(playerId, Set.of()).size();
    }

    public static Integer getTeamProgressValue(MinecraftServer server, UUID teamId, String questId) {
        if (server == null || teamId == null || questId == null) return null;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.teamSyncEnabled) return null;
        return switch (questId) {
            case "quest.obtain_6_log_variants", "quest.obtain_8_log_variants" ->
                    countTaggedItemsForTeam(server, teamId, ItemTags.LOGS);
            case "quest.obtain_6_unique_buckets" ->
                    countUniqueItemsForTeam(server, teamId, BUCKET_ITEMS);
            case "quest.obtain_6_unique_brick_blocks" ->
                    countUniqueItemsByNameContainsForTeam(server, teamId, "bricks");
            case "quest.obtain_5_unique_pressure_plates" ->
                    countUniqueItemsForTeam(server, teamId, PRESSURE_PLATE_ITEMS);
            case "quest.obtain_3_unique_music_disks" ->
                    countUniqueItemsForTeam(server, teamId, MUSIC_DISC_ITEMS);
            case "quest.obtain_7_unique_workstations" ->
                    countUniqueItemsForTeam(server, teamId, WORKSTATION_ITEMS);
            case "quest.obtain_5_types_of_saplings" ->
                    countTaggedItemsForTeam(server, teamId, ItemTags.SAPLINGS);
            case "quest.obtain_6_unique_flowers" ->
                    countTaggedItemsForTeam(server, teamId, ItemTags.FLOWERS);
            case "quest.sprint_1km" ->
                    sumTeamProgress(server, teamId, "quest.sprint_1km");
            case "quest.crouch_for_100m" ->
                    sumTeamProgress(server, teamId, "quest.crouch_for_100m") / 100;
            case "quest.swim_for_500m" ->
                    sumTeamProgress(server, teamId, "quest.swim_for_500m") / 100;
            case "quest.boat_for_2km" ->
                    sumTeamProgress(server, teamId, "quest.boat_for_2km") / 100;
            case "quest.eat_5_unique_foods", "quest.eat_10_unique_foods", "quest.eat_20_unique_foods" ->
                    unionSizeForTeam(server, teamId, uniqueFoods);
            case "quest.kill_7_unique_hostile_mobs", "quest.kill_10_unique_hostile_mobs", "quest.kill_15_unique_hostile_mobs" ->
                    unionSizeForTeam(server, teamId, uniqueHostileKills);
            case "quest.breed_4_unique_animals", "quest.breed_6_unique_animals", "quest.breed_8_unique_animals" ->
                    unionSizeForTeam(server, teamId, uniqueBreeds);
            case "quest.visit_all_nether_biomes" ->
                    unionSizeForTeam(server, teamId, netherBiomes);
            case "quest.visit_10_unique_biomes", "quest.visit_15_unique_biomes", "quest.visit_20_unique_biomes" ->
                    unionSizeForTeam(server, teamId, uniqueBiomes);
            case "quest.visit_all_cave_biomes" ->
                    unionSizeForTeam(server, teamId, uniqueCaveBiomes);
            case "quest.get_15_advancements", "quest.get_25_advancements", "quest.get_35_advancements" ->
                    countTeamUniqueAdvancements(server, teamId);
            case "quest.craft_25_unique_items", "quest.craft_50_unique_items", "quest.craft_75_unique_items" ->
                    unionSizeForTeam(server, teamId, uniqueCrafts);
            case "quest.apply_3_unique_armor_trims", "quest.apply_5_unique_armor_trims", "quest.apply_7_unique_armor_trims" ->
                    unionSizeForTeam(server, teamId, uniqueArmorTrims);
            case "quest.look_at_5_unique_mobs_with_a_spyglass", "quest.look_at_10_unique_mobs_with_a_spyglass",
                    "quest.look_at_15_unique_mobs_with_a_spyglass", "quest.look_at_20_unique_mobs_with_a_spyglass" ->
                    unionSizeForTeam(server, teamId, uniqueSpyglassLooks);
            case "quest.obtain_2_unique_armor_trims" ->
                    unionSizeForTeam(server, teamId, uniqueArmorTrimTemplates);
            case "quest.take_damage_from_8_unique_sources", "quest.take_damage_from_12_unique_sources", "quest.take_damage_from_15_unique_sources" ->
                    unionSizeForTeam(server, teamId, uniqueDamageSources);
            default -> null;
        };
    }

    private static <T> int unionSizeForTeam(MinecraftServer server, UUID teamId, Map<UUID, Set<T>> map) {
        TeamData.TeamInfo team = getTeamInfo(server, teamId);
        if (team == null) return 0;
        Set<T> merged = new HashSet<>();
        for (UUID memberId : team.members) {
            merged.addAll(map.getOrDefault(memberId, Set.of()));
        }
        return merged.size();
    }

    private static int sumTeamProgress(MinecraftServer server, UUID teamId, String questId) {
        TeamData.TeamInfo team = getTeamInfo(server, teamId);
        if (team == null) return 0;
        int total = 0;
        for (UUID memberId : team.members) {
            ServerPlayer member = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
            if (member == null) continue;
            total += QuestTracker.getProgress(member, questId);
        }
        return total;
    }

    private static int countTeamUniqueAdvancements(MinecraftServer server, UUID teamId) {
        TeamData.TeamInfo team = getTeamInfo(server, teamId);
        if (team == null) return 0;
        Set<Identifier> merged = new HashSet<>();
        for (UUID memberId : team.members) {
            merged.addAll(advancementEarnedIds.getOrDefault(memberId, Set.of()));
        }
        return merged.size();
    }

    private static int countTaggedItemsForTeam(MinecraftServer server, UUID teamId, TagKey<Item> tag) {
        TeamData.TeamInfo team = getTeamInfo(server, teamId);
        if (team == null) return 0;
        Set<Item> unique = new HashSet<>();
        for (UUID memberId : team.members) {
            ServerPlayer member = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
            if (member == null) continue;
            for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(member))) {
                if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack) && stack.is(tag)) {
                    unique.add(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack));
                }
            }
            for (ItemStack stack : getEquipmentStacks(member)) {
                if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack) && stack.is(tag)) {
                    unique.add(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack));
                }
            }
        }
        return unique.size();
    }

    private static int countUniqueItemsForTeam(MinecraftServer server, UUID teamId, Set<Item> items) {
        TeamData.TeamInfo team = getTeamInfo(server, teamId);
        if (team == null) return 0;
        Set<Item> unique = new HashSet<>();
        for (UUID memberId : team.members) {
            ServerPlayer member = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
            if (member == null) continue;
            for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(member))) {
                if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack) && items.contains(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack))) {
                    unique.add(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack));
                }
            }
            for (ItemStack stack : getEquipmentStacks(member)) {
                if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack) && items.contains(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack))) {
                    unique.add(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack));
                }
            }
        }
        return unique.size();
    }

    private static int countUniqueItemsByNameContainsForTeam(MinecraftServer server, UUID teamId, String token) {
        TeamData.TeamInfo team = getTeamInfo(server, teamId);
        if (team == null) return 0;
        Set<Item> unique = new HashSet<>();
        for (UUID memberId : team.members) {
            ServerPlayer member = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
            if (member == null) continue;
            for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(member))) {
                if (stack == null) continue;
                if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) continue;
                Identifier id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack));
                if (id != null && id.getPath().contains(token)) {
                    unique.add(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack));
                }
            }
            for (ItemStack stack : getEquipmentStacks(member)) {
                if (stack == null) continue;
                if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) continue;
                Identifier id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack));
                if (id != null && id.getPath().contains(token)) {
                    unique.add(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack));
                }
            }
        }
        return unique.size();
    }

    private static TeamData.TeamInfo getTeamInfo(MinecraftServer server, UUID teamId) {
        if (server == null || teamId == null) return null;
        TeamData teamData = TeamData.get(server);
        return teamData.getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);
    }

    public static int getCrouchDistance(UUID playerId) {
        return crouchDistanceCm.getOrDefault(playerId, 0);
    }

    public static int getSwimDistance(UUID playerId) {
        return swimDistanceCm.getOrDefault(playerId, 0);
    }

    public static int getBoatDistance(UUID playerId) {
        return boatDistanceCm.getOrDefault(playerId, 0);
    }

    public static int getBabyMobKillCount(UUID playerId) {
        return babyMobKills.getOrDefault(playerId, 0);
    }

    public static int getAdvancementsEarned(ServerPlayer p) {
        if (p == null) return 0;
        return advancementEarnedCounts.getOrDefault(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), 0);
    }

    private static String holderName(Holder<?> holder) {
        if (holder == null) return "unknown";
        String name = holder.getRegisteredName();
        if (name != null) return name;
        return holder.unwrapKey().map(Object::toString).orElse(holder.toString());
    }

    private static void updateDistanceProgress(
            ServerPlayer p,
            Map<UUID, Vec3> lastPosMap,
            Map<UUID, Integer> distanceMap,
            String questId,
            int max
    ) {
        UUID id = com.jamie.jamiebingo.util.EntityUtil.getUUID(p);
        Vec3 last = lastPosMap.get(id);
        Vec3 now = p.position();
        if (last != null) {
            double dist = now.distanceTo(last);
            int add = (int) Math.round(dist * 100);
            if (add > 0) {
                int current = distanceMap.getOrDefault(id, 0);
                int next = Math.min(max, current + add);
                distanceMap.put(id, next);
                QuestTracker.addProgress(p, questId, add, max);
            }
        }
        lastPosMap.put(id, now);
    }

    private static boolean isArmorSet(
            ItemStack head,
            ItemStack chest,
            ItemStack legs,
            ItemStack feet,
            Item headItem,
            Item chestItem,
            Item legItem,
            Item feetItem
    ) {
        return head.is(headItem) && chest.is(chestItem) && legs.is(legItem) && feet.is(feetItem);
    }

    private static boolean isChainOrNetheritePiece(
            ItemStack head,
            ItemStack chest,
            ItemStack legs,
            ItemStack feet,
            boolean chain
    ) {
        if (chain) {
            return head.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:chainmail_helmet"))
                    || chest.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:chainmail_chestplate"))
                    || legs.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:chainmail_leggings"))
                    || feet.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:chainmail_boots"));
        }
        return head.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_helmet"))
                || chest.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_chestplate"))
                || legs.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_leggings"))
                || feet.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_boots"));
    }

    private static void checkColoredLeatherArmor(ServerPlayer p, ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet) {
        checkLeatherPiece(p, head, EquipmentSlot.HEAD);
        checkLeatherPiece(p, chest, EquipmentSlot.CHEST);
        checkLeatherPiece(p, legs, EquipmentSlot.LEGS);
        checkLeatherPiece(p, feet, EquipmentSlot.FEET);

        if (isDyedLeather(head) && isDyedLeather(chest) && isDyedLeather(legs) && isDyedLeather(feet)) {
            int c1 = DyedItemColor.getOrDefault(head, DyedItemColor.LEATHER_COLOR);
            int c2 = DyedItemColor.getOrDefault(chest, DyedItemColor.LEATHER_COLOR);
            int c3 = DyedItemColor.getOrDefault(legs, DyedItemColor.LEATHER_COLOR);
            int c4 = DyedItemColor.getOrDefault(feet, DyedItemColor.LEATHER_COLOR);
            Set<Integer> colors = Set.of(c1, c2, c3, c4);
            if (colors.size() == 4) {
                QuestTracker.complete(p, "quest.wear_full_uniquely_coloured_leather_armor");
            }
        }

        if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(head)
                && !com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(chest)
                && !com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(legs)
                && !com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(feet)) {
            String m1 = armorMaterialKey(head);
            String m2 = armorMaterialKey(chest);
            String m3 = armorMaterialKey(legs);
            String m4 = armorMaterialKey(feet);
            if (m1 != null && m2 != null && m3 != null && m4 != null) {
                Set<String> unique = new HashSet<>();
                unique.add(m1);
                unique.add(m2);
                unique.add(m3);
                unique.add(m4);
                if (unique.size() == 4) {
                    QuestTracker.complete(p, "quest.wear_four_unique_armor_pieces");
                }
            }
        }
    }

    private static boolean isDyedLeather(ItemStack stack) {
        if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) return false;
        if (!stack.is(ItemTags.DYEABLE)) return false;
        return com.jamie.jamiebingo.util.ItemStackComponentUtil.has(stack, DataComponents.DYED_COLOR);
    }

    private static void checkLeatherPiece(ServerPlayer p, ItemStack stack, EquipmentSlot slot) {
        if (!stack.is(ItemTags.DYEABLE)) return;
        int color = DyedItemColor.getOrDefault(stack, DyedItemColor.LEATHER_COLOR);

        for (var entry : COLOR_NAME_BY_RGB().entrySet()) {
            if (entry.getKey() != color) continue;

            String colorName = entry.getValue();
            String slotName = switch (slot) {
                case HEAD -> "helmet";
                case CHEST -> "chestplate";
                case LEGS -> "leggings";
                case FEET -> "boots";
                default -> "";
            };

            if (!slotName.isBlank()) {
                QuestTracker.complete(p, "quest.wear_" + colorName + "_colored_leather_" + slotName);
            }
            return;
        }
    }

    private static void checkTeamTrimmedArmor(ServerPlayer p) {
        if (p == null) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(p);
        if (server == null) return;
        UUID teamId = getTeamId(p);
        if (teamId == null) return;

        TeamData teamData = TeamData.get(server);
        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);
        if (team == null || team.members.isEmpty()) return;

        for (UUID memberId : team.members) {
            ServerPlayer member = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
            if (member == null) return;
            if (!hasTrimmedArmorPiece(member)) {
                return;
            }
        }

        completeForTeam(server, teamId, "quest.whole_team_wear_a_piece_of_trimmed_armor");
    }

    private static void checkWholeTeamQuests(MinecraftServer server) {
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isActive() || data.startCountdownActive || data.pregameBoxActive) return;

        TeamData teamData = TeamData.get(server);
        for (TeamData.TeamInfo team : teamData.getTeams()) {
            if (team.members.isEmpty()) continue;

            java.util.List<ServerPlayer> members = new java.util.ArrayList<>();
            boolean allActive = true;
            for (UUID memberId : team.members) {
                ServerPlayer member = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
                if (member == null) continue;
                if (!isQuestActive(member)) {
                    allActive = false;
                    break;
                }
                members.add(member);
            }
            if (!allActive || members.isEmpty()) continue;

            UUID teamId = team.id;

            if (hasQuest(server, "quest.whole_team_swims_at_the_same_time")) {
                if (allMatch(members, ServerPlayer::isInWater)) {
                    completeForTeam(server, teamId, "quest.whole_team_swims_at_the_same_time");
                }
            }

            if (hasQuest(server, "quest.whole_team_boating_at_the_same_time")) {
                if (allMatch(members, p -> p.getVehicle() instanceof net.minecraft.world.entity.vehicle.boat.AbstractBoat)) {
                    completeForTeam(server, teamId, "quest.whole_team_boating_at_the_same_time");
                }
            }

            if (hasQuest(server, "quest.whole_team_rides_a_mob_at_the_same_time")) {
                if (allMatch(members, p -> {
                    var vehicle = p.getVehicle();
                    return vehicle instanceof net.minecraft.world.entity.LivingEntity
                            && !(vehicle instanceof net.minecraft.world.entity.vehicle.boat.AbstractBoat);
                })) {
                    completeForTeam(server, teamId, "quest.whole_team_rides_a_mob_at_the_same_time");
                }
            }

            if (hasQuest(server, "quest.whole_team_in_nether")) {
                if (allMatch(members, p -> com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p).dimension() == Level.NETHER)) {
                    completeForTeam(server, teamId, "quest.whole_team_in_nether");
                }
            }

            if (hasQuest(server, "quest.whole_team_in_the_end")) {
                if (allMatch(members, p -> com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p).dimension() == Level.END)) {
                    completeForTeam(server, teamId, "quest.whole_team_in_the_end");
                }
            }

            if (hasQuest(server, "quest.whole_team_has_a_status_effect_at_the_same_time")) {
                if (allMatch(members, p -> !p.getActiveEffects().isEmpty())) {
                    completeForTeam(server, teamId, "quest.whole_team_has_a_status_effect_at_the_same_time");
                }
            }

            if (hasQuest(server, "quest.whole_team_starving")) {
                if (allMatch(members, p -> {
                    net.minecraft.world.food.FoodData food = com.jamie.jamiebingo.util.PlayerFoodUtil.getFoodData(p);
                    return food != null && com.jamie.jamiebingo.util.FoodDataUtil.getFoodLevel(food) <= 0;
                })) {
                    completeForTeam(server, teamId, "quest.whole_team_starving");
                }
            }

            if (hasQuest(server, "quest.whole_team_have_consecutive_number_of_levels_to_each_other")) {
                if (hasConsecutiveLevels(members)) {
                    completeForTeam(server, teamId, "quest.whole_team_have_consecutive_number_of_levels_to_each_other");
                }
            }

            if (hasQuest(server, "quest.whole_team_wear_a_piece_of_enchanted_armor")) {
                if (allMatch(members, QuestEvents::hasEnchantedArmorPiece)) {
                    completeForTeam(server, teamId, "quest.whole_team_wear_a_piece_of_enchanted_armor");
                }
            }

            if (hasQuest(server, "quest.whole_team_wear_a_piece_of_trimmed_armor")) {
                if (allMatch(members, QuestEvents::hasTrimmedArmorPiece)) {
                    completeForTeam(server, teamId, "quest.whole_team_wear_a_piece_of_trimmed_armor");
                }
            }

            if (hasQuest(server, "quest.whole_team_wears_a_carved_pumpkin_at_the_same_time")) {
                if (allMatch(members, p -> {
                    ItemStack head = p.getItemBySlot(EquipmentSlot.HEAD);
                    return head.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:carved_pumpkin"));
                })) {
                    completeForTeam(server, teamId, "quest.whole_team_wears_a_carved_pumpkin_at_the_same_time");
                }
            }

            if (hasQuest(server, "quest.whole_team_throws_an_enderpearl_at_the_same_time")) {
                if (allWithinTickWindow(members, lastEnderPearlTick, server)) {
                    completeForTeam(server, teamId, "quest.whole_team_throws_an_enderpearl_at_the_same_time");
                }
            }

            if (hasQuest(server, "quest.whole_team_eats_cake_at_the_same_time")) {
                if (allWithinTickWindow(members, lastCakeEatTick, server)) {
                    completeForTeam(server, teamId, "quest.whole_team_eats_cake_at_the_same_time");
                }
            }

            if (hasQuest(server, "quest.whole_team_dies_at_the_same_time")) {
                if (allWithinTickWindow(members, lastDeathTick, server)) {
                    completeForTeam(server, teamId, "quest.whole_team_dies_at_the_same_time");
                }
            }

            if (hasQuest(server, "quest.whole_team_breeds_an_animal_at_the_same_time")) {
                if (allWithinTickWindow(members, lastBreedTick, server)) {
                    completeForTeam(server, teamId, "quest.whole_team_breeds_an_animal_at_the_same_time");
                }
            }

            if (hasQuest(server, "quest.whole_team_has_a_tamed_wolf")) {
                if (allMatch(members, m -> hasLivingTamedOwnedBy(server, m, EntityType.WOLF))) {
                    completeForTeam(server, teamId, "quest.whole_team_has_a_tamed_wolf");
                }
            }

            if (hasQuest(server, "quest.whole_team_has_a_tamed_cat")) {
                if (allMatch(members, m -> hasLivingTamedOwnedBy(server, m, EntityType.CAT))) {
                    completeForTeam(server, teamId, "quest.whole_team_has_a_tamed_cat");
                }
            }

            if (hasQuest(server, "quest.whole_team_stands_on_the_same_happy_ghast")) {
                if (allOnSameHappyGhast(members)) {
                    completeForTeam(server, teamId, "quest.whole_team_stands_on_the_same_happy_ghast");
                }
            }

            if (hasQuest(server, "quest.each_team_member_looks_at_a_different_member_of_their_team")) {
                if (eachMemberLooksAtDifferentTeammate(server, members)) {
                    completeForTeam(server, teamId, "quest.each_team_member_looks_at_a_different_member_of_their_team");
                }
            }
        }
    }

    private static boolean allMatch(java.util.List<ServerPlayer> members, java.util.function.Predicate<ServerPlayer> predicate) {
        for (ServerPlayer member : members) {
            if (!predicate.test(member)) return false;
        }
        return true;
    }

    private static boolean allWithinTickWindow(
            java.util.List<ServerPlayer> members,
            java.util.Map<UUID, Integer> lastTickMap,
            MinecraftServer server
    ) {
        int now = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        for (ServerPlayer member : members) {
            Integer last = lastTickMap.get(com.jamie.jamiebingo.util.EntityUtil.getUUID(member));
            if (last == null) return false;
            if (now - last > WHOLE_TEAM_SAME_TIME_TICKS) return false;
        }
        return true;
    }

    private static boolean hasConsecutiveLevels(java.util.List<ServerPlayer> members) {
        int size = members.size();
        if (size <= 1) return true;
        int[] levels = new int[size];
        for (int i = 0; i < size; i++) {
            levels[i] = com.jamie.jamiebingo.util.PlayerExperienceUtil.getExperienceLevel(members.get(i));
        }
        java.util.Arrays.sort(levels);
        for (int i = 1; i < levels.length; i++) {
            if (levels[i] - levels[i - 1] != 1) {
                return false;
            }
        }
        return true;
    }

    private static boolean allOnSameHappyGhast(java.util.List<ServerPlayer> members) {
        if (members == null || members.isEmpty()) return false;
        Entity first = getSupportedHappyGhast(members.get(0));
        if (first == null) return false;
        for (int i = 1; i < members.size(); i++) {
            Entity supported = getSupportedHappyGhast(members.get(i));
            if (supported == null || supported != first) return false;
        }
        return true;
    }

    private static Entity rootVehicle(Entity entity) {
        if (entity == null) return null;
        Entity root = entity;
        while (root.getVehicle() != null) {
            root = root.getVehicle();
        }
        return root;
    }

    private static boolean eachMemberLooksAtDifferentTeammate(MinecraftServer server, java.util.List<ServerPlayer> members) {
        if (server == null || members == null || members.size() < 2) return false;
        int now = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        Set<UUID> lookedAt = new HashSet<>();
        for (ServerPlayer viewer : members) {
            if (viewer == null) return false;
            UUID viewerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(viewer);
            ServerPlayer freshTarget = findLookedAtTeammate(viewer, members);
            if (freshTarget != null) {
                lastLookedTeammateByViewer.put(viewerId, com.jamie.jamiebingo.util.EntityUtil.getUUID(freshTarget));
                lastLookedTeammateTick.put(viewerId, now);
            }

            Integer seenTick = lastLookedTeammateTick.get(viewerId);
            UUID targetId = lastLookedTeammateByViewer.get(viewerId);
            if (seenTick == null || targetId == null) return false;
            if (now - seenTick > LOOK_DIFFERENT_MEMBER_WINDOW_TICKS) return false;
            if (targetId.equals(viewerId)) return false;
            boolean validTeammate = false;
            for (ServerPlayer member : members) {
                if (member == null || member == viewer) continue;
                if (targetId.equals(com.jamie.jamiebingo.util.EntityUtil.getUUID(member))) {
                    validTeammate = true;
                    break;
                }
            }
            if (!validTeammate) return false;
            if (!lookedAt.add(targetId)) return false;
        }
        return lookedAt.size() == members.size();
    }

    private static ServerPlayer findLookedAtTeammate(ServerPlayer viewer, java.util.List<ServerPlayer> members) {
        if (viewer == null || members == null) return null;
        Vec3 eye = viewer.getEyePosition();
        Vec3 look = viewer.getViewVector(1.0F).normalize();
        Vec3 end = eye.add(look.scale(32.0D));
        ServerPlayer best = null;
        double bestDist = Double.MAX_VALUE;

        for (ServerPlayer candidate : members) {
            if (candidate == null || candidate == viewer) continue;
            AABB candidateBox = candidate.getBoundingBox().inflate(0.4D);
            java.util.Optional<Vec3> clipped = candidateBox.clip(eye, end);
            if (clipped.isPresent()) {
                if (!viewer.hasLineOfSight(candidate)) continue;
                double distSqr = eye.distanceToSqr(clipped.get());
                if (distSqr < bestDist) {
                    best = candidate;
                    bestDist = distSqr;
                }
                continue;
            }
            Vec3 targetEye = candidate.getEyePosition();
            Vec3 to = targetEye.subtract(eye);
            double dist = to.length();
            if (dist < 0.001D || dist > 32.0D) continue;
            Vec3 dir = to.normalize();
            double dot = dir.dot(look);
            if (dot < 0.96D) continue;
            net.minecraft.world.level.ClipContext ctx = new net.minecraft.world.level.ClipContext(
                    eye,
                    targetEye,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    viewer
            );
            BlockHitResult hit = viewer.level().clip(ctx);
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) continue;
            if (!viewer.hasLineOfSight(candidate)) continue;
            if (dist < bestDist) {
                best = candidate;
                bestDist = dist;
            }
        }

        return best;
    }

    private static boolean hasLivingTamedOwnedBy(MinecraftServer server, ServerPlayer owner, EntityType<?> type) {
        if (server == null || owner == null || type == null) return false;
        ServerLevel level = (ServerLevel) com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(owner);
        if (level == null) return false;
        AABB area = owner.getBoundingBox().inflate(96.0D, 48.0D, 96.0D);
        List<Entity> entities = level.getEntities(
                (Entity) null,
                area,
                e -> e != null && e.getType() == type && e.isAlive()
        );
        for (Entity entity : entities) {
            if (!isTamedEntity(entity)) continue;
            ServerPlayer ownerPlayer = getOwnerPlayer(entity, server);
            if (ownerPlayer == null) continue;
            if (com.jamie.jamiebingo.util.EntityUtil.getUUID(ownerPlayer)
                    .equals(com.jamie.jamiebingo.util.EntityUtil.getUUID(owner))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasEnchantedArmorPiece(ServerPlayer p) {
        ItemStack head = p.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = p.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = p.getItemBySlot(EquipmentSlot.FEET);
        return isEnchantedArmor(head) || isEnchantedArmor(chest) || isEnchantedArmor(legs) || isEnchantedArmor(feet);
    }

    private static boolean isEnchantedArmor(ItemStack stack) {
        if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) return false;
        if (!isArmorItem(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack))) return false;
        return stack.isEnchanted();
    }

    private static boolean hasTrimmedArmorPiece(ServerPlayer p) {
        ItemStack head = p.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = p.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = p.getItemBySlot(EquipmentSlot.FEET);
        return hasTrim(head) || hasTrim(chest) || hasTrim(legs) || hasTrim(feet);
    }

    private static boolean hasTrim(ItemStack stack) {
        if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) return false;
        Object trim = com.jamie.jamiebingo.util.ItemStackComponentUtil.get(stack, DataComponents.TRIM);
        return trim instanceof ArmorTrim;
    }

    private static Map<Integer, String> COLOR_NAME_BY_RGB() {
        Map<Integer, String> map = new HashMap<>();
        for (net.minecraft.world.item.DyeColor color : net.minecraft.world.item.DyeColor.values()) {
            int packed = color.getTextureDiffuseColor();
            map.put(packed, color.getName());
        }
        return map;
    }

    private static boolean hasFullToolSet(ServerPlayer p, Item... items) {
        for (Item item : items) {
            if (!hasItem(p, item)) return false;
        }
        return true;
    }

    private static boolean hasItemSet(ServerPlayer p, Set<Item> items) {
        for (Item item : items) {
            if (!hasItem(p, item)) return false;
        }
        return true;
    }

    private static ItemStack[] getEquipmentStacks(ServerPlayer p) {
        return new ItemStack[] {
                com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(p, EquipmentSlot.HEAD),
                com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(p, EquipmentSlot.CHEST),
                com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(p, EquipmentSlot.LEGS),
                com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(p, EquipmentSlot.FEET),
                com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(p, EquipmentSlot.OFFHAND)
        };
    }

    private static Holder<Potion> getPotionHolder(ItemStack stack) {
        Object contentsObj = com.jamie.jamiebingo.util.ItemStackComponentUtil.get(stack, DataComponents.POTION_CONTENTS);
        if (!(contentsObj instanceof PotionContents contents)) return null;
        return contents.potion().orElse(null);
    }

    private static boolean hasItem(ServerPlayer p, Item item) {
        if (item == null || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:air")) return false;
        for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(p))) {
            if (stack.is(item)) return true;
        }
        for (ItemStack stack : getEquipmentStacks(p)) {
            if (stack.is(item)) return true;
        }
        return false;
    }

    private static List<ItemStack> getUniqueInventoryStacks(ServerPlayer p) {
        List<ItemStack> out = new ArrayList<>();
        IdentityHashMap<ItemStack, Boolean> seen = new IdentityHashMap<>();
        for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(p))) {
            if (stack == null) continue;
            if (seen.put(stack, Boolean.TRUE) == null) {
                out.add(stack);
            }
        }
        for (ItemStack stack : getEquipmentStacks(p)) {
            if (stack == null) continue;
            if (seen.put(stack, Boolean.TRUE) == null) {
                out.add(stack);
            }
        }
        return out;
    }

    private static int countItem(ServerPlayer p, Item item) {
        int total = 0;
        for (ItemStack stack : getUniqueInventoryStacks(p)) {
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static int countTaggedItems(ServerPlayer p, net.minecraft.tags.TagKey<Item> tag) {
        Set<Item> unique = new HashSet<>();
        for (ItemStack stack : getUniqueInventoryStacks(p)) {
            if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack) && stack.is(tag)) {
                unique.add(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack));
            }
        }
        return unique.size();
    }

    private static void checkPotionInventory(ServerPlayer p) {
        for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(p))) {
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) continue;
            if (!stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:potion")) && !stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:splash_potion")) && !stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:lingering_potion"))) {
                continue;
            }

            Holder<Potion> potion = getPotionHolder(stack);
            if (potion != null && potion.is(Potions.WATER_BREATHING)) {
                QuestTracker.complete(p, "quest.obtain_a_potion_of_water_breathing");
            }
            if (potion != null && potion.is(Potions.HEALING)) {
                QuestTracker.complete(p, "quest.obtain_a_potion_of_healing");
            }
            if (potion != null && potion.is(Potions.INVISIBILITY)) {
                QuestTracker.complete(p, "quest.obtain_a_potion_of_invisibility");
            }
            if (potion != null && potion.is(Potions.SWIFTNESS)) {
                QuestTracker.complete(p, "quest.obtain_a_potion_of_swiftness");
            }
            if (potion != null && potion.is(Potions.LEAPING)) {
                QuestTracker.complete(p, "quest.obtain_a_potion_of_leaping");
            }
            if (stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:lingering_potion"))) {
                QuestTracker.complete(p, "quest.obtain_a_lingering_potion");
            }
        }
    }
    private static void checkAnyStack64(ServerPlayer p) {
        Map<Item, Integer> counts = new HashMap<>();
        for (ItemStack stack : getUniqueInventoryStacks(p)) {
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) continue;
            counts.merge(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack), stack.getCount(), Integer::sum);
        }
        for (int total : counts.values()) {
            if (total >= 64) {
                QuestTracker.complete(p, "quest.obtain_64_of_any_one_item_block");
                return;
            }
        }
    }

    private static void checkColor64(ServerPlayer p) {
        for (String color : COLOR_NAMES) {
            checkItemCount(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:" + color + "_wool"),
                    "quest.obtain_64_" + color + "_wool");
            checkItemCount(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:" + color + "_concrete"),
                    "quest.obtain_64_" + color + "_concrete");
            checkItemCount(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:" + color + "_terracotta"),
                    "quest.obtain_64_" + color + "_terracotta");
            checkItemCount(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:" + color + "_glazed_terracotta"),
                    "quest.obtain_64_" + color + "_glazed_terracotta");
            checkItemCount(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:" + color + "_stained_glass"),
                    "quest.obtain_64_" + color + "_glass");
        }
    }

    private static void checkUniqueCollections(ServerPlayer p) {
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(p);
        if (server != null && BingoGameData.get(server).teamSyncEnabled) {
            UUID teamId = TeamData.get(server).getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
            if (teamId != null) {
                int banner = countUniqueItemsByNameContainsForTeam(server, teamId, "pattern");
                if (banner >= 3) QuestTracker.complete(p, "quest.obtain_3_unique_banner_patterns");

                int discs = countUniqueItemsForTeam(server, teamId, MUSIC_DISC_ITEMS);
                if (discs >= 3) QuestTracker.complete(p, "quest.obtain_3_unique_music_disks");

                int plates = countUniqueItemsForTeam(server, teamId, PRESSURE_PLATE_ITEMS);
                if (plates >= 5) QuestTracker.complete(p, "quest.obtain_5_unique_pressure_plates");

                int bricks = countUniqueItemsByNameContainsForTeam(server, teamId, "bricks");
                if (bricks >= 6) QuestTracker.complete(p, "quest.obtain_6_unique_brick_blocks");

                int buckets = countUniqueItemsForTeam(server, teamId, BUCKET_ITEMS);
                if (buckets >= 6) QuestTracker.complete(p, "quest.obtain_6_unique_buckets");

                int workstations = countUniqueItemsForTeam(server, teamId, WORKSTATION_ITEMS);
                if (workstations >= 7) QuestTracker.complete(p, "quest.obtain_7_unique_workstations");

                int logs = countTaggedItemsForTeam(server, teamId, ItemTags.LOGS);
                if (logs >= 6) QuestTracker.complete(p, "quest.obtain_6_log_variants");
                if (logs >= 8) QuestTracker.complete(p, "quest.obtain_8_log_variants");
                return;
            }
        }

        int banner = countUniqueItemsByNameContains(p, "pattern");
        if (banner >= 3) QuestTracker.complete(p, "quest.obtain_3_unique_banner_patterns");

        int discs = countUniqueItems(p, MUSIC_DISC_ITEMS);
        if (discs >= 3) QuestTracker.complete(p, "quest.obtain_3_unique_music_disks");

        int plates = countUniqueItems(p, PRESSURE_PLATE_ITEMS);
        if (plates >= 5) QuestTracker.complete(p, "quest.obtain_5_unique_pressure_plates");

        int bricks = countUniqueItemsByNameContains(p, "bricks");
        if (bricks >= 6) QuestTracker.complete(p, "quest.obtain_6_unique_brick_blocks");

        int buckets = countUniqueItems(p, BUCKET_ITEMS);
        if (buckets >= 6) QuestTracker.complete(p, "quest.obtain_6_unique_buckets");

        int workstations = countUniqueItems(p, WORKSTATION_ITEMS);
        if (workstations >= 7) QuestTracker.complete(p, "quest.obtain_7_unique_workstations");

        int logs = countTaggedItems(p, ItemTags.LOGS);
        if (logs >= 6) QuestTracker.complete(p, "quest.obtain_6_log_variants");
        if (logs >= 8) QuestTracker.complete(p, "quest.obtain_8_log_variants");
    }

    private static void checkObtainAllCollections(ServerPlayer p) {
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(p);
        if (server != null && BingoGameData.get(server).teamSyncEnabled) {
            UUID teamId = TeamData.get(server).getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
            if (teamId != null) {
                int totalPatterns = totalItemsByNameContains("pattern");
                if (totalPatterns > 0 && countUniqueItemsByNameContainsForTeam(server, teamId, "pattern") >= totalPatterns) {
                    QuestTracker.complete(p, "quest.obtain_all_banner_patterns");
                }
                int totalDiscs = MUSIC_DISC_ITEMS.size();
                if (totalDiscs > 0 && countUniqueItemsForTeam(server, teamId, MUSIC_DISC_ITEMS) >= totalDiscs) {
                    QuestTracker.complete(p, "quest.obtain_all_music_disks");
                }
                int totalPlates = PRESSURE_PLATE_ITEMS.size();
                if (totalPlates > 0 && countUniqueItemsForTeam(server, teamId, PRESSURE_PLATE_ITEMS) >= totalPlates) {
                    QuestTracker.complete(p, "quest.obtain_all_pressure_plates");
                }
                int totalBricks = totalItemsByNameContains("bricks");
                if (totalBricks > 0 && countUniqueItemsByNameContainsForTeam(server, teamId, "bricks") >= totalBricks) {
                    QuestTracker.complete(p, "quest.obtain_all_bricks");
                }
                int totalBuckets = BUCKET_ITEMS.size();
                if (totalBuckets > 0 && countUniqueItemsForTeam(server, teamId, BUCKET_ITEMS) >= totalBuckets) {
                    QuestTracker.complete(p, "quest.obtain_all_buckets");
                }
                int totalWorkstations = WORKSTATION_ITEMS.size();
                if (totalWorkstations > 0 && countUniqueItemsForTeam(server, teamId, WORKSTATION_ITEMS) >= totalWorkstations) {
                    QuestTracker.complete(p, "quest.obtain_all_workstations");
                }
                return;
            }
        }

        int totalPatterns = totalItemsByNameContains("pattern");
        if (totalPatterns > 0 && countUniqueItemsByNameContains(p, "pattern") >= totalPatterns) {
            QuestTracker.complete(p, "quest.obtain_all_banner_patterns");
        }
        if (hasItemSet(p, MINECART_ITEMS)) {
            QuestTracker.complete(p, "quest.obtain_all_minecart_types");
        }
        if (hasItemSet(p, FURNACE_VARIANTS)) {
            QuestTracker.complete(p, "quest.obtain_all_furnace_variants");
        }
        if (hasItemSet(p, FROGLIGHTS)) {
            QuestTracker.complete(p, "quest.obtain_all_froglights");
        }
        if (hasItemSet(p, MUSHROOM_ITEMS)) {
            QuestTracker.complete(p, "quest.obtain_all_mushrooms");
        }
        if (hasItemSet(p, LOG_TYPES)) {
            QuestTracker.complete(p, "quest.obtain_all_log_types");
        }
        if (hasItemSet(p, ALL_WOOL_TYPES)) {
            QuestTracker.complete(p, "quest.obtain_all_wool_blocks");
        }
        if (hasItemSet(p, PUMPKIN_TYPES)) {
            QuestTracker.complete(p, "quest.obtain_all_pumpkin_types");
        }
        if (hasItemSet(p, RAIL_TYPES)) {
            QuestTracker.complete(p, "quest.obtain_all_rail_types");
        }
        if (hasItemSet(p, TORCH_TYPES)) {
            QuestTracker.complete(p, "quest.obtain_every_type_of_torch");
        }
        if (hasItemSet(p, NAUTILUS_ARMOR_TYPES) || hasAllNautilusArmorMaterials(p)) {
            QuestTracker.complete(p, "quest.obtain_all_nautilus_armor_types");
        }
        if (hasItemSet(p, OVERWORLD_ORE_BLOCKS)) {
            QuestTracker.complete(p, "quest.obtain_all_overworld_ore_blocks");
        }
        if (hasItemSet(p, OVERWORLD_ORES)) {
            QuestTracker.complete(p, "quest.obtain_all_overworld_ores");
        }
        if (hasItemSet(p, NETHER_ORE_BLOCKS)) {
            QuestTracker.complete(p, "quest.obtain_all_nether_ore_blocks");
        }
        if (hasItemSet(p, NETHER_ORES)) {
            QuestTracker.complete(p, "quest.obtain_all_nether_ores");
        }
        if (hasItemSet(p, COBBLESTONE_VARIANTS)) {
            QuestTracker.complete(p, "quest.obtain_all_cobblestone_block_varients");
        }
        if (hasItemSet(p, STONE_BRICK_VARIANTS)) {
            QuestTracker.complete(p, "quest.obtain_all_stone_brick_varients");
        }
        if (hasItemSet(p, SANDSTONE_VARIANTS)) {
            QuestTracker.complete(p, "quest.obtain_all_sandstone_block_varients");
        }
        if (hasItemSet(p, RED_SANDSTONE_VARIANTS)) {
            QuestTracker.complete(p, "quest.obtain_all_red_sandstone_block_varients");
        }
        if (hasItemSet(p, MOSSY_VARIANTS)) {
            QuestTracker.complete(p, "quest.obtain_all_mossy_block_varients");
        }
        if (hasItemSet(p, BLACKSTONE_VARIANTS)) {
            QuestTracker.complete(p, "quest.obtain_all_blackstone_block_varients");
        }
        if (hasItemSet(p, END_STONE_VARIANTS)) {
            QuestTracker.complete(p, "quest.obtain_all_end_stone_varients");
        }
        if (hasItemSet(p, SNOW_VARIANTS)) {
            QuestTracker.complete(p, "quest.obtain_all_snow_varients");
        }
    }

    private static void checkObtain64Extras(ServerPlayer p) {
        checkItemCount(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:arrow"), "quest.obtain_64_arrows");
        checkItemCount(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:coarse_dirt"), "quest.obtain_64_coarse_dirt");
        checkItemCount(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:firefly_bush"), "quest.obtain_64_firefly_bushes");
        checkItemCount(p, com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:glow_lichen"), "quest.obtain_64_glow_lichen");
    }

    private static void checkItemCount(ServerPlayer p, Item item, String questId) {
        if (countItem(p, item) >= 64) {
            QuestTracker.complete(p, questId);
        }
    }

    private static void checkInventoryUnique(ServerPlayer p) {
        int empty = 0;
        int totalSlots = com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(p)).size();
        Set<Item> unique = new HashSet<>();
        for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(p))) {
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) {
                empty++;
            } else {
                unique.add(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack));
            }
        }
        if (empty == 0 && unique.size() >= totalSlots) {
            QuestTracker.complete(p, "quest.fill_inventory_with_unique_items");
        }
    }

    private static void checkEggCollections(ServerPlayer p) {
        if (hasItemSet(p, Set.of(
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:egg"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:blue_egg"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:brown_egg")
        ))) {
            QuestTracker.complete(p, "quest.collect_all_chicken_egg_types");
        }

        if (hasItemSet(p, Set.of(
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:egg"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:blue_egg"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:brown_egg"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:turtle_egg"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:sniffer_egg"),
                com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:dragon_egg")
        ))) {
            QuestTracker.complete(p, "quest.collect_all_egg_types");
        }
    }

    private static void checkBundleOfBundles(ServerPlayer p) {
        for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(p))) {
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) continue;
            if (!stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:bundle"))) continue;
            int bundlesInside = countBundlesInside(stack);
            if (bundlesInside >= 16) {
                QuestTracker.complete(p, "quest.fill_a_bundle_with_16_bundles");
                return;
            }
        }
    }

    private static int countBundlesInside(ItemStack stack) {
        Object contents = com.jamie.jamiebingo.util.ItemStackComponentUtil.get(stack, DataComponents.BUNDLE_CONTENTS);
        if (contents == null) return 0;
        List<ItemStack> items = extractBundleItems(contents);
        if (items == null) return 0;
        int count = 0;
        for (ItemStack s : items) {
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(s)) continue;
            if (s.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:bundle"))) {
                count++;
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private static List<ItemStack> extractBundleItems(Object contents) {
        if (contents == null) return null;
        if (contents instanceof List<?> list) {
            List<ItemStack> out = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof ItemStack s) out.add(s);
            }
            return out;
        }
        try {
            Method m = contents.getClass().getMethod("items");
            Object out = m.invoke(contents);
            if (out instanceof List<?> list) {
                List<ItemStack> res = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof ItemStack s) res.add(s);
                }
                return res;
            }
        } catch (Throwable ignored) {
        }
        try {
            Method m = contents.getClass().getMethod("getItems");
            Object out = m.invoke(contents);
            if (out instanceof List<?> list) {
                List<ItemStack> res = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof ItemStack s) res.add(s);
                }
                return res;
            }
        } catch (Throwable ignored) {
        }
        if (contents instanceof Iterable<?> it) {
            List<ItemStack> res = new ArrayList<>();
            for (Object o : it) {
                if (o instanceof ItemStack s) res.add(s);
            }
            return res;
        }
        return null;
    }

    private static void checkDecoratedShield(ServerPlayer p) {
        ItemStack main = p.getMainHandItem();
        ItemStack off = p.getOffhandItem();
        if (isDecoratedShield(main) || isDecoratedShield(off)) {
            QuestTracker.complete(p, "quest.decorate_a_shield_with_a_banner");
        }
    }

    private static boolean isDecoratedShield(ItemStack stack) {
        if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) return false;
        if (!stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:shield"))) return false;
        return hasBannerPatterns(stack);
    }

    private static void checkContainerFillQuests(ServerPlayer p, ServerLevel level, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos);
        Identifier blockId = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        String path = blockId == null ? "" : blockId.getPath();

        if (path.contains("campfire")) {
            int filled = countCampfireItems(be);
            QuestTracker.setProgress(p, "quest.fill_a_campfire_with_4_food_items", Math.min(filled, 4));
            if (filled >= 4) {
                QuestTracker.complete(p, "quest.fill_a_campfire_with_4_food_items");
            }
            return;
        }

        if (!(be instanceof Container container)) return;

        if ("chiseled_bookshelf".equals(path)) {
            int books = countSlotsFilledWithBooks(container);
            QuestTracker.setProgress(p, "quest.fill_a_chiseled_bookshelf_with_books", Math.min(books, 6));
            if (books >= 6) {
                QuestTracker.complete(p, "quest.fill_a_chiseled_bookshelf_with_books");
            }
        }

        if ("decorated_pot".equals(path)) {
            int filled = countFilledSlots(container);
            QuestTracker.setProgress(p, "quest.fill_a_decorated_pot", filled > 0 ? 1 : 0);
            if (filled > 0) {
                QuestTracker.complete(p, "quest.fill_a_decorated_pot");
            }
        }

        if (path.endsWith("shelf")) {
            Item shelfItem = state.getBlock().asItem();
            if (shelfItem != null && shelfItem != com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:air")) {
                int filled = countSlotsFilledWithItem(container, shelfItem);
                QuestTracker.setProgress(p, "quest.fill_a_shelf_with_shelves", Math.min(filled, container.getContainerSize()));
                if (filled >= container.getContainerSize()) {
                    QuestTracker.complete(p, "quest.fill_a_shelf_with_shelves");
                }
            }
        }
    }

    private static int countFilledSlots(Container container) {
        int size = container.getContainerSize();
        int filled = 0;
        for (int i = 0; i < size; i++) {
            if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(container.getItem(i))) {
                filled++;
            }
        }
        return filled;
    }

    private static int countSlotsFilledWithBooks(Container container) {
        int size = container.getContainerSize();
        int filled = 0;
        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getItem(i);
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) continue;
            Item item = com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack);
            if (isBookItem(item)) filled++;
        }
        return filled;
    }

    private static int countSlotsFilledWithItem(Container container, Item item) {
        int size = container.getContainerSize();
        int filled = 0;
        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getItem(i);
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) continue;
            if (com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack) == item) filled++;
        }
        return filled;
    }

    private static int countCampfireItems(BlockEntity be) {
        if (be == null) return 0;
        if (be instanceof CampfireBlockEntity campfire) {
            try {
                Method m = campfire.getClass().getMethod("getItems");
                Object out = m.invoke(campfire);
                if (out instanceof List<?> list) {
                    int filled = 0;
                    for (Object o : list) {
                        if (o instanceof ItemStack s
                                && !com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(s)
                                && com.jamie.jamiebingo.util.ItemStackComponentUtil.has(s, DataComponents.FOOD)) {
                            filled++;
                        }
                    }
                    return filled;
                }
            } catch (Throwable ignored) {
            }
            try {
                java.lang.reflect.Field f = campfire.getClass().getDeclaredField("items");
                f.setAccessible(true);
                Object out = f.get(campfire);
                if (out instanceof List<?> list) {
                    int filled = 0;
                    for (Object o : list) {
                        if (o instanceof ItemStack s
                                && !com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(s)
                                && com.jamie.jamiebingo.util.ItemStackComponentUtil.has(s, DataComponents.FOOD)) {
                            filled++;
                        }
                    }
                    return filled;
                }
            } catch (Throwable ignored) {
            }
        }
        return 0;
    }

    private static boolean allSlotsFilled(Container container, int minSlots) {
        int size = container.getContainerSize();
        if (size < minSlots) return false;
        for (int i = 0; i < size; i++) {
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(container.getItem(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean anySlotFilled(Container container) {
        int size = container.getContainerSize();
        for (int i = 0; i < size; i++) {
            if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(container.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean allSlotsFilledWithItem(Container container, Item item) {
        int size = container.getContainerSize();
        if (size <= 0) return false;
        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getItem(i);
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) return false;
            if (com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack) != item) return false;
        }
        return true;
    }

    private static boolean allSlotsFilledWithBooks(Container container, int minSlots) {
        int size = container.getContainerSize();
        if (size < minSlots) return false;
        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getItem(i);
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) return false;
            Item item = com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack);
            if (!isBookItem(item)) return false;
        }
        return true;
    }

    private static boolean isBookItem(Item item) {
        return item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:book")
                || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:written_book")
                || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:writable_book")
                || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:enchanted_book");
    }

    private static boolean hasEnchantments(ItemStack stack) {
        if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) return false;
        try {
            return stack.isEnchanted();
        } catch (Throwable ignored) {
        }
        if (stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:enchanted_book"))) {
            return true;
        }
        Object ench = com.jamie.jamiebingo.util.ItemStackComponentUtil.get(stack, DataComponents.ENCHANTMENTS);
        if (ench instanceof java.util.Map<?, ?> map) {
            return !map.isEmpty();
        }
        Object stored = com.jamie.jamiebingo.util.ItemStackComponentUtil.get(stack, DataComponents.STORED_ENCHANTMENTS);
        if (stored instanceof java.util.Map<?, ?> map) {
            return !map.isEmpty();
        }
        if (stored instanceof java.util.Collection<?> col) {
            return !col.isEmpty();
        }
        if (stored != null) {
            try {
                Method m = stored.getClass().getMethod("isEmpty");
                Object out = m.invoke(stored);
                if (out instanceof Boolean b) return !b;
            } catch (Throwable ignored) {
            }
        }
        CompoundTag legacy = com.jamie.jamiebingo.util.ItemStackComponentUtil.getLegacyTagOrEmpty(stack);
        if (legacy.contains("Enchantments") || legacy.contains("StoredEnchantments")) {
            return true;
        }
        return false;
    }

    private static String armorMaterialKey(ItemStack stack) {
        if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) return null;
        Identifier id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack));
        if (id == null) return null;
        String path = id.getPath();
        if (path.startsWith("leather_")) return "leather";
        if (path.startsWith("chainmail_")) return "chainmail";
        if (path.startsWith("iron_")) return "iron";
        if (path.startsWith("golden_")) return "gold";
        if (path.startsWith("diamond_")) return "diamond";
        if (path.startsWith("netherite_")) return "netherite";
        if (path.startsWith("copper_")) return "copper";
        if (path.startsWith("turtle_")) return "turtle";
        return path;
    }

    private static boolean hasBannerPatterns(ItemStack stack) {
        Object patterns = com.jamie.jamiebingo.util.ItemStackComponentUtil.get(stack, DataComponents.BANNER_PATTERNS);
        if (patterns == null) return false;
        if (patterns instanceof java.util.Collection<?> c) return !c.isEmpty();
        try {
            Method isEmpty = patterns.getClass().getMethod("isEmpty");
            Object out = isEmpty.invoke(patterns);
            if (out instanceof Boolean b) return !b;
        } catch (Throwable ignored) {
        }
        try {
            Method size = patterns.getClass().getMethod("size");
            Object out = size.invoke(patterns);
            if (out instanceof Integer i) return i > 0;
        } catch (Throwable ignored) {
        }
        return true;
    }

    private static int countUniqueItemsByNameContains(ServerPlayer p, String token) {
        if (p == null || token == null) return 0;
        String needle = token.toLowerCase(java.util.Locale.ROOT);
        Set<Item> unique = new HashSet<>();
        for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(p))) {
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) continue;
            Item item = com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack);
            Identifier id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
            if (id != null && id.getPath().contains(needle)) {
                unique.add(item);
            }
        }
        for (ItemStack stack : getEquipmentStacks(p)) {
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) continue;
            Item item = com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack);
            Identifier id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
            if (id != null && id.getPath().contains(needle)) {
                unique.add(item);
            }
        }
        return unique.size();
    }

    private static int totalItemsByNameContains(String token) {
        if (token == null) return 0;
        String needle = token.toLowerCase(java.util.Locale.ROOT);
        int total = 0;
        try {
            for (Identifier id : net.minecraftforge.registries.ForgeRegistries.ITEMS.getKeys()) {
                if (id != null && id.getPath().contains(needle)) total++;
            }
        } catch (Throwable ignored) {
        }
        return total;
    }

    private static int totalTaggedItemCount(net.minecraft.tags.TagKey<Item> tag) {
        if (tag == null) return 0;
        int total = 0;
        try {
            for (Item item : net.minecraftforge.registries.ForgeRegistries.ITEMS) {
                if (item != null && item.builtInRegistryHolder().is(tag)) total++;
            }
        } catch (Throwable ignored) {
        }
        return total;
    }

    private static void checkArmorTrimTemplatesInInventory(ServerPlayer p) {
        if (p == null) return;
        for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(p))) {
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) continue;
            recordArmorTrimTemplate(p, stack);
        }
        for (ItemStack stack : getEquipmentStacks(p)) {
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) continue;
            recordArmorTrimTemplate(p, stack);
        }
    }

    private static Entity getSpyglassTarget(ServerPlayer p) {
        Vec3 start = p.getEyePosition(1.0F);
        Vec3 look = p.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(128.0D));
        AABB box = p.getBoundingBox().expandTowards(look.scale(128.0D)).inflate(1.0D);
        List<Entity> entities = p.level().getEntities(p, box, e -> e instanceof LivingEntity && e.isPickable());
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity entity : entities) {
            AABB bb = entity.getBoundingBox().inflate(0.3D);
            java.util.Optional<Vec3> hit = bb.clip(start, end);
            if (hit.isPresent()) {
                double dist = start.distanceToSqr(hit.get());
                if (dist < bestDist) {
                    bestDist = dist;
                    best = entity;
                }
            }
        }
        return best;
    }

    private static boolean isEndermanStaredAt(EnderMan enderman, ServerPlayer p) {
        if (enderman == null || p == null) return false;
        try {
            Method m = enderman.getClass().getMethod("isLookingAtMe", Player.class);
            Object out = m.invoke(enderman, p);
            if (out instanceof Boolean b) return b;
        } catch (Throwable ignored) {
        }
        try {
            Method m = enderman.getClass().getMethod("isStaring", Player.class);
            Object out = m.invoke(enderman, p);
            if (out instanceof Boolean b) return b;
        } catch (Throwable ignored) {
        }
        return true;
    }

    private static boolean isCrossbowRocket(FireworkRocketEntity rocket) {
        if (rocket == null) return false;
        try {
            Method m = rocket.getClass().getMethod("isShotAtAngle");
            Object out = m.invoke(rocket);
            if (out instanceof Boolean b) return b;
        } catch (Throwable ignored) {
        }
        return true;
    }

    private static int getCrafterResultSlot(CrafterMenu menu) {
        if (menu == null) return 9;
        try {
            Method m = menu.getClass().getMethod("getResultSlotIndex");
            Object out = m.invoke(menu);
            if (out instanceof Integer i) return i;
        } catch (Throwable ignored) {
        }
        return 9;
    }

    private static boolean isToolItem(Item item) {
        if (item == null) return false;
        return item.builtInRegistryHolder().is(ItemTags.AXES)
                || item.builtInRegistryHolder().is(ItemTags.HOES)
                || item.builtInRegistryHolder().is(ItemTags.PICKAXES)
                || item.builtInRegistryHolder().is(ItemTags.SHOVELS)
                || item.builtInRegistryHolder().is(ItemTags.SWORDS)
                || item.builtInRegistryHolder().is(ItemTags.SPEARS)
                || item instanceof ShearsItem
                || item instanceof FishingRodItem
                || item instanceof FlintAndSteelItem
                || item instanceof TridentItem;
    }

    private static boolean isArmorItem(Item item) {
        if (item == null) return false;
        return item.builtInRegistryHolder().is(ItemTags.HEAD_ARMOR)
                || item.builtInRegistryHolder().is(ItemTags.CHEST_ARMOR)
                || item.builtInRegistryHolder().is(ItemTags.LEG_ARMOR)
                || item.builtInRegistryHolder().is(ItemTags.FOOT_ARMOR);
    }

    private static boolean isWaxedCopper(BlockState state) {
        Identifier id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && id.getPath().startsWith("waxed_");
    }

    private static boolean isCopperBlock(BlockState state) {
        if (state.is(BlockTags.COPPER)) return true;
        Identifier id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && id.getPath().contains("copper");
    }

    private static boolean isLeadItem(ItemStack stack) {
        return !com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)
                && stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:lead"));
    }

    private static void handleLeadQuests(ServerPlayer p, Entity target) {
        Identifier id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (id != null) {
            if ("cherry_chest_boat".equals(id.getPath())) {
                QuestTracker.complete(p, "quest.attach_a_lead_to_a_cherry_chest_boat");
            }
            String questId = LEAD_TARGET_QUESTS.get(id);
            if (questId != null) {
                QuestTracker.complete(p, questId);
            }
        }

        int unique = countUniqueLeashedTypes(p);
        if (unique >= 4) QuestTracker.complete(p, "quest.attach_a_lead_to_4_unique_entities_at_once");
        if (unique >= 6) QuestTracker.complete(p, "quest.attach_a_lead_to_6_unique_entities_at_once");
        if (unique >= 8) QuestTracker.complete(p, "quest.attach_a_lead_to_8_unique_entities_at_once");
    }

    private static void checkRecentHappyGhastBoatLeadQuest(ServerPlayer p, ServerLevel level) {
        if (p == null || level == null) return;
        Integer recentTick = recentHappyGhastLeadUseTick.get(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        if (recentTick == null) return;
        int now = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer());
        if (now - recentTick > 40) {
            recentHappyGhastLeadUseTick.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
            return;
        }
        if (isHappyGhastBoatLeadPairNearby(level, p, p)) {
            QuestTracker.complete(p, "quest.attach_a_boat_to_a_happy_ghast_using_a_lead");
            recentHappyGhastLeadUseTick.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }
    }

    private static boolean isHappyGhastBoatLeadPairNearby(ServerLevel level, ServerPlayer p, Entity focus) {
        if (level == null || p == null || focus == null) return false;
        AABB box = focus.getBoundingBox().inflate(16.0D, 8.0D, 16.0D);
        List<Entity> nearby = level.getEntities(focus, box, e -> e instanceof Boat || isHappyGhastEntity(e));
        List<Entity> happyGhasts = new ArrayList<>();
        List<Boat> boats = new ArrayList<>();
        for (Entity e : nearby) {
            if (isHappyGhastEntity(e)) happyGhasts.add(e);
            if (e instanceof Boat b) boats.add(b);
        }
        if (isHappyGhastEntity(focus)) happyGhasts.add(focus);
        if (focus instanceof Boat boat) boats.add(boat);

        for (Boat boat : boats) {
            Entity holder = getLeashHolder(boat);
            if (holder != null && happyGhasts.contains(holder)) {
                return true;
            }
            Entity vehicle = boat.getVehicle();
            if (vehicle != null && happyGhasts.contains(vehicle)) {
                return true;
            }
        }
        for (Entity ghast : happyGhasts) {
            if (ghast.getPassengers().stream().anyMatch(Boat.class::isInstance)) {
                return true;
            }
        }
        return false;
    }

    private static int countUniqueLeashedTypes(ServerPlayer p) {
        net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p);
        if (!(baseLevel instanceof ServerLevel level)) return 0;
        AABB box = p.getBoundingBox().inflate(32.0D);
        Set<Identifier> unique = new HashSet<>();
        for (Entity e : level.getEntities(p, box)) {
            Entity holder = getLeashHolder(e);
            if (holder == p) {
                Identifier id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
                if (id != null) unique.add(id);
            }
        }
        return unique.size();
    }

    private static int countUniqueItems(ServerPlayer p, Set<Item> items) {
        if (items == null || items.isEmpty()) return 0;
        Set<Item> unique = new HashSet<>();
        for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(p))) {
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) continue;
            Item item = com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack);
            if (items.contains(item)) unique.add(item);
        }
        for (ItemStack stack : getEquipmentStacks(p)) {
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) continue;
            Item item = com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack);
            if (items.contains(item)) unique.add(item);
        }
        return unique.size();
    }

    public static int getUniqueSaplingCount(ServerPlayer p) {
        return countTaggedItems(p, ItemTags.SAPLINGS);
    }

    public static int getUniqueFlowerCount(ServerPlayer p) {
        return countTaggedItems(p, ItemTags.FLOWERS);
    }

    public static int getUniqueLeashedTypeCount(ServerPlayer p) {
        return countUniqueLeashedTypes(p);
    }

    public static int getItemCount(ServerPlayer p, Item item) {
        return countItem(p, item);
    }

    public static int getMaxStackCount(ServerPlayer p) {
        Map<Item, Integer> counts = new HashMap<>();
        for (ItemStack stack : getUniqueInventoryStacks(p)) {
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) continue;
            counts.merge(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack), stack.getCount(), Integer::sum);
        }
        int max = 0;
        for (int total : counts.values()) {
            if (total > max) max = total;
        }
        return max;
    }

    public static int getMaxBundlesInside(ServerPlayer p) {
        int best = 0;
        for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(p))) {
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) continue;
            if (!stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:bundle"))) continue;
            int bundlesInside = countBundlesInside(stack);
            if (bundlesInside > best) best = bundlesInside;
        }
        for (ItemStack stack : getEquipmentStacks(p)) {
            if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) continue;
            if (!stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:bundle"))) continue;
            int bundlesInside = countBundlesInside(stack);
            if (bundlesInside > best) best = bundlesInside;
        }
        return best;
    }

    private static Entity getLeashHolder(Entity e) {
        if (e == null) return null;
        try {
            Method m = findMethodByNameAndArgCount(e.getClass(), "getLeashHolder", 0);
            if (m != null) {
                Object out = m.invoke(e);
                if (out instanceof Entity ent) return ent;
            }
        } catch (Throwable ignored) {
        }
        try {
            Method m = e.getClass().getMethod("getLeashHolder");
            Object out = m.invoke(e);
            if (out instanceof Entity ent) return ent;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean isDyedLeatherHorseArmor(AbstractHorse horse) {
        ItemStack armor = getHorseArmorItem(horse);
        if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(armor)) return false;
        if (!armor.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:leather_horse_armor"))) return false;
        return com.jamie.jamiebingo.util.ItemStackComponentUtil.has(armor, DataComponents.DYED_COLOR);
    }

    private static ItemStack getHorseArmorItem(AbstractHorse horse) {
        if (horse == null) return com.jamie.jamiebingo.util.ItemStackUtil.empty();
        try {
            Method m = findMethodByNameAndArgCount(horse.getClass(), "getArmor", 0);
            if (m == null) m = findMethodByNameAndArgCount(horse.getClass(), "getBodyArmorItem", 0);
            if (m != null) {
                Object out = m.invoke(horse);
                if (out instanceof ItemStack stack) return stack;
            }
        } catch (Throwable ignored) {
        }
        return com.jamie.jamiebingo.util.ItemStackUtil.empty();
    }

    private static boolean hasWolfArmor(Wolf wolf) {
        if (wolf == null) return false;
        try {
            Method m = findMethodByNameAndArgCount(wolf.getClass(), "getBodyArmorItem", 0);
            if (m == null) m = findMethodByNameAndArgCount(wolf.getClass(), "getArmor", 0);
            if (m != null) {
                Object out = m.invoke(wolf);
                if (out instanceof ItemStack stack) {
                    return !com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack);
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean isTamedEntity(Entity entity) {
        if (entity == null) return false;
        try {
            Method m = findMethodByNameAndArgCount(entity.getClass(), "isTame", 0);
            if (m != null) {
                Object out = m.invoke(entity);
                if (out instanceof Boolean b) return b;
            }
        } catch (Throwable ignored) {
        }
        try {
            Method m = findMethodByNameAndArgCount(entity.getClass(), "isTamed", 0);
            if (m != null) {
                Object out = m.invoke(entity);
                if (out instanceof Boolean b) return b;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean isInLoveAnimal(Entity entity) {
        if (entity == null) return false;
        try {
            Method m = findMethodByNameAndArgCount(entity.getClass(), "isInLove", 0);
            if (m != null) {
                Object out = m.invoke(entity);
                if (out instanceof Boolean b) return b;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static ServerPlayer getOwnerPlayer(Entity entity, MinecraftServer server) {
        if (entity == null || server == null) return null;
        try {
            Method m = entity.getClass().getMethod("getOwner");
            Object out = m.invoke(entity);
            if (out instanceof ServerPlayer sp) return sp;
            if (out instanceof Entity e && e instanceof ServerPlayer sp) return sp;
        } catch (Throwable ignored) {
        }
        try {
            Method m = entity.getClass().getMethod("getOwnerUUID");
            Object out = m.invoke(entity);
            if (out instanceof UUID id) {
                return com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, id);
            }
        } catch (Throwable ignored) {
        }
        try {
            Method m = entity.getClass().getMethod("getOwnerId");
            Object out = m.invoke(entity);
            if (out instanceof UUID id) {
                return com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, id);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean isSnowballDamage(DamageSource source, Entity direct, Entity sourceEntity) {
        if (direct instanceof Snowball || sourceEntity instanceof Snowball) return true;
        Identifier directId = direct == null ? null : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(direct.getType());
        if (directId != null && directId.getPath().contains("snowball")) return true;
        Identifier srcId = sourceEntity == null ? null : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(sourceEntity.getType());
        if (srcId != null && srcId.getPath().contains("snowball")) return true;
        String msgId = source == null ? "" : source.getMsgId();
        return msgId.contains("snowball");
    }

    private static boolean isSplashPotionProjectile(Entity entity) {
        if (entity == null) return false;
        Identifier id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id != null && id.getPath().contains("potion")) {
            return true;
        }
        String name = entity.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        return name.contains("potion");
    }

    private static boolean projectileCarriesItem(Entity projectile, Item expected) {
        if (projectile == null || expected == null) return false;
        try {
            Method m = findMethodByNameAndArgCount(projectile.getClass(), "getItem", 0);
            if (m != null) {
                Object out = m.invoke(projectile);
                if (out instanceof ItemStack stack) {
                    return stack.is(expected);
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean isWindChargeDamage(DamageSource source, Entity direct, Entity sourceEntity) {
        Identifier directId = direct == null ? null : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(direct.getType());
        if (directId != null) {
            String path = directId.getPath();
            if (path.contains("wind_charge") || path.contains("windcharge") || path.contains("breeze_wind_charge")) {
                return true;
            }
        }
        Identifier srcId = sourceEntity == null ? null : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(sourceEntity.getType());
        if (srcId != null) {
            String path = srcId.getPath();
            if (path.contains("wind_charge") || path.contains("windcharge") || path.contains("breeze_wind_charge")) {
                return true;
            }
        }
        String msgId = source == null ? "" : source.getMsgId();
        return msgId.contains("wind_charge") || msgId.contains("windcharge") || msgId.contains("breeze_wind_charge");
    }

    private static boolean isSnowballProjectile(Entity entity) {
        if (entity == null) return false;
        if (entity instanceof Snowball) return true;
        Identifier id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id != null && id.getPath().contains("snowball")) return true;
        String name = entity.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
        return name.contains("snowball");
    }

    private static boolean isWindChargeProjectile(Entity entity) {
        if (entity == null) return false;
        Identifier id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id != null) {
            String path = id.getPath();
            if (path.contains("wind_charge") || path.contains("windcharge") || path.contains("breeze_wind_charge")) {
                return true;
            }
        }
        String name = entity.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
        return name.contains("wind") && name.contains("charge");
    }

    private static PendingCrafterUse findPendingCrafterUse(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return null;
        int tick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer());
        for (PendingCrafterUse use : new ArrayList<>(pendingCrafterUses)) {
            if (tick > use.expireTick) continue;
            if (!use.level.equals(level)) continue;
            if (use.pos.closerThan(pos, 2.0D)) return use;
        }
        return null;
    }

    private static BlockPos findNearbyCrafterPos(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return null;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos check = pos.offset(dx, dy, dz);
                    if (level.getBlockState(check).is(Blocks.CRAFTER)) {
                        return check;
                    }
                }
            }
        }
        return null;
    }

    private static void handleBreedingXp(ServerLevel level, ExperienceOrb orb) {
        if (level == null || orb == null) return;
        int tick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer());
        AABB box = orb.getBoundingBox().inflate(6.0D, 3.0D, 6.0D);
        @SuppressWarnings("unchecked")
        List<Entity> nearby = level.getEntities((Entity) null, box, (java.util.function.Predicate<Entity>) e ->
                e instanceof net.minecraft.world.entity.animal.turtle.Turtle
                        || e instanceof net.minecraft.world.entity.animal.frog.Frog);
        if (nearby.isEmpty()) return;
        for (Entity entity : nearby) {
            UUID ownerId = consumeLoveOwner(entity, tick);
            if (ownerId == null) continue;
            ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(level.getServer(), ownerId);
            if (p == null || !isQuestActive(p)) continue;
            if (entity instanceof net.minecraft.world.entity.animal.turtle.Turtle) {
                QuestTracker.complete(p, "quest.breed_turtle");
                recordUniqueBreed(p, EntityType.TURTLE);
            } else if (entity instanceof net.minecraft.world.entity.animal.frog.Frog) {
                QuestTracker.complete(p, "quest.breed_frogs");
                recordUniqueBreed(p, EntityType.FROG);
            }
        }
    }

    private static void handleGrindstoneXp(ServerLevel level, ExperienceOrb orb) {
        if (level == null || orb == null) return;
        BlockPos pos = orb.blockPosition();
        if (findNearbyBlock(level, pos, Blocks.GRINDSTONE, 2) == null) return;
        Player nearest = level.getNearestPlayer(
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 6.0D, false);
        if (!(nearest instanceof ServerPlayer sp) || !isQuestActive(sp)) return;
        UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(sp);
        int tick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(level.getServer());
        int lastTick = lastGrindstoneXpTriggerTick.getOrDefault(playerId, -1000);
        if (tick - lastTick < 20) return;
        lastGrindstoneXpTriggerTick.put(playerId, tick);
        QuestTracker.complete(sp, "quest.use_a_grindstone");
    }

    private static BlockPos findNearbyBlock(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block block, int radius) {
        if (level == null || pos == null || block == null) return null;
        int r = Math.max(0, radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos check = pos.offset(dx, dy, dz);
                    if (level.getBlockState(check).is(block)) {
                        return check;
                    }
                }
            }
        }
        return null;
    }

    private static UUID consumeLoveOwner(Entity entity, int tick) {
        if (entity == null) return null;
        UUID id = com.jamie.jamiebingo.util.EntityUtil.getUUID(entity);
        LoveOwner owner = loveOwnerByEntity.get(id);
        if (owner == null) return null;
        if (tick >= owner.expireTick) {
            loveOwnerByEntity.remove(id);
            return null;
        }
        loveOwnerByEntity.remove(id);
        return owner.playerId;
    }

    private static UUID consumePendingProjectileKill(Entity entity, String type, int tick) {
        if (entity == null || type == null) return null;
        UUID id = com.jamie.jamiebingo.util.EntityUtil.getUUID(entity);
        PendingProjectileKill pending = pendingProjectileKills.get(id);
        if (pending == null) return null;
        if (tick >= pending.expireTick) {
            pendingProjectileKills.remove(id);
            return null;
        }
        if (!type.equals(pending.type)) return null;
        pendingProjectileKills.remove(id);
        return pending.playerId;
    }

    private static boolean hasAllNautilusArmorMaterials(ServerPlayer p) {
        if (p == null) return false;
        Set<String> materials = new HashSet<>();
        for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(p))) {
            collectNautilusArmorMaterial(stack, materials);
        }
        for (ItemStack stack : getEquipmentStacks(p)) {
            collectNautilusArmorMaterial(stack, materials);
        }
        return materials.contains("copper")
                && materials.contains("iron")
                && materials.contains("gold")
                && materials.contains("diamond")
                && materials.contains("netherite");
    }

    private static void collectNautilusArmorMaterial(ItemStack stack, Set<String> materials) {
        if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) return;
        Item item = com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack);
        Identifier id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
        if (id == null) return;
        String path = id.getPath();
        if (!path.endsWith("_nautilus_armor")) return;
        if (path.startsWith("copper_")) materials.add("copper");
        if (path.startsWith("iron_")) materials.add("iron");
        if (path.startsWith("gold_") || path.startsWith("golden_")) materials.add("gold");
        if (path.startsWith("diamond_")) materials.add("diamond");
        if (path.startsWith("netherite_")) materials.add("netherite");
    }

    private static boolean isShieldOnCooldown(ServerPlayer p) {
        if (p == null) return false;
        ItemStack main = p.getMainHandItem();
        ItemStack off = p.getOffhandItem();
        ItemStack shieldStack = main.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:shield")) ? main
                : off.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:shield")) ? off
                : com.jamie.jamiebingo.util.ItemStackUtil.empty();
        try {
            Method m = p.getCooldowns().getClass().getMethod("isOnCooldown", ItemStack.class);
            Object out = m.invoke(p.getCooldowns(), shieldStack);
            if (out instanceof Boolean b) return b;
        } catch (Throwable ignored) {
        }
        try {
            Method m = p.getCooldowns().getClass().getMethod("isOnCooldown", Item.class);
            Item shield = com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:shield");
            Object out = m.invoke(p.getCooldowns(), shield);
            if (out instanceof Boolean b) return b;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean vaultKeyUsed(BlockEntity be) {
        if (be == null) return false;
        try {
            Method m = be.getClass().getMethod("isUnlocked");
            Object out = m.invoke(be);
            if (out instanceof Boolean b) return b;
        } catch (Throwable ignored) {
        }
        try {
            Method m = be.getClass().getMethod("isOpen");
            Object out = m.invoke(be);
            if (out instanceof Boolean b) return b;
        } catch (Throwable ignored) {
        }
        try {
            Method m = be.getClass().getMethod("isKeyUsed");
            Object out = m.invoke(be);
            if (out instanceof Boolean b) return b;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static ItemStack getGrindstoneResult(GrindstoneMenu menu) {
        if (menu == null) return com.jamie.jamiebingo.util.ItemStackUtil.empty();
        try {
            return menu.getSlot(2).getItem();
        } catch (Throwable ignored) {
        }
        try {
            return menu.getSlot(3).getItem();
        } catch (Throwable ignored) {
        }
        return com.jamie.jamiebingo.util.ItemStackUtil.empty();
    }

    private static void checkAnvilUsage(ServerPlayer p) {
        if (!(p.containerMenu instanceof AnvilMenu menu)) {
            lastAnvilOutput.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
            return;
        }
        ItemStack output = menu.getSlot(2).getItem();
        ItemStack last = lastAnvilOutput.get(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(output)) {
            lastAnvilOutput.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), output.copy());
            return;
        }
        if (last != null && !last.isEmpty()) {
            QuestTracker.complete(p, "quest.use_an_anvil");
            lastAnvilOutput.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }
    }

    private static void checkLoomUsage(ServerPlayer p) {
        if (!(p.containerMenu instanceof LoomMenu menu)) {
            lastLoomOutput.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
            return;
        }
        ItemStack output = menu.getSlot(3).getItem();
        ItemStack last = lastLoomOutput.get(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(output)) {
            lastLoomOutput.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), output.copy());
            return;
        }
        if (last != null && !last.isEmpty()) {
            QuestTracker.complete(p, "quest.use_a_loom_to_design_a_banner");
            lastLoomOutput.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }
    }

    private static void checkSmithingUsage(ServerPlayer p) {
        if (!(p.containerMenu instanceof SmithingMenu menu)) {
            lastSmithingOutput.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
            return;
        }
        ItemStack output = menu.getSlot(3).getItem();
        ItemStack last = lastSmithingOutput.get(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(output)) {
            lastSmithingOutput.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), output.copy());
            return;
        }
        if (last != null && !last.isEmpty()) {
            QuestTracker.complete(p, "quest.use_a_smithing_table");
            recordAppliedArmorTrim(p, last);
            lastSmithingOutput.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }
    }

    private static void checkGrindstoneUsage(ServerPlayer p) {
        UUID id = com.jamie.jamiebingo.util.EntityUtil.getUUID(p);
        if (!(p.containerMenu instanceof GrindstoneMenu menu)) {
            lastGrindstoneOutput.remove(id);
            lastGrindstoneHadEnchant.remove(id);
            lastGrindstoneInputHadEnchant.remove(id);
            return;
        }
        ItemStack output = getGrindstoneResult(menu);
        ItemStack last = lastGrindstoneOutput.get(id);
        ItemStack input0 = menu.getSlot(0).getItem();
        ItemStack input1 = menu.getSlot(1).getItem();
        boolean inputHadEnchant = hasEnchantments(input0) || hasEnchantments(input1);
        boolean prevInputHadEnchant = Boolean.TRUE.equals(lastGrindstoneInputHadEnchant.get(id));
        if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(output)) {
            lastGrindstoneOutput.put(id, output.copy());
            lastGrindstoneHadEnchant.put(id, inputHadEnchant);
            lastGrindstoneInputHadEnchant.put(id, inputHadEnchant);
            if (inputHadEnchant) {
                lastGrindstoneEnchantTick.put(id, com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(p));
            }
            return;
        }
        boolean inputsEmpty = com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(input0)
                && com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(input1);
        if ((last != null && !last.isEmpty()) || (prevInputHadEnchant && !inputHadEnchant) || (prevInputHadEnchant && inputsEmpty)) {
            if (Boolean.TRUE.equals(lastGrindstoneHadEnchant.get(id)) || prevInputHadEnchant) {
                QuestTracker.complete(p, "quest.use_a_grindstone");
            }
            lastGrindstoneOutput.remove(id);
            lastGrindstoneHadEnchant.remove(id);
            lastGrindstoneInputHadEnchant.remove(id);
            lastGrindstoneEnchantTick.remove(id);
        } else {
            lastGrindstoneInputHadEnchant.put(id, inputHadEnchant);
            if (inputHadEnchant) {
                lastGrindstoneEnchantTick.put(id, com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(p));
            }
        }
    }

    private static void checkStonecutterUsage(ServerPlayer p) {
        UUID id = com.jamie.jamiebingo.util.EntityUtil.getUUID(p);
        if (!(p.containerMenu instanceof StonecutterMenu menu)) {
            lastStonecutterOutput.remove(id);
            return;
        }
        ItemStack output = menu.getSlot(1).getItem();
        ItemStack last = lastStonecutterOutput.get(id);
        if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(output)) {
            lastStonecutterOutput.put(id, output.copy());
            return;
        }
        if (last != null && !last.isEmpty()) {
            QuestTracker.complete(p, "quest.use_a_stonecutter");
            lastStonecutterOutput.remove(id);
        }
    }

    private static void checkCrafterUsage(ServerPlayer p) {
        UUID id = com.jamie.jamiebingo.util.EntityUtil.getUUID(p);
        if (!(p.containerMenu instanceof CrafterMenu menu)) {
            lastCrafterOutput.remove(id);
            return;
        }
        int outSlot = getCrafterResultSlot(menu);
        ItemStack output = menu.getSlot(outSlot).getItem();
        ItemStack last = lastCrafterOutput.get(id);
        if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(output)) {
            lastCrafterOutput.put(id, output.copy());
            return;
        }
        if (last != null && !last.isEmpty()) {
            QuestTracker.complete(p, "quest.use_a_crafter");
            lastCrafterOutput.remove(id);
        }
    }

    private static void checkSpyglassLook(ServerPlayer p) {
        if (p == null || !p.isUsingItem()) return;
        ItemStack use = p.getUseItem();
        if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(use)) return;
        if (!use.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:spyglass"))) return;

        int tick = com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(p);
        UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(p);
        int last = lastSpyglassTick.getOrDefault(playerId, 0);
        if (tick - last < SPYGLASS_CHECK_INTERVAL) return;
        lastSpyglassTick.put(playerId, tick);

        Entity entity = getSpyglassTarget(p);
        if (entity == null) return;
        if (!(entity instanceof LivingEntity)) return;

        Identifier id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (id == null) return;

        recordUniqueSpyglassLook(p, id);

        String path = id.getPath();
        if ("enderman".equals(path)) {
            QuestTracker.complete(p, "quest.look_at_an_enderman_with_a_spyglass");
        } else if ("guardian".equals(path)) {
            QuestTracker.complete(p, "quest.look_at_a_guardian_with_a_spyglass");
        } else if ("horse".equals(path)) {
            QuestTracker.complete(p, "quest.look_at_a_horse_with_a_spyglass");
        } else if ("husk".equals(path)) {
            QuestTracker.complete(p, "quest.look_at_a_husk_with_a_spyglass");
        } else if ("piglin_brute".equals(path)) {
            QuestTracker.complete(p, "quest.look_at_a_piglin_brute_with_a_spyglass");
        }
    }

    private static void checkVillagerTradeStat(ServerPlayer p) {
        int trades = p.getStats().getValue(Stats.CUSTOM.get(Stats.TRADED_WITH_VILLAGER));
        int last = lastVillagerTrades.getOrDefault(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), trades);
        if (trades > last) {
            QuestTracker.complete(p, "quest.trade_with_a_villager");
        }
        lastVillagerTrades.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), trades);
    }

    private static void checkRaidStart(ServerPlayer p, ServerLevel level) {
        if (p == null || level == null) return;
        if (!isQuestActive(p)) return;
        if (!isRaidActiveAt(level, p.blockPosition())) return;
        QuestTracker.complete(p, "quest.start_a_raid");
    }

    private static boolean isRaidActiveAt(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return false;
        try {
            Method m = level.getClass().getMethod("getRaidAt", BlockPos.class);
            Object raid = m.invoke(level, pos);
            if (raid == null) return false;
            try {
                Method active = raid.getClass().getMethod("isActive");
                Object out = active.invoke(raid);
                if (out instanceof Boolean b) return b;
            } catch (Throwable ignored) {
            }
            try {
                Method started = raid.getClass().getMethod("isStarted");
                Object out = started.invoke(raid);
                if (out instanceof Boolean b) return b;
            } catch (Throwable ignored) {
            }
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void checkStructureQuests(ServerPlayer p, ServerLevel level) {
        if (p == null || level == null) return;
        if (!isQuestActive(p)) return;
        BlockPos pos = p.blockPosition();
        if (isStructureInChunk(level, pos, id("minecraft:woodland_mansion"))
                || isInStructure(level, pos, id("minecraft:woodland_mansion"))) {
            QuestTracker.complete(p, "quest.locate_woodland_mansion");
        }
        if (isStructureInChunk(level, pos, id("minecraft:trail_ruins"))
                || isInStructure(level, pos, id("minecraft:trail_ruins"))) {
            QuestTracker.complete(p, "quest.locate_trail_ruins");
        }
    }

    private static boolean isStructureInChunk(ServerLevel level, BlockPos pos, Identifier structureId) {
        if (level == null || pos == null || structureId == null) return false;
        try {
            ChunkPos chunkPos = new ChunkPos(pos);
            Object chunk = level.getChunk(chunkPos.x, chunkPos.z);
            if (chunk == null) return false;
            Map<?, ?> refs = getChunkStructureReferences(chunk);
            if (refs != null && mapHasStructureKey(refs, structureId)) {
                return true;
            }
            Map<?, ?> starts = getChunkStructureStarts(chunk);
            if (starts != null && mapHasStructureKey(starts, structureId)) {
                return true;
            }
            if (starts != null && mapHasStructureValueMatch(starts, structureId, chunkPos)) {
                return true;
            }
            ResourceKey<?> key = ResourceKey.create(Registries.STRUCTURE, structureId);
            Object holder = getStructureHolder(getStructureRegistry(level), key);
            if (holder != null) {
                Map<?, ?> refs2 = getChunkStructureReferences(chunk);
                if (refs2 != null && mapHasStructureKey(refs2, holder)) {
                    return true;
                }
                Map<?, ?> starts2 = getChunkStructureStarts(chunk);
                if (starts2 != null && mapHasStructureKey(starts2, holder)) {
                    return true;
                }
                if (starts2 != null && mapHasStructureValueMatch(starts2, structureId, chunkPos)) {
                    return true;
                }
            }
            // Check nearby chunks for a structure start that intersects this chunk.
            int radius = 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    Object nearChunk = level.getChunk(chunkPos.x + dx, chunkPos.z + dz);
                    if (nearChunk == null) continue;
                    Map<?, ?> nearStarts = getChunkStructureStarts(nearChunk);
                    if (nearStarts != null && mapHasStructureValueMatch(nearStarts, structureId, chunkPos)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean isInStructure(ServerLevel level, BlockPos pos, Identifier structureId) {
        if (level == null || pos == null || structureId == null) return false;
        Object manager = getStructureManager(level);
        if (manager == null) return false;

        Object registry = getStructureRegistry(level);
        if (registry == null) return false;

        Object structure = null;
        ResourceKey<?> structureKey = null;
        Object structureHolder = null;
        try {
            Method get = registry.getClass().getMethod("get", Identifier.class);
            structure = get.invoke(registry, structureId);
        } catch (Throwable ignored) {
        }
        try {
            structureKey = ResourceKey.create(Registries.STRUCTURE, structureId);
        } catch (Throwable ignored) {
        }
        if (structureKey != null) {
            structureHolder = getStructureHolder(registry, structureKey);
            if (structure == null) {
                try {
                    Method get = registry.getClass().getMethod("get", ResourceKey.class);
                    structure = get.invoke(registry, structureKey);
                } catch (Throwable ignored) {
                }
            }
        }
        if (structure == null) return false;

        Object start = findStructureStart(manager, pos, structure, structureHolder, structureKey);
        if (start == null) return false;

        if (structureStartContains(start, pos)) return true;

        try {
            Method isValid = start.getClass().getMethod("isValid");
            Object out = isValid.invoke(start);
            if (out instanceof Boolean b) return b;
        } catch (Throwable ignored) {
        }
        try {
            Method isEmpty = start.getClass().getMethod("isEmpty");
            Object out = isEmpty.invoke(start);
            if (out instanceof Boolean b) return !b;
        } catch (Throwable ignored) {
        }
        return true;
    }

    private static Object getStructureManager(ServerLevel level) {
        if (level == null) return null;
        try {
            Method m = level.getClass().getMethod("structureManager");
            return m.invoke(level);
        } catch (Throwable ignored) {
        }
        try {
            Method m = level.getClass().getMethod("getStructureManager");
            return m.invoke(level);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object getStructureRegistry(ServerLevel level) {
        if (level == null) return null;
        try {
            RegistryAccess access = level.registryAccess();
            Method m = access.getClass().getMethod("registry", ResourceKey.class);
            Object out = m.invoke(access, Registries.STRUCTURE);
            if (out instanceof java.util.Optional<?> opt) {
                return opt.orElse(null);
            }
            return out;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object getStructureHolder(Object registry, ResourceKey<?> key) {
        if (registry == null || key == null) return null;
        try {
            Method m = registry.getClass().getMethod("getHolder", ResourceKey.class);
            Object out = m.invoke(registry, key);
            if (out instanceof java.util.Optional<?> opt) {
                return opt.orElse(null);
            }
            return out;
        } catch (Throwable ignored) {
        }
        try {
            Method m = registry.getClass().getMethod("getHolderOrThrow", ResourceKey.class);
            return m.invoke(registry, key);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Map<?, ?> getChunkStructureReferences(Object chunk) {
        if (chunk == null) return null;
        try {
            Method m = chunk.getClass().getMethod("getAllReferences");
            Object out = m.invoke(chunk);
            if (out instanceof Map<?, ?> map) return map;
        } catch (Throwable ignored) {
        }
        try {
            Method m = chunk.getClass().getMethod("getReferences");
            Object out = m.invoke(chunk);
            if (out instanceof Map<?, ?> map) return map;
        } catch (Throwable ignored) {
        }
        try {
            Method m = chunk.getClass().getMethod("getStructureReferences");
            Object out = m.invoke(chunk);
            if (out instanceof Map<?, ?> map) return map;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Map<?, ?> getChunkStructureStarts(Object chunk) {
        if (chunk == null) return null;
        try {
            Method m = chunk.getClass().getMethod("getAllStarts");
            Object out = m.invoke(chunk);
            if (out instanceof Map<?, ?> map) return map;
        } catch (Throwable ignored) {
        }
        try {
            Method m = chunk.getClass().getMethod("getStructureStarts");
            Object out = m.invoke(chunk);
            if (out instanceof Map<?, ?> map) return map;
        } catch (Throwable ignored) {
        }
        try {
            Method m = chunk.getClass().getMethod("getStarts");
            Object out = m.invoke(chunk);
            if (out instanceof Map<?, ?> map) return map;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean mapHasStructureKey(Map<?, ?> map, Identifier structureId) {
        if (map == null || structureId == null) return false;
        for (Object key : map.keySet()) {
            if (key == null) continue;
            if (key instanceof Identifier id && id.equals(structureId)) return true;
            if (key instanceof ResourceKey<?> rk) {
                Identifier id = getResourceKeyId(rk);
                if (id != null && structureId.equals(id)) return true;
            }
            if (key instanceof Holder<?> holder) {
                try {
                    java.util.Optional<?> opt = holder.unwrapKey();
                    if (opt.isPresent() && opt.get() instanceof ResourceKey<?> rk) {
                        Identifier id = getResourceKeyId(rk);
                        if (id != null && structureId.equals(id)) return true;
                    }
                } catch (Throwable ignored) {
                }
            }
            String text = key.toString();
            if (text != null && text.contains(structureId.getPath())) return true;
        }
        return false;
    }

    private static boolean mapHasStructureKey(Map<?, ?> map, Object expectedKey) {
        if (map == null || expectedKey == null) return false;
        for (Object key : map.keySet()) {
            if (key == null) continue;
            if (key.equals(expectedKey)) return true;
        }
        return false;
    }

    private static boolean mapHasStructureValueMatch(Map<?, ?> map, Identifier structureId, ChunkPos chunkPos) {
        if (map == null || structureId == null || chunkPos == null) return false;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value == null) continue;
            Identifier id = getStructureIdFromStart(value);
            if (id != null && structureId.equals(id)) {
                if (structureStartIntersectsChunk(value, chunkPos)) return true;
                return true;
            }
            String text = value.toString();
            if (text != null && text.contains(structureId.getPath())) {
                if (structureStartIntersectsChunk(value, chunkPos)) return true;
                return true;
            }
        }
        return false;
    }

    private static Identifier getStructureIdFromStart(Object start) {
        if (start == null) return null;
        try {
            Method m = start.getClass().getMethod("getStructure");
            Object out = m.invoke(start);
            if (out instanceof Holder<?> holder) {
                java.util.Optional<?> opt = holder.unwrapKey();
                if (opt.isPresent() && opt.get() instanceof ResourceKey<?> rk) {
                    return getResourceKeyId(rk);
                }
            }
            if (out instanceof ResourceKey<?> rk) {
                return getResourceKeyId(rk);
            }
            if (out != null) {
                Identifier id = tryExtractStructureId(out);
                if (id != null) return id;
            }
        } catch (Throwable ignored) {
        }
        try {
            Method m = start.getClass().getMethod("getStructureType");
            Object out = m.invoke(start);
            Identifier id = tryExtractStructureId(out);
            if (id != null) return id;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Identifier tryExtractStructureId(Object structure) {
        if (structure == null) return null;
        if (structure instanceof ResourceKey<?> rk) return getResourceKeyId(rk);
        if (structure instanceof Holder<?> holder) {
            try {
                java.util.Optional<?> opt = holder.unwrapKey();
                if (opt.isPresent() && opt.get() instanceof ResourceKey<?> rk) {
                    return getResourceKeyId(rk);
                }
            } catch (Throwable ignored) {
            }
        }
        String text = structure.toString();
        if (text != null && text.contains(":")) {
            try {
                String candidate = text;
                int idx = text.indexOf("minecraft:");
                if (idx >= 0) {
                    candidate = text.substring(idx);
                }
                return id(candidate);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static boolean structureStartIntersectsChunk(Object start, ChunkPos chunkPos) {
        if (start == null || chunkPos == null) return false;
        try {
            Method m = start.getClass().getMethod("getBoundingBox");
            Object out = m.invoke(start);
            if (out instanceof net.minecraft.world.level.levelgen.structure.BoundingBox bb) {
                int minX = chunkPos.getMinBlockX();
                int minZ = chunkPos.getMinBlockZ();
                int maxX = minX + 15;
                int maxZ = minZ + 15;
                return bb.intersects(minX, minZ, maxX, maxZ);
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static Identifier getResourceKeyId(ResourceKey<?> key) {
        if (key == null) return null;
        try {
            Method m = key.getClass().getMethod("location");
            Object out = m.invoke(key);
            if (out instanceof Identifier id) return id;
        } catch (Throwable ignored) {
        }
        try {
            Method m = key.getClass().getMethod("getLocation");
            Object out = m.invoke(key);
            if (out instanceof Identifier id) return id;
        } catch (Throwable ignored) {
        }
        String text = key.toString();
        if (text == null) return null;
        String candidate = text;
        int lb = text.indexOf('[');
        int rb = text.indexOf(']');
        if (lb >= 0 && rb > lb) {
            candidate = text.substring(lb + 1, rb);
        }
        int idx = candidate.indexOf(':');
        if (idx < 0) return null;
        return id(candidate);
    }

    private static Object findStructureStart(Object manager, BlockPos pos, Object structure, Object holder, ResourceKey<?> key) {
        if (manager == null || pos == null) return null;
        for (Method m : manager.getClass().getMethods()) {
            String name = m.getName().toLowerCase(java.util.Locale.ROOT);
            if (!name.contains("structure")) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length != 2) continue;
            if (p[0] != BlockPos.class) continue;
            Object out = tryInvokeStructureMethod(manager, pos, p[1], m, holder, structure, key);
            if (out != null) return out;
        }
        try {
            Method m = manager.getClass().getMethod("getStructureWithPieceAt", BlockPos.class, structure.getClass());
            Object out = m.invoke(manager, pos, structure);
            if (out != null) return out;
        } catch (Throwable ignored) {
        }
        try {
            Method m = manager.getClass().getMethod("getStructureAt", BlockPos.class, structure.getClass());
            Object out = m.invoke(manager, pos, structure);
            if (out != null) return out;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object tryInvokeStructureMethod(Object manager, BlockPos pos, Class<?> paramType, Method method,
                                                   Object holder, Object structure, ResourceKey<?> key) {
        try {
            if (holder != null && paramType.isAssignableFrom(holder.getClass())) {
                return method.invoke(manager, pos, holder);
            }
            if (structure != null && paramType.isAssignableFrom(structure.getClass())) {
                return method.invoke(manager, pos, structure);
            }
            if (key != null && paramType.isAssignableFrom(key.getClass())) {
                return method.invoke(manager, pos, key);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean structureStartContains(Object start, BlockPos pos) {
        if (start == null || pos == null) return false;
        try {
            Method m = start.getClass().getMethod("getBoundingBox");
            Object out = m.invoke(start);
            if (out instanceof net.minecraft.world.level.levelgen.structure.BoundingBox bb) {
                return bb.isInside(pos);
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void processSleepAlone(MinecraftServer server) {
        if (server == null || sleepAloneCandidate.isEmpty()) return;
        for (UUID playerId : new ArrayList<>(sleepAloneCandidate.keySet())) {
            ServerPlayer player = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, playerId);
            if (player == null) {
                sleepAloneCandidate.remove(playerId);
                continue;
            }
            net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(player);
            if (baseLevel == null) {
                sleepAloneCandidate.remove(playerId);
                continue;
            }
            if (baseLevel.dimension() != Level.OVERWORLD) {
                sleepAloneCandidate.remove(playerId);
                continue;
            }
            if (isDay(baseLevel) && !player.isSleeping()) {
                sleepAloneCandidate.remove(playerId);
                QuestTracker.complete(player, "quest.sleep_alone_in_the_overworld");
            }
        }
    }

    private static boolean armorStandHasFullArmor(ArmorStand stand) {
        ItemStack head = stand.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = stand.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = stand.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = stand.getItemBySlot(EquipmentSlot.FEET);

        return !com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(head) && !com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(chest) && !com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(legs) && !com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(feet);
    }

    private static boolean hasAnySpyglassAdvancement(ServerPlayer p, MinecraftServer server) {
        String[] ids = new String[] {
                "adventure/spyglass_at_parrot",
                "adventure/spyglass_at_ghast",
                "adventure/spyglass_at_dragon"
        };

        for (String id : ids) {
            net.minecraft.advancements.AdvancementHolder adv = getAdvancement(server, id("minecraft:" + id));
            if (adv != null && isAdvancementDone(p, adv)) {
                return true;
            }
        }
        return false;
    }

    private static int countCompletedAdvancements(ServerPlayer p, MinecraftServer server) {
        int total = 0;
        if (!advancementsLoaded(server)) {
            return 0;
        }
        for (net.minecraft.advancements.AdvancementHolder adv : getAllAdvancements(server)) {
            if (!shouldCountAdvancement(adv)) continue;
            if (isAdvancementDone(p, adv)) {
                total++;
            }
        }
        return total;
    }

    private static boolean isAdvancementDone(ServerPlayer p, net.minecraft.advancements.AdvancementHolder adv) {
        if (p == null || adv == null) return false;
        Object playerAdvancements = getPlayerAdvancements(p);
        if (playerAdvancements == null) return false;
        try {
            java.lang.reflect.Method getOrStartProgress = findMethodByNameAndArgCount(playerAdvancements.getClass(), "getOrStartProgress", 1);
            if (getOrStartProgress == null) return false;
            Object progress = getOrStartProgress.invoke(playerAdvancements, adv);
            if (progress == null) return false;
            java.lang.reflect.Method isDone = findMethodByNameAndArgCount(progress.getClass(), "isDone", 0);
            if (isDone == null) return false;
            Object done = isDone.invoke(progress);
            return done instanceof Boolean b && b;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Object getPlayerAdvancements(ServerPlayer p) {
        try {
            java.lang.reflect.Method m = findMethodByNameAndArgCount(p.getClass(), "getAdvancements", 0);
            if (m != null) return m.invoke(p);
        } catch (Exception ignored) {
        }
        // Fallback: try any no-arg method returning a type that looks like PlayerAdvancements
        for (java.lang.reflect.Method m : p.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            String name = m.getName().toLowerCase(java.util.Locale.ROOT);
            String ret = m.getReturnType().getName().toLowerCase(java.util.Locale.ROOT);
            if (name.contains("advancement") || ret.contains("advancement")) {
                try {
                    return m.invoke(p);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static java.lang.reflect.Method findMethodByNameAndArgCount(Class<?> type, String name, int argCount) {
        for (java.lang.reflect.Method m : type.getMethods()) {
            if (!m.getName().equals(name)) continue;
            if (m.getParameterCount() == argCount) return m;
        }
        return null;
    }

    private static boolean advancementsLoaded(MinecraftServer server) {
        if (server == null) return false;
        return getAllAdvancements(server).iterator().hasNext();
    }

    private static boolean shouldCountAdvancement(net.minecraft.advancements.AdvancementHolder adv) {
        if (adv == null) return false;
        // Try to respect display.shouldAnnounceChat(), but fall back to true if we can't read it.
        try {
            // Prefer public value() if available
            java.lang.reflect.Method value = adv.getClass().getMethod("value");
            Object advValue = value.invoke(adv);
            if (advValue != null) {
                java.lang.reflect.Method display = advValue.getClass().getMethod("display");
                Object dispOpt = display.invoke(advValue);
                if (dispOpt instanceof java.util.Optional<?> opt) {
                    if (opt.isEmpty()) return false;
                    Object disp = opt.get();
                    java.lang.reflect.Method announce = disp.getClass().getMethod("shouldAnnounceChat");
                    Object out = announce.invoke(disp);
                    if (out instanceof Boolean b) return b;
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    private static java.util.List<net.minecraft.advancements.AdvancementHolder> getAllAdvancements(MinecraftServer server) {
        Object mgr = getAdvancementManager(server);
        if (mgr == null) return java.util.Collections.emptyList();
        // Try common method name first
        try {
            java.lang.reflect.Method m = mgr.getClass().getMethod("getAllAdvancements");
            Object out = m.invoke(mgr);
            if (out instanceof java.lang.Iterable<?> it) {
                return toAdvList(it);
            }
        } catch (Throwable ignored) {
        }
        // Fallback: any no-arg method returning Iterable/Collection
        for (java.lang.reflect.Method m : mgr.getClass().getMethods()) {
            if (java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            Class<?> rt = m.getReturnType();
            if (!java.lang.Iterable.class.isAssignableFrom(rt) && !java.util.Collection.class.isAssignableFrom(rt)) continue;
            try {
                Object out = m.invoke(mgr);
                if (out instanceof java.lang.Iterable<?> it) {
                    return toAdvList(it);
                }
            } catch (Throwable ignored) {
            }
        }
        return java.util.Collections.emptyList();
    }

    private static java.util.List<net.minecraft.advancements.AdvancementHolder> toAdvList(java.lang.Iterable<?> it) {
        java.util.List<net.minecraft.advancements.AdvancementHolder> list = new java.util.ArrayList<>();
        for (Object o : it) {
            if (o instanceof net.minecraft.advancements.AdvancementHolder ah) {
                list.add(ah);
            }
        }
        return list;
    }

    private static Object getAdvancementManager(MinecraftServer server) {
        if (server == null) return null;
        try {
            java.lang.reflect.Method m = server.getClass().getMethod("getAdvancements");
            return m.invoke(server);
        } catch (Throwable ignored) {
        }
        for (java.lang.reflect.Method m : server.getClass().getMethods()) {
            if (java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            String name = m.getReturnType().getName().toLowerCase();
            if (!name.contains("advancement")) continue;
            try {
                return m.invoke(server);
            } catch (Throwable ignored) {
            }
        }
        for (java.lang.reflect.Field f : server.getClass().getDeclaredFields()) {
            String name = f.getType().getName().toLowerCase();
            if (!name.contains("advancement")) continue;
            try {
                f.setAccessible(true);
                return f.get(server);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static net.minecraft.advancements.AdvancementHolder getAdvancement(MinecraftServer server, net.minecraft.resources.Identifier id) {
        Object mgr = getAdvancementManager(server);
        if (mgr == null || id == null) return null;
        try {
            java.lang.reflect.Method m = mgr.getClass().getMethod("get", net.minecraft.resources.Identifier.class);
            Object out = m.invoke(mgr, id);
            if (out instanceof net.minecraft.advancements.AdvancementHolder ah) return ah;
        } catch (Throwable ignored) {
        }
        for (java.lang.reflect.Method m : mgr.getClass().getMethods()) {
            if (java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length != 1 || p[0] != net.minecraft.resources.Identifier.class) continue;
            if (!net.minecraft.advancements.AdvancementHolder.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                Object out = m.invoke(mgr, id);
                if (out instanceof net.minecraft.advancements.AdvancementHolder ah) return ah;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static net.minecraft.advancements.AdvancementHolder getAdvancementBySuffix(MinecraftServer server, String suffix) {
        if (server == null || suffix == null || suffix.isBlank()) return null;
        String needle = suffix.toLowerCase(java.util.Locale.ROOT);
        for (net.minecraft.advancements.AdvancementHolder adv : getAllAdvancements(server)) {
            Identifier id = getAdvancementId(adv);
            if (id == null) continue;
            String path = id.getPath().toLowerCase(java.util.Locale.ROOT);
            if (path.endsWith(needle)) return adv;
        }
        return null;
    }

    private static Identifier getAdvancementId(net.minecraft.advancements.AdvancementHolder adv) {
        if (adv == null) return null;
        try {
            Method m = adv.getClass().getMethod("id");
            Object out = m.invoke(adv);
            if (out instanceof Identifier id) return id;
        } catch (Throwable ignored) {
        }
        try {
            Method m = adv.getClass().getMethod("getId");
            Object out = m.invoke(adv);
            if (out instanceof Identifier id) return id;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static net.minecraft.advancements.AdvancementHolder getAdvancementByTitle(MinecraftServer server, String titleContains) {
        if (server == null || titleContains == null || titleContains.isBlank()) return null;
        String needle = titleContains.toLowerCase(java.util.Locale.ROOT);
        for (net.minecraft.advancements.AdvancementHolder adv : getAllAdvancements(server)) {
            String title = getAdvancementTitle(adv);
            if (title != null && title.toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                return adv;
            }
        }
        return null;
    }

    private static String getAdvancementTitle(net.minecraft.advancements.AdvancementHolder adv) {
        if (adv == null) return null;
        try {
            Method value = adv.getClass().getMethod("value");
            Object advValue = value.invoke(adv);
            if (advValue != null) {
                Method display = advValue.getClass().getMethod("display");
                Object dispOpt = display.invoke(advValue);
                if (dispOpt instanceof java.util.Optional<?> opt && opt.isPresent()) {
                    Object disp = opt.get();
                    Method title = disp.getClass().getMethod("getTitle");
                    Object out = title.invoke(disp);
                    if (out instanceof net.minecraft.network.chat.Component c) {
                        return c.getString();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean isNetheriteEquipment(Item item) {
        return item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_sword")
                || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_pickaxe")
                || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_axe")
                || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_shovel")
                || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_hoe")
                || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_helmet")
                || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_chestplate")
                || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_leggings")
                || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:netherite_boots");
    }

    private static boolean isNearVines(ServerPlayer p) {
        net.minecraft.world.level.Level baseLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(p);
        if (baseLevel == null) return false;
        BlockPos pos = com.jamie.jamiebingo.util.EntityPosUtil.getBlockPos(p);
        for (int dy = 0; dy <= 2; dy++) {
            BlockState state = baseLevel.getBlockState(pos.above(dy));
            if (state.is(Blocks.VINE)
                    || state.is(Blocks.CAVE_VINES)
                    || state.is(Blocks.CAVE_VINES_PLANT)
                    || state.is(Blocks.TWISTING_VINES)
                    || state.is(Blocks.TWISTING_VINES_PLANT)
                    || state.is(Blocks.WEEPING_VINES)
                    || state.is(Blocks.WEEPING_VINES_PLANT)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDay(net.minecraft.world.level.Level level) {
        if (level == null) return false;
        long time = level.getDayTime() % 24000L;
        return time < 12000L;
    }

    private static final class PendingBlockCheck {
        final ServerLevel level;
        final BlockPos pos;
        final UUID playerId;
        final int checkTick;

        PendingBlockCheck(ServerLevel level, BlockPos pos, UUID playerId, int checkTick) {
            this.level = level;
            this.pos = pos;
            this.playerId = playerId;
            this.checkTick = checkTick;
        }
    }

    private static final class PendingEggCheck {
        final ServerLevel level;
        final BlockPos pos;
        final UUID playerId;
        final int checkTick;

        PendingEggCheck(ServerLevel level, Vec3 hitPos, UUID playerId, int checkTick) {
            this.level = level;
            this.pos = BlockPos.containing(hitPos);
            this.playerId = playerId;
            this.checkTick = checkTick;
        }
    }

    private static final class PendingArmorStandCheck {
        final ServerLevel level;
        final UUID standId;
        final UUID playerId;
        final int checkTick;

        PendingArmorStandCheck(ServerLevel level, UUID standId, UUID playerId, int checkTick) {
            this.level = level;
            this.standId = standId;
            this.playerId = playerId;
            this.checkTick = checkTick;
        }
    }

    private static final class PendingEndCrystalCheck {
        final ServerLevel level;
        final BlockPos pos;
        final UUID playerId;
        final int checkTick;

        PendingEndCrystalCheck(ServerLevel level, BlockPos pos, UUID playerId, int checkTick) {
            this.level = level;
            this.pos = pos;
            this.playerId = playerId;
            this.checkTick = checkTick;
        }
    }

    private static final class PendingVaultKeyCheck {
        final ServerLevel level;
        final BlockPos pos;
        final UUID playerId;
        final Item keyItem;
        final int countBefore;
        final int handCountBefore;
        final net.minecraft.world.InteractionHand hand;
        final int checkTick;

        PendingVaultKeyCheck(ServerLevel level, BlockPos pos, UUID playerId, Item keyItem, int countBefore, int handCountBefore,
                              net.minecraft.world.InteractionHand hand, int checkTick) {
            this.level = level;
            this.pos = pos;
            this.playerId = playerId;
            this.keyItem = keyItem;
            this.countBefore = countBefore;
            this.handCountBefore = handCountBefore;
            this.hand = hand;
            this.checkTick = checkTick;
        }
    }

    private static final class PendingWolfArmorCheck {
        final ServerLevel level;
        final UUID wolfId;
        final UUID playerId;
        final int checkTick;

        PendingWolfArmorCheck(ServerLevel level, UUID wolfId, UUID playerId, int checkTick) {
            this.level = level;
            this.wolfId = wolfId;
            this.playerId = playerId;
            this.checkTick = checkTick;
        }
    }

    private static final class PendingTameCheck {
        final ServerLevel level;
        final UUID entityId;
        final UUID playerId;
        int checkTick;
        final int expireTick;

        PendingTameCheck(ServerLevel level, UUID entityId, UUID playerId, int checkTick, int expireTick) {
            this.level = level;
            this.entityId = entityId;
            this.playerId = playerId;
            this.checkTick = checkTick;
            this.expireTick = expireTick;
        }
    }

    private static final class PendingCrafterUse {
        final ServerLevel level;
        final BlockPos pos;
        final UUID playerId;
        final int expireTick;

        PendingCrafterUse(ServerLevel level, BlockPos pos, UUID playerId, int expireTick) {
            this.level = level;
            this.pos = pos;
            this.playerId = playerId;
            this.expireTick = expireTick;
        }
    }

    private static final class PendingShieldDisableCheck {
        final UUID playerId;
        final int checkTick;

        PendingShieldDisableCheck(UUID playerId, int checkTick) {
            this.playerId = playerId;
            this.checkTick = checkTick;
        }
    }

    private static final class PendingLoveCheck {
        final ServerLevel level;
        final UUID entityId;
        final UUID playerId;
        int checkTick;
        final int expireTick;

        PendingLoveCheck(ServerLevel level, UUID entityId, UUID playerId, int checkTick, int expireTick) {
            this.level = level;
            this.entityId = entityId;
            this.playerId = playerId;
            this.checkTick = checkTick;
            this.expireTick = expireTick;
        }
    }

    private static final class PendingButtonPressCheck {
        final ServerLevel level;
        final BlockPos pos;
        final UUID playerId;
        final boolean wasPoweredOnClick;
        final int checkTick;

        PendingButtonPressCheck(ServerLevel level, BlockPos pos, UUID playerId, boolean wasPoweredOnClick, int checkTick) {
            this.level = level;
            this.pos = pos;
            this.playerId = playerId;
            this.wasPoweredOnClick = wasPoweredOnClick;
            this.checkTick = checkTick;
        }
    }

    private static final class PendingContainerInsertCheck {
        final ServerLevel level;
        final BlockPos pos;
        final UUID playerId;
        final Item targetItem;
        final String questId;
        final int expireTick;

        PendingContainerInsertCheck(ServerLevel level, BlockPos pos, UUID playerId, Item targetItem, String questId, int expireTick) {
            this.level = level;
            this.pos = pos;
            this.playerId = playerId;
            this.targetItem = targetItem;
            this.questId = questId;
            this.expireTick = expireTick;
        }
    }

    private static final class PendingCraftingTableInsertCheck {
        final UUID playerId;
        final Item targetItem;
        final String questId;
        final int expireTick;

        PendingCraftingTableInsertCheck(UUID playerId, Item targetItem, String questId, int expireTick) {
            this.playerId = playerId;
            this.targetItem = targetItem;
            this.questId = questId;
            this.expireTick = expireTick;
        }
    }

    private static final class PendingItemLavaCheck {
        final ServerLevel level;
        final UUID itemEntityId;
        final UUID playerId;
        final String questId;
        final int expireTick;

        PendingItemLavaCheck(ServerLevel level, UUID itemEntityId, UUID playerId, String questId, int expireTick) {
            this.level = level;
            this.itemEntityId = itemEntityId;
            this.playerId = playerId;
            this.questId = questId;
            this.expireTick = expireTick;
        }
    }

    private static final class SaplingOwner {
        final UUID playerId;
        final ResourceKey<Level> dimension;
        final int expireTick;

        SaplingOwner(UUID playerId, ResourceKey<Level> dimension, int expireTick) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.expireTick = expireTick;
        }
    }

    private static final class LoveOwner {
        final UUID playerId;
        final int expireTick;

        LoveOwner(UUID playerId, int expireTick) {
            this.playerId = playerId;
            this.expireTick = expireTick;
        }
    }

    private static final class PendingProjectileKill {
        final UUID playerId;
        final String type;
        final int expireTick;

        PendingProjectileKill(UUID playerId, String type, int expireTick) {
            this.playerId = playerId;
            this.type = type;
            this.expireTick = expireTick;
        }
    }
}











