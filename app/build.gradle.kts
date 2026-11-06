plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val winlatorRoot = rootProject.file("runtime/winlator-app/app/src/main")
val winlatorJavaSource = winlatorRoot.resolve("java")
val winlatorJavaGenerated = layout.buildDirectory.dir("generated/winlator/java")
val winlatorResGenerated = layout.buildDirectory.dir("generated/winlator/res")
val winlatorNoticesGenerated = layout.buildDirectory.dir("generated/winlator/assets")

// Keep upstream source pristine in its pinned submodule. This app uses its own
// applicationId, so adapt Winlator's one hard-coded FileProvider authority in
// a generated build copy rather than editing upstream files in-place.
val prepareWinlatorJava = tasks.register<Copy>("prepareWinlatorJava") {
    from(winlatorJavaSource)
    into(winlatorJavaGenerated)
    doLast {
        val file = winlatorJavaGenerated.get().file("com/winlator/core/FileUtils.java").asFile
        val original = file.readText()
        val oldAuthority = "\"com.winlator.FileProvider\""
        check(original.contains(oldAuthority)) {
            "Pinned Winlator FileProvider code changed; review the appId authority patch."
        }
        file.writeText(original.replace(oldAuthority, "activity.getPackageName() + \".FileProvider\""))
    }
}

val prepareWinlatorResources = tasks.register<Copy>("prepareWinlatorResources") {
    from(winlatorRoot.resolve("res")) {
        // Keep PROJECT R's launcher identity; all other Winlator resources remain intact.
        exclude("mipmap-*/ic_launcher.png", "mipmap-*/ic_launcher_round.png")
        exclude("mipmap-anydpi-v26/ic_launcher.xml", "mipmap-anydpi-v26/ic_launcher_round.xml")
    }
    into(winlatorResGenerated)
    doLast {
        val file = winlatorResGenerated.get().file("values/strings.xml").asFile
        val original = file.readText()
        val appName = Regex("(?m)^\\s*<string name=\"app_name\">[^<]*</string>\\s*\\n?")
        check(appName.containsMatchIn(original)) {
            "Pinned Winlator app_name resource changed; review PROJECT R resource merge."
        }
        file.writeText(appName.replace(original, ""))
    }
}

val prepareWinlatorNotices = tasks.register<Copy>("prepareWinlatorNotices") {
    from(rootProject.file("runtime/winlator-app/LICENSE")) {
        rename { "WINLATOR-LGPL-2.1.txt" }
    }
    from(rootProject.file("runtime/winlator-app/README.md")) {
        rename { "WINLATOR-UPSTREAM-CREDITS.txt" }
    }
    into(winlatorNoticesGenerated.map { it.dir("licenses") })
}

tasks.named("preBuild").configure {
    dependsOn(prepareWinlatorJava, prepareWinlatorResources, prepareWinlatorNotices)
}

android {
    // Winlator's Java source expects the generated R class in this namespace.
    // Keep PROJECT R's existing applicationId so this remains an in-place app update.
    namespace = "com.winlator"
    compileSdk = 35
    ndkVersion = "24.0.8215888"

    defaultConfig {
        applicationId = "com.projectr.launcher"
        minSdk = 26
        // Match Winlator's current storage/runtime assumptions for this first port.
        targetSdk = 28
        versionCode = 4
        versionName = "0.4.0"
        ndk { abiFilters += "arm64-v8a" }
    }

    sourceSets.getByName("main").apply {
        java.srcDir(winlatorJavaGenerated)
        res.srcDir(winlatorResGenerated)
        assets.srcDir(winlatorRoot.resolve("assets"))
        assets.srcDir(winlatorNoticesGenerated)
        jniLibs.srcDir(winlatorRoot.resolve("jniLibs"))
    }

    externalNativeBuild {
        cmake {
            path = file("../runtime/winlator-app/app/src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    androidResources {
        noCompress += listOf("tzst", "sf2", "icp")
    }

    packaging {
        jniLibs { useLegacyPackaging = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.5.1")
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.google.android.material:material:1.4.0")
    implementation("com.github.luben:zstd-jni:1.5.2-3@aar")
    implementation("org.tukaani:xz:1.7")
    implementation("org.apache.commons:commons-compress:1.20")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
