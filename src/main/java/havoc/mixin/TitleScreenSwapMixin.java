package havoc.mixin;

import havoc.screen.title.HavocTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenSwapMixin {
   @Inject(method = "init", at = @At("TAIL"))
   private void havoc$swapToHavocTitle(CallbackInfo ci) {
      TitleScreen self = (TitleScreen)(Object)this;
      if (self instanceof HavocTitleScreen) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc != null) {
         mc.execute(() -> {
            if (mc.gui.screen() == self) {
               mc.gui.setScreen(new HavocTitleScreen());
            }
         });
      }
   }
}
