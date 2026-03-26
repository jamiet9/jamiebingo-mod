package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.JamieBingo;
import com.jamie.jamiebingo.menu.ModMenus;
import com.jamie.jamiebingo.client.screen.TeamSelectScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.lang.reflect.Method;

@Mod.EventBusSubscriber(
        modid = JamieBingo.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class MenuScreensInit {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {

            // Existing menu (DO NOT TOUCH)
            // Use raw types to avoid generic inference issues across mappings
            registerMenuScreenRaw(
                    (net.minecraft.world.inventory.MenuType) ModMenus.TEAM_SELECT.get(),
                    (MenuScreens.ScreenConstructor) (menu, inventory, title) ->
                            new TeamSelectScreen((com.jamie.jamiebingo.menu.TeamSelectMenu) menu, inventory, title)
            );
        });
    }

    private static void registerMenuScreenRaw(net.minecraft.world.inventory.MenuType type, MenuScreens.ScreenConstructor ctor) {
        // Fallback: reflectively find a static register method on MenuScreens
        for (Method m : MenuScreens.class.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 2) continue;
            if (!m.getParameterTypes()[0].isAssignableFrom(type.getClass())) {
                if (!net.minecraft.world.inventory.MenuType.class.isAssignableFrom(m.getParameterTypes()[0])) {
                    continue;
                }
            }
            if (!MenuScreens.ScreenConstructor.class.isAssignableFrom(m.getParameterTypes()[1])) continue;
            try {
                m.setAccessible(true);
                m.invoke(null, type, ctor);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }

        throw new IllegalStateException("Unable to register MenuScreen");
    }
}




