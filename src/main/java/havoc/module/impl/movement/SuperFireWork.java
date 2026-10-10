package havoc.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventScreenRender;
import havoc.event.impl.FireworkEvent;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.utilites.color.ClientColors;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(name = "SuperFireWork", desc = "Luma firework elytra boost", category = Category.MOVEMENT)
public final class SuperFireWork extends Module {
   private final SelectSetting mode = this.registerSetting(
      new SelectSetting("Режим", "BravoHvH", Arrays.asList("ReallyWorld", "BravoHvH", "MetaHvH", "MetaHvH (Super Fast)", "MetaHvH New", "Custom"))
   );
   private final SliderSetting speedXZ = this.slider("Скорость XZ", 1.675F, 1.0F, 3.5F);
   private final SliderSetting speedY = this.slider("Скорость Y", 1.66F, 1.0F, 3.5F);
   private final BooleanSetting diagonalBoost = this.registerSetting(new BooleanSetting("Буст по диагоналям", true).visibleWhen(this::custom));
   private final SliderSetting diagonalSpeed = this.registerSetting(
      new SliderSetting("Диагональная скорость", 1.963F, 1.0F, 4.0F, 0.001F).visibleWhen(() -> this.custom() && this.diagonalBoost.isValue())
   );
   private final BooleanSetting nearBoost = this.registerSetting(new BooleanSetting("Буст от игроков", false).visibleWhen(this::custom));
   private final SliderSetting nearDistance = this.registerSetting(
      new SliderSetting("Дистанция игроков", 5.0F, 2.0F, 15.0F, 0.1F).visibleWhen(() -> this.custom() && this.nearBoost.isValue())
   );
   private final SliderSetting nearSpeed = this.registerSetting(
      new SliderSetting("Буст скорость XZ", 1.82F, 1.0F, 3.5F, 0.01F).visibleWhen(() -> this.custom() && this.nearBoost.isValue())
   );
   private final SliderSetting nearSpeedY = this.registerSetting(
      new SliderSetting("Буст скорость Y", 1.67F, 1.0F, 3.5F, 0.01F).visibleWhen(() -> this.custom() && this.nearBoost.isValue())
   );
   private final BooleanSetting debug = this.registerSetting(new BooleanSetting("Debug", false));

   @Subscribe
   public void onFirework(FireworkEvent event) {
      if (mc.player != null && mc.player.isFallFlying()) {
         float yaw = mc.player.getYRot() % 360.0F;
         if (yaw < 0.0F) {
            yaw += 360.0F;
         }

         float pitch = mc.player.getXRot();
         switch (this.mode.getValue()) {
            case "ReallyWorld":
               this.reallyWorld(event, yaw);
               break;
            case "MetaHvH":
               this.apply(event, this.diagonalSpeed(foldYaw(yaw), pitch), 1.66);
               break;
            case "MetaHvH (Super Fast)":
               this.apply(event, this.pitchSpeedXZ(pitch), this.pitchSpeedVertical(pitch));
               break;
            case "MetaHvH New":
               this.apply(event, this.yawSpeed(foldYaw(yaw)), 1.59);
               break;
            case "Custom":
               this.custom(event, yaw);
               break;
            default:
               this.bravo(event, yaw);
         }
      }
   }

   @Subscribe
   public void onScreenRender(EventScreenRender event) {
      if (this.debug.isValue() && event.getStage() == EventScreenRender.Stage.HUD && mc.player != null) {
         double bps = Math.sqrt(
               mc.player.getDeltaMovement().x * mc.player.getDeltaMovement().x + mc.player.getDeltaMovement().z * mc.player.getDeltaMovement().z
            )
            * 20.0;
         String text = String.format(Locale.ROOT, "Pitch: %.1f | Yaw: %.1f | BPS: %.2f", mc.player.getXRot(), mc.player.getYRot(), bps);
         float size = 6.5F;
         float x = (mc.getWindow().getGuiScaledWidth() - RenderUtil.getTextWidth(text, GFonts.MEDIUM, size)) * 0.5F;
         float y = mc.getWindow().getGuiScaledHeight() * 0.5F + 12.0F;
         RenderUtil.drawText(event.getContext(), text, x, y, ClientColors.TEXT_PRIMARY, GFonts.MEDIUM, size);
      }
   }

