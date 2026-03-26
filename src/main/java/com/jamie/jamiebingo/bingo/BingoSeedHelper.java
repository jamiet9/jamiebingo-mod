package com.jamie.jamiebingo.bingo;

import com.jamie.jamiebingo.casino.CasinoModeManager;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.bingo.WinCondition;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class BingoSeedHelper {

    private BingoSeedHelper() {
    }

    public static boolean startFromSeed(CommandSourceStack source, String seed) {
        if (source == null) return false;
        MinecraftServer server = source.getServer();
        if (server == null) return false;

        BingoGameData data = BingoGameData.get(server);
        CardSeedCodec.SeedData seedData = CardSeedCodec.decode(seed);

        if (seedData == null) {
            com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(source, 
                    com.jamie.jamiebingo.util.ComponentUtil.literal("Invalid seed.")
            );
            return false;
        }

        if (data.isActive()) {
            data.stopGame();
        }

        var settings = seedData.settings();
        String diffKey = settings.getString("GameDifficulty").orElse("");
        net.minecraft.world.Difficulty diff = switch (diffKey) {
            case "easy" -> net.minecraft.world.Difficulty.EASY;
            case "hard" -> net.minecraft.world.Difficulty.HARD;
            default -> net.minecraft.world.Difficulty.NORMAL;
        };
        server.setDifficulty(diff, true);

        int casinoMode = BingoGameData.CASINO_DISABLED;
        data.casinoMode = casinoMode;
        data.minesEnabled = settings.getBoolean("MinesEnabled").orElse(false);
        data.mineAmount = Math.max(1, Math.min(13, settings.getInt("MineAmount").orElse(1)));
        data.mineTimeSeconds = Math.max(1, settings.getInt("MineTimeSeconds").orElse(15));
        data.rerollsPerPlayer = 0;
        CasinoModeManager.setCasinoEnabled(false);

        boolean ok = data.startGameFromSeed(server, seedData);
        if (!ok) {
            com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(source, 
                    com.jamie.jamiebingo.util.ComponentUtil.literal("Failed to start from seed.")
            );
            return false;
        }

        // Seed replays must reproduce the saved card exactly, without casino/draft/reroll phases.
        data.casinoMode = BingoGameData.CASINO_DISABLED;
        data.rerollsPerPlayer = 0;
        CasinoModeManager.setCasinoEnabled(false);
        data.endRerollPhase();
        data.startCountdownOrFinalize(server);
        com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(source, 
                () -> com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Started game from seed."),
                true
        );
        return true;
    }

    public static java.util.List<String> buildSettingsLinesFromSeed(MinecraftServer server, CardSeedCodec.SeedData seedData) {
        return buildSettingsLinesFromSeed(server, seedData, "");
    }

    public static java.util.List<String> buildSettingsLinesFromSeed(MinecraftServer server, CardSeedCodec.SeedData seedData, String worldSeedTextOverride) {
        if (seedData == null) return java.util.List.of();
        var settings = seedData.settings();
        java.util.List<String> lines = new java.util.ArrayList<>();

        String win = settings.getString("Win").orElse("");
        String composition = settings.getString("Composition").orElse("");
        int size = settings.getInt("Size").orElse(0);
        int questPercent = settings.getInt("QuestPercent").orElse(0);
        boolean categoryLogicEnabled = settings.getBoolean("CategoryLogicEnabled").orElse(true);
        boolean rarityLogicEnabled = settings.getBoolean("RarityLogicEnabled").orElse(true);
        boolean itemColorVariantsSeparate = settings.getBoolean("ItemColorVariantsSeparate").orElse(true);
        int gunRounds = settings.getInt("GunRounds").orElse(0);
        int hangmanRounds = settings.getInt("HangmanRounds").orElse(0);
        int hangmanSeconds = settings.getInt("HangmanBaseSeconds").orElse(0);
        int hangmanPenalty = settings.getInt("HangmanPenaltySeconds").orElse(0);
        int casinoMode = settings.contains("CasinoMode")
                ? settings.getInt("CasinoMode").orElse(0)
                : (settings.getBoolean("Casino").orElse(false) ? BingoGameData.CASINO_ENABLED : BingoGameData.CASINO_DISABLED);
        int rerolls = settings.getInt("Rerolls").orElse(0);
        boolean fakeRerollsEnabled = settings.getBoolean("FakeRerollsEnabled").orElse(false);
        int fakeRerollsPerPlayer = settings.getInt("FakeRerollsPerPlayer").orElse(0);
        boolean minesEnabled = settings.getBoolean("MinesEnabled").orElse(false);
        int mineAmount = settings.getInt("MineAmount").orElse(1);
        int mineTimeSeconds = settings.getInt("MineTimeSeconds").orElse(15);
        boolean powerSlotEnabled = settings.getBoolean("PowerSlotEnabled").orElse(false);
        int powerSlotIntervalSeconds = settings.getInt("PowerSlotIntervalSeconds").orElse(0);
        int effectsInterval = settings.getInt("EffectsInterval").orElse(0);
        boolean rtp = settings.getBoolean("Rtp").orElse(false);
        boolean pvp = settings.getBoolean("Pvp").orElse(false);
        boolean hostile = settings.getBoolean("HostileMobs").orElse(false);
        boolean hunger = settings.getBoolean("Hunger").orElse(false);
        boolean naturalRegen = settings.getBoolean("NaturalRegen").orElse(true);
        boolean adventureMode = settings.getBoolean("AdventureMode").orElse(false);
        int prelitPortalsMode = settings.getInt("PrelitPortalsMode").orElse(BingoGameData.PRELIT_PORTALS_OFF);
        boolean keepInv = settings.getBoolean("KeepInv").orElse(false);
        boolean hardcore = settings.getBoolean("Hardcore").orElse(false);
        int daylight = settings.getInt("Daylight").orElse(0);
        int registerMode = settings.getInt("RegisterMode").orElse(0);
        boolean allowLateJoin = settings.getBoolean("AllowLateJoin").orElse(false);
        boolean shuffleEnabled = settings.getBoolean("ShuffleEnabled").orElse(false);
        boolean teamChestEnabled = settings.getBoolean("TeamChestEnabled").orElse(true);
        boolean rushEnabled = settings.getBoolean("RushEnabled").orElse(false);
        int rushSeconds = settings.getInt("RushSeconds").orElse(0);
        int startDelay = settings.getInt("StartDelay").orElse(0);
        boolean countdownEnabled = settings.getBoolean("CountdownEnabled").orElse(false);
        int countdownMinutes = settings.getInt("CountdownMinutes").orElse(0);
        boolean teamSyncEnabled = settings.getBoolean("TeamSyncEnabled").orElse(false);
        int starterKitMode = settings.getInt("StarterKitMode").orElse(0);
        boolean hideGoalDetailsInChat = settings.getBoolean("HideGoalDetailsInChat").orElse(false);
        String difficulty = settings.getString("Difficulty").orElse("");
        boolean customCard = settings.contains("CustomCardEnabled") && settings.getBoolean("CustomCardEnabled").orElse(false);
        boolean customPool = settings.contains("CustomPoolEnabled") && settings.getBoolean("CustomPoolEnabled").orElse(false);
        int poolCount = settings.getList("CustomPoolIds").map(net.minecraft.nbt.ListTag::size).orElse(0);
        int worldTypeMode = settings.getInt("WorldTypeMode").orElse(0);
        int worldCustomBiomeSize = settings.getInt("WorldCustomBiomeSizeBlocks").orElse(96);
        int worldTerrainHillinessPercent = settings.getInt("WorldTerrainHillinessPercent").orElse(50);
        int worldStructureFrequencyPercent = settings.getInt("WorldStructureFrequencyPercent").orElse(100);
        String worldSingleBiome = settings.getString("WorldSingleBiomeId").orElse("minecraft:plains");
        boolean worldSurfaceCave = settings.getBoolean("WorldSurfaceCaveBiomes").orElse(false);
        String worldSetSeedText = settings.getString("WorldSetSeedText").orElse("");
        boolean newSeedEachGame = settings.getBoolean("WorldUseNewSeedEachGame").orElse(false);
        var parsedWorldSeed = com.jamie.jamiebingo.world.WorldRegenerationManager.decodeSettingsSeed(
                worldSeedTextOverride == null || worldSeedTextOverride.isBlank() ? worldSetSeedText : worldSeedTextOverride
        );
        if (parsedWorldSeed != null) {
            worldTypeMode = parsedWorldSeed.worldTypeMode();
            worldCustomBiomeSize = parsedWorldSeed.biomeSizeBlocks();
            worldTerrainHillinessPercent = parsedWorldSeed.terrainHillinessPercent();
            worldStructureFrequencyPercent = parsedWorldSeed.structureFrequencyPercent();
            worldSingleBiome = parsedWorldSeed.singleBiomeId();
            worldSurfaceCave = parsedWorldSeed.surfaceCaveBiomes();
            worldSetSeedText = worldSeedTextOverride == null || worldSeedTextOverride.isBlank()
                    ? worldSetSeedText
                    : worldSeedTextOverride;
            newSeedEachGame = parsedWorldSeed.newSeedEachGame();
            adventureMode = parsedWorldSeed.adventureMode();
            prelitPortalsMode = parsedWorldSeed.prelitPortalsMode();
        }

        lines.add("Mode: " + win);
        lines.add("Card Size: " + size);

        if ("HYBRID_CATEGORY".equalsIgnoreCase(composition)) {
            lines.add("Quests: Category");
        } else if ("HYBRID_PERCENT".equalsIgnoreCase(composition)) {
            lines.add("Quests: " + questPercent + "%");
        } else {
            lines.add("Quests: Disabled");
        }
        lines.add("Category Logic: " + (categoryLogicEnabled ? "Enabled" : "Disabled"));
        lines.add("Rarity Logic: " + (rarityLogicEnabled ? "Enabled" : "Disabled"));
        lines.add("Item Color Variants: " + (itemColorVariantsSeparate ? "Separate" : "Grouped"));

        if ("GUNGAME".equalsIgnoreCase(win) || "GAMEGUN".equalsIgnoreCase(win)) {
            lines.add("Gun Rounds: " + gunRounds);
        }
        if ("HANGMAN".equalsIgnoreCase(win)) {
            lines.add("Hangman Rounds: " + hangmanRounds);
            lines.add("Hangman Time: " + hangmanSeconds + "s");
            lines.add("Hangman Penalty: " + hangmanPenalty + "s");
        }

        String casinoLabel = switch (casinoMode) {
            case BingoGameData.CASINO_ENABLED -> "Enabled";
            case BingoGameData.CASINO_DRAFT -> "Draft";
            default -> "Disabled";
        };
        lines.add("Casino: " + casinoLabel);
        lines.add("Rerolls: " + (rerolls > 0 ? rerolls : "Disabled"));
        lines.add("Fake Rerolls: " + (fakeRerollsEnabled ? fakeRerollsPerPlayer : "Disabled"));
        lines.add("Shuffle: " + (shuffleEnabled ? "Enabled" : "Disabled"));
        lines.add("Mines: " + (minesEnabled ? "Enabled" : "Disabled"));
        if (minesEnabled) {
            lines.add("Mine Amount: " + Math.max(1, Math.min(13, mineAmount)));
            lines.add("Mine Time: " + Math.max(1, mineTimeSeconds) + "s");
        }
        lines.add("Power Slot: " + (powerSlotEnabled ? "Enabled" : "Disabled"));
        if (powerSlotEnabled) {
            lines.add("Power Slot Interval: " + Math.max(1, powerSlotIntervalSeconds) + "s");
        }

        lines.add("Effects: " + (effectsInterval > 0 ? (effectsInterval + "s") : "Disabled"));
        lines.add("RTP: " + (rtp ? "Enabled" : "Disabled"));
        lines.add("PVP: " + (pvp ? "Enabled" : "Disabled"));
        lines.add("Adventure: " + (adventureMode ? "Enabled" : "Disabled"));
        lines.add("Prelit Portals: " + prelitPortalsLabel(prelitPortalsMode));
        lines.add("Hostile Mobs: " + (hostile ? "Enabled" : "Disabled"));
        lines.add("Hunger: " + (hunger ? "Enabled" : "Disabled"));
        lines.add("Natural Regen: " + (naturalRegen ? "On" : "Off"));
        lines.add("Keep Inventory: " + (keepInv ? "Enabled" : "Disabled"));
        lines.add("Hardcore: " + (hardcore ? "Enabled" : "Disabled"));
        lines.add("Daylight: " + daylightLabel(daylight));
        lines.add("Register: " + registerLabel(registerMode));
        lines.add("Team Sync: " + (teamSyncEnabled ? "Enabled" : "Disabled"));
        lines.add("Team Chest: " + (teamChestEnabled ? "Enabled" : "Disabled"));
        lines.add("Starter Kit: " + starterKitLabel(starterKitMode));
        lines.add("Hide Goal Details: " + (hideGoalDetailsInChat ? "Enabled" : "Disabled"));
        lines.add("Late Join: " + (allowLateJoin ? "Enabled" : "Disabled"));
        lines.add("Rush: " + (rushEnabled ? (Math.max(1, rushSeconds) + "s") : "Disabled"));
        lines.add("Start Delay: " + (startDelay > 0 ? (startDelay + "s") : "Disabled"));
        lines.add("Countdown: " + (countdownEnabled ? (countdownMinutes + "m") : "Disabled"));
        lines.add("Card Difficulty: " + (!difficulty.isBlank() ? difficulty : "normal"));
        if (customPool) {
            lines.add("Custom Pool: Enabled (" + poolCount + ")");
        } else if (customCard) {
            lines.add("Custom Card: Enabled");
        }
        lines.add("New Seed Every Game: " + (newSeedEachGame ? "Enabled" : "Disabled"));
        lines.add("World Type: " + worldTypeLabel(worldTypeMode));
        if (worldTypeMode == BingoGameData.WORLD_TYPE_CUSTOM_BIOME_SIZE || worldTypeMode == BingoGameData.WORLD_TYPE_SMALL_BIOMES) {
            int biomeSize = BingoGameData.clampWorldBiomeSize(worldCustomBiomeSize);
            double t = (biomeSize - 40) / 60.0D;
            t = Math.max(0.0D, Math.min(1.0D, t));
            int pct = 1 + (int) Math.round(99.0D * t);
            lines.add("Custom Biome Size: " + pct + "%");
            int hilliness = BingoGameData.clampWorldTerrainHilliness(worldTerrainHillinessPercent);
            lines.add("World Hilliness: " + (hilliness == 50 ? "Default" : (hilliness + "%")));
        }
        lines.add("World Structure Frequency: " + BingoGameData.clampWorldStructureFrequency(worldStructureFrequencyPercent) + "%");
        if (worldTypeMode == BingoGameData.WORLD_TYPE_SINGLE_BIOME) {
            lines.add("World Single Biome: " + worldSingleBiome);
        }
        lines.add("World Surface Cave Biomes: " + (worldSurfaceCave ? "Enabled" : "Disabled"));
        if (!worldSetSeedText.isBlank()) {
            lines.add("World Set Seed: " + worldSetSeedText);
        }

        if (server != null) {
            lines.add("Game Difficulty: " + com.jamie.jamiebingo.util.ServerWorldDataUtil.getDifficultyKey(server));
        }

        return lines;
    }

    public static boolean shouldMaskPreview(CardSeedCodec.SeedData seedData) {
        if (seedData == null) return false;
        var settings = seedData.settings();
        if (settings == null) return false;
        String win = settings.getString("Win").orElse("");
        return "BLIND".equalsIgnoreCase(win) || "HANGMAN".equalsIgnoreCase(win);
    }

    private static String daylightLabel(int daylightMode) {
        return switch (daylightMode) {
            case BingoGameData.DAYLIGHT_DAY -> "Day";
            case BingoGameData.DAYLIGHT_NIGHT -> "Night";
            case BingoGameData.DAYLIGHT_MIDNIGHT -> "Midnight";
            case BingoGameData.DAYLIGHT_DAWN -> "Dawn";
            case BingoGameData.DAYLIGHT_DUSK -> "Dusk";
            default -> "Enabled";
        };
    }

    private static String registerLabel(int registerMode) {
        return registerMode == BingoGameData.REGISTER_ALWAYS_HAVE
                ? "Always Have"
                : "Collect Once";
    }

    private static String prelitPortalsLabel(int mode) {
        return switch (BingoGameData.clampPrelitPortalsMode(mode)) {
            case BingoGameData.PRELIT_PORTALS_NETHER -> "Nether";
            case BingoGameData.PRELIT_PORTALS_END -> "End";
            case BingoGameData.PRELIT_PORTALS_BOTH -> "Both";
            default -> "Off";
        };
    }

    private static String starterKitLabel(int starterKitMode) {
        int normalized = switch (starterKitMode) {
            case BingoGameData.STARTER_KIT_MINIMAL,
                 BingoGameData.STARTER_KIT_AVERAGE,
                 BingoGameData.STARTER_KIT_OP -> starterKitMode;
            default -> BingoGameData.STARTER_KIT_DISABLED;
        };
        return switch (normalized) {
            case BingoGameData.STARTER_KIT_MINIMAL -> "Minimal";
            case BingoGameData.STARTER_KIT_AVERAGE -> "Average";
            case BingoGameData.STARTER_KIT_OP -> "OP";
            default -> "Disabled";
        };
    }

    private static String worldTypeLabel(int mode) {
        return switch (mode) {
            case BingoGameData.WORLD_TYPE_AMPLIFIED -> "Amplified";
            case BingoGameData.WORLD_TYPE_SUPERFLAT -> "Superflat";
            case BingoGameData.WORLD_TYPE_CUSTOM_BIOME_SIZE, BingoGameData.WORLD_TYPE_SMALL_BIOMES -> "Custom Biome Size";
            case BingoGameData.WORLD_TYPE_SINGLE_BIOME -> "Single Biome";
            default -> "Normal";
        };
    }
}
