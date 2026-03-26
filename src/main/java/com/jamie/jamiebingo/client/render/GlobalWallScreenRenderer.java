package com.jamie.jamiebingo.client.render;

import com.jamie.jamiebingo.JamieBingo;
import com.jamie.jamiebingo.bingo.CardComposition;
import com.jamie.jamiebingo.client.ClientMinecraftUtil;
import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import com.jamie.jamiebingo.client.screen.BingoControllerScreen;
import com.jamie.jamiebingo.menu.ControllerSettingsSnapshot;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketGlobalWallAction;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.AddFramePassEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

public final class GlobalWallScreenRenderer {
    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger(com.jamie.jamiebingo.JamieBingo.MOD_ID);
    private static final Identifier PASS_ID = Identifier.fromNamespaceAndPath("jamiebingo", "global_wall_screen_pass");
    private static final Identifier WALL_TEXTURE_ID = Identifier.fromNamespaceAndPath("jamiebingo", "global_wall_screen");
    private static final int GUI_WIDTH = 640;
    private static final int GUI_HEIGHT = 340;
    private static final int OFFSCREEN_SCALE = 1;
    private static final int BOX_HALF = 40;
    private static final double PANEL_WORLD_WIDTH = 69.0D;
    private static final double PANEL_WORLD_HEIGHT = 32.0D;
    private static final double PANEL_BOTTOM_OFFSET = 3.0D;
    private static final int START_W = 260;
    private static final int START_H = 28;
    private static final int SCREEN_X = 0;
    private static final int SCREEN_Y = 0;

    private static boolean active;
    private static BlockPos center;
    private static ControllerSettingsSnapshot serverSnapshot;
    private static ControllerSettingsSnapshot lastSentSnapshot;
    private static int serverSettingsPage;
    private static int lastSentSettingsPage;
    private static BingoControllerScreen wallScreen;
    private static TextureTarget offscreenTarget;
    private static WrappedRenderTargetTexture wrappedTexture;
    private static java.lang.reflect.Method resolveSnapshotMethod;
    private static java.lang.reflect.Field minecraftMainTargetField;
    private static java.lang.reflect.Field gameFogRendererField;
    private static java.lang.reflect.Method fogGetBufferMethod;
    private static GuiRenderer wallGuiRenderer;
    private static GuiRenderState wallGuiRenderState;
    private static SubmitNodeStorage wallSubmitNodes;
    private static FeatureRenderDispatcher wallFeatureDispatcher;
    private static ByteBufferBuilder wallMainBuilder;
    private static ByteBufferBuilder wallCrumblingBuilder;
    private static MultiBufferSource.BufferSource wallBufferSource;
    private static MultiBufferSource.BufferSource wallCrumblingBufferSource;
    private static OutlineBufferSource wallOutlineBufferSource;

    private static boolean rightDragging;
    private static int lastGuiX;
    private static int lastGuiY;
    private static long lastDebugLogMs;

    private GlobalWallScreenRenderer() {
    }

    public static void register() {
        AddFramePassEvent.BUS.addListener(GlobalWallScreenRenderer::onAddFramePass);
        InputEvent.MouseButton.Pre.BUS.addListener(GlobalWallScreenRenderer::onMouseButtonPre);
        TickEvent.ClientTickEvent.Post.BUS.addListener(GlobalWallScreenRenderer::onClientTickPost);
        RenderHighlightEvent.Block.BUS.addListener(GlobalWallScreenRenderer::onRenderHighlightBlock);
    }

