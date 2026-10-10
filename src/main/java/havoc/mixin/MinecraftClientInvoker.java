package havoc.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftClientInvoker {
   @Invoker("startAttack")
   boolean havoc$invokeDoAttack();

   @Invoker("startUseItem")
   void havoc$invokeDoItemUse();
}
