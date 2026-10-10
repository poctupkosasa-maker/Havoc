package havoc.screen.screen;

import havoc.Havoc;
import havoc.module.setting.Setting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractHudElement implements HudElement {
   private final String id;
   private final String displayName;
   private final List<Setting> settings = new ArrayList<>();
   private float x;
   private float y;
   private float width;
   private float height;

   protected AbstractHudElement(String id, String displayName, float defaultX, float defaultY, float defaultWidth, float defaultHeight) {
      this.id = id;
      this.displayName = displayName;
      this.x = defaultX;
      this.y = defaultY;
      this.width = defaultWidth;
      this.height = defaultHeight;
   }

   protected final <T extends Setting> T addSetting(T setting) {
      this.settings.add(setting);
      return setting;
   }

   @Override
   public String getId() {
      return this.id;
   }

   @Override
   public String getDisplayName() {
      return this.displayName;
   }

   @Override
   public float getX() {
      return this.x;
   }

   @Override
   public float getY() {
      return this.y;
   }

   @Override
   public void setPosition(float x, float y) {
      if (Float.isFinite(x) && Float.isFinite(y)) {
         if (!(Math.abs(this.x - x) <= 0.001F) || !(Math.abs(this.y - y) <= 0.001F)) {
            this.x = x;
            this.y = y;
            Havoc.getInstance().markConfigDirty();
         }
      }
   }

   protected final void setBounds(float width, float height) {
      this.width = Math.max(8.0F, width);
      this.height = Math.max(8.0F, height);
   }

   @Override
   public float getWidth() {
      return this.width;
   }

   @Override
   public float getHeight() {
      return this.height;
   }

   @Override
   public List<Setting> getSettings() {
      return Collections.unmodifiableList(this.settings);
   }
}
