package havoc.mixin;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerData.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerList.class)
public abstract class ServerListMixin {
   private static final String HAVOC_SERVER_NAME = "Лучший ХвХ сервер";
   private static final String HAVOC_SERVER_IP = "mc.bravohvh.ru";
   @Unique
   private boolean havoc$pinningFeaturedServer;

   @Shadow
   public abstract int size();

   @Shadow
   public abstract ServerData get(int var1);

   @Shadow
   public abstract void add(ServerData var1, boolean var2);

   @Shadow
   public abstract void swap(int var1, int var2);

   @Shadow
   public abstract void save();

   @Inject(method = "load", at = @At("RETURN"))
   private void havoc$pinFeaturedServer(CallbackInfo ci) {
      int index = -1;

      for (int i = 0; i < this.size(); i++) {
         if ("mc.bravohvh.ru".equalsIgnoreCase(this.get(i).ip)) {
            index = i;
            break;
         }
      }

      boolean changed = false;
      if (index < 0) {
         this.add(new ServerData("Лучший ХвХ сервер", "mc.bravohvh.ru", Type.OTHER), false);
         index = this.size() - 1;
         changed = true;
      } else if (!"Лучший ХвХ сервер".equals(this.get(index).name)) {
         this.get(index).name = "Лучший ХвХ сервер";
         changed = true;
      }

      this.havoc$pinningFeaturedServer = true;

      try {
         while (index > 0) {
            this.swap(index, index - 1);
            index--;
            changed = true;
         }
      } finally {
         this.havoc$pinningFeaturedServer = false;
      }

      if (changed) {
         this.save();
      }
   }

   @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
   private void havoc$preventFeaturedServerRemoval(ServerData server, CallbackInfo ci) {
      if (havoc$isFeatured(server)) {
         ci.cancel();
      }
   }

   @Inject(method = "replace", at = @At("HEAD"), cancellable = true)
   private void havoc$preventFeaturedServerReplacement(int index, ServerData server, CallbackInfo ci) {
      if (index >= 0 && index < this.size() && havoc$isFeatured(this.get(index))) {
         ci.cancel();
      }
   }

   @Inject(method = "swap", at = @At("HEAD"), cancellable = true)
   private void havoc$preventFeaturedServerMove(int first, int second, CallbackInfo ci) {
      if (!this.havoc$pinningFeaturedServer) {
         if (first >= 0 && first < this.size() && havoc$isFeatured(this.get(first))) {
            ci.cancel();
         }

         if (second >= 0 && second < this.size() && havoc$isFeatured(this.get(second))) {
            ci.cancel();
         }
      }
   }

   @Unique
   private static boolean havoc$isFeatured(ServerData server) {
      return server != null && "mc.bravohvh.ru".equalsIgnoreCase(server.ip);
   }
}
