package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.ClientTeamScoreData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.event.network.CustomPayloadEvent;
import com.jamie.jamiebingo.util.FriendlyByteBufUtil;

import java.util.*;
import java.util.function.Supplier;

public class PacketSyncTeamScores {

    public static class TeamPayload {
        public final UUID teamId;
        public final DyeColor color;
        public final int totalScore;
        public final int completedLines;
        public final Map<UUID, Integer> memberScores;
        public final Map<UUID, String> memberNames;

        public TeamPayload(
                UUID teamId,
                DyeColor color,
                int totalScore,
                int completedLines,
                Map<UUID, Integer> memberScores,
                Map<UUID, String> memberNames
        ) {
            this.teamId = teamId;
            this.color = color;
            this.totalScore = totalScore;
            this.completedLines = completedLines;
            this.memberScores = memberScores;
            this.memberNames = memberNames;
        }
    }

    private final List<TeamPayload> teams;

    public PacketSyncTeamScores(List<TeamPayload> teams) {
        this.teams = teams;
    }

    /* =========================
       SERIALIZATION
       ========================= */

    public static void encode(PacketSyncTeamScores msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.teams.size());

        for (TeamPayload team : msg.teams) {
            FriendlyByteBufUtil.writeUUID(buf, team.teamId);
            FriendlyByteBufUtil.writeEnum(buf, team.color);
            buf.writeInt(team.totalScore);
            buf.writeInt(team.completedLines);

            buf.writeInt(team.memberScores.size());
            for (UUID playerId : team.memberScores.keySet()) {
                FriendlyByteBufUtil.writeUUID(buf, playerId);
                buf.writeInt(team.memberScores.get(playerId));
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, team.memberNames.getOrDefault(playerId, "Player"));
            }
        }
    }

    public static PacketSyncTeamScores decode(FriendlyByteBuf buf) {
        int teamCount = buf.readInt();
        List<TeamPayload> teams = new ArrayList<>();

        for (int i = 0; i < teamCount; i++) {
            UUID teamId = FriendlyByteBufUtil.readUUID(buf);
            DyeColor color = FriendlyByteBufUtil.readEnum(buf, DyeColor.class);
            int totalScore = buf.readInt();
            int completedLines = buf.readInt();

            int memberCount = buf.readInt();
            Map<UUID, Integer> memberScores = new HashMap<>();
            Map<UUID, String> memberNames = new HashMap<>();

            for (int m = 0; m < memberCount; m++) {
                UUID playerId = FriendlyByteBufUtil.readUUID(buf);
                int score = buf.readInt();
                String name = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);

                memberScores.put(playerId, score);
                memberNames.put(playerId, name);
            }

            teams.add(new TeamPayload(
                    teamId,
                    color,
                    totalScore,
                    completedLines,
                    memberScores,
                    memberNames
            ));
        }

        return new PacketSyncTeamScores(teams);
    }

    /* =========================
       HANDLER
       ========================= */

    public static void handle(PacketSyncTeamScores msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ClientTeamScoreData.update(msg.teams);
        });
        ctx.setPacketHandled(true);
    }
}

