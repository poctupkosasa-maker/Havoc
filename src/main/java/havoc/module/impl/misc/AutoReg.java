package havoc.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventPacket;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.InputSetting;
import java.util.Locale;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

@ModuleInfo(name = "AutoReg", desc = "Automatic registration and login", category = Category.MISC)
public final class AutoReg extends Module {
   private final InputSetting password = this.registerSetting(new InputSetting("Пароль", "Luma123123", 64));
   private final BooleanSetting autoRegister = this.registerSetting(new BooleanSetting("Авто /reg", true));
   private final BooleanSetting autoLogin = this.registerSetting(new BooleanSetting("Авто /login", true));
   private long lastSend;

   @Subscribe
   public void onPacket(EventPacket event) {
      if (event.isReceive() && mc.getConnection() != null && event.getPacket() instanceof ClientboundSystemChatPacket packet) {
         if (System.currentTimeMillis() - this.lastSend >= 1500L && !this.password.getValue().isBlank()) {
            String text = packet.content().getString().toLowerCase(Locale.ROOT);
            if (!this.autoRegister.isValue() || !containsCommand(text, "/register") && !containsCommand(text, "/reg")) {
               if (this.autoLogin.isValue() && containsCommand(text, "/login")) {
                  mc.getConnection().sendCommand("login " + this.password.getValue());
                  this.lastSend = System.currentTimeMillis();
               }
            } else {
               mc.getConnection().sendCommand("reg " + this.password.getValue() + " " + this.password.getValue());
               this.lastSend = System.currentTimeMillis();
            }
         }
      }
   }

   private static boolean containsCommand(String text, String command) {
      int index = text.indexOf(command);
      if (index < 0) {
         return false;
      }

      int end = index + command.length();
      return end >= text.length() || !Character.isLetterOrDigit(text.charAt(end));
   }
}
