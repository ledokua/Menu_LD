package net.ledok.menuld.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ledok.menuld.config.MenuConfig;
import net.ledok.menuld.config.MenuConfig.Align;
import net.ledok.menuld.config.MenuConfig.Fit;
import net.ledok.menuld.image.MenuImage;
import net.ledok.menuld.image.MenuImages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Drawing helpers. Alphas multiply into whatever shader color the caller already set. */
public final class Gfx {
    private Gfx() {
    }

    /** @return false while the image is not loaded yet (or failed), so callers can fall back. */
    public static boolean image(GuiGraphics g, String ref, boolean smooth, int x, int y, int w, int h, Fit fit,
                                int slice, float alpha) {
        MenuImage image = MenuImages.get(ref, smooth);
        if (image == null) return false;
        if (alpha <= 0 || w <= 0 || h <= 0) return true;
        ResourceLocation tex = image.frame();
        int iw = image.width();
        int ih = image.height();
        float[] color = RenderSystem.getShaderColor().clone();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(color[0], color[1], color[2], color[3] * alpha);
        if (slice > 0) {
            nineSlice(g, tex, x, y, w, h, iw, ih, slice);
        } else {
            switch (fit) {
                case STRETCH -> g.blit(tex, x, y, w, h, 0, 0, iw, ih, iw, ih);
                case COVER -> {
                    float scale = Math.max((float) w / iw, (float) h / ih);
                    int uw = Math.min(iw, Math.round(w / scale));
                    int vh = Math.min(ih, Math.round(h / scale));
                    g.blit(tex, x, y, w, h, (iw - uw) / 2f, (ih - vh) / 2f, uw, vh, iw, ih);
                }
                case CONTAIN -> {
                    float scale = Math.min((float) w / iw, (float) h / ih);
                    int dw = Math.round(iw * scale);
                    int dh = Math.round(ih * scale);
                    g.blit(tex, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh, 0, 0, iw, ih, iw, ih);
                }
            }
        }
        g.setColor(color[0], color[1], color[2], color[3]);
        return true;
    }

    /** Corners keep their texture size, edges and center stretch. */
    private static void nineSlice(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int iw, int ih, int border) {
        int bx = Math.min(border, w / 2);
        int by = Math.min(border, h / 2);
        int ub = Math.min(border, iw / 2);
        int vb = Math.min(border, ih / 2);
        int cw = w - 2 * bx;
        int ch = h - 2 * by;
        int cu = iw - 2 * ub;
        int cv = ih - 2 * vb;
        part(g, tex, x, y, bx, by, 0, 0, ub, vb, iw, ih);
        part(g, tex, x + w - bx, y, bx, by, iw - ub, 0, ub, vb, iw, ih);
        part(g, tex, x, y + h - by, bx, by, 0, ih - vb, ub, vb, iw, ih);
        part(g, tex, x + w - bx, y + h - by, bx, by, iw - ub, ih - vb, ub, vb, iw, ih);
        part(g, tex, x + bx, y, cw, by, ub, 0, cu, vb, iw, ih);
        part(g, tex, x + bx, y + h - by, cw, by, ub, ih - vb, cu, vb, iw, ih);
        part(g, tex, x, y + by, bx, ch, 0, vb, ub, cv, iw, ih);
        part(g, tex, x + w - bx, y + by, bx, ch, iw - ub, vb, ub, cv, iw, ih);
        part(g, tex, x + bx, y + by, cw, ch, ub, vb, cu, cv, iw, ih);
    }

    private static void part(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int u, int v, int uw, int vh,
                             int iw, int ih) {
        if (w > 0 && h > 0 && uw > 0 && vh > 0) g.blit(tex, x, y, w, h, u, v, uw, vh, iw, ih);
    }

    /** ARGB {@code color} with its alpha scaled; 0 when the result is too faint to draw. */
    public static int withAlpha(int color, float alpha) {
        int a = Math.round((color >>> 24) * alpha);
        // Font treats alpha below 4 as fully opaque.
        return a < 4 ? 0 : (a << 24) | (color & 0xFFFFFF);
    }

    public static void fill(GuiGraphics g, int x0, int y0, int x1, int y1, int color, float alpha) {
        int c = withAlpha(color, alpha);
        if (c != 0) g.fill(x0, y0, x1, y1, c);
    }

    public static void text(GuiGraphics g, Component text, Rect at, MenuConfig.Text spec, float alpha) {
        int color = withAlpha(spec.color(), alpha);
        if (color == 0) return;
        Font font = Minecraft.getInstance().font;
        float scale = spec.scale();
        int width = font.width(text);
        float offset = spec.align() == Align.CENTER ? width / 2f : spec.align() == Align.RIGHT ? width : 0;
        g.pose().pushPose();
        g.pose().translate(at.x(), at.y(), 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, Math.round(-offset), 0, color, spec.shadow());
        g.pose().popPose();
    }

    /**
     * Fills the screen with a layout background; false when the background is unset. While a GIF still
     * decodes (or when it failed) the screen is black rather than flashing the vanilla panorama.
     */
    public static boolean background(GuiGraphics g, MenuConfig.Background bg, int width, int height, float alpha) {
        if (!bg.isSet()) return false;
        if ((bg.color() >>> 24) != 0) fill(g, 0, 0, width, height, bg.color(), alpha);
        if (bg.image() != null && !image(g, bg.image(), bg.smooth(), 0, 0, width, height, bg.fit(), 0, alpha)
                && (bg.color() >>> 24) != 0xFF) {
            fill(g, 0, 0, width, height, 0xFF000000, alpha);
        }
        return true;
    }
}