   private void reallyWorld(FireworkEvent event, float yaw) {
      double distance = diagonalDistance(yaw);
      double xz = distance <= 4.0
         ? 2.2
         : (
            distance <= 8.0
               ? 2.06
               : (
                  distance <= 12.0
                     ? 1.98
                     : (
                        distance <= 16.0
                           ? 1.87
                           : (
                              distance <= 20.0
                                 ? 1.8
                                 : (distance <= 24.0 ? 1.74 : (distance <= 28.0 ? 1.7 : (distance <= 32.0 ? 1.65 : (distance <= 36.0 ? 1.63 : 1.61))))
                           )
                     )
               )
         );
      this.apply(event, xz, distance > 36.0 ? 1.61 : 1.5);
   }

   private void bravo(FireworkEvent event, float yaw) {
      this.apply(event, diagonalDistance(yaw) <= 16.0 ? 1.963 : 1.675, 1.66);
   }

   private void custom(FireworkEvent event, float yaw) {
      boolean diagonal = this.diagonalBoost.isValue() && diagonalDistance(yaw) <= 16.0;
      boolean near = this.nearBoost.isValue() && this.nearbyPlayer();
      this.apply(
         event,
         (diagonal ? this.diagonalSpeed.getValue() : (near ? this.nearSpeed.getValue() : this.speedXZ.getValue())).floatValue(),
         (near ? this.nearSpeedY.getValue() : this.speedY.getValue()).floatValue()
      );
   }

   private void apply(FireworkEvent event, double xz, double y) {
      Vec3 rotation = mc.player.getLookAngle();
      Vec3 velocity = event.getVector();
      event.setVector(
         velocity.add(
            rotation.x * 0.1 + (rotation.x * xz - velocity.x) * 0.5,
            rotation.y * 0.1 + (rotation.y * y - velocity.y) * 0.5,
            rotation.z * 0.1 + (rotation.z * xz - velocity.z) * 0.5
         )
      );
   }

   private float pitchSpeedXZ(float pitch) {
      float abs = Math.abs(pitch);
      if (abs >= 40.0F && abs <= 50.0F) {
         return 4.51F;
      } else if (abs >= 35.0F && abs <= 55.0F) {
         return 4.52F;
      } else if (abs >= 30.0F && abs <= 60.0F) {
         return 4.51F;
      } else if (abs >= 25.0F && abs <= 65.0F) {
         return 4.05F;
      } else if (abs >= 20.0F && abs <= 70.0F) {
         return 3.47F;
      } else if (abs >= 15.0F && abs <= 75.0F) {
         return 3.03F;
      } else {
         return abs >= 10.0F && abs <= 80.0F ? 2.52F : 2.3F;
      }
   }

   private float pitchSpeedVertical(float pitch) {
      if (pitch >= 40.0F && pitch <= 50.0F) {
         return 4.63F;
      } else if (pitch >= 35.0F && pitch <= 55.0F) {
         return 4.8F;
      } else if (pitch >= 30.0F && pitch <= 60.0F) {
         return 4.4F;
      } else if (pitch >= 25.0F && pitch <= 65.0F) {
         return 3.8F;
      } else if (pitch >= 20.0F && pitch <= 70.0F) {
         return 1.98F;
      } else if (pitch >= 15.0F && pitch <= 75.0F) {
         return 2.13F;
      } else if (pitch >= 10.0F && pitch <= 80.0F) {
         return 1.98F;
      } else if (pitch >= 0.0F && pitch < 10.0F) {
         return 2.13F;
      } else if (pitch <= -40.0F && pitch >= -50.0F) {
         return 5.1F;
      } else if (pitch <= -35.0F && pitch >= -55.0F) {
         return 4.8F;
      } else if (pitch <= -30.0F && pitch >= -60.0F) {
         return 4.4F;
      } else if (pitch <= -25.0F && pitch >= -65.0F) {
         return 3.8F;
      } else if (pitch <= -20.0F && pitch >= -70.0F) {
         return 3.5F;
      } else if (pitch <= -15.0F && pitch >= -75.0F) {
         return 3.4F;
      } else {
         return pitch <= -10.0F && pitch >= -80.0F ? 3.3F : 3.0F;
      }
   }

