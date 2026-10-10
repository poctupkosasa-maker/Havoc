package havoc.utilites;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class MovementUtil {
   private static final Minecraft mc = Minecraft.getInstance();

   private MovementUtil() {
   }

   public static boolean isMoving() {
      if (mc.player == null) {
         return false;
      }

      Vec2 input = mc.player.input.getMoveVector();
      return input.y != 0.0F || input.x != 0.0F;
   }

   public static void setHorizontalSpeed(double speed) {
      if (mc.player != null) {
         Vec2 input = mc.player.input.getMoveVector();
         float forward = input.y;
         float sideways = input.x;
         float yaw = mc.player.getYRot();
         if (forward == 0.0F && sideways == 0.0F) {
            mc.player.setDeltaMovement(0.0, mc.player.getDeltaMovement().y, 0.0);
         } else {
            if (forward != 0.0F) {
               if (sideways > 0.0F) {
                  yaw += forward > 0.0F ? -45.0F : 45.0F;
               } else if (sideways < 0.0F) {
                  yaw += forward > 0.0F ? 45.0F : -45.0F;
               }

               sideways = 0.0F;
               forward = forward > 0.0F ? 1.0F : -1.0F;
            }

            double sin = Math.sin(Math.toRadians(yaw + 90.0F));
            double cos = Math.cos(Math.toRadians(yaw + 90.0F));
            Vec3 velocity = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(forward * speed * cos + sideways * speed * sin, velocity.y, forward * speed * sin - sideways * speed * cos);
         }
      }
   }
}
