package havoc.event.impl;

import havoc.event.Event;
import net.minecraft.network.protocol.Packet;

public final class EventPacket extends Event {
   private final Packet<?> packet;
   private final boolean receive;

   public EventPacket(Packet<?> packet, boolean receive) {
      this.packet = packet;
      this.receive = receive;
   }

   public Packet<?> getPacket() {
      return this.packet;
   }

   public boolean isReceive() {
      return this.receive;
   }

   public boolean isSend() {
      return !this.receive;
   }
}
