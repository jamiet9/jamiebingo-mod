package com.jamie.jamiebingo.data;


import com.jamie.jamiebingo.util.ServerLevelUtil;
import com.jamie.jamiebingo.bingo.*;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public class TeamScoreData extends SavedData {

    private static final String DATA_NAME = "jamie_bingo_team_scores";
    private static final Codec<TeamScoreData> CODEC = Codec.of(
            new Encoder<>() {
                @Override
                public <T> DataResult<T> encode(TeamScoreData data, DynamicOps<T> ops, T prefix) {
                    CompoundTag tag = new CompoundTag();
                    data.save(tag);
                    return DataResult.success(com.jamie.jamiebingo.util.NbtOpsUtil.instance().convertTo(ops, tag));
                }
            },
            new Decoder<>() {
                @Override
                public <T> DataResult<Pair<TeamScoreData, T>> decode(DynamicOps<T> ops, T input) {
                    CompoundTag tag = (CompoundTag) ops.convertTo(com.jamie.jamiebingo.util.NbtOpsUtil.instance(), input);
                    return DataResult.success(Pair.of(load(tag), input));
                }
            }
    );
    private static final SavedDataType<TeamScoreData> TYPE =
            new SavedDataType<>(DATA_NAME, TeamScoreData::new, CODEC, DataFixTypes.LEVEL);

    private final Map<UUID, Integer> teamScores = new HashMap<>();
    private final Map<UUID, Map<UUID, Integer>> contributions = new HashMap<>();
    private static final java.util.Map<MinecraftServer, TeamScoreData> FALLBACK_BY_SERVER =
            new java.util.WeakHashMap<>();

    public TeamScoreData() {}

    /* =====================
       LEGACY SCORING
       ===================== */

    public void award(UUID teamId, UUID playerId, int amount) {
        teamScores.put(teamId, teamScores.getOrDefault(teamId, 0) + amount);

        contributions
                .computeIfAbsent(teamId, t -> new HashMap<>())
                .merge(playerId, amount, Integer::sum);

        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    /* =====================
       RARITY MODE SCORING
       (FULL RECOMPUTE)
       ===================== */

    public void recomputeRarityScores(
            MinecraftServer server,
            BingoGameData data
    ) {
        teamScores.clear();
        ensureTeamsExist(server);

        TeamData teamData = TeamData.get(server);

        // slotId -> owning team color
        Map<String, DyeColor> ownership =
                data.getSlotOwnershipSnapshot();

        for (TeamData.TeamInfo team : teamData.getTeams()) {

            UUID teamId = team.id;
            DyeColor teamColor = team.color;

            teamScores.put(teamId, 0);

            if (team.members.isEmpty()) continue;
            BingoCard card = data.getActiveCardForTeam(teamId);
            if (card == null) continue;
            Set<String> completed = data.getTeamProgressForDisplay(teamId);

            // slots doubled by completed lines (team-specific)
            Set<String> doubled = findDoubledSlots(card, completed);

            int total = 0;

            for (int y = 0; y < card.getSize(); y++) {
                for (int x = 0; x < card.getSize(); x++) {

                    BingoSlot slot = card.getSlot(x, y);
                    if (slot == null) continue;
                    String slotId = slot.getId();

                    if (!completed.contains(slotId)) continue;

                    DyeColor owner = ownership.get(slotId);
                    if (owner == null || owner != teamColor) continue;

                    int base = RarityScoreCalculator.base(slot);
                    int value = doubled.contains(slotId) ? base * 2 : base;

                    total += value;

                }
            }

            teamScores.put(teamId, total);
            rebalanceContributions(teamId, team.members, total);
        }

        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public void recomputeStandardScores(
            MinecraftServer server,
            BingoGameData data
    ) {
        teamScores.clear();
        ensureTeamsExist(server);

        TeamData teamData = TeamData.get(server);

        for (TeamData.TeamInfo team : teamData.getTeams()) {
            UUID teamId = team.id;

            if (team.members.isEmpty()) {
                teamScores.put(teamId, 0);
                continue;
            }

            Set<String> completed = data.getTeamProgressForDisplay(teamId);
            int total = completed != null ? completed.size() : 0;

            teamScores.put(teamId, total);
            rebalanceContributions(teamId, team.members, total);
        }

        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    private void rebalanceContributions(UUID teamId, Collection<UUID> members, int targetTotal) {
        Map<UUID, Integer> current = contributions.computeIfAbsent(teamId, t -> new HashMap<>());
        Set<UUID> memberSet = new HashSet<>(members);
        current.keySet().removeIf(id -> !memberSet.contains(id));
        for (UUID member : members) {
            current.putIfAbsent(member, 0);
        }

        if (targetTotal <= 0 || members.isEmpty()) {
            for (UUID member : members) {
                current.put(member, 0);
            }
            return;
        }

        int existingTotal = 0;
        for (UUID member : members) {
            existingTotal += Math.max(0, current.getOrDefault(member, 0));
        }

        List<UUID> ordered = new ArrayList<>(members);
        ordered.sort(UUID::compareTo);
        if (existingTotal <= 0) {
            int base = targetTotal / ordered.size();
            int remainder = targetTotal % ordered.size();
            for (int i = 0; i < ordered.size(); i++) {
                current.put(ordered.get(i), base + (i < remainder ? 1 : 0));
            }
            return;
        }

        Map<UUID, Integer> scaled = new HashMap<>();
        int assigned = 0;
        for (UUID member : ordered) {
            int value = Math.max(0, current.getOrDefault(member, 0));
            int next = (int) Math.floor((value / (double) existingTotal) * targetTotal);
            scaled.put(member, next);
            assigned += next;
        }
        int remainder = targetTotal - assigned;
        int idx = 0;
        while (remainder > 0 && !ordered.isEmpty()) {
            UUID member = ordered.get(idx % ordered.size());
            scaled.put(member, scaled.getOrDefault(member, 0) + 1);
            remainder--;
            idx++;
        }
        current.clear();
        current.putAll(scaled);
    }

    /* =====================
       LINE DETECTION
       ===================== */

    private Set<String> findDoubledSlots(
            BingoCard card,
            Set<String> completed
    ) {
        Set<String> doubled = new HashSet<>();
        int size = card.getSize();

        // rows
        for (int y = 0; y < size; y++) {
            boolean ok = true;
            for (int x = 0; x < size; x++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot == null || !completed.contains(slot.getId())) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                for (int x = 0; x < size; x++) {
                    BingoSlot slot = card.getSlot(x, y);
                    if (slot != null) {
                        doubled.add(slot.getId());
                    }
                }
            }
        }

        // columns
        for (int x = 0; x < size; x++) {
            boolean ok = true;
            for (int y = 0; y < size; y++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot == null || !completed.contains(slot.getId())) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                for (int y = 0; y < size; y++) {
                    BingoSlot slot = card.getSlot(x, y);
                    if (slot != null) {
                        doubled.add(slot.getId());
                    }
                }
            }
        }

        // main diagonal
        boolean main = true;
        for (int i = 0; i < size; i++) {
            BingoSlot slot = card.getSlot(i, i);
            if (slot == null || !completed.contains(slot.getId())) {
                main = false;
                break;
            }
        }
        if (main) {
            for (int i = 0; i < size; i++) {
                BingoSlot slot = card.getSlot(i, i);
                if (slot != null) {
                    doubled.add(slot.getId());
                }
            }
        }

        // anti diagonal
        boolean anti = true;
        for (int i = 0; i < size; i++) {
            BingoSlot slot = card.getSlot(size - 1 - i, i);
            if (slot == null || !completed.contains(slot.getId())) {
                anti = false;
                break;
            }
        }
        if (anti) {
            for (int i = 0; i < size; i++) {
                BingoSlot slot = card.getSlot(size - 1 - i, i);
                if (slot != null) {
                    doubled.add(slot.getId());
                }
            }
        }

        return doubled;
    }

    /* =====================
       ACCESSORS
       ===================== */

    public int getTeamScore(UUID teamId) {
        return teamScores.getOrDefault(teamId, 0);
    }

    public Map<UUID, Integer> getAllTeamScores() {
        return Collections.unmodifiableMap(teamScores);
    }

    public Map<UUID, Integer> getTeamContributions(UUID teamId) {
        return Collections.unmodifiableMap(
                contributions.getOrDefault(teamId, Map.of())
        );
    }

    /* =====================
       LIFECYCLE
       ===================== */

    public void reset() {
        teamScores.clear();
        contributions.clear();
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    /* =====================
       SAVE / LOAD
       ===================== */

   public static TeamScoreData load(CompoundTag tag) {
    TeamScoreData data = new TeamScoreData();
     CompoundTag teams = tag.getCompoundOrEmpty("Teams");

     for (String key : teams.keySet()) {
        UUID teamId = UUID.fromString(key);
        data.teamScores.put(teamId, com.jamie.jamiebingo.util.NbtUtil.getInt(teams, key, 0));
        data.contributions.put(teamId, new HashMap<>());
    }

    return data;
}

    public CompoundTag save(CompoundTag tag) {
        CompoundTag teams = new CompoundTag();
        for (var e : teamScores.entrySet()) {
            com.jamie.jamiebingo.util.NbtUtil.putInt(teams, e.getKey().toString(), e.getValue());
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "Teams", teams);
        return tag;
    }

    public static TeamScoreData get(ServerLevel level) {
        if (level == null) return new TeamScoreData();
        var storage = com.jamie.jamiebingo.util.LevelDataStorageUtil.getDataStorage(level);
        TeamScoreData data = com.jamie.jamiebingo.util.SavedDataUtil.computeIfAbsent(storage, TYPE);
        return data != null ? data : new TeamScoreData();
    }

    public static TeamScoreData get(MinecraftServer server) {
        if (server == null) return new TeamScoreData();
        var overworld = ServerLevelUtil.getOverworld(server);
        if (overworld == null) {
            overworld = ServerLevelUtil.getAnyLevel(server);
        }
        if (overworld == null) {
            return FALLBACK_BY_SERVER.computeIfAbsent(server, s -> new TeamScoreData());
        }
        var storage = com.jamie.jamiebingo.util.LevelDataStorageUtil.getDataStorage(overworld);
        if (storage == null) {
            return FALLBACK_BY_SERVER.computeIfAbsent(server, s -> new TeamScoreData());
        }
        TeamScoreData data = com.jamie.jamiebingo.util.SavedDataUtil.computeIfAbsent(storage, TYPE);
        if (data == null) {
            TeamScoreData fallback = FALLBACK_BY_SERVER.computeIfAbsent(server, s -> new TeamScoreData());
            if (!com.jamie.jamiebingo.util.SavedDataUtil.set(storage, TYPE, fallback)) {
                return fallback;
            }
            return fallback;
        }
        return data;
    }

public void ensureTeamsExist(MinecraftServer server) {
    TeamData teamData = TeamData.get(server);

    for (TeamData.TeamInfo team : teamData.getTeams()) {
        teamScores.putIfAbsent(team.id, 0);
        Map<UUID, Integer> map = contributions.computeIfAbsent(team.id, t -> new HashMap<>());
        for (UUID member : team.members) {
            map.putIfAbsent(member, 0);
        }
    }

    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
}
}




