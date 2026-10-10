package havoc.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import havoc.Havoc;
import havoc.event.impl.EventAttackEntity;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import net.minecraft.world.entity.player.Player;

@ModuleInfo(name = "NoFriendDamage", desc = "Prevents attacking friends", category = Category.COMBAT)
public final class NoFriendDamage extends Module {
   @Subscribe
   public void onAttack(EventAttackEntity event) {
      if (event.getTarget() instanceof Player player && Havoc.getInstance().getCommandManager().isFriend(player.getName().getString())) {
         event.cancel();
      }
   }
}
