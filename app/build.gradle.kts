plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.budgetsmart2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.budgetsmart2"
        minSdk = 26
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //RecycleView
    implementation(libs.androidx.recyclerview)

    //viewModelScope
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Circular Progress Bar
    implementation(libs.circularprogressbar)

    // Circle Image View for profile pictures
    implementation(libs.circleimageview)

    //MPAndroidChart
    implementation(libs.mpandroidchart)

    //Firebase
    implementation(platform(libs.firebase.bom)) // Bom
    implementation(libs.firebase.ui.auth) // Firebase AuthUI
    implementation(libs.firebase.firestore) // fireStore

    // Google Play services
    implementation(libs.play.services.auth)

    //Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    //Glide
    implementation(libs.glide)

}

kapt {
    correctErrorTypes = true
}