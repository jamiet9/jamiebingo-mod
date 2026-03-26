package com.jamie.jamiebingo.sound;

import com.jamie.jamiebingo.JamieBingo;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Optional;

public final class ModSounds {

    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, JamieBingo.MOD_ID);

    public static final RegistryObject<SoundEvent> COMPLETE_SLOT =
            SOUND_EVENTS.register("complete_slot",
                    () -> new SoundEvent(
                            com.jamie.jamiebingo.util.IdUtil.id(JamieBingo.MOD_ID + ":complete_slot"),
                            Optional.empty()
                    ));

    public static final RegistryObject<SoundEvent> COMPLETE_LINE =
            SOUND_EVENTS.register("complete_line",
                    () -> new SoundEvent(
                            com.jamie.jamiebingo.util.IdUtil.id(JamieBingo.MOD_ID + ":complete_line"),
                            Optional.empty()
                    ));

    public static void register(BusGroup modBus) {
        SOUND_EVENTS.register(modBus);
    }
}

