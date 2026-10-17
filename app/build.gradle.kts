plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}
val releaseKey = System.getenv("MADARIJ_KEYSTORE")
val playCandidate = providers.gradleProperty("playCandidate").orNull == "true"
val playProduction = providers.gradleProperty("playProduction").orNull == "true"
check(!playCandidate || !playProduction) { "Play candidate and production modes are mutually exclusive" }
if (playProduction) {
    check(!releaseKey.isNullOrBlank() &&
        !System.getenv("MADARIJ_STORE_PASSWORD").isNullOrBlank() &&
        !System.getenv("MADARIJ_KEY_ALIAS").isNullOrBlank() &&
        !System.getenv("MADARIJ_KEY_PASSWORD").isNullOrBlank()
    ) { "Google Play bundle requires the owner's persistent upload keystore and all signing secrets" }
}
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
        versionCode = 58
        versionName = "1.58-volume2-pages3-659-translation-collation"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appTitle"] = "Степени идущих · Тест 1.58"
    }
    if (releaseKey != null) signingConfigs.create("owner") {
        storeFile = file(releaseKey)
        storePassword = System.getenv("MADARIJ_STORE_PASSWORD")
        keyAlias = System.getenv("MADARIJ_KEY_ALIAS")
        keyPassword = System.getenv("MADARIJ_KEY_PASSWORD")
    }
    buildTypes {
        debug {
            // A debug APK has a new signing key in each CI run. Use a distinct
            // application ID so the user's installed reader and its Room data
            // are never uninstalled or overwritten to try the preview.
            applicationIdSuffix = ".qa${System.getenv("GITHUB_RUN_NUMBER") ?: "local"}"
        }
        release {
            // Candidate bundles are intentionally NOT the Play package, and
            // therefore cannot accidentally reserve the production application ID.
            // Production releases keep the base applicationId and REQUIRE owner key.
            if (!playProduction) {
                applicationIdSuffix = when {
                    playCandidate -> ".playqa${System.getenv("GITHUB_RUN_NUMBER") ?: "local"}"
                    providers.gradleProperty("premiumPreview").orNull == "true" -> ".referencepreview"
                    providers.gradleProperty("ciIsolatedRelease").orNull == "true" ->
                        ".r8qa${System.getenv("GITHUB_RUN_NUMBER") ?: "local"}"
                    else -> ".collation158"
                }
            }
            if (playProduction || playCandidate) {
                manifestPlaceholders["appTitle"] = "Степени идущих"
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = if (playProduction || (!releaseKey.isNullOrBlank() && !playCandidate))
                signingConfigs.getByName("owner") else signingConfigs.getByName("debug")
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
val generateVintageAssets by tasks.registering(Exec::class) {
    workingDir(rootProject.projectDir)
    commandLine("python3", "tools/generate_vintage_assets.py")
}
val generateBookTextures by tasks.registering(Exec::class) {
    workingDir(rootProject.projectDir)
    commandLine("python3", "tools/generate_book_textures.py")
}
// The OFL-licensed Cyrillic handwriting face is verified against the official
// Google Fonts Git object hash, then packaged as an Android font resource.
val prepareHandwritingFont by tasks.registering(Exec::class) {
    workingDir(rootProject.projectDir)
    commandLine("python3", "tools/prepare_handwriting_font.py")
}
val verifyKnowledgeReminders by tasks.registering(Exec::class) {
    workingDir(rootProject.projectDir)
    commandLine("python3", "tools/check_home_reminders.py")
}
val prepareCuratedReminders by tasks.registering(Exec::class) {
    workingDir(rootProject.projectDir)
    commandLine("python3", "tools/generate_curated_reminders.py")
}
val prepareStudySupplement by tasks.registering(Exec::class) {
    workingDir(rootProject.projectDir)
    commandLine("python3", "tools/generate_study_supplement.py")
}
tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(generateBookTextures, generateVintageAssets, verifyKnowledgeReminders, prepareCuratedReminders, prepareStudySupplement, prepareHandwritingFont)
}
// Production Play uploads are forbidden until edition/editorial rights,
// Arabic collation, scholarly references and Data Safety are documented.
val verifyPlayProductionClearance by tasks.registering(Exec::class) {
    workingDir(rootProject.projectDir)
    commandLine("python3", "tools/play_release_gate.py", "--mode", "production")
}
val verifyCorpus by tasks.registering(Exec::class) {
    workingDir(rootProject.projectDir)
    commandLine("python3", "-m", "tools.content_gate", "app/src/main/assets/corpus.json")
}
val verifyReleaseAssets by tasks.registering {
    doLast {
        check(file("src/main/res/font/naskh.ttf").isFile) { "Embedded Naskh missing" }
        check(file("src/main/assets/fonts/OFL.txt").isFile) { "Font license missing" }
        check(file("src/main/assets/fonts/BadScript-OFL.txt").isFile) { "New handwriting font license missing" }
        check(file("src/main/res/font/bad_script.ttf").isFile) { "Locally bundled Bad Script Cyrillic font missing" }
        check(file("src/main/res/font/caveat_handwriting.ttf").isFile) { "Caveat Cyrillic handwriting font missing" }
        check(file("src/main/assets/fonts/Caveat-OFL.txt").isFile) { "Caveat OFL license missing" }
        if (releaseKey == null) logger.lifecycle("Owner signing not configured; using development signing for test APK")
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(verifyReleaseAssets)
    if (rootProject.file("tools/content_gate.py").isFile) dependsOn(verifyCorpus)
    if (playProduction) dependsOn(verifyPlayProductionClearance)
}
