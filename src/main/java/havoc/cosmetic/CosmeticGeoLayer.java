package havoc.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import havoc.Havoc;
import havoc.module.impl.visuals.Cosmetics;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;

public final class CosmeticGeoLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
   private static final float PIXEL = 0.0625F;
   private static final Map<Integer, Long> START_TIMES = new java.util.concurrent.ConcurrentHashMap<>();
   private int order;
   private float calibX;
   private float calibY;
   private float calibZ;
   private float calibYaw;
   private float calibScale = 1.0F;

   public CosmeticGeoLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
      super(parent);
   }

   @Override
   public void submit(PoseStack matrices, SubmitNodeCollector collector, int light, AvatarRenderState state, float limbAngle, float limbDistance) {
      Minecraft mc = Minecraft.getInstance();
      Cosmetics cosmetics = Havoc.getInstance() == null ? null : Havoc.getInstance().getModuleManager().getModule(Cosmetics.class);
      if (mc.player != null && cosmetics != null && cosmetics.isEnable() && state.id == mc.player.getId() && !state.isInvisible) {
         List<Integer> indices = cosmetics.selectedGeoIndices();
         if (!indices.isEmpty()) {
            this.order = 0;
            this.calibX = cosmetics.calibX.getValue();
            this.calibY = cosmetics.calibY.getValue();
            this.calibZ = cosmetics.calibZ.getValue();
            this.calibYaw = cosmetics.calibYaw.getValue();
            this.calibScale = cosmetics.calibScale.getValue();

            for (Integer index : indices) {
               CosmeticRegistry.Entry entry = CosmeticRegistry.get(index);
               if (entry != null) {
                  entry.ensureLoaded();
                  GeoModel geometry = entry.geometry;
                  if (geometry != null && !geometry.topLevelBones.isEmpty()) {
                     Identifier texture = entry.texture(mc);
                     if (texture != null) {
                        this.renderEntry(entry, geometry, RenderTypes.entityTranslucent(texture), matrices, collector, light);
                     }
                  }
               }
            }
         }
      }
   }

   private void renderEntry(
      CosmeticRegistry.Entry entry,
      GeoModel geometry,
      RenderType renderType,
      PoseStack matrices,
      SubmitNodeCollector collector,
      int light
   ) {
      float time = this.animationTime(entry);
      matrices.pushPose();
      float positionY = this.attachToBodyPart(matrices, entry);
      matrices.mulPose(new Quaternionf().rotationZ((float)Math.PI));
      matrices.translate(entry.offX + this.calibX, entry.offY + positionY + this.calibY, entry.offZ + this.calibZ);
      if (this.calibYaw != 0.0F) {
         matrices.mulPose(new Quaternionf().rotationY((float)Math.toRadians(this.calibYaw)));
      }

      if (entry.yaw != 0.0F) {
         matrices.mulPose(new Quaternionf().rotationY((float)Math.toRadians(entry.yaw)));
      }

      if (entry.pitch != 0.0F) {
         matrices.mulPose(new Quaternionf().rotationX((float)Math.toRadians(entry.pitch)));
      }

      if (entry.roll != 0.0F) {
         matrices.mulPose(new Quaternionf().rotationZ((float)Math.toRadians(entry.roll)));
      }

      float scale = entry.scale * this.calibScale;
      if (scale != 1.0F) {
         matrices.scale(scale, scale, scale);
      }

      for (GeoModel.Bone bone : geometry.topLevelBones) {
         this.renderBone(bone, entry, time, geometry, renderType, matrices, collector, light);
      }

      matrices.popPose();
   }

   private float animationTime(CosmeticRegistry.Entry entry) {
      CosmeticRegistry.Animation animation = entry.animation();
      long now = System.nanoTime();
      long start = START_TIMES.computeIfAbsent(entry.index, k -> now);
      double seconds = (now - start) / 1.0E9;
      if (animation != null && animation.loop && animation.length > 0.0F) {
         seconds %= animation.length;
      }

      return (float)seconds;
   }

   private void renderBone(
      GeoModel.Bone bone,
      CosmeticRegistry.Entry entry,
      float time,
      GeoModel geometry,
      RenderType renderType,
      PoseStack matrices,
      SubmitNodeCollector collector,
      int light
   ) {
      matrices.pushPose();
      float rotX = bone.rotationX;
      float rotY = bone.rotationY;
      float rotZ = bone.rotationZ;
      float posX = bone.posX;
      float posY = bone.posY;
      float posZ = bone.posZ;
      float scaleX = bone.scaleX;
      float scaleY = bone.scaleY;
      float scaleZ = bone.scaleZ;
      CosmeticRegistry.Animation animation = entry.animation();
      if (animation != null) {
         CosmeticRegistry.BoneAnimation boneAnimation = animation.bones.get(bone.name);
         if (boneAnimation != null) {
            float[] rotation = sample(boneAnimation.rotation, time);
            if (rotation != null) {
               rotX -= (float)Math.toRadians(rotation[0]);
               rotY -= (float)Math.toRadians(rotation[1]);
               rotZ += (float)Math.toRadians(rotation[2]);
            }

            float[] position = sample(boneAnimation.position, time);
            if (position != null) {
               posX += position[0];
               posY += position[1];
               posZ += position[2];
            }

            float[] scale = sample(boneAnimation.scale, time);
            if (scale != null) {
               scaleX *= scale[0];
               scaleY *= scale[1];
               scaleZ *= scale[2];
            }
         }
      }

      matrices.translate(-posX * PIXEL, posY * PIXEL, posZ * PIXEL);
      float pivotX = bone.pivotX * PIXEL;
      float pivotY = bone.pivotY * PIXEL;
      float pivotZ = bone.pivotZ * PIXEL;
      matrices.translate(pivotX, pivotY, pivotZ);
      if (rotZ != 0.0F) {
         matrices.mulPose(new Quaternionf().rotationZ(rotZ));
      }

      if (rotY != 0.0F) {
         matrices.mulPose(new Quaternionf().rotationY(rotY));
      }

      if (rotX != 0.0F) {
         matrices.mulPose(new Quaternionf().rotationX(rotX));
      }

      matrices.scale(scaleX, scaleY, scaleZ);
      matrices.translate(-pivotX, -pivotY, -pivotZ);

      for (GeoModel.Cube cube : bone.cubes) {
         this.submitCube(cube, renderType, matrices, collector, light);
      }

      for (GeoModel.Bone child : bone.childBones) {
         this.renderBone(child, entry, time, geometry, renderType, matrices, collector, light);
      }

      matrices.popPose();
   }

   private void submitCube(GeoModel.Cube cube, RenderType renderType, PoseStack matrices, SubmitNodeCollector collector, int light) {
      matrices.pushPose();
      matrices.translate(cube.pivotX, cube.pivotY, cube.pivotZ);
      if (cube.rotationZ != 0.0F) {
         matrices.mulPose(new Quaternionf().rotationZ(cube.rotationZ));
      }

      if (cube.rotationY != 0.0F) {
         matrices.mulPose(new Quaternionf().rotationY(cube.rotationY));
      }

      if (cube.rotationX != 0.0F) {
         matrices.mulPose(new Quaternionf().rotationX(cube.rotationX));
      }

      matrices.translate(-cube.pivotX, -cube.pivotY, -cube.pivotZ);
      collector.order(this.order++).submitCustomGeometry(matrices, renderType, (pose, consumer) -> writeCube(consumer, pose, cube, light));
      matrices.popPose();
   }

   private float attachToBodyPart(PoseStack matrices, CosmeticRegistry.Entry entry) {
      PlayerModel model = this.getParentModel();
      ModelPart part = entry.position == 2 ? model.head : model.body;
      float y = entry.position == 2 ? 0.5F : -0.3F;
      if (part != null) {
         matrices.translate(part.x * PIXEL, part.y * PIXEL, part.z * PIXEL);
         if (part.zRot != 0.0F) {
            matrices.mulPose(new Quaternionf().rotationZ(part.zRot));
         }

         if (part.yRot != 0.0F) {
            matrices.mulPose(new Quaternionf().rotationY(part.yRot));
         }

         if (part.xRot != 0.0F) {
            matrices.mulPose(new Quaternionf().rotationX(part.xRot));
         }
      }

      return y;
   }

   private static void writeCube(VertexConsumer consumer, PoseStack.Pose pose, GeoModel.Cube cube, int light) {
      for (GeoModel.Quad quad : cube.quads) {
         if (quad != null) {
            for (int i = 0; i < 4; i++) {
               consumer.addVertex(pose, quad.xs[i], quad.ys[i], quad.zs[i])
                  .setColor(255, 255, 255, 255)
                  .setUv(quad.uvs[i * 2], quad.uvs[i * 2 + 1])
                  .setOverlay(OverlayTexture.NO_OVERLAY)
                  .setLight(light)
                  .setNormal(pose, quad.normalX, quad.normalY, quad.normalZ);
            }
         }
      }
   }

   private static float[] sample(Map<Float, float[]> keyframes, float time) {
      if (keyframes == null || keyframes.isEmpty()) {
         return null;
      } else {
         Float prevKey = null;
         Float nextKey = null;

         for (Float key : keyframes.keySet()) {
            if (key <= time && (prevKey == null || key > prevKey)) {
               prevKey = key;
            }

            if (key >= time && (nextKey == null || key < nextKey)) {
               nextKey = key;
            }
         }

         if (prevKey == null) {
            return keyframes.get(nextKey);
         } else if (nextKey == null) {
            return keyframes.get(prevKey);
         } else if (prevKey.equals(nextKey)) {
            return keyframes.get(prevKey);
         } else {
            float[] a = keyframes.get(prevKey);
            float[] b = keyframes.get(nextKey);
            float t = (time - prevKey) / (nextKey - prevKey);
            return new float[]{a[0] + t * (b[0] - a[0]), a[1] + t * (b[1] - a[1]), a[2] + t * (b[2] - a[2])};
         }
      }
   }
}
