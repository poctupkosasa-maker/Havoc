package havoc.module.impl.player;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SliderSetting;

@ModuleInfo(name = "LockSlot", desc = "Locks selected hotbar slot", category = Category.PLAYER)
public final class LockSlot extends Module {
   private final SliderSetting slot = this.registerSetting(new SliderSetting("Slot", 1.0F, 1.0F, 9.0F, 1.0F));

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null) {
         mc.player.getInventory().setSelectedSlot(Math.round(this.slot.getValue()) - 1);
      }
   }
}
