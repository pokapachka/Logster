plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.logster"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.logster"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Стандартные Android библиотеки
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.swiperefreshlayout)
    implementation(libs.flexbox)

    // HTML парсинг
    implementation(libs.jsoup)

    // HTTP клиент и WebSocket
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    // Загрузка изображений
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // JSON обработка
    implementation(libs.json)

    // Тестирование
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}