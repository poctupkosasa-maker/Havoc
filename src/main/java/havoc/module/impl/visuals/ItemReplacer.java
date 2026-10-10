package havoc.module.impl.visuals;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SelectSetting;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@ModuleInfo(name = "ItemReplacer", desc = "Заменяет модели мечей", category = Category.VISUALS)
public final class ItemReplacer extends Module {
   private static final ItemReplacer.SwordVariant[] SWORD_VARIANTS = createSwordVariants();
   private static final Map<String, ItemReplacer.SwordVariant> VARIANTS_BY_LABEL = createVariantMap();
   private static final ItemReplacer.ReplacementSpecialRenderer SPECIAL_RENDERER = new ItemReplacer.ReplacementSpecialRenderer();
   private static final Vector3fc[] MODEL_EXTENTS = new Vector3fc[]{
      new Vector3f(0.0F, 0.0F, 0.0F),
      new Vector3f(0.0F, 0.0F, 1.0F),
      new Vector3f(0.0F, 1.0F, 0.0F),
      new Vector3f(0.0F, 1.0F, 1.0F),
      new Vector3f(1.0F, 0.0F, 0.0F),
      new Vector3f(1.0F, 0.0F, 1.0F),
      new Vector3f(1.0F, 1.0F, 0.0F),
      new Vector3f(1.0F, 1.0F, 1.0F)
   };
   private static ItemReplacer instance;
   private static final ThreadLocal<String> PREVIEW_VARIANT = new ThreadLocal<>();
   private final SelectSetting swordModel = this.registerSetting(new SelectSetting("Модель меча", SWORD_VARIANTS[0].label, List.of(getVariantLabels())));
   private String cachedSelectedLabel;
   private ItemReplacer.SwordVariant cachedSelectedVariant = SWORD_VARIANTS[0];

   public ItemReplacer() {
      instance = this;
   }

   public static void prepareReplacement(ItemStackRenderState renderState, ItemStack stack, ItemDisplayContext displayContext) {
      if (renderState != null && isSword(stack)) {
         ItemReplacer replacer = instance;
         String previewLabel = PREVIEW_VARIANT.get();
         if (replacer != null && (replacer.isEnable() || previewLabel != null)) {
            ItemReplacer.SwordVariant variant = previewLabel == null ? replacer.getSelectedVariant() : replacer.findVariant(previewLabel);
            ItemReplacer.RuntimeSwordModel model = variant.model;
            if (model.loadIfNeeded()) {
               renderState.clear();
               LayerRenderState layer = renderState.newLayer();
               layer.setExtents(() -> MODEL_EXTENTS);
               layer.setupSpecialModel(SPECIAL_RENDERER, new ItemReplacer.ReplacementRenderData(model, displayContext));
               renderState.appendModelIdentityElement(model);
               if (stack.hasFoil()) {
                  layer.setFoilType(FoilType.STANDARD);
                  renderState.setAnimated();
               }
            }
         }
      }
   }

   public static ItemReplacer getInstance() {
      return instance;
   }

   private static boolean isSword(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.is(ItemTags.SWORDS);
   }

   public ItemReplacer.SwordVariant getSelectedVariant() {
      String selected = this.swordModel.getValue();
      if (selected == null) {
         return SWORD_VARIANTS[0];
      }

      if (selected.equalsIgnoreCase(this.cachedSelectedLabel)) {
         return this.cachedSelectedVariant;
      }

      ItemReplacer.SwordVariant variant = VARIANTS_BY_LABEL.get(selected.toLowerCase(Locale.ROOT));
      if (variant == null) {
         variant = SWORD_VARIANTS[0];
      }

      this.cachedSelectedLabel = selected;
      this.cachedSelectedVariant = variant;
      return variant;
   }

   private ItemReplacer.SwordVariant findVariant(String label) {
      return label == null ? SWORD_VARIANTS[0] : VARIANTS_BY_LABEL.getOrDefault(label.toLowerCase(Locale.ROOT), SWORD_VARIANTS[0]);
   }

