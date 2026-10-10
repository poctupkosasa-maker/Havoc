package havoc.screen.screen.elements;

import havoc.Havoc;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.SelectSetting;
import havoc.notification.NotificationManager;
import havoc.notification.NotificationType;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;

public final class NotificationsHudElement extends AbstractHudElement {
   private static final String POSITION_CENTER = "Центр экрана";
   private static final String POSITION_BOTTOM_RIGHT = "Справа снизу";
   private static final int DISABLED_COLOR = -10658467;
   private static final long PREVIEW_ID = Long.MIN_VALUE;
   private static final long BORDER_EXIT_FADE_MS = 120L;
   private final BooleanSetting potionEndingWarnings = this.addSetting(new BooleanSetting("Potion ending warnings", true));
   private final SelectSetting position = this.addSetting(new SelectSetting("Расположение", "Центр экрана", List.of("Центр экрана", "Справа снизу")));
   private final AnimatedValue visibilityAnim = new AnimatedValue();
   private final AnimatedValue previewAlphaAnim = new AnimatedValue();
   private final Map<Long, NotificationsHudElement.RowState> rowStates = new HashMap<>();
   private boolean active = true;
   private boolean positionInitialized;
   private int lastScreenWidth = -1;

   public NotificationsHudElement() {
      super("notifications", "Notifications", 0.0F, 12.0F, 200.0F, 16.0F);
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
   public boolean canInteractInEditor(Minecraft mc) {
      return mc != null && mc.gui.screen() instanceof ChatScreen;
   }

   @Override
   public boolean allowHorizontalDrag() {
      return false;
   }

   @Override
   public boolean allowVerticalDrag() {
      return false;
   }

   public boolean isPotionEndingWarningsEnabled() {
      return this.potionEndingWarnings.isValue();
   }

   @Override
   public void render(GuiGraphicsExtractor context, int screenWidth, int screenHeight, float tickDelta) {
      long nowNano = System.nanoTime();
      long nowMs = System.currentTimeMillis();
      if (!this.positionInitialized || this.lastScreenWidth != screenWidth || !Float.isFinite(this.getX()) || !Float.isFinite(this.getY())) {
         this.setPosition((screenWidth - this.getWidth()) * 0.5F, this.getY());
         this.positionInitialized = true;
         this.lastScreenWidth = screenWidth;
      }

      Havoc havoc = Havoc.getInstance();
      if (havoc != null && havoc.getNotificationManager() != null) {
         NotificationManager manager = havoc.getNotificationManager();
         List<NotificationManager.Notification> snapshot = new ArrayList<>(manager.snapshot());
         snapshot.sort(Comparator.comparingLong(NotificationManager.Notification::id));
         Minecraft mc = Minecraft.getInstance();
         boolean chatOpen = mc != null && mc.gui.screen() instanceof ChatScreen;
         List<NotificationsHudElement.ActiveRow> activeRows = new ArrayList<>();
         Set<Long> aliveIds = new HashSet<>();

         for (NotificationManager.Notification n : snapshot) {
            boolean alive = n.expiresAt() > nowMs;
            if (alive) {
               aliveIds.add(n.id());
               activeRows.add(
                  new NotificationsHudElement.ActiveRow(n.id(), n.message(), n.styledMessage(), n.type(), n.moduleEnabled(), false, 0.0F, n.expiresAt())
               );
            }
         }

         NotificationsHudElement.PreviewTypes preview = chatOpen ? this.getPreviewTypes(nowMs) : null;
         boolean spawnNewFromBottom = this.getY() <= screenHeight * 0.5F;
         activeRows.sort(Comparator.comparingLong(NotificationsHudElement.ActiveRow::id));
         float textSize = 6.5F;
         float iconSize = 7.0F;
         float padX = 4.5F;
         float rowH = 16.0F;
         float gap = 2.5F;
         float iconGap = 3.5F;
         float rowStride = rowH + gap;
         int maxRows = Math.max(1, (int)Math.floor((screenHeight - this.getY() - 6.0F) / rowStride));
         if (activeRows.size() > maxRows) {
            activeRows = new ArrayList<>(activeRows.subList(activeRows.size() - maxRows, activeRows.size()));
         }

         int rowsCount = activeRows.size();

         for (int i = 0; i < rowsCount; i++) {
            NotificationsHudElement.ActiveRow row = activeRows.get(i);
            int targetIndex = spawnNewFromBottom ? i : rowsCount - 1 - i;
            NotificationsHudElement.RowState state = this.rowStates.get(row.id());
            if (state == null) {
               state = new NotificationsHudElement.RowState();
               state.alphaAnim.setValue(0.0F);
               state.indexAnim.setValue(targetIndex);
               this.rowStates.put(row.id(), state);
            }

            state.message = row.message();
            state.styledMessage = row.styledMessage();
            state.type = row.type();
            state.moduleEnabled = row.moduleEnabled();
            state.isPreview = row.isPreview();
            state.previewBlend = row.previewBlend();
            state.expiresAtMs = row.expiresAtMs();
            if (state.createdAtMs == 0L) {
               state.createdAtMs = nowMs;
            }

            state.seen = true;
            state.targetIndex = targetIndex;
         }

         List<NotificationsHudElement.VisualRow> visuals = new ArrayList<>();
         float maxW = 0.0F;

         for (Entry<Long, NotificationsHudElement.RowState> e : this.rowStates.entrySet()) {
            long id = e.getKey();
            NotificationsHudElement.RowState state = e.getValue();
            boolean alive = aliveIds.contains(id) && this.active;
            if (!alive) {
               if (!state.exiting) {
                  state.exiting = true;
                  state.exitStartedAtMs = nowMs;
               }
            } else {
               state.exiting = false;
               state.exitStartedAtMs = 0L;
            }

            float exitAgeMs = state.exiting ? Math.max(0.0F, (float)(nowMs - state.exitStartedAtMs)) : 0.0F;
            float borderPhase = state.exiting ? 1.0F - ColorUtil.clamp01(exitAgeMs / 120.0F) : 1.0F;
            float alphaTarget = state.exiting && exitAgeMs < 120.0F ? 1.0F : (alive ? 1.0F : 0.0F);
            float a = state.alphaAnim.update(alphaTarget, 0.09F, nowNano);
            float yIdx = state.indexAnim.update(state.targetIndex, 0.11F, nowNano);
            if (!(a <= 0.01F) || alive) {
               float contentW = RenderUtil.getTextWidth(state.message, GFonts.MEDIUM, textSize);
               float rowW = Math.max(82.0F, padX + 11.0F + iconGap + contentW + padX);
               rowW = Math.min(screenWidth - 20.0F, rowW);
               maxW = Math.max(maxW, rowW);
               if (a > 0.01F) {
                  float lifetime = Math.max(1.0F, (float)(state.expiresAtMs - state.createdAtMs));
                  float lifeProgress = state.expiresAtMs <= 0L ? 1.0F : ColorUtil.clamp01((float)(state.expiresAtMs - nowMs) / lifetime);
                  visuals.add(
                     new NotificationsHudElement.VisualRow(
                        id,
                        state.message,
                        state.styledMessage,
                        state.type,
                        state.moduleEnabled,
                        state.isPreview,
                        state.previewBlend,
                        yIdx,
                        a,
                        borderPhase,
                        rowW,
                        lifeProgress
                     )
                  );
               }
            } else if (id != Long.MIN_VALUE) {
               manager.remove(id);
            }
         }

         this.rowStates.entrySet().removeIf(e -> {
            long id = e.getKey();
            NotificationsHudElement.RowState s = e.getValue();
            return !aliveIds.contains(id) && s.alphaAnim.getValue() <= 0.01F;
         });

         for (NotificationsHudElement.RowState state : this.rowStates.values()) {
            state.seen = false;
         }

         float visibility = this.visibilityAnim.update(!this.active && !chatOpen ? 0.0F : 1.0F, 0.09F, nowNano);
         float previewAlpha = this.previewAlphaAnim.update(preview != null ? 1.0F : 0.0F, 0.09F, nowNano);
         boolean hasPreviewVisual = previewAlpha > 0.01F;
         if ((!visuals.isEmpty() || hasPreviewVisual) && !(visibility <= 0.01F)) {
            visuals.sort(Comparator.comparingDouble(NotificationsHudElement.VisualRow::animatedIndex));
            if (maxW <= 0.0F) {
               maxW = 105.0F;
            }

            float stackOffsetY = hasPreviewVisual ? rowStride : 0.0F;
            float totalH = (hasPreviewVisual ? rowH + gap : 0.0F) + visuals.size() * rowH + Math.max(0, visuals.size() - 1) * gap;
            float interactionW = maxW;
            float interactionH = totalH;
            if (chatOpen && hasPreviewVisual) {
               float previewW = Math.max(82.0F, padX + 10.0F + iconGap + RenderUtil.getTextWidth("Тестовое уведомление", GFonts.MEDIUM, textSize) + padX);
               previewW = Math.min(screenWidth - 20.0F, previewW);
               interactionW = previewW;
               interactionH = rowH;
            }

            this.setBounds(interactionW, interactionH);
            this.alignPosition(screenWidth, screenHeight, interactionH);
            float stackY = this.getY();
            float stackCenter = this.getX() + this.getWidth() * 0.5F;
            if (hasPreviewVisual) {
               float previewW = Math.max(82.0F, padX + 10.0F + iconGap + RenderUtil.getTextWidth("Тестовое уведомление", GFonts.MEDIUM, textSize) + padX);
               previewW = Math.min(screenWidth - 20.0F, previewW);
               float previewCenter = this.getX() + this.getWidth() * 0.5F;
               float previewX = previewCenter - previewW * 0.5F;
               float previewY = stackY;
               float alpha = visibility * previewAlpha;
               float panelAlpha = alpha;
               NotificationsHudElement.PreviewTypes p = preview != null ? preview : this.getPreviewTypes(nowMs);
               NotificationsHudElement.VisualRow previewRow = new NotificationsHudElement.VisualRow(
                  Long.MIN_VALUE, "Тестовое уведомление", null, p.from, null, true, p.blend, 0.0F, 1.0F, 1.0F, previewW, 1.0F
               );
               this.drawNotificationRow(context, previewRow, previewX, previewY, rowH, iconSize, iconGap, padX, textSize, alpha, panelAlpha);
            }

            for (NotificationsHudElement.VisualRow row : visuals) {
               float alpha = row.alpha * visibility;
               float panelAlpha = alpha;
               float borderAlpha = alpha * row.borderPhase;
               float rowY = stackY + stackOffsetY + row.animatedIndex * rowStride;
               float reveal = HudVisualStyle.easeOutCubic(alpha);
               float rowX = stackCenter - row.width * 0.5F + (1.0F - reveal) * 8.0F;
               this.drawNotificationRow(context, row, rowX, rowY, rowH, iconSize, iconGap, padX, textSize, alpha, panelAlpha * Math.max(0.55F, borderAlpha));
            }
         } else {
            this.setBounds(200.0F, 16.0F);
            this.alignPosition(screenWidth, screenHeight, 16.0F);
         }
      } else {
         this.setBounds(200.0F, 16.0F);
         this.alignPosition(screenWidth, screenHeight, 16.0F);
      }
   }

   private void drawNotificationRow(
      GuiGraphicsExtractor context,
      NotificationsHudElement.VisualRow row,
      float x,
      float y,
      float height,
      float iconSize,
      float iconGap,
      float padX,
      float textSize,
      float alpha,
      float panelAlpha
   ) {
      boolean moduleNotification = row.type == NotificationType.MODULE;
      boolean enabled = row.moduleEnabled == null || row.moduleEnabled;
      int typeColor = this.getTypeColor(row.type, enabled);
      float effectivePanelAlpha = moduleNotification && !enabled ? Math.max(0.0F, panelAlpha - 0.2F) : panelAlpha;
      HudVisualStyle.drawGlassPanel(context, x, y, row.width, height, effectivePanelAlpha);
      float iconBoxX = x + padX;
      float iconBoxY = y + (height - 11.0F) * 0.5F;
      float iconX = iconBoxX + (11.0F - iconSize) * 0.5F;
      float iconY = iconBoxY + (11.0F - iconSize) * 0.5F;
      this.drawTypeIcon(context, row, iconX, iconY, iconSize, alpha);
      float textX = iconBoxX + 11.0F + iconGap;
      float textY = y + 5.5F;
      if (moduleNotification) {
         this.drawModuleMessage(context, row.message, textX, textY, textSize, enabled, alpha);
      } else if (row.styledMessage != null) {
         RenderUtil.drawText(
            context, row.styledMessage, textX, textY, ColorUtil.multiplyAlpha(HudVisualStyle.PRIMARY_TEXT, alpha), GFonts.MEDIUM, (int)textSize
         );
      } else {
         RenderUtil.drawText(context, row.message, textX, textY, ColorUtil.multiplyAlpha(HudVisualStyle.PRIMARY_TEXT, alpha), GFonts.MEDIUM, textSize);
      }
   }

   private String getTypeLabel(NotificationType type, Boolean moduleEnabled) {
      return switch (type) {
         case SUCCESS -> "SUCCESS";
         case WARNING -> "WARNING";
         case MODULE -> Boolean.FALSE.equals(moduleEnabled) ? "MODULE OFF" : "MODULE ON";
         default -> "SYSTEM";
      };
   }

   private void drawTypeIcon(GuiGraphicsExtractor context, NotificationsHudElement.VisualRow row, float x, float y, float size, float alpha) {
      if (row.isPreview) {
         NotificationsHudElement.PreviewTypes preview = this.getPreviewTypes(System.currentTimeMillis());
         float inv = 1.0F - preview.blend;
         this.drawIcon(context, preview.from, x, y, size, ColorUtil.multiplyAlpha(this.getTypeColor(preview.from, true), alpha * inv));
         this.drawIcon(context, preview.to, x, y, size, ColorUtil.multiplyAlpha(this.getTypeColor(preview.to, true), alpha * preview.blend));
      } else {
         boolean enabled = row.moduleEnabled == null || row.moduleEnabled;
         int color = this.getTypeColor(row.type, enabled);
         if (row.type == NotificationType.MODULE) {
            int iconColor = enabled ? (ClientTheme.isLightMode() ? -16777216 : -1) : -10658467;
            RenderUtil.drawText(context, enabled ? "B" : "C", x + 0.5F, y + 0.6F, ColorUtil.multiplyAlpha(iconColor, alpha), GFonts.NOTIFICATION_ICONS, size);
         } else {
            this.drawIcon(context, row.type, x, y, size, ColorUtil.multiplyAlpha(color, alpha));
         }
      }
   }

   private void drawIcon(GuiGraphicsExtractor context, NotificationType type, float x, float y, float size, int color) {
      String path = switch (type) {
         case SUCCESS -> "textures/gui/icons/success.png";
         case WARNING -> "textures/gui/icons/warning.png";
         case MODULE -> "textures/gui/icons/frame.png";
         default -> "textures/gui/icons/info.png";
      };
      RenderUtil.drawImage(context, path, x, y, size, size, 0.0F, color, 1.0F);
   }

   private int getTypeColor(NotificationType type, boolean moduleEnabled) {
      return switch (type) {
         case SUCCESS -> ColorUtil.rgba(82, 194, 118, 255);
         case WARNING -> ColorUtil.rgba(238, 178, 88, 255);
         case MODULE -> moduleEnabled ? ClientColors.CATEGORY_ACCENT : -10658467;
         default -> ClientColors.TEXT_PRIMARY;
      };
   }

   private NotificationsHudElement.PreviewTypes getPreviewTypes(long nowMs) {
      NotificationType[] cycle = new NotificationType[]{NotificationType.INFO, NotificationType.SUCCESS, NotificationType.WARNING, NotificationType.MODULE};
      long step = 800L;
      int idx = (int)(nowMs / step % cycle.length);
      int next = (idx + 1) % cycle.length;
      float blend = (float)(nowMs % step) / (float)step;
      return new NotificationsHudElement.PreviewTypes(cycle[idx], cycle[next], blend);
   }

   private void drawModuleMessage(GuiGraphicsExtractor context, String message, float x, float y, float size, boolean enabled, float alpha) {
      int split = message.indexOf(32);
      String moduleName = split < 0 ? message : message.substring(0, split);
      String suffix = split < 0 ? "" : message.substring(split);
      if (enabled) {
         this.drawModuleGradientText(context, moduleName, x, y, size, alpha);
      } else {
         RenderUtil.drawText(context, moduleName, x, y, ColorUtil.multiplyAlpha(-10658467, alpha), GFonts.MEDIUM, size);
      }

      float suffixX = x + RenderUtil.getTextWidth(moduleName, GFonts.MEDIUM, size);
      int suffixColor = enabled ? HudVisualStyle.PRIMARY_TEXT : -10658467;
      RenderUtil.drawText(context, suffix, suffixX, y, ColorUtil.multiplyAlpha(suffixColor, alpha), GFonts.MEDIUM, size);
   }

   private void drawModuleGradientText(GuiGraphicsExtractor context, String text, float x, float y, float size, float alpha) {
      long nowMs = System.currentTimeMillis();
      float cursor = x;

      for (int i = 0; i < text.length(); i++) {
         String glyph = String.valueOf(text.charAt(i));
         int angle = Math.floorMod((int)(nowMs / 7L - i * 22L), 360);
         angle = angle >= 180 ? 360 - angle : angle;
         int color = ColorUtil.interpolate(ClientColors.CATEGORY_ACCENT, ClientColors.CATEGORY_ACCENT_SECOND, angle / 180.0F);
         RenderUtil.drawText(context, glyph, cursor, y, ColorUtil.multiplyAlpha(color, alpha), GFonts.MEDIUM, size);
         cursor += RenderUtil.getTextWidth(glyph, GFonts.MEDIUM, size);
      }
   }

   private void alignPosition(int screenWidth, int screenHeight, float height) {
      float targetX;
      float targetY;
      if ("Справа снизу".equals(this.position.getValue())) {
         targetX = Math.max(4.0F, screenWidth - this.getWidth() - 8.0F);
         targetY = Math.max(4.0F, screenHeight - height - 38.0F);
      } else {
         targetX = (screenWidth - this.getWidth()) * 0.5F;
         targetY = (screenHeight - height) * 0.5F + 28.0F;
      }

      if (Math.abs(this.getX() - targetX) > 0.01F || Math.abs(this.getY() - targetY) > 0.01F) {
         this.setPosition(targetX, targetY);
      }
   }

   private record ActiveRow(
      long id, String message, Component styledMessage, NotificationType type, Boolean moduleEnabled, boolean isPreview, float previewBlend, long expiresAtMs
   ) {
   }

   private record PreviewTypes(NotificationType from, NotificationType to, float blend) {
   }

   private static final class RowState {
      private final AnimatedValue alphaAnim = new AnimatedValue();
      private final AnimatedValue indexAnim = new AnimatedValue();
      private String message = "";
      private Component styledMessage;
      private NotificationType type = NotificationType.INFO;
      private Boolean moduleEnabled;
      private boolean seen;
      private boolean isPreview;
      private int targetIndex;
      private float previewBlend;
      private boolean exiting;
      private long exitStartedAtMs;
      private long createdAtMs;
      private long expiresAtMs;
   }

   private record VisualRow(
      long id,
      String message,
      Component styledMessage,
      NotificationType type,
      Boolean moduleEnabled,
      boolean isPreview,
      float previewBlend,
      float animatedIndex,
      float alpha,
      float borderPhase,
      float width,
      float lifeProgress
   ) {
   }
}
