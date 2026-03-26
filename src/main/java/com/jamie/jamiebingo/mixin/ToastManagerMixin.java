package com.jamie.jamiebingo.mixin;

import com.jamie.jamiebingo.client.ClientMinecraftUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
public class ToastManagerMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void jamiebingo$suppressToastsWhenCardVisible(GuiGraphics graphics, CallbackInfo ci) {
        if (ClientMinecraftUtil.shouldSuppressToasts()) {
            ci.cancel();
        }
    }
}
