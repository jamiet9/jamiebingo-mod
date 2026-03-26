package com.jamie.jamiebingo.util;

import com.jamie.jamiebingo.data.TeamData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.jamie.jamiebingo.util.DataComponents;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;
import java.util.Random;

public class FireworkHelper {

    private static final Random RANDOM = new Random();

    /* =====================================================
       SINGLE TEAM FIREWORK (ITEM / QUEST COMPLETION)
       ===================================================== */

    public static void launchTeamFirework(ServerPlayer player) {

        TeamData teamData = TeamData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        var teamId = teamData.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
        if (teamId == null) return;

        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);

        if (team == null) return;

        spawnFirework(player,
                player.getX(),
                player.getY() + 1.2,
                player.getZ(),
                team.color,
                1
        );
    }

    /* =====================================================
       WIN CELEBRATION BURST (STEP E)
       ===================================================== */

    public static void launchWinCelebration(ServerPlayer player) {

        TeamData teamData = TeamData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        var teamId = teamData.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
        if (teamId == null) return;

        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);

        if (team == null) return;

        // 🎆 5–7 fireworks around the player
        int count = 5 + RANDOM.nextInt(3);

        for (int i = 0; i < count; i++) {

            double ox = (RANDOM.nextDouble() - 0.5) * 1.6;
            double oz = (RANDOM.nextDouble() - 0.5) * 1.6;
            double oy = RANDOM.nextDouble() * 0.8;

            spawnFirework(
                    player,
                    player.getX() + ox,
                    player.getY() + 1.0 + oy,
                    player.getZ() + oz,
                    team.color,
                    2 + RANDOM.nextInt(2)
            );
        }
    }

    /* =====================================================
       INTERNAL SPAWNER
       ===================================================== */

    private static void spawnFirework(
            ServerPlayer player,
            double x,
            double y,
            double z,
            DyeColor color,
            int power
    ) {

        ItemStack stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:firework_rocket");

        IntList colors = new IntArrayList(new int[]{color.getTextColor()});
        FireworkExplosion.Shape shape = RANDOM.nextBoolean()
                ? FireworkExplosion.Shape.BURST
                : FireworkExplosion.Shape.SMALL_BALL;
        FireworkExplosion explosion = new FireworkExplosion(shape, colors, IntArrayList.of(), true, true);
        ItemStackComponentUtil.set(
                stack,
                DataComponents.FIREWORKS,
                new Fireworks(power, List.of(explosion))
        );

        net.minecraft.world.level.Level level = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(player);
        if (level == null) return;
        FireworkRocketEntity rocket = new FireworkRocketEntity(
                level,
                x, y, z,
                stack
        );

        com.jamie.jamiebingo.util.NbtUtil.putBoolean(rocket.getPersistentData(), "jamiebingo_no_damage", true);
        rocket.setNoGravity(true);
        rocket.setSilent(false); // allow boom sound
        level.addFreshEntity(rocket);
    }
}


