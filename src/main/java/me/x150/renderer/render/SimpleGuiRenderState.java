package me.x150.renderer.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

public record SimpleGuiRenderState(
   RenderPipeline pipeline, TextureSetup textureSetup, ScreenRectangle scissorArea, ScreenRectangle bounds, Consumer<VertexConsumer> vertices
) implements GuiElementRenderState {
   public SimpleGuiRenderState(
      RenderPipeline pipeline, TextureSetup textureSetup, GuiGraphicsExtractor context, ScreenRectangle bounds, Consumer<VertexConsumer> vertices
   ) {
      this(pipeline, textureSetup, currentScissor(context), clippedBounds(context, bounds), visibleVertices(context, bounds, vertices));
   }

   private static ScreenRectangle currentScissor(GuiGraphicsExtractor context) {
      return context instanceof GuiScissorTracker tracker ? tracker.havoc$currentScissor() : null;
   }

   private static ScreenRectangle clippedBounds(GuiGraphicsExtractor context, ScreenRectangle bounds) {
      ScreenRectangle scissor = currentScissor(context);
      if (scissor != null && bounds != null) {
         ScreenRectangle clipped = scissor.intersection(bounds);
         return clipped != null ? clipped : new ScreenRectangle(0, 0, 0, 0);
      } else {
         return bounds;
      }
   }

   private static Consumer<VertexConsumer> visibleVertices(GuiGraphicsExtractor context, ScreenRectangle bounds, Consumer<VertexConsumer> vertices) {
      ScreenRectangle scissor = currentScissor(context);
      if (scissor != null && bounds != null) {
         ScreenRectangle clipped = scissor.intersection(bounds);
         return clipped != null && clipped.width() > 0 && clipped.height() > 0 ? vertices : ignored -> {};
      } else {
         return vertices;
      }
   }

   public void buildVertices(VertexConsumer vertices) {
      this.vertices.accept(vertices);
   }
}
