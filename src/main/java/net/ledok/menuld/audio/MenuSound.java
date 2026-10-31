package net.ledok.menuld.audio;

import net.ledok.menuld.config.MenuConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;

/**
 * A non-positional menu track that stops itself once a world is joined. Title-only tracks keep running
 * silently under other menus and fade back in on return, so loops never restart.
 */
final class MenuSound extends AbstractTickableSoundInstance {
    /** Volume change per tick while fading: a full fade takes half a second. */
    private static final float FADE_STEP = 0.1f;
    private final boolean fromFile;
    private final float fullVolume;
    private final boolean titleOnly;

    MenuSound(Identifier id, MenuConfig.Audio track, boolean fromFile) {
        super(SoundEvent.createVariableRangeEvent(id), track.source(), RandomSource.create());
        this.fromFile = fromFile;
        this.fullVolume = track.volume();
        this.titleOnly = track.titleOnly();
        this.volume = titleOnly && !onTitleScreen() ? 0f : fullVolume;
        this.looping = track.loop();
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }

    void setDelay(int ticks) {
        this.delay = ticks;
    }

    /** File tracks have no sounds.json entry: describe the sound here, always streamed. */
    @Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        if (!fromFile) return super.resolve(manager);
        this.sound = new Sound(getIdentifier(), ConstantFloat.of(1f), ConstantFloat.of(1f), 1, Sound.Type.FILE,
                true, false, 16);
        return new WeighedSoundEvents(getIdentifier(), null);
    }

    /** Starts even at volume 0, so raising the slider later makes it audible. */
    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (Minecraft.getInstance().level != null) {
            stop();
            return;
        }
        if (titleOnly) {
            float target = onTitleScreen() ? fullVolume : 0f;
            float step = FADE_STEP * Math.max(fullVolume, 0.01f);
            volume = volume < target ? Math.min(target, volume + step) : Math.max(target, volume - step);
        }
    }

    private static boolean onTitleScreen() {
        return Minecraft.getInstance().screen instanceof TitleScreen;
    }
}
