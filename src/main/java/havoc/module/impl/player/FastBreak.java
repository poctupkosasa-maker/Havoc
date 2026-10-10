package havoc.module.impl.player;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

@ModuleInfo(name = "FastBreak", desc = "Speeds up block breaking", category = Category.PLAYER)
public final class FastBreak extends Module {
   private final BooleanSetting haste = this.registerSetting(new BooleanSetting("Haste", true));

   @Override
   public void onDisable() {
      if (mc.player != null) {
         mc.player.removeEffect(MobEffects.HASTE);
      }

      super.onDisable();
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && this.haste.isValue()) {
         mc.player.addEffect(new MobEffectInstance(MobEffects.HASTE, 40, 1, false, false));
      }
   }
}
