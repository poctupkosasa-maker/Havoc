package havoc.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SelectSetting;
import java.util.List;
import net.minecraft.client.gui.screens.DeathScreen;

@ModuleInfo(name = "AutoRespawn", desc = "Respawns automatically", category = Category.MISC)
public final class AutoRespawn extends Module {
   private final SelectSetting mode = this.registerSetting(new SelectSetting("Режим", "Default", List.of("Default")));

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.gui.screen() instanceof DeathScreen && mc.player.deathTime > 5) {
         mc.player.respawn();
         mc.gui.setScreen(null);
      }
   }
}
