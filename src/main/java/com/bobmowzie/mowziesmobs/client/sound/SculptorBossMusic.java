package com.bobmowzie.mowziesmobs.client.sound;

import com.bobmowzie.mowziesmobs.server.entity.sculptor.EntitySculptor;
import com.bobmowzie.mowziesmobs.server.sound.MMSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class SculptorBossMusic extends BossMusic<EntitySculptor> {
    protected static SoundEvent soundEventIntro = MMSounds.MUSIC_SCULPTOR_THEME_INTRO.get();
    protected static SoundEvent soundEventLevel1_1 = MMSounds.MUSIC_SCULPTOR_THEME_LEVEL1_1.get();
    protected static SoundEvent soundEventLevel1_2 = MMSounds.MUSIC_SCULPTOR_THEME_LEVEL1_2.get();
    protected static SoundEvent soundEventLevel2_1 = MMSounds.MUSIC_SCULPTOR_THEME_LEVEL2_1.get();
    protected static SoundEvent soundEventLevel2_2 = MMSounds.MUSIC_SCULPTOR_THEME_LEVEL2_2.get();
    protected static SoundEvent soundEventTransition = MMSounds.MUSIC_SCULPTOR_THEME_TRANSITION.get();
    protected static SoundEvent soundEventLevel3_1 = MMSounds.MUSIC_SCULPTOR_THEME_LEVEL3_1.get();
    protected static SoundEvent soundEventLevel3_2 = MMSounds.MUSIC_SCULPTOR_THEME_LEVEL3_2.get();
    protected static SoundEvent soundEventEnding = MMSounds.MUSIC_SCULPTOR_THEME_ENDING.get();
    protected static SoundEvent soundEventOutro = MMSounds.MUSIC_SCULPTOR_THEME_OUTRO.get();
    protected static SoundEvent soundEventCombat = MMSounds.MUSIC_SCULPTOR_THEME_COMBAT.get();

    protected BossMusicSound soundIntro;
    protected BossMusicSound soundTransition;
    protected BossMusicSound soundEnding;
    protected BossMusicSound soundOutro;
    protected BossMusicSound currentSound;

    private int ticksInSection;

    private enum SculptorMusicSection {
        COMBAT(-1),
        INTRO(0),
        LEVEL1_1(1),
        LEVEL1_2(2),
        LEVEL2_1(3),
        LEVEL2_2(4),
        TRANSITION(5),
        LEVEL3_1(6),
        LEVEL3_2(7),
        ENDING(8),
        OUTRO(9);

        SculptorMusicSection(int order) {
            this.order = order;
        }

        private Integer order;

        public boolean isHigherThan(SculptorMusicSection other) {
            return this.order > other.order;
        }
    }

    private static final SortedMap<SculptorMusicSection, Float> SECTION_HEIGHTS = new TreeMap<>();
    static {
        SECTION_HEIGHTS.put(SculptorMusicSection.LEVEL1_1, 0.0f);
        SECTION_HEIGHTS.put(SculptorMusicSection.LEVEL1_2, 0.1f);
        SECTION_HEIGHTS.put(SculptorMusicSection.LEVEL2_1, 0.35f);
        SECTION_HEIGHTS.put(SculptorMusicSection.LEVEL2_2, 0.5f);
        SECTION_HEIGHTS.put(SculptorMusicSection.LEVEL3_1, 0.65f);
        SECTION_HEIGHTS.put(SculptorMusicSection.LEVEL3_2, 0.825f);
        SECTION_HEIGHTS.put(SculptorMusicSection.ENDING, 0.98f);
    }
    private static final Map<SculptorMusicSection, SoundEvent> SECTION_SOUNDS = new HashMap<>();
    static {
        SECTION_SOUNDS.put(SculptorMusicSection.INTRO, soundEventIntro);
        SECTION_SOUNDS.put(SculptorMusicSection.LEVEL1_1, soundEventLevel1_1);
        SECTION_SOUNDS.put(SculptorMusicSection.LEVEL1_2, soundEventLevel1_2);
        SECTION_SOUNDS.put(SculptorMusicSection.LEVEL2_1, soundEventLevel2_1);
        SECTION_SOUNDS.put(SculptorMusicSection.LEVEL2_2, soundEventLevel2_2);
        SECTION_SOUNDS.put(SculptorMusicSection.TRANSITION, soundEventTransition);
        SECTION_SOUNDS.put(SculptorMusicSection.LEVEL3_1, soundEventLevel3_1);
        SECTION_SOUNDS.put(SculptorMusicSection.LEVEL3_2, soundEventLevel3_2);
        SECTION_SOUNDS.put(SculptorMusicSection.ENDING, soundEventEnding);
        SECTION_SOUNDS.put(SculptorMusicSection.OUTRO, soundEventOutro);
        SECTION_SOUNDS.put(SculptorMusicSection.COMBAT, soundEventCombat);
    }

    private SculptorMusicSection currentSection;

    public SculptorBossMusic() {
        super(null);
    }

    @Override
    public void tick() {
        super.tick();
        ticksInSection++;

        if (getBoss() != null) {
            if (currentSection == SculptorMusicSection.INTRO && ticksInSection == 35) {
                startMainTrack();
                return;
            }

            if (currentSection == SculptorMusicSection.TRANSITION) {
                if (ticksInSection == 512) {
                    if (getBoss().playerProgress() > SECTION_HEIGHTS.get(SculptorMusicSection.LEVEL3_2)) {
                        changeLevelSection(SculptorMusicSection.LEVEL3_2);
                        return;
                    }
                    else if (getBoss().playerProgress() > SECTION_HEIGHTS.get(SculptorMusicSection.LEVEL3_1)) {
                        changeLevelSection(SculptorMusicSection.LEVEL3_1);
                        return;
                    }
                }
            }

            if (currentSection == SculptorMusicSection.ENDING) {
                if (ticksInSection % 64 == 0 && getBoss().isTestPassed()) {
                    changeLevelSection(SculptorMusicSection.OUTRO, false);
                    return;
                }
            }

            if (
                    currentSection == SculptorMusicSection.LEVEL1_1 ||
                    currentSection == SculptorMusicSection.LEVEL1_2 ||
                    currentSection == SculptorMusicSection.LEVEL2_1 ||
                    currentSection == SculptorMusicSection.LEVEL2_2 ||
                    currentSection == SculptorMusicSection.TRANSITION ||
                    currentSection == SculptorMusicSection.LEVEL3_1 ||
                    currentSection == SculptorMusicSection.LEVEL3_2 ||
                    currentSection == SculptorMusicSection.ENDING
            ) {
                if (ticksInSection % 128 == 0) {
                    measureBreak();
                }
            }

            if (currentSection != SculptorMusicSection.COMBAT && ticksInSection % 128 == 0 && getBoss().isFighting()) {
                changeLevelSection(SculptorMusicSection.COMBAT);
            }
        }
    }

    private void startMainTrack() {
        ticksInSection = 0;
        if (getBoss() != null && getBoss().isFighting()) {
            changeLevelSection(SculptorMusicSection.COMBAT);
        }
        else {
            changeLevelSection(SculptorMusicSection.LEVEL1_1);
        }
    }

    private void measureBreak() {
        if (getBoss().isTestPassed()) {
            changeLevelSection(SculptorMusicSection.OUTRO, false);
            return;
        }

        SculptorMusicSection currentSectionIgnoreTransition = currentSection;
        if (currentSection == SculptorMusicSection.TRANSITION) currentSectionIgnoreTransition = SculptorMusicSection.LEVEL3_1;

        float playerProgress = getBoss().playerProgress();
        float currentSectionHeight = SECTION_HEIGHTS.get(currentSectionIgnoreTransition);
        SculptorMusicSection nextSection = SculptorMusicSection.LEVEL1_1;
        for (Map.Entry<SculptorMusicSection, Float> sectionHeight : SECTION_HEIGHTS.entrySet()) {
            SculptorMusicSection section = sectionHeight.getKey();
            float height = sectionHeight.getValue();
            // If the current section is above the height we are checking, then the player moved down. We add a slight buffer before switching tracks.
            if (currentSectionHeight >= height) {
                height -= 0.05;
            }
            // If the player is in this height range, play the associated section
            if (playerProgress > height) {
                nextSection = section;
            }
        }
        if (nextSection != currentSectionIgnoreTransition) {
            // If the current section is below the transition and we are moving to level 3, then play the transition instead
            if (!currentSectionIgnoreTransition.isHigherThan(SculptorMusicSection.TRANSITION) && (nextSection == SculptorMusicSection.LEVEL3_1 || nextSection == SculptorMusicSection.LEVEL3_2)) {
                nextSection = SculptorMusicSection.TRANSITION;
            }
            changeLevelSection(nextSection);
        }
    }

    private void changeLevelSection(SculptorMusicSection section) {
        changeLevelSection(section, true);
    }

    private void changeLevelSection(SculptorMusicSection section, boolean loop) {
        if (currentSound != null) {
            currentSound.fadeOut();
        }
        SoundEvent requestedSoundEvent = SECTION_SOUNDS.get(section);
        currentSound = new BossMusicSound(requestedSoundEvent, getBoss(), this, loop);
        Minecraft.getInstance().getSoundManager().play(currentSound);
        currentSection = section;
        ticksInSection = 0;
    }

    public void play() {
        super.play();
        currentSection = SculptorMusicSection.INTRO;
        soundIntro = new BossMusicSound(soundEventIntro, getBoss(), this, false);
        Minecraft.getInstance().getSoundManager().play(soundIntro);
        ticksInSection = 0;
    }

    @Override
    public void stop() {
        if (soundIntro != null) soundIntro.doStop();
        if (soundTransition != null) soundTransition.doStop();
        if (soundEnding != null) soundEnding.doStop();
        if (soundOutro != null) soundOutro.doStop();
        if (currentSound != null) currentSound.doStop();
        super.stop();
    }
}
