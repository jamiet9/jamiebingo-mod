package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.event.TickEvent;

public class ClientForgeEvents {

    private static boolean lastCountdownActive = false;
    private static int lastCountdownBeep = -1;
    private static final int OVERLAY_TOGGLE_COOLDOWN_TICKS = 4;
    private static long lastCardOverlayToggleTick = -1000;
    private static long lastSettingsOverlayToggleTick = -1000;
    private static long lastScoreboardToggleTick = -1000;
    private static boolean settingsOverlayForced = false;
    private static boolean settingsOverlayStateBeforeForce = true;

    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {

        var mc = ClientMinecraftUtil.getMinecraft();
        var player = ClientMinecraftUtil.getPlayer(mc);
        var level = ClientMinecraftUtil.getLevel(mc);
        if (player == null || level == null) return;

        if (!(ClientMinecraftUtil.getScreen(mc) instanceof net.minecraft.client.gui.screens.ChatScreen)) {
            ClientTeamChatState.pendingTeamChat = false;
            ClientTeamChatState.pendingTeamChatUntilTick = -1;
        }

        boolean countdownActive = ClientStartCountdown.isActive();
        boolean casinoPhaseActive = ClientCasinoState.isActive()
                || ClientCasinoState.isDraftPhase()
                || ClientCasinoState.isRerollPhase();
        updateForcedSettingsOverlay(countdownActive || casinoPhaseActive);

        if (countdownActive) {
            int secondsRemaining = ClientStartCountdown.getSecondsRemaining();
            if (secondsRemaining > 0 && secondsRemaining <= 3 && secondsRemaining != lastCountdownBeep) {
                player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 1.0f);
                lastCountdownBeep = secondsRemaining;
            }
            lastCountdownActive = true;
            return;
        }
        if (lastCountdownActive) {
            player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            lastCountdownBeep = -1;
            lastCountdownActive = false;
        }

        if (com.jamie.jamiebingo.util.KeyMappingUtil.consumeClick(ClientKeybinds.TOGGLE_FULLSCREEN_CARD)) {
            var screen = ClientMinecraftUtil.getScreen(mc);
            if (BingoCardScreen.isFullscreenCardScreen(screen)) {
                BingoCardScreen.closeFullscreen();
                ClientStartCountdown.setAutoOpenedFullscreenCard(false);
                return;
            } else if (ClientCardData.hasCard() && screen == null) {
                BingoCardScreen.openFullscreen(ClientCardData.getCard());
                ClientStartCountdown.setAutoOpenedFullscreenCard(false);
                return;
            }
        }

        if (ClientCasinoState.isActive()) {
            return;
        }

        if (ClientMinecraftUtil.getScreen(mc) != null) {
            return;
        }

        long now = level.getGameTime();
        if (com.jamie.jamiebingo.util.KeyMappingUtil.consumeClick(ClientKeybinds.TOGGLE_CARD_OVERLAY)) {
            if (now - lastCardOverlayToggleTick >= OVERLAY_TOGGLE_COOLDOWN_TICKS) {
                ClientEvents.cardOverlayEnabled = !ClientEvents.cardOverlayEnabled;
                lastCardOverlayToggleTick = now;
            }
        }

        if (com.jamie.jamiebingo.util.KeyMappingUtil.consumeClick(ClientKeybinds.TOGGLE_SETTINGS_OVERLAY)) {
            if (now - lastSettingsOverlayToggleTick >= OVERLAY_TOGGLE_COOLDOWN_TICKS) {
                ClientEvents.settingsOverlayEnabled = !ClientEvents.settingsOverlayEnabled;
                lastSettingsOverlayToggleTick = now;
            }
        }

        if (com.jamie.jamiebingo.util.KeyMappingUtil.consumeClick(ClientKeybinds.TOGGLE_SCOREBOARD_OVERLAY)) {
            if (now - lastScoreboardToggleTick >= OVERLAY_TOGGLE_COOLDOWN_TICKS) {
                ClientEvents.scoreboardOverlayEnabled = !ClientEvents.scoreboardOverlayEnabled;
                lastScoreboardToggleTick = now;
            }
        }
    }

    private static void updateForcedSettingsOverlay(boolean forceOn) {
        if (forceOn) {
            if (!settingsOverlayForced) {
                settingsOverlayStateBeforeForce = ClientEvents.settingsOverlayEnabled;
            }
            settingsOverlayForced = true;
            ClientEvents.settingsOverlayEnabled = true;
            return;
        }
        if (settingsOverlayForced) {
            ClientEvents.settingsOverlayEnabled = settingsOverlayStateBeforeForce;
            settingsOverlayForced = false;
        }
    }
}
