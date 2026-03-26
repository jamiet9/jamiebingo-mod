package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;

public class PacketCasinoDraftChoices {

    public record Choice(String id, String name, String category, String rarity, boolean isQuest) {}

    private final List<Choice> choices;

    public PacketCasinoDraftChoices(List<Choice> choices) {
        this.choices = choices == null ? List.of() : List.copyOf(choices);
    }

    public List<Choice> choices() {
        return choices;
    }

    public static void encode(PacketCasinoDraftChoices msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.choices.size());
        for (Choice c : msg.choices) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, c.id());
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, c.name());
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, c.category());
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, c.rarity());
            buf.writeBoolean(c.isQuest());
        }
    }

    public static PacketCasinoDraftChoices decode(FriendlyByteBuf buf) {
        int size = Math.max(0, Math.min(8, buf.readVarInt()));
        List<Choice> choices = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            choices.add(new Choice(
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                    buf.readBoolean()
            ));
        }
        return new PacketCasinoDraftChoices(choices);
    }

    public static void handle(PacketCasinoDraftChoices msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            if (!ClientCasinoState.isActive()) return;
            List<ClientCasinoState.VisualSlot> list = msg.choices.stream()
                    .map(c -> new ClientCasinoState.VisualSlot(c.id(), c.name(), c.category(), c.rarity(), c.isQuest()))
                    .toList();
            ClientCasinoState.setDraftChoices(list);
        });
        ctx.setPacketHandled(true);
    }
}
