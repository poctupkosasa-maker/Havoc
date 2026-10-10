package havoc.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.SliderSetting;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

@ModuleInfo(name = "BackTrack", desc = "Tracks delayed player positions", category = Category.COMBAT)
public final class BackTrack extends Module {
   private static final int MAX_SAMPLES = 48;
   private final SliderSetting delay = this.registerSetting(new SliderSetting("Delay", 200.0F, 50.0F, 1000.0F, 10.0F, "ms"));
   private final SliderSetting distance = this.registerSetting(new SliderSetting("Distance", 6.0F, 3.0F, 10.0F, 0.1F));
   private final Map<Integer, ArrayDeque<BackTrack.Snapshot>> history = new HashMap<>();

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.level != null) {
         long now = System.currentTimeMillis();
         double maxSq = Math.pow(this.distance.getValue().floatValue() + 2.0, 2.0);

         for (Player player : mc.level.players()) {
            if (player != mc.player && player.isAlive() && !(mc.player.distanceToSqr(player) > maxSq)) {
               ArrayDeque<BackTrack.Snapshot> samples = this.history.computeIfAbsent(player.getId(), ignored -> new ArrayDeque<>());
               samples.addLast(new BackTrack.Snapshot(now, player.getBoundingBox()));

               while (samples.size() > 48 || !samples.isEmpty() && (float)(now - samples.peekFirst().time) > this.delay.getValue() + 650.0F) {
                  samples.pollFirst();
               }
            }
         }

         this.history.keySet().removeIf(id -> mc.level.getEntity(id) == null);
      } else {
         this.history.clear();
      }
   }

   public AABB getDelayedBox(LivingEntity entity) {
      ArrayDeque<BackTrack.Snapshot> samples = this.history.get(entity.getId());
      if (samples != null && !samples.isEmpty()) {
         long targetTime = System.currentTimeMillis() - this.delay.getValue().longValue();
         BackTrack.Snapshot best = null;
         long bestDelta = Long.MAX_VALUE;

         for (BackTrack.Snapshot sample : samples) {
            long delta = Math.abs(sample.time - targetTime);
            if (delta < bestDelta) {
               best = sample;
               bestDelta = delta;
            }
         }

         return best == null ? null : best.box;
      } else {
         return null;
      }
   }

   @Override
   public void onDisable() {
      this.history.clear();
      super.onDisable();
   }

   private record Snapshot(long time, AABB box) {
   }
}
