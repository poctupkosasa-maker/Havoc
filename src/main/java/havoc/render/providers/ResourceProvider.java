package havoc.render.providers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ResourceProvider {
   private static final ResourceManager RESOURCE_MANAGER = Minecraft.getInstance().getResourceManager();
   private static final Gson GSON = new Gson();

   public static JsonObject toJson(Identifier identifier) {
      return JsonParser.parseString(toString(identifier)).getAsJsonObject();
   }

   public static <T> T fromJsonToInstance(Identifier identifier, Class<T> clazz) {
      return (T)GSON.fromJson(toString(identifier), clazz);
   }

   public static String toString(Identifier identifier) {
      return toString(identifier, "\n");
   }

   public static String toString(Identifier identifier, String delimiter) {
      try (
         InputStream inputStream = RESOURCE_MANAGER.open(identifier);
         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      ) {
         return reader.lines().collect(Collectors.joining(delimiter));
      } catch (IOException ex) {
         throw new RuntimeException(ex);
      }
   }
}
