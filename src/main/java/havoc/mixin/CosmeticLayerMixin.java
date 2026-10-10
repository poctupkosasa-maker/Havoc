package havoc.mixin;

import havoc.cosmetic.CosmeticGeoLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class CosmeticLayerMixin {
   @Inject(method = "<init>", at = @At("RETURN"))
   private void havoc$addCosmeticLayer(CallbackInfo ci) {
      CosmeticGeoLayer layer = new CosmeticGeoLayer((RenderLayerParent) (Object) this);
      ((LivingEntityRendererAddLayerAccessor) this).havoc$addLayer(layer);
   }
}
