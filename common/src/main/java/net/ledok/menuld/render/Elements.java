package net.ledok.menuld.render;

import net.ledok.menuld.config.MenuConfig;
import net.ledok.menuld.config.MenuConfig.Element;
import net.ledok.menuld.image.MenuImage;
import net.ledok.menuld.image.MenuImages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Draws image and text elements; buttons are real widgets and draw themselves. */
public final class Elements {
    private Elements() {
    }

    public static boolean isFront(Element element) {
        return element instanceof MenuConfig.Image image ? image.front()
                : element instanceof MenuConfig.Text text && text.front();
    }

    public static void render(GuiGraphics g, Element element, int screenWidth, int screenHeight, float alpha) {
        if (alpha <= 0) return;
        switch (element) {
            case MenuConfig.Image image -> {
                MenuImage loaded = MenuImages.get(image.image(), image.smooth());
                if (loaded == null) return;
                Rect r = Rect.place(image.placement(), screenWidth, screenHeight, loaded.width(), loaded.height());
                Gfx.image(g, image.image(), image.smooth(), r.x(), r.y(), r.width(), r.height(), image.fit(),
                        image.slice(), alpha);
            }
            case MenuConfig.Text text -> {
                // x/y is the point the text aligns to (left edge, center or right edge).
                Font font = Minecraft.getInstance().font;
                Rect r = Rect.place(text.placement(), screenWidth, screenHeight, 0,
                        Math.round(font.lineHeight * text.scale()));
                Gfx.text(g, text.text().get(), r, text, alpha);
            }
            case MenuConfig.Button ignored -> {
            }
        }
    }
}
