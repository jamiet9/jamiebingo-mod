package com.jamie.jamiebingo.bingo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

public final class WeeklyChallengeExportCli {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private WeeklyChallengeExportCli() {
    }

    public static void main(String[] args) {
        long baseSeed = WeeklyChallengeManager.currentChallengeSeed();
        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
            baseSeed = Long.parseLong(args[0].trim());
        }

        WeeklyChallengeManager.WeeklyChallenge weekly = WeeklyChallengeManager.buildServerless(baseSeed);
        if (weekly == null) {
            System.err.println("Failed to build weekly challenge for base seed " + baseSeed);
            System.exit(1);
            return;
        }

        Payload payload = new Payload(
                weekly.baseSeed(),
                weekly.challengeId(),
                weekly.nextResetEpochSeconds(),
                weekly.settingsSeed(),
                weekly.worldSeed(),
                weekly.cardSeed(),
                weekly.card().getSize(),
                buildPreviewSlots(weekly.card()),
                weekly.settingsLines()
        );
        System.out.println(GSON.toJson(payload));
    }

    private static List<PreviewSlot> buildPreviewSlots(BingoCard card) {
        List<PreviewSlot> out = new ArrayList<>();
        if (card == null) return out;
        int size = Math.max(0, card.getSize());
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot == null) {
                    out.add(new PreviewSlot("", "", "", "", null));
                    continue;
                }
                out.add(new PreviewSlot(
                        safe(slot.getId()),
                        safe(slot.getName()),
                        safe(slot.getCategory()),
                        safe(slot.getRarity()),
                        null
                ));
            }
        }
        return out;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record Payload(
            long baseSeed,
            String challengeId,
            long nextResetEpochSeconds,
            String settingsSeed,
            String worldSeed,
            String cardSeed,
            int previewSize,
            List<PreviewSlot> previewSlots,
            List<String> settingsLines
    ) {
    }

    private record PreviewSlot(
            String id,
            String name,
            String category,
            String rarity,
            Object questIcon
    ) {
    }
}