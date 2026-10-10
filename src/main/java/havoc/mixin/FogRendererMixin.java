package havoc.mixin;

import havoc.module.impl.visuals.Ambience;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
   @Inject(method = "setupFog", at = @At("RETURN"))
   private void havoc$applyCustomFog(
      Camera camera, int viewDistanceChunks, DeltaTracker tickCounter, float skyDarkness, ClientLevel level, CallbackInfoReturnable<FogData> cir
   ) {
      Ambience ambience = Ambience.getInstance();
      FogData fog = (FogData)cir.getReturnValue();
      boolean hasAmbience = ambience != null && (ambience.shouldOverrideFog() || ambience.shouldOverrideFogColor());
      if (fog != null && hasAmbience && havoc$canOverrideFog(camera)) {
         if (ambience.shouldOverrideFog()) {
            float viewDistance = Math.max(1.0F, viewDistanceChunks * 16.0F);
            float start = ambience.shouldClearFog() ? viewDistance : Math.clamp(ambience.getFogStartDistance(), 0.0F, viewDistance);
            float end = ambience.shouldClearFog() ? viewDistance + 0.5F : Math.clamp(ambience.getFogEndDistance(), start + 0.5F, viewDistance + 0.5F);
            fog.environmentalStart = start;
            fog.renderDistanceStart = start;
            fog.environmentalEnd = end;
            fog.renderDistanceEnd = end;
            fog.skyEnd = end;
            fog.cloudEnd = end;
         }

         if (ambience.shouldOverrideFogColor()) {
            float alpha = fog.color == null ? 1.0F : fog.color.w;
            fog.color = new Vector4f(ambience.getFogRed(), ambience.getFogGreen(), ambience.getFogBlue(), alpha);
         }
      }
   }

   private static boolean havoc$canOverrideFog(Camera camera) {
      FogType fogType = camera.getFluidInCamera();
      return fogType != FogType.WATER && fogType != FogType.LAVA && fogType != FogType.POWDER_SNOW
         ? !(camera.entity() instanceof LivingEntity living && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS)))
         : false;
   }
}
