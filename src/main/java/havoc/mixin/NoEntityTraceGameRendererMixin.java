package havoc.mixin;

import havoc.Havoc;
import havoc.module.impl.player.NoEntityTrace;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class NoEntityTraceGameRendererMixin {
   @Inject(method = "pick", at = @At("TAIL"))
   private void havoc$ignoreEntityTrace(float tickDelta, CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      NoEntityTrace module = Havoc.getInstance().getModuleManager().getModule(NoEntityTrace.class);
      if (client.player != null && module != null && module.shouldIgnoreEntityTrace() && client.hitResult instanceof EntityHitResult) {
         client.hitResult = client.player.pick(6.0, tickDelta, false);
         client.crosshairPickEntity = null;
      }
   }
}
