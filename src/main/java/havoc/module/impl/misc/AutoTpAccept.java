package havoc.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import havoc.Havoc;
import havoc.event.impl.EventPacket;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import java.util.Locale;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

@ModuleInfo(name = "AutoTpAccept", desc = "Accepts teleport requests", category = Category.MISC)
public final class AutoTpAccept extends Module {
   private final BooleanSetting friendsOnly = this.registerSetting(new BooleanSetting("Только друзья", true));

   @Subscribe
   public void onPacket(EventPacket event) {
      if (event.isReceive() && mc.getConnection() != null && event.getPacket() instanceof ClientboundSystemChatPacket packet) {
         String message = packet.content().getString();
         String lower = message.toLowerCase(Locale.ROOT);
         boolean request = lower.contains("has requested teleport")
            || lower.contains("просит телепортироваться")
            || lower.contains("хочет телепортироваться к вам");
         if (request) {
            if (this.friendsOnly.isValue()) {
               String sender = message.strip().split("\\s+", 2)[0].replaceAll("[^A-Za-z0-9_]", "");
               if (sender.isEmpty() || !Havoc.getInstance().getCommandManager().isFriend(sender)) {
                  return;
               }
            }

            mc.getConnection().sendCommand("tpaccept");
         }
      }
   }
}
