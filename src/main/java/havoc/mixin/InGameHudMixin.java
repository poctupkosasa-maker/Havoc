package havoc.mixin;

import havoc.Havoc;
import havoc.event.impl.EventScreenRender;
import havoc.module.impl.visuals.ArrayListModule;
import havoc.module.impl.visuals.Interface;
import havoc.module.impl.visuals.NoRender;
import havoc.screen.menu.MenuCloseAnimation;
import havoc.screen.screen.elements.LogoHudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class InGameHudMixin {
   @Unique
   private boolean havoc$arrayListScoreboardTranslated;
   @Unique
   private boolean havoc$watermarkBossTranslated;
   @Unique
   private boolean havoc$watermarkHotbarTranslated;

   @Inject(method = "extractBossOverlay", at = @At("HEAD"))
   private void havoc$offsetBossbarForWatermark(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
      this.havoc$watermarkBossTranslated = LogoHudElement.isTopPosition();
      if (this.havoc$watermarkBossTranslated) {
         context.pose().pushMatrix();
         context.pose().translate(0.0F, 47.0F);
      }
   }

   @Inject(method = "extractBossOverlay", at = @At("RETURN"))
   private void havoc$restoreBossbarAfterWatermark(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
      if (this.havoc$watermarkBossTranslated) {
         context.pose().popMatrix();
         this.havoc$watermarkBossTranslated = false;
      }
   }

   @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"))
   private void havoc$offsetHotbarForWatermark(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
      this.havoc$watermarkHotbarTranslated = LogoHudElement.isBottomPosition();
      if (this.havoc$watermarkHotbarTranslated) {
         context.pose().pushMatrix();
         context.pose().translate(0.0F, -18.0F);
      }
   }

   @Inject(method = "extractHotbarAndDecorations", at = @At("RETURN"))
   private void havoc$restoreHotbarAfterWatermark(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
      if (this.havoc$watermarkHotbarTranslated) {
         context.pose().popMatrix();
         this.havoc$watermarkHotbarTranslated = false;
      }
   }

   @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
   private void onRenderStatusEffectOverlay(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null && havoc.getModuleManager() != null) {
         if (NoRender.hideBadEffects()) {
            ci.cancel();
         } else {
            Interface interfaceModule = havoc.getModuleManager().getModule(Interface.class);
            if (interfaceModule != null && interfaceModule.isEnable()) {
               if (interfaceModule.isHudElementEnabled("Potions")) {
                  ci.cancel();
               }
            }
         }
      }
   }

   @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
   private void onRenderNauseaOverlay(GuiGraphicsExtractor context, float distortionStrength, CallbackInfo ci) {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null && havoc.getModuleManager() != null) {
         if (NoRender.hideBadEffects()) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"))
   private void havoc$offsetScoreboardForArrayList(GuiGraphicsExtractor context, Objective objective, CallbackInfo ci) {
      this.havoc$arrayListScoreboardTranslated = false;
      Havoc havoc = Havoc.getInstance();
      if (havoc != null && havoc.getModuleManager() != null && objective != null) {
         ArrayListModule arrayList = havoc.getModuleManager().getModule(ArrayListModule.class);
         if (arrayList != null && arrayList.shouldOffsetScoreboard()) {
            long visibleEntries = objective.getScoreboard().listPlayerScores(objective).stream().filter(entry -> !entry.isHidden()).limit(15L).count();
            float scoreboardTop = context.guiHeight() * 0.5F - (float)visibleEntries * 6.0F;
            float scoreboardBottom = context.guiHeight() * 0.5F + (float)visibleEntries * 6.0F + 10.0F;
            float arrayListTop = arrayList.getRenderedTop();
            float arrayListBottom = arrayList.getRenderedBottom();
            if (!(arrayListBottom <= scoreboardTop) && !(arrayListTop >= scoreboardBottom)) {
               float offset = Math.max(0.0F, arrayListBottom + 4.0F - scoreboardTop);
               if (!(offset <= 0.01F)) {
                  context.pose().pushMatrix();
                  context.pose().translate(0.0F, offset);
                  this.havoc$arrayListScoreboardTranslated = true;
               }
            }
         }
      }
   }

   @Inject(method = "displayScoreboardSidebar", at = @At("RETURN"))
   private void havoc$restoreScoreboardAfterArrayList(GuiGraphicsExtractor context, Objective objective, CallbackInfo ci) {
      if (this.havoc$arrayListScoreboardTranslated) {
         context.pose().popMatrix();
         this.havoc$arrayListScoreboardTranslated = false;
      }
   }

   @Inject(method = "extractRenderState", at = @At("RETURN"))
   private void onRenderHud(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null) {
         havoc.getEventBus().post(new EventScreenRender(context, tickCounter, EventScreenRender.Stage.ESP));
         havoc.getEventBus().post(new EventScreenRender(context, tickCounter, EventScreenRender.Stage.HUD));
         MenuCloseAnimation.render(context);
      }
   }
}
