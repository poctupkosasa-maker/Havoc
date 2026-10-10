package havoc.mixin;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import havoc.Havoc;
import havoc.command.ClientCommandManager;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
   @Shadow
   protected EditBox input;
   @Shadow
   private CommandSuggestions commandSuggestions;

   @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
   private void havoc$handleDotCommands(String text, boolean addToHistory, CallbackInfo ci) {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null && havoc.getCommandManager() != null && havoc.getCommandManager().handleChatMessage(text)) {
         ci.cancel();
      }
   }

   @Inject(method = "init", at = @At("TAIL"))
   private void havoc$showInitialClientSuggestions(CallbackInfo ci) {
      this.havoc$updateClientSuggestions();
   }

   @Inject(method = "onEdited", at = @At("TAIL"))
   private void havoc$updateClientSuggestions(String value, CallbackInfo ci) {
      this.havoc$updateClientSuggestions();
   }

   @Unique
   private void havoc$updateClientSuggestions() {
      if (this.input != null && this.commandSuggestions != null && this.input.getValue().startsWith(".")) {
         Havoc havoc = Havoc.getInstance();
         if (havoc != null && havoc.getCommandManager() != null) {
            String value = this.input.getValue();
            List<ClientCommandManager.CommandSuggestion> clientSuggestions = havoc.getCommandManager().getSuggestions(value);
            if (clientSuggestions.isEmpty()) {
               this.commandSuggestions.hide();
            } else {
               int cursor = this.input.getCursorPosition();
               String beforeCursor = value.substring(0, cursor);
               int replacementStart = beforeCursor.lastIndexOf(32) + 1;
               if (replacementStart == 0 && beforeCursor.startsWith(".")) {
                  replacementStart = 1;
               }

               StringRange range = StringRange.between(replacementStart, cursor);
               int start = replacementStart;
               List<Suggestion> brigadierSuggestions = clientSuggestions.stream()
                  .filter(suggestion -> suggestion.completion().length() >= start)
                  .map(suggestion -> new Suggestion(range, suggestion.completion().substring(start)))
                  .toList();
               if (brigadierSuggestions.isEmpty()) {
                  this.commandSuggestions.hide();
               } else {
                  Suggestions suggestions = new Suggestions(range, brigadierSuggestions);
                  ((CommandSuggestionsAccessor)this.commandSuggestions).havoc$setPendingSuggestions(CompletableFuture.completedFuture(suggestions));
                  this.commandSuggestions.setAllowSuggestions(true);
                  this.commandSuggestions.showSuggestions(false);
               }
            }
         }
      }
   }
}
