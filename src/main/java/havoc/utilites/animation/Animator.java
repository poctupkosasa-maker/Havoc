package havoc.utilites.animation;

import java.util.HashMap;
import java.util.Map;

public final class Animator {
   private final Map<String, Float> values = new HashMap<>();
   private long lastFrameNanos = -1L;
   private float dtSec = 0.016666668F;

   public void beginFrame(long frameNanos) {
      if (this.lastFrameNanos > 0L) {
         this.dtSec = (float)(frameNanos - this.lastFrameNanos) / 1.0E9F;
         this.dtSec = this.clamp(this.dtSec, 0.004166667F, 0.08F);
      }

      this.lastFrameNanos = frameNanos;
   }

   public float animate(String key, boolean target, float speed) {
      return this.animate(key, target ? 1.0F : 0.0F, speed);
   }

   public float appear(String key, boolean visible, float speed) {
      float target = visible ? 1.0F : 0.0F;
      if (!this.values.containsKey(key) && visible) {
         this.values.put(key, 0.0F);
      }

      return this.animate(key, target, speed);
   }

   public float animate(String key, float target, float speed) {
      float current = this.values.getOrDefault(key, target);
      float alpha = 1.0F - (float)Math.exp(-Math.max(0.01F, speed) * this.dtSec);
      float next = current + (target - current) * alpha;
      this.values.put(key, next);
      return next;
   }

   public void set(String key, float value) {
      this.values.put(key, value);
   }

   public void reset(String key) {
      this.values.remove(key);
   }

   public void resetAll() {
      this.values.clear();
   }

   private float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }
}
