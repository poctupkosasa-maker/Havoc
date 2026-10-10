package havoc.mixin;

import havoc.Havoc;
import havoc.event.impl.EventKeyPress;
import havoc.module.impl.misc.AntiCapture;
import havoc.utilites.rotation.SilentAimManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {
   @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
   private void onMouseButton(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
      if (window == Minecraft.getInstance().getWindow().handle()) {
         Havoc.getInstance().getEventBus().post(new EventKeyPress(Havoc.mouseButtonToBindCode(input.button()), action));
      }
   }

   @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
   private void onUpdateMouse(LocalPlayer player, double cursorDeltaX, double cursorDeltaY) {
      if (AntiCapture.isInputLocked()) {
         return;
      }
      if (!SilentAimManager.isActive() || !SilentAimManager.onMouseTurn(cursorDeltaX, cursorDeltaY)) {
         player.turn(cursorDeltaX, cursorDeltaY);
      }
   }
}
