package havoc.mixin;

import havoc.Havoc;
import havoc.module.impl.misc.NameProtect;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Font.class)
public abstract class NameProtectTextRendererMixin {
   @ModifyVariable(
      method = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V",
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0,
      require = 0
   )
   private String havoc$protectString(String value) {
      return protect(value);
   }

   @ModifyVariable(
      method = "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V",
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0,
      require = 0
   )
   private Component havoc$protectText(Component value) {
      if (value == null) {
         return null;
      }

      String protectedValue = protect(value.getString());
      return (Component)(protectedValue.equals(value.getString()) ? value : Component.literal(protectedValue).setStyle(value.getStyle()));
   }

   @ModifyVariable(
      method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V",
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0,
      require = 0
   )
   private FormattedCharSequence havoc$protectOrdered(FormattedCharSequence value) {
      if (value == null) {
         return null;
      }

      StringBuilder builder = new StringBuilder();
      value.accept((index, style, codePoint) -> {
         builder.appendCodePoint(codePoint);
         return true;
      });
      String original = builder.toString();
      String protectedValue = protect(original);
      return protectedValue.equals(original) ? value : Component.literal(protectedValue).getVisualOrderText();
   }

   private static String protect(String value) {
      Havoc havoc = Havoc.getInstance();
      NameProtect module = havoc == null ? null : havoc.getModuleManager().getModule(NameProtect.class);
      return module == null ? value : module.replace(value);
   }
}
