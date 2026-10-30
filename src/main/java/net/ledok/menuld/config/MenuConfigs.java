package net.ledok.menuld.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.ledok.menuld.MenuLd;
import net.ledok.menuld.audio.MenuAudio;
import net.ledok.menuld.image.MenuImages;
import net.ledok.menuld.platform.Platform;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

/**
 * Owns {@code config/menu_ld/menu.json5}. The file is re-read whenever its modification time changes and
 * the title screen opens, so a layout can be tuned without restarting the game.
 */
public final class MenuConfigs {
    public static final String FILE_NAME = "menu.json5";

    private static MenuConfig current = MenuConfig.VANILLA;
    private static List<String> problems = List.of();
    private static FileTime loadedStamp;

    private MenuConfigs() {
    }

    public static MenuConfig current() {
        return current;
    }

    /** Problems found by the last load; shown on the title screen until fixed. */
    public static List<String> problems() {
        return problems;
    }

    public static Path directory() {
        return Platform.INSTANCE.configDir().resolve(MenuLd.MOD_ID);
    }

    /** @return whether a new config was loaded. */
    public static boolean reloadIfChanged() {
        Path file = directory().resolve(FILE_NAME);
        try {
            if (!Files.exists(file)) writeDefault(file);
            FileTime stamp = Files.getLastModifiedTime(file);
            if (stamp.equals(loadedStamp)) return false;
            loadedStamp = stamp;
            load(file);
        } catch (IOException e) {
            MenuLd.LOGGER.error("Could not read {}", file, e);
            apply(MenuConfig.VANILLA, List.of("could not read " + FILE_NAME + ": " + e.getMessage()));
        }
        return true;
    }

    private static void load(Path file) throws IOException {
        JsonElement json;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonReader jsonReader = new JsonReader(reader);
            // Comments and trailing commas, as in json5; MC 1.21.1's Gson predates Strictness.
            jsonReader.setLenient(true);
            json = JsonParser.parseReader(jsonReader);
        } catch (RuntimeException e) {
            MenuLd.LOGGER.error("{} is not valid JSON: {}", FILE_NAME, e.getMessage());
            apply(MenuConfig.VANILLA, List.of(FILE_NAME + " is not valid JSON: " + e.getMessage()));
            return;
        }
        if (json.isJsonNull()) {
            apply(MenuConfig.VANILLA, List.of());
            return;
        }
        if (!json.isJsonObject()) {
            apply(MenuConfig.VANILLA, List.of(FILE_NAME + " must contain one JSON object"));
            return;
        }
        ConfigParser parser = new ConfigParser();
        MenuConfig config = parser.parse(json.getAsJsonObject());
        for (String problem : parser.problems()) {
            MenuLd.LOGGER.warn("{}: {}", FILE_NAME, problem);
        }
        MenuLd.LOGGER.info("Loaded {} ({} problem(s))", FILE_NAME, parser.problems().size());
        apply(config, List.copyOf(parser.problems()));
    }

    private static void apply(MenuConfig config, List<String> found) {
        current = config;
        problems = found;
        MenuImages.releaseAll();
        MenuAudio.stopAll();
    }

    private static void writeDefault(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                // Menu_LD layout. Every key is described in the mod's README. Images and .ogg sounds are paths
                // relative to this folder (or namespace:path resources). Saved changes apply the next time the
                // title screen opens: go to Options and back.
                {
                  "title_screen": {
                    "logo": true,
                    "splash": true,
                    "widgets": {},
                    "elements": []
                  },
                  "loading_screen": {}
                }
                """, StandardCharsets.UTF_8);
    }
}
