package havoc.module.base;

import havoc.module.impl.combat.AutoSwap;
import havoc.module.impl.misc.AntiCapture;
import havoc.module.impl.misc.AutoAccept;
import havoc.module.impl.misc.ChatHelper;
import havoc.module.impl.misc.DiscordRPC;
import havoc.module.impl.misc.ScoreboardHealth;
import havoc.module.impl.movement.AutoSprint;
import havoc.module.impl.player.FakePlayer;
import havoc.module.impl.visuals.Ambience;
import havoc.module.impl.visuals.ArrayListModule;
import havoc.module.impl.visuals.Cosmetics;
import havoc.module.impl.visuals.Interface;
import havoc.module.impl.visuals.ItemReplacer;
import havoc.module.impl.visuals.KillEffect;
import havoc.module.impl.visuals.Menu;
import havoc.module.impl.visuals.NoRender;
import havoc.module.impl.visuals.Particles;
import havoc.module.impl.visuals.Predictions;
import havoc.module.impl.visuals.SwingAnimation;
import havoc.module.impl.visuals.TargetESP;
import havoc.module.impl.visuals.ViewModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Generated;

public class ModuleManager {
   private final List<Module> modules = new ArrayList<>();

   public ModuleManager() {
      this.modules
         .addAll(
            Arrays.asList(
               new Menu(),
               new Interface(),
               new ArrayListModule(),
               new NoRender(),
               new TargetESP(),
               new Predictions(),
               new Ambience(),
               new Particles(),
               new ViewModel(),
               new SwingAnimation(),
               new ItemReplacer(),
               new AutoSprint(),
               new AutoSwap(),
               new FakePlayer(),
               new DiscordRPC(),
               new AntiCapture(),
               new KillEffect(),
               new Cosmetics(),
               new ChatHelper(),
               new ScoreboardHealth(),
               new AutoAccept()
            )
         );

      for (Module module : this.modules) {
         ModuleInfo info = module.getClass().getAnnotation(ModuleInfo.class);
         if (info != null && info.enabledByDefault() && !module.isEnable()) {
            module.toggle();
         }
      }
   }

   public List<Module> getModules(Category moduleCategory) {
      return this.modules.stream().filter(module -> module.getCategory() == moduleCategory).toList();
   }

   public <T extends Module> T getModule(Class<T> tClass) {
      return (T)this.modules.stream().filter(module -> module.getClass() == tClass).findFirst().orElse(null);
   }

   @Generated
   public List<Module> getModules() {
      return this.modules;
   }
}
