package havoc.render.renderers;

import com.mojang.blaze3d.vertex.BufferBuilder;
import havoc.render.builders.states.QuadColorState;
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
import org.joml.Matrix3x2fStack;

public record GlowRenderer(SizeState size, QuadRadiusState radius, QuadColorState color, float smoothness, float spread) implements Renderer {
   private static final float SHADOW_GAP_COMPENSATION = 1.0F;
   private static final float SHADOW_INSET = 1.0F;

   private static ScreenRectangle createBounds(GuiGraphicsExtractor c, float x, float y, float w, float h) {
      Matrix3x2fStack mat = c.pose();
      return new ScreenRectangle((int)Math.floor(x), (int)Math.floor(y), (int)Math.ceil(w), (int)Math.ceil(h)).transformMaxBounds(mat);
   }

   @Override
   public void render(double x, double y, GuiGraphicsExtractor ctx) {
      if (PipelineRegistry.GLOW_PIPELINE != null) {
         float safeSpread = Math.max(0.0F, this.spread - 1.0F);
         float baseX = (float)x + 1.0F;
         float baseY = (float)y + 1.0F;
         float baseWidth = Math.max(0.0F, this.size.width() - 2.0F);
         float baseHeight = Math.max(0.0F, this.size.height() - 2.0F);
         float radius1 = Math.max(0.0F, this.radius.radius1() - 1.0F);
         float radius2 = Math.max(0.0F, this.radius.radius2() - 1.0F);
         float radius3 = Math.max(0.0F, this.radius.radius3() - 1.0F);
         float radius4 = Math.max(0.0F, this.radius.radius4() - 1.0F);
         float expandedX = baseX - safeSpread;
         float expandedY = baseY - safeSpread;
         float expandedWidth = baseWidth + safeSpread * 2.0F;
         float expandedHeight = baseHeight + safeSpread * 2.0F;
         Matrix3x2f guiMatrices = new Matrix3x2f(ctx.pose());
         SimpleGuiRenderState state = new SimpleGuiRenderState(
            PipelineRegistry.GLOW_PIPELINE,
            TextureSetup.noTexture(),
            ctx,
            createBounds(ctx, expandedX, expandedY, expandedWidth, expandedHeight),
            buffer -> {
               DirectVertexConsumer dvc = new DirectVertexConsumer((BufferBuilder)buffer, false);
               int c1 = Renderer.toAbgr(this.color.color1());
               int c2 = Renderer.toAbgr(this.color.color2());
               int c3 = Renderer.toAbgr(this.color.color3());
               int c4 = Renderer.toAbgr(this.color.color4());
               dvc.addVertexWith2DPose(guiMatrices, expandedX, expandedY)
                  .setUv(0.0F, 0.0F)
                  .setColor(c1)
                  .setUv(expandedWidth, expandedHeight)
                  .setUv(radius1, radius2)
                  .setUv(radius3, radius4)
                  .setUv(this.smoothness, safeSpread);
               dvc.addVertexWith2DPose(guiMatrices, expandedX, expandedY + expandedHeight)
                  .setUv(0.0F, 1.0F)
                  .setColor(c2)
                  .setUv(expandedWidth, expandedHeight)
                  .setUv(radius1, radius2)
                  .setUv(radius3, radius4)
                  .setUv(this.smoothness, safeSpread);
               dvc.addVertexWith2DPose(guiMatrices, expandedX + expandedWidth, expandedY + expandedHeight)
                  .setUv(1.0F, 1.0F)
                  .setColor(c3)
                  .setUv(expandedWidth, expandedHeight)
                  .setUv(radius1, radius2)
                  .setUv(radius3, radius4)
                  .setUv(this.smoothness, safeSpread);
               dvc.addVertexWith2DPose(guiMatrices, expandedX + expandedWidth, expandedY)
                  .setUv(1.0F, 0.0F)
                  .setColor(c4)
                  .setUv(expandedWidth, expandedHeight)
                  .setUv(radius1, radius2)
                  .setUv(radius3, radius4)
                  .setUv(this.smoothness, safeSpread);
            }
         );
         ((DrawContextAccessor)ctx).getState().addGuiElement(state);
      }
   }
}
