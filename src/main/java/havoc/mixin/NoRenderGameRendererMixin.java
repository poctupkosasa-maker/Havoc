package havoc.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import havoc.module.impl.visuals.NoRender;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class NoRenderGameRendererMixin {
   @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
   private void havoc$skipDamageTilt(CameraRenderState cameraState, PoseStack matrices, CallbackInfo ci) {
      if (NoRender.hideDamage()) {
         ci.cancel();
      }
   }
}
