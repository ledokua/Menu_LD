package net.ledok.menuld.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.ledok.menuld.config.MenuConfig.Action;
import net.ledok.menuld.config.MenuConfig.Align;
import net.ledok.menuld.config.MenuConfig.Audio;
import net.ledok.menuld.config.MenuConfig.Background;
import net.ledok.menuld.config.MenuConfig.ButtonStyle;
import net.ledok.menuld.config.MenuConfig.Element;
import net.ledok.menuld.config.MenuConfig.Fade;
import net.ledok.menuld.config.MenuConfig.Fit;
import net.ledok.menuld.config.MenuConfig.Loading;
import net.ledok.menuld.config.MenuConfig.Placement;
import net.ledok.menuld.config.MenuConfig.ProgressBar;
import net.ledok.menuld.config.MenuConfig.Title;
import net.ledok.menuld.config.MenuConfig.WidgetRule;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Lenient JSON → {@link MenuConfig}. A bad value is reported (with its path) and replaced by its default,
 * so one typo never throws the whole layout away; unknown keys are reported too.
 */
final class ConfigParser {
    private final List<String> problems = new ArrayList<>();

    List<String> problems() {
        return problems;
    }

    MenuConfig parse(JsonObject json) {
        Obj root = new Obj(json, "");
        Background background = root.has("background") ? background(root.obj("background"), true) : Background.NONE;
        List<Audio> audio = new ArrayList<>();
        for (Obj o : root.objects("audio")) {
            audio.add(audio(o));
        }
        Title title = root.has("title_screen") ? title(root.obj("title_screen")) : Title.VANILLA;
        Loading loading = root.has("loading_screen") ? loading(root.obj("loading_screen")) : Loading.VANILLA;
        root.finish();
        return new MenuConfig(background, List.copyOf(audio), title, loading);
    }

    private Background background(Obj o, boolean menus) {
        Background b = new Background(o.string("image", null), o.color("color", 0), o.enumValue("fit", Fit.class, Fit.STRETCH),
                o.bool("smooth", true), menus && o.bool("all_menus", false));
        o.finish();
        return b;
    }

    private Audio audio(Obj o) {
        String sound = o.string("sound", null);
        if (sound == null) {
            o.problem("missing \"sound\" (a .ogg file in config/menu_ld/ or a sound event id)");
            sound = "";
        }
        SoundSource source = SoundSource.MUSIC;
        String sourceName = o.string("source", "music");
        boolean found = false;
        for (SoundSource s : SoundSource.values()) {
            if (s.getName().equalsIgnoreCase(sourceName)) {
                source = s;
                found = true;
            }
        }
        if (!found) o.problem("unknown sound source '" + sourceName + "'");
        Audio a = new Audio(sound, source, o.number("volume", 1f), o.bool("loop", true));
        o.finish();
        return a;
    }

    private Title title(Obj o) {
        ButtonStyle defaults = o.has("button_style") ? style(o.obj("button_style"), true) : ButtonStyle.EMPTY;
        Map<String, WidgetRule> widgets = new LinkedHashMap<>();
        if (o.has("widgets")) {
            Obj w = o.obj("widgets");
            for (String key : w.keys()) {
                widgets.put(key, widgetRule(w.obj(key), defaults));
            }
            w.finish();
        }
        List<Element> elements = new ArrayList<>();
        for (Obj e : o.objects("elements")) {
            Element element = element(e, defaults, true);
            if (element != null) elements.add(element);
        }
        List<MenuConfig.Effect> effects = new ArrayList<>();
        for (Obj e : o.objects("effects")) {
            MenuConfig.Effect effect = effect(e);
            if (effect != null) effects.add(effect);
        }
        Title t = new Title(o.bool("logo", true), o.bool("splash", true), o.bool("realms_notifications", true),
                o.bool("hide_unlisted_widgets", false), o.bool("debug", false), defaults, Map.copyOf(widgets),
                List.copyOf(elements), List.copyOf(effects));
        o.finish();
        return t;
    }

    private WidgetRule widgetRule(Obj o, ButtonStyle defaults) {
        boolean hidden = o.bool("hidden", false);
        Placement placement = o.has("anchor") || o.has("x") || o.has("y") || o.has("width") || o.has("height")
                || o.has("stretch_x") || o.has("stretch_y") ? placement(o) : null;
        WidgetRule rule = new WidgetRule(hidden, placement, fade(o), style(o, false).over(defaults),
                o.text("label"), o.text("tooltip"));
        o.finish();
        return rule;
    }

