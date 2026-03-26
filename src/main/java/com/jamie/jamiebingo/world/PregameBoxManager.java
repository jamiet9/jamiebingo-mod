package com.jamie.jamiebingo.world;


import com.jamie.jamiebingo.addons.effects.EffectRTP;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.item.ModItems;
import com.jamie.jamiebingo.item.PlayerTrackerHandler;
import com.jamie.jamiebingo.util.BlockLookupUtil;
import com.jamie.jamiebingo.util.BlockPosUtil;
import com.jamie.jamiebingo.util.BlockStateUtil;
import com.jamie.jamiebingo.util.EntityRotationUtil;
import com.jamie.jamiebingo.util.LightBlockUtil;
import com.jamie.jamiebingo.util.LevelSetBlockUtil;
import com.jamie.jamiebingo.util.ServerLevelUtil;
import com.jamie.jamiebingo.util.ServerPlayerTeleportUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
public final class PregameBoxManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String GAME_START_SAFE_UNTIL_TICK = "jamiebingo_game_start_safe_until";

    static final int BOX_HALF = 40;
    static final int BOX_HEIGHT = 40;
    private static final int DEFAULT_Y = 220;
    private static final String TIP_TEXT_TAG = "jamiebingo_spawnbox_tip";
    private static final String[] SPAWNBOX_TIPS = new String[] {};
    private static final Set<java.util.UUID> PENDING_BOX_TELEPORTS = ConcurrentHashMap.newKeySet();

    private PregameBoxManager() {}

    public static void handlePlayerJoin(ServerPlayer player) {
        if (player == null) return;

        MinecraftServer server = com.jamie.jamiebingo.util.ServerPlayerUtil.getServer(player);
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (data.isActive()) return;

        ServerLevel level = getPregameLevel(server);
        if (level == null) return;

        ensureBox(level, data);

        if (data.pregameBoxActive) {
            disablePvpInOverworld(server);
            BlockPos center = new BlockPos(data.pregameBoxX, data.pregameBoxY, data.pregameBoxZ);
            clearHostileMobsNearBox(level, center);
            BlockPos spawn = boxLobbySpawn(center);
            boolean teleported = ServerPlayerTeleportUtil.teleport(
                    player,
                    level,
                    BlockPosUtil.getX(spawn) + 0.5,
                    BlockPosUtil.getY(spawn) + 0.05,
                    BlockPosUtil.getZ(spawn) + 0.5,
                    Set.of(),
                    180.0F,
                    EntityRotationUtil.getXRot(player),
                    false
            );
            java.util.UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
            if (teleported) {
                PENDING_BOX_TELEPORTS.remove(playerId);
            } else {
                PENDING_BOX_TELEPORTS.add(playerId);
            }
            giveWorldSettingsItem(player);
        }
    }

    public static void releasePlayersToSpawn(MinecraftServer server) {
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        ServerLevel level = ServerLevelUtil.getOverworld(server);
        if (level == null) {
            level = ServerLevelUtil.getAnyLevel(server);
        }
        if (level == null) {
            for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                data.releaseQuestsForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
            }
            data.pregameBoxActive = false;
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
            PregameSettingsWallManager.deactivateWallForAll(server);
            return;
        }

        BlockPos spawn = resolveSafeGameSpawnFast(level, data);
        setChunksForced(level, spawn, 2, true);
        float yaw = com.jamie.jamiebingo.util.LevelSpawnUtil.getSpawnYaw(level);

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player).clearContent();
            com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player).setChanged();
            com.jamie.jamiebingo.util.PlayerExperienceUtil.resetExperience(player);
            if (PlayerTrackerHandler.shouldGiveTracker(server)) {
                com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player).setItem(8, PlayerTrackerHandler.createTrackerItem(server, player));
            }
            boolean teleported = ServerPlayerTeleportUtil.teleport(
                    player,
                    level,
                    BlockPosUtil.getX(spawn) + 0.5,
                    BlockPosUtil.getY(spawn) + 0.1,
                    BlockPosUtil.getZ(spawn) + 0.5,
                    Set.of(),
                    yaw,
                    EntityRotationUtil.getXRot(player),
                    false
            );
            int safeUntil = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + 200;
            com.jamie.jamiebingo.util.NbtUtil.putInt(player.getPersistentData(), GAME_START_SAFE_UNTIL_TICK, safeUntil);
            player.resetFallDistance();
            player.setDeltaMovement(0.0, 0.0, 0.0);
            data.releaseQuestsForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
        }

        if (data.pregameBoxBuilt) {
            ServerLevel pregameLevel = getPregameLevel(server);
            BlockPos center = new BlockPos(data.pregameBoxX, data.pregameBoxY, data.pregameBoxZ);
            if (pregameLevel != null) {
                removeBox(pregameLevel, center);
            }
        }

        data.pregameBoxActive = false;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        PregameSettingsWallManager.deactivateWallForAll(server);
        final ServerLevel forcedLevel = level;
        final BlockPos forcedSpawn = spawn;
        server.schedule(new net.minecraft.server.TickTask(
                com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + 200,
                () -> setChunksForced(forcedLevel, forcedSpawn, 2, false)
        ));
    }

    public static void ensurePlayersReleased(MinecraftServer server) {
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isActive()) return;

        ServerLevel target = ServerLevelUtil.getOverworld(server);
        if (target == null) target = ServerLevelUtil.getAnyLevel(server);
        if (target == null) return;

        BlockPos spawn = resolveSafeGameSpawnFast(target, data);
        float yaw = com.jamie.jamiebingo.util.LevelSpawnUtil.getSpawnYaw(target);

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            if (player == null) continue;
            boolean wrongDimension = player.level() != target;
            boolean stillInBox = isInsideBox(player, data);
            if (!wrongDimension && !stillInBox) continue;

            boolean teleported = ServerPlayerTeleportUtil.teleport(
                    player,
                    target,
                    BlockPosUtil.getX(spawn) + 0.5,
                    BlockPosUtil.getY(spawn) + 0.1,
                    BlockPosUtil.getZ(spawn) + 0.5,
                    Set.of(),
                    yaw,
                    EntityRotationUtil.getXRot(player),
                    false
            );
            LOGGER.warn("[JamieBingo] ensurePlayersReleased corrected player={} wrongDimension={} stillInBox={} teleported={} nowDimension={}",
                    player.getName().getString(),
                    wrongDimension,
                    stillInBox,
                    teleported,
                    player.level().dimension());
        }
    }

    private static BlockPos resolveSafeGameSpawnFast(ServerLevel level, BingoGameData data) {
        if (level == null) return BlockPos.ZERO;
        BlockPos preferred = null;
        if (data != null && data.gameStartSpawnPrepared) {
            preferred = new BlockPos(data.gameStartSpawnX, data.gameStartSpawnY, data.gameStartSpawnZ);
        }
        if (preferred == null) {
            preferred = com.jamie.jamiebingo.util.LevelSpawnUtil.getSpawnPos(level);
        }

        int baseX = BlockPosUtil.getX(preferred);
        int baseZ = BlockPosUtil.getZ(preferred);
        int[][] offsets = new int[][] {
                {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {2, 0}, {-2, 0}, {0, 2}, {0, -2},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };

        for (int[] off : offsets) {
            int x = baseX + off[0];
            int z = baseZ + off[1];
            level.getChunk(x >> 4, z >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y < level.getSeaLevel() + 1) {
                y = level.getSeaLevel() + 1;
            }
            BlockPos pos = new BlockPos(x, y, z);
            if (isSafeStand(level, pos)) return pos;
        }

        level.getChunk(baseX >> 4, baseZ >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, baseX, baseZ);
        if (y < level.getSeaLevel() + 1) {
            y = level.getSeaLevel() + 1;
        }
        BlockPos fallback = new BlockPos(baseX, y, baseZ);
        if (isSafeStand(level, fallback)) return fallback;

        BlockPos robust = resolveSafeGameSpawn(level, data);
        if (robust != null && isSafeStand(level, robust)) return robust;

        // Never return a potentially unsafe fallback that can drop players into void.
        BlockPos worldSpawn = com.jamie.jamiebingo.util.LevelSpawnUtil.getSpawnPos(level);
        BlockPos checked = findSafeAtXZ(level, worldSpawn);
        return checked != null ? checked : fallback;
    }

    private static void setChunksForced(ServerLevel level, BlockPos center, int radius, boolean forced) {
        if (level == null || center == null) return;
        int r = Math.max(0, radius);
        ChunkPos cc = new ChunkPos(center);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                level.setChunkForced(cc.x + dx, cc.z + dz, forced);
            }
        }
    }

    public static void sendAllToBox(MinecraftServer server) {
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        ServerLevel level = getPregameLevel(server);
        if (level == null) return;

        data.pregameBoxBuilt = false;
        ensureBox(level, data);
        if (!data.pregameBoxActive) {
            data.pregameBoxActive = true;
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        }
        disablePvpInOverworld(server);

        BlockPos center = new BlockPos(data.pregameBoxX, data.pregameBoxY, data.pregameBoxZ);
        clearHostileMobsNearBox(level, center);
        BlockPos spawn = boxLobbySpawn(center);
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            com.jamie.jamiebingo.util.PlayerExperienceUtil.resetExperience(player);
            boolean teleported = ServerPlayerTeleportUtil.teleport(
                    player,
                    level,
                    BlockPosUtil.getX(spawn) + 0.5,
                    BlockPosUtil.getY(spawn) + 0.05,
                    BlockPosUtil.getZ(spawn) + 0.5,
                    Set.of(),
                    180.0F,
                    EntityRotationUtil.getXRot(player),
                    false
            );
            java.util.UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
            if (teleported) {
                PENDING_BOX_TELEPORTS.remove(playerId);
            } else {
                PENDING_BOX_TELEPORTS.add(playerId);
            }
            giveWorldSettingsItem(player);
        }
    }

    public static void handleRespawnToBox(ServerPlayer player) {
        if (player == null) return;
        MinecraftServer server = com.jamie.jamiebingo.util.ServerPlayerUtil.getServer(player);
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.pregameBoxActive) return;

        java.util.UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
        if (!PENDING_BOX_TELEPORTS.contains(playerId) && isInsideBox(player, data)) return;

        ServerLevel level = getPregameLevel(server);
        if (level == null) return;
        BlockPos center = new BlockPos(data.pregameBoxX, data.pregameBoxY, data.pregameBoxZ);
        BlockPos spawn = boxLobbySpawn(center);
        boolean teleported = ServerPlayerTeleportUtil.teleport(
                player,
                level,
                BlockPosUtil.getX(spawn) + 0.5,
                BlockPosUtil.getY(spawn) + 0.05,
                BlockPosUtil.getZ(spawn) + 0.5,
                Set.of(),
                180.0F,
                EntityRotationUtil.getXRot(player),
                false
        );
        if (teleported) {
            PENDING_BOX_TELEPORTS.remove(playerId);
            com.jamie.jamiebingo.item.BingoControllerGiveHandler.giveJoinItemsToPlayer(player);
            giveWorldSettingsItem(player);
        }
    }

    public static boolean isInsideBox(ServerPlayer player, BingoGameData data) {
        if (player == null || data == null) return false;
        if (!data.pregameBoxActive) return false;
        var server = com.jamie.jamiebingo.util.ServerPlayerUtil.getServer(player);
        if (server != null) {
            ServerLevel pregame = getPregameLevel(server);
            ServerLevel playerLevel = com.jamie.jamiebingo.util.ServerPlayerUtil.getLevel(player);
            if (pregame != null && playerLevel != null && playerLevel.dimension() != pregame.dimension()) {
                return false;
            }
        }

        int minX = data.pregameBoxX - BOX_HALF + 1;
        int maxX = data.pregameBoxX + BOX_HALF - 1;
        int minY = data.pregameBoxY + 1;
        int maxY = data.pregameBoxY + BOX_HEIGHT - 1;
        int minZ = data.pregameBoxZ - BOX_HALF + 1;
        int maxZ = data.pregameBoxZ + BOX_HALF - 1;

        var pos = com.jamie.jamiebingo.util.EntityPosUtil.getBlockPos(player);
        return BlockPosUtil.getX(pos) >= minX && BlockPosUtil.getX(pos) <= maxX
                && BlockPosUtil.getY(pos) >= minY && BlockPosUtil.getY(pos) <= maxY
                && BlockPosUtil.getZ(pos) >= minZ && BlockPosUtil.getZ(pos) <= maxZ;
    }

    private static void ensureBox(ServerLevel level, BingoGameData data) {
        BlockPos spawn = com.jamie.jamiebingo.util.LevelSpawnUtil.getSpawnPos(level);
        int spawnX = BlockPosUtil.getX(spawn);
        int spawnZ = BlockPosUtil.getZ(spawn);
        int desiredY = safeBoxY(level, spawnX, spawnZ);
        if (data.pregameBoxBuilt) {
            int maxAllowedY = level.getMaxY() - BOX_HEIGHT - 2;
            // Do not recompute terrain-safe height at an already-built location: the box itself
            // raises local heightmaps and can cause false invalidation/rebuild loops on join.
            boolean invalidY = data.pregameBoxY < DEFAULT_Y || data.pregameBoxY > maxAllowedY;
            if (invalidY) {
                BlockPos stale = new BlockPos(data.pregameBoxX, data.pregameBoxY, data.pregameBoxZ);
                removeBox(level, stale);
                data.pregameBoxBuilt = false;
                data.pregameBoxActive = false;
                com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
            }
        }
        if (data.pregameBoxBuilt) {
            if (!data.pregameBoxActive) {
                data.pregameBoxActive = true;
                com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
            }
            // Refresh wall visuals every time in case code changed or entities were cleared.
            BlockPos center = new BlockPos(data.pregameBoxX, data.pregameBoxY, data.pregameBoxZ);
            PregameSettingsWallManager.buildSettingsWall(level, center);
            refreshSpawnboxTipText(level, center);
            return;
        }

        int y = desiredY;

        BlockPos center = new BlockPos(spawnX, y, spawnZ);
        data.pregameBoxX = BlockPosUtil.getX(center);
        data.pregameBoxY = BlockPosUtil.getY(center);
        data.pregameBoxZ = BlockPosUtil.getZ(center);
        data.pregameBoxBuilt = true;
        data.pregameBoxActive = true;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

        buildBox(level, center);
    }

    private static ServerLevel getPregameLevel(MinecraftServer server) {
        if (server == null) return null;
        ServerLevel lobby = LobbyWorldManager.getLobby(server);
        if (lobby != null) return lobby;
        return ServerLevelUtil.getOverworld(server);
    }

    private static void giveWorldSettingsItem(ServerPlayer player) {
        if (player == null) return;
        var inv = com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player);
        if (inv == null) return;
        boolean has = com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(inv).stream()
                .anyMatch(stack -> stack != null && stack.getItem() == ModItems.WORLD_SETTINGS.get());
        if (!has) {
            com.jamie.jamiebingo.util.PlayerInventoryUtil.addItem(player, new net.minecraft.world.item.ItemStack(ModItems.WORLD_SETTINGS.get()));
        }
        com.jamie.jamiebingo.item.BingoControllerGiveHandler.arrangeLobbyHotbar(player, true);
    }

    private static int safeBoxY(ServerLevel level, int centerX, int centerZ) {
        if (level == null) return DEFAULT_Y;
        int maxAllowedY = level.getMaxY() - BOX_HEIGHT - 2;
        if (maxAllowedY < DEFAULT_Y) return maxAllowedY;

        // Force the spawnbox high in the sky by default.
        int skyPreferred = Math.max(DEFAULT_Y, maxAllowedY - 24);
        int maxSurfaceY = highestSurfaceY(level, centerX, centerZ);
        // Also keep enough clearance above local terrain.
        int minAboveSurface = maxSurfaceY + 16;
        int desired = Math.max(skyPreferred, minAboveSurface);
        if (desired > maxAllowedY) return maxAllowedY;
        return desired;
    }

    private static int highestSurfaceY(ServerLevel level, int centerX, int centerZ) {
        if (level == null) return DEFAULT_Y;
        int highest = level.getMinY();
        for (int x = centerX - BOX_HALF; x <= centerX + BOX_HALF; x++) {
            for (int z = centerZ - BOX_HALF; z <= centerZ + BOX_HALF; z++) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (y > highest) highest = y;
            }
        }
        return highest;
    }

    private static BlockPos resolveSafeGameSpawn(ServerLevel level, BingoGameData data) {
        if (level == null) return BlockPos.ZERO;
        BlockPos preferred = null;
        if (data != null && data.gameStartSpawnPrepared) {
            preferred = new BlockPos(data.gameStartSpawnX, data.gameStartSpawnY, data.gameStartSpawnZ);
        }
        if (preferred == null) {
            preferred = com.jamie.jamiebingo.util.LevelSpawnUtil.getSpawnPos(level);
        }

        BlockPos exact = findSafeAtXZ(level, preferred);
        if (exact != null) return exact;

        if (data != null && data.worldTypeMode == BingoGameData.WORLD_TYPE_SUPERFLAT) {
            BlockPos grass = findNearbyGrassSurface(level, preferred);
            if (grass != null) return grass;
        }

        BlockPos nearby = EffectRTP.findSafeSurface(
                level,
                preferred,
                new Random(System.nanoTime()),
                1024,
                192,
                false
        );
        if (nearby != null) return nearby;

        BlockPos worldSpawn = com.jamie.jamiebingo.util.LevelSpawnUtil.getSpawnPos(level);
        BlockPos fallback = findSafeAtXZ(level, worldSpawn);
        BlockPos out = fallback != null ? fallback : worldSpawn;
        if (data != null && data.worldTypeMode == BingoGameData.WORLD_TYPE_SUPERFLAT) {
            BlockPos grass = findNearbyGrassSurface(level, out);
            if (grass != null) return grass;
        }
        return out;
    }

    private static BlockPos findSafeAtXZ(ServerLevel level, BlockPos reference) {
        if (level == null || reference == null) return null;
        int x = BlockPosUtil.getX(reference);
        int z = BlockPosUtil.getZ(reference);
        level.getChunk(x >> 4, z >> 4);
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (isSafeStand(level, pos)) return pos;
        BlockPos scanned = findNearbySurfaceSpawn(level, x, z);
        if (scanned != null) return scanned;
        for (int r = 1; r <= 8; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    int px = x + dx;
                    int pz = z + dz;
                    level.getChunk(px >> 4, pz >> 4);
                    int py = level.getHeight(Heightmap.Types.WORLD_SURFACE, px, pz);
                    BlockPos p = new BlockPos(px, py, pz);
                    if (isSafeStand(level, p)) return p;
                    BlockPos pScan = findNearbySurfaceSpawn(level, px, pz);
                    if (pScan != null) return pScan;
                }
            }
        }
        return null;
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
        BlockPos feet = pos;
        BlockPos head = pos.above();
        BlockPos floor = pos.below();
        if (!level.getBlockState(floor).isSolidRender()) return false;
        if (!level.getBlockState(feet).getFluidState().isEmpty()) return false;
        if (!level.getBlockState(head).getFluidState().isEmpty()) return false;
        return level.noCollision(new AABB(
                BlockPosUtil.getX(feet) + 0.2,
                BlockPosUtil.getY(feet),
                BlockPosUtil.getZ(feet) + 0.2,
                BlockPosUtil.getX(feet) + 0.8,
                BlockPosUtil.getY(feet) + 1.8,
                BlockPosUtil.getZ(feet) + 0.8
        ));
    }

    private static BlockPos findNearbyGrassSurface(ServerLevel level, BlockPos center) {
        if (level == null || center == null) return null;
        var grass = BlockLookupUtil.block("minecraft:grass_block");
        if (grass == null) return null;
        int cx = BlockPosUtil.getX(center);
        int cz = BlockPosUtil.getZ(center);
        for (int r = 0; r <= 24; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    int x = cx + dx;
                    int z = cz + dz;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos feet = new BlockPos(x, y, z);
                    BlockPos below = feet.below();
                    if (level.getBlockState(below).getBlock() != grass) continue;
                    if (isSafeStand(level, feet)) return feet;
                }
            }
        }
        return null;
    }

    private static void buildBox(ServerLevel level, BlockPos center) {
        int minX = BlockPosUtil.getX(center) - BOX_HALF;
        int maxX = BlockPosUtil.getX(center) + BOX_HALF;
        int minY = BlockPosUtil.getY(center);
        int maxY = BlockPosUtil.getY(center) + BOX_HEIGHT;
        int minZ = BlockPosUtil.getZ(center) - BOX_HALF;
        int maxZ = BlockPosUtil.getZ(center) + BOX_HALF;

        var bedrockBlock = BlockLookupUtil.block("minecraft:bedrock");
        var airBlock = BlockLookupUtil.block("minecraft:air");
        var lightBlock = BlockLookupUtil.block("minecraft:light");
        if (bedrockBlock == null || airBlock == null || lightBlock == null) {
            return;
        }

        BlockState bedrock = BlockStateUtil.defaultState(bedrockBlock);
        BlockState air = BlockStateUtil.defaultState(airBlock);
        BlockState lightBase = BlockStateUtil.defaultState(lightBlock);
        if (bedrock == null || air == null || lightBase == null) {
            return;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean wall =
                            x == minX || x == maxX ||
                            y == minY || y == maxY ||
                            z == minZ || z == maxZ;
                    LevelSetBlockUtil.setBlock(level, new BlockPos(x, y, z), wall ? bedrock : air, 3);
                }
            }
        }

        buildLobbyInterior(level, center, minX, maxX, minY, maxY, minZ, maxZ);

        var lightLevelProp = LightBlockUtil.levelProperty();
        if (lightLevelProp == null) {
            return;
        }
        BlockState light = BlockStateUtil.setValue(lightBase, lightLevelProp, 15);

        int lightY = maxY - 1;
        for (int x = minX + 2; x <= maxX - 2; x += 4) {
            for (int z = minZ + 2; z <= maxZ - 2; z += 4) {
                LevelSetBlockUtil.setBlock(level, new BlockPos(x, lightY, z), light, 3);
            }
        }

        // Fill darker corners around decoration.
        LevelSetBlockUtil.setBlock(level, new BlockPos(minX + 2, minY + 2, minZ + 2), light, 3);
        LevelSetBlockUtil.setBlock(level, new BlockPos(minX + 2, minY + 2, maxZ - 2), light, 3);
        LevelSetBlockUtil.setBlock(level, new BlockPos(maxX - 2, minY + 2, minZ + 2), light, 3);
        LevelSetBlockUtil.setBlock(level, new BlockPos(maxX - 2, minY + 2, maxZ - 2), light, 3);
        addAmbientLightVolume(level, minX, maxX, minY, maxY, minZ, maxZ, light);

        PregameSettingsWallManager.buildSettingsWall(level, center);
        refreshSpawnboxTipText(level, center);
    }

    private static void clearHostileMobsNearBox(ServerLevel level, BlockPos center) {
        if (level == null || center == null) return;
        int minX = BlockPosUtil.getX(center) - BOX_HALF - 4;
        int maxX = BlockPosUtil.getX(center) + BOX_HALF + 4;
        int minY = BlockPosUtil.getY(center) - 64;
        int maxY = BlockPosUtil.getY(center) + BOX_HEIGHT + 4;
        int minZ = BlockPosUtil.getZ(center) - BOX_HALF - 4;
        int maxZ = BlockPosUtil.getZ(center) + BOX_HALF + 4;
        AABB area = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area, m -> m != null && m.getType().getCategory() == MobCategory.MONSTER)) {
            mob.discard();
        }
    }

    private static void addAmbientLightVolume(
            ServerLevel level,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ,
            BlockState light
    ) {
        if (level == null || light == null) return;

        // Dense ceiling lattice.
        int ceilingY = maxY - 1;
        for (int x = minX + 1; x <= maxX - 1; x += 2) {
            for (int z = minZ + 1; z <= maxZ - 1; z += 2) {
                LevelSetBlockUtil.setBlock(level, new BlockPos(x, ceilingY, z), light, 3);
            }
        }

        // Mid-air light grid to suppress dark pockets in a very large room.
        for (int y = minY + 4; y <= maxY - 4; y += 6) {
            for (int x = minX + 3; x <= maxX - 3; x += 6) {
                for (int z = minZ + 3; z <= maxZ - 3; z += 6) {
                    LevelSetBlockUtil.setBlock(level, new BlockPos(x, y, z), light, 3);
                }
            }
        }
    }

    private static void removeBox(ServerLevel level, BlockPos center) {
        int minX = BlockPosUtil.getX(center) - BOX_HALF;
        int maxX = BlockPosUtil.getX(center) + BOX_HALF;
        int minY = BlockPosUtil.getY(center);
        int maxY = BlockPosUtil.getY(center) + BOX_HEIGHT;
        int minZ = BlockPosUtil.getZ(center) - BOX_HALF;
        int maxZ = BlockPosUtil.getZ(center) + BOX_HALF;

        var airBlock = BlockLookupUtil.block("minecraft:air");
        if (airBlock == null) {
            return;
        }
        BlockState air = BlockStateUtil.defaultState(airBlock);
        if (air == null) {
            return;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    LevelSetBlockUtil.setBlock(level, new BlockPos(x, y, z), air, 3);
                }
            }
        }
        clearSpawnboxTipText(level, center);
    }

    private static void disablePvpInOverworld(MinecraftServer server) {
        if (server == null) return;
        ServerLevel overworld = ServerLevelUtil.getOverworld(server);
        if (overworld == null) return;
        net.minecraft.world.level.gamerules.GameRules rules =
                com.jamie.jamiebingo.util.GameRulesUtil.getGameRules(overworld);
        if (rules == null) return;
        rules.set(net.minecraft.world.level.gamerules.GameRules.PVP, false, server);
    }

    private static BlockPos boxLobbySpawn(BlockPos center) {
        if (center == null) return BlockPos.ZERO;
        // Spawn on top of the raised central platform.
        return new BlockPos(
                BlockPosUtil.getX(center),
                BlockPosUtil.getY(center) + 4,
                BlockPosUtil.getZ(center)
        );
    }

    private static void buildLobbyInterior(
            ServerLevel level,
            BlockPos center,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ
    ) {
        BlockState quartz = BlockStateUtil.defaultState(BlockLookupUtil.block("minecraft:smooth_quartz"));
        BlockState andesite = BlockStateUtil.defaultState(BlockLookupUtil.block("minecraft:polished_andesite"));
        BlockState dark = BlockStateUtil.defaultState(BlockLookupUtil.block("minecraft:deepslate_tiles"));
        BlockState leaves = BlockStateUtil.defaultState(BlockLookupUtil.block("minecraft:oak_leaves"));
        BlockState logs = BlockStateUtil.defaultState(BlockLookupUtil.block("minecraft:oak_log"));
        BlockState grass = BlockStateUtil.defaultState(BlockLookupUtil.block("minecraft:moss_block"));
        BlockState air = BlockStateUtil.defaultState(BlockLookupUtil.block("minecraft:air"));
        if (quartz == null || andesite == null || dark == null || leaves == null || logs == null || grass == null || air == null) {
            return;
        }

        int floorY = minY + 1;
        int cx = BlockPosUtil.getX(center);
        int cz = BlockPosUtil.getZ(center);

        // Floor pattern.
        for (int x = minX + 1; x <= maxX - 1; x++) {
            for (int z = minZ + 1; z <= maxZ - 1; z++) {
                boolean checker = ((x + z) & 1) == 0;
                LevelSetBlockUtil.setBlock(level, new BlockPos(x, floorY, z), checker ? dark : andesite, 3);
            }
        }

        // Cross walkways.
        for (int x = minX + 2; x <= maxX - 2; x++) {
            LevelSetBlockUtil.setBlock(level, new BlockPos(x, floorY, cz), quartz, 3);
            LevelSetBlockUtil.setBlock(level, new BlockPos(x, floorY, cz - 1), quartz, 3);
            LevelSetBlockUtil.setBlock(level, new BlockPos(x, floorY, cz + 1), quartz, 3);
        }
        for (int z = minZ + 2; z <= maxZ - 2; z++) {
            LevelSetBlockUtil.setBlock(level, new BlockPos(cx, floorY, z), quartz, 3);
            LevelSetBlockUtil.setBlock(level, new BlockPos(cx - 1, floorY, z), quartz, 3);
            LevelSetBlockUtil.setBlock(level, new BlockPos(cx + 1, floorY, z), quartz, 3);
        }

        // Raised central spawn platform (2 blocks above main floor).
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                LevelSetBlockUtil.setBlock(level, new BlockPos(x, floorY + 1, z), andesite, 3);
                LevelSetBlockUtil.setBlock(level, new BlockPos(x, floorY + 2, z), andesite, 3);
            }
        }
        // No top border trim: keep edges open so players can re-enter platform easily.

        // 2-step stairs on either side (west/east) to reach the higher platform.
        BlockState quartzStairs = BlockStateUtil.defaultState(BlockLookupUtil.block("minecraft:smooth_quartz_stairs"));
        if (quartzStairs != null) {
            BlockState westStairs = BlockStateUtil.setValue(quartzStairs, BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
            BlockState eastStairs = BlockStateUtil.setValue(quartzStairs, BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
            if (westStairs == null) westStairs = quartzStairs;
            if (eastStairs == null) eastStairs = quartzStairs;
            for (int z = cz - 1; z <= cz + 1; z++) {
                // West side: walk east and ascend floorY -> floorY+1 -> floorY+2.
                LevelSetBlockUtil.setBlock(level, new BlockPos(cx - 5, floorY, z), westStairs, 3);
                LevelSetBlockUtil.setBlock(level, new BlockPos(cx - 4, floorY + 1, z), westStairs, 3);
                // East side: walk west and ascend floorY -> floorY+1 -> floorY+2.
                LevelSetBlockUtil.setBlock(level, new BlockPos(cx + 5, floorY, z), eastStairs, 3);
                LevelSetBlockUtil.setBlock(level, new BlockPos(cx + 4, floorY + 1, z), eastStairs, 3);
            }
        }

        // Corner planters with tiny trees.
        buildPlanter(level, minX + 4, floorY, minZ + 4, grass, logs, leaves);
        buildPlanter(level, minX + 4, floorY, maxZ - 4, grass, logs, leaves);
        buildPlanter(level, maxX - 4, floorY, minZ + 4, grass, logs, leaves);
        buildPlanter(level, maxX - 4, floorY, maxZ - 4, grass, logs, leaves);

        // Air corridor in front of settings wall.
        int wallZ = minZ + 2;
        for (int x = cx - 34; x <= cx + 34; x++) {
            for (int y = floorY + 2; y <= maxY - 2; y++) {
                LevelSetBlockUtil.setBlock(level, new BlockPos(x, y, wallZ + 1), air, 3);
            }
        }
    }

    private static void buildPlanter(
            ServerLevel level,
            int x,
            int floorY,
            int z,
            BlockState grass,
            BlockState logs,
            BlockState leaves
    ) {
        for (int px = x - 1; px <= x + 1; px++) {
            for (int pz = z - 1; pz <= z + 1; pz++) {
                LevelSetBlockUtil.setBlock(level, new BlockPos(px, floorY + 1, pz), grass, 3);
            }
        }
        for (int y = floorY + 2; y <= floorY + 4; y++) {
            LevelSetBlockUtil.setBlock(level, new BlockPos(x, y, z), logs, 3);
        }
        for (int lx = x - 2; lx <= x + 2; lx++) {
            for (int lz = z - 2; lz <= z + 2; lz++) {
                if (Math.abs(lx - x) + Math.abs(lz - z) > 3) continue;
                LevelSetBlockUtil.setBlock(level, new BlockPos(lx, floorY + 5, lz), leaves, 3);
            }
        }
        LevelSetBlockUtil.setBlock(level, new BlockPos(x, floorY + 6, z), leaves, 3);
    }

    private static void refreshSpawnboxTipText(ServerLevel level, BlockPos center) {
        if (level == null || center == null) return;
        clearSpawnboxTipText(level, center);
        int cx = BlockPosUtil.getX(center);
        int baseY = BlockPosUtil.getY(center) + 9;
        int z = BlockPosUtil.getZ(center) + 10;
        for (int i = 0; i < SPAWNBOX_TIPS.length; i++) {
            spawnTipLine(level, cx + 0.5, baseY + (SPAWNBOX_TIPS.length - 1 - i) * 0.6, z + 0.5, SPAWNBOX_TIPS[i]);
        }
    }

    private static void clearSpawnboxTipText(ServerLevel level, BlockPos center) {
        if (level == null || center == null) return;
        AABB area = new AABB(
                BlockPosUtil.getX(center) - BOX_HALF,
                BlockPosUtil.getY(center) + 1,
                BlockPosUtil.getZ(center) - BOX_HALF,
                BlockPosUtil.getX(center) + BOX_HALF + 1,
                BlockPosUtil.getY(center) + BOX_HEIGHT + 2,
                BlockPosUtil.getZ(center) + BOX_HALF + 1
        );
        for (Entity e : level.getEntities((Entity) null, area, entity -> entity != null && entity.getTags().contains(TIP_TEXT_TAG))) {
            e.discard();
        }
    }

    private static void spawnTipLine(ServerLevel level, double x, double y, double z, String text) {
        if (level == null || text == null || text.isBlank()) return;
        ArmorStand stand = EntityType.ARMOR_STAND.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (stand == null) return;
        stand.setPos(x, y, z);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        stand.setCustomNameVisible(true);
        stand.setInvulnerable(true);
        stand.setCustomName(Component.literal("\u00a7f" + text));
        stand.addTag(TIP_TEXT_TAG);
        level.addFreshEntity(stand);
    }

}






