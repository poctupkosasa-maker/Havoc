package havoc.module.impl.visuals;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.opengl.GlStateManager;
import havoc.event.impl.EventScreenRender;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.impl.visuals.worldparticles.Particle;
import havoc.module.impl.visuals.worldparticles.ParticleSpawner;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.ColorSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import havoc.render.RenderUtil;
import havoc.render.RenderUtil3D;
import havoc.utilites.color.ClientColors;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

@ModuleInfo(name = "WorldParticles", desc = "", category = Category.VISUALS)
public final class WorldParticles extends Module {
   private static final double MAX_RENDER_DISTANCE_SQ = 22500.0;
   private static final int MAX_RENDERED_PARTICLES = 320;
   private static final int FULL_DETAIL_PARTICLES = 160;
   private final SelectSetting mode = this.registerSetting(
      new SelectSetting("Mode", "Star", List.of("Box", "Heart", "Litght", "Snow", "Star", "Star v2", "Cross", "Glow", "Random"))
   );
   private final SliderSetting particleCount = this.registerSetting(new SliderSetting("Count", 100.0F, 10.0F, 500.0F, 1.0F));
   private final SliderSetting lifeTime = this.registerSetting(new SliderSetting("Lifetime", 10.0F, 2.0F, 60.0F, 0.5F, "s"));
   private final SliderSetting size = this.registerSetting(new SliderSetting("Size", 1.2F, 0.1F, 1.5F, 0.05F));
   private final SliderSetting fallSpeed = this.registerSetting(new SliderSetting("Fall Speed", 1.0F, 0.0F, 5.0F, 0.1F));
   private final BooleanSetting physics = this.registerSetting(new BooleanSetting("Physics", true));
   private final BooleanSetting spawnFromGround = this.registerSetting(new BooleanSetting("Spawn From Ground", false));
   private final ColorSetting color = this.registerSetting(new ColorSetting("Color", ClientColors.CATEGORY_ACCENT));
   private final List<Particle> particles = new ArrayList<>();
   private Vec3 lastPlayerPos;
   private Vec3 playerVelocity = Vec3.ZERO;
   private double playerSpeed;
   private long lastSpawnAtMs;
   private Particle.ParticleType lastParticleType = Particle.ParticleType.STAR;
   private float lastParticleSize = this.size.getValue();
   private float lastFallSpeed = this.fallSpeed.getValue();
   private boolean lastSpawnFromGround = this.spawnFromGround.isValue();
   private boolean lastPhysics = this.physics.isValue();
   private boolean waitingForFadeOut;

   @Override
   public void onDisable() {
      this.resetState();
      super.onDisable();
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (!this.isWorldReady()) {
         this.resetState();
      } else {
         long now = System.currentTimeMillis();
         Vec3 playerPos = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
         Particle.ParticleType currentType = this.resolveParticleType();
         this.updatePlayerMotion(playerPos);
         this.refreshVisualMode(currentType);
         this.refreshParameterChanges();
         this.updateParticles(playerPos, now);
         this.spawnParticles(playerPos, now, currentType);
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
         int baseColor = this.color.getColor();
         float glowScale = 2.2F;
         long now = System.currentTimeMillis();
         GlStateManager._enableBlend(0);
         GlStateManager._blendFuncSeparate(770, 32772, 770, 32772);

         try {
            int start = Math.max(0, this.particles.size() - 320);
            boolean fullDetail = this.particles.size() <= 160;

            for (int index = start; index < this.particles.size(); index++) {
               Particle particle = this.particles.get(index);
               if (particle.getActualType() == Particle.ParticleType.CUBE_3D) {
                  this.renderCube2D(context, particle, tickDelta, cameraPos, screenWidth, screenHeight, baseColor, glowScale, now);
               } else {
                  this.renderParticle2D(context, particle, tickDelta, cameraPos, screenWidth, screenHeight, baseColor, glowScale, now, fullDetail);
               }
            }
         } finally {
            GlStateManager._blendFuncSeparate(770, 771, 770, 771);
            GlStateManager._disableBlend(0);
         }
      }
   }

   private void updatePlayerMotion(Vec3 playerPos) {
      if (this.lastPlayerPos == null) {
         this.playerVelocity = Vec3.ZERO;
         this.playerSpeed = 0.0;
      } else {
         this.playerVelocity = playerPos.subtract(this.lastPlayerPos);
         this.playerSpeed = this.playerVelocity.horizontalDistance();
      }

      this.lastPlayerPos = playerPos;
   }

   private void updateParticles(Vec3 playerPos, long now) {
      double despawnDistanceSq = ParticleSpawner.getDespawnDistanceSquared();

      for (Particle particle : this.particles) {
         particle.update(mc.level, now);
         if (!particle.isFadingOut() && particle.getHorizontalDistanceSquaredTo(playerPos) > despawnDistanceSq) {
            particle.startFadeOut(now);
         }
      }

      this.particles.removeIf(Particle::shouldRemove);
   }

