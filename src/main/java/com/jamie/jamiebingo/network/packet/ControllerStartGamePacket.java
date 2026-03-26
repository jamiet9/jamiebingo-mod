package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.bingo.CardComposition;
import com.jamie.jamiebingo.bingo.WinCondition;
import com.jamie.jamiebingo.casino.CasinoModeManager;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.menu.ControllerSettingsSnapshot;
import com.jamie.jamiebingo.menu.PlayerControllerSettingsStore;
import com.jamie.jamiebingo.util.FriendlyByteBufUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class ControllerStartGamePacket {

    private final WinCondition win;
    private final boolean randomWin;

    private final int questMode;
    private final int questPercent;
    private final boolean categoryLogicEnabled;
    private final boolean rarityLogicEnabled;
    private final boolean itemColorVariantsSeparate;

    private final boolean casino;
    private final int casinoMode;

    private final int rerollsMode; // 0 off, 1 fixed, 2 random (legacy)
    private final int rerollsCount;

    private final int gunRounds;
    private final int hangmanRounds;
    private final int hangmanBaseSeconds;
    private final int hangmanPenaltySeconds;

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
    private final boolean fakeRerollsEnabled;
    private final int fakeRerollsPerPlayer;

    public ControllerStartGamePacket(
            WinCondition win,
            boolean randomWin,
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
            int powerSlotIntervalSeconds,
            boolean fakeRerollsEnabled,
            int fakeRerollsPerPlayer
    ) {
        this.win = win;
        this.randomWin = randomWin;
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
        this.fakeRerollsEnabled = fakeRerollsEnabled;
        this.fakeRerollsPerPlayer = fakeRerollsPerPlayer;
    }

    public static void encode(ControllerStartGamePacket msg, FriendlyByteBuf buf) {
        FriendlyByteBufUtil.writeEnum(buf, msg.win);
        buf.writeBoolean(msg.randomWin);

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
        buf.writeBoolean(msg.fakeRerollsEnabled);
        buf.writeInt(msg.fakeRerollsPerPlayer);
    }

    public static ControllerStartGamePacket decode(FriendlyByteBuf buf) {
        WinCondition win = FriendlyByteBufUtil.readEnum(buf, WinCondition.class);
        boolean randomWin = buf.readBoolean();
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
        String cardDifficulty = FriendlyByteBufUtil.readString(buf, 32767);
        boolean randomCardDifficulty = buf.readBoolean();
        String gameDifficulty = FriendlyByteBufUtil.readString(buf, 32767);
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
        boolean fakeRerollsEnabled = buf.readBoolean();
        int fakeRerollsPerPlayer = buf.readInt();

        return new ControllerStartGamePacket(
                win,
                randomWin,
                questMode,
                questPercent,
                categoryLogicEnabled,
                rarityLogicEnabled,
                itemColorVariantsSeparate,
                casino,
                casinoMode,
                rerollsMode,
                rerollsCount,
                gunRounds,
                hangmanRounds,
                hangmanBaseSeconds,
                hangmanPenaltySeconds,
                cardDifficulty,
                randomCardDifficulty,
                gameDifficulty,
                randomGameDifficulty,
                effectsInterval,
                randomEffectsInterval,
                rtpEnabled,
                randomRtp,
                hostileMobsEnabled,
                randomHostileMobs,
                hungerEnabled,
                naturalRegenEnabled,
                randomNaturalRegen,
                randomHunger,
                cardSize,
                randomCardSize,
                keepInventoryEnabled,
                randomKeepInventory,
                hardcoreEnabled,
                randomHardcore,
                daylightMode,
                randomDaylight,
                startDelaySeconds,
                countdownEnabled,
                countdownMinutes,
                rushEnabled,
                rushSeconds,
                allowLateJoin,
                pvpEnabled,
                adventureMode,
                prelitPortalsMode,
                randomPvp,
                registerMode,
                randomRegister,
                teamSyncEnabled,
                teamChestEnabled,
                shuffleMode,
                starterKitMode,
                hideGoalDetailsInChat,
                minesEnabled,
                mineAmount,
                mineTimeSeconds,
                powerSlotEnabled,
                powerSlotIntervalSeconds,
                fakeRerollsEnabled,
                fakeRerollsPerPlayer
        );
    }

    public static void handle(ControllerStartGamePacket msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {

            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(sender);
            if (server == null) return;

            BingoGameData data = BingoGameData.get(server);
            if (data.worldRegenInProgress || data.worldRegenQueued) {
                sender.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(
                        "World regeneration " + com.jamie.jamiebingo.world.WorldRegenerationManager.regenStatusText(data) + ". Please wait."
                ));
                return;
            }
            PlayerControllerSettingsStore.save(sender, new ControllerSettingsSnapshot(
                    msg.win,
                    msg.questMode,
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
                    msg.hangmanBaseSeconds,
                    msg.hangmanPenaltySeconds,
                    msg.randomCardDifficulty ? "random" : msg.cardDifficulty,
                    msg.randomGameDifficulty ? "random" : msg.gameDifficulty,
                    msg.randomEffectsInterval ? -1 : msg.effectsInterval,
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
                    msg.powerSlotIntervalSeconds,
                    msg.fakeRerollsEnabled,
                    msg.fakeRerollsPerPlayer
            ));
            java.util.Random rng = new java.util.Random();

            /* ======================
               APPLY SETTINGS (FINAL VALUES)
               ====================== */

            // Mode is resolved server-side when random intent is present.
            data.winCondition = msg.win;
            data.randomWinConditionIntent = msg.randomWin;

            data.composition =
                    msg.questMode == 1 ? CardComposition.HYBRID_CATEGORY :
                    msg.questMode == 2 ? CardComposition.HYBRID_PERCENT :
                            CardComposition.CLASSIC_ONLY;

            data.questPercent = msg.questPercent;
            data.categoryLogicEnabled = msg.categoryLogicEnabled;
            data.rarityLogicEnabled = msg.rarityLogicEnabled;
            data.itemColorVariantsSeparate = msg.itemColorVariantsSeparate;
            data.size = Math.max(1, Math.min(10, msg.cardSize));
            data.randomSizeIntent = msg.randomCardSize;

            // Rerolls: in your new flow this should be fixed value or disabled.
            // Keep legacy compatibility anyway.
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

            // Card difficulty is already resolved client-side; store it.
            data.difficulty = msg.cardDifficulty;
            data.randomDifficultyIntent = false;

            if (msg.randomEffectsInterval) {
                data.randomEffectsIntervalIntent = true;
                data.randomEffectsArmed = false;
                data.randomEffectsIntervalSeconds = 0;
            } else {
                data.randomEffectsArmed = msg.effectsInterval > 0;
                data.randomEffectsIntervalIntent = false;
                data.randomEffectsIntervalSeconds = msg.effectsInterval;
            }

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
            if (msg.randomNaturalRegen) {
                data.naturalRegenEnabled = rng.nextDouble() < 0.90d;
            } else {
                data.naturalRegenEnabled = msg.naturalRegenEnabled;
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
            if (msg.randomRegister) {
                data.registerMode = rng.nextDouble() < 0.70d
                        ? BingoGameData.REGISTER_ALWAYS_HAVE
                        : BingoGameData.REGISTER_COLLECT_ONCE;
            } else {
                data.registerMode = msg.registerMode == BingoGameData.REGISTER_ALWAYS_HAVE
                        ? BingoGameData.REGISTER_ALWAYS_HAVE
                        : BingoGameData.REGISTER_COLLECT_ONCE;
            }
            data.teamSyncEnabled = msg.teamSyncEnabled;
            data.teamChestEnabled = msg.teamChestEnabled;
            data.shuffleEnabled = msg.shuffleMode == BingoGameData.SHUFFLE_ENABLED;
            data.randomShuffleIntent = msg.shuffleMode == BingoGameData.SHUFFLE_RANDOM;
            data.starterKitMode = Math.max(BingoGameData.STARTER_KIT_DISABLED, Math.min(BingoGameData.STARTER_KIT_OP, msg.starterKitMode));
            data.hideGoalDetailsInChat = msg.hideGoalDetailsInChat;

            // Vanilla game difficulty is resolved client-side; apply it.
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
            data.powerSlotEnabled = msg.powerSlotEnabled;
            data.powerSlotIntervalSeconds = Math.max(10, Math.min(300, msg.powerSlotIntervalSeconds));
            data.fakeRerollsEnabled = msg.fakeRerollsEnabled;
            data.fakeRerollsPerPlayer = Math.max(1, Math.min(10, msg.fakeRerollsPerPlayer));

            /* ======================
               START GAME
               ====================== */

            if (data.startGame(server)) {
                CasinoModeManager.startPregamePhasesOrFinalize(server, data);
            }

            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        });

        ctx.setPacketHandled(true);
    }
}



