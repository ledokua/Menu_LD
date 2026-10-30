package net.ledok.menuld.neoforge;

import net.ledok.menuld.MenuLd;
import net.ledok.menuld.api.MenuLdAddon;
import net.ledok.menuld.title.TitleMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ServiceLoader;

/** Client-only mod: there is no server-side instance at all. */
@Mod(value = MenuLd.MOD_ID, dist = Dist.CLIENT)
public final class MenuLdNeoForge {
    public MenuLdNeoForge(IEventBus modBus) {
        MenuLd.init();
        ServiceLoader.load(MenuLdAddon.class).forEach(MenuLdAddon::register);

        // LOWEST, so buttons other mods add in their own Init.Post listeners exist already.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ScreenEvent.Init.Post e) ->
                TitleMenu.afterInit(e.getScreen(), e::addListener));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn e) -> MenuLd.onWorldJoined());
    }
}
