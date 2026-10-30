package net.ledok.menuld.title;

import net.ledok.menuld.config.MenuConfig.ButtonStyle;
import net.ledok.menuld.config.MenuConfig.Fit;
import net.ledok.menuld.render.Gfx;
import net.minecraft.client.gui.GuiGraphics;

public final class ButtonSkins {
    private ButtonSkins() {
    }

    /** Draws the styled background; false means "draw the vanilla sprite" (no texture, or not loaded). */
    public static boolean drawBackground(GuiGraphics g, ButtonStyle style, boolean active, boolean hovered,
                                         int x, int y, int w, int h) {
        String ref = !active && style.disabledTexture() != null ? style.disabledTexture()
                : hovered && style.hoverTexture() != null ? style.hoverTexture()
                : style.texture();
        if (ref == null) return false;
        return Gfx.image(g, ref, style.smooth(), x, y, w, h, Fit.STRETCH, Math.max(0, style.slice()), 1f);
    }

    /** Keeps vanilla's alpha and its gray for inactive buttons. */
    public static int labelColor(ButtonStyle style, boolean active, boolean hovered, int vanilla) {
        if (!active) return vanilla;
        Integer color = hovered && style.labelHoverColor() != null ? style.labelHoverColor() : style.labelColor();
        return color == null ? vanilla : (vanilla & 0xFF000000) | (color & 0xFFFFFF);
    }
}
