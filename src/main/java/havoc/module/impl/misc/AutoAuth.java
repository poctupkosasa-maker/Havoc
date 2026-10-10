package havoc.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventPacket;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.InputSetting;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

@ModuleInfo(name = "AutoAuth", desc = "Auto register/login helper", category = Category.MISC)
public final class AutoAuth extends Module {
   private final InputSetting password = this.registerSetting(new InputSetting("Password", "123123qq", 32));

   @Subscribe
   public void onPacket(EventPacket event) {
      if (event.isReceive() && mc.getConnection() != null && event.getPacket() instanceof ClientboundSystemChatPacket packet) {
         String var5 = packet.content().getString().toLowerCase();
         String pass = this.password.getValue();
         if (var5.contains("/register") || var5.contains("register") || var5.contains("зарегистр")) {
            mc.getConnection().sendCommand("register " + pass + " " + pass);
         } else if (var5.contains("/login") || var5.contains("login") || var5.contains("авториз")) {
            mc.getConnection().sendCommand("login " + pass);
         }
      }
   }
}
