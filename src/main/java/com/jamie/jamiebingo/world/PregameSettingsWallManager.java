package com.jamie.jamiebingo.world;

import com.jamie.jamiebingo.JamieBingo;
import com.jamie.jamiebingo.bingo.CardComposition;
import com.jamie.jamiebingo.bingo.WinCondition;
import com.jamie.jamiebingo.casino.CasinoModeManager;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.menu.ControllerSettingsSnapshot;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketGlobalWallSettingsSync;
import com.jamie.jamiebingo.util.BlockLookupUtil;
import com.jamie.jamiebingo.util.BlockPosUtil;
import com.jamie.jamiebingo.util.BlockStateUtil;
import com.jamie.jamiebingo.util.LevelSetBlockUtil;
import com.jamie.jamiebingo.util.SavedDataUtil;
import com.jamie.jamiebingo.util.ServerTickUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID)
public final class PregameSettingsWallManager {

    private static final int SLIDER_LENGTH = 9;
    private static final String SCREEN_TAG = "jamiebingo_global_screen";

    private static final Map<MinecraftServer, SharedSettings> SHARED_BY_SERVER = new WeakHashMap<>();
    private static final Map<MinecraftServer, ControllerSettingsSnapshot> SNAPSHOT_BY_SERVER = new WeakHashMap<>();
    private static final Map<MinecraftServer, Integer> PAGE_BY_SERVER = new WeakHashMap<>();
    private static final Map<UUID, DragState> ACTIVE_DRAGS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> NEXT_SYNC_TICK = new ConcurrentHashMap<>();

    private PregameSettingsWallManager() {
    }

    public static void buildSettingsWall(ServerLevel level, BlockPos center) {
        if (level == null || center == null || level.getServer() == null) return;

        MinecraftServer server = level.getServer();
        BingoGameData data = BingoGameData.get(server);
        SharedSettings shared = shared(server, data);
        ControllerSettingsSnapshot snapshot = snapshotFor(server, data);
        applySnapshot(shared, snapshot);

        BlockState frame = BlockStateUtil.defaultState(BlockLookupUtil.block("minecraft:polished_deepslate"));
        BlockState screen = BlockStateUtil.defaultState(BlockLookupUtil.block("minecraft:black_concrete"));
        BlockState glass = BlockStateUtil.defaultState(BlockLookupUtil.block("minecraft:tinted_glass"));
        BlockState light = lightState(12);
        if (frame == null || screen == null || glass == null || light == null) return;

        int cx = BlockPosUtil.getX(center);
        int floorY = BlockPosUtil.getY(center) + 1;
        int wallZ = BlockPosUtil.getZ(center) - PregameBoxManager.BOX_HALF + 2;
        int y0 = floorY + 2;

        for (int x = cx - 35; x <= cx + 35; x++) {
            for (int y = y0; y <= y0 + 33; y++) {
                boolean edge = x == cx - 35 || x == cx + 35 || y == y0 || y == y0 + 33;
                LevelSetBlockUtil.setBlock(level, new BlockPos(x, y, wallZ), edge ? frame : screen, 3);
            }
        }
        for (int x = cx - 34; x <= cx + 34; x++) {
            LevelSetBlockUtil.setBlock(level, new BlockPos(x, y0 + 33, wallZ), glass, 3);
            if (((x + cx) & 1) == 0) {
                LevelSetBlockUtil.setBlock(level, new BlockPos(x, y0 + 32, wallZ + 1), light, 3);
            }
        }

        redrawControls(level, center, shared);
        redrawDigitalText(level, center, shared);
        broadcastToAll(server, center, snapshot);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level() == null || player.level().isClientSide()) return;

        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (!data.pregameBoxActive || !PregameBoxManager.isInsideBox(player, data)) return;

        BlockPos center = new BlockPos(data.pregameBoxX, data.pregameBoxY, data.pregameBoxZ);
        Hit hit = resolveHit(center, event.getPos());
        if (hit == null) return;
        // Digital wall input is client-forwarded through PacketGlobalWallAction.
        // Consume wall block clicks here so legacy block-hit controls do not conflict.
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        if (player.level() == null || player.level().isClientSide()) return;

        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        UUID id = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
        boolean inBox = data.pregameBoxActive && PregameBoxManager.isInsideBox(player, data);
        if (!inBox) {
            ACTIVE_DRAGS.remove(id);
            NEXT_SYNC_TICK.remove(id);
            return;
        }

        int now = ServerTickUtil.getTickCount(server);
        SharedSettings shared = shared(server, data);
        ControllerSettingsSnapshot snapshot = snapshotFor(server, data);
        applySnapshot(shared, snapshot);
        BlockPos center = new BlockPos(data.pregameBoxX, data.pregameBoxY, data.pregameBoxZ);
        int next = NEXT_SYNC_TICK.getOrDefault(id, 0);
        if (now >= next) {
            sendSyncToPlayer(server, player, center, snapshot);
            NEXT_SYNC_TICK.put(id, now + 20);
        }

