package havoc.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

public final class DiscordClient {
   private static final String APP_ID = "1548733307183956140";
   public static final String DISCORD_URL = "https://discord.gg/5n4GYTXez";
   public static final String TELEGRAM_URL = "https://t.me/fllowdlc";
   private final DiscordIpc ipc = new DiscordIpc(APP_ID);
   private final long startedAt = System.currentTimeMillis() / 1000L;
   private volatile boolean running;
   private Thread thread;
   private String lastSentJson = "";

   public synchronized void start() {
      if (!this.running) {
         this.running = true;
         this.startThread();
      }
   }

   public synchronized void stop() {
      if (this.running) {
         this.running = false;
         if (this.thread != null) {
            this.thread.interrupt();
            this.thread = null;
         }

         this.ipc.close();
      }
   }

   private void startThread() {
      this.thread = new Thread(this::loop, "havoc-discord-rpc");
      this.thread.setDaemon(true);
      this.thread.start();
   }

   private void loop() {
      int retryAfter = 0;
      int age = 0;

      while (this.running && !Thread.currentThread().isInterrupted()) {
         try {
            boolean stale = age >= 30;
            if (stale) {
               age = 0;
            }

            if (stale || retryAfter <= 0) {
               String json = buildActivityJson();
               if (stale || !json.equals(this.lastSentJson)) {
                  if (this.ipc.setActivity(json)) {
                     this.lastSentJson = json;
                     retryAfter = 0;
                  } else {
                     retryAfter = 15;
                  }
               }
            } else {
               retryAfter--;
            }

            Thread.sleep(2000L);
            age++;
         } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            break;
         } catch (Throwable ignored) {
         }
      }
   }

   private String buildActivityJson() {
      String nickname = "";
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.getUser() != null) {
         nickname = mc.getUser().getName();
      }

      JsonObject activity = new JsonObject();
      activity.addProperty("details", "Havoc Visual");
      activity.addProperty("state", nickname.isEmpty() ? "Minecraft 26.2" : "Playing as " + nickname);
      JsonObject timestamps = new JsonObject();
      timestamps.addProperty("start", this.startedAt);
      activity.add("timestamps", timestamps);
      JsonObject assets = new JsonObject();
      assets.addProperty("large_image", "havoc");
      assets.addProperty("large_text", "discord.gg/5n4GYTXez");
      assets.addProperty("small_image", "havoc");
      assets.addProperty("small_text", "t.me/fllowdlc");
      activity.add("assets", assets);
      JsonArray buttons = new JsonArray();
      buttons.add(button("Discord", DISCORD_URL));
      buttons.add(button("Telegram", TELEGRAM_URL));
      activity.add("buttons", buttons);
      return activity.toString();
   }

   private static JsonObject button(String label, String url) {
      JsonObject object = new JsonObject();
      object.addProperty("label", label);
      object.addProperty("url", url);
      return object;
   }
}
