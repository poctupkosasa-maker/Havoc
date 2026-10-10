package havoc.mixin;

import havoc.Havoc;
import havoc.event.impl.EventPacket;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ClientConnectionMixin {
   @Inject(
      method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
      at = @At("HEAD"),
      cancellable = true,
      require = 0
   )
   private void havoc$onReceivePacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null) {
         EventPacket event = new EventPacket(packet, true);
         havoc.getEventBus().post(event);
         if (event.isCanceled()) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true, require = 0)
   private void havoc$onSendPacket(Packet<?> packet, CallbackInfo ci) {
      if (this.havoc$postSendPacket(packet)) {
         ci.cancel();
      }
   }

   @Inject(
      method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
      at = @At("HEAD"),
      cancellable = true,
      require = 0
   )
   private void havoc$onSendPacketWithListener(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
      if (this.havoc$postSendPacket(packet)) {
         ci.cancel();
      }
   }

   @Inject(
      method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
      at = @At("HEAD"),
      cancellable = true,
      require = 0
   )
   private void havoc$onSendPacketWithListenerAndFlush(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
      if (this.havoc$postSendPacket(packet)) {
         ci.cancel();
      }
   }

   private boolean havoc$postSendPacket(Packet<?> packet) {
      Havoc havoc = Havoc.getInstance();
      if (havoc == null) {
         return false;
      }

      EventPacket event = new EventPacket(packet, false);
      havoc.getEventBus().post(event);
      return event.isCanceled();
   }
}
