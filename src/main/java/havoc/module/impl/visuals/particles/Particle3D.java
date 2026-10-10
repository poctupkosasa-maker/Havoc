package havoc.module.impl.visuals.particles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import havoc.module.impl.visuals.worldparticles.ParticleRenderer;
import havoc.utilites.IMinecraft;
import havoc.utilites.color.ColorUtil;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Particle3D implements IMinecraft {
   private static final Particle3D.ParticleMode[] RANDOM_MODES = new Particle3D.ParticleMode[]{
      Particle3D.ParticleMode.HEART,
      Particle3D.ParticleMode.LIGHT,
      Particle3D.ParticleMode.SNOW,
      Particle3D.ParticleMode.STAR,
      Particle3D.ParticleMode.STAR_v2,
      Particle3D.ParticleMode.CROSS,
      Particle3D.ParticleMode.GLOW
   };
   private static final Identifier TEXTURE_HEART = Identifier.fromNamespaceAndPath("havoc", "textures/worldparticles/heart.png");
   private static final Identifier TEXTURE_LIGHT = Identifier.fromNamespaceAndPath("havoc", "textures/worldparticles/lightning.png");
   private static final Identifier TEXTURE_SNOW = Identifier.fromNamespaceAndPath("havoc", "textures/worldparticles/snowflake.png");
   private static final Identifier TEXTURE_STAR = Identifier.fromNamespaceAndPath("havoc", "textures/worldparticles/star.png");
   private static final Identifier TEXTURE_STAR_v2 = Identifier.fromNamespaceAndPath("havoc", "textures/worldparticles/star1.png");
   private static final Identifier TEXTURE_CROSS = Identifier.fromNamespaceAndPath("havoc", "textures/worldparticles/cross.png");
   private static final Identifier TEXTURE_GLOW = Identifier.fromNamespaceAndPath("havoc", "textures/worldparticles/dashbloom.png");
   private static final Identifier TEXTURE_GLOW_SAMPLE = Identifier.fromNamespaceAndPath("havoc", "textures/worldparticles/dashbloomsample.png");
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
   private final int color;
   private final float scale;
   private final float phase;
   private float rotation;
   private float gravityStrength = 0.04F;
   private float velocityMultiplier = 0.98F;
   private long motionDelayMs;
   private boolean collidesWithWorld = true;
   private boolean spinning = true;
   private boolean fadingOut;
   private long fadeOutStartMs;
   private float cachedAlpha;
   private Particle3D.ParticleMode mode = Particle3D.ParticleMode.BOX;
   private Particle3D.ParticleMode actualMode = Particle3D.ParticleMode.BOX;
   private Particle3D.GlowMode glowMode = Particle3D.GlowMode.BOTH;

   public Particle3D(Vec3 pos, Vec3 velocity, int color, float scale, float maxAgeSeconds) {
      this.x = pos.x;
      this.y = pos.y;
      this.z = pos.z;
      this.prevX = pos.x;
      this.prevY = pos.y;
      this.prevZ = pos.z;
      this.motionX = velocity.x;
      this.motionY = velocity.y;
      this.motionZ = velocity.z;
      this.color = color;
      this.scale = scale;
      this.lifeTimeMs = (long)(maxAgeSeconds * 1000.0F);
      this.spawnTimeMs = System.currentTimeMillis();
      this.phase = ThreadLocalRandom.current().nextFloat() * 360.0F;
      this.rotation = ThreadLocalRandom.current().nextFloat() * 360.0F;
   }

   public Particle3D setGravity(float gravity) {
      this.gravityStrength = gravity;
      return this;
   }

   public Particle3D setVelocityMultiplier(float multiplier) {
      this.velocityMultiplier = multiplier;
      return this;
   }

   public Particle3D setMotionDelayMs(long delayMs) {
      this.motionDelayMs = Math.max(0L, delayMs);
      return this;
   }

   public Particle3D setCollision(boolean collision) {
      this.collidesWithWorld = collision;
      return this;
   }

   public Particle3D setMode(Particle3D.ParticleMode mode) {
      this.mode = mode;
      this.actualMode = mode == Particle3D.ParticleMode.RANDOM ? RANDOM_MODES[ThreadLocalRandom.current().nextInt(RANDOM_MODES.length)] : mode;
      return this;
   }

   public Particle3D setGlowMode(Particle3D.GlowMode glowMode) {
      this.glowMode = glowMode;
      return this;
   }

   public Particle3D setSpinning(boolean spinning) {
      this.spinning = spinning;
      return this;
   }

   public void update() {
      long now = System.currentTimeMillis();
      this.prevX = this.x;
      this.prevY = this.y;
      this.prevZ = this.z;
      if (!this.fadingOut && now - this.spawnTimeMs >= this.motionDelayMs + this.lifeTimeMs) {
         this.fadingOut = true;
         this.fadeOutStartMs = now;
      }

      if (now - this.spawnTimeMs < this.motionDelayMs) {
         this.cachedAlpha = this.calculateAlpha(now);
      } else {
         this.motionY = this.motionY - this.gravityStrength;
         if (this.collidesWithWorld && mc.level != null) {
            if (this.isHit(this.x + this.motionX, this.y, this.z)) {
               this.motionX *= -0.8;
            } else {
               this.x = this.x + this.motionX;
            }

            if (this.isHit(this.x, this.y + this.motionY, this.z)) {
               this.motionX *= 0.999;
               this.motionZ *= 0.999;
               this.motionY *= -0.7;
            } else {
               this.y = this.y + this.motionY;
            }

            if (this.isHit(this.x, this.y, this.z + this.motionZ)) {
               this.motionZ *= -0.8;
            } else {
               this.z = this.z + this.motionZ;
            }
         } else {
            this.x = this.x + this.motionX;
            this.y = this.y + this.motionY;
            this.z = this.z + this.motionZ;
         }

         this.motionX = this.motionX * this.velocityMultiplier;
         this.motionY = this.motionY * this.velocityMultiplier;
         this.motionZ = this.motionZ * this.velocityMultiplier;
         if (this.spinning) {
            this.rotation += 2.0F;
         }

         this.cachedAlpha = this.calculateAlpha(now);
      }
   }

   public boolean isDead() {
      return this.fadingOut && this.cachedAlpha <= 0.0F;
   }

   public float getAlpha() {
      return this.calculateAlpha(System.currentTimeMillis());
   }

   private float calculateAlpha(long now) {
      float fadeIn = Mth.clamp((float)(now - this.spawnTimeMs) / 150.0F, 0.0F, 1.0F);
      if (!this.fadingOut) {
         return fadeIn;
      }

      float fadeOut = 1.0F - Mth.clamp((float)(now - this.fadeOutStartMs) / 250.0F, 0.0F, 1.0F);
      return Math.max(0.0F, Math.min(fadeIn, fadeOut));
   }

   public void render(PoseStack matrices, MultiBufferSource.BufferSource consumers, Camera camera, Vec3 cameraPos, float tickDelta) {
      long now = System.currentTimeMillis();
      float alpha = this.calculateAlpha(now);
      if (!(alpha <= 0.01F)) {
         float visualScale = this.scale * this.getVisualScale(now);
         float relX = (float)(Mth.lerp(tickDelta, this.prevX, this.x) - cameraPos.x);
         float relY = (float)(Mth.lerp(tickDelta, this.prevY, this.y) - cameraPos.y);
         float relZ = (float)(Mth.lerp(tickDelta, this.prevZ, this.z) - cameraPos.z);
         if (this.actualMode == Particle3D.ParticleMode.BOX) {
            this.renderBox(matrices, consumers, camera, relX, relY, relZ, alpha, visualScale, now);
         } else {
            this.renderTextured(matrices, consumers, camera, relX, relY, relZ, alpha, visualScale);
         }
      }
   }

   private float getVisualScale(long now) {
      float appear = Mth.clamp((float)(now - this.spawnTimeMs) / 140.0F, 0.0F, 1.0F);
      float smoothAppear = appear * appear * (3.0F - 2.0F * appear);
      if (!this.fadingOut) {
         return 0.58F + smoothAppear * 0.42F;
      }

      float fade = Mth.clamp((float)(now - this.fadeOutStartMs) / 250.0F, 0.0F, 1.0F);
      float smoothFade = fade * fade * (3.0F - 2.0F * fade);
      return (0.58F + smoothAppear * 0.42F) * (1.0F - smoothFade * 0.82F);
   }

   private void renderBox(
      PoseStack matrices, MultiBufferSource.BufferSource consumers, Camera camera, float relX, float relY, float relZ, float alpha, float visualScale, long now
   ) {
      VertexConsumer glowPrimary = consumers.getBuffer(RenderTypes.entityTranslucent(TEXTURE_GLOW));
      VertexConsumer glowSecondary = consumers.getBuffer(RenderTypes.entityTranslucent(TEXTURE_GLOW_SAMPLE));
      float cubeSize = visualScale * 0.38F;
      float timeRotation = (float)(now % 2800L) / 2800.0F * 360.0F;
      float rotY = this.phase + timeRotation;
      float rotX = this.phase * 0.45F + timeRotation * 1.1F;
      float rotZ = this.phase * 0.22F + timeRotation * 0.8F;
      int outerColor = ColorUtil.multiplyAlpha(this.color, 0.15F * alpha);
      int innerColor = ColorUtil.withAlpha(16777215, Math.max(1, Math.round(255.0F * alpha * 0.35F)));
      matrices.pushPose();
      matrices.translate(relX, relY, relZ);
      matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
      matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
      matrices.mulPose(Axis.ZP.rotationDegrees(rotY));
      Pose first = matrices.last();
      ParticleRenderer.drawBillboard(first, glowPrimary, cubeSize, outerColor);
      ParticleRenderer.drawBillboard(first, glowSecondary, cubeSize * 0.56F, innerColor, 0.001F);
      matrices.popPose();
      matrices.pushPose();
      matrices.translate(relX, relY, relZ);
      matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
      matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
      matrices.mulPose(Axis.ZP.rotationDegrees(rotX + 60.0F));
      Pose second = matrices.last();
      ParticleRenderer.drawBillboard(second, glowPrimary, cubeSize * 0.92F, outerColor);
      ParticleRenderer.drawBillboard(second, glowSecondary, cubeSize * 0.52F, innerColor, 0.001F);
      matrices.popPose();
      matrices.pushPose();
      matrices.translate(relX, relY, relZ);
      matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
      matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
      matrices.mulPose(Axis.ZP.rotationDegrees(rotZ - 60.0F));
      Pose third = matrices.last();
      ParticleRenderer.drawBillboard(third, glowPrimary, cubeSize * 0.84F, outerColor);
      ParticleRenderer.drawBillboard(third, glowSecondary, cubeSize * 0.48F, innerColor, 0.001F);
      matrices.popPose();
   }

   private void renderTextured(
      PoseStack matrices, MultiBufferSource.BufferSource consumers, Camera camera, float relX, float relY, float relZ, float alpha, float visualScale
   ) {
      Identifier texture = this.getTexture();
      if (texture != null) {
         VertexConsumer spriteConsumer = consumers.getBuffer(RenderTypes.entityTranslucent(texture));
         VertexConsumer glowPrimary = consumers.getBuffer(RenderTypes.entityTranslucent(TEXTURE_GLOW));
         VertexConsumer glowSecondary = consumers.getBuffer(RenderTypes.entityTranslucent(TEXTURE_GLOW_SAMPLE));
         float spriteSize = visualScale * 0.65F;
         int spriteColor = ColorUtil.multiplyAlpha(this.color, alpha);
         int whiteCoreColor = ColorUtil.withAlpha(16777215, Math.max(1, Math.round(255.0F * alpha * 0.28F)));
         matrices.pushPose();
         matrices.translate(relX, relY, relZ);
         matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
         matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
         if (this.spinning) {
            matrices.mulPose(Axis.ZP.rotationDegrees(this.rotation));
         }

         Pose entry = matrices.last();
         ParticleRenderer.drawBillboard(entry, spriteConsumer, spriteSize, spriteColor);
         if (this.hasWhiteCore()) {
            ParticleRenderer.drawBillboard(entry, spriteConsumer, spriteSize * 0.42F, whiteCoreColor, 0.001F);
         }

         matrices.popPose();
         matrices.pushPose();
         matrices.translate(relX, relY, relZ);
         matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
         matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
         Pose glowEntry = matrices.last();
         switch (this.glowMode) {
            case BLOOM:
               ParticleRenderer.drawBillboard(glowEntry, glowPrimary, spriteSize * 1.35F, ColorUtil.multiplyAlpha(this.color, 0.22F * alpha));
               break;
            case BLOOM_SAMPLE:
               ParticleRenderer.drawBillboard(glowEntry, glowSecondary, spriteSize * 0.8F, ColorUtil.multiplyAlpha(this.color, 0.42F * alpha), 0.001F);
               break;
            case BOTH:
               ParticleRenderer.drawBillboard(glowEntry, glowPrimary, spriteSize * 1.35F, ColorUtil.multiplyAlpha(this.color, 0.22F * alpha));
               ParticleRenderer.drawBillboard(glowEntry, glowSecondary, spriteSize * 0.8F, ColorUtil.multiplyAlpha(this.color, 0.42F * alpha), 0.001F);
         }

         matrices.popPose();
      }
   }

   private Identifier getTexture() {
      return switch (this.actualMode) {
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

   private boolean hasWhiteCore() {
      return switch (this.actualMode) {
         case HEART, LIGHT, SNOW, STAR_v2 -> false;
         default -> true;
      };
   }

   private boolean isHit(double px, double py, double pz) {
      if (mc.level == null) {
         return false;
      }

      BlockPos pos = BlockPos.containing(px, py, pz);
      return mc.level.getBlockState(pos).isCollisionShapeFullBlock(mc.level, pos);
   }

   public enum GlowMode {
      BLOOM,
      BLOOM_SAMPLE,
      BOTH;
   }

   public enum ParticleMode {
      BOX,
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
