package havoc.mixin;

import havoc.module.impl.visuals.NoRender;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DarknessFogEnvironment.class)
public class NoRenderDarknessEffectFogModifierMixin {
   @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
   private void havoc$skipDarknessFog(FogData fogData, Camera camera, ClientLevel world, float viewDistance, DeltaTracker tickCounter, CallbackInfo ci) {
      if (NoRender.hideDarkness()) {
         ci.cancel();
      }
   }

   @Inject(method = "getModifiedDarkness", at = @At("HEAD"), cancellable = true)
   private void havoc$skipDarknessModifier(LivingEntity entity, float factor, float tickProgress, CallbackInfoReturnable<Float> cir) {
      if (NoRender.hideDarkness()) {
         cir.setReturnValue(0.0F);
      }
   }
}
