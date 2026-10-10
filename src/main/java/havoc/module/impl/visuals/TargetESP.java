package havoc.module.impl.visuals;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import havoc.event.impl.EventRender3D;
import havoc.event.impl.EventScreenRender;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import havoc.render.RenderUtil;
import havoc.render.RenderUtil3D;
import havoc.render.TargetEspPipelines;
import havoc.render.font.GFonts;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(name = "TargetESP", desc = "Маркер, кольцо, призраки и скан-брекет цели", category = Category.VISUALS)
public final class TargetESP extends Module {
   private static final String MODE_MARKER = "Маркер";
   private static final String MODE_RING = "Кольцо";
   private static final String MODE_GHOSTS = "Призраки";
   private static final String MODE_BRACKET = "Скан-брекет";
   private static final Identifier GHOST_TEXTURE = Identifier.fromNamespaceAndPath("havoc", "textures/luma/particle/ghost-glow.png");
   private static final int GHOST_LANES = 3;
   private static final int GHOST_TRAIL_PARTICLES = 30;
   private static final int GHOST_HISTORY_SIZE = 256;
   private final SelectSetting mode = this.registerSetting(
      new SelectSetting("Режим", MODE_MARKER, List.of(MODE_MARKER, MODE_RING, MODE_GHOSTS, MODE_BRACKET))
   );
   private final SelectSetting markerSkin = this.registerSetting(
      new SelectSetting("Тип маркера", "Marker", List.of("Marker", "Marker1", "Marker2", "Marker3")).visibleWhen(() -> this.mode.getValue().equals("Маркер"))
   );
   private final SliderSetting markerSize = this.registerSetting(
      new SliderSetting("Размер маркера", 0.55F, 0.2F, 2.0F, 0.05F).visibleWhen(() -> this.mode.getValue().equals("Маркер"))
   );
   private final BooleanSetting hitFlash = this.registerSetting(new BooleanSetting("Подсветка ударов", true));
   private LivingEntity displayedTarget;
   private long animationChangedAt = System.currentTimeMillis();
   private boolean appearing;
   private long epoch = System.currentTimeMillis();
   private long lastFrame = System.currentTimeMillis();
   private float hurtProgress;
   private float circleStep;
   private float circleSpeed;
   private boolean reverseSpeed;
   private double smoothY;
   private double smoothY2;
   private final Vec3[] ghostHistory = new Vec3[256];
   private final long[] ghostHistoryTimes = new long[256];
   private Vec3 ghostAnchor;
   private Vec3 ghostVelocity = Vec3.ZERO;
   private int ghostHistoryCursor = -1;
   private int ghostHistoryCount;
   private LivingEntity ghostTrackedTarget;
   private Vec3 ghostFlightFrom;
   private Vec3 ghostFlightSide = Vec3.ZERO;
   private long ghostFlightStartedAt;
   private long ghostFlightDurationMs;
   private float ghostFlightSideAmount;
   private float ghostFlightArcHeight;
   private boolean ghostSwitching;
   private float ghostFlightStrength;
   private float ghostVisibility;
   private long ghostEpoch = System.currentTimeMillis();
   private Object ghostWorld;
   private static Boolean irisPresent;
   private static Method irisGetInstance;
   private static Method irisShadersInUse;

   @Override
   public void onEnable() {
      this.epoch = System.currentTimeMillis();
      this.ghostEpoch = this.epoch;
      this.lastFrame = this.epoch;
      this.resetGhostMotion();
      this.ghostVisibility = 0.0F;
      this.ghostWorld = mc.level;
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.displayedTarget = null;
      this.smoothY = this.smoothY2 = 0.0;
      this.resetGhostMotion();
      this.ghostVisibility = 0.0F;
      this.ghostWorld = null;
      super.onDisable();
   }

   @Subscribe
   public void onRender(EventRender3D event) {
      if (event != null && mc.player != null && mc.level != null) {
         long now = System.currentTimeMillis();
         if (this.ghostWorld != mc.level) {
            this.displayedTarget = null;
            this.appearing = false;
            this.animationChangedAt = now;
            this.resetGhostMotion();
            this.ghostVisibility = 0.0F;
            this.ghostWorld = mc.level;
         }

         float elapsedSeconds = Mth.clamp((float)(now - this.lastFrame) / 1000.0F, 0.001F, 0.1F);
         float frame = elapsedSeconds * 60.0F;
         this.lastFrame = now;
         LivingEntity target = this.currentTarget();
         this.updateAnimationTarget(target, now);
         float alpha = this.animation(now, elapsedSeconds);
         if (this.displayedTarget != null && !(alpha <= 0.01F)) {
            this.hurtProgress = this.displayedTarget.hurtTime > 0 ? this.displayedTarget.hurtTime / 10.0F : Math.max(0.0F, this.hurtProgress - 0.1F * frame);
            this.circleStep += 0.06F * frame;
            if (this.circleSpeed > 25.0F) {
               this.reverseSpeed = true;
            }

            if (this.circleSpeed < -25.0F) {
               this.reverseSpeed = false;
            }

            this.circleSpeed = this.circleSpeed + (this.reverseSpeed ? -0.5F : 0.5F) * frame;
            this.circleStep = this.circleStep + this.circleSpeed * 8.0E-4F * frame;
            String currentMode = this.mode.getValue();
            if (irisShadersEnabled()) {
               if (currentMode.equals(MODE_GHOSTS)) {
                  this.updateGhostMotion(this.displayedTarget, event.getTickDelta(), elapsedSeconds, now);
               }
            } else {
               if (currentMode.equals(MODE_RING)) {
                  this.renderRing(event, this.displayedTarget, alpha);
               } else if (currentMode.equals(MODE_GHOSTS)) {
                  this.renderGhosts(event, this.displayedTarget, alpha, now, elapsedSeconds);
               } else if (currentMode.equals(MODE_MARKER)) {
                  this.renderMarker(event, this.displayedTarget, alpha, now);
               }
            }
         }
      }
   }

