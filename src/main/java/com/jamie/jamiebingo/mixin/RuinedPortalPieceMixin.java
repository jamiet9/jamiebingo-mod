package com.jamie.jamiebingo.mixin;

import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.RuinedPortalPiece;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(RuinedPortalPiece.class)
public abstract class RuinedPortalPieceMixin {
    private static final int MIN_PORTAL_WIDTH = 2;
    private static final int MIN_PORTAL_HEIGHT = 3;
    private static final int MAX_PORTAL_SIDE = 23;

    @Inject(method = "postProcess", at = @At("TAIL"))
    private void jamiebingo$prelightRuinedPortalAtPlacement(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            BlockPos pivot,
            CallbackInfo ci
    ) {
        if (!isNetherPortalPrelightingEnabled()) return;

        BoundingBox portalBox = ((StructurePiece) (Object) this).getBoundingBox();
        if (portalBox == null) return;
        int minX = portalBox.minX() - 1;
        int maxX = portalBox.maxX() + 1;
        int minY = portalBox.minY() - 1;
        int maxY = portalBox.maxY() + 1;
        int minZ = portalBox.minZ() - 1;
        int maxZ = portalBox.maxZ() + 1;

        repairAndLightPortal(level, minX, maxX, minY, maxY, minZ, maxZ);
    }

    private static boolean isNetherPortalPrelightingEnabled() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        BingoGameData data = BingoGameData.get(server);
        if (data == null) return false;
        int mode = BingoGameData.clampPrelitPortalsMode(data.prelitPortalsMode);
        return mode == BingoGameData.PRELIT_PORTALS_NETHER || mode == BingoGameData.PRELIT_PORTALS_BOTH;
    }

    private static boolean repairAndLightPortal(WorldGenLevel level, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        List<BlockPos> frameBlocks = collectFrameBlocks(level, minX, maxX, minY, maxY, minZ, maxZ);
        if (frameBlocks.isEmpty()) return false;

        List<BlockPos> component = largestComponent(frameBlocks);
        if (component.isEmpty()) return false;

        Direction.Axis axis = inferAxis(component);
        int planeCoord = dominantPlaneCoordinate(component, axis);

        int horizontalMin = Integer.MAX_VALUE;
        int horizontalMax = Integer.MIN_VALUE;
        int verticalMin = Integer.MAX_VALUE;
        int verticalMax = Integer.MIN_VALUE;

        for (BlockPos pos : component) {
            int plane = axis == Direction.Axis.X ? pos.getZ() : pos.getX();
            if (plane != planeCoord) continue;
            int horizontal = axis == Direction.Axis.X ? pos.getX() : pos.getZ();
            horizontalMin = Math.min(horizontalMin, horizontal);
            horizontalMax = Math.max(horizontalMax, horizontal);
            verticalMin = Math.min(verticalMin, pos.getY());
            verticalMax = Math.max(verticalMax, pos.getY());
        }

        if (horizontalMin == Integer.MAX_VALUE || verticalMin == Integer.MAX_VALUE) return false;

        int width = horizontalMax - horizontalMin - 1;
        int height = verticalMax - verticalMin - 1;
        if (width < MIN_PORTAL_WIDTH || height < MIN_PORTAL_HEIGHT) return false;
        if (width > MAX_PORTAL_SIDE || height > MAX_PORTAL_SIDE) return false;

        BlockPos interiorOrigin = axis == Direction.Axis.X
                ? new BlockPos(horizontalMin + 1, verticalMin + 1, planeCoord)
                : new BlockPos(planeCoord, verticalMin + 1, horizontalMin + 1);
        lightFrame(level, interiorOrigin, axis, width, height);
        return true;
    }

    private static void lightFrame(WorldGenLevel level, BlockPos interiorOrigin, Direction.Axis axis, int width, int height) {
        BlockState portalState = Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, axis);
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();

        for (int dx = -1; dx <= width; dx++) {
            for (int dy = -1; dy <= height; dy++) {
                BlockPos pos = offset(interiorOrigin, axis, dx, dy);
                boolean frame = dx == -1 || dx == width || dy == -1 || dy == height;
                if (frame) {
                    if (!level.getBlockState(pos).is(Blocks.OBSIDIAN)) {
                        level.setBlock(pos, obsidian, 3);
                    }
                } else if (!level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) {
                    level.setBlock(pos, portalState, 3);
                }
            }
        }
    }

    private static boolean isPortalInterior(BlockState state) {
        return state.isAir() || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.NETHER_PORTAL);
    }

    private static boolean isFrameBlock(BlockState state) {
        return state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN);
    }

    private static List<BlockPos> collectFrameBlocks(WorldGenLevel level, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        List<BlockPos> out = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    cursor.set(x, y, z);
                    if (isFrameBlock(level.getBlockState(cursor))) {
                        out.add(cursor.immutable());
                    }
                }
            }
        }
        return out;
    }

    private static List<BlockPos> largestComponent(List<BlockPos> blocks) {
        Set<BlockPos> all = new HashSet<>(blocks);
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> largest = List.of();

        for (BlockPos start : blocks) {
            if (!seen.add(start)) continue;
            List<BlockPos> component = new ArrayList<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                BlockPos current = queue.removeFirst();
                component.add(current);
                for (Direction direction : Direction.values()) {
                    BlockPos neighbor = current.relative(direction);
                    if (!all.contains(neighbor) || !seen.add(neighbor)) continue;
                    queue.addLast(neighbor);
                }
            }
            if (component.size() > largest.size()) {
                largest = component;
            }
        }
        return largest;
    }

    private static Direction.Axis inferAxis(List<BlockPos> component) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : component) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return (maxX - minX) >= (maxZ - minZ) ? Direction.Axis.X : Direction.Axis.Z;
    }

    private static int dominantPlaneCoordinate(List<BlockPos> component, Direction.Axis axis) {
        Map<Integer, Integer> counts = new HashMap<>();
        int bestCoord = 0;
        int bestCount = -1;
        for (BlockPos pos : component) {
            int key = axis == Direction.Axis.X ? pos.getZ() : pos.getX();
            int count = counts.merge(key, 1, Integer::sum);
            if (count > bestCount) {
                bestCount = count;
                bestCoord = key;
            }
        }
        return bestCoord;
    }

    private static BlockPos offset(BlockPos origin, Direction.Axis axis, int widthOffset, int heightOffset) {
        return axis == Direction.Axis.X
                ? origin.offset(widthOffset, heightOffset, 0)
                : origin.offset(0, heightOffset, widthOffset);
    }
}
