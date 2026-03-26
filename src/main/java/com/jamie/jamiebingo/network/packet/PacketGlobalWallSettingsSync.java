package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.client.ClientPacketHandlers;
import com.jamie.jamiebingo.menu.ControllerSettingsSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class PacketGlobalWallSettingsSync {
    private final boolean active;
    private final BlockPos center;
    private final ControllerSettingsSnapshot snapshot;
    private final int settingsPage;

    public PacketGlobalWallSettingsSync(boolean active, BlockPos center, ControllerSettingsSnapshot snapshot, int settingsPage) {
        this.active = active;
        this.center = center;
        this.snapshot = snapshot;
        this.settingsPage = Math.max(0, settingsPage);
    }

    public static void encode(PacketGlobalWallSettingsSync msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        buf.writeBoolean(msg.center != null);
        if (msg.center != null) {
            buf.writeBlockPos(msg.center);
        }
        buf.writeInt(msg.settingsPage);
        writeSnapshot(buf, msg.snapshot);
    }

    public static PacketGlobalWallSettingsSync decode(FriendlyByteBuf buf) {
        boolean active = buf.readBoolean();
        BlockPos center = buf.readBoolean() ? buf.readBlockPos() : null;
        int settingsPage = Math.max(0, buf.readInt());
        ControllerSettingsSnapshot snapshot = readSnapshot(buf);
        return new PacketGlobalWallSettingsSync(active, center, snapshot, settingsPage);
    }

    public static void handle(PacketGlobalWallSettingsSync msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> ClientPacketHandlers.handleGlobalWallSync(msg.active, msg.center, msg.snapshot, msg.settingsPage));
        ctx.setPacketHandled(true);
    }

    public ControllerSettingsSnapshot snapshot() {
        return snapshot;
    }

    public static void writeSnapshot(FriendlyByteBuf buf, ControllerSettingsSnapshot s) {
        ControllerSettingsSnapshot snap = s == null ? defaultSnapshot() : s;
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeEnum(buf, snap.win());
        buf.writeInt(snap.questMode());
        buf.writeInt(snap.questPercent());
        buf.writeBoolean(snap.categoryLogicEnabled());
        buf.writeBoolean(snap.rarityLogicEnabled());
        buf.writeBoolean(snap.itemColorVariantsSeparate());
        buf.writeBoolean(snap.casino());
        buf.writeInt(snap.casinoMode());
        buf.writeInt(snap.rerollsMode());
        buf.writeInt(snap.rerollsCount());
        buf.writeInt(snap.gunRounds());
        buf.writeInt(snap.hangmanRounds());
        buf.writeInt(snap.hangmanBaseSeconds());
        buf.writeInt(snap.hangmanPenaltySeconds());
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, snap.cardDifficulty());
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, snap.gameDifficulty());
        buf.writeInt(snap.effectsInterval());
        buf.writeBoolean(snap.rtpEnabled());
        buf.writeBoolean(snap.randomRtp());
        buf.writeBoolean(snap.hostileMobsEnabled());
        buf.writeBoolean(snap.randomHostileMobs());
        buf.writeBoolean(snap.hungerEnabled());
        buf.writeBoolean(snap.naturalRegenEnabled());
        buf.writeBoolean(snap.randomNaturalRegen());
        buf.writeBoolean(snap.randomHunger());
        buf.writeInt(snap.cardSize());
        buf.writeBoolean(snap.randomCardSize());
        buf.writeBoolean(snap.keepInventoryEnabled());
        buf.writeBoolean(snap.randomKeepInventory());
        buf.writeBoolean(snap.hardcoreEnabled());
        buf.writeBoolean(snap.randomHardcore());
        buf.writeInt(snap.daylightMode());
        buf.writeBoolean(snap.randomDaylight());
        buf.writeInt(snap.startDelaySeconds());
        buf.writeBoolean(snap.countdownEnabled());
        buf.writeInt(snap.countdownMinutes());
        buf.writeBoolean(snap.rushEnabled());
        buf.writeInt(snap.rushSeconds());
        buf.writeBoolean(snap.allowLateJoin());
        buf.writeBoolean(snap.pvpEnabled());
        buf.writeBoolean(snap.adventureMode());
        buf.writeInt(snap.prelitPortalsMode());
        buf.writeBoolean(snap.randomPvp());
        buf.writeInt(snap.registerMode());
        buf.writeBoolean(snap.randomRegister());
        buf.writeBoolean(snap.teamSyncEnabled());
        buf.writeBoolean(snap.teamChestEnabled());
        buf.writeInt(snap.shuffleMode());
        buf.writeInt(snap.starterKitMode());
        buf.writeBoolean(snap.hideGoalDetailsInChat());
        buf.writeBoolean(snap.minesEnabled());
        buf.writeInt(snap.mineAmount());
        buf.writeInt(snap.mineTimeSeconds());
        buf.writeBoolean(snap.powerSlotEnabled());
        buf.writeInt(snap.powerSlotIntervalSeconds());
        buf.writeBoolean(snap.fakeRerollsEnabled());
        buf.writeInt(snap.fakeRerollsPerPlayer());
    }

    public static ControllerSettingsSnapshot readSnapshot(FriendlyByteBuf buf) {
        com.jamie.jamiebingo.bingo.WinCondition win =
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readEnum(buf, com.jamie.jamiebingo.bingo.WinCondition.class);
        int questMode = buf.readInt();
        int questPercent = buf.readInt();
        boolean categoryLogicEnabled = buf.readBoolean();
        boolean rarityLogicEnabled = buf.readBoolean();
        boolean itemColorVariantsSeparate = buf.readBoolean();
        boolean casino = buf.readBoolean();
        int casinoMode = buf.readInt();
        int rerollsMode = buf.readInt();
        int rerollsCount = buf.readInt();
        int gunRounds = buf.readInt();
        int hangmanRounds = buf.readInt();
        int hangmanBaseSeconds = buf.readInt();
        int hangmanPenaltySeconds = buf.readInt();
        String cardDifficulty = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
        String gameDifficulty = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
        int effectsInterval = buf.readInt();
        boolean rtpEnabled = buf.readBoolean();
        boolean randomRtp = buf.readBoolean();
        boolean hostileMobsEnabled = buf.readBoolean();
        boolean randomHostileMobs = buf.readBoolean();
        boolean hungerEnabled = buf.readBoolean();
        boolean naturalRegenEnabled = buf.readBoolean();
        boolean randomNaturalRegen = buf.readBoolean();
        boolean randomHunger = buf.readBoolean();
        int cardSize = buf.readInt();
        boolean randomCardSize = buf.readBoolean();
        boolean keepInventoryEnabled = buf.readBoolean();
        boolean randomKeepInventory = buf.readBoolean();
        boolean hardcoreEnabled = buf.readBoolean();
        boolean randomHardcore = buf.readBoolean();
        int daylightMode = buf.readInt();
        boolean randomDaylight = buf.readBoolean();
        int startDelaySeconds = buf.readInt();
        boolean countdownEnabled = buf.readBoolean();
        int countdownMinutes = buf.readInt();
        boolean rushEnabled = buf.readBoolean();
        int rushSeconds = buf.readInt();
        boolean allowLateJoin = buf.readBoolean();
        boolean pvpEnabled = buf.readBoolean();
        boolean adventureMode = buf.readBoolean();
        int prelitPortalsMode = buf.readInt();
        boolean randomPvp = buf.readBoolean();
        int registerMode = buf.readInt();
        boolean randomRegister = buf.readBoolean();
        boolean teamSyncEnabled = buf.readBoolean();
        boolean teamChestEnabled = buf.readBoolean();
        int shuffleMode = buf.readInt();
        int starterKitMode = buf.readInt();
        boolean hideGoalDetailsInChat = buf.readBoolean();
        boolean minesEnabled = buf.readBoolean();
        int mineAmount = buf.readInt();
        int mineTimeSeconds = buf.readInt();
        boolean powerSlotEnabled = buf.readBoolean();
        int powerSlotIntervalSeconds = buf.readInt();
        boolean fakeRerollsEnabled = buf.readBoolean();
        int fakeRerollsPerPlayer = buf.readInt();

        return new ControllerSettingsSnapshot(
                win, questMode, questPercent, categoryLogicEnabled, rarityLogicEnabled, itemColorVariantsSeparate,
                casino, casinoMode, rerollsMode, rerollsCount, gunRounds, hangmanRounds, hangmanBaseSeconds, hangmanPenaltySeconds,
                cardDifficulty, gameDifficulty, effectsInterval, rtpEnabled, randomRtp, hostileMobsEnabled, randomHostileMobs,
                hungerEnabled, naturalRegenEnabled, randomNaturalRegen, randomHunger, cardSize, randomCardSize, keepInventoryEnabled, randomKeepInventory,
                hardcoreEnabled, randomHardcore, daylightMode, randomDaylight, startDelaySeconds, countdownEnabled, countdownMinutes,
                rushEnabled, rushSeconds, allowLateJoin, pvpEnabled, adventureMode, prelitPortalsMode, randomPvp, registerMode, randomRegister, teamSyncEnabled,
                teamChestEnabled,
                shuffleMode, starterKitMode, hideGoalDetailsInChat, minesEnabled, mineAmount, mineTimeSeconds, powerSlotEnabled,
                powerSlotIntervalSeconds, fakeRerollsEnabled, fakeRerollsPerPlayer
        );
    }

    private static ControllerSettingsSnapshot defaultSnapshot() {
        return new ControllerSettingsSnapshot(
                com.jamie.jamiebingo.bingo.WinCondition.LINE,
                0,
                50,
                false,
                false,
                false,
                false,
                0,
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
                com.jamie.jamiebingo.data.BingoGameData.DAYLIGHT_ENABLED,
                false,
                0,
                false,
                10,
                false,
                60,
                false,
                true,
                false,
                com.jamie.jamiebingo.data.BingoGameData.PRELIT_PORTALS_OFF,
                false,
                com.jamie.jamiebingo.data.BingoGameData.REGISTER_COLLECT_ONCE,
                false,
                false,
                true,
                com.jamie.jamiebingo.data.BingoGameData.SHUFFLE_DISABLED,
                com.jamie.jamiebingo.data.BingoGameData.STARTER_KIT_DISABLED,
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
}
