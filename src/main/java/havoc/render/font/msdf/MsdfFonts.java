package havoc.render.font.msdf;

import havoc.render.font.GFonts;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.Identifier;

public final class MsdfFonts {
   private static final Map<GFonts, MsdfFont> FONTS = new EnumMap<>(GFonts.class);
   private static boolean initialized;

   private MsdfFonts() {
   }

   public static synchronized void init() {
      if (!initialized) {
         initialized = true;
         FONTS.put(GFonts.REGULAR, load("inter-medium", "inter-medium"));
         FONTS.put(GFonts.MEDIUM, load("inter-medium", "inter-medium"));
         FONTS.put(GFonts.SEMIBOLD, load("inter-semi", "inter-semi"));
         FONTS.put(GFonts.BOLD, load("inter-bold", "inter-bold"));
         FONTS.put(GFonts.OPAL_MEDIUM, load("productsans-medium", "productsans-medium"));
         FONTS.put(GFonts.OPAL_BOLD, load("productsans-bold", "productsans-bold"));
         FONTS.put(GFonts.OPAL_ICONS, load("materialicons-regular", "materialicons-regular"));
         FONTS.put(GFonts.NOTIFICATION_ICONS, load("havoc-notification-icons", "havoc-notification-icons"));
      }
   }

   public static MsdfFont get(GFonts font) {
      init();
      MsdfFont msdfFont = FONTS.get(font);
      if (msdfFont == null) {
         msdfFont = FONTS.get(GFonts.MEDIUM);
      }

      if (msdfFont == null) {
         throw new IllegalStateException("MSDF fonts are not initialized");
      } else {
         return msdfFont;
      }
   }

   private static MsdfFont load(String jsonName, String pngName) {
      return MsdfFont.load(
         Identifier.fromNamespaceAndPath("havoc", "fonts/msdf/" + jsonName + ".json"),
         Identifier.fromNamespaceAndPath("havoc", "fonts/msdf/" + pngName + ".png")
      );
   }
}