    @Nullable
    private Element element(Obj o, ButtonStyle defaults, boolean allowButtons) {
        String type = o.string("type", "");
        Element e = switch (type) {
            case "image" -> {
                String image = o.string("image", null);
                if (image == null) {
                    o.problem("image element without \"image\"");
                    yield null;
                }
                yield new MenuConfig.Image(placement(o), fade(o), image, o.enumValue("fit", Fit.class, Fit.STRETCH),
                        o.bool("smooth", false), o.integer("slice", 0), o.bool("front", false));
            }
            case "text" -> {
                LocalizedText text = o.text("text");
                if (text == null) {
                    o.problem("text element without \"text\"");
                    yield null;
                }
                yield new MenuConfig.Text(placement(o), fade(o), text, o.color("color", 0xFFFFFFFF),
                        o.bool("shadow", true), o.number("scale", 1f), o.enumValue("align", Align.class, Align.LEFT),
                        o.bool("front", false));
            }
            case "button" -> {
                if (!allowButtons) {
                    o.problem("buttons only work on the title screen");
                    yield null;
                }
                String actionName = o.string("action", "");
                Identifier custom = null;
                Action action;
                if (actionName.indexOf(':') > 0) {
                    custom = Identifier.tryParse(actionName);
                    action = custom == null ? null : Action.CUSTOM;
                    if (custom == null) o.problem("'" + actionName + "' is not a valid action id");
                } else {
                    action = o.enumValue("action", Action.class, null);
                }
                if (action == null) {
                    if (actionName.isEmpty()) o.problem("button without an \"action\"");
                    yield null;
                }
                String target = switch (action) {
                    case LINK -> o.string("url", "");
                    case JOIN_SERVER -> o.string("address", "");
                    case CUSTOM -> o.string("argument", "");
                    default -> "";
                };
                if ((action == Action.LINK || action == Action.JOIN_SERVER) && target.isEmpty()) {
                    o.problem(action == Action.LINK ? "link button without \"url\"" : "join_server button without \"address\"");
                }
                yield new MenuConfig.Button(placement(o), fade(o), style(o, false).over(defaults), o.text("label"),
                        o.text("tooltip"), action, custom, target, o.bool("confirm", false));
            }
            default -> {
                o.problem("unknown element type '" + type + "' (image, text, button)");
                yield null;
            }
        };
        o.finish();
        return e;
    }

    @Nullable
    private MenuConfig.Effect effect(Obj o) {
        MenuConfig.EffectType type = o.enumValue("type", MenuConfig.EffectType.class, null);
        if (type == null) {
            if (!o.has("type")) o.problem("effect without \"type\" (rain, fireflies)");
            o.finish();
            return null;
        }
        boolean rain = type == MenuConfig.EffectType.RAIN;
        MenuConfig.Effect effect = new MenuConfig.Effect(type, o.number("intensity", 1f), o.number("speed", 1f),
                o.number("scale", rain ? 1f : 1.5f), o.color("color", rain ? 0xFFCFE7FF : 0xFFFFE08A),
                o.enumValue("layer", MenuConfig.EffectLayer.class, MenuConfig.EffectLayer.BACKGROUND),
                o.bool("hide_under_buttons", true), rain && o.bool("thunder", false),
                o.number("thunder_brightness", 0.3f),
                rain ? o.string("thunder_sound", null) : null, o.number("thunder_volume", 1f),
                !rain && o.bool("follow_mouse", true));
        o.finish();
        return effect;
    }

    private Loading loading(Obj o) {
        List<Element> elements = new ArrayList<>();
        for (Obj e : o.objects("elements")) {
            Element element = element(e, ButtonStyle.EMPTY, false);
            if (element != null) elements.add(element);
        }
        ProgressBar bar = ProgressBar.VANILLA;
        if (o.has("progress_bar")) {
            Obj p = o.obj("progress_bar");
            Placement placement = p.has("anchor") || p.has("x") || p.has("y") ? placement(p) : null;
            bar = new ProgressBar(p.bool("hidden", false), placement, p.color("color", 0xFFFFFFFF),
                    p.color("track_color", 0), p.color("border_color", 0xFFFFFFFF));
            p.finish();
        }
        Background background = o.has("background") ? background(o.obj("background"), false) : Background.NONE;
        Loading l = new Loading(o.color("color", 0), background, o.bool("mojang_logo", true), List.copyOf(elements), bar);
        o.finish();
        return l;
    }

    private ButtonStyle style(Obj o, boolean isDefaults) {
        return new ButtonStyle(o.string("texture", null), o.string("hover_texture", null),
                o.string("disabled_texture", null), o.integer("slice", isDefaults ? 0 : -1), o.bool("smooth", false),
                o.has("label_color") ? o.color("label_color", 0xFFFFFFFF) : null,
                o.has("label_hover_color") ? o.color("label_hover_color", 0xFFFFFFFF) : null);
    }

