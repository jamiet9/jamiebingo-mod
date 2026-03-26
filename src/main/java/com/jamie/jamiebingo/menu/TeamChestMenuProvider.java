package com.jamie.jamiebingo.menu;

import com.jamie.jamiebingo.data.TeamChestData;
import com.jamie.jamiebingo.data.TeamData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;

import java.util.*;

public class TeamChestMenuProvider {

    public static SimpleMenuProvider create(ServerPlayer opener) {
        TeamData teamData = TeamData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(opener));
        TeamChestData chestData = TeamChestData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(opener));

        UUID teamId = teamData.ensureAssigned(opener);
        Container chest = chestData.getChest(teamId);

        return new SimpleMenuProvider(
                (id, inv, p) -> new TeamChestMenu(
                        net.minecraft.world.inventory.MenuType.GENERIC_9x3,
                        id,
                        opener,
                        chest,
                        3
                ),
                com.jamie.jamiebingo.util.ComponentUtil.literal("Team Ender Chest")
        );
    }
}



