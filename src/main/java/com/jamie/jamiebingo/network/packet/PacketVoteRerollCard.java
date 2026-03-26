package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.data.BroadcastHelper;
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

public class PacketVoteRerollCard {
    private static final Map<MinecraftServer, Set<UUID>> VOTES = new WeakHashMap<>();

    public PacketVoteRerollCard() {
    }

    public static void encode(PacketVoteRerollCard msg, FriendlyByteBuf buf) {
    }

    public static PacketVoteRerollCard decode(FriendlyByteBuf buf) {
        return new PacketVoteRerollCard();
    }

    public static void clearVotes(MinecraftServer server) {
        if (server == null) return;
        synchronized (VOTES) {
            VOTES.remove(server);
        }
        syncVotesToAll(server, 0, 0, null);
    }

    public static void handle(PacketVoteRerollCard msg, CustomPayloadEvent.Context ctx) {
        CustomPayloadEvent.Context context = ctx;
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
            if (server == null) return;
            BingoGameData data = BingoGameData.get(server);
            if (!data.isActive()) {
                clearVotes(server);
                player.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("No active game to reroll unclaimed slots."));
                return;
            }
            if (data.fakeRerollsEnabled) {
                clearVotes(server);
                player.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Vote reroll is disabled while fake rerolls are enabled."));
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
                    int changed = data.rerollCurrentGoalsForVote(server);
                    if (changed > 0) {
                        data.markRunVoteRerollUsed();
                    }
                    if (changed > 0) {
                        BroadcastHelper.broadcast(server, com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Unclaimed slots rerolled by unanimous vote."));
                        BroadcastHelper.broadcastFullSync();
                    } else {
                        BroadcastHelper.broadcast(server, com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Vote passed, but there were no rerollable unclaimed slots."));
                    }
                    return;
                }
            }
            BroadcastHelper.broadcast(server, com.jamie.jamiebingo.util.ComponentUtil.literal(
                    "[Bingo] " + player.getName().getString() + " voted to reroll unclaimed slots, " + (total - voted) + " votes remaining. (" + voted + "/" + total + ")"
            ));
        });
        context.setPacketHandled(true);
    }

    private static void syncVotesToAll(MinecraftServer server, int voted, int total, Set<UUID> votes) {
        if (server == null) return;
        for (ServerPlayer p : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            UUID id = com.jamie.jamiebingo.util.EntityUtil.getUUID(p);
            boolean localVoted = votes != null && votes.contains(id);
            NetworkHandler.sendToPlayer(p, new PacketVoteRerollCardStatus(voted, total, localVoted));
        }
    }
}
