package com.jamie.jamiebingo.bingo;

import com.jamie.jamiebingo.commands.CommandUtil;

import com.jamie.jamiebingo.casino.CasinoModeManager;
import com.jamie.jamiebingo.commands.CommandPermissions;
import com.jamie.jamiebingo.util.CommandSourceUtil;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.bingo.CardSeedCodec;
import com.jamie.jamiebingo.data.BroadcastHelper;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketOpenSeedGui;
import com.jamie.jamiebingo.data.TeamData;
import com.jamie.jamiebingo.data.TeamScoreData;
import com.jamie.jamiebingo.item.PlayerTrackerHandler;
import com.jamie.jamiebingo.menu.TeamSelectMenu;
import com.jamie.jamiebingo.world.SpectatorManager;
import com.jamie.jamiebingo.quest.QuestDatabase;
import com.jamie.jamiebingo.quest.QuestTracker;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.UUID;

public class BingoCommands {

    private static final int SEED_CHUNK_SIZE = 180;
    private static final java.util.Map<java.util.UUID, SeedBuffer> SEED_BUFFERS = new java.util.HashMap<>();

    private static final class SeedBuffer {
        final int total;
        final java.util.Map<Integer, String> parts = new java.util.HashMap<>();

        SeedBuffer(int total) {
            this.total = total;
        }
    }

    private static int startGameFromSeed(CommandContext<CommandSourceStack> ctx, String seed) {
        boolean ok = BingoSeedHelper.startFromSeed(ctx.getSource(), seed);
        return ok ? 1 : 0;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return CommandUtil.literal("bingo")
                .executes(ctx -> {
                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                            () -> com.jamie.jamiebingo.util.ComponentUtil.literal(
                                    "JamieBingo commands: /bingo start | /bingo seed | /bingo gui | /bingo latejoin | /bingo team | /bingo help"
                            ),
                            false
                    );
                    return 1;
                })

