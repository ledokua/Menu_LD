package net.ledok.menuld.api;

import net.ledok.menuld.config.MenuConfigs;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** Addon API (client side, both loaders). Call it from your {@link MenuLdAddon}. See API.md. */
public final class MenuLdApi {
    private static final Map<Identifier, MenuAction> ACTIONS = new ConcurrentHashMap<>();
    private static final List<WidgetId> WIDGET_IDS = new ArrayList<>();

    private MenuLdApi() {
    }

    /** What a layout button with {@code "action": "<your id>"} runs when pressed. */
    @FunctionalInterface
    public interface MenuAction {
        /**
         * @param screen   the title screen the button sits on (use it as the parent of screens you open)
         * @param argument the button's {@code "argument"} string, empty when the layout gives none
         */
        void run(Screen screen, String argument);
    }

    private record WidgetId(String id, Predicate<AbstractWidget> matcher) {
    }

    /**
     * Registers a button action layouts can use as {@code "action": "<namespace>:<path>"}. A later
     * registration under the same id replaces the earlier one.
     */
    public static void registerAction(Identifier id, MenuAction action) {
        ACTIONS.put(Objects.requireNonNull(id), Objects.requireNonNull(action));
    }

    /**
     * Gives a title-screen widget your mod adds a stable id for the layout's {@code widgets} map (instead of
     * its translation key or class name). Use a namespaced id, e.g. {@code "mymod:news"}.
     */
    public static synchronized void registerWidgetId(String id, Predicate<AbstractWidget> matcher) {
        WIDGET_IDS.add(new WidgetId(Objects.requireNonNull(id), Objects.requireNonNull(matcher)));
    }

    /** Re-reads {@code config/menu_ld/menu.json5} now if it changed on disk; true when it was reloaded. */
    public static boolean reloadLayout() {
        return MenuConfigs.reloadIfChanged();
    }

    @Nullable
    public static MenuAction action(Identifier id) {
        return ACTIONS.get(id);
    }

    @Nullable
    public static synchronized String widgetId(AbstractWidget widget) {
        for (WidgetId entry : WIDGET_IDS) {
            if (entry.matcher().test(widget)) return entry.id();
        }
        return null;
    }
}
