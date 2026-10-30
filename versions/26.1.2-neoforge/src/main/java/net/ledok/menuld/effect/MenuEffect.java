package net.ledok.menuld.effect;

import net.ledok.menuld.config.MenuConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.List;

/**
 * An animated screen overlay. Instances keep their particles between frames and across resizes. Every
 * piece is a GUI fill; 26.x batches fills of one pipeline into a single draw.
 */
public abstract class MenuEffect {
    private long lastFrame = -1;
    private float[] cuts = new float[16];

    /** A widget the effect hides behind: inside it, the effect shows at {@code 1 - alpha}. */
    public record Occluder(float x0, float y0, float x1, float y1, float alpha) {
    }

    public static MenuEffect create(MenuConfig.Effect spec) {
        return switch (spec.type()) {
            case RAIN -> new RainEffect(spec);
            case FIREFLIES -> new FirefliesEffect(spec);
        };
    }

    public final void render(GuiGraphicsExtractor g, int width, int height, double mouseX, double mouseY,
                             List<Occluder> occluders) {
        long now = Util.getMillis();
        // Clamped so a paused or minimized game doesn't teleport every particle on return.
        float dt = lastFrame < 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        draw(g, width, height, dt, mouseX, mouseY, occluders);
    }

    protected abstract void draw(GuiGraphicsExtractor g, int width, int height, float dt,
                                 double mouseX, double mouseY, List<Occluder> occluders);

    /** How much of the effect shows at a point: 1 outside every widget. */
    protected static float visibility(List<Occluder> occluders, float x, float y) {
        float visible = 1f;
        for (Occluder o : occluders) {
            if (x >= o.x0() && x < o.x1() && y >= o.y0() && y < o.y1()) visible *= 1f - o.alpha();
        }
        return visible;
    }

    /**
     * A vertical streak fading from {@code topAlpha} to {@code bottomAlpha}, cut into pieces where it crosses
     * widgets so each piece can fade with the widget it passes behind.
     */
    protected void streak(GuiGraphicsExtractor g, float x, float y0, float y1, float thickness,
                          int rgb, float topAlpha, float bottomAlpha, List<Occluder> occluders) {
        if (occluders.isEmpty()) {
            quad(g, x, y0, x + thickness, y1, alpha(rgb, topAlpha), alpha(rgb, bottomAlpha));
            return;
        }
        if (cuts.length < 2 + occluders.size() * 2) cuts = new float[2 + occluders.size() * 2];
        int n = 0;
        cuts[n++] = y0;
        cuts[n++] = y1;
        for (Occluder o : occluders) {
            if (x + thickness <= o.x0() || x >= o.x1()) continue;
            if (o.y0() > y0 && o.y0() < y1) cuts[n++] = o.y0();
            if (o.y1() > y0 && o.y1() < y1) cuts[n++] = o.y1();
        }
        java.util.Arrays.sort(cuts, 0, n);
        float length = y1 - y0;
        for (int i = 0; i < n - 1; i++) {
            float s = cuts[i];
            float e = cuts[i + 1];
            if (e <= s) continue;
            float visible = visibility(occluders, x + thickness / 2, (s + e) / 2);
            if (visible <= 0.01f) continue;
            float as = Mth.lerp((s - y0) / length, topAlpha, bottomAlpha) * visible;
            float ae = Mth.lerp((e - y0) / length, topAlpha, bottomAlpha) * visible;
            quad(g, x, s, x + thickness, e, alpha(rgb, as), alpha(rgb, ae));
        }
    }

    /** One rectangle with separate top and bottom colors (ARGB), snapped to whole GUI pixels. */
    protected static void quad(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1,
                               int topColor, int bottomColor) {
        int ix0 = Math.round(x0);
        int iy0 = Math.round(y0);
        int ix1 = Math.max(ix0 + 1, Math.round(x1));
        int iy1 = Math.max(iy0 + 1, Math.round(y1));
        if (topColor == bottomColor) g.fill(ix0, iy0, ix1, iy1, topColor);
        else g.fillGradient(ix0, iy0, ix1, iy1, topColor, bottomColor);
    }

    protected static int alpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
