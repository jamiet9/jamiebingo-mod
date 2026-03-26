package com.jamie.jamiebingo.data;


import com.jamie.jamiebingo.util.GameRulesUtil;
import com.jamie.jamiebingo.util.ServerLevelUtil;
import com.jamie.jamiebingo.bingo.*;
import com.jamie.jamiebingo.quest.QuestDatabase;
import com.jamie.jamiebingo.quest.QuestDefinition;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.jamie.jamiebingo.quest.QuestEvents;
import com.jamie.jamiebingo.quest.QuestTracker;
import com.jamie.jamiebingo.rtp.BingoRtpHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.PacketFlashSlots;
import com.jamie.jamiebingo.sound.ModSounds;
import com.jamie.jamiebingo.network.PacketPlayTeamSound;
import com.jamie.jamiebingo.network.packet.PacketVoteEndGame;
import com.jamie.jamiebingo.network.packet.PacketVoteRerollCard;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Supplier;

public class BingoGameData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long CARD_GENERATION_TIMEOUT_NANOS =
            java.util.concurrent.TimeUnit.SECONDS.toNanos(30);

    /* =========================
       GAME STATE
       ========================= */

/* =========================
   RTP (ADDON)
   ========================= */

public boolean rtpEnabled = false;

// RTP â€” queued on game start
public boolean pendingGameStartRtp = false;

/* =========================
   WORLD RULE ADDONS
   ========================= */

public boolean hungerEnabled = true;
public boolean naturalRegenEnabled = true;
public boolean hostileMobsEnabled = true;
public boolean pvpEnabled = true;
public boolean adventureMode = false;
public int prelitPortalsMode = PRELIT_PORTALS_OFF;
public int daylightMode = DAYLIGHT_ENABLED;
public boolean randomDaylightIntent = false;
public boolean allowLateJoin = true;
public boolean teamChestEnabled = true;
public boolean customCardEnabled = false;
public boolean customPoolEnabled = false;

private boolean generationTimedOut = false;

public static final int DAYLIGHT_ENABLED = 0;
public static final int DAYLIGHT_DAY = 1;
public static final int DAYLIGHT_NIGHT = 2;
public static final int DAYLIGHT_MIDNIGHT = 3;
public static final int DAYLIGHT_DAWN = 4;
public static final int DAYLIGHT_DUSK = 5;
public static final int PRELIT_PORTALS_OFF = 0;
public static final int PRELIT_PORTALS_NETHER = 1;
public static final int PRELIT_PORTALS_END = 2;
public static final int PRELIT_PORTALS_BOTH = 3;


// Toggle + timing
public boolean randomEffectsEnabled = false;
public boolean randomEffectsArmed = false; // âœ… configured, but waits for game start
public int randomEffectsIntervalSeconds = 0;
public int randomEffectsNextTick = -1;

// Active effect state (server-authoritative)
public String activeRandomEffectId = "";
public String activeRandomEffectName = "";
public int activeRandomEffectAmplifier = 0;

// The effect that is currently applied to players (so we can remove it next time)
public String appliedRandomEffectId = "";

// Hostile mobs / difficulty tracking
public String lastNonPeacefulDifficulty = "normal";

// ===============================
// CORE GAME SETTINGS
// ===============================
public int cardSize = 5;               // 1â€“10, -1 = random

// ===============================
// EFFECTS
// ===============================
public int effectInterval = 60;         // seconds, -1 = random

// ===============================
// START FLOW
// ===============================
public int startDelaySeconds = 0;       // no random
public boolean countdownEnabled = false;
public int countdownMinutes = 10;
public boolean rushEnabled = false;
public int rushSeconds = 60;
public int hangmanRounds = 5;
public int hangmanBaseSeconds = 120;
public int hangmanPenaltySeconds = 60;

// ===============================
// PREGAME BOX
// ===============================
  public boolean pregameBoxBuilt = false;
  public boolean pregameBoxActive = false;
  public int pregameBoxX = 0;
  public int pregameBoxY = 0;
  public int pregameBoxZ = 0;
  public boolean lastGameSpawnSet = false;
  public int lastGameSpawnX = 0;
  public int lastGameSpawnY = 0;
  public int lastGameSpawnZ = 0;
  public boolean gameStartSpawnPrepared = false;
  public int gameStartSpawnX = 0;
  public int gameStartSpawnY = 0;
  public int gameStartSpawnZ = 0;
  public int gameStartSpawnChunkRadius = 15;
  public boolean gameStartSpawnLoading = false;
  public int gameStartSpawnLoadingIndex = 0;
  public int gameStartSpawnLoadingTotal = 0;

  public boolean worldUseNewSeedEachGame = true;
  public int worldTypeMode = WORLD_TYPE_NORMAL;
  public boolean worldSmallBiomes = false;
  public int worldCustomBiomeSizeBlocks = 96;
  public int worldTerrainHillinessPercent = 50;
  public int worldStructureFrequencyPercent = 100;
  public String worldSingleBiomeId = "minecraft:plains";
  public boolean worldSurfaceCaveBiomes = false;
  public String worldSetSeedText = "";
  public boolean worldRegenInProgress = false;
    public boolean worldRegenQueued = false;
    public String worldRegenStage = "";
    public boolean worldFreshSeedPrepared = false;
    public boolean pendingStartAfterWorldRegen = false;
    public boolean pendingWeeklyChallengeStart = false;
    public long pendingWeeklyChallengeBaseSeed = 0L;
    public String activeWeeklyChallengeId = "";
    public String activeWeeklySettingsSeed = "";
    public String activeWeeklyWorldSeed = "";
    public String activeWeeklyCardSeed = "";

  public static final int WORLD_TYPE_NORMAL = 0;
  public static final int WORLD_TYPE_AMPLIFIED = 1;
  public static final int WORLD_TYPE_SUPERFLAT = 2;
  public static final int WORLD_TYPE_CUSTOM_BIOME_SIZE = 3;
  public static final int WORLD_TYPE_SINGLE_BIOME = 4;
  // Legacy-only value from older versions; mapped to custom biome size mode.
  public static final int WORLD_TYPE_SMALL_BIOMES = 5;

// ===============================
// INTERNAL FLAGS (OPTIONAL BUT SAFE)
// ===============================
  public boolean resolvedRandoms = false;
  public boolean startCountdownActive = false;
  public int startCountdownSeconds = 0;
  public int startCountdownEndTick = -1;
  public boolean stopGamePending = false;
  public int stopGamePendingTick = -1;

  public static boolean isCasinoAllowedForWin(WinCondition winCondition) {
      if (winCondition == null) return true;
      return winCondition != WinCondition.BLIND
              && winCondition != WinCondition.HANGMAN
              && winCondition != WinCondition.GUNGAME
              && winCondition != WinCondition.GAMEGUN;
  }

// ===============================
// GAME TIMER
// ===============================
public int gameStartTick = -1;
public int countdownEndTick = -1;
public int resumeElapsedSeconds = -1;
public int resumeCountdownRemainingSeconds = -1;
private transient boolean timerResumePending = false;
public boolean countdownExpired = false;
// ===============================
// WIN END DELAY (BLIND REVEAL)
// ===============================
public boolean pendingWinEndActive = false;
public int pendingWinEndTick = -1;

// ===============================
// SPECTATORS / PARTICIPANTS
// ===============================
public final Set<UUID> participants = new HashSet<>();
public final Set<UUID> spectators = new HashSet<>();
public final Map<UUID, UUID> spectatorViewTargets = new HashMap<>();
public final Set<UUID> lateJoinPending = new HashSet<>();

// ===============================
// HANGMAN RUNTIME
// ===============================
public final List<BingoCard> hangmanCards = new ArrayList<>();
public int hangmanRoundIndex = 0;
public int hangmanNextRevealTick = -1;
public int hangmanRevealCount = 0;
public String hangmanCurrentSlotId = "";
public String hangmanCurrentWord = "";
public String hangmanMaskedWord = "";
public boolean hangmanSlotRevealed = false;
public int hangmanIntermissionEndTick = -1;

// =========================
// RANDOM EFFECTS (CUSTOM)
// =========================

// If non-empty â†’ a custom effect is scheduled
public String activeCustomEffectId = "";

// Currently applied custom effect (so we can clean it up)
public String appliedCustomEffectId = "";

// Random effect history (runtime only)
public String lastRandomEffectKey = "";
public final Map<String, Integer> randomEffectUseCounts = new HashMap<>();

    public boolean active = false;
    public BingoCard currentCard;
    public String lastPlayedSeed = "";
    private final Map<UUID, List<GameHistoryEntry>> playerGameHistory = new HashMap<>();
    private boolean currentRunEndCaptured = false;
    private int currentRunPreviewSize = 0;
    private final List<String> currentRunPreviewSlotIds = new ArrayList<>();
    private boolean currentRunCommandsUsed = false;
    private boolean currentRunVoteRerollUsed = false;
    private static final int MAX_HISTORY_PER_PLAYER = 100;

// ðŸ”« GunGame: shared card sequence
private final List<BingoCard> gunGameCards = new ArrayList<>();

// ðŸ”« GUNGAME: team â†’ cardIndex â†’ completed slot IDs
private final Map<UUID, Map<Integer, Set<String>>> gunGameProgress = new HashMap<>();

public BingoCard getActiveCardForTeam(UUID teamId) {
    if (teamId != null) {
        BingoCard teamCard = shuffleCardsByTeam.get(teamId);
        if (teamCard != null) {
            return teamCard;
        }
    }

    if (winCondition != WinCondition.GUNGAME) {
        return currentCard;
    }

    int idx = gunGameTeamIndex.getOrDefault(teamId, 0);
    if (idx < 0 || idx >= gunGameCards.size()) return null;

    return gunGameCards.get(idx);
}

public BingoCard getOrCreateTeamCardOverride(UUID teamId) {
    if (teamId == null) return currentCard;
    BingoCard existing = shuffleCardsByTeam.get(teamId);
    if (existing != null) return existing;
    if (currentCard == null) return null;
    BingoCard copy = copyCard(currentCard);
    if (copy == null) return null;
    shuffleCardsByTeam.put(teamId, copy);
    return copy;
}

private static boolean isShuffleSupportedWinCondition(WinCondition condition) {
    return condition == WinCondition.FULL
            || condition == WinCondition.LOCKOUT
            || condition == WinCondition.RARITY;
}

public static boolean isPowerSlotSupportedWinCondition(WinCondition condition) {
    return condition != WinCondition.LOCKOUT
            && condition != WinCondition.RARITY
            && condition != WinCondition.GAMEGUN;
}

public static boolean isFakeRerollsSupportedWinCondition(WinCondition condition) {
    return condition != WinCondition.HANGMAN
            && condition != WinCondition.GUNGAME
            && condition != WinCondition.GAMEGUN
            && condition != WinCondition.BLIND;
}

private boolean isShuffleActive() {
    return shuffleEnabled && isShuffleSupportedWinCondition(winCondition);
}

private void clearShuffleRuntime() {
    shuffleCardsByTeam.clear();
    shuffleStepsByTeam.clear();
    shuffleQueueBySlot.clear();
    shuffleSeenIdsByTeam.clear();
    shuffleSeenIdsShared.clear();
}

private static BingoSlot copySlot(BingoSlot slot) {
    if (slot == null) return null;
    return new BingoSlot(slot.getId(), slot.getName(), slot.getCategory(), slot.getRarity());
}

private static BingoCard copyCard(BingoCard source) {
    if (source == null) return null;
    BingoCard copy = new BingoCard(source.getSize());
    for (int y = 0; y < source.getSize(); y++) {
        for (int x = 0; x < source.getSize(); x++) {
            copy.setSlot(x, y, copySlot(source.getSlot(x, y)));
        }
    }
    return copy;
}

private static int maxPossibleLinesForSize(int size) {
    if (size <= 0) return 0;
    return (size * 2) + 2; // rows + cols + 2 diagonals
}

private void initializeShuffleSeenState(MinecraftServer server) {
    shuffleSeenIdsByTeam.clear();
    shuffleSeenIdsShared.clear();
    if (server == null || !isShuffleActive() || currentCard == null) return;
    Set<String> baseSeen = new HashSet<>(currentCard.getAllIds());
    if (winCondition == WinCondition.FULL) {
        TeamData teams = TeamData.get(server);
        for (TeamData.TeamInfo team : teams.getTeams()) {
            if (team == null || team.id == null || team.members.isEmpty()) continue;
            shuffleSeenIdsByTeam.put(team.id, new HashSet<>(baseSeen));
        }
        return;
    }
    if (winCondition == WinCondition.LOCKOUT || winCondition == WinCondition.RARITY) {
        shuffleSeenIdsShared.addAll(baseSeen);
    }
}

private void rebuildShuffleQueuesFromCurrentCard(MinecraftServer server) {
    clearShuffleRuntime();
    if (server == null || currentCard == null) return;
    if (!isShuffleActive() || winCondition != WinCondition.FULL) return;

    int queueDepth = Math.max(0, maxPossibleLinesForSize(currentCard.getSize()) - 1);
    if (queueDepth <= 0) return;

    Random rng = new Random();
    Set<String> generationBlacklist = buildGenerationBlacklist();

    for (int i = 0; i < queueDepth; i++) {
        BingoCard nextCard;
        if (customPoolEnabled && winCondition != WinCondition.HANGMAN
                && winCondition != WinCondition.GUNGAME
                && winCondition != WinCondition.GAMEGUN) {
            nextCard = buildCardFromPool(size, rng);
        } else if (customCardEnabled && winCondition != WinCondition.HANGMAN
                && winCondition != WinCondition.GUNGAME
                && winCondition != WinCondition.GAMEGUN) {
            nextCard = buildCustomCardFromSlots();
        } else {
            nextCard = com.jamie.jamiebingo.bingo.ConfigurableCardGenerator.generate(
                    size,
                    difficulty,
                    composition,
                    questPercent,
                    server,
                    this,
                    generationBlacklist
            );
        }

        if (nextCard == null || nextCard.getSize() != currentCard.getSize()) {
            continue;
        }

        int n = nextCard.getSize();
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int idx = y * n + x;
                BingoSlot slot = nextCard.getSlot(x, y);
                if (slot == null) continue;
                shuffleQueueBySlot.computeIfAbsent(idx, k -> new ArrayList<>()).add(copySlot(slot));
            }
        }
    }
}

private void initializeFullShuffleTeamCards(MinecraftServer server) {
    if (server == null || currentCard == null) return;
    if (!isShuffleActive() || winCondition != WinCondition.FULL) return;

    TeamData teams = TeamData.get(server);
    for (TeamData.TeamInfo team : teams.getTeams()) {
        if (team == null || team.id == null || team.members.isEmpty()) continue;
        shuffleCardsByTeam.put(team.id, copyCard(currentCard));
        shuffleStepsByTeam.put(team.id, new int[currentCard.getSize() * currentCard.getSize()]);
    }
}

public List<BingoCard> getGunGameCardsSnapshot() {
    return new ArrayList<>(gunGameCards);
}

public void setGunGameCardsFromSeed(List<BingoCard> cards) {
    gunGameCards.clear();
    if (cards != null) {
        gunGameCards.addAll(cards);
    }
}
/* =========================
   GUNGAME STATE
   ========================= */

/** How many cards total this GunGame session has */
public int gunGameLength = 0;

/** If true = GAMEGUN (shared), false = GUNGAME (per-team) */
public boolean gunGameShared = false;

/** Team â†’ current card index (GUNGAME only) */
private final Map<UUID, Integer> gunGameTeamIndex = new HashMap<>();

/** Shared card index (GAMEGUN only) */
public int gunGameSharedIndex = 0;

    /* =========================
       CONFIGURATION
       ========================= */

    public int size = 5;
    public String difficulty = "normal";

/* =========================
   RANDOM INTENT FLAGS
   ========================= */

public boolean randomSizeIntent = false;
public boolean randomWinConditionIntent = false;
public boolean randomDifficultyIntent = false;
public boolean randomRerollsIntent = false;

public boolean randomHostileMobsIntent = false;
public boolean randomHungerIntent = false;
public boolean randomRtpIntent = false;
public boolean randomPvpIntent = false;

public boolean randomEffectsIntervalIntent = false;

public boolean randomHardcoreIntent = false;
public boolean randomKeepInventoryIntent = false;

/* =========================
   NEW GLOBAL SETTINGS
   ========================= */

public boolean hardcoreEnabled = false;
public boolean keepInventoryEnabled = false;
public int registerMode = REGISTER_COLLECT_ONCE;
public boolean teamSyncEnabled = true;
public boolean shuffleEnabled = false;
public boolean randomShuffleIntent = false;

public static final int REGISTER_COLLECT_ONCE = 0;
public static final int REGISTER_ALWAYS_HAVE = 1;
public static final int SHUFFLE_DISABLED = 0;
public static final int SHUFFLE_ENABLED = 1;
public static final int SHUFFLE_RANDOM = 2;

/** Game start delay in seconds (0 = disabled) */
public int bingoStartDelaySeconds = 0;

    public BingoMode mode = BingoMode.CLASSIC;
public CardComposition composition = CardComposition.HYBRID_CATEGORY;
public WinCondition winCondition = WinCondition.FULL;

public int questPercent = -1;
public boolean categoryLogicEnabled = false;
public boolean rarityLogicEnabled = true;
public boolean itemColorVariantsSeparate = false;

    /** ðŸŽ² Casino rerolls per player */
    public int rerollsPerPlayer = 0;
    public static final int CASINO_DISABLED = 0;
    public static final int CASINO_ENABLED = 1;
    public static final int CASINO_DRAFT = 2;
    public int casinoMode = CASINO_DISABLED;
    public boolean minesEnabled = false;
    public int mineAmount = 1;
    public int mineTimeSeconds = 15;
    public boolean powerSlotEnabled = false;
    public int powerSlotIntervalSeconds = 60;
    public boolean fakeRerollsEnabled = false;
    public int fakeRerollsPerPlayer = 2;
    public boolean hideGoalDetailsInChat = false;
    public int starterKitMode = STARTER_KIT_DISABLED;

    public static final int STARTER_KIT_DISABLED = 0;
    public static final int STARTER_KIT_MINIMAL = 1;
    public static final int STARTER_KIT_AVERAGE = 2;
    public static final int STARTER_KIT_OP = 3;

    private final Map<UUID, Integer> rerollsUsed = new HashMap<>();

    /* =========================
       REROLL STATE
       ========================= */

    private boolean rerollPhaseActive = false;
    private final List<UUID> rerollTurnOrder = new ArrayList<>();
    private int rerollTurnIndex = 0;
    private boolean fakeRerollPhaseActive = false;
    private final List<UUID> fakeRerollTurnOrder = new ArrayList<>();
    private int fakeRerollTurnIndex = 0;
    private final Map<UUID, Integer> fakeRerollsUsed = new HashMap<>();
    private final Map<Integer, BingoSlot> fakeRealSlotByIndex = new HashMap<>();
    private final Map<Integer, UUID> fakeSourceTeamByIndex = new HashMap<>();
    private final Map<Integer, String> fakeSourcePlayerNameByIndex = new HashMap<>();
    private final Map<UUID, Set<Integer>> fakeRevealedIndicesByTeam = new HashMap<>();
    private final Map<UUID, Set<Integer>> fakeRevealPendingIndicesByTeam = new HashMap<>();
    private final Map<UUID, Set<Integer>> fakeChosenIndicesByTeam = new HashMap<>();

    private static final int FAKE_REVEAL_PRE_TICKS = 60;
    private static final int FAKE_REVEAL_POST_TICKS = 60;

    private static final String DATA_NAME = "jamie_bingo_data";
    private static final Codec<BingoGameData> CODEC = Codec.of(
            new Encoder<>() {
                @Override
                public <T> DataResult<T> encode(BingoGameData data, DynamicOps<T> ops, T prefix) {
                    CompoundTag tag = new CompoundTag();
                    data.save(tag);
                    return DataResult.success(com.jamie.jamiebingo.util.NbtOpsUtil.instance().convertTo(ops, tag));
                }
            },
            new Decoder<>() {
                @Override
                public <T> DataResult<Pair<BingoGameData, T>> decode(DynamicOps<T> ops, T input) {
                    CompoundTag tag = (CompoundTag) ops.convertTo(com.jamie.jamiebingo.util.NbtOpsUtil.instance(), input);
                    return DataResult.success(Pair.of(load(tag), input));
                }
            }
    );
    private static final SavedDataType<BingoGameData> TYPE =
            new SavedDataType<>(DATA_NAME, BingoGameData::new, CODEC, DataFixTypes.LEVEL);
    private static final java.util.Map<MinecraftServer, BingoGameData> FALLBACK_BY_SERVER =
            new java.util.WeakHashMap<>();

    /* =========================
       PROGRESS
       ========================= */

    public final Set<String> progress = new HashSet<>();

    /** teamId â†’ completed slot IDs */
    private final Map<UUID, Set<String>> teamProgress = new HashMap<>();

    /** slotId â†’ owning teams */
    private final Map<String, Set<UUID>> slotOwners = new HashMap<>();

    /** teamId â†’ revealed slot IDs (BLIND mode only) */
    private final Map<UUID, Set<String>> revealedSlots = new HashMap<>();

    /** teamId â†’ highlighted slot IDs */
    private final Map<UUID, Set<String>> highlightedSlots = new HashMap<>();

    /** Shuffle mode runtime: team -> current card view. */
    private final Map<UUID, BingoCard> shuffleCardsByTeam = new HashMap<>();
    /** Shuffle mode runtime: team -> per-slot queue progress. */
    private final Map<UUID, int[]> shuffleStepsByTeam = new HashMap<>();
    /** Shuffle mode runtime: row-major slot index -> queued replacement slots. */
    private final Map<Integer, List<BingoSlot>> shuffleQueueBySlot = new HashMap<>();
    /** Shuffle mode runtime: team -> all slot IDs that have ever appeared for that team this game. */
    private final Map<UUID, Set<String>> shuffleSeenIdsByTeam = new HashMap<>();
    /** Shuffle mode runtime for shared shuffle (lockout/rarity): all IDs that have ever appeared this game. */
    private final Set<String> shuffleSeenIdsShared = new HashSet<>();
    /** Permanently removed goals that should never be generated. */
    private static final Set<String> PERMANENTLY_REMOVED_SLOT_IDS = Set.of(
            "quest.break_any_piece_of_armor"
    );
    /** Runtime guard so a player is only granted the starter kit once per game. */
    private final Set<UUID> starterKitGrantedPlayers = new HashSet<>();

    /** playerId â†’ quests locked until first release from pregame box */
    private final Set<UUID> questReleasePending = new HashSet<>();

    /** Custom card draft slots (10x10, row-major). Empty = "" */
    public final List<String> customCardSlots = new ArrayList<>();

    /** Custom pool entries (item ids and quest ids). */
    public final List<String> customPoolIds = new ArrayList<>();
    /** Optional custom mine quest-id pool used by mines mode. */
    public final List<String> customMineIds = new ArrayList<>();
    /** Global card-generation blacklist (item ids + quest ids). */
    public final Set<String> blacklistedSlotIds = new HashSet<>();
    /** Global card-generation whitelist (item ids + quest ids). Empty = allow all. */
    public final Set<String> whitelistedSlotIds = new HashSet<>();
    /** Global rarity overrides merged from active player presets. */
    public final Map<String, String> rarityOverrides = new HashMap<>();
    /** Runtime player -> blacklist ids used to build the effective global blacklist union. */
    private final transient Map<UUID, Set<String>> playerBlacklistIds = new HashMap<>();
    /** Runtime player -> whitelist ids used to build the effective global whitelist union. */
    private final transient Map<UUID, Set<String>> playerWhitelistIds = new HashMap<>();
    /** Runtime player -> rarity overrides used to build the effective global override map. */
    private final transient Map<UUID, Map<String, String>> playerRarityOverrides = new HashMap<>();

    /** Hardcore: eliminated players */
    private final Set<UUID> eliminatedPlayers = new HashSet<>();

    /** Hardcore: eliminated teams */
    private final Set<UUID> eliminatedTeams = new HashSet<>();
    /** Rush: team -> deadline tick (starts after first completion) */
    private final Map<UUID, Integer> rushDeadlineTickByTeam = new HashMap<>();
    /** Rush resume cache loaded from disk: team -> remaining seconds */
    private final Map<UUID, Integer> rushResumeSecondsByTeam = new HashMap<>();
    /** Mines resume cache: active mine source quest IDs for the current game. */
    private final List<String> mineResumeSourceIds = new ArrayList<>();
    /** Mines resume cache: player -> remaining seconds on mine countdown. */
    private final Map<UUID, Integer> mineResumeRemainingSecondsByPlayer = new HashMap<>();
    /** Mines resume cache: player -> source quest ID that triggered their mine. */
    private final Map<UUID, String> mineResumeTriggeredSourceByPlayer = new HashMap<>();
    /** Mines resume cache: player -> progress for TAKE_100_DAMAGE mine. */
    private final Map<UUID, Integer> mineResumeDamageProgressByPlayer = new HashMap<>();
    /** Mines resume cache: current defuse goal state. */
    private String mineResumeDefuseQuestId = "";
    private String mineResumeDefuseDisplayName = "";
    /** Power slot resume cache: active slot id/display and time remaining. */
    private String powerResumeSlotId = "";
    private String powerResumeDisplayName = "";
    private int powerResumeRemainingSeconds = -1;
    private boolean powerResumeClaimed = false;

    public BingoGameData() {}

    public void ensureCustomCardSlotsSize(int size) {
        if (size <= 0) return;
        while (customCardSlots.size() < size) {
            customCardSlots.add("");
        }
    }

    public void clearCustomCardSlots() {
        customCardSlots.clear();
    }

    public void clearCustomPool() {
        customPoolIds.clear();
    }

    public void clearCustomMines() {
        customMineIds.clear();
    }

    public void setBlacklistedSlotIds(Collection<String> ids) {
        blacklistedSlotIds.clear();
        if (ids == null) return;
        for (String id : ids) {
            if (id == null || id.isBlank()) continue;
            blacklistedSlotIds.add(id);
        }
        if (!blacklistedSlotIds.isEmpty()) {
            playerBlacklistIds.clear();
        }
    }

    public void setWhitelistedSlotIds(Collection<String> ids) {
        whitelistedSlotIds.clear();
        if (ids == null) return;
        for (String id : ids) {
            if (id == null || id.isBlank()) continue;
            whitelistedSlotIds.add(id);
        }
        if (!whitelistedSlotIds.isEmpty()) {
            playerWhitelistIds.clear();
        }
    }

    public void setPlayerSlotListSettings(UUID playerId, Collection<String> blacklistIds, Collection<String> whitelistIds) {
        if (playerId == null) return;
        Set<String> cleanedBlacklist = cleanIdSet(blacklistIds);
        Set<String> cleanedWhitelist = cleanIdSet(whitelistIds);
        if (cleanedBlacklist.isEmpty()) {
            playerBlacklistIds.remove(playerId);
        } else {
            playerBlacklistIds.put(playerId, cleanedBlacklist);
        }
        if (cleanedWhitelist.isEmpty()) {
            playerWhitelistIds.remove(playerId);
        } else {
            playerWhitelistIds.put(playerId, cleanedWhitelist);
        }
        recomputeSlotListsFromPlayers();
    }

    public void setPlayerBlacklistedSlotIds(UUID playerId, Collection<String> ids) {
        setPlayerSlotListSettings(playerId, ids, getPlayerWhitelistedSlotIds(playerId));
    }

    public void clearPlayerBlacklistedSlotIds(UUID playerId) {
        if (playerId == null) return;
        boolean changed = playerBlacklistIds.remove(playerId) != null;
        changed |= playerWhitelistIds.remove(playerId) != null;
        if (changed) {
            recomputeSlotListsFromPlayers();
        }
    }

    public Set<String> getPlayerBlacklistedSlotIds(UUID playerId) {
        if (playerId == null) return Set.of();
        Set<String> ids = playerBlacklistIds.get(playerId);
        if (ids == null || ids.isEmpty()) return Set.of();
        return new HashSet<>(ids);
    }

    public Set<String> getPlayerWhitelistedSlotIds(UUID playerId) {
        if (playerId == null) return Set.of();
        Set<String> ids = playerWhitelistIds.get(playerId);
        if (ids == null || ids.isEmpty()) return Set.of();
        return new HashSet<>(ids);
    }

    private void recomputeSlotListsFromPlayers() {
        blacklistedSlotIds.clear();
        whitelistedSlotIds.clear();
        for (Set<String> ids : playerBlacklistIds.values()) {
            if (ids == null || ids.isEmpty()) continue;
            blacklistedSlotIds.addAll(ids);
        }
        for (Set<String> ids : playerWhitelistIds.values()) {
            if (ids == null || ids.isEmpty()) continue;
            whitelistedSlotIds.addAll(ids);
        }
    }

    public Set<String> getBlacklistedSlotIds() {
        return new HashSet<>(blacklistedSlotIds);
    }

    public Set<String> getWhitelistedSlotIds() {
        return new HashSet<>(whitelistedSlotIds);
    }

    public boolean isSlotBlacklisted(String id) {
        return id != null && !id.isBlank() && blacklistedSlotIds.contains(id);
    }

    public boolean isSlotBlocked(String id) {
        if (id == null || id.isBlank()) return false;
        if (PERMANENTLY_REMOVED_SLOT_IDS.contains(id)) return true;
        if (blacklistedSlotIds.contains(id)) return true;
        return !whitelistedSlotIds.isEmpty() && !whitelistedSlotIds.contains(id);
    }

    public void setPlayerRarityOverrides(UUID playerId, Map<String, String> overrides) {
        if (playerId == null) return;
        Map<String, String> cleaned = new HashMap<>();
        if (overrides != null) {
            for (Map.Entry<String, String> entry : overrides.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) continue;
                String rarity = BingoRarityUtil.normalize(entry.getValue());
                if (!BingoRarityUtil.isKnown(rarity)) continue;
                cleaned.put(entry.getKey(), rarity);
            }
        }
        if (cleaned.isEmpty()) {
            playerRarityOverrides.remove(playerId);
        } else {
            playerRarityOverrides.put(playerId, cleaned);
        }
        recomputeRarityOverridesFromPlayers();
    }

    public void clearPlayerRarityOverrides(UUID playerId) {
        if (playerId == null) return;
        if (playerRarityOverrides.remove(playerId) != null) {
            recomputeRarityOverridesFromPlayers();
        }
    }

    public Map<String, String> getRarityOverrides() {
        return new HashMap<>(rarityOverrides);
    }

    public String getEffectiveRarity(String id, String fallback) {
        if (id == null || id.isBlank()) return fallback;
        String override = rarityOverrides.get(id);
        String base = override == null || override.isBlank() ? fallback : override;
        if (adventureMode && isNetherPortalRarityBuffActive() && isNetherCategorySlot(id)) {
            return shiftRarityMoreCommon(base, 1);
        }
        return base;
    }

    private boolean isNetherPortalRarityBuffActive() {
        return prelitPortalsMode == PRELIT_PORTALS_NETHER || prelitPortalsMode == PRELIT_PORTALS_BOTH;
    }

    private boolean isNetherCategorySlot(String id) {
        if (id == null || id.isBlank()) return false;
        if (id.startsWith("quest.")) {
            QuestDefinition quest = QuestDatabase.getQuestById(id);
            String category = quest == null ? "" : quest.category;
            return isNetherCategoryText(category);
        }
        com.jamie.jamiebingo.ItemDefinition item = ItemDatabase.getRawById(id);
        String category = item == null ? "" : item.category();
        return isNetherCategoryText(category);
    }

    private boolean isNetherCategoryText(String category) {
        if (category == null || category.isBlank()) return false;
        return category.toLowerCase(Locale.ROOT).contains("nether");
    }

    private String shiftRarityMoreCommon(String rarity, int steps) {
        String normalized = BingoRarityUtil.normalize(rarity);
        int idx = BingoRarityUtil.ORDERED_RARITIES.indexOf(normalized);
        if (idx < 0) return normalized.isBlank() ? "impossible" : normalized;
        int target = Math.max(0, idx - Math.max(0, steps));
        return BingoRarityUtil.ORDERED_RARITIES.get(target);
    }

    private void recomputeRarityOverridesFromPlayers() {
        rarityOverrides.clear();
        List<UUID> orderedPlayers = new ArrayList<>(playerRarityOverrides.keySet());
        orderedPlayers.sort(Comparator.comparing(UUID::toString));
        for (UUID playerId : orderedPlayers) {
            Map<String, String> overrides = playerRarityOverrides.get(playerId);
            if (overrides == null || overrides.isEmpty()) continue;
            for (Map.Entry<String, String> entry : overrides.entrySet()) {
                String current = rarityOverrides.get(entry.getKey());
                rarityOverrides.put(entry.getKey(), BingoRarityUtil.lessRare(current, entry.getValue()));
            }
        }
    }

    private static Set<String> cleanIdSet(Collection<String> ids) {
        Set<String> out = new HashSet<>();
        if (ids == null) return out;
        for (String id : ids) {
            if (id == null || id.isBlank()) continue;
            out.add(id);
        }
        return out;
    }

    /* =========================
       OWNERSHIP ACCESS (SAFE)
       ========================= */

    public Map<String, Set<UUID>> getSlotOwners() {
        return slotOwners;
    }

    public Map<String, DyeColor> getSlotOwnershipSnapshot() {
        Map<String, DyeColor> out = new HashMap<>();

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return out;

        TeamData teamData = TeamData.get(server);

        for (var entry : slotOwners.entrySet()) {
            String slotId = entry.getKey();
            for (UUID teamId : entry.getValue()) {
                TeamData.TeamInfo team = teamData.getTeams().stream()
                        .filter(t -> t.id.equals(teamId))
                        .findFirst()
                        .orElse(null);

                if (team != null) {
                    out.put(slotId, team.color);
                }
            }
        }
        return out;
    }

    /* =========================
       BASIC ACCESS
       ========================= */

    public boolean isActive() {
        return active;
    }

    public BingoCard getCurrentCard() {
        return currentCard;
    }

    public void setPendingWeeklyChallengeStart(long baseSeed) {
        pendingWeeklyChallengeStart = true;
        pendingWeeklyChallengeBaseSeed = baseSeed;
    }

    public void clearPendingWeeklyChallengeStart() {
        pendingWeeklyChallengeStart = false;
        pendingWeeklyChallengeBaseSeed = 0L;
    }

    public void clearActiveWeeklyChallenge() {
        activeWeeklyChallengeId = "";
        activeWeeklySettingsSeed = "";
        activeWeeklyWorldSeed = "";
        activeWeeklyCardSeed = "";
    }

    public String getLastPlayedSeed() {
        return lastPlayedSeed == null ? "" : lastPlayedSeed;
    }

    public List<GameHistoryEntry> getGameHistoryForPlayer(UUID playerId) {
        if (playerId == null) return List.of();
        List<GameHistoryEntry> list = playerGameHistory.get(playerId);
        if (list == null || list.isEmpty()) return List.of();
        List<GameHistoryEntry> copy = new ArrayList<>(list.size());
        for (GameHistoryEntry entry : list) {
            copy.add(entry.copy());
        }
        copy.sort((a, b) -> Long.compare(b.finishedAtEpochSeconds, a.finishedAtEpochSeconds));
        return copy;
    }

    private void cacheCurrentSeed(MinecraftServer server) {
        if (server == null || currentCard == null) return;
        String encoded = CardSeedCodec.encode(this, server);
        if (encoded != null && !encoded.isBlank()) {
            lastPlayedSeed = encoded;
        }
    }

    public void setSize(int size) {
        this.size = size;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

public int getGunGameIndexForTeam(UUID teamId) {
    return gunGameTeamIndex.getOrDefault(teamId, 0);
}

public int getGunGameTeamIndex(UUID teamId) {
    return gunGameTeamIndex.getOrDefault(teamId, 0);
}

    /* =========================
       CARD HELPERS
       ========================= */

    public boolean cardContainsForPlayer(UUID playerId, String id) {
    if (playerId == null) return false;

    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    if (server == null) return false;

    TeamData teamData = TeamData.get(server);
    UUID teamId = teamData.getTeamForPlayer(playerId);
    if (teamId == null) {
        BingoCard card = currentCard;
        if (card == null) return false;
        for (int y = 0; y < card.getSize(); y++) {
            for (int x = 0; x < card.getSize(); x++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot != null && slot.getId().equals(id)) return true;
            }
        }
        return false;
    }

    if (isPlayerEliminated(playerId)) return false;

    BingoCard card = getActiveCardForTeam(teamId);
    if (card == null) return false;

    for (int y = 0; y < card.getSize(); y++) {
        for (int x = 0; x < card.getSize(); x++) {
            BingoSlot slot = card.getSlot(x, y);
            if (slot != null && slot.getId().equals(id)) return true;
        }
    }
    return false;
}

   private int[] findSlotCoords(UUID teamId, String id) {
    BingoCard card = getActiveCardForTeam(teamId);
    if (card == null) return null;

    for (int y = 0; y < card.getSize(); y++) {
        for (int x = 0; x < card.getSize(); x++) {
            BingoSlot slot = card.getSlot(x, y);
            if (slot != null && slot.getId().equals(id)) {
                return new int[]{x, y};
            }
        }
    }
    return null;
}

