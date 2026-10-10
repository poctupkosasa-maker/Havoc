package havoc.render.renderers.pipeline;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;

public final class PipelineRegistry {
   private static final DepthStencilState GUI_DEPTH = new DepthStencilState(CompareOp.ALWAYS_PASS, false);
   public static RenderPipeline RECTANGLE_PIPELINE;
   public static RenderPipeline BORDER_PIPELINE;
   public static RenderPipeline TEXTURE_PIPELINE;
   public static RenderPipeline HEAD_TEXTURE_PIPELINE;
   public static RenderPipeline BLUR_PIPELINE;
   public static RenderPipeline GLOW_PIPELINE;
   public static RenderPipeline CIRCLE_PIPELINE;
   public static RenderPipeline METANOISE_PIPELINE;
   public static RenderPipeline RING_PROGRESS_PIPELINE;
   public static RenderPipeline JUMP_CIRCLE_PIPELINE;
   public static RenderPipeline MSDF_FONT_PIPELINE;

   private PipelineRegistry() {
   }

   public static void init() {
      RECTANGLE_PIPELINE = create("rectangle", false, format("UV1", "RadiusXY", "RadiusZW", "ExtraData1"));
      BORDER_PIPELINE = create("border", false, format("UV1", "RadiusXY", "RadiusZW", "BorderData1", "BorderData2"));
      TEXTURE_PIPELINE = create("texture", true, format("UV1", "RadiusXY", "RadiusZW", "ExtraData1"));
      HEAD_TEXTURE_PIPELINE = create("head_texture", true, format("UV1", "RadiusXY", "RadiusZW", "ExtraData1", "ExtraData2"));
      BLUR_PIPELINE = create("blur", true, format("UV1", "RadiusXY", "RadiusZW", "BlurData"));
      GLOW_PIPELINE = create("glow", false, format("UV1", "RadiusXY", "RadiusZW", "GlowData"));
      CIRCLE_PIPELINE = create("circle", false, format("UV1", "RadiusXY", "RadiusZW", "ExtraData1"));
      METANOISE_PIPELINE = create("metanoise", false, format("UV1", "RadiusXY", "RadiusZW", "ExtraData1"));
      RING_PROGRESS_PIPELINE = create("ring_progress", false, format("UV1", "RadiusXY", "RadiusZW", "ExtraData1", "ExtraData2"));
      MSDF_FONT_PIPELINE = create("msdf_font", true, baseFormat());
   }

   private static RenderPipeline create(String shader, boolean textured, VertexFormat format) {
      Builder builder = RenderPipeline.builder(new Snippet[0])
         .withBindGroupLayout(BindGroupLayouts.GLOBALS)
         .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
         .withLocation(Identifier.fromNamespaceAndPath("havoc", "pipeline/" + shader))
         .withVertexShader(Identifier.fromNamespaceAndPath("havoc", "core/" + shader))
         .withFragmentShader(Identifier.fromNamespaceAndPath("havoc", "core/" + shader))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withDepthStencilState(GUI_DEPTH)
         .withCull(false)
         .withVertexBinding(0, format)
         .withPrimitiveTopology(PrimitiveTopology.QUADS);
      if (textured) {
         builder.withBindGroupLayout(BindGroupLayouts.SAMPLER0);
      }

      ((net.fabricmc.fabric.api.client.rendering.v1.FabricRenderPipeline.Builder)builder).withUsePipelineDrawModeForGui(true);
      return builder.build();
   }

   private static VertexFormat baseFormat() {
      return VertexFormat.builder(0)
         .addAttribute("Position", GpuFormat.RGB32_FLOAT)
         .addAttribute("UV0", GpuFormat.RG32_FLOAT)
         .addAttribute("Color", GpuFormat.RGBA8_UNORM)
         .build();
   }

   private static VertexFormat format(String... extraAttributes) {
      com.mojang.blaze3d.vertex.VertexFormat.Builder builder = VertexFormat.builder(0)
         .addAttribute("Position", GpuFormat.RGB32_FLOAT)
         .addAttribute("UV0", GpuFormat.RG32_FLOAT)
         .addAttribute("Color", GpuFormat.RGBA8_UNORM);

      for (String attribute : extraAttributes) {
         builder.addAttribute(attribute, GpuFormat.RG32_FLOAT);
      }

      return builder.build();
   }
}
