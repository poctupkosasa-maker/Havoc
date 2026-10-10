package havoc.mixin;

import havoc.Havoc;
import havoc.event.impl.EventInput;
import havoc.module.impl.misc.AntiCapture;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
   @Unique
   private float havoc$getMovementMultiplier(boolean positive, boolean negative) {
      if (positive == negative) {
         return 0.0F;
      } else {
         return positive ? 1.0F : -1.0F;
      }
   }

   @Inject(method = "tick", at = @At("TAIL"))
   private void havoc$onTick(CallbackInfo ci) {
      if (AntiCapture.isInputLocked()) {
         this.moveVector = Vec2.ZERO;
         this.keyPresses = new Input(false, false, false, false, false, false, false);
         return;
      }
      Havoc havoc = Havoc.getInstance();
      if (havoc != null) {
         EventInput event = new EventInput(
            this.havoc$getMovementMultiplier(this.keyPresses.forward(), this.keyPresses.backward()),
            this.havoc$getMovementMultiplier(this.keyPresses.left(), this.keyPresses.right()),
            this.keyPresses.jump(),
            this.keyPresses.shift(),
            this.keyPresses.sprint()
         );
         havoc.getEventBus().post(event);
         float correctedForward = event.getForward();
         float correctedStrafe = event.getStrafe();
         this.moveVector = new Vec2(correctedStrafe, correctedForward);
         this.keyPresses = new Input(
            correctedForward > 0.0F, correctedForward < 0.0F, correctedStrafe > 0.0F, correctedStrafe < 0.0F, event.isJump(), event.isSneak(), event.isSprint()
         );
      }
   }
}
