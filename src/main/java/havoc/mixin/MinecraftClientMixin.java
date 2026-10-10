package havoc.mixin;

import havoc.Havoc;
import havoc.module.impl.misc.AntiCapture;
import havoc.utilites.ServerInfoHelper;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
   private int havoc$antiCaptureCounter;

   @Inject(method = "tick", at = @At("HEAD"))
   private void onTick(CallbackInfo ci) {
      ServerInfoHelper.onTick();
      if (++this.havoc$antiCaptureCounter >= 20) {
         this.havoc$antiCaptureCounter = 0;
         Havoc havoc = Havoc.getInstance();
         if (havoc != null) {
            AntiCapture antiCapture = havoc.getModuleManager().getModule(AntiCapture.class);
            if (antiCapture != null) {
               antiCapture.tickCheck();
            }
         }
      }
   }
}
