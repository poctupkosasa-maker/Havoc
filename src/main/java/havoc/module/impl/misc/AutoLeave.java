package havoc.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import havoc.Havoc;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.MultiSelectSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

@ModuleInfo(name = "AutoLeave", desc = "Leaves when danger appears", category = Category.MISC)
public final class AutoLeave extends Module {
   private final SelectSetting leaveType = this.registerSetting(new SelectSetting("Тип выхода", "Main Menu", List.of("Hub", "Main Menu")));
   private final MultiSelectSetting triggers = this.registerSetting(
      new MultiSelectSetting("Триггеры", List.of("Players", "Staff"), new LinkedHashSet<>(List.of("Players", "Staff")))
   );
   private final SliderSetting distance = this.registerSetting(
      new SliderSetting("Максимальная дистанция", 10.0F, 5.0F, 40.0F, 1.0F).visibleWhen(() -> this.triggers.isSelected("Players"))
   );

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.level != null && mc.getConnection() != null && this.triggers.isSelected("Players")) {
         for (Player player : mc.level.players()) {
            if (player != mc.player
               && !(mc.player.distanceTo(player) >= this.distance.getValue())
               && !Havoc.getInstance().getCommandManager().isFriend(player.getName().getString())) {
               if (this.leaveType.getValue().equals("Hub")) {
                  mc.getConnection().sendCommand("hub");
               } else {
                  mc.getConnection().getConnection().disconnect(Component.literal("[AutoLeave] " + player.getName().getString()));
               }

               this.setEnable(false);
               return;
            }
         }
      }
   }
}
