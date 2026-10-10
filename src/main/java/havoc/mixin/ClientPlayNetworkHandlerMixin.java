package havoc.mixin;

import havoc.Havoc;
import havoc.event.impl.EventEntityDeath;
import havoc.event.impl.EventTotemPop;
import havoc.module.impl.visuals.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {
   @Shadow
   @Final
   private ClientLevel level;

   @Inject(method = "handleEntityEvent", at = @At("HEAD"))
   private void havoc$onEntityStatus(ClientboundEntityEventPacket packet, CallbackInfo ci) {
      if (packet != null) {
         Havoc havoc = Havoc.getInstance();
         if (havoc != null && havoc.getEventBus() != null && this.level != null) {
            Entity entity = packet.getEntity(this.level);
            if (packet.getEventId() == 35 && entity != null) {
               havoc.getEventBus().post(new EventTotemPop(entity));
            } else if (packet.getEventId() == 3 && entity instanceof LivingEntity living) {
               havoc.getEventBus().post(new EventEntityDeath(living));
            }
         }
      }
   }

   @Redirect(
      method = "handleEntityEvent",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/particle/ParticleEngine;createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V"
      )
   )
   private void havoc$skipVanillaTotemParticles(ParticleEngine manager, Entity entity, ParticleOptions effect, int maxAge) {
      if (effect == ParticleTypes.TOTEM_OF_UNDYING) {
         Havoc havoc = Havoc.getInstance();
         if (havoc != null && havoc.getModuleManager() != null) {
            Particles particles = havoc.getModuleManager().getModule(Particles.class);
            if (particles != null && particles.shouldHideVanillaTotemParticles()) {
               return;
            }
         }
      }

      manager.createTrackingEmitter(entity, effect, maxAge);
   }
}
