package havoc.module.impl.player;

import com.google.common.eventbus.Subscribe;
import com.mojang.authlib.GameProfile;
import havoc.event.impl.EventAttackEntity;
import havoc.event.impl.EventKeyPress;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import java.util.UUID;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(name = "FakePlayer", desc = "", category = Category.PLAYER)
public final class FakePlayer extends Module {
   private static final int FAKE_PLAYER_ENTITY_ID = -23467;
   private static final UUID FAKE_PLAYER_UUID = UUID.fromString("66123666-6666-6666-6666-666666666600");
   private static final String FAKE_PLAYER_NAME = "FakePlayer";
   private RemotePlayer fakePlayer;
   private float moveForward;
   private float moveStrafe;

   public void ensureSpawned() {
      this.spawnFakePlayerIfNeeded();
   }

   public void despawn() {
      this.removeFakePlayer();
   }

   public RemotePlayer getFakePlayerEntity() {
      return this.fakePlayer;
   }

   public boolean hasFakePlayer() {
      return mc != null
         && mc.level != null
         && this.fakePlayer != null
         && !this.fakePlayer.isRemoved()
         && mc.level.getEntity(this.fakePlayer.getId()) == this.fakePlayer;
   }

   public void moveFakeTo(Vec3 position, float yaw, float pitch) {
      if (position != null && this.fakePlayer != null) {
         this.fakePlayer.snapTo(position.x, position.y, position.z, yaw, pitch);
         this.fakePlayer.setYHeadRot(yaw);
         this.fakePlayer.yBodyRot = yaw;
      }
   }

   public int getFakePlayerEntityId() {
      return -23467;
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.spawnFakePlayerIfNeeded();
   }

