package havoc.module.impl.player;

import com.google.common.eventbus.Subscribe;
import havoc.event.impl.EventUpdate;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.utilites.rotation.SilentAimManager;
import net.minecraft.client.CameraType;

@ModuleInfo(name = "FreeLook", desc = "Independent camera view", category = Category.PLAYER)
public final class FreeLook extends Module {
   private final BooleanSetting active = this.registerSetting(new BooleanSetting("Свободный обзор", false));
   private CameraType previousPerspective;

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null) {
         if (this.active.isValue()) {
            if (this.previousPerspective == null) {
               this.previousPerspective = mc.options.getCameraType();
               SilentAimManager.apply(0.0F, 0.0F);
            }

            if (mc.options.getCameraType().isFirstPerson()) {
               mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
         } else {
            this.restore();
         }
      }
   }

   @Override
   public void onDisable() {
      this.restore();
      super.onDisable();
   }

   private void restore() {
      if (this.previousPerspective != null) {
         mc.options.setCameraType(this.previousPerspective);
      }

      this.previousPerspective = null;
      SilentAimManager.clear();
   }
}
