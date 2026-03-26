package com.jamie.jamiebingo.data;


import com.jamie.jamiebingo.util.ServerLevelUtil;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class TeamData extends SavedData {

    private static final String DATA_NAME = "jamiebingo_teams";
    private static final Codec<TeamData> CODEC = Codec.of(
            new Encoder<>() {
                @Override
                public <T> DataResult<T> encode(TeamData data, DynamicOps<T> ops, T prefix) {
                    CompoundTag tag = new CompoundTag();
                    data.save(tag);
                    return DataResult.success(com.jamie.jamiebingo.util.NbtOpsUtil.instance().convertTo(ops, tag));
                }
            },
            new Decoder<>() {
                @Override
                public <T> DataResult<Pair<TeamData, T>> decode(DynamicOps<T> ops, T input) {
                    CompoundTag tag = (CompoundTag) ops.convertTo(com.jamie.jamiebingo.util.NbtOpsUtil.instance(), input);
                    return DataResult.success(Pair.of(load(tag), input));
                }
            }
    );
    private static final SavedDataType<TeamData> TYPE =
            new SavedDataType<>(DATA_NAME, TeamData::new, CODEC, DataFixTypes.LEVEL);

    // Existing structures (unchanged)
    private final Map<UUID, TeamInfo> teams = new HashMap<>();
    private final Map<UUID, UUID> playerToTeam = new HashMap<>();
    private final Map<UUID, DyeColor> preferredTeamColorByPlayer = new HashMap<>();

    // NEW: canonical color → team UUID
    private final EnumMap<DyeColor, UUID> colorTeams = new EnumMap<>(DyeColor.class);
    private static final java.util.Map<MinecraftServer, TeamData> FALLBACK_BY_SERVER =
            new java.util.WeakHashMap<>();

    public TeamData() {}

    /* ============================
       PUBLIC API (SERVER ONLY)
       ============================ */

    public UUID ensureAssigned(net.minecraft.server.level.ServerPlayer player) {
        UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
        if (!preferredTeamColorByPlayer.containsKey(playerId)) {
            int preferredColorId = com.jamie.jamiebingo.util.NbtUtil.getInt(
                    player.getPersistentData(),
                    com.jamie.jamiebingo.menu.TeamSelectMenu.PREF_TEAM_COLOR_TAG,
                    -1
            );
            if (preferredColorId >= 0 && preferredColorId < DyeColor.values().length) {
                preferredTeamColorByPlayer.put(playerId, DyeColor.byId(preferredColorId));
            }
        }

        UUID existing = playerToTeam.get(playerId);
        if (existing != null) {
            TeamInfo existingTeam = teams.get(existing);
            if (existingTeam != null) {
                preferredTeamColorByPlayer.put(playerId, existingTeam.color);
            }
            return existing;
        }

        DyeColor preferred = preferredTeamColorByPlayer.get(playerId);
        if (preferred != null) {
            UUID preferredTeamId = getOrCreateTeamForColor(preferred);
            TeamInfo preferredTeam = teams.get(preferredTeamId);
            if (preferredTeam != null) {
                preferredTeam.members.add(playerId);
                playerToTeam.put(playerId, preferredTeamId);
                com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
                return preferredTeamId;
            }
        }

        // Assign first available color
        for (DyeColor color : DyeColor.values()) {
            UUID teamId = getOrCreateTeamForColor(color);
            TeamInfo team = teams.get(teamId);
            if (team.members.isEmpty()) {
                team.members.add(playerId);
                playerToTeam.put(playerId, teamId);
                preferredTeamColorByPlayer.put(playerId, color);
                com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
                return teamId;
            }
        }

        // Fallback (should never happen)
        DyeColor color = DyeColor.values()[0];
        UUID teamId = getOrCreateTeamForColor(color);
        teams.get(teamId).members.add(playerId);
        playerToTeam.put(playerId, teamId);
        preferredTeamColorByPlayer.put(playerId, color);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        return teamId;
    }

    public void movePlayerToTeam(UUID playerId, UUID teamId) {
        UUID old = playerToTeam.get(playerId);

        if (old != null && old.equals(teamId))
            return;

        if (old != null) {
            TeamInfo oldTeam = teams.get(old);
            if (oldTeam != null) {
                oldTeam.members.remove(playerId);
            }
        }

        TeamInfo newTeam = teams.get(teamId);
        if (newTeam != null) {
            newTeam.members.add(playerId);
            playerToTeam.put(playerId, teamId);
            preferredTeamColorByPlayer.put(playerId, newTeam.color);
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);

            // ✅ NEW: push live update so clients don’t keep stale team member lists
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                BroadcastHelper.broadcastTeamScores(server);
            }
        }
    }

    public void setPreferredTeamColor(UUID playerId, DyeColor color) {
        if (playerId == null || color == null) return;
        preferredTeamColorByPlayer.put(playerId, color);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }
    public UUID getTeamForPlayer(UUID playerId) {
        return playerToTeam.get(playerId);
    }

    public void removePlayerFromTeam(UUID playerId) {
        if (playerId == null) return;
        UUID teamId = playerToTeam.remove(playerId);
        if (teamId == null) return;
        TeamInfo team = teams.get(teamId);
        if (team != null) {
            team.members.remove(playerId);
        }
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public Collection<TeamInfo> getTeams() {
        return teams.values();
    }

    /* ============================
       NEW CANONICAL COLOR TEAMS
       ============================ */

    public UUID getOrCreateTeamForColor(DyeColor color) {
        UUID existing = colorTeams.get(color);
        if (existing != null) {
            return existing;
        }

        UUID id = UUID.randomUUID();
        TeamInfo info = new TeamInfo(id, color);
        teams.put(id, info);
        colorTeams.put(color, id);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        return id;
    }

    public UUID getTeamForColor(DyeColor color) {
        return colorTeams.get(color);
    }

    /* ============================
       SAVED DATA
       ============================ */

    public CompoundTag save(CompoundTag tag) {
        ListTag teamsTag = new ListTag();

        for (TeamInfo team : teams.values()) {
            CompoundTag t = new CompoundTag();
            putUuid(t, "Id", team.id);
            com.jamie.jamiebingo.util.NbtUtil.putInt(t, "Color", team.color.getId());

            ListTag members = new ListTag();
            for (UUID u : team.members) {
                CompoundTag m = new CompoundTag();
                putUuid(m, "UUID", u);
                members.add(m);
            }

            com.jamie.jamiebingo.util.NbtUtil.putTag(t, "Members", members);
            teamsTag.add(t);
        }

        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "Teams", teamsTag);

        CompoundTag colors = new CompoundTag();
        for (Map.Entry<DyeColor, UUID> e : colorTeams.entrySet()) {
            putUuid(colors, e.getKey().getName(), e.getValue());
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "ColorTeams", colors);

        CompoundTag preferred = new CompoundTag();
        for (Map.Entry<UUID, DyeColor> e : preferredTeamColorByPlayer.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            com.jamie.jamiebingo.util.NbtUtil.putInt(preferred, e.getKey().toString(), e.getValue().getId());
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "PreferredTeamColorByPlayer", preferred);

        return tag;
    }

    public static TeamData load(CompoundTag tag) {
        TeamData data = new TeamData();

        ListTag teamsTag = tag.getListOrEmpty("Teams");
        for (int i = 0; i < teamsTag.size(); i++) {
            CompoundTag t = teamsTag.getCompoundOrEmpty(i);
            UUID id = getUuid(t, "Id");
            if (id == null) {
                continue;
            }
            DyeColor color = DyeColor.byId(com.jamie.jamiebingo.util.NbtUtil.getInt(t, "Color", 0));

            TeamInfo info = new TeamInfo(id, color);

            ListTag members = t.getListOrEmpty("Members");
            for (int m = 0; m < members.size(); m++) {
                UUID memberId = getUuid(members.getCompoundOrEmpty(m), "UUID");
                if (memberId != null) {
                    info.members.add(memberId);
                }
            }

            data.teams.put(id, info);

            // Rebuild player → team mapping
            for (UUID u : info.members) {
                data.playerToTeam.put(u, id);
            }
        }

        if (tag.contains("ColorTeams")) {
            CompoundTag colors = tag.getCompoundOrEmpty("ColorTeams");
            for (DyeColor color : DyeColor.values()) {
                UUID id = getUuid(colors, color.getName());
                if (id != null) {
                    data.colorTeams.put(color, id);
                }
            }
        }

        if (tag.contains("PreferredTeamColorByPlayer")) {
            CompoundTag preferred = tag.getCompoundOrEmpty("PreferredTeamColorByPlayer");
            for (String key : preferred.keySet()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    int colorId = com.jamie.jamiebingo.util.NbtUtil.getInt(preferred, key, -1);
                    if (colorId >= 0) {
                        data.preferredTeamColorByPlayer.put(playerId, DyeColor.byId(colorId));
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return data;
    }

    public static TeamData get(MinecraftServer server) {
        ServerLevel overworld = null;
        try {
            overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        } catch (Throwable ignored) {
        }
        if (overworld == null) {
            overworld = ServerLevelUtil.getOverworld(server);
        }
        if (overworld == null) {
            return FALLBACK_BY_SERVER.computeIfAbsent(server, s -> new TeamData());
        }
        var storage = com.jamie.jamiebingo.util.LevelDataStorageUtil.getDataStorage(overworld);
        TeamData data = com.jamie.jamiebingo.util.SavedDataUtil.computeIfAbsent(storage, TYPE);
        if (data != null) return data;
        return FALLBACK_BY_SERVER.computeIfAbsent(server, s -> new TeamData());
    }

    private static UUID getUuid(CompoundTag tag, String key) {
        String raw = com.jamie.jamiebingo.util.NbtUtil.getString(tag, key, null);
        if (raw != null && !raw.isBlank()) {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
            }
        }
        int[] ints = com.jamie.jamiebingo.util.NbtUtil.getIntArray(tag, key);
        if (ints != null && ints.length == 4) {
            return UUIDUtil.uuidFromIntArray(ints);
        }
        return null;
    }

    private static void putUuid(CompoundTag tag, String key, UUID value) {
        if (value == null) return;
        tag.putIntArray(key, UUIDUtil.uuidToIntArray(value));
    }

    /* ============================
       INNER CLASS
       ============================ */

    public static class TeamInfo {
        public final UUID id;
        public final DyeColor color;
        public final Set<UUID> members = new HashSet<>();

        public TeamInfo(UUID id, DyeColor color) {
            this.id = id;
            this.color = color;
        }
    }
}






