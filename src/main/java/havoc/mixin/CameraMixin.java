package havoc.mixin;

import havoc.module.impl.visuals.BetterMinecraft;
import havoc.utilites.rotation.SilentAimManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
   @Shadow
   @Nullable
   private Entity entity;

   @Inject(method = "alignWithEntity", at = @At("HEAD"))
   private void onUpdateHead(float tickProgress, CallbackInfo ci) {
      if (this.entity != null) {
         SilentAimManager.applyRenderInterpolation();
         SilentAimManager.syncFreeLook(this.entity.getYRot(), this.entity.getXRot());
      }
   }

   @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
   private void havoc$animateZoomFov(CallbackInfoReturnable<Float> cir) {
      cir.setReturnValue(BetterMinecraft.adjustFov((Float)cir.getReturnValue()));
   }

   @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
   private float redirectCameraYaw(Entity entity, float tickProgress) {
      return entity == Minecraft.getInstance().player && SilentAimManager.isActive() ? SilentAimManager.getFreeYaw() : entity.getViewYRot(tickProgress);
   }

   @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
   private float redirectCameraPitch(Entity entity, float tickProgress) {
      return entity == Minecraft.getInstance().player && SilentAimManager.isActive() ? SilentAimManager.getFreePitch() : entity.getViewXRot(tickProgress);
   }
}
