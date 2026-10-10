package havoc.module.impl.visuals;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventAttackEntity;
import havoc.event.impl.EventScreenRender;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.MultiSelectSetting;
import havoc.module.setting.SliderSetting;
import havoc.screen.screen.HudElement;
import havoc.screen.screen.HudElementManager;
import havoc.screen.screen.elements.KeybindHudElement;
import havoc.screen.screen.elements.LogoHudElement;
import havoc.screen.screen.elements.NotificationsHudElement;
import havoc.screen.screen.elements.PotionsHudElement;
import havoc.screen.screen.elements.StaffListHudElement;
import havoc.screen.screen.elements.TargetHudElement;
import havoc.utilites.color.ClientTheme;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.input.KeyEvent;

@ModuleInfo(name = "Interface", desc = "HUD elements and interface rendering", category = Category.VISUALS, enabledByDefault = true)
public class Interface extends Module {
   private static Interface INSTANCE;
   private final MultiSelectSetting enabledElements = this.registerSetting(
      new MultiSelectSetting(
         "Elements",
         List.of("Logo", "Keybind", "StaffList", "Potions", "Notifications", "TargetHud"),
         new LinkedHashSet<>(List.of("Logo", "Keybind", "StaffList", "Potions", "Notifications", "TargetHud"))
      )
   );
   private final BooleanSetting hudShadow = this.registerSetting(new BooleanSetting("HUD Shadow", true));
   private final SliderSetting hudShadowOpacity = this.registerSetting(
      new SliderSetting("HUD Shadow Opacity", 0.65F, 0.0F, 1.0F, 0.05F).visibleWhen(this.hudShadow::isValue)
   );
   private final SliderSetting hudShadowSize = this.registerSetting(
      new SliderSetting("HUD Shadow Size", 7.0F, 2.0F, 16.0F, 0.5F).visibleWhen(this.hudShadow::isValue)
   );
   private final HudElementManager hudElementManager = new HudElementManager();
   private final TargetHudElement targetHudElement;
   private boolean pendingDisable;

   public Interface() {
      INSTANCE = this;
      this.hudElementManager.register(new LogoHudElement());
      this.hudElementManager.register(new KeybindHudElement());
      this.hudElementManager.register(new StaffListHudElement());
      this.hudElementManager.register(new PotionsHudElement());
      this.hudElementManager.register(new NotificationsHudElement());
      this.targetHudElement = new TargetHudElement();
      this.hudElementManager.register(this.targetHudElement);
   }

   public List<HudElement> getHudElements() {
      return this.hudElementManager.getElements();
   }

   public static void registerExternalHudElement(HudElement element) {
      if (INSTANCE != null && element != null) {
         INSTANCE.hudElementManager.register(element);
      }
   }

   public static boolean isHudShadowEnabled() {
      return INSTANCE == null || INSTANCE.hudShadow.isValue();
   }

   public static float getHudShadowOpacity() {
      return INSTANCE == null ? 0.65F : INSTANCE.hudShadowOpacity.getValue();
   }

   public static float getHudShadowSize() {
      return INSTANCE == null ? 7.0F : INSTANCE.hudShadowSize.getValue();
   }

   public boolean isHudElementEnabled(String displayName) {
      return this.enabledElements.getSelectedValues().contains(displayName);
   }

   public boolean handleHudEditorKeyPressed(KeyEvent key) {
      return this.hudElementManager.handleKeyPressed(key);
   }

   public boolean handleHudEditorCharTyped(int codePoint) {
      return this.hudElementManager.handleCharTyped(codePoint);
   }

   @Override
   public void onEnable() {
      this.pendingDisable = false;
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.pendingDisable = true;
   }

   @Subscribe
   public void onRenderHud(EventScreenRender event) {
      if (event.getStage() == EventScreenRender.Stage.HUD) {
         ClientTheme.updateDynamicColors();
         Set<String> visible = this.pendingDisable ? Set.of() : this.enabledElements.getSelectedValues();
         this.hudElementManager.render(event.getContext(), event.getTickCounter().getGameTimeDeltaPartialTick(false), visible, true);
         if (this.pendingDisable && !this.hudElementManager.hasRenderableElements()) {
            this.pendingDisable = false;
            super.onDisable();
         }
      }
   }

   @Subscribe
   public void onAttackEntity(EventAttackEntity event) {
      this.targetHudElement.onAttack(event.getTarget());
   }
}
