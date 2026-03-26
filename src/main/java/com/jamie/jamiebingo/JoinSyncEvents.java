package com.jamie.jamiebingo;

import com.jamie.jamiebingo.casino.CasinoModeManager;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.data.BroadcastHelper;
import com.jamie.jamiebingo.data.TeamData;
import com.jamie.jamiebingo.data.TeamScoreData;
import com.jamie.jamiebingo.mines.MineModeManager;
import com.jamie.jamiebingo.util.ServerPlayerUtil;
import com.jamie.jamiebingo.world.PregameBoxManager;
import com.jamie.jamiebingo.world.SpectatorManager;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID)
public class JoinSyncEvents {
    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        processJoin(player, 0);
    }

    private static void processJoin(ServerPlayer player, int attempt) {
        if (player == null) return;
        var server = ServerPlayerUtil.getServer(player);
        if (server == null) return;
        var level = ServerPlayerUtil.getLevel(player);
        if (level == null) {
            if (attempt >= 40) return;
            UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
            server.schedule(new net.minecraft.server.TickTask(
                    com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + 1,
                    () -> {
                        ServerPlayer retry = com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayer(server, playerId);
                        if (retry != null) {
                            processJoin(retry, attempt + 1);
                        }
                    }
            ));
            return;
        }

        BingoGameData data = BingoGameData.get(server);

        boolean shouldSpectate =
                data.isActive() && !data.isParticipant(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));

        if (!shouldSpectate) {
            TeamData teamData = TeamData.get(server);
            int preferredColorId = com.jamie.jamiebingo.util.NbtUtil.getInt(
                    player.getPersistentData(),
                    com.jamie.jamiebingo.menu.TeamSelectMenu.PREF_TEAM_COLOR_TAG,
                    -1
            );
            if (!data.isActive() && preferredColorId >= 0 && preferredColorId < DyeColor.values().length) {
                DyeColor preferred = DyeColor.byId(preferredColorId);
                teamData.setPreferredTeamColor(com.jamie.jamiebingo.util.EntityUtil.getUUID(player), preferred);
                java.util.UUID preferredTeamId = teamData.getOrCreateTeamForColor(preferred);
                teamData.movePlayerToTeam(com.jamie.jamiebingo.util.EntityUtil.getUUID(player), preferredTeamId);
            } else {
                teamData.ensureAssigned(player);
            }
            if (data.isActive()) {
                UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
                data.ensureLegacyProgressMappedToTeam(server, playerId);
                data.recoverSingleplayerTeamProgress(server, playerId);
                data.normalizeSingleplayerProgressState(server, playerId);
                data.recoverSingleplayerRushState(server, playerId);
                data.restoreTimerBaselineIfNeeded(server);
                data.restoreRushDeadlinesIfNeeded(server);
                MineModeManager.restoreFromResumeState(server, data);
                com.jamie.jamiebingo.power.PowerSlotManager.restoreFromResumeState(server, data);
            }
        }

        PregameBoxManager.handlePlayerJoin(player);

        if (!data.isActive()) {
            com.jamie.jamiebingo.casino.CasinoTickScheduler.schedule(
                    server,
                    com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server) + 1,
                    () -> {
                        if (com.jamie.jamiebingo.world.WorldRegenerationManager.ensureFreshSeedPrepared(server, data, "initial_world_load")) {
                            return;
                        }
                        com.jamie.jamiebingo.world.LobbyWorldManager.startPreloadingGameStartSpawn(
                                server,
                                data,
                                false
                        );
                    }
            );
        }

        var recipeManager = com.jamie.jamiebingo.util.RecipeManagerUtil.getRecipeManager(server);
        if (recipeManager != null) {
            var recipes = com.jamie.jamiebingo.util.RecipeManagerUtil.getAllRecipes(recipeManager);
            @SuppressWarnings({"rawtypes", "unchecked"})
            var typed = (java.util.Collection) recipes;
            com.jamie.jamiebingo.util.ServerPlayerRecipeUtil.awardRecipes(player, typed);
        }

        if (!shouldSpectate) {
            if (data.isActive()) {
                TeamScoreData scores = TeamScoreData.get(server);
                if (data.winCondition == com.jamie.jamiebingo.bingo.WinCondition.RARITY) {
                    scores.recomputeRarityScores(server, data);
                } else {
                    scores.recomputeStandardScores(server, data);
                }
            }
            boolean liveMatch = data.isActive() && data.gameStartTick >= 0;
            if (liveMatch) {
                if (data.isRerollPhaseActive()) {
                    data.endRerollPhase();
                }
                if (CasinoModeManager.isCasinoInProgress()) {
                    CasinoModeManager.resetTransientState(server, false);
                }
                BroadcastHelper.syncPlayerJoin(player);
            } else if (CasinoModeManager.isCasinoInProgress() || data.isRerollPhaseActive() || data.isFakeRerollPhaseActive()) {
                CasinoModeManager.syncPlayerJoin(player);
            } else {
                BroadcastHelper.syncPlayerJoin(player);
            }
            MineModeManager.syncPlayerJoin(player);
            com.jamie.jamiebingo.power.PowerSlotManager.syncPlayerJoin(player);
        }

        if (data.isActive() && data.composition != com.jamie.jamiebingo.bingo.CardComposition.CLASSIC_ONLY) {
            com.jamie.jamiebingo.quest.QuestEvents.initAdvancementBase(player);
        }

        BroadcastHelper.broadcastTeamScores(server);

        if (!shouldSpectate && data.isActive() && data.randomEffectsEnabled) {
            if (!data.appliedRandomEffectId.isEmpty()) {
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(
                        com.jamie.jamiebingo.util.IdUtil.id(data.appliedRandomEffectId));
                if (effect != null) {
                    int remainingTicks = data.randomEffectsNextTick > 0
                            ? Math.max(20, data.randomEffectsNextTick - com.jamie.jamiebingo.util.ServerTickUtil.getTickCount(server))
                            : Math.max(20, data.randomEffectsIntervalSeconds * 20);
                    var holder = ForgeRegistries.MOB_EFFECTS.getHolder(effect).orElse(null);
                    if (holder != null) {
                        player.addEffect(new MobEffectInstance(
                                holder,
                                remainingTicks,
                                data.activeRandomEffectAmplifier,
                                false,
                                true
                        ));
                    }
                }
            }
        }

        if (shouldSpectate) {
            if (data.allowLateJoin) {
                player.sendSystemMessage(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Late join enabled - Click here to join")
                                .withStyle(style -> style.withClickEvent(
                                        new ClickEvent.RunCommand("/bingo latejoin")
                                ))
                );
            }
            SpectatorManager.makeSpectator(player, data);
        }
        player.sendSystemMessage(
                com.jamie.jamiebingo.util.ComponentUtil.literal("Tip: use /bingo stop to restart a game without creating a new world")
        );
        player.sendSystemMessage(
                com.jamie.jamiebingo.util.ComponentUtil.literal("Tip: press 'b' to open fullcard screen")
        );
        player.sendSystemMessage(
                com.jamie.jamiebingo.util.ComponentUtil.literal("Tip: blurry textures? Try increasing your gui scale")
        );
        player.sendSystemMessage(
                com.jamie.jamiebingo.util.ComponentUtil.literal("Click here to play this mod with the community!")
                        .withStyle(style -> style
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create("https://discord.gg/sjjegnNZ"))))
        );
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var server = ServerPlayerUtil.getServer(player);
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        if (data.pregameBoxActive) {
            PregameBoxManager.handleRespawnToBox(player);
        }
        if (!data.isActive() || data.pregameBoxActive) {
            com.jamie.jamiebingo.util.PlayerExperienceUtil.resetExperience(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var server = ServerPlayerUtil.getServer(player);
        if (server == null) return;
        BingoGameData data = BingoGameData.get(server);
        data.clearPlayerBlacklistedSlotIds(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
        com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
    }
}
