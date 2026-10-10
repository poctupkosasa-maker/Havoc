package havoc.render.wild;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

public final class FramebufferUtils {
   private static final int TEXTURE_UNITS = 32;

   private FramebufferUtils() {
   }

   public static FramebufferUtils.GlStateSnapshot captureGlState() {
      FramebufferUtils.GlStateSnapshot snapshot = new FramebufferUtils.GlStateSnapshot();
      MemoryStack stack = MemoryStack.stackPush();

      try {
         IntBuffer ints = stack.mallocInt(16);
         ByteBuffer bytes = stack.malloc(4);
         GL11.glGetIntegerv(36006, ints);
         snapshot.drawFramebuffer = ints.get(0);
         GL11.glGetIntegerv(36010, ints);
         snapshot.readFramebuffer = ints.get(0);
         GL11.glGetIntegerv(3073, ints);
         snapshot.drawBuffer = ints.get(0);
         GL11.glGetIntegerv(3074, ints);
         snapshot.readBuffer = ints.get(0);
         GL11.glGetIntegerv(35725, ints);
         snapshot.program = ints.get(0);
         GL11.glGetIntegerv(34229, ints);
         snapshot.vertexArray = ints.get(0);
         GL11.glGetIntegerv(34964, ints);
         snapshot.arrayBuffer = ints.get(0);
         GL11.glGetIntegerv(34965, ints);
         snapshot.elementArrayBuffer = ints.get(0);
         GL11.glGetIntegerv(34016, ints);
         snapshot.activeTexture = ints.get(0) == 0 ? 33984 : ints.get(0);

         for (int i = 0; i < 32; i++) {
            GL13.glActiveTexture(33984 + i);
            GL11.glGetIntegerv(32873, ints);
            snapshot.textures[i] = ints.get(0);
         }

         GL13.glActiveTexture(snapshot.activeTexture);
         GL11.glGetIntegerv(3317, ints);
         snapshot.unpackAlignment = ints.get(0) == 0 ? 4 : ints.get(0);
         GL11.glGetIntegerv(2978, ints);
         snapshot.viewport[0] = ints.get(0);
         snapshot.viewport[1] = ints.get(1);
         snapshot.viewport[2] = ints.get(2);
         snapshot.viewport[3] = ints.get(3);
         GL11.glGetIntegerv(3088, ints);
         snapshot.scissor[0] = ints.get(0);
         snapshot.scissor[1] = ints.get(1);
         snapshot.scissor[2] = ints.get(2);
         snapshot.scissor[3] = ints.get(3);
         GL11.glGetIntegerv(32969, ints);
         snapshot.blendSrcRgb = ints.get(0);
         GL11.glGetIntegerv(32968, ints);
         snapshot.blendDstRgb = ints.get(0);
         GL11.glGetIntegerv(32971, ints);
         snapshot.blendSrcAlpha = ints.get(0);
         GL11.glGetIntegerv(32970, ints);
         snapshot.blendDstAlpha = ints.get(0);
         GL11.glGetBooleanv(3107, bytes);
         snapshot.colorMaskR = bytes.get(0) != 0;
         snapshot.colorMaskG = bytes.get(1) != 0;
         snapshot.colorMaskB = bytes.get(2) != 0;
         snapshot.colorMaskA = bytes.get(3) != 0;
         GL11.glGetBooleanv(2930, bytes);
         snapshot.depthMask = bytes.get(0) != 0;
         snapshot.blend = GL11.glIsEnabled(3042);
         snapshot.depth = GL11.glIsEnabled(2929);
         snapshot.cull = GL11.glIsEnabled(2884);
         snapshot.scissorEnabled = GL11.glIsEnabled(3089);
         snapshot.srgb = GL11.glIsEnabled(36281);
      } catch (Throwable var6) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (stack != null) {
         stack.close();
      }

      return snapshot;
   }

   public static void restoreGlState(FramebufferUtils.GlStateSnapshot snapshot) {
      if (snapshot != null) {
         GL30.glBindFramebuffer(36009, compute(snapshot.drawFramebuffer));
         GL30.glBindFramebuffer(36008, compute(snapshot.readFramebuffer));
         GL11.glDrawBuffer(computeDrawBuffer(snapshot.drawFramebuffer, snapshot.drawBuffer));
         GL11.glReadBuffer(computeDrawBuffer(snapshot.readFramebuffer, snapshot.readBuffer));
         GL20.glUseProgram(snapshot.program);
         GL30.glBindVertexArray(snapshot.vertexArray);
         GL15.glBindBuffer(34962, snapshot.arrayBuffer);
         GL15.glBindBuffer(34963, snapshot.elementArrayBuffer);

         for (int i = 0; i < 32; i++) {
            GL13.glActiveTexture(33984 + i);
            GL11.glBindTexture(3553, snapshot.textures[i]);
         }

         GL13.glActiveTexture(snapshot.activeTexture);
         GL11.glPixelStorei(3317, snapshot.unpackAlignment);
         setEnabled(3042, snapshot.blend);
         setEnabled(2929, snapshot.depth);
         setEnabled(2884, snapshot.cull);
         setEnabled(3089, snapshot.scissorEnabled);
         setEnabled(36281, snapshot.srgb);
         GL14.glBlendFuncSeparate(snapshot.blendSrcRgb, snapshot.blendDstRgb, snapshot.blendSrcAlpha, snapshot.blendDstAlpha);
         GL11.glColorMask(snapshot.colorMaskR, snapshot.colorMaskG, snapshot.colorMaskB, snapshot.colorMaskA);
         GL11.glDepthMask(snapshot.depthMask);
         GL11.glViewport(snapshot.viewport[0], snapshot.viewport[1], snapshot.viewport[2], snapshot.viewport[3]);
         if (snapshot.scissor[2] > 0 && snapshot.scissor[3] > 0) {
            GL11.glScissor(snapshot.scissor[0], snapshot.scissor[1], snapshot.scissor[2], snapshot.scissor[3]);
         }
      }
   }

   public static int compute(int framebuffer) {
      if (framebuffer <= 0) {
         return 0;
      }

      try {
         return GL30.glIsFramebuffer(framebuffer) ? framebuffer : 0;
      } catch (Throwable ignored) {
         return 0;
      }
   }

   private static int computeDrawBuffer(int framebuffer, int buffer) {
      return framebuffer == 0 && buffer != 1026 && buffer != 1027 && buffer != 1029 ? 1029 : buffer;
   }

   private static void setEnabled(int capability, boolean enabled) {
      if (enabled) {
         GL11.glEnable(capability);
      } else {
         GL11.glDisable(capability);
      }
   }

   public static final class GlStateSnapshot {
      int drawFramebuffer;
      int readFramebuffer;
      int drawBuffer;
      int readBuffer;
      int program;
      int vertexArray;
      int arrayBuffer;
      int elementArrayBuffer;
      int activeTexture;
      int unpackAlignment;
      int blendSrcRgb;
      int blendDstRgb;
      int blendSrcAlpha;
      int blendDstAlpha;
      final int[] viewport = new int[4];
      final int[] scissor = new int[4];
      final int[] textures = new int[32];
      boolean blend;
      boolean depth;
      boolean cull;
      boolean scissorEnabled;
      boolean srgb;
      boolean colorMaskR;
      boolean colorMaskG;
      boolean colorMaskB;
      boolean colorMaskA;
      boolean depthMask;
   }
}
