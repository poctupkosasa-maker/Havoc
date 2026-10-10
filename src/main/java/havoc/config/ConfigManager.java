package havoc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import havoc.module.base.Module;
import havoc.module.base.ModuleManager;
import havoc.module.bind.BindData;
import havoc.module.bind.BindMode;
import havoc.module.impl.visuals.Interface;
import havoc.module.setting.AbstractSetting;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.ColorSetting;
import havoc.module.setting.InputSetting;
import havoc.module.setting.MultiSelectSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.Setting;
import havoc.module.setting.SliderSetting;
import havoc.screen.screen.HudElement;
import havoc.utilites.color.ClientTheme;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Generated;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final String AUTO_CONFIG_NAME = "auto";
   private static final int CONFIG_NAME_MAX_LENGTH = 50;
   private static final long AUTO_SAVE_INTERVAL_MS = 150L;
   private final ModuleManager moduleManager;
   private final Path configDir;
   private final ConfigManager.MenuState menuState = new ConfigManager.MenuState();
   private boolean dirty = true;
   private boolean loading;
   private long lastAutoSaveMs;
   private String loadedConfigName = "auto";

   public ConfigManager(ModuleManager moduleManager) {
      this.moduleManager = moduleManager;
      Path legacyConfigDir = FabricLoader.getInstance().getConfigDir().resolve("havoc");
      this.configDir = getRootDir().resolve("config");

      try {
         Files.createDirectories(this.configDir);
      } catch (IOException var4) {
      }

      this.migrateLegacyConfigs(legacyConfigDir);
      this.loadOrCreateAuto();
   }

   public static Path getRootDir() {
      String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      if (!os.contains("win")) {
         return Path.of(System.getProperty("user.home", "."), "Havoc");
      }

      String drive = System.getenv("SystemDrive");
      return Path.of(drive != null && !drive.isBlank() ? drive + "\\" : "C:\\", "Havoc");
   }

   private void migrateLegacyConfigs(Path legacyConfigDir) {
      if (legacyConfigDir != null && !legacyConfigDir.equals(this.configDir) && Files.isDirectory(legacyConfigDir)) {
         try (Stream<Path> paths = Files.list(legacyConfigDir)) {
            paths.filter(x$0 -> Files.isRegularFile(x$0))
               .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
               .forEach(path -> {
                  Path target = this.configDir.resolve(path.getFileName().toString());
                  if (!Files.exists(target)) {
                     try {
                        Files.copy(path, target, StandardCopyOption.COPY_ATTRIBUTES);
                     } catch (IOException var4) {
                     }
                  }
               });
         } catch (IOException var7) {
         }
      }
   }

   public void markDirty() {
      if (!this.loading) {
         this.dirty = true;
      }
   }

   public void flushAutoSave() {
      if (!this.loading && this.dirty) {
         long now = System.currentTimeMillis();
         if (now - this.lastAutoSaveMs >= 150L) {
            this.saveConfig("auto");
         }
      }
   }

   public List<String> listConfigNames() {
      return this.listConfigEntries().stream().map(ConfigManager.ConfigEntry::name).toList();
   }

   public List<ConfigManager.ConfigEntry> listConfigEntries() {
      List<ConfigManager.ConfigEntry> entries = new ArrayList<>();

      try {
         if (!Files.exists(this.configDir)) {
            Files.createDirectories(this.configDir);
         }

         try (Stream<Path> paths = Files.list(this.configDir)) {
            paths.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")).forEach(path -> {
               String fileName = path.getFileName().toString();
               String name = fileName.substring(0, fileName.length() - 5);
               long createdAt = this.resolveCreatedAt(path);
               entries.add(new ConfigManager.ConfigEntry(name, createdAt));
            });
         }
      } catch (IOException var3) {
      }

      entries.sort((a, b) -> {
         if ("auto".equalsIgnoreCase(a.name())) {
            return -1;
         } else {
            return "auto".equalsIgnoreCase(b.name()) ? 1 : a.name().compareToIgnoreCase(b.name());
         }
      });
      return entries;
   }

   public String createDefaultConfig() {
      String base = "New Config";
      String candidate = base;
      int index = 2;
      List<String> names = this.listConfigNames();

      while (this.containsNameIgnoreCase(names, candidate)) {
         candidate = base + " " + index++;
         if (candidate.length() > 50) {
            candidate = candidate.substring(0, 50);
         }
      }

      return this.createConfig(candidate) ? candidate : null;
   }

   private boolean containsNameIgnoreCase(List<String> names, String name) {
      for (String n : names) {
         if (n.equalsIgnoreCase(name)) {
            return true;
         }
      }

      return false;
   }

   private long resolveCreatedAt(Path path) {
      try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
         ConfigManager.ConfigFile file = (ConfigManager.ConfigFile)GSON.fromJson(reader, ConfigManager.ConfigFile.class);
         if (file != null && file.createdAtMs > 0L) {
            return file.createdAtMs;
         }
      } catch (Exception var10) {
      }

      try {
         BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
         long fromFs = attrs.creationTime().toMillis();
         if (fromFs > 0L) {
            return fromFs;
         }
      } catch (Exception var7) {
      }

      try {
         return Files.getLastModifiedTime(path).toMillis();
      } catch (IOException ignored) {
         return System.currentTimeMillis();
      }
   }

   public boolean createConfig(String name) {
      String safe = this.sanitizeConfigName(name);
      return safe == null ? false : this.saveConfig(safe);
   }

   public boolean saveConfig(String name) {
      String safe = this.sanitizeConfigName(name);
      if (safe == null) {
         return false;
      }

      ConfigManager.ConfigFile file = this.buildSnapshot();
      Path path = this.configPath(safe);
      file.createdAtMs = this.resolveCreatedAt(path);

      try {
         Files.createDirectories(this.configDir);

         try (Writer writer = Files.newBufferedWriter(
               path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE
            )) {
            GSON.toJson(file, writer);
         }

         this.dirty = false;
         this.lastAutoSaveMs = System.currentTimeMillis();
         return true;
      } catch (IOException ignored) {
         return false;
      }
   }

   public boolean loadConfig(String name) {
      String safe = this.sanitizeConfigName(name);
      if (safe == null) {
         return false;
      }

      Path path = this.configPath(safe);
      if (!Files.exists(path)) {
         return false;
      }

      try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
         ConfigManager.ConfigFile file = (ConfigManager.ConfigFile)GSON.fromJson(reader, ConfigManager.ConfigFile.class);
         if (file == null) {
            return false;
         }

         this.applySnapshot(file);
         this.dirty = false;
         this.lastAutoSaveMs = System.currentTimeMillis();
         this.loadedConfigName = safe;
         return true;
      } catch (Exception ignored) {
         return false;
      }
   }

   public boolean renameConfig(String from, String to) {
      String safeFrom = this.sanitizeConfigName(from);
      String safeTo = this.sanitizeConfigName(to);
      if (safeFrom != null && safeTo != null && !"auto".equalsIgnoreCase(safeFrom)) {
         Path fromPath = this.configPath(safeFrom);
         Path toPath = this.configPath(safeTo);
         if (!Files.exists(fromPath)) {
            return false;
         }

         try {
            Files.move(fromPath, toPath);
            if (this.loadedConfigName != null && this.loadedConfigName.equalsIgnoreCase(safeFrom)) {
               this.loadedConfigName = safeTo;
            }

            return true;
         } catch (IOException ignored) {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean deleteConfig(String name) {
      String safe = this.sanitizeConfigName(name);
      if (safe != null && !"auto".equalsIgnoreCase(safe)) {
         Path path = this.configPath(safe);

         try {
            boolean deleted = Files.deleteIfExists(path);
            if (deleted && this.loadedConfigName != null && this.loadedConfigName.equalsIgnoreCase(safe)) {
               this.loadedConfigName = "auto";
            }

            return deleted;
         } catch (IOException ignored) {
            return false;
         }
      } else {
         return false;
      }
   }

   public void reload() {
      this.listConfigNames();
   }

   public void openConfigFolder() {
      try {
         Files.createDirectories(this.configDir);
         if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Action.OPEN)) {
               desktop.open(this.configDir.toFile());
               return;
            }
         }

         new ProcessBuilder("explorer.exe", this.configDir.toAbsolutePath().toString()).start();
      } catch (Exception var2) {
      }
   }

   public ConfigManager.MenuState getMenuState() {
      ConfigManager.MenuState copy = new ConfigManager.MenuState();
      copy.selectedCategory = this.menuState.selectedCategory;
      copy.selectedExtension = this.menuState.selectedExtension;
      copy.settingsModule = this.menuState.settingsModule;
      copy.searchQuery = this.menuState.searchQuery;
      copy.selectedConfig = this.menuState.selectedConfig;
      copy.configInput = this.menuState.configInput;
      copy.activeTopTab = this.menuState.activeTopTab;
      copy.configsSelectedConfig = this.menuState.configsSelectedConfig;
      copy.configsScroll = this.menuState.configsScroll;
      copy.guiScalePercent = this.menuState.guiScalePercent;
      copy.lightTheme = this.menuState.lightTheme;
      copy.moduleTabScroll = new HashMap<>(this.menuState.moduleTabScroll);
      copy.selectExpanded = new HashMap<>(this.menuState.selectExpanded);
      copy.multiSelectExpanded = new HashMap<>(this.menuState.multiSelectExpanded);
      return copy;
   }

   public void updateMenuState(ConfigManager.MenuState next) {
      if (next != null) {
         boolean changed = !Objects.equals(this.menuState.selectedCategory, next.selectedCategory)
            || !Objects.equals(this.menuState.selectedExtension, next.selectedExtension)
            || !Objects.equals(this.menuState.settingsModule, next.settingsModule)
            || !Objects.equals(this.menuState.searchQuery, next.searchQuery)
            || !Objects.equals(this.menuState.selectedConfig, next.selectedConfig)
            || !Objects.equals(this.menuState.configInput, next.configInput)
            || !Objects.equals(this.menuState.activeTopTab, next.activeTopTab)
            || !Objects.equals(this.menuState.configsSelectedConfig, next.configsSelectedConfig)
            || this.menuState.configsScroll != next.configsScroll
            || this.menuState.guiScalePercent != next.guiScalePercent
            || this.menuState.lightTheme != next.lightTheme
            || !Objects.equals(this.menuState.moduleTabScroll, next.moduleTabScroll)
            || !Objects.equals(this.menuState.selectExpanded, next.selectExpanded)
            || !Objects.equals(this.menuState.multiSelectExpanded, next.multiSelectExpanded);
         this.menuState.selectedCategory = next.selectedCategory;
         this.menuState.selectedExtension = next.selectedExtension;
         this.menuState.settingsModule = next.settingsModule;
         this.menuState.searchQuery = next.searchQuery;
         this.menuState.selectedConfig = next.selectedConfig;
         this.menuState.configInput = next.configInput;
         this.menuState.activeTopTab = next.activeTopTab;
         this.menuState.configsSelectedConfig = next.configsSelectedConfig;
         this.menuState.configsScroll = next.configsScroll;
         this.menuState.guiScalePercent = next.guiScalePercent <= 0 ? 100 : Math.clamp(next.guiScalePercent, 75, 125);
         this.menuState.lightTheme = next.lightTheme;
         this.menuState.moduleTabScroll = next.moduleTabScroll == null ? new HashMap<>() : new HashMap<>(next.moduleTabScroll);
         this.menuState.selectExpanded = next.selectExpanded == null ? new HashMap<>() : new HashMap<>(next.selectExpanded);
         this.menuState.multiSelectExpanded = next.multiSelectExpanded == null ? new HashMap<>() : new HashMap<>(next.multiSelectExpanded);
         if (changed) {
            this.markDirty();
         }
      }
   }

   public void updateMenuState(
      String selectedCategory, String selectedExtension, String settingsModule, String searchQuery, String selectedConfig, String configInput
   ) {
      ConfigManager.MenuState next = this.getMenuState();
      next.selectedCategory = selectedCategory;
      next.selectedExtension = selectedExtension;
      next.settingsModule = settingsModule;
      next.searchQuery = searchQuery;
      next.selectedConfig = selectedConfig;
      next.configInput = configInput;
      this.updateMenuState(next);
   }

   private void loadOrCreateAuto() {
      if (!this.loadConfig("auto")) {
         this.saveConfig("auto");
      }
   }

   private ConfigManager.ConfigFile buildSnapshot() {
      ConfigManager.ConfigFile file = new ConfigManager.ConfigFile();
      file.version = 4;
      file.menuState = this.getMenuState();
      file.menuState.lightTheme = ClientTheme.isLightMode();
      file.themeAccentColor = ClientTheme.getAccentSetting().getColor();
      file.themePresetName = ClientTheme.getActiveThemeName();
      file.themeColors = ClientTheme.captureColors();
      file.modules = new ArrayList<>();

      for (Module module : this.moduleManager.getModules()) {
         ConfigManager.ModuleState moduleState = new ConfigManager.ModuleState();
         moduleState.name = module.getName();
         moduleState.enabled = module.isEnable();
         moduleState.key = module.getKey();
         moduleState.bind = this.captureBind(module.getBind());
         moduleState.settings = new ArrayList<>();

         for (Setting setting : module.getSettings()) {
            ConfigManager.SettingState settingState = this.captureSetting(setting);
            if (settingState != null) {
               moduleState.settings.add(settingState);
            }
         }

         file.modules.add(moduleState);
      }

      Interface interfaceModule = this.moduleManager.getModule(Interface.class);
      if (interfaceModule != null) {
         file.hudElements = new ArrayList<>();

         for (HudElement hudElement : interfaceModule.getHudElements()) {
            ConfigManager.HudElementState hudState = new ConfigManager.HudElementState();
            hudState.id = hudElement.getId();
            hudState.x = hudElement.getX();
            hudState.y = hudElement.getY();
            hudState.settings = new ArrayList<>();

            for (Setting setting : hudElement.getSettings()) {
               ConfigManager.SettingState settingState = this.captureSetting(setting);
               if (settingState != null) {
                  hudState.settings.add(settingState);
               }
            }

            file.hudElements.add(hudState);
         }
      }

      return file;
   }

   private void applySnapshot(ConfigManager.ConfigFile file) {
      this.loading = true;

      try {
         if (file.menuState != null) {
            this.menuState.selectedCategory = file.menuState.selectedCategory;
            this.menuState.selectedExtension = file.menuState.selectedExtension;
            this.menuState.settingsModule = file.menuState.settingsModule;
            this.menuState.searchQuery = file.menuState.searchQuery;
            this.menuState.selectedConfig = file.menuState.selectedConfig;
            this.menuState.configInput = file.menuState.configInput;
            this.menuState.guiScalePercent = file.menuState.guiScalePercent <= 0 ? 100 : Math.clamp(file.menuState.guiScalePercent, 75, 125);
            this.menuState.lightTheme = file.menuState.lightTheme;
            ClientTheme.setLightMode(this.menuState.lightTheme);
         }

         if (file.themeAccentColor != null) {
            ClientTheme.setAccentColor(file.themeAccentColor);
         }

         boolean builtinTheme = "Dark".equalsIgnoreCase(file.themePresetName)
            || "Light".equalsIgnoreCase(file.themePresetName)
            || "Rainbow".equalsIgnoreCase(file.themePresetName);
         if (!builtinTheme && file.themeColors != null && !file.themeColors.isEmpty()) {
            ClientTheme.applyColors(file.themeColors);
         }

         if (file.themePresetName != null && !file.themePresetName.isBlank()) {
            if ("Rainbow".equalsIgnoreCase(file.themePresetName)) {
               ClientTheme.enableRainbowMode();
            } else if ("Light".equalsIgnoreCase(file.themePresetName)) {
               ClientTheme.setLightMode(true);
            } else if ("Dark".equalsIgnoreCase(file.themePresetName)) {
               ClientTheme.setLightMode(false);
            } else {
               ClientTheme.setActiveThemeName(file.themePresetName);
            }
         }

         this.menuState.lightTheme = ClientTheme.isLightMode();
         if (file.modules == null) {
            return;
         }

         for (ConfigManager.ModuleState moduleState : file.modules) {
            Module module = this.findModule(moduleState.name);
            if (module != null) {
               module.setKey(moduleState.key);
               if (moduleState.bind != null) {
                  this.applyBind(module.getBind(), moduleState.bind);
               }

               module.setEnable(moduleState.enabled);
               if (moduleState.settings != null) {
                  for (ConfigManager.SettingState settingState : moduleState.settings) {
                     Setting setting = this.findSetting(module, settingState.name);
                     if (setting instanceof AbstractSetting) {
                        this.applySetting(setting, settingState);
                     }
                  }
               }
            }
         }

         if (file.hudElements != null && !file.hudElements.isEmpty()) {
            this.applyHudElements(file.hudElements);
         }
      } finally {
         this.loading = false;
      }
   }

   private void applyHudElements(List<ConfigManager.HudElementState> hudStates) {
      Interface interfaceModule = this.moduleManager.getModule(Interface.class);
      if (interfaceModule != null) {
         for (ConfigManager.HudElementState hudState : hudStates) {
            HudElement element = this.findHudElement(interfaceModule, hudState.id);
            if (element != null) {
               if (hudState.x != null && hudState.y != null && Float.isFinite(hudState.x) && Float.isFinite(hudState.y)) {
                  element.setPosition(hudState.x, hudState.y);
               }

               if (hudState.settings != null) {
                  for (ConfigManager.SettingState settingState : hudState.settings) {
                     Setting setting = this.findSetting(element.getSettings(), settingState.name);
                     if (setting instanceof AbstractSetting) {
                        this.applySetting(setting, settingState);
                     }
                  }
               }
            }
         }
      }
   }

   private HudElement findHudElement(Interface interfaceModule, String id) {
      if (interfaceModule != null && id != null) {
         for (HudElement element : interfaceModule.getHudElements()) {
            if (id.equals(element.getId())) {
               return element;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private Module findModule(String name) {
      if (name == null) {
         return null;
      }

      for (Module module : this.moduleManager.getModules()) {
         if (name.equals(module.getName())) {
            return module;
         }
      }

      return null;
   }

   private Setting findSetting(Module module, String name) {
      if (module != null && name != null) {
         for (Setting setting : module.getSettings()) {
            if (name.equals(setting.getName())) {
               return setting;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private Setting findSetting(List<Setting> settings, String name) {
      if (settings != null && name != null) {
         for (Setting setting : settings) {
            if (name.equals(setting.getName())) {
               return setting;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private ConfigManager.BindState captureBind(BindData bind) {
      ConfigManager.BindState state = new ConfigManager.BindState();
      state.mode = bind.getMode() == null ? BindMode.TOGGLE.name() : bind.getMode().name();
      state.keys = new ArrayList<>(bind.getKeys());
      state.active = bind.isActive();
      state.onBindEnable = bind.isOnBindEnable();
      state.showInBinds = bind.isShowInBinds();
      return state;
   }

   private void applyBind(BindData bind, ConfigManager.BindState state) {
      if (state.mode != null) {
         try {
            bind.setMode(BindMode.valueOf(state.mode));
         } catch (IllegalArgumentException ignored) {
            bind.setMode(BindMode.TOGGLE);
         }
      }

      bind.setKeys(state.keys == null ? List.of() : state.keys);
      bind.setActive(state.active);
      bind.setOnBindEnable(state.onBindEnable);
      bind.setShowInBinds(state.showInBinds);
   }

   private ConfigManager.SettingState captureSetting(Setting setting) {
      ConfigManager.SettingState state = new ConfigManager.SettingState();
      state.name = setting.getName();
      state.type = setting.getType().name();
      state.bind = this.captureBind(setting.getBind());
      if (setting instanceof BooleanSetting s) {
         state.boolValue = s.isValue();
         state.boolBindOn = s.getBindOnValue();
         state.boolBindOff = s.getBindOffValue();
         return state;
      } else if (setting instanceof SliderSetting s) {
         state.floatValue = s.getValue();
         state.floatBindOn = s.getBindOnValue();
         state.floatBindOff = s.getBindOffValue();
         return state;
      } else if (setting instanceof SelectSetting s) {
         state.stringValue = s.getValue();
         state.stringBindOn = s.getBindOnValue();
         state.stringBindOff = s.getBindOffValue();
         return state;
      } else if (setting instanceof MultiSelectSetting s) {
         state.listValue = new ArrayList<>(s.getSelectedValues());
         state.listBindOn = new ArrayList<>(s.getBindOnValue());
         state.listBindOff = new ArrayList<>(s.getBindOffValue());
         return state;
      } else if (setting instanceof InputSetting s) {
         state.stringValue = s.getValue();
         state.stringBindOn = s.getBindOnValue();
         state.stringBindOff = s.getBindOffValue();
         return state;
      } else if (setting instanceof ColorSetting s) {
         state.intValue = s.getColor();
         state.intBindOn = s.getBindOnValue();
         state.intBindOff = s.getBindOffValue();
         return state;
      } else {
         return null;
      }
   }

   private void applySetting(Setting setting, ConfigManager.SettingState state) {
      if (state.bind != null) {
         this.applyBind(setting.getBind(), state.bind);
      }

      if (setting instanceof BooleanSetting s) {
         if (state.boolValue != null) {
            s.setValue(state.boolValue);
         }

         if (state.boolBindOn != null) {
            s.setBindOnValue(state.boolBindOn);
         }

         if (state.boolBindOff != null) {
            s.setBindOffValue(state.boolBindOff);
         }
      } else if (setting instanceof SliderSetting s) {
         if (state.floatValue != null) {
            s.setValue(state.floatValue);
         }

         if (state.floatBindOn != null) {
            s.setBindOnValue(state.floatBindOn);
         }

         if (state.floatBindOff != null) {
            s.setBindOffValue(state.floatBindOff);
         }
      } else if (setting instanceof SelectSetting s) {
         if (state.stringValue != null) {
            s.setValue(state.stringValue);
         }

         if (state.stringBindOn != null) {
            s.setBindOnValue(state.stringBindOn);
         }

         if (state.stringBindOff != null) {
            s.setBindOffValue(state.stringBindOff);
         }
      } else if (setting instanceof MultiSelectSetting s) {
         if (state.listValue != null) {
            s.setValue(new LinkedHashSet<>(state.listValue));
         }

         if (state.listBindOn != null) {
            s.setBindOnValue(new LinkedHashSet<>(state.listBindOn));
         }

         if (state.listBindOff != null) {
            s.setBindOffValue(new LinkedHashSet<>(state.listBindOff));
         }
      } else if (setting instanceof InputSetting s) {
         if (state.stringValue != null) {
            s.setValue(state.stringValue);
         }

         if (state.stringBindOn != null) {
            s.setBindOnValue(state.stringBindOn);
         }

         if (state.stringBindOff != null) {
            s.setBindOffValue(state.stringBindOff);
         }
      } else {
         if (setting instanceof ColorSetting s) {
            if (state.intValue != null) {
               s.setColor(state.intValue);
            }

            if (state.intBindOn != null) {
               s.setBindOnValue(state.intBindOn);
            }

            if (state.intBindOff != null) {
               s.setBindOffValue(state.intBindOff);
            }
         }
      }
   }

   private Path configPath(String configName) {
      return this.configDir.resolve(configName + ".json");
   }

   private String sanitizeConfigName(String input) {
      if (input == null) {
         return null;
      }

      String safe = input.trim();
      if (safe.isEmpty()) {
         return null;
      }

      safe = safe.replaceAll("[\\\\/:*?\"<>|]", "_");
      if (safe.length() > 50) {
         safe = safe.substring(0, 50);
      }

      return safe.isEmpty() ? null : safe;
   }

   private boolean equals(String a, String b) {
      return a == null ? b == null : a.equals(b);
   }

   @Generated
   public Path getConfigDir() {
      return this.configDir;
   }

   @Generated
   public String getLoadedConfigName() {
      return this.loadedConfigName;
   }

   private static final class BindState {
      String mode;
      List<Integer> keys;
      boolean active;
      boolean onBindEnable;
      boolean showInBinds;
   }

   public record ConfigEntry(String name, long createdAtMs) {
   }

   private static final class ConfigFile {
      int version;
      long createdAtMs;
      ConfigManager.MenuState menuState;
      Integer themeAccentColor;
      String themePresetName;
      Map<String, Integer> themeColors;
      List<ConfigManager.ModuleState> modules;
      List<ConfigManager.HudElementState> hudElements;
   }

   private static final class HudElementState {
      String id;
      Float x;
      Float y;
      List<ConfigManager.SettingState> settings;
   }

   public static final class MenuState {
      public String selectedCategory;
      public String selectedExtension;
      public String settingsModule;
      public String searchQuery;
      public String selectedConfig;
      public String configInput;
      public String activeTopTab;
      public String configsSelectedConfig;
      public float configsScroll;
      public int guiScalePercent = 100;
      public boolean lightTheme;
      public Map<String, Float> moduleTabScroll = new HashMap<>();
      public Map<String, Boolean> selectExpanded = new HashMap<>();
      public Map<String, Boolean> multiSelectExpanded = new HashMap<>();
   }

   private static final class ModuleState {
      String name;
      int key;
      boolean enabled;
      ConfigManager.BindState bind;
      List<ConfigManager.SettingState> settings;
   }

   private static final class SettingState {
      String name;
      String type;
      ConfigManager.BindState bind;
      Boolean boolValue;
      Boolean boolBindOn;
      Boolean boolBindOff;
      Float floatValue;
      Float floatBindOn;
      Float floatBindOff;
      Integer intValue;
      Integer intBindOn;
      Integer intBindOff;
      String stringValue;
      String stringBindOn;
      String stringBindOff;
      List<String> listValue;
      List<String> listBindOn;
      List<String> listBindOff;
   }
}
