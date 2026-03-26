package com.jamie.jamiebingo.menu;

import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.world.SpectatorManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.jamie.jamiebingo.util.DataComponents;

public class LateJoinMenu extends ChestMenu {

    public LateJoinMenu(int id, Inventory inv) {
        super(
                net.minecraft.world.inventory.MenuType.GENERIC_9x1,
                id,
                inv,
                createContainer(),
                1
        );
    }

    public LateJoinMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (!(player instanceof ServerPlayer sp)) return;

        // Yes
        if (slotId == 3) {
            BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(sp));
            data.setLateJoinPending(com.jamie.jamiebingo.util.EntityUtil.getUUID(sp), true);
            sp.closeContainer();
            sp.openMenu(new SimpleMenuProvider(
                    (i, inv, p) -> new TeamSelectMenu(i, inv),
                    com.jamie.jamiebingo.util.ComponentUtil.literal("Teams")
            ));
            return;
        }

        // No
        if (slotId == 5) {
            sp.closeContainer();
            return;
        }
    }

    @Override public ItemStack quickMoveStack(Player p, int i) { return com.jamie.jamiebingo.util.ItemStackUtil.empty(); }
    @Override public boolean canDragTo(Slot s) { return false; }
    @Override public boolean canTakeItemForPickAll(ItemStack s, Slot slot) { return false; }

    private static SimpleContainer createContainer() {
        SimpleContainer c = new SimpleContainer(9);

        ItemStack yes = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:green_concrete");
        com.jamie.jamiebingo.util.ItemStackComponentUtil.set(
                yes,
                DataComponents.CUSTOM_NAME,
                com.jamie.jamiebingo.util.ComponentUtil.literal("Yes")
        );
        c.setItem(3, yes);

        ItemStack no = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:red_concrete");
        com.jamie.jamiebingo.util.ItemStackComponentUtil.set(
                no,
                DataComponents.CUSTOM_NAME,
                com.jamie.jamiebingo.util.ComponentUtil.literal("No")
        );
        c.setItem(5, no);

        return c;
    }
}


