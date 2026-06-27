# Changelog

All notable changes to the Titanium plugin for JetBrains IDEs are documented here.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- **Build / run / clean** Titanium apps via a dedicated run configuration with platform, target,
  device/simulator, SDK, deploy-type, build-only and LiveView options; targets populate from the
  detected environment.
- **Build-explorer tool window** — tree of project → platforms → targets → devices → SDKs with
  double-click run and a refresh action.
- **Environment detection** by shelling `ti info --output json` (SDKs, simulators, emulators,
  devices, certificates, provisioning profiles), cached and refreshable.
- **Project & module wizards** (`ti create`) and **Alloy generators** (controller, view, style,
  model, migration, widget) wired to the `alloy` CLI, in the Tools menu and project-view context menu.
- **SDK & update management** — install / select SDKs and check for newer releases.
- **Help & Feedback tool window** with documentation links and an update check.
- **Alloy authoring** — `.tss` file type with lexer-based syntax highlighting and basic completion,
  Alloy XML-view completion, related-file gutter navigation and "Open Related" actions
  (controller/view/style) with keymaps, an "insert event handler" intention, and Live Templates
  (`ti*`, `al*`).
- **JavaScript-controller completion** (optional) that activates on IDEs bundling the JavaScript
  plugin.
- **Debugger (experimental)** — breakpoints, stepping and pause/resume bridged to the Titanium
  runtime over the Chrome DevTools Protocol.

### Notes
- The core runs on **every** IntelliJ-Platform IDE, including Community editions and Android Studio.
  JavaScript-controller intelligence lights up automatically where the bundled JavaScript plugin is
  present (WebStorm, IDEA Ultimate, PhpStorm, PyCharm Pro, …).
