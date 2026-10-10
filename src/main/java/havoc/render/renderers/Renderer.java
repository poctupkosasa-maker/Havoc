package havoc.render.renderers;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface Renderer {
   void render(double var1, double var3, GuiGraphicsExtractor var5);

   static int toAbgr(int argb) {
      int a = argb >>> 24 & 0xFF;
      int r = argb >>> 16 & 0xFF;
      int g = argb >>> 8 & 0xFF;
      int b = argb & 0xFF;
      return a << 24 | b << 16 | g << 8 | r;
   }
}
