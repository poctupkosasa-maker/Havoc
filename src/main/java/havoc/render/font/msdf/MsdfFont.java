package havoc.render.font.msdf;

import com.google.gson.Gson;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public final class MsdfFont {
   private static final Gson GSON = new Gson();
   private final AbstractTexture texture;
   private final FontData.AtlasData atlas;
   private final FontData.MetricsData metrics;
   private final Map<Integer, MsdfGlyph> glyphs;
   private final Map<Integer, Map<Integer, Float>> kernings;
   private final ConcurrentHashMap<Long, Float> widthCache = new ConcurrentHashMap<>();

   private MsdfFont(
      AbstractTexture texture,
      FontData.AtlasData atlas,
      FontData.MetricsData metrics,
      Map<Integer, MsdfGlyph> glyphs,
      Map<Integer, Map<Integer, Float>> kernings
   ) {
      this.texture = texture;
      this.atlas = atlas;
      this.metrics = metrics;
      this.glyphs = glyphs;
      this.kernings = kernings;
   }

   public static MsdfFont load(Identifier jsonId, Identifier atlasId) {
      try {
         Minecraft client = Minecraft.getInstance();
         if (client == null) {
            throw new IllegalStateException("MinecraftClient is not initialized");
         }

         FontData data;
         try (
            InputStream in = client.getResourceManager().open(jsonId);
            InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
         ) {
            data = (FontData)GSON.fromJson(reader, FontData.class);
         }

         if (data != null && data.atlas() != null && data.metrics() != null) {
            AbstractTexture texture = client.getTextureManager().getTexture(atlasId);
            float atlasW = data.atlas().width();
            float atlasH = data.atlas().height();
            Map<Integer, MsdfGlyph> glyphs = new HashMap<>();
            if (data.glyphs() != null) {
               for (FontData.GlyphData glyphData : data.glyphs()) {
                  glyphs.put(glyphData.unicode(), new MsdfGlyph(glyphData, atlasW, atlasH));
               }
            }

            Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
            if (data.kernings() != null) {
               for (FontData.KerningData kerning : data.kernings()) {
                  kernings.computeIfAbsent(kerning.leftChar(), k -> new HashMap<>()).put(kerning.rightChar(), kerning.advance());
               }
            }

            return new MsdfFont(texture, data.atlas(), data.metrics(), glyphs, kernings);
         } else {
            throw new IllegalStateException("Invalid MSDF data: " + jsonId);
         }
      } catch (Exception e) {
         throw new RuntimeException("Failed to load MSDF font: " + jsonId + " / " + atlasId, e);
      }
   }

   public GpuTextureView textureView() {
      return this.texture.getTextureView();
   }

   public float range() {
      return this.atlas.range();
   }

   public float lineHeight(float size) {
      return this.metrics.lineHeight() * size;
   }

   public float getWidth(String text, float size) {
      if (text != null && !text.isEmpty()) {
         long key = text.hashCode() & 4294967295L ^ (long)Float.floatToIntBits(size) << 32;
         Float cached = this.widthCache.get(key);
         if (cached != null) {
            return cached;
         }

         float width = this.computeWidth(text, size);
         this.widthCache.put(key, width);
         return width;
      } else {
         return 0.0F;
      }
   }

   public void applyGlyphs(
      Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color
   ) {
      if (text != null && !text.isEmpty()) {
         int prevCodePoint = -1;
         int i = 0;

         while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);
            MsdfGlyph glyph = this.glyphs.get(codePoint);
            if (glyph != null) {
               Map<Integer, Float> kerning = this.kernings.get(prevCodePoint);
               if (kerning != null) {
                  x += kerning.getOrDefault(codePoint, 0.0F) * size;
               }

               x += glyph.apply(matrix, consumer, size, x, y, z, color) + thickness + spacing;
               prevCodePoint = codePoint;
            }
         }
      }
   }

   public MsdfGlyph getGlyph(int codePoint) {
      return this.glyphs.get(codePoint);
   }

   public float getKerningAdvance(int leftCodePoint, int rightCodePoint, float size) {
      Map<Integer, Float> kerning = this.kernings.get(leftCodePoint);
      return kerning == null ? 0.0F : kerning.getOrDefault(rightCodePoint, 0.0F) * size;
   }

   private float computeWidth(String text, float size) {
      int prevCodePoint = -1;
      float width = 0.0F;
      int i = 0;

      while (i < text.length()) {
         int codePoint = text.codePointAt(i);
         i += Character.charCount(codePoint);
         MsdfGlyph glyph = this.glyphs.get(codePoint);
         if (glyph != null) {
            Map<Integer, Float> kerning = this.kernings.get(prevCodePoint);
            if (kerning != null) {
               width += kerning.getOrDefault(codePoint, 0.0F) * size;
            }

            width += glyph.getAdvance(size) + 0.25F;
            prevCodePoint = codePoint;
         }
      }

      return width;
   }
}
