package havoc.screen.menu;

import havoc.Havoc;
import havoc.config.ConfigManager;
import havoc.module.base.Module;
import havoc.module.impl.visuals.Interface;
import havoc.module.impl.visuals.Menu;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.screen.menu.sections.CategoryBarSection;
import havoc.screen.menu.sections.ConfigsSection;
import havoc.screen.menu.sections.CosmeticsSection;
import havoc.screen.menu.sections.ModuleGridSection;
import havoc.screen.menu.sections.ThemeSection;
import havoc.screen.screen.HudVisualStyle;
import havoc.utilites.animation.AnimatedValue;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ClientTheme;
import havoc.utilites.color.ColorUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

public final class MenuScreen extends Screen {
   private static final float PANEL_WIDTH = 396.0F;
   private static final float PANEL_HEIGHT = 300.0F;
   private final Screen parentScreen;
   private final UiAnimationEngine animation = new UiAnimationEngine();
   private final CategoryBarSection categoryBarSection = new CategoryBarSection();
   private final ModuleGridSection moduleGridSection = new ModuleGridSection();
   private final ConfigsSection configsSection = new ConfigsSection();
   private final ThemeSection themeSection = new ThemeSection();
   private final CosmeticsSection cosmeticsSection = new CosmeticsSection();
   private boolean closing;
   private boolean closeApplied;
   private boolean searchOpen;
   private boolean searchFocused;
   private String searchQuery = "";
   private final AnimatedValue searchAnim = new AnimatedValue();

   public MenuScreen(Screen parentScreen) {
      super(Component.literal("Havoc Menu"));
      this.parentScreen = parentScreen;
      this.applySavedMenuState();
   }

   public void requestClose() {
      if (this.moduleGridSection.isPopupOpen()) {
         this.moduleGridSection.requestClosePopup();
      } else if (!this.closing && this.parentScreen == null && this.minecraft != null && this.minecraft.player != null) {
         MenuCloseAnimation.start(this);
         this.applyClose();
         this.minecraft.mouseHandler.grabMouse();
      } else {
         this.closing = true;
      }
   }

   public void renderSpatialAfterimage(GuiGraphicsExtractor context, float alpha, float yawDelta, float pitchDelta) {
      if (!(alpha <= 0.01F) && this.minecraft != null) {
         int fps = Math.max(1, this.minecraft.getFps());
         this.animation.beginFrame(fps);
         float guiScale = this.getInterfaceScale();
         float angle = Math.min(1.0F, Math.abs(yawDelta) / 105.0F + Math.abs(pitchDelta) / 80.0F);
         float perspectiveScale = 1.0F - angle * 0.11F;
         float panelWidth = this.getPanelWidth();
         float panelHeight = this.getPanelHeight();
         float spatialScale = guiScale * perspectiveScale;
         float maxShiftX = Math.max(0.0F, (this.width - panelWidth * spatialScale) * 0.5F - 1.0F);
         float maxShiftY = Math.max(0.0F, (this.height - panelHeight * spatialScale) * 0.5F - 1.0F);
         float shiftX = Math.clamp(-yawDelta * 4.25F, -maxShiftX, maxShiftX);
         float shiftY = Math.clamp(-pitchDelta * 3.65F, -maxShiftY, maxShiftY);
         Matrix3x2fStack matrices = context.pose();
         matrices.pushMatrix();
         matrices.translate(this.width * 0.5F + shiftX, this.height * 0.5F + shiftY);
         matrices.scale(spatialScale, spatialScale);
         matrices.translate(-this.width * 0.5F, -this.height * 0.5F);

         try {
            float panelX = this.getPanelX(panelWidth);
            float panelY = (this.height - panelHeight) * 0.5F;
            float contentX = panelX + 108.0F;
            float contentWidth = panelWidth - 108.0F;
            HudVisualStyle.drawMenuPanel(context, panelX, panelY, panelWidth, panelHeight, 108.0F, alpha);
            boolean configsActive = this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.CONFIGS;
            boolean themesActive = this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.THEMES;
            boolean cosmeticsActive = this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.COSMETICS;
            this.configsSection.setActive(configsActive);
            if (configsActive) {
               this.configsSection.render(context, this.animation, contentX, panelY, contentWidth, panelHeight, -10000, -10000, alpha);
            } else if (themesActive) {
               this.themeSection.render(context, this.animation, contentX, panelY, contentWidth, panelHeight, -10000, -10000, alpha);
            } else if (cosmeticsActive) {
               this.cosmeticsSection.render(context, this.animation, contentX, panelY, contentWidth, panelHeight, -10000, -10000, alpha);
            } else {
               this.moduleGridSection
                  .render(context, this.animation, contentX, panelY, contentWidth, panelHeight, -10000, -10000, alpha, this.categoryBarSection.getActiveTab());
            }

            this.categoryBarSection.render(context, this.animation, panelX, panelY, panelHeight, -10000, -10000, alpha);
            if (!configsActive) {
               this.moduleGridSection.renderOverlay(context, this.animation, -10000, -10000, alpha);
            }
         } finally {
            matrices.popMatrix();
         }
      }
   }

   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      ClientTheme.updateDynamicColors();
      int fps = this.minecraft != null ? this.minecraft.getFps() : 60;
      if (fps <= 0) {
         fps = 60;
      }

