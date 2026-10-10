package havoc.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import havoc.module.impl.visuals.NoRender;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class NoRenderTotemAnimationMixin {
   @Shadow
   private ItemStack itemActivationItem;

   @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), cancellable = true)
   private void havoc$skipTotemAnimation(PoseStack matrices, float tickProgress, SubmitNodeCollector queue, CallbackInfo ci) {
      if (NoRender.hideTotemAnimation()) {
         if (this.itemActivationItem != null && this.itemActivationItem.is(Items.TOTEM_OF_UNDYING)) {
            ci.cancel();
         }
      }
   }
}
