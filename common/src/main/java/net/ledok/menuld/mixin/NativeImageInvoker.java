package net.ledok.menuld.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** {@code NativeImage.read} only takes PNGs; frame archives decode JPEGs with STB themselves. */
@Mixin(NativeImage.class)
public interface NativeImageInvoker {
    /** Wraps pixels from {@code stbi_load*}; the image frees them with {@code stbi_image_free} on close. */
    @Invoker("<init>")
    static NativeImage menuld$wrapStb(NativeImage.Format format, int width, int height, boolean useStbFree, long pixels) {
        throw new AssertionError();
    }
}
