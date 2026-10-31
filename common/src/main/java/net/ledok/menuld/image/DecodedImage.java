package net.ledok.menuld.image;

import com.mojang.blaze3d.platform.NativeImage;
import org.jetbrains.annotations.Nullable;

/** The first frame, plus the decoder that produces the rest for animated images. */
record DecodedImage(NativeImage first, @Nullable FrameSource animation) {
}
