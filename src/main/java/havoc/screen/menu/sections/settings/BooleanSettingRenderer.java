package havoc.screen.menu.sections.settings;

import havoc.module.setting.BooleanSetting;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.render.font.GIcons;
import havoc.screen.menu.UiAnimationEngine;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class BooleanSettingRenderer {
   private static final float CHECK_SIZE = 10.0F;
   private static final float CHECK_RADIUS = 2.0F;
   private static final float KEY_ICON_SIZE = 8.0F;
   private static final float RIGHT_PAD = 2.0F;
   private static final float KEY_ICON_GAP = 3.0F;
   private static final float TEXT_RIGHT_PAD = 4.0F;
   private static final float TITLE_SIZE = 8.0F;
   private static final float DESC_SIZE = 7.0F;
   private static final float DESC_TOP_GAP = 10.2F;
   private static final float DESC_LINE_GAP = 0.0F;
   private static final float BOTTOM_PAD = 0.8F;
   private static final float Y_RENDER_OFFSET = -2.5F;

   private BooleanSettingRenderer() {
   }

   public static float measureHeight(BooleanSetting setting, float width) {
      BooleanSettingRenderer.Layout layout = layout(setting, 0.0F, 0.0F, width, true);
      return layout.blockHeight;
   }

   public static void render(
      GuiGraphicsExtractor context,
      UiAnimationEngine animation,
      String key,
      BooleanSetting setting,
      float x,
      float y,
      float width,
      int mouseX,
      int mouseY,
      float alpha
   ) {
      render(context, animation, key, setting, x, y, width, mouseX, mouseY, alpha, true);
   }

   public static void render(
      GuiGraphicsExtractor context,
      UiAnimationEngine animation,
      String key,
      BooleanSetting setting,
      float x,
      float y,
      float width,
      int mouseX,
      int mouseY,
      float alpha,
      boolean showBindIcon
   ) {
      BooleanSettingRenderer.Layout layout = layout(setting, x, y + -2.5F, width, showBindIcon);
      boolean checkboxHoveredRaw = !setting.isBindOnly() && isHovered(layout.checkX, layout.checkY, 10.0F, 10.0F, mouseX, mouseY);
      boolean keyIconHoveredRaw = showBindIcon && isHovered(layout.keyIconX, layout.keyIconY, 8.0F, 8.0F, mouseX, mouseY);
      float checkboxHover = animation.animate01("setting:boolean:hover:" + key, checkboxHoveredRaw ? 1.0F : 0.0F, 12.0F, 0.0F);
      float keyIconHover = animation.animate01("setting:boolean:key:hover:" + key, keyIconHoveredRaw ? 1.0F : 0.0F, 12.0F, 0.0F);
      float state = animation.animate01("setting:boolean:state:" + key, setting.isValue() ? 1.0F : 0.0F, 16.0F, setting.isValue() ? 1.0F : 0.0F);
      if (showBindIcon) {
         int keyIconBase = ColorUtil.interpolate(ClientColors.CATEGORY_INACTIVE, ClientColors.TEXT_PRIMARY, keyIconHover * 0.7F);
         int keyIconColor = ColorUtil.multiplyAlpha(keyIconBase, alpha);
         RenderUtil.drawIcon(context, GIcons.KEYBOARD, layout.keyIconX, layout.keyIconY, 8.0F, keyIconColor);
      }

      int titleColor = ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha);
      int descColor = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, alpha * 0.95F);
      int titleX1 = (int)Math.floor(x);
      int titleY1 = (int)Math.floor(y);
      int titleX2 = (int)Math.ceil(x + layout.textMaxW);
      int titleY2 = (int)Math.ceil(y + RenderUtil.getTextHeight(GFonts.MEDIUM, 8) + 2.0F);
      context.enableScissor(titleX1, titleY1, titleX2, titleY2);
      RenderUtil.drawText(context, setting.getName(), x, y + 0.3F, titleColor, GFonts.MEDIUM, 8);
      context.disableScissor();
      if (!layout.descriptionLines.isEmpty()) {
         float descY = y + 10.2F;
         float lineStep = RenderUtil.getTextHeight(GFonts.MEDIUM, 7) + 0.0F;

         for (int i = 0; i < layout.descriptionLines.size(); i++) {
            RenderUtil.drawText(context, layout.descriptionLines.get(i), x, descY + i * lineStep, descColor, GFonts.MEDIUM, 7);
         }
      }

      if (!setting.isBindOnly()) {
         int boxBgOff = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_BACKGROUND, alpha);
         int boxBgOn = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, alpha * (0.22F + 0.68F * state));
         int boxBg = ColorUtil.interpolate(boxBgOff, boxBgOn, state);
         int boxBgHover = ColorUtil.interpolate(boxBg, ColorUtil.withAlpha(ClientColors.TEXT_PRIMARY, ColorUtil.alpha(boxBg)), checkboxHover * 0.08F);
         RenderUtil.drawRoundRect(context, layout.checkX, layout.checkY, 10.0F, 10.0F, 2.0F, boxBgHover);
         if (state < 0.999F) {
            int boxBorder = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, alpha * (1.0F - state));
            RenderUtil.drawBorder(context, layout.checkX, layout.checkY, 10.0F, 10.0F, 2.0F, boxBorder, 0.0F, 0.5F, 1.0F);
         }

         if (state > 0.001F) {
            int markColor = ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha * state);
            RenderUtil.drawIcon(context, GIcons.CHECKMARK, layout.checkX + 1.4F, layout.checkY + 1.4F, 7.2F, markColor);
         }
      }
   }

   public static boolean hitCheckbox(BooleanSetting setting, float x, float y, float width, double mouseX, double mouseY) {
      return hitCheckbox(setting, x, y, width, mouseX, mouseY, true);
   }

   public static boolean hitCheckbox(BooleanSetting setting, float x, float y, float width, double mouseX, double mouseY, boolean showBindIcon) {
      if (setting.isBindOnly()) {
         return false;
      }

      BooleanSettingRenderer.Layout layout = layout(setting, x, y + -2.5F, width, showBindIcon);
      return isHovered(layout.checkX, layout.checkY, 10.0F, 10.0F, mouseX, mouseY);
   }

   public static boolean hitKeyIcon(BooleanSetting setting, float x, float y, float width, double mouseX, double mouseY) {
      return hitKeyIcon(setting, x, y, width, mouseX, mouseY, true);
   }

   public static boolean hitKeyIcon(BooleanSetting setting, float x, float y, float width, double mouseX, double mouseY, boolean showBindIcon) {
      if (!showBindIcon) {
         return false;
      }

      BooleanSettingRenderer.Layout layout = layout(setting, x, y + -2.5F, width, true);
      return isHovered(layout.keyIconX, layout.keyIconY, 8.0F, 8.0F, mouseX, mouseY);
   }

   private static BooleanSettingRenderer.Layout layout(BooleanSetting setting, float x, float y, float width, boolean showBindIcon) {
      float checkX = x + width - 2.0F - 10.0F;
      float keyIconX = setting.isBindOnly() ? x + width - 2.0F - 8.0F : checkX - 3.0F - 8.0F;
      float textMaxW = showBindIcon ? Math.max(14.0F, keyIconX - x - 4.0F) : Math.max(14.0F, checkX - x - 4.0F);
      List<String> descriptionLines = wrapDescription(setting.getDescription(), textMaxW);
      float checkY;
      if (descriptionLines.isEmpty()) {
         float titleH = RenderUtil.getTextHeight(GFonts.MEDIUM, 8);
         checkY = y + 0.3F + (titleH - 10.0F) / 2.0F;
      } else {
         float descH = RenderUtil.getTextHeight(GFonts.MEDIUM, 7);
         checkY = y + 10.2F + (descH - 10.0F) / 2.0F;
      }

      keyIconX = showBindIcon ? keyIconX : checkX;
      float keyIconY = checkY + 1.0F;
      float blockHeight = computeBlockHeight(descriptionLines, checkY - y);
      return new BooleanSettingRenderer.Layout(checkX, checkY, keyIconX, keyIconY, textMaxW, descriptionLines, blockHeight);
   }

   private static float computeBlockHeight(List<String> descriptionLines, float checkTopOffset) {
      float titleH = RenderUtil.getTextHeight(GFonts.MEDIUM, 8);
      float textBottom = titleH;
      if (!descriptionLines.isEmpty()) {
         float descH = RenderUtil.getTextHeight(GFonts.MEDIUM, 7);
         float lineStep = descH + 0.0F;
         textBottom = 10.2F + descH + Math.max(0, descriptionLines.size() - 1) * lineStep;
      }

      float checkBottom = checkTopOffset + 10.0F;
      return Math.max(textBottom, checkBottom) + 0.8F;
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
                  lines.addAll(splitLongWord(word, maxWidth));
                  current = "";
               }
            } else {
               String candidate = current + " " + word;
               if (RenderUtil.getTextWidth(candidate, GFonts.MEDIUM, 7) <= maxWidth) {
                  current = candidate;
               } else {
                  lines.add(current);
                  if (RenderUtil.getTextWidth(word, GFonts.MEDIUM, 7) <= maxWidth) {
                     current = word;
                  } else {
                     lines.addAll(splitLongWord(word, maxWidth));
                     current = "";
                  }
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

   private static List<String> splitLongWord(String word, float maxWidth) {
      List<String> out = new ArrayList<>();
      String rest = word;

      while (!rest.isEmpty()) {
         int cut = rest.length();

         while (cut > 1 && RenderUtil.getTextWidth(rest.substring(0, cut), GFonts.MEDIUM, 7) > maxWidth) {
            cut--;
         }

         out.add(rest.substring(0, cut));
         rest = rest.substring(cut);
      }

      return out;
   }

   private static boolean isHovered(float x, float y, float width, float height, double mouseX, double mouseY) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   private static final class Layout {
      private final float checkX;
      private final float checkY;
      private final float keyIconX;
      private final float keyIconY;
      private final float textMaxW;
      private final List<String> descriptionLines;
      private final float blockHeight;

      private Layout(float checkX, float checkY, float keyIconX, float keyIconY, float textMaxW, List<String> descriptionLines, float blockHeight) {
         this.checkX = checkX;
         this.checkY = checkY;
         this.keyIconX = keyIconX;
         this.keyIconY = keyIconY;
         this.textMaxW = textMaxW;
         this.descriptionLines = descriptionLines;
         this.blockHeight = blockHeight;
      }
   }
}
