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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(name = "TargetESP", desc = "Animated target marker, ring and ghosts", category = Category.VISUALS)
public final class TargetESP extends Module {
   private static final String MODE_MARKER = "Маркер";
   private static final String MODE_RING = "Кольцо";
   private static final String MODE_GHOSTS = "Призраки";
   private static final String MODE_GLITCH = "Глитч-диссонанс";
   private static final String MODE_BRACKET = "Скан-брекет";
   private static final String MODE_SONAR = "Сонар";
   private static final String MODE_RUNES = "Рунный круг";
   private static final String MODE_LANCE = "Небесный ланс";
   private static final String MODE_VORTEX = "Вихрь";
   private static final String MODE_SHATTER = "Разлом";
   private static final String MODE_CRYSTAL = "Кристалл";
   private static final String MODE_FLAMES = "Пламя";
   private static final String MODE_HELIX = "Спираль ДНК";
   private static final String MODE_APERTURE = "Диафрагма";
   private static final String MODE_ARC = "Разряд";
   private static final String MODE_PIXEL = "Пиксель-шум";
   private static final String MODE_SCAN = "Голо-скан";
   private static final String MODE_TENDRILS = "Щупальца";
   private static final String MODE_TOTEM = "Тотем-столб";
   private static final String MODE_DOME = "Купол";
   private static final String MODE_INFOPANEL = "Инфо-панель";
   private static final String MODE_SAKURA = "Сакура-вихрь";
   private static final Identifier GHOST_TEXTURE = Identifier.fromNamespaceAndPath("havoc", "textures/luma/particle/ghost-glow.png");
   private static final int GHOST_LANES = 3;
   private static final int GHOST_TRAIL_PARTICLES = 30;
   private static final int GHOST_HISTORY_SIZE = 256;
   private final SelectSetting mode = this.registerSetting(
      new SelectSetting(
         "Режим",
         "Маркер",
         List.of(
            "Маркер",
            "Кольцо",
            "Призраки",
            MODE_GLITCH,
            MODE_BRACKET,
            MODE_SONAR,
            MODE_RUNES,
            MODE_LANCE,
            MODE_VORTEX,
            MODE_SHATTER,
            MODE_CRYSTAL,
            MODE_FLAMES,
            MODE_HELIX,
            MODE_APERTURE,
            MODE_ARC,
            MODE_PIXEL,
            MODE_SCAN,
            MODE_TENDRILS,
            MODE_TOTEM,
            MODE_DOME,
            MODE_INFOPANEL,
            MODE_SAKURA
         )
      )
   );
   private final SelectSetting markerSkin = this.registerSetting(
      new SelectSetting("Тип маркера", "Marker", List.of("Marker", "Marker1", "Marker2", "Marker3")).visibleWhen(() -> this.mode.getValue().equals("Маркер"))
   );
   private final SliderSetting markerSize = this.registerSetting(
      new SliderSetting("Размер маркера", 0.55F, 0.2F, 2.0F, 0.05F).visibleWhen(() -> this.mode.getValue().equals("Маркер"))
   );
   private final SliderSetting domeRadius = this.registerSetting(
      new SliderSetting("Радиус купола", 1.6F, 1.0F, 3.0F, 0.05F).visibleWhen(() -> this.mode.getValue().equals(MODE_DOME))
   );
   private final SliderSetting pixelScale = this.registerSetting(
      new SliderSetting("Размер пикселя", 0.075F, 0.04F, 0.15F, 0.005F).visibleWhen(() -> this.mode.getValue().equals(MODE_PIXEL))
   );
   private final SliderSetting infoScale = this.registerSetting(
      new SliderSetting("Размер панели", 1.0F, 0.7F, 1.5F, 0.05F).visibleWhen(() -> this.mode.getValue().equals(MODE_INFOPANEL))
   );
   private final SliderSetting sakuraDensity = this.registerSetting(
      new SliderSetting("Плотность лепестков", 1.0F, 0.5F, 2.0F, 0.05F).visibleWhen(() -> this.mode.getValue().equals(MODE_SAKURA))
   );
   private final SliderSetting sakuraSpeed = this.registerSetting(
      new SliderSetting("Скорость вихря", 1.0F, 0.2F, 2.5F, 0.05F).visibleWhen(() -> this.mode.getValue().equals(MODE_SAKURA))
   );
   private final BooleanSetting shaderStyle = this.registerSetting(new BooleanSetting("Шейдерный стиль", true));
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
               Scene scene = this.buildSceneFor(currentMode, this.displayedTarget, alpha, now, event.getTickDelta());
               if (scene != null) {
                  this.renderQuads3D(event, scene.quads());
                  this.renderDots3D(event, scene.dots());
               } else if (currentMode.equals(MODE_RING)) {
                  this.renderRing(event, this.displayedTarget, alpha);
               } else if (currentMode.equals(MODE_GHOSTS)) {
                  this.renderGhosts(event, this.displayedTarget, alpha, now, elapsedSeconds);
               } else if (!currentMode.equals(MODE_GLITCH)
                  && !currentMode.equals(MODE_BRACKET)
                  && !currentMode.equals(MODE_INFOPANEL)) {
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
      boolean overlayOnly = currentMode.equals(MODE_GLITCH) || currentMode.equals(MODE_BRACKET) || currentMode.equals(MODE_INFOPANEL);
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
      } else if (currentMode.equals(MODE_GLITCH)) {
         this.renderGlitchOverlay(context, this.displayedTarget, alpha, tickDelta, width, height, now);
      } else if (currentMode.equals(MODE_BRACKET)) {
         this.renderBracketOverlay(context, this.displayedTarget, alpha, tickDelta, width, height, now);
      } else if (currentMode.equals(MODE_INFOPANEL)) {
         this.renderInfoPanel(context, this.displayedTarget, alpha, tickDelta, width, height);
      } else {
         this.renderSceneOverlay(context, this.displayedTarget, currentMode, alpha, tickDelta, width, height, now);
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

   private record Dot(Vec3 pos, float size, int color) {
   }

   private record Quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int ca, int cb, float u0, float u1) {
   }

   private record Scene(List<Quad> quads, List<Dot> dots) {
   }

   private final ArrayList<Quad> cachedQuads = new ArrayList<>();
   private final ArrayList<Dot> cachedDots = new ArrayList<>();

