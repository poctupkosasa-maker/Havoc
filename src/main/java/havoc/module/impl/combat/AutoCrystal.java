package havoc.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SliderSetting;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

@ModuleInfo(name = "AutoCrystal", desc = "Attacks nearby crystals", category = Category.COMBAT)
public final class AutoCrystal extends Module {
   private final SliderSetting range = this.registerSetting(new SliderSetting("Range", 4.5F, 2.0F, 6.0F, 0.1F));
   private int delay;

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (this.delay > 0) {
         this.delay--;
      } else if (mc.player != null && mc.level != null && mc.gameMode != null) {
         EndCrystal crystal = mc.level
            .getEntitiesOfClass(
               EndCrystal.class,
               mc.player.getBoundingBox().inflate(this.range.getValue().floatValue()),
               entity -> entity.isAlive() && mc.player.distanceTo(entity) <= this.range.getValue()
            )
            .stream()
            .min((a, b) -> Float.compare(mc.player.distanceTo(a), mc.player.distanceTo(b)))
            .orElse(null);
         if (crystal != null) {
            mc.gameMode.attack(mc.player, crystal);
            mc.player.swing(mc.player.getUsedItemHand());
            this.delay = 2;
         }
      }
   }
}
