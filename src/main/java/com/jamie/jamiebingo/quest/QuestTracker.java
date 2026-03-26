package com.jamie.jamiebingo.quest;

import com.jamie.jamiebingo.data.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class QuestTracker {

    private static final Map<UUID, Set<String>> flags = new HashMap<>();
    private static final Map<UUID, Map<String, Integer>> progress = new HashMap<>();
    private static final Map<UUID, Map<String, Integer>> baselines = new HashMap<>();
    private static final Map<String, Integer> QUEST_PROGRESS_MAX = Map.ofEntries(
            Map.entry("quest.sprint_1km", 1000),
            Map.entry("quest.crouch_for_100m", 100),
            Map.entry("quest.swim_for_500m", 500),
            Map.entry("quest.boat_for_2km", 2000),
            Map.entry("quest.wear_a_carved_pumpkin_for_5_minutes", 6000),
            Map.entry("quest.deal_400_damage", 400),
            Map.entry("quest.take_200_damage", 200),
            Map.entry("quest.kill_100_mobs", 100),
            Map.entry("quest.kill_5_baby_mobs", 5),
            Map.entry("quest.kill_20_baby_mobs", 20),
            Map.entry("quest.kill_20_arthropods", 20),
            Map.entry("quest.kill_30_undead_mobs", 30),
            Map.entry("quest.get_3_status_effects_at_once", 3),
            Map.entry("quest.get_6_status_effects_at_once", 6),
            Map.entry("quest.eat_5_unique_foods", 5),
            Map.entry("quest.eat_10_unique_foods", 10),
            Map.entry("quest.eat_20_unique_foods", 20),
            Map.entry("quest.kill_7_unique_hostile_mobs", 7),
            Map.entry("quest.kill_10_unique_hostile_mobs", 10),
            Map.entry("quest.kill_15_unique_hostile_mobs", 15),
            Map.entry("quest.breed_4_unique_animals", 4),
            Map.entry("quest.breed_6_unique_animals", 6),
            Map.entry("quest.breed_8_unique_animals", 8),
            Map.entry("quest.visit_all_nether_biomes", 5),
            Map.entry("quest.visit_10_unique_biomes", 10),
            Map.entry("quest.visit_15_unique_biomes", 15),
            Map.entry("quest.visit_20_unique_biomes", 20),
            Map.entry("quest.visit_all_cave_biomes", 3),
            Map.entry("quest.get_15_advancements", 15),
            Map.entry("quest.get_25_advancements", 25),
            Map.entry("quest.get_35_advancements", 35),
            Map.entry("quest.reach_level_15", 15),
            Map.entry("quest.reach_level_30", 30),
            Map.entry("quest.apply_3_unique_armor_trims", 3),
            Map.entry("quest.apply_5_unique_armor_trims", 5),
            Map.entry("quest.apply_7_unique_armor_trims", 7),
            Map.entry("quest.obtain_3_unique_banner_patterns", 3),
            Map.entry("quest.obtain_3_unique_music_disks", 3),
            Map.entry("quest.obtain_5_unique_pressure_plates", 5),
            Map.entry("quest.obtain_6_unique_brick_blocks", 6),
            Map.entry("quest.obtain_6_unique_buckets", 6),
            Map.entry("quest.obtain_7_unique_workstations", 7),
            Map.entry("quest.obtain_6_log_variants", 6),
            Map.entry("quest.obtain_8_log_variants", 8),
            Map.entry("quest.obtain_5_types_of_saplings", 5),
            Map.entry("quest.obtain_6_unique_flowers", 6),
            Map.entry("quest.obtain_64_of_any_one_item_block", 64),
            Map.entry("quest.craft_25_unique_items", 25),
            Map.entry("quest.craft_50_unique_items", 50),
            Map.entry("quest.craft_75_unique_items", 75),
            Map.entry("quest.look_at_5_unique_mobs_with_a_spyglass", 5),
            Map.entry("quest.look_at_10_unique_mobs_with_a_spyglass", 10),
            Map.entry("quest.look_at_15_unique_mobs_with_a_spyglass", 15),
            Map.entry("quest.look_at_20_unique_mobs_with_a_spyglass", 20),
            Map.entry("quest.obtain_2_unique_armor_trims", 2),
            Map.entry("quest.take_damage_from_8_unique_sources", 8),
            Map.entry("quest.take_damage_from_12_unique_sources", 12),
            Map.entry("quest.take_damage_from_15_unique_sources", 15),
            Map.entry("quest.attach_a_lead_to_4_unique_entities_at_once", 4),
            Map.entry("quest.attach_a_lead_to_6_unique_entities_at_once", 6),
            Map.entry("quest.attach_a_lead_to_8_unique_entities_at_once", 8),
            Map.entry("quest.fill_a_bundle_with_16_bundles", 16),
            Map.entry("quest.fill_a_campfire_with_4_food_items", 4),
            Map.entry("quest.fill_a_chiseled_bookshelf_with_books", 6),
            Map.entry("quest.fill_a_decorated_pot", 1),
            Map.entry("quest.fill_a_shelf_with_shelves", 3),
            Map.entry("quest.ride_a_happy_ghast_for_200_meters", 20000),
            Map.entry("quest.ride_a_minecart_for_25_meters", 2500),
            Map.entry("quest.grow_20_trees", 20),
            Map.entry("quest.push_a_button_20_times", 20),
            Map.entry("quest.flick_a_lever_20_times", 20),
            Map.entry("quest.use_a_ladder_to_climb_64_meters_in_hieght", 6400),
            Map.entry("quest.have_10_live_tamed_mobs", 10),
            Map.entry("quest.have_a_tamed_wolf_kill_3_unique_mobs", 3),
            Map.entry("quest.have_a_tamed_wolf_kill_5_unique_mobs", 5),
            Map.entry("quest.have_a_tamed_wolf_kill_8_unique_mobs", 8)
    );

    /* =========================
       FLAG API (RESTORED)
       ========================= */

    public static boolean hasFlag(ServerPlayer p, String f) {
        return flags
                .computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), x -> new HashSet<>())
                .contains(f);
    }

    public static void flag(ServerPlayer p, String f) {
        flags
                .computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), x -> new HashSet<>())
                .add(f);
    }

    /* =========================
       LIFECYCLE
       ========================= */

    public static void clear() {
        flags.clear();
        progress.clear();
        baselines.clear();
    }

    public static void resetForGame(List<ServerPlayer> players) {
        clear();
        for (ServerPlayer p : players) {
            flags.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), new HashSet<>());
            progress.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), new HashMap<>());
            baselines.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), new HashMap<>());
        }
    }

    public static void armQuestBaseline(ServerPlayer p, String questId) {
        if (p == null || questId == null || questId.isBlank()) return;
        UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(p);
        int raw = getRawPlayerProgressForQuest(p, questId);
        baselines
                .computeIfAbsent(playerId, x -> new HashMap<>())
                .put(questId, Math.max(0, raw));
        progress.computeIfAbsent(playerId, x -> new HashMap<>()).put(questId, 0);
        flags.computeIfAbsent(playerId, x -> new HashSet<>()).remove(questId);
    }

    /* =========================
       PROGRESS
       ========================= */

    public static int getProgress(ServerPlayer p, String questId) {
        return progress
                .computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), x -> new HashMap<>())
                .getOrDefault(questId, 0);
    }

    public static void setProgress(ServerPlayer p, String questId, int value) {
        progress
                .computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), x -> new HashMap<>())
                .put(questId, value);
    }

    public static void increment(ServerPlayer p, String questId, int max) {
        int current = getProgress(p, questId);
        if (current >= max) return;

        setProgress(p, questId, current + 1);
        maybeBroadcastProgress(p);
        if (current + 1 >= max) complete(p, questId);
    }

    public static void addProgress(ServerPlayer p, String questId, int amount, int max) {
        if (amount <= 0) return;

        int current = getProgress(p, questId);
        if (current >= max) return;

        int next = Math.min(max, current + amount);
        setProgress(p, questId, next);
        maybeBroadcastProgress(p);
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(p);
        if (server != null && BingoGameData.get(server).teamSyncEnabled) {
            TeamData teamData = TeamData.get(server);
            UUID teamId = teamData.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
            if (teamId != null) {
                int teamValue = 0;
                TeamData.TeamInfo team = teamData.getTeams().stream()
                        .filter(t -> t.id.equals(teamId))
                        .findFirst()
                        .orElse(null);
                if (team != null) {
                    for (UUID memberId : team.members) {
                        ServerPlayer member = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
                        if (member == null) continue;
                        teamValue += getProgress(member, questId);
                    }
                } else {
                    teamValue = next;
                }
                if (teamValue >= max) {
                    complete(p, questId);
                    return;
                }
            }
        }
        if (next >= max) complete(p, questId);
    }

    /* =========================
       COMPLETION
       ========================= */

    public static void complete(ServerPlayer p, String questId) {
        completeWithResult(p, questId);
    }

    public static boolean completeWithResult(ServerPlayer p, String questId) {
        if (!BingoGameData.isQuestModeRunning()) return false;

        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(p);
        com.jamie.jamiebingo.mines.MineModeManager.onGoalCompleted(server, questId, p);
        com.jamie.jamiebingo.power.PowerSlotManager.onGoalCompleted(server, questId, p);
        boolean newlyCompleted = BingoGameData.completeQuest(server, questId, com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        if (!newlyCompleted) return false;

        TeamData teamData = TeamData.get(server);
        UUID teamId = teamData.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));

        if (teamId != null) {
            TeamData.TeamInfo team = teamData.getTeams().stream()
                    .filter(t -> t.id.equals(teamId))
                    .findFirst()
                    .orElse(null);

            if (team != null) {
                QuestDatabase.load();
                String questName = questId;
                var def = QuestDatabase.getQuestById(questId);
                if (def != null && def.name != null && !def.name.isBlank()) {
                    questName = def.name;
                }
                questName = sanitizeQuestName(questName);
                if (questName.isBlank()) {
                    questName = questId;
                }

                Component slotComp = BroadcastHelper.buildSlotChatComponent(
                        server,
                        teamId,
                        questId,
                        questName,
                        com.jamie.jamiebingo.util.ItemStackUtil.empty()
                );
                String teamLabel = team.color == null ? "Team" : "Team " + team.color.getName();
                String playerName = p.getGameProfile().name();
                Component prefix = com.jamie.jamiebingo.util.ComponentUtil.literal("[" + teamLabel + "] " + playerName)
                        .withStyle(BroadcastHelper.teamChatFormatting(team.color));

                if (BingoGameData.get(server).hideGoalDetailsInChat) {
                    BroadcastHelper.broadcast(
                            server,
                            prefix.copy().append(com.jamie.jamiebingo.util.ComponentUtil.literal(" completed a goal."))
                    );
                } else {
                    BroadcastHelper.broadcastCompletionWithIcon(
                            server,
                            prefix.copy()
                                    .append(com.jamie.jamiebingo.util.ComponentUtil.literal(" completed "))
                                    .append(slotComp),
                            questId
                    );
                }
            }
        }

        BroadcastHelper.broadcastProgress(server);
        BroadcastHelper.broadcastTeamScores(server);
        return true;
    }

    public static Map<String, Integer> getClientProgress(ServerPlayer p) {
        return progress.getOrDefault(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), Collections.emptyMap());
    }

    public static Integer getQuestMax(String questId) {
        if (questId != null && questId.startsWith("quest.obtain_64_")) return 64;
        return QUEST_PROGRESS_MAX.get(questId);
    }

    public static int getTeamProgress(MinecraftServer server, UUID teamId, String questId) {
        if (server == null || teamId == null || questId == null) return 0;

        BingoGameData data = BingoGameData.get(server);
        if (data.teamSyncEnabled) {
            Integer teamValue = QuestEvents.getTeamProgressValue(server, teamId, questId);
            if (teamValue != null) {
                Integer max = getQuestMax(questId);
                return max == null ? teamValue : Math.min(max, teamValue);
            }
        }

        TeamData teamData = TeamData.get(server);
        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);
        if (team == null) return 0;

        int best = 0;
        for (UUID memberId : team.members) {
            ServerPlayer member = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
            if (member == null) continue;
            best = Math.max(best, getPlayerProgressForQuest(member, questId));
        }

        Integer max = getQuestMax(questId);
        if (max != null) {
            return Math.min(max, best);
        }
        return best;
    }

    public static int getPlayerProgressForQuest(ServerPlayer p, String questId) {
        if (p == null || questId == null) return 0;
        UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(p);
        int raw = getRawPlayerProgressForQuest(p, questId);
        int baseline = baselines
                .computeIfAbsent(playerId, x -> new HashMap<>())
                .getOrDefault(questId, 0);
        return Math.max(0, raw - baseline);
    }

    private static int getRawPlayerProgressForQuest(ServerPlayer p, String questId) {
        if (p == null || questId == null) return 0;
        if (questId.equals("quest.get_3_status_effects_at_once")
                || questId.equals("quest.get_6_status_effects_at_once")) {
            return p.getActiveEffects().size();
        }
        if (questId.equals("quest.eat_5_unique_foods")
                || questId.equals("quest.eat_10_unique_foods")
                || questId.equals("quest.eat_20_unique_foods")) {
            return QuestEvents.getUniqueFoodCount(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }
        if (questId.equals("quest.kill_7_unique_hostile_mobs")
                || questId.equals("quest.kill_10_unique_hostile_mobs")
                || questId.equals("quest.kill_15_unique_hostile_mobs")) {
            return QuestEvents.getUniqueHostileKillCount(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }
        if (questId.equals("quest.breed_4_unique_animals")
                || questId.equals("quest.breed_6_unique_animals")
                || questId.equals("quest.breed_8_unique_animals")) {
            return QuestEvents.getUniqueBreedCount(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }
        if (questId.equals("quest.visit_all_nether_biomes")) {
            return QuestEvents.getNetherBiomeCount(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }
        if (questId.equals("quest.get_15_advancements")
                || questId.equals("quest.get_25_advancements")
                || questId.equals("quest.get_35_advancements")) {
            return QuestEvents.getAdvancementsEarned(p);
        }
        if (questId.equals("quest.crouch_for_100m")) {
            return QuestEvents.getCrouchDistance(com.jamie.jamiebingo.util.EntityUtil.getUUID(p)) / 100;
        }
        if (questId.equals("quest.sprint_1km")) {
            return getProgress(p, questId) / 100;
        }
        if (questId.equals("quest.swim_for_500m")) {
            return QuestEvents.getSwimDistance(com.jamie.jamiebingo.util.EntityUtil.getUUID(p)) / 100;
        }
        if (questId.equals("quest.boat_for_2km")) {
            return QuestEvents.getBoatDistance(com.jamie.jamiebingo.util.EntityUtil.getUUID(p)) / 100;
        }
        if (questId.equals("quest.kill_5_baby_mobs") || questId.equals("quest.kill_20_baby_mobs")) {
            return QuestEvents.getBabyMobKillCount(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }
        if (questId.equals("quest.visit_10_unique_biomes")
                || questId.equals("quest.visit_15_unique_biomes")
                || questId.equals("quest.visit_20_unique_biomes")) {
            return QuestEvents.getUniqueBiomeCount(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }
        if (questId.equals("quest.visit_all_cave_biomes")) {
            return QuestEvents.getUniqueCaveBiomeCount(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }
        if (questId.equals("quest.apply_3_unique_armor_trims")
                || questId.equals("quest.apply_5_unique_armor_trims")
                || questId.equals("quest.apply_7_unique_armor_trims")) {
            return QuestEvents.getUniqueArmorTrimCount(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }
        if (questId.equals("quest.craft_25_unique_items")
                || questId.equals("quest.craft_50_unique_items")
                || questId.equals("quest.craft_75_unique_items")) {
            return QuestEvents.getUniqueCraftCount(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }
        if (questId.equals("quest.look_at_5_unique_mobs_with_a_spyglass")
                || questId.equals("quest.look_at_10_unique_mobs_with_a_spyglass")
                || questId.equals("quest.look_at_15_unique_mobs_with_a_spyglass")
                || questId.equals("quest.look_at_20_unique_mobs_with_a_spyglass")) {
            return QuestEvents.getUniqueSpyglassLookCount(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }
        if (questId.equals("quest.obtain_2_unique_armor_trims")) {
            return QuestEvents.getUniqueArmorTrimTemplateCount(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }
        if (questId.equals("quest.obtain_3_unique_banner_patterns")) {
            return QuestEvents.getUniqueBannerPatternCount(p);
        }
        if (questId.equals("quest.obtain_3_unique_music_disks")) {
            return QuestEvents.getUniqueMusicDiscCount(p);
        }
        if (questId.equals("quest.obtain_5_unique_pressure_plates")) {
            return QuestEvents.getUniquePressurePlateCount(p);
        }
        if (questId.equals("quest.obtain_6_unique_brick_blocks")) {
            return QuestEvents.getUniqueBrickBlockCount(p);
        }
        if (questId.equals("quest.obtain_6_unique_buckets")) {
            return QuestEvents.getUniqueBucketCount(p);
        }
        if (questId.equals("quest.obtain_7_unique_workstations")) {
            return QuestEvents.getUniqueWorkstationCount(p);
        }
        if (questId.equals("quest.obtain_6_log_variants") || questId.equals("quest.obtain_8_log_variants")) {
            return QuestEvents.getUniqueLogCount(p);
        }
        if (questId.equals("quest.take_damage_from_8_unique_sources")
                || questId.equals("quest.take_damage_from_12_unique_sources")
                || questId.equals("quest.take_damage_from_15_unique_sources")) {
            return QuestEvents.getUniqueDamageSourceCount(com.jamie.jamiebingo.util.EntityUtil.getUUID(p));
        }
        if (questId.equals("quest.obtain_5_types_of_saplings")) {
            return QuestEvents.getUniqueSaplingCount(p);
        }
        if (questId.equals("quest.obtain_6_unique_flowers")) {
            return QuestEvents.getUniqueFlowerCount(p);
        }
        if (questId.equals("quest.attach_a_lead_to_4_unique_entities_at_once")
                || questId.equals("quest.attach_a_lead_to_6_unique_entities_at_once")
                || questId.equals("quest.attach_a_lead_to_8_unique_entities_at_once")) {
            return QuestEvents.getUniqueLeashedTypeCount(p);
        }
        if (questId.equals("quest.reach_level_15") || questId.equals("quest.reach_level_30")) {
            return p.experienceLevel;
        }
        if (questId.equals("quest.fill_a_bundle_with_16_bundles")) {
            return QuestEvents.getMaxBundlesInside(p);
        }
        if (questId.equals("quest.fill_a_campfire_with_4_food_items")
                || questId.equals("quest.fill_a_chiseled_bookshelf_with_books")
                || questId.equals("quest.fill_a_decorated_pot")
                || questId.equals("quest.fill_a_shelf_with_shelves")) {
            return getProgress(p, questId);
        }
        if (questId.startsWith("quest.obtain_64_")) {
            if (questId.equals("quest.obtain_64_of_any_one_item_block")) {
                return QuestEvents.getMaxStackCount(p);
            }
            String raw = questId.substring("quest.obtain_64_".length());
            String itemId;
            if (raw.endsWith("_glass")) {
                itemId = "minecraft:" + raw.replace("_glass", "_stained_glass");
            } else if (raw.equals("arrows")) {
                itemId = "minecraft:arrow";
            } else if (raw.equals("firefly_bushes")) {
                itemId = "minecraft:firefly_bush";
            } else {
                itemId = "minecraft:" + raw;
            }
            net.minecraft.world.item.Item item = com.jamie.jamiebingo.util.ItemLookupUtil.item(itemId);
            return QuestEvents.getItemCount(p, item);
        }

        return getProgress(p, questId);
    }

    public static boolean isQuestComplete(ServerPlayer p, String questId) {
        if (p == null || questId == null) return false;
        Integer max = getQuestMax(questId);
        if (max != null) {
            return getPlayerProgressForQuest(p, questId) >= max;
        }
        return hasFlag(p, questId);
    }

    private static void maybeBroadcastProgress(ServerPlayer p) {
        if (p == null) return;
        if (com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(p) % 20 != 0) return;
        BroadcastHelper.broadcastProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(p));
    }

    private static String sanitizeQuestName(String raw) {
        if (raw == null) return "";
        String cleaned = raw;
        if (cleaned.startsWith("quest.")) {
            cleaned = cleaned.substring("quest.".length());
        }
        cleaned = cleaned.replace('_', ' ').replace('-', ' ');
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) return raw;
        return cleaned;
    }
}






