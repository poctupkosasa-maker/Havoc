package havoc.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import havoc.Havoc;
import havoc.event.impl.EventScreenRender;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.MultiSelectSetting;
import havoc.module.setting.SliderSetting;
import havoc.render.RenderUtil;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import havoc.utilites.rotation.SilentAimManager;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(name = "BowAimbot", desc = "Silent bow and trident aim", category = Category.COMBAT)
public final class BowAimbot extends Module {
   private final SliderSetting range = this.registerSetting(new SliderSetting("Range", 45.0F, 5.0F, 80.0F, 1.0F));
   private final SliderSetting fov = this.registerSetting(new SliderSetting("FOV", 90.0F, 5.0F, 180.0F, 1.0F, "°"));
   private final BooleanSetting drawFov = this.registerSetting(new BooleanSetting("Отображать FOV", true));
   private final MultiSelectSetting targets = this.registerSetting(
      new MultiSelectSetting(
         "Цели",
         Arrays.asList("Игроки", "Голые", "Мобы", "Животные", "Невидимые", "Друзья"),
         new LinkedHashSet<>(Arrays.asList("Игроки", "Голые", "Мобы", "Животные"))
      )
   );
   private LivingEntity target;
   private boolean aiming;

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.level != null && this.isCharging()) {
         this.target = mc.level
            .getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(this.range.getValue().floatValue()), this::validTarget)
            .stream()
            .filter(this::validTarget)
            .min(Comparator.comparingDouble(this::angularDistance).thenComparingDouble(mc.player::distanceToSqr))
            .orElse(null);
         if (this.target == null) {
            this.clearAim();
         } else {
            ItemStack stack = this.weaponStack();
            boolean bow = stack.getItem() instanceof BowItem;
            boolean crossbow = stack.getItem() instanceof CrossbowItem;
            float speed = bow ? this.bowSpeed(stack) : (crossbow ? 3.15F : 2.5F);
            float gravity = 0.05F;
            if (!(speed < 0.2F)) {
               Vec3 eye = mc.player.getEyePosition();
               Vec3 center = this.target.position().add(0.0, this.target.getBbHeight() * 0.62, 0.0);
               Vec3 relativeVelocity = this.target.getDeltaMovement().subtract(mc.player.getDeltaMovement().multiply(1.0, 0.0, 1.0));
               double flightTicks = Math.max(1.0, eye.distanceTo(center) / speed);
               Vec3 predicted = center;
               float pitch = 0.0F;

               for (int i = 0; i < 5; i++) {
                  predicted = center.add(relativeVelocity.scale(flightTicks));
                  Vec3 step = predicted.subtract(eye);
                  double horizontal = Math.hypot(step.x, step.z);
                  pitch = this.ballisticPitch(horizontal, step.y, speed, gravity);
                  double horizontalSpeed = Math.max(0.1, speed * Math.cos(Math.toRadians(-pitch)));
                  flightTicks = Math.max(1.0, horizontal / horizontalSpeed);
               }

               Vec3 delta = predicted.subtract(eye);
               float yaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
               SilentAimManager.applyRaw(Mth.wrapDegrees(yaw - mc.player.getYRot()), Mth.clamp(pitch, -90.0F, 90.0F) - mc.player.getXRot());
               this.aiming = true;
            }
         }
      } else {
         this.clearAim();
      }
   }

   private boolean validTarget(LivingEntity entity) {
      if (entity != mc.player && entity.isAlive() && !entity.isSpectator() && !(entity instanceof ArmorStand)) {
         if (mc.player.distanceToSqr(entity) > this.range.getValue() * this.range.getValue()) {
            return false;
         }

         if (entity.isInvisible() && !this.targets.isSelected("Невидимые")) {
            return false;
         }

         if (entity instanceof Player player) {
            boolean friend = Havoc.getInstance().getCommandManager().isFriend(player.getName().getString());
            if (friend && !this.targets.isSelected("Друзья")) {
               return false;
            }

            boolean naked = player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
               && player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
               && player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
               && player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
            if (naked ? !this.targets.isSelected("Голые") : !this.targets.isSelected("Игроки")) {
               return false;
            }
         } else if (entity instanceof Animal) {
            if (!this.targets.isSelected("Животные")) {
               return false;
            }
         } else {
            if (!(entity instanceof Mob)) {
               return false;
            }

            if (!this.targets.isSelected("Мобы")) {
               return false;
            }
         }

         return this.angularDistance(entity) <= this.fov.getValue() * 0.5F;
      } else {
         return false;
      }
   }

   private double angularDistance(LivingEntity entity) {
      Vec3 delta = entity.getEyePosition().subtract(mc.player.getEyePosition());
      float yaw = (float)Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
      float viewYaw = SilentAimManager.isActive() ? SilentAimManager.getFreeYaw() : mc.player.getYRot();
      float viewPitch = SilentAimManager.isActive() ? SilentAimManager.getFreePitch() : mc.player.getXRot();
      return Math.hypot(Mth.wrapDegrees(yaw - viewYaw), pitch - viewPitch);
   }

   @Subscribe
   public void onRender(EventScreenRender event) {
      if (this.drawFov.isValue() && event.getStage() == EventScreenRender.Stage.HUD && mc.player != null && this.isHoldingWeapon()) {
         float width = mc.getWindow().getGuiScaledWidth();
         float height = mc.getWindow().getGuiScaledHeight();
         float diameter = Math.min(width, height) * this.fov.getValue() / 180.0F;
         int color = ColorUtil.withAlpha(ClientColors.CATEGORY_ACCENT_SECOND, 190);
         RenderUtil.drawRingProgress(event.getContext(), (width - diameter) * 0.5F, (height - diameter) * 0.5F, diameter, 1.0F, 0.75F, color, 1.0F);
      }
   }

   private boolean isCharging() {
      ItemStack stack = this.weaponStack();
      return stack.getItem() instanceof CrossbowItem
         ? CrossbowItem.isCharged(stack)
         : mc.player.isUsingItem() && (stack.getItem() instanceof BowItem || stack.getItem() instanceof TridentItem);
   }

   private boolean isHoldingWeapon() {
      ItemStack stack = mc.player.getMainHandItem();
      return stack.getItem() instanceof BowItem || stack.getItem() instanceof TridentItem || stack.getItem() instanceof CrossbowItem;
   }

   private ItemStack weaponStack() {
      return mc.player.isUsingItem() ? mc.player.getUseItem() : mc.player.getMainHandItem();
   }

   private float ballisticPitch(double horizontal, double height, float speed, float gravity) {
      if (horizontal < 0.001) {
         return height > 0.0 ? -90.0F : 90.0F;
      }

      double speedSq = speed * speed;
      double discriminant = speedSq * speedSq - gravity * (gravity * horizontal * horizontal + 2.0 * height * speedSq);
      if (discriminant < 0.0) {
         return (float)(-Math.toDegrees(Math.atan2(height, horizontal)));
      }

      double tangent = (speedSq - Math.sqrt(discriminant)) / (gravity * horizontal);
      return (float)(-Math.toDegrees(Math.atan(tangent)));
   }

   private float bowSpeed(ItemStack stack) {
      int used = stack.getUseDuration(mc.player) - mc.player.getUseItemRemainingTicks();
      float charge = used / 20.0F;
      charge = (charge * charge + charge * 2.0F) / 3.0F;
      return Math.min(charge, 1.0F) * 3.0F;
   }

   private void clearAim() {
      this.target = null;
      if (this.aiming) {
         SilentAimManager.clear();
      }

      this.aiming = false;
   }

   @Override
   public void onDisable() {
      this.clearAim();
      super.onDisable();
   }
}
