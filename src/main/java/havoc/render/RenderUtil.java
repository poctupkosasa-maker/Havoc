package havoc.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import havoc.render.builders.Builder;
import havoc.render.builders.states.QuadColorState;
import havoc.render.builders.states.QuadRadiusState;
import havoc.render.builders.states.SizeState;
import havoc.render.font.GFonts;
import havoc.render.font.GIcons;
import havoc.render.font.msdf.MsdfFont;
import havoc.render.font.msdf.MsdfFonts;
import havoc.render.font.msdf.MsdfRenderer;
import havoc.render.renderers.CircleRenderer;
import havoc.render.renderers.RingProgressRenderer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

public final class RenderUtil {
   private static MsdfFont iconMsdfFont;
   private static final Map<String, Identifier> TEXTURE_IDENTIFIERS = new HashMap<>();

   private RenderUtil() {
   }

   public static void drawRoundRect(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, int color) {
      drawRoundRect(ctx, x, y, w, h, radius, color, 1.0F);
   }

   public static void drawRoundRect(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, int color, float smoothness) {
      Builder.rectangle()
         .size(new SizeState(w, h))
         .radius(new QuadRadiusState(radius))
         .color(new QuadColorState(color))
         .smoothness(smoothness)
         .build()
         .render(x, y, ctx);
   }

   public static void drawRoundRect(GuiGraphicsExtractor ctx, float x, float y, float w, float h, QuadRadiusState radiusState, int color) {
      drawRoundRect(ctx, x, y, w, h, radiusState, color, 1.0F);
   }

   public static void drawRoundRect(GuiGraphicsExtractor ctx, float x, float y, float w, float h, QuadRadiusState radiusState, int color, float smoothness) {
      Builder.rectangle().size(new SizeState(w, h)).radius(radiusState).color(new QuadColorState(color)).smoothness(smoothness).build().render(x, y, ctx);
   }

   public static void drawRoundRectGradient(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, int leftColor, int rightColor) {
      drawRoundRectGradient(ctx, x, y, w, h, radius, leftColor, rightColor, 1.0F);
   }

   public static void drawRoundRectGradient(
      GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, int leftColor, int rightColor, float smoothness
   ) {
      Builder.rectangle()
         .size(new SizeState(w, h))
         .radius(new QuadRadiusState(radius))
         .color(new QuadColorState(leftColor, rightColor, rightColor, leftColor))
         .smoothness(smoothness)
         .build()
         .render(x, y, ctx);
   }

   public static void drawRoundRectGradientQuad(
      GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, int topLeft, int bottomLeft, int bottomRight, int topRight
   ) {
      drawRoundRectGradientQuad(ctx, x, y, w, h, new QuadRadiusState(radius), topLeft, bottomLeft, bottomRight, topRight, 1.0F);
   }

   public static void drawRoundRectGradientQuad(
      GuiGraphicsExtractor ctx,
      float x,
      float y,
      float w,
      float h,
      QuadRadiusState radius,
      int topLeft,
      int bottomLeft,
      int bottomRight,
      int topRight,
      float smoothness
   ) {
      Builder.rectangle()
         .size(new SizeState(w, h))
         .radius(radius)
         .color(new QuadColorState(topLeft, bottomLeft, bottomRight, topRight))
         .smoothness(smoothness)
         .build()
         .render(x, y, ctx);
   }

   public static void drawBorder(
      GuiGraphicsExtractor ctx,
      float x,
      float y,
      float w,
      float h,
      float radius,
      int color,
      float thickness,
      float internalSmoothness,
      float externalSmoothness
   ) {
      int adjustedColor = accelerateBorderFade(color);
      Builder.border()
         .size(new SizeState(w, h))
         .radius(new QuadRadiusState(radius))
         .color(new QuadColorState(adjustedColor))
         .thickness(thickness)
         .smoothness(internalSmoothness, externalSmoothness)
         .build()
         .render(x, y, ctx);
   }

