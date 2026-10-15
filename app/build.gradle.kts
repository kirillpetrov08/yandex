import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

/* ------------------------------------------------------------------ */
/*  Версии и параметры приложения берутся из gradle.properties         */
/* ------------------------------------------------------------------ */
fun prop(name: String): String = providers.gradleProperty(name).get()
fun propInt(name: String): Int = prop(name).toInt()

/* Подпись релиза: файл keystore.properties в корне проекта (в git не попадает)
   либо переменные окружения KEYSTORE_FILE / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD. */
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
val envKeystorePath: String? = System.getenv("KEYSTORE_FILE")
val hasUploadKey: Boolean = keystorePropsFile.exists() || envKeystorePath != null

/* Тестовая сборка по умолчанию использует демо-блоки Яндекса.
   Зачем: показы и клики по собственной рекламе на своём устройстве РСЯ расценивает как
   накрутку и блокирует аккаунт без восстановления. Релизная сборка (та, что уходит в
   магазины) всегда берёт реальные ID из gradle.properties.
   Отключается одной строкой в gradle.properties: ads.debugUseDemoIds=false */
val debugUsesDemoIds: Boolean = prop("ads.debugUseDemoIds").toBoolean()

android {
    namespace = "com.blockpuzzle.app"
    compileSdk = propInt("app.compileSdk")

    defaultConfig {
        applicationId = prop("app.applicationId")
        minSdk = propInt("app.minSdk")
        targetSdk = propInt("app.targetSdk")
        versionCode = propInt("app.versionCode")
        versionName = prop("app.versionName")
        resourceConfigurations += listOf("en", "ru")

        vectorDrawables.useSupportLibrary = true

        // ID рекламных блоков РСЯ -> BuildConfig (правится в gradle.properties)
        buildConfigField("String", "AD_UNIT_BANNER", "\"${prop("ads.bannerUnitId")}\"")
        buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"${prop("ads.interstitialUnitId")}\"")
        buildConfigField("String", "AD_UNIT_REWARDED", "\"${prop("ads.rewardedUnitId")}\"")
        buildConfigField("String", "AD_UNIT_APP_OPEN", "\"${prop("ads.appOpenUnitId")}\"")
        buildConfigField("String", "AD_UNIT_NATIVE", "\"${prop("ads.nativeUnitId")}\"")
        buildConfigField("boolean", "ADS_TEST_MODE", prop("ads.testMode"))
        buildConfigField("int", "AD_INTERSTITIAL_INTERVAL_SECONDS", prop("ads.interstitialIntervalSeconds"))
        buildConfigField("int", "AD_INTERSTITIAL_GAMES_INTERVAL", prop("ads.interstitialGamesInterval"))
        buildConfigField("int", "AD_APP_OPEN_INTERVAL_HOURS", prop("ads.appOpenIntervalHours"))
    }

    signingConfigs {
        if (hasUploadKey) {
            create("release") {
                storeFile = rootProject.file(envKeystorePath ?: keystoreProps.getProperty("storeFile"))
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: keystoreProps.getProperty("storePassword")
                keyAlias = System.getenv("KEY_ALIAS") ?: keystoreProps.getProperty("keyAlias")
                keyPassword = System.getenv("KEY_PASSWORD") ?: keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false

            // Демо-блоки в тестовой сборке (см. debugUsesDemoIds выше).
            if (debugUsesDemoIds) {
                buildConfigField("String", "AD_UNIT_BANNER", "\"demo-banner-yandex\"")
                buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"demo-interstitial-yandex\"")
                buildConfigField("String", "AD_UNIT_REWARDED", "\"demo-rewarded-yandex\"")
                buildConfigField("String", "AD_UNIT_APP_OPEN", "\"demo-appopenad-yandex\"")
                buildConfigField("String", "AD_UNIT_NATIVE", "\"demo-native-content-yandex\"")
                buildConfigField("boolean", "ADS_TEST_MODE", "true")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Если upload-ключ не задан — релиз подписывается debug-ключом, чтобы APK
            // можно было поставить на телефон и протестировать рекламу. Для Google Play
            // обязательно задайте keystore.properties (см. docs/10-google-play-release.md).
            signingConfig = if (hasUploadKey) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // Проверка актуальности версии SDK Яндекса (падает сборка при устаревшей версии).
        // Комментарий ниже — если нужно временно отключить: disable += "YandexAdsSdkOutdatedVersion"
        abortOnError = true
        checkReleaseBuilds = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

dependencies {
    // --- Яндекс: Mobile Ads SDK (РСЯ) ---
    implementation("com.yandex.android:mobileads:8.6.0")

    // --- AndroidX / Compose ---
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // --- Тесты игровой логики ---
    testImplementation("junit:junit:4.13.2")
}
