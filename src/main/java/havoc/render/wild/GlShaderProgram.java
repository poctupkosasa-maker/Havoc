package havoc.render.wild;

import java.nio.IntBuffer;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

public final class GlShaderProgram {
   private final int id;

   public GlShaderProgram(String vertexSource, String fragmentSource) {
      int vertex = compile(35633, vertexSource);
      int fragment = compile(35632, fragmentSource);
      this.id = GL20.glCreateProgram();
      GL20.glAttachShader(this.id, vertex);
      GL20.glAttachShader(this.id, fragment);
      GL20.glLinkProgram(this.id);
      MemoryStack stack = MemoryStack.stackPush();

      try {
         IntBuffer status = stack.mallocInt(1);
         GL20.glGetProgramiv(this.id, 35714, status);
         if (status.get(0) == 0) {
            String log = GL20.glGetProgramInfoLog(this.id);
            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);
            GL20.glDeleteProgram(this.id);
            throw new IllegalStateException("Program link failed: " + log);
         }
      } catch (Throwable var9) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }
         }

         throw var9;
      }

      if (stack != null) {
         stack.close();
      }

      GL20.glDetachShader(this.id, vertex);
      GL20.glDetachShader(this.id, fragment);
      GL20.glDeleteShader(vertex);
      GL20.glDeleteShader(fragment);
   }

   public static GlShaderProgram resolve(String vertexPath, String fragmentPath) {
      return new GlShaderProgram(ResourceUtils.resolve(vertexPath), ResourceUtils.resolve(fragmentPath));
   }

   private static int compile(int type, String source) {
      int shader = GL20.glCreateShader(type);
      GL20.glShaderSource(shader, source);
      GL20.glCompileShader(shader);
      MemoryStack stack = MemoryStack.stackPush();

      try {
         IntBuffer status = stack.mallocInt(1);
         GL20.glGetShaderiv(shader, 35713, status);
         if (status.get(0) == 0) {
            String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("Shader compile failed: " + log);
         }
      } catch (Throwable var7) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (stack != null) {
         stack.close();
      }

      return shader;
   }

   public void invoke() {
      GL20.glUseProgram(this.id);
   }

   public void invoke2() {
      GL20.glDeleteProgram(this.id);
   }

   public int compute2(String name) {
      return GL20.glGetUniformLocation(this.id, name);
   }
}
