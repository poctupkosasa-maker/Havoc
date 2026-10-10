package havoc.event.impl;

import havoc.event.Event;
import lombok.Generated;

public final class EventRotation extends Event {
   private final EventRotation.Type type;
   private float yaw;
   private float pitch;

   public EventRotation(EventRotation.Type type, float yaw, float pitch) {
      this.type = type;
      this.yaw = yaw;
      this.pitch = pitch;
   }

   @Generated
   public EventRotation.Type getType() {
      return this.type;
   }

   @Generated
   public float getYaw() {
      return this.yaw;
   }

   @Generated
   public float getPitch() {
      return this.pitch;
   }

   @Generated
   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   @Generated
   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public enum Type {
      PACKET,
      VISUAL;
   }
}
