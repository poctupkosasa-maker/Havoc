package havoc.module.impl.visuals;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import havoc.event.impl.EventRender3D;
import havoc.event.impl.EventScreenRender;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.ColorSetting;
import havoc.module.setting.MultiSelectSetting;
import havoc.render.Render3D;
import havoc.render.RenderUtil;
import havoc.render.RenderUtil3D;
import havoc.render.font.GFonts;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Matrix3x2fStack;
import org.lwjgl.opengl.GL11;

@ModuleInfo(name = "Predictions", desc = "", category = Category.VISUALS)
public final class Predictions extends Module {
   private static final String TYPE_PEARL = "Ender Pearl";
   private static final String TYPE_TRIDENT = "Trident";
   private static final String TYPE_SNOWBALL = "Snowball";
   private static final String TYPE_POTION = "Potion";
   private static final int MAX_PREDICT_TICKS = 150;
   private static final int MAX_RENDERED_PROJECTILES = 96;
   private static final double MAX_PROJECTILE_RENDER_DISTANCE = 160.0;
   private static final float LINE_ALPHA = 0.96F;
   private static final float NAME_TEXT_SIZE = 5.0F;
   private static final float TIME_TEXT_SIZE = 4.8F;
   private static final float TAG_PAD_X = 4.0F;
   private static final float TAG_PAD_Y = 3.0F;
   private static final float TAG_GAP = 1.0F;
   private static final float TAG_BLOCK_GAP = 2.0F;
   private static final float TAG_ICON_SIZE = 8.0F;
   private static final float TAG_HEAD_SIZE = 8.0F;
   private static final float TAG_OFFSET_Y = 5.0F;
   private static final float LANDING_RING_SIZE = 8.0F;
   private static final float WORLD_PATH_SMOOTHING = 0.3F;
   private static final double WORLD_PATH_DEAD_ZONE = 0.0055;
   private static final float WORLD_LANDING_SMOOTHING = 0.24F;
   private static final double WORLD_LANDING_DEAD_ZONE = 0.0045;
   private static final float TAG_SMOOTHING = 0.22F;
   private static final float TAG_DEAD_ZONE = 0.25F;
   private static ItemStack pearlStack;
   private static ItemStack tridentStack;
   private static ItemStack snowballStack;
   private static ItemStack potionStack;
   private final MultiSelectSetting projectileTypes = this.registerSetting(
      new MultiSelectSetting("Projectiles", List.of("Ender Pearl", "Trident", "Snowball", "Potion"), new LinkedHashSet<>(List.of("Ender Pearl")))
   );
   private final BooleanSetting showShadow = this.registerSetting(new BooleanSetting("Shadow", true));
   private final ColorSetting color = this.registerSetting(new ColorSetting("Color", ClientColors.CATEGORY_ACCENT));
   private final Map<Integer, Predictions.OwnerSnapshot> ownerCache = new HashMap<>();
   private final Map<Integer, Vec2> landingPointCache = new HashMap<>();
   private final Map<Integer, List<Vec3>> stablePathCache = new HashMap<>();
   private final Map<Integer, Vec3> stableLandingCache = new HashMap<>();
   private final Map<Integer, Integer> projectileLifetimeCache = new HashMap<>();
   private final Map<Integer, Predictions.CachedTrajectory> trajectoryCache = new HashMap<>();
   private final Map<Integer, Predictions.PredictionFrameData> frameData = new LinkedHashMap<>();
   private final Set<Integer> aliveEntityIds = new HashSet<>();

   @Override
   public void onDisable() {
      this.ownerCache.clear();
      this.landingPointCache.clear();
      this.stablePathCache.clear();
      this.stableLandingCache.clear();
      this.projectileLifetimeCache.clear();
      this.trajectoryCache.clear();
      this.frameData.clear();
      this.aliveEntityIds.clear();
      super.onDisable();
   }

