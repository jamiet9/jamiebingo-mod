package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.bingo.CardComposition;
import com.jamie.jamiebingo.bingo.WinCondition;
import com.jamie.jamiebingo.util.FriendlyByteBufUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PacketOpenBingoController {

    private final WinCondition win;
    private final CardComposition composition;
    private final int questPercent;
    private final boolean categoryLogicEnabled;
    private final boolean rarityLogicEnabled;
    private final boolean itemColorVariantsSeparate;
    private final boolean casino;
    private final int casinoMode;
    private final int rerollsMode;
    private final int rerollsCount;
    private final int gunRounds;
    private final int hangmanRounds;
    private final int hangmanSeconds;
    private final int hangmanPenalty;
    private final String cardDifficulty;
    private final boolean randomCardDifficulty;
    private final String gameDifficulty;
    private final boolean randomGameDifficulty;
    private final int effectsInterval;
    private final boolean randomEffectsInterval;
    private final boolean rtpEnabled;
    private final boolean randomRtp;
    private final boolean hostileMobsEnabled;
    private final boolean randomHostileMobs;
    private final boolean hungerEnabled;
    private final boolean naturalRegenEnabled;
    private final boolean randomNaturalRegen;
    private final boolean randomHunger;
    private final int cardSize;
    private final boolean randomCardSize;
    private final boolean keepInventoryEnabled;
    private final boolean randomKeepInventory;
    private final boolean hardcoreEnabled;
    private final boolean randomHardcore;
    private final int daylightMode;
    private final boolean randomDaylight;
    private final int startDelaySeconds;
    private final boolean countdownEnabled;
    private final int countdownMinutes;
    private final boolean rushEnabled;
    private final int rushSeconds;
    private final boolean allowLateJoin;
    private final boolean pvpEnabled;
    private final boolean adventureMode;
    private final int prelitPortalsMode;
    private final boolean randomPvp;
    private final int registerMode;
    private final boolean randomRegister;
    private final boolean teamSyncEnabled;
    private final boolean teamChestEnabled;
    private final int shuffleMode;
    private final int starterKitMode;
    private final boolean hideGoalDetailsInChat;
    private final boolean minesEnabled;
    private final int mineAmount;
    private final int mineTimeSeconds;
    private final boolean powerSlotEnabled;
    private final int powerSlotIntervalSeconds;

    public PacketOpenBingoController(
            WinCondition win,
            CardComposition composition,
            int questPercent,
            boolean categoryLogicEnabled,
            boolean rarityLogicEnabled,
            boolean itemColorVariantsSeparate,
            boolean casino,
            int casinoMode,
            int rerollsMode,
            int rerollsCount,
            int gunRounds,
            int hangmanRounds,
            int hangmanSeconds,
            int hangmanPenalty,
            String cardDifficulty,
            boolean randomCardDifficulty,
            String gameDifficulty,
            boolean randomGameDifficulty,
            int effectsInterval,
            boolean randomEffectsInterval,
            boolean rtpEnabled,
            boolean randomRtp,
            boolean hostileMobsEnabled,
            boolean randomHostileMobs,
            boolean hungerEnabled,
            boolean naturalRegenEnabled,
            boolean randomNaturalRegen,
            boolean randomHunger,
            int cardSize,
            boolean randomCardSize,
            boolean keepInventoryEnabled,
            boolean randomKeepInventory,
            boolean hardcoreEnabled,
            boolean randomHardcore,
            int daylightMode,
            boolean randomDaylight,
            int startDelaySeconds,
            boolean countdownEnabled,
            int countdownMinutes,
            boolean rushEnabled,
            int rushSeconds,
            boolean allowLateJoin,
            boolean pvpEnabled,
            boolean adventureMode,
            int prelitPortalsMode,
            boolean randomPvp,
            int registerMode,
            boolean randomRegister,
            boolean teamSyncEnabled,
            boolean teamChestEnabled,
            int shuffleMode,
            int starterKitMode,
            boolean hideGoalDetailsInChat,
            boolean minesEnabled,
            int mineAmount,
            int mineTimeSeconds,
            boolean powerSlotEnabled,
            int powerSlotIntervalSeconds
    ) {
        this.win = win;
        this.composition = composition;
        this.questPercent = questPercent;
        this.categoryLogicEnabled = categoryLogicEnabled;
        this.rarityLogicEnabled = rarityLogicEnabled;
        this.itemColorVariantsSeparate = itemColorVariantsSeparate;
        this.casino = casino;
        this.casinoMode = casinoMode;
        this.rerollsMode = rerollsMode;
        this.rerollsCount = rerollsCount;
        this.gunRounds = gunRounds;
        this.hangmanRounds = hangmanRounds;
        this.hangmanSeconds = hangmanSeconds;
        this.hangmanPenalty = hangmanPenalty;
        this.cardDifficulty = cardDifficulty;
        this.randomCardDifficulty = randomCardDifficulty;
        this.gameDifficulty = gameDifficulty;
        this.randomGameDifficulty = randomGameDifficulty;
        this.effectsInterval = effectsInterval;
        this.randomEffectsInterval = randomEffectsInterval;
        this.rtpEnabled = rtpEnabled;
        this.randomRtp = randomRtp;
        this.hostileMobsEnabled = hostileMobsEnabled;
        this.randomHostileMobs = randomHostileMobs;
        this.hungerEnabled = hungerEnabled;
        this.naturalRegenEnabled = naturalRegenEnabled;
        this.randomNaturalRegen = randomNaturalRegen;
        this.randomHunger = randomHunger;
        this.cardSize = cardSize;
        this.randomCardSize = randomCardSize;
        this.keepInventoryEnabled = keepInventoryEnabled;
        this.randomKeepInventory = randomKeepInventory;
        this.hardcoreEnabled = hardcoreEnabled;
        this.randomHardcore = randomHardcore;
        this.daylightMode = daylightMode;
        this.randomDaylight = randomDaylight;
        this.startDelaySeconds = startDelaySeconds;
        this.countdownEnabled = countdownEnabled;
        this.countdownMinutes = countdownMinutes;
        this.rushEnabled = rushEnabled;
        this.rushSeconds = rushSeconds;
        this.allowLateJoin = allowLateJoin;
        this.pvpEnabled = pvpEnabled;
        this.adventureMode = adventureMode;
        this.prelitPortalsMode = prelitPortalsMode;
        this.randomPvp = randomPvp;
        this.registerMode = registerMode;
        this.randomRegister = randomRegister;
        this.teamSyncEnabled = teamSyncEnabled;
        this.teamChestEnabled = teamChestEnabled;
        this.shuffleMode = shuffleMode;
        this.starterKitMode = starterKitMode;
        this.hideGoalDetailsInChat = hideGoalDetailsInChat;
        this.minesEnabled = minesEnabled;
        this.mineAmount = mineAmount;
        this.mineTimeSeconds = mineTimeSeconds;
        this.powerSlotEnabled = powerSlotEnabled;
        this.powerSlotIntervalSeconds = powerSlotIntervalSeconds;
    }

    public static void encode(PacketOpenBingoController msg, FriendlyByteBuf buf) {
        FriendlyByteBufUtil.writeEnum(buf, msg.win);
        FriendlyByteBufUtil.writeEnum(buf, msg.composition);
        buf.writeInt(msg.questPercent);
        buf.writeBoolean(msg.categoryLogicEnabled);
        buf.writeBoolean(msg.rarityLogicEnabled);
        buf.writeBoolean(msg.itemColorVariantsSeparate);
        buf.writeBoolean(msg.casino);
        buf.writeInt(msg.casinoMode);
        buf.writeInt(msg.rerollsMode);
        buf.writeInt(msg.rerollsCount);
        buf.writeInt(msg.gunRounds);
        buf.writeInt(msg.hangmanRounds);
        buf.writeInt(msg.hangmanSeconds);
        buf.writeInt(msg.hangmanPenalty);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.cardDifficulty);
        buf.writeBoolean(msg.randomCardDifficulty);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.gameDifficulty);
        buf.writeBoolean(msg.randomGameDifficulty);
        buf.writeInt(msg.effectsInterval);
        buf.writeBoolean(msg.randomEffectsInterval);
        buf.writeBoolean(msg.rtpEnabled);
        buf.writeBoolean(msg.randomRtp);
        buf.writeBoolean(msg.hostileMobsEnabled);
        buf.writeBoolean(msg.randomHostileMobs);
        buf.writeBoolean(msg.hungerEnabled);
        buf.writeBoolean(msg.naturalRegenEnabled);
        buf.writeBoolean(msg.randomNaturalRegen);
        buf.writeBoolean(msg.randomHunger);
        buf.writeInt(msg.cardSize);
        buf.writeBoolean(msg.randomCardSize);
        buf.writeBoolean(msg.keepInventoryEnabled);
        buf.writeBoolean(msg.randomKeepInventory);
        buf.writeBoolean(msg.hardcoreEnabled);
        buf.writeBoolean(msg.randomHardcore);
        buf.writeInt(msg.daylightMode);
        buf.writeBoolean(msg.randomDaylight);
        buf.writeInt(msg.startDelaySeconds);
        buf.writeBoolean(msg.countdownEnabled);
        buf.writeInt(msg.countdownMinutes);
        buf.writeBoolean(msg.rushEnabled);
        buf.writeInt(msg.rushSeconds);
        buf.writeBoolean(msg.allowLateJoin);
        buf.writeBoolean(msg.pvpEnabled);
        buf.writeBoolean(msg.adventureMode);
        buf.writeInt(msg.prelitPortalsMode);
        buf.writeBoolean(msg.randomPvp);
        buf.writeInt(msg.registerMode);
        buf.writeBoolean(msg.randomRegister);
        buf.writeBoolean(msg.teamSyncEnabled);
        buf.writeBoolean(msg.teamChestEnabled);
        buf.writeInt(msg.shuffleMode);
        buf.writeInt(msg.starterKitMode);
        buf.writeBoolean(msg.hideGoalDetailsInChat);
        buf.writeBoolean(msg.minesEnabled);
        buf.writeInt(msg.mineAmount);
        buf.writeInt(msg.mineTimeSeconds);
        buf.writeBoolean(msg.powerSlotEnabled);
        buf.writeInt(msg.powerSlotIntervalSeconds);
    }

    public static PacketOpenBingoController decode(FriendlyByteBuf buf) {
        WinCondition win = FriendlyByteBufUtil.readEnum(buf, WinCondition.class);
        CardComposition composition = FriendlyByteBufUtil.readEnum(buf, CardComposition.class);
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
        int hangmanSeconds = buf.readInt();
        int hangmanPenalty = buf.readInt();
        String cardDifficulty = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
        boolean randomCardDifficulty = buf.readBoolean();
        String gameDifficulty = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
        boolean randomGameDifficulty = buf.readBoolean();
        int effectsInterval = buf.readInt();
        boolean randomEffectsInterval = buf.readBoolean();
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

        return new PacketOpenBingoController(
                win, composition, questPercent, categoryLogicEnabled, rarityLogicEnabled, itemColorVariantsSeparate,
                casino, casinoMode, rerollsMode, rerollsCount, gunRounds, hangmanRounds, hangmanSeconds, hangmanPenalty,
                cardDifficulty, randomCardDifficulty, gameDifficulty, randomGameDifficulty, effectsInterval, randomEffectsInterval,
                rtpEnabled, randomRtp, hostileMobsEnabled, randomHostileMobs, hungerEnabled, naturalRegenEnabled, randomNaturalRegen, randomHunger,
                cardSize, randomCardSize, keepInventoryEnabled, randomKeepInventory, hardcoreEnabled, randomHardcore,
                daylightMode, randomDaylight, startDelaySeconds, countdownEnabled, countdownMinutes, rushEnabled, rushSeconds,
                allowLateJoin, pvpEnabled, adventureMode, prelitPortalsMode, randomPvp, registerMode, randomRegister, teamSyncEnabled, teamChestEnabled, shuffleMode,
                starterKitMode, hideGoalDetailsInChat, minesEnabled, mineAmount, mineTimeSeconds, powerSlotEnabled, powerSlotIntervalSeconds
        );
    }

    public static void handle(PacketOpenBingoController msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> com.jamie.jamiebingo.client.ClientPacketHandlers.handleOpenBingoController(
                        msg.win,
                        msg.composition,
                        msg.questPercent,
                        msg.categoryLogicEnabled,
                        msg.rarityLogicEnabled,
                        msg.itemColorVariantsSeparate,
                        msg.casino,
                        msg.casinoMode,
                        msg.rerollsMode,
                        msg.rerollsCount,
                        msg.gunRounds,
                        msg.hangmanRounds,
                        msg.hangmanSeconds,
                        msg.hangmanPenalty,
                        msg.cardDifficulty,
                        msg.randomCardDifficulty,
                        msg.gameDifficulty,
                        msg.randomGameDifficulty,
                        msg.effectsInterval,
                        msg.randomEffectsInterval,
                        msg.rtpEnabled,
                        msg.randomRtp,
                        msg.hostileMobsEnabled,
                        msg.randomHostileMobs,
                        msg.hungerEnabled,
                        msg.naturalRegenEnabled,
                        msg.randomNaturalRegen,
                        msg.randomHunger,
                        msg.cardSize,
                        msg.randomCardSize,
                        msg.keepInventoryEnabled,
                        msg.randomKeepInventory,
                        msg.hardcoreEnabled,
                        msg.randomHardcore,
                        msg.daylightMode,
                        msg.randomDaylight,
                        msg.startDelaySeconds,
                        msg.countdownEnabled,
                        msg.countdownMinutes,
                        msg.rushEnabled,
                        msg.rushSeconds,
                        msg.allowLateJoin,
                        msg.pvpEnabled,
                        msg.adventureMode,
                        msg.prelitPortalsMode,
                        msg.randomPvp,
                        msg.registerMode,
                        msg.randomRegister,
                        msg.teamSyncEnabled,
                        msg.teamChestEnabled,
                        msg.shuffleMode,
                        msg.starterKitMode,
                        msg.hideGoalDetailsInChat,
                        msg.minesEnabled,
                        msg.mineAmount,
                        msg.mineTimeSeconds,
                        msg.powerSlotEnabled,
                        msg.powerSlotIntervalSeconds
                )
        ));
        ctx.setPacketHandled(true);
    }
}

