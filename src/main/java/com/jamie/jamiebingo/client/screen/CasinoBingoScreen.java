package com.jamie.jamiebingo.client.screen;

import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.client.BingoCardScreen;
import com.jamie.jamiebingo.client.ClientCardData;
import com.jamie.jamiebingo.client.ClientMinecraftUtil;
import com.jamie.jamiebingo.client.ClientMineState;
import com.jamie.jamiebingo.casino.CasinoRerollManager;
import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import com.jamie.jamiebingo.client.casino.ClientCasinoState.SlotPos;
import com.jamie.jamiebingo.client.casino.ClientCasinoState.VisualSlot;
import com.jamie.jamiebingo.client.render.QuestIconRenderer;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.PacketCasinoDraftPlace;
import com.jamie.jamiebingo.network.PacketCasinoRerollSlot;
import com.jamie.jamiebingo.network.PacketCasinoVoteSkip;
import com.jamie.jamiebingo.quest.icon.QuestIconProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class CasinoBingoScreen extends Screen {

    private static final int BASE_SLOT_SIZE = 24;
    private static final int BASE_PADDING = 4;

    private SlotPos lastRollingSlot = null;
    private final Set<SlotPos> soundedResolvedSlots = new HashSet<>();
    private Button voteSkipButton;
    private EditBox chatInput;
    private boolean chatInputOpen = false;
    private boolean suppressNextChatChar = false;

    public CasinoBingoScreen() {
        super(com.jamie.jamiebingo.util.ComponentUtil.literal("Bingo Casino"));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (chatInputOpen && chatInput != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                closeChatInput();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                submitChatInput();
                return true;
            }
            if (chatInput.keyPressed(event)) {
                return true;
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_E) {
            return true;
        }
        if (key == GLFW.GLFW_KEY_T) {
            openChatInput("");
            return true;
        }
        if (key == GLFW.GLFW_KEY_SLASH) {
            openChatInput("/");
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (suppressNextChatChar) {
            suppressNextChatChar = false;
            return true;
        }
        if (chatInputOpen && chatInput != null && chatInput.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        applyFixedScreenSize();
        super.init();
        lastRollingSlot = null;
        soundedResolvedSlots.clear();
        voteSkipButton = addRenderableWidget(
                com.jamie.jamiebingo.util.ButtonUtil.builder(
                                com.jamie.jamiebingo.util.ComponentUtil.literal(voteSkipLabel()),
                                b -> castSkipVote()
                        )
                        .pos(10, 10)
                        .size(130, 20)
                        .build()
        );
        chatInput = addRenderableWidget(new EditBox(
                this.font,
                10,
                this.height - 26,
                Math.max(120, this.width - 20),
                16,
                com.jamie.jamiebingo.util.ComponentUtil.literal("Chat")
        ));
        chatInput.setMaxLength(256);
        chatInput.setCanLoseFocus(false);
        chatInput.setVisible(false);
        chatInput.setFocused(false);
        updateVoteSkipButtonState();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isNew) {
        MouseButtonEvent fixedEvent = FixedGuiScaleUtil.virtualEvent(event, this.minecraft);
        if (super.mouseClicked(fixedEvent, isNew)) {
            return true;
        }

        int button = fixedEvent.button();
        double mouseX = fixedEvent.x();
        double mouseY = fixedEvent.y();

        if (button != 0) return true;
        if (!ClientCasinoState.isActive()) return true;

        int gridSize = ClientCasinoState.getGridSize();
        if (gridSize <= 0) return true;

        Map<SlotPos, VisualSlot> resolved = ClientCasinoState.getResolvedSlots();
        if (resolved == null) return true;

        CardLayout layout = computeCardLayout();

        if (ClientCasinoState.isDraftPhase()) {
            if (handleDraftChoiceClick(mouseX, mouseY, layout)) {
                playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.9f, 1.0f);
                return true;
            }
        }

        if (!ClientCasinoState.isRerollPhase() && !ClientCasinoState.isDraftPhase()) {
            return true;
        }

        int padding = layout.padding;
        int slotSize = layout.slotSize;
        int startX = layout.startX;
        int startY = layout.startY;

        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {

                int px = startX + x * (slotSize + padding);
                int py = startY + y * (slotSize + padding);

                if (mouseX >= px && mouseX <= px + slotSize &&
                        mouseY >= py && mouseY <= py + slotSize) {

                    SlotPos pos = new SlotPos(x, y);

                    if (ClientCasinoState.isRerollPhase() && resolved.containsKey(pos)) {
                        NetworkHandler.sendToServer(new PacketCasinoRerollSlot(x, y));
                        playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.9f, 1.0f);
                        return true;
                    }

                    if (ClientCasinoState.isDraftPhase()
                            && ClientCasinoState.isLocalPlayersTurn()
                            && !resolved.containsKey(pos)) {
                        int choice = ClientCasinoState.getSelectedDraftChoice();
                        NetworkHandler.sendToServer(new PacketCasinoDraftPlace(choice, x, y));
                        playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.9f, 1.0f);
                        return true;
                    }
                }
            }
        }
        if (ClientCasinoState.isRerollPhase() && handleMineRerollClick(mouseX, mouseY, layout)) {
            playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.9f, 1.0f);
            return true;
        }

        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        applyFixedScreenSize();
        if (chatInput != null) {
            chatInput.setWidth(Math.max(120, this.width - 20));
            chatInput.setPosition(10, this.height - 26);
        }
        MenuTextureQualityUtil.ensureNearestFiltering();
        float appliedScale = FixedGuiScaleUtil.beginScaledRender(graphics, this.minecraft);
        int fixedMouseX = FixedGuiScaleUtil.virtualMouseX(mouseX, this.minecraft);
        int fixedMouseY = FixedGuiScaleUtil.virtualMouseY(mouseY, this.minecraft);
        try {
        if (!ClientCasinoState.isActive()) {
            graphics.drawCenteredString(
                    this.font,
                    "Waiting for casino session...",
                    this.width / 2,
                    this.height / 2,
                    0xFFFFFF
            );
            return;
        }

        handleSounds();
        CardLayout layout = computeCardLayout();
        renderCard(graphics, fixedMouseX, fixedMouseY, layout);
        renderRerollMinePanel(graphics, fixedMouseX, fixedMouseY, layout);
        super.render(graphics, fixedMouseX, fixedMouseY, partialTick);
        renderGeneratorPanel(graphics, layout);
        if (chatInputOpen) {
            graphics.drawString(this.font, "Chat", 12, this.height - 39, 0xFFFFFFFF, true);
        }
        updateVoteSkipButtonState();
        } finally {
            FixedGuiScaleUtil.endScaledRender(graphics, appliedScale);
        }
    }

    @Override
    public void tick() {
        super.tick();
        updateVoteSkipButtonState();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        MouseButtonEvent fixedEvent = FixedGuiScaleUtil.virtualEvent(event, this.minecraft);
        double fixedDragX = FixedGuiScaleUtil.virtualDelta(dragX, this.minecraft);
        double fixedDragY = FixedGuiScaleUtil.virtualDelta(dragY, this.minecraft);
        return super.mouseDragged(fixedEvent, fixedDragX, fixedDragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(FixedGuiScaleUtil.virtualEvent(event, this.minecraft));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double fixedMouseX = FixedGuiScaleUtil.virtualMouseX(mouseX, this.minecraft);
        double fixedMouseY = FixedGuiScaleUtil.virtualMouseY(mouseY, this.minecraft);
        return super.mouseScrolled(fixedMouseX, fixedMouseY, deltaX, deltaY);
    }

    private void applyFixedScreenSize() {
        if (this.minecraft == null) return;
        this.width = FixedGuiScaleUtil.virtualWidth(this.minecraft, this.width);
        this.height = FixedGuiScaleUtil.virtualHeight(this.minecraft, this.height);
    }

    private void handleSounds() {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        var sm = ClientMinecraftUtil.getSoundManager(mc);
        if (mc == null || sm == null) return;

        SlotPos current = ClientCasinoState.getCurrentSlot();
        if (current != null && !current.equals(lastRollingSlot)) {
            playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.55f, 1.25f);
            lastRollingSlot = current;
        }
        if (current == null) {
            lastRollingSlot = null;
        }

        Map<SlotPos, VisualSlot> resolved = ClientCasinoState.getResolvedSlots();
        if (resolved == null || resolved.isEmpty()) return;

        for (Map.Entry<SlotPos, VisualSlot> e : resolved.entrySet()) {
            if (soundedResolvedSlots.add(e.getKey())) {
                playRevealSound(e.getValue().rarity());
            }
        }
    }

    private void playRevealSound(String rarity) {
        if (rarity == null) {
            playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.7f, 1.25f);
            return;
        }

        switch (rarity.toLowerCase()) {
            case "mythic" -> playUISound(SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 0.85f);
            case "legendary" -> playUISound(SoundEvents.TOTEM_USE, 1.0f, 0.85f);
            case "epic" -> playUISound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.95f, 1.0f);
            case "rare" -> playUISound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.1f);
            case "uncommon" -> playUISound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f, 1.15f);
            default -> playUISound(SoundEvents.UI_BUTTON_CLICK.value(), 0.7f, 1.25f);
        }
    }

    private void playUISound(SoundEvent sound, float volume, float pitch) {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        var sm = ClientMinecraftUtil.getSoundManager(mc);
        if (mc == null || sm == null || sound == null) return;

        com.jamie.jamiebingo.util.SoundManagerUtil.play(
                sm,
                SimpleSoundInstance.forUI(sound, volume, pitch)
        );
    }

    private void renderFakeMarkers(GuiGraphics graphics, int gridSize, int startX, int startY, int slotSize, int padding) {
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                int fakeIdx = y * Math.max(1, gridSize) + x;
                boolean fakeGreen = ClientCardData.isGreenFakeSlotIndex(fakeIdx);
                boolean fakeRed = ClientCardData.isRedFakeSlotIndex(fakeIdx);
                if (!fakeGreen && !fakeRed) continue;
                int px = startX + x * (slotSize + padding);
                int py = startY + y * (slotSize + padding);
                float fakeScale = 0.55f;
                var fakePose = com.jamie.jamiebingo.util.GuiGraphicsUtil.getPose(graphics);
                fakePose.pushMatrix();
                fakePose.scale(fakeScale, fakeScale);
                int tx = Math.round((px + 3) / fakeScale);
                int ty = Math.round((py + 3) / fakeScale);
                graphics.drawString(this.font, "fake", tx, ty, fakeGreen ? 0xFF62E87A : 0xFFEF7676, true);
                fakePose.popMatrix();
            }
        }
    }

    private void renderGeneratorPanel(GuiGraphics graphics, CardLayout layout) {
        int textX = layout.startX + layout.cardSize / 2;
        int textY = layout.startY - 28;
        if (textY < 10) {
            textY = layout.startY + layout.cardSize + 8;
        }

        if (ClientCasinoState.isDraftPhase()) {
            String line;
            int color;
            if (ClientCasinoState.isLocalPlayersTurn()) {
                line = "Your turn to draft";
                color = 0x55FF55;
            } else {
                String name = ClientCasinoState.getCurrentDraftPlayerName();
                line = name != null ? name + "'s turn to draft" : "Waiting for player...";
                color = 0xFFD700;
            }

            graphics.drawCenteredString(this.font, line, textX, textY, 0xFF000000 | color);
            graphics.drawCenteredString(this.font, "Pick a choice then click an empty slot", textX, textY + 15, 0xFFFFFFFF);
        } else if (ClientCasinoState.isRerollPhase()) {

            String line;
            int color;
            boolean fakePhase = ClientCasinoState.isFakeRerollPhase();
            if (fakePhase) {
                graphics.drawCenteredString(this.font, "FAKE REROLL PHASE", textX, textY - 12, 0xFFFF77FF);
                graphics.drawCenteredString(this.font, "ssshhhhh!", textX, textY - 24, 0xFF90FF90);
            }

            if (ClientCasinoState.isLocalPlayersTurn()) {
                line = fakePhase ? "Your turn to fake reroll" : "Your turn to reroll";
                color = 0x55FF55;
            } else {
                String name = ClientCasinoState.getCurrentRerollPlayerName();
                line = name != null
                        ? name + (fakePhase ? "'s turn to fake reroll" : "'s turn to reroll")
                        : "Waiting for player...";
                color = 0xFFD700;
            }

            graphics.drawCenteredString(this.font, line, textX, textY, 0xFF000000 | color);
            graphics.drawCenteredString(
                    this.font,
                    (fakePhase ? "Fake rerolls left: " : "Rerolls left: ") + ClientCasinoState.getRerollsRemaining(),
                    textX,
                    textY + 15,
                    0xFFFFFFFF
            );

        } else if (ClientCasinoState.isFinishing()) {

            graphics.drawCenteredString(this.font, "Finalizing card...", textX, textY, 0xFFFFD700);

        } else {

            graphics.drawCenteredString(this.font, "Generating card...", textX, textY, 0xFFFFD700);
        }
    }

    private void renderStatusOverlay(GuiGraphics graphics) {
        int x = this.width / 2;
        int y = 12;

        if (ClientCasinoState.isDraftPhase()) {
            String line = ClientCasinoState.isLocalPlayersTurn()
                    ? "Your turn to draft"
                    : (ClientCasinoState.getCurrentDraftPlayerName() != null
                    ? ClientCasinoState.getCurrentDraftPlayerName() + "'s turn to draft"
                    : "Waiting for player...");
            graphics.drawCenteredString(this.font, line, x, y, 0xFFFFFFFF);
            return;
        }

        if (ClientCasinoState.isRerollPhase()) {
            boolean fakePhase = ClientCasinoState.isFakeRerollPhase();
            if (fakePhase) {
                graphics.drawCenteredString(this.font, "FAKE REROLL PHASE", x, y - 12, 0xFFFF77FF);
                graphics.drawCenteredString(this.font, "ssshhhhh!", x, y + 24, 0xFF90FF90);
            }
            String line = ClientCasinoState.isLocalPlayersTurn()
                    ? (fakePhase ? "Your turn to fake reroll" : "Your turn to reroll")
                    : (ClientCasinoState.getCurrentRerollPlayerName() != null
                            ? ClientCasinoState.getCurrentRerollPlayerName() + (fakePhase ? "'s turn to fake reroll" : "'s turn to reroll")
                            : "Waiting for player...");
            graphics.drawCenteredString(this.font, line, x, y, 0xFFFFFFFF);
            graphics.drawCenteredString(this.font,
                    (fakePhase ? "Fake rerolls left: " : "Rerolls left: ") + ClientCasinoState.getRerollsRemaining(),
                    x,
                    y + 12,
                    0xFFFFFFFF
            );
            return;
        }

        if (ClientCasinoState.isFinishing()) {
            graphics.drawCenteredString(this.font, "Finalizing card...", x, y, 0xFFFFFFFF);
        } else {
            graphics.drawCenteredString(this.font, "Generating card...", x, y, 0xFFFFFFFF);
            int votes = ClientCasinoState.getSkipVotes();
            int total = ClientCasinoState.getSkipTotal();
            if (total > 0) {
                graphics.drawCenteredString(this.font, "Skip votes: " + votes + "/" + total, x, y + 12, 0xFFFFFFFF);
            }
        }
    }

    private void castSkipVote() {
        if (!ClientCasinoState.isActive()) return;
        if (ClientCasinoState.isDraftPhase()) return;
        if (ClientCasinoState.isRerollPhase()) return;
        if (ClientCasinoState.isFinishing()) return;
        if (ClientCasinoState.getSkipTotal() <= 0) return;
        if (ClientCasinoState.hasLocalSkipVoted()) return;
        ClientCasinoState.setLocalSkipVoted(true);
        NetworkHandler.sendToServer(new PacketCasinoVoteSkip());
        updateVoteSkipButtonState();
    }

    private void updateVoteSkipButtonState() {
        if (voteSkipButton == null) return;
        boolean visible = ClientCasinoState.isActive()
                && !ClientCasinoState.isDraftPhase()
                && !ClientCasinoState.isRerollPhase()
                && !ClientCasinoState.isFinishing()
                && ClientCasinoState.getSkipTotal() > 0;
        voteSkipButton.visible = visible;
        voteSkipButton.active = visible && !ClientCasinoState.hasLocalSkipVoted();
        voteSkipButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(voteSkipLabel()));
    }

    private String voteSkipLabel() {
        int votes = ClientCasinoState.getSkipVotes();
        int total = ClientCasinoState.getSkipTotal();
        if (ClientCasinoState.hasLocalSkipVoted()) {
            return total > 0 ? "Voted Skip (" + votes + "/" + total + ")" : "Voted Skip";
        }
        return total > 0 ? "Vote Skip (" + votes + "/" + total + ")" : "Vote Skip";
    }

    private void openChatInput(String initial) {
        if (chatInput == null) return;
        if (chatInputOpen) return;
        chatInputOpen = true;
        suppressNextChatChar = true;
        chatInput.setVisible(true);
        chatInput.setFocused(true);
        setFocused(chatInput);
        chatInput.setValue(initial == null ? "" : initial);
        chatInput.moveCursorToEnd(false);
    }

    private void closeChatInput() {
        if (chatInput == null) return;
        chatInputOpen = false;
        suppressNextChatChar = false;
        chatInput.setValue("");
        chatInput.setFocused(false);
        chatInput.setVisible(false);
        setFocused(null);
    }

    private void submitChatInput() {
        if (chatInput == null) return;
        String message = chatInput.getValue();
        if (message == null) {
            closeChatInput();
            return;
        }
        String trimmed = message.trim();
        if (trimmed.isEmpty()) {
            closeChatInput();
            return;
        }

        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        var player = ClientMinecraftUtil.getPlayer(mc);
        if (player == null || player.connection == null) {
            closeChatInput();
            return;
        }

        if (trimmed.startsWith("/")) {
            String command = trimmed.substring(1).trim();
            if (!command.isEmpty()) {
                player.connection.sendCommand(command);
            }
        } else {
            player.connection.sendChat(trimmed);
        }
        closeChatInput();
    }

    private void renderCard(GuiGraphics graphics, int mouseX, int mouseY, CardLayout layout) {

        int gridSize = ClientCasinoState.getGridSize();
        Map<SlotPos, VisualSlot> resolved = ClientCasinoState.getResolvedSlots();
        SlotPos active = ClientCasinoState.getCurrentSlot();
        SlotPos lastRerolled = ClientCasinoState.getLastRerolledSlot();
        SlotPos lastDraftPlaced = ClientCasinoState.getLastDraftPlacedSlot();

        int padding = layout.padding;
        int slotSize = layout.slotSize;
        int startX = layout.startX;
        int startY = layout.startY;

        VisualSlot hovered = null;
        Object hoveredRecipePreview = null;

        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {

                int px = startX + x * (slotSize + padding);
                int py = startY + y * (slotSize + padding);

                SlotPos pos = new SlotPos(x, y);
                VisualSlot slot = resolved != null ? resolved.get(pos) : null;

                graphics.fill(px, py, px + slotSize, py + slotSize, 0xFF2A2A2A);

                if (active != null && active.x() == x && active.y() == y) {
                    graphics.fill(px - 2, py - 2, px + slotSize + 2, py + slotSize + 2, 0x88FFD700);
                }

                if (ClientCasinoState.isRerollPhase()
                        && lastRerolled != null
                        && lastRerolled.x() == x
                        && lastRerolled.y() == y) {
                    var level = ClientMinecraftUtil.getLevel(ClientMinecraftUtil.getMinecraft());
                    long t = level != null ? level.getGameTime() : 0;
                    float pulse = (float) (Math.sin((t - ClientCasinoState.getLastRerolledTick()) * 0.25f) * 0.5f + 0.5f);
                    int alpha = 80 + (int) (90 * pulse);
                    graphics.fill(px - 3, py - 3, px + slotSize + 3, py - 1, (alpha << 24) | 0x00FFFF);
                    graphics.fill(px - 3, py + slotSize + 1, px + slotSize + 3, py + slotSize + 3, (alpha << 24) | 0x00FFFF);
                    graphics.fill(px - 3, py - 1, px - 1, py + slotSize + 1, (alpha << 24) | 0x00FFFF);
                    graphics.fill(px + slotSize + 1, py - 1, px + slotSize + 3, py + slotSize + 1, (alpha << 24) | 0x00FFFF);
                }

                if (ClientCasinoState.isDraftPhase()
                        && lastDraftPlaced != null
                        && lastDraftPlaced.x() == x
                        && lastDraftPlaced.y() == y) {
                    var level = ClientMinecraftUtil.getLevel(ClientMinecraftUtil.getMinecraft());
                    long t = level != null ? level.getGameTime() : 0;
                    float pulse = (float) (Math.sin((t - ClientCasinoState.getLastDraftPlacedTick()) * 0.25f) * 0.5f + 0.5f);
                    int alpha = 110 + (int) (90 * pulse);
                    graphics.fill(px - 3, py - 3, px + slotSize + 3, py - 1, (alpha << 24) | 0x55FF55);
                    graphics.fill(px - 3, py + slotSize + 1, px + slotSize + 3, py + slotSize + 3, (alpha << 24) | 0x55FF55);
                    graphics.fill(px - 3, py - 1, px - 1, py + slotSize + 1, (alpha << 24) | 0x55FF55);
                    graphics.fill(px + slotSize + 1, py - 1, px + slotSize + 3, py + slotSize + 1, (alpha << 24) | 0x55FF55);
                }

                if (slot != null) {
                    int glow = rarityGlow(slot.rarity());
                    if (glow != 0) {
                        graphics.fill(px - 2, py - 2, px + slotSize + 2, py + slotSize + 2, glow);
                    }
                }

                ItemStack stack;
                if (slot != null) {
                    stack = getItemStack(slot.id());
                } else if (active != null && active.x() == x && active.y() == y) {
                    stack = ClientCasinoState.getRollingPreviewStack();
                } else {
                    stack = com.jamie.jamiebingo.util.ItemStackUtil.empty();
                }

                if (slot != null && (slot.isQuest() || slot.id().startsWith("quest."))) {
                    try {
                        BingoSlot questSlot = new BingoSlot(slot.id(), slot.name(), slot.category(), slot.rarity());
                        QuestIconRenderer.renderQuestIcon(graphics, px, py, QuestIconProvider.iconFor(questSlot), slotSize);
                    } catch (Exception ignored) {
                    }
                } else if (!stack.isEmpty()) {
                    graphics.renderItem(stack, px + (slotSize - 16) / 2, py + (slotSize - 16) / 2);
                }

                if (slot != null &&
                        mouseX >= px && mouseX <= px + slotSize &&
                        mouseY >= py && mouseY <= py + slotSize) {
                    hovered = slot;
                    if (slot.isQuest() || slot.id().startsWith("quest.")) {
                        hoveredRecipePreview = null;
                    } else if (!stack.isEmpty()) {
                        hoveredRecipePreview = BingoCardScreen.buildSharedRecipePreview(stack);
                    } else {
                        hoveredRecipePreview = null;
                    }
                }
            }
        }

        renderFakeMarkers(graphics, gridSize, startX, startY, slotSize, padding);

        if (hovered != null) {
            if (hoveredRecipePreview != null) {
                BingoCardScreen.renderSharedRecipePreviewPanel(
                        graphics,
                        hoveredRecipePreview,
                        mouseX,
                        mouseY,
                        this.width,
                        this.height
                );
            }
            ScreenTooltipUtil.drawComponentTooltip(
                    graphics,
                    this.font,
                    List.of(
                            com.jamie.jamiebingo.util.ComponentUtil.literal(hovered.name()),
                            com.jamie.jamiebingo.util.ComponentUtil.literal("Category: " + hovered.category()),
                            com.jamie.jamiebingo.util.ComponentUtil.literal("Rarity: " + hovered.rarity())
                    ),
                    mouseX,
                    mouseY,
                    this.width,
                    this.height,
                    Math.max(180, this.width - 24)
            );
        }

        if (ClientCasinoState.isDraftPhase()) {
            renderDraftChoices(graphics, mouseX, mouseY, layout);
        }
    }

    private boolean handleMineRerollClick(double mouseX, double mouseY, CardLayout layout) {
        if (!ClientMineState.isActive()) return false;
        if (!ClientCasinoState.isLocalPlayersTurn()) return false;
        for (MineButtonLayout button : computeMineRerollLayouts(layout)) {
            if (mouseX < button.x() || mouseX > button.x() + button.size()) continue;
            if (mouseY < button.y() || mouseY > button.y() + button.size()) continue;
            if (button.type() == MineButtonType.MINE) {
                NetworkHandler.sendToServer(new PacketCasinoRerollSlot(CasinoRerollManager.SPECIAL_MINE_X, button.index()));
                return true;
            }
            NetworkHandler.sendToServer(new PacketCasinoRerollSlot(CasinoRerollManager.SPECIAL_DEFUSE_X, 0));
            return true;
        }
        return false;
    }

    private void renderRerollMinePanel(GuiGraphics graphics, int mouseX, int mouseY, CardLayout layout) {
        if (!ClientCasinoState.isRerollPhase()) return;
        if (!ClientMineState.isActive()) return;

        MineButtonLayout hovered = null;
        for (MineButtonLayout button : computeMineRerollLayouts(layout)) {
            int x = button.x();
            int y = button.y();
            int size = button.size();
            graphics.fill(x, y, x + size, y + size, 0xAA101010);
            graphics.fill(x, y, x + size, y + 1, 0x66FFFFFF);
            graphics.fill(x, y + size - 1, x + size, y + size, 0x66000000);
            graphics.fill(x, y, x + 1, y + size, 0x66FFFFFF);
            graphics.fill(x + size - 1, y, x + size, y + size, 0x66000000);
            if (button.type() == MineButtonType.MINE) {
                graphics.drawCenteredString(this.font, "mine " + (button.index() + 1), x + size / 2, y - 9, 0xFFFF7777);
            } else {
                graphics.drawCenteredString(this.font, "defuse", x + size / 2, y - 9, 0xFF99FF99);
            }

            var level = ClientMinecraftUtil.getLevel(ClientMinecraftUtil.getMinecraft());
            long t = level != null ? level.getGameTime() : 0L;
            boolean animate = false;
            if (button.type() == MineButtonType.MINE) {
                animate = ClientCasinoState.getLastSpecialRerolledMineIndex() == button.index();
            } else {
                animate = ClientCasinoState.isLastSpecialRerolledDefuse();
            }
            if (animate && t - ClientCasinoState.getLastSpecialRerolledTick() <= 60) {
                float pulse = (float) (Math.sin((t - ClientCasinoState.getLastSpecialRerolledTick()) * 0.25f) * 0.5f + 0.5f);
                int alpha = 80 + (int) (90 * pulse);
                int col = button.type() == MineButtonType.MINE ? 0x00FF4D4D : 0x0055FFAA;
                graphics.fill(x - 3, y - 3, x + size + 3, y - 1, (alpha << 24) | col);
                graphics.fill(x - 3, y + size + 1, x + size + 3, y + size + 3, (alpha << 24) | col);
                graphics.fill(x - 3, y - 1, x - 1, y + size + 1, (alpha << 24) | col);
                graphics.fill(x + size + 1, y - 1, x + size + 3, y + size + 1, (alpha << 24) | col);
            }

            BingoSlot slot = button.slot();
            ItemStack stack = getItemStack(slot.getId());
            if (slot.getId() != null && slot.getId().startsWith("quest.")) {
                QuestIconRenderer.renderQuestIcon(graphics, x, y, QuestIconProvider.iconFor(slot), size);
            } else if (!stack.isEmpty()) {
                graphics.renderItem(stack, x + (size - 16) / 2, y + (size - 16) / 2);
            } else {
                QuestIconRenderer.renderQuestIcon(graphics, x, y, QuestIconProvider.iconFor(slot), size);
            }

            if (mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size) {
                hovered = button;
            }
        }

        if (hovered != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(com.jamie.jamiebingo.util.ComponentUtil.literal(hovered.slot().getName()));
            String action = ClientCasinoState.isFakeRerollPhase() ? "fake reroll" : "reroll";
            tip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Click to " + action + " this " + (hovered.type() == MineButtonType.MINE ? "mine" : "defuse") + " slot."));
            ScreenTooltipUtil.drawComponentTooltip(
                    graphics,
                    this.font,
                    tip,
                    mouseX,
                    mouseY,
                    this.width,
                    this.height,
                    Math.max(180, this.width - 24)
            );
        }
    }

    private List<MineButtonLayout> computeMineRerollLayouts(CardLayout layout) {
        if (!ClientMineState.isActive()) return List.of();
        List<MineButtonLayout> out = new ArrayList<>();
        List<String> mineIds = ClientMineState.sourceQuestIds();
        String defuseId = ClientMineState.defuseQuestId();
        boolean hasDefuse = defuseId != null && !defuseId.isBlank();
        if (mineIds.isEmpty() && !hasDefuse) return out;

        int count = Math.max(1, mineIds.size());
        int size = Math.min(22, Math.max(12, layout.slotSize));
        int spacing = Math.max(8, size + 16);
        int x = Math.max(4, layout.startX - spacing * (hasDefuse ? 3 : 2));
        int stackHeight = count * size + Math.max(0, count - 1) * 3;
        int startY = layout.startY + Math.max(0, (layout.cardSize - stackHeight) / 2);

        for (int i = 0; i < mineIds.size(); i++) {
            String id = mineIds.get(i);
            BingoSlot slot = ClientMineState.slotFor(id);
            if (slot == null) {
                slot = new BingoSlot(id, ClientMineState.displayNameFor(id), "mine", "rare");
            }
            out.add(new MineButtonLayout(MineButtonType.MINE, i, x, startY + i * (size + 3), size, slot));
        }
        if (hasDefuse) {
            BingoSlot defuse = ClientMineState.slotFor(defuseId);
            if (defuse == null) {
                String name = ClientMineState.defuseDisplayName();
                defuse = new BingoSlot(defuseId, name == null || name.isBlank() ? defuseId : name, "mine", "rare");
            }
            int defuseY = startY + Math.max(0, (stackHeight - size) / 2);
            out.add(new MineButtonLayout(MineButtonType.DEFUSE, 0, x - spacing, defuseY, size, defuse));
        }
        return out;
    }

    private CardLayout computeCardLayout() {
        int gridSize = ClientCasinoState.getGridSize();
        int padding = BASE_PADDING;
        int slotSize = BASE_SLOT_SIZE;

        int maxSize = Math.min(this.width - 40, this.height - 40);
        int desired = gridSize * (slotSize + padding);
        if (desired > maxSize) {
            int maxPerCell = maxSize / Math.max(1, gridSize);
            slotSize = Math.max(12, maxPerCell - padding);
        }

        int cardSize = gridSize * (slotSize + padding);
        int startX = (this.width - cardSize) / 2;
        int startY = (this.height - cardSize) / 2;
        return new CardLayout(startX, startY, cardSize, slotSize, padding);
    }

    private List<ChoiceLayout> computeDraftChoiceLayouts(CardLayout layout) {
        List<VisualSlot> choices = ClientCasinoState.getDraftChoices();
        if (choices.isEmpty()) return List.of();

        int count = choices.size();
        int boxW = Math.max(16, layout.slotSize);
        int boxH = boxW;
        int gap = Math.max(4, layout.padding);
        int totalW = count * boxW + Math.max(0, count - 1) * gap;
        int startX = Math.max(10, (this.width - totalW) / 2);
        int y = layout.startY + layout.cardSize + 14;
        if (y + boxH > this.height - 8) {
            y = Math.max(8, layout.startY - boxH - 14);
        }

        List<ChoiceLayout> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            out.add(new ChoiceLayout(startX + i * (boxW + gap), y, boxW, boxH, i, choices.get(i)));
        }
        return out;
    }

    private boolean handleDraftChoiceClick(double mouseX, double mouseY, CardLayout layout) {
        for (ChoiceLayout cl : computeDraftChoiceLayouts(layout)) {
            if (mouseX >= cl.x() && mouseX <= cl.x() + cl.w() && mouseY >= cl.y() && mouseY <= cl.y() + cl.h()) {
                ClientCasinoState.setSelectedDraftChoice(cl.index());
                return true;
            }
        }
        return false;
    }

    private void renderDraftChoices(GuiGraphics graphics, int mouseX, int mouseY, CardLayout layout) {
        List<ChoiceLayout> choices = computeDraftChoiceLayouts(layout);
        if (choices.isEmpty()) return;

        int selected = ClientCasinoState.getSelectedDraftChoice();
        ChoiceLayout hovered = null;

        for (ChoiceLayout cl : choices) {
            boolean isSelected = cl.index() == selected;
            int bg = isSelected ? 0xCC263526 : 0xCC222222;
            graphics.fill(cl.x(), cl.y(), cl.x() + cl.w(), cl.y() + cl.h(), bg);
            int glow = rarityGlow(cl.slot().rarity());
            if (glow != 0) {
                graphics.fill(cl.x() - 2, cl.y() - 2, cl.x() + cl.w() + 2, cl.y() + cl.h() + 2, glow);
            }
            graphics.fill(cl.x() - 1, cl.y() - 1, cl.x() + cl.w() + 1, cl.y(), 0xFF000000);
            graphics.fill(cl.x() - 1, cl.y() + cl.h(), cl.x() + cl.w() + 1, cl.y() + cl.h() + 1, 0xFF000000);
            graphics.fill(cl.x() - 1, cl.y(), cl.x(), cl.y() + cl.h(), 0xFF000000);
            graphics.fill(cl.x() + cl.w(), cl.y(), cl.x() + cl.w() + 1, cl.y() + cl.h(), 0xFF000000);

            if (isSelected) {
                graphics.fill(cl.x() - 2, cl.y() - 2, cl.x() + cl.w() + 2, cl.y() - 1, 0xFF55FF55);
                graphics.fill(cl.x() - 2, cl.y() + cl.h() + 1, cl.x() + cl.w() + 2, cl.y() + cl.h() + 2, 0xFF55FF55);
                graphics.fill(cl.x() - 2, cl.y() - 1, cl.x() - 1, cl.y() + cl.h() + 1, 0xFF55FF55);
                graphics.fill(cl.x() + cl.w() + 1, cl.y() - 1, cl.x() + cl.w() + 2, cl.y() + cl.h() + 1, 0xFF55FF55);
            }

            ItemStack stack = getItemStack(cl.slot().id());
            if (cl.slot().isQuest() || cl.slot().id().startsWith("quest.")) {
                try {
                    BingoSlot questSlot = new BingoSlot(cl.slot().id(), cl.slot().name(), cl.slot().category(), cl.slot().rarity());
                    QuestIconRenderer.renderQuestIcon(graphics, cl.x(), cl.y(), QuestIconProvider.iconFor(questSlot), cl.w());
                } catch (Exception ignored) {
                }
            } else if (!stack.isEmpty()) {
                graphics.renderItem(stack, cl.x() + (cl.w() - 16) / 2, cl.y() + (cl.h() - 16) / 2);
            }

            if (mouseX >= cl.x() && mouseX <= cl.x() + cl.w() && mouseY >= cl.y() && mouseY <= cl.y() + cl.h()) {
                hovered = cl;
            }
        }

        if (hovered != null) {
            ScreenTooltipUtil.drawComponentTooltip(graphics,
                    this.font,
                    List.of(
                            com.jamie.jamiebingo.util.ComponentUtil.literal(hovered.slot().name()),
                            com.jamie.jamiebingo.util.ComponentUtil.literal("Category: " + hovered.slot().category()),
                            com.jamie.jamiebingo.util.ComponentUtil.literal("Rarity: " + hovered.slot().rarity())
                    ),
                    mouseX,
                    mouseY,
                    this.width,
                    this.height,
                    Math.max(180, this.width - 24)
            );
        }
    }

    private int rarityGlow(String rarity) {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        if (mc == null || mc.level == null || rarity == null) return 0;

        long t = mc.level.getGameTime();
        float pulse = (float) (Math.sin(t * 0.15f) * 0.5f + 0.5f);

        return switch (rarity.toLowerCase()) {
            case "uncommon" -> argb(120 + (int) (40 * pulse), 0x55FF55);
            case "rare" -> argb(140 + (int) (60 * pulse), 0x5599FF);
            case "epic" -> argb(160 + (int) (80 * pulse), 0xAA55FF);
            case "legendary" -> argb(180 + (int) (100 * pulse), 0xFFD700);
            case "mythic" -> argb(210 + (int) (40 * pulse), 0xFF00FF);
            default -> 0;
        };
    }

    private int argb(int a, int rgb) {
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private ItemStack getItemStack(String id) {
        try {
            Item item = ForgeRegistries.ITEMS.getValue(com.jamie.jamiebingo.util.IdUtil.id(id));
            return item != null ? new ItemStack(item) : com.jamie.jamiebingo.util.ItemStackUtil.empty();
        } catch (Exception ignored) {
            return com.jamie.jamiebingo.util.ItemStackUtil.empty();
        }
    }

    private record CardLayout(int startX, int startY, int cardSize, int slotSize, int padding) {}
    private record ChoiceLayout(int x, int y, int w, int h, int index, VisualSlot slot) {}
    private enum MineButtonType { MINE, DEFUSE }
    private record MineButtonLayout(MineButtonType type, int index, int x, int y, int size, BingoSlot slot) {}
}
