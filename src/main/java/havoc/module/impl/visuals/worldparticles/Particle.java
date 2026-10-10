package havoc.module.impl.visuals.worldparticles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import havoc.render.TargetEspPipelines;
import havoc.utilites.IMinecraft;
import havoc.utilites.color.ColorUtil;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class Particle implements IMinecraft {
   private static final Particle.ParticleType[] RANDOM_TYPES = new Particle.ParticleType[]{
      Particle.ParticleType.HEART,
      Particle.ParticleType.LIGHT,
      Particle.ParticleType.SNOW,
      Particle.ParticleType.STAR,
      Particle.ParticleType.STAR_v2,
      Particle.ParticleType.CROSS,
      Particle.ParticleType.GLOW
   };
   private static final Identifier TEXTURE_HEART = Identifier.fromNamespaceAndPath("havoc", "textures/worldparticles/heart.png");
   private static final Identifier TEXTURE_LIGHT = Identifier.fromNamespaceAndPath("havoc", "textures/worldparticles/lightning.png");
   private static final Identifier TEXTURE_SNOW = Identifier.fromNamespaceAndPath("havoc", "textures/worldparticles/snowflake.png");
   private static final Identifier TEXTURE_STAR = Identifier.fromNamespaceAndPath("havoc", "textures/worldparticles/star.png");
   private static final Identifier TEXTURE_STAR_v2 = Identifier.fromNamespaceAndPath("havoc", "textures/worldparticles/star1.png");
   private static final Identifier TEXTURE_CROSS = Identifier.fromNamespaceAndPath("havoc", "textures/worldparticles/cross.png");
   private static final Identifier TEXTURE_GLOW = Identifier.fromNamespaceAndPath("havoc", "textures/worldparticles/dashbloom.png");
   private static final Identifier TEXTURE_GLOW_SAMPLE = Identifier.fromNamespaceAndPath("havoc", "textures/worldparticles/dashbloomsample.png");
   private static final long FADE_IN_MS = 600L;
   private static final long FADE_OUT_MS = 900L;
   private static final long WHITE_TRANSITION_MS = 700L;
   private double x;
   private double y;
   private double z;
   private double prevX;
   private double prevY;
   private double prevZ;
   private double motionX;
   private double motionY;
   private double motionZ;
   private final long spawnTimeMs;
   private final long lifeTimeMs;
   private final int randomColor;
   private final float phase;
   private float rotation;
   private float rotationSpeed;
   private boolean physicsEnabled = true;
   private float fallSpeedMultiplier = 1.0F;
   private float size = 0.5F;
   private boolean fadingOut;
   private long fadeOutStartedAtMs;
   private Particle.ParticleType type = Particle.ParticleType.CUBE_3D;
   private Particle.ParticleType actualType = Particle.ParticleType.CUBE_3D;
   private Identifier texture;
   private float cachedAlpha;
   private Integer customColor;
   private long visibilityCheckedAtMs;
   private boolean cachedVisible = true;
   private double visibilityCameraX;
   private double visibilityCameraY;
   private double visibilityCameraZ;
   private double visibilityParticleX;
   private double visibilityParticleY;
   private double visibilityParticleZ;

   public Particle(double x, double y, double z, double motionX, double motionY, double motionZ, long lifeTimeMs) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.prevX = x;
      this.prevY = y;
      this.prevZ = z;
      this.motionX = motionX;
      this.motionY = motionY;
      this.motionZ = motionZ;
      this.lifeTimeMs = lifeTimeMs;
      this.spawnTimeMs = System.currentTimeMillis();
      this.randomColor = this.generateRandomColor();
      this.phase = ThreadLocalRandom.current().nextFloat() * 360.0F;
      this.rotation = ThreadLocalRandom.current().nextFloat() * 360.0F;
      this.rotationSpeed = 0.5F + ThreadLocalRandom.current().nextFloat() * 1.5F;
   }

   public Particle setType(Particle.ParticleType type) {
      this.type = type;
      this.actualType = type == Particle.ParticleType.RANDOM ? RANDOM_TYPES[ThreadLocalRandom.current().nextInt(RANDOM_TYPES.length)] : type;
      this.texture = this.getTextureForType(this.actualType);
      return this;
   }

   public Particle setFallSpeedMultiplier(float multiplier) {
      this.fallSpeedMultiplier = Math.max(0.0F, multiplier);
      return this;
   }

   public Particle setPhysics(boolean enabled) {
      this.physicsEnabled = enabled;
      return this;
   }

   public Particle setSize(float size) {
      this.size = size;
      return this;
   }

   public Particle setCustomColor(Integer color) {
      this.customColor = color;
      return this;
   }

   public void update(Level world, long now) {
      this.prevX = this.x;
      this.prevY = this.y;
      this.prevZ = this.z;
      if (!this.fadingOut && now - this.spawnTimeMs >= this.lifeTimeMs) {
         this.startFadeOut(now);
      }

      this.moveWithCollision(world);
      this.motionX *= 0.99;
      this.motionY *= 0.99;
      this.motionZ *= 0.99;
      if (this.physicsEnabled) {
         this.motionY = this.motionY - (1.5E-4 + 4.5E-4 * this.fallSpeedMultiplier);
      }

      this.rotation = this.rotation + this.rotationSpeed;
      this.cachedAlpha = this.computeAlpha(now);
   }

   public void render(
      PoseStack matrices,
      MultiBufferSource consumers,
      Camera camera,
      Vec3 cameraPos,
      float tickDelta,
      int baseColor,
      float glowScale,
      boolean useRandomColor,
      boolean whiteOnSpawn,
      boolean whiteCenter
   ) {
      float alpha = this.cachedAlpha;
      if (!(alpha <= 0.01F)) {
         double interpX = Mth.lerp(tickDelta, this.prevX, this.x);
         double interpY = Mth.lerp(tickDelta, this.prevY, this.y);
         double interpZ = Mth.lerp(tickDelta, this.prevZ, this.z);
         float relX = (float)(interpX - cameraPos.x);
         float relY = (float)(interpY - cameraPos.y);
         float relZ = (float)(interpZ - cameraPos.z);
         int color = this.getColor(baseColor, useRandomColor, whiteOnSpawn);
         if (this.actualType == Particle.ParticleType.CUBE_3D) {
            this.renderCube(matrices, consumers, camera, relX, relY, relZ, color, glowScale, alpha);
         } else {
            this.renderTextured(matrices, consumers, camera, relX, relY, relZ, color, glowScale, alpha, whiteCenter);
         }
      }
   }

   public Vec3 getInterpolatedPos(float tickDelta) {
      return new Vec3(Mth.lerp(tickDelta, this.prevX, this.x), Mth.lerp(tickDelta, this.prevY, this.y), Mth.lerp(tickDelta, this.prevZ, this.z));
   }

   public Vec3 getCubeRotation(long now) {
      float timeRotation = (float)(now % 2800L) / 2800.0F * 360.0F;
      float rotY = this.phase + timeRotation;
      float rotX = this.phase * 0.45F + timeRotation * 1.1F;
      float rotZ = this.phase * 0.22F + timeRotation * 0.8F;
      return new Vec3(rotX, rotY, rotZ);
   }

   public float getAlpha() {
      return this.cachedAlpha;
   }

   public float getSize() {
      return this.size;
   }

   public float getRenderScale(long now) {
      float fadeInProgress = Mth.clamp((float)(now - this.spawnTimeMs) / 600.0F, 0.0F, 1.0F);
      float fadeInEase = fadeInProgress * fadeInProgress * (3.0F - 2.0F * fadeInProgress);
      float baseScale = 0.72F + fadeInEase * 0.28F;
      if (!this.fadingOut) {
         return baseScale;
      }

      float fadeOutProgress = Mth.clamp((float)(now - this.fadeOutStartedAtMs) / 900.0F, 0.0F, 1.0F);
      float fadeOutEase = fadeOutProgress * fadeOutProgress * (3.0F - 2.0F * fadeOutProgress);
      return baseScale * (1.0F - fadeOutEase * 0.52F);
   }

   public Identifier getTexture() {
      if (this.actualType == Particle.ParticleType.CUBE_3D) {
         return TEXTURE_GLOW_SAMPLE;
      } else {
         return this.texture == null ? TEXTURE_GLOW : this.texture;
      }
   }

   public Particle.ParticleType getActualType() {
      return this.actualType;
   }

   public boolean hasWhiteCore() {
      return switch (this.actualType) {
         case HEART, LIGHT, SNOW, STAR_v2 -> false;
         default -> true;
      };
   }

   public void startFadeOut(long now) {
      if (!this.fadingOut) {
         this.fadingOut = true;
         this.fadeOutStartedAtMs = now;
      }
   }

   public boolean shouldRemove() {
      return this.fadingOut && this.cachedAlpha <= 0.0F;
   }

   public boolean isFadingOut() {
      return this.fadingOut;
   }

   public double getDistanceSquaredTo(Vec3 pos) {
      double dx = this.x - pos.x;
      double dy = this.y - pos.y;
      double dz = this.z - pos.z;
      return dx * dx + dy * dy + dz * dz;
   }

   public boolean needsVisibilityRefresh(Vec3 cameraPos, Vec3 particlePos, long now) {
      if (this.visibilityCheckedAtMs != 0L && now - this.visibilityCheckedAtMs < 120L) {
         double cameraDx = cameraPos.x - this.visibilityCameraX;
         double cameraDy = cameraPos.y - this.visibilityCameraY;
         double cameraDz = cameraPos.z - this.visibilityCameraZ;
         if (cameraDx * cameraDx + cameraDy * cameraDy + cameraDz * cameraDz > 0.64) {
            return true;
         }

         double particleDx = particlePos.x - this.visibilityParticleX;
         double particleDy = particlePos.y - this.visibilityParticleY;
         double particleDz = particlePos.z - this.visibilityParticleZ;
         return particleDx * particleDx + particleDy * particleDy + particleDz * particleDz > 0.09;
      } else {
         return true;
      }
   }

   public void updateVisibilityCache(Vec3 cameraPos, Vec3 particlePos, long now, boolean visible) {
      this.visibilityCheckedAtMs = now;
      this.cachedVisible = visible;
      this.visibilityCameraX = cameraPos.x;
      this.visibilityCameraY = cameraPos.y;
      this.visibilityCameraZ = cameraPos.z;
      this.visibilityParticleX = particlePos.x;
      this.visibilityParticleY = particlePos.y;
      this.visibilityParticleZ = particlePos.z;
   }

   public boolean isCachedVisible() {
      return this.cachedVisible;
   }

   public double getHorizontalDistanceSquaredTo(Vec3 pos) {
      double dx = this.x - pos.x;
      double dz = this.z - pos.z;
      return dx * dx + dz * dz;
   }

   private void renderCube(
      PoseStack matrices, MultiBufferSource consumers, Camera camera, float relX, float relY, float relZ, int color, float glowScale, float alpha
   ) {
      VertexConsumer solid = consumers.getBuffer(TargetEspPipelines.RING_LAYER);
      VertexConsumer glow = consumers.getBuffer(RenderTypes.entityTranslucent(TEXTURE_GLOW));
      float cubeSize = this.size * 0.34F;
      float timeRotation = (float)(System.currentTimeMillis() % 2800L) / 2800.0F * 360.0F;
      float rotY = this.phase + timeRotation;
      float rotX = this.phase * 0.45F + timeRotation * 1.1F;
      float rotZ = this.phase * 0.22F + timeRotation * 0.8F;
      int faceColor = ColorUtil.multiplyAlpha(color, 0.72F * alpha);
      int brightFace = ColorUtil.multiplyAlpha(ColorUtil.interpolate(color, -1, 0.38F), 0.9F * alpha);
      matrices.pushPose();
      matrices.translate(relX, relY, relZ);
      matrices.mulPose(Axis.XP.rotationDegrees(rotX));
      matrices.mulPose(Axis.YP.rotationDegrees(rotY));
      matrices.mulPose(Axis.ZP.rotationDegrees(rotZ));
      matrices.scale(cubeSize, cubeSize, cubeSize);
      this.drawCubeGeometry(matrices.last(), solid, faceColor, brightFace);
      matrices.popPose();
      matrices.pushPose();
      matrices.translate(relX, relY, relZ);
      matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
      matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
      Pose glowEntry = matrices.last();
      ParticleRenderer.drawBillboard(glowEntry, glow, cubeSize * (1.15F + glowScale * 0.18F), ColorUtil.multiplyAlpha(color, 0.16F * alpha));
      matrices.popPose();
   }

   private void drawCubeGeometry(Pose pose, VertexConsumer out, int side, int light) {
      this.face(pose, out, -1.0F, -1.0F, 1.0F, 1.0F, -1.0F, 1.0F, 1.0F, 1.0F, 1.0F, -1.0F, 1.0F, 1.0F, light);
      this.face(pose, out, 1.0F, -1.0F, -1.0F, -1.0F, -1.0F, -1.0F, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F, -1.0F, side);
      this.face(pose, out, -1.0F, -1.0F, -1.0F, -1.0F, -1.0F, 1.0F, -1.0F, 1.0F, 1.0F, -1.0F, 1.0F, -1.0F, side);
      this.face(pose, out, 1.0F, -1.0F, 1.0F, 1.0F, -1.0F, -1.0F, 1.0F, 1.0F, -1.0F, 1.0F, 1.0F, 1.0F, light);
      this.face(pose, out, -1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, -1.0F, -1.0F, 1.0F, -1.0F, light);
      this.face(pose, out, -1.0F, -1.0F, -1.0F, 1.0F, -1.0F, -1.0F, 1.0F, -1.0F, 1.0F, -1.0F, -1.0F, 1.0F, side);
   }

   private void face(
      Pose pose,
      VertexConsumer out,
      float ax,
      float ay,
      float az,
      float bx,
      float by,
      float bz,
      float cx,
      float cy,
      float cz,
      float dx,
      float dy,
      float dz,
      int color
   ) {
      out.addVertex(pose, ax, ay, az).setColor(color);
      out.addVertex(pose, bx, by, bz).setColor(color);
      out.addVertex(pose, cx, cy, cz).setColor(color);
      out.addVertex(pose, dx, dy, dz).setColor(color);
   }

   private void renderTextured(
      PoseStack matrices,
      MultiBufferSource consumers,
      Camera camera,
      float relX,
      float relY,
      float relZ,
      int color,
      float glowScale,
      float alpha,
      boolean whiteCenter
   ) {
      if (this.texture != null) {
         VertexConsumer spriteConsumer = consumers.getBuffer(RenderTypes.entityTranslucent(this.texture));
         VertexConsumer glowPrimary = consumers.getBuffer(RenderTypes.entityTranslucent(TEXTURE_GLOW));
         VertexConsumer glowSecondary = consumers.getBuffer(RenderTypes.entityTranslucent(TEXTURE_GLOW_SAMPLE));
         float spriteSize = this.size * 0.9F;
         int whiteCoreColor = ColorUtil.withAlpha(16777215, Math.max(1, Math.round(255.0F * alpha * 0.35F)));
         matrices.pushPose();
         matrices.translate(relX, relY, relZ);
         matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
         matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
         matrices.mulPose(Axis.ZP.rotationDegrees(this.rotation));
         Pose entry = matrices.last();
         ParticleRenderer.drawBillboard(entry, spriteConsumer, spriteSize, ColorUtil.multiplyAlpha(color, alpha));
         if (this.hasWhiteCore()) {
            ParticleRenderer.drawBillboard(entry, spriteConsumer, spriteSize * 0.42F, whiteCoreColor, 0.001F);
         }

         if (whiteCenter) {
            ParticleRenderer.drawBillboard(entry, spriteConsumer, spriteSize * 0.52F, ColorUtil.withAlpha(16777215, Math.round(235.0F * alpha)), 0.001F);
         }

         matrices.popPose();
         matrices.pushPose();
         matrices.translate(relX, relY, relZ);
         matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
         matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
         Pose glowEntry = matrices.last();
         ParticleRenderer.drawBillboard(glowEntry, glowPrimary, spriteSize * glowScale * 0.78F, ColorUtil.multiplyAlpha(color, 0.34F * alpha));
         ParticleRenderer.drawBillboard(glowEntry, glowSecondary, spriteSize * glowScale * 0.34F, ColorUtil.multiplyAlpha(color, 0.62F * alpha), 0.001F);
         matrices.popPose();
      }
   }

   private void moveWithCollision(Level world) {
      if (world == null) {
         this.x = this.x + this.motionX;
         this.y = this.y + this.motionY;
         this.z = this.z + this.motionZ;
      } else {
         if (this.isHit(world, this.x + this.motionX, this.y, this.z)) {
            this.motionX *= -0.8;
         } else {
            this.x = this.x + this.motionX;
         }

         if (this.isHit(world, this.x, this.y + this.motionY, this.z)) {
            this.motionY *= -0.8;
         } else {
            this.y = this.y + this.motionY;
         }

         if (this.isHit(world, this.x, this.y, this.z + this.motionZ)) {
            this.motionZ *= -0.8;
         } else {
            this.z = this.z + this.motionZ;
         }
      }
   }

   private boolean isHit(Level world, double x, double y, double z) {
      BlockPos pos = BlockPos.containing(x, y, z);
      return world.getBlockState(pos).isCollisionShapeFullBlock(world, pos);
   }

   private float computeAlpha(long now) {
      float fadeIn = Mth.clamp((float)(now - this.spawnTimeMs) / 600.0F, 0.0F, 1.0F);
      if (!this.fadingOut) {
         return fadeIn;
      }

      float fadeOutProgress = Mth.clamp((float)(now - this.fadeOutStartedAtMs) / 900.0F, 0.0F, 1.0F);
      float fadeOutEase = fadeOutProgress * fadeOutProgress * (3.0F - 2.0F * fadeOutProgress);
      float fadeOut = 1.0F - fadeOutEase;
      return Math.max(0.0F, Math.min(fadeIn, fadeOut));
   }

   public int getColor(int baseColor, boolean useRandomColor, boolean whiteOnSpawn) {
      int targetColor = this.customColor != null ? this.customColor : (useRandomColor ? this.randomColor : baseColor);
      if (!whiteOnSpawn) {
         return ColorUtil.withAlpha(targetColor, Math.round(ColorUtil.alpha(targetColor) * this.cachedAlpha));
      }

      long elapsed = System.currentTimeMillis() - this.spawnTimeMs;
      float progress = Mth.clamp((float)elapsed / 700.0F, 0.0F, 1.0F);
      int blended = ColorUtil.interpolate(-1, targetColor & 16777215 | 0xFF000000, progress);
      return ColorUtil.withAlpha(blended, Math.round(255.0F * this.cachedAlpha));
   }

   private int generateRandomColor() {
      int[] palette = new int[]{-65536, -33024, -256, -16711936, -16711681, -16776961, -7667457, -65281, -60269, -1, -16711809, -40121};
      return palette[ThreadLocalRandom.current().nextInt(palette.length)];
   }

   private Identifier getTextureForType(Particle.ParticleType type) {
      return switch (type) {
         case HEART -> TEXTURE_HEART;
         case LIGHT -> TEXTURE_LIGHT;
         case SNOW -> TEXTURE_SNOW;
         case STAR -> TEXTURE_STAR;
         case STAR_v2 -> TEXTURE_STAR_v2;
         case CROSS -> TEXTURE_CROSS;
         case GLOW -> TEXTURE_GLOW;
         default -> null;
      };
   }

   public enum ParticleType {
      CUBE_3D,
      HEART,
      LIGHT,
      SNOW,
      STAR,
      STAR_v2,
      CROSS,
      GLOW,
      RANDOM;
   }
}
