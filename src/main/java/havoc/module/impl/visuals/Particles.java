package havoc.module.impl.visuals;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.opengl.GlStateManager;
import havoc.event.impl.EventAttackEntity;
import havoc.event.impl.EventRender3D;
import havoc.event.impl.EventScreenRender;
import havoc.event.impl.EventTotemPop;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.impl.visuals.particles.TotemEmitter;
import havoc.module.impl.visuals.worldparticles.Particle;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.MultiSelectSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import havoc.render.RenderUtil;
import havoc.render.RenderUtil3D;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

@ModuleInfo(name = "Particles", desc = "", category = Category.VISUALS)
public final class Particles extends Module {
   private static final double MAX_RENDER_DISTANCE_SQ = 22500.0;
   private static final int MAX_ACTIVE_PARTICLES = 480;
   private static final int MAX_RENDERED_PARTICLES = 220;
   private static final int FULL_DETAIL_PARTICLES = 110;
   private static final String MODE_BOX = "Box";
   private static final String MODE_HEART = "Heart";
   private static final String MODE_LIGHTNING = "Litght";
   private static final String MODE_SNOWFLAKE = "Snow";
   private static final String MODE_STAR = "Star";
   private static final String MODE_STAR_2 = "Star v2";
   private static final String MODE_CROSS = "Cross";
   private static final String MODE_GLOW = "Glow";
   private static final String MODE_RANDOM = "Random";
   private static final String TRIGGER_ATTACK = "Attack";
   private static final String TRIGGER_TOTEM = "Totem";
   private static final String TRIGGER_WALK = "Walk";
   private static final String TRIGGER_PROJECTILE = "Projectile";
   private static final int TOTEM_DURATION = 20;
   private final SelectSetting mode = this.registerSetting(
      new SelectSetting("Mode", "Star", List.of("Box", "Heart", "Litght", "Snow", "Star", "Star v2", "Cross", "Glow", "Random"))
   );
   private final MultiSelectSetting triggers = this.registerSetting(
      new MultiSelectSetting("Triggers", List.of("Attack", "Totem", "Walk", "Projectile"), new LinkedHashSet<>(Set.of("Attack", "Totem", "Walk", "Projectile")))
   );
   private final SliderSetting amount = this.registerSetting(
      new SliderSetting("Amount", 40.0F, 10.0F, 40.0F, 1.0F).visibleWhen(() -> this.triggers.isSelected("Attack"))
   );
   private final SliderSetting walkAmount = this.registerSetting(
      new SliderSetting("Walk Amount", 30.0F, 10.0F, 30.0F, 1.0F).visibleWhen(() -> this.triggers.isSelected("Walk"))
   );
   private final SliderSetting speed = this.registerSetting(new SliderSetting("Speed", 2.0F, 0.1F, 3.0F, 0.1F));
   private final SliderSetting lifeTime = this.registerSetting(new SliderSetting("Lifetime", 2.5F, 0.5F, 10.0F, 0.1F, "s"));
   private final SliderSetting size = this.registerSetting(new SliderSetting("Size", 1.0F, 0.1F, 1.5F, 0.05F));
   private final BooleanSetting shaderGlow = this.registerSetting(new BooleanSetting("Shader Glow", true));
   private final List<Particle> particles = new ArrayList<>();
   private final List<TotemEmitter> totemEmitters = new ArrayList<>();
   private final Set<Particle> walkParticles = new HashSet<>();
   private float walkParticleAccumulator;

   @Override
   public void onDisable() {
      this.particles.clear();
      this.totemEmitters.clear();
      this.walkParticles.clear();
      this.walkParticleAccumulator = 0.0F;
      super.onDisable();
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.level != null) {
         if (this.triggers.isSelected("Walk")) {
            this.handleWalkParticles();
         }

         if (this.triggers.isSelected("Projectile")) {
            this.handleProjectileParticles();
         }

         Iterator<TotemEmitter> emitterIterator = this.totemEmitters.iterator();

         while (emitterIterator.hasNext()) {
            TotemEmitter emitter = emitterIterator.next();
            emitter.tick();
            if (emitter.isAlive()) {
               this.spawnTotemParticlesBurst(emitter.getEntity(), emitter.getProgress());
            } else {
               emitterIterator.remove();
            }
         }

         long now = System.currentTimeMillis();
         Iterator<Particle> iterator = this.particles.iterator();

         while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.update(mc.level, now);
            if (particle.shouldRemove()) {
               this.walkParticles.remove(particle);
               iterator.remove();
            }
         }

