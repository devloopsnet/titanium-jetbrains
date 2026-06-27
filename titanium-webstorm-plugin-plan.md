# Titanium SDK plugin for JetBrains IDEs — Full-Parity Implementation Plan

A roadmap to port the [vscode-titanium](https://github.com/tidev/vscode-titanium) extension to a JetBrains
plugin that runs on **every IntelliJ-Platform IDE** (IDEA Community + Ultimate, PyCharm, WebStorm, PhpStorm,
GoLand, CLion, RubyMine, RustRover, DataGrip, Android Studio, …), written in **Kotlin** on **Gradle** using the
**IntelliJ Platform Gradle Plugin 2.x**.

The plugin wraps the [titanium-cli](https://github.com/tidev/titanium-cli) (`ti`) and the `alloy` CLI to build,
run, debug, scaffold, and provide Alloy code intelligence for Titanium SDK apps and native modules.

---

## 1. Executive summary

The VSCode extension is a thin shell over the tidev toolchain (`titanium-cli`, `alloy`, `titanium-sdk`, and the
shared `titanium-editor-commons` Node library). Almost all heavy lifting — environment detection, completion data
generation, SDK/update management — happens in `titanium-editor-commons`, which the extension reuses as a Node
dependency.

Porting is feasible and the IntelliJ Platform maps cleanly onto every major VSCode concept (commands →
actions, tree views → tool windows, build commands → run configurations, settings → Configurables, completion
providers → CompletionContributors). The two hard problems are (1) reproducing `titanium-editor-commons`'
environment/completion logic on the JVM, and (2) the debug adapter, which in VSCode rides on
`vscode-chrome-debug-core`. Both have pragmatic strategies below.

**Recommended approach for the data/env layer:** rather than reimplement `titanium-editor-commons` in Kotlin, run
it (and the `ti`/`alloy` CLIs) as child processes and parse their JSON output. This keeps the plugin a thin shell —
exactly the architecture that has kept the VSCode extension maintainable — and avoids drifting from the upstream
toolchain.

### Run-everywhere strategy (the key design constraint)

The goal is to run on **every IntelliJ-Platform IDE, including Community editions**. The obstacle is the bundled
`JavaScript` plugin: it is closed-source and ships only in WebStorm and paid IDEs (IDEA Ultimate, PhpStorm, PyCharm
Pro, etc.) — never in Community. A hard dependency on it would lock the plugin to paid IDEs.

The solution is **graceful degradation via an optional plugin dependency**. The plugin splits into two parts:

- **Core (universal)** — hard-depends only on `com.intellij.modules.platform`, present in *all* IntelliJ IDEs. This
  carries the overwhelming majority of value: build/run/clean run configurations, `ti`-CLI environment detection,
  the build/help tool windows, project & module scaffolding, Alloy generators (they just shell `alloy`), settings,
  SDK/update management, the custom `.tss` file type, Live Templates, **and the Alloy XML-view completion/references**
  (XML support is universal too). All of this works on Community IDEs and Android Studio.
- **Optional JS layer** — loaded only when the `JavaScript` plugin is present, via
  `<depends optional="true" config-file="titanium-javascript.xml">JavaScript</depends>`. Holds only the genuinely
  JS-controller-aware features: completion inside Alloy JS controllers (`$.`, Ti/Alloy APIs) and references that
  resolve into JavaScript PSI. On Community IDEs these simply don't load; everything else still works.

So "any JetBrains IDE" is achievable, with the JS-controller intelligence light up automatically on the IDEs that
have JS support. (Note: this means the plugin runs on all *IntelliJ-Platform* IDEs; **Fleet** is a separate,
non-IntelliJ product and is out of scope.)

### Two decisions to confirm before coding

1. **Compile base.** Because the `JavaScript` plugin is **bundled-only** (not downloadable from the Marketplace),
   the JS-dependent code must be *compiled* against an IDE that bundles JS — IDEA **Ultimate** (or WebStorm). This is
   purely a compile-time choice: thanks to the optional `<depends>`, the shipped plugin still loads and runs on
   Community IDEs. Recommend compiling against IDEA Ultimate as the base and verifying against both a Community IDE
   and a JS-bearing IDE. (Details in §2/§6.)
2. **Debugger scope.** The VSCode debug adapter is the single largest and riskiest subsystem. Recommend treating it
   as a separate, later phase (Phase 6) and shipping a fully useful build/run/scaffold/intelligence plugin first.

---

## 2. Target & toolchain

| Concern | Decision |
|---|---|
| Target IDEs | **All IntelliJ-Platform IDEs** — IDEA Community + Ultimate, PyCharm, WebStorm, PhpStorm, GoLand, CLion, RubyMine, RustRover, DataGrip, Android Studio. (Fleet is out of scope — not IntelliJ Platform.) |
| Language | Kotlin (JVM 17) |
| Build | Gradle 9+, IntelliJ Platform Gradle Plugin `2.x` (`org.jetbrains.intellij.platform`) |
| Compile base | IntelliJ IDEA **Ultimate** (`intellijIdeaUltimate(version)` / type `IU`) — needed because it *bundles* `JavaScript`, which the Marketplace does not distribute. Compiling against Ultimate does **not** force Ultimate at runtime. |
| Min platform | IntelliJ Platform 2024.3 (build 243) as a starting floor; revisit per Marketplace stats. Test Android Studio's build line separately. |
| Hard deps (universal) | `<depends>com.intellij.modules.platform</depends>` (present in all IDEs incl. Community). XML support (`com.intellij.modules.xml`) is also universal. |
| Optional dep | `<depends optional="true" config-file="titanium-javascript.xml">JavaScript</depends>` — JS-controller features load only where the JS plugin exists. Compile-time only: `bundledPlugin("JavaScript")`. |
| External runtime | Node 20.18.1+, `ti` (titanium npm), `alloy` npm, an installed Titanium SDK |

**Why compile against Ultimate.** The `JavaScript` plugin is bundled-only; `plugin("JavaScript", …)` (the
Marketplace-download helper) does **not** work because there is no published artifact. To compile the JS-PSI code you
add `bundledPlugin("JavaScript")` against an IDE that bundles it (Ultimate or WebStorm). Runtime gating is governed
entirely by `plugin.xml`, so the optional `<depends>` keeps the shipped plugin loadable on Community.

**Key rules to bake into the template from day one:**

- The optional config file `META-INF/titanium-javascript.xml` is a *partial* descriptor (no `<id>`, no `<name>`,
  no nested `<depends>`) containing **only** the extensions that import `com.intellij.lang.javascript.*`.
- **No JS-PSI imports in core code paths.** Any class touching `com.intellij.lang.javascript.*` must be reachable
  *only* through extensions registered in `titanium-javascript.xml`; otherwise Community IDEs throw
  `NoClassDefFoundError`. Branch from core via runtime detection before calling into JS helpers.
- Runtime detection: `PluginManager.isPluginInstalled(PluginId.getId("JavaScript"))` (stable, public). Avoid
  reaching into the JS plugin's descriptor/classloader — `PluginManagerCore.getPlugin` is going `@Internal`
  (2026.2) and reflective access to other plugins is a Marketplace-ban risk.

---

## 3. Architecture overview

Mirror the VSCode subsystem layout so the two codebases stay conceptually aligned:

```
titanium-webstorm/
├── build.gradle.kts                # IntelliJ Platform Gradle Plugin 2.x
├── gradle.properties               # platformType=WS, platformVersion=...
├── src/main/kotlin/com/tidev/ti/
│   ├── core/                       # TiContainer (app service), state, context keys
│   ├── cli/                        # TiCli + AlloyCli process wrappers, JSON models
│   ├── environment/                # TiEnvironmentService (ti info --output json cache)
│   ├── project/                    # TiProject model (app vs module), tiapp.xml parsing
│   ├── run/                        # ConfigurationType, factories, SettingsEditor, CommandLineState
│   ├── actions/                    # build/run/clean/package/scaffold/alloy-generate actions
│   ├── toolwindow/                 # Build explorer + Help/Updates tool windows, tree nodes
│   ├── settings/                   # Configurable + PersistentStateComponent
│   ├── alloy/                      # XML-view completion + references, related-file nav (UNIVERSAL)
│   ├── alloy/js/                   # JS-controller completion + references (OPTIONAL: JS PSI — quarantined here)
│   ├── tss/                        # .tss LanguageFileType + (optional) parser/highlighter
│   ├── updates/                    # SDK/CLI/Alloy update checking
│   └── debug/                      # (Phase 6) debugger integration
└── src/main/resources/META-INF/
    ├── plugin.xml                  # core: depends only on com.intellij.modules.platform + optional JS
    └── titanium-javascript.xml     # partial descriptor: ONLY the JS-PSI extensions
```

**Universal vs optional split.** Everything except `alloy/js/` lives in the core, universal `plugin.xml` and runs on
every IDE. The `alloy/js/` package is the *only* code allowed to import `com.intellij.lang.javascript.*`, and it is
wired up exclusively by `titanium-javascript.xml`. Core code that wants to call a JS helper first checks
`PluginManager.isPluginInstalled(PluginId.getId("JavaScript"))`.

**Central service — `TiContainer` (application-level service)** replaces the VSCode singleton `ExtensionContainer`.
Holds the cached environment model, project list, running-task handle, and CLI-generation flag. Project-scoped state
(current project, selected target) lives in a **project-level service** to respect dynamic-plugin rules (no static
mutable state; everything `Disposable`-scoped).

**Process-shell principle.** Every operation that VSCode delegates to `titanium-editor-commons`/CLI is run here as a
child process with `--no-prompt --no-banner --no-colors --project-dir <path>` and, where available, `--output json`.
Parse JSON for state; stream stdout/stderr for builds. Detect the CLI generation once via `ti -v` and branch flag
construction (classic v5 vs rewritten v8).

---

## 4. Feature-by-feature mapping (VSCode → IntelliJ Platform)

| VSCode concept | VSCode specifics | IntelliJ equivalent |
|---|---|---|
| Commands (`titanium.*`) | `contributes.commands`, palette + keybindings | `AnAction` subclasses, registered in `<actions>`, `<keyboard-shortcut>` |
| Activity-bar container + tree views | `viewsContainers.activitybar` `titanium`; views `buildExplorer`, `helpExplorer`, welcome | `com.intellij.toolWindow` EP + `ToolWindowFactory`; `Tree`/`ColoredTreeCellRenderer` |
| Build/run/package | `titanium.build.run`, `package.run`, target via tree or QuickPick | **Run configurations** (`ConfigurationType` + `SettingsEditor` + `CommandLineState`) — the idiomatic home |
| Device/target selection | tree hierarchy platform→target→OS→device, or QuickPicks | Combo boxes in the run-config `SettingsEditor`, populated from `TiEnvironmentService`; mirrored in the build tool window |
| Stop build | `titanium.build.stop` | `KillableProcessHandler.destroyProcess()`; Stop button wired to the run console |
| LiveView toggle | `build.liveview` setting + title-bar toggle | `ToggleAction` writing to settings; `--liveview` flag |
| Clean | `titanium.clean` | Action → `ti clean [-p] -d <dir>` |
| Scaffolding (app/module) | `create.application`, `create.module` (wizard via `vscode-wizard`) | Multi-step UI (`DialogWrapper` or `ModuleWizardStep`) → `ti create -t app|module` |
| Alloy generate (controller/model/style/view/widget/migration) | `alloy.generate.*` | Actions → `alloy generate <component> -o <app-dir> --no-colors` |
| Related-file navigation | `alloy.open.related*` + keybindings | Actions + a `RelatedItemLineMarkerProvider` (gutter) for view↔style↔controller |
| Completions — view (XML), style (TSS), tiapp (XML) | `CompletionItemProvider`s, data from `titanium-editor-commons` | `CompletionContributor` per language — **universal** (XML/TSS need no JS plugin); data fetched from CLI/commons at runtime |
| Completions — JS controller (`$.`, Ti/Alloy APIs) | `ControllerCompletionItemProvider` | `CompletionContributor` language `JavaScript` — **optional** (in `titanium-javascript.xml`, loads only where JS plugin present) |
| Go-to-definition — view↔style | `*DefinitionProvider` | `PsiReferenceContributor` (XML/TSS) — **universal** |
| Go-to-definition — into JS controllers | `ControllerDefinitionProvider` | `PsiReferenceContributor` resolving into JS PSI — **optional** (JS layer) |
| Hover | `ViewHoverProvider` | `DocumentationProvider` / `LineMarker` tooltips |
| Code actions (insert handler, insert i18n, extract style) | `ViewCodeActionProvider` | `IntentionAction` / `LocalQuickFix` + `Annotator` |
| Terminal link provider | clickable build-output paths | `Filter` (`RegexpFilter`/`UrlFilter`) on the run console |
| Snippets (`titanium.json`, `alloy.json`) | `contributes.snippets` | Live Templates (`<liveTemplate>` / bundled `.xml` template set) |
| TSS language | `alloy-tss`, scope `source.css.tss`, TextMate grammar | `com.intellij.fileType` EP + `LanguageFileType`; lean on CSS PSI where possible |
| Settings (`contributes.configuration`) | 14 keys under `titanium.*` | `applicationConfigurable`/`projectConfigurable` + `PersistentStateComponent` |
| Task providers (`titanium-build`, `titanium-package`) | scriptable builds with problem matchers | Run configurations + `BeforeRunTask`; problem matching via output `Filter`s |
| Debug adapter (`type: titanium`) | DAP + `vscode-chrome-debug-core` | XDebugger API (`XDebugProcess`) bridging Chrome DevTools Protocol — **Phase 6** |
| Context-key `when` clauses | `titanium:enabled`, `:build:running`, etc. | `AnAction.update()` enable/visibility logic + service state |
| Update management | `updates.*` via `titanium-editor-commons` | `updates/` service shelling `ti sdk list --releases -o json`, etc.; Help tool window |

---

## 5. CLI surface the plugin must wrap

Detect generation first (`ti -v`); build flags for the **classic** CLI for full parity, fall back gracefully on v8.

**Build / run** — `ti build -p <ios|android> -T <target> -C <device-id> -s <sdk> -d <dir> [--deploy-type] [--build-only] [--liveview]`
plus iOS signing (`-V` developer cert, `-R` distribution cert, `-P` provisioning UUID, `-O` output dir,
`--device-family`) and Android signing (`-K` keystore, `-P` store-pass, `--key-password`, `-L` alias, `-O`). Targets:
iOS `simulator|device|dist-appstore|dist-adhoc`; Android `emulator|device|dist-playstore`.

**Environment / enumeration** — `ti info --output json [--types ios|android|titanium]` is the single source of
truth for SDKs, iOS simulators/devices, certs, provisioning profiles, Android AVDs/emulators, and connected devices.
Run once at startup + on manual refresh; cache in `TiEnvironmentService`. Everything that needs a dropdown reads
from this model.

**Scaffolding** — `ti create -t app|module --id <id> -n <name> -p <platforms> -d <workspace-dir> -u <url>`.

**SDK management** — `ti sdk list [--releases|--branches] -o json`, `ti sdk install [<ver>|latest] [--default]`,
`ti sdk select <ver>`, `ti sdk uninstall <ver>`.

**Project / config** — `ti project [<key> [<value>]] -o json` (read/write tiapp.xml), `ti config <key> [<value>]`
(read/write `~/.titanium/config.json`; prefer this over interactive `ti setup`), `ti clean [-p] -d <dir>`,
`ti module -o json`.

**Alloy** — separate `alloy` binary: `alloy generate controller|view|style|model|migration|widget|jmk <name>
[--widgetname <w>] -o <app-dir> [--platform ios|android] [--force] --no-colors`; plus `alloy extract-i18n`,
`alloy copy|move|remove`, `alloy new`.

**Non-interactive contract:** always pass `--no-prompt --no-banner --no-colors --project-dir <path>`; supply every
required value so nothing blocks on a prompt; key success/failure off exit code; stream log-level-prefixed lines for
the build console.

> Note on auth: `ti login`/`ti logout` are legacy (Appcelerator backend is gone, removed in v8). Do **not** build
> around them.

---

## 6. Key IntelliJ Platform building blocks (with class/EP names)

- **Run configuration:** `ConfigurationTypeBase`/`ConfigurationFactory`, `RunConfigurationBase<Options>` with a
  `RunConfigurationOptions` subclass using `StoredProperty` delegates for persistence; `SettingsEditor<T>` for the
  platform/target/device/SDK/deploy-type UI; `CommandLineState.startProcess()` returning an `OSProcessHandler`
  built from `GeneralCommandLine("ti", "build", …)`. Reuse `DefaultRunExecutor`; add a
  `LazyRunConfigurationProducer` to create a config from a `tiapp.xml` context.
- **Process execution + console:** `GeneralCommandLine` → `ProcessHandlerFactory.createColoredProcessHandler` →
  `KillableProcessHandler`; `ProcessTerminatedListener.attach`; `ConsoleView` via
  `TextConsoleBuilderFactory`; clickable paths via `Filter`/`RegexpFilter`/`UrlFilter`.
- **Tool windows:** `com.intellij.toolWindow` EP + `ToolWindowFactory.createToolWindowContent`;
  `SimpleToolWindowPanel` hosting a `com.intellij.ui.treeStructure.Tree` with `ColoredTreeCellRenderer`,
  `TreeSpeedSearch`, and `ToolbarDecorator`. For async/lazy nodes:
  `StructureTreeModel` → `AsyncTreeModel`.
- **Actions:** `AnAction` (field-less!), mandatory `getActionUpdateThread()` (`BGT` for PSI/VFS, `EDT` for Swing),
  `DumbAwareAction` where needed; `ToggleAction` for LiveView.
- **Settings:** `applicationConfigurable` (CLI path, machine-specific, `RoamingType.PER_OS`) and
  `projectConfigurable` (per-project default SDK, i18n language, output dir); modern
  `BoundSearchableConfigurable` + Kotlin UI DSL `panel { }`; state in a
  `PersistentStateComponent` service annotated with `@State`/`@Storage`.
- **Code intelligence:** `CompletionContributor` (EP `com.intellij.completion.contributor`, per language) with
  `PlatformPatterns`/`XmlPatterns` + `LookupElementBuilder`; `PsiReferenceContributor`
  (EP `com.intellij.psi.referenceContributor`) with `PsiReferenceBase`/`PsiPolyVariantReferenceBase`;
  `.tss` via `com.intellij.fileType` EP + `LanguageFileType` (FileTypeFactory is deprecated). XML/TSS contributors go
  in the core `plugin.xml`; `language="JavaScript"` contributors go in the optional `titanium-javascript.xml`.
- **Optional JS dependency (run-everywhere):** main `plugin.xml` declares
  `<depends optional="true" config-file="titanium-javascript.xml">JavaScript</depends>`; the partial descriptor
  `META-INF/titanium-javascript.xml` carries only the JS-PSI extensions. Compile with `bundledPlugin("JavaScript")`
  against an Ultimate/WebStorm base; runtime-branch core code with
  `PluginManager.isPluginInstalled(PluginId.getId("JavaScript"))`.
- **Notifications/progress:** `Notification(...).notify(project)` with a `notificationGroup` EP; `Task.Backgroundable`
  / `ProgressIndicator.checkCanceled()` (never swallow `ProcessCanceledException`); 2024.1+ prefers coroutines.
- **Packaging:** `buildPlugin`, `verifyPlugin`, `signPlugin`, `publishPlugin`; config in the `intellijPlatform { }`
  block (`pluginConfiguration.ideaVersion.sinceBuild/untilBuild`, `signing`, `publishing`, `pluginVerification`).

---

## 7. Phased roadmap

### Phase 0 — Project scaffold & environment plumbing (foundation)
- Gradle project from the **IntelliJ Platform Plugin Template**; compile base
  `intellijPlatform { intellijIdeaUltimate(...) }` + `bundledPlugin("JavaScript")` (compile-time only); core
  `plugin.xml` hard-depends on `com.intellij.modules.platform` and declares the **optional** JS dependency with an
  (initially empty) `titanium-javascript.xml`. Verify a no-op build loads in both an IDEA Community IDE and a
  JS-bearing IDE from the start, so the universal/optional split is proven before features pile up.
- `TiContainer` app service; `TiCli`/`AlloyCli` process wrappers (build command line, capture output, parse JSON);
  CLI-generation detection (`ti -v`).
- `TiEnvironmentService`: run `ti info --output json`, parse into Kotlin data classes (SDKs, iOS sims/devices/certs/
  profiles, Android AVDs/devices), cache + refresh action.
- `TiProject` model: locate `tiapp.xml` projects in the workspace, distinguish app vs module, read name/id/version
  via `ti project -o json`.
- **Milestone:** plugin loads in WebStorm, detects environment, lists projects. *Verification:* `runIde` against a
  sample app/module; assert env model populated; `verifyPlugin` passes.

### Phase 1 — Build / run / clean via run configurations (core value)
- `TiRunConfigurationType` + factory + options; `SettingsEditor` with platform/target/device/SDK/deploy-type/
  build-only/LiveView controls populated from `TiEnvironmentService`.
- `CommandLineState` assembling `ti build` with the non-interactive flag contract; streamed console with output
  `Filter`s for clickable paths; `KillableProcessHandler` for Stop.
- `LazyRunConfigurationProducer` to create a config from `tiapp.xml`.
- Standalone actions (Build / Run / Clean) for palette + keymap parity with VSCode keybindings.
- **Milestone:** build & launch an app on an iOS simulator and an Android emulator from the run-config UI.
  *Verification:* end-to-end run on both platforms; exit-code → success/fail reflected in the run UI.

### Phase 2 — Build explorer & Help tool windows (discoverability)
- `Titanium` tool window (left anchor): tree of platform → target → OS version → device with inline Build/Package
  actions, recent builds, refresh; error node surfacing the output console. Mirror selection into the run config.
- Help/Updates tool window: help links + available-update nodes.
- Welcome/empty states (no project / tooling missing / SDK not configured) via `EditorNotificationProvider` banners
  and tool-window placeholders.
- **Milestone:** full tree-driven build like the VSCode activity bar. *Verification:* node actions trigger correct
  CLI invocations; refresh re-reads `ti info`.

### Phase 3 — Scaffolding & Alloy generators (productivity)
- Create App / Create Module wizards (`DialogWrapper`) → `ti create`; respect `defaultCreationDirectory` setting.
- Alloy generate actions (controller/model/style/view/widget/migration) → `alloy generate … -o <app-dir>`; surface
  in the project-view context menu and a Titanium action group.
- Android keystore create/import action.
- **Milestone:** scaffold a project and generate Alloy components without leaving the IDE. *Verification:* generated
  files appear and compile via a subsequent build.

### Phase 4 — Settings & SDK/update management (lifecycle)
- `applicationConfigurable` (CLI path, log level, terminal-vs-console, update frequency) + `projectConfigurable`
  (default SDK, i18n language, dist output dir, code templates); `PersistentStateComponent` services; map writes to
  `ti config` where they correspond to CLI config.
- Update service: `ti sdk list --releases -o json`, check/install SDK/CLI/Alloy updates with `Task.Backgroundable`
  progress and notifications.
- **Milestone:** settings round-trip and survive restart; SDK install/select works. *Verification:* change a setting,
  restart `runIde`, confirm persistence; install an SDK.

### Phase 5a — Universal Alloy code intelligence (works on every IDE)
- `.tss` file type (`LanguageFileType`), leaning on CSS PSI; syntax highlighting.
- Completion contributors: view (XML tags/props/ids/classes/i18n/image paths), TSS, tiapp.xml. Completion data
  generated at runtime from the installed SDK/Alloy (shell the same data source `titanium-editor-commons` uses, or
  invoke it directly via Node).
- XML/TSS references + go-to-definition + gutter related-file markers (view↔style); related-file nav actions + keymaps.
- Code intentions: insert missing i18n string; extract-style refactor; insert event handler into controller
  (done as plain document text insertion so it needs no JS PSI).
- Live Templates ported from `titanium.json`/`alloy.json` snippets.
- All of the above ships in the core `plugin.xml` and runs on Community IDEs and Android Studio.
- **Milestone:** Alloy XML/TSS authoring feels native on any IDE. *Verification:* exercised on a sample Alloy
  project in a Community IDE.

### Phase 5b — JS-controller intelligence (optional layer, JS-bearing IDEs only)
- `language="JavaScript"` completion contributor: controller `$.`, Ti/Alloy APIs, i18n keys.
- References / go-to-definition resolving from views into JavaScript controller PSI, and controller→view.
- All of this lives in `titanium-javascript.xml` and the quarantined `alloy/js/` package; it loads automatically only
  where the `JavaScript` plugin is present and is simply absent on Community IDEs (no error, core unaffected).
- **Milestone:** full JS-controller intelligence on WebStorm/IDEA Ultimate. *Verification:* run the Plugin Verifier
  against both a Community IDE (extensions inert, no errors) and a JS-bearing IDE (extensions resolve); manually
  confirm completion/navigation in the JS layer.

### Phase 6 — Debugger (optional, highest risk)
- Implement `XDebugProcess` bridging the Chrome DevTools Protocol that Titanium exposes via `--debug-host`
  (Android) and `ios-webkit-debug-proxy` + Safari Web Inspector (iOS) — the same mechanism VSCode's
  `vscode-chrome-debug-core` adapter uses. Map Alloy source maps for breakpoints; provide a REPL via the
  XDebugger evaluate API.
- A `BeforeRunTask` that performs the debug build (`--debug-host`/`--profiler-host`) before attaching.
- **Milestone:** breakpoints hit in an Alloy controller on a simulator/emulator. *Verification:* set a breakpoint,
  confirm pause + variable inspection.

### Phase 7 — Packaging, verification & Marketplace
- `patchPluginXml` (`sinceBuild`/`untilBuild`), `verifyPlugin` against a **matrix**: at least one Community IDE
  (proves the optional split + core-only operation), one JS-bearing IDE (IDEA Ultimate/WebStorm, proves the JS
  layer resolves), and an Android Studio build line. `signPlugin`, `publishPlugin` (first release manual). CI via
  GitHub Actions (template ships it).
- **Milestone:** signed plugin published to the JetBrains Marketplace (or distributed as a ZIP).

---

## 8. Risks & mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| `JavaScript` plugin = paid IDEs only | JS-controller intelligence can't run on Community | Optional `<depends>` + quarantined `alloy/js/` module: core runs everywhere, JS features light up where available |
| JS-PSI class leaks into core code | `NoClassDefFoundError` on Community IDEs | Strict rule: only `alloy/js/` imports `com.intellij.lang.javascript.*`, wired solely by `titanium-javascript.xml`; runtime-branch via `isPluginInstalled` |
| `bundledPlugin` vs `plugin()` confusion | Build fails (JS not on Marketplace) | Compile against IDEA Ultimate/WebStorm with `bundledPlugin("JavaScript")`; never `plugin("JavaScript", …)` |
| Accidentally non-optional JS `<depends>` | Plugin silently locks to paid IDEs | Keep `optional="true"`; CI verifies load on a Community IDE |
| Android Studio build-line divergence | Compat breakage on AS | Treat AS as a separate "core-only" verification target; pin its `sinceBuild/untilBuild` |
| `titanium-editor-commons` reimplementation | Large, drift-prone | Shell the CLIs + (optionally) invoke commons via Node rather than rewriting in Kotlin |
| Debug adapter complexity | Could dominate schedule | Isolated as Phase 6; ship without it first |
| CLI generation differences (classic v5 vs v8) | Wrong flags / failures | Detect via `ti -v`; centralize flag construction in `TiCli`; integration-test both |
| `ti info` JSON shape changes across SDK/CLI versions | Brittle parsing | Defensive parsing, version-aware models, fail soft with actionable notifications |
| Dynamic-plugin rules (no static state) | Load/unload bugs, verifier failures | Services + `Disposable` scoping; run Plugin Verifier in CI (`NOT_DYNAMIC` gate) |
| Interactive CLI prompts blocking a child process | Hung builds | Always `--no-prompt` + supply all inputs; timeouts on enumeration calls |
| TSS full language support scope creep | Time sink | Start with file type + CSS-PSI reuse + highlighting; defer a full grammar |
| WebStorm `modules.platform` depends omission | Plugin won't load | Bake into the plugin.xml template; smoke-test in `runIde` early |

---

## 9. Effort sketch (rough, not a commitment)

Phases 0–2 (foundation + build/run + tool windows) deliver the core daily-driver value and are the bulk of the
"is this usable?" question — a focused effort of a few weeks for one developer familiar with the IntelliJ Platform.
Phases 3–5 (scaffolding, settings/SDK mgmt, Alloy intelligence) bring full non-debug parity. Phase 6 (debugger) is
comparable in effort to all the preceding build/run work combined and should be scoped/scheduled on its own. Phase 7
is small but gated on a JetBrains Marketplace account and signing setup.

A sensible first shippable milestone is **end of Phase 2**: a plugin that detects the environment and builds/runs/
cleans Titanium apps and modules from both a run configuration and a build tool window.

---

## 10. Open questions to resolve before Phase 0

1. **Distribution:** public JetBrains Marketplace listing, or internal/private distribution? (Affects signing,
   vendor metadata, review cadence.)
2. **Debugger:** in scope at all, or explicitly out for v1?
3. **Min platform version** to support across the IDE family (drives `sinceBuild` and the verification matrix);
   decide whether Android Studio is a first-class support target or best-effort.
4. **Reuse `titanium-editor-commons` via Node**, or reimplement env/completion logic natively? (Recommendation:
   reuse via process/Node for v1.)
5. **Repo/licensing** — mirror tidev's licensing and conventions if this is intended to live under or alongside the
   tidev org.
```