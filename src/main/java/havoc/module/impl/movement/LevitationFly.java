package havoc.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SliderSetting;
import havoc.utilites.MovementUtil;
import net.minecraft.world.effect.MobEffects;

@ModuleInfo(name = "LevitationFly", desc = "Controls flight with levitation", category = Category.MOVEMENT)
public final class LevitationFly extends Module {
   private final SliderSetting speed = this.registerSetting(new SliderSetting("Скорость", 0.5F, 0.1F, 2.0F, 0.1F));
   private final SliderSetting vertical = this.registerSetting(new SliderSetting("Вертикальная скорость", 0.3F, 0.1F, 1.0F, 0.05F));

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.player.hasEffect(MobEffects.LEVITATION)) {
         MovementUtil.setHorizontalSpeed(this.speed.getValue().floatValue() / 3.0);
         double y = mc.options.keyJump.isDown() ? this.vertical.getValue().floatValue() : (mc.options.keyShift.isDown() ? -this.vertical.getValue() : 0.0);
         mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, y, mc.player.getDeltaMovement().z);
      }
   }
}
