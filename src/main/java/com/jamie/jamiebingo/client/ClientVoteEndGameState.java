package com.jamie.jamiebingo.client;

public final class ClientVoteEndGameState {
    private static int votes = 0;
    private static int total = 0;
    private static boolean localVoted = false;

    private ClientVoteEndGameState() {
    }

    public static void update(int nextVotes, int nextTotal, boolean nextLocalVoted) {
        votes = Math.max(0, nextVotes);
        total = Math.max(0, nextTotal);
        localVoted = nextLocalVoted;
    }

    public static int votes() {
        return votes;
    }

    public static int total() {
        return total;
    }

    public static boolean localVoted() {
        return localVoted;
    }

    public static void reset() {
        votes = 0;
        total = 0;
        localVoted = false;
    }
}
