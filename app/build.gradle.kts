plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.fakerun.fakerun"
    compileSdk = 33
    buildToolsVersion = "30.0.2"

    defaultConfig {
        applicationId = "com.fakerun.fakerun"
        minSdk = 29
        targetSdk = 33
        versionCode = 13
        versionName = "1.3.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }


    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = null  // using default one
    }

}

dependencies {

    implementation ("org.jetbrains.kotlin:kotlin-stdlib:2.2.0")

    implementation ("com.google.android.material:material:1.12.0")


    implementation ("com.squareup.okhttp3:okhttp:4.9.0")

    testImplementation ("junit:junit:4.+")
    androidTestImplementation ("androidx.test.ext:junit:1.1.2")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.3.0")
}