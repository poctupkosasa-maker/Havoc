package havoc.cosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.Identifier;

public final class CosmeticRegistry {
   public static final int TOTAL = 63;
   private static final Entry[] ENTRIES = loadAll();

   private CosmeticRegistry() {
   }

   public static final class Entry {
      public final int index;
      public final String label;
      public final String type;
      public final int position;
      public float scale = 1.0F;
      public float offX;
      public float offY;
      public float offZ;
      public float yaw;
      public float pitch;
      public float roll;
      public volatile GeoModel geometry;
      private volatile boolean loaded;
      private volatile Animation animation;
      public Identifier texture;
      private boolean textureTried;

      Entry(int index, String label, String type, int position) {
         this.index = index;
         this.label = label;
         this.type = type;
         this.position = position;
      }

      public synchronized void ensureLoaded() {
         if (!this.loaded) {
            this.loaded = true;
            try (
               InputStream input = CosmeticRegistry.class.getResourceAsStream("/assets/havoc/cosmetics/models/cosmetic_" + this.index + ".json");
               Reader reader = input == null ? null : new InputStreamReader(input, StandardCharsets.UTF_8);
            ) {
               if (reader != null) {
                  JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                  if (json.has("model")) {
                     JsonArray array = json.getAsJsonObject("model").getAsJsonArray("minecraft:geometry");
                     if (array != null && !array.isEmpty()) {
                        this.geometry = GeoModel.parse(array.get(0).getAsJsonObject());
                     }
                  }

                  this.scale = floatOpt(json, "scale", 1.0F);
                  this.offX = floatOpt(json, "x", 0.0F);
                  this.offY = floatOpt(json, "y", 0.0F);
                  this.offZ = floatOpt(json, "z", 0.0F);
                  this.yaw = floatOpt(json, "yaw", 0.0F);
                  this.pitch = floatOpt(json, "pitch", 0.0F);
                  this.roll = floatOpt(json, "roll", 0.0F);
                  if (json.has("animation")) {
                     this.animation = parseAnimation(json.getAsJsonObject("animation"));
                  }
               }
            } catch (Exception ignored) {
            }
         }
      }

      public Animation animation() {
         if (!this.loaded) {
            this.ensureLoaded();
         }

         return this.animation;
      }

      public Identifier texture(Minecraft mc) {
         if (this.texture != null) {
            return this.texture;
         } else if (this.textureTried) {
            return null;
         } else {
            this.textureTried = true;
            try {
               Identifier location = Identifier.fromNamespaceAndPath("havoc", "textures/cosmetics/cosmetic_" + this.index + ".png");
               if (mc.getResourceManager().getResource(location).isPresent()) {
                  mc.getTextureManager().registerAndLoad(location, new SimpleTexture(location));
                  this.texture = location;
                  return this.texture;
               }
            } catch (Exception ignored) {
            }

            return null;
         }
      }
   }

   public static final class Animation {
      public final Map<String, BoneAnimation> bones = new HashMap<>();
      public float length = 1.0F;
      public boolean loop = true;
   }

   public static final class BoneAnimation {
      final Map<Float, float[]> rotation = new HashMap<>();
      final Map<Float, float[]> position = new HashMap<>();
      final Map<Float, float[]> scale = new HashMap<>();
   }

   public static Entry[] all() {
      return ENTRIES;
   }

   public static Entry get(int index) {
      return index >= 0 && index < TOTAL ? ENTRIES[index] : null;
   }

   public static String label(int index) {
      Entry entry = get(index);
      return entry == null ? "?" : entry.label;
   }

   public static String type(int index) {
      Entry entry = get(index);
      return entry == null ? "" : entry.type;
   }

   public static List<Integer> indicesOfType(String type) {
      List<Integer> out = new ArrayList<>();

      for (Entry entry : ENTRIES) {
         if (entry.type.equals(type)) {
            out.add(entry.index);
         }
      }

      return out;
   }

   private static Entry[] loadAll() {
      Entry[] entries = new Entry[TOTAL];

      for (int i = 0; i < TOTAL; i++) {
         entries[i] = loadQuick(i);
      }

      return entries;
   }

