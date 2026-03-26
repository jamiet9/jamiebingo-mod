package com.jamie.jamiebingo.item;

import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.world.item.Item;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.event.entity.player.PlayerEvent;
import java.lang.reflect.Method;
import net.minecraft.world.entity.EquipmentSlot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BingoControllerGiveHandler {
    private static final Logger LOGGER = LogManager.getLogger(com.jamie.jamiebingo.JamieBingo.MOD_ID);

    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Ensure /bingo commands are registered and synced even if command events didn't fire.
        try {
            var server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
            if (server != null) {
                var dispatcher = server.getCommands().getDispatcher();
                if (dispatcher.getRoot().getChild("bingo") == null) {
                    dispatcher.register(com.jamie.jamiebingo.bingo.BingoCommands.register());
                    LOGGER.info("[JamieBingo] Registered /bingo commands (player login fallback)");
                }
                // Sync command tree to this player so client autocomplete knows about /bingo.
                server.getCommands().sendCommands(player);
            }
        } catch (Throwable ignored) {
        }

        BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        boolean gameActive = data != null && data.isActive();


        // Never give controller / maker / configurator / team-select items during an active match.
        // This prevents reconnects from receiving join items mid-game.
        if (gameActive) {
            return;
        }

        giveIfMissing(
                player,
                ModItems.BINGO_CONTROLLER.get(),
                "jamiebingo_controller_given",
                gameActive
        );
        giveIfMissing(
                player,
                ModItems.CUSTOM_CARD_MAKER.get(),
                "jamiebingo_custom_card_given",
                gameActive
        );
        giveIfMissing(
                player,
                ModItems.CARD_LAYOUT_CONFIGURATOR.get(),
                "jamiebingo_card_layout_given",
                gameActive
        );
        giveIfMissing(
                player,
                ModItems.BLACKLIST_ITEMS_QUEST.get(),
                "jamiebingo_blacklist_item_given",
                gameActive
        );
        giveIfMissing(
                player,
                ModItems.GAME_HISTORY.get(),
                "jamiebingo_game_history_given",
                gameActive
        );
        giveIfMissing(
                player,
                ModItems.RARITY_CHANGER.get(),
                "jamiebingo_rarity_changer_given",
                gameActive
        );
        giveIfMissing(
                player,
                ModItems.WEEKLY_CHALLENGE.get(),
                "jamiebingo_weekly_challenge_given",
                gameActive
        );

        if (!com.jamie.jamiebingo.util.NbtUtil.getBoolean(player.getPersistentData(), "jamiebingo_team_select_given", false)
                || !PlayerTrackerHandler.hasTeamSelect(player)) {
            if (!gameActive || !PlayerTrackerHandler.hasTeamSelect(player)) {
                com.jamie.jamiebingo.util.PlayerInventoryUtil.addItem(player, PlayerTrackerHandler.createTeamSelectItem());
            }
            com.jamie.jamiebingo.util.NbtUtil.putBoolean(player.getPersistentData(), "jamiebingo_team_select_given", true);
        }

        // If the player already had a plain paper from older versions, try to normalize it.
        PlayerTrackerHandler.normalizeTeamSelect(player);
        arrangeLobbyHotbar(player, hasWorldSettingsItem(player));
    }

    public static void giveJoinItemsToAll(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            giveJoinItems(player);
        }
    }

    public static void giveJoinItemsToPlayer(ServerPlayer player) {
        giveJoinItems(player);
    }

    private static void giveJoinItems(ServerPlayer player) {
        if (player == null) return;
        arrangeLobbyHotbar(player, hasWorldSettingsItem(player));
    }

    public static void arrangeLobbyHotbar(ServerPlayer player, boolean includeWorldSettings) {
        if (player == null) return;
        Inventory inv = com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player);
        if (inv == null) return;

        removeLobbyItems(inv);

        setHotbarSlot(inv, 0, new ItemStack(ModItems.BINGO_CONTROLLER.get()));
        setHotbarSlot(inv, 1, PlayerTrackerHandler.createTeamSelectItem());
        setHotbarSlot(inv, 2, includeWorldSettings ? new ItemStack(ModItems.WORLD_SETTINGS.get()) : ItemStack.EMPTY);
        setHotbarSlot(inv, 3, new ItemStack(ModItems.GAME_HISTORY.get()));
        setHotbarSlot(inv, 4, new ItemStack(ModItems.BLACKLIST_ITEMS_QUEST.get()));
        setHotbarSlot(inv, 5, new ItemStack(ModItems.RARITY_CHANGER.get()));
        setHotbarSlot(inv, 6, new ItemStack(ModItems.CUSTOM_CARD_MAKER.get()));
        setHotbarSlot(inv, 7, new ItemStack(ModItems.CARD_LAYOUT_CONFIGURATOR.get()));
        setHotbarSlot(inv, 8, new ItemStack(ModItems.WEEKLY_CHALLENGE.get()));
        inv.setChanged();
    }

    private static boolean hasWorldSettingsItem(ServerPlayer player) {
        if (player == null) return false;
        return com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(
                com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player)
        ).stream().anyMatch(stack -> {
            Item inStack = getItemSafe(stack);
            return inStack == ModItems.WORLD_SETTINGS.get();
        });
    }

    private static void removeLobbyItems(Inventory inv) {
        if (inv == null) return;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isLobbyManagedItem(stack)) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    private static boolean isLobbyManagedItem(ItemStack stack) {
        Item item = getItemSafe(stack);
        return item == ModItems.BINGO_CONTROLLER.get()
                || item == ModItems.TEAM_SELECTOR.get()
                || item == ModItems.WORLD_SETTINGS.get()
                || item == ModItems.GAME_HISTORY.get()
                || item == ModItems.BLACKLIST_ITEMS_QUEST.get()
                || item == ModItems.RARITY_CHANGER.get()
                || item == ModItems.CUSTOM_CARD_MAKER.get()
                || item == ModItems.CARD_LAYOUT_CONFIGURATOR.get()
                || item == ModItems.WEEKLY_CHALLENGE.get();
    }

    private static void setHotbarSlot(Inventory inv, int slot, ItemStack stack) {
        if (inv == null || slot < 0 || slot >= 9) return;
        inv.setItem(slot, stack == null ? ItemStack.EMPTY : stack);
    }

    private static void giveIfMissing(
            ServerPlayer player,
            Item item,
            String flag,
            boolean gameActive
    ) {
        if (player == null || item == null) return;
        boolean flagged = com.jamie.jamiebingo.util.NbtUtil.getBoolean(player.getPersistentData(), flag, false);
        boolean hasItem = com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player)).stream()
                .anyMatch(stack -> {
                    Item inStack = getItemSafe(stack);
                    return inStack != null && inStack == item;
                })
                || (getItemSafe(com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(player, EquipmentSlot.OFFHAND)) == item);
        if (!flagged || !hasItem) {
            if (!gameActive || !hasItem) {
                com.jamie.jamiebingo.util.PlayerInventoryUtil.addItem(player, new ItemStack(item));
            }
            com.jamie.jamiebingo.util.NbtUtil.putBoolean(player.getPersistentData(), flag, true);
        }
    }

    private static Item getItemSafe(ItemStack stack) {
        if (stack == null) return null;
        try {
            return stack.getItem();
        } catch (Throwable ignored) {
            // Fall through to reflection for obf name changes.
        }
        try {
            for (Method m : stack.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && Item.class.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    Object out = m.invoke(stack);
                    if (out instanceof Item) {
                        return (Item) out;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}





