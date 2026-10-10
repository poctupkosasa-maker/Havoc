package me.x150.renderer.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessor {
   @Accessor("format")
   VertexFormat getVertexFormat();

   @Accessor("vertexPointer")
   long getVertexPointer();

   @Invoker("beginVertex")
   long beginNewVertex();
}
