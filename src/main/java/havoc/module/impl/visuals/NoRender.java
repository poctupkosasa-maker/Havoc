package havoc.module.impl.visuals;

import havoc.Havoc;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.MultiSelectSetting;
import java.util.LinkedHashSet;
import java.util.List;

@ModuleInfo(name = "NoRender", desc = "Hides selected visual effects", category = Category.VISUALS)
public final class NoRender extends Module {
   private static final String FIRE = "Fire";
   private static final String BAD_EFFECTS = "Bad Effects";
   private static final String BLOCK_OVERLAY = "Block Overlay";
   private static final String DARKNESS = "Darkness";
   private static final String DAMAGE = "Damage";
   private static final String TOTEM_ANIMATION = "Totem Animation";
   private static NoRender instance;
   private final MultiSelectSetting elements = this.registerSetting(
      new MultiSelectSetting(
         "Elements",
         List.of("Fire", "Bad Effects", "Block Overlay", "Darkness", "Damage", "Totem Animation"),
         new LinkedHashSet<>(List.of("Fire", "Bad Effects", "Block Overlay", "Darkness", "Damage", "Totem Animation"))
      )
   );

   public NoRender() {
      instance = this;
   }

   public static NoRender getInstance() {
      return instance;
   }

   public static boolean hideFire() {
      NoRender module = resolve();
      return module != null && module.shouldHideFire();
   }

   public static boolean hideBadEffects() {
      NoRender module = resolve();
      return module != null && module.shouldHideBadEffects();
   }

   public static boolean hideBlockOverlay() {
      NoRender module = resolve();
      return module != null && module.shouldHideBlockOverlay();
   }

   public static boolean hideDarkness() {
      NoRender module = resolve();
      return module != null && module.shouldHideDarkness();
   }

   public static boolean hideDamage() {
      NoRender module = resolve();
      return module != null && module.shouldHideDamage();
   }

   public static boolean hideTotemAnimation() {
      NoRender module = resolve();
      return module != null && module.shouldHideTotemAnimation();
   }

   public boolean shouldHideFire() {
      return this.isEnable() && this.elements.isSelected("Fire");
   }

   public boolean shouldHideBadEffects() {
      return this.isEnable() && this.elements.isSelected("Bad Effects");
   }

   public boolean shouldHideBlockOverlay() {
      return this.isEnable() && this.elements.isSelected("Block Overlay");
   }

   public boolean shouldHideDarkness() {
      return this.isEnable() && this.elements.isSelected("Darkness");
   }

   public boolean shouldHideDamage() {
      return this.isEnable() && this.elements.isSelected("Damage");
   }

   public boolean shouldHideTotemAnimation() {
      return this.isEnable() && this.elements.isSelected("Totem Animation");
   }

   private static NoRender resolve() {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null && havoc.getModuleManager() != null) {
         NoRender module = havoc.getModuleManager().getModule(NoRender.class);
         if (module != null) {
            return module;
         }
      }

      return instance;
   }
}
