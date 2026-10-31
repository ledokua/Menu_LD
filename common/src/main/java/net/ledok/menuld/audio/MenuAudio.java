package net.ledok.menuld.audio;

import net.ledok.menuld.MenuLd;
import net.ledok.menuld.config.Assets;
import net.ledok.menuld.config.MenuConfig;
import net.ledok.menuld.config.MenuConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Menu music and ambience: plays while no world is loaded, replaces vanilla's menu music when one of the
 * tracks uses the music channel. {@code .ogg} files are streamed straight from {@code config/menu_ld/}
 * under made-up {@code menu_ld:audio/<n>} ids that {@code SoundBufferLibrary} hands to {@link #openStream}.
 */
public final class MenuAudio {
    private static final String STREAM_PREFIX = "sounds/audio/";
    private static final Map<Integer, Path> FILES = new HashMap<>();
    private static final Map<Path, Integer> FILE_IDS = new HashMap<>();
    private static final List<MenuSound> PLAYING = new ArrayList<>();
    private static int counter;

    private MenuAudio() {
    }

    /** Starts every configured track that is not already playing. */
    public static void startMenuAudio() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) return;
        List<MenuConfig.Audio> tracks = MenuConfigs.current().audio();
        if (tracks.isEmpty()) return;
        // Returning from Options & co. keeps the tracks running; they restart only once all have ended.
        if (PLAYING.stream().anyMatch(s -> mc.getSoundManager().isActive(s))) return;
        stopAll();
        for (MenuConfig.Audio track : tracks) {
            MenuSound sound = create(track);
            if (sound == null) continue;
            PLAYING.add(sound);
            mc.getSoundManager().play(sound);
        }
    }

    @Nullable
    private static MenuSound create(MenuConfig.Audio track) {
        if (Assets.isResource(track.sound())) {
            ResourceLocation event = ResourceLocation.tryParse(track.sound());
            if (event == null) {
                MenuLd.LOGGER.warn("Menu audio: '{}' is not a valid sound event id", track.sound());
                return null;
            }
            return new MenuSound(event, track, false);
        }
        Path file;
        try {
            file = Assets.file(track.sound());
        } catch (IOException e) {
            MenuLd.LOGGER.warn("Menu audio: {}", e.getMessage());
            return null;
        }
        if (!Files.isRegularFile(file)) {
            MenuLd.LOGGER.warn("Menu audio: {} does not exist", file);
            return null;
        }
        int index = FILE_IDS.computeIfAbsent(file, f -> counter++);
        FILES.put(index, file);
        return new MenuSound(MenuLd.id("audio/" + index), track, true);
    }

    public static void stopAll() {
        if (!PLAYING.isEmpty()) {
            for (MenuSound sound : PLAYING) {
                Minecraft.getInstance().getSoundManager().stop(sound);
            }
            PLAYING.clear();
        }
        FILES.clear();
        FILE_IDS.clear();
    }

    /** A one-off title screen sound on the weather channel (menu thunder), {@code delayTicks} from now. */
    public static void playOnce(String sound, float volume, int delayTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) return;
        MenuSound instance = create(new MenuConfig.Audio(sound, SoundSource.WEATHER, volume, false, true));
        if (instance == null) return;
        instance.setDelay(delayTicks);
        mc.getSoundManager().play(instance);
    }

    /** Vanilla's menu music stays silent while a configured music-channel track exists for the menus. */
    public static boolean replacesMenuMusic() {
        if (Minecraft.getInstance().level != null) return false;
        for (MenuConfig.Audio track : MenuConfigs.current().audio()) {
            if (track.source() == SoundSource.MUSIC) return true;
        }
        return false;
    }

    /**
     * Called for every streamed sound; non-null for our own ids. {@code path} is the sound's file path,
     * {@code menu_ld:sounds/audio/<n>.ogg}. Runs on the render thread; the read itself is asynchronous.
     */
    @Nullable
    public static CompletableFuture<AudioStream> openStream(ResourceLocation path, boolean looping) {
        if (!path.getNamespace().equals(MenuLd.MOD_ID) || !path.getPath().startsWith(STREAM_PREFIX)) return null;
        String name = path.getPath().substring(STREAM_PREFIX.length()).replace(".ogg", "");
        Path file;
        try {
            file = FILES.get(Integer.parseInt(name));
        } catch (NumberFormatException e) {
            file = null;
        }
        if (file == null) return CompletableFuture.failedFuture(new IOException("unknown menu sound " + path));
        Path source = file;
        return CompletableFuture.supplyAsync(() -> {
            try {
                InputStream in = new BufferedInputStream(Files.newInputStream(source));
                return looping ? new LoopingAudioStream(JOrbisAudioStream::new, in) : new JOrbisAudioStream(in);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, net.minecraft.Util.nonCriticalIoPool());
    }
}
