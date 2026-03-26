package com.jamie.jamiebingo.client.render;

import com.jamie.jamiebingo.client.ClientMinecraftUtil;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.GuiEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.jamie.jamiebingo.quest.icon.QuestIconData;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class QuestIconRenderer {

    private static final Identifier QUEST_BADGE =
            com.jamie.jamiebingo.util.IdUtil.id("jamiebingo:textures/gui/quest_badge.png");
    private static long lastSwap = 0;
    private static int frame = 0;
    private static final Map<Identifier, int[]> TEXTURE_SIZES = new HashMap<>();
    private static final Map<EntityType<?>, Boolean> ENTITY_DEBUG_LOGGED = new HashMap<>();
    private static final Map<EntityType<?>, Boolean> ENTITY_CREATED_LOGGED = new HashMap<>();
    private static final Map<Integer, GuiEntityRenderer> PIP_RENDERERS = new HashMap<>();
    private static final Map<Class<?>, java.lang.reflect.Method> VARIANT_SETTERS = new HashMap<>();
    private static final Map<Class<?>, Boolean> VARIANT_SETTER_MISSING = new HashMap<>();
    private static final Map<LivingEntity, Identifier> LAST_VARIANT = new java.util.WeakHashMap<>();
    private static net.minecraft.core.RegistryAccess CACHED_ACCESS = null;
    private static final Map<net.minecraft.resources.ResourceKey<?>, Object> REGISTRY_CACHE = new HashMap<>();
    private static final ThreadLocal<Boolean> FORCE_FIXED_GUI4_CONTEXT = ThreadLocal.withInitial(() -> false);
    private static long lastNearestAttemptMs = 0L;

    public static void pushFixedGui4Context() {
        FORCE_FIXED_GUI4_CONTEXT.set(true);
    }

    public static void popFixedGui4Context() {
        FORCE_FIXED_GUI4_CONTEXT.set(false);
    }

    public static void renderQuestIcon(
            GuiGraphics graphics,
            int x,
            int y,
            QuestIconData icon,
            int boxSize
    ) {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        Font font = ClientMinecraftUtil.getFont(mc);
        if (mc == null || font == null) return;
        ensureNearestBlockAtlasFiltering(mc);
        var pose = com.jamie.jamiebingo.util.GuiGraphicsUtil.getPose(graphics);

        if (icon == null) return;

        ItemStack mainItem = icon.mainIcon == null ? com.jamie.jamiebingo.util.ItemStackUtil.empty() : icon.mainIcon;
        List<ItemStack> rotatingItems = icon.rotatingIcons;
        Identifier mainTexture = icon.mainTexture;
        List<Identifier> rotatingTextures = icon.rotatingTextures;
        QuestIconData.TextureRegion mainRegion = icon.mainRegion;
        EntityType<? extends LivingEntity> mainEntityType = icon.mainEntityType;
        List<EntityType<? extends LivingEntity>> rotatingEntities = icon.rotatingEntities;
        Identifier mainEntityVariant = icon.mainEntityVariant;
        List<Identifier> rotatingEntityVariants = icon.rotatingEntityVariants;
        Integer mainEntityColor = icon.mainEntityColor;
        List<Integer> rotatingEntityColors = icon.rotatingEntityColors;
        ItemStack cornerItem = icon.cornerIcon == null ? com.jamie.jamiebingo.util.ItemStackUtil.empty() : icon.cornerIcon;
        Identifier cornerTexture = icon.cornerTexture;
        QuestIconData.TextureRegion cornerRegion = icon.cornerRegion;
        EntityType<? extends LivingEntity> cornerEntityType = icon.cornerEntityType;
        Integer cornerEntityColor = icon.cornerEntityColor;
        List<Integer> rotatingCornerEntityColors = icon.rotatingCornerEntityColors;
        List<EntityType<? extends LivingEntity>> rotatingCornerEntities = icon.rotatingCornerEntities;
        List<ItemStack> rotatingCornerItems = icon.rotatingCornerIcons;
        List<Identifier> rotatingCornerTextures = icon.rotatingCornerTextures;
        String numberText = icon.numberText;
        int cornerCopies = icon.cornerCopies;

        boolean hasRotation =
                (rotatingTextures != null && !rotatingTextures.isEmpty())
                        || (rotatingItems != null && !rotatingItems.isEmpty())
                        || (rotatingEntities != null && !rotatingEntities.isEmpty())
                        || (rotatingCornerTextures != null && !rotatingCornerTextures.isEmpty())
                        || (rotatingCornerItems != null && !rotatingCornerItems.isEmpty())
                        || (rotatingCornerEntities != null && !rotatingCornerEntities.isEmpty());

        if (hasRotation) {
            long now = System.currentTimeMillis();
            if (now - lastSwap > 2000) {
                lastSwap = now;
                frame++;
            }
        }

        // Animated icon if rotating (per-slot offset to prevent global sync)
        int seed = Objects.hash(
                mainItem, mainTexture, mainRegion, mainEntityType, mainEntityColor,
                cornerItem, cornerTexture, cornerRegion, cornerEntityType, cornerEntityColor
        );
        seed = 31 * seed + System.identityHashCode(icon);

        ItemStack itemIcon = mainItem;
        Identifier textureIcon = mainTexture;
        EntityType<? extends LivingEntity> entityIcon = mainEntityType;
        Integer entityColor = mainEntityColor;
        Identifier entityVariant = mainEntityVariant;
        if (rotatingTextures != null && !rotatingTextures.isEmpty()) {
            int idx = Math.floorMod(frame + seed, rotatingTextures.size());
            textureIcon = rotatingTextures.get(idx);
            mainRegion = null;
            entityIcon = null;
        } else if (rotatingItems != null && !rotatingItems.isEmpty()) {
            int idx = Math.floorMod(frame + seed, rotatingItems.size());
            itemIcon = rotatingItems.get(idx);
            entityIcon = null;
        } else if (rotatingEntities != null && !rotatingEntities.isEmpty()) {
            int idx = Math.floorMod(frame + seed, rotatingEntities.size());
            entityIcon = rotatingEntities.get(idx);
            entityColor = pickColor(rotatingEntityColors, idx);
            entityVariant = pickVariant(rotatingEntityVariants, idx);
        }

        // --- MAIN ICON CENTERED ---
        int iconX = x + (boxSize / 2) - 8;
        int iconY = y + (boxSize / 2) - 8;
        if (entityIcon != null) {
            renderEntityIcon(graphics, iconX, iconY, 16, entityIcon, entityColor, entityVariant);
        } else if (mainRegion != null) {
            renderRegion(graphics, mainRegion, iconX, iconY, 16);
        } else if (textureIcon != null) {
            renderScaledTexture(graphics, textureIcon, iconX, iconY, 16);
        } else if (itemIcon != null && !itemIcon.isEmpty()) {
            graphics.renderItem(itemIcon, iconX, iconY);
        }

        // --- CORNER ICON (Top Right, front-most) ---
        graphics.nextStratum();
        if (cornerCopies > 1) {
            var cornerPose = com.jamie.jamiebingo.util.GuiGraphicsUtil.getPose(graphics);
            float scale = 0.5f;
            int baseSize = 8;
            int gap = 1;
            int rightX = x + boxSize - 2 - baseSize;
            int leftX = rightX - baseSize - gap;
            int y0 = y + 2;
            cornerPose.pushMatrix();
            cornerPose.scale(scale, scale);
            int leftScaledX = Math.round(leftX / scale);
            int rightScaledX = Math.round(rightX / scale);
            int topScaledY = Math.round(y0 / scale);

            // If configured, use custom dual-corner visuals; otherwise fallback to two player heads.
            boolean renderedDualCustom = false;
            if (cornerEntityType != null
                    || (cornerItem != null && !cornerItem.isEmpty())
                    || cornerTexture != null
                    || cornerRegion != null) {
                if (cornerEntityType != null) {
                    renderEntityIcon(graphics, leftX, y0, 12, cornerEntityType, cornerEntityColor, null);
                } else if (cornerRegion != null) {
                    renderRegion(graphics, cornerRegion, leftScaledX, topScaledY, 16);
                } else if (cornerTexture != null) {
                    renderScaledTexture(graphics, cornerTexture, leftScaledX, topScaledY, 16);
                } else {
                    graphics.renderItem(cornerItem, leftScaledX, topScaledY);
                }

                if (rotatingCornerEntities != null && !rotatingCornerEntities.isEmpty()) {
                    int idx = Math.floorMod(frame + seed, rotatingCornerEntities.size());
                    EntityType<? extends LivingEntity> rightType = rotatingCornerEntities.get(idx);
                    Integer rightColor = pickColor(rotatingCornerEntityColors, idx);
                    renderEntityIcon(graphics, rightX, y0, 12, rightType, rightColor, null);
                } else if (rotatingCornerItems != null && !rotatingCornerItems.isEmpty()) {
                    int idx = Math.floorMod(frame + seed, rotatingCornerItems.size());
                    graphics.renderItem(rotatingCornerItems.get(idx), rightScaledX, topScaledY);
                } else if (rotatingCornerTextures != null && !rotatingCornerTextures.isEmpty()) {
                    int idx = Math.floorMod(frame + seed, rotatingCornerTextures.size());
                    renderScaledTexture(graphics, rotatingCornerTextures.get(idx), rightScaledX, topScaledY, 16);
                } else if (cornerEntityType != null) {
                    renderEntityIcon(graphics, rightX, y0, 12, cornerEntityType, cornerEntityColor, null);
                } else if (cornerRegion != null) {
                    renderRegion(graphics, cornerRegion, rightScaledX, topScaledY, 16);
                } else if (cornerTexture != null) {
                    renderScaledTexture(graphics, cornerTexture, rightScaledX, topScaledY, 16);
                } else {
                    graphics.renderItem(cornerItem, rightScaledX, topScaledY);
                }
                renderedDualCustom = true;
            }

            if (!renderedDualCustom) {
                ItemStack head = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:player_head");
                graphics.renderItem(head, leftScaledX, topScaledY);
                graphics.renderItem(head, rightScaledX, topScaledY);
            }
            cornerPose.popMatrix();

            // Optional extra mini-corner indicator at bottom-right for complex team quest overlays.
            if (!renderedDualCustom) {
                if (rotatingCornerItems != null && !rotatingCornerItems.isEmpty()) {
                    int idx = Math.floorMod(frame + seed, rotatingCornerItems.size());
                    graphics.renderItem(rotatingCornerItems.get(idx), x + boxSize - 14, y + boxSize - 14);
                } else if (rotatingCornerTextures != null && !rotatingCornerTextures.isEmpty()) {
                    int idx = Math.floorMod(frame + seed, rotatingCornerTextures.size());
                    renderScaledTexture(graphics, rotatingCornerTextures.get(idx), x + boxSize - 14, y + boxSize - 14, 12);
                } else if (rotatingCornerEntities != null && !rotatingCornerEntities.isEmpty()) {
                    int idx = Math.floorMod(frame + seed, rotatingCornerEntities.size());
                    EntityType<? extends LivingEntity> type = rotatingCornerEntities.get(idx);
                    Integer color = pickColor(rotatingCornerEntityColors, idx);
                    renderEntityIcon(graphics, x + boxSize - 14, y + boxSize - 14, 12, type, color, null);
                }
            }
        } else if (rotatingCornerTextures != null && !rotatingCornerTextures.isEmpty()) {
            int idx = Math.floorMod(frame + seed, rotatingCornerTextures.size());
            renderScaledTexture(graphics, rotatingCornerTextures.get(idx), x + boxSize - 14, y + 2, 12);
        } else if (rotatingCornerItems != null && !rotatingCornerItems.isEmpty()) {
            int idx = Math.floorMod(frame + seed, rotatingCornerItems.size());
            graphics.renderItem(
                    rotatingCornerItems.get(idx),
                    x + boxSize - 14,
                    y + 2
            );
        } else if (rotatingCornerEntities != null && !rotatingCornerEntities.isEmpty()) {
            int idx = Math.floorMod(frame + seed, rotatingCornerEntities.size());
            EntityType<? extends LivingEntity> type = rotatingCornerEntities.get(idx);
            Integer color = pickColor(rotatingCornerEntityColors, idx);
            renderEntityIcon(graphics, x + boxSize - 14, y + 2, 12, type, color, null);
            if (cornerTexture != null) {
                renderScaledTexture(graphics, cornerTexture, x + boxSize - 14, y + 2, 12);
            }
        } else if (cornerEntityType != null) {
            renderEntityIcon(graphics, x + boxSize - 14, y + 2, 12, cornerEntityType, cornerEntityColor, null);
            if (cornerTexture != null) {
                renderScaledTexture(graphics, cornerTexture, x + boxSize - 14, y + 2, 12);
            }
        } else if (cornerRegion != null) {
            renderRegion(graphics, cornerRegion, x + boxSize - 14, y + 2, 12);
        } else {
            boolean renderedItem = false;
            if (cornerItem != null && !cornerItem.isEmpty()) {
                graphics.renderItem(cornerItem, x + boxSize - 14, y + 2);
                renderedItem = true;
            }
            if (cornerTexture != null) {
                renderScaledTexture(graphics, cornerTexture, x + boxSize - 14, y + 2, 12);
            } else if (!renderedItem && cornerItem != null && !cornerItem.isEmpty()) {
                graphics.renderItem(cornerItem, x + boxSize - 14, y + 2);
            }
        }

        // --- NUMBER (Bottom Right) ---
        if (numberText != null && !numberText.isBlank()) {
            graphics.nextStratum();
            String displayText = numberText.trim();
            if ("GG".equalsIgnoreCase(displayText)) {
                int nx = x + (boxSize - font.width(displayText)) / 2;
                int ny = y + (boxSize - font.lineHeight) / 2;
                graphics.drawString(font, displayText, nx, ny, 0xFFFFFFFF, false);
            } else if ("variants".equalsIgnoreCase(displayText)) {
                float scale = 0.6f;
                int textWidth = font.width(displayText);
                int nx = x + boxSize - Math.round(textWidth * scale) - 3;
                int ny = y + boxSize - Math.round(9 * scale) - 2;
                var textPose = com.jamie.jamiebingo.util.GuiGraphicsUtil.getPose(graphics);
                textPose.pushMatrix();
                textPose.scale(scale, scale);
                graphics.drawString(font, displayText, Math.round(nx / scale), Math.round(ny / scale), 0xFFFFFFFF, false);
                textPose.popMatrix();
            } else {
                int nx = x + boxSize - font.width(displayText) - 3;
                int ny = y + boxSize - 9;
                graphics.drawString(font, displayText, nx, ny, 0xFFFFFFFF, false);
            }
        }

        // --- QUEST BADGE (Top Left, front-most) ---
        graphics.nextStratum();
        int badgeSize = 10;
        graphics.blit(RenderPipelines.GUI_TEXTURED, QUEST_BADGE, x + 1, y + 1, 0, 0, badgeSize, badgeSize, badgeSize, badgeSize);
    }

    private static void renderEntityIcon(GuiGraphics graphics, int x, int y, int size, EntityType<? extends LivingEntity> type, Integer color, Identifier variantId) {
        LivingEntity entity = createEntity(type, color);
        if (entity == null) {
            debugEntity(graphics, x, y, size, type, "null");
            return;
        }
        applyEntityVariant(entity, variantId);
        if (ENTITY_CREATED_LOGGED.putIfAbsent(type, Boolean.TRUE) == null) {
            System.out.println("[JamieBingo] Entity icon render: requested=" + type + " created=" + entity.getType());
        }

        int left = x - 6;
        int top = y - 6;
        int right = x + size + 6;
        int bottom = y + size + 6;
        int baseSize = Math.round(size * getEntityScale(type));
        int minSize = getEntityMinSize(type);
        int renderSize = Math.max(minSize, baseSize);
        float centerX = (left + right) / 2.0f;
        float centerY = (top + bottom) / 2.0f;

        // Ensure each entity renders in its own GUI stratum.
        graphics.nextStratum();
        renderEntityImmediate(graphics, entity, left, top, right, bottom, renderSize, centerX, centerY);
    }

    private static void renderEntityImmediate(
            GuiGraphics graphics,
            LivingEntity entity,
            int left,
            int top,
            int right,
            int bottom,
            int renderSize,
            float mouseX,
            float mouseY
    ) {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        int drawLeft = left;
        int drawTop = top;
        int drawRight = right;
        int drawBottom = bottom;
        int drawRenderSize = renderSize;
        float drawMouseX = mouseX;
        float drawMouseY = mouseY;

        VirtualMenuTransform menuTransform = fixedMenuTransform(mc);
        if (menuTransform.active && isFixedScaleMenuScreen(mc)) {
            drawLeft = menuTransform.transformX(left);
            drawTop = menuTransform.transformY(top);
            drawRight = menuTransform.transformX(right);
            drawBottom = menuTransform.transformY(bottom);
            drawRenderSize = Math.max(1, menuTransform.transformSize(renderSize));
            drawMouseX = menuTransform.transformX(mouseX);
            drawMouseY = menuTransform.transformY(mouseY);
        }

        float centerX = (left + right) / 2.0f;
        float centerY = (top + bottom) / 2.0f;
        float rotY = (float) Math.atan((centerX - drawMouseX) / 40.0f);
        float rotX = (float) Math.atan((centerY - drawMouseY) / 40.0f);

        Quaternionf baseRot = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf tiltRot = new Quaternionf().rotateX(rotX * 20.0f * 0.017453292f);
        baseRot.mul(tiltRot);

        EntityRenderState state = extractRenderState(entity);
        if (state instanceof LivingEntityRenderState living) {
            living.bodyRot = 180.0f + rotY * 20.0f;
            living.yRot = rotY * 20.0f;
            if (living.pose != Pose.FALL_FLYING) {
                living.xRot = -rotX * 20.0f;
            } else {
                living.xRot = 0.0f;
            }
            living.boundingBoxWidth /= living.scale;
            living.boundingBoxHeight /= living.scale;
            living.scale = 1.0f;
        }

        // Snapshot state so each icon keeps its own immutable copy.
        EntityRenderState snapshot = cloneRenderStateSameType(state);
        float baseOffset = 0.0625f;
        Vector3f pos = new Vector3f(0.0f, snapshot.boundingBoxHeight / 2.0f + baseOffset, 0.0f);
        ScreenRectangle bounds = new ScreenRectangle(drawLeft, drawTop, drawRight - drawLeft, drawBottom - drawTop);
        GuiEntityRenderState pipState = new GuiEntityRenderState(
                snapshot,
                pos,
                baseRot,
                tiltRot,
                drawLeft,
                drawTop,
                drawRight,
                drawBottom,
                drawRenderSize,
                bounds
        );
        int key = Objects.hash(drawLeft, drawTop, drawRight, drawBottom, drawRenderSize);
        GuiEntityRenderer renderer = PIP_RENDERERS.get(key);
        if (renderer == null) {
            var bufferSource = ClientMinecraftUtil.getBufferSource(ClientMinecraftUtil.getMinecraft());
            var dispatcher = ClientMinecraftUtil.getEntityRenderDispatcher(ClientMinecraftUtil.getMinecraft());
            if (bufferSource == null || dispatcher == null) return;
            renderer = new GuiEntityRenderer(bufferSource, dispatcher);
            if (PIP_RENDERERS.size() > 128) {
                PIP_RENDERERS.clear();
            }
            PIP_RENDERERS.put(key, renderer);
        }
        int guiScale = com.jamie.jamiebingo.util.WindowUtil.getGuiScale(ClientMinecraftUtil.getWindow(mc));
        renderer.prepare(pipState, graphics.getRenderState(), guiScale);
    }

    private static boolean isJamieMenuScreen(Minecraft mc) {
        var currentScreen = ClientMinecraftUtil.getScreen(mc);
        if (currentScreen == null) return false;
        String className = currentScreen.getClass().getName();
        return className.startsWith("com.jamie.jamiebingo.client.");
    }

    private static double fixedMenuRenderScale(Minecraft mc) {
        int guiScale = com.jamie.jamiebingo.util.WindowUtil.getGuiScale(ClientMinecraftUtil.getWindow(mc));
        if (guiScale <= 0) return 1.0d;
        return 4.0d / guiScale;
    }

    private static boolean isFixedScaleMenuScreen(Minecraft mc) {
        if (mc == null) return false;
        var currentScreen = ClientMinecraftUtil.getScreen(mc);
        if (currentScreen == null) return false;
        String className = currentScreen.getClass().getName();
        return className.startsWith("com.jamie.jamiebingo.client.screen.");
    }

    private static VirtualMenuTransform fixedMenuTransform(Minecraft mc) {
        if (mc == null) return VirtualMenuTransform.IDENTITY;
        Object window = ClientMinecraftUtil.getWindow(mc);
        int scaledWidth = com.jamie.jamiebingo.util.WindowUtil.getGuiScaledWidth(window);
        int scaledHeight = com.jamie.jamiebingo.util.WindowUtil.getGuiScaledHeight(window);
        if (scaledWidth <= 0 || scaledHeight <= 0) {
            return VirtualMenuTransform.IDENTITY;
        }

        double fitScale = Math.min(
                1.0d,
                Math.min(
                        scaledWidth / 640.0d,
                        scaledHeight / 360.0d
                )
        );
        if (fitScale >= 0.9999d) {
            return VirtualMenuTransform.IDENTITY;
        }
        double offsetX = (scaledWidth - (640.0d * fitScale)) * 0.5d;
        double offsetY = (scaledHeight - (360.0d * fitScale)) * 0.5d;
        return new VirtualMenuTransform(true, fitScale, offsetX, offsetY);
    }

    private static EntityRenderState extractRenderState(LivingEntity entity) {
        EntityRenderDispatcher dispatcher = ClientMinecraftUtil.getEntityRenderDispatcher(ClientMinecraftUtil.getMinecraft());
        EntityRenderer renderer = dispatcher.getRenderer(entity);
        EntityRenderState state = renderer.createRenderState(entity, 1.0f);
        state.lightCoords = 15728880;
        state.shadowPieces.clear();
        return state;
    }

    private static EntityRenderState cloneRenderStateSameType(EntityRenderState src) {
        if (src == null) return null;
        try {
            java.lang.reflect.Constructor<?> ctor = src.getClass().getDeclaredConstructor();
            ctor.setAccessible(true);
            EntityRenderState dst = (EntityRenderState) ctor.newInstance();
            shallowCopyFields(src, dst, src.getClass());
            return dst;
        } catch (Exception ignored) {
            return src;
        }
    }

    private static void shallowCopyFields(Object src, Object dst, Class<?> cls) throws IllegalAccessException {
        if (cls == null || cls == Object.class) return;
        shallowCopyFields(src, dst, cls.getSuperclass());
        for (Field field : cls.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            Object value = field.get(src);
            Object current = field.get(dst);
            if (value == null) {
                field.set(dst, null);
                continue;
            }

            if (value instanceof List<?> listValue) {
                if (current instanceof List<?> listCurrent) {
                    ((List) listCurrent).clear();
                    ((List) listCurrent).addAll(listValue);
                } else {
                    field.set(dst, new java.util.ArrayList<>(listValue));
                }
                continue;
            }

            Class<?> vClass = value.getClass();
            if (vClass.isArray() && value instanceof Object[] arr) {
                field.set(dst, arr.clone());
                continue;
            }

            if (value instanceof net.minecraft.client.renderer.item.ItemStackRenderState) {
                if (current instanceof net.minecraft.client.renderer.item.ItemStackRenderState state) {
                    state.clear();
                } else {
                    field.set(dst, null);
                }
                continue;
            }

            field.set(dst, value);
        }
    }

    private static void debugEntity(GuiGraphics graphics, int x, int y, int size, EntityType<? extends LivingEntity> type, String note) {
        if (type == null) return;
        if (ENTITY_DEBUG_LOGGED.putIfAbsent(type, Boolean.TRUE) == null) {
            System.out.println("[JamieBingo] Entity icon failed: " + type + " (" + note + ")");
        }
    }


    private static void renderScaledTexture(GuiGraphics graphics, Identifier texture, int x, int y, int targetSize) {
        var pose = com.jamie.jamiebingo.util.GuiGraphicsUtil.getPose(graphics);
        if (isSpriteId(texture)) {
            try {
                // Keep all sprite-like quest icons on the same atlas sampling path as sharp item icons.
                graphics.blitSprite(RenderPipelines.GUI, texture, x, y, targetSize, targetSize);
                return;
            } catch (Throwable ignored) {
                if (!isBlockSpriteId(texture)) {
                    return;
                }
                // Fallback for edge-case block sprites when atlas lookup fails.
                Identifier direct = com.jamie.jamiebingo.util.IdUtil.id(
                        texture.getNamespace() + ":textures/" + texture.getPath() + ".png"
                );
                graphics.blit(RenderPipelines.GUI_TEXTURED, direct, x, y, 0, 0, targetSize, targetSize, targetSize, targetSize);
            }
            return;
        }
        int[] size = getTextureSize(texture);
        int width = size[0];
        int height = size[1];

        if (width <= 0 || height <= 0) {
            return;
        }

        // Animated textures (e.g., water/fire/portal) are tall; sample the top 16x16 frame.
        if (width == 16 && height > 16) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, targetSize, targetSize, 16, 16, width, height);
            return;
        }

        float scale = targetSize / (float) Math.max(width, height);
        float drawW = width * scale;
        float drawH = height * scale;
        float offsetX = (targetSize - drawW) / 2f;
        float offsetY = (targetSize - drawH) / 2f;

        pose.pushMatrix();
        pose.scale(scale, scale);
        pose.translate((x + offsetX) / scale, (y + offsetY) / scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0, width, height, width, height);
        pose.popMatrix();
    }

    private static void renderRegion(GuiGraphics graphics, QuestIconData.TextureRegion region, int x, int y, int targetSize) {
        var pose = com.jamie.jamiebingo.util.GuiGraphicsUtil.getPose(graphics);
        if (region == null) return;

        float scale = targetSize / (float) Math.max(region.width, region.height);
        float drawW = region.width * scale;
        float drawH = region.height * scale;
        float offsetX = (targetSize - drawW) / 2f;
        float offsetY = (targetSize - drawH) / 2f;

        pose.pushMatrix();
        pose.scale(scale, scale);
        pose.translate((x + offsetX) / scale, (y + offsetY) / scale);

        graphics.blit(region.texture, 0, 0, region.width, region.height, region.u, region.v, region.width, region.height);

        pose.popMatrix();
    }

    private static int[] getTextureSize(Identifier texture) {
        int[] cached = TEXTURE_SIZES.get(texture);
        if (cached != null) return cached;

        int[] size = new int[] {16, 16};
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        try {
            Resource res = mc.getResourceManager().getResource(texture).orElse(null);
            if (res != null) {
                try (var in = res.open()) {
                    NativeImage img = NativeImage.read(in);
                    size = new int[] {img.getWidth(), img.getHeight()};
                }
            }
        } catch (Exception ignored) {
        }

        TEXTURE_SIZES.put(texture, size);
        return size;
    }

    private static LivingEntity createEntity(EntityType<? extends LivingEntity> type, Integer color) {
        if (type == null) return null;
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        net.minecraft.client.multiplayer.ClientLevel level = ClientMinecraftUtil.getLevel(mc);
        if (level == null) return null;

        LivingEntity entity = type.create(level, EntitySpawnReason.COMMAND);
        if (entity == null) {
            entity = type.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        }
        if (entity == null) {
            entity = type.create(level, EntitySpawnReason.SPAWNER);
        }
        if (entity == null) {
            entity = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        }
        if (entity == null) return null;

        entity.setPos(0.0, 0.0, 0.0);
        entity.setInvisible(false);
        entity.setHealth(entity.getMaxHealth());
        entity.setSilent(true);
        entity.setYRot(0.0f);
        entity.setXRot(0.0f);
        entity.setYHeadRot(0.0f);
        entity.yBodyRot = 0.0f;
        boolean makeBaby = color != null && color < 0;
        if (!makeBaby && color != null && entity instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep) {
            net.minecraft.world.item.DyeColor dye = net.minecraft.world.item.DyeColor.byId(color);
            if (dye != null) {
                sheep.setColor(dye);
            }
        }
        if (makeBaby) {
            setEntityBaby(entity);
        }
        if (entity instanceof net.minecraft.world.entity.monster.zombie.ZombieVillager zv) {
            net.minecraft.world.level.Level zvLevel = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(zv);
            if (zvLevel != null) {
                zv.setVillagerData(zv.getVillagerData()
                        .withType(zvLevel.registryAccess(), VillagerType.PLAINS)
                        .withProfession(zvLevel.registryAccess(), VillagerProfession.NONE));
            }
        }
        return entity;
    }

    private static void setEntityBaby(LivingEntity entity) {
        if (entity == null) return;
        if (entity instanceof net.minecraft.world.entity.AgeableMob ageable) {
            ageable.setBaby(true);
            return;
        }
        try {
            java.lang.reflect.Method m = entity.getClass().getMethod("setBaby", boolean.class);
            m.invoke(entity, true);
            return;
        } catch (Throwable ignored) {
        }
        try {
            java.lang.reflect.Method m = entity.getClass().getMethod("setBaby");
            m.invoke(entity);
        } catch (Throwable ignored) {
        }
    }

    private static boolean isSpriteId(Identifier id) {
        if (id == null) return false;
        String path = id.getPath();
        return path.startsWith("gui/") || path.startsWith("hud/") || path.startsWith("mob_effect/")
                || path.startsWith("block/") || path.startsWith("advancements/");
    }

    private static boolean isBlockSpriteId(Identifier id) {
        if (id == null) return false;
        return id.getPath().startsWith("block/");
    }

    private static void ensureNearestBlockAtlasFiltering(Minecraft mc) {
        long now = System.currentTimeMillis();
        if (now - lastNearestAttemptMs < 1000L) return;
        lastNearestAttemptMs = now;
        trySetNearest(mc, com.jamie.jamiebingo.util.IdUtil.id("textures/atlas/blocks.png"));
    }

    private static void trySetNearest(Minecraft mc, Identifier id) {
        if (mc == null || id == null) return;
        try {
            Object texture = mc.getTextureManager().getTexture(id);
            if (texture == null) return;
            java.lang.reflect.Method setFilter = findSetFilter(texture.getClass());
            if (setFilter != null) {
                setFilter.setAccessible(true);
                setFilter.invoke(texture, false, false);
            }
        } catch (Throwable ignored) {
        }
    }

    private static java.lang.reflect.Method findSetFilter(Class<?> cls) {
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod("setFilter", boolean.class, boolean.class);
            } catch (Throwable ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Integer pickColor(List<Integer> colors, int idx) {
        if (colors == null || colors.isEmpty()) return null;
        if (idx < 0 || idx >= colors.size()) return null;
        return colors.get(idx);
    }

    private static Identifier pickVariant(List<Identifier> variants, int idx) {
        if (variants == null || variants.isEmpty()) return null;
        if (idx < 0 || idx >= variants.size()) return null;
        return variants.get(idx);
    }

    private static void applyEntityVariant(LivingEntity entity, Identifier variantId) {
        if (entity == null || variantId == null) return;
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        net.minecraft.client.multiplayer.ClientLevel level = ClientMinecraftUtil.getLevel(mc);
        if (level == null) return;

        try {
            net.minecraft.core.RegistryAccess access = level.registryAccess();
            if (CACHED_ACCESS != access) {
                CACHED_ACCESS = access;
                REGISTRY_CACHE.clear();
                LAST_VARIANT.clear();
                VARIANT_SETTERS.clear();
                VARIANT_SETTER_MISSING.clear();
            }
            net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<?>> registryKey = null;
            EntityType<?> type = entity.getType();
            if (type == EntityType.WOLF) {
                registryKey = net.minecraft.core.registries.Registries.WOLF_VARIANT;
            } else if (type == EntityType.PIG) {
                registryKey = net.minecraft.core.registries.Registries.PIG_VARIANT;
            } else if (type == EntityType.COW) {
                registryKey = net.minecraft.core.registries.Registries.COW_VARIANT;
            } else if (type == EntityType.CHICKEN) {
                registryKey = net.minecraft.core.registries.Registries.CHICKEN_VARIANT;
            } else if (type == EntityType.CAT) {
                registryKey = net.minecraft.core.registries.Registries.CAT_VARIANT;
            } else {
                return;
            }

            Identifier last = LAST_VARIANT.get(entity);
            if (variantId.equals(last)) {
                return;
            }

            @SuppressWarnings({"unchecked", "rawtypes"})
            net.minecraft.resources.ResourceKey<?> key =
                    net.minecraft.resources.ResourceKey.create((net.minecraft.resources.ResourceKey) registryKey, variantId);

            Object reg = REGISTRY_CACHE.get(registryKey);
            if (reg == null) {
                reg = invokeMethod(access, "registryOrThrow", registryKey);
                if (reg == null) {
                    Object opt = invokeMethod(access, "registry", registryKey);
                    if (opt instanceof java.util.Optional<?> o) {
                        reg = o.orElse(null);
                    }
                }
                if (reg != null) {
                    REGISTRY_CACHE.put(registryKey, reg);
                }
            }
            if (reg == null) return;

            Object holderObj = invokeMethod(reg, "getHolder", key);
            if (holderObj == null) {
                holderObj = invokeMethod(reg, "getHolderOrThrow", key);
            }
            if (holderObj instanceof java.util.Optional<?> opt) {
                holderObj = opt.orElse(null);
            }
            if (!(holderObj instanceof net.minecraft.core.Holder<?> holder)) return;

            java.lang.reflect.Method method = VARIANT_SETTERS.get(entity.getClass());
            if (method == null && !VARIANT_SETTER_MISSING.containsKey(entity.getClass())) {
                for (java.lang.reflect.Method m : entity.getClass().getMethods()) {
                    if (m.getParameterCount() != 1) continue;
                    Class<?> param = m.getParameterTypes()[0];
                    if (!net.minecraft.core.Holder.class.isAssignableFrom(param)) continue;
                    String name = m.getName().toLowerCase();
                    if (name.contains("variant") && name.contains("set")) {
                        method = m;
                        break;
                    }
                    if (name.contains("variant")) {
                        method = m;
                    }
                }
                if (method == null) {
                    for (java.lang.reflect.Method m : entity.getClass().getMethods()) {
                        if (m.getParameterCount() != 1) continue;
                        Class<?> param = m.getParameterTypes()[0];
                        if (!net.minecraft.core.Holder.class.isAssignableFrom(param)) continue;
                        if (m.getReturnType() != Void.TYPE) continue;
                        method = m;
                        break;
                    }
                }
                if (method != null) {
                    VARIANT_SETTERS.put(entity.getClass(), method);
                } else {
                    VARIANT_SETTER_MISSING.put(entity.getClass(), true);
                }
            }
            if (method != null) {
                try {
                    method.invoke(entity, holder);
                    LAST_VARIANT.put(entity, variantId);
                } catch (Throwable ignored) {
                }
            } else {
                if (trySetVariantDataAccessor(entity, holder)) {
                    LAST_VARIANT.put(entity, variantId);
                }
                // Fallback: set a Holder field that looks like a variant
                try {
                    for (java.lang.reflect.Field f : getAllFields(entity.getClass())) {
                        if (!net.minecraft.core.Holder.class.isAssignableFrom(f.getType())) continue;
                        if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                        String name = f.getName().toLowerCase();
                        if (!name.contains("variant")) continue;
                        f.setAccessible(true);
                        f.set(entity, holder);
                        LAST_VARIANT.put(entity, variantId);
                        break;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean trySetVariantDataAccessor(LivingEntity entity, net.minecraft.core.Holder<?> holder) {
        try {
            Object data = entity.getEntityData();
            if (data == null) return false;
            java.lang.reflect.Method getter = null;
            java.lang.reflect.Method setter = null;
            for (java.lang.reflect.Method m : data.getClass().getMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (m.getParameterCount() == 1 && params[0] == net.minecraft.network.syncher.EntityDataAccessor.class) {
                    if (m.getReturnType() != Void.TYPE) {
                        getter = m;
                    }
                }
                if (m.getParameterCount() == 2 && params[0] == net.minecraft.network.syncher.EntityDataAccessor.class) {
                    if (m.getReturnType() == Void.TYPE) {
                        setter = m;
                    }
                }
            }
            if (setter == null) return false;
            for (java.lang.reflect.Field f : getAllFields(entity.getClass())) {
                if (!net.minecraft.network.syncher.EntityDataAccessor.class.isAssignableFrom(f.getType())) continue;
                if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                Object accessor = f.get(null);
                if (accessor == null) continue;
                try {
                    if (getter != null) {
                        Object current = getter.invoke(data, accessor);
                        if (current != null && !(current instanceof net.minecraft.core.Holder<?>)) continue;
                    }
                    setter.invoke(data, accessor, holder);
                    return true;
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static java.lang.reflect.Field[] getAllFields(Class<?> type) {
        java.util.List<java.lang.reflect.Field> fields = new java.util.ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            fields.addAll(java.util.Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields.toArray(new java.lang.reflect.Field[0]);
    }

    private static Object invokeMethod(Object target, String name, Object arg) {
        if (target == null) return null;
        try {
            for (java.lang.reflect.Method m : target.getClass().getMethods()) {
                if (!m.getName().equals(name)) continue;
                if (m.getParameterCount() != 1) continue;
                return m.invoke(target, arg);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static float getEntityScale(EntityType<? extends LivingEntity> type) {
        if (type == null) return 1.0f;
        if (isEntityPath(type, "happy_ghast")) return 0.015f;
        if (type == EntityType.PARROT) return 1.8f;
        if (type == EntityType.ENDER_DRAGON) return 0.02f;
        if (type == EntityType.GHAST) return 0.018f;
        if (type == EntityType.IRON_GOLEM) return 0.45f;
        if (type == EntityType.GUARDIAN) return 0.6f;
        if (type == EntityType.ELDER_GUARDIAN) return 0.8f;
        if (type == EntityType.ENDERMAN) return 0.7f;
        if (type == EntityType.STRAY) return 0.85f;
        if (type == EntityType.SKELETON || type == EntityType.ZOMBIE) return 0.9f;
        if (type == EntityType.LLAMA) return 0.6f;
        if (type == EntityType.HORSE) return 0.7f;
        if (type == EntityType.CAMEL) return 0.18f;
        if (type == EntityType.WARDEN) return 0.20f;
        if (type == EntityType.RAVAGER) return 0.25f;
        if (type == EntityType.VILLAGER || type == EntityType.ZOMBIE_VILLAGER || type == EntityType.SNOW_GOLEM) return 0.8f;
        if (type == EntityType.ZOMBIFIED_PIGLIN || type == EntityType.STRIDER) return 0.5f;
        return 1.0f;
    }

    private static int getEntityMinSize(EntityType<? extends LivingEntity> type) {
        if (type == null) return 12;
        if (isHugeEntity(type)) return 4;
        if (type == EntityType.CAMEL || type == EntityType.WARDEN) return 9;
        if (type == EntityType.RAVAGER) return 8;
        return 12;
    }

    private static boolean isHugeEntity(EntityType<? extends LivingEntity> type) {
        return type == EntityType.ENDER_DRAGON
                || type == EntityType.GHAST
                || isEntityPath(type, "happy_ghast")
                || type == EntityType.ELDER_GUARDIAN;
    }

    private static boolean isEntityPath(EntityType<? extends LivingEntity> type, String path) {
        if (type == null || path == null) return false;
        try {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            return id != null && path.equals(id.getPath());
        } catch (Exception ignored) {
            return false;
        }
    }

    private record VirtualMenuTransform(boolean active, double scale, double offsetX, double offsetY) {
        private static final VirtualMenuTransform IDENTITY = new VirtualMenuTransform(false, 1.0d, 0.0d, 0.0d);

        int transformX(int x) {
            return (int) Math.round((x * scale) + offsetX);
        }

        int transformY(int y) {
            return (int) Math.round((y * scale) + offsetY);
        }

        float transformX(float x) {
            return (float) ((x * scale) + offsetX);
        }

        float transformY(float y) {
            return (float) ((y * scale) + offsetY);
        }

        int transformSize(int size) {
            return (int) Math.round(size * scale);
        }
    }
}

