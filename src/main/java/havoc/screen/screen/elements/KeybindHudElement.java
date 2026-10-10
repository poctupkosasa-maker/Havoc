package havoc.screen.screen.elements;

import havoc.Havoc;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.bind.BindData;
import havoc.module.setting.BooleanSetting;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.screen.screen.AbstractHudElement;
import havoc.screen.screen.HudVisualStyle;
import havoc.utilites.animation.AnimatedValue;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ClientTheme;
import havoc.utilites.color.ColorUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;

public final class KeybindHudElement extends AbstractHudElement {
   private final BooleanSetting alwaysShow = this.addSetting(new BooleanSetting("Always show", true));
   private static final float FIXED_WIDTH = 92.0F;
   private final AnimatedValue widthAnim = new AnimatedValue();
   private final AnimatedValue heightAnim = new AnimatedValue();
   private final AnimatedValue visibilityAnim = new AnimatedValue();
   private final Map<String, KeybindHudElement.RowState> rowStates = new HashMap<>();
   private boolean active = true;

   public KeybindHudElement() {
      super("keybind", "Keybind", 12.0F, 42.0F, 92.0F, 22.0F);
   }

   @Override
   public void setActive(boolean active) {
      this.active = active;
   }

   @Override
   public boolean shouldRenderWhenInactive() {
      return true;
   }

