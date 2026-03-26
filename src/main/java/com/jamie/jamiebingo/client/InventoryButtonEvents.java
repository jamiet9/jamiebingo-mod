package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.JamieBingo;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.PacketOpenTeamChest;
import com.jamie.jamiebingo.network.packet.PacketVoteEndGame;
import com.jamie.jamiebingo.network.packet.PacketVoteRerollCard;
import com.jamie.jamiebingo.client.screen.CardLayoutConfiguratorScreen;
import com.jamie.jamiebingo.util.ItemLookupUtil;
import com.jamie.jamiebingo.util.LivingEntityEffectUtil;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID, value = Dist.CLIENT)
public class InventoryButtonEvents {

    private static int chestX = -1;
    private static int chestY = -1;
    private static int voteX = -1;
    private static int voteY = -1;
    private static int voteRerollX = -1;
    private static int voteRerollY = -1;
    private static int overlaysX = -1;
    private static int overlaysY = -1;
    private static Button voteButton = null;
    private static Button voteRerollButton = null;
    private static Button overlaysButton = null;


    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;

        int x = screen.getGuiLeft() + screen.getXSize() + 6;
        int y = screen.getGuiTop() + 6;
        var mc = ClientMinecraftUtil.getMinecraft();
        var player = ClientMinecraftUtil.getPlayer(mc);
        if (player != null && LivingEntityEffectUtil.hasActiveEffects(player)) {
            y = Math.max(4, screen.getGuiTop() - 36);
        }

        chestX = x;
        chestY = y;
        int chatButtonY = ClientGameState.teamChestEnabled ? y + 18 : y;

        if (ClientGameState.teamChestEnabled) {
            event.addListener(com.jamie.jamiebingo.util.ButtonUtil.builder(
                            com.jamie.jamiebingo.util.ComponentUtil.empty(),
                            btn -> NetworkHandler.sendToServer(new PacketOpenTeamChest())
                    )
                    .pos(x, y)
                    .size(16, 16)
                    .build());
        }

        // Team Chat (below)
        event.addListener(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.empty(),
                        btn -> {
                            ClientTeamChatState.pendingTeamChat = true;
                            var level = ClientMinecraftUtil.getLevel(ClientMinecraftUtil.getMinecraft());
                            if (level != null) {
                                ClientTeamChatState.pendingTeamChatUntilTick = level.getGameTime() + 200;
                            } else {
                                ClientTeamChatState.pendingTeamChatUntilTick = -1;
                            }
                            ClientMinecraftUtil.setScreen(
                                    new net.minecraft.client.gui.screens.ChatScreen("", false)
                            );
                        }
                )
                .pos(x, chatButtonY)
                .size(16, 16)
                .build());

        int voteW = 116;
        voteX = Math.max(4, screen.width - voteW - 4);
        voteY = Math.max(4, screen.height - 20);
        voteRerollX = voteX;
        voteRerollY = Math.max(4, voteY - 22);
        overlaysX = voteX;
        overlaysY = Math.max(4, voteRerollY - 28);

        overlaysButton = com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Edit Overlays"),
                        btn -> ClientMinecraftUtil.setScreen(new CardLayoutConfiguratorScreen())
                )
                .pos(overlaysX, overlaysY)
                .size(voteW, 16)
                .build();
        event.addListener(overlaysButton);

        voteRerollButton = com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal(voteRerollLabel()),
                        btn -> NetworkHandler.sendToServer(new PacketVoteRerollCard())
                )
                .pos(voteRerollX, voteRerollY)
                .size(voteW, 16)
                .build();
        event.addListener(voteRerollButton);
        voteRerollButton.active = !ClientVoteRerollCardState.localVoted();

        voteButton = com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal(voteLabel()),
                        btn -> NetworkHandler.sendToServer(new PacketVoteEndGame())
                )
                .pos(voteX, voteY)
                .size(voteW, 16)
                .build();
        event.addListener(voteButton);
        voteButton.active = !ClientVoteEndGameState.localVoted();
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen)) return;

        if (ClientGameState.teamChestEnabled && chestX >= 0 && chestY >= 0) {
            ItemStack enderChest = new ItemStack(ItemLookupUtil.item("minecraft:ender_chest"));
            com.jamie.jamiebingo.util.GuiGraphicsUtil.renderItem(event.getGuiGraphics(), enderChest, chestX, chestY);
        }
        if (chestX >= 0 && chestY >= 0) {
            ItemStack paper = new ItemStack(ItemLookupUtil.item("minecraft:paper"));
            int paperY = ClientGameState.teamChestEnabled ? chestY + 18 : chestY;
            com.jamie.jamiebingo.util.GuiGraphicsUtil.renderItem(event.getGuiGraphics(), paper, chestX, paperY);
        }
        if (voteButton != null) {
            voteButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(voteLabel()));
            voteButton.active = !ClientVoteEndGameState.localVoted();
        }
        if (voteRerollButton != null) {
            voteRerollButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(voteRerollLabel()));
            voteRerollButton.active = !ClientVoteRerollCardState.localVoted();
        }
        if (overlaysButton != null) {
            overlaysButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Edit Overlays"));
            overlaysButton.active = true;
        }
    }

    private static String voteLabel() {
        int voted = ClientVoteEndGameState.votes();
        int total = ClientVoteEndGameState.total();
        String prefix = ClientVoteEndGameState.localVoted() ? "Voted End Game" : "Vote End Game?";
        return total > 0 ? prefix + " (" + voted + "/" + total + ")" : prefix;
    }

    private static String voteRerollLabel() {
        int voted = ClientVoteRerollCardState.votes();
        int total = ClientVoteRerollCardState.total();
        String prefix = ClientVoteRerollCardState.localVoted() ? "Voted Reroll Unclaimed" : "Vote Reroll Unclaimed?";
        return total > 0 ? prefix + " (" + voted + "/" + total + ")" : prefix;
    }
}





