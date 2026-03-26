package com.jamie.jamiebingo.addons.effects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import com.jamie.jamiebingo.data.BingoGameData;

public class EffectInvertedDrowning implements CustomRandomEffect {

    private static final String NBT_KEY = "jamiebingo_inv_drown_air";

    @Override
    public String id() {
        return "inverted_drowning";
    }

    @Override
    public String displayName() {
        return "Inverted Drowning";
    }

    @Override
    public void onApply(MinecraftServer server) {
        // Initialize fake air to max so the first land-tick starts clean.
        BingoGameData data = BingoGameData.get(server);
        for (ServerPlayer p : data.getParticipantPlayers(server)) {
            CompoundTag tag = p.getPersistentData();
            com.jamie.jamiebingo.util.NbtUtil.putInt(tag, NBT_KEY, p.getMaxAirSupply());
        }
    }

    @Override
    public void onRemove(MinecraftServer server) {
        // Restore normal air behavior
        BingoGameData data = BingoGameData.get(server);
        for (ServerPlayer p : data.getParticipantPlayers(server)) {
            p.setAirSupply(p.getMaxAirSupply());
            p.getPersistentData().remove(NBT_KEY);
        }
    }

    @Override
    public void onTick(MinecraftServer server) {
        BingoGameData data = BingoGameData.get(server);
        for (ServerPlayer p : data.getParticipantPlayers(server)) {

            int maxAir = p.getMaxAirSupply();
            CompoundTag tag = p.getPersistentData();

            // Treat anything "in water/bubbles" as true water breathing time.
            boolean inWater = p.isInWaterOrSwimmable();

            if (inWater) {
                // ✅ Underwater = you can breathe
                com.jamie.jamiebingo.util.NbtUtil.putInt(tag, NBT_KEY, maxAir);
                if (p.getAirSupply() != maxAir) {
                    p.setAirSupply(maxAir);
                }
                continue;
            }

            // ✅ On land = you drown (smoothly, no flicker)
            int fakeAir = com.jamie.jamiebingo.util.NbtUtil.getInt(tag, NBT_KEY, maxAir);

            // Decrease by 1 per tick (exactly like vanilla underwater drain speed)
            fakeAir -= 1;

            // Vanilla drowning logic: when air reaches -20, take damage and reset to 0
            if (fakeAir <= -20) {
                // 2.0F is vanilla drown damage amount
                p.hurt(p.damageSources().drown(), 2.0F);
                fakeAir = 0;
            }

            com.jamie.jamiebingo.util.NbtUtil.putInt(tag, NBT_KEY, fakeAir);

            // Force the visible air meter to match our fake air every tick,
            // so vanilla can't refill it on land (this is what stops flicker).
            if (p.getAirSupply() != fakeAir) {
                p.setAirSupply(fakeAir);
            }
        }
    }
}



