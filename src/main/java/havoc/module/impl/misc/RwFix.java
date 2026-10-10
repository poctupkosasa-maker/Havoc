package havoc.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;

@ModuleInfo(name = "RwFix", desc = "ReallyWorld health fix", category = Category.MISC)
public final class RwFix extends Module {
   private static final Map<String, Float> HEALTH = new ConcurrentHashMap<>();

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.level == null) {
         HEALTH.clear();
      }
   }

   @Override
   public void onDisable() {
      HEALTH.clear();
      super.onDisable();
   }

   public static Float getHealth(Player player) {
      return player == null ? null : HEALTH.get(player.getName().getString().toLowerCase(Locale.ROOT));
   }
}
