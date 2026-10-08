# Always Select Opened File – 1.2.0

Completely rewritten using **only public IntelliJ Platform APIs**.

## What changed

- Removed all internal API (`ProjectViewState`)
- Removed scheduled-for-removal `ProjectManagerListener.projectOpened`
- Uses only:
  - `ProjectActivity` (public, recommended)
  - `ActionManager` + `AnAction` (public)
  - `PropertiesComponent` (public)
- Works on every IntelliJ-based IDE
- Compatible with 2022.1 and newer

## Behaviour

- On first open of a project the option is turned **ON**
- A flag is stored so the plugin never forces it again
- If the user later turns the option off, it stays off

## How to build

### Option A – IntelliJ IDEA (easiest)

1. Open IntelliJ IDEA
2. File → New → Project from Existing Sources
3. Select this folder
4. Choose “Import project from external model” → Gradle (or just create a simple Plugin project and copy the sources)
5. Or use the built-in “Plugin” project wizard and replace the generated files with the ones in this package.

### Option B – Command line (Gradle)

Create a standard IntelliJ Plugin Gradle project and put:

- `src/main/java/...` → your source
- `src/main/resources/META-INF/plugin.xml` → the provided descriptor

Then run:

```bash
./gradlew buildPlugin
```

The resulting zip will be in `build/distributions/`.

## Important note about the Action approach

Because there is **no public setter** for “Always Select Opened File”, the plugin invokes the official toggle action via `ActionManager`.

This is the only fully compliant public way.  
On some IDE builds the exact action ID may differ; the code tries several known IDs.

If the action is not found the plugin logs a warning but does not crash.
