package havoc.module.impl.visuals;

import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.ColorSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import havoc.render.AmbienceSkyRenderer;
import havoc.utilites.color.ClientColors;
import java.time.LocalTime;
import java.util.List;
import lombok.Generated;

@ModuleInfo(name = "Ambience", desc = "спизженый world ambience", category = Category.VISUALS)
public final class Ambience extends Module {
   public static final String SKY_NORMAL = "Обычное";
   public static final String SKY_PLASMA = "Плазма";
   public static final String SKY_SUMMER = "Astra";
   public static final String SUMMER_DAY = "Дневной";
   public static final String SUMMER_NIGHT = "Ночной";
   public static final String TIME_UNCHANGED = "Не менять";
   public static final String TIME_REAL = "Реальное время";
   public static final String FOG_NONE = "Не менять";
   public static final String FOG_CLEAR = "Очистить";
   public static final String FOG_OVERRIDE = "Переопределить";
   private static final double FULL_BRIGHT_GAMMA = 16.0;
   private static Ambience instance;
   private final SelectSetting skyMode = this.registerSetting(new SelectSetting("Небо", "Обычное", List.of("Обычное", "Astra")));
   private final SelectSetting timeMode = this.registerSetting(
      new SelectSetting("Время", "Не менять", List.of("Рассвет", "Утро", "День", "Вечер", "Закат солнца", "Ночь", "Реальное время", "Не менять"))
   );
   private final SelectSetting fogMode = this.registerSetting(new SelectSetting("Туман", "Не менять", List.of("Не менять", "Очистить", "Переопределить")));
   private final ColorSetting fogColor = this.registerSetting(new ColorSetting("Цвет туман", ClientColors.CATEGORY_ACCENT).visibleWhen(() -> false));
   private final SliderSetting fogEnd = this.registerSetting(
      new SliderSetting("Конец тумана", 1.0F, 0.1F, 1.5F, 0.1F).visibleWhen(() -> this.isFog("Переопределить"))
   );
   private final SliderSetting fogStart = this.registerSetting(
      new SliderSetting("Начало тумана", 0.5F, 0.1F, 1.5F, 0.1F).visibleWhen(() -> this.isFog("Переопределить"))
   );
   private final SliderSetting skySpeed = this.registerSetting(
      new SliderSetting("Скорость шейдера", 1.2F, 0.1F, 5.0F, 0.1F).visibleWhen(() -> this.isSky("Плазма"))
   );
   private final SliderSetting skyScale = this.registerSetting(
      new SliderSetting("Масштаб шейдера", 1.0F, 0.2F, 4.0F, 0.1F).visibleWhen(() -> this.isSky("Плазма") || this.isSky("Astra"))
   );
   private final BooleanSetting rays = this.registerSetting(new BooleanSetting("Солнечные лучи", true).visibleWhen(() -> this.isSky("Astra")));
   private final SliderSetting raysIntensity = this.registerSetting(
      new SliderSetting("Яркость лучей", 0.75F, 0.0F, 2.0F, 0.05F).visibleWhen(() -> this.isSky("Astra") && this.rays.isValue())
   );
   private final SliderSetting raysLength = this.registerSetting(
      new SliderSetting("Длина лучей", 1.0F, 0.25F, 2.5F, 0.05F).visibleWhen(() -> this.isSky("Astra") && this.rays.isValue())
   );
   private final SliderSetting skyOutline = this.registerSetting(
      new SliderSetting("Контур шейдера", 0.1F, 0.01F, 1.0F, 0.01F).visibleWhen(() -> this.isSky("Плазма"))
   );
   private final SliderSetting skyGlow = this.registerSetting(
      new SliderSetting("Глоу шейдера", 10.0F, 0.0F, 30.0F, 0.5F).visibleWhen(() -> this.isSky("Плазма"))
   );
   private final SliderSetting skyFill = this.registerSetting(
      new SliderSetting("Заливка шейдера", 0.6F, 0.0F, 2.0F, 0.05F).visibleWhen(() -> this.isSky("Плазма"))
   );
   private final SliderSetting skyAlpha = this.registerSetting(
      new SliderSetting("Альфа шейдера", 1.0F, 0.0F, 2.0F, 0.05F).visibleWhen(() -> this.isSky("Плазма"))
   );

