package havoc.module.impl.visuals;

import com.google.common.eventbus.Subscribe;
import havoc.Havoc;
import havoc.event.impl.EventScreenRender;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.MultiSelectSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.Setting;
import havoc.module.setting.SliderSetting;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.screen.screen.AbstractHudElement;
import havoc.screen.screen.HudElementManager;
import havoc.screen.screen.HudVisualStyle;
import havoc.utilites.animation.AnimatedValue;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ClientTheme;
import havoc.utilites.color.ColorUtil;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@ModuleInfo(name = "ArrayList", desc = "Shows enabled modules in the Opal module-list style", category = Category.VISUALS)
public final class ArrayListModule extends Module {
   private static final float ROW_HEIGHT = 12.0F;
   private static final float FONT_SIZE = 8.0F;
   private static final float HORIZONTAL_PADDING = 6.5F;
   private final SliderSetting scale = this.registerSetting(new SliderSetting("Scale", 1.0F, 0.5F, 1.5F, 0.05F));
   private final BooleanSetting lowercase = this.registerSetting(new BooleanSetting("Lowercase", true));
   private final BooleanSetting showSuffix = this.registerSetting(new BooleanSetting("Show suffix", true));
   private final BooleanSetting offsetScoreboard = this.registerSetting(new BooleanSetting("Offset scoreboard", true));
   private final BooleanSetting bar = this.registerSetting(new BooleanSetting("Bar", true));
   private final BooleanSetting background = this.registerSetting(new BooleanSetting("Background", false));
   private final BooleanSetting shadow = this.registerSetting(new BooleanSetting("Shadow", true));
   private final SliderSetting shadowOpacity = this.registerSetting(
      new SliderSetting("Shadow Opacity", 0.55F, 0.0F, 1.0F, 0.05F).visibleWhen(this.shadow::isValue)
   );
   private final SliderSetting shadowSize = this.registerSetting(new SliderSetting("Shadow Size", 6.0F, 2.0F, 14.0F, 0.5F).visibleWhen(this.shadow::isValue));
   private final MultiSelectSetting visibleCategories = this.registerSetting(
      new MultiSelectSetting(
         "Visible categories",
         List.of("Combat", "Movement", "Visuals", "Player", "Misc"),
         new LinkedHashSet<>(List.of("Combat", "Movement", "Visuals", "Player", "Misc"))
      )
   );
   private final Map<Module, ArrayListModule.Entry> entries = new IdentityHashMap<>();
   private final HudElementManager hudElementManager = new HudElementManager();
   private final ArrayListModule.ArrayListHudElement hudElement = new ArrayListModule.ArrayListHudElement();
   private final AnimatedValue backgroundHeight = new AnimatedValue();
   private float renderedHeight;

   public ArrayListModule() {
      this.hudElementManager.register(this.hudElement);
      Interface.registerExternalHudElement(this.hudElement);
   }

   @Subscribe
   public void onRenderHud(EventScreenRender event) {
      if (event.getStage() == EventScreenRender.Stage.HUD) {
         ClientTheme.updateDynamicColors();
         this.hudElementManager.render(event.getContext(), event.getTickCounter().getGameTimeDeltaPartialTick(false), Set.of("ArrayList"), true);
      }
   }

