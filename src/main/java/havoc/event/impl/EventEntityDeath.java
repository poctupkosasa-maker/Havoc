package havoc.event.impl;

import havoc.event.Event;
import net.minecraft.world.entity.LivingEntity;

public final class EventEntityDeath extends Event {
   private final LivingEntity entity;

   public EventEntityDeath(LivingEntity entity) {
      this.entity = entity;
   }

   public LivingEntity getEntity() {
      return this.entity;
   }
}
