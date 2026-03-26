package com.jamie.jamiebingo;

import com.jamie.jamiebingo.data.*;
import com.jamie.jamiebingo.bingo.WinCondition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID)
public class ProgressEvents {

    private static final int INVENTORY_SCAN_INTERVAL = 20;

    /* =========================
       PICKUP-BASED COMPLETION
       ========================= */

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        if (!data.isActive() || data.getCurrentCard() == null) return;
        if (data.startCountdownActive) return;

        ItemStack stack = event.getItem().getItem();
        if (com.jamie.jamiebingo.item.PlayerTrackerHandler.isPlayerTracker(stack)) return;
        Identifier rl = ForgeRegistries.ITEMS.getKey(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack));
        if (rl == null) return;

        String id = rl.toString();
        com.jamie.jamiebingo.mines.MineModeManager.onGoalCompleted(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player), id, player);
        com.jamie.jamiebingo.power.PowerSlotManager.onGoalCompleted(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player), id, player);
        // ✅ GUNGAME must check the player's active team card, not currentCard
if (!data.cardContainsForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player), id)) return;

        // 🔒 SERVER-AUTH LOCKOUT (LOCKOUT + RARITY)
        if (isBlockedByLockout(player, data, id)) {
            return;
        }

        if (data.markCompleted(com.jamie.jamiebingo.util.EntityUtil.getUUID(player), id)) {
            handleCompletion(player, id, stack);
            BroadcastHelper.syncProgress(player);
        }
    }

    /* =========================
       INVENTORY SCAN COMPLETION
       ========================= */

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        if (com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(player) % INVENTORY_SCAN_INTERVAL != 0) return;

        BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        if (!data.isActive() || data.getCurrentCard() == null) return;
        if (data.startCountdownActive) return;

        for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player)))
            checkStack(player, data, stack);

        for (ItemStack stack : getEquipmentStacks(player)) {
            checkStack(player, data, stack);
        }
    }

    /* =========================
       ALWAYS HAVE (REGISTER MODE)
       ========================= */

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        com.jamie.jamiebingo.util.ServerTickUtil.tick(server);
        if (com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) % INVENTORY_SCAN_INTERVAL != 0) return;

        BingoGameData data = BingoGameData.get(server);
        if (!data.isActive() || data.getCurrentCard() == null) return;
        if (data.startCountdownActive) return;

        if (data.registerMode != BingoGameData.REGISTER_ALWAYS_HAVE) return;

        if (data.winCondition == WinCondition.GUNGAME
                || data.winCondition == WinCondition.GAMEGUN) {
            return;
        }

        boolean changed = enforceAlwaysHave(server, data);
        if (!changed) return;

        TeamScoreData scores = TeamScoreData.get(server);
        if (data.winCondition == WinCondition.RARITY) {
            scores.recomputeRarityScores(server, data);
        } else {
            scores.recomputeStandardScores(server, data);
        }
        BroadcastHelper.broadcastTeamScores(server);
        BroadcastHelper.broadcastProgress(server);
        BroadcastHelper.broadcastSlotOwnership(server);
    }

    private static void checkStack(ServerPlayer player, BingoGameData data, ItemStack stack) {
        if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) return;
        if (com.jamie.jamiebingo.item.PlayerTrackerHandler.isPlayerTracker(stack)) return;

        Identifier rl = ForgeRegistries.ITEMS.getKey(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack));
        if (rl == null) return;

        String id = rl.toString();
        com.jamie.jamiebingo.mines.MineModeManager.onGoalCompleted(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player), id, player);
        com.jamie.jamiebingo.power.PowerSlotManager.onGoalCompleted(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player), id, player);
       // ✅ GUNGAME must check the player's active team card, not currentCard
