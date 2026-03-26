package com.jamie.jamiebingo.data;

import com.jamie.jamiebingo.quest.QuestTracker;
import com.jamie.jamiebingo.util.EntityServerUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.event.ServerChatEvent;

/**
 * Chat is no longer intercepted.
 * Vanilla Minecraft global chat is preserved to avoid
 * signed chat breakage on dedicated servers.
 */
public class GlobalChatHandler {

    public static boolean onServerChat(ServerChatEvent event) {
        if (event == null) return false;
        ServerPlayer player = event.getPlayer();
        if (player == null) return false;
        String message = event.getRawText();
        if (message == null) return false;

        var server = EntityServerUtil.getServer(player);
        if (server == null) return false;

        // Preserve quest completion hook for "gg".
        if ("gg".equalsIgnoreCase(message.trim())) {
            BingoGameData data = BingoGameData.get(server);
            if (data != null && data.isActive() && !data.startCountdownActive && !data.pregameBoxActive) {
                String questId = "quest.say_gg_in_chat_when_this_is_the_last_remaining_slot";
                if (data.cardContainsForPlayer(player.getUUID(), questId)) {
                    var card = data.getCurrentCard();
                    if (card != null) {
                        int remaining = 0;
                        String lastId = null;
                        var progress = data.getPlayerProgress(player.getUUID());
                        for (int y = 0; y < card.getSize(); y++) {
                            for (int x = 0; x < card.getSize(); x++) {
                                var slot = card.getSlot(x, y);
                                if (slot == null || slot.getId() == null || slot.getId().isBlank()) continue;
                                if (progress.contains(slot.getId())) continue;
                                remaining++;
                                lastId = slot.getId();
                            }
                        }
                        if (remaining == 1 && questId.equals(lastId)) {
                            QuestTracker.complete(player, questId);
                        }
                    }
                }
            }
        }

        // Replace vanilla global chat line with a team-prefixed line.
        TeamData teamData = TeamData.get(server);
        DyeColor teamColor = resolveTeamColor(teamData, player.getUUID());
        if (teamColor != null) {
            int colorRgb = teamColor.getTextColor();
            String colorName = capitalize(teamColor.getName());

            Component prefix = com.jamie.jamiebingo.util.ComponentUtil.literal("[" + colorName + "] ")
                    .copy()
                    .withStyle(style -> style.withColor(colorRgb));
            Component name = player.getDisplayName().copy().withStyle(style -> style.withColor(colorRgb));
            Component payload = com.jamie.jamiebingo.util.ComponentUtil.literal(message);
            Component line = Component.empty()
                    .append(prefix)
                    .append(name)
                    .append(com.jamie.jamiebingo.util.ComponentUtil.literal(": "))
                    .append(payload);
            BroadcastHelper.broadcast(server, line);
            return true;
        }
        return false;
    }

    private static DyeColor resolveTeamColor(TeamData data, java.util.UUID playerId) {
        if (data == null || playerId == null) return null;
        java.util.UUID teamId = data.getTeamForPlayer(playerId);
        if (teamId == null) return null;
        for (TeamData.TeamInfo info : data.getTeams()) {
            if (info == null || info.id == null) continue;
            if (info.id.equals(teamId)) return info.color;
        }
        return null;
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) return "Team";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }

}
