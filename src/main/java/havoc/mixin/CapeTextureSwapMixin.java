package havoc.mixin;

import havoc.Havoc;
import havoc.cosmetic.CosmeticRegistry;
import havoc.module.impl.visuals.Cosmetics;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CapeLayer.class)
public abstract class CapeTextureSwapMixin {
   @Redirect(
      method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/PlayerSkin;cape()Lnet/minecraft/core/ClientAsset$Texture;"),
      require = 0
   )
   private ClientAsset.Texture havoc$swapCapeTexture(PlayerSkin skin) {
      if (Havoc.getInstance() != null) {
         Cosmetics cosmetics = Havoc.getInstance().getModuleManager().getModule(Cosmetics.class);
         if (cosmetics != null && cosmetics.isEnable()) {
            Identifier texture = cosmetics.selectedCapeTexture();
            if (texture != null) {
               return new ClientAsset.ResourceTexture(texture);
            }
         }
      }

      return skin.cape();
   }
}