   public static void drawBorder(
      GuiGraphicsExtractor ctx,
      float x,
      float y,
      float w,
      float h,
      QuadRadiusState radius,
      int color,
      float thickness,
      float internalSmoothness,
      float externalSmoothness
   ) {
      int adjustedColor = accelerateBorderFade(color);
      Builder.border()
         .size(new SizeState(w, h))
         .radius(radius)
         .color(new QuadColorState(adjustedColor))
         .thickness(thickness)
         .smoothness(internalSmoothness, externalSmoothness)
         .build()
         .render(x, y, ctx);
   }

   public static void drawBlur(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, int color, float smoothness, float blurRadius) {
      Builder.blur()
         .size(new SizeState(w, h))
         .radius(new QuadRadiusState(radius))
         .color(new QuadColorState(color))
         .smoothness(smoothness)
         .blurRadius(blurRadius)
         .build()
         .render(x, y, ctx);
   }

   public static void drawGlow(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, int color, float smoothness, float spread) {
      Builder.glow()
         .size(new SizeState(w, h))
         .radius(new QuadRadiusState(radius))
         .color(new QuadColorState(color))
         .smoothness(smoothness)
         .spread(spread)
         .build()
         .render(x, y, ctx);
   }

   public static void drawGlow(
      GuiGraphicsExtractor ctx, float x, float y, float w, float h, QuadRadiusState radiusState, int color, float smoothness, float spread
   ) {
      Builder.glow()
         .size(new SizeState(w, h))
         .radius(radiusState)
         .color(new QuadColorState(color))
         .smoothness(smoothness)
         .spread(spread)
         .build()
         .render(x, y, ctx);
   }

   public static void drawShadow(
      GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, int color, float smoothness, float spread, float offsetX, float offsetY
   ) {
      drawGlow(ctx, x + offsetX + 0.5F, y + offsetY + 0.5F, w - 1.0F, h - 1.0F, radius, color, smoothness, spread);
   }

   public static void drawShadow(
      GuiGraphicsExtractor ctx,
      float x,
      float y,
      float w,
      float h,
      QuadRadiusState radiusState,
      int color,
      float smoothness,
      float spread,
      float offsetX,
      float offsetY
   ) {
      drawGlow(ctx, x + offsetX + 0.5F, y + offsetY + 0.5F, w - 1.0F, h - 1.0F, radiusState, color, smoothness, spread);
   }

   public static void drawImage(GuiGraphicsExtractor ctx, GpuTextureView texture, float x, float y, float w, float h, float radius, int color, float smoothness) {
      drawRotatedImage(ctx, texture, x, y, w, h, 0.0F, radius, color, smoothness);
   }

   public static void drawRotatedImage(
      GuiGraphicsExtractor ctx, GpuTextureView texture, float x, float y, float w, float h, float rotation, float radius, int color, float smoothness
   ) {
      Builder.texture()
         .size(new SizeState(w, h))
         .radius(new QuadRadiusState(radius))
         .color(new QuadColorState(color))
         .smoothness(smoothness)
         .rotation(rotation)
         .texture(0.0F, 0.0F, 0.0F, 0.0F, texture)
         .build()
         .render(x, y, ctx);
   }

   public static void drawImage(GuiGraphicsExtractor ctx, Identifier texture, float x, float y, float w, float h, float radius, int color, float smoothness) {
      drawRotatedImage(ctx, texture, x, y, w, h, 0.0F, radius, color, smoothness);
   }

   public static void drawRotatedImage(
      GuiGraphicsExtractor ctx, Identifier texture, float x, float y, float w, float h, float rotation, float radius, int color, float smoothness
   ) {
      Minecraft client = Minecraft.getInstance();
      if (client != null) {
         drawRotatedImage(ctx, client.getTextureManager().getTexture(texture).getTextureView(), x, y, w, h, rotation, radius, color, smoothness);
      }
   }

   public static void drawImage(GuiGraphicsExtractor ctx, String texturePath, float x, float y, float w, float h, float radius, int color, float smoothness) {
      drawRotatedImage(ctx, texturePath, x, y, w, h, 0.0F, radius, color, smoothness);
   }

