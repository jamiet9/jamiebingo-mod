package com.jamie.jamiebingo.addons;

import com.jamie.jamiebingo.addons.effects.CustomEffectRegistry;
import com.jamie.jamiebingo.casino.CasinoModeManager;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.data.BroadcastHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber
public class RandomEffectsScheduler {

    /* ==========================================================
       SERVER TICK
       ========================================================== */

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {


        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);

        /* ======================================================
           RANDOM EFFECTS ONLY
           ====================================================== */

        // 🚪 HARD GATES
        if (!data.isActive()) return;
        if (data.startCountdownActive) return;
        if (CasinoModeManager.isCasinoInProgress()) return;
        if (data.isRerollPhaseActive()) return;
        if (data.pregameBoxActive) return;
        if (data.gameStartTick < 0) return;
        if (!areParticipantsInOverworld(server, data)) return;
        if (!data.randomEffectsEnabled) return;
        if (data.randomEffectsIntervalSeconds <= 0) return;

        // 🔁 Tick ACTIVE custom effect
        if (!data.appliedCustomEffectId.isEmpty()) {
            var active = CustomEffectRegistry.getById(data.appliedCustomEffectId);
            if (active != null) {
                active.onTick(server);
            }
        }

        int currentTick = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);

        // ⏱ First arming after game start
        if (data.randomEffectsNextTick < 0) {
            data.randomEffectsNextTick =
                    currentTick + data.randomEffectsIntervalSeconds * 20;

            selectNextRandomEffect(data);
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
            return;
        }

        int ticksRemaining = data.randomEffectsNextTick - currentTick;

        // 🔔 Countdown (full duration, with sound at 10s)
        if (ticksRemaining > 0 && ticksRemaining % 20 == 0) {
            int seconds = ticksRemaining / 20;

            BroadcastHelper.broadcastEffectCountdown(
                    server,
                    data.activeRandomEffectName,
                    data.activeRandomEffectAmplifier,
                    seconds,
                    seconds == 10
            );
        }

        // 🎯 Apply effect
        if (ticksRemaining <= 0) {

            applySelectedEffect(server, data);

            BroadcastHelper.broadcastEffectApplied(
                    server,
                    data.activeRandomEffectName,
                    data.activeRandomEffectAmplifier
            );

            data.randomEffectsNextTick =
                    currentTick + data.randomEffectsIntervalSeconds * 20;

            selectNextRandomEffect(data);
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        }
    }

    /* ==========================================================
       EFFECT SELECTION
       ========================================================== */

    private static void selectNextRandomEffect(BingoGameData data) {

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        List<EffectChoice> choices = buildChoices(server, data);
        if (choices.isEmpty()) {
            data.activeCustomEffectId = "";
            data.activeRandomEffectId = "";
            data.activeRandomEffectName = "Random Effect";
            data.activeRandomEffectAmplifier = 0;
            return;
        }

        EffectChoice selected = pickWeighted(choices, data);
        if (selected == null) {
            data.activeCustomEffectId = "";
            data.activeRandomEffectId = "";
            data.activeRandomEffectName = "Random Effect";
            data.activeRandomEffectAmplifier = 0;
            return;
        }

        data.lastRandomEffectKey = selected.key;
        data.randomEffectUseCounts.merge(selected.key, 1, Integer::sum);

        if (selected.customEffect != null) {
            data.activeCustomEffectId = selected.customEffect.id();
            data.activeRandomEffectId = "";
            data.activeRandomEffectName = selected.customEffect.displayName();
            data.activeRandomEffectAmplifier = 0;
            return;
        }

        // ---------- VANILLA ----------
        data.activeCustomEffectId = "";

        Identifier id = selected.vanillaId;
        if (id == null) return;

        data.activeRandomEffectId = id.toString();
        data.activeRandomEffectName = id.getPath().replace('_', ' ');

        int amp = 0;
        Random rng = new Random();
        while (amp < 9 && rng.nextDouble() < 0.25) amp++;

        data.activeRandomEffectAmplifier = amp;
    }

    private static List<EffectChoice> buildChoices(MinecraftServer server, BingoGameData data) {
        List<EffectChoice> out = new ArrayList<>();
        boolean singlePlayer = server == null || com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayerCount(server) <= 1;

        for (var custom : CustomEffectRegistry.all()) {
            if (custom == null) continue;
            if (singlePlayer && "player_swap".equals(custom.id())) continue;
            String key = "custom:" + custom.id();
            out.add(new EffectChoice(key, null, custom, null));
        }

        for (MobEffect effect : RandomEffectRegistry.getAllVanillaEffects()) {
            if (effect == null) continue;
            Identifier id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
            if (id == null) continue;
            String key = "vanilla:" + id;
            out.add(new EffectChoice(key, id, null, effect));
        }

        return out;
    }

    private static EffectChoice pickWeighted(List<EffectChoice> choices, BingoGameData data) {
        double total = 0;
        double[] weights = new double[choices.size()];

        for (int i = 0; i < choices.size(); i++) {
            EffectChoice choice = choices.get(i);
            if (choice.key.equals(data.lastRandomEffectKey)) {
                weights[i] = 0;
                continue;
            }
            int count = data.randomEffectUseCounts.getOrDefault(choice.key, 0);
            double weight = 1.0 / (1.0 + count * 0.6);
            weights[i] = weight;
            total += weight;
        }

        if (total <= 0) {
            return null;
        }

        double r = new Random().nextDouble() * total;
        for (int i = 0; i < choices.size(); i++) {
            r -= weights[i];
            if (r <= 0) return choices.get(i);
        }
        return choices.get(choices.size() - 1);
    }

    private record EffectChoice(
            String key,
            Identifier vanillaId,
            com.jamie.jamiebingo.addons.effects.CustomRandomEffect customEffect,
            MobEffect vanillaEffect
    ) {}

    /* ==========================================================
       APPLY EFFECT
       ========================================================== */

    private static void applySelectedEffect(MinecraftServer server, BingoGameData data) {

        // ------------------------------
        // CLEANUP
        // ------------------------------

        if (!data.appliedRandomEffectId.isEmpty()) {
            MobEffect old =
                    ForgeRegistries.MOB_EFFECTS.getValue(
                            com.jamie.jamiebingo.util.IdUtil.id(data.appliedRandomEffectId));
            if (old != null) {
                var oldHolder = ForgeRegistries.MOB_EFFECTS.getHolder(old).orElse(null);
                for (ServerPlayer p : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                    if (oldHolder != null) {
                        p.removeEffect(oldHolder);
                    }
                }
            }
            data.appliedRandomEffectId = "";
        }

        if (!data.appliedCustomEffectId.isEmpty()) {
            var prev = CustomEffectRegistry.getById(data.appliedCustomEffectId);
            if (prev != null) prev.onRemove(server);
            data.appliedCustomEffectId = "";
        }

        // ------------------------------
        // CUSTOM
        // ------------------------------

        if (!data.activeCustomEffectId.isEmpty()) {
            var custom = CustomEffectRegistry.getById(data.activeCustomEffectId);
            if (custom != null) {
                custom.onApply(server);
                data.appliedCustomEffectId = custom.id();
            }
            BroadcastHelper.broadcastCustomEffectState(server, data);
            return;
        }

        // ------------------------------
        // VANILLA
        // ------------------------------

        if (data.activeRandomEffectId.isEmpty()) return;

        MobEffect effect =
                ForgeRegistries.MOB_EFFECTS.getValue(
                        com.jamie.jamiebingo.util.IdUtil.id(data.activeRandomEffectId));

        if (effect == null) return;
        var holder = ForgeRegistries.MOB_EFFECTS.getHolder(effect).orElse(null);
        if (holder == null) return;

        data.appliedRandomEffectId = data.activeRandomEffectId;

        int duration = data.randomEffectsIntervalSeconds * 20;

        for (ServerPlayer player : BingoGameData.get(server).getParticipantPlayers(server)) {
            player.addEffect(new MobEffectInstance(
                    holder,
                    duration,
                    data.activeRandomEffectAmplifier,
                    false,
                    true
            ));
        }
        BroadcastHelper.broadcastCustomEffectState(server, data);
    }

    /* ==========================================================
       FORCE APPLY (COMMAND SUPPORT)
       ========================================================== */

    public static void forceApply(MinecraftServer server, BingoGameData data) {

        data.randomEffectsNextTick = -1;

        applySelectedEffect(server, data);

        BroadcastHelper.broadcastEffectApplied(
                server,
                data.activeRandomEffectName,
                data.activeRandomEffectAmplifier
        );

        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
    }

    private static boolean areParticipantsInOverworld(MinecraftServer server, BingoGameData data) {
        if (server == null || data == null) return false;
        ServerLevel overworld = com.jamie.jamiebingo.util.ServerLevelUtil.getOverworld(server);
        if (overworld == null) return false;
        for (ServerPlayer player : data.getParticipantPlayers(server)) {
            if (player == null) continue;
            if (com.jamie.jamiebingo.util.ServerPlayerUtil.getLevel(player) != overworld) {
                return false;
            }
        }
        return true;
    }
}








