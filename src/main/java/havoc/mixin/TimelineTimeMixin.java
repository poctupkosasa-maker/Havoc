package havoc.mixin;

import havoc.module.impl.visuals.Ambience;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.timeline.AttributeTrackSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AttributeTrackSampler.class)
public abstract class TimelineTimeMixin {
   @Redirect(
      method = "applyTimeBased",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/clock/ClockManager;getTotalTicks(Lnet/minecraft/core/Holder;)J")
   )
   private long havoc$useAmbienceTime(ClockManager clockManager, Holder<WorldClock> clock) {
      Ambience ambience = Ambience.getInstance();
      return ambience != null && ambience.shouldOverrideTime() ? ambience.getForcedTimeOfDay() : clockManager.getTotalTicks(clock);
   }
}
