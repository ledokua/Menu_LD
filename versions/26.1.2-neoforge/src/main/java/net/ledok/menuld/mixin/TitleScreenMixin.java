package net.ledok.menuld.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import net.ledok.menuld.title.TitleMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/TitleScreen;extractPanorama(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V"))
    private void menuld$background(TitleScreen self, GuiGraphicsExtractor graphics, float partialTick,
                                   Operation<Void> original) {
        TitleMenu.renderBehind(self, graphics, () -> original.call(self, graphics, partialTick));
    }

    @WrapWithCondition(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/LogoRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IF)V"))
    private boolean menuld$logo(LogoRenderer logo, GuiGraphicsExtractor graphics, int width, float alpha) {
        return TitleMenu.showLogo();
    }

    @WrapWithCondition(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/SplashRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/client/gui/Font;F)V"))
    private boolean menuld$splash(SplashRenderer splash, GuiGraphicsExtractor graphics, int width, Font font, float alpha) {
        return TitleMenu.showSplash();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void menuld$front(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
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
