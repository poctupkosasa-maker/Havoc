package havoc.screen.menu.sections.settings;

import havoc.module.setting.MultiSelectSetting;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.screen.menu.UiAnimationEngine;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class MultiSelectSettingRenderer {
   private static final float TITLE_SIZE = 8.0F;
   private static final float DESC_SIZE = 7.0F;
   private static final float DESC_TOP_GAP = 9.8F;
   private static final float DESC_LINE_GAP = -1.0F;
   private static final float SECTION_GAP = 3.0F;
   private static final float SELECT_Y_OFFSET = -2.0F;
   private static final float BOTTOM_PAD = 1.0F;
   private static final float ICON_SIZE = 8.0F;
   private static final float TITLE_RIGHT_PAD = 4.0F;
   private static final float SELECT_BOX_H = 13.0F;
   private static final float SELECT_BOX_RADIUS = 2.5F;
   private static final float SELECT_PAD_X = 3.0F;
   private static final float SELECT_LEFT_ICON_SIZE = 7.0F;
   private static final float SELECT_ARROW_SIZE = 7.0F;
   private static final float LIST_TOP_GAP = 1.0F;
   private static final float LIST_PAD_Y = 4.0F;
   private static final float ITEM_H = 10.0F;
   private static final float ITEM_GAP = 2.0F;
   private static final float TOGGLE_W = 12.0F;
   private static final float TOGGLE_H = 8.0F;
   private static final float TOGGLE_KNOB = 5.0F;
   private static final float TOGGLE_INNER_PAD = 1.0F;
   private static final float RIGHT_PAD = 1.5F;

   private MultiSelectSettingRenderer() {
   }

   public static float measureHeight(MultiSelectSetting setting, float width, float expandProgress) {
      MultiSelectSettingRenderer.Layout layout = layout(setting, 0.0F, 0.0F, width, expandProgress);
      return layout.blockHeight;
   }

   public static void render(
      GuiGraphicsExtractor context,
      UiAnimationEngine animation,
      String key,
      MultiSelectSetting setting,
      float x,
      float y,
      float width,
      int mouseX,
      int mouseY,
      float alpha,
      float expandProgress
   ) {
      float expand = ColorUtil.clamp01(expandProgress);
      MultiSelectSettingRenderer.Layout layout = layout(setting, x, y, width, expand);
      int titleColor = ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha);
      int descColor = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, alpha * 0.95F);
      int iconColor = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, alpha);
      RenderUtil.drawTextFadeRight(context, setting.getName(), x, y + 0.2F, titleColor, GFonts.MEDIUM, 8, layout.titleTextMaxW, 5.0F);
      RenderUtil.drawImage(context, "textures/gui/icons/multienum.png", layout.titleIconX, y + 0.1F, 8.0F, 8.0F, 0.0F, iconColor, 1.0F);
      if (!layout.descriptionLines.isEmpty()) {
         float lineStep = RenderUtil.getTextHeight(GFonts.MEDIUM, 7) + -1.0F;

         for (int i = 0; i < layout.descriptionLines.size(); i++) {
            RenderUtil.drawText(context, layout.descriptionLines.get(i), x, layout.descY + i * lineStep, descColor, GFonts.MEDIUM, 7);
         }
      }

      boolean selectHoveredRaw = isHovered(layout.selectX, layout.selectY, layout.selectW, 13.0F, mouseX, mouseY);
      float selectHover = animation.animate01("setting:multi:select:hover:" + key, selectHoveredRaw ? 1.0F : 0.0F, 14.0F, 0.0F);
      int selectBgBase = ColorUtil.interpolate(ClientColors.SETTINGS_BACKGROUND, ClientColors.CATEGORY_ACCENT, selectHover * 0.12F);
      int selectBg = ColorUtil.multiplyAlpha(selectBgBase, alpha);
      int selectBorder = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, alpha);
      RenderUtil.drawRoundRect(context, layout.selectX, layout.selectY, layout.selectW, 13.0F, 2.5F, selectBg);
      RenderUtil.drawBorder(context, layout.selectX, layout.selectY, layout.selectW, 13.0F, 2.5F, selectBorder, 0.0F, 0.5F, 1.0F);
      float leftIconY = layout.selectY + 3.0F;
      RenderUtil.drawImage(context, "textures/gui/icons/enum.png", layout.selectX + 3.0F, leftIconY - 0.5F, 7.0F, 7.0F, 0.0F, iconColor, 1.0F);
      String selectedText = formatSelectedText(setting);
      int selectedColor = ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha);
      float selectedX = layout.selectX + 3.0F + 7.0F + 3.0F;
      RenderUtil.drawTextFadeRight(context, selectedText, selectedX, layout.selectY + 4.0F, selectedColor, GFonts.MEDIUM, 7, layout.selectTextW, 2.0F);
      int arrowDownColor = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, alpha * (1.0F - expand));
      int arrowUpColor = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, alpha * expand);
      float arrowY = layout.selectY + 3.0F;
      if (ColorUtil.alpha(arrowDownColor) > 0) {
         RenderUtil.drawImage(context, "textures/gui/icons/arrow.png", layout.arrowX, arrowY, 7.0F, 7.0F, 0.0F, arrowDownColor, 1.0F);
      }

      if (ColorUtil.alpha(arrowUpColor) > 0) {
         RenderUtil.drawImage(context, "textures/gui/icons/arrow_up.png", layout.arrowX, arrowY, 7.0F, 7.0F, 0.0F, arrowUpColor, 1.0F);
      }

      if (!(expand <= 0.001F)) {
         float visibleListH = layout.listH * expand;
         float listAlpha = alpha * (0.35F + 0.65F * expand);
         int listBg = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_BACKGROUND, listAlpha);
         int listBorder = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, listAlpha);
         RenderUtil.drawRoundRect(context, layout.listX, layout.listY, layout.listW, visibleListH, 3.0F, listBg);
         RenderUtil.drawBorder(context, layout.listX, layout.listY, layout.listW, visibleListH, 3.0F, listBorder, 0.0F, 0.5F, 1.0F);
         int scX1 = (int)Math.floor(layout.listX);
         int scY1 = (int)Math.floor(layout.listY);
         int scX2 = (int)Math.ceil(layout.listX + layout.listW);
         int scY2 = (int)Math.ceil(layout.listY + visibleListH);
         context.enableScissor(scX1, scY1, scX2, scY2);

         for (int i = 0; i < setting.getOptions().size(); i++) {
            String option = setting.getOptions().get(i);
            float itemY = layout.listY + 4.0F + i * getItemStep();
            float toggleX = layout.listX + layout.listW - 1.5F - 12.0F;
            float toggleY = itemY + 1.0F;
            float rowX = layout.listX + 2.0F;
            float rowY = itemY - 1.0F;
            float rowW = layout.listW - 4.0F;
            float rowH = 12.0F;
            boolean selected = setting.isSelected(option);
            boolean hoveredRaw = isHovered(rowX, rowY, rowW, rowH, mouseX, mouseY);
            float hover = animation.animate01("setting:multi:item:hover:" + key + ":" + i, hoveredRaw ? 1.0F : 0.0F, 14.0F, 0.0F);
            float state = animation.animate01("setting:multi:item:state:" + key + ":" + i, selected ? 1.0F : 0.0F, 16.0F, selected ? 1.0F : 0.0F);
            int rowBg = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, listAlpha * hover * 0.1F);
            if (ColorUtil.alpha(rowBg) > 0) {
               RenderUtil.drawRoundRect(context, rowX, rowY, rowW, rowH, 2.0F, rowBg);
            }

            RenderUtil.drawTextFadeRight(
               context,
               option,
               layout.listX + 8.0F,
               itemY + 2.2F,
               ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, listAlpha),
               GFonts.MEDIUM,
               7,
               Math.max(12.0F, toggleX - 4.0F - (layout.listX + 8.0F)),
               4.0F
            );
            int toggleTrackBase = ColorUtil.interpolate(ClientColors.SETTINGS_BACKGROUND, ClientColors.CATEGORY_ACCENT, state);
            toggleTrackBase = ColorUtil.interpolate(toggleTrackBase, ClientColors.TEXT_PRIMARY, hover * 0.12F);
            int toggleTrackColor = ColorUtil.multiplyAlpha(toggleTrackBase, alpha);
            int trackOutlineColor = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, alpha * (1.0F - state));
            RenderUtil.drawRoundRect(context, toggleX - 4.0F, toggleY, 12.0F, 8.0F, 3.0F, toggleTrackColor);
            RenderUtil.drawBorder(context, toggleX - 4.0F, toggleY, 12.0F, 8.0F, 3.0F, trackOutlineColor, 0.0F, 0.5F, 1.0F);
            float knobTravel = 5.0F;
            float knobX = toggleX + 1.0F + knobTravel * state;
            float knobY = toggleY + 1.5F;
            int knobBase = ColorUtil.interpolate(ClientColors.CATEGORY_INACTIVE, ClientColors.TEXT_PRIMARY, state);
            knobBase = ColorUtil.interpolate(knobBase, ClientColors.TEXT_PRIMARY, hover * 0.08F);
            int knobColor = ColorUtil.multiplyAlpha(knobBase, alpha * 0.95F);
            RenderUtil.drawRoundRect(context, knobX - 4.0F, knobY, 5.0F, 5.0F, 1.5F, knobColor);
         }

         context.disableScissor();
      }
   }

   public static boolean hitSelectBox(MultiSelectSetting setting, float x, float y, float width, double mouseX, double mouseY) {
      MultiSelectSettingRenderer.Layout layout = layout(setting, x, y, width, 0.0F);
      return isHovered(layout.selectX, layout.selectY, layout.selectW, 13.0F, mouseX, mouseY);
   }

   public static int hitOptionIndex(MultiSelectSetting setting, float x, float y, float width, double mouseX, double mouseY, float expandProgress) {
      float expand = ColorUtil.clamp01(expandProgress);
      if (expand <= 0.001F) {
         return -1;
      }

      MultiSelectSettingRenderer.Layout layout = layout(setting, x, y, width, expand);
      float visibleListBottom = layout.listY + layout.listH * expand;

      for (int i = 0; i < setting.getOptions().size(); i++) {
         float itemY = layout.listY + 4.0F + i * getItemStep();
         float rowX = layout.listX + 2.0F;
         float rowY = itemY - 1.0F;
         float rowW = layout.listW - 4.0F;
         float rowH = 12.0F;
         if (!(rowY + rowH > visibleListBottom) && isHovered(rowX, rowY, rowW, rowH, mouseX, mouseY)) {
            return i;
         }
      }

      return -1;
   }

   private static MultiSelectSettingRenderer.Layout layout(MultiSelectSetting setting, float x, float y, float width, float expandProgress) {
      float titleH = RenderUtil.getTextHeight(GFonts.MEDIUM, 8);
      float titleIconX = x + width - 8.0F - 1.5F;
      float titleTextMaxW = Math.max(16.0F, titleIconX - 4.0F - x);
      List<String> descLines = wrapDescription(setting.getDescription(), Math.max(16.0F, width - 1.5F));
      float descY = y + 9.8F;
      float descH = 0.0F;
      if (!descLines.isEmpty()) {
         float step = RenderUtil.getTextHeight(GFonts.MEDIUM, 7) + -1.0F;
         descH = RenderUtil.getTextHeight(GFonts.MEDIUM, 7) + Math.max(0, descLines.size() - 1) * step;
      }

      float afterHeaderY = y + titleH;
      if (!descLines.isEmpty()) {
         afterHeaderY = Math.max(afterHeaderY, descY + descH);
      }

      float selectY = afterHeaderY + 3.0F + -2.0F;
      float selectX = x;
      float selectW = width - 1.5F;
      float arrowX = selectX + selectW - 3.0F - 7.0F;
      float selectTextW = Math.max(14.0F, arrowX - 3.0F - (selectX + 3.0F + 7.0F + 3.0F));
      float listX = x;
      float listY = selectY + 13.0F + 1.0F;
      float listW = width - 1.5F;
      int optionCount = setting.getOptions().size();
      float listH = 8.0F + optionCount * 10.0F + Math.max(0, optionCount - 1) * 2.0F;
      float collapsedBottom = selectY + 13.0F;
      float expandedBottom = listY + listH;
      float blockBottom = collapsedBottom + (expandedBottom - collapsedBottom) * ColorUtil.clamp01(expandProgress);
      float blockHeight = blockBottom - y + 1.0F;
      return new MultiSelectSettingRenderer.Layout(
         titleIconX, titleTextMaxW, descY, descLines, selectX, selectY, selectW, arrowX, selectTextW, listX, listY, listW, listH, blockHeight
      );
   }

   private static float getItemStep() {
      return 12.0F;
   }

   private static String formatSelectedText(MultiSelectSetting setting) {
      Set<String> selected = setting.getSelectedValues();
      return selected.isEmpty() ? "None" : String.join(", ", selected);
   }

   private static List<String> wrapDescription(String description, float maxWidth) {
      List<String> lines = new ArrayList<>();
      if (description != null && !description.isBlank() && !(maxWidth <= 1.0F)) {
         String[] words = description.trim().split("\\s+");
         String current = "";

         for (String word : words) {
            if (current.isEmpty()) {
               if (RenderUtil.getTextWidth(word, GFonts.MEDIUM, 7) <= maxWidth) {
                  current = word;
               } else {
                  lines.add(word);
               }
            } else {
               String candidate = current + " " + word;
               if (RenderUtil.getTextWidth(candidate, GFonts.MEDIUM, 7) <= maxWidth) {
                  current = candidate;
               } else {
                  lines.add(current);
                  current = word;
               }
            }
         }

         if (!current.isEmpty()) {
            lines.add(current);
         }

         return lines;
      } else {
         return lines;
      }
   }

   private static boolean isHovered(float x, float y, float width, float height, double mouseX, double mouseY) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   private record Layout(
      float titleIconX,
      float titleTextMaxW,
      float descY,
      List<String> descriptionLines,
      float selectX,
      float selectY,
      float selectW,
      float arrowX,
      float selectTextW,
      float listX,
      float listY,
      float listW,
      float listH,
      float blockHeight
   ) {
   }
}
