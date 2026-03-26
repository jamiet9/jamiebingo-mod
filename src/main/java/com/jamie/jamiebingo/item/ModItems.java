package com.jamie.jamiebingo.item;

import com.jamie.jamiebingo.JamieBingo;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, JamieBingo.MOD_ID);

    public static final RegistryObject<Item> BINGO_CONTROLLER =
            ITEMS.register(
                    "bingo_controller",
                    () -> new BingoControllerItem(
                            com.jamie.jamiebingo.util.ItemPropertiesUtil.withId(
                                    new Item.Properties(),
                                    ITEMS.key("bingo_controller"),
                                    "item." + JamieBingo.MOD_ID + ".bingo_controller"
                            )
                    )
            );

    public static final RegistryObject<Item> CUSTOM_CARD_MAKER =
            ITEMS.register(
                    "custom_card_maker",
                    () -> new CustomCardMakerItem(
                            com.jamie.jamiebingo.util.ItemPropertiesUtil.withId(
                                    new Item.Properties(),
                                    ITEMS.key("custom_card_maker"),
                                    "item." + JamieBingo.MOD_ID + ".custom_card_maker"
                            )
                    )
            );

    public static final RegistryObject<Item> CARD_LAYOUT_CONFIGURATOR =
            ITEMS.register(
                    "card_layout_configurator",
                    () -> new CardLayoutConfiguratorItem(
                            com.jamie.jamiebingo.util.ItemPropertiesUtil.withId(
                                    new Item.Properties(),
                                    ITEMS.key("card_layout_configurator"),
                                    "item." + JamieBingo.MOD_ID + ".card_layout_configurator"
                            )
                    )
            );

    public static final RegistryObject<Item> BLACKLIST_ITEMS_QUEST =
            ITEMS.register(
                    "blacklist_items_quest",
                    () -> new BlacklistItemsQuestItem(
                            com.jamie.jamiebingo.util.ItemPropertiesUtil.withId(
                                    new Item.Properties(),
                                    ITEMS.key("blacklist_items_quest"),
                                    "item." + JamieBingo.MOD_ID + ".blacklist_items_quest"
                            )
                    )
            );

    public static final RegistryObject<Item> GAME_HISTORY =
            ITEMS.register(
                    "game_history",
                    () -> new GameHistoryItem(
                            com.jamie.jamiebingo.util.ItemPropertiesUtil.withId(
                                    new Item.Properties(),
                                    ITEMS.key("game_history"),
                                    "item." + JamieBingo.MOD_ID + ".game_history"
                            )
                    )
            );

    public static final RegistryObject<Item> WORLD_SETTINGS =
            ITEMS.register(
                    "world_settings",
                    () -> new WorldSettingsItem(
                            com.jamie.jamiebingo.util.ItemPropertiesUtil.withId(
                                    new Item.Properties(),
                                    ITEMS.key("world_settings"),
                                    "item." + JamieBingo.MOD_ID + ".world_settings"
                            )
                    )
            );

    public static final RegistryObject<Item> RARITY_CHANGER =
            ITEMS.register(
                    "rarity_changer",
                    () -> new RarityChangerItem(
                            com.jamie.jamiebingo.util.ItemPropertiesUtil.withId(
                                    new Item.Properties(),
                                    ITEMS.key("rarity_changer"),
                                    "item." + JamieBingo.MOD_ID + ".rarity_changer"
                            )
                    )
            );

    public static final RegistryObject<Item> TEAM_SELECTOR =
            ITEMS.register(
                    "team_selector",
                    () -> new TeamSelectorItem(
                            com.jamie.jamiebingo.util.ItemPropertiesUtil.withId(
                                    new Item.Properties(),
                                    ITEMS.key("team_selector"),
                                    "item." + JamieBingo.MOD_ID + ".team_selector"
                            )
                    )
            );

    public static final RegistryObject<Item> WEEKLY_CHALLENGE =
            ITEMS.register(
                    "weekly_challenge",
                    () -> new WeeklyChallengeItem(
                            com.jamie.jamiebingo.util.ItemPropertiesUtil.withId(
                                    new Item.Properties(),
                                    ITEMS.key("weekly_challenge"),
                                    "item." + JamieBingo.MOD_ID + ".weekly_challenge"
                            )
                    )
            );

    public static void register(BusGroup modBus) {
        ITEMS.register(modBus);
    }
}
