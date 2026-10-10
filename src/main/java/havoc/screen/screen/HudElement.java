package havoc.screen.screen;

import havoc.module.setting.Setting;
import havoc.screen.menu.MenuScreen;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;

public interface HudElement {
   String getId();

   String getDisplayName();

   float getX();

   float getY();

   void setPosition(float var1, float var2);

   float getWidth();

   float getHeight();

   List<Setting> getSettings();

   void render(GuiGraphicsExtractor var1, int var2, int var3, float var4);

   default void setActive(boolean active) {
   }

   default boolean shouldRenderWhenInactive() {
      return false;
   }

   default boolean canInteractInEditor(Minecraft mc) {
      return mc != null && (mc.gui.screen() instanceof MenuScreen || mc.gui.screen() instanceof ChatScreen);
   }

   default boolean allowHorizontalDrag() {
      return true;
   }

   default boolean allowVerticalDrag() {
      return true;
   }

   default boolean handleLeftClick(Minecraft mc, float mouseX, float mouseY, int screenWidth, int screenHeight) {
      return false;
   }
}
