package havoc.utilites.color;

public class TextColorAlphaStorage {
   private static final ThreadLocal<Integer> pendingColor = ThreadLocal.withInitial(() -> 0);

   public static void setPending(int color) {
      pendingColor.set(color);
   }

   public static int getPending() {
      Integer value = pendingColor.get();
      return value != null ? value : 0;
   }

   public static void clearPending() {
      pendingColor.remove();
   }
}
