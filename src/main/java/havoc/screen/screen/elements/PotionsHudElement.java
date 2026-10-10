package havoc.screen.screen.elements;

import havoc.Havoc;
import havoc.module.impl.visuals.Interface;
import havoc.module.setting.BooleanSetting;
import havoc.notification.Notifications;
import havoc.render.RenderUtil;
import havoc.render.font.GFonts;
import havoc.screen.screen.AbstractHudElement;
import havoc.screen.screen.HudElement;
import havoc.screen.screen.HudVisualStyle;
import havoc.utilites.animation.AnimatedValue;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ClientTheme;
import havoc.utilites.color.ColorUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public final class PotionsHudElement extends AbstractHudElement {
   private final BooleanSetting alwaysShow = this.addSetting(new BooleanSetting("Always show", true));
   private static final int WARNING_THRESHOLD_TICKS = 200;
   private static final long NOTIFY_STATE_TTL_MS = 5000L;
   private static final float FIXED_WIDTH = 96.0F;
   private final AnimatedValue widthAnim = new AnimatedValue();
   private final AnimatedValue heightAnim = new AnimatedValue();
   private final AnimatedValue visibilityAnim = new AnimatedValue();
   private final Map<String, PotionsHudElement.RowState> rowStates = new HashMap<>();
   private final Map<String, PotionsHudElement.EffectNotifyState> notifyStates = new HashMap<>();
   private boolean active = true;

   public PotionsHudElement() {
      super("potions", "Potions", 12.0F, 68.0F, 96.0F, 22.0F);
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
      Minecraft mc = Minecraft.getInstance();
      List<PotionsHudElement.PotionEntry> entries = new ArrayList<>();
      if (mc != null && mc.player != null) {
         for (MobEffectInstance instance : mc.player.getActiveEffects()) {
            String effectName = this.resolveEffectName(instance);
            String durationText = this.formatDuration(instance);
            int durationTicks = instance.isInfiniteDuration() ? Integer.MAX_VALUE : Math.max(0, instance.getDuration());
            int level = instance.getAmplifier() + 1;
            String id = this.buildEffectId(instance);
            Identifier iconTexture = this.buildEffectTexturePath(instance);
            entries.add(new PotionsHudElement.PotionEntry(id, effectName, level, durationText, durationTicks, iconTexture));
         }
      }

      if (this.active && this.isWarnBeforeEndingEnabled()) {
         this.processEndingNotifications(entries, System.currentTimeMillis());
      } else {
         this.notifyStates.clear();
      }

      float padX = 7.0F;
      float titleSize = 8.5F;
      float textSize = 8.0F;
      float rowGap = 4.5F;
      float titlePadTop = 6.5F;
      float titlePadBottom = 10.5F;
      float rowPadBottom = 4.0F;
      float iconSize = 9.0F;
      float effectIconSize = 9.5F;
      float effectIconGap = 3.0F;

      for (PotionsHudElement.RowState state : this.rowStates.values()) {
         state.seen = false;
      }

      int i = 0;

      while (i < entries.size()) {
         PotionsHudElement.PotionEntry entry = entries.get(i);
         PotionsHudElement.RowState state = this.rowStates.get(entry.id());
         if (state == null) {
            state = new PotionsHudElement.RowState(entry);
            state.alphaAnim.setValue(0.0F);
            state.indexAnim.setValue(i + 0.35F);
            this.rowStates.put(entry.id(), state);
         } else {
            state.entry = entry;
         }

         state.seen = true;
         state.targetIndex = i++;
      }

      float maxLabelW = 0.0F;
      float maxTimeTextW = 0.0F;

      for (PotionsHudElement.RowState state : this.rowStates.values()) {
         PotionsHudElement.PotionEntry entry = state.entry;
         float labelW = RenderUtil.getTextWidth(this.getDisplayName(entry), GFonts.REGULAR, textSize);
         float timeW = RenderUtil.getTextWidth(entry.duration(), GFonts.REGULAR, textSize);
         maxLabelW = Math.max(maxLabelW, labelW);
         maxTimeTextW = Math.max(maxTimeTextW, timeW);
      }

      float rowHeight = RenderUtil.getTextHeight(GFonts.REGULAR, textSize);
      float baseY = titlePadTop + titleSize + titlePadBottom;
      float timeColumnWidth = maxTimeTextW + 6.0F;
      float targetWidth = padX + effectIconSize + effectIconGap + maxLabelW + 12.0F + timeColumnWidth + padX;
      targetWidth = Math.max(96.0F, targetWidth);
      float rowStride = rowHeight + rowGap;
      float maxVisualIndex = -1.0F;
      List<PotionsHudElement.RowVisual> visuals = new ArrayList<>();
      List<String> toRemove = new ArrayList<>();

      for (Entry<String, PotionsHudElement.RowState> stateEntry : this.rowStates.entrySet()) {
         PotionsHudElement.RowState state = stateEntry.getValue();
         float targetAlpha = state.seen ? 1.0F : 0.0F;
         float alpha = state.alphaAnim.update(targetAlpha, 0.05F, now);
         float animatedIndex = state.indexAnim.update(state.targetIndex, 0.06F, now);
         if (!state.seen && alpha <= 0.01F) {
            toRemove.add(stateEntry.getKey());
         } else {
            maxVisualIndex = Math.max(maxVisualIndex, animatedIndex);
            if (alpha > 0.01F) {
               visuals.add(new PotionsHudElement.RowVisual(state.entry, animatedIndex, alpha));
            }
         }
      }

      for (String key : toRemove) {
         this.rowStates.remove(key);
      }

      visuals.sort(Comparator.comparingDouble(PotionsHudElement.RowVisual::animatedIndex));
      float listHeight = maxVisualIndex >= 0.0F ? (maxVisualIndex + 1.0F) * rowStride - rowGap + rowPadBottom : 0.0F;
      float targetHeight = baseY + listHeight;
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
            context, "Potions", x + padX, y + titlePadTop, ColorUtil.multiplyAlpha(HudVisualStyle.PRIMARY_TEXT, visible), GFonts.MEDIUM, titleSize
         );
         RenderUtil.drawText(
            context,
            "I",
            x + this.getWidth() - padX - iconSize,
            y + 5.55F,
            ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, visible),
            GFonts.NOTIFICATION_ICONS,
            iconSize
         );
         int dividerColor = ClientTheme.isLightMode() ? -16777216 : -2500135;
         float dividerWidth = this.getWidth() * 0.47F;
         float dividerX = x + (this.getWidth() - dividerWidth) * 0.5F;
         float dividerY = y + baseY - 5.5F;
         HudVisualStyle.drawHairline(context, dividerX, dividerY, dividerWidth, dividerColor, visible * 0.92F);
         float cursorY = y + baseY;

         for (PotionsHudElement.RowVisual visual : visuals) {
            PotionsHudElement.PotionEntry entry = visual.entry();
            float rowY = cursorY + visual.animatedIndex() * rowStride;
            float rowAlpha = visible * visual.alpha();
            float rowReveal = HudVisualStyle.easeOutCubic(visual.alpha());
            float contentX = x + padX + (1.0F - rowReveal) * 4.0F;
            this.drawEffectIcon(context, entry.iconTexture(), contentX, rowY, effectIconSize, rowAlpha);
            float timeTextW = RenderUtil.getTextWidth(entry.duration(), GFonts.REGULAR, textSize);
            float timeTextX = x + this.getWidth() - padX - timeTextW;
            RenderUtil.drawText(
               context, entry.duration(), timeTextX, rowY + 0.5F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_ACCENT, rowAlpha), GFonts.MEDIUM, textSize
            );
            RenderUtil.drawText(
               context,
               this.getDisplayName(entry),
               contentX + effectIconSize + effectIconGap,
               rowY,
               ColorUtil.multiplyAlpha(HudVisualStyle.PRIMARY_TEXT, rowAlpha),
               GFonts.MEDIUM,
               textSize
            );
         }
      }
   }

   private String getDisplayName(PotionsHudElement.PotionEntry entry) {
      String name = entry.name().toLowerCase(Locale.ROOT);
      return entry.level() > 1 ? name + " " + entry.level() : name;
   }

   private void drawEffectIcon(GuiGraphicsExtractor context, Identifier texture, float x, float y, float size, float alpha) {
      if (texture != null) {
         RenderUtil.drawImage(context, texture, x, y - 1.5F, size, size, 0.0F, ColorUtil.multiplyAlpha(-1, alpha), 1.0F);
      }
   }

   private String resolveEffectName(MobEffectInstance instance) {
      Component text = ((MobEffect)instance.getEffect().value()).getDisplayName();
      return text == null ? "Effect" : text.getString();
   }

   private String buildEffectId(MobEffectInstance instance) {
      Holder<?> effect = instance.getEffect();
      String effectKey = effect.unwrapKey().map(key -> key.identifier().toString()).orElse("effect");
      return effectKey + ":" + instance.getAmplifier();
   }

   private Identifier buildEffectTexturePath(MobEffectInstance instance) {
      Identifier effectId = instance.getEffect().unwrapKey().map(key -> key.identifier()).orElse(Identifier.withDefaultNamespace("speed"));
      return Identifier.fromNamespaceAndPath(effectId.getNamespace(), "textures/mob_effect/" + effectId.getPath() + ".png");
   }

   private String formatDuration(MobEffectInstance instance) {
      if (instance.isInfiniteDuration()) {
         return "INF";
      }

      int ticks = Math.max(0, instance.getDuration());
      int totalSeconds = ticks / 20;
      int minutes = totalSeconds / 60;
      int seconds = totalSeconds % 60;
      return String.format("%d:%02d", minutes, seconds);
   }

   private void processEndingNotifications(List<PotionsHudElement.PotionEntry> entries, long nowMs) {
      Set<String> seenIds = new HashSet<>();

      for (PotionsHudElement.PotionEntry entry : entries) {
         if (entry.durationTicks() != Integer.MAX_VALUE) {
            seenIds.add(entry.id());
            PotionsHudElement.EffectNotifyState state = this.notifyStates.computeIfAbsent(entry.id(), idx -> new PotionsHudElement.EffectNotifyState());
            state.name = entry.name();
            state.lastSeenMs = nowMs;
            if (entry.durationTicks() > 200) {
               state.warnedSoon = false;
               state.endedNotified = false;
            } else {
               if (!state.warnedSoon) {
                  Notifications.info("Эффект '" + entry.name() + "' скоро закончится!");
                  state.warnedSoon = true;
               }

               if (entry.durationTicks() <= 0 && !state.endedNotified) {
                  Notifications.warning("Эффект '" + entry.name() + "' закончился!");
                  state.endedNotified = true;
               }
            }
         }
      }

      Iterator<Entry<String, PotionsHudElement.EffectNotifyState>> it = this.notifyStates.entrySet().iterator();

      while (it.hasNext()) {
         Entry<String, PotionsHudElement.EffectNotifyState> mapEntry = it.next();
         String id = mapEntry.getKey();
         PotionsHudElement.EffectNotifyState state = mapEntry.getValue();
         if (!seenIds.contains(id)) {
            if (state.warnedSoon && !state.endedNotified) {
               Notifications.warning("Эффект '" + state.name + "' закончился!");
               state.endedNotified = true;
            }

            if (state.endedNotified || nowMs - state.lastSeenMs > 5000L) {
               it.remove();
            }
         }
      }
   }

   private boolean isWarnBeforeEndingEnabled() {
      Havoc havoc = Havoc.getInstance();
      if (havoc != null && havoc.getModuleManager() != null) {
         Interface interfaceModule = havoc.getModuleManager().getModule(Interface.class);
         if (interfaceModule == null) {
            return true;
         }

         for (HudElement element : interfaceModule.getHudElements()) {
            if (element instanceof NotificationsHudElement notificationsHudElement) {
               return notificationsHudElement.isPotionEndingWarningsEnabled();
            }
         }

         return true;
      } else {
         return true;
      }
   }

   private static final class EffectNotifyState {
      private String name = "Effect";
      private long lastSeenMs;
      private boolean warnedSoon;
      private boolean endedNotified;
   }

   private record PotionEntry(String id, String name, int level, String duration, int durationTicks, Identifier iconTexture) {
   }

   private static final class RowState {
      private PotionsHudElement.PotionEntry entry;
      private final AnimatedValue alphaAnim = new AnimatedValue();
      private final AnimatedValue indexAnim = new AnimatedValue();
      private boolean seen;
      private int targetIndex;

      private RowState(PotionsHudElement.PotionEntry entry) {
         this.entry = entry;
      }
   }

   private record RowVisual(PotionsHudElement.PotionEntry entry, float animatedIndex, float alpha) {
   }
}
