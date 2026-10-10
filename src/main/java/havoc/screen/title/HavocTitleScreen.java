package havoc.screen.title;

import com.mojang.blaze3d.textures.GpuTextureView;
import havoc.account.AccountManager;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.screen.menu.UiAnimationEngine;
import havoc.screen.style.ConsoleStyle;
import havoc.screen.library.LibraryLayout;
import havoc.screen.library.LibraryArtwork;
import havoc.screen.library.CarouselMotion;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import havoc.utilites.animation.AnimatedValue;
import havoc.utilites.color.ClientTheme;
import havoc.utilites.color.ColorUtil;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;

public final class HavocTitleScreen extends TitleScreen {
   private static final float PANEL_W = 186.0F;
   private static final String[] TITLES = {"MINECRAFT", "MINECRAFT", "Аккаунты", "Настройки", "Завершение игры"};
   private static final String[] LABELS = {"Одиночная игра", "Мультиплеер", "Аккаунты", "Настройки", "Выход"};
   private static final String[] DESCRIPTIONS = {
      "Мир, в который хочется вернуться.",
      "Любимые серверы. Новые приключения.",
      "Выбирай аккаунт и продолжай свою историю.",
      "Графика, звук и управление — так, как удобно тебе.",
      "Заверши игру. Твои миры будут ждать тебя."
   };
   private static final String[] ACTIONS = {"Играть", "Выбрать сервер", "Сменить аккаунт", "Открыть настройки", "Выйти из игры"};
   private static final String[] ICONS = {"computer", "wifi", "console_profile", "settings", "console_power"};
   private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
   private final List<Button> controls = new ArrayList<>();
   private final List<Button> tiles = new ArrayList<>();
   private Button actionControl;
   private Button moreControl;
   private Button collectionControl;
   private final List<Button> quickActions = new ArrayList<>();
   private boolean quickActionsOpen;
   private int selectedTile;
   private int accountOffset;
   private long clockSecond = -1;
   private String clockText = "";
   private static final float ROW_H = 24.0F;
   private final UiAnimationEngine animation = new UiAnimationEngine();
   private final AnimatedValue accountsAnim = new AnimatedValue();
   private final Map<String, GpuTextureView> headCache = new HashMap<>();
   private final Set<String> headRequests = new HashSet<>();
   private boolean accountsOpen;
   private String accountInput = "";
   private boolean inputFocused;
   private String selectedAccount;

   public HavocTitleScreen() {
      super();
      this.clearWidgets();
   }

   @Override
   public void tick() {
   }

