package havoc.screen.screen.elements;

import com.mojang.blaze3d.textures.GpuTextureView;
import havoc.Havoc;
import havoc.module.impl.misc.NameProtect;
import havoc.module.impl.visuals.Interface;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.SelectSetting;
import havoc.render.RenderUtil;
import havoc.render.builders.states.QuadRadiusState;
import havoc.render.builders.states.SizeState;
import havoc.render.font.GFonts;
import havoc.render.renderers.MetanoiseRenderer;
import havoc.screen.menu.MenuScreen;
import havoc.screen.screen.AbstractHudElement;
import havoc.screen.screen.HudVisualStyle;
import havoc.utilites.PlayerHealthUtil;
import havoc.utilites.animation.AnimatedValue;
import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.joml.Matrix3x2fStack;

public final class TargetHudElement extends AbstractHudElement {
   private static final float RANGE_BLOCKS = 6.0F;
   private static final long TARGET_HOLD_MS = 1000L;
   private static final long ATTACK_TARGET_HOLD_MS = 550L;
   private static final float MIN_WIDTH = 116.0F;
   private static final float FIXED_HEIGHT = 30.0F;
   private static final int MAX_ITEMS = 6;
   private static final int HUD_TEXT_COLOR = ColorUtil.rgba(245, 246, 255, 255);
   private static final int HUD_SUBTLE_COLOR = ColorUtil.rgba(93, 98, 116, 255);
   private static final int HUD_SLOT_BG_COLOR = ColorUtil.rgba(20, 22, 30, 255);
   private static final int HUD_RING_BG_COLOR = ColorUtil.rgba(35, 38, 52, 255);
   private static final int HUD_HP_GOLD_COLOR = ColorUtil.rgba(247, 206, 82, 255);
   private static final int HUD_HEAD_HIT_COLOR = ColorUtil.rgba(255, 92, 92, 255);
   private static final int OPAL_HEART_RED = ColorUtil.rgba(255, 85, 85, 255);
   private static final int OPAL_HEART_GOLD = ColorUtil.rgba(255, 194, 71, 255);
   private static final String OPAL_HEART_GLYPH = "\ue87d";
   private static final String MOB_TARGET_ICON_PATH = "textures/gui/icons/target.png";
   private final SelectSetting design = this.addSetting(new SelectSetting("Дизайн", "Мод 1", List.of("Мод 1", "Мод 2", "Netchain")));
   private final BooleanSetting showOnHover = this.addSetting(new BooleanSetting("При наведении", false));
   private final AnimatedValue visibilityAnim = new AnimatedValue();
   private final AnimatedValue hpRatioAnim = new AnimatedValue();
   private final AnimatedValue secondaryHpRatioAnim = new AnimatedValue();
   private final AnimatedValue hpValueAnim = new AnimatedValue();
   private final AnimatedValue absorptionAnim = new AnimatedValue();
   private final AnimatedValue headHitTintAnim = new AnimatedValue();
   private final AnimatedValue opalHealthRatioAnim = new AnimatedValue();
   private final ItemStack[] displayItems = new ItemStack[6];
   private final ItemStack[] armorScratch = new ItemStack[4];
   private boolean active = true;
   private LivingEntity retainedTarget;
   private long retainedTargetSeenAtMs;
   private LivingEntity attackedTarget;
   private long attackedTargetSeenAtMs;
   private int hitTrackedEntityId = Integer.MIN_VALUE;
   private float hitTrackedHealth = -1.0F;
   private long hitFlashStartedAtMs;
   private int hpValueEntityId = Integer.MIN_VALUE;
   private int noiseTargetId = Integer.MIN_VALUE;
   private boolean noiseChatOpen;
   private long noiseStartedAtMs;

   public TargetHudElement() {
      super("target_hud", "TargetHud", 12.0F, 94.0F, 116.0F, 30.0F);
   }

   @Override
   public void setActive(boolean active) {
      this.active = active;
   }

   public void onAttack(Entity entity) {
      if (entity instanceof LivingEntity livingEntity) {
         this.attackedTarget = livingEntity;
         this.attackedTargetSeenAtMs = System.currentTimeMillis();
      }
   }

   @Override
   public boolean shouldRenderWhenInactive() {
      return true;
   }

