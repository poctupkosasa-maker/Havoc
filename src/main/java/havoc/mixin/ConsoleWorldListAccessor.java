package havoc.mixin;

import java.util.List;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldSelectionList.class)
public interface ConsoleWorldListAccessor {
   @Invoker("pollLevelsIgnoreErrors") List<LevelSummary> havoc$poll();
   @Accessor("currentlyDisplayedLevels") List<LevelSummary> havoc$displayed();
   @Invoker("handleNewLevels") void havoc$update(List<LevelSummary> levels);
}
