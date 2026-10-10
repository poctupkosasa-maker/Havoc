package havoc.mixin;

import havoc.Havoc;
import havoc.module.impl.visuals.Chams;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
   @Inject(
      method = "getRenderType(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;",
      at = @At("HEAD"),
      cancellable = true
   )
   private void havoc$hideVanillaSkinForChams(
      LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline, CallbackInfoReturnable<RenderType> cir
   ) {
      if (state instanceof AvatarRenderState playerState) {
         Minecraft mc = Minecraft.getInstance();
         Havoc havoc = Havoc.getInstance();
         if (havoc != null && havoc.getModuleManager() != null && mc != null && mc.level != null) {
            Chams chams = havoc.getModuleManager().getModule(Chams.class);
            if (chams != null && chams.isEnable()) {
               Entity entity = mc.level.getEntity(playerState.id);
               if (entity != null && chams.shouldHideVanillaModel(entity)) {
                  cir.setReturnValue(null);
               }
            }
         }
      }
   }
}
