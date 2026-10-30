package net.ledok.menuld.image;

import com.mojang.blaze3d.platform.NativeImage;
import net.ledok.menuld.MenuLd;
import net.ledok.menuld.config.Assets;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Image cache for layouts. PNGs decode on first use (they are small); GIFs open on the worker thread and
 * draw nothing until their first frame is ready. Render thread only.
 */
public final class MenuImages {
    private static final Map<String, Entry> CACHE = new HashMap<>();
    /** One thread: each GIF decoder is only ever touched by it, one frame at a time. */
    private static final ExecutorService DECODER_THREAD = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Menu_LD image decoder");
        thread.setDaemon(true);
        return thread;
    });
    private static int counter;

    private MenuImages() {
    }

    /** Null while a GIF still opens, or when the image could not be read (logged once). */
    @Nullable
    public static MenuImage get(String ref, boolean smooth) {
        String key = smooth ? ref + "#smooth" : ref;
        Entry entry = CACHE.get(key);
        if (entry == null) {
            entry = new Entry();
            CACHE.put(key, entry);
            entry.pending = isGif(ref)
                    ? CompletableFuture.supplyAsync(() -> decode(ref), DECODER_THREAD)
                    : decodeNow(ref);
        }
        if (entry.image == null && entry.pending != null && entry.pending.isDone()) {
            try {
                entry.image = new MenuImage(MenuLd.id("image/" + counter++), entry.pending.join(), smooth, DECODER_THREAD);
            } catch (CompletionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                MenuLd.LOGGER.warn("Could not load image '{}': {}", ref, cause.getMessage());
            }
            entry.pending = null;
        }
        return entry.image;
    }

    public static void releaseAll() {
        for (Entry entry : CACHE.values()) {
            if (entry.image != null) entry.image.release();
            if (entry.pending != null) {
                // An open still running would otherwise leak its first frame and decoder.
                entry.pending.thenAccept(decoded -> {
                    decoded.first().close();
                    if (decoded.animation() != null) decoded.animation().close();
                });
            }
        }
        CACHE.clear();
    }

    private static boolean isGif(String ref) {
        return ref.toLowerCase(Locale.ROOT).endsWith(".gif");
    }

    private static CompletableFuture<DecodedImage> decodeNow(String ref) {
        try {
            return CompletableFuture.completedFuture(decode(ref));
        } catch (CompletionException e) {
            return CompletableFuture.failedFuture(e.getCause());
        }
    }

    private static DecodedImage decode(String ref) {
        try (InputStream in = new BufferedInputStream(Assets.open(ref))) {
            if (!isGif(ref)) return new DecodedImage(NativeImage.read(in), null);
            GifDecoder decoder = new GifDecoder(in.readAllBytes());
            try {
                return new DecodedImage(decoder.decodeNext(), decoder);
            } catch (IOException | RuntimeException e) {
                decoder.close();
                throw e;
            }
        } catch (IOException | RuntimeException e) {
            throw new CompletionException(e);
        }
    }

    private static final class Entry {
        @Nullable
        CompletableFuture<DecodedImage> pending;
        @Nullable
        MenuImage image;
    }
}
