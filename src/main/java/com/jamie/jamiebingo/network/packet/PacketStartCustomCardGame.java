package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.bingo.WinCondition;
import com.jamie.jamiebingo.casino.CasinoModeManager;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.util.FriendlyByteBufUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class PacketStartCustomCardGame {

    private final WinCondition win;
    private final int questMode;
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
    private final int hangmanBaseSeconds;
    private final int hangmanPenaltySeconds;
    private final String cardDifficulty;
    private final String gameDifficulty;
    private final int effectsInterval;
    private final boolean rtpEnabled;
    private final boolean randomRtp;
    private final boolean hostileMobsEnabled;
    private final boolean randomHostileMobs;
    private final boolean hungerEnabled;
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
    private final boolean randomPvp;
    private final int registerMode;
    private final boolean teamSyncEnabled;
    private final int shuffleMode;
    private final int starterKitMode;
    private final boolean hideGoalDetailsInChat;
    private final boolean minesEnabled;
    private final int mineAmount;
    private final int mineTimeSeconds;
    private final boolean customCardEnabled;
    private final boolean customPoolEnabled;
    private final List<String> customCardSlots;
    private final List<String> customPoolIds;
    private final List<String> customMineIds;

    public PacketStartCustomCardGame(
            WinCondition win,
            int questMode,
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
            int hangmanBaseSeconds,
            int hangmanPenaltySeconds,
            String cardDifficulty,
            String gameDifficulty,
            int effectsInterval,
            boolean rtpEnabled,
            boolean randomRtp,
            boolean hostileMobsEnabled,
            boolean randomHostileMobs,
            boolean hungerEnabled,
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
            boolean randomPvp,
            int registerMode,
            boolean teamSyncEnabled,
            int shuffleMode,
            int starterKitMode,
            boolean hideGoalDetailsInChat,
            boolean minesEnabled,
            int mineAmount,
            int mineTimeSeconds,
            boolean customCardEnabled,
            boolean customPoolEnabled,
            List<String> customCardSlots,
            List<String> customPoolIds,
            List<String> customMineIds
    ) {
        this.win = win;
        this.questMode = questMode;
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
        this.hangmanBaseSeconds = hangmanBaseSeconds;
        this.hangmanPenaltySeconds = hangmanPenaltySeconds;
        this.cardDifficulty = cardDifficulty;
        this.gameDifficulty = gameDifficulty;
        this.effectsInterval = effectsInterval;
        this.rtpEnabled = rtpEnabled;
        this.randomRtp = randomRtp;
        this.hostileMobsEnabled = hostileMobsEnabled;
        this.randomHostileMobs = randomHostileMobs;
        this.hungerEnabled = hungerEnabled;
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
        this.randomPvp = randomPvp;
        this.registerMode = registerMode;
        this.teamSyncEnabled = teamSyncEnabled;
        this.shuffleMode = shuffleMode;
        this.starterKitMode = starterKitMode;
        this.hideGoalDetailsInChat = hideGoalDetailsInChat;
        this.minesEnabled = minesEnabled;
        this.mineAmount = mineAmount;
        this.mineTimeSeconds = mineTimeSeconds;
        this.customCardEnabled = customCardEnabled;
        this.customPoolEnabled = customPoolEnabled;
        this.customCardSlots = customCardSlots == null ? List.of() : new ArrayList<>(customCardSlots);
        this.customPoolIds = customPoolIds == null ? List.of() : new ArrayList<>(customPoolIds);
        this.customMineIds = customMineIds == null ? List.of() : new ArrayList<>(customMineIds);
    }

    public static void encode(PacketStartCustomCardGame msg, FriendlyByteBuf buf) {
        FriendlyByteBufUtil.writeEnum(buf, msg.win);
        buf.writeInt(msg.questMode);
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
        buf.writeInt(msg.hangmanBaseSeconds);
        buf.writeInt(msg.hangmanPenaltySeconds);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.cardDifficulty);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.gameDifficulty);
        buf.writeInt(msg.effectsInterval);
        buf.writeBoolean(msg.rtpEnabled);
        buf.writeBoolean(msg.randomRtp);
        buf.writeBoolean(msg.hostileMobsEnabled);
        buf.writeBoolean(msg.randomHostileMobs);
        buf.writeBoolean(msg.hungerEnabled);
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
        buf.writeBoolean(msg.randomPvp);
        buf.writeInt(msg.registerMode);
        buf.writeBoolean(msg.teamSyncEnabled);
        buf.writeInt(msg.shuffleMode);
        buf.writeInt(msg.starterKitMode);
        buf.writeBoolean(msg.hideGoalDetailsInChat);
        buf.writeBoolean(msg.minesEnabled);
        buf.writeInt(msg.mineAmount);
        buf.writeInt(msg.mineTimeSeconds);
        buf.writeBoolean(msg.customCardEnabled);
        buf.writeBoolean(msg.customPoolEnabled);
        buf.writeInt(msg.customCardSlots.size());
        for (String id : msg.customCardSlots) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, id == null ? "" : id);
        }
        buf.writeInt(msg.customPoolIds.size());
        for (String id : msg.customPoolIds) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, id == null ? "" : id);
        }
        buf.writeInt(msg.customMineIds.size());
        for (String id : msg.customMineIds) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, id == null ? "" : id);
        }
    }

    public static PacketStartCustomCardGame decode(FriendlyByteBuf buf) {
        WinCondition win = FriendlyByteBufUtil.readEnum(buf, WinCondition.class);
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
        boolean randomPvp = buf.readBoolean();
        int registerMode = buf.readInt();
        boolean teamSyncEnabled = buf.readBoolean();
        int shuffleMode = buf.readInt();
        int starterKitMode = buf.readInt();
        boolean hideGoalDetailsInChat = buf.readBoolean();
        boolean minesEnabled = buf.readBoolean();
        int mineAmount = buf.readInt();
        int mineTimeSeconds = buf.readInt();
        boolean customCardEnabled = buf.readBoolean();
        boolean customPoolEnabled = buf.readBoolean();
        int cardSlotsSize = buf.readInt();
        List<String> cardSlots = new ArrayList<>();
        for (int i = 0; i < cardSlotsSize; i++) {
            cardSlots.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
        }
        int poolSize = buf.readInt();
        List<String> poolIds = new ArrayList<>();
        for (int i = 0; i < poolSize; i++) {
            poolIds.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
        }
        int mineSize = buf.readInt();
        List<String> mineIds = new ArrayList<>();
        for (int i = 0; i < mineSize; i++) {
            mineIds.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
        }
        return new PacketStartCustomCardGame(
                win, questMode, questPercent, categoryLogicEnabled, rarityLogicEnabled, itemColorVariantsSeparate,
                casino, casinoMode, rerollsMode, rerollsCount, gunRounds,
                hangmanRounds, hangmanBaseSeconds, hangmanPenaltySeconds, cardDifficulty, gameDifficulty,
                effectsInterval, rtpEnabled, randomRtp, hostileMobsEnabled, randomHostileMobs,
                hungerEnabled, randomHunger, cardSize, randomCardSize, keepInventoryEnabled,
                randomKeepInventory, hardcoreEnabled, randomHardcore, daylightMode, randomDaylight,
                startDelaySeconds, countdownEnabled, countdownMinutes, rushEnabled, rushSeconds, allowLateJoin, pvpEnabled, adventureMode,
                randomPvp, registerMode, teamSyncEnabled, shuffleMode, starterKitMode, hideGoalDetailsInChat, minesEnabled, mineAmount, mineTimeSeconds, customCardEnabled, customPoolEnabled, cardSlots, poolIds, mineIds
        );
    }

    public static void handle(PacketStartCustomCardGame msg, CustomPayloadEvent.Context ctx) {
        CustomPayloadEvent.Context context = ctx;
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;
            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(sender);
            if (server == null) return;

            BingoGameData data = BingoGameData.get(server);
            Random rng = new Random();

            data.winCondition = msg.win;
            data.randomWinConditionIntent = false;

            data.composition =
                    msg.questMode == 1 ? com.jamie.jamiebingo.bingo.CardComposition.HYBRID_CATEGORY :
                    msg.questMode == 2 ? com.jamie.jamiebingo.bingo.CardComposition.HYBRID_PERCENT :
                            com.jamie.jamiebingo.bingo.CardComposition.CLASSIC_ONLY;

            data.questPercent = msg.questPercent;
            data.categoryLogicEnabled = msg.categoryLogicEnabled;
            data.rarityLogicEnabled = msg.rarityLogicEnabled;
            data.itemColorVariantsSeparate = msg.itemColorVariantsSeparate;
            data.size = Math.max(1, Math.min(10, msg.cardSize));
            data.randomSizeIntent = msg.randomCardSize;

            if (msg.rerollsMode == 1) {
                data.rerollsPerPlayer = msg.rerollsCount;
                data.randomRerollsIntent = false;
            } else if (msg.rerollsMode == 2) {
                data.rerollsPerPlayer = 0;
                data.randomRerollsIntent = true;
            } else {
                data.rerollsPerPlayer = 0;
                data.randomRerollsIntent = false;
            }

            data.gunGameLength = msg.gunRounds;
            data.hangmanRounds = msg.hangmanRounds;
            data.hangmanBaseSeconds = Math.max(10, msg.hangmanBaseSeconds);
            data.hangmanPenaltySeconds = Math.max(0, msg.hangmanPenaltySeconds);

            data.difficulty = msg.cardDifficulty;
            data.randomDifficultyIntent = false;

            data.randomEffectsArmed = msg.effectsInterval > 0;
            data.randomEffectsIntervalIntent = false;
            data.randomEffectsIntervalSeconds = msg.effectsInterval;

            if (msg.randomRtp) {
                data.rtpEnabled = rng.nextBoolean();
            } else {
                data.rtpEnabled = msg.rtpEnabled;
            }
            data.randomRtpIntent = false;

            if (msg.randomPvp) {
                data.randomPvpIntent = true;
            } else {
                data.pvpEnabled = msg.pvpEnabled;
                data.randomPvpIntent = false;
            }
            data.adventureMode = msg.adventureMode;

            if (msg.randomHostileMobs) {
                data.hostileMobsEnabled = rng.nextBoolean();
            } else {
                data.hostileMobsEnabled = msg.hostileMobsEnabled;
            }
            data.randomHostileMobsIntent = false;

            if (msg.randomHunger) {
                data.hungerEnabled = rng.nextBoolean();
            } else {
                data.hungerEnabled = msg.hungerEnabled;
            }
            data.randomHungerIntent = false;

            if (msg.randomKeepInventory) {
                data.randomKeepInventoryIntent = true;
            } else {
                data.keepInventoryEnabled = msg.keepInventoryEnabled;
                data.randomKeepInventoryIntent = false;
            }

            if (msg.randomHardcore) {
                data.randomHardcoreIntent = true;
            } else {
                data.hardcoreEnabled = msg.hardcoreEnabled;
                data.randomHardcoreIntent = false;
            }

            if (msg.randomDaylight) {
                data.randomDaylightIntent = true;
            } else {
                int mode = msg.daylightMode;
                if (mode < BingoGameData.DAYLIGHT_ENABLED || mode > BingoGameData.DAYLIGHT_DUSK) {
                    mode = BingoGameData.DAYLIGHT_ENABLED;
                }
                data.daylightMode = mode;
                data.randomDaylightIntent = false;
            }

            data.bingoStartDelaySeconds = Math.max(0, msg.startDelaySeconds);
            data.countdownEnabled = msg.countdownEnabled;
            data.countdownMinutes = msg.countdownEnabled
                    ? Math.max(10, msg.countdownMinutes)
                    : 0;
            data.rushEnabled = msg.rushEnabled;
            data.rushSeconds = Math.max(1, Math.min(300, msg.rushSeconds));
            data.allowLateJoin = msg.allowLateJoin;
            data.registerMode = msg.registerMode == BingoGameData.REGISTER_ALWAYS_HAVE
                    ? BingoGameData.REGISTER_ALWAYS_HAVE
                    : BingoGameData.REGISTER_COLLECT_ONCE;
            data.teamSyncEnabled = msg.teamSyncEnabled;
            data.shuffleEnabled = msg.shuffleMode == BingoGameData.SHUFFLE_ENABLED;
            data.randomShuffleIntent = msg.shuffleMode == BingoGameData.SHUFFLE_RANDOM;
            data.starterKitMode = Math.max(BingoGameData.STARTER_KIT_DISABLED, Math.min(BingoGameData.STARTER_KIT_OP, msg.starterKitMode));
            data.hideGoalDetailsInChat = msg.hideGoalDetailsInChat;

            Difficulty diff = switch (msg.gameDifficulty) {
                case "easy" -> Difficulty.EASY;
                case "hard" -> Difficulty.HARD;
                default -> Difficulty.NORMAL;
            };
            server.setDifficulty(diff, true);

            boolean casinoAllowed = BingoGameData.isCasinoAllowedForWin(data.winCondition);
            data.casinoMode = casinoAllowed
                    ? Math.max(BingoGameData.CASINO_DISABLED, Math.min(BingoGameData.CASINO_DRAFT, msg.casinoMode))
                    : BingoGameData.CASINO_DISABLED;
            if (!msg.win.equals(com.jamie.jamiebingo.bingo.WinCondition.FULL)
                    && !msg.win.equals(com.jamie.jamiebingo.bingo.WinCondition.LOCKOUT)
                    && !msg.win.equals(com.jamie.jamiebingo.bingo.WinCondition.RARITY)) {
                data.shuffleEnabled = false;
                data.randomShuffleIntent = false;
            }
            CasinoModeManager.setCasinoEnabled(casinoAllowed && data.casinoMode == BingoGameData.CASINO_ENABLED);
            data.minesEnabled = msg.minesEnabled;
            data.mineAmount = Math.max(1, Math.min(13, msg.mineAmount));
            data.mineTimeSeconds = Math.max(1, msg.mineTimeSeconds);

            data.customCardEnabled = msg.customCardEnabled;
            data.customPoolEnabled = msg.customPoolEnabled;
            data.customCardSlots.clear();
            data.customCardSlots.addAll(msg.customCardSlots);
            data.customPoolIds.clear();
            data.customPoolIds.addAll(msg.customPoolIds);
            data.customMineIds.clear();
            data.customMineIds.addAll(msg.customMineIds);

            if (data.startGame(server)) {
                CasinoModeManager.startPregamePhasesOrFinalize(server, data);
            }

            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        });
        context.setPacketHandled(true);
    }
}



