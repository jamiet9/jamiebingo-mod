package com.jamie.jamiebingo.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class ServerPlayerListUtil {
    private ServerPlayerListUtil() {
    }

    public static PlayerList getPlayerList(MinecraftServer server) {
        if (server == null) return null;
        // Try any no-arg method returning PlayerList
        for (Method m : server.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (!PlayerList.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                Object out = m.invoke(server);
                if (out instanceof PlayerList list) return list;
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    public static List<ServerPlayer> getPlayers(MinecraftServer server) {
        PlayerList list = getPlayerList(server);
        if (list == null) return List.of();
        // Try any no-arg method returning List
        for (Method m : list.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (!List.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                Object out = m.invoke(list);
                if (out instanceof List<?> l) {
                    @SuppressWarnings("unchecked")
                    List<ServerPlayer> casted = (List<ServerPlayer>) l;
                    return casted;
                }
            } catch (Throwable ignored) {
            }
        }

        return List.of();
    }

    public static int getPlayerCount(MinecraftServer server) {
        PlayerList list = getPlayerList(server);
        if (list == null) return 0;
        // Try any no-arg method returning int/Integer
        for (Method m : list.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() != int.class && m.getReturnType() != Integer.class) continue;
            try {
                Object out = m.invoke(list);
                if (out instanceof Integer i) return i;
            } catch (Throwable ignored) {
            }
        }

        return getPlayers(server).size();
    }

    public static ServerPlayer getPlayer(MinecraftServer server, UUID id) {
        if (id == null) return null;
        PlayerList list = getPlayerList(server);
        if (list == null) return null;
        // Try any method with a single UUID parameter returning ServerPlayer
        for (Method m : list.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 1) continue;
            if (m.getParameterTypes()[0] != UUID.class) continue;
            if (!ServerPlayer.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                Object out = m.invoke(list, id);
                if (out instanceof ServerPlayer sp) return sp;
            } catch (Throwable ignored) {
            }
        }

        // Fallback: search in players list
        for (ServerPlayer sp : getPlayers(server)) {
            if (id.equals(com.jamie.jamiebingo.util.EntityUtil.getUUID(sp))) return sp;
        }
        return null;
    }

    public static int awardRecipes(ServerPlayer player, Collection<?> recipes) {
        return ServerPlayerRecipeUtil.awardRecipes(player, recipes);
    }
}
