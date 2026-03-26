package com.jamie.jamiebingo.world;


import com.jamie.jamiebingo.util.BlockPosUtil;
import com.jamie.jamiebingo.util.LevelRespawnUtil;
import com.jamie.jamiebingo.util.ServerLevelUtil;
import com.jamie.jamiebingo.util.ServerPlayerTeleportUtil;
import com.jamie.jamiebingo.JamieBingo;
import com.jamie.jamiebingo.addons.effects.EffectRTP;
import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;
import java.util.Set;

public final class LobbyWorldManager {

    public static final ResourceKey<net.minecraft.world.level.Level> LOBBY_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, com.jamie.jamiebingo.util.IdUtil.id(JamieBingo.MOD_ID + ":lobby"));

    private static final int GAME_SPAWN_RADIUS = 220000;
    private static final int GAME_SPAWN_ATTEMPTS = 96;
    private static final int MIN_SPAWN_DISTANCE = 10000;
    private static final int GAME_START_CHUNK_RADIUS = 15;

    private LobbyWorldManager() {}

    public static ServerLevel getLobby(MinecraftServer server) {
        if (server == null) return null;
        return server.getLevel(LOBBY_DIMENSION);
    }

    public static ServerLevel getGameWorld(MinecraftServer server) {
        if (server == null) return null;
        return ServerLevelUtil.getOverworld(server);
    }

    public static void sendToLobby(ServerPlayer player) {
        if (player == null) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;

        ServerLevel lobby = getLobby(server);
        if (lobby == null) return;

        BlockPos spawn = com.jamie.jamiebingo.util.LevelSpawnUtil.getSpawnPos(lobby);
        float yaw = com.jamie.jamiebingo.util.LevelSpawnUtil.getSpawnYaw(lobby);
        ServerPlayerTeleportUtil.teleport(
                player,
                lobby,
                BlockPosUtil.getX(spawn) + 0.5,
                BlockPosUtil.getY(spawn) + 1.0,
                BlockPosUtil.getZ(spawn) + 0.5,
                Set.of(),
                yaw,
                com.jamie.jamiebingo.util.EntityRotationUtil.getXRot(player),
                false
        );
    }

    public static void sendAllToLobby(MinecraftServer server) {
        if (server == null) return;
        ServerLevel lobby = getLobby(server);
        if (lobby == null) return;

        BlockPos spawn = com.jamie.jamiebingo.util.LevelSpawnUtil.getSpawnPos(lobby);
        float yaw = com.jamie.jamiebingo.util.LevelSpawnUtil.getSpawnYaw(lobby);
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            ServerPlayerTeleportUtil.teleport(
                    player,
                    lobby,
                    BlockPosUtil.getX(spawn) + 0.5,
                    BlockPosUtil.getY(spawn) + 1.0,
                    BlockPosUtil.getZ(spawn) + 0.5,
                    Set.of(),
                    yaw,
                    com.jamie.jamiebingo.util.EntityRotationUtil.getXRot(player),
                    false
            );
        }
    }

    public static void startPreloadingGameStartSpawn(MinecraftServer server, BingoGameData data, boolean forceNew) {
        if (server == null || data == null) return;

        ServerLevel game = getGameWorld(server);
        if (game == null) return;

        if (data.gameStartSpawnPrepared && !forceNew) {
            return;
        }

        if (data.gameStartSpawnPrepared) {
            clearPreparedGameStartSpawn(server, data);
        }

        BlockPos origin = com.jamie.jamiebingo.util.LevelSpawnUtil.getSpawnPos(game);
        Random rng = new Random(System.nanoTime());
        BlockPos picked = null;
        boolean sameSeedFarMode = !data.worldUseNewSeedEachGame;
        int spawnRadius = computeSpawnRadius(data);
        int minDistance = computeMinDistance(data);

        if (sameSeedFarMode && !data.lastGameSpawnSet) {
            picked = safeSurfaceAt(game, origin);
        } else if (sameSeedFarMode) {
            for (int i = 0; i < GAME_SPAWN_ATTEMPTS; i++) {
                BlockPos candidate = EffectRTP.findSafeSurface(game, origin, rng, spawnRadius, 1, false);
                if (candidate == null) continue;
                candidate = safeSurfaceAt(game, candidate);
                if (candidate == null) continue;

                double dx = BlockPosUtil.getX(candidate) - data.lastGameSpawnX;
                double dz = BlockPosUtil.getZ(candidate) - data.lastGameSpawnZ;
                if (Math.sqrt(dx * dx + dz * dz) < minDistance) {
                    continue;
                }
                picked = candidate;
                break;
            }
        } else {
            // Fresh-seed mode should start at the generated world's actual spawn area.
            picked = safeSurfaceAt(game, origin);
        }

        if (picked == null) picked = safeSurfaceAt(game, origin);
        if (picked == null) picked = origin;

        LevelRespawnUtil.setRespawnData(game, game.dimension(), picked, 0.0F, 0.0F);
        data.lastGameSpawnSet = true;
        data.lastGameSpawnX = BlockPosUtil.getX(picked);
        data.lastGameSpawnY = BlockPosUtil.getY(picked);
        data.lastGameSpawnZ = BlockPosUtil.getZ(picked);
        data.gameStartSpawnPrepared = true;
        data.gameStartSpawnLoading = false;
        data.gameStartSpawnLoadingIndex = 0;
        data.gameStartSpawnLoadingTotal = 0;
        data.gameStartSpawnX = BlockPosUtil.getX(picked);
        data.gameStartSpawnY = BlockPosUtil.getY(picked);
        data.gameStartSpawnZ = BlockPosUtil.getZ(picked);
        data.gameStartSpawnChunkRadius = GAME_START_CHUNK_RADIUS;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
    }

    private static BlockPos safeSurfaceAt(ServerLevel level, BlockPos ref) {
        if (level == null || ref == null) return null;
        int x = BlockPosUtil.getX(ref);
        int z = BlockPosUtil.getZ(ref);
        level.getChunk(x >> 4, z >> 4);
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        BlockPos base = new BlockPos(x, y, z);
        if (isSafeStand(level, base)) return base;
        BlockPos ground = findNearbySurfaceSpawn(level, x, z);
        if (ground != null) return ground;
        return EffectRTP.findSafeSurface(level, base, new Random(System.nanoTime()), 1024, 128, false);
    }

    private static BlockPos findNearbySurfaceSpawn(ServerLevel level, int x, int z) {
        if (level == null) return null;
        for (int r = 0; r <= 16; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    int px = x + dx;
                    int pz = z + dz;
                    level.getChunk(px >> 4, pz >> 4);
                    int py = level.getHeight(Heightmap.Types.WORLD_SURFACE, px, pz);
                    BlockPos feet = new BlockPos(px, py, pz);
                    if (isSafeStand(level, feet)) return feet;
                }
            }
        }
        return null;
    }

    private static boolean isSafeStand(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return false;
        BlockPos floor = pos.below();
        if (!level.getBlockState(floor).isSolidRender()) return false;
        if (!level.getBlockState(pos).getFluidState().isEmpty()) return false;
        if (!level.getBlockState(pos.above()).getFluidState().isEmpty()) return false;
        return true;
    }

    public static void clearPreparedGameStartSpawn(MinecraftServer server, BingoGameData data) {
        if (server == null || data == null) return;
        if (!data.gameStartSpawnPrepared) return;

        ServerLevel game = getGameWorld(server);
        if (game == null) return;

        int radius = Math.max(0, data.gameStartSpawnChunkRadius);
        BlockPos center = new BlockPos(data.gameStartSpawnX, data.gameStartSpawnY, data.gameStartSpawnZ);
        clearSpawnChunks(game, center, radius);
        data.gameStartSpawnPrepared = false;
        data.gameStartSpawnLoading = false;
        data.gameStartSpawnLoadingIndex = 0;
        data.gameStartSpawnLoadingTotal = 0;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
    }


    private static void clearSpawnChunks(ServerLevel level, BlockPos center, int radius) {
        if (level == null || center == null || radius < 0) return;
        ChunkPos centerChunk = new ChunkPos(center);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                level.setChunkForced(centerChunk.x + dx, centerChunk.z + dz, false);
            }
        }
    }

    private static int computeSpawnRadius(BingoGameData data) {
        if (data == null || !data.worldUseNewSeedEachGame) {
            return GAME_SPAWN_RADIUS;
        }
        int base = switch (data.worldTypeMode) {
            case BingoGameData.WORLD_TYPE_SUPERFLAT -> 70000;
            case BingoGameData.WORLD_TYPE_SINGLE_BIOME -> 120000;
            case BingoGameData.WORLD_TYPE_CUSTOM_BIOME_SIZE, BingoGameData.WORLD_TYPE_SMALL_BIOMES -> 140000;
            case BingoGameData.WORLD_TYPE_AMPLIFIED -> 260000;
            default -> GAME_SPAWN_RADIUS;
        };
        if (data.worldTypeMode == BingoGameData.WORLD_TYPE_CUSTOM_BIOME_SIZE
                || data.worldTypeMode == BingoGameData.WORLD_TYPE_SMALL_BIOMES
                || data.worldSmallBiomes) {
            base = Math.max(50000, (int) Math.round(base * 0.6));
        }
        return base;
    }

    private static int computeMinDistance(BingoGameData data) {
        if (data == null) return MIN_SPAWN_DISTANCE;
        if (!data.worldUseNewSeedEachGame) return MIN_SPAWN_DISTANCE;
        int base = (data.worldTypeMode == BingoGameData.WORLD_TYPE_CUSTOM_BIOME_SIZE
                || data.worldTypeMode == BingoGameData.WORLD_TYPE_SMALL_BIOMES
                || data.worldSmallBiomes) ? 4000 : 8000;
        return base;
    }
}







