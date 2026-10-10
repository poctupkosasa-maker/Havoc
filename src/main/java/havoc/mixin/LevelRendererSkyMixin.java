package havoc.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import havoc.module.impl.visuals.Ambience;
import havoc.render.AmbienceSkyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType.Skybox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererSkyMixin {
   @Inject(method = "lambda$addSkyPass$0", at = @At("RETURN"))
   private void havoc$renderSummerSky(GpuBufferSlice fogUniform, SkyRenderState skyState, CallbackInfo ci) {
      Ambience ambience = Ambience.getInstance();
      Minecraft minecraft = Minecraft.getInstance();
      if (ambience != null
         && ambience.hasCustomSky()
         && minecraft != null
         && minecraft.level != null
         && skyState.skybox != Skybox.END
         && skyState.skybox != Skybox.NONE) {
         AmbienceSkyRenderer.draw(minecraft.gameRenderer.mainCamera(), ambience);
      }
   }
}
