package com.jamie.jamiebingo.data;

import com.jamie.jamiebingo.bingo.BingoLineType;
import com.jamie.jamiebingo.bingo.WinCondition;
import com.jamie.jamiebingo.network.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import com.jamie.jamiebingo.bingo.BingoCard;
import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.quest.QuestTracker;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.jamie.jamiebingo.util.DataComponents;
import net.minecraftforge.registries.ForgeRegistries;
import com.jamie.jamiebingo.quest.icon.QuestIconProvider;
import com.jamie.jamiebingo.quest.icon.QuestIconData;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.*;

public class BroadcastHelper {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static MinecraftServer server() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    private static UUID resolveViewTeamId(ServerPlayer player, BingoGameData data, TeamData teamData) {
        if (player == null || data == null || teamData == null) return null;
        UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
        if (!data.isSpectator(playerId)) {
            UUID teamId = teamData.getTeamForPlayer(playerId);
            return teamId != null ? teamId : playerId;
        }

        UUID targetId = data.getSpectatorViewTarget(playerId);
        if (targetId == null) return null;
        UUID teamId = teamData.getTeamForPlayer(targetId);
        return teamId != null ? teamId : targetId;
    }

    /* =========================
       GENERIC CHAT
       ========================= */

    public static void broadcast(MinecraftServer server, Component message) {
        if (server == null) return;
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            sendSystemMessage(player, message);
        }
    }

    public static ChatFormatting teamChatFormatting(DyeColor color) {
        if (color == null) return ChatFormatting.WHITE;
        return switch (color) {
            case WHITE -> ChatFormatting.WHITE;
            case ORANGE -> ChatFormatting.GOLD;
            case MAGENTA -> ChatFormatting.LIGHT_PURPLE;
            case LIGHT_BLUE -> ChatFormatting.AQUA;
            case YELLOW -> ChatFormatting.YELLOW;
            case LIME -> ChatFormatting.GREEN;
            case PINK -> ChatFormatting.LIGHT_PURPLE;
            case GRAY -> ChatFormatting.DARK_GRAY;
            case LIGHT_GRAY -> ChatFormatting.GRAY;
            case CYAN -> ChatFormatting.DARK_AQUA;
            case PURPLE -> ChatFormatting.DARK_PURPLE;
            case BLUE -> ChatFormatting.BLUE;
            case BROWN -> ChatFormatting.GOLD;
            case GREEN -> ChatFormatting.DARK_GREEN;
            case RED -> ChatFormatting.RED;
            case BLACK -> ChatFormatting.BLACK;
        };
    }

    public static MutableComponent teamNameComponent(DyeColor color) {
        String name = color == null ? "Team" : color.getName();
        return com.jamie.jamiebingo.util.ComponentUtil.literal("Team " + name)
                .withStyle(teamChatFormatting(color));
    }

    private static void sendSystemMessage(ServerPlayer player, Component message) {
        if (player == null || message == null) return;
        try {
            java.lang.reflect.Method m = findMethodByNameAndArgCount(player.getClass(), "sendSystemMessage", 1);
            if (m != null) {
                m.invoke(player, message);
                return;
            }
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Method m = findMethodByNameAndArgCount(player.getClass(), "displayClientMessage", 2);
            if (m != null) {
                m.invoke(player, message, false);
                return;
            }
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Method m = findMethodByNameAndArgCount(player.getClass(), "sendMessage", 2);
            if (m != null) {
                Object uuid = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
                m.invoke(player, message, uuid);
                return;
            }
        } catch (Exception ignored) {
        }
    }

    private static java.lang.reflect.Method findMethodByNameAndArgCount(Class<?> type, String name, int argCount) {
        for (java.lang.reflect.Method m : type.getMethods()) {
            if (!m.getName().equals(name)) continue;
            if (m.getParameterCount() == argCount) return m;
        }
        return null;
    }

    public static void broadcastCompletionWithIcon(
            MinecraftServer server,
            Component message,
            String slotId
    ) {
        if (server == null || message == null) return;
        PacketChatIconMessage pkt = new PacketChatIconMessage(message, slotId == null ? "" : slotId);
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            NetworkHandler.send(pkt, PacketDistributor.PLAYER.with(player));
        }
        LOGGER.info("[JamieBingo] broadcastCompletionWithIcon slotId={}", slotId);
    }

    public static MutableComponent buildSlotChatComponent(
            MinecraftServer server,
            UUID teamId,
            String slotId,
            String fallbackName,
            ItemStack explicitStack
    ) {
        if (server == null) {
            String text = (fallbackName != null && !fallbackName.isBlank()) ? fallbackName : slotId;
            return com.jamie.jamiebingo.util.ComponentUtil.literal(text);
        }

        String displayName = (fallbackName != null && !fallbackName.isBlank()) ? fallbackName : slotId;
        BingoSlot slot = null;

        if (teamId != null && slotId != null) {
            BingoGameData data = BingoGameData.get(server);
            BingoCard card = data != null ? data.getActiveCardForTeam(teamId) : null;
            if (card != null) {
                for (int y = 0; y < card.getSize(); y++) {
                    for (int x = 0; x < card.getSize(); x++) {
                        BingoSlot s = card.getSlot(x, y);
                        if (s != null && slotId.equals(s.getId())) {
                            slot = s;
                            break;
                        }
                    }
                    if (slot != null) break;
                }
            }
        }

        if (slot != null && slot.getName() != null && !slot.getName().isBlank()) {
            displayName = slot.getName();
        }

        ItemStack stack = explicitStack != null && !explicitStack.isEmpty()
                ? explicitStack.copy()
                : com.jamie.jamiebingo.util.ItemStackUtil.empty();
        if (!stack.isEmpty()) {
            stack.setCount(1);
        }

        if (stack.isEmpty() && slot != null && slot.getId() != null && slot.getId().startsWith("quest.")) {
            QuestIconData icon = QuestIconProvider.iconFor(slot);
            if (icon != null && icon.mainIcon != null && !icon.mainIcon.isEmpty()) {
                stack = icon.mainIcon.copy();
            } else if (icon != null && icon.rotatingIcons != null && !icon.rotatingIcons.isEmpty()) {
                stack = icon.rotatingIcons.get(0).copy();
            } else if (icon != null && icon.cornerIcon != null && !icon.cornerIcon.isEmpty()) {
                stack = icon.cornerIcon.copy();
            } else {
                stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:book");
            }
        }

        if (stack.isEmpty() && slotId != null && !slotId.startsWith("quest.")) {
            try {
                Identifier rl = com.jamie.jamiebingo.util.IdUtil.id(slotId.contains(":") ? slotId : "minecraft:" + slotId);
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item != null) {
                    stack = new ItemStack(item);
                }
            } catch (Exception ignored) {}
        }

        if (!stack.isEmpty() && displayName != null && !displayName.isBlank()) {
            com.jamie.jamiebingo.util.ItemStackComponentUtil.set(
                    stack,
                    DataComponents.CUSTOM_NAME,
                    com.jamie.jamiebingo.util.ComponentUtil.literal(displayName)
            );
        }

        String safeName = (displayName != null && !displayName.isBlank()) ? displayName : slotId;
        MutableComponent slotComp = com.jamie.jamiebingo.util.ComponentUtil.literal("[")
                .append(com.jamie.jamiebingo.util.ComponentUtil.literal(safeName))
                .append(com.jamie.jamiebingo.util.ComponentUtil.literal("]"));

        if (!stack.isEmpty()) {
            ItemStack hoverStack = sanitizeHoverStack(stack.copy());
            final ItemStack hoverStackFinal = hoverStack;
            slotComp = slotComp.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowItem(hoverStackFinal)));
        }

        if (slotId != null && !slotId.isBlank()) {
            String insertion = "jamiebingo:icon:" + slotId;
            slotComp = slotComp.withStyle(style -> style.withInsertion(insertion));
            LOGGER.info("[JamieBingo] Chat icon insertion set: {}", insertion);
        }

        return slotComp.withStyle(style -> style.withColor(ChatFormatting.AQUA));
    }

    private static ItemStack sanitizeHoverStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return com.jamie.jamiebingo.util.ItemStackUtil.empty();
        Item item = com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack);
        if (item == null || item == Items.AIR) {
            return com.jamie.jamiebingo.util.ItemStackUtil.empty();
        }
        ItemStack safe = new ItemStack(item);
        safe.setCount(1);
        return safe;
    }

    public static Component buildChatIconMarker(String slotId) {
        return com.jamie.jamiebingo.util.ComponentUtil.empty();
    }

    /* =========================
       FULL SYNC
       ========================= */

    public static void broadcastFullSync() {
        MinecraftServer server = server();
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (!data.isActive() || data.getCurrentCard() == null) return;

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {

    TeamData teamData = TeamData.get(server);
    UUID teamId = resolveViewTeamId(player, data, teamData);
    if (teamId == null) {
        continue;
    }

    // 🔑 Pick the correct card for this player
    BingoCard cardToSend = data.getActiveCardForTeam(teamId);
    if (cardToSend == null) {
        cardToSend = data.getCurrentCard();
    }

    NetworkHandler.send(
            new PacketSyncCard(
                    cardToSend,
                    data.winCondition,
                    data.teamChestEnabled,
                    data.getGreenFakeIndicesForTeam(teamId),
                    data.getRedFakeIndicesForTeam(teamId)
            ),
            PacketDistributor.PLAYER.with(player)
    );

    syncProgress(player);
    syncRevealedSlots(player);
    syncHighlightedSlots(player);
    sendSlotOwnership(player, data);
    syncSettings(player);
    syncCustomEffectState(player, data);
    syncGameTimer(player);
}

        broadcastTeamScores(server);
    }

    /* =========================
       PLAYER JOIN
       ========================= */

    public static void syncPlayerJoin(ServerPlayer player) {
        BingoGameData data = BingoGameData.get(server());
        if (!data.isActive() || data.getCurrentCard() == null) return;

        TeamData teamData = TeamData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
UUID teamId = resolveViewTeamId(player, data, teamData);
if (teamId == null) return;

// 🔑 Pick the correct card for this player
BingoCard cardToSend = data.getActiveCardForTeam(teamId);
if (cardToSend == null) {
    cardToSend = data.getCurrentCard();
}

NetworkHandler.send(
        new PacketSyncCard(
                cardToSend,
                data.winCondition,
                data.teamChestEnabled,
                data.getGreenFakeIndicesForTeam(teamId),
                data.getRedFakeIndicesForTeam(teamId)
        ),
        PacketDistributor.PLAYER.with(player)
);

        syncProgress(player);
        syncRevealedSlots(player);
        syncHighlightedSlots(player);
        sendSlotOwnership(player, data);
        syncSettings(player);
        syncCustomEffectState(player, data);
        syncGameTimer(player);
        broadcastTeamScores(server());

        if (data.winCondition == WinCondition.HANGMAN) {
            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
            int remaining = Math.max(0, (data.hangmanIntermissionEndTick - com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server)) / 20);
            String line1 = data.hangmanIntermissionEndTick > 0
                    ? com.jamie.jamiebingo.bingo.HangmanTicker.buildRevealLine(data)
                    : com.jamie.jamiebingo.bingo.HangmanTicker.buildPromptLine(
                            data,
                            com.jamie.jamiebingo.bingo.HangmanTicker.secondsUntilNextReveal(server, data)
                    );
            String line2 = data.hangmanIntermissionEndTick > 0
                    ? "Next word in " + remaining + "s"
                    : data.hangmanMaskedWord;

            NetworkHandler.send(
                    new PacketSyncHangmanState(true, data.hangmanSlotRevealed, line1, line2),
                    PacketDistributor.PLAYER.with(player)
            );
        } else {
            NetworkHandler.send(
                    new PacketSyncHangmanState(false, true, "", ""),
                    PacketDistributor.PLAYER.with(player)
            );
        }
    }

    public static void syncCard(ServerPlayer player) {
    if (player == null) return;

    BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
    if (!data.isActive()) return;

    TeamData teamData = TeamData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
    UUID teamId = resolveViewTeamId(player, data, teamData);
    if (teamId == null) return;

    BingoCard cardToSend = data.getActiveCardForTeam(teamId);
    if (cardToSend == null) {
        cardToSend = data.getCurrentCard();
    }

    // 🔒 CRASH GUARD — THIS IS THE FIX
    if (cardToSend == null) {
        return;
    }

    NetworkHandler.send(
            new PacketSyncCard(
                    cardToSend,
                    data.winCondition,
                    data.teamChestEnabled,
                    data.getGreenFakeIndicesForTeam(teamId),
                    data.getRedFakeIndicesForTeam(teamId)
            ),
            PacketDistributor.PLAYER.with(player)
    );

    syncProgress(player);
    syncRevealedSlots(player);
    sendSlotOwnership(player, data);
    syncSettings(player);
    syncCustomEffectState(player, data);
}

    /* =========================
       PROGRESS
       ========================= */

   public static void syncProgress(ServerPlayer player) {
    if (player == null) return;

    BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
    if (!data.isActive()) return;

    TeamData teamData = TeamData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
    UUID teamId = resolveViewTeamId(player, data, teamData);
    if (teamId == null) return;
    UUID progressPlayerId = resolveProgressPlayerId(player, data);
    ServerPlayer progressPlayer = progressPlayerId == null
            ? player
            : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player), progressPlayerId);
    if (progressPlayer == null) {
        progressPlayer = player;
    }

    NetworkHandler.send(
            new PacketSyncProgress(data.getTeamProgressForDisplay(teamId)),
            PacketDistributor.PLAYER.with(player)
    );

    BingoCard card = data.getActiveCardForTeam(teamId);
    if (card == null) return;

    Map<String, Integer> questProgress = new HashMap<>();
    Map<String, Integer> questMax = new HashMap<>();

    for (int y = 0; y < card.getSize(); y++) {
        for (int x = 0; x < card.getSize(); x++) {
            BingoSlot slot = card.getSlot(x, y);
            if (slot == null) continue;
            String id = slot.getId();
            if (id == null || !id.startsWith("quest.")) continue;

            Integer max = QuestTracker.getQuestMax(id);
            if (max == null) continue;

            int progress = data.teamSyncEnabled
                    ? QuestTracker.getTeamProgress(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player), teamId, id)
                    : QuestTracker.getPlayerProgressForQuest(progressPlayer, id);
            questProgress.put(id, progress);
            questMax.put(id, max);
        }
    }

    NetworkHandler.send(
            new PacketSyncQuestProgress(questProgress, questMax),
            PacketDistributor.PLAYER.with(player)
    );
}

    private static UUID resolveProgressPlayerId(ServerPlayer player, BingoGameData data) {
        if (player == null || data == null) return null;
        UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
        if (!data.isSpectator(playerId)) {
            return playerId;
        }
        UUID targetId = data.getSpectatorViewTarget(playerId);
        return targetId != null ? targetId : playerId;
    }

    public static void broadcastProgress(MinecraftServer server) {
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (!data.isActive()) return;

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            syncProgress(player);
        }
    }

    /* =========================
       BLIND: REVEALED SLOTS
       ========================= */

    public static void syncRevealedSlots(ServerPlayer player) {
        if (player == null) return;

        BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        if (!data.isActive()) return;

    TeamData teamData = TeamData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
    UUID teamId = resolveViewTeamId(player, data, teamData);
    if (teamId == null) return;

        NetworkHandler.send(
                new PacketSyncRevealedSlots(data.getRevealedSlots(teamId)),
                PacketDistributor.PLAYER.with(player)
        );
    }

    /** ✅ NEW: broadcast revealed slots to ALL players (used after a BLIND reveal) */
    public static void broadcastRevealedSlots(MinecraftServer server) {
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (!data.isActive()) return;

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            syncRevealedSlots(player);
        }
    }

    /* =========================
       HIGHLIGHTED SLOTS
       ========================= */

    public static void syncHighlightedSlots(ServerPlayer player) {
        if (player == null) return;

        BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        if (!data.isActive()) return;

        TeamData teamData = TeamData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        UUID teamId = resolveViewTeamId(player, data, teamData);
        if (teamId == null) return;

        NetworkHandler.send(
                new PacketSyncHighlightedSlots(data.getHighlightedSlots(teamId)),
                PacketDistributor.PLAYER.with(player)
        );
    }

    public static void broadcastHighlightedSlots(MinecraftServer server) {
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (!data.isActive()) return;

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            syncHighlightedSlots(player);
        }
    }

    /* =========================
       TEAM SCORES
       ========================= */

    public static void broadcastTeamScores(MinecraftServer server) {
        if (server == null) return;

        TeamData teamData = TeamData.get(server);
        TeamScoreData scoreData = TeamScoreData.get(server);
        BingoGameData gameData = BingoGameData.get(server);

        List<PacketSyncTeamScores.TeamPayload> payloads = new ArrayList<>();

        for (TeamData.TeamInfo team : teamData.getTeams()) {
            if (team.members.isEmpty()) continue;

            UUID teamId = team.id;
            Map<UUID, Integer> memberScores = new HashMap<>(scoreData.getTeamContributions(teamId));
            for (UUID member : team.members) {
                memberScores.putIfAbsent(member, 0);
            }

            Map<UUID, String> memberNames = new HashMap<>();

            for (UUID playerId : memberScores.keySet()) {
                ServerPlayer online = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, playerId);
                if (online != null) {
                    memberNames.put(playerId, online.getGameProfile().name());
                } else {
                    String name = resolveName(server, playerId);
                    if (name != null) {
                        memberNames.put(playerId, name);
                    }
                }
            }

            payloads.add(new PacketSyncTeamScores.TeamPayload(
                    teamId,
                    team.color,
                    scoreData.getTeamScore(teamId),
                    gameData.getCompletedLineCount(teamId),
                    memberScores,
                    memberNames
            ));
        }

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            NetworkHandler.send(
                    new PacketSyncTeamScores(payloads),
                    PacketDistributor.PLAYER.with(player)
            );
        }
    }

    /* =========================
       SLOT OWNERSHIP
       ========================= */

    public static void broadcastSlotOwnership(MinecraftServer server) {
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (!data.isActive()) return;

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            sendSlotOwnership(player, data);
        }
    }

    private static String resolveName(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return null;
        try {
            Object cache = null;
            try {
                var m = server.getClass().getMethod("getProfileCache");
                cache = m.invoke(server);
            } catch (Throwable ignored) {
            }
            if (cache == null) return null;

            Object profile = null;
            try {
                var m = cache.getClass().getMethod("get", UUID.class);
                Object out = m.invoke(cache, playerId);
                if (out instanceof java.util.Optional<?> opt) {
                    profile = opt.orElse(null);
                }
            } catch (Throwable ignored) {
            }
            if (profile == null) {
                try {
                    var m = cache.getClass().getMethod("getProfile", UUID.class);
                    Object out = m.invoke(cache, playerId);
                    if (out instanceof java.util.Optional<?> opt) {
                        profile = opt.orElse(null);
                    }
                } catch (Throwable ignored) {
                }
            }

            if (profile == null) return null;
            try {
                var m = profile.getClass().getMethod("getName");
                Object name = m.invoke(profile);
                if (name instanceof String s) return s;
            } catch (Throwable ignored) {
            }
            try {
                var f = profile.getClass().getDeclaredField("name");
                f.setAccessible(true);
                Object name = f.get(profile);
                if (name instanceof String s) return s;
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void sendSlotOwnership(ServerPlayer player, BingoGameData data) {
        TeamData teamData = TeamData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        UUID ownTeam = resolveViewTeamId(player, data, teamData);
        if (ownTeam == null) return;

        Map<String, DyeColor> visible = new HashMap<>();

        /* ---------- LOCKOUT & RARITY ---------- */
        if (data.winCondition == WinCondition.LOCKOUT
                || data.winCondition == WinCondition.RARITY) {

            for (Map.Entry<String, Set<UUID>> entry : data.getSlotOwners().entrySet()) {
                String slotId = entry.getKey();

                for (UUID teamId : entry.getValue()) {
                    TeamData.TeamInfo team = teamData.getTeams().stream()
                            .filter(t -> t.id.equals(teamId))
                            .findFirst()
                            .orElse(null);

                    if (team != null) {
                        visible.put(slotId, team.color);
                    }
                }
            }

            NetworkHandler.send(
                    new PacketSyncSlotOwnership(visible),
                    PacketDistributor.PLAYER.with(player)
            );
            return;
        }

        /* ---------- LINE, FULL, BLIND ---------- */
        TeamData.TeamInfo ownInfo = teamData.getTeams().stream()
                .filter(t -> t.id.equals(ownTeam))
                .findFirst()
                .orElse(null);

        if (ownInfo != null) {
           Set<String> completed = data.getTeamProgressForDisplay(ownTeam);
for (String slotId : completed) {
    visible.put(slotId, ownInfo.color);
}
        }

        NetworkHandler.send(
                new PacketSyncSlotOwnership(visible),
                PacketDistributor.PLAYER.with(player)
        );
    }

    /* =========================
       SETTINGS OVERLAY
       ========================= */

    public static void syncSettings(ServerPlayer player) {
        if (player == null) return;

        BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        if (!data.isActive()) return;

        NetworkHandler.send(
                new PacketSyncSettingsOverlay(data.buildSettingsLines(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player))),
                PacketDistributor.PLAYER.with(player)
        );
    }

    public static void broadcastSettingsOverlay(MinecraftServer server) {
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (!data.isActive()) return;

        List<String> lines = data.buildSettingsLines(server);

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            NetworkHandler.send(
                    new PacketSyncSettingsOverlay(lines),
                    PacketDistributor.PLAYER.with(player)
            );
        }
    }

    public static void syncCustomEffectState(ServerPlayer player, BingoGameData data) {
        if (player == null || data == null) return;
        if (data.isSpectator(com.jamie.jamiebingo.util.EntityUtil.getUUID(player))) {
            NetworkHandler.send(
                    new PacketSyncCustomEffectState(""),
                    PacketDistributor.PLAYER.with(player)
            );
            return;
        }
        NetworkHandler.send(
                new PacketSyncCustomEffectState(data.appliedCustomEffectId),
                PacketDistributor.PLAYER.with(player)
        );
    }

    public static void syncGameTimer(ServerPlayer player) {
        if (player == null) return;
        BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        if (data == null || !data.isActive() || data.startCountdownActive) {
            NetworkHandler.send(
                    new PacketSyncGameTimer(false, false, 0, false, 0),
                    PacketDistributor.PLAYER.with(player)
            );
            return;
        }
        boolean preMatchHold = data.gameStartTick < 0
                && (data.pregameBoxActive
                || data.isRerollPhaseActive()
                || com.jamie.jamiebingo.casino.CasinoModeManager.isCasinoInProgress());
        if (preMatchHold) {
            NetworkHandler.send(
                    new PacketSyncGameTimer(false, false, 0, false, 0),
                    PacketDistributor.PLAYER.with(player)
            );
            return;
        }

        data.restoreTimerBaselineIfNeeded(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        data.restoreRushDeadlinesIfNeeded(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        int seconds = data.getTimerSeconds(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        int rushSeconds = data.getRushSecondsForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
        NetworkHandler.send(
                new PacketSyncGameTimer(true, data.countdownEnabled, seconds, rushSeconds >= 0, Math.max(0, rushSeconds)),
                PacketDistributor.PLAYER.with(player)
        );
    }

    public static void broadcastGameTimer(MinecraftServer server, boolean active, boolean countdown, int seconds) {
        if (server == null) return;
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            NetworkHandler.send(
                    new PacketSyncGameTimer(active, countdown, seconds, false, 0),
                    PacketDistributor.PLAYER.with(player)
            );
        }
    }

    public static void broadcastHangmanState(
            MinecraftServer server,
            boolean active,
            boolean slotRevealed,
            String line1,
            String line2
    ) {
        if (server == null) return;
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
        NetworkHandler.send(
                    new PacketSyncHangmanState(active, slotRevealed, line1, line2),
                    PacketDistributor.PLAYER.with(player)
            );
        }
    }

    public static void broadcastCustomEffectState(MinecraftServer server, BingoGameData data) {
        if (server == null || data == null) return;
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            syncCustomEffectState(player, data);
        }
    }

       /* =========================
       WINNING LINE
       ========================= */

    public static void broadcastWinningLine(
            MinecraftServer server,
            BingoLineType type,
            int index,
            DyeColor color
    ) {
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (data == null || data.getCurrentCard() == null) return;

        PacketSyncWinningLine pkt =
                new PacketSyncWinningLine(
                        type,
                        index,
                        color,
                        data.getCurrentCard().getSize()
                );

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            NetworkHandler.send(
                    pkt,
                    PacketDistributor.PLAYER.with(player)
            );
        }
    }

      /* =========================
       RANDOM EFFECTS (ADDON)
       ========================= */

    /**
     * Sends a countdown message to all players.
     * Purely visual – server controls timing.
     */
    public static void broadcastEffectCountdown(
            MinecraftServer server,
            String effectName,
            int amplifier,
            int secondsRemaining,
            boolean playSound
    ) {
        if (server == null) return;

        if (effectName == null || effectName.isBlank()) {
            effectName = "Random Effect";
        }

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            BingoGameData data = BingoGameData.get(server);
            if (data != null && data.isSpectator(com.jamie.jamiebingo.util.EntityUtil.getUUID(player))) {
                continue;
            }
            NetworkHandler.send(
                    new PacketEffectCountdown(effectName, amplifier, secondsRemaining, playSound),
                    PacketDistributor.PLAYER.with(player)
            );
        }
    } // ✅ METHOD CLOSED CORRECTLY

    public static void broadcastStartCountdown(MinecraftServer server, int seconds) {
        if (server == null) return;

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            NetworkHandler.send(
                    new PacketStartCountdown(seconds),
                    PacketDistributor.PLAYER.with(player)
            );
        }
    }


    /**
     * Notifies all players that a new effect has been applied.
     * (Client adds the "Effect applied:" text to avoid duplication)
     */
    public static void broadcastEffectApplied(
            MinecraftServer server,
            String effectName,
            int amplifier
    ) {
        if (server == null) return;

        if (effectName == null || effectName.isBlank()) {
            effectName = "Random Effect";
        }

        // Only send raw effect name + level
        MutableComponent msg =
                com.jamie.jamiebingo.util.ComponentUtil.literal(effectName)
                        .withStyle(style -> style.withBold(true))
                        .append(com.jamie.jamiebingo.util.ComponentUtil.literal(" (Level " + (amplifier + 1) + ")"));

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            BingoGameData data = BingoGameData.get(server);
            if (data != null && data.isSpectator(com.jamie.jamiebingo.util.EntityUtil.getUUID(player))) {
                continue;
            }
            NetworkHandler.send(
                    new PacketEffectApplied(msg),
                    PacketDistributor.PLAYER.with(player)
            );
        }
    }

} // ✅ CLASS CLOSED







