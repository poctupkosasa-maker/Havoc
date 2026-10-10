package havoc.mixin;

import havoc.screen.style.ConsoleStyle;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only replaces the default button surface: labels, icons and input stay vanilla. */
@Mixin(AbstractButton.class)
public abstract class ConsoleButtonMixin {
   @Shadow private Supplier<Boolean> overrideRenderHighlightedSprite;
   @Unique private final ConsoleStyle.FocusAnimation havoc$focus = new ConsoleStyle.FocusAnimation();

   @Inject(method = "extractDefaultSprite", at = @At("HEAD"), cancellable = true)
   private void havoc$consoleSurface(GuiGraphicsExtractor graphics, CallbackInfo ci) {
      AbstractButton button = (AbstractButton)(Object)this;
      boolean highlighted = this.overrideRenderHighlightedSprite != null
         ? this.overrideRenderHighlightedSprite.get() : button.isHoveredOrFocused();
      float focus = this.havoc$focus.update(button.active && highlighted);
      ConsoleStyle.button(graphics, button.getX(), button.getY(), button.getWidth(), button.getHeight(),
         button.active, focus, button.getAlpha(), false);
      ci.cancel();
   }
}
