package havoc.event.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import havoc.event.Event;
import lombok.Generated;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;

public final class EventRender3D extends Event {
   private final PoseStack matrices;
   private final float tickDelta;
   private final Vec3 cameraPos;
   private final Frustum frustum;
   private final MultiBufferSource.BufferSource consumers;

   public EventRender3D(PoseStack matrices, float tickDelta, Vec3 cameraPos, Frustum frustum, MultiBufferSource.BufferSource consumers) {
      this.matrices = matrices;
      this.tickDelta = tickDelta;
      this.cameraPos = cameraPos;
      this.frustum = frustum;
      this.consumers = consumers;
   }

   @Generated
   public PoseStack getMatrices() {
      return this.matrices;
   }

   @Generated
   public float getTickDelta() {
      return this.tickDelta;
   }

   @Generated
   public Vec3 getCameraPos() {
      return this.cameraPos;
   }

   @Generated
   public Frustum getFrustum() {
      return this.frustum;
   }

   @Generated
   public MultiBufferSource.BufferSource getConsumers() {
      return this.consumers;
   }
}
