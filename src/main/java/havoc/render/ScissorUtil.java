package havoc.render;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;

public final class ScissorUtil {
   private static final Deque<ScissorUtil.ScissorRect> SCISSOR_STACK = new ArrayDeque<>();

   private ScissorUtil() {
   }

   public static void push(GuiGraphicsExtractor ctx, float x, float y, float width, float height) {
      push(ctx.pose(), x, y, width, height);
   }

   public static void push(Matrix3x2fStack matrices, float x, float y, float width, float height) {
      push(transformRect(matrices, x, y, width, height));
   }

   public static void push(float x, float y, float width, float height) {
      push(new ScissorUtil.ScissorRect(x, y, width, height));
   }

   public static void pop() {
      if (!SCISSOR_STACK.isEmpty()) {
         SCISSOR_STACK.pop();
      }

      if (SCISSOR_STACK.isEmpty()) {
         GL11.glDisable(3089);
      } else {
         applyScissor(SCISSOR_STACK.peek());
      }
   }

   public static void clear() {
      SCISSOR_STACK.clear();
      GL11.glDisable(3089);
   }

   public static boolean isEnabled() {
      return !SCISSOR_STACK.isEmpty();
   }

   private static void push(ScissorUtil.ScissorRect rect) {
      ScissorUtil.ScissorRect clipped = rect;
      if (!SCISSOR_STACK.isEmpty()) {
         clipped = intersect(SCISSOR_STACK.peek(), rect);
      }

      SCISSOR_STACK.push(clipped);
      applyScissor(clipped);
   }

   private static ScissorUtil.ScissorRect transformRect(Matrix3x2fStack matrices, float x, float y, float width, float height) {
      Vector2f topLeft = new Vector2f(x, y);
      Vector2f bottomRight = new Vector2f(x + width, y + height);
      matrices.transformPosition(topLeft);
      matrices.transformPosition(bottomRight);
      float left = Math.min(topLeft.x, bottomRight.x);
      float top = Math.min(topLeft.y, bottomRight.y);
      float right = Math.max(topLeft.x, bottomRight.x);
      float bottom = Math.max(topLeft.y, bottomRight.y);
      return new ScissorUtil.ScissorRect(left, top, right - left, bottom - top);
   }

   private static ScissorUtil.ScissorRect intersect(ScissorUtil.ScissorRect a, ScissorUtil.ScissorRect b) {
      float left = Math.max(a.left(), b.left());
      float top = Math.max(a.top(), b.top());
      float right = Math.min(a.right(), b.right());
      float bottom = Math.min(a.bottom(), b.bottom());
      return new ScissorUtil.ScissorRect(left, top, Math.max(0.0F, right - left), Math.max(0.0F, bottom - top));
   }

   private static void applyScissor(ScissorUtil.ScissorRect rect) {
      Minecraft client = Minecraft.getInstance();
      int framebufferHeight = client.getWindow().getHeight();
      double scale = client.getWindow().getGuiScale();
      float left = (float)(rect.left() * scale);
      float top = (float)(rect.top() * scale);
      float right = (float)(rect.right() * scale);
      float bottom = (float)(rect.bottom() * scale);
      int scissorX = (int)Math.floor(left);
      int scissorY = (int)Math.floor(framebufferHeight - bottom);
      int scissorWidth = Math.max(0, (int)Math.ceil(right - left));
      int scissorHeight = Math.max(0, (int)Math.ceil(bottom - top));
      GL11.glEnable(3089);
      GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
   }

   private record ScissorRect(float x, float y, float width, float height) {
      private float left() {
         return this.x;
      }

      private float top() {
         return this.y;
      }

      private float right() {
         return this.x + this.width;
      }

      private float bottom() {
         return this.y + this.height;
      }
   }
}