   @Override
   public void render(GuiGraphicsExtractor context, int screenWidth, int screenHeight, float tickDelta) {
      long now = System.nanoTime();
      List<KeybindHudElement.BoundEntry> entries = new ArrayList<>();

      for (Module module : Havoc.getInstance().getModuleManager().getModules()) {
         BindData bind = module.getBind();
         if (bind.hasKeys() && module.isEnable() && bind.isShowInBinds()) {
            entries.add(new KeybindHudElement.BoundEntry(module.getName(), this.formatKeys(bind), module.getCategory()));
         }
      }

      float padX = 7.0F;
      float titleSize = 8.5F;
      float textSize = 8.0F;
      float rowGap = 4.5F;
      float titlePadTop = 6.5F;
      float titlePadBottom = 10.5F;
      float rowPadBottom = 4.0F;
      float iconSize = 8.0F;

      for (KeybindHudElement.RowState state : this.rowStates.values()) {
         state.seen = false;
      }

      int i = 0;

      while (i < entries.size()) {
         KeybindHudElement.BoundEntry entry = entries.get(i);
         KeybindHudElement.RowState state = this.rowStates.get(entry.label());
         if (state == null) {
            state = new KeybindHudElement.RowState(entry);
            state.alphaAnim.setValue(0.0F);
            state.indexAnim.setValue(i + 0.35F);
            this.rowStates.put(entry.label(), state);
         } else {
            state.entry = entry;
         }

         state.seen = true;
         state.targetIndex = i++;
      }

      float maxLabelW = 0.0F;
      float maxKeyTextW = 0.0F;

      for (KeybindHudElement.RowState state : this.rowStates.values()) {
         KeybindHudElement.BoundEntry entry = state.entry;
         float labelW = RenderUtil.getTextWidth(entry.label().toLowerCase(Locale.ROOT), GFonts.MEDIUM, textSize);
         float keyW = RenderUtil.getTextWidth(entry.keyText(), GFonts.MEDIUM, textSize);
         maxLabelW = Math.max(maxLabelW, labelW);
         maxKeyTextW = Math.max(maxKeyTextW, keyW);
      }

      float rowHeight = RenderUtil.getTextHeight(GFonts.MEDIUM, textSize);
      float baseY = titlePadTop + titleSize + titlePadBottom;
      float keysBoxExtra = 7.0F;
      float keysColumnWidth = maxKeyTextW + keysBoxExtra;
      float targetWidth = padX + iconSize + 3.0F + maxLabelW + 12.0F + keysColumnWidth + padX;
      targetWidth = Math.max(92.0F, targetWidth);
      float rowStride = rowHeight + rowGap;
      float maxVisualIndex = -1.0F;
      List<KeybindHudElement.RowVisual> visuals = new ArrayList<>();
      List<String> toRemove = new ArrayList<>();

      for (Entry<String, KeybindHudElement.RowState> stateEntry : this.rowStates.entrySet()) {
         KeybindHudElement.RowState state = stateEntry.getValue();
         float targetAlpha = state.seen ? 1.0F : 0.0F;
         float alpha = state.alphaAnim.update(targetAlpha, 0.05F, now);
         float animatedIndex = state.indexAnim.update(state.targetIndex, 0.06F, now);
         if (!state.seen && alpha <= 0.01F) {
            toRemove.add(stateEntry.getKey());
         } else {
            maxVisualIndex = Math.max(maxVisualIndex, animatedIndex);
            if (alpha > 0.01F) {
               visuals.add(new KeybindHudElement.RowVisual(state.entry, animatedIndex, alpha));
            }
         }
      }

      for (String key : toRemove) {
         this.rowStates.remove(key);
      }

      visuals.sort(Comparator.comparingDouble(KeybindHudElement.RowVisual::animatedIndex));
      float listHeight = maxVisualIndex >= 0.0F ? (maxVisualIndex + 1.0F) * rowStride - rowGap + rowPadBottom : 0.0F;
      float targetHeight = baseY + listHeight;
      Minecraft mc = Minecraft.getInstance();
      boolean chatOpen = mc != null && mc.gui.screen() instanceof ChatScreen;
      boolean hasRows = !this.rowStates.isEmpty();
      boolean shouldShow = this.active && (this.alwaysShow.isValue() || hasRows || chatOpen);
      float visibleTarget = shouldShow ? 1.0F : 0.0F;
      float visible = this.visibilityAnim.update(visibleTarget, 0.08F, now);
      float animatedWidth = this.widthAnim.update(targetWidth, 0.08F, now);
      float animatedHeight = this.heightAnim.update(targetHeight, 0.08F, now);
      this.setBounds(animatedWidth, animatedHeight);
      if (!(visible <= 0.01F) && !(animatedWidth <= 1.0F) && !(animatedHeight <= 1.0F)) {
         float x = this.getX();
         float y = this.getY();
         float panelAlpha = Math.min(1.0F, visible / 0.33F);
         HudVisualStyle.drawPanel(context, x, y, this.getWidth(), this.getHeight(), panelAlpha);
         RenderUtil.drawText(
            context, "Hotkeys", x + padX, y + titlePadTop, ColorUtil.multiplyAlpha(HudVisualStyle.PRIMARY_TEXT, visible), GFonts.MEDIUM, titleSize
         );
         float headerIconSize = 9.0F;
         float headerIconX = x + this.getWidth() - padX - headerIconSize;
         RenderUtil.drawText(
            context, "J", headerIconX, y + 5.55F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, visible), GFonts.NOTIFICATION_ICONS, headerIconSize
         );
         int dividerColor = ClientTheme.isLightMode() ? -16777216 : -2500135;
         float dividerWidth = this.getWidth() * 0.47F;
         float dividerX = x + (this.getWidth() - dividerWidth) * 0.5F;
         float dividerY = y + baseY - 5.5F;
         HudVisualStyle.drawHairline(context, dividerX, dividerY, dividerWidth, dividerColor, visible * 0.92F);
         float cursorY = y + baseY;

         for (KeybindHudElement.RowVisual visual : visuals) {
            KeybindHudElement.BoundEntry entry = visual.entry();
            float rowY = cursorY + visual.animatedIndex() * rowStride;
            float rowAlpha = visible * visual.alpha();
            float rowReveal = HudVisualStyle.easeOutCubic(visual.alpha());
            float rowX = x + padX + (1.0F - rowReveal) * 4.0F;
            int labelColor = ColorUtil.multiplyAlpha(HudVisualStyle.PRIMARY_TEXT, rowAlpha);
            int categoryColor = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, rowAlpha);
            String categoryGlyph = this.categoryGlyph(entry.category());
            if (categoryGlyph != null) {
               RenderUtil.drawText(context, categoryGlyph, rowX, rowY - 0.1F, categoryColor, GFonts.NOTIFICATION_ICONS, iconSize);
            } else {
               RenderUtil.drawImage(context, this.categoryIcon(entry.category()), rowX, rowY - 0.65F, iconSize, iconSize, 0.0F, categoryColor, 1.0F);
            }

            float labelX = rowX + iconSize + 3.0F;
            RenderUtil.drawText(context, entry.label().toLowerCase(Locale.ROOT), labelX, rowY + 0.25F, labelColor, GFonts.MEDIUM, textSize);
            float keyTextW = RenderUtil.getTextWidth(entry.keyText(), GFonts.MEDIUM, textSize);
            float bgW = keyTextW + keysBoxExtra;
            float bgX = x + this.getWidth() - padX - bgW + 1.5F;
            float bgY = rowY - 3.0F;
            RenderUtil.drawText(
               context,
               entry.keyText(),
               bgX + (bgW - keyTextW) * 0.5F + 0.5F,
               rowY + 0.25F,
               ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, rowAlpha),
               GFonts.MEDIUM,
               textSize
            );
         }
      }
   }

   private String categoryIcon(Category category) {
      return switch (category) {
         case COMBAT, MOVEMENT -> null;
         case PLAYER -> "textures/gui/icons/player.png";
         case VISUALS -> "textures/gui/icons/visuals.png";
         case MISC -> "textures/gui/icons/misc.png";
      };
   }

   private String categoryGlyph(Category category) {
      return switch (category) {
         case COMBAT -> "D";
         case MOVEMENT -> "E";
         default -> null;
      };
   }

   private String formatKeys(BindData bind) {
      List<String> ks = new ArrayList<>();

      for (int c : bind.getKeys()) {
         ks.add(this.formatBindCode(c));
      }

      return String.join(" + ", ks);
   }

   private String formatBindCode(int code) {
      if (Havoc.isMouseBindCode(code)) {
         return "M" + (Havoc.bindCodeToMouseButton(code) + 1);
      }

      return switch (code) {
         case 32 -> "SPACE";
         case 256 -> "ESC";
         case 257 -> "ENTER";
         case 258 -> "TAB";
         case 259 -> "BACKSPACE";
         case 261 -> "DELETE";
         case 340 -> "L SHIFT";
         case 341 -> "L CTRL";
         case 342 -> "L ALT";
         case 344 -> "R SHIFT";
         case 345 -> "R CTRL";
         case 346 -> "R ALT";
         default -> this.stableKeyName(code);
      };
   }

   private String stableKeyName(int code) {
      if (code >= 65 && code <= 90) {
         return Character.toString((char)(65 + code - 65));
      }

      if (code >= 48 && code <= 57) {
         return Character.toString((char)(48 + code - 48));
      }

      if (code >= 290 && code <= 314) {
         return "F" + (code - 290 + 1);
      }

      return switch (code) {
         case 260 -> "INSERT";
         default -> "KEY " + code;
         case 262 -> "RIGHT";
         case 263 -> "LEFT";
         case 264 -> "DOWN";
         case 265 -> "UP";
         case 266 -> "PGUP";
         case 267 -> "PGDN";
         case 268 -> "HOME";
         case 269 -> "END";
         case 280 -> "CAPS";
      };
   }

   private record BoundEntry(String label, String keyText, Category category) {
   }

   private static final class RowState {
      private KeybindHudElement.BoundEntry entry;
      private final AnimatedValue alphaAnim = new AnimatedValue();
      private final AnimatedValue indexAnim = new AnimatedValue();
      private boolean seen;
      private int targetIndex;

      private RowState(KeybindHudElement.BoundEntry entry) {
         this.entry = entry;
      }
   }

   private record RowVisual(KeybindHudElement.BoundEntry entry, float animatedIndex, float alpha) {
   }
}