   public static void drawRotatedImage(
      GuiGraphicsExtractor ctx, String texturePath, float x, float y, float w, float h, float rotation, float radius, int color, float smoothness
   ) {
      Identifier identifier = TEXTURE_IDENTIFIERS.computeIfAbsent(texturePath, path -> Identifier.fromNamespaceAndPath("havoc", path));
      drawRotatedImage(ctx, identifier, x, y, w, h, rotation, radius, color, smoothness);
   }

   public static void drawImageRegion(
      GuiGraphicsExtractor ctx,
      GpuTextureView texture,
      float x,
      float y,
      float w,
      float h,
      float u,
      float v,
      float texW,
      float texH,
      float radius,
      int color,
      float smoothness
   ) {
      Builder.texture()
         .size(new SizeState(w, h))
         .radius(new QuadRadiusState(radius))
         .color(new QuadColorState(color))
         .smoothness(smoothness)
         .texture(u, v, texW, texH, texture)
         .build()
         .render(x, y, ctx);
   }

   public static void drawHeadImageRegion(
      GuiGraphicsExtractor ctx,
      GpuTextureView texture,
      float x,
      float y,
      float w,
      float h,
      float u,
      float v,
      float texW,
      float texH,
      float radius,
      int color,
      float smoothness
   ) {
      Builder.headTexture()
         .size(new SizeState(w, h))
         .radius(new QuadRadiusState(radius))
         .color(new QuadColorState(color))
         .smoothness(smoothness)
         .texture(u, v, texW, texH, texture)
         .build()
         .render(x, y, ctx);
   }

   public static void drawText(GuiGraphicsExtractor ctx, String text, float x, float y, int color, GFonts font, int size) {
      MsdfRenderer.drawString(ctx, MsdfFonts.get(font), text, size, color, x, y);
   }

   public static void drawText(GuiGraphicsExtractor ctx, String text, float x, float y, int color, GFonts font, float size) {
      MsdfRenderer.drawString(ctx, MsdfFonts.get(font), text, size, color, x, y);
   }

   public static void drawTextFadeRight(
      GuiGraphicsExtractor ctx, String text, float x, float y, int color, GFonts font, int size, float maxWidth, float fadeWidth
   ) {
      MsdfRenderer.drawStringFadeRight(ctx, MsdfFonts.get(font), text, size, color, x, y, maxWidth, fadeWidth);
   }

   public static void drawTextFadeRight(
      GuiGraphicsExtractor ctx, String text, float x, float y, int color, GFonts font, float size, float maxWidth, float fadeWidth
   ) {
      MsdfRenderer.drawStringFadeRight(ctx, MsdfFonts.get(font), text, size, color, x, y, maxWidth, fadeWidth);
   }

   public static void drawIcon(GuiGraphicsExtractor ctx, GIcons icon, float x, float y, float size, int color) {
      if (icon != null) {
         String texturePath = switch (icon) {
            case FRAME -> "textures/gui/icons/frame.png";
            case KEYBOARD -> "textures/gui/icons/keyboard.png";
            case CHECKMARK -> "textures/gui/icons/checkmark.png";
            case LOGO -> "textures/gui/icons/logo.png";
         };
         drawImage(ctx, texturePath, x, y, size, size, 0.0F, color, 0.0F);
      }
   }

   public static void drawIconGlyph(GuiGraphicsExtractor ctx, GIcons icon, float x, float y, float size, int color) {
      if (icon != null) {
         MsdfFont font = getIconMsdfFont();
         if (font == null) {
            drawIcon(ctx, icon, x, y, size, color);
         } else {
            MsdfRenderer.drawString(ctx, font, String.valueOf(icon.glyph()), size, color, x, y - size * 0.08F);
         }
      }
   }

   public static float getIconGlyphWidth(GIcons icon, float size) {
      if (icon == null) {
         return 0.0F;
      }

      MsdfFont font = getIconMsdfFont();
      return font == null ? size : font.getWidth(String.valueOf(icon.glyph()), size);
   }

