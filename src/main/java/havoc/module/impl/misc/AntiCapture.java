package havoc.module.impl.misc;

import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.InputSetting;
import havoc.module.setting.MultiSelectSetting;
import havoc.screen.menu.MenuScreen;
import havoc.utilites.WinCaptureProtection;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

@ModuleInfo(name = "AntiCapture", desc = "Скрывает окно от записей экрана, стримов и удалённого доступа.", category = Category.MISC, enabledByDefault = true)
public final class AntiCapture extends Module {
   public final BooleanSetting antiCapture = this.registerSetting(
      new BooleanSetting("Anti-Capture", true)
   );
   public final BooleanSetting antiRemote = this.registerSetting(
      new BooleanSetting("Anti-Remote (AnyDesk/RustDesk/TD)", true)
   );
   public final BooleanSetting blackout = this.registerSetting(
      new BooleanSetting("Затемнение при удалёнке", false)
   );
   public final MultiSelectSetting streamWhitelist = this.registerSetting(
      new MultiSelectSetting(
         "Вайтлист стримов",
         Arrays.asList("OBS", "Discord", "Streamlabs", "XSplit", "Twitch Studio"),
         new LinkedHashSet<>(Arrays.asList("OBS", "Discord"))
      ).visibleWhen(() -> this.blackout.isValue())
   );
   public final InputSetting customWhitelist = this.registerSetting(
      new InputSetting("Свои процессы (через запятую)", "", 256).visibleWhen(() -> this.blackout.isValue())
   );
   public final BooleanSetting lockInput = this.registerSetting(
      new BooleanSetting("Блок клавиатуры при удалёнке", true).visibleWhen(() -> this.antiRemote.isValue())
   );
   public final BooleanSetting closeMenu = this.registerSetting(
      new BooleanSetting("Закрывать меню при удалёнке", true).visibleWhen(() -> this.antiRemote.isValue())
   );

   private static volatile boolean remoteWatcherActive;
   private static volatile boolean blackoutActive;
   private static volatile boolean inputLocked;

   private static final String[] REMOTE_HINTS = {
      "anydesk", "rustdesk", "teamviewer", "todesk", "sunloginclient", "parsec", "mstsc", "ammyy", "screenway"
   };

   @Override
   public void onEnable() {
      this.ensureApplied();
   }

   @Override
   public void onDisable() {
      WinCaptureProtection.unprotect(this.windowHandle());
      remoteWatcherActive = false;
      blackoutActive = false;
      inputLocked = false;
   }

   public void tickCheck() {
      if (!this.isEnable()) {
         if (remoteWatcherActive) {
            remoteWatcherActive = false;
            blackoutActive = false;
            inputLocked = false;
         }

         return;
      }

      boolean remote = this.antiRemote.isValue()
         && (WinCaptureProtection.isRdpSession() || WinCaptureProtection.processRunning(REMOTE_HINTS));
      if (remote != remoteWatcherActive) {
         if (remote) {
            String detail = WinCaptureProtection.isRdpSession()
               ? "RDP session"
               : WinCaptureProtection.findRunningProcess(REMOTE_HINTS).orElse("unknown process");
            System.out.println("[Havoc] AntiCapture: remote watcher detected (" + detail + ")");
         } else {
            System.out.println("[Havoc] AntiCapture: remote watcher gone");
         }
      }

      remoteWatcherActive = remote;
      blackoutActive = remote && this.blackout.isValue() && !this.isTrustedStreamRunning();

      boolean locked = remote && this.lockInput.isValue();
      if (locked != inputLocked) {
         inputLocked = locked;
         if (locked) {
            System.out.println("[Havoc] AntiCapture: input locked (remote session)");
            this.releaseKeys();
         } else {
            System.out.println("[Havoc] AntiCapture: input unlocked");
         }
      }

      if (remote && this.closeMenu.isValue()) {
         this.closeCheatMenu();
      }

      if (this.antiCapture.isValue()) {
         long handle = this.windowHandle();
         if (handle != 0L && handle != WinCaptureProtection.appliedHandle()) {
            WinCaptureProtection.protect(handle);
         }
      }
   }

   public static boolean isBlackoutActive() {
      return blackoutActive;
   }

   public static boolean isRemoteWatcherActive() {
      return remoteWatcherActive;
   }

   public static boolean isInputLocked() {
      return inputLocked;
   }

   private void closeCheatMenu() {
      try {
         if (mc != null && mc.gui.screen() instanceof MenuScreen menuScreen) {
            menuScreen.requestClose();
         }
      } catch (Throwable ignored) {
      }
   }

   private void releaseKeys() {
      try {
         if (mc == null || mc.options == null) {
            return;
         }
         mc.options.keyUp.setDown(false);
         mc.options.keyDown.setDown(false);
         mc.options.keyLeft.setDown(false);
         mc.options.keyRight.setDown(false);
         mc.options.keyJump.setDown(false);
         mc.options.keyShift.setDown(false);
         mc.options.keySprint.setDown(false);
         mc.options.keyAttack.setDown(false);
         mc.options.keyUse.setDown(false);
      } catch (Throwable ignored) {
      }
   }

   private void ensureApplied() {
      if (this.antiCapture.isValue()) {
         long handle = this.windowHandle();
         if (handle != 0L) {
            WinCaptureProtection.protect(handle);
         }
      }
   }

   private boolean isTrustedStreamRunning() {
      List<String> hints = new ArrayList<>();
      if (this.streamWhitelist.isSelected("OBS")) {
         hints.addAll(List.of("obs64", "obs32", "obs"));
      }
      if (this.streamWhitelist.isSelected("Discord")) {
         hints.addAll(List.of("discord", "vesktop"));
      }
      if (this.streamWhitelist.isSelected("Streamlabs")) {
         hints.addAll(List.of("streamlabs", "slobs"));
      }
      if (this.streamWhitelist.isSelected("XSplit")) {
         hints.add("xsplit");
      }
      if (this.streamWhitelist.isSelected("Twitch Studio")) {
         hints.addAll(List.of("twitchstudio", "twitch"));
      }
      for (String custom : this.customWhitelist.getValue().split("[,;]+")) {
         String hint = custom.trim().toLowerCase();
         if (hint.length() >= 2) {
            hints.add(hint);
         }
      }
      if (hints.isEmpty()) {
         return false;
      }
      return WinCaptureProtection.processRunning(hints.toArray(new String[0]));
   }

   private long windowHandle() {
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.getWindow() == null) {
         return 0L;
      }

      try {
         return org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(mc.getWindow().handle());
      } catch (Throwable ignored) {
         return 0L;
      }
   }
}
