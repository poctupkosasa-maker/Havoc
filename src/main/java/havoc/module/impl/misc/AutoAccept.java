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

@ModuleInfo(name = "AutoAccept", desc = "Accepts teleport requests", category = Category.MISC)
public final class AutoAccept extends Module {
   private final BooleanSetting onlyFriends = this.registerSetting(new BooleanSetting("Только друзья", true));

   @Subscribe
   public void onPacket(EventPacket event) {
      if (event.isReceive() && mc.getConnection() != null && event.getPacket() instanceof ClientboundSystemChatPacket packet) {
         String text = packet.content().getString().toLowerCase(Locale.ROOT);
         boolean request = text.contains("/tpaccept")
            || text.contains("has requested teleport")
            || text.contains("requested to teleport")
            || text.contains("просит к вам телепортироваться")
            || text.contains("запрашивает телепорт");
         if (request && (!this.onlyFriends.isValue() || this.containsFriend(text))) {
            mc.getConnection().sendCommand("tpaccept");
         }
      }
   }

   private boolean containsFriend(String message) {
      Havoc havoc = Havoc.getInstance();
      return havoc != null && havoc.getCommandManager() != null
         ? havoc.getCommandManager().getFriends().stream().map(name -> name.toLowerCase(Locale.ROOT)).anyMatch(message::contains)
         : false;
   }
}
