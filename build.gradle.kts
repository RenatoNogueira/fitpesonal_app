plugins {
    id("com.android.library") version "8.9.3" apply false
    id("com.android.application") version "8.9.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("com.google.dagger.hilt.android") version "2.52" apply false
    // Firebase (analytics opcional): só é aplicado no app quando existe app/google-services.json
    id("com.google.gms.google-services") version "4.4.2" apply false
}
