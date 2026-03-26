package com.jamie.jamiebingo.util;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public final class ClientRunUtil {

    private ClientRunUtil() {
    }

    public static void runOnClient(Runnable task) {
        if (task == null) return;
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> {
                    try {
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc != null) {
                            mc.execute(task);
                            org.apache.logging.log4j.LogManager.getLogger(com.jamie.jamiebingo.JamieBingo.MOD_ID)
                                    .info("[JamieBingo] ClientRunUtil executed on client thread");
                            return;
                        }
                    } catch (Throwable ignored) {
                    }
                    org.apache.logging.log4j.LogManager.getLogger(com.jamie.jamiebingo.JamieBingo.MOD_ID)
                            .info("[JamieBingo] ClientRunUtil skipped (no MC instance)");
                }
        );
    }
}
