package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.bingo.BingoCard;
import com.jamie.jamiebingo.bingo.BingoSeedHelper;
import com.jamie.jamiebingo.bingo.CardSeedCodec;
import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Supplier;

public class PacketSeedPreviewRequest {

    private final String seed;

    public PacketSeedPreviewRequest(String seed) {
        this.seed = seed == null ? "" : seed;
    }

    public static void encode(PacketSeedPreviewRequest msg, FriendlyByteBuf buf) {
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.seed);
    }

    public static PacketSeedPreviewRequest decode(FriendlyByteBuf buf) {
        return new PacketSeedPreviewRequest(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
    }

    public static void handle(PacketSeedPreviewRequest msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            CardSeedCodec.SeedData seedData = CardSeedCodec.decode(msg.seed);
            if (seedData == null || seedData.cards().isEmpty()) {
                PacketSeedPreviewResponse resp = new PacketSeedPreviewResponse(null, List.of(), "Invalid seed.", false);
                com.jamie.jamiebingo.network.NetworkHandler.send(
                        resp,
                        PacketDistributor.PLAYER.with(player)
                );
                return;
            }

            BingoCard card;
            boolean customPool = seedData.settings().contains("CustomPoolEnabled")
                    && seedData.settings().getBoolean("CustomPoolEnabled").orElse(false);
            if (customPool) {
                BingoGameData temp = new BingoGameData();
                temp.customPoolEnabled = true;
                temp.customPoolIds.clear();
                var list = seedData.settings().getList("CustomPoolIds").orElse(null);
                if (list != null) {
                    for (int i = 0; i < list.size(); i++) {
                        String id = list.getStringOr(i, "");
                        if (!id.isBlank()) {
                            temp.customPoolIds.add(id);
                        }
                    }
                }
                int size = seedData.settings().getInt("Size").orElse(0);
                card = temp.buildCardFromPool(size, new java.util.Random());
                if (card == null && !seedData.cards().isEmpty()) {
                    card = seedData.cards().get(0);
                }
            } else {
                card = seedData.cards().get(0);
            }
            List<String> settings = BingoSeedHelper.buildSettingsLinesFromSeed(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player), seedData);
            boolean hidePreviewSlots = BingoSeedHelper.shouldMaskPreview(seedData);

            PacketSeedPreviewResponse resp = new PacketSeedPreviewResponse(card, settings, "", hidePreviewSlots);
            com.jamie.jamiebingo.network.NetworkHandler.send(
                    resp,
                    PacketDistributor.PLAYER.with(player)
            );
        });
        ctx.setPacketHandled(true);
    }
}



