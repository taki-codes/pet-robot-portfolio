plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.taki.petrobot.sensing"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 34
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.google.mlkit.face.detection)
    testImplementation(libs.junit)
}
