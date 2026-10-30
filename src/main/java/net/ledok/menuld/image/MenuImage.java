package net.ledok.menuld.image;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.ledok.menuld.MenuLd;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * A layout image on the GPU: one texture, whatever the frame count. Animated images decode one frame
 * ahead on the worker thread and write it into the same texture when its turn comes.
 */
public final class MenuImage {
    private final Identifier id;
    private final ImageTexture texture;
    private final int width;
    private final int height;
    @Nullable
    private final GifDecoder animation;
    private final Executor decoderThread;
    @Nullable
    private CompletableFuture<NativeImage> nextFrame;
    private int shownFrame;
    private long shownAt;
    private boolean broken;

    MenuImage(Identifier id, DecodedImage decoded, boolean smooth, Executor decoderThread) {
        this.id = id;
        this.width = decoded.first().getWidth();
        this.height = decoded.first().getHeight();
        this.texture = new ImageTexture(id.toString(), width, height, smooth, decoded.first());
        this.animation = decoded.animation() != null && decoded.animation().frameCount() > 1 ? decoded.animation() : null;
        this.decoderThread = decoderThread;
        Minecraft.getInstance().getTextureManager().register(id, texture);
        this.shownAt = Util.getMillis();
        if (animation != null) requestNextFrame();
        else if (decoded.animation() != null) decoded.animation().close();
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** The texture to draw, with the animation advanced to the current frame. */
    public Identifier frame() {
        if (animation != null && !broken) advance();
        return id;
    }

    private void advance() {
        long now = Util.getMillis();
        int delay = animation.delayMs(shownFrame);
        if (now - shownAt < delay || nextFrame == null || !nextFrame.isDone()) return;
        try {
            texture.write(nextFrame.join());
        } catch (CompletionException e) {
            broken = true;
            MenuLd.LOGGER.warn("GIF animation stopped: {}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return;
        }
        shownFrame = (shownFrame + 1) % animation.frameCount();
        // Keep the GIF's own pace through small hitches; start over after a long stall.
        shownAt = now - shownAt > delay + 1000 ? now : shownAt + delay;
        requestNextFrame();
    }

    private void requestNextFrame() {
        GifDecoder decoder = animation;
        nextFrame = CompletableFuture.supplyAsync(() -> {
            try {
                return decoder.decodeNext();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, decoderThread);
    }

    void release() {
        Minecraft.getInstance().getTextureManager().release(id);
        if (nextFrame != null) nextFrame.thenAccept(NativeImage::close);
        // Queued behind any decode still running on the same single worker thread.
        if (animation != null) CompletableFuture.runAsync(animation::close, decoderThread);
    }

    /** One GPU texture; each frame is written in whole and its CPU copy freed. */
    private static final class ImageTexture extends AbstractTexture {
        ImageTexture(String label, int width, int height, boolean smooth, NativeImage first) {
            GpuDevice device = RenderSystem.getDevice();
            this.texture = device.createTexture(label, GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                    TextureFormat.RGBA8, width, height, 1, 1);
            // Clamped, so scaled images don't bleed their opposite edge in.
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(smooth ? FilterMode.LINEAR : FilterMode.NEAREST);
            this.textureView = device.createTextureView(this.texture);
            write(first);
        }

        void write(NativeImage image) {
            try (image) {
                RenderSystem.getDevice().createCommandEncoder().writeToTexture(getTexture(), image);
            }
        }
    }
}
