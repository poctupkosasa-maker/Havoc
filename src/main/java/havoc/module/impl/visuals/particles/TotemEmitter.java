package havoc.module.impl.visuals.particles;

import net.minecraft.world.entity.Entity;

public final class TotemEmitter {
   private final Entity entity;
   private final int maxAge;
   private int age;

   public TotemEmitter(Entity entity, int maxAge) {
      this.entity = entity;
      this.maxAge = maxAge;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public void tick() {
      this.age++;
   }

   public boolean isAlive() {
      return this.age < this.maxAge && this.entity != null && !this.entity.isRemoved();
   }

   public float getProgress() {
      return this.maxAge <= 0 ? 1.0F : (float)this.age / this.maxAge;
   }
}
