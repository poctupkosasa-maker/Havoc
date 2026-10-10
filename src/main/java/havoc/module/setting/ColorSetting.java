package havoc.module.setting;

import java.awt.Color;
import lombok.Generated;

public final class ColorSetting extends AbstractSetting<Integer> {
   private float hue;
   private float saturation;
   private float brightness;
   private float alpha01;

   public ColorSetting(String name, int defaultColor) {
      super(name, defaultColor);
      this.syncFromColor(defaultColor);
   }

   @Override
   public SettingType getType() {
      return SettingType.COLOR;
   }

   public int getColor() {
      return (Integer)super.getValue();
   }

   public void setColor(int argb) {
      super.setValue(argb);
      this.syncFromColor(argb);
   }

   public void setHue(float hue) {
      this.hue = this.clamp01(hue);
      this.syncToColor();
   }

   public void setSaturation(float saturation) {
      this.saturation = this.clamp01(saturation);
      this.syncToColor();
   }

   public void setBrightness(float brightness) {
      this.brightness = this.clamp01(brightness);
      this.syncToColor();
   }

   public void setAlpha01(float alpha01) {
      this.alpha01 = this.clamp01(alpha01);
      this.syncToColor();
   }

   public void setHSVA(float hue, float saturation, float brightness, float alpha01) {
      this.hue = this.clamp01(hue);
      this.saturation = this.clamp01(saturation);
      this.brightness = this.clamp01(brightness);
      this.alpha01 = this.clamp01(alpha01);
      this.syncToColor();
   }

   private void syncToColor() {
      int rgb = Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 16777215;
      int alpha = Math.round(this.alpha01 * 255.0F) & 0xFF;
      super.setValue(alpha << 24 | rgb);
   }

   private void syncFromColor(int argb) {
      int r = argb >> 16 & 0xFF;
      int g = argb >> 8 & 0xFF;
      int b = argb & 0xFF;
      float[] hsb = Color.RGBtoHSB(r, g, b, null);
      this.hue = hsb[0];
      this.saturation = hsb[1];
      this.brightness = hsb[2];
      this.alpha01 = (argb >> 24 & 0xFF) / 255.0F;
   }

   private float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   @Generated
   public float getHue() {
      return this.hue;
   }

   @Generated
   public float getSaturation() {
      return this.saturation;
   }

   @Generated
   public float getBrightness() {
      return this.brightness;
   }

   @Generated
   public float getAlpha01() {
      return this.alpha01;
   }
}