   private void spawnParticles(Vec3 playerPos, long now, Particle.ParticleType particleType) {
      if (this.waitingForFadeOut) {
         if (!this.particles.isEmpty()) {
            return;
         }

         this.waitingForFadeOut = false;
      }

      int maxCount = Math.max(1, Math.round(this.particleCount.getValue()));
      if (this.particles.size() < maxCount) {
         int spawnDelay = ParticleSpawner.calculateSpawnDelay(this.playerSpeed);
         if (now - this.lastSpawnAtMs >= spawnDelay) {
            int spawnCount = ParticleSpawner.calculateSpawnCount(this.playerSpeed, this.particles.size(), maxCount);
            if (spawnCount > 0) {
               long particleLifeTimeMs = (long)(this.lifeTime.getValue() * 1000.0F);
               float particleSize = this.size.getValue();
               float currentFallSpeed = this.fallSpeed.getValue();
               boolean useGroundSpawn = this.spawnFromGround.isValue();
               boolean usePhysics = this.physics.isValue();

               for (int index = 0; index < spawnCount && this.particles.size() < maxCount; index++) {
                  Particle particle = ParticleSpawner.createParticle(
                        playerPos, this.playerVelocity, this.playerSpeed, particleLifeTimeMs, particleType, useGroundSpawn
                     )
                     .setFallSpeedMultiplier(currentFallSpeed)
                     .setPhysics(usePhysics)
                     .setSize(particleSize);
                  this.particles.add(particle);
               }

               this.lastSpawnAtMs = now;
            }
         }
      }
   }

   private void refreshVisualMode(Particle.ParticleType currentType) {
      if (currentType != this.lastParticleType) {
         this.startFadeOutAll();
         this.lastParticleType = currentType;
      }
   }

   private void refreshParameterChanges() {
      float currentSize = this.size.getValue();
      float currentFallSpeed = this.fallSpeed.getValue();
      boolean currentSpawnFromGround = this.spawnFromGround.isValue();
      boolean currentPhysics = this.physics.isValue();
      if (Math.abs(currentSize - this.lastParticleSize) > 0.001F
         || Math.abs(currentFallSpeed - this.lastFallSpeed) > 0.001F
         || currentSpawnFromGround != this.lastSpawnFromGround
         || currentPhysics != this.lastPhysics) {
         this.startFadeOutAll();
         this.lastParticleSize = currentSize;
         this.lastFallSpeed = currentFallSpeed;
         this.lastSpawnFromGround = currentSpawnFromGround;
         this.lastPhysics = currentPhysics;
      }
   }

   private void startFadeOutAll() {
      long now = System.currentTimeMillis();

      for (Particle particle : this.particles) {
         particle.startFadeOut(now);
      }

      this.waitingForFadeOut = !this.particles.isEmpty();
      this.lastSpawnAtMs = 0L;
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
      boolean fullDetail
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
                  float glowPixels = drawSize * glowScale * 0.85F;
                  float corePixels = drawSize * 0.38F;
                  float drawX = screenPos.x - drawSize * 0.5F;
                  float drawY = screenPos.y - drawSize * 0.5F;
                  int particleColor = particle.getColor(baseColor, false, false);
                  int whiteCoreColor = 1728053247;
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
                  RenderUtil.drawImage(context, particle.getTexture(), drawX, drawY, drawSize, drawSize, 0.0F, particleColor, 1.0F);
                  if (fullDetail && particle.hasWhiteCore()) {
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
      long now
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
                  Vec3 rotation = particle.getCubeRotation(now);
                  float shiftX = (float)Math.cos(Math.toRadians(rotation.y)) * outerSize * 0.22F;
                  float shiftY = (float)Math.sin(Math.toRadians(rotation.x + 35.0)) * outerSize * 0.18F;
                  int outerColor = this.withAlpha(baseColor, alpha * 0.15F);
                  int innerColor = this.withAlpha(baseColor, alpha * 0.5F);
                  int outerFillColor = this.withAlpha(baseColor, alpha * 0.35F);
                  int innerFillColor = this.withAlpha(baseColor, alpha * 0.5F);
                  int glowColor = this.withAlpha(baseColor, alpha * 0.18F);
                  float glowSizePx = outerSize * glowScale * 0.95F;
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

   private boolean shouldRender2D(EventScreenRender event) {
      return event != null
         && event.getStage() == EventScreenRender.Stage.ESP
         && !this.particles.isEmpty()
         && this.isWorldReady()
         && RenderUtil3D.hasFrame()
         && event.getContext() != null;
   }

   private boolean isWorldReady() {
      return mc != null && mc.player != null && mc.level != null;
   }

   private void resetState() {
      this.particles.clear();
      this.lastPlayerPos = null;
      this.playerVelocity = Vec3.ZERO;
      this.playerSpeed = 0.0;
      this.lastSpawnAtMs = 0L;
      this.lastParticleType = this.resolveParticleType();
      this.lastParticleSize = this.size.getValue();
      this.lastFallSpeed = this.fallSpeed.getValue();
      this.lastSpawnFromGround = this.spawnFromGround.isValue();
      this.lastPhysics = this.physics.isValue();
      this.waitingForFadeOut = false;
   }
}
