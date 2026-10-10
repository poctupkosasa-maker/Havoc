package havoc.utilites;

import havoc.utilites.color.ClientColors;
import havoc.utilites.color.ColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class ChatUtil implements IMinecraft {
   private static final String PREFIX = "Havoc";
   private static final float PREFIX_ANIMATION_PERIOD_MS = 1800.0F;
   private static final int PREFIX_START_RGB = ClientColors.CATEGORY_ACCENT & 16777215;
   private static final int PREFIX_END_RGB = ColorUtil.interpolate(ClientColors.CATEGORY_ACCENT, ClientColors.TEXT_PRIMARY, 0.7F) & 16777215;

   private ChatUtil() {
   }

   public static void sendMessage(String message) {
      String clean = sanitize(message);
      if (!clean.isEmpty() && mc != null) {
         MutableComponent line = buildGradientPrefixText();
         line.append(Component.literal(" | ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(5592405))));
         line.append(parseColoredText(clean, Style.EMPTY.withColor(TextColor.fromRgb(16777215))));
         mc.execute(() -> {
            if (mc.gui != null && mc.gui.hud != null && mc.gui.hud.getChat() != null) {
               mc.gui.hud.getChat().addClientSystemMessage(line);
            } else {
               if (mc.player != null) {
                  mc.player.sendSystemMessage(line);
               }
            }
         });
      }
   }

   public static void sendActionBar(String message) {
      String clean = sanitize(message);
      if (!clean.isEmpty() && mc != null) {
         MutableComponent line = parseColoredText(clean, Style.EMPTY.withColor(TextColor.fromRgb(16777215)));
         mc.execute(() -> {
            if (mc.player != null) {
               mc.player.sendOverlayMessage(line);
            }
         });
      }
   }

   private static MutableComponent buildGradientPrefixText() {
      MutableComponent out = Component.empty();
      float phase = (float)(System.currentTimeMillis() % 1800L) / 1800.0F;
      int visible = 0;

      for (int i = 0; i < "Havoc".length(); i++) {
         if (!Character.isWhitespace("Havoc".charAt(i))) {
            visible++;
         }
      }

      int fromArgb = 0xFF000000 | PREFIX_START_RGB;
      int toArgb = 0xFF000000 | PREFIX_END_RGB;
      int gradientIndex = 0;

      for (int i = 0; i < "Havoc".length(); i++) {
         char ch = "Havoc".charAt(i);
         if (Character.isWhitespace(ch)) {
            out.append(Component.literal(String.valueOf(ch)));
         } else {
            float t = visible <= 1 ? 0.0F : (float)gradientIndex / (visible - 1);
            t = (t + phase) % 1.0F;
            int rgb = ColorUtil.interpolate(fromArgb, toArgb, t) & 16777215;
            out.append(Component.literal(String.valueOf(ch)).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
            gradientIndex++;
         }
      }

      return out;
   }

   private static MutableComponent parseColoredText(String raw, Style baseStyle) {
      MutableComponent out = Component.empty();
      Style style = baseStyle == null ? Style.EMPTY : baseStyle;
      StringBuilder segment = new StringBuilder();
      int i = 0;

      while (i < raw.length()) {
         char ch = raw.charAt(i);
         ChatUtil.HexMatch hashHex = parseHashHex(raw, i);
         if (hashHex != null) {
            flush(out, segment, style);
            style = style.withColor(TextColor.fromRgb(hashHex.rgb));
            i += hashHex.length;
         } else {
            ChatUtil.LegacyHexMatch legacyHex = parseLegacyHex(raw, i);
            if (legacyHex != null) {
               flush(out, segment, style);
               style = style.withColor(TextColor.fromRgb(legacyHex.rgb));
               i += legacyHex.length;
            } else {
               if ((ch == '&' || ch == 167) && i + 1 < raw.length()) {
                  char code = Character.toLowerCase(raw.charAt(i + 1));
                  ChatFormatting formatting = ChatFormatting.getByCode(code);
                  if (formatting != null) {
                     flush(out, segment, style);
                     style = applyFormatting(style, formatting);
                     i += 2;
                     continue;
                  }
               }

               segment.append(ch);
               i++;
            }
         }
      }

      flush(out, segment, style);
      return out;
   }

   private static void flush(MutableComponent out, StringBuilder segment, Style style) {
      if (!segment.isEmpty()) {
         out.append(Component.literal(segment.toString()).setStyle(style));
         segment.setLength(0);
      }
   }

   private static Style applyFormatting(Style current, ChatFormatting formatting) {
      if (formatting == ChatFormatting.RESET) {
         return Style.EMPTY.withColor(TextColor.fromRgb(16777215));
      } else {
         return formatting.ordinal() <= ChatFormatting.WHITE.ordinal() ? Style.EMPTY.applyFormat(formatting) : current.applyFormat(formatting);
      }
   }

   private static ChatUtil.HexMatch parseHashHex(String text, int i) {
      if (i >= text.length()) {
         return null;
      }

      int start = i;
      if (text.charAt(i) == '&' || text.charAt(i) == 167) {
         i++;
         if (i >= text.length() || text.charAt(i) != '#') {
            return null;
         }
      }

      if (text.charAt(i) != '#') {
         return null;
      }

      int hexStart = i + 1;
      if (hexStart + 6 > text.length()) {
         return null;
      }

      String hex = text.substring(hexStart, hexStart + 6);
      return !isHex(hex) ? null : new ChatUtil.HexMatch(Integer.parseInt(hex, 16), hexStart + 6 - start);
   }

   private static ChatUtil.LegacyHexMatch parseLegacyHex(String text, int i) {
      if (i + 13 >= text.length()) {
         return null;
      }

      char first = text.charAt(i);
      if ((first == '&' || first == 167) && Character.toLowerCase(text.charAt(i + 1)) == 'x') {
         StringBuilder hex = new StringBuilder(6);

         for (int n = 0; n < 6; n++) {
            int markerPos = i + 2 + n * 2;
            int digitPos = markerPos + 1;
            char marker = text.charAt(markerPos);
            char digit = text.charAt(digitPos);
            if (marker != '&' && marker != 167 || Character.digit(digit, 16) < 0) {
               return null;
            }

            hex.append(digit);
         }

         return new ChatUtil.LegacyHexMatch(Integer.parseInt(hex.toString(), 16), 14);
      } else {
         return null;
      }
   }

   private static boolean isHex(String hex) {
      for (int i = 0; i < hex.length(); i++) {
         if (Character.digit(hex.charAt(i), 16) < 0) {
            return false;
         }
      }

      return true;
   }

   private static String sanitize(String value) {
      return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
   }

   private record HexMatch(int rgb, int length) {
   }

   private record LegacyHexMatch(int rgb, int length) {
   }
}
