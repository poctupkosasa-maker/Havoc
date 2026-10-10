package havoc.mixin;

import havoc.module.impl.visuals.Ambience;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class WorldTimeMixin {
   @Inject(method = "getOverworldClockTime", at = @At("HEAD"), cancellable = true)
   private void havoc$overrideOverworldTime(CallbackInfoReturnable<Long> cir) {
      this.havoc$applyTime(cir);
   }

   @Inject(method = "getDefaultClockTime", at = @At("HEAD"), cancellable = true)
   private void havoc$overrideDefaultTime(CallbackInfoReturnable<Long> cir) {
      this.havoc$applyTime(cir);
   }

   @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true, require = 0)
   private void havoc$overrideTimelineTimeYarn(CallbackInfoReturnable<Long> cir) {
      this.havoc$applyTime(cir);
   }

   @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true, require = 0)
   private void havoc$overrideTimelineTimeMojmap(CallbackInfoReturnable<Long> cir) {
      this.havoc$applyTime(cir);
   }

   private void havoc$applyTime(CallbackInfoReturnable<Long> cir) {
      Minecraft minecraft = Minecraft.getInstance();
      Ambience ambience = Ambience.getInstance();
      if (minecraft != null && minecraft.level == (Object)this && ambience != null && ambience.shouldOverrideTime()) {
         cir.setReturnValue(ambience.getForcedTimeOfDay());
      }
   }
}
