package xyz.overdyn.dynconfig;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Глобальный контекст выбранного языка для конфигов.
 */
public final class LangContext {

    private static volatile String current = "en";

    private LangContext() {}

    /**
     * Устанавливает текущий язык (например, "en", "ru").
     */
    public static void set(@NotNull String code) {
        current = code.toLowerCase(Locale.ROOT);
    }

    /**
     * Возвращает текущий код языка.
     */
    @NotNull
    public static String get() {
        return current;
    }
}
