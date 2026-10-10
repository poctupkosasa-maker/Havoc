package havoc.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.utilites.MovementUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

@ModuleInfo(name = "Web Ignore", desc = "Lets you move through cobwebs faster", category = Category.MOVEMENT)
public final class WebIgnore extends Module {
   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.level != null) {
         BlockPos pos = mc.player.blockPosition();
         if (mc.level.getBlockState(pos).is(Blocks.COBWEB) || mc.level.getBlockState(pos.above()).is(Blocks.COBWEB)) {
            MovementUtil.setHorizontalSpeed(0.24);
         }
      }
   }
}