   private Scene scene() {
      this.cachedQuads.clear();
      this.cachedDots.clear();
      return new Scene(this.cachedQuads, this.cachedDots);
   }

   private double rand(int seed) {
      double value = Math.sin(seed * 12.9898) * 43758.5453;
      return value - Math.floor(value);
   }

   private Vec3 centerOf(LivingEntity target, float tickDelta) {
      return this.lerped(target, tickDelta).add(0.0, target.getBbHeight() * 0.5, 0.0);
   }

   private Scene buildSceneFor(String currentMode, LivingEntity target, float alpha, long now, float tickDelta) {
      if (currentMode.equals(MODE_SONAR)) {
         return this.buildSonar(target, alpha, now, tickDelta);
      } else if (currentMode.equals(MODE_RUNES)) {
         return this.buildRunes(target, alpha, now, tickDelta);
      } else if (currentMode.equals(MODE_LANCE)) {
         return this.buildLance(target, alpha, now, tickDelta);
      } else if (currentMode.equals(MODE_VORTEX)) {
         return this.buildVortex(target, alpha, now, tickDelta);
      } else if (currentMode.equals(MODE_SHATTER)) {
         return this.buildShatter(target, alpha, now, tickDelta);
      } else if (currentMode.equals(MODE_CRYSTAL)) {
         return this.buildCrystal(target, alpha, now, tickDelta);
      } else if (currentMode.equals(MODE_FLAMES)) {
         return this.buildFlames(target, alpha, now, tickDelta);
      } else if (currentMode.equals(MODE_HELIX)) {
         return this.buildHelix(target, alpha, now, tickDelta);
      } else if (currentMode.equals(MODE_APERTURE)) {
         return this.buildAperture(target, alpha, now, tickDelta);
      } else if (currentMode.equals(MODE_ARC)) {
         return this.buildArc(target, alpha, now, tickDelta);
      } else if (currentMode.equals(MODE_PIXEL)) {
         return this.buildPixel(target, alpha, now, tickDelta);
      } else if (currentMode.equals(MODE_SCAN)) {
         return this.buildScan(target, alpha, now, tickDelta);
      } else if (currentMode.equals(MODE_TENDRILS)) {
         return this.buildTendrils(target, alpha, now, tickDelta);
      } else if (currentMode.equals(MODE_TOTEM)) {
         return this.buildTotem(target, alpha, now, tickDelta);
      } else if (currentMode.equals(MODE_DOME)) {
         return this.buildDome(target, alpha, now, tickDelta);
      } else if (currentMode.equals(MODE_SAKURA)) {
         return this.buildSakura(target, alpha, now, tickDelta);
      } else {
         return null;
      }
   }

   private void addQuadRing(Scene scene, Vec3 center, double radius, double thickness, int color, boolean flat, int segments, long now) {
      float phase = (float)(now % 60000L) / 1000.0F * 0.35F;
      for (int i = 0; i < segments; i++) {
         double a0 = i * Math.PI * 2.0 / segments;
         double a1 = (i + 1) * Math.PI * 2.0 / segments;
         double inner = radius;
         double outer = radius + thickness;
         Vec3 p0 = flat
            ? center.add(Math.cos(a0) * inner, 0.0, Math.sin(a0) * inner)
            : center.add(Math.cos(a0) * inner, Math.sin(a0) * inner, 0.0);
         Vec3 p1 = flat
            ? center.add(Math.cos(a1) * inner, 0.0, Math.sin(a1) * inner)
            : center.add(Math.cos(a1) * inner, Math.sin(a1) * inner, 0.0);
         Vec3 p2 = flat
            ? center.add(Math.cos(a1) * outer, 0.0, Math.sin(a1) * outer)
            : center.add(Math.cos(a1) * outer, Math.sin(a1) * outer, 0.0);
         Vec3 p3 = flat
            ? center.add(Math.cos(a0) * outer, 0.0, Math.sin(a0) * outer)
            : center.add(Math.cos(a0) * outer, Math.sin(a0) * outer, 0.0);
         scene.quads().add(new Quad(p3, p2, p1, p0, color, color, (float)i / segments + phase, (float)(i + 1) / segments + phase));
      }
   }

   private void addDotChain(Scene scene, Vec3 from, Vec3 to, int steps, float size, int color) {
      for (int i = 0; i <= steps; i++) {
         Vec3 point = from.add(to.subtract(from).scale((double)i / steps));
         scene.dots().add(new Dot(point, size, color));
      }
   }

   private Scene buildSonar(LivingEntity target, float alpha, long now, float tickDelta) {
      Scene out = this.scene();
      int[] pal = this.colors(alpha);
      Vec3 feet = this.lerped(target, tickDelta);
      int rings = 4;
      int segments = 30;
      long period = 1200L;
      for (int r = 0; r < rings; r++) {
         float k = ((float)(now % period) / (float)period + (float)r / rings) % 1.0F;
         double radius = 0.30 + (double)k * 1.8;
         int col = ColorUtil.multiplyAlpha(ColorUtil.interpolate(pal[0], pal[1], k), Math.max(0.12F, 1.0F - k * 0.7F));
         this.addQuadRing(out, feet.add(0.0, 0.03, 0.0), radius, 0.05 + k * 0.02, col, true, segments, now);
      }
      return out;
   }