   private float diagonalSpeed(float yaw, float pitch) {
      float ay = Math.abs(yaw);
      float ap = Math.abs(pitch);
      float result;
      if (ay >= 38.0F && ay <= 52.0F) {
         result = 2.0F;
      } else if (ay >= 34.0F && ay <= 54.0F) {
         result = 1.98F;
      } else if (ay >= 32.0F && ay <= 58.0F) {
         result = 1.96F;
      } else if (ay >= 28.0F && ay <= 62.0F) {
         result = 1.95F;
      } else if (between(ap, 42.0F, 48.0F) || between(ay, 42.0F, 48.0F)) {
         result = 1.9645F;
      } else if (between(ap, 40.0F, 50.0F) || between(ay, 40.0F, 50.0F)) {
         result = 1.963F;
      } else if (between(ap, 35.0F, 55.0F) || between(ay, 35.0F, 55.0F)) {
         result = 1.963F;
      } else if (between(ap, 29.0F, 61.0F) || between(ay, 29.0F, 61.0F)) {
         result = 1.94F;
      } else if (between(ap, 28.0F, 62.0F) || between(ay, 28.0F, 62.0F)) {
         result = 1.865F;
      } else if (between(ap, 26.0F, 64.0F) || between(ay, 26.0F, 64.0F)) {
         result = 1.8F;
      } else if (between(ap, 25.0F, 65.0F) || between(ay, 25.0F, 65.0F)) {
         result = 1.77F;
      } else if (between(ap, 24.0F, 66.0F) || between(ay, 24.0F, 66.0F)) {
         result = 1.74F;
      } else if (between(ap, 22.0F, 68.0F) || between(ay, 22.0F, 68.0F)) {
         result = 1.72F;
      } else if (between(ap, 18.0F, 72.0F) || between(ay, 18.0F, 72.0F)) {
         result = 1.7F;
      } else if (between(ap, 15.0F, 75.0F) || between(ay, 15.0F, 75.0F)) {
         result = 1.68F;
      } else if (between(ap, 13.0F, 77.0F) || between(ay, 13.0F, 77.0F)) {
         result = 1.66F;
      } else if (between(ap, 12.0F, 78.0F) || between(ay, 12.0F, 78.0F)) {
         result = 1.65F;
      } else if (between(ap, 8.0F, 82.0F) || between(ay, 8.0F, 82.0F)) {
         result = 1.62F;
      } else if (between(ap, 5.0F, 85.0F) || between(ay, 5.0F, 85.0F)) {
         result = 1.625F;
      } else if (!between(ap, 1.0F, 89.0F) && !between(ay, 1.0F, 89.0F)) {
         result = 1.5F;
      } else {
         result = 1.6205F;
      }

      if (yaw > 15.0F) {
         result -= 0.0675F;
      }

      return Math.max(1.62F, result);
   }

   private float yawSpeed(float yaw) {
      float abs = Math.abs(yaw);
      if (between(abs, 37.0F, 38.0F)) {
         return 2.02F;
      } else if (between(abs, 25.0F, 30.0F)) {
         return 2.0F;
      } else if (between(abs, 35.0F, 45.0F)) {
         return 1.99F;
      } else if (between(abs, 40.0F, 50.0F)) {
         return 1.97F;
      } else if (between(abs, 50.0F, 60.0F)) {
         return 1.96F;
      } else if (between(abs, 51.0F, 61.0F)) {
         return 1.87F;
      } else {
         return between(abs, 52.0F, 65.0F) ? 1.69F : 1.59F;
      }
   }

   private static float foldYaw(float yaw) {
      float folded = yaw % 90.0F;
      if (folded > 45.0F) {
         folded -= 90.0F;
      }

      return folded;
   }

   private static boolean between(float value, float min, float max) {
      return value >= min && value <= max;
   }

   private SliderSetting slider(String name, float value, float min, float max) {
      return this.registerSetting(new SliderSetting(name, value, min, max, 0.001F).visibleWhen(this::custom));
   }

   private boolean custom() {
      return this.mode.getValue().equals("Custom");
   }

   private boolean nearbyPlayer() {
      for (Player player : mc.level.players()) {
         if (player != mc.player && player.distanceTo(mc.player) <= this.nearDistance.getValue()) {
            return true;
         }
      }

      return false;
   }

   private static double diagonalDistance(float yaw) {
      double result = 180.0;

      for (float diagonal : new float[]{45.0F, 135.0F, 225.0F, 315.0F}) {
         result = Math.min(result, Math.min(Math.abs(yaw - diagonal), 360.0 - Math.abs(yaw - diagonal)));
      }

      return result;
   }
}
