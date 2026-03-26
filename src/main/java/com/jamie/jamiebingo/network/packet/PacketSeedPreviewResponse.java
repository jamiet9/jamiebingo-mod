package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.bingo.BingoCard;
import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.client.SeedPreviewState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketSeedPreviewResponse {

    private final BingoCard card;
    private final List<String> settings;
    private final String error;
    private final boolean hidePreviewSlots;

    public PacketSeedPreviewResponse(BingoCard card, List<String> settings, String error, boolean hidePreviewSlots) {
        this.card = card;
        this.settings = settings == null ? List.of() : new ArrayList<>(settings);
        this.error = error == null ? "" : error;
        this.hidePreviewSlots = hidePreviewSlots;
    }

    public static void encode(PacketSeedPreviewResponse msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.card != null);
        if (msg.card != null) {
            buf.writeInt(msg.card.getSize());
            for (int y = 0; y < msg.card.getSize(); y++) {
                for (int x = 0; x < msg.card.getSize(); x++) {
                    BingoSlot slot = msg.card.getSlot(x, y);
                    if (slot == null) {
                        buf.writeBoolean(false);
                        continue;
                    }
                    buf.writeBoolean(true);
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, slot.getId());
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, slot.getName());
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, slot.getCategory());
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, slot.getRarity());
                }
            }
        }
        buf.writeInt(msg.settings.size());
        for (String line : msg.settings) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, line);
        }
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.error);
        buf.writeBoolean(msg.hidePreviewSlots);
    }

    public static PacketSeedPreviewResponse decode(FriendlyByteBuf buf) {
        BingoCard card = null;
        if (buf.readBoolean()) {
            int size = buf.readInt();
            card = new BingoCard(size);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    boolean hasSlot = buf.readBoolean();
                    if (!hasSlot) continue;
                    BingoSlot slot = new BingoSlot(
                            com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                            com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                            com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                            com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767)
                    );
                    card.setSlot(x, y, slot);
                }
            }
        }
        int settingsCount = buf.readInt();
        List<String> settings = new ArrayList<>();
        for (int i = 0; i < settingsCount; i++) {
            settings.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
        }
        String error = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
        boolean hidePreviewSlots = buf.readBoolean();
        return new PacketSeedPreviewResponse(card, settings, error, hidePreviewSlots);
    }

    public static void handle(PacketSeedPreviewResponse msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> SeedPreviewState.update(msg.card, msg.settings, msg.error, msg.hidePreviewSlots)
        ));
        ctx.setPacketHandled(true);
    }
}

