package havoc.mixin;

import havoc.Havoc;
import havoc.module.impl.combat.HitBox;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityHitboxMixin {
   @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
   private void havoc$expandHitbox(CallbackInfoReturnable<AABB> cir) {
      Entity entity = (Entity)(Object)this;
      if (entity instanceof LivingEntity && entity != Minecraft.getInstance().player) {
         Havoc havoc = Havoc.getInstance();
         HitBox module = havoc == null ? null : havoc.getModuleManager().getModule(HitBox.class);
         if (module != null && module.isEnable()) {
            AABB box = (AABB)cir.getReturnValue();
            float xz = module.horizontal() * 0.5F;
            float y = module.vertical() * 0.5F;
            cir.setReturnValue(new AABB(box.minX - xz, box.minY - y, box.minZ - xz, box.maxX + xz, box.maxY + y, box.maxZ + xz));
         }
      }
   }
}
