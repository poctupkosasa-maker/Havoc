package havoc.screen.library;

import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.screen.menu.UiAnimationEngine;
import havoc.screen.style.ConsoleStyle;
import havoc.utilites.color.ColorUtil;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Composes a carousel around the original vanilla action buttons and callbacks. */
public final class ConsoleLibraryView {
   private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
   private final Screen owner;
   private final boolean servers;
   private final List<Button> actions;
   private final List<TileButton> tiles = new ArrayList<>();
   private final Button more;
   private final Button previous;
   private final Button next;
   private final UiAnimationEngine animation = new UiAnimationEngine();
   private List<LibraryCard> cards = List.of();
   private int selected;
   private String selectedId;
   private boolean optionsOpen;
   private long clockSecond;
   private String clock = "";

   public ConsoleLibraryView(Screen owner, boolean servers, List<Button> actions, Consumer<Button> add,
                             Runnable worldsTab, Runnable serversTab) {
      this.owner = owner;
      this.servers = servers;
      this.actions = actions;
      for (int i = 0; i < 9; i++) {
         TileButton tile = new TileButton();
         this.tiles.add(tile);
         add.accept(tile);
      }
      for (Button action : actions) {
         add.accept(action);
      }
      this.more = Button.builder(Component.literal("•••"), button -> {
         this.optionsOpen = !this.optionsOpen;
         this.layoutActions();
         if (this.optionsOpen && this.actions.size() > 2) {
            this.owner.setFocused(this.actions.get(1));
         }
      }).size(26, 24).build();
      add.accept(this.more);
      this.previous = Button.builder(Component.literal("‹"), button -> this.move(-1)).size(20, 20).build();
      this.next = Button.builder(Component.literal("›"), button -> this.move(1)).size(20, 20).build();
      add.accept(this.previous);
      add.accept(this.next);
      Button worlds = new TabButton("Миры", !servers, worldsTab);
      Button multiplayer = new TabButton("Серверы", servers, serversTab);
      worlds.setPosition(this.layout().margin(), 12);
      multiplayer.setPosition(this.layout().margin() + 68, 12);
      add.accept(worlds);
      add.accept(multiplayer);
      this.layoutActions();
   }

   private LibraryLayout layout() { return new LibraryLayout(this.owner.width, this.owner.height); }

   public void setCards(List<LibraryCard> updated) {
      this.cards = List.copyOf(updated);
      int index = -1;
      if (this.selectedId != null) {
         for (int i = 0; i < cards.size(); i++) {
            if (this.selectedId.equals(cards.get(i).id())) { index = i; break; }
         }
      }
      this.select(index >= 0 ? index : Math.min(this.selected, cards.size() - 1), owner.getFocused() == null);
   }

   private void select(int index, boolean focus) {
      if (cards.isEmpty()) {
         this.selected = 0;
         this.layoutTiles();
         return;
      }
      this.selected = Math.clamp(index, 0, cards.size() - 1);
      LibraryCard card = cards.get(this.selected);
      this.selectedId = card.id();
      card.select();
      this.optionsOpen = false;
      this.layoutActions();
      this.layoutTiles();
      if (focus) {
         for (TileButton tile : tiles) {
            if (tile.index == this.selected && tile.visible) { owner.setFocused(tile); break; }
         }
      }
   }

   public String selectedId() { return this.selectedId; }

   public void restoreSelection(String id) { this.selectedId = id; }

   public boolean isVisible(String id) {
      for (TileButton tile : tiles) {
         if (tile.visible && tile.index < cards.size() && cards.get(tile.index).id().equals(id)) return true;
      }
      return false;
   }

   public void move(int amount) { this.select(this.selected + amount, true); }

   public boolean keyPressed(KeyEvent key) {
      if (key.key() == 256 && optionsOpen) {
         optionsOpen = false;
         layoutActions();
         owner.setFocused(more);
         return true;
      }
      if (!optionsOpen && (key.key() == 262 || key.key() == 263)) {
         move(key.key() == 262 ? 1 : -1);
         return true;
      }
      if (!optionsOpen && key.key() == 268) { select(0, true); return true; }
      if (!optionsOpen && key.key() == 269) { select(cards.size() - 1, true); return true; }
      return false;
   }

   public void scrolled(double amount) { if (amount != 0) move(amount > 0 ? -1 : 1); }

