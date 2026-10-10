package havoc.module.impl.visuals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

@ModuleInfo(name = "SwingAnimation", desc = "Luma first-person swing animations", category = Category.VISUALS)
public final class SwingAnimation extends Module {
   private final SelectSetting type = this.registerSetting(new SelectSetting("Type", "Smooth", List.of("Smooth", "Swipe", "GSmar")));
   private final SliderSetting strength = this.registerSetting(new SliderSetting("Strength", 1.0F, 0.5F, 3.0F, 0.1F));
   private final SliderSetting duration = this.registerSetting(new SliderSetting("Duration", 1.0F, 0.5F, 4.0F, 0.1F));
   private final BooleanSetting onlySwinging = this.registerSetting(new BooleanSetting("Only Swinging", false));

   public boolean shouldApply(float swingProgress) {
      if (!this.isEnable() || mc.player == null) {
         return false;
      }

      if (this.onlySwinging.isValue() && swingProgress <= 0.001F) {
         return false;
      }

      return true;
   }

   public boolean shouldModifyDuration() {
      return this.isEnable();
   }

   public float getDuration() {
      return this.duration.getValue();
   }

   public void apply(PoseStack matrices, HumanoidArm arm, float rawProgress) {
      float progress = Mth.clamp(rawProgress, 0.0F, 1.0F);
      int side = arm == HumanoidArm.RIGHT ? 1 : -1;
      float sin1 = Mth.sin(progress * progress * (float) Math.PI);
      float sin2 = Mth.sin(Mth.sqrt(progress) * (float) Math.PI);
      float power = this.strength.getValue();
      switch (this.type.getValue()) {
         case "Swipe":
            matrices.translate(0.56F * side, -0.32F, -0.72F);
            matrices.mulPose(Axis.YP.rotationDegrees(70 * side));
            matrices.mulPose(Axis.ZP.rotationDegrees(-20 * side));
            matrices.mulPose(Axis.YP.rotationDegrees(sin2 * sin1 * -5.0F * power));
            matrices.mulPose(Axis.XP.rotationDegrees(sin2 * sin1 * -120.0F * power));
            matrices.mulPose(Axis.XP.rotationDegrees(-70.0F));
            break;
         case "GSmar":
            float thrust = Mth.sin(progress * (float) Math.PI);
            float snap = Mth.sin(progress * (float) (Math.PI * 2)) * thrust;
            matrices.translate(side * 0.5F, -0.34F + thrust * 0.08F * power, -0.62F - thrust * 0.34F * power);
            matrices.mulPose(Axis.YP.rotationDegrees(side * (38.0F - thrust * 10.0F * power)));
            matrices.mulPose(Axis.ZP.rotationDegrees(side * (-13.0F + snap * 8.0F * power)));
            matrices.mulPose(Axis.XP.rotationDegrees(-68.0F + snap * 34.0F * power));
            matrices.translate(0.0F, -0.08F, thrust * 0.1F * power);
            break;
         case "Smooth":
            matrices.translate(side * 0.56F, -0.42F, -0.72F);
            matrices.mulPose(Axis.YP.rotationDegrees(side * (45.0F + sin1 * -20.0F * power)));
            matrices.mulPose(Axis.ZP.rotationDegrees(side * sin2 * -20.0F * power));
            matrices.mulPose(Axis.XP.rotationDegrees(sin2 * -80.0F * power));
            matrices.mulPose(Axis.YP.rotationDegrees(side * -45));
            matrices.translate(0.0, -0.1, 0.0);
      }
   }
}
