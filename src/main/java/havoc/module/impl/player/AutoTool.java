package havoc.module.impl.player;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

@ModuleInfo(name = "AutoTool", desc = "Selects best mining tool", category = Category.PLAYER)
public final class AutoTool extends Module {
   private final BooleanSetting visual = this.registerSetting(new BooleanSetting("Визуально", true));
   private int oldSlot = -1;
   private boolean swapped;

   @Override
   public void onDisable() {
      this.restore();
      super.onDisable();
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.level != null) {
         if (mc.options.keyAttack.isDown() && mc.hitResult instanceof BlockHitResult hit) {
            BlockState state = mc.level.getBlockState(hit.getBlockPos());
            int best = this.findBestSlot(state);
            if (best >= 0 && best != mc.player.getInventory().getSelectedSlot()) {
               if (!this.swapped) {
                  this.oldSlot = mc.player.getInventory().getSelectedSlot();
               }

               mc.player.getInventory().setSelectedSlot(best);
               this.swapped = true;
            }
         } else {
            this.restore();
         }
      } else {
         this.restore();
      }
   }

   private void restore() {
      if (mc.player != null && this.swapped && this.oldSlot >= 0 && this.oldSlot < 9) {
         mc.player.getInventory().setSelectedSlot(this.oldSlot);
      }

      this.oldSlot = -1;
      this.swapped = false;
   }

   private int findBestSlot(BlockState state) {
      int bestSlot = -1;
      float bestSpeed = 1.0F;

      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = mc.player.getInventory().getItem(slot);
         float speed = stack.getDestroySpeed(state);
         if (!stack.isEmpty() && speed > bestSpeed) {
            bestSpeed = speed;
            bestSlot = slot;
         }
      }

      return bestSlot;
   }
}
