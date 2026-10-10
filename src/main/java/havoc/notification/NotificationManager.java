package havoc.notification;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.network.chat.Component;

public final class NotificationManager {
   private final List<NotificationManager.Notification> notifications = new ArrayList<>();
   private long nextId = 1L;

   public synchronized void info(String message) {
      this.post(NotificationType.INFO, message, 2600L);
   }

   public synchronized void success(String message) {
      this.post(NotificationType.SUCCESS, message, 2600L);
   }

   public synchronized void success(Component message) {
      this.post(NotificationType.SUCCESS, message, 2600L);
   }

   public synchronized void warning(String message) {
      this.post(NotificationType.WARNING, message, 3200L);
   }

   public synchronized void module(String message, boolean enabled) {
      this.postModule(message, enabled, 2200L);
   }

   public synchronized void post(NotificationType type, String message, long durationMs) {
      this.post(type, message, durationMs, null);
   }

   public synchronized void postModule(String message, boolean enabled, long durationMs) {
      this.post(NotificationType.MODULE, message, durationMs, enabled);
   }

   public synchronized void post(NotificationType type, String message, long durationMs, Boolean moduleEnabled) {
      if (type != null && message != null && !message.isBlank()) {
         long now = System.currentTimeMillis();
         long expiresAt = now + Math.max(300L, durationMs);
         this.notifications.add(new NotificationManager.Notification(this.nextId++, type, message, null, expiresAt, moduleEnabled));
         if (this.notifications.size() > 16) {
            this.notifications.removeFirst();
         }
      }
   }

   public synchronized void post(NotificationType type, Component message, long durationMs) {
      if (type != null && message != null && !message.getString().isBlank()) {
         long now = System.currentTimeMillis();
         long expiresAt = now + Math.max(300L, durationMs);
         this.notifications.add(new NotificationManager.Notification(this.nextId++, type, message.getString(), message, expiresAt, null));
         if (this.notifications.size() > 16) {
            this.notifications.removeFirst();
         }
      }
   }

   public synchronized List<NotificationManager.Notification> snapshot() {
      return this.notifications.stream().sorted(Comparator.comparingLong(NotificationManager.Notification::id)).toList();
   }

   public synchronized void remove(long id) {
      this.notifications.removeIf(n -> n.id == id);
   }

   public static final class Notification {
      private final long id;
      private final NotificationType type;
      private final String message;
      private final Component styledMessage;
      private final long expiresAt;
      private final Boolean moduleEnabled;

      private Notification(long id, NotificationType type, String message, Component styledMessage, long expiresAt, Boolean moduleEnabled) {
         this.id = id;
         this.type = type;
         this.message = message;
         this.styledMessage = styledMessage;
         this.expiresAt = expiresAt;
         this.moduleEnabled = moduleEnabled;
      }

      public long id() {
         return this.id;
      }

      public NotificationType type() {
         return this.type;
      }

      public String message() {
         return this.message;
      }

      public Component styledMessage() {
         return this.styledMessage;
      }

      public long expiresAt() {
         return this.expiresAt;
      }

      public Boolean moduleEnabled() {
         return this.moduleEnabled;
      }
   }
}
