package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderType;

public interface MultiBufferSource {
   VertexConsumer getBuffer(RenderType var1);

   final class BufferSource implements MultiBufferSource {
      private final SubmitNodeCollector collector;
      private final Map<RenderType, MultiBufferSource.RecordingVertexConsumer> buffers = new LinkedHashMap<>();

      public BufferSource(SubmitNodeCollector collector) {
         this.collector = collector;
      }

      @Override
      public VertexConsumer getBuffer(RenderType renderType) {
         return this.buffers.computeIfAbsent(renderType, ignored -> new MultiBufferSource.RecordingVertexConsumer());
      }

      public void draw(RenderType renderType) {
         MultiBufferSource.RecordingVertexConsumer buffer = this.buffers.remove(renderType);
         if (buffer != null && !buffer.vertices.isEmpty()) {
            PoseStack poseStack = new PoseStack();
            this.collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> buffer.replay(consumer));
         }
      }

      public void draw() {
         for (RenderType type : new ArrayList<>(this.buffers.keySet())) {
            this.draw(type);
         }
      }

      public void endBatch(RenderType renderType) {
         this.draw(renderType);
      }

      public void endBatch() {
         this.draw();
      }
   }

   final class RecordingVertexConsumer implements VertexConsumer {
      private final List<MultiBufferSource.Vertex> vertices = new ArrayList<>();
      private MultiBufferSource.Vertex current;

      public VertexConsumer addVertex(float x, float y, float z) {
         this.current = new MultiBufferSource.Vertex(x, y, z);
         this.vertices.add(this.current);
         return this;
      }

      public VertexConsumer setColor(int red, int green, int blue, int alpha) {
         return this.setColor(alpha << 24 | red << 16 | green << 8 | blue);
      }

      public VertexConsumer setColor(int argb) {
         if (this.current != null) {
            this.current.color = argb;
            this.current.hasColor = true;
         }

         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         if (this.current != null) {
            this.current.u = u;
            this.current.v = v;
            this.current.hasUv = true;
         }

         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         if (this.current != null) {
            this.current.uv1U = u;
            this.current.uv1V = v;
            this.current.hasUv1 = true;
         }

         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         if (this.current != null) {
            this.current.uv2U = u;
            this.current.uv2V = v;
            this.current.hasUv2 = true;
         }

         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         if (this.current != null) {
            this.current.normalX = x;
            this.current.normalY = y;
            this.current.normalZ = z;
            this.current.hasNormal = true;
         }

         return this;
      }

      public VertexConsumer setLineWidth(float width) {
         if (this.current != null) {
            this.current.lineWidth = width;
            this.current.hasLineWidth = true;
         }

         return this;
      }

      private void replay(VertexConsumer target) {
         for (MultiBufferSource.Vertex vertex : this.vertices) {
            target.addVertex(vertex.x, vertex.y, vertex.z);
            if (vertex.hasColor) {
               target.setColor(vertex.color);
            }

            if (vertex.hasUv) {
               target.setUv(vertex.u, vertex.v);
            }

            if (vertex.hasUv1) {
               target.setUv1(vertex.uv1U, vertex.uv1V);
            }

            if (vertex.hasUv2) {
               target.setUv2(vertex.uv2U, vertex.uv2V);
            }

            if (vertex.hasNormal) {
               target.setNormal(vertex.normalX, vertex.normalY, vertex.normalZ);
            }

            if (vertex.hasLineWidth) {
               target.setLineWidth(vertex.lineWidth);
            }
         }
      }
   }

   final class Vertex {
      private final float x;
      private final float y;
      private final float z;
      private int color;
      private float u;
      private float v;
      private int uv1U;
      private int uv1V;
      private int uv2U;
      private int uv2V;
      private float normalX;
      private float normalY;
      private float normalZ;
      private float lineWidth;
      private boolean hasColor;
      private boolean hasUv;
      private boolean hasUv1;
      private boolean hasUv2;
      private boolean hasNormal;
      private boolean hasLineWidth;

      private Vertex(float x, float y, float z) {
         this.x = x;
         this.y = y;
         this.z = z;
      }
   }
}
