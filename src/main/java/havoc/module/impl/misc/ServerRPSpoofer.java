package havoc.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventPacket;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket.Action;

@ModuleInfo(name = "ServerRPSpoofer", desc = "Spoofs successful server resource-pack loading", category = Category.MISC)
public final class ServerRPSpoofer extends Module {
   private ServerRPSpoofer.State state = ServerRPSpoofer.State.WAIT;
   private long acceptedAt;

   @Subscribe
   public void onPacket(EventPacket event) {
      if (event.isReceive() && event.getPacket() instanceof ClientboundResourcePackPushPacket) {
         event.cancel();
         this.state = ServerRPSpoofer.State.ACCEPT;
      }
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.getConnection() != null) {
         if (this.state == ServerRPSpoofer.State.ACCEPT) {
            mc.getConnection().send(new ServerboundResourcePackPacket(mc.player.getUUID(), Action.ACCEPTED));
            this.acceptedAt = System.currentTimeMillis();
            this.state = ServerRPSpoofer.State.SUCCESS;
         } else if (this.state == ServerRPSpoofer.State.SUCCESS && System.currentTimeMillis() - this.acceptedAt >= 300L) {
            mc.getConnection().send(new ServerboundResourcePackPacket(mc.player.getUUID(), Action.SUCCESSFULLY_LOADED));
            this.state = ServerRPSpoofer.State.WAIT;
         }
      }
   }

   @Override
   public void onDisable() {
      this.state = ServerRPSpoofer.State.WAIT;
      super.onDisable();
   }

   private enum State {
      ACCEPT,
      SUCCESS,
      WAIT;
   }
}
