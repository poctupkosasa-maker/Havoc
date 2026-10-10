package havoc.screen.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public final class MenuCloseAnimation {
   private static final long DURATION_NANOS = 500000000L;
   private static MenuScreen source;
   private static long startedAt;
   private static float startYaw;
   private static float startPitch;

   private MenuCloseAnimation() {
   }

   public static void start(MenuScreen screen) {
      Minecraft mc = Minecraft.getInstance();
      if (screen != null && mc != null && mc.player != null) {
         source = screen;
         startedAt = System.nanoTime();
         startYaw = cameraYaw(mc);
         startPitch = cameraPitch(mc);
      }
   }

   public static void render(GuiGraphicsExtractor context) {
      MenuScreen screen = source;
      if (screen != null) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.player != null && mc.gui.screen() == null) {
            float progress = Math.clamp((float)(System.nanoTime() - startedAt) / 5.0E8F, 0.0F, 1.0F);
            if (progress >= 1.0F) {
               clear();
            } else {
               float eased = progress * progress * (3.0F - 2.0F * progress);
               float alpha = 1.0F - eased;
               float yawDelta = Mth.wrapDegrees(cameraYaw(mc) - startYaw);
               float pitchDelta = cameraPitch(mc) - startPitch;
               screen.renderSpatialAfterimage(context, alpha, yawDelta, pitchDelta);
            }
         } else {
            clear();
         }
      }
   }

   private static float cameraYaw(Minecraft mc) {
      return mc.gameRenderer != null && mc.gameRenderer.mainCamera().isInitialized() ? mc.gameRenderer.mainCamera().yRot() : mc.player.getYRot();
   }

   private static float cameraPitch(Minecraft mc) {
      return mc.gameRenderer != null && mc.gameRenderer.mainCamera().isInitialized() ? mc.gameRenderer.mainCamera().xRot() : mc.player.getXRot();
   }

   public static void clear() {
      source = null;
      startedAt = 0L;
   }
}
