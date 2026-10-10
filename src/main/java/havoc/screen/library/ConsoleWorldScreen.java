package havoc.screen.library;

import havoc.mixin.ConsoleWorldEntryAccessor;
import havoc.mixin.ConsoleWorldListAccessor;
import havoc.mixin.ConsoleWorldScreenAccessor;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

/** Vanilla world loading, backup/version checks and management, presented as a carousel. */
public final class ConsoleWorldScreen extends SelectWorldScreen {
   private ConsoleLibraryView view;
   private WorldSelectionList worlds;
   private List<WorldSelectionList.Entry> displayed = List.of();

   public ConsoleWorldScreen(Screen parent) { super(parent); }

   @Override
   protected void init() {
      this.view = null;
      super.init();
      this.worlds = ((ConsoleWorldScreenAccessor)(Object)this).havoc$list();
      List<Button> actions = this.children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
      this.clearWidgets();
      this.setFocused(null);
      this.view = new ConsoleLibraryView(this, false, actions, button -> this.addRenderableWidget(button),
         () -> { }, () -> this.minecraft.gui.setScreen(new ConsoleServerScreen(this.lastScreen)));
      if (this.searchBox != null) {
         this.searchBox.setPosition(Math.max(160, this.width - 170), 12);
         this.searchBox.setWidth(Math.min(108, this.width - 220));
         this.addRenderableWidget(this.searchBox);
      }
      this.displayed = List.of();
      this.syncCards();
   }

   @Override
   protected void repositionElements() {
      // Vanilla's header/footer layout would otherwise move our controls on resize.
      if (view != null) { super.removed(); this.rebuildWidgets(); }
      else super.repositionElements();
   }

   @Override
   protected void setInitialFocus() {
      // Leave arrows available for browsing; the search box can still be focused via Tab/click.
   }

   @Override
   public void tick() {
      super.tick();
      if (worlds == null || view == null) return;
      ConsoleWorldListAccessor access = (ConsoleWorldListAccessor)worlds;
      var levels = access.havoc$poll();
      if (levels != access.havoc$displayed()) access.havoc$update(levels);
      if (this.minecraft.gui.screen() != this) return;
      this.syncCards();
   }

   private void syncCards() {
      if (worlds.children().equals(displayed)) return;
      displayed = List.copyOf(worlds.children());
      List<LibraryCard> cards = displayed.stream().filter(WorldSelectionList.WorldListEntry.class::isInstance)
         .map(WorldSelectionList.WorldListEntry.class::cast).map(entry -> (LibraryCard)new LibraryCard() {
            public String id() { return entry.getLevelSummary().getLevelId(); }
            public String title() { return entry.getLevelName(); }
            public String description() { return entry.getLevelSummary().getInfo().getString(); }
            public String detail() {
               long played = entry.getLevelSummary().getLastPlayed();
               return played <= 0 ? "Новый мир" : "Последняя игра: " + DateTimeFormatter.ofPattern("dd.MM.yyyy")
                  .format(Instant.ofEpochMilli(played).atZone(ZoneId.systemDefault()));
            }
            public Identifier image() { return ((ConsoleWorldEntryAccessor)(Object)entry).havoc$icon().textureLocation(); }
            public void select() { worlds.setSelected(entry); }
         }).toList();
      if (cards.isEmpty()) worlds.setSelected(null);
      view.setCards(cards);
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) { }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      if (view != null) view.render(graphics, mouseX, mouseY, delta);
      super.extractRenderState(graphics, mouseX, mouseY, delta);
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (view != null && (searchBox == null || !searchBox.isFocused()) && view.keyPressed(event)) return true;
      return super.keyPressed(event);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
      if (view != null && view.dismissOptions(event.x(), event.y())) return true;
      return super.mouseClicked(event, doubled);
   }

   @Override
   public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      if (view != null) { view.scrolled(vertical != 0 ? vertical : horizontal); return true; }
      return super.mouseScrolled(x, y, horizontal, vertical);
   }
}
