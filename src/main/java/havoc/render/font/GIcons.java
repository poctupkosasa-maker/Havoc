package havoc.render.font;

public enum GIcons {
   FRAME('\ue000'),
   KEYBOARD('\ue001'),
   CHECKMARK('\ue002'),
   LOGO('\ue003');

   private final char glyph;

   GIcons(char glyph) {
      this.glyph = glyph;
   }

   public char glyph() {
      return this.glyph;
   }
}
