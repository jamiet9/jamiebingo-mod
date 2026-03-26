package com.jamie.jamiebingo.client.screen;

import com.jamie.jamiebingo.client.ClientControllerSettingsStore;
import com.jamie.jamiebingo.menu.ControllerSettingsSnapshot;
import com.jamie.jamiebingo.bingo.CardComposition;
import com.jamie.jamiebingo.bingo.SettingsSeedCodec;
import com.jamie.jamiebingo.bingo.WinCondition;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.ControllerStartGamePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import java.lang.reflect.Method;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Consumer;
import java.util.Random;

public class BingoControllerScreen extends Screen {

    /* =========================
       INTERNAL ENUMS
       ========================= */

    private enum RerollMode {
        DISABLED,
        FIXED,
        RANDOM
    }

    private enum EffectsMode {
        DISABLED,
        FIXED,
        RANDOM
    }

    private enum CasinoMode {
        DISABLED,
        ENABLED,
        DRAFT
    }

    private enum ToggleMode {
        DISABLED,
        ENABLED,
        RANDOM
    }

    private enum PrelitPortalsMode {
        OFF,
        NETHER,
        END,
        BOTH;

        public PrelitPortalsMode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public PrelitPortalsMode previous() {
            int idx = ordinal() - 1;
            if (idx < 0) idx = values().length - 1;
            return values()[idx];
        }
    }

    private enum ShuffleMode {
        DISABLED,
        ENABLED,
        RANDOM
    }

    private enum StarterKitMode {
        DISABLED,
        MINIMAL,
        AVERAGE,
        OP
    }

    private enum DaylightMode {
        ENABLED,
        DAY,
        NIGHT,
        MIDNIGHT,
        DAWN,
        DUSK,
        RANDOM;

