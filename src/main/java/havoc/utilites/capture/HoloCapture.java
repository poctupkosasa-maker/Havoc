package havoc.utilites.capture;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.ArrayList;
import java.util.List;

public final class HoloCapture {
   public static final int TARGET_W = 384;
   public static final int TARGET_H = 216;

   public static final int PW_RENDERFULLCONTENT = 0x00000002;
   public static final int WM_MOUSEMOVE = 0x0200;
   public static final int WM_LBUTTONDOWN = 0x0201;
   public static final int WM_LBUTTONUP = 0x0202;
   public static final long MK_LBUTTON = 0x0001L;

   private HoloCapture() {
   }

   public interface ExtUser32 extends Library {
      ExtUser32 INSTANCE = Native.load("user32", ExtUser32.class);

      interface EnumProc extends Callback {
         boolean callback(Pointer hwnd, Pointer data);
      }

      boolean EnumWindows(EnumProc proc, Pointer data);

      int GetWindowTextW(Pointer hwnd, char[] buf, int max);

      boolean IsWindowVisible(Pointer hwnd);

      boolean IsIconic(Pointer hwnd);

      boolean GetWindowRect(Pointer hwnd, RECT rect);

      boolean GetClientRect(Pointer hwnd, RECT rect);

      boolean ClientToScreen(Pointer hwnd, POINT pt);

      boolean PrintWindow(Pointer hwnd, Pointer hdc, int flags);

      long PostMessageW(Pointer hwnd, int msg, long wParam, long lParam);
   }

   public interface ExtGdi32 extends Library {
      ExtGdi32 INSTANCE = Native.load("gdi32", ExtGdi32.class);

      Pointer GetDC(Pointer hwnd);

      int ReleaseDC(Pointer hwnd, Pointer hdc);

      Pointer CreateCompatibleDC(Pointer hdc);

      Pointer CreateCompatibleBitmap(Pointer hdc, int width, int height);

      Pointer SelectObject(Pointer hdc, Pointer obj);

      boolean DeleteDC(Pointer hdc);

      boolean DeleteObject(Pointer obj);

      int GetDIBits(Pointer hdc, Pointer hbmp, int start, int lines, byte[] bits, BITMAPINFO info, int usage);
   }

   @Structure.FieldOrder({"left", "top", "right", "bottom"})
   public static class RECT extends Structure {
      public int left;
      public int top;
      public int right;
      public int bottom;

      public int width() {
         return this.right - this.left;
      }

      public int height() {
         return this.bottom - this.top;
      }
   }

   @Structure.FieldOrder({"x", "y"})
   public static class POINT extends Structure {
      public int x;
      public int y;
   }

   @Structure.FieldOrder({
      "biSize", "biWidth", "biHeight", "biPlanes", "biBitCount", "biCompression",
      "biSizeImage", "biXPelsPerMeter", "biYPelsPerMeter", "biClrUsed", "biClrImportant"
   })
   public static class BITMAPINFO extends Structure {
      public int biSize = 40;
      public int biWidth;
      public int biHeight;
      public int biPlanes = 1;
      public int biBitCount = 32;
      public int biCompression;
      public int biSizeImage;
      public int biXPelsPerMeter;
      public int biYPelsPerMeter;
      public int biClrUsed;
      public int biClrImportant;
   }

   public record WinInfo(Pointer hwnd, String title) {
   }

   public record Target(Pointer hwnd, String title, int clientW, int clientH) {
   }

   public static List<WinInfo> listWindows() {
      List<WinInfo> result = new ArrayList<>();
      try {
         ExtUser32.INSTANCE.EnumWindows((hwnd, data) -> {
            try {
               if (hwnd != null && ExtUser32.INSTANCE.IsWindowVisible(hwnd)) {
                  char[] buf = new char[256];
                  int len = ExtUser32.INSTANCE.GetWindowTextW(hwnd, buf, buf.length);
                  if (len > 0) {
                     String title = new String(buf, 0, len).trim();
                     if (!title.isEmpty()) {
                        result.add(new WinInfo(hwnd, title));
                     }
                  }
               }
            } catch (Throwable ignored) {
            }
            return true;
         }, null);
      } catch (Throwable ignored) {
      }
      return result;
   }

   public static WinInfo findWindow(String hint) {
      if (hint == null || hint.isBlank()) {
         return null;
      }
      String needle = hint.trim().toLowerCase();
      for (WinInfo info : listWindows()) {
         if (info.title().toLowerCase().contains(needle)) {
            return info;
         }
      }
      return null;
   }

   public static void postClick(Pointer hwnd, int x, int y) {
      if (hwnd == null) {
         return;
      }
      try {
         long lParam = ((long) y << 16) | (x & 0xFFFFL);
         ExtUser32.INSTANCE.PostMessageW(hwnd, WM_MOUSEMOVE, 0L, lParam);
         ExtUser32.INSTANCE.PostMessageW(hwnd, WM_LBUTTONDOWN, MK_LBUTTON, lParam);
         ExtUser32.INSTANCE.PostMessageW(hwnd, WM_LBUTTONUP, 0L, lParam);
      } catch (Throwable ignored) {
      }
   }

   public static final class Session implements AutoCloseable {
      private Pointer hwnd;
      private int capW;
      private int capH;
      private Pointer hdcScreen;
      private Pointer hdcMem;
      private Pointer hbmp;
      private Pointer hOld;
      private byte[] dib;

      public int clientOffX;
      public int clientOffY;
      public int clientW;
      public int clientH;

