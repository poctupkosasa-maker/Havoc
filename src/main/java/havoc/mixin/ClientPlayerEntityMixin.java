package havoc.mixin;

import havoc.Havoc;
import havoc.event.impl.EventUpdate;
import havoc.utilites.InventoryUtil;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class ClientPlayerEntityMixin {
   @Inject(method = "tick", at = @At("HEAD"))
   private void havoc$triggerPlayerTickEvent(CallbackInfo ci) {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null) {
         InventoryUtil.tickRestore();
         havoc.getEventBus().post(new EventUpdate());
      }
   }
}