                /* ===============================
                   START GAME
                   =============================== */
                .then(CommandUtil.literal("start")
                        .requires(src -> CommandPermissions.hasPermission(src, 2))
                        .executes(ctx -> {

                            MinecraftServer server = com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource());
                            BingoGameData data = BingoGameData.get(server);

                            if (data.startGame(server)) {
                                CasinoModeManager.startPregamePhasesOrFinalize(server, data);
                            }
                            return 1;
                        })
                        .then(CommandUtil.literal("seed")
                                .then(CommandUtil.argument("seed", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String seed = StringArgumentType.getString(ctx, "seed");
                                            return startGameFromSeed(ctx, seed);
                                        }))))

                .then(CommandUtil.literal("seed")
                        .executes(ctx -> {
                            MinecraftServer server = com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource());
                            BingoGameData data = BingoGameData.get(server);
                            String seed;
                            String label;
                            if (!data.isActive() || data.getCurrentCard() == null) {
                                seed = data.getLastPlayedSeed();
                                label = "Last played seed: ";
                                if (seed == null || seed.isBlank()) {
                                    com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(),
                                            com.jamie.jamiebingo.util.ComponentUtil.literal("No active game and no previous seed found.")
                                    );
                                    return 0;
                                }
                            } else {
                                seed = CardSeedCodec.encode(data, server);
                                label = "Card seed: ";
                            }

                            if (seed == null || seed.isBlank()) {
                                com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), 
                                        com.jamie.jamiebingo.util.ComponentUtil.literal("Failed to generate seed.")
                                );
                                return 0;
                            }

                            Component seedComp = com.jamie.jamiebingo.util.ComponentUtil.literal(seed)
                                    .withStyle(style -> style
                                            .withClickEvent(new ClickEvent.CopyToClipboard(seed))
                                            .withHoverEvent(new HoverEvent.ShowText(
                                                    com.jamie.jamiebingo.util.ComponentUtil.literal("Copy to clipboard")))
                                            .withUnderlined(true)
                                            .withColor(ChatFormatting.AQUA));

                            com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                    () -> com.jamie.jamiebingo.util.ComponentUtil.literal(label).append(seedComp),
                                    false
                            );
                            return 1;
                        })
                        .then(CommandUtil.literal("allgungame")
                                .requires(src -> CommandPermissions.hasPermission(src, 2))
                                .executes(ctx -> {
                                    String seed = CardSeedCodec.buildAllIdsGunGameSeed(10);
                                    if (seed == null || seed.isBlank()) {
                                        com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), 
                                                com.jamie.jamiebingo.util.ComponentUtil.literal("Failed to generate all-items gungame seed.")
                                        );
                                        return 0;
                                    }

                                    Component seedComp = com.jamie.jamiebingo.util.ComponentUtil.literal(seed)
                                            .withStyle(style -> style
                                                    .withClickEvent(new ClickEvent.CopyToClipboard(seed))
                                                    .withHoverEvent(new HoverEvent.ShowText(
                                                            com.jamie.jamiebingo.util.ComponentUtil.literal("Copy to clipboard")))
                                                    .withUnderlined(true)
                                                    .withColor(ChatFormatting.AQUA));

                                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                            () -> com.jamie.jamiebingo.util.ComponentUtil.literal("GunGame all-items seed: ").append(seedComp),
                                            false
                                    );
                                    return 1;
                                }))
                        .then(CommandUtil.literal("gui")
                                .executes(ctx -> {
                                    if (CommandSourceUtil.getEntity(ctx.getSource()) instanceof ServerPlayer player) {
                                        NetworkHandler.send(
                                                new PacketOpenSeedGui(),
                                                net.minecraftforge.network.PacketDistributor.PLAYER.with(player)
                                        );
                                    }
                                    return 1;
                                }))
                        .then(CommandUtil.argument("seed", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String seed = StringArgumentType.getString(ctx, "seed");
                                    return startGameFromSeed(ctx, seed);
                                })))

                .then(CommandUtil.literal("startseed")
                        .then(CommandUtil.argument("seed", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String seed = StringArgumentType.getString(ctx, "seed");
                                    return startGameFromSeed(ctx, seed);
                                }))
                        .then(CommandUtil.literal("part")
                                .then(CommandUtil.argument("index", IntegerArgumentType.integer(1))
                                        .then(CommandUtil.argument("total", IntegerArgumentType.integer(1))
                                                .then(CommandUtil.argument("chunk", StringArgumentType.greedyString())
                                                        .executes(ctx -> {
                                                            int index = IntegerArgumentType.getInteger(ctx, "index");
                                                            int total = IntegerArgumentType.getInteger(ctx, "total");
                                                            String chunk = StringArgumentType.getString(ctx, "chunk");
                                                            java.util.UUID playerId = CommandSourceUtil.getEntity(ctx.getSource()) instanceof ServerPlayer sp
                                                                    ? com.jamie.jamiebingo.util.EntityUtil.getUUID(sp)
                                                                    : new java.util.UUID(0L, 0L);

                                                            SeedBuffer buf = SEED_BUFFERS.computeIfAbsent(playerId, id -> new SeedBuffer(total));
                                                            if (buf.total != total) {
                                                                SEED_BUFFERS.put(playerId, new SeedBuffer(total));
                                                                buf = SEED_BUFFERS.get(playerId);
                                                            }
                                                            buf.parts.put(index, chunk);

                                                            com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                                                    () -> com.jamie.jamiebingo.util.ComponentUtil.literal("Seed part " + index + "/" + total + " stored.")
                                                                            .withStyle(style -> style.withColor(ChatFormatting.GREEN)),
                                                                    false
                                                            );
                                                            return 1;
                                                        })))))
                        .then(CommandUtil.literal("finish")
                                .executes(ctx -> {
                                    java.util.UUID playerId = CommandSourceUtil.getEntity(ctx.getSource()) instanceof ServerPlayer sp
                                            ? com.jamie.jamiebingo.util.EntityUtil.getUUID(sp)
                                            : new java.util.UUID(0L, 0L);
                                    SeedBuffer buf = SEED_BUFFERS.get(playerId);
                                    if (buf == null) {
                                        com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), com.jamie.jamiebingo.util.ComponentUtil.literal("No seed parts received."));
                                        return 0;
                                    }
                                    StringBuilder sb = new StringBuilder();
                                    for (int i = 1; i <= buf.total; i++) {
                                        String part = buf.parts.get(i);
                                        if (part == null) {
                                            com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), 
                                                    com.jamie.jamiebingo.util.ComponentUtil.literal("Missing seed part " + i + "/" + buf.total + ".")
                                            );
                                            return 0;
                                        }
                                        sb.append(part.trim());
                                    }
                                    SEED_BUFFERS.remove(playerId);
                                    return startGameFromSeed(ctx, sb.toString());
                                }))
                        .then(CommandUtil.literal("clear")
                                .executes(ctx -> {
                                    java.util.UUID playerId = CommandSourceUtil.getEntity(ctx.getSource()) instanceof ServerPlayer sp
                                            ? com.jamie.jamiebingo.util.EntityUtil.getUUID(sp)
                                            : new java.util.UUID(0L, 0L);
                                    SEED_BUFFERS.remove(playerId);
                                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                            () -> com.jamie.jamiebingo.util.ComponentUtil.literal("Seed buffer cleared."),
                                            false
                                    );
                                    return 1;
                                })))

                .then(CommandUtil.literal("seedstart")
                        .then(CommandUtil.argument("seed", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String seed = StringArgumentType.getString(ctx, "seed");
                                    return startGameFromSeed(ctx, seed);
                                })))

                .then(CommandUtil.literal("latejoin")
                        .executes(ctx -> {
                            if (!(CommandSourceUtil.getEntity(ctx.getSource()) instanceof ServerPlayer player)) {
                                return 0;
                            }
                            MinecraftServer server = com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource());
                            BingoGameData data = BingoGameData.get(server);
                            if (!data.isActive()) {
                                com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), com.jamie.jamiebingo.util.ComponentUtil.literal("No active game."));
                                return 0;
                            }
                            if (!data.allowLateJoin) {
                                com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), com.jamie.jamiebingo.util.ComponentUtil.literal("Late join is disabled."));
                                return 0;
                            }
                            if (!SpectatorManager.isSpectator(player)) {
                                com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), com.jamie.jamiebingo.util.ComponentUtil.literal("You are not a spectator."));
                                return 0;
                            }

                            SpectatorManager.beginLateJoin(player, data);
                            data.setLateJoinPending(com.jamie.jamiebingo.util.EntityUtil.getUUID(player), true);
                            player.openMenu(new SimpleMenuProvider(
                                    (i, inv, p) -> new TeamSelectMenu(i, inv),
                                    com.jamie.jamiebingo.util.ComponentUtil.literal("Teams")
                            ));
                            return 1;
                        }))

        .then(CommandUtil.literal("controller")
    .executes(ctx -> {

        if (!(CommandSourceUtil.getEntity(ctx.getSource()) instanceof ServerPlayer player)) {
            return 0;
        }

        // Check if player already has one
        boolean hasController = com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player)).stream()
                .anyMatch(stack -> com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack) ==
                        com.jamie.jamiebingo.item.ModItems.BINGO_CONTROLLER.get());

        if (hasController) {
            com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), 
                    com.jamie.jamiebingo.util.ComponentUtil.literal("You already have a Bingo Controller.")
            );
            return 0;
        }

        // Give controller
        com.jamie.jamiebingo.util.PlayerInventoryUtil.addItem(
                player,
                new net.minecraft.world.item.ItemStack(
                        com.jamie.jamiebingo.item.ModItems.BINGO_CONTROLLER.get()
                )
        );

        com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                () -> com.jamie.jamiebingo.util.ComponentUtil.literal("Bingo Controller given."),
                false
        );
        return 1;
    })
)

