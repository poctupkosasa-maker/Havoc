package havoc.render.font.msdf;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import havoc.render.renderers.pipeline.PipelineRegistry;
import me.x150.renderer.mixin.DrawContextAccessor;
import me.x150.renderer.render.SimpleGuiRenderState;
import me.x150.renderer.util.DirectVertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

public final class MsdfRenderer {
   private MsdfRenderer() {
   }

   public static void drawString(GuiGraphicsExtractor context, MsdfFont font, String text, float size, int color, float x, float y) {
      if (text != null && !text.isEmpty() && PipelineRegistry.MSDF_FONT_PIPELINE != null) {
         float textWidth = font.getWidth(text, size);
         float textHeight = font.lineHeight(size);
         if (!(textWidth <= 0.0F) && !(textHeight <= 0.0F)) {
            GpuTextureView textureView = font.textureView();
            Matrix3x2f guiMatrices = new Matrix3x2f(context.pose());
            SimpleGuiRenderState state = new SimpleGuiRenderState(
               PipelineRegistry.MSDF_FONT_PIPELINE,
               TextureSetup.singleTexture(textureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),
               context,
               createBounds(context, x, y, textWidth + 2.0F, textHeight + 2.0F),
               buffer -> {
                  DirectVertexConsumer dvc = new DirectVertexConsumer((BufferBuilder)buffer, false);
                  float thickness = 0.05F;
                  float spacing = 0.0F;
                  float cursorX = x - 0.75F;
                  float baseY = y + size * 0.7F;
                  int prevCodePoint = -1;
                  int outColor = toAbgr(color);
                  int i = 0;

                  while (i < text.length()) {
                     int codePoint = text.codePointAt(i);
                     i += Character.charCount(codePoint);
                     MsdfGlyph glyph = font.getGlyph(codePoint);
                     if (glyph != null) {
                        cursorX += font.getKerningAdvance(prevCodePoint, codePoint, size);
                        float gy = baseY - glyph.topPosition() * size;
                        float gw = glyph.width() * size;
                        float gh = glyph.height() * size;
                        dvc.addVertexWith2DPose(guiMatrices, cursorX, gy).setUv(glyph.minU(), glyph.minV()).setColor(outColor);
                        dvc.addVertexWith2DPose(guiMatrices, cursorX, gy + gh).setUv(glyph.minU(), glyph.maxV()).setColor(outColor);
                        dvc.addVertexWith2DPose(guiMatrices, cursorX + gw, gy + gh).setUv(glyph.maxU(), glyph.maxV()).setColor(outColor);
                        dvc.addVertexWith2DPose(guiMatrices, cursorX + gw, gy).setUv(glyph.maxU(), glyph.minV()).setColor(outColor);
                        cursorX += glyph.getAdvance(size) + thickness * 0.5F * size + spacing;
                        prevCodePoint = codePoint;
                     }
                  }
               }
            );
            ((DrawContextAccessor)context).getState().addGuiElement(state);
         }
      }
   }

   public static void drawStringFadeRight(
      GuiGraphicsExtractor context, MsdfFont font, String text, float size, int color, float x, float y, float maxWidth, float fadeWidth
   ) {
      if (text != null && !text.isEmpty() && PipelineRegistry.MSDF_FONT_PIPELINE != null && !(maxWidth <= 0.0F)) {
         float textWidth = font.getWidth(text, size);
         float textHeight = font.lineHeight(size);
         if (!(textWidth <= 0.0F) && !(textHeight <= 0.0F)) {
            float fadeSpan = Math.max(1.0F, Math.min(maxWidth, fadeWidth));
            float visibleEndX = x + maxWidth;
            float fadeStartX = visibleEndX - fadeSpan;
            int baseAlpha = color >>> 24 & 0xFF;
            int rgb = color & 16777215;
            GpuTextureView textureView = font.textureView();
            Matrix3x2f guiMatrices = new Matrix3x2f(context.pose());
            SimpleGuiRenderState state = new SimpleGuiRenderState(
               PipelineRegistry.MSDF_FONT_PIPELINE,
               TextureSetup.singleTexture(textureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),
               context,
               createBounds(context, x, y, Math.min(textWidth, maxWidth) + 2.0F, textHeight + 2.0F),
               buffer -> {
                  DirectVertexConsumer dvc = new DirectVertexConsumer((BufferBuilder)buffer, false);
                  float thickness = 0.05F;
                  float spacing = 0.0F;
                  float cursorX = x - 0.75F;
                  float baseY = y + size * 0.7F;
                  int prevCodePoint = -1;
                  int i = 0;

                  while (i < text.length()) {
                     int codePoint = text.codePointAt(i);
                     i += Character.charCount(codePoint);
                     MsdfGlyph glyph = font.getGlyph(codePoint);
                     if (glyph != null) {
                        cursorX += font.getKerningAdvance(prevCodePoint, codePoint, size);
                        float gy = baseY - glyph.topPosition() * size;
                        float gw = glyph.width() * size;
                        float gh = glyph.height() * size;
                        float glyphCenter = cursorX + gw * 0.5F;
                        float alphaMul = 1.0F;
                        if (glyphCenter > fadeStartX) {
                           float t = clamp01((glyphCenter - fadeStartX) / fadeSpan);
                           alphaMul = 1.0F - smootherStep01(t);
                        }

                        int outA = Math.round(baseAlpha * alphaMul);
                        if (outA > 0) {
                           int glyphColor = toAbgr((outA & 0xFF) << 24 | rgb);
                           dvc.addVertexWith2DPose(guiMatrices, cursorX, gy).setUv(glyph.minU(), glyph.minV()).setColor(glyphColor);
                           dvc.addVertexWith2DPose(guiMatrices, cursorX, gy + gh).setUv(glyph.minU(), glyph.maxV()).setColor(glyphColor);
                           dvc.addVertexWith2DPose(guiMatrices, cursorX + gw, gy + gh).setUv(glyph.maxU(), glyph.maxV()).setColor(glyphColor);
                           dvc.addVertexWith2DPose(guiMatrices, cursorX + gw, gy).setUv(glyph.maxU(), glyph.minV()).setColor(glyphColor);
                        }

                        cursorX += glyph.getAdvance(size) + thickness * 0.5F * size + spacing;
                        prevCodePoint = codePoint;
                        if (cursorX > visibleEndX + 2.0F) {
                           break;
                        }
                     }
                  }
               }
            );
            ((DrawContextAccessor)context).getState().addGuiElement(state);
         }
      }
   }

   private static ScreenRectangle createBounds(GuiGraphicsExtractor context, float x, float y, float w, float h) {
      Matrix3x2fStack mat = context.pose();
      return new ScreenRectangle((int)Math.floor(x), (int)Math.floor(y), (int)Math.ceil(w), (int)Math.ceil(h)).transformMaxBounds(mat);
   }

   private static float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private static float smootherStep01(float t) {
      float x = clamp01(t);
      return x * x * x * (x * (x * 6.0F - 15.0F) + 10.0F);
   }

   private static int toAbgr(int argb) {
      int a = argb >>> 24 & 0xFF;
      int r = argb >>> 16 & 0xFF;
      int g = argb >>> 8 & 0xFF;
      int b = argb & 0xFF;
      return (a & 0xFF) << 24 | (b & 0xFF) << 16 | (g & 0xFF) << 8 | r & 0xFF;
   }
}
