package com.jamie.jamiebingo.addons.effects;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import com.jamie.jamiebingo.util.EntityLevelUtil;
import com.jamie.jamiebingo.util.ServerPlayerTeleportUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class EffectPlayerSwap implements CustomRandomEffect {

    private static final Random RNG = new Random();

    @Override
    public String id() {
        return "player_swap";
    }

    @Override
    public String displayName() {
        return "Player Swap";
    }

    @Override
    public void onApply(MinecraftServer server) {

        List<ServerPlayer> players =
                new ArrayList<>(com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server));

        // Exclude spectators from swaps
        players.removeIf(ServerPlayer::isSpectator);

        // Need at least 2 players
        if (players.size() < 2) return;

        // ------------------------------
        // SNAPSHOT PLAYER STATES
        // ------------------------------

        record Snapshot(
                ServerPlayer player,
                ServerLevel level,
                Vec3 pos,
                float yaw,
                float pitch
        ) {}

        List<Snapshot> snapshots = new ArrayList<>();

        for (ServerPlayer p : players) {
            snapshots.add(new Snapshot(
                    p,
                    (ServerLevel) EntityLevelUtil.getLevel(p),
                    p.position(),
                    com.jamie.jamiebingo.util.EntityRotationUtil.getYRot(p),
                    com.jamie.jamiebingo.util.EntityRotationUtil.getXRot(p)
            ));
        }

        // ------------------------------
        // BUILD A DERANGEMENT
        // ------------------------------

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < snapshots.size(); i++) {
            indices.add(i);
        }

        // Shuffle until no one maps to themselves
        boolean valid;
        do {
            Collections.shuffle(indices, RNG);
            valid = true;
            for (int i = 0; i < indices.size(); i++) {
                if (indices.get(i) == i) {
                    valid = false;
                    break;
                }
            }
        } while (!valid);

        // ------------------------------
        // TELEPORT EVERYONE
        // ------------------------------

        for (int i = 0; i < snapshots.size(); i++) {

            Snapshot from = snapshots.get(i);
            Snapshot to = snapshots.get(indices.get(i));

            ServerPlayerTeleportUtil.teleport(
                    from.player,
                    to.level,
                    to.pos.x,
                    to.pos.y,
                    to.pos.z,
                    java.util.Set.of(),
                    to.yaw,
                    to.pitch,
                    false
            );
        }
    }

    @Override
    public void onRemove(MinecraftServer server) {
        // no cleanup
    }

    @Override
    public void onTick(MinecraftServer server) {
        // one-shot effect
    }
}


