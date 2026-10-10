package havoc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import havoc.utilites.color.ColorUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Render3D {
   private Render3D() {
   }

   public static void drawOutlinedBox(PoseStack matrices, VertexConsumer consumer, AABB box, int color) {
      Pose entry = matrices.last();
      float r = ColorUtil.red(color) / 255.0F;
      float g = ColorUtil.green(color) / 255.0F;
      float b = ColorUtil.blue(color) / 255.0F;
      float a = ColorUtil.alpha(color) / 255.0F;
      float x1 = (float)box.minX;
      float y1 = (float)box.minY;
      float z1 = (float)box.minZ;
      float x2 = (float)box.maxX;
      float y2 = (float)box.maxY;
      float z2 = (float)box.maxZ;
      drawLine(consumer, entry, x1, y1, z1, x2, y1, z1, r, g, b, a);
      drawLine(consumer, entry, x2, y1, z1, x2, y1, z2, r, g, b, a);
      drawLine(consumer, entry, x2, y1, z2, x1, y1, z2, r, g, b, a);
      drawLine(consumer, entry, x1, y1, z2, x1, y1, z1, r, g, b, a);
      drawLine(consumer, entry, x1, y2, z1, x2, y2, z1, r, g, b, a);
      drawLine(consumer, entry, x2, y2, z1, x2, y2, z2, r, g, b, a);
      drawLine(consumer, entry, x2, y2, z2, x1, y2, z2, r, g, b, a);
      drawLine(consumer, entry, x1, y2, z2, x1, y2, z1, r, g, b, a);
      drawLine(consumer, entry, x1, y1, z1, x1, y2, z1, r, g, b, a);
      drawLine(consumer, entry, x2, y1, z1, x2, y2, z1, r, g, b, a);
      drawLine(consumer, entry, x2, y1, z2, x2, y2, z2, r, g, b, a);
      drawLine(consumer, entry, x1, y1, z2, x1, y2, z2, r, g, b, a);
   }

   public static void drawFilledBox(PoseStack matrices, VertexConsumer consumer, AABB box, int color) {
      if (matrices != null && consumer != null && box != null) {
         Pose entry = matrices.last();
         float r = ColorUtil.red(color) / 255.0F;
         float g = ColorUtil.green(color) / 255.0F;
         float b = ColorUtil.blue(color) / 255.0F;
         float a = ColorUtil.alpha(color) / 255.0F;
         float x1 = (float)box.minX;
         float y1 = (float)box.minY;
         float z1 = (float)box.minZ;
         float x2 = (float)box.maxX;
         float y2 = (float)box.maxY;
         float z2 = (float)box.maxZ;
         addQuad(consumer, entry, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a);
         addQuad(consumer, entry, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, r, g, b, a);
         addQuad(consumer, entry, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, r, g, b, a);
         addQuad(consumer, entry, x1, y2, z2, x2, y2, z2, x2, y1, z2, x1, y1, z2, r, g, b, a);
         addQuad(consumer, entry, x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2, r, g, b, a);
         addQuad(consumer, entry, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, r, g, b, a);
      }
   }

   public static void drawOutlinedOrientedBox(
      PoseStack matrices, VertexConsumer consumer, Vec3 center, float halfX, float halfY, float halfZ, float yawDeg, float pitchDeg, int color
   ) {
      if (matrices != null && consumer != null && center != null) {
         Pose entry = matrices.last();
         float r = ColorUtil.red(color) / 255.0F;
         float g = ColorUtil.green(color) / 255.0F;
         float b = ColorUtil.blue(color) / 255.0F;
         float a = ColorUtil.alpha(color) / 255.0F;
         Vec3[] vertices = orientedBoxVertices(center, halfX, halfY, halfZ, yawDeg, pitchDeg);
         drawLine(
            consumer,
            entry,
            (float)vertices[0].x,
            (float)vertices[0].y,
            (float)vertices[0].z,
            (float)vertices[1].x,
            (float)vertices[1].y,
            (float)vertices[1].z,
            r,
            g,
            b,
            a
         );
         drawLine(
            consumer,
            entry,
            (float)vertices[1].x,
            (float)vertices[1].y,
            (float)vertices[1].z,
            (float)vertices[2].x,
            (float)vertices[2].y,
            (float)vertices[2].z,
            r,
            g,
            b,
            a
         );
         drawLine(
            consumer,
            entry,
            (float)vertices[2].x,
            (float)vertices[2].y,
            (float)vertices[2].z,
            (float)vertices[3].x,
            (float)vertices[3].y,
            (float)vertices[3].z,
            r,
            g,
            b,
            a
         );
         drawLine(
            consumer,
            entry,
            (float)vertices[3].x,
            (float)vertices[3].y,
            (float)vertices[3].z,
            (float)vertices[0].x,
            (float)vertices[0].y,
            (float)vertices[0].z,
            r,
            g,
            b,
            a
         );
         drawLine(
            consumer,
            entry,
            (float)vertices[4].x,
            (float)vertices[4].y,
            (float)vertices[4].z,
            (float)vertices[5].x,
            (float)vertices[5].y,
            (float)vertices[5].z,
            r,
            g,
            b,
            a
         );
         drawLine(
            consumer,
            entry,
            (float)vertices[5].x,
            (float)vertices[5].y,
            (float)vertices[5].z,
            (float)vertices[6].x,
            (float)vertices[6].y,
            (float)vertices[6].z,
            r,
            g,
            b,
            a
         );
         drawLine(
            consumer,
            entry,
            (float)vertices[6].x,
            (float)vertices[6].y,
            (float)vertices[6].z,
            (float)vertices[7].x,
            (float)vertices[7].y,
            (float)vertices[7].z,
            r,
            g,
            b,
            a
         );
         drawLine(
            consumer,
            entry,
            (float)vertices[7].x,
            (float)vertices[7].y,
            (float)vertices[7].z,
            (float)vertices[4].x,
            (float)vertices[4].y,
            (float)vertices[4].z,
            r,
            g,
            b,
            a
         );
         drawLine(
            consumer,
            entry,
            (float)vertices[0].x,
            (float)vertices[0].y,
            (float)vertices[0].z,
            (float)vertices[4].x,
            (float)vertices[4].y,
            (float)vertices[4].z,
            r,
            g,
            b,
            a
         );
         drawLine(
            consumer,
            entry,
            (float)vertices[1].x,
            (float)vertices[1].y,
            (float)vertices[1].z,
            (float)vertices[5].x,
            (float)vertices[5].y,
            (float)vertices[5].z,
            r,
            g,
            b,
            a
         );
         drawLine(
            consumer,
            entry,
            (float)vertices[2].x,
            (float)vertices[2].y,
            (float)vertices[2].z,
            (float)vertices[6].x,
            (float)vertices[6].y,
            (float)vertices[6].z,
            r,
            g,
            b,
            a
         );
         drawLine(
            consumer,
            entry,
            (float)vertices[3].x,
            (float)vertices[3].y,
            (float)vertices[3].z,
            (float)vertices[7].x,
            (float)vertices[7].y,
            (float)vertices[7].z,
            r,
            g,
            b,
            a
         );
      }
   }

   public static void drawFilledOrientedBox(
      PoseStack matrices, VertexConsumer consumer, Vec3 center, float halfX, float halfY, float halfZ, float yawDeg, float pitchDeg, int color
   ) {
      if (matrices != null && consumer != null && center != null) {
         Pose entry = matrices.last();
         float r = ColorUtil.red(color) / 255.0F;
         float g = ColorUtil.green(color) / 255.0F;
         float b = ColorUtil.blue(color) / 255.0F;
         float a = ColorUtil.alpha(color) / 255.0F;
         Vec3[] v = orientedBoxVertices(center, halfX, halfY, halfZ, yawDeg, pitchDeg);
         addQuad(
            consumer,
            entry,
            (float)v[0].x,
            (float)v[0].y,
            (float)v[0].z,
            (float)v[1].x,
            (float)v[1].y,
            (float)v[1].z,
            (float)v[2].x,
            (float)v[2].y,
            (float)v[2].z,
            (float)v[3].x,
            (float)v[3].y,
            (float)v[3].z,
            r,
            g,
            b,
            a
         );
         addQuad(
            consumer,
            entry,
            (float)v[7].x,
            (float)v[7].y,
            (float)v[7].z,
            (float)v[6].x,
            (float)v[6].y,
            (float)v[6].z,
            (float)v[5].x,
            (float)v[5].y,
            (float)v[5].z,
            (float)v[4].x,
            (float)v[4].y,
            (float)v[4].z,
            r,
            g,
            b,
            a
         );
         addQuad(
            consumer,
            entry,
            (float)v[0].x,
            (float)v[0].y,
            (float)v[0].z,
            (float)v[1].x,
            (float)v[1].y,
            (float)v[1].z,
            (float)v[5].x,
            (float)v[5].y,
            (float)v[5].z,
            (float)v[4].x,
            (float)v[4].y,
            (float)v[4].z,
            r,
            g,
            b,
            a
         );
         addQuad(
            consumer,
            entry,
            (float)v[3].x,
            (float)v[3].y,
            (float)v[3].z,
            (float)v[2].x,
            (float)v[2].y,
            (float)v[2].z,
            (float)v[6].x,
            (float)v[6].y,
            (float)v[6].z,
            (float)v[7].x,
            (float)v[7].y,
            (float)v[7].z,
            r,
            g,
            b,
            a
         );
         addQuad(
            consumer,
            entry,
            (float)v[0].x,
            (float)v[0].y,
            (float)v[0].z,
            (float)v[3].x,
            (float)v[3].y,
            (float)v[3].z,
            (float)v[7].x,
            (float)v[7].y,
            (float)v[7].z,
            (float)v[4].x,
            (float)v[4].y,
            (float)v[4].z,
            r,
            g,
            b,
            a
         );
         addQuad(
            consumer,
            entry,
            (float)v[1].x,
            (float)v[1].y,
            (float)v[1].z,
            (float)v[2].x,
            (float)v[2].y,
            (float)v[2].z,
            (float)v[6].x,
            (float)v[6].y,
            (float)v[6].z,
            (float)v[5].x,
            (float)v[5].y,
            (float)v[5].z,
            r,
            g,
            b,
            a
         );
      }
   }

   public static void drawLine(PoseStack matrices, VertexConsumer consumer, Vec3 from, Vec3 to, int color) {
      if (matrices != null && consumer != null && from != null && to != null) {
         Pose entry = matrices.last();
         float r = ColorUtil.red(color) / 255.0F;
         float g = ColorUtil.green(color) / 255.0F;
         float b = ColorUtil.blue(color) / 255.0F;
         float a = ColorUtil.alpha(color) / 255.0F;
         drawLine(consumer, entry, (float)from.x, (float)from.y, (float)from.z, (float)to.x, (float)to.y, (float)to.z, r, g, b, a);
      }
   }

   private static void drawLine(
      VertexConsumer consumer, Pose entry, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a
   ) {
      float nx = x2 - x1;
      float ny = y2 - y1;
      float nz = z2 - z1;
      float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
      if (len > 1.0E-4F) {
         nx /= len;
         ny /= len;
         nz /= len;
      } else {
         nx = 0.0F;
         ny = 1.0F;
         nz = 0.0F;
      }

      consumer.addVertex(entry, x1, y1, z1).setColor(r, g, b, a).setLineWidth(1.0F).setNormal(entry, nx, ny, nz);
      consumer.addVertex(entry, x2, y2, z2).setColor(r, g, b, a).setLineWidth(1.0F).setNormal(entry, nx, ny, nz);
   }

   private static void addQuad(
      VertexConsumer consumer,
      Pose entry,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float x4,
      float y4,
      float z4,
      float r,
      float g,
      float b,
      float a
   ) {
      consumer.addVertex(entry, x1, y1, z1).setColor(r, g, b, a);
      consumer.addVertex(entry, x2, y2, z2).setColor(r, g, b, a);
      consumer.addVertex(entry, x3, y3, z3).setColor(r, g, b, a);
      consumer.addVertex(entry, x4, y4, z4).setColor(r, g, b, a);
   }

   private static Vec3[] orientedBoxVertices(Vec3 center, float halfX, float halfY, float halfZ, float yawDeg, float pitchDeg) {
      Vec3[] local = new Vec3[]{
         new Vec3(-halfX, -halfY, -halfZ),
         new Vec3(halfX, -halfY, -halfZ),
         new Vec3(halfX, -halfY, halfZ),
         new Vec3(-halfX, -halfY, halfZ),
         new Vec3(-halfX, halfY, -halfZ),
         new Vec3(halfX, halfY, -halfZ),
         new Vec3(halfX, halfY, halfZ),
         new Vec3(-halfX, halfY, halfZ)
      };
      Vec3[] out = new Vec3[local.length];

      for (int i = 0; i < local.length; i++) {
         Vec3 r = rotateYawPitch(local[i], yawDeg, pitchDeg);
         out[i] = center.add(r);
      }

      return out;
   }

   private static Vec3 rotateYawPitch(Vec3 vec, float yawDeg, float pitchDeg) {
      double yaw = Math.toRadians(yawDeg);
      double pitch = Math.toRadians(-pitchDeg);
      double cosYaw = Math.cos(yaw);
      double sinYaw = Math.sin(yaw);
      double xzX = vec.x * cosYaw - vec.z * sinYaw;
      double xzZ = vec.x * sinYaw + vec.z * cosYaw;
      double cosPitch = Math.cos(pitch);
      double sinPitch = Math.sin(pitch);
      double yzY = vec.y * cosPitch - xzZ * sinPitch;
      double yzZ = vec.y * sinPitch + xzZ * cosPitch;
      return new Vec3(xzX, yzY, yzZ);
   }
}
