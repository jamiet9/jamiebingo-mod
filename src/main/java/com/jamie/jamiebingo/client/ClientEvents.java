package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import com.jamie.jamiebingo.client.screen.CasinoBingoScreen;
import com.jamie.jamiebingo.network.PacketCasinoEnd;
import com.jamie.jamiebingo.network.PacketCasinoStart;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class ClientEvents {

    public static boolean cardOverlayEnabled = true;
    public static boolean settingsOverlayEnabled = true;
    public static boolean scoreboardOverlayEnabled = true;
    public static boolean fullscreenCardEnabled = false;

    /* =========================
       CASINO START
       ========================= */

    public static void onCasinoStart(
            PacketCasinoStart msg,
            CustomPayloadEvent.Context ctx
    ) {
        ctx.enqueueWork(() -> {

            ClientGameState.enterCasino();
            ClientCasinoState.start(msg.gridSize);

            ClientMinecraftUtil.setScreen(ClientMinecraftUtil.getMinecraft(), new CasinoBingoScreen());
        });

        ctx.setPacketHandled(true);
    }

    /* =========================
       CASINO END
       ========================= */

    public static void onCasinoEnd(
            PacketCasinoEnd msg,
            CustomPayloadEvent.Context ctx
    ) {
        ctx.enqueueWork(() -> {

            ClientCasinoState.end();
            ClientGameState.exitCasino();
        });

        ctx.setPacketHandled(true);
    }
}
