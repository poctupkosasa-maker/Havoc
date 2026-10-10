package havoc.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import havoc.module.impl.visuals.NoRender;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ScreenEffectRenderer.class)
public class NoRenderInGameOverlayRendererMixin {
   @Redirect(
      method = "submit",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;submitFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"
      )
   )
   private void havoc$skipFireOverlay(PoseStack matrices, SubmitNodeCollector submitNodeCollector, TextureAtlasSprite sprite) {
      if (!NoRender.hideFire()) {
         InGameOverlayRendererAccessor.havoc$renderFireOverlay(matrices, submitNodeCollector, sprite);
      }
   }

   @Redirect(
      method = "submit",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;submitBlockSprite(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
      )
   )
   private void havoc$skipBlockOverlay(TextureAtlasSprite sprite, PoseStack matrices, SubmitNodeCollector submitNodeCollector, int color) {
      if (!NoRender.hideBlockOverlay()) {
         InGameOverlayRendererAccessor.havoc$renderInWallOverlay(sprite, matrices, submitNodeCollector, color);
      }
   }
}