   public static void drawText(GuiGraphicsExtractor ctx, Component text, float x, float y, int color, GFonts font, int size) {
      if (text != null) {
         float[] cursor = new float[]{x};
         int baseAlpha = color >>> 24 & 0xFF;
         text.visit((style, segment) -> {
            if (segment != null && !segment.isEmpty()) {
               GFonts segmentFont = style != null && style.isBold() ? GFonts.BOLD : font;
               TextColor styleColor = style == null ? null : style.getColor();
               int rgb = styleColor == null ? color & 16777215 : styleColor.getValue();
               int segmentColor = baseAlpha << 24 | rgb & 16777215;
               MsdfRenderer.drawString(ctx, MsdfFonts.get(segmentFont), segment, size, segmentColor, cursor[0], y);
               cursor[0] += getTextWidth(segment, segmentFont, size);
               return Optional.empty();
            } else {
               return Optional.empty();
            }
         }, Style.EMPTY);
      }
   }

   public static void drawCircle(GuiGraphicsExtractor ctx, float x, float y, float diameter, int color) {
      drawCircle(ctx, x, y, diameter, color, 1.0F);
   }

   public static void drawCircle(GuiGraphicsExtractor ctx, float x, float y, float diameter, int color, float smoothness) {
      new CircleRenderer(diameter, color, smoothness).render(x, y, ctx);
   }

   public static void drawRingProgress(GuiGraphicsExtractor ctx, float x, float y, float diameter, float progress, float thickness, int color, float smoothness) {
      new RingProgressRenderer(diameter, color, progress, thickness, smoothness).render(x, y, ctx);
   }

   public static void pushScissor(GuiGraphicsExtractor ctx, float x, float y, float w, float h) {
      ScissorUtil.push(ctx, x, y, w, h);
   }

   public static void popScissor() {
      ScissorUtil.pop();
   }

   public static void clearScissor() {
      ScissorUtil.clear();
   }

   public static float getTextWidth(String text, GFonts font, int size) {
      MsdfFont msdfFont = MsdfFonts.get(font);
      return msdfFont.getWidth(text, size);
   }

   public static float getTextWidth(String text, GFonts font, float size) {
      MsdfFont msdfFont = MsdfFonts.get(font);
      return msdfFont.getWidth(text, size);
   }

   public static float getTextHeight(GFonts font, int size) {
      MsdfFont msdfFont = MsdfFonts.get(font);
      return msdfFont.lineHeight(size);
   }

   public static float getTextHeight(GFonts font, float size) {
      MsdfFont msdfFont = MsdfFonts.get(font);
      return msdfFont.lineHeight(size);
   }

   public static float getTextHeight(String text, GFonts font, int size) {
      return getTextHeight(font, size);
   }

   public static float getTextHeight(String text, GFonts font, float size) {
      return getTextHeight(font, size);
   }

   public static float getTextWidth(Component text, GFonts font, int size) {
      if (text == null) {
         return 0.0F;
      }

      float[] width = new float[]{0.0F};
      text.visit((style, segment) -> {
         if (segment != null && !segment.isEmpty()) {
            GFonts segmentFont = style != null && style.isBold() ? GFonts.BOLD : font;
            width[0] += getTextWidth(segment, segmentFont, size);
            return Optional.empty();
         } else {
            return Optional.empty();
         }
      }, Style.EMPTY);
      return width[0];
   }

   public static float getTextHeight(Component text, GFonts font, int size) {
      return getTextHeight(font, size);
   }

   private static MsdfFont getIconMsdfFont() {
      if (iconMsdfFont != null) {
         return iconMsdfFont;
      }

      try {
         iconMsdfFont = MsdfFont.load(
            Identifier.fromNamespaceAndPath("havoc", "fonts/msdf/icons.json"), Identifier.fromNamespaceAndPath("havoc", "fonts/msdf/icons.png")
         );
         return iconMsdfFont;
      } catch (Exception ignored) {
         return null;
      }
   }

   private static int accelerateBorderFade(int argb) {
      int a = argb >>> 24 & 0xFF;
      int rgb = argb & 16777215;
      int fasterA = a * a * a / 65025;
      return (fasterA & 0xFF) << 24 | rgb;
   }
}
