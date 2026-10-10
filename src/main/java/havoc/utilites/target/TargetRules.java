package havoc.utilites.target;

import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public final class TargetRules {
   private static final Set<EntityType<?>> WATER_ANIMAL_TYPES = Set.of(
      EntityTypes.AXOLOTL,
      EntityTypes.COD,
      EntityTypes.DOLPHIN,
      EntityTypes.GLOW_SQUID,
      EntityTypes.PUFFERFISH,
      EntityTypes.SALMON,
      EntityTypes.SQUID,
      EntityTypes.TADPOLE,
      EntityTypes.TROPICAL_FISH
   );

   private TargetRules() {
   }

   public static boolean isValidEntity(Player viewer, Entity entity, TargetRuleSet rules) {
      if (!(entity instanceof ItemEntity item)) {
         return entity instanceof LivingEntity living ? isValidLiving(viewer, living, rules) : false;
      } else {
         return rules.targetItems() && !item.isRemoved();
      }
   }

   public static boolean isValidLiving(Player viewer, LivingEntity candidate, TargetRuleSet rules) {
      if (candidate == viewer || !candidate.isAlive() || candidate.isRemoved()) {
         return false;
      }

      if (!rules.targetInvisibles() && candidate.isInvisible()) {
         return false;
      }

      if (candidate instanceof Player player) {
         if (player.isSpectator()) {
            return false;
         } else {
            return !rules.targetPlayers() ? false : rules.targetNaked() || !isNaked(player);
         }
      } else if (candidate instanceof Animal) {
         return rules.targetAnimals();
      } else if (candidate instanceof Enemy) {
         return rules.targetMobs();
      } else if (candidate.getType() == EntityTypes.VILLAGER) {
         return rules.targetVillagers();
      } else {
         return WATER_ANIMAL_TYPES.contains(candidate.getType()) ? rules.targetAnimals() : false;
      }
   }

   private static boolean isNaked(Player player) {
      return player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
         && player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
         && player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
         && player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
   }
}
