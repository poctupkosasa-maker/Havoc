package havoc.render.renderers.pipeline;

import lombok.Generated;

public final class PipelineInitializer {
   private static boolean initialized = false;

   public static void init() {
      if (!initialized) {
         initialized = true;
         PipelineRegistry.init();
      }
   }

   private PipelineInitializer() {
   }

   @Generated
   public static boolean isInitialized() {
      return initialized;
   }
}
