# Changelog

All notable changes to the DAPlayer project are documented in this file.

## [0.4.1] - 2026-10-22

Bug fix release.

### Fixed

- FFmpeg playback (WMA, FLAC, OGG, Opus, M4A/AAC, AIFF, APE, WavPack and
  network streams) could corrupt memory and crash the application.
  The output pointer for the resampler was created with the wrong
  `PointerPointer` constructor, so `swr_convert` wrote the decoded audio to a
  random memory address. Depending on memory layout and garbage collection,
  this went unnoticed or crashed the player after a few seconds
  (`EXCEPTION_ACCESS_VIOLATION` / `STATUS_HEAP_CORRUPTION`).
  The bug existed since 0.3.0 and became visible with the higher memory
  activity of the music library in 0.4.0.

---

## [0.4.0] - 2026-10-22
 
Music library: a persistent, searchable collection of all tracks in the
user's music folders, fully integrated into the player display.
 
### Added
 
#### Music Library
 
- New **LIBRARY** display with three columns: ARTISTS | ALBUMS | TITLES
- Selecting an artist shows their albums, selecting an album shows its tracks
- Albums are listed with year, tracks with track number and duration
- Track count and total duration of the library in the status line
- Artist names: the most frequent spelling is used, names written completely
  in lower case are capitalized; the album artist takes precedence over the
  track artist
- Tracks without an album tag are grouped by their folder name
- Sorting follows German collation rules (umlauts, case)
- Mouse wheel scrolls each column separately
#### Library Management
 
- **FOLDERS** view: add and remove library folders
- Unreachable folders (e.g. disconnected drives) are marked **OFFLINE**
- Adding or removing a folder starts a rescan automatically
- **RESCAN** runs in the background with progress display (`SCANNING x/y`)
  and can be stopped with **CANCEL**
- Incremental scan: only new or changed files are read again
  (file size and modification date)
- Offline protection: tracks of unreachable folders are kept instead of being
  removed from the library
- Scan result in the status line (`+NEW ~CHANGED -REMOVED`)
- The current selection is restored after a rescan
#### Search
 
- Search field in the LIBRARY display (click or **CTRL+F**)
- All words of the query must match
- Results are shown as one list: `Artist - Title [Album]`
- Search is delayed until typing pauses (debounce, 200 ms)
- `NO MATCHES` / `SEARCHING ...` hints
#### Adding to the Playlist
 
- **ADD TO PLAYLIST** adds the selected track, or the whole album / all search
  results if no track is selected
- Double-click or ENTER on a track adds it to the playlist
- Tracks are added instantly from the library data (no tags are read again)
- Status messages such as `ADDED: <title>` disappear after 4 seconds
#### Keyboard
 
- LIBRARY: UP / DOWN move the selection, LEFT / RIGHT switch the column,
  ENTER adds to the playlist, ESC clears the search or leaves FOLDERS,
  DELETE removes the selected folder (FOLDERS)
- While the search field has the focus, all keys go to the search field
  (no accidental play/stop/mute while typing)
### Changed
 
- The former RESERVE menu button is now **LIBRARY**
- Number keys follow the order of the menu buttons:
  1 Playlist, 2 Trackinfo, **3 Library**, 4 Settings, 5 Visuals, 6 Equalizer
### Technical
 
- Added Gson 2.11.0 dependency
- New package `library`: `LibraryStore`, `LibraryScanner`, `MediaLibrary`
- New model class `LibraryEntry` (serializable library entry, conversion
  from/to `Track`)
- New display `LibraryDisplay`
- The library is stored in `library.json` in the configuration directory;
  saving is atomic (via a temporary file), a damaged file is kept as `.broken`
- `MediaLibrary` uses immutable lists (copy-on-write), so the user interface
  can keep reading while a scan is running in the background
- Library keys are handled before the global key bindings
  (chain of responsibility); text input uses `KEY_TYPED` events

---

## [0.3.0] - 2026-10-15

