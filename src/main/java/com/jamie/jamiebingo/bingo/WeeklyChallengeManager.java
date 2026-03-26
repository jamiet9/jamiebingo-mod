package com.jamie.jamiebingo.bingo;

import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.world.WorldRegenerationManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.Random;

public final class WeeklyChallengeManager {
    public static final long RESET_ANCHOR_EPOCH_SECONDS = 1774537841L;
    public static final long RESET_PERIOD_SECONDS = 7L * 24L * 60L * 60L;

    private WeeklyChallengeManager() {
    }

    public static long currentChallengeSeed() {
        long now = System.currentTimeMillis() / 1000L;
        if (now <= RESET_ANCHOR_EPOCH_SECONDS) {
            return RESET_ANCHOR_EPOCH_SECONDS;
        }
        long elapsed = now - RESET_ANCHOR_EPOCH_SECONDS;
        long cycles = elapsed / RESET_PERIOD_SECONDS;
        return RESET_ANCHOR_EPOCH_SECONDS + cycles * RESET_PERIOD_SECONDS;
    }

    public static long nextResetEpochSeconds(long baseSeed) {
        return baseSeed + RESET_PERIOD_SECONDS;
    }

    public static String challengeId(long baseSeed) {
        return "weekly-" + baseSeed;
    }

    public static WeeklyChallenge build(MinecraftServer server, long baseSeed) {
        if (server == null) return null;
        return buildInternal(server, baseSeed);
    }

    public static WeeklyChallenge buildServerless(long baseSeed) {
        return buildInternal(null, baseSeed);
    }

    private static WeeklyChallenge buildInternal(MinecraftServer server, long baseSeed) {
        Random rng = new Random(baseSeed);
        BingoGameData data = new BingoGameData();
        data.worldUseNewSeedEachGame = true;

        applyWeeklyGameplayRandomization(data, rng, baseSeed);
        applyWeeklyWorldRandomization(data, rng);

        long worldSeedValue = rng.nextLong();
        String worldSeed = WorldRegenerationManager.encodeSettingsSeed(worldSeedValue, data);
        data.worldSetSeedText = worldSeed;

        Random cardRng = new Random(baseSeed ^ 0x5DEECE66DL);
        BingoCard card = ConfigurableCardGenerator.generate(
                data.size,
                data.difficulty,
                data.composition,
                data.questPercent,
                server,
                data,
                data.getGenerationBlacklistForPreview(),
                cardRng
        );
        if (card == null) {
            return null;
        }

        CompoundTag settingsTag = SettingsSeedCodec.createSettingsTag(data, server);
        if (data.minesEnabled) {
            net.minecraft.nbt.ListTag selectedMineTag = new net.minecraft.nbt.ListTag();
            for (String id : com.jamie.jamiebingo.mines.MineModeManager.selectedSourceQuestIds(data, new Random(baseSeed ^ 0x4D494E45L))) {
                if (id == null || id.isBlank()) continue;
                selectedMineTag.add(net.minecraft.nbt.StringTag.valueOf(id));
            }
            if (!selectedMineTag.isEmpty()) {
                com.jamie.jamiebingo.util.NbtUtil.putTag(settingsTag, "SelectedMineSourceIds", selectedMineTag);
            }
        }

        String cardSeed = CardSeedCodec.encode(settingsTag, List.of(card));
        CardSeedCodec.SeedData seedData = CardSeedCodec.decode(cardSeed);
        if (seedData == null) {
            return null;
        }
        String settingsSeed = server == null
                ? SettingsSeedCodec.fromCardSeed(cardSeed)
                : SettingsSeedCodec.encode(data, server);
        if (settingsSeed == null || settingsSeed.isBlank()) {
            settingsSeed = SettingsSeedCodec.fromCardSeed(cardSeed);
        }
        List<String> settingsLines = BingoSeedHelper.buildSettingsLinesFromSeed(server, seedData, worldSeed);
        return new WeeklyChallenge(
                baseSeed,
                challengeId(baseSeed),
                nextResetEpochSeconds(baseSeed),
                settingsSeed,
                worldSeed,
                cardSeed,
                card,
                settingsLines
        );
    }

