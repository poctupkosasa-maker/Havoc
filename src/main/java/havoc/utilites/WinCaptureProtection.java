package havoc.utilites;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public final class WinCaptureProtection {
   private static final int WDA_NONE = 0x00000000;
   private static final int WDA_MONITOR = 0x00000001;
   private static final int WDA_EXCLUDEFROMCAPTURE = 0x00000020;
   private static final int SM_REMOTESESSION = 0x1000;
   private static volatile boolean unavailable;
   private static User32 user32;
   private static long appliedHandle = -1L;

   private WinCaptureProtection() {
   }

   public static boolean protect(long windowHandle) {
      if (windowHandle == 0L) {
         return false;
      } else {
         User32 api = api();
         if (api == null) {
            return false;
         } else {
            Pointer hwnd = new Pointer(windowHandle);
            if (api.SetWindowDisplayAffinity(hwnd, WDA_EXCLUDEFROMCAPTURE)) {
               appliedHandle = windowHandle;
               System.out.println("[Havoc] AntiCapture: window excluded from screen capture (WDA_EXCLUDEFROMCAPTURE)");
               return true;
            } else if (api.SetWindowDisplayAffinity(hwnd, WDA_MONITOR)) {
               appliedHandle = windowHandle;
               System.out.println("[Havoc] AntiCapture: fallback WDA_MONITOR applied");
               return true;
            } else {
               System.out.println("[Havoc] AntiCapture: SetWindowDisplayAffinity refused");
               return false;
            }
         }
      }
   }

   public static void unprotect(long windowHandle) {
      User32 api = api();
      if (api != null && windowHandle != 0L) {
         try {
            api.SetWindowDisplayAffinity(new Pointer(windowHandle), WDA_NONE);
         } catch (Throwable ignored) {
         }
      }

      appliedHandle = -1L;
   }

   public static long appliedHandle() {
      return appliedHandle;
   }

   public static boolean isRdpSession() {
      User32 api = api();
      if (api == null) {
         return false;
      }

      try {
         return api.GetSystemMetrics(SM_REMOTESESSION) != 0;
      } catch (Throwable ignored) {
         return false;
      }
   }

   public static boolean processRunning(String... nameHints) {
      return findRunningProcess(nameHints).isPresent();
   }

   public static java.util.Optional<String> findRunningProcess(String... nameHints) {
      try {
         return ProcessHandle.allProcesses().map(handle -> handle.info().command().orElse("")).filter(command -> {
            if (command.isEmpty()) {
               return false;
            }
            String lower = command.toLowerCase();
            for (String hint : nameHints) {
               if (lower.contains(hint.toLowerCase())) {
                  return true;
               }
            }

            return false;
         }).findFirst();
      } catch (Throwable ignored) {
         return java.util.Optional.empty();
      }
   }

   private static User32 api() {
      if (unavailable) {
         return null;
      } else if (user32 == null) {
         try {
            user32 = (User32)Native.load("user32", User32.class);
         } catch (Throwable throwable) {
            unavailable = true;
            return null;
         }
      }

      return user32;
   }

   public interface User32 extends Library {
      boolean SetWindowDisplayAffinity(Pointer hwnd, int affinity);

      int GetSystemMetrics(int index);
   }
}
