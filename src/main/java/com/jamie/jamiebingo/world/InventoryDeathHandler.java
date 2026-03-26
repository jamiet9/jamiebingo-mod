package com.jamie.jamiebingo.world;

import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

public class InventoryDeathHandler {

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getOriginal() instanceof ServerPlayer original)) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (data != null && data.keepInventoryEnabled) {
            AbstractContainerMenu oldMenu = original.inventoryMenu;
            AbstractContainerMenu newMenu = player.inventoryMenu;
            if (oldMenu != null && newMenu != null) {
                List<ItemStack> craftStacks = new ArrayList<>();
                for (Slot slot : oldMenu.slots) {
                    if (slot.container instanceof CraftingContainer) {
                        ItemStack stack = slot.getItem();
                        if (!stack.isEmpty()) {
                            craftStacks.add(stack.copy());
                        }
                    }
                }

                if (!craftStacks.isEmpty()) {
                    List<Slot> newCraftSlots = new ArrayList<>();
                    for (Slot slot : newMenu.slots) {
                        if (slot.container instanceof CraftingContainer) {
                            newCraftSlots.add(slot);
                        }
                    }

                    boolean newHasItems = false;
                    for (Slot slot : newCraftSlots) {
                        if (!slot.getItem().isEmpty()) {
                            newHasItems = true;
                            break;
                        }
                    }

                    if (!newHasItems) {
                        int count = Math.min(newCraftSlots.size(), craftStacks.size());
                        for (int i = 0; i < count; i++) {
                            newCraftSlots.get(i).set(craftStacks.get(i));
                        }
                    }
                }
            }

            ItemStack carried = original.containerMenu != null
                    ? original.containerMenu.getCarried()
                    : com.jamie.jamiebingo.util.ItemStackUtil.empty();
            if (!carried.isEmpty()) {
                ItemStack copy = carried.copy();
                if (!com.jamie.jamiebingo.util.PlayerInventoryUtil.addItem(player, copy)) {
                    player.drop(copy, false);
                }
            }
        }

        com.jamie.jamiebingo.world.StarterKitPersistenceManager.restoreOnClone(player);
    }
}

