package havoc.render.builders.impl;

import havoc.render.builders.AbstractBuilder;
import havoc.render.builders.states.QuadColorState;
import havoc.render.builders.states.QuadRadiusState;
import havoc.render.builders.states.SizeState;
import havoc.render.renderers.BorderRenderer;

public final class BorderBuilder extends AbstractBuilder<BorderRenderer> {
   private SizeState size;
   private QuadRadiusState radius;
   private QuadColorState color;
   private float thickness;
   private float internalSmoothness;
   private float externalSmoothness;

   public BorderBuilder size(SizeState size) {
      this.size = size;
      return this;
   }

   public BorderBuilder radius(QuadRadiusState radius) {
      this.radius = radius;
      return this;
   }

   public BorderBuilder color(QuadColorState color) {
      this.color = color;
      return this;
   }

   public BorderBuilder thickness(float thickness) {
      this.thickness = thickness;
      return this;
   }

   public BorderBuilder smoothness(float internalSmoothness, float externalSmoothness) {
      this.internalSmoothness = internalSmoothness;
      this.externalSmoothness = externalSmoothness;
      return this;
   }

   protected BorderRenderer _build() {
      return new BorderRenderer(this.size, this.radius, this.color, this.thickness, this.internalSmoothness, this.externalSmoothness);
   }

   @Override
   protected void reset() {
      this.size = SizeState.NONE;
      this.radius = QuadRadiusState.NO_ROUND;
      this.color = QuadColorState.TRANSPARENT;
      this.thickness = 0.0F;
      this.internalSmoothness = 1.0F;
      this.externalSmoothness = 1.0F;
   }
}
