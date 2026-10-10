package havoc.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.SliderSetting;
import net.minecraft.world.item.Items;

@ModuleInfo(name = "AutoGApple", desc = "Automatically eats golden apples", category = Category.COMBAT)
public final class AutoGApple extends Module {
   private final SliderSetting health = this.registerSetting(new SliderSetting("Health", 16.0F, 3.0F, 20.0F, 0.5F));
   private final BooleanSetting smart = this.registerSetting(new BooleanSetting("Smart", true));
   private final BooleanSetting absorption = this.registerSetting(new BooleanSetting("Count Absorption", true));
   private final BooleanSetting returnSlot = this.registerSetting(new BooleanSetting("Return Slot", true));
   private boolean eating;
   private int previousSlot = -1;

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.level != null) {
         float effectiveHealth = mc.player.getHealth() + (this.absorption.isValue() ? mc.player.getAbsorptionAmount() : 0.0F);
         if (effectiveHealth <= this.health.getValue() && this.findApple()) {
            mc.options.keyUse.setDown(true);
            this.eating = true;
         } else if (this.eating && (!mc.player.isUsingItem() || effectiveHealth > this.health.getValue())) {
            this.stopEating();
         }
      }
   }

   private boolean findApple() {
      if (mc.player.getOffhandItem().is(Items.GOLDEN_APPLE)) {
         return true;
      }

      if (!this.smart.isValue()) {
         return false;
      }

      if (mc.player.getMainHandItem().is(Items.GOLDEN_APPLE)) {
         return true;
      }

      for (int slot = 0; slot < 9; slot++) {
         if (mc.player.getInventory().getItem(slot).is(Items.GOLDEN_APPLE)) {
            int selected = mc.player.getInventory().getSelectedSlot();
            if (selected != slot) {
               if (this.previousSlot < 0 && this.returnSlot.isValue()) {
                  this.previousSlot = selected;
               }

               mc.player.getInventory().setSelectedSlot(slot);
            }

            return true;
         }
      }

      return false;
   }

   private void stopEating() {
      mc.options.keyUse.setDown(false);
      this.eating = false;
      if (mc.player != null && this.previousSlot >= 0 && this.returnSlot.isValue()) {
         mc.player.getInventory().setSelectedSlot(this.previousSlot);
      }

      this.previousSlot = -1;
   }

   @Override
   public void onDisable() {
      this.stopEating();
      super.onDisable();
   }
}
