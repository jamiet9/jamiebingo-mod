package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketSaveCustomCardState {

    private final boolean customCardEnabled;
    private final boolean customPoolEnabled;
    private final List<String> cardSlots;
    private final List<String> poolIds;
    private final List<String> mineIds;

    public PacketSaveCustomCardState(
            boolean customCardEnabled,
            boolean customPoolEnabled,
            List<String> cardSlots,
            List<String> poolIds,
            List<String> mineIds
    ) {
        this.customCardEnabled = customCardEnabled;
        this.customPoolEnabled = customPoolEnabled;
        this.cardSlots = cardSlots == null ? List.of() : new ArrayList<>(cardSlots);
        this.poolIds = poolIds == null ? List.of() : new ArrayList<>(poolIds);
        this.mineIds = mineIds == null ? List.of() : new ArrayList<>(mineIds);
    }

    public static void encode(PacketSaveCustomCardState msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.customCardEnabled);
        buf.writeBoolean(msg.customPoolEnabled);
        buf.writeInt(msg.cardSlots.size());
        for (String id : msg.cardSlots) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, id == null ? "" : id);
        }
        buf.writeInt(msg.poolIds.size());
        for (String id : msg.poolIds) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, id == null ? "" : id);
        }
        buf.writeInt(msg.mineIds.size());
        for (String id : msg.mineIds) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, id == null ? "" : id);
        }
    }

    public static PacketSaveCustomCardState decode(FriendlyByteBuf buf) {
        boolean customCardEnabled = buf.readBoolean();
        boolean customPoolEnabled = buf.readBoolean();
        int cardSlotsSize = buf.readInt();
        List<String> cardSlots = new ArrayList<>();
        for (int i = 0; i < cardSlotsSize; i++) {
            cardSlots.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
        }
        int poolSize = buf.readInt();
        List<String> poolIds = new ArrayList<>();
        for (int i = 0; i < poolSize; i++) {
            poolIds.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
        }
        int mineSize = buf.readInt();
        List<String> mineIds = new ArrayList<>();
        for (int i = 0; i < mineSize; i++) {
            mineIds.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
        }
        return new PacketSaveCustomCardState(customCardEnabled, customPoolEnabled, cardSlots, poolIds, mineIds);
    }

    public static void handle(PacketSaveCustomCardState msg, CustomPayloadEvent.Context ctx) {
        CustomPayloadEvent.Context context = ctx;
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
            data.customCardEnabled = msg.customCardEnabled;
            data.customPoolEnabled = msg.customPoolEnabled;

            data.customCardSlots.clear();
            data.customCardSlots.addAll(msg.cardSlots);

            data.customPoolIds.clear();
            data.customPoolIds.addAll(msg.poolIds);
            data.customMineIds.clear();
            data.customMineIds.addAll(msg.mineIds);

            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        });
        context.setPacketHandled(true);
    }
}



