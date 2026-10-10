package havoc;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import havoc.command.ClientCommandManager;
import havoc.config.ConfigManager;
import havoc.event.impl.EventKeyPress;
import havoc.module.base.Module;
import havoc.module.base.ModuleManager;
import havoc.module.bind.BindData;
import havoc.module.bind.BindMode;
import havoc.module.impl.visuals.Menu;
import havoc.module.setting.AbstractSetting;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.ColorSetting;
import havoc.module.setting.InputSetting;
import havoc.module.setting.MultiSelectSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.Setting;
import havoc.module.setting.SliderSetting;
import havoc.notification.NotificationManager;
import havoc.notification.Notifications;
import havoc.render.LevelRenderBridge;
import havoc.render.renderers.pipeline.PipelineInitializer;
import havoc.utilites.LumaSoundManager;
import havoc.utilites.color.ClientTheme;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Generated;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.Minecraft;

public class Havoc implements ModInitializer {
   public static final int MOUSE_BIND_CODE_BASE = 1000;
   private static Havoc instance;
   private final EventBus eventBus;
   private final ModuleManager moduleManager;
   private final ConfigManager configManager;
   private final ClientCommandManager commandManager;
   private final NotificationManager notificationManager;
   private final Set<Integer> pressedKeys = new HashSet<>();
   private final IdentityHashMap<BindData, Boolean> comboState = new IdentityHashMap<>();
   private final IdentityHashMap<BindData, Boolean> holdOffRestoreState = new IdentityHashMap<>();
   private final IdentityHashMap<BindData, Float> sliderRestoreState = new IdentityHashMap<>();
   private final IdentityHashMap<BindData, String> selectRestoreState = new IdentityHashMap<>();
   private final IdentityHashMap<BindData, Set<String>> multiRestoreState = new IdentityHashMap<>();
   private final IdentityHashMap<BindData, String> inputRestoreState = new IdentityHashMap<>();
   private final IdentityHashMap<BindData, Integer> colorRestoreState = new IdentityHashMap<>();
   private final Set<Integer> menuScreenPressedKeys = new HashSet<>();
   private boolean menuScreenBindDown;

   public void onInitialize() {
      LumaSoundManager.init();
      PipelineInitializer.init();
      LevelRenderBridge.register();
   }

   public Havoc() {
      instance = this;
      this.eventBus = new EventBus();
      this.notificationManager = new NotificationManager();
      this.moduleManager = new ModuleManager();
      this.configManager = new ConfigManager(this.moduleManager);
      this.commandManager = new ClientCommandManager(this, this.configManager);
      ClientTheme.syncFromSetting();
      this.eventBus.register(this.commandManager);
      this.eventBus.register(this);
   }

   @Subscribe
   public void onPress(EventKeyPress e) {
      Minecraft client = Minecraft.getInstance();
      if (client != null && client.gui.screen() != null) {
         this.processMenuBindWhileScreenOpen(e);
         this.pressedKeys.clear();
         this.comboState.clear();
         this.configManager.flushAutoSave();
      } else {
         this.resetMenuScreenBindState();
         if (e.getAction() == 0) {
            this.pressedKeys.remove(e.getKey());
         } else if (e.getAction() == 1 || e.getAction() == 2) {
            this.pressedKeys.add(e.getKey());
         }

         if (e.getAction() == 1 && this.commandManager.handleKeyPress(e.getKey())) {
            this.configManager.flushAutoSave();
         } else {
            if (e.getAction() == 1) {
               for (Module module : this.moduleManager.getModules()) {
                  if (!module.getBind().hasKeys() && module.getKey() == e.getKey()) {
                     module.toggle();
                  }
               }
            }

            this.processModuleBinds();
            this.processSettingBinds();
            this.configManager.flushAutoSave();
         }
      }
   }

