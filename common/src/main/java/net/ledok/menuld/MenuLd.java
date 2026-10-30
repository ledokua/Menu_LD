package net.ledok.menuld;

import net.ledok.menuld.audio.MenuAudio;
import net.ledok.menuld.config.MenuConfigs;
import net.ledok.menuld.image.MenuImages;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-only mod: everything here runs on the render thread unless a method says otherwise. */
public final class MenuLd {
    public static final String MOD_ID = "menu_ld";
    public static final Logger LOGGER = LoggerFactory.getLogger("Menu_LD");

    private MenuLd() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Loads the config early, so the very first loading overlay already uses it. */
    public static void init() {
        MenuConfigs.reloadIfChanged();
    }

    /** Menu music stops and menu images are freed until the player is back in the menus. */
    public static void onWorldJoined() {
        MenuAudio.stopAll();
        MenuImages.releaseAll();
    }
}
