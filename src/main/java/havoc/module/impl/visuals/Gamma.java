package havoc.module.impl.visuals;

import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SliderSetting;
import lombok.Generated;

@ModuleInfo(name = "Gamma", desc = "FullBright for the world", category = Category.VISUALS)
public final class Gamma extends Module {
   private static Gamma instance;
   private final SliderSetting brightness = this.registerSetting(new SliderSetting("Brightness", 16.0F, 1.0F, 32.0F, 1.0F));

   public Gamma() {
      instance = this;
   }

   public double getGammaValue() {
      return this.brightness.getValue().floatValue();
   }

   @Generated
   public static Gamma getInstance() {
      return instance;
   }
}
