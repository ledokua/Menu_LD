package net.ledok.menuld.title;

import net.ledok.menuld.audio.MenuAudio;
import net.ledok.menuld.config.MenuConfig;
import net.ledok.menuld.config.MenuConfig.Element;
import net.ledok.menuld.config.MenuConfig.Fade;
import net.ledok.menuld.config.MenuConfig.Title;
import net.ledok.menuld.config.MenuConfig.WidgetRule;
import net.ledok.menuld.config.MenuConfigs;
import net.ledok.menuld.effect.MenuEffect;
import net.ledok.menuld.render.Elements;
import net.ledok.menuld.render.Gfx;
import net.ledok.menuld.render.Rect;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Applies the layout to the vanilla title screen in place: other mods' buttons and hooks keep working,
 * the layout only moves, hides and restyles widgets and adds its own elements around them.
 */
public final class TitleMenu {
    private static final List<FadedWidget> FADED = new ArrayList<>();
    private static final List<Map.Entry<MenuConfig.Effect, MenuEffect>> EFFECTS = new ArrayList<>();
    private static Title effectsBuiltFor;
    private static WeakReference<Screen> bound = new WeakReference<>(null);
    private static long openedAt;

    private record FadedWidget(AbstractWidget widget, Fade fade) {
    }

    private TitleMenu() {
    }

    private static Title layout() {
        return MenuConfigs.current().title();
    }

    /** {@code TitleScreen.added()}: the screen was (re)opened, not just resized. */
    public static void onOpened() {
        MenuConfigs.reloadIfChanged();
        openedAt = Util.getMillis();
        MenuAudio.startMenuAudio();
    }

    /** After the screen and every other mod finished adding widgets (also after each resize). */
    public static void afterInit(Screen screen, Consumer<AbstractWidget> addWidget) {
        if (!(screen instanceof TitleScreen)) return;
        bound = new WeakReference<>(screen);
        FADED.clear();
        Title layout = layout();
        for (GuiEventListener child : List.copyOf(screen.children())) {
            if (!(child instanceof AbstractWidget widget)) continue;
            WidgetRule rule = WidgetIds.rule(layout.widgets(), widget);
            if (rule == null) {
                if (layout.hideUnlistedWidgets()) widget.visible = false;
                continue;
            }
            apply(rule, widget, screen);
        }
        for (Element element : layout.elements()) {
            if (!(element instanceof MenuConfig.Button spec)) continue;
            Rect rect = Rect.place(spec.placement(), screen.width, screen.height, 200, 20);
            Component label = spec.label() != null ? spec.label().get() : Component.empty();
            MenuButton button = new MenuButton(rect, label, b -> MenuButton.run(spec, screen));
            if (spec.style().changesSomething()) ((SkinnedButton) (Object) button).menuld$setStyle(spec.style());
            if (spec.tooltip() != null) button.setTooltip(Tooltip.create(spec.tooltip().get()));
            addWidget.accept(button);
            if (spec.fade().animates()) FADED.add(new FadedWidget(button, spec.fade()));
        }
    }

    private static void apply(WidgetRule rule, AbstractWidget widget, Screen screen) {
        if (rule.hidden()) {
            widget.visible = false;
            return;
        }
        if (rule.placement() != null) {
            Rect r = Rect.place(rule.placement(), screen.width, screen.height, widget.getWidth(), widget.getHeight());
            widget.setRectangle(r.width(), r.height(), r.x(), r.y());
        }
        if (rule.label() != null) widget.setMessage(rule.label().get());
        if (rule.tooltip() != null) widget.setTooltip(Tooltip.create(rule.tooltip().get()));
        if (widget instanceof AbstractButton && rule.style().changesSomething()) {
            ((SkinnedButton) widget).menuld$setStyle(rule.style());
        }
        if (rule.fade().animates()) FADED.add(new FadedWidget(widget, rule.fade()));
    }

    private static boolean isBound(Screen screen) {
        return bound.get() == screen;
    }

    private static float elapsedSeconds() {
        long now = Util.getMillis();
        // Fades start once the loading overlay is gone, not underneath it.
        if (Minecraft.getInstance().getOverlay() != null) openedAt = now;
        return (now - openedAt) / 1000f;
    }

    /** Replaces the panorama call: background, elements behind the buttons, widget fades. */
    public static void renderBehind(Screen screen, GuiGraphics g, Runnable vanillaPanorama) {
        if (!isBound(screen)) {
            vanillaPanorama.run();
            return;
        }
        float elapsed = elapsedSeconds();
        for (FadedWidget faded : FADED) {
            faded.widget().setAlpha(faded.fade().alpha(elapsed));
        }
        if (!Gfx.background(g, MenuConfigs.current().background(), screen.width, screen.height, 1f)) {
            vanillaPanorama.run();
        }
        renderEffects(g, screen, MenuConfig.EffectLayer.BACKGROUND, elapsed);
        for (Element element : layout().elements()) {
            if (!Elements.isFront(element)) {
                Elements.render(g, element, screen.width, screen.height, element.fade().alpha(elapsed));
            }
        }
        renderEffects(g, screen, MenuConfig.EffectLayer.BEHIND_BUTTONS, elapsed);
    }

