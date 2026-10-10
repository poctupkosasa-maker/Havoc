package havoc.utilites.combat;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ShieldItem;

public class AttackController {
   private final Minecraft minecraft = Minecraft.getInstance();
   private final CooldownTimer cooldownTimer = new CooldownTimer();
   private static final double SPRINT_RESET_MAX_DELTA_Y = 0.0030162615090425808;

   public void onEnable() {
      this.cooldownTimer.reset();
   }

   public void onDisable() {
      if (this.minecraft.player != null) {
         SprintCoordinator.clearSuppression(this.minecraft.player);
      }
   }

   public boolean tick(AttackRequest request) {
      return this.tick(request, null, null);
   }

   public boolean tick(AttackRequest request, Runnable beforeAttack, Runnable afterAttack) {
      LocalPlayer player = this.minecraft.player;
      if (player != null && request.target() != null && request.target().isAlive()) {
         LivingEntity target = request.target();
         if (request.resetSprint() && this.shouldResetSprint(player, target, request.range())) {
            this.resetSprintForTick(player);
         }

         if (CombatPredicates.strictDistanceTo(player, target) >= request.range() || player.distanceTo(target) > request.range()) {
            return false;
         }

         if (!this.cooldownTimer.finished(this.msCooldown(player))) {
            return false;
         }

         if (!CriticalPolicy.canAttack(player, request.criticalMode())) {
            return false;
         }

         if (request.requireAim() && !CombatPredicates.isAimingAtTarget(player, target, request.range(), request.ignoreWalls())) {
            return false;
         }

         if (request.checkEating() && player.isUsingItem() && !(player.getUseItem().getItem() instanceof ShieldItem)) {
            return false;
         }

         boolean shouldRestoreSprint = request.returnSprint() && player.isSprinting() && !player.onGround() && !player.isInWater();
         if (request.resetSprint()) {
            this.resetSprintForTick(player);
         }

         if (beforeAttack != null) {
            beforeAttack.run();
         }

         try {
            this.minecraft.gameMode.attack(player, target);
            player.swing(InteractionHand.MAIN_HAND);
         } finally {
            if (afterAttack != null) {
               afterAttack.run();
            }
         }

         this.cooldownTimer.reset();
         if (shouldRestoreSprint) {
            this.minecraft.options.keySprint.setDown(true);
            player.setSprinting(true);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean willBeReadyWithin(LocalPlayer player, int ticks) {
      return player == null ? false : this.cooldownTimer.finished(this.msCooldown(player) - Math.max(0, ticks) * 50L);
   }

   private boolean shouldResetSprint(LocalPlayer player, LivingEntity target, float range) {
      return player.distanceTo(target) <= range
         && this.cooldownTimer.finished(this.msCooldown(player) - 50L)
         && !player.onGround()
         && !player.isInWater()
         && player.getDeltaMovement().y <= 0.0030162615090425808;
   }

   private void resetSprintForTick(LocalPlayer player) {
      SprintCoordinator.suppressForCurrentTick(player);
      this.minecraft.options.keySprint.setDown(false);
      player.setSprinting(false);
   }

   private long msCooldown(LocalPlayer player) {
      double attackSpeed = player.getAttributeValue(Attributes.ATTACK_SPEED);
      long base = (long)(1.0 / attackSpeed * 1000.0 * 0.8);
      return base + ThreadLocalRandom.current().nextLong(10L, 20L);
   }
}
