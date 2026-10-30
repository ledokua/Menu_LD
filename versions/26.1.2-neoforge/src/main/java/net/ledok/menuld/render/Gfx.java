package net.ledok.menuld.render;

import net.ledok.menuld.config.MenuConfig;
import net.ledok.menuld.config.MenuConfig.Align;
import net.ledok.menuld.config.MenuConfig.Fit;
import net.ledok.menuld.image.MenuImage;
import net.ledok.menuld.image.MenuImages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/** Drawing helpers. 26.x GUI drawing is deferred: opacity travels as each draw's ARGB tint. */
public final class Gfx {
    private Gfx() {
    }

    /** @return false while the image is not loaded yet (or failed), so callers can fall back. */
    public static boolean image(GuiGraphicsExtractor g, String ref, boolean smooth, int x, int y, int w, int h, Fit fit,
                                int slice, float alpha) {
        MenuImage image = MenuImages.get(ref, smooth);
        if (image == null) return false;
        if (alpha <= 0 || w <= 0 || h <= 0) return true;
        Identifier tex = image.frame();
        int iw = image.width();
        int ih = image.height();
        int tint = ARGB.white(Math.min(1f, alpha));
        if (slice > 0) {
            nineSlice(g, tex, x, y, w, h, iw, ih, slice, tint);
        } else {
            switch (fit) {
                case STRETCH -> g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0, 0, w, h, iw, ih, iw, ih, tint);
                case COVER -> {
                    float scale = Math.max((float) w / iw, (float) h / ih);
                    int uw = Math.min(iw, Math.round(w / scale));
                    int vh = Math.min(ih, Math.round(h / scale));
                    g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, (iw - uw) / 2f, (ih - vh) / 2f, w, h, uw, vh, iw, ih, tint);
                }
                case CONTAIN -> {
                    float scale = Math.min((float) w / iw, (float) h / ih);
                    int dw = Math.round(iw * scale);
                    int dh = Math.round(ih * scale);
                    g.blit(RenderPipelines.GUI_TEXTURED, tex, x + (w - dw) / 2, y + (h - dh) / 2, 0, 0, dw, dh, iw, ih, iw, ih, tint);
                }
            }
        }
        return true;
    }

    /** Corners keep their texture size, edges and center stretch. */
    private static void nineSlice(GuiGraphicsExtractor g, Identifier tex, int x, int y, int w, int h, int iw, int ih,
                                  int border, int tint) {
        int bx = Math.min(border, w / 2);
        int by = Math.min(border, h / 2);
        int ub = Math.min(border, iw / 2);
        int vb = Math.min(border, ih / 2);
        int cw = w - 2 * bx;
        int ch = h - 2 * by;
        int cu = iw - 2 * ub;
        int cv = ih - 2 * vb;
        part(g, tex, x, y, bx, by, 0, 0, ub, vb, iw, ih, tint);
        part(g, tex, x + w - bx, y, bx, by, iw - ub, 0, ub, vb, iw, ih, tint);
        part(g, tex, x, y + h - by, bx, by, 0, ih - vb, ub, vb, iw, ih, tint);
        part(g, tex, x + w - bx, y + h - by, bx, by, iw - ub, ih - vb, ub, vb, iw, ih, tint);
        part(g, tex, x + bx, y, cw, by, ub, 0, cu, vb, iw, ih, tint);
        part(g, tex, x + bx, y + h - by, cw, by, ub, ih - vb, cu, vb, iw, ih, tint);
        part(g, tex, x, y + by, bx, ch, 0, vb, ub, cv, iw, ih, tint);
        part(g, tex, x + w - bx, y + by, bx, ch, iw - ub, vb, ub, cv, iw, ih, tint);
        part(g, tex, x + bx, y + by, cw, ch, ub, vb, cu, cv, iw, ih, tint);
    }

    private static void part(GuiGraphicsExtractor g, Identifier tex, int x, int y, int w, int h, int u, int v, int uw,
                             int vh, int iw, int ih, int tint) {
        if (w > 0 && h > 0 && uw > 0 && vh > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, uw, vh, iw, ih, tint);
        }
    }

    /** ARGB {@code color} with its alpha scaled; 0 when the result is too faint to draw. */
    public static int withAlpha(int color, float alpha) {
        int a = Math.round((color >>> 24) * alpha);
        // Font treats alpha below 4 as fully opaque.
        return a < 4 ? 0 : (a << 24) | (color & 0xFFFFFF);
    }

    public static void fill(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color, float alpha) {
        int c = withAlpha(color, alpha);
        if (c != 0) g.fill(x0, y0, x1, y1, c);
    }

    public static void text(GuiGraphicsExtractor g, Component text, Rect at, MenuConfig.Text spec, float alpha) {
        int color = withAlpha(spec.color(), alpha);
        if (color == 0) return;
        Font font = Minecraft.getInstance().font;
        float scale = spec.scale();
        int width = font.width(text);
        float offset = spec.align() == Align.CENTER ? width / 2f : spec.align() == Align.RIGHT ? width : 0;
        g.pose().pushMatrix();
        g.pose().translate(at.x(), at.y());
        g.pose().scale(scale, scale);
        g.text(font, text, Math.round(-offset), 0, color, spec.shadow());
        g.pose().popMatrix();
    }

    /**
     * Fills the screen with a layout background; false when the background is unset. While a GIF still
     * decodes (or when it failed) the screen is black rather than flashing the vanilla panorama.
     */
    public static boolean background(GuiGraphicsExtractor g, MenuConfig.Background bg, int width, int height, float alpha) {
        if (!bg.isSet()) return false;
        if ((bg.color() >>> 24) != 0) fill(g, 0, 0, width, height, bg.color(), alpha);
        if (bg.image() != null && !image(g, bg.image(), bg.smooth(), 0, 0, width, height, bg.fit(), 0, alpha)
                && (bg.color() >>> 24) != 0xFF) {
            fill(g, 0, 0, width, height, 0xFF000000, alpha);
        }
        return true;
    }
}
