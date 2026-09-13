plugins {
    id("com.android.application")
}

android {
    namespace = "quebec.culture.donnees"
    compileSdk = 35

    defaultConfig {
        applicationId = "quebec.culture.donnees"
        minSdk = 26
        targetSdk = 35
        versionCode = 16
        versionName = "2.5.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
