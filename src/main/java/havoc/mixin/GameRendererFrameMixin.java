package havoc.mixin;

import havoc.render.RenderUtil3D;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererFrameMixin {
   @Shadow
   @Final
   private Camera mainCamera;
   @Shadow
   @Final
   private GameRenderState gameRenderState;

   @Inject(method = "extractCamera", at = @At("TAIL"))
   private void havoc$captureFrame(DeltaTracker deltaTracker, float tickDelta, float cameraTickDelta, CallbackInfo ci) {
      CameraRenderState cameraState = this.gameRenderState.levelRenderState.cameraRenderState;
      RenderUtil3D.captureFrame(this.mainCamera, cameraState.viewRotationMatrix, cameraState.projectionMatrix, tickDelta);
   }

   @Inject(method = "resetData", at = @At("HEAD"))
   private void havoc$clearFrame(CallbackInfo ci) {
      RenderUtil3D.clearFrame();
   }
}
