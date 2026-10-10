package havoc.screen.menu.sections.settings;

import havoc.module.setting.SliderSetting;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.screen.menu.UiAnimationEngine;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class SliderSettingRenderer {
   private static final float ROW_HEIGHT = 23.0F;
   private static final float TITLE_Y = 0.5F;
   private static final float VALUE_Y = 1.2F;
   private static final float TRACK_Y = 13.7F;
   private static final float TRACK_H = 2.4F;
   private static final float KNOB_SIZE = 7.0F;
   private static final float RIGHT_PAD = 1.0F;
   private static final float VALUE_GAP = 0.4F;
   private static final float NAME_FADE_W = 1.8F;

   private SliderSettingRenderer() {
   }

   public static float measureHeight(SliderSetting setting, float width) {
      return 23.0F;
   }

   public static void render(
      GuiGraphicsExtractor context,
      UiAnimationEngine animation,
      String key,
      SliderSetting setting,
      float x,
      float y,
      float width,
      int mouseX,
      int mouseY,
      float alpha
   ) {
      SliderSettingRenderer.Layout layout = layout(setting, x, y, width);
      float valueAnim = animation.animate("setting:slider:value:" + key, setting.getNormalized(), 24.0F, setting.getNormalized());
      float knobAppear = animation.animate01(
         "setting:slider:knob:appear:" + key, Math.max(0.0F, Math.min(1.0F, alpha)), 16.0F, Math.max(0.0F, Math.min(1.0F, alpha))
      );
      float shownValue = valueAnim;
      int nameColor = ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha);
      int valueColor = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, alpha);
      int trackBgBase = ColorUtil.rgb(8, 9, 11);
      int trackFgBase = ColorUtil.interpolate(ClientColors.CATEGORY_ACCENT, ClientColors.TEXT_PRIMARY, 0.12F);
      int trackBg = ColorUtil.multiplyAlpha(trackBgBase, alpha * 0.95F);
      int trackFg = ColorUtil.multiplyAlpha(trackFgBase, alpha);
      int knobFill = ColorUtil.multiplyAlpha(ColorUtil.rgb(9, 10, 12), alpha * knobAppear);
      int knobBorder = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, alpha * knobAppear);
      RenderUtil.drawTextFadeRight(context, setting.getName(), x, y + 0.5F, nameColor, GFonts.MEDIUM, 8, layout.nameMaxW, 1.8F);
      RenderUtil.drawText(context, formatValue(setting), layout.valueX, y + 1.2F, valueColor, GFonts.REGULAR, 7);
      float trackRound = 1.2F;
      RenderUtil.drawRoundRect(context, layout.trackX, layout.trackY, layout.trackW, 2.4F, trackRound, trackBg);
      float fillW = Math.max(0.0F, Math.min(layout.trackW, layout.trackW * shownValue));
      RenderUtil.drawRoundRect(context, layout.trackX, layout.trackY, fillW, 2.4F, trackRound, trackFg);
      float knobFillW = Math.max(0.0F, Math.min(layout.trackW, layout.trackW * valueAnim));
      float knobX = layout.trackX + knobFillW - 3.5F;
      float knobY = layout.trackY + -2.3F;
      float knobRound = 2.5F;
      float knobScale = 0.78F + 0.22F * knobAppear;
      float knobDrawSize = 7.0F * knobScale;
      float knobDx = (7.0F - knobDrawSize) * 0.5F;
      RenderUtil.drawRoundRect(context, knobX + knobDx, knobY + knobDx, knobDrawSize, knobDrawSize, knobRound * knobScale, knobFill);
      RenderUtil.drawBorder(context, knobX + knobDx, knobY + knobDx, knobDrawSize, knobDrawSize, knobRound * knobScale, knobBorder, 0.0F, 1.0F, 1.0F);
   }

   public static boolean hitTrack(SliderSetting setting, float x, float y, float width, double mouseX, double mouseY) {
      SliderSettingRenderer.Layout layout = layout(setting, x, y, width);
      return isHovered(layout.trackX, layout.trackY - 4.0F, layout.trackW, 15.0F, mouseX, mouseY);
   }

   public static void setFromMouse(SliderSetting setting, float x, float y, float width, double mouseX) {
      SliderSettingRenderer.Layout layout = layout(setting, x, y, width);
      float t = (float)((mouseX - layout.trackX) / Math.max(1.0F, layout.trackW));
      setting.setNormalized(Math.max(0.0F, Math.min(1.0F, t)));
   }

   private static SliderSettingRenderer.Layout layout(SliderSetting setting, float x, float y, float width) {
      String value = formatValue(setting);
      float valueW = RenderUtil.getTextWidth(value, GFonts.REGULAR, 7);
      float valueX = x + width - 1.0F - valueW;
      float nameMaxW = Math.max(12.0F, valueX - 0.4F - x);
      float trackX = x;
      float trackW = Math.max(16.0F, x + width - 1.0F - trackX);
      float trackY = y + 13.7F;
      return new SliderSettingRenderer.Layout(valueX, nameMaxW, trackX, trackY, trackW);
   }

   private static String formatValue(SliderSetting setting) {
      float value = setting.getValue();
      String num;
      if (setting.getStep() >= 1.0F) {
         num = Integer.toString(Math.round(value));
      } else {
         num = String.format(Locale.US, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
      }

      String suffix = setting.getSuffix();
      return suffix != null && !suffix.isBlank() ? num + " " + suffix : num;
   }

   private static boolean isHovered(float x, float y, float width, float height, double mouseX, double mouseY) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   private record Layout(float valueX, float nameMaxW, float trackX, float trackY, float trackW) {
   }
}
