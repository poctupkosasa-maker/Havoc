package havoc.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SliderSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;

@ModuleInfo(name = "CrystallOptimizer", desc = "Instantly attacks nearby crystals", category = Category.COMBAT)
public final class CrystallOptimizer extends Module {
   private final SliderSetting range = this.registerSetting(new SliderSetting("Range", 5.0F, 1.0F, 6.0F, 0.1F));

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.level != null && mc.gameMode != null) {
         boolean crystalHeld = mc.player.getMainHandItem().is(Items.END_CRYSTAL) || mc.player.getOffhandItem().is(Items.END_CRYSTAL);
         if (crystalHeld && mc.options.keyUse.isDown()) {
            for (Entity entity : mc.level.entitiesForRendering()) {
               if (entity instanceof EndCrystal crystal && mc.player.distanceTo(crystal) <= this.range.getValue()) {
                  mc.gameMode.attack(mc.player, crystal);
                  mc.player.swing(InteractionHand.MAIN_HAND);
               }
            }
         }
      }
   }
}
