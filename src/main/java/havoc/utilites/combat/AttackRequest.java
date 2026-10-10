package havoc.utilites.combat;

import net.minecraft.world.entity.LivingEntity;

public record AttackRequest(
   LivingEntity target,
   CriticalMode criticalMode,
   boolean resetSprint,
   boolean returnSprint,
   boolean useServerPattern,
   float range,
   boolean ignoreWalls,
   boolean checkEating,
   boolean requireAim
) {
}
