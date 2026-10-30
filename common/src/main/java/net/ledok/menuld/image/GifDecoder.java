package net.ledok.menuld.image;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageInputStreamImpl;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Streams an animated GIF frame by frame: only the compressed file, one composited canvas and the frame
 * being handed over stay in memory (a 426×240, 213-frame GIF: ~10 MB instead of ~87 MB fully decoded).
 * Not thread-safe; {@link MenuImages} drives each decoder from a single worker thread.
 */
final class GifDecoder implements AutoCloseable {
    private final ImageReader reader;
    private final ImageInputStream stream;
    private final int width;
    private final int height;
    private final int frameCount;
    private final int[] delaysMs;
    private final int[] canvas;
    private int next;

    GifDecoder(byte[] data) throws IOException {
        ImageIO.setUseCache(false);
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        if (!readers.hasNext()) throw new IOException("this Java runtime has no GIF reader");
        reader = readers.next();
        stream = new ByteArrayImageInputStream(data);
        try {
            reader.setInput(stream, false);
            frameCount = reader.getNumImages(true);
            if (frameCount <= 0) throw new IOException("GIF has no frames");
            int w = 0;
            int h = 0;
            IIOMetadata streamMeta = reader.getStreamMetadata();
            if (streamMeta != null) {
                Node screen = child(streamMeta.getAsTree("javax_imageio_gif_stream_1.0"), "LogicalScreenDescriptor");
                w = intAttr(screen, "logicalScreenWidth", 0);
                h = intAttr(screen, "logicalScreenHeight", 0);
            }
            if (w <= 0 || h <= 0) {
                w = reader.getWidth(0);
                h = reader.getHeight(0);
            }
            width = w;
            height = h;
            delaysMs = new int[frameCount];
            for (int i = 0; i < frameCount; i++) {
                Node control = child(meta(i), "GraphicControlExtension");
                int delayCs = control == null ? 10 : intAttr(control, "delayTime", 10);
                // Browsers treat 0/1 centisecond delays as 100 ms; GIFs authored for them expect that.
                delaysMs[i] = delayCs <= 1 ? 100 : delayCs * 10;
            }
            canvas = new int[width * height];
        } catch (IOException | RuntimeException e) {
            close();
            throw e;
        }
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    int frameCount() {
        return frameCount;
    }

    int delayMs(int frame) {
        return delaysMs[frame];
    }

    /** Index of the frame {@link #decodeNext()} returns next. */
    int nextIndex() {
        return next;
    }

    /** Composites the next frame (wrapping around) into a new image the caller owns. */
    NativeImage decodeNext() throws IOException {
        int index = next;
        next = (next + 1) % frameCount;
        if (index == 0) Arrays.fill(canvas, 0);
        BufferedImage image = reader.read(index);
        Node meta = meta(index);
        Node descriptor = child(meta, "ImageDescriptor");
        int left = intAttr(descriptor, "imageLeftPosition", 0);
        int top = intAttr(descriptor, "imageTopPosition", 0);
        Node control = child(meta, "GraphicControlExtension");
        String disposal = control == null ? "none" : attr(control, "disposalMethod", "none");

        int[] previous = "restoreToPrevious".equals(disposal) ? canvas.clone() : null;
        int fw = image.getWidth();
        int fh = image.getHeight();
        int[] pixels = image.getRGB(0, 0, fw, fh, null, 0, fw);
        for (int y = 0; y < fh; y++) {
            int cy = top + y;
            if (cy < 0 || cy >= height) continue;
            for (int x = 0; x < fw; x++) {
                int cx = left + x;
                if (cx < 0 || cx >= width) continue;
                int argb = pixels[y * fw + x];
                if ((argb >>> 24) != 0) canvas[cy * width + cx] = argb;
            }
        }
        NativeImage frame = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                frame.setPixelRGBA(x, y, FastColor.ABGR32.fromArgb32(canvas[y * width + x]));
            }
        }
        if ("restoreToBackgroundColor".equals(disposal)) {
            for (int y = Math.max(0, top); y < Math.min(height, top + fh); y++) {
                Arrays.fill(canvas, y * width + Math.max(0, left), y * width + Math.min(width, left + fw), 0);
            }
        } else if (previous != null) {
            System.arraycopy(previous, 0, canvas, 0, canvas.length);
        }
        return frame;
    }

    private Node meta(int index) throws IOException {
        return reader.getImageMetadata(index).getAsTree("javax_imageio_gif_image_1.0");
    }

    @Override
    public void close() {
        reader.dispose();
        try {
            stream.close();
        } catch (IOException ignored) {
        }
    }

    /** ImageIO's own memory stream would cache a second copy of the whole file. */
    private static final class ByteArrayImageInputStream extends ImageInputStreamImpl {
        private final byte[] data;

        ByteArrayImageInputStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() throws IOException {
            checkClosed();
            bitOffset = 0;
            return streamPos < data.length ? data[(int) streamPos++] & 0xFF : -1;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            checkClosed();
            bitOffset = 0;
            if (streamPos >= data.length) return -1;
            int n = (int) Math.min(length, data.length - streamPos);
            System.arraycopy(data, (int) streamPos, buffer, offset, n);
            streamPos += n;
            return n;
        }

        @Override
        public long length() {
            return data.length;
        }
    }

    @Nullable
    private static Node child(@Nullable Node parent, String name) {
        if (parent == null) return null;
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (name.equals(n.getNodeName())) return n;
        }
        return null;
    }

    private static String attr(Node node, String name, String fallback) {
        NamedNodeMap attributes = node.getAttributes();
        Node a = attributes == null ? null : attributes.getNamedItem(name);
        return a == null ? fallback : a.getNodeValue();
    }

    private static int intAttr(@Nullable Node node, String name, int fallback) {
        if (node == null) return fallback;
        try {
            return Integer.parseInt(attr(node, name, Integer.toString(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
