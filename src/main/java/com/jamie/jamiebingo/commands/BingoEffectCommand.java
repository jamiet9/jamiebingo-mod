package com.jamie.jamiebingo.commands;

import com.jamie.jamiebingo.commands.CommandUtil;

import com.jamie.jamiebingo.addons.RandomEffectsScheduler;
import com.jamie.jamiebingo.addons.effects.CustomEffectRegistry;
import com.jamie.jamiebingo.data.BingoGameData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

public class BingoEffectCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
            CommandUtil.literal("bingo")
                .requires(src -> CommandPermissions.hasPermission(src, 2))
                .then(
                    CommandUtil.literal("effect")
                        .then(
                            CommandUtil.literal("force")
                                .then(
                                    CommandUtil.argument("effect_id", StringArgumentType.string())
                                        .executes(ctx -> {

                                            MinecraftServer server =
                                                    ctx.getSource().getServer();
                                            BingoGameData data =
                                                    BingoGameData.get(server);

                                            String id =
                                                    StringArgumentType.getString(ctx, "effect_id");

                                            // ------------------------------
                                            // CUSTOM EFFECT
                                            // ------------------------------
                                            var custom =
                                                    CustomEffectRegistry.getById(id);

                                            if (custom != null) {
                                                data.activeCustomEffectId = custom.id();
                                                data.activeRandomEffectId = "";
                                                data.activeRandomEffectName = custom.displayName();
                                                data.activeRandomEffectAmplifier = 0;

                                                RandomEffectsScheduler.forceApply(server, data);

                                                com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                                        () -> com.jamie.jamiebingo.util.ComponentUtil.literal(
                                                                "Forced custom effect: " + id),
                                                        true
                                                );
                                                return 1;
                                            }

                                            // ------------------------------
                                            // VANILLA EFFECT
                                            // ------------------------------
                                            Identifier rl = null;
                                            try {
                                                String resolved = id.contains(":") ? id : "minecraft:" + id;
                                                rl = com.jamie.jamiebingo.util.IdUtil.id(resolved);
                                            } catch (Exception ignored) {
                                            }
                                            if (rl != null) {
                                                MobEffect effect =
                                                        ForgeRegistries.MOB_EFFECTS.getValue(rl);

                                                if (effect != null) {
                                                    data.activeRandomEffectId = rl.toString();
                                                    data.activeCustomEffectId = "";
                                                    data.activeRandomEffectName =
                                                            rl.getPath().replace('_', ' ');
                                                    data.activeRandomEffectAmplifier = 0;

                                                    RandomEffectsScheduler.forceApply(server, data);

                                                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                                            () -> com.jamie.jamiebingo.util.ComponentUtil.literal(
                                                                    "Forced vanilla effect: " + id),
                                                            true
                                                    );
                                                    return 1;
                                                }
                                            }

                                            // ------------------------------
                                            // FAILURE
                                            // ------------------------------
                                            com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), 
                                                    com.jamie.jamiebingo.util.ComponentUtil.literal(
                                                            "Unknown effect id: " + id)
                                            );
                                            return 0;
                                        })
                                )
                        )
                )
        );
    }
}