      this.animation.beginFrame(fps);
      if (this.minecraft != null && this.minecraft.level == null) {
         this.extractPanorama(context, delta);
      }

      float guiScale = this.getInterfaceScale();
      int logicalMouseX = (int)Math.round(this.toLogicalX(mouseX, guiScale));
      int logicalMouseY = (int)Math.round(this.toLogicalY(mouseY, guiScale));
      Matrix3x2fStack matrices = context.pose();
      matrices.pushMatrix();
      matrices.translate(this.width * 0.5F, this.height * 0.5F);
      matrices.scale(guiScale, guiScale);
      matrices.translate(-this.width * 0.5F, -this.height * 0.5F);

      try {
         float visibility = this.animation.animate01("menu:visibility", this.closing ? 0.0F : 1.0F, 12.0F, this.closing ? 1.0F : 0.0F);
         float panelVisibility;
         float categoriesVisibility;
         if (this.closing) {
            float closeProgress = 1.0F - visibility;
            categoriesVisibility = 1.0F - this.clamp01(closeProgress / 0.62F);
            panelVisibility = 1.0F - this.clamp01((closeProgress - 0.52F) / 0.48F);
         } else {
            float openProgress = visibility;
            panelVisibility = this.clamp01(openProgress / 0.48F);
            categoriesVisibility = this.clamp01((openProgress - 0.32F) / 0.68F);
         }

         panelVisibility = this.smoothStep(panelVisibility);
         categoriesVisibility = this.smoothStep(categoriesVisibility);
         if (!this.closing || this.closeApplied || !(panelVisibility <= 0.02F)) {
            float panelWidth = this.getPanelWidth();
            float panelHeight = this.getPanelHeight();
            float panelX = this.getPanelX(panelWidth);
            float panelYBase = (this.height - panelHeight) / 2.0F;
            float panelYOffset = (1.0F - panelVisibility) * 14.0F;
            float panelY = panelYBase + panelYOffset;
            float contentX = panelX + 108.0F;
            float contentWidth = panelWidth - 108.0F;
            HudVisualStyle.drawMenuPanel(context, panelX, panelY, panelWidth, panelHeight, 108.0F, panelVisibility);
            boolean configsActive = this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.CONFIGS;
            boolean themesActive = this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.THEMES;
            boolean cosmeticsActive = this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.COSMETICS;
            this.configsSection.setActive(configsActive);
            if (configsActive) {
               this.configsSection
                  .render(context, this.animation, contentX, panelY, contentWidth, panelHeight, logicalMouseX, logicalMouseY, categoriesVisibility);
            } else if (themesActive) {
               this.themeSection
                  .render(context, this.animation, contentX, panelY, contentWidth, panelHeight, logicalMouseX, logicalMouseY, categoriesVisibility);
            } else if (cosmeticsActive) {
               this.cosmeticsSection
                  .render(context, this.animation, contentX, panelY, contentWidth, panelHeight, logicalMouseX, logicalMouseY, categoriesVisibility);
            } else {
               this.moduleGridSection
                  .render(
                     context,
                     this.animation,
                     contentX,
                     panelY,
                     contentWidth,
                     panelHeight,
                     logicalMouseX,
                     logicalMouseY,
                     categoriesVisibility,
                     this.categoryBarSection.getActiveTab()
                  );
            }

            this.categoryBarSection.render(context, this.animation, panelX, panelY, panelHeight, logicalMouseX, logicalMouseY, categoriesVisibility);
            if (!configsActive) {
               this.moduleGridSection.renderOverlay(context, this.animation, logicalMouseX, logicalMouseY, categoriesVisibility);
            }

            this.renderSearchHint(context, panelX, panelWidth, panelY + panelHeight, categoriesVisibility);
            this.renderSearchOverlay(context);
            this.persistMenuState();
            Havoc.getInstance().getConfigManager().flushAutoSave();
            return;
         }

         this.applyClose();
      } finally {
         matrices.popMatrix();
      }
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
      MouseButtonEvent logicalClick = this.toLogicalClick(click);
      if (this.searchOpen) {
         return this.handleSearchClick(logicalClick);
      }

