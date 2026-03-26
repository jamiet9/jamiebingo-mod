package com.jamie.jamiebingo.casino;

import com.jamie.jamiebingo.JamieBingo;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.PriorityQueue;

/**
 * Authoritative tick-based scheduler for Casino Mode.
 *
 * Why this exists:
 * TickTask does NOT reliably delay in your current flow (it executes immediately).
 * This scheduler runs tasks ONLY when com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) reaches the target tick.
 */
@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID)
public final class CasinoTickScheduler {

    private CasinoTickScheduler() {}

    private static final class Scheduled implements Comparable<Scheduled> {
        final int runAtTick;
        final Runnable task;

        Scheduled(int runAtTick, Runnable task) {
            this.runAtTick = runAtTick;
            this.task = task;
        }

        @Override
        public int compareTo(Scheduled o) {
            return Integer.compare(this.runAtTick, o.runAtTick);
        }
    }

    private static final PriorityQueue<Scheduled> QUEUE = new PriorityQueue<>();

    public static void schedule(MinecraftServer server, int runAtTick, Runnable task) {
        if (server == null || task == null) return;
        QUEUE.add(new Scheduled(runAtTick, task));
    }

    public static void clearAll() {
        QUEUE.clear();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        int now = com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server);

        while (!QUEUE.isEmpty() && QUEUE.peek().runAtTick <= now) {
            Scheduled next = QUEUE.poll();
            try {
                next.task.run();
            } catch (Throwable t) {
                System.out.println("[Casino] Scheduled task crashed: " + t.getMessage());
                t.printStackTrace();
            }
        }
    }
}





