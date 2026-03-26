package com.jamie.jamiebingo.rtp;


import com.jamie.jamiebingo.util.ServerLevelUtil;
import com.jamie.jamiebingo.addons.effects.EffectRTP;
import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.Random;
/**
 * RTP handler:
 * - RTP ONLY on death
 * - Cancels RTP if ANY spawnpoint is set (bed or anchor)
 */
public final class BingoRtpHandler {

    private static final String TAG_RTP_SPAWN = "jamiebingo_rtp_spawn";
    private static final String TAG_NEXT_RTP_X = "jamiebingo_next_rtp_x";
    private static final String TAG_NEXT_RTP_Y = "jamiebingo_next_rtp_y";
    private static final String TAG_NEXT_RTP_Z = "jamiebingo_next_rtp_z";
    private static final String TAG_LAST_RTP_X = "jamiebingo_last_rtp_x";
    private static final String TAG_LAST_RTP_Y = "jamiebingo_last_rtp_y";
    private static final String TAG_LAST_RTP_Z = "jamiebingo_last_rtp_z";
    private static final String TAG_FORCED_CHUNK_X = "jamiebingo_rtp_forced_chunk_x";
    private static final String TAG_FORCED_CHUNK_Z = "jamiebingo_rtp_forced_chunk_z";

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;

        var original = event.getOriginal();
        if (original == null) return;

        var from = original.getPersistentData();
        var to = newPlayer.getPersistentData();

        if (from.contains(TAG_RTP_SPAWN)) {
            com.jamie.jamiebingo.util.NbtUtil.putBoolean(
                    to,
                    TAG_RTP_SPAWN,
                    com.jamie.jamiebingo.util.NbtUtil.getBoolean(from, TAG_RTP_SPAWN, false)
            );
        }
        if (from.contains(TAG_NEXT_RTP_X)) {
            com.jamie.jamiebingo.util.NbtUtil.putInt(to, TAG_NEXT_RTP_X, com.jamie.jamiebingo.util.NbtUtil.getInt(from, TAG_NEXT_RTP_X, 0));
            com.jamie.jamiebingo.util.NbtUtil.putInt(to, TAG_NEXT_RTP_Y, com.jamie.jamiebingo.util.NbtUtil.getInt(from, TAG_NEXT_RTP_Y, 0));
            com.jamie.jamiebingo.util.NbtUtil.putInt(to, TAG_NEXT_RTP_Z, com.jamie.jamiebingo.util.NbtUtil.getInt(from, TAG_NEXT_RTP_Z, 0));
        }
        if (from.contains(TAG_LAST_RTP_X)) {
            com.jamie.jamiebingo.util.NbtUtil.putInt(to, TAG_LAST_RTP_X, com.jamie.jamiebingo.util.NbtUtil.getInt(from, TAG_LAST_RTP_X, 0));
            com.jamie.jamiebingo.util.NbtUtil.putInt(to, TAG_LAST_RTP_Y, com.jamie.jamiebingo.util.NbtUtil.getInt(from, TAG_LAST_RTP_Y, 0));
            com.jamie.jamiebingo.util.NbtUtil.putInt(to, TAG_LAST_RTP_Z, com.jamie.jamiebingo.util.NbtUtil.getInt(from, TAG_LAST_RTP_Z, 0));
        }
        if (from.contains(TAG_FORCED_CHUNK_X)) {
            com.jamie.jamiebingo.util.NbtUtil.putInt(to, TAG_FORCED_CHUNK_X, com.jamie.jamiebingo.util.NbtUtil.getInt(from, TAG_FORCED_CHUNK_X, 0));
            com.jamie.jamiebingo.util.NbtUtil.putInt(to, TAG_FORCED_CHUNK_Z, com.jamie.jamiebingo.util.NbtUtil.getInt(from, TAG_FORCED_CHUNK_Z, 0));
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (!data.isActive()) return;
        if (!data.rtpEnabled) return;

        var tag = player.getPersistentData();

        // Respect bed/anchor spawns
        if (shouldSkipForBedSpawn(player, server)) {
            return;
        }

        BlockPos stored = getStoredNextRtp(tag);
        ServerLevel overworld = ServerLevelUtil.getOverworld(server);
        if (stored == null) {
            prepareRtpSpawn(player, server);
            return;
        }

        ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();
        if (respawnConfig == null || respawnConfig.respawnData() == null) {
            var respawnData = net.minecraft.world.level.storage.LevelData.RespawnData.of(
                    overworld.dimension(), stored, com.jamie.jamiebingo.util.EntityRotationUtil.getYRot(player), com.jamie.jamiebingo.util.EntityRotationUtil.getXRot(player)
            );
            player.setRespawnPosition(new ServerPlayer.RespawnConfig(respawnData, true), false);
        }
        ensureForcedChunk(overworld, tag, stored);
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (!data.isActive()) return;
        if (!data.rtpEnabled) return;

        // Respect bed/anchor spawns
        if (shouldSkipForBedSpawn(player, server)) {
            return;
        }

        prepareRtpSpawn(player, server);
    }

