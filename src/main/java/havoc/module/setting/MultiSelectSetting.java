package havoc.module.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MultiSelectSetting extends AbstractSetting<Set<String>> {
   private final List<String> options;
   private final Set<String> selected = new LinkedHashSet<>();

   public MultiSelectSetting(String name, List<String> options, Set<String> defaultSelected) {
      this(name, null, options, defaultSelected);
   }

   public MultiSelectSetting(String name, String description, List<String> options, Set<String> defaultSelected) {
      super(name, description, Collections.emptySet());
      if (options != null && !options.isEmpty()) {
         this.options = new ArrayList<>(options);
         if (defaultSelected != null) {
            for (String value : defaultSelected) {
               if (this.options.contains(value)) {
                  this.selected.add(value);
               }
            }
         }

         this.syncValue();
      } else {
         throw new IllegalArgumentException("MultiSelectSetting options must not be empty");
      }
   }

   @Override
   public SettingType getType() {
      return SettingType.MULTI_SELECT;
   }

   public List<String> getOptions() {
      return Collections.unmodifiableList(this.options);
   }

   public Set<String> getSelectedValues() {
      return Collections.unmodifiableSet(this.selected);
   }

   public boolean isSelected(String option) {
      return this.selected.contains(option);
   }

   public void setValue(Set<String> value) {
      this.selected.clear();
      if (value != null) {
         for (String option : value) {
            if (this.options.contains(option)) {
               this.selected.add(option);
            }
         }
      }

      this.syncValue();
   }

   public void toggle(String option) {
      if (this.options.contains(option)) {
         if (!this.selected.remove(option)) {
            this.selected.add(option);
         }

         this.syncValue();
      }
   }

   private void syncValue() {
      super.setValue(Collections.unmodifiableSet(new LinkedHashSet<>(this.selected)));
   }

   protected Set<String> copyValue(Set<String> value) {
      return value == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(value));
   }
}
