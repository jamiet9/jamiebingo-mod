package com.jamie.jamiebingo.bingo;

import com.jamie.jamiebingo.JamieBingo;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.data.BroadcastHelper;
import com.jamie.jamiebingo.data.TeamData;
import com.jamie.jamiebingo.data.TeamScoreData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID)
public final class HangmanTicker {

    private static final Random RNG = new Random();
    private static final int INTERMISSION_SECONDS = 5;

    private HangmanTicker() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (!data.isActive()) return;
        if (data.startCountdownActive) return;
        if (data.winCondition != WinCondition.HANGMAN) return;
        if (data.getCurrentCard() == null) return;

        int tick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);

        if (data.hangmanIntermissionEndTick > 0) {
            int remaining = Math.max(0, (data.hangmanIntermissionEndTick - tick) / 20);
            if (tick % 20 == 0) {
                String line1 = buildRevealLine(data);
                String line2 = "Next word in " + remaining + "s";
                BroadcastHelper.broadcastHangmanState(server, true, true, line1, line2);
            }
            if (remaining <= 0) {
                advanceRound(server, data);
            }
            return;
        }

        if (data.hangmanCurrentSlotId.isEmpty()) {
            initRound(server, data);
            return;
        }

        if (data.hangmanNextRevealTick > 0 && tick >= data.hangmanNextRevealTick) {
            revealNextLetter(server, data);
            return;
        }

        if (tick % 20 == 0) {
            int remaining = secondsUntilNextReveal(server, data);
            String line1 = buildPromptLine(data, remaining);
            String line2 = data.hangmanMaskedWord;
            BroadcastHelper.broadcastHangmanState(server, true, data.hangmanSlotRevealed, line1, line2);
        }
    }

    public static void startIntermission(MinecraftServer server, BingoGameData data) {
        if (server == null || data == null) return;
        data.hangmanIntermissionEndTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + INTERMISSION_SECONDS * 20;
        data.hangmanSlotRevealed = true;
        BroadcastHelper.broadcastHangmanState(server, true, true, buildRevealLine(data), "Next word in " + INTERMISSION_SECONDS + "s");
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
    }

    private static void initRound(MinecraftServer server, BingoGameData data) {
        if (data.hangmanCards.isEmpty()) return;
        if (data.hangmanRoundIndex >= data.hangmanCards.size()) {
            BingoWinEvaluator.forcePointsWin(server);
            return;
        }

        BingoCard card = data.hangmanCards.get(data.hangmanRoundIndex);
        data.currentCard = card;

        BingoSlot slot = card.getSlot(0, 0);
        if (slot == null) return;

        data.hangmanCurrentSlotId = slot.getId();
        data.hangmanCurrentWord = slot.getName();
        data.hangmanMaskedWord = revealOneLetter(maskWord(slot.getName()), slot.getName());
        data.hangmanRevealCount = 0;
        data.hangmanSlotRevealed = !data.hangmanMaskedWord.contains("_");
        data.hangmanNextRevealTick = data.hangmanSlotRevealed
                ? -1
                : com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + data.hangmanBaseSeconds * 20;

        if (data.handleImmediateHangmanTie(server, slot.getId())) {
            return;
        }

        String line1 = buildPromptLine(data, data.hangmanBaseSeconds);
        String line2 = data.hangmanMaskedWord;

        BroadcastHelper.broadcastHangmanState(server, true, false, line1, line2);
        BroadcastHelper.broadcastFullSync();
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
    }

    private static void revealNextLetter(MinecraftServer server, BingoGameData data) {
        String word = data.hangmanCurrentWord;
        String masked = data.hangmanMaskedWord;

        Set<Character> candidates = new HashSet<>();
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            if (Character.isLetterOrDigit(ch) && masked.charAt(i) == '_') {
                candidates.add(Character.toLowerCase(ch));
            }
        }

        if (candidates.isEmpty()) {
            data.hangmanSlotRevealed = true;
            data.hangmanNextRevealTick = -1;
            BroadcastHelper.broadcastHangmanState(server, true, true,
                    buildPromptLine(data, 0), data.hangmanMaskedWord);
            return;
        }

        char[] letters = new char[candidates.size()];
        int idx = 0;
        for (char c : candidates) letters[idx++] = c;
        char chosen = letters[RNG.nextInt(letters.length)];

        StringBuilder next = new StringBuilder(masked);
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            if (Character.toLowerCase(ch) == chosen) {
                next.setCharAt(i, word.charAt(i));
            }
        }

        data.hangmanMaskedWord = next.toString();
        data.hangmanRevealCount++;

        if (!data.hangmanMaskedWord.contains("_")) {
            data.hangmanSlotRevealed = true;
            data.hangmanNextRevealTick = -1;
        } else {
            int penalty = Math.max(0, data.hangmanPenaltySeconds);
            int nextDelay = data.hangmanBaseSeconds + data.hangmanRevealCount * penalty;
            data.hangmanNextRevealTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + nextDelay * 20;
        }

        BroadcastHelper.broadcastHangmanState(server, true, data.hangmanSlotRevealed,
                buildPromptLine(data, secondsUntilNextReveal(server, data)), data.hangmanMaskedWord);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
    }

    private static void advanceRound(MinecraftServer server, BingoGameData data) {
        data.hangmanRoundIndex++;
        data.hangmanIntermissionEndTick = -1;
        data.hangmanCurrentSlotId = "";
        data.hangmanCurrentWord = "";
        data.hangmanMaskedWord = "";
        data.hangmanRevealCount = 0;
        data.hangmanNextRevealTick = -1;
        data.hangmanSlotRevealed = false;

        data.resetProgressForNewCard();

        int remainingRounds = Math.max(0, data.hangmanRounds - data.hangmanRoundIndex);
        if (remainingRounds <= 0) {
            BingoWinEvaluator.forcePointsWin(server);
            return;
        }

        initRound(server, data);
    }

    public static String buildPromptLine(BingoGameData data, int secondsUntilNext) {
        if (data.hangmanCurrentSlotId.startsWith("quest.")) {
            return "Complete quest | Next letter in " + Math.max(0, secondsUntilNext) + "s";
        }
        return "Find item | Next letter in " + Math.max(0, secondsUntilNext) + "s";
    }

    public static String buildRevealLine(BingoGameData data) {
        String prefix = data.hangmanCurrentSlotId.startsWith("quest.")
                ? "Quest was "
                : "Item was ";
        return prefix + data.hangmanCurrentWord + "!";
    }

    private static String maskWord(String word) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            if (ch == ' ') {
                sb.append(' ');
            } else if (Character.isLetterOrDigit(ch)) {
                sb.append('_');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String revealOneLetter(String maskedWord, String fullWord) {
        if (maskedWord == null || fullWord == null) return maskedWord == null ? "" : maskedWord;
        List<Character> candidates = new java.util.ArrayList<>();
        for (int i = 0; i < fullWord.length(); i++) {
            char full = fullWord.charAt(i);
            if (i < maskedWord.length() && maskedWord.charAt(i) == '_' && Character.isLetterOrDigit(full)) {
                char lower = Character.toLowerCase(full);
                if (!candidates.contains(lower)) {
                    candidates.add(lower);
                }
            }
        }
        if (candidates.isEmpty()) return maskedWord;

        char chosen = candidates.get(RNG.nextInt(candidates.size()));
        StringBuilder next = new StringBuilder(maskedWord);
        for (int i = 0; i < fullWord.length() && i < next.length(); i++) {
            char full = fullWord.charAt(i);
            if (Character.toLowerCase(full) == chosen) {
                next.setCharAt(i, full);
            }
        }
        return next.toString();
    }

    public static int secondsUntilNextReveal(MinecraftServer server, BingoGameData data) {
        if (server == null || data == null) return 0;
        if (data.hangmanNextRevealTick <= 0) return 0;
        int remaining = (data.hangmanNextRevealTick - com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server)) / 20;
        return Math.max(0, remaining);
    }

    public static void checkEarlyWin(MinecraftServer server, BingoGameData data) {
        TeamScoreData scores = TeamScoreData.get(server);
        TeamData teams = TeamData.get(server);

        int remainingRounds = Math.max(0, data.hangmanRounds - data.hangmanRoundIndex - 1);
        if (remainingRounds <= 0) return;

        UUID leader = null;
        int leaderScore = -1;
        int maxOther = -1;

        for (TeamData.TeamInfo team : teams.getTeams()) {
            if (team.members.isEmpty()) continue;
            if (data.hardcoreEnabled && data.isTeamEliminated(team.id)) continue;
            int score = scores.getTeamScore(team.id);
            if (score > leaderScore) {
                maxOther = leaderScore;
                leaderScore = score;
                leader = team.id;
            } else if (score > maxOther) {
                maxOther = score;
            }
        }

        if (leader != null && leaderScore > maxOther + remainingRounds) {
            BingoWinEvaluator.forcePointsWin(server);
        }
    }
}






