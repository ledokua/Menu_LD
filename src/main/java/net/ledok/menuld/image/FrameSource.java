package net.ledok.menuld.image;

import com.mojang.blaze3d.platform.NativeImage;

import java.io.IOException;

/**
 * The frames of an animated image, decoded one at a time. Not thread-safe; {@link MenuImages} drives each
 * source from a single worker thread.
 */
interface FrameSource extends AutoCloseable {
    int frameCount();

    int delayMs(int frame);

    /** Decodes the next frame (wrapping around) into a new image the caller owns. */
    NativeImage decodeNext() throws IOException;

    @Override
    void close();
}
