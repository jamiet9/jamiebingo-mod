package com.jamie.jamiebingo.util;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.ChatComponent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class GuiChatUtil {
    private GuiChatUtil() {
    }

    public static ChatComponent getChat(Gui gui) {
        if (gui == null) return null;
        try {
            Method m = gui.getClass().getMethod("getChat");
            Object out = m.invoke(gui);
            if (out instanceof ChatComponent chat) return chat;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : gui.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!m.getName().toLowerCase().contains("chat")) continue;
                Object out = m.invoke(gui);
                if (out instanceof ChatComponent chat) return chat;
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Field f : gui.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (!ChatComponent.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object out = f.get(gui);
                if (out instanceof ChatComponent chat) return chat;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
