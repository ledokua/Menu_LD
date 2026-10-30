package net.ledok.menuld.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Vanilla draws icon-button icons at full opacity; fading (vanilla's or a layout's) now covers them too. */
@Mixin({SpriteIconButton.CenteredIcon.class, SpriteIconButton.TextAndIcon.class})
abstract class SpriteIconButtonMixin extends AbstractWidget {
    private SpriteIconButtonMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void menuld$fadeIcon(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height,
                                 Operation<Void> original) {
        if (alpha >= 1f) {
            original.call(graphics, sprite, x, y, width, height);
            return;
        }
        graphics.setColor(1f, 1f, 1f, alpha);
        RenderSystem.enableBlend();
        original.call(graphics, sprite, x, y, width, height);
        graphics.setColor(1f, 1f, 1f, 1f);
    }
}
