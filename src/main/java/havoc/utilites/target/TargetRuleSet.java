package havoc.utilites.target;

public record TargetRuleSet(
   boolean targetPlayers,
   boolean targetMobs,
   boolean targetAnimals,
   boolean targetInvisibles,
   boolean targetNaked,
   boolean targetVillagers,
   boolean targetItems
) {
   public TargetRuleSet(
      boolean targetPlayers, boolean targetMobs, boolean targetAnimals, boolean targetInvisibles, boolean targetNaked, boolean targetVillagers
   ) {
      this(targetPlayers, targetMobs, targetAnimals, targetInvisibles, targetNaked, targetVillagers, false);
   }
}