if (!data.cardContainsForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player), id)) return;

        // 🔒 SERVER-AUTH LOCKOUT (LOCKOUT + RARITY)
        if (isBlockedByLockout(player, data, id)) {
            return;
        }

        if (data.markCompleted(com.jamie.jamiebingo.util.EntityUtil.getUUID(player), id)) {
            handleCompletion(player, id, stack);
            BroadcastHelper.syncProgress(player);
        }
    }

    private static boolean enforceAlwaysHave(net.minecraft.server.MinecraftServer server, BingoGameData data) {
        TeamData teamData = TeamData.get(server);
        boolean changed = false;

        for (TeamData.TeamInfo team : teamData.getTeams()) {
            if (team.members.isEmpty()) continue;

            Set<String> held = collectTeamItems(server, team);
            Set<String> completed = new HashSet<>(data.getTeamProgressForDisplay(team.id));

            for (String slotId : completed) {
                if (!isItemSlotId(slotId)) continue;
                if (held.contains(slotId)) continue;
                if (data.removeCompletedForTeam(team.id, slotId)) {
                    changed = true;
                }
            }
        }

        return changed;
    }

    private static boolean isItemSlotId(String slotId) {
        if (slotId == null || slotId.isBlank()) return false;
        return !slotId.startsWith("quest.");
    }

    private static Set<String> collectTeamItems(
            net.minecraft.server.MinecraftServer server,
            TeamData.TeamInfo team
    ) {
        Set<String> out = new HashSet<>();

        for (UUID memberId : team.members) {
            ServerPlayer player = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
            if (player == null) continue;

            for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player))) {
                addItemId(stack, out);
            }
            for (ItemStack stack : getEquipmentStacks(player)) {
                addItemId(stack, out);
            }
        }

        return out;
    }

    private static ItemStack[] getEquipmentStacks(ServerPlayer player) {
        return new ItemStack[] {
                com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(player, net.minecraft.world.entity.EquipmentSlot.HEAD),
                com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(player, net.minecraft.world.entity.EquipmentSlot.CHEST),
                com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(player, net.minecraft.world.entity.EquipmentSlot.LEGS),
                com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(player, net.minecraft.world.entity.EquipmentSlot.FEET),
                com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(player, net.minecraft.world.entity.EquipmentSlot.OFFHAND)
        };
    }

    private static void addItemId(ItemStack stack, Set<String> out) {
        if (stack == null || com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) return;
        if (com.jamie.jamiebingo.item.PlayerTrackerHandler.isPlayerTracker(stack)) return;
        Identifier rl = ForgeRegistries.ITEMS.getKey(com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack));
        if (rl == null) return;
        out.add(rl.toString());
    }

    /* =========================
       LOCKOUT RULE (AUTHORITATIVE)
       ========================= */

    private static boolean isBlockedByLockout(ServerPlayer player, BingoGameData data, String slotId) {

        // ✅ LOCKOUT APPLIES TO BOTH MODES
        if (data.winCondition != WinCondition.LOCKOUT
                && data.winCondition != WinCondition.RARITY)
            return false;

        TeamData teamData = TeamData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        UUID myTeam = teamData.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
        if (myTeam == null)
            return false;

        // slotId → owning DyeColor
        var ownership = data.getSlotOwnershipSnapshot();
        var ownerColor = ownership.get(slotId);

        if (ownerColor == null)
            return false;

        UUID owningTeam = teamData.getTeamForColor(ownerColor);
        if (owningTeam == null)
            return false;

        // ❌ another team already owns this slot
        return !owningTeam.equals(myTeam);
    }

    /* =========================
       COMPLETION FEEDBACK
       ========================= */

    private static void handleCompletion(ServerPlayer player, String slotId, ItemStack stack) {

        TeamData teamData = TeamData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        UUID teamId = teamData.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
        if (teamId == null) return;

        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);

        if (team == null) return;

        String fallbackName = stack != null && !com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)
                ? com.jamie.jamiebingo.util.ItemStackNameUtil.getHoverNameString(stack)
                : slotId;

        Component slotComp = BroadcastHelper.buildSlotChatComponent(
                com.jamie.jamiebingo.util.EntityServerUtil.getServer(player),
                teamId,
                slotId,
                fallbackName,
                stack == null ? com.jamie.jamiebingo.util.ItemStackUtil.empty() : stack
        );

        var server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        String teamLabel = team.color == null ? "Team" : "Team " + team.color.getName();
        String playerName = player.getGameProfile().name();
        Component prefix = com.jamie.jamiebingo.util.ComponentUtil.literal("[" + teamLabel + "] " + playerName)
                .withStyle(BroadcastHelper.teamChatFormatting(team.color));
        if (server != null && BingoGameData.get(server).hideGoalDetailsInChat) {
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
                    slotId
            );
        }
    }
}















