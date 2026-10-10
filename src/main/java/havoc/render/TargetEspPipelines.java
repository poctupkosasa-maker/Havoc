package havoc.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.function.Function;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public final class TargetEspPipelines {
   private static final RenderPipeline MARKER = RenderPipeline.builder(new Snippet[0])
      .withBindGroupLayout(BindGroupLayouts.GLOBALS)
      .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
      .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
      .withLocation("pipeline/havoc_target_marker")
      .withVertexShader("core/position_tex_color")
      .withFragmentShader("core/position_tex_color")
      .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
      .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
      .withCull(false)
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .build();
   private static final RenderPipeline RING = RenderPipeline.builder(new Snippet[0])
      .withBindGroupLayout(BindGroupLayouts.GLOBALS)
      .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
      .withLocation("pipeline/havoc_target_ring")
      .withVertexShader("core/position_color")
      .withFragmentShader("core/position_color")
      .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
      .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
      .withCull(false)
      .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
      .withPrimitiveTopology(PrimitiveTopology.QUADS)
      .build();
   public static final Function<Identifier, RenderType> MARKER_LAYER = Util.memoize(
      texture -> RenderType.create("havoc_target_marker", RenderSetup.builder(MARKER).withTexture("Sampler0", texture).sortOnUpload().createRenderSetup())
   );
   public static final RenderType RING_LAYER = RenderType.create("havoc_target_ring", RenderSetup.builder(RING).sortOnUpload().createRenderSetup());
   private TargetEspPipelines() {
   }
}
