package havoc.utilites.target;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import lombok.Generated;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TargetSelector {
   private TargetSelectorConfig config;
   private Integer focusedTargetId = null;

   public TargetSelector(TargetSelectorConfig config) {
      this.config = config;
   }

   public LivingEntity select(Player player, double searchRange) {
      return this.select(player, searchRange, entity -> true);
   }

   public LivingEntity select(Player player, double searchRange, Predicate<LivingEntity> extraFilter) {
      Level world = player.level();
      if (this.config == null) {
         return null;
      }

      Predicate<LivingEntity> filter = extraFilter != null ? extraFilter : entity -> true;
      if (this.config.focusTarget()
         && this.focusedTargetId != null
         && world.getEntity(this.focusedTargetId) instanceof LivingEntity living
         && this.shouldKeepFocused(player, living, searchRange)
         && filter.test(living)) {
         return living;
      }

      AABB searchBox = player.getBoundingBox().inflate(searchRange);
      List<LivingEntity> candidates = world.getEntitiesOfClass(LivingEntity.class, searchBox, e -> this.isValid(player, e, searchRange) && filter.test(e));
      LivingEntity selected = candidates.stream().min(this.comparatorFor(player)).orElse(null);
      this.focusedTargetId = this.config.focusTarget() && selected != null ? selected.getId() : null;
      return selected;
   }

   public void clearFocus() {
      this.focusedTargetId = null;
   }

   private boolean shouldKeepFocused(Player player, LivingEntity candidate, double searchRange) {
      if (candidate != player && candidate.isAlive() && !candidate.isRemoved()) {
         return candidate instanceof Player p && p.isSpectator() ? false : player.distanceToSqr(candidate) <= searchRange * searchRange;
      } else {
         return false;
      }
   }

   private boolean isValid(Player player, LivingEntity candidate, double searchRange) {
      if (candidate != player && candidate.isAlive() && !candidate.isRemoved()) {
         return player.distanceToSqr(candidate) > searchRange * searchRange ? false : TargetRules.isValidLiving(player, candidate, this.ruleSet());
      } else {
         return false;
      }
   }

   private Comparator<LivingEntity> comparatorFor(Player player) {
      return switch (this.config.priority()) {
         case Distance -> Comparator.comparingDouble(player::distanceToSqr);
         case Health -> Comparator.comparingDouble(LivingEntity::getHealth).thenComparingDouble(player::distanceToSqr);
         case Armor -> Comparator.comparingInt(LivingEntity::getArmorValue).thenComparingDouble(player::distanceToSqr);
         case FieldOfView -> Comparator.<LivingEntity, Float>comparing(e -> this.fovScore(player, e)).thenComparingDouble(player::distanceToSqr);
         case AllAtOnce -> Comparator.<LivingEntity, Float>comparing(e -> this.fovScore(player, e))
            .thenComparingDouble(player::distanceToSqr)
            .thenComparingDouble(LivingEntity::getHealth)
            .thenComparingInt(LivingEntity::getArmorValue);
      };
   }

   public float[] computeAngles(Player player, LivingEntity target) {
      Vec3 playerEye = player.getEyePosition();
      Vec3 targetEye = target.getEyePosition();
      double dx = targetEye.x - playerEye.x;
      double dy = targetEye.y - playerEye.y;
      double dz = targetEye.z - playerEye.z;
      double flatDistance = Math.sqrt(dx * dx + dz * dz);
      float yaw = Mth.wrapDegrees((float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
      float pitch = Mth.clamp((float)(-Math.toDegrees(Math.atan2(dy, flatDistance))), -90.0F, 90.0F);
      return new float[]{yaw, pitch};
   }

   private float fovScore(Player player, LivingEntity target) {
      float[] angles = this.computeAngles(player, target);
      return Math.abs(Mth.wrapDegrees(angles[0] - player.getYRot())) + Math.abs(angles[1] - player.getXRot());
   }

   private TargetRuleSet ruleSet() {
      return new TargetRuleSet(
         this.config.targetPlayers(),
         this.config.targetMobs(),
         this.config.targetAnimals(),
         this.config.targetInvisibles(),
         this.config.targetNaked(),
         this.config.targetVillagers()
      );
   }

   @Generated
   public void setConfig(TargetSelectorConfig config) {
      this.config = config;
   }

   @Generated
   public Integer getFocusedTargetId() {
      return this.focusedTargetId;
   }
}
