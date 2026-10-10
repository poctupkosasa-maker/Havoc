package havoc.mixin;

import havoc.Havoc;
import havoc.event.impl.EventAttackEntity;
import havoc.module.impl.combat.NoInteract;
import havoc.module.impl.misc.NoSpherePlace;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {
   private boolean havoc$noInteract() {
      Havoc havoc = Havoc.getInstance();
      NoInteract module = havoc == null ? null : havoc.getModuleManager().getModule(NoInteract.class);
      return module != null && module.isEnable();
   }

   @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
   private void havoc$cancelBlockInteract(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
      if (this.havoc$noInteract() || this.havoc$noSphere(player, hand)) {
         cir.setReturnValue(InteractionResult.PASS);
      }
   }

   @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
   private void havoc$cancelItemInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
      if (this.havoc$noInteract() || this.havoc$noSphere(player, hand)) {
         cir.setReturnValue(InteractionResult.PASS);
      }
   }

   private boolean havoc$noSphere(Player player, InteractionHand hand) {
      Havoc havoc = Havoc.getInstance();
      NoSpherePlace module = havoc == null ? null : havoc.getModuleManager().getModule(NoSpherePlace.class);
      return module != null && module.isEnable() && module.shouldCancel(hand == InteractionHand.OFF_HAND ? player.getOffhandItem() : player.getMainHandItem());
   }

   @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
   private void havoc$onAttackEntity(Player player, Entity target, CallbackInfo ci) {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null) {
         EventAttackEntity event = new EventAttackEntity(target);
         havoc.getEventBus().post(event);
         if (event.isCanceled()) {
            ci.cancel();
         }
      }
   }
}
