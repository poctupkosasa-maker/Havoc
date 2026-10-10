package havoc.mixin;

import havoc.Havoc;
import havoc.event.impl.EventKeyPress;
import havoc.module.impl.misc.AntiCapture;
import havoc.module.impl.visuals.Interface;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
   @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
   private void onKey(long window, int action, KeyEvent input, CallbackInfo ci) {
      if (window == Minecraft.getInstance().getWindow().handle()) {
         if (AntiCapture.isInputLocked()) {
            ci.cancel();
            return;
         }
         Havoc.getInstance().getEventBus().post(new EventKeyPress(input.key(), action));
      }
   }

   @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
   private void onHudEditorKey(long window, int action, KeyEvent input, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.gui.screen() instanceof ChatScreen) {
         if (window == mc.getWindow().handle() && action == 1) {
            Interface interfaceModule = Havoc.getInstance().getModuleManager().getModule(Interface.class);
            if (interfaceModule != null && interfaceModule.handleHudEditorKeyPressed(input)) {
               ci.cancel();
            }
         }
      }
   }

   @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
   private void onHudEditorChar(long window, CharacterEvent input, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.getWindow() != null && window == mc.getWindow().handle() && AntiCapture.isInputLocked()) {
         ci.cancel();
         return;
      }
      if (mc != null && mc.gui.screen() instanceof ChatScreen) {
         if (window == mc.getWindow().handle()) {
            Interface interfaceModule = Havoc.getInstance().getModuleManager().getModule(Interface.class);
            int codePoint = this.resolveCharCodePoint(input);
            if (interfaceModule != null && interfaceModule.handleHudEditorCharTyped(codePoint)) {
               ci.cancel();
            }
         }
      }
   }

   private int resolveCharCodePoint(CharacterEvent input) {
      if (input == null) {
         return 0;
      }

      String[] accessors = new String[]{"codePoint", "codepoint", "character", "chr", "charCode", "value"};

      for (String accessor : accessors) {
         try {
            Object value = input.getClass().getMethod(accessor).invoke(input);
            if (value instanceof Number number) {
               return number.intValue();
            }

            if (value instanceof Character character) {
               return character;
            }
         } catch (Exception var9) {
         }
      }

      return 0;
   }
}