   public String getSelectedVariantLabel() {
      return this.getSelectedVariant().label;
   }

   public void selectVariant(String label) {
      ItemReplacer.SwordVariant variant = this.findVariant(label);
      this.swordModel.setValue(variant.label);
      this.cachedSelectedLabel = variant.label;
      this.cachedSelectedVariant = variant;
   }

   public static void beginPreview(String label) {
      PREVIEW_VARIANT.set(label);
   }

   public static void endPreview() {
      PREVIEW_VARIANT.remove();
   }

   @Override
   public void onDisable() {
      this.clearLoadedModels();
      super.onDisable();
   }

   public void clearLoadedModels() {
      for (ItemReplacer.SwordVariant variant : SWORD_VARIANTS) {
         variant.model.clear();
      }
   }

   public static String[] getVariantLabels() {
      String[] labels = new String[SWORD_VARIANTS.length];

      for (int i = 0; i < SWORD_VARIANTS.length; i++) {
         labels[i] = SWORD_VARIANTS[i].label;
      }

      return labels;
   }

   private static Map<String, ItemReplacer.SwordVariant> createVariantMap() {
      Map<String, ItemReplacer.SwordVariant> variants = new HashMap<>(SWORD_VARIANTS.length * 2);

      for (ItemReplacer.SwordVariant variant : SWORD_VARIANTS) {
         variants.put(variant.label.toLowerCase(Locale.ROOT), variant);
      }

      return variants;
   }

   private static ItemReplacer.SwordVariant[] createSwordVariants() {
      String[][] data = new String[][]{
         {"Abominable Blade", "abominableblade"},
         {"Amethyst Shuriken", "amethyst_shuriken"},
         {"Ancient Royal Greatsword", "ancient_royal_great_sword"},
         {"Blood Edge", "bloodedge"},
         {"Crescent Rose", "crescentrose"},
         {"Cyber Katana", "cyberkatana"},
         {"Dark Cleaver", "dark_cleaver"},
         {"Havoc Reaper", "havoc_reaper"},
         {"Frost Scythe", "frostscythe"},
         {"Rivers of Blood", "riversofblood"}
      };
      ItemReplacer.SwordVariant[] variants = new ItemReplacer.SwordVariant[data.length];

      for (int i = 0; i < data.length; i++) {
         variants[i] = new ItemReplacer.SwordVariant(data[i][0], data[i][1]);
      }

      return variants;
   }

   private static final class DisplayTransform {
      private final double translationX;
      private final double mirroredTranslationX;
      private final double translationY;
      private final double translationZ;
      private final Quaternionf rotation;
      private final Quaternionf mirroredRotation;
      private final float scaleX;
      private final float scaleY;
      private final float scaleZ;
      private final boolean hasTranslation;
      private final boolean hasRotation;
      private final boolean hasScale;

      private DisplayTransform(float[] rotation, float[] translation, float[] scale) {
         this.translationX = translation[0] / 16.0F;
         this.mirroredTranslationX = -this.translationX;
         this.translationY = translation[1] / 16.0F;
         this.translationZ = translation[2] / 16.0F;
         this.rotation = new Quaternionf()
            .rotationXYZ((float)Math.toRadians(rotation[0]), (float)Math.toRadians(rotation[1]), (float)Math.toRadians(rotation[2]));
         this.mirroredRotation = new Quaternionf()
            .rotationXYZ((float)Math.toRadians(rotation[0]), (float)Math.toRadians(-rotation[1]), (float)Math.toRadians(-rotation[2]));
         this.scaleX = scale[0];
         this.scaleY = scale[1];
         this.scaleZ = scale[2];
         this.hasTranslation = translation[0] != 0.0F || translation[1] != 0.0F || translation[2] != 0.0F;
         this.hasRotation = rotation[0] != 0.0F || rotation[1] != 0.0F || rotation[2] != 0.0F;
         this.hasScale = scale[0] != 1.0F || scale[1] != 1.0F || scale[2] != 1.0F;
      }

