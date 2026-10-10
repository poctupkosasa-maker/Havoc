package havoc.module.setting;

import havoc.module.bind.BindData;

public interface Setting {
   String getName();

   default String getDescription() {
      return null;
   }

   SettingType getType();

   BindData getBind();

   default boolean isVisible() {
      return true;
   }
}