    public static void prepareRtpSpawn(ServerPlayer player, MinecraftServer server) {
        if (player == null || server == null) return;

        var tag = player.getPersistentData();
        ServerLevel overworld = ServerLevelUtil.getOverworld(server);
        if (overworld == null) return;

        if (shouldSkipForBedSpawn(player, server)) {
            return;
        }

        BlockPos origin = overworld.getRespawnData().pos();
        BlockPos target = findUniqueRtpTarget(player, overworld, origin);
        if (target == null) return;

        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, TAG_NEXT_RTP_X, target.getX());
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, TAG_NEXT_RTP_Y, target.getY());
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, TAG_NEXT_RTP_Z, target.getZ());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, TAG_RTP_SPAWN, true);

        var data = net.minecraft.world.level.storage.LevelData.RespawnData.of(
                overworld.dimension(), target, com.jamie.jamiebingo.util.EntityRotationUtil.getYRot(player), com.jamie.jamiebingo.util.EntityRotationUtil.getXRot(player)
        );
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(data, true), false);
        ensureForcedChunk(overworld, tag, target);
    }

    private static boolean shouldSkipForBedSpawn(ServerPlayer player, MinecraftServer server) {
        if (player == null || server == null) return true;
        ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();
        if (respawnConfig == null || respawnConfig.respawnData() == null) return false;
        BlockPos respawn = respawnConfig.respawnData().pos();

        // Treat the default world spawn as "no bed/anchor" to allow RTP on servers.
        if (respawnConfig.respawnData().dimension() == ServerLevelUtil.getOverworld(server).dimension()) {
            BlockPos worldSpawn = ServerLevelUtil.getOverworld(server).getRespawnData().pos();
            if (worldSpawn != null && worldSpawn.equals(respawn)) {
                return false;
            }
        }

        var tag = player.getPersistentData();
        BlockPos stored = getStoredNextRtp(tag);
        boolean matchesStored = stored != null
                && respawn.equals(stored)
                && respawnConfig.respawnData().dimension() == ServerLevelUtil.getOverworld(server).dimension();

        if (!matchesStored) {
            clearRtpData(player, server);
            return true;
        }

        return false;
    }

    private static BlockPos getStoredNextRtp(net.minecraft.nbt.CompoundTag tag) {
        if (tag == null) return null;
        if (!tag.contains(TAG_NEXT_RTP_X)) return null;
        return new BlockPos(
                com.jamie.jamiebingo.util.NbtUtil.getInt(tag, TAG_NEXT_RTP_X, 0),
                com.jamie.jamiebingo.util.NbtUtil.getInt(tag, TAG_NEXT_RTP_Y, 0),
                com.jamie.jamiebingo.util.NbtUtil.getInt(tag, TAG_NEXT_RTP_Z, 0)
        );
    }

    private static void ensureForcedChunk(ServerLevel level, net.minecraft.nbt.CompoundTag tag, BlockPos target) {
        if (level == null || tag == null || target == null) return;

        int forcedX = tag.contains(TAG_FORCED_CHUNK_X) ? com.jamie.jamiebingo.util.NbtUtil.getInt(tag, TAG_FORCED_CHUNK_X, Integer.MIN_VALUE) : Integer.MIN_VALUE;
        int forcedZ = tag.contains(TAG_FORCED_CHUNK_Z) ? com.jamie.jamiebingo.util.NbtUtil.getInt(tag, TAG_FORCED_CHUNK_Z, Integer.MIN_VALUE) : Integer.MIN_VALUE;

        ChunkPos newChunk = new ChunkPos(target);
        if (forcedX == newChunk.x && forcedZ == newChunk.z) {
            level.setChunkForced(newChunk.x, newChunk.z, true);
            return;
        }

        clearForcedChunk(level, tag);
        level.setChunkForced(newChunk.x, newChunk.z, true);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, TAG_FORCED_CHUNK_X, newChunk.x);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, TAG_FORCED_CHUNK_Z, newChunk.z);
    }

    private static void clearForcedChunk(ServerLevel level, net.minecraft.nbt.CompoundTag tag) {
        if (level == null || tag == null) return;
        if (tag.contains(TAG_FORCED_CHUNK_X) && tag.contains(TAG_FORCED_CHUNK_Z)) {
            int chunkX = com.jamie.jamiebingo.util.NbtUtil.getInt(tag, TAG_FORCED_CHUNK_X, 0);
            int chunkZ = com.jamie.jamiebingo.util.NbtUtil.getInt(tag, TAG_FORCED_CHUNK_Z, 0);
            level.setChunkForced(chunkX, chunkZ, false);
            tag.remove(TAG_FORCED_CHUNK_X);
            tag.remove(TAG_FORCED_CHUNK_Z);
        }
    }

    private static void clearRtpData(ServerPlayer player, MinecraftServer server) {
        if (player == null || server == null) return;
        var tag = player.getPersistentData();
        ServerLevel overworld = ServerLevelUtil.getOverworld(server);
        clearForcedChunk(overworld, tag);
        tag.remove(TAG_RTP_SPAWN);
        tag.remove(TAG_NEXT_RTP_X);
        tag.remove(TAG_NEXT_RTP_Y);
        tag.remove(TAG_NEXT_RTP_Z);
    }

    private static BlockPos findUniqueRtpTarget(ServerPlayer player, ServerLevel level, BlockPos origin) {
        if (player == null || level == null || origin == null) return null;

        Random rng = new Random();
        int attempts = 96;
        int radius = 12000;
        int minDistance = 500;

        BlockPos last = null;
        var tag = player.getPersistentData();
        if (tag.contains(TAG_LAST_RTP_X)) {
            last = new BlockPos(
                    com.jamie.jamiebingo.util.NbtUtil.getInt(tag, TAG_LAST_RTP_X, 0),
                    com.jamie.jamiebingo.util.NbtUtil.getInt(tag, TAG_LAST_RTP_Y, 0),
                    com.jamie.jamiebingo.util.NbtUtil.getInt(tag, TAG_LAST_RTP_Z, 0)
            );
        }

        for (int i = 0; i < attempts; i++) {
            BlockPos candidate = EffectRTP.findSafeSurface(level, origin, rng, radius, 1, false);
            if (candidate == null) continue;
            if (last != null && candidate.distSqr(last) < (double) minDistance * minDistance) {
                continue;
            }
            com.jamie.jamiebingo.util.NbtUtil.putInt(tag, TAG_LAST_RTP_X, candidate.getX());
            com.jamie.jamiebingo.util.NbtUtil.putInt(tag, TAG_LAST_RTP_Y, candidate.getY());
            com.jamie.jamiebingo.util.NbtUtil.putInt(tag, TAG_LAST_RTP_Z, candidate.getZ());
            return candidate;
        }

        BlockPos fallback = EffectRTP.findSafeSurface(level, origin, rng, radius, attempts, false);
        if (fallback != null) {
            com.jamie.jamiebingo.util.NbtUtil.putInt(tag, TAG_LAST_RTP_X, fallback.getX());
            com.jamie.jamiebingo.util.NbtUtil.putInt(tag, TAG_LAST_RTP_Y, fallback.getY());
            com.jamie.jamiebingo.util.NbtUtil.putInt(tag, TAG_LAST_RTP_Z, fallback.getZ());
        }
        return fallback;
    }
}








