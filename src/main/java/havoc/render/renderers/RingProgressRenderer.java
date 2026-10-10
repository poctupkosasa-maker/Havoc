package havoc.render.renderers;

import com.mojang.blaze3d.vertex.BufferBuilder;
import havoc.render.renderers.pipeline.PipelineRegistry;
import me.x150.renderer.mixin.DrawContextAccessor;
import me.x150.renderer.render.SimpleGuiRenderState;
import me.x150.renderer.util.DirectVertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

public record RingProgressRenderer(float diameter, int color, float progress, float thickness, float smoothness) implements Renderer {
   private static ScreenRectangle createBounds(GuiGraphicsExtractor c, float x, float y, float w, float h) {
      Matrix3x2fStack mat = c.pose();
      return new ScreenRectangle((int)Math.floor(x), (int)Math.floor(y), (int)Math.ceil(w), (int)Math.ceil(h)).transformMaxBounds(mat);
   }

   @Override
   public void render(double x, double y, GuiGraphicsExtractor ctx) {
      if (PipelineRegistry.RING_PROGRESS_PIPELINE != null) {
         Matrix3x2f guiMatrices = new Matrix3x2f(ctx.pose());
         int vertexColor = Renderer.toAbgr(this.color);
         float clampedProgress = Math.max(0.0F, Math.min(1.0F, this.progress));
         float safeThickness = Math.max(0.01F, this.thickness);
         SimpleGuiRenderState state = new SimpleGuiRenderState(
            PipelineRegistry.RING_PROGRESS_PIPELINE,
            TextureSetup.noTexture(),
            ctx,
            createBounds(ctx, (float)x - 1.0F, (float)y - 1.0F, this.diameter + 2.0F, this.diameter + 2.0F),
            buffer -> {
               DirectVertexConsumer dvc = new DirectVertexConsumer((BufferBuilder)buffer, false);
               dvc.addVertexWith2DPose(guiMatrices, (float)x, (float)y)
                  .setUv(0.0F, 0.0F)
                  .setColor(vertexColor)
                  .setUv(this.diameter, this.diameter)
                  .setUv(0.0F, 0.0F)
                  .setUv(0.0F, 0.0F)
                  .setUv(clampedProgress, safeThickness)
                  .setUv(this.smoothness, 0.0F);
               dvc.addVertexWith2DPose(guiMatrices, (float)x, (float)(y + this.diameter))
                  .setUv(0.0F, 1.0F)
                  .setColor(vertexColor)
                  .setUv(this.diameter, this.diameter)
                  .setUv(0.0F, 0.0F)
                  .setUv(0.0F, 0.0F)
                  .setUv(clampedProgress, safeThickness)
                  .setUv(this.smoothness, 0.0F);
               dvc.addVertexWith2DPose(guiMatrices, (float)(x + this.diameter), (float)(y + this.diameter))
                  .setUv(1.0F, 1.0F)
                  .setColor(vertexColor)
                  .setUv(this.diameter, this.diameter)
                  .setUv(0.0F, 0.0F)
                  .setUv(0.0F, 0.0F)
                  .setUv(clampedProgress, safeThickness)
                  .setUv(this.smoothness, 0.0F);
               dvc.addVertexWith2DPose(guiMatrices, (float)(x + this.diameter), (float)y)
                  .setUv(1.0F, 0.0F)
                  .setColor(vertexColor)
                  .setUv(this.diameter, this.diameter)
                  .setUv(0.0F, 0.0F)
                  .setUv(0.0F, 0.0F)
                  .setUv(clampedProgress, safeThickness)
                  .setUv(this.smoothness, 0.0F);
            }
         );
         ((DrawContextAccessor)ctx).getState().addGuiElement(state);
      }
   }
}
