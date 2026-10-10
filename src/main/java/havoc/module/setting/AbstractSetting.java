package havoc.module.setting;

import havoc.Havoc;
import havoc.module.bind.BindData;
import java.util.function.BooleanSupplier;
import lombok.Generated;

public abstract class AbstractSetting<T> implements Setting {
   private final String name;
   private final String description;
   private T value;
   private final BindData bind = new BindData();
   private T bindOnValue;
   private T bindOffValue;
   private BooleanSupplier visibleCondition = () -> true;

   protected AbstractSetting(String name, T defaultValue) {
      this(name, null, defaultValue);
   }

   protected AbstractSetting(String name, String description, T defaultValue) {
      this.name = name;
      this.description = description;
      this.value = defaultValue;
      this.bindOnValue = this.copyValue(defaultValue);
      this.bindOffValue = this.copyValue(defaultValue);
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public String getDescription() {
      return this.description;
   }

   protected T copyValue(T value) {
      return value;
   }

   public T getBindOnValue() {
      return this.bindOnValue;
   }

   public T getBindOffValue() {
      return this.bindOffValue;
   }

   public void setBindOnValue(T bindOnValue) {
      this.bindOnValue = this.copyValue(bindOnValue);
      Havoc.getInstance().markConfigDirty();
   }

   public void setBindOffValue(T bindOffValue) {
      this.bindOffValue = this.copyValue(bindOffValue);
      Havoc.getInstance().markConfigDirty();
   }

   public void setValue(T value) {
      this.value = this.copyValue(value);
      Havoc.getInstance().markConfigDirty();
   }

   public void setBindValues(T bindOnValue, T bindOffValue) {
      this.setBindOnValue(bindOnValue);
      this.setBindOffValue(bindOffValue);
   }

   public void captureBindOnFromCurrent() {
      this.setBindOnValue(this.getValue());
   }

   public void captureBindOffFromCurrent() {
      this.setBindOffValue(this.getValue());
   }

   public void applyBindState(boolean active) {
      this.bind.setActive(active);
      T targetValue = active ? this.bindOnValue : this.bindOffValue;
      if (targetValue != null) {
         this.setValue(this.copyValue(targetValue));
      }
   }

   public <S extends AbstractSetting<T>> S visibleWhen(BooleanSupplier condition) {
      this.visibleCondition = condition == null ? () -> true : condition;
      return (S)this;
   }

   @Override
   public boolean isVisible() {
      try {
         return this.visibleCondition == null || this.visibleCondition.getAsBoolean();
      } catch (Exception ignored) {
         return true;
      }
   }

   @Generated
   public T getValue() {
      return this.value;
   }

   @Generated
   @Override
   public BindData getBind() {
      return this.bind;
   }
}
