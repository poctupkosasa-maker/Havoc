package havoc.module.impl.visuals;

import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.impl.misc.AntiCapture;
import havoc.module.setting.SelectSetting;
import havoc.screen.menu.MenuScreen;
import java.util.List;

@ModuleInfo(name = "Menu", desc = "Opens the client menu", category = Category.VISUALS)
public class Menu extends Module {
   private final SelectSetting interfaceScale = this.registerSetting(new SelectSetting("Размер интерфейса", "100", List.of("50", "75", "100", "125")));

   public Menu() {
      this.setKey(344);
   }

   public float getInterfaceScale() {
      try {
         return Integer.parseInt(this.interfaceScale.getValue()) / 100.0F;
      } catch (NumberFormatException ignored) {
         return 1.0F;
      }
   }

   @Override
   public void toggle() {
      if (!(mc.gui.screen() instanceof MenuScreen) && AntiCapture.isInputLocked()) {
         return;
      }
      if (mc.gui.screen() instanceof MenuScreen menuScreen) {
         menuScreen.requestClose();
      } else {
         mc.gui.setScreen(new MenuScreen(mc.gui.screen()));
         this.setEnable(true);
      }
   }
}
