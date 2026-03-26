package com.jamie.jamiebingo.mines;

import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.data.BroadcastHelper;
import com.jamie.jamiebingo.data.TeamData;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.PacketPlayTeamSound;
import com.jamie.jamiebingo.network.PacketSyncMineState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

public final class MineModeManager {

    public enum Trigger {
        CATCH_FIRE,
        DIE,
        JUMP,
        OBTAIN_CRAFTING_TABLE,
        OBTAIN_OBSIDIAN,
        OBTAIN_WHEAT_SEEDS,
        TAKE_100_DAMAGE,
        TAKE_FALL_DAMAGE,
        TOUCH_WATER,
        OBTAIN_ADVANCEMENT,
        STAND_ON_NETHERRACK,
        STAND_ON_STONE,
        WEAR_ARMOR
    }

    public record MineDef(Trigger trigger, String sourceQuestId, String displayName) {}

    private static final List<MineDef> DEFINITIONS = List.of(
            new MineDef(Trigger.CATCH_FIRE, "quest.an_opponent_catches_on_fire", "Don't catch on fire"),
            new MineDef(Trigger.DIE, "quest.an_opponent_dies", "Don't die"),
            new MineDef(Trigger.JUMP, "quest.an_opponent_jumps", "Don't jump"),
            new MineDef(Trigger.OBTAIN_CRAFTING_TABLE, "quest.an_opponent_obtains_a_crafting_table", "Don't obtain a crafting table"),
            new MineDef(Trigger.OBTAIN_OBSIDIAN, "quest.an_opponent_obtains_obsidian", "Don't obtain obsidian"),
            new MineDef(Trigger.OBTAIN_WHEAT_SEEDS, "quest.an_opponent_obtains_wheat_seeds", "Don't obtain a wheat seed"),
            new MineDef(Trigger.TAKE_100_DAMAGE, "quest.an_opponent_takes_100_total_damage", "Don't take 100 total damage"),
            new MineDef(Trigger.TAKE_FALL_DAMAGE, "quest.an_opponent_takes_fall_damage", "Don't take fall damage"),
            new MineDef(Trigger.TOUCH_WATER, "quest.an_opponent_touches_water", "Don't touch water"),
            new MineDef(Trigger.OBTAIN_ADVANCEMENT, "quest.opponent_obtains_advancement", "Don't obtain an advancement"),
            new MineDef(Trigger.STAND_ON_NETHERRACK, "quest.opponent_stands_on_netherrack", "Don't stand on netherrack"),
            new MineDef(Trigger.STAND_ON_STONE, "quest.opponent_stands_on_stone", "Don't stand on stone"),
            new MineDef(Trigger.WEAR_ARMOR, "quest.opponent_wears_armor", "Don't wear armor")
    );

    private static boolean active = false;
    private static final List<MineDef> activeMines = new ArrayList<>();
    private static final Map<Trigger, MineDef> activeByTrigger = new EnumMap<>(Trigger.class);
    private static final Map<UUID, Integer> damageProgressByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> deadlineTickByPlayer = new HashMap<>();
    private static final Map<UUID, String> triggeredMineIdByPlayer = new HashMap<>();
    private static final Map<UUID, Integer> lastWarningSecondsByPlayer = new HashMap<>();
    private static String defuseQuestId = "";
    private static String defuseDisplayName = "";
    private static int lastSyncTick = -100;

    private MineModeManager() {
    }

