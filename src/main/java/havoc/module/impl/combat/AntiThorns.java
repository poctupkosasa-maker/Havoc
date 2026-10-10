package havoc.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventAttackEntity;
import havoc.event.impl.EventPacket;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.LivingEntity;

@ModuleInfo(name = "AntiThorns", desc = "Suppresses self knockback right after hits", category = Category.COMBAT)
public final class AntiThorns extends Module {
   private int suppressTicks;

   @Subscribe
   public void onAttack(EventAttackEntity event) {
      if (event.getTarget() instanceof LivingEntity) {
         this.suppressTicks = 4;
      }
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (this.suppressTicks > 0) {
         this.suppressTicks--;
      }
   }

   @Subscribe
   public void onPacket(EventPacket event) {
      if (this.suppressTicks > 0
         && event.isReceive()
         && mc.player != null
         && event.getPacket() instanceof ClientboundSetEntityMotionPacket packet
         && packet.id() == mc.player.getId()) {
         event.cancel();
      }
   }
}
