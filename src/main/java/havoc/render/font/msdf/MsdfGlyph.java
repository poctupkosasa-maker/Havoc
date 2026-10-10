package havoc.render.font.msdf;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

public final class MsdfGlyph {
   private final float minU;
   private final float maxU;
   private final float minV;
   private final float maxV;
   private final float advance;
   private final float topPosition;
   private final float width;
   private final float height;

   public MsdfGlyph(FontData.GlyphData data, float atlasWidth, float atlasHeight) {
      this.advance = data.advance();
      FontData.BoundsData atlasBounds = data.atlasBounds();
      if (atlasBounds != null) {
         this.minU = atlasBounds.left() / atlasWidth;
         this.maxU = atlasBounds.right() / atlasWidth;
         this.minV = 1.0F - atlasBounds.top() / atlasHeight;
         this.maxV = 1.0F - atlasBounds.bottom() / atlasHeight;
      } else {
         this.minU = 0.0F;
         this.maxU = 0.0F;
         this.minV = 0.0F;
         this.maxV = 0.0F;
      }

      FontData.BoundsData planeBounds = data.planeBounds();
      if (planeBounds != null) {
         this.width = planeBounds.right() - planeBounds.left();
         this.height = planeBounds.top() - planeBounds.bottom();
         this.topPosition = planeBounds.top();
      } else {
         this.width = 0.0F;
         this.height = 0.0F;
         this.topPosition = 0.0F;
      }
   }

   public float apply(Matrix4f matrix, VertexConsumer consumer, float size, float x, float y, float z, int color) {
      y -= this.topPosition * size;
      float w = this.width * size;
      float h = this.height * size;
      consumer.addVertex(matrix, x, y, z).setUv(this.minU, this.minV).setColor(color);
      consumer.addVertex(matrix, x, y + h, z).setUv(this.minU, this.maxV).setColor(color);
      consumer.addVertex(matrix, x + w, y + h, z).setUv(this.maxU, this.maxV).setColor(color);
      consumer.addVertex(matrix, x + w, y, z).setUv(this.maxU, this.minV).setColor(color);
      return this.advance * size;
   }

   public float getAdvance(float size) {
      return this.advance * size;
   }

   public float minU() {
      return this.minU;
   }

   public float maxU() {
      return this.maxU;
   }

   public float minV() {
      return this.minV;
   }

   public float maxV() {
      return this.maxV;
   }

   public float topPosition() {
      return this.topPosition;
   }

   public float width() {
      return this.width;
   }

   public float height() {
      return this.height;
   }
}