   public boolean dismissOptions(double x, double y) {
      if (!optionsOpen) return false;
      LibraryLayout l = layout();
      int popupY = l.actionY() - (actions.size() - 2) * 23 - 7;
      if (x >= l.heroX() && x <= l.heroX() + 170 && y >= popupY && y <= l.actionY() + 26) return false;
      optionsOpen = false;
      layoutActions();
      return true;
   }

   private void layoutActions() {
      LibraryLayout l = layout();
      Button primary = actions.getFirst();
      primary.setPosition(l.heroX(), l.actionY());
      primary.setWidth(l.actionWidth());
      primary.setHeight(25);
      primary.visible = true;
      Button back = actions.getLast();
      back.setPosition(owner.width - 66, owner.height - 25);
      back.setWidth(58);
      back.setHeight(18);
      back.visible = true;
      for (int i = 1; i < actions.size() - 1; i++) {
         Button action = actions.get(i);
         action.setPosition(l.heroX() + 5, l.actionY() - (actions.size() - 1 - i) * 23 - 3);
         action.setWidth(160);
         action.setHeight(20);
         action.visible = optionsOpen;
      }
      if (more != null) more.setPosition(l.heroX() + l.actionWidth() + 8, l.actionY());
      if (previous != null) previous.setPosition(owner.width - l.margin() - 46, l.rowY());
      if (next != null) next.setPosition(owner.width - l.margin() - 22, l.rowY());
   }

   private void layoutTiles() {
      LibraryLayout l = layout();
      // Reserve room for the arrow controls on the right edge.
      int capacity = l.capacity();
      int first = Math.clamp(selected - 1, 0, Math.max(0, cards.size() - capacity));
      int end = Math.min(cards.size(), first + capacity);
      // Keep the same widget bound to the same card while the visible window moves.
      // Screen.mouseClicked assigns focus after onPress; recycling that widget mid-click
      // would make Enter act on a different card.
      for (TileButton tile : tiles) {
         tile.visible = tile.index >= first && tile.index < end;
         tile.active = tile.visible && !optionsOpen;
      }
      int x = l.margin();
      for (int index = first; index < end; index++) {
         TileButton tile = null;
         for (TileButton candidate : tiles) {
            if (candidate.index == index) { tile = candidate; break; }
         }
         if (tile == null) {
            for (TileButton candidate : tiles) {
               if (!candidate.visible) { tile = candidate; break; }
            }
         }
         if (tile == null) break;
         tile.index = index;
         tile.visible = true;
         tile.active = !optionsOpen;
         int size = index == selected ? l.largeTile() : l.smallTile();
         tile.setPosition(x, l.rowY());
         tile.setWidth(size);
         tile.setHeight(size);
         tile.setMessage(Component.literal(cards.get(index).title()));
         x += size + l.gap();
      }
      previous.active = !optionsOpen && !cards.isEmpty() && selected > 0;
      next.active = !optionsOpen && !cards.isEmpty() && selected < cards.size() - 1;
   }

