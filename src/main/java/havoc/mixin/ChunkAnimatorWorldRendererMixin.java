package havoc.mixin;

import havoc.module.impl.visuals.BetterMinecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class ChunkAnimatorWorldRendererMixin {
   @Inject(method = "allChanged", at = @At("HEAD"))
   private void havoc$clearChunkAnimatorOnReload(CallbackInfo ci) {
      BetterMinecraft.clearChunkAnimations();
   }
}
