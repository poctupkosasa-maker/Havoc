package havoc.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import havoc.utilites.MovementUtil;
import java.util.Arrays;

@ModuleInfo(name = "Jesus", desc = "Luma water movement", category = Category.MOVEMENT)
public final class Jesus extends Module {
   private final SelectSetting mode = this.registerSetting(new SelectSetting("Режим", "Matrix", Arrays.asList("Matrix", "MetaHVH", "FunTime New")));
   private final SliderSetting funTimeSpeed = this.registerSetting(
      new SliderSetting("Скорость FT", 0.08F, 0.01F, 0.2F, 0.01F).visibleWhen(() -> this.mode.getValue().equals("FunTime New"))
   );

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.player.isInWater()) {
         double speed = this.mode.getValue().equals("FunTime New")
            ? this.funTimeSpeed.getValue().floatValue()
            : (this.mode.getValue().equals("MetaHVH") ? 0.34 : 0.25);
         MovementUtil.setHorizontalSpeed(speed);
         if (mc.player.getDeltaMovement().y < 0.0) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.02, mc.player.getDeltaMovement().z);
         }
      }
   }
}
