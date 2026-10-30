package net.ledok.menuld.audio;

import net.ledok.menuld.config.MenuConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;

/** A non-positional menu track that stops itself once a world is joined. */
final class MenuSound extends AbstractTickableSoundInstance {
    private final boolean fromFile;

    MenuSound(Identifier id, MenuConfig.Audio track, boolean fromFile) {
        super(SoundEvent.createVariableRangeEvent(id), track.source(), RandomSource.create());
        this.fromFile = fromFile;
        this.volume = track.volume();
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
        if (Minecraft.getInstance().level != null) stop();
    }
}
