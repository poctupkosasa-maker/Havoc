package havoc.mixin;

import havoc.Havoc;
import havoc.module.impl.misc.ChatHelper;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
   @ModifyVariable(method = "addMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
   private Component havoc$decorateDuplicateMessages(Component message) {
      ChatHelper helper = havoc$getChatHelper();
      return helper == null ? message : helper.decorateAntiSpam(message);
   }

   @ModifyVariable(method = "clearMessages", at = @At("HEAD"), argsOnly = true, ordinal = 0)
   private boolean havoc$preserveRecentChat(boolean clearRecentChat) {
      ChatHelper helper = havoc$getChatHelper();
      return clearRecentChat && (helper == null || !helper.shouldPreserveHistory());
   }

   private static ChatHelper havoc$getChatHelper() {
      Havoc havoc = Havoc.getInstance();
      return havoc != null && havoc.getModuleManager() != null ? havoc.getModuleManager().getModule(ChatHelper.class) : null;
   }
}
