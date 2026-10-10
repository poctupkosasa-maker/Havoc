package havoc.mixin;

import havoc.module.impl.visuals.Ambience;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public class SkyRenderingMixin {
   @Inject(method = "renderDarkDisc", at = @At("HEAD"), cancellable = true)
   private void havoc$cancelSkyDarkForCustomFog(CallbackInfo ci) {
      Ambience ambience = Ambience.getInstance();
      if (ambience != null && (ambience.shouldOverrideFog() || ambience.hasCustomSky())) {
         ci.cancel();
      }
   }
}
