package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.bingo.BingoCard;
import com.jamie.jamiebingo.bingo.BingoSeedHelper;
import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.bingo.CardSeedCodec;
import com.jamie.jamiebingo.bingo.SettingsSeedCodec;
import com.jamie.jamiebingo.client.ClientGameHistoryOpener;
import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;

public class PacketOpenGameHistory {
    private final List<EntryData> entries;

    public PacketOpenGameHistory(List<EntryData> entries) {
        this.entries = entries == null ? List.of() : new ArrayList<>(entries);
    }

    public static PacketOpenGameHistory fromEntries(List<BingoGameData.GameHistoryEntry> source) {
        List<EntryData> out = new ArrayList<>();
        if (source != null) {
            for (BingoGameData.GameHistoryEntry entry : source) {
                if (entry == null) continue;
                CardSeedCodec.SeedData seedData = CardSeedCodec.decode(entry.cardSeed);
                BingoCard previewCard = seedData != null && seedData.cards() != null && !seedData.cards().isEmpty()
                        ? seedData.cards().getFirst()
                        : null;
                List<String> settingsLines = new ArrayList<>();
                List<String> storedSettings = entry.settingsLines == null ? List.of() : entry.settingsLines;
                List<String> seedSettings = BingoSeedHelper.buildSettingsLinesFromSeed(null, seedData, entry.worldSeed);
                settingsLines.addAll(storedSettings);
                for (String line : seedSettings) {
                    if (line == null || line.isBlank()) continue;
                    String key = settingKey(line);
                    if (key.isBlank() || settingsLines.stream().noneMatch(existing -> settingKey(existing).equals(key))) {
                        settingsLines.add(line);
                    }
                }
                if (entry.mineDisplayName != null && !entry.mineDisplayName.isBlank()
                        && settingsLines.stream().noneMatch(line -> line != null && line.startsWith("Mine:"))) {
                    settingsLines.add("Mine: " + entry.mineDisplayName);
                }
                boolean hidePreviewSlots = BingoSeedHelper.shouldMaskPreview(seedData);
                out.add(new EntryData(
                        entry.cardSeed,
                        entry.worldSeed,
                        entry.settingsSeed == null || entry.settingsSeed.isBlank()
                                ? SettingsSeedCodec.fromCardSeed(entry.cardSeed)
                                : entry.settingsSeed,
                        entry.durationSeconds,
                        entry.completed,
                        entry.finishedAtEpochSeconds,
                        previewCard != null ? previewCard.getSize() : entry.previewSize,
                        previewCard != null ? buildPreviewSlots(previewCard) : PreviewSlotData.fromIds(entry.previewSlotIds),
                        entry.completedSlotIds,
                        entry.opponentCompletedSlotIds,
                        entry.teamColorId,
                        settingsLines,
                        hidePreviewSlots,
                        entry.participantCount,
                        entry.commandsUsed,
                        entry.voteRerollUsed,
                        entry.rerollsUsedCount,
                        entry.fakeRerollsUsedCount,
                        entry.weeklyChallenge,
                        entry.weeklyChallengeId,
                        entry.leaderboardCategory,
                        entry.leaderboardCategoryReason
                ));
            }
        }
        return new PacketOpenGameHistory(out);
    }

