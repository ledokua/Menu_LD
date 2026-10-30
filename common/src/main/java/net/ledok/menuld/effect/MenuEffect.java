package net.ledok.menuld.effect;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ledok.menuld.config.MenuConfig;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.List;

/** An animated screen overlay. Instances keep their particles between frames and across resizes. */
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

    public final void render(GuiGraphics g, int width, int height, double mouseX, double mouseY,
                             List<Occluder> occluders) {
        long now = Util.getMillis();
        // Clamped so a paused or minimized game doesn't teleport every particle on return.
        float dt = lastFrame < 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        VertexConsumer buffer = g.bufferSource().getBuffer(RenderType.gui());
        draw(buffer, g.pose().last().pose(), width, height, dt, mouseX, mouseY, occluders);
        g.flush();
    }

    protected abstract void draw(VertexConsumer buffer, Matrix4f pose, int width, int height, float dt,
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
    protected void streak(VertexConsumer buffer, Matrix4f pose, float x, float y0, float y1, float thickness,
                          int rgb, float topAlpha, float bottomAlpha, List<Occluder> occluders) {
        if (occluders.isEmpty()) {
            quad(buffer, pose, x, y0, x + thickness, y1, alpha(rgb, topAlpha), alpha(rgb, bottomAlpha));
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
            quad(buffer, pose, x, s, x + thickness, e, alpha(rgb, as), alpha(rgb, ae));
        }
    }

    /** One quad with separate top and bottom colors (ARGB). */
    protected static void quad(VertexConsumer buffer, Matrix4f pose, float x0, float y0, float x1, float y1,
                               int topColor, int bottomColor) {
        buffer.addVertex(pose, x0, y0, 0).setColor(topColor);
        buffer.addVertex(pose, x0, y1, 0).setColor(bottomColor);
        buffer.addVertex(pose, x1, y1, 0).setColor(bottomColor);
        buffer.addVertex(pose, x1, y0, 0).setColor(topColor);
    }

    protected static int alpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
