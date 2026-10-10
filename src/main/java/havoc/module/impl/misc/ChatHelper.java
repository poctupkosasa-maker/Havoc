package havoc.module.impl.misc;

import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.utilites.color.ClientColors;
import net.minecraft.network.chat.Component;

@ModuleInfo(name = "ChatHelper", desc = "Chat history and anti-spam", category = Category.MISC)
public final class ChatHelper extends Module {
   private final BooleanSetting chatHistory = this.registerSetting(new BooleanSetting("История чата", true));
   private final BooleanSetting antiSpam = this.registerSetting(new BooleanSetting("Анти Спам", true));
   private String lastMessage = "";
   private long lastMessageAt;
   private int duplicateCount;

   public boolean shouldPreserveHistory() {
      return this.isEnable() && this.chatHistory.isValue();
   }

   public Component decorateAntiSpam(Component message) {
      if (this.isEnable() && this.antiSpam.isValue() && message != null && !message.getString().isBlank()) {
         String raw = message.getString();
         long now = System.currentTimeMillis();
         boolean duplicate = raw.equals(this.lastMessage) && now - this.lastMessageAt <= 10000L;
         this.duplicateCount = duplicate ? this.duplicateCount + 1 : 1;
         this.lastMessage = raw;
         this.lastMessageAt = now;
         return (Component)(!duplicate
            ? message
            : message.copy().append(Component.literal(" (x" + this.duplicateCount + ")").withColor(ClientColors.CATEGORY_ACCENT & 16777215)));
      } else {
         return message;
      }
   }

   @Override
   public void onDisable() {
      this.lastMessage = "";
      this.lastMessageAt = 0L;
      this.duplicateCount = 0;
      super.onDisable();
   }
}
