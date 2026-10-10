package havoc.screen.menu;

import java.util.HashMap;
import java.util.Map;

public final class UiAnimationEngine {
   private final Map<String, Float> values = new HashMap<>();
   private final Map<String, Float> pixelRawValues = new HashMap<>();
   private final Map<String, Float> textRawValues = new HashMap<>();
   private final Map<String, Float> textSnappedValues = new HashMap<>();
   private float fpsFactor = 1.0F;
   private static final float SNAP_EPSILON = 0.0015F;
   private static final float TEXT_HYSTERESIS = 0.62F;

   public void beginFrame(int fps) {
      int safeFps = Math.max(15, Math.min(360, fps));
      this.fpsFactor = 60.0F / safeFps;
   }

   public float animate(String key, float target, float speed, float initial) {
      float current = this.values.getOrDefault(key, initial);
      if (Math.abs(target - current) <= 0.0015F) {
         this.values.put(key, target);
         return target;
      }

      float factor = this.clamp(speed * 0.01F * this.fpsFactor, 0.0F, 1.0F);
      float next = current + (target - current) * factor;
      if (Math.abs(target - next) <= 0.0015F) {
         next = target;
      }

      this.values.put(key, next);
      return next;
   }

   public float animate01(String key, float target, float speed, float initial) {
      return this.clamp(this.animate(key, target, speed, initial), 0.0F, 1.0F);
   }

   public float animateLinear(String key, float target, float unitsPerFrame60, float initial) {
      float current = this.values.getOrDefault(key, initial);
      float diff = target - current;
      if (Math.abs(diff) <= 0.0015F) {
         this.values.put(key, target);
         return target;
      } else {
         float maxStep = Math.max(0.0F, unitsPerFrame60 * this.fpsFactor);
         if (maxStep <= 0.0015F) {
            this.values.put(key, current);
            return current;
         } else if (Math.abs(diff) <= maxStep) {
            this.values.put(key, target);
            return target;
         } else {
            float next = current + Math.copySign(maxStep, diff);
            this.values.put(key, next);
            return next;
         }
      }
   }

   public float animatePixel(String key, float target, float speed, float initial) {
      float currentRaw = this.pixelRawValues.getOrDefault(key, initial);
      if (Math.abs(target - currentRaw) <= 0.0015F) {
         float snapped = Math.round(target);
         this.pixelRawValues.put(key, target);
         this.values.put(key, snapped);
         return snapped;
      } else {
         float factor = this.clamp(speed * 0.01F * this.fpsFactor, 0.0F, 1.0F);
         float nextRaw = currentRaw + (target - currentRaw) * factor;
         this.pixelRawValues.put(key, nextRaw);
         float nextPixel = Math.round(nextRaw);
         this.values.put(key, nextPixel);
         return nextPixel;
      }
   }

   public float animateText(String key, float target, float speed, float initial) {
      float raw = this.textRawValues.getOrDefault(key, initial);
      float snapped = this.textSnappedValues.getOrDefault(key, (float)Math.round(initial));
      if (Math.abs(target - raw) <= 0.0015F) {
         float targetPx = Math.round(target);
         this.textRawValues.put(key, target);
         this.textSnappedValues.put(key, targetPx);
         this.values.put(key, targetPx);
         return targetPx;
      }

      float factor = this.clamp(speed * 0.01F * this.fpsFactor, 0.0F, 1.0F);
      float nextRaw = raw + (target - raw) * factor;
      this.textRawValues.put(key, nextRaw);

      while (nextRaw >= snapped + 0.62F) {
         snapped++;
      }

      while (nextRaw <= snapped - 0.62F) {
         snapped--;
      }

      this.textSnappedValues.put(key, snapped);
      this.values.put(key, snapped);
      return snapped;
   }

   public float stabilizeText(String key, float rawValue) {
      float snapped = this.textSnappedValues.getOrDefault(key, (float)Math.round(rawValue));

      while (rawValue >= snapped + 0.62F) {
         snapped++;
      }

      while (rawValue <= snapped - 0.62F) {
         snapped--;
      }

      this.textRawValues.put(key, rawValue);
      this.textSnappedValues.put(key, snapped);
      this.values.put(key, snapped);
      return snapped;
   }

   public void snap(String key, float value) {
      this.values.put(key, value);
      this.pixelRawValues.put(key, value);
      this.textRawValues.put(key, value);
      this.textSnappedValues.put(key, (float)Math.round(value));
   }

   private float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }
}
