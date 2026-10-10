package havoc.utilites.combat;

class CooldownTimer {
   private long startTime = System.currentTimeMillis();

   public boolean finished(long delay) {
      return System.currentTimeMillis() - delay >= this.startTime;
   }

   public void reset() {
      this.startTime = System.currentTimeMillis();
   }
}
