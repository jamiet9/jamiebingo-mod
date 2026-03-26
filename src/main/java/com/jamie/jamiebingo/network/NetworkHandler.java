package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.network.packet.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class NetworkHandler {

    private static final int PROTOCOL_VERSION = 1;
    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger(com.jamie.jamiebingo.JamieBingo.MOD_ID);

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(com.jamie.jamiebingo.util.IdUtil.id("jamiebingo:main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .clientAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
            .serverAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
            .simpleChannel();

    public static void register() {

        int id = 0;

        // ===============================
        // CORE BINGO
        // ===============================
        registerMessage(id++,
                PacketSyncCard.class,
                PacketSyncCard::encode,
                PacketSyncCard::decode,
                PacketSyncCard::handle
        );

        registerMessage(id++,
                PacketSyncProgress.class,
                PacketSyncProgress::encode,
                PacketSyncProgress::decode,
                PacketSyncProgress::handle
        );

        registerMessage(id++,
                PacketSyncQuestProgress.class,
                PacketSyncQuestProgress::encode,
                PacketSyncQuestProgress::decode,
                PacketSyncQuestProgress::handle
        );

        registerMessage(id++,
                PacketSyncTeamScores.class,
                PacketSyncTeamScores::encode,
                PacketSyncTeamScores::decode,
                PacketSyncTeamScores::handle
        );

        registerMessage(id++,
                PacketOpenTeamChest.class,
                PacketOpenTeamChest::encode,
                PacketOpenTeamChest::decode,
                PacketOpenTeamChest::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(id++,
                PacketTeamChatMessage.class,
                PacketTeamChatMessage::encode,
                PacketTeamChatMessage::decode,
                PacketTeamChatMessage::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(id++,
                PacketSyncSlotOwnership.class,
                PacketSyncSlotOwnership::encode,
                PacketSyncSlotOwnership::decode,
                PacketSyncSlotOwnership::handle
        );

        registerMessage(id++,
                PacketSyncWinningLine.class,
                PacketSyncWinningLine::encode,
                PacketSyncWinningLine::decode,
                PacketSyncWinningLine::handle
        );

        registerMessage(id++,
                PacketSyncRevealedSlots.class,
                PacketSyncRevealedSlots::encode,
                PacketSyncRevealedSlots::decode,
                PacketSyncRevealedSlots::handle
        );

        registerMessage(id++,
                PacketSyncHighlightedSlots.class,
                PacketSyncHighlightedSlots::encode,
                PacketSyncHighlightedSlots::decode,
                PacketSyncHighlightedSlots::handle
        );

        registerMessage(id++,
                PacketFlashSlots.class,
                PacketFlashSlots::encode,
                PacketFlashSlots::decode,
                PacketFlashSlots::handle
        );

        // ===============================
        // RANDOM EFFECTS
        // ===============================
        registerMessage(id++,
                PacketEffectCountdown.class,
                PacketEffectCountdown::encode,
                PacketEffectCountdown::decode,
                PacketEffectCountdown::handle
        );

        registerMessage(id++,
                PacketEffectApplied.class,
                PacketEffectApplied::encode,
                PacketEffectApplied::decode,
                PacketEffectApplied::handle
        );

        registerMessage(id++,
                PacketPlayTeamSound.class,
                PacketPlayTeamSound::encode,
                PacketPlayTeamSound::decode,
                PacketPlayTeamSound::handle
        );

        registerMessage(id++,
                PacketChatIconMessage.class,
                PacketChatIconMessage::encode,
                PacketChatIconMessage::decode,
                PacketChatIconMessage::handle
        );

        registerMessage(id++,
                PacketSyncCustomEffectState.class,
                PacketSyncCustomEffectState::encode,
                PacketSyncCustomEffectState::decode,
                PacketSyncCustomEffectState::handle
        );

        registerMessage(id++,
                PacketSyncGameTimer.class,
                PacketSyncGameTimer::encode,
                PacketSyncGameTimer::decode,
                PacketSyncGameTimer::handle
        );

        registerMessage(id++,
                PacketSyncHangmanState.class,
                PacketSyncHangmanState::encode,
                PacketSyncHangmanState::decode,
                PacketSyncHangmanState::handle
        );

        registerMessage(id++,
                PacketStartCountdown.class,
                PacketStartCountdown::encode,
                PacketStartCountdown::decode,
                PacketStartCountdown::handle
        );

        registerMessage(id++,
                PacketSyncPreferredTeamColor.class,
                PacketSyncPreferredTeamColor::encode,
                PacketSyncPreferredTeamColor::decode,
                PacketSyncPreferredTeamColor::handle,
                NetworkDirection.PLAY_TO_CLIENT
        );

        registerMessage(
                id++,
                PacketOpenBingoController.class,
                PacketOpenBingoController::encode,
                PacketOpenBingoController::decode,
                PacketOpenBingoController::handle,
                NetworkDirection.PLAY_TO_CLIENT
        );

        registerMessage(
                id++,
                PacketOpenCustomCardMaker.class,
                PacketOpenCustomCardMaker::encode,
                PacketOpenCustomCardMaker::decode,
                PacketOpenCustomCardMaker::handle,
                NetworkDirection.PLAY_TO_CLIENT
        );

        registerMessage(
                id++,
                PacketOpenCardLayoutConfigurator.class,
                PacketOpenCardLayoutConfigurator::encode,
                PacketOpenCardLayoutConfigurator::decode,
                PacketOpenCardLayoutConfigurator::handle,
                NetworkDirection.PLAY_TO_CLIENT
        );

        registerMessage(
                id++,
                PacketOpenBlacklistMenu.class,
                PacketOpenBlacklistMenu::encode,
                PacketOpenBlacklistMenu::decode,
                PacketOpenBlacklistMenu::handle,
                NetworkDirection.PLAY_TO_CLIENT
        );

        registerMessage(
                id++,
                PacketOpenRarityChanger.class,
                PacketOpenRarityChanger::encode,
                PacketOpenRarityChanger::decode,
                PacketOpenRarityChanger::handle,
                NetworkDirection.PLAY_TO_CLIENT
        );

        registerMessage(
                id++,
                PacketOpenWorldSettings.class,
                PacketOpenWorldSettings::encode,
                PacketOpenWorldSettings::decode,
                PacketOpenWorldSettings::handle,
                NetworkDirection.PLAY_TO_CLIENT
        );

        registerMessage(
                id++,
                PacketSaveCustomCardState.class,
                PacketSaveCustomCardState::encode,
                PacketSaveCustomCardState::decode,
                PacketSaveCustomCardState::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(
                id++,
                PacketSetBlacklistIds.class,
                PacketSetBlacklistIds::encode,
                PacketSetBlacklistIds::decode,
                PacketSetBlacklistIds::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(
                id++,
                PacketSetRarityOverrides.class,
                PacketSetRarityOverrides::encode,
                PacketSetRarityOverrides::decode,
                PacketSetRarityOverrides::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(
                id++,
                PacketSetRarityOverridesChunk.class,
                PacketSetRarityOverridesChunk::encode,
                PacketSetRarityOverridesChunk::decode,
                PacketSetRarityOverridesChunk::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(
                id++,
                PacketSetPreferredTeamColor.class,
                PacketSetPreferredTeamColor::encode,
                PacketSetPreferredTeamColor::decode,
                PacketSetPreferredTeamColor::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(
                id++,
                PacketToggleBlacklistId.class,
                PacketToggleBlacklistId::encode,
                PacketToggleBlacklistId::decode,
                PacketToggleBlacklistId::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(
                id++,
                PacketSaveWorldSettings.class,
                PacketSaveWorldSettings::encode,
                PacketSaveWorldSettings::decode,
                PacketSaveWorldSettings::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(
                id++,
                PacketImportCustomSeed.class,
                PacketImportCustomSeed::encode,
                PacketImportCustomSeed::decode,
                PacketImportCustomSeed::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(
                id++,
                PacketStartCustomCardGame.class,
                PacketStartCustomCardGame::encode,
                PacketStartCustomCardGame::decode,
                PacketStartCustomCardGame::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(
                id++,
                PacketRequestCustomSeed.class,
                PacketRequestCustomSeed::encode,
                PacketRequestCustomSeed::decode,
                PacketRequestCustomSeed::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(id++,
                PacketOpenSeedGui.class,
                PacketOpenSeedGui::encode,
                PacketOpenSeedGui::decode,
                PacketOpenSeedGui::handle
        );

        registerMessage(id++,
                PacketOpenGameHistory.class,
                PacketOpenGameHistory::encode,
                PacketOpenGameHistory::decode,
                PacketOpenGameHistory::handle,
                NetworkDirection.PLAY_TO_CLIENT
        );

        registerMessage(id++,
                PacketOpenWeeklyChallenge.class,
                PacketOpenWeeklyChallenge::encode,
                PacketOpenWeeklyChallenge::decode,
                PacketOpenWeeklyChallenge::handle,
                NetworkDirection.PLAY_TO_CLIENT
        );

        registerMessage(id++,
                PacketSeedPastePart.class,
                PacketSeedPastePart::encode,
                PacketSeedPastePart::decode,
                PacketSeedPastePart::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(id++,
                PacketSeedPasteFinish.class,
                PacketSeedPasteFinish::encode,
                PacketSeedPasteFinish::decode,
                PacketSeedPasteFinish::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(id++,
                PacketSeedPreviewRequest.class,
                PacketSeedPreviewRequest::encode,
                PacketSeedPreviewRequest::decode,
                PacketSeedPreviewRequest::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(id++,
                PacketSeedPreviewResponse.class,
                PacketSeedPreviewResponse::encode,
                PacketSeedPreviewResponse::decode,
                PacketSeedPreviewResponse::handle
        );

        registerMessage(id++,
                PacketWeeklyChallengePreviewRequest.class,
                PacketWeeklyChallengePreviewRequest::encode,
                PacketWeeklyChallengePreviewRequest::decode,
                PacketWeeklyChallengePreviewRequest::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(id++,
                PacketWeeklyChallengePreviewResponse.class,
                PacketWeeklyChallengePreviewResponse::encode,
                PacketWeeklyChallengePreviewResponse::decode,
                PacketWeeklyChallengePreviewResponse::handle
        );

        registerMessage(id++,
                PacketStartWeeklyChallenge.class,
                PacketStartWeeklyChallenge::encode,
                PacketStartWeeklyChallenge::decode,
                PacketStartWeeklyChallenge::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(id++,
                com.jamie.jamiebingo.network.packet.PacketClearClientState.class,
                com.jamie.jamiebingo.network.packet.PacketClearClientState::encode,
                com.jamie.jamiebingo.network.packet.PacketClearClientState::decode,
                com.jamie.jamiebingo.network.packet.PacketClearClientState::handle
        );

        registerMessage(id++,
                PacketToggleHighlight.class,
                PacketToggleHighlight::encode,
                PacketToggleHighlight::decode,
                PacketToggleHighlight::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        // ===============================
        // BINGO CONTROLLER (NEW UI)
        // ===============================
        registerMessage(id++,
                ControllerStartGamePacket.class,
                ControllerStartGamePacket::encode,
                ControllerStartGamePacket::decode,
                ControllerStartGamePacket::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(id++,
                ControllerCycleModePacket.class,
                ControllerCycleModePacket::encode,
                ControllerCycleModePacket::decode,
                ControllerCycleModePacket::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(id++,
                PacketGlobalWallAction.class,
                PacketGlobalWallAction::encode,
                PacketGlobalWallAction::decode,
                PacketGlobalWallAction::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(id++,
                PacketGlobalWallSettingsSync.class,
                PacketGlobalWallSettingsSync::encode,
                PacketGlobalWallSettingsSync::decode,
                PacketGlobalWallSettingsSync::handle,
                NetworkDirection.PLAY_TO_CLIENT
        );

        // ===============================
        // CASINO PHASE
        // ===============================
        registerMessage(id++,
                PacketCasinoStart.class,
                PacketCasinoStart::encode,
                PacketCasinoStart::decode,
                PacketCasinoStart::handle
        );

        registerMessage(id++,
                PacketCasinoSlotStart.class,
                PacketCasinoSlotStart::encode,
                PacketCasinoSlotStart::decode,
                PacketCasinoSlotStart::handle
        );

        registerMessage(id++,
                PacketCasinoRollRarity.class,
                PacketCasinoRollRarity::encode,
                PacketCasinoRollRarity::decode,
                PacketCasinoRollRarity::handle
        );

        registerMessage(id++,
                PacketCasinoRollPath.class,
                PacketCasinoRollPath::encode,
                PacketCasinoRollPath::decode,
                PacketCasinoRollPath::handle
        );

        registerMessage(id++,
                PacketCasinoRollFinal.class,
                PacketCasinoRollFinal::encode,
                PacketCasinoRollFinal::decode,
                PacketCasinoRollFinal::handle
        );

        registerMessage(id++,
                PacketCasinoEnd.class,
                PacketCasinoEnd::encode,
                PacketCasinoEnd::decode,
                PacketCasinoEnd::handle
        );

        registerMessage(id++,
                PacketCasinoVoteSkip.class,
                PacketCasinoVoteSkip::encode,
                PacketCasinoVoteSkip::decode,
                PacketCasinoVoteSkip::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(id++,
                PacketCasinoSkipStatus.class,
                PacketCasinoSkipStatus::encode,
                PacketCasinoSkipStatus::decode,
                PacketCasinoSkipStatus::handle
        );

        // ===============================
        // DRAFT PHASE
        // ===============================
        registerMessage(id++,
                PacketCasinoEnterDraftPhase.class,
                PacketCasinoEnterDraftPhase::encode,
                PacketCasinoEnterDraftPhase::decode,
                PacketCasinoEnterDraftPhase::handle
        );

        registerMessage(id++,
                PacketCasinoDraftTurn.class,
                PacketCasinoDraftTurn::encode,
                PacketCasinoDraftTurn::decode,
                PacketCasinoDraftTurn::handle
        );

        registerMessage(id++,
                PacketCasinoDraftChoices.class,
                PacketCasinoDraftChoices::encode,
                PacketCasinoDraftChoices::decode,
                PacketCasinoDraftChoices::handle
        );

        registerMessage(id++,
                PacketCasinoDraftPlace.class,
                PacketCasinoDraftPlace::encode,
                PacketCasinoDraftPlace::decode,
                PacketCasinoDraftPlace::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        // ===============================
        // REROLL PHASE
        // ===============================
        registerMessage(id++,
                PacketCasinoEnterRerollPhase.class,
                PacketCasinoEnterRerollPhase::encode,
                PacketCasinoEnterRerollPhase::decode,
                PacketCasinoEnterRerollPhase::handle
        );

        registerMessage(id++,
                PacketCasinoRerollSlot.class,
                PacketCasinoRerollSlot::encode,
                PacketCasinoRerollSlot::decode,
                PacketCasinoRerollSlot::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(id++,
                PacketCasinoRerollCount.class,
                PacketCasinoRerollCount::encode,
                PacketCasinoRerollCount::decode,
                PacketCasinoRerollCount::handle
        );

        registerMessage(id++,
                PacketCasinoRerollTurn.class,
                PacketCasinoRerollTurn::encode,
                PacketCasinoRerollTurn::decode,
                PacketCasinoRerollTurn::handle
        );

        // 🔥 Prevents LAN desync crashes
        registerMessage(id++,
                PacketCasinoRerollReject.class,
                PacketCasinoRerollReject::encode,
                PacketCasinoRerollReject::decode,
                PacketCasinoRerollReject::handle
        );

        registerMessage(id++,
                PacketCasinoShutdown.class,
                PacketCasinoShutdown::encode,
                PacketCasinoShutdown::decode,
                PacketCasinoShutdown::handle
        );

        registerMessage(id++,
                PacketSyncSettingsOverlay.class,
                PacketSyncSettingsOverlay::encode,
                PacketSyncSettingsOverlay::decode,
                PacketSyncSettingsOverlay::handle
        );

        registerMessage(id++,
                PacketSyncMineState.class,
                PacketSyncMineState::encode,
                PacketSyncMineState::decode,
                PacketSyncMineState::handle
        );

        registerMessage(id++,
                PacketSyncPowerSlotState.class,
                PacketSyncPowerSlotState::encode,
                PacketSyncPowerSlotState::decode,
                PacketSyncPowerSlotState::handle
        );

        registerMessage(id++,
                PacketPowerSlotWheelEvent.class,
                PacketPowerSlotWheelEvent::encode,
                PacketPowerSlotWheelEvent::decode,
                PacketPowerSlotWheelEvent::handle
        );

        registerMessage(id++,
                PacketVoteEndGame.class,
                PacketVoteEndGame::encode,
                PacketVoteEndGame::decode,
                PacketVoteEndGame::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(id++,
                PacketVoteEndGameStatus.class,
                PacketVoteEndGameStatus::encode,
                PacketVoteEndGameStatus::decode,
                PacketVoteEndGameStatus::handle,
                NetworkDirection.PLAY_TO_CLIENT
        );

        registerMessage(id++,
                PacketVoteRerollCard.class,
                PacketVoteRerollCard::encode,
                PacketVoteRerollCard::decode,
                PacketVoteRerollCard::handle,
                NetworkDirection.PLAY_TO_SERVER
        );

        registerMessage(id++,
                PacketVoteRerollCardStatus.class,
                PacketVoteRerollCardStatus::encode,
                PacketVoteRerollCardStatus::decode,
                PacketVoteRerollCardStatus::handle,
                NetworkDirection.PLAY_TO_CLIENT
        );

        LOGGER.info("[JamieBingo] Network packets registered (SEQUENTIAL IDS LOCKED)");
    }

    public static void sendToServer(Object msg) {
        if (msg == null) return;
        try {
            java.lang.reflect.Method m = CHANNEL.getClass().getMethod("sendToServer", Object.class);
            m.invoke(CHANNEL, msg);
            return;
        } catch (Throwable ignored) {
        }
        LOGGER.warn("[JamieBingo] sendToServer fallback: {}", msg.getClass().getSimpleName());
        send(msg, PacketDistributor.SERVER.noArg());
    }

    public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player, Object msg) {
        if (player == null || msg == null) return;
        try {
            PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(player);
            send(msg, target);
            return;
        } catch (Throwable ignored) {
        }
        // Reflective fallback for launcher-obf signature changes.
        try {
            Object target = null;
            for (java.lang.reflect.Method m : PacketDistributor.class.getMethods()) {
                if (!"PLAYER".equals(m.getName())) continue;
                if (m.getParameterCount() != 0) continue;
                target = m.invoke(null);
                break;
            }
            if (target != null) {
                for (java.lang.reflect.Method m : target.getClass().getMethods()) {
                    if (!"with".equals(m.getName())) continue;
                    if (m.getParameterCount() != 1) continue;
                    if (!java.util.function.Supplier.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
                    Object packetTarget = m.invoke(target, (java.util.function.Supplier<net.minecraft.server.level.ServerPlayer>) () -> player);
                    if (packetTarget instanceof PacketDistributor.PacketTarget pt) {
                        send(msg, pt);
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        LOGGER.warn("[JamieBingo] sendToPlayer failed: {}", msg.getClass().getSimpleName());
    }

    public static void send(Object msg, PacketDistributor.PacketTarget target) {
        if (msg == null || target == null) return;
        // Try common signatures first.
        try {
            CHANNEL.send(msg, target);
            return;
        } catch (Throwable t) {
            LOGGER.warn("[JamieBingo] send threw for {} (msg,target)", msg.getClass().getSimpleName(), t);
        }
        // Reflective fallback for launcher-obf signature changes.
        try {
            for (java.lang.reflect.Method m : CHANNEL.getClass().getMethods()) {
                if (!"send".equals(m.getName())) continue;
                if (m.getParameterCount() != 2) continue;
                Class<?>[] p = m.getParameterTypes();
                // Order: (msg, target)
                if (p[0].isAssignableFrom(msg.getClass()) && p[1].isAssignableFrom(target.getClass())) {
                    m.invoke(CHANNEL, msg, target);
                    return;
                }
                // Order: (target, msg)
                if (p[0].isAssignableFrom(target.getClass()) && p[1].isAssignableFrom(msg.getClass())) {
                    m.invoke(CHANNEL, target, msg);
                    return;
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("[JamieBingo] send reflect threw for {}", msg.getClass().getSimpleName(), t);
        }
        LOGGER.warn("[JamieBingo] send failed: {}", msg.getClass().getSimpleName());
    }

    private static <T> void registerMessage(
            int id,
            Class<T> type,
            BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder,
            BiConsumer<T, CustomPayloadEvent.Context> handler
    ) {
        CHANNEL.messageBuilder(type, id)
                .encoder(encoder)
                .decoder(decoder)
                .consumerMainThread(handler)
                .add();
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private static <T> void registerMessage(
            int id,
            Class<T> type,
            BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder,
            BiConsumer<T, CustomPayloadEvent.Context> handler,
            NetworkDirection<?> direction
    ) {
        CHANNEL.messageBuilder(type, id, (NetworkDirection) direction)
                .encoder(encoder)
                .decoder(decoder)
                .consumerMainThread(handler)
                .add();
    }
}

