package havoc.utilites.target;

public record TargetSelectorConfig(
   boolean focusTarget,
   boolean targetPlayers,
   boolean targetMobs,
   boolean targetAnimals,
   boolean targetInvisibles,
   boolean targetNaked,
   boolean targetVillagers,
   CombatPriority priority
) {
}
