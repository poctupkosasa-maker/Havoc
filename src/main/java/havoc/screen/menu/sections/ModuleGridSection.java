package havoc.screen.menu.sections;

import havoc.Havoc;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.bind.BindData;
import havoc.module.bind.BindMode;
import havoc.module.impl.visuals.ItemReplacer;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.ColorSetting;
import havoc.module.setting.MultiSelectSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.Setting;
import havoc.module.setting.SliderSetting;
import havoc.render.RenderUtil;
import havoc.render.builders.states.QuadRadiusState;
import havoc.render.font.GFonts;
import havoc.render.font.GIcons;
import havoc.screen.menu.UiAnimationEngine;
import havoc.screen.menu.sections.settings.BooleanSettingRenderer;
import havoc.screen.menu.sections.settings.ColorSettingRenderer;
import havoc.screen.menu.sections.settings.MultiSelectSettingRenderer;
import havoc.screen.menu.sections.settings.SelectSettingRenderer;
import havoc.screen.menu.sections.settings.SliderSettingRenderer;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ClientTheme;
import havoc.utilites.color.ColorUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

public final class ModuleGridSection {
   private static final int COLUMNS = 2;
   private static final float CONTENT_PAD_X = 10.0F;
   private static final float CONTENT_TOP = 14.0F;
   private static final float CONTENT_BOTTOM_PAD = 8.0F;
   private static final float SCROLL_SPEED = 26.0F;
   private static final float SCROLL_STEP = 22.0F;
   private static final float SCROLLBAR_WIDTH = 2.0F;
   private static final float SCROLLBAR_MIN_THUMB_H = 18.0F;
   private static final float SCROLLBAR_SIDE_PAD = 4.0F;
   private static final float CARD_BASE_HEIGHT = 25.0F;
   private static final float CARD_GAP_X = 8.0F;
   private static final float CARD_GAP_Y = 8.0F;
   private static final float MODULE_SETTING_BOTTOM_PAD = 4.0F;
   private static final float MODULE_ICON_SIZE = 8.0F;
   private static final float MODULE_ICON_X_PAD = 8.0F;
   private static final float MODULE_TEXT_X_GAP = 5.0F;
   private static final float MODULE_TEXT_GAP_TO_BIND = 6.0F;
   private static final float MODULE_TEXT_FADE_WIDTH = 22.0F;
   private static final float MODULE_TITLE_FADE_WIDTH = 9.0F;
   private static final float MODULE_TEXT_MIN_FINAL_ALPHA = 0.004F;
   private static final float MODULE_TITLE_SCROLL_SPEED = 4.8F;
   private static final float MODULE_TITLE_TO_SETTING_GAP_EXTRA = 4.0F;
   private static final float MODULE_SETTINGS_GAP = 2.0F;
   private static final float MODULE_SETTINGS_SELECT_TO_BOOLEAN_GAP = 5.0F;
   private static final float TOGGLE_WIDTH = 15.0F;
   private static final float TOGGLE_HEIGHT = 10.0F;
   private static final float TOGGLE_KNOB_SIZE = 7.0F;
   private static final float TOGGLE_INNER_PAD = 1.0F;
   private static final float BIND_HEIGHT = 8.5F;
   private static final float BIND_GAP_FROM_TOGGLE = 4.0F;
   private static final float BIND_ICON_SIZE = 6.5F;
   private static final float BIND_PAD_X = 3.0F;
   private static final float BIND_ICON_GAP = 2.0F;
   private static final int BIND_TEXT_SIZE = 5;
   private static final float BIND_HOVER_SPEED = 12.0F;
   private static final float BIND_HOVER_BG_MIX = 0.22F;
   private static final float BIND_HOVER_ICON_MIX = 0.55F;
   private static final float BIND_HOVER_TEXT_MIX = 0.6F;
   private static final float BIND_WIDTH_ANIM_SPEED = 20.0F;
   private static final float BIND_TEXT_REVEAL_SPEED = 16.0F;
   private static final float BIND_TEXT_REVEAL_EPS = 0.12F;
   private static final float BIND_WIDTH_SETTLE_EPS = 0.06F;
   private static final float SELECT_EXPAND_SPEED = 18.0F;
   private static final float MULTI_SELECT_EXPAND_SPEED = 18.0F;
   private static final float POPUP_WIDTH = 156.0F;
   private static final float POPUP_HEIGHT = 67.0F;
   private static final float POPUP_PAD = 8.0F;
   private static final float MODE_BUTTON_W = 34.0F;
   private static final float MODE_BUTTON_H = 14.0F;
   private static final float POPUP_TITLE_Y = 8.5F;
   private static final float POPUP_BADGE_Y = 7.0F;
   private static final float POPUP_MODE_Y = 32.0F;
   private static final float POPUP_KEY_Y = 52.0F;
   private static final float COLOR_POPUP_WIDTH = 170.0F;
   private static final float COLOR_POPUP_HEIGHT = 194.0F;
   private static final float COLOR_POPUP_PAD = 8.0F;
   private static final float COLOR_PICKER_SIZE = 110.0F;
   private static final float COLOR_BAR_W = 8.0F;
   private static final float COLOR_BAR_GAP = 6.0F;
   private static final float COLOR_PRESET_SIZE = 7.0F;
   private static final float COLOR_PRESET_GAP = 4.0F;
   private static final float COLOR_CLOSE_H = 16.0F;
   private static final float COLOR_INFO_H = 14.0F;
   private static final float COLOR_ACTION_ICON_SIZE = 7.0F;
   private static final int[] COLOR_PRESETS = new int[]{
      ColorUtil.rgb(255, 77, 77),
      ColorUtil.rgb(85, 214, 109),
      ColorUtil.rgb(244, 108, 196),
      ClientColors.CATEGORY_ACCENT,
      ColorUtil.rgb(255, 183, 77),
      ColorUtil.rgb(97, 189, 255),
      ColorUtil.rgb(186, 120, 255),
      ColorUtil.rgb(255, 92, 140),
      ColorUtil.rgb(117, 230, 201),
      ColorUtil.rgb(238, 238, 238),
      ColorUtil.rgb(255, 140, 56),
      ColorUtil.rgb(255, 232, 102)
   };
   private CategoryBarSection.TopTab displayTab;
   private CategoryBarSection.TopTab previousTab;
   private int switchEpoch;
   private Module bindPopupModule;
   private Setting bindPopupSetting;
   private boolean bindPopupOpen;
   private boolean bindCapture;
   private Module draggingSliderModule;
   private SliderSetting draggingSliderSetting;
   private Module selectedSliderModule;
   private SliderSetting selectedSliderSetting;
   private float draggingSliderX;
   private float draggingSliderY;
   private float draggingSliderW;
   private float bindPopupAnchorX;
   private float bindPopupAnchorY;
   private Module colorPopupModule;
   private ColorSetting colorPopupSetting;
   private String colorPopupTitleOverride;
   private boolean colorPopupOpen;
   private float colorPopupAnchorX;
   private float colorPopupAnchorY;
   private ModuleGridSection.ColorDragMode colorDragMode = ModuleGridSection.ColorDragMode.NONE;
   private final LinkedHashSet<Integer> capturePressedCodes = new LinkedHashSet<>();
   private final LinkedHashSet<Integer> captureSelectedCodes = new LinkedHashSet<>();
   private final Map<String, Float> moduleBindWidths = new HashMap<>();
   private final Map<String, Float> popupKeyWidths = new HashMap<>();
   private final Map<String, String> moduleBindTexts = new HashMap<>();
   private final Map<String, Boolean> moduleBindTextUnlocked = new HashMap<>();
   private final Map<String, String> popupKeyTexts = new HashMap<>();
   private final Map<String, Boolean> popupKeyTextUnlocked = new HashMap<>();
   private final Map<String, Float> moduleTitleScrollValues = new HashMap<>();
   private final Map<String, Boolean> moduleTitleScrollForward = new HashMap<>();
   private final Map<CategoryBarSection.TopTab, Float> tabScrollTargets = new HashMap<>();
   private final Map<CategoryBarSection.TopTab, Float> tabScrollValues = new HashMap<>();
   private final Map<CategoryBarSection.TopTab, Float> tabMaxScroll = new HashMap<>();
   private final Map<CategoryBarSection.TopTab, ModuleGridSection.ScrollbarState> tabScrollbars = new HashMap<>();
   private boolean scrollbarDragging;
   private float scrollbarDragOffsetY;
   private final Map<String, Boolean> selectExpanded = new HashMap<>();
   private final Map<String, Float> selectExpandProgress = new HashMap<>();
   private final Map<String, Boolean> multiSelectExpanded = new HashMap<>();
   private final Map<String, Float> multiSelectExpandProgress = new HashMap<>();

   public void render(
      GuiGraphicsExtractor context,
      UiAnimationEngine animation,
      float panelX,
      float panelY,
      float panelWidth,
      float panelHeight,
      int mouseX,
      int mouseY,
      float alpha,
      CategoryBarSection.TopTab activeTab
   ) {
      if (this.displayTab == null) {
         this.displayTab = activeTab;
         this.previousTab = null;
      }

      if (activeTab != this.displayTab) {
         this.previousTab = this.displayTab;
         this.displayTab = activeTab;
         this.switchEpoch++;
         this.scrollbarDragging = false;
      }

      this.updateScrollbarDrag(activeTab, mouseY);
      float switchAnim = this.previousTab == null ? 1.0F : animation.animate01("modules:switch:" + this.switchEpoch, 1.0F, 18.0F, 0.0F);
      float oldAlpha = alpha * (1.0F - switchAnim);
      if (oldAlpha > 0.01F && this.previousTab != null) {
         this.renderModulesForTab(context, animation, panelX, panelY, panelWidth, panelHeight, mouseX, mouseY, oldAlpha, this.previousTab, false);
      }

      float newAlpha = alpha * switchAnim;
      if (newAlpha > 0.01F) {
         this.renderModulesForTab(context, animation, panelX, panelY, panelWidth, panelHeight, mouseX, mouseY, newAlpha, this.displayTab, true);
      }

      if (switchAnim >= 0.999F) {
         this.previousTab = null;
      }

      this.updateSliderDrag(mouseX);
      this.renderBindPopup(context, animation, panelX, panelY, panelWidth, panelHeight, mouseX, mouseY, alpha);
      this.updateColorDrag(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight);
   }

   public CategoryBarSection.TopTab focusModule(Module target, float panelWidth, float panelHeight) {
      if (target == null) {
         return null;
      }

      CategoryBarSection.TopTab tab = this.toTab(target.getCategory());
      if (tab == null) {
         return null;
      }

      List<Module> modules = Havoc.getInstance().getModuleManager().getModules(target.getCategory());
      int index = modules.indexOf(target);
      if (index < 0) {
         return tab;
      }

      float contentW = panelWidth - 20.0F;
      float cardWidth = (contentW - 8.0F) / 2.0F;
      float viewportHeight = Math.max(1.0F, panelHeight - 14.0F - 8.0F);
      float[] columnBottomY = new float[2];
      float moduleY = 0.0F;

      for (int i = 0; i <= index; i++) {
         int col = i % 2;
         Module module = modules.get(i);
         float y = columnBottomY[col];
         if (i == index) {
            moduleY = y;
            break;
         }

         columnBottomY[col] = y + this.getModuleCardHeight(module, cardWidth, null) + 8.0F;
      }

      float maxScroll = this.computeMaxScroll(modules, cardWidth, viewportHeight, null);
      float targetScroll = Math.max(0.0F, moduleY - viewportHeight * 0.32F);
      this.tabScrollTargets.put(tab, this.clamp(targetScroll, 0.0F, maxScroll));
      this.displayTab = tab;
      this.previousTab = null;
      this.scrollbarDragging = false;
      return tab;
   }

