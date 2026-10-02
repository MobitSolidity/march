import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing. CI passes env vars (see README); locally you can use Bazaaryar/keystore.properties.
// Neither the keystore nor keystore.properties may ever be committed.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(env: String, prop: String): String? = System.getenv(env) ?: keystoreProps.getProperty(prop)
val releaseStore: String? = signingValue("BAZAARYAR_KEYSTORE", "storeFile")

android {
    namespace = "ir.bazaaryar.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "ir.bazaaryar.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "2.0"
    }

    signingConfigs {
        if (releaseStore != null) {
            create("release") {
                storeFile = rootProject.file(releaseStore)
                storePassword = signingValue("BAZAARYAR_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("BAZAARYAR_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("BAZAARYAR_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Without a release keystore the build still works, but is signed with the debug key and is NOT publishable.
            signingConfig = if (releaseStore != null) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    lint { abortOnError = false }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.core:core-ktx:1.13.1")
    // WebSocket (Binance live ticks) + HTTP with gzip and connection pooling. Ships its own R8 rules.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
