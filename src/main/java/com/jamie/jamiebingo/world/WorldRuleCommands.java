package com.jamie.jamiebingo.world;


import com.jamie.jamiebingo.util.GameRulesUtil;
import com.jamie.jamiebingo.util.ServerLevelUtil;
import com.jamie.jamiebingo.commands.CommandUtil;

import com.jamie.jamiebingo.data.BingoGameData;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.gamerules.GameRules;

public class WorldRuleCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        /* ===============================
           /difficulty (VANILLA)
           =============================== */
        dispatcher.register(
            CommandUtil.literal("difficulty")
                .then(CommandUtil.literal("easy").executes(ctx -> {
                    MinecraftServer server = ctx.getSource().getServer();
                    BingoGameData data = BingoGameData.get(server);

                    data.randomDifficultyIntent = false;
                    server.setDifficulty(Difficulty.EASY, true);
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                        () -> com.jamie.jamiebingo.util.ComponentUtil.literal("World difficulty set to EASY"),
                        true
                    );
                    return 1;
                }))
                .then(CommandUtil.literal("normal").executes(ctx -> {
                    MinecraftServer server = ctx.getSource().getServer();
                    BingoGameData data = BingoGameData.get(server);

                    data.randomDifficultyIntent = false;
                    server.setDifficulty(Difficulty.NORMAL, true);
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                        () -> com.jamie.jamiebingo.util.ComponentUtil.literal("World difficulty set to NORMAL"),
                        true
                    );
                    return 1;
                }))
                .then(CommandUtil.literal("hard").executes(ctx -> {
                    MinecraftServer server = ctx.getSource().getServer();
                    BingoGameData data = BingoGameData.get(server);

                    data.randomDifficultyIntent = false;
                    server.setDifficulty(Difficulty.HARD, true);
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                        () -> com.jamie.jamiebingo.util.ComponentUtil.literal("World difficulty set to HARD"),
                        true
                    );
                    return 1;
                }))
                .then(CommandUtil.literal("random").executes(ctx -> {
                    MinecraftServer server = ctx.getSource().getServer();
                    BingoGameData data = BingoGameData.get(server);

                    data.randomDifficultyIntent = true;
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                        () -> com.jamie.jamiebingo.util.ComponentUtil.literal("World difficulty set to RANDOM"),
                        true
                    );
                    return 1;
                }))
        );

        /* ===============================
           /hardcore
           =============================== */
        dispatcher.register(
            CommandUtil.literal("hardcore")
                .then(CommandUtil.literal("enabled").executes(ctx -> {
                    MinecraftServer server = ctx.getSource().getServer();
                    BingoGameData data = BingoGameData.get(server);

                    data.randomHardcoreIntent = false;
                    data.hardcoreEnabled = true;
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                        () -> com.jamie.jamiebingo.util.ComponentUtil.literal("Hardcore ENABLED"),
                        true
                    );
                    return 1;
                }))
                .then(CommandUtil.literal("disabled").executes(ctx -> {
                    MinecraftServer server = ctx.getSource().getServer();
                    BingoGameData data = BingoGameData.get(server);

                    data.randomHardcoreIntent = false;
                    data.hardcoreEnabled = false;
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                        () -> com.jamie.jamiebingo.util.ComponentUtil.literal("Hardcore DISABLED"),
                        true
                    );
                    return 1;
                }))
                .then(CommandUtil.literal("random").executes(ctx -> {
                    BingoGameData data = BingoGameData.get(ctx.getSource().getServer());
                    data.randomHardcoreIntent = true;
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                        () -> com.jamie.jamiebingo.util.ComponentUtil.literal("Hardcore set to RANDOM (10%)"),
                        true
                    );
                    return 1;
                }))
        );

        /* ===============================
           /keepinventory
           =============================== */
        dispatcher.register(
            CommandUtil.literal("keepinventory")
                .then(CommandUtil.literal("enabled").executes(ctx -> {
                    MinecraftServer server = ctx.getSource().getServer();
                    BingoGameData data = BingoGameData.get(server);

                    data.randomKeepInventoryIntent = false;
                    data.keepInventoryEnabled = true;
                    var overworld = ServerLevelUtil.getOverworld(server);
                    var rules = GameRulesUtil.getGameRules(overworld);
                    if (rules != null) {
                        rules.set(GameRules.KEEP_INVENTORY, true, server);
                    }
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                        () -> com.jamie.jamiebingo.util.ComponentUtil.literal("Keep Inventory ENABLED"),
                        true
                    );
                    return 1;
                }))
                .then(CommandUtil.literal("disabled").executes(ctx -> {
                    MinecraftServer server = ctx.getSource().getServer();
                    BingoGameData data = BingoGameData.get(server);

                    data.randomKeepInventoryIntent = false;
                    data.keepInventoryEnabled = false;
                    var overworld = ServerLevelUtil.getOverworld(server);
                    var rules = GameRulesUtil.getGameRules(overworld);
                    if (rules != null) {
                        rules.set(GameRules.KEEP_INVENTORY, false, server);
                    }
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                        () -> com.jamie.jamiebingo.util.ComponentUtil.literal("Keep Inventory DISABLED"),
                        true
                    );
                    return 1;
                }))
                .then(CommandUtil.literal("random").executes(ctx -> {
                    BingoGameData data = BingoGameData.get(ctx.getSource().getServer());
                    data.randomKeepInventoryIntent = true;
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                        () -> com.jamie.jamiebingo.util.ComponentUtil.literal("Keep Inventory set to RANDOM"),
                        true
                    );
                    return 1;
                }))
        );
    }
}




