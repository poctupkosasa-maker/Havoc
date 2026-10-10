package havoc.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import havoc.utilites.MovementUtil;
import java.util.List;
import net.minecraft.world.level.block.Blocks;

@ModuleInfo(name = "WaterSpeed", desc = "Luma swimming acceleration", category = Category.MOVEMENT)
public final class WaterSpeed extends Module {
   private final SelectSetting mode = this.registerSetting(new SelectSetting("Режим", "FunTime", List.of("FunTime")));
   private final BooleanSetting iceBoost = this.registerSetting(new BooleanSetting("Ускорение под льдом", true));
   private final SliderSetting iceSpeed = this.registerSetting(
      new SliderSetting("Скорость под льдом", 1.5F, 1.0F, 3.0F, 0.1F).visibleWhen(this.iceBoost::isValue)
   );

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.level != null && mc.player.isSwimming()) {
         double speed = 0.34;
         if (this.iceBoost.isValue() && mc.level.getBlockState(mc.player.blockPosition().above()).is(Blocks.ICE)) {
            speed *= this.iceSpeed.getValue().floatValue();
         }

         MovementUtil.setHorizontalSpeed(speed);
      }
   }
}
