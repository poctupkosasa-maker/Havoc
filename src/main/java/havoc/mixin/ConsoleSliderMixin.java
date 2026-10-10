package havoc.mixin;

import havoc.render.RenderUtil;
import havoc.screen.style.ConsoleStyle;
import havoc.utilites.color.ColorUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
public abstract class ConsoleSliderMixin extends AbstractWidget.WithInactiveMessage {
   @Shadow protected double value;
   @Shadow protected boolean canChangeValue;
   @Unique private final ConsoleStyle.FocusAnimation havoc$focus = new ConsoleStyle.FocusAnimation();

   protected ConsoleSliderMixin(int x, int y, int width, int height, Component message) {
      super(x, y, width, height, message);
   }

   @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
   private void havoc$consoleSlider(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      float focus = this.havoc$focus.update(this.active && (this.isHoveredOrFocused() || this.canChangeValue));
      ConsoleStyle.button(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.active, focus, this.alpha, false);
      float trackWidth = Math.max(0, this.getWidth() - 12);
      float trackY = this.getY() + this.getHeight() - 4;
      float progress = (float)Math.clamp(this.value, 0.0, 1.0);
      RenderUtil.drawRoundRect(graphics, this.getX() + 6, trackY, trackWidth, 1.5F, 0.75F,
         ColorUtil.multiplyAlpha(0xFF52617A, this.alpha));
      RenderUtil.drawRoundRect(graphics, this.getX() + 6, trackY, trackWidth * progress, 1.5F, 0.75F,
         ColorUtil.multiplyAlpha(this.active ? 0xFFB6D7FF : 0xFF667389, this.alpha));
      RenderUtil.drawRoundRect(graphics, this.getX() + 4 + trackWidth * progress, trackY - 1.5F, 4, 4.5F, 2,
         ColorUtil.multiplyAlpha(this.active ? ConsoleStyle.TEXT : ConsoleStyle.MUTED, this.alpha));
      this.extractScrollingStringOverContents(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE), this.getMessage(), 2);
      this.handleCursor(graphics);
      ci.cancel();
   }
}
