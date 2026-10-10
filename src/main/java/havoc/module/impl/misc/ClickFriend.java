package havoc.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import havoc.Havoc;
import havoc.event.impl.EventKeyPress;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.utilites.ChatUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

@ModuleInfo(name = "ClickFriend", desc = "Toggles friend under crosshair by middle click", category = Category.MISC)
public final class ClickFriend extends Module {
   @Subscribe
   public void onKey(EventKeyPress event) {
      if (event.getAction() != 0 && event.getKey() == 1002) {
         if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Player player) {
            String name = player.getName().getString();
            boolean added = Havoc.getInstance().getCommandManager().toggleFriend(name);
            ChatUtil.sendMessage((added ? "Added friend: " : "Removed friend: ") + name);
         }
      }
   }
}
