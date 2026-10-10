package havoc.utilites.combat;

import net.minecraft.client.player.LocalPlayer;

public final class SprintCoordinator {
   private static int suppressedTick = -1;

   public static void suppressForCurrentTick(LocalPlayer player) {
      suppressedTick = player.tickCount;
   }

   public static void clearSuppression(LocalPlayer player) {
      if (suppressedTick == player.tickCount) {
         suppressedTick = -1;
      }
   }
}
