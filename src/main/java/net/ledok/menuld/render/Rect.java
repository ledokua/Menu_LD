package net.ledok.menuld.render;

import net.ledok.menuld.config.MenuConfig.Placement;

/** A placed element in GUI pixels. */
public record Rect(int x, int y, int width, int height) {
    /** Default sizes are used where the placement leaves width/height unset (below 0). */
    public static Rect place(Placement p, int screenWidth, int screenHeight, int defaultWidth, int defaultHeight) {
        int w = p.stretchX() ? screenWidth : (p.width() >= 0 ? p.width() : defaultWidth);
        int h = p.stretchY() ? screenHeight : (p.height() >= 0 ? p.height() : defaultHeight);
        int x = p.stretchX() ? 0 : p.anchor().originX(screenWidth) + p.x();
        int y = p.stretchY() ? 0 : p.anchor().originY(screenHeight) + p.y();
        if (p.stayOnScreen()) {
            if (w <= screenWidth) x = Math.max(0, Math.min(x, screenWidth - w));
            if (h <= screenHeight) y = Math.max(0, Math.min(y, screenHeight - h));
        }
        return new Rect(x, y, w, h);
    }

    public boolean contains(double px, double py) {
        return px >= x && py >= y && px < x + width && py < y + height;
    }
}
