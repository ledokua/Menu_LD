package net.ledok.menuld.neoforge;

import net.ledok.menuld.platform.Platform;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.ModListScreen;

import java.nio.file.Path;

public final class NeoForgePlatform implements Platform {
    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Screen modsScreen(Screen parent) {
        return new ModListScreen(parent);
    }
}
