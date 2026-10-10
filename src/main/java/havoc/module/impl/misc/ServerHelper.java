package havoc.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.SelectSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@ModuleInfo(name = "ServerHelper", desc = "Luma server item assistant", category = Category.MISC)
public final class ServerHelper extends Module {
   private final SelectSetting server = this.registerSetting(new SelectSetting("Тип сервера", "FunTime", List.of("ReallyWorld", "HolyWorld", "FunTime")));
   private final SelectSetting swapMode = this.registerSetting(new SelectSetting("Режим свапа", "Legit", List.of("Instant", "Legit")));
   private final List<ServerHelper.ItemAction> actions = new ArrayList<>();
   private int cooldown;

   public ServerHelper() {
      this.add("Анти полет", Items.FIREWORK_STAR, "ReallyWorld");
      this.add("Свиток опыта", Items.FLOWER_BANNER_PATTERN, "ReallyWorld");
      this.add("Взрывная трапка", Items.PRISMARINE_SHARD, "HolyWorld");
      this.add("Обычная трапка", Items.POPPED_CHORUS_FRUIT, "HolyWorld");
      this.add("Стан", Items.NETHER_STAR, "HolyWorld");
      this.add("Взрывная штучка", Items.FIRE_CHARGE, "HolyWorld");
      this.add("Снежок заморозка", Items.SNOWBALL, null);
      this.add("Божья аура", Items.PHANTOM_MEMBRANE, "FunTime");
      this.add("Трапка", Items.NETHERITE_SCRAP, "FunTime");
      this.add("Пласт", Items.DRIED_KELP, "FunTime");
      this.add("Явная пыль", Items.SUGAR, "FunTime");
      this.add("Огненный смерч", Items.FIRE_CHARGE, "FunTime");
      this.add("Дезориентация", Items.ENDER_EYE, "FunTime");
      this.add("Светильник Джека", Items.JACK_O_LANTERN, "HolyWorld");
      this.add("Пузырь опыта", Items.EXPERIENCE_BOTTLE, "HolyWorld");
      this.add("Рюкзак 1 уровня", (Item)Items.DYED_SHULKER_BOX.pick(DyeColor.PINK), "HolyWorld");
      this.add("Рюкзак 2 уровня", (Item)Items.DYED_SHULKER_BOX.pick(DyeColor.BLUE), "HolyWorld");
      this.add("Рюкзак 3 уровня", (Item)Items.DYED_SHULKER_BOX.pick(DyeColor.RED), "HolyWorld");
      this.add("Рюкзак 4 уровня", (Item)Items.DYED_SHULKER_BOX.pick(DyeColor.PINK), "HolyWorld");
      this.add("Хлопушка", Items.SPLASH_POTION, "FunTime");
      this.add("Святая вода", Items.SPLASH_POTION, "FunTime");
      this.add("Зелье Гнева", Items.SPLASH_POTION, "FunTime");
      this.add("Зелье Палладина", Items.SPLASH_POTION, "FunTime");
      this.add("Зелье Ассасина", Items.SPLASH_POTION, "FunTime");
      this.add("Зелье Радиации", Items.SPLASH_POTION, "FunTime");
      this.add("Снотворное", Items.SPLASH_POTION, "FunTime");
   }

   private void add(String name, Item item, String requiredServer) {
      BooleanSetting trigger = this.registerSetting(
         new BooleanSetting(name, false).visibleWhen(() -> requiredServer == null || this.server.getValue().equals(requiredServer))
      );
      this.actions.add(new ServerHelper.ItemAction(name, item, trigger));
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (this.cooldown > 0) {
         this.cooldown--;
      }

      if (mc.player != null && mc.gameMode != null && this.cooldown <= 0) {
         for (ServerHelper.ItemAction action : this.actions) {
            if (action.trigger.isValue()) {
               action.trigger.setValue(false);
               this.use(action);
               return;
            }
         }
      }
   }

   private void use(ServerHelper.ItemAction action) {
      int slot = this.findHotbar(action);
      if (slot >= 0) {
         int old = mc.player.getInventory().getSelectedSlot();
         mc.player.getInventory().setSelectedSlot(slot);
         mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
         mc.player.swing(InteractionHand.MAIN_HAND);
         mc.player.getInventory().setSelectedSlot(old);
         this.cooldown = this.swapMode.getValue().equals("Legit") ? 5 : 1;
      }
   }

   private int findHotbar(ServerHelper.ItemAction action) {
      String expected = action.name.toLowerCase(Locale.ROOT);

      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = mc.player.getInventory().getItem(slot);
         if (stack.is(action.item)) {
            String actual = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            if (action.item != Items.SPLASH_POTION || actual.contains(expected)) {
               return slot;
            }
         }
      }

      return -1;
   }

   private record ItemAction(String name, Item item, BooleanSetting trigger) {
   }
}
