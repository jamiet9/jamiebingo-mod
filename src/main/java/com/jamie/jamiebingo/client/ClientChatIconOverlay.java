package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.client.render.QuestIconRenderer;
import com.jamie.jamiebingo.quest.icon.QuestIconData;
import com.jamie.jamiebingo.quest.icon.QuestIconProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.Options;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.Modifier;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.Deque;

public final class ClientChatIconOverlay {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MARKER_PREFIX = "jamiebingo:icon:";
    private static final int ICON_SIZE = 16;

    private static Field CHAT_MESSAGES_FIELD;
    private static Field CHAT_LINES_FIELD;
    private static Field CHAT_SCROLL_FIELD;
    private static Method CHAT_HIDDEN_METHOD;
    private static Method OPTIONS_CHAT_OPACITY_METHOD;
    private static Method RENDER_SYSTEM_COLOR_METHOD;
    private static Field MESSAGE_TIME_FIELD;
    private static Field MESSAGE_COMPONENT_FIELD;
    private static Field LINE_TIME_FIELD;
    private static Field LINE_CONTENT_FIELD;
    private static Field LINE_IS_LAST_FIELD;
    private static boolean CHAT_OVERLAY_DISABLED = false;
    private static boolean CHAT_OVERLAY_ERROR_LOGGED = false;
    private static final List<ActiveChatIcon> ACTIVE_ICONS = new java.util.ArrayList<>();
    private static final Deque<String> PENDING_SLOT_IDS = new ArrayDeque<>();

    private ClientChatIconOverlay() {}

    public static void render(GuiGraphics graphics) {
        if (CHAT_OVERLAY_DISABLED) return;
        try {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        Gui gui = ClientMinecraftUtil.getGui(mc);
        net.minecraft.client.multiplayer.ClientLevel level = ClientMinecraftUtil.getLevel(mc);
        if (gui == null || level == null) return;

        ChatComponent chat = com.jamie.jamiebingo.util.GuiChatUtil.getChat(gui);
        if (chat == null) return;
        if (isChatHidden(chat)) return;

        net.minecraft.client.Options options = ClientMinecraftUtil.getOptions(mc);
        if (options == null) return;
        double scale = com.jamie.jamiebingo.util.OptionsUtil.getChatScale(options);
        if (scale <= 0.01) return;

        int guiHeight = com.jamie.jamiebingo.util.GuiGraphicsUtil.getGuiHeight(graphics);
        boolean focused = ClientMinecraftUtil.getScreen(mc) instanceof ChatScreen;
        double heightOption = focused
                ? com.jamie.jamiebingo.util.OptionsUtil.getChatHeightFocused(options)
                : com.jamie.jamiebingo.util.OptionsUtil.getChatHeightUnfocused(options);
        int chatHeight = (int) Math.floor(160.0 * heightOption + 20.0);
        double spacing = com.jamie.jamiebingo.util.OptionsUtil.getChatLineSpacing(options);
        int lineHeight = (int) (9.0 * (spacing + 1.0));
        int textYOffset = (int) Math.round(8.0 * (spacing + 1.0) - 4.0 * spacing);
        int linesPerPage = chatHeight / Math.max(1, lineHeight);
        int guiTicks = getGuiTicks(gui);
        float opacity = getChatOpacity(mc);
        Font font = ClientMinecraftUtil.getFont(mc);
        if (font == null) return;
        int scroll = getChatScroll(chat);
        List<Object> lines = getChatLines(chat);
        if (lines == null || lines.isEmpty()) return;
        Map<Integer, String> slotIdsByTime = buildSlotIdsByTime(chat);
        flushPendingIcons(mc, lines);

        int totalLines = lines.size();
        int visible = Math.min(Math.max(0, totalLines - scroll), linesPerPage);
        if (visible <= 0) return;

        int baseY = (int) Math.floor((guiHeight - 40) / scale);

        var pose = com.jamie.jamiebingo.util.GuiGraphicsUtil.getPose(graphics);
        pose.pushMatrix();
        pose.scale((float) scale, (float) scale);
        for (int rel = 0; rel < visible; rel++) {
            int i5 = visible - 1 - rel;
            int lineIndex = i5 + scroll;
            if (lineIndex < 0 || lineIndex >= totalLines) continue;

            Object lineObj = lines.get(lineIndex);
            if (lineObj == null) continue;
            FormattedCharSequence content = getLineContent(lineObj);
            if (content == null) continue;
            String slotId = findMarkerSlotId(content);
            if (slotId == null || slotId.isBlank()) {
                slotId = slotIdsByTime.get(getLineTime(lineObj));
            }
            if (slotId == null || slotId.isBlank()) continue;
            float alpha = computeLineAlpha(mc, guiTicks, lineObj) * opacity;
            if (alpha <= 0.01f) continue;

            int lineY = baseY - i5 * lineHeight - textYOffset;
            int lineWidth = font.width(content);
            int iconX = 2 + lineWidth + 2;
            int iconY = lineY;

            setShaderColor(1f, 1f, 1f, alpha);
            renderIcon(graphics, slotId, iconX, iconY);
            setShaderColor(1f, 1f, 1f, 1f);
        }

        if (!ACTIVE_ICONS.isEmpty()) {
            int now = guiTicks;
            ACTIVE_ICONS.removeIf(icon -> now - icon.addedTime > 200);
            for (ActiveChatIcon icon : ACTIVE_ICONS) {
                if (icon.lineIndex < scroll || icon.lineIndex >= scroll + visible) continue;
                int relIndex = visible - 1 - (icon.lineIndex - scroll);
                if (relIndex < 0 || relIndex >= visible) continue;

                float alpha = computeLineAlpha(mc, guiTicks, icon.addedTime) * opacity;
                if (alpha <= 0.01f) continue;

                int lineY = baseY - relIndex * lineHeight - textYOffset;
                int iconX = 2 + icon.lineWidth + 2;
                int iconY = lineY;

                setShaderColor(1f, 1f, 1f, alpha);
                renderIcon(graphics, icon.slotId, iconX, iconY);
                setShaderColor(1f, 1f, 1f, 1f);
            }
        }

        pose.popMatrix();
        } catch (Throwable t) {
            if (!CHAT_OVERLAY_ERROR_LOGGED) {
                CHAT_OVERLAY_ERROR_LOGGED = true;
                LOGGER.warn("[JamieBingo] Disabling chat icon overlay due to error", t);
            }
            CHAT_OVERLAY_DISABLED = true;
        }
    }

