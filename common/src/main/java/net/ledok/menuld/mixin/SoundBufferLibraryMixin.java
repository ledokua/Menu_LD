package net.ledok.menuld.mixin;

import net.ledok.menuld.audio.MenuAudio;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/** Menu tracks stream from config files; every other sound still comes from resource packs. */
@Mixin(SoundBufferLibrary.class)
abstract class SoundBufferLibraryMixin {
    @Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
    private void menuld$fileStream(ResourceLocation path, boolean looping,
                                   CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
        CompletableFuture<AudioStream> stream = MenuAudio.openStream(path, looping);
        if (stream != null) cir.setReturnValue(stream);
    }
}