         this.trimParticleBudget();
      }
   }

   @Subscribe
   public void onAttack(EventAttackEntity event) {
      if (this.triggers.isSelected("Attack") && event != null && event.getTarget() != null) {
         Entity target = event.getTarget();
         float spreadValue = 0.15F;
         float speedMultiplier = this.speed.getValue();

         for (int i = 0; i < Math.round(this.amount.getValue()); i++) {
            Vec3 position = new Vec3(target.getX(), target.getY() + Math.random() * target.getBbHeight(), target.getZ());
            Vec3 velocity = new Vec3(
               (Math.random() - 0.5) * 2.0 * spreadValue * speedMultiplier,
               (Math.random() - 0.5) * 2.0 * spreadValue * speedMultiplier,
               (Math.random() - 0.5) * 2.0 * spreadValue * speedMultiplier
            );
            this.particles.add(this.createParticle(position, velocity, this.lifeTime.getValue(), this.size.getValue(), true));
         }
      }
   }

   @Subscribe
   public void onTotemPop(EventTotemPop event) {
      if (this.triggers.isSelected("Totem") && event != null && event.getEntity() != null) {
         this.totemEmitters.add(new TotemEmitter(event.getEntity(), 20));
      }
   }

   @Subscribe
   public void onScreenRender(EventScreenRender event) {
      if (this.shouldRender2D(event)) {
         GuiGraphicsExtractor context = event.getContext();
         float tickDelta = event.getTickCounter() == null ? RenderUtil3D.getFrameTickDelta() : event.getTickCounter().getGameTimeDeltaPartialTick(false);
         Vec3 cameraPos = mc.gameRenderer.mainCamera().position();
         int screenWidth = mc.getWindow().getGuiScaledWidth();
         int screenHeight = mc.getWindow().getGuiScaledHeight();
         int firstThemeColor = ClientColors.CATEGORY_ACCENT;
         int secondThemeColor = ClientColors.CATEGORY_ACCENT_SECOND;
         float glowScale = 2.05F;
         long now = System.currentTimeMillis();
         GlStateManager._enableBlend(0);
         GlStateManager._blendFuncSeparate(770, 32772, 770, 32772);

         try {
            int start = Math.max(0, this.particles.size() - 220);
            int renderedCount = this.particles.size() - start;
            boolean fullDetail = renderedCount <= 110;

            for (int index = start; index < this.particles.size(); index++) {
               Particle particle = this.particles.get(index);
               float themeMix = (index - start) % 7 / 6.0F;
               int baseColor = ColorUtil.interpolate(firstThemeColor, secondThemeColor, themeMix);
               boolean renderGlow = this.shaderGlow.isValue() && (fullDetail || (index - start & 1) == 0);
               if (particle.getActualType() != Particle.ParticleType.CUBE_3D) {
                  this.renderParticle2D(context, particle, tickDelta, cameraPos, screenWidth, screenHeight, baseColor, glowScale, now, fullDetail, renderGlow);
               }
            }
         } finally {
            GlStateManager._blendFuncSeparate(770, 771, 770, 771);
            GlStateManager._disableBlend(0);
         }
      }
   }

   private void handleWalkParticles() {
      double deltaX = mc.player.getX() - mc.player.xo;
      double deltaZ = mc.player.getZ() - mc.player.zo;
      double horizontalMovementSq = deltaX * deltaX + deltaZ * deltaZ;
      Vec3 playerVelocity = mc.player.getDeltaMovement();
      if (horizontalMovementSq <= 2.5E-6 && playerVelocity != null) {
         deltaX = playerVelocity.x;
         deltaZ = playerVelocity.z;
         horizontalMovementSq = deltaX * deltaX + deltaZ * deltaZ;
      }

      boolean isMoving = horizontalMovementSq > 2.5E-6;
      if (!isMoving) {
         this.walkParticleAccumulator = 0.0F;
      } else {
         float particlesPerTick = this.walkAmount.getValue() / 20.0F;
         this.walkParticleAccumulator += particlesPerTick;
         int particlesToSpawn = (int)this.walkParticleAccumulator;
         this.walkParticleAccumulator -= particlesToSpawn;
         if (particlesToSpawn > 0) {
            double movementLength = Math.sqrt(horizontalMovementSq);
            double offsetX = deltaX / movementLength * 0.55;
            double offsetZ = deltaZ / movementLength * 0.55;
            float spreadValue = 0.05F;
            float speedMultiplier = this.speed.getValue();

            for (int i = 0; i < particlesToSpawn; i++) {
               Vec3 position = new Vec3(
                  mc.player.getX() - offsetX + (Math.random() - 0.5) * 0.3,
                  mc.player.getY() + 0.08 + Math.random() * 0.42,
                  mc.player.getZ() - offsetZ + (Math.random() - 0.5) * 0.3
               );
               Vec3 velocity = new Vec3(
                  (Math.random() - 0.5) * spreadValue * speedMultiplier,
                  (Math.random() - 0.5) * spreadValue * 0.5 * speedMultiplier,
                  (Math.random() - 0.5) * spreadValue * speedMultiplier
               );
               Particle particle = this.createParticle(position, velocity, this.lifeTime.getValue() * 0.5F, this.size.getValue() * 0.6F, true);
               this.particles.add(particle);
               this.walkParticles.add(particle);
            }
         }
      }
   }

   private void handleProjectileParticles() {
      float spreadValue = 0.03F;
      float speedMultiplier = this.speed.getValue();

      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof ThrowableProjectile || entity instanceof Arrow || entity instanceof ThrownTrident) {
            Projectile projectile = (Projectile)entity;
            boolean isMoving = Math.abs(projectile.getX() - projectile.xo) > 0.01
               || Math.abs(projectile.getY() - projectile.yo) > 0.01
               || Math.abs(projectile.getZ() - projectile.zo) > 0.01;
            if (isMoving || !(projectile.getDeltaMovement().lengthSqr() <= 0.01)) {
               for (int i = 0; i < 2; i++) {
                  Vec3 position = new Vec3(
                     projectile.getX() + (Math.random() - 0.5) * 0.5,
                     projectile.getY() + Math.random() * projectile.getBbHeight(),
                     projectile.getZ() + (Math.random() - 0.5) * 0.5
                  );
                  Vec3 velocity = new Vec3(
                     (Math.random() - 0.5) * 2.0 * spreadValue * speedMultiplier,
                     (Math.random() - 0.5) * 2.0 * spreadValue * speedMultiplier,
                     (Math.random() - 0.5) * 2.0 * spreadValue * speedMultiplier
                  );
                  this.particles.add(this.createParticle(position, velocity, this.lifeTime.getValue() * 0.3F, this.size.getValue() * 0.5F, true));
               }
            }
         }
      }
   }

   private void spawnTotemParticlesBurst(Entity entity, float progress) {
      if (entity != null && !entity.isRemoved()) {
         float spreadMultiplier = 1.0F - progress * 0.5F;
         float spreadValue = 1.0F;
         float speedMultiplier = this.speed.getValue();

         for (int i = 0; i < 4; i++) {
            double dx = Math.random() * 2.0 - 1.0;
            double dy = Math.random() * 2.0 - 1.0;
            double dz = Math.random() * 2.0 - 1.0;
            if (!(dx * dx + dy * dy + dz * dz > 1.0)) {
               Vec3 position = new Vec3(
                  entity.getX() + dx * entity.getBbWidth() * 0.5,
                  entity.getY(0.5) + dy * entity.getBbHeight() * 0.5,
                  entity.getZ() + dz * entity.getBbWidth() * 0.5
               );
               double velocityScale = spreadValue * 0.18 * spreadMultiplier * speedMultiplier;
               double initialUpward = Math.random() < 0.4 ? (0.15 + Math.random() * 0.2) * speedMultiplier : (0.03 + Math.random() * 0.07) * speedMultiplier;
               Vec3 velocity = new Vec3(dx * velocityScale, initialUpward, dz * velocityScale);
               Particle particle = new Particle(
                     position.x, position.y, position.z, velocity.x, velocity.y, velocity.z, (long)(this.lifeTime.getValue() * 0.8F * 1000.0F)
                  )
                  .setType(this.resolveParticleType())
                  .setPhysics(true)
                  .setFallSpeedMultiplier(1.0F)
                  .setSize(this.size.getValue() * 0.8F);
               this.particles.add(particle);
            }
         }
      }
   }

   private Particle createParticle(Vec3 position, Vec3 velocity, float lifeSeconds, float particleSize, boolean physics) {
      return new Particle(position.x, position.y, position.z, velocity.x, velocity.y, velocity.z, (long)(lifeSeconds * 1000.0F))
         .setType(this.resolveParticleType())
         .setPhysics(physics)
         .setFallSpeedMultiplier(1.0F)
         .setSize(particleSize);
   }

   private Particle.ParticleType resolveParticleType() {
      return switch (this.mode.getValue()) {
         case "Box" -> Particle.ParticleType.CUBE_3D;
         case "Heart" -> Particle.ParticleType.HEART;
         case "Litght" -> Particle.ParticleType.LIGHT;
         case "Snow" -> Particle.ParticleType.SNOW;
         case "Star" -> Particle.ParticleType.STAR;
         case "Star v2" -> Particle.ParticleType.STAR_v2;
         case "Cross" -> Particle.ParticleType.CROSS;
         case "Glow" -> Particle.ParticleType.GLOW;
         case "Random" -> Particle.ParticleType.RANDOM;
         default -> Particle.ParticleType.STAR;
      };
   }

   private void renderParticle2D(
      GuiGraphicsExtractor context,
      Particle particle,
      float tickDelta,
      Vec3 cameraPos,
      int screenWidth,
      int screenHeight,
      int baseColor,
      float glowScale,
      long now,
      boolean fullDetail,
      boolean renderGlow
   ) {
      double distanceSq = particle.getDistanceSquaredTo(cameraPos);
      if (!(distanceSq > 22500.0)) {
         Vec3 renderPos = particle.getInterpolatedPos(tickDelta);
         Vec2 screenPos = RenderUtil3D.worldToScreen(renderPos.x, renderPos.y, renderPos.z, screenWidth, screenHeight);
         if (screenPos != null
            && !(screenPos.x < -48.0F)
            && !(screenPos.y < -48.0F)
            && !(screenPos.x > screenWidth + 48.0F)
            && !(screenPos.y > screenHeight + 48.0F)) {
            if (this.isParticleVisible(particle, cameraPos, renderPos, now)) {
               float alpha = particle.getAlpha();
               if (!(alpha <= 0.01F)) {
                  float renderScale = particle.getRenderScale(now);
                  float distance = (float)Math.sqrt(Math.max(1.0E-4, distanceSq));
                  float distanceScale = Mth.clamp(1.6F - distance / 22.0F, 0.45F, 1.45F);
                  float drawSize = particle.getSize() * 22.0F * distanceScale * renderScale;
                  float darknessMultiplier = this.darknessGlowMultiplier(renderPos, now);
                  float glowPixels = drawSize * glowScale * darknessMultiplier * 0.85F;
                  float corePixels = drawSize * 0.38F;
                  float drawX = screenPos.x - drawSize * 0.5F;
                  float drawY = screenPos.y - drawSize * 0.5F;
                  int particleColor = particle.getColor(baseColor, false, false);
                  int whiteCoreColor = 1728053247;
                  if (renderGlow) {
                     if (darknessMultiplier > 1.04F) {
                        float outerGlow = glowPixels * 1.28F;
                        RenderUtil.drawGlow(
                           context,
                           screenPos.x - outerGlow * 0.5F,
                           screenPos.y - outerGlow * 0.5F,
                           outerGlow,
                           outerGlow,
                           outerGlow * 0.32F,
                           ColorUtil.multiplyAlpha(particleColor, Math.min(0.36F, (darknessMultiplier - 1.0F) * 0.2F)),
                           1.0F,
                           1.75F
                        );
                     }

                     RenderUtil.drawGlow(
                        context,
                        screenPos.x - glowPixels * 0.5F,
                        screenPos.y - glowPixels * 0.5F,
                        glowPixels,
                        glowPixels,
                        glowPixels * 0.28F,
                        particleColor,
                        1.0F,
                        1.4F
                     );
                  }

                  RenderUtil.drawImage(context, particle.getTexture(), drawX, drawY, drawSize, drawSize, 0.0F, particleColor, 1.0F);
                  if (fullDetail && particle.hasWhiteCore()) {
                     if (renderGlow) {
                        RenderUtil.drawGlow(
                           context,
                           screenPos.x - corePixels * 0.5F,
                           screenPos.y - corePixels * 0.5F,
                           corePixels,
                           corePixels,
                           corePixels * 0.45F,
                           whiteCoreColor,
                           1.0F,
                           0.9F
                        );
                     }

                     RenderUtil.drawImage(
                        context,
                        particle.getTexture(),
                        screenPos.x - corePixels * 0.5F,
                        screenPos.y - corePixels * 0.5F,
                        corePixels,
                        corePixels,
                        0.0F,
                        whiteCoreColor,
                        1.0F
                     );
                  }
               }
            }
         }
      }
   }

   private void renderCube2D(
      GuiGraphicsExtractor context,
      Particle particle,
      float tickDelta,
      Vec3 cameraPos,
      int screenWidth,
      int screenHeight,
      int baseColor,
      float glowScale,
      long now,
      boolean renderGlow
   ) {
      double distanceSq = particle.getDistanceSquaredTo(cameraPos);
      if (!(distanceSq > 22500.0)) {
         Vec3 center = particle.getInterpolatedPos(tickDelta);
         float alpha = particle.getAlpha();
         if (!(alpha <= 0.01F)) {
            Vec2 centerScreen = RenderUtil3D.worldToScreen(center.x, center.y, center.z, screenWidth, screenHeight);
            if (centerScreen != null
               && !(centerScreen.x < -48.0F)
               && !(centerScreen.y < -48.0F)
               && !(centerScreen.x > screenWidth + 48.0F)
               && !(centerScreen.y > screenHeight + 48.0F)) {
               if (this.isParticleVisible(particle, cameraPos, center, now)) {
                  float renderScale = particle.getRenderScale(now);
                  float distance = (float)Math.sqrt(Math.max(1.0E-4, distanceSq));
                  float distanceScale = Mth.clamp(1.5F - distance / 24.0F, 0.4F, 1.35F);
                  float outerSize = particle.getSize() * 20.0F * distanceScale * renderScale;
                  float innerSize = outerSize * 0.56F;
                  float darknessMultiplier = this.darknessGlowMultiplier(center, now);
                  Vec3 rotation = particle.getCubeRotation(now);
                  float shiftX = (float)Math.cos(Math.toRadians(rotation.y)) * outerSize * 0.22F;
                  float shiftY = (float)Math.sin(Math.toRadians(rotation.x + 35.0)) * outerSize * 0.18F;
                  int outerColor = this.withAlpha(baseColor, alpha * 0.15F);
                  int innerColor = this.withAlpha(baseColor, alpha * 0.5F);
                  int outerFillColor = this.withAlpha(baseColor, alpha * 0.35F);
                  int innerFillColor = this.withAlpha(baseColor, alpha * 0.5F);
                  int glowColor = this.withAlpha(baseColor, alpha * 0.18F);
                  float glowSizePx = outerSize * glowScale * darknessMultiplier * 0.95F;
                  if (renderGlow) {
                     if (darknessMultiplier > 1.04F) {
                        float outerGlow = glowSizePx * 1.25F;
                        RenderUtil.drawGlow(
                           context,
                           centerScreen.x - outerGlow * 0.5F,
                           centerScreen.y - outerGlow * 0.5F,
                           outerGlow,
                           outerGlow,
                           outerGlow * 0.32F,
                           ColorUtil.multiplyAlpha(baseColor, Math.min(0.3F, (darknessMultiplier - 1.0F) * 0.16F)),
                           1.0F,
                           1.6F
                        );
                     }

                     RenderUtil.drawGlow(
                        context,
                        centerScreen.x - glowSizePx * 0.5F,
                        centerScreen.y - glowSizePx * 0.5F,
                        glowSizePx,
                        glowSizePx,
                        glowSizePx * 0.28F,
                        glowColor,
                        1.0F,
                        1.1F
                     );
                  }

                  this.drawCubeFrame(context, centerScreen.x, centerScreen.y, outerSize, shiftX, shiftY, outerColor, outerFillColor);
                  this.drawCubeFrame(context, centerScreen.x, centerScreen.y, innerSize, shiftX * 0.56F, shiftY * 0.56F, innerColor, innerFillColor);
               }
            }
         }
      }
   }

   private void drawCubeFrame(
      GuiGraphicsExtractor context, float centerX, float centerY, float size, float shiftX, float shiftY, int borderColor, int fillColor
   ) {
      float half = size * 0.5F;
      float frontX = centerX - half;
      float frontY = centerY - half;
      float backX = frontX + shiftX;
      float backY = frontY + shiftY;
      float radius = Math.max(0.5F, size * 0.04F);
      RenderUtil.drawRoundRect(context, frontX, frontY, size, size, radius, fillColor, 1.0F);
      RenderUtil.drawRoundRect(context, backX, backY, size, size, radius, fillColor, 1.0F);
      RenderUtil.drawBorder(context, frontX, frontY, size, size, radius, borderColor, 1.0F, 0.0F, 0.6F);
      RenderUtil.drawBorder(context, backX, backY, size, size, radius, borderColor, 1.0F, 0.0F, 0.6F);
   }

   private int withAlpha(int color, float alphaFactor) {
      int alpha = Math.max(1, Math.min(255, Math.round(255.0F * alphaFactor)));
      return alpha << 24 | color & 16777215;
   }

   private float darknessGlowMultiplier(Vec3 position, long now) {
      if (this.shaderGlow.isValue() && mc != null && mc.level != null && position != null) {
         int light = mc.level.getMaxLocalRawBrightness(BlockPos.containing(position));
         float darkness = 1.0F - Mth.clamp(light / 15.0F, 0.0F, 1.0F);
         float pulse = 0.94F + 0.06F * (float)Math.sin(now * 0.005);
         return 1.0F + darkness * darkness * 1.4F * pulse;
      } else {
         return 1.0F;
      }
   }

   private boolean isParticleVisible(Particle particle, Vec3 cameraPos, Vec3 targetPos, long now) {
      if (mc == null || mc.level == null || mc.player == null) {
         return false;
      }

      if (!particle.needsVisibilityRefresh(cameraPos, targetPos, now)) {
         return particle.isCachedVisible();
      }

      HitResult blockHit = mc.level.clip(new ClipContext(cameraPos, targetPos, Block.COLLIDER, Fluid.NONE, mc.player));
      boolean visible = blockHit.getType() == Type.MISS || cameraPos.distanceToSqr(blockHit.getLocation()) + 0.002 >= cameraPos.distanceToSqr(targetPos);
      particle.updateVisibilityCache(cameraPos, targetPos, now, visible);
      return visible;
   }

   private void trimParticleBudget() {
      int overflow = this.particles.size() - 480;
      if (overflow > 0) {
         this.particles.subList(0, overflow).clear();
         this.walkParticles.retainAll(this.particles);
      }
   }

   private boolean shouldRender2D(EventScreenRender event) {
      return event != null
         && event.getStage() == EventScreenRender.Stage.ESP
         && !this.particles.isEmpty()
         && mc != null
         && mc.player != null
         && mc.level != null
         && mc.gameRenderer != null
         && RenderUtil3D.hasFrame()
         && event.getContext() != null;
   }

   public boolean shouldHideVanillaTotemParticles() {
      return this.isEnable() && this.triggers.isSelected("Totem");
   }

   @Subscribe
   public void onRender3D(EventRender3D event) {
      if (event != null && mc.player != null && mc.level != null && "Box".equals(this.mode.getValue())) {
         int baseColor = ClientColors.CATEGORY_ACCENT;

         for (Particle particle : this.particles) {
            if (particle.getActualType() == Particle.ParticleType.CUBE_3D) {
               particle.render(
                  event.getMatrices(),
                  event.getConsumers(),
                  mc.gameRenderer.mainCamera(),
                  event.getCameraPos(),
                  event.getTickDelta(),
                  baseColor,
                  1.0F,
                  false,
                  false,
                  true
               );
            }
         }

         event.getConsumers().endBatch();
      }
   }
}
