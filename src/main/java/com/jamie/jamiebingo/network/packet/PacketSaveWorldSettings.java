package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.menu.PlayerControllerSettingsStore;
import com.jamie.jamiebingo.world.LobbyWorldManager;
import com.jamie.jamiebingo.world.PregameSettingsWallManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class PacketSaveWorldSettings {
    private final boolean newSeedEachGame;
    private final int worldTypeMode;
    private final int worldCustomBiomeSizeBlocks;
    private final int worldTerrainHillinessPercent;
    private final int worldStructureFrequencyPercent;
    private final String singleBiomeId;
    private final boolean surfaceCaveBiomes;
    private final String setSeedText;
    private final boolean generateNow;
    private final boolean adventureMode;
    private final int prelitPortalsMode;

    public PacketSaveWorldSettings(boolean newSeedEachGame, int worldTypeMode, int worldCustomBiomeSizeBlocks, int worldTerrainHillinessPercent, int worldStructureFrequencyPercent, String singleBiomeId, boolean surfaceCaveBiomes, String setSeedText, boolean generateNow, boolean adventureMode, int prelitPortalsMode) {
        this.newSeedEachGame = newSeedEachGame;
        this.worldTypeMode = worldTypeMode;
        this.worldCustomBiomeSizeBlocks = worldCustomBiomeSizeBlocks;
        this.worldTerrainHillinessPercent = worldTerrainHillinessPercent;
        this.worldStructureFrequencyPercent = worldStructureFrequencyPercent;
        this.singleBiomeId = singleBiomeId == null ? "minecraft:plains" : singleBiomeId;
        this.surfaceCaveBiomes = surfaceCaveBiomes;
        this.setSeedText = setSeedText == null ? "" : setSeedText;
        this.generateNow = generateNow;
        this.adventureMode = adventureMode;
        this.prelitPortalsMode = BingoGameData.clampPrelitPortalsMode(prelitPortalsMode);
    }

    public static void encode(PacketSaveWorldSettings msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.newSeedEachGame);
        buf.writeInt(msg.worldTypeMode);
        buf.writeInt(msg.worldCustomBiomeSizeBlocks);
        buf.writeInt(msg.worldTerrainHillinessPercent);
        buf.writeInt(msg.worldStructureFrequencyPercent);
        buf.writeUtf(msg.singleBiomeId);
        buf.writeBoolean(msg.surfaceCaveBiomes);
        buf.writeUtf(msg.setSeedText);
        buf.writeBoolean(msg.generateNow);
        buf.writeBoolean(msg.adventureMode);
        buf.writeInt(msg.prelitPortalsMode);
    }

    public static PacketSaveWorldSettings decode(FriendlyByteBuf buf) {
        return new PacketSaveWorldSettings(
                buf.readBoolean(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readUtf(256),
                buf.readBoolean(),
                buf.readUtf(256),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readInt()
        );
    }

    public static void handle(PacketSaveWorldSettings msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(sender);
            if (server == null) return;

            BingoGameData data = BingoGameData.get(server);
            if (data.isActive() || !data.pregameBoxActive) return;

            boolean oldNewSeed = data.worldUseNewSeedEachGame;
            int oldType = data.worldTypeMode;
            int oldBiomeSize = data.worldCustomBiomeSizeBlocks;
            int oldHilliness = data.worldTerrainHillinessPercent;
            int oldStructureFrequency = data.worldStructureFrequencyPercent;
            String oldSingleBiome = data.worldSingleBiomeId == null ? "minecraft:plains" : data.worldSingleBiomeId;
            boolean oldSurfaceCave = data.worldSurfaceCaveBiomes;
            String oldSetSeed = data.worldSetSeedText == null ? "" : data.worldSetSeedText;
            int oldPrelitPortalsMode = BingoGameData.clampPrelitPortalsMode(data.prelitPortalsMode);
            data.adventureMode = msg.adventureMode;

            String seedText = msg.setSeedText == null ? "" : msg.setSeedText.trim();
            var parsed = com.jamie.jamiebingo.world.WorldRegenerationManager.decodeSettingsSeed(seedText);
            boolean forceGenerate = msg.generateNow;
            data.worldUseNewSeedEachGame = forceGenerate ? true : msg.newSeedEachGame;
            if (data.worldUseNewSeedEachGame) {
                if (parsed != null) {
                    data.worldUseNewSeedEachGame = parsed.newSeedEachGame();
                    data.worldTypeMode = parsed.worldTypeMode() == BingoGameData.WORLD_TYPE_SMALL_BIOMES
                            ? BingoGameData.WORLD_TYPE_CUSTOM_BIOME_SIZE
                            : Math.max(BingoGameData.WORLD_TYPE_NORMAL, Math.min(BingoGameData.WORLD_TYPE_SINGLE_BIOME, parsed.worldTypeMode()));
                    data.worldSmallBiomes = false;
                    data.worldCustomBiomeSizeBlocks = BingoGameData.clampWorldBiomeSize(parsed.biomeSizeBlocks());
                    data.worldTerrainHillinessPercent = BingoGameData.clampWorldTerrainHilliness(parsed.terrainHillinessPercent());
                    data.worldStructureFrequencyPercent = BingoGameData.clampWorldStructureFrequency(parsed.structureFrequencyPercent());
                    data.worldSurfaceCaveBiomes = parsed.surfaceCaveBiomes();
                    data.worldSingleBiomeId = parsed.singleBiomeId() == null || parsed.singleBiomeId().isBlank()
                            ? "minecraft:plains"
                            : parsed.singleBiomeId();
                    data.worldSetSeedText = seedText;
                    data.adventureMode = parsed.adventureMode();
                    data.prelitPortalsMode = BingoGameData.clampPrelitPortalsMode(parsed.prelitPortalsMode());
                } else {
                    data.worldTypeMode = msg.worldTypeMode == BingoGameData.WORLD_TYPE_SMALL_BIOMES
                            ? BingoGameData.WORLD_TYPE_CUSTOM_BIOME_SIZE
                            : Math.max(BingoGameData.WORLD_TYPE_NORMAL, Math.min(BingoGameData.WORLD_TYPE_SINGLE_BIOME, msg.worldTypeMode));
                    data.worldSmallBiomes = false;
                    data.worldCustomBiomeSizeBlocks = BingoGameData.clampWorldBiomeSize(msg.worldCustomBiomeSizeBlocks);
                    data.worldTerrainHillinessPercent = BingoGameData.clampWorldTerrainHilliness(msg.worldTerrainHillinessPercent);
                    data.worldStructureFrequencyPercent = BingoGameData.clampWorldStructureFrequency(msg.worldStructureFrequencyPercent);
                    if (data.worldTypeMode == BingoGameData.WORLD_TYPE_SINGLE_BIOME) {
                        data.worldSingleBiomeId = (msg.singleBiomeId == null || msg.singleBiomeId.isBlank())
                                ? "minecraft:plains"
                                : msg.singleBiomeId;
                    } else if (data.worldSingleBiomeId == null || data.worldSingleBiomeId.isBlank()) {
                        data.worldSingleBiomeId = "minecraft:plains";
                    }
                    data.worldSurfaceCaveBiomes = msg.surfaceCaveBiomes;
                    data.worldSetSeedText = seedText;
                }
            } else {
                data.worldSmallBiomes = false;
                data.worldSurfaceCaveBiomes = false;
                data.prelitPortalsMode = BingoGameData.PRELIT_PORTALS_OFF;
            }
            if (data.worldUseNewSeedEachGame && parsed == null) {
                data.prelitPortalsMode = BingoGameData.clampPrelitPortalsMode(msg.prelitPortalsMode);
            }

            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
            PlayerControllerSettingsStore.clear(sender);
            PregameSettingsWallManager.refreshFromData(server);
            boolean typeChanged = oldType != data.worldTypeMode;
            boolean biomeSizeChanged = oldBiomeSize != data.worldCustomBiomeSizeBlocks;
            boolean hillinessChanged = oldHilliness != data.worldTerrainHillinessPercent;
            boolean structureFrequencyChanged = oldStructureFrequency != data.worldStructureFrequencyPercent;
            boolean biomeChanged = data.worldTypeMode == BingoGameData.WORLD_TYPE_SINGLE_BIOME
                    && !oldSingleBiome.equals(data.worldSingleBiomeId == null ? "minecraft:plains" : data.worldSingleBiomeId);
            boolean caveChanged = oldSurfaceCave != data.worldSurfaceCaveBiomes;
            boolean seedChanged = !oldSetSeed.equals(data.worldSetSeedText == null ? "" : data.worldSetSeedText);
            boolean prelitChanged = oldPrelitPortalsMode != data.prelitPortalsMode;
            boolean modeEnabledNow = !oldNewSeed && data.worldUseNewSeedEachGame;
            boolean needsRegen = data.worldUseNewSeedEachGame && (modeEnabledNow || typeChanged || biomeSizeChanged || hillinessChanged || structureFrequencyChanged || biomeChanged || caveChanged || seedChanged || prelitChanged);
            if (msg.generateNow) {
                com.jamie.jamiebingo.world.WorldRegenerationManager.queueRegeneration(server, "force_seed");
            } else if (needsRegen) {
                com.jamie.jamiebingo.world.WorldRegenerationManager.queueRegeneration(server, "settings_changed");
            } else if (!com.jamie.jamiebingo.world.WorldRegenerationManager.ensureFreshSeedPrepared(server, data, "settings_idle_prepare")) {
                LobbyWorldManager.startPreloadingGameStartSpawn(server, data, true);
            }
        });
        ctx.setPacketHandled(true);
    }
}
