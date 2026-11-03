// :app - ứng dụng Android (Java) - client duy nhất của hệ thống.
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "vn.edu.ptit.mobilechess"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "vn.edu.ptit.mobilechess"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":common"))
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
}
