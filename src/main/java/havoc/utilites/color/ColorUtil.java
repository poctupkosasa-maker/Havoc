package havoc.utilites.color;

public final class ColorUtil {
   private ColorUtil() {
   }

   public static int rgba(int r, int g, int b, int a) {
      return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
   }

   public static int argb(int a, int r, int g, int b) {
      return rgba(r, g, b, a);
   }

   public static int rgb(int r, int g, int b) {
      return rgba(r, g, b, 255);
   }

   public static int alpha(int color) {
      return color >> 24 & 0xFF;
   }

   public static int red(int color) {
      return color >> 16 & 0xFF;
   }

   public static int green(int color) {
      return color >> 8 & 0xFF;
   }

   public static int blue(int color) {
      return color & 0xFF;
   }

   public static int withAlpha(int color, int alpha) {
      return (alpha & 0xFF) << 24 | color & 16777215;
   }

   public static int multiplyAlpha(int color, float multiplier) {
      return withAlpha(color, Math.round(alpha(color) * clamp01(multiplier)));
   }

   public static int interpolate(int from, int to, float progress) {
      float clamped = clamp01(progress);
      return rgba(
         Math.round(red(from) + (red(to) - red(from)) * clamped),
         Math.round(green(from) + (green(to) - green(from)) * clamped),
         Math.round(blue(from) + (blue(to) - blue(from)) * clamped),
         Math.round(alpha(from) + (alpha(to) - alpha(from)) * clamped)
      );
   }

   public static float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }
}
