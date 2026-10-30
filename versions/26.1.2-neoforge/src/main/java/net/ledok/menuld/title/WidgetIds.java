package net.ledok.menuld.title;

import net.ledok.menuld.api.MenuLdApi;
import net.ledok.menuld.config.MenuConfig.WidgetRule;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Names the title screen's widgets for the layout's {@code widgets} map. Known buttons get short ids;
 * any other widget can be matched by its label's translation key or its class name.
 */
public final class WidgetIds {
    private static final Map<String, String> BY_TRANSLATION_KEY = Map.ofEntries(
            Map.entry("menu.singleplayer", "singleplayer"),
            Map.entry("menu.multiplayer", "multiplayer"),
            Map.entry("menu.online", "realms"),
            Map.entry("menu.options", "options"),
            Map.entry("menu.quit", "quit"),
            Map.entry("options.language", "language"),
            Map.entry("options.accessibility", "accessibility"),
            Map.entry("accessibility.onboarding.accessibility.button", "accessibility"),
            Map.entry("title.credits", "copyright"),
            Map.entry("menu.playdemo", "play_demo"),
            Map.entry("menu.resetdemo", "reset_demo"),
            Map.entry("fml.menu.mods", "mods"),
            Map.entry("modmenu.title", "mods"));

    private WidgetIds() {
    }

    /** The short id, else the translation key, else the class name: what the debug overlay shows. */
    public static String describe(AbstractWidget widget) {
        String known = known(widget);
        if (known != null) return known;
        String key = translationKey(widget.getMessage());
        return key != null ? key : widget.getClass().getSimpleName();
    }

    @Nullable
    public static WidgetRule rule(Map<String, WidgetRule> rules, AbstractWidget widget) {
        if (rules.isEmpty()) return null;
        String known = known(widget);
        if (known != null && rules.containsKey(known)) return rules.get(known);
        String key = translationKey(widget.getMessage());
        if (key != null && rules.containsKey(key)) return rules.get(key);
        WidgetRule byClass = rules.get(widget.getClass().getName());
        return byClass != null ? byClass : rules.get(widget.getClass().getSimpleName());
    }

    @Nullable
    private static String known(AbstractWidget widget) {
        String registered = MenuLdApi.widgetId(widget);
        if (registered != null) return registered;
        String className = widget.getClass().getName();
        // Mod Menu's button labels carry the mod count, so match its classes instead.
        if (className.startsWith("com.terraformersmc.modmenu.gui.widget.")) return "mods";
        String key = translationKey(widget.getMessage());
        return key == null ? null : BY_TRANSLATION_KEY.get(key);
    }

    @Nullable
    private static String translationKey(Component component) {
        if (component.getContents() instanceof TranslatableContents translatable) return translatable.getKey();
        return null;
    }
}
