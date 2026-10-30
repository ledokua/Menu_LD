package net.ledok.menuld.effect;

import net.ledok.menuld.audio.MenuAudio;
import net.ledok.menuld.config.MenuConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.RandomSource;

import java.util.List;

/** Falling streaks; with {@code thunder}, an occasional double flash and an optional thunder sound. */
final class RainEffect extends MenuEffect {
    private static final int MAX_DROPS = 2000;

    private final MenuConfig.Effect spec;
    private final RandomSource random = RandomSource.create();
    private final float[] x = new float[MAX_DROPS];
    private final float[] y = new float[MAX_DROPS];
    private final float[] length = new float[MAX_DROPS];
    private final float[] speed = new float[MAX_DROPS];
    private final float[] opacity = new float[MAX_DROPS];
    private int count;
    private int seededWidth = -1;
    private int seededHeight = -1;
    private float untilThunder;
    private float flashTime = -1;

    RainEffect(MenuConfig.Effect spec) {
        this.spec = spec;
        this.untilThunder = nextThunderDelay();
    }

    private float nextThunderDelay() {
        return 8 + random.nextFloat() * 22;
    }

    private void seed(int width, int height) {
        // About one drop per 900 GUI px² at intensity 1 (≈250 on a 640×360 GUI).
        count = Math.min(MAX_DROPS, Math.round(width * height / 900f * spec.intensity()));
        for (int i = 0; i < count; i++) {
            respawn(i, width, height, random.nextFloat() * height);
        }
        seededWidth = width;
        seededHeight = height;
    }

    private void respawn(int i, int width, int height, float startY) {
        float depth = 0.4f + random.nextFloat() * 0.6f;
        x[i] = random.nextFloat() * width;
        y[i] = startY;
        length[i] = (5 + 9 * depth) * spec.scale();
        speed[i] = (180 + 220 * depth) * spec.speed();
        opacity[i] = 0.18f + 0.4f * depth;
    }

    @Override
    protected void draw(GuiGraphicsExtractor g, int width, int height, float dt,
                        double mouseX, double mouseY, List<Occluder> occluders) {
        if (width != seededWidth || height != seededHeight) seed(width, height);
        int rgb = spec.color() & 0xFFFFFF;
        float thickness = Math.max(0.5f, spec.scale());
        for (int i = 0; i < count; i++) {
            y[i] += speed[i] * dt;
            if (y[i] > height) respawn(i, width, height, -length[i] - random.nextFloat() * 20);
            streak(g, x[i], y[i], y[i] + length[i], thickness, rgb, 0, opacity[i], occluders);
        }
        if (spec.thunder()) thunder(g, width, height, dt);
    }

    private void thunder(GuiGraphicsExtractor g, int width, int height, float dt) {
        if (flashTime < 0) {
            untilThunder -= dt;
            if (untilThunder <= 0) {
                flashTime = 0;
                if (spec.thunderSound() != null) {
                    // Sound lags the flash a little, like distant thunder.
                    MenuAudio.playOnce(spec.thunderSound(), spec.thunderVolume(), 6 + random.nextInt(20));
                }
            }
            return;
        }
        flashTime += dt;
        float strength = flash(flashTime);
        if (flashTime > 1f) {
            flashTime = -1;
            untilThunder = nextThunderDelay();
        }
        if (strength > 0) {
            int color = alpha(0xFFFFFF, strength * spec.thunderBrightness());
            quad(g, 0, 0, width, height, color, color);
        }
    }

    /** Two quick strikes: a bright one, a dimmer echo, then darkness. */
    private static float flash(float t) {
        if (t < 0.06f) return t / 0.06f;
        if (t < 0.18f) return 1 - (t - 0.06f) / 0.12f * 0.8f;
        if (t < 0.26f) return 0.2f + (t - 0.18f) / 0.08f * 0.5f;
        if (t < 0.9f) return 0.7f * (1 - (t - 0.26f) / 0.64f);
        return 0;
    }
}