      private void apply(PoseStack matrixStack, boolean leftHand) {
         if (this.hasTranslation) {
            matrixStack.translate(leftHand ? this.mirroredTranslationX : this.translationX, this.translationY, this.translationZ);
         }

         if (this.hasRotation) {
            matrixStack.mulPose(leftHand ? this.mirroredRotation : this.rotation);
         }

         if (this.hasScale) {
            matrixStack.scale(this.scaleX, this.scaleY, this.scaleZ);
         }
      }
   }

   private static final class FaceUv {
      private final float u1;
      private final float v1;
      private final float u2;
      private final float v2;

      private FaceUv(float u1, float v1, float u2, float v2) {
         this.u1 = u1;
         this.v1 = v1;
         this.u2 = u2;
         this.v2 = v2;
      }
   }

   private static final class ModelCube {
      private static final int NO_ROTATION = 0;
      private static final int ROTATE_X = 1;
      private static final int ROTATE_Y = 2;
      private static final int ROTATE_Z = 3;
      private final float x1;
      private final float y1;
      private final float z1;
      private final float x2;
      private final float y2;
      private final float z2;
      private ItemReplacer.FaceUv north;
      private ItemReplacer.FaceUv south;
      private ItemReplacer.FaceUv east;
      private ItemReplacer.FaceUv west;
      private ItemReplacer.FaceUv up;
      private ItemReplacer.FaceUv down;
      private float rotationOriginX;
      private float rotationOriginY;
      private float rotationOriginZ;
      private int rotationAxis = 0;
      private float rotationSin;
      private float rotationCos = 1.0F;

      private ModelCube(float x1, float y1, float z1, float x2, float y2, float z2) {
         this.x1 = x1;
         this.y1 = y1;
         this.z1 = z1;
         this.x2 = x2;
         this.y2 = y2;
         this.z2 = z2;
      }

      private int countFaces() {
         int faces = 0;
         if (this.north != null) {
            faces++;
         }

         if (this.south != null) {
            faces++;
         }

         if (this.east != null) {
            faces++;
         }

         if (this.west != null) {
            faces++;
         }

         if (this.up != null) {
            faces++;
         }

         if (this.down != null) {
            faces++;
         }

         return faces;
      }

      private void setRotation(String axis, float angle) {
         switch (axis) {
            case "x":
               this.rotationAxis = 1;
               break;
            case "y":
               this.rotationAxis = 2;
               break;
            case "z":
               this.rotationAxis = 3;
               break;
            default:
               this.rotationAxis = 0;
               return;
         }

         float radians = (float)Math.toRadians(angle);
         this.rotationSin = (float)Math.sin(radians);
         this.rotationCos = (float)Math.cos(radians);
      }
   }

   private record ReplacementRenderData(ItemReplacer.RuntimeSwordModel model, ItemDisplayContext displayContext) {
   }

   private static final class ReplacementSpecialRenderer implements SpecialModelRenderer<ItemReplacer.ReplacementRenderData> {
      public void submit(
         ItemReplacer.ReplacementRenderData data, PoseStack matrices, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int seed
      ) {
         if (data != null && data.model != null && data.model.isReady()) {
            matrices.pushPose();
            matrices.translate(0.5F, 0.5F, 0.5F);
            data.model.applyDisplayTransform(matrices, data.displayContext);
            collector.order(0).submitCustomGeometry(matrices, data.model.renderType, (pose, consumer) -> data.model.drawMesh(consumer, pose, light, overlay));
            if (hasFoil) {
               collector.order(1)
                  .submitCustomGeometry(matrices, RenderTypes.entityGlint(), (pose, consumer) -> data.model.drawMesh(consumer, pose, light, overlay));
            }

            matrices.popPose();
         }
      }

      public void getExtents(Consumer<Vector3fc> output) {
         for (Vector3fc point : ItemReplacer.MODEL_EXTENTS) {
            output.accept(point);
         }
      }