// âœ… Compatibility wrapper for legacy callers
public boolean cardContains(String id) {
    if (id == null) return false;
    if (currentCard == null) return false;

    for (int y = 0; y < currentCard.getSize(); y++) {
        for (int x = 0; x < currentCard.getSize(); x++) {
            BingoSlot slot = currentCard.getSlot(x, y);
            if (slot != null && slot.getId().equals(id)) {
                return true;
            }
        }
    }
    return false;
}

    /* =========================
       PROGRESS ACCESS
       ========================= */

    public Set<String> getPlayerProgress(UUID playerId) {
        if (playerId == null) return Collections.emptySet();

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return Collections.emptySet();

        TeamData teamData = TeamData.get(server);
        UUID teamId = teamData.getTeamForPlayer(playerId);
        if (teamId == null) return Collections.emptySet();

        return teamProgress.computeIfAbsent(teamId, id -> new HashSet<>());
    }

/* =========================
   DISPLAY PROGRESS (UI / SCOREBOARD)
   ========================= */

public Set<String> getTeamProgressForDisplay(UUID teamId) {

    if (winCondition == WinCondition.GUNGAME) {
        int idx = gunGameTeamIndex.getOrDefault(teamId, 0);
        return gunGameProgress
                .getOrDefault(teamId, Map.of())
                .getOrDefault(idx, Set.of());
    }

    return teamProgress.getOrDefault(teamId, Set.of());
}

    /* =========================
       BLIND HELPERS
       ========================= */

    public Set<String> getRevealedSlots(UUID teamId) {
        return revealedSlots.computeIfAbsent(teamId, id -> new HashSet<>());
    }

    public Set<String> getHighlightedSlots(UUID teamId) {
        return highlightedSlots.computeIfAbsent(teamId, id -> new HashSet<>());
    }

    public boolean toggleHighlight(UUID teamId, String slotId) {
        if (teamId == null || slotId == null || slotId.isBlank()) return false;
        if (getTeamProgressForDisplay(teamId).contains(slotId)) {
            return removeHighlight(teamId, slotId);
        }
        Set<String> set = getHighlightedSlots(teamId);
        if (set.contains(slotId)) {
            set.remove(slotId);
            return true;
        }
        set.add(slotId);
        return true;
    }

    public boolean removeHighlight(UUID teamId, String slotId) {
        if (teamId == null || slotId == null || slotId.isBlank()) return false;
        Set<String> set = highlightedSlots.get(teamId);
        if (set == null) return false;
        return set.remove(slotId);
    }

    public boolean isPlayerEliminated(UUID playerId) {
        return playerId != null && eliminatedPlayers.contains(playerId);
    }

    public boolean isTeamEliminated(UUID teamId) {
        return teamId != null && eliminatedTeams.contains(teamId);
    }

    public int getRushSecondsForPlayer(UUID playerId) {
        if (!active || !rushEnabled || playerId == null) return -1;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return -1;
        TeamData teamData = TeamData.get(server);
        UUID teamId = teamData.getTeamForPlayer(playerId);
        if (teamId == null || eliminatedTeams.contains(teamId)) return -1;
        Integer deadline = rushDeadlineTickByTeam.get(teamId);
        if (deadline == null) {
            java.util.List<ServerPlayer> online = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server);
            if (online.size() == 1 && !rushDeadlineTickByTeam.isEmpty()) {
                UUID sourceTeam = null;
                int bestDeadline = Integer.MIN_VALUE;
                for (Map.Entry<UUID, Integer> entry : rushDeadlineTickByTeam.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) continue;
                    if (entry.getValue() > bestDeadline) {
                        bestDeadline = entry.getValue();
                        sourceTeam = entry.getKey();
                    }
                }
                if (sourceTeam != null) {
                    Integer sourceDeadline = rushDeadlineTickByTeam.remove(sourceTeam);
                    Integer sourceRemaining = rushResumeSecondsByTeam.remove(sourceTeam);
                    if (sourceDeadline != null) {
                        rushDeadlineTickByTeam.put(teamId, sourceDeadline);
                        deadline = sourceDeadline;
                    }
                    if (sourceRemaining != null) {
                        rushResumeSecondsByTeam.put(teamId, sourceRemaining);
                    }
                    if (eliminatedTeams.remove(sourceTeam)) {
                        eliminatedTeams.add(teamId);
                    }
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
                }
            }
            if (deadline == null) return -1;
        }
        int now = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        return Math.max(0, (deadline - now) / 20);
    }

    public Integer getRushDeadlineTickForTeam(UUID teamId) {
        return teamId == null ? null : rushDeadlineTickByTeam.get(teamId);
    }

    public Set<UUID> getRushDeadlineTeamsSnapshot() {
        return new HashSet<>(rushDeadlineTickByTeam.keySet());
    }

    public void restoreRushDeadlinesIfNeeded(MinecraftServer server) {
        if (server == null || rushResumeSecondsByTeam.isEmpty()) return;
        int currentTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        boolean changed = false;
        for (Map.Entry<UUID, Integer> entry : rushResumeSecondsByTeam.entrySet()) {
            UUID teamId = entry.getKey();
            Integer remaining = entry.getValue();
            if (teamId == null || remaining == null) continue;
            if (remaining < 0) continue;
            if (eliminatedTeams.contains(teamId)) continue;
            if (rushDeadlineTickByTeam.containsKey(teamId)) continue;
            rushDeadlineTickByTeam.put(teamId, currentTick + Math.max(0, remaining) * 20);
            changed = true;
        }
        if (changed) {
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        }
    }

    public void updateRushResumeSnapshot(MinecraftServer server) {
        if (server == null || !active || !rushEnabled || rushDeadlineTickByTeam.isEmpty()) return;
        int currentTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        boolean changed = false;
        for (Map.Entry<UUID, Integer> entry : rushDeadlineTickByTeam.entrySet()) {
            UUID teamId = entry.getKey();
            Integer deadline = entry.getValue();
            if (teamId == null || deadline == null) continue;
            int remaining = Math.max(0, (deadline - currentTick) / 20);
            Integer old = rushResumeSecondsByTeam.get(teamId);
            if (old == null || old != remaining) {
                rushResumeSecondsByTeam.put(teamId, remaining);
                changed = true;
            }
        }
        if (changed) {
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        }
    }

    public void recordRushCompletion(MinecraftServer server, UUID teamId, int currentTick) {
        if (!rushEnabled || teamId == null || currentTick < 0) return;
        int timeout = Math.max(1, rushSeconds);
        rushDeadlineTickByTeam.put(teamId, currentTick + timeout * 20);
        rushResumeSecondsByTeam.put(teamId, timeout);
        if (server != null && com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server).size() == 1) {
            rushDeadlineTickByTeam.entrySet().removeIf(e -> e.getKey() != null && !teamId.equals(e.getKey()));
            rushResumeSecondsByTeam.entrySet().removeIf(e -> e.getKey() != null && !teamId.equals(e.getKey()));
            eliminatedTeams.removeIf(id -> id != null && !teamId.equals(id));
        }
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        if (server != null) {
            for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                BroadcastHelper.syncGameTimer(player);
            }
        }
    }

    public void normalizeSingleplayerProgressState(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return;
        if (com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server).size() != 1) return;
        TeamData teamData = TeamData.get(server);
        UUID currentTeam = teamData.getTeamForPlayer(playerId);
        if (currentTeam == null) return;

        boolean changed = false;
        Set<String> merged = teamProgress.computeIfAbsent(currentTeam, id -> new HashSet<>());
        java.util.List<UUID> staleTeams = new ArrayList<>();
        for (Map.Entry<UUID, Set<String>> entry : teamProgress.entrySet()) {
            UUID source = entry.getKey();
            if (source == null || source.equals(currentTeam)) continue;
            Set<String> sourceSet = entry.getValue();
            if (sourceSet != null && !sourceSet.isEmpty() && merged.addAll(sourceSet)) {
                changed = true;
            }
            staleTeams.add(source);
        }
        for (UUID stale : staleTeams) {
            if (teamProgress.remove(stale) != null) {
                changed = true;
            }
        }

        for (Set<UUID> owners : slotOwners.values()) {
            if (owners == null || owners.isEmpty()) continue;
            boolean hadNonCurrent = owners.removeIf(id -> id != null && !currentTeam.equals(id));
            if (!owners.contains(currentTeam)) {
                owners.add(currentTeam);
                changed = true;
            } else if (hadNonCurrent) {
                changed = true;
            }
        }

        if (changed) {
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        }
    }

    public void clearRushState() {
        rushDeadlineTickByTeam.clear();
        rushResumeSecondsByTeam.clear();
    }

    private void clearMineResumeStateInternal(boolean dirty) {
        mineResumeSourceIds.clear();
        mineResumeRemainingSecondsByPlayer.clear();
        mineResumeTriggeredSourceByPlayer.clear();
        mineResumeDamageProgressByPlayer.clear();
        mineResumeDefuseQuestId = "";
        mineResumeDefuseDisplayName = "";
        if (dirty) {
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        }
    }

    public void clearMineResumeState() {
        clearMineResumeStateInternal(true);
    }

    public void setMineSeedSelection(Collection<String> sourceIds) {
        mineResumeSourceIds.clear();
        if (sourceIds != null) {
            for (String id : sourceIds) {
                if (id == null || id.isBlank()) continue;
                mineResumeSourceIds.add(id);
            }
        }
    }

    public void updateMineResumeState(
            MinecraftServer server,
            Collection<String> sourceIds,
            Map<UUID, Integer> remainingSecondsByPlayer,
            Map<UUID, String> triggeredSourceByPlayer,
            Map<UUID, Integer> damageProgressByPlayer,
            String defuseQuestId,
            String defuseDisplayName
    ) {
        mineResumeSourceIds.clear();
        if (sourceIds != null) {
            for (String id : sourceIds) {
                if (id == null || id.isBlank()) continue;
                mineResumeSourceIds.add(id);
            }
        }

        mineResumeRemainingSecondsByPlayer.clear();
        if (remainingSecondsByPlayer != null) {
            for (Map.Entry<UUID, Integer> entry : remainingSecondsByPlayer.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                int remaining = Math.max(0, entry.getValue());
                if (remaining <= 0) continue;
                mineResumeRemainingSecondsByPlayer.put(entry.getKey(), remaining);
            }
        }

        mineResumeTriggeredSourceByPlayer.clear();
        if (triggeredSourceByPlayer != null) {
            for (Map.Entry<UUID, String> entry : triggeredSourceByPlayer.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isBlank()) continue;
                mineResumeTriggeredSourceByPlayer.put(entry.getKey(), entry.getValue());
            }
        }

        mineResumeDamageProgressByPlayer.clear();
        if (damageProgressByPlayer != null) {
            for (Map.Entry<UUID, Integer> entry : damageProgressByPlayer.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                mineResumeDamageProgressByPlayer.put(entry.getKey(), Math.max(0, entry.getValue()));
            }
        }

        mineResumeDefuseQuestId = defuseQuestId == null ? "" : defuseQuestId;
        mineResumeDefuseDisplayName = defuseDisplayName == null ? "" : defuseDisplayName;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public List<String> getMineResumeSourceIds() {
        return new ArrayList<>(mineResumeSourceIds);
    }

    public Map<UUID, Integer> getMineResumeRemainingSecondsByPlayer() {
        return new HashMap<>(mineResumeRemainingSecondsByPlayer);
    }

    public Map<UUID, String> getMineResumeTriggeredSourceByPlayer() {
        return new HashMap<>(mineResumeTriggeredSourceByPlayer);
    }

    public Map<UUID, Integer> getMineResumeDamageProgressByPlayer() {
        return new HashMap<>(mineResumeDamageProgressByPlayer);
    }

    public String getMineResumeDefuseQuestId() {
        return mineResumeDefuseQuestId;
    }

    public String getMineResumeDefuseDisplayName() {
        return mineResumeDefuseDisplayName;
    }

    private void clearPowerSlotResumeStateInternal(boolean dirty) {
        powerResumeSlotId = "";
        powerResumeDisplayName = "";
        powerResumeRemainingSeconds = -1;
        powerResumeClaimed = false;
        if (dirty) {
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        }
    }

    public void clearPowerSlotResumeState() {
        clearPowerSlotResumeStateInternal(true);
    }

    public void updatePowerSlotResumeState(
            String slotId,
            String displayName,
            int remainingSeconds,
            boolean claimed
    ) {
        String nextId = slotId == null ? "" : slotId;
        String nextName = displayName == null ? "" : displayName;
        int nextRemaining = Math.max(-1, remainingSeconds);
        boolean changed = !java.util.Objects.equals(powerResumeSlotId, nextId)
                || !java.util.Objects.equals(powerResumeDisplayName, nextName)
                || powerResumeRemainingSeconds != nextRemaining
                || powerResumeClaimed != claimed;
        if (!changed) return;
        powerResumeSlotId = nextId;
        powerResumeDisplayName = nextName;
        powerResumeRemainingSeconds = nextRemaining;
        powerResumeClaimed = claimed;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public String getPowerResumeSlotId() {
        return powerResumeSlotId;
    }

    public String getPowerResumeDisplayName() {
        return powerResumeDisplayName;
    }

    public int getPowerResumeRemainingSeconds() {
        return powerResumeRemainingSeconds;
    }

    public boolean isPowerResumeClaimed() {
        return powerResumeClaimed;
    }

    public boolean isParticipant(UUID playerId) {
        return playerId != null && participants.contains(playerId);
    }

    public boolean isSpectator(UUID playerId) {
        return playerId != null && spectators.contains(playerId);
    }

    public UUID getSpectatorViewTarget(UUID spectatorId) {
        return spectatorId == null ? null : spectatorViewTargets.get(spectatorId);
    }

    public void setSpectatorViewTarget(UUID spectatorId, UUID targetId) {
        if (spectatorId == null) return;
        if (targetId == null) {
            spectatorViewTargets.remove(spectatorId);
        } else {
            spectatorViewTargets.put(spectatorId, targetId);
        }
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public void initializeParticipants(MinecraftServer server) {
        participants.clear();
        spectators.clear();
        spectatorViewTargets.clear();
        lateJoinPending.clear();
        if (server == null) return;
        TeamData teamData = TeamData.get(server);
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            participants.add(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
            teamData.ensureAssigned(player);
        }
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public void lockQuestsForAllParticipants() {
        questReleasePending.clear();
        questReleasePending.addAll(participants);
    }

    public void lockQuestsForPlayer(UUID playerId) {
        if (playerId == null) return;
        questReleasePending.add(playerId);
    }

    public void releaseQuestsForPlayer(UUID playerId) {
        if (playerId == null) return;
        questReleasePending.remove(playerId);
    }

    public boolean areQuestsReleased(UUID playerId) {
        return playerId != null && !questReleasePending.contains(playerId);
    }

    public boolean isLateJoinPending(UUID playerId) {
        return playerId != null && lateJoinPending.contains(playerId);
    }

    public void setLateJoinPending(UUID playerId, boolean pending) {
        if (playerId == null) return;
        if (pending) {
            lateJoinPending.add(playerId);
        } else {
            lateJoinPending.remove(playerId);
        }
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public void resetProgressForNewCard() {
        teamProgress.clear();
        slotOwners.clear();
        revealedSlots.clear();
        highlightedSlots.clear();
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public List<ServerPlayer> getParticipantPlayers(MinecraftServer server) {
        List<ServerPlayer> out = new ArrayList<>();
        if (server == null) return out;
        for (UUID id : participants) {
            ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, id);
            if (p != null) out.add(p);
        }
        return out;
    }

    public void markPlayerEliminated(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return;
        if (!active || !hardcoreEnabled) return;
        if (pendingWinEndActive || stopGamePending) return;

        eliminatedPlayers.add(playerId);

        TeamData teamData = TeamData.get(server);
        UUID teamId = teamData.getTeamForPlayer(playerId);
        if (teamId == null) {
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
            return;
        }

        boolean allOut = true;
        TeamData.TeamInfo teamInfo = teamData.getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);

        if (teamInfo == null || teamInfo.members.isEmpty()) {
            allOut = false;
        } else {
            for (UUID memberId : teamInfo.members) {
                if (!eliminatedPlayers.contains(memberId)) {
                    allOut = false;
                    break;
                }
            }
        }

        if (allOut) {
            eliminatedTeams.add(teamId);
            checkLastTeamStanding(server, true);
        }

        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    private void checkLastTeamStanding(MinecraftServer server, boolean requireHardcore) {
        if (server == null || !active) return;
        if (requireHardcore && !hardcoreEnabled) return;
        if (pendingWinEndActive || stopGamePending) return;

        TeamData teamData = TeamData.get(server);
        List<UUID> remainingTeams = new ArrayList<>();

        for (TeamData.TeamInfo team : teamData.getTeams()) {
            if (team.members.isEmpty()) continue;
            if (eliminatedTeams.contains(team.id)) continue;
            remainingTeams.add(team.id);
        }

        if (remainingTeams.isEmpty()) {
            handleAllTeamsEliminated(server);
            return;
        }

        if (remainingTeams.size() == 1) {
            com.jamie.jamiebingo.bingo.BingoWinEvaluator.forceEliminationWin(server, remainingTeams.get(0));
        }
    }

    private void handleAllTeamsEliminated(MinecraftServer server) {
        if (server == null || !active) return;
        if (stopGamePending) return;

        net.minecraft.network.chat.Component title =
                com.jamie.jamiebingo.util.ComponentUtil.literal("You lost!")
                        .withStyle(ChatFormatting.BOLD, ChatFormatting.RED);

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 45, 10));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
        }

        BroadcastHelper.broadcast(
                server,
                com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Everyone was eliminated. You lost.")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
        );
        scheduleStopGameWithDelay(server, 60);
    }

    public void eliminateTeamForRush(MinecraftServer server, UUID teamId) {
        if (server == null || teamId == null || !active || !rushEnabled) return;
        if (pendingWinEndActive || stopGamePending) return;
        if (eliminatedTeams.contains(teamId)) return;
        UUID requestedTeamId = teamId;
        UUID effectiveTeamId = teamId;
        TeamData teamData = TeamData.get(server);
        UUID lookupTeamId = effectiveTeamId;
        TeamData.TeamInfo teamInfo = teamData.getTeams().stream()
                .filter(t -> t.id.equals(lookupTeamId))
                .findFirst()
                .orElse(null);
        List<UUID> members = new ArrayList<>();
        if (teamInfo != null && !teamInfo.members.isEmpty()) {
            members.addAll(teamInfo.members);
        }
        if (members.isEmpty()) {
            for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                if (player == null) continue;
                UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
                UUID mappedTeam = teamData.getTeamForPlayer(playerId);
                if (teamId.equals(mappedTeam)) {
                    members.add(playerId);
                }
            }
        }
        if (members.isEmpty()) {
            List<ServerPlayer> online = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server);
            if (online.size() == 1) {
                ServerPlayer lone = online.get(0);
                UUID loneId = com.jamie.jamiebingo.util.EntityUtil.getUUID(lone);
                members.add(loneId);
                UUID mappedTeam = teamData.getTeamForPlayer(loneId);
                if (mappedTeam != null) {
                    effectiveTeamId = mappedTeam;
                    teamInfo = teamData.getTeams().stream()
                            .filter(t -> t != null && mappedTeam.equals(t.id))
                            .findFirst()
                            .orElse(teamInfo);
                }
            }
        }
        if (eliminatedTeams.contains(effectiveTeamId)) return;
        if (members.isEmpty()) return;
        for (UUID memberId : members) {
            eliminatedPlayers.add(memberId);
            participants.remove(memberId);
            lateJoinPending.remove(memberId);
            questReleasePending.remove(memberId);
            ServerPlayer sp = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
            if (sp != null) {
                com.jamie.jamiebingo.world.SpectatorManager.makeSpectator(sp, this);
            }
        }

        eliminatedTeams.add(effectiveTeamId);
        rushDeadlineTickByTeam.remove(effectiveTeamId);
        rushResumeSecondsByTeam.remove(effectiveTeamId);
        if (!requestedTeamId.equals(effectiveTeamId)) {
            rushDeadlineTickByTeam.remove(requestedTeamId);
            rushResumeSecondsByTeam.remove(requestedTeamId);
            eliminatedTeams.remove(requestedTeamId);
        }
        TeamData.TeamInfo eliminationInfo = teamInfo;
        if (eliminationInfo == null) {
            DyeColor color = null;
            UUID firstMember = members.get(0);
            UUID mapped = teamData.getTeamForPlayer(firstMember);
            if (mapped != null) {
                TeamData.TeamInfo mappedInfo = teamData.getTeams().stream()
                        .filter(t -> mapped.equals(t.id))
                        .findFirst()
                        .orElse(null);
                if (mappedInfo != null) {
                    color = mappedInfo.color;
                }
            }
            eliminationInfo = new TeamData.TeamInfo(effectiveTeamId, color == null ? DyeColor.WHITE : color);
            eliminationInfo.members.addAll(members);
        }
        broadcastRushEliminationAlert(server, eliminationInfo);
        BroadcastHelper.broadcastTeamScores(server);
        checkLastTeamStanding(server, false);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public void eliminatePlayerForMines(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null || !active) return;
        if (pendingWinEndActive || stopGamePending) return;
        if (eliminatedPlayers.contains(playerId)) return;

        eliminatedPlayers.add(playerId);
        participants.remove(playerId);
        lateJoinPending.remove(playerId);
        questReleasePending.remove(playerId);

        ServerPlayer sp = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, playerId);
        if (sp != null) {
            com.jamie.jamiebingo.world.SpectatorManager.makeSpectator(sp, this);
        }

        TeamData teamData = TeamData.get(server);
        UUID teamId = teamData.getTeamForPlayer(playerId);
        if (teamId != null) {
            TeamData.TeamInfo teamInfo = teamData.getTeams().stream()
                    .filter(t -> t.id.equals(teamId))
                    .findFirst()
                    .orElse(null);
            if (teamInfo != null && !teamInfo.members.isEmpty()) {
                boolean allOut = true;
                for (UUID memberId : teamInfo.members) {
                    if (!eliminatedPlayers.contains(memberId)) {
                        allOut = false;
                        break;
                    }
                }
                if (allOut) {
                    eliminatedTeams.add(teamId);
                    checkLastTeamStanding(server, false);
                }
            }
        }

        BroadcastHelper.broadcastTeamScores(server);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    private void broadcastRushEliminationAlert(MinecraftServer server, TeamData.TeamInfo eliminatedTeam) {
        if (server == null || eliminatedTeam == null) return;

        String teamName = formatTeamName(eliminatedTeam.color);
        net.minecraft.network.chat.Component title = com.jamie.jamiebingo.util.ComponentUtil.literal(teamName + " eliminated!")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.RED);

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 45, 10));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            NetworkHandler.send(
                    new PacketPlayTeamSound("minecraft:entity.ender_dragon.growl", 1.0f, 1.0f),
                    PacketDistributor.PLAYER.with(player)
            );
        }

        BroadcastHelper.broadcast(
                server,
                com.jamie.jamiebingo.util.ComponentUtil.literal("[RUSH] " + teamName + " eliminated!")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
        );
    }

    private static String formatTeamName(DyeColor color) {
        String base = color == null ? "Team" : color.getName();
        if (base == null || base.isBlank()) return "Team";
        String[] parts = base.split("_");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isBlank()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) out.append(p.substring(1));
        }
        return "Team " + out;
    }

    private void initRevealedSlots(MinecraftServer server) {
        revealedSlots.clear();
        highlightedSlots.clear();
        if (currentCard == null) return;

        TeamData teamData = TeamData.get(server);
        BingoSlot start = currentCard.getSlot(0, 0);
        if (start == null) return;

        for (TeamData.TeamInfo team : teamData.getTeams()) {
            revealedSlots
                    .computeIfAbsent(team.id, k -> new HashSet<>())
                    .add(start.getId());
        }
    }

    private void revealAdjacent(UUID teamId, String slotId) {
    if (currentCard == null) return;

    int[] pos = findSlotCoords(teamId, slotId);
    if (pos == null) return;

    BingoCard card = getActiveCardForTeam(teamId);
if (card == null) return;

int size = card.getSize();
    int x = pos[0];
    int y = pos[1];

    Set<String> revealed = getRevealedSlots(teamId);

    for (int dy = -1; dy <= 1; dy++) {
        for (int dx = -1; dx <= 1; dx++) {
            if (dx == 0 && dy == 0) continue;

            int nx = x + dx;
            int ny = y + dy;

            // âœ… CRITICAL: stop out-of-bounds access (top-left crash fix)
            if (nx < 0 || ny < 0 || nx >= size || ny >= size) continue;

            BingoSlot adj = card.getSlot(nx, ny);
            if (adj != null) {
                revealed.add(adj.getId());
            }
        }
    }
}

       private boolean isBottomRight(UUID teamId, String id) {
    int[] pos = findSlotCoords(teamId, id);
    if (pos == null) return false;

    BingoCard card = getActiveCardForTeam(teamId);
    if (card == null) return false;

    int max = card.getSize() - 1;
    return pos[0] == max && pos[1] == max;
}

    /* =========================
       COMPLETION
       ========================= */

    private static final int COMPLETION_FLASH_TICKS = 40;

    public boolean markCompleted(UUID playerId, String id) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;

        TeamData teamData = TeamData.get(server);
        UUID teamId = teamData.getTeamForPlayer(playerId);
        if (teamId == null) {
            ServerPlayer player = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, playerId);
            if (player == null) return false;
            teamId = teamData.ensureAssigned(player);
        }
        normalizeSingleplayerProgressState(server, playerId);
        recoverSingleplayerRushState(server, playerId);
        UUID remappedTeam = teamData.getTeamForPlayer(playerId);
        if (remappedTeam != null) {
            teamId = remappedTeam;
        }
        if (!active || startCountdownActive || gameStartTick < 0) return false;

        if (isPlayerEliminated(playerId)) return false;

        // GunGame: slot must exist on this team's active card
        if (winCondition == WinCondition.GUNGAME) {
            if (!cardContainsForPlayer(playerId, id)) {
                return false;
            }
        }

        /* ---------- HANGMAN MODE ---------- */
        if (winCondition == WinCondition.HANGMAN) {
            if (hangmanCurrentSlotId == null || !hangmanCurrentSlotId.equals(id)) return false;
            if (hangmanIntermissionEndTick > 0) return false;

            TeamScoreData.get(server).award(teamId, playerId, 1);
            recordRushCompletion(server, teamId, com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server));
            BroadcastHelper.broadcastTeamScores(server);

            com.jamie.jamiebingo.bingo.HangmanTicker.startIntermission(server, this);
            com.jamie.jamiebingo.bingo.HangmanTicker.checkEarlyWin(server, this);

            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
            return true;
        }

        /* ---------- BLIND MODE ---------- */
        if (winCondition == WinCondition.BLIND) {
            Set<String> revealed = getRevealedSlots(teamId);
            if (!revealed.contains(id)) return false;

            Set<String> teamSet = teamProgress.computeIfAbsent(teamId, k -> new HashSet<>());
            if (!teamSet.add(id)) return false;

            boolean highlightRemoved = removeHighlight(teamId, id);

            handleCompletionFeedback(server, teamId, id);

            TeamScoreData.get(server).award(teamId, playerId, 1);
            recordRushCompletion(server, teamId, com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server));
            BroadcastHelper.broadcastTeamScores(server);
            BroadcastHelper.broadcastSlotOwnership(server);

            slotOwners.computeIfAbsent(id, k -> new HashSet<>()).add(teamId);

            revealAdjacent(teamId, id);

            BroadcastHelper.broadcastTeamScores(server);

            if (isBottomRight(teamId, id)) {
                BingoWinEvaluator.forceBlindWin(server, teamId);
            }

            BroadcastHelper.broadcastSlotOwnership(server);
            BroadcastHelper.broadcastRevealedSlots(server);
            if (highlightRemoved) {
                BroadcastHelper.broadcastHighlightedSlots(server);
            }

            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
            BingoWinEvaluator.onProgress(server, teamId);
            return true;
        }

        /* ---------- PROGRESS STORAGE ---------- */
        if (handleFakeCompletionAttempt(server, teamId, playerId, id)) {
            return false;
        }

        Set<String> teamSet;

        if (winCondition == WinCondition.GUNGAME) {
            int cardIndex = gunGameTeamIndex.getOrDefault(teamId, 0);

            teamSet = gunGameProgress
                    .computeIfAbsent(teamId, t -> new HashMap<>())
                    .computeIfAbsent(cardIndex, i -> new HashSet<>());
        } else {
            teamSet = teamProgress.computeIfAbsent(teamId, k -> new HashSet<>());
        }

        if (teamSet == null || !teamSet.add(id)) return false;
        if (winCondition == WinCondition.LOCKOUT || winCondition == WinCondition.RARITY) {
            revealFakeRealForOtherTeamsOnRealCompletion(server, teamId, id);
        }
        if (winCondition == WinCondition.LOCKOUT || winCondition == WinCondition.RARITY) {
            Set<UUID> owners = slotOwners.get(id);
            if (owners != null && !owners.isEmpty() && !owners.contains(teamId)) {
                teamSet.remove(id);
                return false;
            }
        }
        boolean highlightRemoved = removeHighlight(teamId, id);

        boolean lineCompleted = handleCompletionFeedback(server, teamId, id);
        com.jamie.jamiebingo.mines.MineModeManager.onGoalCompleted(server, id);
        ServerPlayer completedPlayer = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, playerId);
        com.jamie.jamiebingo.power.PowerSlotManager.onGoalCompleted(server, id, completedPlayer);

        if (winCondition == WinCondition.LOCKOUT
                || winCondition == WinCondition.RARITY
                || winCondition == WinCondition.GAMEGUN) {
            playOpponentGoalSound(server, teamId);
        }

        /* ---------- OWNERSHIP (LOCKOUT / RARITY) ---------- */
        if (winCondition == WinCondition.LOCKOUT || winCondition == WinCondition.RARITY) {
            slotOwners
                    .computeIfAbsent(id, k -> new HashSet<>())
                    .add(teamId);

            BroadcastHelper.broadcastSlotOwnership(server);
            BroadcastHelper.broadcastProgress(server);
        }

        /* ---------- SCORING ---------- */
        if (winCondition == WinCondition.RARITY) {
            BingoSlot slot = null;

            for (int y = 0; y < currentCard.getSize(); y++) {
                for (int x = 0; x < currentCard.getSize(); x++) {
                    BingoSlot s = currentCard.getSlot(x, y);
                    if (s != null && s.getId().equals(id)) {
                        slot = s;
                        break;
                    }
                }
                if (slot != null) break;
            }

            if (slot != null) {
                int rarityScore = RarityScoreCalculator.base(slot);
                TeamScoreData.get(server).award(teamId, playerId, rarityScore);
            }
        } else {
            TeamScoreData.get(server).award(teamId, playerId, 1);
        }
        recordRushCompletion(server, teamId, com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server));

        BroadcastHelper.broadcastTeamScores(server);

        /* ---------- VISUAL SYNC ---------- */
        if (winCondition == WinCondition.LOCKOUT
                || winCondition == WinCondition.RARITY
                || winCondition == WinCondition.LINE
                || winCondition == WinCondition.FULL) {

            BroadcastHelper.broadcastSlotOwnership(server);
        }
        BroadcastHelper.broadcastProgress(server);
        if (highlightRemoved) {
            BroadcastHelper.broadcastHighlightedSlots(server);
        }

        if (lineCompleted) {
            final UUID completedTeamId = teamId;
            TeamData.TeamInfo teamInfo = TeamData.get(server).getTeams().stream().filter(t -> completedTeamId.equals(t.id)).findFirst().orElse(null);
            if (teamInfo != null) {
                BroadcastHelper.broadcast(
                        server,
                        BroadcastHelper.teamNameComponent(teamInfo.color)
                                .append(com.jamie.jamiebingo.util.ComponentUtil.literal(" completed a line."))
                );
            }
            applyShuffleRerollForTeam(server, teamId, teamSet);
        }

        /* ---------- GAMEGUN (shared progression) ---------- */
        if (winCondition == WinCondition.GAMEGUN) {
            gunGameSharedIndex++;

            if (gunGameSharedIndex >= gunGameCards.size()) {
                BingoWinEvaluator.onProgress(server, teamId);
                com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
                return true;
            }

            currentCard = gunGameCards.get(gunGameSharedIndex);

            teamProgress.clear();
            slotOwners.clear();
            revealedSlots.clear();
            highlightedSlots.clear();

            BroadcastHelper.broadcastFullSync();
            BroadcastHelper.broadcastTeamScores(server);
            handleImmediateGameGunTie(server);
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
            return true;
        }

        /* ---------- GUNGAME (per-team progression) ---------- */
        if (winCondition == WinCondition.GUNGAME) {
            int currentIndex = gunGameTeamIndex.getOrDefault(teamId, 0);

            // check win BEFORE advancing card
            BingoWinEvaluator.onProgress(server, teamId);

            if (!active) {
                com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
                return true;
            }

            int nextIndex = currentIndex + 1;
            gunGameTeamIndex.put(teamId, nextIndex);

            revealedSlots.remove(teamId);
            highlightedSlots.remove(teamId);

            for (Set<UUID> owners : slotOwners.values()) {
                owners.remove(teamId);
            }

            final UUID teamIdFinal = teamId;
            TeamData.TeamInfo teamInfo = teamData.getTeams().stream()
                    .filter(t -> t.id.equals(teamIdFinal))
                    .findFirst()
                    .orElse(null);

              if (teamInfo != null) {
                  for (UUID memberId : teamInfo.members) {
                      ServerPlayer sp = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
                      if (sp != null) {
                          BroadcastHelper.syncCard(sp);
                      }
                  }
              }

              handleImmediateGunGameTie(server, nextIndex);
              BroadcastHelper.broadcastTeamScores(server);

              com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
              return true;
          }

        BingoWinEvaluator.onProgress(server, teamId);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        return true;
    }

    private boolean handleFakeCompletionAttempt(MinecraftServer server, UUID teamId, UUID playerId, String attemptedId) {
        if (server == null || teamId == null || playerId == null || attemptedId == null || attemptedId.isBlank()) return false;
        if (fakeRealSlotByIndex.isEmpty() || currentCard == null) return false;
        if (isFakeRerollPhaseActive()) return false;

        BingoCard teamCard = getActiveCardForTeam(teamId);
        if (teamCard == null) return false;
        int sizeNow = teamCard.getSize();
        if (sizeNow <= 0) return false;

        for (Map.Entry<Integer, BingoSlot> entry : fakeRealSlotByIndex.entrySet()) {
            int idx = entry.getKey() == null ? -1 : entry.getKey();
            BingoSlot real = entry.getValue();
            if (idx < 0 || real == null || real.getId() == null || real.getId().isBlank()) continue;
            if (isFakeRealRevealedForTeam(teamId, idx)) continue;
            if (isFakeRevealPendingForTeam(teamId, idx)) return true;
            int x = idx % sizeNow;
            int y = idx / sizeNow;
            if (x < 0 || y < 0 || x >= sizeNow || y >= sizeNow) continue;
            BingoSlot visible = teamCard.getSlot(x, y);
            if (visible == null || visible.getId() == null || visible.getId().isBlank()) continue;
            if (!visible.getId().equals(attemptedId)) continue;
            if (visible.getId().equals(real.getId())) continue;

            TeamData teamData = TeamData.get(server);
            TeamData.TeamInfo victimTeam = teamData.getTeams().stream()
                    .filter(t -> t != null && teamId.equals(t.id))
                    .findFirst()
                    .orElse(null);
            ChatFormatting victimFmt = BroadcastHelper.teamChatFormatting(victimTeam == null ? null : victimTeam.color);

            UUID fakerTeamId = getFakeSourceTeamId(idx);
            TeamData.TeamInfo fakerTeam = teamData.getTeams().stream()
                    .filter(t -> t != null && fakerTeamId != null && fakerTeamId.equals(t.id))
                    .findFirst()
                    .orElse(null);
            String fakerPlayerName = getFakeSourcePlayerName(idx);
            if (fakerPlayerName == null || fakerPlayerName.isBlank()) {
                ServerPlayer sp = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, playerId);
                fakerPlayerName = sp == null ? "player" : sp.getName().getString();
            }
            String fakerTeamName = fakerTeam == null || fakerTeam.color == null ? "Team" : "Team " + fakerTeam.color.getName();
            String victimTeamName = victimTeam == null || victimTeam.color == null ? "Team" : "Team " + victimTeam.color.getName();
            ServerPlayer victimPlayer = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, playerId);
            String victimPlayerName = victimPlayer == null ? "player" : victimPlayer.getName().getString();

            MutableComponent msg = com.jamie.jamiebingo.util.ComponentUtil.literal(
                    victimTeamName + " " + victimPlayerName + " was faked by " + fakerTeamName + " " + fakerPlayerName
            ).withStyle(victimFmt);
            BroadcastHelper.broadcast(server, msg);

            for (UUID memberId : victimTeam == null ? Set.<UUID>of() : victimTeam.members) {
                ServerPlayer member = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
                if (member == null) continue;
                member.connection.send(new ClientboundSetTitlesAnimationPacket(6, 26, 8));
                member.connection.send(new ClientboundSetTitleTextPacket(com.jamie.jamiebingo.util.ComponentUtil.literal("FAKE!")));
                NetworkHandler.send(
                        new PacketPlayTeamSound("minecraft:entity.villager.no", 1.0f, 0.85f),
                        PacketDistributor.PLAYER.with(member)
                );
                member.playSound(SoundEvents.VILLAGER_NO, 1.0f, 0.85f);
            }

            sendFlashSlotsStyled(server, teamId, Set.of(visible.getId()), FAKE_REVEAL_PRE_TICKS, PacketFlashSlots.STYLE_SHAKE);
            markFakeRevealPendingForTeam(teamId, idx);
            com.jamie.jamiebingo.casino.CasinoTickScheduler.schedule(
                    server,
                    com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + FAKE_REVEAL_PRE_TICKS,
                    () -> {
                        BingoGameData data = BingoGameData.get(server);
                        if (data == null || !data.isActive()) return;
                        if (!data.revealFakeRealForTeam(teamId, idx)) {
                            data.clearFakeRevealPendingForTeam(teamId, idx);
                            return;
                        }
                        TeamData.TeamInfo vt = TeamData.get(server).getTeams().stream()
                                .filter(t -> t != null && teamId.equals(t.id))
                                .findFirst()
                                .orElse(null);
                        if (vt != null) {
                            for (UUID memberId : vt.members) {
                                ServerPlayer sp = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
                                if (sp != null) {
                                    BroadcastHelper.syncCard(sp);
                                }
                            }
                        }
                        sendFlashSlotsStyled(server, teamId, Set.of(real.getId()), FAKE_REVEAL_POST_TICKS, PacketFlashSlots.STYLE_PULSE);
                        data.clearFakeRevealPendingForTeam(teamId, idx);
                        for (UUID memberId : vt == null ? Set.<UUID>of() : vt.members) {
                            ServerPlayer sp = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
                            if (sp == null) continue;
                            NetworkHandler.send(
                                    new PacketPlayTeamSound("minecraft:entity.slime.squish", 0.9f, 1.22f),
                                    PacketDistributor.PLAYER.with(sp)
                            );
                        }
                    }
            );
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
            return true;
        }

        return false;
    }
    private int findFakeRealSlotIndexById(String realId) {
        if (realId == null || realId.isBlank()) return -1;
        for (Map.Entry<Integer, BingoSlot> entry : fakeRealSlotByIndex.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            String id = entry.getValue().getId();
            if (id != null && id.equals(realId)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    private void revealFakeRealForOtherTeamsOnRealCompletion(MinecraftServer server, UUID completingTeamId, String completedSlotId) {
        if (server == null || completedSlotId == null || completedSlotId.isBlank()) return;
        int idx = findFakeRealSlotIndexById(completedSlotId);
        if (idx < 0) return;
        TeamData teamData = TeamData.get(server);
        for (TeamData.TeamInfo team : teamData.getTeams()) {
            if (team == null || team.id == null || team.members == null || team.members.isEmpty()) continue;
            UUID teamId = team.id;
            if (teamId.equals(completingTeamId)) continue;
            if (isFakeRealRevealedForTeam(teamId, idx)) continue;
            BingoCard teamCard = getActiveCardForTeam(teamId);
            if (teamCard == null || teamCard.getSize() <= 0) continue;
            int x = idx % teamCard.getSize();
            int y = idx / teamCard.getSize();
            if (x < 0 || y < 0 || x >= teamCard.getSize() || y >= teamCard.getSize()) continue;
            BingoSlot visible = teamCard.getSlot(x, y);
            String visibleId = visible == null ? "" : (visible.getId() == null ? "" : visible.getId());
            if (visibleId.isBlank()) continue;
            sendFlashSlotsStyled(server, teamId, Set.of(visibleId), FAKE_REVEAL_PRE_TICKS, PacketFlashSlots.STYLE_SHAKE);
            com.jamie.jamiebingo.casino.CasinoTickScheduler.schedule(
                    server,
                    com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + FAKE_REVEAL_PRE_TICKS,
                    () -> {
                        BingoGameData data = BingoGameData.get(server);
                        if (data == null || !data.isActive()) return;
                        if (!data.revealFakeRealForTeam(teamId, idx)) return;
                        for (UUID memberId : team.members) {
                            ServerPlayer sp = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
                            if (sp != null) {
                                BroadcastHelper.syncCard(sp);
                            }
                        }
                        sendFlashSlotsStyled(server, teamId, Set.of(completedSlotId), FAKE_REVEAL_POST_TICKS, PacketFlashSlots.STYLE_PULSE);
                    }
            );
        }
    }
    private boolean handleCompletionFeedback(MinecraftServer server, UUID teamId, String slotId) {
        if (server == null || teamId == null || slotId == null) return false;
        LOGGER.info("[JamieBingo] handleCompletionFeedback slotId={} teamId={}", slotId, teamId);

        BingoCard card = getActiveCardForTeam(teamId);
        if (card == null) return false;

        Set<String> completed = getTeamProgressForDisplay(teamId);
        Set<String> lineSlots = computeNewLineSlots(teamId, card, completed, slotId);
        boolean lineCompleted = !lineSlots.isEmpty();

        SoundEvent sound = lineCompleted
                ? ModSounds.COMPLETE_LINE.get()
                : ModSounds.COMPLETE_SLOT.get();
        LOGGER.info("[JamieBingo] Completion sound selected: {}", sound == null ? "null" : sound.toString());
        if (sound != null) {
            playTeamSound(server, teamId, sound, 1.0f, 1.0f);
        }

        Set<String> flashSlots = lineCompleted ? lineSlots : Set.of(slotId);
        sendFlashSlots(server, teamId, flashSlots, COMPLETION_FLASH_TICKS);
        return lineCompleted;
    }

    private void applyShuffleRerollForTeam(MinecraftServer server, UUID teamId, Set<String> completedSlotIds) {
        if (server == null || teamId == null || completedSlotIds == null) return;
        if (!isShuffleActive() || currentCard == null) return;
        if (winCondition == WinCondition.LOCKOUT || winCondition == WinCondition.RARITY) {
            applySharedShuffleReroll(server, teamId, completedSlotIds);
            return;
        }
        if (winCondition != WinCondition.FULL) return;

        BingoCard teamCard = shuffleCardsByTeam.computeIfAbsent(teamId, t -> copyCard(currentCard));
        if (teamCard == null) return;
        Set<String> seenIds = shuffleSeenIdsByTeam.computeIfAbsent(teamId, t -> new HashSet<>(teamCard.getAllIds()));
        Set<String> lockedLineSlots = computeAllCompletedLineSlots(teamCard, completedSlotIds);
        boolean progressPruned = completedSlotIds.removeIf(id -> !lockedLineSlots.contains(id));

        int size = teamCard.getSize();
        int[] steps = shuffleStepsByTeam.computeIfAbsent(teamId, t -> new int[size * size]);
        boolean consumed = false;
        boolean changed = false;
        Set<String> newlyAppearedIds = new HashSet<>();
        Map<String, Integer> occupiedCounts = buildIdCounts(teamCard);
        Random fallbackRng = new Random();

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int idx = y * size + x;
                BingoSlot current = teamCard.getSlot(x, y);
                if (current == null) continue;
                if (completedSlotIds.contains(current.getId())) continue;
                decrementIdCount(occupiedCounts, current.getId());

                List<BingoSlot> queue = shuffleQueueBySlot.get(idx);
                if (queue == null || queue.isEmpty() || idx < 0 || idx >= steps.length || steps[idx] >= queue.size()) {
                    incrementIdCount(occupiedCounts, current.getId());
                    continue;
                }

                BingoSlot chosen = null;
                while (steps[idx] < queue.size()) {
                    BingoSlot candidate = queue.get(steps[idx]++);
                    consumed = true;
                    if (candidate == null || candidate.getId() == null || candidate.getId().isBlank()) continue;
                    if (occupiedCounts.getOrDefault(candidate.getId(), 0) > 0) continue;
                    if (seenIds.contains(candidate.getId())) continue;
                    chosen = candidate;
                    break;
                }

                if (chosen == null) {
                    BingoSlot fallback = generateRerolledSlotForPosition(
                            server, fallbackRng, x, y, size, current.getId(), lockedLineSlots, occupiedCounts, seenIds, true
                    );
                    if (fallback == null) {
                        incrementIdCount(occupiedCounts, current.getId());
                        continue;
                    }
                    consumed = true;
                    chosen = fallback;
                }

                if (!Objects.equals(current.getId(), chosen.getId())) {
                    teamCard.setSlot(x, y, copySlot(chosen));
                    changed = true;
                    newlyAppearedIds.add(chosen.getId());
                }
                seenIds.add(chosen.getId());
                incrementIdCount(occupiedCounts, chosen.getId());
            }
        }

        if (consumed || progressPruned) {
            Set<String> validIds = teamCard.getAllIds();
            Set<String> highlights = highlightedSlots.get(teamId);
            if (highlights != null && highlights.removeIf(id -> !validIds.contains(id))) {
                BroadcastHelper.broadcastHighlightedSlots(server);
            }
            TeamData teamData = TeamData.get(server);
            TeamData.TeamInfo teamInfo = teamData.getTeams().stream()
                    .filter(t -> teamId.equals(t.id))
                    .findFirst()
                    .orElse(null);
            if (changed && teamInfo != null) {
                for (UUID memberId : teamInfo.members) {
                    ServerPlayer sp = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
                    if (sp != null) {
                        armRerolledGoalTrackingForPlayer(sp, newlyAppearedIds);
                        BroadcastHelper.syncCard(sp);
                    }
                }
            }
            if (changed || progressPruned) {
                if (teamInfo != null) {
                    Set<String> teamSet = teamProgress.get(teamId);
                    if (teamSet != null) {
                        teamSet.removeIf(id -> !validIds.contains(id));
                    }
                }
                TeamScoreData scores = TeamScoreData.get(server);
                if (winCondition == WinCondition.RARITY) {
                    scores.recomputeRarityScores(server, this);
                } else {
                    scores.recomputeStandardScores(server, this);
                }
                BroadcastHelper.broadcastProgress(server);
                BroadcastHelper.broadcastTeamScores(server);
            }
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        }
    }

    private void applySharedShuffleReroll(MinecraftServer server, UUID triggeringTeamId, Set<String> triggeringTeamCompleted) {
        if (server == null || currentCard == null || triggeringTeamId == null || triggeringTeamCompleted == null) return;
        int size = currentCard.getSize();
        if (size <= 0) return;

        Set<String> lockedLineSlots = computeAllCompletedLineSlotsForAllTeams(currentCard);
        Set<String> removedCompletions = new HashSet<>();
        Iterator<String> completionIt = triggeringTeamCompleted.iterator();
        while (completionIt.hasNext()) {
            String id = completionIt.next();
            if (lockedLineSlots.contains(id)) continue;
            completionIt.remove();
            removedCompletions.add(id);
            Set<UUID> owners = slotOwners.get(id);
            if (owners != null) {
                owners.remove(triggeringTeamId);
                if (owners.isEmpty()) {
                    slotOwners.remove(id);
                }
            }
        }

        Random rng = new Random();
        Set<String> completedIds = new HashSet<>(lockedLineSlots);
        if (shuffleSeenIdsShared.isEmpty()) {
            shuffleSeenIdsShared.addAll(currentCard.getAllIds());
        }
        boolean changed = false;
        boolean tieRerollOccurred = false;
        Set<String> newlyAppearedIds = new HashSet<>();
        Map<String, Integer> occupiedCounts = buildIdCounts(currentCard);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BingoSlot slot = currentCard.getSlot(x, y);
                if (slot == null || slot.getId() == null || slot.getId().isBlank()) continue;
                if (completedIds.contains(slot.getId())) continue;
                String previousId = slot.getId();
                decrementIdCount(occupiedCounts, previousId);

                BingoSlot replacement = generateRerolledSlotForPosition(
                        server, rng, x, y, size, previousId, completedIds, occupiedCounts, shuffleSeenIdsShared, true
                );
                if (replacement == null) {
                    incrementIdCount(occupiedCounts, previousId);
                    continue;
                }
                if (!Objects.equals(slot.getId(), replacement.getId())) {
                    currentCard.setSlot(x, y, replacement);
                    changed = true;
                    newlyAppearedIds.add(replacement.getId());
                }
                shuffleSeenIdsShared.add(replacement.getId());
                incrementIdCount(occupiedCounts, replacement.getId());
            }
        }

        if (!changed && removedCompletions.isEmpty()) return;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BingoSlot slot = currentCard.getSlot(x, y);
                if (slot == null || slot.getId() == null || slot.getId().isBlank()) continue;
                if (completedIds.contains(slot.getId())) continue;
                while (countTeamsWithSlotComplete(server, slot.getId()) >= 2) {
                    String previousId = slot.getId();
                    decrementIdCount(occupiedCounts, previousId);
                    BroadcastHelper.broadcast(server, com.jamie.jamiebingo.util.ComponentUtil.literal("tied goal, rerolled slot"));
                    tieRerollOccurred = true;
                    BingoSlot rerolled = generateRerolledSlotForPosition(
                            server, rng, x, y, size, previousId, completedIds, occupiedCounts, shuffleSeenIdsShared, true
                    );
                    if (rerolled == null) {
                        incrementIdCount(occupiedCounts, previousId);
                        break;
                    }
                    currentCard.setSlot(x, y, rerolled);
                    slot = rerolled;
                    changed = true;
                    newlyAppearedIds.add(rerolled.getId());
                    shuffleSeenIdsShared.add(rerolled.getId());
                    incrementIdCount(occupiedCounts, rerolled.getId());
                }
            }
        }

        if (tieRerollOccurred) {
            LOGGER.info("[JamieBingo] Shared shuffle tie reroll applied.");
        }
        if (sanitizeImmediateSpawnTies(server)) {
            LOGGER.info("[JamieBingo] Shared shuffle immediate tie reroll applied.");
        }
        Set<String> validIds = currentCard.getAllIds();
        for (Set<String> teamSet : teamProgress.values()) {
            if (teamSet == null) continue;
            teamSet.removeIf(id -> !validIds.contains(id));
        }
        slotOwners.entrySet().removeIf(e -> e.getKey() == null || !validIds.contains(e.getKey()));

        for (ServerPlayer player : getParticipantPlayers(server)) {
            armRerolledGoalTrackingForPlayer(player, newlyAppearedIds);
        }
        TeamScoreData scores = TeamScoreData.get(server);
        if (winCondition == WinCondition.RARITY) {
            scores.recomputeRarityScores(server, this);
        } else {
            scores.recomputeStandardScores(server, this);
        }
        BroadcastHelper.broadcastSlotOwnership(server);
        BroadcastHelper.broadcastProgress(server);
        BroadcastHelper.broadcastTeamScores(server);
        BroadcastHelper.broadcastFullSync();
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    private void armRerolledGoalTrackingForPlayer(ServerPlayer player, Set<String> slotIds) {
        if (player == null || slotIds == null || slotIds.isEmpty()) return;
        for (String slotId : slotIds) {
            if (slotId == null || !slotId.startsWith("quest.")) continue;
            if (!QuestTracker.isQuestComplete(player, slotId)) continue;
            QuestTracker.complete(player, slotId);
        }
    }

    public int rerollCurrentGoalsForVote(MinecraftServer server) {
        if (server == null || !active) return 0;

        Random rng = new Random();
        int changed = 0;
        Set<String> sharedNewIds = new HashSet<>();
        Map<UUID, Set<String>> perTeamNewIds = new HashMap<>();

        if (winCondition == WinCondition.GUNGAME) {
            TeamData teamData = TeamData.get(server);
            Map<Integer, Set<UUID>> teamsByIndex = new HashMap<>();
            for (TeamData.TeamInfo team : teamData.getTeams()) {
                if (team == null || team.id == null || team.members.isEmpty()) continue;
                int idx = gunGameTeamIndex.getOrDefault(team.id, 0);
                teamsByIndex.computeIfAbsent(idx, k -> new HashSet<>()).add(team.id);
            }

            for (Map.Entry<Integer, Set<UUID>> entry : teamsByIndex.entrySet()) {
                int idx = entry.getKey();
                if (idx < 0 || idx >= gunGameCards.size()) continue;
                BingoCard card = gunGameCards.get(idx);
                if (card == null) continue;

                Set<String> locked = new HashSet<>();
                for (UUID teamId : entry.getValue()) {
                    Map<Integer, Set<String>> byIdx = gunGameProgress.get(teamId);
                    if (byIdx == null) continue;
                    Set<String> done = byIdx.get(idx);
                    if (done != null) {
                        locked.addAll(done);
                    }
                }

                Set<String> newlyAppeared = new HashSet<>();
                int cardChanged = rerollUncompletedSlotsInCard(server, card, locked, rng, newlyAppeared);
                if (cardChanged <= 0) continue;
                changed += cardChanged;
                sharedNewIds.addAll(newlyAppeared);

                Set<String> validIds = card.getAllIds();
                for (UUID teamId : entry.getValue()) {
                    Map<Integer, Set<String>> byIdx = gunGameProgress.get(teamId);
                    if (byIdx != null) {
                        Set<String> done = byIdx.get(idx);
                        if (done != null) {
                            done.removeIf(id -> !validIds.contains(id));
                        }
                    }
                    pruneTeamVisualStateToValid(teamId, validIds);
                }
            }
        } else if (isShuffleActive() && winCondition == WinCondition.FULL && !shuffleCardsByTeam.isEmpty()) {
            for (Map.Entry<UUID, BingoCard> entry : shuffleCardsByTeam.entrySet()) {
                UUID teamId = entry.getKey();
                BingoCard card = entry.getValue();
                if (teamId == null || card == null) continue;

                Set<String> completed = teamProgress.computeIfAbsent(teamId, k -> new HashSet<>());
                Set<String> newlyAppeared = new HashSet<>();
                int cardChanged = rerollUncompletedSlotsInCard(server, card, completed, rng, newlyAppeared);
                if (cardChanged <= 0) continue;
                changed += cardChanged;
                perTeamNewIds.put(teamId, newlyAppeared);

                Set<String> validIds = card.getAllIds();
                completed.removeIf(id -> !validIds.contains(id));
                pruneTeamVisualStateToValid(teamId, validIds);
            }
        } else {
            BingoCard card = currentCard;
            if (card == null) return 0;
            Map<UUID, Set<Integer>> blindRevealedByTeam = new HashMap<>();
            Map<UUID, Set<Integer>> blindHighlightsByTeam = new HashMap<>();
            if (winCondition == WinCondition.BLIND) {
                TeamData teamData = TeamData.get(server);
                Set<UUID> teamIds = new HashSet<>();
                for (TeamData.TeamInfo team : teamData.getTeams()) {
                    if (team != null && team.id != null) {
                        teamIds.add(team.id);
                    }
                }
                teamIds.addAll(revealedSlots.keySet());
                teamIds.addAll(highlightedSlots.keySet());
                teamIds.addAll(teamProgress.keySet());
                for (UUID teamId : teamIds) {
                    Set<String> revealed = revealedSlots.get(teamId);
                    if (revealed != null && !revealed.isEmpty()) {
                        blindRevealedByTeam.put(teamId, captureSlotIndices(card, revealed));
                    }
                    Set<String> highlights = highlightedSlots.get(teamId);
                    if (highlights != null && !highlights.isEmpty()) {
                        blindHighlightsByTeam.put(teamId, captureSlotIndices(card, highlights));
                    }
                }
            }

            Set<String> completedIds = getAllCompletedSlotIds();
            Set<String> newlyAppeared = new HashSet<>();
            changed = rerollUncompletedSlotsInCard(server, card, completedIds, rng, newlyAppeared);
            if (changed > 0) {
                sharedNewIds.addAll(newlyAppeared);
                Set<String> validIds = card.getAllIds();
                for (Set<String> teamSet : teamProgress.values()) {
                    if (teamSet != null) {
                        teamSet.removeIf(id -> !validIds.contains(id));
                    }
                }
                slotOwners.entrySet().removeIf(e -> e.getKey() == null || !validIds.contains(e.getKey()));
                if (winCondition == WinCondition.BLIND) {
                    Set<UUID> teamIds = new HashSet<>();
                    teamIds.addAll(teamProgress.keySet());
                    teamIds.addAll(blindRevealedByTeam.keySet());
                    teamIds.addAll(blindHighlightsByTeam.keySet());
                    for (UUID teamId : teamIds) {
                        restoreSlotsFromIndices(card, revealedSlots.get(teamId), blindRevealedByTeam.get(teamId));
                        restoreSlotsFromIndices(card, highlightedSlots.get(teamId), blindHighlightsByTeam.get(teamId));
                        pruneTeamVisualStateToValid(teamId, validIds);
                    }
                } else {
                    for (UUID teamId : teamProgress.keySet()) {
                        pruneTeamVisualStateToValid(teamId, validIds);
                    }
                }

                if (winCondition == WinCondition.GAMEGUN && !gunGameCards.isEmpty()) {
                    int idx = Math.max(0, Math.min(gunGameSharedIndex, gunGameCards.size() - 1));
                    gunGameCards.set(idx, copyCard(currentCard));
                }
                if (winCondition == WinCondition.HANGMAN && !hangmanCards.isEmpty()) {
                    int idx = Math.max(0, Math.min(hangmanRoundIndex, hangmanCards.size() - 1));
                    hangmanCards.set(idx, copyCard(currentCard));
                    resetHangmanRuntimeForReroll(server);
                }
            }
        }

        if (changed > 0) {
            if (!sharedNewIds.isEmpty()) {
                for (ServerPlayer player : getParticipantPlayers(server)) {
                    armRerolledGoalTrackingForPlayer(player, sharedNewIds);
                }
            }
            if (!perTeamNewIds.isEmpty()) {
                TeamData teamData = TeamData.get(server);
                for (Map.Entry<UUID, Set<String>> entry : perTeamNewIds.entrySet()) {
                    TeamData.TeamInfo team = teamData.getTeams().stream()
                            .filter(t -> t.id.equals(entry.getKey()))
                            .findFirst()
                            .orElse(null);
                    if (team == null) continue;
                    for (UUID memberId : team.members) {
                        ServerPlayer player = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
                        if (player != null) {
                            armRerolledGoalTrackingForPlayer(player, entry.getValue());
                        }
                    }
                }
            }

            BroadcastHelper.broadcastProgress(server);
            BroadcastHelper.broadcastTeamScores(server);
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        }

        return changed;
    }

    private int rerollUncompletedSlotsInCard(
            MinecraftServer server,
            BingoCard card,
            Set<String> lockedIds,
            Random rng,
            Set<String> newlyAppearedIds
    ) {
        if (server == null || card == null || card.getSize() <= 0) return 0;
        Set<String> locked = lockedIds == null ? Set.of() : lockedIds;
        Map<String, Integer> occupiedCounts = buildIdCounts(card);
        int size = card.getSize();
        int changed = 0;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot == null || slot.getId() == null || slot.getId().isBlank()) continue;
                if (locked.contains(slot.getId())) continue;

                String previousId = slot.getId();
                decrementIdCount(occupiedCounts, previousId);
                BingoSlot rerolled = generateRerolledSlotForPosition(
                        server, rng, x, y, size, previousId, locked, occupiedCounts, null, true
                );
                if (rerolled == null) {
                    incrementIdCount(occupiedCounts, previousId);
                    continue;
                }
                if (!Objects.equals(previousId, rerolled.getId())) {
                    card.setSlot(x, y, rerolled);
                    if (newlyAppearedIds != null) {
                        newlyAppearedIds.add(rerolled.getId());
                    }
                    changed++;
                }
                incrementIdCount(occupiedCounts, rerolled.getId());
            }
        }

        return changed;
    }

    private void pruneTeamVisualStateToValid(UUID teamId, Set<String> validIds) {
        if (teamId == null || validIds == null) return;
        Set<String> highlights = highlightedSlots.get(teamId);
        if (highlights != null) {
            highlights.removeIf(id -> !validIds.contains(id));
        }
        Set<String> revealed = revealedSlots.get(teamId);
        if (revealed != null) {
            revealed.removeIf(id -> !validIds.contains(id));
        }
    }

    private Set<Integer> captureSlotIndices(BingoCard card, Set<String> slotIds) {
        Set<Integer> indices = new HashSet<>();
        if (card == null || slotIds == null || slotIds.isEmpty()) return indices;
        int size = card.getSize();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot == null || slot.getId() == null) continue;
                if (slotIds.contains(slot.getId())) {
                    indices.add(y * size + x);
                }
            }
        }
        return indices;
    }

    private void restoreSlotsFromIndices(BingoCard card, Set<String> target, Set<Integer> indices) {
        if (card == null || target == null || indices == null) return;
        target.clear();
        int size = card.getSize();
        for (Integer idx : indices) {
            if (idx == null) continue;
            if (idx < 0) continue;
            int x = idx % size;
            int y = idx / size;
            if (x < 0 || y < 0 || x >= size || y >= size) continue;
            BingoSlot slot = card.getSlot(x, y);
            if (slot == null || slot.getId() == null || slot.getId().isBlank()) continue;
            target.add(slot.getId());
        }
    }

    private void resetHangmanRuntimeForReroll(MinecraftServer server) {
        hangmanCurrentSlotId = "";
        hangmanCurrentWord = "";
        hangmanMaskedWord = "";
        hangmanRevealCount = 0;
        hangmanNextRevealTick = -1;
        hangmanSlotRevealed = false;
        hangmanIntermissionEndTick = -1;
        if (server != null) {
            BroadcastHelper.broadcastHangmanState(server, true, false, "", "");
        }
    }

    private Set<String> computeAllCompletedLineSlotsForAllTeams(BingoCard card) {
        Set<String> out = new HashSet<>();
        if (card == null) return out;
        for (Set<String> completed : teamProgress.values()) {
            if (completed == null || completed.isEmpty()) continue;
            out.addAll(computeAllCompletedLineSlots(card, completed));
        }
        return out;
    }

    private Set<String> computeNewLineSlots(
            UUID teamId,
            BingoCard card,
            Set<String> completed,
            String slotId
    ) {
        if (card == null || completed == null || slotId == null) return Set.of();
        int[] pos = findSlotCoords(teamId, slotId);
        if (pos == null) return Set.of();

        int size = card.getSize();
        int x = pos[0];
        int y = pos[1];
        Set<String> out = new HashSet<>();

        if (rowComplete(card, completed, y)) {
            for (int cx = 0; cx < size; cx++) {
                BingoSlot slot = card.getSlot(cx, y);
                if (slot != null) {
                    out.add(slot.getId());
                }
            }
        }
        if (columnComplete(card, completed, x)) {
            for (int cy = 0; cy < size; cy++) {
                BingoSlot slot = card.getSlot(x, cy);
                if (slot != null) {
                    out.add(slot.getId());
                }
            }
        }
        if (x == y && diagonalMain(card, completed)) {
            for (int i = 0; i < size; i++) {
                BingoSlot slot = card.getSlot(i, i);
                if (slot != null) {
                    out.add(slot.getId());
                }
            }
        }
        if (x + y == size - 1 && diagonalAnti(card, completed)) {
            for (int i = 0; i < size; i++) {
                BingoSlot slot = card.getSlot(size - 1 - i, i);
                if (slot != null) {
                    out.add(slot.getId());
                }
            }
        }
        return out;
    }

    private boolean rowComplete(BingoCard card, Set<String> completed, int y) {
        for (int x = 0; x < card.getSize(); x++) {
            BingoSlot slot = card.getSlot(x, y);
            if (slot == null) return false;
            if (!completed.contains(slot.getId())) return false;
        }
        return true;
    }

    private boolean columnComplete(BingoCard card, Set<String> completed, int x) {
        for (int y = 0; y < card.getSize(); y++) {
            BingoSlot slot = card.getSlot(x, y);
            if (slot == null) return false;
            if (!completed.contains(slot.getId())) return false;
        }
        return true;
    }

    private boolean diagonalMain(BingoCard card, Set<String> completed) {
        for (int i = 0; i < card.getSize(); i++) {
            BingoSlot slot = card.getSlot(i, i);
            if (slot == null) return false;
            if (!completed.contains(slot.getId())) return false;
        }
        return true;
    }

    private boolean diagonalAnti(BingoCard card, Set<String> completed) {
        for (int i = 0; i < card.getSize(); i++) {
            BingoSlot slot = card.getSlot(card.getSize() - 1 - i, i);
            if (slot == null) return false;
            if (!completed.contains(slot.getId())) return false;
        }
        return true;
    }

    private Set<String> computeAllCompletedLineSlots(BingoCard card, Set<String> completed) {
        Set<String> locked = new HashSet<>();
        if (card == null || completed == null || completed.isEmpty()) return locked;
        int size = card.getSize();

        for (int y = 0; y < size; y++) {
            if (!rowComplete(card, completed, y)) continue;
            for (int x = 0; x < size; x++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot != null && slot.getId() != null && !slot.getId().isBlank()) {
                    locked.add(slot.getId());
                }
            }
        }
        for (int x = 0; x < size; x++) {
            if (!columnComplete(card, completed, x)) continue;
            for (int y = 0; y < size; y++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot != null && slot.getId() != null && !slot.getId().isBlank()) {
                    locked.add(slot.getId());
                }
            }
        }
        if (diagonalMain(card, completed)) {
            for (int i = 0; i < size; i++) {
                BingoSlot slot = card.getSlot(i, i);
                if (slot != null && slot.getId() != null && !slot.getId().isBlank()) {
                    locked.add(slot.getId());
                }
            }
        }
        if (diagonalAnti(card, completed)) {
            for (int i = 0; i < size; i++) {
                BingoSlot slot = card.getSlot(size - 1 - i, i);
                if (slot != null && slot.getId() != null && !slot.getId().isBlank()) {
                    locked.add(slot.getId());
                }
            }
        }
        return locked;
    }

    public int getCompletedLineCount(UUID teamId) {
        if (teamId == null) return 0;
        BingoCard card = getActiveCardForTeam(teamId);
        if (card == null) return 0;
        Set<String> completed = getTeamProgressForDisplay(teamId);
        return computeCompletedLineCount(card, completed);
    }

    private int computeCompletedLineCount(BingoCard card, Set<String> completed) {
        if (card == null || completed == null || completed.isEmpty()) return 0;
        int size = card.getSize();
        int count = 0;
        for (int y = 0; y < size; y++) {
            if (rowComplete(card, completed, y)) count++;
        }
        for (int x = 0; x < size; x++) {
            if (columnComplete(card, completed, x)) count++;
        }
        if (diagonalMain(card, completed)) count++;
        if (diagonalAnti(card, completed)) count++;
        return count;
    }

    private void playTeamSound(
            MinecraftServer server,
            UUID teamId,
            SoundEvent sound,
            float volume,
            float pitch
    ) {
        if (server == null || teamId == null || sound == null) return;
        LOGGER.info("[JamieBingo] playTeamSound soundKey={}", ForgeRegistries.SOUND_EVENTS.getKey(sound));
        TeamData teamData = TeamData.get(server);
        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);
        if (team == null) return;

        String soundKey = ForgeRegistries.SOUND_EVENTS.getKey(sound) != null
                ? ForgeRegistries.SOUND_EVENTS.getKey(sound).toString()
                : "";
        PacketPlayTeamSound pkt = new PacketPlayTeamSound(soundKey, volume, pitch);

        for (UUID memberId : team.members) {
            ServerPlayer player = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
            if (player != null) {
                NetworkHandler.send(pkt, PacketDistributor.PLAYER.with(player));
                LOGGER.info("[JamieBingo] Sent team sound packet: {}", soundKey);
            }
        }
    }

    private void sendFlashSlots(
            MinecraftServer server,
            UUID teamId,
            Set<String> slots,
            int durationTicks
    ) {
        if (server == null || teamId == null || slots == null || slots.isEmpty()) return;
        TeamData teamData = TeamData.get(server);
        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);
        if (team == null) return;

        PacketFlashSlots pkt = new PacketFlashSlots(slots, durationTicks);
        for (UUID memberId : team.members) {
            ServerPlayer player = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
            if (player != null) {
                NetworkHandler.send(pkt, PacketDistributor.PLAYER.with(player));
            }
        }
    }

    private void sendFlashSlotsStyled(
            MinecraftServer server,
            UUID teamId,
            Set<String> slots,
            int durationTicks,
            int style
    ) {
        if (server == null || teamId == null || slots == null || slots.isEmpty()) return;
        TeamData teamData = TeamData.get(server);
        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);
        if (team == null) return;

        PacketFlashSlots pkt = new PacketFlashSlots(slots, durationTicks, style);
        for (UUID memberId : team.members) {
            ServerPlayer player = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
            if (player != null) {
                NetworkHandler.send(pkt, PacketDistributor.PLAYER.with(player));
            }
        }
    }

    public void revealAllBlindSlots(MinecraftServer server) {
        if (currentCard == null) return;
        if (server == null) return;

        TeamData teamData = TeamData.get(server);
        for (TeamData.TeamInfo team : teamData.getTeams()) {
            Set<String> revealed = getRevealedSlots(team.id);
            for (int y = 0; y < currentCard.getSize(); y++) {
                for (int x = 0; x < currentCard.getSize(); x++) {
                    BingoSlot slot = currentCard.getSlot(x, y);
                    if (slot != null) {
                        revealed.add(slot.getId());
                    }
                }
            }
        }
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    private void playOpponentGoalSound(MinecraftServer server, UUID winningTeamId) {
        if (server == null || winningTeamId == null) return;

        TeamData teamData = TeamData.get(server);
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            UUID teamId = teamData.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
            if (teamId == null || teamId.equals(winningTeamId)) continue;

            net.minecraft.world.level.Level level = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(player);
            if (level != null) {
                level.playSound(
                        null,
                        com.jamie.jamiebingo.util.EntityPosUtil.getBlockPos(player),
                        SoundEvents.VILLAGER_NO,
                        SoundSource.PLAYERS,
                        0.8f,
                        0.9f
                );
            }
        }
    }

    public boolean removeCompletedForTeam(UUID teamId, String id) {
        if (teamId == null || id == null) return false;

        boolean removed = false;

        if (winCondition == WinCondition.GUNGAME) {
            int idx = gunGameTeamIndex.getOrDefault(teamId, 0);
            Set<String> teamSet = gunGameProgress
                    .getOrDefault(teamId, Map.of())
                    .getOrDefault(idx, null);
            if (teamSet != null) {
                removed = teamSet.remove(id);
            }
        } else {
            Set<String> teamSet = teamProgress.get(teamId);
            if (teamSet != null) {
                removed = teamSet.remove(id);
            }
        }

        if (!removed) return false;

        if (winCondition == WinCondition.LOCKOUT || winCondition == WinCondition.RARITY) {
            Set<UUID> owners = slotOwners.get(id);
            if (owners != null) {
                owners.remove(teamId);
                if (owners.isEmpty()) {
                    slotOwners.remove(id);
                }
            }
        }

        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        return true;
    }

    /* =========================
       REROLL API
       ========================= */

    public boolean consumeReroll(UUID playerId) {
        int used = rerollsUsed.getOrDefault(playerId, 0);
        if (used >= rerollsPerPlayer) return false;
        rerollsUsed.put(playerId, used + 1);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        return true;
    }

    public int getRemainingRerolls(UUID playerId) {
        return Math.max(0, rerollsPerPlayer - rerollsUsed.getOrDefault(playerId, 0));
    }

    public void beginRerollPhase(Collection<UUID> players) {
        rerollTurnOrder.clear();
        rerollTurnOrder.addAll(players);
        rerollTurnIndex = 0;
        rerollPhaseActive = !rerollTurnOrder.isEmpty();
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public void endRerollPhase() {
        rerollPhaseActive = false;
        rerollTurnOrder.clear();
        rerollTurnIndex = 0;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public boolean isRerollPhaseActive() {
        return rerollPhaseActive;
    }

    public UUID getCurrentRerollPlayer() {
        if (!rerollPhaseActive || rerollTurnOrder.isEmpty()) return null;
        return rerollTurnOrder.get(rerollTurnIndex);
    }

    public void advanceRerollTurn() {
        if (!rerollPhaseActive || rerollTurnOrder.isEmpty()) return;
        rerollTurnIndex = (rerollTurnIndex + 1) % rerollTurnOrder.size();
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public boolean consumeFakeReroll(UUID playerId) {
        if (playerId == null) return false;
        int used = fakeRerollsUsed.getOrDefault(playerId, 0);
        if (used >= fakeRerollsPerPlayer) return false;
        fakeRerollsUsed.put(playerId, used + 1);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        return true;
    }

    public int getRemainingFakeRerolls(UUID playerId) {
        if (playerId == null) return 0;
        return Math.max(0, fakeRerollsPerPlayer - fakeRerollsUsed.getOrDefault(playerId, 0));
    }

    public void markRunCommandUsed() {
        if (currentRunCommandsUsed) return;
        currentRunCommandsUsed = true;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public void markRunVoteRerollUsed() {
        if (currentRunVoteRerollUsed) return;
        currentRunVoteRerollUsed = true;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public boolean hasRunVoteRerollUsed() {
        return currentRunVoteRerollUsed;
    }

    public boolean hasRunUsedCommands() {
        return currentRunCommandsUsed;
    }

    public int getTotalRerollsUsedThisRun() {
        int total = 0;
        for (Integer value : rerollsUsed.values()) {
            if (value != null) total += Math.max(0, value);
        }
        return total;
    }

    public int getTotalFakeRerollsUsedThisRun() {
        int total = 0;
        for (Integer value : fakeRerollsUsed.values()) {
            if (value != null) total += Math.max(0, value);
        }
        return total;
    }

    public void beginFakeRerollPhase(Collection<UUID> players) {
        fakeRerollTurnOrder.clear();
        if (players != null) {
            fakeRerollTurnOrder.addAll(players);
        }
        fakeRerollTurnIndex = 0;
        fakeRerollPhaseActive = !fakeRerollTurnOrder.isEmpty() && fakeRerollsPerPlayer > 0;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public void endFakeRerollPhase() {
        fakeRerollPhaseActive = false;
        fakeRerollTurnOrder.clear();
        fakeRerollTurnIndex = 0;
        fakeRerollsUsed.clear();
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public boolean isFakeRerollPhaseActive() {
        return fakeRerollPhaseActive;
    }

    public UUID getCurrentFakeRerollPlayer() {
        if (!fakeRerollPhaseActive || fakeRerollTurnOrder.isEmpty()) return null;
        return fakeRerollTurnOrder.get(fakeRerollTurnIndex);
    }

    public void advanceFakeRerollTurn() {
        if (!fakeRerollPhaseActive || fakeRerollTurnOrder.isEmpty()) return;
        fakeRerollTurnIndex = (fakeRerollTurnIndex + 1) % fakeRerollTurnOrder.size();
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public boolean hasFakeRealAtIndex(int slotIndex) {
        return fakeRealSlotByIndex.containsKey(slotIndex);
    }

    public BingoSlot getFakeRealAtIndex(int slotIndex) {
        return fakeRealSlotByIndex.get(slotIndex);
    }

    public boolean isFakeRealRevealedForTeam(UUID teamId, int slotIndex) {
        if (teamId == null) return false;
        return fakeRevealedIndicesByTeam.getOrDefault(teamId, Set.of()).contains(slotIndex);
    }

    public boolean teamHasFakeSelectableSlots(UUID teamId) {
        if (teamId == null || currentCard == null) return false;
        int total = currentCard.getSize() * currentCard.getSize();
        if (total <= 0) return false;
        return fakeChosenIndicesByTeam.getOrDefault(teamId, Set.of()).size() < total;
    }

    public Set<Integer> getUnrevealedFakeIndicesForTeam(UUID teamId) {
        if (teamId == null || fakeRealSlotByIndex.isEmpty()) return Set.of();
        Set<Integer> revealed = fakeRevealedIndicesByTeam.getOrDefault(teamId, Set.of());
        Set<Integer> out = new HashSet<>();
        for (Integer idx : fakeRealSlotByIndex.keySet()) {
            if (idx == null) continue;
            if (revealed.contains(idx)) continue;
            out.add(idx);
        }
        return out;
    }

    public Set<Integer> getRevealedFakeIndicesForTeam(UUID teamId) {
        if (teamId == null) return Set.of();
        return new HashSet<>(fakeRevealedIndicesByTeam.getOrDefault(teamId, Set.of()));
    }

    public Set<Integer> getChosenFakeIndicesForTeam(UUID teamId) {
        if (teamId == null) return Set.of();
        return new HashSet<>(fakeChosenIndicesByTeam.getOrDefault(teamId, Set.of()));
    }

    public boolean hasTeamChosenFakeSlot(UUID teamId, int slotIndex) {
        if (teamId == null || slotIndex < 0) return false;
        return fakeChosenIndicesByTeam.getOrDefault(teamId, Set.of()).contains(slotIndex);
    }

    public Set<Integer> getGreenFakeIndicesForTeam(UUID teamId) {
        return getChosenFakeIndicesForTeam(teamId);
    }

    public Set<Integer> getRedFakeIndicesForTeam(UUID teamId) {
        if (teamId == null) return Set.of();
        Set<Integer> red = getRevealedFakeIndicesForTeam(teamId);
        red.removeAll(getChosenFakeIndicesForTeam(teamId));
        return red;
    }

    private boolean isFakeRevealPendingForTeam(UUID teamId, int slotIndex) {
        if (teamId == null || slotIndex < 0) return false;
        return fakeRevealPendingIndicesByTeam.getOrDefault(teamId, Set.of()).contains(slotIndex);
    }

    private void markFakeRevealPendingForTeam(UUID teamId, int slotIndex) {
        if (teamId == null || slotIndex < 0) return;
        fakeRevealPendingIndicesByTeam.computeIfAbsent(teamId, k -> new HashSet<>()).add(slotIndex);
    }

    private void clearFakeRevealPendingForTeam(UUID teamId, int slotIndex) {
        if (teamId == null || slotIndex < 0) return;
        Set<Integer> set = fakeRevealPendingIndicesByTeam.get(teamId);
        if (set == null) return;
        set.remove(slotIndex);
        if (set.isEmpty()) {
            fakeRevealPendingIndicesByTeam.remove(teamId);
        }
    }

    public UUID getFakeSourceTeamId(int slotIndex) {
        return fakeSourceTeamByIndex.get(slotIndex);
    }

    public String getFakeSourcePlayerName(int slotIndex) {
        return fakeSourcePlayerNameByIndex.get(slotIndex);
    }

    public BingoSlot assignOrGetFakeRealSlot(MinecraftServer server, int slotIndex, UUID sourceTeamId, String sourcePlayerName) {
        if (server == null || currentCard == null || slotIndex < 0) return null;
        if (sourceTeamId != null) {
            fakeChosenIndicesByTeam.computeIfAbsent(sourceTeamId, k -> new HashSet<>()).add(slotIndex);
        }
        BingoSlot existing = fakeRealSlotByIndex.get(slotIndex);
        if (existing != null) {
            return copySlot(existing);
        }

        Set<String> excluded = new HashSet<>();
        int sizeNow = currentCard.getSize();
        for (int y = 0; y < sizeNow; y++) {
            for (int x = 0; x < sizeNow; x++) {
                BingoSlot slot = currentCard.getSlot(x, y);
                if (slot == null || slot.getId() == null || slot.getId().isBlank()) continue;
                excluded.add(slot.getId());
            }
        }
        for (BingoSlot slot : fakeRealSlotByIndex.values()) {
            if (slot == null || slot.getId() == null || slot.getId().isBlank()) continue;
            excluded.add(slot.getId());
        }

        Random rng = new Random();
        BingoSlot generated = generateRandomGoalNotOnCurrentCard(server, rng, excluded);
        if (generated == null || generated.getId() == null || generated.getId().isBlank()) {
            return null;
        }
        BingoSlot assigned = new BingoSlot(generated.getId(), generated.getName(), generated.getCategory(), generated.getRarity());
        fakeRealSlotByIndex.put(slotIndex, assigned);
        fakeSourceTeamByIndex.put(slotIndex, sourceTeamId);
        fakeSourcePlayerNameByIndex.put(slotIndex, sourcePlayerName == null ? "" : sourcePlayerName);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        return copySlot(assigned);
    }

    public boolean revealFakeRealForTeam(UUID teamId, int slotIndex) {
        if (teamId == null || currentCard == null || slotIndex < 0) return false;
        BingoSlot real = fakeRealSlotByIndex.get(slotIndex);
        if (real == null || real.getId() == null || real.getId().isBlank()) return false;
        int sizeNow = currentCard.getSize();
        int x = slotIndex % sizeNow;
        int y = slotIndex / sizeNow;
        if (x < 0 || y < 0 || x >= sizeNow || y >= sizeNow) return false;
        BingoCard card = getOrCreateTeamCardOverride(teamId);
        if (card == null) return false;
        card.setSlot(x, y, copySlot(real));
        fakeRevealedIndicesByTeam.computeIfAbsent(teamId, k -> new HashSet<>()).add(slotIndex);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        return true;
    }

    public void clearFakeRerollRuntime() {
        fakeRerollPhaseActive = false;
        fakeRerollTurnOrder.clear();
        fakeRerollTurnIndex = 0;
        fakeRerollsUsed.clear();
        fakeRealSlotByIndex.clear();
        fakeSourceTeamByIndex.clear();
        fakeSourcePlayerNameByIndex.clear();
        fakeRevealedIndicesByTeam.clear();
        fakeRevealPendingIndicesByTeam.clear();
        fakeChosenIndicesByTeam.clear();
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    /* =========================
       GAME LIFECYCLE
       ========================= */

public boolean startGame(MinecraftServer server) {

    com.jamie.jamiebingo.bingo.BingoWinEvaluator.resetGameEnding();
    PacketVoteEndGame.clearVotes(server);
    PacketVoteRerollCard.clearVotes(server);
    if (worldRegenInProgress || worldRegenQueued) {
        LOGGER.warn("[JamieBingo] startGame ignored (world regeneration active/queued).");
        return false;
    }
    if (active || startCountdownActive) {
        LOGGER.warn("[JamieBingo] startGame ignored (already active or countdown in progress).");
        return false;
    }
    if (worldUseNewSeedEachGame && !worldFreshSeedPrepared) {
        pendingStartAfterWorldRegen = true;
        com.jamie.jamiebingo.world.WorldRegenerationManager.queueRegeneration(server, "start_game");
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        return false;
    }
    com.jamie.jamiebingo.casino.CasinoModeManager.resetTransientState(server, true);

    long generationDeadline = System.nanoTime() + CARD_GENERATION_TIMEOUT_NANOS;
    generationTimedOut = false;

 Random rng = new Random();
 startCountdownActive = false;
 startCountdownSeconds = 0;
 startCountdownEndTick = -1;
 gameStartTick = -1;
 countdownEndTick = -1;
 countdownExpired = false;
 pendingWinEndActive = false;
 pendingWinEndTick = -1;
 lastRandomEffectKey = "";
 randomEffectUseCounts.clear();
 hangmanCards.clear();
 hangmanRoundIndex = 0;
 hangmanNextRevealTick = -1;
 hangmanRevealCount = 0;
 hangmanCurrentSlotId = "";
 hangmanCurrentWord = "";
 hangmanMaskedWord = "";
 hangmanSlotRevealed = false;
 hangmanIntermissionEndTick = -1;

/* ðŸŽ¯ Card size (Gaussian-like, centered on 5) */
if (randomSizeIntent) {
    int[] sizes = {1,2,3,4,5,6,7,8,9,10};
    double[] weights = new double[sizes.length];

    for (int i = 0; i < sizes.length; i++) {
        int d = sizes[i] - 5;
        weights[i] = Math.exp(-0.6 * d * d);
    }

    size = weightedPick(sizes, weights, rng);
    randomSizeIntent = false;
}

/* ðŸŽ¯ Win condition (even odds) */
    boolean resolvedRandomWinToHangman = false;
    if (randomWinConditionIntent) {
        winCondition = pickRandomWinCondition(server, rng);
        resolvedRandomWinToHangman = winCondition == WinCondition.HANGMAN;
        randomWinConditionIntent = false;
    }

    if (winCondition == WinCondition.LOCKOUT
            && countActiveTeams(server) <= 1) {
        winCondition = WinCondition.LINE;
    }

    if (!isCasinoAllowedForWin(winCondition)) {
        com.jamie.jamiebingo.casino.CasinoModeManager.setCasinoEnabled(false);
    }

    applyPregameModeConflictRules();
    startDelaySeconds = Math.max(0, bingoStartDelaySeconds);

/* ðŸŽ¯ Difficulty (even odds) */
if (randomDifficultyIntent) {
    String[] diffs = {"easy", "normal", "hard"};
    difficulty = diffs[rng.nextInt(diffs.length)];
}

// Hangman is always size 1
if (winCondition == WinCondition.HANGMAN) {
    size = 1;
    randomSizeIntent = false;
    if (resolvedRandomWinToHangman) {
        hangmanBaseSeconds = pickRandomHangmanBaseSeconds(rng);
        hangmanPenaltySeconds = pickRandomHangmanPenaltySeconds(rng);
    }
}

/* ðŸŽ¯ Rerolls (1 common â†’ 5 rare) */
if (randomRerollsIntent) {
    int[] values = {1,2,3,4,5};
    double[] weights = {1.0, 0.6, 0.35, 0.15, 0.05};
    rerollsPerPlayer = weightedPick(values, weights, rng);
}

    applyPregameModeConflictRules();

/* ðŸŽ¯ Hostile mobs */
if (randomHostileMobsIntent) {
    hostileMobsEnabled = rng.nextBoolean();
}

/* ðŸŽ¯ Hunger */
if (randomHungerIntent) {
    hungerEnabled = rng.nextBoolean();
}

/* ðŸŽ¯ RTP */
if (randomRtpIntent) {
    rtpEnabled = rng.nextBoolean();
}

/* ðŸŽ¯ PVP */
if (randomPvpIntent) {
    pvpEnabled = rng.nextBoolean();
    randomPvpIntent = false;
}

/* ðŸŽ¯ Random effects interval (20â€“300, centered on 60) */
if (randomEffectsIntervalIntent) {
    boolean enabled = rng.nextBoolean();
    randomEffectsArmed = enabled;
    if (enabled) {
        randomEffectsIntervalSeconds = pickRandomEffectInterval(rng);
    } else {
        randomEffectsIntervalSeconds = 0;
    }
    randomEffectsIntervalIntent = false;
}

/* ðŸŽ¯ Daylight cycle */
if (randomDaylightIntent) {
    int roll = rng.nextInt(6);
    daylightMode = switch (roll) {
        case 1 -> DAYLIGHT_DAY;
        case 2 -> DAYLIGHT_NIGHT;
        case 3 -> DAYLIGHT_MIDNIGHT;
        case 4 -> DAYLIGHT_DAWN;
        case 5 -> DAYLIGHT_DUSK;
        default -> DAYLIGHT_ENABLED;
    };
    randomDaylightIntent = false;
}

/* ðŸŽ¯ Hardcore (10% chance) */
if (randomHardcoreIntent) {
    hardcoreEnabled = rng.nextDouble() < 0.10;
    randomHardcoreIntent = false;
}

/* ðŸŽ¯ Keep Inventory */
if (randomKeepInventoryIntent) {
    keepInventoryEnabled = rng.nextBoolean();
    randomKeepInventoryIntent = false;
}

if (randomShuffleIntent) {
    shuffleEnabled = rng.nextBoolean();
    randomShuffleIntent = false;
}

if (!isShuffleSupportedWinCondition(winCondition)) {
    shuffleEnabled = false;
    randomShuffleIntent = false;
}

if (rtpEnabled) {
    pendingGameStartRtp = true;
}

    com.jamie.jamiebingo.data.TeamChestData.get(server).clearAll();
    Set<String> generationBlacklist = buildGenerationBlacklist();

    // 1ï¸âƒ£ Generate initial shared card (used by all non-GUNGAME modes)
    BingoCard customCard = null;
    if (customPoolEnabled && winCondition != WinCondition.HANGMAN
            && winCondition != WinCondition.GUNGAME
            && winCondition != WinCondition.GAMEGUN) {
        customCard = buildCardFromPool(size, rng);
    } else if (customCardEnabled && winCondition != WinCondition.HANGMAN
            && winCondition != WinCondition.GUNGAME
            && winCondition != WinCondition.GAMEGUN) {
        customCard = buildCustomCardFromSlots();
    }

        if (winCondition == WinCondition.HANGMAN) {
            hangmanCards.clear();
            int rounds = Math.max(2, Math.min(20, hangmanRounds));
            Set<String> usedRoundIds = new HashSet<>();
            for (int i = 0; i < rounds; i++) {
                if (checkGenerationTimeout(server, generationDeadline)) {
                    abortCardGeneration(server);
                    return false;
                }
                BingoCard card = generateUniqueCard(() ->
                        com.jamie.jamiebingo.bingo.ConfigurableCardGenerator.generate(
                                1,
                                difficulty,
                                composition,
                                questPercent,
                                server,
                                this,
                                generationBlacklist
                        ), usedRoundIds, generationDeadline, server);
                if (card == null) {
                    card = new BingoCard(1);
                }
                hangmanCards.add(card);
                addCardIdsToSet(card, usedRoundIds);
            }
            currentCard = hangmanCards.get(0);
            hangmanRoundIndex = 0;
        } else if (customCard != null) {
            currentCard = customCard;
            size = customCard.getSize();
        } else {
        if (checkGenerationTimeout(server, generationDeadline)) {
            abortCardGeneration(server);
            return false;
        }
        currentCard = com.jamie.jamiebingo.bingo.ConfigurableCardGenerator.generate(
                size,
                difficulty,
                composition,
                questPercent,
                server,
                this,
                generationBlacklist
        );
    }

// 2ï¸âƒ£ Pre-generate GunGame / GameGun card sequence
gunGameCards.clear();

        if (winCondition == WinCondition.GUNGAME || winCondition == WinCondition.GAMEGUN) {
            Set<String> usedRoundIds = new HashSet<>();
            for (int i = 0; i < gunGameLength; i++) {
                if (checkGenerationTimeout(server, generationDeadline)) {
                    abortCardGeneration(server);
                    return false;
                }
                BingoCard card = generateUniqueCard(
                        () -> com.jamie.jamiebingo.bingo.ConfigurableCardGenerator.generate(
                                size,
                                difficulty,
                                CardComposition.CLASSIC_ONLY,
                                0,
                                server,
                                this,
                                generationBlacklist
                        ),
                        usedRoundIds,
                        generationDeadline,
                        server
                );
                if (card == null) {
                    card = new BingoCard(size);
                }
                gunGameCards.add(card);
                addCardIdsToSet(card, usedRoundIds);
            }
            currentCard = gunGameCards.get(0);
        }

    boolean pregamePhasesPending = casinoMode != CASINO_DISABLED || rerollsPerPlayer > 0;
    if (pregamePhasesPending) {
        clearShuffleRuntime();
    } else {
        rebuildShuffleQueuesFromCurrentCard(server);
    }

 // ðŸŽ² Reset random effects state at game start
activeRandomEffectId = "";
activeRandomEffectName = "";
activeRandomEffectAmplifier = 0;
appliedRandomEffectId = "";
randomEffectsNextTick = -1;
currentRunVoteRerollUsed = false;

// ðŸŽ¯ Activate random effects ONLY if they were armed
if (randomEffectsArmed) {
    randomEffectsEnabled = true;
}

    // 3ï¸âƒ£ Reset state
    active = true;
    progress.clear();
    teamProgress.clear();
    starterKitGrantedPlayers.clear();
    slotOwners.clear();
    revealedSlots.clear();
    highlightedSlots.clear();
    rerollsUsed.clear();
    clearFakeRerollRuntime();
    eliminatedPlayers.clear();
    eliminatedTeams.clear();
    rushDeadlineTickByTeam.clear();
    rushResumeSecondsByTeam.clear();
    clearMineResumeStateInternal(false);
    clearPowerSlotResumeStateInternal(false);
gunGameProgress.clear(); // âœ… CLEAR GUNGAME PER-CARD PROGRESS

    gunGameTeamIndex.clear();
    gunGameSharedIndex = 0;

    endRerollPhase();
    initRevealedSlots(server);

    initializeParticipants(server);
    if (!pregamePhasesPending) {
        initializeFullShuffleTeamCards(server);
        initializeShuffleSeenState(server);
    }
    lockQuestsForAllParticipants();

   TeamScoreData.get(server).reset();
TeamScoreData.get(server).ensureTeamsExist(server);
BroadcastHelper.broadcastTeamScores(server);
    BroadcastHelper.broadcastFullSync();
    if (winCondition != WinCondition.HANGMAN) {
        BroadcastHelper.broadcastHangmanState(server, false, true, "", "");
    }
    if (winCondition == WinCondition.GUNGAME) {
        handleImmediateGunGameTie(server, 0);
    }

    resetPlayerAdvancements(server);
    QuestTracker.resetForGame(com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server));
    QuestEvents.resetForGame(server);
com.jamie.jamiebingo.mines.MineModeManager.start(server, this);
    cacheCurrentSeed(server);

worldFreshSeedPrepared = false;
pendingStartAfterWorldRegen = false;
com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
return true;
}

    private void applyPregameModeConflictRules() {
        if (!isPowerSlotSupportedWinCondition(winCondition)) {
            powerSlotEnabled = false;
        }
        if (!isFakeRerollsSupportedWinCondition(winCondition)) {
            fakeRerollsEnabled = false;
        }
        if (winCondition != WinCondition.HANGMAN && winCondition != WinCondition.BLIND) {
            if (!isShuffleSupportedWinCondition(winCondition)) {
                shuffleEnabled = false;
                randomShuffleIntent = false;
            }
            return;
        }
        casinoMode = CASINO_DISABLED;
        com.jamie.jamiebingo.casino.CasinoModeManager.setCasinoEnabled(false);
        rerollsPerPlayer = 0;
        randomRerollsIntent = false;
        shuffleEnabled = false;
        randomShuffleIntent = false;
        bingoStartDelaySeconds = 0;
        startDelaySeconds = 0;
    }

    public boolean startGameFromSeed(MinecraftServer server, com.jamie.jamiebingo.bingo.CardSeedCodec.SeedData seed) {
        if (server == null || seed == null || seed.cards() == null || seed.cards().isEmpty()) {
            return false;
        }

        com.jamie.jamiebingo.bingo.BingoWinEvaluator.resetGameEnding();

        CompoundTag settings = seed.settings();
        if (settings == null) return false;

        String winName = settings.getString("Win").orElse(null);
        String compositionName = settings.getString("Composition").orElse(null);
        if (winName == null || compositionName == null) return false;

        try {
            winCondition = WinCondition.valueOf(winName);
            composition = CardComposition.valueOf(compositionName);
        } catch (Exception e) {
            return false;
        }

        questPercent = getInt(settings, "QuestPercent", questPercent);
        size = Math.max(1, getInt(settings, "Size", size));
        if (winCondition == WinCondition.HANGMAN) {
            size = 1;
        }
        difficulty = getString(settings, "Difficulty", difficulty);
        gunGameLength = getInt(settings, "GunRounds", gunGameLength);
        gunGameShared = getBoolean(settings, "GunShared", gunGameShared);
        rerollsPerPlayer = getInt(settings, "Rerolls", rerollsPerPlayer);
        casinoMode = settings.contains("CasinoMode")
                ? getInt(settings, "CasinoMode", casinoMode)
                : (getBoolean(settings, "Casino", false) ? CASINO_ENABLED : CASINO_DISABLED);
        minesEnabled = settings.contains("MinesEnabled")
                ? getBoolean(settings, "MinesEnabled", minesEnabled)
                : minesEnabled;
        mineAmount = settings.contains("MineAmount")
                ? Math.max(1, Math.min(13, getInt(settings, "MineAmount", mineAmount)))
                : mineAmount;
        mineTimeSeconds = settings.contains("MineTimeSeconds")
                ? Math.max(1, getInt(settings, "MineTimeSeconds", mineTimeSeconds))
                : mineTimeSeconds;
        powerSlotEnabled = settings.contains("PowerSlotEnabled")
                ? getBoolean(settings, "PowerSlotEnabled", powerSlotEnabled)
                : powerSlotEnabled;
        powerSlotIntervalSeconds = settings.contains("PowerSlotIntervalSeconds")
                ? Math.max(10, Math.min(300, getInt(settings, "PowerSlotIntervalSeconds", powerSlotIntervalSeconds)))
                : powerSlotIntervalSeconds;
        fakeRerollsEnabled = settings.contains("FakeRerollsEnabled")
                ? getBoolean(settings, "FakeRerollsEnabled", fakeRerollsEnabled)
                : fakeRerollsEnabled;
        fakeRerollsPerPlayer = settings.contains("FakeRerollsPerPlayer")
                ? Math.max(1, Math.min(10, getInt(settings, "FakeRerollsPerPlayer", fakeRerollsPerPlayer)))
                : fakeRerollsPerPlayer;
        randomEffectsIntervalSeconds = getInt(settings, "EffectsInterval", randomEffectsIntervalSeconds);
        randomEffectsArmed = getBoolean(settings, "EffectsArmed", randomEffectsArmed);
        rtpEnabled = getBoolean(settings, "Rtp", rtpEnabled);
        hostileMobsEnabled = getBoolean(settings, "HostileMobs", hostileMobsEnabled);
        hungerEnabled = getBoolean(settings, "Hunger", hungerEnabled);
        naturalRegenEnabled = getBoolean(settings, "NaturalRegen", naturalRegenEnabled);
        pvpEnabled = getBoolean(settings, "Pvp", pvpEnabled);
        adventureMode = getBoolean(settings, "AdventureMode", adventureMode);
        prelitPortalsMode = settings.contains("PrelitPortalsMode")
                ? clampPrelitPortalsMode(getInt(settings, "PrelitPortalsMode", prelitPortalsMode))
                : prelitPortalsMode;
        keepInventoryEnabled = getBoolean(settings, "KeepInv", keepInventoryEnabled);
        hardcoreEnabled = getBoolean(settings, "Hardcore", hardcoreEnabled);
        daylightMode = getInt(settings, "Daylight", daylightMode);
        bingoStartDelaySeconds = getInt(settings, "StartDelay", bingoStartDelaySeconds);
        countdownEnabled = getBoolean(settings, "CountdownEnabled", countdownEnabled);
        countdownMinutes = getInt(settings, "CountdownMinutes", countdownMinutes);
        rushEnabled = getBoolean(settings, "RushEnabled", rushEnabled);
        rushSeconds = Math.max(1, Math.min(300, getInt(settings, "RushSeconds", rushSeconds)));
        allowLateJoin = getBoolean(settings, "AllowLateJoin", allowLateJoin);
        teamChestEnabled = getBoolean(settings, "TeamChestEnabled", teamChestEnabled);
        registerMode = getInt(settings, "RegisterMode", registerMode);
        teamSyncEnabled = getBoolean(settings, "TeamSyncEnabled", teamSyncEnabled);
        shuffleEnabled = getBoolean(settings, "ShuffleEnabled", shuffleEnabled);
        starterKitMode = normalizeStarterKitMode(getInt(settings, "StarterKitMode", starterKitMode));
        hideGoalDetailsInChat = getBoolean(settings, "HideGoalDetailsInChat", hideGoalDetailsInChat);
        randomShuffleIntent = false;
        categoryLogicEnabled = settings.contains("CategoryLogicEnabled")
                ? getBoolean(settings, "CategoryLogicEnabled", categoryLogicEnabled)
                : categoryLogicEnabled;
        rarityLogicEnabled = settings.contains("RarityLogicEnabled")
                ? getBoolean(settings, "RarityLogicEnabled", rarityLogicEnabled)
                : rarityLogicEnabled;
        itemColorVariantsSeparate = settings.contains("ItemColorVariantsSeparate")
                ? getBoolean(settings, "ItemColorVariantsSeparate", itemColorVariantsSeparate)
                : itemColorVariantsSeparate;
        worldTypeMode = normalizeWorldType(settings.contains("WorldTypeMode")
                ? getInt(settings, "WorldTypeMode", worldTypeMode)
                : worldTypeMode);
        worldSmallBiomes = settings.contains("WorldSmallBiomes")
                ? getBoolean(settings, "WorldSmallBiomes", worldSmallBiomes)
                : worldSmallBiomes;
        worldCustomBiomeSizeBlocks = settings.contains("WorldCustomBiomeSizeBlocks")
                ? clampWorldBiomeSize(settings.getInt("WorldCustomBiomeSizeBlocks").orElse(worldCustomBiomeSizeBlocks))
                : worldCustomBiomeSizeBlocks;
        worldTerrainHillinessPercent = settings.contains("WorldTerrainHillinessPercent")
                ? clampWorldTerrainHilliness(settings.getInt("WorldTerrainHillinessPercent").orElse(worldTerrainHillinessPercent))
                : worldTerrainHillinessPercent;
        worldStructureFrequencyPercent = settings.contains("WorldStructureFrequencyPercent")
                ? clampWorldStructureFrequency(settings.getInt("WorldStructureFrequencyPercent").orElse(worldStructureFrequencyPercent))
                : worldStructureFrequencyPercent;
        worldSingleBiomeId = settings.contains("WorldSingleBiomeId")
                ? getString(settings, "WorldSingleBiomeId", worldSingleBiomeId)
                : worldSingleBiomeId;
        if (worldSingleBiomeId == null || worldSingleBiomeId.isBlank()) {
            worldSingleBiomeId = "minecraft:plains";
        }
        worldSurfaceCaveBiomes = settings.contains("WorldSurfaceCaveBiomes")
                ? getBoolean(settings, "WorldSurfaceCaveBiomes", worldSurfaceCaveBiomes)
                : worldSurfaceCaveBiomes;
        worldSetSeedText = settings.contains("WorldSetSeedText")
                ? getString(settings, "WorldSetSeedText", worldSetSeedText)
                : worldSetSeedText;
        if (worldSetSeedText == null) worldSetSeedText = "";

        if (!isCasinoAllowedForWin(winCondition)) {
            casinoMode = CASINO_DISABLED;
            com.jamie.jamiebingo.casino.CasinoModeManager.setCasinoEnabled(false);
        } else {
            com.jamie.jamiebingo.casino.CasinoModeManager.setCasinoEnabled(casinoMode == CASINO_ENABLED);
        }
        if (!isShuffleSupportedWinCondition(winCondition)) {
            shuffleEnabled = false;
        }
        applyPregameModeConflictRules();
        startDelaySeconds = Math.max(0, bingoStartDelaySeconds);
        customCardEnabled = settings.contains("CustomCardEnabled")
                ? getBoolean(settings, "CustomCardEnabled", customCardEnabled)
                : customCardEnabled;
        customPoolEnabled = settings.contains("CustomPoolEnabled")
                ? getBoolean(settings, "CustomPoolEnabled", customPoolEnabled)
                : customPoolEnabled;
        hangmanRounds = settings.contains("HangmanRounds")
                ? getInt(settings, "HangmanRounds", hangmanRounds)
                : hangmanRounds;
        hangmanBaseSeconds = settings.contains("HangmanBaseSeconds")
                ? Math.max(10, getInt(settings, "HangmanBaseSeconds", hangmanBaseSeconds))
                : hangmanBaseSeconds;
        hangmanPenaltySeconds = settings.contains("HangmanPenaltySeconds")
                ? getInt(settings, "HangmanPenaltySeconds", hangmanPenaltySeconds)
                : hangmanPenaltySeconds;

        randomSizeIntent = false;
        randomWinConditionIntent = false;
        randomDifficultyIntent = false;
        randomRerollsIntent = false;
        randomHostileMobsIntent = false;
        randomHungerIntent = false;
        randomRtpIntent = false;
        randomDaylightIntent = false;
        randomPvpIntent = false;
        randomEffectsIntervalIntent = false;
        randomHardcoreIntent = false;
        randomKeepInventoryIntent = false;
        randomShuffleIntent = false;

        customPoolIds.clear();
        if (settings.contains("CustomPoolIds")) {
            ListTag list = settings.getListOrEmpty("CustomPoolIds");
            for (int i = 0; i < list.size(); i++) {
                String id = list.getStringOr(i, "");
                if (!id.isBlank()) {
                    customPoolIds.add(id);
                }
            }
        }
        customMineIds.clear();
        if (settings.contains("CustomMineIds")) {
            ListTag list = settings.getListOrEmpty("CustomMineIds");
            for (int i = 0; i < list.size(); i++) {
                String id = list.getStringOr(i, "");
                if (!id.isBlank()) {
                    customMineIds.add(id);
                }
            }
        }
        List<String> selectedMineSourceIds = new ArrayList<>();
        if (settings.contains("SelectedMineSourceIds")) {
            ListTag list = settings.getListOrEmpty("SelectedMineSourceIds");
            for (int i = 0; i < list.size(); i++) {
                String id = list.getStringOr(i, "");
                if (!id.isBlank()) {
                    selectedMineSourceIds.add(id);
                }
            }
        }

          if (customPoolEnabled) {
              customCardEnabled = false;
              BingoCard pooled = buildCardFromPool(size, new Random());
              if (pooled != null) {
                  currentCard = pooled;
                  size = pooled.getSize();
              }
          } else if (customCardEnabled && !seed.cards().isEmpty()) {
              ensureCustomCardSlotsSize(100);
              Collections.fill(customCardSlots, "");
              BingoCard c = seed.cards().get(0);
              if (c != null) {
                  int idx = 0;
                  for (int y = 0; y < c.getSize(); y++) {
                      for (int x = 0; x < c.getSize(); x++) {
                          BingoSlot slot = c.getSlot(x, y);
                          if (slot != null && idx < customCardSlots.size()) {
                            customCardSlots.set(idx, slot.getId());
                            idx++;
                        }
                    }
                }
            }
        }

        hangmanCards.clear();
        hangmanRoundIndex = 0;
        hangmanNextRevealTick = -1;
        hangmanRevealCount = 0;
        hangmanCurrentSlotId = "";
        hangmanCurrentWord = "";
        hangmanMaskedWord = "";
        hangmanSlotRevealed = false;
        hangmanIntermissionEndTick = -1;

        if (winCondition == WinCondition.HANGMAN) {
            hangmanCards.clear();
            hangmanCards.addAll(seed.cards());
            hangmanRounds = seed.cards().size();
            currentCard = seed.cards().get(0);
            hangmanRoundIndex = 0;
          } else {
              if (!customPoolEnabled) {
                  setGunGameCardsFromSeed(seed.cards());
                  if (winCondition == WinCondition.GUNGAME || winCondition == WinCondition.GAMEGUN) {
                      if (!seed.cards().isEmpty()) {
                          gunGameLength = seed.cards().size();
                          currentCard = seed.cards().get(0);
                      }
                  } else {
                      currentCard = seed.cards().get(0);
                  }
              }
          }

        clearShuffleRuntime();
        Set<String> generationBlacklist = buildGenerationBlacklist();
        if (isShuffleActive() && currentCard != null && winCondition == WinCondition.FULL) {
            Random shuffleRng = new Random();
            int queueDepth = Math.max(0, maxPossibleLinesForSize(currentCard.getSize()) - 1);
            for (int i = 0; i < queueDepth; i++) {
                BingoCard nextCard;
                if (customPoolEnabled && winCondition != WinCondition.HANGMAN
                        && winCondition != WinCondition.GUNGAME
                        && winCondition != WinCondition.GAMEGUN) {
                    nextCard = buildCardFromPool(size, shuffleRng);
                } else if (customCardEnabled && winCondition != WinCondition.HANGMAN
                        && winCondition != WinCondition.GUNGAME
                        && winCondition != WinCondition.GAMEGUN) {
                    nextCard = buildCustomCardFromSlots();
                } else {
                    nextCard = com.jamie.jamiebingo.bingo.ConfigurableCardGenerator.generate(
                            size,
                            difficulty,
                            composition,
                            questPercent,
                            server,
                            this,
                            generationBlacklist
                    );
                }
                if (nextCard == null || nextCard.getSize() != currentCard.getSize()) continue;
                int n = nextCard.getSize();
                for (int y = 0; y < n; y++) {
                    for (int x = 0; x < n; x++) {
                        int idx = y * n + x;
                        BingoSlot slot = nextCard.getSlot(x, y);
                        if (slot == null) continue;
                        shuffleQueueBySlot.computeIfAbsent(idx, k -> new ArrayList<>()).add(copySlot(slot));
                    }
                }
            }
        }

        pendingGameStartRtp = rtpEnabled;
        com.jamie.jamiebingo.data.TeamChestData.get(server).clearAll();

        activeRandomEffectId = "";
        activeRandomEffectName = "";
        activeRandomEffectAmplifier = 0;
        appliedRandomEffectId = "";
        randomEffectsNextTick = -1;
        randomEffectsEnabled = randomEffectsArmed;
        startCountdownActive = false;
        startCountdownSeconds = 0;
        startCountdownEndTick = -1;
        gameStartTick = -1;
        countdownEndTick = -1;
        countdownExpired = false;
        pendingWinEndActive = false;
        pendingWinEndTick = -1;
        pendingWinEndActive = false;
        pendingWinEndTick = -1;
        lastRandomEffectKey = "";
        randomEffectUseCounts.clear();

        active = true;
        progress.clear();
        teamProgress.clear();
        starterKitGrantedPlayers.clear();
        slotOwners.clear();
        revealedSlots.clear();
        highlightedSlots.clear();
        rerollsUsed.clear();
        eliminatedPlayers.clear();
        eliminatedTeams.clear();
        rushDeadlineTickByTeam.clear();
        rushResumeSecondsByTeam.clear();
        clearMineResumeStateInternal(false);
        setMineSeedSelection(selectedMineSourceIds);
        clearPowerSlotResumeStateInternal(false);
        gunGameProgress.clear();
        gunGameTeamIndex.clear();
        gunGameSharedIndex = 0;

        endRerollPhase();
        initRevealedSlots(server);
        initializeParticipants(server);
        if (isShuffleActive() && currentCard != null && winCondition == WinCondition.FULL) {
            TeamData teams = TeamData.get(server);
            for (TeamData.TeamInfo team : teams.getTeams()) {
                if (team == null || team.id == null || team.members.isEmpty()) continue;
                shuffleCardsByTeam.put(team.id, copyCard(currentCard));
                shuffleStepsByTeam.put(team.id, new int[currentCard.getSize() * currentCard.getSize()]);
            }
        }
        initializeShuffleSeenState(server);
        lockQuestsForAllParticipants();

        TeamScoreData.get(server).reset();
        TeamScoreData.get(server).ensureTeamsExist(server);
        BroadcastHelper.broadcastTeamScores(server);
          BroadcastHelper.broadcastFullSync();
          if (winCondition != WinCondition.HANGMAN) {
              BroadcastHelper.broadcastHangmanState(server, false, true, "", "");
          }
          if (winCondition == WinCondition.GUNGAME) {
              handleImmediateGunGameTie(server, 0);
          }

        resetPlayerAdvancements(server);
        QuestTracker.resetForGame(com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server));
        QuestEvents.resetForGame(server);
    com.jamie.jamiebingo.mines.MineModeManager.start(server, this);
        cacheCurrentSeed(server);

com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        return true;
    }

    public void stopGame() {
      PacketVoteEndGame.clearVotes(ServerLifecycleHooks.getCurrentServer());
      PacketVoteRerollCard.clearVotes(ServerLifecycleHooks.getCurrentServer());
      captureCurrentRunResult(ServerLifecycleHooks.getCurrentServer(), false);
      com.jamie.jamiebingo.mines.MineModeManager.endGameHoldDisplay(ServerLifecycleHooks.getCurrentServer());
      com.jamie.jamiebingo.power.PowerSlotManager.clear(ServerLifecycleHooks.getCurrentServer());
      com.jamie.jamiebingo.casino.CasinoModeManager.resetTransientState(ServerLifecycleHooks.getCurrentServer(), true);
      com.jamie.jamiebingo.bingo.BingoWinEvaluator.resetGameEnding();
      stopGamePending = false;
  clearActiveWeeklyChallenge();
      stopGamePendingTick = -1;
      // ðŸŽ² Clear random effect amplifier
     activeRandomEffectId = "";
activeRandomEffectName = "";
activeRandomEffectAmplifier = 0;
appliedRandomEffectId = "";
randomEffectsNextTick = -1;
randomEffectsEnabled = false;
startCountdownActive = false;
startCountdownSeconds = 0;
startCountdownEndTick = -1;
gameStartTick = -1;
countdownEndTick = -1;
resumeElapsedSeconds = -1;
resumeCountdownRemainingSeconds = -1;
timerResumePending = false;
countdownExpired = false;
pendingWinEndActive = false;
pendingWinEndTick = -1;
pendingWinEndActive = false;
pendingWinEndTick = -1;
lastRandomEffectKey = "";
randomEffectUseCounts.clear();
participants.clear();
spectators.clear();
spectatorViewTargets.clear();
lateJoinPending.clear();
questReleasePending.clear();
hangmanCards.clear();
hangmanRoundIndex = 0;
hangmanNextRevealTick = -1;
hangmanRevealCount = 0;
hangmanCurrentSlotId = "";
hangmanCurrentWord = "";
hangmanMaskedWord = "";
hangmanSlotRevealed = false;
hangmanIntermissionEndTick = -1;
currentRunPreviewSize = 0;
currentRunPreviewSlotIds.clear();
        active = false;
        currentCard = null;
        progress.clear();
        teamProgress.clear();
        starterKitGrantedPlayers.clear();
        slotOwners.clear();
        revealedSlots.clear();
        highlightedSlots.clear();
        rerollsUsed.clear();
        eliminatedPlayers.clear();
        eliminatedTeams.clear();
        rushDeadlineTickByTeam.clear();
        rushResumeSecondsByTeam.clear();
        clearMineResumeStateInternal(false);
        clearPowerSlotResumeStateInternal(false);
        participants.clear();
        spectators.clear();
        spectatorViewTargets.clear();
        lateJoinPending.clear();
        questReleasePending.clear();
        hangmanCards.clear();
        hangmanRoundIndex = 0;
        hangmanNextRevealTick = -1;
        hangmanRevealCount = 0;
        hangmanCurrentSlotId = "";
        hangmanCurrentWord = "";
        hangmanMaskedWord = "";
        hangmanSlotRevealed = false;
        hangmanIntermissionEndTick = -1;
        clearShuffleRuntime();
gunGameProgress.clear(); // âœ… HERE TOO
        endRerollPhase();
           QuestTracker.clear();
           QuestEvents.resetForGame(ServerLifecycleHooks.getCurrentServer());

    // ðŸ”« Reset GunGame state
    gunGameTeamIndex.clear();
    gunGameSharedIndex = 0;
    gunGameLength = 0;
   gunGameShared = false;

      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      clearAllActiveEffects(server);
        if (server != null) {
            BroadcastHelper.broadcastStartCountdown(server, 0);
            BroadcastHelper.broadcastGameTimer(server, false, false, 0);
            BroadcastHelper.broadcastHangmanState(server, false, true, "", "");
            clearAllPlayerInventories(server);
            for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
                    com.jamie.jamiebingo.util.GameModeTracking.runWithoutCommandTracking(player, () -> player.setGameMode(GameType.SURVIVAL));
                }
                com.jamie.jamiebingo.menu.PlayerControllerSettingsStore.clear(player);
            }
            com.jamie.jamiebingo.world.PregameBoxManager.sendAllToBox(server);
            com.jamie.jamiebingo.world.PregameSettingsWallManager.refreshFromData(server);
            if (worldUseNewSeedEachGame) {
                com.jamie.jamiebingo.world.WorldRegenerationManager.queueRegeneration(server, "end_game");
            } else {
                com.jamie.jamiebingo.casino.CasinoTickScheduler.schedule(
                        server,
                        com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + 5,
                        () -> com.jamie.jamiebingo.world.LobbyWorldManager.startPreloadingGameStartSpawn(server, this, true)
                );
            }
            com.jamie.jamiebingo.item.BingoControllerGiveHandler.giveJoinItemsToAll(server);
        }

      customCardEnabled = false;
      customPoolEnabled = false;

    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public void scheduleWinEnd(MinecraftServer server, int delayTicks) {
        if (server == null) return;
        pendingWinEndActive = true;
        pendingWinEndTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + Math.max(0, delayTicks);
        com.jamie.jamiebingo.casino.CasinoTickScheduler.schedule(
                server,
                pendingWinEndTick,
                () -> {
                    BingoGameData data = BingoGameData.get(server);
                    if (data.pendingWinEndActive && com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) >= data.pendingWinEndTick) {
                        data.stopGameAfterWin(server);
                        data.clearPendingWinEnd();
                    }
                }
        );
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public void clearPendingWinEnd() {
        pendingWinEndActive = false;
        pendingWinEndTick = -1;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    private int pickRandomEffectInterval(Random rng) {
        double value = 40 + rng.nextGaussian() * 35.0;
        int clamped = (int) Math.round(Math.max(20, Math.min(300, value)));
        return clamped;
    }

    private int pickRandomHangmanBaseSeconds(Random rng) {
        double value = 30 + rng.nextGaussian() * 18.0;
        return (int) Math.round(Math.max(10, Math.min(300, value)));
    }

    private int pickRandomHangmanPenaltySeconds(Random rng) {
        double value = 10 + rng.nextGaussian() * 9.0;
        return (int) Math.round(Math.max(0, Math.min(300, value)));
    }

    private WinCondition pickRandomWinCondition(MinecraftServer server, Random rng) {
        WinCondition[] values = WinCondition.values();
        List<WinCondition> candidates = new ArrayList<>();
        int activeTeams = countActiveTeams(server);
        for (WinCondition value : values) {
            if (value == null) continue;
            if (value == WinCondition.RANDOM) continue;
            if (activeTeams <= 1 && (value == WinCondition.LOCKOUT || value == WinCondition.RARITY)) continue;
            candidates.add(value);
        }
        if (candidates.isEmpty()) return WinCondition.LINE;
        return candidates.get(rng.nextInt(candidates.size()));
    }

    private int countActiveTeams(MinecraftServer server) {
        if (server == null) return 0;
        TeamData teamData = TeamData.get(server);
        int count = 0;
        for (TeamData.TeamInfo team : teamData.getTeams()) {
            if (!team.members.isEmpty()) count++;
        }
        return count;
    }

    public void stopGameAfterWin(MinecraftServer server) {
        captureCurrentRunResult(server, true);
        if (server != null) {
            clearAllPlayerInventories(server);
        }
        stopGame();
    }

    private void captureCurrentRunResult(MinecraftServer server, boolean completed) {
        if (currentRunEndCaptured || !active || server == null) return;
        String cardSeed = getLastPlayedSeed();
        if (cardSeed.isBlank()) return;
        if (currentRunPreviewSlotIds.isEmpty() && currentCard != null) {
            cacheCurrentRunPreviewCard();
        }
        currentRunEndCaptured = true;

        int nowTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        long durationSeconds = 0L;
        if (gameStartTick != -1 && nowTick > gameStartTick) {
            durationSeconds = (nowTick - gameStartTick) / 20L;
        }

        String worldSeed = resolveWorldSeed(server);
        long finishedAtEpochSeconds = System.currentTimeMillis() / 1000L;
        int participantCount = Math.max(0, participants.size());
        boolean commandsUsed = currentRunCommandsUsed;
        boolean voteRerollUsed = currentRunVoteRerollUsed;
        int rerollsUsedCount = getTotalRerollsUsedThisRun();
        int fakeRerollsUsedCount = getTotalFakeRerollsUsedThisRun();
        List<String> settingsLines = buildSettingsLines(server);
        if (com.jamie.jamiebingo.mines.MineModeManager.selectedMineName() != null
                && !com.jamie.jamiebingo.mines.MineModeManager.selectedMineName().isBlank()) {
            settingsLines.add("Mine: " + com.jamie.jamiebingo.mines.MineModeManager.selectedMineName());
        }

        Set<UUID> targets = new HashSet<>(participants);
        if (targets.isEmpty()) {
            for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                targets.add(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
            }
        }
        if (targets.isEmpty()) return;

        LeaderboardCategory leaderboardCategory = evaluateLeaderboardCategory(participantCount, rerollsUsedCount, fakeRerollsUsedCount);
        String settingsSeed = SettingsSeedCodec.encode(this, server);
        boolean weeklyChallenge = !activeWeeklyChallengeId.isBlank()
                && activeWeeklyCardSeed.equals(cardSeed == null ? "" : cardSeed)
                && activeWeeklyWorldSeed.equals(worldSeed == null ? "" : worldSeed)
                && activeWeeklySettingsSeed.equals(settingsSeed == null ? "" : settingsSeed);
        String weeklyChallengeId = weeklyChallenge ? activeWeeklyChallengeId : "";
        for (UUID playerId : targets) {
            if (playerId == null) continue;
            TeamData teamData = TeamData.get(server);
            UUID teamId = teamData.getTeamForPlayer(playerId);
            Set<String> completedSlots = resolveCompletedSlotsForHistory(server, playerId, teamId);
            Set<String> opponentCompletedSlots = resolveOpponentCompletedSlotsForHistory(teamId);
            int teamColorId = 10;
            if (teamId != null) {
                TeamData.TeamInfo team = teamData.getTeams().stream()
                        .filter(t -> t != null && teamId.equals(t.id))
                        .findFirst()
                        .orElse(null);
                if (team != null && team.color != null) {
                    teamColorId = team.color.getId();
                }
            }
            List<GameHistoryEntry> entries = playerGameHistory.computeIfAbsent(playerId, id -> new ArrayList<>());
            entries.add(new GameHistoryEntry(
                    cardSeed,
                    worldSeed,
                    settingsSeed,
                    durationSeconds,
                    completed,
                    currentRunPreviewSize,
                    currentRunPreviewSlotIds,
                    new ArrayList<>(completedSlots),
                    new ArrayList<>(opponentCompletedSlots),
                    teamColorId,
                    finishedAtEpochSeconds,
                    com.jamie.jamiebingo.mines.MineModeManager.selectedMineName(),
                    settingsLines,
                    participantCount,
                    commandsUsed,
                    voteRerollUsed,
                    rerollsUsedCount,
                    fakeRerollsUsedCount,
                    weeklyChallenge,
                    weeklyChallengeId,
                    leaderboardCategory.label(),
                    leaderboardCategory.reason()
            ));
            while (entries.size() > MAX_HISTORY_PER_PLAYER) {
                entries.remove(0);
            }
        }

        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    private Set<String> resolveCompletedSlotsForHistory(MinecraftServer server, UUID playerId, UUID teamId) {
        if (teamId == null) return Set.of();

        Set<String> direct = new HashSet<>(getTeamProgressForDisplay(teamId));
        if (!direct.isEmpty()) return direct;

        if (server == null || playerId == null) return direct;
        if (com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server).size() != 1) return direct;

        ensureLegacyProgressMappedToTeam(server, playerId);
        recoverSingleplayerTeamProgress(server, playerId);
        Set<String> recovered = new HashSet<>(getTeamProgressForDisplay(teamId));
        if (!recovered.isEmpty()) return recovered;

        UUID sourceTeam = null;
        int nonEmptyCount = 0;
        for (Map.Entry<UUID, Set<String>> entry : teamProgress.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) continue;
            nonEmptyCount++;
            sourceTeam = entry.getKey();
        }
        if (nonEmptyCount == 1 && sourceTeam != null) {
            return new HashSet<>(teamProgress.getOrDefault(sourceTeam, Set.of()));
        }
        return recovered;
    }

    private Set<String> resolveOpponentCompletedSlotsForHistory(UUID teamId) {
        if (teamId == null) return Set.of();
        if (winCondition != WinCondition.LOCKOUT && winCondition != WinCondition.RARITY) {
            return Set.of();
        }
        Set<String> out = new HashSet<>();
        for (Map.Entry<String, Set<UUID>> entry : slotOwners.entrySet()) {
            String slotId = entry.getKey();
            Set<UUID> owners = entry.getValue();
            if (slotId == null || slotId.isBlank() || owners == null || owners.isEmpty()) continue;
            if (owners.contains(teamId)) continue;
            out.add(slotId);
        }
        return out;
    }

    private String resolveWorldSeed(MinecraftServer server) {
        if (server == null) return "";
        try {
            var worldData = server.getWorldData();
            if (worldData != null && worldData.worldGenOptions() != null) {
                long actualSeed = worldData.worldGenOptions().seed();
                return com.jamie.jamiebingo.world.WorldRegenerationManager.encodeSettingsSeed(actualSeed, this);
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    public void scheduleStopGameWithDelay(MinecraftServer server, int delayTicks) {
        if (server == null) return;
        if (stopGamePending) return;

        int clampedDelay = Math.max(0, delayTicks);
        int clearTick = Math.max(0, clampedDelay - 1);
        int stopTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + clampedDelay;

        stopGamePending = true;
        stopGamePendingTick = stopTick;

        com.jamie.jamiebingo.casino.CasinoTickScheduler.schedule(
                server,
                com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + clearTick,
                () -> clearAllPlayerInventories(server)
        );

        com.jamie.jamiebingo.casino.CasinoTickScheduler.schedule(
                server,
                stopTick,
                () -> {
                    BingoGameData data = BingoGameData.get(server);
                    if (data.stopGamePending && com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) >= data.stopGamePendingTick) {
                        data.stopGamePending = false;
                        data.stopGamePendingTick = -1;
                        data.stopGame();
                    }
                }
        );

        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public void clearAllPlayerInventories(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player).clearContent();
            com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player).setChanged();
            if (player.containerMenu != null) {
                player.containerMenu.setCarried(com.jamie.jamiebingo.util.ItemStackUtil.empty());
                player.containerMenu.broadcastChanges();
            }
        }
    }

    public void beginMatchAfterCasino(MinecraftServer server) {
          LOGGER.info("[JamieBingo] beginMatchAfterCasino: active={} rerollActive={} casinoMode={} startDelay={}s latchedDelay={}s", active, isRerollPhaseActive(), casinoMode, bingoStartDelaySeconds, startDelaySeconds);
          active = true;
        progress.clear();
        teamProgress.clear();
        starterKitGrantedPlayers.clear();
        slotOwners.clear();
        rerollsUsed.clear();
        clearFakeRerollRuntime();
        endRerollPhase();
        initRevealedSlots(server);
        rebuildShuffleQueuesFromCurrentCard(server);

        initializeParticipants(server);
        initializeFullShuffleTeamCards(server);
        initializeShuffleSeenState(server);
        lockQuestsForAllParticipants();

        TeamScoreData.get(server).reset();
TeamScoreData.get(server).ensureTeamsExist(server);
BroadcastHelper.broadcastTeamScores(server);
BroadcastHelper.broadcastFullSync();

        resetPlayerAdvancements(server);
        QuestTracker.resetForGame(com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server));
        QuestEvents.resetForGame(server);
        if (com.jamie.jamiebingo.casino.FakeRerollManager.start(server)) {
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
            return;
        }
        startCountdownOrFinalize(server);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    /* =========================
       START DELAY FLOW
       ========================= */

    public void startCountdownOrFinalize(MinecraftServer server) {
        if (server == null) return;
        if (isFakeRerollPhaseActive()) return;
        int nowTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        if (startCountdownActive && startCountdownEndTick > nowTick) {
            LOGGER.info("[JamieBingo] startCountdownOrFinalize ignored: countdown already active (nowTick={} endTick={})",
                    nowTick, startCountdownEndTick);
            return;
        }
        com.jamie.jamiebingo.mines.MineModeManager.start(server, this);
        com.jamie.jamiebingo.power.PowerSlotManager.start(server, this);

        int delay = Math.max(0, startDelaySeconds);
        LOGGER.info("[JamieBingo] startCountdownOrFinalize: delay={}s (latched) liveDelay={}s nowTick={} active={} pregameBoxActive={}", delay, bingoStartDelaySeconds, nowTick, active, pregameBoxActive);
        if (delay <= 0) {
            finalizeGameStart(server);
            return;
        }

        startCountdownActive = true;
        startCountdownSeconds = delay;
        startCountdownEndTick = nowTick + delay * 20;

        // Hold effects until the real start moment
        randomEffectsEnabled = false;
        randomEffectsNextTick = -1;

        BroadcastHelper.broadcastStartCountdown(server, delay);
        LOGGER.info("[JamieBingo] startCountdown scheduled: endTick={}", startCountdownEndTick);

        com.jamie.jamiebingo.casino.CasinoTickScheduler.schedule(
                server,
                startCountdownEndTick,
                () -> {
                    BingoGameData data = BingoGameData.get(server);
                    if (data.startCountdownActive) {
                        LOGGER.info("[JamieBingo] startCountdown casinoScheduler firing at tick={}", com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server));
                        data.finalizeGameStart(server);
                    }
                }
        );
        // Secondary fail-safe scheduler: do not depend solely on CasinoTickScheduler queue state.
        server.schedule(new net.minecraft.server.TickTask(
                startCountdownEndTick,
                () -> {
                    BingoGameData data = BingoGameData.get(server);
                    if (data.startCountdownActive) {
                        LOGGER.info("[JamieBingo] startCountdown tickTask firing at tick={}", com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server));
                        data.finalizeGameStart(server);
                    }
                }
        ));

        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public void finalizeGameStart(MinecraftServer server) {
        if (server == null) return;
        int nowTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        if (startCountdownActive && startCountdownEndTick > nowTick) {
            LOGGER.info("[JamieBingo] finalizeGameStart ignored: countdown not finished (nowTick={} endTick={})",
                    nowTick, startCountdownEndTick);
            return;
        }
        if (!startCountdownActive && Math.max(0, bingoStartDelaySeconds) > 0) {
            // Safety guard: never bypass configured start delay via direct finalize calls.
            startCountdownOrFinalize(server);
            return;
        }
        LOGGER.info("[JamieBingo] finalizeGameStart begin: tick={} startCountdownActive={} pregameBoxActive={}",
                com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server), startCountdownActive, pregameBoxActive);

        startCountdownActive = false;
        startCountdownSeconds = 0;
        startCountdownEndTick = -1;
        gameStartTick = -1;
        if (countdownEnabled) {
            resumeCountdownRemainingSeconds = Math.max(1, countdownMinutes) * 60;
            resumeElapsedSeconds = -1;
        } else {
            resumeElapsedSeconds = 0;
            resumeCountdownRemainingSeconds = -1;
        }
        timerResumePending = false;
        currentRunEndCaptured = false;
        currentRunCommandsUsed = false;
        cacheCurrentRunPreviewCard();
        countdownExpired = false;
        countdownEndTick = -1;

        clearAllActiveEffects(server);
        resetPlayersForGameStart(server);
        resetPlayerAdvancements(server);

        if (randomEffectsArmed) {
            randomEffectsEnabled = true;
            randomEffectsNextTick = -1;
        }

        com.jamie.jamiebingo.world.PregameBoxManager.releasePlayersToSpawn(server);
        server.schedule(new net.minecraft.server.TickTask(
                com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + 1,
                () -> com.jamie.jamiebingo.world.PregameBoxManager.ensurePlayersReleased(server)
        ));
        server.schedule(new net.minecraft.server.TickTask(
                com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + 20,
                () -> com.jamie.jamiebingo.world.PregameBoxManager.ensurePlayersReleased(server)
        ));
        server.schedule(new net.minecraft.server.TickTask(
                com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + 2,
                () -> {
                    BingoGameData data = BingoGameData.get(server);
                    if (data == null || !data.active || data.startCountdownActive) return;
                    data.grantStarterKits(server);
                }
        ));
        // Arm timer only after post-teleport stabilization tick so it never starts while players are still transitioning.
        gameStartTick = -1;
        countdownEndTick = -1;
        server.schedule(new net.minecraft.server.TickTask(
                com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + 20,
                () -> {
                    BingoGameData data = BingoGameData.get(server);
                    if (data == null || !data.active || data.startCountdownActive) return;
                    if (data.sanitizeImmediateSpawnTies(server)) {
                        BroadcastHelper.broadcastFullSync();
                        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
                    }
                    if (data.gameStartTick != -1) return;
                    int startTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
                    data.gameStartTick = startTick;
                    data.countdownEndTick = data.countdownEnabled
                            ? startTick + data.countdownMinutes * 60 * 20
                            : -1;
                    for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                        BroadcastHelper.syncGameTimer(player);
                    }
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
                }
        ));
        com.jamie.jamiebingo.casino.CasinoTickScheduler.schedule(
                server,
                com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + 1,
                () -> com.jamie.jamiebingo.world.LobbyWorldManager.clearPreparedGameStartSpawn(server, this)
        );

        applyWorldRules(server);

          BroadcastHelper.broadcastStartCountdown(server, 0);
          BroadcastHelper.broadcastSettingsOverlay(server);
          LOGGER.info("[JamieBingo] finalizeGameStart complete: gameStartTick={} countdownEndTick={} pregameBoxActive={}",
                  gameStartTick, countdownEndTick, pregameBoxActive);

          com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
      }

    private void cacheCurrentRunPreviewCard() {
        currentRunPreviewSize = 0;
        currentRunPreviewSlotIds.clear();
        BingoCard card = currentCard;
        if (card == null || card.getSize() <= 0) return;
        currentRunPreviewSize = card.getSize();
        for (int y = 0; y < card.getSize(); y++) {
            for (int x = 0; x < card.getSize(); x++) {
                BingoSlot slot = card.getSlot(x, y);
                currentRunPreviewSlotIds.add(slot == null ? "" : slot.getId());
            }
        }
    }

    private void resetPlayersForGameStart(MinecraftServer server) {
        if (server == null) return;
        resetAllPlayerLevels(server);
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            // Clear stale bed/respawn-anchor spawnpoints from previous games.
            player.setRespawnPosition(null, false);
       GameType targetMode = adventureMode ? GameType.ADVENTURE : GameType.SURVIVAL;
       if (player.gameMode.getGameModeForPlayer() != targetMode) {
           com.jamie.jamiebingo.util.GameModeTracking.runWithoutCommandTracking(player, () -> player.setGameMode(targetMode));
       }
            player.setHealth(player.getMaxHealth());
            var food = com.jamie.jamiebingo.util.PlayerFoodUtil.getFoodData(player);
              if (food != null) {
                  com.jamie.jamiebingo.util.FoodDataUtil.setFoodLevel(food, 20);
                  com.jamie.jamiebingo.util.FoodDataUtil.setSaturation(food, 5.0F);
              }
          }
          if (rtpEnabled) {
              assignRandomRespawns(server);
          }
      }

    private void assignRandomRespawns(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            if (player == null) continue;
            BingoRtpHandler.prepareRtpSpawn(player, server);
        }
    }

    private void clearAllActiveEffects(MinecraftServer server) {
        if (server == null) return;

        if (!appliedRandomEffectId.isEmpty()) {
            var old =
                    net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getHolder(
                            com.jamie.jamiebingo.util.IdUtil.id(appliedRandomEffectId)).orElse(null);
            if (old != null) {
                for (ServerPlayer p : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                    p.removeEffect(old);
                }
            }
            appliedRandomEffectId = "";
        }

        if (!appliedCustomEffectId.isEmpty()) {
            var prev = com.jamie.jamiebingo.addons.effects.CustomEffectRegistry.getById(appliedCustomEffectId);
            if (prev != null) prev.onRemove(server);
            appliedCustomEffectId = "";
        }

        for (ServerPlayer p : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            clearAllEffects(p);
        }
        BroadcastHelper.broadcastCustomEffectState(server, this);
    }

    private static void clearAllEffects(ServerPlayer p) {
        if (p == null) return;
        // Prefer a direct removeAllEffects call if present.
        try {
            java.lang.reflect.Method m = findMethodByNameAndArgCount(p.getClass(), "removeAllEffects", 0);
            if (m != null) {
                m.invoke(p);
                return;
            }
        } catch (Exception ignored) {
        }
        // Fallback: remove each active effect via reflection.
        java.util.Collection<?> effects = com.jamie.jamiebingo.util.LivingEntityEffectUtil.getActiveEffects(p);
        if (effects == null || effects.isEmpty()) return;
        for (Object inst : effects) {
            Object effect = extractEffectHolder(inst);
            if (effect != null) {
                if (invokeRemoveEffect(p, effect)) {
                    continue;
                }
            }
        }
    }

    private static Object extractEffectHolder(Object effectInstance) {
        if (effectInstance == null) return null;
        try {
            java.lang.reflect.Method m = findMethodByNameAndArgCount(effectInstance.getClass(), "getEffect", 0);
            if (m != null) return m.invoke(effectInstance);
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Method m = findMethodByNameAndArgCount(effectInstance.getClass(), "getEffectHolder", 0);
            if (m != null) return m.invoke(effectInstance);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean invokeRemoveEffect(ServerPlayer p, Object effect) {
        if (p == null || effect == null) return false;
        try {
            for (java.lang.reflect.Method m : p.getClass().getMethods()) {
                if (m.getParameterCount() != 1) continue;
                String name = m.getName().toLowerCase(java.util.Locale.ROOT);
                if (!name.contains("remove") || !name.contains("effect")) continue;
                Class<?> param = m.getParameterTypes()[0];
                if (!param.isAssignableFrom(effect.getClass())) continue;
                m.invoke(p, effect);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static java.lang.reflect.Method findMethodByNameAndArgCount(Class<?> type, String name, int argCount) {
        for (java.lang.reflect.Method m : type.getMethods()) {
            if (!m.getName().equals(name)) continue;
            if (m.getParameterCount() == argCount) return m;
        }
        return null;
    }

    private void applyWorldRules(MinecraftServer server) {
        if (server == null) return;

        var overworld = ServerLevelUtil.getOverworld(server);
        if (overworld == null) return;

        net.minecraft.world.level.gamerules.GameRules rules = GameRulesUtil.getGameRules(overworld);
        if (rules == null) return;
        rules.set(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY, keepInventoryEnabled, server);
        GameRulesUtil.setBooleanByName(
                rules,
                server,
                naturalRegenEnabled,
                "natural_health_regeneration",
                "naturalRegeneration",
                "natural_regeneration",
                "doNaturalRegeneration",
                "naturalHealthRegeneration"
        );
        try {
            var source = server.createCommandSourceStack().withSuppressedOutput();
            server.getCommands().performPrefixedCommand(
                    source,
                    "gamerule natural_health_regeneration " + (naturalRegenEnabled ? "true" : "false")
            );
        } catch (Throwable ignored) {
        }

        if (daylightMode == DAYLIGHT_ENABLED) {
            rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME, true, server);
            overworld.setDayTime(1000L);
        } else {
            rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME, false, server);
            long time = switch (daylightMode) {
                case DAYLIGHT_DAY -> 1000L;
                case DAYLIGHT_NIGHT -> 13000L;
                case DAYLIGHT_MIDNIGHT -> 18000L;
                case DAYLIGHT_DAWN -> 23000L;
                case DAYLIGHT_DUSK -> 12000L;
                default -> 1000L;
            };
            overworld.setDayTime(time);
        }

        rules.set(net.minecraft.world.level.gamerules.GameRules.PVP, pvpEnabled, server);
    }

    private int normalizeStarterKitMode(int mode) {
        return Math.max(STARTER_KIT_DISABLED, Math.min(STARTER_KIT_OP, mode));
    }

    private Set<String> getStarterKitBlockedSlotIds() {
        Set<String> blocked = new HashSet<>();
        int mode = normalizeStarterKitMode(starterKitMode);
        if (mode <= STARTER_KIT_DISABLED) return blocked;

        // Any starter kit can accelerate early advancement flow from spawn setup/inventory progression.
        blocked.add("quest.opponent_obtains_advancement");

        blocked.add("minecraft:stone_axe");
        blocked.add("minecraft:stone_pickaxe");
        if (mode >= STARTER_KIT_AVERAGE) {
            blocked.add("minecraft:stone_shovel");
            blocked.add("minecraft:leather_boots");
            blocked.add("quest.opponent_wears_armor");
            blocked.add("quest.whole_team_wear_a_piece_of_enchanted_armor");
            blocked.add("quest.wear_black_colored_leather_boots");
            blocked.add("quest.wear_blue_colored_leather_boots");
            blocked.add("quest.wear_brown_colored_leather_boots");
            blocked.add("quest.wear_cyan_colored_leather_boots");
            blocked.add("quest.wear_gray_colored_leather_boots");
            blocked.add("quest.wear_green_colored_leather_boots");
            blocked.add("quest.wear_light_blue_colored_leather_boots");
            blocked.add("quest.wear_light_gray_colored_leather_boots");
            blocked.add("quest.wear_lime_colored_leather_boots");
            blocked.add("quest.wear_magenta_colored_leather_boots");
            blocked.add("quest.wear_orange_colored_leather_boots");
            blocked.add("quest.wear_pink_colored_leather_boots");
            blocked.add("quest.wear_purple_colored_leather_boots");
            blocked.add("quest.wear_red_colored_leather_boots");
            blocked.add("quest.wear_white_colored_leather_boots");
            blocked.add("quest.wear_yellow_colored_leather_boots");
            if (hungerEnabled) {
                blocked.add("minecraft:bread");
            }
        }
        if (mode >= STARTER_KIT_OP) {
            blocked.add("minecraft:iron_pickaxe");
            blocked.add("minecraft:iron_axe");
            blocked.add("minecraft:iron_shovel");
            blocked.add("quest.whole_team_has_a_status_effect_at_the_same_time");
            blocked.add("quest.remove_a_status_effect_with_a_milk_bucket");
        }
        return blocked;
    }

    private Set<String> buildGenerationBlacklist() {
        Set<String> blocked = new HashSet<>(blacklistedSlotIds);
        blocked.addAll(getStarterKitBlockedSlotIds());
        return blocked;
    }

    public Set<String> getGenerationBlacklistForPreview() {
        return new HashSet<>(buildGenerationBlacklist());
    }

    private void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return;
        boolean added = player.getInventory().add(stack);
        if (!added) {
            player.drop(stack, false);
        }
    }

    private ItemStack asStarterKitItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return stack;
        com.jamie.jamiebingo.world.StarterKitPersistenceManager.markStarterKitItem(stack);
        return stack;
    }

    private void enchantItem(ServerPlayer player, ItemStack stack, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantment, int level) {
        if (player == null || stack == null || stack.isEmpty() || enchantment == null || level <= 0) return;
        try {
            Holder<net.minecraft.world.item.enchantment.Enchantment> holder = player.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(enchantment);
            stack.enchant(holder, level);
        } catch (Throwable ignored) {
        }
    }

    private void giveStarterKitToPlayer(ServerPlayer player) {
        if (player == null) return;
        int mode = normalizeStarterKitMode(starterKitMode);
        if (mode <= STARTER_KIT_DISABLED) return;
        switch (mode) {
            case STARTER_KIT_MINIMAL -> {
                giveOrDrop(player, asStarterKitItem(new ItemStack(Items.STONE_AXE)));
                giveOrDrop(player, asStarterKitItem(new ItemStack(Items.STONE_PICKAXE)));
            }
            case STARTER_KIT_AVERAGE -> {
                giveOrDrop(player, asStarterKitItem(new ItemStack(Items.STONE_AXE)));
                giveOrDrop(player, asStarterKitItem(new ItemStack(Items.STONE_PICKAXE)));
                giveOrDrop(player, asStarterKitItem(new ItemStack(Items.STONE_SHOVEL)));
                if (hungerEnabled) {
                    giveOrDrop(player, asStarterKitItem(new ItemStack(Items.BREAD, 5)));
                }
                ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
                enchantItem(player, boots, Enchantments.DEPTH_STRIDER, 3);
                asStarterKitItem(boots);
                player.setItemSlot(EquipmentSlot.FEET, boots);
            }
            case STARTER_KIT_OP -> {
                giveOrDrop(player, asStarterKitItem(new ItemStack(Items.IRON_PICKAXE)));
                giveOrDrop(player, asStarterKitItem(new ItemStack(Items.IRON_AXE)));
                giveOrDrop(player, asStarterKitItem(new ItemStack(Items.IRON_SHOVEL)));
                if (hungerEnabled) {
                    giveOrDrop(player, asStarterKitItem(new ItemStack(Items.BREAD, 20)));
                }
                ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
                enchantItem(player, boots, Enchantments.DEPTH_STRIDER, 3);
                asStarterKitItem(boots);
                player.setItemSlot(EquipmentSlot.FEET, boots);
                ItemStack silkPick = new ItemStack(Items.IRON_PICKAXE);
                enchantItem(player, silkPick, Enchantments.SILK_TOUCH, 1);
                giveOrDrop(player, asStarterKitItem(silkPick));
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false, true));
            }
            default -> {
            }
        }
    }

    private void grantStarterKits(MinecraftServer server) {
        if (server == null || normalizeStarterKitMode(starterKitMode) <= STARTER_KIT_DISABLED) return;
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            if (player == null) continue;
            UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
            if (!isParticipant(playerId)) continue;
            if (!starterKitGrantedPlayers.add(playerId)) continue;
            giveStarterKitToPlayer(player);
        }
    }

    public BingoCard buildCustomCardFromSlots() {
        ensureCustomCardSlotsSize(100);
        Set<String> starterBlocked = getStarterKitBlockedSlotIds();
        List<BingoSlot> slots = new ArrayList<>();
        for (String id : customCardSlots) {
            if (id == null || id.isBlank()) continue;
            if (isSlotBlocked(id)) continue;
            if (starterBlocked.contains(id)) continue;
            BingoSlot slot = com.jamie.jamiebingo.bingo.SlotResolver.resolveSlot(id);
            if (slot != null) {
                slots.add(slot);
            }
        }
        if (slots.isEmpty()) return null;

        int count = slots.size();
        int size = (int) Math.ceil(Math.sqrt(count));
        size = Math.max(1, Math.min(10, size));

        BingoCard card = new BingoCard(size);
        int idx = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (idx >= slots.size()) break;
                card.setSlot(x, y, slots.get(idx++));
            }
        }
        return card;
    }

    public BingoCard buildCardFromPool(int size, Random rng) {
        if (rng == null) rng = new Random();
        Set<String> starterBlocked = getStarterKitBlockedSlotIds();
        List<BingoSlot> pool = new ArrayList<>();
        for (String id : customPoolIds) {
            if (id == null || id.isBlank()) continue;
            if (isSlotBlocked(id)) continue;
            if (starterBlocked.contains(id)) continue;
            BingoSlot slot = com.jamie.jamiebingo.bingo.SlotResolver.resolveSlot(id);
            if (slot != null) pool.add(slot);
        }
        if (pool.isEmpty()) return null;

        int finalSize = Math.max(1, Math.min(10, size));
        int total = finalSize * finalSize;
        BingoCard card = new BingoCard(finalSize);

        if (pool.size() >= total) {
            List<BingoSlot> bag = new ArrayList<>(pool);
            for (int y = 0; y < finalSize; y++) {
                for (int x = 0; x < finalSize; x++) {
                    int pick = rng.nextInt(bag.size());
                    card.setSlot(x, y, bag.remove(pick));
                }
            }
        } else {
            for (int y = 0; y < finalSize; y++) {
                for (int x = 0; x < finalSize; x++) {
                    BingoSlot slot = pool.get(rng.nextInt(pool.size()));
                    card.setSlot(x, y, slot);
                }
            }
        }

        return card;
    }

    public int getTimerSeconds(MinecraftServer server) {
        if (server == null) return 0;
        if (timerResumePending) {
            restoreTimerBaselineIfNeeded(server);
        }
        if (gameStartTick == -1) return 0;

        int currentTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        if (countdownEnabled) {
            if (countdownEndTick < 0) {
                countdownEndTick = gameStartTick + countdownMinutes * 60 * 20;
            }
            return Math.max(0, (countdownEndTick - currentTick) / 20);
        }
        return Math.max(0, (currentTick - gameStartTick) / 20);
    }

    public void restoreTimerBaselineIfNeeded(MinecraftServer server) {
        if (server == null || !active || startCountdownActive) return;
        boolean preMatchHold = gameStartTick < 0
                && (pregameBoxActive
                || isRerollPhaseActive()
                || com.jamie.jamiebingo.casino.CasinoModeManager.isCasinoInProgress());
        if (preMatchHold) return;

        int currentTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        boolean changed = false;
        boolean staleAbsoluteBaseline = gameStartTick > currentTick + 20;
        if (!timerResumePending) {
            if (gameStartTick == -1) {
                gameStartTick = currentTick;
                if (countdownEnabled && countdownEndTick < 0) {
                    countdownEndTick = gameStartTick + Math.max(1, countdownMinutes) * 60 * 20;
                }
                changed = true;
            } else if (staleAbsoluteBaseline) {
                if (countdownEnabled) {
                    int total = Math.max(1, countdownMinutes) * 60;
                    int remaining = resumeCountdownRemainingSeconds >= 0
                            ? Math.max(0, Math.min(total, resumeCountdownRemainingSeconds))
                            : total;
                    gameStartTick = currentTick - Math.max(0, total - remaining) * 20;
                    countdownEndTick = currentTick + remaining * 20;
                    resumeCountdownRemainingSeconds = -1;
                    resumeElapsedSeconds = -1;
                } else {
                    int elapsed = resumeElapsedSeconds >= 0 ? Math.max(0, resumeElapsedSeconds) : 0;
                    int restoredStartTick = currentTick - elapsed * 20;
                    if (restoredStartTick == -1) restoredStartTick = -2;
                    gameStartTick = restoredStartTick;
                    resumeElapsedSeconds = -1;
                    resumeCountdownRemainingSeconds = -1;
                }
                changed = true;
            }
            if (changed) {
                com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
            }
            return;
        }

        if (countdownEnabled) {
            if (resumeCountdownRemainingSeconds >= 0) {
                int remaining = Math.max(0, resumeCountdownRemainingSeconds);
                countdownEndTick = currentTick + remaining * 20;
                int total = Math.max(1, countdownMinutes) * 60;
                gameStartTick = currentTick - Math.max(0, total - remaining) * 20;
                resumeCountdownRemainingSeconds = -1;
                changed = true;
            } else if (gameStartTick == -1) {
                gameStartTick = currentTick;
                if (countdownEndTick < 0) {
                    countdownEndTick = gameStartTick + Math.max(1, countdownMinutes) * 60 * 20;
                }
                changed = true;
            }
        } else {
            if (resumeElapsedSeconds >= 0) {
                int elapsed = Math.max(0, resumeElapsedSeconds);
                int restoredStartTick = currentTick - elapsed * 20;
                if (restoredStartTick == -1) {
                    restoredStartTick = -2;
                }
                gameStartTick = restoredStartTick;
                resumeElapsedSeconds = -1;
                changed = true;
            } else if (gameStartTick == -1) {
                gameStartTick = currentTick;
                changed = true;
            }
        }

        if (!changed && staleAbsoluteBaseline) {
            if (countdownEnabled) {
                int total = Math.max(1, countdownMinutes) * 60;
                int remaining = resumeCountdownRemainingSeconds >= 0
                        ? Math.max(0, Math.min(total, resumeCountdownRemainingSeconds))
                        : total;
                gameStartTick = currentTick - Math.max(0, total - remaining) * 20;
                countdownEndTick = currentTick + remaining * 20;
                resumeCountdownRemainingSeconds = -1;
                resumeElapsedSeconds = -1;
            } else {
                int elapsed = resumeElapsedSeconds >= 0 ? Math.max(0, resumeElapsedSeconds) : 0;
                int restoredStartTick = currentTick - elapsed * 20;
                if (restoredStartTick == -1) restoredStartTick = -2;
                gameStartTick = restoredStartTick;
                resumeElapsedSeconds = -1;
                resumeCountdownRemainingSeconds = -1;
            }
            changed = true;
        }

        if (changed) {
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        }
        timerResumePending = false;
    }

    public void updateResumeTimerSnapshot(boolean countdownMode, int seconds) {
        int safe = Math.max(0, seconds);
        if (countdownMode) {
            if (resumeCountdownRemainingSeconds != safe || resumeElapsedSeconds != -1) {
                resumeCountdownRemainingSeconds = safe;
                resumeElapsedSeconds = -1;
                com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
            }
        } else {
            if (resumeElapsedSeconds != safe || resumeCountdownRemainingSeconds != -1) {
                resumeElapsedSeconds = safe;
                resumeCountdownRemainingSeconds = -1;
                com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
            }
        }
    }

    public void ensureLegacyProgressMappedToTeam(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return;
        if (progress.isEmpty()) return;
        if (!teamProgress.isEmpty()) return;
        UUID teamId = TeamData.get(server).getTeamForPlayer(playerId);
        if (teamId == null) return;
        teamProgress.put(teamId, new HashSet<>(progress));
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public void recoverSingleplayerTeamProgress(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return;
        if (com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server).size() != 1) return;

        TeamData teamData = TeamData.get(server);
        UUID currentTeam = teamData.getTeamForPlayer(playerId);
        if (currentTeam == null) return;

        Set<String> current = teamProgress.get(currentTeam);
        if (current != null && !current.isEmpty()) return;

        UUID sourceTeam = null;
        int bestSize = 0;
        for (Map.Entry<UUID, Set<String>> entry : teamProgress.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) continue;
            int size = entry.getValue().size();
            if (size > bestSize) {
                bestSize = size;
                sourceTeam = entry.getKey();
            }
        }
        if (sourceTeam == null || sourceTeam.equals(currentTeam)) return;

        Set<String> copied = new HashSet<>(teamProgress.getOrDefault(sourceTeam, Set.of()));
        teamProgress.put(currentTeam, copied);
        for (Set<UUID> owners : slotOwners.values()) {
            if (owners == null) continue;
            if (owners.remove(sourceTeam)) {
                owners.add(currentTeam);
            }
        }
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    public void recoverSingleplayerRushState(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return;
        if (!active || !rushEnabled) return;
        if (com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server).size() != 1) return;

        TeamData teamData = TeamData.get(server);
        UUID currentTeam = teamData.getTeamForPlayer(playerId);
        if (currentTeam == null) return;

        int now = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
        Integer currentDeadline = rushDeadlineTickByTeam.get(currentTeam);
        Integer currentResume = rushResumeSecondsByTeam.get(currentTeam);
        boolean currentHasLiveDeadline = currentDeadline != null && currentDeadline > now;
        boolean currentHasLiveResume = currentResume != null && currentResume > 0;
        if (currentHasLiveDeadline || currentHasLiveResume) {
            return;
        }

        UUID sourceTeam = null;
        int bestRemaining = Integer.MIN_VALUE;
        for (Map.Entry<UUID, Integer> entry : rushDeadlineTickByTeam.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            int remaining = entry.getValue() - com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);
            if (remaining < 0) continue;
            if (remaining > bestRemaining) {
                bestRemaining = remaining;
                sourceTeam = entry.getKey();
            }
        }
        if (sourceTeam == null) {
            for (Map.Entry<UUID, Integer> entry : rushResumeSecondsByTeam.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                int remaining = entry.getValue();
                if (remaining < 0) continue;
                if (remaining > bestRemaining) {
                    bestRemaining = remaining;
                    sourceTeam = entry.getKey();
                }
            }
        }
        if (sourceTeam == null || sourceTeam.equals(currentTeam)) return;

        Integer deadline = rushDeadlineTickByTeam.remove(sourceTeam);
        Integer remaining = rushResumeSecondsByTeam.remove(sourceTeam);
        if (deadline != null) rushDeadlineTickByTeam.put(currentTeam, deadline);
        if (remaining != null) rushResumeSecondsByTeam.put(currentTeam, remaining);
        if (eliminatedTeams.remove(sourceTeam)) {
            eliminatedTeams.add(currentTeam);
        }
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    private void resetPlayerAdvancements(MinecraftServer server) {
        if (server == null) return;
        resetAllPlayerLevels(server);
        if (!server.getAdvancements().getAllAdvancements().iterator().hasNext()) return;

        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            net.minecraft.server.PlayerAdvancements advancements = player.getAdvancements();
            for (net.minecraft.advancements.AdvancementHolder adv : server.getAdvancements().getAllAdvancements()) {
                net.minecraft.advancements.AdvancementProgress progress = advancements.getOrStartProgress(adv);
                for (String criterion : progress.getCompletedCriteria()) {
                    advancements.revoke(adv, criterion);
                }
            }
        }
    }

    public void resetAllPlayerLevels(MinecraftServer server) {
        if (server == null) return;
        java.util.List<UUID> ids = new java.util.ArrayList<>();
        for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
            ids.add(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
            com.jamie.jamiebingo.util.PlayerExperienceUtil.resetExperience(player);
        }
        // Some launcher/server flows can overwrite XP shortly after a reset.
        com.jamie.jamiebingo.casino.CasinoTickScheduler.schedule(
                server,
                com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + 2,
                () -> {
                    for (UUID id : ids) {
                        ServerPlayer player = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, id);
                        if (player != null) {
                            com.jamie.jamiebingo.util.PlayerExperienceUtil.resetExperience(player);
                        }
                    }
                }
        );
    }

    public boolean isGameStartSpawnReady() {
        return gameStartSpawnPrepared && !gameStartSpawnLoading;
    }

    private String buildGameStartMessage(MinecraftServer server) {
        StringBuilder sb = new StringBuilder("[Bingo] Game started: ");

        sb.append("Mode: ").append(winCondition);
        sb.append(" | Card Size: ").append(size);

        if (composition == CardComposition.HYBRID_CATEGORY) {
            sb.append(" | Quests: Category");
        } else if (composition == CardComposition.HYBRID_PERCENT) {
            sb.append(" | Quests: ").append(questPercent).append("%");
        } else {
            sb.append(" | Quests: Disabled");
        }
        sb.append(" | Category Logic: ").append(categoryLogicEnabled ? "Enabled" : "Disabled");
        sb.append(" | Rarity Logic: ").append(rarityLogicEnabled ? "Enabled" : "Disabled");
        sb.append(" | Item Color Variants: ").append(itemColorVariantsSeparate ? "Separate" : "Grouped");

        if (winCondition == WinCondition.GUNGAME || winCondition == WinCondition.GAMEGUN) {
            sb.append(" | Gun Rounds: ").append(gunGameLength);
        }
        if (winCondition == WinCondition.HANGMAN) {
            sb.append(" | Hangman Rounds: ").append(hangmanRounds);
            sb.append(" | Hangman Time: ").append(hangmanBaseSeconds).append("s");
            sb.append(" | Hangman Penalty: ").append(hangmanPenaltySeconds).append("s");
        }

        sb.append(" | Casino: ").append(casinoModeLabel());
        sb.append(" | Rerolls: ")
          .append(rerollsPerPlayer > 0 ? rerollsPerPlayer : "Disabled");
        sb.append(" | Mines: ")
          .append(minesEnabled ? ("Enabled (" + mineAmount + ", " + mineTimeSeconds + "s)") : "Disabled");

        if (randomEffectsIntervalSeconds > 0) {
            sb.append(" | Effects: ").append(randomEffectsIntervalSeconds).append("s");
        } else {
            sb.append(" | Effects: Disabled");
        }

        sb.append(" | RTP: ").append(rtpEnabled ? "Enabled" : "Disabled");
        sb.append(" | PVP: ").append(pvpEnabled ? "Enabled" : "Disabled");
        sb.append(" | Adventure: ").append(adventureMode ? "Enabled" : "Disabled");
        sb.append(" | Prelit Portals: ").append(prelitPortalsLabel());
        sb.append(" | Hostile Mobs: ").append(hostileMobsEnabled ? "Enabled" : "Disabled");
        sb.append(" | Hunger: ").append(hungerEnabled ? "Enabled" : "Disabled");
        sb.append(" | Natural Regen: ").append(naturalRegenEnabled ? "On" : "Off");
        sb.append(" | Keep Inventory: ").append(keepInventoryEnabled ? "Enabled" : "Disabled");
        sb.append(" | Hardcore: ").append(hardcoreEnabled ? "Enabled" : "Disabled");
        sb.append(" | Daylight: ").append(daylightLabel());
        sb.append(" | Register: ").append(registerLabel());
        sb.append(" | Late Join: ").append(allowLateJoin ? "Enabled" : "Disabled");
        sb.append(" | Rush: ").append(rushEnabled ? (rushSeconds + "s") : "Disabled");
        sb.append(" | Start Delay: ").append(bingoStartDelaySeconds > 0
                ? (bingoStartDelaySeconds + "s")
                : "Disabled");
        sb.append(" | Countdown: ").append(countdownEnabled
                ? (countdownMinutes + "m")
                : "Disabled");

        if (difficulty != null && !difficulty.isBlank()) {
            sb.append(" | Card Difficulty: ").append(difficulty);
        } else {
            sb.append(" | Card Difficulty: normal");
        }

        if (server != null) {
            sb.append(" | Game Difficulty: ")
              .append(com.jamie.jamiebingo.util.ServerWorldDataUtil.getDifficultyKey(server));
        }

        return sb.toString();
    }

    public List<String> buildSettingsLines(MinecraftServer server) {
        List<String> lines = new ArrayList<>();

        lines.add("Mode: " + winCondition);
        lines.add("Card Size: " + size);

        if (composition == CardComposition.HYBRID_CATEGORY) {
            lines.add("Quests: Category");
        } else if (composition == CardComposition.HYBRID_PERCENT) {
            lines.add("Quests: " + questPercent + "%");
        } else {
            lines.add("Quests: Disabled");
        }
        lines.add("Category Logic: " + (categoryLogicEnabled ? "Enabled" : "Disabled"));
        lines.add("Rarity Logic: " + (rarityLogicEnabled ? "Enabled" : "Disabled"));
        lines.add("Item Color Variants: " + (itemColorVariantsSeparate ? "Separate" : "Grouped"));

        if (winCondition == WinCondition.GUNGAME || winCondition == WinCondition.GAMEGUN) {
            lines.add("Gun Rounds: " + gunGameLength);
        }
        if (winCondition == WinCondition.HANGMAN) {
            lines.add("Hangman Rounds: " + hangmanRounds);
            lines.add("Hangman Time: " + hangmanBaseSeconds + "s");
            lines.add("Hangman Penalty: " + hangmanPenaltySeconds + "s");
        }

        lines.add("Casino: " + casinoModeLabel());
        lines.add("Rerolls: " + (rerollsPerPlayer > 0 ? rerollsPerPlayer : "Disabled"));
        lines.add("Fake Rerolls: " + (fakeRerollsEnabled ? fakeRerollsPerPlayer : "Disabled"));
        lines.add("Shuffle: " + (shuffleEnabled ? "Enabled" : "Disabled"));
        lines.add("Mines: " + (minesEnabled ? "Enabled" : "Disabled"));
        if (minesEnabled) {
            lines.add("Mine Amount: " + mineAmount);
            lines.add("Mine Time: " + mineTimeSeconds + "s");
        }
        lines.add("Power Slot: " + (powerSlotEnabled ? "Enabled" : "Disabled"));
        if (powerSlotEnabled) {
            lines.add("Power Slot Interval: " + powerSlotIntervalSeconds + "s");
        }

        lines.add("Effects: " + (randomEffectsIntervalSeconds > 0
                ? (randomEffectsIntervalSeconds + "s")
                : "Disabled"));
        lines.add("RTP: " + (rtpEnabled ? "Enabled" : "Disabled"));
        lines.add("PVP: " + (pvpEnabled ? "Enabled" : "Disabled"));
        lines.add("Adventure: " + (adventureMode ? "Enabled" : "Disabled"));
        lines.add("Prelit Portals: " + prelitPortalsLabel());
        lines.add("Hostile Mobs: " + (hostileMobsEnabled ? "Enabled" : "Disabled"));
        lines.add("Hunger: " + (hungerEnabled ? "Enabled" : "Disabled"));
        lines.add("Natural Regen: " + (naturalRegenEnabled ? "On" : "Off"));
        lines.add("Keep Inventory: " + (keepInventoryEnabled ? "Enabled" : "Disabled"));
        lines.add("Hardcore: " + (hardcoreEnabled ? "Enabled" : "Disabled"));
        lines.add("Daylight: " + daylightLabel());
        lines.add("Register: " + registerLabel());
        lines.add("Team Sync: " + (teamSyncEnabled ? "Enabled" : "Disabled"));
        lines.add("Team Chest: " + (teamChestEnabled ? "Enabled" : "Disabled"));
        lines.add("Starter Kit: " + switch (normalizeStarterKitMode(starterKitMode)) {
            case STARTER_KIT_MINIMAL -> "Minimal";
            case STARTER_KIT_AVERAGE -> "Average";
            case STARTER_KIT_OP -> "OP";
            default -> "Disabled";
        });
        lines.add("Hide Goal Details: " + (hideGoalDetailsInChat ? "Enabled" : "Disabled"));
        lines.add("Late Join: " + (allowLateJoin ? "Enabled" : "Disabled"));
        lines.add("Rush: " + (rushEnabled ? (rushSeconds + "s") : "Disabled"));
        lines.add("Start Delay: " + (bingoStartDelaySeconds > 0
                ? (bingoStartDelaySeconds + "s")
                : "Disabled"));
        lines.add("Countdown: " + (countdownEnabled
                ? (countdownMinutes + "m")
                : "Disabled"));

        if (customPoolEnabled) {
            lines.add("Custom Pool: Enabled (" + customPoolIds.size() + ")");
        } else if (customCardEnabled) {
            lines.add("Custom Card: Enabled");
        }
        lines.add("New Seed Every Game: " + (worldUseNewSeedEachGame ? "Enabled" : "Disabled"));
        lines.add("World Type: " + switch (worldTypeMode) {
            case WORLD_TYPE_AMPLIFIED -> "Amplified";
            case WORLD_TYPE_SUPERFLAT -> "Superflat";
            case WORLD_TYPE_CUSTOM_BIOME_SIZE -> "Custom Biome Size";
            case WORLD_TYPE_SINGLE_BIOME -> "Single Biome";
            default -> "Normal";
        });
        lines.add("World Structure Frequency: " + worldStructureFrequencyPercent + "%");
        lines.add("World Surface Cave Biomes: " + (worldSurfaceCaveBiomes ? "Enabled" : "Disabled"));
        if (worldTypeMode == WORLD_TYPE_CUSTOM_BIOME_SIZE) {
            int biomeSize = clampWorldBiomeSize(worldCustomBiomeSizeBlocks);
            double t = (biomeSize - 40) / 60.0D;
            t = Math.max(0.0D, Math.min(1.0D, t));
            int pct = 1 + (int) Math.round(99.0D * t);
            lines.add("World Biome Size: " + pct + "%");
        }
        if (worldTypeMode == WORLD_TYPE_SINGLE_BIOME) {
            lines.add("World Single Biome: " + (worldSingleBiomeId == null || worldSingleBiomeId.isBlank() ? "minecraft:plains" : worldSingleBiomeId));
        }
        if (worldTypeMode == WORLD_TYPE_NORMAL || worldTypeMode == WORLD_TYPE_AMPLIFIED || worldTypeMode == WORLD_TYPE_CUSTOM_BIOME_SIZE) {
            lines.add("World Hilliness: " + (worldTerrainHillinessPercent == 50 ? "Default" : (worldTerrainHillinessPercent + "%")));
        }

        lines.add("Card Difficulty: " +
                (difficulty != null && !difficulty.isBlank() ? difficulty : "normal"));

        if (server != null) {
            lines.add("Game Difficulty: " +
                    com.jamie.jamiebingo.util.ServerWorldDataUtil.getDifficultyKey(server));
        }

        return lines;
    }

    private LeaderboardCategory evaluateLeaderboardCategory(int participantCount, int rerollsUsedCount, int fakeRerollsUsedCount) {
        BingoGameData defaults = new BingoGameData();

        if (!blacklistedSlotIds.isEmpty()) {
            return new LeaderboardCategory("Custom", "Blacklist preset active");
        }
        if (!whitelistedSlotIds.isEmpty()) {
            return new LeaderboardCategory("Custom", "Whitelist preset active");
        }
        if (!rarityOverrides.isEmpty()) {
            return new LeaderboardCategory("Custom", "Rarity changer preset active");
        }
        if (customPoolEnabled || customCardEnabled) {
            return new LeaderboardCategory("Custom", "Custom card usage is active");
        }
        if (!worldUseNewSeedEachGame) {
            return new LeaderboardCategory("Custom", "World seed mode is not new seed every game");
        }
        if (worldTypeMode != WORLD_TYPE_NORMAL) {
            return new LeaderboardCategory("Custom", "World type is not normal");
        }
        if (worldStructureFrequencyPercent != 100) {
            return new LeaderboardCategory("Custom", "World structure frequency is not default");
        }
        if (worldSurfaceCaveBiomes) {
            return new LeaderboardCategory("Custom", "Surface cave biomes are enabled");
        }
        if (clampPrelitPortalsMode(prelitPortalsMode) != PRELIT_PORTALS_OFF) {
            return new LeaderboardCategory("Custom", "Prelit portals are enabled");
        }
        if (worldSetSeedText != null && !worldSetSeedText.isBlank()) {
            return new LeaderboardCategory("Custom", "World is generated from a set seed");
        }
        if (participantCount != 1) {
            return new LeaderboardCategory("Custom", "Run was not singleplayer");
        }
        if (rerollsUsedCount > 0 || fakeRerollsUsedCount > 0) {
            return new LeaderboardCategory("Custom", "Rerolls were used");
        }

        if (mode != defaults.mode) return new LeaderboardCategory("Custom", "Mode setting is not default");
        if (composition != defaults.composition) return new LeaderboardCategory("Custom", "Quest composition is not default");
        if (winCondition != defaults.winCondition) return new LeaderboardCategory("Custom", "Win condition is not default");
        if (size != defaults.size) return new LeaderboardCategory("Custom", "Card size is not default");
        if (!Objects.equals(difficulty, defaults.difficulty)) return new LeaderboardCategory("Custom", "Card difficulty is not default");
        if (questPercent != defaults.questPercent) return new LeaderboardCategory("Custom", "Quest percentage is not default");
        if (categoryLogicEnabled != defaults.categoryLogicEnabled) return new LeaderboardCategory("Custom", "Category logic is not default");
        if (rarityLogicEnabled != defaults.rarityLogicEnabled) return new LeaderboardCategory("Custom", "Rarity logic is not default");
        if (itemColorVariantsSeparate != defaults.itemColorVariantsSeparate) return new LeaderboardCategory("Custom", "Item colour mode is not default");
        if (rerollsPerPlayer != defaults.rerollsPerPlayer) return new LeaderboardCategory("Custom", "Rerolls setting is not default");
        if (casinoMode != defaults.casinoMode) return new LeaderboardCategory("Custom", "Casino setting is not default");
        if (minesEnabled != defaults.minesEnabled) return new LeaderboardCategory("Custom", "Mines setting is not default");
        if (mineAmount != defaults.mineAmount) return new LeaderboardCategory("Custom", "Mine amount is not default");
        if (mineTimeSeconds != defaults.mineTimeSeconds) return new LeaderboardCategory("Custom", "Mine time is not default");
        if (powerSlotEnabled != defaults.powerSlotEnabled) return new LeaderboardCategory("Custom", "Power slot is not default");
        if (powerSlotIntervalSeconds != defaults.powerSlotIntervalSeconds) return new LeaderboardCategory("Custom", "Power slot interval is not default");
        if (fakeRerollsEnabled != defaults.fakeRerollsEnabled) return new LeaderboardCategory("Custom", "Fake rerolls are enabled");
        if (fakeRerollsPerPlayer != defaults.fakeRerollsPerPlayer) return new LeaderboardCategory("Custom", "Fake reroll count is not default");
        if (hideGoalDetailsInChat != defaults.hideGoalDetailsInChat) return new LeaderboardCategory("Custom", "Hide goal details is not default");
        if (starterKitMode != defaults.starterKitMode) return new LeaderboardCategory("Custom", "Starter kit is not default");
        if (rtpEnabled != defaults.rtpEnabled) return new LeaderboardCategory("Custom", "RTP is not default");
        if (hungerEnabled != defaults.hungerEnabled) return new LeaderboardCategory("Custom", "Hunger is not default");
        if (naturalRegenEnabled != defaults.naturalRegenEnabled) return new LeaderboardCategory("Custom", "Natural regen is not default");
        if (hostileMobsEnabled != defaults.hostileMobsEnabled) return new LeaderboardCategory("Custom", "Hostile mobs is not default");
        if (pvpEnabled != defaults.pvpEnabled) return new LeaderboardCategory("Custom", "PVP is not default");
        if (adventureMode != defaults.adventureMode) return new LeaderboardCategory("Custom", "Adventure mode is not default");
        if (daylightMode != defaults.daylightMode) return new LeaderboardCategory("Custom", "Daylight is not default");
        if (allowLateJoin != defaults.allowLateJoin) return new LeaderboardCategory("Custom", "Late join is not default");
        if (teamChestEnabled != defaults.teamChestEnabled) return new LeaderboardCategory("Custom", "Team chest is not default");
        if (randomEffectsEnabled != defaults.randomEffectsEnabled) return new LeaderboardCategory("Custom", "Random effects are enabled");
        if (randomEffectsIntervalSeconds != defaults.randomEffectsIntervalSeconds) return new LeaderboardCategory("Custom", "Effects interval is not default");
        if (keepInventoryEnabled != defaults.keepInventoryEnabled) return new LeaderboardCategory("Custom", "Keep inventory is not default");
        if (hardcoreEnabled != defaults.hardcoreEnabled) return new LeaderboardCategory("Custom", "Hardcore is not default");
        if (registerMode != defaults.registerMode) return new LeaderboardCategory("Custom", "Register mode is not default");
        if (teamSyncEnabled != defaults.teamSyncEnabled) return new LeaderboardCategory("Custom", "Team sync is not default");
        if (rushEnabled != defaults.rushEnabled) return new LeaderboardCategory("Custom", "Rush is not default");
        if (rushSeconds != defaults.rushSeconds) return new LeaderboardCategory("Custom", "Rush time is not default");
        if (bingoStartDelaySeconds != defaults.bingoStartDelaySeconds) return new LeaderboardCategory("Custom", "Start delay is not default");
        if (countdownEnabled != defaults.countdownEnabled) return new LeaderboardCategory("Custom", "Countdown is not default");
        if (countdownMinutes != defaults.countdownMinutes) return new LeaderboardCategory("Custom", "Countdown time is not default");
        if (worldCustomBiomeSizeBlocks != defaults.worldCustomBiomeSizeBlocks) return new LeaderboardCategory("Custom", "World biome size is not default");
        if (worldTerrainHillinessPercent != defaults.worldTerrainHillinessPercent) return new LeaderboardCategory("Custom", "World hilliness is not default");
        if (!Objects.equals(worldSingleBiomeId, defaults.worldSingleBiomeId)) return new LeaderboardCategory("Custom", "World biome is not default");

        return new LeaderboardCategory("Default", "");
    }

    private record LeaderboardCategory(String label, String reason) {
    }

    private String daylightLabel() {
        return switch (daylightMode) {
            case DAYLIGHT_DAY -> "Day";
            case DAYLIGHT_NIGHT -> "Night";
            case DAYLIGHT_MIDNIGHT -> "Midnight";
            case DAYLIGHT_DAWN -> "Dawn";
            case DAYLIGHT_DUSK -> "Dusk";
            default -> "Enabled";
        };
    }

    private String registerLabel() {
        return registerMode == REGISTER_ALWAYS_HAVE
                ? "Always Have"
                : "Collect Once";
    }

    private String prelitPortalsLabel() {
        return switch (clampPrelitPortalsMode(prelitPortalsMode)) {
            case PRELIT_PORTALS_NETHER -> "Nether";
            case PRELIT_PORTALS_END -> "End";
            case PRELIT_PORTALS_BOTH -> "Both";
            default -> "Off";
        };
    }

    private String casinoModeLabel() {
        return switch (casinoMode) {
            case CASINO_ENABLED -> "Enabled";
            case CASINO_DRAFT -> "Draft";
            default -> "Disabled";
        };
    }

    private List<ServerPlayer> getPlayersWithSlotComplete(MinecraftServer server, String slotId) {
        if (server == null || slotId == null || slotId.isBlank()) return Collections.emptyList();

        List<ServerPlayer> matches = new ArrayList<>();
        for (ServerPlayer player : getParticipantPlayers(server)) {
            if (player == null) continue;
            if (isPlayerCompleteForSlot(player, slotId)) {
                matches.add(player);
            }
        }
        return matches;
    }

    private List<ServerPlayer> getPlayersWithSlotCompleteForGunGameIndex(
            MinecraftServer server,
            String slotId,
            int cardIndex
    ) {
        if (server == null || slotId == null || slotId.isBlank()) return Collections.emptyList();
        TeamData teamData = TeamData.get(server);
        List<ServerPlayer> matches = new ArrayList<>();
        for (ServerPlayer player : getParticipantPlayers(server)) {
            if (player == null) continue;
            UUID teamId = teamData.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
            if (teamId == null) continue;
            if (gunGameTeamIndex.getOrDefault(teamId, 0) != cardIndex) continue;
            if (isPlayerCompleteForSlot(player, slotId)) {
                matches.add(player);
            }
        }
        return matches;
    }

    private BingoCard generateUniqueCard(
            Supplier<BingoCard> supplier,
            Set<String> usedIds,
            long deadlineNanos,
            MinecraftServer server
    ) {
        if (supplier == null) return null;
        int attempts = 200;
        for (int i = 0; i < attempts; i++) {
            if (checkGenerationTimeout(server, deadlineNanos)) {
                return null;
            }
            BingoCard card = supplier.get();
            if (card == null) continue;
            Set<String> ids = card.getAllIds();
            if (Collections.disjoint(ids, usedIds)) {
                return card;
            }
        }
        if (checkGenerationTimeout(server, deadlineNanos)) {
            return null;
        }
        BingoCard fallback = supplier.get();
        if (fallback != null) {
            System.out.println("[JamieBingo] WARNING: Could not avoid repeat slots after " + attempts + " attempts.");
        }
        return fallback;
    }

    private boolean checkGenerationTimeout(MinecraftServer server, long deadlineNanos) {
        if (generationTimedOut) return true;
        if (System.nanoTime() <= deadlineNanos) return false;
        generationTimedOut = true;
        BroadcastHelper.broadcast(server,
                com.jamie.jamiebingo.util.ComponentUtil.literal("Card failed to generate in time. Try loosening rarity settings.")
        );
        LOGGER.warn("[JamieBingo] Card generation timed out.");
        return true;
    }

    private void abortCardGeneration(MinecraftServer server) {
        captureCurrentRunResult(server, false);
        active = false;
        startCountdownActive = false;
        currentCard = null;
        hangmanCards.clear();
        gunGameCards.clear();
        randomEffectsEnabled = false;
        randomEffectsNextTick = -1;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
    }

    private void addCardIdsToSet(BingoCard card, Set<String> usedIds) {
        if (card == null || usedIds == null) return;
        usedIds.addAll(card.getAllIds());
    }

    private boolean isPlayerCompleteForSlot(ServerPlayer player, String slotId) {
        if (player == null || slotId == null || slotId.isBlank()) return false;
        if (slotId.startsWith("quest.")) {
            return QuestTracker.isQuestComplete(player, slotId);
        }
        Identifier key = Identifier.tryParse(slotId);
        if (key == null) return false;
        Item item = ForgeRegistries.ITEMS.getValue(key);
        if (item == null) return false;
        return playerHasItem(player, item);
    }

    private boolean playerHasItem(ServerPlayer player, Item item) {
        if (player == null || item == null) return false;

        for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player))) {
            if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)
                    && com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack) == item) return true;
        }
        ItemStack head = com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(player, EquipmentSlot.HEAD);
        if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(head)
                && com.jamie.jamiebingo.util.ItemStackUtil.getItem(head) == item) return true;
        ItemStack chest = com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(player, EquipmentSlot.CHEST);
        if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(chest)
                && com.jamie.jamiebingo.util.ItemStackUtil.getItem(chest) == item) return true;
        ItemStack legs = com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(player, EquipmentSlot.LEGS);
        if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(legs)
                && com.jamie.jamiebingo.util.ItemStackUtil.getItem(legs) == item) return true;
        ItemStack feet = com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(player, EquipmentSlot.FEET);
        if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(feet)
                && com.jamie.jamiebingo.util.ItemStackUtil.getItem(feet) == item) return true;
        ItemStack offhand = com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(player, EquipmentSlot.OFFHAND);
        if (!com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(offhand)
                && com.jamie.jamiebingo.util.ItemStackUtil.getItem(offhand) == item) return true;
        return false;
    }

    private Set<String> getAllCompletedSlotIds() {
        Set<String> completedIds = new HashSet<>();
        for (Set<String> ids : teamProgress.values()) {
            if (ids != null) completedIds.addAll(ids);
        }
        for (Map.Entry<String, Set<UUID>> entry : slotOwners.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) continue;
            Set<UUID> owners = entry.getValue();
            if (owners == null || owners.isEmpty()) continue;
            completedIds.add(entry.getKey());
        }
        return completedIds;
    }

    private int countTeamsWithSlotComplete(MinecraftServer server, String slotId) {
        if (server == null || slotId == null || slotId.isBlank()) return 0;
        TeamData teamData = TeamData.get(server);
        Set<UUID> teams = new HashSet<>();
        for (ServerPlayer player : getParticipantPlayers(server)) {
            if (player == null) continue;
            if (!isPlayerCompleteForSlot(player, slotId)) continue;
            UUID teamId = teamData.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
            if (teamId != null) {
                teams.add(teamId);
            }
        }
        return teams.size();
    }

    private BingoCard generateCardForSlotReroll(MinecraftServer server, Random rng, int targetSize) {
        if (server == null || targetSize <= 0) return null;
        BingoCard generated;
        if (customPoolEnabled && winCondition != WinCondition.HANGMAN
                && winCondition != WinCondition.GUNGAME
                && winCondition != WinCondition.GAMEGUN) {
            generated = buildCardFromPool(targetSize, rng);
        } else if (customCardEnabled && winCondition != WinCondition.HANGMAN
                && winCondition != WinCondition.GUNGAME
                && winCondition != WinCondition.GAMEGUN) {
            generated = buildCustomCardFromSlots();
        } else {
            CardComposition rerollComposition = (winCondition == WinCondition.GUNGAME || winCondition == WinCondition.GAMEGUN)
                    ? CardComposition.CLASSIC_ONLY
                    : composition;
            generated = com.jamie.jamiebingo.bingo.ConfigurableCardGenerator.generate(
                    targetSize,
                    difficulty,
                    rerollComposition,
                    questPercent,
                    server,
                    this,
                    buildGenerationBlacklist()
            );
        }
        if (generated == null || generated.getSize() != targetSize) {
            return null;
        }
        return generated;
    }

    public BingoSlot generateRandomGoalNotOnCurrentCard(MinecraftServer server, Random rng, Set<String> excludedIds) {
        if (server == null) return null;
        if (rng == null) rng = new Random();
        int targetSize = currentCard != null && currentCard.getSize() > 0 ? currentCard.getSize() : Math.max(1, size);
        Set<String> blocked = excludedIds == null ? Set.of() : excludedIds;
        int attempts = 80;
        for (int i = 0; i < attempts; i++) {
            BingoCard generated = generateCardForSlotReroll(server, rng, targetSize);
            if (generated == null) continue;
            for (int y = 0; y < generated.getSize(); y++) {
                for (int x = 0; x < generated.getSize(); x++) {
                    BingoSlot candidate = generated.getSlot(x, y);
                    if (candidate == null || candidate.getId() == null || candidate.getId().isBlank()) continue;
                    if (blocked.contains(candidate.getId())) continue;
                    return copySlot(candidate);
                }
            }
        }
        return null;
    }

    private BingoSlot generateRerolledSlotForPosition(
            MinecraftServer server,
            Random rng,
            int x,
            int y,
            int size,
            String previousId,
            Set<String> reservedIds,
            Map<String, Integer> occupiedCounts,
            Set<String> seenIds,
            boolean avoidReservedIds
    ) {
        if (server == null || rng == null || size <= 0) return null;
        int attempts = 80;
        for (int i = 0; i < attempts; i++) {
            BingoCard generated = generateCardForSlotReroll(server, rng, size);
            if (generated == null) continue;
            BingoSlot primary = generated.getSlot(x, y);
            if (isRerollCandidateAllowed(primary, previousId, reservedIds, occupiedCounts, seenIds, avoidReservedIds)) {
                return copySlot(primary);
            }
            for (int ry = 0; ry < generated.getSize(); ry++) {
                for (int rx = 0; rx < generated.getSize(); rx++) {
                    BingoSlot candidate = generated.getSlot(rx, ry);
                    if (!isRerollCandidateAllowed(candidate, previousId, reservedIds, occupiedCounts, seenIds, avoidReservedIds)) continue;
                    return copySlot(candidate);
                }
            }
        }
        return null;
    }

    private boolean isRerollCandidateAllowed(
            BingoSlot candidate,
            String previousId,
            Set<String> reservedIds,
            Map<String, Integer> occupiedCounts,
            Set<String> seenIds,
            boolean avoidReservedIds
    ) {
        if (candidate == null || candidate.getId() == null || candidate.getId().isBlank()) return false;
        String id = candidate.getId();
        if (previousId != null && previousId.equals(id)) return false;
        if (avoidReservedIds && reservedIds != null && reservedIds.contains(id)) return false;
        if (occupiedCounts != null && occupiedCounts.getOrDefault(id, 0) > 0) return false;
        if (seenIds != null && seenIds.contains(id)) return false;
        return true;
    }

    private boolean sanitizeImmediateSpawnTies(MinecraftServer server) {
        if (server == null || currentCard == null) return false;
        if (winCondition != WinCondition.LOCKOUT
                && winCondition != WinCondition.RARITY
                && winCondition != WinCondition.GAMEGUN) {
            return false;
        }

        Random rng = new Random();
        int size = currentCard.getSize();
        if (size <= 0) return false;
        boolean changed = false;
        Set<String> completedIds = getAllCompletedSlotIds();
        Map<String, Integer> occupiedCounts = buildIdCounts(currentCard);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BingoSlot slot = currentCard.getSlot(x, y);
                if (slot == null || slot.getId() == null || slot.getId().isBlank()) continue;
                if (completedIds.contains(slot.getId())) continue;

                while (countTeamsWithSlotComplete(server, slot.getId()) >= 2) {
                    String previousId = slot.getId();
                    decrementIdCount(occupiedCounts, previousId);
                    BroadcastHelper.broadcast(server, com.jamie.jamiebingo.util.ComponentUtil.literal("tied goal, rerolled slot"));
                    BingoSlot rerolled = generateRerolledSlotForPosition(
                            server, rng, x, y, size, previousId, completedIds, occupiedCounts, shuffleSeenIdsShared, true
                    );
                    if (rerolled == null) {
                        incrementIdCount(occupiedCounts, previousId);
                        break;
                    }
                    currentCard.setSlot(x, y, rerolled);
                    slot = rerolled;
                    changed = true;
                    shuffleSeenIdsShared.add(rerolled.getId());
                    incrementIdCount(occupiedCounts, rerolled.getId());
                }
            }
        }

        if (changed && winCondition == WinCondition.GAMEGUN && !gunGameCards.isEmpty()) {
            int idx = Math.max(0, Math.min(gunGameSharedIndex, gunGameCards.size() - 1));
            gunGameCards.set(idx, copyCard(currentCard));
        }
        return changed;
    }

    private static Map<String, Integer> buildIdCounts(BingoCard card) {
        Map<String, Integer> counts = new HashMap<>();
        if (card == null) return counts;
        for (int y = 0; y < card.getSize(); y++) {
            for (int x = 0; x < card.getSize(); x++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot == null || slot.getId() == null || slot.getId().isBlank()) continue;
                counts.put(slot.getId(), counts.getOrDefault(slot.getId(), 0) + 1);
            }
        }
        return counts;
    }

    private static void decrementIdCount(Map<String, Integer> counts, String id) {
        if (counts == null || id == null || id.isBlank()) return;
        int next = counts.getOrDefault(id, 0) - 1;
        if (next <= 0) {
            counts.remove(id);
        } else {
            counts.put(id, next);
        }
    }

    private static void incrementIdCount(Map<String, Integer> counts, String id) {
        if (counts == null || id == null || id.isBlank()) return;
        counts.put(id, counts.getOrDefault(id, 0) + 1);
    }

    public boolean handleImmediateHangmanTie(MinecraftServer server, String slotId) {
        if (server == null || winCondition != WinCondition.HANGMAN) return false;
        List<ServerPlayer> matches = getPlayersWithSlotComplete(server, slotId);
        if (matches.size() < 2) return false;

        TeamData teamData = TeamData.get(server);
        List<String> names = new ArrayList<>();
        Set<UUID> awardedTeams = new HashSet<>();
        for (ServerPlayer player : matches) {
            UUID teamId = teamData.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
            if (teamId == null) continue;
            if (awardedTeams.add(teamId)) {
                TeamScoreData.get(server).award(teamId, com.jamie.jamiebingo.util.EntityUtil.getUUID(player), 1);
            }
            names.add(player.getGameProfile().name());
        }

        BroadcastHelper.broadcastTeamScores(server);
        BroadcastHelper.broadcast(
                server,
                com.jamie.jamiebingo.util.ComponentUtil.literal(
                        "Tie! " + String.join(", ", names) + " already had it and share the point."
                )
        );

        com.jamie.jamiebingo.bingo.HangmanTicker.startIntermission(server, this);
        com.jamie.jamiebingo.bingo.HangmanTicker.checkEarlyWin(server, this);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        return true;
    }

    public void handleImmediateGameGunTie(MinecraftServer server) {
        if (server == null || winCondition != WinCondition.GAMEGUN) return;
        if (currentCard == null) return;
        if (sanitizeImmediateSpawnTies(server)) {
            BroadcastHelper.broadcastFullSync();
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        }
    }

    public void handleImmediateGunGameTie(MinecraftServer server, int cardIndex) {
        if (server == null || winCondition != WinCondition.GUNGAME) return;
        if (cardIndex < 0 || cardIndex >= gunGameCards.size()) return;

        int activeIndex = cardIndex;
        for (int guard = 0; guard < 5; guard++) {
            BingoCard card = gunGameCards.get(activeIndex);
            if (card == null) return;

            BingoSlot tiedSlot = null;
            for (int y = 0; y < card.getSize(); y++) {
                for (int x = 0; x < card.getSize(); x++) {
                    BingoSlot slot = card.getSlot(x, y);
                    if (slot == null) continue;
                    List<ServerPlayer> matches = getPlayersWithSlotCompleteForGunGameIndex(server, slot.getId(), activeIndex);
                    if (matches.size() >= 2) {
                        tiedSlot = slot;
                        break;
                    }
                }
                if (tiedSlot != null) break;
            }

            if (tiedSlot == null) return;

            List<ServerPlayer> matches = getPlayersWithSlotCompleteForGunGameIndex(server, tiedSlot.getId(), activeIndex);
            if (matches.size() < 2) return;

            TeamData teamData = TeamData.get(server);
            List<String> names = new ArrayList<>();
            Set<UUID> awardedTeams = new HashSet<>();
            for (ServerPlayer player : matches) {
                UUID teamId = teamData.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
                if (teamId == null) continue;
                if (gunGameTeamIndex.getOrDefault(teamId, 0) != activeIndex) continue;
                if (awardedTeams.add(teamId)) {
                    TeamScoreData.get(server).award(teamId, com.jamie.jamiebingo.util.EntityUtil.getUUID(player), 1);
                }
                names.add(player.getGameProfile().name());
            }

            BroadcastHelper.broadcastTeamScores(server);
            BroadcastHelper.broadcast(
                    server,
                    com.jamie.jamiebingo.util.ComponentUtil.literal(
                            "Tie! " + String.join(", ", names) + " already had it and share the point."
                    )
            );

            int nextIndex = activeIndex + 1;
            if (nextIndex >= gunGameCards.size()) {
                BingoWinEvaluator.forcePointsWin(server);
                com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
                return;
            }

            for (TeamData.TeamInfo team : teamData.getTeams()) {
                if (team.members.isEmpty()) continue;
                if (gunGameTeamIndex.getOrDefault(team.id, 0) != activeIndex) continue;

                gunGameTeamIndex.put(team.id, nextIndex);
                revealedSlots.remove(team.id);
                highlightedSlots.remove(team.id);
                for (Set<UUID> owners : slotOwners.values()) {
                    owners.remove(team.id);
                }

                for (UUID memberId : team.members) {
                    ServerPlayer sp = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, memberId);
                    if (sp != null) {
                        BroadcastHelper.syncCard(sp);
                    }
                }
            }

            activeIndex = nextIndex;
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
        }
    }

    /* =========================
       QUEST HELPERS
       ========================= */

    public static boolean isQuestModeRunning() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;

        BingoGameData data = get(server);
        if (!data.active) return false;
        if (data.composition != CardComposition.CLASSIC_ONLY) return true;
        return data.currentCardHasQuests();
    }

    public boolean currentCardHasQuests() {
        BingoCard card = currentCard;
        if (card == null) return false;
        for (int y = 0; y < card.getSize(); y++) {
            for (int x = 0; x < card.getSize(); x++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot != null && slot.getId() != null && slot.getId().startsWith("quest.")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean completeQuest(MinecraftServer server, String id, UUID playerId) {
        if (server == null) return false;

        BingoGameData data = get(server);
        if (!data.active) return false;
        if (!data.cardContainsForPlayer(playerId, id)) return false;

        boolean newlyCompleted = data.markCompleted(playerId, id);
        if (newlyCompleted) {
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        }
        return newlyCompleted;
    }

    /* =========================
       SAVE / LOAD
       ========================= */

    public static BingoGameData load(CompoundTag tag) {
        BingoGameData d = new BingoGameData();
d.randomSizeIntent = getBoolean(tag, "RandomSizeIntent", false);
  d.randomWinConditionIntent = getBoolean(tag, "RandomWinIntent", false);
  d.randomDifficultyIntent = getBoolean(tag, "RandomDifficultyIntent", false);
  d.randomRerollsIntent = getBoolean(tag, "RandomRerollsIntent", false);

  d.randomHostileMobsIntent = getBoolean(tag, "RandomHostileMobsIntent", false);
  d.randomHungerIntent = getBoolean(tag, "RandomHungerIntent", false);
  d.randomRtpIntent = getBoolean(tag, "RandomRtpIntent", false);
  d.randomDaylightIntent = getBoolean(tag, "RandomDaylightIntent", false);
  d.randomPvpIntent = getBoolean(tag, "RandomPvpIntent", false);

  d.randomEffectsIntervalIntent = getBoolean(tag, "RandomEffectsIntervalIntent", false);

  d.randomHardcoreIntent = getBoolean(tag, "RandomHardcoreIntent", false);
  d.randomKeepInventoryIntent = getBoolean(tag, "RandomKeepInventoryIntent", false);

d.hardcoreEnabled = getBoolean(tag, "HardcoreEnabled", d.hardcoreEnabled);
d.keepInventoryEnabled = getBoolean(tag, "KeepInventoryEnabled", d.keepInventoryEnabled);
d.allowLateJoin = tag.contains("AllowLateJoin") ? getBoolean(tag, "AllowLateJoin", d.allowLateJoin) : d.allowLateJoin;
d.teamChestEnabled = tag.contains("TeamChestEnabled") ? getBoolean(tag, "TeamChestEnabled", d.teamChestEnabled) : d.teamChestEnabled;
d.hangmanRounds = tag.contains("HangmanRounds") ? getInt(tag, "HangmanRounds", d.hangmanRounds) : d.hangmanRounds;
d.hangmanBaseSeconds = tag.contains("HangmanBaseSeconds") ? getInt(tag, "HangmanBaseSeconds", d.hangmanBaseSeconds) : d.hangmanBaseSeconds;
d.hangmanPenaltySeconds = tag.contains("HangmanPenaltySeconds") ? getInt(tag, "HangmanPenaltySeconds", d.hangmanPenaltySeconds) : d.hangmanPenaltySeconds;
d.daylightMode = tag.contains("DaylightMode")
        ? getInt(tag, "DaylightMode", DAYLIGHT_ENABLED)
        : DAYLIGHT_ENABLED;
d.customCardEnabled = tag.contains("CustomCardEnabled") ? getBoolean(tag, "CustomCardEnabled", d.customCardEnabled) : d.customCardEnabled;
d.customPoolEnabled = tag.contains("CustomPoolEnabled") ? getBoolean(tag, "CustomPoolEnabled", d.customPoolEnabled) : d.customPoolEnabled;
  d.pvpEnabled = tag.contains("PvpEnabled")
          ? getBoolean(tag, "PvpEnabled", true)
          : true;
d.adventureMode = tag.contains("AdventureMode")
        ? getBoolean(tag, "AdventureMode", d.adventureMode)
        : d.adventureMode;
d.prelitPortalsMode = tag.contains("PrelitPortalsMode")
        ? clampPrelitPortalsMode(getInt(tag, "PrelitPortalsMode", d.prelitPortalsMode))
        : d.prelitPortalsMode;
d.registerMode = tag.contains("RegisterMode")
        ? getInt(tag, "RegisterMode", REGISTER_COLLECT_ONCE)
        : REGISTER_COLLECT_ONCE;
d.teamSyncEnabled = tag.contains("TeamSyncEnabled")
        ? getBoolean(tag, "TeamSyncEnabled", d.teamSyncEnabled)
        : d.teamSyncEnabled;
d.shuffleEnabled = tag.contains("ShuffleEnabled")
        ? getBoolean(tag, "ShuffleEnabled", d.shuffleEnabled)
        : d.shuffleEnabled;
d.randomShuffleIntent = tag.contains("RandomShuffleIntent")
        ? getBoolean(tag, "RandomShuffleIntent", d.randomShuffleIntent)
        : d.randomShuffleIntent;

d.bingoStartDelaySeconds = getInt(tag, "BingoStartDelaySeconds", d.bingoStartDelaySeconds);
d.countdownEnabled = tag.contains("CountdownEnabled") ? getBoolean(tag, "CountdownEnabled", d.countdownEnabled) : d.countdownEnabled;
  d.countdownMinutes = tag.contains("CountdownMinutes")
          ? Math.max(1, getInt(tag, "CountdownMinutes", 10))
          : 10;
d.rushEnabled = tag.contains("RushEnabled")
        ? getBoolean(tag, "RushEnabled", d.rushEnabled)
        : d.rushEnabled;
d.rushSeconds = tag.contains("RushSeconds")
        ? Math.max(1, Math.min(300, getInt(tag, "RushSeconds", d.rushSeconds)))
        : d.rushSeconds;
        d.pregameBoxBuilt = getBoolean(tag, "PregameBoxBuilt", d.pregameBoxBuilt);
        d.pregameBoxActive = getBoolean(tag, "PregameBoxActive", d.pregameBoxActive);
        d.pregameBoxX = getInt(tag, "PregameBoxX", d.pregameBoxX);
        d.pregameBoxY = getInt(tag, "PregameBoxY", d.pregameBoxY);
        d.pregameBoxZ = getInt(tag, "PregameBoxZ", d.pregameBoxZ);
        d.lastGameSpawnSet = getBoolean(tag, "LastGameSpawnSet", d.lastGameSpawnSet);
        d.lastGameSpawnX = getInt(tag, "LastGameSpawnX", d.lastGameSpawnX);
        d.lastGameSpawnY = getInt(tag, "LastGameSpawnY", d.lastGameSpawnY);
        d.lastGameSpawnZ = getInt(tag, "LastGameSpawnZ", d.lastGameSpawnZ);
        d.worldUseNewSeedEachGame = getBoolean(tag, "WorldUseNewSeedEachGame", d.worldUseNewSeedEachGame);
        d.worldTypeMode = normalizeWorldType(getInt(tag, "WorldTypeMode", d.worldTypeMode));
        d.worldSmallBiomes = getBoolean(tag, "WorldSmallBiomes", d.worldSmallBiomes);
        d.worldCustomBiomeSizeBlocks = clampWorldBiomeSize(getInt(tag, "WorldCustomBiomeSizeBlocks", d.worldCustomBiomeSizeBlocks));
        d.worldTerrainHillinessPercent = clampWorldTerrainHilliness(getInt(tag, "WorldTerrainHillinessPercent", d.worldTerrainHillinessPercent));
        d.worldStructureFrequencyPercent = clampWorldStructureFrequency(getInt(tag, "WorldStructureFrequencyPercent", d.worldStructureFrequencyPercent));
        d.worldSingleBiomeId = getString(tag, "WorldSingleBiomeId", d.worldSingleBiomeId);
        if (d.worldSingleBiomeId == null || d.worldSingleBiomeId.isBlank()) {
            d.worldSingleBiomeId = "minecraft:plains";
        }
        d.worldSurfaceCaveBiomes = getBoolean(tag, "WorldSurfaceCaveBiomes", d.worldSurfaceCaveBiomes);
        d.worldSetSeedText = getString(tag, "WorldSetSeedText", d.worldSetSeedText);
        if (d.worldSetSeedText == null) d.worldSetSeedText = "";
        d.worldFreshSeedPrepared = getBoolean(tag, "WorldFreshSeedPrepared", d.worldFreshSeedPrepared);
        // Runtime-only state: always clear on load to avoid sticky "in progress" after crashes/restarts.
        d.worldRegenInProgress = false;
        d.worldRegenQueued = false;
        d.worldRegenStage = "";
        d.pendingStartAfterWorldRegen = false;
        d.pendingWeeklyChallengeStart = false;
        d.pendingWeeklyChallengeBaseSeed = 0L;
        d.activeWeeklyChallengeId = getString(tag, "ActiveWeeklyChallengeId", "");
        d.activeWeeklySettingsSeed = getString(tag, "ActiveWeeklySettingsSeed", "");
        d.activeWeeklyWorldSeed = getString(tag, "ActiveWeeklyWorldSeed", "");
        d.activeWeeklyCardSeed = getString(tag, "ActiveWeeklyCardSeed", "");
        d.gameStartSpawnPrepared = false;
        d.gameStartSpawnLoading = false;
        d.gameStartSpawnLoadingIndex = 0;
        d.gameStartSpawnLoadingTotal = 0;
        d.active = getBoolean(tag, "Active", d.active);
        d.gameStartTick = getInt(tag, "GameStartTick", d.gameStartTick);
        d.countdownEndTick = getInt(tag, "CountdownEndTick", d.countdownEndTick);
        d.countdownExpired = getBoolean(tag, "CountdownExpired", d.countdownExpired);
        d.resumeElapsedSeconds = getInt(tag, "ResumeElapsedSeconds", d.resumeElapsedSeconds);
        d.resumeCountdownRemainingSeconds = getInt(tag, "ResumeCountdownRemainingSeconds", d.resumeCountdownRemainingSeconds);
        int savedTimerSeconds = getInt(tag, "SavedTimerSeconds", -1);
        if (d.active && !d.startCountdownActive && savedTimerSeconds >= 0
                && d.resumeElapsedSeconds < 0 && d.resumeCountdownRemainingSeconds < 0) {
            if (d.countdownEnabled) {
                d.resumeCountdownRemainingSeconds = savedTimerSeconds;
            } else {
                d.resumeElapsedSeconds = savedTimerSeconds;
            }
        }
        d.timerResumePending = d.active
                && !d.startCountdownActive
                && (d.resumeElapsedSeconds >= 0
                || d.resumeCountdownRemainingSeconds >= 0
                || d.gameStartTick != -1
                || d.countdownEndTick >= 0);
        if (tag.contains("RushRemainingByTeam")) {
            CompoundTag rushRemaining = tag.getCompoundOrEmpty("RushRemainingByTeam");
            d.rushResumeSecondsByTeam.clear();
            for (String key : rushRemaining.keySet()) {
                try {
                    UUID teamId = UUID.fromString(key);
                    int remaining = Math.max(0, getInt(rushRemaining, key, 0));
                    d.rushResumeSecondsByTeam.put(teamId, remaining);
                } catch (Exception ignored) {
                }
            }
        }
        d.mineResumeSourceIds.clear();
        if (tag.contains("MineResumeSourceIds")) {
            ListTag ids = tag.getListOrEmpty("MineResumeSourceIds");
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.getStringOr(i, "");
                if (id == null || id.isBlank()) continue;
                d.mineResumeSourceIds.add(id);
            }
        }
        d.mineResumeRemainingSecondsByPlayer.clear();
        if (tag.contains("MineResumeRemainingByPlayer")) {
            CompoundTag all = tag.getCompoundOrEmpty("MineResumeRemainingByPlayer");
            for (String key : all.keySet()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    int seconds = Math.max(0, getInt(all, key, 0));
                    if (seconds > 0) {
                        d.mineResumeRemainingSecondsByPlayer.put(playerId, seconds);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        d.mineResumeTriggeredSourceByPlayer.clear();
        if (tag.contains("MineResumeTriggeredByPlayer")) {
            CompoundTag all = tag.getCompoundOrEmpty("MineResumeTriggeredByPlayer");
            for (String key : all.keySet()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    String sourceId = getString(all, key, "");
                    if (sourceId != null && !sourceId.isBlank()) {
                        d.mineResumeTriggeredSourceByPlayer.put(playerId, sourceId);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        d.mineResumeDamageProgressByPlayer.clear();
        if (tag.contains("MineResumeDamageByPlayer")) {
            CompoundTag all = tag.getCompoundOrEmpty("MineResumeDamageByPlayer");
            for (String key : all.keySet()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    int progress = Math.max(0, getInt(all, key, 0));
                    d.mineResumeDamageProgressByPlayer.put(playerId, progress);
                } catch (Exception ignored) {
                }
            }
        }
        d.mineResumeDefuseQuestId = getString(tag, "MineResumeDefuseQuestId", d.mineResumeDefuseQuestId);
        d.mineResumeDefuseDisplayName = getString(tag, "MineResumeDefuseDisplayName", d.mineResumeDefuseDisplayName);
        d.powerResumeSlotId = getString(tag, "PowerResumeSlotId", d.powerResumeSlotId);
        d.powerResumeDisplayName = getString(tag, "PowerResumeDisplayName", d.powerResumeDisplayName);
        d.powerResumeRemainingSeconds = getInt(tag, "PowerResumeRemainingSeconds", d.powerResumeRemainingSeconds);
        d.powerResumeClaimed = getBoolean(tag, "PowerResumeClaimed", d.powerResumeClaimed);

        // Runtime-only countdown/casino flags should never persist into active matches after reload.
        d.startCountdownActive = false;
        d.startCountdownSeconds = 0;
        d.startCountdownEndTick = -1;
        if (d.active) {
            d.pregameBoxActive = false;
        }
        d.rerollPhaseActive = false;
        d.rerollTurnOrder.clear();
        d.rerollTurnIndex = 0;
        d.lastPlayedSeed = getString(tag, "LastPlayedSeed", d.lastPlayedSeed);
        d.currentRunPreviewSize = getInt(tag, "CurrentRunPreviewSize", d.currentRunPreviewSize);
        d.currentRunPreviewSlotIds.clear();
        if (tag.contains("CurrentRunPreviewSlots")) {
            ListTag preview = tag.getListOrEmpty("CurrentRunPreviewSlots");
            for (int i = 0; i < preview.size(); i++) {
                d.currentRunPreviewSlotIds.add(preview.getStringOr(i, ""));
            }
        }
        if (tag.contains("CurrentCard")) {
            d.currentCard = loadCard(tag.getCompoundOrEmpty("CurrentCard"));
        }
        if (tag.contains("HangmanCards")) {
            ListTag cards = tag.getListOrEmpty("HangmanCards");
            d.hangmanCards.clear();
            for (int i = 0; i < cards.size(); i++) {
                BingoCard card = loadCard(cards.getCompoundOrEmpty(i));
                if (card != null) {
                    d.hangmanCards.add(card);
                }
            }
        }
        if (tag.contains("GunGameCards")) {
            ListTag cards = tag.getListOrEmpty("GunGameCards");
            d.gunGameCards.clear();
            for (int i = 0; i < cards.size(); i++) {
                BingoCard card = loadCard(cards.getCompoundOrEmpty(i));
                if (card != null) {
                    d.gunGameCards.add(card);
                }
            }
        }
        d.hangmanRoundIndex = getInt(tag, "HangmanRoundIndex", d.hangmanRoundIndex);
        d.gunGameSharedIndex = getInt(tag, "GunGameSharedIndex", d.gunGameSharedIndex);
        if (tag.contains("GunGameTeamIndex")) {
            CompoundTag byTeam = tag.getCompoundOrEmpty("GunGameTeamIndex");
            for (String key : byTeam.keySet()) {
                try {
                    UUID teamId = UUID.fromString(key);
                    d.gunGameTeamIndex.put(teamId, Math.max(0, getInt(byTeam, key, 0)));
                } catch (Exception ignored) {
                }
            }
        }
        if (tag.contains("Progress")) {
            ListTag list = tag.getListOrEmpty("Progress");
            d.progress.clear();
            for (int i = 0; i < list.size(); i++) {
                String id = list.getStringOr(i, "");
                if (!id.isBlank()) {
                    d.progress.add(id);
                }
            }
        }
        if (tag.contains("TeamProgress")) {
            CompoundTag all = tag.getCompoundOrEmpty("TeamProgress");
            d.teamProgress.clear();
            for (String key : all.keySet()) {
                UUID teamId;
                try {
                    teamId = UUID.fromString(key);
                } catch (Exception ignored) {
                    continue;
                }
                ListTag list = all.getListOrEmpty(key);
                Set<String> ids = new HashSet<>();
                for (int i = 0; i < list.size(); i++) {
                    String id = list.getStringOr(i, "");
                    if (!id.isBlank()) {
                        ids.add(id);
                    }
                }
                if (!ids.isEmpty()) {
                    d.teamProgress.put(teamId, ids);
                }
            }
        }
        if (tag.contains("SlotOwners")) {
            CompoundTag all = tag.getCompoundOrEmpty("SlotOwners");
            d.slotOwners.clear();
            for (String slotId : all.keySet()) {
                if (slotId == null || slotId.isBlank()) continue;
                ListTag ownersTag = all.getListOrEmpty(slotId);
                Set<UUID> owners = new HashSet<>();
                for (int i = 0; i < ownersTag.size(); i++) {
                    String raw = ownersTag.getStringOr(i, "");
                    if (raw == null || raw.isBlank()) continue;
                    try {
                        owners.add(UUID.fromString(raw));
                    } catch (Exception ignored) {
                    }
                }
                if (!owners.isEmpty()) {
                    d.slotOwners.put(slotId, owners);
                }
            }
        }
        if (tag.contains("PlayerGameHistory")) {
            CompoundTag historyTag = tag.getCompoundOrEmpty("PlayerGameHistory");
            for (String playerKey : historyTag.keySet()) {
                UUID playerId;
                try {
                    playerId = UUID.fromString(playerKey);
                } catch (Exception ignored) {
                    continue;
                }
                ListTag entriesTag = historyTag.getListOrEmpty(playerKey);
                List<GameHistoryEntry> entries = new ArrayList<>();
                for (int i = 0; i < entriesTag.size(); i++) {
                    CompoundTag entryTag = entriesTag.getCompoundOrEmpty(i);
                    entries.add(GameHistoryEntry.load(entryTag));
                }
                if (!entries.isEmpty()) {
                    d.playerGameHistory.put(playerId, entries);
                }
            }
        }
        d.size = getInt(tag, "Size", d.size);
        d.difficulty = getString(tag, "Difficulty", d.difficulty);
        d.questPercent = getInt(tag, "QuestPercent", d.questPercent);
        d.categoryLogicEnabled = tag.contains("CategoryLogicEnabled")
                ? getBoolean(tag, "CategoryLogicEnabled", d.categoryLogicEnabled)
                : d.categoryLogicEnabled;
        d.rarityLogicEnabled = tag.contains("RarityLogicEnabled")
                ? getBoolean(tag, "RarityLogicEnabled", d.rarityLogicEnabled)
                : d.rarityLogicEnabled;
        d.itemColorVariantsSeparate = tag.contains("ItemColorVariantsSeparate")
                ? getBoolean(tag, "ItemColorVariantsSeparate", d.itemColorVariantsSeparate)
                : d.itemColorVariantsSeparate;
        d.rerollsPerPlayer = getInt(tag, "RerollsPerPlayer", d.rerollsPerPlayer);
        d.shuffleEnabled = getBoolean(tag, "ShuffleEnabled", d.shuffleEnabled);
        d.randomShuffleIntent = getBoolean(tag, "RandomShuffleIntent", d.randomShuffleIntent);
        d.casinoMode = tag.contains("CasinoMode")
                ? getInt(tag, "CasinoMode", d.casinoMode)
                : (com.jamie.jamiebingo.casino.CasinoModeManager.isCasinoEnabled() ? CASINO_ENABLED : CASINO_DISABLED);
        d.minesEnabled = getBoolean(tag, "MinesEnabled", d.minesEnabled);
        d.mineAmount = Math.max(1, Math.min(13, getInt(tag, "MineAmount", d.mineAmount)));
        d.mineTimeSeconds = Math.max(1, getInt(tag, "MineTimeSeconds", d.mineTimeSeconds));
        d.powerSlotEnabled = getBoolean(tag, "PowerSlotEnabled", d.powerSlotEnabled);
        d.powerSlotIntervalSeconds = Math.max(10, Math.min(300, getInt(tag, "PowerSlotIntervalSeconds", d.powerSlotIntervalSeconds)));
        d.fakeRerollsEnabled = getBoolean(tag, "FakeRerollsEnabled", d.fakeRerollsEnabled);
        d.fakeRerollsPerPlayer = Math.max(1, Math.min(10, getInt(tag, "FakeRerollsPerPlayer", d.fakeRerollsPerPlayer)));
        d.hideGoalDetailsInChat = getBoolean(tag, "HideGoalDetailsInChat", d.hideGoalDetailsInChat);
        d.starterKitMode = d.normalizeStarterKitMode(getInt(tag, "StarterKitMode", d.starterKitMode));
        d.randomEffectsArmed = getBoolean(tag, "RandomEffectsArmed", d.randomEffectsArmed);
        d.randomEffectsIntervalSeconds = getInt(tag, "RandomEffectsIntervalSeconds", d.randomEffectsIntervalSeconds);
        d.activeRandomEffectId = getString(tag, "ActiveRandomEffectId", d.activeRandomEffectId);
d.appliedRandomEffectId = getString(tag, "AppliedRandomEffectId", d.appliedRandomEffectId);
d.rtpEnabled = getBoolean(tag, "RtpEnabled", d.rtpEnabled);
d.hungerEnabled = tag.contains("HungerEnabled")
        ? getBoolean(tag, "HungerEnabled", true)
        : true;
d.naturalRegenEnabled = tag.contains("NaturalRegenEnabled")
        ? getBoolean(tag, "NaturalRegenEnabled", d.naturalRegenEnabled)
        : d.naturalRegenEnabled;

  d.hostileMobsEnabled = tag.contains("HostileMobsEnabled")
          ? getBoolean(tag, "HostileMobsEnabled", true)
          : true;

  d.lastNonPeacefulDifficulty = tag.contains("LastDifficulty")
          ? getString(tag, "LastDifficulty", d.lastNonPeacefulDifficulty)
        : "normal";

if (!d.activeRandomEffectId.isBlank()) {
    var rl = com.jamie.jamiebingo.util.IdUtil.id(d.activeRandomEffectId);
    var effect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(rl);
    if (effect != null) {
        d.activeRandomEffectName = rl.getPath().replace('_', ' ');
    }
}

        // runtime-only, always recomputed
        d.randomEffectsNextTick = -1;

        if (tag.contains("Mode")) d.mode = BingoMode.valueOf(getString(tag, "Mode", d.mode.name()));
        if (tag.contains("Composition")) d.composition = CardComposition.valueOf(getString(tag, "Composition", d.composition.name()));
        if (tag.contains("WinCondition")) d.winCondition = WinCondition.valueOf(getString(tag, "WinCondition", d.winCondition.name()));

        if (tag.contains("RevealedSlots")) {
            CompoundTag all = tag.getCompoundOrEmpty("RevealedSlots");
            for (String key : all.keySet()) {
                UUID teamId = UUID.fromString(key);
                ListTag list = all.getListOrEmpty(key);
                Set<String> slots = new HashSet<>();
                for (int i = 0; i < list.size(); i++) {
                    String id = list.getStringOr(i, "");
                    if (!id.isBlank()) {
                        slots.add(id);
                    }
                }
                d.revealedSlots.put(teamId, slots);
            }
        }

        if (tag.contains("HighlightedSlots")) {
            CompoundTag all = tag.getCompoundOrEmpty("HighlightedSlots");
            for (String key : all.keySet()) {
                UUID teamId = UUID.fromString(key);
                ListTag list = all.getListOrEmpty(key);
                Set<String> slots = new HashSet<>();
                for (int i = 0; i < list.size(); i++) {
                    String id = list.getStringOr(i, "");
                    if (!id.isBlank()) {
                        slots.add(id);
                    }
                }
                d.highlightedSlots.put(teamId, slots);
            }
        }

        if (tag.contains("CustomCardSlots")) {
            ListTag list = tag.getListOrEmpty("CustomCardSlots");
            d.customCardSlots.clear();
            for (int i = 0; i < list.size(); i++) {
                d.customCardSlots.add(list.getStringOr(i, ""));
            }
        }

        if (tag.contains("CustomPoolIds")) {
            ListTag list = tag.getListOrEmpty("CustomPoolIds");
            d.customPoolIds.clear();
            for (int i = 0; i < list.size(); i++) {
                String id = list.getStringOr(i, "");
                if (!id.isBlank()) {
                    d.customPoolIds.add(id);
                }
            }
        }

        if (tag.contains("CustomMineIds")) {
            ListTag list = tag.getListOrEmpty("CustomMineIds");
            d.customMineIds.clear();
            for (int i = 0; i < list.size(); i++) {
                String id = list.getStringOr(i, "");
                if (!id.isBlank()) {
                    d.customMineIds.add(id);
                }
            }
        }

        if (tag.contains("BlacklistedSlotIds")) {
            ListTag list = tag.getListOrEmpty("BlacklistedSlotIds");
            d.blacklistedSlotIds.clear();
            for (int i = 0; i < list.size(); i++) {
                String id = list.getStringOr(i, "");
                if (!id.isBlank()) {
                    d.blacklistedSlotIds.add(id);
                }
            }
        }

        if (tag.contains("WhitelistedSlotIds")) {
            ListTag list = tag.getListOrEmpty("WhitelistedSlotIds");
            d.whitelistedSlotIds.clear();
            for (int i = 0; i < list.size(); i++) {
                String id = list.getStringOr(i, "");
                if (!id.isBlank()) {
                    d.whitelistedSlotIds.add(id);
                }
            }
        }

        if (tag.contains("RarityOverrides")) {
            CompoundTag overrides = tag.getCompoundOrEmpty("RarityOverrides");
            d.rarityOverrides.clear();
            for (String key : overrides.keySet()) {
                if (key == null || key.isBlank()) continue;
                String rarity = BingoRarityUtil.normalize(overrides.getStringOr(key, ""));
                if (BingoRarityUtil.isKnown(rarity)) {
                    d.rarityOverrides.put(key, rarity);
                }
            }
        }

        if (tag.contains("EliminatedPlayers")) {
            ListTag list = tag.getListOrEmpty("EliminatedPlayers");
            for (int i = 0; i < list.size(); i++) {
                String raw = list.getStringOr(i, "");
                if (!raw.isBlank()) {
                    d.eliminatedPlayers.add(UUID.fromString(raw));
                }
            }
        }

        if (tag.contains("EliminatedTeams")) {
            ListTag list = tag.getListOrEmpty("EliminatedTeams");
            for (int i = 0; i < list.size(); i++) {
                String raw = list.getStringOr(i, "");
                if (!raw.isBlank()) {
                    d.eliminatedTeams.add(UUID.fromString(raw));
                }
            }
        }

        if (tag.contains("Participants")) {
            ListTag list = tag.getListOrEmpty("Participants");
            for (int i = 0; i < list.size(); i++) {
                String raw = list.getStringOr(i, "");
                if (!raw.isBlank()) {
                    d.participants.add(UUID.fromString(raw));
                }
            }
        }

        if (tag.contains("Spectators")) {
            ListTag list = tag.getListOrEmpty("Spectators");
            for (int i = 0; i < list.size(); i++) {
                String raw = list.getStringOr(i, "");
                if (!raw.isBlank()) {
                    d.spectators.add(UUID.fromString(raw));
                }
            }
        }

        if (tag.contains("SpectatorViews")) {
            CompoundTag views = tag.getCompoundOrEmpty("SpectatorViews");
            for (String key : views.keySet()) {
                try {
                    UUID spectatorId = UUID.fromString(key);
                    UUID targetId = getUuid(views, key);
                    if (targetId != null) {
                        d.spectatorViewTargets.put(spectatorId, targetId);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (d.active && d.currentCard == null && d.lastPlayedSeed != null && !d.lastPlayedSeed.isBlank()) {
            try {
                CardSeedCodec.SeedData seed = CardSeedCodec.decode(d.lastPlayedSeed);
                if (seed != null && seed.cards() != null && !seed.cards().isEmpty()) {
                    d.currentCard = seed.cards().get(0);
                    if (d.hangmanCards.isEmpty() && d.winCondition == WinCondition.HANGMAN) {
                        d.hangmanCards.addAll(seed.cards());
                    }
                    if (d.gunGameCards.isEmpty()
                            && (d.winCondition == WinCondition.GUNGAME || d.winCondition == WinCondition.GAMEGUN)) {
                        d.gunGameCards.addAll(seed.cards());
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return d;
    }

    public CompoundTag save(CompoundTag tag) {
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "RandomSizeIntent", randomSizeIntent);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "RandomWinIntent", randomWinConditionIntent);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "RandomDifficultyIntent", randomDifficultyIntent);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "RandomRerollsIntent", randomRerollsIntent);

com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "RandomHostileMobsIntent", randomHostileMobsIntent);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "RandomHungerIntent", randomHungerIntent);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "RandomRtpIntent", randomRtpIntent);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "RandomDaylightIntent", randomDaylightIntent);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "RandomPvpIntent", randomPvpIntent);

com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "RandomEffectsIntervalIntent", randomEffectsIntervalIntent);

com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "RandomHardcoreIntent", randomHardcoreIntent);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "RandomKeepInventoryIntent", randomKeepInventoryIntent);

com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "HardcoreEnabled", hardcoreEnabled);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "KeepInventoryEnabled", keepInventoryEnabled);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "AllowLateJoin", allowLateJoin);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "TeamChestEnabled", teamChestEnabled);
com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "HangmanRounds", hangmanRounds);
com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "HangmanBaseSeconds", hangmanBaseSeconds);
com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "HangmanPenaltySeconds", hangmanPenaltySeconds);
com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "DaylightMode", daylightMode);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "PvpEnabled", pvpEnabled);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "AdventureMode", adventureMode);
com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "PrelitPortalsMode", clampPrelitPortalsMode(prelitPortalsMode));
com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "RegisterMode", registerMode);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "TeamSyncEnabled", teamSyncEnabled);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "ShuffleEnabled", shuffleEnabled);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "RandomShuffleIntent", randomShuffleIntent);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "CustomCardEnabled", customCardEnabled);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "CustomPoolEnabled", customPoolEnabled);

