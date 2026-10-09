# Always Select Opened File

An IntelliJ Platform plugin that turns on the Project View option
**Always Select Opened File** (formerly *Autoscroll from Source*) for every project you open, so
editing a file always highlights and reveals it in the project tree.

## What it does

When a project finishes opening, the plugin enables the built-in Project View option
`ProjectView.AutoscrollFromSource`.

- Only ever turns the option **on** — it never toggles an already-enabled option back off.
- If the option is unavailable (an IDE without a Project View), the plugin quietly does nothing.
- Works on every IntelliJ-based IDE.

If you disable the option from the Project View menu, it will be re-enabled the next time the
project is opened — that is the plugin's purpose.

## Compatibility

| | |
|---|---|
| Supported since | **2023.1** (build `231`) |
| Upper bound | none — newer IDEs are supported |
| Dependencies | `com.intellij.modules.platform` only |
| API usage | **public API only**, no `@ApiStatus.Internal` |

The `231` floor is set by `ProjectActivity` and `ActionUiKind`, neither of which exists in 2022.3 —
confirmed by compiling against the 2022.3 platform and getting unresolved-reference errors.

The option itself lives in `com.intellij.ide.projectView.impl`, which is internal API. The plugin
never references it. Instead it drives the option through the action the platform registers for it
(`ProjectView.AutoscrollFromSource`), using public classes from
`com.intellij.openapi.actionSystem`.

## How it works

`src/main/kotlin/com/github/alwaysselectopenedfile/EnableAlwaysSelectOpenedFileActivity.kt` is
registered as a `postStartupActivity` in
[`plugin.xml`](src/main/resources/META-INF/plugin.xml) and implements the public
`com.intellij.openapi.startup.ProjectActivity` interface:

1. `execute` is a suspending function, so it is handed off to the EDT — the Project View is Swing UI.
2. The action is looked up by ID through the public `ActionManager`.
3. `ToggleOptionAction.isSelected` / `setSelected` read and persist the option. `setSelected` is the
   supported entry point: it writes both the per-project state and the shared default, exactly as
   clicking the menu item does.

The `DataContext` passed to the action event carries the project, because `ToggleOptionAction`
resolves its option from `AnActionEvent.getProject()`.

## Building

```bash
./gradlew buildPlugin      # produces build/distributions/always-select-opened-file-<version>.zip
./gradlew runIde           # launches a sandbox IDE with the plugin installed
./gradlew verifyPlugin     # JetBrains Plugin Verifier — checks API compatibility and internal API usage
```

The artifact is a zip to upload to the
[JetBrains Marketplace](https://plugins.jetbrains.com/plugin/upload).

### Target platform

The IDE to compile against is set by the `platformPath` property in `gradle.properties`:

```properties
platformPath = /Applications/WebStorm.app
```

Override it for a one-off build:

```bash
./gradlew buildPlugin -PplatformPath=/Applications/IntelliJ\ IDEA\ CE.app
```

`patchPluginXml` would otherwise overwrite `since-build` with the build number of whatever IDE you
compiled against, pinning the plugin to that single platform version. The supported range is
therefore stated explicitly in the `intellijPlatform.pluginConfiguration.ideaVersion` block in
`build.gradle.kts`.

To check API compatibility against specific IDEs — including internal-API violations — configure
`pluginVerification` and run `./gradlew verifyPlugin`.

> **Note**
> `verifyPluginProjectConfiguration` reports two advisories when compiling against a recent IDE:
> that `since-build` is below the target platform version, and that the Java level is below what
> the target platform requires. Both come from deliberately supporting 2023.1 while compiling
> against a newer IDE. Java 17 bytecode loads on old and new IDEs alike, so the warnings are not
> acted on. Compiling against a 2023.1 IDE (`-PplatformPath=...`) silences them and additionally
> guarantees that no newer-only API is used by accident.

## Publishing

### First upload — manual, unavoidable

The first version of a plugin **must** be uploaded by hand; JetBrains does not allow it to be
automated:

1. Create a [JetBrains Account](https://account.jetbrains.com).
2. Go to [JetBrains Marketplace](https://plugins.jetbrains.com/author/me) → *Add new plugin*.
3. Upload `build/distributions/always-select-opened-file-<version>.zip`.

Before uploading, confirm it works in a real IDE — either `./gradlew runIde`, or
*Settings → Plugins → ⚙ → Install Plugin from Disk…* in your own IDE.

### Later versions — automated

Once a version exists on the Marketplace, uploads can be automated:

1. Generate a token at [My Tokens](https://plugins.jetbrains.com/author/me/tokens). It is shown
   only once.
2. Supply it as an environment variable:

   ```bash
   export ORG_GRADLE_PROJECT_intellijPlatformPublishingToken='<token>'
   ```

   Put that line in your shell profile (`~/.zshrc`) so it persists.

   The token is deliberately **not** declared in this repo's `gradle.properties`. That file is
   committed to git, and a property defined there outranks `~/.gradle/gradle.properties`, so an
   empty placeholder would silently override a token you stored in your user home file. Leaving
   it undefined lets the environment variable win.

   If you prefer not to use an environment variable, store it outside the repository instead:

   ```properties
   # ~/.gradle/gradle.properties
   intellijPlatformPublishingToken=<token>
   ```
3. Bump `version` in `gradle.properties` and add a matching `## [x.y.z]` section to
   `CHANGELOG.md`. The changelog plugin feeds the Marketplace change notes from it and the build
   fails if the section is missing; the Marketplace also rejects a second artifact with a version
   number it has already seen.
4. Publish:

   ```bash
   ./gradlew publishPlugin
   ```

   This signs the ZIP (`signPlugin`), uploads it, and JetBrains then verifies compatibility before
   users are notified.

To publish to a pre-release channel rather than the default repository:

```kotlin
intellijPlatform { publishing { channels = listOf("beta") } }
```

## Project layout

```
.
├── .run/                            Run/Debug configurations (IDE, tests, verification)
├── gradle/libs.versions.toml        Version catalog
├── src/main/kotlin/com/github/alwaysselectopenedfile/
│   └── EnableAlwaysSelectOpenedFileActivity.kt
├── src/main/resources/META-INF/
│   ├── plugin.xml                   Plugin manifest
│   └── pluginIcon.svg
├── CHANGELOG.md                     Changelog (source for the Marketplace change notes)
├── build.gradle.kts                 Gradle build configuration
└── gradle.properties                Version and group
```

## Useful links

- [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij)
- [Internal API Migration](https://plugins.jetbrains.com/docs/intellij/api-internal.html)
- [Plugin configuration file reference](https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html)
- [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)
- [JetBrains Marketplace Quality Guidelines](https://plugins.jetbrains.com/docs/marketplace/quality-guidelines.html)