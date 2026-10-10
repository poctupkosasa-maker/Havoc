package havoc.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class RenderUtilWorld3D {
   private RenderUtilWorld3D() {
   }

   public static void drawLineStrip(PoseStack matrices, VertexConsumer consumer, List<Vec3> points, int color) {
      if (matrices != null && consumer != null && points != null && points.size() >= 2) {
         Pose entry = matrices.last();
         float r = (color >> 16 & 0xFF) / 255.0F;
         float g = (color >> 8 & 0xFF) / 255.0F;
         float b = (color & 0xFF) / 255.0F;
         float a = (color >> 24 & 0xFF) / 255.0F;

         for (int i = 0; i < points.size() - 1; i++) {
            Vec3 from = points.get(i);
            Vec3 to = points.get(i + 1);
            drawLine(consumer, entry, from, to, r, g, b, a);
         }
      }
   }

   public static void drawLine(PoseStack matrices, VertexConsumer consumer, Vec3 from, Vec3 to, int color) {
      if (matrices != null && consumer != null && from != null && to != null) {
         Pose entry = matrices.last();
         float r = (color >> 16 & 0xFF) / 255.0F;
         float g = (color >> 8 & 0xFF) / 255.0F;
         float b = (color & 0xFF) / 255.0F;
         float a = (color >> 24 & 0xFF) / 255.0F;
         drawLine(consumer, entry, from, to, r, g, b, a);
      }
   }

   public static void drawLine(PoseStack matrices, BufferBuilder builder, Vec3 from, Vec3 to, int color) {
      if (matrices != null && builder != null && from != null && to != null) {
         Pose entry = matrices.last();
         Matrix4f matrix = entry.pose();
         float r = (color >> 16 & 0xFF) / 255.0F;
         float g = (color >> 8 & 0xFF) / 255.0F;
         float b = (color & 0xFF) / 255.0F;
         float a = (color >> 24 & 0xFF) / 255.0F;
         builder.addVertex(matrix, (float)from.x, (float)from.y, (float)from.z).setColor(r, g, b, a);
         builder.addVertex(matrix, (float)to.x, (float)to.y, (float)to.z).setColor(r, g, b, a);
      }
   }

   public static void drawCross(PoseStack matrices, VertexConsumer consumer, Vec3 center, float radius, int color) {
      if (center != null && !(radius <= 0.0F)) {
         drawLine(matrices, consumer, center.add(-radius, 0.0, 0.0), center.add(radius, 0.0, 0.0), color);
         drawLine(matrices, consumer, center.add(0.0, -radius, 0.0), center.add(0.0, radius, 0.0), color);
         drawLine(matrices, consumer, center.add(0.0, 0.0, -radius), center.add(0.0, 0.0, radius), color);
      }
   }

   private static void drawLine(VertexConsumer consumer, Pose entry, Vec3 from, Vec3 to, float r, float g, float b, float a) {
      consumer.addVertex(entry, (float)from.x, (float)from.y, (float)from.z).setColor(r, g, b, a);
      consumer.addVertex(entry, (float)to.x, (float)to.y, (float)to.z).setColor(r, g, b, a);
   }
}
