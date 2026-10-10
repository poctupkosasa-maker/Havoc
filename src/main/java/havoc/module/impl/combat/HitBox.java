package havoc.module.impl.combat;

import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SliderSetting;

@ModuleInfo(name = "HitBox", desc = "Expands target hitboxes", category = Category.COMBAT)
public final class HitBox extends Module {
   private final SliderSetting horizontal = this.registerSetting(new SliderSetting("Expand XZ", 0.2F, 0.0F, 3.0F, 0.05F));
   private final SliderSetting vertical = this.registerSetting(new SliderSetting("Expand Y", 0.0F, 0.0F, 3.0F, 0.05F));

   public float horizontal() {
      return this.horizontal.getValue();
   }

   public float vertical() {
      return this.vertical.getValue();
   }
}
