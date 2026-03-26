package com.jamie.jamiebingo.menu;

import com.jamie.jamiebingo.data.TeamData;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.world.SpectatorManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import com.jamie.jamiebingo.util.DataComponents;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

public class TeamSelectMenu extends ChestMenu {
    public static final String PREF_TEAM_COLOR_TAG = "jamiebingo_preferred_team_color";
    private final SimpleContainer teamContainer;
    private final ServerPlayer serverPlayer;

    // ===== SERVER =====
    public TeamSelectMenu(int id, Inventory inv) {
        super(
                net.minecraft.world.inventory.MenuType.GENERIC_9x3,
                id,
                inv,
                createContainer(inv.player),
                3
        );
        this.teamContainer = (SimpleContainer) this.getContainer();
        this.serverPlayer = inv.player instanceof ServerPlayer sp ? sp : null;
    }

    // ===== CLIENT =====
    public TeamSelectMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void broadcastChanges() {
        if (serverPlayer != null) {
            refreshContainer(teamContainer, serverPlayer);
            teamContainer.setChanged();
        }
        super.broadcastChanges();
    }

    @Override
    public void slotsChanged(net.minecraft.world.Container container) {
        if (serverPlayer != null) {
            refreshContainer(teamContainer, serverPlayer);
            teamContainer.setChanged();
        }
        super.slotsChanged(container);
    }

    /* ============================
       CLICK ? SWITCH TEAM
       ============================ */

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= DyeColor.values().length) return;
        if (!(player instanceof ServerPlayer sp)) return;

        DyeColor color = DyeColor.values()[slotId];
        TeamData data = TeamData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(sp));
        UUID teamId = data.getOrCreateTeamForColor(color);

        UUID oldTeam = data.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(sp));
        if (oldTeam != null && oldTeam.equals(teamId)) {
            sp.closeContainer();
            return;
        }

        data.movePlayerToTeam(com.jamie.jamiebingo.util.EntityUtil.getUUID(sp), teamId);
        com.jamie.jamiebingo.util.NbtUtil.putInt(sp.getPersistentData(), PREF_TEAM_COLOR_TAG, color.getId());
        com.jamie.jamiebingo.network.NetworkHandler.send(
                new com.jamie.jamiebingo.network.PacketSyncPreferredTeamColor(color.getId()),
                PacketDistributor.PLAYER.with(sp)
        );

        BingoGameData gameData = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(sp));
        if (gameData.isLateJoinPending(com.jamie.jamiebingo.util.EntityUtil.getUUID(sp))) {
            SpectatorManager.finishLateJoin(sp, gameData);
            sp.closeContainer();
            return;
        }

        net.minecraft.world.level.Level level = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(sp);
        if (level != null) {
            level.playSound(
                    null,
                    com.jamie.jamiebingo.util.EntityPosUtil.getBlockPos(sp),
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(),
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0f,
                    1.2f
            );
        }

        sp.closeContainer();
        sp.openMenu(new SimpleMenuProvider(
                (i, inv, p) -> new TeamSelectMenu(i, inv),
                com.jamie.jamiebingo.util.ComponentUtil.literal("Teams")
        ));
    }

    /* ============================
       HARD LOCK INVENTORY
       ============================ */

    @Override public ItemStack quickMoveStack(Player p, int i) { return com.jamie.jamiebingo.util.ItemStackUtil.empty(); }
    @Override public boolean canDragTo(Slot s) { return false; }
    @Override public boolean canTakeItemForPickAll(ItemStack s, Slot slot) { return false; }

    /* ============================
       CONTAINER CONTENTS
       ============================ */

    private static SimpleContainer createContainer(Player player) {
        SimpleContainer c = new SimpleContainer(27);
        if (!(player instanceof ServerPlayer sp)) return c;

        refreshContainer(c, sp);

        return c;
    }

    private static void refreshContainer(SimpleContainer c, ServerPlayer sp) {
        TeamData data = TeamData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(sp));
        UUID self = data.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(sp));

        for (int i = 0; i < DyeColor.values().length; i++) {
            DyeColor color = DyeColor.values()[i];
            UUID teamId = data.getOrCreateTeamForColor(color);

            ItemStack stack = new ItemStack(getWool(color));
            com.jamie.jamiebingo.util.ItemStackComponentUtil.set(
                    stack,
                    DataComponents.CUSTOM_NAME,
                    com.jamie.jamiebingo.util.ComponentUtil.literal("Team " + cap(color.getName()))
            );

            java.util.List<Component> loreLines = new java.util.ArrayList<>();
            TeamData.TeamInfo team = data.getTeams().stream()
                    .filter(t -> t.id.equals(teamId))
                    .findFirst().orElse(null);

            if (team != null) {
                for (UUID u : team.members) {
                    String name = "Unknown";
                    var server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(sp);
                    ServerPlayer online = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, u);
                    if (online != null) {
                        name = online.getGameProfile().name();
                    }
                    loreLines.add(com.jamie.jamiebingo.util.ComponentUtil.literal("- " + name));
                }
            }

            if (loreLines.isEmpty()) {
                loreLines.add(com.jamie.jamiebingo.util.ComponentUtil.literal("- (empty)"));
            }

            com.jamie.jamiebingo.util.ItemStackComponentUtil.set(
                    stack,
                    DataComponents.LORE,
                    new ItemLore(loreLines)
            );

            boolean isSelfTeam = teamId != null && teamId.equals(self);
            if (isSelfTeam) {
                com.jamie.jamiebingo.util.ItemStackComponentUtil.set(
                        stack,
                        DataComponents.ENCHANTMENT_GLINT_OVERRIDE,
                        true
                );
                net.minecraft.world.level.Level level = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(sp);
                Holder<net.minecraft.world.item.enchantment.Enchantment> ench = null;
                if (level != null) {
                    ench = level.registryAccess()
                            .lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.UNBREAKING);
                }
                if (ench != null) {
                    stack.enchant(ench, 1);
                }
            } else {
                com.jamie.jamiebingo.util.ItemStackComponentUtil.remove(
                        stack,
                        DataComponents.ENCHANTMENT_GLINT_OVERRIDE
                );
            }

            c.setItem(i, stack);
        }
    }

    private static Item getWool(DyeColor c) {
        return switch (c) {
            case WHITE -> com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:white_wool");
            case ORANGE -> com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:orange_wool");
            case MAGENTA -> com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:magenta_wool");
            case LIGHT_BLUE -> com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:light_blue_wool");
            case YELLOW -> com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:yellow_wool");
            case LIME -> com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:lime_wool");
            case PINK -> com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:pink_wool");
            case GRAY -> com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:gray_wool");
            case LIGHT_GRAY -> com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:light_gray_wool");
            case CYAN -> com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:cyan_wool");
            case PURPLE -> com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:purple_wool");
            case BLUE -> com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:blue_wool");
            case BROWN -> com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:brown_wool");
            case GREEN -> com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:green_wool");
            case RED -> com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:red_wool");
            case BLACK -> com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:black_wool");
        };
    }

    private static String cap(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

