import  com.mason.logic.config.Apps

plugins {
    id("mason.lib")
    id("kotlinx-serialization")
    id("maven-publish")
}

android {
    namespace = Apps.LIB_ID
}

dependencies {
    // main
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.coroutine)
    implementation(libs.kotlin.serialization)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work)
    implementation(libs.mason.util)
    implementation(libs.squareup.okhttp)
    implementation(platform(libs.squareup.okhttp.bom))
    implementation(libs.squareup.retrofit)

}
tasks.register<Jar>("androidSourcesJar") {
    from(android.sourceSets["main"].java.srcDirs)
    archiveClassifier.set("sources")
}
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("releaseAar") {
                version = Apps.VERSION_NAME
                groupId = Apps.GROUP_ID
                artifactId = Apps.ARTIFACT_ID

                artifact(tasks["bundleReleaseAar"])
                artifact(tasks["androidSourcesJar"])
            }
            // Creates a Maven publication called “snapshot”.
            create<MavenPublication>("snapshotAar") {
                // Applies the component for the snapshot build variant.
                groupId = Apps.GROUP_ID
                artifactId = Apps.ARTIFACT_ID
                version = "${Apps.VERSION_NAME}-SNAPSHOT"

                artifact(tasks["bundleReleaseAar"])
                artifact(tasks["androidSourcesJar"])
            }
        }
    }
}