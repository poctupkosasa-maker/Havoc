package me.x150.renderer.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import me.x150.renderer.mixin.BufferBuilderAccessor;
import net.minecraft.util.Mth;
import org.lwjgl.system.MemoryUtil;

public final class DirectVertexConsumer implements VertexConsumer {
   private final BufferBuilderAccessor buffer;
   private final VertexFormat format;
   private ByteBuffer target;

   public DirectVertexConsumer(BufferBuilder original, boolean reuseCurrentVertex) {
      this.buffer = (BufferBuilderAccessor)original;
      this.format = this.buffer.getVertexFormat();
      long pointer = reuseCurrentVertex ? this.buffer.getVertexPointer() : this.buffer.beginNewVertex();
      this.target = this.view(pointer);
   }

   private ByteBuffer view(long pointer) {
      ByteBuffer result = MemoryUtil.memByteBuffer(pointer, this.format.getVertexSize());
      result.order(ByteOrder.nativeOrder());
      return result;
   }

   private void ensureSpace() {
      if (!this.target.hasRemaining()) {
         this.target = this.view(this.buffer.beginNewVertex());
      }
   }

   public VertexConsumer addVertex(float x, float y, float z) {
      this.ensureSpace();
      this.target.putFloat(x).putFloat(y).putFloat(z);
      return this;
   }

   public VertexConsumer setColor(int red, int green, int blue, int alpha) {
      return this.setColor(alpha << 24 | red << 16 | green << 8 | blue);
   }

   public VertexConsumer setColor(int argb) {
      this.ensureSpace();
      this.target.putInt(argb);
      return this;
   }

   public VertexConsumer setUv(float u, float v) {
      this.ensureSpace();
      this.target.putFloat(u).putFloat(v);
      return this;
   }

   public VertexConsumer setUv1(int u, int v) {
      this.ensureSpace();
      this.target.putShort((short)u).putShort((short)v);
      return this;
   }

   public VertexConsumer setUv2(int u, int v) {
      return this.setUv1(u, v);
   }

   public VertexConsumer setNormal(float x, float y, float z) {
      this.ensureSpace();
      this.target.put(toNormalByte(x)).put(toNormalByte(y)).put(toNormalByte(z));
      return this;
   }

   public VertexConsumer setLineWidth(float width) {
      this.ensureSpace();
      this.target.putFloat(width);
      return this;
   }

   private static byte toNormalByte(float value) {
      return (byte)((int)(Mth.clamp(value, -1.0F, 1.0F) * 127.0F) & 0xFF);
   }
}