   @Subscribe
   public void onScreenRender(EventScreenRender event) {
      if (event == null || this.displayedTarget == null || mc.player == null) {
         return;
      }
      if (event.getStage() != EventScreenRender.Stage.ESP) {
         return;
      }
      String currentMode = this.mode.getValue();
      boolean overlayOnly = currentMode.equals(MODE_BRACKET);
      if (!irisShadersEnabled() && !overlayOnly) {
         return;
      }
      long now = System.currentTimeMillis();
      float alpha = currentMode.equals(MODE_GHOSTS)
         ? this.ghostVisibility
         : (this.appearing ? 1.0F : 1.0F - this.smoothStep(Mth.clamp((float)(now - this.animationChangedAt) / 300.0F, 0.0F, 1.0F)));
      if (alpha <= 0.01F) {
         return;
      }
      float tickDelta = event.getTickCounter() == null ? RenderUtil3D.getFrameTickDelta() : event.getTickCounter().getGameTimeDeltaPartialTick(false);
      int width = mc.getWindow().getGuiScaledWidth();
      int height = mc.getWindow().getGuiScaledHeight();
      GuiGraphicsExtractor context = event.getContext();
      if (currentMode.equals(MODE_GHOSTS)) {
         this.renderGhostsOverlay(context, this.displayedTarget, alpha, now, width, height);
      } else if (currentMode.equals(MODE_RING)) {
         this.renderRingOverlay(context, this.displayedTarget, alpha, tickDelta, width, height);
      } else if (currentMode.equals(MODE_MARKER)) {
         this.renderMarkerOverlay(context, this.displayedTarget, alpha, tickDelta, now, width, height);
      } else if (currentMode.equals(MODE_BRACKET)) {
         this.renderBracketOverlay(context, this.displayedTarget, alpha, tickDelta, width, height, now);
      }
   }

   private void renderMarkerOverlay(GuiGraphicsExtractor context, LivingEntity target, float alpha, float tickDelta, long now, int width, int height) {
      Vec3 center = this.lerped(target, tickDelta).add(0.0, target.getBbHeight() * 0.5, 0.0);
      Vec2 point = RenderUtil3D.worldToScreen(center.x, center.y, center.z, width, height);
      if (point != null) {
         float time = (float)(now - this.epoch) / 1000.0F;
         float[] parameters = this.markerParameters(this.markerSkin.getValue());
         float size = 34.0F
            * this.markerSize.getValue()
            * (1.0F + parameters[4] * Mth.sin(time * parameters[3] * (float) (Math.PI * 2)))
            * (1.0F + 0.25F * this.hurtProgress);
         int[] palette = this.colors(alpha);
         Identifier texture = Identifier.fromNamespaceAndPath("havoc", "textures/luma/world/" + this.markerSkin.getValue().toLowerCase() + ".png");
         RenderUtil.drawGlow(
            context,
            point.x - size * 0.36F,
            point.y - size * 0.36F,
            size * 0.72F,
            size * 0.72F,
            size * 0.36F,
            ColorUtil.multiplyAlpha(palette[0], 0.28F),
            1.0F,
            size * 0.3F
         );
         RenderUtil.drawRotatedImage(context, texture, point.x - size, point.y - size, size * 2.0F, size * 2.0F, time * 55.0F, 0.0F, palette[1], 1.0F);
      }
   }

   private void renderRingOverlay(GuiGraphicsExtractor context, LivingEntity target, float alpha, float tickDelta, int width, int height) {
      Vec3 center = this.lerped(target, tickDelta);
      float radius = target.getBbWidth();
      int[] palette = this.colors(alpha);

      for (int i = 0; i < 56; i++) {
         double angle = i * (float) (Math.PI * 2) / 56.0;
         Vec3 world = center.add(Math.cos(angle) * radius, this.smoothY, Math.sin(angle) * radius);
         Vec2 point = RenderUtil3D.worldToScreen(world.x, world.y, world.z, width, height);
         if (point != null) {
            int color = ColorUtil.interpolate(palette[0], palette[1], (float)(0.5 - 0.5 * Math.cos(angle)));
            RenderUtil.drawGlow(context, point.x - 3.5F, point.y - 3.5F, 7.0F, 7.0F, 3.5F, ColorUtil.multiplyAlpha(color, 0.52F), 1.0F, 5.5F);
            RenderUtil.drawCircle(context, point.x - 2.1F, point.y - 2.1F, 4.2F, color);
         }
      }
   }