    public static void onServerSync(boolean nowActive, BlockPos nowCenter, ControllerSettingsSnapshot snapshot, int settingsPage) {
        // Never allow pregame wall rendering to reactivate once casino/gameplay is active.
        if (ClientCasinoState.isActive()) {
            forceDeactivate();
            if (snapshot != null) {
                serverSnapshot = snapshot;
                lastSentSnapshot = snapshot;
                serverSettingsPage = Math.max(0, settingsPage);
                lastSentSettingsPage = serverSettingsPage;
            }
            return;
        }
        active = nowActive;
        center = nowCenter;
        serverSettingsPage = Math.max(0, settingsPage);
        if (!active) {
            rightDragging = false;
        }
        LOGGER.info("[JamieBingo] GlobalWall sync received: active={} center={} snapshot={} page={}",
                nowActive, nowCenter, snapshot != null, serverSettingsPage);
        if (snapshot != null) {
            boolean changed = !snapshot.equals(serverSnapshot);
            serverSnapshot = snapshot;
            if (wallScreen == null || (changed && !rightDragging)) {
                rebuildWallScreenFromSnapshot();
            } else if (wallScreen != null) {
                wallScreen.setSettingsPage(serverSettingsPage);
            }
            // Treat authoritative server sync as latest sent to avoid noisy resend loops.
            lastSentSnapshot = snapshot;
            lastSentSettingsPage = serverSettingsPage;
        }
    }

    public static void forceDeactivate() {
        active = false;
        center = null;
        rightDragging = false;
        wallScreen = null;
        serverSnapshot = null;
        lastSentSnapshot = null;
        serverSettingsPage = 0;
        lastSentSettingsPage = 0;
    }

    private static void onAddFramePass(AddFramePassEvent event) {
        event.addPass(PASS_ID, new net.minecraftforge.client.FramePassManager.PassDefinition() {
            @Override
            public void extracts(net.minecraft.client.renderer.LevelTargetBundle bundle, com.mojang.blaze3d.framegraph.FramePass pass) {
                pass.readsAndWrites(bundle.main);
                // Force this pass late in the frame graph so world geometry doesn't overwrite the wall quad.
                if (bundle.translucent != null) pass.reads(bundle.translucent);
                if (bundle.itemEntity != null) pass.reads(bundle.itemEntity);
                if (bundle.particles != null) pass.reads(bundle.particles);
                if (bundle.weather != null) pass.reads(bundle.weather);
                if (bundle.clouds != null) pass.reads(bundle.clouds);
                if (bundle.entityOutline != null) pass.reads(bundle.entityOutline);
            }

            @Override
            public void executes(LevelRenderState state) {
                renderPass(state);
            }
        });
    }

    private static void renderPass(LevelRenderState state) {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        if (ClientCasinoState.isActive()) return;
        if (mc == null || !active || center == null) return;
        var player = ClientMinecraftUtil.getPlayer(mc);
        if (player == null || player.level() == null) return;

        ensureWallScreen();
        if (wallScreen == null) return;

        GuiHit hit = projectLookToScreen(player);
        int mouseX = hit == null ? -1000 : hit.guiX;
        int mouseY = hit == null ? -1000 : hit.guiY;

        renderWallGuiToOffscreen(mc, mouseX, mouseY);
        renderWorldQuad(mc, state);
        long now = System.currentTimeMillis();
        if (now - lastDebugLogMs >= 1000L) {
            lastDebugLogMs = now;
            LOGGER.info("[JamieBingo] GlobalWall render pass tick: center={} offscreen={}x{} wallScreen={}",
                    center,
                    offscreenTarget == null ? -1 : offscreenTarget.width,
                    offscreenTarget == null ? -1 : offscreenTarget.height,
                    wallScreen != null);
        }
    }

