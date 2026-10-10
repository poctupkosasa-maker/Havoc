package havoc.mixin;

import havoc.Havoc;
import havoc.module.impl.player.NoPush;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMovementMixin {
   @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
   private void havoc$cancelEntityPush(Entity entity, CallbackInfo ci) {
      Entity self = (Entity)(Object)this;
      if (self == Minecraft.getInstance().player) {
         Havoc havoc = Havoc.getInstance();
         NoPush module = havoc == null ? null : havoc.getModuleManager().getModule(NoPush.class);
         if (module != null && module.isEnable() && module.entities()) {
            ci.cancel();
         }
      }
   }

   @Redirect(
      method = "isLocalInstanceAuthoritative",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isClientAuthoritative()Z"),
      require = 0
   )
   private boolean havoc$fixFallDistanceValue(Entity instance) {
      return instance.isClientAuthoritative();
   }
}
