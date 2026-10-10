package havoc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import havoc.Havoc;
import havoc.event.impl.FireworkEvent;
import havoc.utilites.IMinecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin implements IMinecraft {
   @Shadow
   @Nullable
   private LivingEntity attachedToEntity;

   @WrapOperation(
      method = "tick",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V")
   )
   private void havoc$fireworkVelocity(LivingEntity entity, Vec3 velocity, Operation<Void> original) {
      if (mc.player != null && this.attachedToEntity == mc.player && this.attachedToEntity.isFallFlying()) {
         FireworkEvent event = new FireworkEvent(velocity);
         Havoc.getInstance().getEventBus().post(event);
         original.call(new Object[]{entity, event.getVector()});
      } else {
         original.call(new Object[]{entity, velocity});
      }
   }
}
