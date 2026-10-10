package havoc.screen.menu.sections.settings;

import havoc.module.setting.InputSetting;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class InputSettingRenderer {
   private static final float TITLE_SIZE = 7.5F;
   private static final float INPUT_H = 14.0F;
   private static final float LABEL_GAP = 4.0F;
   private static final float INPUT_PAD_X = 5.0F;

   private InputSettingRenderer() {
   }

   public static float measureHeight(InputSetting setting, float width) {
      return 25.5F;
   }

   public static void render(
      GuiGraphicsExtractor context,
      String key,
      InputSetting setting,
      float x,
      float y,
      float width,
      int mouseX,
      int mouseY,
      float alpha,
      boolean focused,
      int caretIndex
   ) {
      int titleColor = ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha);
      int bgColor = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_BACKGROUND, alpha);
      int borderColor = ColorUtil.multiplyAlpha(focused ? ClientColors.CATEGORY_ACCENT : ClientColors.SETTINGS_OUTLINE, alpha);
      int textColor = ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha);
      int placeholderColor = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, alpha);
      RenderUtil.drawTextFadeRight(context, setting.getName(), x, y, titleColor, GFonts.MEDIUM, 7.5F, width, 7.0F);
      float boxY = y + 7.5F + 4.0F;
      RenderUtil.drawRoundRect(context, x, boxY, width, 14.0F, 3.0F, bgColor);
      RenderUtil.drawBorder(context, x, boxY, width, 14.0F, 3.0F, borderColor, 0.0F, 0.5F, 1.0F);
      String value = setting.getValue();
      boolean empty = value == null || value.isEmpty();
      String drawText = empty ? "Enter value..." : value;
      RenderUtil.drawTextFadeRight(context, drawText, x + 5.0F, boxY + 4.0F, empty ? placeholderColor : textColor, GFonts.REGULAR, 6.5F, width - 10.0F, 8.0F);
      if (focused) {
         String safe = value == null ? "" : value;
         int caret = Math.max(0, Math.min(caretIndex, safe.length()));
         float caretX = x + 5.0F + RenderUtil.getTextWidth(safe.substring(0, caret), GFonts.REGULAR, 6.5F);
         RenderUtil.drawRoundRect(context, caretX, boxY + 3.0F, 1.0F, 8.0F, 0.5F, ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha));
      }
   }

   public static boolean hitInputBox(float x, float y, float width, float mouseX, float mouseY) {
      float boxY = y + 7.5F + 4.0F;
      return mouseX >= x && mouseX <= x + width && mouseY >= boxY && mouseY <= boxY + 14.0F;
   }
}
