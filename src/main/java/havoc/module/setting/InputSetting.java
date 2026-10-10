package havoc.module.setting;

public final class InputSetting extends AbstractSetting<String> {
   private final int maxLength;

   public InputSetting(String name, String defaultValue, int maxLength) {
      super(name, defaultValue == null ? "" : defaultValue);
      this.maxLength = Math.max(1, maxLength);
      this.setValue(defaultValue == null ? "" : defaultValue);
   }

   @Override
   public SettingType getType() {
      return SettingType.INPUT;
   }

   public int getMaxLength() {
      return this.maxLength;
   }

   public String getValue() {
      return (String)super.getValue();
   }

   public void setValue(String value) {
      String safe = value == null ? "" : value;
      if (safe.length() > this.maxLength) {
         safe = safe.substring(0, this.maxLength);
      }

      super.setValue(safe);
   }
}
