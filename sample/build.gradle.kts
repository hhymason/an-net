plugins {
    id("mason.app")
}

android {
    namespace = "com.mason.net.sample"

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // main
    implementation(project(":library"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.kotlin.coroutine)
    implementation(libs.kotlin.serialization)
    implementation(libs.mason.util)
}
