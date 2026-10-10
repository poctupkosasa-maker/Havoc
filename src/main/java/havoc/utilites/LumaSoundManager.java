package havoc.utilites;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class LumaSoundManager implements IMinecraft {
   public static final SoundEvent MOAN_1 = register("hit_moan1");
   public static final SoundEvent MOAN_2 = register("hit_moan2");
   public static final SoundEvent MOAN_3 = register("hit_moan3");
   public static final SoundEvent MOAN_4 = register("hit_moan4");
   public static final SoundEvent METALLIC = register("hit_metallic");
   public static final SoundEvent CRIME = register("hit_crime");
   public static final SoundEvent MODULE_ENABLE = register("module_enable");
   public static final SoundEvent MODULE_DISABLE = register("module_disable");
   public static final SoundEvent MODULE_ON = register("module_on");
   public static final SoundEvent MODULE_OFF = register("module_off");

   private LumaSoundManager() {
   }

   public static void init() {
   }

   public static void play(SoundEvent event, float volume) {
      if (mc != null) {
         mc.getSoundManager().play(SimpleSoundInstance.forUI(event, 1.0F, volume));
      }
   }

   private static SoundEvent register(String path) {
      Identifier id = Identifier.fromNamespaceAndPath("havoc", path);
      return (SoundEvent)Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
   }
}