   @Override
   public void render(GuiGraphicsExtractor context, int screenWidth, int screenHeight, float tickDelta) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null) {
         long nowNanos = System.nanoTime();
         long nowMs = System.currentTimeMillis();
         boolean chatOpen = mc.gui.screen() instanceof ChatScreen;
         if (!(mc.gui.screen() instanceof MenuScreen) && !chatOpen) {
            boolean editorPreview = false;
         } else {
            boolean editorPreview = true;
         }

         LivingEntity target = this.resolveTarget(mc, nowMs, this.showOnHover.isValue());
         LivingEntity statsEntity = (LivingEntity)(target != null ? target : mc.player);
         LivingEntity renderEntity = (LivingEntity)(target != null ? target : mc.player);
         boolean shouldShow = this.active && (target != null || chatOpen && mc.player != null || mc.gui.screen() instanceof MenuScreen && mc.player != null);
         int currentNoiseTargetId = target == null ? Integer.MIN_VALUE : target.getId();
         if (currentNoiseTargetId != this.noiseTargetId || chatOpen && !this.noiseChatOpen) {
            this.noiseTargetId = currentNoiseTargetId;
            this.noiseStartedAtMs = nowMs;
         }

         this.noiseChatOpen = chatOpen;
         float visible = this.visibilityAnim.update(shouldShow ? 1.0F : 0.0F, 0.11F, nowNanos);
         if (!(visible <= 0.01F) && statsEntity != null) {
            int statsEntityId = statsEntity.getId();
            float health = PlayerHealthUtil.getDisplayedHealth(statsEntity, mc.level);
            float absorption = Math.max(0.0F, statsEntity.getAbsorptionAmount());
            float totalHealth = health + absorption;
            if (statsEntityId != this.hpValueEntityId) {
               this.hpValueEntityId = statsEntityId;
               this.hpValueAnim.setValue(totalHealth);
            }

            float animatedHealth = Math.max(0.0F, this.hpValueAnim.update(totalHealth, 0.1F, nowNanos));
            float maxHealth = Math.max(1.0F, statsEntity.getMaxHealth());
            float hpRatio = ColorUtil.clamp01(health / maxHealth);
            float hpProgress = ColorUtil.clamp01(this.hpRatioAnim.update(hpRatio, 0.12F, nowNanos));
            float secondaryHpProgress = ColorUtil.clamp01(this.secondaryHpRatioAnim.update(hpRatio, 0.055F, nowNanos));
            float absorptionProgress = absorption > 0.01F ? ColorUtil.clamp01(absorption / maxHealth) : 0.0F;
            float absorptionAnimated = ColorUtil.clamp01(this.absorptionAnim.update(absorptionProgress, 0.14F, nowNanos));
            if (!(absorption > 0.01F) && !(absorptionAnimated > 0.01F)) {
               boolean hasAbsorption = false;
            } else {
               boolean hasAbsorption = true;
            }

            if (absorption > 0.01F) {
               Math.max(0.38F, absorptionAnimated);
            }

            float headHitTint = this.updateHeadHitTint(renderEntity, totalHealth, nowNanos, nowMs);
            String name = statsEntity.getName().getString();
            NameProtect nameProtect = Havoc.getInstance().getModuleManager().getModule(NameProtect.class);
            if (nameProtect != null) {
               name = nameProtect.replace(name);
            }

            if ("Netchain".equals(this.design.getValue())) {
               this.renderOpalMode(context, mc, statsEntity, renderEntity, name, visible, headHitTint, nowNanos);
            } else if ("Мод 2".equals(this.design.getValue())) {
               this.renderSkyCoreMode(
                  context,
                  mc,
                  statsEntity,
                  renderEntity,
                  name,
                  animatedHealth,
                  absorption,
                  hpProgress,
                  secondaryHpProgress,
                  absorptionAnimated,
                  visible,
                  headHitTint
               );
            } else {
               this.renderReferenceMode(context, mc, statsEntity, renderEntity, name, animatedHealth, hpProgress, visible, headHitTint);
            }
         } else {
            this.setBounds(0.0F, 0.0F);
         }
      }
   }

   private void renderReferenceMode(
      GuiGraphicsExtractor context,
      Minecraft mc,
      LivingEntity statsEntity,
      LivingEntity renderEntity,
      String name,
      float animatedHealth,
      float healthProgress,
      float visible,
      float headHitTint
   ) {
      float panelTop = 8.0F;
      float panelHeight = 36.0F;
      float panelRadius = 6.0F;
      float headSize = 27.0F;
      float headRadius = 5.0F;
      float padding = 4.5F;
      float contentGap = 4.5F;
      float nameSize = 8.4F;
      float healthSize = 6.5F;
      float itemSize = 7.5F;
      float itemGap = 1.0F;
      int itemCount = this.collectDisplayItems(statsEntity);
      float equipmentWidth = itemCount * 7.5F + Math.max(0, itemCount - 1) * 1.0F;
      float nameWidth = RenderUtil.getTextWidth(name, GFonts.SEMIBOLD, 8.4F);
      float contentWidth = Math.max(71.0F, Math.max(nameWidth, equipmentWidth));
      float width = Math.max(112.0F, 36.0F + contentWidth + 4.5F);
      float totalHeight = 44.0F;
      this.setBounds(width, totalHeight);
      float x = this.getX();
      float y = this.getY();
      float panelY = y + 8.0F;
      float noiseProgress = this.noiseStartedAtMs <= 0L ? 1.0F : ColorUtil.clamp01((float)(System.currentTimeMillis() - this.noiseStartedAtMs) / 500.0F);
      float contentProgress = noiseProgress * noiseProgress * (3.0F - 2.0F * noiseProgress);
      visible *= contentProgress;
      if (!(visible <= 0.015F)) {
         float panelAlpha = Math.min(1.0F, visible / 0.35F);
         if (noiseProgress < 0.999F) {
            if (Interface.isHudShadowEnabled()) {
               float shadowProgress = ColorUtil.clamp01(noiseProgress * 3.5F);
               RenderUtil.drawGlow(
                  context,
                  x + 0.5F,
                  panelY + 0.5F,
                  width - 1.0F,
                  35.0F,
                  6.0F,
                  ColorUtil.multiplyAlpha(ClientColors.HUD_SHADOW, visible * shadowProgress * Interface.getHudShadowOpacity()),
                  1.0F,
                  Interface.getHudShadowSize()
               );
            }

            new MetanoiseRenderer(
                  new SizeState(width, 36.0F), new QuadRadiusState(6.0F), ColorUtil.multiplyAlpha(ClientColors.HUD_BACKGROUND, panelAlpha), 0.8F, noiseProgress
               )
               .render(x, panelY, context);
         } else {
            HudVisualStyle.drawPanel(context, x, panelY, width, 36.0F, panelAlpha, 6.0F);
         }

         float headX = x + 4.5F;
         float headY = panelY + 4.5F;
         this.drawTargetAvatar(context, mc, renderEntity, headX, headY, 27.0F, visible, headHitTint, 5.0F);
         float contentX = headX + 27.0F + 4.5F;
         float contentRight = x + width - 4.5F;
         float availableWidth = Math.max(10.0F, contentRight - contentX);
         float equipmentX = Math.max(contentX, contentRight - equipmentWidth);

         for (int i = 0; i < itemCount; i++) {
            ItemStack stack = this.displayItems[i];
            this.displayItems[i] = ItemStack.EMPTY;
            if (stack != null && !stack.isEmpty() && !(visible < 0.45F)) {
               float iconX = equipmentX + i * 8.5F;
               Matrix3x2fStack matrices = context.pose();
               matrices.pushMatrix();
               matrices.translate(iconX, y);
               matrices.scale(0.46875F, 0.46875F);
               context.fakeItem(stack, 0, 0);
               matrices.popMatrix();
            }
         }

         int textColor = ColorUtil.multiplyAlpha(HudVisualStyle.PRIMARY_TEXT, visible);
         float nameY = panelY + 4.7F;
         if (nameWidth <= availableWidth) {
            RenderUtil.drawText(context, name, contentX, nameY, textColor, GFonts.SEMIBOLD, 8.4F);
         } else {
            RenderUtil.drawTextFadeRight(context, name, contentX, nameY, textColor, GFonts.SEMIBOLD, 8.4F, availableWidth, 8.0F);
         }

         int healthPercent = Math.round(ColorUtil.clamp01(animatedHealth / Math.max(1.0F, statsEntity.getMaxHealth())) * 100.0F);
         String healthText = healthPercent + "%";
         float healthTextWidth = RenderUtil.getTextWidth(healthText, GFonts.MEDIUM, 6.5F);
         RenderUtil.drawText(context, healthText, contentRight - healthTextWidth, panelY + 20.5F, textColor, GFonts.MEDIUM, 6.5F);
         float barY = panelY + 36.0F - 7.5F;
         float barHeight = 2.8F;
         RenderUtil.drawRoundRect(
            context, contentX, barY, availableWidth, barHeight, barHeight * 0.5F, ColorUtil.multiplyAlpha(HUD_RING_BG_COLOR, visible * 0.92F)
         );
         if (healthProgress > 0.01F) {
            RenderUtil.drawRoundRectGradient(
               context,
               contentX,
               barY,
               Math.max(barHeight, availableWidth * healthProgress),
               barHeight,
               barHeight * 0.5F,
               ColorUtil.multiplyAlpha(this.flowAccentColor(System.currentTimeMillis(), 0), visible),
               ColorUtil.multiplyAlpha(this.flowAccentColor(System.currentTimeMillis(), 10), visible)
            );
         }
      }
   }

   private void renderOpalMode(
      GuiGraphicsExtractor context,
      Minecraft mc,
      LivingEntity statsEntity,
      LivingEntity renderEntity,
      String name,
      float visible,
      float headHitTint,
      long nowNanos
   ) {
      float padding = 3.0F;
      float headOffset = 22.5F;
      float equipmentWidth = 55.0F;
      float nameSize = 6.0F;
      float hpSize = 5.0F;
      float height = 31.5F;
      float nameWidth = RenderUtil.getTextWidth(name, GFonts.OPAL_BOLD, 6.0F);
      float width = 6.0F + Math.max(50.0F, Math.max(55.0F, nameWidth)) + 22.5F + 1.0F;
      float x = this.getX();
      float y = this.getY();
      this.setBounds(width, 31.5F);
      float panelAlpha = Math.min(1.0F, visible / 0.35F);
      HudVisualStyle.drawPanel(context, x, y, width, 31.5F, panelAlpha, 4.0F);
      RenderUtil.drawText(
         context, name, x + 3.0F + 22.5F + 0.75F, y + 4.8F, ColorUtil.multiplyAlpha(HudVisualStyle.PRIMARY_TEXT, visible), GFonts.OPAL_BOLD, 6.0F
      );
      float absorption = Math.max(0.0F, statsEntity.getAbsorptionAmount());
      float health = Math.max(0.0F, statsEntity.getHealth());
      float maxHealth = Math.max(1.0F, statsEntity.getMaxHealth());
      float trueHealthPercent = ColorUtil.clamp01((health + absorption) / (maxHealth + absorption));
      float animatedHealthPercent = ColorUtil.clamp01(this.opalHealthRatioAnim.update(trueHealthPercent, 0.145F, nowNanos));
      String hp = this.formatOpalHealth(health + absorption);
      float heartWidth = RenderUtil.getTextWidth("\ue87d", GFonts.OPAL_ICONS, 5.0F);
      float hpWidth = RenderUtil.getTextWidth(hp, GFonts.OPAL_MEDIUM, 5.0F);
      float measuredHealthWidth = RenderUtil.getTextWidth(hp.length() > 2 ? hp : "88.", GFonts.OPAL_MEDIUM, 5.0F);
      float healthBarWidth = Math.max(4.0F, width - 8.25F - measuredHealthWidth - heartWidth);
      float barX = x + 3.0F - 0.125F;
      float barY = y + 24.75F;
      long flowNow = System.currentTimeMillis();
      int themeFirst = this.flowAccentColor(flowNow, 0);
      int themeSecond = this.flowAccentColor(flowNow, 12);
      int healthBackground = ColorUtil.multiplyAlpha(this.darkenOpal(themeSecond, 0.8F), visible * 0.6F);
      RenderUtil.drawRoundRect(context, barX, barY, healthBarWidth, 4.0F, 1.6666666F, healthBackground);
      if (animatedHealthPercent > 0.01F) {
         RenderUtil.drawRoundRectGradient(
            context,
            barX,
            barY,
            healthBarWidth * animatedHealthPercent,
            4.0F,
            1.6666666F,
            ColorUtil.multiplyAlpha(this.darkenOpal(themeFirst, 0.6F), visible),
            ColorUtil.multiplyAlpha(this.darkenOpal(themeSecond, 0.6F), visible)
         );
      }

      if (trueHealthPercent > 0.01F) {
         float trueWidth = healthBarWidth * trueHealthPercent;
         RenderUtil.drawRoundRectGradient(
            context, barX, barY, trueWidth, 4.0F, 1.6666666F, ColorUtil.multiplyAlpha(themeFirst, visible), ColorUtil.multiplyAlpha(themeSecond, visible)
         );
         int transparentBlack = ColorUtil.rgba(0, 0, 0, 0);
         int shadedBlack = ColorUtil.multiplyAlpha(ColorUtil.rgba(0, 0, 0, 153), visible);
         RenderUtil.drawRoundRectGradientQuad(context, barX, barY, trueWidth, 4.0F, 1.6666666F, transparentBlack, shadedBlack, shadedBlack, transparentBlack);
      }

      float heartX = x + width - 7.5F + 0.75F;
      int heartColor = absorption > 0.0F ? OPAL_HEART_GOLD : OPAL_HEART_RED;
      RenderUtil.drawText(context, "\ue87d", heartX, y + 25.5F, ColorUtil.multiplyAlpha(heartColor, visible), GFonts.OPAL_ICONS, 5.0F);
      RenderUtil.drawText(
         context,
         hp,
         x + width - 3.0F - hpWidth - heartWidth - 0.25F + 0.75F,
         y + 25.0F,
         ColorUtil.multiplyAlpha(HudVisualStyle.PRIMARY_TEXT, visible),
         GFonts.OPAL_MEDIUM,
         5.0F
      );
      this.drawOpalHead(context, mc, renderEntity, x + 3.0F + 0.25F, y + 3.0F, 19.5F, visible, headHitTint);
      int equipmentCount = this.collectOpalEquipment(statsEntity);

      for (int i = 0; i < equipmentCount; i++) {
         float boxX = x + i * 11.5F + 3.0F + 22.5F - 0.5F;
         float boxY = y + 3.0F + 8.5F;
         RenderUtil.drawRoundRect(context, boxX, boxY, 10.5F, 10.5F, 1.0F, ColorUtil.multiplyAlpha(ClientColors.CATEGORY_BACKGROUND, visible * 0.58F));
         ItemStack stack = this.displayItems[i];
         this.displayItems[i] = ItemStack.EMPTY;
         if (stack != null && !stack.isEmpty() && !(visible < 0.5F)) {
            Matrix3x2fStack matrices = context.pose();
            matrices.pushMatrix();
            matrices.translate(boxX + 0.25F, boxY + 0.25F);
            matrices.scale(0.625F, 0.625F);
            context.fakeItem(stack, 0, 0);
            matrices.popMatrix();
         }
      }
   }

   private void renderSkyCoreMode(
      GuiGraphicsExtractor context,
      Minecraft mc,
      LivingEntity statsEntity,
      LivingEntity renderEntity,
      String name,
      float animatedHealth,
      float absorption,
      float hpProgress,
      float secondaryHpProgress,
      float absorptionProgress,
      float visible,
      float headHitTint
   ) {
      float padding = 5.0F;
      float avatarSize = 20.0F;
      float ringSize = 20.0F;
      float contentGap = 7.0F;
      float itemSize = 7.0F;
      float itemGap = 0.75F;
      float itemScale = 0.4F;
      float nameSize = 8.0F;
      float hpSize = 6.3F;
      int itemCount = this.collectDisplayItems(statsEntity);
      float itemsWidth = itemCount * itemSize + Math.max(0, itemCount - 1) * itemGap;
      float nameWidth = RenderUtil.getTextWidth(name, GFonts.SEMIBOLD, nameSize);
      float middleWidth = Math.max(Math.max(nameWidth, itemsWidth), 58.0F);
      float width = padding + avatarSize + contentGap + middleWidth + contentGap + ringSize + padding;
      width = Math.max(116.0F, width);
      float height = 30.0F;
      float x = this.getX();
      float y = this.getY();
      this.setBounds(width, height);
      float panelAlpha = Math.min(1.0F, visible / 0.35F);
      int accent = this.flowAccentColor(System.currentTimeMillis(), 0);
      float reveal = HudVisualStyle.easeOutCubic(visible);
      HudVisualStyle.drawPanel(context, x, y, width, height, panelAlpha, 8.0F);
      float avatarX = x + padding;
      float avatarY = y + (height - avatarSize) * 0.5F;
      this.drawTargetAvatar(context, mc, renderEntity, avatarX, avatarY, avatarSize, visible, headHitTint);
      float contentShift = (1.0F - reveal) * 5.0F;
      float textX = avatarX + avatarSize + contentGap + contentShift;
      float nameY = y + 6.1F;
      float itemsY = y + 16.0F;
      float ringX = x + width - padding - ringSize;
      float ringY = y + (height - ringSize) * 0.5F;
      float nameMaxWidth = Math.max(0.0F, ringX - contentGap - textX);
      int textColor = ColorUtil.multiplyAlpha(HudVisualStyle.PRIMARY_TEXT, visible);
      if (nameWidth <= nameMaxWidth) {
         RenderUtil.drawText(context, name, textX, nameY, textColor, GFonts.SEMIBOLD, nameSize);
      } else {
         RenderUtil.drawTextFadeRight(context, name, textX, nameY, textColor, GFonts.SEMIBOLD, nameSize, nameMaxWidth, 8.0F);
      }

      for (int i = 0; i < itemCount; i++) {
         ItemStack stack = this.displayItems[i];
         this.displayItems[i] = ItemStack.EMPTY;
         this.drawItemSlot(context, stack, textX + i * (itemSize + itemGap), itemsY, itemSize, itemScale, visible);
      }

      RenderUtil.drawRingProgress(context, ringX, ringY, ringSize, 1.0F, 0.12F, ColorUtil.multiplyAlpha(HUD_RING_BG_COLOR, visible), 1.0F);
      RenderUtil.drawRingProgress(
         context,
         ringX,
         ringY,
         ringSize,
         secondaryHpProgress,
         0.12F,
         ColorUtil.multiplyAlpha(this.flowAccentColor(System.currentTimeMillis(), 12), visible * 0.68F),
         1.0F
      );
      RenderUtil.drawRingProgress(context, ringX, ringY, ringSize, hpProgress, 0.12F, ColorUtil.multiplyAlpha(accent, visible), 1.0F);
      if (absorption > 0.01F || absorptionProgress > 0.01F) {
         RenderUtil.drawRingProgress(
            context, ringX, ringY, ringSize, ColorUtil.clamp01(absorptionProgress), 0.13F, ColorUtil.multiplyAlpha(HUD_HP_GOLD_COLOR, visible), 1.0F
         );
      }

      String hpText = Integer.toString(Math.max(0, Math.round(animatedHealth)));
      float hpTextWidth = RenderUtil.getTextWidth(hpText, GFonts.BOLD, hpSize);
      float hpTextHeight = RenderUtil.getTextHeight(GFonts.BOLD, hpSize);
      float hpTextX = ringX + (ringSize - hpTextWidth) * 0.5F + 1.2F;
      float hpTextY = ringY + (ringSize - hpTextHeight) * 0.5F + 1.45F;
      int hpTextColor = textColor;
      RenderUtil.drawText(context, hpText, hpTextX, hpTextY, hpTextColor, GFonts.BOLD, hpSize);
   }

   private int collectDisplayItems(LivingEntity target) {
      this.armorScratch[0] = target.getItemBySlot(EquipmentSlot.HEAD);
      this.armorScratch[1] = target.getItemBySlot(EquipmentSlot.CHEST);
      this.armorScratch[2] = target.getItemBySlot(EquipmentSlot.LEGS);
      this.armorScratch[3] = target.getItemBySlot(EquipmentSlot.FEET);
      int count = 0;

      for (int i = 0; i < this.armorScratch.length && count < this.displayItems.length; i++) {
         this.displayItems[count++] = this.armorScratch[i] == null ? ItemStack.EMPTY : this.armorScratch[i];
         this.armorScratch[i] = ItemStack.EMPTY;
      }

      if (count < this.displayItems.length) {
         this.displayItems[count++] = target.getOffhandItem();
      }

      if (count < this.displayItems.length) {
         this.displayItems[count++] = target.getMainHandItem();
      }

      return count;
   }

   private int collectOpalEquipment(LivingEntity target) {
      this.displayItems[0] = target.getMainHandItem();
      this.displayItems[1] = target.getItemBySlot(EquipmentSlot.FEET);
      this.displayItems[2] = target.getItemBySlot(EquipmentSlot.LEGS);
      this.displayItems[3] = target.getItemBySlot(EquipmentSlot.CHEST);
      this.displayItems[4] = target.getItemBySlot(EquipmentSlot.HEAD);
      return 5;
   }

   private String formatOpalHealth(float health) {
      float rounded = Math.round(health * 10.0F) / 10.0F;
      return Math.abs(rounded - Math.round(rounded)) < 0.001F
         ? Integer.toString(Math.max(0, Math.round(rounded)))
         : String.format(Locale.ROOT, "%.1f", Math.max(0.0F, rounded));
   }

   private int flowAccentColor(long nowMs, int phase) {
      int angle = Math.floorMod((int)(nowMs / 7L - phase * 22L), 360);
      angle = angle >= 180 ? 360 - angle : angle;
      return ColorUtil.interpolate(ClientColors.CATEGORY_ACCENT, ClientColors.CATEGORY_ACCENT_SECOND, angle / 180.0F);
   }

   private int darkenOpal(int color, float factor) {
      float multiplier = 1.0F - ColorUtil.clamp01(factor);
      return ColorUtil.rgba(
         Math.round(ColorUtil.red(color) * multiplier),
         Math.round(ColorUtil.green(color) * multiplier),
         Math.round(ColorUtil.blue(color) * multiplier),
         ColorUtil.alpha(color)
      );
   }

   private void drawItemSlot(GuiGraphicsExtractor context, ItemStack stack, float x, float y, float size, float itemScale, float alpha) {
      if (stack != null && !stack.isEmpty()) {
         float itemScreenSize = 16.0F * itemScale;
         float itemX = x + (size - itemScreenSize) * 0.5F;
         float itemY = y + (size - itemScreenSize) * 0.5F;
         Matrix3x2fStack matrices = context.pose();
         matrices.pushMatrix();
         matrices.translate(itemX, itemY);
         matrices.scale(itemScale, itemScale);
         context.fakeItem(stack, 0, 0);
         matrices.popMatrix();
         if (stack.isDamageableItem() && stack.getMaxDamage() > 0 && stack.getDamageValue() > 0) {
            float progress = ColorUtil.clamp01(1.0F - (float)stack.getDamageValue() / stack.getMaxDamage());
            float barW = size * 0.75F;
            float barX = x + (size - barW) * 0.5F;
            float barY = y + size - 1.0F;
            RenderUtil.drawRoundRect(context, barX, barY, barW, 1.4F, 0.5F, ColorUtil.multiplyAlpha(HUD_SLOT_BG_COLOR, alpha));
            RenderUtil.drawRoundRect(context, barX, barY, barW * progress, 1.4F, 0.5F, this.getDurabilityColor(progress, Math.round(255.0F * alpha)));
         }
      } else {
         RenderUtil.drawRoundRect(context, x + 1.5F, y + 2.0F, size - 3.0F, size - 3.0F, 1.0F, ColorUtil.multiplyAlpha(HUD_SUBTLE_COLOR, alpha * 0.32F));
      }
   }

   private int getDurabilityColor(float progress, int alpha) {
      if (progress > 0.6F) {
         float t = (progress - 0.6F) / 0.4F;
         return ColorUtil.rgba(Math.round(255.0F * (1.0F - t)), 200, 60, alpha);
      } else if (progress > 0.3F) {
         float t = (progress - 0.3F) / 0.3F;
         return ColorUtil.rgba(255, Math.round(200.0F * t), 0, alpha);
      } else {
         return ColorUtil.rgba(220, Math.round(60.0F * (progress / 0.3F)), 0, alpha);
      }
   }

   private LivingEntity resolveTarget(Minecraft mc, long nowMs, boolean allowHoverTarget) {
      if (allowHoverTarget) {
         LivingEntity crosshairTarget = this.resolveCrosshairTarget(mc);
         if (crosshairTarget != null) {
            this.retainedTarget = crosshairTarget;
            this.retainedTargetSeenAtMs = nowMs;
            return crosshairTarget;
         }
      }

      LivingEntity recentAttackTarget = this.resolveRecentAttackTarget(mc, nowMs);
      if (recentAttackTarget != null) {
         this.retainedTarget = recentAttackTarget;
         this.retainedTargetSeenAtMs = nowMs;
         return recentAttackTarget;
      }

      if (allowHoverTarget && this.isRetainedTargetValid(mc, nowMs)) {
         return this.retainedTarget;
      }

      this.retainedTarget = null;
      this.retainedTargetSeenAtMs = 0L;
      return null;
   }

   private LivingEntity resolveRecentAttackTarget(Minecraft mc, long nowMs) {
      if (this.attackedTarget != null && mc.player != null && mc.level != null) {
         if (nowMs - this.attackedTargetSeenAtMs <= 550L
            && this.attackedTarget != mc.player
            && !this.attackedTarget.isRemoved()
            && this.attackedTarget.isAlive()) {
            return this.attackedTarget;
         }

         this.attackedTarget = null;
         this.attackedTargetSeenAtMs = 0L;
         return null;
      } else {
         return null;
      }
   }

   private LivingEntity resolveCrosshairTarget(Minecraft mc) {
      if (mc.player == null) {
         return null;
      }

      if (mc.hitResult instanceof EntityHitResult entityHit) {
         if (entityHit.getEntity() instanceof LivingEntity target) {
            if (target == mc.player) {
               return null;
            } else {
               return mc.player.distanceToSqr(target) > 36.0 ? null : target;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private boolean isRetainedTargetValid(Minecraft mc, long nowMs) {
      if (this.retainedTarget != null && mc.player != null && mc.level != null) {
         if (nowMs - this.retainedTargetSeenAtMs > 1000L) {
            return false;
         } else if (this.retainedTarget != mc.player && !this.retainedTarget.isRemoved() && this.retainedTarget.isAlive()) {
            float stickyRange = 7.5F;
            return mc.player.distanceToSqr(this.retainedTarget) <= stickyRange * stickyRange;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private float updateHeadHitTint(LivingEntity entity, float totalHealth, long nowNanos, long nowMs) {
      if (entity == null) {
         this.hitTrackedEntityId = Integer.MIN_VALUE;
         this.hitTrackedHealth = -1.0F;
         this.hitFlashStartedAtMs = 0L;
         return this.headHitTintAnim.update(0.0F, 0.1F, nowNanos);
      }

      int entityId = entity.getId();
      if (entityId != this.hitTrackedEntityId) {
         this.hitTrackedEntityId = entityId;
         this.hitTrackedHealth = totalHealth;
         this.hitFlashStartedAtMs = 0L;
         return this.headHitTintAnim.update(0.0F, 0.1F, nowNanos);
      }

      if (this.hitTrackedHealth >= 0.0F && totalHealth + 0.01F < this.hitTrackedHealth) {
         this.hitFlashStartedAtMs = nowMs;
      }

      this.hitTrackedHealth = totalHealth;
      float targetHit = 0.0F;
      if (this.hitFlashStartedAtMs > 0L) {
         float ageSec = (float)(nowMs - this.hitFlashStartedAtMs) / 1000.0F;
         if (ageSec < 0.55F) {
            float t = 1.0F - ageSec / 0.55F;
            targetHit = t * t;
         }
      }

      return this.headHitTintAnim.update(targetHit, 0.08F, nowNanos);
   }

   private void drawOpalHead(GuiGraphicsExtractor context, Minecraft mc, LivingEntity entity, float x, float y, float size, float alpha, float hitTint) {
      GpuTextureView textureView = null;
      if (entity instanceof AbstractClientPlayer player) {
         textureView = this.resolvePlayerSkinTextureView(mc, player);
      } else {
         Identifier textureId = null;
         if (entity instanceof Skeleton) {
            textureId = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/skeleton/skeleton.png");
         } else if (entity instanceof Zombie) {
            textureId = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/zombie/zombie.png");
         } else if (entity instanceof Creeper) {
            textureId = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/creeper/creeper.png");
         } else if (entity instanceof Piglin) {
            textureId = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/piglin/piglin.png");
         }

         if (textureId != null) {
            try {
               textureView = mc.getTextureManager().getTexture(textureId).getTextureView();
            } catch (Exception ignored) {
               textureView = null;
            }
         }
      }

      if (textureView != null) {
         int normal = ColorUtil.multiplyAlpha(ColorUtil.rgba(255, 255, 255, 255), alpha);
         int damaged = ColorUtil.multiplyAlpha(ColorUtil.rgba(255, 102, 102, 255), alpha);
         int tint = ColorUtil.interpolate(normal, damaged, ColorUtil.clamp01(hitTint));
         RenderUtil.drawHeadImageRegion(context, textureView, x, y, size, size, 8.0F, 8.0F, 8.0F, 8.0F, 2.0F, tint, 1.0F);
      }
   }

   private void drawTargetAvatar(GuiGraphicsExtractor context, Minecraft mc, LivingEntity entity, float x, float y, float size, float alpha, float hitTint) {
      this.drawTargetAvatar(context, mc, entity, x, y, size, alpha, hitTint, 4.0F);
   }

   private void drawTargetAvatar(
      GuiGraphicsExtractor context, Minecraft mc, LivingEntity entity, float x, float y, float size, float alpha, float hitTint, float radius
   ) {
      if (entity != null) {
         int baseIconColor = ColorUtil.multiplyAlpha(HUD_TEXT_COLOR, alpha);
         int hurtIconColor = ColorUtil.multiplyAlpha(HUD_HEAD_HIT_COLOR, alpha);
         int iconColor = ColorUtil.interpolate(baseIconColor, hurtIconColor, ColorUtil.clamp01(hitTint * 0.45F));
         RenderUtil.drawRoundRect(context, x, y, size, size, radius, ColorUtil.multiplyAlpha(HUD_SLOT_BG_COLOR, alpha));
         float inset = 0.0F;
         float innerSize = size - inset * 2.0F;
         float innerX = x + inset;
         float innerY = y + inset;
         float headRadius = Math.min(radius, innerSize * 0.5F);
         if (entity instanceof AbstractClientPlayer player) {
            GpuTextureView textureView = this.resolvePlayerSkinTextureView(mc, player);
            if (textureView != null) {
               int textureColor = ColorUtil.interpolate(ColorUtil.multiplyAlpha(-1, alpha), hurtIconColor, ColorUtil.clamp01(hitTint));
               RenderUtil.drawHeadImageRegion(
                  context, textureView, innerX, innerY, innerSize, innerSize, 8.0F, 8.0F, 8.0F, 8.0F, headRadius, textureColor, 1.0F
               );
               RenderUtil.drawHeadImageRegion(
                  context, textureView, innerX, innerY, innerSize, innerSize, 40.0F, 8.0F, 8.0F, 8.0F, headRadius, textureColor, 1.0F
               );
               return;
            }
         }

         RenderUtil.drawImage(context, "textures/gui/icons/target.png", innerX, innerY, innerSize, innerSize, headRadius, iconColor, 1.0F);
      }
   }

   private GpuTextureView resolvePlayerSkinTextureView(Minecraft mc, AbstractClientPlayer player) {
      PlayerSkin textures = this.resolvePlayerSkinTextures(mc, player);
      if (textures == null) {
         textures = DefaultPlayerSkin.get(player.getUUID());
      }

      Identifier skinId = this.resolveSkinIdentifier(textures);
      if (skinId == null) {
         return null;
      }

      try {
         return mc.getTextureManager().getTexture(skinId).getTextureView();
      } catch (Exception ignored) {
         return null;
      }
   }

   private PlayerSkin resolvePlayerSkinTextures(Minecraft mc, AbstractClientPlayer player) {
      try {
         if (mc.getConnection() != null) {
            PlayerInfo entry = mc.getConnection().getPlayerInfo(player.getUUID());
            if (entry != null) {
               return entry.getSkin();
            }
         }
      } catch (Exception var5) {
      }

      try {
         return player.getSkin();
      } catch (Exception ignored) {
         return null;
      }
   }

   private Identifier resolveSkinIdentifier(PlayerSkin textures) {
      return textures != null && textures.body() != null ? textures.body().texturePath() : null;
   }
}
