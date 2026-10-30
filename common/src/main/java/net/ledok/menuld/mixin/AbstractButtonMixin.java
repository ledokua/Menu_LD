package net.ledok.menuld.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ledok.menuld.config.MenuConfig.ButtonStyle;
import net.ledok.menuld.title.ButtonSkins;
import net.ledok.menuld.title.SkinnedButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AbstractButton.class)
abstract class AbstractButtonMixin extends AbstractWidget implements SkinnedButton {
    @Unique
    @Nullable
    private ButtonStyle menuld$style;

    private AbstractButtonMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Override
    @Nullable
    public ButtonStyle menuld$style() {
        return menuld$style;
    }

    @Override
    public void menuld$setStyle(@Nullable ButtonStyle style) {
        this.menuld$style = style;
    }

    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void menuld$background(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height,
                                   Operation<Void> original) {
        if (menuld$style == null
                || !ButtonSkins.drawBackground(graphics, menuld$style, active, isHoveredOrFocused(), x, y, width, height)) {
            original.call(graphics, sprite, x, y, width, height);
        }
    }

    @ModifyArg(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/AbstractButton;renderString(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V"),
            index = 2)
    private int menuld$labelColor(int color) {
        return menuld$style == null ? color : ButtonSkins.labelColor(menuld$style, active, isHoveredOrFocused(), color);
    }
}
