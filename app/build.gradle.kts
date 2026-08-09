plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.mg4.launcher.simple"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mg4.launcher.simple"
        minSdk = 28
        targetSdk = 34
        versionCode = 6
        versionName = "1.5.0"
    }

    // Signed with the SAME platform keystore as the rest of the MG4 suite (MG4Control,
    // MG4Tasker). The launcher claims no privileged permission of its own — the shared key
    // is what makes the suite one installable set, and it is what the manual download
    // signature check compares an incoming APK against.
    val keystorePath = System.getenv("MG4_KEYSTORE") ?: (project.findProperty("mg4.keystore") as String?)
    signingConfigs {
        if (keystorePath != null && file(keystorePath).exists()) {
            create("platform") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("MG4_KEYSTORE_PASSWORD") ?: (project.findProperty("mg4.keystore.password") as String?)
                keyAlias = System.getenv("MG4_KEY_ALIAS") ?: (project.findProperty("mg4.key.alias") as String?) ?: "platform"
                keyPassword = System.getenv("MG4_KEY_PASSWORD") ?: (project.findProperty("mg4.key.password") as String?)
            }
        }
    }

    // Distribution channels (mirrors MG4Tasker / MG4Control / ABRP):
    // Both channels have no self-updater and no privileged installer. The MG4Suite screen
    // may check releases and export a verified APK only after explicit user actions.
    flavorDimensions += "channel"
    productFlavors {
        create("stable") {
            dimension = "channel"
        }
        create("unstable") {
            dimension = "channel"
            applicationIdSuffix = ".unstable"
            // Package Installer orders updates by versionCode. Keep the rolling build
            // number in it as well as versionName so Android sees each build as an upgrade.
            versionCode = defaultConfig.versionCode!! * 100_000 +
                (project.findProperty("unstableBuild")?.toString()?.toIntOrNull() ?: 0)
            // Version stays numerically comparable for Android ("1.5.0.42-unstable"):
            // the CI passes -PunstableBuild=<n>; 0 locally.
            versionName = "${defaultConfig.versionName}.${project.findProperty("unstableBuild") ?: "0"}"
            versionNameSuffix = "-unstable"
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            // R8 off: an unminified APK stays verifiable line-by-line against this source,
            // which matters more here than a few hundred kB on a head unit.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("platform")?.let { signingConfig = it }
        }
        debug {
            signingConfigs.findByName("platform")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Prints the unstable versionName so the unstable workflow can name the APK asset
// numerically comparable ("MG4SimpleLauncher-unstable-1.5.0.42.apk"). The pre-release itself
// is always tagged "unstable" and overwritten, so the asset name carries the version.
tasks.register("printUnstableVersion") {
    doLast {
        println("${android.defaultConfig.versionName}.${project.findProperty("unstableBuild") ?: "0"}")
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)
    implementation("com.google.zxing:core:3.5.3")

    testImplementation(libs.junit)
}
