package havoc.screen.library;

import havoc.render.RenderUtil;
import havoc.utilites.color.ColorUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class LibraryArtwork {
   public static final Identifier WORLDS = Identifier.fromNamespaceAndPath("havoc", "textures/gui/console_background.png");
   public static final Identifier SERVERS = Identifier.fromNamespaceAndPath("havoc", "textures/gui/console_servers.png");

   public static final Identifier WORLD_COVER = Identifier.fromNamespaceAndPath("havoc", "textures/gui/console_world_cover.png");
   public static final Identifier SERVER_COVER = Identifier.fromNamespaceAndPath("havoc", "textures/gui/console_server_cover.png");

   public static Identifier cover(boolean servers) { return servers ? SERVER_COVER : WORLD_COVER; }

   private LibraryArtwork() { }

   public static void background(GuiGraphicsExtractor graphics, int width, int height, boolean servers) {
      background(graphics, width, height, servers ? 1.0F : 0.0F);
   }

   public static void background(GuiGraphicsExtractor graphics, int width, int height, float serverMix) {
      serverMix = Math.clamp(serverMix, 0, 1);
      // Both illustrations use a cinematic landscape composition, not enlarged low-res favicons.
      float aspect = 1376.0F / 768.0F;
      float w = Math.max(width, height * aspect);
      float h = w / aspect;
      if (serverMix < 1) RenderUtil.drawImage(graphics, WORLDS, (width - w) / 2, (height - h) / 2, w, h, 0, -1, 1);
      if (serverMix > 0) RenderUtil.drawImage(graphics, SERVERS, (width - w) / 2, (height - h) / 2, w, h, 0,
         ColorUtil.multiplyAlpha(-1, serverMix), 1);
      RenderUtil.drawRoundRectGradient(graphics, 0, 0, width, height, 0, 0xE9050910, 0x23050910);
      RenderUtil.drawRoundRectGradientQuad(graphics, 0, 0, width, height, 0, 0x2603070E, 0xAC03070E, 0x8603070E, 0x1803070E);
   }
}
