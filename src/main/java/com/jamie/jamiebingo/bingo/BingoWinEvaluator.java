package com.jamie.jamiebingo.bingo;

import com.jamie.jamiebingo.data.*;
import com.jamie.jamiebingo.util.FireworkHelper;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import java.util.*;

public final class BingoWinEvaluator {

    private static boolean gameEnding = false;

    private BingoWinEvaluator() {}

    public static void resetGameEnding() {
        gameEnding = false;
    }

   public static void onProgress(MinecraftServer server, UUID scoringTeam) {
    if (gameEnding) return;

    BingoGameData data = BingoGameData.get(server);
    if (!data.isActive() || data.getCurrentCard() == null) return;
    if (data.hardcoreEnabled && data.isTeamEliminated(scoringTeam)) return;

    switch (data.winCondition) {
        case LINE -> checkLine(server, data, scoringTeam);
        case FULL -> checkFull(server, data, scoringTeam);
        case BLIND -> checkFull(server, data, scoringTeam);
        case LOCKOUT -> checkLockout(server, data);
        case RARITY -> checkRarity(server, data);
        case HANGMAN -> { }
        case GAMEGUN -> checkGameGunFinal(server, data, scoringTeam);
        case GUNGAME -> checkGunGameFinal(server, data, scoringTeam);
    }
}

private static void checkGameGunFinal(
        MinecraftServer server,
        BingoGameData data,
        UUID scoringTeam
) {
    // not on final card yet
    if (data.gunGameSharedIndex < data.gunGameLength - 1) return;

    // 🏆 any completion on final card wins
    handleWin(server, data, scoringTeam, null);
}

private static void checkGunGameFinal(
        MinecraftServer server,
        BingoGameData data,
        UUID scoringTeam
) {
    int idx = data.getGunGameTeamIndex(scoringTeam);

    // not on final card yet
    if (idx < data.gunGameLength - 1) return;

    // 🏆 any slot on final card = win
    handleWin(server, data, scoringTeam, null);
}

    /* =========================
       RARITY MODE
       ========================= */

    private static void checkRarity(MinecraftServer server, BingoGameData data) {

        TeamData teamData = TeamData.get(server);
        TeamScoreData scores = TeamScoreData.get(server);

        scores.recomputeRarityScores(server, data);
        BroadcastHelper.broadcastTeamScores(server);

        int teamsWithProgress = 0;

        for (TeamData.TeamInfo team : teamData.getTeams()) {
            if (team.members.isEmpty()) continue;
            if (data.hardcoreEnabled && data.isTeamEliminated(team.id)) continue;
            UUID any = team.members.iterator().next();
            if (!data.getPlayerProgress(any).isEmpty()) teamsWithProgress++;
        }

        if (teamsWithProgress < 2) return;

        UUID leader = null;
        int leaderScore = -1;

        for (TeamData.TeamInfo team : teamData.getTeams()) {
            if (team.members.isEmpty()) continue;
            if (data.hardcoreEnabled && data.isTeamEliminated(team.id)) continue;
            int s = scores.getTeamScore(team.id);
            if (s > leaderScore) {
                leaderScore = s;
                leader = team.id;
            }
        }

        if (leader == null) return;

        for (TeamData.TeamInfo other : teamData.getTeams()) {
            if (other.id.equals(leader) || other.members.isEmpty()) continue;
            if (data.hardcoreEnabled && data.isTeamEliminated(other.id)) continue;
            int possible = maxRemainingPoints(server, data, other.id);
            if (scores.getTeamScore(other.id) + possible >= leaderScore) return;
        }

        handleWin(server, data, leader, findLine(server, data, leader));
    }

    /* =========================
       MAX REMAINING POINTS
       ========================= */

    private static int maxRemainingPoints(
            MinecraftServer server,
            BingoGameData data,
            UUID teamId
    ) {
        TeamData teamData = TeamData.get(server);

        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);

        if (team == null || team.members.isEmpty()) return 0;

        UUID any = team.members.iterator().next();
        Set<String> completed = data.getTeamProgressForDisplay(team.id);
        Map<String, DyeColor> ownership = data.getSlotOwnershipSnapshot();

