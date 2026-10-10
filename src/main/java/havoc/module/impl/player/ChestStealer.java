package havoc.module.impl.player;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SliderSetting;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

@ModuleInfo(name = "ChestStealer", desc = "Luma chest item transfer", category = Category.PLAYER)
public final class ChestStealer extends Module {
   private final SliderSetting delay = this.registerSetting(new SliderSetting("Задержка", 50.0F, 0.0F, 500.0F, 1.0F, "ms"));
   private long last;

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.gameMode != null && mc.player.containerMenu instanceof ChestMenu handler) {
         if (!((float)(System.currentTimeMillis() - this.last) < this.delay.getValue())) {
            for (Slot slot : handler.slots) {
               if (slot.container != mc.player.getInventory() && slot.hasItem()) {
                  mc.gameMode.handleContainerInput(handler.containerId, slot.index, 0, ContainerInput.QUICK_MOVE, mc.player);
                  this.last = System.currentTimeMillis();
                  return;
               }
            }
         }
      }
   }
}
