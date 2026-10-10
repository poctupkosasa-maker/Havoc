package havoc.screen.screen.elements;

import havoc.Havoc;
import havoc.module.impl.misc.NameProtect;
import havoc.module.setting.MultiSelectSetting;
import havoc.module.setting.SelectSetting;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.screen.screen.AbstractHudElement;
import havoc.screen.screen.HudVisualStyle;
import havoc.utilites.ServerInfoHelper;
import havoc.utilites.animation.AnimatedValue;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class LogoHudElement extends AbstractHudElement {
   private static final String POSITION_TOP = "Сверху";
   private static final String POSITION_RIGHT = "Справа";
   private static final String POSITION_LEFT = "Слева";
   private static final String POSITION_BOTTOM = "Снизу";
   private static LogoHudElement INSTANCE;
   private static final String ITEM_PLAYER = "Player";
   private static final String ITEM_PING = "Ping";
   private static final String ITEM_FPS = "FPS";
   private static final String ITEM_SERVER = "Server";
   private static final String ITEM_TPS = "TPS";
   private static final String ITEM_TIME = "Time";
   private static final String ITEM_COORDINATES = "Coordinates";
   private static final String ITEM_BPS = "BPS";
   private static final float ROW_HEIGHT = 15.5F;
   private static final float ROW_GAP = 3.0F;
   private static final float PAD_X = 5.0F;
   private static final float TEXT_SIZE = 7.0F;
   private static final float BRAND_ICON_SIZE = 16.0F;
   private static final float BRAND_ICON_GAP = 1.8F;
   private static final float BRAND_CONTENT_SHIFT_X = -3.0F;
   private static final float ICON_SIZE = 6.5F;
   private static final float ICON_GAP = 2.2F;
   private static final float DIVIDER_GAP = 3.2F;
   private static final float GROUP_GAP = 3.0F;
   private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
   private final MultiSelectSetting content = this.addSetting(
      new MultiSelectSetting(
         "Display",
         List.of("Player", "Ping", "FPS", "Server", "TPS", "Time", "Coordinates", "BPS"),
         new LinkedHashSet<>(List.of("Player", "Ping", "FPS", "Server", "TPS", "Time", "Coordinates", "BPS"))
      )
   );
   private final SelectSetting position = this.addSetting(new SelectSetting("Position", "Сверху", List.of("Сверху", "Справа", "Слева", "Снизу")));
   private final AnimatedValue firstWidthAnim = new AnimatedValue();
   private final AnimatedValue secondWidthAnim = new AnimatedValue();
   private final AnimatedValue visibilityAnim = new AnimatedValue();
   private final AnimatedValue fpsAnim = new AnimatedValue();
   private final AnimatedValue pingAnim = new AnimatedValue();
   private final AnimatedValue tpsAnim = new AnimatedValue();
   private final AnimatedValue bpsAnim = new AnimatedValue();
   private final List<LogoHudElement.Segment> firstRowScratch = new ArrayList<>(5);
   private final List<LogoHudElement.Segment> secondRowScratch = new ArrayList<>(3);
   private boolean active = true;

   public LogoHudElement() {
      super("logo", "Logo", 12.0F, 12.0F, 150.0F, 34.0F);
      INSTANCE = this;
   }

   @Override
   public boolean allowHorizontalDrag() {
      return false;
   }

   @Override
   public boolean allowVerticalDrag() {
      return false;
   }

   public static boolean isTopPosition() {
      return INSTANCE != null && "Сверху".equals(INSTANCE.position.getValue()) && INSTANCE.active;
   }

   public static boolean isBottomPosition() {
      return INSTANCE != null && "Снизу".equals(INSTANCE.position.getValue()) && INSTANCE.active;
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
      float visible = this.visibilityAnim.update(this.active ? 1.0F : 0.0F, 0.1F, now);
      if (visible <= 0.01F) {
         this.setBounds(0.0F, 0.0F);
      } else {
         Minecraft mc = Minecraft.getInstance();
         String playerName = "private";
         NameProtect nameProtect = Havoc.getInstance().getModuleManager().getModule(NameProtect.class);
         if (nameProtect != null) {
            playerName = nameProtect.replace(playerName);
         }

         int ping = 0;
         if (mc != null && mc.player != null && mc.getConnection() != null) {
            PlayerInfo entry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (entry != null) {
               ping = Math.max(0, entry.getLatency());
            }
         }

         float fps = mc != null ? Math.max(0, mc.getFps()) : 0.0F;
         float tps = ServerInfoHelper.getTps();
         double rawBps = mc != null && mc.player != null ? mc.player.getDeltaMovement().horizontalDistance() * 20.0 : 0.0;
         int xPos = mc != null && mc.player != null ? (int)Math.floor(mc.player.getX()) : 0;
         int yPos = mc != null && mc.player != null ? (int)Math.floor(mc.player.getY()) : 0;
         int zPos = mc != null && mc.player != null ? (int)Math.floor(mc.player.getZ()) : 0;
         String fpsText = Long.toString(Math.round(this.fpsAnim.update(fps, 0.05F, now)));
         String pingText = Long.toString(Math.round(this.pingAnim.update(ping, 0.05F, now)));
         String tpsText = String.format(Locale.ROOT, "%.1f", this.tpsAnim.update(tps, 0.05F, now));
         String bpsText = String.format(Locale.ROOT, "%.1f", this.bpsAnim.update((float)rawBps, 0.08F, now));
         String timeText = LocalTime.now().format(TIME_FORMATTER);
         Set<String> selected = this.content.getSelectedValues();
         this.firstRowScratch.clear();
         this.secondRowScratch.clear();
         List<LogoHudElement.Segment> firstRow = this.firstRowScratch;
         if (selected.contains("Player")) {
            firstRow.add(this.plainSegment("textures/gui/icons/player.png", playerName));
         }

         if (selected.contains("Server")) {
            firstRow.add(this.plainSegment("textures/gui/icons/cloud.png", ServerInfoHelper.getServerName()));
         }

         if (selected.contains("FPS")) {
            firstRow.add(this.unitSegment("textures/gui/icons/computer.png", fpsText, "fps"));
         }

         if (selected.contains("Ping")) {
            firstRow.add(this.unitSegment("textures/gui/icons/wifi.png", pingText, "ms"));
         }

         if (selected.contains("Time")) {
            firstRow.add(this.plainSegment("textures/gui/icons/time.png", timeText));
         }

         List<LogoHudElement.Segment> secondRow = this.secondRowScratch;
         if (selected.contains("Coordinates")) {
            secondRow.add(this.coordinatesSegment(xPos, yPos, zPos));
         }

         if (selected.contains("BPS")) {
            secondRow.add(this.unitSegment("textures/gui/icons/movement.png", bpsText, "bps"));
         }

         if (selected.contains("TPS")) {
            secondRow.add(this.unitSegment("textures/gui/icons/misc.png", tpsText, "tps"));
         }

         float brandWidth = 27.8F + RenderUtil.getTextWidth("Havoc", GFonts.SEMIBOLD, 7.0F);
         float firstTargetWidth = firstRow.isEmpty() ? 0.0F : this.measureRow(firstRow);
         float secondTargetWidth = secondRow.isEmpty() ? 0.0F : this.measureRow(secondRow);
         float firstWidth = this.firstWidthAnim.update(firstTargetWidth, 0.13F, now);
         float secondWidth = this.secondWidthAnim.update(secondTargetWidth, 0.13F, now);
         boolean hasSecondRow = secondWidth > 0.5F;
         boolean bottom = "Снизу".equals(this.position.getValue());
         float totalHeight = bottom ? 15.5F : 15.5F + (hasSecondRow ? 18.5F : 0.0F);
         float topWidth = brandWidth + (firstWidth > 0.5F ? 3.0F + firstWidth : 0.0F);
         float bottomExtra = bottom && secondWidth > 0.5F ? 3.0F + secondWidth : 0.0F;
         float layoutWidth = bottom ? topWidth + bottomExtra : Math.max(topWidth, secondWidth);
         this.setBounds(layoutWidth, totalHeight);
         float layoutX;
         float layoutY;
         switch (this.position.getValue()) {
            case "Справа":
               layoutX = Math.max(2.0F, screenWidth - layoutWidth - 12.0F);
               layoutY = 12.0F;
               break;
            case "Слева":
               layoutX = 12.0F;
               layoutY = 12.0F;
               break;
            case "Снизу":
               layoutX = Math.max(2.0F, (screenWidth - layoutWidth) * 0.5F);
               layoutY = Math.max(2.0F, screenHeight - 15.5F - 2.0F);
               break;
            default:
               layoutX = Math.max(2.0F, (screenWidth - layoutWidth) * 0.5F);
               layoutY = 12.0F;
         }

         super.setPosition(layoutX, layoutY);
         float panelAlpha = Math.min(1.0F, visible / 0.34F);
         this.drawBrand(context, layoutX, layoutY, brandWidth, visible, panelAlpha);
         if (firstWidth > 0.5F) {
            this.drawRow(context, firstRow, layoutX + brandWidth + 3.0F, layoutY, firstWidth, visible, panelAlpha);
         }

         if (hasSecondRow) {
            float secondY;
            float secondX;
            if (bottom) {
               secondX = layoutX + topWidth + 3.0F;
               secondY = layoutY;
            } else {
               secondX = switch (this.position.getValue()) {
                  case "Сверху" -> layoutX + (topWidth - secondWidth) * 0.5F;
                  case "Справа" -> layoutX + Math.max(0.0F, topWidth - secondWidth);
                  default -> layoutX;
               };
               secondY = layoutY + 15.5F + 3.0F;
            }

            this.drawRow(context, secondRow, secondX, secondY, secondWidth, visible, panelAlpha);
         }
      }
   }

   private void drawBrand(GuiGraphicsExtractor context, float x, float y, float width, float alpha, float panelAlpha) {
      HudVisualStyle.drawGlassPanel(context, x, y, width, 15.5F, panelAlpha);
      long nowMs = System.currentTimeMillis();
      int iconColor = this.brandGradientColor(nowMs, 0);
      RenderUtil.drawRotatedImage(
         context, "textures/gui/logo.png", x + 5.0F + -3.0F, y + -0.25F, 16.0F, 16.0F, 0.0F, 0.0F, ColorUtil.multiplyAlpha(iconColor, alpha), 1.0F
      );
      this.drawBrandGradientText(context, "Havoc", x + 5.0F + 16.0F + 1.8F + -3.0F, y + 4.9F, alpha, nowMs);
   }

   private void drawBrandGradientText(GuiGraphicsExtractor context, String text, float x, float y, float alpha, long nowMs) {
      float cursor = x;

      for (int i = 0; i < text.length(); i++) {
         String glyph = String.valueOf(text.charAt(i));
         int color = ColorUtil.multiplyAlpha(this.brandGradientColor(nowMs, i + 1), alpha);
         RenderUtil.drawText(context, glyph, cursor, y, color, GFonts.SEMIBOLD, 7.0F);
         cursor += RenderUtil.getTextWidth(glyph, GFonts.SEMIBOLD, 7.0F);
      }
   }

   private int brandGradientColor(long nowMs, int index) {
      int angle = Math.floorMod((int)(nowMs / 6L - index * 22L), 360);
      angle = angle >= 180 ? 360 - angle : angle;
      return ColorUtil.interpolate(ClientColors.CATEGORY_ACCENT, ClientColors.CATEGORY_ACCENT_SECOND, angle / 180.0F);
   }

   private float measureRow(List<LogoHudElement.Segment> segments) {
      float width = 10.0F;

      for (int i = 0; i < segments.size(); i++) {
         LogoHudElement.Segment segment = segments.get(i);
         if (i > 0) {
            width += 7.05F;
         }

         if (segment.iconPath() != null) {
            width += 8.7F;
         }

         for (LogoHudElement.TextPart part : segment.parts()) {
            width += RenderUtil.getTextWidth(part.text(), GFonts.MEDIUM, 7.0F);
         }
      }

      return width;
   }

   private void drawRow(GuiGraphicsExtractor context, List<LogoHudElement.Segment> segments, float x, float y, float width, float alpha, float panelAlpha) {
      HudVisualStyle.drawGlassPanel(context, x, y, width, 15.5F, panelAlpha);
      float cursor = x + 5.0F;
      float textY = y + 4.9F;
      float iconY = y + 4.5F;
      RenderUtil.pushScissor(context, x, y, width, 15.5F);

      for (int i = 0; i < segments.size(); i++) {
         LogoHudElement.Segment segment = segments.get(i);
         if (i > 0) {
            cursor += 3.2F;
            RenderUtil.drawRoundRect(context, cursor, y + 3.75F, 0.65F, 8.0F, 0.33F, ColorUtil.multiplyAlpha(ColorUtil.rgba(255, 255, 255, 34), alpha));
            cursor += 3.85F;
         }

         if (segment.iconPath() != null) {
            RenderUtil.drawImage(
               context, segment.iconPath(), cursor, iconY, 6.5F, 6.5F, 0.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, alpha), 1.0F
            );
            cursor += 8.7F;
         }

         for (LogoHudElement.TextPart part : segment.parts()) {
            int textColor = ColorUtil.multiplyAlpha(part.secondary() ? HudVisualStyle.SECONDARY_TEXT : HudVisualStyle.PRIMARY_TEXT, alpha);
            RenderUtil.drawText(context, part.text(), cursor, textY, textColor, GFonts.MEDIUM, 7.0F);
            cursor += RenderUtil.getTextWidth(part.text(), GFonts.MEDIUM, 7.0F);
         }
      }

      RenderUtil.popScissor();
   }

   private LogoHudElement.Segment plainSegment(String iconPath, String text) {
      return new LogoHudElement.Segment(iconPath, List.of(new LogoHudElement.TextPart(text, false)));
   }

   private LogoHudElement.Segment unitSegment(String iconPath, String value, String unit) {
      return new LogoHudElement.Segment(iconPath, List.of(new LogoHudElement.TextPart(value, false), new LogoHudElement.TextPart(unit, true)));
   }

   private LogoHudElement.Segment coordinatesSegment(int x, int y, int z) {
      return new LogoHudElement.Segment(
         "textures/gui/icons/target.png",
         List.of(
            new LogoHudElement.TextPart("x", true),
            new LogoHudElement.TextPart(Integer.toString(x), false),
            new LogoHudElement.TextPart(" y", true),
            new LogoHudElement.TextPart(Integer.toString(y), false),
            new LogoHudElement.TextPart(" z", true),
            new LogoHudElement.TextPart(Integer.toString(z), false)
         )
      );
   }

   private record Segment(String iconPath, List<LogoHudElement.TextPart> parts) {
   }

   private record TextPart(String text, boolean secondary) {
   }
}
