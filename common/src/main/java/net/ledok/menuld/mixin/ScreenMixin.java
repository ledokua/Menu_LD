package net.ledok.menuld.mixin;

import net.ledok.menuld.title.TitleMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Menus outside a world (options, server list, connecting...) can share the title background. */
@Mixin(Screen.class)
abstract class ScreenMixin {
    @Shadow
    public int width;
    @Shadow
    public int height;

    @Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true)
    private void menuld$menuBackground(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        if (TitleMenu.renderMenuBackground(graphics, width, height)) ci.cancel();
    }
}
