package havoc.module.impl.visuals;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.vertex.VertexConsumer;
import havoc.event.impl.EventRender3D;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.ColorSetting;
import havoc.module.setting.SelectSetting;
import havoc.module.setting.SliderSetting;
import havoc.render.Render3D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(name = "KillEffect", desc = "Combatant kill visual effects", category = Category.VISUALS)
public final class KillEffect extends Module {
   private final SelectSetting mode = this.registerSetting(
      new SelectSetting("Mode", "Orthodox", Arrays.asList("Orthodox", "FallingLava", "EmbersArc", "Lightning"))
   );
   private final SliderSetting ySpeed = this.registerSetting(
      new SliderSetting("Y Speed", 0.0F, -10.0F, 10.0F, 1.0F).visibleWhen(() -> this.mode.getValue().equals("Orthodox"))
   );
   private final BooleanSetting playSound = this.registerSetting(
      new BooleanSetting("Play Sound", true).visibleWhen(() -> this.mode.getValue().equals("Orthodox"))
   );
   private final SliderSetting soundVolume = this.registerSetting(
      new SliderSetting("Sound Volume", 1.0F, 0.0F, 2.0F, 0.05F).visibleWhen(() -> this.mode.getValue().equals("Orthodox") && this.playSound.isValue())
   );
   private final ColorSetting color = this.registerSetting(
      new ColorSetting("Color", -27136).visibleWhen(() -> this.mode.getValue().equals("Orthodox") || this.mode.getValue().equals("Lightning"))
   );
   private final BooleanSetting mobs = this.registerSetting(new BooleanSetting("Mobs", false));
   private final Map<Integer, Long> handled = new HashMap<>();
   private final List<KillEffect.OrthodoxMark> orthodoxMarks = new ArrayList<>();
   private final List<KillEffect.LightningPath> lightningPaths = new ArrayList<>();
   private final Random random = new Random();

   @Subscribe
   public void onTick(EventUpdate event) {
      if (mc.level != null && mc.player != null) {
         long now = System.currentTimeMillis();
         this.handled.entrySet().removeIf(entry -> now - entry.getValue() > 6000L);
         this.orthodoxMarks.removeIf(mark -> now - mark.createdAt > 3000L);
         this.lightningPaths.removeIf(path -> now - path.createdAt > 1450L);

         for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(128.0), ignored -> true)) {
            if (entity != mc.player
               && (this.mobs.isValue() || entity instanceof Player)
               && !entity.isAlive()
               && !(entity.getHealth() > 0.0F)
               && !this.handled.containsKey(entity.getId())) {
               this.handled.put(entity.getId(), now);
               Vec3 position = entity.position();
               switch (this.mode.getValue()) {
                  case "Orthodox":
                     this.orthodoxMarks.add(new KillEffect.OrthodoxMark(position, now));
                     if (this.playSound.isValue()) {
                        mc.level
                           .playSound(
                              mc.player, position.x, position.y, position.z, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, this.soundVolume.getValue(), 0.75F
                           );
                     }
                     break;
                  case "FallingLava":
                     this.spawnFallingLava(entity);
                     break;
                  case "EmbersArc":
                     this.spawnEmbers(position);
                     break;
                  case "Lightning":
                     this.lightningPaths.add(new KillEffect.LightningPath(position, now));
                     LightningBolt bolt = new LightningBolt(EntityTypes.LIGHTNING_BOLT, mc.level);
                     bolt.snapTo(position.x, position.y, position.z);
                     bolt.setVisualOnly(true);
                     mc.level.addEntity(bolt);
               }
            }
         }
      }
   }

   @Subscribe
   public void onRender(EventRender3D event) {
      if (!this.orthodoxMarks.isEmpty() || !this.lightningPaths.isEmpty()) {
         VertexConsumer lines = event.getConsumers().getBuffer(RenderTypes.lines());
         Vec3 camera = event.getCameraPos();
         long now = System.currentTimeMillis();
         int baseColor = this.color.getColor();
         double offset = this.ySpeed.getValue().floatValue() / 100.0;

         for (KillEffect.OrthodoxMark mark : this.orthodoxMarks) {
            Vec3 pos = mark.position.subtract(camera).add(0.0, offset, 0.0);
            Render3D.drawLine(event.getMatrices(), lines, pos, pos.add(0.0, 3.0, 0.0), baseColor);
            Render3D.drawLine(event.getMatrices(), lines, pos.add(1.0, 2.3, 0.0), pos.add(-1.0, 2.3, 0.0), baseColor);
            Render3D.drawLine(event.getMatrices(), lines, pos.add(0.5, 1.2, 0.0), pos.add(-0.5, 0.8, 0.0), baseColor);
         }

         for (KillEffect.LightningPath path : this.lightningPaths) {
            float alpha = Math.max(0.0F, 1.0F - (float)(now - path.createdAt) / 1450.0F);
            int drawColor = (int)((baseColor >>> 24 & 0xFF) * alpha) << 24 | baseColor & 16777215;

            for (int i = 1; i < path.points.size(); i++) {
               Render3D.drawLine(event.getMatrices(), lines, path.points.get(i - 1).subtract(camera), path.points.get(i).subtract(camera), drawColor);
            }
         }
      }
   }

   private void spawnFallingLava(LivingEntity entity) {
      int height = (int)(entity.getBbHeight() * 10.0F);
      int width = (int)(entity.getBbWidth() * 10.0F);

      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            for (int z = 0; z < width; z++) {
               mc.level.addParticle(ParticleTypes.FALLING_LAVA, entity.getX() + x * 0.1, entity.getY() + y * 0.1, entity.getZ() + z * 0.1, 0.0, 0.0, 0.0);
            }
         }
      }
   }

   private void spawnEmbers(Vec3 position) {
      int count = 16 + this.random.nextInt(11);

      for (int i = 0; i < count; i++) {
         double angle = this.random.nextDouble() * Math.PI * 2.0;
         double speed = 0.18 + this.random.nextDouble() * 0.18;
         mc.level
            .addParticle(
               ParticleTypes.FLAME, position.x, position.y, position.z, Math.cos(angle) * speed, 0.28 + this.random.nextDouble() * 0.2, Math.sin(angle) * speed
            );
      }
   }

   @Override
   public void onDisable() {
      this.handled.clear();
      this.orthodoxMarks.clear();
      this.lightningPaths.clear();
      super.onDisable();
   }

   private final class LightningPath {
      private final List<Vec3> points = new ArrayList<>(200);
      private final long createdAt;

      private LightningPath(Vec3 start, long createdAt) {
         this.createdAt = createdAt;
         Vec3 current = start;

         for (int i = 0; i < 200; i++) {
            current = current.add(KillEffect.this.random.nextFloat() * 0.5F - 0.25F, 0.25, KillEffect.this.random.nextFloat() * 0.5F - 0.25F);
            this.points.add(current);
         }
      }
   }

   private record OrthodoxMark(Vec3 position, long createdAt) {
   }
}
