package havoc.utilites;

import havoc.Havoc;
import havoc.module.impl.misc.ClientSounds;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;

public final class ClientSoundPlayer {
   private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
      Thread thread = new Thread(r, "Havoc sounds");
      thread.setDaemon(true);
      return thread;
   });

   private ClientSoundPlayer() {
   }

   public static void playModuleToggle(boolean enabled) {
      try {
         ClientSounds sounds = Havoc.getInstance().getModuleManager().getModule(ClientSounds.class);
         if (sounds != null && sounds.isEnable()) {
            LumaSoundManager.play(
               sounds.isNewSound()
                  ? (enabled ? LumaSoundManager.MODULE_ENABLE : LumaSoundManager.MODULE_DISABLE)
                  : (enabled ? LumaSoundManager.MODULE_ON : LumaSoundManager.MODULE_OFF),
               sounds.getVolume()
            );
            return;
         }
      } catch (Exception var2) {
      }

      play(enabled ? "module_on.wav" : "module_off.wav", 0.42);
   }

   private static void play(String fileName, double volume) {
      EXECUTOR.execute(() -> {
         try (InputStream raw = ClientSoundPlayer.class.getResourceAsStream("/assets/havoc/sounds/" + fileName)) {
            if (raw != null) {
               try (
                  BufferedInputStream buffered = new BufferedInputStream(raw);
                  AudioInputStream stream = AudioSystem.getAudioInputStream(buffered);
               ) {
                  Clip clip = AudioSystem.getClip();
                  clip.open(stream);
                  setVolume(clip, volume);
                  clip.start();
               }
            }
         } catch (Exception var14) {
         }
      });
   }

   private static void setVolume(Clip clip, double volume) {
      if (clip.isControlSupported(Type.MASTER_GAIN)) {
         double safeVolume = Math.max(1.0E-4, Math.min(1.0, volume));
         FloatControl control = (FloatControl)clip.getControl(Type.MASTER_GAIN);
         control.setValue((float)(Math.log10(safeVolume) * 20.0));
      }
   }
}
