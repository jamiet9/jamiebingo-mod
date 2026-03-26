package com.jamie.jamiebingo.menu;

import com.jamie.jamiebingo.data.BroadcastHelper;
import com.jamie.jamiebingo.data.TeamData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamChestMenu extends ChestMenu {

    private final ServerPlayer player;
    private final UUID teamId;
    private final Map<String, Integer> snapshot = new HashMap<>();

    protected TeamChestMenu(
            MenuType<?> type,
            int id,
            ServerPlayer player,
            Container container,
            int rows
    ) {
        super(type, id, com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player), container, rows);
        this.player = player;

        TeamData teamData = TeamData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        this.teamId = teamData.ensureAssigned(player);

        takeSnapshot(container);
    }

    public static TeamChestMenu threeRows(
            int id,
            ServerPlayer player,
            Container container
    ) {
        return new TeamChestMenu(
                MenuType.GENERIC_9x3,
                id,
                player,
                container,
                3
        );
    }

    private void takeSnapshot(Container container) {
        snapshot.clear();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                snapshot.merge(
                        stack.getHoverName().getString(), // ✅ HUMAN-READABLE
                        stack.getCount(),
                        Integer::sum
                );
            }
        }
    }

    @Override
    public void removed(net.minecraft.world.entity.player.Player p) {
        super.removed(p);

        if (!(p instanceof ServerPlayer)) return;
        if (teamId == null) return;

        Container container = getContainer();
        Map<String, Integer> after = new HashMap<>();

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                after.merge(
                        stack.getHoverName().getString(), // ✅ HUMAN-READABLE
                        stack.getCount(),
                        Integer::sum
                );
            }
        }

        java.util.List<String> addedParts = new java.util.ArrayList<>();
        java.util.List<String> removedParts = new java.util.ArrayList<>();

        for (Map.Entry<String, Integer> e : after.entrySet()) {
            int before = snapshot.getOrDefault(e.getKey(), 0);
            int delta = e.getValue() - before;
            if (delta > 0) {
                addedParts.add("+" + delta + " " + e.getKey());
            }
        }

        for (Map.Entry<String, Integer> e : snapshot.entrySet()) {
            int now = after.getOrDefault(e.getKey(), 0);
            int delta = now - e.getValue();
            if (delta < 0) {
                removedParts.add(delta + " " + e.getKey());
            }
        }

        if (addedParts.isEmpty() && removedParts.isEmpty()) return;

        StringBuilder msg = new StringBuilder();
        msg.append("\u00A77[\u00A7bTeam Chest\u00A77] \u00A7e")
                .append(player.getName().getString());
        if (!addedParts.isEmpty()) {
            msg.append(" \u00A7aadded \u00A7f").append(String.join(", ", addedParts));
        }
        if (!removedParts.isEmpty()) {
            msg.append(" \u00A7cremoved \u00A7f").append(String.join(", ", removedParts));
        }

        var server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        TeamData teamData = TeamData.get(server);
        TeamData.TeamInfo team = null;
        for (TeamData.TeamInfo info : teamData.getTeams()) {
            if (info.id.equals(teamId)) {
                team = info;
                break;
            }
        }
        if (team == null) return;

        for (UUID member : team.members) {
            ServerPlayer sp = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, member);
            if (sp == null) continue;
            sp.playSound(net.minecraft.sounds.SoundEvents.BELL_BLOCK, 0.8f, 1.0f);
            sp.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(msg.toString()));
        }
    }
}



