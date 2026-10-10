package havoc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import havoc.Havoc;
import havoc.module.impl.visuals.SwingAnimation;
import havoc.module.impl.visuals.ViewModel;
import havoc.utilites.InventoryUtil;
import havoc.utilites.rotation.SilentAimManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {
   @Unique
   private boolean havoc$customSwingAnimation;

   @WrapOperation(
      method = "submitArmWithItem",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V"
      )
   )
   private void havoc$swingAnimation(
      ItemInHandRenderer renderer,
      PoseStack matrices,
      HumanoidArm arm,
      float equipProgress,
      Operation<Void> original,
      AbstractClientPlayer player,
      float tickDelta,
      float pitch,
      InteractionHand hand,
      float swingProgress
   ) {
      SwingAnimation animation = Havoc.getInstance().getModuleManager().getModule(SwingAnimation.class);
      boolean usingThisHand = player.isUsingItem() && player.getUsedItemHand() == hand;
      if (!usingThisHand && hand == InteractionHand.MAIN_HAND && animation != null && animation.shouldApply(swingProgress)) {
         animation.apply(matrices, arm, swingProgress);
         this.havoc$customSwingAnimation = true;
      } else {
         this.havoc$customSwingAnimation = false;
         original.call(new Object[]{renderer, matrices, arm, equipProgress});
      }
   }

   @WrapOperation(
      method = "submitArmWithItem",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"
      )
   )
   private void havoc$skipVanillaSwing(
      ItemInHandRenderer renderer, float swingProgress, PoseStack matrices, int armX, HumanoidArm arm, Operation<Void> original
   ) {
      if (!this.havoc$customSwingAnimation) {
         original.call(new Object[]{renderer, swingProgress, matrices, armX, arm});
      }
   }

   @Inject(
      method = "submitArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
      at = @At("HEAD")
   )
   private void havoc$applyViewModel(
      AbstractClientPlayer player,
      float tickDelta,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack matrices,
      SubmitNodeCollector queue,
      int light,
      CallbackInfo ci
   ) {
      this.havoc$customSwingAnimation = false;
      ViewModel viewModel = ViewModel.getInstance();
      if (viewModel != null && viewModel.isEnable() && matrices != null && hand != null && stack != null) {
         if (hand != InteractionHand.MAIN_HAND || !(stack.getItem() instanceof CrossbowItem)) {
            if (hand == InteractionHand.MAIN_HAND) {
               matrices.translate(viewModel.getMainHandX().getValue(), viewModel.getMainHandY().getValue(), viewModel.getMainHandZ().getValue());
            } else {
               matrices.translate(viewModel.getOffHandX().getValue(), viewModel.getOffHandY().getValue(), viewModel.getOffHandZ().getValue());
            }
         }
      }
   }

   @Inject(
      method = "submitArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
      at = @At("RETURN")
   )
   private void havoc$revertViewModel(
      AbstractClientPlayer player,
      float tickDelta,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack matrices,
      SubmitNodeCollector queue,
      int light,
      CallbackInfo ci
   ) {
      ViewModel viewModel = ViewModel.getInstance();
      if (viewModel != null && viewModel.isEnable() && matrices != null && hand != null && stack != null) {
         if (hand != InteractionHand.MAIN_HAND || !(stack.getItem() instanceof CrossbowItem)) {
            if (hand == InteractionHand.MAIN_HAND) {
               matrices.translate(-viewModel.getMainHandX().getValue(), -viewModel.getMainHandY().getValue(), -viewModel.getMainHandZ().getValue());
            } else {
               matrices.translate(-viewModel.getOffHandX().getValue(), -viewModel.getOffHandY().getValue(), -viewModel.getOffHandZ().getValue());
            }
         }
      }
   }

   @Redirect(
      method = "tick()V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;")
   )
   private ItemStack havoc$tickSilentMainHand(LocalPlayer player) {
      return InventoryUtil.getSilentMainHandRenderStack(player.getMainHandItem());
   }

   @ModifyArg(
      method = "submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;submitArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
         ordinal = 0
      ),
      index = 5
   )
   private ItemStack havoc$submitSilentMainHand(ItemStack stack) {
      return InventoryUtil.getSilentMainHandRenderStack(stack);
   }

   @Redirect(
      method = "submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getViewXRot(F)F")
   )
   private float redirectGetPitch(LocalPlayer player, float tickDelta) {
      return SilentAimManager.isActive() ? SilentAimManager.getFreePitch() : player.getViewXRot(tickDelta);
   }

   @Redirect(
      method = "submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getViewYRot(F)F")
   )
   private float redirectGetYaw(LocalPlayer player, float tickDelta) {
      return SilentAimManager.isActive() ? SilentAimManager.getFreeYaw() : player.getViewYRot(tickDelta);
   }

   @Redirect(
      method = "submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F", ordinal = 0)
   )
   private float useCameraPitch(float delta, float start, float end) {
      return Minecraft.getInstance().gameRenderer.mainCamera().xRot();
   }

   @Redirect(
      method = "submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F", ordinal = 1)
   )
   private float useCameraYaw(float delta, float start, float end) {
      return Minecraft.getInstance().gameRenderer.mainCamera().yRot();
   }
}
