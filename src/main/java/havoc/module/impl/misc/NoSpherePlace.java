package havoc.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventPacket;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import java.util.Locale;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

@ModuleInfo(name = "NoSpherePlace", desc = "Prevents placing spheres and player heads", category = Category.MISC)
public final class NoSpherePlace extends Module {
   private final BooleanSetting cancelSphere = this.registerSetting(new BooleanSetting("Блокировать сферу", true));
   private final BooleanSetting cancelHeads = this.registerSetting(new BooleanSetting("Блокировать головы", true));

   @Subscribe
   public void onPacket(EventPacket event) {
      if (event.isSend() && mc.player != null) {
         InteractionHand hand = event.getPacket() instanceof ServerboundUseItemOnPacket packet
            ? packet.getHand()
            : (event.getPacket() instanceof ServerboundUseItemPacket packet ? packet.getHand() : null);
         if (hand != null && this.shouldCancel(hand == InteractionHand.OFF_HAND ? mc.player.getOffhandItem() : mc.player.getMainHandItem())) {
            event.cancel();
         }
      }
   }

   public boolean shouldCancel(ItemStack stack) {
      if (!stack.is(Items.PLAYER_HEAD)) {
         return false;
      }

      if (this.cancelHeads.isValue()) {
         return true;
      }

      if (!this.cancelSphere.isValue()) {
         return false;
      }

      StringBuilder text = new StringBuilder(stack.getHoverName().getString());
      Component customName = (Component)stack.get(DataComponents.CUSTOM_NAME);
      if (customName != null) {
         text.append(' ').append(customName.getString());
      }

      ItemLore lore = (ItemLore)stack.get(DataComponents.LORE);
      if (lore != null) {
         for (Component line : lore.lines()) {
            text.append(' ').append(line.getString());
         }
      }

      CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      if (data != null) {
         text.append(' ').append(data.copyTag());
      }

      String lower = text.toString().toLowerCase(Locale.ROOT);
      return lower.contains("sphere") || lower.contains("sfera") || lower.contains("сфер") || lower.contains("шар");
   }
}
