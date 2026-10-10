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

@ModuleInfo(name = "Fly", desc = "Luma flight modes", category = Category.MOVEMENT)
public final class Fly extends Module {
   private final SelectSetting mode = this.registerSetting(new SelectSetting("Режим", "Normal", Arrays.asList("Normal", "Dragon Fly")));
   private final SliderSetting horizontal = this.registerSetting(new SliderSetting("Скорость XZ", 1.5F, 1.0F, 10.0F, 0.1F));
   private final SliderSetting vertical = this.registerSetting(new SliderSetting("Скорость Y", 1.5F, 0.0F, 10.0F, 0.1F));

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null) {
         if (!this.mode.getValue().equals("Dragon Fly") || mc.player.getAbilities().flying) {
            MovementUtil.setHorizontalSpeed(this.horizontal.getValue().floatValue() / 3.0);
            double y = mc.options.keyJump.isDown() ? this.vertical.getValue().floatValue() : (mc.options.keyShift.isDown() ? -this.vertical.getValue() : 0.0);
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, y, mc.player.getDeltaMovement().z);
         }
      }
   }
}