   private void processMenuBindWhileScreenOpen(EventKeyPress e) {
      Module menuModule = this.moduleManager.getModule(Menu.class);
      if (menuModule != null) {
         if (e.getAction() == 0) {
            this.menuScreenPressedKeys.remove(e.getKey());
         } else if (e.getAction() == 1 || e.getAction() == 2) {
            this.menuScreenPressedKeys.add(e.getKey());
         }

         BindData bind = menuModule.getBind();
         if (!bind.hasKeys()) {
            if (e.getAction() == 1 && menuModule.getKey() == e.getKey()) {
               menuModule.toggle();
            }
         } else {
            boolean down = this.menuScreenPressedKeys.containsAll(bind.getKeys());
            if (down && !this.menuScreenBindDown && (e.getAction() == 1 || e.getAction() == 2)) {
               menuModule.toggle();
            }

            this.menuScreenBindDown = down;
         }
      }
   }

   private void resetMenuScreenBindState() {
      if (!this.menuScreenPressedKeys.isEmpty() || this.menuScreenBindDown) {
         this.menuScreenPressedKeys.clear();
         this.menuScreenBindDown = false;
      }
   }

   public void markConfigDirty() {
      if (this.configManager != null) {
         this.configManager.markDirty();
      }
   }

   public static int mouseButtonToBindCode(int mouseButton) {
      return 1000 + mouseButton;
   }

   public static boolean isMouseBindCode(int code) {
      return code >= 1000;
   }

   public static int bindCodeToMouseButton(int code) {
      return code - 1000;
   }

   private void processModuleBinds() {
      for (Module module : this.moduleManager.getModules()) {
         BindData bind = module.getBind();
         if (!bind.hasKeys()) {
            this.comboState.remove(bind);
            this.holdOffRestoreState.remove(bind);
            this.sliderRestoreState.remove(bind);
            this.selectRestoreState.remove(bind);
            this.multiRestoreState.remove(bind);
            this.inputRestoreState.remove(bind);
            this.colorRestoreState.remove(bind);
         } else {
            boolean down = this.pressedKeys.containsAll(bind.getKeys());
            boolean prevDown = this.comboState.getOrDefault(bind, false);
            if (bind.getMode() == BindMode.HOLD) {
               if (down && !prevDown) {
                  boolean wasEnabled = module.isEnable();
                  this.holdOffRestoreState.put(bind, wasEnabled);
                  module.setEnable(!wasEnabled);
                  bind.setActive(true);
               } else if (!down && prevDown) {
                  Boolean restore = this.holdOffRestoreState.get(bind);
                  if (restore != null && module.isEnable() != restore) {
                     module.setEnable(restore);
                  }

                  this.holdOffRestoreState.remove(bind);
                  bind.setActive(false);
               } else {
                  bind.setActive(down);
               }
            } else if (down && !prevDown) {
               module.toggle();
               bind.setActive(module.isEnable());
            }

            this.comboState.put(bind, down);
         }
      }
   }

