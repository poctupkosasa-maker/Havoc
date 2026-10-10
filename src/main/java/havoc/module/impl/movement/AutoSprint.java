package havoc.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.SelectSetting;
import havoc.utilites.MovementUtil;
import java.util.List;
import net.minecraft.world.level.block.Blocks;

@ModuleInfo(name = "AutoSprint", desc = "Автоматически включает бег", category = Category.MOVEMENT, enabledByDefault = true)
public final class AutoSprint extends Module {
   private static final String NORMAL = "Обычный";
   private static final String PACKET = "Пакетный";
   private final SelectSetting mode = this.registerSetting(new SelectSetting("Мод", "Обычный", List.of("Обычный", "Пакетный")));
   private final BooleanSetting water = this.registerSetting(new BooleanSetting("Спринт в воде", false));

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.level != null) {
         boolean inWater = mc.player.isInWater() || mc.player.isUnderWater();
         boolean safeToSprint = MovementUtil.isMoving()
            && !mc.player.isFallFlying()
            && !mc.player.horizontalCollision
            && !mc.player.isShiftKeyDown()
            && !mc.player.isUsingItem()
            && (this.water.isValue() || !inWater);
         if (!safeToSprint) {
            this.stopSprint();
         } else {
            if ("Пакетный".equals(this.mode.getValue())) {
               mc.options.keySprint.setDown(false);
               mc.player.setSprinting(true);
            } else {
               mc.options.keySprint.setDown(true);
            }
         }
      }
   }

   private void stopSprint() {
      mc.options.keySprint.setDown(false);
      mc.player.setSprinting(false);
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         this.stopSprint();
      }

      super.onDisable();
   }
}