      public ItemReplacer.ReplacementRenderData extractArgument(ItemStack stack) {
         ItemReplacer replacer = ItemReplacer.instance;
         if (replacer != null && replacer.isEnable() && ItemReplacer.isSword(stack)) {
            ItemReplacer.RuntimeSwordModel model = replacer.getSelectedVariant().model;
            return model.loadIfNeeded() ? new ItemReplacer.ReplacementRenderData(model, ItemDisplayContext.NONE) : null;
         } else {
            return null;
         }
      }
   }

   private static final class RuntimeSwordModel {
      private static final String MODEL_ROOT = "item_replacer/sword/models/";
      private static final String TEXTURE_ROOT = "item_replacer/sword/textures/";
      private static final float PIXEL = 0.0625F;
      private static final float MODEL_CENTER = 0.5F;
      private static final int VERTEX_STRIDE = 8;
      private final Minecraft mc = Minecraft.getInstance();
      private final String modelPath;
      private final ArrayList<ItemReplacer.ModelCube> cubes = new ArrayList<>();
      private final ItemReplacer.DisplayTransform[] displayTransforms = new ItemReplacer.DisplayTransform[8];
      private Identifier textureLocation;
      private RenderType renderType;
      private float[] bakedMesh = new float[0];
      private int vertexCount;
      private boolean loaded;

      private RuntimeSwordModel(String modelName) {
         this.modelPath = "item_replacer/sword/models/" + modelName + ".json";
      }

      private void clear() {
         this.loaded = false;
         this.textureLocation = null;
         this.renderType = null;
         this.bakedMesh = new float[0];
         this.vertexCount = 0;
         this.cubes.clear();

         for (int i = 0; i < this.displayTransforms.length; i++) {
            this.displayTransforms[i] = null;
         }
      }

      private boolean isReady() {
         return this.loaded && this.vertexCount > 0 && this.renderType != null;
      }

      private boolean loadIfNeeded() {
         if (this.loaded) {
            return this.isReady();
         }

         this.loaded = true;
         Identifier modelLocation = Identifier.fromNamespaceAndPath("havoc", this.modelPath);

         try (Reader reader = this.mc.getResourceManager().openAsReader(modelLocation)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            this.readModel(root);
         } catch (Exception exception) {
            System.err.println("[ItemReplacer] Failed to load model " + this.modelPath + ": " + exception.getMessage());
         }

         return this.isReady();
      }

      private void readModel(JsonObject root) {
         JsonObject texturesObject = root.getAsJsonObject("textures");
         if (texturesObject != null) {
            Map<String, String> textureMap = new HashMap<>();

            for (Entry<String, JsonElement> entry : texturesObject.entrySet()) {
               textureMap.put(entry.getKey(), entry.getValue().getAsString());
            }

            String primaryTextureKey = this.resolvePrimaryTextureKey(textureMap, root);
            String primaryTexturePath = textureMap.get(primaryTextureKey);
            if (primaryTexturePath != null) {
               String textureName = primaryTexturePath.contains("/")
                  ? primaryTexturePath.substring(primaryTexturePath.lastIndexOf(47) + 1)
                  : primaryTexturePath;
               this.textureLocation = Identifier.fromNamespaceAndPath("havoc", "item_replacer/sword/textures/" + textureName.toLowerCase(Locale.ROOT) + ".png");
               this.renderType = this.hasPartialAlpha(this.textureLocation)
                  ? RenderTypes.entityTranslucent(this.textureLocation)
                  : RenderTypes.entityCutout(this.textureLocation);
               JsonArray elements = root.getAsJsonArray("elements");
               if (elements != null) {
                  String primaryTextureReference = "#" + primaryTextureKey;

                  for (JsonElement element : elements) {
                     JsonObject object = element.getAsJsonObject();
                     JsonArray from = object.getAsJsonArray("from");
                     JsonArray to = object.getAsJsonArray("to");
                     if (from != null && to != null) {
                        ItemReplacer.ModelCube cube = new ItemReplacer.ModelCube(
                           from.get(0).getAsFloat() * 0.0625F,
                           from.get(1).getAsFloat() * 0.0625F,
                           from.get(2).getAsFloat() * 0.0625F,
                           to.get(0).getAsFloat() * 0.0625F,
                           to.get(1).getAsFloat() * 0.0625F,
                           to.get(2).getAsFloat() * 0.0625F
                        );
                        JsonObject faces = object.getAsJsonObject("faces");
                        if (faces != null && this.readFaces(cube, faces, primaryTextureReference)) {
                           if (object.has("rotation")) {
                              this.readRotation(cube, object.getAsJsonObject("rotation"));
                           }

                           this.cubes.add(cube);
                        }
                     }
                  }

                  this.bakeMesh();
                  this.readDisplayTransforms(root);
               }
            }
         }
      }

