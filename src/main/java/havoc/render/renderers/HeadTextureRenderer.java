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
import org.joml.Matrix3x2fStack;

public record HeadTextureRenderer(
   SizeState size, QuadRadiusState radius, QuadColorState color, float smoothness, float u, float v, float texWidth, float texHeight, GpuTextureView texture
) implements Renderer {
   private static ScreenRectangle createBounds(GuiGraphicsExtractor c, float x, float y, float w, float h) {
      Matrix3x2fStack mat = c.pose();
      return new ScreenRectangle((int)Math.floor(x), (int)Math.floor(y), (int)Math.ceil(w), (int)Math.ceil(h)).transformMaxBounds(mat);
   }

   @Override
   public void render(double x, double y, GuiGraphicsExtractor ctx) {
      if (PipelineRegistry.HEAD_TEXTURE_PIPELINE != null) {
         if (this.texture != null) {
            Matrix3x2f guiMatrices = new Matrix3x2f(ctx.pose());
            TextureSetup textureSetup = TextureSetup.singleTexture(this.texture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            float u0 = 0.0F;
            float v0 = 0.0F;
            float u1 = 1.0F;
            float v1 = 1.0F;
            if (this.texWidth > 0.0F && this.texHeight > 0.0F && this.texture != null) {
               int texW = this.texture.getWidth(0);
               int texH = this.texture.getHeight(0);
               if (texW > 0 && texH > 0) {
                  u0 = this.u / texW;
                  v0 = this.v / texH;
                  u1 = (this.u + this.texWidth) / texW;
                  v1 = (this.v + this.texHeight) / texH;
               }
            }

            float fu0 = u0;
            float fv0 = v0;
            float fu1 = u1;
            float fv1 = v1;
            SimpleGuiRenderState state = new SimpleGuiRenderState(
               PipelineRegistry.HEAD_TEXTURE_PIPELINE,
               textureSetup,
               ctx,
               createBounds(ctx, (float)x, (float)y, this.size.width(), this.size.height()),
               buffer -> {
                  DirectVertexConsumer dvc = new DirectVertexConsumer((BufferBuilder)buffer, false);
                  int c1 = Renderer.toAbgr(this.color.color1());
                  int c2 = Renderer.toAbgr(this.color.color2());
                  int c3 = Renderer.toAbgr(this.color.color3());
                  int c4 = Renderer.toAbgr(this.color.color4());
                  dvc.addVertexWith2DPose(guiMatrices, (float)x, (float)y)
                     .setUv(0.0F, 0.0F)
                     .setColor(c1)
                     .setUv(this.size.width(), this.size.height())
                     .setUv(this.radius.radius1(), this.radius.radius2())
                     .setUv(this.radius.radius3(), this.radius.radius4())
                     .setUv(fu0, fv0)
                     .setUv(this.smoothness, 0.0F);
                  dvc.addVertexWith2DPose(guiMatrices, (float)x, (float)(y + this.size.height()))
                     .setUv(0.0F, 1.0F)
                     .setColor(c2)
                     .setUv(this.size.width(), this.size.height())
                     .setUv(this.radius.radius1(), this.radius.radius2())
                     .setUv(this.radius.radius3(), this.radius.radius4())
                     .setUv(fu0, fv1)
                     .setUv(this.smoothness, 0.0F);
                  dvc.addVertexWith2DPose(guiMatrices, (float)(x + this.size.width()), (float)(y + this.size.height()))
                     .setUv(1.0F, 1.0F)
                     .setColor(c3)
                     .setUv(this.size.width(), this.size.height())
                     .setUv(this.radius.radius1(), this.radius.radius2())
                     .setUv(this.radius.radius3(), this.radius.radius4())
                     .setUv(fu1, fv1)
                     .setUv(this.smoothness, 0.0F);
                  dvc.addVertexWith2DPose(guiMatrices, (float)(x + this.size.width()), (float)y)
                     .setUv(1.0F, 0.0F)
                     .setColor(c4)
                     .setUv(this.size.width(), this.size.height())
                     .setUv(this.radius.radius1(), this.radius.radius2())
                     .setUv(this.radius.radius3(), this.radius.radius4())
                     .setUv(fu1, fv0)
                     .setUv(this.smoothness, 0.0F);
               }
            );
            ((DrawContextAccessor)ctx).getState().addGuiElement(state);
         }
      }
   }
}
