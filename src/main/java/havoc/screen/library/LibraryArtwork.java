package havoc.screen.library;

import havoc.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class LibraryArtwork {
   public static final Identifier WORLDS = Identifier.fromNamespaceAndPath("havoc", "textures/gui/console_background.png");
   public static final Identifier SERVERS = Identifier.fromNamespaceAndPath("havoc", "textures/gui/console_servers.png");

   private LibraryArtwork() { }

   public static void background(GuiGraphicsExtractor graphics, int width, int height, boolean servers) {
      // Both illustrations use a cinematic landscape composition, not enlarged low-res favicons.
      float aspect = 1376.0F / 768.0F;
      float w = Math.max(width, height * aspect);
      float h = w / aspect;
      RenderUtil.drawImage(graphics, servers ? SERVERS : WORLDS, (width - w) / 2, (height - h) / 2, w, h, 0, -1, 1);
      RenderUtil.drawRoundRectGradient(graphics, 0, 0, width, height, 0, 0xE5080B13, 0x35080B13);
      RenderUtil.drawRoundRectGradientQuad(graphics, 0, 0, width, height, 0, 0x70060A12, 0xEF060A12, 0xA0060A12, 0x28060A12);
   }
}
