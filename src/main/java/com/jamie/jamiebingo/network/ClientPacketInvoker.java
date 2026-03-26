package com.jamie.jamiebingo.network;

import java.lang.reflect.Method;

public final class ClientPacketInvoker {

    private ClientPacketInvoker() {
    }

    public static void invoke(String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            Class<?> handler = Class.forName("com.jamie.jamiebingo.client.ClientPacketHandlers");
            Method method = handler.getMethod(methodName, paramTypes);
            method.invoke(null, args);
        } catch (Throwable ignored) {
        }
    }
}
