package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.JamieBingo;
import com.jamie.jamiebingo.bingo.BingoRarityUtil;
import com.jamie.jamiebingo.bingo.WinCondition;
import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketSetPreferredTeamColor;
import com.jamie.jamiebingo.network.packet.PacketSetBlacklistIds;
import com.jamie.jamiebingo.network.packet.PacketSetRarityOverridesChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID, value = Dist.CLIENT)
public class ClientConnectionEvents {
    private static final int MAX_RARITY_OVERRIDES_PER_PACKET = 180;
    private static int pendingPreferredColorId = -1;
    private static int loginTickCounter = -1;

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        clearClientState();
        ClientBlacklistSettings.load();
        ClientRarityOverrideSettings.load();
        ClientTeamPreferenceSettings.load();
        NetworkHandler.sendToServer(new PacketSetBlacklistIds(
                new java.util.ArrayList<>(ClientBlacklistSettings.getActiveBlacklistUnion()),
                new java.util.ArrayList<>(ClientBlacklistSettings.getActiveWhitelistUnion())
        ));
        sendRarityOverridesInChunks(ClientRarityOverrideSettings.getActiveOverrides());
        int preferredTeamColorId = ClientTeamPreferenceSettings.getPreferredTeamColorId();
        if (preferredTeamColorId >= 0) {
            NetworkHandler.sendToServer(new PacketSetPreferredTeamColor(preferredTeamColorId));
            pendingPreferredColorId = preferredTeamColorId;
            loginTickCounter = 0;
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clearClientState();
        pendingPreferredColorId = -1;
        loginTickCounter = -1;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        if (pendingPreferredColorId < 0 || loginTickCounter < 0) return;
        loginTickCounter++;
        // Resend after world join settles so fresh singleplayer worlds reliably receive preference.
        if (loginTickCounter == 20 || loginTickCounter == 80) {
            NetworkHandler.sendToServer(new PacketSetPreferredTeamColor(pendingPreferredColorId));
        }
        if (loginTickCounter > 120) {
            pendingPreferredColorId = -1;
            loginTickCounter = -1;
        }
    }

    public static void clearClientState() {
        ClientCardData.clear();
        ClientProgressData.clear();
        ClientQuestProgressData.clear();
        ClientRevealedSlots.clear();
        ClientHighlightedSlots.clear();
        ClientSlotOwnership.clear();
        ClientSettingsOverlay.clear();
        ClientTeamScoreData.clear();
        ClientWinningLineData.clear();
        ClientStartCountdown.clear();
        ClientFlashSlots.clear();
        ClientCasinoState.end();
        com.jamie.jamiebingo.client.render.GlobalWallScreenRenderer.forceDeactivate();
        ClientGameState.exitGame();
        ClientGameState.winCondition = WinCondition.LINE;
        ClientGameState.teamChestEnabled = true;
        ClientGameTimer.clear();
        ClientHangmanState.clear();
        ClientMineState.clear();
        ClientPowerSlotState.clear();
        ClientPowerSlotWheelAnimation.clear();
        ClientVoteEndGameState.reset();
        ClientVoteRerollCardState.reset();
        ClientCustomCardState.setState(null, false, false, java.util.List.of(), java.util.List.of(), java.util.List.of());
        OverlayRenderer.randomEffectText = "";
        OverlayRenderer.randomEffectTicksRemaining = 0;
    }

    private static void sendRarityOverridesInChunks(java.util.Map<String, String> allOverrides) {
        if (allOverrides == null || allOverrides.isEmpty()) {
            NetworkHandler.sendToServer(new PacketSetRarityOverridesChunk(true, true, java.util.Map.of()));
            return;
        }
        java.util.List<java.util.Map.Entry<String, String>> entries = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, String> entry : allOverrides.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) continue;
            entries.add(java.util.Map.entry(entry.getKey(), BingoRarityUtil.normalize(entry.getValue())));
        }
        if (entries.isEmpty()) {
            NetworkHandler.sendToServer(new PacketSetRarityOverridesChunk(true, true, java.util.Map.of()));
            return;
        }
        java.util.Map<String, String> chunk = new java.util.LinkedHashMap<>();
        boolean first = true;
        for (int i = 0; i < entries.size(); i++) {
            java.util.Map.Entry<String, String> entry = entries.get(i);
            chunk.put(entry.getKey(), entry.getValue());
            if (chunk.size() >= MAX_RARITY_OVERRIDES_PER_PACKET) {
                boolean finalChunk = (i == entries.size() - 1);
                NetworkHandler.sendToServer(new PacketSetRarityOverridesChunk(first, finalChunk, chunk));
                chunk = new java.util.LinkedHashMap<>();
                first = false;
            }
        }
        if (!chunk.isEmpty()) {
            NetworkHandler.sendToServer(new PacketSetRarityOverridesChunk(first, true, chunk));
        }
    }
}




