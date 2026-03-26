package com.jamie.jamiebingo.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class CommandUtil {

    private CommandUtil() {}

    public static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static ArgumentType<?> player() {
        try {
            for (Method m : EntityArgument.class.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                if (!"c".equals(m.getName())) continue;
                if (m.getParameterCount() != 0) continue;
                if (!ArgumentType.class.isAssignableFrom(m.getReturnType())) continue;
                m.setAccessible(true);
                return (ArgumentType<?>) m.invoke(null);
            }
            for (Method m : EntityArgument.class.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 0) continue;
                if (!ArgumentType.class.isAssignableFrom(m.getReturnType())) continue;
                m.setAccessible(true);
                return (ArgumentType<?>) m.invoke(null);
            }
        } catch (Exception ignored) {
        }
        throw new IllegalStateException("Unable to resolve EntityArgument.player()");
    }
}