    /** Effect instances live as long as the layout object, so particles survive resizes and screen hops. */
    private static void renderEffects(GuiGraphics g, Screen screen, MenuConfig.EffectLayer layer, float elapsed) {
        Title layout = layout();
        if (effectsBuiltFor != layout) {
            effectsBuiltFor = layout;
            EFFECTS.clear();
            for (MenuConfig.Effect spec : layout.effects()) {
                EFFECTS.add(Map.entry(spec, MenuEffect.create(spec)));
            }
        }
        List<MenuEffect.Occluder> occluders = null;
        Minecraft mc = Minecraft.getInstance();
        double mouseX = mc.mouseHandler.xpos() * screen.width / mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * screen.height / mc.getWindow().getScreenHeight();
        for (Map.Entry<MenuConfig.Effect, MenuEffect> entry : EFFECTS) {
            if (entry.getKey().layer() != layer) continue;
            if (entry.getKey().hideUnderButtons() && occluders == null) occluders = occluders(screen, elapsed);
            entry.getValue().render(g, screen.width, screen.height, mouseX, mouseY,
                    entry.getKey().hideUnderButtons() ? occluders : List.of());
        }
    }

    /** Visible widgets with how far each has faded in; effects thin out inside them by that much. */
    private static List<MenuEffect.Occluder> occluders(Screen screen, float elapsed) {
        List<MenuEffect.Occluder> out = new ArrayList<>();
        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof AbstractWidget w) || !w.visible || w.getWidth() <= 0 || w.getHeight() <= 0) continue;
            float alpha = 1f;
            for (FadedWidget faded : FADED) {
                if (faded.widget() == w) alpha = faded.fade().alpha(elapsed);
            }
            if (alpha > 0) out.add(new MenuEffect.Occluder(w.getX(), w.getY(), w.getX() + w.getWidth(), w.getY() + w.getHeight(), alpha));
        }
        return out;
    }

    /** End of the title screen's render: front elements, debug overlay, config problems. */
    public static void renderFront(Screen screen, GuiGraphics g) {
        if (!isBound(screen)) return;
        Title layout = layout();
        float elapsed = elapsedSeconds();
        for (Element element : layout.elements()) {
            if (Elements.isFront(element)) {
                Elements.render(g, element, screen.width, screen.height, element.fade().alpha(elapsed));
            }
        }
        renderEffects(g, screen, MenuConfig.EffectLayer.FRONT, elapsed);
        Font font = Minecraft.getInstance().font;
        if (layout.debug()) {
            for (GuiEventListener child : screen.children()) {
                if (!(child instanceof AbstractWidget w)) continue;
                boolean listed = WidgetIds.rule(layout.widgets(), w) != null || w instanceof MenuButton;
                int color = !w.visible ? 0xFFFF5555 : listed ? 0xFF55FF55 : 0xFFFFFF55;
                g.renderOutline(w.getX(), w.getY(), w.getWidth(), w.getHeight(), color);
                String label = (w.visible ? "" : "hidden: ") + WidgetIds.describe(w) + " " + w.getX() + "," + w.getY();
                g.pose().pushPose();
                g.pose().translate(w.getX(), w.getY() - 5, 400);
                g.pose().scale(0.5f, 0.5f, 1);
                g.drawString(font, label, 0, 0, color);
                g.pose().popPose();
            }
        }
        List<String> problems = MenuConfigs.problems();
        if (!problems.isEmpty()) {
            g.drawString(font, Component.translatable("menu_ld.config_problems", problems.size(), MenuConfigs.FILE_NAME),
                    2, 2, 0xFFFF5555);
            if (layout.debug()) {
                for (int i = 0; i < Math.min(problems.size(), 12); i++) {
                    g.drawString(font, problems.get(i), 2, 13 + i * 10, 0xFFFFAAAA);
                }
            }
        }
    }

    /** Other menus' panorama, when {@code background.all_menus} is on; false keeps vanilla's. */
    public static boolean renderMenuBackground(GuiGraphics g, int width, int height) {
        MenuConfig.Background background = MenuConfigs.current().background();
        return background.allMenus() && Gfx.background(g, background, width, height, 1f);
    }

    public static boolean showLogo() {
        return layout().logo();
    }

    public static boolean showSplash() {
        return layout().splash();
    }

    public static boolean showRealmsNotifications() {
        return layout().realmsNotifications();
    }
}
