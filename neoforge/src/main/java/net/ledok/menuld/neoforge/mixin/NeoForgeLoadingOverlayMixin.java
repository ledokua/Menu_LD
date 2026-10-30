package net.ledok.menuld.neoforge.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.ledok.menuld.loading.LoadingScreen;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.loading.NeoForgeLoadingOverlay;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge's startup overlay replaces vanilla's render with its early loading screen, so the layout is
 * drawn over it instead of edited into it. Its own fade timing is mirrored so the hand-off to the title
 * screen still cross-fades.
 */
@Mixin(NeoForgeLoadingOverlay.class)
abstract class NeoForgeLoadingOverlayMixin {
    @Shadow
    private float currentProgress;
    @Shadow
    private long fadeOutStart;

    @Inject(method = "render", at = @At("TAIL"))
    private void menuld$cover(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!LoadingScreen.isCustomized()) return;
        float fadeOutTimer = fadeOutStart > -1L ? (Util.getMillis() - fadeOutStart) / 1000f : -1f;
        float fade = 1f - Mth.clamp(fadeOutTimer - 1f, 0f, 1f);
        float barAlpha = 1f - Mth.clamp(fadeOutTimer, 0f, 1f);

        // The overlay left a framebuffer-pixel projection and an identity model-view behind; GUI drawing
        // needs GameRenderer's GUI projection and the model-view still on top of the stack.
        Window window = Minecraft.getInstance().getWindow();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0f,
                (float) (window.getWidth() / window.getGuiScale()), (float) (window.getHeight() / window.getGuiScale()),
                0f, 1000f, ClientHooks.getGuiFarPlane()), VertexSorting.ORTHOGRAPHIC_Z);
        RenderSystem.applyModelViewMatrix();
        LoadingScreen.renderStandalone(graphics, currentProgress, fade, barAlpha);
        graphics.flush();
    }
}
