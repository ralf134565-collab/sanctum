// SPDX-License-Identifier: GPL-3.0-or-later
// Модуль `app` — единая точка сборки. На текущем этапе монолит,
// но архитектура (data / domain / presentation) уже изолирована по пакетам,
// чтобы в будущем легко выделить multi-module Gradle структуру.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.pocketreflect.app"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        // Уникальный applicationId продакшн-проекта (Local-First приватный дневник).
        applicationId = "com.pocketreflect.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 4
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Схемы Room коммитим в репозиторий — пригодятся для тестов миграций.
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
    }

    flavorDimensions.add("store")

    productFlavors {
        create("global") {
            dimension = "store"
        }
        create("rustore") {
            dimension = "store"
            applicationIdSuffix = ".rustore"
        }
    }

    val signingPropertiesFile = rootProject.file("release-signing.properties")
    signingConfigs {
        create("release") {
            if (signingPropertiesFile.exists()) {
                val properties = Properties().apply {
                    signingPropertiesFile.inputStream().use { load(it) }
                }
                storeFile = rootProject.file(properties.getProperty("storeFile"))
                storePassword = properties.getProperty("storePassword")
                keyAlias = properties.getProperty("keyAlias")
                keyPassword = properties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingPropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // `android.util.Log` на JVM-юнит-тестах — пустой stub: по умолчанию он
    // бросает `RuntimeException("Method ... not mocked")`. У нас есть один
    // legitimate `Log.w(...)` в `EngineCoordinator` (логируем fallback на mock
    // при падении real-движка) — поэтому включаем стандартный Android-флаг,
    // чтобы заглушки молча возвращали дефолтные значения вместо exception.
    // Документация: https://developer.android.com/studio/test/unit-testing/local-tests#error-not-mocked
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            // Robolectric + Room MigrationTestHelper на JVM.
            isIncludeAndroidResources = true
        }
    }
}

// Kotlin 2.0+ ввёл единый `kotlin { compilerOptions { ... } }` DSL и пометил
// старый `android.kotlinOptions { ... }` deprecated. На Kotlin 2.3.x старый
// блок становится hard error, поэтому в Sub-PR #3a мы переехали сюда.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            // KT-73255: явный opt-in на новое поведение default target для
            // аннотаций на параметрах конструктора. В Kotlin 2.3+ аннотации
            // вроде `@Inject` на `class Foo @Inject constructor(val x: Bar)`
            // по умолчанию будут применяться И к value parameter, И к property
            // (раньше — только к параметру). Без этого флага компилятор сыпет
            // warning на каждый `@Inject`-конструктор (6+ файлов у нас).
            // Включение здесь, а не точечно через @param: — потому что
            // Hilt-стиль `@Inject constructor` у нас идиома проекта,
            // и применяться к property он должен везде одинаково.
            "-Xannotation-default-target=param-property",
        )
    }
}

dependencies {

    implementation(project(":feature-mandala"))
    implementation(project(":feature-insights"))

    // --- AndroidX / Lifecycle ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    // --- Compose UI (BOM) ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // --- Coroutines ---
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // --- Room (история записей + AI-профиль) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // --- Hilt DI ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // --- WorkManager (прогрев локальной модели ИИ) ---
    implementation(libs.androidx.work.runtime.ktx)

    // --- Security: биометрия, SQLCipher (Room), аппаратное шифрование кодовой фразы ---
    implementation(libs.androidx.biometric)
    implementation(libs.sqlcipher.android)

    // --- Пользовательские настройки (DataStore<Preferences>, не SharedPreferences) ---
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.glance.appwidget)

    // --- Media3: локальный ambient-плеер (без сети) ---
    implementation(libs.androidx.media3.exoplayer)

    // --- Encrypted backup payload (JSON внутри .sanctum файла) ---
    implementation(libs.kotlinx.serialization.json)

    // --- LiteRT-LM: локальный рантайм для Gemma 4 ---
    // Подключён в Sub-PR #3b. Контракт `GemmaLocalEngine` теперь реализуется
    // настоящим `LiteRtGemmaEngine` (`com.google.ai.edge.litertlm.Engine`),
    // обёрнутым в `EngineCoordinator` (decorator → fallback на mock пока
    // модель не прикреплена или real падает). См. `di/AIModule.kt`.
    //
    // Backend по умолчанию — GPU с автофоллбеком на CPU при init-ошибке;
    // пользовательский toggle GPU/CPU добавится в Sub-PR #3c.
    //
    // Для GPU-бэкенда в AndroidManifest добавлены `uses-native-library`
    // libOpenCL.so + libvndksupport.so (required="false"), а в
    // `proguard-rules.pro` — keep-правила для `com.google.ai.edge.litertlm.**`.
    implementation(libs.litert.lm)

    // --- Tests ---
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.datastore.preferences.core)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