    public static List<String> sourceQuestIdsForAmount(int amount) {
        int count = Math.max(1, Math.min(DEFINITIONS.size(), amount));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            out.add(DEFINITIONS.get(i).sourceQuestId());
        }
        return out;
    }

    public static List<MineDef> definitionsForAmount(int amount) {
        int count = Math.max(1, Math.min(DEFINITIONS.size(), amount));
        return new ArrayList<>(DEFINITIONS.subList(0, count));
    }

    public static List<MineDef> definitionsForIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        Set<String> wanted = new LinkedHashSet<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                wanted.add(id);
            }
        }
        if (wanted.isEmpty()) return List.of();
        List<MineDef> out = new ArrayList<>();
        for (MineDef def : DEFINITIONS) {
            if (wanted.contains(def.sourceQuestId())) {
                out.add(def);
            }
        }
        return out;
    }

    public static List<String> selectedSourceQuestIds(BingoGameData data, Random rng) {
        List<String> out = new ArrayList<>();
        for (MineDef def : selectedMineDefinitions(data, rng)) {
            if (def == null || def.sourceQuestId() == null || def.sourceQuestId().isBlank()) continue;
            out.add(def.sourceQuestId());
        }
        return out;
    }

    public static void start(MinecraftServer server, BingoGameData data) {
        if (server == null || data == null || !data.isActive() || !data.minesEnabled) return;
        if (isActive()) {
            persistResumeState(server);
            syncAll(server);
            return;
        }

        clear(server);

        List<MineDef> candidates = definitionsForIds(data.getMineResumeSourceIds());
        if (candidates.isEmpty()) {
            candidates = selectedMineDefinitions(data, new java.util.Random());
        }
        if (candidates.isEmpty()) return;

        activeMines.clear();
        activeMines.addAll(candidates);
        activeByTrigger.clear();
        for (MineDef mine : candidates) {
            activeByTrigger.put(mine.trigger(), mine);
        }

        active = true;
        lastSyncTick = -100;
        lastWarningSecondsByPlayer.clear();
        defuseQuestId = "";
        defuseDisplayName = "";

        for (ServerPlayer p : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            damageProgressByPlayer.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), 0);
        }

        BroadcastHelper.broadcast(server,
                com.jamie.jamiebingo.util.ComponentUtil.literal("[Mines] Active mines: " + activeMines.size()));
        persistResumeState(server);
        syncAll(server);
    }

    public static void clear(MinecraftServer server) {
        active = false;
        activeMines.clear();
        activeByTrigger.clear();
        damageProgressByPlayer.clear();
        deadlineTickByPlayer.clear();
        triggeredMineIdByPlayer.clear();
        lastWarningSecondsByPlayer.clear();
        defuseQuestId = "";
        defuseDisplayName = "";
        lastSyncTick = -100;
        if (server != null) {
            BingoGameData.get(server).clearMineResumeState();
        }
        if (server != null) {
            NetworkHandler.send(
                    new PacketSyncMineState(false, List.of(), List.of(), "", -1, "", 0, 0, "", ""),
                    PacketDistributor.ALL.noArg()
            );
        }
    }

    public static boolean isActive() {
        return active && !activeMines.isEmpty();
    }

    public static String selectedMineName() {
        if (activeMines.isEmpty()) return "";
        return activeMines.stream().map(MineDef::displayName).reduce((a, b) -> a + ", " + b).orElse("");
    }

    public static void onPlayerTrigger(MinecraftServer server, ServerPlayer player, Trigger trigger) {
        onPlayerTrigger(server, player, trigger, 0);
    }

    public static void onPlayerTrigger(MinecraftServer server, ServerPlayer player, Trigger trigger, int amount) {
        if (!isActive() || server == null || player == null) return;
        MineDef hitMine = activeByTrigger.get(trigger);
        if (hitMine == null) return;

        UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
        List<UUID> teamMemberIds = getTeamMemberIds(server, playerId);
        if (teamMemberIds.isEmpty()) {
            teamMemberIds = List.of(playerId);
        }
        BingoGameData data = BingoGameData.get(server);
        if (!data.isActive()) return;
        if (data.pendingWinEndActive || data.stopGamePending) return;
        if (data.isPlayerEliminated(playerId)) return;
        if (teamHasActiveCountdown(teamMemberIds)) return;

        if (trigger == Trigger.TAKE_100_DAMAGE) {
            int next = Math.max(0, damageProgressByPlayer.getOrDefault(playerId, 0) + Math.max(0, amount));
            damageProgressByPlayer.put(playerId, next);
            if (next < 100) {
                persistResumeState(server);
                syncPlayer(server, player);
                return;
            }
        }

        int now = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        int deadline = now + Math.max(1, data.mineTimeSeconds) * 20;
        for (UUID memberId : teamMemberIds) {
            deadlineTickByPlayer.put(memberId, deadline);
            triggeredMineIdByPlayer.put(memberId, hitMine.sourceQuestId());
            lastWarningSecondsByPlayer.remove(memberId);
        }
        ensureDefuseForCurrentGame(server, data);

        BroadcastHelper.broadcast(server, buildTeamActionMessage(
                server,
                player,
                " activated " + hitMine.displayName() + " mine"
        ));
        persistResumeState(server);
        syncAll(server);
    }

    private static List<MineDef> selectedMineDefinitions(BingoGameData data, Random rng) {
        if (data == null) return List.of();
        List<MineDef> candidates = (data.customCardEnabled || data.customPoolEnabled)
                ? definitionsForIds(data.customMineIds)
                : List.of();
        if (candidates.isEmpty()) {
            candidates = new ArrayList<>(DEFINITIONS);
        } else {
            candidates = new ArrayList<>(candidates);
        }
        candidates.removeIf(def -> !isAllowedForStarterKit(def, data));
        if (candidates.isEmpty()) return List.of();
        java.util.Collections.shuffle(candidates, rng == null ? new java.util.Random() : rng);
        int targetCount = Math.max(1, Math.min(candidates.size(), data.mineAmount));
        return new ArrayList<>(candidates.subList(0, targetCount));
    }

    public static void onServerTick(MinecraftServer server) {
        if (!isActive() || server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (!data.isActive()) return;
        if (data.pendingWinEndActive || data.stopGamePending) return;

        int now = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        List<UUID> explode = new ArrayList<>();

        for (Map.Entry<UUID, Integer> entry : deadlineTickByPlayer.entrySet()) {
            UUID playerId = entry.getKey();
            int seconds = Math.max(0, (entry.getValue() - now + 19) / 20);
            int prev = lastWarningSecondsByPlayer.getOrDefault(playerId, -1);
            if (seconds <= 10 && seconds > 0 && seconds != prev) {
                ServerPlayer player = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, playerId);
                if (player != null) {
                    NetworkHandler.send(
                            new PacketPlayTeamSound("minecraft:block.note_block.bell", 1.0f, 0.9f),
                            PacketDistributor.PLAYER.with(player)
                    );
                    player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0f, 0.8f);
                }
            }
            lastWarningSecondsByPlayer.put(playerId, seconds);
            if (now >= entry.getValue()) {
                explode.add(playerId);
            }
        }

        for (UUID playerId : explode) {
            eliminatePlayer(server, playerId);
            deadlineTickByPlayer.remove(playerId);
            triggeredMineIdByPlayer.remove(playerId);
            lastWarningSecondsByPlayer.remove(playerId);
        }
        if (deadlineTickByPlayer.isEmpty()) {
            clearDefuse();
        }

        if (now - lastSyncTick >= 20) {
            lastSyncTick = now;
            syncAll(server);
        }
        persistResumeState(server);
    }

    public static void syncPlayerJoin(ServerPlayer player) {
        if (player == null) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;
        if (!isActive()) {
            NetworkHandler.send(new PacketSyncMineState(false, List.of(), List.of(), "", -1, "", 0, 0, "", ""), PacketDistributor.PLAYER.with(player));
            return;
        }
        syncPlayer(server, player);
    }

    private static void eliminatePlayer(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isActive()) return;
        if (data.isPlayerEliminated(playerId)) return;

        ServerPlayer player = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, playerId);
        if (player != null) {
            PrimedTnt tnt = new PrimedTnt(player.level(), player.getX(), player.getY(), player.getZ(), player);
            tnt.setFuse(0);
            player.level().addFreshEntity(tnt);
        }

        data.eliminatePlayerForMines(server, playerId);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
    }

    private static void syncAll(MinecraftServer server) {
        if (!isActive()) {
            NetworkHandler.send(new PacketSyncMineState(false, List.of(), List.of(), "", -1, "", 0, 0, "", ""), PacketDistributor.ALL.noArg());
            return;
        }

        for (ServerPlayer p : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            syncPlayer(server, p);
        }
    }
    public static void endGameHoldDisplay(MinecraftServer server) {
        if (server == null) return;
        if (activeMines.isEmpty()) {
            clear(server);
            return;
        }
        active = true;
        damageProgressByPlayer.clear();
        deadlineTickByPlayer.clear();
        triggeredMineIdByPlayer.clear();
        lastWarningSecondsByPlayer.clear();
        defuseQuestId = "";
        defuseDisplayName = "";
        lastSyncTick = -100;
        for (ServerPlayer p : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            damageProgressByPlayer.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(p), 0);
        }
        syncAll(server);
    }
    private static void syncPlayer(MinecraftServer server, ServerPlayer player) {
        if (server == null || player == null) return;
        if (!isActive()) {
            NetworkHandler.send(new PacketSyncMineState(false, List.of(), List.of(), "", -1, "", 0, 0, "", ""), PacketDistributor.PLAYER.with(player));
            return;
        }

        UUID id = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
        int now = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        Integer deadline = deadlineTickByPlayer.get(id);
        int remainingSeconds = deadline != null ? Math.max(0, (deadline - now + 19) / 20) : -1;

        String triggeredMineId = triggeredMineIdByPlayer.getOrDefault(id, "");
        MineDef damageMine = activeByTrigger.get(Trigger.TAKE_100_DAMAGE);
        String progressMineId = damageMine == null ? "" : damageMine.sourceQuestId();
        int progress = damageMine == null ? 0 : damageProgressByPlayer.getOrDefault(id, 0);
        int max = damageMine == null ? 0 : 100;

        List<String> questIds = new ArrayList<>();
        List<String> displayNames = new ArrayList<>();
        for (MineDef mine : activeMines) {
            questIds.add(mine.sourceQuestId());
            displayNames.add(mine.displayName());
        }

        NetworkHandler.send(
                new PacketSyncMineState(
                        true,
                        questIds,
                        displayNames,
                        triggeredMineId,
                        remainingSeconds,
                        progressMineId,
                        progress,
                        max,
                        defuseQuestId,
                        defuseDisplayName
                ),
                PacketDistributor.PLAYER.with(player)
        );
    }
    public static void onGoalCompleted(MinecraftServer server, String slotId) {
        onGoalCompleted(server, slotId, null);
    }

    public static void onGoalCompleted(MinecraftServer server, String slotId, ServerPlayer completedBy) {
        if (!isActive() || server == null || slotId == null || slotId.isBlank()) return;
        if (defuseQuestId.isBlank() || !defuseQuestId.equals(slotId)) return;
        if (deadlineTickByPlayer.isEmpty()) {
            clearDefuse();
            persistResumeState(server);
            return;
        }
        removeItemTriggeredMineItemsForTeam(server, completedBy);
        if (completedBy != null) {
            UUID defuserId = com.jamie.jamiebingo.util.EntityUtil.getUUID(completedBy);
            List<UUID> teamMembers = getTeamMemberIds(server, defuserId);
            if (teamMembers.isEmpty()) {
                teamMembers = List.of(defuserId);
            }
            for (UUID memberId : teamMembers) {
                deadlineTickByPlayer.remove(memberId);
                triggeredMineIdByPlayer.remove(memberId);
                lastWarningSecondsByPlayer.remove(memberId);
            }
        } else {
            deadlineTickByPlayer.clear();
            triggeredMineIdByPlayer.clear();
            lastWarningSecondsByPlayer.clear();
        }
        if (completedBy != null) {
            BroadcastHelper.broadcast(server, buildTeamActionMessage(
                    server,
                    completedBy,
                    " successfully defused their teams mines"
            ));
        } else {
            BroadcastHelper.broadcast(server,
                    com.jamie.jamiebingo.util.ComponentUtil.literal("[Mines] Defuse completed. All active mine countdowns were cleared."));
        }
        clearDefuse();
        persistResumeState(server);
        syncAll(server);
    }

    public static boolean isDefuseGoalId(String slotId) {
        if (slotId == null || slotId.isBlank()) return false;
        return !defuseQuestId.isBlank() && defuseQuestId.equals(slotId);
    }

    public static boolean rerollMineByIndex(MinecraftServer server, BingoGameData data, int mineIndex) {
        if (server == null || data == null || !isActive()) return false;
        if (mineIndex < 0 || mineIndex >= activeMines.size()) return false;
        MineDef oldMine = activeMines.get(mineIndex);
        if (oldMine == null) return false;

        Set<String> usedSourceIds = new HashSet<>();
        for (MineDef mine : activeMines) {
            if (mine == null || mine.sourceQuestId() == null || mine.sourceQuestId().isBlank()) continue;
            usedSourceIds.add(mine.sourceQuestId());
        }
        usedSourceIds.remove(oldMine.sourceQuestId());

        List<MineDef> candidates = new ArrayList<>();
        for (MineDef def : DEFINITIONS) {
            if (def == null || def.sourceQuestId() == null || def.sourceQuestId().isBlank()) continue;
            if (usedSourceIds.contains(def.sourceQuestId())) continue;
            if (!isAllowedForStarterKit(def, data)) continue;
            candidates.add(def);
        }
        if (candidates.isEmpty()) return false;

        java.util.Collections.shuffle(candidates, new java.util.Random());
        MineDef replacement = candidates.get(0);
        activeMines.set(mineIndex, replacement);
        activeByTrigger.remove(oldMine.trigger());
        activeByTrigger.put(replacement.trigger(), replacement);

        persistResumeState(server);
        syncAll(server);
        return true;
    }

    public static boolean rerollDefuseGoal(MinecraftServer server, BingoGameData data) {
        if (server == null || data == null || !isActive()) return false;
        if (defuseQuestId == null || defuseQuestId.isBlank()) return false;
        var card = data.getCurrentCard();
        if (card == null) return false;

        Set<String> excluded = new HashSet<>();
        int size = card.getSize();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                var slot = card.getSlot(x, y);
                if (slot == null || slot.getId() == null || slot.getId().isBlank()) continue;
                excluded.add(slot.getId());
            }
        }
        for (MineDef mine : activeMines) {
            if (mine == null || mine.sourceQuestId() == null || mine.sourceQuestId().isBlank()) continue;
            excluded.add(mine.sourceQuestId());
        }
        excluded.add(defuseQuestId);

        var chosen = data.generateRandomGoalNotOnCurrentCard(server, new Random(), excluded);
        if (chosen == null || chosen.getId() == null || chosen.getId().isBlank()) return false;

        defuseQuestId = chosen.getId();
        defuseDisplayName = chosen.getName() != null && !chosen.getName().isBlank()
                ? chosen.getName()
                : chosen.getId();
        if (defuseDisplayName == null || defuseDisplayName.isBlank()) {
            defuseDisplayName = defuseQuestId;
        }

        persistResumeState(server);
        syncAll(server);
        return true;
    }

    private static void ensureDefuseForCurrentGame(MinecraftServer server, BingoGameData data) {
        if (server == null || data == null) return;
        if (!defuseQuestId.isBlank()) return;
        var card = data.getCurrentCard();
        if (card == null) return;
        Set<String> excluded = new HashSet<>();
        int size = card.getSize();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                var slot = card.getSlot(x, y);
                if (slot == null || slot.getId() == null || slot.getId().isBlank()) continue;
                excluded.add(slot.getId());
            }
        }
        var chosen = data.generateRandomGoalNotOnCurrentCard(server, new Random(), excluded);
        if (chosen == null || chosen.getId() == null || chosen.getId().isBlank()) return;
        defuseQuestId = chosen.getId();
        defuseDisplayName = chosen.getName() != null && !chosen.getName().isBlank()
                ? chosen.getName()
                : chosen.getId();
        if (defuseDisplayName.isBlank()) {
            defuseDisplayName = defuseQuestId;
        }
        BroadcastHelper.broadcast(server, com.jamie.jamiebingo.util.ComponentUtil.literal("[Mines] Defuse goal active."));
        persistResumeState(server);
    }

    private static boolean isAllowedForStarterKit(MineDef def, BingoGameData data) {
        if (def == null || data == null) return true;
        int mode = Math.max(BingoGameData.STARTER_KIT_DISABLED, Math.min(BingoGameData.STARTER_KIT_OP, data.starterKitMode));
        if (mode <= BingoGameData.STARTER_KIT_DISABLED) return true;
        if (def.trigger() == Trigger.OBTAIN_ADVANCEMENT) return false;
        if (mode >= BingoGameData.STARTER_KIT_AVERAGE && def.trigger() == Trigger.OBTAIN_CRAFTING_TABLE) return false;
        if (mode >= BingoGameData.STARTER_KIT_AVERAGE && def.trigger() == Trigger.WEAR_ARMOR) return false;
        return true;
    }
    private static Component buildTeamActionMessage(MinecraftServer server, ServerPlayer player, String actionText) {
        if (server == null || player == null) {
            return com.jamie.jamiebingo.util.ComponentUtil.literal(actionText == null ? "" : actionText);
        }
        TeamData teamData = TeamData.get(server);
        UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
        UUID teamId = teamData.getTeamForPlayer(playerId);
        TeamData.TeamInfo info = teamId == null ? null : teamData.getTeams().stream()
                .filter(t -> t != null && teamId.equals(t.id))
                .findFirst()
                .orElse(null);
        net.minecraft.world.item.DyeColor teamColor = info != null ? info.color : null;
        ChatFormatting fmt = BroadcastHelper.teamChatFormatting(teamColor);
        String teamName = teamColor == null ? "Team" : "Team " + teamColor.getName();
        String playerName = player.getGameProfile().name();
        MutableComponent base = com.jamie.jamiebingo.util.ComponentUtil.literal(
                "[" + teamName + "] " + playerName + (actionText == null ? "" : actionText)
        );
        return base.withStyle(fmt);
    }

    private static void removeItemTriggeredMineItemsForTeam(MinecraftServer server, ServerPlayer completedBy) {
        if (server == null || completedBy == null) return;
        TeamData teamData = TeamData.get(server);
        UUID defuserId = com.jamie.jamiebingo.util.EntityUtil.getUUID(completedBy);
        UUID teamId = teamData.getTeamForPlayer(defuserId);
        if (teamId == null) return;
        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t != null && teamId.equals(t.id))
                .findFirst()
                .orElse(null);
        if (team == null || team.members.isEmpty()) return;

        Set<Item> itemsToRemove = new HashSet<>();
        for (UUID memberId : team.members) {
            String sourceQuestId = triggeredMineIdByPlayer.get(memberId);
            Item item = triggerItemForMineSourceQuest(sourceQuestId);
            if (item != null) {
                itemsToRemove.add(item);
            }
        }
        if (itemsToRemove.isEmpty()) return;

        for (UUID memberId : team.members) {
            ServerPlayer member = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
            if (member == null) continue;
            removeItemsFromInventory(member, itemsToRemove);
        }
    }

    private static Item triggerItemForMineSourceQuest(String sourceQuestId) {
        if (sourceQuestId == null || sourceQuestId.isBlank()) return null;
        return switch (sourceQuestId) {
            case "quest.an_opponent_obtains_wheat_seeds" ->
                    com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:wheat_seeds");
            case "quest.an_opponent_obtains_obsidian" ->
                    com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:obsidian");
            case "quest.an_opponent_obtains_a_crafting_table" ->
                    com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:crafting_table");
            default -> null;
        };
    }

    private static void removeItemsFromInventory(ServerPlayer player, Set<Item> items) {
        if (player == null || items == null || items.isEmpty()) return;
        var inv = com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player);
        if (inv == null) return;
        int size = inv.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null || stack.isEmpty()) continue;
            if (!items.contains(stack.getItem())) continue;
            inv.setItem(i, ItemStack.EMPTY);
        }
        if (player.containerMenu != null) {
            player.containerMenu.broadcastChanges();
        }
    }
    private static List<UUID> getTeamMemberIds(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return List.of();
        TeamData teamData = TeamData.get(server);
        UUID teamId = teamData.getTeamForPlayer(playerId);
        if (teamId == null) return List.of(playerId);
        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t != null && teamId.equals(t.id))
                .findFirst()
                .orElse(null);
        if (team == null || team.members == null || team.members.isEmpty()) {
            return List.of(playerId);
        }
        return new ArrayList<>(team.members);
    }

    private static boolean teamHasActiveCountdown(List<UUID> teamMemberIds) {
        if (teamMemberIds == null || teamMemberIds.isEmpty()) return false;
        for (UUID memberId : teamMemberIds) {
            if (memberId != null && deadlineTickByPlayer.containsKey(memberId)) {
                return true;
            }
        }
        return false;
    }

    public static void restoreFromResumeState(MinecraftServer server, BingoGameData data) {
        if (server == null || data == null || !data.isActive() || !data.minesEnabled) return;
        if (isActive()) return;

        List<MineDef> restored = definitionsForIds(data.getMineResumeSourceIds());
        if (restored.isEmpty()) return;

        activeMines.clear();
        activeMines.addAll(restored);
        activeByTrigger.clear();
        for (MineDef mine : restored) {
            activeByTrigger.put(mine.trigger(), mine);
        }

        damageProgressByPlayer.clear();
        damageProgressByPlayer.putAll(data.getMineResumeDamageProgressByPlayer());

        int now = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        deadlineTickByPlayer.clear();
        for (Map.Entry<UUID, Integer> entry : data.getMineResumeRemainingSecondsByPlayer().entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            int remaining = Math.max(0, entry.getValue());
            if (remaining <= 0) continue;
            deadlineTickByPlayer.put(entry.getKey(), now + remaining * 20);
        }

        triggeredMineIdByPlayer.clear();
        triggeredMineIdByPlayer.putAll(data.getMineResumeTriggeredSourceByPlayer());
        lastWarningSecondsByPlayer.clear();
        defuseQuestId = data.getMineResumeDefuseQuestId();
        defuseDisplayName = data.getMineResumeDefuseDisplayName();
        active = true;
        lastSyncTick = -100;
        syncAll(server);
    }

    private static void persistResumeState(MinecraftServer server) {
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isActive() || !data.minesEnabled || !isActive()) {
            data.clearMineResumeState();
            return;
        }

        Map<UUID, Integer> remaining = new HashMap<>();
        int now = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        for (Map.Entry<UUID, Integer> entry : deadlineTickByPlayer.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            int seconds = Math.max(0, (entry.getValue() - now + 19) / 20);
            if (seconds > 0) {
                remaining.put(entry.getKey(), seconds);
            }
        }

        List<String> sourceIds = new ArrayList<>();
        for (MineDef mine : activeMines) {
            if (mine == null || mine.sourceQuestId() == null || mine.sourceQuestId().isBlank()) continue;
            sourceIds.add(mine.sourceQuestId());
        }

        data.updateMineResumeState(
                server,
                sourceIds,
                remaining,
                triggeredMineIdByPlayer,
                damageProgressByPlayer,
                defuseQuestId,
                defuseDisplayName
        );
    }
    public static void resetTransientRuntime() {
        active = false;
        activeMines.clear();
        activeByTrigger.clear();
        damageProgressByPlayer.clear();
        deadlineTickByPlayer.clear();
        triggeredMineIdByPlayer.clear();
        lastWarningSecondsByPlayer.clear();
        defuseQuestId = "";
        defuseDisplayName = "";
        lastSyncTick = -100;
    }
    private static void clearDefuse() {
        defuseQuestId = "";
        defuseDisplayName = "";
    }
}















































