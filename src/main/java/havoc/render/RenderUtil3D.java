package havoc.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class RenderUtil3D {
   private static Matrix4f frameViewMatrix;
   private static Matrix4f frameProjectionMatrix;
   private static Vec3 frameCameraPos;
   private static float frameTickDelta;

   private RenderUtil3D() {
   }

   public static void captureFrame(Camera camera, Matrix4f viewMatrix, Matrix4f projectionMatrix, float tickDelta) {
      if (camera != null && viewMatrix != null && projectionMatrix != null) {
         Vec3 pos = camera.position();
         if (pos == null) {
            clearFrame();
         } else {
            frameViewMatrix = new Matrix4f(viewMatrix);
            frameProjectionMatrix = new Matrix4f(projectionMatrix);
            frameCameraPos = pos;
            frameTickDelta = tickDelta;
         }
      } else {
         clearFrame();
      }
   }

   public static void clearFrame() {
      frameViewMatrix = null;
      frameProjectionMatrix = null;
      frameCameraPos = null;
      frameTickDelta = 0.0F;
   }

   public static boolean hasFrame() {
      return frameViewMatrix != null && frameProjectionMatrix != null && frameCameraPos != null;
   }

   public static float getFrameTickDelta() {
      return frameTickDelta;
   }

   public static Matrix4f getInverseViewProjectionMatrix() {
      return !hasFrame() ? null : new Matrix4f(frameProjectionMatrix).mul(frameViewMatrix).invert();
   }

   public static Matrix4f getViewMatrix() {
      return frameViewMatrix == null ? null : new Matrix4f(frameViewMatrix);
   }

   public static Matrix4f getProjectionMatrix() {
      return frameProjectionMatrix == null ? null : new Matrix4f(frameProjectionMatrix);
   }

   public static Vec3 getInterpolatedPos(Entity entity, float tickDelta) {
      if (entity == null) {
         return Vec3.ZERO;
      } else {
         double dx = entity.getX() - entity.xOld;
         double dy = entity.getY() - entity.yOld;
         double dz = entity.getZ() - entity.zOld;
         if (Double.isFinite(entity.xOld) && Double.isFinite(entity.yOld) && Double.isFinite(entity.zOld)) {
            return dx * dx + dy * dy + dz * dz > 64.0
               ? new Vec3(entity.getX(), entity.getY(), entity.getZ())
               : new Vec3(
                  Mth.lerp(tickDelta, entity.xOld, entity.getX()),
                  Mth.lerp(tickDelta, entity.yOld, entity.getY()),
                  Mth.lerp(tickDelta, entity.zOld, entity.getZ())
               );
         } else {
            return new Vec3(entity.getX(), entity.getY(), entity.getZ());
         }
      }
   }

   public static Vec2 worldToScreen(double worldX, double worldY, double worldZ, int screenWidth, int screenHeight) {
      if (!hasFrame()) {
         return null;
      }

      Vector4f clipSpacePos = new Vector4f((float)(worldX - frameCameraPos.x), (float)(worldY - frameCameraPos.y), (float)(worldZ - frameCameraPos.z), 1.0F);
      clipSpacePos.mul(frameViewMatrix).mul(frameProjectionMatrix);
      if (clipSpacePos.w <= 0.0F) {
         return null;
      }

      Vector4f ndc = clipSpacePos.div(clipSpacePos.w);
      float screenX = (ndc.x + 1.0F) * 0.5F * screenWidth;
      float screenY = (1.0F - ndc.y) * 0.5F * screenHeight;
      return new Vec2(screenX, screenY);
   }

   public static List<Vec2> projectWorldPoints(List<Vec3> worldPoints, int screenWidth, int screenHeight) {
      return projectWorldPoints(worldPoints, screenWidth, screenHeight, 1);
   }

   public static List<Vec2> projectWorldPoints(List<Vec3> worldPoints, int screenWidth, int screenHeight, int step) {
      List<Vec2> screenPoints = new ArrayList<>();
      if (worldPoints != null && !worldPoints.isEmpty()) {
         int safeStep = Math.max(1, step);

         for (int i = 0; i < worldPoints.size(); i += safeStep) {
            Vec3 point = worldPoints.get(i);
            if (point != null) {
               Vec2 screenPoint = worldToScreen(point.x, point.y, point.z, screenWidth, screenHeight);
               if (screenPoint != null) {
                  screenPoints.add(screenPoint);
               }
            }
         }

         int lastIndex = worldPoints.size() - 1;
         if (lastIndex >= 0 && lastIndex % safeStep != 0) {
            Vec3 point = worldPoints.get(lastIndex);
            Vec2 screenPoint = worldToScreen(point.x, point.y, point.z, screenWidth, screenHeight);
            if (screenPoint != null && (screenPoints.isEmpty() || !screenPoints.get(screenPoints.size() - 1).equals(screenPoint))) {
               screenPoints.add(screenPoint);
            }
         }

         return screenPoints;
      } else {
         return screenPoints;
      }
   }

   public static RenderUtil3D.TrajectoryResult simulateEnderPearl(Level world, ThrownEnderpearl pearl, int maxTicks) {
      return simulateEnderPearl(world, pearl, maxTicks, null);
   }

   public static RenderUtil3D.TrajectoryResult simulateEnderPearl(Level world, ThrownEnderpearl pearl, int maxTicks, Float tickDelta) {
      if (world != null && pearl != null && maxTicks > 0) {
         List<Vec3> points = new ArrayList<>();
         Vec3 position;
         if (tickDelta != null) {
            position = getInterpolatedPos(pearl, Mth.clamp(tickDelta, 0.0F, 1.0F));
         } else {
            position = new Vec3(pearl.getX(), pearl.getY(), pearl.getZ());
         }

         Vec3 velocity = pearl.getDeltaMovement();
         points.add(position);

         for (int tick = 1; tick <= maxTicks; tick++) {
            Vec3 nextPosition = position.add(velocity);
            BlockPos currentPos = BlockPos.containing(position);
            BlockPos nextPos = BlockPos.containing(nextPosition);
            if (!world.hasChunkAt(currentPos) || !world.hasChunkAt(nextPos)) {
               break;
            }

            BlockHitResult blockHit = world.clip(new ClipContext(position, nextPosition, Block.COLLIDER, Fluid.NONE, pearl));
            double maxDistanceSq = position.distanceToSqr(nextPosition);
            if (blockHit.getType() != Type.MISS) {
               maxDistanceSq = position.distanceToSqr(blockHit.getLocation());
            }

            AABB currentBox = AABB.ofSize(position, pearl.getBbWidth(), pearl.getBbHeight(), pearl.getBbWidth());
            AABB searchBox = currentBox.expandTowards(velocity).inflate(1.0);
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
               pearl,
               position,
               nextPosition,
               searchBox,
               entity -> entity != null
                  && entity.isAlive()
                  && !entity.isSpectator()
                  && entity.getType() != EntityTypes.ENDER_PEARL
                  && entity != pearl.getOwner(),
               maxDistanceSq
            );
            if (entityHit != null) {
               Vec3 hitPos = entityHit.getLocation();
               points.add(hitPos);
               return new RenderUtil3D.TrajectoryResult(points, hitPos, tick, true);
            }

            if (blockHit.getType() != Type.MISS) {
               Vec3 hitPos = blockHit.getLocation();
               points.add(hitPos);
               return new RenderUtil3D.TrajectoryResult(points, hitPos, tick, true);
            }

            position = nextPosition;
            points.add(position);
            boolean water = world.getBlockState(currentPos).liquid();
            double drag = water ? 0.8 : 0.99;
            velocity = velocity.scale(drag).add(0.0, -0.03, 0.0);
         }

         return new RenderUtil3D.TrajectoryResult(points, position, maxTicks, false);
      } else {
         return RenderUtil3D.TrajectoryResult.empty();
      }
   }

   public record TrajectoryResult(List<Vec3> points, Vec3 landingPos, int ticksRemaining, boolean hit) {
      public static RenderUtil3D.TrajectoryResult empty() {
         return new RenderUtil3D.TrajectoryResult(List.of(), Vec3.ZERO, 0, false);
      }
   }
}
