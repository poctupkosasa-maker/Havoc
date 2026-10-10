package havoc.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList.Entry;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList.OnlineServerEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin {
   @Shadow
   protected ServerSelectionList serverSelectionList;
   @Shadow
   private Button editButton;
   @Shadow
   private Button deleteButton;

   @Inject(method = "onSelectedChange", at = @At("RETURN"))
   private void havoc$lockFeaturedServerButtons(CallbackInfo ci) {
      if (this.havoc$isFeaturedSelected()) {
         this.editButton.active = false;
         this.deleteButton.active = false;
      }
   }

   @Inject(method = "deleteCallback", at = @At("HEAD"), cancellable = true)
   private void havoc$cancelFeaturedServerDeletion(boolean confirmed, CallbackInfo ci) {
      if (this.havoc$isFeaturedSelected()) {
         Minecraft.getInstance().gui.setScreen((JoinMultiplayerScreen)(Object)this);
         ci.cancel();
      }
   }

   @Inject(method = "editServerCallback", at = @At("HEAD"), cancellable = true)
   private void havoc$cancelFeaturedServerEdit(boolean confirmed, CallbackInfo ci) {
      if (this.havoc$isFeaturedSelected()) {
         Minecraft.getInstance().gui.setScreen((JoinMultiplayerScreen)(Object)this);
         ci.cancel();
      }
   }

   @Unique
   private boolean havoc$isFeaturedSelected() {
      if (this.serverSelectionList == null) {
         return false;
      }

      Entry selected = (Entry)this.serverSelectionList.getSelected();
      return selected instanceof OnlineServerEntry online && "mc.bravohvh.ru".equalsIgnoreCase(online.getServerData().ip);
   }
}
