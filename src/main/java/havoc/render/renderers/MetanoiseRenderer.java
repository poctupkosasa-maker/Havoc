package havoc.render.renderers;

import com.mojang.blaze3d.vertex.BufferBuilder;
import havoc.render.builders.states.QuadRadiusState;
import havoc.render.builders.states.SizeState;
import havoc.render.renderers.pipeline.PipelineRegistry;
import me.x150.renderer.mixin.DrawContextAccessor;
import me.x150.renderer.render.SimpleGuiRenderState;
import me.x150.renderer.util.DirectVertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2f;

public record MetanoiseRenderer(SizeState size, QuadRadiusState radius, int color, float smoothness, float progress) implements Renderer {
   @Override
   public void render(double x, double y, GuiGraphicsExtractor context) {
      if (PipelineRegistry.METANOISE_PIPELINE != null) {
         Matrix3x2f matrix = new Matrix3x2f(context.pose());
         ScreenRectangle bounds = new ScreenRectangle(
               (int)Math.floor(x), (int)Math.floor(y), (int)Math.ceil(this.size.width()), (int)Math.ceil(this.size.height())
            )
            .transformMaxBounds(context.pose());
         int abgr = Renderer.toAbgr(this.color);
         SimpleGuiRenderState state = new SimpleGuiRenderState(PipelineRegistry.METANOISE_PIPELINE, TextureSetup.noTexture(), context, bounds, buffer -> {
            DirectVertexConsumer out = new DirectVertexConsumer((BufferBuilder)buffer, false);
            this.vertex(out, matrix, (float)x, (float)y, 0.0F, 0.0F, abgr);
            this.vertex(out, matrix, (float)x, (float)y + this.size.height(), 0.0F, 1.0F, abgr);
            this.vertex(out, matrix, (float)x + this.size.width(), (float)y + this.size.height(), 1.0F, 1.0F, abgr);
            this.vertex(out, matrix, (float)x + this.size.width(), (float)y, 1.0F, 0.0F, abgr);
         });
         ((DrawContextAccessor)context).getState().addGuiElement(state);
      }
   }

   private void vertex(DirectVertexConsumer out, Matrix3x2f matrix, float x, float y, float u, float v, int color) {
      out.addVertexWith2DPose(matrix, x, y)
         .setUv(u, v)
         .setColor(color)
         .setUv(this.size.width(), this.size.height())
         .setUv(this.radius.radius1(), this.radius.radius2())
         .setUv(this.radius.radius3(), this.radius.radius4())
         .setUv(this.smoothness, this.progress);
   }
}
