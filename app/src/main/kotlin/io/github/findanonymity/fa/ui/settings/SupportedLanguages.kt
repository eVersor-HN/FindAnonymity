package io.github.findanonymity.fa.ui.settings

data class SupportedLanguage(val tag: String, val nativeName: String)

val SUPPORTED_LANGUAGES = listOf(
    SupportedLanguage("en", "English"),
    SupportedLanguage("de", "Deutsch"),
    SupportedLanguage("es", "Español"),
    SupportedLanguage("fr", "Français"),
    SupportedLanguage("it", "Italiano"),
    SupportedLanguage("pt", "Português"),
    SupportedLanguage("ru", "Русский"),
    SupportedLanguage("zh", "中文"),
    SupportedLanguage("ja", "日本語"),
    SupportedLanguage("ko", "한국어"),
    SupportedLanguage("ar", "العربية"),
    SupportedLanguage("hi", "हिन्दी"),
    SupportedLanguage("tr", "Türkçe"),
    SupportedLanguage("pl", "Polski"),
    SupportedLanguage("nl", "Nederlands"),
)