    public static boolean matchesCurrentWeekly(MinecraftServer server, String settingsSeed, String worldSeed, String cardSeed) {
        WeeklyChallenge weekly = build(server, currentChallengeSeed());
        if (weekly == null) return false;
        return weekly.settingsSeed.equals(settingsSeed == null ? "" : settingsSeed)
                && weekly.worldSeed.equals(worldSeed == null ? "" : worldSeed)
                && weekly.cardSeed.equals(cardSeed == null ? "" : cardSeed);
    }

    public static boolean startWeeklyChallenge(MinecraftServer server, long baseSeed) {
        if (server == null) return false;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || data.isActive()) return false;
        WeeklyChallenge weekly = build(server, baseSeed);
        if (weekly == null) return false;

        applyWorldSeed(data, weekly.worldSeed);
        data.setPendingWeeklyChallengeStart(baseSeed);
        data.worldFreshSeedPrepared = false;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        WorldRegenerationManager.queueRegeneration(server, "weekly_challenge");
        return true;
    }

    public static void finalizePendingWeeklyChallenge(MinecraftServer server, long baseSeed) {
        if (server == null) return;
        WeeklyChallenge weekly = build(server, baseSeed);
        if (weekly == null) return;
        CardSeedCodec.SeedData seedData = CardSeedCodec.decode(weekly.cardSeed);
        if (seedData == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (data == null || data.isActive()) return;
        data.clearPendingWeeklyChallengeStart();
        if (!data.startGameFromSeed(server, seedData)) {
            return;
        }
        data.casinoMode = BingoGameData.CASINO_DISABLED;
        data.rerollsPerPlayer = 0;
        com.jamie.jamiebingo.casino.CasinoModeManager.setCasinoEnabled(false);
        data.endRerollPhase();
        data.startCountdownOrFinalize(server);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
    }

    public static boolean startPublishedWeeklyChallenge(MinecraftServer server, String challengeId, String settingsSeed, String worldSeed, String cardSeed) {
        if (server == null || cardSeed == null || cardSeed.isBlank()) return false;

        BingoGameData data = BingoGameData.get(server);
        if (data == null || data.isActive()) return false;

        CardSeedCodec.SeedData seedData = CardSeedCodec.decode(cardSeed);
        if (seedData == null) return false;

        applyWorldSeed(data, worldSeed);
        data.clearPendingWeeklyChallengeStart();
        if (!data.startGameFromSeed(server, seedData)) {
            return false;
        }

        data.casinoMode = BingoGameData.CASINO_DISABLED;
        data.rerollsPerPlayer = 0;
        data.activeWeeklyChallengeId = challengeId == null ? "" : challengeId;
        data.activeWeeklySettingsSeed = settingsSeed == null || settingsSeed.isBlank()
                ? SettingsSeedCodec.fromCardSeed(cardSeed)
                : settingsSeed;
        data.activeWeeklyWorldSeed = worldSeed == null ? "" : worldSeed;
        data.activeWeeklyCardSeed = cardSeed;
        com.jamie.jamiebingo.casino.CasinoModeManager.setCasinoEnabled(false);
        data.endRerollPhase();
        data.startCountdownOrFinalize(server);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        return true;
    }

    private static void applyWeeklyGameplayRandomization(BingoGameData data, Random rng, long baseSeed) {
        data.winCondition = rng.nextDouble() < 0.60d ? WinCondition.FULL : WinCondition.LINE;
        data.composition = CardComposition.HYBRID_CATEGORY;
        data.questPercent = 35;
        data.categoryLogicEnabled = rng.nextBoolean();
        data.rarityLogicEnabled = rng.nextBoolean();
        data.itemColorVariantsSeparate = false;
        data.randomEffectsArmed = rng.nextBoolean();
        data.randomEffectsIntervalSeconds = data.randomEffectsArmed ? pickRandomEffectInterval(rng) : 0;
        data.rtpEnabled = rng.nextBoolean();
        data.hostileMobsEnabled = rng.nextBoolean();
        data.hungerEnabled = rng.nextBoolean();
        data.naturalRegenEnabled = rng.nextDouble() < 0.90d;
        data.size = pickWeeklyCardSize(rng);
        data.keepInventoryEnabled = rng.nextBoolean();
        data.hardcoreEnabled = rng.nextDouble() < 0.10d;
        data.daylightMode = pickWeeklyDaylight(rng);
        data.pvpEnabled = rng.nextBoolean();
        data.adventureMode = false;
        data.registerMode = rng.nextDouble() < 0.70d
                ? BingoGameData.REGISTER_ALWAYS_HAVE
                : BingoGameData.REGISTER_COLLECT_ONCE;
        data.shuffleEnabled = rng.nextBoolean();
        data.difficulty = switch (rng.nextInt(3)) {
            case 0 -> "easy";
            case 1 -> "normal";
            default -> "hard";
        };
        data.casinoMode = BingoGameData.CASINO_DISABLED;
        data.rerollsPerPlayer = 0;
        data.fakeRerollsEnabled = false;
        data.fakeRerollsPerPlayer = 2;
        data.minesEnabled = new Random(baseSeed ^ 0x4D494E45534C4F4EL).nextBoolean();
        data.mineAmount = 1;
        data.mineTimeSeconds = 60;
        data.powerSlotEnabled = rng.nextBoolean();
        data.powerSlotIntervalSeconds = pickRandomEffectInterval(rng);
        data.rushEnabled = rng.nextBoolean();
        data.rushSeconds = 60;
        data.teamSyncEnabled = true;
        data.teamChestEnabled = false;
        data.allowLateJoin = rng.nextBoolean();
        data.countdownEnabled = false;
        data.countdownMinutes = 10;
        data.bingoStartDelaySeconds = 0;
        data.starterKitMode = BingoGameData.STARTER_KIT_DISABLED;
        data.hideGoalDetailsInChat = false;

        if (!isShuffleSupportedWinCondition(data.winCondition)) {
            data.shuffleEnabled = false;
        }
        if (!BingoGameData.isPowerSlotSupportedWinCondition(data.winCondition)) {
            data.powerSlotEnabled = false;
        }
        if (data.winCondition == WinCondition.HANGMAN) {
            data.size = 1;
            data.rushEnabled = false;
            data.shuffleEnabled = false;
            data.fakeRerollsEnabled = false;
        }
    }

    private static void applyWeeklyWorldRandomization(BingoGameData data, Random rng) {
        double typeRoll = rng.nextDouble();
        if (typeRoll < 0.40d) {
            data.worldTypeMode = BingoGameData.WORLD_TYPE_NORMAL;
        } else if (typeRoll < 0.50d) {
            data.worldTypeMode = BingoGameData.WORLD_TYPE_AMPLIFIED;
        } else {
            data.worldTypeMode = BingoGameData.WORLD_TYPE_CUSTOM_BIOME_SIZE;
        }
        data.worldSingleBiomeId = "minecraft:plains";
        data.worldSurfaceCaveBiomes = rng.nextDouble() >= 0.60d;
        double prelitRoll = rng.nextDouble();
        if (prelitRoll < 0.60d) {
            data.prelitPortalsMode = BingoGameData.PRELIT_PORTALS_OFF;
        } else {
            int offset = rng.nextInt(3);
            data.prelitPortalsMode = switch (offset) {
                case 0 -> BingoGameData.PRELIT_PORTALS_NETHER;
                case 1 -> BingoGameData.PRELIT_PORTALS_END;
                default -> BingoGameData.PRELIT_PORTALS_BOTH;
            };
        }
        data.worldCustomBiomeSizeBlocks = biasedPercentToBiomeSize(sampleBiasedPercent(rng, 40));
        data.worldTerrainHillinessPercent = sampleBiasedPercent(rng, 40);
        data.worldStructureFrequencyPercent = sampleBiasedStructureFrequency(rng, 200);
    }

    private static void applyWorldSeed(BingoGameData data, String worldSeed) {
        if (data == null) return;
        WorldRegenerationManager.ParsedSettingsSeed parsed = WorldRegenerationManager.decodeSettingsSeed(worldSeed);
        if (parsed == null) return;
        data.worldUseNewSeedEachGame = parsed.newSeedEachGame();
        data.worldTypeMode = parsed.worldTypeMode();
        data.worldSmallBiomes = parsed.smallBiomes();
        data.worldSurfaceCaveBiomes = parsed.surfaceCaveBiomes();
        data.worldSingleBiomeId = parsed.singleBiomeId();
        data.worldCustomBiomeSizeBlocks = parsed.biomeSizeBlocks();
        data.worldTerrainHillinessPercent = parsed.terrainHillinessPercent();
        data.worldStructureFrequencyPercent = parsed.structureFrequencyPercent();
        data.adventureMode = parsed.adventureMode();
        data.prelitPortalsMode = parsed.prelitPortalsMode();
        data.worldSetSeedText = worldSeed == null ? "" : worldSeed;
    }

    private static int pickWeeklyCardSize(Random rng) {
        int[] sizes = {1,2,3,4,5,6,7,8,9,10};
        double[] weights = new double[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            int d = sizes[i] - 5;
            weights[i] = Math.exp(-0.6 * d * d);
        }
        return weightedPick(sizes, weights, rng);
    }

    private static int pickWeeklyDaylight(Random rng) {
        int roll = rng.nextInt(6);
        return switch (roll) {
            case 1 -> BingoGameData.DAYLIGHT_DAY;
            case 2 -> BingoGameData.DAYLIGHT_NIGHT;
            case 3 -> BingoGameData.DAYLIGHT_MIDNIGHT;
            case 4 -> BingoGameData.DAYLIGHT_DAWN;
            case 5 -> BingoGameData.DAYLIGHT_DUSK;
            default -> BingoGameData.DAYLIGHT_ENABLED;
        };
    }

    private static int pickRandomEffectInterval(Random rng) {
        double value = 40 + rng.nextGaussian() * 35.0;
        return (int) Math.round(Math.max(20, Math.min(300, value)));
    }

    private static int pickWeeklyCountdownMinutes(Random rng) {
        double value = 10 + rng.nextGaussian() * 4.0;
        return Math.max(1, Math.min(60, (int) Math.round(value)));
    }

    private static int sampleBiasedPercent(Random rng, int target) {
        double raw = target + rng.nextGaussian() * 18.0;
        return Math.max(0, Math.min(100, (int) Math.round(raw)));
    }

    private static int sampleBiasedStructureFrequency(Random rng, int target) {
        double raw = target + rng.nextGaussian() * 55.0;
        return BingoGameData.clampWorldStructureFrequency((int) Math.round(raw));
    }

    private static int biasedPercentToBiomeSize(int percent) {
        double t = Math.max(0.0d, Math.min(1.0d, (percent - 1) / 99.0d));
        int blocks = 40 + (int) Math.round(60.0d * t);
        return BingoGameData.clampWorldBiomeSize(blocks);
    }

    private static int weightedPick(int[] values, double[] weights, Random rng) {
        double total = 0.0d;
        for (double weight : weights) total += weight;
        double roll = rng.nextDouble() * total;
        double upto = 0.0d;
        for (int i = 0; i < values.length; i++) {
            upto += weights[i];
            if (upto >= roll) return values[i];
        }
        return values[0];
    }

    private static boolean isShuffleSupportedWinCondition(WinCondition winCondition) {
        return winCondition == WinCondition.FULL
                || winCondition == WinCondition.LOCKOUT
                || winCondition == WinCondition.RARITY;
    }

    public record WeeklyChallenge(
            long baseSeed,
            String challengeId,
            long nextResetEpochSeconds,
            String settingsSeed,
            String worldSeed,
            String cardSeed,
            BingoCard card,
            List<String> settingsLines
    ) {
    }
}