    private static void renderWallGuiToOffscreen(Minecraft mc, int mouseX, int mouseY) {
        if (mc == null) return;
        int offscreenWidth = GUI_WIDTH * OFFSCREEN_SCALE;
        int offscreenHeight = GUI_HEIGHT * OFFSCREEN_SCALE;
        if (offscreenTarget == null || offscreenTarget.width != offscreenWidth || offscreenTarget.height != offscreenHeight) {
            offscreenTarget = new TextureTarget("jamiebingo_global_wall_gui", offscreenWidth, offscreenHeight, true);
        }

        var color = offscreenTarget.getColorTexture();
        var depth = offscreenTarget.getDepthTexture();
        if (color == null || depth == null) return;
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(color, 0xFF000000, depth, 1.0D);

        ensureDedicatedGuiRenderer(mc);
        if (wallGuiRenderer == null || wallGuiRenderState == null) return;

        RenderSystem.outputColorTextureOverride = offscreenTarget.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = offscreenTarget.getDepthTextureView();
        RenderTarget previousMainTarget = null;
        boolean swapped = false;
        try {
            previousMainTarget = swapMinecraftMainTarget(mc, offscreenTarget);
            swapped = previousMainTarget != null;
            wallGuiRenderState.reset();
            GuiGraphics graphics = new GuiGraphics(mc, wallGuiRenderState, mouseX, mouseY);
            var pose = graphics.pose();
            pose.pushMatrix();

            graphics.fill(0, 0, GUI_WIDTH, GUI_HEIGHT, 0xFF101217);

            renderWallWidgets(graphics, mouseX, mouseY);
            if (mouseX >= 0 && mouseY >= 0 && mouseX < GUI_WIDTH && mouseY < GUI_HEIGHT) {
                graphics.fill(mouseX - 3, mouseY - 1, mouseX + 4, mouseY + 1, 0xE0FFFFFF);
                graphics.fill(mouseX - 1, mouseY - 3, mouseX + 1, mouseY + 4, 0xE0FFFFFF);
                AbstractWidget hovered = findWidgetAt(mouseX, mouseY);
                if (hovered != null) {
                    int hx = hovered.getX();
                    int hy = hovered.getY();
                    int hw = hovered.getWidth();
                    int hh = hovered.getHeight();
                    graphics.fill(hx - 1, hy - 1, hx + hw + 1, hy, 0xCC5EC8FF);
                    graphics.fill(hx - 1, hy + hh, hx + hw + 1, hy + hh + 1, 0xCC5EC8FF);
                    graphics.fill(hx - 1, hy, hx, hy + hh, 0xCC5EC8FF);
                    graphics.fill(hx + hw, hy, hx + hw + 1, hy + hh, 0xCC5EC8FF);
                }
            }
            pose.popMatrix();

            GpuBufferSlice fogBuffer = resolveFogBuffer(mc);
            if (fogBuffer != null) {
                wallGuiRenderer.render(fogBuffer);
            }
            wallGuiRenderer.incrementFrameNumber();
            wallGuiRenderState.reset();
            if (wallBufferSource != null) wallBufferSource.endBatch();
            if (wallCrumblingBufferSource != null) wallCrumblingBufferSource.endBatch();
            if (wallOutlineBufferSource != null) wallOutlineBufferSource.endOutlineBatch();
            if (wallFeatureDispatcher != null) {
                wallFeatureDispatcher.endFrame();
            }
            if (wallSubmitNodes != null) {
                wallSubmitNodes.endFrame();
            }
        } finally {
            if (swapped) {
                restoreMinecraftMainTarget(mc, previousMainTarget);
            }
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
        }

        if (wrappedTexture == null) {
            wrappedTexture = new WrappedRenderTargetTexture();
            mc.getTextureManager().register(WALL_TEXTURE_ID, wrappedTexture);
        }
        wrappedTexture.bind(mc, offscreenTarget);
    }

