package havoc.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventAttackEntity;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;

@ModuleInfo(name = "ShiftTap", desc = "Briefly sneaks after a hit", category = Category.COMBAT)
public final class ShiftTap extends Module {
   private long releaseAt;
   private boolean controlled;

   @Subscribe
   public void onAttack(EventAttackEntity event) {
      if (mc.player != null) {
         this.releaseAt = System.currentTimeMillis() + 25L;
         mc.options.keyShift.setDown(true);
         this.controlled = true;
      }
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (this.controlled && System.currentTimeMillis() >= this.releaseAt) {
         this.release();
      }
   }

   private void release() {
      if (this.controlled) {
         mc.options.keyShift.setDown(false);
         this.controlled = false;
      }
   }

   @Override
   public void onDisable() {
      this.release();
      super.onDisable();
   }
}
