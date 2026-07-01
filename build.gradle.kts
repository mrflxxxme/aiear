// Root build script. Все плагины объявлены `apply false` — версии берутся из
// gradle/libs.versions.toml (single source of versions). Применяются в :app.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}
