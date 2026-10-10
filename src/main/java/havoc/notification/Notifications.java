package havoc.notification;

import havoc.Havoc;
import net.minecraft.network.chat.Component;

public final class Notifications {
   private Notifications() {
   }

   public static void info(String message) {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null && havoc.getNotificationManager() != null) {
         havoc.getNotificationManager().info(message);
      }
   }

   public static void success(String message) {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null && havoc.getNotificationManager() != null) {
         havoc.getNotificationManager().success(message);
      }
   }

   public static void success(Component message) {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null && havoc.getNotificationManager() != null) {
         havoc.getNotificationManager().success(message);
      }
   }

   public static void warning(String message) {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null && havoc.getNotificationManager() != null) {
         havoc.getNotificationManager().warning(message);
      }
   }

   public static void post(NotificationType type, String message, long durationMs) {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null && havoc.getNotificationManager() != null) {
         havoc.getNotificationManager().post(type, message, durationMs);
      }
   }

   public static void module(String message, boolean enabled) {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null && havoc.getNotificationManager() != null) {
         havoc.getNotificationManager().module(message, enabled);
      }
   }
}
