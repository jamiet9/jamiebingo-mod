package com.jamie.jamiebingo.power;

import com.jamie.jamiebingo.bingo.BingoCard;
import com.jamie.jamiebingo.bingo.BingoRarityUtil;
import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.casino.CasinoTickScheduler;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.data.BroadcastHelper;
import com.jamie.jamiebingo.data.TeamData;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.PacketFlashSlots;
import com.jamie.jamiebingo.network.PacketPlayTeamSound;
import com.jamie.jamiebingo.network.PacketPowerSlotWheelEvent;
import com.jamie.jamiebingo.network.PacketSyncCard;
import com.jamie.jamiebingo.network.PacketSyncPowerSlotState;
import com.jamie.jamiebingo.quest.QuestTracker;
import com.jamie.jamiebingo.util.ComponentUtil;
import com.jamie.jamiebingo.util.ServerPlayerListUtil;
import com.jamie.jamiebingo.util.ServerTickUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public final class PowerSlotManager {
    private static final int WHEEL_SPIN_TICKS = 60;
    private static final int WHEEL_SYNC_BUFFER_TICKS = 8;
    private static final int CLAIMED_REMAINING_SENTINEL = -2;
    private static final int BUFF_SABOTAGE_PRE_REROLL_TICKS = 60;
    private static final int BUFF_SABOTAGE_POST_REROLL_TICKS = 60;
    private static final int QUEUED_REROLL_BATCH_INTERVAL_TICKS = 2;
    private static boolean active = false;
    private static String slotId = "";
    private static String displayName = "";
    private static int intervalSeconds = 60;
    private static int nextRerollTick = -1;
    private static int lastSyncTick = -100;
    private static boolean pendingResolution = false;
    private static boolean pendingBuff = false;
    private static int pendingResolveTick = -1;
    private static UUID pendingActorId = null;
    private static UUID pendingActorTeamId = null;
    private static String pendingCompletedId = "";
    private static final Set<UUID> pendingOpponentTeamIds = new HashSet<>();
    private record QueuedReroll(UUID teamId, int x, int y, String oldId, BingoSlot replacement) {}

    private PowerSlotManager() {
    }

    public static void start(MinecraftServer server, BingoGameData data) {
        clear(server);
        if (server == null || data == null || !data.isActive() || !data.powerSlotEnabled) return;
        if (!BingoGameData.isPowerSlotSupportedWinCondition(data.winCondition)) return;
        active = true;
        intervalSeconds = Math.max(10, Math.min(300, data.powerSlotIntervalSeconds));
        rerollGoal(server, data);
        int now = ServerTickUtil.getTickCount(server);
        nextRerollTick = now + intervalSeconds * 20;
        lastSyncTick = -100;
        persistResumeState(server);
        syncAll(server);
    }

    public static void clear(MinecraftServer server) {
        active = false;
        slotId = "";
        displayName = "";
        intervalSeconds = 60;
        nextRerollTick = -1;
        lastSyncTick = -100;
        pendingResolution = false;
        pendingBuff = false;
        pendingResolveTick = -1;
        pendingActorId = null;
        pendingActorTeamId = null;
        pendingCompletedId = "";
        pendingOpponentTeamIds.clear();
        if (server != null) {
            BingoGameData.get(server).clearPowerSlotResumeState();
            NetworkHandler.send(new PacketSyncPowerSlotState(false, "", "", -1), PacketDistributor.ALL.noArg());
        }
    }

    public static void resetTransientRuntime() {
        active = false;
        slotId = "";
        displayName = "";
        intervalSeconds = 60;
        nextRerollTick = -1;
        lastSyncTick = -100;
        pendingResolution = false;
        pendingBuff = false;
        pendingResolveTick = -1;
        pendingActorId = null;
        pendingActorTeamId = null;
        pendingCompletedId = "";
        pendingOpponentTeamIds.clear();
    }

    public static void onServerTick(MinecraftServer server) {
        if (server == null) return;
        if (!active) return;
        BingoGameData data = BingoGameData.get(server);
        if (!data.isActive() || data.pendingWinEndActive || data.stopGamePending) return;
        if (!data.powerSlotEnabled || !BingoGameData.isPowerSlotSupportedWinCondition(data.winCondition)) {
            clear(server);
            return;
        }
        int now = ServerTickUtil.getTickCount(server);
        if (pendingResolution) {
            if (now >= pendingResolveTick) {
                resolvePendingOutcome(server, data);
            }
            if (now - lastSyncTick >= 20) {
                lastSyncTick = now;
                syncAll(server);
            }
            persistResumeState(server);
            return;
        }
        intervalSeconds = Math.max(10, Math.min(300, data.powerSlotIntervalSeconds));
        if (slotId.isBlank() || nextRerollTick < 0 || now >= nextRerollTick) {
            rerollGoal(server, data);
            nextRerollTick = now + intervalSeconds * 20;
            syncAll(server);
            persistResumeState(server);
            return;
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
        if (server == null || !active || slotId.isBlank()) {
            NetworkHandler.send(new PacketSyncPowerSlotState(false, "", "", -1), PacketDistributor.PLAYER.with(player));
            return;
        }
        syncPlayer(server, player);
    }

    public static void onGoalCompleted(MinecraftServer server, String completedId, ServerPlayer completedBy) {
        if (!active || server == null || completedId == null || completedId.isBlank()) return;
        if (pendingResolution) return;
        if (slotId.isBlank() || !slotId.equals(completedId)) return;
        if (completedBy == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isActive()) return;
        TeamData teamData = TeamData.get(server);
        UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(completedBy);
        UUID teamId = teamData.getTeamForPlayer(playerId);
        if (teamId == null) return;

        List<UUID> activeTeams = getActiveTeamIds(server, data);
        List<UUID> opponentTeams = new ArrayList<>();
        for (UUID id : activeTeams) {
            if (!id.equals(teamId)) {
                opponentTeams.add(id);
            }
        }

        boolean canBuff = canApplyBuffForTeam(data, teamId);
        boolean canSabotage = false;
        for (UUID opponentId : opponentTeams) {
            if (canApplySabotageForTeam(data, opponentId)) {
                canSabotage = true;
                break;
            }
        }

        if (!canBuff && !canSabotage) {
            BroadcastHelper.broadcast(server, ComponentUtil.literal("no possible slots to reroll."));
            rerollGoal(server, data);
            int now = ServerTickUtil.getTickCount(server);
            nextRerollTick = now + intervalSeconds * 20;
            persistResumeState(server);
            syncAll(server);
            return;
        }

        boolean buff;
        if (opponentTeams.isEmpty()) {
            buff = true;
        } else if (canBuff && canSabotage) {
            buff = new Random().nextBoolean();
        } else {
            buff = canBuff;
        }
        NetworkHandler.send(new PacketPowerSlotWheelEvent(buff), PacketDistributor.PLAYER.with(completedBy));
        pendingResolution = true;
        pendingBuff = buff;
        pendingResolveTick = ServerTickUtil.getTickCount(server) + WHEEL_SPIN_TICKS + WHEEL_SYNC_BUFFER_TICKS;
        pendingActorId = playerId;
        pendingActorTeamId = teamId;
        pendingCompletedId = completedId;
        pendingOpponentTeamIds.clear();
        pendingOpponentTeamIds.addAll(opponentTeams);
        persistResumeState(server);
        syncAll(server);
    }

    private static void resolvePendingOutcome(MinecraftServer server, BingoGameData data) {
        if (!pendingResolution || server == null || data == null) return;

        UUID actorId = pendingActorId;
        UUID actorTeamId = pendingActorTeamId;
        boolean buff = pendingBuff;
        String completedId = pendingCompletedId;

        pendingResolution = false;
        pendingBuff = false;
        pendingResolveTick = -1;
        pendingActorId = null;
        pendingActorTeamId = null;
        pendingCompletedId = "";

        if (actorTeamId == null) {
            pendingOpponentTeamIds.clear();
            rerollGoal(server, data);
            int now = ServerTickUtil.getTickCount(server);
            nextRerollTick = now + intervalSeconds * 20;
            persistResumeState(server);
            syncAll(server);
            return;
        }

        ServerPlayer actor = actorId == null ? null : ServerPlayerListUtil.getPlayer(server, actorId);
        Set<UUID> buffedTeams = new HashSet<>();
        Set<UUID> sabotagedTeams = new HashSet<>();
        List<QueuedReroll> queuedRerolls = new ArrayList<>();
        if (buff) {
            if (applySingleBuff(server, data, actorTeamId, completedId, queuedRerolls)) {
                buffedTeams.add(actorTeamId);
            }
            if (!buffedTeams.isEmpty() && actor != null) {
                broadcastActionMessage(server, actor, true);
            }
        } else {
            for (UUID opponentId : pendingOpponentTeamIds) {
                if (applySingleSabotage(server, data, opponentId, completedId, queuedRerolls)) {
                    sabotagedTeams.add(opponentId);
                }
            }
            if (!sabotagedTeams.isEmpty() && actor != null) {
                broadcastActionMessage(server, actor, false);
            }
        }

        if (!buffedTeams.isEmpty()) {
            for (UUID id : buffedTeams) {
                sendTeamAnnouncement(server, id, true);
            }
        }
        if (!sabotagedTeams.isEmpty()) {
            for (UUID id : sabotagedTeams) {
                sendTeamAnnouncement(server, id, false);
            }
        }
        if (!queuedRerolls.isEmpty()) {
            schedule(server, BUFF_SABOTAGE_PRE_REROLL_TICKS, () -> applyQueuedRerolls(server, data, queuedRerolls));
        }
        pendingOpponentTeamIds.clear();

        rerollGoal(server, data);
        int now = ServerTickUtil.getTickCount(server);
        nextRerollTick = now + intervalSeconds * 20;
        persistResumeState(server);
        syncAll(server);
    }

    private static boolean applySingleBuff(MinecraftServer server, BingoGameData data, UUID teamId, String protectedId, List<QueuedReroll> queued) {
        return rerollSingleUnclaimedSlot(server, data, teamId, true, protectedId, queued);
    }

    private static boolean applySingleSabotage(MinecraftServer server, BingoGameData data, UUID teamId, String protectedId, List<QueuedReroll> queued) {
        return rerollSingleUnclaimedSlot(server, data, teamId, false, protectedId, queued);
    }

    private static boolean canApplySabotageForTeam(BingoGameData data, UUID teamId) {
        if (data == null || teamId == null) return false;
        BingoCard card = data.getActiveCardForTeam(teamId);
        if (card == null) return false;
        Set<String> teamCompleted = data.getTeamProgressForDisplay(teamId);
        for (int y = 0; y < card.getSize(); y++) {
            for (int x = 0; x < card.getSize(); x++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot == null || slot.getId() == null || slot.getId().isBlank()) continue;
                if (teamCompleted.contains(slot.getId())) continue;
                int idx = rarityIndex(slot.getRarity());
                if (idx >= 0 && idx < BingoRarityUtil.ORDERED_RARITIES.size() - 1) return true;
            }
        }
        return false;
    }

    private static boolean rerollSingleUnclaimedSlot(MinecraftServer server, BingoGameData data, UUID teamId, boolean moreCommon, String protectedId, List<QueuedReroll> queued) {
        BingoCard card = data.getOrCreateTeamCardOverride(teamId);
        if (card == null || card.getSize() <= 0) return false;
        Set<String> teamCompleted = data.getTeamProgressForDisplay(teamId);
        List<int[]> candidates = new ArrayList<>();
        for (int y = 0; y < card.getSize(); y++) {
            for (int x = 0; x < card.getSize(); x++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot == null || slot.getId() == null || slot.getId().isBlank()) continue;
                if (teamCompleted.contains(slot.getId())) continue;
                if (protectedId != null && !protectedId.isBlank() && protectedId.equals(slot.getId())) continue;
                int idx = rarityIndex(slot.getRarity());
                if (moreCommon) {
                    if (idx <= 0) continue;
                } else {
                    if (idx < 0 || idx >= BingoRarityUtil.ORDERED_RARITIES.size() - 1) continue;
                }
                candidates.add(new int[]{x, y});
            }
        }
        if (candidates.isEmpty()) return false;

        Collections.shuffle(candidates, new Random());
        for (int[] pos : candidates) {
            int x = pos[0];
            int y = pos[1];
            BingoSlot from = card.getSlot(x, y);
            if (from == null) continue;
            int idx = rarityIndex(from.getRarity());
            int targetIdx = moreCommon ? idx - 1 : idx + 1;
            if (targetIdx < 0 || targetIdx >= BingoRarityUtil.ORDERED_RARITIES.size()) continue;
            String targetRarity = BingoRarityUtil.ORDERED_RARITIES.get(targetIdx);
            BingoSlot replacement = generateReplacementForRarity(server, data, card, from.getId(), targetRarity);
            if (replacement == null) continue;
            String oldId = from.getId();
            animateTeamSlots(server, teamId, Set.of(oldId), BUFF_SABOTAGE_PRE_REROLL_TICKS, PacketFlashSlots.STYLE_SHAKE);
            if (queued != null) {
                queued.add(new QueuedReroll(teamId, x, y, oldId, replacement));
            }
            return true;
        }
        return false;
    }

    private static void applyQueuedRerolls(MinecraftServer server, BingoGameData data, List<QueuedReroll> queuedRerolls) {
        if (server == null || data == null || queuedRerolls == null || queuedRerolls.isEmpty()) return;
        Map<UUID, List<QueuedReroll>> queuedByTeam = new LinkedHashMap<>();
        for (QueuedReroll q : queuedRerolls) {
            if (q == null || q.teamId() == null || q.replacement() == null) continue;
            queuedByTeam.computeIfAbsent(q.teamId(), ignored -> new ArrayList<>()).add(q);
        }
        if (queuedByTeam.isEmpty()) return;

        int delay = 0;
        for (Map.Entry<UUID, List<QueuedReroll>> entry : queuedByTeam.entrySet()) {
            UUID teamId = entry.getKey();
            List<QueuedReroll> teamQueued = entry.getValue();
            if (teamId == null || teamQueued == null || teamQueued.isEmpty()) continue;
            int teamDelay = delay;
            schedule(server, teamDelay, () -> applyQueuedRerollBatch(server, data, teamId, teamQueued));
            delay += QUEUED_REROLL_BATCH_INTERVAL_TICKS;
        }
    }

    private static void applyQueuedRerollBatch(MinecraftServer server, BingoGameData data, UUID teamId, List<QueuedReroll> queuedRerolls) {
        if (server == null || data == null || teamId == null || queuedRerolls == null || queuedRerolls.isEmpty()) return;
        BingoCard current = data.getActiveCardForTeam(teamId);
        if (current == null) return;

        Set<String> newIds = new HashSet<>();
        boolean changedAny = false;
        for (QueuedReroll q : queuedRerolls) {
            if (q == null || q.replacement() == null) continue;
            if (q.y() < 0 || q.y() >= current.getSize() || q.x() < 0 || q.x() >= current.getSize()) continue;
            BingoSlot existing = current.getSlot(q.x(), q.y());
            if (existing == null || existing.getId() == null || !existing.getId().equals(q.oldId())) continue;
            current.setSlot(q.x(), q.y(), q.replacement());
            changedAny = true;
            if (q.replacement().getId() != null && !q.replacement().getId().isBlank()) {
                newIds.add(q.replacement().getId());
            }
        }
        if (!changedAny || newIds.isEmpty()) return;

        syncTeamCard(server, data, teamId);
        animateTeamSlots(server, teamId, newIds, BUFF_SABOTAGE_POST_REROLL_TICKS, PacketFlashSlots.STYLE_PULSE);
        playTeamPopSound(server, teamId);
    }

    private static void syncTeamCard(MinecraftServer server, BingoGameData data, UUID teamId) {
        if (server == null || data == null || teamId == null) return;
        TeamData teamData = TeamData.get(server);
        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t != null && teamId.equals(t.id))
                .findFirst()
                .orElse(null);
        if (team == null || team.members == null || team.members.isEmpty()) return;
        BingoCard card = data.getActiveCardForTeam(teamId);
        if (card == null) return;
        for (UUID memberId : team.members) {
            ServerPlayer player = ServerPlayerListUtil.getPlayer(server, memberId);
            if (player == null) continue;
            NetworkHandler.send(
                    new PacketSyncCard(
                            card,
                            data.winCondition,
                            data.teamChestEnabled,
                            data.getGreenFakeIndicesForTeam(teamId),
                            data.getRedFakeIndicesForTeam(teamId)
                    ),
                    PacketDistributor.PLAYER.with(player)
            );
        }
    }

    private static BingoSlot generateReplacementForRarity(
            MinecraftServer server,
            BingoGameData data,
            BingoCard card,
            String previousId,
            String targetRarity
    ) {
        if (server == null || data == null || card == null) return null;
        Set<String> excluded = new HashSet<>();
        for (int y = 0; y < card.getSize(); y++) {
            for (int x = 0; x < card.getSize(); x++) {
                BingoSlot s = card.getSlot(x, y);
                if (s == null || s.getId() == null || s.getId().isBlank()) continue;
                if (previousId != null && previousId.equals(s.getId())) continue;
                excluded.add(s.getId());
            }
        }
        if (slotId != null && !slotId.isBlank()) {
            excluded.add(slotId);
        }
        Random rng = new Random();
        for (int attempt = 0; attempt < 120; attempt++) {
            BingoSlot candidate = data.generateRandomGoalNotOnCurrentCard(server, rng, excluded);
            if (candidate == null || candidate.getId() == null || candidate.getId().isBlank()) continue;
            if (!BingoRarityUtil.normalize(candidate.getRarity()).equals(targetRarity)) continue;
            if (!isCandidateAllowedForPowerGoal(server, data, candidate)) continue;
            return new BingoSlot(candidate.getId(), candidate.getName(), candidate.getCategory(), candidate.getRarity());
        }
        return null;
    }

    private static boolean canApplyBuffForTeam(BingoGameData data, UUID teamId) {
        if (data == null || teamId == null) return false;
        BingoCard card = data.getActiveCardForTeam(teamId);
        if (card == null) return false;
        Set<String> teamCompleted = data.getTeamProgressForDisplay(teamId);
        for (int y = 0; y < card.getSize(); y++) {
            for (int x = 0; x < card.getSize(); x++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot == null || slot.getId() == null || slot.getId().isBlank()) continue;
                if (teamCompleted.contains(slot.getId())) continue;
                if (rarityIndex(slot.getRarity()) > 0) return true;
            }
        }
        return false;
    }

    private static int rarityIndex(String rarity) {
        int idx = BingoRarityUtil.ORDERED_RARITIES.indexOf(BingoRarityUtil.normalize(rarity));
        return idx < 0 ? -1 : idx;
    }

    private static void rerollGoal(MinecraftServer server, BingoGameData data) {
        if (server == null || data == null) return;
        Set<String> excluded = new HashSet<>();
        BingoCard card = data.getCurrentCard();
        if (card != null) {
            for (int y = 0; y < card.getSize(); y++) {
                for (int x = 0; x < card.getSize(); x++) {
                    BingoSlot s = card.getSlot(x, y);
                    if (s != null && s.getId() != null && !s.getId().isBlank()) {
                        excluded.add(s.getId());
                    }
                }
            }
        }
        if (!slotId.isBlank()) {
            excluded.add(slotId);
        }
        Random rng = new Random();
        BingoSlot chosen = null;
        for (int i = 0; i < 140; i++) {
            BingoSlot candidate = data.generateRandomGoalNotOnCurrentCard(server, rng, excluded);
            if (candidate == null) continue;
            if (!isCandidateAllowedForPowerGoal(server, data, candidate)) continue;
            chosen = candidate;
            break;
        }
        if (chosen == null || chosen.getId() == null || chosen.getId().isBlank()) {
            slotId = "";
            displayName = "";
            return;
        }
        slotId = chosen.getId();
        displayName = chosen.getName() == null || chosen.getName().isBlank() ? chosen.getId() : chosen.getName();
    }

    private static boolean isCandidateAllowedForPowerGoal(MinecraftServer server, BingoGameData data, BingoSlot candidate) {
        if (server == null || data == null || candidate == null || candidate.getId() == null || candidate.getId().isBlank()) return false;
        String id = candidate.getId();
        if (id.startsWith("quest.")) {
            for (ServerPlayer player : ServerPlayerListUtil.getPlayers(server)) {
                UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
                if (!data.isParticipant(playerId) || data.isSpectator(playerId)) continue;
                if (QuestTracker.isQuestComplete(player, id)) {
                    return false;
                }
            }
            return true;
        }
        for (ServerPlayer player : ServerPlayerListUtil.getPlayers(server)) {
            UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
            if (!data.isParticipant(playerId) || data.isSpectator(playerId)) continue;
            if (isHoldingCandidateItem(player, id)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHoldingCandidateItem(ServerPlayer player, String slotId) {
        if (player == null || slotId == null || slotId.isBlank()) return false;
        Identifier key = Identifier.tryParse(slotId.contains(":") ? slotId : "minecraft:" + slotId);
        if (key == null) return false;
        Item target = ForgeRegistries.ITEMS.getValue(key);
        if (target == null) return false;
        for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player))) {
            if (stack != null && !stack.isEmpty() && stack.getItem() == target) {
                return true;
            }
        }
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            ItemStack equipped = com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(player, slot);
            if (equipped != null && !equipped.isEmpty() && equipped.getItem() == target) {
                return true;
            }
        }
        return false;
    }

    private static void syncAll(MinecraftServer server) {
        if (server == null) return;
        if (!active || slotId.isBlank()) {
            NetworkHandler.send(new PacketSyncPowerSlotState(false, "", "", -1), PacketDistributor.ALL.noArg());
            return;
        }
        for (ServerPlayer player : ServerPlayerListUtil.getPlayers(server)) {
            syncPlayer(server, player);
        }
    }

    private static void syncPlayer(MinecraftServer server, ServerPlayer player) {
        if (server == null || player == null) return;
        if (!active || slotId.isBlank()) {
            NetworkHandler.send(new PacketSyncPowerSlotState(false, "", "", -1), PacketDistributor.PLAYER.with(player));
            return;
        }
        int remaining;
        if (pendingResolution) {
            remaining = CLAIMED_REMAINING_SENTINEL;
        } else {
            int now = ServerTickUtil.getTickCount(server);
            remaining = nextRerollTick > 0 ? Math.max(0, (nextRerollTick - now + 19) / 20) : -1;
        }
        NetworkHandler.send(
                new PacketSyncPowerSlotState(true, slotId, displayName, remaining),
                PacketDistributor.PLAYER.with(player)
        );
    }

    public static void restoreFromResumeState(MinecraftServer server, BingoGameData data) {
        if (server == null || data == null || !data.isActive() || !data.powerSlotEnabled) return;
        if (!BingoGameData.isPowerSlotSupportedWinCondition(data.winCondition)) return;
        if (isActive()) return;
        String savedId = data.getPowerResumeSlotId();
        if (savedId == null || savedId.isBlank()) return;

        active = true;
        slotId = savedId;
        displayName = data.getPowerResumeDisplayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = slotId;
        }
        intervalSeconds = Math.max(10, Math.min(300, data.powerSlotIntervalSeconds));
        int remainingSeconds = Math.max(0, data.getPowerResumeRemainingSeconds());
        int now = ServerTickUtil.getTickCount(server);
        nextRerollTick = now + remainingSeconds * 20;
        pendingResolution = false;
        pendingBuff = false;
        pendingResolveTick = -1;
        pendingActorId = null;
        pendingActorTeamId = null;
        pendingOpponentTeamIds.clear();
        lastSyncTick = -100;
        syncAll(server);
    }

    private static void persistResumeState(MinecraftServer server) {
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isActive() || !data.powerSlotEnabled || !active || slotId == null || slotId.isBlank()) {
            if (data != null) {
                data.clearPowerSlotResumeState();
            }
            return;
        }

        int remainingSeconds;
        if (pendingResolution) {
            remainingSeconds = 0;
        } else {
            int now = ServerTickUtil.getTickCount(server);
            remainingSeconds = nextRerollTick > 0 ? Math.max(0, (nextRerollTick - now + 19) / 20) : -1;
        }
        data.updatePowerSlotResumeState(slotId, displayName, remainingSeconds, pendingResolution);
    }

    private static boolean isActive() {
        return active && slotId != null && !slotId.isBlank();
    }

    private static void animateTeamSlots(MinecraftServer server, UUID teamId, Set<String> slots) {
        animateTeamSlots(server, teamId, slots, 140, PacketFlashSlots.STYLE_PULSE);
    }

    private static void animateTeamSlots(MinecraftServer server, UUID teamId, Set<String> slots, int durationTicks, int style) {
        if (server == null || teamId == null || slots == null || slots.isEmpty()) return;
        TeamData teamData = TeamData.get(server);
        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t != null && teamId.equals(t.id))
                .findFirst()
                .orElse(null);
        if (team == null) return;
        PacketFlashSlots packet = new PacketFlashSlots(slots, durationTicks, style);
        for (UUID memberId : team.members) {
            ServerPlayer player = ServerPlayerListUtil.getPlayer(server, memberId);
            if (player != null) {
                NetworkHandler.send(packet, PacketDistributor.PLAYER.with(player));
            }
        }
    }

    private static void playTeamPopSound(MinecraftServer server, UUID teamId) {
        if (server == null || teamId == null) return;
        TeamData teamData = TeamData.get(server);
        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t != null && teamId.equals(t.id))
                .findFirst()
                .orElse(null);
        if (team == null) return;
        for (UUID memberId : team.members) {
            ServerPlayer player = ServerPlayerListUtil.getPlayer(server, memberId);
            if (player == null) continue;
            NetworkHandler.send(
                    new PacketPlayTeamSound("minecraft:entity.slime.squish", 0.9f, 1.25f),
                    PacketDistributor.PLAYER.with(player)
            );
            player.playSound(SoundEvents.SLIME_SQUISH, 0.85f, 1.2f);
        }
    }

    private static void schedule(MinecraftServer server, int delayTicks, Runnable run) {
        if (server == null || run == null) return;
        int when = ServerTickUtil.getTickCount(server) + Math.max(0, delayTicks);
        CasinoTickScheduler.schedule(server, when, run);
    }

    private static void sendTeamAnnouncement(MinecraftServer server, UUID teamId, boolean buff) {
        if (server == null || teamId == null) return;
        TeamData teamData = TeamData.get(server);
        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t != null && teamId.equals(t.id))
                .findFirst()
                .orElse(null);
        if (team == null) return;
        String text = buff ? "Buffed!" : "Sabotaged!";
        String soundId = buff ? "minecraft:entity.player.levelup" : "minecraft:entity.villager.no";
        float soundPitch = buff ? 1.15f : 0.8f;
        for (UUID memberId : team.members) {
            ServerPlayer player = ServerPlayerListUtil.getPlayer(server, memberId);
            if (player == null) continue;
            player.connection.send(new ClientboundSetTitleTextPacket(ComponentUtil.literal(text)));
            NetworkHandler.send(new PacketPlayTeamSound(soundId, 1.0f, soundPitch), PacketDistributor.PLAYER.with(player));
            if (buff) {
                player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.1f);
            } else {
                player.playSound(SoundEvents.VILLAGER_NO, 1.0f, 0.8f);
            }
        }
    }

    private static void broadcastActionMessage(MinecraftServer server, ServerPlayer actor, boolean buff) {
        if (server == null || actor == null) return;
        TeamData teamData = TeamData.get(server);
        UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(actor);
        UUID teamId = teamData.getTeamForPlayer(playerId);
        TeamData.TeamInfo info = teamData.getTeams().stream()
                .filter(t -> t != null && teamId != null && teamId.equals(t.id))
                .findFirst()
                .orElse(null);
        ChatFormatting fmt = BroadcastHelper.teamChatFormatting(info == null ? null : info.color);
        String teamName = info == null || info.color == null ? "Team" : "Team " + info.color.getName();
        String text = buff
                ? teamName + " " + actor.getGameProfile().name() + " triggered a buff!"
                : teamName + " " + actor.getGameProfile().name() + " triggered a sabotage!";
        MutableComponent message = ComponentUtil.literal(text).withStyle(fmt);
        BroadcastHelper.broadcast(server, message);
    }

    private static List<UUID> getActiveTeamIds(MinecraftServer server, BingoGameData data) {
        List<UUID> active = new ArrayList<>();
        if (server == null || data == null) return active;
        TeamData teamData = TeamData.get(server);
        for (TeamData.TeamInfo team : teamData.getTeams()) {
            if (team == null || team.id == null || team.members.isEmpty()) continue;
            if (data.isTeamEliminated(team.id)) continue;
            boolean hasActiveMember = false;
            for (UUID memberId : team.members) {
                if (!data.isParticipant(memberId) || data.isSpectator(memberId)) continue;
                hasActiveMember = true;
                break;
            }
            if (hasActiveMember) {
                active.add(team.id);
            }
        }
        return active;
    }
}
