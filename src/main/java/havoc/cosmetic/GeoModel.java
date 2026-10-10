package havoc.cosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GeoModel {
   public final List<Bone> bones = new ArrayList<>();
   public final Map<String, Bone> byName = new HashMap<>();
   public final List<Bone> topLevelBones = new ArrayList<>();
   public final int textureWidth;
   public final int textureHeight;

   private GeoModel(int textureWidth, int textureHeight) {
      this.textureWidth = textureWidth;
      this.textureHeight = textureHeight;
   }

   public static final class Bone {
      public final String name;
      public Bone parent;
      public final List<Bone> childBones = new ArrayList<>();
      public final List<Cube> cubes = new ArrayList<>();
      public float pivotX;
      public float pivotY;
      public float pivotZ;
      public float rotationX;
      public float rotationY;
      public float rotationZ;
      public float posX;
      public float posY;
      public float posZ;
      public float scaleX = 1.0F;
      public float scaleY = 1.0F;
      public float scaleZ = 1.0F;

      Bone(String name) {
         this.name = name;
      }
   }

   public static final class Cube {
      public final Quad[] quads = new Quad[6];
      public float pivotX;
      public float pivotY;
      public float pivotZ;
      public float rotationX;
      public float rotationY;
      public float rotationZ;
   }

   public static final class Quad {
      public final float[] xs = new float[4];
      public final float[] ys = new float[4];
      public final float[] zs = new float[4];
      public final float[] uvs = new float[8];
      public float normalX;
      public float normalY;
      public float normalZ;
   }

   public static GeoModel parse(JsonObject geometryEntry) {
      JsonObject description = geometryEntry.getAsJsonObject("description");
      int texW = description.has("texture_width") ? description.get("texture_width").getAsInt() : 64;
      int texH = description.has("texture_height") ? description.get("texture_height").getAsInt() : 64;
      GeoModel model = new GeoModel(texW, texH);
      JsonArray bonesJson = geometryEntry.getAsJsonArray("bones");
      if (bonesJson == null) {
         return model;
      } else {
         List<JsonObject> raw = new ArrayList<>();

         for (JsonElement element : bonesJson) {
            JsonObject object = element.getAsJsonObject();
            Bone bone = parseBone(object, model);
            if (bone != null) {
               raw.add(object);
               model.bones.add(bone);
               model.byName.put(bone.name, bone);
            }
         }

         for (int i = 0; i < raw.size(); i++) {
            JsonObject object = raw.get(i);
            Bone bone = model.bones.get(i);
            Bone parent = object.has("parent") ? model.byName.get(object.get("parent").getAsString()) : null;
            if (parent != null) {
               bone.parent = parent;
               parent.childBones.add(bone);
            } else {
               model.topLevelBones.add(bone);
            }
         }

         return model;
      }
   }

   private static Bone parseBone(JsonObject json, GeoModel model) {
      if (!json.has("name")) {
         return null;
      } else {
         Bone bone = new Bone(json.get("name").getAsString());
         if (json.has("pivot")) {
            JsonArray pivot = json.getAsJsonArray("pivot");
            bone.pivotX = -pivot.get(0).getAsFloat();
            bone.pivotY = pivot.get(1).getAsFloat();
            bone.pivotZ = pivot.get(2).getAsFloat();
         }

         if (json.has("rotation")) {
            JsonArray rotation = json.getAsJsonArray("rotation");
            bone.rotationX = (float)Math.toRadians(-rotation.get(0).getAsFloat());
            bone.rotationY = (float)Math.toRadians(-rotation.get(1).getAsFloat());
            bone.rotationZ = (float)Math.toRadians(rotation.get(2).getAsFloat());
         }

         if (json.has("cubes")) {
            for (JsonElement element : json.getAsJsonArray("cubes")) {
               Cube cube = parseCube(element.getAsJsonObject(), model);
               if (cube != null) {
                  bone.cubes.add(cube);
               }
            }
         }

         return bone;
      }
   }

   private static Cube parseCube(JsonObject json, GeoModel model) {
      float[] origin = arr(json, "origin", new float[]{0.0F, 0.0F, 0.0F});
      float[] size = arr(json, "size", new float[]{1.0F, 1.0F, 1.0F});
      if (size.length < 3 || origin.length < 3) {
         return null;
      } else {
         float[] pivot = arr(json, "pivot", origin);
         float[] rotation = arr(json, "rotation", new float[]{0.0F, 0.0F, 0.0F});
         float inflate = json.has("inflate") ? json.get("inflate").getAsFloat() : 0.0F;
         float ox = (origin[0] - inflate) / 16.0F;
         float oy = (origin[1] - inflate) / 16.0F;
         float oz = (origin[2] - inflate) / 16.0F;
         float sx = (size[0] + inflate * 2.0F) / 16.0F;
         float sy = (size[1] + inflate * 2.0F) / 16.0F;
         float sz = (size[2] + inflate * 2.0F) / 16.0F;
         Cube cube = new Cube();
         cube.pivotX = -pivot[0] / 16.0F;
         cube.pivotY = pivot[1] / 16.0F;
         cube.pivotZ = pivot[2] / 16.0F;
         if (rotation.length >= 3) {
            cube.rotationX = (float)Math.toRadians(-rotation[0]);
            cube.rotationY = (float)Math.toRadians(-rotation[1]);
            cube.rotationZ = (float)Math.toRadians(rotation[2]);
         }

         float x1 = -(ox + sx);
         float x2 = -ox;
         float y1 = oy;
         float y2 = oy + sy;
         float z1 = oz;
         float z2 = oz + sz;
         float uw = sx * 16.0F;
         float vh = sy * 16.0F;
         float dw = sz * 16.0F;
         JsonElement uvElement = json.get("uv");
         if (uvElement != null && uvElement.isJsonArray() && uvElement.getAsJsonArray().size() >= 2) {
            JsonArray uv = uvElement.getAsJsonArray();
            float u = uv.get(0).getAsFloat();
            float v = uv.get(1).getAsFloat();
            face(cube, 0, new float[]{x1, x1, x1, x1}, new float[]{y2, y2, y1, y1}, new float[]{z1, z2, z2, z1}, u, v, dw, vh, -1.0F, 0.0F, 0.0F, model);
            face(cube, 1, new float[]{x2, x2, x2, x2}, new float[]{y2, y2, y1, y1}, new float[]{z2, z1, z1, z2}, u + dw + uw, v, dw, vh, 1.0F, 0.0F, 0.0F, model);
            face(cube, 2, new float[]{x1, x2, x2, x1}, new float[]{y1, y1, y1, y1}, new float[]{z1, z1, z2, z2}, u + dw, v + vh, uw, dw, 0.0F, -1.0F, 0.0F, model);
            face(cube, 3, new float[]{x1, x2, x2, x1}, new float[]{y2, y2, y2, y2}, new float[]{z2, z2, z1, z1}, u + dw + uw, v + vh, uw, dw, 0.0F, 1.0F, 0.0F, model);
            face(cube, 4, new float[]{x1, x2, x2, x1}, new float[]{y1, y1, y2, y2}, new float[]{z1, z1, z1, z1}, u + dw, v, uw, vh, 0.0F, 0.0F, -1.0F, model);
            face(
               cube, 5, new float[]{x2, x1, x1, x2}, new float[]{y1, y1, y2, y2}, new float[]{z2, z2, z2, z2}, u + dw * 2.0F + uw, v, uw, vh, 0.0F, 0.0F, 1.0F, model
            );
         } else if (uvElement != null && uvElement.isJsonObject()) {
            JsonObject perFace = uvElement.getAsJsonObject();
            boxFace(cube, perFace, 0, "west", new float[]{x1, x1, x1, x1}, new float[]{y2, y2, y1, y1}, new float[]{z1, z2, z2, z1}, -1.0F, 0.0F, 0.0F, model);
            boxFace(cube, perFace, 1, "east", new float[]{x2, x2, x2, x2}, new float[]{y2, y2, y1, y1}, new float[]{z2, z1, z1, z2}, 1.0F, 0.0F, 0.0F, model);
            boxFace(cube, perFace, 2, "down", new float[]{x1, x2, x2, x1}, new float[]{y1, y1, y1, y1}, new float[]{z1, z1, z2, z2}, 0.0F, -1.0F, 0.0F, model);
            boxFace(cube, perFace, 3, "up", new float[]{x1, x2, x2, x1}, new float[]{y2, y2, y2, y2}, new float[]{z2, z2, z1, z1}, 0.0F, 1.0F, 0.0F, model);
            boxFace(cube, perFace, 4, "north", new float[]{x1, x2, x2, x1}, new float[]{y1, y1, y2, y2}, new float[]{z1, z1, z1, z1}, 0.0F, 0.0F, -1.0F, model);
            boxFace(cube, perFace, 5, "south", new float[]{x2, x1, x1, x2}, new float[]{y1, y1, y2, y2}, new float[]{z2, z2, z2, z2}, 0.0F, 0.0F, 1.0F, model);
         }

         return cube;
      }
   }

   private static void boxFace(
      Cube cube,
      JsonObject perFace,
      int slot,
      String name,
      float[] xs,
      float[] ys,
      float[] zs,
      float nx,
      float ny,
      float nz,
      GeoModel model
   ) {
      if (perFace.has(name)) {
         JsonObject faceJson = perFace.getAsJsonObject(name);
         if (faceJson.has("uv")) {
            JsonArray uv = faceJson.getAsJsonArray("uv");
            float u0 = uv.get(0).getAsFloat();
            float v0 = uv.get(1).getAsFloat();
            float u1 = u0;
            float v1 = v0;
            if (faceJson.has("uv_size")) {
               JsonArray uvSize = faceJson.getAsJsonArray("uv_size");
               u1 = u0 + uvSize.get(0).getAsFloat();
               v1 = v0 + uvSize.get(1).getAsFloat();
            }

            face(cube, slot, xs, ys, zs, Math.min(u0, u1), Math.min(v0, v1), Math.abs(u1 - u0), Math.abs(v1 - v0), nx, ny, nz, model);
         }
      }
   }

   private static void face(
      Cube cube,
      int slot,
      float[] xs,
      float[] ys,
      float[] zs,
      float u,
      float v,
      float w,
      float h,
      float nx,
      float ny,
      float nz,
      GeoModel model
   ) {
      Quad quad = new Quad();
      quad.xs[0] = xs[0];
      quad.xs[1] = xs[1];
      quad.xs[2] = xs[2];
      quad.xs[3] = xs[3];
      quad.ys[0] = ys[0];
      quad.ys[1] = ys[1];
      quad.ys[2] = ys[2];
      quad.ys[3] = ys[3];
      quad.zs[0] = zs[0];
      quad.zs[1] = zs[1];
      quad.zs[2] = zs[2];
      quad.zs[3] = zs[3];
      float fu = u / model.textureWidth;
      float fv = v / model.textureHeight;
      float fw = w / model.textureWidth;
      float fh = h / model.textureHeight;
      quad.uvs[0] = fu;
      quad.uvs[1] = fv;
      quad.uvs[2] = fu + fw;
      quad.uvs[3] = fv;
      quad.uvs[4] = fu + fw;
      quad.uvs[5] = fv + fh;
      quad.uvs[6] = fu;
      quad.uvs[7] = fv + fh;
      quad.normalX = nx;
      quad.normalY = ny;
      quad.normalZ = nz;
      cube.quads[slot] = quad;
   }

   private static float[] arr(JsonObject json, String key, float[] fallback) {
      if (!json.has(key)) {
         return fallback;
      } else {
         JsonArray array = json.getAsJsonArray(key);
         if (array.size() < 3) {
            return fallback;
         } else {
            return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
         }
      }
   }
}
