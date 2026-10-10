package havoc.module.impl.visuals;

import havoc.cosmetic.CosmeticRegistry;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

@ModuleInfo(name = "Cosmetics", desc = "Плащи, крылья, рюкзаки, питомцы, шляпы", category = Category.VISUALS, enabledByDefault = true)
public final class Cosmetics extends Module {
   private static final String NONE = "Нет";
   public static final String[] TYPES = {"cape", "wings", "bodywear", "pet", "hat"};
   private final SelectSetting cape = this.registerSetting(creating("cape", "Плащ"));
   private final SelectSetting wings = this.registerSetting(creating("wings", "Крылья"));
   private final SelectSetting bodywear = this.registerSetting(creating("bodywear", "Рюкзак"));
   private final SelectSetting pet = this.registerSetting(creating("pet", "Питомец"));
   private final SelectSetting hat = this.registerSetting(creating("hat", "Головной убор"));
   public final SliderSetting calibX = this.registerSetting(new SliderSetting("Калибровка X", 0.0F, -1.5F, 1.5F, 0.01F));
   public final SliderSetting calibY = this.registerSetting(new SliderSetting("Калибровка Y", 0.0F, -1.5F, 1.5F, 0.01F));
   public final SliderSetting calibZ = this.registerSetting(new SliderSetting("Калибровка Z", 0.0F, -1.5F, 1.5F, 0.01F));
   public final SliderSetting calibYaw = this.registerSetting(new SliderSetting("Калибровка поворот", 0.0F, -180.0F, 180.0F, 5.0F, "°"));
   public final SliderSetting calibScale = this.registerSetting(new SliderSetting("Калибровка масштаб", 1.0F, 0.3F, 2.0F, 0.05F));

   private static SelectSetting creating(String type, String label) {
      List<String> options = new ArrayList<>();
      options.add(NONE);

      for (Integer index : CosmeticRegistry.indicesOfType(type)) {
         options.add(CosmeticRegistry.label(index));
      }

      return new SelectSetting(label, NONE, options);
   }

   public SelectSetting settingFor(String type) {
      switch (type) {
         case "cape":
            return this.cape;
         case "wings":
            return this.wings;
         case "bodywear":
            return this.bodywear;
         case "pet":
            return this.pet;
         default:
            return this.hat;
      }
   }

   public int selectedIndex(String type) {
      String value = this.settingFor(type).getValue();
      if (NONE.equals(value)) {
         return -1;
      } else {
         for (Integer index : CosmeticRegistry.indicesOfType(type)) {
            if (CosmeticRegistry.label(index).equals(value)) {
               return index;
            }
         }

         return -1;
      }
   }

   public void selectIndex(String type, int index) {
      this.settingFor(type).setValue(index < 0 ? NONE : CosmeticRegistry.label(index));
   }

   public List<Integer> selectedGeoIndices() {
      List<Integer> out = new ArrayList<>();

      for (String type : TYPES) {
         if ("cape".equals(type)) {
            continue;
         }

         int index = this.selectedIndex(type);
         if (index >= 0) {
            out.add(index);
         }
      }

      return out;
   }

   public Identifier selectedCapeTexture() {
      int index = this.selectedIndex("cape");
      if (index < 0) {
         return null;
      } else {
         CosmeticRegistry.Entry entry = CosmeticRegistry.get(index);
         return entry == null ? null : entry.texture(net.minecraft.client.Minecraft.getInstance());
      }
   }
}