   @Subscribe
   public void onRender3D(EventRender3D event) {
      if (event != null && mc.level != null && mc.player != null) {
         VertexConsumer lineConsumer = event.getConsumers().getBuffer(RenderTypes.lines());
         Vec3 camera = event.getCameraPos();
         float tickDelta = event.getTickDelta();
         int lineColor = this.withAlpha(this.color.getColor(), 0.96F);
         this.frameData.clear();
         this.aliveEntityIds.clear();
         int renderedCount = 0;

         for (Entity projectile : mc.level.entitiesForRendering()) {
            if (renderedCount >= 96) {
               break;
            }

            Predictions.ProjectileKind kind = this.resolveKind(projectile);
            if (kind != null && projectile.isAlive() && !(mc.player.distanceToSqr(projectile) > 25600.0)) {
               Predictions.CachedTrajectory result = this.getTrajectory(projectile, tickDelta, kind);
               if (result.path().size() >= 2) {
                  renderedCount++;
                  int entityId = projectile.getId();
                  this.aliveEntityIds.add(entityId);
                  this.cacheOwnerIfKnown(projectile);
                  this.drawPath3D(event, lineConsumer, camera, result.path(), lineColor);
                  Predictions.OwnerInfo owner = this.resolveOwner(projectile);
                  int ticksRemaining = Math.max(0, result.ticksRemaining());
                  int maxLifetimeTicks = Math.max(this.projectileLifetimeCache.getOrDefault(entityId, 0), ticksRemaining);
                  this.projectileLifetimeCache.put(entityId, maxLifetimeTicks);
                  this.frameData
                     .put(entityId, new Predictions.PredictionFrameData(entityId, result.landingPos(), ticksRemaining, maxLifetimeTicks, owner, kind));
               }
            }
         }

         this.ownerCache.keySet().removeIf(id -> !this.aliveEntityIds.contains(id));
         this.landingPointCache.keySet().removeIf(id -> !this.aliveEntityIds.contains(id));
         this.stablePathCache.keySet().removeIf(id -> !this.aliveEntityIds.contains(id));
         this.stableLandingCache.keySet().removeIf(id -> !this.aliveEntityIds.contains(id));
         this.projectileLifetimeCache.keySet().removeIf(id -> !this.aliveEntityIds.contains(id));
         this.trajectoryCache.keySet().removeIf(id -> !this.aliveEntityIds.contains(id));
         if (renderedCount > 0) {
            GL11.glDisable(2929);

            try {
               event.getConsumers().draw(RenderTypes.lines());
            } finally {
               GL11.glEnable(2929);
            }
         }
      }
   }

   @Subscribe
   public void onRender2D(EventScreenRender event) {
      if (event != null && event.getStage() == EventScreenRender.Stage.ESP) {
         if (mc.level != null && mc.player != null && RenderUtil3D.hasFrame()) {
            GuiGraphicsExtractor context = event.getContext();
            if (context != null && !this.frameData.isEmpty()) {
               int screenWidth = mc.getWindow().getGuiScaledWidth();
               int screenHeight = mc.getWindow().getGuiScaledHeight();

               for (Predictions.PredictionFrameData data : this.frameData.values()) {
                  this.drawLandingInfo(context, data, screenWidth, screenHeight);
               }
            }
         }
      }
   }

   private Predictions.ProjectileKind resolveKind(Entity entity) {
      if (entity instanceof ThrownEnderpearl && this.projectileTypes.isSelected("Ender Pearl")) {
         return Predictions.ProjectileKind.PEARL;
      } else if (entity instanceof ThrownTrident && this.projectileTypes.isSelected("Trident")) {
         return Predictions.ProjectileKind.TRIDENT;
      } else if (entity instanceof Snowball && this.projectileTypes.isSelected("Snowball")) {
         return Predictions.ProjectileKind.SNOWBALL;
      } else {
         return entity instanceof AbstractThrownPotion && this.projectileTypes.isSelected("Potion") ? Predictions.ProjectileKind.POTION : null;
      }
   }

