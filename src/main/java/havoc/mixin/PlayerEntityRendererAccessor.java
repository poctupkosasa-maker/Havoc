package havoc.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AvatarRenderer.class)
public interface PlayerEntityRendererAccessor {
   @Invoker("setupRotations")
   void havoc$setupTransforms(AvatarRenderState var1, PoseStack var2, float var3, float var4);

   @Invoker("scale")
   void havoc$scale(AvatarRenderState var1, PoseStack var2);
}
