package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.bingo.CardSeedCodec;
import com.jamie.jamiebingo.bingo.WinCondition;
import com.jamie.jamiebingo.bingo.CardComposition;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketImportCustomSeed {

    private final String seed;

    public PacketImportCustomSeed(String seed) {
        this.seed = seed == null ? "" : seed;
    }

    public static void encode(PacketImportCustomSeed msg, FriendlyByteBuf buf) {
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.seed);
    }

    public static PacketImportCustomSeed decode(FriendlyByteBuf buf) {
        return new PacketImportCustomSeed(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
    }

    public static void handle(PacketImportCustomSeed msg, CustomPayloadEvent.Context ctx) {
        CustomPayloadEvent.Context context = ctx;
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);

            CardSeedCodec.SeedData seedData = CardSeedCodec.decode(msg.seed);
            if (seedData == null) {
                player.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Invalid seed."));
                return;
            }

            var settings = seedData.settings();
            if (settings == null) {
                player.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Invalid seed."));
                return;
            }

            String winKey = settings.getString("Win").orElse(null);
            String compositionKey = settings.getString("Composition").orElse(null);
            if (winKey == null || compositionKey == null) {
                player.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Invalid seed."));
                return;
            }
            WinCondition win;
            CardComposition composition;
            try {
                win = WinCondition.valueOf(winKey);
                composition = CardComposition.valueOf(compositionKey);
            } catch (Exception e) {
                player.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Invalid seed."));
                return;
            }

            boolean customCardEnabled = settings.contains("CustomCardEnabled")
                    && settings.getBoolean("CustomCardEnabled").orElse(false);
            boolean customPoolEnabled = settings.contains("CustomPoolEnabled")
                    && settings.getBoolean("CustomPoolEnabled").orElse(false);
            if (!customPoolEnabled) {
                customCardEnabled = true;
            }

            List<String> poolIds = new ArrayList<>();
            if (settings.contains("CustomPoolIds")) {
                var list = settings.getList("CustomPoolIds").orElse(null);
                if (list != null) {
                    for (int i = 0; i < list.size(); i++) {
                        String id = list.getStringOr(i, "");
                        if (!id.isBlank()) {
                            poolIds.add(id);
                        }
                    }
                }
            }

            List<String> cardSlots = new ArrayList<>();
            if (!seedData.cards().isEmpty()) {
                var card = seedData.cards().get(0);
                if (card != null) {
                    for (int y = 0; y < card.getSize(); y++) {
                        for (int x = 0; x < card.getSize(); x++) {
                            var slot = card.getSlot(x, y);
                            cardSlots.add(slot != null ? slot.getId() : "");
                        }
                    }
                }
            }

            BingoGameData data = BingoGameData.get(server);
            data.customCardEnabled = customCardEnabled;
            data.customPoolEnabled = customPoolEnabled;
            data.customCardSlots.clear();
            data.customCardSlots.addAll(cardSlots);
            data.customPoolIds.clear();
            data.customPoolIds.addAll(poolIds);
            data.customMineIds.clear();
            if (settings.contains("CustomMineIds")) {
                var mineList = settings.getListOrEmpty("CustomMineIds");
                for (int i = 0; i < mineList.size(); i++) {
                    String id = mineList.getStringOr(i, "");
                    if (!id.isBlank()) {
                        data.customMineIds.add(id);
                    }
                }
            }
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

            PacketOpenCustomCardMaker packet = new PacketOpenCustomCardMaker(
                    win,
                    composition,
                    settings.getInt("QuestPercent").orElse(0),
                    settings.getBoolean("CategoryLogicEnabled").orElse(true),
                    settings.getBoolean("RarityLogicEnabled").orElse(true),
                    settings.getBoolean("ItemColorVariantsSeparate").orElse(true),
                    settings.getBoolean("Casino").orElse(false),
                    settings.getInt("CasinoMode").orElse(settings.getBoolean("Casino").orElse(false) ? 1 : 0),
                    settings.getInt("Rerolls").orElse(0) > 0 ? 1 : 0,
                    settings.getInt("Rerolls").orElse(0),
                    settings.getInt("GunRounds").orElse(0),
                    settings.getInt("HangmanRounds").orElse(0),
                    settings.getInt("HangmanBaseSeconds").orElse(0),
                    settings.getInt("HangmanPenaltySeconds").orElse(0),
                    settings.getString("Difficulty").orElse(""),
                    false,
                    settings.getString("GameDifficulty").orElse(""),
                    false,
                    settings.getInt("EffectsInterval").orElse(0),
                    false,
                    settings.getBoolean("Rtp").orElse(false),
                    false,
                    settings.getBoolean("HostileMobs").orElse(false),
                    false,
                    settings.getBoolean("Hunger").orElse(false),
                    false,
                    settings.getInt("Size").orElse(0),
                    false,
                    settings.getBoolean("KeepInv").orElse(false),
                    false,
                    settings.getBoolean("Hardcore").orElse(false),
                    false,
                    settings.getInt("Daylight").orElse(0),
                    false,
                    settings.getInt("StartDelay").orElse(0),
                    settings.getBoolean("CountdownEnabled").orElse(false),
                    settings.getInt("CountdownMinutes").orElse(0),
                    settings.getBoolean("RushEnabled").orElse(false),
                    settings.getInt("RushSeconds").orElse(90),
                    settings.getBoolean("AllowLateJoin").orElse(false),
                    settings.getBoolean("Pvp").orElse(false),
                    settings.getBoolean("AdventureMode").orElse(false),
                    settings.getInt("PrelitPortalsMode").orElse(BingoGameData.PRELIT_PORTALS_OFF),
                    false,
                    settings.getInt("RegisterMode").orElse(0),
                    settings.getBoolean("TeamSyncEnabled").orElse(false),
                    settings.getBoolean("ShuffleEnabled").orElse(false) ? BingoGameData.SHUFFLE_ENABLED : BingoGameData.SHUFFLE_DISABLED,
                    settings.getBoolean("MinesEnabled").orElse(false),
                    settings.getInt("MineAmount").orElse(1),
                    settings.getInt("MineTimeSeconds").orElse(15),
                    customCardEnabled,
                    customPoolEnabled,
                    cardSlots,
                    poolIds,
                    new java.util.ArrayList<>(data.customMineIds)
            );

            NetworkHandler.send(packet, PacketDistributor.PLAYER.with(player));
        });
        context.setPacketHandled(true);
    }
}




