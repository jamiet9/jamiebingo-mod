package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.addons.effects.EffectInvertedScreen;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientCameraHandler {

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {

        if (!EffectInvertedScreen.ACTIVE) return;

        // Flip the camera upside-down
        event.setRoll(event.getRoll() + 180.0F);
    }
}




