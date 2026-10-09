<!-- Keep a Changelog guide → https://keepachangelog.com -->

# Always-select-opened-file Changelog

## [1.2.0]

### Changed

* Completely rewritten using only public IntelliJ Platform APIs.
* Removed all internal API usage (`ProjectViewState`).
* Removed scheduled-for-removal `ProjectManagerListener.projectOpened`.
* Compatible with all IntelliJ-based IDEs.

### Fixed

* Restored the missing `EnableAlwaysSelectOpenedFileActivity` implementation. The extension was
  registered in `plugin.xml` but the class was never added, so the plugin failed to start with
  `ClassNotFoundException`.

## [1.0.0]

### Added

* Initial release of the Always-select-opened-file plugin.
* Automatically selects the currently opened file in the project tree.
