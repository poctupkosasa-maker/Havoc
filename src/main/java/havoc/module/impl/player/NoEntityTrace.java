package havoc.module.impl.player;

import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import java.util.Locale;

@ModuleInfo(name = "NoEntityTrace", desc = "Allows interaction through entities", category = Category.PLAYER)
public final class NoEntityTrace extends Module {
   private final BooleanSetting disableWithSword = this.registerSetting(new BooleanSetting("Выключать с мечом", true));

   public boolean shouldIgnoreEntityTrace() {
      if (!this.isEnable() || mc.player == null) {
         return false;
      } else {
         return !this.disableWithSword.isValue() ? true : !mc.player.getMainHandItem().getItem().getDescriptionId().toLowerCase(Locale.ROOT).contains("sword");
      }
   }
}
