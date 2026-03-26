package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.ClientFlashSlots;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.HashSet;
import java.util.Set;

public class PacketFlashSlots {
    public static final int STYLE_PULSE = 0;
    public static final int STYLE_SHAKE = 1;

    private final Set<String> slots;
    private final int durationTicks;
    private final int style;

    public PacketFlashSlots(Set<String> slots, int durationTicks) {
        this(slots, durationTicks, STYLE_PULSE);
    }

    public PacketFlashSlots(Set<String> slots, int durationTicks, int style) {
        this.slots = slots == null ? Set.of() : new HashSet<>(slots);
        this.durationTicks = Math.max(1, durationTicks);
        this.style = style == STYLE_SHAKE ? STYLE_SHAKE : STYLE_PULSE;
    }

    public static void encode(PacketFlashSlots msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.durationTicks);
        buf.writeInt(msg.style);
        buf.writeInt(msg.slots.size());
        for (String id : msg.slots) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, id);
        }
    }

    public static PacketFlashSlots decode(FriendlyByteBuf buf) {
        int duration = buf.readInt();
        int style = buf.readInt();
        int count = buf.readInt();
        Set<String> slots = new HashSet<>();
        for (int i = 0; i < count; i++) {
            slots.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
        }
        return new PacketFlashSlots(slots, duration, style);
    }

    public static void handle(PacketFlashSlots msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientFlashSlots.flash(msg.slots, msg.durationTicks, msg.style)
        ));
        ctx.setPacketHandled(true);
    }
}

