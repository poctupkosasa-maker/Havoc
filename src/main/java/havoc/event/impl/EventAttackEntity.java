package havoc.event.impl;

import havoc.event.Event;
import net.minecraft.world.entity.Entity;

public final class EventAttackEntity extends Event {
   private final Entity target;

   public EventAttackEntity(Entity target) {
      this.target = target;
   }

   public Entity getTarget() {
      return this.target;
   }
}