   private Predictions.CachedTrajectory getTrajectory(Entity projectile, float tickDelta, Predictions.ProjectileKind kind) {
      int entityId = projectile.getId();
      long worldTick = mc.level.getGameTime();
      Predictions.CachedTrajectory cached = this.trajectoryCache.get(entityId);
      if (cached != null && cached.worldTick() == worldTick && cached.kind() == kind) {
         return cached;
      }

      Predictions.TrajectoryResult simulated = this.simulateProjectile(mc.level, projectile, 150, tickDelta, kind.drag, kind.gravity);
      List<Vec3> path = simulated.points().size() < 2 ? simulated.points() : this.stabilizeWorldPath(entityId, simulated.points());
      Vec3 landing = simulated.points().size() < 2 ? simulated.landingPos() : this.stabilizeWorldLanding(entityId, simulated.landingPos());
      Predictions.CachedTrajectory updated = new Predictions.CachedTrajectory(worldTick, kind, path, landing, simulated.ticksRemaining());
      this.trajectoryCache.put(entityId, updated);
      return updated;
   }

   private void drawPath3D(EventRender3D event, VertexConsumer lineConsumer, Vec3 camera, List<Vec3> points, int color) {
      if (event != null && lineConsumer != null && camera != null && points != null && points.size() >= 2) {
         for (int i = 0; i < points.size() - 1; i++) {
            Vec3 from = points.get(i).subtract(camera);
            Vec3 to = points.get(i + 1).subtract(camera);
            Render3D.drawLine(event.getMatrices(), lineConsumer, from, to, color);
         }
      }
   }

   private void drawLandingInfo(GuiGraphicsExtractor context, Predictions.PredictionFrameData data, int screenWidth, int screenHeight) {
      if (data != null && data.landingPos() != null) {
         Vec3 landingPos = data.landingPos();
         Vec2 landingScreenPos = RenderUtil3D.worldToScreen(landingPos.x, landingPos.y + 0.15, landingPos.z, screenWidth, screenHeight);
         if (landingScreenPos != null) {
            landingScreenPos = this.stabilizeLandingPoint(data.entityId(), landingScreenPos, data.ticksRemaining() <= 1);
            String ownerName = data.owner().name();

            ItemStack iconStack = switch (data.kind()) {
               case PEARL -> pearlStack != null ? pearlStack : (pearlStack = new ItemStack(Items.ENDER_PEARL));
               case TRIDENT -> tridentStack != null ? tridentStack : (tridentStack = new ItemStack(Items.TRIDENT));
               case SNOWBALL -> snowballStack != null ? snowballStack : (snowballStack = new ItemStack(Items.SNOWBALL));
               case POTION -> potionStack != null ? potionStack : (potionStack = new ItemStack(Items.SPLASH_POTION));
            };
            float nameTextHeight = RenderUtil.getTextHeight(GFonts.MEDIUM, 5.0F);
            float ownerWidth = RenderUtil.getTextWidth(ownerName, GFonts.MEDIUM, 5.0F);
            float mainContentW = 9.0F + ownerWidth;
            float mainRowH = Math.max(8.0F, nameTextHeight);
            float mainW = 8.0F + mainContentW;
            float mainH = 6.0F + mainRowH;
            float secondaryContentW = 17.0F;
            float secondaryRowH = Math.max(8.0F, 8.0F);
            float secondaryW = 8.0F + secondaryContentW;
            float secondaryH = 6.0F + secondaryRowH;
            float baseX = this.stabilizeCoord(landingScreenPos.x);
            float mainX = this.stabilizeCoord(baseX - mainW * 0.5F);
            float mainY = this.stabilizeCoord(landingScreenPos.y + 5.0F);
            float secondaryX = this.stabilizeCoord(baseX - secondaryW * 0.5F);
            float secondaryY = this.stabilizeCoord(mainY + mainH + 2.0F);
            if (this.showShadow.isValue()) {
               RenderUtil.drawShadow(context, mainX, mainY, mainW, mainH, 3.0F, ClientColors.MENU_SHADOW, 1.0F, 9.0F, 0.0F, 0.0F);
               RenderUtil.drawShadow(context, secondaryX, secondaryY, secondaryW, secondaryH, 3.0F, ClientColors.MENU_SHADOW, 1.0F, 9.0F, 0.0F, 0.0F);
            }

            RenderUtil.drawRoundRect(context, mainX, mainY, mainW, mainH, 3.0F, ClientColors.MENU_BACKGROUND);
            RenderUtil.drawRoundRect(context, secondaryX, secondaryY, secondaryW, secondaryH, 3.0F, ClientColors.MENU_BACKGROUND);
            float mainRowY = this.stabilizeCoord(mainY + 3.0F);
            float headX = this.stabilizeCoord(mainX + 4.0F);
            float headY = this.stabilizeCoord(mainRowY + (mainRowH - 8.0F) * 0.5F);
            this.drawPlayerHead(context, data.owner().player(), data.owner().uuid(), headX, headY, 8.0F);
            float ownerX = this.stabilizeCoord(headX + 8.0F + 1.0F);
            float ownerY = this.stabilizeCoord(mainRowY + (mainRowH - nameTextHeight) * 0.5F + 1.0F);
            RenderUtil.drawText(context, ownerName, ownerX, ownerY, ClientColors.TEXT_PRIMARY, GFonts.MEDIUM, 5.0F);
            float secondaryRowY = this.stabilizeCoord(secondaryY + 3.0F);
            float itemX = this.stabilizeCoord(secondaryX + 4.0F);
            float itemY = this.stabilizeCoord(secondaryRowY + (secondaryRowH - 8.0F) * 0.5F);
            this.drawItem(context, iconStack, itemX, itemY, 0.5F);
            float ringX = this.stabilizeCoord(itemX + 8.0F + 1.0F);
            float ringY = this.stabilizeCoord(secondaryRowY + (secondaryRowH - 8.0F) * 0.5F);
            float lifetime = Math.max(1.0F, data.maxLifetimeTicks());
            float landingProgress = ColorUtil.clamp01(data.ticksRemaining() / lifetime);
            int timerColor = this.getTimerColorByProgress(landingProgress);
            this.drawLandingTimerCircle(context, ringX, ringY, 8.0F, landingProgress, timerColor);
         }
      }
   }

