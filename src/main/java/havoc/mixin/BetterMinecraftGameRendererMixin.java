package havoc.mixin;

import havoc.utilites.rotation.SilentAimManager;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GameRenderer.class)
public class BetterMinecraftGameRendererMixin {
   @ModifyArg(
      method = "renderLevel",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V"
      ),
      index = 2
   )
   private Matrix4fc useRealViewForHand(Matrix4fc positionMatrix) {
      if (!SilentAimManager.isActive()) {
         return positionMatrix;
      }

      float yaw = SilentAimManager.getFreeYaw();
      float pitch = SilentAimManager.getFreePitch();
      Quaternionf rotation = new Quaternionf()
         .rotationYXZ((float) Math.PI - yaw * (float) (Math.PI / 180.0), -pitch * (float) (Math.PI / 180.0), 0.0F)
         .conjugate();
      return new Matrix4f().rotation(rotation);
   }
}
