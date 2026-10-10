package havoc.screen.style;

import havoc.render.RenderUtil;
import havoc.utilites.color.ColorUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared console-style surfaces. Input, labels and narration remain with the widget. */
public final class ConsoleStyle {
   public static final int TEXT = 0xFFF4F7FC;
   public static final int MUTED = 0xFFA7B5CB;

   private ConsoleStyle() {
   }

   /** Per-widget animation: no global widget references and no frame-rate dependence. */
   public static final class FocusAnimation {
      private long lastNanos;
      private float value;

      public float update(boolean focused) {
         long now = System.nanoTime();
         float elapsed = this.lastNanos == 0 ? 0.016F : Math.clamp((now - this.lastNanos) / 1_000_000_000.0F, 0, 0.1F);
         this.lastNanos = now;
         this.value += ((focused ? 1 : 0) - this.value) * (1 - (float)Math.exp(-16 * elapsed));
         return this.value;
      }
   }

   public static void button(GuiGraphicsExtractor graphics, float x, float y, float width, float height,
                             boolean enabled, float focus, float alpha, boolean primary) {
      float radius = height * 0.5F;
      int base = primary ? 0xFFE5EBF5 : ColorUtil.interpolate(0xED1F2531, 0xF03B4557, focus);
      if (!enabled) {
         base = 0xA0141C2A;
      }
      RenderUtil.drawRoundRect(graphics, x, y, width, height, radius, ColorUtil.multiplyAlpha(base, alpha));
      if (enabled && focus > 0.02F) {
         RenderUtil.drawBorder(graphics, x - 2.0F, y - 2.0F, width + 4.0F, height + 4.0F, radius + 2.0F,
            ColorUtil.multiplyAlpha(0xFFB6D7FF, alpha * focus * 0.65F), 1.0F, 0.0F, 0.5F);
      }
   }
}
