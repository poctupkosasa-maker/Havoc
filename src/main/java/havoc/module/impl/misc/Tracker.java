package havoc.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventAttackEntity;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.utilites.ChatUtil;
import havoc.utilites.color.ClientColors;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@ModuleInfo(name = "Tracker", desc = "", category = Category.MISC)
public final class Tracker extends Module {
   private static final long TRACK_AFTER_HIT_MS = 7000L;
   private static final char SECTION = '§';
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private final Map<UUID, Long> trackedTargets = new HashMap<>();
   private final Map<UUID, int[]> stageByTarget = new HashMap<>();

   @Override
   public void onDisable() {
      this.trackedTargets.clear();
      this.stageByTarget.clear();
      super.onDisable();
   }

   @Subscribe
   public void onAttack(EventAttackEntity event) {
      if (mc != null && mc.player != null && event != null && event.getTarget() instanceof Player target) {
         if (target != mc.player) {
            UUID uuid = target.getUUID();
            this.trackedTargets.put(uuid, System.currentTimeMillis());
            this.stageByTarget.putIfAbsent(uuid, this.captureStages(target));
         }
      }
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc != null && mc.player != null && mc.level != null) {
         long now = System.currentTimeMillis();
         Iterator<Entry<UUID, Long>> iterator = this.trackedTargets.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<UUID, Long> entry = iterator.next();
            UUID targetId = entry.getKey();
            if (now - entry.getValue() > 7000L) {
               iterator.remove();
               this.stageByTarget.remove(targetId);
            } else {
               Player target = mc.level.getPlayerByUUID(targetId);
               if (target != null && target.isAlive() && target != mc.player) {
                  int[] previousStages = this.stageByTarget.computeIfAbsent(targetId, id -> this.captureStages(target));
                  int[] currentStages = this.captureStages(target);

                  for (int i = 0; i < ARMOR_SLOTS.length; i++) {
                     if (currentStages[i] > previousStages[i]) {
                        this.sendArmorWarning(target.getName().getString(), ARMOR_SLOTS[i], currentStages[i]);
                     }

                     previousStages[i] = currentStages[i];
                  }
               } else {
                  iterator.remove();
                  this.stageByTarget.remove(targetId);
               }
            }
         }
      } else {
         this.trackedTargets.clear();
         this.stageByTarget.clear();
      }
   }

   private int[] captureStages(Player player) {
      int[] stages = new int[ARMOR_SLOTS.length];

      for (int i = 0; i < ARMOR_SLOTS.length; i++) {
         stages[i] = this.durabilityStage(player.getItemBySlot(ARMOR_SLOTS[i]));
      }

      return stages;
   }

   private int durabilityStage(ItemStack stack) {
      if (stack != null && !stack.isEmpty() && stack.isDamageableItem()) {
         int max = stack.getMaxDamage();
         if (max <= 0) {
            return -1;
         } else {
            int remaining = Math.max(0, max - stack.getDamageValue());
            float percent = remaining * 100.0F / max;
            if (percent <= 0.0F) {
               return 4;
            } else if (percent <= 25.0F) {
               return 3;
            } else if (percent <= 50.0F) {
               return 2;
            } else {
               return percent <= 75.0F ? 1 : 0;
            }
         }
      } else {
         return -1;
      }
   }

   private void sendArmorWarning(String playerName, EquipmentSlot slot, int stage) {
      int threshold = switch (stage) {
         case 1 -> 75;
         case 2 -> 50;
         case 3 -> 25;
         default -> 0;
      };
      String accent = this.legacyHex(ClientColors.CATEGORY_ACCENT);

      String slotName = switch (slot) {
         case HEAD -> "Шлем";
         case CHEST -> "Нагрудник";
         case LEGS -> "Поножи";
         case FEET -> "Ботинки";
         default -> slot.getName();
      };
      String message = this.colorCode('7')
         + "У игрока "
         + accent
         + playerName
         + this.colorCode('7')
         + " прочность брони: "
         + this.colorCode('7')
         + slotName
         + this.colorCode('7')
         + ": "
         + accent
         + String.format(Locale.ROOT, "%d%%", threshold);
      ChatUtil.sendMessage(message);
   }

   private String legacyHex(int argb) {
      int rgb = argb & 16777215;
      String hex = String.format(Locale.ROOT, "%06X", rgb);
      StringBuilder out = new StringBuilder(14);
      out.append('§').append('x');

      for (int i = 0; i < hex.length(); i++) {
         out.append('§').append(hex.charAt(i));
      }

      return out.toString();
   }

   private String colorCode(char code) {
      return "§" + code;
   }
}
