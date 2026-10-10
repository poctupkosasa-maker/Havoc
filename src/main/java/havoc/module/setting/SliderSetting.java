package havoc.module.setting;

public final class SliderSetting extends AbstractSetting<Float> {
   private final float min;
   private final float max;
   private final float step;
   private final String suffix;

   public SliderSetting(String name, float defaultValue, float min, float max, float step) {
      this(name, defaultValue, min, max, step, null);
   }

   public SliderSetting(String name, float defaultValue, float min, float max, float step, String suffix) {
      super(name, defaultValue);
      this.min = Math.min(min, max);
      this.max = Math.max(min, max);
      this.step = Math.max(1.0E-4F, step);
      this.suffix = suffix != null && !suffix.isBlank() ? suffix : null;
      this.setValue(defaultValue);
   }

   @Override
   public SettingType getType() {
      return SettingType.SLIDER;
   }

   public float getMin() {
      return this.min;
   }

   public float getMax() {
      return this.max;
   }

   public float getStep() {
      return this.step;
   }

   public String getSuffix() {
      return this.suffix;
   }

   public float getNormalized() {
      return this.max <= this.min ? 0.0F : this.clamp01((this.getValue() - this.min) / (this.max - this.min));
   }

   public void setNormalized(float value) {
      float normalized = this.clamp01(value);
      float absolute = this.min + (this.max - this.min) * normalized;
      this.setValue(absolute);
   }

   public void setValue(float value) {
      float clamped = Math.max(this.min, Math.min(this.max, value));
      float stepped = Math.round((clamped - this.min) / this.step) * this.step + this.min;
      float finalValue = Math.max(this.min, Math.min(this.max, stepped));
      super.setValue(finalValue);
   }

   private float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }
}