   private void renderArrayList(GuiGraphicsExtractor context, int screenWidth, int screenHeight, float originX, float originY, long nowNanos) {
      Minecraft mc = Minecraft.getInstance();
      Havoc havoc = Havoc.getInstance();
      if (mc != null && mc.getWindow() != null && havoc != null && havoc.getModuleManager() != null) {
         for (Module module : havoc.getModuleManager().getModules()) {
            this.entries.computeIfAbsent(module, x$0 -> new ArrayListModule.Entry(x$0));
         }

         float uiScale = this.scale.getValue();
         long nowMs = System.currentTimeMillis();
         List<ArrayListModule.Entry> sorted = this.entries
            .values()
            .stream()
            .peek(entryx -> entryx.updateText(this.showSuffix.isValue(), this.lowercase.isValue()))
            .sorted(Comparator.comparingDouble(ArrayListModule.Entry::width).reversed())
            .toList();
         int visibleIndex = 0;

         for (ArrayListModule.Entry entry : sorted) {
            boolean enabled = this.isEntryEnabled(entry.module);
            if (enabled) {
               entry.targetY = visibleIndex * 12.0F;
               visibleIndex++;
            }

            entry.enabled = enabled;
         }

         float widestEntry = 0.0F;

         for (ArrayListModule.Entry entry : sorted) {
            if (entry.enabled || entry.visibility.getValue() > 0.01F) {
               widestEntry = Math.max(widestEntry, entry.width);
            }
         }

         float elementWidth = Math.max(36.0F, (widestEntry + 6.5F) * uiScale);
         float elementRight = originX + elementWidth;
         boolean leftSide = originX + elementWidth * 0.5F < screenWidth * 0.5F;
         float panelHeight = this.backgroundHeight.update(Math.max(8.0F, visibleIndex * 12.0F * uiScale), 0.1F, nowNanos);
         if (this.bar.isValue() && (visibleIndex > 0 || panelHeight > 8.1F)) {
            float railWidth = Math.max(2.0F, 2.5F * uiScale);
            float railX = leftSide ? originX - 0.75F * uiScale : elementRight - railWidth + 0.75F * uiScale;
            float railHeight = Math.max(2.0F, panelHeight);
            float railRadius = railWidth * 0.5F;
            int railColor = this.animatedThemeColor(nowMs, 0);
            RenderUtil.drawGlow(context, railX, originY, railWidth, railHeight, railRadius, ColorUtil.multiplyAlpha(railColor, 0.58F), 1.0F, 2.5F * uiScale);
            RenderUtil.drawRoundRect(context, railX, originY, railWidth, railHeight, railRadius, ColorUtil.withAlpha(railColor, 255));
         }

         float animatedRows = 0.0F;
         int colorIndex = 0;

         for (ArrayListModule.Entry entry : sorted) {
            float visibility = ColorUtil.clamp01(entry.visibility.update(entry.enabled ? 1.0F : 0.0F, 0.07F, nowNanos));
            if (!(visibility <= 0.01F)) {
               float rowY = entry.y.update(entry.targetY, 0.1F, nowNanos);
               this.renderEntry(context, entry, colorIndex, originX, elementRight, originY + rowY * uiScale, uiScale, visibility, nowMs, leftSide);
               animatedRows += visibility;
               if (entry.enabled) {
                  colorIndex++;
               }
            }
         }

         this.renderedHeight = animatedRows * 12.0F * uiScale;
         this.hudElement.updateBounds(elementWidth, Math.max(8.0F, Math.max(this.renderedHeight, panelHeight)));
      }
   }

   private boolean isEntryEnabled(Module module) {
      return module != null && module.isEnable() ? this.visibleCategories.isSelected(module.getCategory().getName()) : false;
   }

   private void renderEntry(
      GuiGraphicsExtractor context,
      ArrayListModule.Entry entry,
      int index,
      float elementLeft,
      float elementRight,
      float posY,
      float uiScale,
      float visibility,
      long nowMs,
      boolean leftSide
   ) {
      float width = entry.width;
      float easedVisibility = this.easeOutExpo(visibility);
      float slide = 8.0F - (width + 8.0F) * easedVisibility;
      float posX = leftSide ? elementLeft + 6.5F * uiScale - (width + slide) * uiScale : elementRight + slide * uiScale;
      int color = this.animatedThemeColor(nowMs, index);
      int mainColor = ColorUtil.multiplyAlpha(color, visibility);
      float textOffset = 3.5F;
      float textX = posX - textOffset * uiScale + 0.75F * uiScale;
      float textY = posY + 3.4F * uiScale;
      float fontSize = 8.0F * uiScale;
      float renderedTextWidth = RenderUtil.getTextWidth(entry.name + entry.suffix, GFonts.MEDIUM, fontSize);
      if (this.background.isValue()) {
         float backgroundPadding = 2.0F * uiScale;
         float seamOverlap = Math.max(0.6F, uiScale);
         int backgroundColor = ClientColors.HUD_BACKGROUND;
         if (this.shadow.isValue()) {
            RenderUtil.drawGlow(
               context,
               textX - backgroundPadding + 0.5F,
               posY + 0.5F,
               renderedTextWidth + backgroundPadding * 2.0F - 1.0F,
               12.0F * uiScale,
               0.0F,
               ColorUtil.multiplyAlpha(ClientColors.HUD_SHADOW, visibility * this.shadowOpacity.getValue()),
               1.0F,
               this.shadowSize.getValue()
            );
         }

         RenderUtil.drawRoundRect(
            context,
            textX - backgroundPadding,
            posY - seamOverlap * 0.5F,
            renderedTextWidth + backgroundPadding * 2.0F,
            12.0F * uiScale + seamOverlap,
            0.0F,
            ColorUtil.multiplyAlpha(backgroundColor, visibility)
         );
      }

      int textShadow = ColorUtil.multiplyAlpha(this.shadowColor(color), visibility);
      RenderUtil.drawText(context, entry.name, textX + 0.5F * uiScale, textY + 0.5F * uiScale, textShadow, GFonts.MEDIUM, fontSize);
      RenderUtil.drawText(context, entry.name, textX, textY, mainColor, GFonts.MEDIUM, fontSize);
      if (!entry.suffix.isEmpty()) {
         float suffixX = textX + RenderUtil.getTextWidth(entry.name, GFonts.MEDIUM, fontSize);
         int suffixBaseColor = HudVisualStyle.SECONDARY_TEXT;
         int suffixColor = ColorUtil.multiplyAlpha(suffixBaseColor, visibility);
         RenderUtil.drawText(
            context,
            entry.suffix,
            suffixX + 0.5F * uiScale,
            textY + 0.5F * uiScale,
            ColorUtil.multiplyAlpha(this.shadowColor(suffixBaseColor), visibility),
            GFonts.MEDIUM,
            fontSize
         );
         RenderUtil.drawText(context, entry.suffix, suffixX, textY, suffixColor, GFonts.MEDIUM, fontSize);
      }
   }

