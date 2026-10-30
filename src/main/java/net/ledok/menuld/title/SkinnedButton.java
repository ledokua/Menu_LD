package net.ledok.menuld.title;

import net.ledok.menuld.config.MenuConfig.ButtonStyle;
import org.jetbrains.annotations.Nullable;

/** Mixed into every {@code AbstractButton}: a layout style replacing its sprite and label color. */
public interface SkinnedButton {
    @Nullable
    ButtonStyle menuld$style();

    void menuld$setStyle(@Nullable ButtonStyle style);
}
