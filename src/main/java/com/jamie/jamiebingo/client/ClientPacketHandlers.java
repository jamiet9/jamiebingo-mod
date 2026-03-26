package com.jamie.jamiebingo.client;


import com.jamie.jamiebingo.client.ClientMinecraftUtil;
import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import com.jamie.jamiebingo.client.screen.CasinoBingoScreen;
import com.jamie.jamiebingo.client.screen.BingoControllerScreen;
import com.jamie.jamiebingo.client.screen.BingoWorldSettingsScreen;
import com.jamie.jamiebingo.menu.ControllerSettingsSnapshot;
import com.jamie.jamiebingo.client.screen.CustomCardMakerScreen;
import com.jamie.jamiebingo.client.screen.BlacklistItemsQuestScreen;
import com.jamie.jamiebingo.client.screen.CardLayoutConfiguratorScreen;
import com.jamie.jamiebingo.client.screen.RarityChangerScreen;
import com.jamie.jamiebingo.client.screen.WeeklyChallengeScreen;
import com.jamie.jamiebingo.network.PacketCasinoEnd;
import com.jamie.jamiebingo.network.PacketCasinoShutdown;
import com.jamie.jamiebingo.network.PacketCasinoStart;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class ClientPacketHandlers {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ClientPacketHandlers() {
    }

    public static void handleCasinoStart(PacketCasinoStart msg) {
        LOGGER.info("[CLIENT] Casino START packet received (grid={})", msg.gridSize);

        com.jamie.jamiebingo.client.render.GlobalWallScreenRenderer.forceDeactivate();
        ClientCasinoState.start(msg.gridSize);

        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        if (mc == null) return;

        ClientMinecraftUtil.setScreen(mc, null);
        ClientMinecraftUtil.setScreen(mc, new CasinoBingoScreen());
    }

    public static void handleCasinoEnd(PacketCasinoEnd msg) {
        if (!ClientCasinoState.isActive()) {
            return;
        }

        ClientCasinoState.beginFinishing();

        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        if (!(ClientMinecraftUtil.getScreen(mc) instanceof CasinoBingoScreen)) {
            ClientMinecraftUtil.setScreen(mc, new CasinoBingoScreen());
        }
    }

    public static void handleCasinoShutdown(PacketCasinoShutdown msg) {
        ClientCasinoState.end();
        com.jamie.jamiebingo.client.render.GlobalWallScreenRenderer.forceDeactivate();

        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        if (ClientMinecraftUtil.getScreen(mc) instanceof CasinoBingoScreen) {
            ClientMinecraftUtil.setScreen(mc, null);
        }
    }

    public static void handleEffectApplied(Component effectName) {
        String name = "Random Effect";
        if (effectName != null && !effectName.getString().isBlank()) {
            name = effectName.getString();
        }

        OverlayRenderer.randomEffectText = "Effect applied: " + name;
        OverlayRenderer.randomEffectTicksRemaining = 60;

        if (ClientMinecraftUtil.getPlayer() != null) {
            ClientMinecraftUtil.getPlayer().playSound(
                    SoundEvents.BEACON_ACTIVATE,
                    0.8f,
                    1.0f
            );
        }
    }

    public static void handleTeamSound(String soundId, float volume, float pitch) {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        var player = ClientMinecraftUtil.getPlayer(mc);
        if (mc == null || player == null) return;
        if (soundId == null || soundId.isBlank()) return;
        try {
            SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(com.jamie.jamiebingo.util.IdUtil.id(soundId));
            if (sound != null) {
                player.playSound(sound, volume * 12.0f, pitch);
            } else {
                LOGGER.warn("[JamieBingo] Missing sound event on client: {}", soundId);
            }
        } catch (Exception ignored) {
        }
    }

    public static void handleChatIconMessage(Component message, String slotId) {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        var gui = ClientMinecraftUtil.getGui(mc);
        if (mc == null || gui == null) return;
        if (message != null) {
            gui.getChat().addMessage(message);
        }
        if (slotId != null && !slotId.isBlank()) {
            ClientChatIconOverlay.recordSlotId(slotId);
        }
    }

    public static void handleEffectCountdown(
            String effectName,
            int amplifier,
            int secondsRemaining,
            boolean playSound
    ) {
        String name =
                (effectName == null || effectName.isBlank())
                        ? "Random Effect"
                        : effectName;

        String roman = toRoman(amplifier + 1);

        OverlayRenderer.randomEffectText =
                "Effect " + name + " " + roman +
                        " applying in " + secondsRemaining + "s";

        OverlayRenderer.randomEffectTicksRemaining =
                secondsRemaining * 20;

        if (playSound && ClientMinecraftUtil.getPlayer() != null) {
            ClientMinecraftUtil.getPlayer().playSound(
                    SoundEvents.NOTE_BLOCK_PLING.value(),
                    1.0f,
                    1.2f
            );
        }
    }

    public static void handleStartCountdown(int seconds) {
        if (seconds <= 0) {
            boolean autoClose = ClientStartCountdown.wasAutoOpenedFullscreenCard();
            boolean wasActive = ClientStartCountdown.isActive();
            ClientStartCountdown.clear();
            OverlayRenderer.randomEffectText = "";
            OverlayRenderer.randomEffectTicksRemaining = 0;
            com.jamie.jamiebingo.client.render.GlobalWallScreenRenderer.forceDeactivate();
            com.jamie.jamiebingo.client.casino.ClientCasinoState.end();
            Minecraft mc = ClientMinecraftUtil.getMinecraft();
            if (mc != null && ClientMinecraftUtil.getScreen(mc) instanceof CasinoBingoScreen) {
                ClientMinecraftUtil.setScreen(mc, null);
            }
            if (mc != null && BingoCardScreen.isFullscreenCardScreen(ClientMinecraftUtil.getScreen(mc))) {
                BingoCardScreen.closeFullscreen();
            }
            return;
        }

        ClientStartCountdown.start(seconds);
        if (!ClientCasinoState.isActive() && ClientCardData.hasCard()) {
            Minecraft mc = ClientMinecraftUtil.getMinecraft();
            if (mc != null && !BingoCardScreen.isFullscreenCardScreen(ClientMinecraftUtil.getScreen(mc))) {
                BingoCardScreen.openFullscreen(ClientCardData.getCard());
                ClientStartCountdown.setAutoOpenedFullscreenCard(true);
            } else {
                ClientStartCountdown.setAutoOpenedFullscreenCard(true);
            }
        }
    }

    public static void handleOpenBingoController(
            com.jamie.jamiebingo.bingo.WinCondition win,
            com.jamie.jamiebingo.bingo.CardComposition composition,
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
        if (ClientCasinoState.isActive()) {
            return;
        }
        ClientGameState.teamChestEnabled = teamChestEnabled;
        try {
            BingoControllerScreen.openWithState(
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
        } catch (Throwable t) {
            LOGGER.error("[JamieBingo] openWithState failed (BingoController)", t);
        }
        try {
            net.minecraft.client.Minecraft mc = com.jamie.jamiebingo.client.ClientMinecraftUtil.getMinecraft();
            net.minecraft.client.gui.screens.Screen screen = new BingoControllerScreen();
            com.jamie.jamiebingo.client.ClientMinecraftUtil.setScreen(mc, screen);
        } catch (Throwable t) {
            LOGGER.error("[JamieBingo] Failed to open BingoController screen", t);
        }
    }

    public static void handleOpenCustomCardMaker(
            com.jamie.jamiebingo.bingo.WinCondition win,
            com.jamie.jamiebingo.bingo.CardComposition composition,
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
            boolean teamSyncEnabled,
            int shuffleMode,
            boolean minesEnabled,
            int mineAmount,
            int mineTimeSeconds,
            boolean customCardEnabled,
            boolean customPoolEnabled,
            java.util.List<String> customCardSlots,
            java.util.List<String> customPoolIds,
            java.util.List<String> customMineIds
    ) {
        try {
            ControllerSettingsSnapshot snapshot = new ControllerSettingsSnapshot(
                    win,
                    composition == com.jamie.jamiebingo.bingo.CardComposition.HYBRID_CATEGORY ? 1 :
                            composition == com.jamie.jamiebingo.bingo.CardComposition.HYBRID_PERCENT ? 2 : 0,
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
                    randomCardDifficulty ? "random" : cardDifficulty,
                    randomGameDifficulty ? "random" : gameDifficulty,
                    effectsInterval,
                    rtpEnabled,
                    randomRtp,
                    hostileMobsEnabled,
                    randomHostileMobs,
                    hungerEnabled,
                    true,
                    false,
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
                    false,
                    teamSyncEnabled,
                    true,
                    shuffleMode,
                    com.jamie.jamiebingo.data.BingoGameData.STARTER_KIT_DISABLED,
                    false,
                    minesEnabled,
                    mineAmount,
                    mineTimeSeconds,
                    false,
                    60,
                    false,
                    2
            );

            ClientCustomCardState.setState(
                    snapshot,
                    customCardEnabled,
                    customPoolEnabled,
                    customCardSlots,
                    customPoolIds,
                    customMineIds
            );
        } catch (Throwable t) {
            LOGGER.error("[JamieBingo] Failed to prepare CustomCardMaker state", t);
        }
        try {
            net.minecraft.client.Minecraft mc = com.jamie.jamiebingo.client.ClientMinecraftUtil.getMinecraft();
            net.minecraft.client.gui.screens.Screen screen = new CustomCardMakerScreen();
            com.jamie.jamiebingo.client.ClientMinecraftUtil.setScreen(mc, screen);
        } catch (Throwable t) {
            LOGGER.error("[JamieBingo] Failed to open CustomCardMaker screen", t);
        }
    }

    public static void handleOpenCardLayoutConfigurator() {
        try {
            net.minecraft.client.Minecraft mc = com.jamie.jamiebingo.client.ClientMinecraftUtil.getMinecraft();
            net.minecraft.client.gui.screens.Screen screen = new CardLayoutConfiguratorScreen();
            com.jamie.jamiebingo.client.ClientMinecraftUtil.setScreen(mc, screen);
        } catch (Throwable t) {
            LOGGER.error("[JamieBingo] Failed to open CardLayoutConfigurator screen", t);
        }
    }

    public static void handleOpenBlacklistMenu() {
        try {
            net.minecraft.client.Minecraft mc = com.jamie.jamiebingo.client.ClientMinecraftUtil.getMinecraft();
            net.minecraft.client.gui.screens.Screen screen = new BlacklistItemsQuestScreen();
            com.jamie.jamiebingo.client.ClientMinecraftUtil.setScreen(mc, screen);
        } catch (Throwable t) {
            LOGGER.error("[JamieBingo] Failed to open blacklist screen", t);
        }
    }

    public static void handleOpenRarityChanger() {
        try {
            net.minecraft.client.Minecraft mc = com.jamie.jamiebingo.client.ClientMinecraftUtil.getMinecraft();
            com.jamie.jamiebingo.client.ClientMinecraftUtil.setScreen(mc, new RarityChangerScreen());
        } catch (Throwable t) {
            LOGGER.error("[JamieBingo] Failed to open rarity changer screen", t);
        }
    }

    public static void handleOpenWorldSettings(
            boolean newSeedEachGame,
            int worldTypeMode,
            int worldCustomBiomeSizeBlocks,
            int worldTerrainHillinessPercent,
            int worldStructureFrequencyPercent,
            String singleBiomeId,
            boolean surfaceCaveBiomes,
            String setSeedText,
            boolean adventureMode,
            int prelitPortalsMode
    ) {
        try {
            BingoWorldSettingsScreen.openWithState(
                    newSeedEachGame,
                    worldTypeMode,
                    worldCustomBiomeSizeBlocks,
                    worldTerrainHillinessPercent,
                    worldStructureFrequencyPercent,
                    singleBiomeId,
                    surfaceCaveBiomes,
                    setSeedText,
                    adventureMode,
                    prelitPortalsMode
            );
            net.minecraft.client.Minecraft mc = com.jamie.jamiebingo.client.ClientMinecraftUtil.getMinecraft();
            com.jamie.jamiebingo.client.ClientMinecraftUtil.setScreen(mc, new BingoWorldSettingsScreen());
        } catch (Throwable t) {
            LOGGER.error("[JamieBingo] Failed to open world settings screen", t);
        }
    }

    public static void handleOpenWeeklyChallenge() {
        try {
            WeeklyChallengeClient.refresh();
        } catch (Throwable t) {
            LOGGER.error("[JamieBingo] Failed to refresh weekly challenge state", t);
        }
        try {
            net.minecraft.client.Minecraft mc = com.jamie.jamiebingo.client.ClientMinecraftUtil.getMinecraft();
            com.jamie.jamiebingo.client.ClientMinecraftUtil.setScreen(mc, new WeeklyChallengeScreen());
        } catch (Throwable t) {
            LOGGER.error("[JamieBingo] Failed to open weekly challenge screen", t);
        }
    }

    public static void handleCustomEffectState(String activeId) {
        String id = activeId == null ? "" : activeId;
        com.jamie.jamiebingo.addons.effects.EffectInvertedScreen.ACTIVE = "inverted_screen".equals(id);
    }

    public static void handleGlobalWallSync(boolean active, net.minecraft.core.BlockPos center, ControllerSettingsSnapshot snapshot, int settingsPage) {
        com.jamie.jamiebingo.client.render.GlobalWallScreenRenderer.onServerSync(active, center, snapshot, settingsPage);
    }

    private static String toRoman(int number) {
        if (number <= 0) return "";
        String[] romans = {
                "I", "II", "III", "IV", "V",
                "VI", "VII", "VIII", "IX", "X"
        };
        if (number <= romans.length) {
            return romans[number - 1];
        }
        return "X+";
    }
}


