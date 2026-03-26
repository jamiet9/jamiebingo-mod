package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.bingo.BingoCard;
import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.bingo.WeeklyChallengeManager;
import com.jamie.jamiebingo.client.WeeklyChallengeClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;

public class PacketWeeklyChallengePreviewResponse {
    private final BingoCard card;
    private final List<String> settingsLines;
    private final String cardSeed;
    private final String worldSeed;
    private final String settingsSeed;
    private final String challengeId;
    private final long nextResetEpochSeconds;
    private final String error;

    public PacketWeeklyChallengePreviewResponse(BingoCard card, List<String> settingsLines, String cardSeed, String worldSeed, String settingsSeed, String challengeId, long nextResetEpochSeconds, String error) {
        this.card = card;
        this.settingsLines = settingsLines == null ? List.of() : new ArrayList<>(settingsLines);
        this.cardSeed = cardSeed == null ? "" : cardSeed;
        this.worldSeed = worldSeed == null ? "" : worldSeed;
        this.settingsSeed = settingsSeed == null ? "" : settingsSeed;
        this.challengeId = challengeId == null ? "" : challengeId;
        this.nextResetEpochSeconds = Math.max(0L, nextResetEpochSeconds);
        this.error = error == null ? "" : error;
    }

    public static PacketWeeklyChallengePreviewResponse from(WeeklyChallengeManager.WeeklyChallenge weekly) {
        if (weekly == null) {
            return new PacketWeeklyChallengePreviewResponse(null, List.of(), "", "", "", "", 0L, "Weekly challenge unavailable");
        }
        return new PacketWeeklyChallengePreviewResponse(
                weekly.card(),
                weekly.settingsLines(),
                weekly.cardSeed(),
                weekly.worldSeed(),
                weekly.settingsSeed(),
                weekly.challengeId(),
                weekly.nextResetEpochSeconds(),
                ""
        );
    }

    public static void encode(PacketWeeklyChallengePreviewResponse msg, FriendlyByteBuf buf) {
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
        buf.writeInt(msg.settingsLines.size());
        for (String line : msg.settingsLines) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, line);
        }
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.cardSeed);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.worldSeed);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.settingsSeed);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.challengeId);
        buf.writeLong(msg.nextResetEpochSeconds);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.error);
    }

    public static PacketWeeklyChallengePreviewResponse decode(FriendlyByteBuf buf) {
        BingoCard card = null;
        if (buf.readBoolean()) {
            int size = buf.readInt();
            card = new BingoCard(size);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (!buf.readBoolean()) continue;
                    card.setSlot(x, y, new BingoSlot(
                            com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                            com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                            com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                            com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767)
                    ));
                }
            }
        }
        int settingsCount = Math.max(0, buf.readInt());
        List<String> settingsLines = new ArrayList<>(settingsCount);
        for (int i = 0; i < settingsCount; i++) {
            settingsLines.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
        }
        return new PacketWeeklyChallengePreviewResponse(
                card,
                settingsLines,
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 100000),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 100000),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 100000),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                buf.readLong(),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767)
        );
    }

    public static void handle(PacketWeeklyChallengePreviewResponse msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> WeeklyChallengeClientState.setPreview(
                        msg.card,
                        msg.settingsLines,
                        msg.cardSeed,
                        msg.worldSeed,
                        msg.settingsSeed,
                        msg.challengeId,
                        msg.nextResetEpochSeconds,
                        msg.error
                )
        ));
        ctx.setPacketHandled(true);
    }
}
