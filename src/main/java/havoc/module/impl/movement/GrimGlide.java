package havoc.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SliderSetting;

@ModuleInfo(name = "GlideFly", desc = "Softens falling while gliding/falling", category = Category.MOVEMENT)
public final class GrimGlide extends Module {
   private final SliderSetting fallSpeed = this.registerSetting(new SliderSetting("Fall Speed", 0.05F, 0.0F, 0.5F, 0.01F));

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && !mc.player.onGround()) {
         if (!mc.options.keyShift.isDown()) {
            if (mc.player.getDeltaMovement().y < -this.fallSpeed.getValue()) {
               mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -this.fallSpeed.getValue(), mc.player.getDeltaMovement().z);
            }
         }
      }
   }
}