.then(CommandUtil.literal("tracker")
    .executes(ctx -> {
        if (!(CommandSourceUtil.getEntity(ctx.getSource()) instanceof ServerPlayer player)) {
            return 0;
        }

        if (!PlayerTrackerHandler.shouldGiveTracker(com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource()))) {
            com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), 
                    com.jamie.jamiebingo.util.ComponentUtil.literal("Player tracker is disabled with only one team.")
            );
            return 0;
        }

        ItemStack tracker = PlayerTrackerHandler.createTrackerItem(
                com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource()),
                player
        );
        boolean added = com.jamie.jamiebingo.util.PlayerInventoryUtil.addItem(player, tracker);
        if (!added) {
            player.drop(tracker, false);
        }

        com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                () -> com.jamie.jamiebingo.util.ComponentUtil.literal("Player tracker given."),
                false
        );
        return 1;
    })
)

/* ===============================
   WORLD RULES
   =============================== */
.then(CommandUtil.literal("hunger")
        .requires(src -> CommandPermissions.hasPermission(src, 2))

        .then(CommandUtil.literal("enabled")
                .executes(ctx -> {
                    MinecraftServer server = com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource());
                    BingoGameData data = BingoGameData.get(server);

                    data.hungerEnabled = true;
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                    BroadcastHelper.broadcast(
                            server,
                            com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Hunger enabled.")
                                    .withStyle(style -> style.withColor(ChatFormatting.GREEN))
                    );
                    return 1;
                }))

        .then(CommandUtil.literal("disabled")
                .executes(ctx -> {
                    MinecraftServer server = com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource());
                    BingoGameData data = BingoGameData.get(server);

                    data.hungerEnabled = false;
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                    BroadcastHelper.broadcast(
                            server,
                            com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Hunger disabled.")
                                    .withStyle(style -> style.withColor(ChatFormatting.RED))
                    );
                    return 1;
                }))
)

