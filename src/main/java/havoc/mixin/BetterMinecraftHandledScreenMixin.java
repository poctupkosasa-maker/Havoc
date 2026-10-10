package havoc.mixin;

import havoc.module.impl.visuals.BetterMinecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class BetterMinecraftHandledScreenMixin {
   @Unique
   private boolean havoc$containerTransformApplied;
   @Unique
   private boolean havoc$backgroundBypassApplied;
   @Unique
   private float havoc$containerCenterX;
   @Unique
   private float havoc$containerCenterY;
   @Unique
   private float havoc$containerScale;
   @Unique
   private float havoc$containerOffsetY;

   @Inject(method = "init", at = @At("TAIL"))
   private void havoc$markContainerOpened(CallbackInfo ci) {
      if ((Object)this instanceof AbstractContainerScreen<?> handledScreen) {
         BetterMinecraft.markContainerOpened(handledScreen);
      }
   }

   @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"))
   private void havoc$pushContainerAnimation(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if ((Object)this instanceof AbstractContainerScreen<?> handledScreen) {
         float progress = BetterMinecraft.getContainerAnimationProgress(handledScreen);
         if (progress >= 0.999F) {
            this.havoc$containerTransformApplied = false;
         } else {
            this.havoc$containerScale = 0.9F + 0.1F * progress;
            this.havoc$containerOffsetY = (1.0F - progress) * 18.0F;
            this.havoc$containerCenterX = context.guiWidth() * 0.5F;
            this.havoc$containerCenterY = context.guiHeight() * 0.5F;
            context.pose().pushMatrix();
            context.pose().translate(this.havoc$containerCenterX, this.havoc$containerCenterY + this.havoc$containerOffsetY);
            context.pose().scale(this.havoc$containerScale, this.havoc$containerScale);
            context.pose().translate(-this.havoc$containerCenterX, -this.havoc$containerCenterY);
            this.havoc$containerTransformApplied = true;
            this.havoc$backgroundBypassApplied = false;
         }
      } else {
         this.havoc$containerTransformApplied = false;
      }
   }

   @Inject(method = "extractBackground", at = @At("HEAD"))
   private void havoc$disableAnimationForDarkening(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (this.havoc$containerTransformApplied && (Object)this instanceof AbstractContainerScreen<?>) {
         context.pose().popMatrix();
         this.havoc$backgroundBypassApplied = true;
      } else {
         this.havoc$backgroundBypassApplied = false;
      }
   }

   @Inject(method = "extractBackground", at = @At("RETURN"))
   private void havoc$restoreAnimationAfterDarkening(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (this.havoc$backgroundBypassApplied && (Object)this instanceof AbstractContainerScreen<?>) {
         context.pose().pushMatrix();
         context.pose().translate(this.havoc$containerCenterX, this.havoc$containerCenterY + this.havoc$containerOffsetY);
         context.pose().scale(this.havoc$containerScale, this.havoc$containerScale);
         context.pose().translate(-this.havoc$containerCenterX, -this.havoc$containerCenterY);
         this.havoc$backgroundBypassApplied = false;
      }
   }

   @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("RETURN"))
   private void havoc$popContainerAnimation(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (this.havoc$backgroundBypassApplied) {
         context.pose().pushMatrix();
         context.pose().translate(this.havoc$containerCenterX, this.havoc$containerCenterY + this.havoc$containerOffsetY);
         context.pose().scale(this.havoc$containerScale, this.havoc$containerScale);
         context.pose().translate(-this.havoc$containerCenterX, -this.havoc$containerCenterY);
         this.havoc$backgroundBypassApplied = false;
      }

      if (this.havoc$containerTransformApplied) {
         context.pose().popMatrix();
         this.havoc$containerTransformApplied = false;
      }
   }
}