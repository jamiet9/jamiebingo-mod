package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.bingo.BingoLineType;
import com.jamie.jamiebingo.client.ClientWinningLineData;
import com.jamie.jamiebingo.util.FriendlyByteBufUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PacketSyncWinningLine {

    private final BingoLineType type;
    private final int index;
    private final DyeColor color;
    private final int cardSize;

    public PacketSyncWinningLine(
            BingoLineType type,
            int index,
            DyeColor color,
            int cardSize
    ) {
        this.type = type;
        this.index = index;
        this.color = color;
        this.cardSize = cardSize;
    }

    public static void encode(PacketSyncWinningLine msg, FriendlyByteBuf buf) {
        FriendlyByteBufUtil.writeEnum(buf, msg.type);
        buf.writeInt(msg.index);
        FriendlyByteBufUtil.writeEnum(buf, msg.color);
        buf.writeInt(msg.cardSize);
    }

    public static PacketSyncWinningLine decode(FriendlyByteBuf buf) {
        return new PacketSyncWinningLine(
                FriendlyByteBufUtil.readEnum(buf, BingoLineType.class),
                buf.readInt(),
                FriendlyByteBufUtil.readEnum(buf, DyeColor.class),
                buf.readInt()
        );
    }

    public static void handle(PacketSyncWinningLine msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() ->
                ClientWinningLineData.set(
                        msg.type,
                        msg.index,
                        msg.color,
                        msg.cardSize
                )
        );
        ctx.setPacketHandled(true);
    }
}
