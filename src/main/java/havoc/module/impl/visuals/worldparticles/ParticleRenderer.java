package havoc.module.impl.visuals.worldparticles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import havoc.render.Render3D;
import havoc.utilites.color.ColorUtil;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;

public final class ParticleRenderer {
   public static final int FULL_BRIGHT = 15728880;

   private ParticleRenderer() {
   }

   public static void drawCube(PoseStack matrices, VertexConsumer consumer, float size, int color) {
      float half = size * 0.5F;
      Render3D.drawFilledBox(matrices, consumer, new AABB(-half, -half, -half, half, half, half), color);
   }

   public static void drawCubeOutline(PoseStack matrices, VertexConsumer consumer, float size, int color) {
      float half = size * 0.5F;
      Render3D.drawOutlinedBox(matrices, consumer, new AABB(-half, -half, -half, half, half, half), color);
   }

   public static void drawBillboard(Pose entry, VertexConsumer consumer, float size, int color) {
      drawBillboard(entry, consumer, size, color, 0.0F);
   }

   public static void drawBillboard(Pose entry, VertexConsumer consumer, float size, int color, float zOffset) {
      drawBillboard(entry, consumer, size, size, color, zOffset);
   }

   public static void drawBillboard(Pose entry, VertexConsumer consumer, float width, float height, int color, float zOffset) {
      float halfWidth = width * 0.5F;
      float halfHeight = height * 0.5F;
      float r = ColorUtil.red(color) / 255.0F;
      float g = ColorUtil.green(color) / 255.0F;
      float b = ColorUtil.blue(color) / 255.0F;
      float a = ColorUtil.alpha(color) / 255.0F;
      consumer.addVertex(entry, -halfWidth, -halfHeight, zOffset)
         .setColor(r, g, b, a)
         .setUv(0.0F, 1.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(entry, 0.0F, 1.0F, 0.0F);
      consumer.addVertex(entry, -halfWidth, halfHeight, zOffset)
         .setColor(r, g, b, a)
         .setUv(0.0F, 0.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(entry, 0.0F, 1.0F, 0.0F);
      consumer.addVertex(entry, halfWidth, halfHeight, zOffset)
         .setColor(r, g, b, a)
         .setUv(1.0F, 0.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(entry, 0.0F, 1.0F, 0.0F);
      consumer.addVertex(entry, halfWidth, -halfHeight, zOffset)
         .setColor(r, g, b, a)
         .setUv(1.0F, 1.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(15728880)
         .setNormal(entry, 0.0F, 1.0F, 0.0F);
   }
}
