package havoc.utilites;

import havoc.Havoc;
import havoc.module.impl.misc.ScoreboardHealth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;

public final class PlayerHealthUtil {
   private PlayerHealthUtil() {
   }

   public static float getDisplayedHealth(LivingEntity entity, Level world) {
      if (entity == null) {
         return 0.0F;
      }

      if (entity instanceof Player player) {
         Havoc havoc = Havoc.getInstance();
         ScoreboardHealth module = havoc != null && havoc.getModuleManager() != null ? havoc.getModuleManager().getModule(ScoreboardHealth.class) : null;
         if (module != null && module.isEnable()) {
            Float belowName = getBelowNameHealth(player, world);
            if (belowName != null) {
               return belowName;
            }
         }
      }

      return Math.max(0.0F, entity.getHealth());
   }

   public static Float getBelowNameHealth(Player player, Level world) {
      if (player != null && world != null) {
         Objective objective = world.getScoreboard().getDisplayObjective(DisplaySlot.BELOW_NAME);
         if (objective == null) {
            return null;
         }

         ReadOnlyScoreInfo score = world.getScoreboard().getPlayerScoreInfo(player, objective);
         return score == null ? null : Math.max(0.0F, score.value());
      } else {
         return null;
      }
   }
}