   private void renderGhostsOverlay(GuiGraphicsExtractor context, LivingEntity target, float alpha, long now, int width, int height) {
      int[] palette = this.colors(alpha);
      float time = (float)(now - this.ghostEpoch) / 1000.0F;
      Vec3 targetBase = this.lerped(target, RenderUtil3D.getFrameTickDelta());
      Vec2 targetBottom = RenderUtil3D.worldToScreen(targetBase.x, targetBase.y, targetBase.z, width, height);
      Vec2 targetTop = RenderUtil3D.worldToScreen(targetBase.x, targetBase.y + target.getBbHeight(), targetBase.z, width, height);
      float perspectiveSize = targetBottom != null && targetTop != null ? Mth.clamp(Math.abs(targetBottom.y - targetTop.y) * 0.105F, 7.0F, 25.0F) : 13.0F;

      for (int lane = 0; lane < 3; lane++) {
         float laneOffset = lane * (float) (Math.PI * 2.0 / 3.0);

         for (int particle = 0; particle < 30; particle++) {
            float trail = particle / 29.0F;
            long delay = particle * 24L;
            Vec3 base = this.sampleGhostHistory(now - delay);
            float phase = (float)(now - delay - this.ghostEpoch) / 1000.0F * 1.35F + laneOffset - trail * 0.42F;
            float radius = 0.72F * (1.0F - this.ghostFlightStrength * 0.18F);
            Vec3 world = base.add(
               Mth.sin(phase) * radius, target.getBbHeight() * (0.24F + lane * 0.24F) + Mth.sin(phase * 1.2F) * 0.13F, Mth.cos(phase) * radius
            );
            Vec2 point = RenderUtil3D.worldToScreen(world.x, world.y, world.z, width, height);
            if (point != null) {
               float size = perspectiveSize * (1.0F - trail * 0.58F);
               float tailAlpha = 0.08F + 0.92F * (float)Math.pow(1.0F - trail, 1.45);
               int color = ColorUtil.multiplyAlpha(ColorUtil.interpolate(palette[0], palette[1], 0.5F + 0.5F * Mth.sin(phase)), tailAlpha);
               if (particle == 0) {
                  RenderUtil.drawGlow(
                     context,
                     point.x - size * 0.72F,
                     point.y - size * 0.72F,
                     size * 1.44F,
                     size * 1.44F,
                     size * 0.72F,
                     ColorUtil.multiplyAlpha(color, 0.35F),
                     1.0F,
                     size * 1.45F
                  );
               }

               RenderUtil.drawRotatedImage(
                  context,
                  GHOST_TEXTURE,
                  point.x - size * 1.35F,
                  point.y - size * 1.35F,
                  size * 2.7F,
                  size * 2.7F,
                  -time * 31.0F - particle * 8.0F,
                  0.0F,
                  ColorUtil.multiplyAlpha(color, 0.22F),
                  1.0F
               );
               RenderUtil.drawRotatedImage(
                  context, GHOST_TEXTURE, point.x - size, point.y - size, size * 2.0F, size * 2.0F, time * 48.0F + particle * 14.0F, 0.0F, color, 1.0F
               );
            }
         }
      }
   }

   private static boolean irisShadersEnabled() {
      try {
         if (irisPresent == null) {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            irisGetInstance = api.getMethod("getInstance");
            irisShadersInUse = api.getMethod("isShaderPackInUse");
            irisPresent = true;
         }

         if (!irisPresent) {
            return false;
         }

         Object instance = irisGetInstance.invoke(null);
         return Boolean.TRUE.equals(irisShadersInUse.invoke(instance));
      } catch (ClassNotFoundException ignored) {
         irisPresent = false;
         return false;
      } catch (ReflectiveOperationException ignored) {
         return false;
      }
   }

   private LivingEntity currentTarget() {
      if (mc.hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
         return target.isAlive() && !target.isRemoved() ? target : null;
      } else {
         return null;
      }
   }

   private void updateAnimationTarget(LivingEntity target, long now) {
      if (target != null) {
         if (this.displayedTarget != target) {
            boolean replacingVisibleTarget = this.displayedTarget != null && this.appearing && this.mode.getValue().equals("Призраки");
            this.displayedTarget = target;
            if (!replacingVisibleTarget) {
               this.animationChangedAt = now;
            }

            this.epoch = now;
         }

         if (!this.appearing) {
            this.animationChangedAt = now;
         }

         this.appearing = true;
      } else if (this.appearing) {
         this.appearing = false;
         this.animationChangedAt = now;
      }
   }

   private float animation(long now, float elapsedSeconds) {
      if (this.mode.getValue().equals("Призраки")) {
         float goal = this.appearing ? 1.0F : 0.0F;
         float rate = this.appearing ? 12.0F : 6.0F;
         float response = 1.0F - (float)Math.exp(-elapsedSeconds * rate);
         this.ghostVisibility = this.ghostVisibility + (goal - this.ghostVisibility) * response;
         if (!this.appearing && this.ghostVisibility <= 0.01F) {
            this.ghostVisibility = 0.0F;
            this.displayedTarget = null;
            this.resetGhostMotion();
         }

         return this.ghostVisibility;
      } else {
         float t = Mth.clamp((float)(now - this.animationChangedAt) / 300.0F, 0.0F, 1.0F);
         if (!this.appearing && t >= 1.0F) {
            this.displayedTarget = null;
            this.resetGhostMotion();
         }

         return this.appearing ? this.easeOutBack(t) : 1.0F - this.smoothStep(t);
      }
   }

