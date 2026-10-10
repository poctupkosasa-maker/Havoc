package havoc.module.impl.visuals;

import com.google.common.eventbus.Subscribe;
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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(name = "Trails", desc = "Luma player trail", category = Category.VISUALS)
public final class Trails extends Module {
   private static final int MAX_POINTS = 90;
   private final SliderSetting life = this.registerSetting(new SliderSetting("Длина следа", 1.25F, 0.35F, 3.0F, 0.05F, "s"));
   private final SliderSetting width = this.registerSetting(new SliderSetting("Толщина", 2.15F, 0.8F, 5.0F, 0.05F));
   private final BooleanSetting themeColor = this.registerSetting(new BooleanSetting("Цвет темы", true));
   private final ColorSetting firstColor = this.registerSetting(new ColorSetting("Первый цвет", -167772161).visibleWhen(() -> !this.themeColor.isValue()));
   private final ColorSetting secondColor = this.registerSetting(new ColorSetting("Второй цвет", -760849153).visibleWhen(() -> !this.themeColor.isValue()));
   private final List<Trails.TrailPoint> points = new ArrayList<>();

   @Override
   public void onDisable() {
      this.points.clear();
      super.onDisable();
   }

   @Subscribe
   public void onRender3D(EventRender3D event) {
      if (mc.player != null && mc.level != null) {
         long now = System.currentTimeMillis();
         Vec3 playerPos = new Vec3(
            Mth.lerp(event.getTickDelta(), mc.player.xo, mc.player.getX()),
            Mth.lerp(event.getTickDelta(), mc.player.yo, mc.player.getY()) + 0.08,
            Mth.lerp(event.getTickDelta(), mc.player.zo, mc.player.getZ())
         );
         if (this.points.isEmpty() || this.points.getLast().pos.distanceToSqr(playerPos) > 0.0035) {
            this.points.add(new Trails.TrailPoint(playerPos, now));
         }

         long maxAge = (long)(this.life.getValue() * 1000.0F);
         this.points.removeIf(point -> now - point.time > maxAge);

         while (this.points.size() > 90) {
            this.points.removeFirst();
         }

         if (this.points.size() >= 2) {
            VertexConsumer lines = event.getConsumers().getBuffer(RenderTypes.lines());
            Vec3 camera = event.getCameraPos();
            int start = this.themeColor.isValue() ? -167772161 : this.firstColor.getColor();
            int end = this.themeColor.isValue() ? ColorUtil.withAlpha(ClientColors.CATEGORY_ACCENT, 210) : this.secondColor.getColor();

            for (int index = 1; index < this.points.size(); index++) {
               Trails.TrailPoint previous = this.points.get(index - 1);
               Trails.TrailPoint current = this.points.get(index);
               float fadePrevious = 1.0F - Mth.clamp((float)(now - previous.time) / (float)maxAge, 0.0F, 1.0F);
               float fadeCurrent = 1.0F - Mth.clamp((float)(now - current.time) / (float)maxAge, 0.0F, 1.0F);
               float progress = (float)index / (this.points.size() - 1);
               int previousColor = ColorUtil.withAlpha(ColorUtil.interpolate(end, start, progress), Math.round(235.0F * fadePrevious));
               int currentColor = ColorUtil.withAlpha(ColorUtil.interpolate(end, start, progress), Math.round(235.0F * fadeCurrent));
               Render3D.drawLine(event.getMatrices(), lines, previous.pos.subtract(camera), current.pos.subtract(camera), previousColor);
               Render3D.drawLine(
                  event.getMatrices(),
                  lines,
                  previous.pos.subtract(camera).add(0.0, 0.015, 0.0),
                  current.pos.subtract(camera).add(0.0, 0.015, 0.0),
                  currentColor
               );
            }
         }
      } else {
         this.points.clear();
      }
   }

   private record TrailPoint(Vec3 pos, long time) {
   }
}
