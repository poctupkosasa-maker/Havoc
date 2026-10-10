package havoc.event.impl;

import havoc.event.Event;
import lombok.Generated;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class EventScreenRender extends Event {
   private final GuiGraphicsExtractor context;
   private final DeltaTracker tickCounter;
   private final EventScreenRender.Stage stage;

   public EventScreenRender(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
      this(context, tickCounter, EventScreenRender.Stage.HUD);
   }

   public EventScreenRender(GuiGraphicsExtractor context, DeltaTracker tickCounter, EventScreenRender.Stage stage) {
      this.context = context;
      this.tickCounter = tickCounter;
      this.stage = stage == null ? EventScreenRender.Stage.HUD : stage;
   }

   @Generated
   public GuiGraphicsExtractor getContext() {
      return this.context;
   }

   @Generated
   public DeltaTracker getTickCounter() {
      return this.tickCounter;
   }

   @Generated
   public EventScreenRender.Stage getStage() {
      return this.stage;
   }

   public enum Stage {
      ESP,
      HUD;
   }
}
