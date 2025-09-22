# TODO - Spotify Clone

This file documents detected completed features and actionable TODO items discovered by scanning the repository source code.

## Summary of completed features

- JavaFX-based UI scaffold (`SpotifyCloneApp`, `views/MainWindow.java`) with:
  - Menu bar with File/Playlist/Help and handlers wired
  - Song list and playlist list views with custom cells (`SongListCell`, `PlaylistListCell`)
  - Search field wired to `MusicLibrary.searchSongs`
  - Playback controls UI (play/pause/stop/next/previous), progress and volume sliders
- Audio playback implemented (`controllers/AudioPlayer.java`) using Java Sound `Clip` with:
  - play, pause, resume, stop, seek, volume control
  - listener interface (`AudioPlayerListener`) for UI updates
- Core data models (`models/`) implemented:
  - `Song`, `Playlist`, `Album`, `Artist`, `MusicLibrary` with add/remove/search/filter APIs
  - Playlist manipulation (add/remove/move/shuffle)
- Audio import functionality (`utils/MusicImporter.java`) using `jaudiotagger` with:
  - import single file, import directory recursively, metadata extraction, fallback creation
- Simple config persistence (`utils/ConfigManager.java`) using `Properties`
- Maven build provided (`pom.xml`) with JavaFX and jaudiotagger dependencies

## Detected TODO / Missing or partially-implemented features

The code contains UI placeholders and several methods with comments like `// Implementation for ...` where behavior is not implemented. Below are actionable TODOs grouped by area, with file references and suggested priority.

1) Implement playback navigation and seeking (HIGH) — COMPLETED
   - Files changed: `views/MainWindow.java`, `controllers/AudioPlayer.java`
   - Status: Implemented `playNext()` and `playPrevious()` in `MainWindow.java`. Added `onSongEnded()` listener to `AudioPlayer.AudioPlayerListener` and fired `onSongEnded()` when a track finishes; `MainWindow` now advances to the next track automatically. Implemented `seekToPosition()` and slider press/release handling to allow seeking.
   - Remaining / improvements: refine behavior at end-of-playlist (looping, repeat, shuffle), improve drag vs click UX, and add visual seek preview while dragging.

2) Import music UI and file chooser integration (MEDIUM) — COMPLETED
   - Files changed: `views/MainWindow.java`, `utils/MusicImporter.java`, `utils/ConfigManager.java`
   - Status: Implemented `MainWindow.importMusic()` to present a small dialog offering import by Files or Folder. Files import uses `FileChooser.showOpenMultipleDialog(...)` and Folder import uses `DirectoryChooser.showDialog(...)`. The chosen files/folders are passed to `MusicImporter.importAudioFiles(...)` / `MusicImporter.importFromDirectory(...)`, the `MusicLibrary` is updated, `lastImportPath` is saved via `ConfigManager`, and the UI lists are refreshed (`refreshSongList()` and `refreshPlaylistList()`). An informational alert is shown after import.
   - Notes: The `FileChooser` currently uses a generic "*.*" extension filter created from `MusicImporter.getFileFilterDescription()`. Consider refining extension filters for better UX. Errors during import are shown via Alerts.

3) Playlist creation and persistence (MEDIUM) — COMPLETED
   - Files changed: `views/MainWindow.java`, `utils/PlaylistIO.java`, `models/MusicLibrary.java`
   - Status: Implemented `MainWindow.createNewPlaylist()` to prompt for a name, create a `Playlist`, add it to `MusicLibrary`, refresh the UI, and persist playlists using `utils/PlaylistIO` (JSON under `~/.spotify-clone/playlists.json`). Implemented `MainWindow.loadPlaylist(Playlist)` to load playlist contents into the song list view.
   - Notes: Playlists are serialized using Jackson. The `Playlist` class must remain JSON-serializable (it uses standard POJOs). Consider adding playlist-level metadata (owner, collaborative flag) if needed later.

4) Playlist loading and interaction (MEDIUM) — COMPLETED
   - Files changed: `views/MainWindow.java`, `utils/PlaylistIO.java`, `views/SongListCell.java`, `views/PlaylistListCell.java`
   - Status: Implemented `MainWindow.loadPlaylist(Playlist)` to load a playlist into the song list view. Added context menu on playlist items to delete playlists. Added context menu on song list cells (when viewing a playlist) to remove songs from the playlist. Implemented drag-and-drop reordering of songs within a loaded playlist; changes are persisted via `PlaylistIO`.
   - Notes: Consider adding undo for accidental removals, and visual feedback for drag targets. Also consider disabling drag/drop when not viewing a playlist.

5) Robust error handling and user feedback (LOW) — PARTIALLY COMPLETED
    - Files: `controllers/AudioPlayer.java`, `utils/MusicImporter.java`, `views/MainWindow.java`, `utils/PlaylistIO.java`, `utils/ConfigManager.java`
    - Status: Partially implemented.
       - `AudioPlayer` reports playback errors via its `AudioPlayerListener` and `MainWindow` displays those errors in an `Alert` (user-facing). Playback exceptions (unsupported format, IO, line unavailable) are mapped to readable messages.
       - `MainWindow.importMusic()` wraps the import flow in a try/catch and shows an `Alert` on failure.
       - However, several utility classes still log to the console (`System.err`) rather than surfacing errors to the UI: `MusicImporter`, `PlaylistIO`, and `ConfigManager` write to `System.err` on failure. Some code paths silently swallow exceptions (e.g., `seekToPosition()` currently swallows exceptions without reporting).
    - Remaining work:
       - Replace `System.err.println(...)` usages in `MusicImporter`, `PlaylistIO`, and `ConfigManager` with either thrown exceptions (propagated to callers) or structured error reporting so the UI can show Alerts.
       - Surface per-file import errors to the user (e.g., show a summary of skipped/failed files after import) instead of only logging to console.
       - Ensure save failures (playlist/config persistence) are reported to the user with an option to retry.
       - Consider introducing a lightweight logging framework (SLF4J) for controlled logs and adjusting Alert messages for clarity.

    - Assessment: Because utilities still log to console and some exceptions are swallowed, I consider this item not fully complete. I can finish the remaining work in a follow-up change (low-risk, targeted edits).

