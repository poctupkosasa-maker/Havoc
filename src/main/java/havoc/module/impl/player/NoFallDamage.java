package havoc.module.impl.player;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SelectSetting;
import java.util.List;

@ModuleInfo(name = "NoFallDamage", desc = "Luma SpookyTime fall protection", category = Category.PLAYER)
public final class NoFallDamage extends Module {
   private final SelectSetting mode = this.registerSetting(new SelectSetting("Mode", "SpookyTime", List.of("SpookyTime")));

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.level != null) {
         if (mc.player.fallDistance > 4.0 && mc.player.getDeltaMovement().y < 0.0) {
            mc.player.setDeltaMovement(0.0, 0.0, 0.0);
         }
      }
   }
}
