package com.jamie.jamiebingo.bingo;

import com.jamie.jamiebingo.casino.CasinoModeManager;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.data.ItemDatabase;
import com.jamie.jamiebingo.ItemDefinition;
import com.jamie.jamiebingo.quest.QuestDatabase;
import com.jamie.jamiebingo.quest.QuestDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CardSeedCodec {

    private static final int VERSION = 1;
    private static final byte[] V2_MAGIC = new byte[] { 'B', '2' };
    private static final byte[] V3_MAGIC = new byte[] { 'B', '3' };

    private CardSeedCodec() {
    }

    public record SeedData(CompoundTag settings, List<BingoCard> cards) {}

    public static String encode(BingoGameData data, MinecraftServer server) {
        if (data == null || server == null || data.getCurrentCard() == null) return "";
        // Always emit the full NBT seed so world-generation settings are preserved.
        CompoundTag settings = SettingsSeedCodec.createSettingsTag(data, server);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "CustomCardEnabled", data.customCardEnabled);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "CustomPoolEnabled", data.customPoolEnabled);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "WorldTypeMode", data.worldTypeMode);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "WorldSmallBiomes", data.worldSmallBiomes);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "WorldCustomBiomeSizeBlocks", com.jamie.jamiebingo.data.BingoGameData.clampWorldBiomeSize(data.worldCustomBiomeSizeBlocks));
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "WorldTerrainHillinessPercent", com.jamie.jamiebingo.data.BingoGameData.clampWorldTerrainHilliness(data.worldTerrainHillinessPercent));
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "WorldStructureFrequencyPercent", com.jamie.jamiebingo.data.BingoGameData.clampWorldStructureFrequency(data.worldStructureFrequencyPercent));
        com.jamie.jamiebingo.util.NbtUtil.putString(settings, "WorldSingleBiomeId", data.worldSingleBiomeId == null ? "minecraft:plains" : data.worldSingleBiomeId);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "WorldSurfaceCaveBiomes", data.worldSurfaceCaveBiomes);
        com.jamie.jamiebingo.util.NbtUtil.putString(settings, "WorldSetSeedText", data.worldSetSeedText == null ? "" : data.worldSetSeedText);

        ListTag poolTag = new ListTag();
        for (String id : data.customPoolIds) {
            poolTag.add(StringTag.valueOf(id == null ? "" : id));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(settings, "CustomPoolIds", poolTag);
        ListTag mineTag = new ListTag();
        for (String id : data.customMineIds) {
            if (id == null || id.isBlank()) continue;
            mineTag.add(StringTag.valueOf(id));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(settings, "CustomMineIds", mineTag);
        ListTag selectedMineTag = new ListTag();
        for (String id : data.getMineResumeSourceIds()) {
            if (id == null || id.isBlank()) continue;
            selectedMineTag.add(StringTag.valueOf(id));
        }
        if (!selectedMineTag.isEmpty()) {
            com.jamie.jamiebingo.util.NbtUtil.putTag(settings, "SelectedMineSourceIds", selectedMineTag);
        }

        List<BingoCard> cards = new ArrayList<>();
        if (data.winCondition == WinCondition.GUNGAME || data.winCondition == WinCondition.GAMEGUN) {
            cards.addAll(data.getGunGameCardsSnapshot());
        } else if (data.winCondition == WinCondition.HANGMAN) {
            cards.addAll(data.hangmanCards);
        } else {
            cards.add(data.getCurrentCard());
        }

        return encode(settings, cards);
    }

    public static String encode(CompoundTag settings, List<BingoCard> cards) {
        if (settings == null || cards == null || cards.isEmpty()) return "";

        CompoundTag root = new CompoundTag();
        com.jamie.jamiebingo.util.NbtUtil.putInt(root, "v", VERSION);

        com.jamie.jamiebingo.util.NbtUtil.putTag(root, "Settings", settings);

        ListTag cardsTag = new ListTag();
        for (BingoCard card : cards) {
            if (card == null) continue;
            CompoundTag c = new CompoundTag();
            com.jamie.jamiebingo.util.NbtUtil.putInt(c, "Size", card.getSize());
            ListTag slots = new ListTag();
            for (int y = 0; y < card.getSize(); y++) {
                for (int x = 0; x < card.getSize(); x++) {
                    BingoSlot slot = card.getSlot(x, y);
                    if (slot == null) continue;
                    CompoundTag s = new CompoundTag();
                    com.jamie.jamiebingo.util.NbtUtil.putInt(s, "X", x);
                    com.jamie.jamiebingo.util.NbtUtil.putInt(s, "Y", y);
                    com.jamie.jamiebingo.util.NbtUtil.putString(s, "Id", slot.getId());
                    com.jamie.jamiebingo.util.NbtUtil.putString(s, "Name", slot.getName());
                    com.jamie.jamiebingo.util.NbtUtil.putString(s, "Category", slot.getCategory());
                    com.jamie.jamiebingo.util.NbtUtil.putString(s, "Rarity", slot.getRarity());
                    slots.add(s);
                }
            }
            com.jamie.jamiebingo.util.NbtUtil.putTag(c, "Slots", slots);
            cardsTag.add(c);
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(root, "Cards", cardsTag);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            NbtIo.writeCompressed(root, out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    public static SeedData decode(String seed) {
        if (seed == null || seed.isBlank()) return null;
        try {
            byte[] raw = Base64.getDecoder().decode(seed.trim());

            if (raw.length >= 2 && raw[0] == V3_MAGIC[0] && raw[1] == V3_MAGIC[1]) {
                return decodeV3(raw);
            }

            if (raw.length >= 2 && raw[0] == V2_MAGIC[0] && raw[1] == V2_MAGIC[1]) {
                return decodeV2(raw);
            }

            CompoundTag root = NbtIo.readCompressed(new ByteArrayInputStream(raw), NbtAccounter.unlimitedHeap());
            if (root == null) return null;

            int v = root.getInt("v").orElse(-1);
            if (v != VERSION) {
                return null;
            }

            CompoundTag settings = root.getCompoundOrEmpty("Settings");
            List<BingoCard> cards = new ArrayList<>();

            ListTag cardsTag = root.getListOrEmpty("Cards");
            for (int i = 0; i < cardsTag.size(); i++) {
                CompoundTag c = cardsTag.getCompoundOrEmpty(i);
                int size = c.getInt("Size").orElse(0);
                BingoCard card = new BingoCard(size);
                ListTag slots = c.getListOrEmpty("Slots");
                for (int j = 0; j < slots.size(); j++) {
                    CompoundTag s = slots.getCompoundOrEmpty(j);
                    int x = s.getInt("X").orElse(0);
                    int y = s.getInt("Y").orElse(0);
                    BingoSlot slot = new BingoSlot(
                            s.getString("Id").orElse(""),
                            s.getString("Name").orElse(""),
                            s.getString("Category").orElse(""),
                            s.getString("Rarity").orElse("")
                    );
                    card.setSlot(x, y, slot);
                }
                cards.add(card);
            }

            if (cards.isEmpty()) return null;
            return new SeedData(settings, cards);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Builds a gungame seed that includes every known item + quest id once,
     * spanning as many 10x10 cards as needed. Uses the current item/quest
     * dictionaries so the seed validates at runtime.
     */
    public static String buildAllIdsGunGameSeed(int size) {
        try {
            ItemDatabase.load();
            QuestDatabase.load();

            List<String> itemIds = ItemDatabaseItems.getIds();
            List<String> questIds = QuestDatabaseItems.getIds();

            if (itemIds.isEmpty()) return null;

            List<String> allIds = new ArrayList<>(itemIds.size() + questIds.size());
            allIds.addAll(itemIds);
            allIds.addAll(questIds);

            int clampedSize = clamp(size, 1, 10);
            int slotsPerCard = clampedSize * clampedSize;
            int cardCount = (int) Math.ceil(allIds.size() / (double) slotsPerCard);
            if (cardCount <= 0 || cardCount > 63) return null;

            int totalSlots = cardCount * slotsPerCard;
            if (allIds.size() < totalSlots) {
                // repeat from start to fill final card
                for (int i = 0; allIds.size() < totalSlots && i < itemIds.size(); i++) {
                    allIds.add(itemIds.get(i));
                }
                for (int i = 0; allIds.size() < totalSlots && i < questIds.size(); i++) {
                    allIds.add(questIds.get(i));
                }
            }

            Map<String, Integer> itemIndex = indexMap(itemIds);
            Map<String, Integer> questIndex = indexMap(questIds);

            int itemBits = bitsForSize(itemIds.size());
            int questBits = bitsForSize(Math.max(1, questIds.size()));

            BitWriter bw = new BitWriter();
            bw.writeBytes(V2_MAGIC);

            // Settings
            bw.writeBits(WinCondition.GUNGAME.ordinal(), 4);
            bw.writeBits(CardComposition.HYBRID_PERCENT.ordinal(), 2);
            bw.writeBits(encodeDifficulty("normal"), 3);
            bw.writeBits(clampedSize, 4);
            bw.writeBits(50, 7); // quest percent (display only, cards are fixed)
            bw.writeBits(clamp(cardCount, 0, 31), 5); // gun rounds
            bw.writeBits(0, 1);  // gun shared
            bw.writeBits(0, 4);  // rerolls
            bw.writeBits(0, 9);  // effects interval
            bw.writeBits(0, 1);  // effects armed
            bw.writeBits(0, 1);  // rtp
            bw.writeBits(1, 1);  // hostile mobs enabled
            bw.writeBits(1, 1);  // hunger enabled
            bw.writeBits(0, 1);  // pvp
            bw.writeBits(0, 1);  // keep inv
            bw.writeBits(0, 1);  // hardcore
            bw.writeBits(0, 3);  // daylight
            bw.writeBits(0, 10); // start delay
            bw.writeBits(0, 1);  // countdown enabled
            bw.writeBits(0, 8);  // countdown minutes
            bw.writeBits(1, 1);  // allow late join
            bw.writeBits(0, 1);  // register mode
            bw.writeBits(0, 1);  // casino
            bw.writeBits(1, 2);  // game difficulty normal
            bw.writeBits(0, 5);  // hangman rounds
            bw.writeBits(0, 9);  // hangman base seconds
            bw.writeBits(0, 9);  // hangman penalty seconds

            // Dictionary sizes for validation
            bw.writeBits(clamp(itemIds.size(), 1, 65535), 16);
            bw.writeBits(clamp(Math.max(1, questIds.size()), 1, 65535), 16);

            // Cards
            bw.writeBits(clamp(cardCount, 1, 63), 6);
            int idx = 0;
            for (int c = 0; c < cardCount; c++) {
                bw.writeBits(clampedSize, 4);
                for (int y = 0; y < clampedSize; y++) {
                    for (int x = 0; x < clampedSize; x++) {
                        String id = allIds.get(idx++);
                        if (id != null && id.startsWith("quest.")) {
                            Integer q = questIndex.get(id);
                            if (q == null) return null;
                            bw.writeBits(1, 1);
                            bw.writeBits(q, questBits);
                        } else {
                            Integer it = itemIndex.get(id);
                            if (it == null) return null;
                            bw.writeBits(0, 1);
                            bw.writeBits(it, itemBits);
                        }
                    }
                }
            }

            byte[] payload = bw.toByteArray();
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            return null;
        }
    }

    private static String encodeV2(BingoGameData data, MinecraftServer server) {
        try {
            ItemDatabase.load();
            QuestDatabase.load();

            List<String> itemIds = ItemDatabaseItems.getIds();
            List<String> questIds = QuestDatabaseItems.getIds();

            if (itemIds.isEmpty()) return null;

            Map<String, Integer> itemIndex = indexMap(itemIds);
            Map<String, Integer> questIndex = indexMap(questIds);

            List<BingoCard> cards = new ArrayList<>();
            if (data.winCondition == WinCondition.GUNGAME || data.winCondition == WinCondition.GAMEGUN) {
                cards.addAll(data.getGunGameCardsSnapshot());
            } else if (data.winCondition == WinCondition.HANGMAN) {
                cards.addAll(data.hangmanCards);
            } else {
                cards.add(data.getCurrentCard());
            }

            if (cards.isEmpty()) return null;

            int itemBits = bitsForSize(itemIds.size());
            int questBits = bitsForSize(Math.max(1, questIds.size()));

            BitWriter bw = new BitWriter();
            bw.writeBytes(V2_MAGIC);

            // Settings (bit-packed)
            bw.writeBits(data.winCondition.ordinal(), 4);
            bw.writeBits(data.composition.ordinal(), 2);
            bw.writeBits(encodeDifficulty(data.difficulty), 3);
            bw.writeBits(clamp(data.size, 1, 10), 4);
            bw.writeBits(clamp(data.questPercent, 0, 100), 7);
            bw.writeBits(clamp(data.gunGameLength, 0, 31), 5);
            bw.writeBits(data.gunGameShared ? 1 : 0, 1);
            bw.writeBits(clamp(data.rerollsPerPlayer, 0, 10), 4);
            bw.writeBits(clamp(data.randomEffectsIntervalSeconds, 0, 300), 9);
            bw.writeBits(data.randomEffectsArmed ? 1 : 0, 1);
            bw.writeBits(data.rtpEnabled ? 1 : 0, 1);
            bw.writeBits(data.hostileMobsEnabled ? 1 : 0, 1);
            bw.writeBits(data.hungerEnabled ? 1 : 0, 1);
            bw.writeBits(data.pvpEnabled ? 1 : 0, 1);
            bw.writeBits(data.keepInventoryEnabled ? 1 : 0, 1);
            bw.writeBits(data.hardcoreEnabled ? 1 : 0, 1);
            bw.writeBits(clamp(data.daylightMode, 0, 7), 3);
            bw.writeBits(clamp(data.bingoStartDelaySeconds, 0, 1023), 10);
            bw.writeBits(data.countdownEnabled ? 1 : 0, 1);
            bw.writeBits(clamp(data.countdownMinutes, 0, 255), 8);
            bw.writeBits(data.allowLateJoin ? 1 : 0, 1);
            bw.writeBits(data.registerMode == BingoGameData.REGISTER_ALWAYS_HAVE ? 1 : 0, 1);
            bw.writeBits(CasinoModeManager.isCasinoEnabled() ? 1 : 0, 1);
            bw.writeBits(encodeGameDifficulty(com.jamie.jamiebingo.util.ServerWorldDataUtil.getDifficultyKey(server)), 2);
            bw.writeBits(clamp(data.hangmanRounds, 0, 31), 5);
            bw.writeBits(clamp(data.hangmanBaseSeconds, 0, 511), 9);
            bw.writeBits(clamp(data.hangmanPenaltySeconds, 0, 511), 9);

            // Dictionary sizes for validation
            bw.writeBits(clamp(itemIds.size(), 1, 65535), 16);
            bw.writeBits(clamp(Math.max(1, questIds.size()), 1, 65535), 16);

            // Cards
            bw.writeBits(clamp(cards.size(), 1, 63), 6);
            for (BingoCard card : cards) {
                if (card == null) return null;
                int size = clamp(card.getSize(), 1, 10);
                bw.writeBits(size, 4);
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        BingoSlot slot = card.getSlot(x, y);
                        if (slot == null) {
                            bw.writeBits(0, 1);
                            bw.writeBits(0, itemBits);
                            continue;
                        }
                        String id = slot.getId();
                        if (id != null && id.startsWith("quest.")) {
                            Integer idx = questIndex.get(id);
                            if (idx == null) return null;
                            bw.writeBits(1, 1);
                            bw.writeBits(idx, questBits);
                        } else {
                            Integer idx = itemIndex.get(id);
                            if (idx == null) return null;
                            bw.writeBits(0, 1);
                            bw.writeBits(idx, itemBits);
                        }
                    }
                }
            }

            byte[] payload = bw.toByteArray();
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            return null;
        }
    }

    private static String encodeV3(BingoGameData data, MinecraftServer server) {
        if (data == null) return null;
        if (!data.customCardEnabled && !data.customPoolEnabled && data.customPoolIds.isEmpty()) {
            return null;
        }
        try {
            ItemDatabase.load();
            QuestDatabase.load();

            List<String> itemIds = ItemDatabaseItems.getIds();
            List<String> questIds = QuestDatabaseItems.getIds();

            if (itemIds.isEmpty()) return null;

            Map<String, Integer> itemIndex = indexMap(itemIds);
            Map<String, Integer> questIndex = indexMap(questIds);

            List<BingoCard> cards = new ArrayList<>();
            if (data.winCondition == WinCondition.GUNGAME || data.winCondition == WinCondition.GAMEGUN) {
                cards.addAll(data.getGunGameCardsSnapshot());
            } else if (data.winCondition == WinCondition.HANGMAN) {
                cards.addAll(data.hangmanCards);
            } else {
                cards.add(data.getCurrentCard());
            }

            if (cards.isEmpty()) return null;

            int itemBits = bitsForSize(itemIds.size());
            int questBits = bitsForSize(Math.max(1, questIds.size()));

            BitWriter bw = new BitWriter();
            bw.writeBytes(V3_MAGIC);

            // Settings (bit-packed)
            bw.writeBits(data.winCondition.ordinal(), 4);
            bw.writeBits(data.composition.ordinal(), 2);
            bw.writeBits(encodeDifficulty(data.difficulty), 3);
            bw.writeBits(clamp(data.size, 1, 10), 4);
            bw.writeBits(clamp(data.questPercent, 0, 100), 7);
            bw.writeBits(clamp(data.gunGameLength, 0, 31), 5);
            bw.writeBits(data.gunGameShared ? 1 : 0, 1);
            bw.writeBits(clamp(data.rerollsPerPlayer, 0, 10), 4);
            bw.writeBits(clamp(data.randomEffectsIntervalSeconds, 0, 300), 9);
            bw.writeBits(data.randomEffectsArmed ? 1 : 0, 1);
            bw.writeBits(data.rtpEnabled ? 1 : 0, 1);
            bw.writeBits(data.hostileMobsEnabled ? 1 : 0, 1);
            bw.writeBits(data.hungerEnabled ? 1 : 0, 1);
            bw.writeBits(data.pvpEnabled ? 1 : 0, 1);
            bw.writeBits(data.keepInventoryEnabled ? 1 : 0, 1);
            bw.writeBits(data.hardcoreEnabled ? 1 : 0, 1);
            bw.writeBits(clamp(data.daylightMode, 0, 7), 3);
            bw.writeBits(clamp(data.bingoStartDelaySeconds, 0, 1023), 10);
            bw.writeBits(data.countdownEnabled ? 1 : 0, 1);
            bw.writeBits(clamp(data.countdownMinutes, 0, 255), 8);
            bw.writeBits(data.allowLateJoin ? 1 : 0, 1);
            bw.writeBits(data.registerMode == BingoGameData.REGISTER_ALWAYS_HAVE ? 1 : 0, 1);
            bw.writeBits(CasinoModeManager.isCasinoEnabled() ? 1 : 0, 1);
            bw.writeBits(encodeGameDifficulty(com.jamie.jamiebingo.util.ServerWorldDataUtil.getDifficultyKey(server)), 2);
            bw.writeBits(clamp(data.hangmanRounds, 0, 31), 5);
            bw.writeBits(clamp(data.hangmanBaseSeconds, 0, 511), 9);
            bw.writeBits(clamp(data.hangmanPenaltySeconds, 0, 511), 9);

            int customMode =
                    data.customPoolEnabled ? 2 :
                    data.customCardEnabled ? 1 : 0;
            bw.writeBits(customMode, 2);

            // Dictionary sizes for validation
            bw.writeBits(clamp(itemIds.size(), 1, 65535), 16);
            bw.writeBits(clamp(Math.max(1, questIds.size()), 1, 65535), 16);

            // Pool entries
            int poolCount = Math.min(4095, data.customPoolIds.size());
            bw.writeBits(poolCount, 12);
            for (int i = 0; i < poolCount; i++) {
                String id = data.customPoolIds.get(i);
                if (id != null && id.startsWith("quest.")) {
                    Integer idx = questIndex.get(id);
                    if (idx == null) return null;
                    bw.writeBits(1, 1);
                    bw.writeBits(idx, questBits);
                } else {
                    Integer idx = itemIndex.get(id);
                    if (idx == null) return null;
                    bw.writeBits(0, 1);
                    bw.writeBits(idx, itemBits);
                }
            }

            // Cards
            bw.writeBits(clamp(cards.size(), 1, 63), 6);
            for (BingoCard card : cards) {
                if (card == null) return null;
                int size = clamp(card.getSize(), 1, 10);
                bw.writeBits(size, 4);
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        BingoSlot slot = card.getSlot(x, y);
                        if (slot == null) {
                            bw.writeBits(0, 1);
                            bw.writeBits(0, itemBits);
                            continue;
                        }
                        String id = slot.getId();
                        if (id != null && id.startsWith("quest.")) {
                            Integer idx = questIndex.get(id);
                            if (idx == null) return null;
                            bw.writeBits(1, 1);
                            bw.writeBits(idx, questBits);
                        } else {
                            Integer idx = itemIndex.get(id);
                            if (idx == null) return null;
                            bw.writeBits(0, 1);
                            bw.writeBits(idx, itemBits);
                        }
                    }
                }
            }

            byte[] payload = bw.toByteArray();
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            return null;
        }
    }

    private static SeedData decodeV2(byte[] raw) throws IOException {
        ItemDatabase.load();
        QuestDatabase.load();

        List<String> itemIds = ItemDatabaseItems.getIds();
        List<String> questIds = QuestDatabaseItems.getIds();

        BitReader br = new BitReader(raw, V2_MAGIC.length);

        int winOrdinal = br.readBits(4);
        int compOrdinal = br.readBits(2);
        int difficultyId = br.readBits(3);
        int size = br.readBits(4);
        int questPercent = br.readBits(7);
        int gunRounds = br.readBits(5);
        boolean gunShared = br.readBits(1) == 1;
        int rerolls = br.readBits(4);
        int effectsInterval = br.readBits(9);
        boolean effectsArmed = br.readBits(1) == 1;
        boolean rtp = br.readBits(1) == 1;
        boolean hostile = br.readBits(1) == 1;
        boolean hunger = br.readBits(1) == 1;
        boolean pvp = br.readBits(1) == 1;
        boolean keepInv = br.readBits(1) == 1;
        boolean hardcore = br.readBits(1) == 1;
        int daylight = br.readBits(3);
        int startDelay = br.readBits(10);
        boolean countdownEnabled = br.readBits(1) == 1;
        int countdownMinutes = br.readBits(8);
        boolean allowLateJoin = br.readBits(1) == 1;
        int registerMode = br.readBits(1) == 1
                ? BingoGameData.REGISTER_ALWAYS_HAVE
                : BingoGameData.REGISTER_COLLECT_ONCE;
        boolean casino = br.readBits(1) == 1;
        int gameDiffId = br.readBits(2);
        int hangmanRounds = br.readBits(5);
        int hangmanBaseSeconds = br.readBits(9);
        int hangmanPenaltySeconds = br.readBits(9);

        int itemCount = br.readBits(16);
        int questCount = br.readBits(16);
        if (itemCount != itemIds.size()) {
            return null;
        }
        if (questCount != Math.max(1, questIds.size())) {
            return null;
        }

        int cardCount = br.readBits(6);
        if (cardCount <= 0) return null;

        int itemBits = bitsForSize(itemIds.size());
        int questBits = bitsForSize(Math.max(1, questIds.size()));

        List<BingoCard> cards = new ArrayList<>();
        for (int c = 0; c < cardCount; c++) {
            int cardSize = br.readBits(4);
            if (cardSize <= 0 || cardSize > 10) return null;
            BingoCard card = new BingoCard(cardSize);
            for (int y = 0; y < cardSize; y++) {
                for (int x = 0; x < cardSize; x++) {
                    boolean isQuest = br.readBits(1) == 1;
                    int idx = br.readBits(isQuest ? questBits : itemBits);
                    String id;
                    if (isQuest) {
                        if (idx < 0 || idx >= questIds.size()) return null;
                        id = questIds.get(idx);
                    } else {
                        if (idx < 0 || idx >= itemIds.size()) return null;
                        id = itemIds.get(idx);
                    }
                    BingoSlot slot = resolveSlot(id);
                    if (slot == null) return null;
                    card.setSlot(x, y, slot);
                }
            }
            cards.add(card);
        }

        CompoundTag settings = new CompoundTag();
        com.jamie.jamiebingo.util.NbtUtil.putString(settings, "Win", decodeWin(winOrdinal));
        com.jamie.jamiebingo.util.NbtUtil.putString(settings, "Composition", decodeComposition(compOrdinal));
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "QuestPercent", questPercent);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "Size", size);
        com.jamie.jamiebingo.util.NbtUtil.putString(settings, "Difficulty", decodeDifficulty(difficultyId));
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "GunRounds", gunRounds);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "GunShared", gunShared);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "Rerolls", rerolls);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "EffectsInterval", effectsInterval);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "EffectsArmed", effectsArmed);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "Rtp", rtp);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "HostileMobs", hostile);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "Hunger", hunger);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "Pvp", pvp);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "AdventureMode", false);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "PrelitPortalsMode", com.jamie.jamiebingo.data.BingoGameData.PRELIT_PORTALS_OFF);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "KeepInv", keepInv);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "Hardcore", hardcore);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "Daylight", daylight);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "StartDelay", startDelay);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "CountdownEnabled", countdownEnabled);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "CountdownMinutes", countdownMinutes);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "AllowLateJoin", allowLateJoin);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "RegisterMode", registerMode);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "ShuffleEnabled", false);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "Casino", casino);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "CasinoMode", casino ? BingoGameData.CASINO_ENABLED : BingoGameData.CASINO_DISABLED);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "MinesEnabled", false);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "MineAmount", 1);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "MineTimeSeconds", 15);
        com.jamie.jamiebingo.util.NbtUtil.putString(settings, "GameDifficulty", decodeGameDifficulty(gameDiffId));
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "HangmanRounds", hangmanRounds);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "HangmanBaseSeconds", hangmanBaseSeconds);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "HangmanPenaltySeconds", hangmanPenaltySeconds);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "CustomCardEnabled", false);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "CustomPoolEnabled", false);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "CategoryLogicEnabled", true);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "RarityLogicEnabled", true);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "ItemColorVariantsSeparate", true);

        return new SeedData(settings, cards);
    }

    private static SeedData decodeV3(byte[] raw) throws IOException {
        ItemDatabase.load();
        QuestDatabase.load();

        List<String> itemIds = ItemDatabaseItems.getIds();
        List<String> questIds = QuestDatabaseItems.getIds();

        BitReader br = new BitReader(raw, V3_MAGIC.length);

        int winOrdinal = br.readBits(4);
        int compOrdinal = br.readBits(2);
        int difficultyId = br.readBits(3);
        int size = br.readBits(4);
        int questPercent = br.readBits(7);
        int gunRounds = br.readBits(5);
        boolean gunShared = br.readBits(1) == 1;
        int rerolls = br.readBits(4);
        int effectsInterval = br.readBits(9);
        boolean effectsArmed = br.readBits(1) == 1;
        boolean rtp = br.readBits(1) == 1;
        boolean hostile = br.readBits(1) == 1;
        boolean hunger = br.readBits(1) == 1;
        boolean pvp = br.readBits(1) == 1;
        boolean keepInv = br.readBits(1) == 1;
        boolean hardcore = br.readBits(1) == 1;
        int daylight = br.readBits(3);
        int startDelay = br.readBits(10);
        boolean countdownEnabled = br.readBits(1) == 1;
        int countdownMinutes = br.readBits(8);
        boolean allowLateJoin = br.readBits(1) == 1;
        int registerMode = br.readBits(1) == 1
                ? BingoGameData.REGISTER_ALWAYS_HAVE
                : BingoGameData.REGISTER_COLLECT_ONCE;
        boolean casino = br.readBits(1) == 1;
        int gameDiffId = br.readBits(2);
        int hangmanRounds = br.readBits(5);
        int hangmanBaseSeconds = br.readBits(9);
        int hangmanPenaltySeconds = br.readBits(9);

        int customMode = br.readBits(2);

        int itemCount = br.readBits(16);
        int questCount = br.readBits(16);
        if (itemCount != itemIds.size()) {
            return null;
        }
        if (questCount != Math.max(1, questIds.size())) {
            return null;
        }

        int itemBits = bitsForSize(itemIds.size());
        int questBits = bitsForSize(Math.max(1, questIds.size()));

        int poolCount = br.readBits(12);
        List<String> poolIds = new ArrayList<>();
        for (int i = 0; i < poolCount; i++) {
            boolean isQuest = br.readBits(1) == 1;
            int idx = br.readBits(isQuest ? questBits : itemBits);
            String id;
            if (isQuest) {
                if (idx < 0 || idx >= questIds.size()) return null;
                id = questIds.get(idx);
            } else {
                if (idx < 0 || idx >= itemIds.size()) return null;
                id = itemIds.get(idx);
            }
            poolIds.add(id);
        }

        int cardCount = br.readBits(6);
        if (cardCount <= 0) return null;

        List<BingoCard> cards = new ArrayList<>();
        for (int c = 0; c < cardCount; c++) {
            int cardSize = br.readBits(4);
            if (cardSize <= 0 || cardSize > 10) return null;
            BingoCard card = new BingoCard(cardSize);
            for (int y = 0; y < cardSize; y++) {
                for (int x = 0; x < cardSize; x++) {
                    boolean isQuest = br.readBits(1) == 1;
                    int idx = br.readBits(isQuest ? questBits : itemBits);
                    String id;
                    if (isQuest) {
                        if (idx < 0 || idx >= questIds.size()) return null;
                        id = questIds.get(idx);
                    } else {
                        if (idx < 0 || idx >= itemIds.size()) return null;
                        id = itemIds.get(idx);
                    }
                    BingoSlot slot = resolveSlot(id);
                    if (slot == null) return null;
                    card.setSlot(x, y, slot);
                }
            }
            cards.add(card);
        }

        CompoundTag settings = new CompoundTag();
        com.jamie.jamiebingo.util.NbtUtil.putString(settings, "Win", decodeWin(winOrdinal));
        com.jamie.jamiebingo.util.NbtUtil.putString(settings, "Composition", decodeComposition(compOrdinal));
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "QuestPercent", questPercent);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "Size", size);
        com.jamie.jamiebingo.util.NbtUtil.putString(settings, "Difficulty", decodeDifficulty(difficultyId));
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "GunRounds", gunRounds);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "GunShared", gunShared);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "Rerolls", rerolls);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "EffectsInterval", effectsInterval);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "EffectsArmed", effectsArmed);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "Rtp", rtp);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "HostileMobs", hostile);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "Hunger", hunger);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "Pvp", pvp);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "AdventureMode", false);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "PrelitPortalsMode", com.jamie.jamiebingo.data.BingoGameData.PRELIT_PORTALS_OFF);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "KeepInv", keepInv);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "Hardcore", hardcore);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "Daylight", daylight);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "StartDelay", startDelay);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "CountdownEnabled", countdownEnabled);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "CountdownMinutes", countdownMinutes);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "AllowLateJoin", allowLateJoin);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "RegisterMode", registerMode);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "ShuffleEnabled", false);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "Casino", casino);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "CasinoMode", casino ? BingoGameData.CASINO_ENABLED : BingoGameData.CASINO_DISABLED);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "MinesEnabled", false);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "MineAmount", 1);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "MineTimeSeconds", 15);
        com.jamie.jamiebingo.util.NbtUtil.putString(settings, "GameDifficulty", decodeGameDifficulty(gameDiffId));
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "HangmanRounds", hangmanRounds);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "HangmanBaseSeconds", hangmanBaseSeconds);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "HangmanPenaltySeconds", hangmanPenaltySeconds);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "CustomCardEnabled", customMode == 1);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "CustomPoolEnabled", customMode == 2);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "CategoryLogicEnabled", true);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "RarityLogicEnabled", true);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "ItemColorVariantsSeparate", true);

        ListTag poolTag = new ListTag();
        for (String id : poolIds) {
            poolTag.add(StringTag.valueOf(id == null ? "" : id));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(settings, "CustomPoolIds", poolTag);

        return new SeedData(settings, cards);
    }

    private static BingoSlot resolveSlot(String id) {
        return SlotResolver.resolveSlot(id);
    }

    private static int encodeDifficulty(String diff) {
        if (diff == null) return 1;
        return switch (diff.toLowerCase()) {
            case "easy" -> 0;
            case "hard" -> 2;
            case "extreme" -> 3;
            default -> 1;
        };
    }

    private static String decodeDifficulty(int id) {
        return switch (id) {
            case 0 -> "easy";
            case 2 -> "hard";
            case 3 -> "extreme";
            default -> "normal";
        };
    }

    private static int encodeGameDifficulty(String diff) {
        if (diff == null) return 1;
        return switch (diff.toLowerCase()) {
            case "easy" -> 0;
            case "hard" -> 2;
            default -> 1;
        };
    }

    private static String decodeGameDifficulty(int id) {
        return switch (id) {
            case 0 -> "easy";
            case 2 -> "hard";
            default -> "normal";
        };
    }

    private static String decodeWin(int ordinal) {
        WinCondition[] values = WinCondition.values();
        if (ordinal < 0 || ordinal >= values.length) return WinCondition.LINE.name();
        return values[ordinal].name();
    }

    private static String decodeComposition(int ordinal) {
        CardComposition[] values = CardComposition.values();
        if (ordinal < 0 || ordinal >= values.length) return CardComposition.CLASSIC_ONLY.name();
        return values[ordinal].name();
    }

    private static int bitsForSize(int size) {
        int bits = 1;
        int max = 2;
        while (max < size) {
            bits++;
            max <<= 1;
        }
        return bits;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static Map<String, Integer> indexMap(List<String> ids) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            map.put(ids.get(i), i);
        }
        return map;
    }

    private static final class ItemDatabaseItems {
        private static List<String> cachedIds;
        private static Map<String, ItemDefinition> cachedById;

        private static List<String> getIds() {
            if (cachedIds != null) return cachedIds;
            List<ItemDefinition> items = ItemDatabase.getAllItems();
            List<String> ids = new ArrayList<>();
            Map<String, ItemDefinition> byId = new HashMap<>();
            for (ItemDefinition def : items) {
                if (def == null || def.id() == null || def.id().isBlank()) continue;
                ids.add(def.id());
                byId.put(def.id(), def);
            }
            ids.sort(Comparator.naturalOrder());
            cachedIds = ids;
            cachedById = byId;
            return cachedIds;
        }

        private static ItemDefinition getById(String id) {
            if (cachedById == null) {
                getIds();
            }
            return cachedById.get(id);
        }
    }

    private static final class QuestDatabaseItems {
        private static List<String> cachedIds;

        private static List<String> getIds() {
            if (cachedIds != null) return cachedIds;
            List<String> ids = new ArrayList<>();
            for (QuestDefinition def : QuestDatabase.getQuests()) {
                if (def == null || def.id == null || def.id.isBlank()) continue;
                ids.add(def.id);
            }
            ids.sort(Comparator.naturalOrder());
            cachedIds = ids;
            return cachedIds;
        }
    }

    private static final class BitWriter {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private int current;
        private int bitPos;

        void writeBytes(byte[] bytes) throws IOException {
            flushBits();
            out.write(bytes);
        }

        void writeBits(int value, int bits) {
            for (int i = 0; i < bits; i++) {
                int bit = (value >> i) & 1;
                current |= (bit << bitPos);
                bitPos++;
                if (bitPos == 8) {
                    out.write(current);
                    current = 0;
                    bitPos = 0;
                }
            }
        }

        private void flushBits() {
            if (bitPos > 0) {
                out.write(current);
                current = 0;
                bitPos = 0;
            }
        }

        byte[] toByteArray() {
            flushBits();
            return out.toByteArray();
        }
    }

    private static final class BitReader {
        private final byte[] data;
        private int index;
        private int current;
        private int bitPos;

        BitReader(byte[] data, int start) {
            this.data = data;
            this.index = start;
        }

        int readBits(int bits) throws IOException {
            int value = 0;
            for (int i = 0; i < bits; i++) {
                if (bitPos == 0) {
                    if (index >= data.length) throw new IOException("EOF");
                    current = data[index++] & 0xFF;
                }
                int bit = (current >> bitPos) & 1;
                value |= (bit << i);
                bitPos = (bitPos + 1) & 7;
            }
            return value;
        }
    }
}







