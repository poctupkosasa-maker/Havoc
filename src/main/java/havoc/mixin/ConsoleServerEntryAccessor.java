package havoc.mixin;

import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public interface ConsoleServerEntryAccessor {
   @Accessor("icon") FaviconTexture havoc$icon();
   @Invoker("refreshStatus") void havoc$refreshStatus();
   @Invoker("uploadServerIcon") boolean havoc$uploadIcon(byte[] bytes);
}
