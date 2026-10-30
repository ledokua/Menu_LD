package net.ledok.menuld.api;

/**
 * An addon's entry point, run once on the client while Menu_LD starts: Fabric's {@code "menu_ld"}
 * entrypoint, a NeoForge {@code ServiceLoader} provider of this interface. Register through
 * {@link MenuLdApi} from here; Menu_LD only loads the class when it is installed itself.
 */
public interface MenuLdAddon {
    void register();
}
