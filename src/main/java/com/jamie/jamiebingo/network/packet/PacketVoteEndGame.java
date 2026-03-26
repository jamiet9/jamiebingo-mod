package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.data.BroadcastHelper;
import com.jamie.jamiebingo.data.TeamChestData;
import com.jamie.jamiebingo.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public class PacketVoteEndGame {
    private static final Map<MinecraftServer, Set<UUID>> VOTES = new WeakHashMap<>();

    public PacketVoteEndGame() {
    }

    public static void encode(PacketVoteEndGame msg, FriendlyByteBuf buf) {
    }

    public static PacketVoteEndGame decode(FriendlyByteBuf buf) {
        return new PacketVoteEndGame();
    }

    public static void clearVotes(MinecraftServer server) {
        if (server == null) return;
        synchronized (VOTES) {
            VOTES.remove(server);
        }
        syncVotesToAll(server, 0, 0, null);
    }

    public static void handle(PacketVoteEndGame msg, CustomPayloadEvent.Context ctx) {
        CustomPayloadEvent.Context context = ctx;
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
            if (server == null) return;
            BingoGameData data = BingoGameData.get(server);
            if (!data.isActive()) {
                clearVotes(server);
                player.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("No active game to vote-stop."));
                return;
            }

            Set<UUID> online = new HashSet<>();
            for (ServerPlayer p : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                online.add(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
            }
            if (online.isEmpty()) return;

            int voted;
            int total = online.size();
            UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
            synchronized (VOTES) {
                Set<UUID> votes = VOTES.computeIfAbsent(server, s -> new HashSet<>());
                votes.retainAll(online);
                if (!votes.add(playerId)) {
                    voted = votes.size();
                    player.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("You already voted. (" + voted + "/" + total + ")"));
                    syncVotesToAll(server, voted, total, votes);
                    return;
                }
                voted = votes.size();
                syncVotesToAll(server, voted, total, votes);
                if (voted >= total) {
                    clearVotes(server);
                    TeamChestData.get(server).clearAll();
                    data.stopGame();
                    for (ServerPlayer p : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                        p.setHealth(p.getMaxHealth());
                    }
                    BroadcastHelper.broadcast(server, com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Game stopped by unanimous vote."));
                    BroadcastHelper.broadcastFullSync();
                    return;
                }
            }
            BroadcastHelper.broadcast(server, com.jamie.jamiebingo.util.ComponentUtil.literal(
                    "[Bingo] " + player.getName().getString() + " voted to end this game, " + (total - voted) + " votes remaining. (" + voted + "/" + total + ")"
            ));
        });
        context.setPacketHandled(true);
    }

    private static void syncVotesToAll(MinecraftServer server, int voted, int total, Set<UUID> votes) {
        if (server == null) return;
        for (ServerPlayer p : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            UUID id = com.jamie.jamiebingo.util.EntityUtil.getUUID(p);
            boolean localVoted = votes != null && votes.contains(id);
            NetworkHandler.sendToPlayer(p, new PacketVoteEndGameStatus(voted, total, localVoted));
        }
    }
}
