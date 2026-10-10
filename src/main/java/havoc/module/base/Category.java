package havoc.module.base;

import lombok.Generated;

public enum Category {
   COMBAT("Combat"),
   MOVEMENT("Movement"),
   VISUALS("Visuals"),
   PLAYER("Player"),
   MISC("Misc");

   final String name;

   @Generated
   Category(final String name) {
      this.name = name;
   }

   @Generated
   public String getName() {
      return this.name;
   }
}
