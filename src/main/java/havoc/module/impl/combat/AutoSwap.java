package havoc.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import havoc.notification.Notifications;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ModuleInfo(name = "AutoSwap", desc = "Luma configurable offhand swap", category = Category.COMBAT)
public final class AutoSwap extends Module {
   private final SelectSetting mode = this.registerSetting(new SelectSetting("Режим", "Legit", Arrays.asList("Instant", "Legit", "Custom")));
   private final BooleanSetting swapBind = this.registerSetting(new BooleanSetting("Кнопка свапа", false).bindOnly());
   private final SelectSetting first = this.registerSetting(new SelectSetting("Первый предмет", "Totem of Undying", items()));
   private final SelectSetting second = this.registerSetting(new SelectSetting("Второй предмет", "Player Head", items()));
   private final BooleanSetting stopMovement = this.registerSetting(new BooleanSetting("Стоп движение", true).visibleWhen(this::custom));
   private final BooleanSetting stopSprint = this.registerSetting(new BooleanSetting("Стоп спринт", true).visibleWhen(this::custom));
   private final BooleanSetting closeInventory = this.registerSetting(new BooleanSetting("Закрыть инвентарь", true).visibleWhen(this::custom));
   private final SliderSetting preStopMin = this.customSlider("До стопа (мин)", 0.0F, 0.0F, 300.0F);
   private final SliderSetting preStopMax = this.customSlider("До стопа (макс)", 50.0F, 0.0F, 300.0F);
   private final SliderSetting waitStopMin = this.customSlider("Ожидание стопа (мин)", 50.0F, 0.0F, 500.0F);
   private final SliderSetting waitStopMax = this.customSlider("Ожидание стопа (макс)", 150.0F, 0.0F, 500.0F);
   private final SliderSetting preSwapMin = this.customSlider("До свапа (мин)", 20.0F, 0.0F, 300.0F);
   private final SliderSetting preSwapMax = this.customSlider("До свапа (макс)", 100.0F, 0.0F, 300.0F);
   private final SliderSetting postSwapMin = this.customSlider("После свапа (мин)", 20.0F, 0.0F, 300.0F);
   private final SliderSetting postSwapMax = this.customSlider("После свапа (макс)", 80.0F, 0.0F, 300.0F);
   private final SliderSetting resumeMin = this.customSlider("Восстановление (мин)", 50.0F, 0.0F, 300.0F);
   private final SliderSetting resumeMax = this.customSlider("Восстановление (макс)", 150.0F, 0.0F, 300.0F);
   private final SliderSetting velocityThreshold = this.registerSetting(
      new SliderSetting("Порог скорости", 0.001F, 1.0E-4F, 0.05F, 1.0E-4F).visibleWhen(this::custom)
   );
   private boolean previousBind;
   private long pendingAt;

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.gameMode != null) {
         boolean pressed = this.swapBind.isValue();
         if (pressed && !this.previousBind) {
            this.pendingAt = System.currentTimeMillis() + this.delayBeforeSwap();
         }

         this.previousBind = pressed;
         if (this.pendingAt != 0L && System.currentTimeMillis() >= this.pendingAt) {
            this.performSwap();
            this.pendingAt = 0L;
            this.swapBind.setValue(false);
            this.previousBind = false;
         }
      }
   }

   private void performSwap() {
      Item target = mc.player.getOffhandItem().is(this.resolve(this.first.getValue()))
         ? this.resolve(this.second.getValue())
         : this.resolve(this.first.getValue());
      int slot = this.find(target);
      if (slot >= 0) {
         Component itemName = this.getItemName(slot, target);
         if (this.stopSprint.isValue() && this.custom()) {
            mc.player.setSprinting(false);
         }

         if (this.stopMovement.isValue() && this.custom()) {
            mc.player.setDeltaMovement(0.0, mc.player.getDeltaMovement().y, 0.0);
         }

         mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, slot, 40, ContainerInput.SWAP, mc.player);
         Notifications.success(Component.literal("Свапнул на ").append(itemName));
         if (this.closeInventory.isValue() && this.custom() && mc.gui.screen() != null) {
            mc.gui.setScreen(null);
         }
      }
   }

   private Component getItemName(int slotId, Item fallback) {
      int inventorySlot = slotId >= 36 && slotId <= 44 ? slotId - 36 : slotId;
      if (inventorySlot >= 0 && inventorySlot < mc.player.getInventory().getContainerSize()) {
         ItemStack stack = mc.player.getInventory().getItem(inventorySlot);
         if (!stack.isEmpty()) {
            return stack.getHoverName().copy();
         }
      }

      return fallback.getName(fallback.getDefaultInstance()).copy();
   }

   private long delayBeforeSwap() {
      if (this.mode.getValue().equals("Instant")) {
         return 0L;
      } else {
         return this.mode.getValue().equals("Legit") ? 75L : Math.round((this.preSwapMin.getValue() + this.preSwapMax.getValue()) * 0.5F);
      }
   }

   private SliderSetting customSlider(String name, float value, float min, float max) {
      return this.registerSetting(new SliderSetting(name, value, min, max, 1.0F, "ms").visibleWhen(this::custom));
   }

   private boolean custom() {
      return this.mode.getValue().equals("Custom");
   }

   private int find(Item item) {
      for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
         if (mc.player.getInventory().getItem(i).is(item)) {
            return i < 9 ? i + 36 : i;
         }
      }

      return -1;
   }

   private Item resolve(String name) {
      return switch (name) {
         case "Player Head" -> Items.PLAYER_HEAD;
         case "Golden Apple" -> Items.GOLDEN_APPLE;
         case "Shield" -> Items.SHIELD;
         default -> Items.TOTEM_OF_UNDYING;
      };
   }

   private static List<String> items() {
      return Arrays.asList("Totem of Undying", "Player Head", "Golden Apple", "Shield");
   }
}
