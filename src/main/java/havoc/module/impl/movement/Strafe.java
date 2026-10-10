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

@ModuleInfo(name = "Strafe", desc = "Luma strafe modes", category = Category.MOVEMENT)
public final class Strafe extends Module {
   private final SelectSetting mode = this.registerSetting(new SelectSetting("Mode", "Matrix", Arrays.asList("Matrix", "Grim")));
   private final SliderSetting speed = this.registerSetting(
      new SliderSetting("Speed", 0.42F, 0.0F, 1.0F, 0.01F).visibleWhen(() -> this.mode.getValue().equals("Matrix"))
   );

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && MovementUtil.isMoving()) {
         if (this.mode.getValue().equals("Matrix")) {
            MovementUtil.setHorizontalSpeed(this.speed.getValue().floatValue() * 1.5);
         }
      }
   }
}
