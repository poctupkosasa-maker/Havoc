package havoc.module.setting;

public final class BooleanSetting extends AbstractSetting<Boolean> {
   private boolean bindOnly;

   public BooleanSetting(String name, boolean defaultValue) {
      super(name, defaultValue);
   }

   public BooleanSetting(String name, String description, boolean defaultValue) {
      super(name, description, defaultValue);
   }

   @Override
   public SettingType getType() {
      return SettingType.BOOLEAN;
   }

   public boolean isValue() {
      return this.getValue();
   }

   public void setValue(boolean value) {
      super.setValue(value);
   }

   public void toggle() {
      this.setValue(!this.isValue());
   }

   public BooleanSetting bindOnly() {
      this.bindOnly = true;
      this.setBindValues(true, false);
      return this;
   }

   public boolean isBindOnly() {
      return this.bindOnly;
   }
}
