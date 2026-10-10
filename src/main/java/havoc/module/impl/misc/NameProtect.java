package havoc.module.impl.misc;

import havoc.Havoc;
import havoc.module.base.Category;
import havoc.module.base.Module;
import havoc.module.base.ModuleInfo;
import havoc.module.setting.BooleanSetting;
import havoc.module.setting.InputSetting;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleInfo(name = "NameProtect", desc = "Stores protected display name", category = Category.MISC)
public final class NameProtect extends Module {
   private final InputSetting name = this.registerSetting(new InputSetting("Имя", "Protected", 32));
   private final BooleanSetting friends = this.registerSetting(new BooleanSetting("Друзья", true));

   public String getProtectedName() {
      return this.name.getValue();
   }

   public String replace(String text) {
      if (this.isEnable() && text != null && mc.getUser() != null) {
         String result = replaceIgnoreCase(text, mc.getUser().getName(), this.name.getValue());
         if (this.friends.isValue() && Havoc.getInstance() != null) {
            for (String friend : Havoc.getInstance().getCommandManager().getFriends()) {
               result = replaceIgnoreCase(result, friend, this.name.getValue());
            }
         }

         return result;
      } else {
         return text;
      }
   }

   private static String replaceIgnoreCase(String text, String source, String replacement) {
      return source != null && !source.isBlank()
         ? Pattern.compile(Pattern.quote(source), 2)
            .matcher(text)
            .replaceAll(Matcher.quoteReplacement(replacement != null && !replacement.isBlank() ? replacement : "Protected"))
         : text;
   }
}
