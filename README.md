# Titanium for JetBrains IDEs

A JetBrains plugin for developing [Titanium SDK](https://titaniumsdk.com) apps and modules — a port
of the [vscode-titanium](https://github.com/tidev/vscode-titanium) extension. It wraps the
[`titanium`](https://github.com/tidev/titanium-cli) (`ti`) and `alloy` CLIs.

The **core runs on every IntelliJ-Platform IDE**, including Community editions (IDEA Community,
PyCharm Community) and Android Studio. JavaScript-controller intelligence lights up automatically on
IDEs that ship the bundled JavaScript plugin (WebStorm, IDEA Ultimate, PhpStorm, PyCharm Pro, …).

## Feature status (all phases)

| Area | Status | Files |
|---|---|---|
| Gradle / Kotlin project (IntelliJ Platform Gradle Plugin 2.x) | ✅ | `build.gradle.kts`, `gradle.properties` |
| Universal/optional plugin descriptors | ✅ | `plugin.xml`, `titanium-javascript.xml` |
| Titanium CLI wrapper (non-interactive, JSON) | ✅ | `cli/TiCli.kt`, `cli/AlloyCli.kt` |
| `ti info --output json` parser → environment model | ✅ | `cli/model/*` |
| Environment service (cache + background refresh) | ✅ | `environment/TiEnvironmentService.kt` |
| Project discovery (tiapp.xml / timodule.xml) | ✅ | `project/*` |
| Build/run **run configuration** + `tiapp.xml` producer | ✅ | `run/*` |
| Build-explorer + Help **tool windows** | ✅ | `toolwindow/*` |
| `ti clean` action | ✅ | `actions/TiCleanAction.kt` |
| Settings + persistence | ✅ | `settings/*` |
| **Create app / module wizards** (`ti create`) | ✅ | `actions/create/*` |
| **Alloy generators** (controller/view/style/model/migration/widget) | ✅ | `actions/alloy/*` |
| **SDK & update management** (install/select/check) | ✅ | `sdk/*`, `actions/sdk/*` |
| `.tss` language: lexer, highlighting, flat parser, completion | ✅ | `tss/*` |
| Alloy XML view completion (**universal**) | ✅ | `alloy/AlloyViewCompletionContributor.kt` |
| **Related-file gutter markers + Open-Related actions** | ✅ | `alloy/AlloyRelated*`, `actions/alloy/OpenRelatedFileActions.kt` |
| **Insert-handler intention** | ✅ | `intentions/AlloyInsertHandlerIntention.kt` |
| **Live templates** (`ti*`, `al*`) | ✅ | `resources/liveTemplates/*` |
| JS controller completion + markers (**optional**) | ✅ | `alloy/js/*`, `titanium-javascript.xml` |
| **Debugger** over Chrome DevTools Protocol (experimental) | ✅ | `debug/*` |
| **CI + release pipeline**, LICENSE, CHANGELOG | ✅ | `.github/workflows/*`, `LICENSE`, `CHANGELOG.md` |

All roadmap phases (0–7) are implemented. The debugger is an initial implementation (see Notes).

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

## Publishing to the JetBrains Marketplace

1. Create the plugin's signing key (`openssl`) and a Marketplace token (Account → My Tokens).
2. Add repo secrets: `PUBLISH_TOKEN`, `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`.
3. Tag a release: `git tag v0.1.0 && git push --tags`. The **Release** workflow verifies, signs and
   publishes; the first version must also be approved once in the Marketplace UI.

Locally: `./gradlew signPlugin` and `./gradlew publishPlugin` (with the same env vars set).

## Notes / known follow-ups

- JSON parsing uses the platform-bundled Gson (`com.google.gson`).
- **Completion** is SDK-backed: `TiApiMetadata` reads the installed SDK's `api.jsca` for real
  element/property names, falling back to curated lists when it's missing.
- **Debugger** builds with `--debug-host`, connects over the Chrome DevTools Protocol, and bridges
  breakpoints / stepping / pause **and local-variable inspection** (`Runtime.getProperties`).
  Expression evaluation and source-map fidelity are still pending and need validation against a
  running simulator/device (iOS also needs `ios-webkit-debug-proxy`).
- **TSS** has lexer highlighting, a flat parser, completion, brace matching and commenting. A
  structured grammar (selectors/blocks) and TSS↔view references remain a future enhancement.
- `ti info` JSON shape varies across CLI/SDK versions — `TiInfoParser` is defensive and covered by a
  unit test (`src/test`); validate against a real machine and tighten as needed.
- Toolbar platform/device selection persists per-workspace.

### Editor intelligence
- Completion for Alloy views, **tiapp.xml**, TSS, and JS controllers.
- **Hover / quick-doc** from the SDK's `api.jsca` descriptions.
- **Go-to-definition**: event handler → controller function, i18n key → `strings.xml`.
- Code intentions: insert event handler, **insert i18n string**, **extract style to TSS**.
- Related-file gutter markers + Open-Related actions.
- Environment issues from `ti info` are surfaced in the Titanium tool window.

- **Recent builds** are remembered per-workspace and re-runnable from the tool window.
- **Open All Related Files** (Ctrl+Alt+A); clickable file paths in build/package consoles.
- **Update Titanium CLI / Alloy** actions (`npm i -g …`) in the SDK menu.

### Still open (deliberately)
- Symbol-level view↔TSS resolution (needs a full TSS grammar/PSI).
- Deeper debugger features (expression evaluation, watches, source maps).
- Broader test coverage (only the parser is unit-tested so far).
- Validation of the debugger, `api.jsca` completion, and `ti info` parsing against a real install.
