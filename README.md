# Titanium for JetBrains IDEs

A JetBrains plugin for developing [Titanium SDK](https://titaniumsdk.com) apps and modules — a port
of the [vscode-titanium](https://github.com/tidev/vscode-titanium) extension. It wraps the
[`titanium`](https://github.com/tidev/titanium-cli) (`ti`) and `alloy` CLIs.

The **core runs on every IntelliJ-Platform IDE**, including Community editions (IDEA Community,
PyCharm Community) and Android Studio. JavaScript-controller intelligence lights up automatically on
IDEs that ship the bundled JavaScript plugin (WebStorm, IDEA Ultimate, PhpStorm, PyCharm Pro, …).

## What's in this scaffold (Phase 0–2 + slices of 3–5)

| Area | Status | Files |
|---|---|---|
| Gradle / Kotlin project (IntelliJ Platform Gradle Plugin 2.x) | ✅ | `build.gradle.kts`, `gradle.properties` |
| Universal/optional plugin descriptors | ✅ | `plugin.xml`, `titanium-javascript.xml` |
| Titanium CLI wrapper (non-interactive, JSON) | ✅ | `cli/TiCli.kt`, `cli/AlloyCli.kt` |
| `ti info --output json` parser → environment model | ✅ | `cli/model/*` |
| Environment service (cache + background refresh) | ✅ | `environment/TiEnvironmentService.kt` |
| Project discovery (tiapp.xml / timodule.xml) | ✅ | `project/*` |
| Build/run **run configuration** (platform/target/device/SDK/…) | ✅ | `run/*` |
| Run-config producer from `tiapp.xml` context | ✅ | `run/TiRunConfigurationProducer.kt` |
| Build-explorer **tool window** (tree + double-click run) | ✅ | `toolwindow/*` |
| `ti clean` action | ✅ | `actions/TiCleanAction.kt` |
| Settings (CLI paths, log level, …) + persistence | ✅ | `settings/*` |
| `.tss` file type | ✅ | `tss/TssLanguage.kt` |
| Alloy XML view completion (**universal**) | ✅ | `alloy/AlloyViewCompletionContributor.kt` |
| JS controller completion (**optional**, JS-bearing IDEs) | ✅ | `alloy/js/AlloyControllerCompletionContributor.kt` |

Next (see `titanium-webstorm-plugin-plan.md`): scaffolding wizards & Alloy generators (Phase 3),
SDK/update management (Phase 4), TSS grammar + references/intentions (Phase 5), debugger (Phase 6).

## The run-everywhere trick

Titanium apps are JS/Alloy, so JS-aware features need the bundled `JavaScript` plugin — which ships
only in paid IDEs. To still run on Community editions, the plugin is split:

- **Core** (`plugin.xml`) hard-depends only on `com.intellij.modules.platform` (universal). This is
  almost everything: build/run, environment detection, tool window, settings, `.tss`, and the
  **XML** Alloy-view completion.
- **Optional JS layer** (`titanium-javascript.xml`) is loaded only when the JavaScript plugin is
  present, via `<depends optional="true" config-file="…">JavaScript</depends>`. It holds only the
  `language="JavaScript"` contributors. On Community IDEs it simply never loads.

Because the `JavaScript` plugin is **bundled-only** (not on the Marketplace), the project *compiles*
against IDEA Ultimate (`platformType=IU`, which bundles it) — a compile-time choice that does **not**
force Ultimate at runtime. Core code never imports `com.intellij.lang.javascript.*`; it branches via
`TiPlugins.isJavaScriptAvailable` (`PluginManager.isPluginInstalled`).

## Build & run

Prerequisites: JDK 17+, and a Titanium toolchain on PATH for runtime testing (`npm i -g titanium alloy`).

This scaffold ships without the Gradle wrapper JAR (binary). Generate it once, or just open the
folder in IntelliJ IDEA (it imports the Gradle project and creates the wrapper automatically):

```bash
gradle wrapper --gradle-version 8.13   # one-time, if you have a system Gradle
```

Then:

```bash
./gradlew runIde          # launch a sandbox IDE with the plugin (IDEA Ultimate by default)
./gradlew buildPlugin     # produce build/distributions/Titanium-0.1.0.zip
./gradlew verifyPlugin    # Plugin Verifier
```

To prove the optional split, run `verifyPlugin` / `runIde` against a Community IDE too — set
`platformType=IC` only for a verification run (note: the JS completion will be absent there, as
intended; do not compile the JS code against IC).

## Notes / known follow-ups

- JSON parsing uses the platform-bundled Gson (`com.google.gson`).
- The `.tss` file type currently opens as plain text; a lexer/parser + highlighter is a Phase-5 task.
- `ti info` JSON shape varies across CLI/SDK versions — `TiInfoParser` is intentionally defensive and
  best-effort; validate device/sim/cert parsing against a real machine and tighten as needed.
- This was authored without a compile pass against the IntelliJ SDK (no SDK/Gradle in the authoring
  environment). The code is written to compile against the 2024.3 platform; expect to resolve a small
  number of API drift issues on first `runIde`.
