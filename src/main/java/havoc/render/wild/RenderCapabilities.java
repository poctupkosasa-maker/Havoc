package havoc.render.wild;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

public final class RenderCapabilities {
   private RenderCapabilities() {
   }

   public static void invoke(int internalFormat, int width, int height, int format, int type) {
      int unpackBuffer = GL11.glGetInteger(35055);
      if (unpackBuffer != 0) {
         GL15.glBindBuffer(35052, 0);
      }

      try {
         GL11.glTexImage2D(3553, 0, internalFormat, width, height, 0, format, type, (ByteBuffer)null);
      } finally {
         if (unpackBuffer != 0) {
            GL15.glBindBuffer(35052, unpackBuffer);
         }
      }
   }
}
