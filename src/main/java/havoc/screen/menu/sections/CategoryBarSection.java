package havoc.screen.menu.sections;

import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.screen.menu.UiAnimationEngine;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import lombok.Generated;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public final class CategoryBarSection {
   public static final float SIDEBAR_WIDTH = 108.0F;
   private static final float TITLE_X = 36.5F;
   private static final float TITLE_Y = 1.5F;
   private static final float TITLE_ICON_SIZE = 35.0F;
   private static final float TAB_X_PAD = 8.0F;
   private static final float TAB_TOP = 38.0F;
   private static final float TAB_HEIGHT = 24.0F;
   private static final float TAB_ICON_SIZE = 8.0F;
   private static final float TAB_ICON_X_PAD = 9.0F;
   private static final float TAB_TEXT_GAP = 7.0F;
   private static final float TAB_ICON_Y_OFFSET = -1.5F;
   private static final float TAB_TEXT_Y_OFFSET = 8.15F;
   private static final float TAB_GAP = 2.0F;
   private static final float SECTION_LABEL_BLOCK = 12.0F;
   private static final float SECTION_OTHER_TOP_GAP = 8.0F;
   private static final float SECTION_LABEL_SIZE = 5.0F;
   private static final float SECTION_LABEL_Y_OFFSET = 1.5F;
   private CategoryBarSection.TopTab activeTab = CategoryBarSection.TopTab.COMBAT;

   public void setActiveTab(CategoryBarSection.TopTab tab) {
      if (tab != null) {
         this.activeTab = tab;
      }
   }

   public void render(
      GuiGraphicsExtractor context,
      UiAnimationEngine animation,
      float panelX,
      float panelY,
      float panelHeight,
      int mouseX,
      int mouseY,
      float categoriesVisibility
   ) {
      RenderUtil.drawRoundRect(
         context, panelX, panelY, 108.0F, panelHeight, 6.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND, categoriesVisibility)
      );
      RenderUtil.drawBorder(
         context,
         panelX,
         panelY,
         108.0F,
         panelHeight,
         6.0F,
         ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND_OUTLINE, categoriesVisibility),
         1.0F,
         0.0F,
         0.5F
      );
      RenderUtil.drawRoundRect(
         context,
         panelX + 108.0F - 1.0F,
         panelY + 7.0F,
         1.0F,
         panelHeight - 14.0F,
         0.5F,
         ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND_OUTLINE, categoriesVisibility * 0.65F)
      );
      RenderUtil.drawRotatedImage(
         context,
         "textures/gui/logo_menu.png",
         panelX + 36.5F,
         panelY + 1.5F,
         35.0F,
         35.0F,
         0.0F,
         0.0F,
         ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT_SECOND, categoriesVisibility),
         1.0F
      );
      float tabX = panelX + 8.0F;
      float tabW = 92.0F;
      float tabY = panelY + 38.0F;
      this.renderSectionLabel(context, "GENERAL", panelX, tabY, categoriesVisibility);
      tabY += 12.0F;

      for (CategoryBarSection.TopTab tab : CategoryBarSection.TopTab.values()) {
         if (tab == CategoryBarSection.TopTab.CONFIGS) {
            tabY += 8.0F;
            this.renderSectionLabel(context, "OTHER", panelX, tabY, categoriesVisibility);
            tabY += 12.0F;
         }

         boolean active = tab == this.activeTab;
         boolean hovered = this.isHovered(mouseX, mouseY, tabX, tabY, tabW, 24.0F);
         float hoverAnim = animation.animate01("tab:hover:" + tab.name(), hovered ? 1.0F : 0.0F, 12.0F, 0.0F);
         float activeAnim = animation.animate01("tab:active:" + tab.name(), active ? 1.0F : 0.0F, 9.0F, active ? 1.0F : 0.0F);
         int tabBg = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND, categoriesVisibility * hoverAnim * 0.18F);
         if (ColorUtil.alpha(tabBg) > 0) {
            RenderUtil.drawRoundRect(context, tabX, tabY, tabW, 24.0F, 4.0F, tabBg);
         }

         int baseColor = ColorUtil.interpolate(ClientColors.CATEGORY_INACTIVE, ClientColors.CATEGORY_ACCENT, activeAnim);
         float iconAlpha = 0.58F + activeAnim * 0.34F + hoverAnim * 0.16F;
         int drawColor = ColorUtil.multiplyAlpha(baseColor, categoriesVisibility * iconAlpha);
         float iconX = tabX + 9.0F;
         float iconY = tabY + 8.0F + -1.5F;
         if (tab.iconGlyph != null) {
            float glyphSize = tab == CategoryBarSection.TopTab.COSMETICS ? 6.0F : 8.0F;
            float glyphX = iconX + (8.0F - glyphSize) * 0.5F;
            float glyphY = iconY + (8.0F - glyphSize) * 0.5F + 0.65F;
            RenderUtil.drawText(context, tab.iconGlyph, glyphX, glyphY, drawColor, GFonts.NOTIFICATION_ICONS, glyphSize);
         } else {
            RenderUtil.drawImage(context, tab.iconPath, Math.round(iconX), Math.round(iconY), 8.0F, 8.0F, 0.0F, drawColor, 1.0F);
         }

         int textColor = ColorUtil.multiplyAlpha(
            ColorUtil.interpolate(ClientColors.CATEGORY_INACTIVE, ClientColors.TEXT_PRIMARY, activeAnim * 0.88F + hoverAnim * 0.28F), categoriesVisibility
         );
         RenderUtil.drawText(context, tab.label, iconX + 8.0F + 7.0F, tabY + 8.15F, textColor, GFonts.MEDIUM, 7);
         tabY += 26.0F;
      }
   }

   public boolean handleClick(MouseButtonEvent click, float panelX, float panelY, float panelHeight) {
      if (click.button() != 0) {
         return false;
      }

      float tabX = panelX + 8.0F;
      float tabW = 92.0F;
      float tabY = panelY + 38.0F;
      tabY += 12.0F;

      for (CategoryBarSection.TopTab tab : CategoryBarSection.TopTab.values()) {
         if (tab == CategoryBarSection.TopTab.CONFIGS) {
            tabY += 20.0F;
         }

         if (tabY + 24.0F <= panelY + panelHeight && this.isHovered(click.x(), click.y(), tabX, tabY, tabW, 24.0F)) {
            this.activeTab = tab;
            return true;
         }

         tabY += 26.0F;
      }

      return false;
   }

   private void renderSectionLabel(GuiGraphicsExtractor context, String label, float panelX, float y, float alpha) {
      float textX = panelX + 8.0F + 9.0F;
      RenderUtil.drawText(context, label, textX, y + 1.5F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, alpha * 0.62F), GFonts.MEDIUM, 5.0F);
   }

   private boolean isHovered(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   @Generated
   public CategoryBarSection.TopTab getActiveTab() {
      return this.activeTab;
   }

   public enum TopTab {
      COMBAT("Combat", null, "D"),
      MOVEMENT("Movement", null, "E"),
      PLAYER("Player", "textures/gui/icons/player.png"),
      VISUALS("Visuals", "textures/gui/icons/visuals.png"),
      MISC("Misc", "textures/gui/icons/misc.png"),
      CONFIGS("Configs", "textures/gui/icons/config.png"),
      THEMES("Themes", "textures/gui/icons/theme.png"),
      COSMETICS("Cosmetics", null, "G");

      private final String label;
      private final String iconPath;
      private final String iconGlyph;

      TopTab(String label, String iconPath) {
         this(label, iconPath, null);
      }

      TopTab(String label, String iconPath, String iconGlyph) {
         this.label = label;
         this.iconPath = iconPath;
         this.iconGlyph = iconGlyph;
      }
   }
}
