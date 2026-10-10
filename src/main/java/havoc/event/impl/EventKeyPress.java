package havoc.event.impl;

import havoc.event.Event;
import lombok.Generated;

public class EventKeyPress extends Event {
   int key;
   int action;

   @Generated
   public int getKey() {
      return this.key;
   }

   @Generated
   public int getAction() {
      return this.action;
   }

   @Generated
   public EventKeyPress(int key, int action) {
      this.key = key;
      this.action = action;
   }
}