Completion of the open features, new formats and many bug fixes.

### Added

#### Formats & Streams

- Universal FFmpeg decoder (`FfmpegDecoder`) replaces `WmaDecoder`
- New formats: FLAC, OGG/Vorbis, Opus, M4A/AAC, AIFF, APE, WavPack
- Network streams / internet radio (http, https, ...) via **ADD LOCATION**
- WAV: 8/24/32 bit integer, 32/64 bit float, `WAVE_FORMAT_EXTENSIBLE`
- Multichannel material is downmixed to stereo (WAV and FFmpeg)
- Additional WAV INFO tags (genre, year, track number), UTF-8 detection

#### Playlist

- Double-click or ENTER plays a track
- **REMOVE** button or DELETE removes the selected track
- **ADD LOCATION**: add a URL, file or folder by typing it
- Drag & drop of files, folders, playlists and URLs onto the window
- Folders are scanned recursively and sorted
- Files are loaded in the background (progress "LOADING x/y")
- Load and save M3U/M3U8 (relative paths are resolved)
- Track count and total duration in the header line
- The playlist of the last session is restored on start

#### Controls

- Keyboard controls (see SETTINGS / **KEY CONTROLS**)
- Volume, shuffle, repeat and the last folder are remembered
- EQUALIZER: **RESET** button, double-click resets a single band
- PREV restarts the current track after more than 3 s
- Error messages for unplayable files in TRACKINFO
- Active submenu button in SETTINGS and VISUALS is highlighted
- Spectrum with 80 logarithmic bands, Hann window and fall-off
- TRACKINFO shows sample rate / bit depth / channels / bit rate
- Application icon and window title

### Fixed

- Artwork was re-read from disk ~60 times per second during playback (now LRU cache)
- MP3: mono and MPEG-2 files (e.g. 22.05 kHz) were decoded incorrectly (`SampleBuffer.getBufferLength()` was ignored)
- MP3: corrupt frames no longer end the whole track
- WMA tags were read with ID3 frame IDs and therefore empty; now format-independent via `FieldKey`
- Dates like `2004-05-01` threw an exception, losing all tags of the file
- 24-bit WAV (`WAVE_FORMAT_EXTENSIBLE`) was rejected; 8-bit WAV produced noise
- Hit areas of the playlist buttons did not match the drawn buttons
- Button state (PLAYING/PAUSED/STOPPED) was partly wrong after NEXT/PREV or at the end of the playlist
- NEXT/PREV ignored shuffle mode
- Shuffle + repeat with a single track threw an `IndexOutOfBoundsException`
- Shuffle ignored tracks added later
- EQ bands above Nyquist could become unstable; coefficients are now computed thread-safely in the audio thread
- `MASTER_GAIN` was used without checking (crash on some devices); software volume as fallback
- Missing audio devices prevented playback; fallback to the system default device
- Playback threads could close each other's decoder/line on fast track changes
- Position display ran ahead of the audible audio by the buffer size
- Configuration path was hard-wired to `AppData/Roaming` (Windows)
- Visualization no longer freezes on the last frame after STOP
- OFF shuts the application down cleanly (including saving the session)

### Changed

- Audio engine: every playback runs in its own session (thread, decoder, line); files and streams are opened in the playback thread
- `PlayerController` manages a central playback state (`STOPPED`, `PLAYING`, `PAUSED`)
- Lower seek latency through a smaller output buffer (300 ms)
- The ACCENT COLORS button "ÜBERNEHMEN" is now "APPLY"

---

## [0.2.0] - 2026-09-28

WMA support and continued playback and visualization improvements.

### Added

#### WMA Support

- WMA audio file support
- WMA playback using FFmpeg via JavaCPP
- WMA metadata reading
- WMA track duration
- WMA seeking
- WMA automatic track transition
- WMA pause and resume
- WMA PCM decoding

#### Playback Modes

- Shuffle playback mode
- Repeat playback mode

#### Audio Visualizations

- RGB visualization mode

### Technical

