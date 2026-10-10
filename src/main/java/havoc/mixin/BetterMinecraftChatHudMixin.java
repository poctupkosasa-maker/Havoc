package havoc.mixin;

import havoc.module.impl.visuals.BetterMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ChatComponent.ChatGraphicsAccess;
import net.minecraft.client.gui.components.ChatComponent.DisplayMode;
import net.minecraft.client.multiplayer.chat.GuiMessage.Line;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class BetterMinecraftChatHudMixin {
   @Unique
   private static float havoc$currentChatTime;

   @Unique
   private static float havoc$getChatProgress(Line line) {
      float age = Math.max(0.0F, havoc$currentChatTime - line.addedTime());
      float raw = Math.min(1.0F, age / 6.0F);
      return BetterMinecraft.easeOutCubic(raw);
   }

   @Inject(method = "extractRenderState", at = @At("HEAD"))
   private void havoc$captureChatTick(
      GuiGraphicsExtractor context, Font textRenderer, int currentTick, int mouseX, int mouseY, DisplayMode displayMode, boolean chatFocused, CallbackInfo ci
   ) {
      Minecraft client = Minecraft.getInstance();
      float partial = client != null && client.getDeltaTracker() != null ? client.getDeltaTracker().getGameTimeDeltaPartialTick(false) : 0.0F;
      havoc$currentChatTime = currentTick + partial;
   }

   @Inject(method = "lambda$extractRenderState$1", at = @At("HEAD"), cancellable = true)
   private static void havoc$animateChatLineBackground(
      int baseY, int lineHeight, ChatGraphicsAccess backend, int x, float backgroundOpacity, Line line, int lineIndex, float lineOpacity, CallbackInfo ci
   ) {
      if (BetterMinecraft.shouldAnimateChat()) {
         float progress = havoc$getChatProgress(line);
         if (!(progress >= 0.999F)) {
            float rise = (1.0F - progress) * 5.0F;
            float alpha = Math.max(0.0F, Math.min(1.0F, backgroundOpacity * lineOpacity * progress));
            int bottomY = baseY - lineIndex * lineHeight;
            int topY = bottomY - lineHeight;
            backend.updatePose(matrix -> matrix.translate(0.0F, rise));
            backend.fill(-4, topY, x + 8, bottomY, ARGB.color(alpha, -16777216));
            ci.cancel();
         }
      }
   }
}