   private int animatedThemeColor(long nowMs, int index) {
      int angle = Math.floorMod((int)(nowMs / 6L - index * 20L), 360);
      angle = angle >= 180 ? 360 - angle : angle;
      return ColorUtil.interpolate(ClientColors.CATEGORY_ACCENT, ClientColors.CATEGORY_ACCENT_SECOND, angle / 180.0F);
   }

   private int shadowColor(int color) {
      return ColorUtil.rgba(ColorUtil.red(color) / 4, ColorUtil.green(color) / 4, ColorUtil.blue(color) / 4, ColorUtil.alpha(color));
   }

   private String resolveSuffix(Module module) {
      if (module == this) {
         return null;
      }

      for (Setting setting : module.getSettings()) {
         if (setting instanceof SelectSetting selectSetting) {
            return selectSetting.getValue();
         }
      }

      return null;
   }

   public boolean shouldOffsetScoreboard() {
      Minecraft mc = Minecraft.getInstance();
      return this.isEnable() && this.offsetScoreboard.isValue() && mc != null && mc.getWindow() != null
         ? this.hudElement.getX() + this.hudElement.getWidth() * 0.5F >= mc.getWindow().getGuiScaledWidth() * 0.5F
         : false;
   }

   public float getRenderedHeight() {
      return this.renderedHeight;
   }

   public float getRenderedTop() {
      return this.hudElement.getY();
   }

   public float getRenderedBottom() {
      return this.hudElement.getY() + this.renderedHeight;
   }

   private float easeOutExpo(float value) {
      float clamped = ColorUtil.clamp01(value);
      return clamped >= 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, -10.0F * clamped);
   }

   private final class ArrayListHudElement extends AbstractHudElement {
      private final AnimatedValue renderX = new AnimatedValue();
      private final AnimatedValue renderY = new AnimatedValue();
      private boolean positionInitialized;

      private ArrayListHudElement() {
         super("array_list", "ArrayList", 2.0F, 2.0F, 86.0F, 20.0F);
      }

      @Override
      public void setPosition(float x, float y) {
         super.setPosition(x, y);
         this.positionInitialized = true;
      }

      @Override
      public void render(GuiGraphicsExtractor context, int screenWidth, int screenHeight, float tickDelta) {
         long nowNanos = System.nanoTime();
         if (!this.positionInitialized) {
            float initialX = Math.max(2.0F, screenWidth - this.getWidth() - 2.0F);
            super.setPosition(initialX, 2.0F);
            this.renderX.setValue(initialX);
            this.renderY.setValue(2.0F);
            this.positionInitialized = true;
         }

         float animatedX = this.renderX.update(this.getX(), 0.075F, nowNanos);
         float animatedY = this.renderY.update(this.getY(), 0.075F, nowNanos);
         ArrayListModule.this.renderArrayList(context, screenWidth, screenHeight, animatedX, animatedY, nowNanos);
      }

      private void updateBounds(float width, float height) {
         this.setBounds(width, height);
      }
   }

   private final class Entry {
      private final Module module;
      private final AnimatedValue visibility = new AnimatedValue();
      private final AnimatedValue y = new AnimatedValue();
      private String name = "";
      private String suffix = "";
      private float width;
      private float targetY;
      private boolean enabled;

      private Entry(Module module) {
         this.module = module;
         this.visibility.setValue(0.0F);
      }

      private void updateText(boolean includeSuffix, boolean forceLowercase) {
         String moduleName = this.module.getName();
         String moduleSuffix = includeSuffix ? ArrayListModule.this.resolveSuffix(this.module) : null;
         if (forceLowercase) {
            moduleName = moduleName.toLowerCase(Locale.ROOT);
            if (moduleSuffix != null) {
               moduleSuffix = moduleSuffix.toLowerCase(Locale.ROOT);
            }
         }

         this.name = moduleName;
         this.suffix = moduleSuffix != null && !moduleSuffix.isBlank() ? " " + moduleSuffix : "";
         this.width = RenderUtil.getTextWidth(this.name + this.suffix, GFonts.MEDIUM, 8.0F);
      }

      private float width() {
         return this.width;
      }
   }
}
