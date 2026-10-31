package net.ledok.menuld.image;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import net.ledok.menuld.MenuLd;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * A layout image on the GPU: one texture, whatever the frame count. Animated images decode one frame
 * ahead on the worker thread and upload it into the same texture when its turn comes.
 */
public final class MenuImage {
    private final ResourceLocation id;
    private final ImageTexture texture;
    private final int width;
    private final int height;
    @Nullable
    private final FrameSource animation;
    private final Executor decoderThread;
    @Nullable
    private CompletableFuture<NativeImage> nextFrame;
    private int shownFrame;
    private long shownAt;
    private boolean broken;

    MenuImage(ResourceLocation id, DecodedImage decoded, boolean smooth, Executor decoderThread) {
        this.id = id;
        this.width = decoded.first().getWidth();
        this.height = decoded.first().getHeight();
        this.texture = new ImageTexture(decoded.first(), smooth);
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
    public ResourceLocation frame() {
        if (animation != null && !broken) advance();
        texture.uploadPending();
        return id;
    }

    private void advance() {
        long now = Util.getMillis();
        int delay = animation.delayMs(shownFrame);
        if (now - shownAt < delay || nextFrame == null || !nextFrame.isDone()) return;
        try {
            texture.setPending(nextFrame.join());
        } catch (CompletionException e) {
            broken = true;
            MenuLd.LOGGER.warn("Animation stopped: {}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return;
        }
        shownFrame = (shownFrame + 1) % animation.frameCount();
        // Keep the GIF's own pace through small hitches; start over after a long stall.
        shownAt = now - shownAt > delay + 1000 ? now : shownAt + delay;
        requestNextFrame();
    }

    private void requestNextFrame() {
        FrameSource decoder = animation;
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

    /** One GL texture; the next image to show waits in {@code pending} until the render thread uploads it. */
    private static final class ImageTexture extends AbstractTexture {
        private final boolean smooth;
        @Nullable
        private NativeImage pending;
        private boolean allocated;

        ImageTexture(NativeImage first, boolean smooth) {
            this.pending = first;
            this.smooth = smooth;
        }

        void setPending(NativeImage image) {
            if (pending != null) pending.close();
            pending = image;
        }

        void uploadPending() {
            if (pending == null) return;
            NativeImage image = pending;
            pending = null;
            if (!allocated) {
                TextureUtil.prepareImage(getId(), image.getWidth(), image.getHeight());
                allocated = true;
            } else {
                bind();
            }
            // blur = smooth scaling, clamp = no edge bleeding, autoClose frees the CPU copy.
            image.upload(0, 0, 0, 0, 0, image.getWidth(), image.getHeight(), smooth, true, false, true);
        }

        @Override
        public void load(ResourceManager resourceManager) {
        }

        @Override
        public void close() {
            if (pending != null) {
                pending.close();
                pending = null;
            }
        }
    }
}