   @Override
   protected void init() {
      this.clearWidgets();
      this.controls.clear();
      this.tiles.clear();
      this.quickActions.clear();
      this.quickActionsOpen = false;
      for (int i = 0; i < LABELS.length; i++) {
         final int index = i;
         Button tile = new ConsoleControl(Math.round(this.tileX(i)), Math.round(this.tileY()),
            Math.round(this.cardSize(i)), Math.round(this.cardSize(i)), LABELS[i], () -> {
               if (this.selectedTile == index) {
                  this.activateSelected();
               } else {
                  this.selectTile(index);
               }
            }, index);
         tile.setTooltip(Tooltip.create(Component.literal(LABELS[i])));
         this.tiles.add(tile);
         this.controls.add(this.addRenderableWidget(tile));
      }
      this.actionControl = new ConsoleControl(Math.round(this.margin()), Math.round(this.actionY()),
         this.consoleLayout().actionWidth(), 25, ACTIONS[this.selectedTile], this::activateSelected, -1);
      this.controls.add(this.addRenderableWidget(this.actionControl));
      this.controls.add(this.addRenderableWidget(new ConsoleControl(this.width - 56, 12, 20, 20, "Язык",
         () -> this.openScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager())), -1)));
      this.controls.add(this.addRenderableWidget(new ConsoleControl(this.width - 30, 12, 20, 20, "Специальные возможности",
         () -> this.openScreen(new AccessibilityOptionsScreen(this, this.minecraft.options)), -1)));
      this.moreControl = new ConsoleControl(Math.round(this.margin()) + this.consoleLayout().actionWidth() + 8,
         Math.round(this.actionY()), 25, 25, "Дополнительные действия", this::toggleQuickActions, -1);
      this.controls.add(this.addRenderableWidget(this.moreControl));
      for (int i = 0; i < 3; i++) {
         final int section = i + 2;
         Button button = new ConsoleControl(Math.round(this.margin()) + 4, Math.round(this.actionY()) - 78 + i * 24,
            144, 21, LABELS[section], () -> {
               this.quickActionsOpen = false;
               this.selectTile(section);
               this.updateControlState();
               this.activateSelected();
            }, -1);
         this.quickActions.add(button);
         this.controls.add(this.addRenderableWidget(button));
      }
      this.collectionControl = new ConsoleControl(this.consoleLayout().margin() + 44, 12, 68, 20, "Коллекция миров",
         () -> this.openScreen(new SelectWorldScreen(this)), -1);
      this.controls.add(this.addRenderableWidget(this.collectionControl));
      this.setFocused(this.tiles.get(this.selectedTile));
      this.updateControlState();
   }

   private LibraryLayout consoleLayout() { return new LibraryLayout(this.width, this.height); }
   private float margin() { return this.consoleLayout().heroX(); }
   private float cardSize(int index) { return index == this.selectedTile ? this.consoleLayout().largeTile() : this.consoleLayout().smallTile(); }
   private float tileX(int index) {
      float x = this.consoleLayout().margin();
      for (int i = 0; i < index; i++) x += this.cardSize(i) + this.consoleLayout().gap();
      return x;
   }
   private float tileY() { return this.consoleLayout().rowY(); }
   private float heroY() { return this.consoleLayout().heroY(); }
   private float actionY() { return this.consoleLayout().actionY(); }

   private void selectTile(int index) {
      this.selectedTile = Math.floorMod(index, LABELS.length);
      if (this.actionControl != null) {
         this.actionControl.setMessage(Component.literal(ACTIONS[this.selectedTile]));
      }
   }

   private void updateControlState() {
      for (Button control : this.controls) {
         control.active = !this.accountsOpen && (!this.quickActionsOpen || this.quickActions.contains(control) || control == this.moreControl);
      }
      for (Button button : this.quickActions) {
         button.visible = this.quickActionsOpen;
      }
      if (!this.accountsOpen && !this.quickActionsOpen && this.quickActions.contains(this.getFocused())) {
         this.setFocused(this.tiles.get(this.selectedTile));
      }
   }

   private void toggleQuickActions() {
      this.quickActionsOpen = !this.quickActionsOpen;
      this.updateControlState();
      this.setFocused(this.quickActionsOpen ? this.quickActions.getFirst() : this.moreControl);
   }

   private void updateTileBounds() {
      long now = System.nanoTime();
      for (int i = 0; i < this.tiles.size(); i++) {
         ConsoleControl tile = (ConsoleControl)this.tiles.get(i);
         tile.motion.target(this.tileX(i), this.tileY(), this.cardSize(i));
         tile.motion.update(now);
         tile.setPosition(tile.motion.x(), tile.motion.y());
         tile.setWidth(tile.motion.size());
         tile.setHeight(tile.motion.size());
      }
   }

   private void activateSelected() {
      switch (this.selectedTile) {
         case 0 -> this.openScreen(new SelectWorldScreen(this));
         case 1 -> this.openScreen(new JoinMultiplayerScreen(this));
         case 2 -> {
            this.accountsOpen = true;
            this.accountsAnim.setValue(0.0F);
            this.selectedAccount = null;
            this.inputFocused = false;
            this.accountOffset = 0;
            this.updateControlState();
         }
         case 3 -> this.openScreen(new OptionsScreen(this, this.minecraft.options, false));
         case 4 -> this.minecraft.stop();
         default -> { }
      }
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      ClientTheme.updateDynamicColors();
      this.animation.beginFrame(Math.max(1, this.minecraft.getFps()));
      float reveal = this.animation.animate01("title:reveal", 1.0F, 9.0F, 0.0F);
      this.drawBackground(context, reveal);
      this.updateTileBounds();
      // Real widgets retain focus, narration, click sounds and vanilla Tab navigation.
      for (Button control : this.controls) {
         control.extractRenderState(context, mouseX, mouseY, delta);
      }
      float left = this.margin();
      float headerX = this.consoleLayout().margin();
      RenderUtil.drawText(context, "Игры", headerX, 18.0F, ConsoleStyle.TEXT, GFonts.SEMIBOLD, 10.0F);
      RenderUtil.drawText(context, "Коллекция", headerX + 44, 19, this.collectionControl.isHoveredOrFocused() ? ConsoleStyle.TEXT : ConsoleStyle.MUTED, GFonts.MEDIUM, 8);
      long second = System.currentTimeMillis() / 1000L;
      if (second != this.clockSecond) {
         this.clockSecond = second;
         this.clockText = LocalTime.now().format(CLOCK);
      }
      RenderUtil.drawText(context, this.clockText, this.width - 91, 19, ConsoleStyle.TEXT, GFonts.MEDIUM, 8);
      for (int i = 0; i < LABELS.length; i++) {
         this.drawTile(context, i, reveal);
      }
      float hero = this.heroY();
      RenderUtil.drawText(context, "H A V O C", left, hero - 14, ConsoleStyle.MUTED, GFonts.MEDIUM, 6);
      RenderUtil.drawTextFadeRight(context, TITLES[this.selectedTile], left, hero,
         ColorUtil.multiplyAlpha(ConsoleStyle.TEXT, reveal), GFonts.BOLD, this.consoleLayout().heroFontSize(), this.consoleLayout().heroWidth(), 10);
      RenderUtil.drawTextFadeRight(context, this.selectedTile < 2 ? LABELS[this.selectedTile] : "Havoc Client", left, hero + 27,
         ConsoleStyle.TEXT, GFonts.MEDIUM, 10, this.consoleLayout().heroWidth(), 8);
      if (this.consoleLayout().showDescription()) {
         RenderUtil.drawTextFadeRight(context, DESCRIPTIONS[this.selectedTile], left, hero + 52,
            ConsoleStyle.MUTED, GFonts.MEDIUM, 7.5F, this.consoleLayout().heroWidth(), 10);
      }
      this.drawButton(context, ACTIONS[this.selectedTile], left, this.actionY(), this.consoleLayout().actionWidth(), 25, true,
         mouseX, mouseY, "title:action", 0, this.actionControl.isFocused());
      this.drawButton(context, "•••", this.moreControl.getX(), this.moreControl.getY(), 25, 25, false,
         mouseX, mouseY, "title:more", 0, this.moreControl.isFocused() || this.quickActionsOpen);
      if (this.quickActionsOpen) {
         RenderUtil.drawRoundRect(context, left, this.actionY() - 83, 152, 79, 7, 0xFA101726);
         for (int i = 0; i < this.quickActions.size(); i++) {
            Button button = this.quickActions.get(i);
            this.drawButton(context, button.getMessage().getString(), button.getX(), button.getY(), button.getWidth(), button.getHeight(),
               false, mouseX, mouseY, "title:quick:" + i, 0, button.isFocused());
         }
      }
      this.drawCornerIcons(context, mouseX, mouseY);
      this.drawFooter(context, reveal);
      if (this.accountsOpen) {
         RenderUtil.drawRoundRect(context, 0, 0, this.width, this.height, 0, 0xB008101D);
         this.drawAccountsPanel(context, 0, mouseX, mouseY);
      }
   }

   private void drawBackground(GuiGraphicsExtractor context, float alpha) {
      float serverMix = this.animation.animate01("title:background", this.selectedTile == 1 ? 1 : 0, 9, this.selectedTile == 1 ? 1 : 0);
      LibraryArtwork.background(context, this.width, this.height, serverMix);
      RenderUtil.drawRoundRect(context, 0, 0, this.width, this.height, 0, ColorUtil.multiplyAlpha(0xFF050A13, 1 - alpha));
   }

   private void drawTile(GuiGraphicsExtractor context, int index, float alpha) {
      Button tile = this.tiles.get(index);
      float focus = this.animation.animate01("title:tile:" + index, this.selectedTile == index ? 1 : 0, 14, 0);
      float hover = this.animation.animate01("title:tile:hover:" + index, !this.accountsOpen && tile.isHoveredOrFocused() ? 1 : 0, 16, 0);
      float x = tile.getX();
      float y = tile.getY();
      float size = tile.getWidth();
      if (index < 2) {
         RenderUtil.drawImage(context, LibraryArtwork.cover(index == 1), x, y, size, size, 6, ColorUtil.multiplyAlpha(-1, alpha), 1);
      } else {
         RenderUtil.drawRoundRectGradientQuad(context, x, y, size, size, 6,
            ColorUtil.multiplyAlpha(0xFF1B2535, alpha), ColorUtil.multiplyAlpha(0xFF0D1420, alpha),
            ColorUtil.multiplyAlpha(0xFF0D1420, alpha), ColorUtil.multiplyAlpha(0xFF1B2535, alpha));
         float icon = size * 0.37F;
         RenderUtil.drawImage(context, "textures/gui/icons/" + ICONS[index] + ".png", x + (size - icon) / 2,
            y + (size - icon) / 2, icon, icon, 0, ColorUtil.multiplyAlpha(ConsoleStyle.TEXT, alpha), 1);
      }
      RenderUtil.drawBorder(context, x - 2, y - 2, size + 4, size + 4, 8,
         ColorUtil.multiplyAlpha(0xFFDCE6F4, alpha * Math.max(focus * 0.9F, hover * 0.35F)), 1, 0, 0.5F);
      if (this.selectedTile == index) {
         float labelY = y + this.consoleLayout().smallTile() + 8;
         for (int i = index + 1; i < this.tiles.size(); i++) {
            labelY = Math.max(labelY, this.tiles.get(i).getY() + this.tiles.get(i).getHeight() + 8);
         }
         RenderUtil.drawTextFadeRight(context, LABELS[index], x + size + 8,
            labelY, ColorUtil.multiplyAlpha(ConsoleStyle.TEXT, alpha),
            GFonts.MEDIUM, 9, this.width - x - size - 24, 8);
      }
   }

   private void drawButton(GuiGraphicsExtractor context, String label, float x, float y, float w, float h,
                           boolean primary, int mouseX, int mouseY, String key, float stagger, boolean stickyActive) {
      boolean hover = this.inRect(mouseX, mouseY, x, y, w, h);
      float focus = this.animation.animate01(key + ":hover", hover || stickyActive ? 1 : 0, 16, 0);
      ConsoleStyle.button(context, x, y, w, h, true, focus, 1, false);
      float size = h >= 24 ? 8 : 7;
      this.drawCenteredText(context, label, x + w / 2, y + (h - size) / 2, ConsoleStyle.TEXT, size, true);
   }

   private final class ConsoleControl extends Button {
      private final int tileIndex;
      private final CarouselMotion motion = new CarouselMotion();

      private ConsoleControl(int x, int y, int w, int h, String label, Runnable action, int tileIndex) {
         super(x, y, w, h, Component.literal(label), button -> action.run(), DEFAULT_NARRATION);
         this.tileIndex = tileIndex;
         this.motion.snap(x, y, w);
      }

      @Override
      protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
         // Drawn by the title screen after all widget hover states are updated.
      }

      @Override
      public void setFocused(boolean focused) {
         super.setFocused(focused);
         if (focused && this.tileIndex >= 0) {
            HavocTitleScreen.this.selectTile(this.tileIndex);
         }
      }
   }

   private void drawCenteredText(GuiGraphicsExtractor context, String text, float cx, float y, int color, float size, boolean bold) {
      GFonts font = bold ? GFonts.SEMIBOLD : GFonts.MEDIUM;
      RenderUtil.drawText(context, text, cx - RenderUtil.getTextWidth(text, font, size) / 2.0F, y, color, font, size);
   }

   private void drawCornerIcons(GuiGraphicsExtractor context, int mouseX, int mouseY) {
      this.drawButton(context, "Яз", this.width - 56, 12, 20, 20, false, mouseX, mouseY, "title:lang", 0,
         this.controls.get(6).isFocused());
      this.drawButton(context, "Аа", this.width - 30, 12, 20, 20, false, mouseX, mouseY, "title:a11y", 0,
         this.controls.get(7).isFocused());
   }

   private void drawFooter(GuiGraphicsExtractor context, float alpha) {
      RenderUtil.drawText(context, "← →  Выбор    Enter  Открыть    Tab  Навигация", this.margin(), this.height - 19,
         ColorUtil.multiplyAlpha(ConsoleStyle.MUTED, alpha), GFonts.MEDIUM, 6);
      if (this.width > 450) {
         String name = AccountManager.activeName();
         RenderUtil.drawTextFadeRight(context, name == null ? "Havoc" : name, this.width - 120, this.height - 19,
            ConsoleStyle.TEXT, GFonts.MEDIUM, 6, 100, 8);
      }
   }

   private float panelHeight() { return Math.min(274.0F, this.height - 20.0F); }
   private float panelX() { return (this.width - PANEL_W) / 2; }
   private float panelY(float ignored) { return (this.height - this.panelHeight()) / 2; }
   private int visibleAccountRows() { return Math.max(1, (int)((this.panelHeight() - 138) / (ROW_H + 3))); }

   private void drawAccountsPanel(GuiGraphicsExtractor context, float colTop, int mouseX, int mouseY) {
      this.accountOffset = Math.clamp(this.accountOffset, 0, Math.max(0, AccountManager.get().size() - this.visibleAccountRows()));
      float anim = this.accountsAnim.update(1.0F, 0.09F, System.nanoTime());
      float a = this.smoothStep(anim);
      float x = this.panelX();
      float y = this.panelY(colTop);
      RenderUtil.drawRoundRect(context, x, y, PANEL_W, this.panelHeight(), 9, ColorUtil.multiplyAlpha(0xFF101A2B, a));
      RenderUtil.drawShadow(context, x, y, PANEL_W, this.panelHeight(), 5.0F, ColorUtil.multiplyAlpha(0xFF050A13, 0.5F * a), 1.0F, 12.0F, 0.0F, 6.0F);
      RenderUtil.drawBorder(context, x, y, PANEL_W, this.panelHeight(), 5.0F, ColorUtil.multiplyAlpha(0xFF52617A, a * 0.7F), 1.0F, 0.0F, 0.5F);
      float px = x + 10.0F;
      float pw = PANEL_W - 20.0F;
      float cy = y + 10.0F;
      this.drawCenteredText(context, "Аккаунты", x + PANEL_W / 2.0F, cy, ColorUtil.multiplyAlpha(ConsoleStyle.TEXT, a), 8.0F, true);
      this.drawButton(context, "×", x + PANEL_W - 24, y + 7, 16, 16, false, mouseX, mouseY, "title:close", 0, false);
      cy += 18.0F;
      String active = AccountManager.activeName();
      this.drawHead(context, active, px, cy, 22.0F, a);
      RenderUtil.drawText(context, "текущий аккаунт", px + 28.0F, cy + 2.0F, ColorUtil.multiplyAlpha(ConsoleStyle.MUTED, a * 0.9F), GFonts.MEDIUM, 5.5F);
      RenderUtil.drawText(
         context, active == null ? "—" : active, px + 28.0F, cy + 10.0F, ColorUtil.multiplyAlpha(ConsoleStyle.TEXT, a), GFonts.SEMIBOLD, 7.5F
      );
      cy += 30.0F;
      this.layoutInputY = cy;
      float addW = 16.0F;
      boolean inputHover = this.inRect(mouseX, mouseY, px, cy, pw - addW - 4.0F, 16.0F);
      float focusMix = this.animation.animate01("title:input", this.inputFocused ? 1.0F : inputHover ? 0.45F : 0.0F, 16.0F, 0.0F);
      RenderUtil.drawRoundRect(context, px, cy, pw - addW - 4.0F, 16.0F, 3.0F, ColorUtil.multiplyAlpha(0xFF19263C, a * 0.95F));
      RenderUtil.drawBorder(
         context,
         px,
         cy,
         pw - addW - 4.0F,
         16.0F,
         3.0F,
         ColorUtil.multiplyAlpha(ColorUtil.interpolate(0xFF52617A, 0xFFB6D7FF, focusMix), a),
         1.0F,
         0.0F,
         0.5F
      );
      boolean placeholder = this.accountInput.isEmpty() && !this.inputFocused;
      String display = placeholder ? "имя аккаунта" : this.accountInput;
      int displayColor = ColorUtil.multiplyAlpha(placeholder ? ConsoleStyle.MUTED : ConsoleStyle.TEXT, a);
      RenderUtil.drawText(context, display, px + 6.0F, cy + 4.5F, displayColor, GFonts.MEDIUM, 6.5F);
      if (this.inputFocused && (System.currentTimeMillis() / 520L & 1L) == 0L) {
         float caretX = px + 6.5F + RenderUtil.getTextWidth(this.accountInput, GFonts.MEDIUM, 6.5F);
         RenderUtil.drawRoundRect(context, caretX, cy + 3.5F, 1.0F, 9.0F, 0.5F, ColorUtil.multiplyAlpha(ConsoleStyle.TEXT, a));
      }

      this.drawButton(context, "+", px + pw - addW, cy, addW, 16.0F, true, mouseX, mouseY, "title:add", 0.0F, false);
      cy += 24.0F;
      RenderUtil.drawText(context, "СОХРАНЁННЫЕ  ·  ПРОКРУТКА КОЛЕСОМ", px, cy, ColorUtil.multiplyAlpha(ConsoleStyle.MUTED, a * 0.7F), GFonts.MEDIUM, 5.5F);
      cy += 10.0F;
      this.layoutListY = cy;
      int idx = 0;

      for (AccountManager.Stored stored : AccountManager.get().stream().skip(this.accountOffset).toList()) {
         if (idx >= this.visibleAccountRows()) {
            break;
         }

         this.drawAccountRow(context, stored, px, cy, pw, a, mouseX, mouseY, active, idx);
         cy += ROW_H + 3.0F;
         idx++;
      }

      if (idx == 0) {
         RenderUtil.drawText(
            context, "Список пуст — добавь ник выше", px + 2.0F, cy + 5.0F, ColorUtil.multiplyAlpha(ConsoleStyle.MUTED, a * 0.8F), GFonts.MEDIUM, 6.5F
         );
      }

      float actionW = (pw - 4.0F) / 2.0F;
      float actionY = y + this.panelHeight() - 26.0F;
      this.layoutActionY = actionY;
      this.layoutActionW = actionW;
      this.drawButton(
         context,
         "Использовать",
         px,
         actionY,
         actionW,
         16.0F,
         this.selectedAccount != null && !this.selectedAccount.equalsIgnoreCase(active),
         mouseX,
         mouseY,
         "title:switch",
         0.0F,
         false
      );
      this.drawButton(context, "Удалить", px + actionW + 4.0F, actionY, actionW, 16.0F, false, mouseX, mouseY, "title:del", 0.0F, false);
   }

   private float layoutInputY;
   private float layoutListY;
   private float layoutActionY;
   private float layoutActionW;

   private void drawAccountRow(
      GuiGraphicsExtractor context,
      AccountManager.Stored stored,
      float px,
      float y,
      float pw,
      float a,
      int mouseX,
      int mouseY,
      String active,
      int index
   ) {
      boolean activeRow = stored.name.equalsIgnoreCase(active);
      boolean selected = this.selectedAccount != null && this.selectedAccount.equalsIgnoreCase(stored.name);
      boolean hovered = this.inRect(mouseX, mouseY, px, y, pw, ROW_H);
      float hover = this.animation.animate01("title:row:" + stored.name, hovered ? 1.0F : 0.0F, 16.0F, 0.0F);
      int rowBg = ColorUtil.interpolate(0xFF101A2B, selected ? 0xFFB6D7FF : 0xFF25354E, 0.35F + hover * 0.25F);
      RenderUtil.drawRoundRect(context, px, y, pw, ROW_H, 3.5F, ColorUtil.multiplyAlpha(rowBg, a * 0.9F));
      if (selected) {
         RenderUtil.drawBorder(context, px, y, pw, ROW_H, 3.5F, ColorUtil.multiplyAlpha(0xFFB6D7FF, a * 0.75F), 1.0F, 0.0F, 0.5F);
      }

      this.drawHead(context, stored.name, px + 4.0F, y + 4.0F, 16.0F, a);
      int nameColor = ColorUtil.multiplyAlpha(activeRow ? 0xFFB6D7FF : ConsoleStyle.TEXT, a);
      RenderUtil.drawText(context, stored.name, px + 26.0F, y + 3.5F, nameColor, GFonts.SEMIBOLD, 7.0F);
      String sub = activeRow ? "используется сейчас" : "добавлен " + formatDate(stored.addedAt);
      RenderUtil.drawText(context, sub, px + 26.0F, y + 12.5F, ColorUtil.multiplyAlpha(ConsoleStyle.MUTED, a * 0.85F), GFonts.MEDIUM, 5.5F);
   }

   private void drawHead(GuiGraphicsExtractor context, String name, float x, float y, float size, float alpha) {
      GpuTextureView view = this.resolveHead(name);
      int color = ColorUtil.multiplyAlpha(ColorUtil.rgba(255, 255, 255, 255), alpha);
      if (view != null) {
         RenderUtil.drawHeadImageRegion(context, view, x, y, size, size, 8.0F, 8.0F, 8.0F, 8.0F, 3.0F, color, 1.0F);
         RenderUtil.drawHeadImageRegion(context, view, x, y, size, size, 40.0F, 8.0F, 8.0F, 8.0F, 3.0F, color, 1.0F);
      } else {
         RenderUtil.drawRoundRect(context, x, y, size, size, 3.0F, ColorUtil.multiplyAlpha(0xFF25354E, alpha * 0.8F));
      }
   }

   private GpuTextureView resolveHead(String name) {
      if (this.minecraft == null || name == null) {
         return null;
      } else {
         GpuTextureView cached = this.headCache.get(name);
         if (cached == null) {
            try {
               PlayerSkin skin = DefaultPlayerSkin.get(AccountManager.offlineUuid(name));
               if (skin != null && skin.body() != null) {
                  cached = this.minecraft.getTextureManager().getTexture(skin.body().texturePath()).getTextureView();
                  this.headCache.put(name, cached);
               }
            } catch (Exception ignored) {
            }
         }

         if (this.headRequests.add(name)) {
            UUID uuid = AccountManager.offlineUuid(name);
            Minecraft mc = this.minecraft;

            try {
               mc.getSkinManager()
                  .get(new GameProfile(uuid, name))
                  .thenAccept(optional -> mc.execute(
                     () -> optional.ifPresent(playerSkin -> {
                        try {
                           this.headCache.put(name, mc.getTextureManager().getTexture(playerSkin.body().texturePath()).getTextureView());
                        } catch (Exception ignored) {
                        }
                     })
                  ));
            } catch (Exception ignored) {
            }
         }

         return cached;
      }
   }

   private static String formatDate(long ms) {
      return ms <= 0L ? "недавно" : Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().toString();
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      if (this.accountsOpen) {
         if (click.button() == 0) {
            this.handleAccountsClick(click.x(), click.y());
            this.updateControlState();
         }
         return true;
      }
      if (this.quickActionsOpen && !this.inRect(click.x(), click.y(), this.margin(), this.actionY() - 83,
            Math.max(152, this.consoleLayout().actionWidth() + 33), 110)) {
         this.toggleQuickActions();
         return true;
      }
      return super.mouseClicked(click, doubled);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.accountsOpen) {
         this.accountOffset = Math.clamp(this.accountOffset - (int)Math.signum(verticalAmount), 0,
            Math.max(0, AccountManager.get().size() - this.visibleAccountRows()));
         return true;
      }
      return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
   }

   private void openScreen(Screen screen) {
      if (this.minecraft != null) {
         this.minecraft.gui.setScreen(screen);
      }
   }

   private boolean handleAccountsClick(double mx, double my) {
      float x = this.panelX();
      float y = this.panelY(0);
      if (this.inRect(mx, my, x, y, PANEL_W, this.panelHeight())) {
         float px = x + 10.0F;
         if (this.inRect(mx, my, x + PANEL_W - 24, y + 7, 16, 16)) {
            this.accountsOpen = false;
            this.inputFocused = false;
            return true;
         }
         float pw = PANEL_W - 20.0F;
         float addW = 16.0F;
         this.inputFocused = this.inRect(mx, my, px, this.layoutInputY, pw - addW - 4.0F, 16.0F);
         if (this.inRect(mx, my, px + pw - addW, this.layoutInputY, addW, 16.0F)) {
            this.tryAddAccount();
            return true;
         }

         int idx = 0;

         for (AccountManager.Stored stored : AccountManager.get().stream().skip(this.accountOffset).toList()) {
            if (idx >= this.visibleAccountRows()) {
               break;
            }

            float rowY = this.layoutListY + idx * (ROW_H + 3.0F);
            if (this.inRect(mx, my, px, rowY, pw, ROW_H)) {
               this.selectedAccount = stored.name;
               return true;
            }

            idx++;
         }

         if (this.inRect(mx, my, px, this.layoutActionY, this.layoutActionW, 16.0F)) {
            if (this.selectedAccount != null) {
               AccountManager.apply(this.selectedAccount);
               this.headCache.clear();
            }

            return true;
         }

         if (this.inRect(mx, my, px + this.layoutActionW + 4.0F, this.layoutActionY, this.layoutActionW, 16.0F)) {
            if (this.selectedAccount != null) {
               AccountManager.remove(this.selectedAccount);
               this.selectedAccount = null;
            }

            return true;
         }

         return true;
      } else {
         this.accountsOpen = false;
         this.inputFocused = false;
         return true;
      }
   }

   private void tryAddAccount() {
      String name = AccountManager.sanitize(this.accountInput);
      if (name != null) {
         AccountManager.add(name);
         this.accountInput = "";
         this.inputFocused = false;
         this.selectedAccount = name;
      }
   }

   @Override
   public boolean keyPressed(KeyEvent key) {
      if (this.quickActionsOpen) {
         if (key.key() == 256) { this.toggleQuickActions(); return true; }
         return super.keyPressed(key);
      }
      if (this.accountsOpen) {
         if (this.inputFocused) {
            if (key.key() == 256) {
               this.inputFocused = false;
               return true;
            }

            if (key.key() == 257 || key.key() == 335) {
               this.tryAddAccount();
               return true;
            }

            if (key.key() == 259 && !this.accountInput.isEmpty()) {
               this.accountInput = this.accountInput.substring(0, this.accountInput.length() - 1);
               return true;
            }

            return true;
         }

         if (key.key() == 256) {
            this.accountsOpen = false;
            this.updateControlState();
            return true;
         }
      }

      if (this.accountsOpen) {
         return true;
      }
      if (key.key() == 262 || key.key() == 263) {
         this.selectTile(this.selectedTile + (key.key() == 262 ? 1 : -1));
         this.setFocused(this.tiles.get(this.selectedTile));
         return true;
      }
      return super.keyPressed(key);
   }

   @Override
   public boolean charTyped(CharacterEvent input) {
      if (this.accountsOpen && this.inputFocused) {
         int codePoint = input.codepoint();
         if (codePoint >= 32 && codePoint != 127 && this.accountInput.length() < 16) {
            String ch = new String(Character.toChars(codePoint));
            if (ch.matches("[A-Za-z0-9_]")) {
               this.accountInput = this.accountInput + ch;
            }
         }

         return true;
      } else {
         return super.charTyped(input);
      }
   }

   private boolean inRect(double mx, double my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }

   private float smoothStep(float v) {
      float x = Math.clamp(v, 0.0F, 1.0F);
      return x * x * (3.0F - 2.0F * x);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
