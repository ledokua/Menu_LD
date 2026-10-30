package net.ledok.menuld.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.ledok.menuld.MenuLd;
import net.ledok.menuld.platform.Platform;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public final class FabricPlatform implements Platform {
    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    /** Mod Menu's public API, looked up reflectively so it stays an optional dependency. */
    @Override
    @Nullable
    public Screen modsScreen(Screen parent) {
        if (!FabricLoader.getInstance().isModLoaded("modmenu")) return null;
        try {
            return (Screen) Class.forName("com.terraformersmc.modmenu.api.ModMenuApi")
                    .getMethod("createModsScreen", Screen.class)
                    .invoke(null, parent);
        } catch (ReflectiveOperationException | ClassCastException e) {
            MenuLd.LOGGER.warn("Could not open Mod Menu's mod list", e);
            return null;
        }
    }
}
