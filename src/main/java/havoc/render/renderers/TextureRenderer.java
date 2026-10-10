package havoc.render.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
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

public record TextureRenderer(SizeState size, QuadRadiusState radius, QuadColorState color, float smoothness, float rotation, GpuTextureView texture)
   implements Renderer {
   private static ScreenRectangle createBounds(GuiGraphicsExtractor context, float x, float y, float width, float height) {
      return new ScreenRectangle((int)Math.floor(x), (int)Math.floor(y), (int)Math.ceil(width), (int)Math.ceil(height)).transformMaxBounds(context.pose());
   }

   @Override
   public void render(double x, double y, GuiGraphicsExtractor context) {
      if (PipelineRegistry.TEXTURE_PIPELINE != null && this.texture != null) {
         Matrix3x2f matrices = new Matrix3x2f(context.pose());
         TextureSetup textureSetup = TextureSetup.singleTexture(this.texture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         float width = this.size.width();
         float height = this.size.height();
         float centerX = (float)x + width * 0.5F;
         float centerY = (float)y + height * 0.5F;
         float x1;
         float y1;
         float x2;
         float y2;
         float x3;
         float y3;
         float x4;
         float y4;
         float boundsWidth;
         float boundsHeight;
         if (Math.abs(this.rotation) < 0.001F) {
            x1 = (float)x;
            y1 = (float)y;
            x2 = (float)x;
            y2 = (float)y + height;
            x3 = (float)x + width;
            y3 = (float)y + height;
            x4 = (float)x + width;
            y4 = (float)y;
            boundsWidth = width;
            boundsHeight = height;
         } else {
            float radians = (float)Math.toRadians(this.rotation);
            float cos = (float)Math.cos(radians);
            float sin = (float)Math.sin(radians);
            float halfWidth = width * 0.5F;
            float halfHeight = height * 0.5F;
            x1 = centerX - halfWidth * cos + halfHeight * sin;
            y1 = centerY - halfWidth * sin - halfHeight * cos;
            x2 = centerX - halfWidth * cos - halfHeight * sin;
            y2 = centerY - halfWidth * sin + halfHeight * cos;
            x3 = centerX + halfWidth * cos - halfHeight * sin;
            y3 = centerY + halfWidth * sin + halfHeight * cos;
            x4 = centerX + halfWidth * cos + halfHeight * sin;
            y4 = centerY + halfWidth * sin - halfHeight * cos;
            boundsWidth = Math.abs(width * cos) + Math.abs(height * sin);
            boundsHeight = Math.abs(width * sin) + Math.abs(height * cos);
         }

         int color1 = Renderer.toAbgr(this.color.color1());
         int color2 = Renderer.toAbgr(this.color.color2());
         int color3 = Renderer.toAbgr(this.color.color3());
         int color4 = Renderer.toAbgr(this.color.color4());
         SimpleGuiRenderState state = new SimpleGuiRenderState(
            PipelineRegistry.TEXTURE_PIPELINE,
            textureSetup,
            context,
            createBounds(context, centerX - boundsWidth * 0.5F, centerY - boundsHeight * 0.5F, boundsWidth, boundsHeight),
            buffer -> {
               DirectVertexConsumer consumer = new DirectVertexConsumer((BufferBuilder)buffer, false);
               consumer.addVertexWith2DPose(matrices, x1, y1)
                  .setUv(0.0F, 0.0F)
                  .setColor(color1)
                  .setUv(width, height)
                  .setUv(this.radius.radius1(), this.radius.radius2())
                  .setUv(this.radius.radius3(), this.radius.radius4())
                  .setUv(this.smoothness, 0.0F);
               consumer.addVertexWith2DPose(matrices, x2, y2)
                  .setUv(0.0F, 1.0F)
                  .setColor(color2)
                  .setUv(width, height)
                  .setUv(this.radius.radius1(), this.radius.radius2())
                  .setUv(this.radius.radius3(), this.radius.radius4())
                  .setUv(this.smoothness, 0.0F);
               consumer.addVertexWith2DPose(matrices, x3, y3)
                  .setUv(1.0F, 1.0F)
                  .setColor(color3)
                  .setUv(width, height)
                  .setUv(this.radius.radius1(), this.radius.radius2())
                  .setUv(this.radius.radius3(), this.radius.radius4())
                  .setUv(this.smoothness, 0.0F);
               consumer.addVertexWith2DPose(matrices, x4, y4)
                  .setUv(1.0F, 0.0F)
                  .setColor(color4)
                  .setUv(width, height)
                  .setUv(this.radius.radius1(), this.radius.radius2())
                  .setUv(this.radius.radius3(), this.radius.radius4())
                  .setUv(this.smoothness, 0.0F);
            }
         );
         ((DrawContextAccessor)context).getState().addGuiElement(state);
      }
   }
}
