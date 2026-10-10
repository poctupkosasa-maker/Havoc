package havoc.screen.library;

/** Shared, GUI-scale-independent console layout for the title and both libraries. */
public record LibraryLayout(int width, int height) {
   public int margin() { return Math.max(14, Math.round(width * 0.048F)); }
   public int heroX() { return Math.max(22, Math.round(width * 0.09F)); }
   public int rowY() { return Math.max(44, Math.round(height * 0.116F)); }
   public int largeTile() { return Math.max(42, Math.min(100, Math.round(height * 0.17F))); }
   public int smallTile() { return Math.round(largeTile() * 0.60F); }
   public int gap() { return Math.max(4, Math.round(largeTile() * 0.10F)); }
   public int capacity() { return Math.max(3, Math.min(9, (width - 2 * margin() - 56 - largeTile()) / (smallTile() + gap()) + 1)); }
   public int heroY() { return Math.max(rowY() + largeTile() + 24, Math.round(height * 0.56F)); }
   public int actionY() { return Math.min(height - 42, Math.round(height * 0.79F)); }
   public int actionWidth() { return Math.max(96, Math.min(156, Math.round(width * 0.16F))); }
   public float heroFontSize() { return Math.min(20.0F, width / 24.0F); }
   public float heroWidth() { return Math.min(340.0F, width - 2.0F * heroX()); }
   public boolean showDescription() { return actionY() - heroY() >= 76; }
   public int firstVisible(int selected, int count) { return Math.clamp(selected - 1, 0, Math.max(0, count - capacity())); }
}
