package net.ledok.menuld.image;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * An animation stored as a {@code .zip} of JPEG/PNG frames, played in file-name order. Far smaller than a
 * GIF for full-screen footage (every frame keeps full color and compresses on its own). The archive stays
 * on disk; each frame is read and decoded natively by STB when its turn comes, so memory holds one
 * compressed and one decoded frame. An optional {@code animation.json} sets {@code {"fps": 15}}.
 */
final class FrameArchive implements FrameSource {
    static final String SETTINGS = "animation.json";
    private static final float DEFAULT_FPS = 15f;

    private final ZipFile zip;
    private final List<ZipEntry> frames;
    private final int delayMs;
    private int width = -1;
    private int height = -1;
    private int next;

    FrameArchive(Path file) throws IOException {
        zip = new ZipFile(file.toFile());
        try {
            List<ZipEntry> found = new ArrayList<>();
            float fps = DEFAULT_FPS;
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                String name = entry.getName().toLowerCase(Locale.ROOT);
                if (entry.isDirectory()) continue;
                if (name.equals(SETTINGS)) {
                    fps = readFps(entry);
                } else if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
                    found.add(entry);
                }
            }
            if (found.isEmpty()) throw new IOException(file.getFileName() + " holds no .jpg or .png frames");
            found.sort(Comparator.comparing(ZipEntry::getName));
            frames = List.copyOf(found);
            delayMs = Math.max(1, Math.round(1000f / fps));
        } catch (IOException | RuntimeException e) {
            close();
            throw e;
        }
    }

    private float readFps(ZipEntry entry) throws IOException {
        try (InputStream in = zip.getInputStream(entry)) {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            float fps = json.has("fps") ? json.get("fps").getAsFloat() : DEFAULT_FPS;
            if (!(fps > 0 && fps <= 120)) throw new IOException(SETTINGS + ": fps must be between 0 and 120");
            return fps;
        } catch (RuntimeException e) {
            throw new IOException(SETTINGS + ": " + e.getMessage());
        }
    }

    @Override
    public int frameCount() {
        return frames.size();
    }

    @Override
    public int delayMs(int frame) {
        return delayMs;
    }

    @Override
    public NativeImage decodeNext() throws IOException {
        ZipEntry entry = frames.get(next);
        next = (next + 1) % frames.size();
        byte[] bytes;
        try (InputStream in = zip.getInputStream(entry)) {
            bytes = in.readAllBytes();
        }
        ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            buffer.put(bytes).flip();
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            ByteBuffer pixels = STBImage.stbi_load_from_memory(buffer, w, h, channels, 4);
            if (pixels == null) throw new IOException(entry.getName() + ": " + STBImage.stbi_failure_reason());
            if (width < 0) {
                width = w.get(0);
                height = h.get(0);
            } else if (w.get(0) != width || h.get(0) != height) {
                STBImage.stbi_image_free(pixels);
                throw new IOException(entry.getName() + " is " + w.get(0) + "×" + h.get(0) + ", the first frame "
                        + width + "×" + height + " (all frames need one size)");
            }
            // useStbFree: the image frees STB's pixels itself on close.
            return new NativeImage(NativeImage.Format.RGBA, w.get(0), h.get(0), true, MemoryUtil.memAddress(pixels));
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }

    @Override
    public void close() {
        try {
            zip.close();
        } catch (IOException ignored) {
        }
    }
}