    public static void encode(PacketOpenGameHistory msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entries.size());
        for (EntryData entry : msg.entries) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, entry.cardSeed);
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, entry.worldSeed);
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, entry.settingsSeed);
            buf.writeInt((int) Math.min(Integer.MAX_VALUE, Math.max(0L, entry.durationSeconds)));
            buf.writeBoolean(entry.completed);
            buf.writeInt((int) Math.min(Integer.MAX_VALUE, Math.max(0L, entry.finishedAtEpochSeconds)));
            encodePreviewSlots(buf, entry.previewSize, entry.previewSlots);
            buf.writeInt(entry.completedSlotIds.size());
            for (String id : entry.completedSlotIds) {
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, id == null ? "" : id);
            }
            buf.writeInt(entry.opponentCompletedSlotIds.size());
            for (String id : entry.opponentCompletedSlotIds) {
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, id == null ? "" : id);
            }
            buf.writeInt(entry.teamColorId);
            buf.writeInt(entry.settingsLines.size());
            for (String line : entry.settingsLines) {
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, line);
            }
            buf.writeBoolean(entry.hidePreviewSlots);
            buf.writeInt(entry.participantCount);
            buf.writeBoolean(entry.commandsUsed);
            buf.writeBoolean(entry.voteRerollUsed);
            buf.writeInt(entry.rerollsUsedCount);
            buf.writeInt(entry.fakeRerollsUsedCount);
            buf.writeBoolean(entry.weeklyChallenge);
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, entry.weeklyChallengeId);
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, entry.leaderboardCategory);
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, entry.leaderboardCategoryReason);
        }
    }

    public static PacketOpenGameHistory decode(FriendlyByteBuf buf) {
        int count = Math.max(0, buf.readInt());
        List<EntryData> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String cardSeed = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 100000);
            String worldSeed = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 100000);
            String settingsSeed = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 100000);
            long durationSeconds = Math.max(0, buf.readInt());
            boolean completed = buf.readBoolean();
            long finishedAt = Math.max(0, buf.readInt());
            PreviewGrid preview = decodePreviewSlots(buf);
            int completedCount = Math.max(0, buf.readInt());
            List<String> completedSlotIds = new ArrayList<>(completedCount);
            for (int j = 0; j < completedCount; j++) {
                completedSlotIds.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
            }
            int opponentCompletedCount = Math.max(0, buf.readInt());
            List<String> opponentCompletedSlotIds = new ArrayList<>(opponentCompletedCount);
            for (int j = 0; j < opponentCompletedCount; j++) {
                opponentCompletedSlotIds.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
            }
            int teamColorId = Math.max(0, Math.min(15, buf.readInt()));
            int settingsCount = Math.max(0, buf.readInt());
            List<String> settingsLines = new ArrayList<>(settingsCount);
            for (int k = 0; k < settingsCount; k++) {
                settingsLines.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
            }
            boolean hidePreviewSlots = buf.readBoolean();
            int participantCount = Math.max(0, buf.readInt());
            boolean commandsUsed = buf.readBoolean();
            boolean voteRerollUsed = buf.readBoolean();
            int rerollsUsedCount = Math.max(0, buf.readInt());
            int fakeRerollsUsedCount = Math.max(0, buf.readInt());
            boolean weeklyChallenge = buf.readBoolean();
            String weeklyChallengeId = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
            String leaderboardCategory = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
            String leaderboardCategoryReason = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
            entries.add(new EntryData(
                    cardSeed,
                    worldSeed,
                    settingsSeed,
                    durationSeconds,
                    completed,
                    finishedAt,
                    preview.previewSize(),
                    preview.previewSlots(),
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
            ));
        }
        return new PacketOpenGameHistory(entries);
    }

    public static void handle(PacketOpenGameHistory msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientGameHistoryOpener.open(msg.entries)
        ));
        ctx.setPacketHandled(true);
    }

    private static List<PreviewSlotData> buildPreviewSlots(BingoCard card) {
        List<PreviewSlotData> out = new ArrayList<>();
        if (card == null) return out;
        for (int y = 0; y < card.getSize(); y++) {
            for (int x = 0; x < card.getSize(); x++) {
                BingoSlot slot = card.getSlot(x, y);
                out.add(slot == null
                        ? PreviewSlotData.empty()
                        : new PreviewSlotData(slot.getId(), slot.getName(), slot.getCategory(), slot.getRarity()));
            }
        }
        return out;
    }

    private static void encodePreviewSlots(FriendlyByteBuf buf, int previewSize, List<PreviewSlotData> previewSlots) {
        buf.writeInt(Math.max(0, previewSize));
        buf.writeInt(previewSlots == null ? 0 : previewSlots.size());
        if (previewSlots == null) return;
        for (PreviewSlotData slot : previewSlots) {
            PreviewSlotData safe = slot == null ? PreviewSlotData.empty() : slot;
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, safe.id());
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, safe.name());
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, safe.category());
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, safe.rarity());
        }
    }

    private static PreviewGrid decodePreviewSlots(FriendlyByteBuf buf) {
        int previewSize = Math.max(0, buf.readInt());
        int count = Math.max(0, buf.readInt());
        List<PreviewSlotData> previewSlots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            previewSlots.add(new PreviewSlotData(
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767)
            ));
        }
        return new PreviewGrid(previewSize, previewSlots);
    }

    private static String settingKey(String line) {
        if (line == null) return "";
        int idx = line.indexOf(':');
        return idx < 0 ? line.trim() : line.substring(0, idx).trim();
    }

    public static final class EntryData {
        public final String cardSeed;
        public final String worldSeed;
        public final String settingsSeed;
        public final long durationSeconds;
        public final boolean completed;
        public final long finishedAtEpochSeconds;
        public final int previewSize;
        public final List<PreviewSlotData> previewSlots;
        public final List<String> previewSlotIds;
        public final List<String> completedSlotIds;
        public final List<String> opponentCompletedSlotIds;
        public final int teamColorId;
        public final List<String> settingsLines;
        public final boolean hidePreviewSlots;
        public final int participantCount;
        public final boolean commandsUsed;
        public final boolean voteRerollUsed;
        public final int rerollsUsedCount;
        public final int fakeRerollsUsedCount;
        public final boolean weeklyChallenge;
        public final String weeklyChallengeId;
        public final String leaderboardCategory;
        public final String leaderboardCategoryReason;

        public EntryData(
                String cardSeed,
                String worldSeed,
                String settingsSeed,
                long durationSeconds,
                boolean completed,
                long finishedAtEpochSeconds,
                int previewSize,
                List<PreviewSlotData> previewSlots,
                List<String> completedSlotIds,
                List<String> opponentCompletedSlotIds,
                int teamColorId,
                List<String> settingsLines,
                boolean hidePreviewSlots,
                int participantCount,
                boolean commandsUsed,
                boolean voteRerollUsed,
                int rerollsUsedCount,
                int fakeRerollsUsedCount,
                boolean weeklyChallenge,
                String weeklyChallengeId,
                String leaderboardCategory,
                String leaderboardCategoryReason
        ) {
            this.cardSeed = cardSeed == null ? "" : cardSeed;
            this.worldSeed = worldSeed == null ? "" : worldSeed;
            this.settingsSeed = settingsSeed == null ? "" : settingsSeed;
            this.durationSeconds = Math.max(0L, durationSeconds);
            this.completed = completed;
            this.finishedAtEpochSeconds = Math.max(0L, finishedAtEpochSeconds);
            this.previewSize = Math.max(0, previewSize);
            this.previewSlots = previewSlots == null ? List.of() : new ArrayList<>(previewSlots);
            this.previewSlotIds = this.previewSlots.stream().map(slot -> slot == null ? "" : slot.id()).toList();
            this.completedSlotIds = completedSlotIds == null ? List.of() : new ArrayList<>(completedSlotIds);
            this.opponentCompletedSlotIds = opponentCompletedSlotIds == null ? List.of() : new ArrayList<>(opponentCompletedSlotIds);
            this.teamColorId = Math.max(0, Math.min(15, teamColorId));
            this.settingsLines = settingsLines == null ? List.of() : new ArrayList<>(settingsLines);
            this.hidePreviewSlots = hidePreviewSlots;
            this.participantCount = Math.max(0, participantCount);
            this.commandsUsed = commandsUsed;
            this.voteRerollUsed = voteRerollUsed;
            this.rerollsUsedCount = Math.max(0, rerollsUsedCount);
            this.fakeRerollsUsedCount = Math.max(0, fakeRerollsUsedCount);
            this.weeklyChallenge = weeklyChallenge;
            this.weeklyChallengeId = weeklyChallengeId == null ? "" : weeklyChallengeId;
            this.leaderboardCategory = leaderboardCategory == null || leaderboardCategory.isBlank() ? "Custom" : leaderboardCategory;
            this.leaderboardCategoryReason = leaderboardCategoryReason == null ? "" : leaderboardCategoryReason;
        }

        public BingoCard previewCard() {
            if (previewSize <= 0 || previewSlots.isEmpty()) return null;
            BingoCard card = new BingoCard(previewSize);
            int expected = previewSize * previewSize;
            for (int i = 0; i < expected && i < previewSlots.size(); i++) {
                PreviewSlotData slot = previewSlots.get(i);
                if (slot == null || slot.id().isBlank()) continue;
                int x = i % previewSize;
                int y = i / previewSize;
                card.setSlot(x, y, new BingoSlot(slot.id(), slot.name(), slot.category(), slot.rarity()));
            }
            return card;
        }
    }

    private record PreviewGrid(int previewSize, List<PreviewSlotData> previewSlots) {
    }

    public record PreviewSlotData(String id, String name, String category, String rarity) {
        public PreviewSlotData {
            id = id == null ? "" : id;
            name = name == null ? "" : name;
            category = category == null ? "" : category;
            rarity = rarity == null ? "" : rarity;
        }

        public static PreviewSlotData empty() {
            return new PreviewSlotData("", "", "", "");
        }

        public static List<PreviewSlotData> fromIds(List<String> ids) {
            List<PreviewSlotData> out = new ArrayList<>();
            if (ids == null) return out;
            for (String id : ids) {
                out.add(new PreviewSlotData(id, id, "", ""));
            }
            return out;
        }
    }
}
