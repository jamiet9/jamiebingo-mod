package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.bingo.BingoCard;
import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.bingo.WinCondition;
import com.jamie.jamiebingo.client.ClientCardData;
import com.jamie.jamiebingo.client.ClientGameState;
import com.jamie.jamiebingo.client.ClientHangmanState;
import com.jamie.jamiebingo.client.ClientWinningLineData;
import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import com.jamie.jamiebingo.util.FriendlyByteBufUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class PacketSyncCard {

    private final BingoCard card;
    private final WinCondition winCondition;
    private final boolean teamChestEnabled;
    private final Set<Integer> fakeGreenSlotIndices;
    private final Set<Integer> fakeRedSlotIndices;

    /* ===============================
       CONSTRUCTOR
       =============================== */

    public PacketSyncCard(BingoCard card, WinCondition winCondition) {
        this(card, winCondition, true, Set.of(), Set.of());
    }

    public PacketSyncCard(BingoCard card, WinCondition winCondition, boolean teamChestEnabled) {
        this(card, winCondition, teamChestEnabled, Set.of(), Set.of());
    }

    public PacketSyncCard(BingoCard card, WinCondition winCondition, boolean teamChestEnabled, Set<Integer> fakeGreenSlotIndices, Set<Integer> fakeRedSlotIndices) {
        this.card = card;
        this.winCondition = winCondition;
        this.teamChestEnabled = teamChestEnabled;
        this.fakeGreenSlotIndices = fakeGreenSlotIndices == null
                ? Set.of()
                : Collections.unmodifiableSet(new HashSet<>(fakeGreenSlotIndices));
        this.fakeRedSlotIndices = fakeRedSlotIndices == null
                ? Set.of()
                : Collections.unmodifiableSet(new HashSet<>(fakeRedSlotIndices));
    }

    /* ===============================
       ENCODE (UNCHANGED)
       =============================== */

    public static void encode(PacketSyncCard msg, FriendlyByteBuf buf) {

        // --- win condition FIRST ---
        FriendlyByteBufUtil.writeEnum(buf, msg.winCondition);
        buf.writeBoolean(msg.teamChestEnabled);

        // --- card ---
        buf.writeInt(msg.card.getSize());
        for (int y = 0; y < msg.card.getSize(); y++) {
            for (int x = 0; x < msg.card.getSize(); x++) {
                BingoSlot s = msg.card.getSlot(x, y);
                if (s == null) {
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, "");
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, "");
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, "");
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, "");
                } else {
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, s.getId());
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, s.getName());
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, s.getCategory());
                    com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, s.getRarity());
                }
            }
        }

        buf.writeInt(msg.fakeGreenSlotIndices.size());
        for (Integer idx : msg.fakeGreenSlotIndices) {
            if (idx == null) continue;
            buf.writeInt(idx);
        }

        buf.writeInt(msg.fakeRedSlotIndices.size());
        for (Integer idx : msg.fakeRedSlotIndices) {
            if (idx == null) continue;
            buf.writeInt(idx);
        }
    }

    /* ===============================
       DECODE (UNCHANGED)
       =============================== */

    public static PacketSyncCard decode(FriendlyByteBuf buf) {

        WinCondition winCondition = FriendlyByteBufUtil.readEnum(buf, WinCondition.class);
        boolean teamChestEnabled = buf.readBoolean();

        int size = buf.readInt();
        BingoCard card = new BingoCard(size);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                String id = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
                String name = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
                String cat = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
                String rar = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
                card.setSlot(x, y, new BingoSlot(id, name, cat, rar));
            }
        }
        int fakeGreenCount = Math.max(0, buf.readInt());
        Set<Integer> fakeGreenIndices = new HashSet<>();
        for (int i = 0; i < fakeGreenCount; i++) {
            fakeGreenIndices.add(buf.readInt());
        }

        int fakeRedCount = Math.max(0, buf.readInt());
        Set<Integer> fakeRedIndices = new HashSet<>();
        for (int i = 0; i < fakeRedCount; i++) {
            fakeRedIndices.add(buf.readInt());
        }

        return new PacketSyncCard(card, winCondition, teamChestEnabled, fakeGreenIndices, fakeRedIndices);
    }

    /* ===============================
       HANDLE (CLIENT) — FIXED
       =============================== */

    public static void handle(PacketSyncCard msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {

            // 🚫 CRITICAL: ignore card sync while casino animation is active
            if (ClientCasinoState.isActive()) {
                ClientCardData.setFakeMarkers(msg.fakeGreenSlotIndices, msg.fakeRedSlotIndices);
                return;
            }

            // Clear any previous win animation
            ClientWinningLineData.clear();

            // Sync card
            ClientCardData.setCard(msg.card, msg.fakeGreenSlotIndices, msg.fakeRedSlotIndices);

            // Sync authoritative win condition
            ClientGameState.winCondition = msg.winCondition;
            ClientGameState.teamChestEnabled = msg.teamChestEnabled;

            // Prevent a one-frame reveal flash on hangman game start.
            if (msg.winCondition == WinCondition.HANGMAN) {
                ClientHangmanState.slotRevealed = false;
            }
        });

        ctx.setPacketHandled(true);
    }
}

