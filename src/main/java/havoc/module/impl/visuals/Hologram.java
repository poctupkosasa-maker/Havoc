package havoc.module.impl.visuals;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sun.jna.Pointer;
import havoc.Havoc;
import havoc.event.impl.EventRender3D;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.InputSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import havoc.render.Render3D;
import havoc.utilites.capture.HoloCapture;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

@ModuleInfo(name = "Hologram", desc = "Голограммы с трансляцией окон Windows.", category = Category.VISUALS)
public final class Hologram extends Module {
   private static final int MAX_SCREENS = 3;
   private static final int FULL_BRIGHT = 0xF000F0;
   private static final int FRAME_COLOR = 0xB44DE1FF;

   public final SelectSetting mode = this.registerSetting(
      new SelectSetting("Режим", "Следует", List.of("Следует", "Статичная"))
   );
   public final SliderSetting count = this.registerSetting(
      new SliderSetting("Экраны", 1.0F, 1.0F, 3.0F, 1.0F)
   );
   public final InputSetting window1 = this.registerSetting(new InputSetting("Окно 1", "Discord", 64));
   public final InputSetting window2 = this.registerSetting(
      new InputSetting("Окно 2", "", 64).visibleWhen(() -> this.count.getValue() >= 1.5F)
   );
   public final InputSetting window3 = this.registerSetting(
      new InputSetting("Окно 3", "", 64).visibleWhen(() -> this.count.getValue() >= 2.5F)
   );
   public final SliderSetting scale1 = this.registerSetting(new SliderSetting("Размер 1", 1.6F, 0.6F, 4.0F, 0.1F));
   public final SliderSetting scale2 = this.registerSetting(
      new SliderSetting("Размер 2", 1.6F, 0.6F, 4.0F, 0.1F).visibleWhen(() -> this.count.getValue() >= 1.5F)
   );
   public final SliderSetting scale3 = this.registerSetting(
      new SliderSetting("Размер 3", 1.6F, 0.6F, 4.0F, 0.1F).visibleWhen(() -> this.count.getValue() >= 2.5F)
   );
   public final SliderSetting distance = this.registerSetting(
      new SliderSetting("Дистанция", 2.5F, 1.2F, 6.0F, 0.1F).visibleWhen(() -> this.mode.getValue().equals("Следует"))
   );
   public final SliderSetting yOffset = this.registerSetting(new SliderSetting("Высота", 0.2F, -2.0F, 2.0F, 0.1F));
   public final SliderSetting fps = this.registerSetting(new SliderSetting("FPS захвата", 10.0F, 1.0F, 20.0F, 1.0F));
   public final BooleanSetting clicks = this.registerSetting(new BooleanSetting("Клики по экранам", true));
   public final BooleanSetting setPos = this.registerSetting(
      new BooleanSetting("Поставить сюда", false).visibleWhen(() -> this.mode.getValue().equals("Статичная"))
   );

   private final HoloScreen[] screens = {new HoloScreen(0), new HoloScreen(1), new HoloScreen(2)};
   private ScheduledExecutorService captureThread;
   private long lastCaptureMs;
   private volatile Vec3 staticAnchor;
   private volatile Vec3 lastFollow = Vec3.ZERO;

   public static boolean consumeClick(int button, int action) {
      if (button != 0 || action != 1) {
         return false;
      }
      Havoc havoc = Havoc.getInstance();
      if (havoc == null) {
         return false;
      }
      Hologram holo = havoc.getModuleManager().getModule(Hologram.class);
      if (holo == null || !holo.isEnable() || !holo.clicks.isValue()) {
         return false;
      }
      return holo.clickNearest();
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.setPos.setValue(false);
      this.stopCaptureThread();
      this.captureThread = Executors.newSingleThreadScheduledExecutor(task -> {
         Thread thread = new Thread(task, "havoc-hologram");
         thread.setDaemon(true);
         return thread;
      });
      this.lastCaptureMs = 0L;
      this.captureThread.scheduleWithFixedDelay(this::captureLoop, 0L, 50L, TimeUnit.MILLISECONDS);
   }

   @Override
   public void onDisable() {
      this.stopCaptureThread();
      for (HoloScreen screen : this.screens) {
         screen.reset();
      }
      this.staticAnchor = null;
      super.onDisable();
   }

   @Subscribe
   public void onTick(EventUpdate event) {
      if (mc.level == null || mc.player == null) {
         return;
      }
      Vec3 follow = this.computeFollowCenter();
      if (follow != null) {
         this.lastFollow = follow;
      }
      if (this.setPos.isValue()) {
         if (follow != null) {
            this.staticAnchor = follow;
         }
         this.setPos.setValue(false);
      }
   }

