package net.ledok.menuld.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.ledok.menuld.loading.LoadingScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.IntSupplier;

@Mixin(LoadingOverlay.class)
abstract class LoadingOverlayMixin {
    @Shadow
    private float currentProgress;

    /** Every read of the brand color: the clear-color override, the fade-in fill and the fade-out fill. */
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Ljava/util/function/IntSupplier;getAsInt()I"))
    private int menuld$backgroundColor(IntSupplier brandColor, Operation<Integer> original) {
        return LoadingScreen.backgroundColor(original.call(brandColor));
    }

    /** The two Mojang logo halves; the left one is drawn with a negative u offset, the fade in its tint. */
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V"))
    private void menuld$logo(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture, int x, int y,
                             float u, float v, int width, int height, int srcWidth, int srcHeight, int textureWidth,
                             int textureHeight, int color, Operation<Void> original) {
        if (!LoadingScreen.replaceLogoHalf(graphics, u < 0, ARGB.alpha(color) / 255f)) {
            original.call(graphics, pipeline, texture, x, y, u, v, width, height, srcWidth, srcHeight, textureWidth,
                    textureHeight, color);
        }
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;extractProgressBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIF)V"))
    private void menuld$progressBar(LoadingOverlay self, GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1,
                                    float alpha, Operation<Void> original) {
        if (!LoadingScreen.drawProgressBar(graphics, x0, y0, x1, y1, currentProgress, alpha)) {
            original.call(self, graphics, x0, y0, x1, y1, alpha);
        }
    }
}
