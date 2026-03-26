package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.bingo.WinCondition;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.data.BroadcastHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class ControllerCycleModePacket {

    private static final WinCondition[] MODES = {
            WinCondition.FULL,
            WinCondition.LINE,
            WinCondition.LOCKOUT,
            WinCondition.RARITY,
            WinCondition.BLIND,
            WinCondition.GUNGAME,
            WinCondition.GAMEGUN
    };

    public ControllerCycleModePacket() {}

    public static void encode(ControllerCycleModePacket pkt, FriendlyByteBuf buf) {}

    public static ControllerCycleModePacket decode(FriendlyByteBuf buf) {
        return new ControllerCycleModePacket();
    }

    public static void handle(ControllerCycleModePacket pkt,
                              CustomPayloadEvent.Context ctx) {

        ctx.enqueueWork(() -> {
            BingoGameData data = BingoGameData.get(
                    com.jamie.jamiebingo.util.EntityServerUtil.getServer(ctx.getSender())
            );

            if (data.randomWinConditionIntent) {
                data.randomWinConditionIntent = false;
                data.winCondition = MODES[0];
            } else {
                int i = 0;
                for (; i < MODES.length; i++) {
                    if (MODES[i] == data.winCondition) break;
                }
                i++;
                if (i >= MODES.length) {
                    data.randomWinConditionIntent = true;
                } else {
                    data.winCondition = MODES[i];
                }
            }

            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
            BroadcastHelper.broadcastFullSync();
        });

        ctx.setPacketHandled(true);
    }
}

