package havoc.render.builders.impl;

import com.mojang.blaze3d.textures.GpuTextureView;
import havoc.render.builders.AbstractBuilder;
import havoc.render.builders.states.QuadColorState;
import havoc.render.builders.states.QuadRadiusState;
import havoc.render.builders.states.SizeState;
import havoc.render.renderers.TextureRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;

public final class TextureBuilder extends AbstractBuilder<TextureRenderer> {
   private SizeState size;
   private QuadRadiusState radius;
   private QuadColorState color;
   private float smoothness;
   private float rotation;
   private float u;
   private float v;
   private float texWidth;
   private float texHeight;
   private GpuTextureView textureId;

   public TextureBuilder size(SizeState size) {
      this.size = size;
      return this;
   }

   public TextureBuilder radius(QuadRadiusState radius) {
      this.radius = radius;
      return this;
   }

   public TextureBuilder color(QuadColorState color) {
      this.color = color;
      return this;
   }

   public TextureBuilder smoothness(float smoothness) {
      this.smoothness = smoothness;
      return this;
   }

   public TextureBuilder rotation(float rotation) {
      this.rotation = rotation;
      return this;
   }

   public TextureBuilder texture(float u, float v, float texWidth, float texHeight, AbstractTexture texture) {
      return this.texture(u, v, texWidth, texHeight, texture.getTextureView());
   }

   public TextureBuilder texture(float u, float v, float texWidth, float texHeight, GpuTextureView texture) {
      this.u = u;
      this.v = v;
      this.texWidth = texWidth;
      this.texHeight = texHeight;
      this.textureId = texture;
      return this;
   }

   protected TextureRenderer _build() {
      return new TextureRenderer(this.size, this.radius, this.color, this.smoothness, this.rotation, this.textureId);
   }

   @Override
   protected void reset() {
      this.size = SizeState.NONE;
      this.radius = QuadRadiusState.NO_ROUND;
      this.color = QuadColorState.WHITE;
      this.smoothness = 1.0F;
      this.rotation = 0.0F;
      this.u = 0.0F;
      this.v = 0.0F;
      this.texWidth = 0.0F;
      this.texHeight = 0.0F;
      this.textureId = null;
   }
}
