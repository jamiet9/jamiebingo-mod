package com.jamie.jamiebingo.commands;

import com.jamie.jamiebingo.JamieBingo;
import com.jamie.jamiebingo.bingo.BingoCommands;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.commands.BingoEffectCommand;
import com.jamie.jamiebingo.commands.BingoRtpCommand;
import com.jamie.jamiebingo.world.WorldRuleCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommandEvents {
    private static final Logger LOGGER = LogManager.getLogger(JamieBingo.MOD_ID);

    private CommandEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("[JamieBingo] CommandEvents.onRegisterCommands invoked");
        BingoEffectCommand.register(event.getDispatcher());
        BingoRtpCommand.register(event.getDispatcher());
        WorldRuleCommands.register(event.getDispatcher());

        if (event.getDispatcher().getRoot().getChild("bingo") == null) {
            event.getDispatcher().register(BingoCommands.register());
            LOGGER.info("[JamieBingo] Registered /bingo commands (fallback)");
        }
    }

    @SubscribeEvent
    public static void onCommandExecuted(CommandEvent event) {
        if (event == null || event.getParseResults() == null) return;
        CommandSourceStack source = event.getParseResults().getContext().getSource();
        if (source == null) return;
        ServerPlayer player = source.getPlayer();
        if (player == null) return;
        MinecraftServer server = source.getServer();
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isActive()) return;
        data.markRunCommandUsed();
    }

    @SubscribeEvent
    public static void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event == null || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (com.jamie.jamiebingo.util.GameModeTracking.isSuppressed(player)) return;
        MinecraftServer server = com.jamie.jamiebingo.util.ServerPlayerUtil.getServer(player);
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data == null || !data.isActive() || data.pregameBoxActive) return;
        data.markRunCommandUsed();
    }
}




