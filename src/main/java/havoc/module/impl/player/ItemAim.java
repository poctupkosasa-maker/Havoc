package havoc.module.impl.player;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SliderSetting;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(name = "ItemAim", desc = "Aims throwable items at players", category = Category.PLAYER)
public final class ItemAim extends Module {
   private final SliderSetting range = this.registerSetting(new SliderSetting("Range", 25.0F, 4.0F, 60.0F, 1.0F));

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.level != null && this.isThrowable()) {
         LivingEntity target = mc.level
            .players()
            .stream()
            .filter(player -> player != mc.player && player.isAlive() && mc.player.distanceTo(player) <= this.range.getValue())
            .min((a, b) -> Float.compare(mc.player.distanceTo(a), mc.player.distanceTo(b)))
            .orElse(null);
         if (target instanceof Player) {
            this.aimAt(target.getEyePosition());
         }
      }
   }

   private boolean isThrowable() {
      return mc.player.getMainHandItem().is(Items.ENDER_PEARL)
         || mc.player.getMainHandItem().is(Items.SNOWBALL)
         || mc.player.getMainHandItem().is(Items.EGG)
         || mc.player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE);
   }

   private void aimAt(Vec3 pos) {
      Vec3 diff = pos.subtract(mc.player.getEyePosition());
      mc.player.setYRot((float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0));
      mc.player.setXRot((float)(-Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)))));
   }
}
