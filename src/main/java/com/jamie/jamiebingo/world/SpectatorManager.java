package com.jamie.jamiebingo.world;


import com.jamie.jamiebingo.util.ServerLevelUtil;
import com.jamie.jamiebingo.util.BlockPosUtil;
import com.jamie.jamiebingo.util.EntityLevelUtil;
import com.jamie.jamiebingo.util.ServerPlayerTeleportUtil;
import com.jamie.jamiebingo.JamieBingo;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.data.BroadcastHelper;
import com.jamie.jamiebingo.menu.TeamSelectMenu;
import com.jamie.jamiebingo.menu.LateJoinMenu;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.core.BlockPos;
import com.jamie.jamiebingo.util.DataComponents;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID)
public final class SpectatorManager {

    private static final String TAG_SPECTATOR_COMPASS = "jamiebingo_spectator_join";
    private static final String TAG_SPECTATOR_HEAD = "jamiebingo_spectator_head";
    private static final String TAG_SPECTATOR_TARGET = "jamiebingo_spectator_target";
    private static final String TAG_SPECTATOR_NEXT = "jamiebingo_spectator_next";
    private static final String TAG_SPECTATOR_PAGE = "jamiebingo_spectator_page";

    private SpectatorManager() {
    }

    public static boolean isSpectator(ServerPlayer player) {
        if (player == null) return false;
        BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        return data != null && data.isSpectator(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
    }

    public static void makeSpectator(ServerPlayer player, BingoGameData data) {
        if (player == null || data == null) return;

        UUID id = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
        data.participants.remove(id);
        data.spectators.add(id);
        data.lateJoinPending.remove(id);
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

     if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
         com.jamie.jamiebingo.util.GameModeTracking.runWithoutCommandTracking(player, () -> player.setGameMode(GameType.SPECTATOR));
     }

        if (data.getSpectatorViewTarget(id) == null) {
            UUID target = pickDefaultTarget(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player), data, id);
            data.setSpectatorViewTarget(id, target);
        }

