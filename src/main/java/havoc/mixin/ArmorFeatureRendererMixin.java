package havoc.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import havoc.Havoc;
import havoc.module.impl.visuals.Chams;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public class ArmorFeatureRendererMixin {
   @Inject(
      method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void havoc$toggleArmorForChams(
      PoseStack matrices, SubmitNodeCollector queue, int light, HumanoidRenderState state, float limbAngle, float limbDistance, CallbackInfo ci
   ) {
      if (state instanceof AvatarRenderState) {
         Havoc havoc = Havoc.getInstance();
         if (havoc != null && havoc.getModuleManager() != null) {
            Chams chams = havoc.getModuleManager().getModule(Chams.class);
            if (chams != null && chams.isEnable() && !chams.isArmorVisible()) {
               ci.cancel();
            }
         }
      }
   }
}
