package net.ledok.menuld.effect;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ledok.menuld.config.MenuConfig;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

import java.util.List;

/** Slowly wandering, pulsing glow dots that drift toward the mouse when it comes close. */
final class FirefliesEffect extends MenuEffect {
    private static final int MAX_FLIES = 300;
    private static final float MOUSE_RADIUS = 60;

    private final MenuConfig.Effect spec;
    private final RandomSource random = RandomSource.create();
    private final float[] x = new float[MAX_FLIES];
    private final float[] y = new float[MAX_FLIES];
    private final float[] vx = new float[MAX_FLIES];
    private final float[] vy = new float[MAX_FLIES];
    private final float[] phase = new float[MAX_FLIES];
    private final float[] pulse = new float[MAX_FLIES];
    private int count;
    private int seededWidth = -1;
    private int seededHeight = -1;
    private float time;

    FirefliesEffect(MenuConfig.Effect spec) {
        this.spec = spec;
    }

    private void seed(int width, int height) {
        // About 24 on a 640×360 GUI at intensity 1.
        count = Math.min(MAX_FLIES, Math.round(width * height / 9600f * spec.intensity()));
        for (int i = 0; i < count; i++) {
            x[i] = random.nextFloat() * width;
            y[i] = height * (0.35f + random.nextFloat() * 0.65f);
            float angle = random.nextFloat() * Mth.TWO_PI;
            vx[i] = Mth.cos(angle) * 6;
            vy[i] = Mth.sin(angle) * 6;
            phase[i] = random.nextFloat() * Mth.TWO_PI;
            pulse[i] = 0.6f + random.nextFloat() * 1.2f;
        }
        seededWidth = width;
        seededHeight = height;
    }

    @Override
    protected void draw(VertexConsumer buffer, Matrix4f pose, int width, int height, float dt,
                        double mouseX, double mouseY, List<Occluder> occluders) {
        if (width != seededWidth || height != seededHeight) seed(width, height);
        time += dt;
        int rgb = spec.color() & 0xFFFFFF;
        float size = spec.scale();
        float maxSpeed = 14 * spec.speed();
        for (int i = 0; i < count; i++) {
            // Wander: nudge the heading a little every frame.
            vx[i] += (random.nextFloat() - 0.5f) * 30 * dt;
            vy[i] += (random.nextFloat() - 0.5f) * 30 * dt;
            if (spec.followMouse()) {
                float dx = (float) mouseX - x[i];
                float dy = (float) mouseY - y[i];
                float distance = Mth.sqrt(dx * dx + dy * dy);
                if (distance < MOUSE_RADIUS && distance > 4) {
                    vx[i] += dx / distance * 20 * dt;
                    vy[i] += dy / distance * 20 * dt;
                }
            }
            float speed = Mth.sqrt(vx[i] * vx[i] + vy[i] * vy[i]);
            if (speed > maxSpeed) {
                vx[i] *= maxSpeed / speed;
                vy[i] *= maxSpeed / speed;
            }
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
            if (x[i] < -4) x[i] = width + 4;
            if (x[i] > width + 4) x[i] = -4;
            if (y[i] < height * 0.2f) vy[i] += 8 * dt;
            if (y[i] > height + 4) y[i] = -4;

            float glow = (0.25f + 0.75f * (0.5f + 0.5f * Mth.sin(time * pulse[i] + phase[i])))
                    * visibility(occluders, x[i], y[i]);
            if (glow <= 0.01f) continue;
            float halo = 1.5f * size;
            int haloColor = alpha(rgb, 0.18f * glow);
            int coreColor = alpha(rgb, 0.95f * glow);
            quad(buffer, pose, x[i] - halo, y[i] - halo, x[i] + halo, y[i] + halo, haloColor, haloColor);
            quad(buffer, pose, x[i] - size / 2, y[i] - size / 2, x[i] + size / 2, y[i] + size / 2, coreColor, coreColor);
        }
    }
}
