package havoc.screen.menu.sections;

import havoc.Havoc;
import havoc.config.ConfigManager;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.screen.menu.UiAnimationEngine;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

public final class ConfigsSection {
   private static final float CONTENT_PAD_X = 12.0F;
   private static final float CONTENT_TOP = 14.0F;
   private static final float CONTENT_BOTTOM_PAD = 10.0F;
   private static final float HEADER_H = 16.0F;
   private static final float TOP_BUTTON_SIZE = 14.0F;
   private static final float TOP_BUTTON_GAP = 6.0F;
   private static final float LIST_GAP_TOP = 8.0F;
   private static final float ENTRY_H = 24.0F;
   private static final float ENTRY_GAP = 6.0F;
   private static final float ENTRY_PAD_X = 6.0F;
   private static final float ROW_BUTTON_W = 28.0F;
   private static final float ROW_BUTTON_H = 12.0F;
   private static final float ROW_BUTTON_GAP = 4.0F;
   private static final float LIST_SCROLL_SPEED = 26.0F;
   private static final float LIST_SCROLL_STEP = 22.0F;
   private String selectedConfigName;
   private float scrollTarget;
   private float scrollValue;
   private boolean active;
   private String editingConfigName;
   private String editingText;
   private int caretIndex;
   private int selectionIndex;
   private boolean selectingWithMouse;

   public void setActive(boolean active) {
      this.active = active;
   }