   private void renderMarker(EventRender3D event, LivingEntity target, float alpha, long now) {
      Identifier texture = Identifier.fromNamespaceAndPath("havoc", "textures/luma/world/" + this.markerSkin.getValue().toLowerCase() + ".png");
      VertexConsumer consumer = event.getConsumers().getBuffer(TargetEspPipelines.MARKER_LAYER.apply(texture));
      PoseStack stack = event.getMatrices();
      Vec3 pos = this.lerped(target, event.getTickDelta()).subtract(event.getCameraPos());
      float time = (float)(now - this.epoch) / 1000.0F;
      float[] p = this.markerParameters(this.markerSkin.getValue());
      float omega = (float)((Math.PI * 2) / p[1]);
      float angle = -(p[0] / omega) * (float)Math.cos(omega * (time + p[2])) + p[5] * (float)Math.sin(time * 0.7);
      float size = this.markerSize.getValue() * (1.0F + p[4] * (float)Math.sin(time * p[3] * Math.PI * 2.0)) * (1.0F + 0.25F * this.hurtProgress);
      int[] colors = this.colors(alpha * (0.85F + 0.15F * (float)Math.sin(time * p[3] * Math.PI * 2.0)));
      stack.pushPose();
      stack.translate(pos.x, pos.y + target.getBbHeight() * 0.5, pos.z);
      stack.mulPose(mc.gameRenderer.mainCamera().rotation());
      stack.mulPose(Axis.ZP.rotationDegrees(angle));
      stack.scale(size, size, 1.0F);
      Pose entry = stack.last();
      consumer.addVertex(entry, -1.0F, -1.0F, 0.0F).setUv(0.0F, 0.0F).setColor(colors[1]);
      consumer.addVertex(entry, -1.0F, 1.0F, 0.0F).setUv(0.0F, 1.0F).setColor(colors[0]);
      consumer.addVertex(entry, 1.0F, 1.0F, 0.0F).setUv(1.0F, 1.0F).setColor(colors[1]);
      consumer.addVertex(entry, 1.0F, -1.0F, 0.0F).setUv(1.0F, 0.0F).setColor(colors[0]);
      stack.popPose();
      event.getConsumers().draw(TargetEspPipelines.MARKER_LAYER.apply(texture));
   }

   private void renderRing(EventRender3D event, LivingEntity target, float alpha) {
      VertexConsumer consumer = event.getConsumers().getBuffer(TargetEspPipelines.RING_LAYER);
      Pose entry = event.getMatrices().last();
      Vec3 center = this.lerped(target, event.getTickDelta()).subtract(event.getCameraPos());
      double wave = 0.5 - 0.5 * Math.cos(this.circleStep);
      double wave2 = 0.5 - 0.5 * Math.cos(this.circleStep - 0.35);
      this.smoothY = this.smoothY + (wave * target.getBbHeight() - this.smoothY) * 0.12;
      this.smoothY2 = this.smoothY2 + (wave2 * target.getBbHeight() - this.smoothY2) * 0.1;
      float radius = target.getBbWidth() * (1.0F + (float)Math.sin(Math.min(this.hurtProgress * 2.0F, 1.0F) * Math.PI) * 0.18F);
      int[] base = this.colors(alpha);

      for (int i = 0; i < 64; i++) {
         double a1 = i * Math.PI * 2.0 / 64.0;
         double a2 = (i + 1) * Math.PI * 2.0 / 64.0;
         float gradient1 = (float)(0.5 - 0.5 * Math.cos(a1));
         float gradient2 = (float)(0.5 - 0.5 * Math.cos(a2));
         int c1 = ColorUtil.interpolate(base[0], base[1], gradient1);
         int c2 = ColorUtil.interpolate(base[0], base[1], gradient2);
         Vec3 top1 = center.add(Math.cos(a1) * radius, this.smoothY, Math.sin(a1) * radius);
         Vec3 top2 = center.add(Math.cos(a2) * radius, this.smoothY, Math.sin(a2) * radius);
         Vec3 bottom1 = center.add(Math.cos(a1) * radius, this.smoothY2, Math.sin(a1) * radius);
         Vec3 bottom2 = center.add(Math.cos(a2) * radius, this.smoothY2, Math.sin(a2) * radius);
         this.quad(consumer, entry, top1, top2, bottom2, bottom1, c1, c2, 0, 0);
         this.quad(consumer, entry, bottom1, bottom2, top2, top1, 0, 0, c2, c1);
      }

      event.getConsumers().draw(TargetEspPipelines.RING_LAYER);
   }

