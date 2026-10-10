package havoc.mixin;

import havoc.module.impl.misc.AntiCapture;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class RemoteBlackoutScreenMixin {
   @Unique
   private int havoc$blackoutTick;

   @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
   private void havoc$blackout(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      this.havoc$maybeBlackout(context);
   }

   @Unique
   private void havoc$maybeBlackout(GuiGraphicsExtractor context) {
      if (this.havoc$blackoutTick++ % 10 != 0) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.getWindow() != null && AntiCapture.isBlackoutActive()) {
         context.fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), -16777216);
      }
   }
}
