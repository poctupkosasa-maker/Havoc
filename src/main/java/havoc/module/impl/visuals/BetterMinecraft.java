package havoc.module.impl.visuals;

import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.List;
import lombok.Generated;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "BetterMinecraft", desc = "Better Minecraft", category = Category.VISUALS)
public final class BetterMinecraft extends Module {
   private static final float CHAT_ANIMATION_TICKS = 7.0F;
   private static final long CONTAINER_ANIMATION_DURATION_NANOS = 220000000L;
   private static final float CHUNK_ANIMATION_MAX_OFFSET = 16.0F;
   private static final String ZOOM_KEY_C = "C";
   private static final String ZOOM_KEY_V = "V";
   private static final String ZOOM_KEY_X = "X";
   private static final String ZOOM_KEY_Z = "Z";
   private static final String ZOOM_KEY_LEFT_ALT = "Left Alt";
   private static final String ZOOM_KEY_LEFT_CTRL = "Left Ctrl";
   private static final String ZOOM_KEY_MOUSE_4 = "Mouse 4";
   private static final String ZOOM_KEY_MOUSE_5 = "Mouse 5";
   private static BetterMinecraft instance;
   private static Screen animatedContainerScreen;
   private static long containerAnimationStartNanos;
   private static final Long2LongOpenHashMap chunkAnimationStartBySection = new Long2LongOpenHashMap();
   private final BooleanSetting tabVanishButton = this.registerSetting(new BooleanSetting("Spectators In Tab", "Shows spectator controls in tab", true));
   private final BooleanSetting zoom = this.registerSetting(new BooleanSetting("Zoom", "Smooth camera zoom", true));
   private final SelectSetting zoomKey = this.registerSetting(
      new SelectSetting("Zoom Key", "C", List.of("C", "V", "X", "Z", "Left Alt", "Left Ctrl", "Mouse 4", "Mouse 5")).visibleWhen(() -> this.zoom.isValue())
   );
   private final SliderSetting zoomFov = this.registerSetting(
      new SliderSetting("Zoom Fov", 30.0F, 5.0F, 70.0F, 1.0F, "").visibleWhen(() -> this.zoom.isValue())
   );
   private final SliderSetting zoomSpeed = this.registerSetting(
      new SliderSetting("Zoom Speed", 22.0F, 5.0F, 40.0F, 1.0F, "").visibleWhen(() -> this.zoom.isValue())
   );
   private final BooleanSetting chatAnimation = this.registerSetting(new BooleanSetting("Chat Animation", "Animated chat reveal", true));
   private final BooleanSetting containerAnimation = this.registerSetting(
      new BooleanSetting("Container Animation", "Smooth inventory and container open animation", true)
   );
   private final BooleanSetting chunkAnimator = this.registerSetting(new BooleanSetting("Chunk Animator", "Animates appearing chunks", true));
   private final SliderSetting chunkAnimatorSpeed = this.registerSetting(
      new SliderSetting("Chunk Speed", 10.0F, 1.0F, 20.0F, 1.0F, "").visibleWhen(() -> this.chunkAnimator.isValue())
   );
   private float zoomProgress;
   private long lastZoomUpdateNanos;

   public BetterMinecraft() {
      instance = this;
      chunkAnimationStartBySection.defaultReturnValue(Long.MIN_VALUE);
   }

   public static BetterMinecraft getInstance() {
      return instance;
   }

   @Override
   public void onDisable() {
      clearChunkAnimations();
      super.onDisable();
   }

   public static float adjustFov(float baseFov) {
      BetterMinecraft module = instance;
      return module == null ? baseFov : module.getAnimatedFov(baseFov);
   }

   public static boolean shouldAnimateChat() {
      BetterMinecraft module = instance;
      return module != null && module.isEnable() && module.chatAnimation.isValue();
   }

   public static float getChatAnimationProgress(int currentTick, int addedTick) {
      if (!shouldAnimateChat()) {
         return 1.0F;
      }

      float age = Math.max(0.0F, currentTick - addedTick);
      float raw = Math.min(1.0F, age / 7.0F);
      return easeOutCubic(raw);
   }

   public static void markContainerOpened(Screen screen) {
      if (screen != null) {
         if (screen != animatedContainerScreen) {
            animatedContainerScreen = screen;
            containerAnimationStartNanos = System.nanoTime();
         }
      }
   }

   public static float getContainerAnimationProgress(Screen screen) {
      BetterMinecraft module = instance;
      if (module == null || !module.isEnable() || !module.containerAnimation.isValue() || screen == null) {
         return 1.0F;
      } else if (screen != animatedContainerScreen) {
         animatedContainerScreen = screen;
         containerAnimationStartNanos = System.nanoTime();
         return 0.0F;
      } else if (containerAnimationStartNanos == 0L) {
         containerAnimationStartNanos = System.nanoTime();
         return 0.0F;
      } else {
         long elapsed = System.nanoTime() - containerAnimationStartNanos;
         float raw = Math.max(0.0F, Math.min(1.0F, (float)elapsed / 2.2E8F));
         return easeOutCubic(raw);
      }
   }

