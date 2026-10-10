package havoc.utilites.rotation;

import lombok.Generated;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public class SilentAimManager {
   private static final float MOUSE_TURN_SCALE = 0.15F;
   private static float freeYaw = 0.0F;
   private static float freePitch = 0.0F;
   private static boolean active = false;
   private static boolean hasLastAngle = false;
   private static float lastYaw = 0.0F;
   private static float lastPitch = 0.0F;
   private static float lastHeadYaw = 0.0F;
   private static float lastBodyYaw = 0.0F;

   private SilentAimManager() {
   }

   public static void apply(float yawDelta, float pitchDelta) {
      applyInternal(yawDelta, pitchDelta);
   }

   public static void applyRaw(float yawDelta, float pitchDelta) {
      applyInternal(yawDelta, pitchDelta);
   }

   private static void applyInternal(float yawDelta, float pitchDelta) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         if (!active) {
            freeYaw = player.getYRot();
            freePitch = player.getXRot();
            active = true;
         }

         rememberRenderAngles(player);
         float nextYaw = player.yHeadRot + yawDelta;
         float nextPitch = Mth.clamp(player.getXRot() + pitchDelta, -90.0F, 90.0F);
         player.setYRot(nextYaw);
         player.setXRot(nextPitch);
         player.yHeadRot = nextYaw;
      }
   }

   public static void syncFreeLook(float yaw, float pitch) {
      if (!active) {
         freeYaw = yaw;
         freePitch = pitch;
      }
   }

   public static boolean onMouseTurn(double yawInput, double pitchInput) {
      if (!active) {
         return false;
      }

      freeYaw += (float)yawInput * 0.15F;
      freePitch = Mth.clamp(freePitch + (float)pitchInput * 0.15F, -90.0F, 90.0F);
      return true;
   }

   public static void applyRenderInterpolation() {
      if (active && hasLastAngle) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player == null) {
            hasLastAngle = false;
         } else {
            player.yRotO = lastYaw;
            player.xRotO = lastPitch;
            player.yHeadRotO = lastHeadYaw;
            player.yBodyRotO = lastBodyYaw;
         }
      }
   }

   public static void clear() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         if (active) {
            player.setYRot(freeYaw);
            player.setXRot(freePitch);
            player.yHeadRot = freeYaw;
         }

         freeYaw = player.getYRot();
         freePitch = player.getXRot();
      }

      active = false;
      hasLastAngle = false;
   }

   private static void rememberRenderAngles(LocalPlayer player) {
      lastYaw = player.getYRot();
      lastPitch = player.getXRot();
      lastHeadYaw = player.yHeadRot;
      lastBodyYaw = player.yBodyRot;
      hasLastAngle = true;
   }

   @Generated
   public static float getFreeYaw() {
      return freeYaw;
   }

   @Generated
   public static float getFreePitch() {
      return freePitch;
   }

   @Generated
   public static boolean isActive() {
      return active;
   }
}
