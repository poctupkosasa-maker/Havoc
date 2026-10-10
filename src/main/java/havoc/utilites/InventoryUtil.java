package havoc.utilites;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class InventoryUtil {
   private static final Minecraft mc = Minecraft.getInstance();
   private static int pendingRestoreSlot = -1;
   private static int pendingInventorySwapSlotId = -1;
   private static int pendingInventorySwapSelected = -1;
   private static int restoreDelay;
   private static ItemStack silentRenderStack = ItemStack.EMPTY;
   private static int silentRenderTicks;

   private InventoryUtil() {
   }

   public static int findHotbar(Item item) {
      if (mc.player != null && item != null) {
         for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (stack.is(item)) {
               return slot;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public static int findInventory(Item item) {
      if (mc.player != null && item != null) {
         for (int slot = 0; slot < 36; slot++) {
            if (mc.player.getInventory().getItem(slot).is(item)) {
               return slot;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public static boolean useFromInventory(Item item, float pitch) {
      if (mc.player != null && mc.gameMode != null) {
         int source = findInventory(item);
         return silentUseFromInventorySlot(source, mc.player.getYRot(), pitch);
      } else {
         return false;
      }
   }

   public static boolean useItemSimple(Item item) {
      return inventorySwapClick(item);
   }

   public static boolean useInventoryItemSimple(int source) {
      if (mc.player != null && source >= 0 && source < mc.player.getInventory().getContainerSize()) {
         int selected = selectedSlot();
         if (source < 9 && source == selected) {
            sendUseItemPacket();
            return true;
         } else if (source < 9) {
            swapAndUseHotbar(source, selected);
            return true;
         } else {
            int hotbarSlot = selected % 8 + 1;
            int sourceSlotId = source < 9 ? source + 36 : source;
            mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, sourceSlotId, hotbarSlot, ContainerInput.SWAP, mc.player);
            swapAndUseHotbar(hotbarSlot, selected);
            mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, sourceSlotId, hotbarSlot, ContainerInput.SWAP, mc.player);
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean inventorySwapClick(Item item) {
      if (mc.player != null && mc.gameMode != null && mc.getConnection() != null && item != null) {
         int slot = findInventory(item);
         return slot == -1 ? false : useInventoryItemSimple(slot);
      } else {
         return false;
      }
   }

   public static boolean useFromInventorySlot(int source, float yaw, float pitch) {
      return silentUseFromInventorySlot(source, yaw, pitch);
   }

   public static boolean normalUseFromInventorySlot(int source, float yaw, float pitch) {
      if (mc.player != null
         && mc.gameMode != null
         && source >= 0
         && source < mc.player.getInventory().getContainerSize()
         && !mc.player.getInventory().getItem(source).isEmpty()) {
         int selected = selectedSlot();
         float oldYaw = mc.player.getYRot();
         float oldPitch = mc.player.getXRot();
         boolean inventorySwap = source >= 9;
         int sourceSlotId = source < 9 ? source + 36 : source;
         if (inventorySwap) {
            mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, sourceSlotId, selected, ContainerInput.SWAP, mc.player);
         } else {
            switchToHotbar(source);
         }

         if (mc.getConnection() != null) {
            mc.getConnection().send(new Rot(yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));
         }

         mc.player.setYRot(yaw);
         mc.player.setXRot(pitch);
         mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
         mc.player.setYRot(oldYaw);
         mc.player.setXRot(oldPitch);
         if (mc.getConnection() != null) {
            mc.getConnection().send(new Rot(oldYaw, oldPitch, mc.player.onGround(), mc.player.horizontalCollision));
         }

         if (inventorySwap) {
            scheduleInventorySwapRestore(sourceSlotId, selected, 2);
         } else {
            scheduleSelectedSlotRestore(selected, 2);
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean silentUseFromInventorySlot(int source, float yaw, float pitch) {
      if (mc.player != null
         && mc.gameMode != null
         && mc.getConnection() != null
         && source >= 0
         && source < mc.player.getInventory().getContainerSize()
         && !mc.player.getInventory().getItem(source).isEmpty()) {
         int selected = selectedSlot();
         if (selected < 0) {
            return false;
         }

         float oldYaw = mc.player.getYRot();
         float oldPitch = mc.player.getXRot();
         ItemStack oldRenderStack = mc.player.getInventory().getItem(selected).copy();
         boolean inventorySwap = source >= 9;
         int sourceSlotId = source < 9 ? source + 36 : source;
         suppressMainHandRender(oldRenderStack, 12);
         if (inventorySwap) {
            sendSilentSwapPacket(sourceSlotId, selected);
         } else if (source != selected) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(source));
         }

         mc.getConnection().send(new Rot(yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));
         mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, yaw, pitch));
         mc.getConnection().send(new Rot(oldYaw, oldPitch, mc.player.onGround(), mc.player.horizontalCollision));
         if (inventorySwap) {
            sendSilentSwapPacket(sourceSlotId, selected);
         } else if (source != selected) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(selected));
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean useHotbarItemSimple(int slot) {
      if (mc.player != null && mc.gameMode != null && slot >= 0 && slot <= 8 && !mc.player.getInventory().getItem(slot).isEmpty()) {
         int selected = selectedSlot();
         if (selected < 0) {
            return false;
         }

         switchToHotbar(slot);
         mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
         scheduleSelectedSlotRestore(selected, 3);
         return true;
      } else {
         return false;
      }
   }

   private static void swapAndUseHotbar(int targetSlot, int selectedSlot) {
      mc.getConnection().send(new ServerboundSetCarriedItemPacket(targetSlot));
      sendUseItemPacket();
      mc.getConnection().send(new ServerboundSetCarriedItemPacket(selectedSlot));
   }

   private static void sendUseItemPacket() {
      if (mc.player != null && mc.getConnection() != null) {
         mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), mc.player.getXRot()));
      }
   }

   public static ItemStack getSilentMainHandRenderStack(ItemStack fallback) {
      return silentRenderTicks > 0 && !silentRenderStack.isEmpty() ? silentRenderStack : fallback;
   }

   private static void suppressMainHandRender(ItemStack stack, int ticks) {
      silentRenderStack = stack == null ? ItemStack.EMPTY : stack.copy();
      silentRenderTicks = Math.max(1, ticks);
   }

   private static void sendSilentSwapPacket(int slotId, int hotbarSlot) {
      if (mc.player != null && mc.getConnection() != null) {
         mc.getConnection()
            .send(
               new ServerboundContainerClickPacket(
                  mc.player.containerMenu.containerId,
                  mc.player.containerMenu.getStateId(),
                  (short)slotId,
                  (byte)hotbarSlot,
                  ContainerInput.SWAP,
                  new Int2ObjectOpenHashMap(),
                  HashedStack.EMPTY
               )
            );
      }
   }

   public static void scheduleSelectedSlotRestore(int slot, int delayTicks) {
      if (slot >= 0 && slot <= 8) {
         pendingRestoreSlot = slot;
         pendingInventorySwapSlotId = -1;
         pendingInventorySwapSelected = -1;
         restoreDelay = Math.max(1, delayTicks);
      }
   }

   private static void scheduleInventorySwapRestore(int sourceSlotId, int selectedSlot, int delayTicks) {
      if (sourceSlotId >= 0 && selectedSlot >= 0 && selectedSlot <= 8) {
         pendingRestoreSlot = -1;
         pendingInventorySwapSlotId = sourceSlotId;
         pendingInventorySwapSelected = selectedSlot;
         restoreDelay = Math.max(1, delayTicks);
      }
   }

   public static void tickRestore() {
      if (silentRenderTicks > 0) {
         silentRenderTicks--;
         if (silentRenderTicks <= 0) {
            silentRenderStack = ItemStack.EMPTY;
         }
      }

      if (restoreDelay > 0) {
         restoreDelay--;
      } else if (pendingRestoreSlot >= 0) {
         int slot = pendingRestoreSlot;
         pendingRestoreSlot = -1;
         switchToHotbar(slot);
      } else {
         if (pendingInventorySwapSlotId >= 0 && mc.player != null && mc.gameMode != null) {
            int sourceSlotId = pendingInventorySwapSlotId;
            int selected = pendingInventorySwapSelected;
            pendingInventorySwapSlotId = -1;
            pendingInventorySwapSelected = -1;
            mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, sourceSlotId, selected, ContainerInput.SWAP, mc.player);
         }
      }
   }

   public static boolean switchToHotbar(int slot) {
      if (mc.player != null && slot >= 0 && slot <= 8) {
         mc.player.getInventory().setSelectedSlot(slot);
         if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
         }

         return true;
      } else {
         return false;
      }
   }

   public static int selectedSlot() {
      return mc.player == null ? -1 : mc.player.getInventory().getSelectedSlot();
   }
}
