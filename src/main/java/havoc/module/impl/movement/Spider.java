package havoc.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SelectSetting;
import java.util.Arrays;

@ModuleInfo(name = "Spider", desc = "Luma wall climbing modes", category = Category.MOVEMENT)
public final class Spider extends Module {
   private final SelectSetting mode = this.registerSetting(
      new SelectSetting("Режим", "Slime Block", Arrays.asList("SpookyTime", "FunTime", "Slime Block", "Water Bucket"))
   );

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.player.horizontalCollision) {
         double y = switch (this.mode.getValue()) {
            case "SpookyTime" -> 0.42;
            case "FunTime" -> 0.36;
            default -> 0.28;
         };
         mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, y, mc.player.getDeltaMovement().z);
      }
   }
}
