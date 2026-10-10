package havoc.module.bind;

import havoc.Havoc;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Generated;

public final class BindData {
   private BindMode mode = BindMode.TOGGLE;
   private final LinkedHashSet<Integer> keys = new LinkedHashSet<>();
   private boolean active;
   private boolean onBindEnable = true;
   private boolean showInBinds = true;

   public void setMode(BindMode mode) {
      this.mode = mode == null ? BindMode.TOGGLE : mode;
      Havoc.getInstance().markConfigDirty();
   }

   public Set<Integer> getKeys() {
      return Collections.unmodifiableSet(this.keys);
   }

   public void setKeys(Collection<Integer> keys) {
      this.keys.clear();
      if (keys != null) {
         this.keys.addAll(keys);
      }

      Havoc.getInstance().markConfigDirty();
   }

   public void clearKeys() {
      this.keys.clear();
      Havoc.getInstance().markConfigDirty();
   }

   public boolean hasKeys() {
      return !this.keys.isEmpty();
   }

   public void setActive(boolean active) {
      this.active = active;
      Havoc.getInstance().markConfigDirty();
   }

   public void setOnBindEnable(boolean onBindEnable) {
      this.onBindEnable = onBindEnable;
      Havoc.getInstance().markConfigDirty();
   }

   public void setShowInBinds(boolean showInBinds) {
      this.showInBinds = showInBinds;
      Havoc.getInstance().markConfigDirty();
   }

   @Generated
   public BindMode getMode() {
      return this.mode;
   }

   @Generated
   public boolean isActive() {
      return this.active;
   }

   @Generated
   public boolean isOnBindEnable() {
      return this.onBindEnable;
   }

   @Generated
   public boolean isShowInBinds() {
      return this.showInBinds;
   }
}
