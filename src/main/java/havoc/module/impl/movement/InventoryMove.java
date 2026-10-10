package havoc.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventInput;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.SelectSetting;
import java.util.Arrays;

@ModuleInfo(name = "InventoryMove", desc = "Movement while inventory is open", category = Category.MOVEMENT)
public final class InventoryMove extends Module {
   private final SelectSetting mode = this.registerSetting(new SelectSetting("Режим", "Legit", Arrays.asList("Normal", "Legit")));
   private final BooleanSetting grimBypass = this.registerSetting(new BooleanSetting("Grim Bypass", true));

   @Subscribe
   public void onInput(EventInput event) {
      if (mc.gui.screen() != null) {
         event.setForward((mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0));
         event.setStrafe((mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0));
         event.setJump(mc.options.keyJump.isDown());
      }
   }
}
