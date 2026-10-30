package net.ledok.menuld.platform;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ServiceLoader;

/** Loader-specific lookups; each loader module provides one implementation via ServiceLoader. */
public interface Platform {
    Platform INSTANCE = ServiceLoader.load(Platform.class).findFirst()
            .orElseThrow(() -> new IllegalStateException("No Menu_LD Platform service"));

    /** The game's {@code config/} directory. */
    Path configDir();

    /** The loader's mod list (Mod Menu's on Fabric, when installed); null when there is none. */
    @Nullable
    Screen modsScreen(Screen parent);
}
