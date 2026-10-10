plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}
val releaseKey = System.getenv("MADARIJ_KEYSTORE")
android {
    namespace = "ru.madarij.nativeapp"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    signingConfigs.getByName("debug") {
        storeFile = rootProject.file("development-signing/debug.keystore")
    }
    defaultConfig {
        applicationId = "ru.madarij.nativeapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 32
        versionName = "1.32-paper-refinement-r8"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    if (releaseKey != null) signingConfigs.create("owner") {
        storeFile = file(releaseKey)
        storePassword = System.getenv("MADARIJ_STORE_PASSWORD")
        keyAlias = System.getenv("MADARIJ_KEY_ALIAS")
        keyPassword = System.getenv("MADARIJ_KEY_PASSWORD")
    }
    buildTypes {
        release {
            applicationIdSuffix = if (providers.gradleProperty("premiumPreview").orNull == "true") ".referencepreview" else ".readerpolish"
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = if (releaseKey != null) signingConfigs.getByName("owner") else signingConfigs.getByName("debug")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}
kapt { arguments { arg("room.schemaLocation", "$projectDir/schemas") } }
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    testImplementation("junit:junit:4.13.2")
}
val generateBookTextures by tasks.registering(Exec::class) {
    workingDir(rootProject.projectDir)
    commandLine("python3", "tools/generate_book_textures.py")
}
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(generateBookTextures) }
val verifyCorpus by tasks.registering(Exec::class) {
    workingDir(rootProject.projectDir)
    commandLine("python3", "-m", "tools.content_gate", "app/src/main/assets/corpus.json")
}
val verifyReleaseAssets by tasks.registering {
    doLast {
        check(file("src/main/res/font/naskh.ttf").isFile) { "Embedded Naskh missing" }
        check(file("src/main/assets/fonts/OFL.txt").isFile) { "Font license missing" }
        if (releaseKey == null) logger.lifecycle("Owner signing not configured; using development signing for test APK")
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(verifyReleaseAssets)
    if (rootProject.file("tools/content_gate.py").isFile) dependsOn(verifyCorpus)
}
