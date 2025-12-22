package xyz.overdyn.dynconfig.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Comment {

    Entry[] value();

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface Entry {
        String lang();      // "ru", "en", "de", ...
        String[] lines();   // сами комментарии
    }
}
