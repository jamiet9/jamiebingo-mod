package com.jamie.jamiebingo.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

public class PacketOpenWorldSettings {
    private final boolean newSeedEachGame;
    private final int worldTypeMode;
    private final int worldCustomBiomeSizeBlocks;
    private final int worldTerrainHillinessPercent;
    private final int worldStructureFrequencyPercent;
    private final String singleBiomeId;
    private final boolean surfaceCaveBiomes;
    private final String setSeedText;
    private final boolean adventureMode;
    private final int prelitPortalsMode;

    public PacketOpenWorldSettings(boolean newSeedEachGame, int worldTypeMode, int worldCustomBiomeSizeBlocks, int worldTerrainHillinessPercent, int worldStructureFrequencyPercent, String singleBiomeId, boolean surfaceCaveBiomes, String setSeedText, boolean adventureMode, int prelitPortalsMode) {
        this.newSeedEachGame = newSeedEachGame;
        this.worldTypeMode = worldTypeMode;
        this.worldCustomBiomeSizeBlocks = worldCustomBiomeSizeBlocks;
        this.worldTerrainHillinessPercent = worldTerrainHillinessPercent;
        this.worldStructureFrequencyPercent = worldStructureFrequencyPercent;
        this.singleBiomeId = singleBiomeId == null ? "minecraft:plains" : singleBiomeId;
        this.surfaceCaveBiomes = surfaceCaveBiomes;
        this.setSeedText = setSeedText == null ? "" : setSeedText;
        this.adventureMode = adventureMode;
        this.prelitPortalsMode = prelitPortalsMode;
    }

    public static void encode(PacketOpenWorldSettings msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.newSeedEachGame);
        buf.writeInt(msg.worldTypeMode);
        buf.writeInt(msg.worldCustomBiomeSizeBlocks);
        buf.writeInt(msg.worldTerrainHillinessPercent);
        buf.writeInt(msg.worldStructureFrequencyPercent);
        buf.writeUtf(msg.singleBiomeId);
        buf.writeBoolean(msg.surfaceCaveBiomes);
        buf.writeUtf(msg.setSeedText);
        buf.writeBoolean(msg.adventureMode);
        buf.writeInt(msg.prelitPortalsMode);
    }

    public static PacketOpenWorldSettings decode(FriendlyByteBuf buf) {
        return new PacketOpenWorldSettings(
                buf.readBoolean(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readUtf(256),
                buf.readBoolean(),
                buf.readUtf(256),
                buf.readBoolean(),
                buf.readInt()
        );
    }

    public static void handle(PacketOpenWorldSettings msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> com.jamie.jamiebingo.client.ClientPacketHandlers.handleOpenWorldSettings(
                        msg.newSeedEachGame,
                        msg.worldTypeMode,
                        msg.worldCustomBiomeSizeBlocks,
                        msg.worldTerrainHillinessPercent,
                        msg.worldStructureFrequencyPercent,
                        msg.singleBiomeId,
                        msg.surfaceCaveBiomes,
                        msg.setSeedText,
                        msg.adventureMode,
                        msg.prelitPortalsMode
                )
        ));
        ctx.setPacketHandled(true);
    }
}
