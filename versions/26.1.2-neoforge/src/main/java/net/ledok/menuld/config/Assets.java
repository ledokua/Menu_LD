package net.ledok.menuld.config;

import net.minecraft.IdentifierException;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves an asset reference from the layout: {@code namespace:path} reads a resource-pack resource,
 * anything else is a file path relative to {@code config/menu_ld/}.
 */
public final class Assets {
    private Assets() {
    }

    public static boolean isResource(String ref) {
        return ref.indexOf(':') > 0;
    }

    public static Path file(String ref) throws IOException {
        Path base = MenuConfigs.directory().toAbsolutePath().normalize();
        Path path = base.resolve(ref).normalize();
        if (!path.startsWith(base)) throw new IOException("'" + ref + "' points outside config/menu_ld/");
        return path;
    }

    /** Callers close the stream. Resource refs need the resource manager, so not during the very first load. */
    public static InputStream open(String ref) throws IOException {
        if (isResource(ref)) {
            Identifier id;
            try {
                id = Identifier.parse(ref);
            } catch (IdentifierException e) {
                throw new IOException("'" + ref + "' is not a valid resource id");
            }
            return Minecraft.getInstance().getResourceManager().open(id);
        }
        return Files.newInputStream(file(ref));
    }
}
