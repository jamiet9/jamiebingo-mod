package com.jamie.jamiebingo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class ClientMinecraftUtil {
    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger(com.jamie.jamiebingo.JamieBingo.MOD_ID);

    private ClientMinecraftUtil() {
    }

    public static Minecraft getMinecraft() {
        // Try known accessor first
        try {
            Method getInstance = Minecraft.class.getDeclaredMethod("getInstance");
            if (Modifier.isStatic(getInstance.getModifiers()) && Minecraft.class.isAssignableFrom(getInstance.getReturnType())) {
                getInstance.setAccessible(true);
                return (Minecraft) getInstance.invoke(null);
            }
        } catch (ReflectiveOperationException ignored) {
        }

        // Fallback: any static no-arg method returning Minecraft
        for (Method m : Minecraft.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (!Minecraft.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                m.setAccessible(true);
                return (Minecraft) m.invoke(null);
            } catch (ReflectiveOperationException ignored) {
            }
        }

        // Fallback: any static field of type Minecraft
        for (Field f : Minecraft.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) continue;
            if (!Minecraft.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                return (Minecraft) f.get(null);
            } catch (IllegalAccessException ignored) {
            }
        }

        throw new IllegalStateException("Unable to resolve Minecraft instance");
    }

    public static net.minecraft.client.player.LocalPlayer getPlayer(Minecraft mc) {
        try {
            Field playerField = Minecraft.class.getDeclaredField("player");
            if (net.minecraft.client.player.LocalPlayer.class.isAssignableFrom(playerField.getType())) {
                playerField.setAccessible(true);
                return (net.minecraft.client.player.LocalPlayer) playerField.get(mc);
            }
        } catch (ReflectiveOperationException ignored) {
        }

        for (Field f : Minecraft.class.getDeclaredFields()) {
            if (!net.minecraft.client.player.LocalPlayer.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                return (net.minecraft.client.player.LocalPlayer) f.get(mc);
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static net.minecraft.client.player.LocalPlayer getPlayer() {
        return getPlayer(getMinecraft());
    }

    public static net.minecraft.client.multiplayer.ClientLevel getLevel(Minecraft mc) {
        try {
            Field levelField = Minecraft.class.getDeclaredField("level");
            if (net.minecraft.client.multiplayer.ClientLevel.class.isAssignableFrom(levelField.getType())) {
                levelField.setAccessible(true);
                return (net.minecraft.client.multiplayer.ClientLevel) levelField.get(mc);
            }
        } catch (ReflectiveOperationException ignored) {
        }

        for (Field f : Minecraft.class.getDeclaredFields()) {
            if (!net.minecraft.client.multiplayer.ClientLevel.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                return (net.minecraft.client.multiplayer.ClientLevel) f.get(mc);
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static net.minecraft.client.multiplayer.ClientLevel getLevel() {
        return getLevel(getMinecraft());
    }

    public static net.minecraft.client.gui.screens.Screen getScreen(Minecraft mc) {
        try {
            Field screenField = Minecraft.class.getDeclaredField("screen");
            if (net.minecraft.client.gui.screens.Screen.class.isAssignableFrom(screenField.getType())) {
                screenField.setAccessible(true);
                return (net.minecraft.client.gui.screens.Screen) screenField.get(mc);
            }
        } catch (ReflectiveOperationException ignored) {
        }

        for (Field f : Minecraft.class.getDeclaredFields()) {
            if (!net.minecraft.client.gui.screens.Screen.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                return (net.minecraft.client.gui.screens.Screen) f.get(mc);
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static net.minecraft.client.gui.screens.Screen getScreen() {
        return getScreen(getMinecraft());
    }

    public static void clearToasts(Minecraft mc) {
        // Intentionally left as a no-op. Clearing toast state during HUD render
        // can recurse into Minecraft crash/report paths on some client builds.
    }

    public static void clearToasts() {
        clearToasts(getMinecraft());
    }

    public static void setScreen(Minecraft mc, Screen screen) {
        if (mc == null) return;
        boolean isJamieScreen = screen != null && screen.getClass().getName().startsWith("com.jamie.jamiebingo.client.screen.");
        try {
            Method m = Minecraft.class.getMethod("setScreen", Screen.class);
            m.invoke(mc, screen);
            if (screen != null && isJamieScreen) {
                LOGGER.info("[JamieBingo] setScreen via Minecraft#setScreen (forceInit=false, jamieScreen=true)");
                callScreenInit(mc, screen);
            }
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        for (Method m : Minecraft.class.getDeclaredMethods()) {
            if (m.getParameterCount() != 1) continue;
            if (!Screen.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
            if (m.getReturnType() != void.class) continue;
            try {
                m.setAccessible(true);
                m.invoke(mc, screen);
                if (screen != null && isJamieScreen) {
                    LOGGER.info("[JamieBingo] setScreen via reflective MC method (forceInit=false, jamieScreen=true)");
                    callScreenInit(mc, screen);
                }
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        // Last resort: set field directly
        try {
            Field f = Minecraft.class.getDeclaredField("screen");
            if (Screen.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                f.set(mc, screen);
                // Ensure Screen.init is called so widgets render in launcher.
                if (screen != null) {
                    LOGGER.info("[JamieBingo] setScreen via field set (jamieScreen={})", isJamieScreen);
                    callScreenInit(mc, screen);
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void callScreenInit(Minecraft mc, Screen screen) {
        if (mc == null || screen == null) return;
        // Ensure Screen has a Minecraft instance when we bypass Minecraft#setScreen.
        try {
            Field mcField = Screen.class.getDeclaredField("minecraft");
            if (Minecraft.class.isAssignableFrom(mcField.getType())) {
                mcField.setAccessible(true);
                mcField.set(screen, mc);
            }
        } catch (ReflectiveOperationException ignored) {
            for (Field f : Screen.class.getDeclaredFields()) {
                if (!Minecraft.class.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    f.set(screen, mc);
                    break;
                } catch (IllegalAccessException ignored2) {
                }
            }
        }
        // Ensure Screen has a font instance (some screens assume it is non-null).
        net.minecraft.client.gui.Font mcFont = getFont(mc);
        try {
            Field fontField = Screen.class.getDeclaredField("font");
            if (net.minecraft.client.gui.Font.class.isAssignableFrom(fontField.getType())) {
                fontField.setAccessible(true);
                fontField.set(screen, mcFont);
            }
        } catch (ReflectiveOperationException ignored) {
            for (Field f : Screen.class.getDeclaredFields()) {
                if (!net.minecraft.client.gui.Font.class.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    f.set(screen, mcFont);
                    break;
                } catch (IllegalAccessException ignored2) {
                }
            }
        }
        int w = 0;
        int h = 0;
        try {
            Object win = getWindow(mc);
            if (win != null) {
                for (Method m : win.getClass().getMethods()) {
                    if (m.getParameterCount() != 0) continue;
                    String name = m.getName().toLowerCase();
                    if (name.contains("guiscaledwidth") || name.contains("scaledwidth")) {
                        Object out = m.invoke(win);
                        if (out instanceof Integer i) w = i;
                    } else if (name.contains("guiscaledheight") || name.contains("scaledheight")) {
                        Object out = m.invoke(win);
                        if (out instanceof Integer i) h = i;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        if (w <= 0 || h <= 0) {
            // Fallback to window size if scaled size couldn't be resolved.
            try {
                Object win = getWindow(mc);
                if (win != null) {
                    for (Method m : win.getClass().getMethods()) {
                        if (m.getParameterCount() != 0) continue;
                        String name = m.getName().toLowerCase();
                        if (name.contains("width")) {
                            Object out = m.invoke(win);
                            if (out instanceof Integer i) w = Math.max(w, i);
                        } else if (name.contains("height")) {
                            Object out = m.invoke(win);
                            if (out instanceof Integer i) h = Math.max(h, i);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        if (w <= 0 || h <= 0) {
            w = 854;
            h = 480;
        }
        // Ensure Screen width/height fields are set before init.
        try {
            Field wField = Screen.class.getDeclaredField("width");
            Field hField = Screen.class.getDeclaredField("height");
            wField.setAccessible(true);
            hField.setAccessible(true);
            wField.setInt(screen, w);
            hField.setInt(screen, h);
        } catch (ReflectiveOperationException ignored) {
            for (Field f : Screen.class.getDeclaredFields()) {
                try {
                    if (f.getType() == int.class && f.getName().toLowerCase().contains("width")) {
                        f.setAccessible(true);
                        f.setInt(screen, w);
                    } else if (f.getType() == int.class && f.getName().toLowerCase().contains("height")) {
                        f.setAccessible(true);
                        f.setInt(screen, h);
                    }
                } catch (IllegalAccessException ignored2) {
                }
            }
        }
        try {
            LOGGER.info("[JamieBingo] Screen init {} ({}x{})", screen.getClass().getSimpleName(), w, h);
            boolean invoked = false;
            Method initMethod = null;
            // Prefer the init(int,int) declared on Screen itself to avoid picking the wrong method.
            for (Method m : Screen.class.getDeclaredMethods()) {
                if (m.getReturnType() != void.class) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length != 2 || params[0] != int.class || params[1] != int.class) continue;
                initMethod = m;
                break;
            }
            if (initMethod == null) {
                for (Method m : Screen.class.getMethods()) {
                    if (m.getReturnType() != void.class) continue;
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length != 2 || params[0] != int.class || params[1] != int.class) continue;
                    initMethod = m;
                    break;
                }
            }
            if (initMethod != null) {
                try {
                    // Ensure init() will run.
                    try {
                        Field initField = Screen.class.getDeclaredField("initialized");
                        initField.setAccessible(true);
                        initField.setBoolean(screen, false);
                    } catch (ReflectiveOperationException ignored2) {
                    }
                    initMethod.setAccessible(true);
                    initMethod.invoke(screen, w, h);
                    invoked = true;
                } catch (ReflectiveOperationException ex) {
                    LOGGER.warn("[JamieBingo] Screen init(int,int) invoke failed for {}", screen.getClass().getSimpleName(), ex);
                }
            }
            if (!invoked) {
                LOGGER.warn("[JamieBingo] Screen init method not found for {}", screen.getClass().getSimpleName());
            }
            int widgets = countWidgets(screen);
            LOGGER.info("[JamieBingo] Screen widgets after init {} = {}", screen.getClass().getSimpleName(), widgets);
            if (widgets == 0) {
                // Fallback: invoke the screen's protected no-arg init override (obf names in prod).
                try {
                    // Resolve the actual init() name from Screen itself, then invoke override on subclass.
                    String initName = null;
                    for (Method m : Screen.class.getDeclaredMethods()) {
                        if (m.getReturnType() != void.class) continue;
                        if (m.getParameterCount() != 0) continue;
                        int mods = m.getModifiers();
                        if (!Modifier.isProtected(mods) || Modifier.isStatic(mods)) continue;
                        initName = m.getName();
                        break;
                    }
                    Method candidate = null;
                    if (initName != null) {
                        LOGGER.info("[JamieBingo] Resolved Screen.init name: {}", initName);
                        try {
                            candidate = screen.getClass().getDeclaredMethod(initName);
                        } catch (NoSuchMethodException ignored3) {
                        }
                    }
                    if (candidate == null) {
                        for (Method m : screen.getClass().getDeclaredMethods()) {
                            if (m.getReturnType() != void.class) continue;
                            if (m.getParameterCount() != 0) continue;
                            int mods = m.getModifiers();
                            if (!Modifier.isProtected(mods) || Modifier.isStatic(mods)) continue;
                            candidate = m;
                            break;
                        }
                    }
                    if (candidate != null) {
                        LOGGER.info("[JamieBingo] Invoking protected init method: {}", candidate.getName());
                        candidate.setAccessible(true);
                        candidate.invoke(screen);
                        widgets = countWidgets(screen);
                        LOGGER.info("[JamieBingo] Screen widgets after protected init {} = {}", screen.getClass().getSimpleName(), widgets);
                    } else {
                        LOGGER.warn("[JamieBingo] Protected init method not found on {}", screen.getClass().getSimpleName());
                    }
                } catch (ReflectiveOperationException ex) {
                    Throwable cause = ex.getCause();
                    if (cause != null) {
                        LOGGER.warn("[JamieBingo] Protected init invoke failed for {} (cause: {})",
                                screen.getClass().getSimpleName(),
                                cause.toString(),
                                ex);
                    } else {
                        LOGGER.warn("[JamieBingo] Protected init invoke failed for {}", screen.getClass().getSimpleName(), ex);
                    }
                }
            }
        } catch (Throwable ignored) {
            LOGGER.warn("[JamieBingo] Screen init failed for {}", screen.getClass().getSimpleName(), ignored);
        }
    }

    private static int countWidgets(Screen screen) {
        if (screen == null) return 0;
        int total = 0;
        // Try public no-arg list accessors (children/renderables/narratables).
        for (Method m : screen.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            if (!java.util.List.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                Object out = m.invoke(screen);
                if (out instanceof java.util.List<?> list) {
                    total = Math.max(total, list.size());
                }
            } catch (Throwable ignored) {
            }
        }
        // Fallback: scan fields that look like widget lists.
        for (Field f : screen.getClass().getDeclaredFields()) {
            if (!java.util.List.class.isAssignableFrom(f.getType())) continue;
            String name = f.getName().toLowerCase();
            if (!(name.contains("children") || name.contains("render") || name.contains("narrat"))) continue;
            try {
                f.setAccessible(true);
                Object out = f.get(screen);
                if (out instanceof java.util.List<?> list) {
                    total = Math.max(total, list.size());
                }
            } catch (Throwable ignored) {
            }
        }
        return total;
    }

    public static String debugWidgetCounts(Screen screen) {
        if (screen == null) return "null";
        int childrenCount = -1;
        int renderablesCount = -1;
        int narratablesCount = -1;
        try {
            Field f = Screen.class.getDeclaredField("children");
            f.setAccessible(true);
            Object out = f.get(screen);
            if (out instanceof java.util.List<?> list) childrenCount = list.size();
        } catch (Throwable ignored) {
        }
        try {
            Field f = Screen.class.getDeclaredField("renderables");
            f.setAccessible(true);
            Object out = f.get(screen);
            if (out instanceof java.util.List<?> list) renderablesCount = list.size();
        } catch (Throwable ignored) {
        }
        try {
            Field f = Screen.class.getDeclaredField("narratables");
            f.setAccessible(true);
            Object out = f.get(screen);
            if (out instanceof java.util.List<?> list) narratablesCount = list.size();
        } catch (Throwable ignored) {
        }
        return "children=" + childrenCount + " renderables=" + renderablesCount + " narratables=" + narratablesCount;
    }

    private static boolean shouldForceInit(Screen screen) {
        try {
            Field initField = Screen.class.getDeclaredField("initialized");
            initField.setAccessible(true);
            Object out = initField.get(screen);
            if (out instanceof Boolean b && !b) {
                return true;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Field wField = Screen.class.getDeclaredField("width");
            Field hField = Screen.class.getDeclaredField("height");
            wField.setAccessible(true);
            hField.setAccessible(true);
            int w = (int) wField.get(screen);
            int h = (int) hField.get(screen);
            if (w <= 0 || h <= 0) return true;
            return !hasWidgets(screen);
        } catch (ReflectiveOperationException ignored) {
        }
        // If we can't read dimensions, fall back to widget presence.
        return !hasWidgets(screen);
    }

    private static boolean hasWidgets(Screen screen) {
        if (screen == null) return false;
        // Check any no-arg methods returning a List (children/renderables/narratables).
        for (Method m : screen.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            if (!java.util.List.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                Object out = m.invoke(screen);
                if (out instanceof java.util.List<?> list && !list.isEmpty()) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        // Fallback: check fields that look like widget lists.
        for (Field f : screen.getClass().getDeclaredFields()) {
            if (!java.util.List.class.isAssignableFrom(f.getType())) continue;
            String name = f.getName().toLowerCase();
            if (!(name.contains("children") || name.contains("render") || name.contains("narrat"))) continue;
            try {
                f.setAccessible(true);
                Object out = f.get(screen);
                if (out instanceof java.util.List<?> list && !list.isEmpty()) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    public static void setScreen(Screen screen) {
        setScreen(getMinecraft(), screen);
    }

    public static void forceInit(Screen screen) {
        Minecraft mc = getMinecraft();
        if (mc == null || screen == null) return;
        LOGGER.info("[JamieBingo] forceInit {}", screen.getClass().getSimpleName());
        callScreenInit(mc, screen);
    }

    public static Gui getGui(Minecraft mc) {
        try {
            Field f = Minecraft.class.getDeclaredField("gui");
            if (Gui.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return (Gui) f.get(mc);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        for (Field f : Minecraft.class.getDeclaredFields()) {
            if (!Gui.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                return (Gui) f.get(mc);
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static Options getOptions(Minecraft mc) {
        try {
            Field f = Minecraft.class.getDeclaredField("options");
            if (Options.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return (Options) f.get(mc);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        for (Field f : Minecraft.class.getDeclaredFields()) {
            if (!Options.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                return (Options) f.get(mc);
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static Font getFont(Minecraft mc) {
        try {
            Field f = Minecraft.class.getDeclaredField("font");
            if (Font.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return (Font) f.get(mc);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        for (Field f : Minecraft.class.getDeclaredFields()) {
            if (!Font.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                return (Font) f.get(mc);
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static Object getSoundManager(Minecraft mc) {
        try {
            Method m = Minecraft.class.getMethod("getSoundManager");
            Object out = m.invoke(mc);
            if (out != null) return out;
        } catch (ReflectiveOperationException ignored) {
        }
        for (Field f : Minecraft.class.getDeclaredFields()) {
            String name = f.getType().getName().toLowerCase();
            if (!name.contains("soundmanager")) continue;
            try {
                f.setAccessible(true);
                return f.get(mc);
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static ItemRenderer getItemRenderer(Minecraft mc) {
        try {
            Method m = Minecraft.class.getMethod("getItemRenderer");
            Object out = m.invoke(mc);
            if (out instanceof ItemRenderer ir) return ir;
        } catch (ReflectiveOperationException ignored) {
        }
        for (Field f : Minecraft.class.getDeclaredFields()) {
            if (!ItemRenderer.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                return (ItemRenderer) f.get(mc);
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static RenderBuffers getRenderBuffers(Minecraft mc) {
        try {
            Method m = Minecraft.class.getMethod("renderBuffers");
            Object out = m.invoke(mc);
            if (out instanceof RenderBuffers rb) return rb;
        } catch (ReflectiveOperationException ignored) {
        }
        for (Field f : Minecraft.class.getDeclaredFields()) {
            if (!RenderBuffers.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                return (RenderBuffers) f.get(mc);
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static MultiBufferSource.BufferSource getBufferSource(Minecraft mc) {
        RenderBuffers rb = getRenderBuffers(mc);
        return rb == null ? null : rb.bufferSource();
    }

    public static EntityRenderDispatcher getEntityRenderDispatcher(Minecraft mc) {
        try {
            Method m = Minecraft.class.getMethod("getEntityRenderDispatcher");
            Object out = m.invoke(mc);
            if (out instanceof EntityRenderDispatcher ed) return ed;
        } catch (ReflectiveOperationException ignored) {
        }
        for (Field f : Minecraft.class.getDeclaredFields()) {
            if (!EntityRenderDispatcher.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                return (EntityRenderDispatcher) f.get(mc);
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static Object getWindow(Minecraft mc) {
        try {
            Method m = Minecraft.class.getMethod("getWindow");
            Object out = m.invoke(mc);
            if (out != null) return out;
        } catch (ReflectiveOperationException ignored) {
        }
        for (Field f : Minecraft.class.getDeclaredFields()) {
            String name = f.getType().getName().toLowerCase();
            if (!name.contains(".window") && !name.endsWith("window")) continue;
            try {
                f.setAccessible(true);
                return f.get(mc);
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static KeyboardHandler getKeyboardHandler(Minecraft mc) {
        try {
            Field f = Minecraft.class.getDeclaredField("keyboardHandler");
            if (KeyboardHandler.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return (KeyboardHandler) f.get(mc);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        for (Field f : Minecraft.class.getDeclaredFields()) {
            if (!KeyboardHandler.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                return (KeyboardHandler) f.get(mc);
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static boolean shouldSuppressToasts() {
        Minecraft mc = getMinecraft();
        if (mc == null) return false;
        if (mc.screen instanceof com.jamie.jamiebingo.client.BingoCardScreen) {
            return true;
        }
        return ClientEvents.cardOverlayEnabled
                && com.jamie.jamiebingo.client.ClientCardData.hasCard()
                && !com.jamie.jamiebingo.client.casino.ClientCasinoState.isActive();
    }
}
