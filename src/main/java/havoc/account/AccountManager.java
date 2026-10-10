package havoc.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import havoc.config.ConfigManager;
import havoc.mixin.MinecraftUserAccessor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

public final class AccountManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static List<Stored> accounts;

   private AccountManager() {
   }

   public static final class Stored {
      public String name;
      public String uuid;
      public long addedAt;
      public long lastUsed;

      Stored() {
      }

      public Stored(String name) {
         this.name = name;
         this.uuid = offlineUuid(name).toString();
         this.addedAt = System.currentTimeMillis();
         this.lastUsed = this.addedAt;
      }
   }

   public static synchronized List<Stored> get() {
      if (accounts == null) {
         accounts = load();
         String active = activeName();
         if (active != null && find(active) == null) {
            accounts.add(new Stored(active));
            save();
         }
      }

      return accounts;
   }

   public static Stored find(String name) {
      if (name == null) {
         return null;
      } else {
         for (Stored stored : get()) {
            if (name.equalsIgnoreCase(stored.name)) {
               return stored;
            }
         }

         return null;
      }
   }

   public static boolean add(String name) {
      String clean = sanitize(name);
      if (clean == null) {
         return false;
      } else if (find(clean) != null) {
         apply(clean);
         return false;
      } else {
         get().add(new Stored(clean));
         save();
         return true;
      }
   }

   public static void remove(String name) {
      Stored stored = find(name);
      if (stored != null && get().size() > 1) {
         get().remove(stored);
         save();
      }
   }

   public static String activeName() {
      Minecraft mc = Minecraft.getInstance();
      return mc == null ? null : mc.getUser().getName();
   }

   public static void apply(String name) {
      Stored stored = find(name);
      if (stored != null) {
         Minecraft mc = Minecraft.getInstance();
         if (mc == null) {
            return;
         }

         User old = mc.getUser();
         User next = new User(stored.name, UUID.fromString(stored.uuid), old.getAccessToken(), old.getClientId(), old.getXuid());
         ((MinecraftUserAccessor)mc).havoc$setUser(next);
         mc.updateTitle();
         stored.lastUsed = System.currentTimeMillis();
         save();
      }
   }

   public static UUID offlineUuid(String name) {
      return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
   }

   public static String sanitize(String raw) {
      if (raw == null) {
         return null;
      } else {
         String trimmed = raw.trim();
         if (trimmed.length() >= 3 && trimmed.length() <= 16) {
            for (int i = 0; i < trimmed.length(); i++) {
               char c = trimmed.charAt(i);
               if (!Character.isLetterOrDigit(c) && c != '_') {
                  return null;
               }
            }

            return trimmed;
         } else {
            return null;
         }
      }
   }

   private static Path file() {
      return ConfigManager.getRootDir().resolve("accounts.json");
   }

   private static List<Stored> load() {
      try {
         Path path = file();
         if (Files.exists(path)) {
            List<Stored> parsed = GSON.fromJson(Files.readString(path), AccountsFile.class).accounts;
            if (parsed != null) {
               return parsed;
            }
         }
      } catch (Exception ignored) {
      }

      return new ArrayList<>();
   }

   private static synchronized void save() {
      try {
         Files.createDirectories(file().getParent());
         AccountsFile data = new AccountsFile();
         data.accounts = get();
         Files.writeString(file(), GSON.toJson(data));
      } catch (Exception ignored) {
      }
   }

   private static final class AccountsFile {
      private List<Stored> accounts = new ArrayList<>();
   }
}
