package havoc.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SelectWorldScreen.class)
public interface ConsoleWorldScreenAccessor {
   @Accessor("list") WorldSelectionList havoc$list();
   @Accessor("lastScreen") Screen havoc$parent();
}