.then(CommandUtil.literal("hostilemobs")
        .requires(src -> CommandPermissions.hasPermission(src, 2))

        .then(CommandUtil.literal("enabled")
                .executes(ctx -> {
                    MinecraftServer server = com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource());
                    BingoGameData data = BingoGameData.get(server);

                    data.hostileMobsEnabled = true;
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                    BroadcastHelper.broadcast(
                            server,
                            com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Hostile mobs enabled.")
                                    .withStyle(style -> style.withColor(ChatFormatting.GREEN))
                    );
                    return 1;
                }))

        .then(CommandUtil.literal("disabled")
                .executes(ctx -> {
                    MinecraftServer server = com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource());
                    BingoGameData data = BingoGameData.get(server);

                    data.hostileMobsEnabled = false;
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                    BroadcastHelper.broadcast(
                            server,
                            com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Hostile mobs disabled.")
                                    .withStyle(style -> style.withColor(ChatFormatting.RED))
                    );
                    return 1;
                }))
)

                /* ===============================
                   CASINO MODE
                   =============================== */
                .then(CommandUtil.literal("casino")
                        .requires(src -> CommandPermissions.hasPermission(src, 2))

                        .then(CommandUtil.literal("on")
                                .executes(ctx -> {
                                    CasinoModeManager.setCasinoEnabled(true);
                                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                            () -> com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Casino mode enabled."), true);
                                    return 1;
                                }))

                        .then(CommandUtil.literal("off")
                                .executes(ctx -> {
                                    CasinoModeManager.setCasinoEnabled(false);
                                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                            () -> com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Casino mode disabled."), true);
                                    return 1;
                                }))

                        /* ===============================
                           REROLL CONFIG (NEW)
                           =============================== */
                        .then(CommandUtil.literal("reroll")
                                .then(CommandUtil.argument("count",
                                        IntegerArgumentType.integer(0, 25))
                                        .executes(ctx -> {

                                            int count = IntegerArgumentType.getInteger(ctx, "count");
                                            BingoGameData data = BingoGameData.get(
                                                    com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource()));

                                            data.rerollsPerPlayer = count;
                                            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                                            com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                                    () -> com.jamie.jamiebingo.util.ComponentUtil.literal(
                                                            "[Bingo] Casino rerolls per player set to " + count),
                                                    true
                                            );
                                            return 1;
                                        }))))

/* ===============================
   HOSTILE MOBS
   =============================== */
