package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.network.PacketSyncTeamScores;
import net.minecraft.world.item.DyeColor;

import java.util.*;

public class ClientTeamScoreData {

    public static class TeamEntry {
        public UUID teamId;
        public DyeColor color;
        public int totalScore;
        public int completedLines;
        public Map<UUID, Integer> memberScores = new HashMap<>();
        public Map<UUID, String> memberNames = new HashMap<>();
    }

    private static final Map<UUID, TeamEntry> teams = new HashMap<>();
    private static final Map<UUID, Float> animatedY = new HashMap<>();

    private ClientTeamScoreData() {}

    /* =========================
       UPDATE FROM PACKET
       ========================= */

    public static void update(List<PacketSyncTeamScores.TeamPayload> payloads) {
        teams.clear();

        for (PacketSyncTeamScores.TeamPayload p : payloads) {
            TeamEntry entry = new TeamEntry();
            entry.teamId = p.teamId;
            entry.color = p.color;
            entry.totalScore = p.totalScore;
            entry.completedLines = p.completedLines;
            entry.memberScores.putAll(p.memberScores);
            entry.memberNames.putAll(p.memberNames);
            teams.put(p.teamId, entry);
        }
    }

    /* =========================
       ACCESSORS
       ========================= */

    public static boolean hasData() {
        return !teams.isEmpty();
    }

    public static Collection<TeamEntry> getTeamsSorted() {
        List<TeamEntry> list = new ArrayList<>(teams.values());
        list.sort((a, b) -> Integer.compare(b.totalScore, a.totalScore));
        return list;
    }

    public static int getTeamColorARGB(TeamEntry team) {
        int rgb = team.color.getTextColor();
        return 0xFF000000 | rgb;
    }

    public static float getAnimatedY(UUID teamId, float targetY, float speed) {
        float current = animatedY.getOrDefault(teamId, targetY);
        current += (targetY - current) * speed;
        animatedY.put(teamId, current);
        return current;
    }

    public static void clear() {
        teams.clear();
        animatedY.clear();
    }
}
