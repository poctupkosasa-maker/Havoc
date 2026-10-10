package havoc.screen.menu.sections;

import havoc.Havoc;
import havoc.module.impl.visuals.ItemReplacer;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.screen.menu.UiAnimationEngine;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;

public final class CosmeticsSection {
   private static final float PAD = 12.0F;
   private static final float TAB_H = 18.0F;
   private static final float TAB_GAP = 5.0F;
   private static final float CARD_GAP = 6.0F;
   private static final float CARD_H = 82.0F;
   private static final float SCROLL_STEP = 34.0F;
   private static final List<String> TABS = List.of("Мечи");
   private float scrollTarget;
   private float scroll;

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
      CosmeticsSection.Layout l = this.layout(panelX, panelY, panelWidth, panelHeight);
      RenderUtil.drawText(context, "Cosmetics", l.x, l.titleY, ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha), GFonts.MEDIUM, 9.0F);
      float tabX = l.x;

      for (int i = 0; i < TABS.size(); i++) {
         float w = this.tabWidth(TABS.get(i));
         boolean selected = true;
         boolean hovered = this.hovered(mouseX, mouseY, tabX, l.tabsY, w, 18.0F);
         float hoverState = animation.animate01("cosmetics:tab:hover:" + i, hovered ? 1.0F : 0.0F, 12.0F, 0.0F);
         int idleText = ColorUtil.interpolate(ClientColors.CATEGORY_INACTIVE, ClientColors.TEXT_PRIMARY, hoverState * 0.36F);
         float textW = RenderUtil.getTextWidth(TABS.get(i), GFonts.MEDIUM, 6.5F);
         RenderUtil.drawText(
            context,
            TABS.get(i),
            tabX + (w - textW) * 0.5F,
            l.tabsY + 6.0F,
            ColorUtil.multiplyAlpha(selected ? ClientColors.TEXT_PRIMARY : idleText, alpha),
            GFonts.MEDIUM,
            6.5F
         );
         tabX += w + 5.0F;
      }

      RenderUtil.drawRoundRect(context, l.x, l.bodyY, l.w, l.bodyH, 6.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND, alpha * 0.72F));
      RenderUtil.drawBorder(
         context, l.x, l.bodyY, l.w, l.bodyH, 6.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND_OUTLINE, alpha), 1.0F, 0.0F, 0.5F
      );
      this.renderSwords(context, animation, l, mouseX, mouseY, alpha);
   }

   private void renderSwords(GuiGraphicsExtractor context, UiAnimationEngine animation, CosmeticsSection.Layout l, int mouseX, int mouseY, float alpha) {
      String[] labels = ItemReplacer.getVariantLabels();
      int columns = 3;
      float cardW = (l.w - 8.0F - 6.0F * (columns - 1)) / columns;
      int itemCount = labels.length + 1;
      int rows = (itemCount + columns - 1) / columns;
      float contentH = rows * 82.0F + Math.max(0, rows - 1) * 6.0F;
      float maxScroll = Math.max(0.0F, contentH - (l.bodyH - 8.0F));
      this.scrollTarget = this.clamp(this.scrollTarget, 0.0F, maxScroll);
      this.scroll = animation.animate("cosmetics:swords:scroll", this.scrollTarget, 24.0F, this.scroll);
      ItemReplacer replacer = Havoc.getInstance().getModuleManager().getModule(ItemReplacer.class);
      String selected = replacer == null ? "" : replacer.getSelectedVariantLabel();
      context.enableScissor((int)l.x + 1, (int)l.bodyY + 1, (int)(l.x + l.w - 1.0F), (int)(l.bodyY + l.bodyH - 1.0F));

      for (int i = 0; i < itemCount; i++) {
         int col = i % columns;
         int row = i / columns;
         float x = l.x + 4.0F + col * (cardW + 6.0F);
         float y = l.bodyY + 4.0F + row * 88.0F - this.scroll;
         if (!(y + 82.0F < l.bodyY) && !(y > l.bodyY + l.bodyH)) {
            boolean removeCard = i == 0;
            String label = removeCard ? "Убрать меч" : labels[i - 1];
            boolean isSelected = removeCard
               ? replacer == null || !replacer.isEnable()
               : replacer != null && replacer.isEnable() && label.equalsIgnoreCase(selected);
            boolean hover = this.hovered(mouseX, mouseY, x, y, cardW, 82.0F);
            float state = animation.animate01("cosmetics:sword:" + i, isSelected ? 1.0F : (hover ? 0.38F : 0.0F), 11.0F, isSelected ? 1.0F : 0.0F);
            int bg = ColorUtil.multiplyAlpha(ColorUtil.interpolate(ClientColors.SETTINGS_BACKGROUND, ClientColors.CATEGORY_ACCENT, state * 0.18F), alpha);
            int outline = ColorUtil.multiplyAlpha(ColorUtil.interpolate(ClientColors.SETTINGS_OUTLINE, ClientColors.CATEGORY_ACCENT, state), alpha);
            RenderUtil.drawRoundRect(context, x, y, cardW, 82.0F, 5.0F, bg);
            RenderUtil.drawBorder(context, x, y, cardW, 82.0F, 5.0F, outline, 0.0F, 0.5F, 1.0F);
            if (removeCard) {
               this.drawRemoveSymbol(context, x, y, cardW, alpha);
            } else {
               this.drawSwordPreview(context, label, x, y, cardW, alpha);
            }

            RenderUtil.drawTextFadeRight(
               context, label, x + 5.0F, y + 82.0F - 12.0F, ColorUtil.multiplyAlpha(ClientColors.TEXT_PRIMARY, alpha), GFonts.MEDIUM, 6.0F, cardW - 10.0F, 5.0F
            );
         }
      }

      context.disableScissor();
   }

   private void drawSwordPreview(GuiGraphicsExtractor context, String label, float x, float y, float cardW, float alpha) {
      if (!(alpha < 0.08F)) {
         Matrix3x2fStack pose = context.pose();
         pose.pushMatrix();
         float scale = 2.55F;
         pose.translate(x + cardW * 0.5F - 8.0F * scale, y + 8.0F);
         pose.scale(scale, scale);
         ItemReplacer.beginPreview(label);

         try {
            context.fakeItem(new ItemStack(Items.DIAMOND_SWORD), 0, 0);
         } finally {
            ItemReplacer.endPreview();
            pose.popMatrix();
         }
      }
   }

   private void drawRemoveSymbol(GuiGraphicsExtractor context, float x, float y, float w, float alpha) {
      float cx = x + w * 0.5F;
      float cy = y + 34.0F;
      RenderUtil.drawRoundRect(context, cx - 8.0F, cy - 1.0F, 16.0F, 2.0F, 1.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_INACTIVE, alpha));
   }

   public boolean handleClick(MouseButtonEvent click, float panelX, float panelY, float panelWidth, float panelHeight) {
      if (click.button() != 0) {
         return false;
      }

      CosmeticsSection.Layout l = this.layout(panelX, panelY, panelWidth, panelHeight);
      float tabX = l.x;

      for (int i = 0; i < TABS.size(); i++) {
         float w = this.tabWidth(TABS.get(i));
         if (this.hovered(click.x(), click.y(), tabX, l.tabsY, w, 18.0F)) {
            this.scrollTarget = 0.0F;
            return true;
         }

         tabX += w + 5.0F;
      }

      if (!this.hovered(click.x(), click.y(), l.x, l.bodyY, l.w, l.bodyH)) {
         return false;
      }

      String[] labels = ItemReplacer.getVariantLabels();
      int columns = 3;
      float cardW = (l.w - 8.0F - 6.0F * (columns - 1)) / columns;

      for (int i = 0; i < labels.length + 1; i++) {
         float x = l.x + 4.0F + i % columns * (cardW + 6.0F);
         float y = l.bodyY + 4.0F + i / columns * 88.0F - this.scroll;
         if (this.hovered(click.x(), click.y(), x, y, cardW, 82.0F)) {
            ItemReplacer replacer = Havoc.getInstance().getModuleManager().getModule(ItemReplacer.class);
            if (replacer != null) {
               if (i == 0) {
                  if (replacer.isEnable()) {
                     replacer.setEnable(false);
                  }
               } else {
                  replacer.selectVariant(labels[i - 1]);
                  if (!replacer.isEnable()) {
                     replacer.setEnable(true);
                  }
               }

               Havoc.getInstance().markConfigDirty();
            }

            return true;
         }
      }

      return true;
   }

   public boolean handleScroll(double mouseX, double mouseY, double amount, float panelX, float panelY, float panelWidth, float panelHeight) {
      CosmeticsSection.Layout l = this.layout(panelX, panelY, panelWidth, panelHeight);
      if (this.hovered(mouseX, mouseY, l.x, l.bodyY, l.w, l.bodyH)) {
         this.scrollTarget -= (float)amount * 34.0F;
         return true;
      } else {
         return false;
      }
   }

   private float tabWidth(String text) {
      return Math.max(52.0F, RenderUtil.getTextWidth(text, GFonts.MEDIUM, 6.5F) + 18.0F);
   }

   private CosmeticsSection.Layout layout(float panelX, float panelY, float panelWidth, float panelHeight) {
      float x = panelX + 12.0F;
      float w = panelWidth - 24.0F;
      float titleY = panelY + 14.0F;
      float tabsY = panelY + 32.0F;
      float bodyY = tabsY + 18.0F + 8.0F;
      return new CosmeticsSection.Layout(x, w, titleY, tabsY, bodyY, panelY + panelHeight - bodyY - 12.0F);
   }

   private boolean hovered(double mx, double my, float x, float y, float w, float h) {
      return mx >= x && mx <= x + w && my >= y && my <= y + h;
   }

   private float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private record Layout(float x, float w, float titleY, float tabsY, float bodyY, float bodyH) {
   }
}
