package havoc.event.impl;

import havoc.event.Event;
import havoc.utilites.IMinecraft;
import lombok.Generated;
import net.minecraft.util.Mth;

public final class EventInput extends Event implements IMinecraft {
   private float forward;
   private float strafe;
   private boolean jump;
   private boolean sneak;
   private boolean sprint;
   private double sneakSlowDownMultiplier = 0.3;

   public EventInput(float forward, float strafe, boolean jump, boolean sneak, boolean sprint) {
      this.forward = forward;
      this.strafe = strafe;
      this.jump = jump;
      this.sneak = sneak;
      this.sprint = sprint;
   }

   public void setYaw(float yaw, float targetYaw) {
      float currentForward = this.forward;
      float currentStrafe = this.strafe;
      double angle = Mth.wrapDegrees(Math.toDegrees(direction(targetYaw, currentForward, currentStrafe)));
      if (currentForward != 0.0F || currentStrafe != 0.0F) {
         float closestForward = 0.0F;
         float closestStrafe = 0.0F;
         float closestDifference = Float.MAX_VALUE;

         for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward++) {
            for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe++) {
               if (predictedStrafe != 0.0F || predictedForward != 0.0F) {
                  double predictedAngle = Mth.wrapDegrees(Math.toDegrees(direction(yaw, predictedForward, predictedStrafe)));
                  double difference = Math.abs(angle - predictedAngle);
                  if (difference < closestDifference) {
                     closestDifference = (float)difference;
                     closestForward = predictedForward;
                     closestStrafe = predictedStrafe;
                  }
               }
            }
         }

         this.forward = closestForward;
         this.strafe = closestStrafe;
      }
   }

   public void setYaw(float yaw) {
      if (mc.player != null) {
         this.setYaw(yaw, mc.player.getYRot());
      }
   }

   private static double direction(float rotationYaw, double moveForward, double moveStrafing) {
      if (moveForward < 0.0) {
         rotationYaw += 180.0F;
      }

      float forward = 1.0F;
      if (moveForward < 0.0) {
         forward = -0.5F;
      } else if (moveForward > 0.0) {
         forward = 0.5F;
      }

      if (moveStrafing > 0.0) {
         rotationYaw -= 90.0F * forward;
      }

      if (moveStrafing < 0.0) {
         rotationYaw += 90.0F * forward;
      }

      return Math.toRadians(rotationYaw);
   }

   @Generated
   public float getForward() {
      return this.forward;
   }

   @Generated
   public float getStrafe() {
      return this.strafe;
   }

   @Generated
   public boolean isJump() {
      return this.jump;
   }

   @Generated
   public boolean isSneak() {
      return this.sneak;
   }

   @Generated
   public boolean isSprint() {
      return this.sprint;
   }

   @Generated
   public double getSneakSlowDownMultiplier() {
      return this.sneakSlowDownMultiplier;
   }

   @Generated
   public void setForward(float forward) {
      this.forward = forward;
   }

   @Generated
   public void setStrafe(float strafe) {
      this.strafe = strafe;
   }

   @Generated
   public void setJump(boolean jump) {
      this.jump = jump;
   }

   @Generated
   public void setSneak(boolean sneak) {
      this.sneak = sneak;
   }

   @Generated
   public void setSprint(boolean sprint) {
      this.sprint = sprint;
   }

   @Generated
   public void setSneakSlowDownMultiplier(double sneakSlowDownMultiplier) {
      this.sneakSlowDownMultiplier = sneakSlowDownMultiplier;
   }
}