   public void render(
      GuiGraphicsExtractor context,
      UiAnimationEngine animation,
      float panelX,
      float panelY,
      float panelWidth,
      float panelHeight,
      int mouseX,
      int mouseY,
      float alpha
   ) {
      ConfigManager manager = Havoc.getInstance().getConfigManager();
      List<ConfigManager.ConfigEntry> entries = manager.listConfigEntries();
      if ((this.selectedConfigName == null || this.selectedConfigName.isBlank()) && !entries.isEmpty()) {
         this.selectedConfigName = manager.getLoadedConfigName();
      }

      if (this.editingConfigName != null && entries.stream().noneMatch(e -> e.name().equalsIgnoreCase(this.editingConfigName))) {
         this.cancelEditing();
      }

      float show = animation.animate01("cfg:manager:show", this.active ? 1.0F : 0.0F, 16.0F, this.active ? 1.0F : 0.0F);
      if (!(show <= 0.001F)) {
         float drawAlpha = alpha * show;
         float yOffset = (1.0F - show) * 8.0F;
         ConfigsSection.Layout layout = this.layout(panelX, panelY + yOffset, panelWidth, panelHeight);
         int titleColor = ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, drawAlpha);
         int subColor = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, drawAlpha * 0.95F);
         RenderUtil.drawText(context, "Configs", layout.contentX, layout.headerY + 2.0F, titleColor, GFonts.MEDIUM, 9);
         RenderUtil.drawText(context, "Loaded: " + manager.getLoadedConfigName(), layout.contentX + 58.0F, layout.headerY + 2.2F, subColor, GFonts.MEDIUM, 7);
         this.renderTopButton(context, animation, "cfg:create", "textures/gui/icons/create.png", layout.createBtnX, layout.headerY, mouseX, mouseY, drawAlpha);
         this.renderTopButton(context, animation, "cfg:reload", "textures/gui/icons/reload.png", layout.reloadBtnX, layout.headerY, mouseX, mouseY, drawAlpha);
         this.renderTopButton(context, animation, "cfg:folder", "textures/gui/icons/folder.png", layout.folderBtnX, layout.headerY, mouseX, mouseY, drawAlpha);
         float contentHeight = entries.isEmpty() ? 0.0F : entries.size() * 24.0F + Math.max(0, entries.size() - 1) * 6.0F;
         float maxScroll = Math.max(0.0F, contentHeight - layout.listH);
         this.scrollTarget = this.clamp(this.scrollTarget, 0.0F, maxScroll);
         this.scrollValue = animation.animate("cfg:list:scroll", this.scrollTarget, 26.0F, this.scrollValue);
         this.scrollValue = this.clamp(this.scrollValue, 0.0F, maxScroll);
         int listBg = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_BACKGROUND, drawAlpha * 0.5F);
         int listBorder = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, drawAlpha * 0.95F);
         RenderUtil.drawRoundRect(context, layout.listX, layout.listY, layout.listW, layout.listH, 4.0F, listBg);
         RenderUtil.drawBorder(context, layout.listX, layout.listY, layout.listW, layout.listH, 4.0F, listBorder, 0.0F, 0.5F, 1.0F);
         int scX1 = (int)Math.floor(layout.listX + 1.0F);
         int scY1 = (int)Math.floor(layout.listY + 1.0F);
         int scX2 = (int)Math.ceil(layout.listX + layout.listW - 1.0F);
         int scY2 = (int)Math.ceil(layout.listY + layout.listH - 1.0F);
         context.enableScissor(scX1, scY1, scX2, scY2);

         for (int i = 0; i < entries.size(); i++) {
            ConfigManager.ConfigEntry entry = entries.get(i);
            ConfigsSection.EntryLayout row = this.rowLayout(layout, i, this.scrollValue);
            if (!(row.y + row.h < layout.listY) && !(row.y > layout.listY + layout.listH)) {
               this.drawEntry(context, animation, manager, entry, row, mouseX, mouseY, drawAlpha);
            }
         }

         context.disableScissor();
      }
   }

   public boolean handleClick(MouseButtonEvent click, float panelX, float panelY, float panelWidth, float panelHeight) {
      if (click.button() != 0) {
         return false;
      }

      ConfigManager manager = Havoc.getInstance().getConfigManager();
      List<ConfigManager.ConfigEntry> entries = manager.listConfigEntries();
      ConfigsSection.Layout layout = this.layout(panelX, panelY, panelWidth, panelHeight);
      if (this.hitTopButton(layout.createBtnX, layout.headerY, click)) {
         this.commitEditingIfNeeded();
         String created = manager.createDefaultConfig();
         if (created != null) {
            this.selectedConfigName = created;
         }

         return true;
      } else {
         if (this.hitTopButton(layout.reloadBtnX, layout.headerY, click)) {
            this.commitEditingIfNeeded();
            manager.reload();
            return true;
         }

         if (this.hitTopButton(layout.folderBtnX, layout.headerY, click)) {
            this.commitEditingIfNeeded();
            manager.openConfigFolder();
            return true;
         }

         if (!this.isHovered(click.x(), click.y(), layout.listX, layout.listY, layout.listW, layout.listH)) {
            this.commitEditingIfNeeded();
            return false;
         }

         for (int i = 0; i < entries.size(); i++) {
            ConfigManager.ConfigEntry entry = entries.get(i);
            ConfigsSection.EntryLayout row = this.rowLayout(layout, i, this.scrollValue);
            if (this.isHovered(click.x(), click.y(), row.x, row.y, row.w, row.h)) {
               this.selectedConfigName = entry.name();
               if (this.isHovered(click.x(), click.y(), row.loadX, row.btnY, 28.0F, 12.0F)) {
                  this.commitEditingIfNeeded();
                  manager.loadConfig(entry.name());
                  return true;
               }

               if (this.isHovered(click.x(), click.y(), row.saveX, row.btnY, 28.0F, 12.0F)) {
                  this.commitEditingIfNeeded();
                  manager.saveConfig(entry.name());
                  return true;
               }

               if (this.isHovered(click.x(), click.y(), row.deleteX, row.btnY, 28.0F, 12.0F)) {
                  this.commitEditingIfNeeded();
                  if (!entry.name().equalsIgnoreCase("auto")) {
                     manager.deleteConfig(entry.name());
                     if (entry.name().equalsIgnoreCase(this.selectedConfigName)) {
                        this.selectedConfigName = manager.getLoadedConfigName();
                     }
                  }

                  return true;
               }

               if (this.editingConfigName == null || !this.editingConfigName.equalsIgnoreCase(entry.name())) {
                  this.startEditing(entry.name());
               }

               int index = this.indexFromMouseX(this.editingText, row.nameTextX, (float)click.x());
               this.caretIndex = index;
               this.selectionIndex = index;
               this.selectingWithMouse = true;
               return true;
            }
         }

         return true;
      }
   }

   public boolean handleMouseDragged(double mouseX, double mouseY, int button, float panelX, float panelY, float panelWidth, float panelHeight) {
      if (this.selectingWithMouse && button == 0 && this.editingConfigName != null) {
         ConfigManager manager = Havoc.getInstance().getConfigManager();
         List<ConfigManager.ConfigEntry> entries = manager.listConfigEntries();
         ConfigsSection.Layout layout = this.layout(panelX, panelY, panelWidth, panelHeight);

         for (int i = 0; i < entries.size(); i++) {
            ConfigManager.ConfigEntry entry = entries.get(i);
            if (entry.name().equalsIgnoreCase(this.editingConfigName)) {
               ConfigsSection.EntryLayout row = this.rowLayout(layout, i, this.scrollValue);
               this.caretIndex = this.indexFromMouseX(this.editingText, row.nameTextX, (float)mouseX);
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean handleMouseReleased(int button) {
      if (button == 0 && this.selectingWithMouse) {
         this.selectingWithMouse = false;
         return true;
      } else {
         return false;
      }
   }

   public boolean handleKeyPressed(KeyEvent key) {
      if (this.editingConfigName == null) {
         return false;
      }

      int code = key.key();
      boolean ctrl = this.isCtrlDown();
      boolean shift = this.isShiftDown();
      if (code == 256) {
         this.cancelEditing();
         return true;
      }

      if (code == 257 || code == 335) {
         this.commitEditingIfNeeded();
         return true;
      }

      if (ctrl && code == 65) {
         this.selectionIndex = 0;
         this.caretIndex = this.editingText.length();
         return true;
      }

      if (ctrl && code == 67) {
         this.copySelection();
         return true;
      }

      if (ctrl && code == 88) {
         if (!this.isEditingAuto() && this.hasSelection()) {
            this.copySelection();
            this.deleteSelection();
         }

         return true;
      } else if (ctrl && code == 86) {
         if (!this.isEditingAuto()) {
            String clip = this.getClipboard();
            if (clip != null && !clip.isEmpty()) {
               this.insertText(clip);
            }
         }

         return true;
      } else {
         if (code == 263) {
            this.moveCaret(-1, shift);
            return true;
         }

         if (code == 262) {
            this.moveCaret(1, shift);
            return true;
         }

         if (code == 268) {
            this.setCaret(0, shift);
            return true;
         }

         if (code == 269) {
            this.setCaret(this.editingText.length(), shift);
            return true;
         }

         if (code == 259) {
            if (!this.isEditingAuto() && !this.deleteSelection() && this.caretIndex > 0) {
               this.editingText = this.editingText.substring(0, this.caretIndex - 1) + this.editingText.substring(this.caretIndex);
               this.caretIndex--;
               this.selectionIndex = this.caretIndex;
            }

            return true;
         } else if (code == 261) {
            if (!this.isEditingAuto() && !this.deleteSelection() && this.caretIndex < this.editingText.length()) {
               this.editingText = this.editingText.substring(0, this.caretIndex) + this.editingText.substring(this.caretIndex + 1);
               this.selectionIndex = this.caretIndex;
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public boolean handleCharTyped(int codePoint) {
      if (this.editingConfigName == null || this.isEditingAuto()) {
         return false;
      } else if (!Character.isValidCodePoint(codePoint)) {
         return false;
      } else {
         char chr = (char)codePoint;
         if (chr >= ' ' && chr != 127) {
            this.insertText(String.valueOf(chr));
            return true;
         } else {
            return false;
         }
      }
   }

   public boolean handleScroll(double mouseX, double mouseY, double verticalAmount, float panelX, float panelY, float panelWidth, float panelHeight) {
      if (verticalAmount == 0.0) {
         return false;
      }

      ConfigsSection.Layout layout = this.layout(panelX, panelY, panelWidth, panelHeight);
      if (!this.isHovered(mouseX, mouseY, layout.listX, layout.listY, layout.listW, layout.listH)) {
         return false;
      }

      List<ConfigManager.ConfigEntry> entries = Havoc.getInstance().getConfigManager().listConfigEntries();
      float contentHeight = entries.isEmpty() ? 0.0F : entries.size() * 24.0F + Math.max(0, entries.size() - 1) * 6.0F;
      float maxScroll = Math.max(0.0F, contentHeight - layout.listH);
      this.scrollTarget = this.clamp(this.scrollTarget - (float)verticalAmount * 22.0F, 0.0F, maxScroll);
      return true;
   }

   public void applyUiState(String selectedConfig, float configsScroll) {
      this.selectedConfigName = selectedConfig;
      this.scrollTarget = Math.max(0.0F, configsScroll);
      this.scrollValue = this.scrollTarget;
   }

   public String getSelectedConfigName() {
      return this.selectedConfigName;
   }

   public float getScrollTarget() {
      return this.scrollTarget;
   }

   private void drawEntry(
      GuiGraphicsExtractor context,
      UiAnimationEngine animation,
      ConfigManager manager,
      ConfigManager.ConfigEntry entry,
      ConfigsSection.EntryLayout row,
      int mouseX,
      int mouseY,
      float alpha
   ) {
      boolean loaded = entry.name().equalsIgnoreCase(manager.getLoadedConfigName());
      boolean selected = entry.name().equalsIgnoreCase(this.selectedConfigName);
      boolean hovered = this.isHovered(mouseX, mouseY, row.x, row.y, row.w, row.h);
      float hover = animation.animate01("cfg:row:hover:" + entry.name(), hovered ? 1.0F : 0.0F, 14.0F, 0.0F);
      int rowBg = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, alpha * ((selected ? 0.18F : 0.0F) + hover * 0.1F));
      if (ColorUtil.alpha(rowBg) > 0) {
         RenderUtil.drawRoundRect(context, row.x, row.y, row.w, row.h, 3.5F, rowBg);
      }

      int rowBorder = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, alpha * 0.65F);
      RenderUtil.drawBorder(context, row.x, row.y, row.w, row.h, 3.5F, rowBorder, 0.0F, 0.5F, 1.0F);
      this.renderRowButton(context, animation, "cfg:row:load:" + entry.name(), "Load", row.loadX, row.btnY, mouseX, mouseY, alpha);
      this.renderRowButton(context, animation, "cfg:row:save:" + entry.name(), "Save", row.saveX, row.btnY, mouseX, mouseY, alpha);
      boolean canDelete = !entry.name().equalsIgnoreCase("auto");
      this.renderRowButton(context, animation, "cfg:row:del:" + entry.name(), canDelete ? "Delete" : "-", row.deleteX, row.btnY, mouseX, mouseY, alpha);
      boolean editingThis = this.editingConfigName != null && this.editingConfigName.equalsIgnoreCase(entry.name());
      int textColor = ColorUtil.multiplyAlpha(loaded ? ClientColors.TEXT_PRIMARY : ClientColors.CATEGORY_INACTIVE, alpha);
      float textY = row.y + 8.0F;
      if (editingThis) {
         this.drawEditingText(context, animation, row, textY, textColor, alpha);
      } else {
         RenderUtil.drawTextFadeRight(context, entry.name(), row.nameTextX, textY, textColor, GFonts.MEDIUM, 8, row.nameTextW, 4.0F);
      }
   }

   private void drawEditingText(
      GuiGraphicsExtractor context, UiAnimationEngine animation, ConfigsSection.EntryLayout row, float textY, int textColor, float alpha
   ) {
      int selStart = Math.min(this.caretIndex, this.selectionIndex);
      int selEnd = Math.max(this.caretIndex, this.selectionIndex);
      if (selEnd > selStart) {
         String before = this.editingText.substring(0, selStart);
         String mid = this.editingText.substring(selStart, selEnd);
         float startX = row.nameTextX + RenderUtil.getTextWidth(before, GFonts.MEDIUM, 8);
         float selW = RenderUtil.getTextWidth(mid, GFonts.MEDIUM, 8);
         int selColor = ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, alpha * 0.38F);
         RenderUtil.drawRoundRect(context, startX - 0.5F, row.y + 4.0F, selW + 1.0F, 11.0F, 2.0F, selColor);
      }

      RenderUtil.drawTextFadeRight(context, this.editingText, row.nameTextX, textY, textColor, GFonts.MEDIUM, 8, row.nameTextW, 4.0F);
      float caretBlink = animation.animate01("cfg:caret:blink", 1.0F, 10.0F, 0.0F);
      int caretColor = ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha * (0.35F + 0.65F * caretBlink));
      float caretX = row.nameTextX
         + RenderUtil.getTextWidth(this.editingText.substring(0, this.clampInt(this.caretIndex, 0, this.editingText.length())), GFonts.MEDIUM, 8);
      RenderUtil.drawRoundRect(context, caretX, row.y + 4.0F, 1.0F, 11.0F, 0.5F, caretColor);
   }

   private void renderTopButton(
      GuiGraphicsExtractor context, UiAnimationEngine animation, String key, String iconPath, float x, float y, int mouseX, int mouseY, float alpha
   ) {
      boolean hovered = this.isHovered(mouseX, mouseY, x, y, 14.0F, 14.0F);
      float hover = animation.animate01("cfg:topbtn:hover:" + key, hovered ? 1.0F : 0.0F, 14.0F, 0.0F);
      int bg = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_BACKGROUND, alpha * (0.6F + hover * 0.24F));
      int border = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, alpha * 0.95F);
      int icon = ColorUtil.multiplyAlpha(ColorUtil.interpolate(ClientColors.CATEGORY_INACTIVE, ClientColors.TEXT_PRIMARY, hover * 0.8F), alpha);
      RenderUtil.drawRoundRect(context, x, y, 14.0F, 14.0F, 3.0F, bg);
      RenderUtil.drawBorder(context, x, y, 14.0F, 14.0F, 3.0F, border, 0.0F, 0.5F, 1.0F);
      RenderUtil.drawImage(context, iconPath, x + 3.0F, y + 3.0F, 8.0F, 8.0F, 0.0F, icon, 1.0F);
   }

   private void renderRowButton(
      GuiGraphicsExtractor context, UiAnimationEngine animation, String key, String label, float x, float y, int mouseX, int mouseY, float alpha
   ) {
      boolean hovered = this.isHovered(mouseX, mouseY, x, y, 28.0F, 12.0F);
      float hover = animation.animate01("cfg:rowbtn:hover:" + key, hovered ? 1.0F : 0.0F, 14.0F, 0.0F);
      int bg = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_BACKGROUND, alpha * (0.62F + hover * 0.18F));
      int border = ColorUtil.multiplyAlpha(ClientColors.SETTINGS_OUTLINE, alpha * 0.92F);
      int text = ColorUtil.multiplyAlpha(ColorUtil.interpolate(ClientColors.CATEGORY_INACTIVE, ClientColors.TEXT_PRIMARY, hover * 0.75F), alpha);
      RenderUtil.drawRoundRect(context, x, y, 28.0F, 12.0F, 2.5F, bg);
      RenderUtil.drawBorder(context, x, y, 28.0F, 12.0F, 2.5F, border, 0.0F, 0.5F, 1.0F);
      float tx = x + (28.0F - RenderUtil.getTextWidth(label, GFonts.MEDIUM, 6)) * 0.5F;
      RenderUtil.drawText(context, label, tx, y + 4.0F, text, GFonts.MEDIUM, 6);
   }

   private void startEditing(String name) {
      this.commitEditingIfNeeded();
      this.editingConfigName = name;
      this.editingText = name;
      this.caretIndex = this.editingText.length();
      this.selectionIndex = this.caretIndex;
   }

   private void commitEditingIfNeeded() {
      if (this.editingConfigName != null) {
         String from = this.editingConfigName;
         String to = this.editingText == null ? "" : this.editingText.trim();
         if (!from.equalsIgnoreCase(to) && !from.equalsIgnoreCase("auto")) {
            boolean renamed = Havoc.getInstance().getConfigManager().renameConfig(from, to);
            if (renamed) {
               this.selectedConfigName = to;
            } else {
               this.selectedConfigName = from;
            }
         } else {
            this.selectedConfigName = from;
         }

         this.cancelEditing();
      }
   }

   private void cancelEditing() {
      this.editingConfigName = null;
      this.editingText = null;
      this.caretIndex = 0;
      this.selectionIndex = 0;
      this.selectingWithMouse = false;
   }

   private boolean isEditingAuto() {
      return this.editingConfigName != null && this.editingConfigName.equalsIgnoreCase("auto");
   }

   private boolean hasSelection() {
      return this.caretIndex != this.selectionIndex;
   }

   private boolean deleteSelection() {
      if (!this.hasSelection()) {
         return false;
      }

      int start = Math.min(this.caretIndex, this.selectionIndex);
      int end = Math.max(this.caretIndex, this.selectionIndex);
      this.editingText = this.editingText.substring(0, start) + this.editingText.substring(end);
      this.caretIndex = start;
      this.selectionIndex = start;
      return true;
   }

   private void insertText(String text) {
      if (text != null && !text.isEmpty()) {
         this.deleteSelection();
         String safe = text.replaceAll("[\\r\\n\\t]", " ");
         this.editingText = this.editingText.substring(0, this.caretIndex) + safe + this.editingText.substring(this.caretIndex);
         this.caretIndex = this.caretIndex + safe.length();
         this.selectionIndex = this.caretIndex;
      }
   }

   private void moveCaret(int delta, boolean selecting) {
      int next = this.clampInt(this.caretIndex + delta, 0, this.editingText.length());
      this.setCaret(next, selecting);
   }

   private void setCaret(int next, boolean selecting) {
      this.caretIndex = this.clampInt(next, 0, this.editingText.length());
      if (!selecting) {
         this.selectionIndex = this.caretIndex;
      }
   }

   private void copySelection() {
      if (this.hasSelection()) {
         int start = Math.min(this.caretIndex, this.selectionIndex);
         int end = Math.max(this.caretIndex, this.selectionIndex);
         this.setClipboard(this.editingText.substring(start, end));
      }
   }

   private int indexFromMouseX(String text, float textStartX, float mouseX) {
      if (text != null && !text.isEmpty() && !(mouseX <= textStartX)) {
         float localX = mouseX - textStartX;
         int best = text.length();

         for (int i = 1; i <= text.length(); i++) {
            float w = RenderUtil.getTextWidth(text.substring(0, i), GFonts.MEDIUM, 8);
            if (localX < w) {
               best = i - 1;
               float prevW = RenderUtil.getTextWidth(text.substring(0, best), GFonts.MEDIUM, 8);
               if (Math.abs(localX - w) < Math.abs(localX - prevW)) {
                  best = i;
               }

               return this.clampInt(best, 0, text.length());
            }
         }

         return text.length();
      } else {
         return 0;
      }
   }

   private boolean isCtrlDown() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.getWindow() != null) {
         long handle = mc.getWindow().handle();
         return GLFW.glfwGetKey(handle, 341) == 1 || GLFW.glfwGetKey(handle, 345) == 1;
      } else {
         return false;
      }
   }

   private boolean isShiftDown() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.getWindow() != null) {
         long handle = mc.getWindow().handle();
         return GLFW.glfwGetKey(handle, 340) == 1 || GLFW.glfwGetKey(handle, 344) == 1;
      } else {
         return false;
      }
   }

   private String getClipboard() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.keyboardHandler != null) {
         String value = mc.keyboardHandler.getClipboard();
         return value == null ? "" : value;
      } else {
         return "";
      }
   }

   private void setClipboard(String value) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null && mc.keyboardHandler != null) {
         mc.keyboardHandler.setClipboard(value == null ? "" : value);
      }
   }

   private boolean hitTopButton(float x, float y, MouseButtonEvent click) {
      return this.isHovered(click.x(), click.y(), x, y, 14.0F, 14.0F);
   }

   private ConfigsSection.EntryLayout rowLayout(ConfigsSection.Layout layout, int index, float currentScroll) {
      float y = layout.listY + index * 30.0F - currentScroll;
      float rowX = layout.listX + 2.0F;
      float rowW = layout.listW - 4.0F;
      float buttonsBlockW = 92.0F;
      float deleteX = rowX + rowW - 6.0F - 28.0F;
      float saveX = deleteX - 4.0F - 28.0F;
      float loadX = saveX - 4.0F - 28.0F;
      float btnY = y + 6.0F;
      float nameX = rowX + 6.0F;
      float nameW = Math.max(16.0F, loadX - 8.0F - nameX);
      return new ConfigsSection.EntryLayout(rowX, y, rowW, 24.0F, nameX, nameW, loadX, saveX, deleteX, btnY);
   }

   private ConfigsSection.Layout layout(float panelX, float panelY, float panelWidth, float panelHeight) {
      float contentX = panelX + 12.0F;
      float contentW = panelWidth - 24.0F;
      float headerY = panelY + 14.0F;
      float topButtonsW = 54.0F;
      float btnStartX = contentX + contentW - topButtonsW;
      float listX = contentX;
      float listY = headerY + 16.0F + 8.0F;
      float listW = contentW;
      float listH = Math.max(24.0F, panelY + panelHeight - 10.0F - listY);
      return new ConfigsSection.Layout(contentX, headerY, listX, listY, listW, listH, btnStartX, btnStartX + 14.0F + 6.0F, btnStartX + 40.0F);
   }

   private int clampInt(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   private float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private boolean isHovered(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   private record EntryLayout(float x, float y, float w, float h, float nameTextX, float nameTextW, float loadX, float saveX, float deleteX, float btnY) {
   }

   private record Layout(
      float contentX, float headerY, float listX, float listY, float listW, float listH, float createBtnX, float reloadBtnX, float folderBtnX
   ) {
   }
}