   private void renderGhosts(EventRender3D event, LivingEntity target, float alpha, long now, float elapsedSeconds) {
      VertexConsumer consumer = event.getConsumers().getBuffer(TargetEspPipelines.MARKER_LAYER.apply(GHOST_TEXTURE));
      PoseStack stack = event.getMatrices();
      Vec3 camera = event.getCameraPos();
      float flight = this.updateGhostMotion(target, event.getTickDelta(), elapsedSeconds, now);
      float time = (float)(now - this.ghostEpoch) / 1000.0F;
      float hurtScale = 1.0F + this.hurtProgress * 0.18F;
      int[] palette = this.colors(alpha);

      for (int lane = 0; lane < 3; lane++) {
         float laneOffset = lane * (float) (Math.PI * 2.0 / 3.0);

         for (int particle = 0; particle < 30; particle++) {
            float trail = particle / 29.0F;
            long delay = (long)(particle * (18.0F + flight * 16.0F));
            float delayedTime = (float)(now - delay - this.ghostEpoch) / 1000.0F;
            Vec3 base = this.sampleGhostHistory(now - delay);
            float phase = delayedTime * (0.72F + flight * 0.1F) + laneOffset - trail * 0.32F;
            float radius = (0.72F + 0.1F * Mth.sin(delayedTime * 1.4F + lane)) * hurtScale * (1.0F - flight * 0.18F);
            double px = base.x + Mth.sin(phase) * radius;
            double pz = base.z + Mth.cos(phase) * radius;
            double py = base.y
               + target.getBbHeight() * (0.24F + lane * 0.24F)
               + Mth.sin(delayedTime * 1.35F + particle * 0.12F + laneOffset) * (0.13F + flight * 0.05F);
            float pulse = 0.94F + 0.06F * Mth.sin(time * 4.2F + laneOffset - trail * 2.0F);
            float size = (0.27F - trail * 0.16F) * pulse * (1.0F + this.hurtProgress * 0.22F + flight * (1.0F - trail) * 0.16F);
            float tailAlpha = 0.08F + 0.92F * (float)Math.pow(1.0F - trail, 1.45F);
            int color = ColorUtil.multiplyAlpha(ColorUtil.interpolate(palette[0], palette[1], 0.5F + 0.5F * Mth.sin(phase + trail * 2.0F)), tailAlpha);
            float rotation = time * 16.0F + particle * 5.0F + lane * 18.0F + flight * Mth.sin(time * 1.4F + laneOffset) * 10.0F;
            float stretchX = 1.0F + flight * (1.0F - trail) * 0.62F;
            float stretchY = 1.0F - flight * (1.0F - trail) * 0.12F;
            Vec3 particlePos = new Vec3(px, py, pz);
            if (particle == 0) {
               int glow = ColorUtil.multiplyAlpha(color, 0.2F + flight * 0.18F);
               this.drawGhostSprite(stack, consumer, particlePos, camera, size * 2.15F, size * 1.9F, rotation * 0.72F, glow);
            }

            this.drawGhostSprite(stack, consumer, particlePos, camera, size * stretchX, size * stretchY, rotation, color);
         }
      }

      event.getConsumers().draw(TargetEspPipelines.MARKER_LAYER.apply(GHOST_TEXTURE));
   }

