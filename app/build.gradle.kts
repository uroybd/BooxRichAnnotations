plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "me.utsob.booxrichannotation"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "me.utsob.booxrichannotation"
        minSdk = 24
        targetSdk = 36
        versionCode = 14
        versionName = "1.9.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Use debug signing for easier distribution
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    
    splits {
        abi {
            isEnable = true
            reset()
            // Common ARM architectures in Boox devices
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.material)
    
    // Pebble templating engine
    implementation("io.pebbletemplates:pebble:3.2.2")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}