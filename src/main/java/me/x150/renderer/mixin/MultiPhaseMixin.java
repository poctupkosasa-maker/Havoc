package me.x150.renderer.mixin;

import com.mojang.blaze3d.systems.RenderPass;
import java.util.function.Consumer;
import me.x150.renderer.util.MoreMultiPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "net.minecraft.class_1921$class_4687")
public class MultiPhaseMixin implements MoreMultiPhase {
   @Unique
   private Consumer<RenderPass> renderPassSetup;

   @Override
   public MoreMultiPhase withRenderPassSetup(Consumer<RenderPass> rp) {
      this.renderPassSetup = rp;
      return this;
   }
}
