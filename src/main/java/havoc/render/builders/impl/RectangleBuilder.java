package havoc.render.builders.impl;

import havoc.render.builders.AbstractBuilder;
import havoc.render.builders.states.QuadColorState;
import havoc.render.builders.states.QuadRadiusState;
import havoc.render.builders.states.SizeState;
import havoc.render.renderers.RectangleRenderer;

public final class RectangleBuilder extends AbstractBuilder<RectangleRenderer> {
   private SizeState size;
   private QuadRadiusState radius;
   private QuadColorState color;
   private float smoothness;

   public RectangleBuilder size(SizeState size) {
      this.size = size;
      return this;
   }

   public RectangleBuilder radius(QuadRadiusState radius) {
      this.radius = radius;
      return this;
   }

   public RectangleBuilder color(QuadColorState color) {
      this.color = color;
      return this;
   }

   public RectangleBuilder smoothness(float smoothness) {
      this.smoothness = smoothness;
      return this;
   }

   protected RectangleRenderer _build() {
      return new RectangleRenderer(this.size, this.radius, this.color, this.smoothness);
   }

   @Override
   protected void reset() {
      this.size = SizeState.NONE;
      this.radius = QuadRadiusState.NO_ROUND;
      this.color = QuadColorState.TRANSPARENT;
      this.smoothness = 1.0F;
   }
}
