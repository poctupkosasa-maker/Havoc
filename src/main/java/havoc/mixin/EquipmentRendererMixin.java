package havoc.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import havoc.Havoc;
import havoc.module.impl.visuals.Chams;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EquipmentLayerRenderer.class)
public class EquipmentRendererMixin {
   private static final int ARMOR_BLACK = 0;

   @Redirect(
      method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
      )
   )
   private RenderType havoc$useTranslucentArmorLayer(Identifier texture) {
      return this.shouldRenderTranslucentArmor() && !this.isArmorFullyOpaque() ? RenderTypes.armorTranslucent(texture) : RenderTypes.armorCutoutNoCull(texture);
   }

   @Redirect(
      method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
      )
   )
   private void havoc$shadeArmorModels(
      OrderedSubmitNodeCollector queue,
      Model model,
      Object state,
      PoseStack matrices,
      RenderType renderLayer,
      int light,
      int overlay,
      int color,
      TextureAtlasSprite sprite,
      int outlineColor,
      CrumblingOverlay crumblingOverlay,
      LayerType layerType,
      ResourceKey<?> assetId,
      Model ignoredModel,
      Object ignoredState,
      ItemStack stack,
      PoseStack ignoredMatrices,
      SubmitNodeCollector orderedQueue,
      int ignoredLight,
      Identifier texture
   ) {
      if (!(state instanceof AvatarRenderState) || !this.shouldRenderTranslucentArmor()) {
         queue.submitModel(model, state, matrices, renderLayer, light, overlay, color, sprite, outlineColor, crumblingOverlay);
      } else if (this.isArmorFullyOpaque()) {
         queue.submitModel(model, state, matrices, renderLayer, light, overlay, -16777216, sprite, outlineColor, crumblingOverlay);
      } else {
         int translucentColor = 0 | this.getArmorAlpha() << 24;
         queue.submitModel(model, state, matrices, renderLayer, light, overlay, translucentColor, sprite, outlineColor, crumblingOverlay);
      }
   }

   private boolean shouldRenderTranslucentArmor() {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null && havoc.getModuleManager() != null) {
         Chams chams = havoc.getModuleManager().getModule(Chams.class);
         return chams != null && chams.isEnable() && chams.isArmorVisible();
      } else {
         return false;
      }
   }

   private int getArmorAlpha() {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null && havoc.getModuleManager() != null) {
         Chams chams = havoc.getModuleManager().getModule(Chams.class);
         return chams == null ? 115 : chams.getArmorAlpha();
      } else {
         return 115;
      }
   }

   private boolean isArmorFullyOpaque() {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null && havoc.getModuleManager() != null) {
         Chams chams = havoc.getModuleManager().getModule(Chams.class);
         return chams != null && chams.isArmorFullyOpaque();
      } else {
         return false;
      }
   }
}
