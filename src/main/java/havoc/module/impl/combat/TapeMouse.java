package havoc.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.mixin.MinecraftClientInvoker;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import java.util.Arrays;

@ModuleInfo(name = "TapeMouse", desc = "Automatically clicks mouse buttons", category = Category.COMBAT)
public final class TapeMouse extends Module {
   private final SelectSetting button = this.registerSetting(new SelectSetting("Button", "Left", Arrays.asList("Left", "Right")));
   private final SliderSetting delay = this.registerSetting(new SliderSetting("Delay", 1.0F, 1.0F, 15.0F, 0.5F));
   private long lastClick;

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.gui.screen() == null) {
         long interval = (long)(this.delay.getValue() * 300.0F);
         if (System.currentTimeMillis() - this.lastClick >= interval) {
            MinecraftClientInvoker invoker = (MinecraftClientInvoker)mc;
            if (this.button.getValue().equals("Left")) {
               invoker.havoc$invokeDoAttack();
            } else {
               invoker.havoc$invokeDoItemUse();
            }

            this.lastClick = System.currentTimeMillis();
         }
      }
   }
}
