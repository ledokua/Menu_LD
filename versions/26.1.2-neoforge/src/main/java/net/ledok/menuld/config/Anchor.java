package net.ledok.menuld.config;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** The screen point an element's {@code x}/{@code y} offset starts from (the element's top-left corner). */
public enum Anchor {
    TOP_LEFT(0, 0), TOP(1, 0), TOP_RIGHT(2, 0),
    LEFT(0, 1), CENTER(1, 1), RIGHT(2, 1),
    BOTTOM_LEFT(0, 2), BOTTOM(1, 2), BOTTOM_RIGHT(2, 2);

    private final int column;
    private final int row;

    Anchor(int column, int row) {
        this.column = column;
        this.row = row;
    }

    public int originX(int screenWidth) {
        return column * screenWidth / 2;
    }

    public int originY(int screenHeight) {
        return row * screenHeight / 2;
    }

    /** Accepts our names and FancyMenu's ({@code mid-centered}, {@code top-left}, ...). */
    @Nullable
    public static Anchor parse(String name) {
        String n = name.toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (n) {
            case "top_left" -> TOP_LEFT;
            case "top", "top_center", "top_centered" -> TOP;
            case "top_right" -> TOP_RIGHT;
            case "left", "mid_left", "center_left" -> LEFT;
            case "center", "mid_centered", "middle" -> CENTER;
            case "right", "mid_right", "center_right" -> RIGHT;
            case "bottom_left" -> BOTTOM_LEFT;
            case "bottom", "bottom_center", "bottom_centered" -> BOTTOM;
            case "bottom_right" -> BOTTOM_RIGHT;
            default -> null;
        };
    }
}