- Added FFmpeg JavaCPP platform dependency
- Added `WmaDecoder`
- WMA audio is decoded from planar floating-point samples to 16-bit PCM
- FFmpeg logging configured to show errors only

### Improved

- Audio decoder architecture now supports WAV, MP3 and WMA
- Visualization system supports RGB mode
- Playback engine supports shuffle and repeat modes

---

## [0.1.1] - 2026-09-24

Second documented functional development version of DAPlayer.

### Added

#### Equalizer

- 10-band graphic equalizer
- Frequency bands from 31 Hz to 16 kHz
- Gain control from -12 dB to +12 dB
- Real-time equalizer processing
- Persistent equalizer settings
- Equalizer display

#### Audio Visualizations

- Visuals display
- Spectrum visualization
- Waveform visualization
- VU-Meter visualization
- Vectorscope visualization
- Real-time visualization data
- Separate left and right channel data
- Peak and RMS level data for the VU-Meter

#### Playback Control

- Visual playback state indication
- PLAYING state
- PAUSED state
- STOPPED state
- Automatic stopped state after the end of the playlist

### Changed

#### User Interface

- Added Visuals to the main navigation
- Added Equalizer to the player display
- Added active state indication to the playback controls
- Improved visual feedback for playback states

#### Audio Engine

- Added visualization data processing
- Added visualization listener support
- Added left/right channel analysis
- Added peak level calculation
- Added RMS level calculation

### Improved

- Separated audio analysis from visualization rendering
- Visualization rendering is performed on the JavaFX application thread
- Improved visualization performance and stability

### Technical

- Added `VisualizationData`
- Added `VisualizationListener`
- Added `Equalizer`
- Added `EqualizerSettings`
- Added `SpectrumVisualization`
- Added `WaveformVisualization`
- Added `VuVisualization`
- Added `VectorScope`

---

## [0.1.0] - 2026-09-21

First documented functional development version of DAPlayer.

### Added

#### Project Foundation

* Initial DAPlayer JavaFX project
* Maven project configuration
* Basic application structure

#### User Interface

* Custom player background
* Custom volume knob
* Playback control buttons
* Menu buttons
* Off and navigation buttons
* Start display
* Track list display
* Track information display
* Settings display
* Custom UI colors and accent color handling

#### File and Playlist Handling

* File chooser
* Folder-based file selection
* Playlist implementation
* Track selection and navigation

#### Audio

* WAV decoder
* MP3 decoder
* Audio playback engine
* Playback control
* Automatic transition to the next track
* Automatic stop at the end of the playlist
* Volume control
* Mute functionality
* Audio output device selection

#### Track Information

* Track metadata display
* Album artwork display
* Track list
* Track information display

#### Playback Progress

* Current playback position
* Total track duration
* Visual playback progress bar
* Mouse-based seeking
* WAV seeking
* MP3 seeking

#### Settings

* Audio device settings
* Accent color settings

### Technical

* Java 21
* JavaFX 24.0.2
* Maven
* JAudioTagger 3.0.1
* JLayer 1.0.1

### Git History

The 0.1.0 development stage was built incrementally through the following major commits:

```text
a7c93be  Initial DAPlayer project
82a5e43  rebuild volume-knob
316ff87  added control-buttons
54b819b  added menu-buttons
bac8a6b  added off- and move-button
b0daedd  added FileChooser
3d3441c  added Playlist
1a14819  added WAVE-decoder
7fe3b78  added MP3-Decoder, TrackList and TrackInfo
5fb2947  added artwork to trackinfo
0495d44  added AudioDeviceSettings
93a2b50  added AccentColorSettings
75e78bd  added playback-logic
d7ca5fc  added track-progress-bar
```

The current 0.1.0 documentation also includes the subsequently completed seeking functionality for WAV and MP3.

---

## Versioning

DAPlayer currently follows semantic versioning in the form:

```text
MAJOR.MINOR.PATCH
```

The project is currently at:

```text
0.4.1
```
