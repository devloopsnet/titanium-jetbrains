import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.6.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // JUnit 4 for the pure unit tests (the platform test framework doesn't bring it in).
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        // --- Compile base -------------------------------------------------------
        // Compile against IDEA Ultimate because it BUNDLES the JavaScript plugin.
        // This is a compile-time choice only; runtime gating is done by the
        // optional <depends> in plugin.xml, so the plugin still loads on
        // Community IDEs and Android Studio.
        create(
            providers.gradleProperty("platformType"),
            providers.gradleProperty("platformVersion"),
        )

        // --- Optional JavaScript support ---------------------------------------
        // bundledPlugin() (NOT plugin("JavaScript", ...)) because the JS plugin is
        // not distributed on the Marketplace. Only puts JS classes on the compile
        // classpath; the optional <depends> keeps it optional at runtime.
        bundledPlugin("JavaScript")

        // --- Tooling ------------------------------------------------------------
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        id = providers.gradleProperty("pluginGroup")
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            // Open-ended: don't strand users when they upgrade the IDE. Rely on
            // the Plugin Verifier matrix instead of a hard untilBuild ceiling.
            untilBuild = provider { null }
        }

        vendor {
            name = "Titanium Community"
            url = "https://titaniumsdk.com"
        }

        changeNotes = provider {
            """
            <ul>
              <li>Initial release: build/run/clean, environment detection, build-explorer tool window.</li>
              <li>Project &amp; module wizards and Alloy generators (controller/view/style/model/migration/widget).</li>
              <li>SDK &amp; update management, Help tool window.</li>
              <li>Alloy authoring: TSS highlighting + completion, related-file navigation, handler intention, live templates.</li>
              <li>Experimental debugger over the Chrome DevTools Protocol.</li>
            </ul>
            """.trimIndent()
        }
    }

    // Verify the optional split actually holds: run against a Community IDE
    // (core only, JS extensions inert) and a JS-bearing IDE.
    pluginVerification {
        ides {
            recommended()
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

kotlin {
    jvmToolchain(providers.gradleProperty("javaVersion").get().toInt())
}