   private Scene buildRunes(LivingEntity target, float alpha, long now, float tickDelta) {
      Scene out = this.scene();
      int[] pal = this.colors(alpha);
      Vec3 feet = this.lerped(target, tickDelta);
      double radius = Math.max(0.6, target.getBbWidth() * 1.25);
      this.addQuadRing(out, feet.add(0.0, 0.03, 0.0), radius, 0.045, ColorUtil.multiplyAlpha(pal[0], alpha * 0.55F), true, 36, now);
      float rot = (float)(now % 60000L) / 1000.0F * 0.35F;
      int lit = (int)((now / 450L) % 12L);
      for (int g = 0; g < 12; g++) {
         double a = g * Math.PI * 2.0 / 12.0 + rot;
         Vec3 point = feet.add(Math.cos(a) * radius * 0.8, 0.06, Math.sin(a) * radius * 0.8);
         boolean active = g == lit;
         int col = active
            ? ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT_SECOND, alpha)
            : ColorUtil.multiplyAlpha(pal[0], alpha * 0.7F);
         out.dots().add(new Dot(point, active ? 0.13F : 0.08F, col));
      }
      return out;
   }

   private Scene buildLance(LivingEntity target, float alpha, long now, float tickDelta) {
      Scene out = this.scene();
      int[] pal = this.colors(alpha);
      Vec3 feet = this.lerped(target, tickDelta);
      double top = feet.y + 12.0;
      double width = 0.12;
      int steps = 10;
      for (int i = 0; i < steps; i++) {
         double y0 = feet.y + (top - feet.y) * i / steps;
         double y1 = feet.y + (top - feet.y) * (i + 1) / steps;
         float fade = 0.06F + 0.4F * (float)(top - y0) / (float)(top - feet.y);
         int high = ColorUtil.multiplyAlpha(pal[1], Math.min(1.0F, fade));
         int low = ColorUtil.multiplyAlpha(pal[0], Math.min(1.0F, fade));
         out.quads().add(new Quad(
            new Vec3(feet.x - width, y0, feet.z), new Vec3(feet.x + width, y0, feet.z),
            new Vec3(feet.x + width, y1, feet.z), new Vec3(feet.x - width, y1, feet.z), high, low, 0.0F, 1.0F
         ));
         out.quads().add(new Quad(
            new Vec3(feet.x, y0, feet.z - width), new Vec3(feet.x, y0, feet.z + width),
            new Vec3(feet.x, y1, feet.z + width), new Vec3(feet.x, y1, feet.z - width), high, low, 0.0F, 1.0F
         ));
      }
      float charge = (float)(now % 1200L) / 1200.0F;
      double chargeY = top - (top - feet.y) * charge;
      int chargeColor = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT_SECOND, alpha * 0.9F);
      out.quads().add(new Quad(
         new Vec3(feet.x - 0.26, chargeY, feet.z), new Vec3(feet.x + 0.26, chargeY, feet.z),
         new Vec3(feet.x + 0.26, chargeY + 0.07, feet.z), new Vec3(feet.x - 0.26, chargeY + 0.07, feet.z), chargeColor, chargeColor, 0.0F, 1.0F
      ));
      out.dots().add(new Dot(feet.add(0.0, 0.05, 0.0), 0.95F, ColorUtil.multiplyAlpha(pal[0], alpha * 0.45F)));
      return out;
   }

   private Scene buildVortex(LivingEntity target, float alpha, long now, float tickDelta) {
      Scene out = this.scene();
      int[] pal = this.colors(alpha);
      Vec3 center = this.centerOf(target, tickDelta);
      int count = 48;
      long period = 1400L;
      for (int i = 0; i < count; i++) {
         float p = ((float)(now % period) / (float)period + (float)i / count) % 1.0F;
         double radius = 2.6 * (1.0 - p) + 0.22;
         double angle = i * 2.399 + p * 7.0;
         Vec3 pos = center.add(Math.cos(angle) * radius, Math.sin(angle * 2.0) * 0.5 * (1.0 - p), Math.sin(angle) * radius);
         float fade = Math.min(1.0F, Math.min((1.0F - p) * 6.0F, p * 3.0F + 0.15F));
         int col = ColorUtil.multiplyAlpha(ColorUtil.interpolate(pal[0], pal[1], p), alpha * fade);
         out.dots().add(new Dot(pos, 0.14F + 0.1F * (1.0F - p), col));
      }
      return out;
   }

   private Scene buildShatter(LivingEntity target, float alpha, long now, float tickDelta) {
      Scene out = this.scene();
      int hurt = target.hurtTime;
      if (hurt <= 0) {
         return out;
      }
      int[] pal = this.colors(alpha);
      Vec3 center = this.centerOf(target, tickDelta);
      float progress = 1.0F - (float)hurt / 10.0F;
      for (int i = 0; i < 20; i++) {
         double angle = i * 0.618 * Math.PI * 2.0;
         double distance = 0.25 + progress * 1.7;
         double up = Math.sin(progress * Math.PI) * 0.7;
         Vec3 pos = center.add(
            Math.cos(angle) * distance,
            up * (0.4 + (i % 3) * 0.35) - progress * progress * 0.5,
            Math.sin(angle) * distance
         );
         int col = i % 3 == 0
            ? ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT_SECOND, alpha * (1.0F - progress))
            : ColorUtil.multiplyAlpha(ColorUtil.interpolate(pal[0], pal[1], (float)(i % 5) / 5.0F), alpha * (1.0F - progress));
         out.dots().add(new Dot(pos, 0.15F * (1.0F - progress * 0.5F), col));
      }
      this.addQuadRing(out, center, 0.32 + progress * 1.1, 0.05, ColorUtil.multiplyAlpha(0xFFFFFFFF, alpha * (1.0F - progress)), true, 24, now);
      return out;
   }

   private Scene buildCrystal(LivingEntity target, float alpha, long now, float tickDelta) {
      Scene out = this.scene();
      int[] pal = this.colors(alpha);
      Vec3 center = this.centerOf(target, tickDelta);
      double radius = 0.62;
      double half = Math.max(0.85, target.getBbHeight() * 0.62);
      float spin = (float)(now % 60000L) / 1000.0F * 1.1F;
      float tilt = (float)Math.sin(spin * 0.7) * 0.5F;
      Vec3[] vertices = new Vec3[6];
      vertices[0] = center.add(0.0, -half, 0.0);
      vertices[1] = center.add(0.0, half, 0.0);
      for (int i = 0; i < 4; i++) {
         double angle = spin + i * Math.PI / 2.0;
         double x = Math.cos(angle) * radius;
         double z = Math.sin(angle) * radius;
         vertices[2 + i] = center.add(x, z * Math.sin(tilt), z * Math.cos(tilt));
      }
      int[][] edges = {{0, 2}, {0, 3}, {0, 4}, {0, 5}, {1, 2}, {1, 3}, {1, 4}, {1, 5}, {2, 3}, {3, 4}, {4, 5}, {5, 2}};
      int color = ColorUtil.multiplyAlpha(pal[0], alpha * 0.9F);
      for (int[] edge : edges) {
         this.addDotChain(out, vertices[edge[0]], vertices[edge[1]], 6, 0.055F, color);
      }
      return out;
   }

   private Scene buildFlames(LivingEntity target, float alpha, long now, float tickDelta) {
      Scene out = this.scene();
      int[] pal = this.colors(alpha);
      Vec3 feet = this.lerped(target, tickDelta);
      int count = 24;
      long period = 900L;
      for (int i = 0; i < count; i++) {
         float p = ((float)(now % period) / (float)period + (float)i / count) % 1.0F;
         double angle = i * 2.4;
         double offset = 0.12 + (i % 5) * 0.075;
         Vec3 pos = feet.add(
            Math.cos(angle) * offset + Math.sin(p * 6.0 + i) * 0.06,
            0.06 + p * 1.5,
            Math.sin(angle) * offset
         );
         float fade = (float)Math.sin(p * Math.PI) * (0.75F + (target.hurtTime > 0 ? 0.25F : 0.0F));
         int col = ColorUtil.multiplyAlpha(ColorUtil.interpolate(pal[0], ClientColors.CATEGORY_ACCENT_SECOND, p), alpha * fade);
         out.dots().add(new Dot(pos, 0.14F + 0.1F * (1.0F - p), col));
      }
      return out;
   }

   private Scene buildHelix(LivingEntity target, float alpha, long now, float tickDelta) {
      Scene out = this.scene();
      int[] pal = this.colors(alpha);
      Vec3 base = this.lerped(target, tickDelta);
      double height = Math.max(1.2, target.getBbHeight() + 0.35);
      double radius = 0.42;
      int nodes = 22;
      float spin = (float)(now % 60000L) / 1000.0F * 1.6F;
      for (int i = 0; i < nodes; i++) {
         double k = (double)i / (nodes - 1);
         double y = base.y + 0.1 + k * height;
         double angle = spin + k * 5.4;
         Vec3 first = new Vec3(base.x + Math.cos(angle) * radius, y, base.z + Math.sin(angle) * radius);
         Vec3 second = new Vec3(base.x + Math.cos(angle + Math.PI) * radius, y, base.z + Math.sin(angle + Math.PI) * radius);
         out.dots().add(new Dot(first, 0.085F, ColorUtil.multiplyAlpha(pal[0], alpha)));
         out.dots().add(new Dot(second, 0.085F, ColorUtil.multiplyAlpha(pal[1], alpha)));
         if (i % 2 == 0) {
            this.addDotChain(out, first, second, 4, 0.042F, ColorUtil.multiplyAlpha(pal[0], alpha * 0.6F));
         }
      }
      return out;
   }

   private Scene buildAperture(LivingEntity target, float alpha, long now, float tickDelta) {
      Scene out = this.scene();
      int[] pal = this.colors(alpha);
      Vec3 center = this.centerOf(target, tickDelta);
      int blade = 60;
      float spin = (float)(now % 60000L) / 1000.0F * 0.9F;
      double scale = 1.0 + (target.hurtTime > 0 ? 0.12 : 0.0);
      for (int i = 0; i < blade; i++) {
         double angle = i * Math.PI * 2.0 / blade;
         double radius = 0.52 + 0.14 * Math.sin(6.0 * angle + spin * 2.0);
         Vec3 pos = center.add(
            Math.cos(angle) * radius * scale,
            Math.sin(angle * 3.0) * 0.06,
            Math.sin(angle) * radius * scale
         );
         int col = ColorUtil.multiplyAlpha(i % 2 == 0 ? pal[0] : pal[1], alpha * 0.9F);
         out.dots().add(new Dot(pos, 0.07F, col));
      }
      return out;
   }

   private Scene buildArc(LivingEntity target, float alpha, long now, float tickDelta) {
      Scene out = this.scene();
      int[] pal = this.colors(alpha);
      Vec3 base = this.lerped(target, tickDelta);
      double height = Math.max(1.2, target.getBbHeight());
      int seed = (int)(now / 80L);
      int chains = 3 + (target.hurtTime > 0 ? 2 : 0);
      for (int chain = 0; chain < chains; chain++) {
         double x = base.x + (this.rand(seed * 3 + chain) - 0.5) * 1.3;
         double y = base.y + 0.2 + this.rand(seed * 7 + chain) * height;
         double z = base.z + (this.rand(seed * 5 + chain) - 0.5) * 0.9;
         double angle = Math.PI / 2.0 + (this.rand(seed + chain * 11) - 0.5) * 2.0;
         Vec3 pos = new Vec3(x, y, z);
         for (int k = 0; k < 7; k++) {
            angle += (this.rand(seed * 13 + chain * 17 + k * 3) - 0.5) * 1.15;
            pos = pos.add(Math.cos(angle) * 0.13, Math.sin(angle) * 0.13, (this.rand(seed + k) - 0.5) * 0.07);
            int col = ColorUtil.multiplyAlpha(k == 0 ? ClientColors.CATEGORY_ACCENT_SECOND : pal[1], alpha * (0.85F - k * 0.07F));
            out.dots().add(new Dot(pos, k == 6 ? 0.1F : 0.062F, col));
         }
      }
      return out;
   }

   private Scene buildPixel(LivingEntity target, float alpha, long now, float tickDelta) {
      Scene out = this.scene();
      int[] pal = this.colors(alpha);
      Vec3 base = this.lerped(target, tickDelta);
      double width = Math.max(0.6, target.getBbWidth() + 0.25);
      double height = Math.max(1.5, target.getBbHeight() + 0.25);
      float step = this.pixelScale.getValue();
      int seed = (int)(now / 125L);
      int cols = 6;
      int rows = 10;
      for (int cx = 0; cx < cols; cx++) {
         for (int ry = 0; ry < rows; ry++) {
            double r = this.rand(seed * 31 + cx * 7 + ry * 13);
            if (r < 0.25) {
               continue;
            }
            double zOffset = (this.rand(seed * 7 + cx * 17 + ry * 23) - 0.5) * width * 0.75;
            Vec3 pos = new Vec3(
               base.x - width / 2.0 + width * (cx + 0.5) / cols,
               base.y + height * (ry + 0.5) / rows,
               base.z + zOffset
            );
            int col = ColorUtil.multiplyAlpha(r > 0.7 ? pal[1] : pal[0], alpha * (0.3F + (float)r * 0.65F));
            out.dots().add(new Dot(pos, step * 3.0F + 0.02F, col));
         }
      }
      return out;
   }

   private Scene buildScan(LivingEntity target, float alpha, long now, float tickDelta) {
      Scene out = this.scene();
      int[] pal = this.colors(alpha);
      Vec3 base = this.lerped(target, tickDelta);
      double height = Math.max(1.4, target.getBbHeight() + 0.15);
      float sweep = (float)((Math.sin(now / 1000.0 * 1.15) + 1.0) * 0.5);
      double sweepY = base.y + 0.05 + sweep * height;
      int dim = ColorUtil.multiplyAlpha(pal[0], alpha * 0.35F);
      int bright = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT_SECOND, alpha * 0.95F);
      for (int i = 0; i <= 14; i++) {
         double y = base.y + height * i / 14.0;
         float near = (float)(1.0 - Math.min(1.0, Math.abs(y - sweepY) / 0.35));
         out.dots().add(new Dot(new Vec3(base.x, y, base.z), 0.055F, ColorUtil.interpolate(dim, bright, near)));
      }
      for (int rib = 0; rib < 4; rib++) {
         double y = base.y + height * (0.28 + rib * 0.14);
         for (int i = -3; i <= 3; i++) {
            Vec3 pos = new Vec3(base.x + i * 0.09, y, base.z);
            float near = (float)(1.0 - Math.min(1.0, Math.abs(y - sweepY) / 0.35));
            out.dots().add(new Dot(pos, 0.045F, ColorUtil.interpolate(dim, bright, near)));
         }
      }
      this.addQuadRing(out, new Vec3(base.x, sweepY, base.z), 0.46, 0.03, ColorUtil.multiplyAlpha(pal[1], alpha * 0.8F), true, 24, now);
      return out;
   }

   private Scene buildTendrils(LivingEntity target, float alpha, long now, float tickDelta) {
      Scene out = this.scene();
      int[] pal = this.colors(alpha);
      Vec3 base = this.lerped(target, tickDelta);
      Vec3 center = this.centerOf(target, tickDelta);
      for (int i = 0; i < 5; i++) {
         double startX = base.x + (i - 2) * 0.34;
         float phase = (float)(now % 60000L) / 1000.0F * 1.4F + i * 1.1F;
         double reach = 0.84 + 0.14 * Math.sin(phase) + (target.hurtTime > 0 ? 0.16 : 0.0);
         double wave = Math.sin(phase * 1.3) * 0.3;
         int steps = 10;
         for (int k = 0; k <= steps; k++) {
            double t = (double)k / steps;
            double x = startX + (center.x - startX) * t * reach + wave * Math.sin(t * Math.PI);
            double y = base.y + (center.y - base.y) * t * reach;
            double z = base.z + Math.sin(t * Math.PI * 2.0 + phase) * 0.12 * t;
            int col = ColorUtil.multiplyAlpha(k == steps ? ClientColors.CATEGORY_ACCENT_SECOND : pal[0], alpha * (0.35F + 0.55F * (float)t));
            out.dots().add(new Dot(new Vec3(x, y, z), 0.075F - (float)t * 0.02F, col));
         }
      }
      return out;
   }

   private Scene buildTotem(LivingEntity target, float alpha, long now, float tickDelta) {
      Scene out = this.scene();
      int[] pal = this.colors(alpha);
      Vec3 base = this.lerped(target, tickDelta);
      float spin = (float)(now % 60000L) / 1000.0F;
      for (int i = 0; i < 4; i++) {
         double y = base.y + 0.16 + i * 0.3;
         double half = 0.13;
         double angle = spin * (0.6 + i * 0.25) + i;
         int col = ColorUtil.multiplyAlpha(i % 2 == 0 ? pal[0] : pal[1], alpha * 0.85F);
         Vec3[] corners = new Vec3[4];
         for (int c = 0; c < 4; c++) {
            double a = angle + c * Math.PI / 2.0;
            corners[c] = new Vec3(base.x + Math.cos(a) * half, y, base.z + Math.sin(a) * half);
         }
         out.quads().add(new Quad(corners[3], corners[2], corners[1], corners[0], col, col, 0.0F, 1.0F));
      }
      return out;
   }

   private Scene buildDome(LivingEntity target, float alpha, long now, float tickDelta) {
      Scene out = this.scene();
      int[] pal = this.colors(alpha);
      Vec3 base = this.lerped(target, tickDelta);
      Vec3 center = base.add(0.0, target.getBbHeight() * 0.5, 0.0);
      double radius = this.domeRadius.getValue();
      float spin = (float)(now % 60000L) / 1000.0F * 0.25F;
      int meridians = 6;
      int parallels = 5;
      int ringSteps = 30;
      int col = ColorUtil.multiplyAlpha(pal[0], alpha * 0.75F);
      int litCol = ColorUtil.multiplyAlpha(pal[1], alpha * 0.95F);
      for (int lat = 1; lat <= parallels; lat++) {
         double phi = lat * Math.PI / (parallels + 1.0);
         double ringRadius = radius * Math.sin(phi);
         double y = center.y + radius * Math.cos(phi);
         for (int i = 0; i < ringSteps; i++) {
            double a = i * Math.PI * 2.0 / ringSteps;
            Vec3 pos = new Vec3(center.x + Math.cos(a) * ringRadius, y, center.z + Math.sin(a) * ringRadius);
            out.dots().add(new Dot(pos, 0.06F, i % 5 == 0 ? litCol : col));
         }
      }
      for (int m = 0; m < meridians; m++) {
         double theta = m * Math.PI / meridians + spin;
         for (int i = 0; i <= ringSteps / 2; i++) {
            double phi = i * Math.PI / (ringSteps / 2.0);
            Vec3 pos = new Vec3(
               center.x + Math.sin(phi) * radius * Math.cos(theta),
               center.y + Math.cos(phi) * radius,
               center.z + Math.sin(phi) * radius * Math.sin(theta)
            );
            out.dots().add(new Dot(pos, 0.05F, col));
         }
      }
      out.dots().add(new Dot(base.add(0.0, 0.04, 0.0), 1.15F, ColorUtil.multiplyAlpha(pal[0], alpha * 0.4F)));
      return out;
   }

   private Scene buildSakura(LivingEntity target, float alpha, long now, float tickDelta) {
      Scene out = this.scene();
      int[] pal = this.sakuraColors(alpha);
      Vec3 base = this.lerped(target, tickDelta);
      double height = Math.max(1.4, target.getBbHeight() + 0.35);
      float t = (float)(now % 60000L) / 1000.0F;
      float speed = this.sakuraSpeed.getValue();
      float density = this.sakuraDensity.getValue();
      float hurt = this.hurtProgress;
      float expand = 1.0F + 0.22F * hurt;
      float yawRad = (float)Math.toRadians(mc.gameRenderer.mainCamera().yRot());
      float pitchRad = (float)Math.toRadians(mc.gameRenderer.mainCamera().xRot());
      double cy = Math.cos(yawRad);
      double sy = Math.sin(yawRad);
      double cp = Math.cos(pitchRad);
      double sp = Math.sin(pitchRad);
      Vec3 camRight = new Vec3(-cy, 0.0, -sy);
      Vec3 camUp = new Vec3(-sy * sp, cp, cy * sp);
      int stream = (int)(30.0F * density);

      for (int i = 0; i < stream; i++) {
         float p = (t * 0.16F * speed + (float)this.rand(i * 3 + 7)) % 1.0F;
         double angle = i * 2.39996323 + t * 0.85 * speed + p * 4.6;
         double radius = (0.42 + 0.5 * this.rand(i * 11 + 3) + 0.16 * Math.sin(angle * 2.0 + t)) * expand;
         Vec3 pos = base.add(Math.cos(angle) * radius, 0.06 + p * (height + 0.45), Math.sin(angle) * radius);
         float fade = (float)Math.sin(p * Math.PI) * 0.9F + 0.1F;
         float spin = (float)(this.rand(i * 5 + 1) * Math.PI * 2.0 + t * speed * (0.8 + this.rand(i) * 1.4) + hurt * 1.2);
         float size = 0.075F + 0.05F * (float)this.rand(i * 13 + 5);
         int core = ColorUtil.multiplyAlpha(pal[(int)(this.rand(i * 17 + 2) * 2.99)], alpha * fade * 0.92F);
         int edge = ColorUtil.multiplyAlpha(pal[3], alpha * fade * 0.75F);
         Vec3 dir = this.spun(camRight, camUp, spin);
         Vec3 perp = this.spun(camRight, camUp, spin + Math.PI / 2.0);
         this.addPetal(out, pos, dir, perp, size * 1.8F, size * 1.25F, ColorUtil.multiplyAlpha(core, 0.16F), ColorUtil.multiplyAlpha(edge, 0.12F));
         this.addPetal(out, pos, dir, perp, size, size * 0.72F, core, edge);
      }

      int drift = (int)(14.0F * density);
      if (drift > 0) {
         for (int i = 0; i < drift; i++) {
            double angle = i * 1.7 + t * 0.42 * speed * (0.7 + this.rand(i * 9));
            double radius = (0.32 + 0.42 * this.rand(i * 21 + 9)) * expand;
            double yLift = 0.25 + this.rand(i * 31 + 4) * (height - 0.35) + 0.07 * Math.sin(t * 1.1 + i);
            Vec3 pos = base.add(Math.cos(angle) * radius, yLift, Math.sin(angle) * radius);
            float spin = (float)(this.rand(i * 7) * Math.PI * 2.0 + t * speed * 0.55);
            float size = 0.07F + 0.045F * (float)this.rand(i * 23 + 8);
            int core = ColorUtil.multiplyAlpha(pal[i % 3], alpha * 0.8F);
            int edge = ColorUtil.multiplyAlpha(pal[3], alpha * 0.62F);
            Vec3 dir = this.spun(camRight, camUp, spin);
            Vec3 perp = this.spun(camRight, camUp, spin + Math.PI / 2.0);
            this.addPetal(out, pos, dir, perp, size * 1.7F, size * 1.2F, ColorUtil.multiplyAlpha(core, 0.14F), ColorUtil.multiplyAlpha(edge, 0.11F));
            this.addPetal(out, pos, dir, perp, size, size * 0.7F, core, edge);
         }
      }

      int ground = (int)(20.0F * density);
      if (ground > 0) {
         for (int i = 0; i < ground; i++) {
            double ringAngle = i * Math.PI * 2.0 / ground + t * 0.22 * speed;
            double radius = (0.55 + 0.14 * Math.sin(ringAngle * 3.0 + t * 0.6)) * expand;
            Vec3 pos = base.add(
               Math.cos(ringAngle) * radius, 0.035 + 0.03 * Math.sin(t * 1.3 + i * 0.8), Math.sin(ringAngle) * radius
            );
            double phi = ringAngle + 1.2 + this.rand(i * 4 + 2) * 1.4 + t * 0.35 * speed;
            double tilt = 0.22 + 0.28 * this.rand(i * 6 + 5) + 0.1 * Math.sin(t + i);
            Vec3 dir = new Vec3(Math.cos(phi) * Math.cos(tilt), Math.sin(tilt), Math.sin(phi) * Math.cos(tilt));
            Vec3 perp = new Vec3(-Math.sin(phi), 0.0, Math.cos(phi));
            float size = 0.085F + 0.05F * (float)this.rand(i * 8 + 3);
            int core = ColorUtil.multiplyAlpha(pal[1], alpha * 0.72F);
            int edge = ColorUtil.multiplyAlpha(pal[2], alpha * 0.85F);
            this.addPetal(out, pos, dir, perp, size * 1.6F, size * 1.15F, ColorUtil.multiplyAlpha(core, 0.15F), ColorUtil.multiplyAlpha(edge, 0.12F));
            this.addPetal(out, pos, dir, perp, size, size * 0.68F, core, edge);
         }
      }

      int dust = (int)(22.0F * density);
      if (dust > 0) {
         for (int i = 0; i < dust; i++) {
            float p = (t * 0.12F * speed + (float)this.rand(i * 15 + 7)) % 1.0F;
            double angle = i * 2.399 + t * 0.5 * speed;
            double radius = (0.3 + 0.5 * this.rand(i * 19 + 1)) * expand;
            Vec3 pos = base.add(
               Math.cos(angle) * radius + 0.07 * Math.sin(t * 1.7 + i),
               0.1 + p * (height + 0.3),
               Math.sin(angle) * radius + 0.07 * Math.cos(t * 1.4 + i * 1.3)
            );
            float fade = (float)Math.sin(p * Math.PI);
            float size = 0.03F + 0.035F * (float)this.rand(i * 27 + 6) * (1.2F - p);
            out.dots().add(new Dot(pos, size, ColorUtil.multiplyAlpha(pal[3], alpha * fade * 0.8F)));
         }
      }

      out.dots().add(new Dot(base.add(0.0, 0.05, 0.0), 1.35F, ColorUtil.multiplyAlpha(pal[0], alpha * 0.30F)));
      out.dots().add(new Dot(base.add(0.0, 0.06, 0.0), 0.85F, ColorUtil.multiplyAlpha(pal[1], alpha * 0.38F)));
      return out;
   }

   private int[] sakuraColors(float alpha) {
      int deep = ColorUtil.rgba(255, 112, 182, 255);
      int mid = ColorUtil.rgba(255, 156, 203, 255);
      int light = ColorUtil.rgba(255, 205, 228, 255);
      int white = ColorUtil.rgba(255, 238, 246, 255);
      if (this.hitFlash.isValue() && this.hurtProgress > 0.0F) {
         int flash = ColorUtil.rgba(255, 240, 248, 255);
         deep = ColorUtil.interpolate(deep, flash, this.hurtProgress * 0.85F);
         mid = ColorUtil.interpolate(mid, flash, this.hurtProgress * 0.85F);
         light = ColorUtil.interpolate(light, flash, this.hurtProgress * 0.85F);
      }

      int a = Mth.clamp((int)(255.0F * alpha), 0, 255);
      return new int[]{ColorUtil.withAlpha(deep, a), ColorUtil.withAlpha(mid, a), ColorUtil.withAlpha(light, a), ColorUtil.withAlpha(white, a)};
   }

   private Vec3 spun(Vec3 right, Vec3 up, double angle) {
      double c = Math.cos(angle);
      double s = Math.sin(angle);
      return right.scale(c).add(up.scale(s));
   }

   private void addPetal(Scene out, Vec3 pos, Vec3 dir, Vec3 perp, float halfLen, float halfWid, int core, int edge) {
      Vec3 a = pos.subtract(dir.scale(halfLen)).subtract(perp.scale(halfWid));
      Vec3 b = pos.add(dir.scale(halfLen)).subtract(perp.scale(halfWid));
      Vec3 c = pos.add(dir.scale(halfLen)).add(perp.scale(halfWid));
      Vec3 d = pos.subtract(dir.scale(halfLen)).add(perp.scale(halfWid));
      out.quads().add(new Quad(a, b, c, d, core, edge, 0.0F, 1.0F));
   }

   private void renderQuads3D(EventRender3D event, List<Quad> quads) {
      if (quads.isEmpty()) {
         return;
      }
      Pose entry = event.getMatrices().last();
      Vec3 camera = event.getCameraPos();
      boolean shaded = this.shaderStyle.isValue();
      VertexConsumer consumer = event.getConsumers().getBuffer(shaded ? TargetEspPipelines.ESP_FIELD_LAYER : TargetEspPipelines.RING_LAYER);
      for (Quad quad : quads) {
         if (shaded) {
            this.texturedQuad(
               consumer, entry,
               quad.a().subtract(camera), quad.b().subtract(camera), quad.c().subtract(camera), quad.d().subtract(camera),
               quad.u0(), 0.0F, quad.u1(), 1.0F, quad.ca(), quad.cb()
            );
         } else {
            this.quad(
               consumer, entry,
               quad.a().subtract(camera), quad.b().subtract(camera), quad.c().subtract(camera), quad.d().subtract(camera),
               quad.ca(), quad.cb(), quad.cb(), quad.ca()
            );
         }
      }
      event.getConsumers().draw(shaded ? TargetEspPipelines.ESP_FIELD_LAYER : TargetEspPipelines.RING_LAYER);
   }

   private void renderDots3D(EventRender3D event, List<Dot> dots) {
      if (dots.isEmpty()) {
         return;
      }
      RenderType layer = this.shaderStyle.isValue()
         ? TargetEspPipelines.ESP_PARTICLE_LAYER
         : TargetEspPipelines.MARKER_LAYER.apply(GHOST_TEXTURE);
      VertexConsumer consumer = event.getConsumers().getBuffer(layer);
      PoseStack matrices = event.getMatrices();
      Vec3 camera = event.getCameraPos();
      for (Dot dot : dots) {
         this.drawGhostSprite(matrices, consumer, dot.pos(), camera, dot.size(), dot.size(), 0.0F, dot.color());
      }
      event.getConsumers().draw(layer);
   }

   private void texturedQuad(VertexConsumer out, Pose e, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float u0, float v0, float u1, float v1, int ca, int cb) {
      out.addVertex(e, (float)a.x, (float)a.y, (float)a.z).setUv(u0, v0).setColor(ca);
      out.addVertex(e, (float)b.x, (float)b.y, (float)b.z).setUv(u1, v0).setColor(cb);
      out.addVertex(e, (float)c.x, (float)c.y, (float)c.z).setUv(u1, v1).setColor(cb);
      out.addVertex(e, (float)d.x, (float)d.y, (float)d.z).setUv(u0, v1).setColor(ca);
   }

   private void renderSceneOverlay(GuiGraphicsExtractor context, LivingEntity target, String currentMode, float alpha, float tickDelta, int width, int height, long now) {
      Scene scene = this.buildSceneFor(currentMode, target, alpha, now, tickDelta);
      if (scene == null) {
         return;
      }
      int quadStep = scene.quads().size() > 70 ? 2 : 1;
      for (int i = 0; i < scene.quads().size(); i += quadStep) {
         Quad quad = scene.quads().get(i);
         Vec3 midpoint = quad.a().add(quad.b()).add(quad.c()).add(quad.d()).scale(0.25);
         Vec2 point = RenderUtil3D.worldToScreen(midpoint.x, midpoint.y, midpoint.z, width, height);
         if (point != null) {
            RenderUtil.drawGlow(context, point.x - 4.0F, point.y - 4.0F, 8.0F, 8.0F, 4.0F, quad.ca(), 1.0F, 6.0F);
         }
      }
      int dotStep = scene.dots().size() > 90 ? 2 : 1;
      for (int i = 0; i < scene.dots().size(); i += dotStep) {
         Dot dot = scene.dots().get(i);
         Vec2 point = RenderUtil3D.worldToScreen(dot.pos().x, dot.pos().y, dot.pos().z, width, height);
         if (point == null) {
            continue;
         }
         float size = Mth.clamp(dot.size() * 90.0F, 3.0F, 30.0F);
         RenderUtil.drawGlow(context, point.x - size / 2.0F, point.y - size / 2.0F, size, size, size / 2.0F, dot.color(), 1.0F, size * 0.9F);
      }
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

   private void renderGlitchOverlay(GuiGraphicsExtractor context, LivingEntity target, float alpha, float tickDelta, int width, int height, long now) {
      float[] box = this.projectedBounds(target, tickDelta, width, height);
      if (box == null) {
         return;
      }
      float x0 = box[0];
      float y0 = box[1];
      float boxWidth = box[2] - box[0];
      float boxHeight = box[3] - box[1];
      int[] pal = this.colors(alpha);
      int redCopy = ColorUtil.multiplyAlpha(ColorUtil.rgba(251, 113, 133, 255), alpha * 0.45F);
      int cyanCopy = ColorUtil.multiplyAlpha(ColorUtil.rgba(34, 211, 238, 255), alpha * 0.45F);
      int bands = 8;
      int epoch = (int)(now / 450L);
      float bandHeight = boxHeight / bands;
      for (int i = 0; i < bands; i++) {
         float first = (float)this.rand(epoch * 13 + i * 7);
         float second = (float)this.rand(epoch * 31 + i * 11);
         float offset = first > 0.6F ? (second - 0.5F) * 26.0F : 0.0F;
         if (target.hurtTime > 0) {
            offset *= 2.5F;
         }
         float bandY = y0 + i * bandHeight;
         float bandThickness = Math.max(1.0F, bandHeight - 1.5F);
         RenderUtil.drawRoundRect(context, x0 + offset * 0.75F, bandY, boxWidth, bandThickness, 1.0F, redCopy);
         RenderUtil.drawRoundRect(context, x0 - offset * 0.75F, bandY, boxWidth, bandThickness, 1.0F, cyanCopy);
         RenderUtil.drawRoundRect(context, x0 + offset, bandY, boxWidth, bandThickness, 1.0F, ColorUtil.multiplyAlpha(pal[0], alpha * 0.2F));
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

   private void renderInfoPanel(GuiGraphicsExtractor context, LivingEntity target, float alpha, float tickDelta, int width, int height) {
      Vec3 center = this.centerOf(target, tickDelta);
      Vec2 point = RenderUtil3D.worldToScreen(center.x, center.y, center.z, width, height);
      if (point == null) {
         return;
      }
      float scale = this.infoScale.getValue();
      float panelWidth = 122.0F * scale;
      float panelHeight = 34.0F * scale;
      float panelX = Mth.clamp(point.x + 30.0F * scale, 4.0F, width - panelWidth - 4.0F);
      float panelY = Mth.clamp(point.y - panelHeight * 0.5F, 4.0F, height - panelHeight - 4.0F);
      int background = ColorUtil.multiplyAlpha(ClientColors.HUD_BACKGROUND, alpha);
      int outline = ColorUtil.multiplyAlpha(ClientColors.HUD_OUTLINE, alpha);
      int accent = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, alpha);
      RenderUtil.drawGlow(
         context, panelX, panelY, panelWidth, panelHeight, 6.0F * scale,
         ColorUtil.multiplyAlpha(ClientColors.HUD_SHADOW, alpha * 0.85F), 1.0F, 12.0F
      );
      RenderUtil.drawRoundRect(context, panelX - 1.0F, panelY - 1.0F, panelWidth + 2.0F, panelHeight + 2.0F, 7.0F * scale, outline);
      RenderUtil.drawRoundRect(context, panelX, panelY, panelWidth, panelHeight, 6.0F * scale, background);
      RenderUtil.drawRoundRect(context, panelX, panelY, 3.0F * scale, panelHeight, 1.5F * scale, accent);
      RenderUtil.drawGlow(context, point.x - 2.0F, point.y - 2.0F, 4.0F, 4.0F, 2.0F, accent, 1.0F, 6.0F);
      float contentX = panelX + 9.0F * scale;
      float contentRight = panelX + panelWidth - 8.0F * scale;
      float availableWidth = Math.max(10.0F, contentRight - contentX);
      int textColor = ColorUtil.multiplyAlpha(ClientColors.HUD_TEXT, alpha);
      int subtleColor = ColorUtil.multiplyAlpha(ClientColors.HUD_SECONDARY_TEXT, alpha);
      String name = target.getName().getString();
      RenderUtil.drawTextFadeRight(context, name, contentX, panelY + 6.0F * scale, textColor, GFonts.SEMIBOLD, 8.4F * scale, availableWidth, 8.0F);
      float maxHealth = Math.max(1.0F, target.getMaxHealth());
      float health = Mth.clamp(target.getHealth(), 0.0F, maxHealth);
      float ratio = health / maxHealth;
      float barY = panelY + 18.0F * scale;
      float barHeight = 2.8F * scale;
      RenderUtil.drawRoundRect(context, contentX, barY, availableWidth, barHeight, barHeight * 0.5F, ColorUtil.multiplyAlpha(ColorUtil.rgba(35, 38, 52, 255), alpha));
      int barColor = ratio > 0.5F
         ? ColorUtil.rgba(247, 206, 82, 255)
         : ColorUtil.rgba(255, 92, 92, 255);
      RenderUtil.drawRoundRect(context, contentX, barY, availableWidth * ratio, barHeight, barHeight * 0.5F, ColorUtil.multiplyAlpha(barColor, alpha));
      String healthText = Math.round(ratio * 100.0F) + "%";
      float healthWidth = RenderUtil.getTextWidth(healthText, GFonts.MEDIUM, 6.5F * scale);
      RenderUtil.drawText(context, healthText, contentRight - healthWidth, barY + 4.0F * scale, subtleColor, GFonts.MEDIUM, 6.5F * scale);
      float distance = mc.player.distanceTo(target);
      RenderUtil.drawText(
         context, String.format(java.util.Locale.ROOT, "%.1f m", distance), contentX, barY + 5.0F * scale,
         subtleColor, GFonts.MEDIUM, 6.5F * scale
      );
   }

   private float smootherStep(float x) {
      return x * x * x * (x * (x * 6.0F - 15.0F) + 10.0F);
   }
}
