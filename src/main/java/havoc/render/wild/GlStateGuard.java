package havoc.render.wild;

public final class GlStateGuard {
   private static final GlStateGuard INSTANCE = new GlStateGuard();

   private GlStateGuard() {
   }

   public static GlStateGuard getINSTANCE() {
      return INSTANCE;
   }

   public void invoke2(int triangles) {
   }
}
