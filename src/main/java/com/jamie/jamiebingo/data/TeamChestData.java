package com.jamie.jamiebingo.data;


import com.jamie.jamiebingo.util.ServerLevelUtil;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamChestData extends SavedData {

    private static final String DATA_NAME = "jamiebingo_team_chests";
    private static final Codec<TeamChestData> CODEC = Codec.of(
            new Encoder<>() {
                @Override
                public <T> DataResult<T> encode(TeamChestData data, DynamicOps<T> ops, T prefix) {
                    CompoundTag tag = new CompoundTag();
                    data.save(tag);
                    return DataResult.success(com.jamie.jamiebingo.util.NbtOpsUtil.instance().convertTo(ops, tag));
                }
            },
            new Decoder<>() {
                @Override
                public <T> DataResult<Pair<TeamChestData, T>> decode(DynamicOps<T> ops, T input) {
                    CompoundTag tag = (CompoundTag) ops.convertTo(com.jamie.jamiebingo.util.NbtOpsUtil.instance(), input);
                    return DataResult.success(Pair.of(load(tag), input));
                }
            }
    );
    private static final SavedDataType<TeamChestData> TYPE =
            new SavedDataType<>(DATA_NAME, TeamChestData::new, CODEC, DataFixTypes.LEVEL);

    private final Map<UUID, SimpleContainer> chests = new HashMap<>();
    private static final java.util.Map<MinecraftServer, TeamChestData> FALLBACK_BY_SERVER =
            new java.util.WeakHashMap<>();

    public SimpleContainer getChest(UUID teamId) {
        return chests.computeIfAbsent(teamId, id -> {
            SimpleContainer c = new SimpleContainer(27) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(TeamChestData.this);
                }
            };
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
            return c;
        });
    }

    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();

        for (Map.Entry<UUID, SimpleContainer> e : chests.entrySet()) {
            CompoundTag t = new CompoundTag();
            putUuid(t, "Team", e.getKey());

            ListTag items = new ListTag();
            SimpleContainer c = e.getValue();
            for (int i = 0; i < c.getContainerSize(); i++) {
                ItemStack stack = c.getItem(i);
                if (!stack.isEmpty()) {
                    CompoundTag it = saveItemStack(stack);
                    com.jamie.jamiebingo.util.NbtUtil.putInt(it, "Slot", i);
                    items.add(it);
                }
            }

            com.jamie.jamiebingo.util.NbtUtil.putTag(t, "Items", items);
            list.add(t);
        }

        com.jamie.jamiebingo.util.NbtUtil.putTag(tag, "Chests", list);
        return tag;
    }

    public static TeamChestData load(CompoundTag tag) {
        TeamChestData data = new TeamChestData();
        ListTag list = tag.getListOrEmpty("Chests");

        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompoundOrEmpty(i);
            UUID team = getUuid(t, "Team");
            if (team == null) {
                continue;
            }
            SimpleContainer c = new SimpleContainer(27) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
                }
            };

            ListTag items = t.getListOrEmpty("Items");
            for (int j = 0; j < items.size(); j++) {
                CompoundTag it = items.getCompoundOrEmpty(j);
                int slot = com.jamie.jamiebingo.util.NbtUtil.getInt(it, "Slot", 0);
                CompoundTag stackTag = it.copy();
                stackTag.remove("Slot");
                c.setItem(slot, loadItemStack(stackTag));
            }

            data.chests.put(team, c);
        }

        return data;
    }

    public static TeamChestData get(MinecraftServer server) {
        var overworld = ServerLevelUtil.getOverworld(server);
        if (overworld == null) {
            return FALLBACK_BY_SERVER.computeIfAbsent(server, s -> new TeamChestData());
        }
        var storage = com.jamie.jamiebingo.util.LevelDataStorageUtil.getDataStorage(overworld);
        if (storage == null) {
            return FALLBACK_BY_SERVER.computeIfAbsent(server, s -> new TeamChestData());
        }
        TeamChestData data = com.jamie.jamiebingo.util.SavedDataUtil.computeIfAbsent(storage, TYPE);
        if (data == null) {
            TeamChestData fallback = FALLBACK_BY_SERVER.computeIfAbsent(server, s -> new TeamChestData());
            if (!com.jamie.jamiebingo.util.SavedDataUtil.set(storage, TYPE, fallback)) {
                return fallback;
            }
            return fallback;
        }
        return data;
    }

    public void clearAll() {
        chests.clear();
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(this);
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

    private static CompoundTag saveItemStack(ItemStack stack) {
        DataResult<Tag> result = ItemStack.CODEC.encodeStart(com.jamie.jamiebingo.util.NbtOpsUtil.instance(), stack);
        return result.result().filter(t -> t instanceof CompoundTag).map(t -> (CompoundTag) t).orElseGet(CompoundTag::new);
    }

    private static ItemStack loadItemStack(CompoundTag tag) {
        return ItemStack.CODEC.parse(com.jamie.jamiebingo.util.NbtOpsUtil.instance(), tag).result().orElse(com.jamie.jamiebingo.util.ItemStackUtil.empty());
    }
}
