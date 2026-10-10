package havoc.render.wild;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.lwjgl.BufferUtils;

public final class ResourceUtils {
   private ResourceUtils() {
   }

   public static String resolve(String path) {
      ClassLoader classLoader = ResourceUtils.class.getClassLoader();
      String normalized = normalize(path);

      try (InputStream inputStream = classLoader.getResourceAsStream(normalized)) {
         if (inputStream == null) {
            throw new IllegalStateException("Resource not found: " + path);
         }

         try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
               builder.append(line).append('\n');
            }

            return builder.toString();
         }
      } catch (IOException exception) {
         throw new RuntimeException("Failed to read resource: " + path, exception);
      }
   }

   public static ByteBuffer resolve2(String path) {
      ClassLoader classLoader = ResourceUtils.class.getClassLoader();
      String normalized = normalize(path);

      try (InputStream inputStream = classLoader.getResourceAsStream(normalized)) {
         if (inputStream == null) {
            throw new IllegalStateException("Resource not found: " + path);
         }

         byte[] bytes = inputStream.readAllBytes();
         ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
         buffer.put(bytes).flip();
         return buffer;
      } catch (IOException exception) {
         throw new RuntimeException("Failed to read resource: " + path, exception);
      }
   }

   private static String normalize(String path) {
      if (path == null) {
         throw new IllegalArgumentException("path");
      } else {
         return path.startsWith("/") ? path.substring(1) : path;
      }
   }
}