   private void processSettingBinds() {
      for (Module module : this.moduleManager.getModules()) {
         for (Setting rawSetting : module.getSettings()) {
            if (rawSetting instanceof AbstractSetting<?> setting) {
               BindData bind = setting.getBind();
               if (!bind.hasKeys()) {
                  this.comboState.remove(bind);
                  this.holdOffRestoreState.remove(bind);
               } else {
                  boolean down = this.pressedKeys.containsAll(bind.getKeys());
                  boolean prevDown = this.comboState.getOrDefault(bind, false);
                  if (!(setting instanceof BooleanSetting booleanSetting)) {
                     if (setting instanceof SliderSetting sliderSetting) {
                        if (bind.getMode() == BindMode.HOLD) {
                           if (down && !prevDown) {
                              this.sliderRestoreState.put(bind, sliderSetting.getValue());
                           }

                           if (!down && prevDown) {
                              Float restore = this.sliderRestoreState.get(bind);
                              if (restore != null && sliderSetting.getValue() != restore) {
                                 sliderSetting.setValue(restore);
                              }
                           }

                           bind.setActive(down);
                           if (down) {
                              Float target = sliderSetting.getBindOnValue();
                              if (target != null && sliderSetting.getValue() != target) {
                                 sliderSetting.setValue(target);
                              }
                           }
                        } else if (down && !prevDown) {
                           boolean newActive = !bind.isActive();
                           if (newActive) {
                              this.sliderRestoreState.put(bind, sliderSetting.getValue());
                              bind.setActive(true);
                              Float target = sliderSetting.getBindOnValue();
                              if (target != null && sliderSetting.getValue() != target) {
                                 sliderSetting.setValue(target);
                              }
                           } else {
                              bind.setActive(false);
                              Float restore = this.sliderRestoreState.get(bind);
                              if (restore != null && sliderSetting.getValue() != restore) {
                                 sliderSetting.setValue(restore);
                              }
                           }
                        }

                        this.comboState.put(bind, down);
                     } else if (setting instanceof SelectSetting selectSetting) {
                        if (bind.getMode() == BindMode.HOLD) {
                           if (down && !prevDown) {
                              this.selectRestoreState.put(bind, selectSetting.getValue());
                           }

                           if (!down && prevDown) {
                              String restore = this.selectRestoreState.get(bind);
                              if (restore != null && !restore.equals(selectSetting.getValue())) {
                                 selectSetting.setValue(restore);
                              }
                           }

                           bind.setActive(down);
                           if (down) {
                              String target = selectSetting.getBindOnValue();
                              if (target != null && !target.equals(selectSetting.getValue())) {
                                 selectSetting.setValue(target);
                              }
                           }
                        } else if (down && !prevDown) {
                           boolean newActive = !bind.isActive();
                           if (newActive) {
                              this.selectRestoreState.put(bind, selectSetting.getValue());
                              bind.setActive(true);
                              String target = selectSetting.getBindOnValue();
                              if (target != null && !target.equals(selectSetting.getValue())) {
                                 selectSetting.setValue(target);
                              }
                           } else {
                              bind.setActive(false);
                              String restore = this.selectRestoreState.get(bind);
                              if (restore != null && !restore.equals(selectSetting.getValue())) {
                                 selectSetting.setValue(restore);
                              }
                           }
                        }

                        this.comboState.put(bind, down);
                     } else if (setting instanceof MultiSelectSetting multiSelectSetting) {
                        if (bind.getMode() == BindMode.HOLD) {
                           if (down && !prevDown) {
                              this.multiRestoreState.put(bind, new LinkedHashSet<>(multiSelectSetting.getSelectedValues()));
                           }

                           if (!down && prevDown) {
                              Set<String> restore = this.multiRestoreState.get(bind);
                              if (restore != null && !restore.equals(multiSelectSetting.getSelectedValues())) {
                                 multiSelectSetting.setValue(restore);
                              }
                           }

                           bind.setActive(down);
                           if (down) {
                              Set<String> target = multiSelectSetting.getBindOnValue();
                              if (target != null && !target.equals(multiSelectSetting.getSelectedValues())) {
                                 multiSelectSetting.setValue(target);
                              }
                           }
                        } else if (down && !prevDown) {
                           boolean newActive = !bind.isActive();
                           if (newActive) {
                              this.multiRestoreState.put(bind, new LinkedHashSet<>(multiSelectSetting.getSelectedValues()));
                              bind.setActive(true);
                              Set<String> target = multiSelectSetting.getBindOnValue();
                              if (target != null && !target.equals(multiSelectSetting.getSelectedValues())) {
                                 multiSelectSetting.setValue(target);
                              }
                           } else {
                              bind.setActive(false);
                              Set<String> restore = this.multiRestoreState.get(bind);
                              if (restore != null && !restore.equals(multiSelectSetting.getSelectedValues())) {
                                 multiSelectSetting.setValue(restore);
                              }
                           }
                        }

                        this.comboState.put(bind, down);
                     } else if (setting instanceof InputSetting inputSetting) {
                        if (bind.getMode() == BindMode.HOLD) {
                           if (down && !prevDown) {
                              this.inputRestoreState.put(bind, inputSetting.getValue());
                           }

                           if (!down && prevDown) {
                              String restore = this.inputRestoreState.get(bind);
                              if (restore != null && !restore.equals(inputSetting.getValue())) {
                                 inputSetting.setValue(restore);
                              }
                           }

                           bind.setActive(down);
                           if (down) {
                              String target = inputSetting.getBindOnValue();
                              if (target != null && !target.equals(inputSetting.getValue())) {
                                 inputSetting.setValue(target);
                              }
                           }
                        } else if (down && !prevDown) {
                           boolean newActive = !bind.isActive();
                           if (newActive) {
                              this.inputRestoreState.put(bind, inputSetting.getValue());
                              bind.setActive(true);
                              String target = inputSetting.getBindOnValue();
                              if (target != null && !target.equals(inputSetting.getValue())) {
                                 inputSetting.setValue(target);
                              }
                           } else {
                              bind.setActive(false);
                              String restore = this.inputRestoreState.get(bind);
                              if (restore != null && !restore.equals(inputSetting.getValue())) {
                                 inputSetting.setValue(restore);
                              }
                           }
                        }

                        this.comboState.put(bind, down);
                     } else if (setting instanceof ColorSetting colorSetting) {
                        if (bind.getMode() == BindMode.HOLD) {
                           if (down && !prevDown) {
                              this.colorRestoreState.put(bind, colorSetting.getColor());
                           }

                           if (!down && prevDown) {
                              Integer restore = this.colorRestoreState.get(bind);
                              if (restore != null && restore != colorSetting.getColor()) {
                                 colorSetting.setColor(restore);
                              }
                           }

                           bind.setActive(down);
                           if (down) {
                              Integer target = colorSetting.getBindOnValue();
                              if (target != null && target != colorSetting.getColor()) {
                                 colorSetting.setColor(target);
                              }
                           }
                        } else if (down && !prevDown) {
                           boolean newActive = !bind.isActive();
                           if (newActive) {
                              this.colorRestoreState.put(bind, colorSetting.getColor());
                              bind.setActive(true);
                              Integer target = colorSetting.getBindOnValue();
                              if (target != null && target != colorSetting.getColor()) {
                                 colorSetting.setColor(target);
                              }
                           } else {
                              bind.setActive(false);
                              Integer restore = this.colorRestoreState.get(bind);
                              if (restore != null && restore != colorSetting.getColor()) {
                                 colorSetting.setColor(restore);
                              }
                           }
                        }

                        this.comboState.put(bind, down);
                     } else {
                        if (bind.getMode() == BindMode.HOLD) {
                           if (bind.isActive() != down) {
                              setting.applyBindState(down);
                           }
                        } else if (down && !prevDown) {
                           setting.applyBindState(!bind.isActive());
                        }

                        this.comboState.put(bind, down);
                     }
                  } else if (booleanSetting.isBindOnly()) {
                     if (down && !prevDown) {
                        booleanSetting.setValue(true);
                        bind.setActive(true);
                     } else if (!down && prevDown) {
                        bind.setActive(false);
                     }

                     this.comboState.put(bind, down);
                  } else {
                     if (bind.getMode() == BindMode.HOLD) {
                        if (down && !prevDown) {
                           boolean wasValue = booleanSetting.isValue();
                           this.holdOffRestoreState.put(bind, wasValue);
                           booleanSetting.setValue(!wasValue);
                           bind.setActive(true);
                           this.notifySettingBind(module, booleanSetting);
                        } else if (!down && prevDown) {
                           Boolean restore = this.holdOffRestoreState.get(bind);
                           if (restore != null && booleanSetting.isValue() != restore) {
                              booleanSetting.setValue(restore);
                              this.notifySettingBind(module, booleanSetting);
                           }

                           this.holdOffRestoreState.remove(bind);
                           bind.setActive(false);
                        } else {
                           bind.setActive(down);
                        }
                     } else if (down && !prevDown) {
                        booleanSetting.toggle();
                        bind.setActive(booleanSetting.isValue());
                        this.notifySettingBind(module, booleanSetting);
                     }

                     this.comboState.put(bind, down);
                  }
               }
            }
         }
      }
   }

   private void notifySettingBind(Module module, BooleanSetting setting) {
      if (!setting.isBindOnly()) {
         String msg = module.getName() + "/" + setting.getName() + (setting.isValue() ? " включён!" : " выключен!");
         Notifications.module(msg, setting.isValue());
      }
   }

   @Generated
   public static Havoc getInstance() {
      return instance;
   }

   @Generated
   public EventBus getEventBus() {
      return this.eventBus;
   }

   @Generated
   public ModuleManager getModuleManager() {
      return this.moduleManager;
   }

   @Generated
   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   @Generated
   public ClientCommandManager getCommandManager() {
      return this.commandManager;
   }

   @Generated
   public NotificationManager getNotificationManager() {
      return this.notificationManager;
   }
}
