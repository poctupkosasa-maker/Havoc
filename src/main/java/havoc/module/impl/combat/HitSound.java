package havoc.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventAttackEntity;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import havoc.utilites.LumaSoundManager;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;

@ModuleInfo(name = "HitSound", desc = "Plays Luma hit sounds", category = Category.COMBAT)
public final class HitSound extends Module {
   private final SelectSetting type = this.registerSetting(new SelectSetting("Sound", "Moan", Arrays.asList("Moan", "Metallic", "Crime")));
   private final SliderSetting volume = this.registerSetting(new SliderSetting("Volume", 1.0F, 0.1F, 2.0F, 0.1F));

   @Subscribe
   public void onAttack(EventAttackEntity event) {
      if (event.getTarget() instanceof LivingEntity) {
         LumaSoundManager.play(this.selectedSound(), this.volume.getValue());
      }
   }

   private SoundEvent selectedSound() {
      return switch (this.type.getValue()) {
         case "Metallic" -> LumaSoundManager.METALLIC;
         case "Crime" -> LumaSoundManager.CRIME;
         default -> {
            switch (ThreadLocalRandom.current().nextInt(4)) {
               case 0:
                  yield LumaSoundManager.MOAN_1;
               case 1:
                  yield LumaSoundManager.MOAN_2;
               case 2:
                  yield LumaSoundManager.MOAN_3;
               default:
                  yield LumaSoundManager.MOAN_4;
            }
         }
      };
   }
}