   private float updateGhostMotion(LivingEntity target, float tickDelta, float elapsedSeconds, long now) {
      if (this.ghostAnchor == null || target.isAlive() && !target.isRemoved()) {
         Vec3 targetPosition = this.lerped(target, tickDelta);
         if (this.ghostAnchor != null && this.finite(this.ghostAnchor)) {
            double anchorDistance = targetPosition.distanceTo(this.ghostAnchor);
            if (Double.isFinite(anchorDistance) && !(anchorDistance > 14.0)) {
               boolean switchedTarget = this.ghostTrackedTarget != target;
               if (switchedTarget) {
                  Vec3 toTarget = targetPosition.subtract(this.ghostAnchor);
                  double distance = toTarget.length();
                  Vec3 side = new Vec3(-toTarget.z, 0.0, toTarget.x);
                  if (side.lengthSqr() < 1.0E-6) {
                     side = new Vec3((target.getId() & 1) == 0 ? 1.0 : -1.0, 0.0, 0.0);
                  } else {
                     side = side.normalize();
                  }

                  double direction = (target.getId() & 1) == 0 ? 1.0 : -1.0;
                  this.ghostFlightFrom = this.ghostAnchor;
                  this.ghostFlightSide = side.scale(direction);
                  this.ghostFlightStartedAt = now;
                  this.ghostFlightDurationMs = (long)Math.max(1150.0, Math.min(1900.0, 1050.0 + distance * 95.0));
                  this.ghostFlightSideAmount = (float)Math.min(1.35, 0.22 + distance * 0.14);
                  this.ghostFlightArcHeight = (float)Math.min(1.15, 0.24 + distance * 0.09);
                  this.ghostSwitching = true;
                  this.ghostVelocity = this.ghostVelocity.scale(0.2);
                  this.ghostTrackedTarget = target;
                  this.ghostFlightStrength = Math.max(this.ghostFlightStrength, 0.25F);
               }

               Vec3 targetMotion = target.getDeltaMovement();
               double targetMotionLength = targetMotion.length();
               if (targetMotionLength > 0.8) {
                  targetMotion = targetMotion.scale(0.8 / targetMotionLength);
               }

               double distanceBefore = targetPosition.distanceTo(this.ghostAnchor);
               double leadTicks = 2.1 + Math.min(distanceBefore * 0.16, 2.2);
               Vec3 desiredPosition = targetPosition.add(targetMotion.scale(leadTicks));
               double totalStep = Math.max(0.001, Math.min(elapsedSeconds, 0.1));
               float switchVisual = 0.0F;
               if (this.ghostSwitching && this.ghostFlightFrom != null) {
                  float progress = Mth.clamp((float)(now - this.ghostFlightStartedAt) / (float)this.ghostFlightDurationMs, 0.0F, 1.0F);
                  float eased = this.smootherStep(progress);
                  float arc = Mth.sin(progress * (float) Math.PI);
                  Vec3 nextAnchor = this.lerp(this.ghostFlightFrom, desiredPosition, eased)
                     .add(this.ghostFlightSide.scale(arc * this.ghostFlightSideAmount))
                     .add(0.0, arc * this.ghostFlightArcHeight, 0.0);
                  this.ghostVelocity = nextAnchor.subtract(this.ghostAnchor).scale(1.0 / totalStep);
                  this.ghostAnchor = nextAnchor;
                  switchVisual = arc;
                  if (progress >= 1.0F) {
                     this.ghostSwitching = false;
                     this.ghostVelocity = targetMotion.scale(20.0);
                  }
               } else {
                  int subSteps = Math.max(1, (int)Math.ceil(totalStep / 0.016666666666666666));
                  double step = totalStep / subSteps;

                  for (int i = 0; i < subSteps; i++) {
                     Vec3 offset = desiredPosition.subtract(this.ghostAnchor);
                     double distance = offset.length();
                     double stiffness = 25.0 + Math.min(distance * 2.0, 22.0);
                     double damping = 2.0 * Math.sqrt(stiffness) * 1.18;
                     Vec3 acceleration = offset.scale(stiffness).subtract(this.ghostVelocity.scale(damping));
                     this.ghostVelocity = this.ghostVelocity.add(acceleration.scale(step));
                     double speed = this.ghostVelocity.length();
                     double maxSpeed = 4.2 + Math.min(distance * 1.8, 14.0);
                     if (speed > maxSpeed) {
                        this.ghostVelocity = this.ghostVelocity.scale(maxSpeed / speed);
                     }

                     this.ghostAnchor = this.ghostAnchor.add(this.ghostVelocity.scale(step));
                  }
               }

               double remainingDistance = targetPosition.distanceTo(this.ghostAnchor);
               double speed = this.ghostVelocity.length();
               float targetFlight = Mth.clamp((float)((remainingDistance - 0.18) / 3.8 + speed / 28.0 * 0.55), 0.0F, 1.0F);
               if (this.ghostSwitching) {
                  targetFlight = Math.max(targetFlight, 0.42F + switchVisual * 0.38F);
               }

               float response = 1.0F - (float)Math.exp(-elapsedSeconds * (targetFlight > this.ghostFlightStrength ? 5.0F : 2.4F));
               this.ghostFlightStrength = this.ghostFlightStrength + (targetFlight - this.ghostFlightStrength) * response;
               this.rememberGhostAnchor(now);
               return Mth.clamp(this.ghostFlightStrength, 0.0F, 1.0F);
            } else {
               this.ghostAnchor = targetPosition;
               this.ghostVelocity = Vec3.ZERO;
               this.ghostTrackedTarget = target;
               this.ghostSwitching = false;
               this.ghostFlightFrom = null;
               this.ghostFlightStrength = 0.0F;
               this.ghostHistoryCursor = -1;
               this.ghostHistoryCount = 0;
               this.rememberGhostAnchor(now);
               return 0.0F;
            }
         } else {
            this.ghostAnchor = targetPosition;
            this.ghostVelocity = Vec3.ZERO;
            this.ghostTrackedTarget = target;
            this.ghostFlightStrength = 0.0F;
            this.ghostHistoryCursor = -1;
            this.ghostHistoryCount = 0;
            this.rememberGhostAnchor(now);
            return 0.0F;
         }
      } else {
         this.ghostSwitching = false;
         double damping = Math.exp(-elapsedSeconds * 6.0);
         this.ghostVelocity = this.ghostVelocity.scale(damping);
         this.ghostAnchor = this.ghostAnchor.add(this.ghostVelocity.scale(elapsedSeconds));
         this.ghostFlightStrength = this.ghostFlightStrength * (float)Math.exp(-elapsedSeconds * 2.5);
         this.rememberGhostAnchor(now);
         return Mth.clamp(this.ghostFlightStrength, 0.0F, 1.0F);
      }
   }

   private void rememberGhostAnchor(long now) {
      if (this.ghostAnchor != null) {
         if (this.ghostHistoryCount > 0 && this.ghostHistoryTimes[this.ghostHistoryCursor] == now) {
            this.ghostHistory[this.ghostHistoryCursor] = this.ghostAnchor;
         } else {
            this.ghostHistoryCursor = (this.ghostHistoryCursor + 1) % 256;
            this.ghostHistory[this.ghostHistoryCursor] = this.ghostAnchor;
            this.ghostHistoryTimes[this.ghostHistoryCursor] = now;
            if (this.ghostHistoryCount < 256) {
               this.ghostHistoryCount++;
            }
         }
      }
   }

