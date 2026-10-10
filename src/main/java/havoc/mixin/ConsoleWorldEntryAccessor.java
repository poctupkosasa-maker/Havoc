package havoc.mixin;

import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldSelectionList.WorldListEntry.class)
public interface ConsoleWorldEntryAccessor {
   @Accessor("icon") FaviconTexture havoc$icon();
}
