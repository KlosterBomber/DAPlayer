# Changelog

All notable changes to the DAPlayer project are documented in this file.

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
0.2.0
```
