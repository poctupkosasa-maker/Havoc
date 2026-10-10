package havoc.utilites.combat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class CombatPredicates {
   private CombatPredicates() {
   }

   public static double strictDistanceTo(LocalPlayer player, LivingEntity target) {
      Vec3 eye = player.getEyePosition();
      AABB box = target.getBoundingBox();
      Vec3 closest = new Vec3(Math.clamp(eye.x, box.minX, box.maxX), Math.clamp(eye.y, box.minY, box.maxY), Math.clamp(eye.z, box.minZ, box.maxZ));
      return closest.subtract(eye).length();
   }

   public static boolean isPlayerInCriticalState(LocalPlayer player) {
      boolean crit = player.fallDistance > 0.0 && (player.fallDistance < 0.08F || isLikelyAirborneNextTick(player));
      return !player.onGround() && crit;
   }

   public static boolean isAimingAtTarget(LocalPlayer player, LivingEntity target, float range, boolean ignoreWalls) {
      Vec3 start = player.getEyePosition();
      Vec3 view = player.getViewVector(1.0F);
      Vec3 end = start.add(view.x * range, view.y * range, view.z * range);
      Vec3 effectiveEnd = end;
      if (!ignoreWalls) {
         BlockHitResult hit = player.level().clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, player));
         if (hit.getType() != Type.MISS) {
            effectiveEnd = hit.getLocation();
         }
      }

      AABB scanBox = player.getBoundingBox().inflate(view.x * range, view.y * range, view.z * range).inflate(1.0, 1.0, 1.0);
      EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, effectiveEnd, scanBox, entity -> entity == target, (double)range * range);
      return hit != null && hit.getEntity() == target;
   }

   private static boolean isLikelyAirborneNextTick(LocalPlayer player) {
      AABB nextBox = player.getBoundingBox().move(0.0, player.getDeltaMovement().y, 0.0);
      return player.level().noCollision(player, nextBox);
   }
}
