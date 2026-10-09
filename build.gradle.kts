import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

// Bytecode level must match the oldest supported IDE: 2023.1 runs on Java 17, so anything compiled
// at a higher target would fail to load there with UnsupportedClassVersionError.
//
// verifyPluginProjectConfiguration will warn that "sourceCompatibility is too low for target
// platform" when compiling against a recent IDE (e.g. WebStorm 2026.2 wants Java 25). That warning
// is expected here and is deliberately not acted on: Java 17 bytecode runs on both old and new
// IDEs, which is what broad compatibility requires. The same task also warns that since-build is
// below the target platform version, which follows from the same choice. To silence both, compile
// against a 2023.1 IDE via -PplatformPath=... — which is also the safest way to guarantee no
// newer-only API is used by accident.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        // Target platform for compilation. Taken from the `platformPath` Gradle property so the
        // absolute path lives in one obvious place instead of being hard-wired here.
        // Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html#target-versions
        local(providers.gradleProperty("platformPath"))

        testFramework(TestFrameworkType.Platform)
    }
}

// patchPluginXml overwrites since-build with the build number of the IDE it compiles against, which
// would pin this plugin to a single platform version and break it everywhere else. The supported
// range is stated explicitly instead, and left open-ended so newer IDEs keep working.
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#patchPluginXml
intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "231" // 2023.1 — floor imposed by ProjectActivity and ActionUiKind
        }
    }
}
