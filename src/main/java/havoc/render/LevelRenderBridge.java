package havoc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import havoc.Havoc;
import havoc.event.impl.EventRender3D;
import havoc.event.impl.EventWorldRender;
import havoc.module.impl.visuals.Ambience;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.AfterOpaqueTerrain;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.CollectSubmits;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

public final class LevelRenderBridge {
   private LevelRenderBridge() {
   }

   public static void register() {
      LevelRenderEvents.COLLECT_SUBMITS.register((CollectSubmits)context -> {
         Havoc havoc = Havoc.getInstance();
         Minecraft client = Minecraft.getInstance();
         if (havoc != null && client != null && client.gameRenderer != null) {
            Camera camera = client.gameRenderer.mainCamera();
            if (camera != null) {
               float tickDelta = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
               PoseStack matrices = context.poseStack();
               Vec3 cameraPos = camera.position();
               MultiBufferSource.BufferSource consumers = new MultiBufferSource.BufferSource(context.submitNodeCollector());
               havoc.getEventBus().post(new EventRender3D(matrices, tickDelta, cameraPos, null, consumers));
               consumers.endBatch();
            }
         }
      });
      LevelRenderEvents.AFTER_OPAQUE_TERRAIN.register((AfterOpaqueTerrain)context -> {
         Havoc havoc = Havoc.getInstance();
         Minecraft client = Minecraft.getInstance();
         if (havoc != null && client != null && client.gameRenderer != null) {
            Camera camera = client.gameRenderer.mainCamera();
            if (camera != null) {
               float tickDelta = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
               havoc.getEventBus().post(new EventWorldRender(camera, tickDelta));
            }

            Ambience ambience = havoc.getModuleManager().getModule(Ambience.class);
            if (ambience != null && ambience.hasCustomSky()) {
               if (camera != null) {
                  AmbienceSkyRenderer.draw(camera, ambience);
               }
            }
         }
      });
   }
}
