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

@ModuleInfo(name = "AutoExplosion", desc = "Breaks nearby crystals while using one", category = Category.COMBAT)
public final class AutoExplosion extends Module {
   private final SliderSetting range = this.registerSetting(new SliderSetting("Дистанция", 5.0F, 1.0F, 6.0F, 0.1F));
   private int cooldown;

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.level != null && mc.gameMode != null) {
         if (this.cooldown > 0) {
            this.cooldown--;
         } else {
            boolean holdsCrystal = mc.player.getMainHandItem().is(Items.END_CRYSTAL) || mc.player.getOffhandItem().is(Items.END_CRYSTAL);
            if (holdsCrystal && mc.options.keyUse.isDown()) {
               for (EndCrystal crystal : mc.level
                  .getEntitiesOfClass(EndCrystal.class, mc.player.getBoundingBox().inflate(this.range.getValue().floatValue()), Entity::isAlive)) {
                  if (!(mc.player.distanceToSqr(crystal) > this.range.getValue() * this.range.getValue())) {
                     mc.gameMode.attack(mc.player, crystal);
                     mc.player.swing(InteractionHand.MAIN_HAND);
                     this.cooldown = 1;
                     return;
                  }
               }
            }
         }
      }
   }
}
