package havoc.mixin;

import havoc.screen.title.HavocTitleScreen;
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
      return screen instanceof TitleScreen && !(screen instanceof HavocTitleScreen) ? new HavocTitleScreen() : screen;
   }
}
