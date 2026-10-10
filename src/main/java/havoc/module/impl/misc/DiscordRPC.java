package havoc.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import havoc.discord.DiscordClient;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;

@ModuleInfo(name = "DiscordRPC", desc = "Показывает активность чита в дискорде.", category = Category.MISC)
public final class DiscordRPC extends Module {
   private final DiscordClient client = new DiscordClient();

   @Override
   public void onEnable() {
      this.client.start();
   }

   @Override
   public void onDisable() {
      this.client.stop();
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (this.isEnable()) {
         this.client.start();
      }
   }
}
