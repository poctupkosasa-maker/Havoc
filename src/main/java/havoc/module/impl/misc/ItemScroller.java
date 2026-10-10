package havoc.module.impl.misc;

import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SliderSetting;

@ModuleInfo(name = "ItemScroller", desc = "Compatibility item scroller toggle", category = Category.MISC)
public final class ItemScroller extends Module {
   private final SliderSetting delay = this.registerSetting(new SliderSetting("Задержка прокрутки предметов", 50.0F, 0.0F, 200.0F, 1.0F, "ms"));

   public long getDelayMs() {
      return this.delay.getValue().longValue();
   }
}
