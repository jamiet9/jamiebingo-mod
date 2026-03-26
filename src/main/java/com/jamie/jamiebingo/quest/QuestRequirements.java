package com.jamie.jamiebingo.quest;

import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.data.TeamData;
import net.minecraft.server.MinecraftServer;

public class QuestRequirements {

    public final boolean requireHostileMobs;
    public final boolean requireHunger;
    public final boolean requireEffectsDisabled;
    public final boolean requireRtpDisabled;
    public final boolean requirePvpEnabled;
    public final boolean requireDaylightNotDayOnly;
    public final int minTeams;
    public final int maxTeams;
    public final int minPlayers;
    public final int maxPlayers;

    public QuestRequirements(
            boolean requireHostileMobs,
            boolean requireHunger,
            boolean requireEffectsDisabled,
            boolean requireRtpDisabled,
            boolean requirePvpEnabled,
            boolean requireDaylightNotDayOnly,
            int minTeams,
            int maxTeams,
            int minPlayers,
            int maxPlayers
    ) {
        this.requireHostileMobs = requireHostileMobs;
        this.requireHunger = requireHunger;
        this.requireEffectsDisabled = requireEffectsDisabled;
        this.requireRtpDisabled = requireRtpDisabled;
        this.requirePvpEnabled = requirePvpEnabled;
        this.requireDaylightNotDayOnly = requireDaylightNotDayOnly;
        this.minTeams = minTeams;
        this.maxTeams = maxTeams;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
    }

    public boolean isEligible(MinecraftServer server, BingoGameData data) {
        if (data == null) return false;

        if (requireHostileMobs && !data.hostileMobsEnabled) return false;
        if (requireHunger && !data.hungerEnabled) return false;
        if (requireEffectsDisabled && data.randomEffectsIntervalSeconds > 0) return false;
        if (requireRtpDisabled && data.rtpEnabled) return false;
        if (requirePvpEnabled && !data.pvpEnabled) return false;

        if (requireDaylightNotDayOnly && data.daylightMode == BingoGameData.DAYLIGHT_DAY) {
            return false;
        }

        if (server == null) return true;

        TeamData teamData = TeamData.get(server);
        int teamCount = (int) teamData.getTeams().stream()
                .filter(t -> !t.members.isEmpty())
                .count();
        int playerCount = (int) teamData.getTeams().stream()
                .mapToLong(t -> t.members.size())
                .sum();

        if (minTeams > 0 && teamCount < minTeams) return false;
        if (maxTeams > 0 && teamCount > maxTeams) return false;
        if (minPlayers > 0 && playerCount < minPlayers) return false;
        if (maxPlayers > 0 && playerCount > maxPlayers) return false;

        return true;
    }

    public static QuestRequirements none() {
        return new QuestRequirements(false, false, false, false, false, false, 0, 0, 0, 0);
    }
}
