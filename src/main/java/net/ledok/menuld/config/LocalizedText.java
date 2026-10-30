package net.ledok.menuld.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Text a layout shows. Three spellings: a plain string; a chat component ({@code {"translate": "key"}},
 * {@code {"text": "...", "color": "gold"}}); or one string per game language
 * ({@code {"en_us": "Play", "uk_ua": "Грати"}}), falling back to {@code en_us}, then to any entry.
 */
public final class LocalizedText {
    @Nullable
    private final Component component;
    private final Map<String, String> byLanguage;

    private LocalizedText(@Nullable Component component, Map<String, String> byLanguage) {
        this.component = component;
        this.byLanguage = byLanguage;
    }

    public static LocalizedText parse(JsonElement json) {
        if (json.isJsonPrimitive()) {
            return new LocalizedText(Component.literal(json.getAsString()), Map.of());
        }
        if (json.isJsonObject() && !isComponent(json.getAsJsonObject())) {
            Map<String, String> map = new LinkedHashMap<>();
            json.getAsJsonObject().entrySet().forEach(e -> map.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue().getAsString()));
            if (map.isEmpty()) throw new IllegalArgumentException("empty language map");
            return new LocalizedText(null, map);
        }
        Component parsed = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(msg -> new IllegalArgumentException("bad text component: " + msg));
        return new LocalizedText(parsed, Map.of());
    }

    private static boolean isComponent(JsonObject o) {
        return o.has("text") || o.has("translate") || o.has("keybind") || o.has("extra") || o.has("type");
    }

    public Component get() {
        if (component != null) return component;
        String language = Minecraft.getInstance().getLanguageManager().getSelected().toLowerCase(Locale.ROOT);
        String text = byLanguage.get(language);
        if (text == null) text = byLanguage.get("en_us");
        if (text == null) text = byLanguage.values().iterator().next();
        return Component.literal(text);
    }
}
