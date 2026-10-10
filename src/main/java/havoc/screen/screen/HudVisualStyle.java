package havoc.screen.screen;

import havoc.module.impl.visuals.Interface;
import havoc.render.RenderUtil;
import havoc.render.builders.states.QuadRadiusState;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudVisualStyle {
   public static final float RADIUS = 5.0F;
   public static int PRIMARY_TEXT = ColorUtil.rgba(244, 244, 247, 255);
   public static int SECONDARY_TEXT = ColorUtil.rgba(155, 155, 164, 255);
   public static int MUTED_TEXT = ColorUtil.rgba(101, 102, 112, 255);
   private static boolean lightMode;

   private HudVisualStyle() {
   }

   public static void applyLightMode(boolean light) {
      applyThemePalette(light);
   }

   public static void applyThemePalette(boolean light) {
      lightMode = light;
      PRIMARY_TEXT = ClientColors.HUD_TEXT;
      SECONDARY_TEXT = ClientColors.HUD_SECONDARY_TEXT;
      MUTED_TEXT = ColorUtil.interpolate(ClientColors.HUD_SECONDARY_TEXT, ClientColors.HUD_BACKGROUND, 0.32F);
   }

   public static void drawPanel(GuiGraphicsExtractor context, float x, float y, float width, float height, float alpha) {
      drawPanel(context, x, y, width, height, alpha, 5.0F);
   }

   public static void drawPanel(GuiGraphicsExtractor context, float x, float y, float width, float height, float alpha, float radius) {
      float a = ColorUtil.clamp01(alpha);
      if (!(a <= 0.01F) && !(width <= 1.0F) && !(height <= 1.0F)) {
         float safeRadius = Math.max(0.0F, Math.min(radius, Math.min(width, height) * 0.5F));
         if (Interface.isHudShadowEnabled()) {
            RenderUtil.drawGlow(
               context,
               x + 0.5F,
               y + 0.5F,
               width - 1.0F,
               height - 1.0F,
               safeRadius,
               ColorUtil.multiplyAlpha(ClientColors.HUD_SHADOW, a * Interface.getHudShadowOpacity()),
               1.0F,
               Interface.getHudShadowSize()
            );
         }

         RenderUtil.drawRoundRect(context, x, y, width, height, safeRadius, ColorUtil.multiplyAlpha(ClientColors.HUD_BACKGROUND, a));
         RenderUtil.drawBorder(context, x, y, width, height, safeRadius, ColorUtil.multiplyAlpha(ClientColors.HUD_OUTLINE, a), 0.7F, 0.0F, 0.5F);
         float inset = 0.8F;
         RenderUtil.drawRoundRect(
            context,
            x + inset,
            y + inset,
            width - inset * 2.0F,
            height - inset * 2.0F,
            Math.max(0.0F, safeRadius - inset),
            ColorUtil.multiplyAlpha(ClientColors.HUD_INSET, a)
         );
         RenderUtil.drawBorder(
            context,
            x + inset,
            y + inset,
            width - inset * 2.0F,
            height - inset * 2.0F,
            Math.max(0.0F, safeRadius - inset),
            ColorUtil.multiplyAlpha(ClientColors.HUD_OUTLINE, a * 0.55F),
            0.55F,
            0.0F,
            0.5F
         );
      }
   }

   public static void drawGlassPanel(GuiGraphicsExtractor context, float x, float y, float width, float height, float alpha) {
      float a = ColorUtil.clamp01(alpha);
      if (!(a <= 0.01F) && !(width <= 1.0F) && !(height <= 1.0F)) {
         if (Interface.isHudShadowEnabled()) {
            RenderUtil.drawGlow(
               context,
               x + 0.5F,
               y + 0.5F,
               width - 1.0F,
               height - 1.0F,
               5.0F,
               ColorUtil.multiplyAlpha(ClientColors.HUD_SHADOW, a * Interface.getHudShadowOpacity()),
               1.0F,
               Interface.getHudShadowSize()
            );
         }

         RenderUtil.drawRoundRect(context, x, y, width, height, 5.0F, ColorUtil.multiplyAlpha(ClientColors.HUD_BACKGROUND, a));
         RenderUtil.drawBorder(context, x, y, width, height, 5.0F, ColorUtil.multiplyAlpha(ClientColors.HUD_OUTLINE, a * 0.86F), 0.65F, 0.0F, 0.5F);
      }
   }

   public static void drawMenuPanel(GuiGraphicsExtractor context, float x, float y, float width, float height, float sidebarWidth, float alpha) {
      float a = ColorUtil.clamp01(alpha);
      if (!(a <= 0.01F) && !(width <= 1.0F) && !(height <= 1.0F)) {
         if (Interface.isHudShadowEnabled()) {
            RenderUtil.drawShadow(context, x, y + 0.5F, width, height, 5.0F, ColorUtil.multiplyAlpha(ColorUtil.rgba(3, 3, 5, 104), a), 1.0F, 10.0F, 0.0F, 1.0F);
         }

         RenderUtil.drawRoundRect(context, x, y, width, height, 5.0F, ColorUtil.multiplyAlpha(ClientColors.MENU_BACKGROUND, a));
         float split = Math.max(0.0F, Math.min(width, sidebarWidth));
         if (split < width - 1.0F) {
            RenderUtil.drawRoundRect(
               context,
               x + split,
               y,
               width - split,
               height,
               new QuadRadiusState(0.0F, 0.0F, 5.0F, 5.0F),
               ColorUtil.multiplyAlpha(ClientColors.SETTINGS_BACKGROUND, a)
            );
         }

         RenderUtil.drawBorder(context, x, y, width, height, 5.0F, ColorUtil.multiplyAlpha(ColorUtil.rgba(255, 255, 255, 20), a), 0.7F, 0.0F, 0.5F);
         RenderUtil.drawRoundRect(
            context, x + 6.0F, y + 0.9F, Math.max(8.0F, width - 12.0F), 0.55F, 0.3F, ColorUtil.multiplyAlpha(ColorUtil.rgba(255, 255, 255, 13), a)
         );
      }
   }

   public static void drawSignalRail(GuiGraphicsExtractor context, float x, float y, float length, int color, float alpha) {
      float a = ColorUtil.clamp01(alpha);
      RenderUtil.drawRoundRect(context, x, y, 1.25F, length, 0.65F, ColorUtil.multiplyAlpha(ColorUtil.rgba(255, 255, 255, 18), a));
      RenderUtil.drawRoundRect(context, x, y, 1.25F, Math.max(2.0F, length * 0.42F), 0.65F, ColorUtil.multiplyAlpha(color, a));
   }

   public static void drawProminentSignalRail(GuiGraphicsExtractor context, float x, float y, float length, int color, float alpha) {
      float a = ColorUtil.clamp01(alpha);
      if (!(length <= 0.0F) && !(a <= 0.01F)) {
         RenderUtil.drawRoundRect(context, x - 0.55F, y - 1.0F, 2.4F, length + 2.0F, 1.2F, ColorUtil.multiplyAlpha(color, a * 0.16F));
         RenderUtil.drawRoundRect(context, x, y, 1.4F, length, 0.7F, ColorUtil.multiplyAlpha(color, a * 0.92F));
         RenderUtil.drawRoundRect(
            context, x + 0.25F, y + 0.7F, 0.55F, Math.max(1.0F, length - 1.4F), 0.3F, ColorUtil.multiplyAlpha(ColorUtil.rgba(255, 255, 255, 150), a)
         );
      }
   }

   public static void drawHairline(GuiGraphicsExtractor context, float x, float y, float width, int color, float alpha) {
      float a = ColorUtil.clamp01(alpha);
      if (!(width <= 0.0F) && !(a <= 0.01F)) {
         RenderUtil.drawRoundRect(context, x, y, width, 0.65F, 0.35F, ColorUtil.multiplyAlpha(color, a));
      }
   }

   public static float easeOutCubic(float value) {
      float v = ColorUtil.clamp01(value);
      float inv = 1.0F - v;
      return 1.0F - inv * inv * inv;
   }

   public static void drawInset(GuiGraphicsExtractor context, float x, float y, float width, float height, float alpha) {
      float a = ColorUtil.clamp01(alpha);
      RenderUtil.drawRoundRect(context, x, y, width, height, 2.5F, ColorUtil.multiplyAlpha(ClientColors.HUD_INSET, a));
      RenderUtil.drawBorder(context, x, y, width, height, 2.5F, ColorUtil.multiplyAlpha(ClientColors.HUD_OUTLINE, a * 0.65F), 0.55F, 0.0F, 0.5F);
   }
}