com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "BingoStartDelaySeconds", bingoStartDelaySeconds);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "CountdownEnabled", countdownEnabled);
com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "CountdownMinutes", countdownMinutes);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "RushEnabled", rushEnabled);
com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "RushSeconds", rushSeconds);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "PregameBoxBuilt", pregameBoxBuilt);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "PregameBoxActive", pregameBoxActive);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "PregameBoxX", pregameBoxX);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "PregameBoxY", pregameBoxY);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "PregameBoxZ", pregameBoxZ);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "LastGameSpawnSet", lastGameSpawnSet);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "LastGameSpawnX", lastGameSpawnX);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "LastGameSpawnY", lastGameSpawnY);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "LastGameSpawnZ", lastGameSpawnZ);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "WorldUseNewSeedEachGame", worldUseNewSeedEachGame);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "WorldTypeMode", normalizeWorldType(worldTypeMode));
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "WorldSmallBiomes", worldSmallBiomes);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "WorldCustomBiomeSizeBlocks", clampWorldBiomeSize(worldCustomBiomeSizeBlocks));
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "WorldTerrainHillinessPercent", clampWorldTerrainHilliness(worldTerrainHillinessPercent));
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "WorldStructureFrequencyPercent", clampWorldStructureFrequency(worldStructureFrequencyPercent));
        com.jamie.jamiebingo.util.NbtUtil.putString(tag, "WorldSingleBiomeId", worldSingleBiomeId == null || worldSingleBiomeId.isBlank() ? "minecraft:plains" : worldSingleBiomeId);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "WorldSurfaceCaveBiomes", worldSurfaceCaveBiomes);
        com.jamie.jamiebingo.util.NbtUtil.putString(tag, "WorldSetSeedText", worldSetSeedText == null ? "" : worldSetSeedText);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "WorldFreshSeedPrepared", worldFreshSeedPrepared);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "PendingWeeklyChallengeStart", pendingWeeklyChallengeStart);
        com.jamie.jamiebingo.util.NbtUtil.putString(tag, "PendingWeeklyChallengeBaseSeed", String.valueOf(pendingWeeklyChallengeBaseSeed));
        com.jamie.jamiebingo.util.NbtUtil.putString(tag, "ActiveWeeklyChallengeId", activeWeeklyChallengeId == null ? "" : activeWeeklyChallengeId);
        com.jamie.jamiebingo.util.NbtUtil.putString(tag, "ActiveWeeklySettingsSeed", activeWeeklySettingsSeed == null ? "" : activeWeeklySettingsSeed);
        com.jamie.jamiebingo.util.NbtUtil.putString(tag, "ActiveWeeklyWorldSeed", activeWeeklyWorldSeed == null ? "" : activeWeeklyWorldSeed);
        com.jamie.jamiebingo.util.NbtUtil.putString(tag, "ActiveWeeklyCardSeed", activeWeeklyCardSeed == null ? "" : activeWeeklyCardSeed);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "Active", active);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "GameStartTick", gameStartTick);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "CountdownEndTick", countdownEndTick);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "CountdownExpired", countdownExpired);
        int savedTimerSeconds = -1;
        if (active && !startCountdownActive) {
            if (countdownEnabled && resumeCountdownRemainingSeconds >= 0) {
                savedTimerSeconds = Math.max(0, resumeCountdownRemainingSeconds);
            } else if (!countdownEnabled && resumeElapsedSeconds >= 0) {
                savedTimerSeconds = Math.max(0, resumeElapsedSeconds);
            }
        }
        MinecraftServer timerServer = ServerLifecycleHooks.getCurrentServer();
        int currentTickForTimer = timerServer == null ? -1 : com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(timerServer);
        if (active && !startCountdownActive && currentTickForTimer >= 0 && gameStartTick != -1) {
            if (countdownEnabled) {
                int endTick = countdownEndTick >= 0
                        ? countdownEndTick
                        : gameStartTick + Math.max(1, countdownMinutes) * 60 * 20;
                savedTimerSeconds = Math.max(0, (endTick - currentTickForTimer) / 20);
                resumeCountdownRemainingSeconds = savedTimerSeconds;
                resumeElapsedSeconds = -1;
            } else {
                savedTimerSeconds = Math.max(0, (currentTickForTimer - gameStartTick) / 20);
                resumeElapsedSeconds = savedTimerSeconds;
                resumeCountdownRemainingSeconds = -1;
            }
        }
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "ResumeElapsedSeconds", resumeElapsedSeconds);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "ResumeCountdownRemainingSeconds", resumeCountdownRemainingSeconds);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "SavedTimerSeconds", savedTimerSeconds);
        CompoundTag rushRemaining = new CompoundTag();
        int currentTick = currentTickForTimer;
        for (Map.Entry<UUID, Integer> entry : rushDeadlineTickByTeam.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            int remainingSeconds;
            if (currentTick >= 0) {
                remainingSeconds = Math.max(0, (entry.getValue() - currentTick) / 20);
            } else {
                remainingSeconds = Math.max(0, rushResumeSecondsByTeam.getOrDefault(entry.getKey(), -1));
            }
            if (remainingSeconds <= 0) continue;
            com.jamie.jamiebingo.util.NbtUtil.putInt(rushRemaining, entry.getKey().toString(), remainingSeconds);
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "RushRemainingByTeam", rushRemaining);
        ListTag mineSourceIdsTag = new ListTag();
        for (String id : mineResumeSourceIds) {
            if (id == null || id.isBlank()) continue;
            mineSourceIdsTag.add(StringTag.valueOf(id));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "MineResumeSourceIds", mineSourceIdsTag);
        CompoundTag mineRemainingTag = new CompoundTag();
        for (Map.Entry<UUID, Integer> entry : mineResumeRemainingSecondsByPlayer.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            int remaining = Math.max(0, entry.getValue());
            if (remaining <= 0) continue;
            com.jamie.jamiebingo.util.NbtUtil.putInt(mineRemainingTag, entry.getKey().toString(), remaining);
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "MineResumeRemainingByPlayer", mineRemainingTag);
        CompoundTag mineTriggeredTag = new CompoundTag();
        for (Map.Entry<UUID, String> entry : mineResumeTriggeredSourceByPlayer.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isBlank()) continue;
            com.jamie.jamiebingo.util.NbtUtil.putString(mineTriggeredTag, entry.getKey().toString(), entry.getValue());
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "MineResumeTriggeredByPlayer", mineTriggeredTag);
        CompoundTag mineDamageTag = new CompoundTag();
        for (Map.Entry<UUID, Integer> entry : mineResumeDamageProgressByPlayer.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            int progressValue = Math.max(0, entry.getValue());
            com.jamie.jamiebingo.util.NbtUtil.putInt(mineDamageTag, entry.getKey().toString(), progressValue);
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "MineResumeDamageByPlayer", mineDamageTag);
        com.jamie.jamiebingo.util.NbtUtil.putString(tag, "MineResumeDefuseQuestId", mineResumeDefuseQuestId == null ? "" : mineResumeDefuseQuestId);
        com.jamie.jamiebingo.util.NbtUtil.putString(tag, "MineResumeDefuseDisplayName", mineResumeDefuseDisplayName == null ? "" : mineResumeDefuseDisplayName);
        com.jamie.jamiebingo.util.NbtUtil.putString(tag, "PowerResumeSlotId", powerResumeSlotId == null ? "" : powerResumeSlotId);
        com.jamie.jamiebingo.util.NbtUtil.putString(tag, "PowerResumeDisplayName", powerResumeDisplayName == null ? "" : powerResumeDisplayName);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "PowerResumeRemainingSeconds", powerResumeRemainingSeconds);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "PowerResumeClaimed", powerResumeClaimed);
        com.jamie.jamiebingo.util.NbtUtil.putString(tag, "LastPlayedSeed", getLastPlayedSeed());
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "CurrentRunPreviewSize", currentRunPreviewSize);
        ListTag currentRunPreviewTag = new ListTag();
        for (String id : currentRunPreviewSlotIds) {
            currentRunPreviewTag.add(StringTag.valueOf(id == null ? "" : id));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "CurrentRunPreviewSlots", currentRunPreviewTag);
        if (currentCard != null) {
            com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "CurrentCard", saveCard(currentCard));
        }
        ListTag hangmanCardsTag = new ListTag();
        for (BingoCard card : hangmanCards) {
            if (card == null) continue;
            hangmanCardsTag.add(saveCard(card));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "HangmanCards", hangmanCardsTag);
        ListTag gunCardsTag = new ListTag();
        for (BingoCard card : gunGameCards) {
            if (card == null) continue;
            gunCardsTag.add(saveCard(card));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "GunGameCards", gunCardsTag);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "HangmanRoundIndex", hangmanRoundIndex);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "GunGameSharedIndex", gunGameSharedIndex);
        CompoundTag gunTeamIndex = new CompoundTag();
        for (Map.Entry<UUID, Integer> entry : gunGameTeamIndex.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            com.jamie.jamiebingo.util.NbtUtil.putInt(gunTeamIndex, entry.getKey().toString(), Math.max(0, entry.getValue()));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "GunGameTeamIndex", gunTeamIndex);
        ListTag progressTag = new ListTag();
        for (String id : progress) {
            if (id == null || id.isBlank()) continue;
            progressTag.add(StringTag.valueOf(id));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "Progress", progressTag);
        CompoundTag teamProgressTag = new CompoundTag();
        for (Map.Entry<UUID, Set<String>> entry : teamProgress.entrySet()) {
            if (entry.getKey() == null) continue;
            ListTag list = new ListTag();
            for (String id : entry.getValue()) {
                if (id == null || id.isBlank()) continue;
                list.add(StringTag.valueOf(id));
            }
            teamProgressTag.put(entry.getKey().toString(), list);
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "TeamProgress", teamProgressTag);
        CompoundTag slotOwnersTag = new CompoundTag();
        for (Map.Entry<String, Set<UUID>> entry : slotOwners.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) continue;
            ListTag list = new ListTag();
            for (UUID owner : entry.getValue()) {
                if (owner == null) continue;
                list.add(StringTag.valueOf(owner.toString()));
            }
            slotOwnersTag.put(entry.getKey(), list);
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "SlotOwners", slotOwnersTag);
        CompoundTag historyTag = new CompoundTag();
        for (Map.Entry<UUID, List<GameHistoryEntry>> entry : playerGameHistory.entrySet()) {
            UUID playerId = entry.getKey();
            if (playerId == null) continue;
            ListTag entriesTag = new ListTag();
            for (GameHistoryEntry historyEntry : entry.getValue()) {
                if (historyEntry == null) continue;
                entriesTag.add(historyEntry.save());
            }
            historyTag.put(playerId.toString(), entriesTag);
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "PlayerGameHistory", historyTag);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "Size", size);
        com.jamie.jamiebingo.util.NbtUtil.putString(tag, "Difficulty", difficulty);
        com.jamie.jamiebingo.util.NbtUtil.putString(tag, "Mode", mode.name());
        com.jamie.jamiebingo.util.NbtUtil.putString(tag, "Composition", composition.name());
        com.jamie.jamiebingo.util.NbtUtil.putString(tag, "WinCondition", winCondition.name());
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "QuestPercent", questPercent);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "CategoryLogicEnabled", categoryLogicEnabled);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "RarityLogicEnabled", rarityLogicEnabled);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "ItemColorVariantsSeparate", itemColorVariantsSeparate);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "RerollsPerPlayer", rerollsPerPlayer);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "CasinoMode", casinoMode);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "MinesEnabled", minesEnabled);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "MineAmount", mineAmount);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "MineTimeSeconds", mineTimeSeconds);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "PowerSlotEnabled", powerSlotEnabled);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "PowerSlotIntervalSeconds", powerSlotIntervalSeconds);
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "FakeRerollsEnabled", fakeRerollsEnabled);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "FakeRerollsPerPlayer", Math.max(1, Math.min(10, fakeRerollsPerPlayer)));
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "HideGoalDetailsInChat", hideGoalDetailsInChat);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "StarterKitMode", normalizeStarterKitMode(starterKitMode));
        com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "RandomEffectsArmed", randomEffectsArmed);
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "RandomEffectsIntervalSeconds", randomEffectsIntervalSeconds);
        com.jamie.jamiebingo.util.NbtUtil.putString(tag, "ActiveRandomEffectId", activeRandomEffectId);
