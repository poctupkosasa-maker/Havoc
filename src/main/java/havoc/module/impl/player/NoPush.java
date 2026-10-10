package havoc.module.impl.player;

import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.MultiSelectSetting;
import java.util.Arrays;
import java.util.LinkedHashSet;

@ModuleInfo(name = "AntiPush", desc = "Cancels selected push types", category = Category.PLAYER)
public final class NoPush extends Module {
   private final MultiSelectSetting types = this.registerSetting(
      new MultiSelectSetting(
         "Игнорировать",
         Arrays.asList("Water", "Blocks", "Entities", "Powder Snow", "Berries"),
         new LinkedHashSet<>(Arrays.asList("Water", "Blocks", "Entities", "Powder Snow", "Berries"))
      )
   );

   public boolean entities() {
      return this.types.isSelected("Entities");
   }
}
