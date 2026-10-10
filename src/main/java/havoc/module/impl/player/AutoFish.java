package havoc.module.impl.player;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.SliderSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;

@ModuleInfo(name = "AutoFish", desc = "Luma automatic fishing", category = Category.PLAYER)
public final class AutoFish extends Module {
   private final BooleanSetting autoWalk = this.registerSetting(new BooleanSetting("AutoWalk", true));
   private final SliderSetting searchRadius = this.registerSetting(
      new SliderSetting("Search Radius", 16.0F, 4.0F, 64.0F, 1.0F).visibleWhen(this.autoWalk::isValue)
   );
   private final SliderSetting castRange = this.registerSetting(new SliderSetting("Cast Range", 7.0F, 3.0F, 12.0F, 1.0F));
   private final SliderSetting reelDelay = this.registerSetting(new SliderSetting("Reel Delay", 3.0F, 0.0F, 20.0F, 1.0F));
   private final SliderSetting recastDelay = this.registerSetting(new SliderSetting("Recast Delay", 12.0F, 1.0F, 40.0F, 1.0F));
   private int reelTicks = -1;
   private int recastTicks;

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.gameMode != null && mc.gui.screen() == null) {
         if (mc.player.getMainHandItem().is(Items.FISHING_ROD)) {
            FishingHook bobber = mc.player.fishing;
            if (bobber == null) {
               if (this.recastTicks-- <= 0) {
                  mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                  this.recastTicks = this.recastDelay.getValue().intValue();
               }
            } else {
               if (bobber.getDeltaMovement().y < -0.08 && this.reelTicks < 0) {
                  this.reelTicks = this.reelDelay.getValue().intValue();
               }

               if (this.reelTicks >= 0 && --this.reelTicks <= 0) {
                  mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                  mc.player.swing(InteractionHand.MAIN_HAND);
                  this.reelTicks = -1;
                  this.recastTicks = this.recastDelay.getValue().intValue();
               }
            }
         }
      }
   }
}