   private Predictions.TrajectoryResult simulateProjectile(Level world, Entity projectile, int maxTicks, float tickDelta, double drag, double gravity) {
      if (world != null && projectile != null && maxTicks > 0) {
         List<Vec3> points = new ArrayList<>();
         Vec3 position = RenderUtil3D.getInterpolatedPos(projectile, Mth.clamp(tickDelta, 0.0F, 1.0F));
         Vec3 velocity = projectile.getDeltaMovement();
         points.add(position);

         for (int tick = 1; tick <= maxTicks; tick++) {
            Vec3 nextPosition = position.add(velocity);
            BlockPos currentPos = BlockPos.containing(position);
            BlockPos nextPos = BlockPos.containing(nextPosition);
            if (!world.hasChunkAt(currentPos) || !world.hasChunkAt(nextPos)) {
               break;
            }

            BlockHitResult blockHit = world.clip(new ClipContext(position, nextPosition, Block.COLLIDER, Fluid.NONE, projectile));
            double maxDistanceSq = position.distanceToSqr(nextPosition);
            if (blockHit.getType() != Type.MISS) {
               maxDistanceSq = position.distanceToSqr(blockHit.getLocation());
            }

            AABB currentBox = AABB.ofSize(position, projectile.getBbWidth(), projectile.getBbHeight(), projectile.getBbWidth());
            AABB searchBox = currentBox.expandTowards(velocity).inflate(1.0);
            Entity owner = this.getProjectileOwner(projectile);
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
               projectile,
               position,
               nextPosition,
               searchBox,
               entity -> entity != null && entity.isAlive() && !entity.isSpectator() && entity != owner && entity != projectile,
               maxDistanceSq
            );
            if (entityHit != null) {
               Vec3 hitPos = entityHit.getLocation();
               points.add(hitPos);
               return new Predictions.TrajectoryResult(points, hitPos, tick, true);
            }

            if (blockHit.getType() != Type.MISS) {
               Vec3 hitPos = blockHit.getLocation();
               points.add(hitPos);
               return new Predictions.TrajectoryResult(points, hitPos, tick, true);
            }

            position = nextPosition;
            points.add(position);
            boolean water = world.getBlockState(currentPos).liquid();
            double tickDrag = water ? 0.8 : drag;
            velocity = velocity.scale(tickDrag).add(0.0, -gravity, 0.0);
         }

         return new Predictions.TrajectoryResult(points, position, maxTicks, false);
      } else {
         return Predictions.TrajectoryResult.empty();
      }
   }

   private List<Vec3> stabilizeWorldPath(int entityId, List<Vec3> currentPath) {
      List<Vec3> previousPath = this.stablePathCache.get(entityId);
      if (previousPath != null && !previousPath.isEmpty()) {
         int min = Math.min(previousPath.size(), currentPath.size());
         List<Vec3> stabilized = new ArrayList<>(currentPath.size());

         for (int i = 0; i < min; i++) {
            Vec3 prev = previousPath.get(i);
            Vec3 cur = currentPath.get(i);
            stabilized.add(this.smoothVec(prev, cur, 0.3F, 0.0055));
         }

         for (int i = min; i < currentPath.size(); i++) {
            stabilized.add(currentPath.get(i));
         }

         this.stablePathCache.put(entityId, stabilized);
         return stabilized;
      } else {
         List<Vec3> copy = this.copyPath(currentPath);
         this.stablePathCache.put(entityId, copy);
         return copy;
      }
   }

   private Vec3 stabilizeWorldLanding(int entityId, Vec3 currentLanding) {
      Vec3 previous = this.stableLandingCache.get(entityId);
      if (previous == null) {
         this.stableLandingCache.put(entityId, currentLanding);
         return currentLanding;
      } else {
         Vec3 stabilized = this.smoothVec(previous, currentLanding, 0.24F, 0.0045);
         this.stableLandingCache.put(entityId, stabilized);
         return stabilized;
      }
   }

   private Vec3 smoothVec(Vec3 previous, Vec3 current, float smoothing, double deadZone) {
      if (previous == null) {
         return current;
      }

      if (current == null) {
         return previous;
      }

      double dx = current.x - previous.x;
      double dy = current.y - previous.y;
      double dz = current.z - previous.z;
      return dx * dx + dy * dy + dz * dz <= deadZone * deadZone
         ? previous
         : new Vec3(previous.x + dx * smoothing, previous.y + dy * smoothing, previous.z + dz * smoothing);
   }

   private List<Vec3> copyPath(List<Vec3> path) {
      List<Vec3> copy = new ArrayList<>(path.size());
      copy.addAll(path);
      return copy;
   }

   private Vec2 stabilizeLandingPoint(int entityId, Vec2 point, boolean freeze) {
      Vec2 previous = this.landingPointCache.get(entityId);
      if (previous == null) {
         Vec2 initial = new Vec2(point.x, point.y);
         this.landingPointCache.put(entityId, initial);
         return initial;
      }

      if (freeze) {
         return previous;
      }

      float x = this.stabilizeAxis(previous.x, point.x, 0.22F, 0.25F);
      float y = this.stabilizeAxis(previous.y, point.y, 0.22F, 0.25F);
      Vec2 stabilized = new Vec2(x, y);
      this.landingPointCache.put(entityId, stabilized);
      return stabilized;
   }

   private float stabilizeAxis(float previous, float current, float smoothing, float deadZone) {
      float delta = current - previous;
      return Math.abs(delta) <= deadZone ? previous : previous + delta * smoothing;
   }

   private void cacheOwnerIfKnown(Entity projectile) {
      if (projectile != null) {
         int id = projectile.getId();
         if (!this.ownerCache.containsKey(id)) {
            if (this.getProjectileOwner(projectile) instanceof Player ownerPlayer) {
               String direct = ownerPlayer.getDisplayName() != null ? ownerPlayer.getDisplayName().getString() : ownerPlayer.getName().getString();
               if (direct != null && !direct.isBlank()) {
                  this.ownerCache.put(id, new Predictions.OwnerSnapshot(direct, ownerPlayer.getUUID()));
                  return;
               }
            }

            Player nearest = mc.level
               .getNearestPlayer(projectile.getX(), projectile.getY(), projectile.getZ(), 24.0, player -> player != null && !player.isSpectator());
            if (nearest != null && nearest.getName() != null) {
               String nearestName = nearest.getName().getString();
               if (nearestName != null && !nearestName.isBlank()) {
                  this.ownerCache.put(id, new Predictions.OwnerSnapshot(nearestName, nearest.getUUID()));
               }
            }
         }
      }
   }

   private Predictions.OwnerInfo resolveOwner(Entity projectile) {
      if (projectile == null) {
         return new Predictions.OwnerInfo("Unknown", null, null);
      }

      if (this.getProjectileOwner(projectile) instanceof Player ownerPlayer) {
         String ownerName = ownerPlayer.getDisplayName() != null ? ownerPlayer.getDisplayName().getString() : ownerPlayer.getName().getString();
         if (ownerName != null && !ownerName.isBlank()) {
            this.ownerCache.put(projectile.getId(), new Predictions.OwnerSnapshot(ownerName, ownerPlayer.getUUID()));
            return new Predictions.OwnerInfo(ownerName, ownerPlayer, ownerPlayer.getUUID());
         }
      }

      Predictions.OwnerSnapshot cached = this.ownerCache.get(projectile.getId());
      if (cached != null) {
         Player cachedPlayer = this.findPlayerByUuid(cached.uuid());
         return new Predictions.OwnerInfo(cached.name(), cachedPlayer, cached.uuid());
      } else {
         return new Predictions.OwnerInfo("Unknown", null, null);
      }
   }

   private Player findPlayerByUuid(UUID uuid) {
      if (uuid != null && mc.level != null) {
         for (Player player : mc.level.players()) {
            if (player != null && uuid.equals(player.getUUID())) {
               return player;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private Entity getProjectileOwner(Entity entity) {
      return entity instanceof Projectile projectileEntity ? projectileEntity.getOwner() : null;
   }

   private void drawItem(GuiGraphicsExtractor context, ItemStack stack, float x, float y, float scale) {
      Matrix3x2fStack matrices = context.pose();
      matrices.pushMatrix();
      matrices.translate(x, y);
      matrices.scale(scale, scale);
      context.fakeItem(stack, 0, 0);
      matrices.popMatrix();
   }

   private void drawPlayerHead(GuiGraphicsExtractor context, Player player, UUID fallbackUuid, float x, float y, float size) {
      GpuTextureView textureView = this.resolvePlayerSkinTextureView(player, fallbackUuid);
      if (textureView != null) {
         float radius = Math.min(2.0F, size * 0.25F);
         RenderUtil.drawHeadImageRegion(context, textureView, x, y, size, size, 8.0F, 8.0F, 8.0F, 8.0F, radius, -1, 1.0F);
         RenderUtil.drawHeadImageRegion(context, textureView, x, y, size, size, 40.0F, 8.0F, 8.0F, 8.0F, radius, -1, 1.0F);
      }
   }

   private GpuTextureView resolvePlayerSkinTextureView(Player player, UUID fallbackUuid) {
      PlayerSkin textures = this.resolvePlayerSkinTextures(player);
      if (textures == null) {
         textures = DefaultPlayerSkin.get(fallbackUuid != null ? fallbackUuid : new UUID(0L, 0L));
      }

      Identifier skinId = this.resolveSkinIdentifier(textures);
      if (skinId == null) {
         return null;
      }

      try {
         return mc.getTextureManager().getTexture(skinId).getTextureView();
      } catch (Exception ignored) {
         return null;
      }
   }

   private PlayerSkin resolvePlayerSkinTextures(Player player) {
      if (player instanceof AbstractClientPlayer clientPlayer) {
         try {
            if (mc.getConnection() != null) {
               PlayerInfo entry = mc.getConnection().getPlayerInfo(clientPlayer.getUUID());
               if (entry != null) {
                  return entry.getSkin();
               }
            }
         } catch (Exception var5) {
         }

         try {
            return clientPlayer.getSkin();
         } catch (Exception var4) {
            return null;
         }
      } else {
         return null;
      }
   }

   private Identifier resolveSkinIdentifier(PlayerSkin textures) {
      return textures != null && textures.body() != null ? textures.body().texturePath() : null;
   }

   private float stabilizeCoord(float value) {
      return Math.round(value * 4.0F) * 0.25F;
   }

   private void drawLandingTimerCircle(GuiGraphicsExtractor context, float x, float y, float diameter, float progress, int baseColor) {
      float stroke = Math.max(1.05F, diameter * 0.145F) + 0.1F;
      float inner = Math.max(0.0F, diameter - stroke * 2.0F);
      int trackOuter = this.withAlpha(-11908526, 1.0F);
      int fillOuter = this.withAlpha(baseColor, 1.0F);
      RenderUtil.drawCircle(context, x, y, diameter, trackOuter, 1.0F);
      float progressClamped = ColorUtil.clamp01(progress);
      float progressDiameter = Math.max(0.0F, diameter - 0.28F);
      float progressInset = (diameter - progressDiameter) * 0.5F;
      float progressStroke = Math.max(0.2F, stroke - 0.14F);
      RenderUtil.drawRingProgress(context, x + progressInset, y + progressInset, progressDiameter, progressClamped, progressStroke, fillOuter, 1.0F);
      if (progressClamped > 0.0F && progressClamped < 1.0F) {
         float centerX = x + progressInset + progressDiameter * 0.5F;
         float centerY = y + progressInset + progressDiameter * 0.5F;
         float radius = progressDiameter * 0.5F - progressStroke * 0.5F;
         float angle = (float)((-Math.PI / 2) + (Math.PI * 2) * progressClamped);
         float capSize = Math.max(0.72F, progressStroke * 0.92F);
         float capX = centerX + (float)Math.cos(angle) * radius - capSize * 0.5F;
         float capY = centerY + (float)Math.sin(angle) * radius - capSize * 0.5F;
         RenderUtil.drawCircle(context, capX, capY, capSize, fillOuter, 1.0F);
      }

      RenderUtil.drawCircle(context, x + stroke, y + stroke, inner, this.withAlpha(ClientColors.MENU_BACKGROUND, 1.0F), 1.0F);
   }

   private int withAlpha(int color, float alpha) {
      return Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F) << 24 | color & 16777215;
   }

   private int getTimerColorByProgress(float progress) {
      int accentColor = this.color.getColor();
      int yellowColor = ColorUtil.rgba(226, 198, 128, 255);
      int redColor = ColorUtil.rgba(235, 84, 84, 255);
      float p = ColorUtil.clamp01(progress);
      if (p >= 0.5F) {
         return accentColor;
      } else if (p >= 0.3F) {
         float t = (0.5F - p) / 0.2F;
         return ColorUtil.interpolate(accentColor, yellowColor, t);
      } else {
         float t = (0.3F - p) / 0.3F;
         return ColorUtil.interpolate(yellowColor, redColor, t);
      }
   }

   private record CachedTrajectory(long worldTick, Predictions.ProjectileKind kind, List<Vec3> path, Vec3 landingPos, int ticksRemaining) {
   }

   private record OwnerInfo(String name, Player player, UUID uuid) {
   }

   private record OwnerSnapshot(String name, UUID uuid) {
   }

   private record PredictionFrameData(
      int entityId, Vec3 landingPos, int ticksRemaining, int maxLifetimeTicks, Predictions.OwnerInfo owner, Predictions.ProjectileKind kind
   ) {
   }

   private enum ProjectileKind {
      PEARL(0.99, 0.03),
      TRIDENT(0.99, 0.05),
      SNOWBALL(0.99, 0.03),
      POTION(0.99, 0.05);

      private final double drag;
      private final double gravity;

      ProjectileKind(double drag, double gravity) {
         this.drag = drag;
         this.gravity = gravity;
      }
   }

   private record TrajectoryResult(List<Vec3> points, Vec3 landingPos, int ticksRemaining, boolean hit) {
      private static Predictions.TrajectoryResult empty() {
         return new Predictions.TrajectoryResult(List.of(), Vec3.ZERO, 0, false);
      }
   }
}
