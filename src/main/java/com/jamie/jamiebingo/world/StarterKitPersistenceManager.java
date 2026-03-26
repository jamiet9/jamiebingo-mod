package com.jamie.jamiebingo.world;

import com.jamie.jamiebingo.JamieBingo;
import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID)
public final class StarterKitPersistenceManager {
    private StarterKitPersistenceManager() {
    }

    private static final String TAG_STARTER_KIT_ITEM = "jamiebingo_starter_kit_item";
    private static final Map<UUID, List<ItemStack>> pendingRestoreByPlayer = new HashMap<>();

    public static void markStarterKitItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        com.jamie.jamiebingo.util.ItemStackComponentUtil.updateCustomData(
                stack,
                tag -> com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, TAG_STARTER_KIT_ITEM, true)
        );
        com.jamie.jamiebingo.util.ItemStackComponentUtil.putLegacyBoolean(stack, TAG_STARTER_KIT_ITEM, true);
    }

    public static boolean isStarterKitItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        CompoundTag tag = com.jamie.jamiebingo.util.ItemStackComponentUtil.getCustomDataTagOrLegacy(stack);
        return com.jamie.jamiebingo.util.NbtUtil.getBoolean(tag, TAG_STARTER_KIT_ITEM, false);
    }

    public static void restoreOnClone(ServerPlayer player) {
        if (player == null) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        BingoGameData data = server == null ? null : BingoGameData.get(server);
        List<ItemStack> toRestore = pendingRestoreByPlayer.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
        if (toRestore == null || toRestore.isEmpty()) return;
        for (ItemStack stack : toRestore) {
            if (stack == null || stack.isEmpty()) continue;
            ItemStack copy = stack.copy();
            if (isStarterKitBoots(copy)) {
                ItemStack currentFeet = com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(player, EquipmentSlot.FEET);
                boolean wearingDifferentBoots = currentFeet != null && !currentFeet.isEmpty() && !isStarterKitItem(currentFeet);
                if (!wearingDifferentBoots || data == null || !data.keepInventoryEnabled) {
                    if (currentFeet == null || currentFeet.isEmpty() || isStarterKitItem(currentFeet)) {
                        player.setItemSlot(EquipmentSlot.FEET, copy);
                        continue;
                    }
                }
            }
            if (!com.jamie.jamiebingo.util.PlayerInventoryUtil.addItem(player, copy)) {
                player.drop(copy, false);
            }
        }
    }

    private static boolean isStarterKitBoots(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && isStarterKitItem(stack)
                && stack.is(Items.LEATHER_BOOTS);
    }

    private static boolean shouldHandleFor(ServerPlayer player, BingoGameData data) {
        if (player == null || data == null) return false;
        if (!data.isActive()) return false;
        if (data.starterKitMode <= BingoGameData.STARTER_KIT_DISABLED) return false;
        return data.isParticipant(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
    }

    private static void ensureOpNightVision(ServerPlayer player, BingoGameData data) {
        if (player == null || data == null) return;
        if (!shouldHandleFor(player, data) || data.starterKitMode != BingoGameData.STARTER_KIT_OP) return;
        MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
        if (current == null || current.getDuration() < 120 || current.getAmplifier() != 0) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false, true));
        }
    }

    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (!shouldHandleFor(player, data)) return;

        List<ItemStack> captured = new ArrayList<>();
        Iterator<ItemEntity> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemEntity drop = it.next();
            if (drop == null) continue;
            ItemStack stack = drop.getItem();
            if (!isStarterKitItem(stack)) continue;
            captured.add(stack.copy());
            it.remove();
        }
        if (!captured.isEmpty()) {
            pendingRestoreByPlayer.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(player), captured);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        ensureOpNightVision(player, data);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        if ((player.tickCount % 20) != 0) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        ensureOpNightVision(player, data);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        pendingRestoreByPlayer.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
    }

    @SubscribeEvent
    public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        pendingRestoreByPlayer.clear();
    }
}