   public static float getChunkOffsetY(BlockPos origin) {
      BetterMinecraft module = instance;
      if (module != null && module.isEnable() && module.chunkAnimator.isValue() && origin != null) {
         long key = SectionPos.asLong(origin.getX() >> 4, origin.getY() >> 4, origin.getZ() >> 4);
         long now = System.nanoTime();
         long startedAt = chunkAnimationStartBySection.get(key);
         if (startedAt == Long.MIN_VALUE) {
            chunkAnimationStartBySection.put(key, now);
            startedAt = now;
         }

         float raw = Math.max(0.0F, Math.min(1.0F, (float)(now - startedAt) / (float)module.getChunkAnimationDurationNanos()));
         return raw >= 1.0F ? 0.0F : -(1.0F - easeOutCubic(raw)) * 16.0F;
      } else {
         return 0.0F;
      }
   }

   public static void clearChunkAnimations() {
      chunkAnimationStartBySection.clear();
   }

   public static float easeOutCubic(float progress) {
      float clamped = Math.max(0.0F, Math.min(1.0F, progress));
      float inv = 1.0F - clamped;
      return 1.0F - inv * inv * inv;
   }

   private float getAnimatedFov(float baseFov) {
      long now = System.nanoTime();
      if (this.lastZoomUpdateNanos == 0L) {
         this.lastZoomUpdateNanos = now;
      }

      float deltaSeconds = Math.max(0.0F, Math.min(0.05F, (float)(now - this.lastZoomUpdateNanos) / 1.0E9F));
      this.lastZoomUpdateNanos = now;
      boolean bindZoom = this.isZoomKeyHeld();
      boolean shouldZoom = this.isEnable() && this.zoom.isValue() && bindZoom && mc != null && mc.gui.screen() == null;
      float target = shouldZoom ? 1.0F : 0.0F;
      float smoothing = 1.0F - (float)Math.exp(-this.zoomSpeed.getValue() * deltaSeconds);
      this.zoomProgress = this.zoomProgress + (target - this.zoomProgress) * smoothing;
      if (Math.abs(target - this.zoomProgress) < 5.0E-4F) {
         this.zoomProgress = target;
      }

      return lerp(baseFov, this.zoomFov.getValue(), this.zoomProgress);
   }

   private static float lerp(float from, float to, float delta) {
      return from + (to - from) * Math.max(0.0F, Math.min(1.0F, delta));
   }

   private long getChunkAnimationDurationNanos() {
      float normalized = (this.chunkAnimatorSpeed.getValue() - this.chunkAnimatorSpeed.getMin())
         / (this.chunkAnimatorSpeed.getMax() - this.chunkAnimatorSpeed.getMin());
      float durationMs = 900.0F - normalized * 780.0F;
      return (long)(durationMs * 1000000.0F);
   }

   private boolean isZoomKeyHeld() {
      if (mc != null && mc.getWindow() != null) {
         long handle = mc.getWindow().handle();

         return switch (this.zoomKey.getValue()) {
            case "V" -> GLFW.glfwGetKey(handle, 86) == 1;
            case "X" -> GLFW.glfwGetKey(handle, 88) == 1;
            case "Z" -> GLFW.glfwGetKey(handle, 90) == 1;
            case "Left Alt" -> GLFW.glfwGetKey(handle, 342) == 1;
            case "Left Ctrl" -> GLFW.glfwGetKey(handle, 341) == 1;
            case "Mouse 4" -> GLFW.glfwGetMouseButton(handle, 3) == 1;
            case "Mouse 5" -> GLFW.glfwGetMouseButton(handle, 4) == 1;
            default -> GLFW.glfwGetKey(handle, 67) == 1;
         };
      } else {
         return false;
      }
   }

   @Generated
   public BooleanSetting getTabVanishButton() {
      return this.tabVanishButton;
   }

   @Generated
   public BooleanSetting getZoom() {
      return this.zoom;
   }

   @Generated
   public SelectSetting getZoomKey() {
      return this.zoomKey;
   }

   @Generated
   public SliderSetting getZoomFov() {
      return this.zoomFov;
   }

   @Generated
   public SliderSetting getZoomSpeed() {
      return this.zoomSpeed;
   }

   @Generated
   public BooleanSetting getChatAnimation() {
      return this.chatAnimation;
   }

   @Generated
   public BooleanSetting getContainerAnimation() {
      return this.containerAnimation;
   }

   @Generated
   public BooleanSetting getChunkAnimator() {
      return this.chunkAnimator;
   }

   @Generated
   public SliderSetting getChunkAnimatorSpeed() {
      return this.chunkAnimatorSpeed;
   }

   @Generated
   public float getZoomProgress() {
      return this.zoomProgress;
   }

   @Generated
   public long getLastZoomUpdateNanos() {
      return this.lastZoomUpdateNanos;
   }
}
