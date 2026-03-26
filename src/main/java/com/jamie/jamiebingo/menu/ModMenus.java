package com.jamie.jamiebingo.menu;

import com.jamie.jamiebingo.JamieBingo;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, JamieBingo.MOD_ID);

    // ===============================
    // TEAM SELECT MENU (EXISTING – KEEP)
    // ===============================
    public static final RegistryObject<MenuType<TeamSelectMenu>> TEAM_SELECT =
            MENUS.register(
                    "team_select",
                    () -> createMenuType(TeamSelectMenu::new)
            );

    public static void register(BusGroup bus) {
        MENUS.register(bus);
    }

    @SuppressWarnings("unchecked")
    private static <T extends AbstractContainerMenu> MenuType<T> createMenuType(MenuType.MenuSupplier<T> supplier) {
        try {
            Constructor<MenuType> ctor = MenuType.class.getDeclaredConstructor(MenuType.MenuSupplier.class);
            ctor.setAccessible(true);
            return (MenuType<T>) ctor.newInstance(supplier);
        } catch (Throwable ignored) {
        }
        try {
            Class<?> flagSetClass = Class.forName("net.minecraft.world.flag.FeatureFlagSet");
            Object flags = resolveFeatureFlagSet(flagSetClass);
            Constructor<MenuType> ctor = MenuType.class.getDeclaredConstructor(MenuType.MenuSupplier.class, flagSetClass);
            ctor.setAccessible(true);
            return (MenuType<T>) ctor.newInstance(supplier, flags);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create MenuType", e);
        }
    }

    private static Object resolveFeatureFlagSet(Class<?> flagSetClass) throws Exception {
        Class<?> flagsClass = Class.forName("net.minecraft.world.flag.FeatureFlags");
        for (Field field : flagsClass.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!flagSetClass.isAssignableFrom(field.getType())) continue;
            field.setAccessible(true);
            Object value = field.get(null);
            if (value != null) {
                return value;
            }
        }
        for (Method method : flagsClass.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) continue;
            if (method.getParameterCount() != 0) continue;
            if (!flagSetClass.isAssignableFrom(method.getReturnType())) continue;
            method.setAccessible(true);
            Object value = method.invoke(null);
            if (value != null) {
                return value;
            }
        }
        throw new IllegalStateException("Unable to resolve FeatureFlagSet");
    }
}
