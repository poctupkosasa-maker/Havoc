package havoc.event.impl;

import havoc.event.Event;
import net.minecraft.world.entity.Entity;

public final class EventTotemPop extends Event {
   private final Entity entity;

   public EventTotemPop(Entity entity) {
      this.entity = entity;
   }

   public Entity getEntity() {
      return this.entity;
   }
}
