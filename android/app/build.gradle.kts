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

// Fase zero: a chave da YouTube Data API vem do secret YOUTUBE_API_KEY do GitHub Actions,
// nunca do repositório (que é público). Sem ela, o app pede a chave dos pais, como antes.
val chaveYoutube = (System.getenv("YOUTUBE_API_KEY") ?: "").filter { it.isLetterOrDigit() || it == '-' || it == '_' }

// Assinatura fixa dos APKs de teste: com a mesma chave, cada versão nova instala por cima da
// anterior sem perder a configuração. O workflow grava o keystore do secret num arquivo temporário.
val keystoreTeste = System.getenv("ANDROID_KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.exists() }

android {
    namespace = "br.com.vsa.horadodesenho"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.com.vsa.horadodesenho"
        minSdk = 21
        targetSdk = 34
        versionCode = 7
        versionName = "0.6.0"
        buildConfigField("String", "YOUTUBE_API_KEY", "\"$chaveYoutube\"")
    }

    signingConfigs {
        if (keystoreTeste != null) create("teste") {
            storeFile = keystoreTeste
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: "horadodesenho"
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD") ?: System.getenv("ANDROID_KEYSTORE_PASSWORD")
        }
    }

    sourceSets["main"].assets.srcDir(webDir)

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            if (keystoreTeste != null) signingConfig = signingConfigs.getByName("teste")
        }
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
