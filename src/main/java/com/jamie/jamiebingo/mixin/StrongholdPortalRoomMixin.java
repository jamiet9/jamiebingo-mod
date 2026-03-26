package com.jamie.jamiebingo.mixin;

import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StrongholdPieces.PortalRoom.class)
public abstract class StrongholdPortalRoomMixin {
    @Redirect(
            method = "postProcess",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/RandomSource;nextFloat()F"
            )
    )
    private float jamiebingo$forceFilledEndFramesWhenEnabled(RandomSource random) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            int mode = BingoGameData.clampPrelitPortalsMode(BingoGameData.get(server).prelitPortalsMode);
            if (mode == BingoGameData.PRELIT_PORTALS_END || mode == BingoGameData.PRELIT_PORTALS_BOTH) {
                return 1.0F;
            }
        }
        return random.nextFloat();
    }
}
