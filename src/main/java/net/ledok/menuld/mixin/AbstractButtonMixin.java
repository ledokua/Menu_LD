package net.ledok.menuld.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ledok.menuld.config.MenuConfig.ButtonStyle;
import net.ledok.menuld.title.ButtonSkins;
import net.ledok.menuld.title.SkinnedButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 26.x draws every button background through the final {@code extractDefaultSprite}: one hook skins them all. */
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

    @Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true)
    private void menuld$background(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (menuld$style != null && ButtonSkins.drawBackground(graphics, menuld$style, active, isHoveredOrFocused(),
                getX(), getY(), getWidth(), getHeight(), alpha)) {
            ci.cancel();
        }
    }

    /** NeoForge's per-widget label color is what the default label is drawn in. */
    @WrapOperation(method = "extractDefaultLabel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/AbstractButton;getFGColor()I"))
    private int menuld$labelColor(AbstractButton self, Operation<Integer> original) {
        int vanilla = original.call(self);
        return menuld$style == null ? vanilla : ButtonSkins.labelColor(menuld$style, active, isHoveredOrFocused(), vanilla);
    }
}
