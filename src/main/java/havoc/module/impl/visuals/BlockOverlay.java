package havoc.module.impl.visuals;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import havoc.event.impl.EventRender3D;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.ColorSetting;
import havoc.module.setting.SliderSetting;
import havoc.render.Render3D;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@ModuleInfo(name = "BlockOverlay", desc = "", category = Category.VISUALS)
public final class BlockOverlay extends Module {
   private static final float EXPAND = 0.0035F;
   private final BooleanSetting fill = this.registerSetting(new BooleanSetting("Fill", true));
   private final BooleanSetting outline = this.registerSetting(new BooleanSetting("Outline", true));
   private final SliderSetting fillAlpha = this.registerSetting(new SliderSetting("Fill Alpha", 34.0F, 0.0F, 100.0F, 1.0F, "%").visibleWhen(this.fill::isValue));
   private final SliderSetting outlineAlpha = this.registerSetting(
      new SliderSetting("Outline Alpha", 92.0F, 0.0F, 100.0F, 1.0F, "%").visibleWhen(this.outline::isValue)
   );
   private final ColorSetting fillColor = this.registerSetting(new ColorSetting("Fill Color", ClientColors.CATEGORY_ACCENT).visibleWhen(this.fill::isValue));
   private final ColorSetting outlineColor = this.registerSetting(
      new ColorSetting("Outline Color", ClientColors.CATEGORY_ACCENT).visibleWhen(this.outline::isValue)
   );

   @Subscribe
   public void onRender3D(EventRender3D event) {
      AABB worldBox = this.resolveTargetBox();
      if (event != null && worldBox != null) {
         AABB cameraBox = worldBox.move(-event.getCameraPos().x, -event.getCameraPos().y, -event.getCameraPos().z);
         PoseStack matrices = event.getMatrices();
         MultiBufferSource.BufferSource consumers = event.getConsumers();
         if (matrices != null && consumers != null) {
            if (this.fill.isValue()) {
               this.renderClientFill(matrices, consumers, cameraBox);
            }

            if (this.outline.isValue()) {
               VertexConsumer lineConsumer = consumers.getBuffer(RenderTypes.lines());
               Render3D.drawOutlinedBox(
                  matrices, lineConsumer, cameraBox, ColorUtil.multiplyAlpha(this.outlineColor.getColor(), this.outlineAlpha.getValue() / 100.0F)
               );
            }
         }
      }
   }

   private void renderClientFill(PoseStack matrices, MultiBufferSource.BufferSource consumers, AABB box) {
      int accent = this.fillColor.getColor();
      int baseFillColor = ColorUtil.multiplyAlpha(ColorUtil.interpolate(ColorUtil.rgb(9, 13, 24), accent, 0.34F), this.fillAlpha.getValue() / 100.0F * 0.72F);
      VertexConsumer baseConsumer = consumers.getBuffer(RenderTypes.debugQuads());
      Render3D.drawFilledBox(matrices, baseConsumer, box, baseFillColor);
   }

   private AABB resolveTargetBox() {
      if (mc != null && mc.player != null && mc.level != null && mc.hitResult != null) {
         if (mc.hitResult instanceof BlockHitResult blockHitResult && blockHitResult.getType() == Type.BLOCK) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            VoxelShape shape = mc.level.getBlockState(blockPos).getShape(mc.level, blockPos, CollisionContext.of(mc.player));
            return shape.isEmpty() ? null : shape.bounds().move(blockPos).inflate(0.0035F);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }
}
