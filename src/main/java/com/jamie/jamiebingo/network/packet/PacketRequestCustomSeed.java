package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.bingo.CardSeedCodec;
import com.jamie.jamiebingo.bingo.WinCondition;
import com.jamie.jamiebingo.casino.CasinoModeManager;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.util.FriendlyByteBufUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class PacketRequestCustomSeed {

    private final WinCondition win;
    private final int questMode;
    private final int questPercent;
    private final boolean categoryLogicEnabled;
    private final boolean rarityLogicEnabled;
    private final boolean itemColorVariantsSeparate;
    private final boolean casino;
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
    private final boolean customCardEnabled;
    private final boolean customPoolEnabled;
    private final List<String> customCardSlots;
    private final List<String> customPoolIds;
    private final List<String> customMineIds;

    public PacketRequestCustomSeed(
            WinCondition win,
            int questMode,
            int questPercent,
            boolean categoryLogicEnabled,
            boolean rarityLogicEnabled,
            boolean itemColorVariantsSeparate,
            boolean casino,
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
        this.customCardEnabled = customCardEnabled;
        this.customPoolEnabled = customPoolEnabled;
        this.customCardSlots = customCardSlots == null ? List.of() : new ArrayList<>(customCardSlots);
        this.customPoolIds = customPoolIds == null ? List.of() : new ArrayList<>(customPoolIds);
        this.customMineIds = customMineIds == null ? List.of() : new ArrayList<>(customMineIds);
    }

    public static void encode(PacketRequestCustomSeed msg, FriendlyByteBuf buf) {
        FriendlyByteBufUtil.writeEnum(buf, msg.win);
        buf.writeInt(msg.questMode);
        buf.writeInt(msg.questPercent);
        buf.writeBoolean(msg.categoryLogicEnabled);
        buf.writeBoolean(msg.rarityLogicEnabled);
        buf.writeBoolean(msg.itemColorVariantsSeparate);
        buf.writeBoolean(msg.casino);
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

    public static PacketRequestCustomSeed decode(FriendlyByteBuf buf) {
        WinCondition win = FriendlyByteBufUtil.readEnum(buf, WinCondition.class);
        int questMode = buf.readInt();
        int questPercent = buf.readInt();
        boolean categoryLogicEnabled = buf.readBoolean();
        boolean rarityLogicEnabled = buf.readBoolean();
        boolean itemColorVariantsSeparate = buf.readBoolean();
        boolean casino = buf.readBoolean();
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
        return new PacketRequestCustomSeed(
                win, questMode, questPercent, categoryLogicEnabled, rarityLogicEnabled, itemColorVariantsSeparate,
                casino, rerollsMode, rerollsCount, gunRounds,
                hangmanRounds, hangmanBaseSeconds, hangmanPenaltySeconds, cardDifficulty, gameDifficulty,
                effectsInterval, rtpEnabled, randomRtp, hostileMobsEnabled, randomHostileMobs,
                hungerEnabled, randomHunger, cardSize, randomCardSize, keepInventoryEnabled,
                randomKeepInventory, hardcoreEnabled, randomHardcore, daylightMode, randomDaylight,
                startDelaySeconds, countdownEnabled, countdownMinutes, rushEnabled, rushSeconds, allowLateJoin, pvpEnabled, adventureMode,
                randomPvp, registerMode, teamSyncEnabled, shuffleMode, starterKitMode, hideGoalDetailsInChat, customCardEnabled, customPoolEnabled, cardSlots, poolIds, mineIds
        );
    }

    public static void handle(PacketRequestCustomSeed msg, CustomPayloadEvent.Context ctx) {
        CustomPayloadEvent.Context context = ctx;
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;
            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(sender);
            if (server == null) return;

            BingoGameData temp = new BingoGameData();
            Random rng = new Random();

            temp.winCondition = msg.win;
            temp.composition =
                    msg.questMode == 1 ? com.jamie.jamiebingo.bingo.CardComposition.HYBRID_CATEGORY :
                    msg.questMode == 2 ? com.jamie.jamiebingo.bingo.CardComposition.HYBRID_PERCENT :
                            com.jamie.jamiebingo.bingo.CardComposition.CLASSIC_ONLY;

            temp.questPercent = msg.questPercent;
            temp.categoryLogicEnabled = msg.categoryLogicEnabled;
            temp.rarityLogicEnabled = msg.rarityLogicEnabled;
            temp.itemColorVariantsSeparate = msg.itemColorVariantsSeparate;
            temp.size = Math.max(1, Math.min(10, msg.cardSize));
            temp.randomSizeIntent = msg.randomCardSize;
            temp.rerollsPerPlayer = msg.rerollsMode == 1 ? msg.rerollsCount : 0;
            temp.gunGameLength = msg.gunRounds;
            temp.hangmanRounds = msg.hangmanRounds;
            temp.hangmanBaseSeconds = Math.max(10, msg.hangmanBaseSeconds);
            temp.hangmanPenaltySeconds = Math.max(0, msg.hangmanPenaltySeconds);
            temp.difficulty = msg.cardDifficulty;
            temp.randomEffectsArmed = msg.effectsInterval > 0;
            temp.randomEffectsIntervalSeconds = msg.effectsInterval;
            temp.rtpEnabled = msg.randomRtp ? rng.nextBoolean() : msg.rtpEnabled;
            temp.hostileMobsEnabled = msg.randomHostileMobs ? rng.nextBoolean() : msg.hostileMobsEnabled;
            temp.hungerEnabled = msg.randomHunger ? rng.nextBoolean() : msg.hungerEnabled;
            temp.keepInventoryEnabled = msg.randomKeepInventory ? rng.nextBoolean() : msg.keepInventoryEnabled;
            temp.hardcoreEnabled = msg.randomHardcore ? rng.nextBoolean() : msg.hardcoreEnabled;
            temp.daylightMode = msg.randomDaylight ? BingoGameData.DAYLIGHT_ENABLED : msg.daylightMode;
            temp.bingoStartDelaySeconds = Math.max(0, msg.startDelaySeconds);
            temp.countdownEnabled = msg.countdownEnabled;
            temp.countdownMinutes = msg.countdownEnabled ? Math.max(10, msg.countdownMinutes) : 0;
            temp.rushEnabled = msg.rushEnabled;
            temp.rushSeconds = Math.max(1, Math.min(300, msg.rushSeconds));
            temp.allowLateJoin = msg.allowLateJoin;
            temp.pvpEnabled = msg.randomPvp ? rng.nextBoolean() : msg.pvpEnabled;
            temp.adventureMode = msg.adventureMode;
            temp.registerMode = msg.registerMode;
            temp.teamSyncEnabled = msg.teamSyncEnabled;
            temp.shuffleEnabled = msg.shuffleMode == BingoGameData.SHUFFLE_ENABLED;
            temp.randomShuffleIntent = msg.shuffleMode == BingoGameData.SHUFFLE_RANDOM;
            temp.starterKitMode = Math.max(BingoGameData.STARTER_KIT_DISABLED, Math.min(BingoGameData.STARTER_KIT_OP, msg.starterKitMode));
            temp.hideGoalDetailsInChat = msg.hideGoalDetailsInChat;

            Difficulty diff = switch (msg.gameDifficulty) {
                case "easy" -> Difficulty.EASY;
                case "hard" -> Difficulty.HARD;
                default -> Difficulty.NORMAL;
            };
            server.setDifficulty(diff, true);

            CasinoModeManager.setCasinoEnabled(msg.casino);

            temp.customCardEnabled = msg.customCardEnabled;
            temp.customPoolEnabled = msg.customPoolEnabled;
            temp.customCardSlots.addAll(msg.customCardSlots);
            temp.customPoolIds.addAll(msg.customPoolIds);
            temp.customMineIds.addAll(msg.customMineIds);

            if (temp.customPoolEnabled) {
                temp.currentCard = temp.buildCardFromPool(temp.size, rng);
            } else if (temp.customCardEnabled) {
                temp.currentCard = temp.buildCustomCardFromSlots();
            }

            if (temp.winCondition == WinCondition.HANGMAN) {
                if (temp.currentCard != null) {
                    temp.hangmanCards.add(temp.currentCard);
                }
            } else if (temp.winCondition == WinCondition.GUNGAME || temp.winCondition == WinCondition.GAMEGUN) {
                if (temp.currentCard != null) {
                    temp.setGunGameCardsFromSeed(java.util.List.of(temp.currentCard));
                }
            }

            String seed = CardSeedCodec.encode(temp, server);
            if (seed == null || seed.isBlank()) {
                sender.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Failed to build seed."));
                return;
            }

            MutableComponent seedComponent = com.jamie.jamiebingo.util.ComponentUtil.literal(seed)
                    .withStyle(style -> style
                            .withClickEvent(new ClickEvent.CopyToClipboard(seed))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    com.jamie.jamiebingo.util.ComponentUtil.literal("Copy to clipboard")))
                    );

            sender.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Custom card seed: ").append(seedComponent));
        });
        context.setPacketHandled(true);
    }
}


