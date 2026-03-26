package com.jamie.jamiebingo.bingo;

import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.menu.ControllerSettingsSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public final class SettingsSeedCodec {
    private static final int VERSION = 1;
    private static final String PREFIX = "JBSET1:";

    private SettingsSeedCodec() {
    }

    public static String encode(BingoGameData data, MinecraftServer server) {
        if (data == null || server == null) return "";
        return encode(snapshotFromGameData(data, server));
    }

    public static String encode(ControllerSettingsSnapshot snapshot) {
        if (snapshot == null) return "";
        try {
            CompoundTag root = new CompoundTag();
            com.jamie.jamiebingo.util.NbtUtil.putInt(root, "v", VERSION);
            com.jamie.jamiebingo.util.NbtUtil.putTag(root, "Settings", createSettingsTag(snapshot));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            NbtIo.writeCompressed(root, out);
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
        } catch (Exception ignored) {
            return "";
        }
    }

    public static String fromCardSeed(String cardSeed) {
        CardSeedCodec.SeedData data = CardSeedCodec.decode(cardSeed);
        if (data == null || data.settings() == null) return "";
        ControllerSettingsSnapshot snapshot = decodeSettings(data.settings());
        return snapshot == null ? "" : encode(snapshot);
    }

    public static ControllerSettingsSnapshot decode(String seed) {
        if (seed == null || seed.isBlank()) return null;
        String raw = seed.trim();
        if (!raw.startsWith(PREFIX)) return null;
        try {
            byte[] compressed = Base64.getUrlDecoder().decode(raw.substring(PREFIX.length()));
            CompoundTag root = NbtIo.readCompressed(new ByteArrayInputStream(compressed), NbtAccounter.unlimitedHeap());
            if (root == null || root.getInt("v").orElse(-1) != VERSION) return null;
            return decodeSettings(root.getCompoundOrEmpty("Settings"));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static CompoundTag createSettingsTag(BingoGameData data, MinecraftServer server) {
        return createSettingsTag(snapshotFromGameData(data, server));
    }

    public static CompoundTag createSettingsTag(ControllerSettingsSnapshot snapshot) {
        CompoundTag settings = new CompoundTag();
        if (snapshot == null) return settings;

        CardComposition composition = switch (snapshot.questMode()) {
            case 1 -> CardComposition.HYBRID_CATEGORY;
            case 2 -> CardComposition.HYBRID_PERCENT;
            default -> CardComposition.CLASSIC_ONLY;
        };

        com.jamie.jamiebingo.util.NbtUtil.putString(settings, "Win", snapshot.win().name());
        com.jamie.jamiebingo.util.NbtUtil.putString(settings, "Composition", composition.name());
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "QuestPercent", snapshot.questPercent());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "CategoryLogicEnabled", snapshot.categoryLogicEnabled());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "RarityLogicEnabled", snapshot.rarityLogicEnabled());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "ItemColorVariantsSeparate", snapshot.itemColorVariantsSeparate());
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "Size", snapshot.cardSize());
        com.jamie.jamiebingo.util.NbtUtil.putString(settings, "Difficulty", snapshot.cardDifficulty() == null ? "" : snapshot.cardDifficulty());
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "GunRounds", snapshot.gunRounds());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "GunShared", false);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "Rerolls", snapshot.rerollsMode() == 1 ? Math.max(0, snapshot.rerollsCount()) : 0);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "EffectsInterval", Math.max(0, snapshot.effectsInterval()));
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "EffectsArmed", snapshot.effectsInterval() > 0);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "Rtp", snapshot.rtpEnabled());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "HostileMobs", snapshot.hostileMobsEnabled());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "Hunger", snapshot.hungerEnabled());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "NaturalRegen", snapshot.naturalRegenEnabled());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "Pvp", snapshot.pvpEnabled());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "AdventureMode", snapshot.adventureMode());
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "PrelitPortalsMode", BingoGameData.clampPrelitPortalsMode(snapshot.prelitPortalsMode()));
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "KeepInv", snapshot.keepInventoryEnabled());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "Hardcore", snapshot.hardcoreEnabled());
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "Daylight", snapshot.daylightMode());
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "StartDelay", snapshot.startDelaySeconds());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "CountdownEnabled", snapshot.countdownEnabled());
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "CountdownMinutes", snapshot.countdownMinutes());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "RushEnabled", snapshot.rushEnabled());
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "RushSeconds", snapshot.rushSeconds());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "AllowLateJoin", snapshot.allowLateJoin());
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "RegisterMode", snapshot.registerMode());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "TeamSyncEnabled", snapshot.teamSyncEnabled());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "TeamChestEnabled", snapshot.teamChestEnabled());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "ShuffleEnabled", snapshot.shuffleMode() == BingoGameData.SHUFFLE_ENABLED);
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "StarterKitMode", snapshot.starterKitMode());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "HideGoalDetailsInChat", snapshot.hideGoalDetailsInChat());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "Casino", snapshot.casino());
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "CasinoMode", snapshot.casinoMode());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "MinesEnabled", snapshot.minesEnabled());
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "MineAmount", snapshot.mineAmount());
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "MineTimeSeconds", snapshot.mineTimeSeconds());
        com.jamie.jamiebingo.util.NbtUtil.putString(settings, "GameDifficulty", snapshot.gameDifficulty() == null ? "" : snapshot.gameDifficulty());
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "HangmanRounds", snapshot.hangmanRounds());
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "HangmanBaseSeconds", snapshot.hangmanBaseSeconds());
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "HangmanPenaltySeconds", snapshot.hangmanPenaltySeconds());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "PowerSlotEnabled", snapshot.powerSlotEnabled());
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "PowerSlotIntervalSeconds", snapshot.powerSlotIntervalSeconds());
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(settings, "FakeRerollsEnabled", snapshot.fakeRerollsEnabled());
        com.jamie.jamiebingo.util.NbtUtil.putInt(settings, "FakeRerollsPerPlayer", snapshot.fakeRerollsPerPlayer());
        return settings;
    }

    public static ControllerSettingsSnapshot decodeSettings(CompoundTag settings) {
        if (settings == null) return null;
        WinCondition win = parseEnum(settings.getString("Win").orElse("FULL"), WinCondition.class, WinCondition.FULL);
        CardComposition composition = parseEnum(settings.getString("Composition").orElse("HYBRID_CATEGORY"), CardComposition.class, CardComposition.HYBRID_CATEGORY);
        int questMode = composition == CardComposition.HYBRID_CATEGORY ? 1 : composition == CardComposition.HYBRID_PERCENT ? 2 : 0;
        int casinoMode = settings.contains("CasinoMode")
                ? settings.getInt("CasinoMode").orElse(settings.getBoolean("Casino").orElse(false) ? 1 : 0)
                : (settings.getBoolean("Casino").orElse(false) ? 1 : 0);
        int rerolls = Math.max(0, settings.getInt("Rerolls").orElse(0));

        return new ControllerSettingsSnapshot(
                win,
                questMode,
                settings.getInt("QuestPercent").orElse(50),
                settings.getBoolean("CategoryLogicEnabled").orElse(false),
                settings.getBoolean("RarityLogicEnabled").orElse(true),
                settings.getBoolean("ItemColorVariantsSeparate").orElse(false),
                settings.getBoolean("Casino").orElse(casinoMode != 0),
                casinoMode,
                rerolls > 0 ? 1 : 0,
                rerolls,
                settings.getInt("GunRounds").orElse(8),
                settings.getInt("HangmanRounds").orElse(5),
                settings.getInt("HangmanBaseSeconds").orElse(120),
                settings.getInt("HangmanPenaltySeconds").orElse(60),
                settings.getString("Difficulty").orElse("normal"),
                settings.getString("GameDifficulty").orElse("normal"),
                Math.max(0, settings.getInt("EffectsInterval").orElse(0)),
                settings.getBoolean("Rtp").orElse(false),
                false,
                settings.getBoolean("HostileMobs").orElse(false),
                false,
                settings.getBoolean("Hunger").orElse(false),
                settings.contains("NaturalRegen")
                        ? settings.getBoolean("NaturalRegen").orElse(true)
                        : settings.getBoolean("NaturalRegenEnabled").orElse(true),
                false,
                false,
                Math.max(1, Math.min(10, settings.getInt("Size").orElse(5))),
                false,
                settings.getBoolean("KeepInv").orElse(false),
                false,
                settings.getBoolean("Hardcore").orElse(false),
                false,
                settings.getInt("Daylight").orElse(0),
                false,
                Math.max(0, settings.getInt("StartDelay").orElse(0)),
                settings.getBoolean("CountdownEnabled").orElse(false),
                Math.max(0, settings.getInt("CountdownMinutes").orElse(0)),
                settings.getBoolean("RushEnabled").orElse(false),
                Math.max(1, settings.getInt("RushSeconds").orElse(60)),
                settings.getBoolean("AllowLateJoin").orElse(true),
                settings.getBoolean("Pvp").orElse(false),
                settings.getBoolean("AdventureMode").orElse(false),
                BingoGameData.clampPrelitPortalsMode(settings.getInt("PrelitPortalsMode").orElse(BingoGameData.PRELIT_PORTALS_OFF)),
                false,
                settings.getInt("RegisterMode").orElse(BingoGameData.REGISTER_COLLECT_ONCE),
                false,
                settings.getBoolean("TeamSyncEnabled").orElse(true),
                settings.getBoolean("TeamChestEnabled").orElse(true),
                settings.getBoolean("ShuffleEnabled").orElse(false)
                        ? BingoGameData.SHUFFLE_ENABLED
                        : BingoGameData.SHUFFLE_DISABLED,
                settings.getInt("StarterKitMode").orElse(BingoGameData.STARTER_KIT_DISABLED),
                settings.getBoolean("HideGoalDetailsInChat").orElse(false),
                settings.getBoolean("MinesEnabled").orElse(false),
                Math.max(1, settings.getInt("MineAmount").orElse(1)),
                Math.max(1, settings.getInt("MineTimeSeconds").orElse(15)),
                settings.getBoolean("PowerSlotEnabled").orElse(false),
                Math.max(10, settings.getInt("PowerSlotIntervalSeconds").orElse(60)),
                settings.getBoolean("FakeRerollsEnabled").orElse(false),
                Math.max(1, settings.getInt("FakeRerollsPerPlayer").orElse(2))
        );
    }

    private static ControllerSettingsSnapshot snapshotFromGameData(BingoGameData data, MinecraftServer server) {
        CardComposition composition = data.composition == null ? CardComposition.HYBRID_CATEGORY : data.composition;
        int questMode = composition == CardComposition.HYBRID_CATEGORY ? 1 : composition == CardComposition.HYBRID_PERCENT ? 2 : 0;
        return new ControllerSettingsSnapshot(
                data.winCondition == null ? WinCondition.FULL : data.winCondition,
                questMode,
                data.questPercent,
                data.categoryLogicEnabled,
                data.rarityLogicEnabled,
                data.itemColorVariantsSeparate,
                data.casinoMode != BingoGameData.CASINO_DISABLED,
                data.casinoMode,
                data.rerollsPerPlayer > 0 ? 1 : 0,
                data.rerollsPerPlayer,
                data.gunGameLength,
                data.hangmanRounds,
                data.hangmanBaseSeconds,
                data.hangmanPenaltySeconds,
                data.difficulty == null || data.difficulty.isBlank() ? "normal" : data.difficulty,
                com.jamie.jamiebingo.util.ServerWorldDataUtil.getDifficultyKey(server),
                data.randomEffectsIntervalSeconds,
                data.rtpEnabled,
                false,
                data.hostileMobsEnabled,
                false,
                data.hungerEnabled,
                data.naturalRegenEnabled,
                false,
                false,
                data.size,
                false,
                data.keepInventoryEnabled,
                false,
                data.hardcoreEnabled,
                false,
                data.daylightMode,
                false,
                data.bingoStartDelaySeconds,
                data.countdownEnabled,
                data.countdownMinutes,
                data.rushEnabled,
                data.rushSeconds,
                data.allowLateJoin,
                data.pvpEnabled,
                data.adventureMode,
                BingoGameData.clampPrelitPortalsMode(data.prelitPortalsMode),
                false,
                data.registerMode,
                false,
                data.teamSyncEnabled,
                data.teamChestEnabled,
                data.shuffleEnabled ? BingoGameData.SHUFFLE_ENABLED : BingoGameData.SHUFFLE_DISABLED,
                data.starterKitMode,
                data.hideGoalDetailsInChat,
                data.minesEnabled,
                data.mineAmount,
                data.mineTimeSeconds,
                data.powerSlotEnabled,
                data.powerSlotIntervalSeconds,
                data.fakeRerollsEnabled,
                data.fakeRerollsPerPlayer
        );
    }

    private static <E extends Enum<E>> E parseEnum(String value, Class<E> type, E fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
