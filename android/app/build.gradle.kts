plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// A interface é o mesmo index.html da raiz do repositório, empacotado como asset.
val webDir = layout.buildDirectory.dir("generated/web").get().asFile
val copyWeb by tasks.registering(Copy::class) {
    from(rootProject.projectDir.resolve("../index.html"))
    from(rootProject.projectDir.resolve("../img")) { into("img") }
    into(webDir)
}

android {
    namespace = "br.com.vsa.horadodesenho"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.com.vsa.horadodesenho"
        minSdk = 21
        targetSdk = 34
        versionCode = 6
        versionName = "0.5.0"
    }

    sourceSets["main"].assets.srcDir(webDir)

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.named("preBuild") { dependsOn(copyWeb) }

dependencies {
    implementation("androidx.webkit:webkit:1.11.0")
}