   private void renderModulesForTab(
      GuiGraphicsExtractor context,
      UiAnimationEngine animation,
      float panelX,
      float panelY,
      float panelWidth,
      float panelHeight,
      int mouseX,
      int mouseY,
      float alpha,
      CategoryBarSection.TopTab tab,
      boolean animateSlotState
   ) {
      Category category = this.toCategory(tab);
      if (category != null) {
         List<Module> modules = this.visibleModules(category);
         if (!modules.isEmpty()) {
            float contentX = panelX + 10.0F;
            float contentY = panelY + 14.0F;
            float contentW = panelWidth - 20.0F;
            float cardWidth = (contentW - 8.0F) / 2.0F;
            float viewportTop = contentY;
            float viewportBottom = panelY + panelHeight - 8.0F;
            float viewportHeight = Math.max(1.0F, viewportBottom - viewportTop);
            float maxScroll = this.computeMaxScroll(modules, cardWidth, viewportHeight, animation);
            float scrollTarget = this.clamp(this.tabScrollTargets.getOrDefault(tab, 0.0F), 0.0F, maxScroll);
            this.tabScrollTargets.put(tab, scrollTarget);
            this.tabMaxScroll.put(tab, maxScroll);
            String scrollKey = "modules:scroll:" + tab.name();
            float scrollValue = animation.animate(scrollKey, scrollTarget, 26.0F, scrollTarget);
            if (Math.abs(scrollValue - scrollTarget) <= 0.01F) {
               scrollValue = scrollTarget;
               animation.snap(scrollKey, scrollTarget);
            }

            this.tabScrollValues.put(tab, scrollValue);
            int scX1 = (int)Math.floor(contentX);
            int scY1 = (int)Math.floor(viewportTop);
            int scX2 = (int)Math.ceil(contentX + contentW);
            int scY2 = (int)Math.ceil(viewportBottom);
            context.enableScissor(scX1, scY1, scX2, scY2);
            float[] columnBottomY = new float[2];

            for (int c = 0; c < 2; c++) {
               columnBottomY[c] = contentY;
            }

            for (int i = 0; i < modules.size(); i++) {
               int col = i % 2;
               float x = contentX + col * (cardWidth + 8.0F);
               float y = columnBottomY[col];
               Module module = modules.get(i);
               float moduleCardHeight = this.getModuleCardHeight(module, cardWidth, animation);
               float drawY = y - scrollValue;
               if (!(drawY >= viewportBottom) && !(drawY + moduleCardHeight <= viewportTop)) {
                  int cardColor = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND, alpha * 0.95F);
                  int borderColor = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND_OUTLINE, alpha);
                  int textColor = ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha);
                  RenderUtil.drawRoundRect(context, x, drawY, cardWidth, moduleCardHeight, 5.0F, cardColor);
                  RenderUtil.drawBorder(context, x, drawY, cardWidth, moduleCardHeight, 5.0F, borderColor, 1.0F, 0.0F, 0.5F);
                  String moduleKey = module.getClass().getName();
                  float targetState = module.isEnable() ? 1.0F : 0.0F;
                  float stateAnim = animateSlotState ? animation.animate01("module:state:slot:" + i, targetState, 11.0F, targetState) : targetState;
                  float toggleX = x + cardWidth - 8.0F - 15.0F;
                  float toggleY = drawY + 7.5F;
                  String bindText = this.formatBindText(module.getBind().getKeys());
                  float bindTargetWidth = this.getModuleBindTargetWidth(bindText);
                  float bindWidth = this.animateWidth(animation, this.moduleBindWidths, "module:bind:w:" + moduleKey, bindTargetWidth, 20.0F);
                  float bindRightX = toggleX - 4.0F;
                  float bindX = bindRightX - bindWidth;
                  float bindY = drawY + 8.25F;
                  boolean bindHoveredRaw = this.isHovered(mouseX, mouseY, bindX, bindY, bindWidth, 8.5F);
                  float bindHover = animation.animate01("module:bind:hover:" + moduleKey, bindHoveredRaw ? 1.0F : 0.0F, 12.0F, 0.0F);
                  float bindTextReveal = this.animateTextRevealAfterWidth(
                     animation,
                     this.moduleBindTexts,
                     this.moduleBindTextUnlocked,
                     moduleKey,
                     "module:bind:text:reveal:" + moduleKey,
                     bindText,
                     bindWidth,
                     bindTargetWidth
                  );
                  int bindBgBase = ColorUtil.interpolate(ClientColors.SETTINGS_BACKGROUND, ClientColors.CATEGORY_ACCENT, bindHover * 0.22F);
                  int bindBgColor = ColorUtil.multiplyAlpha(bindBgBase, alpha);
                  int bindBorderColor = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, alpha);
                  int bindIconBase = ColorUtil.interpolate(ClientColors.CATEGORY_INACTIVE, ClientColors.TEXT_PRIMARY, bindHover * 0.55F);
                  int bindTextBase = ColorUtil.interpolate(ClientColors.CATEGORY_INACTIVE, ClientColors.TEXT_PRIMARY, bindHover * 0.6F);
                  int bindIconColor = ColorUtil.multiplyAlpha(bindIconBase, alpha);
                  int bindTextColor = ColorUtil.multiplyAlpha(bindTextBase, alpha * bindTextReveal);
                  RenderUtil.drawRoundRect(context, bindX, bindY, bindWidth, 8.5F, 2.5F, bindBgColor);
                  RenderUtil.drawBorder(context, bindX, bindY, bindWidth, 8.5F, 2.5F, bindBorderColor, 0.0F, 0.5F, 1.0F);
                  float bindIconX = bindX + 3.0F;
                  float bindIconY = bindY + 1.0F;
                  RenderUtil.drawIcon(context, GIcons.KEYBOARD, bindIconX, bindIconY, 6.5F, bindIconColor);
                  float bindTextX = bindIconX + 6.5F + 2.0F;
                  float bindTextY = bindY + (8.5F + RenderUtil.getTextHeight(GFonts.BOLD, 5)) / 2.0F - 5.5F;
                  RenderUtil.drawText(context, bindText, bindTextX, bindTextY, bindTextColor, GFonts.BOLD, 5);
                  boolean toggleHoveredRaw = this.isHovered(mouseX, mouseY, toggleX - 1.0F, toggleY - 1.0F, 17.0F, 12.0F);
                  float toggleHover = animation.animate01("module:toggle:hover:" + moduleKey, toggleHoveredRaw ? 1.0F : 0.0F, 14.0F, 0.0F);
                  int toggleTrackBase = ColorUtil.interpolate(ClientColors.SETTINGS_BACKGROUND, ClientColors.CATEGORY_ACCENT, stateAnim);
                  toggleTrackBase = ColorUtil.interpolate(toggleTrackBase, ClientColors.TEXT_PRIMARY, toggleHover * 0.12F);
                  int toggleTrackColor = ColorUtil.multiplyAlpha(toggleTrackBase, alpha);
                  int trackOutlineColor = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, alpha * (1.0F - stateAnim));
                  RenderUtil.drawRoundRect(context, toggleX, toggleY, 15.0F, 10.0F, 4.0F, toggleTrackColor);
                  RenderUtil.drawBorder(context, toggleX, toggleY, 15.0F, 10.0F, 4.0F, trackOutlineColor, 0.0F, 0.5F, 1.0F);
                  float knobTravel = 6.0F;
                  float knobX = toggleX + 1.0F + knobTravel * stateAnim;
                  float knobY = toggleY + 1.5F;
                  int knobBase = ColorUtil.interpolate(ClientColors.CATEGORY_INACTIVE, ClientColors.TEXT_PRIMARY, stateAnim);
                  knobBase = ColorUtil.interpolate(knobBase, ClientColors.TEXT_PRIMARY, toggleHover * 0.08F);
                  int knobColor = ColorUtil.multiplyAlpha(knobBase, alpha * 0.95F);
                  float knobOffsetX = 1.0F + -2.0F * stateAnim;
                  RenderUtil.drawRoundRect(context, knobX + knobOffsetX, knobY, 7.0F, 7.0F, 2.5F, knobColor);
                  float textY = drawY + (25.0F + RenderUtil.getTextHeight(GFonts.MEDIUM, 9)) / 2.0F - 8.0F;
                  float textX = x + 8.0F;
                  float maxModuleTextWidth = bindX - 6.0F - textX;
                  float moduleNameWidth = RenderUtil.getTextWidth(module.getName(), GFonts.MEDIUM, 9);
                  if (moduleNameWidth <= maxModuleTextWidth) {
                     this.moduleTitleScrollValues.remove(moduleKey);
                     this.moduleTitleScrollForward.remove(moduleKey);
                     RenderUtil.drawText(context, module.getName(), textX, textY - 0.5F, textColor, GFonts.MEDIUM, 9);
                  } else {
                     boolean titleHoveredRaw = this.isHovered(mouseX, mouseY, textX, drawY, maxModuleTextWidth, 25.0F);
                     float titleHover = animation.animate01("module:title:hover:" + moduleKey, titleHoveredRaw ? 1.0F : 0.0F, 8.0F, 0.0F);
                     float titleMix = this.smootherStep01(titleHover);
                     float motionBlend = this.smootherStep01(ColorUtil.clamp01((titleMix - 0.22F) / 0.78F));
                     float gradientBlend = this.smootherStep01(1.0F - ColorUtil.clamp01((titleMix - 0.1F) / 0.78F));
                     float overflowWidth = Math.max(0.0F, moduleNameWidth - maxModuleTextWidth);
                     float shiftMax = overflowWidth;
                     float scrollOffset = this.updateModuleTitleShift(animation, moduleKey, titleMix, shiftMax);
                     float motionAlpha = motionBlend;
                     float fadeAlpha = gradientBlend;
                     float easedShift = scrollOffset * motionBlend;
                     if (fadeAlpha > 0.004F) {
                        int fadeColor = ColorUtil.multiplyAlpha(textColor, fadeAlpha);
                        this.drawTextWithRightFade(context, module.getName(), textX, textY, fadeColor, GFonts.MEDIUM, 9, maxModuleTextWidth, 9.0F, drawY, 25.0F);
                     }

                     if (motionAlpha > 0.004F) {
                        int motionColor = ColorUtil.multiplyAlpha(textColor, motionAlpha);
                        this.drawTextClipped(
                           context, module.getName(), textX - easedShift, textY - 0.5F, motionColor, GFonts.MEDIUM, 9, textX, maxModuleTextWidth, drawY, 25.0F
                        );
                     }
                  }

                  List<Setting> renderSettings = this.getRenderableSettings(module);
                  if (!renderSettings.isEmpty()) {
                     float titleOffset = (25.0F + RenderUtil.getTextHeight(GFonts.MEDIUM, 9)) / 2.0F - 8.0F;
                     float settingX = x + 8.0F;
                     float settingW = Math.max(16.0F, x + cardWidth - 8.0F - settingX);
                     float settingY = drawY + titleOffset + titleOffset + 4.0F;

                     for (int si = 0; si < renderSettings.size(); si++) {
                        Setting setting = renderSettings.get(si);
                        Setting nextSetting = si + 1 < renderSettings.size() ? renderSettings.get(si + 1) : null;
                        if (setting instanceof BooleanSetting moduleBoolean) {
                           BooleanSettingRenderer.render(
                              context, animation, moduleKey + ":" + moduleBoolean.getName(), moduleBoolean, settingX, settingY, settingW, mouseX, mouseY, alpha
                           );
                           settingY += BooleanSettingRenderer.measureHeight(moduleBoolean, settingW) + this.getSettingsGap(setting, nextSetting);
                        } else if (setting instanceof SliderSetting sliderSetting) {
                           SliderSettingRenderer.render(
                              context, animation, moduleKey + ":" + sliderSetting.getName(), sliderSetting, settingX, settingY, settingW, mouseX, mouseY, alpha
                           );
                           settingY += SliderSettingRenderer.measureHeight(sliderSetting, settingW) + this.getSettingsGap(setting, nextSetting);
                        } else if (setting instanceof ColorSetting colorSetting) {
                           ColorSettingRenderer.render(
                              context, animation, moduleKey + ":" + colorSetting.getName(), colorSetting, settingX, settingY, settingW, mouseX, mouseY, alpha
                           );
                           settingY += ColorSettingRenderer.measureHeight(colorSetting, settingW) + this.getSettingsGap(setting, nextSetting);
                        } else if (setting instanceof SelectSetting selectSetting) {
                           String settingKey = moduleKey + ":" + selectSetting.getName();
                           boolean expanded = this.selectExpanded.getOrDefault(settingKey, false);
                           float expandProgress = this.getSelectExpandProgress(settingKey, expanded);
                           SelectSettingRenderer.render(
                              context, animation, settingKey, selectSetting, settingX, settingY, settingW, mouseX, mouseY, alpha, expandProgress
                           );
                           settingY += SelectSettingRenderer.measureHeight(selectSetting, settingW, expandProgress) + this.getSettingsGap(setting, nextSetting);
                        } else if (setting instanceof MultiSelectSetting multiSelectSetting) {
                           String settingKey = moduleKey + ":" + multiSelectSetting.getName();
                           boolean expanded = this.multiSelectExpanded.getOrDefault(settingKey, false);
                           float expandProgress = this.getMultiSelectExpandProgress(settingKey, expanded);
                           MultiSelectSettingRenderer.render(
                              context, animation, settingKey, multiSelectSetting, settingX, settingY, settingW, mouseX, mouseY, alpha, expandProgress
                           );
                           settingY += MultiSelectSettingRenderer.measureHeight(multiSelectSetting, settingW, expandProgress)
                              + this.getSettingsGap(setting, nextSetting);
                        }
                     }
                  }

