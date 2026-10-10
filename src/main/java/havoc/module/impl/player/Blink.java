package havoc.module.impl.player;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventPacket;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

@ModuleInfo(name = "Blink", desc = "Buffers movement packets", category = Category.PLAYER)
public final class Blink extends Module {
   private final List<Packet<?>> packets = new ArrayList<>();
   private boolean flushing;

   @Override
   public void onDisable() {
      this.flush();
      super.onDisable();
   }

   @Subscribe
   public void onPacket(EventPacket event) {
      if (!this.flushing && event.isSend() && mc.getConnection() != null) {
         if (event.getPacket() instanceof ServerboundMovePlayerPacket) {
            this.packets.add(event.getPacket());
            event.cancel();
         }
      }
   }

   private void flush() {
      if (mc.getConnection() != null && !this.packets.isEmpty()) {
         this.flushing = true;

         for (Packet<?> packet : this.packets) {
            mc.getConnection().send(packet);
         }

         this.packets.clear();
         this.flushing = false;
      } else {
         this.packets.clear();
      }
   }
}
