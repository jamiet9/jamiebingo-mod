package com.jamie.jamiebingo.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jamie.jamiebingo.network.packet.PacketOpenGameHistory;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClientGameHistoryStore {
    private static final int MAX_ENTRIES_PER_ACCOUNT = 2000;
    private static final Path CONFIG_PATH =
            FMLPaths.CONFIGDIR.get().resolve("jamiebingo_history.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean loaded = false;
    private static SaveData data = new SaveData();

    private ClientGameHistoryStore() {
    }

    public static List<PacketOpenGameHistory.EntryData> mergeForCurrentAccount(
            List<PacketOpenGameHistory.EntryData> incoming
    ) {
        load();
        AccountHistory account = getAccount(resolveAccountKey());
        if (account == null) return List.of();

        boolean changed = false;
        Set<String> seen = new HashSet<>();
        List<EntryRecord> kept = new ArrayList<>();
        for (EntryRecord record : account.entries) {
            if (record == null) continue;
            String key = signature(record);
            if (account.deletedSignatures.contains(key)) continue;
            if (!seen.add(key)) continue;
            kept.add(record);
        }
        if (kept.size() != account.entries.size()) {
            account.entries = kept;
            changed = true;
        }

        if (incoming != null) {
            for (PacketOpenGameHistory.EntryData entry : incoming) {
                if (entry == null) continue;
                EntryRecord record = EntryRecord.fromEntry(entry);
                String key = signature(record);
                if (account.deletedSignatures.contains(key)) continue;
                if (!seen.add(key)) continue;
                account.entries.add(record);
                changed = true;
            }
        }

        account.entries.sort(Comparator.comparingLong((EntryRecord r) -> r.finishedAtEpochSeconds).reversed());
        if (account.entries.size() > MAX_ENTRIES_PER_ACCOUNT) {
            account.entries = new ArrayList<>(account.entries.subList(0, MAX_ENTRIES_PER_ACCOUNT));
            changed = true;
        }

        if (changed) save();
        return toEntries(account.entries);
    }

    public static void deleteForCurrentAccount(PacketOpenGameHistory.EntryData entry) {
        if (entry == null) return;
        load();
        AccountHistory account = getAccount(resolveAccountKey());
        if (account == null) return;

        String key = signature(EntryRecord.fromEntry(entry));
        account.deletedSignatures.add(key);
        account.entries.removeIf(record -> key.equals(signature(record)));
        save();
    }

    public static boolean isSubmittedForCurrentAccount(PacketOpenGameHistory.EntryData entry) {
        if (entry == null) return false;
        load();
        AccountHistory account = getAccount(resolveAccountKey());
        if (account == null) return false;
        return account.submittedSignatures.contains(signature(EntryRecord.fromEntry(entry)));
    }

    public static void markSubmittedForCurrentAccount(PacketOpenGameHistory.EntryData entry) {
        if (entry == null) return;
        load();
        AccountHistory account = getAccount(resolveAccountKey());
        if (account == null) return;
        if (account.submittedSignatures.add(signature(EntryRecord.fromEntry(entry)))) {
            save();
        }
    }

    private static List<PacketOpenGameHistory.EntryData> toEntries(List<EntryRecord> records) {
        List<PacketOpenGameHistory.EntryData> out = new ArrayList<>();
        if (records == null) return out;
        for (EntryRecord r : records) {
            if (r == null) continue;
            out.add(r.toEntry());
        }
        return out;
    }

    private static String resolveAccountKey() {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        if (mc != null && mc.getUser() != null && mc.getUser().getName() != null && !mc.getUser().getName().isBlank()) {
            return "name:" + mc.getUser().getName().trim().toLowerCase(java.util.Locale.ROOT);
        }
        if (mc != null && mc.player != null) {
            return "uuid:" + com.jamie.jamiebingo.util.EntityUtil.getUUID(mc.player);
        }
        return "default";
    }

    private static AccountHistory getAccount(String key) {
        if (key == null || key.isBlank()) return null;
        return data.accounts.computeIfAbsent(key, ignored -> new AccountHistory());
    }

    private static void load() {
        if (loaded) return;
        loaded = true;

        if (!Files.exists(CONFIG_PATH)) {
            data = new SaveData();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            SaveData loadedData = GSON.fromJson(reader, SaveData.class);
            data = loadedData == null ? new SaveData() : loadedData;
            if (data.accounts == null) data.accounts = new HashMap<>();
            for (AccountHistory account : data.accounts.values()) {
                if (account == null) continue;
                if (account.entries == null) account.entries = new ArrayList<>();
                if (account.deletedSignatures == null) account.deletedSignatures = new HashSet<>();
                if (account.submittedSignatures == null) account.submittedSignatures = new HashSet<>();
            }
        } catch (IOException ignored) {
            data = new SaveData();
        }
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
        } catch (IOException ignored) {
        }

        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(data, writer);
        } catch (IOException ignored) {
        }
    }

    private static String signature(EntryRecord r) {
        if (r == null) return "";
        return String.join(
                "\u001F",
                safe(r.cardSeed),
                safe(r.worldSeed),
                safe(r.settingsSeed),
                String.valueOf(r.durationSeconds),
                String.valueOf(r.completed),
                String.valueOf(r.finishedAtEpochSeconds),
                String.valueOf(r.previewSize),
                join(r.previewSlotIds),
                join(r.completedSlotIds),
                join(r.opponentCompletedSlotIds),
                String.valueOf(r.participantCount),
                String.valueOf(r.commandsUsed),
                String.valueOf(r.voteRerollUsed),
                String.valueOf(r.rerollsUsedCount),
                String.valueOf(r.fakeRerollsUsedCount),
                String.valueOf(r.weeklyChallenge),
                safe(r.weeklyChallengeId),
                safe(r.leaderboardCategory),
                safe(r.leaderboardCategoryReason)
        );
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) out.append('\u001E');
            out.append(safe(values.get(i)));
        }
        return out.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class SaveData {
        Map<String, AccountHistory> accounts = new HashMap<>();
    }

    private static final class AccountHistory {
        List<EntryRecord> entries = new ArrayList<>();
        Set<String> deletedSignatures = new HashSet<>();
        Set<String> submittedSignatures = new HashSet<>();
    }

    private static final class EntryRecord {
        String cardSeed = "";
        String worldSeed = "";
        String settingsSeed = "";
        long durationSeconds = 0L;
        boolean completed = false;
        long finishedAtEpochSeconds = 0L;
        int previewSize = 0;
        List<PacketOpenGameHistory.PreviewSlotData> previewSlots = new ArrayList<>();
        List<String> previewSlotIds = new ArrayList<>();
        List<String> completedSlotIds = new ArrayList<>();
        List<String> opponentCompletedSlotIds = new ArrayList<>();
        int teamColorId = 10;
        List<String> settingsLines = new ArrayList<>();
        boolean hidePreviewSlots = false;
        int participantCount = 0;
        boolean commandsUsed = false;
        boolean voteRerollUsed = false;
        int rerollsUsedCount = 0;
        int fakeRerollsUsedCount = 0;
        boolean weeklyChallenge = false;
        String weeklyChallengeId = "";
        String leaderboardCategory = "Custom";
        String leaderboardCategoryReason = "";

        static EntryRecord fromEntry(PacketOpenGameHistory.EntryData e) {
            EntryRecord r = new EntryRecord();
            r.cardSeed = e.cardSeed == null ? "" : e.cardSeed;
            r.worldSeed = e.worldSeed == null ? "" : e.worldSeed;
            r.settingsSeed = e.settingsSeed == null ? "" : e.settingsSeed;
            r.durationSeconds = e.durationSeconds;
            r.completed = e.completed;
            r.finishedAtEpochSeconds = e.finishedAtEpochSeconds;
            r.previewSize = e.previewSize;
            r.previewSlots = e.previewSlots == null ? new ArrayList<>() : new ArrayList<>(e.previewSlots);
            r.previewSlotIds = e.previewSlotIds == null ? new ArrayList<>() : new ArrayList<>(e.previewSlotIds);
            r.completedSlotIds = e.completedSlotIds == null ? new ArrayList<>() : new ArrayList<>(e.completedSlotIds);
            r.opponentCompletedSlotIds = e.opponentCompletedSlotIds == null ? new ArrayList<>() : new ArrayList<>(e.opponentCompletedSlotIds);
            r.teamColorId = e.teamColorId;
            r.settingsLines = e.settingsLines == null ? new ArrayList<>() : new ArrayList<>(e.settingsLines);
            r.hidePreviewSlots = e.hidePreviewSlots;
            r.participantCount = e.participantCount;
            r.commandsUsed = e.commandsUsed;
            r.voteRerollUsed = e.voteRerollUsed;
            r.rerollsUsedCount = e.rerollsUsedCount;
            r.fakeRerollsUsedCount = e.fakeRerollsUsedCount;
            r.weeklyChallenge = e.weeklyChallenge;
            r.weeklyChallengeId = e.weeklyChallengeId;
            r.leaderboardCategory = e.leaderboardCategory;
            r.leaderboardCategoryReason = e.leaderboardCategoryReason;
            return r;
        }

        PacketOpenGameHistory.EntryData toEntry() {
            return new PacketOpenGameHistory.EntryData(
                    cardSeed,
                    worldSeed,
                    settingsSeed,
                    durationSeconds,
                    completed,
                    finishedAtEpochSeconds,
                    previewSize,
                    previewSlots == null || previewSlots.isEmpty()
                            ? PacketOpenGameHistory.PreviewSlotData.fromIds(previewSlotIds)
                            : previewSlots,
                    completedSlotIds,
                    opponentCompletedSlotIds,
                    teamColorId,
                    settingsLines,
                    hidePreviewSlots,
                    participantCount,
                    commandsUsed,
                    voteRerollUsed,
                    rerollsUsedCount,
                    fakeRerollsUsedCount,
                    weeklyChallenge,
                    weeklyChallengeId,
                    leaderboardCategory,
                    leaderboardCategoryReason
            );
        }
    }
}