   private Vec3 sampleGhostHistory(long sampleTime) {
      if (this.ghostHistoryCount > 0 && this.ghostHistoryCursor >= 0 && this.ghostAnchor != null) {
         int newest = this.ghostHistoryCursor;
         if (sampleTime >= this.ghostHistoryTimes[newest]) {
            return this.ghostHistory[newest];
         }

         for (int i = 0; i < this.ghostHistoryCount - 1; i++) {
            int newer = (newest - i + 256) % 256;
            int older = (newer - 1 + 256) % 256;
            long newerTime = this.ghostHistoryTimes[newer];
            long olderTime = this.ghostHistoryTimes[older];
            if (sampleTime >= olderTime) {
               double progress = newerTime == olderTime ? 1.0 : (double)(sampleTime - olderTime) / (newerTime - olderTime);
               return this.lerp(this.ghostHistory[older], this.ghostHistory[newer], progress);
            }
         }

         int oldest = (newest - this.ghostHistoryCount + 1 + 256) % 256;
         return this.ghostHistory[oldest];
      } else {
         return this.ghostAnchor == null ? Vec3.ZERO : this.ghostAnchor;
      }
   }

   private void drawGhostSprite(PoseStack stack, VertexConsumer consumer, Vec3 position, Vec3 camera, float width, float height, float rotation, int color) {
      Vec3 relative = position.subtract(camera);
      stack.pushPose();
      stack.translate(relative.x, relative.y, relative.z);
      stack.mulPose(mc.gameRenderer.mainCamera().rotation());
      stack.mulPose(Axis.ZP.rotationDegrees(rotation));
      stack.scale(width, height, 1.0F);
      Pose entry = stack.last();
      consumer.addVertex(entry, -1.0F, -1.0F, 0.0F).setUv(0.0F, 0.0F).setColor(color);
      consumer.addVertex(entry, -1.0F, 1.0F, 0.0F).setUv(0.0F, 1.0F).setColor(color);
      consumer.addVertex(entry, 1.0F, 1.0F, 0.0F).setUv(1.0F, 1.0F).setColor(color);
      consumer.addVertex(entry, 1.0F, -1.0F, 0.0F).setUv(1.0F, 0.0F).setColor(color);
      stack.popPose();
   }

   private Vec3 lerp(Vec3 from, Vec3 to, double delta) {
      return new Vec3(Mth.lerp(delta, from.x, to.x), Mth.lerp(delta, from.y, to.y), Mth.lerp(delta, from.z, to.z));
   }

   private boolean finite(Vec3 value) {
      return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
   }

   private void resetGhostMotion() {
      this.ghostAnchor = null;
      this.ghostVelocity = Vec3.ZERO;
      this.ghostHistoryCursor = -1;
      this.ghostHistoryCount = 0;
      this.ghostTrackedTarget = null;
      this.ghostFlightFrom = null;
      this.ghostFlightSide = Vec3.ZERO;
      this.ghostFlightStartedAt = 0L;
      this.ghostFlightDurationMs = 0L;
      this.ghostFlightSideAmount = 0.0F;
      this.ghostFlightArcHeight = 0.0F;
      this.ghostSwitching = false;
      this.ghostFlightStrength = 0.0F;
   }

   private void quad(VertexConsumer out, Pose e, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int ca, int cb, int cc, int cd) {
      out.addVertex(e, (float)a.x, (float)a.y, (float)a.z).setColor(ca);
      out.addVertex(e, (float)b.x, (float)b.y, (float)b.z).setColor(cb);
      out.addVertex(e, (float)c.x, (float)c.y, (float)c.z).setColor(cc);
      out.addVertex(e, (float)d.x, (float)d.y, (float)d.z).setColor(cd);
   }

   private int[] colors(float alpha) {
      int first = ClientColors.CATEGORY_ACCENT;
      int second = ClientColors.CATEGORY_ACCENT_SECOND;
      if (this.hitFlash.isValue() && this.hurtProgress > 0.0F) {
         first = ColorUtil.interpolate(first, -58854, this.hurtProgress);
         second = ColorUtil.interpolate(second, -58854, this.hurtProgress);
      }

      int a = Mth.clamp((int)(255.0F * alpha), 0, 255);
      return new int[]{ColorUtil.withAlpha(first, a), ColorUtil.withAlpha(second, a)};
   }

   private float[] markerParameters(String skin) {
      return switch (skin) {
         case "Marker1" -> new float[]{220.0F, 2.2F, 0.0F, 2.4F, 0.07F, 4.0F};
         case "Marker2" -> new float[]{-180.0F, 2.6F, 1.0F, 1.8F, 0.12F, 0.0F};
         case "Marker3" -> new float[]{140.0F, 3.0F, 2.0F, 1.2F, 0.1F, 6.0F};
         default -> new float[]{110.0F, 2.8F, 0.5F, 1.6F, 0.06F, 3.0F};
      };
   }

   private Vec3 lerped(LivingEntity entity, float delta) {
      return new Vec3(Mth.lerp(delta, entity.xOld, entity.getX()), Mth.lerp(delta, entity.yOld, entity.getY()), Mth.lerp(delta, entity.zOld, entity.getZ()));
   }

