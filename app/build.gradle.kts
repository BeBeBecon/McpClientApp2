import java.util.Properties  // 🔧 明示的にPropertiesクラスをimport
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") // 🔧 シリアライゼーション用プラグイン
}

// 🔑 local.properties からAPIキーを安全に読み込み
val localProperties = Properties()  // 🔧 修正：java.util.を削除
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))  // 🔧 より明示的なファイル読み込み
}

// 🔐 APIキーの取得（オプショナル対応）
val anthropicApiKey: String = localProperties.getProperty("ANTHROPIC_API_KEY", "")  // 🔧 デフォルト空文字
val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY")
    ?: throw GradleException("GEMINI_API_KEY が local.properties に見つかりません")

android {
    namespace = "com.example.mcpclientapp2"  // 🎯 正しいパッケージ名
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mcpclientapp2"
        minSdk = 26  // 🔧 API 26以上に変更（Android 8.0以降）
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 🔑 BuildConfig でAPIキーを安全に提供
        buildConfigField("String", "ANTHROPIC_API_KEY", "\"$anthropicApiKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        // 🔧 APIキーの有無をフラグで管理
        buildConfigField("boolean", "HAS_ANTHROPIC_API_KEY", "${anthropicApiKey.isNotEmpty()}")
        buildConfigField("boolean", "HAS_GEMINI_API_KEY", "${geminiApiKey.isNotEmpty()}")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11  // 🆙 Java 11に更新
        targetCompatibility = JavaVersion.VERSION_11  // 🆙 Java 11に更新
    }

    kotlinOptions {
        jvmTarget = "11"  // 🆙 JVM 11に更新
    }

    buildFeatures {
        viewBinding = true  // 🎨 型安全なView参照のため
        buildConfig = true  // 🔑 BuildConfig を有効化（APIキー用）
    }

    // 🔧 重複ファイルの競合を解決するpackagingブロック
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",     // Apache HttpComponents由来の重複ファイル
                "META-INF/LICENSE",          // ライセンスファイルの重複
                "META-INF/LICENSE.txt",      // ライセンスファイルの重複
                "META-INF/license.txt",      // ライセンスファイルの重複
                "META-INF/NOTICE",           // 通知ファイルの重複
                "META-INF/NOTICE.txt",       // 通知ファイルの重複
                "META-INF/notice.txt",       // 通知ファイルの重複
                "META-INF/ASL2.0"            // Apache Software License 2.0
            )
        }
    }
}

dependencies {
    // 📱 Android基本ライブラリ（安定バージョン）
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // ⚡ 非同期処理（コルーチン）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // 🌐 ネットワーク通信（SSE対応）
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")  // SSE用

    // 📄 JSON処理
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // 🤖 AI SDK（Anthropic）- 安定バージョン
    implementation("com.anthropic:anthropic-java:0.8.0")

    // 🔄 ライフサイクル管理
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // 🧪 テスト
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}