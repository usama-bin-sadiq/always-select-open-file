<!-- Keep a Changelog guide → https://keepachangelog.com -->

# Always-select-opened-file Changelog

## Unreleased

## 1.2.2

### Fixed

- Removed the last internal API usage. The startup activity implemented `DataContext` directly to
  supply a project-carrying data context, but `DataContext.getData(String)` is marked
  `@ApiStatus.Internal` and `@Deprecated(forRemoval = true)`, which the Plugin Verifier reported as
  an internal API usage, a scheduled-for-removal usage and a non-extendable violation. The data
  context is now obtained from `DataManager.getInstance().getDataContext(...)` instead, and nothing
  is implemented by hand.

### Changed

- Minimum supported IDE raised from 2023.1 to 2024.3. The Plugin Verifier reported compatibility
  problems for every build below 243.

## 1.2.1

### Fixed

- Plugin now loads in the IDE. The `postStartupActivity` registered in `plugin.xml` pointed at
  `EnableAlwaysSelectOpenedFileActivity`, which was absent from the distributed artifact, so startup
  failed with `ClassNotFoundException` wrapped in `PluginException: Cannot create extension`.

## 1.2.0

### Changed

- Completely rewritten using only public IntelliJ Platform APIs.
- Removed all internal API usage (`ProjectViewState`).
- Removed scheduled-for-removal `ProjectManagerListener.projectOpened`.
- Compatible with all IntelliJ-based IDEs.

### Fixed

- Restored the missing `EnableAlwaysSelectOpenedFileActivity` implementation. The extension was
  registered in `plugin.xml` but the class was never added, so the plugin failed to start with
  `ClassNotFoundException`.

## 1.0.0

### Added

- Initial release of the Always-select-opened-file plugin.
- Automatically selects the currently opened file in the project tree.
