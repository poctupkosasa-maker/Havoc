package havoc.utilites;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class ServerInfoHelper {
   private static final long[] TICK_TIMES = new long[60];
   private static int tickIndex = 0;
   private static int tickCount = 0;
   private static long lastTickNanos = 0L;
   private static float cachedTps = 20.0F;

   private ServerInfoHelper() {
   }

   public static String getServerName() {
      Minecraft mc = Minecraft.getInstance();
      if (mc == null) {
         return "Local";
      }

      if (mc.isLocalServer()) {
         return "Local";
      }

      ServerData info = mc.getCurrentServer();
      return info != null && info.ip != null ? info.ip : "Unknown";
   }

   public static float getTps() {
      return cachedTps;
   }

   public static void onTick() {
      long now = System.nanoTime();
      if (lastTickNanos == 0L) {
         lastTickNanos = now;
      } else {
         long delta = now - lastTickNanos;
         lastTickNanos = now;
         if (delta <= 500000000L) {
            TICK_TIMES[tickIndex] = delta;
            tickIndex = (tickIndex + 1) % TICK_TIMES.length;
            if (tickCount < TICK_TIMES.length) {
               tickCount++;
            }

            long sum = 0L;

            for (int i = 0; i < tickCount; i++) {
               sum += TICK_TIMES[i];
            }

            float avgMs = (float)sum / (tickCount * 1000000.0F);
            cachedTps = avgMs > 0.01F ? Math.min(20.0F, 1000.0F / avgMs) : 20.0F;
         }
      }
   }
}
