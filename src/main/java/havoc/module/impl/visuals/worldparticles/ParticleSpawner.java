package havoc.module.impl.visuals.worldparticles;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.phys.Vec3;

public final class ParticleSpawner {
   private static final double MIN_RADIUS = 1.75;
   private static final double MAX_RADIUS = 10.5;
   private static final double GROUND_MIN_HEIGHT_OFFSET = -0.85;
   private static final double GROUND_MAX_HEIGHT_OFFSET = 0.35;
   private static final double SKY_MIN_HEIGHT_OFFSET = 7.0;
   private static final double SKY_MAX_HEIGHT_OFFSET = 15.0;
   private static final double DEFAULT_MIN_HEIGHT_OFFSET = -0.75;
   private static final double DEFAULT_MAX_HEIGHT_OFFSET = 3.5;
   private static final double DESPAWN_DISTANCE = 18.0;

   private ParticleSpawner() {
   }

   public static Particle createParticle(
      Vec3 playerPos, Vec3 playerVelocity, double playerSpeed, long lifeTimeMs, Particle.ParticleType type, boolean spawnFromGround
   ) {
      double radius = 1.75 + Math.random() * 8.75;
      double angle = Math.random() * Math.PI * 2.0;
      double spawnX = playerPos.x;
      double spawnZ = playerPos.z;
      if (playerSpeed > 0.05 && playerVelocity.horizontalDistance() > 0.01) {
         Vec3 normalizedVelocity = playerVelocity.normalize();
         double forwardAngle = Math.atan2(normalizedVelocity.z, normalizedVelocity.x);
         double angleSpread = Math.PI * 2.0 / 5.0;
         angle = forwardAngle + (Math.random() - 0.5) * angleSpread * 2.0;
         double forwardOffset = radius * 0.7 * Math.min(playerSpeed * 8.0, 1.0);
         spawnX += normalizedVelocity.x * forwardOffset;
         spawnZ += normalizedVelocity.z * forwardOffset;
      }

      double finalX = spawnX + Math.cos(angle) * radius;
      double finalZ = spawnZ + Math.sin(angle) * radius;
      boolean useGroundSpawn = spawnFromGround && ThreadLocalRandom.current().nextBoolean();
      double minHeight = -0.75;
      double maxHeight = 3.5;
      if (useGroundSpawn) {
         minHeight = -0.85;
         maxHeight = 0.35;
      } else {
         minHeight = 7.0;
         maxHeight = 15.0;
      }

      double finalY = playerPos.y + minHeight + Math.random() * (maxHeight - minHeight);
      double motionX = (Math.random() - 0.5) * 0.08;
      double motionY;
      if (useGroundSpawn) {
         motionY = 0.002 + Math.random() * 0.014;
      } else {
         motionY = -0.02 - Math.random() * 0.02;
      }

      double motionZ = (Math.random() - 0.5) * 0.08;
      return new Particle(finalX, finalY, finalZ, motionX, motionY, motionZ, lifeTimeMs).setType(type);
   }

   public static int calculateSpawnDelay(double playerSpeed) {
      int baseDelay = 40;
      if (playerSpeed <= 0.05) {
         return baseDelay;
      }

      double speedFactor = Math.min(playerSpeed * 5.0, 4.0);
      return Math.max((int)(baseDelay / (1.0 + speedFactor)), 8);
   }

   public static int calculateSpawnCount(double playerSpeed, int currentCount, int maxCount) {
      if (currentCount >= maxCount) {
         return 0;
      }

      int spawnCount = 1;
      if (playerSpeed > 0.1) {
         spawnCount = Math.min(8, maxCount - currentCount);
         spawnCount = Math.max(1, (int)(spawnCount * Math.min(playerSpeed * 5.0, 1.0)));
      }

      return spawnCount;
   }

   public static double getDespawnDistanceSquared() {
      return 324.0;
   }
}
