package net.ledok.menuld.loading;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ledok.menuld.config.MenuConfig;
import net.ledok.menuld.config.MenuConfig.Loading;
import net.ledok.menuld.config.MenuConfig.ProgressBar;
import net.ledok.menuld.config.MenuConfigs;
import net.ledok.menuld.render.Elements;
import net.ledok.menuld.render.Gfx;
import net.ledok.menuld.render.Rect;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The resource-loading overlay (game start, F3+T, resource pack changes). Vanilla's overlay keeps its
 * timing and fades; this only swaps its background color, logo and progress bar.
 */
public final class LoadingScreen {
    private LoadingScreen() {
    }

    private static Loading layout() {
        return MenuConfigs.current().loading();
    }

    /** Replaces vanilla's red (or dark-mode black) brand color; alpha is set by the caller. */
    public static int backgroundColor(int vanilla) {
        int color = layout().color();
        return (color >>> 24) == 0 ? vanilla : color | 0xFF000000;
    }

    /**
     * Called in place of each of the two Mojang logo halves, with the overlay's fade already in the shader
     * color. The first call draws the layout (background image and elements); returns whether vanilla's
     * logo half should be skipped.
     */
    public static boolean replaceLogoHalf(GuiGraphics g, boolean firstHalf) {
        Loading layout = layout();
        if (layout.isVanilla()) return false;
        if (firstHalf) {
            // Vanilla draws the logo with additive blending; layout images need the normal blend.
            drawLayout(g, layout, 1f);
            if (layout.mojangLogo()) {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(770, 1);
            }
        }
        return !layout.mojangLogo();
    }

    private static void drawLayout(GuiGraphics g, Loading layout, float alpha) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        if (layout.background().image() != null) {
            Gfx.image(g, layout.background().image(), layout.background().smooth(), 0, 0, w, h,
                    layout.background().fit(), 0, alpha);
        }
        for (MenuConfig.Element element : layout.elements()) {
            Elements.render(g, element, w, h, element.fade().opacity() * alpha);
        }
    }

    /** @return whether vanilla's bar should be skipped. {@code x0..y1} is vanilla's bar rectangle. */
    public static boolean drawProgressBar(GuiGraphics g, int x0, int y0, int x1, int y1, float progress, float alpha) {
        ProgressBar bar = layout().progressBar();
        if (bar.equals(ProgressBar.VANILLA)) return false;
        drawBar(g, bar, x0, y0, x1, y1, progress, alpha);
        return true;
    }

    private static void drawBar(GuiGraphics g, ProgressBar bar, int x0, int y0, int x1, int y1, float progress, float alpha) {
        if (bar.hidden()) return;
        if (bar.placement() != null) {
            Rect r = Rect.place(bar.placement(), g.guiWidth(), g.guiHeight(), x1 - x0, y1 - y0);
            x0 = r.x();
            y0 = r.y();
            x1 = r.x() + r.width();
            y1 = r.y() + r.height();
        }
        Gfx.fill(g, x0 + 1, y0 + 1, x1 - 1, y1 - 1, bar.trackColor(), alpha);
        int filled = (int) Math.ceil((x1 - x0 - 4) * progress);
        Gfx.fill(g, x0 + 2, y0 + 2, x0 + 2 + filled, y1 - 2, bar.color(), alpha);
        Gfx.fill(g, x0 + 1, y0, x1 - 1, y0 + 1, bar.borderColor(), alpha);
        Gfx.fill(g, x0 + 1, y1 - 1, x1 - 1, y1, bar.borderColor(), alpha);
        Gfx.fill(g, x0, y0, x0 + 1, y1, bar.borderColor(), alpha);
        Gfx.fill(g, x1 - 1, y0, x1, y1, bar.borderColor(), alpha);
    }

    /**
     * Draws the whole overlay over whatever is underneath (NeoForge's startup overlay renders its own early
     * loading screen instead of vanilla's, so it is covered rather than edited).
     */
    public static void renderStandalone(GuiGraphics g, float progress, float fade, float barAlpha) {
        Loading layout = layout();
        int vanillaBrand = 0xFFEF323D;
        Gfx.fill(g, 0, 0, g.guiWidth(), g.guiHeight(), backgroundColor(vanillaBrand), fade);
        drawLayout(g, layout, fade);
        if (barAlpha > 0) {
            int w = g.guiWidth();
            int h = g.guiHeight();
            // Vanilla's geometry: half-logo width either side of the center, at 83.25% height.
            int half = (int) (Math.min(w * 0.75, h) * 0.25 * 2);
            int y = (int) (h * 0.8325);
            drawBar(g, layout.progressBar(), w / 2 - half, y - 5, w / 2 + half, y + 5, progress, barAlpha);
        }
    }

    /** NeoForge's startup overlay is only covered when the layout changes something. */
    public static boolean isCustomized() {
        return !layout().isVanilla();
    }
}
