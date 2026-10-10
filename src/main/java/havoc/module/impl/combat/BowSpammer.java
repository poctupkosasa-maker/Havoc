package havoc.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SliderSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BowItem;

@ModuleInfo(name = "BowSpammer", desc = "Rapidly releases a bow", category = Category.COMBAT)
public final class BowSpammer extends Module {
   private final SliderSetting delay = this.registerSetting(new SliderSetting("Delay", 2.5F, 2.2F, 5.0F, 0.1F));

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.getConnection() != null) {
         if (mc.player.getMainHandItem().getItem() instanceof BowItem) {
            if (mc.player.isUsingItem() && !(mc.player.getTicksUsingItem() < this.delay.getValue())) {
               mc.getConnection().send(new ServerboundPlayerActionPacket(Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
               mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
               mc.player.releaseUsingItem();
            }
         }
      }
   }
}
