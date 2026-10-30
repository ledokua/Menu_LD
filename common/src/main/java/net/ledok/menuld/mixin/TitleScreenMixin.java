package net.ledok.menuld.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import net.ledok.menuld.title.TitleMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
abstract class TitleScreenMixin extends Screen {
    private TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "added", at = @At("HEAD"))
    private void menuld$opened(CallbackInfo ci) {
        TitleMenu.onOpened();
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/TitleScreen;renderPanorama(Lnet/minecraft/client/gui/GuiGraphics;F)V"))
    private void menuld$background(TitleScreen self, GuiGraphics graphics, float partialTick, Operation<Void> original) {
        TitleMenu.renderBehind(self, graphics, () -> original.call(self, graphics, partialTick));
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/LogoRenderer;renderLogo(Lnet/minecraft/client/gui/GuiGraphics;IF)V"))
    private boolean menuld$logo(LogoRenderer logo, GuiGraphics graphics, int width, float alpha) {
        return TitleMenu.showLogo();
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/SplashRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/client/gui/Font;I)V"))
    private boolean menuld$splash(SplashRenderer splash, GuiGraphics graphics, int width, Font font, int color) {
        return TitleMenu.showSplash();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void menuld$front(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        TitleMenu.renderFront(this, graphics);
    }

    /**
     * Hidden Realms notifications are never created: every vanilla use of the field is null-checked, so
     * init, added(), ticking, rendering and the Realms data fetch are all skipped.
     */
    @WrapOperation(method = "init", at = @At(value = "NEW",
            target = "()Lcom/mojang/realmsclient/gui/screens/RealmsNotificationsScreen;"))
    private RealmsNotificationsScreen menuld$realmsNotifications(Operation<RealmsNotificationsScreen> original) {
        return TitleMenu.showRealmsNotifications() ? original.call() : null;
    }
}