      float panelWidth = this.getPanelWidth();
      float panelHeight = this.getPanelHeight();
      float panelX = this.getPanelX(panelWidth);
      float panelY = (this.height - panelHeight) / 2.0F;
      float contentX = panelX + 108.0F;
      float contentWidth = panelWidth - 108.0F;
      if (this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.CONFIGS) {
         if (this.configsSection.handleClick(logicalClick, contentX, panelY, contentWidth, panelHeight)) {
            return true;
         }
      } else if (this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.THEMES) {
         if (this.moduleGridSection.handleClick(logicalClick, contentX, panelY, contentWidth, panelHeight, this.categoryBarSection.getActiveTab())) {
            return true;
         }

         if (this.themeSection.handleClick(logicalClick, contentX, panelY, contentWidth, panelHeight, this.moduleGridSection)) {
            return true;
         }
      } else if (this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.COSMETICS) {
         if (this.cosmeticsSection.handleClick(logicalClick, contentX, panelY, contentWidth, panelHeight)) {
            return true;
         }
      } else if (this.moduleGridSection.handleClick(logicalClick, contentX, panelY, contentWidth, panelHeight, this.categoryBarSection.getActiveTab())) {
         return true;
      }

      if (this.categoryBarSection.handleClick(logicalClick, panelX, panelY, panelHeight)) {
         return true;
      } else {
         return logicalClick.button() == 0 ? true : super.mouseClicked(click, doubleClick);
      }
   }

   public boolean keyPressed(KeyEvent key) {
      if (this.isCtrlDown() && key.key() == 70) {
         this.searchOpen = true;
         this.searchFocused = false;
         this.searchQuery = "";
         this.searchAnim.setValue(0.0F);
         return true;
      } else if (this.searchOpen) {
         return this.handleSearchKeyPressed(key);
      } else if (this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.THEMES && this.themeSection.handleKeyPressed(key)) {
         return true;
      } else if (this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.CONFIGS) {
         return this.configsSection.handleKeyPressed(key) ? true : super.keyPressed(key);
      } else {
         Interface interfaceModule = Havoc.getInstance().getModuleManager().getModule(Interface.class);
         if (interfaceModule != null && interfaceModule.handleHudEditorKeyPressed(key)) {
            return true;
         } else {
            return this.moduleGridSection.handleKeyPressed(key) ? true : super.keyPressed(key);
         }
      }
   }

   public boolean keyReleased(KeyEvent key) {
      if (this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.CONFIGS) {
         return super.keyReleased(key);
      } else {
         return this.moduleGridSection.handleKeyReleased(key) ? true : super.keyReleased(key);
      }
   }

   public boolean mouseReleased(MouseButtonEvent click) {
      MouseButtonEvent logicalClick = this.toLogicalClick(click);
      if (this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.CONFIGS) {
         return this.configsSection.handleMouseReleased(logicalClick.button()) ? true : super.mouseReleased(click);
      } else {
         return this.moduleGridSection.handleMouseReleased(logicalClick.button()) ? true : super.mouseReleased(click);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      float scale = this.getInterfaceScale();
      double logicalMouseX = this.toLogicalX(mouseX, scale);
      double logicalMouseY = this.toLogicalY(mouseY, scale);
      float panelWidth = this.getPanelWidth();
      float panelHeight = this.getPanelHeight();
      float panelX = this.getPanelX(panelWidth);
      float panelY = (this.height - panelHeight) / 2.0F;
      float contentX = panelX + 108.0F;
      float contentWidth = panelWidth - 108.0F;
      if (this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.CONFIGS) {
         if (this.configsSection.handleScroll(logicalMouseX, logicalMouseY, verticalAmount, contentX, panelY, contentWidth, panelHeight)) {
            return true;
         }
      } else if (this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.THEMES) {
         if (this.themeSection.handleScroll(logicalMouseX, logicalMouseY, verticalAmount, contentX, panelY, contentWidth, panelHeight)) {
            return true;
         }
      } else if (this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.COSMETICS) {
         if (this.cosmeticsSection.handleScroll(logicalMouseX, logicalMouseY, verticalAmount, contentX, panelY, contentWidth, panelHeight)) {
            return true;
         }
      } else if (this.moduleGridSection
         .handleScroll(logicalMouseX, logicalMouseY, verticalAmount, contentX, panelY, contentWidth, panelHeight, this.categoryBarSection.getActiveTab())) {
         return true;
      }

      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   public boolean charTyped(CharacterEvent input) {
      int codePoint = this.resolveCharCodePoint(input);
      if (this.searchOpen) {
         if (this.searchFocused && codePoint >= 32 && codePoint != 127) {
            this.searchQuery = this.searchQuery + new String(Character.toChars(codePoint));
         }

         return true;
      } else {
         if (this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.THEMES && this.themeSection.handleCharTyped(codePoint)) {
            return true;
         }

         if (this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.CONFIGS && this.configsSection.handleCharTyped(codePoint)) {
            return true;
         }

         Interface interfaceModule = Havoc.getInstance().getModuleManager().getModule(Interface.class);
         return interfaceModule != null && interfaceModule.handleHudEditorCharTyped(codePoint) ? true : super.charTyped(input);
      }
   }

   public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
      MouseButtonEvent logicalClick = this.toLogicalClick(click);
      float panelWidth = this.getPanelWidth();
      float panelHeight = this.getPanelHeight();
      float panelX = this.getPanelX(panelWidth);
      float panelY = (this.height - panelHeight) / 2.0F;
      return this.categoryBarSection.getActiveTab() == CategoryBarSection.TopTab.CONFIGS
            && this.configsSection.handleMouseDragged(logicalClick.x(), logicalClick.y(), logicalClick.button(), panelX, panelY, panelWidth, panelHeight)
         ? true
         : super.mouseDragged(click, deltaX, deltaY);
   }

   private void applySavedMenuState() {
      ConfigManager configManager = Havoc.getInstance().getConfigManager();
      ConfigManager.MenuState state = configManager.getMenuState();
      if (state.activeTopTab != null) {
         try {
            this.categoryBarSection.setActiveTab(CategoryBarSection.TopTab.valueOf(state.activeTopTab));
         } catch (IllegalArgumentException var4) {
         }
      }

      this.moduleGridSection.applyUiState(state.moduleTabScroll, state.selectExpanded, state.multiSelectExpanded);
      this.configsSection.applyUiState(state.configsSelectedConfig, state.configsScroll);
   }

   private void persistMenuState() {
      ConfigManager configManager = Havoc.getInstance().getConfigManager();
      ConfigManager.MenuState state = new ConfigManager.MenuState();
      state.activeTopTab = this.categoryBarSection.getActiveTab().name();
      state.configsSelectedConfig = this.configsSection.getSelectedConfigName();
      state.configsScroll = this.configsSection.getScrollTarget();
      state.moduleTabScroll = this.moduleGridSection.captureTabScrollState();
      state.selectExpanded = this.moduleGridSection.captureSelectExpandedState();
      state.multiSelectExpanded = this.moduleGridSection.captureMultiSelectExpandedState();
      configManager.updateMenuState(state);
   }

   private float clamp01(float value) {
      return Math.clamp(value, 0.0F, 1.0F);
   }

   private int resolveCharCodePoint(CharacterEvent input) {
      if (input == null) {
         return 0;
      }

      String[] accessors = new String[]{"codePoint", "codepoint", "character", "chr", "charCode", "value"};

      for (String accessor : accessors) {
         try {
            Object v = input.getClass().getMethod(accessor).invoke(input);
            if (v instanceof Number n) {
               return n.intValue();
            }

            if (v instanceof Character c) {
               return c;
            }
         } catch (Exception var9) {
         }
      }

      return 0;
   }

   private float smoothStep(float value) {
      float x = this.clamp01(value);
      return x * x * (3.0F - 2.0F * x);
   }

   private boolean handleSearchKeyPressed(KeyEvent key) {
      int code = key.key();
      if (code == 256) {
         this.searchOpen = false;
         this.searchFocused = false;
         this.searchQuery = "";
         return true;
      }

      if (!this.searchFocused) {
         return true;
      }

      if (code == 259) {
         if (!this.searchQuery.isEmpty()) {
            this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
         }

         return true;
      } else {
         if (code != 257 && code != 335) {
            return true;
         }

         List<Module> matches = this.collectSearchMatches();
         if (!matches.isEmpty()) {
            this.focusSearchResult(matches.get(0));
         }

         return true;
      }
   }

   private boolean handleSearchClick(MouseButtonEvent click) {
      if (click.button() != 0) {
         return true;
      }

      float panelWidth = this.getPanelWidth();
      float panelHeight = this.getPanelHeight();
      float panelY = (this.height - panelHeight) / 2.0F;
      float boxW = Math.min(236.0F, panelWidth - 44.0F);
      float panelX = this.getPanelX(panelWidth);
      float boxX = panelX + (panelWidth - boxW) * 0.5F;
      float boxY = panelY + 42.0F;
      float rowY = boxY + 28.0F;
      float rowH = 17.0F;
      List<Module> matches = this.collectSearchMatches();
      int rows = Math.min(7, matches.size());
      if (click.x() >= boxX && click.x() <= boxX + boxW && click.y() >= boxY && click.y() <= boxY + 22.0F) {
         this.searchFocused = true;
         return true;
      }

      for (int i = 0; i < rows; i++) {
         float y = rowY + i * (rowH + 2.5F);
         if (click.x() >= boxX && click.x() <= boxX + boxW && click.y() >= y && click.y() <= y + rowH) {
            this.focusSearchResult(matches.get(i));
            return true;
         }
      }

      if (!(click.x() < boxX) && !(click.x() > boxX + boxW) && !(click.y() < boxY) && !(click.y() > rowY + rows * (rowH + 3.0F))) {
         this.searchFocused = false;
      } else {
         this.searchOpen = false;
         this.searchFocused = false;
         this.searchQuery = "";
      }

      return true;
   }

   private void renderSearchOverlay(GuiGraphicsExtractor context) {
      if (this.searchOpen) {
         float searchVisibility = this.searchAnim.update(1.0F, 0.08F, System.nanoTime());
         float eased = this.smoothStep(searchVisibility);
         float panelWidth = this.getPanelWidth();
         float panelHeight = this.getPanelHeight();
         float panelX = this.getPanelX(panelWidth);
         float panelY = (this.height - panelHeight) / 2.0F;
         RenderUtil.drawRoundRect(context, panelX, panelY, panelWidth, panelHeight, 6.0F, ColorUtil.rgba(0, 0, 0, Math.round(132.0F * eased)));
         float boxW = Math.min(236.0F, panelWidth - 44.0F);
         float boxX = panelX + (panelWidth - boxW) * 0.5F;
         float boxY = panelY + 42.0F - (1.0F - eased) * 8.0F;
         float inputH = 22.0F;
         float radius = 4.0F;
         RenderUtil.drawShadow(
            context, boxX, boxY, boxW, inputH, radius, ColorUtil.multiplyAlpha(ClientColors.MENU_SHADOW, 0.78F * eased), 1.0F, 8.0F, 0.0F, 0.0F
         );
         RenderUtil.drawRoundRect(context, boxX, boxY, boxW, inputH, radius, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND, 0.98F * eased));
         RenderUtil.drawBorder(
            context, boxX, boxY, boxW, inputH, radius, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND_OUTLINE, eased), 1.0F, 0.0F, 0.5F
         );
         boolean showPlaceholder = this.searchQuery.isEmpty() && !this.searchFocused;
         String label = showPlaceholder ? "Search modules..." : this.searchQuery;
         int textColor = showPlaceholder
            ? ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, eased)
            : ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, eased);
         if (!label.isEmpty()) {
            RenderUtil.drawText(context, label, boxX + 11.0F, boxY + 7.2F, textColor, GFonts.MEDIUM, 7);
         }

         if (this.searchFocused && (System.currentTimeMillis() / 520L & 1L) == 0L) {
            float caretBaseX = boxX + 11.0F;
            float caretX = caretBaseX + (this.searchQuery.isEmpty() ? 0.0F : RenderUtil.getTextWidth(this.searchQuery, GFonts.MEDIUM, 7)) + 1.5F;
            RenderUtil.drawRoundRect(
               context, caretX, boxY + 6.0F, 1.0F, inputH - 12.0F, 0.5F, ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, eased * 0.86F)
            );
         }

         List<Module> matches = this.collectSearchMatches();
         float rowY = boxY + 28.0F;
         float rowH = 17.0F;
         int rows = Math.min(7, matches.size());

         for (int i = 0; i < rows; i++) {
            Module module = matches.get(i);
            float rowEase = this.smoothStep(Math.clamp((eased - i * 0.045F) / 0.82F, 0.0F, 1.0F));
            float y = rowY + i * (rowH + 2.5F) + (1.0F - rowEase) * 5.0F;
            float appear = (1.0F - i * 0.055F) * rowEase;
            int bg = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND, appear * 0.96F);
            int border = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND_OUTLINE, appear);
            RenderUtil.drawRoundRect(context, boxX, y, boxW, rowH, 3.5F, bg);
            RenderUtil.drawBorder(context, boxX, y, boxW, rowH, 3.5F, border, 1.0F, 0.0F, 0.5F);
            this.drawHighlightedModuleName(context, module.getName(), this.searchQuery, boxX + 8.0F, y + 5.0F, appear);
            RenderUtil.drawText(
               context,
               module.getCategory().name(),
               boxX + boxW - 54.0F,
               y + 5.3F,
               ColorUtil.rgba(132, 136, 154, Math.round(205.0F * appear)),
               GFonts.MEDIUM,
               6
            );
         }

         if (rows == 0 && !this.searchQuery.isEmpty()) {
            RenderUtil.drawRoundRect(context, boxX, rowY, boxW, rowH, 3.5F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND, 0.92F));
            RenderUtil.drawText(context, "No modules found", boxX + 8.0F, rowY + 5.0F, ColorUtil.rgba(160, 164, 180, 230), GFonts.MEDIUM, 7);
         }
      }
   }

   private void renderSearchHint(GuiGraphicsExtractor context, float panelX, float panelWidth, float panelBottomY, float alpha) {
      String hint = "CTRL + F - открыть поиск модулей";
      float textSize = 6.0F;
      float textW = RenderUtil.getTextWidth(hint, GFonts.MEDIUM, textSize);
      float x = panelX + (panelWidth - textW) * 0.5F;
      float y = panelBottomY + 6.0F;
      RenderUtil.drawText(context, hint, x, y, ColorUtil.rgba(255, 255, 255, Math.round(185.0F * alpha)), GFonts.MEDIUM, textSize);
   }

   private List<Module> collectSearchMatches() {
      List<Module> modules = Havoc.getInstance().getModuleManager().getModules();
      String query = this.searchQuery.trim().toLowerCase(Locale.ROOT);
      List<Module> matches = new ArrayList<>();
      if (query.isEmpty()) {
         return matches;
      }

      for (Module module : modules) {
         String name = module.getName();
         if (name.toLowerCase(Locale.ROOT).contains(query)) {
            matches.add(module);
         }
      }

      matches.sort((a, b) -> {
         String an = a.getName().toLowerCase(Locale.ROOT);
         String bn = b.getName().toLowerCase(Locale.ROOT);
         boolean ap = !query.isEmpty() && an.startsWith(query);
         boolean bp = !query.isEmpty() && bn.startsWith(query);
         if (ap != bp) {
            return ap ? -1 : 1;
         } else {
            return an.compareTo(bn);
         }
      });
      return matches;
   }

   private void drawHighlightedModuleName(GuiGraphicsExtractor context, String name, String query, float x, float y, float alpha) {
      String normalizedQuery = query == null ? "" : query.trim();
      int baseColor = ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha);
      if (normalizedQuery.isEmpty()) {
         RenderUtil.drawText(context, name, x, y, baseColor, GFonts.MEDIUM, 7);
      } else {
         String lowerName = name.toLowerCase(Locale.ROOT);
         String lowerQuery = normalizedQuery.toLowerCase(Locale.ROOT);
         int start = lowerName.indexOf(lowerQuery);
         if (start < 0) {
            RenderUtil.drawText(context, name, x, y, baseColor, GFonts.MEDIUM, 7);
         } else {
            String before = name.substring(0, start);
            String match = name.substring(start, start + normalizedQuery.length());
            String after = name.substring(start + normalizedQuery.length());
            float cursor = x;
            if (!before.isEmpty()) {
               RenderUtil.drawText(context, before, cursor, y, baseColor, GFonts.MEDIUM, 7);
               cursor += RenderUtil.getTextWidth(before, GFonts.MEDIUM, 7);
            }

            int accentColor = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, alpha);
            RenderUtil.drawText(context, match, cursor, y, accentColor, GFonts.MEDIUM, 7);
            cursor += RenderUtil.getTextWidth(match, GFonts.MEDIUM, 7);
            if (!after.isEmpty()) {
               RenderUtil.drawText(context, after, cursor, y, baseColor, GFonts.MEDIUM, 7);
            }
         }
      }
   }

   private void focusSearchResult(Module module) {
      float panelWidth = this.getPanelWidth();
      float panelHeight = this.getPanelHeight();
      float contentWidth = panelWidth - 108.0F;
      CategoryBarSection.TopTab tab = this.moduleGridSection.focusModule(module, contentWidth, panelHeight);
      if (tab != null) {
         this.categoryBarSection.setActiveTab(tab);
      }

      this.searchOpen = false;
      this.searchFocused = false;
      this.searchQuery = "";
      this.searchAnim.setValue(0.0F);
   }

   private boolean isCtrlDown() {
      if (this.minecraft != null && this.minecraft.getWindow() != null) {
         long handle = this.minecraft.getWindow().handle();
         return GLFW.glfwGetKey(handle, 341) == 1 || GLFW.glfwGetKey(handle, 345) == 1;
      } else {
         return false;
      }
   }

   private float getPanelWidth() {
      float scale = Math.max(0.01F, this.getInterfaceScale());
      return Math.min((this.width - 48.0F) / scale, 396.0F);
   }

   private float getPanelX(float panelWidth) {
      return (this.width - panelWidth) * 0.5F;
   }

   private float getPanelHeight() {
      float scale = Math.max(0.01F, this.getInterfaceScale());
      return Math.min((this.height - 56.0F) / scale, 300.0F);
   }

   private MouseButtonEvent toLogicalClick(MouseButtonEvent click) {
      float scale = this.getInterfaceScale();
      return new MouseButtonEvent(this.toLogicalX(click.x(), scale), this.toLogicalY(click.y(), scale), click.buttonInfo());
   }

   private double toLogicalX(double value, float scale) {
      return this.width * 0.5 + (value - this.width * 0.5) / Math.max(0.01F, scale);
   }

   private double toLogicalY(double value, float scale) {
      return this.height * 0.5 + (value - this.height * 0.5) / Math.max(0.01F, scale);
   }

   private float getInterfaceScale() {
      Menu menu = Havoc.getInstance().getModuleManager().getModule(Menu.class);
      return menu == null ? 1.0F : menu.getInterfaceScale();
   }

   public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
   }

   public void onClose() {
      this.requestClose();
   }

   private void applyClose() {
      if (!this.closeApplied) {
         this.closeApplied = true;
         Module menuModule = Havoc.getInstance().getModuleManager().getModule(Menu.class);
         if (menuModule != null) {
            menuModule.setEnable(false);
         }

         if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parentScreen);
         }
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}
