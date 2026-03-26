package com.jamie.jamiebingo.addons.effects;


import com.jamie.jamiebingo.util.BlockPosUtil;
import com.jamie.jamiebingo.util.LevelBoundsUtil;
import com.jamie.jamiebingo.util.LevelSpawnUtil;
import com.jamie.jamiebingo.util.ServerLevelUtil;
import com.jamie.jamiebingo.util.ServerPlayerTeleportUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;

import java.util.Random;

public class EffectRTP implements CustomRandomEffect {

    private static final Random RANDOM = new Random();
    private static final int RADIUS = 5000;
    private static final int ATTEMPTS = 64;

    @Override
    public String id() {
        return "rtp";
    }

    @Override
    public String displayName() {
        return "Random Teleport";
    }

    @Override
    public void onApply(MinecraftServer server) {
        BingoGameData data = BingoGameData.get(server);
        for (ServerPlayer player : data.getParticipantPlayers(server)) {
            teleportPlayerSafely(player);
        }
    }

    @Override
    public void onRemove(MinecraftServer server) {}

    @Override
    public void onTick(MinecraftServer server) {}

    /* ==========================================================
       SAFE TELEPORT (NEVER FAILS)
       ========================================================== */

    public static void teleportPlayerSafely(ServerPlayer player) {
        if (player == null) return;

        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;

        ServerLevel level = ServerLevelUtil.getOverworld(server);
        if (level == null) return;

        BlockPos pos = findSurface(level, LevelSpawnUtil.getSpawnPos(level));

        // 🔁 ABSOLUTE FALLBACK — SPAWN
        if (pos == null) {
            pos = LevelSpawnUtil.getSpawnPos(level);
            System.out.println("[JamieBingo][RTP] Fallback to world spawn");
        }

        System.out.println("[JamieBingo][RTP] Teleporting "
                + player.getName().getString() + " to " + pos);

        ServerPlayerTeleportUtil.teleport(
                player,
                level,
                BlockPosUtil.getX(pos) + 0.5,
                BlockPosUtil.getY(pos) + 1,
                BlockPosUtil.getZ(pos) + 0.5,
                java.util.Set.of(),
                com.jamie.jamiebingo.util.EntityRotationUtil.getYRot(player),
                com.jamie.jamiebingo.util.EntityRotationUtil.getXRot(player),
                false
        );
    }

    /* ==========================================================
       SURFACE SEARCH (FIXED)
       ========================================================== */

    private static BlockPos findSurface(ServerLevel level, BlockPos origin) {
        return findSafeSurface(level, origin, RANDOM, RADIUS, ATTEMPTS, true);
    }

    public static BlockPos findSafeSurface(ServerLevel level, BlockPos origin, Random rng, int radius, int attempts) {
        return findSafeSurface(level, origin, rng, radius, attempts, true);
    }

    public static BlockPos findSafeSurface(ServerLevel level, BlockPos origin, Random rng, int radius, int attempts, boolean logFailure) {
        if (level == null || origin == null || rng == null) return null;

        int cappedAttempts = Math.max(1, attempts);
        int cappedRadius = Math.max(1, radius);

        for (int i = 0; i < cappedAttempts; i++) {
            int x = BlockPosUtil.getX(origin) + rng.nextInt(cappedRadius * 2) - cappedRadius;
            int z = BlockPosUtil.getZ(origin) + rng.nextInt(cappedRadius * 2) - cappedRadius;

            // 🔑 FORCE CHUNK LOAD
            ChunkPos chunkPos = new ChunkPos(x >> 4, z >> 4);
            level.getChunk(chunkPos.x, chunkPos.z);

            int y = level.getHeight(
                    Heightmap.Types.WORLD_SURFACE,
                    x,
                    z
            );

            if (y <= LevelBoundsUtil.getMinY(level) + 5) continue;

            BlockPos pos = new BlockPos(x, y, z);

            if (isSafeRtpSpot(level, pos) && level.noCollision(playerAABB(pos))) {
                return pos;
            }
        }

        if (logFailure) {
            System.out.println("[JamieBingo][RTP] Failed to find safe RTP location!");
        }
        return null;
    }

    private static boolean isSafeRtpSpot(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).getFluidState().isEmpty()) return false;
        BlockPos below = pos.below();
        if (!level.getBlockState(below).getFluidState().isEmpty()) return false;
        if (level.getBlockState(below).is(Blocks.ICE) || level.getBlockState(below).is(Blocks.PACKED_ICE)) return false;
        return level.getBlockState(below).isSolidRender();
    }

    private static net.minecraft.world.phys.AABB playerAABB(BlockPos pos) {
        return new net.minecraft.world.phys.AABB(
                BlockPosUtil.getX(pos) + 0.2,
                BlockPosUtil.getY(pos),
                BlockPosUtil.getZ(pos) + 0.2,
                BlockPosUtil.getX(pos) + 0.8,
                BlockPosUtil.getY(pos) + 1.8,
                BlockPosUtil.getZ(pos) + 0.8
        );
    }
}




