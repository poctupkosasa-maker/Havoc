package havoc.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.MultiSelectSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(name = "MaceTarget", desc = "Targets enemies for mace attacks", category = Category.COMBAT)
public final class MaceTarget extends Module {
   private final SelectSetting server = this.registerSetting(new SelectSetting("Сервер", "Default", Arrays.asList("Default", "ReallyWorld")));
   private final SelectSetting mode = this.registerSetting(new SelectSetting("Режим", "Silent", Arrays.asList("Silent", "Legit")));
   private final SliderSetting height = this.registerSetting(new SliderSetting("Высота", 30.0F, 20.0F, 60.0F, 1.0F));
   private final MultiSelectSetting targets = this.registerSetting(
      new MultiSelectSetting("Цели", Arrays.asList("Игроки", "Мобы", "Животные"), new LinkedHashSet<>(List.of("Игроки")))
   );
   private final BooleanSetting autoEquipChest = this.registerSetting(new BooleanSetting("Авто-нагрудник", true));
   private final BooleanSetting predictMovement = this.registerSetting(new BooleanSetting("Предугадывание", true));
   private LivingEntity target;
   private boolean equippedElytra;

   @Override
   public void onDisable() {
      if (this.autoEquipChest.isValue() && mc.player != null && mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
         this.swapChest(false);
      }

      this.equippedElytra = false;
      this.target = null;
      super.onDisable();
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.level != null && mc.gameMode != null) {
         this.target = this.findTarget();
         if (this.target != null) {
            if (!mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
               this.equippedElytra = this.swapChest(true);
            }

            Vec3 predicted = new Vec3(this.target.getX(), this.target.getY(), this.target.getZ())
               .add(this.predictMovement.isValue() ? this.target.getDeltaMovement().scale(7.0) : Vec3.ZERO);
            if (mc.player.isFallFlying()) {
               double wantedY = mc.player.getY() < predicted.y + this.height.getValue().floatValue()
                  ? predicted.y + this.height.getValue().floatValue()
                  : predicted.y + 1.4;
               Vec3 destination = new Vec3(predicted.x, wantedY, predicted.z);
               Vec3 direction = destination.subtract(mc.player.getX(), mc.player.getY(), mc.player.getZ()).normalize();
               mc.player.setDeltaMovement(mc.player.getDeltaMovement().scale(0.82).add(direction.scale(0.18)));
            }

            if (mc.player.getMainHandItem().is(Items.MACE) && mc.player.distanceTo(this.target) <= 3.25F && mc.player.getAttackStrengthScale(0.5F) > 0.9F) {
               mc.gameMode.attack(mc.player, this.target);
               mc.player.swing(InteractionHand.MAIN_HAND);
               this.setEnable(false);
            }
         }
      }
   }

   private LivingEntity findTarget() {
      return mc.level
         .getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(128.0), this::validTarget)
         .stream()
         .min(Comparator.comparingDouble(mc.player::distanceToSqr))
         .orElse(null);
   }

   private boolean validTarget(LivingEntity entity) {
      return entity != mc.player && entity.isAlive() && !entity.isSpectator()
         ? entity instanceof Player && this.targets.isSelected("Игроки")
            || entity instanceof Animal && this.targets.isSelected("Животные")
            || entity instanceof Mob && !(entity instanceof Animal) && this.targets.isSelected("Мобы")
         : false;
   }

   private boolean swapChest(boolean toElytra) {
      if (mc.player != null && mc.gameMode != null) {
         int source = -1;

         for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (toElytra && stack.is(Items.ELYTRA)) {
               source = slot;
               break;
            }

            if (!toElytra && stack.getItem().getDescriptionId().contains("chestplate")) {
               source = slot;
               break;
            }
         }

         if (source < 0) {
            return false;
         }

         int sourceSlotId = source < 9 ? source + 36 : source;
         int syncId = mc.player.containerMenu.containerId;
         mc.gameMode.handleContainerInput(syncId, sourceSlotId, 0, ContainerInput.PICKUP, mc.player);
         mc.gameMode.handleContainerInput(syncId, 6, 0, ContainerInput.PICKUP, mc.player);
         mc.gameMode.handleContainerInput(syncId, sourceSlotId, 0, ContainerInput.PICKUP, mc.player);
         return true;
      } else {
         return false;
      }
   }
}