        BingoCard card = data.getActiveCardForTeam(teamId);
        if (card == null) return 0;
        int size = card.getSize();
        int max = 0;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot == null) continue;
                String id = slot.getId();

                if (completed.contains(id)) continue;

                DyeColor owner = ownership.get(id);
                if (owner != null && owner != team.color) continue;

                int base = RarityScoreCalculator.base(slot);

                if (slotCouldCompleteLine(card, completed, ownership, team.color, x, y))
                    max += base * 2;
                else
                    max += base;
            }
        }
        return max;
    }

    /* =========================
       LINE POSSIBILITY CHECK
       ========================= */

    private static boolean slotCouldCompleteLine(
            BingoCard card,
            Set<String> completed,
            Map<String, DyeColor> ownership,
            DyeColor teamColor,
            int sx,
            int sy
    ) {
        int size = card.getSize();

        boolean ok = true;
        for (int x = 0; x < size; x++) {
            BingoSlot slot = card.getSlot(x, sy);
            if (slot == null) {
                ok = false;
                break;
            }
            String id = slot.getId();
            DyeColor o = ownership.get(id);
            if (o != null && o != teamColor && !completed.contains(id)) {
                ok = false;
                break;
            }
        }
        if (ok) return true;

        ok = true;
        for (int y = 0; y < size; y++) {
            BingoSlot slot = card.getSlot(sx, y);
            if (slot == null) {
                ok = false;
                break;
            }
            String id = slot.getId();
            DyeColor o = ownership.get(id);
            if (o != null && o != teamColor && !completed.contains(id)) {
                ok = false;
                break;
            }
        }
        if (ok) return true;

        if (sx == sy) {
            ok = true;
            for (int i = 0; i < size; i++) {
                BingoSlot slot = card.getSlot(i, i);
                if (slot == null) {
                    ok = false;
                    break;
                }
                String id = slot.getId();
                DyeColor o = ownership.get(id);
                if (o != null && o != teamColor && !completed.contains(id)) {
                    ok = false;
                    break;
                }
            }
            if (ok) return true;
        }

        if (sx + sy == size - 1) {
            ok = true;
            for (int i = 0; i < size; i++) {
                BingoSlot slot = card.getSlot(size - 1 - i, i);
                if (slot == null) {
                    ok = false;
                    break;
                }
                String id = slot.getId();
                DyeColor o = ownership.get(id);
                if (o != null && o != teamColor && !completed.contains(id)) {
                    ok = false;
                    break;
                }
            }
            if (ok) return true;
        }

        return false;
    }

    /* =========================
       EXISTING MODES
       ========================= */

    private static void checkLine(MinecraftServer server, BingoGameData data, UUID teamId) {
        LineResult line = findLine(server, data, teamId);
        if (line != null) handleWin(server, data, teamId, line);
    }

    private static void checkFull(MinecraftServer server, BingoGameData data, UUID teamId) {
        TeamData.TeamInfo team = TeamData.get(server).getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);

        if (team == null || team.members.isEmpty()) return;

        UUID any = team.members.iterator().next();
        BingoCard card = data.getActiveCardForTeam(teamId);
        if (card == null) return;
        if (data.getPlayerProgress(any).size()
                < card.getSize() * card.getSize())
            return;

        handleWin(server, data, teamId, null);
    }

    private static void checkLockout(MinecraftServer server, BingoGameData data) {
        TeamScoreData scores = TeamScoreData.get(server);
        TeamData teams = TeamData.get(server);

        int totalSlots =
                data.getCurrentCard().getSize() *
                        data.getCurrentCard().getSize();

        int taken = teams.getTeams().stream()
                .filter(t -> !t.members.isEmpty())
                .filter(t -> !data.hardcoreEnabled || !data.isTeamEliminated(t.id))
                .mapToInt(t -> scores.getTeamScore(t.id))
                .sum();

        int remaining = totalSlots - taken;

        UUID leader = null;
        int best = -1;

        for (TeamData.TeamInfo team : teams.getTeams()) {
            if (team.members.isEmpty()) continue;
            if (data.hardcoreEnabled && data.isTeamEliminated(team.id)) continue;
            int s = scores.getTeamScore(team.id);
            if (s > best) {
                best = s;
                leader = team.id;
            }
        }

        if (leader == null) return;

        for (TeamData.TeamInfo other : teams.getTeams()) {
            if (other.id.equals(leader) || other.members.isEmpty()) continue;
            if (data.hardcoreEnabled && data.isTeamEliminated(other.id)) continue;
            if (scores.getTeamScore(other.id) + remaining >= best) return;
        }

        handleWin(server, data, leader, findLine(server, data, leader));
    }

    /* =========================
       LINE DETECTION
       ========================= */

    private static LineResult findLine(
            MinecraftServer server,
            BingoGameData data,
            UUID teamId
    ) {
        BingoCard card = data.getActiveCardForTeam(teamId);
        if (card == null) return null;

        TeamData.TeamInfo team = TeamData.get(server).getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);

        if (team == null || team.members.isEmpty()) return null;

        UUID any = team.members.iterator().next();
        Set<String> completed = data.getTeamProgressForDisplay(team.id);

        int size = card.getSize();

        for (int y = 0; y < size; y++)
            if (rowComplete(card, completed, y))
                return new LineResult(BingoLineType.ROW, y);

        for (int x = 0; x < size; x++)
            if (columnComplete(card, completed, x))
                return new LineResult(BingoLineType.COLUMN, x);

        if (diagonalMain(card, completed))
            return new LineResult(BingoLineType.DIAGONAL_MAIN, 0);

        if (diagonalAnti(card, completed))
            return new LineResult(BingoLineType.DIAGONAL_ANTI, 0);

        return null;
    }

    private static boolean rowComplete(BingoCard c, Set<String> done, int y) {
        for (int x = 0; x < c.getSize(); x++) {
            BingoSlot slot = c.getSlot(x, y);
            if (slot == null) return false;
            if (!done.contains(slot.getId())) return false;
        }
        return true;
    }

    private static boolean columnComplete(BingoCard c, Set<String> done, int x) {
        for (int y = 0; y < c.getSize(); y++) {
            BingoSlot slot = c.getSlot(x, y);
            if (slot == null) return false;
            if (!done.contains(slot.getId())) return false;
        }
        return true;
    }

    private static boolean diagonalMain(BingoCard c, Set<String> done) {
        for (int i = 0; i < c.getSize(); i++) {
            BingoSlot slot = c.getSlot(i, i);
            if (slot == null) return false;
            if (!done.contains(slot.getId())) return false;
        }
        return true;
    }

    private static boolean diagonalAnti(BingoCard c, Set<String> done) {
        for (int i = 0; i < c.getSize(); i++) {
            BingoSlot slot = c.getSlot(c.getSize() - 1 - i, i);
            if (slot == null) return false;
            if (!done.contains(slot.getId())) return false;
        }
        return true;
    }

    /* =========================
       WIN HANDLING
       ========================= */

    private static void handleWin(
            MinecraftServer server,
            BingoGameData data,
            UUID teamId,
            LineResult line
    ) {
        if (gameEnding) return;
        gameEnding = true;

        TeamData.TeamInfo team = TeamData.get(server).getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);

        if (team == null) return;

        int endDelayTicks = 20 * 10;
        if (data.winCondition == WinCondition.BLIND) {
            data.revealAllBlindSlots(server);
            BroadcastHelper.broadcastRevealedSlots(server);
            BroadcastHelper.broadcastFullSync();
            endDelayTicks = 20 * 20;
        }

        if (line != null) {
            BroadcastHelper.broadcastWinningLine(
                    server,
                    line.type,
                    line.index,
                    team.color
            );
        }

        String teamName = team.color.getName();
        List<String> winners = getTeamMemberNames(server, team);
        String winnersList = winners.isEmpty() ? "no players" : String.join(", ", winners);

        BroadcastHelper.broadcast(
                server,
                com.jamie.jamiebingo.util.ComponentUtil.literal("Team " + teamName + " wins!")
        );

        sendWinTitle(server,
                com.jamie.jamiebingo.util.ComponentUtil.literal("Team " + teamName + " wins!!"),
                com.jamie.jamiebingo.util.ComponentUtil.literal("congratulations " + winnersList),
                10,
                200,
                20
        );

        for (UUID member : team.members) {
            ServerPlayer player = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, member);
            if (player != null) {
                FireworkHelper.launchWinCelebration(player);
            }
        }

        data.scheduleWinEnd(server, endDelayTicks);
    }

    private static void handleTie(
            MinecraftServer server,
            BingoGameData data,
            List<UUID> teamIds
    ) {
        if (gameEnding) return;
        gameEnding = true;

        TeamData teamData = TeamData.get(server);

        MutableComponent msg = com.jamie.jamiebingo.util.ComponentUtil.literal("It's a tie between: ");

        for (int i = 0; i < teamIds.size(); i++) {
            UUID teamId = teamIds.get(i);

            TeamData.TeamInfo team = teamData.getTeams().stream()
                    .filter(t -> t.id.equals(teamId))
                    .findFirst()
                    .orElse(null);

            if (team == null) continue;

            msg.append(com.jamie.jamiebingo.util.ComponentUtil.literal(team.color.getName()));
            if (i < teamIds.size() - 1) msg.append(com.jamie.jamiebingo.util.ComponentUtil.literal(", "));
        }

        BroadcastHelper.broadcast(server, msg);

        int endDelayTicks = 20 * 10;
        if (data.winCondition == WinCondition.BLIND) {
            data.revealAllBlindSlots(server);
            BroadcastHelper.broadcastRevealedSlots(server);
            BroadcastHelper.broadcastFullSync();
            endDelayTicks = 20 * 20;
        }

        data.scheduleWinEnd(server, endDelayTicks);
    }

    /* =========================
       BLIND MODE FORCE WIN
       ========================= */

    public static void forceBlindWin(MinecraftServer server, UUID teamId) {
        if (gameEnding) return;

        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isActive()) return;

        handleWin(server, data, teamId, null);
    }

    public static void forcePointsWin(MinecraftServer server) {
        if (gameEnding) return;

        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isActive()) return;

        TeamScoreData scores = TeamScoreData.get(server);
        if (data.winCondition == WinCondition.RARITY) {
            scores.recomputeRarityScores(server, data);
        } else {
            scores.recomputeStandardScores(server, data);
        }
        BroadcastHelper.broadcastTeamScores(server);

        TeamData teamData = TeamData.get(server);
        int best = Integer.MIN_VALUE;
        java.util.List<java.util.UUID> leaders = new java.util.ArrayList<>();

        for (TeamData.TeamInfo team : teamData.getTeams()) {
            if (team.members.isEmpty()) continue;
            if (data.hardcoreEnabled && data.isTeamEliminated(team.id)) continue;

            int score = scores.getTeamScore(team.id);
            if (score > best) {
                best = score;
                leaders.clear();
                leaders.add(team.id);
            } else if (score == best) {
                leaders.add(team.id);
            }
        }

        if (leaders.isEmpty()) return;
        if (leaders.size() == 1) {
            handleWin(server, data, leaders.get(0), null);
        } else {
            handleTie(server, data, leaders);
        }
    }

    public static void forceEliminationWin(MinecraftServer server, UUID teamId) {
        if (gameEnding) return;

        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isActive()) return;

        handleWin(server, data, teamId, null);
    }

    private static List<String> getTeamMemberNames(MinecraftServer server, TeamData.TeamInfo team) {
        List<String> names = new ArrayList<>();
        if (server == null || team == null) return names;
        for (UUID member : team.members) {
            ServerPlayer player = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, member);
            if (player != null) {
                names.add(player.getGameProfile().name());
            } else {
                names.add("Unknown");
            }
        }
        return names;
    }

    private static void sendWinTitle(
            MinecraftServer server,
            Component title,
            Component subtitle,
            int fadeIn,
            int stay,
            int fadeOut
    ) {
        if (server == null) return;
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        }
    }

    private record LineResult(BingoLineType type, int index) {}
}

