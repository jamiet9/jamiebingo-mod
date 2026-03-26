package com.jamie.jamiebingo.client;

import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraft.resources.Identifier;
import com.jamie.jamiebingo.JamieBingo;

public class OverlayInit {

    public static void registerOverlays(AddGuiOverlayLayersEvent event) {
        var layered = event.getLayeredDraw();
        var rootStack = layered
                .locateStack(ForgeLayeredDraw.VANILLA_ROOT)
                .orElse(layered);

        Identifier overlayId = com.jamie.jamiebingo.util.IdUtil.id(JamieBingo.MOD_ID + ":jamiebingo_overlay");
        rootStack.add(overlayId, new OverlayRenderer());
        layered.putAbove(ForgeLayeredDraw.VANILLA_ROOT, ForgeLayeredDraw.HOTBAR_AND_DECOS, overlayId);
    }
}

