package havoc.mixin;

import havoc.Havoc;
import havoc.module.impl.misc.ItemScroller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class ItemScrollerHandledScreenMixin<T extends AbstractContainerMenu> {
   @Shadow
   protected Slot hoveredSlot;
   private long havoc$lastScrollAction;

   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void havoc$itemScroller(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      ItemScroller module = Havoc.getInstance().getModuleManager().getModule(ItemScroller.class);
      if (module != null && module.isEnable() && client.player != null && client.gameMode != null && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
         if (client.options.keyShift.isDown() && !client.options.keySprint.isDown()) {
            ContainerInput action = client.options.keyDrop.isDown()
               ? ContainerInput.THROW
               : (client.options.keyAttack.isDown() ? ContainerInput.QUICK_MOVE : null);
            if (action != null && System.currentTimeMillis() - this.havoc$lastScrollAction >= module.getDelayMs()) {
               client.gameMode
                  .handleContainerInput(
                     client.player.containerMenu.containerId, this.hoveredSlot.index, action == ContainerInput.THROW ? 1 : 0, action, client.player
                  );
               this.havoc$lastScrollAction = System.currentTimeMillis();
            }
         }
      }
   }
}