com.jamie.jamiebingo.util.NbtUtil.putString(tag, "AppliedRandomEffectId", appliedRandomEffectId);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "RtpEnabled", rtpEnabled);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "HungerEnabled", hungerEnabled);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "NaturalRegenEnabled", naturalRegenEnabled);
com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "HostileMobsEnabled", hostileMobsEnabled);
com.jamie.jamiebingo.util.NbtUtil.putString(tag, "LastDifficulty", lastNonPeacefulDifficulty);


        CompoundTag revealed = new CompoundTag();
        for (var entry : revealedSlots.entrySet()) {
            ListTag list = new ListTag();
            for (String id : entry.getValue()) {
                list.add(StringTag.valueOf(id));
            }
            revealed.put(entry.getKey().toString(), list);
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "RevealedSlots", revealed);

        ListTag customSlots = new ListTag();
        for (String id : customCardSlots) {
            customSlots.add(StringTag.valueOf(id == null ? "" : id));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "CustomCardSlots", customSlots);

        ListTag customPool = new ListTag();
        for (String id : customPoolIds) {
            customPool.add(StringTag.valueOf(id == null ? "" : id));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "CustomPoolIds", customPool);

        ListTag customMines = new ListTag();
        for (String id : customMineIds) {
            if (id == null || id.isBlank()) continue;
            customMines.add(StringTag.valueOf(id));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "CustomMineIds", customMines);

        ListTag blacklisted = new ListTag();
        for (String id : blacklistedSlotIds) {
            if (id == null || id.isBlank()) continue;
            blacklisted.add(StringTag.valueOf(id));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "BlacklistedSlotIds", blacklisted);

        ListTag whitelisted = new ListTag();
        for (String id : whitelistedSlotIds) {
            if (id == null || id.isBlank()) continue;
            whitelisted.add(StringTag.valueOf(id));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "WhitelistedSlotIds", whitelisted);

        CompoundTag rarityOverridesTag = new CompoundTag();
        for (var entry : rarityOverrides.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) continue;
            String rarity = BingoRarityUtil.normalize(entry.getValue());
            if (!BingoRarityUtil.isKnown(rarity)) continue;
            rarityOverridesTag.putString(entry.getKey(), rarity);
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "RarityOverrides", rarityOverridesTag);

        CompoundTag highlighted = new CompoundTag();
        for (var entry : highlightedSlots.entrySet()) {
            ListTag list = new ListTag();
            for (String id : entry.getValue()) {
                list.add(StringTag.valueOf(id));
            }
            highlighted.put(entry.getKey().toString(), list);
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "HighlightedSlots", highlighted);

        ListTag participantsTag = new ListTag();
        for (UUID id : participants) {
            participantsTag.add(StringTag.valueOf(id.toString()));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "Participants", participantsTag);

        ListTag spectatorsTag = new ListTag();
        for (UUID id : spectators) {
            spectatorsTag.add(StringTag.valueOf(id.toString()));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "Spectators", spectatorsTag);

        CompoundTag views = new CompoundTag();
        for (Map.Entry<UUID, UUID> entry : spectatorViewTargets.entrySet()) {
            putUuid(views, entry.getKey().toString(), entry.getValue());
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "SpectatorViews", views);

        ListTag eliminatedPlayersTag = new ListTag();
        for (UUID id : eliminatedPlayers) {
            eliminatedPlayersTag.add(StringTag.valueOf(id.toString()));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "EliminatedPlayers", eliminatedPlayersTag);

        ListTag eliminatedTeamsTag = new ListTag();
        for (UUID id : eliminatedTeams) {
            eliminatedTeamsTag.add(StringTag.valueOf(id.toString()));
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "EliminatedTeams", eliminatedTeamsTag);

        return tag;
    }

    private static CompoundTag saveCard(BingoCard card) {
        CompoundTag tag = new CompoundTag();
        if (card == null || card.getSize() <= 0) {
            com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "Size", 0);
            return tag;
        }
        int size = card.getSize();
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "Size", size);
        ListTag slots = new ListTag();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BingoSlot slot = card.getSlot(x, y);
                CompoundTag slotTag = new CompoundTag();
                if (slot != null) {
                    com.jamie.jamiebingo.util.NbtUtil.putString(slotTag, "Id", slot.getId());
                    com.jamie.jamiebingo.util.NbtUtil.putString(slotTag, "Name", slot.getName());
                    com.jamie.jamiebingo.util.NbtUtil.putString(slotTag, "Category", slot.getCategory());
                    com.jamie.jamiebingo.util.NbtUtil.putString(slotTag, "Rarity", slot.getRarity());
                } else {
                    com.jamie.jamiebingo.util.NbtUtil.putString(slotTag, "Id", "");
                    com.jamie.jamiebingo.util.NbtUtil.putString(slotTag, "Name", "");
                    com.jamie.jamiebingo.util.NbtUtil.putString(slotTag, "Category", "");
                    com.jamie.jamiebingo.util.NbtUtil.putString(slotTag, "Rarity", "");
                }
                slots.add(slotTag);
            }
        }
        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "Slots", slots);
        return tag;
    }

    private static BingoCard loadCard(CompoundTag tag) {
        if (tag == null) return null;
        int size = getInt(tag, "Size", 0);
        if (size <= 0) return null;
        BingoCard card = new BingoCard(size);
        ListTag slots = tag.getListOrEmpty("Slots");
        int expected = size * size;
        for (int i = 0; i < expected && i < slots.size(); i++) {
            CompoundTag slotTag = slots.getCompoundOrEmpty(i);
            String id = getString(slotTag, "Id", "");
            String name = getString(slotTag, "Name", "");
            String category = getString(slotTag, "Category", "");
            String rarity = getString(slotTag, "Rarity", "");
            int x = i % size;
            int y = i / size;
            card.setSlot(x, y, new BingoSlot(id, name, category, rarity));
        }
        return card;
    }

    public static final class GameHistoryEntry {
        public final String cardSeed;
        public final String worldSeed;
        public final String settingsSeed;
        public final long durationSeconds;
        public final boolean completed;
        public final int previewSize;
        public final List<String> previewSlotIds;
        public final List<String> completedSlotIds;
        public final List<String> opponentCompletedSlotIds;
        public final int teamColorId;
        public final long finishedAtEpochSeconds;
        public final String mineDisplayName;
        public final List<String> settingsLines;
        public final int participantCount;
        public final boolean commandsUsed;
        public final boolean voteRerollUsed;
        public final int rerollsUsedCount;
        public final int fakeRerollsUsedCount;
        public final boolean weeklyChallenge;
        public final String weeklyChallengeId;
        public final String leaderboardCategory;
        public final String leaderboardCategoryReason;

        public GameHistoryEntry(
                String cardSeed,
                String worldSeed,
                String settingsSeed,
                long durationSeconds,
                boolean completed,
                int previewSize,
                List<String> previewSlotIds,
                List<String> completedSlotIds,
                List<String> opponentCompletedSlotIds,
                int teamColorId,
                long finishedAtEpochSeconds,
                String mineDisplayName,
                List<String> settingsLines,
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
            this.previewSize = Math.max(0, previewSize);
            this.previewSlotIds = previewSlotIds == null ? List.of() : new ArrayList<>(previewSlotIds);
            this.completedSlotIds = completedSlotIds == null ? List.of() : new ArrayList<>(completedSlotIds);
            this.opponentCompletedSlotIds = opponentCompletedSlotIds == null ? List.of() : new ArrayList<>(opponentCompletedSlotIds);
            this.teamColorId = Math.max(0, Math.min(15, teamColorId));
            this.finishedAtEpochSeconds = Math.max(0L, finishedAtEpochSeconds);
            this.mineDisplayName = mineDisplayName == null ? "" : mineDisplayName;
            this.settingsLines = settingsLines == null ? List.of() : new ArrayList<>(settingsLines);
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

        public GameHistoryEntry copy() {
            return new GameHistoryEntry(
                    cardSeed,
                    worldSeed,
                    settingsSeed,
                    durationSeconds,
                    completed,
                    previewSize,
                    previewSlotIds,
                    completedSlotIds,
                    opponentCompletedSlotIds,
                    teamColorId,
                    finishedAtEpochSeconds,
                    mineDisplayName,
                    settingsLines,
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

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            com.jamie.jamiebingo.util.NbtUtil.putString(tag, "CardSeed", cardSeed);
            com.jamie.jamiebingo.util.NbtUtil.putString(tag, "WorldSeed", worldSeed);
            com.jamie.jamiebingo.util.NbtUtil.putString(tag, "SettingsSeed", settingsSeed);
            com.jamie.jamiebingo.util.NbtUtil.putString(tag, "DurationSeconds", String.valueOf(durationSeconds));
            com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "Completed", completed);
            com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "PreviewSize", previewSize);
            ListTag preview = new ListTag();
            for (String id : previewSlotIds) {
                preview.add(StringTag.valueOf(id == null ? "" : id));
            }
            com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "PreviewSlots", preview);
            ListTag completedSlots = new ListTag();
            for (String id : completedSlotIds) {
                completedSlots.add(StringTag.valueOf(id == null ? "" : id));
            }
            com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "CompletedSlots", completedSlots);
            ListTag opponentCompletedSlots = new ListTag();
            for (String id : opponentCompletedSlotIds) {
                opponentCompletedSlots.add(StringTag.valueOf(id == null ? "" : id));
            }
            com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "OpponentCompletedSlots", opponentCompletedSlots);
            com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "TeamColorId", teamColorId);
            com.jamie.jamiebingo.util.NbtUtil.putString(tag, "FinishedAt", String.valueOf(finishedAtEpochSeconds));
            com.jamie.jamiebingo.util.NbtUtil.putString(tag, "MineDisplayName", mineDisplayName);
            ListTag settings = new ListTag();
            for (String line : settingsLines) {
                settings.add(StringTag.valueOf(line == null ? "" : line));
            }
            com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "SettingsLines", settings);
            com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "ParticipantCount", participantCount);
            com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "CommandsUsed", commandsUsed);
            com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "VoteRerollUsed", voteRerollUsed);
            com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "RerollsUsedCount", rerollsUsedCount);
            com.jamie.jamiebingo.util.NbtUtil.putInt(tag, "FakeRerollsUsedCount", fakeRerollsUsedCount);
            com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, "WeeklyChallenge", weeklyChallenge);
            com.jamie.jamiebingo.util.NbtUtil.putString(tag, "WeeklyChallengeId", weeklyChallengeId);
            com.jamie.jamiebingo.util.NbtUtil.putString(tag, "LeaderboardCategory", leaderboardCategory);
            com.jamie.jamiebingo.util.NbtUtil.putString(tag, "LeaderboardCategoryReason", leaderboardCategoryReason);
            return tag;
        }

        public static GameHistoryEntry load(CompoundTag tag) {
            if (tag == null) {
                return new GameHistoryEntry("", "", "", 0L, false, 0, List.of(), List.of(), List.of(), 10, 0L, "", List.of(), 0, false, false, 0, 0, false, "", "Custom", "");
            }
            List<String> previewSlots = new ArrayList<>();
            ListTag preview = tag.getListOrEmpty("PreviewSlots");
            for (int i = 0; i < preview.size(); i++) {
                previewSlots.add(preview.getStringOr(i, ""));
            }
            List<String> completedSlots = new ArrayList<>();
            ListTag completed = tag.getListOrEmpty("CompletedSlots");
            for (int i = 0; i < completed.size(); i++) {
                completedSlots.add(completed.getStringOr(i, ""));
            }
            List<String> opponentCompletedSlots = new ArrayList<>();
            ListTag opponentCompleted = tag.getListOrEmpty("OpponentCompletedSlots");
            for (int i = 0; i < opponentCompleted.size(); i++) {
                opponentCompletedSlots.add(opponentCompleted.getStringOr(i, ""));
            }
            List<String> settingsLines = new ArrayList<>();
            ListTag settings = tag.getListOrEmpty("SettingsLines");
            for (int i = 0; i < settings.size(); i++) {
                settingsLines.add(settings.getStringOr(i, ""));
            }
            return new GameHistoryEntry(
                    getString(tag, "CardSeed", ""),
                    getString(tag, "WorldSeed", ""),
                    getString(tag, "SettingsSeed", ""),
                    parseLongOrZero(getString(tag, "DurationSeconds", "0")),
                    getBoolean(tag, "Completed", false),
                    getInt(tag, "PreviewSize", 0),
                    previewSlots,
                    completedSlots,
                    opponentCompletedSlots,
                    getInt(tag, "TeamColorId", 10),
                    parseLongOrZero(getString(tag, "FinishedAt", "0")),
                    getString(tag, "MineDisplayName", ""),
                    settingsLines,
                    getInt(tag, "ParticipantCount", 0),
                    getBoolean(tag, "CommandsUsed", false),
                    getBoolean(tag, "VoteRerollUsed", false),
                    getInt(tag, "RerollsUsedCount", 0),
                    getInt(tag, "FakeRerollsUsedCount", 0),
                    getBoolean(tag, "WeeklyChallenge", false),
                    getString(tag, "WeeklyChallengeId", ""),
                    getString(tag, "LeaderboardCategory", "Custom"),
                    getString(tag, "LeaderboardCategoryReason", "")
            );
        }

        private static long parseLongOrZero(String value) {
            if (value == null || value.isBlank()) return 0L;
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
    }

    private static int getInt(CompoundTag tag, String key, int fallback) {
        return com.jamie.jamiebingo.util.NbtUtil.getInt(tag, key, fallback);
    }

    private static int normalizeWorldType(int mode) {
        int clamped = Math.max(WORLD_TYPE_NORMAL, Math.min(WORLD_TYPE_SMALL_BIOMES, mode));
        if (clamped == WORLD_TYPE_SMALL_BIOMES) {
            return WORLD_TYPE_CUSTOM_BIOME_SIZE;
        }
        return clamped;
    }

    public static int clampWorldBiomeSize(int blocks) {
        return Math.max(40, Math.min(100, blocks));
    }

    public static int clampWorldTerrainHilliness(int percent) {
        return Math.max(0, Math.min(100, percent));
    }

    public static int clampWorldStructureFrequency(int percent) {
        return Math.max(25, Math.min(500, percent));
    }

    public static int clampPrelitPortalsMode(int mode) {
        return Math.max(PRELIT_PORTALS_OFF, Math.min(PRELIT_PORTALS_BOTH, mode));
    }

    private static boolean getBoolean(CompoundTag tag, String key, boolean fallback) {
        return com.jamie.jamiebingo.util.NbtUtil.getBoolean(tag, key, fallback);
    }

    private static String getString(CompoundTag tag, String key, String fallback) {
        return com.jamie.jamiebingo.util.NbtUtil.getString(tag, key, fallback);
    }

    private static UUID getUuid(CompoundTag tag, String key) {
        String raw = com.jamie.jamiebingo.util.NbtUtil.getString(tag, key, null);
        if (raw != null && !raw.isBlank()) {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
            }
        }
        int[] ints = com.jamie.jamiebingo.util.NbtUtil.getIntArray(tag, key);
        if (ints != null && ints.length == 4) {
            return UUIDUtil.uuidFromIntArray(ints);
        }
        return null;
    }

    private static void putUuid(CompoundTag tag, String key, UUID value) {
        if (value == null) return;
        tag.putIntArray(key, UUIDUtil.uuidToIntArray(value));
    }