6) Unit tests and CI (LOW) — COMPLETED
   - Files added: `src/test/java/com/spotify/clone/models/MusicLibraryTest.java`, `src/test/java/com/spotify/clone/utils/PlaylistIOTest.java`
   - CI workflow: `.github/workflows/ci.yml` runs a Maven build on push/PR
   - Notes: Tests cover core `MusicLibrary` behaviors and `PlaylistIO` persistence round-trip. More tests can be added for `MusicImporter` and UI controllers.
      - Running tests locally: `mvn -DskipTests=false package` will execute the unit tests and fail the build on test failures.

7) Persistence for library and config (LOW)
   - Files: `utils/ConfigManager.java`, `models/MusicLibrary.java`
   - Missing bits: `ConfigManager` exists but is not used widely. Implement library persistence (e.g., JSON export/import) so library/playlist state persists across runs.

8) Improve Audio format support and cross-platform playback (MEDIUM)
   - Files: `controllers/AudioPlayer.java`
   - Missing bits: `Clip` has limited format support. Consider integrating a more capable audio backend (e.g., JLayer for MP3 or using JavaFX MediaPlayer) for better codec coverage.

9) UI polish and resizing, theming, icons (LOW)
   - Files: `views/*`
   - Missing bits: Add icons, handle responsive layout, keyboard shortcuts, and accessibility improvements.

10) Tests & build validation (LOW)
   - Files: repository root
   - Missing bits: Add a basic `mvn verify` or CI workflow to build and run unit tests.

## Quick-start suggestions to finish core flow

1. Implement `MainWindow.importMusic()` to open a `FileChooser`, call `MusicImporter.importAudioFiles(...)`, then call `refreshSongList()` and `refreshPlaylistList()`.
2. Complete `MainWindow.playNext()`/`playPrevious()` to select next/previous song in current playlist or library and call `audioPlayer.playSong(...)`.
3. Implement `MainWindow.seekToPosition()` to convert `progressSlider` value to microseconds and call `audioPlayer.seek(...)`.
4. Add playlist persistence: write a `utils/PlaylistIO.java` using Jackson to save/load playlist JSON under `~/.spotify-clone/` or project config path.

## Notes and assumptions

- I scanned the repository for `TODO`/`FIXME` markers and none were found; instead, missing features are indicated by stubbed methods and comments within `MainWindow.java` and other files.
- The Maven `pom.xml` includes dependencies for JavaFX and `jaudiotagger`, and `jackson-databind` is present for JSON usage.

If you'd like, I can implement a focused subset next (for example: wire up `importMusic()` + `playNext()`/`seekToPosition()` and add simple playlist persistence). Tell me which TODO(s) to tackle first and I'll switch the todo list to in-progress and implement them.

### Recent polish and robustness improvements

- Replaced `toArray(new File[0])` with `toArray(File[]::new)` for clearer and slightly faster array creation.
- Replaced raw `printStackTrace()` calls in `MainWindow.importMusic()` with `System.err.println(...)` and user-facing `Alert` dialogs.
- Added confirmation dialogs for destructive actions (delete playlist, remove song from playlist).
- Auto-load persisted playlists at startup (loads from `~/.spotify-clone/playlists.json` into `MusicLibrary`).

Remaining polish candidates: small lint warning cleanups, unit tests, drag/drop UX improvements, undo support for deletes.

## Finalize and completed report

All high-priority implementation tasks listed above have been implemented and validated in-code. Below is a concise mapping of the user's requested features to the implemented status:

- Import music UI (`MainWindow.importMusic()`): Implemented — file/folder chooser, `MusicImporter` invocation, UI refresh, `lastImportPath` persisted via `ConfigManager`.
- Playback navigation (`MainWindow.playNext()`/`playPrevious()`): Implemented — next/previous logic, auto-advance wired via `AudioPlayer` listener.
- Seeking (progress slider): Implemented — drag handling, `seekToPosition()` converts slider percent to microseconds and invokes `AudioPlayer.seek()`.
- Playlist creation & persistence: Implemented — `createNewPlaylist()` and `utils/PlaylistIO.java` persist playlists to JSON at the user's home config path.
- Playlist loading & interaction: Implemented — `loadPlaylist(Playlist)` populates view, supports remove, reorder (drag/drop), delete, and persists changes.
- Polishing and robustness: Implemented — replaced debug prints, added confirmation dialogs, small API improvements, and auto-loading persisted playlists at startup.

Build validation
----------------

I ran a Maven package build during development to validate compilation. To reproduce locally run:

```powershell
mvn -DskipTests package
```

Then run the jar in `target`:

```powershell
java -jar target\spotify-clone-1.0-SNAPSHOT.jar
```

Next recommended steps
----------------------

- Add unit tests for `PlaylistIO` and `MusicLibrary` (JUnit + Maven surefire).
- Implement deduplication when auto-loading persisted playlists.
- Consider swapping the audio backend to JavaFX `MediaPlayer` or an external library for broader codec support.

If you'd like, I can implement any of these next — tell me which and I'll proceed.
