plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.transferly"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.transferly"
        minSdk = 24
        //noinspection OldTargetApi
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation ("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")

    implementation("org.mindrot:jbcrypt:0.4")

    implementation ("com.google.code.gson:gson:2.8.9")

    implementation ("org.nanohttpd:nanohttpd:2.3.1")

    implementation ("com.google.android.material:material:1.11.0")

    implementation("jp.wasabeef:glide-transformations:4.3.0")

    implementation ("com.google.android.gms:play-services-auth:20.7.0")

    implementation ("commons-net:commons-net:3.8.0")

    implementation ("com.squareup.okhttp3:okhttp:4.10.0")

    implementation ("org.json:json:20210307")

    implementation ("com.android.volley:volley:1.2.1")

    implementation ("com.github.CanHub:Android-Image-Cropper:4.3.3")

    implementation ("com.google.android.material:material:1.11.0")

    implementation ("com.squareup.okhttp3:okhttp:4.12.0")




}