package com.jamie.jamiebingo.util;

import java.lang.reflect.Method;

public final class SoundManagerUtil {
    private SoundManagerUtil() {
    }

    public static void play(Object soundManager, Object soundInstance) {
        if (soundManager == null || soundInstance == null) return;
        try {
            Method m = soundManager.getClass().getMethod("play", soundInstance.getClass());
            m.invoke(soundManager, soundInstance);
            return;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : soundManager.getClass().getMethods()) {
                if (m.getParameterCount() != 1) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("play")) continue;
                if (!m.getParameterTypes()[0].isAssignableFrom(soundInstance.getClass())) continue;
                m.invoke(soundManager, soundInstance);
                return;
            }
        } catch (Throwable ignored) {
        }
    }
}
