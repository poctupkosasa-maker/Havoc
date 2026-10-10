package havoc.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventAttackEntity;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SelectSetting;
import java.util.List;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.entity.LivingEntity;

@ModuleInfo(name = "Criticals", desc = "Packet critical hits", category = Category.COMBAT)
public final class Criticals extends Module {
   private final SelectSetting mode = this.registerSetting(new SelectSetting("Режим", "ReallyWorld", List.of("ReallyWorld")));

   @Subscribe
   public void onAttack(EventAttackEntity event) {
      if (mc.player != null && mc.getConnection() != null && event.getTarget() instanceof LivingEntity) {
         if (mc.player.onGround() && !mc.player.isInWater() && !mc.player.isInLava() && !mc.player.onClimbable()) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            mc.getConnection().send(new Pos(x, y + 0.0625, z, false, mc.player.horizontalCollision));
            mc.getConnection().send(new Pos(x, y, z, false, mc.player.horizontalCollision));
         }
      }
   }
}
