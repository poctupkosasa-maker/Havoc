package havoc.screen.title;

import com.mojang.blaze3d.textures.GpuTextureView;
import havoc.account.AccountManager;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.screen.menu.UiAnimationEngine;
import havoc.screen.screen.HudVisualStyle;
import havoc.utilites.animation.AnimatedValue;
import havoc.utilites.color.ClientColors;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

public final class HavocTitleScreen extends TitleScreen {
   private static final float BTN_W = 176.0F;
   private static final float BTN_H = 16.0F;
   private static final float BTN_GAP = 5.0F;
   private static final float PANEL_W = 186.0F;
   private static final float PANEL_H = 214.0F;
   private static final float ROW_H = 24.0F;
   private final UiAnimationEngine animation = new UiAnimationEngine();
   private final AnimatedValue accountsAnim = new AnimatedValue();
   private final Map<String, GpuTextureView> headCache = new HashMap<>();
   private final Set<String> headRequests = new HashSet<>();
   private final float[] iconRects = new float[4];
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
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      ClientTheme.updateDynamicColors();
      int fps = this.minecraft != null ? Math.max(1, this.minecraft.getFps()) : 60;
      this.animation.beginFrame(fps);
      float reveal = this.animation.animate01("title:reveal", 1.0F, 10.0F, 0.0F);
      float eased = HudVisualStyle.easeOutCubic(reveal);
      this.drawBackground(context, eased);
      float cx = this.width / 2.0F;
      float colTop = this.height / 2.0F - (5 * BTN_H + 4 * BTN_GAP) / 2.0F + 26.0F;
      this.drawLogo(context, cx, colTop - 58.0F, eased);
      this.drawVersionTag(context, cx, colTop - 20.0F, eased);
      float y = colTop + (1.0F - eased) * 10.0F;
      this.drawButton(context, "Играть", cx - BTN_W / 2.0F, y, BTN_W, BTN_H, true, mouseX, mouseY, "title:play", 0.0F, false);
      y += BTN_H + BTN_GAP;
      this.drawButton(context, "Мультиплеер", cx - BTN_W / 2.0F, y, BTN_W, BTN_H, false, mouseX, mouseY, "title:mp", 0.12F, false);
      y += BTN_H + BTN_GAP;
      this.drawButton(context, "Аккаунты", cx - BTN_W / 2.0F, y, BTN_W, BTN_H, false, mouseX, mouseY, "title:acc", 0.24F, this.accountsOpen);
      y += BTN_H + BTN_GAP;
      this.drawButton(context, "Настройки", cx - BTN_W / 2.0F, y, BTN_W, BTN_H, false, mouseX, mouseY, "title:options", 0.36F, false);
      y += BTN_H + BTN_GAP;
      this.drawButton(context, "Выйти", cx - BTN_W / 2.0F, y, BTN_W, BTN_H, false, mouseX, mouseY, "title:quit", 0.48F, false);
      this.drawCornerIcons(context, mouseX, mouseY);
      this.drawFooter(context, eased);
      if (this.accountsOpen) {
         this.drawAccountsPanel(context, colTop, mouseX, mouseY);
      }
   }

   private void drawBackground(GuiGraphicsExtractor context, float alpha) {
      boolean hasImage = false;
      if (this.minecraft != null) {
         try {
            hasImage = this.minecraft
               .getResourceManager()
               .getResource(Identifier.fromNamespaceAndPath("havoc", "textures/gui/menu_background.png"))
               .isPresent();
         } catch (Exception ignored) {
         }
      }

      if (hasImage) {
         RenderUtil.drawImage(
            context, "textures/gui/menu_background.png", 0.0F, 0.0F, this.width, this.height, 0.0F, ColorUtil.rgba(255, 255, 255, 255), 1.0F
         );
         RenderUtil.drawRoundRect(context, 0.0F, 0.0F, this.width, this.height, 0.0F, ColorUtil.multiplyAlpha(ClientColors.MENU_SHADOW, 0.58F));
      } else if (this.minecraft != null && this.minecraft.level == null) {
         try {
            this.extractPanorama(context, 0.0F);
         } catch (Exception ignored) {
            this.drawGradientBackground(context, alpha);
         }
      } else {
         this.drawGradientBackground(context, alpha);
      }
   }

   private void drawGradientBackground(GuiGraphicsExtractor context, float alpha) {
      int top = ColorUtil.multiplyAlpha(ClientColors.MENU_BACKGROUND, alpha);
      int bottom = ColorUtil.multiplyAlpha(ClientColors.MENU_SHADOW, alpha);
      RenderUtil.drawRoundRectGradientQuad(context, 0.0F, 0.0F, this.width, this.height, 0.0F, top, bottom, bottom, top);
      RenderUtil.drawRoundRectGradientQuad(
         context,
         0.0F,
         0.0F,
         this.width,
         this.height,
         0.0F,
         ColorUtil.rgba(0, 0, 0, 0),
         ColorUtil.multiplyAlpha(ClientColors.MENU_SHADOW, alpha * 0.9F),
         ColorUtil.multiplyAlpha(ClientColors.MENU_SHADOW, alpha * 0.9F),
         ColorUtil.rgba(0, 0, 0, 0)
      );
   }

   private void drawLogo(GuiGraphicsExtractor context, float cx, float y, float alpha) {
      float textSize = 26.0F;
      String brand = "Havoc";
      GFonts font = GFonts.BOLD;
      float titleW = RenderUtil.getTextWidth(brand, font, textSize);
      float textH = RenderUtil.getTextHeight(font, textSize);
      float iconSize = 28.0F;
      float blockW = iconSize + 8.0F + titleW;
      float startX = cx - blockW / 2.0F;
      long nowMs = System.currentTimeMillis();
      RenderUtil.drawImage(context, "textures/gui/logo.png", startX, y, iconSize, iconSize, 4.0F, this.gradientColor(nowMs, 0, alpha), 1.0F);
      float cursor = startX + iconSize + 8.0F;
      float textTop = y + (iconSize - textH) / 2.0F;

      for (int i = 0; i < brand.length(); i++) {
         String glyph = String.valueOf(brand.charAt(i));
         RenderUtil.drawText(context, glyph, cursor, textTop, this.gradientColor(nowMs, i + 1, alpha), font, textSize);
         cursor += RenderUtil.getTextWidth(glyph, font, textSize);
      }
   }

   private void drawVersionTag(GuiGraphicsExtractor context, float cx, float y, float alpha) {
      String text = "1.0  •  Minecraft " + (this.minecraft != null ? this.minecraft.getLaunchedVersion() : "26.2");
      float w = RenderUtil.getTextWidth(text, GFonts.MEDIUM, 6.0F);
      RenderUtil.drawText(context, text, cx - w / 2.0F, y, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, alpha * 0.9F), GFonts.MEDIUM, 6.0F);
   }

   private int gradientColor(long nowMs, int index, float alpha) {
      int angle = Math.floorMod((int)(nowMs / 6L - index * 22L), 360);
      angle = angle >= 180 ? 360 - angle : angle;
      return ColorUtil.multiplyAlpha(
         ColorUtil.interpolate(ClientColors.CATEGORY_ACCENT, ClientColors.CATEGORY_ACCENT_SECOND, angle / 180.0F), Math.max(0.0F, alpha)
      );
   }

   private void drawButton(
      GuiGraphicsExtractor context,
      String label,
      float x,
      float y,
      float w,
      float h,
      boolean primary,
      int mouseX,
      int mouseY,
      String key,
      float stagger,
      boolean stickyActive
   ) {
      float progress = this.animation.animate01(key + ":appear", 1.0F, 11.0F, 0.0F);
      float appear = this.smoothStep(Math.clamp((progress - stagger) / Math.max(0.001F, 1.0F - stagger), 0.0F, 1.0F));
      if (appear <= 0.01F) {
         return;
      } else {
         boolean hovered = this.inRect(mouseX, mouseY, x, y, w, h);
         float hover = this.animation.animate01(key + ":hover", hovered ? 1.0F : 0.0F, 14.0F, 0.0F);
         y += (1.0F - appear) * 5.0F;
         if (primary) {
            float pulse = 0.5F + 0.5F * (float)Math.sin(System.currentTimeMillis() / 900.0);
            int left = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, appear);
            int right = ColorUtil.multiplyAlpha(ColorUtil.interpolate(ClientColors.CATEGORY_ACCENT_SECOND, ClientColors.CATEGORY_ACCENT, pulse), appear);
            RenderUtil.drawShadow(
               context, x, y, w, h, 4.0F, ColorUtil.multiplyAlpha(ClientColors.MENU_SHADOW, appear * (0.55F + hover * 0.45F)), 1.0F, 11.0F, 0.0F, 3.0F
            );
            RenderUtil.drawRoundRectGradient(context, x, y, w, h, 4.0F, left, right, 0.9F);
            RenderUtil.drawBorder(
               context, x, y, w, h, 4.0F, ColorUtil.rgba(255, 255, 255, Math.round(70.0F * appear * (0.4F + hover * 0.6F))), 1.0F, 0.0F, 0.5F
            );
            this.drawCenteredText(context, label, x + w / 2.0F, y + (h - 7.0F) / 2.0F, ColorUtil.rgba(255, 255, 255, Math.round(255.0F * appear)), 7.0F, true);
         } else {
            float bgAlpha = stickyActive ? 0.85F : 0.5F + hover * 0.25F;
            RenderUtil.drawRoundRect(context, x, y, w, h, 4.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND, appear * bgAlpha));
            int borderColor = stickyActive
               ? ClientColors.CATEGORY_ACCENT
               : ColorUtil.interpolate(ClientColors.CATEGORY_BACKGROUND_OUTLINE, ClientColors.CATEGORY_ACCENT, hover);
            float borderAlpha = stickyActive ? 0.9F : 0.35F + hover * 0.55F;
            RenderUtil.drawBorder(context, x, y, w, h, 4.0F, ColorUtil.multiplyAlpha(borderColor, appear * borderAlpha), 1.0F, 0.0F, 0.5F);
            int textColor = ColorUtil.multiplyAlpha(
               ColorUtil.interpolate(ClientColors.TEXT_PRIMARY, ClientColors.CATEGORY_INACTIVE, stickyActive ? 0.0F : 0.35F * (1.0F - hover)), appear
            );
            this.drawCenteredText(context, label, x + w / 2.0F, y + (h - 7.0F) / 2.0F, textColor, 7.0F, false);
         }
      }
   }

   private void drawCenteredText(GuiGraphicsExtractor context, String text, float cx, float y, int color, float size, boolean bold) {
      GFonts font = bold ? GFonts.SEMIBOLD : GFonts.MEDIUM;
      RenderUtil.drawText(context, text, cx - RenderUtil.getTextWidth(text, font, size) / 2.0F, y, color, font, size);
   }

   private void drawCornerIcons(GuiGraphicsExtractor context, int mouseX, int mouseY) {
      float size = 16.0F;
      float topY = 8.0F;
      this.iconRects[0] = this.width - size - 8.0F;
      this.iconRects[1] = topY;
      this.iconRects[2] = this.width - size * 2.0F - 14.0F;
      this.drawIcon(context, this.iconRects[0], topY, size, mouseX, mouseY, "title:lang", "\uE894");
      this.drawIcon(context, this.iconRects[2], topY, size, mouseX, mouseY, "title:a11y", "\uE8C3");
   }

   private void drawIcon(GuiGraphicsExtractor context, float x, float y, float size, int mouseX, int mouseY, String key, String glyph) {
      boolean hovered = this.inRect(mouseX, mouseY, x, y, size, size);
      float hover = this.animation.animate01(key + ":hover", hovered ? 1.0F : 0.0F, 16.0F, 0.0F);
      RenderUtil.drawRoundRect(context, x, y, size, size, 4.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND, 0.5F + hover * 0.3F));
      RenderUtil.drawBorder(
         context, x, y, size, size, 4.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND_OUTLINE, 0.35F + hover * 0.55F), 1.0F, 0.0F, 0.5F
      );
      int color = ColorUtil.interpolate(ClientColors.CATEGORY_INACTIVE, ClientColors.TEXT_PRIMARY, hover);
      float gw = RenderUtil.getTextWidth(glyph, GFonts.OPAL_ICONS, 9.0F);
      RenderUtil.drawText(context, glyph, x + (size - gw) / 2.0F, y + (size - 9.0F) / 2.0F + 0.5F, color, GFonts.OPAL_ICONS, 9.0F);
   }

   private void drawFooter(GuiGraphicsExtractor context, float alpha) {
      RenderUtil.drawText(context, "Havoc Client", 6.0F, this.height - 12.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, alpha), GFonts.MEDIUM, 6.0F);
      String right = "не аффилировано с Mojang";
      float rw = RenderUtil.getTextWidth(right, GFonts.MEDIUM, 6.0F);
      RenderUtil.drawText(
         context, right, this.width - rw - 6.0F, this.height - 12.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, alpha * 0.7F), GFonts.MEDIUM, 6.0F
      );
   }

   private float panelX() {
      float x = this.width / 2.0F + BTN_W / 2.0F + 10.0F;
      return Math.min(x, this.width - PANEL_W - 6.0F);
   }

   private float panelY(float colTop) {
      return colTop - 8.0F;
   }

   private void drawAccountsPanel(GuiGraphicsExtractor context, float colTop, int mouseX, int mouseY) {
      float anim = this.accountsAnim.update(1.0F, 0.09F, System.nanoTime());
      float a = this.smoothStep(anim);
      float x = this.panelX() + (1.0F - a) * 12.0F;
      float y = this.panelY(colTop);
      HudVisualStyle.drawPanel(context, x, y, PANEL_W, PANEL_H, a);
      RenderUtil.drawShadow(context, x, y, PANEL_W, PANEL_H, 5.0F, ColorUtil.multiplyAlpha(ClientColors.MENU_SHADOW, 0.5F * a), 1.0F, 12.0F, 0.0F, 6.0F);
      RenderUtil.drawBorder(context, x, y, PANEL_W, PANEL_H, 5.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND_OUTLINE, a * 0.7F), 1.0F, 0.0F, 0.5F);
      float px = x + 10.0F;
      float pw = PANEL_W - 20.0F;
      float cy = y + 10.0F;
      this.drawCenteredText(context, "Аккаунты", x + PANEL_W / 2.0F, cy, ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, a), 8.0F, true);
      cy += 18.0F;
      String active = AccountManager.activeName();
      this.drawHead(context, active, px, cy, 22.0F, a);
      RenderUtil.drawText(context, "текущий аккаунт", px + 28.0F, cy + 2.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, a * 0.9F), GFonts.MEDIUM, 5.5F);
      RenderUtil.drawText(
         context, active == null ? "—" : active, px + 28.0F, cy + 10.0F, ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, a), GFonts.SEMIBOLD, 7.5F
      );
      cy += 30.0F;
      this.layoutInputY = cy;
      float addW = 16.0F;
      boolean inputHover = this.inRect(mouseX, mouseY, px, cy, pw - addW - 4.0F, 16.0F);
      float focusMix = this.animation.animate01("title:input", this.inputFocused ? 1.0F : inputHover ? 0.45F : 0.0F, 16.0F, 0.0F);
      RenderUtil.drawRoundRect(context, px, cy, pw - addW - 4.0F, 16.0F, 3.0F, ColorUtil.multiplyAlpha(ClientColors.SETTINGS_BACKGROUND, a * 0.95F));
      RenderUtil.drawBorder(
         context,
         px,
         cy,
         pw - addW - 4.0F,
         16.0F,
         3.0F,
         ColorUtil.multiplyAlpha(ColorUtil.interpolate(ClientColors.SETTINGS_OUTLINE, ClientColors.CATEGORY_ACCENT, focusMix), a),
         1.0F,
         0.0F,
         0.5F
      );
      boolean placeholder = this.accountInput.isEmpty() && !this.inputFocused;
      String display = placeholder ? "имя аккаунта" : this.accountInput;
      int displayColor = ColorUtil.multiplyAlpha(placeholder ? ClientColors.CATEGORY_INACTIVE : ClientColors.TEXT_PRIMARY, a);
      RenderUtil.drawText(context, display, px + 6.0F, cy + 4.5F, displayColor, GFonts.MEDIUM, 6.5F);
      if (this.inputFocused && (System.currentTimeMillis() / 520L & 1L) == 0L) {
         float caretX = px + 6.5F + RenderUtil.getTextWidth(this.accountInput, GFonts.MEDIUM, 6.5F);
         RenderUtil.drawRoundRect(context, caretX, cy + 3.5F, 1.0F, 9.0F, 0.5F, ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, a));
      }

      this.drawButton(context, "+", px + pw - addW, cy, addW, 16.0F, true, mouseX, mouseY, "title:add", 0.0F, false);
      cy += 24.0F;
      RenderUtil.drawText(context, "СОХРАНЁННЫЕ", px, cy, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, a * 0.7F), GFonts.MEDIUM, 5.5F);
      cy += 10.0F;
      this.layoutListY = cy;
      int idx = 0;

      for (AccountManager.Stored stored : AccountManager.get()) {
         if (idx >= 5) {
            break;
         }

         this.drawAccountRow(context, stored, px, cy, pw, a, mouseX, mouseY, active, idx);
         cy += ROW_H + 3.0F;
         idx++;
      }

      if (idx == 0) {
         RenderUtil.drawText(
            context, "Список пуст — добавь ник выше", px + 2.0F, cy + 5.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, a * 0.8F), GFonts.MEDIUM, 6.5F
         );
      }

      float actionW = (pw - 4.0F) / 2.0F;
      float actionY = y + PANEL_H - 26.0F;
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
      int rowBg = ColorUtil.interpolate(ClientColors.MENU_BACKGROUND, selected ? ClientColors.CATEGORY_ACCENT : ClientColors.CATEGORY_BACKGROUND, 0.35F + hover * 0.25F);
      RenderUtil.drawRoundRect(context, px, y, pw, ROW_H, 3.5F, ColorUtil.multiplyAlpha(rowBg, a * 0.9F));
      if (selected) {
         RenderUtil.drawBorder(context, px, y, pw, ROW_H, 3.5F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, a * 0.75F), 1.0F, 0.0F, 0.5F);
      }

      this.drawHead(context, stored.name, px + 4.0F, y + 4.0F, 16.0F, a);
      int nameColor = ColorUtil.multiplyAlpha(activeRow ? ClientColors.CATEGORY_ACCENT : ClientColors.TEXT_PRIMARY, a);
      RenderUtil.drawText(context, stored.name, px + 26.0F, y + 3.5F, nameColor, GFonts.SEMIBOLD, 7.0F);
      String sub = activeRow ? "используется сейчас" : "добавлен " + formatDate(stored.addedAt);
      RenderUtil.drawText(context, sub, px + 26.0F, y + 12.5F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, a * 0.85F), GFonts.MEDIUM, 5.5F);
   }

   private void drawHead(GuiGraphicsExtractor context, String name, float x, float y, float size, float alpha) {
      GpuTextureView view = this.resolveHead(name);
      int color = ColorUtil.multiplyAlpha(ColorUtil.rgba(255, 255, 255, 255), alpha);
      if (view != null) {
         RenderUtil.drawHeadImageRegion(context, view, x, y, size, size, 8.0F, 8.0F, 8.0F, 8.0F, 3.0F, color, 1.0F);
         RenderUtil.drawHeadImageRegion(context, view, x, y, size, size, 40.0F, 8.0F, 8.0F, 8.0F, 3.0F, color, 1.0F);
      } else {
         RenderUtil.drawRoundRect(context, x, y, size, size, 3.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND, alpha * 0.8F));
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
      if (click.button() == 0) {
         double mx = click.x();
         double my = click.y();
         if (this.accountsOpen && this.handleAccountsClick(mx, my)) {
            return true;
         }

         float x = this.width / 2.0F - BTN_W / 2.0F;
         float y = this.height / 2.0F - (5 * BTN_H + 4 * BTN_GAP) / 2.0F + 26.0F;
         if (this.inRect(mx, my, x, y, BTN_W, BTN_H)) {
            this.openScreen(new SelectWorldScreen(this));
            return true;
         }

         y += BTN_H + BTN_GAP;
         if (this.inRect(mx, my, x, y, BTN_W, BTN_H)) {
            this.openScreen(new JoinMultiplayerScreen(this));
            return true;
         }

         y += BTN_H + BTN_GAP;
         if (this.inRect(mx, my, x, y, BTN_W, BTN_H)) {
            this.accountsOpen = !this.accountsOpen;
            if (this.accountsOpen) {
               this.accountsAnim.setValue(0.0F);
               this.selectedAccount = null;
               this.inputFocused = false;
            }

            return true;
         }

         y += BTN_H + BTN_GAP;
         if (this.inRect(mx, my, x, y, BTN_W, BTN_H)) {
            this.openScreen(new OptionsScreen(this, this.minecraft.options, false));
            return true;
         }

         y += BTN_H + BTN_GAP;
         if (this.inRect(mx, my, x, y, BTN_W, BTN_H)) {
            if (this.minecraft != null) {
               this.minecraft.stop();
            }

            return true;
         }

         if (this.inRect(mx, my, this.iconRects[0], this.iconRects[1], 16.0F, 16.0F)) {
            this.openScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager()));
            return true;
         }

         if (this.inRect(mx, my, this.iconRects[2], this.iconRects[1], 16.0F, 16.0F)) {
            this.openScreen(new AccessibilityOptionsScreen(this, this.minecraft.options));
            return true;
         }
      }

      return super.mouseClicked(click, doubled);
   }

   private void openScreen(Screen screen) {
      if (this.minecraft != null) {
         this.minecraft.gui.setScreen(screen);
      }
   }

   private boolean handleAccountsClick(double mx, double my) {
      float x = this.panelX() - 12.0F;
      float y = this.panelY(this.menuColTop());
      float anim = this.accountsAnim.getValue();
      if (this.inRect(mx, my, x, y, PANEL_W, PANEL_H)) {
         float px = x + 10.0F + (1.0F - anim) * 12.0F;
         float pw = PANEL_W - 20.0F;
         float addW = 16.0F;
         this.inputFocused = this.inRect(mx, my, px, this.layoutInputY, pw - addW - 4.0F, 16.0F);
         if (this.inRect(mx, my, px + pw - addW, this.layoutInputY, addW, 16.0F)) {
            this.tryAddAccount();
            return true;
         }

         int idx = 0;

         for (AccountManager.Stored stored : AccountManager.get()) {
            if (idx >= 5) {
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

   private float menuColTop() {
      return this.height / 2.0F - (5 * BTN_H + 4 * BTN_GAP) / 2.0F + 26.0F;
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
            return true;
         }
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
