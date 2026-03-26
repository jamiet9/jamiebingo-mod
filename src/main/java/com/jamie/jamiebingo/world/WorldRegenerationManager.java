package com.jamie.jamiebingo.world;

import com.google.common.collect.ImmutableList;
import com.jamie.jamiebingo.casino.CasinoTickScheduler;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.util.ComponentUtil;
import com.jamie.jamiebingo.util.SavedDataUtil;
import com.jamie.jamiebingo.util.ServerTickUtil;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Stopwatches;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.storage.CommandStorage;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.scores.ScoreboardSaveData;
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.entity.npc.CatSpawner;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;
import net.minecraftforge.event.ForgeEventFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileVisitResult;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

public final class WorldRegenerationManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SETTINGS_SEED_PREFIX = "jbws1:";
    private static final Map<ResourceKey<StructureSet>, StructurePlacement> ORIGINAL_STRUCTURE_PLACEMENTS = new HashMap<>();

    private WorldRegenerationManager() {}

    public static boolean ensureFreshSeedPrepared(MinecraftServer server, BingoGameData data, String reason) {
        if (server == null || data == null) return false;
        if (!data.worldUseNewSeedEachGame) return false;
        if (data.isActive()) return false;
        if (data.worldFreshSeedPrepared) return false;
        if (data.worldRegenInProgress || data.worldRegenQueued) return true;
        queueRegeneration(server, reason == null || reason.isBlank() ? "prepare_seed" : reason);
        return true;
    }

    public static void queueRegeneration(MinecraftServer server, String reason) {
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null) return;
        if (data.worldRegenInProgress || data.worldRegenQueued) {
            broadcast(server, "[Bingo] Dimension regeneration already " + regenStatusText(data) + ".");
            return;
        }

        data.worldRegenQueued = true;
        data.worldRegenStage = "queued";
        SavedDataUtil.markDirty(data);
        broadcast(server, "[Bingo] Queued dimension regeneration...");
        int runAt = ServerTickUtil.getTickCount(server) + 5;
        CasinoTickScheduler.schedule(server, runAt, () -> regenerateNow(server, reason));
    }

    private static void regenerateNow(MinecraftServer server, String reason) {
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null) return;
        if (data.isActive()) {
            data.worldRegenQueued = false;
            data.worldRegenStage = "";
            SavedDataUtil.markDirty(data);
            broadcast(server, "[Bingo] Regeneration aborted: game is active.");
            return;
        }
        if (data.worldRegenInProgress) return;

        data.worldRegenInProgress = true;
        data.worldRegenQueued = false;
        data.worldRegenStage = "starting";
        SavedDataUtil.markDirty(data);
        broadcast(server, "[Bingo] Regenerating game dimensions" + (reason == null || reason.isBlank() ? "" : " (" + reason + ")") + "...");

        try {
            if (LobbyWorldManager.getLobby(server) == null) {
                broadcast(server, "[Bingo] Regeneration aborted: lobby dimension is not loaded.");
                return;
            }
            RuntimeHandles handles = resolveRuntimeHandles(server);
            data.worldRegenStage = "moving_players";
            SavedDataUtil.markDirty(data);
            broadcast(server, "[Bingo] Regen 1/6: moving non-lobby players to spawnbox...");
            ensureAllPlayersInLobbySpawnbox(server);

            data.worldRegenStage = "saving";
            SavedDataUtil.markDirty(data);
            broadcast(server, "[Bingo] Regen 2/6: saving player data...");
            server.getPlayerList().saveAll();

            WorldData worldData = server.getWorldData();
            long baseSeed = worldData.worldGenOptions().seed();
            long seed;
            boolean forceConfiguredSeed = reason != null && reason.contains("force_seed");
            if (forceConfiguredSeed) {
                seed = resolveConfiguredSeed(data);
                if (seed == Long.MIN_VALUE) {
                    // "Force seed" with an empty/invalid textbox should still generate a fresh seed.
                    seed = WorldOptions.randomSeed();
                }
            } else if (!data.worldUseNewSeedEachGame) {
                seed = baseSeed;
            } else {
                // In fresh-seed mode, automatic regenerations must always pick a new seed.
                // Textbox seeds are only for explicit "Generate From Set Seed" requests.
                seed = WorldOptions.randomSeed();
            }
            if (seed == baseSeed) {
                // Extremely unlikely, but avoid accidental no-op seed regenerations.
                long next = WorldOptions.randomSeed();
                if (next == seed) next ^= System.nanoTime();
                seed = next;
            }
            setWorldOptions(worldData, worldData.worldGenOptions().withSeed(OptionalLong.of(seed)));

            data.worldRegenStage = "unloading";
            SavedDataUtil.markDirty(data);
            broadcast(server, "[Bingo] Regen 3/6: unloading game dimensions...");
            unloadTargetLevels(server);

            data.worldRegenStage = "deleting";
            SavedDataUtil.markDirty(data);
            broadcast(server, "[Bingo] Regen 4/6: deleting old world chunks...");
            deleteTargetChunkFiles(server);

            data.worldRegenStage = "recreating";
            SavedDataUtil.markDirty(data);
            broadcast(server, "[Bingo] Regen 5/6: creating fresh dimensions...");
            recreateTargetLevels(server, data, handles);

            data.worldRegenStage = "preloading";
            SavedDataUtil.markDirty(data);
            broadcast(server, "[Bingo] Regen 6/6: preloading start area...");

            BingoGameData currentData = BingoGameData.get(server);
            if (currentData != null && currentData != data) {
                currentData.worldUseNewSeedEachGame = data.worldUseNewSeedEachGame;
                currentData.worldTypeMode = data.worldTypeMode;
                currentData.worldSmallBiomes = data.worldSmallBiomes;
                currentData.worldCustomBiomeSizeBlocks = data.worldCustomBiomeSizeBlocks;
                currentData.worldTerrainHillinessPercent = data.worldTerrainHillinessPercent;
                currentData.worldStructureFrequencyPercent = data.worldStructureFrequencyPercent;
                currentData.worldSingleBiomeId = data.worldSingleBiomeId;
                currentData.worldSurfaceCaveBiomes = data.worldSurfaceCaveBiomes;
                currentData.worldSetSeedText = data.worldSetSeedText;
                currentData.worldRegenInProgress = data.worldRegenInProgress;
                currentData.worldRegenQueued = data.worldRegenQueued;
                currentData.worldRegenStage = data.worldRegenStage;
                currentData.pendingStartAfterWorldRegen = data.pendingStartAfterWorldRegen;
                currentData.pendingWeeklyChallengeStart = data.pendingWeeklyChallengeStart;
                currentData.pendingWeeklyChallengeBaseSeed = data.pendingWeeklyChallengeBaseSeed;
                data = currentData;
            }

            LobbyWorldManager.startPreloadingGameStartSpawn(server, data, true);
            data.worldFreshSeedPrepared = true;
            SavedDataUtil.markDirty(data);
            broadcast(server, "[Bingo] Game dimensions regenerated. Seed: " + seed);
            if (data.pendingStartAfterWorldRegen) {
                data.pendingStartAfterWorldRegen = false;
                SavedDataUtil.markDirty(data);
                CasinoTickScheduler.schedule(server, ServerTickUtil.getTickCount(server) + 2, () -> {
                    BingoGameData refreshed = BingoGameData.get(server);
                    if (refreshed == null || refreshed.isActive()) return;
                    if (refreshed.startGame(server)) {
                        com.jamie.jamiebingo.casino.CasinoModeManager.startPregamePhasesOrFinalize(server, refreshed);
                    }
                });
            }
            if (data.pendingWeeklyChallengeStart) {
                long weeklyBaseSeed = data.pendingWeeklyChallengeBaseSeed;
                CasinoTickScheduler.schedule(server, ServerTickUtil.getTickCount(server) + 2, () ->
                        com.jamie.jamiebingo.bingo.WeeklyChallengeManager.finalizePendingWeeklyChallenge(server, weeklyBaseSeed)
                );
            }
        } catch (Throwable t) {
            LOGGER.error("[JamieBingo] World regeneration failed", t);
            broadcast(server, "[Bingo] Dimension regeneration failed. Check logs.");
        } finally {
            BingoGameData currentData = BingoGameData.get(server);
            if (currentData != null) {
                currentData.worldRegenInProgress = false;
                currentData.worldRegenQueued = false;
                currentData.worldRegenStage = "";
                SavedDataUtil.markDirty(currentData);
            } else {
                data.worldRegenInProgress = false;
                data.worldRegenQueued = false;
                data.worldRegenStage = "";
                SavedDataUtil.markDirty(data);
            }
        }
    }

    public static String regenStatusText(BingoGameData data) {
        if (data == null) return "idle";
        if (data.worldRegenInProgress) {
            if (data.worldRegenStage != null && !data.worldRegenStage.isBlank()) {
                return "in progress (" + data.worldRegenStage + ")";
            }
            return "in progress";
        }
        if (data.worldRegenQueued) return "queued";
        return "idle";
    }

    private static void recreateTargetLevels(MinecraftServer server, BingoGameData data, RuntimeHandles handles) throws Exception {
        applyStructureFrequencyScaling(server, data);
        WorldData worldData = server.getWorldData();
        boolean debug = worldData.isDebugWorld();
        long obfuscatedSeed = net.minecraft.world.level.biome.BiomeManager.obfuscateSeed(worldData.worldGenOptions().seed());
        ServerLevelData overworldData = worldData.overworldData();
        @SuppressWarnings("unchecked")
        java.util.Map<ResourceKey<Level>, ServerLevel> worldMap =
                (java.util.Map<ResourceKey<Level>, ServerLevel>) server.forgeGetWorldMap();

        Registry<LevelStem> stemRegistry = server.registries().compositeAccess().lookupOrThrow(Registries.LEVEL_STEM);
        LevelStem currentOverworld = stemRegistry.getValue(LevelStem.OVERWORLD);
        LevelStem newOverworldStem = buildOverworldStem(server, currentOverworld, data);

        List<CustomSpawner> customSpawners = ImmutableList.of(
                new PhantomSpawner(),
                new PatrolSpawner(),
                new CatSpawner(),
                new VillageSiege(),
                new WanderingTraderSpawner(overworldData)
        );

        ServerLevel overworld = new ServerLevel(
                server,
                handles.executor,
                handles.storageSource,
                overworldData,
                Level.OVERWORLD,
                newOverworldStem,
                debug,
                obfuscatedSeed,
                customSpawners,
                true,
                null
        );
        worldMap.put(Level.OVERWORLD, overworld);
        ForgeEventFactory.onLevelLoad(overworld);

        DimensionDataStorage ds = overworld.getDataStorage();
        server.getScoreboard().load(ds.computeIfAbsent(ScoreboardSaveData.TYPE).getData());
        setField(server, "commandStorage", new CommandStorage(ds));
        setField(server, "stopwatches", ds.computeIfAbsent(Stopwatches.TYPE));

        var sequences = overworld.getRandomSequences();
        recreateDimensionFromStem(server, worldMap, stemRegistry, LevelStem.NETHER, overworldData, debug, obfuscatedSeed, sequences, handles, data);
        recreateDimensionFromStem(server, worldMap, stemRegistry, LevelStem.END, overworldData, debug, obfuscatedSeed, sequences, handles, data);

        for (ServerLevel level : List.of(overworld, worldMap.get(Level.NETHER), worldMap.get(Level.END))) {
            if (level == null) continue;
            WorldBorder border = level.getWorldBorder();
            border.setAbsoluteMaxSize(server.getAbsoluteMaxWorldSize());
            server.getPlayerList().addWorldborderListener(level);
        }

        server.setRespawnData(overworld.getRespawnData());
        server.markWorldsDirty();
    }

    private static void recreateDimensionFromStem(
            MinecraftServer server,
            java.util.Map<ResourceKey<Level>, ServerLevel> worldMap,
            Registry<LevelStem> stemRegistry,
            ResourceKey<LevelStem> stemKey,
            ServerLevelData overworldData,
            boolean debug,
            long obfuscatedSeed,
            net.minecraft.world.RandomSequences sequences,
            RuntimeHandles handles,
            BingoGameData data
    ) throws Exception {
        LevelStem stem = stemRegistry.getValue(stemKey);
        if (stem == null) return;
        if (stemKey == LevelStem.NETHER && data != null
                && (data.worldTypeMode == BingoGameData.WORLD_TYPE_CUSTOM_BIOME_SIZE
                || data.worldTypeMode == BingoGameData.WORLD_TYPE_SMALL_BIOMES
                || data.worldSmallBiomes)) {
            stem = buildNetherSmallBiomesStem(server, stem, data);
        }
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, stemKey.identifier());

        ServerLevelData derived = new DerivedLevelData(server.getWorldData(), overworldData);
        ServerLevel level = new ServerLevel(
                server,
                handles.executor,
                handles.storageSource,
                derived,
                levelKey,
                stem,
                debug,
                obfuscatedSeed,
                ImmutableList.of(),
                false,
                sequences
        );
        worldMap.put(levelKey, level);
        ForgeEventFactory.onLevelLoad(level);
    }

    private static LevelStem buildOverworldStem(MinecraftServer server, LevelStem base, BingoGameData data) {
        if (base == null) return null;
        if (data == null) return base;

        try {
            if (data.worldTypeMode == BingoGameData.WORLD_TYPE_SUPERFLAT) {
                HolderGetter<Biome> biomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
                HolderGetter<StructureSet> structures = server.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
                HolderGetter<PlacedFeature> placed = server.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
                FlatLevelGeneratorSettings flat = FlatLevelGeneratorSettings.getDefault(biomes, structures, placed);
                flat.setDecoration();
                return new LevelStem(base.type(), new FlatLevelSource(flat));
            }

            ChunkGenerator baseGenerator = base.generator();
            if (!(baseGenerator instanceof NoiseBasedChunkGenerator noise)) {
                return base;
            }

            Holder<NoiseGeneratorSettings> settingsHolder = pickNoiseSettings(server, data);
            if (settingsHolder == null) {
                settingsHolder = noise.generatorSettings();
            }

            var biomeSource = noise.getBiomeSource();
            if (data.worldTypeMode == BingoGameData.WORLD_TYPE_SINGLE_BIOME) {
                biomeSource = new FixedBiomeSource(resolveSingleBiome(server, data));
            } else if (data.worldTypeMode == BingoGameData.WORLD_TYPE_CUSTOM_BIOME_SIZE
                    || data.worldTypeMode == BingoGameData.WORLD_TYPE_SMALL_BIOMES
                    || data.worldSmallBiomes) {
                settingsHolder = scaleBiomeNoiseLikeTinyBiomes(
                        settingsHolder,
                        data.worldCustomBiomeSizeBlocks,
                        data.worldTerrainHillinessPercent
                );
            }

            return new LevelStem(base.type(), new NoiseBasedChunkGenerator(biomeSource, settingsHolder));
        } catch (Throwable t) {
            LOGGER.error("[JamieBingo] Failed to build overworld stem for world type {}", data.worldTypeMode, t);
            return base;
        }
    }

    private static Holder<NoiseGeneratorSettings> pickNoiseSettings(MinecraftServer server, BingoGameData data) {
        HolderGetter<NoiseGeneratorSettings> holder = server.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS);
        ResourceKey<NoiseGeneratorSettings> key = switch (data.worldTypeMode) {
            case BingoGameData.WORLD_TYPE_AMPLIFIED -> NoiseGeneratorSettings.AMPLIFIED;
            default -> NoiseGeneratorSettings.OVERWORLD;
        };
        return holder.getOrThrow(key);
    }

    private static Holder<Biome> resolveSingleBiome(MinecraftServer server, BingoGameData data) {
        HolderGetter<Biome> biomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
        try {
            String id = data == null ? "minecraft:plains" : data.worldSingleBiomeId;
            if (id == null || id.isBlank()) id = "minecraft:plains";
            Identifier parsed = Identifier.parse(id);
            ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, parsed);
            return biomes.getOrThrow(biomeKey);
        } catch (Throwable ignored) {
            return biomes.getOrThrow(Biomes.PLAINS);
        }
    }

    private static LevelStem buildNetherSmallBiomesStem(MinecraftServer server, LevelStem stem, BingoGameData data) {
        if (stem == null || stem.generator() == null) return stem;
        ChunkGenerator generator = stem.generator();
        if (!(generator instanceof NoiseBasedChunkGenerator noise)) return stem;
        try {
            int overworldBlocks = data == null ? 96 : data.worldCustomBiomeSizeBlocks;
            int blocks = Math.max(40, BingoGameData.clampWorldBiomeSize(overworldBlocks / 8));
            int hilliness = data == null ? 50 : data.worldTerrainHillinessPercent;
            Holder<NoiseGeneratorSettings> settings = scaleBiomeNoiseLikeTinyBiomes(noise.generatorSettings(), blocks, hilliness);
            return new LevelStem(stem.type(), new NoiseBasedChunkGenerator(noise.getBiomeSource(), settings));
        } catch (Throwable t) {
            LOGGER.warn("[JamieBingo] Failed to build nether small-biome stem", t);
            return stem;
        }
    }

    private static Holder<NoiseGeneratorSettings> scaleBiomeNoiseLikeTinyBiomes(Holder<NoiseGeneratorSettings> baseHolder, int biomeSizeBlocks, int terrainHillinessPercent) {
        if (baseHolder == null) return null;
        try {
            double climateScale = tinyBiomeClimateScaleFromSlider(biomeSizeBlocks);
            double terrainScale = tinyBiomeTerrainScaleFromSlider(biomeSizeBlocks, terrainHillinessPercent);
            if (Math.abs(climateScale - 1.0D) < 1.0E-9D && Math.abs(terrainScale - 1.0D) < 1.0E-9D) {
                return baseHolder;
            }
            NoiseGeneratorSettings base = baseHolder.value();
            NoiseRouter router = base.noiseRouter().mapAll(new TinyBiomeNoiseScaleVisitor(climateScale, terrainScale));
            NoiseGeneratorSettings out = new NoiseGeneratorSettings(
                    base.noiseSettings(),
                    base.defaultBlock(),
                    base.defaultFluid(),
                    router,
                    base.surfaceRule(),
                    base.spawnTarget(),
                    base.seaLevel(),
                    base.aquifersEnabled(),
                    base.oreVeinsEnabled(),
                    base.useLegacyRandomSource(),
                    base.disableMobGeneration()
            );
            return Holder.direct(out);
        } catch (Throwable t) {
            LOGGER.warn("[JamieBingo] Failed to scale biome noise like TinyBiomes", t);
            return baseHolder;
        }
    }

    private static double tinyBiomeClimateScaleFromSlider(int biomeSizeBlocks) {
        int blocks = BingoGameData.clampWorldBiomeSize(biomeSizeBlocks);
        double t = (100.0D - blocks) / 60.0D; // 0 at 100, 1 at 40
        t = Math.max(0.0D, Math.min(1.0D, t));
        // Smooth curve reduces sensitivity near both ends, especially at the small-biome end.
        double s = t * t * (3.0D - 2.0D * t);
        // Up to 16x on temperature/vegetation (less extreme than previous cap, still strong).
        return Math.pow(2.0D, 4.0D * s);
    }

    private static double tinyBiomeTerrainScaleFromSlider(int biomeSizeBlocks, int terrainHillinessPercent) {
        int blocks = BingoGameData.clampWorldBiomeSize(biomeSizeBlocks);
        int hilliness = BingoGameData.clampWorldTerrainHilliness(terrainHillinessPercent);
        double t = (100.0D - blocks) / 60.0D;
        t = Math.max(0.0D, Math.min(1.0D, t));
        double s = t * t * (3.0D - 2.0D * t);
        // 0% -> strong smoothing (below vanilla), 50% -> near-vanilla, 100% -> strong amplification.
        double hillinessSigned = (hilliness / 100.0D) * 2.0D - 1.0D;
        double exponent = 2.0D * s * hillinessSigned;
        return Math.pow(2.0D, exponent);
    }

    private static final class TinyBiomeNoiseScaleVisitor implements DensityFunction.Visitor {
        private static final Set<ResourceKey<NormalNoise.NoiseParameters>> CLIMATE_TARGETS = Set.of(
                Noises.TEMPERATURE,
                Noises.VEGETATION
        );
        private static final Set<ResourceKey<NormalNoise.NoiseParameters>> TERRAIN_TARGETS = Set.of(
                Noises.CONTINENTALNESS,
                Noises.EROSION,
                Noises.RIDGE,
                Noises.JAGGED
        );

        private final double climateScale;
        private final double terrainScale;

        private TinyBiomeNoiseScaleVisitor(double climateScale, double terrainScale) {
            this.climateScale = climateScale;
            this.terrainScale = terrainScale;
        }

        @Override
        public DensityFunction apply(DensityFunction function) {
            String cls = function.getClass().getName();
            if ("net.minecraft.world.level.levelgen.DensityFunctions$Noise".equals(cls)) {
                DensityFunction.NoiseHolder holder = invoke(function, "noise", DensityFunction.NoiseHolder.class);
                Double xzScale = invoke(function, "xzScale", Double.class);
                Double yScale = invoke(function, "yScale", Double.class);
                double scale = scaleFor(holder);
                if (holder != null && xzScale != null && yScale != null && scale > 0.0D) {
                    return DensityFunctions.noise(holder.noiseData(), xzScale * scale, yScale);
                }
                return function;
            }
            if ("net.minecraft.world.level.levelgen.DensityFunctions$ShiftedNoise".equals(cls)) {
                DensityFunction.NoiseHolder holder = invoke(function, "noise", DensityFunction.NoiseHolder.class);
                DensityFunction shiftX = invoke(function, "shiftX", DensityFunction.class);
                DensityFunction shiftZ = invoke(function, "shiftZ", DensityFunction.class);
                Double xzScale = invoke(function, "xzScale", Double.class);
                double scale = scaleFor(holder);
                if (holder != null && shiftX != null && shiftZ != null && xzScale != null && scale > 0.0D) {
                    // Vanilla climate noises are 2D shifted noises; keep same shifts, only scale xz.
                    return DensityFunctions.shiftedNoise2d(shiftX, shiftZ, xzScale * scale, holder.noiseData());
                }
                return function;
            }
            return function;
        }

        private double scaleFor(DensityFunction.NoiseHolder holder) {
            if (holder == null) return 0.0D;
            var keyOpt = holder.noiseData().unwrapKey();
            if (keyOpt.isEmpty()) return 0.0D;
            ResourceKey<NormalNoise.NoiseParameters> key = keyOpt.get();
            if (CLIMATE_TARGETS.contains(key)) return climateScale;
            if (TERRAIN_TARGETS.contains(key)) return terrainScale;
            return 0.0D;
        }

        private static <T> T invoke(Object target, String method, Class<T> type) {
            try {
                Object out = target.getClass().getMethod(method).invoke(target);
                return type.cast(out);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private static void applyStructureFrequencyScaling(MinecraftServer server, BingoGameData data) {
        if (server == null) return;
        int frequencyPercent = data == null ? 100 : BingoGameData.clampWorldStructureFrequency(data.worldStructureFrequencyPercent);
        double scale = frequencyPercent / 100.0D;
        try {
            Registry<StructureSet> structureRegistry = server.registries().compositeAccess().lookupOrThrow(Registries.STRUCTURE_SET);
            for (Holder.Reference<StructureSet> reference : structureRegistry.listElements().toList()) {
                if (reference == null || !reference.isBound()) continue;
                ResourceKey<StructureSet> key = reference.key();
                if (key == null) continue;
                StructureSet current = reference.value();
                if (current == null) continue;
                ORIGINAL_STRUCTURE_PLACEMENTS.putIfAbsent(key, current.placement());
                StructurePlacement basePlacement = ORIGINAL_STRUCTURE_PLACEMENTS.get(key);
                if (basePlacement == null) continue;
                StructurePlacement adjusted = scaleStructurePlacement(basePlacement, scale);
                if (adjusted == null || adjusted == current.placement()) continue;
                bindHolderValue(reference, new StructureSet(current.structures(), adjusted));
            }
        } catch (Throwable t) {
            LOGGER.warn("[JamieBingo] Failed to apply structure frequency scaling", t);
        }
    }

    private static StructurePlacement scaleStructurePlacement(StructurePlacement placement, double scale) {
        if (placement == null) return null;
        if (Math.abs(scale - 1.0D) < 1.0E-9D) return placement;

        if (placement instanceof RandomSpreadStructurePlacement random) {
            float frequency = clampFloat(readPlacementFrequency(placement) * (float) scale, 0.0F, 1.0F);
            int spacing = random.spacing();
            int separation = random.separation();
            if (scale > 1.0D) {
                double spacingFactor = 1.0D / Math.sqrt(scale);
                spacing = clampInt((int) Math.round(spacing * spacingFactor), 3, 4096);
                separation = clampInt((int) Math.round(separation * spacingFactor), 1, 4095);
                separation = Math.min(separation, Math.max(1, spacing - 1));
            }
            return new RandomSpreadStructurePlacement(
                    readPlacementOffset(placement),
                    readPlacementFrequencyMethod(placement),
                    frequency,
                    readPlacementSalt(placement),
                    readPlacementExclusionZone(placement),
                    spacing,
                    separation,
                    random.spreadType()
            );
        }

        if (placement instanceof ConcentricRingsStructurePlacement concentric) {
            float frequency = clampFloat(readPlacementFrequency(placement) * (float) scale, 0.0F, 1.0F);
            int distance = concentric.distance();
            int spread = concentric.spread();
            int count = concentric.count();
            if (scale > 1.0D) {
                double spacingFactor = 1.0D / Math.sqrt(scale);
                distance = clampInt((int) Math.round(distance * spacingFactor), 3, 4096);
                spread = clampInt((int) Math.round(spread * spacingFactor), 1, 4096);
                count = clampInt((int) Math.round(count * Math.sqrt(scale)), 1, 4096);
            }
            return new ConcentricRingsStructurePlacement(
                    readPlacementOffset(placement),
                    readPlacementFrequencyMethod(placement),
                    frequency,
                    readPlacementSalt(placement),
                    readPlacementExclusionZone(placement),
                    distance,
                    spread,
                    count,
                    concentric.preferredBiomes()
            );
        }

        return placement;
    }

    private static Vec3i readPlacementOffset(StructurePlacement placement) {
        Vec3i fallback = new Vec3i(0, 0, 0);
        Vec3i out = invokePlacementMethod(placement, "locateOffset", Vec3i.class);
        return out == null ? fallback : out;
    }

    private static StructurePlacement.FrequencyReductionMethod readPlacementFrequencyMethod(StructurePlacement placement) {
        StructurePlacement.FrequencyReductionMethod out = invokePlacementMethod(
                placement,
                "frequencyReductionMethod",
                StructurePlacement.FrequencyReductionMethod.class
        );
        return out == null ? StructurePlacement.FrequencyReductionMethod.DEFAULT : out;
    }

    private static float readPlacementFrequency(StructurePlacement placement) {
        Float out = invokePlacementMethod(placement, "frequency", Float.class);
        return out == null ? 1.0F : out;
    }

    private static int readPlacementSalt(StructurePlacement placement) {
        Integer out = invokePlacementMethod(placement, "salt", Integer.class);
        return out == null ? 0 : out;
    }

    @SuppressWarnings("unchecked")
    private static Optional<StructurePlacement.ExclusionZone> readPlacementExclusionZone(StructurePlacement placement) {
        Optional<StructurePlacement.ExclusionZone> out = invokePlacementMethod(placement, "exclusionZone", Optional.class);
        return out == null ? Optional.empty() : out;
    }

    private static <T> T invokePlacementMethod(StructurePlacement placement, String name, Class<T> type) {
        if (placement == null) return null;
        try {
            var method = StructurePlacement.class.getDeclaredMethod(name);
            method.setAccessible(true);
            Object out = method.invoke(placement);
            return type.cast(out);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void bindHolderValue(Holder.Reference<StructureSet> reference, StructureSet value) {
        if (reference == null || value == null) return;
        try {
            var method = Holder.Reference.class.getDeclaredMethod("bindValue", Object.class);
            method.setAccessible(true);
            method.invoke(reference, value);
        } catch (Throwable first) {
            try {
                var field = Holder.Reference.class.getDeclaredField("value");
                field.setAccessible(true);
                field.set(reference, value);
            } catch (Throwable second) {
                LOGGER.warn("[JamieBingo] Failed to update structure-set holder value", second);
            }
        }
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long resolveConfiguredSeed(BingoGameData data) {
        if (data == null) return Long.MIN_VALUE;
        String raw = data.worldSetSeedText;
        if (raw == null) return Long.MIN_VALUE;
        String text = raw.trim();
        if (text.isEmpty()) return Long.MIN_VALUE;
        ParsedSettingsSeed parsed = decodeSettingsSeed(text);
        if (parsed != null) return parsed.seed();
        try {
            return Long.parseLong(text);
        } catch (Throwable ignored) {
            // Vanilla-like behavior for non-numeric seed text.
            return text.hashCode();
        }
    }

    public static String encodeSettingsSeed(long seed, BingoGameData data) {
        if (data == null) return String.valueOf(seed);
        String biome = data.worldSingleBiomeId == null || data.worldSingleBiomeId.isBlank() ? "minecraft:plains" : data.worldSingleBiomeId;
        int biomeSize = BingoGameData.clampWorldBiomeSize(data.worldCustomBiomeSizeBlocks);
        int hilliness = BingoGameData.clampWorldTerrainHilliness(data.worldTerrainHillinessPercent);
        int structureFrequency = BingoGameData.clampWorldStructureFrequency(data.worldStructureFrequencyPercent);
        String payload = seed
                + "|" + data.worldTypeMode
                + "|" + (data.worldSmallBiomes ? 1 : 0)
                + "|" + (data.worldSurfaceCaveBiomes ? 1 : 0)
                + "|" + biome
                + "|" + biomeSize
                + "|" + hilliness
                + "|" + structureFrequency
                + "|" + (data.worldUseNewSeedEachGame ? 1 : 0)
                + "|" + (data.adventureMode ? 1 : 0)
                + "|" + BingoGameData.clampPrelitPortalsMode(data.prelitPortalsMode);
        String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        // Keep numeric seed visible and include settings payload for exact reproduction.
        return seed + "|" + SETTINGS_SEED_PREFIX + b64;
    }

    public static ParsedSettingsSeed decodeSettingsSeed(String text) {
        if (text == null) return null;
        String in = text.trim();
        int prefixAt = in.indexOf(SETTINGS_SEED_PREFIX);
        if (prefixAt < 0) return null;
        try {
            String b64 = in.substring(prefixAt + SETTINGS_SEED_PREFIX.length());
            String payload = new String(Base64.getUrlDecoder().decode(b64), StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|", -1);
            if (parts.length < 5) return null;
            long seed = Long.parseLong(parts[0]);
            int type = Integer.parseInt(parts[1]);
            boolean small = "1".equals(parts[2]);
            boolean surface = "1".equals(parts[3]);
            String biome = parts[4] == null || parts[4].isBlank() ? "minecraft:plains" : parts[4];
            int biomeSize = parts.length >= 6 ? BingoGameData.clampWorldBiomeSize(Integer.parseInt(parts[5])) : 96;
            int hilliness = parts.length >= 7 ? BingoGameData.clampWorldTerrainHilliness(Integer.parseInt(parts[6])) : 50;
            int structureFrequency = parts.length >= 8 ? BingoGameData.clampWorldStructureFrequency(Integer.parseInt(parts[7])) : 100;
            boolean newSeedEachGame = parts.length >= 9 ? "1".equals(parts[8]) : true;
            boolean adventureMode = parts.length >= 10 && "1".equals(parts[9]);
            int prelitPortalsMode = parts.length >= 11
                    ? BingoGameData.clampPrelitPortalsMode(Integer.parseInt(parts[10]))
                    : BingoGameData.PRELIT_PORTALS_OFF;
            return new ParsedSettingsSeed(seed, type, small, surface, biome, biomeSize, hilliness, structureFrequency, newSeedEachGame, adventureMode, prelitPortalsMode);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public record ParsedSettingsSeed(
            long seed,
            int worldTypeMode,
            boolean smallBiomes,
            boolean surfaceCaveBiomes,
            String singleBiomeId,
            int biomeSizeBlocks,
            int terrainHillinessPercent,
            int structureFrequencyPercent,
            boolean newSeedEachGame,
            boolean adventureMode,
            int prelitPortalsMode
    ) {}

    private static void unloadTargetLevels(MinecraftServer server) {
        @SuppressWarnings("unchecked")
        java.util.Map<ResourceKey<Level>, ServerLevel> worldMap =
                (java.util.Map<ResourceKey<Level>, ServerLevel>) server.forgeGetWorldMap();

        for (ResourceKey<Level> key : List.of(Level.NETHER, Level.END, Level.OVERWORLD)) {
            ServerLevel level = worldMap.get(key);
            if (level == null) continue;
            try {
                level.getChunkSource().deactivateTicketsOnClosing();
                level.getChunkSource().tick(() -> false, false);
                ForgeEventFactory.onLevelUnload(level);
                level.close();
            } catch (Throwable t) {
                LOGGER.warn("[JamieBingo] Failed while unloading {}", key, t);
            }
            worldMap.remove(key);
        }
    }

    private static void ensureAllPlayersInLobbySpawnbox(MinecraftServer server) {
        if (server == null) return;
        ServerLevel lobby = LobbyWorldManager.getLobby(server);
        if (lobby == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null) return;

        // Keep players already inside lobby exactly where they are.
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            ServerLevel playerLevel = com.jamie.jamiebingo.util.ServerPlayerUtil.getLevel(player);
            if (playerLevel != null && playerLevel.dimension() == lobby.dimension()) {
                continue;
            }
            PregameBoxManager.handlePlayerJoin(player);
        }
    }

    private static void deleteTargetChunkFiles(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT);
        if (root == null) return;
        deleteDir(root.resolve("region"));
        deleteDir(root.resolve("entities"));
        deleteDir(root.resolve("poi"));

        Path nether = root.resolve("DIM-1");
        deleteDir(nether.resolve("region"));
        deleteDir(nether.resolve("entities"));
        deleteDir(nether.resolve("poi"));

        Path end = root.resolve("DIM1");
        deleteDir(end.resolve("region"));
        deleteDir(end.resolve("entities"));
        deleteDir(end.resolve("poi"));
    }

    private static void deleteDir(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    Files.deleteIfExists(d);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Throwable t) {
            LOGGER.warn("[JamieBingo] Failed deleting directory {}", dir, t);
        }
    }

    private static void setWorldOptions(WorldData worldData, WorldOptions options) {
        if (!(worldData instanceof PrimaryLevelData primary) || options == null) return;
        try {
            Field f = PrimaryLevelData.class.getDeclaredField("worldOptions");
            f.setAccessible(true);
            f.set(primary, options);
        } catch (Throwable t) {
            LOGGER.error("[JamieBingo] Failed to set world options", t);
        }
    }

    private static void broadcast(MinecraftServer server, String text) {
        if (server == null || text == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(ComponentUtil.literal(text));
        }
    }

    private static <T> T getField(Object target, String name, Class<T> type) throws Exception {
        Class<?> cls = target.getClass();
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                Object out = f.get(target);
                return type.cast(out);
            } catch (NoSuchFieldException ignored) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Class<?> cls = target.getClass();
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static RuntimeHandles resolveRuntimeHandles(MinecraftServer server) throws Exception {
        LevelStorageSource.LevelStorageAccess storage = getField(server, "storageSource", LevelStorageSource.LevelStorageAccess.class);
        java.util.concurrent.Executor executor = getField(server, "executor", java.util.concurrent.Executor.class);
        return new RuntimeHandles(storage, executor);
    }

    private record RuntimeHandles(
            LevelStorageSource.LevelStorageAccess storageSource,
            java.util.concurrent.Executor executor
    ) {}
}
