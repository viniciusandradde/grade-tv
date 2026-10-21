plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val webDir = layout.buildDirectory.dir("generated/web").get().asFile
val copyWeb by tasks.registering(Copy::class) {
    from(rootProject.projectDir.resolve("web"))
    into(webDir)
}

android {
    namespace = "br.com.vsa.tempoemfamilia"
    compileSdk = 34
    defaultConfig {
        applicationId = "br.com.vsa.tempoemfamilia"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }
    sourceSets["main"].assets.srcDir(webDir)
    buildFeatures { buildConfig = true }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
tasks.named("preBuild") { dependsOn(copyWeb) }
dependencies { implementation("androidx.webkit:webkit:1.11.0") }
