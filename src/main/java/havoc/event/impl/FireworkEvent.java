package havoc.event.impl;

import havoc.event.Event;
import lombok.Generated;
import net.minecraft.world.phys.Vec3;

public final class FireworkEvent extends Event {
   private Vec3 vector;

   public FireworkEvent(Vec3 vector) {
      this.vector = vector;
   }

   @Generated
   public Vec3 getVector() {
      return this.vector;
   }

   @Generated
   public void setVector(Vec3 vector) {
      this.vector = vector;
   }
}