        public DaylightMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private enum RegisterMode {
        COLLECT_ONCE,
        ALWAYS_HAVE,
        RANDOM;

        public RegisterMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private enum DifficultyMode {
        EASY,
        NORMAL,
        HARD,
        EXTREME,
        CUSTOM,
        RANDOM;

        public DifficultyMode nextCard() {
            return values()[(ordinal() + 1) % values().length];
        }

        public DifficultyMode nextGame() {
            return switch (this) {
                case EASY -> NORMAL;
                case NORMAL -> HARD;
                case HARD -> RANDOM;
                case EXTREME -> EASY;
                case CUSTOM -> EASY;
                case RANDOM -> EASY;
            };
        }
    }

    /* =========================
       INITIAL STATE (FROM SERVER)
       ========================= */

    private static WinCondition initialWin;
    private static CardComposition initialComposition;
    private static int initialQuestPercent;
    private static boolean initialCategoryLogicEnabled = true;
    private static boolean initialRarityLogicEnabled = true;
    private static boolean initialItemColorVariantsSeparate = true;
    private static boolean initialCasino;
    private static int initialCasinoMode;
    private static int initialRerollsMode;
    private static int initialRerollsCount;
    private static int initialGunRounds;
    private static int initialHangmanRounds;
    private static int initialHangmanSeconds;
    private static int initialHangmanPenalty;

    private static String initialCardDifficulty;
    private static String initialGameDifficulty;

    private static int initialEffectsInterval;
    private static boolean initialEffectsRandom;
    private static int initialEffectsMode;

    private static boolean initialRtpEnabled;
    private static boolean initialRtpRandom;

    private static boolean initialHostileMobsEnabled;
    private static boolean initialHostileMobsRandom;

    private static boolean initialHungerEnabled;
    private static boolean initialHungerRandom;
    private static boolean initialNaturalRegenEnabled = true;
    private static boolean initialNaturalRegenRandom;

    private static int initialCardSize;
    private static boolean initialRandomCardSize;

    private static boolean initialKeepInventoryEnabled;
    private static boolean initialKeepInventoryRandom;

    private static boolean initialHardcoreEnabled;
    private static boolean initialHardcoreRandom;

    private static int initialDaylightMode;
    private static boolean initialDaylightRandom;

    private static int initialStartDelaySeconds;
    private static boolean initialCountdownEnabled;
    private static int initialCountdownMinutes;
    private static boolean initialRushEnabled;
    private static boolean initialRushRandom;
    private static int initialRushSeconds;
    private static boolean initialAllowLateJoin;

    private static boolean initialPvpEnabled;
    private static boolean initialAdventureMode;
    private static int initialPrelitPortalsMode;
    private static boolean initialPvpRandom;

    private static int initialRegisterMode;
    private static boolean initialRegisterRandom;
    private static boolean initialTeamSyncEnabled;
    private static boolean initialTeamChestEnabled;
    private static int initialShuffleMode;
    private static int initialStarterKitMode;
    private static boolean initialHideGoalDetailsInChat;
    private static boolean initialMinesEnabled;
    private static boolean initialMinesRandom;
    private static int initialMineAmount;
    private static int initialMineTimeSeconds;
    private static boolean initialPowerSlotEnabled;
    private static boolean initialPowerSlotRandom;
    private static int initialPowerSlotIntervalSeconds;
    private static boolean initialFakeRerollsEnabled;
    private static int initialFakeRerollsPerPlayer = 2;
    private static boolean initialRandomMode;
    private static boolean initialRandomGunRounds;
    private static boolean initialHangmanTimeRandom;
    private static boolean cachedState = false;
    private static boolean pendingSettingsOnly = false;
    private static Consumer<ControllerSettingsSnapshot> pendingSettingsListener = null;

    private static final WinCondition DEFAULT_MODE = WinCondition.FULL;
    private static final CardComposition DEFAULT_QUESTS = CardComposition.HYBRID_CATEGORY;
    private static final boolean DEFAULT_CATEGORY_LOGIC = false;
    private static final boolean DEFAULT_ITEM_COLORS_SEPARATE = false;
    private static final boolean DEFAULT_TEAM_SYNC = true;
    private static final boolean DEFAULT_TEAM_CHEST = true;
    private static final boolean DEFAULT_ALLOW_LATE_JOIN = true;
    private static final boolean DEFAULT_FAKE_REROLLS_ENABLED = false;
    private static final int DEFAULT_FAKE_REROLLS_PER_PLAYER = 2;

    private final boolean settingsOnly;
    private final Consumer<ControllerSettingsSnapshot> settingsListener;

    public static void openWithState(
            WinCondition win,
            CardComposition composition,
            int questPercent,
            boolean categoryLogicEnabled,
            boolean rarityLogicEnabled,
            boolean itemColorVariantsSeparate,
            boolean casino,
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
            boolean teamChestEnabled
    ) {
        openWithState(
                win,
                composition,
                questPercent,
                categoryLogicEnabled,
                rarityLogicEnabled,
                itemColorVariantsSeparate,
                casino,
                casino ? 1 : 0,
                rerollsMode,
                rerollsCount,
                gunRounds,
                hangmanRounds,
                hangmanSeconds,
                hangmanPenalty,
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
                com.jamie.jamiebingo.data.BingoGameData.SHUFFLE_DISABLED,
                com.jamie.jamiebingo.data.BingoGameData.STARTER_KIT_DISABLED,
                false,
                false,
                1,
                15,
                false,
                60
        );
    }

    public static void openWithState(
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
        ControllerSettingsSnapshot saved = ClientControllerSettingsStore.load();
        if (saved != null) {
            applyInitialStateFromSnapshot(saved);
        } else {
            applyInitialState(
                    win,
                    composition,
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
                    hangmanSeconds,
                    hangmanPenalty,
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
                    powerSlotIntervalSeconds
            );
        }
        pendingSettingsOnly = false;
        pendingSettingsListener = null;
    }

    public static void openWithStateForced(
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
        applyInitialState(
                win,
                composition,
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
                hangmanSeconds,
                hangmanPenalty,
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
                powerSlotIntervalSeconds
        );
        pendingSettingsOnly = false;
        pendingSettingsListener = null;
    }

    private static void applyInitialState(
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
        initialWin = win == null ? WinCondition.LINE : win;
        initialComposition = composition;
        initialQuestPercent = questPercent;
        initialCategoryLogicEnabled = categoryLogicEnabled;
        initialRarityLogicEnabled = rarityLogicEnabled;
        initialItemColorVariantsSeparate = itemColorVariantsSeparate;
        initialCasino = casino;
        initialCasinoMode = casinoMode;
        initialRerollsMode = rerollsMode;
        initialRerollsCount = rerollsCount;
        initialGunRounds = gunRounds;
        initialHangmanRounds = hangmanRounds;
        initialHangmanSeconds = hangmanSeconds;
        initialHangmanPenalty = hangmanPenalty;
        initialHangmanTimeRandom = hangmanSeconds <= 0;

        initialCardDifficulty = randomCardDifficulty ? "random" : cardDifficulty;
        initialGameDifficulty = randomGameDifficulty ? "random" : gameDifficulty;

        initialEffectsInterval = effectsInterval > 0 ? effectsInterval : 60;
        initialEffectsRandom = randomEffectsInterval;
        initialEffectsMode = randomEffectsInterval ? 2 : (effectsInterval > 0 ? 1 : 0);

        initialRtpEnabled = rtpEnabled;
        initialRtpRandom = randomRtp;

        initialHostileMobsEnabled = hostileMobsEnabled;
        initialHostileMobsRandom = randomHostileMobs;

        initialHungerEnabled = hungerEnabled;
        initialNaturalRegenEnabled = naturalRegenEnabled;
        initialNaturalRegenRandom = randomNaturalRegen;
        initialHungerRandom = randomHunger;

        initialCardSize = cardSize;
        initialRandomCardSize = randomCardSize;

        initialKeepInventoryEnabled = keepInventoryEnabled;
        initialKeepInventoryRandom = randomKeepInventory;

        initialHardcoreEnabled = hardcoreEnabled;
        initialHardcoreRandom = randomHardcore;

        initialDaylightMode = daylightMode;
        initialDaylightRandom = randomDaylight;

        initialStartDelaySeconds = startDelaySeconds;
        initialCountdownEnabled = countdownEnabled;
        initialCountdownMinutes = countdownMinutes;
        initialRushEnabled = rushEnabled;
        initialRushRandom = false;
        initialRushSeconds = rushSeconds;
        initialAllowLateJoin = allowLateJoin;

        initialPvpEnabled = pvpEnabled;
        initialAdventureMode = adventureMode;
        initialPrelitPortalsMode = clampPrelitPortalsMode(prelitPortalsMode);
        initialPvpRandom = randomPvp;

        initialRegisterMode = registerMode;
        initialRegisterRandom = randomRegister;
        initialTeamSyncEnabled = teamSyncEnabled;
        initialTeamChestEnabled = teamChestEnabled;
        initialShuffleMode = shuffleMode;
        initialStarterKitMode = starterKitMode;
        initialHideGoalDetailsInChat = hideGoalDetailsInChat;
        initialMinesRandom = !minesEnabled && mineAmount <= 0;
        initialMinesEnabled = !initialMinesRandom && minesEnabled;
        initialMineAmount = initialMinesRandom ? 1 : mineAmount;
        initialMineTimeSeconds = mineTimeSeconds;
        initialPowerSlotRandom = !powerSlotEnabled && powerSlotIntervalSeconds <= 0;
        initialPowerSlotEnabled = !initialPowerSlotRandom && powerSlotEnabled;
        initialPowerSlotIntervalSeconds = initialPowerSlotRandom ? 60 : powerSlotIntervalSeconds;
        initialFakeRerollsEnabled = DEFAULT_FAKE_REROLLS_ENABLED;
        initialFakeRerollsPerPlayer = DEFAULT_FAKE_REROLLS_PER_PLAYER;
        initialRandomMode = initialWin == WinCondition.RANDOM;
        initialRandomGunRounds = gunRounds <= 0;
    }

    private static void applyInitialStateFromSnapshot(ControllerSettingsSnapshot snapshot) {
        if (snapshot == null) return;
        applyInitialState(
                snapshot.win(),
                snapshot.questMode() == 1 ? CardComposition.HYBRID_CATEGORY
                        : snapshot.questMode() == 2 ? CardComposition.HYBRID_PERCENT
                        : CardComposition.CLASSIC_ONLY,
                snapshot.questPercent(),
                snapshot.categoryLogicEnabled(),
                snapshot.rarityLogicEnabled(),
                snapshot.itemColorVariantsSeparate(),
                snapshot.casino(),
                snapshot.casinoMode(),
                snapshot.rerollsMode(),
                snapshot.rerollsCount(),
                snapshot.gunRounds(),
                snapshot.hangmanRounds(),
                snapshot.hangmanBaseSeconds(),
                snapshot.hangmanPenaltySeconds(),
                snapshot.cardDifficulty(),
                "random".equalsIgnoreCase(snapshot.cardDifficulty()),
                snapshot.gameDifficulty(),
                "random".equalsIgnoreCase(snapshot.gameDifficulty()),
                snapshot.effectsInterval(),
                snapshot.effectsInterval() < 0,
                snapshot.rtpEnabled(),
                snapshot.randomRtp(),
                snapshot.hostileMobsEnabled(),
                snapshot.randomHostileMobs(),
                snapshot.hungerEnabled(),
                snapshot.naturalRegenEnabled(),
                snapshot.randomNaturalRegen(),
                snapshot.randomHunger(),
                snapshot.cardSize(),
                snapshot.randomCardSize(),
                snapshot.keepInventoryEnabled(),
                snapshot.randomKeepInventory(),
                snapshot.hardcoreEnabled(),
                snapshot.randomHardcore(),
                snapshot.daylightMode(),
                snapshot.randomDaylight(),
                snapshot.startDelaySeconds(),
                snapshot.countdownEnabled(),
                snapshot.countdownMinutes(),
                snapshot.rushEnabled(),
                snapshot.rushSeconds(),
                snapshot.allowLateJoin(),
                snapshot.pvpEnabled(),
                snapshot.adventureMode(),
                snapshot.prelitPortalsMode(),
                snapshot.randomPvp(),
                snapshot.registerMode(),
                snapshot.randomRegister(),
                snapshot.teamSyncEnabled(),
                snapshot.teamChestEnabled(),
                snapshot.shuffleMode(),
                snapshot.starterKitMode(),
                snapshot.hideGoalDetailsInChat(),
                snapshot.minesEnabled(),
                snapshot.mineAmount(),
                snapshot.mineTimeSeconds(),
                snapshot.powerSlotEnabled(),
                snapshot.powerSlotIntervalSeconds()
        );
        initialNaturalRegenEnabled = snapshot.naturalRegenEnabled();
        initialNaturalRegenRandom = snapshot.randomNaturalRegen();
        if (snapshot.randomRegister()) {
            initialRegisterRandom = true;
        }
        initialFakeRerollsEnabled = snapshot.fakeRerollsEnabled();
        initialFakeRerollsPerPlayer = Math.max(1, Math.min(10, snapshot.fakeRerollsPerPlayer()));
    }

    public static void openWithStateForSettings(
            WinCondition win,
            CardComposition composition,
            int questPercent,
            boolean categoryLogicEnabled,
            boolean rarityLogicEnabled,
            boolean itemColorVariantsSeparate,
            boolean casino,
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
            Consumer<ControllerSettingsSnapshot> listener
    ) {
        openWithStateForSettings(
                win,
                composition,
                questPercent,
                categoryLogicEnabled,
                rarityLogicEnabled,
                itemColorVariantsSeparate,
                casino,
                casino ? 1 : 0,
                rerollsMode,
                rerollsCount,
                gunRounds,
                hangmanRounds,
                hangmanSeconds,
                hangmanPenalty,
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
                com.jamie.jamiebingo.data.BingoGameData.SHUFFLE_DISABLED,
                com.jamie.jamiebingo.data.BingoGameData.STARTER_KIT_DISABLED,
                false,
                false,
                1,
                15,
                false,
                60,
                listener
        );
    }

    public static void openWithStateForSettings(
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
            int powerSlotIntervalSeconds,
            Consumer<ControllerSettingsSnapshot> listener
    ) {
        openWithState(
                win,
                composition,
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
                hangmanSeconds,
                hangmanPenalty,
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
                powerSlotIntervalSeconds
        );
        pendingSettingsOnly = true;
        pendingSettingsListener = listener;
    }

    public BingoControllerScreen() {
        this(pendingSettingsOnly, pendingSettingsListener);
        pendingSettingsOnly = false;
        pendingSettingsListener = null;
    }

    public BingoControllerScreen(boolean settingsOnly, Consumer<ControllerSettingsSnapshot> listener) {
        super(com.jamie.jamiebingo.util.ComponentUtil.literal("Bingo Controller"));
        this.settingsOnly = settingsOnly;
        this.settingsListener = listener;
    }

    /* =========================
       STATE
       ========================= */

    private boolean initialized = false;
    private final Random rng = new Random();

    private WinCondition mode;
    private boolean randomMode;

    private int gunRounds;
    private boolean randomGunRounds;
    private int hangmanRounds;
    private int hangmanSeconds;
    private boolean hangmanTimeRandom;
    private int hangmanPenaltySeconds;

    private CardComposition quests;
    private int questPercent;
    private boolean categoryLogicEnabled;
    private boolean rarityLogicEnabled;
    private boolean itemColorVariantsSeparate;

    private RerollMode rerollMode;
    private int rerollsValue;

    private EffectsMode effectsMode;
    private int effectsInterval;

    private ToggleMode rtpMode;
    private ToggleMode pvpMode;
    private boolean adventureMode;
    private PrelitPortalsMode prelitPortalsMode;
    private ToggleMode hostileMobsMode;
    private ToggleMode hungerMode;
    private boolean naturalRegenEnabled;
    private boolean naturalRegenRandom;

    private int cardSize;
    private boolean randomCardSize;

    private ToggleMode keepInventoryMode;
    private ToggleMode hardcoreMode;
    private DaylightMode daylightMode;
    private RegisterMode registerMode;
    private boolean teamSyncEnabled;
    private boolean teamChestEnabled;
    private ShuffleMode shuffleMode;
    private StarterKitMode starterKitMode;
    private boolean hideGoalDetailsInChat;

    private boolean delayEnabled;
    private int delaySeconds;
    private boolean countdownEnabled;
    private int countdownMinutes;
    private ToggleMode rushMode;
    private int rushSeconds;
    private boolean allowLateJoin;

    private DifficultyMode cardDifficulty;
    private DifficultyMode gameDifficulty;

    private boolean casino;
    private CasinoMode casinoMode;
    private ToggleMode minesMode;
    private int mineAmount;
    private int mineTimeSeconds;
    private ToggleMode powerSlotMode;
    private int powerSlotIntervalSeconds;
    private ToggleMode fakeRerollsMode;
    private int fakeRerollsPerPlayer;
    private static final int[] DEFAULT_CUSTOM_WEIGHTS = new int[] { 75, 17, 6, 2, 1, 0 };
    private static final String CUSTOM_DIFFICULTY_PREFIX = "custom:";
    private int customCommon = DEFAULT_CUSTOM_WEIGHTS[0];
    private int customUncommon = DEFAULT_CUSTOM_WEIGHTS[1];
    private int customRare = DEFAULT_CUSTOM_WEIGHTS[2];
    private int customEpic = DEFAULT_CUSTOM_WEIGHTS[3];
    private int customLegendary = DEFAULT_CUSTOM_WEIGHTS[4];
    private int customMythic = DEFAULT_CUSTOM_WEIGHTS[5];
    private int layoutColumnsCount = 1;
    private int layoutRowsPerColumn = 1;
    private int layoutStartX = 0;
    private static final int FIXED_SCREEN_WIDTH = 640;
    private static final int FIXED_SCREEN_HEIGHT = 340;
    private int layoutSpacing = 6;
    private static final int CONTENT_TOP_MARGIN = 50;
    private static final int CONTENT_BOTTOM_MARGIN = 110;
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_WIDTH = 150;
    private static final int ROW_ARROW_WIDTH = 14;
    private static final int ROW_ARROW_GAP = 2;
    private static final int MIN_ROW_HEIGHT = 20;
    private static final int MIN_ROW_WIDTH = 110;
    private static final int ROW_GAP = 4;
    private int layoutRowHeight = ROW_HEIGHT;
    private int layoutRowWidth = ROW_WIDTH;
    private int layoutButtonHeight = ROW_HEIGHT - ROW_GAP;
    private boolean postOpenRefreshPending = true;
    private int settingsPage = 0;
    private int customDifficultyRowIndex = -1;
    private static final int SETTINGS_PAGE_COUNT = 2;
    private boolean fixedGuiScaleEnabled = false;
    private int viewportWidth = FIXED_SCREEN_WIDTH;
    private int viewportHeight = FIXED_SCREEN_HEIGHT;
    private double adaptiveScale = 1.0d;
    private double adaptiveOffsetX = 0.0d;
    private double adaptiveOffsetY = 0.0d;

    public int getSettingsPage() {
        return settingsPage;
    }

    public void setFixedGuiScaleEnabled(boolean enabled) {
        this.fixedGuiScaleEnabled = enabled;
    }

    public void setSettingsPage(int page) {
        int normalized = ((page % SETTINGS_PAGE_COUNT) + SETTINGS_PAGE_COUNT) % SETTINGS_PAGE_COUNT;
        if (settingsPage == normalized) return;
        settingsPage = normalized;
        if (initialized) {
            rebuild();
        }
    }

    @Override
    protected void init() {
        applyFixedScreenSize();

        if (!initialized) {
            mode = initialWin;
            randomMode = initialRandomMode;

            gunRounds = clamp(initialGunRounds > 0 ? initialGunRounds : 8, 2, 20);
            randomGunRounds = initialRandomGunRounds;

            hangmanRounds = clamp(initialHangmanRounds > 0 ? initialHangmanRounds : 5, 2, 20);
            hangmanSeconds = clamp(initialHangmanSeconds > 0 ? initialHangmanSeconds : 120, 10, 300);
            hangmanTimeRandom = initialHangmanTimeRandom;
            hangmanPenaltySeconds = clamp(initialHangmanPenalty >= 0 ? initialHangmanPenalty : 60, 0, 300);

            quests = normalizeQuests(initialComposition);
            questPercent = clamp(initialQuestPercent > 0 ? initialQuestPercent : 50, 10, 100);
            categoryLogicEnabled = initialCategoryLogicEnabled;
            rarityLogicEnabled = initialRarityLogicEnabled;
            itemColorVariantsSeparate = initialItemColorVariantsSeparate;

            if (initialRerollsMode == 2) {
                rerollMode = RerollMode.RANDOM;
                rerollsValue = 3;
            } else if (initialRerollsMode == 1) {
                rerollMode = RerollMode.FIXED;
                rerollsValue = clamp(initialRerollsCount, 1, 10);
            } else {
                rerollMode = RerollMode.DISABLED;
                rerollsValue = 3;
            }

            effectsInterval = clamp(initialEffectsInterval > 0 ? initialEffectsInterval : 60, 20, 300);
            if (initialEffectsMode == 2 || initialEffectsRandom) {
                effectsMode = EffectsMode.RANDOM;
            } else if (initialEffectsMode == 1) {
                effectsMode = EffectsMode.FIXED;
            } else {
                effectsMode = EffectsMode.DISABLED;
            }

            if (isCustomDifficultyString(initialCardDifficulty)) {
                applyCustomWeightsFromString(initialCardDifficulty);
            }
            cardDifficulty = parseDifficulty(initialCardDifficulty);
            gameDifficulty = parseDifficulty(initialGameDifficulty);

            rtpMode = initialRtpRandom
                    ? ToggleMode.RANDOM
                    : (initialRtpEnabled ? ToggleMode.ENABLED : ToggleMode.DISABLED);

            pvpMode = initialPvpRandom
                    ? ToggleMode.RANDOM
                    : (initialPvpEnabled ? ToggleMode.ENABLED : ToggleMode.DISABLED);
            adventureMode = initialAdventureMode;
            prelitPortalsMode = fromPrelitPortalsModeValue(initialPrelitPortalsMode);

            hostileMobsMode = initialHostileMobsRandom
                    ? ToggleMode.RANDOM
                    : (initialHostileMobsEnabled ? ToggleMode.ENABLED : ToggleMode.DISABLED);

            hungerMode = initialHungerRandom
                    ? ToggleMode.RANDOM
                    : (initialHungerEnabled ? ToggleMode.ENABLED : ToggleMode.DISABLED);
            naturalRegenEnabled = initialNaturalRegenEnabled;
            naturalRegenRandom = initialNaturalRegenRandom;

            cardSize = clamp(initialCardSize > 0 ? initialCardSize : 5, 1, 10);
            randomCardSize = initialRandomCardSize;

            keepInventoryMode = initialKeepInventoryRandom
                    ? ToggleMode.RANDOM
                    : (initialKeepInventoryEnabled ? ToggleMode.ENABLED : ToggleMode.DISABLED);

            hardcoreMode = initialHardcoreRandom
                    ? ToggleMode.RANDOM
                    : (initialHardcoreEnabled ? ToggleMode.ENABLED : ToggleMode.DISABLED);

            daylightMode = initialDaylightRandom
                    ? DaylightMode.RANDOM
                    : parseDaylightMode(initialDaylightMode);

            registerMode = initialRegisterMode == com.jamie.jamiebingo.data.BingoGameData.REGISTER_ALWAYS_HAVE
                    ? RegisterMode.ALWAYS_HAVE
                    : RegisterMode.COLLECT_ONCE;
            if (initialRegisterRandom) {
                registerMode = RegisterMode.RANDOM;
            }
            teamSyncEnabled = initialTeamSyncEnabled;
            teamChestEnabled = initialTeamChestEnabled;
            shuffleMode = parseShuffleMode(initialShuffleMode);
            starterKitMode = parseStarterKitMode(initialStarterKitMode);
            hideGoalDetailsInChat = initialHideGoalDetailsInChat;

            delayEnabled = initialStartDelaySeconds > 0;
            delaySeconds = Math.max(10, initialStartDelaySeconds > 0 ? initialStartDelaySeconds : 10);

            countdownEnabled = initialCountdownEnabled;
            countdownMinutes = Math.max(10, initialCountdownMinutes > 0 ? initialCountdownMinutes : 10);
            rushMode = initialRushRandom
                    ? ToggleMode.RANDOM
                    : (initialRushEnabled ? ToggleMode.ENABLED : ToggleMode.DISABLED);
            rushSeconds = clamp(initialRushSeconds > 0 ? initialRushSeconds : 60, 1, 300);
            allowLateJoin = initialAllowLateJoin;

            casino = initialCasino;
            casinoMode = parseCasinoMode(initialCasinoMode, initialCasino);
            minesMode = initialMinesRandom
                    ? ToggleMode.RANDOM
                    : (initialMinesEnabled ? ToggleMode.ENABLED : ToggleMode.DISABLED);
            mineAmount = clamp(initialMineAmount <= 0 ? 1 : initialMineAmount, 1, 13);
            mineTimeSeconds = Math.max(1, initialMineTimeSeconds <= 0 ? 15 : initialMineTimeSeconds);
            powerSlotMode = initialPowerSlotRandom
                    ? ToggleMode.RANDOM
                    : (initialPowerSlotEnabled ? ToggleMode.ENABLED : ToggleMode.DISABLED);
            powerSlotIntervalSeconds = clamp(initialPowerSlotIntervalSeconds <= 0 ? 60 : initialPowerSlotIntervalSeconds, 10, 300);
            fakeRerollsMode = initialFakeRerollsEnabled ? ToggleMode.ENABLED : ToggleMode.DISABLED;
            fakeRerollsPerPlayer = clamp(initialFakeRerollsPerPlayer <= 0 ? 2 : initialFakeRerollsPerPlayer, 1, 10);
            initialized = true;
        }

        resolveLayoutSizing();
        setupLayout();
        int rowIndex = 0;
        int rowWidth = layoutRowWidth;
        customDifficultyRowIndex = -1;

        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Randomize All"),
                        b -> { randomizeCurrentPage(); rebuild(); }
                )
                .pos(8, 10)
                .size(92, layoutButtonHeight)
                .build());

        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Reset Default"),
                        b -> {
                            resetCurrentPageToDefaults();
                            rebuild();
                        }
                )
                .pos(width - 110, 10)
                .size(102, layoutButtonHeight)
                .build());

        if (settingsOnly) {
            addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                            com.jamie.jamiebingo.util.ComponentUtil.literal("Import Settings Seed"),
                            b -> importSettingsSeedFromClipboard()
                    )
                    .pos(108, 10)
                    .size(124, layoutButtonHeight)
                    .build());
        }

        if (settingsPage == 0) {
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, modeLabel(), this::cycleModeBackward, this::cycleModeForward));

            if (isGunModeSelected()) {
                rowIndex = ensureRowsInColumn(rowIndex, 2);
                rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                        addCycleRow(x, y, w, gunRoundsLabel(), this::toggleGunRoundsMode, this::toggleGunRoundsMode));
            } else {
                rowIndex = skipRow(rowIndex);
            }

            if (isGunModeSelected() && !randomGunRounds) {
                rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                        addIntSlider(x, y, w, 2, 20, () -> gunRounds, v -> gunRounds = v, "Gun Rnds: ", ""));
            } else {
                rowIndex = skipRow(rowIndex);
            }

            if (mode == WinCondition.HANGMAN) {
                rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                        addIntSlider(x, y, w, 2, 20, () -> hangmanRounds, v -> hangmanRounds = v, "Hang Rnds: ", ""));
            } else {
                rowIndex = skipRow(rowIndex);
            }

            if (mode == WinCondition.HANGMAN) {
                rowIndex = ensureRowsInColumn(rowIndex, 2);
                rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                        addCycleRow(x, y, w, hangmanTimeLabel(), this::toggleHangmanTimeMode, this::toggleHangmanTimeMode));
            } else {
                rowIndex = skipRow(rowIndex);
            }

            if (mode == WinCondition.HANGMAN && !hangmanTimeRandom) {
                rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                        addIntSlider(x, y, w, 10, 300, () -> hangmanSeconds, v -> hangmanSeconds = v, "Hang: ", "s"));
            } else {
                rowIndex = skipRow(rowIndex);
            }

            if (mode == WinCondition.HANGMAN) {
                rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                        addIntSlider(x, y, w, 0, 300, () -> hangmanPenaltySeconds, v -> hangmanPenaltySeconds = v,
                                "Penalty: ", "s"));
            } else {
                rowIndex = skipRow(rowIndex);
            }

            rowIndex = ensureRowsInColumn(rowIndex, 2);
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, cardSizeLabel(), this::cycleCardSizeBackward, this::cycleCardSizeForward));
            if (!randomCardSize) {
                rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                        addIntSlider(x, y, w, 1, 10, () -> cardSize, v -> cardSize = v, "Size: ", ""));
            } else {
                rowIndex = skipRow(rowIndex);
            }

            rowIndex = skipToRowIndex(rowIndex, layoutRowsPerColumn);
            rowIndex = ensureRowsInColumn(rowIndex, 2);
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, questsLabel(), this::cycleQuestsBackward, this::cycleQuestsForward));
            if (quests == CardComposition.HYBRID_PERCENT) {
                rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                        addIntSlider(x, y, w, 10, 100, () -> questPercent, v -> questPercent = v, "Quest %: ", "%"));
            } else {
                rowIndex = skipRow(rowIndex);
            }

            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, casinoLabel(), this::cycleCasinoModeBackward, this::cycleCasinoModeForward));
            rowIndex = ensureRowsInColumn(rowIndex, 2);
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, rerollsLabel(), this::cycleRerollModeBackward, this::cycleRerollModeForward));

            if (rerollMode == RerollMode.FIXED) {
                rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                        addIntSlider(x, y, w, 1, 10, () -> rerollsValue, v -> rerollsValue = v, "Rerolls: ", ""));
            } else {
                rowIndex = skipRow(rowIndex);
            }
            rowIndex = ensureRowsInColumn(rowIndex, 2);
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, fakeRerollsLabel(), this::cycleFakeRerollsModeBackward, this::cycleFakeRerollsModeForward));
            if (fakeRerollsMode == ToggleMode.ENABLED) {
                rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                        addIntSlider(x, y, w, 1, 10, () -> fakeRerollsPerPlayer, v -> fakeRerollsPerPlayer = v, "Fake: ", ""));
            } else {
                rowIndex = skipRow(rowIndex);
            }
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, shuffleLabel(), this::cycleShuffleModeBackward, this::cycleShuffleModeForward));

            rowIndex = ensureRowsInColumn(rowIndex, 2);
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, rushLabel(), this::cycleRushModeBackward, this::cycleRushModeForward));
            if (rushMode == ToggleMode.ENABLED) {
                rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                        addIntSlider(x, y, w, 1, 300, () -> rushSeconds, v -> rushSeconds = v, "Rush: ", "s"));
            } else {
                rowIndex = skipRow(rowIndex);
            }
            rowIndex = ensureRowsInColumn(rowIndex, 3);
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, minesLabel(), this::cycleMinesModeBackward, this::cycleMinesModeForward));
            if (minesMode == ToggleMode.ENABLED) {
                rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                        addIntSlider(x, y, w, 1, 13, () -> mineAmount, v -> mineAmount = v, "Mine Amt: ", ""));
                rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                        addIntSlider(x, y, w, 1, 120, () -> mineTimeSeconds, v -> mineTimeSeconds = v, "Mine Time: ", "s"));
            } else {
                rowIndex = skipRow(rowIndex);
                rowIndex = skipRow(rowIndex);
            }
            rowIndex = ensureRowsInColumn(rowIndex, 2);
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, powerSlotLabel(), this::cyclePowerSlotModeBackward, this::cyclePowerSlotModeForward));
            if (powerSlotMode == ToggleMode.ENABLED) {
                rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                        addIntSlider(x, y, w, 10, 300, () -> powerSlotIntervalSeconds, v -> powerSlotIntervalSeconds = v, "Power: ", "s"));
            } else {
                rowIndex = skipRow(rowIndex);
            }

            rowIndex = ensureRowsInColumn(rowIndex, 2);
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, effectsLabel(), this::cycleEffectsModeBackward, this::cycleEffectsModeForward));
            if (effectsMode == EffectsMode.FIXED) {
                rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                        addIntSlider(x, y, w, 20, 300, () -> effectsInterval, v -> effectsInterval = v, "Effects: ", "s"));
            } else {
                rowIndex = skipRow(rowIndex);
            }

            rowIndex = skipToRowIndex(rowIndex, layoutRowsPerColumn * 2);
            rowIndex = ensureRowsInColumn(rowIndex, 2);
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, delayLabel(), this::toggleDelay, this::toggleDelay));
            if (delayEnabled) {
                rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                        addIntSlider(x, y, w, 10, 120, () -> delaySeconds, v -> delaySeconds = v, "Delay: ", "s"));
            } else {
                rowIndex = skipRow(rowIndex);
            }

            rowIndex = skipToRowIndex(rowIndex, layoutRowsPerColumn * 3);
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, "Game: " + gameDifficulty, this::cycleGameDifficultyBackward, this::cycleGameDifficultyForward));
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, "Card: " + cardDifficulty, this::cycleCardDifficultyBackward, this::cycleCardDifficultyForward));

            customDifficultyRowIndex = rowIndex;
            if (cardDifficulty == DifficultyMode.CUSTOM) {
                rowIndex = skipRow(rowIndex);
                rowIndex = skipRow(rowIndex);
                rowIndex = skipRow(rowIndex);
                rowIndex = skipRow(rowIndex);
                rowIndex = skipRow(rowIndex);
                rowIndex = skipRow(rowIndex);
            } else {
                rowIndex = skipRow(rowIndex);
                rowIndex = skipRow(rowIndex);
                rowIndex = skipRow(rowIndex);
                rowIndex = skipRow(rowIndex);
                rowIndex = skipRow(rowIndex);
                rowIndex = skipRow(rowIndex);
            }
        } else {
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, categoryLogicLabel(), this::toggleCategoryLogic, this::toggleCategoryLogic));
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, rarityLogicLabel(), this::toggleRarityLogic, this::toggleRarityLogic));
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, itemColorVariantsLabel(), this::toggleItemColorVariants, this::toggleItemColorVariants));
            rowIndex = ensureRowsInColumn(rowIndex, 2);
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, countdownLabel(), this::toggleCountdown, this::toggleCountdown));
            if (countdownEnabled) {
                rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                        addIntSlider(x, y, w, 10, 180, () -> countdownMinutes, v -> countdownMinutes = v, "Countdown: ", "m"));
            } else {
                rowIndex = skipRow(rowIndex);
            }
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, registerLabel(), this::cycleRegisterBackward, this::cycleRegisterForward));
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, teamSyncLabel(), this::toggleTeamSync, this::toggleTeamSync));
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, teamChestLabel(), this::toggleTeamChest, this::toggleTeamChest));
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, rtpLabel(), this::cycleRtpModeBackward, this::cycleRtpModeForward));
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, pvpLabel(), this::cyclePvpModeBackward, this::cyclePvpModeForward));
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, hostileMobsLabel(), this::cycleHostileMobsModeBackward, this::cycleHostileMobsModeForward));
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, hungerLabel(), this::cycleHungerModeBackward, this::cycleHungerModeForward));
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, naturalRegenLabel(), this::cycleNaturalRegenBackward, this::cycleNaturalRegenForward));
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, keepInventoryLabel(), this::cycleKeepInventoryModeBackward, this::cycleKeepInventoryModeForward));
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, hardcoreLabel(), this::cycleHardcoreModeBackward, this::cycleHardcoreModeForward));
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, daylightLabel(), this::cycleDaylightBackward, this::cycleDaylightForward));
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, allowLateJoinLabel(), this::toggleAllowLateJoin, this::toggleAllowLateJoin));
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, starterKitLabel(), this::cycleStarterKitBackward, this::cycleStarterKitForward));
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, hideGoalDetailsLabel(), this::toggleHideGoalDetailsInChat, this::toggleHideGoalDetailsInChat));
            rowIndex = addRow(rowIndex, rowWidth, (x, y, w) ->
                    addCycleRow(x, y, w, adventureModeLabel(), this::toggleAdventureMode, this::toggleAdventureMode));
        }

        /* ---------- START / SAVE (ALWAYS VISIBLE) ---------- */
        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal(settingsOnly ? "SAVE SETTINGS" : "START BINGO"),
                        b -> {
                            if (settingsOnly) {
                                saveSettings();
                            } else {
                                startGame();
                            }
                        }
                )
                .pos((width - 180) / 2, height - 30)
                .size(180, 22)
                .build());

        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("<"),
                        b -> {
                            settingsPage = (settingsPage + SETTINGS_PAGE_COUNT - 1) % SETTINGS_PAGE_COUNT;
                            rebuild();
                        })
                .pos(8, height - 30)
                .size(20, 22)
                .build());

        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal(">"),
                        b -> {
                            settingsPage = (settingsPage + 1) % SETTINGS_PAGE_COUNT;
                            rebuild();
                        })
                .pos(width - 28, height - 30)
                .size(20, 22)
                .build());

        if (settingsPage == 0 && cardDifficulty == DifficultyMode.CUSTOM) {
            addCompactCustomDifficultyPanel();
        }

    }

    /* =========================
       START GAME
       ========================= */

    private ControllerSettingsSnapshot resolveSettingsSnapshot() {

        // ✅ Resolve RANDOM into REAL values BEFORE sending to server.
        WinCondition finalMode = randomMode ? pickFairModeExcludingRandom() : mode;

        int finalGunRounds = 0;
        if (finalMode == WinCondition.GUNGAME || finalMode == WinCondition.GAMEGUN) {
            // If user explicitly chose gun rounds UI, use it; otherwise weighted random (required for Mode Random).
            if (!randomMode && isGunModeSelected()) {
                finalGunRounds = randomGunRounds ? pickWeightedInt(2, 20, 8) : clamp(gunRounds, 2, 20);
            } else {
                finalGunRounds = pickWeightedInt(2, 20, 8);
            }
        }

        int finalHangmanRounds = clamp(hangmanRounds, 2, 20);
        int finalHangmanSeconds = clamp(hangmanSeconds, 10, 300);
        int finalHangmanPenalty = clamp(hangmanPenaltySeconds, 0, 300);
        if (finalMode == WinCondition.HANGMAN) {
            boolean timeRandom = hangmanTimeRandom || randomMode;
            if (timeRandom) {
                finalHangmanSeconds = pickWeightedInt(10, 300, 30, 0.02);
                finalHangmanPenalty = pickWeightedInt(0, 300, 10, 0.03);
            }
        }

        int finalQuestMode =
                quests == CardComposition.HYBRID_CATEGORY ? 1 :
                quests == CardComposition.HYBRID_PERCENT ? 2 : 0;

        int finalQuestPercent =
                quests == CardComposition.HYBRID_PERCENT ? questPercent
                        : (quests == CardComposition.HYBRID_CATEGORY && !categoryLogicEnabled ? 35 : 0);

        int finalRerollsMode = 0;
        int finalRerolls = 0;

        if (rerollMode == RerollMode.FIXED) {
            finalRerollsMode = 1;
            finalRerolls = rerollsValue;
        } else if (rerollMode == RerollMode.RANDOM) {
            // We resolve it here so chat + reopen show real value.
            finalRerollsMode = 1;
            finalRerolls = pickWeightedInt(1, 10, 3);
        }
        int finalEffectsInterval = 0;
        if (effectsMode == EffectsMode.FIXED) {
            finalEffectsInterval = clamp(effectsInterval, 20, 300);
        } else if (effectsMode == EffectsMode.RANDOM) {
            // 50/50 enabled/disabled, then roll a wider interval if enabled.
            if (rng.nextBoolean()) {
                finalEffectsInterval = pickRandomEffectInterval();
            } else {
                finalEffectsInterval = 0;
            }
        } // disabled => 0

        String finalCardDifficulty =
                cardDifficulty == DifficultyMode.RANDOM ? pickCardDifficulty() : cardDifficulty.name().toLowerCase();
        if (cardDifficulty == DifficultyMode.CUSTOM) {
            finalCardDifficulty = buildCustomDifficultyStringNormalized();
        }

        String finalGameDifficulty =
                gameDifficulty == DifficultyMode.RANDOM ? pickGameDifficulty() : gameDifficulty.name().toLowerCase();

        boolean finalRtpEnabled = rtpMode == ToggleMode.ENABLED;
        boolean randomRtpIntent = rtpMode == ToggleMode.RANDOM;

        boolean finalPvpEnabled = pvpMode == ToggleMode.ENABLED;
        boolean randomPvpIntent = pvpMode == ToggleMode.RANDOM;

        boolean finalHostileMobsEnabled = hostileMobsMode == ToggleMode.ENABLED;
        boolean randomHostileMobsIntent = hostileMobsMode == ToggleMode.RANDOM;

        boolean finalHungerEnabled = hungerMode == ToggleMode.ENABLED;
        boolean randomHungerIntent = hungerMode == ToggleMode.RANDOM;

        int finalCardSize = clamp(cardSize, 1, 10);
        boolean randomCardSizeIntent = randomCardSize;

        boolean finalKeepInventoryEnabled = keepInventoryMode == ToggleMode.ENABLED;
        boolean randomKeepInventoryIntent = keepInventoryMode == ToggleMode.RANDOM;

        boolean finalHardcoreEnabled = hardcoreMode == ToggleMode.ENABLED;
        boolean randomHardcoreIntent = hardcoreMode == ToggleMode.RANDOM;

        int finalDaylightMode = toDaylightModeValue(daylightMode);
        boolean randomDaylightIntent = daylightMode == DaylightMode.RANDOM;

        int finalDelaySeconds = delayEnabled ? Math.max(10, delaySeconds) : 0;
        boolean finalCountdownEnabled = countdownEnabled;
        int finalCountdownMinutes = countdownEnabled ? clamp(countdownMinutes, 10, 180) : 0;
        boolean finalRushEnabled;
        int finalRushSeconds;
        if (rushMode == ToggleMode.RANDOM) {
            finalRushEnabled = rng.nextBoolean();
            finalRushSeconds = finalRushEnabled ? pickWeightedInt(1, 300, 30, 0.015) : 60;
        } else {
            finalRushEnabled = rushMode == ToggleMode.ENABLED;
            finalRushSeconds = finalRushEnabled ? clamp(rushSeconds, 1, 300) : 60;
        }
        boolean finalAllowLateJoin = allowLateJoin;
        boolean finalMinesEnabled;
        int finalMineAmount;
        int finalMineTimeSeconds;
        if (minesMode == ToggleMode.RANDOM) {
            finalMinesEnabled = rng.nextBoolean();
            finalMineAmount = finalMinesEnabled ? pickWeightedInt(1, 13, 1, 0.5) : clamp(mineAmount, 1, 13);
            finalMineTimeSeconds = finalMinesEnabled ? pickWeightedInt(1, 120, 60, 0.01) : Math.max(1, mineTimeSeconds);
        } else {
            finalMinesEnabled = minesMode == ToggleMode.ENABLED;
            finalMineAmount = clamp(mineAmount, 1, 13);
            finalMineTimeSeconds = Math.max(1, mineTimeSeconds);
        }
        boolean finalPowerSlotEnabled;
        int finalPowerSlotIntervalSeconds;
        if (powerSlotMode == ToggleMode.RANDOM) {
            finalPowerSlotEnabled = rng.nextBoolean();
            finalPowerSlotIntervalSeconds = finalPowerSlotEnabled
                    ? pickWeightedInt(10, 300, 60, 0.0045)
                    : clamp(powerSlotIntervalSeconds, 10, 300);
        } else {
            finalPowerSlotEnabled = powerSlotMode == ToggleMode.ENABLED;
            finalPowerSlotIntervalSeconds = clamp(powerSlotIntervalSeconds, 10, 300);
        }
        boolean finalFakeRerollsEnabled;
        int finalFakeRerollsPerPlayer;
        if (fakeRerollsMode == ToggleMode.RANDOM) {
            finalFakeRerollsEnabled = rng.nextBoolean();
            finalFakeRerollsPerPlayer = finalFakeRerollsEnabled
                    ? pickWeightedInt(1, 10, 2, 0.35)
                    : clamp(fakeRerollsPerPlayer, 1, 10);
        } else {
            finalFakeRerollsEnabled = fakeRerollsMode == ToggleMode.ENABLED;
            finalFakeRerollsPerPlayer = clamp(fakeRerollsPerPlayer, 1, 10);
        }
        boolean finalNaturalRegenEnabled = naturalRegenRandom
                ? rng.nextDouble() < 0.90d
                : naturalRegenEnabled;
        boolean finalRandomNaturalRegen = naturalRegenRandom;
        int finalRegisterMode = switch (registerMode) {
            case ALWAYS_HAVE -> com.jamie.jamiebingo.data.BingoGameData.REGISTER_ALWAYS_HAVE;
            case RANDOM -> rng.nextDouble() < 0.70d
                    ? com.jamie.jamiebingo.data.BingoGameData.REGISTER_ALWAYS_HAVE
                    : com.jamie.jamiebingo.data.BingoGameData.REGISTER_COLLECT_ONCE;
            default -> com.jamie.jamiebingo.data.BingoGameData.REGISTER_COLLECT_ONCE;
        };
        boolean finalRandomRegister = registerMode == RegisterMode.RANDOM;
        int finalShuffleMode;
        if (!shuffleAllowedForMode(finalMode)) {
            finalShuffleMode = com.jamie.jamiebingo.data.BingoGameData.SHUFFLE_DISABLED;
        } else if (shuffleMode == ShuffleMode.RANDOM) {
            finalShuffleMode = rng.nextBoolean()
                    ? com.jamie.jamiebingo.data.BingoGameData.SHUFFLE_ENABLED
                    : com.jamie.jamiebingo.data.BingoGameData.SHUFFLE_DISABLED;
        } else {
            finalShuffleMode = toShuffleModeValue(shuffleMode);
        }

        return new ControllerSettingsSnapshot(
                finalMode,
                finalQuestMode,
                finalQuestPercent,
                categoryLogicEnabled,
                rarityLogicEnabled,
                itemColorVariantsSeparate,
                casinoMode != CasinoMode.DISABLED,
                toCasinoModeValue(casinoMode),
                finalRerollsMode,
                finalRerolls,
                finalGunRounds,
                finalHangmanRounds,
                finalHangmanSeconds,
                finalHangmanPenalty,
                finalCardDifficulty,
                finalGameDifficulty,
                finalEffectsInterval,
                finalRtpEnabled,
                randomRtpIntent,
                finalHostileMobsEnabled,
                randomHostileMobsIntent,
                finalHungerEnabled,
                finalNaturalRegenEnabled,
                finalRandomNaturalRegen,
                randomHungerIntent,
                finalCardSize,
                randomCardSizeIntent,
                finalKeepInventoryEnabled,
                randomKeepInventoryIntent,
                finalHardcoreEnabled,
                randomHardcoreIntent,
                finalDaylightMode,
                randomDaylightIntent,
                finalDelaySeconds,
                finalCountdownEnabled,
                finalCountdownMinutes,
                finalRushEnabled,
                finalRushSeconds,
                finalAllowLateJoin,
                finalPvpEnabled,
                adventureMode,
                toPrelitPortalsModeValue(prelitPortalsMode),
                randomPvpIntent,
                finalRegisterMode,
                finalRandomRegister,
                teamSyncEnabled,
                teamChestEnabled,
                finalShuffleMode,
                toStarterKitModeValue(starterKitMode),
                hideGoalDetailsInChat,
                finalMinesEnabled,
                finalMineAmount,
                finalMineTimeSeconds,
                finalPowerSlotEnabled,
                finalPowerSlotIntervalSeconds,
                finalFakeRerollsEnabled,
                finalFakeRerollsPerPlayer
        );
    }

    // Raw UI snapshot for shared wall syncing: preserves RANDOM selections instead of resolving them.
    private ControllerSettingsSnapshot resolveSettingsSnapshotRaw() {
        WinCondition uiMode = randomMode ? WinCondition.RANDOM : mode;
        int uiGunRounds = randomGunRounds ? 0 : clamp(gunRounds, 2, 20);
        int uiHangmanRounds = clamp(hangmanRounds, 2, 20);
        int uiHangmanSeconds = hangmanTimeRandom ? 0 : clamp(hangmanSeconds, 10, 300);
        int uiHangmanPenalty = clamp(hangmanPenaltySeconds, 0, 300);

        int uiQuestMode =
                quests == CardComposition.HYBRID_CATEGORY ? 1 :
                quests == CardComposition.HYBRID_PERCENT ? 2 : 0;
        int uiQuestPercent =
                quests == CardComposition.HYBRID_PERCENT ? questPercent
                        : (quests == CardComposition.HYBRID_CATEGORY && !categoryLogicEnabled ? 35 : 0);

        int uiRerollsMode = rerollMode == RerollMode.RANDOM ? 2 : (rerollMode == RerollMode.FIXED ? 1 : 0);
        int uiRerollsCount = rerollMode == RerollMode.FIXED ? rerollsValue : 0;
        int uiEffectsInterval = effectsMode == EffectsMode.RANDOM ? -1
                : (effectsMode == EffectsMode.FIXED ? clamp(effectsInterval, 20, 300) : 0);

        String uiCardDifficulty = cardDifficulty == DifficultyMode.RANDOM
                ? "random"
                : (cardDifficulty == DifficultyMode.CUSTOM ? buildCustomDifficultyStringNormalized() : cardDifficulty.name().toLowerCase());
        String uiGameDifficulty = gameDifficulty == DifficultyMode.RANDOM ? "random" : gameDifficulty.name().toLowerCase();

        boolean uiRtpEnabled = rtpMode == ToggleMode.ENABLED;
        boolean uiRandomRtp = rtpMode == ToggleMode.RANDOM;
        boolean uiPvpEnabled = pvpMode == ToggleMode.ENABLED;
        boolean uiRandomPvp = pvpMode == ToggleMode.RANDOM;
        boolean uiHostileEnabled = hostileMobsMode == ToggleMode.ENABLED;
        boolean uiRandomHostile = hostileMobsMode == ToggleMode.RANDOM;
        boolean uiHungerEnabled = hungerMode == ToggleMode.ENABLED;
        boolean uiRandomHunger = hungerMode == ToggleMode.RANDOM;
        boolean uiRandomNaturalRegen = naturalRegenRandom;
        boolean uiKeepInvEnabled = keepInventoryMode == ToggleMode.ENABLED;
        boolean uiRandomKeepInv = keepInventoryMode == ToggleMode.RANDOM;
        boolean uiHardcoreEnabled = hardcoreMode == ToggleMode.ENABLED;
        boolean uiRandomHardcore = hardcoreMode == ToggleMode.RANDOM;
        boolean uiRandomCardSize = randomCardSize;
        int uiCardSize = clamp(cardSize, 1, 10);
        boolean uiRandomDaylight = daylightMode == DaylightMode.RANDOM;
        int uiDaylightMode = toDaylightModeValue(daylightMode);

        int uiDelaySeconds = delayEnabled ? Math.max(10, delaySeconds) : 0;
        boolean uiCountdownEnabled = countdownEnabled;
        int uiCountdownMinutes = countdownEnabled ? clamp(countdownMinutes, 10, 180) : 0;
        boolean uiRushEnabled = rushMode == ToggleMode.ENABLED;
        int uiRushSeconds = uiRushEnabled ? clamp(rushSeconds, 1, 300) : 60;
        boolean uiAllowLateJoin = allowLateJoin;
        int uiRegisterMode = registerMode == RegisterMode.ALWAYS_HAVE
                ? com.jamie.jamiebingo.data.BingoGameData.REGISTER_ALWAYS_HAVE
                : com.jamie.jamiebingo.data.BingoGameData.REGISTER_COLLECT_ONCE;
        boolean uiRandomRegister = registerMode == RegisterMode.RANDOM;
        int uiShuffleMode = toShuffleModeValue(shuffleMode);
        boolean uiPowerEnabled = powerSlotMode == ToggleMode.ENABLED;
        int uiPowerInterval = powerSlotMode == ToggleMode.RANDOM ? 0 : clamp(powerSlotIntervalSeconds, 10, 300);
        boolean uiFakeEnabled = fakeRerollsMode == ToggleMode.ENABLED;
        int uiFakePerPlayer = fakeRerollsMode == ToggleMode.RANDOM ? 0 : clamp(fakeRerollsPerPlayer, 1, 10);
        return new ControllerSettingsSnapshot(
                uiMode,
                uiQuestMode,
                uiQuestPercent,
                categoryLogicEnabled,
                rarityLogicEnabled,
                itemColorVariantsSeparate,
                casinoMode != CasinoMode.DISABLED,
                toCasinoModeValue(casinoMode),
                uiRerollsMode,
                uiRerollsCount,
                uiGunRounds,
                uiHangmanRounds,
                uiHangmanSeconds,
                uiHangmanPenalty,
                uiCardDifficulty,
                uiGameDifficulty,
                uiEffectsInterval,
                uiRtpEnabled,
                uiRandomRtp,
                uiHostileEnabled,
                uiRandomHostile,
                uiHungerEnabled,
                naturalRegenEnabled,
                uiRandomNaturalRegen,
                uiRandomHunger,
                uiCardSize,
                uiRandomCardSize,
                uiKeepInvEnabled,
                uiRandomKeepInv,
                uiHardcoreEnabled,
                uiRandomHardcore,
                uiDaylightMode,
                uiRandomDaylight,
                uiDelaySeconds,
                uiCountdownEnabled,
                uiCountdownMinutes,
                uiRushEnabled,
                uiRushSeconds,
                uiAllowLateJoin,
                uiPvpEnabled,
                adventureMode,
                toPrelitPortalsModeValue(prelitPortalsMode),
                uiRandomPvp,
                uiRegisterMode,
                uiRandomRegister,
                teamSyncEnabled,
                teamChestEnabled,
                uiShuffleMode,
                toStarterKitModeValue(starterKitMode),
                hideGoalDetailsInChat,
                minesMode == ToggleMode.ENABLED,
                minesMode == ToggleMode.RANDOM ? 0 : clamp(mineAmount, 1, 13),
                Math.max(1, mineTimeSeconds),
                uiPowerEnabled,
                uiPowerInterval,
                uiFakeEnabled,
                uiFakePerPlayer
        );
    }

    private void saveSettings() {
        if (settingsListener != null) {
            settingsListener.accept(resolveSettingsSnapshot());
        }
        cacheCurrentState();
        if (!settingsOnly) {
            onClose();
        }
    }

    private void importSettingsSeedFromClipboard() {
        ControllerSettingsSnapshot imported = SettingsSeedCodec.decode(readClipboard());
        if (imported == null) {
            return;
        }
        applySnapshotToLiveState(imported);
        rebuild();
    }

    private String readClipboard() {
        if (minecraft == null || minecraft.keyboardHandler == null) return "";
        return minecraft.keyboardHandler.getClipboard();
    }

    private void applySnapshotToLiveState(ControllerSettingsSnapshot snapshot) {
        if (snapshot == null) return;

        mode = snapshot.win() == WinCondition.RANDOM ? DEFAULT_MODE : snapshot.win();
        randomMode = snapshot.win() == WinCondition.RANDOM;

        gunRounds = clamp(snapshot.gunRounds() > 0 ? snapshot.gunRounds() : 8, 2, 20);
        randomGunRounds = snapshot.gunRounds() <= 0;

        hangmanRounds = clamp(snapshot.hangmanRounds() > 0 ? snapshot.hangmanRounds() : 5, 2, 20);
        hangmanSeconds = clamp(snapshot.hangmanBaseSeconds() > 0 ? snapshot.hangmanBaseSeconds() : 120, 10, 300);
        hangmanTimeRandom = snapshot.hangmanBaseSeconds() <= 0;
        hangmanPenaltySeconds = clamp(snapshot.hangmanPenaltySeconds(), 0, 300);

        quests = snapshot.questMode() == 1
                ? CardComposition.HYBRID_CATEGORY
                : snapshot.questMode() == 2
                ? CardComposition.HYBRID_PERCENT
                : CardComposition.CLASSIC_ONLY;
        quests = normalizeQuests(quests);
        questPercent = clamp(snapshot.questPercent() > 0 ? snapshot.questPercent() : 50, 10, 100);
        categoryLogicEnabled = snapshot.categoryLogicEnabled();
        rarityLogicEnabled = snapshot.rarityLogicEnabled();
        itemColorVariantsSeparate = snapshot.itemColorVariantsSeparate();

        if (snapshot.rerollsMode() == 2) {
            rerollMode = RerollMode.RANDOM;
            rerollsValue = 3;
        } else if (snapshot.rerollsMode() == 1) {
            rerollMode = RerollMode.FIXED;
            rerollsValue = clamp(snapshot.rerollsCount(), 1, 10);
        } else {
            rerollMode = RerollMode.DISABLED;
            rerollsValue = 3;
        }

        effectsInterval = clamp(snapshot.effectsInterval() > 0 ? snapshot.effectsInterval() : 60, 20, 300);
        if (snapshot.effectsInterval() < 0) {
            effectsMode = EffectsMode.RANDOM;
        } else if (snapshot.effectsInterval() > 0) {
            effectsMode = EffectsMode.FIXED;
        } else {
            effectsMode = EffectsMode.DISABLED;
        }

        if (isCustomDifficultyString(snapshot.cardDifficulty())) {
            applyCustomWeightsFromString(snapshot.cardDifficulty());
        }
        cardDifficulty = parseDifficulty(snapshot.cardDifficulty());
        gameDifficulty = parseDifficulty(snapshot.gameDifficulty());

        rtpMode = snapshot.randomRtp()
                ? ToggleMode.RANDOM
                : (snapshot.rtpEnabled() ? ToggleMode.ENABLED : ToggleMode.DISABLED);
        pvpMode = snapshot.randomPvp()
                ? ToggleMode.RANDOM
                : (snapshot.pvpEnabled() ? ToggleMode.ENABLED : ToggleMode.DISABLED);
        hostileMobsMode = snapshot.randomHostileMobs()
                ? ToggleMode.RANDOM
                : (snapshot.hostileMobsEnabled() ? ToggleMode.ENABLED : ToggleMode.DISABLED);
        hungerMode = snapshot.randomHunger()
                ? ToggleMode.RANDOM
                : (snapshot.hungerEnabled() ? ToggleMode.ENABLED : ToggleMode.DISABLED);

        adventureMode = snapshot.adventureMode();
        prelitPortalsMode = fromPrelitPortalsModeValue(snapshot.prelitPortalsMode());
        naturalRegenEnabled = snapshot.naturalRegenEnabled();
        naturalRegenRandom = snapshot.randomNaturalRegen();

        cardSize = clamp(snapshot.cardSize() > 0 ? snapshot.cardSize() : 5, 1, 10);
        randomCardSize = snapshot.randomCardSize();

        keepInventoryMode = snapshot.randomKeepInventory()
                ? ToggleMode.RANDOM
                : (snapshot.keepInventoryEnabled() ? ToggleMode.ENABLED : ToggleMode.DISABLED);
        hardcoreMode = snapshot.randomHardcore()
                ? ToggleMode.RANDOM
                : (snapshot.hardcoreEnabled() ? ToggleMode.ENABLED : ToggleMode.DISABLED);
        daylightMode = snapshot.randomDaylight()
                ? DaylightMode.RANDOM
                : parseDaylightMode(snapshot.daylightMode());

        registerMode = snapshot.randomRegister()
                ? RegisterMode.RANDOM
                : (snapshot.registerMode() == com.jamie.jamiebingo.data.BingoGameData.REGISTER_ALWAYS_HAVE
                ? RegisterMode.ALWAYS_HAVE
                : RegisterMode.COLLECT_ONCE);
        teamSyncEnabled = snapshot.teamSyncEnabled();
        teamChestEnabled = snapshot.teamChestEnabled();
        shuffleMode = parseShuffleMode(snapshot.shuffleMode());
        starterKitMode = parseStarterKitMode(snapshot.starterKitMode());
        hideGoalDetailsInChat = snapshot.hideGoalDetailsInChat();

        delayEnabled = snapshot.startDelaySeconds() > 0;
        delaySeconds = Math.max(10, snapshot.startDelaySeconds() > 0 ? snapshot.startDelaySeconds() : 10);
        countdownEnabled = snapshot.countdownEnabled();
        countdownMinutes = Math.max(10, snapshot.countdownMinutes() > 0 ? snapshot.countdownMinutes() : 10);
        rushMode = snapshot.rushEnabled() ? ToggleMode.ENABLED : ToggleMode.DISABLED;
        rushSeconds = clamp(snapshot.rushSeconds() > 0 ? snapshot.rushSeconds() : 60, 1, 300);
        allowLateJoin = snapshot.allowLateJoin();

        casino = snapshot.casino();
        casinoMode = parseCasinoMode(snapshot.casinoMode(), snapshot.casino());
        minesMode = snapshot.minesEnabled() ? ToggleMode.ENABLED : ToggleMode.DISABLED;
        mineAmount = clamp(snapshot.mineAmount() <= 0 ? 1 : snapshot.mineAmount(), 1, 13);
        mineTimeSeconds = Math.max(1, snapshot.mineTimeSeconds() <= 0 ? 15 : snapshot.mineTimeSeconds());
        powerSlotMode = snapshot.powerSlotEnabled() ? ToggleMode.ENABLED : ToggleMode.DISABLED;
        powerSlotIntervalSeconds = clamp(snapshot.powerSlotIntervalSeconds() <= 0 ? 60 : snapshot.powerSlotIntervalSeconds(), 10, 300);
        fakeRerollsMode = snapshot.fakeRerollsEnabled() ? ToggleMode.ENABLED : ToggleMode.DISABLED;
        fakeRerollsPerPlayer = clamp(snapshot.fakeRerollsPerPlayer() <= 0 ? 2 : snapshot.fakeRerollsPerPlayer(), 1, 10);

        cacheCurrentState();
    }

    private void startGame() {
        ControllerSettingsSnapshot snapshot = resolveSettingsSnapshot();
        NetworkHandler.sendToServer(
                new ControllerStartGamePacket(
                        snapshot.win(),
                        randomMode,
                        snapshot.questMode(),
                        snapshot.questPercent(),
                        snapshot.categoryLogicEnabled(),
                        snapshot.rarityLogicEnabled(),
                        snapshot.itemColorVariantsSeparate(),
                        snapshot.casino(),
                        snapshot.casinoMode(),
                        snapshot.rerollsMode(),
                        snapshot.rerollsCount(),
                        snapshot.gunRounds(),
                        snapshot.hangmanRounds(),
                        snapshot.hangmanBaseSeconds(),
                        snapshot.hangmanPenaltySeconds(),
                        snapshot.cardDifficulty(),
                        false,
                        snapshot.gameDifficulty(),
                        false,
                        snapshot.effectsInterval(),
                        false,
                        snapshot.rtpEnabled(),
                        snapshot.randomRtp(),
                        snapshot.hostileMobsEnabled(),
                        snapshot.randomHostileMobs(),
                        snapshot.hungerEnabled(),
                        snapshot.naturalRegenEnabled(),
                        snapshot.randomNaturalRegen(),
                        snapshot.randomHunger(),
                        snapshot.cardSize(),
                        snapshot.randomCardSize(),
                        snapshot.keepInventoryEnabled(),
                        snapshot.randomKeepInventory(),
                        snapshot.hardcoreEnabled(),
                        snapshot.randomHardcore(),
                        snapshot.daylightMode(),
                        snapshot.randomDaylight(),
                        snapshot.startDelaySeconds(),
                        snapshot.countdownEnabled(),
                        snapshot.countdownMinutes(),
                        snapshot.rushEnabled(),
                        snapshot.rushSeconds(),
                        snapshot.allowLateJoin(),
                        snapshot.pvpEnabled(),
                        snapshot.adventureMode(),
                        snapshot.prelitPortalsMode(),
                        snapshot.randomPvp(),
                        snapshot.registerMode(),
                        snapshot.randomRegister(),
                        snapshot.teamSyncEnabled(),
                        snapshot.teamChestEnabled(),
                        snapshot.shuffleMode(),
                        snapshot.starterKitMode(),
                        snapshot.hideGoalDetailsInChat(),
                        snapshot.minesEnabled(),
                        snapshot.mineAmount(),
                        snapshot.mineTimeSeconds(),
                        snapshot.powerSlotEnabled(),
                        snapshot.powerSlotIntervalSeconds(),
                        snapshot.fakeRerollsEnabled(),
                        snapshot.fakeRerollsPerPlayer()
                )
        );

        if (!settingsOnly) {
            onClose();
        }
    }

    @Override
    public void onClose() {
        cacheCurrentState();
        super.onClose();
    }

    @Override
    public void removed() {
        cacheCurrentState();
        super.removed();
    }

    /* =========================
       UI + LOGIC HELPERS
       ========================= */

    private void rebuild() {
        clearWidgets();
        init();
    }

    private void cacheCurrentState() {
        if (!initialized) return;

        initialRandomMode = randomMode;
        initialWin = mode;
        initialComposition = quests;
        initialQuestPercent = questPercent;
        initialCategoryLogicEnabled = categoryLogicEnabled;
        initialRarityLogicEnabled = rarityLogicEnabled;
        initialItemColorVariantsSeparate = itemColorVariantsSeparate;
        initialCasino = casinoMode != CasinoMode.DISABLED;
        initialCasinoMode = toCasinoModeValue(casinoMode);
        initialGunRounds = gunRounds;
        initialRandomGunRounds = randomGunRounds;
        initialHangmanRounds = hangmanRounds;
        initialHangmanSeconds = hangmanSeconds;
        initialHangmanPenalty = hangmanPenaltySeconds;
        initialHangmanTimeRandom = hangmanTimeRandom;

        initialRerollsMode = rerollMode == RerollMode.RANDOM ? 2 : (rerollMode == RerollMode.FIXED ? 1 : 0);
        initialRerollsCount = rerollsValue;

        initialEffectsRandom = effectsMode == EffectsMode.RANDOM;
        initialEffectsMode = effectsMode == EffectsMode.RANDOM ? 2
                : (effectsMode == EffectsMode.FIXED ? 1 : 0);
        initialEffectsInterval = effectsInterval;

        initialCardDifficulty = cardDifficulty == DifficultyMode.RANDOM
                ? "random"
                : (cardDifficulty == DifficultyMode.CUSTOM
                        ? buildCustomDifficultyString()
                        : cardDifficulty.name().toLowerCase());
        initialGameDifficulty = gameDifficulty == DifficultyMode.RANDOM
                ? "random"
                : gameDifficulty.name().toLowerCase();

        initialRtpEnabled = rtpMode == ToggleMode.ENABLED;
        initialRtpRandom = rtpMode == ToggleMode.RANDOM;

        initialPvpEnabled = pvpMode == ToggleMode.ENABLED;
        initialAdventureMode = adventureMode;
        initialPrelitPortalsMode = toPrelitPortalsModeValue(prelitPortalsMode);
        initialPvpRandom = pvpMode == ToggleMode.RANDOM;

        initialHostileMobsEnabled = hostileMobsMode == ToggleMode.ENABLED;
        initialHostileMobsRandom = hostileMobsMode == ToggleMode.RANDOM;

        initialHungerEnabled = hungerMode == ToggleMode.ENABLED;
        initialHungerRandom = hungerMode == ToggleMode.RANDOM;
        initialNaturalRegenEnabled = naturalRegenEnabled;
        initialNaturalRegenRandom = naturalRegenRandom;

        initialCardSize = cardSize;
        initialRandomCardSize = randomCardSize;

        initialKeepInventoryEnabled = keepInventoryMode == ToggleMode.ENABLED;
        initialKeepInventoryRandom = keepInventoryMode == ToggleMode.RANDOM;

        initialHardcoreEnabled = hardcoreMode == ToggleMode.ENABLED;
        initialHardcoreRandom = hardcoreMode == ToggleMode.RANDOM;

        initialDaylightRandom = daylightMode == DaylightMode.RANDOM;
        initialDaylightMode = toDaylightModeValue(daylightMode);

        initialStartDelaySeconds = delayEnabled ? delaySeconds : 0;
        initialCountdownEnabled = countdownEnabled;
        initialCountdownMinutes = countdownMinutes;
        initialRushEnabled = rushMode == ToggleMode.ENABLED;
        initialRushRandom = rushMode == ToggleMode.RANDOM;
        initialRushSeconds = rushSeconds;
        initialAllowLateJoin = allowLateJoin;

        initialRegisterMode = registerMode == RegisterMode.ALWAYS_HAVE
                ? com.jamie.jamiebingo.data.BingoGameData.REGISTER_ALWAYS_HAVE
                : com.jamie.jamiebingo.data.BingoGameData.REGISTER_COLLECT_ONCE;
        initialRegisterRandom = registerMode == RegisterMode.RANDOM;
        initialTeamSyncEnabled = teamSyncEnabled;
        initialTeamChestEnabled = teamChestEnabled;
        initialShuffleMode = toShuffleModeValue(shuffleMode);
        initialStarterKitMode = toStarterKitModeValue(starterKitMode);
        initialHideGoalDetailsInChat = hideGoalDetailsInChat;
        initialMinesEnabled = minesMode == ToggleMode.ENABLED;
        initialMinesRandom = minesMode == ToggleMode.RANDOM;
        initialMineAmount = mineAmount;
        initialMineTimeSeconds = mineTimeSeconds;
        initialPowerSlotEnabled = powerSlotMode == ToggleMode.ENABLED;
        initialPowerSlotRandom = powerSlotMode == ToggleMode.RANDOM;
        initialPowerSlotIntervalSeconds = powerSlotIntervalSeconds;
        initialFakeRerollsEnabled = fakeRerollsMode == ToggleMode.ENABLED;
        initialFakeRerollsPerPlayer = fakeRerollsPerPlayer;

        cachedState = true;
        ClientControllerSettingsStore.save(resolveSettingsSnapshotRaw());
    }

    private int rowX(int rowIndex) {
        int col = Math.min(layoutColumnsCount - 1, rowIndex / layoutRowsPerColumn);
        return layoutStartX + col * (layoutRowWidth + layoutSpacing);
    }

    private int rowY(int rowIndex) {
        int row = rowIndex % layoutRowsPerColumn;
        return CONTENT_TOP_MARGIN + row * layoutRowHeight;
    }

    private int addRow(int rowIndex, int width, RowBuilder builder) {
        int x = rowX(rowIndex);
        int y = rowY(rowIndex);
        builder.add(x, y, width);
        return rowIndex + 1;
    }

    private int skipRow(int rowIndex) {
        return rowIndex + 1;
    }

    private int skipToRowIndex(int rowIndex, int targetRowIndex) {
        while (rowIndex < targetRowIndex) {
            rowIndex = skipRow(rowIndex);
        }
        return rowIndex;
    }

    private int ensureRowsInColumn(int rowIndex, int neededRows) {
        if (layoutRowsPerColumn <= 0 || neededRows <= 0) return rowIndex;
        int pos = rowIndex % layoutRowsPerColumn;
        if (pos + neededRows > layoutRowsPerColumn) {
            return rowIndex + (layoutRowsPerColumn - pos);
        }
        return rowIndex;
    }

    private interface RowBuilder {
        void add(int x, int y, int width);
    }

    private void addCycleRow(int x, int y, int width, String label, Runnable prev, Runnable next) {
        int labelWidth = width - (ROW_ARROW_WIDTH * 2 + ROW_ARROW_GAP * 2);
        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(com.jamie.jamiebingo.util.ComponentUtil.literal("<"), b -> {
                    prev.run();
                    rebuild();
                })
                .pos(x, y)
                .size(ROW_ARROW_WIDTH, layoutButtonHeight)
                .build());
        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(com.jamie.jamiebingo.util.ComponentUtil.literal(label), b -> {})
                .pos(x + ROW_ARROW_WIDTH + ROW_ARROW_GAP, y)
                .size(labelWidth, layoutButtonHeight)
                .build());
        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(com.jamie.jamiebingo.util.ComponentUtil.literal(">"), b -> {
                    next.run();
                    rebuild();
                })
                .pos(x + ROW_ARROW_WIDTH + ROW_ARROW_GAP + labelWidth + ROW_ARROW_GAP, y)
                .size(ROW_ARROW_WIDTH, layoutButtonHeight)
                .build());
    }

    private void addIntSlider(int x, int y, int width, int min, int max,
                              IntSupplier getter, IntConsumer setter, String label, String suffix) {
        addRenderableWidget(new IntSliderWidget(
                x, y, width, layoutButtonHeight,
                min, max, getter.getAsInt(),
                label, suffix, setter
        ));
    }

    private void addCompactIntSlider(int x, int y, int width, int height, int min, int max,
                                     IntSupplier getter, IntConsumer setter, String label, String suffix) {
        addRenderableWidget(new IntSliderWidget(
                x, y, width, height,
                min, max, getter.getAsInt(),
                label, suffix, setter
        ));
    }

    private void addCompactCustomDifficultyPanel() {
        int sliderWidth = 128;
        int sliderHeight = Math.max(14, layoutButtonHeight - 6);
        int gap = 3;
        int x;
        int y;
        int totalHeight = (sliderHeight + gap) * 6 - gap;
        if (customDifficultyRowIndex >= 0) {
            x = rowX(customDifficultyRowIndex) + Math.max(0, (layoutRowWidth - sliderWidth) / 2);
            y = rowY(customDifficultyRowIndex) + layoutRowHeight + 1;
        } else {
            x = width - sliderWidth - 8;
            y = Math.max(CONTENT_TOP_MARGIN, height - 36 - totalHeight);
        }
        x = clamp(x, 4, Math.max(4, width - sliderWidth - 4));
        y = clamp(y, CONTENT_TOP_MARGIN, Math.max(CONTENT_TOP_MARGIN, height - totalHeight - 36));

        addCompactIntSlider(x, y, sliderWidth, sliderHeight, 0, 100, () -> customCommon, v -> setCustomWeight(0, v), "C: ", "%");
        y += sliderHeight + gap;
        addCompactIntSlider(x, y, sliderWidth, sliderHeight, 0, 100, () -> customUncommon, v -> setCustomWeight(1, v), "U: ", "%");
        y += sliderHeight + gap;
        addCompactIntSlider(x, y, sliderWidth, sliderHeight, 0, 100, () -> customRare, v -> setCustomWeight(2, v), "R: ", "%");
        y += sliderHeight + gap;
        addCompactIntSlider(x, y, sliderWidth, sliderHeight, 0, 100, () -> customEpic, v -> setCustomWeight(3, v), "E: ", "%");
        y += sliderHeight + gap;
        addCompactIntSlider(x, y, sliderWidth, sliderHeight, 0, 100, () -> customLegendary, v -> setCustomWeight(4, v), "L: ", "%");
        y += sliderHeight + gap;
        addCompactIntSlider(x, y, sliderWidth, sliderHeight, 0, 100, () -> customMythic, v -> setCustomWeight(5, v), "M: ", "%");
    }

    private void resolveLayoutSizing() {
        layoutSpacing = 6;
        layoutRowHeight = ROW_HEIGHT;
        layoutButtonHeight = ROW_HEIGHT - ROW_GAP;
        layoutRowWidth = ROW_WIDTH;
        int totalRows = Math.max(1, getReservedRowCount());
        layoutColumnsCount = settingsPage == 0 ? 4 : 3;
        layoutRowsPerColumn = (int) Math.ceil(totalRows / (double) layoutColumnsCount);
    }

    private void setupLayout() {
        if (settingsPage == 0) {
            layoutStartX = 10;
        } else {
            int totalWidth = layoutColumnsCount * layoutRowWidth + (layoutColumnsCount - 1) * layoutSpacing;
            layoutStartX = Math.max(10, (width - totalWidth) / 2);
        }
    }

    private int getReservedRowCount() {
        if (settingsPage == 0) return 36;
        // Extra settings has 20 fixed rows plus one optional countdown-duration slider row.
        return countdownEnabled ? 21 : 20;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isNew) {
        return super.mouseClicked(remapMouseEvent(event), isNew);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        MouseButtonEvent fixedEvent = remapMouseEvent(event);
        double fixedDragX = remapDelta(dragX);
        double fixedDragY = remapDelta(dragY);
        return super.mouseDragged(fixedEvent, fixedDragX, fixedDragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(remapMouseEvent(event));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        applyFixedScreenSize();
        updateAdaptiveViewport();
        boolean transformed = false;
        if (!fixedGuiScaleEnabled) {
            if (Math.abs(adaptiveScale - 1.0d) > 0.0001d || Math.abs(adaptiveOffsetX) > 0.0001d || Math.abs(adaptiveOffsetY) > 0.0001d) {
                var pose = graphics.pose();
                pose.pushMatrix();
                double safeScale = Math.max(0.0001d, adaptiveScale);
                pose.translate((float) (adaptiveOffsetX / safeScale), (float) (adaptiveOffsetY / safeScale));
                pose.scale((float) adaptiveScale, (float) adaptiveScale);
                transformed = true;
            }
        }
        int fixedMouseX = remapMouseX(mouseX);
        int fixedMouseY = remapMouseY(mouseY);
        try {
        if (!initialized) {
            // Launcher-safe: ensure init runs even if Minecraft#setScreen skipped it.
            this.init(this.width, this.height);
        }
        if (postOpenRefreshPending && initialized) {
            postOpenRefreshPending = false;
            rebuild();
        }
        renderBlurredBackground(graphics);
        graphics.fill(0, 0, width, height, 0xCC0D0F12);
        super.render(graphics, fixedMouseX, fixedMouseY, partialTicks);
        graphics.drawCenteredString(this.font, currentPageTitle(), width / 2, 14, 0xFFFFFFFF);
        renderHoveredTooltip(graphics, fixedMouseX, fixedMouseY);
        graphics.fill(0, height - 8, width, height, 0xCC0D0F12);
        } finally {
            if (transformed) {
                graphics.pose().popMatrix();
            }
        }
    }

    private String currentPageTitle() {
        return settingsPage == 0 ? "gamemode settings" : "Extra settings";
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Avoid double-blur from Screen.render calling renderBackground again.
    }

    private void applyFixedScreenSize() {
        if (this.minecraft == null) return;
        this.width = FIXED_SCREEN_WIDTH;
        this.height = FIXED_SCREEN_HEIGHT;
    }

    private MouseButtonEvent remapMouseEvent(MouseButtonEvent event) {
        if (event == null) return null;
        if (fixedGuiScaleEnabled) {
            return event;
        }
        updateAdaptiveViewport();
        double safeScale = Math.max(0.0001d, adaptiveScale);
        return new MouseButtonEvent(
                (event.x() - adaptiveOffsetX) / safeScale,
                (event.y() - adaptiveOffsetY) / safeScale,
                new MouseButtonInfo(event.button(), extractModifiers(event))
        );
    }

    private int remapMouseX(int mouseX) {
        if (fixedGuiScaleEnabled) return mouseX;
        updateAdaptiveViewport();
        double safeScale = Math.max(0.0001d, adaptiveScale);
        return (int) Math.floor((mouseX - adaptiveOffsetX) / safeScale);
    }

    private int remapMouseY(int mouseY) {
        if (fixedGuiScaleEnabled) return mouseY;
        updateAdaptiveViewport();
        double safeScale = Math.max(0.0001d, adaptiveScale);
        return (int) Math.floor((mouseY - adaptiveOffsetY) / safeScale);
    }

    private double remapDelta(double delta) {
        if (fixedGuiScaleEnabled) return delta;
        updateAdaptiveViewport();
        double safeScale = Math.max(0.0001d, adaptiveScale);
        return delta / safeScale;
    }

    private void updateAdaptiveViewport() {
        if (this.minecraft == null) {
            viewportWidth = FIXED_SCREEN_WIDTH;
            viewportHeight = FIXED_SCREEN_HEIGHT;
            adaptiveScale = 1.0d;
            adaptiveOffsetX = 0.0d;
            adaptiveOffsetY = 0.0d;
            return;
        }

        Object window = com.jamie.jamiebingo.client.ClientMinecraftUtil.getWindow(this.minecraft);
        int vw = com.jamie.jamiebingo.util.WindowUtil.getGuiScaledWidth(window);
        int vh = com.jamie.jamiebingo.util.WindowUtil.getGuiScaledHeight(window);
        viewportWidth = vw > 0 ? vw : FIXED_SCREEN_WIDTH;
        viewportHeight = vh > 0 ? vh : FIXED_SCREEN_HEIGHT;

        if (fixedGuiScaleEnabled) {
            adaptiveScale = 1.0d;
            adaptiveOffsetX = 0.0d;
            adaptiveOffsetY = 0.0d;
            return;
        }

        adaptiveScale = Math.min(
                1.0d,
                Math.min(
                        viewportWidth / (double) FIXED_SCREEN_WIDTH,
                        viewportHeight / (double) FIXED_SCREEN_HEIGHT
                )
        );
        adaptiveOffsetX = Math.max(0.0d, (viewportWidth - (FIXED_SCREEN_WIDTH * adaptiveScale)) * 0.5d);
        adaptiveOffsetY = Math.max(0.0d, (viewportHeight - (FIXED_SCREEN_HEIGHT * adaptiveScale)) * 0.5d);
    }

    private static int extractModifiers(MouseButtonEvent event) {
        try {
            Method m = event.getClass().getMethod("modifiers");
            Object out = m.invoke(event);
            if (out instanceof Number n) return n.intValue();
        } catch (Throwable ignored) {
        }
        try {
            Method m = event.getClass().getMethod("getModifiers");
            Object out = m.invoke(event);
            if (out instanceof Number n) return n.intValue();
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private void renderHoveredTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (var child : this.children()) {
            if (!(child instanceof AbstractWidget widget)) continue;
            if (!widget.isMouseOver(mouseX, mouseY)) continue;
            String tooltip = tooltipForWidget(widget);
            if (tooltip == null || tooltip.isBlank()) return;
            ScreenTooltipUtil.drawTextTooltip(
                    graphics,
                    this.font,
                    tooltip,
                    mouseX,
                    mouseY,
                    this.width,
                    this.height,
                    Math.max(180, this.width - 24)
            );
            return;
        }
    }

    private String tooltipForWidget(AbstractWidget widget) {
        if (widget == null || widget.getMessage() == null) return null;
        String message = widget.getMessage().getString();
        boolean slider = widget instanceof IntSliderWidget || widget instanceof AbstractSliderButton;

        if (message.equals("Randomize All")) return "a randomised preset.";
        if (message.startsWith("Mode: LINE")) return "first team to get a line wins!.";
        if (message.startsWith("Mode: FULL")) return "first team to complete the full card wins!.";
        if (message.startsWith("Mode: LOCKOUT")) return "Once a team completes a slot, no other team can complete that same slot, it's locked out!.";
        if (message.startsWith("Mode: RARITY")) return "Same as lockout, but rather than the most completed slots wins, the rarer the item/quest completed, the more points the slot is worth. Lines are worth double points!.";
        if (message.startsWith("Mode: BLIND")) return "Only the top left slot is revealed, upon completion, the slots surrounding it are revealed. The first team to collect the bottom right slot wins the game!.";
        if (message.startsWith("Mode: HANGMAN")) return "A letter is revealed at each time interval, be the first player to figure out what the item/quest is and complete it. Their can be multiple rounds per game, the team who wins the most rounds wins!.";
        if (message.startsWith("Mode: GUNGAME")) return "A multiple card mode whereby a team needs to complete 1 slot on their card to move onto a new card. The first player to complete all of the cards wins. All cards are the same for all teams and the card only moves onto the next for the team that completed it meaning that one team can be on card 8 whilst another can be falling behind on card 4. Set card size to 1 to imitate the popular gungame mode feel in shooter games!";
        if (message.startsWith("Mode: GAMEGUN")) return "The same as gungame, however, all teams are always on the same card, meaning that once one player completes an item on a card, they get that point for the round and all other teams skip that card. Kind of like gungame meets lockout!.";
        if (message.startsWith("Mode: Rand")) return "random game mode!";
        if (message.startsWith("Hang Rnds:")) return "Number of hangman rounds.";
        if (message.startsWith("Hang: ") && !slider) return "Use the slider below to select the time interval in seconds in which a random letter is revealed, or set it to \"Rand\" for a random set interval.";
        if (message.startsWith("Hang: ") && slider) return "Use the slider below to select the time interval in seconds in which a random letter is revealed, or set it to \"Rand\" for a random set interval.";
        if (message.startsWith("Penalty:")) return "For every other letter adds this amount of time to the reveal interval. For example if the interval is 20s with a penalty of 10s, the first letter will reveal in 20s, then the second letter will reveal 30s later, then the next 40s later, etc.";
        if (message.startsWith("Size: ") && !slider) return "use the slider below to chose a card size from 1x1 to 10x10, or select \"Rand\" to have it randomised.";
        if (message.startsWith("Size: ") && slider) return "Chose a card size from 1x1 to 10x10.";
        if (message.equals("Quests: Off")) return "Chose whether quests can appear on the card!";
        if (message.equals("Quests: Cat")) return "Category hybrid mode. If category logic is off, this defaults to 35% quests.";
        if (message.equals("Quests: %")) return "Use the slider below to select a specific chance for a quest to appear per slot.";
        if (message.startsWith("Quests: ")) return "Chose whether quests can appear on the card!";
        if (message.startsWith("Quest %:")) return "Select a specific chance for a quest to appear per slot.";
        if (message.startsWith("Category Logic: ")) return "When On, the card tries to spread picks across categories so you do not get too many similar tasks. When Off, category balancing is ignored.";
        if (message.startsWith("Rarity Logic: ")) return "When On, easier difficulties roll more common tasks and harder ones roll more rare tasks. When Off, rarities are not used for weighting, but rarity labels still show on slots.";
        if (message.startsWith("Item Colors: ")) return "When Grouped, colored variants (like wool colors) count as one entry and a random color is chosen if rolled. This helps reduce many colored variants of the same block appearing on one card. When Separate, each color can appear as its own slot.";
        if (message.startsWith("Effects: ") && !slider) return "Gives all players a random potion or custom effect every time interval. The time interval can be chosen using the slider below. Chose \"Rand\" to randomise this setting. Custom effects include random teleport, player swap, player size, inverted drowning, inverted screen, and random movement!.";
        if (message.startsWith("Effects: ") && slider) return "Chose how often a new effect is given to all players.";
        if (message.startsWith("RTP: ")) return "Random teleport on death.";
        if (message.startsWith("PVP: ")) return "Chose if players can attack each other.";
        if (message.startsWith("Adventure: ")) return "If enabled, players start in Adventure mode instead of Survival. Recommended: import the adventure preset in the rarity changer first.";
        if (message.startsWith("Hostile: ")) return "Chose if hostile mobs spawn, disabling this also makes neutral mobs not aggro on players.";
        if (message.startsWith("Hunger: ")) return "Chose if players can get hungry.";
        if (message.startsWith("Natural Regen: ")) return "If off, natural health regeneration is turned off.";
        if (message.startsWith("KeepInv: ")) return "Chose if keep inventory is enabled.";
        if (message.startsWith("Hardcore: ")) return "Chose to enable hardcore mode (players who die are eliminated and sent to spectator mode. A win is automatically given to the last team standing if all other teams die).";
        if (message.startsWith("Time: ")) return "Select \"Cycle\" to enable normal daylight cycle, or select a permanent time of day.";
        if (message.startsWith("Register: ")) return "Chose if all players need to collect an item once for them to complete a slot, or if they must always be holding the items, in which case if they drop an item on the card, they lose that slot!.";
        if (message.startsWith("TeamSync: ")) return "Chose if progress towards a quest is shared between all members of a team, or is player specific. For example, for collect 64 glass if teamsync is on, then one player can get 32, and another player can get 32 to complete the slot, if off then one player on the team has to get all 64 glass.";
        if (message.startsWith("Starter Kit: ")) return "Give all players a starter inventory on spawn. Also removes starter-kit freebies from the card pool.";
        if (message.startsWith("Hide Goal Details: ")) return "If enabled, chat says a player/team completed a goal without revealing which goal.";
        if (message.startsWith("Late: ")) return "Chose if spectators can join midgame.";
        if (message.startsWith("Delay: ") && !slider) return "Forces card fullscreen to be displayed for a select amount of time (using the slider below) before the game starts so that players have time to inspect the card beforehand!";
        if (message.startsWith("Delay: ") && slider) return "Select the amount of time players are locked into fullscreen card mode before game start.";
        if (message.startsWith("Countdown: ") && !slider) return "Add a time limit (adjustable using the slider below) to games, the timer is shown on the top left of your screen, once it reaches 0s, the team with the most points wins!";
        if (message.startsWith("Countdown: ") && slider) return "Chose the maximum length a game can last.";
        if (message.startsWith("Rush: ") && !slider) return "If enabled, once a team completes a slot, they must complete another slot within the selected seconds or they are eliminated. Rand gives a 50/50 on/off roll, and if on it randomises rush time weighted around 30s.";
        if (message.startsWith("Rush: ") && slider) return "Set rush timeout in seconds before elimination.";
        if (message.startsWith("Casino: ")) return "Off skips pregame card phases. On uses casino mode (auto-fills a random card through casino rolls). Draft uses the same card reveal style, but players take turns choosing from 3 rolled options and placing them until the card is full.";
        if (message.startsWith("Rerolls: ") && !slider) return "Gives players the ability to reroll items on the card by clicking on a slot, the number of rerolls per player can be chosen using the slider below!";
        if (message.startsWith("Rerolls: ") && slider) return "Chose how many rerolls each player gets.";
        if (message.startsWith("Shuffle: ")) return "When a team completes a line, all non-completed slots reroll for that team only. Works in Full/Lockout/Rarity. Rand is 50/50.";
        if (message.startsWith("Mines: ")) return "Mines assigns danger objectives (like Don't touch water). Triggering one starts a countdown; when it reaches 0 you are eliminated. Rand gives a 50/50 on/off roll, and if on randomises mine amount and mine timer.";
        if (message.startsWith("Mine Amt: ")) return "chose how many mines are active";
        if (message.startsWith("Mine Time: ")) return "How many seconds after triggering a mine before elimination.";
        if (message.startsWith("Power Slot: ")) return "Power slot spawns a rotating bonus objective. Completing it triggers a wheel: buff rerolls 1 unclaimed slot on your team one rarity more common, sabotage rerolls 1 unclaimed slot on every other team one rarity less common. On uses the slider below. Rand gives a 50/50 on/off roll and weighted interval around 60s. Auto-disabled in Lockout, Rarity, and Gamegun.";
        if (message.startsWith("Power: ")) return "How often the power slot rerolls in seconds.";
        if (message.startsWith("Fake Rerolls: ")) return "Adds a pregame fake reroll phase. When your team picks a slot, your team sees its real rerolled goal, but other teams keep seeing the old fake goal for that slot until it is revealed. If an enemy completes what they see and it is fake, it shows FAKE, gives no point, and reveals the real goal to them. Rand gives a 50/50 on/off roll and weighted rerolls around 2.";
        if (message.startsWith("Fake: ")) return "How many fake rerolls each player gets.";
        if (message.startsWith("Game: ")) return "Chose the Minecraft vanilla game difficulty.";
        if (message.startsWith("Card: ")) return "Chose the card difficulty from a preset, or at random,  or make your own custom difficulty by adjusting the sliders below.";
        if (message.equals("Start Bingo")) return "Start the game! Good luck!";
        return null;
    }

    private void cycleMode() {
        if (randomMode) {
            randomMode = false;
            mode = WinCondition.LINE;
            return;
        }

        WinCondition[] values = new WinCondition[] {
                WinCondition.LINE,
                WinCondition.FULL,
                WinCondition.LOCKOUT,
                WinCondition.RARITY,
                WinCondition.BLIND,
                WinCondition.HANGMAN,
                WinCondition.GUNGAME,
                WinCondition.GAMEGUN
        };
        int index = 0;
        for (; index < values.length; index++) {
            if (values[index] == mode) break;
        }
        int next = index + 1;

        if (next >= values.length) {
            randomMode = true;
        } else {
            mode = values[next];
        }
    }

    private void cycleModeForward() {
        cycleMode();
    }

    private void cycleModeBackward() {
        WinCondition[] values = new WinCondition[] {
                WinCondition.LINE,
                WinCondition.FULL,
                WinCondition.LOCKOUT,
                WinCondition.RARITY,
                WinCondition.BLIND,
                WinCondition.HANGMAN,
                WinCondition.GUNGAME,
                WinCondition.GAMEGUN
        };
        if (randomMode) {
            randomMode = false;
            mode = values[values.length - 1];
            return;
        }
        int index = 0;
        for (; index < values.length; index++) {
            if (values[index] == mode) break;
        }
        int prev = index - 1;
        if (prev < 0) {
            randomMode = true;
        } else {
            mode = values[prev];
        }
    }

    private String modeLabel() {
        return randomMode ? "Mode: Rand" : "Mode: " + mode.name();
    }

    private boolean isGunModeSelected() {
        return !randomMode && (mode == WinCondition.GUNGAME || mode == WinCondition.GAMEGUN);
    }

    private void toggleGunRoundsMode() {
        randomGunRounds = !randomGunRounds;
    }

    private String gunRoundsLabel() {
        return randomGunRounds ? "Gun: Random" : "Gun: Fixed";
    }

    // Keep your wrap behaviour
    private void incGunRounds() {
        if (randomGunRounds) {
            randomGunRounds = false;
            gunRounds = 2;
        } else if (++gunRounds > 20) {
            randomGunRounds = true;
            gunRounds = 8;
        }
    }

    private void decGunRounds() {
        if (randomGunRounds) {
            randomGunRounds = false;
            gunRounds = 20;
        } else if (--gunRounds < 2) {
            randomGunRounds = true;
            gunRounds = 8;
        }
    }

    private void cycleQuests() {
        quests =
                quests == CardComposition.CLASSIC_ONLY ? CardComposition.HYBRID_CATEGORY :
                quests == CardComposition.HYBRID_CATEGORY ? CardComposition.HYBRID_PERCENT :
                        CardComposition.CLASSIC_ONLY;
    }

    private void cycleQuestsForward() {
        cycleQuests();
    }

    private void cycleQuestsBackward() {
        quests =
                quests == CardComposition.CLASSIC_ONLY ? CardComposition.HYBRID_PERCENT :
                quests == CardComposition.HYBRID_PERCENT ? CardComposition.HYBRID_CATEGORY :
                        CardComposition.CLASSIC_ONLY;
    }

    private String questsLabel() {
        return "Quests: " + switch (quests) {
            case CLASSIC_ONLY -> "Off";
            case HYBRID_CATEGORY -> "Cat";
            case HYBRID_PERCENT -> "%";
            default -> "Off";
        };
    }

    private void toggleCategoryLogic() {
        categoryLogicEnabled = !categoryLogicEnabled;
    }

    private void toggleRarityLogic() {
        rarityLogicEnabled = !rarityLogicEnabled;
    }

    private void toggleItemColorVariants() {
        itemColorVariantsSeparate = !itemColorVariantsSeparate;
    }

    private String categoryLogicLabel() {
        return "Category Logic: " + (categoryLogicEnabled ? "On" : "Off");
    }

    private String rarityLogicLabel() {
        return "Rarity Logic: " + (rarityLogicEnabled ? "On" : "Off");
    }

    private String itemColorVariantsLabel() {
        return "Item Colors: " + (itemColorVariantsSeparate ? "Separate" : "Grouped");
    }

    private void cycleCardSize() {
        if (randomCardSize) {
            randomCardSize = false;
            cardSize = 1;
            return;
        }
        if (cardSize >= 10) {
            randomCardSize = true;
            cardSize = 5;
            return;
        }
        cardSize++;
    }

    private void cycleCardSizeForward() {
        randomCardSize = !randomCardSize;
    }

    private void cycleCardSizeBackward() {
        randomCardSize = !randomCardSize;
    }

    private String cardSizeLabel() {
        return randomCardSize ? "Size: Rand" : "Size: " + cardSize;
    }

    private void cycleRerollMode() {
        rerollMode = switch (rerollMode) {
            case DISABLED -> RerollMode.FIXED;
            case FIXED -> RerollMode.RANDOM;
            case RANDOM -> RerollMode.DISABLED;
        };
    }

    private void cycleRerollModeForward() {
        cycleRerollMode();
    }

    private void cycleRerollModeBackward() {
        rerollMode = switch (rerollMode) {
            case DISABLED -> RerollMode.RANDOM;
            case RANDOM -> RerollMode.FIXED;
            case FIXED -> RerollMode.DISABLED;
        };
    }

    private String rerollsLabel() {
        return switch (rerollMode) {
            case DISABLED -> "Rerolls: Off";
            case FIXED -> "Rerolls: On";
            case RANDOM -> "Rerolls: Rand";
        };
    }

    private void cycleEffectsMode() {
        effectsMode = switch (effectsMode) {
            case DISABLED -> EffectsMode.FIXED;
            case FIXED -> EffectsMode.RANDOM;
            case RANDOM -> EffectsMode.DISABLED;
        };
    }

    private void cycleEffectsModeForward() {
        cycleEffectsMode();
    }

    private void cycleEffectsModeBackward() {
        effectsMode = switch (effectsMode) {
            case DISABLED -> EffectsMode.RANDOM;
            case RANDOM -> EffectsMode.FIXED;
            case FIXED -> EffectsMode.DISABLED;
        };
    }

    private String effectsLabel() {
        return switch (effectsMode) {
            case DISABLED -> "Effects: Off";
            case FIXED -> "Effects: " + effectsInterval + "s";
            case RANDOM -> "Effects: Rand";
        };
    }

    private void cycleRtpMode() {
        rtpMode = switch (rtpMode) {
            case DISABLED -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.DISABLED;
        };
    }

    private void cycleRtpModeForward() {
        cycleRtpMode();
    }

    private void cycleRtpModeBackward() {
        rtpMode = switch (rtpMode) {
            case DISABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.DISABLED;
        };
    }

    private String rtpLabel() {
        return switch (rtpMode) {
            case DISABLED -> "RTP: Off";
            case ENABLED -> "RTP: On";
            case RANDOM -> "RTP: Rand";
        };
    }

    private void cyclePvpMode() {
        pvpMode = switch (pvpMode) {
            case DISABLED -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.DISABLED;
        };
    }

    private void cyclePvpModeForward() {
        cyclePvpMode();
    }

    private void cyclePvpModeBackward() {
        pvpMode = switch (pvpMode) {
            case DISABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.DISABLED;
        };
    }

    private String pvpLabel() {
        return switch (pvpMode) {
            case DISABLED -> "PVP: Off";
            case ENABLED -> "PVP: On";
            case RANDOM -> "PVP: Rand";
        };
    }

    private void toggleAdventureMode() {
        adventureMode = !adventureMode;
    }

    private String adventureModeLabel() {
        return "Adventure: " + (adventureMode ? "On" : "Off");
    }

    private void cyclePrelitPortalsForward() {
        prelitPortalsMode = prelitPortalsMode.next();
    }

    private void cyclePrelitPortalsBackward() {
        prelitPortalsMode = prelitPortalsMode.previous();
    }

    private String prelitPortalsLabel() {
        return "Prelit Portals: " + switch (prelitPortalsMode) {
            case NETHER -> "Nether";
            case END -> "End";
            case BOTH -> "Both";
            default -> "Off";
        };
    }

    private void cycleHostileMobsMode() {
        hostileMobsMode = switch (hostileMobsMode) {
            case DISABLED -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.DISABLED;
        };
    }

    private void cycleHostileMobsModeForward() {
        cycleHostileMobsMode();
    }

    private void cycleHostileMobsModeBackward() {
        hostileMobsMode = switch (hostileMobsMode) {
            case DISABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.DISABLED;
        };
    }

    private String hostileMobsLabel() {
        return switch (hostileMobsMode) {
            case DISABLED -> "Hostile: Off";
            case ENABLED -> "Hostile: On";
            case RANDOM -> "Hostile: Rand";
        };
    }

    private void cycleHungerMode() {
        hungerMode = switch (hungerMode) {
            case DISABLED -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.DISABLED;
        };
    }

    private void cycleHungerModeForward() {
        cycleHungerMode();
    }

    private void cycleHungerModeBackward() {
        hungerMode = switch (hungerMode) {
            case DISABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.DISABLED;
        };
    }

    private String hungerLabel() {
        return switch (hungerMode) {
            case DISABLED -> "Hunger: Off";
            case ENABLED -> "Hunger: On";
            case RANDOM -> "Hunger: Rand";
        };
    }

    private void cycleNaturalRegenForward() {
        if (naturalRegenRandom) {
            naturalRegenRandom = false;
            naturalRegenEnabled = false;
        } else if (!naturalRegenEnabled) {
            naturalRegenEnabled = true;
        } else {
            naturalRegenEnabled = true;
            naturalRegenRandom = true;
        }
    }

    private void cycleNaturalRegenBackward() {
        if (naturalRegenRandom) {
            naturalRegenRandom = false;
            naturalRegenEnabled = true;
        } else if (naturalRegenEnabled) {
            naturalRegenEnabled = false;
        } else {
            naturalRegenEnabled = true;
            naturalRegenRandom = true;
        }
    }

    private String naturalRegenLabel() {
        return "Natural Regen: " + (naturalRegenRandom ? "Rand" : (naturalRegenEnabled ? "On" : "Off"));
    }

    private void cycleKeepInventoryMode() {
        keepInventoryMode = switch (keepInventoryMode) {
            case DISABLED -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.DISABLED;
        };
    }

    private void cycleKeepInventoryModeForward() {
        cycleKeepInventoryMode();
    }

    private void cycleKeepInventoryModeBackward() {
        keepInventoryMode = switch (keepInventoryMode) {
            case DISABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.DISABLED;
        };
    }

    private String keepInventoryLabel() {
        return switch (keepInventoryMode) {
            case DISABLED -> "KeepInv: Off";
            case ENABLED -> "KeepInv: On";
            case RANDOM -> "KeepInv: Rand";
        };
    }

    private void cycleHardcoreMode() {
        hardcoreMode = switch (hardcoreMode) {
            case DISABLED -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.DISABLED;
        };
    }

    private void cycleHardcoreModeForward() {
        cycleHardcoreMode();
    }

    private void cycleHardcoreModeBackward() {
        hardcoreMode = switch (hardcoreMode) {
            case DISABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.DISABLED;
        };
    }

    private String hardcoreLabel() {
        return switch (hardcoreMode) {
            case DISABLED -> "Hardcore: Off";
            case ENABLED -> "Hardcore: On";
            case RANDOM -> "Hardcore: Rand";
        };
    }

    private DaylightMode parseDaylightMode(int mode) {
        return switch (mode) {
            case com.jamie.jamiebingo.data.BingoGameData.DAYLIGHT_DAY -> DaylightMode.DAY;
            case com.jamie.jamiebingo.data.BingoGameData.DAYLIGHT_NIGHT -> DaylightMode.NIGHT;
            case com.jamie.jamiebingo.data.BingoGameData.DAYLIGHT_MIDNIGHT -> DaylightMode.MIDNIGHT;
            case com.jamie.jamiebingo.data.BingoGameData.DAYLIGHT_DAWN -> DaylightMode.DAWN;
            case com.jamie.jamiebingo.data.BingoGameData.DAYLIGHT_DUSK -> DaylightMode.DUSK;
            default -> DaylightMode.ENABLED;
        };
    }

    private int toDaylightModeValue(DaylightMode mode) {
        return switch (mode) {
            case DAY -> com.jamie.jamiebingo.data.BingoGameData.DAYLIGHT_DAY;
            case NIGHT -> com.jamie.jamiebingo.data.BingoGameData.DAYLIGHT_NIGHT;
            case MIDNIGHT -> com.jamie.jamiebingo.data.BingoGameData.DAYLIGHT_MIDNIGHT;
            case DAWN -> com.jamie.jamiebingo.data.BingoGameData.DAYLIGHT_DAWN;
            case DUSK -> com.jamie.jamiebingo.data.BingoGameData.DAYLIGHT_DUSK;
            case RANDOM -> com.jamie.jamiebingo.data.BingoGameData.DAYLIGHT_ENABLED;
            default -> com.jamie.jamiebingo.data.BingoGameData.DAYLIGHT_ENABLED;
        };
    }

    private String daylightLabel() {
        return "Time: " + switch (daylightMode) {
            case ENABLED -> "Cycle";
            case DAY -> "Day";
            case NIGHT -> "Night";
            case MIDNIGHT -> "Midnight";
            case DAWN -> "Dawn";
            case DUSK -> "Dusk";
            case RANDOM -> "Rand";
        };
    }

    private void cycleDaylightForward() {
        daylightMode = daylightMode.next();
    }

    private void cycleDaylightBackward() {
        DaylightMode[] values = DaylightMode.values();
        int index = 0;
        for (; index < values.length; index++) {
            if (values[index] == daylightMode) break;
        }
        int prev = index - 1;
        daylightMode = prev < 0 ? values[values.length - 1] : values[prev];
    }

    private void toggleDelay() {
        delayEnabled = !delayEnabled;
        if (delayEnabled && delaySeconds < 10) {
            delaySeconds = 10;
        }
    }

    private String delayLabel() {
        return delayEnabled ? "Delay: " + delaySeconds + "s" : "Delay: Off";
    }

    private String hangmanTimeLabel() {
        return hangmanTimeRandom ? "Hang: Rand" : "Hang: " + hangmanSeconds + "s";
    }

    private void toggleHangmanTimeMode() {
        hangmanTimeRandom = !hangmanTimeRandom;
    }

    private void toggleCountdown() {
        countdownEnabled = !countdownEnabled;
        if (countdownEnabled && countdownMinutes < 10) {
            countdownMinutes = 10;
        }
    }

    private String countdownLabel() {
        return countdownEnabled ? "Countdown: " + countdownMinutes + "m" : "Countdown: Off";
    }

    private void cycleRushModeForward() {
        rushMode = switch (rushMode) {
            case DISABLED -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.DISABLED;
        };
        if (rushMode == ToggleMode.ENABLED && rushSeconds < 1) {
            rushSeconds = 60;
        }
    }

    private void cycleRushModeBackward() {
        rushMode = switch (rushMode) {
            case DISABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.DISABLED;
        };
        if (rushMode == ToggleMode.ENABLED && rushSeconds < 1) {
            rushSeconds = 60;
        }
    }

    private String rushLabel() {
        return switch (rushMode) {
            case DISABLED -> "Rush: Off";
            case ENABLED -> "Rush: " + rushSeconds + "s";
            case RANDOM -> "Rush: Rand";
        };
    }

    private static int clampPrelitPortalsMode(int mode) {
        return Math.max(com.jamie.jamiebingo.data.BingoGameData.PRELIT_PORTALS_OFF,
                Math.min(com.jamie.jamiebingo.data.BingoGameData.PRELIT_PORTALS_BOTH, mode));
    }

    private static PrelitPortalsMode fromPrelitPortalsModeValue(int value) {
        return switch (clampPrelitPortalsMode(value)) {
            case com.jamie.jamiebingo.data.BingoGameData.PRELIT_PORTALS_NETHER -> PrelitPortalsMode.NETHER;
            case com.jamie.jamiebingo.data.BingoGameData.PRELIT_PORTALS_END -> PrelitPortalsMode.END;
            case com.jamie.jamiebingo.data.BingoGameData.PRELIT_PORTALS_BOTH -> PrelitPortalsMode.BOTH;
            default -> PrelitPortalsMode.OFF;
        };
    }

    private static int toPrelitPortalsModeValue(PrelitPortalsMode mode) {
        if (mode == null) return com.jamie.jamiebingo.data.BingoGameData.PRELIT_PORTALS_OFF;
        return switch (mode) {
            case NETHER -> com.jamie.jamiebingo.data.BingoGameData.PRELIT_PORTALS_NETHER;
            case END -> com.jamie.jamiebingo.data.BingoGameData.PRELIT_PORTALS_END;
            case BOTH -> com.jamie.jamiebingo.data.BingoGameData.PRELIT_PORTALS_BOTH;
            default -> com.jamie.jamiebingo.data.BingoGameData.PRELIT_PORTALS_OFF;
        };
    }

    private String registerLabel() {
        return "Register: " + switch (registerMode) {
            case ALWAYS_HAVE -> "Always";
            case COLLECT_ONCE -> "Once";
            case RANDOM -> "Rand";
        };
    }

    private void cycleRegisterForward() {
        registerMode = registerMode.next();
    }

    private void cycleRegisterBackward() {
        registerMode = switch (registerMode) {
            case COLLECT_ONCE -> RegisterMode.RANDOM;
            case RANDOM -> RegisterMode.ALWAYS_HAVE;
            case ALWAYS_HAVE -> RegisterMode.COLLECT_ONCE;
        };
    }

    private String teamSyncLabel() {
        return "TeamSync: " + (teamSyncEnabled ? "On" : "Off");
    }

    private void toggleTeamSync() {
        teamSyncEnabled = !teamSyncEnabled;
    }

    private String teamChestLabel() {
        return "TeamChest: " + (teamChestEnabled ? "On" : "Off");
    }

    private void toggleTeamChest() {
        teamChestEnabled = !teamChestEnabled;
    }

    private void cycleStarterKitForward() {
        starterKitMode = switch (starterKitMode) {
            case DISABLED -> StarterKitMode.MINIMAL;
            case MINIMAL -> StarterKitMode.AVERAGE;
            case AVERAGE -> StarterKitMode.OP;
            case OP -> StarterKitMode.DISABLED;
        };
    }

    private void cycleStarterKitBackward() {
        starterKitMode = switch (starterKitMode) {
            case DISABLED -> StarterKitMode.OP;
            case OP -> StarterKitMode.AVERAGE;
            case AVERAGE -> StarterKitMode.MINIMAL;
            case MINIMAL -> StarterKitMode.DISABLED;
        };
    }

    private String starterKitLabel() {
        return "Starter Kit: " + switch (starterKitMode) {
            case MINIMAL -> "Minimal";
            case AVERAGE -> "Average";
            case OP -> "OP";
            default -> "Disabled";
        };
    }

    private void toggleHideGoalDetailsInChat() {
        hideGoalDetailsInChat = !hideGoalDetailsInChat;
    }

    private String hideGoalDetailsLabel() {
        return "Hide Goal Details: " + (hideGoalDetailsInChat ? "On" : "Off");
    }

    private void toggleAllowLateJoin() {
        allowLateJoin = !allowLateJoin;
    }

    private String allowLateJoinLabel() {
        return "Late: " + (allowLateJoin ? "On" : "Off");
    }

    private void addPlusMinus(int x, int y, Runnable plus, Runnable minus, LabelSupplier label) {
        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(com.jamie.jamiebingo.util.ComponentUtil.literal("+"), b -> {
                    plus.run();
                    rebuild();
                })
                .pos(x, y)
                .size(20, 20)
                .build());
        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(com.jamie.jamiebingo.util.ComponentUtil.literal(label.get()), b -> {})
                .pos(x + 24, y)
                .size(30, 20)
                .build());
        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(com.jamie.jamiebingo.util.ComponentUtil.literal("-"), b -> {
                    minus.run();
                    rebuild();
                })
                .pos(x + 58, y)
                .size(20, 20)
                .build());
    }

    private void randomizeCurrentPage() {
        if (settingsPage == 0) {
            randomizeGamemodePage();
        } else {
            randomizeExtraPage();
        }
    }

    private void resetCurrentPageToDefaults() {
        if (settingsPage == 0) {
            resetGamemodePageToDefaults();
        } else {
            resetExtraPageToDefaults();
        }
    }

    private void randomizeGamemodePage() {
        randomMode = true;
        randomGunRounds = true;
        gunRounds = 8;
        hangmanTimeRandom = true;
        hangmanSeconds = 120;
        hangmanPenaltySeconds = 60;

        cardDifficulty = DifficultyMode.RANDOM;
        gameDifficulty = DifficultyMode.RANDOM;

        randomCardSize = true;
        cardSize = 5;
        rushMode = ToggleMode.RANDOM;
        rushSeconds = 60;
        minesMode = ToggleMode.RANDOM;
        mineAmount = 1;
        mineTimeSeconds = 60;
        powerSlotMode = ToggleMode.RANDOM;
        powerSlotIntervalSeconds = 60;
        fakeRerollsMode = ToggleMode.RANDOM;
        fakeRerollsPerPlayer = 2;
        shuffleMode = ShuffleMode.RANDOM;

        rerollMode = RerollMode.RANDOM;
        effectsMode = EffectsMode.RANDOM;
    }

    private void randomizeExtraPage() {
        rtpMode = ToggleMode.RANDOM;
        pvpMode = ToggleMode.RANDOM;
        hostileMobsMode = ToggleMode.RANDOM;
        hungerMode = ToggleMode.RANDOM;
        naturalRegenRandom = true;
        naturalRegenEnabled = true;
        keepInventoryMode = ToggleMode.RANDOM;
        hardcoreMode = ToggleMode.RANDOM;
        daylightMode = DaylightMode.RANDOM;
        registerMode = RegisterMode.RANDOM;
    }

    private void resetGamemodePageToDefaults() {
        mode = DEFAULT_MODE;
        randomMode = false;
        randomGunRounds = false;
        gunRounds = 8;
        hangmanRounds = 5;
        hangmanTimeRandom = false;
        hangmanSeconds = 120;
        hangmanPenaltySeconds = 60;
        cardDifficulty = DifficultyMode.NORMAL;
        gameDifficulty = DifficultyMode.NORMAL;
        randomCardSize = false;
        cardSize = 5;
        quests = DEFAULT_QUESTS;
        questPercent = 35;
        casino = false;
        casinoMode = CasinoMode.DISABLED;
        rerollMode = RerollMode.DISABLED;
        rerollsValue = 3;
        fakeRerollsMode = DEFAULT_FAKE_REROLLS_ENABLED ? ToggleMode.ENABLED : ToggleMode.DISABLED;
        fakeRerollsPerPlayer = DEFAULT_FAKE_REROLLS_PER_PLAYER;
        shuffleMode = ShuffleMode.DISABLED;
        rushMode = ToggleMode.DISABLED;
        rushSeconds = 60;
        minesMode = ToggleMode.DISABLED;
        mineAmount = 1;
        mineTimeSeconds = 15;
        powerSlotMode = ToggleMode.DISABLED;
        powerSlotIntervalSeconds = 60;
        effectsMode = EffectsMode.DISABLED;
        effectsInterval = 60;
        delayEnabled = false;
        delaySeconds = 10;
    }

    private void resetExtraPageToDefaults() {
        categoryLogicEnabled = DEFAULT_CATEGORY_LOGIC;
        rarityLogicEnabled = true;
        itemColorVariantsSeparate = DEFAULT_ITEM_COLORS_SEPARATE;
        countdownEnabled = false;
        countdownMinutes = 10;
        registerMode = RegisterMode.COLLECT_ONCE;
        teamSyncEnabled = DEFAULT_TEAM_SYNC;
        teamChestEnabled = DEFAULT_TEAM_CHEST;
        rtpMode = ToggleMode.DISABLED;
        pvpMode = ToggleMode.ENABLED;
        hostileMobsMode = ToggleMode.ENABLED;
        hungerMode = ToggleMode.ENABLED;
        naturalRegenEnabled = true;
        naturalRegenRandom = false;
        keepInventoryMode = ToggleMode.DISABLED;
        hardcoreMode = ToggleMode.DISABLED;
        daylightMode = DaylightMode.ENABLED;
        allowLateJoin = DEFAULT_ALLOW_LATE_JOIN;
        starterKitMode = StarterKitMode.DISABLED;
        hideGoalDetailsInChat = false;
        adventureMode = false;
        prelitPortalsMode = PrelitPortalsMode.OFF;
    }

    private void randomizeAll() {
        randomizeGamemodePage();
        randomizeExtraPage();
    }

    private CasinoMode parseCasinoMode(int mode, boolean legacyCasinoEnabled) {
        return switch (mode) {
            case 1 -> CasinoMode.ENABLED;
            case 2 -> CasinoMode.DRAFT;
            default -> legacyCasinoEnabled ? CasinoMode.ENABLED : CasinoMode.DISABLED;
        };
    }

    private int toCasinoModeValue(CasinoMode mode) {
        return switch (mode) {
            case ENABLED -> 1;
            case DRAFT -> 2;
            default -> 0;
        };
    }

    private void cycleCasinoModeForward() {
        casinoMode = switch (casinoMode) {
            case DISABLED -> CasinoMode.ENABLED;
            case ENABLED -> CasinoMode.DRAFT;
            case DRAFT -> CasinoMode.DISABLED;
        };
    }

    private void cycleCasinoModeBackward() {
        casinoMode = switch (casinoMode) {
            case DISABLED -> CasinoMode.DRAFT;
            case DRAFT -> CasinoMode.ENABLED;
            case ENABLED -> CasinoMode.DISABLED;
        };
    }

    private String casinoLabel() {
        return "Casino: " + switch (casinoMode) {
            case DISABLED -> "Off";
            case ENABLED -> "On";
            case DRAFT -> "Draft";
        };
    }

    private ShuffleMode parseShuffleMode(int mode) {
        return switch (mode) {
            case com.jamie.jamiebingo.data.BingoGameData.SHUFFLE_ENABLED -> ShuffleMode.ENABLED;
            case com.jamie.jamiebingo.data.BingoGameData.SHUFFLE_RANDOM -> ShuffleMode.RANDOM;
            default -> ShuffleMode.DISABLED;
        };
    }

    private StarterKitMode parseStarterKitMode(int mode) {
        return switch (mode) {
            case com.jamie.jamiebingo.data.BingoGameData.STARTER_KIT_MINIMAL -> StarterKitMode.MINIMAL;
            case com.jamie.jamiebingo.data.BingoGameData.STARTER_KIT_AVERAGE -> StarterKitMode.AVERAGE;
            case com.jamie.jamiebingo.data.BingoGameData.STARTER_KIT_OP -> StarterKitMode.OP;
            default -> StarterKitMode.DISABLED;
        };
    }

    private int toShuffleModeValue(ShuffleMode mode) {
        return switch (mode) {
            case ENABLED -> com.jamie.jamiebingo.data.BingoGameData.SHUFFLE_ENABLED;
            case RANDOM -> com.jamie.jamiebingo.data.BingoGameData.SHUFFLE_RANDOM;
            default -> com.jamie.jamiebingo.data.BingoGameData.SHUFFLE_DISABLED;
        };
    }

    private int toStarterKitModeValue(StarterKitMode mode) {
        return switch (mode) {
            case MINIMAL -> com.jamie.jamiebingo.data.BingoGameData.STARTER_KIT_MINIMAL;
            case AVERAGE -> com.jamie.jamiebingo.data.BingoGameData.STARTER_KIT_AVERAGE;
            case OP -> com.jamie.jamiebingo.data.BingoGameData.STARTER_KIT_OP;
            default -> com.jamie.jamiebingo.data.BingoGameData.STARTER_KIT_DISABLED;
        };
    }

    private boolean shuffleAllowedForMode(WinCondition value) {
        return value == WinCondition.FULL || value == WinCondition.LOCKOUT || value == WinCondition.RARITY;
    }

    private void cycleShuffleModeForward() {
        shuffleMode = switch (shuffleMode) {
            case DISABLED -> ShuffleMode.ENABLED;
            case ENABLED -> ShuffleMode.RANDOM;
            case RANDOM -> ShuffleMode.DISABLED;
        };
    }

    private void cycleShuffleModeBackward() {
        shuffleMode = switch (shuffleMode) {
            case DISABLED -> ShuffleMode.RANDOM;
            case RANDOM -> ShuffleMode.ENABLED;
            case ENABLED -> ShuffleMode.DISABLED;
        };
    }

    private String shuffleLabel() {
        return "Shuffle: " + switch (shuffleMode) {
            case ENABLED -> "On";
            case RANDOM -> "Rand";
            case DISABLED -> "Off";
        };
    }

    private void cycleMinesModeForward() {
        minesMode = switch (minesMode) {
            case DISABLED -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.DISABLED;
        };
    }

    private void cycleMinesModeBackward() {
        minesMode = switch (minesMode) {
            case DISABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.DISABLED;
        };
    }

    private String minesLabel() {
        return switch (minesMode) {
            case DISABLED -> "Mines: Off";
            case ENABLED -> "Mines: On";
            case RANDOM -> "Mines: Rand";
        };
    }

    private void cyclePowerSlotModeForward() {
        powerSlotMode = switch (powerSlotMode) {
            case DISABLED -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.DISABLED;
        };
    }

    private void cyclePowerSlotModeBackward() {
        powerSlotMode = switch (powerSlotMode) {
            case DISABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.DISABLED;
        };
    }

    private String powerSlotLabel() {
        return switch (powerSlotMode) {
            case DISABLED -> "Power Slot: Off";
            case ENABLED -> "Power Slot: On";
            case RANDOM -> "Power Slot: Rand";
        };
    }

    private void cycleFakeRerollsModeForward() {
        fakeRerollsMode = switch (fakeRerollsMode) {
            case DISABLED -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.DISABLED;
        };
    }

    private void cycleFakeRerollsModeBackward() {
        fakeRerollsMode = switch (fakeRerollsMode) {
            case DISABLED -> ToggleMode.RANDOM;
            case RANDOM -> ToggleMode.ENABLED;
            case ENABLED -> ToggleMode.DISABLED;
        };
    }

    private String fakeRerollsLabel() {
        return switch (fakeRerollsMode) {
            case DISABLED -> "Fake Rerolls: Off";
            case ENABLED -> "Fake Rerolls: On";
            case RANDOM -> "Fake Rerolls: Rand";
        };
    }

    private interface LabelSupplier {
        String get();
    }

    private WinCondition pickFairModeExcludingRandom() {
        WinCondition[] values = WinCondition.values();

        // Fair pick among all values except the special "RANDOM" entry if it exists.
        // (If your enum doesn't contain RANDOM, this still works.)
        int tries = 0;
        while (tries++ < 50) {
            WinCondition chosen = values[rng.nextInt(values.length)];
            if (!"RANDOM".equalsIgnoreCase(chosen.name())) {
                return chosen;
            }
        }
        return WinCondition.LINE;
    }

    // Gaussian-like weights around "center"
    private int pickWeightedInt(int min, int max, int center) {
        return pickWeightedInt(min, max, center, 0.06);
    }

    private int pickWeightedInt(int min, int max, int center, double falloff) {
        double total = 0;
        double[] weights = new double[max - min + 1];

        for (int v = min; v <= max; v++) {
            double w = Math.exp(-falloff * (v - center) * (v - center));
            weights[v - min] = w;
            total += w;
        }

        double r = rng.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            r -= weights[i];
            if (r <= 0) return min + i;
        }
        return center;
    }

    private int pickRandomEffectInterval() {
        double value = 40 + rng.nextGaussian() * 35.0;
        int clamped = (int) Math.round(Math.max(20, Math.min(300, value)));
        return clamped;
    }

    private DifficultyMode parseDifficulty(String s) {
        if (s == null) return DifficultyMode.NORMAL;
        String key = s.toLowerCase();
        if (key.startsWith(CUSTOM_DIFFICULTY_PREFIX)) {
            return DifficultyMode.CUSTOM;
        }
        return switch (key) {
            case "easy" -> DifficultyMode.EASY;
            case "hard" -> DifficultyMode.HARD;
            case "extreme" -> DifficultyMode.EXTREME;
            case "random" -> DifficultyMode.RANDOM;
            default -> DifficultyMode.NORMAL;
        };
    }

    private void cycleCardDifficultyForward() {
        cardDifficulty = cardDifficulty.nextCard();
    }

    private void cycleCardDifficultyBackward() {
        DifficultyMode[] values = DifficultyMode.values();
        int index = 0;
        for (; index < values.length; index++) {
            if (values[index] == cardDifficulty) break;
        }
        int prev = index - 1;
        cardDifficulty = prev < 0 ? values[values.length - 1] : values[prev];
    }

    private void cycleGameDifficultyForward() {
        gameDifficulty = gameDifficulty.nextGame();
    }

    private void cycleGameDifficultyBackward() {
        DifficultyMode[] values = new DifficultyMode[] {
                DifficultyMode.EASY,
                DifficultyMode.NORMAL,
                DifficultyMode.HARD,
                DifficultyMode.RANDOM
        };
        int index = 0;
        for (; index < values.length; index++) {
            if (values[index] == gameDifficulty) break;
        }
        int prev = index - 1;
        gameDifficulty = prev < 0 ? values[values.length - 1] : values[prev];
    }

    private String pickCardDifficulty() {
        int r = rng.nextInt(3);
        return r == 0 ? "easy" : (r == 1 ? "normal" : "hard");
    }

    private String pickGameDifficulty() {
        int r = rng.nextInt(3);
        return r == 0 ? "easy" : (r == 1 ? "normal" : "hard");
    }

    private CardComposition normalizeQuests(CardComposition in) {
        return (in == CardComposition.HYBRID_CATEGORY || in == CardComposition.HYBRID_PERCENT)
                ? in
                : CardComposition.CLASSIC_ONLY;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private boolean isCustomDifficultyString(String s) {
        return s != null && s.toLowerCase().startsWith(CUSTOM_DIFFICULTY_PREFIX);
    }

    private void applyCustomWeightsFromString(String s) {
        if (s == null) return;
        String raw = s.toLowerCase();
        if (!raw.startsWith(CUSTOM_DIFFICULTY_PREFIX)) return;
        String values = raw.substring(CUSTOM_DIFFICULTY_PREFIX.length());
        String[] parts = values.split(",");
        if (parts.length < 6) return;
        try {
            customCommon = clamp(Integer.parseInt(parts[0].trim()), 0, 100);
            customUncommon = clamp(Integer.parseInt(parts[1].trim()), 0, 100);
            customRare = clamp(Integer.parseInt(parts[2].trim()), 0, 100);
            customEpic = clamp(Integer.parseInt(parts[3].trim()), 0, 100);
            customLegendary = clamp(Integer.parseInt(parts[4].trim()), 0, 100);
            customMythic = clamp(Integer.parseInt(parts[5].trim()), 0, 100);
        } catch (Exception ignored) {
        }
    }

    private String buildCustomDifficultyString() {
        return CUSTOM_DIFFICULTY_PREFIX +
                clamp(customCommon, 0, 100) + "," +
                clamp(customUncommon, 0, 100) + "," +
                clamp(customRare, 0, 100) + "," +
                clamp(customEpic, 0, 100) + "," +
                clamp(customLegendary, 0, 100) + "," +
                clamp(customMythic, 0, 100);
    }

    private String buildCustomDifficultyStringNormalized() {
        int[] weights = new int[] {
                clamp(customCommon, 0, 100),
                clamp(customUncommon, 0, 100),
                clamp(customRare, 0, 100),
                clamp(customEpic, 0, 100),
                clamp(customLegendary, 0, 100),
                clamp(customMythic, 0, 100)
        };
        int sum = 0;
        for (int w : weights) sum += w;
        if (sum <= 0) {
            weights[0] = 100;
            for (int i = 1; i < weights.length; i++) weights[i] = 0;
        } else if (sum != 100) {
            double scale = 100.0 / sum;
            int running = 0;
            int maxIdx = 0;
            int maxVal = -1;
            for (int i = 0; i < weights.length; i++) {
                int scaled = (int) Math.round(weights[i] * scale);
                weights[i] = scaled;
                running += scaled;
                if (scaled > maxVal) {
                    maxVal = scaled;
                    maxIdx = i;
                }
            }
            if (running != 100) {
                weights[maxIdx] = clamp(weights[maxIdx] + (100 - running), 0, 100);
            }
        }
        return CUSTOM_DIFFICULTY_PREFIX +
                weights[0] + "," +
                weights[1] + "," +
                weights[2] + "," +
                weights[3] + "," +
                weights[4] + "," +
                weights[5];
    }

    private void setCustomWeight(int index, int value) {
        int clamped = clamp(value, 0, 100);
        switch (index) {
            case 0 -> customCommon = clamped;
            case 1 -> customUncommon = clamped;
            case 2 -> customRare = clamped;
            case 3 -> customEpic = clamped;
            case 4 -> customLegendary = clamped;
            case 5 -> customMythic = clamped;
            default -> {}
        }
        int sum = customCommon + customUncommon + customRare + customEpic + customLegendary + customMythic;
        if (sum > 100) {
            normalizeCustomWeights(index);
        }
        rebuild();
    }

    private void normalizeCustomWeights(int lockedIndex) {
        int[] weights = new int[] {
                customCommon, customUncommon, customRare, customEpic, customLegendary, customMythic
        };
        int locked = lockedIndex >= 0 && lockedIndex < weights.length ? weights[lockedIndex] : -1;
        if (lockedIndex >= 0 && locked > 100) {
            weights[lockedIndex] = 100;
            locked = 100;
        }

        int sum = 0;
        for (int w : weights) sum += w;
        if (sum == 100) {
            applyWeights(weights);
            return;
        }

        if (lockedIndex >= 0) {
            int otherSum = sum - weights[lockedIndex];
            int targetOther = Math.max(0, 100 - weights[lockedIndex]);
            if (otherSum <= 0) {
                for (int i = 0; i < weights.length; i++) {
                    if (i != lockedIndex) weights[i] = 0;
                }
            } else {
                double scale = targetOther / (double) otherSum;
                int running = weights[lockedIndex];
                int maxIdx = -1;
                int maxVal = -1;
                for (int i = 0; i < weights.length; i++) {
                    if (i == lockedIndex) continue;
                    int scaled = (int) Math.round(weights[i] * scale);
                    weights[i] = scaled;
                    running += scaled;
                    if (scaled > maxVal) {
                        maxVal = scaled;
                        maxIdx = i;
                    }
                }
                if (running != 100 && maxIdx >= 0) {
                    weights[maxIdx] = clamp(weights[maxIdx] + (100 - running), 0, 100);
                }
            }
        } else {
            if (sum <= 0) {
                weights[0] = 100;
                for (int i = 1; i < weights.length; i++) weights[i] = 0;
            } else {
                double scale = 100.0 / sum;
                int running = 0;
                int maxIdx = 0;
                int maxVal = -1;
                for (int i = 0; i < weights.length; i++) {
                    int scaled = (int) Math.round(weights[i] * scale);
                    weights[i] = scaled;
                    running += scaled;
                    if (scaled > maxVal) {
                        maxVal = scaled;
                        maxIdx = i;
                    }
                }
                if (running != 100) {
                    weights[maxIdx] = clamp(weights[maxIdx] + (100 - running), 0, 100);
                }
            }
        }
        applyWeights(weights);
    }

    private void applyWeights(int[] weights) {
        if (weights == null || weights.length < 6) return;
        customCommon = clamp(weights[0], 0, 100);
        customUncommon = clamp(weights[1], 0, 100);
        customRare = clamp(weights[2], 0, 100);
        customEpic = clamp(weights[3], 0, 100);
        customLegendary = clamp(weights[4], 0, 100);
        customMythic = clamp(weights[5], 0, 100);
    }

    private static final class IntSliderWidget extends AbstractWidget {
        private final int min;
        private final int max;
        private final String label;
        private final String suffix;
        private final IntConsumer onChange;
        private int value;
        private boolean dragging;

        private IntSliderWidget(
                int x, int y, int width, int height,
                int min, int max, int value,
                String label, String suffix, IntConsumer onChange
        ) {
            super(x, y, width, height, com.jamie.jamiebingo.util.ComponentUtil.empty());
            this.min = min;
            this.max = Math.max(min, max);
            this.label = label;
            this.suffix = suffix;
            this.onChange = onChange;
            this.value = Math.max(this.min, Math.min(this.max, value));
            refreshMessage();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();

            int bg = this.active ? 0xFF2A2E35 : 0xFF1A1D22;
            graphics.fill(x, y, x + w, y + h, bg);
            graphics.fill(x, y, x + w, y + 1, 0x66FFFFFF);
            graphics.fill(x, y + h - 1, x + w, y + h, 0x44000000);

            int pad = 6;
            int trackX = x + pad;
            int trackW = Math.max(1, w - pad * 2);
            int trackY = y + h - 7;
            graphics.fill(trackX, trackY, trackX + trackW, trackY + 3, 0xFF101318);

            double t = normalized();
            int fillW = (int) Math.round(trackW * t);
            graphics.fill(trackX, trackY, trackX + fillW, trackY + 3, 0xFF5AA9E6);

            int knobX = trackX + (int) Math.round(trackW * t);
            graphics.fill(knobX - 2, trackY - 3, knobX + 2, trackY + 6, 0xFFE8EEF6);

            graphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    getMessage(),
                    x + w / 2,
                    y + 6,
                    0xFFFFFFFF
            );
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean isNew) {
            super.onClick(event, isNew);
            if (event.button() != 0) return;
            dragging = true;
            setFromMouse(event.x());
        }

        @Override
        protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
            super.onDrag(event, dragX, dragY);
            if (!dragging || event.button() != 0) return;
            setFromMouse(event.x());
        }

        @Override
        public void onRelease(MouseButtonEvent event) {
            super.onRelease(event);
            dragging = false;
        }

        private void setFromMouse(double mouseX) {
            int pad = 6;
            int trackX = getX() + pad;
            int trackW = Math.max(1, getWidth() - pad * 2);
            double t = (mouseX - trackX) / (double) trackW;
            t = Math.max(0.0d, Math.min(1.0d, t));
            int next = min + (int) Math.round((max - min) * t);
            setIntValue(next);
        }

        private void setIntValue(int next) {
            int clamped = Math.max(min, Math.min(max, next));
            if (clamped != this.value) {
                this.value = clamped;
                onChange.accept(clamped);
            }
            refreshMessage();
        }

        private double normalized() {
            if (max <= min) return 0.0d;
            return (value - min) / (double) (max - min);
        }

        private void refreshMessage() {
            setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(label + value + suffix));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }

    private int computeContentHeight(boolean showRerolls) {
        int y = 0;
        y += 26; // mode
        if (mode == WinCondition.HANGMAN) {
            y += 26; // hangman rounds
            y += 26; // hangman time
            y += 26; // hangman penalty
        }
        y += 26; // card size
        y += 26; // quests
        if (quests == CardComposition.HYBRID_PERCENT) {
            y += 26; // quest percent
        }
        y += 26; // category logic
        y += 26; // rarity logic
        y += 26; // item color variants
        y += 26; // effects
        y += 26; // rtp
        y += 26; // pvp
        y += 26; // adventure mode
        y += 26; // hostile mobs
        y += 26; // hunger
        y += 26; // keep inventory
        y += 26; // hardcore
        y += 26; // daylight
        y += 26; // register
        y += 26; // team sync
        y += 26; // allow late join
        y += 26; // delay
        y += 26; // countdown
        y += 26; // rush
        y += 26; // card difficulty
        if (cardDifficulty == DifficultyMode.CUSTOM) {
            y += 26 * 6; // custom rarity sliders
        }
        y += 26; // game difficulty
        y += 26; // casino
        if (showRerolls) {
            y += 26; // rerolls
        }
        y += 26; // shuffle
        return y;
    }
}
























