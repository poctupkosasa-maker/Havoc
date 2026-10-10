package havoc.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ScreenEffectRenderer.class)
public interface InGameOverlayRendererAccessor {
   @Invoker("submitFire")
   static void havoc$renderFireOverlay(PoseStack matrices, SubmitNodeCollector submitNodeCollector, TextureAtlasSprite sprite) {
      throw new AssertionError();
   }

   @Invoker("submitBlockSprite")
   static void havoc$renderInWallOverlay(TextureAtlasSprite sprite, PoseStack matrices, SubmitNodeCollector submitNodeCollector, int color) {
      throw new AssertionError();
   }
}
