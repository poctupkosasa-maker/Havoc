package havoc.module.base;

import havoc.Havoc;
import havoc.module.bind.BindData;
import havoc.module.setting.Setting;
import havoc.notification.Notifications;
import havoc.utilites.ClientSoundPlayer;
import havoc.utilites.IMinecraft;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Generated;
import net.minecraft.client.Minecraft;

public class Module implements IMinecraft {
   private final String name;
   private final String desc;
   private final Category category;
   private final List<Setting> settings = new ArrayList<>();
   private final BindData bind = new BindData();
   private int key;
   private boolean enable;

   public Module() {
      ModuleInfo moduleInfo = this.getClass().getAnnotation(ModuleInfo.class);
      Objects.requireNonNull(moduleInfo, "ModuleInfo не найдена да " + this.getClass().getSimpleName());
      this.name = moduleInfo.name();
      this.desc = moduleInfo.desc();
      this.category = moduleInfo.category();
   }

   public void onEnable() {
      Havoc.getInstance().getEventBus().register(this);
      Notifications.module(this.getName() + " включён!", true);
   }

   public void onDisable() {
      Havoc.getInstance().getEventBus().unregister(this);
      Notifications.module(this.getName() + " выключен!", false);
   }

   public void toggle() {
      this.setEnable(!this.enable);
   }

   public void setEnable(boolean enable) {
      if (this.enable != enable) {
         this.enable = enable;
         if (enable) {
            this.onEnable();
         } else {
            this.onDisable();
         }

         if (Minecraft.getInstance().player != null) {
            ClientSoundPlayer.playModuleToggle(enable);
         }

         Havoc.getInstance().markConfigDirty();
      }
   }

   public void setKey(int key) {
      if (this.key != key) {
         this.key = key;
         Havoc.getInstance().markConfigDirty();
      }
   }

   protected <T extends Setting> T registerSetting(T setting) {
      this.settings.add(setting);
      return setting;
   }

   protected final void clearBindKeys() {
      this.bind.clearKeys();
   }

   public List<Setting> getSettings() {
      return Collections.unmodifiableList(this.settings);
   }

   public boolean hasSettings() {
      return !this.settings.isEmpty();
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public String getDesc() {
      return this.desc;
   }

   @Generated
   public Category getCategory() {
      return this.category;
   }

   @Generated
   public BindData getBind() {
      return this.bind;
   }

   @Generated
   public int getKey() {
      return this.key;
   }

   @Generated
   public boolean isEnable() {
      return this.enable;
   }
}
