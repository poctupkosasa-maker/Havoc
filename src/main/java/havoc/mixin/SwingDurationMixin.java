package havoc.mixin;

import havoc.Havoc;
import havoc.module.impl.visuals.SwingAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class SwingDurationMixin {
   @Inject(method = "getCurrentSwingDuration", at = @At("HEAD"), cancellable = true)
   private void havoc$swingDuration(CallbackInfoReturnable<Integer> cir) {
      Minecraft client = Minecraft.getInstance();
      if ((Object)this == client.player) {
         SwingAnimation animation = Havoc.getInstance().getModuleManager().getModule(SwingAnimation.class);
         if (animation != null && animation.shouldModifyDuration()) {
            LivingEntity entity = (LivingEntity)(Object)this;
            float duration = animation.getDuration();
            if (MobEffectUtil.hasDigSpeed(entity)) {
               duration *= 6 - (1 + MobEffectUtil.getDigSpeedAmplification(entity));
            } else if (entity.hasEffect(MobEffects.MINING_FATIGUE)) {
               duration *= 6 + (1 + entity.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) * 2;
            } else {
               duration *= 6.0F;
            }

            cir.setReturnValue(Math.max(1, (int)duration));
         }
      }
   }
}