   public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      animation.beginFrame(Math.max(1, Minecraft.getInstance().getFps()));
      layoutActions();
      layoutTiles();
      LibraryLayout l = layout();
      LibraryArtwork.background(graphics, owner.width, owner.height, servers);
      long second = System.currentTimeMillis() / 1000;
      if (second != clockSecond) { clockSecond = second; clock = LocalTime.now().format(CLOCK); }
      RenderUtil.drawText(graphics, clock, owner.width - l.margin() - 28, 18, ConsoleStyle.TEXT, GFonts.MEDIUM, 9);
      LibraryCard card = cards.isEmpty() ? null : cards.get(selected);
      if (card != null) {
         TileButton tile = tiles.stream().filter(t -> t.visible && t.index == selected).findFirst().orElse(null);
         if (tile != null) {
            RenderUtil.drawTextFadeRight(graphics, card.title(), tile.getX() + tile.getWidth() + 8,
               l.rowY() + l.smallTile() + 8, ConsoleStyle.TEXT, GFonts.MEDIUM, 9,
               Math.max(10, owner.width - tile.getX() - tile.getWidth() - 24), 8);
         }
      }
      String title = card == null ? (servers ? "Добавь свой сервер" : "Выбери свой мир") : card.title();
      String description = card == null ? "Создание и другие действия — в меню •••" : card.description();
      RenderUtil.drawText(graphics, servers ? "HAVOC  /  MULTIPLAYER" : "MINECRAFT  /  ТВОЯ КОЛЛЕКЦИЯ",
         l.heroX(), l.heroY() - 15, ConsoleStyle.MUTED, GFonts.MEDIUM, 6.5F);
      RenderUtil.drawTextFadeRight(graphics, title, l.heroX(), l.heroY(), ConsoleStyle.TEXT, GFonts.BOLD,
         Math.min(30, owner.width / 17.0F), owner.width - l.heroX() * 2, 12);
      RenderUtil.drawTextFadeRight(graphics, description.replace('\n', ' '), l.heroX(), l.heroY() + 34,
         ConsoleStyle.MUTED, GFonts.MEDIUM, 8, owner.width - l.heroX() * 2, 10);
      if (card != null && owner.width >= 520) {
         float x = owner.width - l.margin() - 155;
         RenderUtil.drawRoundRect(graphics, x, l.actionY() - 2, 155, 32, 4, 0xC00B101A);
         RenderUtil.drawTextFadeRight(graphics, card.detail(), x + 10, l.actionY() + 8, ConsoleStyle.TEXT,
            GFonts.MEDIUM, 7, 135, 8);
      }
      String count = cards.isEmpty() ? "" : (selected + 1) + " / " + cards.size() + "   ·   ";
      RenderUtil.drawText(graphics, count + "← →  Выбор   Enter  Играть   Колесо  Листать", l.margin(), owner.height - 18,
         ConsoleStyle.MUTED, GFonts.MEDIUM, 5.5F);
      if (optionsOpen) {
         int popupHeight = (actions.size() - 2) * 23 + 10;
         RenderUtil.drawRoundRect(graphics, l.heroX(), l.actionY() - popupHeight, 170, popupHeight, 7, 0xFA101726);
      }
      // The host renders the actual widgets after the scene (including vanilla action tooltips).
   }

   private final class TileButton extends Button {
      private int index = -1;
      private final ConsoleStyle.FocusAnimation focus = new ConsoleStyle.FocusAnimation();

      private TileButton() {
         super(0, 0, 1, 1, Component.empty(), button -> { }, DEFAULT_NARRATION);
      }

      @Override
      public void onPress(net.minecraft.client.input.InputWithModifiers input) {
         if (index >= cards.size()) return;
         if (index == selected && actions.getFirst().active) {
            actions.getFirst().onPress(input);
         } else {
            select(index, false);
         }
      }

      @Override
      protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
         if (index >= cards.size()) return;
         LibraryCard card = cards.get(index);
         boolean chosen = index == selected;
         float f = focus.update(chosen || isHoveredOrFocused());
         Identifier image = card.image();
         RenderUtil.drawImage(graphics, image == null ? (servers ? LibraryArtwork.SERVERS : LibraryArtwork.WORLDS) : image,
            getX(), getY(), getWidth(), getHeight(), 6, -1, 1);
         RenderUtil.drawRoundRectGradientQuad(graphics, getX(), getY(), getWidth(), getHeight(), 6,
            0x05080C14, 0xDD080C14, 0xDD080C14, 0x05080C14);
         RenderUtil.drawTextFadeRight(graphics, card.title(), getX() + 4, getY() + getHeight() - 13,
            ConsoleStyle.TEXT, GFonts.SEMIBOLD, chosen ? 6.5F : 5.5F, getWidth() - 8, 4);
         RenderUtil.drawBorder(graphics, getX() - 2, getY() - 2, getWidth() + 4, getHeight() + 4, 8,
            ColorUtil.multiplyAlpha(0xFFDFE7F4, f * 0.9F), 1, 0, 0.5F);
      }
   }

   private static final class TabButton extends Button {
      private final boolean selected;
      private TabButton(String title, boolean selected, Runnable action) {
         super(0, 0, 62, 20, Component.literal(title), button -> action.run(), DEFAULT_NARRATION);
         this.selected = selected;
      }
      @Override
      protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
         RenderUtil.drawText(graphics, getMessage().getString(), getX(), getY() + 4,
            selected || isHoveredOrFocused() ? ConsoleStyle.TEXT : ConsoleStyle.MUTED, selected ? GFonts.SEMIBOLD : GFonts.MEDIUM, 10);
         if (isFocused()) RenderUtil.drawRoundRect(graphics, getX(), getY() + 18, 44, 1, 0.5F, ConsoleStyle.TEXT);
      }
   }
}
