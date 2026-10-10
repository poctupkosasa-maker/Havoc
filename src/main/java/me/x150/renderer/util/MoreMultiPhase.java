package me.x150.renderer.util;

import com.mojang.blaze3d.systems.RenderPass;
import java.util.function.Consumer;

public interface MoreMultiPhase {
   MoreMultiPhase withRenderPassSetup(Consumer<RenderPass> var1);
}
