package com.jamie.jamiebingo.util;

import net.minecraft.network.FriendlyByteBuf;

import java.lang.reflect.Method;
import java.util.UUID;

public final class FriendlyByteBufUtil {
    private FriendlyByteBufUtil() {
    }

    public static <E extends Enum<E>> void writeEnum(FriendlyByteBuf buf, E value) {
        if (buf == null) return;
        try {
            Method m = buf.getClass().getMethod("writeEnum", Enum.class);
            m.invoke(buf, value);
            return;
        } catch (Throwable ignored) {
        }
        int ordinal = value == null ? -1 : value.ordinal();
        buf.writeVarInt(ordinal);
    }

    public static <E extends Enum<E>> E readEnum(FriendlyByteBuf buf, Class<E> enumClass) {
        if (buf == null || enumClass == null) return null;
        try {
            Method m = buf.getClass().getMethod("readEnum", Class.class);
            Object out = m.invoke(buf, enumClass);
            return enumClass.cast(out);
        } catch (Throwable ignored) {
        }
        int ordinal = buf.readVarInt();
        if (ordinal < 0) return null;
        E[] values = enumClass.getEnumConstants();
        if (values == null || ordinal >= values.length) return null;
        return values[ordinal];
    }

    public static void writeUUID(FriendlyByteBuf buf, UUID uuid) {
        if (buf == null || uuid == null) return;
        try {
            Method m = buf.getClass().getMethod("writeUUID", UUID.class);
            m.invoke(buf, uuid);
            return;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : buf.getClass().getMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (m.getParameterTypes()[0] != UUID.class) continue;
                if (!m.getName().toLowerCase().contains("uuid")) continue;
                m.invoke(buf, uuid);
                return;
            }
        } catch (Throwable ignored) {
        }
        // Fallback: two longs
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    public static UUID readUUID(FriendlyByteBuf buf) {
        if (buf == null) return new UUID(0L, 0L);
        try {
            Method m = buf.getClass().getMethod("readUUID");
            Object out = m.invoke(buf);
            if (out instanceof UUID u) return u;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : buf.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!m.getName().toLowerCase().contains("uuid")) continue;
                Object out = m.invoke(buf);
                if (out instanceof UUID u) return u;
            }
        } catch (Throwable ignored) {
        }
        // Fallback: two longs
        long most = buf.readLong();
        long least = buf.readLong();
        return new UUID(most, least);
    }

    public static void writeString(FriendlyByteBuf buf, String value) {
        if (buf == null) return;
        if (value == null) value = "";
        try {
            Method m = buf.getClass().getMethod("writeUtf", String.class);
            m.invoke(buf, value);
            return;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : buf.getClass().getMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (m.getParameterTypes()[0] != String.class) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("utf") && !name.contains("string")) continue;
                m.invoke(buf, value);
                return;
            }
        } catch (Throwable ignored) {
        }
        buf.writeByteArray(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static String readString(FriendlyByteBuf buf, int maxLen) {
        if (buf == null) return "";
        try {
            Method m = buf.getClass().getMethod("readUtf", int.class);
            Object out = m.invoke(buf, maxLen);
            return out != null ? out.toString() : "";
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : buf.getClass().getMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (m.getParameterTypes()[0] != int.class) continue;
                if (m.getReturnType() != String.class && m.getReturnType() != Object.class) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("utf") && !name.contains("string")) continue;
                Object out = m.invoke(buf, maxLen);
                return out != null ? out.toString() : "";
            }
        } catch (Throwable ignored) {
        }
        try {
            byte[] raw = buf.readByteArray();
            return new String(raw, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
        }
        return "";
    }
}
