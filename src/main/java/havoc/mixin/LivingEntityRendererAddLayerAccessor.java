package havoc.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAddLayerAccessor {
   @Invoker("addLayer")
   @SuppressWarnings("unchecked")
   boolean havoc$addLayer(RenderLayer<?, ?> layer);
}
