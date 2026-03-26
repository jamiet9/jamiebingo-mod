package com.jamie.jamiebingo.client;

/**
 * Simple client-side flag:
 * if true, the next chat message is sent to team chat instead of global.
 */
public class ClientTeamChatState {
    public static boolean pendingTeamChat = false;
    public static long pendingTeamChatUntilTick = -1;
}
