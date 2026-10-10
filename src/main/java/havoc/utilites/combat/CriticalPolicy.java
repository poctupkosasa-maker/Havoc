package havoc.utilites.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

public final class CriticalPolicy {
   public static boolean canAttack(LocalPlayer player, CriticalMode mode) {
      return switch (mode) {
         case None -> true;
         case Adaptive -> isBestMomentToHit(player, true);
         case Always -> isBestMomentToHit(player, false);
      };
   }

   private static boolean isBestMomentToHit(LocalPlayer player, boolean smartCrits) {
      if (player.getMainHandItem().is(Items.MACE)) {
         return true;
      } else {
         return CombatPredicates.isPlayerInCriticalState(player) ? true : smartCrits && !Minecraft.getInstance().options.keyJump.isDown();
      }
   }
}