        refreshSpectatorHotbar(player, data);
        BroadcastHelper.syncPlayerJoin(player);
    }

    public static void finishLateJoin(ServerPlayer player, BingoGameData data) {
        if (player == null || data == null) return;

        UUID id = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
        data.spectators.remove(id);
        data.participants.add(id);
        data.lateJoinPending.remove(id);
        data.spectatorViewTargets.remove(id);
        data.lockQuestsForPlayer(id);
        if (!data.pregameBoxActive) {
            data.releaseQuestsForPlayer(id);
        }
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

     GameType targetMode = data.adventureMode ? GameType.ADVENTURE : GameType.SURVIVAL;
     if (player.gameMode.getGameModeForPlayer() != targetMode) {
         com.jamie.jamiebingo.util.GameModeTracking.runWithoutCommandTracking(player, () -> player.setGameMode(targetMode));
     }

        com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player).clearContent();
        BroadcastHelper.syncPlayerJoin(player);
    }

    public static void beginLateJoin(ServerPlayer player, BingoGameData data) {
        if (player == null || data == null) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;

        UUID id = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
        data.spectators.remove(id);
        data.participants.add(id);
        data.lateJoinPending.add(id);
        data.spectatorViewTargets.remove(id);
        data.lockQuestsForPlayer(id);
        if (!data.pregameBoxActive) {
            data.releaseQuestsForPlayer(id);
        }
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);

      GameType targetMode = data.adventureMode ? GameType.ADVENTURE : GameType.SURVIVAL;
      if (player.gameMode.getGameModeForPlayer() != targetMode) {
          com.jamie.jamiebingo.util.GameModeTracking.runWithoutCommandTracking(player, () -> player.setGameMode(targetMode));
      }

        com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player).clearContent();
        BroadcastHelper.syncPlayerJoin(player);

        ServerLevel overworld = ServerLevelUtil.getOverworld(server);
        if (overworld != null) {
            BlockPos spawn = data.lastGameSpawnSet
                    ? new BlockPos(data.lastGameSpawnX, data.lastGameSpawnY, data.lastGameSpawnZ)
                    : com.jamie.jamiebingo.util.LevelSpawnUtil.getSpawnPos(overworld);
            ServerPlayerTeleportUtil.teleport(
                    player,
                    overworld,
                    BlockPosUtil.getX(spawn) + 0.5,
                    BlockPosUtil.getY(spawn) + 1.0,
                    BlockPosUtil.getZ(spawn) + 0.5,
                    Set.of(),
                    com.jamie.jamiebingo.util.EntityRotationUtil.getYRot(player),
                    com.jamie.jamiebingo.util.EntityRotationUtil.getXRot(player),
                    false
            );
        }
    }

    public static void refreshSpectatorHotbar(ServerPlayer player, BingoGameData data) {
        if (player == null || data == null) return;
        if (!data.isSpectator(com.jamie.jamiebingo.util.EntityUtil.getUUID(player))) return;

        Inventory inv = com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player);
        inv.clearContent();

        boolean allowLateJoin = data.allowLateJoin;
        int headStart = allowLateJoin ? 1 : 0;
        int headEnd = 7;

        int page = getPage(player);
        List<ServerPlayer> targets = getActivePlayers(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player), data);
        UUID currentTarget = data.getSpectatorViewTarget(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
        if (currentTarget == null || targets.stream().noneMatch(p -> com.jamie.jamiebingo.util.EntityUtil.getUUID(p).equals(currentTarget))) {
            UUID fallback = pickDefaultTarget(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player), data, com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
            data.setSpectatorViewTarget(com.jamie.jamiebingo.util.EntityUtil.getUUID(player), fallback);
        }

        int perPage = (headEnd - headStart + 1);
        int maxPage = Math.max(0, (targets.size() - 1) / perPage);
        if (page > maxPage) {
            page = maxPage;
            setPage(player, page);
        }

        int startIndex = page * perPage;
        int endIndex = Math.min(targets.size(), startIndex + perPage);

        int slot = headStart;
        for (int i = startIndex; i < endIndex; i++) {
            ServerPlayer target = targets.get(i);
            inv.setItem(slot, createPlayerHead(target));
            slot++;
        }

        if (allowLateJoin) {
            inv.setItem(0, createLateJoinCompass());
        }

        if (targets.size() > perPage) {
            inv.setItem(8, createNextPagePaper(page, maxPage));
        } else {
            inv.setItem(8, createNextPagePaper(0, 0));
        }

        inv.setChanged();
        com.jamie.jamiebingo.util.InventoryUtil.getEquipment(inv).clear();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        if (com.jamie.jamiebingo.util.LevelSideUtil.isClientSide(EntityLevelUtil.getLevel(player))) return;
        if (com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(player) % 20 != 0) return;

        BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        if (data == null || !data.isActive()) return;
        if (!data.isSpectator(com.jamie.jamiebingo.util.EntityUtil.getUUID(player))) return;

        refreshSpectatorHotbar(player, data);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (com.jamie.jamiebingo.util.LevelSideUtil.isClientSide(event.getLevel())) return;

        BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        if (data == null || !data.isActive()) return;
        if (!data.isSpectator(com.jamie.jamiebingo.util.EntityUtil.getUUID(player))) return;

        ItemStack stack = event.getItemStack();
        if (isLateJoinCompass(stack)) {
            if (data.allowLateJoin) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new LateJoinMenu(id, inv),
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Join Late?")
                ));
            }
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
            return;
        }

        if (isNextPagePaper(stack)) {
            int perPage = data.allowLateJoin ? 7 : 8;
            int maxPage = Math.max(0, (getActivePlayers(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player), data).size() - 1) / perPage);
            int page = getPage(player) + 1;
            if (page > maxPage) {
                page = 0;
            }
            setPage(player, page);
            refreshSpectatorHotbar(player, data);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
            return;
        }

        UUID targetId = getSpectatorTarget(stack);
        if (targetId != null) {
            var server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
            ServerPlayer target = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, targetId);
            if (target != null) {
                ServerPlayerTeleportUtil.teleport(
                        player,
                        (ServerLevel) EntityLevelUtil.getLevel(target),
                        target.getX(),
                        target.getY(),
                        target.getZ(),
                        Set.of(),
                        com.jamie.jamiebingo.util.EntityRotationUtil.getYRot(target),
                        com.jamie.jamiebingo.util.EntityRotationUtil.getXRot(target),
                        false
                );
                data.setSpectatorViewTarget(com.jamie.jamiebingo.util.EntityUtil.getUUID(player), targetId);
                BroadcastHelper.syncPlayerJoin(player);
            }
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (isSpectator(player)) {
            event.setResult(net.minecraftforge.common.util.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (isSpectator(player)) {
            ItemStack stack = event.getEntity().getItem();
            event.getEntity().discard();
            com.jamie.jamiebingo.util.PlayerInventoryUtil.addItem(player, stack);
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (com.jamie.jamiebingo.util.LevelSideUtil.isClientSide(EntityLevelUtil.getLevel(player))) return;

        BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
        if (data == null || !data.isActive()) return;
        if (!data.isLateJoinPending(com.jamie.jamiebingo.util.EntityUtil.getUUID(player))) return;
        if (!(event.getContainer() instanceof TeamSelectMenu)) return;

        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;
        com.jamie.jamiebingo.casino.CasinoTickScheduler.schedule(
                server,
                com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + 1,
                () -> player.openMenu(new SimpleMenuProvider(
                        (i, inv, p) -> new TeamSelectMenu(i, inv),
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Teams")
                ))
        );
    }

    private static List<ServerPlayer> getActivePlayers(MinecraftServer server, BingoGameData data) {
        List<ServerPlayer> out = new ArrayList<>();
        if (server == null || data == null) return out;
        for (UUID id : data.participants) {
            ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, id);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    private static UUID pickDefaultTarget(MinecraftServer server, BingoGameData data, UUID self) {
        if (server == null || data == null) return null;
        for (UUID id : data.participants) {
            if (id.equals(self)) continue;
            ServerPlayer p = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, id);
            if (p != null) return id;
        }
        return null;
    }

    private static ItemStack createLateJoinCompass() {
        ItemStack stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:compass");
        com.jamie.jamiebingo.util.ItemStackComponentUtil.set(
                stack,
                DataComponents.CUSTOM_NAME,
                com.jamie.jamiebingo.util.ComponentUtil.literal("join in late?")
        );
        com.jamie.jamiebingo.util.ItemStackComponentUtil.updateCustomData(
                stack,
                tag -> com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, TAG_SPECTATOR_COMPASS, true)
        );
        return stack;
    }

    private static ItemStack createNextPagePaper(int page, int maxPage) {
        ItemStack stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:paper");
        com.jamie.jamiebingo.util.ItemStackComponentUtil.set(
                stack,
                DataComponents.CUSTOM_NAME,
                com.jamie.jamiebingo.util.ComponentUtil.literal("next page")
        );
        com.jamie.jamiebingo.util.ItemStackComponentUtil.updateCustomData(stack, tag -> {
            com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, TAG_SPECTATOR_NEXT, true);
            com.jamie.jamiebingo.util.NbtUtil.putInt(tag, TAG_SPECTATOR_PAGE, page);
        });
        return stack;
    }

    private static ItemStack createPlayerHead(ServerPlayer target) {
        ItemStack head = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:player_head");
        com.jamie.jamiebingo.util.ItemStackComponentUtil.set(
                head,
                DataComponents.CUSTOM_NAME,
                com.jamie.jamiebingo.util.ComponentUtil.literal(target.getName().getString())
        );
        com.jamie.jamiebingo.util.ItemStackComponentUtil.set(
                head,
                DataComponents.PROFILE,
                ResolvableProfile.createResolved(target.getGameProfile())
        );
        com.jamie.jamiebingo.util.ItemStackComponentUtil.updateCustomData(head, tag -> {
            com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, TAG_SPECTATOR_HEAD, true);
            putUuid(tag, TAG_SPECTATOR_TARGET, com.jamie.jamiebingo.util.EntityUtil.getUUID(target));
        });
        return head;
    }

    private static boolean isLateJoinCompass(ItemStack stack) {
        if (stack == null || !stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:compass"))) return false;
        CompoundTag tag = getCustomDataTag(stack);
        return com.jamie.jamiebingo.util.NbtUtil.getBoolean(tag, TAG_SPECTATOR_COMPASS, false);
    }

    private static boolean isNextPagePaper(ItemStack stack) {
        if (stack == null || !stack.is(com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:paper"))) return false;
        CompoundTag tag = getCustomDataTag(stack);
        return com.jamie.jamiebingo.util.NbtUtil.getBoolean(tag, TAG_SPECTATOR_NEXT, false);
    }

    private static UUID getSpectatorTarget(ItemStack stack) {
        if (stack == null) return null;
        CompoundTag tag = getCustomDataTag(stack);
        if (!com.jamie.jamiebingo.util.NbtUtil.getBoolean(tag, TAG_SPECTATOR_HEAD, false)) return null;
        if (!hasUuid(tag, TAG_SPECTATOR_TARGET)) return null;
        return getUuid(tag, TAG_SPECTATOR_TARGET);
    }

    private static boolean hasUuid(CompoundTag tag, String key) {
        return tag.getString(key).isPresent() || tag.getIntArray(key).isPresent();
    }

    private static UUID getUuid(CompoundTag tag, String key) {
        String raw = com.jamie.jamiebingo.util.NbtUtil.getString(tag, key, null);
        if (raw != null && !raw.isBlank()) {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
            }
        }
        int[] ints = com.jamie.jamiebingo.util.NbtUtil.getIntArray(tag, key);
        if (ints != null && ints.length == 4) {
            return UUIDUtil.uuidFromIntArray(ints);
        }
        return null;
    }

    private static void putUuid(CompoundTag tag, String key, UUID value) {
        if (value == null) return;
        tag.putIntArray(key, UUIDUtil.uuidToIntArray(value));
    }

    private static CompoundTag getCustomDataTag(ItemStack stack) {
        CustomData data = com.jamie.jamiebingo.util.ItemStackComponentUtil.getOrDefault(
                stack,
                DataComponents.CUSTOM_DATA,
                com.jamie.jamiebingo.util.ItemStackComponentUtil.emptyCustomData()
        );
        return com.jamie.jamiebingo.util.ItemStackComponentUtil.copyCustomDataTag(data);
    }

    private static int getPage(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        return com.jamie.jamiebingo.util.NbtUtil.getInt(tag, TAG_SPECTATOR_PAGE, 0);
    }

    private static void setPage(ServerPlayer player, int page) {
        CompoundTag tag = player.getPersistentData();
        com.jamie.jamiebingo.util.NbtUtil.putInt(tag, TAG_SPECTATOR_PAGE, Math.max(0, page));
    }
}



















