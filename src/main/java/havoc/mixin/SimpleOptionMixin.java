package havoc.mixin;

import havoc.module.impl.visuals.Gamma;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OptionInstance.class)
public class SimpleOptionMixin<T> {
   @Inject(method = "get", at = @At("HEAD"), cancellable = true)
   private void havoc$getGammaOverride(CallbackInfoReturnable<T> cir) {
      Gamma gammaModule = Gamma.getInstance();
      Minecraft minecraft = Minecraft.getInstance();
      if (gammaModule != null && gammaModule.isEnable() && minecraft != null && minecraft.options != null && minecraft.options.gamma() == (Object)this) {
         T gamma = (T)(Object)Double.valueOf(Math.min(1.0, gammaModule.getGammaValue() / 32.0));
         cir.setReturnValue(gamma);
      }
   }
}