    private Placement placement(Obj o) {
        return new Placement(o.anchor("anchor", Anchor.TOP_LEFT), o.integer("x", 0), o.integer("y", 0),
                o.integer("width", -1), o.integer("height", -1), o.bool("stretch_x", false), o.bool("stretch_y", false),
                o.bool("stay_on_screen", true));
    }

    private Fade fade(Obj o) {
        return new Fade(o.number("opacity", 1f), o.number("fade_in", 0f), o.number("delay", 0f));
    }

    /** One JSON object plus its path for messages; remembers which keys were read to flag the rest. */
    private final class Obj {
        private final JsonObject json;
        private final String path;
        private final Set<String> used = new HashSet<>();

        Obj(JsonObject json, String path) {
            this.json = json;
            this.path = path;
        }

        void problem(String message) {
            problems.add((path.isEmpty() ? "" : path + ": ") + message);
        }

        private String at(String key) {
            return path.isEmpty() ? key : path + "." + key;
        }

        boolean has(String key) {
            used.add(key);
            return json.has(key) && !json.get(key).isJsonNull();
        }

        Set<String> keys() {
            used.addAll(json.keySet());
            return json.keySet();
        }

        void finish() {
            for (String key : json.keySet()) {
                if (!used.contains(key)) problems.add(at(key) + ": unknown key (ignored)");
            }
        }

        Obj obj(String key) {
            used.add(key);
            JsonElement e = json.get(key);
            if (e != null && e.isJsonObject()) return new Obj(e.getAsJsonObject(), at(key));
            if (e != null) problems.add(at(key) + ": expected an object");
            return new Obj(new JsonObject(), at(key));
        }

        List<Obj> objects(String key) {
            used.add(key);
            JsonElement e = json.get(key);
            List<Obj> out = new ArrayList<>();
            if (e == null || e.isJsonNull()) return out;
            if (!e.isJsonArray()) {
                problems.add(at(key) + ": expected a list");
                return out;
            }
            JsonArray array = e.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                String p = at(key) + "[" + i + "]";
                if (array.get(i).isJsonObject()) out.add(new Obj(array.get(i).getAsJsonObject(), p));
                else problems.add(p + ": expected an object");
            }
            return out;
        }

        @Nullable
        String string(String key, @Nullable String fallback) {
            if (!has(key)) return fallback;
            JsonElement e = json.get(key);
            if (e.isJsonPrimitive()) return e.getAsString();
            problems.add(at(key) + ": expected a string");
            return fallback;
        }

        boolean bool(String key, boolean fallback) {
            if (!has(key)) return fallback;
            JsonElement e = json.get(key);
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) return e.getAsBoolean();
            problems.add(at(key) + ": expected true or false");
            return fallback;
        }

        float number(String key, float fallback) {
            if (!has(key)) return fallback;
            JsonElement e = json.get(key);
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) return e.getAsFloat();
            problems.add(at(key) + ": expected a number");
            return fallback;
        }

        int integer(String key, int fallback) {
            return Math.round(number(key, fallback));
        }

        /** {@code #RRGGBB} or {@code #RRGGBBAA} (CSS order, like FancyMenu) → ARGB. */
        int color(String key, int fallback) {
            String s = string(key, null);
            if (s == null) return fallback;
            String hex = s.startsWith("#") ? s.substring(1) : s;
            try {
                if (hex.length() == 6) return 0xFF000000 | Integer.parseUnsignedInt(hex, 16);
                if (hex.length() == 8) {
                    int rgba = Integer.parseUnsignedInt(hex, 16);
                    return (rgba >>> 8) | (rgba << 24);
                }
            } catch (NumberFormatException ignored) {
            }
            problems.add(at(key) + ": '" + s + "' is not a #RRGGBB or #RRGGBBAA color");
            return fallback;
        }

        Anchor anchor(String key, Anchor fallback) {
            String s = string(key, null);
            if (s == null) return fallback;
            Anchor a = Anchor.parse(s);
            if (a == null) problems.add(at(key) + ": unknown anchor '" + s + "'");
            return a == null ? fallback : a;
        }

        @Nullable
        <E extends Enum<E>> E enumValue(String key, Class<E> type, @Nullable E fallback) {
            String s = string(key, null);
            if (s == null) return fallback;
            try {
                return Enum.valueOf(type, s.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                List<String> names = new ArrayList<>();
                for (E c : type.getEnumConstants()) names.add(c.name().toLowerCase(Locale.ROOT));
                problems.add(at(key) + ": '" + s + "' is not one of " + names);
                return fallback;
            }
        }

        @Nullable
        LocalizedText text(String key) {
            if (!has(key)) return null;
            try {
                return LocalizedText.parse(json.get(key));
            } catch (RuntimeException ex) {
                problems.add(at(key) + ": " + ex.getMessage());
                return null;
            }
        }
    }
}