   @Override
   public void onDisable() {
      this.removeFakePlayer();
      super.onDisable();
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc != null && mc.level != null && mc.player != null) {
         boolean existsInCurrentWorld = this.fakePlayer != null && mc.level.getEntity(this.fakePlayer.getId()) == this.fakePlayer;
         if (this.fakePlayer == null || this.fakePlayer.isRemoved() || !existsInCurrentWorld) {
            this.spawnFakePlayerIfNeeded();
         }

         this.updateFakeMovement();
      } else {
         this.removeFakePlayer();
      }
   }

   @Subscribe
   public void onAttack(EventAttackEntity event) {
      if (event != null && mc != null && mc.level != null && mc.player != null && this.fakePlayer != null) {
         if (event.getTarget() != null) {
            if (event.getTarget() == this.fakePlayer || event.getTarget().getId() == this.fakePlayer.getId()) {
               if (this.fakePlayer.hurtTime == 0) {
                  mc.level
                     .playSound(
                        mc.player,
                        this.fakePlayer.getX(),
                        this.fakePlayer.getY(),
                        this.fakePlayer.getZ(),
                        SoundEvents.PLAYER_HURT,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                     );
                  if (mc.player.fallDistance > 0.0) {
                     mc.level
                        .playSound(
                           mc.player,
                           this.fakePlayer.getX(),
                           this.fakePlayer.getY(),
                           this.fakePlayer.getZ(),
                           SoundEvents.PLAYER_ATTACK_CRIT,
                           SoundSource.PLAYERS,
                           1.0F,
                           1.0F
                        );
                  } else {
                     mc.level
                        .playSound(
                           mc.player,
                           this.fakePlayer.getX(),
                           this.fakePlayer.getY(),
                           this.fakePlayer.getZ(),
                           SoundEvents.PLAYER_ATTACK_SWEEP,
                           SoundSource.PLAYERS,
                           1.0F,
                           1.0F
                        );
                  }

                  this.fakePlayer.handleDamageEvent(mc.level.damageSources().generic());
                  this.fakePlayer.setHealth(this.fakePlayer.getHealth() + this.fakePlayer.getAbsorptionAmount() - 1.0F);
                  if (this.fakePlayer.isDeadOrDying()) {
                     this.fakePlayer.setHealth(10.0F);
                     if (mc.player.connection != null) {
                        new ClientboundEntityEventPacket(this.fakePlayer, (byte)35).handle(mc.player.connection);
                     }
                  }
               }
            }
         }
      }
   }

   @Subscribe
   public void onKeyPress(EventKeyPress event) {
      if (event != null && this.fakePlayer != null && mc != null && mc.gui.screen() == null) {
         int key = event.getKey();
         int action = event.getAction();
         if (key == 265) {
            this.moveForward = this.isKeyDown(action) ? 1.0F : 0.0F;
         } else if (key == 264) {
            this.moveForward = this.isKeyDown(action) ? -1.0F : 0.0F;
         } else if (key == 263) {
            this.moveStrafe = this.isKeyDown(action) ? 1.0F : 0.0F;
         } else if (key == 262) {
            this.moveStrafe = this.isKeyDown(action) ? -1.0F : 0.0F;
         }
      }
   }

   private void spawnFakePlayerIfNeeded() {
      if (mc != null && mc.level != null && mc.player != null) {
         if (this.fakePlayer == null || this.fakePlayer.isRemoved() || mc.level.getEntity(this.fakePlayer.getId()) != this.fakePlayer) {
            this.removeFakePlayer();
            if (mc.level.getEntity(-23467) != null) {
               mc.level.getEntity(-23467).discard();
            }

            this.fakePlayer = new RemotePlayer(mc.level, new GameProfile(FAKE_PLAYER_UUID, "FakePlayer"));
            this.fakePlayer.setId(-23467);
            this.fakePlayer.copyPosition(mc.player);
            this.fakePlayer.setYHeadRot(mc.player.getYHeadRot());
            this.fakePlayer.yBodyRot = mc.player.yBodyRot;
            this.fakePlayer.setPose(mc.player.getPose());
            this.fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().copy());
            this.fakePlayer.setItemInHand(InteractionHand.OFF_HAND, mc.player.getOffhandItem().copy());

            for (EquipmentSlot slot : EquipmentSlot.values()) {
               if (slot.getType() == Type.HUMANOID_ARMOR) {
                  this.fakePlayer.setItemSlot(slot, mc.player.getItemBySlot(slot).copy());
               }
            }

            this.fakePlayer.setHealth(Math.max(1.0F, mc.player.getHealth()));
            this.fakePlayer.setAbsorptionAmount(mc.player.getAbsorptionAmount());
            this.fakePlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 9999, 2, false, false));
            this.fakePlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 9999, 4, false, false));
            this.fakePlayer.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 9999, 1, false, false));
            mc.level.addEntity(this.fakePlayer);
         }
      }
   }

   private void removeFakePlayer() {
      this.moveForward = 0.0F;
      this.moveStrafe = 0.0F;
      if (this.fakePlayer != null) {
         this.fakePlayer.discard();
         this.fakePlayer = null;
      }
   }

   private void updateFakeMovement() {
      if (this.fakePlayer != null && mc != null && mc.player != null) {
         if (this.moveForward == 0.0F && this.moveStrafe == 0.0F) {
            this.fakePlayer.setSprinting(false);
            this.fakePlayer.setDeltaMovement(0.0, this.fakePlayer.getDeltaMovement().y, 0.0);
            this.fakePlayer.walkAnimation.setSpeed(0.0F);
         } else {
            float yaw = mc.player.getYRot();
            double speed = 0.2;
            double motionX = this.moveStrafe * Math.cos(Math.toRadians(yaw)) - this.moveForward * Math.sin(Math.toRadians(yaw));
            double motionZ = this.moveForward * Math.cos(Math.toRadians(yaw)) + this.moveStrafe * Math.sin(Math.toRadians(yaw));
            Vec3 velocity = new Vec3(motionX * speed, this.fakePlayer.getDeltaMovement().y, motionZ * speed);
            this.fakePlayer.setDeltaMovement(velocity);
            this.fakePlayer.move(MoverType.SELF, velocity);
            this.fakePlayer.setSprinting(true);
         }
      }
   }

   private boolean isKeyDown(int action) {
      return action == 1 || action == 2;
   }
}
