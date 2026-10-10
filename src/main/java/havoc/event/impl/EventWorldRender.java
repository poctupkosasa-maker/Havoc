package havoc.event.impl;

import havoc.event.Event;
import lombok.Generated;
import net.minecraft.client.Camera;

public final class EventWorldRender extends Event {
   private final Camera camera;
   private final float tickDelta;

   public EventWorldRender(Camera camera, float tickDelta) {
      this.camera = camera;
      this.tickDelta = tickDelta;
   }

   @Generated
   public Camera getCamera() {
      return this.camera;
   }

   @Generated
   public float getTickDelta() {
      return this.tickDelta;
   }
}