   private static Entry loadQuick(int index) {
      try (
         InputStream input = CosmeticRegistry.class.getResourceAsStream("/assets/havoc/cosmetics/models/cosmetic_" + index + ".json");
      ) {
         if (input == null) {
            return new Entry(index, "Cosmetic " + (index + 1), "wings", 1);
         } else {
            byte[] head = input.readNBytes(2048);
            String raw = new String(head, StandardCharsets.UTF_8);
            String name = between(raw, "\"name\":\"", "\"");
            String type = between(raw, "\"type\":\"", "\"");
            int position = parseInt(between(raw, "\"pos\":", ","), 1);
            String label = prettify(name == null || name.isBlank() ? "Cosmetic " + (index + 1) : name);
            if (type == null || type.isBlank()) {
               type = classify(index, label, position);
            }

            return new Entry(index, label, type, position);
         }
      } catch (Exception ignored) {
         return new Entry(index, "Cosmetic " + (index + 1), "wings", 1);
      }
   }

   private static float floatOpt(JsonObject json, String key, float fallback) {
      return json.has(key) ? json.get(key).getAsFloat() : fallback;
   }

   private static Animation parseAnimation(JsonObject root) {
      Animation animation = new Animation();
      JsonObject animations = root.has("animations") ? root.getAsJsonObject("animations") : root;

      for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
         if (entry.getValue().isJsonObject()) {
            JsonObject value = entry.getValue().getAsJsonObject();
            if (value.has("animation_length")) {
               animation.length = value.get("animation_length").getAsFloat();
            }

            if (value.has("loop")) {
               animation.loop = value.get("loop").getAsBoolean();
            }

            if (value.has("bones")) {
               for (Map.Entry<String, JsonElement> boneEntry : value.getAsJsonObject("bones").entrySet()) {
                  if (boneEntry.getValue().isJsonObject()) {
                     JsonObject boneJson = boneEntry.getValue().getAsJsonObject();
                     BoneAnimation boneAnimation = animation.bones.computeIfAbsent(boneEntry.getKey(), k -> new BoneAnimation());
                     readKeyframes(boneJson, "rotation", boneAnimation.rotation);
                     readKeyframes(boneJson, "position", boneAnimation.position);
                     readKeyframes(boneJson, "scale", boneAnimation.scale);
                  }
               }
            }
         }
      }

      return animation;
   }

   private static void readKeyframes(JsonObject boneJson, String key, Map<Float, float[]> target) {
      if (boneJson.has(key)) {
         JsonElement element = boneJson.get(key);
         if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
               try {
                  float time = Float.parseFloat(entry.getKey());
                  JsonElement value = entry.getValue();
                  JsonArray array = null;
                  if (value.isJsonArray()) {
                     array = value.getAsJsonArray();
                  } else if (value.isJsonObject() && value.getAsJsonObject().has("vector")) {
                     array = value.getAsJsonObject().getAsJsonArray("vector");
                  }

                  if (array != null && array.size() >= 3) {
                     target.put(time, new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()});
                  }
               } catch (Exception ignored) {
               }
            }
         }
      }
   }

   private static String between(String text, String open, String close) {
      int start = text.indexOf(open);
      if (start < 0) {
         return null;
      } else {
         start += open.length();
         int end = text.indexOf(close, start);
         return end < 0 ? null : text.substring(start, end);
      }
   }

   private static int parseInt(String value, int fallback) {
      try {
         return Integer.parseInt(value.trim());
      } catch (Exception ignored) {
         return fallback;
      }
   }

   private static String prettify(String raw) {
      String name = raw == null ? "" : raw;

      for (String prefix : new String[]{"pulse_", "akvi4_", "fluger_", "bro9i_", "wota_"}) {
         if (name.startsWith(prefix)) {
            name = name.substring(prefix.length());
            break;
         }
      }

      name = name.replace('_', ' ').trim();
      return name.isEmpty() ? "Cosmetic" : name;
   }

   private static String classify(int index, String name, int position) {
      String lower = name.toLowerCase();
      if (lower.contains("cape")) {
         return "cape";
      } else if (lower.contains("wing")) {
         return "wings";
      } else if (lower.contains("pet") || lower.contains("bee") || lower.contains("radish")) {
         return "pet";
      } else if (lower.contains("hat") || lower.contains("nimb")) {
         return "hat";
      } else if (index <= 13) {
         return "cape";
      } else if (index <= 26) {
         return "wings";
      } else if (index <= 37) {
         return "bodywear";
      } else if (index <= 49) {
         return "pet";
      } else {
         return position == 2 ? "hat" : "bodywear";
      }
   }
}