      public boolean open(Pointer hwnd) {
         this.close();
         if (hwnd == null) {
            return false;
         }
         try {
            RECT win = new RECT();
            RECT client = new RECT();
            POINT origin = new POINT();
            if (!ExtUser32.INSTANCE.GetWindowRect(hwnd, win) || !ExtUser32.INSTANCE.GetClientRect(hwnd, client)) {
               return false;
            }
            origin.x = 0;
            origin.y = 0;
            if (!ExtUser32.INSTANCE.ClientToScreen(hwnd, origin)) {
               return false;
            }
            int w = win.width();
            int h = win.height();
            if (w <= 0 || h <= 0 || w > 4096 || h > 4096) {
               return false;
            }
            Pointer screen = ExtGdi32.INSTANCE.GetDC(null);
            if (screen == null) {
               return false;
            }
            Pointer mem = ExtGdi32.INSTANCE.CreateCompatibleDC(screen);
            Pointer bmp = ExtGdi32.INSTANCE.CreateCompatibleBitmap(screen, w, h);
            ExtGdi32.INSTANCE.ReleaseDC(null, screen);
            if (mem == null || bmp == null) {
               if (mem != null) {
                  ExtGdi32.INSTANCE.DeleteDC(mem);
               }
               if (bmp != null) {
                  ExtGdi32.INSTANCE.DeleteObject(bmp);
               }
               return false;
            }
            this.hwnd = hwnd;
            this.capW = w;
            this.capH = h;
            this.hdcMem = mem;
            this.hbmp = bmp;
            this.hOld = ExtGdi32.INSTANCE.SelectObject(mem, bmp);
            this.dib = new byte[w * h * 4];
            this.clientOffX = Math.max(0, origin.x - win.left);
            this.clientOffY = Math.max(0, origin.y - win.top);
            this.clientW = Math.max(0, Math.min(client.width(), w - this.clientOffX));
            this.clientH = Math.max(0, Math.min(client.height(), h - this.clientOffY));
            return this.clientW > 0 && this.clientH > 0;
         } catch (Throwable ignored) {
            this.close();
            return false;
         }
      }

      public boolean grab(int[] outAbgr) {
         if (this.hwnd == null || this.hdcMem == null || this.hbmp == null || outAbgr == null) {
            return false;
         }
         try {
            if (ExtUser32.INSTANCE.IsIconic(this.hwnd)) {
               return false;
            }
            if (!ExtUser32.INSTANCE.PrintWindow(this.hwnd, this.hdcMem, PW_RENDERFULLCONTENT)) {
               return false;
            }
            BITMAPINFO info = new BITMAPINFO();
            info.biWidth = this.capW;
            info.biHeight = this.capH;
            if (ExtGdi32.INSTANCE.GetDIBits(this.hdcMem, this.hbmp, 0, this.capH, this.dib, info, 0) == 0) {
               return false;
            }
            downscale(this.dib, this.capW, this.capH, this.clientOffX, this.clientOffY, this.clientW, this.clientH, outAbgr);
            return true;
         } catch (Throwable ignored) {
            return false;
         }
      }

      @Override
      public void close() {
         try {
            if (this.hdcMem != null && this.hOld != null) {
               ExtGdi32.INSTANCE.SelectObject(this.hdcMem, this.hOld);
            }
         } catch (Throwable ignored) {
         }
         try {
            if (this.hbmp != null) {
               ExtGdi32.INSTANCE.DeleteObject(this.hbmp);
            }
         } catch (Throwable ignored) {
         }
         try {
            if (this.hdcMem != null) {
               ExtGdi32.INSTANCE.DeleteDC(this.hdcMem);
            }
         } catch (Throwable ignored) {
         }
         this.hwnd = null;
         this.hdcMem = null;
         this.hbmp = null;
         this.hOld = null;
         this.dib = null;
         this.capW = 0;
         this.capH = 0;
      }
   }

   private static void downscale(
      byte[] bgraBottomUp, int srcW, int srcH,
      int regionX, int regionY, int regionW, int regionH,
      int[] outAbgr
   ) {
      if (regionW <= 0 || regionH <= 0) {
         return;
      }
      for (int ty = 0; ty < TARGET_H; ty++) {
         int y0 = regionY + ty * regionH / TARGET_H;
         int y1 = regionY + (ty + 1) * regionH / TARGET_H;
         if (y1 <= y0) {
            y1 = y0 + 1;
         }
         for (int tx = 0; tx < TARGET_W; tx++) {
            int x0 = regionX + tx * regionW / TARGET_W;
            int x1 = regionX + (tx + 1) * regionW / TARGET_W;
            if (x1 <= x0) {
               x1 = x0 + 1;
            }
            long sumB = 0;
            long sumG = 0;
            long sumR = 0;
            int count = 0;
            for (int sy = y0; sy < y1 && sy < srcH; sy++) {
               int dibRow = srcH - 1 - sy;
               int rowBase = dibRow * srcW * 4;
               for (int sx = x0; sx < x1 && sx < srcW; sx++) {
                  int p = rowBase + sx * 4;
                  sumB += bgraBottomUp[p] & 0xFF;
                  sumG += bgraBottomUp[p + 1] & 0xFF;
                  sumR += bgraBottomUp[p + 2] & 0xFF;
                  count++;
               }
            }
            int b = count == 0 ? 0 : (int) (sumB / count);
            int g = count == 0 ? 0 : (int) (sumG / count);
            int r = count == 0 ? 0 : (int) (sumR / count);
            outAbgr[ty * TARGET_W + tx] = 0xFF000000 | (b << 16) | (g << 8) | r;
         }
      }
   }
}
