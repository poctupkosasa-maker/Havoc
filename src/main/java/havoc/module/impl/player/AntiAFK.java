package havoc.module.impl.player;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.mixin.MinecraftClientInvoker;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.InputSetting;
import havoc.module.setting.MultiSelectSetting;
import havoc.module.setting.SliderSetting;
import java.util.Arrays;
import java.util.LinkedHashSet;

@ModuleInfo(name = "AntiAFK", desc = "Performs periodic player actions", category = Category.PLAYER)
public final class AntiAFK extends Module {
   private final SliderSetting delay = this.registerSetting(new SliderSetting("Delay", 30.0F, 1.0F, 300.0F, 1.0F, "s"));
   private final MultiSelectSetting actions = this.registerSetting(
      new MultiSelectSetting("Actions", Arrays.asList("Walk", "Jump", "Swing", "Chat"), new LinkedHashSet<>(Arrays.asList("Walk", "Swing")))
   );
   private final InputSetting message = this.registerSetting(new InputSetting("Message", "/afk", 128).visibleWhen(() -> this.actions.isSelected("Chat")));
   private long lastAction;

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.gui.screen() == null) {
         if (!((float)(System.currentTimeMillis() - this.lastAction) < this.delay.getValue() * 1000.0F)) {
            if (this.actions.isSelected("Jump") && mc.player.onGround()) {
               mc.player.jumpFromGround();
            }

            if (this.actions.isSelected("Swing")) {
               ((MinecraftClientInvoker)mc).havoc$invokeDoItemUse();
            }

            if (this.actions.isSelected("Chat") && !this.message.getValue().isBlank()) {
               mc.player.connection.sendChat(this.message.getValue());
            }

            if (this.actions.isSelected("Walk")) {
               mc.options.keyUp.setDown(true);
               mc.execute(() -> mc.options.keyUp.setDown(false));
            }

            this.lastAction = System.currentTimeMillis();
         }
      }
   }

   @Override
   public void onDisable() {
      mc.options.keyUp.setDown(false);
      super.onDisable();
   }
}
