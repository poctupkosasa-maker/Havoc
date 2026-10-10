import havoc.screen.library.LibraryLayout;

/** Runs against the actual production layout without downloading a test framework. */
public final class LibraryLayoutTest {
   public static void main(String[] args) {
      int[][] windows = {{320, 240}, {427, 240}, {480, 270}, {640, 360}, {960, 540}, {1280, 720}, {1920, 1080}, {960, 270}};
      for (int[] window : windows) {
         LibraryLayout l = new LibraryLayout(window[0], window[1]);
         require(l.capacity() >= 3 && l.capacity() <= 9, "capacity");
         int stripRight = l.margin() + l.largeTile() + (l.capacity() - 1) * (l.smallTile() + l.gap());
         require(stripRight < l.width() - l.margin() - 46, "carousel must not cover arrows");
         require(l.heroY() > l.rowY() + l.largeTile(), "hero below carousel");
         require(l.heroY() + 42 < l.actionY(), "description above action");
         require(l.actionY() + 25 < l.height() - 16, "action above footer");
         for (int count : new int[]{0, 1, 4, 12, 100}) {
            for (int selected = 0; selected < Math.max(1, count); selected++) {
               int first = l.firstVisible(selected, count);
               require(first >= 0, "nonnegative first");
               if (count > 0) require(first <= selected && selected < first + l.capacity(), "selection visible");
            }
         }
      }
      System.out.println("Library layout: all window sizes and carousel bounds passed");
   }

   private static void require(boolean condition, String message) {
      if (!condition) throw new AssertionError(message);
   }
}