    private static void renderIcon(GuiGraphics graphics, String slotId, int x, int y) {
        ItemStack stack = buildItemStack(slotId);
        var pose = com.jamie.jamiebingo.util.GuiGraphicsUtil.getPose(graphics);
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.scale(0.5f, 0.5f);

        if (!stack.isEmpty()) {
            graphics.renderItem(stack, 0, 0);
            pose.popMatrix();
            return;
        }

        if (slotId != null && slotId.startsWith("quest.")) {
            BingoSlot dummy = new BingoSlot(slotId, slotId, "", "common");
            QuestIconData icon = QuestIconProvider.iconFor(dummy);
            if (stack.isEmpty() && icon != null) {
                stack = pickQuestItem(icon);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, 0, 0);
                    pose.popMatrix();
                    return;
                }
            }
            QuestIconRenderer.renderQuestIcon(graphics, 0, 0, icon, ICON_SIZE);
        }

        pose.popMatrix();
    }

    private static ItemStack buildItemStack(String slotId) {
        if (slotId == null || slotId.isBlank() || slotId.startsWith("quest.")) {
            return com.jamie.jamiebingo.util.ItemStackUtil.empty();
        }
        try {
            Identifier rl = com.jamie.jamiebingo.util.IdUtil.id(slotId.contains(":") ? slotId : "minecraft:" + slotId);
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item != null) return new ItemStack(item);
        } catch (Exception ignored) {}
        return com.jamie.jamiebingo.util.ItemStackUtil.empty();
    }

    private static ItemStack pickQuestItem(QuestIconData icon) {
        if (icon == null) return com.jamie.jamiebingo.util.ItemStackUtil.empty();
        if (icon.mainIcon != null && !icon.mainIcon.isEmpty()) return icon.mainIcon.copy();
        if (icon.rotatingIcons != null && !icon.rotatingIcons.isEmpty()) return icon.rotatingIcons.get(0).copy();
        if (icon.cornerIcon != null && !icon.cornerIcon.isEmpty()) return icon.cornerIcon.copy();
        if (icon.rotatingCornerIcons != null && !icon.rotatingCornerIcons.isEmpty()) {
            return icon.rotatingCornerIcons.get(0).copy();
        }
        EntityType<?> entity = icon.mainEntityType;
        if (entity == null && icon.rotatingEntities != null && !icon.rotatingEntities.isEmpty()) {
            entity = icon.rotatingEntities.get(0);
        }
        if (entity == null && icon.cornerEntityType != null) {
            entity = icon.cornerEntityType;
        }
        if (entity == null && icon.rotatingCornerEntities != null && !icon.rotatingCornerEntities.isEmpty()) {
            entity = icon.rotatingCornerEntities.get(0);
        }
        if (entity != null) {
            ItemStack egg = spawnEggForEntity(entity);
            if (!egg.isEmpty()) return egg;
        }
        return com.jamie.jamiebingo.util.ItemStackUtil.empty();
    }

    private static ItemStack spawnEggForEntity(EntityType<?> entityType) {
        try {
            Identifier key = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
            if (key == null) return com.jamie.jamiebingo.util.ItemStackUtil.empty();
            Identifier eggId = com.jamie.jamiebingo.util.IdUtil.id(key.getNamespace() + ":" + key.getPath() + "_spawn_egg");
            Item item = ForgeRegistries.ITEMS.getValue(eggId);
            if (item != null) return new ItemStack(item);
        } catch (Exception ignored) {
        }
        return com.jamie.jamiebingo.util.ItemStackUtil.empty();
    }

    private static String findMarkerSlotId(FormattedCharSequence sequence) {
        if (sequence == null) return null;
        final String[] out = new String[1];
        tryAcceptFormatted(sequence, out);
        return out[0];
    }

    private static void tryAcceptFormatted(FormattedCharSequence sequence, String[] out) {
        if (sequence == null || out == null) return;
        net.minecraft.util.FormattedCharSink sink = (index, style, codePoint) -> {
            String insertion = com.jamie.jamiebingo.util.ComponentUtil.getInsertion(style);
            if (insertion != null && insertion.startsWith(MARKER_PREFIX)) {
                out[0] = insertion.substring(MARKER_PREFIX.length());
                return false;
            }
            return true;
        };
        // Direct method name on dev mappings
        try {
            java.lang.reflect.Method m = sequence.getClass().getMethod("accept", net.minecraft.util.FormattedCharSink.class);
            m.invoke(sequence, sink);
            return;
        } catch (Throwable ignored) {
        }
        // Fallback: any method that takes FormattedCharSink
        try {
            for (java.lang.reflect.Method m : sequence.getClass().getMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (!net.minecraft.util.FormattedCharSink.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
                m.invoke(sequence, sink);
                return;
            }
        } catch (Throwable ignored) {
        }
    }

    private static String findMarkerSlotId(Component component) {
        if (component == null) return null;
        String insertion = com.jamie.jamiebingo.util.ComponentUtil.getInsertion(
                com.jamie.jamiebingo.util.ComponentUtil.getStyle(component)
        );
        if (insertion != null && insertion.startsWith(MARKER_PREFIX)) {
            return insertion.substring(MARKER_PREFIX.length());
        }
        for (Component sibling : com.jamie.jamiebingo.util.ComponentUtil.getSiblings(component)) {
            String found = findMarkerSlotId(sibling);
            if (found != null) return found;
        }
        return null;
    }

    private static Map<Integer, String> buildSlotIdsByTime(ChatComponent chat) {
        Map<Integer, String> out = new HashMap<>();
        List<Object> messages = getChatMessages(chat);
        if (messages == null) return out;
        for (Object msg : messages) {
            int time = getMessageTime(msg);
            String slotId = findMarkerSlotId(getMessageComponent(msg));
            if (slotId != null && !slotId.isBlank()) {
                out.put(time, slotId);
            }
        }
        return out;
    }

    public static void recordSlotId(String slotId) {
        if (slotId == null || slotId.isBlank()) return;
        PENDING_SLOT_IDS.addLast(slotId);
        LOGGER.info("[JamieBingo] recordSlotId pending slotId={}", slotId);
    }

    private static void flushPendingIcons(Minecraft mc, List<Object> lines) {
        Font font = ClientMinecraftUtil.getFont(mc);
        if (PENDING_SLOT_IDS.isEmpty() || mc == null || font == null) return;
        int lineCount = lines.size();
        int pendingCount = PENDING_SLOT_IDS.size();
        int startIndex = Math.max(0, lineCount - pendingCount);

        int idx = 0;
        while (!PENDING_SLOT_IDS.isEmpty() && startIndex + idx < lineCount) {
            String slotId = PENDING_SLOT_IDS.pollFirst();
            Object lineObj = lines.get(startIndex + idx);
            int time = getLineTime(lineObj);
            FormattedCharSequence content = getLineContent(lineObj);
            int width = content != null ? font.width(content) : 0;
            if (time >= 0) {
                ACTIVE_ICONS.add(new ActiveChatIcon(slotId, startIndex + idx, time, width));
                LOGGER.info("[JamieBingo] flushPendingIcons slotId={} lineIndex={} time={} width={}",
                        slotId, startIndex + idx, time, width);
            }
            idx++;
        }
    }

    private static float computeLineAlpha(Minecraft mc, int guiTicks, Object lineObj) {
        if (mc == null) return 0f;
        boolean focused = ClientMinecraftUtil.getScreen(mc) instanceof ChatScreen;
        if (focused) return 1f;
        int lineTime = getLineTime(lineObj);
        if (lineTime < 0) return 0f;
        int age = guiTicks - lineTime;
        double d = 1.0 - (age / 200.0);
        d = clamp(d * 10.0, 0.0, 1.0);
        d = d * d;
        return (float) d;
    }

    private static float computeLineAlpha(Minecraft mc, int guiTicks, int lineTime) {
        if (mc == null) return 0f;
        boolean focused = ClientMinecraftUtil.getScreen(mc) instanceof ChatScreen;
        if (focused) return 1f;
        if (lineTime < 0) return 0f;
        int age = guiTicks - lineTime;
        double d = 1.0 - (age / 200.0);
        d = clamp(d * 10.0, 0.0, 1.0);
        d = d * d;
        return (float) d;
    }

    private static float getChatOpacity(Minecraft mc) {
        if (mc == null) return 1f;
        Options options = ClientMinecraftUtil.getOptions(mc);
        if (options == null) return 1f;
        double v = com.jamie.jamiebingo.util.OptionsUtil.getChatOpacity(options);
        return (float) (v * 0.9 + 0.1);
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private static boolean isChatHidden(ChatComponent chat) {
        if (chat == null) return false;
        try {
            if (CHAT_HIDDEN_METHOD == null) {
                CHAT_HIDDEN_METHOD = ObfuscationReflectionHelper.findMethod(
                        chat.getClass(),
                        "l"
                );
            }
            return (boolean) CHAT_HIDDEN_METHOD.invoke(chat);
        } catch (Exception e) {
            return false;
        }
    }

    private static void setShaderColor(float r, float g, float b, float a) {
        try {
            if (RENDER_SYSTEM_COLOR_METHOD == null) {
                for (Method m : RenderSystem.class.getDeclaredMethods()) {
                    if (!Modifier.isStatic(m.getModifiers())) continue;
                    if (m.getParameterCount() != 4) continue;
                    Class<?>[] p = m.getParameterTypes();
                    if (p[0] == float.class && p[1] == float.class && p[2] == float.class && p[3] == float.class) {
                        RENDER_SYSTEM_COLOR_METHOD = m;
                        RENDER_SYSTEM_COLOR_METHOD.setAccessible(true);
                        break;
                    }
                }
            }
            if (RENDER_SYSTEM_COLOR_METHOD != null) {
                RENDER_SYSTEM_COLOR_METHOD.invoke(null, r, g, b, a);
            }
        } catch (Exception ignored) {
        }
    }

    private static int getMessageTime(Object msg) {
        if (msg == null) return -1;
        try {
            if (MESSAGE_TIME_FIELD == null) {
                MESSAGE_TIME_FIELD = findFieldByType(msg.getClass(), int.class, 0);
            }
            return MESSAGE_TIME_FIELD != null ? MESSAGE_TIME_FIELD.getInt(msg) : -1;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static Component getMessageComponent(Object msg) {
        if (msg == null) return null;
        try {
            if (MESSAGE_COMPONENT_FIELD == null) {
                MESSAGE_COMPONENT_FIELD = findFieldByType(msg.getClass(), Component.class, 0);
            }
            return MESSAGE_COMPONENT_FIELD != null ? (Component) MESSAGE_COMPONENT_FIELD.get(msg) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int getLineTime(Object lineObj) {
        if (lineObj == null) return -1;
        try {
            if (LINE_TIME_FIELD == null) {
                LINE_TIME_FIELD = findFieldByType(lineObj.getClass(), int.class, 0);
            }
            return LINE_TIME_FIELD != null ? LINE_TIME_FIELD.getInt(lineObj) : -1;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static FormattedCharSequence getLineContent(Object lineObj) {
        if (lineObj == null) return null;
        try {
            if (LINE_CONTENT_FIELD == null) {
                LINE_CONTENT_FIELD = findFieldByType(lineObj.getClass(), FormattedCharSequence.class, 0);
            }
            return LINE_CONTENT_FIELD != null ? (FormattedCharSequence) LINE_CONTENT_FIELD.get(lineObj) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean getLineIsLast(Object lineObj) {
        if (lineObj == null) return false;
        try {
            if (LINE_IS_LAST_FIELD == null) {
                LINE_IS_LAST_FIELD = findFieldByType(lineObj.getClass(), boolean.class, 0);
            }
            return LINE_IS_LAST_FIELD != null && LINE_IS_LAST_FIELD.getBoolean(lineObj);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Field findFieldByType(Class<?> owner, Class<?> type, int index) {
        int found = 0;
        for (Field f : owner.getDeclaredFields()) {
            if (f.getType() != type) continue;
            if (found == index) {
                f.setAccessible(true);
                return f;
            }
            found++;
        }
        return null;
    }

    private static int getGuiTicks(Gui gui) {
        if (gui == null) return 0;
        try {
            Method m = gui.getClass().getMethod("getGuiTicks");
            Object out = m.invoke(gui);
            if (out instanceof Number n) return n.intValue();
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : gui.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                Class<?> rt = m.getReturnType();
                if (!(rt == int.class || rt == Integer.class)) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("tick")) continue;
                Object out = m.invoke(gui);
                if (out instanceof Number n) return n.intValue();
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Field f : gui.getClass().getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                if (f.getType() != int.class) continue;
                String name = f.getName().toLowerCase();
                if (!name.contains("tick")) continue;
                f.setAccessible(true);
                return f.getInt(gui);
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> getChatLines(ChatComponent chat) {
        try {
            if (CHAT_LINES_FIELD == null) {
                CHAT_LINES_FIELD = findChatListField(chat, "n", "net.minecraft.client.GuiMessage$Line");
            }
            if (CHAT_LINES_FIELD == null) return List.of();
            return (List<Object>) CHAT_LINES_FIELD.get(chat);
        } catch (Exception e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> getChatMessages(ChatComponent chat) {
        try {
            if (CHAT_MESSAGES_FIELD == null) {
                CHAT_MESSAGES_FIELD = findChatListField(chat, "m", "net.minecraft.client.GuiMessage");
            }
            if (CHAT_MESSAGES_FIELD == null) return List.of();
            return (List<Object>) CHAT_MESSAGES_FIELD.get(chat);
        } catch (Exception e) {
            return List.of();
        }
    }

    private static Field findChatListField(ChatComponent chat, String mappedName, String className) {
        try {
            Field f = ObfuscationReflectionHelper.findField(chat.getClass(), mappedName);
            f.setAccessible(true);
            return f;
        } catch (Exception ignored) {
        }

        Class<?> target = null;
        try {
            target = Class.forName(className);
        } catch (Exception ignored) {
        }

        for (Field f : chat.getClass().getDeclaredFields()) {
            if (!List.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                Object value = f.get(chat);
                if (!(value instanceof List<?> list)) continue;
                if (list.isEmpty()) {
                    if (target == null) return f;
                    continue;
                }
                Object first = list.get(0);
                if (first != null && (target == null || target.isInstance(first))) {
                    return f;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static int getChatScroll(ChatComponent chat) {
        try {
            if (CHAT_SCROLL_FIELD == null) {
                CHAT_SCROLL_FIELD = ObfuscationReflectionHelper.findField(
                        chat.getClass(),
                        "o"
                );
            }
            return CHAT_SCROLL_FIELD.getInt(chat);
        } catch (Exception e) {
            return 0;
        }
    }

    private record ActiveChatIcon(
            String slotId,
            int lineIndex,
            int addedTime,
            int lineWidth
    ) {}
}