/* =========================
   ðŸŽ² WEIGHTED RANDOM HELPER
   ========================= */

private static int weightedPick(int[] values, double[] weights, Random rng) {
    double total = 0;
    for (double w : weights) total += w;

    double r = rng.nextDouble() * total;
    double upto = 0;

    for (int i = 0; i < values.length; i++) {
        upto += weights[i];
        if (upto >= r) return values[i];
    }
    return values[0];
}


    public static BingoGameData get(ServerLevel level) {
        net.minecraft.world.level.storage.DimensionDataStorage storage =
                com.jamie.jamiebingo.util.LevelDataStorageUtil.getDataStorage(level);
        if (storage == null) {
            throw new IllegalStateException("Unable to resolve DimensionDataStorage");
        }
        BingoGameData data = com.jamie.jamiebingo.util.SavedDataUtil.computeIfAbsent(storage, TYPE);
        if (data == null) {
            // Fallback: create and register the data if computeIfAbsent path is not available.
            data = new BingoGameData();
            if (!com.jamie.jamiebingo.util.SavedDataUtil.set(storage, TYPE, data)) {
                throw new IllegalStateException("Unable to load BingoGameData from DimensionDataStorage");
            }
        }
        return data;
    }

    public static BingoGameData get(MinecraftServer server) {
        ServerLevel overworld = com.jamie.jamiebingo.util.ServerLevelUtil.getOverworld(server);
        if (overworld == null) {
            // Fallback: any available level, or a player's current level
            overworld = com.jamie.jamiebingo.util.ServerLevelUtil.getAnyLevel(server);
            if (overworld == null) {
                java.util.List<net.minecraft.server.level.ServerPlayer> players =
                        com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server);
                if (!players.isEmpty()) {
                    overworld = com.jamie.jamiebingo.util.ServerPlayerUtil.getLevel(players.get(0));
                }
            }
        }
        if (overworld == null) {
            // Last resort: return a stable per-server fallback (never a fresh per-call instance).
            return FALLBACK_BY_SERVER.computeIfAbsent(server, s -> new BingoGameData());
        }
        return get(overworld);
}
}





































