package havoc.mixin;

import havoc.module.impl.misc.AntiCapture;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class RemoteBlackoutHudMixin {
   @Unique
   private int havoc$blackoutTick;

   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void havoc$blackout(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
      if (this.havoc$blackoutTick++ % 4 != 0) {
         return;
      }

      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.getWindow() != null && AntiCapture.isBlackoutActive()) {
         context.fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), -16777216);
      }
   }
}
