package com.jamie.jamiebingo.commands;

import com.jamie.jamiebingo.commands.CommandUtil;

import com.jamie.jamiebingo.data.BingoGameData;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class BingoRtpCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
            CommandUtil.literal("bingo")
                .requires(src -> CommandPermissions.hasPermission(src, 2))
                .then(
                    CommandUtil.literal("rtp")
                        .then(
                            CommandUtil.literal("enabled")
                                .executes(ctx -> {
                                    BingoGameData data =
                                            BingoGameData.get(ctx.getSource().getServer());

                                    data.rtpEnabled = true;
                                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                            () -> com.jamie.jamiebingo.util.ComponentUtil.literal("Random teleport enabled."),
                                            true
                                    );
                                    return 1;
                                })
                        )
                        .then(
                            CommandUtil.literal("disabled")
                                .executes(ctx -> {
                                    BingoGameData data =
                                            BingoGameData.get(ctx.getSource().getServer());

                                    data.rtpEnabled = false;
                                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                            () -> com.jamie.jamiebingo.util.ComponentUtil.literal("Random teleport disabled."),
                                            true
                                    );
                                    return 1;
                                })
                        )
                )
        );
    }
}


