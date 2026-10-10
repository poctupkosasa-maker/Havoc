package havoc.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import havoc.module.impl.visuals.Ambience;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import java.nio.ByteBuffer;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

public final class AmbienceSkyRenderer {
   private static final int BUFFER_SIZE = 176;
   private static final BindGroupLayout SKY_DATA_LAYOUT = BindGroupLayout.builder()
      .withSampler("DepthSampler")
      .withUniform("AmbienceSkyData", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withBindGroupLayout(BindGroupLayouts.GLOBALS)
      .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
      .withBindGroupLayout(SKY_DATA_LAYOUT)
      .withLocation(Identifier.fromNamespaceAndPath("havoc", "pipeline/ambience_sky"))
      .withVertexShader(Identifier.fromNamespaceAndPath("havoc", "core/ambience_sky"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("havoc", "core/ambience_sky"))
      .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
      .withColorTargetState(new ColorTargetState(BlendFunction.OVERLAY))
      .withCull(false)
      .build();
   private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Vector3f MODEL_OFFSET = new Vector3f();
   private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
   private static GpuBuffer uniformBuffer;
   private static ByteBuffer dataBuffer;
   private static long lastTimestampMs = -1L;
   private static float accumulatedTime;

   private AmbienceSkyRenderer() {
   }

   public static void bootstrap() {
   }

   public static boolean draw(Camera camera, Ambience ambience) {
      Minecraft client = Minecraft.getInstance();
      if (client != null
         && camera != null
         && ambience != null
         && client.gameRenderer.mainRenderTarget() != null
         && client.gameRenderer.mainRenderTarget().getColorTexture() != null) {
         ensureInitialized();
         if (!prepareUniformData(client, camera, ambience)) {
            return false;
         }

         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);
         GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
            .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
         RenderPass renderPass = encoder.createRenderPass(
            () -> "havoc:ambience_sky_pass", client.gameRenderer.mainRenderTarget().getColorTextureView(), Optional.empty()
         );

         try {
            renderPass.setPipeline(PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setUniform("AmbienceSkyData", uniformBuffer);
            renderPass.bindTexture(
               "DepthSampler", client.gameRenderer.mainRenderTarget().getDepthTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)
            );
            renderPass.draw(0, 6, 0, 1);
         } catch (Throwable var9) {
            if (renderPass != null) {
               try {
                  renderPass.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (renderPass != null) {
            renderPass.close();
         }

         return true;
      } else {
         return false;
      }
   }

   private static void ensureInitialized() {
      if (dataBuffer == null) {
         dataBuffer = MemoryUtil.memAlloc(176);
      }
   }

   private static boolean prepareUniformData(Minecraft client, Camera camera, Ambience ambience) {
      long now = System.currentTimeMillis();
      if (lastTimestampMs > 0L) {
         accumulatedTime = accumulatedTime + Math.min(0.1F, Math.max(0.0F, (float)(now - lastTimestampMs) / 1000.0F));
      }

      lastTimestampMs = now;
      Matrix4f inverseViewProjection = RenderUtil3D.getInverseViewProjectionMatrix();
      if (inverseViewProjection == null) {
         return false;
      }

      int first = ClientColors.CATEGORY_ACCENT;
      int second = ClientColors.CATEGORY_ACCENT_SECOND;
      dataBuffer.clear();
      putVec4(accumulatedTime, 0.0F, 0.0F, 0.0F);
      putMatrix(inverseViewProjection);
      putColor(first);
      putColor(second);
      putVec4(ambience.getCustomSkyMode(), ambience.getSkySpeed(), ambience.getSkyScale(), ambience.getSkyOutline());
      putVec4(ambience.getSkyGlow(), ambience.getSkyFill(), ambience.getSkyAlpha(), ambience.isSummerNight() ? 1.0F : 0.0F);
      putVec4(ambience.getTimeFraction(), ambience.hasRays() ? 1.0F : 0.0F, ambience.getRaysIntensity(), ambience.getRaysLength());
      float rain = client.level == null ? 0.0F : client.level.getRainLevel(1.0F);
      putVec4(rain, ambience.getFogStartDistance(), ambience.getFogEndDistance(), 0.0F);
      dataBuffer.flip();
      if (uniformBuffer == null || uniformBuffer.size() < 176L) {
         if (uniformBuffer != null) {
            uniformBuffer.close();
         }

         uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "havoc:ambience_sky_uniform", 136, 176L);
      }

      return true;
   }

   private static void putColor(int color) {
      putVec4(ColorUtil.red(color) / 255.0F, ColorUtil.green(color) / 255.0F, ColorUtil.blue(color) / 255.0F, 1.0F);
   }

   private static void putMatrix(Matrix4f matrix) {
      dataBuffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
      dataBuffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
      dataBuffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
      dataBuffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
   }

   private static void putVec4(float x, float y, float z, float w) {
      dataBuffer.putFloat(x).putFloat(y).putFloat(z).putFloat(w);
   }
}
