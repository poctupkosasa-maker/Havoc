package havoc.module.impl.visuals;

import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SliderSetting;
import lombok.Generated;

@ModuleInfo(name = "ViewModel", desc = "Adjust first-person hand offsets", category = Category.VISUALS)
public final class ViewModel extends Module {
   private static ViewModel instance;
   private final SliderSetting mainHandX = this.registerSetting(new SliderSetting("Main Hand X", 0.0F, -1.0F, 1.0F, 0.01F));
   private final SliderSetting mainHandY = this.registerSetting(new SliderSetting("Main Hand Y", 0.0F, -1.0F, 1.0F, 0.01F));
   private final SliderSetting mainHandZ = this.registerSetting(new SliderSetting("Main Hand Z", 0.0F, -2.5F, 2.5F, 0.01F));
   private final SliderSetting offHandX = this.registerSetting(new SliderSetting("Off Hand X", 0.0F, -1.0F, 1.0F, 0.01F));
   private final SliderSetting offHandY = this.registerSetting(new SliderSetting("Off Hand Y", 0.0F, -1.0F, 1.0F, 0.01F));
   private final SliderSetting offHandZ = this.registerSetting(new SliderSetting("Off Hand Z", 0.0F, -2.5F, 2.5F, 0.01F));

   public ViewModel() {
      instance = this;
   }

   public static ViewModel getInstance() {
      return instance;
   }

   @Generated
   public SliderSetting getMainHandX() {
      return this.mainHandX;
   }

   @Generated
   public SliderSetting getMainHandY() {
      return this.mainHandY;
   }

   @Generated
   public SliderSetting getMainHandZ() {
      return this.mainHandZ;
   }

   @Generated
   public SliderSetting getOffHandX() {
      return this.offHandX;
   }

   @Generated
   public SliderSetting getOffHandY() {
      return this.offHandY;
   }

   @Generated
   public SliderSetting getOffHandZ() {
      return this.offHandZ;
   }
}
