package havoc.screen.library;

/** Frame-rate-independent card bounds. The same rounded bounds drive drawing and input. */
public final class CarouselMotion {
   private float x;
   private float y;
   private float size;
   private float targetX;
   private float targetY;
   private float targetSize;
   private long lastNanos;
   private boolean initialized;

   public void snap(float x, float y, float size) {
      this.x = this.targetX = x;
      this.y = this.targetY = y;
      this.size = this.targetSize = size;
      this.initialized = true;
      this.lastNanos = 0;
   }

   public void target(float x, float y, float size) {
      if (!this.initialized) this.snap(x, y, size);
      this.targetX = x;
      this.targetY = y;
      this.targetSize = size;
   }

   public void update(long nowNanos) {
      if (!this.initialized) return;
      double dt = this.lastNanos == 0 ? 0 : Math.clamp((nowNanos - this.lastNanos) / 1_000_000_000.0, 0, 0.1);
      this.lastNanos = nowNanos;
      float factor = (float)(1 - Math.exp(-18 * dt));
      this.x = approach(this.x, this.targetX, factor);
      this.y = approach(this.y, this.targetY, factor);
      this.size = approach(this.size, this.targetSize, factor);
   }

   private static float approach(float current, float target, float factor) {
      float next = current + (target - current) * factor;
      return Math.abs(next - target) < 0.05F ? target : next;
   }

   public int x() { return Math.round(this.x); }
   public int y() { return Math.round(this.y); }
   public int size() { return Math.max(1, Math.round(this.size)); }
}
