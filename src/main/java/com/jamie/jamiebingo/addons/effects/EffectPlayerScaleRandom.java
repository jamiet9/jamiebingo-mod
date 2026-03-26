package com.jamie.jamiebingo.addons.effects;

import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Random;

public class EffectPlayerScaleRandom implements CustomRandomEffect {

    private static final Random RNG = new Random();

    @Override
    public String id() {
        return "player_scale_random";
    }

    @Override
    public String displayName() {
        return "Random Player Scale";
    }

    @Override
    public void onApply(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : BingoGameData.get(server).getParticipantPlayers(server)) {
            if (player == null) continue;
            double scale = 0.2 + (RNG.nextDouble() * 4.8);
            runScaleCommand(player, scale);
        }
    }

    @Override
    public void onRemove(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : BingoGameData.get(server).getParticipantPlayers(server)) {
            if (player == null) continue;
            runScaleCommand(player, 1.0);
        }
    }

    private static void runScaleCommand(ServerPlayer player, double value) {
        if (player == null) return;
        MinecraftServer server = com.jamie.jamiebingo.util.ServerPlayerUtil.getServer(player);
        if (server == null) return;

        String formatted = String.format(java.util.Locale.ROOT, "%.2f", value);
        var source = player.createCommandSourceStack().withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, "attribute @s minecraft:scale base set " + formatted);
        server.getCommands().performPrefixedCommand(source, "attribute @s minecraft:generic.scale base set " + formatted);
    }
}