      private boolean readFaces(ItemReplacer.ModelCube cube, JsonObject faces, String primaryTextureReference) {
         boolean hasFace = false;
         if (faces.has("north")) {
            cube.north = this.readFace(faces.getAsJsonObject("north"), primaryTextureReference);
            hasFace |= cube.north != null;
         }

         if (faces.has("south")) {
            cube.south = this.readFace(faces.getAsJsonObject("south"), primaryTextureReference);
            hasFace |= cube.south != null;
         }

         if (faces.has("east")) {
            cube.east = this.readFace(faces.getAsJsonObject("east"), primaryTextureReference);
            hasFace |= cube.east != null;
         }

         if (faces.has("west")) {
            cube.west = this.readFace(faces.getAsJsonObject("west"), primaryTextureReference);
            hasFace |= cube.west != null;
         }

         if (faces.has("up")) {
            cube.up = this.readFace(faces.getAsJsonObject("up"), primaryTextureReference);
            hasFace |= cube.up != null;
         }

         if (faces.has("down")) {
            cube.down = this.readFace(faces.getAsJsonObject("down"), primaryTextureReference);
            hasFace |= cube.down != null;
         }

         return hasFace;
      }

      private ItemReplacer.FaceUv readFace(JsonObject face, String primaryTextureReference) {
         if (face.has("texture") && !face.get("texture").getAsString().equals(primaryTextureReference)) {
            return null;
         }

         JsonArray uv = face.getAsJsonArray("uv");
         return uv != null && uv.size() >= 4
            ? new ItemReplacer.FaceUv(
               uv.get(0).getAsFloat() * 0.0625F, uv.get(1).getAsFloat() * 0.0625F, uv.get(2).getAsFloat() * 0.0625F, uv.get(3).getAsFloat() * 0.0625F
            )
            : null;
      }

      private void readRotation(ItemReplacer.ModelCube cube, JsonObject rotation) {
         if (rotation != null && rotation.has("origin") && rotation.has("axis") && rotation.has("angle")) {
            JsonArray origin = rotation.getAsJsonArray("origin");
            cube.rotationOriginX = origin.get(0).getAsFloat() * 0.0625F;
            cube.rotationOriginY = origin.get(1).getAsFloat() * 0.0625F;
            cube.rotationOriginZ = origin.get(2).getAsFloat() * 0.0625F;
            cube.setRotation(rotation.get("axis").getAsString(), rotation.get("angle").getAsFloat());
         }
      }

