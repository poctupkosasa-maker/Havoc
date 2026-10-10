package havoc.mixin;

import havoc.Havoc;
import havoc.module.impl.misc.NameProtect;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(StringDecomposer.class)
public abstract class NameProtectTextVisitMixin {
   @ModifyVariable(
      method = {
            "iterate(Ljava/lang/String;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
            "iterateBackwards(Ljava/lang/String;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
            "iterateFormatted(Ljava/lang/String;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z"
      },
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0,
      require = 0
   )
   private static String havoc$protectName(String text) {
      Havoc havoc = Havoc.getInstance();
      NameProtect module = havoc == null ? null : havoc.getModuleManager().getModule(NameProtect.class);
      return module == null ? text : module.replace(text);
   }
}
