package havoc.module.impl.visuals;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import havoc.event.impl.EventRender3D;
import havoc.mixin.PlayerEntityRendererAccessor;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.ColorSetting;
import havoc.module.setting.MultiSelectSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import havoc.render.Render3D;
import havoc.render.RenderUtil3D;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.lwjgl.opengl.GL11;

@ModuleInfo(name = "Chams", desc = "", category = Category.VISUALS)
public final class Chams extends Module {
   private static final String SHOW_PLAYERS = "Players";
   private static final String SHOW_SELF = "Self";
   private static final String MODE_FILLED = "Filled";
   private static final String MODE_FLAT = "Flat";
   private static final float LEG_TOP_EXTENSION = 0.11F;
   private static final float BODY_BOTTOM_EXTENSION = 0.05F;
   private static final float WAIST_CONNECTOR_HEIGHT = 0.08F;
   private static final float WAIST_FRONT_OVERLAY = 0.004F;
   private static final float OUTLINE_ALPHA = 0.16F;
   private static final float FILLED_FILL_ALPHA = 0.22F;
   private static final float FILLED_GLOW_ALPHA = 0.1F;
   private static final float FILLED_GLOW_OUTSET = 0.022F;
   private static final float MODEL_INSET = 0.0105F;
   private static final float DISTANCE_EFFECT_START = 14.0F;
   private static final float DISTANCE_EFFECT_END = 72.0F;
   private static final float DISTANCE_MIN_VISUAL_SCALE = 0.38F;
   private static final float DISTANCE_MAX_INSET_SCALE = 2.6F;
   private static final float DISTANCE_OUTLINE_CUTOFF = 22.0F;
   private static final long OCCLUSION_CACHE_NANOS = 120000000L;
   private static final double OCCLUSION_MOVEMENT_EPSILON_SQUARED = 0.04;
   private final MultiSelectSetting targets = this.registerSetting(
      new MultiSelectSetting("Targets", List.of("Players", "Self"), new LinkedHashSet<>(List.of("Players")))
   );
   private final SelectSetting mode = this.registerSetting(new SelectSetting("Mode", "Filled", List.of("Filled", "Flat")));
   private final SliderSetting flatAlpha = this.registerSetting(
      new SliderSetting("Flat Alpha", 100.0F, 0.0F, 100.0F, 1.0F, "%").visibleWhen(() -> "Flat".equals(this.mode.getValue()))
   );
   private final BooleanSetting throughWalls = this.registerSetting(new BooleanSetting("Through Walls", true));
   private final BooleanSetting showArmor = this.registerSetting(new BooleanSetting("Show Armor", true));
   private final SliderSetting armorAlpha = this.registerSetting(
      new SliderSetting("Armor Alpha", 45.0F, 0.0F, 100.0F, 1.0F, "%").visibleWhen(this.showArmor::isValue)
   );
   private final ColorSetting visibleColor = this.registerSetting(new ColorSetting("Visible Color", ClientColors.CATEGORY_ACCENT));
   private final ColorSetting hiddenColor = this.registerSetting(
      new ColorSetting("Hidden Color", ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, 0.45F)).visibleWhen(this.throughWalls::isValue)
   );
   private final List<Entity> renderTargets = new ArrayList<>();
   private final Set<Integer> renderTargetIds = new HashSet<>();
   private final Map<Integer, Chams.OcclusionState> occlusionCache = new HashMap<>();

   @Override
   public void onDisable() {
      super.onDisable();
      this.renderTargets.clear();
      this.renderTargetIds.clear();
      this.occlusionCache.clear();
   }

   @Subscribe
   public void onRender3D(EventRender3D event) {
      if (event != null && mc != null && mc.player != null && mc.level != null) {
         PoseStack matrices = event.getMatrices();
         MultiBufferSource.BufferSource consumers = event.getConsumers();
         Vec3 camera = event.getCameraPos();
         if (matrices != null && consumers != null && camera != null) {
            this.renderTargets.clear();
            this.renderTargetIds.clear();

            for (Entity entity : mc.level.entitiesForRendering()) {
               if (this.shouldRender(entity)) {
                  this.renderTargets.add(entity);
                  this.renderTargetIds.add(entity.getId());
               }
            }

            this.occlusionCache.keySet().removeIf(id -> !this.renderTargetIds.contains(id));
            boolean filledMode = "Filled".equals(this.mode.getValue());
            boolean flatMode = "Flat".equals(this.mode.getValue());
            int visibleBaseColor = this.visibleColor.getColor();
            int hiddenBaseColor = this.hiddenColor.getColor();
            int visibleFillColor = flatMode ? ColorUtil.withAlpha(visibleBaseColor, this.getFlatAlpha()) : ColorUtil.multiplyAlpha(visibleBaseColor, 0.22F);
            int hiddenFillColor = flatMode ? ColorUtil.withAlpha(hiddenBaseColor, this.getFlatAlpha()) : ColorUtil.multiplyAlpha(hiddenBaseColor, 0.22F);
            int visibleLineColor = filledMode ? this.createOutlineColor(visibleBaseColor, 1.0F) : 0;
            int hiddenLineColor = filledMode ? this.createOutlineColor(hiddenBaseColor, 1.0F) : 0;
            GL11.glEnable(2848);
            GL11.glHint(3154, 4354);

            try {
               if (this.throughWalls.isValue()) {
                  GL11.glDisable(2929);
                  this.renderEntities(event, true, filledMode, hiddenFillColor, hiddenLineColor, true);
                  consumers.draw();
                  GL11.glEnable(2929);
               }

               this.renderEntities(event, true, filledMode, visibleFillColor, visibleLineColor, false);
            } finally {
               GL11.glDisable(2848);
            }
         }
      }
   }

   private void renderEntities(EventRender3D event, boolean renderFilled, boolean renderOutline, int fillColor, int lineColor, boolean hiddenPass) {
      PoseStack matrices = event.getMatrices();
      MultiBufferSource.BufferSource consumers = event.getConsumers();
      Vec3 camera = event.getCameraPos();

      for (Entity entity : this.renderTargets) {
         if (!hiddenPass || this.shouldRenderHiddenPass(entity, camera)) {
            if (entity instanceof Player player) {
               this.renderPlayerChams(player, event.getTickDelta(), camera, matrices, consumers, renderFilled, renderOutline, fillColor, lineColor);
            } else {
               this.renderBox(
                  matrices,
                  consumers,
                  this.getInterpolatedCameraRelativeBox(entity, event.getTickDelta(), camera),
                  renderFilled,
                  renderOutline,
                  fillColor,
                  lineColor
               );
            }
         }
      }
   }

   private void renderPlayerChams(
      Player player,
      float tickDelta,
      Vec3 camera,
      PoseStack matrices,
      MultiBufferSource.BufferSource consumers,
      boolean renderFilled,
      boolean renderOutline,
      int fillColor,
      int lineColor
   ) {
      if (mc.getEntityRenderDispatcher().getRenderer(player) instanceof AvatarRenderer playerRenderer) {
         AvatarRenderState state = playerRenderer.createRenderState();
         playerRenderer.extractRenderState(player, state, tickDelta);
         Vec3 interpolated = RenderUtil3D.getInterpolatedPos(player, tickDelta);
         float distance = (float)interpolated.distanceTo(camera);
         float distanceVisualScale = this.getDistanceVisualScale(distance);
         float modelInset = 0.0105F * this.getDistanceInsetScale(distance);
         int adjustedFillColor = this.scaleColorAlpha(fillColor, this.getDistanceFillAlphaMultiplier(distance));
         int adjustedLineColor = this.scaleColorAlpha(lineColor, distanceVisualScale);
         boolean allowOutlinePass = distance <= 22.0F;
         Vec3 offset = playerRenderer.getRenderOffset(state);
         PlayerModel model = (PlayerModel)playerRenderer.getModel();
         PlayerEntityRendererAccessor accessor = (PlayerEntityRendererAccessor)playerRenderer;
         matrices.pushPose();
         matrices.translate(
            (float)(interpolated.x - camera.x + offset.x), (float)(interpolated.y - camera.y + offset.y), (float)(interpolated.z - camera.z + offset.z)
         );
         float baseScale = state.scale;
         matrices.scale(baseScale, baseScale, baseScale);
         accessor.havoc$setupTransforms(state, matrices, state.bodyRot, baseScale);
         matrices.scale(-1.0F, -1.0F, 1.0F);
         accessor.havoc$scale(state, matrices);
         matrices.translate(0.0F, -1.501F, 0.0F);
         model.setupAnim(state);
         if (renderFilled) {
            this.renderPlayerModelParts(matrices, model, consumers.getBuffer(RenderTypes.debugQuads()), true, adjustedFillColor, modelInset);
         }

         if (renderOutline && allowOutlinePass) {
            this.renderPlayerModelParts(matrices, model, consumers.getBuffer(RenderTypes.lines()), false, adjustedLineColor, modelInset);
            this.renderSubtleFilledGlow(matrices, model, consumers.getBuffer(RenderTypes.debugQuads()), adjustedFillColor, distanceVisualScale, modelInset);
         }

         matrices.popPose();
      } else {
         this.renderBox(
            matrices, consumers, this.getInterpolatedCameraRelativeBox(player, tickDelta, camera), renderFilled, renderOutline, fillColor, lineColor
         );
      }
   }

   private void renderBox(
      PoseStack matrices, MultiBufferSource.BufferSource consumers, AABB box, boolean renderFilled, boolean renderOutline, int fillColor, int lineColor
   ) {
      if (renderFilled) {
         VertexConsumer filledConsumer = consumers.getBuffer(RenderTypes.debugQuads());
         Render3D.drawFilledBox(matrices, filledConsumer, box, fillColor);
      }

      if (renderOutline) {
         VertexConsumer lineConsumer = consumers.getBuffer(RenderTypes.lines());
         Render3D.drawOutlinedBox(matrices, lineConsumer, box, lineColor);
      }
   }

   private void renderPlayerModelParts(PoseStack matrices, PlayerModel model, VertexConsumer consumer, boolean filled, int color, float inset) {
      this.renderModelPart(matrices, model.head, consumer, filled, color, 0.0F, 0.0F, inset);
      this.renderModelPart(matrices, model.body, consumer, filled, color, 0.0F, 0.05F, inset);
      this.renderModelPart(matrices, model.leftArm, consumer, filled, color, 0.0F, 0.0F, inset);
      this.renderModelPart(matrices, model.rightArm, consumer, filled, color, 0.0F, 0.0F, inset);
      this.renderModelPart(matrices, model.leftLeg, consumer, filled, color, 0.11F, 0.0F, inset);
      this.renderModelPart(matrices, model.rightLeg, consumer, filled, color, 0.11F, 0.0F, inset);
      if (filled) {
         this.renderWaistConnector(matrices, model, consumer, color, inset);
      }
   }

   private void renderSubtleFilledGlow(PoseStack matrices, PlayerModel model, VertexConsumer consumer, int color, float distanceVisualScale, float inset) {
      float glowScale = Math.max(0.0F, distanceVisualScale - 0.08F);
      if (!(glowScale <= 0.0F)) {
         float glowOutset = 0.022F * glowScale;
         int glowColor = ColorUtil.withAlpha(this.createOutlineColor(color, glowScale), Math.max(1, Math.round(25.5F * glowScale)));
         this.renderGlowPart(matrices, model.head, consumer, glowColor, glowOutset, glowOutset, inset);
         this.renderGlowPart(matrices, model.body, consumer, glowColor, glowOutset, glowOutset + 0.004F * glowScale, inset);
         this.renderGlowPart(matrices, model.leftArm, consumer, glowColor, glowOutset, glowOutset, inset);
         this.renderGlowPart(matrices, model.rightArm, consumer, glowColor, glowOutset, glowOutset, inset);
         this.renderGlowPart(matrices, model.leftLeg, consumer, glowColor, 0.11F + glowOutset, glowOutset, inset);
         this.renderGlowPart(matrices, model.rightLeg, consumer, glowColor, 0.11F + glowOutset, glowOutset, inset);
      }
   }

   private void renderGlowPart(PoseStack matrices, ModelPart part, VertexConsumer consumer, int color, float topExtension, float bottomExtension, float inset) {
      if (part != null) {
         part.visit(
            matrices,
            (entry, path, index, cuboid) -> this.addFilledCuboid(
               entry,
               consumer,
               cuboid.minX / 16.0F - 0.022F + inset,
               cuboid.minY / 16.0F - topExtension + inset,
               cuboid.minZ / 16.0F - 0.022F + inset,
               cuboid.maxX / 16.0F + 0.022F - inset,
               cuboid.maxY / 16.0F + bottomExtension - inset,
               cuboid.maxZ / 16.0F + 0.022F - inset,
               color
            )
         );
      }
   }

   private void renderWaistConnector(PoseStack matrices, PlayerModel model, VertexConsumer consumer, int color, float inset) {
      if (model.body != null && model.leftLeg != null && model.rightLeg != null) {
         model.body
            .visit(
               matrices,
               (bodyEntry, bodyPath, bodyIndex, bodyCuboid) -> model.leftLeg
                  .visit(
                     matrices,
                     (leftEntry, leftPath, leftIndex, leftCuboid) -> model.rightLeg.visit(matrices, (rightEntry, rightPath, rightIndex, rightCuboid) -> {
                        float bodyMinX = bodyCuboid.minX / 16.0F + inset;
                        float bodyMaxX = bodyCuboid.maxX / 16.0F - inset;
                        float bodyMaxY = bodyCuboid.maxY / 16.0F + 0.05F - inset;
                        float bodyMinZ = bodyCuboid.minZ / 16.0F + inset;
                        float bodyMaxZ = bodyCuboid.maxZ / 16.0F - inset;
                        float connectorTop = Math.min(leftCuboid.minY / 16.0F - 0.11F, rightCuboid.minY / 16.0F - 0.11F) + 0.08F - inset;
                        if (!(connectorTop <= bodyMaxY)) {
                           this.addFilledCuboid(bodyEntry, consumer, bodyMinX, bodyMaxY, bodyMinZ, bodyMaxX, connectorTop, bodyMaxZ, color);
                           this.addFrontWaistOverlay(bodyEntry, consumer, bodyMinX, bodyMaxY, bodyMaxX, connectorTop, bodyMaxZ + 0.004F, color);
                        }
                     })
                  )
            );
      }
   }

   private void renderModelPart(
      PoseStack matrices, ModelPart part, VertexConsumer consumer, boolean filled, int color, float topExtension, float bottomExtension, float inset
   ) {
      if (part != null && consumer != null) {
         part.visit(matrices, (entry, path, index, cuboid) -> {
            if (filled) {
               this.drawCuboidFill(entry, consumer, cuboid, color, topExtension, bottomExtension, inset);
            } else {
               this.drawCuboidOutline(entry, consumer, cuboid, color, topExtension, bottomExtension, inset);
            }
         });
      }
   }

   private void drawCuboidFill(Pose entry, VertexConsumer consumer, Cube cuboid, int color, float topExtension, float bottomExtension, float inset) {
      float minX = cuboid.minX / 16.0F + inset;
      float minY = cuboid.minY / 16.0F - topExtension + inset;
      float minZ = cuboid.minZ / 16.0F + inset;
      float maxX = cuboid.maxX / 16.0F - inset;
      float maxY = cuboid.maxY / 16.0F + bottomExtension - inset;
      float maxZ = cuboid.maxZ / 16.0F - inset;
      if (!(minX >= maxX) && !(minY >= maxY) && !(minZ >= maxZ)) {
         this.addFilledCuboid(entry, consumer, minX, minY, minZ, maxX, maxY, maxZ, color);
      }
   }

   private void drawCuboidOutline(Pose entry, VertexConsumer consumer, Cube cuboid, int color, float topExtension, float bottomExtension, float inset) {
      float minX = cuboid.minX / 16.0F + inset;
      float minY = cuboid.minY / 16.0F - topExtension + inset;
      float minZ = cuboid.minZ / 16.0F + inset;
      float maxX = cuboid.maxX / 16.0F - inset;
      float maxY = cuboid.maxY / 16.0F + bottomExtension - inset;
      float maxZ = cuboid.maxZ / 16.0F - inset;
      if (!(minX >= maxX) && !(minY >= maxY) && !(minZ >= maxZ)) {
         float r = ColorUtil.red(color) / 255.0F;
         float g = ColorUtil.green(color) / 255.0F;
         float b = ColorUtil.blue(color) / 255.0F;
         float a = ColorUtil.alpha(color) / 255.0F;
         this.addOutlineLine(consumer, entry, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
         this.addOutlineLine(consumer, entry, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
         this.addOutlineLine(consumer, entry, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
         this.addOutlineLine(consumer, entry, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
         this.addOutlineLine(consumer, entry, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
         this.addOutlineLine(consumer, entry, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
         this.addOutlineLine(consumer, entry, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
         this.addOutlineLine(consumer, entry, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
         this.addOutlineLine(consumer, entry, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
         this.addOutlineLine(consumer, entry, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
         this.addOutlineLine(consumer, entry, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
         this.addOutlineLine(consumer, entry, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
      }
   }

   private void addFilledCuboid(Pose entry, VertexConsumer consumer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int color) {
      float r = ColorUtil.red(color) / 255.0F;
      float g = ColorUtil.green(color) / 255.0F;
      float b = ColorUtil.blue(color) / 255.0F;
      float a = ColorUtil.alpha(color) / 255.0F;
      this.addQuad(entry, consumer, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
      this.addQuad(entry, consumer, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, r, g, b, a);
      this.addQuad(entry, consumer, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, r, g, b, a);
      this.addQuad(entry, consumer, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
      this.addQuad(entry, consumer, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, a);
      this.addQuad(entry, consumer, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);
   }

   private void addFrontWaistOverlay(Pose entry, VertexConsumer consumer, float minX, float minY, float maxX, float maxY, float frontZ, int color) {
      float r = ColorUtil.red(color) / 255.0F;
      float g = ColorUtil.green(color) / 255.0F;
      float b = ColorUtil.blue(color) / 255.0F;
      float a = ColorUtil.alpha(color) / 255.0F;
      this.addQuad(entry, consumer, minX, maxY, frontZ, maxX, maxY, frontZ, maxX, minY, frontZ, minX, minY, frontZ, r, g, b, a);
   }

   private void addOutlineLine(
      VertexConsumer consumer, Pose entry, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a
   ) {
      float nx = x2 - x1;
      float ny = y2 - y1;
      float nz = z2 - z1;
      float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
      if (length > 1.0E-4F) {
         nx /= length;
         ny /= length;
         nz /= length;
      } else {
         nx = 0.0F;
         ny = 1.0F;
         nz = 0.0F;
      }

      consumer.addVertex(entry, x1, y1, z1).setColor(r, g, b, a).setLineWidth(1.0F).setNormal(entry, nx, ny, nz);
      consumer.addVertex(entry, x2, y2, z2).setColor(r, g, b, a).setLineWidth(1.0F).setNormal(entry, nx, ny, nz);
   }

   private void addQuad(
      Pose entry,
      VertexConsumer consumer,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float x4,
      float y4,
      float z4,
      float r,
      float g,
      float b,
      float a
   ) {
      consumer.addVertex(entry, x1, y1, z1).setColor(r, g, b, a);
      consumer.addVertex(entry, x2, y2, z2).setColor(r, g, b, a);
      consumer.addVertex(entry, x3, y3, z3).setColor(r, g, b, a);
      consumer.addVertex(entry, x4, y4, z4).setColor(r, g, b, a);
   }

   private int createOutlineColor(int baseColor, float alphaMultiplier) {
      int brightened = ColorUtil.rgba(
         Math.min(255, Math.round(ColorUtil.red(baseColor) * 1.25F + 10.0F)),
         Math.min(255, Math.round(ColorUtil.green(baseColor) * 1.25F + 10.0F)),
         Math.min(255, Math.round(ColorUtil.blue(baseColor) * 1.25F + 10.0F)),
         255
      );
      int alpha = Math.max(1, Math.round(40.8F * Math.max(0.0F, Math.min(1.0F, alphaMultiplier))));
      return brightened & 16777215 | alpha << 24;
   }

   private int scaleColorAlpha(int color, float multiplier) {
      if (color == 0) {
         return 0;
      }

      int alpha = Math.max(0, Math.min(255, Math.round(ColorUtil.alpha(color) * Math.max(0.0F, Math.min(1.0F, multiplier)))));
      return color & 16777215 | alpha << 24;
   }

   private float getDistanceVisualScale(float distance) {
      float normalized = Mth.clamp((distance - 14.0F) / 58.0F, 0.0F, 1.0F);
      return Mth.lerp(normalized, 1.0F, 0.38F);
   }

   private float getDistanceFillAlphaMultiplier(float distance) {
      float normalized = Mth.clamp((distance - 14.0F) / 58.0F, 0.0F, 1.0F);
      return Mth.lerp(normalized, 1.0F, 0.7F);
   }

   private float getDistanceInsetScale(float distance) {
      float normalized = Mth.clamp((distance - 14.0F) / 58.0F, 0.0F, 1.0F);
      return Mth.lerp(normalized, 1.0F, 2.6F);
   }

   private boolean shouldRender(Entity entity) {
      if (entity == null || !entity.isAlive() || entity instanceof ArmorStand) {
         return false;
      } else {
         return entity == mc.player
            ? this.targets.isSelected("Self") && (mc.options == null || !mc.options.getCameraType().isFirstPerson())
            : entity instanceof Player && this.targets.isSelected("Players");
      }
   }

   public boolean shouldHideVanillaModel(Entity entity) {
      return this.isEnable() && this.shouldRender(entity);
   }

   public boolean isArmorVisible() {
      return this.showArmor.isValue();
   }

   public boolean isThroughWallsEnabled() {
      return this.throughWalls.isValue();
   }

   public int getArmorAlpha() {
      return Math.round(this.armorAlpha.getValue() / 100.0F * 255.0F);
   }

   public boolean isArmorFullyOpaque() {
      return this.armorAlpha.getValue() >= 100.0F;
   }

   private int getFlatAlpha() {
      return Math.max(0, Math.min(255, Math.round(this.flatAlpha.getValue() / 100.0F * 255.0F)));
   }

   private boolean shouldRenderHiddenPass(Entity entity, Vec3 cameraPos) {
      if (mc != null && mc.level != null && mc.player != null && entity != null && cameraPos != null) {
         Vec3 target = entity.getBoundingBox().getCenter();
         long now = System.nanoTime();
         Chams.OcclusionState cached = this.occlusionCache.get(entity.getId());
         if (cached != null
            && now - cached.checkedNanos() < 120000000L
            && cached.cameraDistanceSquared(cameraPos) < 0.04
            && cached.targetDistanceSquared(target) < 0.04) {
            return cached.hidden();
         }

         BlockHitResult hit = mc.level.clip(new ClipContext(cameraPos, target, Block.COLLIDER, Fluid.NONE, mc.player));
         boolean hidden = hit.getType() != Type.MISS;
         this.occlusionCache.put(entity.getId(), new Chams.OcclusionState(now, cameraPos.x, cameraPos.y, cameraPos.z, target.x, target.y, target.z, hidden));
         return hidden;
      } else {
         return false;
      }
   }

   private AABB getInterpolatedCameraRelativeBox(Entity entity, float tickDelta, Vec3 camera) {
      Vec3 interpolated = RenderUtil3D.getInterpolatedPos(entity, tickDelta);
      Vec3 delta = interpolated.subtract(entity.getX(), entity.getY(), entity.getZ());
      return entity.getBoundingBox().move(delta).move(-camera.x, -camera.y, -camera.z).inflate(0.01);
   }

   private record OcclusionState(
      long checkedNanos, double cameraX, double cameraY, double cameraZ, double targetX, double targetY, double targetZ, boolean hidden
   ) {
      private double cameraDistanceSquared(Vec3 position) {
         return squaredDistance(this.cameraX, this.cameraY, this.cameraZ, position);
      }

      private double targetDistanceSquared(Vec3 position) {
         return squaredDistance(this.targetX, this.targetY, this.targetZ, position);
      }

      private static double squaredDistance(double x, double y, double z, Vec3 position) {
         double dx = x - position.x;
         double dy = y - position.y;
         double dz = z - position.z;
         return dx * dx + dy * dy + dz * dz;
      }
   }
}