   public Ambience() {
      instance = this;
      AmbienceSkyRenderer.bootstrap();
   }

   public boolean hasCustomSky() {
      return this.isEnable() && !this.isSky("Обычное");
   }

   public int getCustomSkyMode() {
      return this.isSky("Astra") ? 2 : 0;
   }

   public boolean isSummerNight() {
      long time = this.getForcedTimeOfDay() % 24000L;
      return time >= 13000L && time <= 23000L;
   }

   public boolean hasRays() {
      return this.rays.isValue();
   }

   public float getRaysIntensity() {
      return this.raysIntensity.getValue();
   }

   public float getRaysLength() {
      return this.raysLength.getValue();
   }

   public float getSkySpeed() {
      return this.skySpeed.getValue();
   }

   public float getSkyScale() {
      return this.skyScale.getValue();
   }

   public float getSkyOutline() {
      return this.skyOutline.getValue();
   }

   public float getSkyGlow() {
      return this.skyGlow.getValue();
   }

   public float getSkyFill() {
      return this.skyFill.getValue();
   }

   public float getSkyAlpha() {
      return this.skyAlpha.getValue();
   }

   public boolean shouldOverrideTime() {
      return this.isEnable() && !"Не менять".equals(this.timeMode.getValue());
   }

   public boolean shouldOverrideFog() {
      return this.isEnable() && (this.isSky("Astra") || !this.isFog("Не менять"));
   }

   public boolean shouldOverrideFogColor() {
      return this.isEnable() && (this.isSky("Astra") || this.isFog("Переопределить"));
   }

   public boolean shouldClearFog() {
      return this.isEnable() && this.isFog("Очистить");
   }

   public float getFogStartDistance() {
      float start = this.fogStart.getValue() * 42.67F;
      float end = this.fogEnd.getValue() * 42.67F;
      return Math.max(0.0F, Math.min(start, end - 0.5F));
   }

   public float getFogEndDistance() {
      float start = this.fogStart.getValue() * 42.67F;
      float end = this.fogEnd.getValue() * 42.67F;
      return Math.max(start + 0.5F, end);
   }

   public float getFogRed() {
      return (this.resolveFogColor() >> 16 & 0xFF) / 255.0F;
   }

   public float getFogGreen() {
      return (this.resolveFogColor() >> 8 & 0xFF) / 255.0F;
   }

   public float getFogBlue() {
      return (this.resolveFogColor() & 0xFF) / 255.0F;
   }

   public boolean isFullBrightEnabled() {
      return false;
   }

   public long getForcedTimeOfDay() {
      return switch (this.timeMode.getValue()) {
         case "Рассвет" -> 23000L;
         case "Утро" -> 1000L;
         case "День" -> 6000L;
         case "Вечер" -> 12000L;
         case "Закат солнца" -> 13000L;
         case "Ночь" -> 18000L;
         case "Реальное время" -> this.getRealWorldTime();
         default -> mc.level != null ? mc.level.getOverworldClockTime() : 0L;
      };
   }

   public float getTimeFraction() {
      return (float)Math.floorMod(this.getForcedTimeOfDay(), 24000L) / 24000.0F;
   }

   public double getFullBrightGammaValue() {
      return 16.0;
   }

   private boolean isSky(String mode) {
      return mode.equals(this.skyMode.getValue());
   }

   private boolean isFog(String mode) {
      return mode.equals(this.fogMode.getValue());
   }

   private int resolveFogColor() {
      if (this.isSky("Astra")) {
         return this.isSummerNight() ? -9600316 : -12662;
      } else {
         return ClientColors.CATEGORY_ACCENT_SECOND;
      }
   }

   private long getRealWorldTime() {
      LocalTime now = LocalTime.now();
      int totalSeconds = now.toSecondOfDay();
      int offsetSeconds = Math.floorMod(totalSeconds - 21600, 86400);
      return (long)(offsetSeconds / 86400.0 * 24000.0);
   }

   @Generated
   public static Ambience getInstance() {
      return instance;
   }
}
