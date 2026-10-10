package havoc.module.impl.player;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

@ModuleInfo(name = "AutoArmor", desc = "Equips better armor", category = Category.PLAYER)
public final class AutoArmor extends Module {
   private int delay;

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (this.delay > 0) {
         this.delay--;
      } else if (mc.player != null && mc.gameMode != null && mc.player.containerMenu != null) {
         for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            int best = this.findBestArmor(slot);
            if (best >= 0) {
               int armorButton = 8 - slot.getIndex();
               mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, best, armorButton, ContainerInput.SWAP, mc.player);
               this.delay = 3;
               return;
            }
         }
      }
   }

   private int findBestArmor(EquipmentSlot slot) {
      ItemStack equipped = mc.player.getItemBySlot(slot);
      int equippedProtection = this.armorProtection(equipped, slot);
      int bestSlot = -1;
      int bestProtection = equippedProtection;

      for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         int protection = this.armorProtection(stack, slot);
         if (protection > bestProtection) {
            bestProtection = protection;
            bestSlot = i < 9 ? i + 36 : i;
         }
      }

      return bestSlot;
   }

   private int armorProtection(ItemStack stack, EquipmentSlot slot) {
      if (stack != null && !stack.isEmpty()) {
         Equippable equippable = (Equippable)stack.get(DataComponents.EQUIPPABLE);
         if (equippable != null && equippable.slot() == slot) {
            String item = stack.getItem().toString().toLowerCase();
            if (item.contains("netherite")) {
               return 6;
            } else if (item.contains("diamond")) {
               return 5;
            } else if (item.contains("iron")) {
               return 4;
            } else if (item.contains("chainmail")) {
               return 3;
            } else if (item.contains("gold")) {
               return 2;
            } else if (item.contains("leather")) {
               return 1;
            } else {
               return item.contains("turtle") ? 2 : 0;
            }
         } else {
            return -1;
         }
      } else {
         return -1;
      }
   }
}
