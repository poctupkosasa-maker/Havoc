package me.x150.renderer.mixin;

import java.util.ArrayDeque;
import java.util.Deque;
import me.x150.renderer.render.GuiScissorTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin implements GuiScissorTracker {
   @Unique
   private static final ScreenRectangle HAVOC_HIDDEN_SCISSOR = new ScreenRectangle(0, 0, 1, 1);
   @Shadow
   @Final
   private Matrix3x2fStack pose;
   @Unique
   private final Deque<ScreenRectangle> havoc$scissors = new ArrayDeque<>();

   @Inject(method = "enableScissor", at = @At("HEAD"))
   private void havoc$pushScissor(int x1, int y1, int x2, int y2, CallbackInfo ci) {
      ScreenRectangle area = new ScreenRectangle(x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1)).transformMaxBounds(new Matrix3x2f(this.pose));
      ScreenRectangle parent = this.havoc$scissors.peek();
      ScreenRectangle clipped = parent == null ? area : parent.intersection(area);
      this.havoc$scissors.push(havoc$nonEmptyScissor(havoc$clipToFramebuffer(clipped)));
   }

   @Inject(method = "disableScissor", at = @At("HEAD"))
   private void havoc$popScissor(CallbackInfo ci) {
      if (!this.havoc$scissors.isEmpty()) {
         this.havoc$scissors.pop();
      }
   }

   @Override
   public ScreenRectangle havoc$currentScissor() {
      return this.havoc$scissors.peek();
   }

   @Unique
   private static ScreenRectangle havoc$nonEmptyScissor(ScreenRectangle area) {
      return area != null && area.width() > 0 && area.height() > 0 ? area : HAVOC_HIDDEN_SCISSOR;
   }

   @Unique
   private static ScreenRectangle havoc$clipToFramebuffer(ScreenRectangle area) {
      if (area == null) {
         return null;
      } else {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft != null && minecraft.getWindow() != null) {
            ScreenRectangle framebuffer = new ScreenRectangle(
               0, 0, Math.max(1, minecraft.getWindow().getWidth()), Math.max(1, minecraft.getWindow().getHeight())
            );
            return framebuffer.intersection(area);
         } else {
            return area;
         }
      }
   }
}
