package havoc.module.base;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleInfo {
   String name();

   String desc();

   Category category();

   boolean enabledByDefault() default false;
}
