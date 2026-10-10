package havoc.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.SelectSetting;
import havoc.utilites.InventoryUtil;
import java.util.List;
import net.minecraft.world.item.Items;

@ModuleInfo(name = "WindJump", desc = "Throws a wind charge below the player", category = Category.MISC)
public final class WindJump extends Module {
   private final SelectSetting mode = this.registerSetting(new SelectSetting("Режим", "Legit", List.of("Instant", "Legit")));
   private final BooleanSetting trigger = this.registerSetting(new BooleanSetting("Кнопка", false));
   private int cooldown;

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (this.cooldown > 0) {
         this.cooldown--;
      }

      if (this.trigger.isValue() && this.cooldown <= 0 && mc.player != null && mc.gameMode != null) {
         this.trigger.setValue(false);
         if (InventoryUtil.useFromInventory(Items.WIND_CHARGE, 90.0F)) {
            this.cooldown = this.mode.getValue().equals("Legit") ? 6 : 2;
         }
      }
   }
}
