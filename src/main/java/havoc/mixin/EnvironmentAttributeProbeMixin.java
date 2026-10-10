package havoc.mixin;

import havoc.module.impl.visuals.Ambience;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnvironmentAttributeProbe.class)
public class EnvironmentAttributeProbeMixin {
   @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
   private <Value> void havoc$overrideAmbienceTime(EnvironmentAttribute<Value> attribute, float tickDelta, CallbackInfoReturnable<Value> cir) {
      Ambience ambience = Ambience.getInstance();
      if (ambience != null && ambience.shouldOverrideTime()) {
         float phase = havoc$celestialPhase(ambience.getForcedTimeOfDay());
         float angleDegrees = phase * 360.0F;
         float daylight = Math.clamp((float)(Math.cos(phase * Math.PI * 2.0) * 1.5 + 0.15), 0.0F, 1.0F);
         Object value = null;
         if (attribute == EnvironmentAttributes.SUN_ANGLE || attribute == EnvironmentAttributes.STAR_ANGLE) {
            value = angleDegrees;
         } else if (attribute == EnvironmentAttributes.MOON_ANGLE) {
            value = (angleDegrees + 180.0F) % 360.0F;
         } else if (attribute == EnvironmentAttributes.STAR_BRIGHTNESS) {
            float stars = 1.0F - (float)(Math.cos(phase * Math.PI * 2.0) * 2.0 + 0.25);
            stars = Math.clamp(stars, 0.0F, 1.0F);
            value = stars * stars * 0.5F;
         } else if (attribute == EnvironmentAttributes.SKY_LIGHT_FACTOR) {
            value = daylight;
         }

         if (value != null) {
            Value castValue = (Value)value;
            cir.setReturnValue(castValue);
         }
      }
   }

   private static float havoc$celestialPhase(long timeOfDay) {
      double phase = Math.floorMod(timeOfDay, 24000L) / 24000.0 - 0.25;
      if (phase < 0.0) {
         phase++;
      }

      if (phase > 1.0) {
         phase--;
      }

      double eased = 0.5 - Math.cos(phase * Math.PI) * 0.5;
      return (float)(phase + (eased - phase) / 3.0);
   }
}
