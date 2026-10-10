package havoc.module.impl.misc;

import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import java.util.List;

@ModuleInfo(name = "ClientSounds", desc = "Luma module toggle sounds", category = Category.MISC)
public final class ClientSounds extends Module {
   private final SelectSetting soundType = this.registerSetting(new SelectSetting("Тип звука", "New", List.of("New", "Old")));
   private final SliderSetting volume = this.registerSetting(new SliderSetting("Громкость", 1.0F, 0.1F, 2.0F, 0.1F));

   public boolean isNewSound() {
      return this.soundType.getValue().equals("New");
   }

   public float getVolume() {
      return this.volume.getValue();
   }
}