    private static void renderWorldQuad(Minecraft mc, LevelRenderState state) {
        if (offscreenTarget == null || center == null || mc == null) return;
        PanelBounds panel = computePanelBounds();
        if (panel == null) return;
        Vec3 camPos = (state == null || state.cameraRenderState == null || state.cameraRenderState.pos == null)
                ? Vec3.ZERO
                : state.cameraRenderState.pos;

        PoseStack pose = new PoseStack();

        // No-cull to ensure the quad is visible regardless of winding/camera side.
        RenderType type = RenderTypes.entityTranslucent(WALL_TEXTURE_ID);
        var p = pose.last();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, type.format());
        VertexConsumer vc = bb;
        float x0 = (float) (panel.xMin - camPos.x);
        float x1 = (float) (panel.xMax - camPos.x);
        float y0 = (float) (panel.yMin - camPos.y);
        float y1 = (float) (panel.yMax - camPos.y);
        float z = (float) (panel.z - camPos.z);
        vc.addVertex(p, x0, y1, z).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
        vc.addVertex(p, x1, y1, z).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
        vc.addVertex(p, x1, y0, z).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
        vc.addVertex(p, x0, y0, z).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
        type.draw(bb.buildOrThrow());
    }

    private static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (!active || center == null) return;
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;

        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        var player = ClientMinecraftUtil.getPlayer(mc);
        if (mc == null || player == null || wallScreen == null) return;
        if (ClientMinecraftUtil.getScreen(mc) != null) return;

        GuiHit hit = projectLookToScreen(player);
        if (hit == null) return;
        lastGuiX = hit.guiX;
        lastGuiY = hit.guiY;
        MouseButtonEvent mouse = new MouseButtonEvent(
                hit.guiX - SCREEN_X,
                hit.guiY - SCREEN_Y,
                new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, event.getModifiers())
        );

        if (event.getAction() == com.mojang.blaze3d.platform.InputConstants.PRESS) {
            rightDragging = true;
            wallScreen.mouseClicked(mouse, false);
            sendSnapshotIfChanged();
            cancelMouseEvent(event);
            return;
        }

        if (event.getAction() == com.mojang.blaze3d.platform.InputConstants.RELEASE) {
            rightDragging = false;
            wallScreen.mouseReleased(mouse);
            sendSnapshotIfChanged();
            cancelMouseEvent(event);
        }
    }

    private static void onClientTickPost(TickEvent.ClientTickEvent.Post event) {
        if (!active || !rightDragging || wallScreen == null) return;
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        var player = ClientMinecraftUtil.getPlayer(mc);
        if (mc == null || player == null || ClientMinecraftUtil.getScreen(mc) != null) return;

        GuiHit hit = projectLookToScreen(player);
        if (hit == null) return;
        int dx = hit.guiX - lastGuiX;
        int dy = hit.guiY - lastGuiY;
        lastGuiX = hit.guiX;
        lastGuiY = hit.guiY;

        MouseButtonEvent mouse = new MouseButtonEvent(
                hit.guiX - SCREEN_X,
                hit.guiY - SCREEN_Y,
                new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0)
        );
        wallScreen.mouseDragged(mouse, dx, dy);
        sendSnapshotIfChanged();
    }

    private static void onRenderHighlightBlock(RenderHighlightEvent.Block event) {
        if (event == null || !active || center == null) return;
        event.setCustomRenderer((source, stack, translucent, levelState) -> {
            if (source == null || stack == null || levelState == null || levelState.cameraRenderState == null) return;
            renderWorldQuadFromSource(source, stack, levelState.cameraRenderState.pos);
        });
    }

    private static void renderWorldQuadFromSource(net.minecraft.client.renderer.MultiBufferSource.BufferSource source, PoseStack stack, Vec3 camPos) {
        if (source == null || stack == null || center == null || camPos == null) return;
        PanelBounds panel = computePanelBounds();
        if (panel == null) return;

        double rx0 = panel.xMin - camPos.x;
        double rx1 = panel.xMax - camPos.x;
        double ry0 = panel.yMin - camPos.y;
        double ry1 = panel.yMax - camPos.y;
        double rz = panel.z - camPos.z;

        var pose = stack.last();
        RenderType type = RenderTypes.entityTranslucent(WALL_TEXTURE_ID);
        VertexConsumer vc = source.getBuffer(type);
        vc.addVertex(pose, (float) rx0, (float) ry1, (float) rz).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
        vc.addVertex(pose, (float) rx1, (float) ry1, (float) rz).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
        vc.addVertex(pose, (float) rx1, (float) ry0, (float) rz).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
        vc.addVertex(pose, (float) rx0, (float) ry0, (float) rz).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0x00F000F0).setNormal(0, 0, 1);
    }

    private static void sendSnapshotIfChanged() {
        ControllerSettingsSnapshot now = resolveSnapshot();
        int currentPage = wallScreen == null ? serverSettingsPage : wallScreen.getSettingsPage();
        if (now == null) return;
        if (now.equals(lastSentSnapshot) && currentPage == lastSentSettingsPage) return;
        lastSentSnapshot = now;
        lastSentSettingsPage = currentPage;
        NetworkHandler.sendToServer(new PacketGlobalWallAction(now, false, currentPage));
    }

    private static void sendStartWithCurrentSnapshot() {
        ControllerSettingsSnapshot now = resolveSnapshot();
        int currentPage = wallScreen == null ? serverSettingsPage : wallScreen.getSettingsPage();
        if (now != null) {
            lastSentSnapshot = now;
        }
        lastSentSettingsPage = currentPage;
        NetworkHandler.sendToServer(new PacketGlobalWallAction(now, true, currentPage));
    }

    private static void renderWallWidgets(GuiGraphics graphics, int mouseX, int mouseY) {
        if (wallScreen == null || graphics == null) return;
        wallScreen.setFixedGuiScaleEnabled(true);
        var pose = graphics.pose();
        boolean pushed = false;
        try {
            Minecraft mc = ClientMinecraftUtil.getMinecraft();
            double sx = wallRenderScaleX(mc);
            double sy = wallRenderScaleY(mc);
            pose.pushMatrix();
            pushed = true;
            pose.scale((float) sx, (float) sy);
            wallScreen.render(
                    graphics,
                    mouseX,
                    mouseY,
                    0.0F
            );
            return;
        } catch (Throwable ignored) {
        } finally {
            if (pushed) {
                pose.popMatrix();
            }
        }
    }

    private static double wallRenderScaleX(Minecraft mc) {
        if (mc == null) return 1.0d;
        Object window = com.jamie.jamiebingo.client.ClientMinecraftUtil.getWindow(mc);
        int scaledW = com.jamie.jamiebingo.util.WindowUtil.getGuiScaledWidth(window);
        if (scaledW <= 0) return 1.0d;
        return Math.max(0.0001d, scaledW / (double) GUI_WIDTH);
    }

    private static double wallRenderScaleY(Minecraft mc) {
        if (mc == null) return 1.0d;
        Object window = com.jamie.jamiebingo.client.ClientMinecraftUtil.getWindow(mc);
        int scaledH = com.jamie.jamiebingo.util.WindowUtil.getGuiScaledHeight(window);
        if (scaledH <= 0) return 1.0d;
        return Math.max(0.0001d, scaledH / (double) GUI_HEIGHT);
    }

    private static AbstractWidget findWidgetAt(int guiX, int guiY) {
        if (wallScreen == null) return null;
        try {
            for (var r : wallScreen.renderables) {
                if (r instanceof AbstractWidget widget && widget.isMouseOver(guiX, guiY)) {
                    return widget;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static ControllerSettingsSnapshot resolveSnapshot() {
        if (wallScreen == null) return null;
        try {
            if (resolveSnapshotMethod == null) {
                try {
                    resolveSnapshotMethod = BingoControllerScreen.class.getDeclaredMethod("resolveSettingsSnapshotRaw");
                } catch (NoSuchMethodException ignored) {
                    resolveSnapshotMethod = BingoControllerScreen.class.getDeclaredMethod("resolveSettingsSnapshot");
                }
                resolveSnapshotMethod.setAccessible(true);
            }
            Object out = resolveSnapshotMethod.invoke(wallScreen);
            return out instanceof ControllerSettingsSnapshot s ? s : null;
        } catch (Throwable t) {
            JamieBingo.MOD_ID.length(); // keep no-op to avoid stripping in obf weirdness
            return null;
        }
    }

    private static void ensureWallScreen() {
        if (wallScreen != null) return;
        if (serverSnapshot == null) {
            serverSnapshot = defaultSnapshot();
        }
        rebuildWallScreenFromSnapshot();
    }

    private static void rebuildWallScreenFromSnapshot() {
        ControllerSettingsSnapshot s = serverSnapshot;
        if (s == null) return;
        CardComposition composition = s.questMode() == 1 ? CardComposition.HYBRID_CATEGORY
                : s.questMode() == 2 ? CardComposition.HYBRID_PERCENT : CardComposition.CLASSIC_ONLY;
        boolean randomCardDifficulty = "random".equalsIgnoreCase(s.cardDifficulty());
        boolean randomGameDifficulty = "random".equalsIgnoreCase(s.gameDifficulty());
        boolean randomEffectsInterval = s.effectsInterval() < 0;
        int effectsInterval = randomEffectsInterval ? 0 : s.effectsInterval();
        BingoControllerScreen.openWithStateForced(
                s.win(),
                composition,
                s.questPercent(),
                s.categoryLogicEnabled(),
                s.rarityLogicEnabled(),
                s.itemColorVariantsSeparate(),
                s.casino(),
                s.casinoMode(),
                s.rerollsMode(),
                s.rerollsCount(),
                s.gunRounds(),
                s.hangmanRounds(),
                s.hangmanBaseSeconds(),
                s.hangmanPenaltySeconds(),
                s.cardDifficulty(),
                randomCardDifficulty,
                s.gameDifficulty(),
                randomGameDifficulty,
                effectsInterval,
                randomEffectsInterval,
                s.rtpEnabled(),
                s.randomRtp(),
                s.hostileMobsEnabled(),
                s.randomHostileMobs(),
                s.hungerEnabled(),
                s.naturalRegenEnabled(),
                s.randomNaturalRegen(),
                s.randomHunger(),
                s.cardSize(),
                s.randomCardSize(),
                s.keepInventoryEnabled(),
                s.randomKeepInventory(),
                s.hardcoreEnabled(),
                s.randomHardcore(),
                s.daylightMode(),
                s.randomDaylight(),
                s.startDelaySeconds(),
                s.countdownEnabled(),
                s.countdownMinutes(),
                s.rushEnabled(),
                s.rushSeconds(),
                s.allowLateJoin(),
                s.pvpEnabled(),
                s.adventureMode(),
                s.prelitPortalsMode(),
                s.randomPvp(),
                s.registerMode(),
                s.randomRegister(),
                s.teamSyncEnabled(),
                s.teamChestEnabled(),
                s.shuffleMode(),
                s.starterKitMode(),
                s.hideGoalDetailsInChat(),
                s.minesEnabled(),
                s.mineAmount(),
                s.mineTimeSeconds(),
                s.powerSlotEnabled(),
                s.powerSlotIntervalSeconds()
        );
        wallScreen = new BingoControllerScreen(false, unused -> {});
        wallScreen.setFixedGuiScaleEnabled(true);
        wallScreen.init(GUI_WIDTH, GUI_HEIGHT);
        wallScreen.setSettingsPage(serverSettingsPage);
    }

    private static void ensureDedicatedGuiRenderer(Minecraft mc) {
        if (mc == null) return;
        if (wallGuiRenderer != null && wallGuiRenderState != null) return;
        try {
            wallGuiRenderState = new GuiRenderState();
            wallSubmitNodes = new SubmitNodeStorage();
            wallMainBuilder = new ByteBufferBuilder(2 * 1024 * 1024);
            wallCrumblingBuilder = new ByteBufferBuilder(512 * 1024);
            wallBufferSource = MultiBufferSource.immediate(wallMainBuilder);
            wallCrumblingBufferSource = MultiBufferSource.immediate(wallCrumblingBuilder);
            wallOutlineBufferSource = new OutlineBufferSource();
            wallFeatureDispatcher = new FeatureRenderDispatcher(
                    wallSubmitNodes,
                    mc.getBlockRenderer(),
                    wallBufferSource,
                    mc.getAtlasManager(),
                    wallOutlineBufferSource,
                    wallCrumblingBufferSource,
                    mc.font
            );
            wallGuiRenderer = new GuiRenderer(
                    wallGuiRenderState,
                    wallBufferSource,
                    wallSubmitNodes,
                    wallFeatureDispatcher,
                    java.util.List.of()
            );
        } catch (Throwable ignored) {
            wallGuiRenderer = null;
            wallGuiRenderState = null;
        }
    }

    private static GuiHit projectLookToScreen(net.minecraft.client.player.LocalPlayer player) {
        if (player == null || center == null) return null;
        PanelBounds panel = computePanelBounds();
        if (panel == null) return null;

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        if (Math.abs(look.z) < 1.0E-5) return null;
        double t = (panel.z - eye.z) / look.z;
        if (t <= 0.0D) return null;

        Vec3 hit = eye.add(look.scale(t));
        if (hit.x < panel.xMin || hit.x > panel.xMax || hit.y < panel.yMin || hit.y > panel.yMax) return null;

        double u = (hit.x - panel.xMin) / (panel.xMax - panel.xMin);
        double v = 1.0D - ((hit.y - panel.yMin) / (panel.yMax - panel.yMin));
        int guiX = clamp((int) Math.floor(u * (GUI_WIDTH - 1)), 0, GUI_WIDTH - 1);
        int guiY = clamp((int) Math.floor(v * (GUI_HEIGHT - 1)), 0, GUI_HEIGHT - 1);
        return new GuiHit(guiX, guiY);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static void lockGuiScale4Once(Minecraft mc) {
        // Intentionally fixed; independent of player GUI scale and window changes.
    }

    private static PanelBounds computePanelBounds() {
        if (center == null) return null;
        int cx = center.getX();
        int floorY = center.getY() + 1;
        int wallZ = center.getZ() - BOX_HALF + 2;
        double halfWidth = PANEL_WORLD_WIDTH * 0.5D;
        double xMin = cx - halfWidth;
        double xMax = cx + halfWidth;
        double yMin = floorY + PANEL_BOTTOM_OFFSET;
        double yMax = yMin + PANEL_WORLD_HEIGHT;
        double z = wallZ + 1.002D;
        return new PanelBounds(xMin, xMax, yMin, yMax, z);
    }

    private static ControllerSettingsSnapshot defaultSnapshot() {
        return new ControllerSettingsSnapshot(
                com.jamie.jamiebingo.bingo.WinCondition.LINE,
                0,
                50,
                false,
                false,
                false,
                false,
                0,
                0,
                0,
                5,
                5,
                60,
                10,
                "normal",
                "normal",
                0,
                true,
                false,
                true,
                false,
                true,
                true,
                false,
                false,
                5,
                false,
                false,
                false,
                false,
                false,
                com.jamie.jamiebingo.data.BingoGameData.DAYLIGHT_ENABLED,
                false,
                0,
                false,
                10,
                false,
                60,
                false,
                true,
                false,
                com.jamie.jamiebingo.data.BingoGameData.PRELIT_PORTALS_OFF,
                false,
                com.jamie.jamiebingo.data.BingoGameData.REGISTER_COLLECT_ONCE,
                false,
                false,
                true,
                com.jamie.jamiebingo.data.BingoGameData.SHUFFLE_DISABLED,
                com.jamie.jamiebingo.data.BingoGameData.STARTER_KIT_DISABLED,
                false,
                false,
                1,
                15,
                false,
                60,
                false,
                2
        );
    }

    private static void cancelMouseEvent(InputEvent.MouseButton.Pre event) {
        if (event == null) return;
        try {
            java.lang.reflect.Method m = event.getClass().getMethod("cancel");
            if (m.getParameterCount() == 0) {
                m.invoke(event);
                return;
            }
        } catch (Throwable ignored) {
        }
        try {
            java.lang.reflect.Method m = event.getClass().getMethod("setCanceled", boolean.class);
            m.invoke(event, true);
        } catch (Throwable ignored) {
        }
    }

    private static RenderTarget swapMinecraftMainTarget(Minecraft mc, RenderTarget replacement) {
        if (mc == null || replacement == null) return null;
        try {
            if (minecraftMainTargetField == null) {
                minecraftMainTargetField = Minecraft.class.getDeclaredField("mainRenderTarget");
                minecraftMainTargetField.setAccessible(true);
            }
            Object old = minecraftMainTargetField.get(mc);
            if (!(old instanceof RenderTarget oldTarget)) return null;
            minecraftMainTargetField.set(mc, replacement);
            return oldTarget;
        } catch (Throwable ignored) {
            return null;
        }
    }


    private static void restoreMinecraftMainTarget(Minecraft mc, RenderTarget previous) {
        if (mc == null || previous == null || minecraftMainTargetField == null) return;
        try {
            minecraftMainTargetField.set(mc, previous);
        } catch (Throwable ignored) {
        }
    }

    private static GpuBufferSlice resolveFogBuffer(Minecraft mc) {
        if (mc == null || mc.gameRenderer == null) return null;
        try {
            if (gameFogRendererField == null) {
                gameFogRendererField = mc.gameRenderer.getClass().getDeclaredField("fogRenderer");
                gameFogRendererField.setAccessible(true);
            }
            Object fogRenderer = gameFogRendererField.get(mc.gameRenderer);
            if (fogRenderer == null) return RenderSystem.getShaderFog();
            if (fogGetBufferMethod == null) {
                Class<?> fogModeClass = Class.forName("net.minecraft.client.renderer.fog.FogRenderer$FogMode");
                fogGetBufferMethod = fogRenderer.getClass().getMethod("getBuffer", fogModeClass);
            }
            Object fogModeNone = null;
            for (Object c : fogGetBufferMethod.getParameterTypes()[0].getEnumConstants()) {
                if (c != null && "NONE".equals(c.toString())) {
                    fogModeNone = c;
                    break;
                }
            }
            if (fogModeNone == null) return RenderSystem.getShaderFog();
            Object out = fogGetBufferMethod.invoke(fogRenderer, fogModeNone);
            if (out instanceof GpuBufferSlice slice) return slice;
        } catch (Throwable ignored) {
        }
        try {
            return RenderSystem.getShaderFog();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private record GuiHit(int guiX, int guiY) {
    }

    private record PanelBounds(double xMin, double xMax, double yMin, double yMax, double z) {
    }

    private static final class WrappedRenderTargetTexture extends AbstractTexture {
        void bind(Minecraft mc, TextureTarget target) {
            if (mc == null || target == null) return;
            this.texture = target.getColorTexture();
            this.textureView = target.getColorTextureView();
            forceNearestFilter();
        }

        private void forceNearestFilter() {
            try {
                for (java.lang.reflect.Method m : this.getClass().getSuperclass().getDeclaredMethods()) {
                    if (!"setFilter".equals(m.getName())) continue;
                    m.setAccessible(true);
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 2 && p[0] == boolean.class && p[1] == boolean.class) {
                        m.invoke(this, false, false);
                        return;
                    }
                    if (p.length == 1 && p[0] == boolean.class) {
                        m.invoke(this, false);
                        return;
                    }
                    if (p.length == 2 && p[1] == boolean.class && p[0].isEnum()) {
                        Object[] constants = p[0].getEnumConstants();
                        if (constants != null && constants.length > 0) {
                            // Pick the first enum constant; on modern MC this maps to nearest/none.
                            m.invoke(this, constants[0], false);
                            return;
                        }
                    }
                    if (p.length == 1 && p[0].isEnum()) {
                        Object[] constants = p[0].getEnumConstants();
                        if (constants != null && constants.length > 0) {
                            m.invoke(this, constants[0]);
                            return;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
