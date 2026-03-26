package com.jamie.jamiebingo.util;

import com.jamie.jamiebingo.JamieBingo;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID)
public final class FireworkDamageHandler {

    @SubscribeEvent
    public static void onFireworkHurt(LivingHurtEvent event) {
        FireworkRocketEntity rocket = null;
        if (DamageSourceUtil.getEntity(event.getSource()) instanceof FireworkRocketEntity entityRocket) {
            rocket = entityRocket;
        } else if (DamageSourceUtil.getDirectEntity(event.getSource()) instanceof FireworkRocketEntity directRocket) {
            rocket = directRocket;
        }
        if (rocket == null) return;
        if (com.jamie.jamiebingo.util.NbtUtil.getBoolean(rocket.getPersistentData(), "jamiebingo_no_damage", false)) {
            event.setAmount(0.0f);
        }
    }
}