   private float easeOutBack(float x) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + c3 * (float)Math.pow(x - 1.0F, 3.0) + c1 * (float)Math.pow(x - 1.0F, 2.0);
   }

   private float smoothStep(float x) {
      return x * x * (3.0F - 2.0F * x);
   }

   private float[] projectedBounds(LivingEntity target, float tickDelta, int width, int height) {
      Vec3 base = this.lerped(target, tickDelta);
      double halfWidth = Math.max(0.3, target.getBbWidth() * 0.5 + 0.1);
      double boxHeight = Math.max(1.6, target.getBbHeight() + 0.2);
      float minX = Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      float maxX = -Float.MAX_VALUE;
      float maxY = -Float.MAX_VALUE;
      for (int sx = -1; sx <= 1; sx += 2) {
         for (int sy = 0; sy <= 1; sy++) {
            for (int sz = -1; sz <= 1; sz += 2) {
               Vec2 point = RenderUtil3D.worldToScreen(
                  base.x + sx * halfWidth, base.y + sy * boxHeight, base.z + sz * halfWidth, width, height
               );
               if (point == null) {
                  return null;
               }
               minX = Math.min(minX, point.x);
               maxX = Math.max(maxX, point.x);
               minY = Math.min(minY, point.y);
               maxY = Math.max(maxY, point.y);
            }
         }
      }
      if (maxX - minX < 2.0F || maxY - minY < 2.0F) {
         return null;
      } else {
         return new float[]{minX, minY, maxX, maxY};
      }
   }

   private void renderBracketOverlay(GuiGraphicsExtractor context, LivingEntity target, float alpha, float tickDelta, int width, int height, long now) {
      float[] box = this.projectedBounds(target, tickDelta, width, height);
      if (box == null) {
         return;
      }
      float recoil = target.hurtTime > 0 ? (float)target.hurtTime / 10.0F : 0.0F;
      float expand = 1.0F + recoil * 0.16F;
      float x0 = box[0];
      float y0 = box[1];
      float x1 = box[2];
      float y1 = box[3];
      float centerX = (x0 + x1) * 0.5F;
      float centerY = (y0 + y1) * 0.5F;
      float halfWidth = (x1 - x0) * 0.5F * expand;
      float halfHeight = (y1 - y0) * 0.5F * expand;
      int[] pal = this.colors(alpha);
      int accent = ColorUtil.multiplyAlpha(pal[0], alpha);
      int bright = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT_SECOND, alpha);
      float armLength = Math.min(18.0F, halfHeight * 0.45F);
      float thickness = 3.0F;
      float left = centerX - halfWidth;
      float right = centerX + halfWidth;
      float top = centerY - halfHeight;
      float bottom = centerY + halfHeight;
      RenderUtil.drawGlow(context, left - 2.0F, top - 2.0F, armLength + 4.0F, thickness + 4.0F, 2.0F, ColorUtil.multiplyAlpha(accent, 0.35F), 1.0F, 8.0F);
      RenderUtil.drawRoundRect(context, left, top, armLength, thickness, 1.0F, bright);
      RenderUtil.drawRoundRect(context, left, top, thickness, armLength, 1.0F, bright);
      RenderUtil.drawRoundRect(context, right - armLength, top, armLength, thickness, 1.0F, bright);
      RenderUtil.drawRoundRect(context, right - thickness, top, thickness, armLength, 1.0F, bright);
      RenderUtil.drawRoundRect(context, left, bottom - thickness, armLength, thickness, 1.0F, bright);
      RenderUtil.drawRoundRect(context, left, bottom - armLength, thickness, armLength, 1.0F, bright);
      RenderUtil.drawRoundRect(context, right - armLength, bottom - thickness, armLength, thickness, 1.0F, bright);
      RenderUtil.drawRoundRect(context, right - thickness, bottom - armLength, thickness, armLength, 1.0F, bright);
      float scanProgress = (float)(now % 1900L) / 1900.0F;
      float scanY = top + scanProgress * (bottom - top);
      RenderUtil.drawRoundRect(context, left, scanY - 1.0F, halfWidth * 2.0F, 2.0F, 1.0F, ColorUtil.multiplyAlpha(ColorUtil.rgba(34, 211, 238, 255), alpha * 0.9F));
      int litTicks = (int)((now / 500L) % 8L);
      for (int i = 0; i < 7; i++) {
         boolean lit = i < litTicks;
         RenderUtil.drawRoundRect(
            context, left - 14.0F, centerY - halfHeight + 6.0F + i * 13.0F, lit ? 11.0F : 7.0F, 2.5F, 1.0F,
            ColorUtil.multiplyAlpha(ColorUtil.rgba(34, 211, 238, 255), alpha * (lit ? 0.95F : 0.25F))
         );
      }
      float distance = mc.player.distanceTo(target);
      RenderUtil.drawText(
         context, "LOCK " + String.format(java.util.Locale.ROOT, "%.1f", distance) + "m",
         right - 40.0F, top - 12.0F, ColorUtil.multiplyAlpha(bright, 0.8F), GFonts.MEDIUM, 7.0F
      );
   }

   private float smootherStep(float x) {
      return x * x * x * (x * (x * 6.0F - 15.0F) + 10.0F);
   }
}
