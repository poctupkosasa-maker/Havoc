import havoc.screen.library.CarouselMotion;

public final class CarouselMotionTest {
   public static void main(String[] args) {
      CarouselMotion motion = new CarouselMotion();
      motion.snap(10, 40, 42);
      motion.update(1_000_000_000L);
      motion.target(100, 40, 70);
      int previousX = motion.x();
      for (int i = 1; i <= 60; i++) {
         motion.update(1_000_000_000L + i * 16_666_667L);
         require(motion.x() >= previousX && motion.x() <= 100, "no position overshoot");
         require(motion.size() >= 42 && motion.size() <= 70, "no size overshoot");
         previousX = motion.x();
      }
      require(motion.x() == 100 && motion.size() == 70, "settles on the target");
      motion.target(0, 50, 30);
      motion.update(2_100_000_000L);
      require(motion.x() >= 0 && motion.x() < 100, "can reverse direction");
      motion.snap(300, 40, 45);
      require(motion.x() == 300 && motion.size() == 45, "recycled slot resets immediately");

      CarouselMotion thirty = simulate(30);
      CarouselMotion oneTwenty = simulate(120);
      require(Math.abs(thirty.x() - oneTwenty.x()) <= 1, "position is independent of FPS");
      require(Math.abs(thirty.size() - oneTwenty.size()) <= 1, "size is independent of FPS");

      CarouselMotion left = new CarouselMotion();
      CarouselMotion right = new CarouselMotion();
      left.snap(10, 40, 60);
      right.snap(76, 40, 36);
      left.update(1_000_000_000L);
      right.update(1_000_000_000L);
      left.target(10, 40, 36);
      right.target(52, 40, 60);
      for (int i = 1; i <= 60; i++) {
         long now = 1_000_000_000L + i * 16_666_667L;
         left.update(now);
         right.update(now);
         require(right.x() - (left.x() + left.size()) >= 5, "adjacent cards do not overlap");
      }
      System.out.println("Carousel motion: easing, recycling, FPS and spacing checks passed");
   }

   private static CarouselMotion simulate(int fps) {
      CarouselMotion result = new CarouselMotion();
      result.snap(0, 0, 40);
      result.update(1_000_000_000L);
      result.target(200, 0, 80);
      for (int i = 1; i <= fps / 5; i++) result.update(1_000_000_000L + Math.round(i * 1_000_000_000.0 / fps));
      return result;
   }

   private static void require(boolean condition, String message) {
      if (!condition) throw new AssertionError(message);
   }
}
