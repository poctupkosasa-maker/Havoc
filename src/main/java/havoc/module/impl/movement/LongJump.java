package havoc.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SelectSetting;
import java.util.Arrays;
import net.minecraft.world.level.block.SlimeBlock;

@ModuleInfo(name = "LongJump", desc = "Luma long jump modes", category = Category.MOVEMENT)
public final class LongJump extends Module {
   private final SelectSetting mode = this.registerSetting(
      new SelectSetting("Режим", "Boat", Arrays.asList("Boat", "Shulker Screen", "Slime Boost", "FunTime Soul Sand"))
   );
   private boolean slime;

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.level != null) {
         if (this.mode.getValue().equals("FunTime Soul Sand") && mc.player.isInWater() && !mc.player.isUnderWater()) {
            mc.player.push(0.0, 0.56, 0.0);
         }

         boolean onSlime = mc.level.getBlockState(mc.player.blockPosition().below()).getBlock() instanceof SlimeBlock;
         if (this.mode.getValue().equals("Slime Boost") && this.slime && !mc.player.onGround() && mc.player.getDeltaMovement().y > 0.0) {
            mc.player.push(0.0, 1.35, 0.0);
         }

         this.slime = onSlime && mc.player.onGround();
      }
   }
}
