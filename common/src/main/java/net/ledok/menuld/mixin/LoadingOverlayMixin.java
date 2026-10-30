package net.ledok.menuld.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ledok.menuld.loading.LoadingScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.IntSupplier;

@Mixin(LoadingOverlay.class)
abstract class LoadingOverlayMixin {
    @Shadow
    private float currentProgress;

    /** Every read of the brand color: the clear, the fade-in fill and the fade-out fill. */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/function/IntSupplier;getAsInt()I"))
    private int menuld$backgroundColor(IntSupplier brandColor, Operation<Integer> original) {
        return LoadingScreen.backgroundColor(original.call(brandColor));
    }

    /** The two Mojang logo halves; the left one is drawn with a negative u offset. */
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"))
    private void menuld$logo(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
                             float u, float v, int uWidth, int vHeight, int textureWidth, int textureHeight,
                             Operation<Void> original) {
        if (!LoadingScreen.replaceLogoHalf(graphics, u < 0)) {
            original.call(graphics, texture, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight);
        }
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;drawProgressBar(Lnet/minecraft/client/gui/GuiGraphics;IIIIF)V"))
    private void menuld$progressBar(LoadingOverlay self, GuiGraphics graphics, int x0, int y0, int x1, int y1,
                                    float alpha, Operation<Void> original) {
        if (!LoadingScreen.drawProgressBar(graphics, x0, y0, x1, y1, currentProgress, alpha)) {
            original.call(self, graphics, x0, y0, x1, y1, alpha);
        }
    }
}