.then(CommandUtil.literal("hostilemobs")
    .requires(src -> CommandPermissions.hasPermission(src, 2))

    // /bingo hostilemobs disabled
    .then(CommandUtil.literal("disabled")
        .executes(ctx -> {

            MinecraftServer server = com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource());
            BingoGameData data = BingoGameData.get(server);

            data.hostileMobsEnabled = false;
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

            com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                () -> com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Hostile mobs disabled (spawns blocked)."),
                true
            );
            return 1;
        })
    )

    // /bingo hostilemobs enabled
    .then(CommandUtil.literal("enabled")
        .executes(ctx -> {

            MinecraftServer server = com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource());
            BingoGameData data = BingoGameData.get(server);

            data.hostileMobsEnabled = true;
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

            com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                () -> com.jamie.jamiebingo.util.ComponentUtil.literal(
                    "[Bingo] Hostile mobs enabled."
                ),
                true
            );
            return 1;
        })
    )
)

                /* ===============================
                   RANDOM EFFECTS
                   =============================== */
                .then(CommandUtil.literal("effects")
                        .requires(src -> CommandPermissions.hasPermission(src, 2))

                        // /bingo effects enabled <seconds>
                        .then(CommandUtil.literal("enabled")
                                .then(CommandUtil.argument("seconds",
                                        IntegerArgumentType.integer(10, 3600))
                                        .executes(ctx -> {

                                            int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
                                            MinecraftServer server = com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource());
                                            BingoGameData data = BingoGameData.get(server);

                                            data.randomEffectsEnabled = true;
                                            data.randomEffectsIntervalSeconds = seconds;
                                            data.randomEffectsNextTick = -1;
                                            data.activeRandomEffectId = "";
                                            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                                            BroadcastHelper.broadcast(
                                                    server,
                                                    com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Random effects enabled (every "
                                                            + seconds + "s).")
                                                            .withStyle(style -> style.withColor(ChatFormatting.GREEN))
                                            );
                                            return 1;
                                        })))

                        // /bingo effects disabled
                        .then(CommandUtil.literal("disabled")
                                .executes(ctx -> {

                                    MinecraftServer server = com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource());
                                    BingoGameData data = BingoGameData.get(server);

                                    data.randomEffectsEnabled = false;
                                    data.randomEffectsIntervalSeconds = 0;
                                    data.randomEffectsNextTick = -1;
                                    data.activeRandomEffectId = "";
                                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                                    BroadcastHelper.broadcast(
                                            server,
                                            com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Random effects disabled.")
                                                    .withStyle(style -> style.withColor(ChatFormatting.RED))
                                    );
                                    return 1;
                                })))

