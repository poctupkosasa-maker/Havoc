package havoc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import havoc.Havoc;
import havoc.utilites.color.ClientTheme;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public final class ThemeManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final int NAME_MAX_LENGTH = 50;
   private static final int BUILTIN_VERSION = 3;
   private static final String FILE_SUFFIX = ".theme.json";
   private static final Map<String, Map<String, Integer>> COLOR_PRESETS = Map.of(
      "Ocean", coloredPreset(0xFF38BDF8, 0xFF67E8F9, 0xFF081523, 0xFF102337),
      "Forest", coloredPreset(0xFF4ADE80, 0xFFA3E635, 0xFF0B1812, 0xFF142A20),
      "Sunset", coloredPreset(0xFFFB923C, 0xFFFB7185, 0xFF20121A, 0xFF35202C),
      "Amethyst", coloredPreset(0xFFC084FC, 0xFFF0ABFC, 0xFF160F24, 0xFF261B3A),
      "Rose", coloredPreset(0xFFF472B6, 0xFFFDA4AF, 0xFF200F19, 0xFF361C2B),
      "Nord", coloredPreset(0xFF88C0D0, 0xFF81A1C1, 0xFF242933, 0xFF2E3440)
   );
   private final Path themeDir = ConfigManager.getRootDir().resolve("themes");

   public ThemeManager() {
      try {
         Files.createDirectories(this.themeDir);
      } catch (IOException var2) {
      }

      this.ensureBuiltins();
   }

   public List<ThemeManager.ThemeEntry> listThemes() {
      List<ThemeManager.ThemeEntry> entries = new ArrayList<>();

      try {
         Files.createDirectories(this.themeDir);

         try (Stream<Path> paths = Files.list(this.themeDir)) {
            paths.filter(x$0 -> Files.isRegularFile(x$0))
               .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".theme.json"))
               .forEach(path -> {
                  String fileName = path.getFileName().toString();
                  String name = fileName.substring(0, fileName.length() - ".theme.json".length());
                  entries.add(new ThemeManager.ThemeEntry(name, this.resolveCreatedAt(path), this.previewColor(path)));
               });
         }
      } catch (IOException var7) {
      }

      entries.sort((a, b) -> {
         if ("Dark".equalsIgnoreCase(a.name())) {
            return -1;
         } else if ("Dark".equalsIgnoreCase(b.name())) {
            return 1;
         } else if ("Light".equalsIgnoreCase(a.name())) {
            return -1;
         } else {
            return "Light".equalsIgnoreCase(b.name()) ? 1 : a.name().compareToIgnoreCase(b.name());
         }
      });
      return entries;
   }

   public String createTheme() {
      String base = "Новая тема";
      String candidate = base;
      int index = 2;
      List<String> names = this.listThemes().stream().map(ThemeManager.ThemeEntry::name).toList();

      while (this.containsIgnoreCase(names, candidate)) {
         candidate = base + " " + index++;
      }

      if (!this.saveTheme(candidate)) {
         return null;
      }

      ClientTheme.setActiveThemeName(candidate);
      Havoc.getInstance().markConfigDirty();
      return candidate;
   }

   public boolean saveTheme(String name) {
      String safe = this.sanitize(name);
      if (safe != null && !this.isBuiltin(safe)) {
         ThemeManager.ThemeFile file = new ThemeManager.ThemeFile();
         file.version = 2;
         file.createdAtMs = this.resolveCreatedAt(this.themePath(safe));
         file.light = ClientTheme.isLightMode();
         file.colors = ClientTheme.captureColors();
         return this.writeTheme(safe, file);
      } else {
         return false;
      }
   }

   public boolean loadTheme(String name) {
      String safe = this.sanitize(name);
      if (safe == null) {
         return false;
      }

      if ("Dark".equalsIgnoreCase(safe)) {
         ClientTheme.setLightMode(false);
         Havoc.getInstance().markConfigDirty();
         return true;
      }

      if ("Light".equalsIgnoreCase(safe)) {
         ClientTheme.setLightMode(true);
         Havoc.getInstance().markConfigDirty();
         return true;
      }

      if ("Rainbow".equalsIgnoreCase(safe)) {
         ClientTheme.enableRainbowMode();
         Havoc.getInstance().markConfigDirty();
         return true;
      }

      Path path = this.themePath(safe);
      if (!Files.exists(path)) {
         return false;
      }

      try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
         ThemeManager.ThemeFile file = (ThemeManager.ThemeFile)GSON.fromJson(reader, ThemeManager.ThemeFile.class);
         if (file == null) {
            return false;
         } else if (file.rainbow || "Rainbow".equalsIgnoreCase(safe)) {
            ClientTheme.enableRainbowMode();
            Havoc.getInstance().markConfigDirty();
            return true;
         } else if (file.colors != null && !file.colors.isEmpty()) {
            ClientTheme.setLightMode(file.light);
            ClientTheme.applyColors(file.colors);
            ClientTheme.setActiveThemeName(safe);
            Havoc.getInstance().markConfigDirty();
            return true;
         } else {
            return false;
         }
      } catch (Exception ignored) {
         return false;
      }
   }

   public boolean deleteTheme(String name) {
      String safe = this.sanitize(name);
      if (safe != null && !this.isBuiltin(safe)) {
         try {
            boolean deleted = Files.deleteIfExists(this.themePath(safe));
            if (deleted && safe.equalsIgnoreCase(ClientTheme.getActiveThemeName())) {
               this.loadTheme(ClientTheme.isLightMode() ? "Light" : "Dark");
            }

            return deleted;
         } catch (IOException ignored) {
            return false;
         }
      } else {
         return false;
      }
   }

   public String renameTheme(String from, String to) {
      String safeFrom = this.sanitize(from);
      String safeTo = this.sanitize(to);
      if (safeFrom != null && safeTo != null && !this.isBuiltin(safeFrom) && !this.isBuiltin(safeTo)) {
         if (safeFrom.equals(safeTo)) {
            return safeFrom;
         }

         for (ThemeManager.ThemeEntry entry : this.listThemes()) {
            if (!entry.name().equalsIgnoreCase(safeFrom) && entry.name().equalsIgnoreCase(safeTo)) {
               return null;
            }
         }

         Path source = this.themePath(safeFrom);
         Path target = this.themePath(safeTo);
         if (Files.isRegularFile(source) && !Files.exists(target)) {
            try {
               Files.move(source, target);
               if (safeFrom.equalsIgnoreCase(ClientTheme.getActiveThemeName())) {
                  ClientTheme.setActiveThemeName(safeTo);
               }

               Havoc.getInstance().markConfigDirty();
               return safeTo;
            } catch (IOException ignored) {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public void reload() {
      this.ensureBuiltins();
   }

   public void openThemeFolder() {
      try {
         Files.createDirectories(this.themeDir);
         if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Action.OPEN)) {
               desktop.open(this.themeDir.toFile());
               return;
            }
         }

         new ProcessBuilder("explorer.exe", this.themeDir.toAbsolutePath().toString()).start();
      } catch (Exception var2) {
      }
   }

   public boolean isBuiltin(String name) {
      return "Dark".equalsIgnoreCase(name) || "Light".equalsIgnoreCase(name) || "Rainbow".equalsIgnoreCase(name)
         || COLOR_PRESETS.keySet().stream().anyMatch(preset -> preset.equalsIgnoreCase(name));
   }

   private void ensureBuiltins() {
      try {
         Files.createDirectories(this.themeDir);
      } catch (IOException var5) {
      }

      Path dark = this.themePath("Dark");
      if (this.requiresBuiltinRefresh(dark)) {
         ThemeManager.ThemeFile file = new ThemeManager.ThemeFile();
         file.version = 3;
         file.createdAtMs = System.currentTimeMillis();
         file.light = false;
         file.colors = ClientTheme.darkPreset();
         this.writeTheme("Dark", file);
      }

      Path light = this.themePath("Light");
      if (this.requiresBuiltinRefresh(light)) {
         ThemeManager.ThemeFile file = new ThemeManager.ThemeFile();
         file.version = 3;
         file.createdAtMs = System.currentTimeMillis();
         file.light = true;
         file.colors = ClientTheme.lightPreset();
         this.writeTheme("Light", file);
      }

      Path rainbow = this.themePath("Rainbow");
      if (this.requiresBuiltinRefresh(rainbow)) {
         ThemeManager.ThemeFile file = new ThemeManager.ThemeFile();
         file.version = 3;
         file.createdAtMs = System.currentTimeMillis();
         file.light = false;
         file.rainbow = true;
         file.colors = ClientTheme.darkPreset();
         this.writeTheme("Rainbow", file);
      }

      COLOR_PRESETS.forEach((name, colors) -> {
         if (this.requiresBuiltinRefresh(this.themePath(name))) {
            ThemeManager.ThemeFile file = new ThemeManager.ThemeFile();
            file.version = BUILTIN_VERSION;
            file.createdAtMs = System.currentTimeMillis();
            file.colors = colors;
            this.writeTheme(name, file);
         }
      });
   }

   private static Map<String, Integer> coloredPreset(int accent, int secondary, int background, int surface) {
      Map<String, Integer> colors = new LinkedHashMap<>(ClientTheme.darkPreset());
      colors.put(ClientTheme.ACCENT, accent);
      colors.put(ClientTheme.ACCENT_SECOND, secondary);
      colors.put(ClientTheme.MENU_BACKGROUND, background);
      colors.put(ClientTheme.SIDEBAR_BACKGROUND, background);
      colors.put(ClientTheme.SETTINGS_BACKGROUND, surface);
      colors.put(ClientTheme.OUTLINE, (accent & 0x00FFFFFF) | 0x40000000);
      colors.put(ClientTheme.SECONDARY_TEXT, 0xFFADB6C6);
      colors.put(ClientTheme.SCROLLBAR, (secondary & 0x00FFFFFF) | 0xDC000000);
      colors.put(ClientTheme.HUD_BACKGROUND, (background & 0x00FFFFFF) | 0xB0000000);
      colors.put(ClientTheme.HUD_INSET, (surface & 0x00FFFFFF) | 0x80000000);
      colors.put(ClientTheme.HUD_OUTLINE, (accent & 0x00FFFFFF) | 0x40000000);
      return Map.copyOf(colors);
   }

   private boolean writeTheme(String name, ThemeManager.ThemeFile file) {
      try {
         Files.createDirectories(this.themeDir);

         try (Writer writer = Files.newBufferedWriter(
               this.themePath(name), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE
            )) {
            GSON.toJson(file, writer);
         }

         return true;
      } catch (IOException ignored) {
         return false;
      }
   }

   private boolean requiresBuiltinRefresh(Path path) {
      if (!Files.exists(path)) {
         return true;
      }

      try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
         ThemeManager.ThemeFile file = (ThemeManager.ThemeFile)GSON.fromJson(reader, ThemeManager.ThemeFile.class);
         return file == null || file.version < 3;
      } catch (Exception ignored) {
         return true;
      }
   }

   private int previewColor(Path path) {
      try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
         ThemeManager.ThemeFile file = (ThemeManager.ThemeFile)GSON.fromJson(reader, ThemeManager.ThemeFile.class);
         if (file != null && file.rainbow) {
            return -45620;
         }

         Integer color = file != null && file.colors != null ? file.colors.get("accent") : null;
         return color == null ? ClientTheme.getAccentSetting().getColor() : color;
      } catch (Exception ignored) {
         return ClientTheme.getAccentSetting().getColor();
      }
   }

   private long resolveCreatedAt(Path path) {
      if (!Files.exists(path)) {
         return System.currentTimeMillis();
      }

      try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
         ThemeManager.ThemeFile file = (ThemeManager.ThemeFile)GSON.fromJson(reader, ThemeManager.ThemeFile.class);
         if (file != null && file.createdAtMs > 0L) {
            return file.createdAtMs;
         }
      } catch (Exception var9) {
      }

      try {
         return Files.readAttributes(path, BasicFileAttributes.class).creationTime().toMillis();
      } catch (Exception ignored) {
         return System.currentTimeMillis();
      }
   }

   private Path themePath(String name) {
      return this.themeDir.resolve(name + ".theme.json");
   }

   private String sanitize(String input) {
      if (input == null) {
         return null;
      }

      String safe = input.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
      if (safe.isEmpty()) {
         return null;
      }

      if (safe.length() > 50) {
         safe = safe.substring(0, 50);
      }

      return safe;
   }

   private boolean containsIgnoreCase(List<String> values, String target) {
      for (String value : values) {
         if (value.equalsIgnoreCase(target)) {
            return true;
         }
      }

      return false;
   }

   public record ThemeEntry(String name, long createdAtMs, int previewColor) {
   }

   private static final class ThemeFile {
      int version;
      long createdAtMs;
      boolean light;
      boolean rainbow;
      Map<String, Integer> colors;
   }
}
