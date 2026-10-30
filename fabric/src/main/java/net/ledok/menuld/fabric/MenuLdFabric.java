package net.ledok.menuld.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.loader.api.FabricLoader;
import net.ledok.menuld.MenuLd;
import net.ledok.menuld.api.MenuLdAddon;
import net.ledok.menuld.title.TitleMenu;
import net.minecraft.resources.ResourceLocation;

public final class MenuLdFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuLd.init();
        FabricLoader.getInstance().getEntrypoints("menu_ld", MenuLdAddon.class).forEach(MenuLdAddon::register);

        // Runs after every default-phase listener, so buttons other mods add (Mod Menu's) exist already.
        ResourceLocation late = MenuLd.id("after_other_mods");
        ScreenEvents.AFTER_INIT.addPhaseOrdering(Event.DEFAULT_PHASE, late);
        ScreenEvents.AFTER_INIT.register(late, (client, screen, width, height) ->
                TitleMenu.afterInit(screen, widget -> Screens.getButtons(screen).add(widget)));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> MenuLd.onWorldJoined());
    }
}
