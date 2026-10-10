package havoc.mixin;

import havoc.screen.title.HavocTitleScreen;
import havoc.screen.library.ConsoleWorldScreen;
import havoc.screen.library.ConsoleServerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
public class HavocTitleScreenSwapMixin {
   @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
   private Screen havoc$swapTitleScreen(Screen screen) {
      if (screen != null && screen.getClass() == SelectWorldScreen.class) {
         return new ConsoleWorldScreen(((ConsoleWorldScreenAccessor)screen).havoc$parent());
      }
      if (screen != null && screen.getClass() == JoinMultiplayerScreen.class) {
         return new ConsoleServerScreen(((ConsoleServerScreenAccessor)screen).havoc$parent());
      }
      return screen instanceof TitleScreen && !(screen instanceof HavocTitleScreen) ? new HavocTitleScreen() : screen;
   }
}
