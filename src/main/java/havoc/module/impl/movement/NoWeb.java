package havoc.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SelectSetting;
import havoc.utilites.MovementUtil;
import java.util.List;
import net.minecraft.world.level.block.Blocks;

@ModuleInfo(name = "NoWeb", desc = "Luma Grim cobweb movement", category = Category.MOVEMENT)
public final class NoWeb extends Module {
   private final SelectSetting mode = this.registerSetting(new SelectSetting("Mode", "Grim", List.of("Grim")));

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.level != null) {
         if (mc.level.getBlockState(mc.player.blockPosition()).is(Blocks.COBWEB)) {
            MovementUtil.setHorizontalSpeed(0.35);
            double y = mc.options.keyJump.isDown() ? 0.65 : (mc.options.keyShift.isDown() ? -0.65 : 0.0);
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, y, mc.player.getDeltaMovement().z);
         }
      }
   }
}
