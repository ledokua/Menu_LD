package net.ledok.menuld.mixin;

import net.ledok.menuld.audio.MenuAudio;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
abstract class MusicManagerMixin {
    @Shadow
    @Nullable
    private SoundInstance currentMusic;

    @Shadow
    public abstract void stopPlaying();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void menuld$menuMusic(CallbackInfo ci) {
        if (MenuAudio.replacesMenuMusic()) {
            if (currentMusic != null) stopPlaying();
            ci.cancel();
        }
    }
}
