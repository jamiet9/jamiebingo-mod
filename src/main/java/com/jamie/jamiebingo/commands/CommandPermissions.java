package com.jamie.jamiebingo.commands;

import net.minecraft.commands.CommandSourceStack;
import com.jamie.jamiebingo.util.CommandSourceUtil;

public final class CommandPermissions {

    private CommandPermissions() {}

    public static boolean hasPermission(CommandSourceStack src, int level) {
        if (CommandSourceUtil.getEntity(src) == null) {
            return true;
        }
        return CommandSourceUtil.hasPermission(src, level);
    }
}
