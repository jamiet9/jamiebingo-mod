package com.jamie.jamiebingo.item;

import com.jamie.jamiebingo.data.TeamData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import com.jamie.jamiebingo.util.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import com.jamie.jamiebingo.menu.TeamSelectMenu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlayerTrackerHandler {
    private static final Logger LOGGER = LogManager.getLogger(com.jamie.jamiebingo.JamieBingo.MOD_ID);

    private static final String TAG_TRACKER = "jamiebingo_player_tracker";
    private static final String TAG_TRACKER_TEAM = "jamiebingo_tracker_team";
    private static final String TAG_TRACKER_PLAYER = "jamiebingo_tracker_player";
    private static final String TAG_TEAM_SELECT = "jamiebingo_team_select";

    public static ItemStack createTrackerItem(MinecraftServer server, ServerPlayer holder) {
        ItemStack stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:compass");
        com.jamie.jamiebingo.util.ItemStackComponentUtil.set(
                stack,
                DataComponents.CUSTOM_NAME,
                com.jamie.jamiebingo.util.ComponentUtil.literal("Player Tracker")
        );
        com.jamie.jamiebingo.util.ItemStackComponentUtil.setHoverName(
                stack,
                com.jamie.jamiebingo.util.ComponentUtil.literal("Player Tracker")
        );
        com.jamie.jamiebingo.util.ItemStackComponentUtil.updateCustomData(
                stack,
                tag -> com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, TAG_TRACKER, true)
        );
        if (server != null && holder != null) {
            selectInitialTarget(server, holder, stack);
        }
        return stack;
    }

    public static ItemStack createTeamSelectItem() {
        ItemStack stack = new ItemStack(ModItems.TEAM_SELECTOR.get());
        applyTeamSelectTag(stack);
        return stack;
    }

    public static boolean shouldGiveTracker(MinecraftServer server) {
        if (server == null) return false;
        TeamData data = TeamData.get(server);
        int teamsWithMembers = 0;
        for (TeamData.TeamInfo team : data.getTeams()) {
            if (!team.members.isEmpty() && ++teamsWithMembers > 1) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPlayerTracker(ItemStack stack) {
        return isTracker(stack);
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (com.jamie.jamiebingo.util.LevelSideUtil.isClientSide(event.getLevel())) return;

        ItemStack stack = event.getItemStack();

        // Ensure launcher-safe behavior: route use via event handler for our items.
        if (stack != null) {
            net.minecraft.world.item.Item item = com.jamie.jamiebingo.util.ItemStackUtil.getItem(stack);
            if (item instanceof BingoControllerItem controller) {
                controller.use(event.getLevel(), player, event.getHand());
                event.setCancellationResult(com.jamie.jamiebingo.util.InteractionResultUtil.success());
                return;
            }
            if (item instanceof CustomCardMakerItem maker) {
                maker.use(event.getLevel(), player, event.getHand());
                event.setCancellationResult(com.jamie.jamiebingo.util.InteractionResultUtil.success());
                return;
            }
            if (item instanceof CardLayoutConfiguratorItem configurator) {
                configurator.use(event.getLevel(), player, event.getHand());
                event.setCancellationResult(com.jamie.jamiebingo.util.InteractionResultUtil.success());
                return;
            }
            if (item instanceof BlacklistItemsQuestItem blacklist) {
                blacklist.use(event.getLevel(), player, event.getHand());
                event.setCancellationResult(com.jamie.jamiebingo.util.InteractionResultUtil.success());
                return;
            }
            if (item instanceof GameHistoryItem history) {
                history.use(event.getLevel(), player, event.getHand());
                event.setCancellationResult(com.jamie.jamiebingo.util.InteractionResultUtil.success());
                return;
            }
            if (item instanceof TeamSelectorItem selector) {
                selector.use(event.getLevel(), player, event.getHand());
                event.setCancellationResult(com.jamie.jamiebingo.util.InteractionResultUtil.success());
                return;
            }
        }

        if (isTeamSelect(stack)) {
            com.jamie.jamiebingo.util.MenuOpenUtil.open(
                    player,
                    new SimpleMenuProvider(
                            (id, inv, p) -> new TeamSelectMenu(id, inv),
                            com.jamie.jamiebingo.util.ComponentUtil.literal("Teams")
                    )
            );
            SoundEvent click = com.jamie.jamiebingo.util.SoundUtil.create(
                    com.jamie.jamiebingo.util.IdUtil.id("minecraft:ui.button.click")
            );
            com.jamie.jamiebingo.util.SoundUtil.playToPlayer(player, click, 0.6f, 1.0f);
            event.setCancellationResult(com.jamie.jamiebingo.util.InteractionResultUtil.success());
            return;
        }

        if (!isTracker(stack)) return;

        if (player.isShiftKeyDown()) {
            cycleTeam(player, stack);
        } else {
            cyclePlayer(player, stack);
        }

        event.setCancellationResult(com.jamie.jamiebingo.util.InteractionResultUtil.success());
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        if (com.jamie.jamiebingo.util.LevelSideUtil.isClientSide(com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(player))) return;

        ItemStack stack = com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(player, EquipmentSlot.MAINHAND);
        if (!isTracker(stack)) return;

        if (com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(player) % 10 != 0) return;

        ensureValidTarget(player, stack);
        updateCompassTarget(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player), player, stack);
        sendActionBar(player, stack);
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;
        if (!shouldGiveTracker(server)) return;

        com.jamie.jamiebingo.data.BingoGameData data = com.jamie.jamiebingo.data.BingoGameData.get(server);
        if (data == null || !data.isActive() || data.keepInventoryEnabled) return;

        if (hasTracker(player)) return;

        ItemStack tracker = createTrackerItem(server, player);
        com.jamie.jamiebingo.util.PlayerInventoryUtil.addItem(player, tracker);
    }

    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null || !shouldGiveTracker(server)) return;
        event.getDrops().removeIf(drop -> {
            if (drop == null) return false;
            ItemStack stack = drop.getItem();
            return isTracker(stack);
        });
    }

    public static boolean hasTracker(ServerPlayer player) {
        if (player == null) return false;
        return com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player)).stream().anyMatch(PlayerTrackerHandler::isTracker)
                || isTracker(com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(player, EquipmentSlot.OFFHAND));
    }

    public static boolean hasTeamSelect(ServerPlayer player) {
        if (player == null) return false;
        return com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player)).stream().anyMatch(PlayerTrackerHandler::isTeamSelect)
                || isTeamSelect(com.jamie.jamiebingo.util.InventoryUtil.getEquipmentItem(player, EquipmentSlot.OFFHAND));
    }

    public static void normalizeTeamSelect(ServerPlayer player) {
        if (player == null) return;
        boolean hasTeamSelect = hasTeamSelect(player);
        boolean upgraded = false;
        // Try to find a plain paper stack and upgrade it to a Team Select item.
        for (ItemStack stack : com.jamie.jamiebingo.util.InventoryUtil.getNonEquipmentItems(
                com.jamie.jamiebingo.util.PlayerInventoryUtil.getInventory(player))) {
            if (stack == null) continue;
            boolean isLegacyPaper = com.jamie.jamiebingo.util.ItemLookupUtil.is(stack, "minecraft:paper");
            boolean isNewSelector = stack.getItem() == ModItems.TEAM_SELECTOR.get();
            if (!isLegacyPaper && !isNewSelector) continue;
            String name = com.jamie.jamiebingo.util.ItemStackNameUtil.getHoverNameString(stack);
            boolean isPlain = name == null || name.isBlank() || "paper".equalsIgnoreCase(name);
            if (!hasTeamSelect || isPlain) {
                if (isLegacyPaper) {
                    stack.shrink(1);
                    stack = createTeamSelectItem();
                    com.jamie.jamiebingo.util.PlayerInventoryUtil.addItem(player, stack);
                } else {
                    applyTeamSelectTag(stack);
                }
                upgraded = true;
                break;
            }
        }
        if (!hasTeamSelect && !upgraded) {
            com.jamie.jamiebingo.util.PlayerInventoryUtil.addItem(player, createTeamSelectItem());
        }
    }

    private static void applyTeamSelectTag(ItemStack stack) {
        if (stack == null) return;
        com.jamie.jamiebingo.util.DataComponents.refresh();
        var name = com.jamie.jamiebingo.util.ComponentUtil.literal("Team Select");
        try {
            stack.set(DataComponents.CUSTOM_NAME, name);
        } catch (Throwable ignored) {
        }
        try {
            if (DataComponents.CUSTOM_NAME != null) {
                Class<?> typedClass = Class.forName("net.minecraft.core.component.TypedDataComponent");
                java.lang.reflect.Constructor<?> ctor = typedClass.getConstructor(
                        net.minecraft.core.component.DataComponentType.class, Object.class);
                Object typed = ctor.newInstance(DataComponents.CUSTOM_NAME, name);
                java.lang.reflect.Method setTyped = stack.getClass().getMethod("set", typedClass);
                setTyped.invoke(stack, typed);
            }
        } catch (Throwable ignored) {
        }
        com.jamie.jamiebingo.util.ItemStackComponentUtil.setHoverName(stack, name);
        // Fallbacks for launcher environments where hover name may not persist.
        try {
            java.lang.reflect.Method m = stack.getClass().getMethod("setHoverName", net.minecraft.network.chat.Component.class);
            m.invoke(stack, name);
        } catch (Throwable ignored) {
        }
        try {
            java.lang.reflect.Method m = stack.getClass().getMethod("getOrCreateTag");
            Object out = m.invoke(stack);
            if (out instanceof net.minecraft.nbt.CompoundTag tag) {
                net.minecraft.nbt.CompoundTag display = new net.minecraft.nbt.CompoundTag();
                com.jamie.jamiebingo.util.NbtUtil.putString(
                        display,
                        "Name",
                        com.jamie.jamiebingo.util.ComponentUtil.toJson(name)
                );
                com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "display", display);
            }
        } catch (Throwable ignored) {
        }
        com.jamie.jamiebingo.util.ItemStackComponentUtil.updateCustomData(
                stack,
                tag -> com.jamie.jamiebingo.util.NbtUtil.putBoolean(tag, TAG_TEAM_SELECT, true)
        );
        com.jamie.jamiebingo.util.ItemStackComponentUtil.putLegacyBoolean(stack, TAG_TEAM_SELECT, true);
        String appliedName = com.jamie.jamiebingo.util.ItemStackNameUtil.getHoverNameString(stack);
        LOGGER.info("[JamieBingo] TeamSelect name applied: {}", appliedName);
    }

    private static boolean isTracker(ItemStack stack) {
        if (stack == null || !com.jamie.jamiebingo.util.ItemLookupUtil.is(stack, "minecraft:compass")) return false;
        CompoundTag tag = getCustomDataTag(stack);
        if (com.jamie.jamiebingo.util.NbtUtil.getBoolean(tag, TAG_TRACKER, false)) return true;
        String name = com.jamie.jamiebingo.util.ItemStackNameUtil.getHoverNameString(stack);
        return "Player Tracker".equalsIgnoreCase(name);
    }

    private static boolean isTeamSelect(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getItem() == ModItems.TEAM_SELECTOR.get()) return true;
        if (!com.jamie.jamiebingo.util.ItemLookupUtil.is(stack, "minecraft:paper")) return false;
        CompoundTag tag = getCustomDataTag(stack);
        if (com.jamie.jamiebingo.util.NbtUtil.getBoolean(tag, TAG_TEAM_SELECT, false)) return true;
        String name = com.jamie.jamiebingo.util.ItemStackNameUtil.getHoverNameString(stack);
        return "Team Select".equalsIgnoreCase(name);
    }

    private static void selectInitialTarget(MinecraftServer server, ServerPlayer holder, ItemStack stack) {
        List<TeamData.TeamInfo> teams = getTrackableTeams(server, com.jamie.jamiebingo.util.EntityUtil.getUUID(holder));
        if (teams.isEmpty()) {
            clearTarget(stack);
            return;
        }

        TeamData.TeamInfo team = teams.get(0);
        List<ServerPlayer> players = getTrackablePlayers(server, team, com.jamie.jamiebingo.util.EntityUtil.getUUID(holder));
        if (players.isEmpty()) {
            clearTarget(stack);
            return;
        }

        setTarget(stack, team.id, com.jamie.jamiebingo.util.EntityUtil.getUUID(players.get(0)));
    }

    private static void cycleTeam(ServerPlayer holder, ItemStack stack) {
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(holder);
        if (server == null) return;

        List<TeamData.TeamInfo> teams = getTrackableTeams(server, com.jamie.jamiebingo.util.EntityUtil.getUUID(holder));
        if (teams.isEmpty()) {
            clearTarget(stack);
            return;
        }

        UUID currentTeamId = getTrackedTeam(stack);
        int index = -1;
        for (int i = 0; i < teams.size(); i++) {
            if (teams.get(i).id.equals(currentTeamId)) {
                index = i;
                break;
            }
        }

        int next = (index + 1) % teams.size();
        TeamData.TeamInfo team = teams.get(next);
        List<ServerPlayer> players = getTrackablePlayers(server, team, com.jamie.jamiebingo.util.EntityUtil.getUUID(holder));
        if (players.isEmpty()) {
            clearTarget(stack);
            return;
        }

        setTarget(stack, team.id, com.jamie.jamiebingo.util.EntityUtil.getUUID(players.get(0)));
    }

    private static void cyclePlayer(ServerPlayer holder, ItemStack stack) {
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(holder);
        if (server == null) return;

        UUID teamId = getTrackedTeam(stack);
        if (teamId == null) {
            cycleTeam(holder, stack);
            return;
        }

        TeamData.TeamInfo team = TeamData.get(server).getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);
        if (team == null) {
            cycleTeam(holder, stack);
            return;
        }

        List<ServerPlayer> players = getTrackablePlayers(server, team, com.jamie.jamiebingo.util.EntityUtil.getUUID(holder));
        if (players.isEmpty()) {
            cycleTeam(holder, stack);
            return;
        }

        UUID currentPlayerId = getTrackedPlayer(stack);
        int index = -1;
        for (int i = 0; i < players.size(); i++) {
            if (com.jamie.jamiebingo.util.EntityUtil.getUUID(players.get(i)).equals(currentPlayerId)) {
                index = i;
                break;
            }
        }

        int next = (index + 1) % players.size();
        setTarget(stack, team.id, com.jamie.jamiebingo.util.EntityUtil.getUUID(players.get(next)));
    }

    private static void ensureValidTarget(ServerPlayer holder, ItemStack stack) {
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(holder);
        if (server == null) return;

        UUID teamId = getTrackedTeam(stack);
        UUID playerId = getTrackedPlayer(stack);

        if (teamId == null || playerId == null) {
            selectInitialTarget(server, holder, stack);
            return;
        }

        ServerPlayer target = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, playerId);
        if (target == null || com.jamie.jamiebingo.util.EntityUtil.getUUID(target).equals(com.jamie.jamiebingo.util.EntityUtil.getUUID(holder))) {
            selectInitialTarget(server, holder, stack);
            return;
        }

        TeamData teamData = TeamData.get(server);
        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);
        if (team == null || !team.members.contains(playerId)) {
            selectInitialTarget(server, holder, stack);
        }
    }

    private static void sendActionBar(ServerPlayer holder, ItemStack stack) {
        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(holder);
        if (server == null) return;

        UUID teamId = getTrackedTeam(stack);
        UUID playerId = getTrackedPlayer(stack);
        if (teamId == null || playerId == null) {
            holder.displayClientMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Tracking: None"), true);
            return;
        }

        TeamData teamData = TeamData.get(server);
        TeamData.TeamInfo team = teamData.getTeams().stream()
                .filter(t -> t.id.equals(teamId))
                .findFirst()
                .orElse(null);
        ServerPlayer target = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, playerId);

        if (team == null || target == null) {
            holder.displayClientMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Tracking: None"), true);
            return;
        }

        holder.displayClientMessage(
                com.jamie.jamiebingo.util.ComponentUtil.literal("Tracking: Team " + team.color.getName() + " | " + target.getName().getString()),
                true
        );
    }

    private static void updateCompassTarget(MinecraftServer server, ServerPlayer holder, ItemStack stack) {
        UUID playerId = getTrackedPlayer(stack);
        if (server == null || playerId == null) {
            pointToSpawn(server, stack);
            return;
        }

        ServerPlayer target = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, playerId);
        if (target == null) {
            pointToSpawn(server, stack);
            return;
        }

        net.minecraft.world.level.Level level = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(target);
        if (level != null) {
            setCompassTarget(stack, level.dimension(), com.jamie.jamiebingo.util.EntityPosUtil.getBlockPos(target));
        }
    }

    private static void pointToSpawn(MinecraftServer server, ItemStack stack) {
        if (server == null) return;
        BlockPos spawn = server.getRespawnData().pos();
        setCompassTarget(stack, server.getRespawnData().dimension(), spawn);
    }

    private static void setTarget(ItemStack stack, UUID teamId, UUID playerId) {
        com.jamie.jamiebingo.util.ItemStackComponentUtil.updateCustomData(stack, tag -> {
            putUuid(tag, TAG_TRACKER_TEAM, teamId);
            putUuid(tag, TAG_TRACKER_PLAYER, playerId);
        });
    }

    private static void clearTarget(ItemStack stack) {
        com.jamie.jamiebingo.util.ItemStackComponentUtil.updateCustomData(stack, tag -> {
            tag.remove(TAG_TRACKER_TEAM);
            tag.remove(TAG_TRACKER_PLAYER);
        });
    }

    private static UUID getTrackedTeam(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        if (tag == null || !hasUuid(tag, TAG_TRACKER_TEAM)) return null;
        return getUuid(tag, TAG_TRACKER_TEAM);
    }

    private static UUID getTrackedPlayer(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        if (tag == null || !hasUuid(tag, TAG_TRACKER_PLAYER)) return null;
        return getUuid(tag, TAG_TRACKER_PLAYER);
    }

    private static boolean hasUuid(CompoundTag tag, String key) {
        return com.jamie.jamiebingo.util.NbtUtil.getString(tag, key, null) != null
                || com.jamie.jamiebingo.util.NbtUtil.getIntArray(tag, key) != null;
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
        if (stack == null) return new CompoundTag();
        return com.jamie.jamiebingo.util.ItemStackComponentUtil.getCustomDataTagOrLegacy(stack);
    }

    private static void setCompassTarget(ItemStack stack, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos pos) {
        if (stack == null || dimension == null || pos == null) return;
        com.jamie.jamiebingo.util.ItemStackComponentUtil.set(
                stack,
                DataComponents.LODESTONE_TRACKER,
                new LodestoneTracker(java.util.Optional.of(GlobalPos.of(dimension, pos)), false)
        );
    }

    private static List<TeamData.TeamInfo> getTrackableTeams(MinecraftServer server, UUID holderId) {
        List<TeamData.TeamInfo> out = new ArrayList<>();
        TeamData teamData = TeamData.get(server);
        for (TeamData.TeamInfo team : teamData.getTeams()) {
            List<ServerPlayer> players = getTrackablePlayers(server, team, holderId);
            if (!players.isEmpty()) {
                out.add(team);
            }
        }
        out.sort(Comparator.comparingInt(t -> t.color.getId()));
        return out;
    }

    private static List<ServerPlayer> getTrackablePlayers(
            MinecraftServer server,
            TeamData.TeamInfo team,
            UUID holderId
    ) {
        List<ServerPlayer> players = new ArrayList<>();
        for (UUID member : team.members) {
            if (member.equals(holderId)) continue;
            ServerPlayer player = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, member);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }
}








