package havoc.screen.menu.sections.settings;

import havoc.module.setting.ColorSetting;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.screen.menu.UiAnimationEngine;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ColorSettingRenderer {
   private static final float ROW_HEIGHT = 16.0F;
   private static final float TITLE_Y = 0.5F;
   private static final float RIGHT_PAD = 1.0F;
   private static final float ICON_SIZE = 8.0F;
   private static final float SWATCH_SIZE = 8.0F;
   private static final float GAP = 4.0F;
   private static final float NAME_FADE_W = 3.0F;

   private ColorSettingRenderer() {
   }

   public static float measureHeight(ColorSetting setting, float width) {
      return 16.0F;
   }

   public static void render(
      GuiGraphicsExtractor context,
      UiAnimationEngine animation,
      String key,
      ColorSetting setting,
      float x,
      float y,
      float width,
      int mouseX,
      int mouseY,
      float alpha
   ) {
      ColorSettingRenderer.Layout layout = layout(x, y, width);
      boolean hovered = isHovered(layout.iconX, layout.iconY, 8.0F, 8.0F, mouseX, mouseY);
      float hover = animation.animate01("setting:color:hover:" + key, hovered ? 1.0F : 0.0F, 16.0F, 0.0F);
      int textColor = ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha);
      int iconColor = ColorUtil.multiplyAlpha(ColorUtil.interpolate(ClientColors.CATEGORY_INACTIVE, ClientColors.TEXT_PRIMARY, hover * 0.7F), alpha);
      int swatchBorder = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, alpha);
      int swatchColor = ColorUtil.multiplyAlpha(setting.getColor(), alpha);
      RenderUtil.drawTextFadeRight(context, setting.getName(), x, y + 0.5F, textColor, GFonts.MEDIUM, 8, layout.nameMaxW, 3.0F);
      RenderUtil.drawRoundRect(context, layout.swatchX, layout.swatchY, 8.0F, 8.0F, 3.0F, swatchColor);
      RenderUtil.drawBorder(context, layout.swatchX, layout.swatchY, 8.0F, 8.0F, 3.0F, swatchBorder, 0.0F, 0.6F, 1.0F);
      RenderUtil.drawImage(context, "textures/gui/icons/theme.png", layout.iconX, layout.iconY, 8.0F, 8.0F, 0.0F, iconColor, 0.0F);
   }

   public static boolean hitThemeButton(ColorSetting setting, float x, float y, float width, double mouseX, double mouseY) {
      ColorSettingRenderer.Layout layout = layout(x, y, width);
      return isHovered(layout.iconX, layout.iconY, 8.0F, 8.0F, mouseX, mouseY);
   }

   private static ColorSettingRenderer.Layout layout(float x, float y, float width) {
      float iconX = x + width - 1.0F - 8.0F;
      float iconY = y + 1.0F;
      float swatchX = iconX - 4.0F - 8.0F;
      float swatchY = y + 1.0F;
      float nameMaxW = Math.max(16.0F, swatchX - 4.0F - x);
      return new ColorSettingRenderer.Layout(iconX, iconY, swatchX, swatchY, nameMaxW);
   }

   private static boolean isHovered(float x, float y, float width, float height, double mouseX, double mouseY) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   private record Layout(float iconX, float iconY, float swatchX, float swatchY, float nameMaxW) {
   }
}
