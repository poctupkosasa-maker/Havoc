package havoc.mixin;

import havoc.module.impl.visuals.Ambience;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientLevel$ClientLevelData")
public class ClientWorldPropertiesMixin {
   @Inject(method = "getTime", at = @At("HEAD"), cancellable = true)
   private void havoc$overrideClientTime(CallbackInfoReturnable<Long> cir) {
      Ambience ambience = Ambience.getInstance();
      if (ambience != null && ambience.shouldOverrideTime()) {
         cir.setReturnValue(ambience.getForcedTimeOfDay());
      }
   }

   @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
   private void havoc$overrideClientTimeOfDay(CallbackInfoReturnable<Long> cir) {
      Ambience ambience = Ambience.getInstance();
      if (ambience != null && ambience.shouldOverrideTime()) {
         cir.setReturnValue(ambience.getForcedTimeOfDay());
      }
   }
}
