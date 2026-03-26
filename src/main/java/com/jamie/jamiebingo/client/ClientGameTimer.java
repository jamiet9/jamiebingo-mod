package com.jamie.jamiebingo.client;

public final class ClientGameTimer {

    public static boolean active = false;
    public static boolean countdownEnabled = false;
    public static int seconds = 0;
    public static boolean rushActive = false;
    public static int rushSeconds = 0;
    private static int lastClientTick = -1;
    private static int tickRemainder = 0;

    private ClientGameTimer() {
    }

    public static void update(
            boolean activeValue,
            boolean countdownValue,
            int secondsValue,
            boolean rushActiveValue,
            int rushSecondsValue
    ) {
        if (activeValue) {
            com.jamie.jamiebingo.client.render.GlobalWallScreenRenderer.forceDeactivate();
            com.jamie.jamiebingo.client.casino.ClientCasinoState.end();
            net.minecraft.client.Minecraft mc = com.jamie.jamiebingo.client.ClientMinecraftUtil.getMinecraft();
            if (mc != null && com.jamie.jamiebingo.client.ClientMinecraftUtil.getScreen(mc) instanceof com.jamie.jamiebingo.client.screen.CasinoBingoScreen) {
                com.jamie.jamiebingo.client.ClientMinecraftUtil.setScreen(mc, null);
            }
        }
        active = activeValue;
        countdownEnabled = countdownValue;
        seconds = Math.max(0, secondsValue);
        rushActive = rushActiveValue;
        rushSeconds = Math.max(0, rushSecondsValue);
        lastClientTick = -1;
        tickRemainder = 0;
    }

    public static void clear() {
        active = false;
        countdownEnabled = false;
        seconds = 0;
        rushActive = false;
        rushSeconds = 0;
        lastClientTick = -1;
        tickRemainder = 0;
    }

    public static void tickVisual() {
        if (!active) {
            lastClientTick = -1;
            tickRemainder = 0;
            return;
        }
        var player = com.jamie.jamiebingo.client.ClientMinecraftUtil.getPlayer();
        if (player == null) {
            lastClientTick = -1;
            tickRemainder = 0;
            return;
        }
        int now = com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(player);
        if (lastClientTick < 0) {
            lastClientTick = now;
            return;
        }
        int deltaTicks = now - lastClientTick;
        if (deltaTicks <= 0) return;
        lastClientTick = now;
        int total = tickRemainder + deltaTicks;
        int wholeSeconds = total / 20;
        tickRemainder = total % 20;
        if (wholeSeconds <= 0) return;

        if (countdownEnabled) {
            seconds = Math.max(0, seconds - wholeSeconds);
        } else {
            seconds = Math.max(0, seconds + wholeSeconds);
        }
        if (rushActive) {
            rushSeconds = Math.max(0, rushSeconds - wholeSeconds);
        }
    }
}