      private String resolvePrimaryTextureKey(Map<String, String> textureMap, JsonObject root) {
         if (textureMap.containsKey("0")) {
            return "0";
         }

         Map<String, Integer> faceUseCount = new HashMap<>();
         JsonArray elements = root.getAsJsonArray("elements");
         if (elements != null) {
            for (JsonElement element : elements) {
               JsonObject faces = element.getAsJsonObject().getAsJsonObject("faces");
               if (faces != null) {
                  for (Entry<String, JsonElement> faceEntry : faces.entrySet()) {
                     JsonObject face = faceEntry.getValue().getAsJsonObject();
                     if (face.has("texture")) {
                        String reference = face.get("texture").getAsString().replace("#", "");
                        faceUseCount.merge(reference, 1, Integer::sum);
                     }
                  }
               }
            }
         }

         String bestKey = null;
         int bestCount = -1;

         for (Entry<String, Integer> entry : faceUseCount.entrySet()) {
            String key = entry.getKey();
            int count = entry.getValue();
            if (textureMap.containsKey(key) && !"particle".equals(key) && count > bestCount) {
               bestKey = key;
               bestCount = count;
            }
         }

         if (bestKey != null) {
            return bestKey;
         }

         for (String key : textureMap.keySet()) {
            if (!"particle".equals(key)) {
               return key;
            }
         }

         return textureMap.keySet().iterator().next();
      }

      private boolean hasPartialAlpha(Identifier location) {
         try (InputStream input = this.mc.getResourceManager().open(location)) {
            NativeImage image;
            int y;
            label108: {
               image = NativeImage.read(input);

               boolean var7;
               try {
                  y = 0;

                  label80:
                  while (true) {
                     if (y >= image.getHeight()) {
                        y = 0;
                        break label108;
                     }

                     for (int x = 0; x < image.getWidth(); x++) {
                        int alpha = image.getPixel(x, y) >>> 24 & 0xFF;
                        if (alpha > 0 && alpha < 255) {
                           var7 = true;
                           break label80;
                        }
                     }

                     y++;
                  }
               } catch (Throwable var10) {
                  if (image != null) {
                     try {
                        image.close();
                     } catch (Throwable var9) {
                        var10.addSuppressed(var9);
                     }
                  }

                  throw var10;
               }

               if (image != null) {
                  image.close();
               }

               return var7;
            }

            if (image != null) {
               image.close();
            }

            return false;
         } catch (Exception ignored) {
            return true;
         }
      }

      private void bakeMesh() {
         int faceCount = 0;

         for (ItemReplacer.ModelCube cube : this.cubes) {
            faceCount += cube.countFaces();
         }

         float[] baked = new float[faceCount * 4 * 8];
         int offset = 0;

         for (ItemReplacer.ModelCube cube : this.cubes) {
            offset = this.bakeCube(baked, offset, cube);
         }

         this.bakedMesh = baked;
         this.vertexCount = offset / 8;
         this.cubes.clear();
         this.cubes.trimToSize();
      }