   @Subscribe
   public void onRender3D(EventRender3D event) {
      if (mc.level == null || mc.player == null) {
         return;
      }
      Camera camera = mc.gameRenderer.mainCamera();
      if (camera == null) {
         return;
      }
      Vec3 camPos = event.getCameraPos();
      Vec3 look = toVec(camera.forwardVector()).normalize();
      if (look.lengthSqr() < 1.0E-6) {
         return;
      }
      int active = this.activeCount();
      VertexConsumer lineConsumer = null;
      for (int i = 0; i < active; i++) {
         if (this.renderScreen(event, this.screens[i], i, active, camPos, look)) {
            if (lineConsumer == null) {
               lineConsumer = event.getConsumers().getBuffer(RenderTypes.lines());
            }
            this.drawFrame(event, lineConsumer, this.screens[i], camPos);
         }
      }
      if (lineConsumer != null) {
         try {
            event.getConsumers().draw(RenderTypes.lines());
         } catch (Throwable ignored) {
         }
      }
   }

   private boolean renderScreen(EventRender3D event, HoloScreen screen, int index, int active, Vec3 camPos, Vec3 look) {
      try {
         if (!screen.ensureTexture()) {
            return false;
         }
         screen.pumpTexture();
         Vec3 center = this.screenCenter(index, active, camPos, look);
         if (center == null) {
            return false;
         }
         float width = this.screenScale(index);
         float height = width * HoloCapture.TARGET_H / (float) HoloCapture.TARGET_W;
         Vec3 up0 = new Vec3(0.0, 1.0, 0.0);
         Vec3 right = look.cross(up0);
         if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
         }
         right = right.normalize();
         Vec3 up = right.cross(look).normalize();
         screen.setTransform(center, right, up, width, height);
         if (!screen.hasFrame) {
            return true;
         }
         float hw = width * 0.5F;
         float hh = height * 0.5F;
         Vec3 topLeft = center.add(right.scale(-hw)).add(up.scale(hh)).subtract(camPos);
         Vec3 bottomLeft = center.add(right.scale(-hw)).add(up.scale(-hh)).subtract(camPos);
         Vec3 bottomRight = center.add(right.scale(hw)).add(up.scale(-hh)).subtract(camPos);
         Vec3 topRight = center.add(right.scale(hw)).add(up.scale(hh)).subtract(camPos);
         RenderType type = RenderTypes.entityTranslucent(screen.id);
         VertexConsumer consumer = event.getConsumers().getBuffer(type);
         PoseStack.Pose pose = event.getMatrices().last();
         quad(consumer, pose, topLeft, bottomLeft, bottomRight, topRight);
         quad(consumer, pose, topLeft, topRight, bottomRight, bottomLeft);
         try {
            event.getConsumers().draw(type);
         } catch (Throwable ignored) {
         }
         return true;
      } catch (Throwable ignored) {
         return false;
      }
   }

   private static void quad(
      VertexConsumer consumer, PoseStack.Pose pose,
      Vec3 a, Vec3 b, Vec3 c, Vec3 d,
      float ua, float va, float ub, float vb, float uc, float vc, float ud, float vd
   ) {
      consumer.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(255, 255, 255, 255).setUv(ua, va).setLight(FULL_BRIGHT);
      consumer.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(255, 255, 255, 255).setUv(ub, vb).setLight(FULL_BRIGHT);
      consumer.addVertex(pose, (float) c.x, (float) c.y, (float) c.z).setColor(255, 255, 255, 255).setUv(uc, vc).setLight(FULL_BRIGHT);
      consumer.addVertex(pose, (float) d.x, (float) d.y, (float) d.z).setColor(255, 255, 255, 255).setUv(ud, vd).setLight(FULL_BRIGHT);
   }

   private static void quad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
      quad(consumer, pose, a, b, c, d, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F);
   }

   private void drawFrame(EventRender3D event, VertexConsumer lines, HoloScreen screen, Vec3 camPos) {
      try {
         float hw = screen.w * 0.5F;
         float hh = screen.h * 0.5F;
         Vec3 topLeft = screen.center.add(screen.right.scale(-hw)).add(screen.up.scale(hh)).subtract(camPos);
         Vec3 bottomLeft = screen.center.add(screen.right.scale(-hw)).add(screen.up.scale(-hh)).subtract(camPos);
         Vec3 bottomRight = screen.center.add(screen.right.scale(hw)).add(screen.up.scale(-hh)).subtract(camPos);
         Vec3 topRight = screen.center.add(screen.right.scale(hw)).add(screen.up.scale(hh)).subtract(camPos);
         PoseStack matrices = event.getMatrices();
         Render3D.drawLine(matrices, lines, topLeft, topRight, FRAME_COLOR);
         Render3D.drawLine(matrices, lines, topRight, bottomRight, FRAME_COLOR);
         Render3D.drawLine(matrices, lines, bottomRight, bottomLeft, FRAME_COLOR);
         Render3D.drawLine(matrices, lines, bottomLeft, topLeft, FRAME_COLOR);
      } catch (Throwable ignored) {
      }
   }

   private boolean clickNearest() {
      try {
         if (mc.level == null || mc.player == null) {
            return false;
         }
         Camera camera = mc.gameRenderer.mainCamera();
         if (camera == null) {
            return false;
         }
         Vec3 origin = camera.position();
         Vec3 dir = toVec(camera.forwardVector()).normalize();
         if (dir.lengthSqr() < 1.0E-6) {
            return false;
         }
         int active = this.activeCount();
         double bestT = Double.MAX_VALUE;
         HoloScreen best = null;
         double bestU = 0.0;
         double bestV = 0.0;
         for (int i = 0; i < active; i++) {
            HoloScreen screen = this.screens[i];
            if (!screen.hasFrame || screen.center == null) {
               continue;
            }
            Vec3 normal = screen.up.cross(screen.right).normalize();
            double denom = dir.dot(normal);
            if (Math.abs(denom) < 1.0E-6) {
               continue;
            }
            double t = screen.center.subtract(origin).dot(normal) / denom;
            if (t < 0.0 || t > 64.0 || t >= bestT) {
               continue;
            }
            Vec3 hit = origin.add(dir.scale(t));
            Vec3 rel = hit.subtract(screen.center);
            double u = rel.dot(screen.right) / screen.w + 0.5;
            double v = 0.5 - rel.dot(screen.up) / screen.h;
            if (u >= 0.0 && u <= 1.0 && v >= 0.0 && v <= 1.0) {
               bestT = t;
               best = screen;
               bestU = u;
               bestV = v;
            }
         }
         if (best == null) {
            return false;
         }
         HoloCapture.Target target = best.target;
         if (target == null || target.hwnd() == null || target.clientW() <= 0 || target.clientH() <= 0) {
            return false;
         }
         HoloCapture.postClick(target.hwnd(), (int) (bestU * target.clientW()), (int) (bestV * target.clientH()));
         return true;
      } catch (Throwable ignored) {
         return false;
      }
   }

   private Vec3 screenCenter(int index, int active, Vec3 camPos, Vec3 look) {
      if (this.mode.getValue().equals("Статичная")) {
         Vec3 anchor = this.staticAnchor != null ? this.staticAnchor : this.lastFollow;
         if (anchor == null) {
            return null;
         }
         Vec3 up0 = new Vec3(0.0, 1.0, 0.0);
         Vec3 right = look.cross(up0);
         if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
         }
         right = right.normalize();
         float spacing = this.screenScale(index) + 0.4F;
         return anchor.add(right.scale((index - (active - 1) * 0.5F) * spacing));
      }
      return this.computeFollowCenter(camPos, look);
   }

   private Vec3 computeFollowCenter() {
      try {
         Camera camera = mc.gameRenderer.mainCamera();
         if (camera == null) {
            return null;
         }
         return this.computeFollowCenter(camera.position(), toVec(camera.forwardVector()).normalize());
      } catch (Throwable ignored) {
         return null;
      }
   }

   private Vec3 computeFollowCenter(Vec3 camPos, Vec3 look) {
      if (camPos == null || look == null || look.lengthSqr() < 1.0E-6) {
         return null;
      }
      return camPos.add(look.scale(this.distance.getValue())).add(new Vec3(0.0, this.yOffset.getValue(), 0.0));
   }

   private int activeCount() {
      try {
         return Math.max(1, Math.min(MAX_SCREENS, Math.round(this.count.getValue())));
      } catch (Throwable ignored) {
         return 1;
      }
   }

   private float screenScale(int index) {
      try {
         float value = switch (index) {
            case 1 -> this.scale2.getValue();
            case 2 -> this.scale3.getValue();
            default -> this.scale1.getValue();
         };
         return Math.max(0.3F, Math.min(6.0F, value));
      } catch (Throwable ignored) {
         return 1.6F;
      }
   }

   private String windowHint(int index) {
      try {
         String hint = switch (index) {
            case 1 -> this.window2.getValue();
            case 2 -> this.window3.getValue();
            default -> this.window1.getValue();
         };
         return hint == null ? "" : hint.trim();
      } catch (Throwable ignored) {
         return "";
      }
   }

   private void captureLoop() {
      try {
         if (!this.isEnable() || mc.level == null || mc.player == null) {
            return;
         }
         long period = 1000L / Math.max(1L, Math.round(this.fps.getValue()));
         long now = System.currentTimeMillis();
         if (now - this.lastCaptureMs < period) {
            return;
         }
         this.lastCaptureMs = now;
         int active = this.activeCount();
         for (int i = 0; i < active; i++) {
            try {
               this.screens[i].capture(this.windowHint(i));
            } catch (Throwable ignored) {
            }
         }
      } catch (Throwable ignored) {
      }
   }

   private void stopCaptureThread() {
      ScheduledExecutorService thread = this.captureThread;
      this.captureThread = null;
      if (thread != null) {
         try {
            thread.shutdownNow();
         } catch (Throwable ignored) {
         }
      }
      for (HoloScreen screen : this.screens) {
         try {
            screen.closeSession();
         } catch (Throwable ignored) {
         }
      }
   }

   private static Vec3 toVec(Vector3fc v) {
      return new Vec3(v.x(), v.y(), v.z());
   }

   private final class HoloScreen {
      private final int index;
      private final Identifier id;
      private final int[] staging = new int[HoloCapture.TARGET_W * HoloCapture.TARGET_H];
      private final Object lock = new Object();
      private final HoloCapture.Session session = new HoloCapture.Session();

      private DynamicTexture texture;
      private volatile boolean dirty;
      private volatile boolean hasFrame;
      private volatile HoloCapture.Target target;
      private volatile Pointer hwnd;
      private volatile long lastResolveMs;

      private Vec3 center;
      private Vec3 right = new Vec3(1.0, 0.0, 0.0);
      private Vec3 up = new Vec3(0.0, 1.0, 0.0);
      private float w = 1.6F;
      private float h = 0.9F;

      private HoloScreen(int index) {
         this.index = index;
         this.id = Identifier.fromNamespaceAndPath("havoc", "hologram/" + index);
      }

      private boolean ensureTexture() {
         if (this.texture != null) {
            return true;
         }
         try {
            NativeImage image = new NativeImage(NativeImage.Format.RGBA, HoloCapture.TARGET_W, HoloCapture.TARGET_H, false);
            DynamicTexture dynamic = new DynamicTexture(() -> "havoc hologram " + this.index, image);
            mc.getTextureManager().register(this.id, dynamic);
            this.texture = dynamic;
            return true;
         } catch (Throwable ignored) {
            return false;
         }
      }

      private void pumpTexture() {
         if (!this.dirty || this.texture == null) {
            return;
         }
         try {
            NativeImage image;
            synchronized (this.lock) {
               if (!this.dirty) {
                  return;
               }
               this.dirty = false;
               image = this.texture.getPixels();
               for (int y = 0; y < HoloCapture.TARGET_H; y++) {
                  int row = y * HoloCapture.TARGET_W;
                  for (int x = 0; x < HoloCapture.TARGET_W; x++) {
                     image.setPixelABGR(x, y, this.staging[row + x]);
                  }
               }
            }
            this.texture.upload();
            this.hasFrame = true;
         } catch (Throwable ignored) {
         }
      }

      private void capture(String hint) {
         if (hint == null || hint.isEmpty()) {
            this.target = null;
            this.closeSession();
            return;
         }
         long now = System.currentTimeMillis();
         if (this.hwnd == null || now - this.lastResolveMs > 5000L) {
            this.lastResolveMs = now;
            HoloCapture.WinInfo found = HoloCapture.findWindow(hint);
            if (found == null || !this.session.open(found.hwnd())) {
               this.target = null;
               this.hwnd = null;
               return;
            }
            this.hwnd = found.hwnd();
            this.target = new HoloCapture.Target(found.hwnd(), found.title(), this.session.clientW, this.session.clientH);
         }
         synchronized (this.lock) {
            if (this.session.grab(this.staging)) {
               this.dirty = true;
            }
         }
      }

      private void setTransform(Vec3 center, Vec3 right, Vec3 up, float w, float h) {
         this.center = center;
         this.right = right;
         this.up = up;
         this.w = w;
         this.h = h;
      }

      private void closeSession() {
         try {
            this.session.close();
         } catch (Throwable ignored) {
         }
         this.hwnd = null;
      }

      private void reset() {
         this.closeSession();
         this.target = null;
         this.dirty = false;
         this.hasFrame = false;
         this.center = null;
         if (this.texture != null) {
            try {
               this.texture.close();
            } catch (Throwable ignored) {
            }
            this.texture = null;
         }
      }
   }
}
