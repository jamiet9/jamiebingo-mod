package com.jamie.jamiebingo.util;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

public final class ButtonUtil {
    private ButtonUtil() {
    }

    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger(com.jamie.jamiebingo.JamieBingo.MOD_ID);

    public static BuilderCompat builder(Component message, Consumer<Button> onPress) {
        return new BuilderCompat(message, onPress);
    }

    public static Button createButton(Component message, Consumer<Button> onPress, int x, int y, int width, int height) {
        Component msg = message != null ? message : ComponentUtil.empty();
        Button.OnPress press = adaptOnPress(onPress);

        // Try builder-style factory
        try {
            Object builder = invokeBuilder(msg, press);
            if (builder != null) {
                applyPos(builder, x, y);
                applySize(builder, width, height);
                Button built = invokeBuild(builder);
                if (built != null) return built;
            }
        } catch (Throwable ignored) {
        }

        // Try constructors directly
        for (Constructor<?> c : Button.class.getConstructors()) {
            Class<?>[] params = c.getParameterTypes();
            Object[] args = new Object[params.length];
            int intCount = 0;
            boolean ok = true;
            for (int i = 0; i < params.length; i++) {
                Class<?> p = params[i];
                if (p == int.class || p == Integer.class) {
                    intCount++;
                    args[i] = switch (intCount) {
                        case 1 -> x;
                        case 2 -> y;
                        case 3 -> width;
                        case 4 -> height;
                        default -> 0;
                    };
                } else if (Component.class.isAssignableFrom(p)) {
                    args[i] = msg;
                } else if (Button.OnPress.class.isAssignableFrom(p)) {
                    args[i] = press;
                } else {
                    // tooltip or narration suppliers can be null
                    args[i] = null;
                }
            }
            if (intCount < 4) continue;
            try {
                Object out = c.newInstance(args);
                if (out instanceof Button b) return b;
            } catch (Throwable ignored) {
            }
        }

        LOGGER.warn("[JamieBingo] Failed to create Button for '{}' at {},{} ({}x{})",
                msg == null ? "null" : msg.getString(), x, y, width, height);
        return null;
    }

    private static Object invokeBuilder(Component message, Button.OnPress onPress) {
        for (Method m : Button.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) continue;
            if (!m.getReturnType().getName().contains("Button$Builder")) continue;
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 2
                    && Component.class.isAssignableFrom(params[0])
                    && Button.OnPress.class.isAssignableFrom(params[1])) {
                try {
                    m.setAccessible(true);
                    return m.invoke(null, message, onPress);
                } catch (Throwable ignored) {
                }
            }
            if (params.length == 3
                    && Component.class.isAssignableFrom(params[0])
                    && Button.OnPress.class.isAssignableFrom(params[1])) {
                try {
                    m.setAccessible(true);
                    return m.invoke(null, message, onPress, null);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static Button.OnPress adaptOnPress(Consumer<Button> onPress) {
        if (onPress == null) {
            return button -> {};
        }
        try {
            return (Button.OnPress) Proxy.newProxyInstance(
                    Button.OnPress.class.getClassLoader(),
                    new Class[]{Button.OnPress.class},
                    (proxy, method, args) -> {
                        if (args != null && args.length > 0 && args[0] instanceof Button b) {
                            onPress.accept(b);
                        } else {
                            onPress.accept(null);
                        }
                        return null;
                    }
            );
        } catch (Throwable ignored) {
        }
        return button -> onPress.accept(button);
    }

    private static void applyPos(Object builder, int x, int y) {
        for (Method m : builder.getClass().getMethods()) {
            if (m.getParameterCount() != 2) continue;
            if (m.getParameterTypes()[0] != int.class || m.getParameterTypes()[1] != int.class) continue;
            String name = m.getName().toLowerCase();
            if (!name.contains("pos") && !name.contains("position") && !name.contains("x")) continue;
            try {
                m.invoke(builder, x, y);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    private static void applySize(Object builder, int width, int height) {
        for (Method m : builder.getClass().getMethods()) {
            if (m.getParameterCount() == 2
                    && m.getParameterTypes()[0] == int.class
                    && m.getParameterTypes()[1] == int.class) {
                String name = m.getName().toLowerCase();
                if (!name.contains("size") && !name.contains("dim") && !name.contains("width")) continue;
                try {
                    m.invoke(builder, width, height);
                    return;
                } catch (Throwable ignored) {
                }
            }
            if (m.getParameterCount() == 4) {
                Class<?>[] p = m.getParameterTypes();
                if (p[0] != int.class || p[1] != int.class || p[2] != int.class || p[3] != int.class) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("bound")) continue;
                try {
                    m.invoke(builder, 0, 0, width, height);
                    return;
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static Button invokeBuild(Object builder) {
        for (Method m : builder.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            if (!Button.class.isAssignableFrom(m.getReturnType())) continue;
            String name = m.getName().toLowerCase();
            if (!name.contains("build") && !name.contains("create")) continue;
            try {
                Object out = m.invoke(builder);
                if (out instanceof Button b) return b;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static final class BuilderCompat {
        private final Component message;
        private final Consumer<Button> onPress;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;

        private BuilderCompat(Component message, Consumer<Button> onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public BuilderCompat pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public BuilderCompat size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Button build() {
            return createButton(message, onPress, x, y, width, height);
        }
    }
}
