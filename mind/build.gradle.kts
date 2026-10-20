import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun prop(name: String, default: String = ""): String =
    providers.gradleProperty(name).orElse(default).get()

fun propInt(name: String, default: Int): Int = prop(name, default.toString()).toInt()

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
val envKeystorePath = System.getenv("KEYSTORE_FILE")
val hasUploadKey = keystorePropsFile.exists() || envKeystorePath != null
val bannerId = prop("focus.bannerUnitId").trim()

android {
    namespace = "com.ayaks.focus"
    compileSdk = propInt("focus.compileSdk", 36)

    defaultConfig {
        applicationId = prop("focus.applicationId", "com.ayaks.focus")
        minSdk = propInt("focus.minSdk", 23)
        targetSdk = propInt("focus.targetSdk", 36)
        versionCode = propInt("focus.versionCode", 1)
        versionName = prop("focus.versionName", "1.0.0")
        resourceConfigurations += listOf("en", "ru")
        buildConfigField("String", "BANNER_AD_UNIT_ID", "\"${bannerId.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("boolean", "ADS_CONFIGURED", bannerId.isNotEmpty().toString())
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
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

// Temporary CI diagnostics: print the failing Gradle task and cause as a check annotation.
gradle.taskGraph.addTaskExecutionListener(object : org.gradle.api.execution.TaskExecutionListener {
    override fun beforeExecute(task: org.gradle.api.Task) = Unit
    override fun afterExecute(task: org.gradle.api.Task, state: org.gradle.api.tasks.TaskState) {
        state.failure?.let { failure ->
            val details = failure.stackTraceToString()
                .take(4_000)
                .replace("%", "%25")
                .replace("\r", "%0D")
                .replace("\n", "%0A")
                .replace(":", "%3A")
            System.err.println("::error title=Gradle task failure::${task.path} ${details}")
        }
    }
})

dependencies {
    implementation("com.yandex.android:mobileads:8.6.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