.then(CommandUtil.literal("delay")
    .then(CommandUtil.argument("seconds", IntegerArgumentType.integer(0))
        .executes(ctx -> {
            MinecraftServer server = com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource());
            BingoGameData data = BingoGameData.get(server);

            int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
            data.bingoStartDelaySeconds = seconds;
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

            com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
    () -> com.jamie.jamiebingo.util.ComponentUtil.literal("Bingo start delay set to " + seconds + " seconds"),
    true
);
            return 1;
        })
    )
)

                /* ===============================
                   WIN CONDITION
                   =============================== */
               .then(CommandUtil.literal("win")
    .requires(src -> CommandPermissions.hasPermission(src, 2))

    .then(CommandUtil.literal("line").executes(ctx -> {
        BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource()));
        data.winCondition = WinCondition.LINE;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                () -> com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Win condition set to LINE."), true);
        return 1;
    }))

    .then(CommandUtil.literal("full").executes(ctx -> {
        BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource()));
        data.winCondition = WinCondition.FULL;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                () -> com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Win condition set to FULL CARD."), true);
        return 1;
    }))

    .then(CommandUtil.literal("lockout").executes(ctx -> {
        BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource()));
        data.winCondition = WinCondition.LOCKOUT;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                () -> com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Win condition set to LOCKOUT."), true);
        return 1;
    }))

    .then(CommandUtil.literal("rarity").executes(ctx -> {
        BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource()));
        data.winCondition = WinCondition.RARITY;
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                () -> com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Win condition set to RARITY."), true);
        return 1;
    }))

    // ✅ BLIND
    .then(CommandUtil.literal("blind").executes(ctx -> {
        BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource()));
        data.winCondition = WinCondition.BLIND;
        CasinoModeManager.setCasinoEnabled(false);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                () -> com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Win condition set to BLIND."), true);
        return 1;
    }))

    // 🔫 GUNGAME (per-team progression)
    .then(CommandUtil.literal("gungame")
        .then(CommandUtil.argument("cards", IntegerArgumentType.integer(1, 100))
            .executes(ctx -> {
                int cards = IntegerArgumentType.getInteger(ctx, "cards");
                BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource()));

                CasinoModeManager.setCasinoEnabled(false);

                data.winCondition = WinCondition.GUNGAME;
                data.gunGameLength = cards;
                data.gunGameShared = false;
                com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                        () -> com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Win condition set to GUNGAME (" + cards + " cards)."),
                        true);
                return 1;
            })
        )
    )

    // 🔫 GAMEGUN (shared progression)
    .then(CommandUtil.literal("gamegun")
        .then(CommandUtil.argument("cards", IntegerArgumentType.integer(1, 100))
            .executes(ctx -> {
                int cards = IntegerArgumentType.getInteger(ctx, "cards");
                BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource()));

                CasinoModeManager.setCasinoEnabled(false);

                data.winCondition = WinCondition.GAMEGUN;
                data.gunGameLength = cards;
                data.gunGameShared = true;
                com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                        () -> com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Win condition set to GAMEGUN (" + cards + " cards)."),
                        true);
                return 1;
            })
        )
    )
)

                /* ===============================
                   QUEST SETTINGS
                   =============================== */
                .then(CommandUtil.literal("quest")
                        .requires(src -> CommandPermissions.hasPermission(src, 2))

                        .then(CommandUtil.literal("enabled").executes(ctx -> {
                            BingoGameData data = BingoGameData.get(
                                    com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource()));

                            if (!QuestDatabase.hasQuests()) {
                                com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), 
                                        com.jamie.jamiebingo.util.ComponentUtil.literal("Quest Mode unavailable — no quests loaded."));
                                return 0;
                            }

                            data.composition = CardComposition.HYBRID_CATEGORY;
                            data.questPercent = -1;
                            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                            com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                    () -> com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Quest category hybrid enabled."), true);
                            return 1;
                        }))

                        .then(CommandUtil.literal("disabled").executes(ctx -> {
                            BingoGameData data = BingoGameData.get(
                                    com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource()));

                            data.composition = CardComposition.CLASSIC_ONLY;
                            data.questPercent = -1;
                            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                            com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                    () -> com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Quests disabled."), true);
                            return 1;
                        }))

                        .then(CommandUtil.literal("percent")
                                .then(CommandUtil.argument("value",
                                        IntegerArgumentType.integer(0, 100))
                                        .executes(ctx -> {

                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                            BingoGameData data = BingoGameData.get(
                                                    com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource()));

                                            if (!QuestDatabase.hasQuests()) {
                                                com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), 
                                                        com.jamie.jamiebingo.util.ComponentUtil.literal("Quest Mode unavailable — no quests loaded."));
                                                return 0;
                                            }

                                            data.questPercent = value;
                                            data.composition = CardComposition.HYBRID_PERCENT;
                                            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

                                            com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                                    () -> com.jamie.jamiebingo.util.ComponentUtil.literal(
                                                            "[Bingo] Quest chance set to " + value + "%."),
                                                    true);
                                            return 1;
                                        }))))

                /* ===============================
                   MARK / UNMARK
                   =============================== */
                .then(CommandUtil.literal("mark")
                        .requires(src -> CommandPermissions.hasPermission(src, 2))
                        .then(CommandUtil.argument("x", IntegerArgumentType.integer())
                                .then(CommandUtil.argument("y", IntegerArgumentType.integer())
                                        .then(CommandUtil.argument("player", CommandUtil.player())
                                                .executes(ctx -> {
                                                    MinecraftServer server = com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource());
                                                    BingoGameData data = BingoGameData.get(server);
                                                    if (!data.isActive()) {
                                                        com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), com.jamie.jamiebingo.util.ComponentUtil.literal("No active bingo game."));
                                                        return 0;
                                                    }

                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    TeamData teamData = TeamData.get(server);
                                                    UUID teamId = teamData.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(target));
                                                    if (teamId == null) {
                                                        com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), com.jamie.jamiebingo.util.ComponentUtil.literal("Player is not on a team."));
                                                        return 0;
                                                    }

                                                    BingoCard card = data.getActiveCardForTeam(teamId);
                                                    if (card == null) {
                                                        com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), com.jamie.jamiebingo.util.ComponentUtil.literal("No active card for that player."));
                                                        return 0;
                                                    }

                                                    int size = card.getSize();
                                                    int x = resolveSlotIndex(IntegerArgumentType.getInteger(ctx, "x"), size);
                                                    int y = resolveSlotIndex(IntegerArgumentType.getInteger(ctx, "y"), size);
                                                    if (y >= 0) {
                                                        y = size - 1 - y;
                                                    }
                                                    if (x < 0 || y < 0) {
                                                        com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), com.jamie.jamiebingo.util.ComponentUtil.literal("Invalid slot coordinates."));
                                                        return 0;
                                                    }

                                                    BingoSlot slot = card.getSlot(x, y);
                                                    if (slot == null) {
                                                        com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), com.jamie.jamiebingo.util.ComponentUtil.literal("No slot at that position."));
                                                        return 0;
                                                    }

                                                    boolean ok;
                                                    if (slot.getId().startsWith("quest.")) {
                                                        ok = QuestTracker.completeWithResult(target, slot.getId());
                                                    } else {
                                                        ok = data.markCompleted(com.jamie.jamiebingo.util.EntityUtil.getUUID(target), slot.getId());
                                                    }

                                                    if (!ok) {
                                                        com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), com.jamie.jamiebingo.util.ComponentUtil.literal("Goal was already completed or invalid."));
                                                        return 0;
                                                    }

                                                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                                            () -> com.jamie.jamiebingo.util.ComponentUtil.literal("Marked " + slot.getName() + " for " + target.getName().getString()),
                                                            true
                                                    );
                                                    return 1;
                                                })))))
                .then(CommandUtil.literal("unmark")
                        .requires(src -> CommandPermissions.hasPermission(src, 2))
                        .then(CommandUtil.argument("x", IntegerArgumentType.integer())
                                .then(CommandUtil.argument("y", IntegerArgumentType.integer())
                                        .then(CommandUtil.argument("player", CommandUtil.player())
                                                .executes(ctx -> {
                                                    MinecraftServer server = com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource());
                                                    BingoGameData data = BingoGameData.get(server);
                                                    if (!data.isActive()) {
                                                        com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), com.jamie.jamiebingo.util.ComponentUtil.literal("No active bingo game."));
                                                        return 0;
                                                    }

                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    TeamData teamData = TeamData.get(server);
                                                    UUID teamId = teamData.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(target));
                                                    if (teamId == null) {
                                                        com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), com.jamie.jamiebingo.util.ComponentUtil.literal("Player is not on a team."));
                                                        return 0;
                                                    }

                                                    BingoCard card = data.getActiveCardForTeam(teamId);
                                                    if (card == null) {
                                                        com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), com.jamie.jamiebingo.util.ComponentUtil.literal("No active card for that player."));
                                                        return 0;
                                                    }

                                                    int size = card.getSize();
                                                    int x = resolveSlotIndex(IntegerArgumentType.getInteger(ctx, "x"), size);
                                                    int y = resolveSlotIndex(IntegerArgumentType.getInteger(ctx, "y"), size);
                                                    if (y >= 0) {
                                                        y = size - 1 - y;
                                                    }
                                                    if (x < 0 || y < 0) {
                                                        com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), com.jamie.jamiebingo.util.ComponentUtil.literal("Invalid slot coordinates."));
                                                        return 0;
                                                    }

                                                    BingoSlot slot = card.getSlot(x, y);
                                                    if (slot == null) {
                                                        com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), com.jamie.jamiebingo.util.ComponentUtil.literal("No slot at that position."));
                                                        return 0;
                                                    }

                                                    boolean removed = data.removeCompletedForTeam(teamId, slot.getId());
                                                    if (!removed) {
                                                        com.jamie.jamiebingo.util.CommandSourceUtil.sendFailure(ctx.getSource(), com.jamie.jamiebingo.util.ComponentUtil.literal("Goal was not completed."));
                                                        return 0;
                                                    }

                                                    TeamScoreData scores = TeamScoreData.get(server);
                                                    if (data.winCondition == WinCondition.RARITY) {
                                                        scores.recomputeRarityScores(server, data);
                                                    } else {
                                                        scores.recomputeStandardScores(server, data);
                                                    }

                                                    BroadcastHelper.broadcastProgress(server);
                                                    BroadcastHelper.broadcastSlotOwnership(server);
                                                    BroadcastHelper.broadcastTeamScores(server);

                                                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                                            () -> com.jamie.jamiebingo.util.ComponentUtil.literal("Unmarked " + slot.getName() + " for " + target.getName().getString()),
                                                            true
                                                    );
                                                    return 1;
                                                })))))

                /* ===============================
                   STOP GAME
                   =============================== */
                .then(CommandUtil.literal("stop")
                        .executes(ctx -> {
                            MinecraftServer server = com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource());
                            BingoGameData data = BingoGameData.get(server);
                            com.jamie.jamiebingo.data.TeamChestData.get(server).clearAll();

                            data.stopGame();
                            for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                                player.setHealth(player.getMaxHealth());
                            }
                            BroadcastHelper.broadcast(server,
                                    com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Game stopped."));
                            BroadcastHelper.broadcastFullSync();
                            return 1;
                        }))

                /* ===============================
                   SIZE
                   =============================== */
                .then(CommandUtil.literal("size")
                        .requires(src -> CommandPermissions.hasPermission(src, 2))
                        .then(CommandUtil.argument("value",
                                IntegerArgumentType.integer(1, 10))
                                .executes(ctx -> {
                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                    BingoGameData data = BingoGameData.get(
                                            com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource()));

                                    data.setSize(value);
                                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                            () -> com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Board size set to " + value),
                                            true);
                                    return 1;
                                })))


                /* ===============================
                   DIFFICULTY
                   =============================== */
                .then(CommandUtil.literal("difficulty")
                        .requires(src -> CommandPermissions.hasPermission(src, 2))
                        .then(CommandUtil.argument("value",
                                StringArgumentType.word())
                                .executes(ctx -> {
                                    String value = StringArgumentType.getString(ctx, "value")
                                            .toLowerCase();
                                    BingoGameData data = BingoGameData.get(
                                            com.jamie.jamiebingo.util.CommandSourceUtil.getServer(ctx.getSource()));

                                    data.setDifficulty(value);
                                    com.jamie.jamiebingo.util.CommandSourceUtil.sendSuccess(ctx.getSource(), 
                                            () -> com.jamie.jamiebingo.util.ComponentUtil.literal("[Bingo] Difficulty set to " + value),
                                            true);
                                    return 1;
                                })))


                /* ===============================
                   TEAMS
                   =============================== */
                .then(CommandUtil.literal("teams")
                        .executes(ctx -> {
                            if (CommandSourceUtil.getEntity(ctx.getSource()) instanceof ServerPlayer player) {
                                com.jamie.jamiebingo.util.MenuOpenUtil.open(
                                        player,
                                        new SimpleMenuProvider(
                                                (id, inv, p) -> new TeamSelectMenu(id, inv),
                                                com.jamie.jamiebingo.util.ComponentUtil.literal("Teams")
                                        )
                                );
                                return 1;
                            }
                            return 0;
                        }))

                /* ===============================
                   TEAM CHEST
                   =============================== */
                .then(CommandUtil.literal("teamchest")
                        .executes(ctx -> {
                            if (CommandSourceUtil.getEntity(ctx.getSource()) instanceof ServerPlayer sp) {
                                com.jamie.jamiebingo.util.MenuOpenUtil.open(
                                        sp,
                                        com.jamie.jamiebingo.menu.TeamChestMenuProvider.create(sp)
                                );
                                return 1;
                            }
                            return 0;
                        }));
    }

    private static int resolveSlotIndex(int input, int size) {
        if (input >= 1 && input <= size) return input - 1;
        if (input >= 0 && input < size) return input;
        return -1;
    }
}







