package havoc.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import java.util.Arrays;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

@ModuleInfo(name = "TriggerBot", desc = "Luma trigger attack", category = Category.COMBAT)
public final class TriggerBot extends Module {
   private final SliderSetting range = this.registerSetting(new SliderSetting("Range", 3.0F, 1.0F, 6.0F, 0.1F));
   private final BooleanSetting onlyCriticals = this.registerSetting(new BooleanSetting("Only Criticals", true));
   private final BooleanSetting smartCriticals = this.registerSetting(new BooleanSetting("Smart Criticals", true).visibleWhen(this.onlyCriticals::isValue));
   private final BooleanSetting ignoreWalls = this.registerSetting(new BooleanSetting("Hit Through Walls", false));
   private final SelectSetting sprintReset = this.registerSetting(new SelectSetting("Sprint Reset", "Legit", Arrays.asList("Legit", "Packet")));

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.gameMode != null) {
         if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target) {
            if (target.isAlive() && !(mc.player.distanceTo(target) > this.range.getValue())) {
               if (this.ignoreWalls.isValue() || mc.player.hasLineOfSight(target)) {
                  if (!(mc.player.getAttackStrengthScale(0.5F) < 0.92F)) {
                     if (!this.onlyCriticals.isValue() || this.canCritical() || this.smartCriticals.isValue()) {
                        boolean sprinting = mc.player.isSprinting();
                        if (sprinting) {
                           mc.player.setSprinting(false);
                        }

                        mc.gameMode.attack(mc.player, target);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                        if (sprinting) {
                           mc.player.setSprinting(true);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean canCritical() {
      return mc.player.fallDistance > 0.0 && !mc.player.onGround() && !mc.player.isInWater() && !mc.player.isInLava() && !mc.player.onClimbable();
   }
}
