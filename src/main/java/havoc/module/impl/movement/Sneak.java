package havoc.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventInput;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;

@ModuleInfo(name = "Sneak", desc = "Keeps sneak pressed", category = Category.MOVEMENT)
public final class Sneak extends Module {
   @Subscribe
   public void onInput(EventInput event) {
      event.setSneak(true);
   }
}
