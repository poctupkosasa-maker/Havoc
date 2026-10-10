package havoc.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;

@ModuleInfo(name = "NoFall", desc = "Reduces fall damage", category = Category.MOVEMENT)
public final class NoFall extends Module {
   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.getConnection() != null) {
         if (!mc.player.onGround() && mc.player.fallDistance > 2.5) {
            mc.getConnection()
               .send(
                  new PosRot(
                     mc.player.getX(),
                     mc.player.getY() + 1.0E-9,
                     mc.player.getZ(),
                     mc.player.getYRot(),
                     mc.player.getXRot(),
                     true,
                     mc.player.horizontalCollision
                  )
               );
            mc.player.resetFallDistance();
         }
      }
   }
}