      private int bakeCube(float[] out, int offset, ItemReplacer.ModelCube cube) {
         float x1 = cube.x1;
         float y1 = cube.y1;
         float z1 = cube.z1;
         float x2 = cube.x2;
         float y2 = cube.y2;
         float z2 = cube.z2;
         if (cube.north != null) {
            ItemReplacer.FaceUv face = cube.north;
            offset = this.writeBakedVertex(out, offset, cube, x2, y1, z1, face.u1, face.v2, 0.0F, 0.0F, -1.0F);
            offset = this.writeBakedVertex(out, offset, cube, x1, y1, z1, face.u2, face.v2, 0.0F, 0.0F, -1.0F);
            offset = this.writeBakedVertex(out, offset, cube, x1, y2, z1, face.u2, face.v1, 0.0F, 0.0F, -1.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y2, z1, face.u1, face.v1, 0.0F, 0.0F, -1.0F);
         }

         if (cube.south != null) {
            ItemReplacer.FaceUv face = cube.south;
            offset = this.writeBakedVertex(out, offset, cube, x1, y1, z2, face.u1, face.v2, 0.0F, 0.0F, 1.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y1, z2, face.u2, face.v2, 0.0F, 0.0F, 1.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y2, z2, face.u2, face.v1, 0.0F, 0.0F, 1.0F);
            offset = this.writeBakedVertex(out, offset, cube, x1, y2, z2, face.u1, face.v1, 0.0F, 0.0F, 1.0F);
         }

         if (cube.east != null) {
            ItemReplacer.FaceUv face = cube.east;
            offset = this.writeBakedVertex(out, offset, cube, x2, y1, z2, face.u1, face.v2, 1.0F, 0.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y1, z1, face.u2, face.v2, 1.0F, 0.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y2, z1, face.u2, face.v1, 1.0F, 0.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y2, z2, face.u1, face.v1, 1.0F, 0.0F, 0.0F);
         }

         if (cube.west != null) {
            ItemReplacer.FaceUv face = cube.west;
            offset = this.writeBakedVertex(out, offset, cube, x1, y1, z1, face.u1, face.v2, -1.0F, 0.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x1, y1, z2, face.u2, face.v2, -1.0F, 0.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x1, y2, z2, face.u2, face.v1, -1.0F, 0.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x1, y2, z1, face.u1, face.v1, -1.0F, 0.0F, 0.0F);
         }

         if (cube.up != null) {
            ItemReplacer.FaceUv face = cube.up;
            offset = this.writeBakedVertex(out, offset, cube, x1, y2, z1, face.u1, face.v1, 0.0F, 1.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x1, y2, z2, face.u1, face.v2, 0.0F, 1.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y2, z2, face.u2, face.v2, 0.0F, 1.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y2, z1, face.u2, face.v1, 0.0F, 1.0F, 0.0F);
         }

         if (cube.down != null) {
            ItemReplacer.FaceUv face = cube.down;
            offset = this.writeBakedVertex(out, offset, cube, x1, y1, z2, face.u1, face.v1, 0.0F, -1.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x1, y1, z1, face.u1, face.v2, 0.0F, -1.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y1, z1, face.u2, face.v2, 0.0F, -1.0F, 0.0F);
            offset = this.writeBakedVertex(out, offset, cube, x2, y1, z2, face.u2, face.v1, 0.0F, -1.0F, 0.0F);
         }

         return offset;
      }

      private int writeBakedVertex(
         float[] out, int offset, ItemReplacer.ModelCube cube, float x, float y, float z, float u, float v, float normalX, float normalY, float normalZ
      ) {
         if (cube.rotationAxis != 0) {
            float dx = x - cube.rotationOriginX;
            float dy = y - cube.rotationOriginY;
            float dz = z - cube.rotationOriginZ;
            float sin = cube.rotationSin;
            float cos = cube.rotationCos;
            switch (cube.rotationAxis) {
               case 1: {
                  y = cube.rotationOriginY + dy * cos - dz * sin;
                  z = cube.rotationOriginZ + dy * sin + dz * cos;
                  float rotatedNormalY = normalY * cos - normalZ * sin;
                  float rotatedNormalZ = normalY * sin + normalZ * cos;
                  normalY = rotatedNormalY;
                  normalZ = rotatedNormalZ;
                  break;
               }
               case 2: {
                  x = cube.rotationOriginX + dx * cos + dz * sin;
                  z = cube.rotationOriginZ - dx * sin + dz * cos;
                  float rotatedNormalX = normalX * cos + normalZ * sin;
                  float rotatedNormalZ = -normalX * sin + normalZ * cos;
                  normalX = rotatedNormalX;
                  normalZ = rotatedNormalZ;
                  break;
               }
               case 3: {
                  x = cube.rotationOriginX + dx * cos - dy * sin;
                  y = cube.rotationOriginY + dx * sin + dy * cos;
                  float rotatedNormalX = normalX * cos - normalY * sin;
                  float rotatedNormalY = normalX * sin + normalY * cos;
                  normalX = rotatedNormalX;
                  normalY = rotatedNormalY;
               }
            }
         }

         out[offset++] = x - 0.5F;
         out[offset++] = y - 0.5F;
         out[offset++] = z - 0.5F;
         out[offset++] = u;
         out[offset++] = v;
         out[offset++] = normalX;
         out[offset++] = normalY;
         out[offset++] = normalZ;
         return offset;
      }

