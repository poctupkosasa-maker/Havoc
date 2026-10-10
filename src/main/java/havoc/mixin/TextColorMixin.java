package havoc.mixin;

import havoc.utilites.color.ITextColorWithAlpha;
import havoc.utilites.color.TextColorAlphaStorage;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextColor.class)
public class TextColorMixin implements ITextColorWithAlpha {
   @Final
   @Shadow
   private int value;
   @Unique
   private int fullColor = 0;
   @Unique
   private boolean hasAlpha = false;

   @ModifyVariable(method = "<init>(I)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private static int captureFullColor(int rgb) {
      TextColorAlphaStorage.setPending(rgb);
      return rgb;
   }

   @ModifyVariable(method = "<init>(ILjava/lang/String;)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
   private static int captureFullColorNamed(int rgb) {
      TextColorAlphaStorage.setPending(rgb);
      return rgb;
   }

   @Inject(method = "<init>(I)V", at = @At("RETURN"))
   private void storeCapturedColor(int rgb, CallbackInfo ci) {
      int pending = TextColorAlphaStorage.getPending();
      this.fullColor = pending;
      this.hasAlpha = (pending & 0xFF000000) != 0;
      TextColorAlphaStorage.clearPending();
   }

   @Inject(method = "<init>(ILjava/lang/String;)V", at = @At("RETURN"))
   private void storeCapturedColorNamed(int rgb, String name, CallbackInfo ci) {
      int pending = TextColorAlphaStorage.getPending();
      this.fullColor = pending;
      this.hasAlpha = (pending & 0xFF000000) != 0;
      TextColorAlphaStorage.clearPending();
   }

   @Unique
   @Override
   public int getFullColor() {
      int result;
      if (this.hasAlpha) {
         result = this.fullColor & 0xFF000000 | this.value & 16777215;
      } else {
         result = 0xFF000000 | this.value;
      }

      return result;
   }
}
