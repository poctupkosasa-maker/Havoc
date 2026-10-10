package havoc.module.impl.player;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventInput;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.InputSetting;

@ModuleInfo(name = "AutoPilot", desc = "Luma waypoint autopilot", category = Category.MOVEMENT)
public final class AutoPilot extends Module {
   private final InputSetting waypoint = this.registerSetting(new InputSetting("Waypoint", "", 64));
   private final BooleanSetting autoFirework = this.registerSetting(new BooleanSetting("Автофейерверк", true));
   private final BooleanSetting autoTakeoff = this.registerSetting(new BooleanSetting("Автовзлёт", true));
   private final BooleanSetting autoLand = this.registerSetting(new BooleanSetting("Автоприземление", false));

   @Subscribe
   public void onInput(EventInput event) {
      event.setForward(1.0F);
      event.setSprint(true);
   }
}
