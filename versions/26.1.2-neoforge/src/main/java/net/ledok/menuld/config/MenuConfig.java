package net.ledok.menuld.config;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/** The parsed {@code config/menu_ld/menu.json5}. Colors are ARGB ints; asset paths are unresolved refs. */
public record MenuConfig(Background background, List<Audio> audio, Title title, Loading loading) {
    public static final MenuConfig VANILLA = new MenuConfig(Background.NONE, List.of(), Title.VANILLA, Loading.VANILLA);

    public enum Fit { STRETCH, COVER, CONTAIN }

    /** {@code color} with alpha 0 means "no fill". */
    public record Background(@Nullable String image, int color, Fit fit, boolean smooth, boolean allMenus) {
        public static final Background NONE = new Background(null, 0, Fit.COVER, true, false);

        public boolean isSet() {
            return image != null || (color >>> 24) != 0;
        }
    }

    public record Audio(String sound, SoundSource source, float volume, boolean loop) {
    }

    /** Width/height below 0 keep the widget's own size; stretch fills that screen axis. */
    public record Placement(Anchor anchor, int x, int y, int width, int height, boolean stretchX, boolean stretchY,
                            boolean stayOnScreen) {
    }

    public record Fade(float opacity, float fadeIn, float delay) {
        public static final Fade NONE = new Fade(1, 0, 0);

        public float alpha(float elapsedSeconds) {
            float t = elapsedSeconds - delay;
            if (t < 0) return 0;
            return fadeIn <= 0 ? opacity : opacity * Mth.clamp(t / fadeIn, 0, 1);
        }

        public boolean animates() {
            return fadeIn > 0 || delay > 0 || opacity < 1;
        }
    }

    /** Null fields inherit from {@link Title#buttonStyle()}; {@code slice} 0 stretches the whole texture. */
    public record ButtonStyle(@Nullable String texture, @Nullable String hoverTexture,
                              @Nullable String disabledTexture, int slice, boolean smooth,
                              @Nullable Integer labelColor, @Nullable Integer labelHoverColor) {
        public static final ButtonStyle EMPTY = new ButtonStyle(null, null, null, -1, false, null, null);

        public ButtonStyle over(ButtonStyle base) {
            return new ButtonStyle(
                    texture != null ? texture : base.texture,
                    hoverTexture != null ? hoverTexture : base.hoverTexture,
                    disabledTexture != null ? disabledTexture : base.disabledTexture,
                    slice >= 0 ? slice : base.slice,
                    smooth || base.smooth,
                    labelColor != null ? labelColor : base.labelColor,
                    labelHoverColor != null ? labelHoverColor : base.labelHoverColor);
        }

        public boolean changesSomething() {
            return texture != null || labelColor != null || labelHoverColor != null;
        }
    }

    /** {@code placement} null keeps the widget where its screen put it. */
    public record WidgetRule(boolean hidden, @Nullable Placement placement, Fade fade, ButtonStyle style,
                             @Nullable LocalizedText label, @Nullable LocalizedText tooltip) {
    }

    /** CUSTOM: an action another mod registered through {@code MenuLdApi.registerAction}. */
    public enum Action { LINK, JOIN_SERVER, SINGLEPLAYER, MULTIPLAYER, REALMS, OPTIONS, MODS, LANGUAGE, ACCESSIBILITY, QUIT, CUSTOM }

    public enum Align { LEFT, CENTER, RIGHT }

    public sealed interface Element permits Image, Button, Text {
        Placement placement();

        Fade fade();
    }

    public record Image(Placement placement, Fade fade, String image, Fit fit, boolean smooth, int slice,
                        boolean front) implements Element {
    }

    /**
     * {@code target}: the URL for LINK, {@code host:port} for JOIN_SERVER, the {@code argument} for CUSTOM;
     * {@code customAction} is set only for CUSTOM.
     */
    public record Button(Placement placement, Fade fade, ButtonStyle style, @Nullable LocalizedText label,
                         @Nullable LocalizedText tooltip, Action action, @Nullable Identifier customAction,
                         String target, boolean confirm) implements Element {
    }

    public record Text(Placement placement, Fade fade, LocalizedText text, int color, boolean shadow, float scale,
                       Align align, boolean front) implements Element {
    }

    public enum EffectType { RAIN, FIREFLIES }

    /** BACKGROUND: right over the background, under images and buttons; BEHIND_BUTTONS: over images. */
    public enum EffectLayer { BACKGROUND, BEHIND_BUTTONS, FRONT }

    /**
     * Unused fields are ignored by the other effect type. {@code hideUnderButtons}: the effect fades out
     * inside visible widgets as they fade in, so see-through buttons don't show it.
     */
    public record Effect(EffectType type, float intensity, float speed, float scale, int color, EffectLayer layer,
                         boolean hideUnderButtons, boolean thunder, float thunderBrightness,
                         @Nullable String thunderSound, float thunderVolume, boolean followMouse) {
    }

    public record Title(boolean logo, boolean splash, boolean realmsNotifications, boolean hideUnlistedWidgets,
                        boolean debug, ButtonStyle buttonStyle, Map<String, WidgetRule> widgets,
                        List<Element> elements, List<Effect> effects) {
        public static final Title VANILLA = new Title(true, true, true, false, false, ButtonStyle.EMPTY, Map.of(),
                List.of(), List.of());
    }

    /** {@code placement} null keeps vanilla's bar geometry. */
    public record ProgressBar(boolean hidden, @Nullable Placement placement, int color, int trackColor,
                              int borderColor) {
        public static final ProgressBar VANILLA = new ProgressBar(false, null, 0xFFFFFFFF, 0, 0xFFFFFFFF);
    }

    /** {@code color} with alpha 0 keeps vanilla's red / dark background. */
    public record Loading(int color, Background background, boolean mojangLogo, List<Element> elements,
                          ProgressBar progressBar) {
        public static final Loading VANILLA = new Loading(0, Background.NONE, true, List.of(), ProgressBar.VANILLA);

        public boolean isVanilla() {
            return this.equals(VANILLA);
        }
    }
}