                  columnBottomY[col] = y + moduleCardHeight + 8.0F;
               } else {
                  columnBottomY[col] = y + moduleCardHeight + 8.0F;
               }
            }

            context.disableScissor();
            if (maxScroll <= 0.01F) {
               this.tabScrollbars.remove(tab);
            } else {
               float trackX = panelX + panelWidth - 4.0F - 2.0F;
               float trackY = viewportTop + 2.0F;
               float trackH = Math.max(8.0F, viewportHeight - 4.0F);
               float contentHeight = viewportHeight + maxScroll;
               float thumbH = Math.max(18.0F, trackH * (viewportHeight / Math.max(1.0F, contentHeight)));
               thumbH = Math.min(trackH, thumbH);
               float travel = Math.max(0.0F, trackH - thumbH);
               float progress = maxScroll <= 0.0F ? 0.0F : this.clamp(scrollValue / maxScroll, 0.0F, 1.0F);
               float thumbY = trackY + travel * progress;
               boolean scrollbarHoveredRaw = this.isHovered(mouseX, mouseY, trackX - 2.0F, trackY, 6.0F, trackH);
               float scrollbarHover = animation.animate01("modules:scrollbar:hover:" + tab.name(), scrollbarHoveredRaw ? 1.0F : 0.0F, 14.0F, 0.0F);
               boolean scrollbarPressed = this.scrollbarDragging && tab == this.displayTab;
               float scrollbarPress = animation.animate01("modules:scrollbar:press:" + tab.name(), scrollbarPressed ? 1.0F : 0.0F, 18.0F, 0.0F);
               float trackAlpha = alpha * (0.2F + scrollbarHover * 0.14F + scrollbarPress * 0.1F);
               int trackColor = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, trackAlpha);
               int thumbBase = ColorUtil.interpolate(ClientColors.SCROLLBAR, ClientColors.TEXT_PRIMARY, scrollbarHover * 0.32F + scrollbarPress * 0.18F);
               int thumbColor = ColorUtil.multiplyAlpha(thumbBase, alpha * (0.68F + scrollbarHover * 0.16F + scrollbarPress * 0.16F));
               RenderUtil.drawRoundRect(context, trackX, trackY, 2.0F, trackH, 1.0F, trackColor);
               RenderUtil.drawRoundRect(context, trackX, thumbY, 2.0F, thumbH, 1.0F, thumbColor);
               this.tabScrollbars.put(tab, new ModuleGridSection.ScrollbarState(trackX, trackY, 2.0F, trackH, thumbY, thumbH, maxScroll));
            }
         }
      }
   }

   public boolean handleClick(MouseButtonEvent click, float panelX, float panelY, float panelWidth, float panelHeight, CategoryBarSection.TopTab activeTab) {
      if (this.isColorPopupActive()) {
         if (!this.colorPopupOpen) {
            return true;
         }

         float[] popupRect = this.getColorPopupRect();
         float px = popupRect[0];
         float py = popupRect[1];
         float pw = popupRect[2];
         float ph = popupRect[3];
         if (!this.isHovered(click.x(), click.y(), px, py, pw, ph)) {
            this.colorPopupOpen = false;
            this.colorDragMode = ModuleGridSection.ColorDragMode.NONE;
            return true;
         }

         if (click.button() == 0) {
            ModuleGridSection.ColorPopupLayout layout = this.getColorPopupLayout(px, py);
            if (this.isHovered(click.x(), click.y(), layout.closeX, layout.closeY, layout.closeW, layout.closeH)) {
               this.colorPopupOpen = false;
               this.colorDragMode = ModuleGridSection.ColorDragMode.NONE;
               return true;
            }

            int presetIndex = this.hitColorPresetIndex(layout, click.x(), click.y());
            if (presetIndex >= 0) {
               this.applyColorPreset(presetIndex);
               return true;
            }

            if (this.isHovered(click.x(), click.y(), layout.copyX, layout.infoY + 4.0F, 7.0F, 7.0F)) {
               this.copyCurrentColor();
               return true;
            }

            if (this.isHovered(click.x(), click.y(), layout.loadX, layout.infoY + 4.0F, 7.0F, 7.0F)) {
               this.pasteCurrentColor();
               return true;
            }

            if (this.isHovered(click.x(), click.y(), layout.paletteX, layout.paletteY, layout.paletteSize, layout.paletteSize)) {
               this.colorDragMode = ModuleGridSection.ColorDragMode.PALETTE;
               this.updateColorFromMouse(ModuleGridSection.ColorDragMode.PALETTE, click.x(), click.y(), layout);
               return true;
            }

            if (this.isHovered(click.x(), click.y(), layout.hueX, layout.paletteY, layout.barW, layout.paletteSize)) {
               this.colorDragMode = ModuleGridSection.ColorDragMode.HUE;
               this.updateColorFromMouse(ModuleGridSection.ColorDragMode.HUE, click.x(), click.y(), layout);
               return true;
            }

            if (this.isHovered(click.x(), click.y(), layout.alphaX, layout.paletteY, layout.barW, layout.paletteSize)) {
               this.colorDragMode = ModuleGridSection.ColorDragMode.ALPHA;
               this.updateColorFromMouse(ModuleGridSection.ColorDragMode.ALPHA, click.x(), click.y(), layout);
               return true;
            }
         }

         return true;
      } else if (this.isBindPopupActive()) {
         if (!this.bindPopupOpen) {
            return true;
         }

         float[] popupRect = this.getPopupRect(panelX, panelY, panelWidth, panelHeight);
         float px = popupRect[0];
         float py = popupRect[1];
         float pw = popupRect[2];
         float ph = popupRect[3];
         if (!this.isHovered(click.x(), click.y(), px, py, pw, ph)) {
            this.bindPopupOpen = false;
            this.bindCapture = false;
            this.clearCaptureState();
            return true;
         }

         if (this.bindCapture) {
            this.onCapturePress(Havoc.mouseButtonToBindCode(click.button()));
            return true;
         }

         if (click.button() == 0) {
            float modeY = py + 32.0F;
            float segW = 72.0F;
            float segX = px + pw - 8.0F - segW;
            float segY = modeY - 7.0F;
            float halfW = segW * 0.5F;
            if (this.isHovered(click.x(), click.y(), segX, segY, halfW, 14.0F)) {
               this.getActivePopupBind().setMode(BindMode.HOLD);
               return true;
            }

            if (this.isHovered(click.x(), click.y(), segX + halfW, segY, halfW, 14.0F)) {
               this.getActivePopupBind().setMode(BindMode.TOGGLE);
               return true;
            }

            float keyY = py + 52.0F;
            String keyText = this.bindCapture ? "..." : this.formatBindText(this.getActivePopupBind().getKeys());
            float keyTargetW = this.getPopupKeyTargetWidth(keyText);
            float keyW = keyTargetW;
            float keyX = px + pw - 8.0F - keyW;
            if (this.isHovered(click.x(), click.y(), keyX, keyY - 7.0F, keyW, 14.0F)) {
               this.bindCapture = true;
               this.clearCaptureState();
               return true;
            }
         }

         return true;
      } else {
         if (click.button() == 0 && this.handleScrollbarPress((float)click.x(), (float)click.y(), activeTab)) {
            return true;
         }

         if (click.button() != 0) {
            return false;
         }

         this.selectedSliderModule = null;
         this.selectedSliderSetting = null;
         Category category = this.toCategory(activeTab);
         if (category == null) {
            return false;
         }

         List<Module> modules = this.visibleModules(category);
         if (modules.isEmpty()) {
            return false;
         }

         float contentX = panelX + 10.0F;
         float contentY = panelY + 14.0F;
         float contentW = panelWidth - 20.0F;
         float cardWidth = (contentW - 8.0F) / 2.0F;
         float viewportTop = contentY;
         float viewportBottom = panelY + panelHeight - 8.0F;
         float viewportHeight = Math.max(1.0F, viewportBottom - viewportTop);
         float maxScroll = this.computeMaxScroll(modules, cardWidth, viewportHeight, null);
         float scrollValue = this.clamp(this.tabScrollValues.getOrDefault(activeTab, this.tabScrollTargets.getOrDefault(activeTab, 0.0F)), 0.0F, maxScroll);
         this.tabMaxScroll.put(activeTab, maxScroll);
         float[] columnBottomY = new float[2];

         for (int c = 0; c < 2; c++) {
            columnBottomY[c] = contentY;
         }

         for (int i = 0; i < modules.size(); i++) {
            int col = i % 2;
            float x = contentX + col * (cardWidth + 8.0F);
            float y = columnBottomY[col];
            Module module = modules.get(i);
            float moduleCardHeight = this.getModuleCardHeight(module, cardWidth, null);
            float drawY = y - scrollValue;
            if (!(drawY >= viewportBottom) && !(drawY + moduleCardHeight <= viewportTop)) {
               float toggleX = x + cardWidth - 8.0F - 15.0F;
               float toggleY = drawY + 7.5F;
               if (this.isHovered(click.x(), click.y(), toggleX - 1.0F, toggleY - 1.0F, 17.0F, 12.0F)) {
                  module.toggle();
                  return true;
               }

               String bindText = this.formatBindText(module.getBind().getKeys());
               float bindTargetWidth = this.getModuleBindTargetWidth(bindText);
               float bindWidth = bindTargetWidth;
               float bindX = toggleX - 4.0F - bindWidth;
               float bindY = drawY + 8.25F;
               if (this.isHovered(click.x(), click.y(), bindX, bindY, bindWidth, 8.5F)) {
                  this.bindPopupModule = module;
                  this.bindPopupSetting = null;
                  this.bindPopupOpen = true;
                  this.bindPopupAnchorX = (float)click.x();
                  this.bindPopupAnchorY = (float)click.y();
                  this.bindCapture = false;
                  this.clearCaptureState();
                  return true;
               }

               List<Setting> renderSettings = this.getRenderableSettings(module);
               if (!renderSettings.isEmpty()) {
                  float iconX = x + 8.0F;
                  float titleOffset = (25.0F + RenderUtil.getTextHeight(GFonts.MEDIUM, 9)) / 2.0F - 8.0F;
                  float settingX = iconX;
                  float settingW = Math.max(16.0F, x + cardWidth - 8.0F - settingX);
                  float settingY = drawY + titleOffset + titleOffset + 4.0F;

                  for (int si = 0; si < renderSettings.size(); si++) {
                     Setting setting = renderSettings.get(si);
                     Setting nextSetting = si + 1 < renderSettings.size() ? renderSettings.get(si + 1) : null;
                     if (setting instanceof BooleanSetting moduleBoolean) {
                        if (BooleanSettingRenderer.hitKeyIcon(moduleBoolean, settingX, settingY, settingW, click.x(), click.y())) {
                           this.bindPopupModule = module;
                           this.bindPopupSetting = moduleBoolean;
                           this.bindPopupOpen = true;
                           this.bindPopupAnchorX = (float)click.x();
                           this.bindPopupAnchorY = (float)click.y();
                           this.bindCapture = false;
                           this.clearCaptureState();
                           return true;
                        }

                        if (BooleanSettingRenderer.hitCheckbox(moduleBoolean, settingX, settingY, settingW, click.x(), click.y())) {
                           moduleBoolean.toggle();
                           return true;
                        }

                        settingY += BooleanSettingRenderer.measureHeight(moduleBoolean, settingW) + this.getSettingsGap(setting, nextSetting);
                     } else if (setting instanceof SliderSetting sliderSetting) {
                        if (click.button() == 0 && SliderSettingRenderer.hitTrack(sliderSetting, settingX, settingY, settingW, click.x(), click.y())) {
                           SliderSettingRenderer.setFromMouse(sliderSetting, settingX, settingY, settingW, click.x());
                           this.draggingSliderModule = module;
                           this.draggingSliderSetting = sliderSetting;
                           this.selectedSliderModule = module;
                           this.selectedSliderSetting = sliderSetting;
                           this.draggingSliderX = settingX;
                           this.draggingSliderY = settingY;
                           this.draggingSliderW = settingW;
                           return true;
                        }

                        settingY += SliderSettingRenderer.measureHeight(sliderSetting, settingW) + this.getSettingsGap(setting, nextSetting);
                     } else if (setting instanceof ColorSetting colorSetting) {
                        if (ColorSettingRenderer.hitThemeButton(colorSetting, settingX, settingY, settingW, click.x(), click.y())) {
                           this.colorPopupModule = module;
                           this.colorPopupSetting = colorSetting;
                           this.colorPopupOpen = true;
                           this.colorPopupAnchorX = (float)click.x();
                           this.colorPopupAnchorY = (float)click.y();
                           this.colorDragMode = ModuleGridSection.ColorDragMode.NONE;
                           return true;
                        }

                        settingY += ColorSettingRenderer.measureHeight(colorSetting, settingW) + this.getSettingsGap(setting, nextSetting);
                     } else if (setting instanceof SelectSetting selectSetting) {
                        String settingKey = module.getClass().getName() + ":" + selectSetting.getName();
                        boolean expanded = this.selectExpanded.getOrDefault(settingKey, false);
                        float expandProgress = this.getSelectExpandProgress(settingKey, expanded);
                        if (SelectSettingRenderer.hitSelectBox(selectSetting, settingX, settingY, settingW, click.x(), click.y())) {
                           this.selectExpanded.put(settingKey, !expanded);
                           return true;
                        }

                        int optionIndex = SelectSettingRenderer.hitOptionIndex(
                           selectSetting, settingX, settingY, settingW, click.x(), click.y(), expandProgress
                        );
                        if (optionIndex >= 0 && optionIndex < selectSetting.getOptions().size()) {
                           selectSetting.setIndex(optionIndex);
                           return true;
                        }

                        settingY += SelectSettingRenderer.measureHeight(selectSetting, settingW, expandProgress) + this.getSettingsGap(setting, nextSetting);
                     } else if (setting instanceof MultiSelectSetting multiSelectSetting) {
                        String settingKey = module.getClass().getName() + ":" + multiSelectSetting.getName();
                        boolean expanded = this.multiSelectExpanded.getOrDefault(settingKey, false);
                        float expandProgress = this.getMultiSelectExpandProgress(settingKey, expanded);
                        if (MultiSelectSettingRenderer.hitSelectBox(multiSelectSetting, settingX, settingY, settingW, click.x(), click.y())) {
                           this.multiSelectExpanded.put(settingKey, !expanded);
                           return true;
                        }

                        int optionIndex = MultiSelectSettingRenderer.hitOptionIndex(
                           multiSelectSetting, settingX, settingY, settingW, click.x(), click.y(), expandProgress
                        );
                        if (optionIndex >= 0 && optionIndex < multiSelectSetting.getOptions().size()) {
                           multiSelectSetting.toggle(multiSelectSetting.getOptions().get(optionIndex));
                           return true;
                        }

                        settingY += MultiSelectSettingRenderer.measureHeight(multiSelectSetting, settingW, expandProgress)
                           + this.getSettingsGap(setting, nextSetting);
                     }
                  }
               }

               columnBottomY[col] = y + moduleCardHeight + 8.0F;
            } else {
               columnBottomY[col] = y + moduleCardHeight + 8.0F;
            }
         }

         return false;
      }
   }

   public boolean handleKeyPressed(KeyEvent key) {
      if (this.isBindPopupActive() && this.bindPopupOpen) {
         int code = key.key();
         if (this.bindCapture) {
            if (code == 256) {
               this.getActivePopupBind().clearKeys();
               this.bindCapture = false;
               this.clearCaptureState();
               return true;
            } else {
               this.onCapturePress(code);
               return true;
            }
         } else if (code == 256) {
            this.bindPopupOpen = false;
            this.bindCapture = false;
            this.clearCaptureState();
            return true;
         } else {
            return false;
         }
      } else {
         int code = key.key();
         if (this.selectedSliderSetting != null) {
            if (code == 263) {
               this.selectedSliderSetting.setValue(this.selectedSliderSetting.getValue() - this.selectedSliderSetting.getStep());
               return true;
            }

            if (code == 262) {
               this.selectedSliderSetting.setValue(this.selectedSliderSetting.getValue() + this.selectedSliderSetting.getStep());
               return true;
            }
         }

         return false;
      }
   }

   public boolean handleKeyReleased(KeyEvent key) {
      if (this.isBindPopupActive() && this.bindPopupOpen && this.bindCapture) {
         this.onCaptureRelease(key.key());
         return true;
      } else {
         return false;
      }
   }

   public boolean handleMouseReleased(int button) {
      if (button == 0 && this.draggingSliderSetting != null) {
         this.draggingSliderModule = null;
         this.draggingSliderSetting = null;
         return true;
      } else if (button == 0 && this.colorDragMode != ModuleGridSection.ColorDragMode.NONE) {
         this.colorDragMode = ModuleGridSection.ColorDragMode.NONE;
         return true;
      } else if (button == 0 && this.scrollbarDragging) {
         this.scrollbarDragging = false;
         return true;
      } else if (this.isBindPopupActive() && this.bindPopupOpen && this.bindCapture) {
         this.onCaptureRelease(Havoc.mouseButtonToBindCode(button));
         return true;
      } else {
         return false;
      }
   }

   public boolean handleScroll(
      double mouseX, double mouseY, double verticalAmount, float panelX, float panelY, float panelWidth, float panelHeight, CategoryBarSection.TopTab activeTab
   ) {
      if (!this.isBindPopupActive() && !this.isColorPopupActive() && verticalAmount != 0.0) {
         float contentX = panelX + 10.0F;
         float contentY = panelY + 14.0F;
         float contentW = panelWidth - 20.0F;
         float contentH = panelHeight - 14.0F - 8.0F;
         if (!this.isHovered(mouseX, mouseY, contentX, contentY, contentW, contentH)) {
            return false;
         } else {
            Category category = this.toCategory(activeTab);
            if (category == null) {
               return false;
            } else {
               List<Module> modules = this.visibleModules(category);
               if (modules.isEmpty()) {
                  return false;
               } else {
                  float cardWidth = (contentW - 8.0F) / 2.0F;
                  float viewportHeight = Math.max(1.0F, contentH);
                  float maxScroll = this.computeMaxScroll(modules, cardWidth, viewportHeight, null);
                  this.tabMaxScroll.put(activeTab, maxScroll);
                  if (maxScroll <= 0.01F) {
                     this.tabScrollTargets.put(activeTab, 0.0F);
                     this.tabScrollValues.put(activeTab, 0.0F);
                     return true;
                  } else {
                     float currentTarget = this.tabScrollTargets.getOrDefault(activeTab, this.tabScrollValues.getOrDefault(activeTab, 0.0F));
                     float nextTarget = this.clamp(currentTarget - (float)verticalAmount * 22.0F, 0.0F, maxScroll);
                     this.tabScrollTargets.put(activeTab, nextTarget);
                     return true;
                  }
               }
            }
         }
      } else {
         return false;
      }
   }

   private boolean handleScrollbarPress(float mouseX, float mouseY, CategoryBarSection.TopTab activeTab) {
      ModuleGridSection.ScrollbarState state = this.tabScrollbars.get(activeTab);
      if (state != null && !(state.maxScroll <= 0.01F)) {
         if (!this.isHovered(mouseX, mouseY, state.trackX - 2.0F, state.trackY, state.trackW + 4.0F, state.trackH)) {
            return false;
         }

         this.scrollbarDragging = true;
         if (mouseY >= state.thumbY && mouseY <= state.thumbY + state.thumbH) {
            this.scrollbarDragOffsetY = mouseY - state.thumbY;
         } else {
            this.scrollbarDragOffsetY = state.thumbH * 0.5F;
            float thumbTop = mouseY - this.scrollbarDragOffsetY;
            this.setScrollFromThumbTop(activeTab, state, thumbTop);
         }

         return true;
      } else {
         return false;
      }
   }

   private void updateScrollbarDrag(CategoryBarSection.TopTab activeTab, float mouseY) {
      if (this.scrollbarDragging) {
         ModuleGridSection.ScrollbarState state = this.tabScrollbars.get(activeTab);
         if (state != null && !(state.maxScroll <= 0.01F)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getWindow() != null) {
               long handle = mc.getWindow().handle();
               if (GLFW.glfwGetMouseButton(handle, 0) != 1) {
                  this.scrollbarDragging = false;
               } else {
                  float thumbTop = mouseY - this.scrollbarDragOffsetY;
                  this.setScrollFromThumbTop(activeTab, state, thumbTop);
               }
            } else {
               this.scrollbarDragging = false;
            }
         } else {
            this.scrollbarDragging = false;
         }
      }
   }

   private void setScrollFromThumbTop(CategoryBarSection.TopTab tab, ModuleGridSection.ScrollbarState state, float thumbTop) {
      float travel = Math.max(0.0F, state.trackH - state.thumbH);
      if (!(travel <= 0.001F) && !(state.maxScroll <= 0.001F)) {
         float clampedThumbTop = this.clamp(thumbTop, state.trackY, state.trackY + travel);
         float progress = (clampedThumbTop - state.trackY) / travel;
         float targetScroll = this.clamp(progress * state.maxScroll, 0.0F, state.maxScroll);
         this.tabScrollTargets.put(tab, targetScroll);
         this.tabScrollValues.put(tab, targetScroll);
      } else {
         this.tabScrollTargets.put(tab, 0.0F);
         this.tabScrollValues.put(tab, 0.0F);
      }
   }

   public void requestClosePopup() {
      this.bindPopupOpen = false;
      this.bindCapture = false;
      this.colorPopupOpen = false;
      this.colorPopupTitleOverride = null;
      this.colorDragMode = ModuleGridSection.ColorDragMode.NONE;
      this.draggingSliderModule = null;
      this.draggingSliderSetting = null;
      this.selectedSliderModule = null;
      this.selectedSliderSetting = null;
      this.clearCaptureState();
   }

   public boolean isPopupOpen() {
      return this.isBindPopupOpen() || this.isColorPopupOpen();
   }

   public boolean isBindPopupOpen() {
      return this.isBindPopupActive() && this.bindPopupOpen;
   }

   public boolean isColorPopupOpen() {
      return this.isColorPopupActive() && this.colorPopupOpen;
   }

   public void openStandaloneColorPicker(ColorSetting setting, String title, float anchorX, float anchorY) {
      this.colorPopupModule = null;
      this.colorPopupSetting = setting;
      this.colorPopupTitleOverride = title;
      this.colorPopupOpen = true;
      this.colorPopupAnchorX = anchorX;
      this.colorPopupAnchorY = anchorY;
      this.colorDragMode = ModuleGridSection.ColorDragMode.NONE;
   }

   public void renderOverlay(GuiGraphicsExtractor context, UiAnimationEngine animation, int mouseX, int mouseY, float alpha) {
      this.renderColorPopup(context, animation, mouseX, mouseY, alpha);
   }

   private void renderBindPopup(
      GuiGraphicsExtractor context,
      UiAnimationEngine animation,
      float panelX,
      float panelY,
      float panelWidth,
      float panelHeight,
      int mouseX,
      int mouseY,
      float alpha
   ) {
      if (this.isBindPopupActive()) {
         String popupKey = this.getPopupAnimKey();
         float target = this.bindPopupOpen ? 1.0F : 0.0F;
         float popupAlpha = animation.animate01("bind:popup:alpha:" + popupKey, target, 14.0F, 0.0F);
         if (popupAlpha <= 0.01F) {
            if (!this.bindPopupOpen) {
               this.bindPopupModule = null;
               this.bindPopupSetting = null;
            }
         } else {
            float[] popupRect = this.getPopupRect(panelX, panelY, panelWidth, panelHeight);
            float x = popupRect[0];
            float y = popupRect[1];
            float w = popupRect[2];
            float h = popupRect[3];
            float a = alpha * popupAlpha;
            RenderUtil.drawRoundRect(context, x, y, w, h, 6.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND, a));
            RenderUtil.drawBorder(context, x, y, w, h, 6.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND_OUTLINE, a), 1.0F, 0.0F, 0.5F);
            float titleY = y + 8.5F;
            float titleIconX = x + 8.0F;
            String badgeText = "KEYBIND";
            float badgeTextW = RenderUtil.getTextWidth(badgeText, GFonts.BOLD, 6);
            float badgeW = 15.0F + badgeTextW + 6.0F;
            float badgeX = x + w - 8.0F - badgeW;
            float badgeY = y + 7.0F;
            RenderUtil.drawIcon(context, GIcons.KEYBOARD, titleIconX, titleY, 8.0F, ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, a));
            float titleTextX = titleIconX + 12.0F;
            float titleMaxW = Math.max(6.0F, badgeX - 4.0F - titleTextX);
            int titleScX1 = (int)Math.floor(titleTextX);
            int titleScY1 = (int)Math.floor(y - 1.0F);
            int titleScX2 = (int)Math.ceil(titleTextX + titleMaxW);
            int titleScY2 = (int)Math.ceil(y + 16.0F);
            context.enableScissor(titleScX1, titleScY1, titleScX2, titleScY2);
            RenderUtil.drawText(
               context, this.getPopupTitle(), titleTextX, titleY + 1.0F, ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, a), GFonts.SEMIBOLD, 9
            );
            context.disableScissor();
            int badgeBg = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, a * 0.3F);
            RenderUtil.drawRoundRect(context, badgeX, badgeY, badgeW, 11.0F, 3.0F, badgeBg);
            RenderUtil.drawImage(
               context,
               "textures/gui/icons/property.png",
               badgeX + 6.0F,
               badgeY + 2.5F,
               6.0F,
               6.0F,
               0.0F,
               ColorUtil.multiplyAlpha(ColorUtil.withAlpha(ClientColors.CATEGORY_ACCENT, 200), a),
               1.0F
            );
            RenderUtil.drawText(context, badgeText, badgeX + 15.0F, badgeY + 3.5F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, a), GFonts.BOLD, 6);
            float modeY = y + 32.0F;
            RenderUtil.drawText(context, "Bind mode", x + 8.0F, modeY - 3.0F, ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, a), GFonts.SEMIBOLD, 8);
            float segW = 72.0F;
            float segX = x + w - 8.0F - segW;
            float segY = modeY - 7.0F;
            int segBg = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_BACKGROUND, a);
            int segBorder = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, a);
            RenderUtil.drawRoundRect(context, segX, segY, segW, 14.0F, 3.0F, segBg);
            RenderUtil.drawBorder(context, segX, segY, segW, 14.0F, 3.0F, segBorder, 0.0F, 0.5F, 1.0F);
            float modeAnim = animation.animate01(
               "bind:mode:" + popupKey,
               this.getActivePopupBind().getMode() == BindMode.TOGGLE ? 1.0F : 0.0F,
               22.0F,
               this.getActivePopupBind().getMode() == BindMode.TOGGLE ? 1.0F : 0.0F
            );
            float halfW = segW * 0.5F;
            float activeX = segX + halfW * modeAnim;
            RenderUtil.drawRoundRect(context, activeX, segY, halfW, 14.0F, 3.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, a));
            RenderUtil.drawRoundRect(context, segX + halfW - 0.5F, segY + 1.5F, 1.0F, 11.0F, 0.5F, ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, a));
            int holdColor = ColorUtil.multiplyAlpha(ColorUtil.interpolate(ClientColors.TEXT_PRIMARY, ClientColors.CATEGORY_INACTIVE, modeAnim), a);
            int toggleColor = ColorUtil.multiplyAlpha(ColorUtil.interpolate(ClientColors.CATEGORY_INACTIVE, ClientColors.TEXT_PRIMARY, modeAnim), a);
            this.drawModeText(context, segX + 1.0F, segY - 1.0F, halfW, "HOLD", holdColor);
            this.drawModeText(context, segX + 1.0F + halfW, segY - 1.0F, halfW, "TOGGLE", toggleColor);
            float keyY = y + 52.0F;
            RenderUtil.drawText(context, "Key", x + 8.0F, keyY - 2.0F, ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, a), GFonts.SEMIBOLD, 8);
            String keyText = this.bindCapture
               ? (this.captureSelectedCodes.isEmpty() ? "..." : this.formatBindText(this.captureSelectedCodes))
               : this.formatBindText(this.getActivePopupBind().getKeys());
            float keyTargetW = this.getPopupKeyTargetWidth(keyText);
            float keyW = this.animateWidth(animation, this.popupKeyWidths, "popup:key:w:" + popupKey, keyTargetW, 20.0F);
            float keyTextReveal = this.animateTextRevealAfterWidth(
               animation, this.popupKeyTexts, this.popupKeyTextUnlocked, popupKey, "popup:key:text:reveal:" + popupKey, keyText, keyW, keyTargetW
            );
            float keyRightX = x + w - 8.0F;
            float keyX = keyRightX - keyW;
            boolean keyHoveredRaw = this.isHovered(mouseX, mouseY, keyX, keyY - 7.0F, keyW, 14.0F);
            float keyHover = animation.animate01("popup:key:hover:" + popupKey, keyHoveredRaw ? 1.0F : 0.0F, 12.0F, 0.0F);
            int keyBgBase = ColorUtil.interpolate(ClientColors.SETTINGS_BACKGROUND, ClientColors.CATEGORY_ACCENT, keyHover * 0.22F);
            int keyBg = ColorUtil.multiplyAlpha(keyBgBase, a);
            int keyBorder = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, a);
            float keyYDraw = keyY - 7.0F;
            RenderUtil.drawRoundRect(context, keyX, keyYDraw, keyW, 14.0F, 3.0F, keyBg);
            RenderUtil.drawBorder(context, keyX, keyYDraw, keyW, 14.0F, 3.0F, keyBorder, 0.0F, 0.5F, 1.0F);
            float keyIconX = keyX + 4.0F;
            float keyIconY = keyY - 7.0F + 3.75F;
            int keyIconColor = ColorUtil.multiplyAlpha(ColorUtil.interpolate(ClientColors.CATEGORY_INACTIVE, ClientColors.TEXT_PRIMARY, keyHover * 0.55F), a);
            RenderUtil.drawIcon(context, GIcons.KEYBOARD, keyIconX, keyIconY, 6.5F, keyIconColor);
            float keyTextY = keyY + (14.0F + RenderUtil.getTextHeight(GFonts.BOLD, 5)) / 2.0F - 5.1F - 7.0F;
            int keyTextColor = ColorUtil.multiplyAlpha(
               ColorUtil.interpolate(ClientColors.CATEGORY_INACTIVE, ClientColors.TEXT_PRIMARY, keyHover * 0.6F), a * keyTextReveal
            );
            RenderUtil.drawText(context, keyText, keyIconX + 6.5F + 3.0F, keyTextY - 0.5F, keyTextColor, GFonts.BOLD, 5);
         }
      }
   }

   private void renderColorPopup(GuiGraphicsExtractor context, UiAnimationEngine animation, int mouseX, int mouseY, float alpha) {
      if (this.isColorPopupActive()) {
         String popupKey = this.getColorPopupAnimKey();
         float target = this.colorPopupOpen ? 1.0F : 0.0F;
         float popupAlpha = animation.animate01("color:popup:alpha:" + popupKey, target, 14.0F, 0.0F);
         if (popupAlpha <= 0.01F) {
            if (!this.colorPopupOpen) {
               this.colorPopupModule = null;
               this.colorPopupSetting = null;
               this.colorPopupTitleOverride = null;
               this.colorDragMode = ModuleGridSection.ColorDragMode.NONE;
            }
         } else {
            float[] popupRect = this.getColorPopupRect();
            float x = popupRect[0];
            float y = popupRect[1];
            float w = popupRect[2];
            float h = popupRect[3];
            float a = alpha * popupAlpha;
            ModuleGridSection.ColorPopupLayout layout = this.getColorPopupLayout(x, y);
            context.fill(
               0,
               0,
               Minecraft.getInstance().getWindow().getGuiScaledWidth(),
               Minecraft.getInstance().getWindow().getGuiScaledHeight(),
               ColorUtil.multiplyAlpha(ColorUtil.rgb(3, 3, 4), a * 0.42F)
            );
            RenderUtil.drawShadow(context, x, y, w, h, 8.0F, ColorUtil.multiplyAlpha(ClientColors.MENU_SHADOW, a), 1.0F, 10.0F, 0.0F, 0.0F);
            RenderUtil.drawRoundRect(context, x, y, w, h, 6.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND, a));
            RenderUtil.drawBorder(context, x, y, w, h, 6.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND_OUTLINE, a), 1.0F, 0.0F, 0.5F);
            RenderUtil.drawImage(
               context, "textures/gui/icons/theme.png", x + 8.0F, y + 7.0F, 7.0F, 7.0F, 0.0F, ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, a), 0.0F
            );
            RenderUtil.drawText(
               context, this.getColorPopupTitle(), x + 8.0F + 11.0F, y + 6.8F, ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, a), GFonts.SEMIBOLD, 9
            );
            this.renderColorPalette(context, layout, a);
            this.renderColorBar(context, layout.hueX, layout.paletteY, layout.barW, layout.paletteSize, a, true);
            this.renderColorBar(context, layout.alphaX, layout.paletteY, layout.barW, layout.paletteSize, a, false);
            this.renderColorHandles(context, layout, a);
            this.renderColorInfo(context, layout, a);
            this.renderColorPresets(context, layout, mouseX, mouseY, a);
            boolean closeHovered = this.isHovered(mouseX, mouseY, layout.closeX, layout.closeY, layout.closeW, layout.closeH);
            float closeHover = animation.animate01("color:popup:close:hover:" + popupKey, closeHovered ? 1.0F : 0.0F, 14.0F, 0.0F);
            int closeBg = ColorUtil.multiplyAlpha(ColorUtil.interpolate(ClientColors.SETTINGS_BACKGROUND, ClientColors.CATEGORY_ACCENT, closeHover * 0.22F), a);
            RenderUtil.drawRoundRect(context, layout.closeX, layout.closeY, layout.closeW, layout.closeH, 4.0F, closeBg);
            RenderUtil.drawBorder(
               context,
               layout.closeX,
               layout.closeY,
               layout.closeW,
               layout.closeH,
               4.0F,
               ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, a),
               0.0F,
               0.6F,
               1.0F
            );
            float closeTextX = layout.closeX + (layout.closeW - RenderUtil.getTextWidth("Close", GFonts.BOLD, 7)) / 2.0F;
            float closeTextY = layout.closeY + (layout.closeH - RenderUtil.getTextHeight(GFonts.BOLD, 7)) / 2.0F + 1.0F;
            RenderUtil.drawText(context, "Close", closeTextX, closeTextY, ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, a), GFonts.BOLD, 7);
         }
      }
   }

   private void drawModeText(GuiGraphicsExtractor context, float x, float y, float width, String text, int color) {
      float textX = x + (width - RenderUtil.getTextWidth(text, GFonts.BOLD, 6)) / 2.0F;
      float textY = y + (14.0F + RenderUtil.getTextHeight(GFonts.BOLD, 6)) / 2.0F - 4.6F;
      RenderUtil.drawText(context, text, textX, textY, color, GFonts.BOLD, 6);
   }

   private float[] getPopupRect(float panelX, float panelY, float panelWidth, float panelHeight) {
      Minecraft mc = Minecraft.getInstance();
      float screenWidth = mc.getWindow().getGuiScaledWidth();
      float screenHeight = mc.getWindow().getGuiScaledHeight();
      float x = Math.min(panelX + panelWidth + 8.0F, screenWidth - 156.0F - 6.0F);
      float y = this.clamp(panelY + (panelHeight - 67.0F) * 0.5F, 6.0F, screenHeight - 67.0F - 6.0F);
      return new float[]{x, y, 156.0F, 67.0F};
   }

   private float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private boolean isBindPopupActive() {
      return this.bindPopupModule != null || this.bindPopupSetting != null;
   }

   private BindData getActivePopupBind() {
      if (this.bindPopupSetting != null) {
         return this.bindPopupSetting.getBind();
      } else {
         return this.bindPopupModule != null ? this.bindPopupModule.getBind() : null;
      }
   }

   private String getPopupAnimKey() {
      if (this.bindPopupSetting != null && this.bindPopupModule != null) {
         return this.bindPopupModule.getClass().getName() + ":setting:" + this.bindPopupSetting.getName();
      } else {
         return this.bindPopupModule != null ? this.bindPopupModule.getClass().getName() : "popup:none";
      }
   }

   private String getPopupTitle() {
      if (this.bindPopupSetting != null) {
         return this.bindPopupSetting.getName();
      } else {
         return this.bindPopupModule == null ? "" : this.bindPopupModule.getName();
      }
   }

   private boolean isColorPopupActive() {
      return this.colorPopupSetting != null;
   }

   private String getColorPopupAnimKey() {
      if (!this.isColorPopupActive()) {
         return "color:none";
      } else {
         return this.colorPopupModule != null
            ? this.colorPopupModule.getClass().getName() + ":" + this.colorPopupSetting.getName()
            : "standalone:" + (this.colorPopupTitleOverride == null ? this.colorPopupSetting.getName() : this.colorPopupTitleOverride);
      }
   }

   private String getColorPopupTitle() {
      if (this.colorPopupTitleOverride != null && !this.colorPopupTitleOverride.isBlank()) {
         return this.colorPopupTitleOverride;
      } else {
         return this.colorPopupSetting == null ? "" : this.colorPopupSetting.getName();
      }
   }

   private float[] getColorPopupRect() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.getWindow() != null) {
         float screenW = mc.getWindow().getGuiScaledWidth();
         float screenH = mc.getWindow().getGuiScaledHeight();
         float x = (screenW - 170.0F) * 0.5F;
         float y = (screenH - 194.0F) * 0.5F;
         return new float[]{x, y, 170.0F, 194.0F};
      } else {
         return new float[]{this.colorPopupAnchorX, this.colorPopupAnchorY, 170.0F, 194.0F};
      }
   }

   private ModuleGridSection.ColorPopupLayout getColorPopupLayout(float x, float y) {
      float paletteX = x + 8.0F;
      float paletteY = y + 20.0F;
      float hueX = paletteX + 110.0F + 6.0F;
      float alphaX = hueX + 8.0F + 4.0F;
      float infoY = paletteY + 110.0F + 8.0F;
      float presetsY = infoY + 14.0F + 7.0F;
      float closeY = presetsY + 7.0F + 10.0F;
      float infoW = 154.0F;
      float rightInset = 6.0F;
      float iconGap = 9.0F;
      float loadX = x + 170.0F - 8.0F - rightInset - 7.0F;
      float copyX = loadX - iconGap;
      float closeX = x + 8.0F + 3.0F;
      float closeW = infoW - 6.0F;
      return new ModuleGridSection.ColorPopupLayout(
         paletteX, paletteY, 110.0F, hueX, alphaX, 8.0F, infoY, presetsY, closeX, closeY, closeW, 16.0F, copyX, loadX
      );
   }

   private void renderColorPalette(GuiGraphicsExtractor context, ModuleGridSection.ColorPopupLayout layout, float alpha) {
      if (this.colorPopupSetting != null) {
         float hue = this.colorPopupSetting.getHue();
         int pure = 0xFF000000 | Color.HSBtoRGB(hue, 1.0F, 1.0F) & 16777215;
         QuadRadiusState radius = new QuadRadiusState(5.0F);
         RenderUtil.drawRoundRectGradientQuad(
            context,
            layout.paletteX,
            layout.paletteY,
            layout.paletteSize,
            layout.paletteSize,
            radius,
            ColorUtil.multiplyAlpha(ColorUtil.rgb(255, 255, 255), alpha),
            ColorUtil.multiplyAlpha(ColorUtil.rgb(255, 255, 255), alpha),
            ColorUtil.multiplyAlpha(pure, alpha),
            ColorUtil.multiplyAlpha(pure, alpha),
            1.0F
         );
         RenderUtil.drawRoundRectGradientQuad(
            context,
            layout.paletteX,
            layout.paletteY,
            layout.paletteSize,
            layout.paletteSize,
            radius,
            ColorUtil.multiplyAlpha(ColorUtil.rgba(0, 0, 0, 0), alpha),
            ColorUtil.multiplyAlpha(ColorUtil.rgb(0, 0, 0), alpha),
            ColorUtil.multiplyAlpha(ColorUtil.rgb(0, 0, 0), alpha),
            ColorUtil.multiplyAlpha(ColorUtil.rgba(0, 0, 0, 0), alpha),
            1.0F
         );
         RenderUtil.drawBorder(
            context,
            layout.paletteX,
            layout.paletteY,
            layout.paletteSize,
            layout.paletteSize,
            5.0F,
            ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, alpha),
            0.0F,
            0.5F,
            1.0F
         );
      }
   }

   private void renderColorBar(GuiGraphicsExtractor context, float x, float y, float width, float height, float alpha, boolean hueBar) {
      RenderUtil.drawRoundRect(context, x, y, width, height, 4.0F, ColorUtil.multiplyAlpha(ColorUtil.rgb(8, 8, 10), alpha));
      float innerX = x + 1.0F;
      float innerY = y + 1.0F;
      float innerW = width - 2.0F;
      float innerH = height - 2.0F;
      if (hueBar) {
         for (int row = 0; row < (int)innerH; row++) {
            float t = row / Math.max(1.0F, innerH - 1.0F);
            int color = 0xFF000000 | Color.HSBtoRGB(t, 1.0F, 1.0F) & 16777215;

            for (int col = 0; col < (int)innerW; col++) {
               if (this.isInsideRoundedRect(col + 0.5F, row + 0.5F, innerW, innerH, 3.0F)) {
                  context.fill(
                     Math.round(innerX + col),
                     Math.round(innerY + row),
                     Math.round(innerX + col + 1.0F),
                     Math.round(innerY + row + 1.0F),
                     ColorUtil.multiplyAlpha(color, alpha)
                  );
               }
            }
         }
      } else {
         int rgb = this.colorPopupSetting == null ? ClientColors.CATEGORY_ACCENT : this.colorPopupSetting.getColor() & 16777215;

         for (int row = 0; row < (int)innerH; row++) {
            float t = row / Math.max(1.0F, innerH - 1.0F);
            int color = ColorUtil.interpolate(ColorUtil.withAlpha(rgb, 255), ColorUtil.withAlpha(rgb, 0), t);

            for (int col = 0; col < (int)innerW; col++) {
               if (this.isInsideRoundedRect(col + 0.5F, row + 0.5F, innerW, innerH, 3.0F)) {
                  context.fill(
                     Math.round(innerX + col),
                     Math.round(innerY + row),
                     Math.round(innerX + col + 1.0F),
                     Math.round(innerY + row + 1.0F),
                     ColorUtil.multiplyAlpha(color, alpha)
                  );
               }
            }
         }
      }

      RenderUtil.drawBorder(context, x, y, width, height, 4.0F, ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, alpha), 0.0F, 0.5F, 1.0F);
   }

   private void renderColorHandles(GuiGraphicsExtractor context, ModuleGridSection.ColorPopupLayout layout, float alpha) {
      if (this.colorPopupSetting != null) {
         float paletteInset = 2.0F;
         float paletteInnerSize = layout.paletteSize - paletteInset * 2.0F;
         float paletteHandleRadius = 3.0F;
         float paletteHandleX = layout.paletteX + paletteInset + this.colorPopupSetting.getSaturation() * paletteInnerSize;
         float paletteHandleY = layout.paletteY + paletteInset + (1.0F - this.colorPopupSetting.getBrightness()) * paletteInnerSize;
         paletteHandleX = this.clamp(
            paletteHandleX, layout.paletteX + paletteInset + paletteHandleRadius, layout.paletteX + layout.paletteSize - paletteInset - paletteHandleRadius
         );
         paletteHandleY = this.clamp(
            paletteHandleY, layout.paletteY + paletteInset + paletteHandleRadius, layout.paletteY + layout.paletteSize - paletteInset - paletteHandleRadius
         );
         RenderUtil.drawBorder(
            context,
            paletteHandleX - 3.0F,
            paletteHandleY - 3.0F,
            6.0F,
            6.0F,
            3.0F,
            ColorUtil.multiplyAlpha(ColorUtil.rgb(255, 255, 255), alpha),
            0.0F,
            0.8F,
            1.0F
         );
         RenderUtil.drawRoundRect(
            context, paletteHandleX - 1.0F, paletteHandleY - 1.0F, 2.0F, 2.0F, 1.0F, ColorUtil.multiplyAlpha(ColorUtil.rgb(255, 255, 255), alpha)
         );
         float barInset = 2.0F;
         float barInnerHeight = layout.paletteSize - barInset * 2.0F;
         float markerHeight = 2.0F;
         float hueHandleY = layout.paletteY + barInset + this.colorPopupSetting.getHue() * barInnerHeight;
         float alphaHandleY = layout.paletteY + barInset + (1.0F - this.colorPopupSetting.getAlpha01()) * barInnerHeight;
         hueHandleY = this.clamp(
            hueHandleY, layout.paletteY + barInset + markerHeight * 0.5F, layout.paletteY + layout.paletteSize - barInset - markerHeight * 0.5F
         );
         alphaHandleY = this.clamp(
            alphaHandleY, layout.paletteY + barInset + markerHeight * 0.5F, layout.paletteY + layout.paletteSize - barInset - markerHeight * 0.5F
         );
         RenderUtil.drawRoundRect(
            context,
            layout.hueX,
            hueHandleY - markerHeight * 0.5F,
            layout.barW,
            markerHeight,
            1.0F,
            ColorUtil.multiplyAlpha(ColorUtil.rgb(255, 255, 255), alpha)
         );
         RenderUtil.drawRoundRect(
            context,
            layout.alphaX,
            alphaHandleY - markerHeight * 0.5F,
            layout.barW,
            markerHeight,
            1.0F,
            ColorUtil.multiplyAlpha(ColorUtil.rgb(255, 255, 255), alpha)
         );
      }
   }

   private void renderColorInfo(GuiGraphicsExtractor context, ModuleGridSection.ColorPopupLayout layout, float alpha) {
      if (this.colorPopupSetting != null) {
         int boxBg = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_BACKGROUND, alpha);
         int border = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, alpha);
         RenderUtil.drawRoundRect(context, layout.paletteX, layout.infoY, 146.0F, 14.0F, 4.0F, boxBg);
         RenderUtil.drawBorder(context, layout.paletteX, layout.infoY, 146.0F, 14.0F, 4.0F, border, 0.0F, 0.5F, 1.0F);
         int color = this.colorPopupSetting.getColor();
         String rgba = ColorUtil.red(color)
            + "  "
            + ColorUtil.green(color)
            + "  "
            + ColorUtil.blue(color)
            + "  "
            + Math.round(this.colorPopupSetting.getAlpha01() * 100.0F)
            + "%";
         RenderUtil.drawImage(
            context,
            "textures/gui/icons/test_tube.png",
            layout.paletteX + 6.0F,
            layout.infoY + 3.5F,
            7.0F,
            7.0F,
            0.0F,
            ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha),
            0.0F
         );
         RenderUtil.drawText(
            context, "RGBA", layout.paletteX + 16.0F, layout.infoY + 4.5F, ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha), GFonts.BOLD, 6
         );
         RenderUtil.drawText(
            context, rgba, layout.paletteX + 41.0F, layout.infoY + 4.5F, ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha), GFonts.REGULAR, 6
         );
         double uiMouseX = Minecraft.getInstance().mouseHandler.xpos() / Minecraft.getInstance().getWindow().getGuiScale();
         double uiMouseY = Minecraft.getInstance().mouseHandler.ypos() / Minecraft.getInstance().getWindow().getGuiScale();
         boolean copyHovered = this.isHovered(uiMouseX, uiMouseY, layout.copyX - 1.0F, layout.infoY + 2.5F, 9.0F, 9.0F);
         boolean loadHovered = this.isHovered(uiMouseX, uiMouseY, layout.loadX - 1.0F, layout.infoY + 2.5F, 9.0F, 9.0F);
         int copyColor = ColorUtil.multiplyAlpha(copyHovered ? ClientColors.CATEGORY_ACCENT : ClientColors.TEXT_PRIMARY, alpha);
         int loadColor = ColorUtil.multiplyAlpha(loadHovered ? ClientColors.CATEGORY_ACCENT : ClientColors.TEXT_PRIMARY, alpha);
         float iconY = layout.infoY + 3.0F;
         if (copyHovered) {
            RenderUtil.drawRoundRect(
               context, layout.copyX - 1.5F, iconY - 1.0F, 10.0F, 10.0F, 2.5F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, alpha * 0.18F)
            );
         }

         if (loadHovered) {
            RenderUtil.drawRoundRect(
               context, layout.loadX - 1.5F, iconY - 1.0F, 10.0F, 10.0F, 2.5F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, alpha * 0.18F)
            );
         }

         RenderUtil.drawImage(context, "textures/gui/icons/copy.png", layout.copyX, iconY, 7.0F, 7.0F, 0.0F, copyColor, 0.0F);
         RenderUtil.drawImage(context, "textures/gui/icons/load.png", layout.loadX, iconY, 7.0F, 7.0F, 0.0F, loadColor, 0.0F);
      }
   }

   private void renderColorPresets(GuiGraphicsExtractor context, ModuleGridSection.ColorPopupLayout layout, int mouseX, int mouseY, float alpha) {
      float startX = layout.paletteX + 2.0F;

      for (int i = 0; i < COLOR_PRESETS.length; i++) {
         float px = startX + i * 11.0F;
         float py = layout.presetsY;
         boolean hovered = this.isHovered(mouseX, mouseY, px, py, 7.0F, 7.0F);
         float size = hovered ? 7.8F : 7.0F;
         float offset = (7.0F - size) * 0.5F;
         int presetColor = ColorUtil.withAlpha(COLOR_PRESETS[i], 255);
         RenderUtil.drawCircle(context, px + offset, py + offset, size, ColorUtil.multiplyAlpha(presetColor, alpha));
         if (hovered) {
            RenderUtil.drawBorder(
               context,
               px + offset - 1.0F,
               py + offset - 1.0F,
               size + 2.0F,
               size + 2.0F,
               6.0F,
               ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha * 0.75F),
               0.0F,
               0.6F,
               1.0F
            );
         }
      }
   }

   private int hitColorPresetIndex(ModuleGridSection.ColorPopupLayout layout, double mouseX, double mouseY) {
      float startX = layout.paletteX + 2.0F;

      for (int i = 0; i < COLOR_PRESETS.length; i++) {
         float px = startX + i * 11.0F;
         float py = layout.presetsY;
         if (this.isHovered(mouseX, mouseY, px, py, 7.0F, 7.0F)) {
            return i;
         }
      }

      return -1;
   }

   private void applyColorPreset(int presetIndex) {
      if (this.colorPopupSetting != null && presetIndex >= 0 && presetIndex < COLOR_PRESETS.length) {
         int nextColor = ColorUtil.withAlpha(COLOR_PRESETS[presetIndex], Math.round(this.colorPopupSetting.getAlpha01() * 255.0F));
         this.colorPopupSetting.setColor(nextColor);
         ClientTheme.syncSetting(this.colorPopupSetting);
      }
   }

   private void updateColorDrag(int mouseX, int mouseY, float panelX, float panelY, float panelWidth, float panelHeight) {
      if (this.isColorPopupActive() && this.colorPopupOpen && this.colorDragMode != ModuleGridSection.ColorDragMode.NONE) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.getWindow() != null) {
            long handle = mc.getWindow().handle();
            if (GLFW.glfwGetMouseButton(handle, 0) != 1) {
               this.colorDragMode = ModuleGridSection.ColorDragMode.NONE;
            } else {
               float[] popupRect = this.getColorPopupRect();
               this.updateColorFromMouse(this.colorDragMode, mouseX, mouseY, this.getColorPopupLayout(popupRect[0], popupRect[1]));
            }
         } else {
            this.colorDragMode = ModuleGridSection.ColorDragMode.NONE;
         }
      }
   }

   private void updateColorFromMouse(ModuleGridSection.ColorDragMode dragMode, double mouseX, double mouseY, ModuleGridSection.ColorPopupLayout layout) {
      if (this.colorPopupSetting != null) {
         if (dragMode == ModuleGridSection.ColorDragMode.PALETTE) {
            float saturation = this.clamp((float)((mouseX - layout.paletteX) / layout.paletteSize), 0.0F, 1.0F);
            float brightness = 1.0F - this.clamp((float)((mouseY - layout.paletteY) / layout.paletteSize), 0.0F, 1.0F);
            this.colorPopupSetting.setSaturation(saturation);
            this.colorPopupSetting.setBrightness(brightness);
            ClientTheme.syncSetting(this.colorPopupSetting);
         } else if (dragMode == ModuleGridSection.ColorDragMode.HUE) {
            float hue = this.clamp((float)((mouseY - layout.paletteY) / layout.paletteSize), 0.0F, 1.0F);
            this.colorPopupSetting.setHue(hue);
            ClientTheme.syncSetting(this.colorPopupSetting);
         } else if (dragMode == ModuleGridSection.ColorDragMode.ALPHA) {
            float alpha = 1.0F - this.clamp((float)((mouseY - layout.paletteY) / layout.paletteSize), 0.0F, 1.0F);
            this.colorPopupSetting.setAlpha01(alpha);
            ClientTheme.syncSetting(this.colorPopupSetting);
         }
      }
   }

   private void copyCurrentColor() {
      if (this.colorPopupSetting != null) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.keyboardHandler != null) {
            mc.keyboardHandler.setClipboard(String.format("#%08X", this.colorPopupSetting.getColor()));
         }
      }
   }

   private void pasteCurrentColor() {
      if (this.colorPopupSetting != null) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.keyboardHandler != null) {
            String text = mc.keyboardHandler.getClipboard();
            Integer parsed = this.parseColorText(text);
            if (parsed != null) {
               this.colorPopupSetting.setColor(parsed);
               ClientTheme.syncSetting(this.colorPopupSetting);
            }
         }
      }
   }

   private Integer parseColorText(String text) {
      if (text == null) {
         return null;
      }

      String normalized = text.trim().replace("0x", "").replace("0X", "").replace("#", "").replace("_", "").replace(" ", "");
      if (normalized.length() == 6) {
         normalized = "FF" + normalized;
      }

      if (normalized.length() != 8) {
         return null;
      }

      try {
         return (int)Long.parseLong(normalized, 16);
      } catch (NumberFormatException ignored) {
         return null;
      }
   }

   private boolean isInsideRoundedRect(float x, float y, float width, float height, float radius) {
      float r = Math.max(0.0F, Math.min(radius, Math.min(width, height) * 0.5F));
      if (r <= 0.0F) {
         return x >= 0.0F && x <= width && y >= 0.0F && y <= height;
      }

      if (x >= r && x <= width - r) {
         return y >= 0.0F && y <= height;
      }

      if (y >= r && y <= height - r) {
         return x >= 0.0F && x <= width;
      }

      float cx = x < r ? r : width - r;
      float cy = y < r ? r : height - r;
      float dx = x - cx;
      float dy = y - cy;
      return dx * dx + dy * dy <= r * r;
   }

   private float getModuleCardHeight(Module module, float cardWidth, UiAnimationEngine animation) {
      List<Setting> renderSettings = this.getRenderableSettings(module);
      if (renderSettings.isEmpty()) {
         return 25.0F;
      }

      float titleOffset = (25.0F + RenderUtil.getTextHeight(GFonts.MEDIUM, 9)) / 2.0F - 8.0F;
      float settingXOffset = 8.0F;
      float settingW = Math.max(16.0F, cardWidth - 8.0F - settingXOffset);
      float settingHeight = 0.0F;

      for (int i = 0; i < renderSettings.size(); i++) {
         Setting setting = renderSettings.get(i);
         if (setting instanceof BooleanSetting booleanSetting) {
            settingHeight += BooleanSettingRenderer.measureHeight(booleanSetting, settingW);
         } else if (setting instanceof SliderSetting sliderSetting) {
            settingHeight += SliderSettingRenderer.measureHeight(sliderSetting, settingW);
         } else if (setting instanceof ColorSetting colorSetting) {
            settingHeight += ColorSettingRenderer.measureHeight(colorSetting, settingW);
         } else if (setting instanceof SelectSetting selectSetting) {
            String settingKey = module.getClass().getName() + ":" + selectSetting.getName();
            boolean expanded = this.selectExpanded.getOrDefault(settingKey, false);
            float expandProgress = animation == null
               ? this.getSelectExpandProgress(settingKey, expanded)
               : animation.animate01("setting:select:open:" + settingKey, expanded ? 1.0F : 0.0F, 18.0F, expanded ? 1.0F : 0.0F);
            this.selectExpandProgress.put(settingKey, expandProgress);
            settingHeight += SelectSettingRenderer.measureHeight(selectSetting, settingW, expandProgress);
         } else if (setting instanceof MultiSelectSetting multiSelectSetting) {
            String settingKey = module.getClass().getName() + ":" + multiSelectSetting.getName();
            boolean expanded = this.multiSelectExpanded.getOrDefault(settingKey, false);
            float expandProgress = animation == null
               ? this.getMultiSelectExpandProgress(settingKey, expanded)
               : animation.animate01("setting:multi:open:" + settingKey, expanded ? 1.0F : 0.0F, 18.0F, expanded ? 1.0F : 0.0F);
            this.multiSelectExpandProgress.put(settingKey, expandProgress);
            settingHeight += MultiSelectSettingRenderer.measureHeight(multiSelectSetting, settingW, expandProgress);
         }

         if (i < renderSettings.size() - 1) {
            Setting nextSetting = renderSettings.get(i + 1);
            settingHeight += this.getSettingsGap(setting, nextSetting);
         }
      }

      return Math.max(25.0F, titleOffset + titleOffset + 4.0F + settingHeight + 4.0F);
   }

   private float getSettingsGap(Setting current, Setting next) {
      if (next == null) {
         return 0.0F;
      } else {
         return (current instanceof SelectSetting || current instanceof MultiSelectSetting) && next instanceof BooleanSetting ? 5.0F : 2.0F;
      }
   }

   private float getSelectExpandProgress(String settingKey, boolean expanded) {
      return this.selectExpandProgress.getOrDefault(settingKey, expanded ? 1.0F : 0.0F);
   }

   private float getMultiSelectExpandProgress(String settingKey, boolean expanded) {
      return this.multiSelectExpandProgress.getOrDefault(settingKey, expanded ? 1.0F : 0.0F);
   }

   private float getModuleBindTargetWidth(String bindText) {
      float bindTextW = RenderUtil.getTextWidth(bindText, GFonts.BOLD, 5);
      return 11.5F + bindTextW + 3.0F - 1.0F;
   }

   private float getPopupKeyTargetWidth(String keyText) {
      float keyTextW = RenderUtil.getTextWidth(keyText, GFonts.BOLD, 5);
      return 13.5F + keyTextW + 5.0F;
   }

   private float animateWidth(UiAnimationEngine animation, Map<String, Float> widths, String key, float targetWidth, float speed) {
      if (!widths.containsKey(key)) {
         animation.snap(key, targetWidth);
         widths.put(key, targetWidth);
         return targetWidth;
      }

      float current = widths.getOrDefault(key, targetWidth);
      float next = animation.animate(key, targetWidth, speed, current);
      if (Math.abs(next - targetWidth) <= 0.06F) {
         next = targetWidth;
         animation.snap(key, targetWidth);
      }

      widths.put(key, next);
      return next;
   }

   private float animateTextRevealAfterWidth(
      UiAnimationEngine animation,
      Map<String, String> textMap,
      Map<String, Boolean> unlockedMap,
      String stateKey,
      String alphaKey,
      String nextText,
      float currentWidth,
      float targetWidth
   ) {
      String prevText = textMap.get(stateKey);
      if (prevText == null) {
         textMap.put(stateKey, nextText);
         unlockedMap.put(stateKey, true);
         animation.snap(alphaKey, 1.0F);
         return 1.0F;
      }

      if (!prevText.equals(nextText)) {
         textMap.put(stateKey, nextText);
         unlockedMap.put(stateKey, false);
         animation.snap(alphaKey, 0.0F);
         return 0.0F;
      }

      boolean unlocked = unlockedMap.getOrDefault(stateKey, false);
      if (!unlocked && Math.abs(currentWidth - targetWidth) <= 0.12F) {
         unlocked = true;
         unlockedMap.put(stateKey, true);
      }

      if (!unlocked) {
         animation.snap(alphaKey, 0.0F);
         return 0.0F;
      } else {
         return animation.animate01(alphaKey, 1.0F, 16.0F, 0.0F);
      }
   }

   private void drawTextWithRightFade(
      GuiGraphicsExtractor context,
      String text,
      float x,
      float y,
      int color,
      GFonts font,
      int fontSize,
      float maxWidth,
      float fadeWidth,
      float clipTop,
      float clipHeight
   ) {
      if (text != null && !text.isEmpty() && !(maxWidth <= 0.0F)) {
         float fullWidth = RenderUtil.getTextWidth(text, font, fontSize);
         if (fullWidth <= maxWidth) {
            RenderUtil.drawText(context, text, x, y - 0.5F, color, font, fontSize);
         } else {
            int scissorTop = (int)Math.floor(clipTop);
            int scissorBottom = (int)Math.ceil(clipTop + clipHeight);
            int x1 = (int)Math.floor(x);
            int x2 = (int)Math.ceil(x + maxWidth);
            context.enableScissor(x1, scissorTop, x2, scissorBottom);
            RenderUtil.drawTextFadeRight(context, text, x, y - 0.5F, color, font, fontSize, maxWidth, fadeWidth);
            context.disableScissor();
         }
      }
   }

   private void drawTextClipped(
      GuiGraphicsExtractor context,
      String text,
      float x,
      float y,
      int color,
      GFonts font,
      int fontSize,
      float clipX,
      float clipWidth,
      float clipTop,
      float clipHeight
   ) {
      if (text != null && !text.isEmpty() && !(clipWidth <= 0.0F) && ColorUtil.alpha(color) > 0) {
         int x1 = (int)Math.floor(clipX);
         int y1 = (int)Math.floor(clipTop);
         int x2 = (int)Math.ceil(clipX + clipWidth);
         int y2 = (int)Math.ceil(clipTop + clipHeight);
         context.enableScissor(x1, y1, x2, y2);
         RenderUtil.drawText(context, text, x, y, color, font, fontSize);
         context.disableScissor();
      }
   }

   private void onCapturePress(int code) {
      this.capturePressedCodes.add(code);
      if (this.captureSelectedCodes.size() < 3 || this.captureSelectedCodes.contains(code)) {
         this.captureSelectedCodes.add(code);
      }
   }

   private void onCaptureRelease(int code) {
      this.capturePressedCodes.remove(code);
      if (this.capturePressedCodes.isEmpty() && !this.captureSelectedCodes.isEmpty()) {
         BindData activePopupBind = this.getActivePopupBind();
         if (activePopupBind != null) {
            activePopupBind.setKeys(new LinkedHashSet<>(this.captureSelectedCodes));
         }

         this.bindCapture = false;
         this.clearCaptureState();
      }
   }

   private void clearCaptureState() {
      this.capturePressedCodes.clear();
      this.captureSelectedCodes.clear();
   }

   private float updateModuleTitleShift(UiAnimationEngine animation, String moduleKey, float titleMix, float shiftMax) {
      String animKey = "module:title:shift:" + moduleKey;
      float current = this.moduleTitleScrollValues.getOrDefault(moduleKey, 0.0F);
      boolean forward = this.moduleTitleScrollForward.getOrDefault(moduleKey, true);
      float edgeEpsilon = 0.0015F;
      if (shiftMax <= 0.5F) {
         this.moduleTitleScrollValues.put(moduleKey, 0.0F);
         this.moduleTitleScrollForward.put(moduleKey, true);
         animation.snap(animKey, 0.0F);
         return 0.0F;
      }

      if (titleMix <= 0.02F) {
         float back = animation.animate(animKey, 0.0F, 18.0F, current);
         this.moduleTitleScrollValues.put(moduleKey, back);
         this.moduleTitleScrollForward.put(moduleKey, true);
         return back;
      }

      float target = forward ? shiftMax : 0.0F;
      float next = animation.animate(animKey, target, 4.8F, current);
      if (forward && next >= shiftMax - 0.0015F) {
         next = shiftMax;
         this.moduleTitleScrollForward.put(moduleKey, false);
      } else if (!forward && next <= 0.0015F) {
         next = 0.0F;
         this.moduleTitleScrollForward.put(moduleKey, true);
      } else {
         this.moduleTitleScrollForward.put(moduleKey, forward);
      }

      this.moduleTitleScrollValues.put(moduleKey, next);
      return next;
   }

   private float smootherStep01(float t) {
      float x = ColorUtil.clamp01(t);
      return x * x * x * (x * (x * 6.0F - 15.0F) + 10.0F);
   }

   private float computeMaxScroll(List<Module> modules, float cardWidth, float viewportHeight, UiAnimationEngine animation) {
      float[] columnBottom = new float[2];

      for (int c = 0; c < 2; c++) {
         columnBottom[c] = 0.0F;
      }

      for (int i = 0; i < modules.size(); i++) {
         int col = i % 2;
         Module module = modules.get(i);
         float moduleCardHeight = this.getModuleCardHeight(module, cardWidth, animation);
         columnBottom[col] += moduleCardHeight + 8.0F;
      }

      float contentHeight = 0.0F;

      for (int c = 0; c < 2; c++) {
         contentHeight = Math.max(contentHeight, columnBottom[c]);
      }

      if (contentHeight > 0.0F) {
         contentHeight -= 8.0F;
      }

      return Math.max(0.0F, contentHeight - viewportHeight);
   }

   private boolean isHovered(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   private List<Setting> getRenderableSettings(Module module) {
      List<Setting> result = new ArrayList<>();

      for (Setting setting : module.getSettings()) {
         if (setting.isVisible()
            && (
               setting instanceof BooleanSetting
                  || setting instanceof SliderSetting
                  || setting instanceof ColorSetting
                  || setting instanceof SelectSetting
                  || setting instanceof MultiSelectSetting
            )) {
            result.add(setting);
         }
      }

      return result;
   }

   private void updateSliderDrag(int mouseX) {
      if (this.draggingSliderModule != null && this.draggingSliderSetting != null) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.getWindow() != null) {
            long handle = mc.getWindow().handle();
            if (GLFW.glfwGetMouseButton(handle, 0) != 1) {
               this.draggingSliderModule = null;
               this.draggingSliderSetting = null;
            } else {
               SliderSettingRenderer.setFromMouse(this.draggingSliderSetting, this.draggingSliderX, this.draggingSliderY, this.draggingSliderW, mouseX);
            }
         } else {
            this.draggingSliderModule = null;
            this.draggingSliderSetting = null;
         }
      }
   }

   private String formatBindText(Set<Integer> keys) {
      if (keys != null && !keys.isEmpty()) {
         List<String> parts = new ArrayList<>();

         for (int code : keys) {
            parts.add(this.formatBindCode(code));
            if (parts.size() >= 3) {
               break;
            }
         }

         return String.join(" + ", parts);
      } else {
         return "None";
      }
   }

   private String formatBindCode(int code) {
      if (Havoc.isMouseBindCode(code)) {
         return "M" + (Havoc.bindCodeToMouseButton(code) + 1);
      }

      return switch (code) {
         case 32 -> "SPACE";
         case 256 -> "ESC";
         case 257 -> "ENTER";
         case 258 -> "TAB";
         case 259 -> "BACKSPACE";
         case 261 -> "DELETE";
         case 340 -> "L SHIFT";
         case 341 -> "L CTRL";
         case 342 -> "L ALT";
         case 344 -> "R SHIFT";
         case 345 -> "R CTRL";
         case 346 -> "R ALT";
         default -> {
            String name = GLFW.glfwGetKeyName(code, 0);
            yield name != null && !name.isBlank() ? name.toUpperCase() : "KEY " + code;
         }
      };
   }

   private Category toCategory(CategoryBarSection.TopTab tab) {
      return switch (tab) {
         case COMBAT -> Category.COMBAT;
         case MOVEMENT -> Category.MOVEMENT;
         case PLAYER -> Category.PLAYER;
         case VISUALS -> Category.VISUALS;
         case MISC -> Category.MISC;
         case CONFIGS, THEMES, COSMETICS -> null;
      };
   }

   private List<Module> visibleModules(Category category) {
      return Havoc.getInstance().getModuleManager().getModules(category).stream().filter(module -> !(module instanceof ItemReplacer)).toList();
   }

   private CategoryBarSection.TopTab toTab(Category category) {
      if (category == null) {
         return null;
      }

      return switch (category) {
         case COMBAT -> CategoryBarSection.TopTab.COMBAT;
         case MOVEMENT -> CategoryBarSection.TopTab.MOVEMENT;
         case PLAYER -> CategoryBarSection.TopTab.PLAYER;
         case VISUALS -> CategoryBarSection.TopTab.VISUALS;
         case MISC -> CategoryBarSection.TopTab.MISC;
      };
   }

   public Map<String, Float> captureTabScrollState() {
      Map<String, Float> state = new HashMap<>();

      for (Entry<CategoryBarSection.TopTab, Float> entry : this.tabScrollTargets.entrySet()) {
         state.put(entry.getKey().name(), entry.getValue());
      }

      return state;
   }

   public Map<String, Boolean> captureSelectExpandedState() {
      return new HashMap<>(this.selectExpanded);
   }

   public Map<String, Boolean> captureMultiSelectExpandedState() {
      return new HashMap<>(this.multiSelectExpanded);
   }

   public void applyUiState(Map<String, Float> scrollByTab, Map<String, Boolean> selectExpandedState, Map<String, Boolean> multiSelectExpandedState) {
      if (scrollByTab != null) {
         this.tabScrollTargets.clear();
         this.tabScrollValues.clear();

         for (Entry<String, Float> entry : scrollByTab.entrySet()) {
            try {
               CategoryBarSection.TopTab tab = CategoryBarSection.TopTab.valueOf(entry.getKey());
               float value = Math.max(0.0F, entry.getValue() == null ? 0.0F : entry.getValue());
               this.tabScrollTargets.put(tab, value);
               this.tabScrollValues.put(tab, value);
            } catch (IllegalArgumentException var8) {
            }
         }
      }

      if (selectExpandedState != null) {
         this.selectExpanded.clear();
         this.selectExpanded.putAll(selectExpandedState);
      }

      if (multiSelectExpandedState != null) {
         this.multiSelectExpanded.clear();
         this.multiSelectExpanded.putAll(multiSelectExpandedState);
      }
   }

   private enum ColorDragMode {
      NONE,
      PALETTE,
      HUE,
      ALPHA;
   }

   private record ColorPopupLayout(
      float paletteX,
      float paletteY,
      float paletteSize,
      float hueX,
      float alphaX,
      float barW,
      float infoY,
      float presetsY,
      float closeX,
      float closeY,
      float closeW,
      float closeH,
      float copyX,
      float loadX
   ) {
   }

   private record ScrollbarState(float trackX, float trackY, float trackW, float trackH, float thumbY, float thumbH, float maxScroll) {
   }
}