        ACTIVE_DRAGS.remove(id);
    }

    private static void redrawAll(MinecraftServer server, BlockPos center, SharedSettings shared) {
        if (server == null || center == null || shared == null) return;
        ServerLevel level = LobbyWorldManager.getLobby(server);
        if (level == null) {
            level = com.jamie.jamiebingo.util.ServerLevelUtil.getOverworld(server);
        }
        if (level == null) return;
        redrawControls(level, center, shared);
        redrawDigitalText(level, center, shared);
    }

    private static void redrawControls(ServerLevel level, BlockPos center, SharedSettings shared) {
        if (level == null || center == null || shared == null) return;

        int cx = BlockPosUtil.getX(center);
        int floorY = BlockPosUtil.getY(center) + 1;
        int wallZ = BlockPosUtil.getZ(center) - PregameBoxManager.BOX_HALF + 2;
        int y0 = floorY + 2;

        BlockState dark = BlockStateUtil.defaultState(BlockLookupUtil.block("minecraft:black_concrete"));
        if (dark == null) return;

        for (int x = cx - 34; x <= cx + 34; x++) {
            for (int y = y0 + 1; y <= y0 + 32; y++) {
                LevelSetBlockUtil.setBlock(level, new BlockPos(x, y, wallZ), dark, 3);
            }
        }
    }

    private static void redrawDigitalText(ServerLevel level, BlockPos center, SharedSettings shared) {
        if (level == null || center == null || shared == null) return;
        int cx = BlockPosUtil.getX(center);
        int floorY = BlockPosUtil.getY(center) + 1;
        int wallZ = BlockPosUtil.getZ(center) - PregameBoxManager.BOX_HALF + 2;

        AABB area = new AABB(cx - 36, floorY + 2, wallZ + 0.2, cx + 36, floorY + 40, wallZ + 2.5);
        for (Entity e : level.getEntities((Entity) null, area, en -> en.getTags().contains(SCREEN_TAG))) {
            e.discard();
        }
    }

    private static void addText(ServerLevel level, double x, double y, double z, String text, int alpha, boolean bold) {
        ArmorStand stand = EntityType.ARMOR_STAND.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (stand == null) return;
        stand.setPos(x, y, z);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        stand.setCustomNameVisible(true);
        stand.setCustomName(Component.literal(colorPrefix(alpha) + (bold ? text.toUpperCase() : text)));
        stand.addTag(SCREEN_TAG);
        level.addFreshEntity(stand);
    }

    private static Hit resolveHit(BlockPos center, BlockPos clicked) {
        for (ButtonControl control : ButtonControl.values()) {
            BlockPos pos = controlPos(center, control);
            if (BlockPosUtil.getZ(clicked) == BlockPosUtil.getZ(pos)
                    && Math.abs(BlockPosUtil.getX(clicked) - BlockPosUtil.getX(pos)) <= 1
                    && Math.abs(BlockPosUtil.getY(clicked) - BlockPosUtil.getY(pos)) <= 1) {
                return new Hit(control, null);
            }
        }
        for (SliderControl slider : SliderControl.values()) {
            int y = sliderY(center, slider);
            int z = sliderZ(center);
            int left = sliderLeft(center, slider);
            int right = left + SLIDER_LENGTH - 1;
            if (BlockPosUtil.getY(clicked) == y && BlockPosUtil.getZ(clicked) == z
                    && BlockPosUtil.getX(clicked) >= left && BlockPosUtil.getX(clicked) <= right) {
                return new Hit(null, slider);
            }
        }
        return null;
    }

    private static BlockPos controlPos(BlockPos center, ButtonControl control) {
        int cx = BlockPosUtil.getX(center);
        int floorY = BlockPosUtil.getY(center) + 1;
        int z = sliderZ(center);
        return switch (control) {
            case START -> new BlockPos(cx - 10, floorY + 10, z);
            case WIN -> new BlockPos(cx - 7, floorY + 10, z);
            case COMPOSITION -> new BlockPos(cx - 4, floorY + 10, z);
            case PVP -> new BlockPos(cx + 2, floorY + 10, z);
            case HUNGER -> new BlockPos(cx + 4, floorY + 10, z);
            case HOSTILE -> new BlockPos(cx + 6, floorY + 10, z);
            case KEEP_INV -> new BlockPos(cx + 8, floorY + 10, z);
            case HARDCORE -> new BlockPos(cx + 2, floorY + 8, z);
            case LATE_JOIN -> new BlockPos(cx + 4, floorY + 8, z);
            case TEAM_SYNC -> new BlockPos(cx + 6, floorY + 8, z);
            case COUNTDOWN -> new BlockPos(cx + 8, floorY + 8, z);
        };
    }

    private static int sliderLeft(BlockPos center, SliderControl slider) {
        int cx = BlockPosUtil.getX(center);
        return switch (slider) {
            case SIZE -> cx - 10;
            case QUEST -> cx - 10;
            case START_DELAY -> cx - 10;
            case COUNTDOWN_MINUTES -> cx - 10;
        };
    }

    private static int sliderY(BlockPos center, SliderControl slider) {
        int floorY = BlockPosUtil.getY(center) + 1;
        return switch (slider) {
            case SIZE -> floorY + 8;
            case QUEST -> floorY + 7;
            case START_DELAY -> floorY + 6;
            case COUNTDOWN_MINUTES -> floorY + 5;
        };
    }

    private static int sliderZ(BlockPos center) {
        return BlockPosUtil.getZ(center) - PregameBoxManager.BOX_HALF + 2;
    }

    private static void applyButton(SharedSettings shared, ButtonControl control) {
        if (shared == null || control == null) return;
        switch (control) {
            case START -> {}
            case WIN -> shared.winCondition = nextWin(shared.winCondition);
            case COMPOSITION -> shared.composition = nextComposition(shared.composition);
            case PVP -> shared.pvpEnabled = !shared.pvpEnabled;
            case HUNGER -> shared.hungerEnabled = !shared.hungerEnabled;
            case HOSTILE -> shared.hostileMobsEnabled = !shared.hostileMobsEnabled;
            case KEEP_INV -> shared.keepInventoryEnabled = !shared.keepInventoryEnabled;
            case HARDCORE -> shared.hardcoreEnabled = !shared.hardcoreEnabled;
            case LATE_JOIN -> shared.allowLateJoin = !shared.allowLateJoin;
            case TEAM_SYNC -> shared.teamSyncEnabled = !shared.teamSyncEnabled;
            case COUNTDOWN -> shared.countdownEnabled = !shared.countdownEnabled;
        }
    }

    private static boolean applySliderFromLook(ServerPlayer player, SharedSettings shared, BlockPos center, SliderControl slider) {
        if (player == null || shared == null || center == null || slider == null) return false;
        Vec3 hit = sliderHitPoint(player, center, slider);
        if (hit == null) return false;

        int left = sliderLeft(center, slider);
        double percent = (hit.x - left) / (SLIDER_LENGTH - 1);
        percent = Math.max(0.0D, Math.min(1.0D, percent));
        return setSliderPercent(shared, slider, percent);
    }

    private static Vec3 sliderHitPoint(ServerPlayer player, BlockPos center, SliderControl slider) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        double dz = look.z;
        if (Math.abs(dz) < 1.0E-5) return null;

        double planeZ = sliderZ(center) + 1.0D;
        double t = (planeZ - eye.z) / dz;
        if (t <= 0.0D || t > 8.0D) return null;

        Vec3 hit = eye.add(look.scale(t));
        double sliderY = sliderY(center, slider) + 0.5D;
        if (Math.abs(hit.y - sliderY) > 0.85D) return null;
        return hit;
    }

    private static boolean isLookingAtSlider(ServerPlayer player, BlockPos center, SliderControl slider) {
        return sliderHitPoint(player, center, slider) != null;
    }

    private static boolean setSliderPercent(SharedSettings shared, SliderControl slider, double percent) {
        if (shared == null || slider == null) return false;
        switch (slider) {
            case SIZE -> {
                int value = Math.max(1, Math.min(10, 1 + (int) Math.round(percent * 9.0D)));
                if (shared.size == value) return false;
                shared.size = value;
                return true;
            }
            case QUEST -> {
                int value = Math.max(0, Math.min(100, (int) Math.round(percent * 100.0D)));
                if (shared.questPercent == value) return false;
                shared.questPercent = value;
                return true;
            }
            case START_DELAY -> {
                int value = Math.max(0, Math.min(60, (int) Math.round(percent * 60.0D)));
                if (shared.bingoStartDelaySeconds == value) return false;
                shared.bingoStartDelaySeconds = value;
                return true;
            }
            case COUNTDOWN_MINUTES -> {
                int value = Math.max(10, Math.min(60, 10 + (int) Math.round(percent * 50.0D)));
                if (shared.countdownMinutes == value) return false;
                shared.countdownMinutes = value;
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private static double sliderPercent(SharedSettings shared, SliderControl slider) {
        if (shared == null || slider == null) return 0.0D;
        return switch (slider) {
            case SIZE -> (shared.size - 1) / 9.0D;
            case QUEST -> shared.questPercent / 100.0D;
            case START_DELAY -> shared.bingoStartDelaySeconds / 60.0D;
            case COUNTDOWN_MINUTES -> (shared.countdownMinutes - 10) / 50.0D;
        };
    }

    private static String sliderValueText(SharedSettings shared, SliderControl slider) {
        if (shared == null || slider == null) return "";
        return switch (slider) {
            case SIZE -> Integer.toString(shared.size);
            case QUEST -> shared.questPercent + "%";
            case START_DELAY -> shared.bingoStartDelaySeconds + "s";
            case COUNTDOWN_MINUTES -> shared.countdownMinutes + "m";
        };
    }

    private static String rowCycle(String label) {
        return "<  " + label + "  >";
    }

    private static String rowSlider(String label, int value, int min, int max) {
        int len = 14;
        double p = (double) (value - min) / Math.max(1.0D, (double) (max - min));
        p = Math.max(0.0D, Math.min(1.0D, p));
        int pos = (int) Math.round(p * (len - 1));
        StringBuilder bar = new StringBuilder();
        bar.append("[");
        for (int i = 0; i < len; i++) {
            bar.append(i == pos ? "|" : "-");
        }
        bar.append("]");
        return label + bar + " " + value;
    }

    private static SharedSettings shared(MinecraftServer server, BingoGameData data) {
        if (server == null) return new SharedSettings();
        synchronized (SHARED_BY_SERVER) {
            SharedSettings existing = SHARED_BY_SERVER.get(server);
            if (existing != null) return existing;
            SharedSettings created = SharedSettings.fromData(data);
            SHARED_BY_SERVER.put(server, created);
            return created;
        }
    }

    public static ControllerSettingsSnapshot sharedSnapshot(MinecraftServer server) {
        if (server == null) return null;
        BingoGameData data = BingoGameData.get(server);
        return snapshotFor(server, data);
    }

    public static boolean isPregameActive(MinecraftServer server) {
        if (server == null) return false;
        return BingoGameData.get(server).pregameBoxActive;
    }

    public static BlockPos currentCenter(MinecraftServer server) {
        if (server == null) return null;
        BingoGameData data = BingoGameData.get(server);
        return new BlockPos(data.pregameBoxX, data.pregameBoxY, data.pregameBoxZ);
    }

    public static void applySharedSnapshot(MinecraftServer server, ControllerSettingsSnapshot snapshot, int settingsPage) {
        if (server == null || snapshot == null) return;
        BingoGameData data = BingoGameData.get(server);
        SharedSettings shared = shared(server, data);
        setSnapshotFor(server, snapshot);
        setPageFor(server, settingsPage);
        applySnapshot(shared, snapshot);
        BlockPos center = currentCenter(server);
        redrawAll(server, center, shared);
        broadcastToAll(server, center, snapshot);
    }

    public static void refreshFromData(MinecraftServer server) {
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        ControllerSettingsSnapshot snapshot = snapshotFromData(data);
        SharedSettings refreshedShared = SharedSettings.fromData(data);
        synchronized (SHARED_BY_SERVER) {
            SNAPSHOT_BY_SERVER.put(server, snapshot);
            SHARED_BY_SERVER.put(server, refreshedShared);
        }
        BlockPos center = currentCenter(server);
        if (data.pregameBoxActive) {
            redrawAll(server, center, refreshedShared);
        }
        broadcastToAll(server, center, snapshot);
    }

    public static void startFromShared(ServerPlayer player) {
        if (player == null) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (!data.pregameBoxActive || !PregameBoxManager.isInsideBox(player, data)) return;
        if (data.worldRegenInProgress || data.worldRegenQueued) {
            player.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(
                    "World regeneration " + com.jamie.jamiebingo.world.WorldRegenerationManager.regenStatusText(data) + ". Please wait."
            ));
            return;
        }
        SharedSettings shared = shared(server, data);
        ControllerSettingsSnapshot snapshot = snapshotFor(server, data);
        applySnapshot(shared, snapshot);
        applySnapshotToGameData(snapshot, data, server);
        if (data.startGame(server)) {
            CasinoModeManager.startPregamePhasesOrFinalize(server, data);
        }
        SavedDataUtil.markDirty(data);
        BlockPos center = currentCenter(server);
        broadcastToAll(server, center, snapshot);
    }

    private static void applySnapshotToGameData(ControllerSettingsSnapshot snapshot, BingoGameData data, MinecraftServer server) {
        if (snapshot == null || data == null || server == null) return;

        java.util.Random rng = new java.util.Random();

        data.winCondition = snapshot.win() == null ? WinCondition.LINE : snapshot.win();
        data.randomWinConditionIntent = data.winCondition == WinCondition.RANDOM;
        data.composition =
                snapshot.questMode() == 1 ? CardComposition.HYBRID_CATEGORY :
                snapshot.questMode() == 2 ? CardComposition.HYBRID_PERCENT :
                        CardComposition.CLASSIC_ONLY;
        data.questPercent = snapshot.questPercent();
        data.categoryLogicEnabled = snapshot.categoryLogicEnabled();
        data.rarityLogicEnabled = snapshot.rarityLogicEnabled();
        data.itemColorVariantsSeparate = snapshot.itemColorVariantsSeparate();
        data.size = Math.max(1, Math.min(10, snapshot.cardSize()));
        data.randomSizeIntent = snapshot.randomCardSize();

        if (snapshot.rerollsMode() == 1) {
            data.rerollsPerPlayer = snapshot.rerollsCount();
            data.randomRerollsIntent = false;
        } else if (snapshot.rerollsMode() == 2) {
            data.rerollsPerPlayer = 0;
            data.randomRerollsIntent = true;
        } else {
            data.rerollsPerPlayer = 0;
            data.randomRerollsIntent = false;
        }

        data.gunGameLength = snapshot.gunRounds();
        data.hangmanRounds = snapshot.hangmanRounds();
        data.hangmanBaseSeconds = Math.max(10, snapshot.hangmanBaseSeconds());
        data.hangmanPenaltySeconds = Math.max(0, snapshot.hangmanPenaltySeconds());

        data.difficulty = snapshot.cardDifficulty();
        data.randomDifficultyIntent = "random".equalsIgnoreCase(snapshot.cardDifficulty());

        if (snapshot.effectsInterval() < 0) {
            data.randomEffectsIntervalIntent = true;
            data.randomEffectsArmed = false;
            data.randomEffectsIntervalSeconds = 0;
        } else {
            data.randomEffectsArmed = snapshot.effectsInterval() > 0;
            data.randomEffectsIntervalIntent = false;
            data.randomEffectsIntervalSeconds = snapshot.effectsInterval();
        }

        if (snapshot.randomRtp()) {
            data.rtpEnabled = rng.nextBoolean();
        } else {
            data.rtpEnabled = snapshot.rtpEnabled();
        }
        data.randomRtpIntent = false;

        if (snapshot.randomPvp()) {
            data.randomPvpIntent = true;
        } else {
            data.pvpEnabled = snapshot.pvpEnabled();
            data.adventureMode = snapshot.adventureMode();
            data.randomPvpIntent = false;
        }

        if (snapshot.randomHostileMobs()) {
            data.hostileMobsEnabled = rng.nextBoolean();
        } else {
            data.hostileMobsEnabled = snapshot.hostileMobsEnabled();
        }
        data.randomHostileMobsIntent = false;

        if (snapshot.randomHunger()) {
            data.hungerEnabled = rng.nextBoolean();
        } else {
            data.hungerEnabled = snapshot.hungerEnabled();
        }
        data.randomHungerIntent = false;

        if (snapshot.randomKeepInventory()) {
            data.randomKeepInventoryIntent = true;
        } else {
            data.keepInventoryEnabled = snapshot.keepInventoryEnabled();
            data.randomKeepInventoryIntent = false;
        }

        if (snapshot.randomHardcore()) {
            data.randomHardcoreIntent = true;
        } else {
            data.hardcoreEnabled = snapshot.hardcoreEnabled();
            data.randomHardcoreIntent = false;
        }

        if (snapshot.randomDaylight()) {
            data.randomDaylightIntent = true;
        } else {
            int mode = snapshot.daylightMode();
            if (mode < BingoGameData.DAYLIGHT_ENABLED || mode > BingoGameData.DAYLIGHT_DUSK) {
                mode = BingoGameData.DAYLIGHT_ENABLED;
            }
            data.daylightMode = mode;
            data.randomDaylightIntent = false;
        }

        data.bingoStartDelaySeconds = Math.max(0, snapshot.startDelaySeconds());
        data.countdownEnabled = snapshot.countdownEnabled();
        data.countdownMinutes = snapshot.countdownEnabled() ? Math.max(10, snapshot.countdownMinutes()) : 0;
        data.rushEnabled = snapshot.rushEnabled();
        data.rushSeconds = Math.max(1, Math.min(300, snapshot.rushSeconds()));
        data.allowLateJoin = snapshot.allowLateJoin();
        data.registerMode = snapshot.registerMode() == BingoGameData.REGISTER_ALWAYS_HAVE
                ? BingoGameData.REGISTER_ALWAYS_HAVE
                : BingoGameData.REGISTER_COLLECT_ONCE;
        data.teamSyncEnabled = snapshot.teamSyncEnabled();
        data.teamChestEnabled = snapshot.teamChestEnabled();
        data.shuffleEnabled = snapshot.shuffleMode() == BingoGameData.SHUFFLE_ENABLED;
        data.randomShuffleIntent = snapshot.shuffleMode() == BingoGameData.SHUFFLE_RANDOM;

        Difficulty diff = switch (snapshot.gameDifficulty()) {
            case "easy" -> Difficulty.EASY;
            case "hard" -> Difficulty.HARD;
            default -> Difficulty.NORMAL;
        };
        server.setDifficulty(diff, true);

        boolean casinoAllowed = BingoGameData.isCasinoAllowedForWin(data.winCondition);
        data.casinoMode = casinoAllowed
                ? Math.max(BingoGameData.CASINO_DISABLED, Math.min(BingoGameData.CASINO_DRAFT, snapshot.casinoMode()))
                : BingoGameData.CASINO_DISABLED;
        CasinoModeManager.setCasinoEnabled(casinoAllowed && data.casinoMode == BingoGameData.CASINO_ENABLED);
        if (!data.winCondition.equals(WinCondition.FULL)
                && !data.winCondition.equals(WinCondition.LOCKOUT)
                && !data.winCondition.equals(WinCondition.RARITY)) {
            data.shuffleEnabled = false;
            data.randomShuffleIntent = false;
        }
        boolean randomMinesIntent = !snapshot.minesEnabled() && snapshot.mineAmount() <= 0;
        if (randomMinesIntent) {
            data.minesEnabled = rng.nextBoolean();
            data.mineAmount = data.minesEnabled ? 1 + rng.nextInt(13) : Math.max(1, Math.min(13, data.mineAmount));
            data.mineTimeSeconds = data.minesEnabled ? 1 + rng.nextInt(120) : Math.max(1, snapshot.mineTimeSeconds());
        } else {
            data.minesEnabled = snapshot.minesEnabled();
            data.mineAmount = Math.max(1, Math.min(13, snapshot.mineAmount()));
            data.mineTimeSeconds = Math.max(1, snapshot.mineTimeSeconds());
        }
        boolean randomPowerIntent = !snapshot.powerSlotEnabled() && snapshot.powerSlotIntervalSeconds() <= 0;
        if (randomPowerIntent) {
            data.powerSlotEnabled = rng.nextBoolean();
            data.powerSlotIntervalSeconds = data.powerSlotEnabled
                    ? com.jamie.jamiebingo.util.RandomSettingResolver.pickPowerSlotInterval(rng)
                    : Math.max(10, Math.min(300, data.powerSlotIntervalSeconds));
        } else {
            data.powerSlotEnabled = snapshot.powerSlotEnabled();
            data.powerSlotIntervalSeconds = Math.max(10, Math.min(300, snapshot.powerSlotIntervalSeconds()));
        }
        if (data.winCondition == WinCondition.LOCKOUT
                || data.winCondition == WinCondition.RARITY
                || data.winCondition == WinCondition.GAMEGUN) {
            data.powerSlotEnabled = false;
        }
    }

    private static void applySnapshot(SharedSettings shared, ControllerSettingsSnapshot snapshot) {
        if (shared == null || snapshot == null) return;
        shared.winCondition = snapshot.win() == null ? WinCondition.LINE : snapshot.win();
        shared.composition = snapshot.questMode() == 1
                ? CardComposition.HYBRID_CATEGORY
                : snapshot.questMode() == 2
                ? CardComposition.HYBRID_PERCENT
                : CardComposition.CLASSIC_ONLY;
        shared.questPercent = Math.max(0, Math.min(100, snapshot.questPercent()));
        shared.size = Math.max(1, Math.min(10, snapshot.cardSize()));
        shared.pvpEnabled = snapshot.pvpEnabled();
        shared.hungerEnabled = snapshot.hungerEnabled();
        shared.hostileMobsEnabled = snapshot.hostileMobsEnabled();
        shared.keepInventoryEnabled = snapshot.keepInventoryEnabled();
        shared.hardcoreEnabled = snapshot.hardcoreEnabled();
        shared.allowLateJoin = snapshot.allowLateJoin();
        shared.teamSyncEnabled = snapshot.teamSyncEnabled();
        shared.teamChestEnabled = snapshot.teamChestEnabled();
        shared.naturalRegenEnabled = snapshot.naturalRegenEnabled();
        shared.bingoStartDelaySeconds = Math.max(0, Math.min(120, snapshot.startDelaySeconds()));
        shared.countdownEnabled = snapshot.countdownEnabled();
        shared.countdownMinutes = Math.max(10, Math.min(180, snapshot.countdownMinutes()));
        shared.rtpEnabled = snapshot.rtpEnabled();
        shared.adventureMode = snapshot.adventureMode();
        shared.prelitPortalsMode = snapshot.prelitPortalsMode();
    }

    private static ControllerSettingsSnapshot toSnapshot(SharedSettings shared) {
        if (shared == null) {
            return new ControllerSettingsSnapshot(
                    WinCondition.LINE, 0, 50, false, false, false, false, 0, 0, 5, 5, 5, 60, 10,
                    "normal", "normal", 0, false, false, true, false, true, true, false, false,
                    5, false, false, false, false, false, 0, false,
                    0, false, 10, false, 60, false, true, false, BingoGameData.PRELIT_PORTALS_OFF, false, 0, false, false, true, BingoGameData.SHUFFLE_DISABLED,
                    BingoGameData.STARTER_KIT_DISABLED, false,
                    false, 1, 15,
                    false, 60,
                    false, 2
            );
        }
        int questMode = shared.composition == CardComposition.HYBRID_CATEGORY ? 1
                : shared.composition == CardComposition.HYBRID_PERCENT ? 2 : 0;
        return new ControllerSettingsSnapshot(
                shared.winCondition,
                questMode,
                shared.questPercent,
                false,
                false,
                false,
                false,
                0,
                0,
                5,
                5,
                5,
                60,
                10,
                "normal",
                "normal",
                0,
                shared.rtpEnabled,
                false,
                shared.hostileMobsEnabled,
                false,
                shared.hungerEnabled,
                shared.naturalRegenEnabled,
                false,
                false,
                shared.size,
                false,
                shared.keepInventoryEnabled,
                false,
                shared.hardcoreEnabled,
                false,
                BingoGameData.DAYLIGHT_ENABLED,
                false,
                shared.bingoStartDelaySeconds,
                shared.countdownEnabled,
                shared.countdownMinutes,
                false,
                60,
                shared.allowLateJoin,
                shared.pvpEnabled,
                shared.adventureMode,
                shared.prelitPortalsMode,
                false,
                BingoGameData.REGISTER_COLLECT_ONCE,
                false,
                shared.teamSyncEnabled,
                shared.teamChestEnabled,
                BingoGameData.SHUFFLE_DISABLED,
                BingoGameData.STARTER_KIT_DISABLED,
                false,
                false,
                1,
                15,
                false,
                60,
                false,
                2
        );
    }

    private static ControllerSettingsSnapshot snapshotFor(MinecraftServer server, BingoGameData data) {
        if (server == null) return null;
        synchronized (SHARED_BY_SERVER) {
            ControllerSettingsSnapshot existing = SNAPSHOT_BY_SERVER.get(server);
            if (existing != null) return existing;
            ControllerSettingsSnapshot created = snapshotFromData(data);
            SNAPSHOT_BY_SERVER.put(server, created);
            return created;
        }
    }

    private static void setSnapshotFor(MinecraftServer server, ControllerSettingsSnapshot snapshot) {
        if (server == null || snapshot == null) return;
        synchronized (SHARED_BY_SERVER) {
            SNAPSHOT_BY_SERVER.put(server, snapshot);
        }
    }

    private static ControllerSettingsSnapshot snapshotFromData(BingoGameData data) {
        if (data == null) return defaultControllerSnapshot();
        int questMode = data.composition == CardComposition.HYBRID_CATEGORY ? 1
                : data.composition == CardComposition.HYBRID_PERCENT ? 2 : 0;
        int rerollsMode = data.randomRerollsIntent ? 2 : (data.rerollsPerPlayer > 0 ? 1 : 0);
        int effectsInterval = data.randomEffectsIntervalIntent ? -1
                : (data.randomEffectsArmed ? data.randomEffectsIntervalSeconds : 0);
        String cardDifficulty = data.randomDifficultyIntent ? "random" : data.difficulty;
        return new ControllerSettingsSnapshot(
                data.winCondition == null ? WinCondition.LINE : data.winCondition,
                questMode,
                data.questPercent,
                data.categoryLogicEnabled,
                data.rarityLogicEnabled,
                data.itemColorVariantsSeparate,
                data.casinoMode != BingoGameData.CASINO_DISABLED,
                data.casinoMode,
                rerollsMode,
                data.rerollsPerPlayer,
                data.gunGameLength,
                data.hangmanRounds,
                data.hangmanBaseSeconds,
                data.hangmanPenaltySeconds,
                cardDifficulty == null || cardDifficulty.isBlank() ? "normal" : cardDifficulty,
                "normal",
                effectsInterval,
                data.rtpEnabled,
                data.randomRtpIntent,
                data.hostileMobsEnabled,
                data.randomHostileMobsIntent,
                data.hungerEnabled,
                data.naturalRegenEnabled,
                false,
                data.randomHungerIntent,
                Math.max(1, Math.min(10, data.size)),
                data.randomSizeIntent,
                data.keepInventoryEnabled,
                data.randomKeepInventoryIntent,
                data.hardcoreEnabled,
                data.randomHardcoreIntent,
                data.daylightMode,
                data.randomDaylightIntent,
                Math.max(0, data.bingoStartDelaySeconds),
                data.countdownEnabled,
                Math.max(10, data.countdownMinutes),
                data.rushEnabled,
                Math.max(1, Math.min(300, data.rushSeconds)),
                data.allowLateJoin,
                data.pvpEnabled,
                data.adventureMode,
                data.prelitPortalsMode,
                data.randomPvpIntent,
                data.registerMode,
                false,
                data.teamSyncEnabled,
                data.teamChestEnabled,
                data.randomShuffleIntent ? BingoGameData.SHUFFLE_RANDOM
                        : (data.shuffleEnabled ? BingoGameData.SHUFFLE_ENABLED : BingoGameData.SHUFFLE_DISABLED),
                data.starterKitMode,
                data.hideGoalDetailsInChat,
                data.minesEnabled,
                Math.max(1, Math.min(13, data.mineAmount)),
                Math.max(1, data.mineTimeSeconds),
                data.powerSlotEnabled,
                Math.max(10, Math.min(300, data.powerSlotIntervalSeconds)),
                data.fakeRerollsEnabled,
                Math.max(1, Math.min(10, data.fakeRerollsPerPlayer))
        );
    }

    private static ControllerSettingsSnapshot defaultControllerSnapshot() {
        return new ControllerSettingsSnapshot(
                WinCondition.LINE,
                0,
                50,
                true,
                true,
                true,
                false,
                BingoGameData.CASINO_DISABLED,
                0,
                0,
                5,
                5,
                60,
                10,
                "normal",
                "normal",
                0,
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                false,
                5,
                false,
                false,
                false,
                false,
                false,
                BingoGameData.DAYLIGHT_ENABLED,
                false,
                0,
                false,
                10,
                false,
                60,
                false,
                true,
                false,
                BingoGameData.PRELIT_PORTALS_OFF,
                false,
                BingoGameData.REGISTER_COLLECT_ONCE,
                false,
                false,
                true,
                BingoGameData.SHUFFLE_DISABLED,
                BingoGameData.STARTER_KIT_DISABLED,
                false,
                false,
                1,
                15,
                false,
                60,
                false,
                2
        );
    }

    private static void broadcastToAll(MinecraftServer server, BlockPos center, ControllerSettingsSnapshot snapshot) {
        if (server == null || center == null || snapshot == null) return;
        boolean isActive = BingoGameData.get(server).pregameBoxActive;
        int settingsPage = getPageFor(server);
        PacketGlobalWallSettingsSync packet = new PacketGlobalWallSettingsSync(
                isActive,
                center,
                snapshot,
                settingsPage
        );
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            NetworkHandler.sendToPlayer(target, packet);
        }
    }

    private static void sendSyncToPlayer(MinecraftServer server, ServerPlayer player, BlockPos center, ControllerSettingsSnapshot snapshot) {
        if (server == null || player == null || center == null || snapshot == null) return;
        boolean isActive = BingoGameData.get(server).pregameBoxActive;
        NetworkHandler.sendToPlayer(player, new PacketGlobalWallSettingsSync(isActive, center, snapshot, getPageFor(server)));
    }

    public static void deactivateWallForAll(MinecraftServer server) {
        if (server == null) return;
        ControllerSettingsSnapshot snapshot = snapshotFor(server, BingoGameData.get(server));
        BlockPos center = currentCenter(server);
        PacketGlobalWallSettingsSync packet = new PacketGlobalWallSettingsSync(
                false,
                center,
                snapshot,
                getPageFor(server)
        );
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            NetworkHandler.sendToPlayer(target, packet);
        }
    }

    private static int getPageFor(MinecraftServer server) {
        if (server == null) return 0;
        synchronized (SHARED_BY_SERVER) {
            return Math.max(0, PAGE_BY_SERVER.getOrDefault(server, 0));
        }
    }

    private static void setPageFor(MinecraftServer server, int settingsPage) {
        if (server == null) return;
        synchronized (SHARED_BY_SERVER) {
            PAGE_BY_SERVER.put(server, Math.max(0, settingsPage));
        }
    }

    private static WinCondition nextWin(WinCondition current) {
        WinCondition[] values = {
                WinCondition.LINE,
                WinCondition.FULL,
                WinCondition.LOCKOUT,
                WinCondition.RARITY,
                WinCondition.BLIND,
                WinCondition.HANGMAN,
                WinCondition.GUNGAME,
                WinCondition.GAMEGUN
        };
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) return values[(i + 1) % values.length];
        }
        return WinCondition.LINE;
    }

    private static CardComposition nextComposition(CardComposition current) {
        if (current == CardComposition.CLASSIC_ONLY) return CardComposition.HYBRID_CATEGORY;
        if (current == CardComposition.HYBRID_CATEGORY) return CardComposition.HYBRID_PERCENT;
        return CardComposition.CLASSIC_ONLY;
    }

    private static BlockState lightState(int level) {
        BlockState light = BlockStateUtil.defaultState(BlockLookupUtil.block("minecraft:light"));
        var prop = com.jamie.jamiebingo.util.LightBlockUtil.levelProperty();
        if (light == null || prop == null) return null;
        return BlockStateUtil.setValue(light, prop, Math.max(0, Math.min(15, level)));
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static String colorPrefix(int alpha) {
        // Use bright white for higher-alpha lines and gray for lower-alpha.
        return alpha >= 220 ? "\u00a7f" : "\u00a77";
    }

    private static String onOffWord(boolean value) {
        return value ? "On" : "Off";
    }

    private static String compositionLabel(CardComposition composition) {
        if (composition == CardComposition.HYBRID_CATEGORY) return "Categories";
        if (composition == CardComposition.HYBRID_PERCENT) return "Percent";
        return "Classic";
    }

    private enum ButtonControl {
        START("Start"),
        WIN("Win Mode"),
        COMPOSITION("Composition"),
        PVP("PVP"),
        HUNGER("Hunger"),
        HOSTILE("Hostile Mobs"),
        KEEP_INV("Keep Inventory"),
        HARDCORE("Hardcore"),
        LATE_JOIN("Allow Late Join"),
        TEAM_SYNC("Team Sync"),
        COUNTDOWN("Countdown");

        private final String label;

        ButtonControl(String label) {
            this.label = label;
        }
    }

    private enum SliderControl {
        SIZE("Card Size"),
        QUEST("Quest Percent"),
        START_DELAY("Start Delay"),
        COUNTDOWN_MINUTES("Countdown");

        private final String label;

        SliderControl(String label) {
            this.label = label;
        }
    }

    private record Hit(ButtonControl button, SliderControl slider) {}

    private record DragState(SliderControl slider, int untilTick) {}

    private static final class SharedSettings {
        private WinCondition winCondition = WinCondition.LINE;
        private CardComposition composition = CardComposition.CLASSIC_ONLY;
        private int size = 5;
        private int questPercent = 50;
        private int bingoStartDelaySeconds = 0;
        private boolean countdownEnabled = false;
        private int countdownMinutes = 10;
        private boolean rtpEnabled = true;
        private boolean pvpEnabled = true;
        private boolean adventureMode = false;
        private int prelitPortalsMode = BingoGameData.PRELIT_PORTALS_OFF;
        private boolean hungerEnabled = true;
        private boolean naturalRegenEnabled = true;
        private boolean hostileMobsEnabled = true;
        private boolean keepInventoryEnabled = false;
        private boolean hardcoreEnabled = false;
        private boolean allowLateJoin = false;
        private boolean teamSyncEnabled = false;
        private boolean teamChestEnabled = true;

        private static SharedSettings fromData(BingoGameData data) {
            SharedSettings s = new SharedSettings();
            if (data == null) return s;
            s.winCondition = data.winCondition == null ? WinCondition.LINE : data.winCondition;
            s.composition = data.composition == null ? CardComposition.CLASSIC_ONLY : data.composition;
            s.size = Math.max(1, Math.min(10, data.size));
            s.questPercent = Math.max(0, Math.min(100, data.questPercent < 0 ? 50 : data.questPercent));
            s.bingoStartDelaySeconds = Math.max(0, Math.min(60, data.bingoStartDelaySeconds));
            s.countdownEnabled = data.countdownEnabled;
            s.countdownMinutes = Math.max(10, Math.min(60, data.countdownMinutes));
            s.rtpEnabled = data.rtpEnabled;
            s.pvpEnabled = data.pvpEnabled;
            s.adventureMode = data.adventureMode;
            s.prelitPortalsMode = BingoGameData.clampPrelitPortalsMode(data.prelitPortalsMode);
            s.hungerEnabled = data.hungerEnabled;
            s.naturalRegenEnabled = data.naturalRegenEnabled;
            s.hostileMobsEnabled = data.hostileMobsEnabled;
            s.keepInventoryEnabled = data.keepInventoryEnabled;
            s.hardcoreEnabled = data.hardcoreEnabled;
            s.allowLateJoin = data.allowLateJoin;
            s.teamSyncEnabled = data.teamSyncEnabled;
            s.teamChestEnabled = data.teamChestEnabled;
            return s;
        }
    }
}
