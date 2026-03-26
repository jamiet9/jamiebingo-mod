package com.jamie.jamiebingo.quest;

import com.google.gson.*;
import com.jamie.jamiebingo.data.TeamData;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class QuestDatabase {

    private static final List<QuestDefinition> QUESTS = new ArrayList<>();
    private static boolean loaded = false;

    public static void load() {
        if (loaded) return;

        try {
            InputStream stream = QuestDatabase.class.getClassLoader()
                    .getResourceAsStream("data/jamiebingo/bingo_quests.json");
            if (stream == null) {
                stream = QuestDatabase.class.getClassLoader()
                        .getResourceAsStream("jamiebingo/bingo_quests.json");
            }

            if (stream == null) {
                System.out.println("[JamieBingo] WARNING: bingo_quests.json not found — Quest Mode disabled.");
                loaded = true;
                return;
            }

            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();

            JsonArray questsJson = root.getAsJsonArray("quests");
            if (questsJson == null) {
                System.out.println("[JamieBingo] WARNING: bingo_quests.json contains no quests — Quest Mode disabled.");
                loaded = true;
                return;
            }

            int invalid = 0;

            for (JsonElement e : questsJson) {
                try {
                    JsonObject o = e.getAsJsonObject();

                    String id = o.get("id").getAsString();
                    String name = o.get("name").getAsString();
                    String desc = o.get("description").getAsString();
                    String rarity = o.get("rarity").getAsString();
                    String category = normalizeCategory(o.get("category").getAsString());
                    String extra = o.has("extra") ? o.get("extra").getAsString() : "";
                    String texture = o.has("texture") ? o.get("texture").getAsString() : "";

                    if (id == null || id.isBlank() || name == null || name.isBlank()) {
                        invalid++;
                        continue;
                    }
                    if ("quest.break_any_piece_of_armor".equals(id)) {
                        continue;
                    }

                    QuestRequirements req = parseRequirements(extra);
                    QUESTS.add(new QuestDefinition(
                            id,
                            name,
                            desc,
                            rarity,
                            category,
                            extra,
                            texture,
                            req
                    ));

                } catch (Exception ex) {
                    invalid++;
                }
            }

            System.out.println("[JamieBingo] Loaded " + QUESTS.size() + " quests."
                    + (invalid > 0 ? " (" + invalid + " invalid skipped)" : ""));

        } catch (Exception e) {
            System.out.println("[JamieBingo] ERROR loading bingo_quests.json — Quest Mode disabled.");
            e.printStackTrace();
        }

        loaded = true;
    }

    public static boolean hasQuests() {
        return !QUESTS.isEmpty();
    }

    public static List<QuestDefinition> getQuests() {
        return Collections.unmodifiableList(QUESTS);
    }

    public static QuestDefinition getQuestById(String id) {
        if (id == null || id.isBlank()) return null;
        for (QuestDefinition quest : QUESTS) {
            if (quest != null && id.equalsIgnoreCase(quest.id)) {
                return quest;
            }
        }
        return null;
    }

    public static List<QuestDefinition> getEligibleQuests(
            net.minecraft.server.MinecraftServer server,
            com.jamie.jamiebingo.data.BingoGameData data
    ) {
        if (QUESTS.isEmpty()) return Collections.emptyList();
        return QUESTS.stream()
                .filter(q -> q.requirements == null || q.requirements.isEligible(server, data))
                .filter(q -> !isHungerDisabledQuest(q, data))
                .filter(q -> !isPvpDisabledPlayerKillQuest(q, data))
                .filter(q -> !isHostileMobsDisabledQuest(q, data))
                .filter(q -> !isBlindCompeteQuest(q, data))
                .filter(q -> !isHardcoreDeathQuest(q, data))
                .filter(q -> !isOpponentQuestDisallowed(q, data))
                .filter(q -> !isBlocked(q, data))
                .filter(q -> isTeamsQuestAllowed(q, server))
                .filter(q -> !isOpponentQuest(q) || hasMoreThanOneActiveTeam(server))
                .map(q -> withEffectiveRarity(q, data))
                .toList();
    }

    private static boolean hasMoreThanOneActiveTeam(net.minecraft.server.MinecraftServer server) {
        if (server == null) return false;
        TeamData teamData = TeamData.get(server);
        int active = (int) teamData.getTeams().stream().filter(t -> !t.members.isEmpty()).count();
        return active > 1;
    }

    private static boolean isOpponentQuest(QuestDefinition q) {
        if (q == null) return false;
        String haystack = (q.id + " " + q.name + " " + q.description).toLowerCase();
        return haystack.contains("opponent");
    }

    private static boolean isTeamsQuestAllowed(QuestDefinition q, net.minecraft.server.MinecraftServer server) {
        if (q == null) return false;
        if (q.category == null) return true;
        if (!"team".equalsIgnoreCase(q.category) && !"teams".equalsIgnoreCase(q.category)) return true;
        if (server == null) return false;
        TeamData teamData = TeamData.get(server);
        List<TeamData.TeamInfo> activeTeams = teamData.getTeams().stream()
                .filter(t -> !t.members.isEmpty())
                .toList();
        if (activeTeams.isEmpty()) return false;
        for (TeamData.TeamInfo team : activeTeams) {
            if (team.members.size() < 2) return false;
        }
        return true;
    }

    private static boolean isOpponentQuestDisallowed(QuestDefinition q, com.jamie.jamiebingo.data.BingoGameData data) {
        if (q == null || data == null) return false;
        if (!isOpponentQuest(q)) return false;
        if (data.winCondition != com.jamie.jamiebingo.bingo.WinCondition.LOCKOUT
                && data.winCondition != com.jamie.jamiebingo.bingo.WinCondition.RARITY) {
            return true;
        }
        return data.rtpEnabled;
    }

    private static boolean isBlocked(QuestDefinition q, com.jamie.jamiebingo.data.BingoGameData data) {
        if (q == null || data == null) return false;
        return data.isSlotBlocked(q.id);
    }

    private static QuestDefinition withEffectiveRarity(QuestDefinition q, com.jamie.jamiebingo.data.BingoGameData data) {
        if (q == null || data == null) return q;
        String effectiveRarity = data.getEffectiveRarity(q.id, q.rarity);
        if (effectiveRarity == null || effectiveRarity.equalsIgnoreCase(q.rarity)) return q;
        return new QuestDefinition(
                q.id,
                q.name,
                q.description,
                effectiveRarity,
                q.category,
                q.extra,
                q.texture,
                q.requirements
        );
    }

    private static boolean isHungerDisabledQuest(QuestDefinition q, com.jamie.jamiebingo.data.BingoGameData data) {
        if (q == null || data == null) return false;
        if (data.hungerEnabled) return false;
        String id = q.id == null ? "" : q.id.toLowerCase(Locale.ROOT);
        String cat = q.category == null ? "" : q.category.toLowerCase(Locale.ROOT);
        String text = (q.name + " " + q.description + " " + q.extra).toLowerCase(Locale.ROOT);
        if (id.startsWith("quest.eat_")) return true;
        if (id.contains("hunger")) return true;
        if (cat.contains("hunger") || cat.contains("eat") || cat.contains("food")) return true;
        return text.contains(" hunger ") || text.contains("eat ");
    }

    private static boolean isPvpDisabledPlayerKillQuest(QuestDefinition q, com.jamie.jamiebingo.data.BingoGameData data) {
        if (q == null || data == null) return false;
        if (data.pvpEnabled) return false;
        String haystack = (q.id + " " + q.name + " " + q.description + " " + q.extra).toLowerCase(Locale.ROOT);
        if (haystack.contains("kill another player")) return true;
        if (haystack.contains("kill a player")) return true;
        if (haystack.contains("player kill")) return true;
        if (haystack.contains("an opponent dies")) return true;
        return false;
    }

    private static boolean isHostileMobsDisabledQuest(QuestDefinition q, com.jamie.jamiebingo.data.BingoGameData data) {
        if (q == null || data == null) return false;
        if (data.hostileMobsEnabled) return false;

        String id = q.id == null ? "" : q.id.toLowerCase(Locale.ROOT);
        if (id.equals("quest.kill_a_pillager_with_a_crossbow")) return true;
        if (id.equals("quest.breed_hoglin")) return true;
        if (id.equals("quest.kill_all_raid_mobs")) return true;
        if (id.equals("quest.die_to_warden")) return true;
        if (id.equals("quest.start_a_raid")) return true;
        if (id.equals("quest.open_ominous_vault")) return true;
        if (id.equals("quest.open_trial_vault")) return true;

        String haystack = (q.id + " " + q.name + " " + q.description + " " + q.extra + " " + q.category)
                .toLowerCase(Locale.ROOT);
        if (haystack.contains("hostile mobs are enabled")) return true;
        if (haystack.contains("can only appear if hostile mobs are enabled")) return true;
        if (haystack.contains("pillager") || haystack.contains("raid")) return true;
        if (haystack.contains("warden") || haystack.contains("hoglin")) return true;
        if (haystack.contains("vault") || haystack.contains("ominous")) return true;
        return false;
    }

    private static boolean isBlindCompeteQuest(QuestDefinition q, com.jamie.jamiebingo.data.BingoGameData data) {
        if (q == null || data == null) return false;
        if (data.winCondition != com.jamie.jamiebingo.bingo.WinCondition.BLIND) return false;
        return "compete".equalsIgnoreCase(q.category);
    }

    private static boolean isHardcoreDeathQuest(QuestDefinition q, com.jamie.jamiebingo.data.BingoGameData data) {
        if (q == null || data == null) return false;
        if (!data.hardcoreEnabled) return false;
        if ("death".equalsIgnoreCase(q.category)) return true;
        if ("die".equalsIgnoreCase(q.category)) return true;
        if (q.id != null && q.id.startsWith("quest.die_")) return true;
        String haystack = (q.id + " " + q.name + " " + q.description).toLowerCase();
        if (haystack.contains(" death ")) return true;
        if (haystack.contains(" die ") || haystack.startsWith("die ") || haystack.endsWith(" die")) return true;
        if (haystack.contains(" die by") || haystack.contains(" died ")) return true;
        if (haystack.contains("killed by") || haystack.contains("get killed") || haystack.contains("be killed")) return true;
        return haystack.contains("slain by");
    }

    private static QuestRequirements parseRequirements(String extraRaw) {
        if (extraRaw == null || extraRaw.isBlank()) return QuestRequirements.none();

        String extra = extraRaw.toLowerCase();

        boolean hostile = extra.contains("hostile mobs are enabled")
                || extra.contains("hostilemobs are enabled")
                || extra.contains("hostil mobs are enabled");
        boolean hunger = extra.contains("hunger is enabled");
        boolean effectsDisabled = extra.contains("effects are disabled")
                || extra.contains("effects mode is disabled");
        boolean rtpDisabled = extra.contains("rtp is disabled");
        boolean pvpEnabled = extra.contains("pvp is enabled");
        boolean daylightNotDayOnly = extra.contains("daylight cycle setting is not set to day only");

        int minTeams = extra.contains("more than one team") ? 2 : 0;
        int maxTeams = 0;
        int minPlayers = 0;
        int maxPlayers = 0;

        if (extra.contains("two teams and two players")) {
            minTeams = 2;
            maxTeams = 2;
            minPlayers = 2;
            maxPlayers = 2;
        }

        return new QuestRequirements(
                hostile,
                hunger,
                effectsDisabled,
                rtpDisabled,
                pvpEnabled,
                daylightNotDayOnly,
                minTeams,
                maxTeams,
                minPlayers,
                maxPlayers
        );
    }

    private static String normalizeCategory(String category) {
        if (category == null) return null;
        String s = category.trim().toLowerCase(Locale.ROOT);
        if (s.isBlank()) return s;
        return switch (s) {
            case "shove;", "shove" -> "shovel";
            case "restone", "resdstone" -> "redstone";
            case "moster" -> "monster";
            case "unkown" -> "unknown";
            case "die" -> "death";
            default -> s;
        };
    }
}