      private void readDisplayTransforms(JsonObject root) {
         if (root.has("display")) {
            JsonObject display = root.getAsJsonObject("display");
            this.displayTransforms[0] = this.readDisplayTransform(display, "thirdperson_righthand");
            this.displayTransforms[1] = this.readDisplayTransform(display, "thirdperson_lefthand");
            this.displayTransforms[2] = this.readDisplayTransform(display, "firstperson_righthand");
            this.displayTransforms[3] = this.readDisplayTransform(display, "firstperson_lefthand");
            this.displayTransforms[4] = this.readDisplayTransform(display, "ground");
            this.displayTransforms[5] = this.readDisplayTransform(display, "gui");
            this.displayTransforms[6] = this.readDisplayTransform(display, "head");
            this.displayTransforms[7] = this.readDisplayTransform(display, "fixed");
         }
      }

      private ItemReplacer.DisplayTransform readDisplayTransform(JsonObject display, String key) {
         if (!display.has(key)) {
            return null;
         }

         JsonObject object = display.getAsJsonObject(key);
         float[] rotation = new float[]{0.0F, 0.0F, 0.0F};
         float[] translation = new float[]{0.0F, 0.0F, 0.0F};
         float[] scale = new float[]{1.0F, 1.0F, 1.0F};
         if (object.has("rotation")) {
            this.readFloatArray(object.getAsJsonArray("rotation"), rotation);
         }

         if (object.has("translation")) {
            this.readFloatArray(object.getAsJsonArray("translation"), translation);
         }

         if (object.has("scale")) {
            this.readFloatArray(object.getAsJsonArray("scale"), scale);
         }

         return new ItemReplacer.DisplayTransform(rotation, translation, scale);
      }

      private void readFloatArray(JsonArray array, float[] target) {
         for (int i = 0; i < target.length && i < array.size(); i++) {
            target[i] = array.get(i).getAsFloat();
         }
      }

      private void applyDisplayTransform(PoseStack matrixStack, ItemDisplayContext type) {
         ItemReplacer.DisplayTransform transform = this.getDisplayTransform(type);
         if (transform != null) {
            boolean leftHand = type == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || type == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            transform.apply(matrixStack, leftHand);
         }
      }

      private ItemReplacer.DisplayTransform getDisplayTransform(ItemDisplayContext type) {
         switch (type) {
            case THIRD_PERSON_RIGHT_HAND:
               return this.displayTransforms[0];
            case THIRD_PERSON_LEFT_HAND:
               return this.displayTransforms[1];
            case FIRST_PERSON_RIGHT_HAND:
               return this.displayTransforms[2];
            case FIRST_PERSON_LEFT_HAND:
               return this.displayTransforms[3];
            case GROUND:
               return this.displayTransforms[4];
            case GUI:
               return this.displayTransforms[5];
            case HEAD:
               return this.displayTransforms[6];
            case FIXED:
               return this.displayTransforms[7];
            default:
               return null;
         }
      }

      private void drawMesh(VertexConsumer builder, Pose pose, int light, int overlay) {
         int limit = this.vertexCount * 8;

         for (int i = 0; i < limit; i += 8) {
            builder.addVertex(pose, this.bakedMesh[i], this.bakedMesh[i + 1], this.bakedMesh[i + 2])
               .setColor(255, 255, 255, 255)
               .setUv(this.bakedMesh[i + 3], this.bakedMesh[i + 4])
               .setOverlay(overlay)
               .setLight(light)
               .setNormal(pose, this.bakedMesh[i + 5], this.bakedMesh[i + 6], this.bakedMesh[i + 7]);
         }
      }
   }

   public static final class SwordVariant {
      public final String label;
      public final String modelName;
      private final ItemReplacer.RuntimeSwordModel model;

      private SwordVariant(String label, String modelName) {
         this.label = label;
         this.modelName = modelName;
         this.model = new ItemReplacer.RuntimeSwordModel(modelName);
      }
   }
}
